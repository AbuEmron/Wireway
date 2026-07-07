package com.wirewaypro.app.data.clients

import com.wirewaypro.app.data.local.ClientDao
import com.wirewaypro.app.data.local.ClientEntity
import com.wirewaypro.app.data.local.SyncStatus
import com.wirewaypro.app.data.offline.NetworkMonitor
import com.wirewaypro.app.data.offline.OfflineQueue
import com.wirewaypro.app.data.offline.QueuedSave
import com.wirewaypro.app.data.offline.isConnectivityError
import com.wirewaypro.app.domain.model.Client
import com.wirewaypro.app.domain.model.ClientInput
import com.wirewaypro.app.domain.repository.ClientRepository
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

@Singleton
class ClientRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
    private val queue: OfflineQueue,
    private val network: NetworkMonitor,
    private val dao: ClientDao,
) : ClientRepository {

    private fun clients() = client.postgrest.from("clients")

    private val json = Json { ignoreUnknownKeys = true }

    override fun pendingSyncCount() = dao.pendingCount()

    /** Mirror the `clients` cache from Supabase, preserving unsynced local rows (LWW). */
    private suspend fun refresh(userId: String) {
        if (!network.isOnline()) return
        val rows = clients()
            .select {
                filter { eq("user_id", userId) }
                order("name", Order.ASCENDING)
            }
            .decodeList<ClientDto>()
        val now = System.currentTimeMillis()
        val pendingIds = dao.pending().mapTo(HashSet()) { it.id }
        dao.upsertAll(rows.filter { it.id !in pendingIds }.map { it.toEntity(userId, updatedAt = now) })
        val serverIds = rows.mapTo(HashSet()) { it.id }
        dao.allIds(userId).forEach { id -> if (id !in serverIds && id !in pendingIds) dao.hardDelete(id) }
    }

    override suspend fun getClients(userId: String): Result<List<Client>> = runCatching {
        runCatching { refresh(userId) } // offline → serve the cache
        dao.observeClients(userId).first().map { it.toDomain() }
    }

    override suspend fun saveClient(userId: String, input: ClientInput): Result<Client> = runCatching {
        // Matches the web app's client columns: name + nullable email/phone.
        // Stable id + user_id in the body so an offline upsert is idempotent.
        val rowId = input.id ?: UUID.randomUUID().toString()
        val payload = buildJsonObject {
            put("id", rowId)
            put("user_id", userId)
            put("name", input.name)
            put("email", input.email)
            put("phone", input.phone)
        }
        val now = System.currentTimeMillis()

        // 1) Write-through to Room first.
        val createdAt = if (input.id != null) dao.getById(rowId)?.createdAt else null
        dao.upsert(localEntity(userId, payload, SyncStatus.PENDING, updatedAt = now, createdAt = createdAt))

        // 2) Push if online.
        if (network.isOnline()) {
            try {
                val saved = if (input.id == null) {
                    clients().insert(payload) { select() }.decodeSingle<ClientDto>()
                } else {
                    clients().update(payload) {
                        filter { eq("id", rowId); eq("user_id", userId) }
                        select()
                    }.decodeSingle<ClientDto>()
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
        queue.enqueue(QueuedSave(rowId, "clients", "upsert", payload.toString(), userId, now))
        json.decodeFromJsonElement(ClientDto.serializer(), payload).toDomain()
    }

    override suspend fun deleteClient(userId: String, clientId: String): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        if (network.isOnline()) {
            try {
                clients().delete { filter { eq("id", clientId); eq("user_id", userId) } }
                dao.hardDelete(clientId)
                return@runCatching
            } catch (e: Exception) {
                if (!isConnectivityError(e)) throw e
            }
        }
        dao.getById(clientId)?.let {
            dao.upsert(it.copy(deleted = true, syncStatus = SyncStatus.PENDING, updatedAt = now))
        }
        queue.enqueue(QueuedSave(clientId, "clients", "delete", "{}", userId, now))
    }

    /** Builds the local row from the exact push payload, tagged with a sync state. */
    private fun localEntity(
        userId: String,
        payload: JsonObject,
        syncStatus: String,
        updatedAt: Long,
        createdAt: String?,
    ): ClientEntity =
        json.decodeFromJsonElement(ClientDto.serializer(), payload)
            .toEntity(userId, updatedAt = updatedAt, syncStatus = syncStatus)
            .copy(createdAt = createdAt, deleted = false)
}
