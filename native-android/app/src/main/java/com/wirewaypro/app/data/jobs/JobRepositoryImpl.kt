package com.wirewaypro.app.data.jobs

import com.wirewaypro.app.data.local.JobDao
import com.wirewaypro.app.data.local.JobDrawDao
import com.wirewaypro.app.data.local.JobDrawEntity
import com.wirewaypro.app.data.local.JobEntity
import com.wirewaypro.app.data.local.SyncStatus
import com.wirewaypro.app.data.offline.NetworkMonitor
import com.wirewaypro.app.data.offline.OfflineQueue
import com.wirewaypro.app.data.offline.QueuedSave
import com.wirewaypro.app.data.offline.isConnectivityError
import com.wirewaypro.app.domain.model.Job
import com.wirewaypro.app.domain.model.JobDraw
import com.wirewaypro.app.domain.model.JobDrawInput
import com.wirewaypro.app.domain.model.JobInput
import com.wirewaypro.app.domain.repository.JobRepository
import com.wirewaypro.app.domain.util.IsoDate
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
    private val queue: OfflineQueue,
    private val network: NetworkMonitor,
    private val dao: JobDao,
    private val drawDao: JobDrawDao,
) : JobRepository {

    private fun jobs() = client.postgrest.from("jobs")
    private fun draws() = client.postgrest.from("job_draws")

    private val json = Json { ignoreUnknownKeys = true }

    override fun pendingSyncCount() = dao.pendingCount()

    /** Mirror the `jobs` cache from Supabase, preserving unsynced local rows (LWW). */
    private suspend fun refresh(userId: String) {
        if (!network.isOnline()) return
        val rows = jobs()
            .select {
                filter { eq("user_id", userId) }
                order("scheduled_date", Order.DESCENDING)
                order("created_at", Order.DESCENDING)
                limit(200)
            }
            .decodeList<JobDto>()
        val now = System.currentTimeMillis()
        val pendingIds = dao.pending().mapTo(HashSet()) { it.id }
        dao.upsertAll(rows.filter { it.id !in pendingIds }.map { it.toEntity(userId, updatedAt = now) })
        val serverIds = rows.mapTo(HashSet()) { it.id }
        dao.allIds(userId).forEach { id -> if (id !in serverIds && id !in pendingIds) dao.hardDelete(id) }
    }

    override suspend fun getJobs(userId: String): Result<List<Job>> = runCatching {
        runCatching { refresh(userId) } // offline → serve the cache
        dao.observeJobs(userId).first().map { it.toDomain() }
    }

    override suspend fun getJob(jobId: String): Result<Job> = runCatching {
        dao.getById(jobId)?.toDomain()
            ?: jobs()
                .select { filter { eq("id", jobId) } }
                .decodeSingleOrNull<JobDto>()
                ?.toDomain()
            ?: error("Job not found.")
    }

    /** Mirror a job's draws from Supabase, preserving unsynced local rows (LWW). */
    private suspend fun refreshDraws(jobId: String) {
        if (!network.isOnline()) return
        val rows = draws()
            .select {
                filter { eq("job_id", jobId) }
                order("sort_order", Order.ASCENDING)
            }
            .decodeList<JobDrawDto>()
        val now = System.currentTimeMillis()
        val pendingIds = drawDao.pending().mapTo(HashSet()) { it.id }
        drawDao.upsertAll(
            rows.filter { it.id !in pendingIds }.map { it.toEntity(fallbackJobId = jobId, updatedAt = now) },
        )
        val serverIds = rows.mapTo(HashSet()) { it.id }
        drawDao.allIdsForJob(jobId).forEach { id ->
            if (id !in serverIds && id !in pendingIds) drawDao.hardDelete(id)
        }
    }

    override suspend fun getJobDraws(jobId: String): Result<List<JobDraw>> = runCatching {
        runCatching { refreshDraws(jobId) } // offline → serve the cache
        drawDao.observeForJob(jobId).first().map { it.toDomain() }
    }

    override suspend fun getDuePendingDraws(userId: String, onOrBeforeDate: String): Result<List<JobDraw>> =
        runCatching {
            // Best-effort refresh (no prune — this is a cross-job query), then read Room.
            if (network.isOnline()) {
                runCatching {
                    val rows = draws()
                        .select {
                            filter {
                                eq("user_id", userId)
                                eq("status", "pending")
                                lte("due_date", onOrBeforeDate)
                            }
                            order("due_date", Order.ASCENDING)
                        }
                        .decodeList<JobDrawDto>()
                    val now = System.currentTimeMillis()
                    val pendingIds = drawDao.pending().mapTo(HashSet()) { it.id }
                    drawDao.upsertAll(
                        rows.filter { it.id !in pendingIds }.map { it.toEntity(fallbackUserId = userId, updatedAt = now) },
                    )
                }
            }
            drawDao.duePending(userId, onOrBeforeDate).map { it.toDomain() }
        }

    override suspend fun saveJob(userId: String, input: JobInput): Result<Job> = runCatching {
        // Stable id + user_id in the body so an offline upsert is idempotent.
        val rowId = input.id ?: UUID.randomUUID().toString()
        val payload = buildJsonObject {
            put("id", rowId)
            put("user_id", userId)
            put("title", input.title)
            put("client_name", input.clientName)
            put("client_phone", input.clientPhone)
            put("client_email", input.clientEmail)
            put("job_address", input.jobAddress)
            put("notes", input.notes)
            put("scheduled_date", IsoDate.normalizeOrNull(input.scheduledDate))
            put("scheduled_time", input.scheduledTime)
            put("duration_hours", input.durationHours)
            put("status", input.status)
            put("total", input.total)
            put("quote_id", input.quoteId)
        }
        val now = System.currentTimeMillis()

        // 1) Write-through to Room first (crash-safe, instantly visible offline).
        val createdAt = if (input.id != null) dao.getById(rowId)?.createdAt else null
        dao.upsert(localEntity(userId, payload, SyncStatus.PENDING, updatedAt = now, createdAt = createdAt))

        // 2) Push if online.
        if (network.isOnline()) {
            try {
                val saved = if (input.id == null) {
                    jobs().insert(payload) { select() }.decodeSingle<JobDto>()
                } else {
                    jobs().update(payload) {
                        filter { eq("id", rowId); eq("user_id", userId) }
                        select()
                    }.decodeSingle<JobDto>()
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
        queue.enqueue(
            QueuedSave(rowId, "jobs", "upsert", payload.toString(), userId, now),
        )
        json.decodeFromJsonElement(JobDto.serializer(), payload).toDomain()
    }

    override suspend fun deleteJob(userId: String, jobId: String): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        if (network.isOnline()) {
            try {
                jobs().delete { filter { eq("id", jobId); eq("user_id", userId) } }
                dao.hardDelete(jobId)
                return@runCatching
            } catch (e: Exception) {
                if (!isConnectivityError(e)) throw e
            }
        }
        dao.getById(jobId)?.let {
            dao.upsert(it.copy(deleted = true, syncStatus = SyncStatus.PENDING, updatedAt = now))
        }
        queue.enqueue(QueuedSave(jobId, "jobs", "delete", "{}", userId, now))
    }

    /** Builds the local row from the exact push payload, tagged with a sync state. */
    private fun localEntity(
        userId: String,
        payload: JsonObject,
        syncStatus: String,
        updatedAt: Long,
        createdAt: String?,
    ): JobEntity =
        json.decodeFromJsonElement(JobDto.serializer(), payload)
            .toEntity(userId, updatedAt = updatedAt, syncStatus = syncStatus)
            .copy(createdAt = createdAt, deleted = false)

    override suspend fun saveDraw(userId: String, input: JobDrawInput): Result<JobDraw> = runCatching {
        val rowId = input.id ?: UUID.randomUUID().toString()
        val payload = buildJsonObject {
            put("id", rowId)
            put("user_id", userId)
            put("job_id", input.jobId)
            put("label", input.label)
            put("amount", input.amount)
            put("retainage_pct", input.retainagePct)
            put("status", input.status)
            put("due_date", IsoDate.normalizeOrNull(input.dueDate))
            put("sort_order", input.sortOrder)
        }
        val now = System.currentTimeMillis()

        // 1) Write-through to Room first.
        drawDao.upsert(localDraw(userId, input.jobId, payload, SyncStatus.PENDING, now))

        // 2) Push if online.
        if (network.isOnline()) {
            try {
                val saved = if (input.id == null) {
                    draws().insert(payload) { select() }.decodeSingle<JobDrawDto>()
                } else {
                    draws().update(payload) {
                        filter { eq("id", rowId); eq("user_id", userId) }
                        select()
                    }.decodeSingle<JobDrawDto>()
                }
                drawDao.upsert(saved.toEntity(userId, input.jobId, updatedAt = now, syncStatus = SyncStatus.SYNCED))
                return@runCatching saved.toDomain()
            } catch (e: Exception) {
                if (!isConnectivityError(e)) {
                    drawDao.markError(rowId)
                    throw e
                }
            }
        }

        // 3) Offline — row stays PENDING; enqueue the upsert for later.
        queue.enqueue(QueuedSave(rowId, "job_draws", "upsert", payload.toString(), userId, now))
        json.decodeFromJsonElement(JobDrawDto.serializer(), payload).toDomain()
    }

    override suspend fun deleteDraw(userId: String, drawId: String): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        if (network.isOnline()) {
            try {
                draws().delete { filter { eq("id", drawId); eq("user_id", userId) } }
                drawDao.hardDelete(drawId)
                return@runCatching
            } catch (e: Exception) {
                if (!isConnectivityError(e)) throw e
            }
        }
        drawDao.getById(drawId)?.let {
            drawDao.upsert(it.copy(deleted = true, syncStatus = SyncStatus.PENDING, updatedAt = now))
        }
        queue.enqueue(QueuedSave(drawId, "job_draws", "delete", "{}", userId, now))
    }

    override suspend fun setDrawStatus(
        userId: String,
        drawId: String,
        status: String,
    ): Result<JobDraw> = runCatching {
        val row = drawDao.getById(drawId) ?: error("Draw not found.")
        val now = System.currentTimeMillis()
        // Patch the FULL cached row so an offline upsert replays every column.
        val patched = patchPayload(row.payloadJson) {
            put("status", status)
            if (status == "invoiced") put("invoiced_at", Instant.now().toString())
            if (status == "paid") put("paid_at", Instant.now().toString())
        }

        // 1) Write-through to Room first.
        drawDao.upsert(row.copy(status = status, payloadJson = patched.toString(), syncStatus = SyncStatus.PENDING, updatedAt = now))

        // 2) Push if online.
        if (network.isOnline()) {
            try {
                val saved = draws().update(patched) {
                    filter { eq("id", drawId); eq("user_id", userId) }
                    select()
                }.decodeSingle<JobDrawDto>()
                drawDao.upsert(saved.toEntity(userId, row.jobId, updatedAt = now, syncStatus = SyncStatus.SYNCED))
                return@runCatching saved.toDomain()
            } catch (e: Exception) {
                if (!isConnectivityError(e)) {
                    drawDao.markError(drawId)
                    throw e
                }
            }
        }

        // 3) Offline — row stays PENDING; enqueue the upsert for later.
        queue.enqueue(QueuedSave(drawId, "job_draws", "upsert", patched.toString(), userId, now))
        json.decodeFromJsonElement(JobDrawDto.serializer(), patched).toDomain()
    }

    /** Local draw row built from the exact push payload, tagged with a sync state. */
    private fun localDraw(
        userId: String,
        jobId: String,
        payload: JsonObject,
        syncStatus: String,
        updatedAt: Long,
    ): JobDrawEntity =
        json.decodeFromJsonElement(JobDrawDto.serializer(), payload)
            .toEntity(fallbackUserId = userId, fallbackJobId = jobId, updatedAt = updatedAt, syncStatus = syncStatus)

    /** Returns [payloadJson] parsed and merged with [changes] (adds/overrides keys). */
    private fun patchPayload(payloadJson: String, changes: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit): JsonObject {
        val base = json.parseToJsonElement(payloadJson) as JsonObject
        return buildJsonObject {
            base.forEach { (k, v) -> put(k, v) }
            changes()
        }
    }
}
