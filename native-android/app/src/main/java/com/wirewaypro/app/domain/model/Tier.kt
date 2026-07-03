package com.wirewaypro.app.domain.model

/**
 * The three entitlement tiers (WIREWAY_PRICING_TIERS.md):
 * Free = try it and trust it · Pro = win the job and get paid · Elite = run and
 * grow the business. Teams is a seats axis that carries Pro-level features.
 *
 * Resolution is deterministic and pure: the server profile (`profiles.plan`) and
 * the device's owned Play subscriptions each map to a tier, and the HIGHEST wins —
 * so a fresh Play purchase unlocks instantly even before the backend entitlement
 * sync lands, and a server-granted plan works with billing unavailable.
 */
enum class Tier {
    FREE,
    PRO,
    ELITE;

    /** True when this tier includes [other]'s features (Elite ⊇ Pro ⊇ Free). */
    fun atLeast(other: Tier): Boolean = ordinal >= other.ordinal

    companion object {
        /** Maps a Play product id to the tier it purchases. Unknown ids grant nothing. */
        fun ofPlayProduct(productId: String): Tier = when {
            productId.startsWith("wireway_elite") -> ELITE
            productId.startsWith("wireway_pro") || productId.startsWith("wireway_teams") -> PRO
            else -> FREE
        }

        /** Maps the server profile to its tier (mirrors UserProfile.isPro/isElite). */
        fun ofProfile(profile: UserProfile?): Tier = when {
            profile == null -> FREE
            profile.isElite -> ELITE
            profile.isPro -> PRO
            else -> FREE
        }

        /** The user's effective tier: the highest of server plan and Play purchases. */
        fun resolve(profile: UserProfile?, ownedPlayProducts: Collection<String> = emptyList()): Tier {
            val fromPlay = ownedPlayProducts.maxOfOrNull { ofPlayProduct(it) } ?: FREE
            return maxOf(ofProfile(profile), fromPlay)
        }
    }
}

/** Free-tier ceilings (WIREWAY_PRICING_TIERS.md: "a small number of complete quotes"). */
object FreeLimits {
    /** Saved estimates/invoices before the Pro wall. Enough to prove the loop end to end. */
    const val MAX_QUOTES = 5

    /** Saved clients before the Pro wall. */
    const val MAX_CLIENTS = 10
}
