package com.wirewaypro.app.domain.repository

import com.wirewaypro.app.domain.model.QuoteDetail
import com.wirewaypro.app.domain.model.QuoteInput
import com.wirewaypro.app.domain.model.QuoteSummary
import kotlinx.coroutines.flow.Flow

interface QuoteRepository {
    /** Live count of quotes with local changes still waiting to sync (pending or errored). */
    fun pendingSyncCount(): Flow<Int>

    /** Quotes that are NOT in invoice mode (estimates), newest first. */
    suspend fun getEstimates(userId: String): Result<List<QuoteSummary>>

    /** Quotes with invoice_mode = true (invoices), newest first. */
    suspend fun getInvoices(userId: String): Result<List<QuoteSummary>>

    /** Full quote (estimate or invoice) by id, including JSON line items. */
    suspend fun getQuote(quoteId: String): Result<QuoteDetail>

    /**
     * Creates (id == null) or updates a quote. Computes totals with the web app's
     * formula, generates the quote number when absent, and preserves any catalog
     * `entries` on edit. Returns the saved record.
     */
    suspend fun saveQuote(userId: String, input: QuoteInput): Result<QuoteDetail>

    /** Deletes a quote (estimate or invoice). */
    suspend fun deleteQuote(userId: String, quoteId: String): Result<Unit>

    /** Marks an invoice paid/unpaid (sets invoice_paid, paid_at, status). */
    suspend fun setInvoicePaid(userId: String, quoteId: String, paid: Boolean): Result<QuoteDetail>

    /** Sets (or clears) an invoice's due date. */
    suspend fun setInvoiceDueDate(userId: String, quoteId: String, dueDate: String?): Result<QuoteDetail>
}
