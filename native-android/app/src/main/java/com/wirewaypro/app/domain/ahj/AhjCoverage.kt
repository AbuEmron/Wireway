package com.wirewaypro.app.domain.ahj

import com.wirewaypro.app.domain.model.Jurisdiction

/**
 * How strong the coverage we can honestly show for a jurisdiction is. Drives the
 * UI's tint/icon — it is NOT a compliance verdict.
 */
enum class CoverageConfidence {
    /** We have a corroborated adopted edition (state baseline). The trust magnet. */
    BASELINE,

    /** We know the jurisdiction but the adopted edition is ambiguous/uncorroborated. */
    AMBIGUOUS,

    /** No jurisdiction selected, or nothing mapped for it yet. */
    NONE,
}

/**
 * The honest, deterministic coverage summary for a selected jurisdiction — the
 * exact thing shown on the picker and on an estimate/job.
 *
 * ## The one rule this type enforces
 * It states WHAT WE KNOW and, loudly, WHAT WE DON'T. It never says an install is
 * "compliant" or "will pass" — this batch maps the adopted STATE edition only,
 * not local amendments or per-circuit rules. Absence of data is rendered as
 * absence ([amendmentsLine] always says amendments are not yet mapped), so an
 * unmapped area can never read as a pass. See WIREWAY_VISION_2.0 AHJ section.
 */
data class AhjCoverage(
    val jurisdictionLabel: String,
    val headline: String,
    val editionLine: String,
    val edition: Int?,
    val status: AdoptionStatus?,
    val confidence: CoverageConfidence,
    /** Provenance for the shown edition — null when no edition is shown. */
    val sourceLine: String?,
    /** Always present: this batch does not map local amendments anywhere. */
    val amendmentsLine: String,
    /** Per-jurisdiction nuance (a delay, a split by occupancy, a source conflict). */
    val note: String?,
) {
    companion object {

        /** The invariant, honest amendment-coverage line for this batch. */
        const val AMENDMENTS_NOT_MAPPED =
            "Local amendments: not yet mapped for your area — confirm with your AHJ."

        private const val NO_JURISDICTION_HEADLINE = "No jurisdiction selected"

        /**
         * Build the honest coverage report for [jurisdiction] against the
         * [table]. A null jurisdiction yields the "set your jurisdiction" state.
         * Pure and deterministic — same inputs, same output, offline.
         */
        fun of(
            jurisdiction: Jurisdiction?,
            table: NecAdoptionTable = NecAdoptionTable,
        ): AhjCoverage {
            if (jurisdiction == null) {
                return AhjCoverage(
                    jurisdictionLabel = "—",
                    headline = NO_JURISDICTION_HEADLINE,
                    editionLine = "Set your jurisdiction to check against the code your inspector enforces.",
                    edition = null,
                    status = null,
                    confidence = CoverageConfidence.NONE,
                    sourceLine = null,
                    amendmentsLine = AMENDMENTS_NOT_MAPPED,
                    note = null,
                )
            }

            val label = jurisdiction.displayLabel
            val record = table.forState(jurisdiction.stateCode)
            val headline = "Checked against $label"

            // Unknown code (shouldn't happen via the picker) — treat as unmapped.
            if (record == null) {
                return AhjCoverage(
                    jurisdictionLabel = label,
                    headline = headline,
                    editionLine = "Adopted NEC edition: not yet mapped for this jurisdiction.",
                    edition = null,
                    status = AdoptionStatus.NOT_MAPPED,
                    confidence = CoverageConfidence.NONE,
                    sourceLine = null,
                    amendmentsLine = AMENDMENTS_NOT_MAPPED,
                    note = null,
                )
            }

            val editionLine: String
            val confidence: CoverageConfidence
            val sourceLine: String?
            when (record.status) {
                AdoptionStatus.VERIFIED -> {
                    val eff = record.effectiveDate?.let { " · in effect since $it" } ?: ""
                    editionLine = "Adopted: NEC ${record.edition} (verified)$eff"
                    confidence = CoverageConfidence.BASELINE
                    sourceLine = NecAdoptionTable.SOURCE_LINE
                }
                AdoptionStatus.UNVERIFIED -> {
                    editionLine = if (record.edition != null) {
                        "Best-known adopted edition: NEC ${record.edition} — UNVERIFIED (not corroborated)."
                    } else {
                        "Adopted edition: unverified for your state — not corroborated."
                    }
                    confidence = CoverageConfidence.AMBIGUOUS
                    sourceLine = NecAdoptionTable.SOURCE_LINE
                }
                AdoptionStatus.LOCAL_ONLY -> {
                    editionLine = "No statewide NEC adoption — the edition is set locally (city/county)."
                    confidence = CoverageConfidence.AMBIGUOUS
                    sourceLine = NecAdoptionTable.SOURCE_LINE
                }
                AdoptionStatus.NOT_MAPPED -> {
                    editionLine = "Adopted NEC edition: not yet mapped for your state/territory."
                    confidence = CoverageConfidence.NONE
                    sourceLine = null
                }
            }

            return AhjCoverage(
                jurisdictionLabel = label,
                headline = headline,
                editionLine = editionLine,
                edition = record.edition,
                status = record.status,
                confidence = confidence,
                sourceLine = sourceLine,
                amendmentsLine = AMENDMENTS_NOT_MAPPED,
                note = record.note,
            )
        }
    }
}
