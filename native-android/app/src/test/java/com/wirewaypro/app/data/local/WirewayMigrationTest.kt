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
