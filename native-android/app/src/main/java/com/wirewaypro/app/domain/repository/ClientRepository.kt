package com.wirewaypro.app.domain.repository

import com.wirewaypro.app.domain.model.Client

interface ClientRepository {
    /** The user's clients, ordered by name. */
    suspend fun getClients(userId: String): Result<List<Client>>
}
