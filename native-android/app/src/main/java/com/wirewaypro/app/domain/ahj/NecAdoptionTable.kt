package com.wirewaypro.app.domain.ahj

/**
 * How confident we are in a state's adopted-NEC-edition record. This is a
 * SOURCING confidence — NOT an "is your install compliant" claim, and NOT the
 * `verified-by-outcome` signal the AHJ standard earns later from logged
 * inspections. It only says how well the *adopted edition* is corroborated.
 */
enum class AdoptionStatus {
    /** Two independent public adoption references agree on the edition. */
    VERIFIED,

    /** Sources conflict, are stale, or the edition is genuinely ambiguous. Never claim certainty. */
    UNVERIFIED,

    /** No statewide adoption — the edition is set locally (city/county). Honestly "not statewide". */
    LOCAL_ONLY,

    /** We haven't mapped this jurisdiction's adopted edition yet (e.g. territories). */
    NOT_MAPPED,
}

/**
 * One state/territory's currently-adopted NEC (NFPA 70) edition.
 *
 * Every field is defensible: [edition] is the code year, [status] is how well
 * it's corroborated, [effectiveDate] (ISO, when known) is when it took effect,
 * and [note] carries any nuance (a delay, a split by occupancy, a conflict
 * between sources). Absence of certainty is encoded as [AdoptionStatus.UNVERIFIED]
 * or a null [edition] — never faked into a confident number.
 */
data class StateNecAdoption(
    val stateCode: String,
    val edition: Int?,
    val status: AdoptionStatus,
    val effectiveDate: String? = null,
    val note: String? = null,
) {
    /** True only when we have a corroborated, unambiguous adopted edition to show. */
    val isVerified: Boolean get() = status == AdoptionStatus.VERIFIED && edition != null
}

/**
 * The finite, public STATE-LEVEL adopted-NEC-edition baseline — so every user
 * gets a real, defensible edition the moment they pick their state, before any
 * county/city amendments are mapped.
 *
 * ## Provenance (this is a trust artifact — read WIREWAY_VISION_2.0 "AHJ compliance")
 * The data is CROSS-REFERENCED from two public, independent NEC-adoption
 * references (both track state statutes/administrative rules that reference an
 * NFPA 70 edition):
 *   1. Mike Holt Enterprises — "NEC Adoption List" (mikeholt.com/necadoptionlist.php)
 *   2. JADE Learning — "U.S. State NEC Adoptions" (jadelearning.com), page-stated
 *      last-updated 2025-07-18.
 * Both retrieved 2026-07-05. Nothing here is from model memory.
 *
 * ## Verification rule (deterministic, honest)
 *   - [AdoptionStatus.VERIFIED]   — both sources agree on the edition.
 *   - [AdoptionStatus.UNVERIFIED] — sources disagree, or a documented delay /
 *     occupancy-split / staleness makes it ambiguous. We show the best-known
 *     edition but flag it as not-corroborated and tell the user to confirm.
 *   - [AdoptionStatus.LOCAL_ONLY] — both sources indicate no statewide adoption.
 *   - [AdoptionStatus.NOT_MAPPED] — not yet researched (territories).
 *
 * The UI must NEVER present any of this as "your job is compliant." It states the
 * adopted edition, its source + as-of date, and that local amendments are not yet
 * mapped. Absence of data is shown as absence.
 */
object NecAdoptionTable {

    const val SOURCE_PRIMARY = "Mike Holt Enterprises — NEC Adoption List"
    const val SOURCE_SECONDARY = "JADE Learning — U.S. State NEC Adoptions"

    /** Best-defensible as-of date: JADE's page-stated last-updated. */
    const val AS_OF = "2025-07-18"

    /** When these two references were fetched for this build. */
    const val RETRIEVED = "2026-07-05"

    /** A one-line, always-shown provenance string for the coverage surface. */
    const val SOURCE_LINE =
        "Sources: $SOURCE_PRIMARY + $SOURCE_SECONDARY (as of $AS_OF, retrieved $RETRIEVED)"

    private val entries: List<StateNecAdoption> = listOf(
        // ── VERIFIED: both references agree on the edition ──────────────────────
        StateNecAdoption("AL", 2020, AdoptionStatus.VERIFIED, "2022-07-01"),
        StateNecAdoption("AK", 2020, AdoptionStatus.VERIFIED, "2020-04-16"),
        StateNecAdoption("AR", 2020, AdoptionStatus.VERIFIED, "2022-08-01"),
        StateNecAdoption("CA", 2020, AdoptionStatus.VERIFIED, "2023-01-01"),
        StateNecAdoption("CO", 2023, AdoptionStatus.VERIFIED, "2023-08-01"),
        StateNecAdoption("CT", 2020, AdoptionStatus.VERIFIED, "2022-10-01"),
        StateNecAdoption("DE", 2020, AdoptionStatus.VERIFIED, "2021-09-01"),
        StateNecAdoption("FL", 2020, AdoptionStatus.VERIFIED, "2023-12-31"),
        StateNecAdoption("GA", 2023, AdoptionStatus.VERIFIED, "2025-01-01"),
        StateNecAdoption("ID", 2023, AdoptionStatus.VERIFIED, "2024-07-01"),
        StateNecAdoption("IA", 2023, AdoptionStatus.VERIFIED, "2025-07-01"),
        StateNecAdoption("KY", 2023, AdoptionStatus.VERIFIED, "2025-01-01"),
        StateNecAdoption("LA", 2020, AdoptionStatus.VERIFIED, "2023-01-01"),
        StateNecAdoption("ME", 2023, AdoptionStatus.VERIFIED, "2024-07-01"),
        StateNecAdoption("MD", 2020, AdoptionStatus.VERIFIED, "2023-05-29"),
        StateNecAdoption("MA", 2023, AdoptionStatus.VERIFIED, "2023-02-17"),
        StateNecAdoption("MI", 2023, AdoptionStatus.VERIFIED, "2024-03-12"),
        StateNecAdoption("MN", 2023, AdoptionStatus.VERIFIED, "2023-07-01"),
        StateNecAdoption("MT", 2020, AdoptionStatus.VERIFIED, "2022-06-10"),
        StateNecAdoption("NE", 2023, AdoptionStatus.VERIFIED, "2024-08-01"),
        StateNecAdoption("NJ", 2020, AdoptionStatus.VERIFIED, "2022-09-06"),
        StateNecAdoption("NM", 2020, AdoptionStatus.VERIFIED, "2023-03-28"),
        StateNecAdoption("NY", 2017, AdoptionStatus.VERIFIED, "2020-05-12"),
        StateNecAdoption("ND", 2023, AdoptionStatus.VERIFIED, "2024-07-01"),
        StateNecAdoption("OH", 2023, AdoptionStatus.VERIFIED, "2024-03-01"),
        StateNecAdoption("OK", 2023, AdoptionStatus.VERIFIED, "2024-09-14"),
        StateNecAdoption("OR", 2023, AdoptionStatus.VERIFIED, "2023-10-01"),
        StateNecAdoption("RI", 2020, AdoptionStatus.VERIFIED, "2022-02-01"),
        StateNecAdoption("SC", 2020, AdoptionStatus.VERIFIED, "2023-01-01"),
        StateNecAdoption("SD", 2023, AdoptionStatus.VERIFIED, "2024-11-12"),
        StateNecAdoption("TN", 2017, AdoptionStatus.VERIFIED, "2018-10-01"),
        StateNecAdoption("TX", 2023, AdoptionStatus.VERIFIED, "2023-09-01"),
        StateNecAdoption("VT", 2020, AdoptionStatus.VERIFIED, "2022-04-15"),
        StateNecAdoption("VA", 2020, AdoptionStatus.VERIFIED, "2024-01-18"),
        StateNecAdoption("WA", 2023, AdoptionStatus.VERIFIED, "2024-04-01"),
        StateNecAdoption("WV", 2020, AdoptionStatus.VERIFIED, "2022-08-01"),
        StateNecAdoption("WI", 2017, AdoptionStatus.VERIFIED, "2018-08-01"),
        StateNecAdoption("WY", 2023, AdoptionStatus.VERIFIED, "2023-07-01"),

        // ── LOCAL_ONLY: both references indicate no statewide adoption ──────────
        StateNecAdoption(
            "MS", null, AdoptionStatus.LOCAL_ONLY,
            note = "No statewide NEC adoption — the code is set locally (city/county). " +
                "Confirm the edition with your local AHJ.",
        ),
        StateNecAdoption(
            "MO", null, AdoptionStatus.LOCAL_ONLY,
            note = "No statewide NEC adoption — the code is set locally (city/county). " +
                "Confirm the edition with your local AHJ.",
        ),

        // ── UNVERIFIED: sources conflict, are stale, or the status is ambiguous ─
        StateNecAdoption(
            "AZ", null, AdoptionStatus.UNVERIFIED,
            note = "No consistent statewide adoption — Arizona is largely adopted locally " +
                "(one source lists NEC 2017). Confirm with your AHJ.",
        ),
        StateNecAdoption(
            "DC", 2014, AdoptionStatus.UNVERIFIED,
            note = "One source lists NEC 2014, which may be stale. Confirm the current D.C. edition with your AHJ.",
        ),
        StateNecAdoption(
            "HI", 2020, AdoptionStatus.UNVERIFIED,
            note = "Sources disagree (NEC 2020 vs 2023). Confirm the current Hawaii edition with your AHJ.",
        ),
        StateNecAdoption(
            "IL", null, AdoptionStatus.UNVERIFIED,
            note = "No consistent statewide adoption — Illinois is largely adopted locally " +
                "(Chicago enforces its own electrical code). Confirm with your AHJ.",
        ),
        StateNecAdoption(
            "IN", 2017, AdoptionStatus.UNVERIFIED,
            note = "Adoption varies by occupancy/use in Indiana (one source lists 2008/2017). Confirm with your AHJ.",
        ),
        StateNecAdoption(
            "KS", null, AdoptionStatus.UNVERIFIED,
            note = "No statewide adoption — Kansas is adopted locally (one source lists NEC 2017). Confirm with your AHJ.",
        ),
        StateNecAdoption(
            "NV", null, AdoptionStatus.UNVERIFIED,
            note = "No statewide adoption — Nevada is adopted locally (Clark County/Las Vegas set their own; " +
                "one source lists NEC 2017). Confirm with your AHJ.",
        ),
        StateNecAdoption(
            "NH", 2023, AdoptionStatus.UNVERIFIED, "2025-07-01",
            note = "Sources disagree (NEC 2020 vs 2023); one lists NEC 2023 effective 2025-07-01. Confirm with your AHJ.",
        ),
        StateNecAdoption(
            "NC", 2020, AdoptionStatus.UNVERIFIED,
            note = "North Carolina's move to NEC 2023 was delayed indefinitely (Session Law 2025-2); the 2017/2020 " +
                "editions remain in effect (2017 residential, 2020 otherwise). Confirm with your AHJ.",
        ),
        StateNecAdoption(
            "PA", 2020, AdoptionStatus.UNVERIFIED, "2026-01-01",
            note = "Sources disagree (NEC 2017 vs 2020); one lists NEC 2020 effective 2026-01-01. Confirm with your AHJ.",
        ),
        StateNecAdoption(
            "UT", 2023, AdoptionStatus.UNVERIFIED, "2025-07-01",
            note = "Sources disagree (NEC 2020 vs 2023); one lists NEC 2023 effective 2025-07-01. Confirm with your AHJ.",
        ),

        // ── NOT_MAPPED: territories not yet researched (shown honestly as absent) ─
        StateNecAdoption("PR", null, AdoptionStatus.NOT_MAPPED, note = "Adopted edition not yet mapped for this territory."),
        StateNecAdoption("GU", null, AdoptionStatus.NOT_MAPPED, note = "Adopted edition not yet mapped for this territory."),
        StateNecAdoption("VI", null, AdoptionStatus.NOT_MAPPED, note = "Adopted edition not yet mapped for this territory."),
        StateNecAdoption("AS", null, AdoptionStatus.NOT_MAPPED, note = "Adopted edition not yet mapped for this territory."),
        StateNecAdoption("MP", null, AdoptionStatus.NOT_MAPPED, note = "Adopted edition not yet mapped for this territory."),
    )

    private val byCode: Map<String, StateNecAdoption> = entries.associateBy { it.stateCode }

    /**
     * The adopted-edition record for a state code, or null if the code is unknown.
     * A known-but-unmapped jurisdiction returns a [StateNecAdoption] with
     * [AdoptionStatus.NOT_MAPPED] — callers still get an honest record, never a
     * fabricated edition.
     */
    fun forState(code: String?): StateNecAdoption? =
        code?.let { byCode[it.trim().uppercase()] }

    /** Every record (for tests and any full-table views). */
    fun all(): List<StateNecAdoption> = entries
}
