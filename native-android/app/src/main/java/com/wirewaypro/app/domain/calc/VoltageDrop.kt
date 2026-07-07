package com.wirewaypro.app.domain.calc

/**
 * Voltage drop over a conductor run, by the NEC-informed approximate method used in
 * Informational Note to 210.19(A): Vd = (phase constant) × K × I × L ÷ CM.
 *
 *  • K = resistivity in ohm-circular-mils per foot: 12.9 copper, 21.2 aluminum.
 *  • CM = conductor area in circular mils (Chapter 9, Table 8).
 *  • L = one-way length in feet (the ×2 / ×√3 accounts for the return path).
 *  • Phase constant: 2 for single-phase, √3 (≈1.732) for three-phase.
 *
 * The NEC recommends (does not mandate) ≤3% on a branch circuit and ≤5% combined
 * feeder + branch. Deterministic and offline; educational.
 */
object VoltageDrop {

    const val K_COPPER = 12.9
    const val K_ALUMINUM = 21.2

    /** NEC informational-note recommendation: 3% branch, 5% total. */
    const val RECOMMENDED_BRANCH_PCT = 3.0

    enum class Phase(val label: String, val constant: Double) {
        SINGLE("Single-phase", 2.0),
        THREE("Three-phase", 1.7320508075688772),
    }

    private fun k(material: ConductorMaterial) =
        if (material == ConductorMaterial.COPPER) K_COPPER else K_ALUMINUM

    data class Result(
        val voltageDrop: Double,     // volts lost over the run
        val percent: Double,         // % of system voltage
        val endVoltage: Double,      // voltage at the load
        val withinRecommended: Boolean, // ≤ 3%
    )

    /**
     * Voltage drop for a specific conductor.
     *
     * @param systemVolts nominal circuit voltage (e.g. 120, 240, 208).
     * @param loadAmps current drawn.
     * @param lengthFtOneWay one-way run length in feet.
     */
    fun calculate(
        gauge: Awg,
        material: ConductorMaterial,
        systemVolts: Double,
        loadAmps: Double,
        lengthFtOneWay: Double,
        phase: Phase = Phase.SINGLE,
    ): Result {
        val vd = phase.constant * k(material) * loadAmps * lengthFtOneWay / gauge.cmil
        val pct = if (systemVolts > 0) vd / systemVolts * 100.0 else 0.0
        return Result(
            voltageDrop = vd,
            percent = pct,
            endVoltage = systemVolts - vd,
            withinRecommended = pct <= RECOMMENDED_BRANCH_PCT,
        )
    }

    /**
     * Smallest conductor that keeps the drop at or under [targetPct]. Handy for "upsize
     * for the long run" — returns null if even 500 kcmil can't meet it.
     */
    fun minimumSizeForDrop(
        material: ConductorMaterial,
        systemVolts: Double,
        loadAmps: Double,
        lengthFtOneWay: Double,
        targetPct: Double = RECOMMENDED_BRANCH_PCT,
        phase: Phase = Phase.SINGLE,
    ): Awg? = AmpacityTable.gauges(material).firstOrNull { gauge ->
        calculate(gauge, material, systemVolts, loadAmps, lengthFtOneWay, phase).percent <= targetPct
    }

    /** Circular mils needed to hit exactly [targetPct] — the sizing rearrangement. */
    fun requiredCircularMils(
        material: ConductorMaterial,
        systemVolts: Double,
        loadAmps: Double,
        lengthFtOneWay: Double,
        targetPct: Double = RECOMMENDED_BRANCH_PCT,
        phase: Phase = Phase.SINGLE,
    ): Double {
        val allowedVd = systemVolts * targetPct / 100.0
        if (allowedVd <= 0) return Double.POSITIVE_INFINITY
        return phase.constant * k(material) * loadAmps * lengthFtOneWay / allowedVd
    }
}
