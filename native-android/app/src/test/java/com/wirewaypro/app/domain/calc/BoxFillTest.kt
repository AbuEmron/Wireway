package com.wirewaypro.app.domain.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Box fill against NEC 314.16(B) counting rules and volume allowances. */
class BoxFillTest {

    @Test
    fun volume_allowances_match_table_314_16_B() {
        assertEquals(2.00, BoxFill.BoxWire.AWG14.volumeSqIn, 1e-9)
        assertEquals(2.25, BoxFill.BoxWire.AWG12.volumeSqIn, 1e-9)
        assertEquals(5.00, BoxFill.BoxWire.AWG6.volumeSqIn, 1e-9)
    }

    @Test
    fun two_14_2_cables_with_receptacle_clamps_and_ground() {
        // Two 14/2 NM = 4 insulated #14 conductors, one receptacle (device), internal
        // clamps, and the EGCs. Count = 4 + 2 (device) + 1 (clamps) + 1 (grounds) = 8.
        val input = BoxFill.Input(
            conductorSize = BoxFill.BoxWire.AWG14,
            conductors = 4,
            devices = 1,
            hasClamps = true,
            groundingConductors = 2,
        )
        val onEighteen = BoxFill.evaluate(input, boxVolumeSqIn = 18.0)
        assertEquals(8.0, onEighteen.conductorEquivalents, 1e-9)
        assertEquals(16.0, onEighteen.requiredVolumeSqIn, 1e-9) // 8 × 2.00
        assertTrue(onEighteen.withinLimit)

        // The same fill overflows a small 14 in³ box.
        assertFalse(BoxFill.evaluate(input, boxVolumeSqIn = 14.0).withinLimit)
    }

    @Test
    fun pigtail_grounds_count_once_not_per_conductor() {
        val many = BoxFill.Input(
            conductorSize = BoxFill.BoxWire.AWG12,
            conductors = 3,
            devices = 1,
            hasClamps = true,
            groundingConductors = 3,
        )
        // 3 + 2 + 1 (clamps) + 1 (all grounds together) = 7 → 7 × 2.25 = 15.75 in³.
        val r = BoxFill.evaluate(many, boxVolumeSqIn = 18.0)
        assertEquals(7.0, r.conductorEquivalents, 1e-9)
        assertEquals(15.75, r.requiredVolumeSqIn, 1e-9)
    }
}
