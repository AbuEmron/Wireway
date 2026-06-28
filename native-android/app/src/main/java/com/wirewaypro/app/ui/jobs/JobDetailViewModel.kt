package com.wirewaypro.app.ui.jobs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.domain.model.Job
import com.wirewaypro.app.domain.model.JobDraw
import com.wirewaypro.app.domain.repository.JobRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JobDetailUiState(
    val isLoading: Boolean = true,
    val job: Job? = null,
    val draws: List<JobDraw> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class JobDetailViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val jobId: String = checkNotNull(savedStateHandle[ARG_ID]) { "Missing job id" }

    private val _state = MutableStateFlow(JobDetailUiState())
    val state: StateFlow<JobDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val jobResult = jobRepository.getJob(jobId)
            val job = jobResult.getOrNull()
            if (job == null) {
                _state.update { it.copy(isLoading = false, error = "Couldn't load this job.") }
                return@launch
            }
            // Draws are supplementary — a failure here shouldn't blank the screen.
            val draws = jobRepository.getJobDraws(jobId).getOrDefault(emptyList())
            _state.update { JobDetailUiState(isLoading = false, job = job, draws = draws, error = null) }
        }
    }

    companion object {
        const val ARG_ID = "id"
    }
}
