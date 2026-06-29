package com.wirewaypro.app.ui.bank

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.data.plaid.PlaidService
import com.wirewaypro.app.domain.model.PlaidTxn
import com.wirewaypro.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BankUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val transactions: List<PlaidTxn> = emptyList(),
    val error: String? = null,
    val linking: Boolean = false,
    val status: String? = null,
    val pendingLinkToken: String? = null, // one-shot: the screen launches Plaid Link with it
) {
    val isEmpty: Boolean get() = transactions.isEmpty()
}

@HiltViewModel
class BankViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val plaidService: PlaidService,
) : ViewModel() {

    private val _state = MutableStateFlow(BankUiState())
    val state: StateFlow<BankUiState> = _state.asStateFlow()

    init {
        load(isRefresh = false)
    }

    fun refresh() = load(isRefresh = true)

    private fun load(isRefresh: Boolean) {
        val userId = auth.currentUserId() ?: run {
            _state.update { it.copy(isLoading = false, isRefreshing = false, error = "Session expired.") }
            return
        }
        _state.update { it.copy(isLoading = !isRefresh && it.transactions.isEmpty(), isRefreshing = isRefresh, error = null) }
        viewModelScope.launch {
            plaidService.getTransactions(userId)
                .onSuccess { txns -> _state.update { it.copy(isLoading = false, isRefreshing = false, transactions = txns, error = null) } }
                .onFailure { _state.update { it.copy(isLoading = false, isRefreshing = false, error = "Couldn't load transactions.") } }
        }
    }

    /** Fetch a Plaid link token; the screen opens Plaid Link with it. */
    fun connectBank() {
        _state.update { it.copy(linking = true, status = null, error = null) }
        viewModelScope.launch {
            plaidService.createLinkToken()
                .onSuccess { token -> _state.update { it.copy(linking = false, pendingLinkToken = token) } }
                .onFailure { _state.update { it.copy(linking = false, error = "Couldn't start bank connection. Check your plan/network.") } }
        }
    }

    fun linkConsumed() = _state.update { it.copy(pendingLinkToken = null) }

    /** Called after a successful Plaid Link: exchange the public token + sync. */
    fun onLinked(publicToken: String, institutionId: String?, institutionName: String?) {
        _state.update { it.copy(linking = true, status = "Linking bank…") }
        viewModelScope.launch {
            plaidService.exchangeToken(publicToken, institutionId, institutionName)
                .onFailure { _state.update { it.copy(linking = false, error = "Couldn't link the bank.") }; return@launch }
            _state.update { it.copy(status = "Syncing transactions…") }
            val synced = plaidService.sync().getOrDefault(0)
            _state.update { it.copy(linking = false, status = "Synced $synced transactions") }
            refresh()
        }
    }

    fun onLinkCancelled() = _state.update { it.copy(linking = false, status = null) }
}
