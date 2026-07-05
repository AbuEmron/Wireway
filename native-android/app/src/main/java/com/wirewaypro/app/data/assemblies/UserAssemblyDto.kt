package com.wirewaypro.app.data.assemblies

import com.wirewaypro.app.data.local.SyncStatus
import com.wirewaypro.app.data.local.UserAssemblyEntity
import com.wirewaypro.app.data.local.toSyncState
import com.wirewaypro.app.domain.catalog.AssemblyItem
import com.wirewaypro.app.domain.model.UserTemplate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Wire + storage shape of a `user_assemblies` row. This is BOTH the Supabase
 * row shape and the exact JSON stored in [UserAssemblyEntity.payloadJson], so a
 * template round-trips losslessly between Room and the server. snake_case
 * matches the Postgres columns.
 */
@Serializable
data class UserAssemblyDto(
    val id: String,
    val user_id: String? = null,
    val name: String,
    val description: String = "",
    val category: String = "Custom",
    val items: List<UserAssemblyItemDto> = emptyList(),
    val created_at: String? = null,
    val updated_at: String? = null,
) {
    fun toDomain(syncState: com.wirewaypro.app.domain.model.SyncState): UserTemplate =
        UserTemplate(
            id = id,
            name = name,
            description = description,
            category = category,
            items = items.map { AssemblyItem(it.service_id, it.qty, it.variant_idx) },
            syncState = syncState,
        )
}

@Serializable
data class UserAssemblyItemDto(
    val service_id: String,
    val qty: Double = 1.0,
    val variant_idx: Int = 0,
)

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

fun UserTemplate.toDto(userId: String, createdAt: String? = null): UserAssemblyDto =
    UserAssemblyDto(
        id = id,
        user_id = userId,
        name = name,
        description = description,
        category = category,
        items = items.map { UserAssemblyItemDto(it.serviceId, it.qty, it.variantIdx) },
        created_at = createdAt,
    )

fun UserAssemblyDto.encode(): String = json.encodeToString(UserAssemblyDto.serializer(), this)

fun decodeUserAssembly(payload: String): UserAssemblyDto =
    json.decodeFromString(UserAssemblyDto.serializer(), payload)

fun UserAssemblyDto.toEntity(
    userId: String,
    syncStatus: String = SyncStatus.SYNCED,
    updatedAt: Long,
    createdAt: String? = this.created_at,
): UserAssemblyEntity = UserAssemblyEntity(
    id = id,
    userId = userId,
    name = name,
    category = category,
    payloadJson = copy(user_id = userId).encode(),
    syncStatus = syncStatus,
    deleted = false,
    updatedAt = updatedAt,
    createdAt = createdAt,
)

fun UserAssemblyEntity.toDomain(): UserTemplate =
    decodeUserAssembly(payloadJson).toDomain(syncStatus.toSyncState())
