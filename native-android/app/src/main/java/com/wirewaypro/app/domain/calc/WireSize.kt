package com.wirewaypro.app.domain.calc

/**
 * Minimum conductor size for a load — NEC Table 310.16 with the continuous-load rule.
 *
 * The conductor is sized to the termination temperature column that the equipment is
 * listed for (60 °C for most breakers/lugs ≤100 A; 75 °C for larger commercial gear).
 * Continuous loads (on ≥3 hours) require the conductor's allowable ampacity to be at
 * least 125% of the load — 210.19(A) / 215.2(A).
 *
 * Deterministic and offline. Educational — the electrician confirms terminations,
 * derating, and the OCPD against the adopted code.
 */
object WireSize {

    data class Result(
        val gauge: Awg,
        val gaugeAmpacity: Int,
        val requiredAmpacity: Double,
        val material: ConductorMaterial,
        val temp: TempRating,
        val continuous: Boolean,
    )

    /**
     * Smallest listed conductor whose Table 310.16 ampacity (at [temp]) meets the load.
     * Returns null if no size in the table is large enough (load beyond 500 kcmil here).
     *
     * @param loadAmps the connected load in amps.
     * @param continuous true if the load runs 3 hours or more (applies the 125% factor).
     */
    fun minimumSize(
        loadAmps: Double,
        material: ConductorMaterial,
        temp: TempRating,
        continuous: Boolean = false,
    ): Result? {
        if (loadAmps <= 0.0) return null
        val required = if (continuous) loadAmps * 1.25 else loadAmps
        for (gauge in AmpacityTable.gauges(material)) {
            val amp = AmpacityTable.ampacity(material, gauge, temp) ?: continue
            if (amp >= required) {
                return Result(
                    gauge = gauge,
                    gaugeAmpacity = amp,
                    requiredAmpacity = required,
                    material = material,
                    temp = temp,
                    continuous = continuous,
                )
            }
        }
        return null
    }
}
