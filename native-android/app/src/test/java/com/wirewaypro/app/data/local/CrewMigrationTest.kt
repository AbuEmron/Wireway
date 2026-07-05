package com.wirewaypro.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.Connection
import java.sql.DriverManager

/**
 * Migration test for Room v6 → v7 (the Elite `crew_members` table).
 *
 * Runs the EXACT DDL constants that [WirewayDatabase.MIGRATION_6_7] executes
 * (they're referenced directly, so the test can never drift from the real
 * migration) against a pure-JVM in-memory SQLite — no emulator or Robolectric.
 * It proves the migration is:
 *  - additive (a row written before the migration survives it — never lose data),
 *  - correctly shaped (columns/types/PK match [CrewMemberEntity], which is what
 *    Room validates on open), and
 *  - idempotent (safe to re-run, matching `CREATE TABLE IF NOT EXISTS`).
 */
class CrewMigrationTest {

    private fun memDb(): Connection = DriverManager.getConnection("jdbc:sqlite::memory:")

    /** Applies the v6→v7 migration statements exactly as Room does. */
    private fun applyMigration6to7(c: Connection) {
        c.createStatement().use { st ->
            st.execute(WirewayDatabase.CREW_MEMBERS_CREATE_TABLE)
            st.execute(WirewayDatabase.CREW_MEMBERS_INDEX_USER)
            st.execute(WirewayDatabase.CREW_MEMBERS_INDEX_SYNC)
        }
    }

    @Test
    fun migration_addsCrewTable_withoutTouchingExistingData() {
        memDb().use { c ->
            // A pre-existing table with a row that MUST survive the migration.
            c.createStatement().use { st ->
                st.execute("CREATE TABLE `jobs` (`id` TEXT NOT NULL, `payloadJson` TEXT NOT NULL, PRIMARY KEY(`id`))")
                st.execute("INSERT INTO `jobs` (`id`, `payloadJson`) VALUES ('job-1', '{\"id\":\"job-1\"}')")
            }

            applyMigration6to7(c)

            // Existing data is untouched (additive migration).
            c.createStatement().use { st ->
                st.executeQuery("SELECT COUNT(*) FROM `jobs`").use { rs ->
                    rs.next()
                    assertEquals("existing job row must survive the migration", 1, rs.getInt(1))
                }
            }
        }
    }

    @Test
    fun crewTable_hasExactRoomSchema() {
        memDb().use { c ->
            applyMigration6to7(c)

            // Column name → (declared type, notNull, pk) per PRAGMA table_info.
            data class Col(val type: String, val notNull: Boolean, val pk: Boolean)
            val cols = mutableMapOf<String, Col>()
            c.createStatement().use { st ->
                st.executeQuery("PRAGMA table_info(`crew_members`)").use { rs ->
                    while (rs.next()) {
                        cols[rs.getString("name")] = Col(
                            type = rs.getString("type").uppercase(),
                            notNull = rs.getInt("notnull") == 1,
                            pk = rs.getInt("pk") == 1,
                        )
                    }
                }
            }

            // Must match CrewMemberEntity exactly — this is what Room checks on open.
            val expected = mapOf(
                "id" to Col("TEXT", true, true),
                "userId" to Col("TEXT", true, false),
                "name" to Col("TEXT", true, false),
                "role" to Col("TEXT", false, false),
                "hourlyCostRate" to Col("REAL", true, false),
                "active" to Col("INTEGER", true, false),
                "createdAt" to Col("TEXT", false, false),
                "payloadJson" to Col("TEXT", true, false),
                "syncStatus" to Col("TEXT", true, false),
                "deleted" to Col("INTEGER", true, false),
                "updatedAt" to Col("INTEGER", true, false),
                "syncAttempts" to Col("INTEGER", true, false),
            )
            assertEquals("crew_members column set", expected.keys, cols.keys)
            expected.forEach { (name, col) ->
                assertEquals("$name type", col.type, cols[name]!!.type)
                assertEquals("$name notNull", col.notNull, cols[name]!!.notNull)
                assertEquals("$name pk", col.pk, cols[name]!!.pk)
            }
        }
    }

    @Test
    fun crewTable_hasIndicesAndIsUsable() {
        memDb().use { c ->
            applyMigration6to7(c)

            val indices = mutableSetOf<String>()
            c.createStatement().use { st ->
                st.executeQuery("PRAGMA index_list(`crew_members`)").use { rs ->
                    while (rs.next()) indices += rs.getString("name")
                }
            }
            assertTrue("userId index", "index_crew_members_userId" in indices)
            assertTrue("syncStatus index", "index_crew_members_syncStatus" in indices)

            // The table is actually usable: insert + read a crew row back.
            c.prepareStatement(
                "INSERT INTO `crew_members` (`id`,`userId`,`name`,`role`,`hourlyCostRate`," +
                    "`active`,`createdAt`,`payloadJson`,`syncStatus`,`deleted`,`updatedAt`,`syncAttempts`) " +
                    "VALUES ('c1','u1','Sam','Journeyman',48.5,1,NULL,'{}','synced',0,10,0)",
            ).use { it.executeUpdate() }
            c.createStatement().use { st ->
                st.executeQuery("SELECT hourlyCostRate FROM `crew_members` WHERE id='c1'").use { rs ->
                    rs.next()
                    assertEquals(48.5, rs.getDouble(1), 0.0001)
                }
            }
        }
    }

    @Test
    fun migration_isIdempotent() {
        memDb().use { c ->
            applyMigration6to7(c)
            var reRanCleanly = true
            try {
                applyMigration6to7(c) // CREATE ... IF NOT EXISTS → no-op
            } catch (e: Exception) {
                reRanCleanly = false
            }
            assertTrue("re-running the migration must be safe", reRanCleanly)
        }
    }
}
