package com.wirewaypro.app.esign.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Supplies the AES-256 key that [SignatureCipher] uses to encrypt signature
 * images, backed by the **Android Keystore**. The key material is generated
 * inside the Keystore and is non-exportable — the app can encrypt/decrypt with
 * it but can never read the raw bytes, so a stolen database or a rooted-device
 * dump of app storage does not yield the signatures.
 *
 * Interface + keystore split so tests inject an in-memory key (see
 * [SignatureCipher] tests) and another app supplies its own provider.
 */
interface EsignKeyProvider {
    /** The (device-bound) AES key for signature encryption, created on first use. */
    fun getOrCreateKey(): SecretKey
}

class AndroidKeystoreKeyProvider(
    private val alias: String = DEFAULT_ALIAS,
) : EsignKeyProvider {

    override fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (ks.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        gen.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                // Not tied to biometric/lock-screen: signatures must be sealable in
                // the field on any device state (offline-first, never lose a record).
                .setUserAuthenticationRequired(false)
                .build(),
        )
        return gen.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val DEFAULT_ALIAS = "wireway_esign_signature_key_v1"
    }
}
