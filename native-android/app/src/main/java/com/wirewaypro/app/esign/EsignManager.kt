package com.wirewaypro.app.esign

import android.content.Context
import android.graphics.Bitmap
import com.wirewaypro.app.domain.model.BusinessInfo
import com.wirewaypro.app.domain.model.QuoteDetail
import com.wirewaypro.app.esign.consent.ConsentDisclosures
import com.wirewaypro.app.esign.crypto.Sha256
import com.wirewaypro.app.esign.crypto.SignatureVault
import com.wirewaypro.app.esign.data.EsignRepository
import com.wirewaypro.app.esign.pdf.SealInput
import com.wirewaypro.app.ui.util.QuotePdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The e-signature module's public facade — the ONE clean entry point the app (or
 * any host app) drives. It sequences the flow required for an enforceable ESIGN/UETA
 * electronic signature and keeps the pieces (consent audit, crypto, PDF seal, sync)
 * behind a small surface so the whole module can be extracted intact.
 *
 * The flow (order matters — see WIREWAY_ESIGN_CONSENT_FLOW.md):
 *   beginSession → recordConsentGiven → recordSignatureConfirmed → seal → (verify)
 * with recordDeclinedForPaper as the always-available off-ramp before signing.
 *
 * Every step writes to the append-only audit trail (Room-first, then synced). The
 * consent event is recorded SEPARATELY from the signature, and each event stores the
 * disclosure VERSION shown, so we can always reproduce exactly what the signer saw.
 *
 * V1 is IN-PERSON sign-on-device. Nothing here assumes a network at signing time —
 * a signed record is durable locally the instant [seal] returns.
 */
@Singleton
class EsignManager @Inject constructor(
    private val repository: EsignRepository,
    private val vault: SignatureVault,
) {

    /**
     * A signing session. The [recordId] is minted up front so consent events
     * (captured before the record exists) reference the same record they'll seal
     * into. Carries the disclosure [consentVersion] shown for the whole session.
     */
    data class Session(
        val recordId: String,
        val userId: String,
        val quoteId: String,
        val documentTitle: String,
        val consentVersion: String,
        val startedAtMillis: Long,
        var consentGivenAtMillis: Long? = null,
    )

    /** Open a signing session and record that the disclosures were presented. */
    suspend fun beginSession(
        userId: String,
        quoteId: String,
        documentTitle: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): Session {
        val session = Session(
            recordId = java.util.UUID.randomUUID().toString(),
            userId = userId,
            quoteId = quoteId,
            documentTitle = documentTitle,
            consentVersion = ConsentDisclosures.VERSION,
            startedAtMillis = nowMillis,
        )
        repository.appendEvent(
            userId, session.recordId, EsignEventType.CONSENT_PRESENTED,
            session.consentVersion, nowMillis,
        )
        return session
    }

    /** Record the affirmative consent act (box checked + "Agree and continue"). */
    suspend fun recordConsentGiven(session: Session, nowMillis: Long = System.currentTimeMillis()) {
        session.consentGivenAtMillis = nowMillis
        repository.appendEvent(
            session.userId, session.recordId, EsignEventType.CONSENT_GIVEN,
            session.consentVersion, nowMillis,
        )
    }

    /** The always-available off-ramp: the signer chose paper. No signature captured. */
    suspend fun recordDeclinedForPaper(session: Session, nowMillis: Long = System.currentTimeMillis()) {
        repository.appendEvent(
            session.userId, session.recordId, EsignEventType.DECLINED_FOR_PAPER,
            session.consentVersion, nowMillis,
        )
    }

    /** Record the final "You're about to sign" confirmation tap. */
    suspend fun recordSignatureConfirmed(session: Session, nowMillis: Long = System.currentTimeMillis()) {
        repository.appendEvent(
            session.userId, session.recordId, EsignEventType.SIGNATURE_CONFIRMED,
            session.consentVersion, nowMillis,
        )
    }

    /**
     * Seal the signature into [quote]'s proposal, encrypt the signature image at
     * rest, compute the hashes, append the Completion Certificate, and durably store
     * the record + audit events. Room-first: the returned record is saved locally
     * before this suspends-returns, even fully offline.
     *
     * Requires [Session.consentGivenAtMillis] to be set (consent precedes signing) —
     * returns a failure otherwise rather than sealing without recorded consent.
     */
    suspend fun seal(
        context: Context,
        session: Session,
        quote: QuoteDetail,
        signer: Signer,
        method: SignatureMethod,
        signatureBitmap: Bitmap?,
        typedName: String?,
        business: BusinessInfo? = null,
        logo: Bitmap? = null,
        accent: Int? = null,
        nowMillis: Long = System.currentTimeMillis(),
    ): Result<EsignRecord> = runCatching {
        val consentAt = session.consentGivenAtMillis
            ?: error("Cannot seal before consent is recorded.")
        val env = SigningEnvironment.of(context)

        // 1) Audit: a signature mark was captured.
        repository.appendEvent(
            session.userId, session.recordId, EsignEventType.SIGNATURE_CAPTURED,
            session.consentVersion, nowMillis,
            detailJson = """{"method":"${method.wire}"}""",
        )

        val input = SealInput(
            signatureBitmap = signatureBitmap,
            typedName = typedName,
            method = method,
            signerName = signer.name,
            signerEmail = signer.email,
            documentTitle = session.documentTitle,
            consentGivenAtMillis = consentAt,
            signedAtMillis = nowMillis,
            deviceModel = env.deviceModel,
            appVersion = env.appVersion,
            ipAddress = env.ipAddress,
            identityCheck = if (signer.email.isNullOrBlank()) "In person" else "Email on file",
        )

        // 2) Render + hash the sealed PDF (off the main thread), then relocate it
        //    from the (evictable) cache into the DURABLE files dir — a signed legal
        //    record must survive cache clearing (never lose a signed record). The
        //    bytes are copied verbatim, so sealedSha256 still matches the file.
        val sealedPdf = withContext(Dispatchers.IO) {
            val seal = QuotePdfGenerator.generateSealed(context, quote, input, business, logo, accent)
                ?: return@withContext null
            val durable = File(File(context.filesDir, "esign").apply { mkdirs() }, "signed-${session.recordId}.pdf")
            seal.file.copyTo(durable, overwrite = true)
            seal.file.delete()
            Triple(durable, seal.contentSha256, seal.sealedSha256)
        } ?: error("Couldn't build the signed PDF.")
        val (sealedFile, contentSha, sealedSha) = sealedPdf

        // 3) Encrypt the signature image at rest (drawn only; typed has no bitmap).
        val encPath = signatureBitmap?.let { bmp ->
            withContext(Dispatchers.IO) {
                val png = ByteArrayOutputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it); it.toByteArray() }
                val dest = File(File(context.filesDir, "esign").apply { mkdirs() }, "sig-${session.recordId}.enc")
                vault.encryptToFile(png, dest).absolutePath
            }
        }

        val record = EsignRecord(
            id = session.recordId,
            quoteId = session.quoteId,
            documentTitle = session.documentTitle,
            signer = signer,
            method = method,
            consentVersion = session.consentVersion,
            consentGivenAtMillis = consentAt,
            signedAtMillis = nowMillis,
            contentSha256 = contentSha,
            sealedSha256 = sealedSha,
            sealedPdfPath = sealedFile.absolutePath,
            encryptedSignaturePath = encPath,
            context = env,
        )

        // 4) Durable store (Room-first, never lost) + push best-effort.
        repository.insertRecord(session.userId, record).getOrThrow()

        // 5) Audit: sealed, with the fingerprints.
        repository.appendEvent(
            session.userId, session.recordId, EsignEventType.DOCUMENT_SEALED,
            session.consentVersion, nowMillis,
            detailJson = """{"content_sha256":"$contentSha","sealed_sha256":"$sealedSha"}""",
        )
        record
    }

    /**
     * Tamper check: recompute the SHA-256 of the sealed PDF on disk and compare it
     * to the hash recorded when it was sealed. Appends an INTEGRITY_VERIFIED or
     * INTEGRITY_FAILED audit event. Returns a failure only if the file is missing.
     */
    suspend fun verify(
        userId: String,
        record: EsignRecord,
        nowMillis: Long = System.currentTimeMillis(),
    ): Result<VerificationResult> = runCatching {
        val file = File(record.sealedPdfPath)
        if (!file.exists()) error("Signed document not found on this device.")
        val actual = Sha256.hexOfFile(file)
        val intact = actual.equals(record.sealedSha256, ignoreCase = true)
        repository.appendEvent(
            userId, record.id,
            if (intact) EsignEventType.INTEGRITY_VERIFIED else EsignEventType.INTEGRITY_FAILED,
            record.consentVersion, nowMillis,
            detailJson = """{"expected":"${record.sealedSha256}","actual":"$actual"}""",
        )
        VerificationResult(intact = intact, expectedSha256 = record.sealedSha256, actualSha256 = actual)
    }

    /** The append-only audit trail for a record (oldest first). */
    suspend fun auditTrail(recordId: String) = repository.auditTrail(recordId)

    /** The most-recent sealed record for a quote, or null. */
    suspend fun latestForQuote(quoteId: String) = repository.latestForQuote(quoteId)
}
