package com.wirewaypro.app.ui.quotes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.data.ai.MaterialPullService
import com.wirewaypro.app.data.entitlements.TierService
import com.wirewaypro.app.data.location.LocationService
import com.wirewaypro.app.domain.model.PullListResult
import com.wirewaypro.app.domain.model.QuoteDetail
import com.wirewaypro.app.domain.model.Tier
import com.wirewaypro.app.domain.repository.QuoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MaterialPullUiState(
    val isLoadingQuote: Boolean = true,
    val isPro: Boolean = true,
    val jobName: String? = null,
    val lines: List<String> = emptyList(),
    val locationInput: String = "",
    val locatingArea: Boolean = false,
    val building: Boolean = false,
    val result: PullListResult? = null,
    val error: String? = null,
)

/**
 * Drives the Material Pull List screen: loads the quote's line items, resolves
 * the job area (typed or GPS), gates on Pro, and builds the AI pull list.
 */
@HiltViewModel
class MaterialPullViewModel @Inject constructor(
    private val quoteRepository: QuoteRepository,
    private val tierService: TierService,
    private val pullService: MaterialPullService,
    private val locationService: LocationService,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val quoteId: String? = savedStateHandle.get<String>(ARG_ID)?.takeIf { it.isNotBlank() }

    private val _state = MutableStateFlow(MaterialPullUiState())
    val state: StateFlow<MaterialPullUiState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        val id = quoteId
        if (id == null) {
            _state.update { it.copy(isLoadingQuote = false, error = "Missing quote.") }
            return
        }
        viewModelScope.launch {
            // Tier engine (server plan OR Play purchase — highest wins), so a
            // fresh Play upgrade unlocks the pull list before backend sync.
            val isPro = tierService.current().atLeast(Tier.PRO)
            quoteRepository.getQuote(id)
                .onSuccess { q -> applyLoaded(q, isPro) }
                .onFailure { _state.update { it.copy(isLoadingQuote = false, isPro = isPro, error = "Couldn't load this quote.") } }
        }
    }

    private fun applyLoaded(q: QuoteDetail, isPro: Boolean) {
        val lines = q.lineItems.map { li ->
            buildString {
                append("- ").append(li.label)
                if (li.quantity != 1.0) append(" × ").append(numText(li.quantity))
                if (q.clientBuysAll) append(" [client supplies materials]")
            }
        }
        _state.update {
            it.copy(
                isLoadingQuote = false,
                isPro = isPro,
                jobName = q.jobName,
                lines = lines,
                locationInput = q.jobName.orEmpty(), // a sensible starting hint; user edits
            )
        }
    }

    fun setLocationInput(v: String) = _state.update { it.copy(locationInput = v, error = null) }

    fun useMyLocation() {
        _state.update { it.copy(locatingArea = true, error = null) }
        viewModelScope.launch {
            val area = locationService.currentArea()
            _state.update {
                it.copy(
                    locatingArea = false,
                    locationInput = area?.label ?: it.locationInput,
                    error = if (area == null) "Couldn't read your location — type the job address instead." else null,
                )
            }
        }
    }

    fun build() {
        val s = _state.value
        if (!s.isPro) return
        if (s.lines.isEmpty()) {
            _state.update { it.copy(error = "This quote has no line items to source.") }
            return
        }
        _state.update { it.copy(building = true, error = null, result = null) }
        viewModelScope.launch {
            pullService.build(s.jobName, s.lines, s.locationInput.trim())
                .onSuccess { r -> _state.update { it.copy(building = false, result = r) } }
                .onFailure { e -> _state.update { it.copy(building = false, error = e.message ?: "Couldn't build the pull list. Try again.") } }
        }
    }

    companion object {
        const val ARG_ID = "id"

        private fun numText(value: Double): String =
            if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
    }
}
