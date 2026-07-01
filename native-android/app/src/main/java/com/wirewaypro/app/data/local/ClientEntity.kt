package com.wirewaypro.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local offline-first copy of a `clients` row. [payloadJson] holds the full
 * ClientDto JSON; [name] is pulled out as a column for the name-sorted list.
 * Sync bookkeeping mirrors [QuoteEntity].
 */
@Entity(
    tableName = "clients",
    indices = [Index("userId"), Index("syncStatus")],
)
data class ClientEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String = "",
    val createdAt: String? = null,
    val payloadJson: String,
    val syncStatus: String = SyncStatus.SYNCED,
    val deleted: Boolean = false,
    val updatedAt: Long = 0L,
    val syncAttempts: Int = 0,
)
