package com.wirewaypro.app.domain.calc

/**
 * Conductor ampacity derating — NEC 2023.
 *  • Ambient temperature correction, Table 310.15(B)(1) (30 °C base).
 *  • Conductor bundling (more than 3 current-carrying conductors) adjustment,
 *    Table 310.15(C)(1).
 *
 * Derated ampacity = base ampacity × ambient factor × bundling factor. The corrected
 * value is what the conductor may actually carry in the real install; the electrician
 * then still respects the 60/75 °C termination limit separately.
 *
 * Pure, deterministic, offline. Educational — verify against the adopted code.
 */
object Derating {

    /**
     * One ambient band from Table 310.15(B)(1): applies when ambient ≤ [maxC].
     * Factors are per temperature column; null where the code lists no value (the
     * conductor can't be used at that ambient for that insulation rating).
     */
    private data class AmbientBand(
        val maxC: Int,
        val f60: Double?,
        val f75: Double?,
        val f90: Double?,
    )

    // Ascending by upper bound. The first band whose maxC ≥ ambient applies.
    private val AMBIENT_BANDS = listOf(
        AmbientBand(10, 1.29, 1.20, 1.15),
        AmbientBand(15, 1.22, 1.15, 1.12),
        AmbientBand(20, 1.15, 1.11, 1.08),
        AmbientBand(25, 1.08, 1.05, 1.04),
        AmbientBand(30, 1.00, 1.00, 1.00),
        AmbientBand(35, 0.91, 0.94, 0.96),
        AmbientBand(40, 0.82, 0.88, 0.91),
        AmbientBand(45, 0.71, 0.82, 0.87),
        AmbientBand(50, 0.58, 0.75, 0.82),
        AmbientBand(55, 0.41, 0.67, 0.76),
        AmbientBand(60, null, 0.58, 0.71),
        AmbientBand(65, null, 0.47, 0.65),
        AmbientBand(70, null, 0.33, 0.58),
        AmbientBand(75, null, null, 0.50),
        AmbientBand(80, null, null, 0.41),
        AmbientBand(85, null, null, 0.29),
    )

    /**
     * Ambient temperature correction factor for the insulation temperature column,
     * Table 310.15(B)(1). Returns null if the ambient is off the table (too hot for
     * that insulation).
     */
    fun ambientFactor(temp: TempRating, ambientC: Int): Double? {
        val band = AMBIENT_BANDS.firstOrNull { ambientC <= it.maxC } ?: return null
        return when (temp) {
            TempRating.C60 -> band.f60
            TempRating.C75 -> band.f75
            TempRating.C90 -> band.f90
        }
    }

    /**
     * Adjustment factor for the number of current-carrying conductors bundled together,
     * Table 310.15(C)(1). 1–3 conductors = no adjustment.
     */
    fun bundlingFactor(currentCarrying: Int): Double = when {
        currentCarrying <= 3 -> 1.00
        currentCarrying <= 6 -> 0.80
        currentCarrying <= 9 -> 0.70
        currentCarrying <= 20 -> 0.50
        currentCarrying <= 30 -> 0.45
        currentCarrying <= 40 -> 0.40
        else -> 0.35
    }

    data class Result(
        val baseAmpacity: Double,
        val ambientFactor: Double,
        val bundlingFactor: Double,
        val deratedAmpacity: Double,
    )

    /**
     * Apply both corrections to a base ampacity. Returns null only if the ambient is
     * off the table for the chosen insulation column.
     */
    fun derate(
        baseAmpacity: Double,
        temp: TempRating,
        ambientC: Int,
        currentCarrying: Int,
    ): Result? {
        val amb = ambientFactor(temp, ambientC) ?: return null
        val bundle = bundlingFactor(currentCarrying)
        return Result(
            baseAmpacity = baseAmpacity,
            ambientFactor = amb,
            bundlingFactor = bundle,
            deratedAmpacity = baseAmpacity * amb * bundle,
        )
    }
}
