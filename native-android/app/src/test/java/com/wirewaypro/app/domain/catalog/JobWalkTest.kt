package com.wirewaypro.app.domain.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Locks the job-walk merge rules against the real template library. */
class JobWalkTest {

    private fun assembly(id: String): Assembly =
        requireNotNull(Assemblies.byId(id)) { "template $id missing from library" }

    @Test
    fun `shared services across areas add up, permits dedupe to one`() {
        // Kitchen (2× circuit_20, permit_general) + bathroom (1× circuit_20, permit_general).
        val merged = JobWalk.merge(listOf(assembly("kitchen_remodel"), assembly("bathroom_remodel")))

        val circuit20 = merged.entries.single { it.serviceId == "circuit_20" }
        assertEquals(3.0, circuit20.qty, 0.0)

        val permits = merged.entries.filter { it.serviceId == "permit_general" }
        assertEquals(1, permits.size)
        assertEquals(1.0, permits.single().qty, 0.0)

        // Recessed cans: kitchen 6 + bathroom 2.
        assertEquals(8.0, merged.entries.single { it.serviceId == "light_recessed" }.qty, 0.0)
    }

    @Test
    fun `a service permit covers general work — permit_general drops`() {
        // 200A service upgrade (permit_service) + EV charger (permit_general).
        val merged = JobWalk.merge(listOf(assembly("service_upgrade_200"), assembly("ev_charger_l2")))

        assertNotNull(merged.entries.find { it.serviceId == "permit_service" })
        assertTrue(merged.entries.none { it.serviceId == "permit_general" })
        // Both areas' work still rides.
        assertNotNull(merged.entries.find { it.serviceId == "panel_200" })
        assertNotNull(merged.entries.find { it.serviceId == "circuit_50" })
    }

    @Test
    fun `single template merges to itself`() {
        val one = assembly("ev_charger_l2")
        val merged = JobWalk.merge(listOf(one))
        assertEquals(one.items.size, merged.entries.size)
    }

    @Test
    fun `commercial custom lines concatenate`() {
        val commercial = Assemblies.all.first { it.customItems.isNotEmpty() }
        val merged = JobWalk.merge(listOf(commercial, assembly("ev_charger_l2")))
        assertEquals(commercial.customItems.size, merged.customItems.size)
    }

    @Test
    fun `merge is order stable — first area's lines come first`() {
        val merged = JobWalk.merge(listOf(assembly("kitchen_remodel"), assembly("bathroom_remodel")))
        assertEquals("circuit_20", merged.entries.first().serviceId)
    }
}
