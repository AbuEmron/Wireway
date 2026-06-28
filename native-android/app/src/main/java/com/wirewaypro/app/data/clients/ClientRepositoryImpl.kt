package com.wirewaypro.app.data.clients

import com.wirewaypro.app.domain.model.Client
import com.wirewaypro.app.domain.model.ClientInput
import com.wirewaypro.app.domain.repository.ClientRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
) : ClientRepository {

    private fun clients() = client.postgrest.from("clients")

    override suspend fun getClients(userId: String): Result<List<Client>> = runCatching {
        clients()
            .select {
                filter { eq("user_id", userId) }
                order("name", Order.ASCENDING)
            }
            .decodeList<ClientDto>()
            .map { it.toDomain() }
    }

    override suspend fun saveClient(userId: String, input: ClientInput): Result<Client> = runCatching {
        // Matches the web app's client columns: name + nullable email/phone.
        val payload = buildJsonObject {
            if (input.id == null) put("user_id", userId)
            put("name", input.name)
            put("email", input.email)
            put("phone", input.phone)
        }
        val saved = if (input.id == null) {
            clients().insert(payload) { select() }.decodeSingle<ClientDto>()
        } else {
            clients().update(payload) {
                filter { eq("id", input.id); eq("user_id", userId) }
                select()
            }.decodeSingle<ClientDto>()
        }
        saved.toDomain()
    }

    override suspend fun deleteClient(userId: String, clientId: String): Result<Unit> = runCatching {
        clients().delete { filter { eq("id", clientId); eq("user_id", userId) } }
    }
}
