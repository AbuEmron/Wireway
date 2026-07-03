package com.wirewaypro.app.domain.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Conduit fill against NEC Chapter 9 Tables 1, 4, and 5. */
class ConduitFillTest {

    @Test
    fun max_fill_fraction_matches_table_1() {
        assertEquals(0.53, ConduitFill.maxFillFraction(1), 1e-9)
        assertEquals(0.31, ConduitFill.maxFillFraction(2), 1e-9)
        assertEquals(0.40, ConduitFill.maxFillFraction(3), 1e-9)
        assertEquals(0.40, ConduitFill.maxFillFraction(9), 1e-9)
    }

    @Test
    fun three_12awg_thhn_fit_half_inch_emt() {
        val conductors = listOf(ConduitFill.ConductorSpec(Awg.AWG12, 3))
        val half = ConduitFill.tradeSizes(ConduitFill.ConduitType.EMT).first()
        val r = ConduitFill.evaluate(conductors, ConduitFill.ConduitType.EMT, half)
        // 3 × 0.0133 = 0.0399 in²; 0.0399 ÷ 0.304 = 13.13% fill; allowed 40%.
        assertEquals(0.0399, r.conductorAreaSqIn, 1e-6)
        assertEquals(13.13, r.fillPercent, 0.05)
        assertTrue(r.withinLimit)
        assertEquals("1/2\"", ConduitFill.minimumConduit(conductors, ConduitFill.ConduitType.EMT)!!.label)
    }

    @Test
    fun nine_12awg_thhn_is_the_max_for_half_inch_emt() {
        val nine = listOf(ConduitFill.ConductorSpec(Awg.AWG12, 9))
        val ten = listOf(ConduitFill.ConductorSpec(Awg.AWG12, 10))
        val half = ConduitFill.tradeSizes(ConduitFill.ConduitType.EMT).first()
        // Allowed area = 0.304 × 0.40 = 0.1216 in². 9 fit (0.1197), 10 do not (0.1330).
        assertTrue(ConduitFill.evaluate(nine, ConduitFill.ConduitType.EMT, half).withinLimit)
        assertFalse(ConduitFill.evaluate(ten, ConduitFill.ConduitType.EMT, half).withinLimit)
    }

    @Test
    fun four_4_0_thhn_need_2_inch_emt() {
        val conductors = listOf(ConduitFill.ConductorSpec(Awg.AWG4_0, 4))
        // 4 × 0.3237 = 1.2948 in²; 40% fill needs total ≥ 3.237 in² → 2" EMT (3.356).
        assertEquals("2\"", ConduitFill.minimumConduit(conductors, ConduitFill.ConduitType.EMT)!!.label)
    }

    @Test
    fun large_kcmil_areas_match_chapter_9_table_5() {
        // NEC Ch.9 Table 5, THHN/THWN-2 approximate area (in²).
        assertEquals(0.4608, Awg.KCMIL300.areaThhn, 1e-9)
        assertEquals(0.5863, Awg.KCMIL400.areaThhn, 1e-9)
        assertEquals(0.8676, Awg.KCMIL600.areaThhn, 1e-9)
        assertEquals(0.9887, Awg.KCMIL700.areaThhn, 1e-9)
        assertEquals(1.0496, Awg.KCMIL750.areaThhn, 1e-9)
        assertEquals(1.3478, Awg.KCMIL1000.areaThhn, 1e-9)
    }

    @Test
    fun three_600_kcmil_thhn_need_3_inch_emt() {
        // A 3-phase commercial feeder: 3 × 0.8676 = 2.6028 in² at 40% fill needs
        // total ≥ 6.507 in² → 2-1/2" EMT (5.858) is too small, 3" (8.846) fits.
        val conductors = listOf(ConduitFill.ConductorSpec(Awg.KCMIL600, 3))
        val r3 = ConduitFill.evaluate(
            conductors,
            ConduitFill.ConduitType.EMT,
            ConduitFill.tradeSizes(ConduitFill.ConduitType.EMT).first { it.label == "3\"" },
        )
        assertEquals(2.6028, r3.conductorAreaSqIn, 1e-6)
        assertTrue(r3.withinLimit)
        assertEquals("3\"", ConduitFill.minimumConduit(conductors, ConduitFill.ConduitType.EMT)!!.label)
    }

    @Test
    fun four_750_kcmil_thhn_need_3_and_a_half_inch_emt() {
        // 4 × 1.0496 = 4.1984 in²; 40% fill needs total ≥ 10.496 in² →
        // 3" EMT (8.846) is too small, 3-1/2" (11.545) fits at 36.4%.
        val conductors = listOf(ConduitFill.ConductorSpec(Awg.KCMIL750, 4))
        assertEquals(4.1984, ConduitFill.conductorArea(conductors), 1e-6)
        assertEquals("3-1/2\"", ConduitFill.minimumConduit(conductors, ConduitFill.ConduitType.EMT)!!.label)
    }
}
