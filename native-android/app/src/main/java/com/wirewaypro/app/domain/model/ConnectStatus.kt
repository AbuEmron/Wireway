package com.wirewaypro.app.domain.model

/**
 * The contractor's Stripe Connect state (from /api/connect-onboarding status).
 *  - [connected]: a connected account exists (onboarding started).
 *  - [chargesEnabled]: the account can actually accept client payments.
 */
data class ConnectStatus(
    val connected: Boolean,
    val chargesEnabled: Boolean,
)
