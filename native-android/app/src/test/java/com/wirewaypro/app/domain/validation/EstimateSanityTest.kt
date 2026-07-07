package com.wirewaypro.app.domain.validation

import com.wirewaypro.app.domain.model.QuoteCatalogEntry
import com.wirewaypro.app.domain.model.QuoteCustomItem
import com.wirewaypro.app.domain.model.QuoteTotals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EstimateSanityTest {

    private fun totals(material: Double = 100.0, labor: Double = 500.0) =
        QuoteTotals(
            totalMaterial = material,
            totalLabor = labor,
            totalHours = 4.0,
            markupAmount = 0.0,
            taxAmount = 0.0,
            total = material + labor,
        )

    private fun check(
        entries: List<QuoteCatalogEntry> = emptyList(),
        items: List<QuoteCustomItem> = emptyList(),
        t: QuoteTotals = totals(),
        clientBuysAll: Boolean = false,
        depositPercent: Int? = null,
    ) = EstimateSanity.check(entries, items, t, clientBuysAll, depositPercent)

    @Test
    fun `permit anchors derive from the template library and include panel work`() {
        // The rule is built OFF the assemblies: the 200A-service and generator
        // templates carry permits, so their anchor services must be in the set.
        assertTrue(EstimateSanity.permitAnchors.contains("panel_200"))
        assertTrue(EstimateSanity.permitAnchors.contains("generator_install"))
        assertTrue(EstimateSanity.permitAnchors.contains("battery_storage"))
        // circuit_15 anchors the (permitted) basement template but ALSO appears
        // in the permit-free can-lights template — the library itself says it
        // doesn't always need a permit, so it must NOT be an anchor.
        assertTrue("circuit_15" !in EstimateSanity.permitAnchors)
    }

    @Test
    fun `panel upgrade without a permit line is flagged`() {
        val flags = check(entries = listOf(QuoteCatalogEntry("panel_200", qty = 1.0)))
        assertTrue(flags.any { it.id == "permit-missing" })
    }

    @Test
    fun `panel upgrade WITH a permit line is clean`() {
        val flags = check(
            entries = listOf(
                QuoteCatalogEntry("panel_200", qty = 1.0),
                QuoteCatalogEntry("permit_service", qty = 1.0),
            ),
        )
        assertTrue(flags.none { it.id == "permit-missing" })
    }

    @Test
    fun `non permit work does not demand a permit`() {
        // A couple of floods + a timer never co-anchor a permit template.
        val flags = check(entries = listOf(QuoteCatalogEntry("flood_light", qty = 2.0)))
        assertTrue(flags.none { it.id == "permit-missing" })
    }

    @Test
    fun `fat fingered quantity is flagged`() {
        val flags = check(entries = listOf(QuoteCatalogEntry("light_recessed", qty = 60.0)))
        assertTrue(flags.any { it.id == "qty-suspect:light_recessed" })
        // 6 cans is a normal job — no flag.
        assertTrue(check(entries = listOf(QuoteCatalogEntry("light_recessed", qty = 6.0)))
            .none { it.id.startsWith("qty-suspect") })
    }

    @Test
    fun `zero materials while contractor supplies them is flagged, client-supplied is not`() {
        val t = totals(material = 0.0, labor = 800.0)
        assertTrue(check(t = t, clientBuysAll = false).any { it.id == "materials-zero" })
        assertTrue(check(t = t, clientBuysAll = true).none { it.id == "materials-zero" })
    }

    @Test
    fun `hours with unpriced labor on a custom line is flagged`() {
        val flags = check(
            items = listOf(QuoteCustomItem(label = "Install VFD", qty = 1.0, laborHours = 6.0, laborCost = 0.0)),
        )
        assertTrue(flags.any { it.id.startsWith("labor-unpriced") })
    }

    @Test
    fun `deposit above fifty percent is flagged`() {
        assertTrue(check(depositPercent = 60).any { it.id == "deposit-high" })
        assertTrue(check(depositPercent = 50).none { it.id == "deposit-high" })
        assertTrue(check(depositPercent = null).none { it.id == "deposit-high" })
    }

    @Test
    fun `duplicate custom labels are flagged once per label`() {
        val flags = check(
            items = listOf(
                QuoteCustomItem(label = "Trench 40 ft", qty = 1.0, laborCost = 100.0),
                QuoteCustomItem(label = "trench 40 ft", qty = 1.0, laborCost = 100.0),
            ),
        )
        assertEquals(1, flags.count { it.id.startsWith("duplicate-line") })
    }

    @Test
    fun `a clean estimate produces zero flags`() {
        val flags = check(
            entries = listOf(
                QuoteCatalogEntry("light_recessed", qty = 6.0),
                QuoteCatalogEntry("dimmer_single", qty = 1.0),
                QuoteCatalogEntry("circuit_15", qty = 1.0),
            ),
        )
        assertTrue(flags.isEmpty())
    }
}
