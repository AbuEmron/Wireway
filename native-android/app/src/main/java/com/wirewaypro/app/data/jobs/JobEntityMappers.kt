package com.wirewaypro.app.data.jobs

import com.wirewaypro.app.data.local.JobEntity
import com.wirewaypro.app.data.local.SyncStatus
import com.wirewaypro.app.data.local.toSyncState
import com.wirewaypro.app.domain.model.Job
import kotlinx.serialization.json.Json

/**
 * Bridges the Supabase wire shape ([JobDto]) and the local [JobEntity], mirroring
 * the quotes mappers. [JobEntity.payloadJson] holds the full row JSON so the
 * domain [Job] rebuilds without a second fetch.
 */
private val entityJson = Json { ignoreUnknownKeys = true }

fun JobDto.toEntity(
    userId: String,
    updatedAt: Long,
    syncStatus: String = SyncStatus.SYNCED,
): JobEntity = JobEntity(
    id = id,
    userId = userId,
    scheduledDate = scheduledDate,
    createdAt = createdAt,
    payloadJson = entityJson.encodeToString(JobDto.serializer(), this),
    syncStatus = syncStatus,
    deleted = false,
    updatedAt = updatedAt,
)

fun JobEntity.toDomain(): Job =
    entityJson.decodeFromString(JobDto.serializer(), payloadJson).toDomain()
        .copy(syncState = syncStatus.toSyncState())
