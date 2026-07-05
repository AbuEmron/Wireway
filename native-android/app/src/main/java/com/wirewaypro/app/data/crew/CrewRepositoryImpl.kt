package com.wirewaypro.app.data.crew

import com.wirewaypro.app.data.local.CrewMemberDao
import com.wirewaypro.app.data.local.CrewMemberEntity
import com.wirewaypro.app.data.local.SyncStatus
import com.wirewaypro.app.data.offline.NetworkMonitor
import com.wirewaypro.app.data.offline.OfflineQueue
import com.wirewaypro.app.data.offline.QueuedSave
import com.wirewaypro.app.data.offline.isConnectivityError
import com.wirewaypro.app.domain.model.CrewMember
import com.wirewaypro.app.domain.model.CrewMemberInput
import com.wirewaypro.app.domain.repository.CrewRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first crew roster. Mirrors [com.wirewaypro.app.data.jobs.JobRepositoryImpl]:
 * write-through to Room first (crash-safe, instantly visible offline), push to
 * Supabase when online, and enqueue for the [SyncManager] otherwise. Reads serve
 * the Room cache after a best-effort refresh so the app works with no signal.
 */
@Singleton
class CrewRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
    private val queue: OfflineQueue,
    private val network: NetworkMonitor,
    private val dao: CrewMemberDao,
) : CrewRepository {

    private fun crew() = client.postgrest.from("crew_members")
    private val json = Json { ignoreUnknownKeys = true }

    override fun pendingSyncCount() = dao.pendingCount()

    /** Mirror the `crew_members` cache from Supabase, preserving unsynced local rows (LWW). */
    private suspend fun refresh(userId: String) {
        if (!network.isOnline()) return
        val rows = crew()
            .select {
                filter { eq("user_id", userId) }
                order("name", Order.ASCENDING)
                limit(500)
            }
            .decodeList<CrewMemberDto>()
        val now = System.currentTimeMillis()
        val pendingIds = dao.pending().mapTo(HashSet()) { it.id }
        dao.upsertAll(rows.filter { it.id !in pendingIds }.map { it.toEntity(userId, updatedAt = now) })
        val serverIds = rows.mapTo(HashSet()) { it.id }
        dao.allIds(userId).forEach { id -> if (id !in serverIds && id !in pendingIds) dao.hardDelete(id) }
    }

    override suspend fun getCrew(userId: String): Result<List<CrewMember>> = runCatching {
        runCatching { refresh(userId) } // offline → serve the cache
        dao.observeCrew(userId).first().map { it.toDomain() }
    }

    override suspend fun getCrewMember(id: String): Result<CrewMember> = runCatching {
        dao.getById(id)?.toDomain() ?: error("Crew member not found.")
    }

    override suspend fun saveCrew(userId: String, input: CrewMemberInput): Result<CrewMember> = runCatching {
        val rowId = input.id ?: UUID.randomUUID().toString()
        val payload = buildJsonObject {
            put("id", rowId)
            put("user_id", userId)
            put("name", input.name)
            put("role", input.role)
            put("hourly_cost_rate", input.hourlyCostRate)
            put("active", input.active)
        }
        val now = System.currentTimeMillis()

        // 1) Write-through to Room first (crash-safe, instantly visible offline).
        val createdAt = if (input.id != null) dao.getById(rowId)?.createdAt else null
        dao.upsert(localEntity(userId, payload, SyncStatus.PENDING, updatedAt = now, createdAt = createdAt))

        // 2) Push if online.
        if (network.isOnline()) {
            try {
                val saved = if (input.id == null) {
                    crew().insert(payload) { select() }.decodeSingle<CrewMemberDto>()
                } else {
                    crew().update(payload) {
                        filter { eq("id", rowId); eq("user_id", userId) }
                        select()
                    }.decodeSingle<CrewMemberDto>()
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
        queue.enqueue(QueuedSave(rowId, "crew_members", "upsert", payload.toString(), userId, now))
        json.decodeFromJsonElement(CrewMemberDto.serializer(), payload).toDomain()
    }

    override suspend fun deleteCrew(userId: String, crewId: String): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        if (network.isOnline()) {
            try {
                crew().delete { filter { eq("id", crewId); eq("user_id", userId) } }
                dao.hardDelete(crewId)
                return@runCatching
            } catch (e: Exception) {
                if (!isConnectivityError(e)) throw e
            }
        }
        dao.getById(crewId)?.let {
            dao.upsert(it.copy(deleted = true, syncStatus = SyncStatus.PENDING, updatedAt = now))
        }
        queue.enqueue(QueuedSave(crewId, "crew_members", "delete", "{}", userId, now))
    }

    /** Builds the local row from the exact push payload, tagged with a sync state. */
    private fun localEntity(
        userId: String,
        payload: JsonObject,
        syncStatus: String,
        updatedAt: Long,
        createdAt: String?,
    ): CrewMemberEntity =
        json.decodeFromJsonElement(CrewMemberDto.serializer(), payload)
            .toEntity(userId, updatedAt = updatedAt, syncStatus = syncStatus)
            .copy(createdAt = createdAt, deleted = false)
}
