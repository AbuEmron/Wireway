package com.wirewaypro.app.domain.catalog

import com.wirewaypro.app.domain.model.QuoteCatalogEntry
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The accuracy-first heart of the job walk: a room template must expand to the
 * EXACT catalog services, quantities, variants and labor hours — no fabrication,
 * every number traceable to [Catalog]. These lock that expansion, and the
 * per-area quantity multiply ("3 bedrooms"), against any drift.
 */
class AreaWalkTest {

    private fun assembly(id: String) = requireNotNull(Assemblies.byId(id)) { "template $id missing" }

    private fun qtyOf(m: JobWalk.Merged, serviceId: String) =
        m.entries.single { it.serviceId == serviceId }.qty

    @Test
    fun `bedroom expands to the exact catalog lines, quantities and variants`() {
        val bedroom = assembly("bedroom_standard")
        val entries = bedroom.toCatalogEntries().associateBy { it.serviceId }

        assertEquals(5, entries.size)
        // qty + variant index must be exact — a wrong variant changes the price.
        entries.assertLine("outlet_standard", qty = 4.0, variant = 1)
        entries.assertLine("light_ceiling", qty = 1.0, variant = 1)
        entries.assertLine("switch_single", qty = 1.0, variant = 3)
        entries.assertLine("smoke_co_combo", qty = 1.0, variant = 1)
        entries.assertLine("arc_fault", qty = 1.0, variant = 0)
    }

    @Test
    fun `bedroom labor hours equal the deterministic catalog math`() {
        // hours = Σ (laborHours × variant multiplier × qty), straight from Catalog.
        // outlet_standard 0.25×2.4×4 = 2.40
        // light_ceiling   0.75×2.0×1 = 1.50
        // switch_single   0.25×2.0×1 = 0.50
        // smoke_co_combo  0.60×1.65×1 = 0.99
        // arc_fault       1.25×1.0×1 = 1.25  → 6.64
        val merged = JobWalk.mergeAreas(listOf(JobWalk.WalkArea(assembly("bedroom_standard"), 1)))
        assertEquals(6.64, JobWalk.totalLaborHours(merged), 1e-9)
    }

    @Test
    fun `three bedrooms multiply every line by the area count`() {
        val merged = JobWalk.mergeAreas(listOf(JobWalk.WalkArea(assembly("bedroom_standard"), 3)))
        assertEquals(12.0, qtyOf(merged, "outlet_standard"), 0.0)
        assertEquals(3.0, qtyOf(merged, "light_ceiling"), 0.0)
        assertEquals(3.0, qtyOf(merged, "switch_single"), 0.0)
        assertEquals(3.0, qtyOf(merged, "smoke_co_combo"), 0.0)
        assertEquals(3.0, qtyOf(merged, "arc_fault"), 0.0)
        // Labor scales linearly with the count.
        assertEquals(6.64 * 3, JobWalk.totalLaborHours(merged), 1e-9)
    }

    @Test
    fun `a full walk merges shared services and dedupes the permit to one`() {
        // 3 bedrooms + 2 baths + 1 kitchen.
        val merged = JobWalk.mergeAreas(
            listOf(
                JobWalk.WalkArea(assembly("bedroom_standard"), 3),
                JobWalk.WalkArea(assembly("bathroom_standard"), 2),
                JobWalk.WalkArea(assembly("kitchen_area"), 1),
            ),
        )
        // circuit_20: bath 1×2 + kitchen 2×1 = 4.
        assertEquals(4.0, qtyOf(merged, "circuit_20"), 0.0)
        // outlet_gfci: bath 1×2 + kitchen 4×1 = 6.
        assertEquals(6.0, qtyOf(merged, "outlet_gfci"), 0.0)
        // Two baths + a kitchen each carry permit_general → exactly one line.
        assertEquals(1, merged.entries.count { it.serviceId == "permit_general" })
        assertEquals(1.0, qtyOf(merged, "permit_general"), 0.0)
        // Bedroom-only lines still ride at 3×.
        assertEquals(12.0, qtyOf(merged, "outlet_standard"), 0.0)
    }

    @Test
    fun `zero-count areas contribute nothing`() {
        val merged = JobWalk.mergeAreas(listOf(JobWalk.WalkArea(assembly("bedroom_standard"), 0)))
        assertEquals(0, merged.entries.size)
        assertEquals(0.0, JobWalk.totalLaborHours(merged), 0.0)
    }

    @Test
    fun `every room-and-area template references real catalog lines`() {
        val roomIds = listOf(
            "recessed_light_each", "dedicated_20a", "bedroom_standard",
            "living_room_standard", "bathroom_standard", "kitchen_area",
            "hallway_standard", "garage_area",
        )
        roomIds.forEach { id ->
            val a = assembly(id)
            a.items.forEach { item ->
                val svc = requireNotNull(Catalog.service(item.serviceId)) { "$id → ${item.serviceId} not in catalog" }
                assertEquals(
                    "$id → ${item.serviceId} variant out of range",
                    true,
                    item.variantIdx in svc.variants.indices,
                )
            }
        }
    }

    private fun Map<String, QuoteCatalogEntry>.assertLine(id: String, qty: Double, variant: Int) {
        val e = requireNotNull(this[id]) { "expected line $id" }
        assertEquals("$id qty", qty, e.qty, 0.0)
        assertEquals("$id variant", variant, e.variantIdx)
    }
}
