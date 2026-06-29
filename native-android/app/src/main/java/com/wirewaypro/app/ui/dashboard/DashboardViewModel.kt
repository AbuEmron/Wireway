package com.wirewaypro.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.data.offline.SyncManager
import com.wirewaypro.app.data.widget.WidgetUpdater
import com.wirewaypro.app.domain.model.UserProfile
import com.wirewaypro.app.domain.repository.AuthRepository
import com.wirewaypro.app.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val jobCount: Long? = null,
    val error: String? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val widgetUpdater: WidgetUpdater,
    syncManager: SyncManager,
) : ViewModel() {

    private val _home = MutableStateFlow(HomeUiState())
    val home: StateFlow<HomeUiState> = _home.asStateFlow()

    /** Count of writes waiting to sync (drives the "pending sync" chip). */
    val pendingSync: StateFlow<Int> = syncManager.pendingCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        loadHome()
    }

    /** Pulls the profile row + job count from Supabase to prove the data layer. */
    fun loadHome() {
        val userId = authRepository.currentUserId()
        if (userId == null) {
            _home.update { it.copy(isLoading = false, error = "Session expired.") }
            return
        }

        _home.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val profileResult = profileRepository.getProfile(userId)
            val jobCountResult = profileRepository.getJobCount(userId)

            val error = (profileResult.exceptionOrNull() ?: jobCountResult.exceptionOrNull())
                ?.let { "Couldn't load your data. Pull to retry." }

            _home.update {
                it.copy(
                    isLoading = false,
                    profile = profileResult.getOrNull() ?: it.profile,
                    jobCount = jobCountResult.getOrNull() ?: it.jobCount,
                    error = error,
                )
            }
        }

        // Refresh the home-screen widget's cached snapshot (best-effort, off the UI path).
        viewModelScope.launch { widgetUpdater.refresh() }
    }
}
