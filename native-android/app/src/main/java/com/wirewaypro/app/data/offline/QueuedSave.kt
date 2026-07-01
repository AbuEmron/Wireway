package com.wirewaypro.app.data.offline

import kotlinx.serialization.Serializable

/**
 * A pending write held locally while offline, mirroring the web app's quote save
 * queue (src/lib/supabase.js). [payload] is the exact JSON row to replay; [mode]
 * is "upsert" (idempotent on id, used for quotes) or "insert" (expenses).
 */
@Serializable
data class QueuedSave(
    val id: String,        // stable row id (so replay is idempotent)
    val table: String,     // "quotes" | "expenses"
    val mode: String,      // "upsert" | "insert"
    val payload: String,   // serialized JSON object of the row
    val userId: String,
    val createdAt: Long,
    val attempts: Int = 0,
)
