package com.wirewaypro.app.domain.repository

import com.wirewaypro.app.domain.model.Client
import com.wirewaypro.app.domain.model.ClientInput

interface ClientRepository {
    /** The user's clients, ordered by name. */
    suspend fun getClients(userId: String): Result<List<Client>>

    /** Creates (id == null) or updates a client. Returns the saved record. */
    suspend fun saveClient(userId: String, input: ClientInput): Result<Client>

    /** Deletes a client. */
    suspend fun deleteClient(userId: String, clientId: String): Result<Unit>
}
