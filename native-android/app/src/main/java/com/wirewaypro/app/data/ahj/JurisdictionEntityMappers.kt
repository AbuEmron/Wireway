package com.wirewaypro.app.data.ahj

import com.wirewaypro.app.data.local.JurisdictionEntity
import com.wirewaypro.app.data.local.SyncStatus
import com.wirewaypro.app.data.local.toSyncState
import com.wirewaypro.app.domain.model.Jurisdiction
import kotlinx.serialization.json.Json

/**
 * Bridges the Supabase wire shape ([JurisdictionDto]) and the local
 * [JurisdictionEntity], mirroring the crew mappers. [JurisdictionEntity.payloadJson]
 * holds the full row JSON so the domain [Jurisdiction] rebuilds without a re-fetch.
 */
private val entityJson = Json { ignoreUnknownKeys = true }

fun JurisdictionDto.toEntity(
    userId: String,
    updatedAt: Long,
    syncStatus: String = SyncStatus.SYNCED,
): JurisdictionEntity = JurisdictionEntity(
    id = id,
    userId = userId,
    stateCode = stateCode,
    county = county,
    city = city,
    source = source,
    createdAt = createdAt,
    payloadJson = entityJson.encodeToString(JurisdictionDto.serializer(), this),
    syncStatus = syncStatus,
    deleted = false,
    updatedAt = updatedAt,
)

fun JurisdictionEntity.toDomain(): Jurisdiction =
    entityJson.decodeFromString(JurisdictionDto.serializer(), payloadJson).toDomain()
        .copy(syncState = syncStatus.toSyncState())
