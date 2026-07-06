package com.wirewaypro.app.esign.data

import com.wirewaypro.app.data.local.SyncStatus
import com.wirewaypro.app.data.offline.NetworkMonitor
import com.wirewaypro.app.data.offline.OfflineQueue
import com.wirewaypro.app.data.offline.QueuedSave
import com.wirewaypro.app.data.offline.isConnectivityError
import com.wirewaypro.app.esign.EsignAuditEvent
import com.wirewaypro.app.esign.EsignEventType
import com.wirewaypro.app.esign.EsignRecord
import com.wirewaypro.app.esign.Signer
import com.wirewaypro.app.esign.SignatureMethod
import com.wirewaypro.app.esign.SigningContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistence + sync for the e-signature module. Offline-first and append-only:
 * every record and every audit event is written to Room FIRST (crash-safe,
 * instantly durable, never lost in the field), then pushed to Supabase when
 * online, or enqueued for the [com.wirewaypro.app.data.offline.SyncManager]
 * otherwise. The server side is INSERT-only (append-only RLS), so pushes never
 * update or delete — a duplicate id on retry is treated as already-synced.
 */
interface EsignRepository {
    /** Insert a sealed record (write-once). Room-first; push best-effort. */
    suspend fun insertRecord(userId: String, record: EsignRecord): Result<Unit>

    /** Append one audit event (write-once). Room-first; push best-effort. */
    suspend fun appendEvent(
        userId: String,
        recordId: String,
        type: EsignEventType,
        consentVersion: String,
        atMillis: Long,
        detailJson: String? = null,
    ): Result<EsignAuditEvent>

    /** The most-recent sealed record for a quote, or null if never signed. */
    suspend fun latestForQuote(quoteId: String): EsignRecord?

    /** Live stream of a quote's sealed records (for the signed-status UI). */
    fun observeForQuote(quoteId: String): Flow<List<EsignRecord>>

    /** The append-only event log for a record, oldest first. */
    suspend fun auditTrail(recordId: String): List<EsignAuditEvent>

    /** Count of records/events still waiting to sync. */
    fun pendingSyncCount(): Flow<Int>
}

@Singleton
class EsignRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
    private val queue: OfflineQueue,
    private val network: NetworkMonitor,
    private val recordDao: EsignRecordDao,
    private val auditDao: EsignAuditEventDao,
) : EsignRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private fun records() = client.postgrest.from(TABLE_RECORDS)
    private fun events() = client.postgrest.from(TABLE_EVENTS)

    override fun pendingSyncCount(): Flow<Int> = recordDao.pendingCount()

    override suspend fun insertRecord(userId: String, record: EsignRecord): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val payload = record.toPushPayload(userId)
        // 1) Room first — the sealed record is durable the instant this returns,
        //    online or not. This is the never-lose-a-signed-record guarantee.
        recordDao.insert(record.toEntity(userId, payload.toString(), SyncStatus.PENDING, now))

        // 2) Push (insert-only). Any failure keeps the row PENDING and enqueues it;
        //    the seal still succeeded locally.
        pushInsert(
            table = TABLE_RECORDS,
            id = record.id,
            userId = userId,
            payload = payload,
            markSynced = { recordDao.markSynced(record.id) },
            markError = { recordDao.markError(record.id) },
        )
    }

    override suspend fun appendEvent(
        userId: String,
        recordId: String,
        type: EsignEventType,
        consentVersion: String,
        atMillis: Long,
        detailJson: String?,
    ): Result<EsignAuditEvent> = runCatching {
        val eventId = java.util.UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val payload = buildJsonObject {
            put("id", eventId)
            put("user_id", userId)
            put("record_id", recordId)
            put("event_type", type.wire)
            put("at", Instant.ofEpochMilli(atMillis).toString())
            put("consent_version", consentVersion)
            put("detail_json", detailJson)
        }
        auditDao.insert(
            EsignAuditEventEntity(
                id = eventId,
                userId = userId,
                recordId = recordId,
                eventType = type.wire,
                atMillis = atMillis,
                consentVersion = consentVersion,
                detailJson = detailJson,
                syncStatus = SyncStatus.PENDING,
                updatedAt = now,
                createdAt = Instant.ofEpochMilli(now).toString(),
            ),
        )
        pushInsert(
            table = TABLE_EVENTS,
            id = eventId,
            userId = userId,
            payload = payload,
            markSynced = { auditDao.markSynced(eventId) },
            markError = { auditDao.markError(eventId) },
        )
        EsignAuditEvent(eventId, recordId, type, atMillis, consentVersion, detailJson)
    }

    override suspend fun latestForQuote(quoteId: String): EsignRecord? =
        recordDao.latestForQuote(quoteId)?.toDomain()

    override fun observeForQuote(quoteId: String): Flow<List<EsignRecord>> =
        recordDao.observeForQuote(quoteId).map { list -> list.map { it.toDomain() } }

    override suspend fun auditTrail(recordId: String): List<EsignAuditEvent> =
        auditDao.forRecord(recordId).map { it.toDomain() }

    /**
     * Best-effort INSERT push shared by records + events. On success marks the row
     * synced; on a duplicate-key error treats it as already-synced (idempotent
     * append-only retry); on any other server error flags the row ERROR but keeps
     * it locally (never lost) and enqueues for a later retry; offline → enqueue.
     */
    private suspend fun pushInsert(
        table: String,
        id: String,
        userId: String,
        payload: JsonObject,
        markSynced: suspend () -> Unit,
        markError: suspend () -> Unit,
    ) {
        val now = System.currentTimeMillis()
        if (network.isOnline()) {
            try {
                client.postgrest.from(table).insert(payload)
                markSynced()
                return
            } catch (e: Exception) {
                if (isDuplicateKey(e)) { markSynced(); return }
                if (!isConnectivityError(e)) {
                    // Server rejected (e.g. table/migration not applied yet). Keep the
                    // row, flag it, and park it in the queue so a retry replays it.
                    markError()
                    queue.enqueue(QueuedSave(id, table, "insert", payload.toString(), userId, now))
                    return
                }
            }
        }
        // Offline (or connectivity error mid-push) — row stays PENDING; enqueue.
        queue.enqueue(QueuedSave(id, table, "insert", payload.toString(), userId, now))
    }

    private fun isDuplicateKey(t: Throwable): Boolean {
        val m = (t.message ?: "").lowercase()
        return "duplicate key" in m || "23505" in m || "already exists" in m
    }

    companion object {
        const val TABLE_RECORDS = "esign_records"
        const val TABLE_EVENTS = "esign_audit_events"
    }
}

// ── DTO / entity / domain mapping ────────────────────────────────────────────────

private fun EsignRecord.toPushPayload(userId: String): JsonObject = buildJsonObject {
    put("id", id)
    put("user_id", userId)
    put("quote_id", quoteId)
    put("document_title", documentTitle)
    put("signer_name", signer.name)
    put("signer_email", signer.email)
    put("method", method.wire)
    put("consent_version", consentVersion)
    put("consent_given_at", Instant.ofEpochMilli(consentGivenAtMillis).toString())
    put("signed_at", Instant.ofEpochMilli(signedAtMillis).toString())
    put("content_sha256", contentSha256)
    put("sealed_sha256", sealedSha256)
    put("device_model", context.deviceModel)
    put("app_version", context.appVersion)
    put("ip_address", context.ipAddress)
}

private fun EsignRecord.toEntity(
    userId: String,
    payloadJson: String,
    syncStatus: String,
    updatedAt: Long,
): EsignRecordEntity = EsignRecordEntity(
    id = id,
    userId = userId,
    quoteId = quoteId,
    documentTitle = documentTitle,
    signerName = signer.name,
    signerEmail = signer.email,
    method = method.wire,
    consentVersion = consentVersion,
    consentGivenAt = consentGivenAtMillis,
    signedAt = signedAtMillis,
    contentSha256 = contentSha256,
    sealedSha256 = sealedSha256,
    sealedPdfPath = sealedPdfPath,
    encryptedSignaturePath = encryptedSignaturePath,
    deviceModel = context.deviceModel,
    appVersion = context.appVersion,
    ipAddress = context.ipAddress,
    payloadJson = payloadJson,
    syncStatus = syncStatus,
    updatedAt = updatedAt,
    createdAt = Instant.ofEpochMilli(updatedAt).toString(),
)

private fun EsignRecordEntity.toDomain(): EsignRecord = EsignRecord(
    id = id,
    quoteId = quoteId,
    documentTitle = documentTitle,
    signer = Signer(signerName, signerEmail),
    method = SignatureMethod.from(method),
    consentVersion = consentVersion,
    consentGivenAtMillis = consentGivenAt,
    signedAtMillis = signedAt,
    contentSha256 = contentSha256,
    sealedSha256 = sealedSha256,
    sealedPdfPath = sealedPdfPath,
    encryptedSignaturePath = encryptedSignaturePath,
    context = SigningContext(deviceModel, appVersion, ipAddress),
)

private fun EsignAuditEventEntity.toDomain(): EsignAuditEvent = EsignAuditEvent(
    id = id,
    recordId = recordId,
    type = EsignEventType.from(eventType) ?: EsignEventType.SIGNATURE_CAPTURED,
    atMillis = atMillis,
    consentVersion = consentVersion,
    detailJson = detailJson,
)
