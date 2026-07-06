package com.wirewaypro.app.domain.ahj

import com.wirewaypro.app.domain.model.JurisdictionInput
import com.wirewaypro.app.domain.model.JurisdictionSource

/**
 * The pure, framework-free state of the jurisdiction picker as the user fills it
 * in — progressively: state first, then optional county, then optional city/AHJ.
 * Kept out of the ViewModel so its rules (state required; county/city optional;
 * state alone is valid) are unit-testable with no Android.
 *
 * The picker is deliberately permissive downward: choosing a state alone yields a
 * valid, saveable selection. County and city only narrow it.
 */
data class JurisdictionDraft(
    val id: String? = null,
    val stateCode: String? = null,
    val county: String = "",
    val city: String = "",
    /** GPS pre-filled this draft (until the user edits/confirms it). */
    val fromGps: Boolean = false,
) {
    /** State is the only requirement — county/city are optional refinements. */
    val isValid: Boolean get() = !stateCode.isNullOrBlank()

    /** County can only be entered once a state is chosen (progressive disclosure). */
    val countyEnabled: Boolean get() = isValid

    /** City/AHJ can only be entered once a state is chosen. */
    val cityEnabled: Boolean get() = isValid

    /**
     * Turn the draft into a saveable [JurisdictionInput], or null if no state is
     * chosen yet. Blank county/city normalize to null; the source records whether
     * a GPS suggestion was ultimately confirmed by the user.
     */
    fun toInput(): JurisdictionInput? {
        val state = stateCode?.trim()?.uppercase()?.takeIf { it.isNotBlank() } ?: return null
        return JurisdictionInput(
            id = id,
            stateCode = state,
            county = county.trim().ifBlank { null },
            city = city.trim().ifBlank { null },
            source = if (fromGps) JurisdictionSource.GPS_CONFIRMED else JurisdictionSource.MANUAL,
        )
    }

    /**
     * Choosing a new state clears the county/city that belonged to the old one —
     * they don't carry across states. Re-selecting the same state is a no-op.
     */
    fun withState(code: String): JurisdictionDraft =
        if (code.equals(stateCode, ignoreCase = true)) this
        else copy(stateCode = code, county = "", city = "")
}
