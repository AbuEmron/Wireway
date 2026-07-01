package com.wirewaypro.app.domain.load

/** A load a homeowner might add. va = typical nameplate volt-amps (≈ 240 V × amps). */
data class NewLoad(val id: String, val label: String, val va: Double)

/**
 * NEC dwelling load calculations for "can my service handle a new load?" — ports the
 * web app's LoadAdvisor. Two methods:
 *  • 220.83 (calculated) for an existing dwelling adding a load.
 *  • 220.87 (metered) when a 15-min annual peak demand is available.
 * Educational estimate; a licensed load calc on the actual panel governs.
 */
object LoadAdvisor {

    val NEW_LOADS: List<NewLoad> = listOf(
        NewLoad("ev48", "EV charger — Level 2, 48 A", 11_520.0),
        NewLoad("ev32", "EV charger — 32 A", 7_680.0),
        NewLoad("heatpump", "Heat pump / mini-split (≈3 ton)", 7_200.0),
        NewLoad("ac", "Central AC (≈3 ton)", 7_200.0),
        NewLoad("range", "Electric range / oven", 8_000.0),
        NewLoad("dryer", "Electric dryer", 5_000.0),
        NewLoad("wh", "Electric water heater", 4_500.0),
        NewLoad("hottub", "Hot tub / spa (50 A)", 9_600.0),
    )

    val SERVICE_SIZES: List<Int> = listOf(100, 125, 150, 200, 320, 400)

    data class Result(
        val totalVa: Double,
        val amps: Double,
        val serviceAmps: Int,
        val fits: Boolean,
        val withinEightyPct: Boolean,
        val recommendation: String,
    )

    /**
     * NEC 220.83 calculated method. "Other" loads (general lighting at 3 VA/ft², the two
     * small-appliance + laundry circuits at 1500 VA each, and existing fixed appliances
     * plus the new load) take 100% of the first 8 kVA + 40% of the remainder; the largest
     * of heating vs. AC is added at 100%.
     */
    fun calculate(
        serviceAmps: Int,
        sqft: Int,
        existingFixedVa: Double,
        newLoadVa: Double,
        heatOrAcVa: Double,
    ): Result {
        val general = 3.0 * sqft.coerceAtLeast(0)
        val smallAppliance = 1_500.0 * 3 // two small-appliance + one laundry circuit
        val other = general + smallAppliance + existingFixedVa.coerceAtLeast(0.0) + newLoadVa.coerceAtLeast(0.0)
        val otherDemand = if (other <= 8_000.0) other else 8_000.0 + 0.4 * (other - 8_000.0)
        val totalVa = otherDemand + heatOrAcVa.coerceAtLeast(0.0)
        return result(totalVa, serviceAmps)
    }

    /** NEC 220.87 metered method: existing 15-min annual peak × 1.25 + the new load. */
    fun fromMeteredPeak(serviceAmps: Int, peakKw: Double, newLoadVa: Double): Result {
        val totalVa = peakKw.coerceAtLeast(0.0) * 1000.0 * 1.25 + newLoadVa.coerceAtLeast(0.0)
        return result(totalVa, serviceAmps)
    }

    private fun result(totalVa: Double, serviceAmps: Int): Result {
        val amps = totalVa / 240.0
        val fits = amps <= serviceAmps
        val within80 = amps <= serviceAmps * 0.8
        val rec = when {
            within80 -> "Fits comfortably — your ${serviceAmps} A service has headroom."
            fits -> "It fits, but you're over 80% of the ${serviceAmps} A service. Consider load management to keep margin."
            else -> {
                val next = if (amps <= 160) 200 else 400
                "Over capacity for a ${serviceAmps} A service. Upgrade to ${next} A, or add a load-management device (NEC 625.42 / 750) to share capacity."
            }
        }
        return Result(totalVa, amps, serviceAmps, fits, within80, rec)
    }
}
