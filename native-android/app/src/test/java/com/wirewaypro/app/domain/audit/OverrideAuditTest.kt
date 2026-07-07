package com.wirewaypro.app.domain.audit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverrideAuditTest {

    private val baseline = listOf(
        OverrideAudit.Derived("rate:hourly", "Hourly rate", 95.0),
        OverrideAudit.Derived("labor-hours:panel swap", "Labor hrs — panel swap", 8.0),
        OverrideAudit.Derived("labor-cost:panel swap", "Labor — panel swap", 760.0),
    )

    @Test
    fun `a changed derived value is recorded with original and override`() {
        val overrides = OverrideAudit.diff(
            baseline,
            mapOf("rate:hourly" to 120.0, "labor-hours:panel swap" to 8.0, "labor-cost:panel swap" to 760.0),
        )
        assertEquals(1, overrides.size)
        val o = overrides.single()
        assertEquals("Hourly rate", o.label)
        assertEquals(95.0, o.original, 0.0)
        assertEquals(120.0, o.overridden, 0.0)
    }

    @Test
    fun `unchanged values leave no trail`() {
        val overrides = OverrideAudit.diff(
            baseline,
            mapOf("rate:hourly" to 95.0, "labor-hours:panel swap" to 8.0, "labor-cost:panel swap" to 760.0),
        )
        assertTrue(overrides.isEmpty())
    }

    @Test
    fun `a removed line is not an override — the work was cut, not repriced`() {
        val overrides = OverrideAudit.diff(baseline, mapOf("rate:hourly" to 95.0))
        assertTrue(overrides.isEmpty())
    }

    @Test
    fun `float noise inside epsilon does not count as an override`() {
        val overrides = OverrideAudit.diff(baseline, mapOf("rate:hourly" to 95.0000001))
        assertTrue(overrides.isEmpty())
    }

    @Test
    fun `multiple overrides are all recorded`() {
        val overrides = OverrideAudit.diff(
            baseline,
            mapOf("rate:hourly" to 110.0, "labor-hours:panel swap" to 10.0),
        )
        assertEquals(setOf("rate:hourly", "labor-hours:panel swap"), overrides.map { it.key }.toSet())
    }
}
