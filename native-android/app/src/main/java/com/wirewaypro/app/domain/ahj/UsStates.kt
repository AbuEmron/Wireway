package com.wirewaypro.app.domain.ahj

/**
 * One US state / district / territory, identified by its 2-letter postal code.
 * This is the top level of an [com.wirewaypro.app.domain.model.Jurisdiction] —
 * the user always picks a state first (county and city/AHJ are optional refinements).
 *
 * The list is the canonical, finite set of adopting jurisdictions at the state
 * level. It is deliberately data, not an enum, so it can be iterated for the
 * picker and joined against [NecAdoptionTable] by [code].
 */
data class UsState(
    val code: String,
    val name: String,
)

/** The 50 states + D.C. + the five inhabited US territories, alphabetical by name. */
object UsStates {

    val all: List<UsState> = listOf(
        UsState("AL", "Alabama"),
        UsState("AK", "Alaska"),
        UsState("AZ", "Arizona"),
        UsState("AR", "Arkansas"),
        UsState("CA", "California"),
        UsState("CO", "Colorado"),
        UsState("CT", "Connecticut"),
        UsState("DE", "Delaware"),
        UsState("DC", "District of Columbia"),
        UsState("FL", "Florida"),
        UsState("GA", "Georgia"),
        UsState("HI", "Hawaii"),
        UsState("ID", "Idaho"),
        UsState("IL", "Illinois"),
        UsState("IN", "Indiana"),
        UsState("IA", "Iowa"),
        UsState("KS", "Kansas"),
        UsState("KY", "Kentucky"),
        UsState("LA", "Louisiana"),
        UsState("ME", "Maine"),
        UsState("MD", "Maryland"),
        UsState("MA", "Massachusetts"),
        UsState("MI", "Michigan"),
        UsState("MN", "Minnesota"),
        UsState("MS", "Mississippi"),
        UsState("MO", "Missouri"),
        UsState("MT", "Montana"),
        UsState("NE", "Nebraska"),
        UsState("NV", "Nevada"),
        UsState("NH", "New Hampshire"),
        UsState("NJ", "New Jersey"),
        UsState("NM", "New Mexico"),
        UsState("NY", "New York"),
        UsState("NC", "North Carolina"),
        UsState("ND", "North Dakota"),
        UsState("OH", "Ohio"),
        UsState("OK", "Oklahoma"),
        UsState("OR", "Oregon"),
        UsState("PA", "Pennsylvania"),
        UsState("RI", "Rhode Island"),
        UsState("SC", "South Carolina"),
        UsState("SD", "South Dakota"),
        UsState("TN", "Tennessee"),
        UsState("TX", "Texas"),
        UsState("UT", "Utah"),
        UsState("VT", "Vermont"),
        UsState("VA", "Virginia"),
        UsState("WA", "Washington"),
        UsState("WV", "West Virginia"),
        UsState("WI", "Wisconsin"),
        UsState("WY", "Wyoming"),
        // Inhabited territories — included so no user is locked out of picking
        // their jurisdiction. Their adopted edition is not yet mapped (see
        // NecAdoptionTable), which the coverage surface states honestly.
        UsState("PR", "Puerto Rico"),
        UsState("GU", "Guam"),
        UsState("VI", "U.S. Virgin Islands"),
        UsState("AS", "American Samoa"),
        UsState("MP", "Northern Mariana Islands"),
    )

    private val byCode = all.associateBy { it.code }

    /** Look up a state/territory by 2-letter code (case-insensitive), or null. */
    fun byCode(code: String?): UsState? = code?.let { byCode[it.trim().uppercase()] }

    /**
     * Resolve a state's postal code from a free-text name (e.g. from a reverse
     * geocode's admin-area). Case/space-insensitive exact match on the full name,
     * or null when it doesn't match a known state. Deterministic — no fuzzy AI.
     */
    fun codeForName(name: String?): String? {
        val needle = name?.trim()?.lowercase() ?: return null
        return all.firstOrNull { it.name.lowercase() == needle }?.code
    }
}
