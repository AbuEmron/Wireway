package com.wirewaypro.app.domain.calc

/**
 * Conduit fill — NEC Chapter 9.
 *  • Table 1 — maximum fill percentage: 1 conductor 53%, 2 conductors 31%,
 *    3 or more 40%.
 *  • Table 4 — internal cross-sectional area of the raceway (per type & trade size).
 *  • Table 5 — cross-sectional area of each conductor (THHN/THWN-2 here; see [Awg]).
 *
 * Fill is judged on the sum of conductor areas vs. the allowed percentage of the
 * raceway's internal area. Deterministic and offline; educational.
 */
object ConduitFill {

    /** Raceway types carried here, with Chapter 9 Table 4 internal areas (in²). */
    enum class ConduitType(val label: String) {
        EMT("EMT"),
        PVC40("PVC Sch 40"),
        RMC("Rigid Metal (RMC)"),
    }

    /** A trade size with its total internal area (Table 4, "Total 100%" column, in²). */
    data class TradeSize(val label: String, val totalAreaSqIn: Double)

    private val EMT = listOf(
        TradeSize("1/2\"", 0.304),
        TradeSize("3/4\"", 0.533),
        TradeSize("1\"", 0.864),
        TradeSize("1-1/4\"", 1.496),
        TradeSize("1-1/2\"", 2.036),
        TradeSize("2\"", 3.356),
        TradeSize("2-1/2\"", 5.858),
        TradeSize("3\"", 8.846),
        TradeSize("3-1/2\"", 11.545),
        TradeSize("4\"", 14.753),
    )

    private val PVC40 = listOf(
        TradeSize("1/2\"", 0.285),
        TradeSize("3/4\"", 0.508),
        TradeSize("1\"", 0.832),
        TradeSize("1-1/4\"", 1.453),
        TradeSize("1-1/2\"", 1.986),
        TradeSize("2\"", 3.291),
        TradeSize("2-1/2\"", 4.695),
        TradeSize("3\"", 7.268),
        TradeSize("3-1/2\"", 9.737),
        TradeSize("4\"", 12.554),
    )

    private val RMC = listOf(
        TradeSize("1/2\"", 0.314),
        TradeSize("3/4\"", 0.549),
        TradeSize("1\"", 0.887),
        TradeSize("1-1/4\"", 1.526),
        TradeSize("1-1/2\"", 2.071),
        TradeSize("2\"", 3.408),
        TradeSize("2-1/2\"", 4.866),
        TradeSize("3\"", 7.499),
        TradeSize("3-1/2\"", 10.010),
        TradeSize("4\"", 12.882),
    )

    fun tradeSizes(type: ConduitType): List<TradeSize> = when (type) {
        ConduitType.EMT -> EMT
        ConduitType.PVC40 -> PVC40
        ConduitType.RMC -> RMC
    }

    /** A group of identical conductors going into the raceway. */
    data class ConductorSpec(val gauge: Awg, val count: Int)

    /** Max fill fraction from Chapter 9 Table 1, by total conductor count. */
    fun maxFillFraction(totalConductors: Int): Double = when {
        totalConductors <= 0 -> 0.0
        totalConductors == 1 -> 0.53
        totalConductors == 2 -> 0.31
        else -> 0.40
    }

    /** Total THHN/THWN-2 area of all conductors (Table 5), in². */
    fun conductorArea(conductors: List<ConductorSpec>): Double =
        conductors.sumOf { it.gauge.areaThhn * it.count }

    fun conductorCount(conductors: List<ConductorSpec>): Int =
        conductors.sumOf { it.count }

    data class Result(
        val conductorAreaSqIn: Double,
        val conductorCount: Int,
        val maxFillFraction: Double,
        val allowedAreaSqIn: Double, // raceway internal area × max fill fraction
        val fillPercent: Double,     // conductor area ÷ raceway internal area × 100
        val withinLimit: Boolean,
    )

    /** Evaluate fill for a specific raceway trade size. */
    fun evaluate(
        conductors: List<ConductorSpec>,
        type: ConduitType,
        tradeSize: TradeSize,
    ): Result {
        val area = conductorArea(conductors)
        val count = conductorCount(conductors)
        val frac = maxFillFraction(count)
        val allowed = tradeSize.totalAreaSqIn * frac
        val fillPct = if (tradeSize.totalAreaSqIn > 0) area / tradeSize.totalAreaSqIn * 100.0 else 0.0
        return Result(
            conductorAreaSqIn = area,
            conductorCount = count,
            maxFillFraction = frac,
            allowedAreaSqIn = allowed,
            fillPercent = fillPct,
            withinLimit = area <= allowed + 1e-9,
        )
    }

    /** Smallest trade size of [type] whose Table 1 allowance fits the conductors. */
    fun minimumConduit(
        conductors: List<ConductorSpec>,
        type: ConduitType,
    ): TradeSize? {
        val area = conductorArea(conductors)
        val frac = maxFillFraction(conductorCount(conductors))
        return tradeSizes(type).firstOrNull { it.totalAreaSqIn * frac >= area - 1e-9 }
    }
}
