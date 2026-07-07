package com.wirewaypro.app.data.crew

import com.wirewaypro.app.data.local.CrewMemberEntity
import com.wirewaypro.app.data.local.SyncStatus
import com.wirewaypro.app.data.local.toSyncState
import com.wirewaypro.app.domain.model.CrewMember
import kotlinx.serialization.json.Json

/**
 * Bridges the Supabase wire shape ([CrewMemberDto]) and the local
 * [CrewMemberEntity], mirroring the jobs mappers. [CrewMemberEntity.payloadJson]
 * holds the full row JSON so the domain [CrewMember] rebuilds without a re-fetch.
 */
private val entityJson = Json { ignoreUnknownKeys = true }

fun CrewMemberDto.toEntity(
    userId: String,
    updatedAt: Long,
    syncStatus: String = SyncStatus.SYNCED,
): CrewMemberEntity = CrewMemberEntity(
    id = id,
    userId = userId,
    name = name,
    role = role,
    hourlyCostRate = hourlyCostRate,
    active = active,
    createdAt = createdAt,
    payloadJson = entityJson.encodeToString(CrewMemberDto.serializer(), this),
    syncStatus = syncStatus,
    deleted = false,
    updatedAt = updatedAt,
)

fun CrewMemberEntity.toDomain(): CrewMember =
    entityJson.decodeFromString(CrewMemberDto.serializer(), payloadJson).toDomain()
        .copy(syncState = syncStatus.toSyncState())
