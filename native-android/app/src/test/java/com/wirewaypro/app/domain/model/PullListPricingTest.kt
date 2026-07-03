package com.wirewaypro.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Locks the pull-list line math to purchasable units (accuracy doctrine).
 *
 * The bug these tests kill: 25 ft of 10/3 NM-B priced $75 per 25-ft coil was
 * totalled as 25 × $75 = $1,875 — a per-PACKAGE price multiplied by FEET.
 * Correct: ceil(25/25) = one coil → $75.
 */
class PullListPricingTest {

    private fun item(
        qty: Double,
        unit: String,
        price: Double?,
        basis: PriceBasis = PriceBasis.PER_UNIT,
        packageSize: Double? = null,
        prices: List<StorePrice> = emptyList(),
    ) = PullItem(
        name = "test",
        spec = null,
        qty = qty,
        unit = unit,
        price = price,
        bestStore = null,
        prices = prices,
        live = true,
        basis = basis,
        packageSize = packageSize,
    )

    // ── The screenshot bug: coil-priced wire must total per coil, not per foot ──

    @Test
    fun `25 ft of NM-B at 75 per 25-ft coil totals one coil, not 1875`() {
        val wire = item(qty = 25.0, unit = "ft", price = 75.0, basis = PriceBasis.PER_PACKAGE, packageSize = 25.0)
        assertEquals(75.0, wire.lineTotal!!, 0.001)
        assertNotEquals(1875.0, wire.lineTotal!!, 0.001)
    }

    @Test
    fun `50 ft of NM-B at 75 per 25-ft coil totals two coils, not 3750`() {
        val wire = item(qty = 50.0, unit = "ft", price = 75.0, basis = PriceBasis.PER_PACKAGE, packageSize = 25.0)
        assertEquals(150.0, wire.lineTotal!!, 0.001)
    }

    @Test
    fun `packages round UP - 60 ft needs three 25-ft coils`() {
        val wire = item(qty = 60.0, unit = "ft", price = 75.0, basis = PriceBasis.PER_PACKAGE, packageSize = 25.0)
        assertEquals(225.0, wire.lineTotal!!, 0.001)
    }

    @Test
    fun `true per-foot price multiplies by footage`() {
        val thhn = item(qty = 120.0, unit = "ft", price = 0.42, basis = PriceBasis.PER_FOOT)
        assertEquals(50.4, thhn.lineTotal!!, 0.001)
    }

    // ── Never fabricate: mismatched or unknown basis yields no total ──

    @Test
    fun `footage with unknown basis has no line total`() {
        val wire = item(qty = 50.0, unit = "ft", price = 75.0, basis = PriceBasis.UNKNOWN)
        assertNull(wire.lineTotal)
    }

    @Test
    fun `footage with a per-unit price is a units mismatch - no total`() {
        // Even if the parser mislabels, the domain never multiplies a package
        // price by feet.
        val wire = item(qty = 50.0, unit = "ft", price = 75.0, basis = PriceBasis.PER_UNIT)
        assertNull(wire.lineTotal)
    }

    @Test
    fun `per-package without a package size has no total`() {
        val wire = item(qty = 50.0, unit = "ft", price = 75.0, basis = PriceBasis.PER_PACKAGE, packageSize = null)
        assertNull(wire.lineTotal)
    }

    // ── Regression guard: counted units keep the classic qty × price ──

    @Test
    fun `one breaker at 28 each totals 28`() {
        val breaker = item(qty = 1.0, unit = "ea", price = 28.0)
        assertEquals(28.0, breaker.lineTotal!!, 0.001)
    }

    @Test
    fun `two flex whips at 8 each total 16`() {
        val whips = item(qty = 2.0, unit = "ea", price = 8.0)
        assertEquals(16.0, whips.lineTotal!!, 0.001)
    }

    @Test
    fun `boxed smalls keep qty times price`() {
        val nuts = item(qty = 3.0, unit = "box", price = 12.5)
        assertEquals(37.5, nuts.lineTotal!!, 0.001)
    }

    // ── Rollups stay honest ──

    @Test
    fun `estimated total sums only unit-consistent lines and counts the rest as unconfirmed`() {
        val result = PullListResult(
            sections = listOf(
                PullSection(
                    "Service",
                    listOf(
                        item(qty = 25.0, unit = "ft", price = 75.0, basis = PriceBasis.PER_PACKAGE, packageSize = 25.0), // 75
                        item(qty = 1.0, unit = "ea", price = 28.0), // 28
                        item(qty = 50.0, unit = "ft", price = 75.0, basis = PriceBasis.UNKNOWN), // excluded
                    ),
                ),
            ),
            notes = null,
        )
        assertEquals(103.0, result.estTotal, 0.001)
        assertEquals(1, result.unconfirmedCount)
    }

    @Test
    fun `savings uses purchasable units, not feet`() {
        val result = PullListResult(
            sections = listOf(
                PullSection(
                    "Service",
                    listOf(
                        item(
                            qty = 50.0, unit = "ft", price = 75.0,
                            basis = PriceBasis.PER_PACKAGE, packageSize = 25.0,
                            prices = listOf(StorePrice("Home Depot", 75.0), StorePrice("Lowe's", 80.0)),
                        ),
                    ),
                ),
            ),
            notes = null,
        )
        // Two coils × $5 spread = $10 — not 50 ft × $5 = $250.
        assertEquals(10.0, result.savings, 0.001)
    }
}
