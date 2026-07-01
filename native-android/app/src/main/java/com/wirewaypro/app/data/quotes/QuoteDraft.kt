package com.wirewaypro.app.data.quotes

import kotlinx.serialization.Serializable

/**
 * Serializable snapshot of the quote-builder form, persisted as the draft's
 * [com.wirewaypro.app.data.local.QuoteDraftEntity.contentJson]. Mirrors the
 * builder's UI state at the field level — numeric inputs are kept as the raw
 * TEXT the user is typing (e.g. "8." mid-entry) so a restore lands them exactly
 * where they were, not on a reformatted value.
 *
 * The ViewModel owns the mapping to/from its UI state; this type is pure data so
 * the data layer never depends on UI classes.
 */
@Serializable
data class QuoteDraft(
    val isInvoice: Boolean = false,
    val quoteNumber: String? = null,
    val clientName: String = "",
    val clientEmail: String = "",
    val clientPhone: String = "",
    val jobName: String = "",
    val notes: String = "",
    val markupPct: String = "30",
    val hourlyRate: String = "85",
    val rateMode: String = "flat",
    val clientBuysAll: Boolean = false,
    val taxEnabled: Boolean = false,
    val taxRatePct: String = "8",
    val invoiceDueDate: String = "",
    val invoicePaid: Boolean = false,
    val catalogItems: List<DraftCatalogEntry> = emptyList(),
    val items: List<DraftCustomItem> = emptyList(),
) {
    /** True when the form has nothing worth restoring — avoids saving empty drafts. */
    val isEmpty: Boolean
        get() = clientName.isBlank() && clientEmail.isBlank() && clientPhone.isBlank() &&
            jobName.isBlank() && notes.isBlank() && catalogItems.isEmpty() && items.isEmpty()
}

@Serializable
data class DraftCatalogEntry(
    val serviceId: String,
    val qty: String = "1",
    val variantIdx: Int = 0,
    val clientBuys: Boolean = false,
)

@Serializable
data class DraftCustomItem(
    val id: Long? = null,
    val label: String = "",
    val qty: String = "1",
    val materialCost: String = "0",
    val laborCost: String = "0",
    val laborHours: String = "0",
    val kind: String? = null,
)
