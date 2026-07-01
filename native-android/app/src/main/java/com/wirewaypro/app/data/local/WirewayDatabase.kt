package com.wirewaypro.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The app's local database — the offline-first source of truth for core
 * entities. Phase 0 starts with quotes (estimates + invoices) plus autosaved
 * quote drafts; jobs and clients follow the same pattern in later commits.
 *
 * exportSchema is off, but we ship REAL migrations (not destructive) so an
 * upgrade never drops rows — a quote still waiting to sync must survive an app
 * update. [fallbackToDestructiveMigration] stays only as a last-resort backstop.
 */
@Database(
    entities = [QuoteEntity::class, QuoteDraftEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class WirewayDatabase : RoomDatabase() {
    abstract fun quoteDao(): QuoteDao
    abstract fun quoteDraftDao(): QuoteDraftDao

    companion object {
        const val NAME = "wireway.db"

        /** v1 → v2: add the autosaved quote-drafts table (quotes are untouched). */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `quote_drafts` (" +
                        "`draftKey` TEXT NOT NULL, " +
                        "`contentJson` TEXT NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`draftKey`))",
                )
            }
        }
    }
}
