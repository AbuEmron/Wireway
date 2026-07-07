package com.wirewaypro.app.esign.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.Connection
import java.sql.DriverManager

/**
 * Migration test for Room v8 → v9 (the e-signature `esign_records` +
 * `esign_audit_events` tables).
 *
 * Runs the EXACT DDL constants that [EsignSchema.MIGRATION_8_9] executes (they're
 * referenced directly, so the test can never drift from the real migration) against
 * a pure-JVM in-memory SQLite — no emulator or Robolectric. It proves the migration
 * is:
 *  - additive (rows written before the migration survive it — never lose data),
 *  - correctly shaped (columns/types/PK match the entities, which is what Room
 *    validates on open), and
 *  - idempotent (safe to re-run, matching `CREATE TABLE IF NOT EXISTS`).
 */
class EsignMigrationTest {

    private fun memDb(): Connection = DriverManager.getConnection("jdbc:sqlite::memory:")

    /** Applies the v8→v9 migration statements exactly as Room does. */
    private fun applyMigration8to9(c: Connection) {
        c.createStatement().use { st ->
            st.execute(EsignSchema.CREATE_RECORDS)
            st.execute(EsignSchema.INDEX_RECORDS_USER)
            st.execute(EsignSchema.INDEX_RECORDS_SYNC)
            st.execute(EsignSchema.INDEX_RECORDS_QUOTE)
            st.execute(EsignSchema.CREATE_AUDIT)
            st.execute(EsignSchema.INDEX_AUDIT_USER)
            st.execute(EsignSchema.INDEX_AUDIT_SYNC)
            st.execute(EsignSchema.INDEX_AUDIT_RECORD)
        }
    }

    private data class Col(val type: String, val notNull: Boolean, val pk: Boolean)

    private fun schemaOf(c: Connection, table: String): Map<String, Col> {
        val cols = mutableMapOf<String, Col>()
        c.createStatement().use { st ->
            st.executeQuery("PRAGMA table_info(`$table`)").use { rs ->
                while (rs.next()) {
                    cols[rs.getString("name")] = Col(
                        type = rs.getString("type").uppercase(),
                        notNull = rs.getInt("notnull") == 1,
                        pk = rs.getInt("pk") == 1,
                    )
                }
            }
        }
        return cols
    }

    @Test
    fun migration_isAdditive_existingRowsSurvive() {
        memDb().use { c ->
            // A pre-existing table (e.g. crew_members from v7) with a row that must survive.
            c.createStatement().use { st ->
                st.execute("CREATE TABLE `crew_members` (`id` TEXT NOT NULL, `payloadJson` TEXT NOT NULL, PRIMARY KEY(`id`))")
                st.execute("INSERT INTO `crew_members` (`id`, `payloadJson`) VALUES ('c-1', '{}')")
            }

            applyMigration8to9(c)

            c.createStatement().use { st ->
                st.executeQuery("SELECT COUNT(*) FROM `crew_members`").use { rs ->
                    rs.next()
                    assertEquals("existing rows must survive the additive migration", 1, rs.getInt(1))
                }
            }
            // Both new tables now exist.
            assertTrue(schemaOf(c, "esign_records").isNotEmpty())
            assertTrue(schemaOf(c, "esign_audit_events").isNotEmpty())
        }
    }

    @Test
    fun esignRecords_hasExactRoomSchema() {
        memDb().use { c ->
            applyMigration8to9(c)
            val cols = schemaOf(c, "esign_records")
            val expected = mapOf(
                "id" to Col("TEXT", true, true),
                "userId" to Col("TEXT", true, false),
                "quoteId" to Col("TEXT", true, false),
                "documentTitle" to Col("TEXT", true, false),
                "signerName" to Col("TEXT", true, false),
                "signerEmail" to Col("TEXT", false, false),
                "method" to Col("TEXT", true, false),
                "consentVersion" to Col("TEXT", true, false),
                "consentGivenAt" to Col("INTEGER", true, false),
                "signedAt" to Col("INTEGER", true, false),
                "contentSha256" to Col("TEXT", true, false),
                "sealedSha256" to Col("TEXT", true, false),
                "sealedPdfPath" to Col("TEXT", true, false),
                "encryptedSignaturePath" to Col("TEXT", false, false),
                "deviceModel" to Col("TEXT", true, false),
                "appVersion" to Col("TEXT", true, false),
                "ipAddress" to Col("TEXT", false, false),
                "payloadJson" to Col("TEXT", true, false),
                "syncStatus" to Col("TEXT", true, false),
                "deleted" to Col("INTEGER", true, false),
                "updatedAt" to Col("INTEGER", true, false),
                "syncAttempts" to Col("INTEGER", true, false),
                "createdAt" to Col("TEXT", false, false),
            )
            assertEquals("esign_records column set", expected.keys, cols.keys)
            expected.forEach { (name, col) -> assertEquals("column $name", col, cols[name]) }
        }
    }

    @Test
    fun esignAuditEvents_hasExactRoomSchema() {
        memDb().use { c ->
            applyMigration8to9(c)
            val cols = schemaOf(c, "esign_audit_events")
            val expected = mapOf(
                "id" to Col("TEXT", true, true),
                "userId" to Col("TEXT", true, false),
                "recordId" to Col("TEXT", true, false),
                "eventType" to Col("TEXT", true, false),
                "atMillis" to Col("INTEGER", true, false),
                "consentVersion" to Col("TEXT", true, false),
                "detailJson" to Col("TEXT", false, false),
                "syncStatus" to Col("TEXT", true, false),
                "updatedAt" to Col("INTEGER", true, false),
                "syncAttempts" to Col("INTEGER", true, false),
                "createdAt" to Col("TEXT", false, false),
            )
            assertEquals("esign_audit_events column set", expected.keys, cols.keys)
            expected.forEach { (name, col) -> assertEquals("column $name", col, cols[name]) }
        }
    }

    @Test
    fun migration_isIdempotent_andTablesAreWritable() {
        memDb().use { c ->
            applyMigration8to9(c)
            applyMigration8to9(c) // re-run must be safe (CREATE TABLE IF NOT EXISTS)

            // A record row inserts and reads back — proves the table is usable.
            c.createStatement().use { st ->
                st.execute(
                    "INSERT INTO `esign_records` (`id`,`userId`,`quoteId`,`documentTitle`,`signerName`," +
                        "`method`,`consentVersion`,`consentGivenAt`,`signedAt`,`contentSha256`,`sealedSha256`," +
                        "`sealedPdfPath`,`deviceModel`,`appVersion`,`payloadJson`,`syncStatus`,`deleted`," +
                        "`updatedAt`,`syncAttempts`) VALUES " +
                        "('r-1','u-1','q-1','Proposal #1','Jane Client','typed','2026-07-06.1',1,2," +
                        "'aa','bb','/x.pdf','Pixel','1.1.1','{}','pending',0,10,0)",
                )
                st.executeQuery("SELECT signerName FROM `esign_records` WHERE id='r-1'").use { rs ->
                    rs.next()
                    assertEquals("Jane Client", rs.getString(1))
                }
            }
        }
    }
}
