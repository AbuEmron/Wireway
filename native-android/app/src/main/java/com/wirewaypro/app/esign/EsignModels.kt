package com.wirewaypro.app.esign

/**
 * Public domain models for the e-signature module. Deliberately framework-free
 * (no Android, no Room, no Supabase types) so the module has a clean, extractable
 * API surface. The data layer maps these to/from Room entities and Supabase DTOs.
 *
 * US ESIGN Act / UETA framing. Language stays honest: this is an "electronic
 * signature", never "notarized", "certified", or "qualified". Legally-operative
 * wording lives in [com.wirewaypro.app.esign.consent.ConsentDisclosures] and is
 * versioned. See WIREWAY_ESIGN_CONSENT_FLOW.md.
 */

/** How the signer produced their mark. */
enum class SignatureMethod(val wire: String, val label: String) {
    DRAWN("drawn", "Drawn on device"),
    TYPED("typed", "Typed");

    companion object {
        fun from(v: String?): SignatureMethod = entries.firstOrNull { it.wire == v } ?: DRAWN
    }
}

/** Who signed + how their identity was checked (V1: in-person, email on file). */
data class Signer(
    val name: String,
    val email: String?,
)

/**
 * The append-only audit trail's event kinds. Each signing produces a sequence of
 * these; the consent event is recorded SEPARATELY from the signature event (as the
 * spec requires) so the affirmative act of consenting is independently evidenced.
 */
enum class EsignEventType(val wire: String) {
    /** The disclosures were displayed to the signer. */
    CONSENT_PRESENTED("consent_presented"),

    /** The signer affirmatively agreed (checked the box + tapped "Agree and continue"). */
    CONSENT_GIVEN("consent_given"),

    /** The signer chose paper instead — no signature captured. */
    DECLINED_FOR_PAPER("declined_for_paper"),

    /** The signer confirmed the final "You're about to sign" screen. */
    SIGNATURE_CONFIRMED("signature_confirmed"),

    /** A signature mark was captured (drawn or typed). */
    SIGNATURE_CAPTURED("signature_captured"),

    /** The sealed PDF was produced and its hash recorded. */
    DOCUMENT_SEALED("document_sealed"),

    /** An integrity re-check was run against the sealed PDF. */
    INTEGRITY_VERIFIED("integrity_verified"),

    /** An integrity re-check FAILED — the sealed PDF no longer matches its hash. */
    INTEGRITY_FAILED("integrity_failed");

    companion object {
        fun from(v: String?): EsignEventType? = entries.firstOrNull { it.wire == v }
    }
}

/** A single append-only audit event. */
data class EsignAuditEvent(
    val id: String,
    val recordId: String,
    val type: EsignEventType,
    val atMillis: Long,
    /** Disclosure version in effect for this event (so we can always reproduce what was shown). */
    val consentVersion: String,
    /** Small JSON blob of event-specific detail (method, hash, ip, etc.). */
    val detailJson: String?,
)

/** Environment captured at signing, printed on the Completion Certificate. */
data class SigningContext(
    val deviceModel: String,
    val appVersion: String,
    /** Best-effort network address; null when unknown (honest — never fabricated). */
    val ipAddress: String?,
)

/**
 * The durable signed record — the proof. Immutable once sealed. Both this and the
 * audit events are stored Room-first (offline, never lost) and synced to Supabase
 * under per-user RLS.
 */
data class EsignRecord(
    val id: String,
    val quoteId: String,
    val documentTitle: String,
    val signer: Signer,
    val method: SignatureMethod,
    val consentVersion: String,
    val consentGivenAtMillis: Long,
    val signedAtMillis: Long,
    /** SHA-256 of the signed proposal content (the certificate fingerprint). */
    val contentSha256: String,
    /** SHA-256 of the final sealed PDF file — what [verify] recomputes. */
    val sealedSha256: String,
    /** Local path to the sealed PDF (the shareable artifact). */
    val sealedPdfPath: String,
    /** Local path to the ENCRYPTED signature image blob (null for typed-only). */
    val encryptedSignaturePath: String?,
    val context: SigningContext,
)

/** Result of re-checking a sealed PDF against its recorded hash. */
data class VerificationResult(
    val intact: Boolean,
    val expectedSha256: String,
    val actualSha256: String,
) {
    val tampered: Boolean get() = !intact
}
