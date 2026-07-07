package com.wirewaypro.app.ui.crew

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.data.entitlements.TierService
import com.wirewaypro.app.domain.model.CrewMember
import com.wirewaypro.app.domain.model.CrewMemberInput
import com.wirewaypro.app.domain.model.Tier
import com.wirewaypro.app.domain.repository.AuthRepository
import com.wirewaypro.app.domain.repository.CrewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Editable crew fields (rate as text). id == null → a new crew member. */
data class CrewDraft(
    val id: String? = null,
    val name: String = "",
    val role: String = "",
    val hourlyCostRate: String = "",
    val active: Boolean = true,
)

data class CrewUiState(
    val isLoading: Boolean = true,
    // Defaults to ELITE so the roster never flashes the upgrade wall for paying
    // users before the real tier resolves.
    val tier: Tier = Tier.ELITE,
    val crew: List<CrewMember> = emptyList(),
    val editing: CrewDraft? = null,
    val error: String? = null,
) {
    val isElite: Boolean get() = tier.atLeast(Tier.ELITE)
}

@HiltViewModel
class CrewViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val crewRepository: CrewRepository,
    private val tierService: TierService,
) : ViewModel() {

    private val _state = MutableStateFlow(CrewUiState())
    val state: StateFlow<CrewUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(isLoading = it.crew.isEmpty(), error = null) }
        viewModelScope.launch {
            val tier = tierService.current()
            val userId = auth.currentUserId()
            val crew = if (userId != null && tier.atLeast(Tier.ELITE)) {
                crewRepository.getCrew(userId).getOrDefault(emptyList())
            } else {
                emptyList()
            }
            _state.update { it.copy(isLoading = false, tier = tier, crew = crew) }
        }
    }

    fun addCrew() = _state.update { it.copy(editing = CrewDraft()) }

    fun editCrew(member: CrewMember) = _state.update {
        it.copy(
            editing = CrewDraft(
                id = member.id,
                name = member.name,
                role = member.role.orEmpty(),
                hourlyCostRate = rateText(member.hourlyCostRate),
                active = member.active,
            ),
        )
    }

    fun updateDraft(transform: (CrewDraft) -> CrewDraft) = _state.update {
        it.copy(editing = it.editing?.let(transform))
    }

    fun closeEditor() = _state.update { it.copy(editing = null) }

    fun saveCrew() {
        val userId = auth.currentUserId() ?: return
        val draft = _state.value.editing ?: return
        if (draft.name.isBlank()) return // keep the editor open; name is required
        val input = CrewMemberInput(
            id = draft.id,
            name = draft.name.trim(),
            role = draft.role.trim().ifBlank { null },
            hourlyCostRate = draft.hourlyCostRate.trim().toDoubleOrNull() ?: 0.0,
            active = draft.active,
        )
        viewModelScope.launch {
            crewRepository.saveCrew(userId, input)
                .onSuccess { _state.update { it.copy(editing = null) }; load() }
                .onFailure { e -> _state.update { it.copy(error = e.message ?: "Couldn't save this crew member.") } }
        }
    }

    fun deleteCrew(crewId: String) {
        val userId = auth.currentUserId() ?: return
        viewModelScope.launch {
            crewRepository.deleteCrew(userId, crewId)
            _state.update { it.copy(editing = null) }
            load()
        }
    }

    companion object {
        /** "45" not "45.0"; "45.5" kept. */
        fun rateText(value: Double): String =
            if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
    }
}
