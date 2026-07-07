package com.wirewaypro.app.esign.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * The signature-image-at-rest round-trip. [SignatureVault] is the file-level API
 * the app uses; here the Keystore is replaced by an in-memory key provider so the
 * full encrypt-to-file / decrypt-from-file path runs on the JVM.
 */
class SignatureVaultTest {

    private class InMemoryKeyProvider : EsignKeyProvider {
        private val key: SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        override fun getOrCreateKey(): SecretKey = key
    }

    @Test
    fun encryptToFile_thenDecryptFile_roundTrips() {
        val vault = SignatureVault(InMemoryKeyProvider())
        val png = ByteArray(2048) { (it % 256).toByte() }
        val dest = Files.createTempDirectory("esign-vault").resolve("sig.enc").toFile()

        vault.encryptToFile(png, dest)

        // On-disk blob is NOT the plaintext PNG (encrypted at rest)…
        val onDisk = dest.readBytes()
        assertFalse("stored blob must not equal the plaintext PNG", onDisk.contentEquals(png))
        assertTrue("blob is larger than plaintext (version+IV+tag)", onDisk.size > png.size)

        // …and decrypts back to exactly the original signature bytes.
        assertArrayEquals(png, vault.decryptFile(dest))
    }

    @Test
    fun differentVault_key_cannotDecrypt() {
        val png = "signature".toByteArray()
        val dest = Files.createTempDirectory("esign-vault2").resolve("sig.enc").toFile()
        SignatureVault(InMemoryKeyProvider()).encryptToFile(png, dest)

        // A vault with a different key (a different device/keystore) can't read it.
        var failed = false
        try {
            SignatureVault(InMemoryKeyProvider()).decryptFile(dest)
        } catch (e: Exception) {
            failed = true
        }
        assertTrue("a foreign key must not decrypt the signature", failed)
    }
}
