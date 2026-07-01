package com.wirewaypro.app.ui.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.domain.model.AgingReport
import com.wirewaypro.app.domain.model.JobPnlReport
import com.wirewaypro.app.domain.model.MoneySnapshot
import com.wirewaypro.app.domain.repository.AuthRepository
import com.wirewaypro.app.domain.repository.MoneyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class MoneyUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val snapshot: MoneySnapshot? = null,
    val aging: AgingReport? = null,
    val pnl: JobPnlReport? = null,
    val error: String? = null,
    val exporting: Boolean = false,
    val csvExport: String? = null, // one-shot: non-null when ready for the share sheet
    val qbExport: String? = null,  // one-shot: QuickBooks CSV ready for the share sheet
)

@HiltViewModel
class MoneyViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val moneyRepository: MoneyRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MoneyUiState())
    val state: StateFlow<MoneyUiState> = _state.asStateFlow()

    val year: Int = YearMonth.now().year

    init {
        load(isRefresh = false)
    }

    fun refresh() = load(isRefresh = true)

    private fun load(isRefresh: Boolean) {
        val userId = auth.currentUserId()
        if (userId == null) {
            _state.update { it.copy(isLoading = false, isRefreshing = false, error = "Session expired.") }
            return
        }
        _state.update { it.copy(isLoading = !isRefresh && it.snapshot == null, isRefreshing = isRefresh, error = null) }
        val now = YearMonth.now()
        viewModelScope.launch {
            val snapshot = moneyRepository.getSnapshot(userId, now.year, now.monthValue).getOrNull()
            val aging = moneyRepository.getReceivables(userId).getOrNull()
            val pnl = moneyRepository.getJobsPnl(userId).getOrNull()
            if (snapshot == null && aging == null && pnl == null) {
                _state.update { it.copy(isLoading = false, isRefreshing = false, error = "Couldn't load the money dashboard.") }
            } else {
                _state.update { it.copy(isLoading = false, isRefreshing = false, snapshot = snapshot, aging = aging, pnl = pnl, error = null) }
            }
        }
    }

    fun exportCsv() {
        val userId = auth.currentUserId() ?: return
        _state.update { it.copy(exporting = true) }
        viewModelScope.launch {
            moneyRepository.buildAccountantCsv(userId, year)
                .onSuccess { csv -> _state.update { it.copy(exporting = false, csvExport = csv) } }
                .onFailure { _state.update { it.copy(exporting = false, error = "Couldn't build the CSV export.") } }
        }
    }

    /** Called by the screen once the CSV has been handed to the share sheet. */
    fun csvConsumed() = _state.update { it.copy(csvExport = null) }

    /** Builds a QuickBooks Online bank-import CSV and hands it to the share sheet. */
    fun exportQuickBooks() {
        val userId = auth.currentUserId() ?: return
        _state.update { it.copy(exporting = true) }
        viewModelScope.launch {
            moneyRepository.buildQuickBooksCsv(userId, year)
                .onSuccess { csv -> _state.update { it.copy(exporting = false, qbExport = csv) } }
                .onFailure { _state.update { it.copy(exporting = false, error = "Couldn't build the QuickBooks export.") } }
        }
    }

    fun qbConsumed() = _state.update { it.copy(qbExport = null) }
}
