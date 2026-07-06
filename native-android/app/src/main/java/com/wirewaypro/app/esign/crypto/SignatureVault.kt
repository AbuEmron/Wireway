package com.wirewaypro.app.esign.crypto

import java.io.File

/**
 * The signature image at rest, encrypted. Ties [SignatureCipher] (algorithm) to
 * an [EsignKeyProvider] (the Keystore key) and reads/writes the encrypted blob as
 * a file in the app's private storage.
 *
 * We store the signature image ENCRYPTED and keep only the flattened, visible
 * copy inside the sealed PDF. That way the raw signature bitmap never sits in
 * plaintext on disk, satisfying the "signature encrypted at rest" requirement,
 * while the legally-operative artifact (the sealed PDF + its hash) stays intact.
 */
class SignatureVault(
    private val keyProvider: EsignKeyProvider,
) {

    /** Encrypt [pngBytes] and write the blob to [dest]. Returns [dest]. */
    fun encryptToFile(pngBytes: ByteArray, dest: File): File {
        dest.parentFile?.mkdirs()
        val blob = SignatureCipher.encrypt(pngBytes, keyProvider.getOrCreateKey())
        dest.writeBytes(blob)
        return dest
    }

    /** Decrypt a blob written by [encryptToFile] back to the original PNG bytes. */
    fun decryptFile(src: File): ByteArray =
        SignatureCipher.decrypt(src.readBytes(), keyProvider.getOrCreateKey())
}
