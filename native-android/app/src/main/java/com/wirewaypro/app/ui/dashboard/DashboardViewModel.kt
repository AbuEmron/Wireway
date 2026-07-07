package com.wirewaypro.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.data.entitlements.PlayEntitlements
import com.wirewaypro.app.data.offline.SyncManager
import com.wirewaypro.app.data.widget.WidgetUpdater
import com.wirewaypro.app.domain.model.MoneySnapshot
import com.wirewaypro.app.domain.model.QuoteSummary
import com.wirewaypro.app.domain.model.Tier
import com.wirewaypro.app.domain.model.UserProfile
import com.wirewaypro.app.domain.repository.AuthRepository
import com.wirewaypro.app.domain.repository.MoneyRepository
import com.wirewaypro.app.domain.repository.ProfileRepository
import com.wirewaypro.app.domain.repository.QuoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val jobCount: Long? = null,
    val snapshot: MoneySnapshot? = null,
    /** The newest few estimates for the dashboard's "Recent estimates" rail. */
    val recent: List<QuoteSummary> = emptyList(),
    val error: String? = null,
    // Effective tier (server plan OR Play purchase — highest wins). Defaults to
    // ELITE so paid features never flash locked while the tier resolves.
    val tier: Tier = Tier.ELITE,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val moneyRepository: MoneyRepository,
    private val quoteRepository: QuoteRepository,
    private val widgetUpdater: WidgetUpdater,
    private val playEntitlements: PlayEntitlements,
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
            val now = YearMonth.now()
            val snapshotResult = moneyRepository.getSnapshot(userId, now.year, now.monthValue)
            // Newest three estimates for the home rail (best-effort; the rail
            // simply hides when empty or unavailable).
            val recentResult = quoteRepository.getEstimates(userId)
                .map { list -> list.sortedByDescending { it.createdAt.orEmpty() }.take(3) }

            val error = (profileResult.exceptionOrNull() ?: jobCountResult.exceptionOrNull())
                ?.let { "Couldn't load your data. Pull to retry." }

            _home.update {
                val profile = profileResult.getOrNull() ?: it.profile
                it.copy(
                    isLoading = false,
                    profile = profile,
                    jobCount = jobCountResult.getOrNull() ?: it.jobCount,
                    snapshot = snapshotResult.getOrNull() ?: it.snapshot,
                    recent = recentResult.getOrNull() ?: it.recent,
                    error = error,
                    // Resolve from the profile just fetched (no second network hit)
                    // + the device's owned Play subscriptions — highest wins.
                    tier = Tier.resolve(profile, playEntitlements.ownedProducts.value),
                )
            }
        }

        // Refresh the home-screen widget's cached snapshot (best-effort, off the UI path).
        viewModelScope.launch { widgetUpdater.refresh() }
    }
}
