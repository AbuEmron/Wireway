package com.wirewaypro.app.ui.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val error: String? = null,
)

@HiltViewModel
class MoneyViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val moneyRepository: MoneyRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MoneyUiState())
    val state: StateFlow<MoneyUiState> = _state.asStateFlow()

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
            moneyRepository.getSnapshot(userId, now.year, now.monthValue)
                .onSuccess { snap -> _state.update { it.copy(isLoading = false, isRefreshing = false, snapshot = snap, error = null) } }
                .onFailure { _state.update { it.copy(isLoading = false, isRefreshing = false, error = "Couldn't load the money dashboard.") } }
        }
    }
}
