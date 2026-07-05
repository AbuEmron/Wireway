package com.wirewaypro.app.ui.jobs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.data.entitlements.TierService
import com.wirewaypro.app.domain.model.CrewMember
import com.wirewaypro.app.domain.model.Job
import com.wirewaypro.app.domain.model.JobCosting
import com.wirewaypro.app.domain.model.JobDraw
import com.wirewaypro.app.domain.model.JobDrawInput
import com.wirewaypro.app.domain.model.JobProfitability
import com.wirewaypro.app.domain.model.MoneyMath
import com.wirewaypro.app.domain.model.TimeEntry
import com.wirewaypro.app.domain.model.Tier
import com.wirewaypro.app.domain.repository.AuthRepository
import com.wirewaypro.app.data.prefs.SettingsPrefs
import com.wirewaypro.app.domain.repository.CrewRepository
import com.wirewaypro.app.domain.repository.ExpenseRepository
import com.wirewaypro.app.domain.repository.JobRepository
import com.wirewaypro.app.domain.repository.QuoteRepository
import com.wirewaypro.app.domain.repository.TimeEntryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Editable draw fields (numeric values as text). id == null → a new draw. */
data class DrawDraft(
    val id: String? = null,
    val label: String = "",
    val amount: String = "0",
    val retainagePct: String = "0",
    val status: String = "pending",
    val dueDate: String = "",
    val sortOrder: Int = 0,
) {
    companion object {
        val STATUSES = listOf("pending", "invoiced", "paid")
    }
}

/**
 * Editor for logging a crew member's hours against this job. [crewMemberId] null
 * until a crew member is chosen; [hours] as text. Labor cost = hours × the chosen
 * crew member's cost rate, computed deterministically at save.
 */
data class CrewLogDraft(
    val crewMemberId: String? = null,
    val hours: String = "",
    val notes: String = "",
)

data class JobDetailUiState(
    val isLoading: Boolean = true,
    val job: Job? = null,
    val draws: List<JobDraw> = emptyList(),
    val profitability: JobProfitability? = null,
    val reviewLink: String = "",
    val error: String? = null,
    val editingDraw: DrawDraft? = null,
    val deleted: Boolean = false,
    // Defaults to ELITE so the upgrade moment never flashes for paying users
    // before the real tier resolves.
    val tier: Tier = Tier.ELITE,
    /** The job's contract total ("you bid"), set for Elite's bid-vs-actual rows. */
    val estimateTotal: Double? = null,
    // ── Elite crew + time (only populated for Elite) ─────────────────────────
    /** Active crew for the "log hours" picker. */
    val crew: List<CrewMember> = emptyList(),
    /** All time entries tagged to this job (running + completed), newest first. */
    val timeEntries: List<TimeEntry> = emptyList(),
    /** The open "log crew hours" editor, or null. */
    val crewLog: CrewLogDraft? = null,
    /** Elite true job costing: estimate vs actuals + true profit (null for lower tiers). */
    val costing: JobCosting? = null,
) {
    /** Entries still on the clock for this job (multiple crew can be clocked in). */
    val runningEntries: List<TimeEntry> get() = timeEntries.filter { it.isRunning }
}

@HiltViewModel
class JobDetailViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val jobRepository: JobRepository,
    private val expenseRepository: ExpenseRepository,
    private val timeEntryRepository: TimeEntryRepository,
    private val crewRepository: CrewRepository,
    private val quoteRepository: QuoteRepository,
    private val settingsPrefs: SettingsPrefs,
    private val tierService: TierService,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val jobId: String = checkNotNull(savedStateHandle[ARG_ID]) { "Missing job id" }

    private val _state = MutableStateFlow(JobDetailUiState())
    val state: StateFlow<JobDetailUiState> = _state.asStateFlow()

    // Estimate basis for job costing, loaded once per job from the linked quote.
    // Labor is compared in HOURS (the estimate's labor dollars are a bill rate,
    // not a cost — see JobCosting); materials in cost dollars.
    private var estLaborHours = 0.0
    private var estMaterialCost = 0.0
    private var hasEstimate = false

    init {
        load()
        viewModelScope.launch {
            settingsPrefs.reviewLink.collect { link -> _state.update { it.copy(reviewLink = link) } }
        }
    }

    fun load() {
        // Spinner only on first load; a reload keeps the current job on screen.
        _state.update { it.copy(isLoading = it.job == null, error = null) }
        viewModelScope.launch {
            val job = jobRepository.getJob(jobId).getOrNull()
            if (job == null) {
                _state.update { st ->
                    st.copy(isLoading = false, error = if (st.job == null) "Couldn't load this job." else st.error)
                }
                return@launch
            }
            val draws = jobRepository.getJobDraws(jobId).getOrDefault(emptyList())
            _state.update { it.copy(isLoading = false, job = job, draws = draws, error = null) }
            refreshProfitability(draws)

            // Elite's bid-vs-actual: the job's contract total is the bid, so
            // "Did I make money?" can line it up against real costs. Lower
            // tiers see the contextual Elite moment instead.
            val tier = tierService.current()
            _state.update {
                it.copy(tier = tier, estimateTotal = if (tier.atLeast(Tier.ELITE)) job.total else null)
            }
            // Elite: load the crew roster + the estimate basis for true costing.
            if (tier.atLeast(Tier.ELITE)) {
                val userId = auth.currentUserId()
                val crew = userId?.let { crewRepository.getCrew(it).getOrDefault(emptyList()) }
                    ?.filter { it.active }.orEmpty()
                _state.update { it.copy(crew = crew) }
                loadEstimateBasis(job)
                // Rebuild costing now that the estimate basis is loaded.
                _state.value.profitability?.let { buildCosting(it) }
            }
        }
    }

    /**
     * Pulls the estimate basis from the job's linked quote: estimated labor HOURS
     * (quote total_hours) and estimated material COST (quote total_material). No
     * linked quote → no estimate side (the card shows actuals only, never a fake $0).
     */
    private suspend fun loadEstimateBasis(job: Job) {
        val quoteId = job.quoteId
        if (quoteId.isNullOrBlank()) {
            hasEstimate = false
            estLaborHours = 0.0
            estMaterialCost = 0.0
            return
        }
        val quote = quoteRepository.getQuote(quoteId).getOrNull()
        if (quote == null) {
            hasEstimate = false
            return
        }
        estLaborHours = quote.totalHours ?: 0.0
        estMaterialCost = quote.totalMaterial ?: 0.0
        hasEstimate = quote.totalHours != null || quote.totalMaterial != null
    }

    /** Builds the Elite [JobCosting] from the estimate basis + recorded actuals. */
    private fun buildCosting(p: JobProfitability) {
        _state.update {
            it.copy(
                costing = JobCosting(
                    estimatedLaborHours = estLaborHours,
                    actualLaborHours = p.laborHours,
                    actualLaborCost = p.laborCost,
                    estimatedMaterialCost = estMaterialCost,
                    actualMaterialCost = p.materials,
                    collected = p.collected,
                    hasEstimate = hasEstimate,
                ),
            )
        }
    }

    /**
     * "Did I make money?" — paid draws (net) minus job-tagged expenses and
     * completed time entries. Best-effort: failures just leave the card as-is.
     */
    private suspend fun refreshProfitability(draws: List<JobDraw>) {
        val userId = auth.currentUserId() ?: return
        val expenses = expenseRepository.getExpensesForJob(userId, jobId).getOrDefault(emptyList())
        val allEntries = timeEntryRepository.getForJob(userId, jobId).getOrDefault(emptyList())
        // Only COMPLETED entries contribute real labor cost; running timers don't
        // count until they're stopped (hours are still accruing).
        val completed = allEntries.filter { !it.isRunning }
        val profitability = JobProfitability(
            collected = MoneyMath.round2(draws.filter { it.status == "paid" }.sumOf { it.net }),
            materials = MoneyMath.round2(expenses.sumOf { it.amount }),
            laborHours = MoneyMath.round2(completed.sumOf { it.hours ?: 0.0 }),
            laborCost = MoneyMath.round2(completed.sumOf { it.laborCost }),
        )
        _state.update { it.copy(profitability = profitability, timeEntries = allEntries) }
        // Keep the Elite costing card in step with freshly-logged actuals.
        if (_state.value.tier.atLeast(Tier.ELITE)) buildCosting(profitability)
    }

    // ── Elite: log crew hours against this job ────────────────────────────────
    fun openCrewLog() = _state.update {
        it.copy(crewLog = CrewLogDraft(crewMemberId = it.crew.firstOrNull()?.id))
    }

    fun updateCrewLog(transform: (CrewLogDraft) -> CrewLogDraft) = _state.update {
        it.copy(crewLog = it.crewLog?.let(transform))
    }

    fun closeCrewLog() = _state.update { it.copy(crewLog = null) }

    /** Saves a manual crew-hours entry: labor cost = hours × that crew member's cost rate. */
    fun saveCrewLog() {
        val userId = auth.currentUserId() ?: return
        val draft = _state.value.crewLog ?: return
        val member = _state.value.crew.firstOrNull { it.id == draft.crewMemberId } ?: return
        val hours = draft.hours.trim().toDoubleOrNull() ?: return
        if (hours <= 0.0) return
        viewModelScope.launch {
            timeEntryRepository.addManual(
                userId = userId,
                jobId = jobId,
                hours = hours,
                rate = member.hourlyCostRate, // deterministic cost rate snapshot
                notes = draft.notes.trim().ifBlank { null },
                crewMemberId = member.id,
                workerName = member.name,
            )
            _state.update { it.copy(crewLog = null) }
            reloadDraws()
        }
    }

    /** Clocks a crew member in on this job (rate snapshotted from the roster). */
    fun clockInCrew(crewMemberId: String) {
        val userId = auth.currentUserId() ?: return
        val member = _state.value.crew.firstOrNull { it.id == crewMemberId } ?: return
        viewModelScope.launch {
            timeEntryRepository.start(
                userId = userId,
                jobId = jobId,
                rate = member.hourlyCostRate,
                crewMemberId = member.id,
                workerName = member.name,
            )
            reloadDraws()
        }
    }

    /** Clocks a running crew entry out; hours computed from clock-in feed job costing. */
    fun clockOutEntry(entry: TimeEntry) {
        val userId = auth.currentUserId() ?: return
        viewModelScope.launch {
            timeEntryRepository.stop(userId, entry)
            reloadDraws()
        }
    }

    fun deleteTimeEntry(entryId: String) {
        val userId = auth.currentUserId() ?: return
        viewModelScope.launch {
            timeEntryRepository.delete(userId, entryId)
            reloadDraws()
        }
    }

    private suspend fun reloadDraws() {
        val draws = jobRepository.getJobDraws(jobId).getOrDefault(emptyList())
        _state.update { it.copy(draws = draws) }
        refreshProfitability(draws)
    }

    // ── Draw editor ─────────────────────────────────────────────────────────────
    fun addDraw() = _state.update {
        it.copy(editingDraw = DrawDraft(sortOrder = it.draws.size))
    }

    fun editDraw(draw: JobDraw) = _state.update {
        it.copy(
            editingDraw = DrawDraft(
                id = draw.id,
                label = draw.label,
                amount = numText(draw.amount),
                retainagePct = numText(draw.retainagePct),
                status = draw.status,
                dueDate = draw.dueDate.orEmpty(),
                sortOrder = draw.sortOrder,
            )
        )
    }

    fun updateDrawDraft(transform: (DrawDraft) -> DrawDraft) = _state.update {
        it.copy(editingDraw = it.editingDraw?.let(transform))
    }

    fun closeDrawEditor() = _state.update { it.copy(editingDraw = null) }

    fun saveDraw() {
        val userId = auth.currentUserId() ?: return
        val draft = _state.value.editingDraw ?: return
        if (draft.label.isBlank()) {
            _state.update { it.copy(editingDraw = draft.copy(label = draft.label)) } // no-op; keep open
            return
        }
        val input = JobDrawInput(
            id = draft.id,
            jobId = jobId,
            label = draft.label.trim(),
            amount = draft.amount.trim().toDoubleOrNull() ?: 0.0,
            retainagePct = draft.retainagePct.trim().toDoubleOrNull() ?: 0.0,
            status = draft.status,
            dueDate = draft.dueDate.ifBlank { null },
            sortOrder = draft.sortOrder,
        )
        viewModelScope.launch {
            jobRepository.saveDraw(userId, input)
            _state.update { it.copy(editingDraw = null) }
            reloadDraws()
        }
    }

    fun deleteDraw(drawId: String) {
        val userId = auth.currentUserId() ?: return
        viewModelScope.launch {
            jobRepository.deleteDraw(userId, drawId)
            reloadDraws()
        }
    }

    fun cycleDrawStatus(draw: JobDraw) {
        val userId = auth.currentUserId() ?: return
        val order = DrawDraft.STATUSES
        val next = order[(order.indexOf(draw.status).coerceAtLeast(0) + 1) % order.size]
        viewModelScope.launch {
            jobRepository.setDrawStatus(userId, draw.id, next)
            reloadDraws()
        }
    }

    fun deleteJob() {
        val userId = auth.currentUserId() ?: return
        viewModelScope.launch {
            jobRepository.deleteJob(userId, jobId)
                .onSuccess { _state.update { it.copy(deleted = true) } }
                .onFailure { _state.update { it.copy(error = "Couldn't delete this job.") } }
        }
    }

    companion object {
        const val ARG_ID = "id"

        private fun numText(value: Double): String =
            if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
    }
}
