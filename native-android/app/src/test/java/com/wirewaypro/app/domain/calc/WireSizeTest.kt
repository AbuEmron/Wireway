package com.wirewaypro.app.domain.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/** Conductor selection against Table 310.16, including the 125% continuous factor. */
class WireSizeTest {

    @Test
    fun noncontinuous_40a_copper_60c_is_8awg() {
        val r = WireSize.minimumSize(40.0, ConductorMaterial.COPPER, TempRating.C60, continuous = false)
        assertNotNull(r)
        assertEquals(Awg.AWG8, r!!.gauge)
        assertEquals(40, r.gaugeAmpacity)
    }

    @Test
    fun continuous_40a_copper_60c_upsizes_to_6awg() {
        // 40 A × 1.25 = 50 A required → 8 AWG (40) fails, 6 AWG (55) is the first fit.
        val r = WireSize.minimumSize(40.0, ConductorMaterial.COPPER, TempRating.C60, continuous = true)
        assertNotNull(r)
        assertEquals(Awg.AWG6, r!!.gauge)
        assertEquals(50.0, r.requiredAmpacity, 1e-9)
    }

    @Test
    fun noncontinuous_200a_copper_75c_is_3_0() {
        val r = WireSize.minimumSize(200.0, ConductorMaterial.COPPER, TempRating.C75)
        assertNotNull(r)
        assertEquals(Awg.AWG3_0, r!!.gauge)
        assertEquals(200, r.gaugeAmpacity)
    }

    @Test
    fun noncontinuous_100a_copper_60c_is_1awg() {
        val r = WireSize.minimumSize(100.0, ConductorMaterial.COPPER, TempRating.C60)
        assertNotNull(r)
        assertEquals(Awg.AWG1, r!!.gauge)
    }
}
