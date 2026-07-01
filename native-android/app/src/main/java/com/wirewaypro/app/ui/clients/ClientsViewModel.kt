package com.wirewaypro.app.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.data.offline.NetworkMonitor
import com.wirewaypro.app.domain.model.Client
import com.wirewaypro.app.domain.repository.AuthRepository
import com.wirewaypro.app.domain.repository.ClientRepository
import com.wirewaypro.app.ui.common.ListUiState
import com.wirewaypro.app.ui.common.SyncBannerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClientsViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val clientRepository: ClientRepository,
    network: NetworkMonitor,
) : ViewModel() {

    private val _state = MutableStateFlow(ListUiState<Client>())
    val state: StateFlow<ListUiState<Client>> = _state.asStateFlow()

    /** Offline + pending-sync status for the banner atop the list. */
    val syncBanner: StateFlow<SyncBannerState> =
        combine(network.online, clientRepository.pendingSyncCount()) { online, pending ->
            SyncBannerState(isOffline = !online, pendingCount = pending)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncBannerState())

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
            clientRepository.getClients(userId)
                .onSuccess { list ->
                    _state.update { it.copy(isLoading = false, isRefreshing = false, items = list, error = null) }
                }
                .onFailure {
                    _state.update { it.copy(isLoading = false, isRefreshing = false, error = "Couldn't load clients.") }
                }
        }
    }
}
