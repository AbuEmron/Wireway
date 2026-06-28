package com.wirewaypro.app.domain.model

/**
 * Domain view of a row in the `profiles` table (same table the web app reads).
 * Mirrors only the fields this phase needs; grows as features land.
 */
data class UserProfile(
    val id: String,
    val fullName: String?,
    val email: String?,
    val plan: String?,
    val subscriptionStatus: String?,
) {
    /** Mirrors `isPro()` in the web app's src/lib/supabase.js. */
    val isPro: Boolean
        get() = plan == "pro" ||
            plan == "teams" ||
            subscriptionStatus == "trialing" ||
            subscriptionStatus == "active"
}
