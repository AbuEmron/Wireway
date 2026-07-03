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
}
