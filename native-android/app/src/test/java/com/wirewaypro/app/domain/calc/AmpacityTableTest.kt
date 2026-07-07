package com.wirewaypro.app.domain.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Verifies transcribed NEC Table 310.16 values against the published code table. */
class AmpacityTableTest {

    @Test
    fun copper_common_sizes_match_310_16() {
        assertEquals(20, AmpacityTable.ampacity(ConductorMaterial.COPPER, Awg.AWG12, TempRating.C60))
        assertEquals(25, AmpacityTable.ampacity(ConductorMaterial.COPPER, Awg.AWG12, TempRating.C75))
        assertEquals(30, AmpacityTable.ampacity(ConductorMaterial.COPPER, Awg.AWG12, TempRating.C90))

        assertEquals(30, AmpacityTable.ampacity(ConductorMaterial.COPPER, Awg.AWG10, TempRating.C60))
        assertEquals(65, AmpacityTable.ampacity(ConductorMaterial.COPPER, Awg.AWG6, TempRating.C75))
        assertEquals(225, AmpacityTable.ampacity(ConductorMaterial.COPPER, Awg.AWG3_0, TempRating.C90))
        assertEquals(230, AmpacityTable.ampacity(ConductorMaterial.COPPER, Awg.AWG4_0, TempRating.C75))
        assertEquals(380, AmpacityTable.ampacity(ConductorMaterial.COPPER, Awg.KCMIL500, TempRating.C75))
    }

    @Test
    fun aluminum_common_sizes_match_310_16() {
        assertEquals(20, AmpacityTable.ampacity(ConductorMaterial.ALUMINUM, Awg.AWG12, TempRating.C75))
        assertEquals(180, AmpacityTable.ampacity(ConductorMaterial.ALUMINUM, Awg.AWG4_0, TempRating.C75))
        assertEquals(230, AmpacityTable.ampacity(ConductorMaterial.ALUMINUM, Awg.KCMIL250, TempRating.C90))
    }

    @Test
    fun aluminum_14awg_is_not_listed() {
        assertNull(AmpacityTable.ampacity(ConductorMaterial.ALUMINUM, Awg.AWG14, TempRating.C75))
    }

    @Test
    fun aluminum_gauge_list_excludes_14awg() {
        val gauges = AmpacityTable.gauges(ConductorMaterial.ALUMINUM)
        assertEquals(false, gauges.contains(Awg.AWG14))
        assertEquals(true, gauges.contains(Awg.AWG12))
    }
}
