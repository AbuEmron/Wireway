package com.wirewaypro.app.domain.ahj

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The adopted-NEC-edition baseline is a TRUST artifact: these tests pin that the
 * table returns the exact CITED value for known states, that every state/territory
 * the picker can select has an honest record, and that certainty is never faked.
 * If a sourced value is ever changed, one of these assertions changes with it —
 * the data can't silently drift.
 */
class NecAdoptionTableTest {

    @Test
    fun lookup_returnsCitedVerifiedEditions() {
        // Each of these is corroborated by BOTH public references in the table's docs.
        NecAdoptionTable.forState("CA").let {
            assertNotNull(it); assertEquals(2020, it!!.edition)
            assertEquals(AdoptionStatus.VERIFIED, it.status)
            assertEquals("2023-01-01", it.effectiveDate)
        }
        NecAdoptionTable.forState("TX").let {
            assertEquals(2023, it!!.edition); assertEquals(AdoptionStatus.VERIFIED, it.status)
        }
        NecAdoptionTable.forState("NY").let {
            assertEquals(2017, it!!.edition); assertEquals(AdoptionStatus.VERIFIED, it.status)
        }
    }

    @Test
    fun lookup_isCaseAndSpaceInsensitive() {
        assertEquals(NecAdoptionTable.forState("TX"), NecAdoptionTable.forState(" tx "))
    }

    @Test
    fun ambiguousStates_areUnverified_neverFakedCertain() {
        // North Carolina: 2023 adoption delayed indefinitely (Session Law 2025-2) —
        // sources disagree, so it must be UNVERIFIED with the delay called out.
        NecAdoptionTable.forState("NC").let {
            assertEquals(AdoptionStatus.UNVERIFIED, it!!.status)
            assertTrue("NC note must explain the delay", it.note!!.contains("delayed", ignoreCase = true))
            assertTrue("isVerified must be false for an unverified record", !it.isVerified)
        }
        // Utah / New Hampshire / Pennsylvania: sources conflict → unverified.
        listOf("UT", "NH", "PA", "HI").forEach {
            assertEquals("$it must be unverified", AdoptionStatus.UNVERIFIED, NecAdoptionTable.forState(it)!!.status)
        }
    }

    @Test
    fun localOnlyStates_haveNoStatewideEdition() {
        listOf("MS", "MO").forEach { code ->
            val rec = NecAdoptionTable.forState(code)!!
            assertEquals("$code should be local-only", AdoptionStatus.LOCAL_ONLY, rec.status)
            assertNull("$code must not fake a statewide edition", rec.edition)
        }
    }

    @Test
    fun territories_areNotMapped_notFaked() {
        listOf("PR", "GU", "VI", "AS", "MP").forEach { code ->
            val rec = NecAdoptionTable.forState(code)!!
            assertEquals("$code should be not-mapped", AdoptionStatus.NOT_MAPPED, rec.status)
            assertNull("$code must not fake an edition", rec.edition)
        }
    }

    @Test
    fun everyPickableJurisdiction_hasARecord() {
        // No user can select a state/territory the table can't answer for.
        UsStates.all.forEach { state ->
            assertNotNull("no adoption record for ${state.code}", NecAdoptionTable.forState(state.code))
        }
        assertEquals("table size must match the state list", UsStates.all.size, NecAdoptionTable.all().size)
    }

    @Test
    fun everyVerifiedRecord_hasEditionAndEffectiveDate() {
        NecAdoptionTable.all().filter { it.status == AdoptionStatus.VERIFIED }.forEach { rec ->
            assertNotNull("${rec.stateCode} verified but no edition", rec.edition)
            assertNotNull("${rec.stateCode} verified but no effective date", rec.effectiveDate)
        }
    }

    @Test
    fun noDuplicateStateCodes() {
        val codes = NecAdoptionTable.all().map { it.stateCode }
        assertEquals("duplicate state codes in the table", codes.size, codes.toSet().size)
    }

    @Test
    fun unknownCode_returnsNull() {
        assertNull(NecAdoptionTable.forState("ZZ"))
        assertNull(NecAdoptionTable.forState(null))
    }
}
