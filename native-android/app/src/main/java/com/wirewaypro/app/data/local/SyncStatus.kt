package com.wirewaypro.app.data.local

/**
 * Per-row sync state for offline-first entities. Stored as a plain String column
 * so it survives Room schema evolution without an enum TypeConverter.
 *
 *  - [SYNCED]  — the local row matches what's on Supabase (nothing to push).
 *  - [PENDING] — created/edited/deleted locally, waiting to push when online.
 *  - [ERROR]   — the server rejected the push (bad data / auth); kept locally so
 *                the user's edit is never silently lost, surfaced in the UI.
 */
object SyncStatus {
    const val SYNCED = "synced"
    const val PENDING = "pending"
    const val ERROR = "error"
}
