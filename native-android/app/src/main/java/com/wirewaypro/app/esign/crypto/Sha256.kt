package com.wirewaypro.app.esign.crypto

import java.io.File
import java.security.MessageDigest

/**
 * SHA-256 hashing — the integrity floor for the e-signature module.
 *
 * The sealed PDF's hash is recorded in the audit trail at sealing time; [verify]
 * recomputes it later and confirms it still matches (tamper check). Deterministic
 * and dependency-free (pure JVM), so it runs identically in a unit test, on the
 * device, and — byte-for-byte — anywhere a third party re-checks the same file.
 *
 * Self-contained: nothing here touches the Android framework, so the whole crypto
 * package can be lifted into another app unchanged.
 */
object Sha256 {

    /** Lowercase 64-char hex SHA-256 of [bytes]. */
    fun hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

    /** Lowercase 64-char hex SHA-256 of a file's exact bytes (streamed, any size). */
    fun hexOfFile(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().toHex()
    }

    /**
     * True iff [file]'s current bytes still hash to [expectedHex]. Constant-time
     * comparison so a verify() call never leaks where a mismatch starts.
     */
    fun matchesFile(file: File, expectedHex: String): Boolean =
        constantTimeEquals(hexOfFile(file), expectedHex.trim().lowercase())

    private fun ByteArray.toHex(): String {
        val out = StringBuilder(size * 2)
        for (b in this) {
            val v = b.toInt() and 0xFF
            out.append(HEX[v ushr 4])
            out.append(HEX[v and 0x0F])
        }
        return out.toString()
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }

    private val HEX = "0123456789abcdef".toCharArray()
}
