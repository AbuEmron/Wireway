package com.wirewaypro.app.data.quotes

import com.wirewaypro.app.domain.catalog.Catalog
import com.wirewaypro.app.domain.model.QuoteCalculator
import com.wirewaypro.app.domain.model.QuoteCatalogEntry
import com.wirewaypro.app.domain.model.QuoteCustomItem
import com.wirewaypro.app.domain.model.QuoteDetail
import com.wirewaypro.app.domain.model.QuoteLineItem
import com.wirewaypro.app.domain.model.QuoteSummary
import com.wirewaypro.app.domain.model.RateMode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Wire shape of a `quotes` row. The table backs both estimates and invoices
 * (invoice = `invoice_mode = true`). Line items are NOT a related table — they
 * live in the JSON `entries` (catalog items, keyed by id) and `custom_items`
 * (array) columns; [parseLineItems] flattens both into [QuoteLineItem]s.
 *
 * For list queries the JSON columns aren't selected, so [entries] / [customItems]
 * default to null and only [toSummary] is used.
 */
@Serializable
data class QuoteDto(
    val id: String,
    @SerialName("quote_number") val quoteNumber: String? = null,
    @SerialName("client_name") val clientName: String? = null,
    @SerialName("client_email") val clientEmail: String? = null,
    @SerialName("client_phone") val clientPhone: String? = null,
    @SerialName("job_name") val jobName: String? = null,
    val notes: String? = null,
    val status: String? = null,
    @SerialName("invoice_mode") val invoiceMode: Boolean? = null,
    @SerialName("invoice_due_date") val invoiceDueDate: String? = null,
    @SerialName("invoice_paid") val invoicePaid: Boolean? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("paid_at") val paidAt: String? = null,
    @SerialName("show_materials") val showMaterials: Boolean? = null,
    @SerialName("client_buys_all") val clientBuysAll: Boolean? = null,
    @SerialName("tax_enabled") val taxEnabled: Boolean? = null,
    @SerialName("tax_rate") val taxRate: Double? = null,
    val markup: Double? = null,
    @SerialName("hourly_rate") val hourlyRate: Double? = null,
    @SerialName("rate_mode") val rateMode: String? = null,
    @SerialName("total_material") val totalMaterial: Double? = null,
    @SerialName("total_labor") val totalLabor: Double? = null,
    @SerialName("total_hours") val totalHours: Double? = null,
    @SerialName("total_markup") val totalMarkup: Double? = null,
    @SerialName("total_tax") val totalTax: Double? = null,
    val total: Double? = null,
    val entries: JsonElement? = null,
    @SerialName("custom_items") val customItems: JsonElement? = null,
) {
    fun toSummary(): QuoteSummary = QuoteSummary(
        id = id,
        quoteNumber = quoteNumber,
        clientName = clientName,
        jobName = jobName,
        total = total,
        status = status,
        createdAt = createdAt,
        isInvoice = invoiceMode == true,
        invoiceDueDate = invoiceDueDate,
        invoicePaid = invoicePaid == true,
        paidAt = paidAt,
    )

    fun toDetail(): QuoteDetail = QuoteDetail(
        id = id,
        quoteNumber = quoteNumber,
        clientName = clientName,
        clientEmail = clientEmail,
        clientPhone = clientPhone,
        jobName = jobName,
        notes = notes,
        status = status,
        isInvoice = invoiceMode == true,
        invoiceDueDate = invoiceDueDate,
        invoicePaid = invoicePaid == true,
        createdAt = createdAt,
        paidAt = paidAt,
        showMaterials = showMaterials != false,
        clientBuysAll = clientBuysAll == true,
        totalMaterial = totalMaterial,
        totalLabor = totalLabor,
        totalHours = totalHours,
        totalMarkup = totalMarkup,
        taxEnabled = taxEnabled == true,
        totalTax = totalTax,
        total = total,
        markup = markup,
        hourlyRate = hourlyRate,
        taxRate = taxRate,
        rateMode = RateMode.from(rateMode),
        lineItems = parseLineItems(entries, customItems, hourlyRate ?: 85.0),
        customItems = parseCustomItems(customItems),
        catalogEntries = parseCatalogEntries(entries),
    )
}

/** Columns to fetch for list rows (skips the heavy JSON line-item columns). */
internal val QUOTE_LIST_COLUMNS = listOf(
    "id", "quote_number", "client_name", "job_name", "total", "status",
    "created_at", "paid_at", "invoice_mode", "invoice_due_date", "invoice_paid",
)

// All extractors use `as? JsonPrimitive` (never `.jsonPrimitive`, which throws on
// objects/arrays) so a malformed/legacy entries/custom_items blob can't crash a load.
private fun JsonElement.numberOrNull(key: String): Double? =
    ((this as? JsonObject)?.get(key) as? JsonPrimitive)?.doubleOrNull

private fun JsonElement.stringOrNull(key: String): String? {
    val prim = (this as? JsonObject)?.get(key) as? JsonPrimitive ?: return null
    if (prim is JsonNull) return null
    val content = prim.content
    return if (content == "null") null else content
}

/**
 * Flattens the two JSON shapes the web app writes:
 *  - `entries`: an object keyed by catalog id → { qty, ... }. Shown as "name × qty".
 *  - `custom_items`: an array of { label, qty, materialCost, laborCost, kind }.
 *    amount = (materialCost + laborCost) × qty, matching QuotePublicPage.jsx.
 */
private fun parseLineItems(
    entries: JsonElement?,
    customItems: JsonElement?,
    hourlyRate: Double,
): List<QuoteLineItem> {
    val items = mutableListOf<QuoteLineItem>()

    parseCatalogEntries(entries).forEach { entry ->
        val service = Catalog.service(entry.serviceId)
        val variantLabel = service?.variants?.getOrNull(entry.variantIdx)?.label
        val label = buildString {
            append(service?.label ?: entry.serviceId.replace('_', ' '))
            if (variantLabel != null && (service?.variants?.size ?: 0) > 1) append(" · $variantLabel")
        }
        items += QuoteLineItem(
            label = label,
            quantity = entry.qty,
            amount = QuoteCalculator.catalogLineAmount(entry, hourlyRate),
            kind = null,
        )
    }

    (customItems as? JsonArray)?.forEach { element ->
        val label = element.stringOrNull("label") ?: return@forEach
        val qty = element.numberOrNull("qty") ?: 1.0
        if (label.isBlank() || qty <= 0.0) return@forEach
        val material = element.numberOrNull("materialCost") ?: 0.0
        val labor = element.numberOrNull("laborCost") ?: 0.0
        items += QuoteLineItem(
            label = label,
            quantity = qty,
            amount = (material + labor) * qty,
            kind = element.stringOrNull("kind"),
        )
    }

    return items
}

/** Parses the `entries` object into editable [QuoteCatalogEntry]s (qty > 0 only). */
private fun parseCatalogEntries(entries: JsonElement?): List<QuoteCatalogEntry> =
    (entries as? JsonObject)?.mapNotNull { (id, value) ->
        val qty = value.numberOrNull("qty") ?: 0.0
        if (qty <= 0.0) return@mapNotNull null
        QuoteCatalogEntry(
            serviceId = id,
            qty = qty,
            variantIdx = value.numberOrNull("variantIdx")?.toInt() ?: 0,
            clientBuys = ((value as? JsonObject)?.get("clientBuys") as? JsonPrimitive)?.booleanOrNull ?: false,
        )
    }.orEmpty()

/** Parses `custom_items` into editable [QuoteCustomItem]s (keeps blanks out). */
private fun parseCustomItems(customItems: JsonElement?): List<QuoteCustomItem> =
    (customItems as? JsonArray)?.mapNotNull { element ->
        if (element !is JsonObject) return@mapNotNull null
        QuoteCustomItem(
            id = (element["id"] as? JsonPrimitive)?.longOrNull,
            label = element.stringOrNull("label").orEmpty(),
            qty = element.numberOrNull("qty") ?: 1.0,
            materialCost = element.numberOrNull("materialCost") ?: 0.0,
            laborCost = element.numberOrNull("laborCost") ?: 0.0,
            laborHours = element.numberOrNull("laborHours") ?: 0.0,
            kind = element.stringOrNull("kind"),
        )
    }.orEmpty()
