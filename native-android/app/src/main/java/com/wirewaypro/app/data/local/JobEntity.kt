package com.wirewaypro.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local offline-first copy of a `jobs` row. Same shape as [QuoteEntity]:
 * [payloadJson] holds the full JobDto JSON (reconstructs the domain Job and is
 * the exact upsert body), while [scheduledDate]/[createdAt] are pulled out as
 * columns purely for list ordering. Sync bookkeeping mirrors [QuoteEntity]
 * ([syncStatus], [deleted] tombstone, [updatedAt] = last-write-wins key).
 */
@Entity(
    tableName = "jobs",
    indices = [Index("userId"), Index("syncStatus")],
)
data class JobEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val scheduledDate: String? = null,
    val createdAt: String? = null,
    val payloadJson: String,
    val syncStatus: String = SyncStatus.SYNCED,
    val deleted: Boolean = false,
    val updatedAt: Long = 0L,
    val syncAttempts: Int = 0,
)
