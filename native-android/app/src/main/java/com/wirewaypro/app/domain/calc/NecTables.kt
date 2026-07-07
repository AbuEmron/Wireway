package com.wirewaypro.app.domain.calc

/**
 * Shared, deterministic NEC table data for the calculator engine — the single source
 * of truth every calculator reads from. No UI, no framework, no AI: pure published
 * values so the same inputs always produce the same, defensible output, offline.
 *
 * Values are transcribed from the National Electrical Code (NFPA 70), 2023 edition:
 *  • Table 310.16 — allowable ampacities (30 °C ambient, ≤3 current-carrying conductors)
 *  • Chapter 9, Table 5 — conductor cross-sectional area (THHN/THWN-2)
 *  • Chapter 9, Table 8 — conductor properties (area in circular mils)
 *
 * Educational engineering tool. The electrician confirms the final design against the
 * adopted code and local amendments.
 */

/** Conductor material — changes both ampacity and the voltage-drop resistivity K. */
enum class ConductorMaterial(val label: String) {
    COPPER("Copper"),
    ALUMINUM("Aluminum"),
}

/** Termination/insulation temperature column of Table 310.16. */
enum class TempRating(val label: String, val celsius: Int) {
    C60("60 °C", 60),
    C75("75 °C", 75),
    C90("90 °C", 90),
}

/**
 * The conductor set, smallest area first — residential sizes through the
 * commercial/industrial feeder range (kcmil to 1000). Each carries its Table 8
 * circular-mil area and Table 5 THHN/THWN-2 cross-sectional area so the
 * conduit-fill and voltage-drop calculators share one definition.
 *
 * NOTE: sizes above 500 kcmil intentionally have no Table 310.16 rows yet
 * (see [AmpacityTable]) — the ampacity/derating tools list only sizes whose
 * ampacities are transcribed and table-verified; conduit fill and voltage drop
 * work from the exact areas below.
 */
enum class Awg(
    val label: String,
    val cmil: Int,      // Ch.9 Table 8 — area in circular mils
    val areaThhn: Double // Ch.9 Table 5 — approx. area in² (THHN/THWN-2)
) {
    AWG14("14 AWG", 4_110, 0.0097),
    AWG12("12 AWG", 6_530, 0.0133),
    AWG10("10 AWG", 10_380, 0.0211),
    AWG8("8 AWG", 16_510, 0.0366),
    AWG6("6 AWG", 26_240, 0.0507),
    AWG4("4 AWG", 41_740, 0.0824),
    AWG3("3 AWG", 52_620, 0.0973),
    AWG2("2 AWG", 66_360, 0.1158),
    AWG1("1 AWG", 83_690, 0.1562),
    AWG1_0("1/0 AWG", 105_600, 0.1855),
    AWG2_0("2/0 AWG", 133_100, 0.2223),
    AWG3_0("3/0 AWG", 167_800, 0.2679),
    AWG4_0("4/0 AWG", 211_600, 0.3237),
    KCMIL250("250 kcmil", 250_000, 0.3970),
    KCMIL300("300 kcmil", 300_000, 0.4608),
    KCMIL350("350 kcmil", 350_000, 0.5242),
    KCMIL400("400 kcmil", 400_000, 0.5863),
    KCMIL500("500 kcmil", 500_000, 0.7073),
    KCMIL600("600 kcmil", 600_000, 0.8676),
    KCMIL700("700 kcmil", 700_000, 0.9887),
    KCMIL750("750 kcmil", 750_000, 1.0496),
    KCMIL800("800 kcmil", 800_000, 1.1085),
    KCMIL900("900 kcmil", 900_000, 1.2311),
    KCMIL1000("1000 kcmil", 1_000_000, 1.3478),
}

/**
 * NEC Table 310.16 allowable ampacities. Keyed [material][gauge] → (60, 75, 90 °C).
 * A null entry means that size isn't listed for that material (e.g. 14 AWG aluminum).
 */
object AmpacityTable {

    // Triple order: 60 °C, 75 °C, 90 °C.
    private val COPPER: Map<Awg, Triple<Int, Int, Int>> = mapOf(
        Awg.AWG14 to Triple(15, 20, 25),
        Awg.AWG12 to Triple(20, 25, 30),
        Awg.AWG10 to Triple(30, 35, 40),
        Awg.AWG8 to Triple(40, 50, 55),
        Awg.AWG6 to Triple(55, 65, 75),
        Awg.AWG4 to Triple(70, 85, 95),
        Awg.AWG3 to Triple(85, 100, 110),
        Awg.AWG2 to Triple(95, 115, 130),
        Awg.AWG1 to Triple(110, 130, 145),
        Awg.AWG1_0 to Triple(125, 150, 170),
        Awg.AWG2_0 to Triple(145, 175, 195),
        Awg.AWG3_0 to Triple(165, 200, 225),
        Awg.AWG4_0 to Triple(195, 230, 260),
        Awg.KCMIL250 to Triple(215, 255, 290),
        Awg.KCMIL350 to Triple(260, 310, 350),
        Awg.KCMIL500 to Triple(320, 380, 430),
    )

    private val ALUMINUM: Map<Awg, Triple<Int, Int, Int>> = mapOf(
        Awg.AWG12 to Triple(15, 20, 25),
        Awg.AWG10 to Triple(25, 30, 35),
        Awg.AWG8 to Triple(35, 40, 45),
        Awg.AWG6 to Triple(40, 50, 55),
        Awg.AWG4 to Triple(55, 65, 75),
        Awg.AWG3 to Triple(65, 75, 85),
        Awg.AWG2 to Triple(75, 90, 100),
        Awg.AWG1 to Triple(85, 100, 115),
        Awg.AWG1_0 to Triple(100, 120, 135),
        Awg.AWG2_0 to Triple(115, 135, 150),
        Awg.AWG3_0 to Triple(130, 155, 175),
        Awg.AWG4_0 to Triple(150, 180, 205),
        Awg.KCMIL250 to Triple(170, 205, 230),
        Awg.KCMIL350 to Triple(210, 250, 280),
        Awg.KCMIL500 to Triple(260, 310, 350),
    )

    /** Base (30 °C, ≤3 CCC) allowable ampacity, or null if the size isn't listed. */
    fun ampacity(material: ConductorMaterial, gauge: Awg, temp: TempRating): Int? {
        val row = when (material) {
            ConductorMaterial.COPPER -> COPPER[gauge]
            ConductorMaterial.ALUMINUM -> ALUMINUM[gauge]
        } ?: return null
        return when (temp) {
            TempRating.C60 -> row.first
            TempRating.C75 -> row.second
            TempRating.C90 -> row.third
        }
    }

    /** Gauges available for a material, smallest first. */
    fun gauges(material: ConductorMaterial): List<Awg> =
        Awg.entries.filter { ampacity(material, it, TempRating.C75) != null }
}
