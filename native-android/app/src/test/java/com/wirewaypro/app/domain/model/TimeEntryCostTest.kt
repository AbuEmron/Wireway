package com.wirewaypro.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Deterministic labor-cost contract: a completed crew time entry contributes
 * hours × the crew member's cost rate — no rounding surprises, no fabricated
 * numbers. This is the atom the job-costing actuals are summed from.
 */
class TimeEntryCostTest {

    private fun entry(hours: Double?, rate: Double, running: Boolean = false) = TimeEntry(
        id = "t", jobId = "j", workerName = "Sam", clockIn = null, clockOut = null,
        hours = hours, rate = rate, isRunning = running, notes = null, createdAt = null,
        crewMemberId = "c1",
    )

    @Test
    fun laborCost_isHoursTimesRate() {
        assertEquals(360.0, entry(hours = 8.0, rate = 45.0).laborCost, 0.0001)
        assertEquals(46.75, entry(hours = 1.1, rate = 42.5).laborCost, 0.0001)
    }

    @Test
    fun laborCost_isZeroWhenNoHoursYet() {
        // A running timer (or an entry with no hours) contributes nothing until stopped.
        assertEquals(0.0, entry(hours = null, rate = 55.0, running = true).laborCost, 0.0001)
    }

    @Test
    fun summedLaborCost_matchesSumOfHoursTimesRate() {
        val entries = listOf(
            entry(hours = 8.0, rate = 45.0),   // 360
            entry(hours = 6.5, rate = 30.0),   // 195
            entry(hours = 2.0, rate = 52.5),   // 105
        )
        val expected = 360.0 + 195.0 + 105.0
        assertEquals(expected, entries.sumOf { it.laborCost }, 0.0001)
    }
}
