package com.wirewaypro.app.esign.pdf

import android.graphics.Bitmap
import com.wirewaypro.app.esign.SignatureMethod
import java.io.File

/**
 * Everything the PDF layer needs to seal a signature into a proposal and append a
 * Completion Certificate page (fields per WIREWAY_ESIGN_CONSENT_FLOW.md, Screen 3).
 *
 * The actual rendering reuses [com.wirewaypro.app.ui.util.QuotePdfGenerator] so the
 * signed document is visually identical to the estimate the client reviewed — the
 * signature is flattened onto the same proposal, then the certificate is appended.
 */
data class SealInput(
    /** Drawn signature bitmap (transparent PNG-style). Null when the signer typed. */
    val signatureBitmap: Bitmap?,
    /** Typed signature text (method = TYPED). Also used as the printed signer label. */
    val typedName: String?,
    val method: SignatureMethod,
    val signerName: String,
    val signerEmail: String?,
    val documentTitle: String,
    val consentGivenAtMillis: Long,
    val signedAtMillis: Long,
    val deviceModel: String,
    val appVersion: String,
    /** Best-effort network address; null when unknown (printed as "Not recorded"). */
    val ipAddress: String?,
    /** Identity-check description, e.g. "Email on file". Honest — no over-claiming. */
    val identityCheck: String,
)

/**
 * The sealed artifact + its two hashes.
 *  - [contentSha256] — SHA-256 of the signed proposal (pre-certificate). Printed on
 *    the certificate as the document fingerprint; answers "did the agreement change?"
 *  - [sealedSha256] — SHA-256 of the FINAL sealed file (proposal + signature +
 *    certificate). Stored in the audit record and recomputed by verify() as the
 *    tamper check on the whole signed document.
 */
data class SealResult(
    val file: File,
    val contentSha256: String,
    val sealedSha256: String,
)
