package com.wirewaypro.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.Connection
import java.sql.DriverManager

/**
 * Migration test for Room v7 → v8 (the AHJ `user_jurisdictions` table).
 *
 * Runs the EXACT DDL constants that [WirewayDatabase.MIGRATION_7_8] executes
 * (referenced directly, so the test can never drift from the real migration)
 * against a pure-JVM in-memory SQLite — no emulator or Robolectric. It proves the
 * migration is:
 *  - additive (a row written before the migration survives it — never lose data),
 *  - correctly shaped (columns/types/PK match [JurisdictionEntity], which is what
 *    Room validates on open), and
 *  - idempotent (safe to re-run, matching `CREATE TABLE IF NOT EXISTS`).
 */
class JurisdictionMigrationTest {

    private fun memDb(): Connection = DriverManager.getConnection("jdbc:sqlite::memory:")

    /** Applies the v7→v8 migration statements exactly as Room does. */
    private fun applyMigration7to8(c: Connection) {
        c.createStatement().use { st ->
            st.execute(WirewayDatabase.JURISDICTIONS_CREATE_TABLE)
            st.execute(WirewayDatabase.JURISDICTIONS_INDEX_USER)
            st.execute(WirewayDatabase.JURISDICTIONS_INDEX_SYNC)
        }
    }

    @Test
    fun migration_addsJurisdictionTable_withoutTouchingExistingData() {
        memDb().use { c ->
            // A pre-existing v7 table (crew_members) with a row that MUST survive.
            c.createStatement().use { st ->
                st.execute(WirewayDatabase.CREW_MEMBERS_CREATE_TABLE)
                st.execute(
                    "INSERT INTO `crew_members` (`id`,`userId`,`name`,`hourlyCostRate`,`active`," +
                        "`payloadJson`,`syncStatus`,`deleted`,`updatedAt`,`syncAttempts`) " +
                        "VALUES ('c1','u1','Sam',48.5,1,'{}','synced',0,10,0)",
                )
            }

            applyMigration7to8(c)

            // Existing data is untouched (additive migration).
            c.createStatement().use { st ->
                st.executeQuery("SELECT COUNT(*) FROM `crew_members`").use { rs ->
                    rs.next()
                    assertEquals("existing crew row must survive the migration", 1, rs.getInt(1))
                }
            }
        }
    }

    @Test
    fun jurisdictionTable_hasExactRoomSchema() {
        memDb().use { c ->
            applyMigration7to8(c)

            data class Col(val type: String, val notNull: Boolean, val pk: Boolean)
            val cols = mutableMapOf<String, Col>()
            c.createStatement().use { st ->
                st.executeQuery("PRAGMA table_info(`user_jurisdictions`)").use { rs ->
                    while (rs.next()) {
                        cols[rs.getString("name")] = Col(
                            type = rs.getString("type").uppercase(),
                            notNull = rs.getInt("notnull") == 1,
                            pk = rs.getInt("pk") == 1,
                        )
                    }
                }
            }

            // Must match JurisdictionEntity exactly — this is what Room checks on open.
            val expected = mapOf(
                "id" to Col("TEXT", true, true),
                "userId" to Col("TEXT", true, false),
                "stateCode" to Col("TEXT", true, false),
                "county" to Col("TEXT", false, false),
                "city" to Col("TEXT", false, false),
                "source" to Col("TEXT", true, false),
                "createdAt" to Col("TEXT", false, false),
                "payloadJson" to Col("TEXT", true, false),
                "syncStatus" to Col("TEXT", true, false),
                "deleted" to Col("INTEGER", true, false),
                "updatedAt" to Col("INTEGER", true, false),
                "syncAttempts" to Col("INTEGER", true, false),
            )
            assertEquals("user_jurisdictions column set", expected.keys, cols.keys)
            expected.forEach { (name, col) ->
                assertEquals("$name type", col.type, cols[name]!!.type)
                assertEquals("$name notNull", col.notNull, cols[name]!!.notNull)
                assertEquals("$name pk", col.pk, cols[name]!!.pk)
            }
        }
    }

    @Test
    fun jurisdictionTable_hasIndicesAndIsUsable() {
        memDb().use { c ->
            applyMigration7to8(c)

            val indices = mutableSetOf<String>()
            c.createStatement().use { st ->
                st.executeQuery("PRAGMA index_list(`user_jurisdictions`)").use { rs ->
                    while (rs.next()) indices += rs.getString("name")
                }
            }
            assertTrue("userId index", "index_user_jurisdictions_userId" in indices)
            assertTrue("syncStatus index", "index_user_jurisdictions_syncStatus" in indices)

            // The table is actually usable: insert + read a jurisdiction row back.
            c.prepareStatement(
                "INSERT INTO `user_jurisdictions` (`id`,`userId`,`stateCode`,`county`,`city`," +
                    "`source`,`createdAt`,`payloadJson`,`syncStatus`,`deleted`,`updatedAt`,`syncAttempts`) " +
                    "VALUES ('j1','u1','TX','Travis','Austin','manual',NULL,'{}','synced',0,10,0)",
            ).use { it.executeUpdate() }
            c.createStatement().use { st ->
                st.executeQuery("SELECT stateCode, city FROM `user_jurisdictions` WHERE id='j1'").use { rs ->
                    rs.next()
                    assertEquals("TX", rs.getString(1))
                    assertEquals("Austin", rs.getString(2))
                }
            }
        }
    }

    @Test
    fun migration_isIdempotent() {
        memDb().use { c ->
            applyMigration7to8(c)
            var reRanCleanly = true
            try {
                applyMigration7to8(c) // CREATE ... IF NOT EXISTS → no-op
            } catch (e: Exception) {
                reRanCleanly = false
            }
            assertTrue("re-running the migration must be safe", reRanCleanly)
        }
    }
}
