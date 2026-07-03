package com.wirewaypro.app.domain.audit

import kotlin.math.abs

/**
 * Manual-override audit (doctrine: overrides are always allowed — the
 * contractor is in control — but they leave a trail so the estimate stays
 * defensible). Pure domain: capture the DERIVED numbers a builder session was
 * seeded with (template labor, resolved default rate, takeoff quantities),
 * then diff them against what actually got saved.
 */
object OverrideAudit {

    /** A calculated/derived value the session started from. */
    data class Derived(
        /** Stable key, e.g. "rate:hourly", "labor-hours:Install VFD". */
        val key: String,
        /** Human label for the trail, e.g. "Hourly rate". */
        val label: String,
        val value: Double,
    )

    /** One recorded override: the derived original vs what the contractor saved. */
    data class Override(
        val key: String,
        val label: String,
        val original: Double,
        val overridden: Double,
    )

    /**
     * Diffs the seeded baseline against the saved values. Only keys present in
     * BOTH count: a removed line isn't an override (the work was cut), and a
     * user-added line was never derived. [epsilon] absorbs float noise.
     */
    fun diff(
        baseline: List<Derived>,
        saved: Map<String, Double>,
        epsilon: Double = 0.005,
    ): List<Override> = baseline.mapNotNull { d ->
        val now = saved[d.key] ?: return@mapNotNull null
        if (abs(now - d.value) > epsilon) Override(d.key, d.label, d.value, now) else null
    }
}
