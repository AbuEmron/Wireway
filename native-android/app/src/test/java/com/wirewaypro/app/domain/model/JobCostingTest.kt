package com.wirewaypro.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * True job-costing math. Every figure must be defensible (doctrine #1), so this
 * pins:
 *  - actual labor cost = Σ(hours × crew cost rate),
 *  - true profit = collected − actual costs,
 *  - variance = actual − estimate (labor in hours, materials in cost),
 * against worked numbers.
 */
class JobCostingTest {

    private fun costing(
        estLaborHours: Double = 40.0,
        actualLaborHours: Double = 46.0,
        actualLaborCost: Double = 2070.0,
        estMaterial: Double = 1200.0,
        actualMaterial: Double = 1500.0,
        collected: Double = 6000.0,
        hasEstimate: Boolean = true,
    ) = JobCosting(
        estimatedLaborHours = estLaborHours,
        actualLaborHours = actualLaborHours,
        actualLaborCost = actualLaborCost,
        estimatedMaterialCost = estMaterial,
        actualMaterialCost = actualMaterial,
        collected = collected,
        hasEstimate = hasEstimate,
    )

    @Test
    fun actualLaborCost_isSumOfHoursTimesRate() {
        // Two crew: 8h @ $45 (=360) + 6.5h @ $30 (=195) = 555 over 14.5 hours.
        val entries = listOf(8.0 to 45.0, 6.5 to 30.0)
        val laborCost = entries.sumOf { (h, r) -> h * r }
        val laborHours = entries.sumOf { it.first }
        val c = costing(actualLaborHours = laborHours, actualLaborCost = laborCost)
        assertEquals(555.0, c.actualLaborCost, 0.0001)
        assertEquals(14.5, c.actualLaborHours, 0.0001)
    }

    @Test
    fun trueProfit_isCollectedMinusActualCosts() {
        val c = costing(actualLaborCost = 2070.0, actualMaterial = 1500.0, collected = 6000.0)
        assertEquals(3570.0, c.actualTotalCost, 0.0001)     // 2070 + 1500
        assertEquals(2430.0, c.trueProfit, 0.0001)          // 6000 − 3570
        assertEquals(2430.0 / 6000.0, c.margin!!, 0.0001)
    }

    @Test
    fun variance_isActualMinusEstimate_overBudget() {
        val c = costing(
            estLaborHours = 40.0, actualLaborHours = 46.0,   // +6 hrs over
            estMaterial = 1200.0, actualMaterial = 1500.0,   // +$300 over
        )
        assertEquals(6.0, c.laborHoursVariance, 0.0001)
        assertEquals(300.0, c.materialVariance, 0.0001)
        assertTrue("labor over budget", c.laborHoursVariance > 0)
        assertTrue("materials over budget", c.materialVariance > 0)
    }

    @Test
    fun variance_isNegativeWhenUnderBudget() {
        val c = costing(
            estLaborHours = 50.0, actualLaborHours = 44.0,   // −6 hrs under
            estMaterial = 1600.0, actualMaterial = 1450.0,   // −$150 under
        )
        assertEquals(-6.0, c.laborHoursVariance, 0.0001)
        assertEquals(-150.0, c.materialVariance, 0.0001)
    }

    @Test
    fun margin_isNullBeforeAnyMoneyCollected() {
        assertNull(costing(collected = 0.0).margin)
    }

    @Test
    fun isEmpty_whenNothingRecorded() {
        val empty = JobCosting(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, hasEstimate = false)
        assertTrue(empty.isEmpty)
        assertEquals(0.0, empty.trueProfit, 0.0001)
    }
}
