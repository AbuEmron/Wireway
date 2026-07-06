package com.wirewaypro.app.domain.ahj

import com.wirewaypro.app.domain.model.Jurisdiction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The coverage surface is where the honesty rule is enforced: state what we know,
 * loudly state what we don't, and NEVER present an unmapped area as compliant.
 * These tests assert those invariants hold for every selectable jurisdiction.
 */
class AhjCoverageTest {

    private fun jurisdiction(state: String, county: String? = null, city: String? = null) =
        Jurisdiction(id = "j1", stateCode = state, county = county, city = city)

    @Test
    fun nullJurisdiction_promptsToSetOne_notAPass() {
        val cov = AhjCoverage.of(null)
        assertEquals(CoverageConfidence.NONE, cov.confidence)
        assertNull(cov.edition)
        assertNull(cov.sourceLine)
        assertEquals(AhjCoverage.AMENDMENTS_NOT_MAPPED, cov.amendmentsLine)
    }

    @Test
    fun verifiedState_showsEdition_withSource_andAmendmentsCaveat() {
        val cov = AhjCoverage.of(jurisdiction("TX", county = "Travis", city = "Austin"))
        assertEquals(2023, cov.edition)
        assertEquals(CoverageConfidence.BASELINE, cov.confidence)
        assertTrue(cov.editionLine.contains("NEC 2023"))
        assertTrue(cov.editionLine.contains("verified", ignoreCase = true))
        assertNotNull("a shown edition must carry its provenance", cov.sourceLine)
        assertTrue(cov.headline.contains("Austin"))
        // The amendment caveat is ALWAYS present — a verified edition is not a pass.
        assertTrue(cov.amendmentsLine.contains("not yet mapped"))
    }

    @Test
    fun unverifiedState_flagsUncertainty_andCarriesTheNote() {
        val cov = AhjCoverage.of(jurisdiction("NC"))
        assertEquals(CoverageConfidence.AMBIGUOUS, cov.confidence)
        assertTrue(cov.editionLine.contains("UNVERIFIED", ignoreCase = true))
        assertNotNull(cov.note)
        assertTrue(cov.note!!.contains("delayed", ignoreCase = true))
    }

    @Test
    fun localOnlyState_saysSetLocally_notAnEdition() {
        val cov = AhjCoverage.of(jurisdiction("MS"))
        assertNull(cov.edition)
        assertTrue(cov.editionLine.contains("locally", ignoreCase = true))
        assertEquals(CoverageConfidence.AMBIGUOUS, cov.confidence)
    }

    @Test
    fun notMappedTerritory_showsAbsence_notAGuess() {
        val cov = AhjCoverage.of(jurisdiction("PR"))
        assertNull(cov.edition)
        assertNull(cov.sourceLine)
        assertEquals(CoverageConfidence.NONE, cov.confidence)
        assertTrue(cov.editionLine.contains("not yet mapped", ignoreCase = true))
    }

    @Test
    fun noJurisdiction_everReadsAsCompliantOrPass_forAnyState() {
        // The core trust invariant, checked across the WHOLE selectable set:
        // nothing in the rendered coverage may assert compliance or a pass, and
        // the amendment gap is always disclosed.
        val banned = listOf("compliant", "will pass", "passes inspection", "code compliant", "guaranteed")
        UsStates.all.forEach { state ->
            val cov = AhjCoverage.of(jurisdiction(state.code))
            val haystack = listOfNotNull(cov.headline, cov.editionLine, cov.amendmentsLine, cov.note)
                .joinToString(" ").lowercase()
            banned.forEach { phrase ->
                assertFalse("${state.code} coverage must never say \"$phrase\"", haystack.contains(phrase))
            }
            assertTrue(
                "${state.code} must always disclose the amendment gap",
                cov.amendmentsLine.contains("not yet mapped"),
            )
        }
    }

    @Test
    fun stateOnlySelection_isEnough_toGetABaseline() {
        // A user who picks only their state (no county/city) still gets the real,
        // defensible edition — the whole point of the state baseline.
        val cov = AhjCoverage.of(jurisdiction("CO"))
        assertEquals(2023, cov.edition)
        assertEquals(CoverageConfidence.BASELINE, cov.confidence)
    }
}
