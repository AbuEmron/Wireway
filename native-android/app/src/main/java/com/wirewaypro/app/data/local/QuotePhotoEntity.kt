package com.wirewaypro.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A job-walk site photo attached to a quote — stored on-device (downscaled
 * JPEG under filesDir/quote-photos/<quoteId>/) as bid/crew reference. Local
 * only for now: `quotes` has no attachments column, so nothing syncs.
 */
@Entity(tableName = "quote_photos", indices = [Index("quoteId")])
data class QuotePhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val quoteId: String,
    /** Absolute path of the stored JPEG. */
    val path: String,
    val atMillis: Long,
)
