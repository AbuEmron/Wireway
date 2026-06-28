package com.wirewaypro.app.data.quotes

import com.wirewaypro.app.domain.model.QuoteCalculator
import com.wirewaypro.app.domain.model.QuoteCatalogEntry
import com.wirewaypro.app.domain.model.QuoteCustomItem
import com.wirewaypro.app.domain.model.QuoteDetail
import com.wirewaypro.app.domain.model.QuoteInput
import com.wirewaypro.app.domain.model.QuoteSummary
import com.wirewaypro.app.domain.repository.QuoteRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.Year
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuoteRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
) : QuoteRepository {

    private fun quotes() = client.postgrest.from("quotes")

    private suspend fun fetchSummaries(userId: String): List<QuoteSummary> =
        quotes()
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
        quotes()
            .select { filter { eq("id", quoteId) } }
            .decodeSingleOrNull<QuoteDto>()
            ?.toDetail()
            ?: error("Quote not found.")
    }

    override suspend fun saveQuote(userId: String, input: QuoteInput): Result<QuoteDetail> = runCatching {
        // Catalog entries are fully editable now — compute everything from the
        // input; no need to re-read or "freeze" anything.
        val totals = QuoteCalculator.compute(
            catalogEntries = input.catalogEntries,
            customItems = input.customItems,
            markup = input.markup,
            taxEnabled = input.taxEnabled,
            taxRate = input.taxRate,
            hourlyRate = input.hourlyRate,
        )

        val quoteNumber = input.quoteNumber?.takeIf { it.isNotBlank() }
            ?: generateQuoteNumber(userId)

        val entriesJson = entriesToJson(input.catalogEntries)
        val customItemsJson = customItemsToJson(input.customItems)

        val payload = buildJsonObject {
            if (input.id == null) put("user_id", userId)
            put("quote_number", quoteNumber)
            put("client_name", input.clientName)
            put("client_email", input.clientEmail)
            put("client_phone", input.clientPhone)
            put("job_name", input.jobName)
            put("notes", input.notes)
            put("hourly_rate", input.hourlyRate)
            put("markup", input.markup)
            put("show_materials", input.showMaterials)
            put("client_buys_all", false)
            put("flat_rate_mode", false)
            put("invoice_mode", input.invoiceMode)
            put("invoice_due_date", input.invoiceDueDate)
            put("invoice_paid", input.invoicePaid)
            put("tax_enabled", input.taxEnabled)
            put("tax_rate", input.taxRate)
            put("entries", entriesJson)
            put("custom_items", customItemsJson)
            put("total_material", totals.totalMaterial)
            put("total_labor", totals.totalLabor)
            put("total_hours", totals.totalHours)
            put("total_markup", totals.markupAmount)
            put("total_tax", totals.taxAmount)
            put("total", totals.total)
            put("status", "draft")
        }

        val saved = if (input.id == null) {
            quotes().insert(payload) { select() }.decodeSingle<QuoteDto>()
        } else {
            quotes().update(payload) {
                filter { eq("id", input.id); eq("user_id", userId) }
                select()
            }.decodeSingle<QuoteDto>()
        }
        saved.toDetail()
    }

    override suspend fun deleteQuote(userId: String, quoteId: String): Result<Unit> = runCatching {
        quotes().delete { filter { eq("id", quoteId); eq("user_id", userId) } }
    }

    override suspend fun setInvoicePaid(
        userId: String,
        quoteId: String,
        paid: Boolean,
    ): Result<QuoteDetail> = runCatching {
        val payload = buildJsonObject {
            put("invoice_paid", paid)
            put("paid_at", if (paid) Instant.now().toString() else null as String?)
            put("status", if (paid) "paid" else "sent")
        }
        quotes().update(payload) {
            filter { eq("id", quoteId); eq("user_id", userId) }
            select()
        }.decodeSingle<QuoteDto>().toDetail()
    }

    override suspend fun setInvoiceDueDate(
        userId: String,
        quoteId: String,
        dueDate: String?,
    ): Result<QuoteDetail> = runCatching {
        val payload = buildJsonObject { put("invoice_due_date", dueDate) }
        quotes().update(payload) {
            filter { eq("id", quoteId); eq("user_id", userId) }
            select()
        }.decodeSingle<QuoteDto>().toDetail()
    }

    /** WW-{year}-{(existing quote count + 1) padded to 3}, matching genQuoteNum(). */
    private suspend fun generateQuoteNumber(userId: String): String {
        val count = quotes()
            .select(Columns.list("id")) {
                filter { eq("user_id", userId) }
                count(Count.EXACT)
            }
            .countOrNull() ?: 0L
        val seq = (count + 1).toString().padStart(3, '0')
        return "WW-${Year.now().value}-$seq"
    }

    private fun entriesToJson(entries: List<QuoteCatalogEntry>): JsonObject = buildJsonObject {
        entries.forEach { e ->
            if (e.qty > 0.0) {
                put(
                    e.serviceId,
                    buildJsonObject {
                        put("qty", e.qty)
                        put("variantIdx", e.variantIdx)
                        put("clientBuys", e.clientBuys)
                    },
                )
            }
        }
    }

    private fun customItemsToJson(items: List<QuoteCustomItem>): JsonArray {
        val base = Instant.now().toEpochMilli()
        return buildJsonArray {
            items.forEachIndexed { index, item ->
                add(
                    buildJsonObject {
                        put("id", item.id ?: (base + index))
                        put("label", item.label)
                        put("qty", item.qty)
                        put("materialCost", item.materialCost)
                        put("laborCost", item.laborCost)
                        put("laborHours", item.laborHours)
                        if (item.kind != null) put("kind", item.kind)
                    }
                )
            }
        }
    }
}
