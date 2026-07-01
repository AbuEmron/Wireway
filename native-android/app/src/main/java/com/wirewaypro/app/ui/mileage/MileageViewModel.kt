package com.wirewaypro.app.ui.mileage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.domain.model.Mileage
import com.wirewaypro.app.domain.model.Trip
import com.wirewaypro.app.domain.model.TripInput
import com.wirewaypro.app.domain.repository.AuthRepository
import com.wirewaypro.app.domain.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Year
import javax.inject.Inject

data class MileageUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val trips: List<Trip> = emptyList(),
    val saving: Boolean = false,
    val error: String? = null,
) {
    val totalMiles: Double get() = trips.sumOf { it.miles }
    val deduction: Double get() = Mileage.deduction(totalMiles)
    val isEmpty: Boolean get() = trips.isEmpty()
}

@HiltViewModel
class MileageViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val tripRepository: TripRepository,
) : ViewModel() {

    val year: Int = Year.now().value

    private val _state = MutableStateFlow(MileageUiState())
    val state: StateFlow<MileageUiState> = _state.asStateFlow()

    init {
        load(isRefresh = false)
    }

    fun refresh() = load(isRefresh = true)

    private fun load(isRefresh: Boolean) {
        val userId = auth.currentUserId() ?: run {
            _state.update { it.copy(isLoading = false, isRefreshing = false, error = "Session expired.") }
            return
        }
        _state.update { it.copy(isLoading = !isRefresh && it.trips.isEmpty(), isRefreshing = isRefresh, error = null) }
        viewModelScope.launch {
            tripRepository.getTrips(userId, year)
                .onSuccess { list -> _state.update { it.copy(isLoading = false, isRefreshing = false, trips = list, error = null) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, isRefreshing = false, error = e.message ?: "Couldn't load trips.") } }
        }
    }

    /** Logs a trip; calls [onSaved] on success so the screen can collapse its form. */
    fun addTrip(input: TripInput, onSaved: () -> Unit) {
        val userId = auth.currentUserId() ?: return
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            tripRepository.addTrip(userId, input)
                .onSuccess {
                    _state.update { it.copy(saving = false) }
                    onSaved()
                    refresh()
                }
                .onFailure { e -> _state.update { it.copy(saving = false, error = e.message ?: "Couldn't save the trip.") } }
        }
    }

    fun deleteTrip(tripId: String) {
        val userId = auth.currentUserId() ?: return
        viewModelScope.launch {
            tripRepository.deleteTrip(userId, tripId).onSuccess { refresh() }
        }
    }
}
