package com.wirewaypro.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local offline-first copy of a `user_assemblies` row — a contractor-authored
 * job template. Same shape as [JobEntity]: [payloadJson] holds the full template
 * definition JSON (the list of catalog line items + metadata, and the exact
 * upsert body), while [name]/[category] are pulled out as columns for listing.
 * Sync bookkeeping mirrors the other entities ([syncStatus], [deleted] tombstone,
 * [updatedAt] last-write-wins key) so a template the user built offline is never
 * lost across an app upgrade or before it syncs.
 */
@Entity(
    tableName = "user_assemblies",
    indices = [Index("userId"), Index("syncStatus")],
)
data class UserAssemblyEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val category: String,
    val payloadJson: String,
    val syncStatus: String = SyncStatus.SYNCED,
    val deleted: Boolean = false,
    val updatedAt: Long = 0L,
    val syncAttempts: Int = 0,
    val createdAt: String? = null,
)
