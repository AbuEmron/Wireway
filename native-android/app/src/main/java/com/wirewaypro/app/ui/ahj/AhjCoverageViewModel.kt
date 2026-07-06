package com.wirewaypro.app.ui.ahj

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.domain.ahj.AhjCoverage
import com.wirewaypro.app.domain.model.Jurisdiction
import com.wirewaypro.app.domain.repository.AuthRepository
import com.wirewaypro.app.domain.repository.JurisdictionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AhjCoverageUiState(
    val loading: Boolean = true,
    val jurisdiction: Jurisdiction? = null,
    val coverage: AhjCoverage = AhjCoverage.of(null),
) {
    val hasJurisdiction: Boolean get() = jurisdiction != null
}

/**
 * Backs the drop-in [AhjCoverageCard]: streams the user's current jurisdiction and
 * turns it into the honest [AhjCoverage] surface. Kept self-contained so any
 * screen (estimate/job detail, home nudge) can show coverage without threading it
 * through that screen's own ViewModel.
 */
@HiltViewModel
class AhjCoverageViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val repository: JurisdictionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AhjCoverageUiState())
    val state: StateFlow<AhjCoverageUiState> = _state.asStateFlow()

    init {
        val userId = auth.currentUserId()
        if (userId == null) {
            _state.update { it.copy(loading = false) }
        } else {
            viewModelScope.launch {
                // Best-effort refresh so a jurisdiction set on another device shows up,
                // then stream local changes as the source of truth.
                repository.getJurisdiction(userId)
                repository.observeJurisdiction(userId).collect { j ->
                    _state.update { it.copy(loading = false, jurisdiction = j, coverage = AhjCoverage.of(j)) }
                }
            }
        }
    }
}
