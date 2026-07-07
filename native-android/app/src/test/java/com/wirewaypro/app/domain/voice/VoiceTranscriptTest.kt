package com.wirewaypro.app.domain.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Locks the deterministic accumulation of live dictation into scope text. */
class VoiceTranscriptTest {

    @Test
    fun `partial results show but do not commit`() {
        val t = VoiceTranscript().withPartial("install twelve rec")
        assertEquals("install twelve rec", t.display)
        assertEquals("", t.finalized)
    }

    @Test
    fun `committing a final clears the partial and locks the text`() {
        val t = VoiceTranscript()
            .withPartial("install twelve recessed")
            .commitFinal("install twelve recessed lights")
        assertEquals("install twelve recessed lights", t.finalized)
        assertEquals("", t.partial)
        assertEquals("install twelve recessed lights", t.display)
    }

    @Test
    fun `sequential finals join with single spaces`() {
        val t = VoiceTranscript()
            .commitFinal("install twelve recessed lights")
            .commitFinal("and three dimmers")
        assertEquals("install twelve recessed lights and three dimmers", t.display)
    }

    @Test
    fun `partial after a final trails the committed text`() {
        val t = VoiceTranscript()
            .commitFinal("kitchen remodel")
            .withPartial("add a dedicated")
        assertEquals("kitchen remodel add a dedicated", t.display)
    }

    @Test
    fun `blank final is ignored — no stray spaces`() {
        val t = VoiceTranscript().commitFinal("panel swap").commitFinal("   ")
        assertEquals("panel swap", t.display)
    }

    @Test
    fun `empty transcript reports empty`() {
        assertTrue(VoiceTranscript().isEmpty)
        assertFalse(VoiceTranscript().commitFinal("x").isEmpty)
    }

    @Test
    fun `append onto empty field takes the dictation as-is`() {
        assertEquals("install a subpanel", VoiceTranscript.append("", "install a subpanel"))
    }

    @Test
    fun `append onto a typed field keeps the typed text on its own line`() {
        assertEquals(
            "existing scope\nadd two bathroom fans",
            VoiceTranscript.append("existing scope", "add two bathroom fans"),
        )
    }

    @Test
    fun `append of blank dictation is a no-op`() {
        assertEquals("existing scope", VoiceTranscript.append("existing scope", "   "))
    }
}
