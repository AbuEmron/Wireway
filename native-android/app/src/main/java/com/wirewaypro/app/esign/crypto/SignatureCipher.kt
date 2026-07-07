package com.wirewaypro.app.esign.crypto

import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM authenticated encryption for the signature image at rest.
 *
 * This is the algorithm core ONLY — it takes a [SecretKey] and knows nothing
 * about where the key comes from. On device the key is a non-exportable
 * Android Keystore key (see [AndroidKeystoreKeyProvider]); in unit tests it's a
 * plain in-memory AES key. That split is deliberate: the round-trip is fully
 * testable on the JVM without an emulator, and the module drops into another app
 * by swapping only the key provider.
 *
 * Wire format (single byte array): [1-byte version][12-byte IV][ciphertext‖GCM tag].
 * GCM authenticates the ciphertext, so any tampering with the stored blob fails
 * the tag check on decrypt (throws) rather than returning corrupted bytes.
 */
object SignatureCipher {

    private const val VERSION: Byte = 1
    private const val IV_LEN = 12          // 96-bit nonce (GCM standard)
    private const val TAG_BITS = 128
    private const val TRANSFORM = "AES/GCM/NoPadding"

    /** Encrypt [plain] under [key]; returns a self-describing blob (see wire format). */
    fun encrypt(plain: ByteArray, key: SecretKey): ByteArray {
        // Android Keystore forbids caller-provided IVs on encrypt: init WITHOUT a
        // GCMParameterSpec and let the provider generate the IV, then read it back.
        val cipher = Cipher.getInstance(TRANSFORM).apply {
            init(Cipher.ENCRYPT_MODE, key)
        }
        val iv = cipher.iv
        require(iv.size == IV_LEN) { "Unexpected GCM IV length: ${iv.size}" }
        val ct = cipher.doFinal(plain)
        return ByteArray(1 + IV_LEN + ct.size).also { out ->
            out[0] = VERSION
            System.arraycopy(iv, 0, out, 1, IV_LEN)
            System.arraycopy(ct, 0, out, 1 + IV_LEN, ct.size)
        }
    }

    /**
     * Decrypt a blob produced by [encrypt] under the same [key]. Throws if the
     * version byte is unknown, the blob is truncated, or the GCM tag fails
     * (i.e. the ciphertext was altered or the wrong key was used).
     */
    fun decrypt(blob: ByteArray, key: SecretKey): ByteArray {
        require(blob.isNotEmpty() && blob[0] == VERSION) { "Unknown signature blob version" }
        require(blob.size > 1 + IV_LEN) { "Signature blob too short" }
        val iv = blob.copyOfRange(1, 1 + IV_LEN)
        val ct = blob.copyOfRange(1 + IV_LEN, blob.size)
        val cipher = Cipher.getInstance(TRANSFORM).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        }
        return cipher.doFinal(ct)
    }
}
