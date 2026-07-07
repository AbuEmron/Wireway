package com.wirewaypro.app.domain.model

/**
 * The user's selected Authority Having Jurisdiction (AHJ) — the place whose
 * electrical code and inspector actually govern their work. Progressive and
 * honest: [stateCode] is required; [county] and [city] refine it and are
 * optional (state alone is a valid selection).
 *
 * Saved to the user's profile (`user_jurisdictions`), offline-first, so every
 * estimate/job can be checked against *their* local rules rather than a
 * hardcoded national default. One active jurisdiction per user.
 *
 * This is deliberately NOT a compliance verdict — it only records WHERE the user
 * works. The adopted-edition baseline ([com.wirewaypro.app.domain.ahj.NecAdoptionTable])
 * and the honest coverage surface ([com.wirewaypro.app.domain.ahj.AhjCoverage])
 * are what turn a jurisdiction into shown-with-sources information.
 */
data class Jurisdiction(
    val id: String,
    val stateCode: String,
    val county: String? = null,
    val city: String? = null,
    /** How the selection was arrived at, for the honest "you confirmed this" trail. */
    val source: JurisdictionSource = JurisdictionSource.MANUAL,
    val createdAt: String? = null,
    val syncState: SyncState = SyncState.SYNCED,
) {
    /** "City, County, ST" — the deepest parts the user actually picked. */
    val displayLabel: String
        get() = listOfNotNull(
            city?.takeIf { it.isNotBlank() },
            county?.takeIf { it.isNotBlank() }?.let { if (it.endsWith("County", true)) it else "$it County" },
            stateCode,
        ).joinToString(", ")
}

/**
 * How a jurisdiction selection was set. Even a GPS pre-suggestion is only ever a
 * suggestion — the user always confirms, so a saved record is [MANUAL] or
 * [GPS_CONFIRMED], never an unconfirmed GPS guess.
 */
enum class JurisdictionSource {
    /** The user typed/picked it. */
    MANUAL,

    /** GPS pre-filled it and the user confirmed. */
    GPS_CONFIRMED,
}

/** Everything the picker can write. id == null → create (the user's first selection). */
data class JurisdictionInput(
    val id: String?,
    val stateCode: String,
    val county: String? = null,
    val city: String? = null,
    val source: JurisdictionSource = JurisdictionSource.MANUAL,
)
