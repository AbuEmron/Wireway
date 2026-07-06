package com.wirewaypro.app.esign

import com.wirewaypro.app.esign.consent.ConsentDisclosures
import com.wirewaypro.app.esign.crypto.EsignKeyProvider
import com.wirewaypro.app.esign.crypto.SignatureVault
import com.wirewaypro.app.esign.data.EsignRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Audit-trail completeness for the pre-signature flow (consent + confirmation),
 * which is fully device-independent. Drives [EsignManager] against an in-memory
 * repository and asserts:
 *  - the consent event is captured SEPARATELY from the signature step,
 *  - every event stores the disclosure VERSION the signer saw, and
 *  - the "I'd rather use paper" off-ramp is recorded (and no consent is implied).
 *
 * The seal step (PDF render + Keystore) needs Android and is covered by the crypto
 * + hash + migration JVM tests plus on-device use; here we prove the trail is
 * complete and correctly ordered up to the pen.
 */
class EsignAuditTest {

    /** Minimal in-memory EsignRepository — records every appended event in order. */
    private class FakeRepo : EsignRepository {
        val events = mutableListOf<EsignAuditEvent>()

        override suspend fun insertRecord(userId: String, record: EsignRecord): Result<Unit> = Result.success(Unit)

        override suspend fun appendEvent(
            userId: String,
            recordId: String,
            type: EsignEventType,
            consentVersion: String,
            atMillis: Long,
            detailJson: String?,
        ): Result<EsignAuditEvent> {
            val e = EsignAuditEvent(
                id = "evt-${events.size}",
                recordId = recordId,
                type = type,
                atMillis = atMillis,
                consentVersion = consentVersion,
                detailJson = detailJson,
            )
            events += e
            return Result.success(e)
        }

        override suspend fun latestForQuote(quoteId: String): EsignRecord? = null
        override fun observeForQuote(quoteId: String): Flow<List<EsignRecord>> = flowOf(emptyList())
        override suspend fun auditTrail(recordId: String): List<EsignAuditEvent> =
            events.filter { it.recordId == recordId }
        override fun pendingSyncCount(): Flow<Int> = flowOf(0)
    }

    private fun manager(repo: EsignRepository): EsignManager {
        val vault = SignatureVault(object : EsignKeyProvider {
            val k: SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
            override fun getOrCreateKey() = k
        })
        return EsignManager(repo, vault)
    }

    @Test
    fun consentFlow_recordsConsentSeparately_withDisclosureVersion() = runBlocking {
        val repo = FakeRepo()
        val mgr = manager(repo)

        val session = mgr.beginSession("user-1", "quote-1", "Proposal #1042", nowMillis = 1_000L)
        mgr.recordConsentGiven(session, nowMillis = 2_000L)
        mgr.recordSignatureConfirmed(session, nowMillis = 3_000L)

        val trail = repo.auditTrail(session.recordId).sortedBy { it.atMillis }
        val types = trail.map { it.type }

        // Consent is presented, then affirmatively given, then the final confirm —
        // three distinct events; consent is NOT collapsed into the signature step.
        assertEquals(
            listOf(
                EsignEventType.CONSENT_PRESENTED,
                EsignEventType.CONSENT_GIVEN,
                EsignEventType.SIGNATURE_CONFIRMED,
            ),
            types,
        )
        // Separate consent event exists and precedes any signature-related event.
        val consentIdx = types.indexOf(EsignEventType.CONSENT_GIVEN)
        val confirmIdx = types.indexOf(EsignEventType.SIGNATURE_CONFIRMED)
        assertTrue("consent must be captured before the signature step", consentIdx < confirmIdx)

        // Every event carries the exact disclosure version shown.
        assertTrue(trail.all { it.consentVersion == ConsentDisclosures.VERSION })
        // The session recorded when consent was affirmatively given.
        assertEquals(2_000L, session.consentGivenAtMillis)
    }

    @Test
    fun paperOffRamp_isRecorded_andImpliesNoConsent() = runBlocking {
        val repo = FakeRepo()
        val mgr = manager(repo)

        val session = mgr.beginSession("user-1", "quote-1", "Proposal #1042", nowMillis = 1_000L)
        mgr.recordDeclinedForPaper(session, nowMillis = 1_500L)

        val types = repo.auditTrail(session.recordId).map { it.type }
        assertTrue(types.contains(EsignEventType.DECLINED_FOR_PAPER))
        // Choosing paper must never look like consent.
        assertTrue("declining paper implies no consent given", !types.contains(EsignEventType.CONSENT_GIVEN))
        assertEquals("no signature is implied by the paper off-ramp", null, session.consentGivenAtMillis)
    }

    @Test
    fun eventTypes_haveStableWireValues() {
        // The audit trail is long-lived evidence — wire strings must not drift.
        assertEquals("consent_given", EsignEventType.CONSENT_GIVEN.wire)
        assertEquals("document_sealed", EsignEventType.DOCUMENT_SEALED.wire)
        assertEquals(EsignEventType.CONSENT_GIVEN, EsignEventType.from("consent_given"))
    }
}
