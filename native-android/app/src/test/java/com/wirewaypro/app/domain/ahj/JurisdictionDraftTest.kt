package com.wirewaypro.app.domain.ahj

import com.wirewaypro.app.domain.model.JurisdictionSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The picker's progressive rules, tested purely: state is the only requirement,
 * county/city are optional refinements gated behind a chosen state, and switching
 * states clears the finer parts that belonged to the old one.
 */
class JurisdictionDraftTest {

    @Test
    fun emptyDraft_isInvalid_andGatesCountyCity() {
        val d = JurisdictionDraft()
        assertFalse(d.isValid)
        assertFalse(d.countyEnabled)
        assertFalse(d.cityEnabled)
        assertNull("no state → nothing to save", d.toInput())
    }

    @Test
    fun stateAlone_isValid_andSaveable() {
        val d = JurisdictionDraft().withState("TX")
        assertTrue(d.isValid)
        assertTrue(d.countyEnabled)
        val input = d.toInput()!!
        assertEquals("TX", input.stateCode)
        assertNull(input.county)
        assertNull(input.city)
    }

    @Test
    fun countyAndCity_areOptionalRefinements() {
        val d = JurisdictionDraft().withState("TX").copy(county = " Travis ", city = " Austin ")
        val input = d.toInput()!!
        assertEquals("Travis", input.county) // trimmed
        assertEquals("Austin", input.city)
    }

    @Test
    fun blankCountyCity_normalizeToNull() {
        val input = JurisdictionDraft().withState("CA").copy(county = "   ", city = "").toInput()!!
        assertNull(input.county)
        assertNull(input.city)
    }

    @Test
    fun switchingState_clearsOldCountyAndCity() {
        val d = JurisdictionDraft().withState("TX").copy(county = "Travis", city = "Austin")
        val switched = d.withState("CA")
        assertEquals("CA", switched.stateCode)
        assertEquals("", switched.county)
        assertEquals("", switched.city)
    }

    @Test
    fun reselectingSameState_keepsCountyAndCity() {
        val d = JurisdictionDraft().withState("TX").copy(county = "Travis", city = "Austin")
        val same = d.withState("TX")
        assertEquals("Travis", same.county)
        assertEquals("Austin", same.city)
    }

    @Test
    fun stateCode_isUppercasedOnSave() {
        assertEquals("TX", JurisdictionDraft(stateCode = "tx").toInput()!!.stateCode)
    }

    @Test
    fun gpsFlag_recordsConfirmedSource_notAnUnconfirmedGuess() {
        val manual = JurisdictionDraft().withState("TX").toInput()!!
        assertEquals(JurisdictionSource.MANUAL, manual.source)
        val gps = JurisdictionDraft(fromGps = true).withState("TX").copy(fromGps = true).toInput()!!
        assertEquals(JurisdictionSource.GPS_CONFIRMED, gps.source)
    }
}
