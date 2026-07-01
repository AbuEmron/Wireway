package com.wirewaypro.app.ui.jobs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.domain.model.Job
import com.wirewaypro.app.domain.model.JobInput
import com.wirewaypro.app.domain.repository.AuthRepository
import com.wirewaypro.app.domain.repository.JobRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JobEditUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEdit: Boolean = false,
    val title: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val clientEmail: String = "",
    val jobAddress: String = "",
    val scheduledDate: String = "",
    val scheduledTime: String = "",
    val durationHours: String = "",
    val status: String = "scheduled",
    val total: String = "",
    val notes: String = "",
    val error: String? = null,
    val saved: Boolean = false,
) {
    companion object {
        val STATUSES = listOf("scheduled", "in_progress", "complete", "cancelled")
    }
}

@HiltViewModel
class JobEditViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val jobRepository: JobRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val jobId: String? = savedStateHandle.get<String>(ARG_ID)?.takeIf { it.isNotBlank() }
    private val linkedQuoteId: String? = savedStateHandle.get<String>(ARG_QUOTE_ID)?.takeIf { it.isNotBlank() }

    private val _state = MutableStateFlow(JobEditUiState(isEdit = jobId != null, isLoading = jobId != null))
    val state: StateFlow<JobEditUiState> = _state.asStateFlow()

    init {
        if (jobId != null) load(jobId)
    }

    private fun load(id: String) {
        viewModelScope.launch {
            jobRepository.getJob(id)
                .onSuccess { j -> apply(j) }
                .onFailure { _state.update { it.copy(isLoading = false, error = "Couldn't load this job.") } }
        }
    }

    private fun apply(j: Job) = _state.update {
        it.copy(
            isLoading = false,
            title = j.title,
            clientName = j.clientName.orEmpty(),
            clientPhone = j.clientPhone.orEmpty(),
            clientEmail = j.clientEmail.orEmpty(),
            jobAddress = j.jobAddress.orEmpty(),
            scheduledDate = j.scheduledDate.orEmpty(),
            scheduledTime = j.scheduledTime?.take(5).orEmpty(),
            durationHours = j.durationHours?.let { d -> if (d % 1.0 == 0.0) d.toLong().toString() else d.toString() }.orEmpty(),
            status = j.status ?: "scheduled",
            total = j.total?.let { t -> if (t % 1.0 == 0.0) t.toLong().toString() else t.toString() }.orEmpty(),
            notes = j.notes.orEmpty(),
        )
    }

    fun setTitle(v: String) = _state.update { it.copy(title = v, error = null) }
    fun setClientName(v: String) = _state.update { it.copy(clientName = v) }
    fun setClientPhone(v: String) = _state.update { it.copy(clientPhone = v) }
    fun setClientEmail(v: String) = _state.update { it.copy(clientEmail = v) }
    fun setJobAddress(v: String) = _state.update { it.copy(jobAddress = v) }
    fun setScheduledDate(v: String) = _state.update { it.copy(scheduledDate = v) }
    fun setScheduledTime(v: String) = _state.update { it.copy(scheduledTime = v) }
    fun setDurationHours(v: String) = _state.update { it.copy(durationHours = v) }
    fun setStatus(v: String) = _state.update { it.copy(status = v) }
    fun setTotal(v: String) = _state.update { it.copy(total = v) }
    fun setNotes(v: String) = _state.update { it.copy(notes = v) }

    fun save() {
        val userId = auth.currentUserId()
        if (userId == null) {
            _state.update { it.copy(error = "Session expired.") }
            return
        }
        val s = _state.value
        if (s.title.isBlank()) {
            _state.update { it.copy(error = "A job title is required.") }
            return
        }
        val input = JobInput(
            id = jobId,
            title = s.title.trim(),
            clientName = s.clientName.ifBlank { null },
            clientPhone = s.clientPhone.ifBlank { null },
            clientEmail = s.clientEmail.ifBlank { null },
            jobAddress = s.jobAddress.ifBlank { null },
            notes = s.notes.ifBlank { null },
            scheduledDate = s.scheduledDate.ifBlank { null },
            scheduledTime = s.scheduledTime.ifBlank { null },
            durationHours = s.durationHours.trim().toDoubleOrNull(),
            status = s.status,
            total = s.total.trim().toDoubleOrNull(),
            quoteId = linkedQuoteId,
        )
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            jobRepository.saveJob(userId, input)
                .onSuccess { _state.update { it.copy(isSaving = false, saved = true) } }
                .onFailure { _state.update { it.copy(isSaving = false, error = "Couldn't save. Try again.") } }
        }
    }

    companion object {
        const val ARG_ID = "id"
        const val ARG_QUOTE_ID = "quoteId"
    }
}
