package com.wirewaypro.app.ui.jobs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.domain.model.Job
import com.wirewaypro.app.domain.model.JobDraw
import com.wirewaypro.app.domain.model.JobDrawInput
import com.wirewaypro.app.domain.model.JobProfitability
import com.wirewaypro.app.domain.model.MoneyMath
import com.wirewaypro.app.domain.repository.AuthRepository
import com.wirewaypro.app.domain.repository.ExpenseRepository
import com.wirewaypro.app.domain.repository.JobRepository
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

data class JobDetailUiState(
    val isLoading: Boolean = true,
    val job: Job? = null,
    val draws: List<JobDraw> = emptyList(),
    val profitability: JobProfitability? = null,
    val error: String? = null,
    val editingDraw: DrawDraft? = null,
    val deleted: Boolean = false,
)

@HiltViewModel
class JobDetailViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val jobRepository: JobRepository,
    private val expenseRepository: ExpenseRepository,
    private val timeEntryRepository: TimeEntryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val jobId: String = checkNotNull(savedStateHandle[ARG_ID]) { "Missing job id" }

    private val _state = MutableStateFlow(JobDetailUiState())
    val state: StateFlow<JobDetailUiState> = _state.asStateFlow()

    init {
        load()
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
        }
    }

    /**
     * "Did I make money?" — paid draws (net) minus job-tagged expenses and
     * completed time entries. Best-effort: failures just leave the card as-is.
     */
    private suspend fun refreshProfitability(draws: List<JobDraw>) {
        val userId = auth.currentUserId() ?: return
        val expenses = expenseRepository.getExpensesForJob(userId, jobId).getOrDefault(emptyList())
        val entries = timeEntryRepository.getForJob(userId, jobId).getOrDefault(emptyList())
            .filter { !it.isRunning }
        val profitability = JobProfitability(
            collected = MoneyMath.round2(draws.filter { it.status == "paid" }.sumOf { it.net }),
            materials = MoneyMath.round2(expenses.sumOf { it.amount }),
            laborHours = MoneyMath.round2(entries.sumOf { it.hours ?: 0.0 }),
            laborCost = MoneyMath.round2(entries.sumOf { it.laborCost }),
        )
        _state.update { it.copy(profitability = profitability) }
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
