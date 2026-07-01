package com.wirewaypro.app.domain.model

/**
 * Whole-quote pricing mode, persisted to `quotes.rate_mode`.
 *  - [FLAT]: the catalog/itemized total (materials + labor + markup + tax).
 *  - [HOURLY]: a time bid — total = estimated hours × hourly rate.
 *
 * Distinct from the web's `flat_rate_mode` (a proposal presentation toggle).
 */
enum class RateMode(val value: String) {
    FLAT("flat"),
    HOURLY("hourly");

    companion object {
        fun from(value: String?): RateMode = entries.firstOrNull { it.value == value } ?: FLAT
    }
}

/**
 * The `quotes` table backs BOTH estimates and invoices — an invoice is simply a
 * quote row with `invoice_mode = true`. [isInvoice] distinguishes them.
 *
 * [QuoteSummary] is the list-row projection; [QuoteDetail] is the full record.
 */
/**
 * Offline-first sync state of a locally-held record.
 *  - [SYNCED]  — matches the server; nothing pending.
 *  - [PENDING] — created/edited/deleted locally, waiting to push.
 *  - [ERROR]   — the server rejected the push; kept locally so it's never lost,
 *                surfaced so the user can retry.
 */
enum class SyncState { SYNCED, PENDING, ERROR }

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
    val syncState: SyncState = SyncState.SYNCED,
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
    val clientBuysAll: Boolean,   // true = client supplies materials (labor only)
    val totalMaterial: Double?,
    val totalLabor: Double?,
    val totalHours: Double?,
    val totalMarkup: Double?,
    val taxEnabled: Boolean,
    val totalTax: Double?,
    val total: Double?,
    val markup: Double?,
    val hourlyRate: Double?,
    val taxRate: Double?,
    /** Whole-percent deposit required to accept this estimate (null/0 = none). */
    val depositPercent: Int? = null,
    val rateMode: RateMode,
    // Display merge of catalog `entries` + `custom_items` (read-only screens).
    val lineItems: List<QuoteLineItem>,
    // The editable custom items only (parsed from `custom_items`), for the builder.
    val customItems: List<QuoteCustomItem>,
    // The editable catalog selections (parsed from `entries`), for the builder.
    val catalogEntries: List<QuoteCatalogEntry>,
) {
    /** Deposit due on acceptance ([depositPercent] of the headline total). */
    val depositDue: Double?
        get() = depositPercent?.takeIf { it > 0 }?.let { pct ->
            total?.let { MoneyMath.round2(it * pct / 100.0) }
        }
}

/**
 * A selected catalog service — the `entries` JSON shape the web app writes,
 * keyed by catalog id: { [serviceId]: { qty, variantIdx, clientBuys } }.
 */
data class QuoteCatalogEntry(
    val serviceId: String,
    val qty: Double = 1.0,
    val variantIdx: Int = 0,
    val clientBuys: Boolean = false,
)

/**
 * An editable custom line item — the `custom_items` JSON shape the web app writes:
 * { id, label, qty, materialCost, laborCost, laborHours, kind? }.
 */
data class QuoteCustomItem(
    val id: Long? = null,
    val label: String,
    val qty: Double = 1.0,
    val materialCost: Double = 0.0,
    val laborCost: Double = 0.0,
    val laborHours: Double = 0.0,
    val kind: String? = null,
)

/**
 * Everything the quote builder can write — both catalog [catalogEntries] and
 * [customItems] are fully editable; the repository computes totals and writes the
 * `entries` + `custom_items` JSON from these.
 */
data class QuoteInput(
    val id: String?,            // null = create
    val quoteNumber: String?,   // null = generate WW-YYYY-NNN
    val clientName: String?,
    val clientEmail: String?,
    val clientPhone: String?,
    val jobName: String?,
    val notes: String?,
    val markup: Double,         // fraction, e.g. 0.30 = 30%
    val hourlyRate: Double,
    val rateMode: RateMode,     // whole-quote pricing mode (flat catalog vs hourly)
    val taxEnabled: Boolean,
    val taxRate: Double,        // fraction, e.g. 0.08
    val depositPercent: Int?,   // whole percent required to accept (null = none)
    val invoiceMode: Boolean,   // true = invoice, false = estimate
    val invoiceDueDate: String?,
    val invoicePaid: Boolean,
    val showMaterials: Boolean,
    val clientBuysAll: Boolean,   // true = client supplies materials (labor only)
    val catalogEntries: List<QuoteCatalogEntry>,
    val customItems: List<QuoteCustomItem>,
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
