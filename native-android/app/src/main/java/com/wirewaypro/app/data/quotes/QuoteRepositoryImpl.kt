package com.wirewaypro.app.data.quotes

import com.wirewaypro.app.domain.model.QuoteDetail
import com.wirewaypro.app.domain.model.QuoteSummary
import com.wirewaypro.app.domain.repository.QuoteRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuoteRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
) : QuoteRepository {

    // An invoice is a quote with invoice_mode = true. We fetch the user's quotes
    // once and split locally: this tolerates rows where invoice_mode is null
    // (legacy estimates) without an `or(... is null ...)` filter.
    private suspend fun fetchSummaries(userId: String): List<QuoteSummary> =
        client.postgrest.from("quotes")
            .select(Columns.list(*QUOTE_LIST_COLUMNS.toTypedArray())) {
                filter { eq("user_id", userId) }
                order("created_at", Order.DESCENDING)
                limit(200)
            }
            .decodeList<QuoteDto>()
            .map { it.toSummary() }

    override suspend fun getEstimates(userId: String): Result<List<QuoteSummary>> =
        runCatching { fetchSummaries(userId).filter { !it.isInvoice } }

    override suspend fun getInvoices(userId: String): Result<List<QuoteSummary>> =
        runCatching { fetchSummaries(userId).filter { it.isInvoice } }

    override suspend fun getQuote(quoteId: String): Result<QuoteDetail> = runCatching {
        client.postgrest.from("quotes")
            .select { filter { eq("id", quoteId) } }
            .decodeSingleOrNull<QuoteDto>()
            ?.toDetail()
            ?: error("Quote not found.")
    }
}
