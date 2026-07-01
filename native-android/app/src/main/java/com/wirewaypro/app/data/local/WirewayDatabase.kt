package com.wirewaypro.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The app's local database — the offline-first source of truth for core
 * entities. Phase 0 starts with quotes (estimates + invoices); jobs and clients
 * follow the same pattern in later commits.
 *
 * exportSchema is off for now: we ship destructive migrations during Phase 0
 * (the DB is a rebuildable cache of Supabase, so wiping it on upgrade is safe —
 * a refresh repopulates it, and unsynced writes are guarded before any bump).
 */
@Database(
    entities = [QuoteEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class WirewayDatabase : RoomDatabase() {
    abstract fun quoteDao(): QuoteDao

    companion object {
        const val NAME = "wireway.db"
    }
}
