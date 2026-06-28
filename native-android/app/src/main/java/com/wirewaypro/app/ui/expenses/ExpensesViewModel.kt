package com.wirewaypro.app.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.domain.model.Expense
import com.wirewaypro.app.domain.repository.AuthRepository
import com.wirewaypro.app.domain.repository.ExpenseRepository
import com.wirewaypro.app.ui.common.ListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Year
import javax.inject.Inject

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val expenseRepository: ExpenseRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ListUiState<Expense>())
    val state: StateFlow<ListUiState<Expense>> = _state.asStateFlow()

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
        _state.update {
            it.copy(isLoading = !isRefresh && it.items.isEmpty(), isRefreshing = isRefresh, error = null)
        }
        viewModelScope.launch {
            expenseRepository.getExpenses(userId, Year.now().value)
                .onSuccess { list ->
                    _state.update { it.copy(isLoading = false, isRefreshing = false, items = list, error = null) }
                }
                .onFailure {
                    _state.update { it.copy(isLoading = false, isRefreshing = false, error = "Couldn't load expenses.") }
                }
        }
    }

    fun delete(expenseId: String) {
        val userId = auth.currentUserId() ?: return
        viewModelScope.launch {
            expenseRepository.deleteExpense(userId, expenseId)
            refresh()
        }
    }
}
