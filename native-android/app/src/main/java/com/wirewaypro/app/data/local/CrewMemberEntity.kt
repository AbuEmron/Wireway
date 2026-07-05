package com.wirewaypro.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local offline-first copy of a `crew_members` row. Same shape as [JobEntity]:
 * [payloadJson] holds the full CrewMemberDto JSON (the exact upsert body and the
 * source the domain model rebuilds from), while [name]/[role]/[hourlyCostRate]/
 * [active] are pulled out as columns purely for list ordering and pickers. Sync
 * bookkeeping mirrors [JobEntity] ([syncStatus], [deleted] tombstone,
 * [updatedAt] = last-write-wins key).
 *
 * The migration DDL that creates this table lives in
 * [WirewayDatabase.CREW_MEMBERS_CREATE_TABLE] and must stay column-for-column in
 * sync with this entity (Room validates it on open).
 */
@Entity(
    tableName = "crew_members",
    indices = [Index("userId"), Index("syncStatus")],
)
data class CrewMemberEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val role: String? = null,
    val hourlyCostRate: Double,
    val active: Boolean = true,
    val createdAt: String? = null,
    val payloadJson: String,
    val syncStatus: String = SyncStatus.SYNCED,
    val deleted: Boolean = false,
    val updatedAt: Long = 0L,
    val syncAttempts: Int = 0,
)
