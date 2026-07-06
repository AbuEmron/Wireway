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
 * exportSchema is off, but we ship REAL additive migrations (never destructive)
 * so an upgrade never drops rows — a quote still waiting to sync must survive an
 * app update. There is NO unconditional destructive fallback (see DatabaseModule);
 * only a downgrade resets the cache. Every migration's DDL is exposed as `const`
 * so the JVM migration tests run the exact shipping SQL.
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
        JurisdictionEntity::class,
        com.wirewaypro.app.esign.data.EsignRecordEntity::class,
        com.wirewaypro.app.esign.data.EsignAuditEventEntity::class,
    ],
    version = 9,
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
    abstract fun jurisdictionDao(): JurisdictionDao
    abstract fun esignRecordDao(): com.wirewaypro.app.esign.data.EsignRecordDao
    abstract fun esignAuditEventDao(): com.wirewaypro.app.esign.data.EsignAuditEventDao

    companion object {
        const val NAME = "wireway.db"

        // Every migration's DDL lives in `const val` constants so the JVM
        // migration tests can run the EXACT SQL Room runs (no emulator, no drift).
        // See WirewayMigrationTest (full v1→current chain) + CrewMigrationTest.
        const val SQL_CREATE_QUOTE_DRAFTS =
            "CREATE TABLE IF NOT EXISTS `quote_drafts` (" +
                "`draftKey` TEXT NOT NULL, `contentJson` TEXT NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, PRIMARY KEY(`draftKey`))"

        /** v1 → v2: add the autosaved quote-drafts table (quotes are untouched). */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(SQL_CREATE_QUOTE_DRAFTS)
            }
        }

        const val SQL_CREATE_JOBS =
            "CREATE TABLE IF NOT EXISTS `jobs` (" +
                "`id` TEXT NOT NULL, `userId` TEXT NOT NULL, " +
                "`scheduledDate` TEXT, `createdAt` TEXT, `payloadJson` TEXT NOT NULL, " +
                "`syncStatus` TEXT NOT NULL, `deleted` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, `syncAttempts` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))"
        const val SQL_INDEX_JOBS_USER = "CREATE INDEX IF NOT EXISTS `index_jobs_userId` ON `jobs` (`userId`)"
        const val SQL_INDEX_JOBS_SYNC = "CREATE INDEX IF NOT EXISTS `index_jobs_syncStatus` ON `jobs` (`syncStatus`)"
        const val SQL_CREATE_CLIENTS =
            "CREATE TABLE IF NOT EXISTS `clients` (" +
                "`id` TEXT NOT NULL, `userId` TEXT NOT NULL, " +
                "`name` TEXT NOT NULL, `createdAt` TEXT, `payloadJson` TEXT NOT NULL, " +
                "`syncStatus` TEXT NOT NULL, `deleted` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, `syncAttempts` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))"
        const val SQL_INDEX_CLIENTS_USER = "CREATE INDEX IF NOT EXISTS `index_clients_userId` ON `clients` (`userId`)"
        const val SQL_INDEX_CLIENTS_SYNC = "CREATE INDEX IF NOT EXISTS `index_clients_syncStatus` ON `clients` (`syncStatus`)"
        const val SQL_CREATE_JOB_DRAWS =
            "CREATE TABLE IF NOT EXISTS `job_draws` (" +
                "`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `jobId` TEXT NOT NULL, " +
                "`status` TEXT NOT NULL, `dueDate` TEXT, `sortOrder` INTEGER NOT NULL, " +
                "`payloadJson` TEXT NOT NULL, `syncStatus` TEXT NOT NULL, " +
                "`deleted` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                "`syncAttempts` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        const val SQL_INDEX_JOB_DRAWS_JOB = "CREATE INDEX IF NOT EXISTS `index_job_draws_jobId` ON `job_draws` (`jobId`)"
        const val SQL_INDEX_JOB_DRAWS_USER = "CREATE INDEX IF NOT EXISTS `index_job_draws_userId` ON `job_draws` (`userId`)"
        const val SQL_INDEX_JOB_DRAWS_SYNC = "CREATE INDEX IF NOT EXISTS `index_job_draws_syncStatus` ON `job_draws` (`syncStatus`)"

        /** v2 → v3: add jobs, clients, and job_draws offline tables (additive). */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(SQL_CREATE_JOBS)
                db.execSQL(SQL_INDEX_JOBS_USER)
                db.execSQL(SQL_INDEX_JOBS_SYNC)
                db.execSQL(SQL_CREATE_CLIENTS)
                db.execSQL(SQL_INDEX_CLIENTS_USER)
                db.execSQL(SQL_INDEX_CLIENTS_SYNC)
                db.execSQL(SQL_CREATE_JOB_DRAWS)
                db.execSQL(SQL_INDEX_JOB_DRAWS_JOB)
                db.execSQL(SQL_INDEX_JOB_DRAWS_USER)
                db.execSQL(SQL_INDEX_JOB_DRAWS_SYNC)
            }
        }

        const val SQL_CREATE_QUOTE_OVERRIDES =
            "CREATE TABLE IF NOT EXISTS `quote_overrides` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`quoteId` TEXT NOT NULL, `field` TEXT NOT NULL, " +
                "`original` REAL NOT NULL, `overridden` REAL NOT NULL, " +
                "`atMillis` INTEGER NOT NULL)"
        const val SQL_INDEX_QUOTE_OVERRIDES_QUOTE =
            "CREATE INDEX IF NOT EXISTS `index_quote_overrides_quoteId` ON `quote_overrides` (`quoteId`)"

        /** v3 → v4: add the manual-override audit trail (additive). */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(SQL_CREATE_QUOTE_OVERRIDES)
                db.execSQL(SQL_INDEX_QUOTE_OVERRIDES_QUOTE)
            }
        }

        const val SQL_CREATE_QUOTE_PHOTOS =
            "CREATE TABLE IF NOT EXISTS `quote_photos` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`quoteId` TEXT NOT NULL, `path` TEXT NOT NULL, " +
                "`atMillis` INTEGER NOT NULL)"
        const val SQL_INDEX_QUOTE_PHOTOS_QUOTE =
            "CREATE INDEX IF NOT EXISTS `index_quote_photos_quoteId` ON `quote_photos` (`quoteId`)"

        /** v4 → v5: add job-walk site photos (additive). */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(SQL_CREATE_QUOTE_PHOTOS)
                db.execSQL(SQL_INDEX_QUOTE_PHOTOS_QUOTE)
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

        // ── v7 → v8: AHJ jurisdiction (additive) ───────────────────────────────
        // The user's selected Authority Having Jurisdiction. The exact DDL is
        // exposed as constants so a pure-JVM migration test runs the identical SQL
        // Room runs (see JurisdictionMigrationTest). Column list + types MUST stay
        // in lock-step with [JurisdictionEntity] — Room validates the schema on
        // open and crashes on any drift (which is what we want).
        const val JURISDICTIONS_CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS `user_jurisdictions` (" +
                "`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `stateCode` TEXT NOT NULL, " +
                "`county` TEXT, `city` TEXT, `source` TEXT NOT NULL, `createdAt` TEXT, " +
                "`payloadJson` TEXT NOT NULL, `syncStatus` TEXT NOT NULL, " +
                "`deleted` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                "`syncAttempts` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        const val JURISDICTIONS_INDEX_USER =
            "CREATE INDEX IF NOT EXISTS `index_user_jurisdictions_userId` ON `user_jurisdictions` (`userId`)"
        const val JURISDICTIONS_INDEX_SYNC =
            "CREATE INDEX IF NOT EXISTS `index_user_jurisdictions_syncStatus` ON `user_jurisdictions` (`syncStatus`)"

        /** v7 → v8: add the AHJ jurisdiction table (additive; no existing table touched). */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(JURISDICTIONS_CREATE_TABLE)
                db.execSQL(JURISDICTIONS_INDEX_USER)
                db.execSQL(JURISDICTIONS_INDEX_SYNC)
            }
        }
    }
}
