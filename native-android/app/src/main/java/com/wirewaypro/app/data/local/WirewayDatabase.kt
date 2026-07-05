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
    entities = [
        QuoteEntity::class,
        QuoteDraftEntity::class,
        JobEntity::class,
        ClientEntity::class,
        JobDrawEntity::class,
        OverrideEntity::class,
        QuotePhotoEntity::class,
        UserAssemblyEntity::class,
        CrewMemberEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
abstract class WirewayDatabase : RoomDatabase() {
    abstract fun quoteDao(): QuoteDao
    abstract fun quoteDraftDao(): QuoteDraftDao
    abstract fun jobDao(): JobDao
    abstract fun clientDao(): ClientDao
    abstract fun jobDrawDao(): JobDrawDao
    abstract fun overrideDao(): OverrideDao
    abstract fun quotePhotoDao(): QuotePhotoDao
    abstract fun userAssemblyDao(): UserAssemblyDao
    abstract fun crewMemberDao(): CrewMemberDao

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

        /** v2 → v3: add jobs, clients, and job_draws offline tables (additive). */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `jobs` (" +
                        "`id` TEXT NOT NULL, `userId` TEXT NOT NULL, " +
                        "`scheduledDate` TEXT, `createdAt` TEXT, `payloadJson` TEXT NOT NULL, " +
                        "`syncStatus` TEXT NOT NULL, `deleted` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, `syncAttempts` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_jobs_userId` ON `jobs` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_jobs_syncStatus` ON `jobs` (`syncStatus`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `clients` (" +
                        "`id` TEXT NOT NULL, `userId` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, `createdAt` TEXT, `payloadJson` TEXT NOT NULL, " +
                        "`syncStatus` TEXT NOT NULL, `deleted` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, `syncAttempts` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_clients_userId` ON `clients` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_clients_syncStatus` ON `clients` (`syncStatus`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `job_draws` (" +
                        "`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `jobId` TEXT NOT NULL, " +
                        "`status` TEXT NOT NULL, `dueDate` TEXT, `sortOrder` INTEGER NOT NULL, " +
                        "`payloadJson` TEXT NOT NULL, `syncStatus` TEXT NOT NULL, " +
                        "`deleted` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                        "`syncAttempts` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_job_draws_jobId` ON `job_draws` (`jobId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_job_draws_userId` ON `job_draws` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_job_draws_syncStatus` ON `job_draws` (`syncStatus`)")
            }
        }

        /** v3 → v4: add the manual-override audit trail (additive). */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `quote_overrides` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`quoteId` TEXT NOT NULL, `field` TEXT NOT NULL, " +
                        "`original` REAL NOT NULL, `overridden` REAL NOT NULL, " +
                        "`atMillis` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_quote_overrides_quoteId` ON `quote_overrides` (`quoteId`)",
                )
            }
        }

        /** v4 → v5: add job-walk site photos (additive). */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `quote_photos` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`quoteId` TEXT NOT NULL, `path` TEXT NOT NULL, " +
                        "`atMillis` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_quote_photos_quoteId` ON `quote_photos` (`quoteId`)",
                )
            }
        }

        // v5 → v6: add contractor-authored job templates (additive). The exact
        // DDL lives in constants so a JVM migration test can exercise it without
        // an emulator (WirewayMigrationTest) — the migration and the test can
        // never drift because they run the same SQL.
        const val SQL_CREATE_USER_ASSEMBLIES =
            "CREATE TABLE IF NOT EXISTS `user_assemblies` (" +
                "`id` TEXT NOT NULL, `userId` TEXT NOT NULL, " +
                "`name` TEXT NOT NULL, `category` TEXT NOT NULL, " +
                "`payloadJson` TEXT NOT NULL, `syncStatus` TEXT NOT NULL, " +
                "`deleted` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                "`syncAttempts` INTEGER NOT NULL, `createdAt` TEXT, " +
                "PRIMARY KEY(`id`))"
        const val SQL_INDEX_USER_ASSEMBLIES_USER =
            "CREATE INDEX IF NOT EXISTS `index_user_assemblies_userId` ON `user_assemblies` (`userId`)"
        const val SQL_INDEX_USER_ASSEMBLIES_SYNC =
            "CREATE INDEX IF NOT EXISTS `index_user_assemblies_syncStatus` ON `user_assemblies` (`syncStatus`)"

        /** v5 → v6: add contractor-authored job templates (additive). */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(SQL_CREATE_USER_ASSEMBLIES)
                db.execSQL(SQL_INDEX_USER_ASSEMBLIES_USER)
                db.execSQL(SQL_INDEX_USER_ASSEMBLIES_SYNC)
            }
        }

        // ── v6 → v7: Elite crew roster (additive) ──────────────────────────────
        // The exact DDL is exposed as constants so a pure-JVM migration test can
        // run the identical SQL Room runs (see CrewMigrationTest). Column list +
        // types MUST stay in lock-step with [CrewMemberEntity] — Room validates
        // the schema on open and crashes on any drift (which is what we want).
        const val CREW_MEMBERS_CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS `crew_members` (" +
                "`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`role` TEXT, `hourlyCostRate` REAL NOT NULL, `active` INTEGER NOT NULL, " +
                "`createdAt` TEXT, `payloadJson` TEXT NOT NULL, `syncStatus` TEXT NOT NULL, " +
                "`deleted` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                "`syncAttempts` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        const val CREW_MEMBERS_INDEX_USER =
            "CREATE INDEX IF NOT EXISTS `index_crew_members_userId` ON `crew_members` (`userId`)"
        const val CREW_MEMBERS_INDEX_SYNC =
            "CREATE INDEX IF NOT EXISTS `index_crew_members_syncStatus` ON `crew_members` (`syncStatus`)"

        /** v6 → v7: add the Elite crew roster table (additive; no existing table touched). */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(CREW_MEMBERS_CREATE_TABLE)
                db.execSQL(CREW_MEMBERS_INDEX_USER)
                db.execSQL(CREW_MEMBERS_INDEX_SYNC)
            }
        }
    }
}
