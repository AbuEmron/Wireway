package com.wirewaypro.app.ui.timetracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.domain.model.TimeEntry
import com.wirewaypro.app.domain.model.TripInput
import com.wirewaypro.app.domain.repository.AuthRepository
import com.wirewaypro.app.domain.repository.TimeEntryRepository
import com.wirewaypro.app.domain.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimeUiState(
    val isLoading: Boolean = true,
    val running: TimeEntry? = null,
    val recent: List<TimeEntry> = emptyList(),
    val busy: Boolean = false,
    val promptMiles: Boolean = false, // one-shot: ask for miles after a stop
    val error: String? = null,
) {
    val completed: List<TimeEntry> get() = recent.filter { !it.isRunning }
    val totalHours: Double get() = completed.sumOf { it.hours ?: 0.0 }
}

@HiltViewModel
class TimeTrackingViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val repo: TimeEntryRepository,
    private val tripRepo: TripRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TimeUiState())
    val state: StateFlow<TimeUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        val userId = auth.currentUserId() ?: run {
            _state.update { it.copy(isLoading = false, error = "Session expired.") }
            return
        }
        viewModelScope.launch {
            val running = repo.getRunning(userId).getOrNull()
            val recent = repo.getRecent(userId).getOrNull().orEmpty()
            _state.update { it.copy(isLoading = false, running = running, recent = recent, error = null) }
        }
    }

    /** Start the "on my way / on the job" timer. */
    fun startTimer(jobId: String? = null) {
        val userId = auth.currentUserId() ?: return
        if (_state.value.running != null) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            repo.start(userId, jobId, rate = 0.0)
                .onSuccess { _state.update { s -> s.copy(busy = false) }; load() }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.message ?: "Couldn't start the timer.") } }
        }
    }

    /** Stop the running timer; hours are computed from clock-in. */
    fun stopTimer() {
        val userId = auth.currentUserId() ?: return
        val running = _state.value.running ?: return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            repo.stop(userId, running)
                .onSuccess { _state.update { s -> s.copy(busy = false, promptMiles = true) }; load() }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.message ?: "Couldn't stop the timer.") } }
        }
    }

    /** Log the miles driven for the trip that just ended (from the stop prompt). */
    fun logMilesForTrip(miles: Double) {
        val userId = auth.currentUserId() ?: return
        _state.update { it.copy(promptMiles = false) }
        if (miles <= 0) return
        viewModelScope.launch {
            tripRepo.addTrip(
                userId,
                TripInput(
                    tripDate = LocalDate.now().toString(),
                    miles = miles,
                    purpose = "Drive to job",
                ),
            )
        }
    }

    fun dismissMilesPrompt() = _state.update { it.copy(promptMiles = false) }

    fun addManual(hours: Double, notes: String?, onSaved: () -> Unit) {
        val userId = auth.currentUserId() ?: return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            repo.addManual(userId, jobId = null, hours = hours, rate = 0.0, notes = notes)
                .onSuccess { _state.update { s -> s.copy(busy = false) }; onSaved(); load() }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.message ?: "Couldn't add the entry.") } }
        }
    }

    fun delete(entryId: String) {
        val userId = auth.currentUserId() ?: return
        viewModelScope.launch { repo.delete(userId, entryId).onSuccess { load() } }
    }
}
