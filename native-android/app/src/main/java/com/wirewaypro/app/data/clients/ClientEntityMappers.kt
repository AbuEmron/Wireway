package com.wirewaypro.app.data.clients

import com.wirewaypro.app.data.local.ClientEntity
import com.wirewaypro.app.data.local.SyncStatus
import com.wirewaypro.app.data.local.toSyncState
import com.wirewaypro.app.domain.model.Client
import kotlinx.serialization.json.Json

/**
 * Bridges the Supabase wire shape ([ClientDto]) and the local [ClientEntity],
 * mirroring the quotes/jobs mappers. [ClientEntity.payloadJson] holds the full
 * row JSON so the domain [Client] rebuilds without a second fetch.
 */
private val entityJson = Json { ignoreUnknownKeys = true }

fun ClientDto.toEntity(
    userId: String,
    updatedAt: Long,
    syncStatus: String = SyncStatus.SYNCED,
): ClientEntity = ClientEntity(
    id = id,
    userId = userId,
    name = name,
    createdAt = createdAt,
    payloadJson = entityJson.encodeToString(ClientDto.serializer(), this),
    syncStatus = syncStatus,
    deleted = false,
    updatedAt = updatedAt,
)

fun ClientEntity.toDomain(): Client =
    entityJson.decodeFromString(ClientDto.serializer(), payloadJson).toDomain()
        .copy(syncState = syncStatus.toSyncState())
