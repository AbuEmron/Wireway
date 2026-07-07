package com.wirewaypro.app.ui.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.data.offline.NetworkMonitor
import com.wirewaypro.app.data.offline.SyncManager
import com.wirewaypro.app.domain.model.Job
import com.wirewaypro.app.domain.repository.AuthRepository
import com.wirewaypro.app.domain.repository.JobRepository
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
class JobsViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val jobRepository: JobRepository,
    network: NetworkMonitor,
    private val syncManager: SyncManager,
) : ViewModel() {

    private val _state = MutableStateFlow(ListUiState<Job>())
    val state: StateFlow<ListUiState<Job>> = _state.asStateFlow()

    /** Offline + pending + failed-sync status for the banner atop the list. */
    val syncBanner: StateFlow<SyncBannerState> =
        combine(network.online, jobRepository.pendingSyncCount(), syncManager.failedCount) { online, pending, failed ->
            SyncBannerState(isOffline = !online, pendingCount = pending, failedCount = failed)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncBannerState())

    /** Re-arm and flush writes parked after exhausting auto-retry. */
    fun retrySync() {
        viewModelScope.launch { syncManager.retryFailed() }
    }

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
            it.copy(
                isLoading = !isRefresh && it.items.isEmpty(),
                isRefreshing = isRefresh,
                error = null,
            )
        }
        viewModelScope.launch {
            jobRepository.getJobs(userId)
                .onSuccess { jobs ->
                    _state.update { it.copy(isLoading = false, isRefreshing = false, items = jobs, error = null) }
                }
                .onFailure {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = "Couldn't load jobs.",
                        )
                    }
                }
        }
    }
}
