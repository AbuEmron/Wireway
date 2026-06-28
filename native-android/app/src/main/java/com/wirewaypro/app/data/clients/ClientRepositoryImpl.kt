package com.wirewaypro.app.data.clients

import com.wirewaypro.app.domain.model.Client
import com.wirewaypro.app.domain.repository.ClientRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
) : ClientRepository {

    override suspend fun getClients(userId: String): Result<List<Client>> = runCatching {
        client.postgrest.from("clients")
            .select {
                filter { eq("user_id", userId) }
                order("name", Order.ASCENDING)
            }
            .decodeList<ClientDto>()
            .map { it.toDomain() }
    }
}
