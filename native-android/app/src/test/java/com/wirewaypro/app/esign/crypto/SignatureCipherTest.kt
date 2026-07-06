package com.wirewaypro.app.esign.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.AEADBadTagException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * AES-256-GCM signature encryption. On device the key lives in the Android
 * Keystore; here we inject a plain in-memory AES key so the round-trip and the
 * authentication guarantees are exercised without an emulator.
 */
class SignatureCipherTest {

    private fun aesKey(): SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    @Test
    fun encryptThenDecrypt_roundTrips() {
        val key = aesKey()
        val signaturePng = ByteArray(4096) { (it * 7 % 256).toByte() } // stand-in for the PNG bytes
        val blob = SignatureCipher.encrypt(signaturePng, key)
        val back = SignatureCipher.decrypt(blob, key)
        assertArrayEquals("decrypted signature must equal the original bytes", signaturePng, back)
    }

    @Test
    fun ciphertext_isNotPlaintext_andIsNonDeterministic() {
        val key = aesKey()
        val plain = "signature".toByteArray()
        val a = SignatureCipher.encrypt(plain, key)
        val b = SignatureCipher.encrypt(plain, key)
        // Random IV per call → two encryptions of the same input differ…
        assertFalse("IV must randomize ciphertext", a.contentEquals(b))
        // …but both still decrypt back to the same plaintext.
        assertArrayEquals(plain, SignatureCipher.decrypt(a, key))
        assertArrayEquals(plain, SignatureCipher.decrypt(b, key))
    }

    @Test
    fun decrypt_withWrongKey_fails() {
        val blob = SignatureCipher.encrypt("secret".toByteArray(), aesKey())
        assertThrows(AEADBadTagException::class.java) {
            SignatureCipher.decrypt(blob, aesKey()) // different key
        }
    }

    @Test
    fun decrypt_ofTamperedBlob_failsAuthentication() {
        val key = aesKey()
        val blob = SignatureCipher.encrypt("secret".toByteArray(), key)
        blob[blob.size - 1] = (blob[blob.size - 1] + 1).toByte() // flip a ciphertext/tag byte
        assertThrows(AEADBadTagException::class.java) { SignatureCipher.decrypt(blob, key) }
    }

    @Test
    fun decrypt_ofTruncatedBlob_isRejected() {
        val key = aesKey()
        val blob = SignatureCipher.encrypt("secret".toByteArray(), key)
        assertThrows(IllegalArgumentException::class.java) {
            SignatureCipher.decrypt(blob.copyOf(5), key)
        }
    }

    @Test
    fun blob_carriesVersionByte() {
        val blob = SignatureCipher.encrypt("x".toByteArray(), aesKey())
        assertTrue("version-prefixed wire format", blob[0].toInt() == 1)
    }
}
