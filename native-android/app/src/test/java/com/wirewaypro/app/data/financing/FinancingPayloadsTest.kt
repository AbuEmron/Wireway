package com.wirewaypro.app.data.financing

import com.wirewaypro.app.domain.financing.FinancingOfferStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the financing doctrine at the parsing seam: every customer-visible
 * figure comes from the provider response — a missing/zero "as low as" stays
 * null (nothing is shown), never derived; unknown statuses read as UNKNOWN,
 * never guessed into something reassuring.
 */
class FinancingPayloadsTest {

    @Test
    fun `setup parses connected merchant`() {
        val s = FinancingPayloads.parseSetup(
            """{"connected":true,"provider":"wisetack","merchantName":"Volt Bros Electric"}""",
        )
        assertTrue(s.connected)
        assertEquals("wisetack", s.provider)
        assertEquals("Volt Bros Electric", s.merchantName)
        assertNull(s.connectUrl)
    }

    @Test
    fun `setup defaults to not connected on missing or null fields`() {
        val s = FinancingPayloads.parseSetup("""{"provider":null}""")
        assertFalse(s.connected)
        assertNull(s.provider)
    }

    @Test
    fun `offer parses full provider response`() {
        val o = FinancingPayloads.parseOffer(
            "est-1",
            """{"estimateId":"est-1","applicationUrl":"https://wisetack.us/apply/abc",
                "status":"PREQUALIFIED","asLowAsMonthly":182.5,"termMonths":60,"updatedAt":1720000000000}""",
        )
        assertEquals("https://wisetack.us/apply/abc", o.applicationUrl)
        assertEquals(FinancingOfferStatus.PREQUALIFIED, o.status)
        assertEquals(182.5, o.asLowAsMonthly!!, 0.0)
        assertEquals(60, o.termMonths)
    }

    @Test
    fun `offer without provider monthly stays null — never fabricated`() {
        val o = FinancingPayloads.parseOffer(
            "est-2",
            """{"applicationUrl":"https://wisetack.us/apply/xyz","status":"CREATED"}""",
        )
        assertNull(o.asLowAsMonthly)
        assertNull(o.termMonths)
        assertEquals("est-2", o.estimateId) // falls back to the requested id
    }

    @Test
    fun `zero or negative monthly is treated as absent`() {
        val o = FinancingPayloads.parseOffer(
            "est-3",
            """{"applicationUrl":"https://x.example/a","status":"CREATED","asLowAsMonthly":0}""",
        )
        assertNull(o.asLowAsMonthly)
    }

    @Test(expected = IllegalStateException::class)
    fun `offer without an application link is rejected`() {
        FinancingPayloads.parseOffer("est-4", """{"status":"CREATED"}""")
    }

    @Test
    fun `unknown provider status maps to UNKNOWN, known ones map exactly`() {
        assertEquals(FinancingOfferStatus.UNKNOWN, FinancingOfferStatus.from("SOMETHING_NEW"))
        assertEquals(FinancingOfferStatus.UNKNOWN, FinancingOfferStatus.from(null))
        assertEquals(FinancingOfferStatus.APPROVED, FinancingOfferStatus.from("approved"))
        assertEquals(FinancingOfferStatus.DECLINED, FinancingOfferStatus.from("REJECTED"))
        assertEquals(FinancingOfferStatus.FUNDED, FinancingOfferStatus.from("SETTLED"))
        assertEquals(FinancingOfferStatus.EXPIRED, FinancingOfferStatus.from("CANCELLED"))
    }
}
