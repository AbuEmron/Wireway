package com.wirewaypro.app.esign.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The e-signature module's own schema + migration, kept WITH the module (not
 * inlined into WirewayDatabase) so the module stays self-contained and extractable.
 * WirewayDatabase references [MIGRATION_8_9] and adds the two entities; DatabaseModule
 * registers the migration. The JVM migration test runs these exact `const` DDL
 * strings, so the migration and the test can never drift.
 *
 * v8 → v9 is purely ADDITIVE: it creates two brand-new tables and touches nothing
 * else, so no existing row can be lost on upgrade (the non-negotiable offline-first
 * rule). There is no destructive fallback except on downgrade (see DatabaseModule).
 */
object EsignSchema {

    const val VERSION_FROM = 8
    const val VERSION_TO = 9

    // Column list + types MUST match EsignRecordEntity exactly — Room validates the
    // schema on open and crashes on any drift (which is what we want).
    const val CREATE_RECORDS =
        "CREATE TABLE IF NOT EXISTS `esign_records` (" +
            "`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `quoteId` TEXT NOT NULL, " +
            "`documentTitle` TEXT NOT NULL, `signerName` TEXT NOT NULL, `signerEmail` TEXT, " +
            "`method` TEXT NOT NULL, `consentVersion` TEXT NOT NULL, " +
            "`consentGivenAt` INTEGER NOT NULL, `signedAt` INTEGER NOT NULL, " +
            "`contentSha256` TEXT NOT NULL, `sealedSha256` TEXT NOT NULL, " +
            "`sealedPdfPath` TEXT NOT NULL, `encryptedSignaturePath` TEXT, " +
            "`deviceModel` TEXT NOT NULL, `appVersion` TEXT NOT NULL, `ipAddress` TEXT, " +
            "`payloadJson` TEXT NOT NULL, `syncStatus` TEXT NOT NULL, `deleted` INTEGER NOT NULL, " +
            "`updatedAt` INTEGER NOT NULL, `syncAttempts` INTEGER NOT NULL, `createdAt` TEXT, " +
            "PRIMARY KEY(`id`))"
    const val INDEX_RECORDS_USER =
        "CREATE INDEX IF NOT EXISTS `index_esign_records_userId` ON `esign_records` (`userId`)"
    const val INDEX_RECORDS_SYNC =
        "CREATE INDEX IF NOT EXISTS `index_esign_records_syncStatus` ON `esign_records` (`syncStatus`)"
    const val INDEX_RECORDS_QUOTE =
        "CREATE INDEX IF NOT EXISTS `index_esign_records_quoteId` ON `esign_records` (`quoteId`)"

    // Column list + types MUST match EsignAuditEventEntity exactly.
    const val CREATE_AUDIT =
        "CREATE TABLE IF NOT EXISTS `esign_audit_events` (" +
            "`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `recordId` TEXT NOT NULL, " +
            "`eventType` TEXT NOT NULL, `atMillis` INTEGER NOT NULL, `consentVersion` TEXT NOT NULL, " +
            "`detailJson` TEXT, `syncStatus` TEXT NOT NULL, " +
            "`updatedAt` INTEGER NOT NULL, `syncAttempts` INTEGER NOT NULL, `createdAt` TEXT, " +
            "PRIMARY KEY(`id`))"
    const val INDEX_AUDIT_USER =
        "CREATE INDEX IF NOT EXISTS `index_esign_audit_events_userId` ON `esign_audit_events` (`userId`)"
    const val INDEX_AUDIT_SYNC =
        "CREATE INDEX IF NOT EXISTS `index_esign_audit_events_syncStatus` ON `esign_audit_events` (`syncStatus`)"
    const val INDEX_AUDIT_RECORD =
        "CREATE INDEX IF NOT EXISTS `index_esign_audit_events_recordId` ON `esign_audit_events` (`recordId`)"

    /** v8 → v9: add the e-signature records + append-only audit tables (additive). */
    val MIGRATION_8_9 = object : Migration(VERSION_FROM, VERSION_TO) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(CREATE_RECORDS)
            db.execSQL(INDEX_RECORDS_USER)
            db.execSQL(INDEX_RECORDS_SYNC)
            db.execSQL(INDEX_RECORDS_QUOTE)
            db.execSQL(CREATE_AUDIT)
            db.execSQL(INDEX_AUDIT_USER)
            db.execSQL(INDEX_AUDIT_SYNC)
            db.execSQL(INDEX_AUDIT_RECORD)
        }
    }
}
