package com.wirewaypro.app.esign.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * The integrity floor: the sealed document's hash must be STABLE for identical
 * bytes and must CHANGE if a single byte changes (the tamper guarantee that
 * verify() relies on).
 */
class Sha256Test {

    @Test
    fun hash_isStable_forIdenticalBytes() {
        val doc = "Proposal #1042 — total $4,850.00".toByteArray()
        assertEquals(Sha256.hex(doc), Sha256.hex(doc.copyOf()))
    }

    @Test
    fun hash_isCorrectLength_andLowercaseHex() {
        val h = Sha256.hex("anything".toByteArray())
        assertEquals(64, h.length)
        assertTrue("must be lowercase hex", h.matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun hash_changes_whenDocumentChanges_tamper() {
        val original = "Total: \$4,850.00".toByteArray()
        val tampered = "Total: \$8,450.00".toByteArray() // a re-priced proposal
        assertNotEquals(
            "a changed document must not keep the same fingerprint",
            Sha256.hex(original),
            Sha256.hex(tampered),
        )
    }

    @Test
    fun hash_changes_onSingleByteFlip() {
        val bytes = ByteArray(1000) { (it % 251).toByte() }
        val before = Sha256.hex(bytes)
        bytes[500] = (bytes[500] + 1).toByte()
        assertNotEquals(before, Sha256.hex(bytes))
    }

    @Test
    fun fileHash_matchesByteHash_andDetectsTampering() {
        val f = Files.createTempFile("esign-seal", ".pdf").toFile()
        try {
            val bytes = "sealed pdf bytes v1".toByteArray()
            f.writeBytes(bytes)
            val recorded = Sha256.hexOfFile(f)

            // hexOfFile must equal hashing the same bytes in memory.
            assertEquals(Sha256.hex(bytes), recorded)
            // Intact file verifies against the recorded hash…
            assertTrue(Sha256.matchesFile(f, recorded))
            // …and a later edit is detected (verify integrity → tampered).
            f.writeBytes("sealed pdf bytes v1 (edited)".toByteArray())
            assertFalse("tampered sealed PDF must fail verification", Sha256.matchesFile(f, recorded))
        } finally {
            f.delete()
        }
    }

    @Test
    fun matchesFile_isCaseAndWhitespaceTolerant_onExpected() {
        val f: File = Files.createTempFile("esign", ".bin").toFile()
        try {
            f.writeBytes("x".toByteArray())
            val h = Sha256.hexOfFile(f)
            assertTrue(Sha256.matchesFile(f, "  ${h.uppercase()}  "))
        } finally {
            f.delete()
        }
    }
}
