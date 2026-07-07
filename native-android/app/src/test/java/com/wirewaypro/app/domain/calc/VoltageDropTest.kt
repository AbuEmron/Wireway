package com.wirewaypro.app.domain.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Voltage drop via Vd = (phase) × K × I × L ÷ CM against hand-worked examples. */
class VoltageDropTest {

    @Test
    fun single_phase_12awg_copper_20a_100ft() {
        // 2 × 12.9 × 20 × 100 ÷ 6530 = 7.902 V; 7.902 ÷ 120 = 6.585%.
        val r = VoltageDrop.calculate(
            gauge = Awg.AWG12,
            material = ConductorMaterial.COPPER,
            systemVolts = 120.0,
            loadAmps = 20.0,
            lengthFtOneWay = 100.0,
        )
        assertEquals(7.902, r.voltageDrop, 0.01)
        assertEquals(6.585, r.percent, 0.01)
        assertEquals(112.098, r.endVoltage, 0.01)
        assertFalse(r.withinRecommended)
    }

    @Test
    fun short_run_is_within_three_percent() {
        val r = VoltageDrop.calculate(
            gauge = Awg.AWG12,
            material = ConductorMaterial.COPPER,
            systemVolts = 120.0,
            loadAmps = 15.0,
            lengthFtOneWay = 25.0,
        )
        assertTrue(r.withinRecommended)
    }

    @Test
    fun min_size_for_three_percent_upsizes_the_long_run() {
        // 120 V, 20 A, 100 ft, ≤3% → needs ≥14,333 cmil → 8 AWG (16,510).
        val g = VoltageDrop.minimumSizeForDrop(
            material = ConductorMaterial.COPPER,
            systemVolts = 120.0,
            loadAmps = 20.0,
            lengthFtOneWay = 100.0,
        )
        assertEquals(Awg.AWG8, g)
    }

    @Test
    fun required_circular_mils_matches_rearranged_formula() {
        val cm = VoltageDrop.requiredCircularMils(
            material = ConductorMaterial.COPPER,
            systemVolts = 120.0,
            loadAmps = 20.0,
            lengthFtOneWay = 100.0,
            targetPct = 3.0,
        )
        // 2 × 12.9 × 20 × 100 ÷ (120 × 0.03 = 3.6) = 14,333.3 cmil.
        assertEquals(14333.33, cm, 0.5)
    }
}
