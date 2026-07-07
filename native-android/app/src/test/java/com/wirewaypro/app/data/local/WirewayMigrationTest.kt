package com.wirewaypro.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.DriverManager

/**
 * Exercises the v5 → v6 migration SQL against a real SQLite engine on the JVM
 * (no emulator needed). It runs the EXACT DDL constants the [WirewayDatabase]
 * migration executes, so the migration and this test can never drift.
 *
 * The doctrine is "never lose data": this proves the migration is purely
 * additive — the new `user_assemblies` table appears with the right schema, a
 * row round-trips, and rows already present before the migration survive it.
 */
class WirewayMigrationTest {

    private fun memoryDb() = DriverManager.getConnection("jdbc:sqlite::memory:")

    /** Every migration's statements, in version order — the exact shipping DDL. */
    private val migrationChain: List<List<String>> = listOf(
        listOf(WirewayDatabase.SQL_CREATE_QUOTE_DRAFTS),                                   // 1→2
        listOf(                                                                            // 2→3
            WirewayDatabase.SQL_CREATE_JOBS,
            WirewayDatabase.SQL_INDEX_JOBS_USER, WirewayDatabase.SQL_INDEX_JOBS_SYNC,
            WirewayDatabase.SQL_CREATE_CLIENTS,
            WirewayDatabase.SQL_INDEX_CLIENTS_USER, WirewayDatabase.SQL_INDEX_CLIENTS_SYNC,
            WirewayDatabase.SQL_CREATE_JOB_DRAWS,
            WirewayDatabase.SQL_INDEX_JOB_DRAWS_JOB, WirewayDatabase.SQL_INDEX_JOB_DRAWS_USER,
            WirewayDatabase.SQL_INDEX_JOB_DRAWS_SYNC,
        ),
        listOf(WirewayDatabase.SQL_CREATE_QUOTE_OVERRIDES, WirewayDatabase.SQL_INDEX_QUOTE_OVERRIDES_QUOTE), // 3→4
        listOf(WirewayDatabase.SQL_CREATE_QUOTE_PHOTOS, WirewayDatabase.SQL_INDEX_QUOTE_PHOTOS_QUOTE),       // 4→5
        listOf(                                                                            // 5→6
            WirewayDatabase.SQL_CREATE_USER_ASSEMBLIES,
            WirewayDatabase.SQL_INDEX_USER_ASSEMBLIES_USER, WirewayDatabase.SQL_INDEX_USER_ASSEMBLIES_SYNC,
        ),
        listOf(                                                                            // 6→7
            WirewayDatabase.CREW_MEMBERS_CREATE_TABLE,
            WirewayDatabase.CREW_MEMBERS_INDEX_USER, WirewayDatabase.CREW_MEMBERS_INDEX_SYNC,
        ),
    )

    /**
     * The whole point of the "never lose data" doctrine: a database created back
     * at v1 with real rows in it is upgraded step-by-step to the current version,
     * and NOTHING the user had is lost. Runs the exact shipping DDL (the same
     * constants Room executes), inserting rows at early versions and asserting
     * they all survive to the end — the guarantee we removed the destructive
     * fallback to rely on.
     */
    @Test
    fun `v1 to current upgrade preserves rows end to end`() {
        memoryDb().use { db ->
            val st = db.createStatement()

            // v1 schema held a `quotes` table (Room-created; represented minimally
            // here since the migrations never touch it). Seed a real quote.
            st.execute("CREATE TABLE `quotes` (`id` TEXT NOT NULL, `payloadJson` TEXT NOT NULL, PRIMARY KEY(`id`))")
            st.execute("INSERT INTO `quotes` (`id`,`payloadJson`) VALUES ('q1','{\"unsynced\":true}')")

            // 1→2, then seed a draft.
            migrationChain[0].forEach { st.execute(it) }
            st.execute("INSERT INTO `quote_drafts` (`draftKey`,`contentJson`,`updatedAt`) VALUES ('d1','{}',1)")

            // 2→3, then seed a job.
            migrationChain[1].forEach { st.execute(it) }
            st.execute(
                "INSERT INTO `jobs` (`id`,`userId`,`payloadJson`,`syncStatus`,`deleted`,`updatedAt`,`syncAttempts`) " +
                    "VALUES ('j1','u1','{}','pending',0,5,0)",
            )

            // 3→4, 4→5, 5→6, 6→7 — the rest of the chain.
            migrationChain.drop(2).forEach { step -> step.forEach { st.execute(it) } }

            // Seed a crew row into the final table to prove it's usable.
            st.execute(
                "INSERT INTO `crew_members` (`id`,`userId`,`name`,`role`,`hourlyCostRate`,`active`," +
                    "`createdAt`,`payloadJson`,`syncStatus`,`deleted`,`updatedAt`,`syncAttempts`) " +
                    "VALUES ('c1','u1','Sam','Journeyman',48.5,1,NULL,'{}','synced',0,9,0)",
            )

            // Every table the migration chain should have created now exists.
            val tables = mutableSetOf<String>()
            st.executeQuery("SELECT name FROM sqlite_master WHERE type='table'").use { rs ->
                while (rs.next()) tables += rs.getString("name")
            }
            listOf(
                "quotes", "quote_drafts", "jobs", "clients", "job_draws",
                "quote_overrides", "quote_photos", "user_assemblies", "crew_members",
            ).forEach { assertTrue("table $it missing after v1→current", it in tables) }

            // NOTHING was lost: the v1 quote, the v2 draft, the v3 job all survive,
            // and the crew row round-trips.
            fun count(sql: String): Int =
                st.executeQuery(sql).use { it.next(); it.getInt(1) }
            assertEquals("v1 quote lost", 1, count("SELECT COUNT(*) FROM `quotes` WHERE id='q1'"))
            assertEquals("v2 draft lost", 1, count("SELECT COUNT(*) FROM `quote_drafts` WHERE draftKey='d1'"))
            assertEquals("v3 job lost", 1, count("SELECT COUNT(*) FROM `jobs` WHERE id='j1'"))
            assertEquals("crew row lost", 1, count("SELECT COUNT(*) FROM `crew_members` WHERE id='c1'"))

            st.close()
        }
    }

    @Test
    fun `migration adds user_assemblies and preserves existing rows`() {
        memoryDb().use { db ->
            db.createStatement().use { st ->
                // Simulate a v5 database that already holds a user's data.
                st.execute("CREATE TABLE quotes (id TEXT PRIMARY KEY, payloadJson TEXT NOT NULL)")
                st.execute("INSERT INTO quotes (id, payloadJson) VALUES ('q1', '{\"n\":1}')")

                // Run the actual v5 → v6 DDL.
                st.execute(WirewayDatabase.SQL_CREATE_USER_ASSEMBLIES)
                st.execute(WirewayDatabase.SQL_INDEX_USER_ASSEMBLIES_USER)
                st.execute(WirewayDatabase.SQL_INDEX_USER_ASSEMBLIES_SYNC)
            }

            // The new table exists.
            db.createStatement().use { st ->
                st.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='user_assemblies'",
                ).use { rs ->
                    assertTrue("user_assemblies table missing after migration", rs.next())
                }
            }

            // Its columns match the Room entity exactly.
            val columns = mutableSetOf<String>()
            db.createStatement().use { st ->
                st.executeQuery("PRAGMA table_info(user_assemblies)").use { rs ->
                    while (rs.next()) columns += rs.getString("name")
                }
            }
            assertEquals(
                setOf("id", "userId", "name", "category", "payloadJson", "syncStatus", "deleted", "updatedAt", "syncAttempts", "createdAt"),
                columns,
            )

            // A template row round-trips through the new table.
            db.prepareStatement(
                "INSERT INTO user_assemblies " +
                    "(id, userId, name, category, payloadJson, syncStatus, deleted, updatedAt, syncAttempts, createdAt) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?)",
            ).use { ps ->
                ps.setString(1, "t1"); ps.setString(2, "u1"); ps.setString(3, "My Panel Combo")
                ps.setString(4, "Custom"); ps.setString(5, "{}"); ps.setString(6, "pending")
                ps.setInt(7, 0); ps.setLong(8, 123L); ps.setInt(9, 0); ps.setString(10, null)
                assertEquals(1, ps.executeUpdate())
            }
            db.createStatement().use { st ->
                st.executeQuery("SELECT name FROM user_assemblies WHERE id='t1'").use { rs ->
                    assertTrue(rs.next())
                    assertEquals("My Panel Combo", rs.getString("name"))
                }
            }

            // The pre-existing quote survived the migration — no data lost.
            db.createStatement().use { st ->
                st.executeQuery("SELECT COUNT(*) AS c FROM quotes WHERE id='q1'").use { rs ->
                    rs.next()
                    assertEquals(1, rs.getInt("c"))
                }
            }
        }
    }
}
