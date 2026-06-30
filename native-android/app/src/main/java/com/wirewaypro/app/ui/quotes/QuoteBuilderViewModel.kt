package com.wirewaypro.app.ui.quotes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.data.ai.AiService
import com.wirewaypro.app.data.ai.TakeoffHandoff
import com.wirewaypro.app.data.prefs.SettingsPrefs
import com.wirewaypro.app.domain.catalog.Catalog
import com.wirewaypro.app.domain.model.QuoteCalculator
import com.wirewaypro.app.domain.model.QuoteCatalogEntry
import com.wirewaypro.app.domain.model.QuoteCustomItem
import com.wirewaypro.app.domain.model.QuoteDetail
import com.wirewaypro.app.domain.model.QuoteInput
import com.wirewaypro.app.domain.model.QuoteTotals
import com.wirewaypro.app.domain.model.RateMode
import com.wirewaypro.app.domain.repository.AuthRepository
import com.wirewaypro.app.domain.repository.QuoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A selected catalog service being edited (numeric qty as text). */
data class CatalogEntryUi(
    val serviceId: String,
    val qty: String = "1",
    val variantIdx: Int = 0,
    val clientBuys: Boolean = false,
)

/** A custom line item being edited; numeric fields are text for smooth input. */
data class CustomItemUi(
    val id: Long? = null,
    val label: String = "",
    val qty: String = "1",
    val materialCost: String = "0",
    val laborCost: String = "0",
    val laborHours: String = "0",
    val kind: String? = null,
)

data class QuoteBuilderUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEdit: Boolean = false,
    val isInvoice: Boolean = false,
    val quoteNumber: String? = null,
    val clientName: String = "",
    val clientEmail: String = "",
    val clientPhone: String = "",
    val jobName: String = "",
    val notes: String = "",
    val markupPct: String = "30",
    val hourlyRate: String = "85",
    val rateMode: RateMode = RateMode.FLAT,
    val clientBuysAll: Boolean = false,   // true = client supplies materials (labor only)
    val taxEnabled: Boolean = false,
    val taxRatePct: String = "8",
    val invoiceDueDate: String = "",
    val invoicePaid: Boolean = false,
    val catalogItems: List<CatalogEntryUi> = emptyList(),
    val items: List<CustomItemUi> = emptyList(),
    val draftingNotes: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
)

private fun String.toD(): Double = trim().toDoubleOrNull() ?: 0.0

@HiltViewModel
class QuoteBuilderViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val quoteRepository: QuoteRepository,
    private val aiService: AiService,
    private val settingsPrefs: SettingsPrefs,
    takeoffHandoff: TakeoffHandoff,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val quoteId: String? =
        savedStateHandle.get<String>(ARG_ID)?.takeIf { it.isNotBlank() }

    private val _state = MutableStateFlow(
        QuoteBuilderUiState(
            isEdit = quoteId != null,
            isInvoice = savedStateHandle.get<Boolean>(ARG_INVOICE) ?: false,
            isLoading = quoteId != null,
        )
    )
    val state: StateFlow<QuoteBuilderUiState> = _state.asStateFlow()

    init {
        if (quoteId != null) {
            loadExisting(quoteId)
        } else {
            // A new quote may have been seeded by the AI takeoff screen.
            takeoffHandoff.take()?.takeIf { it.isNotEmpty() }?.let { entries ->
                _state.update { s ->
                    s.copy(catalogItems = entries.map { CatalogEntryUi(it.serviceId, numText(it.qty), it.variantIdx, it.clientBuys) })
                }
            }
            // Prefill the contractor's baseline hourly rate on a fresh quote.
            viewModelScope.launch {
                val baseline = settingsPrefs.defaultHourlyRate.first()
                if (baseline > 0) _state.update { it.copy(hourlyRate = numText(baseline)) }
            }
        }
    }

    /** Live totals preview, computed with the same calculator the repository saves with. */
    val previewTotals: QuoteTotals
        get() = QuoteCalculator.compute(
            catalogEntries = currentCatalogEntries(),
            customItems = currentItems(),
            markup = _state.value.markupPct.toD() / 100.0,
            taxEnabled = _state.value.taxEnabled,
            taxRate = _state.value.taxRatePct.toD() / 100.0,
            hourlyRate = currentHourlyRate(),
            clientBuysAll = _state.value.clientBuysAll,
        )

    private fun currentHourlyRate(): Double = _state.value.hourlyRate.toD().takeIf { it > 0 } ?: 85.0

    private fun currentCatalogEntries(): List<QuoteCatalogEntry> =
        _state.value.catalogItems.map {
            QuoteCatalogEntry(it.serviceId, it.qty.toD(), it.variantIdx, it.clientBuys)
        }

    private fun currentItems(): List<QuoteCustomItem> =
        _state.value.items.map {
            QuoteCustomItem(
                id = it.id,
                label = it.label.trim(),
                qty = it.qty.toD(),
                materialCost = it.materialCost.toD(),
                laborCost = it.laborCost.toD(),
                laborHours = it.laborHours.toD(),
                kind = it.kind,
            )
        }

    private fun loadExisting(id: String) {
        viewModelScope.launch {
            quoteRepository.getQuote(id)
                .onSuccess { q -> applyLoaded(q) }
                .onFailure { _state.update { it.copy(isLoading = false, error = "Couldn't load this quote.") } }
        }
    }

    private fun applyLoaded(q: QuoteDetail) {
        _state.update {
            it.copy(
                isLoading = false,
                isInvoice = q.isInvoice,
                quoteNumber = q.quoteNumber,
                clientName = q.clientName.orEmpty(),
                clientEmail = q.clientEmail.orEmpty(),
                clientPhone = q.clientPhone.orEmpty(),
                jobName = q.jobName.orEmpty(),
                notes = q.notes.orEmpty(),
                markupPct = pctText(q.markup ?: 0.30),
                hourlyRate = numText(q.hourlyRate ?: 85.0),
                rateMode = q.rateMode,
                clientBuysAll = q.clientBuysAll,
                taxEnabled = q.taxEnabled,
                taxRatePct = pctText(q.taxRate ?: 0.08),
                invoiceDueDate = q.invoiceDueDate.orEmpty(),
                invoicePaid = q.invoicePaid,
                catalogItems = q.catalogEntries.map { e ->
                    CatalogEntryUi(e.serviceId, numText(e.qty), e.variantIdx, e.clientBuys)
                },
                items = q.customItems.map { ci ->
                    CustomItemUi(ci.id, ci.label, numText(ci.qty), numText(ci.materialCost), numText(ci.laborCost), numText(ci.laborHours), ci.kind)
                },
            )
        }
    }

    // ── Field setters ─────────────────────────────────────────────────────────
    fun setClientName(v: String) = _state.update { it.copy(clientName = v, error = null) }
    fun setClientEmail(v: String) = _state.update { it.copy(clientEmail = v) }
    fun setClientPhone(v: String) = _state.update { it.copy(clientPhone = v) }
    fun setJobName(v: String) = _state.update { it.copy(jobName = v) }
    fun setNotes(v: String) = _state.update { it.copy(notes = v) }
    fun setMarkupPct(v: String) = _state.update { it.copy(markupPct = v) }
    fun setHourlyRate(v: String) = _state.update { it.copy(hourlyRate = v) }
    fun setRateMode(v: RateMode) = _state.update { it.copy(rateMode = v) }
    /** Hourly sub-option: true = "Just labor" (client supplies materials). */
    fun setClientBuysAll(v: Boolean) = _state.update { it.copy(clientBuysAll = v) }
    fun setTaxEnabled(v: Boolean) = _state.update { it.copy(taxEnabled = v) }
    fun setTaxRatePct(v: String) = _state.update { it.copy(taxRatePct = v) }
    fun setInvoiceMode(v: Boolean) = _state.update { it.copy(isInvoice = v) }
    fun setInvoiceDueDate(v: String) = _state.update { it.copy(invoiceDueDate = v) }
    fun setInvoicePaid(v: Boolean) = _state.update { it.copy(invoicePaid = v) }

    // ── Catalog entries ─────────────────────────────────────────────────────────
    fun addCatalogEntry(serviceId: String) = _state.update { s ->
        if (s.catalogItems.any { it.serviceId == serviceId }) s
        else s.copy(catalogItems = s.catalogItems + CatalogEntryUi(serviceId), error = null)
    }

    fun updateCatalogEntry(index: Int, transform: (CatalogEntryUi) -> CatalogEntryUi) = _state.update { s ->
        s.copy(catalogItems = s.catalogItems.mapIndexed { i, e -> if (i == index) transform(e) else e })
    }

    fun removeCatalogEntry(index: Int) = _state.update {
        it.copy(catalogItems = it.catalogItems.filterIndexed { i, _ -> i != index })
    }

    // ── Custom items ──────────────────────────────────────────────────────────────
    fun addItem() = _state.update { it.copy(items = it.items + CustomItemUi(), error = null) }

    fun removeItem(index: Int) = _state.update {
        it.copy(items = it.items.filterIndexed { i, _ -> i != index })
    }

    fun updateItem(index: Int, transform: (CustomItemUi) -> CustomItemUi) = _state.update { s ->
        s.copy(items = s.items.mapIndexed { i, item -> if (i == index) transform(item) else item })
    }

    /** Drafts a professional notes/description from the current line items via AI. */
    fun draftNotes() {
        val s = _state.value
        val labels = mutableListOf<String>()
        s.catalogItems.forEach { ci ->
            val svc = Catalog.service(ci.serviceId) ?: return@forEach
            val variant = svc.variants.getOrNull(ci.variantIdx)?.label
            val qty = ci.qty.trim()
            labels += buildString {
                append(svc.label)
                if (variant != null && svc.variants.size > 1) append(" ($variant)")
                if (qty.isNotBlank() && qty != "1") append(" x$qty")
            }
        }
        s.items.forEach {
            val label = it.label.trim()
            if (label.isNotBlank()) {
                val qty = it.qty.trim()
                labels += label + if (qty.isNotBlank() && qty != "1") " x$qty" else ""
            }
        }
        if (labels.isEmpty()) {
            _state.update { it.copy(error = "Add line items first, then draft.") }
            return
        }

        val kind = if (s.isInvoice) "invoice" else "estimate"
        val userText = buildString {
            append("Write a short, professional project description (2-4 sentences, plain text, ")
            append("no markdown, no bullet points) for an electrical $kind")
            s.jobName.ifBlank { null }?.let { append(" for the job \"$it\"") }
            s.clientName.ifBlank { null }?.let { append(", client $it") }
            append(". The work includes: ")
            append(labels.joinToString("; "))
            append(".")
        }
        val system = "You are an assistant for a licensed electrical contractor writing " +
            "customer-facing estimate/invoice descriptions. Be concise, professional, and " +
            "specific. Output only the description text, nothing else."

        _state.update { it.copy(draftingNotes = true, error = null) }
        viewModelScope.launch {
            aiService.complete(system, userText)
                .onSuccess { text -> _state.update { it.copy(draftingNotes = false, notes = text) } }
                .onFailure { _state.update { it.copy(draftingNotes = false, error = "Couldn't draft with AI. Try again.") } }
        }
    }

    fun save() {
        val userId = auth.currentUserId()
        if (userId == null) {
            _state.update { it.copy(error = "Session expired.") }
            return
        }
        val catalog = currentCatalogEntries().filter { it.qty > 0.0 }
        val items = currentItems().filter { it.label.isNotBlank() }
        if (catalog.isEmpty() && items.isEmpty()) {
            _state.update { it.copy(error = "Add at least one catalog or custom line item.") }
            return
        }

        val s = _state.value
        val input = QuoteInput(
            id = quoteId,
            quoteNumber = s.quoteNumber,
            clientName = s.clientName.ifBlank { null },
            clientEmail = s.clientEmail.ifBlank { null },
            clientPhone = s.clientPhone.ifBlank { null },
            jobName = s.jobName.ifBlank { null },
            notes = s.notes.ifBlank { null },
            markup = s.markupPct.toD() / 100.0,
            hourlyRate = currentHourlyRate(),
            rateMode = s.rateMode,
            taxEnabled = s.taxEnabled,
            taxRate = s.taxRatePct.toD() / 100.0,
            invoiceMode = s.isInvoice,
            invoiceDueDate = s.invoiceDueDate.ifBlank { null },
            invoicePaid = s.invoicePaid,
            showMaterials = true,
            clientBuysAll = s.clientBuysAll,
            catalogEntries = catalog,
            customItems = items,
        )

        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            quoteRepository.saveQuote(userId, input)
                .onSuccess { _state.update { it.copy(isSaving = false, saved = true) } }
                .onFailure { _state.update { it.copy(isSaving = false, error = "Couldn't save. Try again.") } }
        }
    }

    companion object {
        const val ARG_ID = "id"
        const val ARG_INVOICE = "invoice"

        private fun pctText(fraction: Double): String = numText(fraction * 100.0)

        private fun numText(value: Double): String =
            if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
    }
}
