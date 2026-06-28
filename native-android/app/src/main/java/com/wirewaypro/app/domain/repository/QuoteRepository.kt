package com.wirewaypro.app.domain.repository

import com.wirewaypro.app.domain.model.QuoteDetail
import com.wirewaypro.app.domain.model.QuoteSummary

interface QuoteRepository {
    /** Quotes that are NOT in invoice mode (estimates), newest first. */
    suspend fun getEstimates(userId: String): Result<List<QuoteSummary>>

    /** Quotes with invoice_mode = true (invoices), newest first. */
    suspend fun getInvoices(userId: String): Result<List<QuoteSummary>>

    /** Full quote (estimate or invoice) by id, including JSON line items. */
    suspend fun getQuote(quoteId: String): Result<QuoteDetail>
}
