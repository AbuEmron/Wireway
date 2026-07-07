package com.wirewaypro.app.domain.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the template library against catalog drift: every residential line
 * must reference a real Catalog service + in-range variant, every
 * commercial/industrial line a real EliteCatalog id, quantities and hours
 * positive, ids unique. A template that fails here would seed a broken quote.
 */
class AssembliesTest {

    @Test
    fun `every template line references a real catalog entry`() {
        assertTrue(Assemblies.validate())
    }

    @Test
    fun `template ids are unique`() {
        val ids = Assemblies.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `no template is empty`() {
        Assemblies.all.forEach { a ->
            assertTrue("${a.id} has no lines", a.itemCount > 0)
        }
    }

    @Test
    fun `residential templates use catalog lines and elite templates use custom lines`() {
        Assemblies.all.forEach { a ->
            when (a.sector) {
                AssemblySector.RESIDENTIAL ->
                    assertTrue("${a.id} residential template must carry catalog lines", a.items.isNotEmpty())
                AssemblySector.COMMERCIAL_INDUSTRIAL ->
                    assertTrue("${a.id} elite template must carry custom lines", a.customItems.isNotEmpty())
            }
        }
    }

    @Test
    fun `toCatalogEntries maps qty and variant faithfully`() {
        val assembly = Assemblies.byId("service_upgrade_200")!!
        val entries = assembly.toCatalogEntries()
        assertEquals(assembly.items.size, entries.size)
        assembly.items.zip(entries).forEach { (item, entry) ->
            assertEquals(item.serviceId, entry.serviceId)
            assertEquals(item.qty, entry.qty, 0.0)
            assertEquals(item.variantIdx, entry.variantIdx)
        }
    }

    @Test
    fun `service work templates carry a permit line`() {
        // The "job in a box" promise: service/panel work includes its permit.
        listOf("service_upgrade_200", "panel_swap_100", "whole_house_rewire", "standby_generator")
            .forEach { id ->
                val a = Assemblies.byId(id)!!
                assertTrue(
                    "$id should include a permit line",
                    a.items.any { it.serviceId.startsWith("permit_") },
                )
            }
    }
}
