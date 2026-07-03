package com.wirewaypro.app.domain.repository

import com.wirewaypro.app.domain.financing.FinancingOffer
import com.wirewaypro.app.domain.financing.FinancingSetup

/**
 * Provider-agnostic client financing (Elite). Implementations talk to the
 * Wireway backend, which holds the provider keys — no financing-provider
 * credentials ever live in the app.
 */
interface FinancingRepository {

    /** Whether the contractor's financing account is connected, and to whom. */
    suspend fun setup(): Result<FinancingSetup>

    /**
     * Creates (or refreshes) a financing offer for an estimate — the provider
     * returns the customer-facing prequalify/apply link. Amounts and any
     * "as low as" figures come from the provider, never computed locally.
     */
    suspend fun createOffer(
        estimateId: String,
        amount: Double,
        clientName: String?,
        clientEmail: String?,
        clientPhone: String?,
    ): Result<FinancingOffer>

    /** The current offer for an estimate (webhook-updated status), or null if none. */
    suspend fun offerFor(estimateId: String): Result<FinancingOffer?>

    /** Withdraws the offer for an estimate (link off the proposal, backend cancels). */
    suspend fun removeOffer(estimateId: String): Result<Unit>
}
