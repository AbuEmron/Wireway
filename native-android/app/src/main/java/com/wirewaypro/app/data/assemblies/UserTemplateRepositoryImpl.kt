package com.wirewaypro.app.data.assemblies

import com.wirewaypro.app.data.local.SyncStatus
import com.wirewaypro.app.data.local.UserAssemblyDao
import com.wirewaypro.app.data.local.UserAssemblyEntity
import com.wirewaypro.app.data.offline.NetworkMonitor
import com.wirewaypro.app.data.offline.isConnectivityError
import com.wirewaypro.app.domain.model.UserTemplate
import com.wirewaypro.app.domain.repository.UserTemplateRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first [UserTemplateRepository]. Mirrors the job/quote repos: every
 * write lands in Room first (crash-safe, instantly visible with no signal),
 * then pushes to Supabase when online. Unsynced rows stay PENDING and get
 * pushed on the next [refresh]. No AI, no fabricated data — templates carry only
 * real catalog line references.
 */
@Singleton
class UserTemplateRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
    private val network: NetworkMonitor,
    private val dao: UserAssemblyDao,
) : UserTemplateRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private fun table() = client.postgrest.from("user_assemblies")

    override fun observe(userId: String): Flow<List<UserTemplate>> =
        dao.observe(userId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun refresh(userId: String): Result<Unit> = runCatching {
        if (!network.isOnline()) return@runCatching
        pushPending(userId)
        val rows = table()
            .select { filter { eq("user_id", userId) } }
            .decodeList<UserAssemblyDto>()
        val now = System.currentTimeMillis()
        val pendingIds = dao.pending().mapTo(HashSet()) { it.id }
        dao.upsertAll(rows.filter { it.id !in pendingIds }.map { it.toEntity(userId, updatedAt = now) })
        // Prune rows deleted on the server (that aren't waiting to push locally).
        val serverIds = rows.mapTo(HashSet()) { it.id }
        dao.allIds(userId).forEach { id ->
            if (id !in serverIds && id !in pendingIds) {
                dao.getById(id)?.let { dao.upsert(it.copy(deleted = true)) }
            }
        }
    }

    override suspend fun get(id: String): UserTemplate? = dao.getById(id)?.toDomain()

    override suspend fun save(userId: String, template: UserTemplate): Result<UserTemplate> = runCatching {
        val now = System.currentTimeMillis()
        val dto = template.toDto(userId)
        val createdAt = dao.getById(template.id)?.createdAt

        // 1) Write-through to Room first.
        dao.upsert(dto.toEntity(userId, SyncStatus.PENDING, updatedAt = now, createdAt = createdAt))

        // 2) Push if online (update-then-insert, no upsert API dependency).
        if (network.isOnline()) {
            try {
                val payload = buildJsonObject {
                    put("id", template.id)
                    put("user_id", userId)
                    put("name", template.name)
                    put("description", template.description)
                    put("category", template.category)
                    put("items", json.encodeToJsonElement(ListSerializer(UserAssemblyItemDto.serializer()), dto.items))
                }
                val updated = table().update(payload) {
                    filter { eq("id", template.id); eq("user_id", userId) }
                    select()
                }.decodeList<UserAssemblyDto>()
                val saved = updated.firstOrNull()
                    ?: table().insert(payload) { select() }.decodeSingle<UserAssemblyDto>()
                dao.upsert(saved.toEntity(userId, SyncStatus.SYNCED, updatedAt = now, createdAt = createdAt))
                return@runCatching saved.toDomain(com.wirewaypro.app.domain.model.SyncState.SYNCED)
            } catch (e: Exception) {
                if (!isConnectivityError(e)) {
                    dao.markError(template.id)
                    throw e
                }
            }
        }
        // 3) Offline — stays PENDING, surfaced as such; pushes on next refresh.
        template.copy(syncState = com.wirewaypro.app.domain.model.SyncState.PENDING)
    }

    override suspend fun delete(userId: String, id: String): Result<Unit> = runCatching {
        if (network.isOnline()) {
            try {
                table().delete { filter { eq("id", id); eq("user_id", userId) } }
                dao.getById(id)?.let { dao.upsert(it.copy(deleted = true, syncStatus = SyncStatus.SYNCED)) }
                return@runCatching
            } catch (e: Exception) {
                if (!isConnectivityError(e)) throw e
            }
        }
        // Offline: tombstone locally, pushed on next refresh.
        val now = System.currentTimeMillis()
        dao.getById(id)?.let { dao.upsert(it.copy(deleted = true, syncStatus = SyncStatus.PENDING, updatedAt = now)) }
    }

    /** Flush locally-PENDING creates/edits/deletes to the server (best effort). */
    private suspend fun pushPending(userId: String) {
        dao.pending().forEach { row ->
            runCatching { pushOne(userId, row) }
                .onSuccess { dao.markSynced(row.id) }
                .onFailure { if (!isConnectivityError(it)) dao.markError(row.id) }
        }
    }

    private suspend fun pushOne(userId: String, row: UserAssemblyEntity) {
        if (row.deleted) {
            table().delete { filter { eq("id", row.id); eq("user_id", userId) } }
            return
        }
        val dto = decodeUserAssembly(row.payloadJson)
        val payload = buildJsonObject {
            put("id", row.id)
            put("user_id", userId)
            put("name", dto.name)
            put("description", dto.description)
            put("category", dto.category)
            put("items", json.encodeToJsonElement(ListSerializer(UserAssemblyItemDto.serializer()), dto.items))
        }
        val updated = table().update(payload) {
            filter { eq("id", row.id); eq("user_id", userId) }
            select()
        }.decodeList<UserAssemblyDto>()
        if (updated.isEmpty()) table().insert(payload) { }
    }
}
