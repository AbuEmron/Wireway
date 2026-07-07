package com.wirewaypro.app.domain.financing

/**
 * Client-financing domain model — provider-agnostic (Wisetack is the first
 * adapter, but nothing here names it). Doctrine: every number shown to a
 * customer comes from the provider's real API responses; the app NEVER
 * computes or fabricates approvals, rates, or monthly amounts.
 */

/** Connection state of the contractor's financing account. */
data class FinancingSetup(
    /** True once the contractor's merchant account is linked on the backend. */
    val connected: Boolean,
    /** Provider key, e.g. "wisetack" — null until connected. */
    val provider: String? = null,
    /** Merchant/business name as registered with the provider, when known. */
    val merchantName: String? = null,
    /** Provider onboarding link (merchant signup) when not yet connected. */
    val connectUrl: String? = null,
)

/**
 * A provider-issued financing offer for one estimate: the customer-facing
 * prequalify/apply link plus the provider-reported status.
 */
data class FinancingOffer(
    val estimateId: String,
    /** The provider's hosted application/prequalification URL for THIS estimate. */
    val applicationUrl: String,
    val status: FinancingOfferStatus,
    /**
     * Provider-computed promo ("as low as $X/mo") — null unless the provider
     * returned one. Never derived locally: a wrong payment figure on a proposal
     * is a trust-killer and possibly a compliance problem.
     */
    val asLowAsMonthly: Double? = null,
    /** Term months backing [asLowAsMonthly], when the provider reports it. */
    val termMonths: Int? = null,
    /** Epoch millis of the last provider/webhook update, when reported. */
    val updatedAt: Long? = null,
)

/**
 * Lifecycle of a financing application, updated on the backend by provider
 * webhooks. UNKNOWN covers provider states this app version doesn't know yet —
 * shown verbatim-neutral, never guessed at.
 */
enum class FinancingOfferStatus(val label: String) {
    CREATED("Link created"),
    PREQUALIFIED("Prequalified"),
    APPLICATION_STARTED("Application started"),
    APPROVED("Approved"),
    DECLINED("Declined"),
    FUNDED("Funded"),
    EXPIRED("Expired"),
    UNKNOWN("Status unavailable");

    companion object {
        /** Maps a provider/backend status string; anything unrecognized is UNKNOWN. */
        fun from(raw: String?): FinancingOfferStatus = when (raw?.trim()?.uppercase()) {
            "CREATED", "INITIATED", "LINK_CREATED" -> CREATED
            "PREQUALIFIED", "PRE_QUALIFIED" -> PREQUALIFIED
            "APPLICATION_STARTED", "STARTED", "PENDING" -> APPLICATION_STARTED
            "APPROVED", "AUTHORIZED", "ACCEPTED" -> APPROVED
            "DECLINED", "REJECTED" -> DECLINED
            "FUNDED", "SETTLED", "PAID" -> FUNDED
            "EXPIRED", "CANCELED", "CANCELLED" -> EXPIRED
            else -> UNKNOWN
        }
    }
}
