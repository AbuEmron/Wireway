package com.wirewaypro.app.ui.ahj

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.data.location.LocationService
import com.wirewaypro.app.domain.ahj.AhjCoverage
import com.wirewaypro.app.domain.ahj.JurisdictionDraft
import com.wirewaypro.app.domain.ahj.UsState
import com.wirewaypro.app.domain.ahj.UsStates
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

data class JurisdictionUiState(
    val isLoading: Boolean = true,
    val states: List<UsState> = UsStates.all,
    val draft: JurisdictionDraft = JurisdictionDraft(),
    /** Live, honest coverage preview for whatever state is currently chosen. */
    val preview: AhjCoverage = AhjCoverage.of(null),
    val gpsBusy: Boolean = false,
    /** Set when a GPS suggestion was applied, so the UI can say "confirm or change it". */
    val gpsApplied: Boolean = false,
    /** True when the device has no location permission yet (drives the request). */
    val needsLocationPermission: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
) {
    val selectedState: UsState? get() = UsStates.byCode(draft.stateCode)
    val canSave: Boolean get() = draft.isValid && !saving
}

/**
 * Drives the universal AHJ jurisdiction picker: seed from the saved jurisdiction,
 * let the user pick state → optional county → optional city, preview the honest
 * coverage live, optionally pre-fill from GPS (always user-confirmed), and save.
 */
@HiltViewModel
class JurisdictionViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val repository: JurisdictionRepository,
    private val location: LocationService,
) : ViewModel() {

    private val _state = MutableStateFlow(JurisdictionUiState())
    val state: StateFlow<JurisdictionUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val userId = auth.currentUserId()
            val current = userId?.let { repository.getJurisdiction(it).getOrNull() }
            val draft = current?.let {
                JurisdictionDraft(
                    id = it.id,
                    stateCode = it.stateCode,
                    county = it.county.orEmpty(),
                    city = it.city.orEmpty(),
                )
            } ?: JurisdictionDraft()
            _state.update {
                it.copy(
                    isLoading = false,
                    draft = draft,
                    preview = previewFor(draft),
                    needsLocationPermission = !location.hasPermission(),
                )
            }
        }
    }

    fun selectState(code: String) = updateDraft { it.withState(code) }

    fun setCounty(county: String) = updateDraft { it.copy(county = county) }

    fun setCity(city: String) = updateDraft { it.copy(city = city) }

    private fun updateDraft(transform: (JurisdictionDraft) -> JurisdictionDraft) =
        _state.update {
            val next = transform(it.draft)
            it.copy(draft = next, preview = previewFor(next), gpsApplied = false, error = null)
        }

    /** The user tapped "Use my location". Pre-fills the draft; they still confirm + save. */
    fun useMyLocation() {
        if (!location.hasPermission()) {
            _state.update { it.copy(needsLocationPermission = true) }
            return
        }
        _state.update { it.copy(gpsBusy = true, error = null) }
        viewModelScope.launch {
            val suggestion = location.suggestJurisdiction()
            _state.update { s ->
                if (suggestion == null) {
                    s.copy(gpsBusy = false, error = "Couldn't determine your area — pick it below.")
                } else {
                    val next = s.draft
                        .withState(suggestion.stateCode)
                        .copy(
                            county = suggestion.county.orEmpty(),
                            city = suggestion.city.orEmpty(),
                            fromGps = true,
                        )
                    s.copy(draft = next, preview = previewFor(next), gpsBusy = false, gpsApplied = true)
                }
            }
        }
    }

    /** Called once the runtime location permission result is known. */
    fun onLocationPermissionResult(granted: Boolean) {
        _state.update { it.copy(needsLocationPermission = !granted) }
        if (granted) useMyLocation()
    }

    fun save() {
        val userId = auth.currentUserId() ?: return
        val input = _state.value.draft.toInput() ?: return
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            repository.saveJurisdiction(userId, input)
                .onSuccess { _state.update { s -> s.copy(saving = false, saved = true) } }
                .onFailure { e ->
                    _state.update { it.copy(saving = false, error = e.message ?: "Couldn't save your jurisdiction.") }
                }
        }
    }

    /** Build the honest coverage preview from the draft's currently-chosen parts. */
    private fun previewFor(draft: JurisdictionDraft): AhjCoverage {
        val state = draft.stateCode ?: return AhjCoverage.of(null)
        return AhjCoverage.of(
            Jurisdiction(
                id = "preview",
                stateCode = state,
                county = draft.county.trim().ifBlank { null },
                city = draft.city.trim().ifBlank { null },
            ),
        )
    }
}
