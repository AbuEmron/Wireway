package com.wirewaypro.app.domain.model

/**
 * The `quotes` table backs BOTH estimates and invoices — an invoice is simply a
 * quote row with `invoice_mode = true`. [isInvoice] distinguishes them.
 *
 * [QuoteSummary] is the list-row projection; [QuoteDetail] is the full record.
 */
data class QuoteSummary(
    val id: String,
    val quoteNumber: String?,
    val clientName: String?,
    val jobName: String?,
    val total: Double?,
    val status: String?,        // draft | sent | accepted | paid | ...
    val createdAt: String?,
    val isInvoice: Boolean,
    val invoiceDueDate: String?,
    val invoicePaid: Boolean,
    val paidAt: String?,
)

data class QuoteDetail(
    val id: String,
    val quoteNumber: String?,
    val clientName: String?,
    val clientEmail: String?,
    val clientPhone: String?,
    val jobName: String?,
    val notes: String?,
    val status: String?,
    val isInvoice: Boolean,
    val invoiceDueDate: String?,
    val invoicePaid: Boolean,
    val createdAt: String?,
    val paidAt: String?,
    // Money breakdown
    val showMaterials: Boolean,
    val totalMaterial: Double?,
    val totalLabor: Double?,
    val totalHours: Double?,
    val totalMarkup: Double?,
    val taxEnabled: Boolean,
    val totalTax: Double?,
    val total: Double?,
    // Line items live in the JSON `entries` / `custom_items` columns.
    val lineItems: List<QuoteLineItem>,
)

/**
 * A single line on a quote. Catalog items (`entries`) carry a quantity but no
 * standalone price in the row; custom items (`custom_items`) carry a computed
 * amount. [amount] is null when the line has no resolvable price.
 */
data class QuoteLineItem(
    val label: String,
    val quantity: Double,
    val amount: Double?,
    val kind: String?, // e.g. "mileage", or null for a normal line
)
