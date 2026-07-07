package com.wirewaypro.app.domain.pricing

import kotlin.math.roundToInt

/**
 * A typical billed hourly-rate band for one US state — an OFFLINE, instant
 * starting point for the contractor's default rate. No AI, no network.
 *
 * Honesty contract: these are approximations for a licensed electrical
 * contractor's CLIENT-FACING billed rate (which carries overhead, insurance,
 * vehicle, tools, licensing and profit — well above an electrician's wage).
 * They are starting points the contractor edits, never a floor or ceiling,
 * and the UI must always label them as approximate.
 */
data class RateBand(
    val stateCode: String,
    val stateName: String,
    val low: Int,
    val typical: Int,
    val high: Int,
)

/**
 * Per-state billed-rate bands derived from two public inputs:
 *  1. A national homeowner-facing billed band of roughly $85–$150/hr with a
 *     typical figure around $110/hr (industry surveys of electrician rates).
 *  2. BLS OES median electrician wages by state (approximate, rounded), used
 *     only as a RELATIVE index — state band = national band × (state wage /
 *     national median wage ≈ $30/hr).
 *
 * Keeping the basis to two documented inputs makes the numbers checkable and
 * avoids fake precision; output is rounded to the nearest $5.
 */
object RegionalLaborRates {

    private const val NATIONAL_LOW = 85.0
    private const val NATIONAL_TYPICAL = 110.0
    private const val NATIONAL_HIGH = 150.0
    private const val NATIONAL_MEDIAN_WAGE = 30.0

    private data class StateWage(val name: String, val wage: Double)

    /** Approximate BLS OES median hourly wage for electricians (47-2111), by state. */
    private val states: Map<String, StateWage> = mapOf(
        "AL" to StateWage("Alabama", 24.5),
        "AK" to StateWage("Alaska", 38.5),
        "AZ" to StateWage("Arizona", 27.5),
        "AR" to StateWage("Arkansas", 24.0),
        "CA" to StateWage("California", 36.5),
        "CO" to StateWage("Colorado", 29.5),
        "CT" to StateWage("Connecticut", 34.5),
        "DE" to StateWage("Delaware", 30.0),
        "DC" to StateWage("District of Columbia", 38.0),
        "FL" to StateWage("Florida", 24.5),
        "GA" to StateWage("Georgia", 27.0),
        "HI" to StateWage("Hawaii", 38.5),
        "ID" to StateWage("Idaho", 27.0),
        "IL" to StateWage("Illinois", 40.0),
        "IN" to StateWage("Indiana", 30.0),
        "IA" to StateWage("Iowa", 29.0),
        "KS" to StateWage("Kansas", 27.5),
        "KY" to StateWage("Kentucky", 27.5),
        "LA" to StateWage("Louisiana", 27.0),
        "ME" to StateWage("Maine", 29.5),
        "MD" to StateWage("Maryland", 30.5),
        "MA" to StateWage("Massachusetts", 37.5),
        "MI" to StateWage("Michigan", 32.0),
        "MN" to StateWage("Minnesota", 37.0),
        "MS" to StateWage("Mississippi", 25.0),
        "MO" to StateWage("Missouri", 32.5),
        "MT" to StateWage("Montana", 32.0),
        "NE" to StateWage("Nebraska", 26.5),
        "NV" to StateWage("Nevada", 33.5),
        "NH" to StateWage("New Hampshire", 29.0),
        "NJ" to StateWage("New Jersey", 37.5),
        "NM" to StateWage("New Mexico", 27.5),
        "NY" to StateWage("New York", 38.5),
        "NC" to StateWage("North Carolina", 25.5),
        "ND" to StateWage("North Dakota", 32.0),
        "OH" to StateWage("Ohio", 28.5),
        "OK" to StateWage("Oklahoma", 27.5),
        "OR" to StateWage("Oregon", 43.0),
        "PA" to StateWage("Pennsylvania", 32.5),
        "RI" to StateWage("Rhode Island", 32.0),
        "SC" to StateWage("South Carolina", 25.5),
        "SD" to StateWage("South Dakota", 26.5),
        "TN" to StateWage("Tennessee", 26.5),
        "TX" to StateWage("Texas", 27.5),
        "UT" to StateWage("Utah", 29.0),
        "VT" to StateWage("Vermont", 29.0),
        "VA" to StateWage("Virginia", 28.5),
        "WA" to StateWage("Washington", 40.5),
        "WV" to StateWage("West Virginia", 30.0),
        "WI" to StateWage("Wisconsin", 33.0),
        "WY" to StateWage("Wyoming", 30.0),
    )

    /** Every state's suggested billed-rate band, sorted by state name (for pickers). */
    fun allStates(): List<RateBand> =
        states.keys.mapNotNull { forState(it) }.sortedBy { it.stateName }

    /** The suggested billed-rate band for a state code, or null if unknown. */
    fun forState(code: String?): RateBand? {
        val c = code?.trim()?.uppercase() ?: return null
        val s = states[c] ?: return null
        val idx = s.wage / NATIONAL_MEDIAN_WAGE
        return RateBand(
            stateCode = c,
            stateName = s.name,
            low = round5(NATIONAL_LOW * idx),
            typical = round5(NATIONAL_TYPICAL * idx),
            high = round5(NATIONAL_HIGH * idx),
        )
    }

    /**
     * Finds a US state in free-form address text ("12 Main St, Binghamton, NY
     * 13901" → "NY"). Prefers the LAST standalone two-letter code (addresses
     * end "City, ST zip"), then falls back to full state names (longest first,
     * so "West Virginia" wins over "Virginia").
     */
    fun detectState(address: String?): String? {
        if (address.isNullOrBlank()) return null
        val upper = address.uppercase()
        Regex("(?:^|[\\s,.])([A-Z]{2})(?=$|[\\s,.0-9])").findAll(upper)
            .map { it.groupValues[1] }
            .lastOrNull { states.containsKey(it) }
            ?.let { return it }
        return states.entries
            .sortedByDescending { it.value.name.length }
            .firstOrNull { upper.contains(it.value.name.uppercase()) }
            ?.key
    }

    private fun round5(v: Double): Int = (v / 5.0).roundToInt() * 5
}
