package com.wirewaypro.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One manual override of a calculated/derived number on a quote — the audit
 * trail behind "here's what I changed and why it's still defensible". Local
 * Room storage (offline-first source of truth); rows ride with the device.
 */
@Entity(tableName = "quote_overrides", indices = [Index("quoteId")])
data class OverrideEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val quoteId: String,
    /** Human label of the value, e.g. "Hourly rate", "Labor hrs — Install VFD". */
    val field: String,
    val original: Double,
    val overridden: Double,
    val atMillis: Long,
)
