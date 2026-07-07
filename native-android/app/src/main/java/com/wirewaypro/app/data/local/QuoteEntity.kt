package com.wirewaypro.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local, offline-first copy of a `quotes` row (estimates + invoices share the
 * table; invoice = [invoiceMode] true). This is the SOURCE OF TRUTH the UI reads
 * and writes; the Supabase sync layer reconciles it in the background.
 *
 * The queryable summary columns ([quoteNumber]..[paidAt]) back fast list reads
 * with no JSON parsing. [payloadJson] holds the FULL Supabase row as JSON — it's
 * both what we reconstruct the detail screen from and the exact body we upsert
 * when pushing, so the local shape can never drift from the wire shape.
 *
 * Sync bookkeeping:
 *  - [syncStatus] — see [SyncStatus].
 *  - [deleted]    — tombstone: a delete made offline. The row stays until the
 *                   delete is pushed, then it's hard-removed. Hidden from lists.
 *  - [updatedAt]  — local last-edit time (epoch ms). The last-write-wins key:
 *                   a refresh from Supabase must never clobber a row whose local
 *                   edit is still [SyncStatus.PENDING].
 */
@Entity(
    tableName = "quotes",
    indices = [Index("userId"), Index("syncStatus")],
)
data class QuoteEntity(
    @PrimaryKey val id: String,
    val userId: String,

    // ── Summary columns (fast list reads) ───────────────────────────────────
    val quoteNumber: String? = null,
    val clientName: String? = null,
    val jobName: String? = null,
    val total: Double? = null,
    val status: String? = null,
    val createdAt: String? = null,          // server ISO timestamp; null until first sync
    val invoiceMode: Boolean = false,
    val invoiceDueDate: String? = null,
    val invoicePaid: Boolean = false,
    val paidAt: String? = null,

    // ── Full row JSON (detail reconstruction + push payload) ─────────────────
    val payloadJson: String,

    // ── Sync bookkeeping ─────────────────────────────────────────────────────
    @ColumnInfo(defaultValue = SyncStatus.SYNCED) val syncStatus: String = SyncStatus.SYNCED,
    @ColumnInfo(defaultValue = "0") val deleted: Boolean = false,
    val updatedAt: Long = 0L,
    @ColumnInfo(defaultValue = "0") val syncAttempts: Int = 0,
)
