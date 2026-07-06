package com.wirewaypro.app.data.ahj

import com.wirewaypro.app.data.local.JurisdictionDao
import com.wirewaypro.app.data.local.JurisdictionEntity
import com.wirewaypro.app.data.local.SyncStatus
import com.wirewaypro.app.data.offline.NetworkMonitor
import com.wirewaypro.app.data.offline.OfflineQueue
import com.wirewaypro.app.data.offline.QueuedSave
import com.wirewaypro.app.data.offline.isConnectivityError
import com.wirewaypro.app.domain.model.Jurisdiction
import com.wirewaypro.app.domain.model.JurisdictionInput
import com.wirewaypro.app.domain.model.JurisdictionSource
import com.wirewaypro.app.domain.repository.JurisdictionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first AHJ jurisdiction. Mirrors [com.wirewaypro.app.data.crew.CrewRepositoryImpl]:
 * write-through to Room first (crash-safe, instantly visible offline), push to
 * Supabase when online, and enqueue for the [com.wirewaypro.app.data.offline.SyncManager]
 * otherwise. One active jurisdiction per user — [saveJurisdiction] reuses the
 * existing row id so a change updates in place rather than piling up rows.
 */
@Singleton
class JurisdictionRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
    private val queue: OfflineQueue,
    private val network: NetworkMonitor,
    private val dao: JurisdictionDao,
) : JurisdictionRepository {

    private fun table() = client.postgrest.from("user_jurisdictions")
    private val json = Json { ignoreUnknownKeys = true }

    override fun pendingSyncCount() = dao.pendingCount()

    override fun observeJurisdiction(userId: String): Flow<Jurisdiction?> =
        dao.observeCurrent(userId).map { it?.toDomain() }

    /** Mirror the user's jurisdiction from Supabase, preserving an unsynced local row (LWW). */
    private suspend fun refresh(userId: String) {
        if (!network.isOnline()) return
        val rows = table()
            .select { filter { eq("user_id", userId) } }
            .decodeList<JurisdictionDto>()
        val now = System.currentTimeMillis()
        val pendingIds = dao.pending().mapTo(HashSet()) { it.id }
        dao.upsertAll(rows.filter { it.id !in pendingIds }.map { it.toEntity(userId, updatedAt = now) })
        val serverIds = rows.mapTo(HashSet()) { it.id }
        dao.allIds(userId).forEach { id -> if (id !in serverIds && id !in pendingIds) dao.hardDelete(id) }
    }

    override suspend fun getJurisdiction(userId: String): Result<Jurisdiction?> = runCatching {
        runCatching { refresh(userId) } // offline → serve the cache
        dao.observeCurrent(userId).first()?.toDomain()
    }

    override suspend fun saveJurisdiction(userId: String, input: JurisdictionInput): Result<Jurisdiction> = runCatching {
        // Reuse the existing row id so the user keeps exactly one jurisdiction.
        val existing = dao.getCurrent(userId)
        val rowId = input.id ?: existing?.id ?: UUID.randomUUID().toString()
        val isNew = existing == null
        val sourceValue = when (input.source) {
            JurisdictionSource.GPS_CONFIRMED -> "gps_confirmed"
            JurisdictionSource.MANUAL -> "manual"
        }
        val payload = buildJsonObject {
            put("id", rowId)
            put("user_id", userId)
            put("state_code", input.stateCode)
            put("county", input.county)
            put("city", input.city)
            put("source", sourceValue)
        }
        val now = System.currentTimeMillis()

        // 1) Write-through to Room first (crash-safe, instantly visible offline).
        val createdAt = existing?.createdAt
        dao.upsert(localEntity(userId, payload, SyncStatus.PENDING, updatedAt = now, createdAt = createdAt))

        // 2) Push if online — insert the first time, update in place after (one row
        //    per user). Mirrors CrewRepositoryImpl's known-good insert/update path.
        if (network.isOnline()) {
            try {
                val saved = if (isNew) {
                    table().insert(payload) { select() }.decodeSingle<JurisdictionDto>()
                } else {
                    table().update(payload) {
                        filter { eq("id", rowId); eq("user_id", userId) }
                        select()
                    }.decodeSingle<JurisdictionDto>()
                }
                dao.upsert(saved.toEntity(userId, updatedAt = now, syncStatus = SyncStatus.SYNCED))
                return@runCatching saved.toDomain()
            } catch (e: Exception) {
                if (!isConnectivityError(e)) {
                    dao.markError(rowId)
                    throw e
                }
            }
        }

        // 3) Offline — row stays PENDING; enqueue the upsert for later.
        queue.enqueue(QueuedSave(rowId, "user_jurisdictions", "upsert", payload.toString(), userId, now))
        json.decodeFromJsonElement(JurisdictionDto.serializer(), payload).toDomain()
    }

    /** Builds the local row from the exact push payload, tagged with a sync state. */
    private fun localEntity(
        userId: String,
        payload: JsonObject,
        syncStatus: String,
        updatedAt: Long,
        createdAt: String?,
    ): JurisdictionEntity =
        json.decodeFromJsonElement(JurisdictionDto.serializer(), payload)
            .toEntity(userId, updatedAt = updatedAt, syncStatus = syncStatus)
            .copy(createdAt = createdAt, deleted = false)
}
