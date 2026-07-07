package com.wirewaypro.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local offline-first copy of a `user_jurisdictions` row — the user's selected
 * AHJ. Same shape/discipline as [CrewMemberEntity]: [payloadJson] holds the full
 * JurisdictionDto JSON (the exact upsert body the domain model rebuilds from);
 * [stateCode]/[county]/[city] are surfaced as columns for querying/display. Sync
 * bookkeeping mirrors the other entities ([syncStatus], [deleted] tombstone,
 * [updatedAt] = last-write-wins key).
 *
 * One active jurisdiction per user, but modeled as a normal row (not a singleton)
 * so it round-trips through the identical LWW sync path as jobs/clients/crew.
 *
 * The migration DDL that creates this table lives in
 * [WirewayDatabase.JURISDICTIONS_CREATE_TABLE] and must stay column-for-column in
 * sync with this entity (Room validates it on open).
 */
@Entity(
    tableName = "user_jurisdictions",
    indices = [Index("userId"), Index("syncStatus")],
)
data class JurisdictionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val stateCode: String,
    val county: String? = null,
    val city: String? = null,
    val source: String = "manual",
    val createdAt: String? = null,
    val payloadJson: String,
    val syncStatus: String = SyncStatus.SYNCED,
    val deleted: Boolean = false,
    val updatedAt: Long = 0L,
    val syncAttempts: Int = 0,
)
