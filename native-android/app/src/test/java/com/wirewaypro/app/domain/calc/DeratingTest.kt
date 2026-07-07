package com.wirewaypro.app.domain.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Ambient (Table 310.15(B)(1)) and bundling (Table 310.15(C)(1)) factors. */
class DeratingTest {

    @Test
    fun ambient_factors_match_table_310_15_B_1() {
        assertEquals(0.88, Derating.ambientFactor(TempRating.C75, 38)!!, 1e-9)
        assertEquals(0.96, Derating.ambientFactor(TempRating.C90, 33)!!, 1e-9)
        assertEquals(0.41, Derating.ambientFactor(TempRating.C60, 52)!!, 1e-9)
        assertEquals(1.00, Derating.ambientFactor(TempRating.C75, 30)!!, 1e-9)
    }

    @Test
    fun ambient_off_table_returns_null_for_60c_conductor() {
        // 60 °C insulation has no factor above 55 °C ambient.
        assertNull(Derating.ambientFactor(TempRating.C60, 62))
    }

    @Test
    fun bundling_factors_match_table_310_15_C_1() {
        assertEquals(1.00, Derating.bundlingFactor(3), 1e-9)
        assertEquals(0.80, Derating.bundlingFactor(4), 1e-9)
        assertEquals(0.80, Derating.bundlingFactor(6), 1e-9)
        assertEquals(0.70, Derating.bundlingFactor(7), 1e-9)
        assertEquals(0.50, Derating.bundlingFactor(12), 1e-9)
        assertEquals(0.45, Derating.bundlingFactor(25), 1e-9)
        assertEquals(0.40, Derating.bundlingFactor(40), 1e-9)
        assertEquals(0.35, Derating.bundlingFactor(45), 1e-9)
    }

    @Test
    fun combined_derate_multiplies_both_factors() {
        // 12 AWG Cu 90 °C base = 30 A; ambient 40 °C (0.91); 4 conductors (0.80).
        val base = AmpacityTable.ampacity(ConductorMaterial.COPPER, Awg.AWG12, TempRating.C90)!!.toDouble()
        val r = Derating.derate(base, TempRating.C90, ambientC = 40, currentCarrying = 4)
        assertNotNull(r)
        assertEquals(0.91, r!!.ambientFactor, 1e-9)
        assertEquals(0.80, r.bundlingFactor, 1e-9)
        assertEquals(30.0 * 0.91 * 0.80, r.deratedAmpacity, 1e-9)
    }
}
