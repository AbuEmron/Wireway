package com.wirewaypro.app.ui.quotes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.data.ai.AiService
import com.wirewaypro.app.data.ai.PricingAdvisorService
import com.wirewaypro.app.data.ai.TakeoffHandoff
import com.wirewaypro.app.data.entitlements.TierService
import com.wirewaypro.app.data.location.LocationService
import com.wirewaypro.app.data.prefs.DEFAULT_HOURLY_RATE
import com.wirewaypro.app.data.prefs.SettingsPrefs
import com.wirewaypro.app.data.quotes.DraftCatalogEntry
import com.wirewaypro.app.data.quotes.DraftCustomItem
import com.wirewaypro.app.data.quotes.OverrideTrail
import com.wirewaypro.app.data.quotes.QuoteDraft
import com.wirewaypro.app.data.quotes.QuoteDraftStore
import com.wirewaypro.app.domain.audit.OverrideAudit
import com.wirewaypro.app.domain.catalog.Catalog
import com.wirewaypro.app.domain.model.FreeLimits
import com.wirewaypro.app.domain.model.QuoteCalculator
import com.wirewaypro.app.domain.model.QuoteCatalogEntry
import com.wirewaypro.app.domain.model.QuoteCustomItem
import com.wirewaypro.app.domain.model.QuoteDetail
import com.wirewaypro.app.domain.model.PricingRecommendation
import com.wirewaypro.app.domain.model.QuoteInput
import com.wirewaypro.app.domain.model.QuoteTotals
import com.wirewaypro.app.domain.model.RateMode
import com.wirewaypro.app.domain.model.Tier
import com.wirewaypro.app.domain.pricing.DefaultRate
import com.wirewaypro.app.domain.repository.AuthRepository
import com.wirewaypro.app.domain.repository.QuoteRepository
import com.wirewaypro.app.domain.validation.EstimateSanity
import com.wirewaypro.app.domain.validation.SanityFlag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
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
    /** One-line note on where the seeded default rate came from (approximate, edit me). */
    val rateHint: String? = null,
    val rateMode: RateMode = RateMode.FLAT,
    val clientBuysAll: Boolean = false,   // true = client supplies materials (labor only)
    val taxEnabled: Boolean = false,
    val taxRatePct: String = "8",
    val depositPct: String = "",   // whole % of total due to accept ("" = none)
    val invoiceDueDate: String = "",
    val invoicePaid: Boolean = false,
    val catalogItems: List<CatalogEntryUi> = emptyList(),
    val items: List<CustomItemUi> = emptyList(),
    val draftingNotes: Boolean = false,
    // AI pricing advisor (Batch D)
    val locationInput: String = "",
    val locatingArea: Boolean = false,
    val advising: Boolean = false,
    val advice: PricingRecommendation? = null,
    val adviceError: String? = null,
    val error: String? = null,
    val saved: Boolean = false,
    /** True when a Free user hit the saved-quote ceiling — the Pro moment. */
    val quoteCapReached: Boolean = false,
)

private fun String.toD(): Double = trim().toDoubleOrNull() ?: 0.0

@HiltViewModel
class QuoteBuilderViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val quoteRepository: QuoteRepository,
    private val aiService: AiService,
    private val settingsPrefs: SettingsPrefs,
    private val pricingAdvisor: PricingAdvisorService,
    private val locationService: LocationService,
    private val draftStore: QuoteDraftStore,
    private val takeoffHandoff: TakeoffHandoff,
    private val tierService: TierService,
    private val overrideTrail: OverrideTrail,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /**
     * The derived numbers this session was seeded with (resolved default rate,
     * template labor, takeoff quantities). Diffed against the saved values to
     * record the manual-override audit trail — empty on plain edits, where the
     * starting values are the contractor's own, not calculations.
     */
    private var derivedBaseline: List<OverrideAudit.Derived> = emptyList()

    private val quoteId: String? =
        savedStateHandle.get<String>(ARG_ID)?.takeIf { it.isNotBlank() }

    private val isInvoiceArg: Boolean = savedStateHandle.get<Boolean>(ARG_INVOICE) ?: false

    /** Stable key for this builder session's autosaved draft. */
    private val draftKey: String = draftStore.keyFor(quoteId, isInvoiceArg)

    private val _state = MutableStateFlow(
        QuoteBuilderUiState(
            isEdit = quoteId != null,
            isInvoice = isInvoiceArg,
            isLoading = quoteId != null,
        )
    )
    val state: StateFlow<QuoteBuilderUiState> = _state.asStateFlow()

    /** The autosave collector — cancelled on a successful save so a debounced
     *  write can't re-create the draft that save() just cleared. */
    private var autosaveJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            // Restore an autosaved draft first — a crash/kill mid-estimate must
            // never lose work. A draft always wins over the saved copy (it IS the
            // newer, unsynced edit). No draft → load the quote (edit) or seed (new).
            val draft = draftStore.load(draftKey)
            when {
                draft != null -> applyDraft(draft)
                quoteId != null -> loadExisting(quoteId)
                else -> seedNewQuote()
            }
            _state.update { it.copy(isLoading = false) }

            // On an untouched edit, don't autosave the loaded quote back as a draft
            // until the user actually changes something (avoids stale drafts).
            val baseline = if (quoteId != null && draft == null) _state.value.toDraft() else null
            autosaveJob = _state.map { it.toDraft() }
                .distinctUntilChanged()
                .debounce(AUTOSAVE_DEBOUNCE_MS)
                .onEach { d ->
                    if (!d.isEmpty && d != baseline) {
                        draftStore.save(draftKey, d, System.currentTimeMillis())
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    /**
     * A new quote may have been seeded by the AI takeoff screen; also prefill the
     * rate. The default rate is resolved offline: the contractor's saved rate if
     * set, otherwise their regional typical band, otherwise a national starting
     * point — never the blind $85. [rateHint] tells the user where it came from.
     */
    private suspend fun seedNewQuote() {
        val derived = mutableListOf<OverrideAudit.Derived>()
        // Rate first: template custom lines price their labor from it below.
        val resolved = DefaultRate.resolve(
            personalRate = settingsPrefs.rawDefaultHourlyRate.first(),
            regionalTypical = settingsPrefs.regionalDefaultRate.first(),
            national = DEFAULT_HOURLY_RATE,
        )
        _state.update { it.copy(hourlyRate = numText(resolved.rate), rateHint = resolved.hint) }
        derived += OverrideAudit.Derived("rate:hourly", "Hourly rate", resolved.rate)

        takeoffHandoff.take()?.takeIf { it.isNotEmpty() }?.let { entries ->
            _state.update { s ->
                s.copy(catalogItems = entries.map { CatalogEntryUi(it.serviceId, numText(it.qty), it.variantIdx, it.clientBuys) })
            }
            entries.forEach { e ->
                val label = Catalog.service(e.serviceId)?.label ?: e.serviceId
                derived += OverrideAudit.Derived("catalog-qty:${e.serviceId}", "Qty — $label", e.qty)
            }
        }
        // Commercial/industrial template lines: labor priced at the contractor's
        // OWN rate (typical hours × rate, editable); material left at 0 for the
        // supplier's quote — never a fabricated number.
        takeoffHandoff.takeCustom()?.takeIf { it.isNotEmpty() }?.let { customs ->
            _state.update { s ->
                s.copy(
                    items = s.items + customs.map { c ->
                        CustomItemUi(
                            label = c.label,
                            qty = numText(c.qty),
                            materialCost = "0",
                            laborCost = numText(Math.round(c.laborHours * resolved.rate).toDouble()),
                            laborHours = numText(c.laborHours),
                        )
                    },
                )
            }
            customs.forEach { c ->
                val seededCost = Math.round(c.laborHours * resolved.rate).toDouble()
                derived += OverrideAudit.Derived("labor-hours:${c.label.trim().lowercase()}", "Labor hrs — ${c.label}", c.laborHours)
                derived += OverrideAudit.Derived("labor-cost:${c.label.trim().lowercase()}", "Labor — ${c.label}", seededCost)
            }
        }
        derivedBaseline = derived
    }

    /** The saved values for every derived key, for the override diff. */
    private fun savedDerivedValues(): Map<String, Double> {
        val s = _state.value
        val m = mutableMapOf<String, Double>()
        m["rate:hourly"] = s.hourlyRate.toD()
        s.catalogItems.forEach { m["catalog-qty:${it.serviceId}"] = it.qty.toD() }
        s.items.forEach { item ->
            val key = item.label.trim().lowercase()
            if (key.isNotBlank()) {
                m["labor-hours:$key"] = item.laborHours.toD()
                m["labor-cost:$key"] = item.laborCost.toD()
            }
        }
        return m
    }

    /** Current form → serializable draft snapshot. */
    private fun QuoteBuilderUiState.toDraft(): QuoteDraft = QuoteDraft(
        isInvoice = isInvoice,
        quoteNumber = quoteNumber,
        clientName = clientName,
        clientEmail = clientEmail,
        clientPhone = clientPhone,
        jobName = jobName,
        notes = notes,
        markupPct = markupPct,
        hourlyRate = hourlyRate,
        rateMode = rateMode.value,
        clientBuysAll = clientBuysAll,
        taxEnabled = taxEnabled,
        taxRatePct = taxRatePct,
        depositPct = depositPct,
        invoiceDueDate = invoiceDueDate,
        invoicePaid = invoicePaid,
        catalogItems = catalogItems.map { DraftCatalogEntry(it.serviceId, it.qty, it.variantIdx, it.clientBuys) },
        items = items.map { DraftCustomItem(it.id, it.label, it.qty, it.materialCost, it.laborCost, it.laborHours, it.kind) },
    )

    /** Restore a saved draft into the form. */
    private fun applyDraft(d: QuoteDraft) {
        _state.update {
            it.copy(
                isLoading = false,
                isInvoice = d.isInvoice,
                quoteNumber = d.quoteNumber,
                clientName = d.clientName,
                clientEmail = d.clientEmail,
                clientPhone = d.clientPhone,
                jobName = d.jobName,
                notes = d.notes,
                markupPct = d.markupPct,
                hourlyRate = d.hourlyRate,
                rateMode = RateMode.from(d.rateMode),
                clientBuysAll = d.clientBuysAll,
                taxEnabled = d.taxEnabled,
                taxRatePct = d.taxRatePct,
                depositPct = d.depositPct,
                invoiceDueDate = d.invoiceDueDate,
                invoicePaid = d.invoicePaid,
                catalogItems = d.catalogItems.map { CatalogEntryUi(it.serviceId, it.qty, it.variantIdx, it.clientBuys) },
                items = d.items.map { CustomItemUi(it.id, it.label, it.qty, it.materialCost, it.laborCost, it.laborHours, it.kind) },
            )
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

    /**
     * Deterministic heads-ups on the current form (missing permit line,
     * fat-fingered quantity, unpriced labor…) — pure rules off the template
     * library, recomputed live like [previewTotals]. Advisory, never blocking.
     */
    val sanityFlags: List<SanityFlag>
        get() = EstimateSanity.check(
            catalogEntries = currentCatalogEntries(),
            customItems = currentItems(),
            totals = previewTotals,
            clientBuysAll = _state.value.clientBuysAll,
            depositPercent = _state.value.depositPct.trim().toDoubleOrNull()?.let { kotlin.math.round(it).toInt() },
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

    private suspend fun loadExisting(id: String) {
        quoteRepository.getQuote(id)
            .onSuccess { q -> applyLoaded(q) }
            .onFailure { _state.update { it.copy(isLoading = false, error = "Couldn't load this quote.") } }
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
                depositPct = q.depositPercent?.takeIf { it > 0 }?.toString().orEmpty(),
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

    /** Voice-dictated scope lands appended to Notes — reviewable, never auto-priced. */
    fun appendScopeNote(text: String) = _state.update {
        val body = text.trim()
        it.copy(notes = if (it.notes.isBlank()) body else it.notes.trimEnd() + "\n" + body)
    }

    /** The device has no speech recognizer — say so instead of failing silently. */
    fun dictationUnavailable() =
        _state.update { it.copy(error = "Voice input isn't available on this device — type the scope instead.") }
    fun setMarkupPct(v: String) = _state.update { it.copy(markupPct = v) }
    fun setHourlyRate(v: String) = _state.update { it.copy(hourlyRate = v, rateHint = null) }
    fun setRateMode(v: RateMode) = _state.update { it.copy(rateMode = v) }
    /** Hourly sub-option: true = "Just labor" (client supplies materials). */
    fun setClientBuysAll(v: Boolean) = _state.update { it.copy(clientBuysAll = v) }
    fun setTaxEnabled(v: Boolean) = _state.update { it.copy(taxEnabled = v) }

    // ── AI pricing advisor (Batch D) ─────────────────────────────────────────────
    fun setLocationInput(v: String) = _state.update { it.copy(locationInput = v, adviceError = null) }

    /** Fill the location from GPS. The UI requests the runtime permission first. */
    fun useMyLocation() {
        _state.update { it.copy(locatingArea = true, adviceError = null) }
        viewModelScope.launch {
            val area = locationService.currentArea()
            _state.update {
                it.copy(
                    locatingArea = false,
                    locationInput = area?.label ?: it.locationInput,
                    adviceError = if (area == null) "Couldn't read your location — type the job address instead." else null,
                )
            }
        }
    }

    /** Ask the AI to suggest pricing for the current job in the entered area. */
    fun requestPricing() {
        val desc = currentJobDescription()
        _state.update { it.copy(advising = true, adviceError = null, advice = null) }
        viewModelScope.launch {
            val hourly = settingsPrefs.defaultHourlyRate.first()
            val flat = settingsPrefs.defaultFlatRate.first().takeIf { it > 0 }
            pricingAdvisor.recommend(desc, _state.value.locationInput.trim(), hourly, flat)
                .onSuccess { rec -> _state.update { it.copy(advising = false, advice = rec) } }
                .onFailure { e -> _state.update { it.copy(advising = false, adviceError = e.message ?: "Couldn't get a suggestion. Try again.") } }
        }
    }

    /** Apply the suggestion — sets the mode, and the hourly rate when hourly. */
    fun applyAdvice() {
        val rec = _state.value.advice ?: return
        _state.update { s ->
            s.copy(
                rateMode = rec.mode,
                hourlyRate = if (rec.mode == RateMode.HOURLY && (rec.recommendedRate ?: 0.0) > 0)
                    numText(rec.recommendedRate!!) else s.hourlyRate,
                rateHint = null,
                advice = null,
            )
        }
    }

    fun dismissAdvice() = _state.update { it.copy(advice = null, adviceError = null) }

    /** A short plain-English description of the current line items for the AI. */
    private fun currentJobDescription(): String {
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
        val base = labels.joinToString("; ").ifBlank { "general residential electrical work" }
        return s.jobName.ifBlank { null }?.let { "$it — $base" } ?: base
    }
    fun setTaxRatePct(v: String) = _state.update { it.copy(taxRatePct = v) }
    fun setDepositPct(v: String) = _state.update { it.copy(depositPct = v) }
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
                .onFailure { e -> _state.update { it.copy(draftingNotes = false, error = e.message ?: "Couldn't draft with AI. Try again.") } }
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
            depositPercent = s.depositPct.trim().toDoubleOrNull()?.let { kotlin.math.round(it).toInt() }?.coerceIn(0, 100)?.takeIf { it > 0 },
            invoiceMode = s.isInvoice,
            invoiceDueDate = s.invoiceDueDate.ifBlank { null },
            invoicePaid = s.invoicePaid,
            showMaterials = true,
            clientBuysAll = s.clientBuysAll,
            catalogEntries = catalog,
            customItems = items,
        )

        _state.update { it.copy(isSaving = true, error = null, quoteCapReached = false) }
        viewModelScope.launch {
            // Free ceiling (WIREWAY_PRICING_TIERS.md): a small number of complete
            // quotes proves the loop end to end; unlimited lives in Pro. Applies
            // only to NEW records — edits to existing ones always save.
            if (quoteId == null && !tierService.current().atLeast(Tier.PRO)) {
                val saved = (quoteRepository.getEstimates(userId).getOrNull()?.size ?: 0) +
                    (quoteRepository.getInvoices(userId).getOrNull()?.size ?: 0)
                if (saved >= FreeLimits.MAX_QUOTES) {
                    _state.update { it.copy(isSaving = false, quoteCapReached = true) }
                    return@launch
                }
            }
            quoteRepository.saveQuote(userId, input)
                .onSuccess { savedQuote ->
                    // The quote is now persisted for real — stop autosaving FIRST
                    // (a debounced write landing after the clear would resurrect
                    // the draft and pre-fill the next new estimate), then drop it.
                    autosaveJob?.cancel()
                    draftStore.clear(draftKey)
                    // Audit trail: any seeded/calculated number the contractor
                    // changed is recorded (original → override), never blocked.
                    overrideTrail.record(
                        savedQuote.id,
                        OverrideAudit.diff(derivedBaseline, savedDerivedValues()),
                        System.currentTimeMillis(),
                    )
                    _state.update { it.copy(isSaving = false, saved = true) }
                }
                .onFailure { _state.update { it.copy(isSaving = false, error = "Couldn't save. Try again.") } }
        }
    }

    companion object {
        const val ARG_ID = "id"
        const val ARG_INVOICE = "invoice"

        /** Coalesce rapid keystrokes into one draft write instead of one per char. */
        private const val AUTOSAVE_DEBOUNCE_MS = 400L

        private fun pctText(fraction: Double): String = numText(fraction * 100.0)

        private fun numText(value: Double): String =
            if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
    }
}
