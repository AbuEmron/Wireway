package com.wirewaypro.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the entitlement ladder (WIREWAY_PRICING_TIERS.md): Free ⊂ Pro ⊂ Elite,
 * Teams carries Pro-level features, and the effective tier is the HIGHEST of the
 * server plan and the device's owned Play subscriptions — so a fresh purchase
 * unlocks instantly, and a server grant works with billing unavailable.
 */
class TierTest {

    private fun profile(
        plan: String? = null,
        subscriptionStatus: String? = null,
        email: String? = "user@example.com",
    ) = UserProfile(
        id = "u1",
        fullName = "Test User",
        email = email,
        plan = plan,
        subscriptionStatus = subscriptionStatus,
    )

    // ── Ordering ────────────────────────────────────────────────────────────────

    @Test
    fun `elite includes pro includes free`() {
        assertTrue(Tier.ELITE.atLeast(Tier.PRO))
        assertTrue(Tier.ELITE.atLeast(Tier.FREE))
        assertTrue(Tier.PRO.atLeast(Tier.FREE))
        assertTrue(Tier.FREE.atLeast(Tier.FREE))
    }

    @Test
    fun `lower tiers do not include higher ones`() {
        assertFalse(Tier.FREE.atLeast(Tier.PRO))
        assertFalse(Tier.FREE.atLeast(Tier.ELITE))
        assertFalse(Tier.PRO.atLeast(Tier.ELITE))
    }

    // ── Server profile mapping ──────────────────────────────────────────────────

    @Test
    fun `no profile resolves to free`() {
        assertEquals(Tier.FREE, Tier.resolve(profile = null))
    }

    @Test
    fun `plan strings map to their tiers`() {
        assertEquals(Tier.FREE, Tier.ofProfile(profile(plan = null)))
        assertEquals(Tier.PRO, Tier.ofProfile(profile(plan = "pro")))
        assertEquals(Tier.PRO, Tier.ofProfile(profile(plan = "teams")))
        assertEquals(Tier.ELITE, Tier.ofProfile(profile(plan = "elite")))
    }

    @Test
    fun `active or trialing subscription grants pro`() {
        assertEquals(Tier.PRO, Tier.ofProfile(profile(subscriptionStatus = "active")))
        assertEquals(Tier.PRO, Tier.ofProfile(profile(subscriptionStatus = "trialing")))
    }

    // ── Play product mapping ────────────────────────────────────────────────────

    @Test
    fun `play product ids map to their tiers`() {
        assertEquals(Tier.PRO, Tier.ofPlayProduct("wireway_pro_monthly"))
        assertEquals(Tier.PRO, Tier.ofPlayProduct("wireway_pro_yearly"))
        assertEquals(Tier.PRO, Tier.ofPlayProduct("wireway_teams_monthly"))
        assertEquals(Tier.PRO, Tier.ofPlayProduct("wireway_teams_yearly"))
        assertEquals(Tier.ELITE, Tier.ofPlayProduct("wireway_elite_monthly"))
        assertEquals(Tier.ELITE, Tier.ofPlayProduct("wireway_elite_yearly"))
    }

    @Test
    fun `unknown play product grants nothing`() {
        assertEquals(Tier.FREE, Tier.ofPlayProduct("some_future_sku"))
        assertEquals(Tier.FREE, Tier.ofPlayProduct(""))
    }

    // ── Resolution: highest of server + Play wins ───────────────────────────────

    @Test
    fun `fresh play purchase unlocks before server entitlement sync lands`() {
        // Server still says free; the device owns an Elite sub → Elite immediately.
        assertEquals(
            Tier.ELITE,
            Tier.resolve(profile(plan = null), ownedPlayProducts = listOf("wireway_elite_monthly")),
        )
    }

    @Test
    fun `server grant works with billing unavailable`() {
        assertEquals(Tier.ELITE, Tier.resolve(profile(plan = "elite"), ownedPlayProducts = emptyList()))
    }

    @Test
    fun `highest of the two sources wins in both directions`() {
        assertEquals(
            Tier.ELITE,
            Tier.resolve(profile(plan = "elite"), ownedPlayProducts = listOf("wireway_pro_monthly")),
        )
        assertEquals(
            Tier.ELITE,
            Tier.resolve(profile(plan = "pro"), ownedPlayProducts = listOf("wireway_elite_yearly")),
        )
    }

    @Test
    fun `no entitlements anywhere resolves to free`() {
        assertEquals(Tier.FREE, Tier.resolve(profile(), ownedPlayProducts = emptyList()))
    }

    @Test
    fun `multiple owned products resolve to the highest`() {
        assertEquals(
            Tier.ELITE,
            Tier.resolve(
                profile = null,
                ownedPlayProducts = listOf("wireway_pro_monthly", "wireway_elite_yearly", "junk"),
            ),
        )
    }
}
