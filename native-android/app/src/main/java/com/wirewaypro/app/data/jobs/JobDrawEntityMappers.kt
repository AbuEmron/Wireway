package com.wirewaypro.app.data.jobs

import com.wirewaypro.app.data.local.JobDrawEntity
import com.wirewaypro.app.data.local.SyncStatus
import com.wirewaypro.app.domain.model.JobDraw
import kotlinx.serialization.json.Json

/**
 * Bridges the Supabase wire shape ([JobDrawDto]) and the local [JobDrawEntity].
 * user_id / job_id come from the row itself when refreshed from the server, or
 * from [fallbackUserId] / [fallbackJobId] for a locally-built payload.
 */
private val entityJson = Json { ignoreUnknownKeys = true }

fun JobDrawDto.toEntity(
    fallbackUserId: String? = null,
    fallbackJobId: String? = null,
    updatedAt: Long,
    syncStatus: String = SyncStatus.SYNCED,
): JobDrawEntity = JobDrawEntity(
    id = id,
    userId = userId ?: fallbackUserId.orEmpty(),
    jobId = jobId ?: fallbackJobId.orEmpty(),
    status = status,
    dueDate = dueDate,
    sortOrder = sortOrder,
    payloadJson = entityJson.encodeToString(JobDrawDto.serializer(), this),
    syncStatus = syncStatus,
    deleted = false,
    updatedAt = updatedAt,
)

fun JobDrawEntity.toDomain(): JobDraw =
    entityJson.decodeFromString(JobDrawDto.serializer(), payloadJson).toDomain()
