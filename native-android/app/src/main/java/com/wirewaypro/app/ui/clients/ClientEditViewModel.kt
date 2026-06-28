package com.wirewaypro.app.ui.clients

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.domain.model.ClientInput
import com.wirewaypro.app.domain.repository.AuthRepository
import com.wirewaypro.app.domain.repository.ClientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClientEditUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEdit: Boolean = false,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val error: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class ClientEditViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val clientRepository: ClientRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val clientId: String? = savedStateHandle.get<String>(ARG_ID)?.takeIf { it.isNotBlank() }

    private val _state = MutableStateFlow(ClientEditUiState(isEdit = clientId != null, isLoading = clientId != null))
    val state: StateFlow<ClientEditUiState> = _state.asStateFlow()

    init {
        // Clients have no single-row getter; load from the list and find by id.
        if (clientId != null) {
            viewModelScope.launch {
                val userId = auth.currentUserId()
                if (userId == null) {
                    _state.update { it.copy(isLoading = false, error = "Session expired.") }
                    return@launch
                }
                clientRepository.getClients(userId)
                    .onSuccess { list ->
                        val c = list.find { it.id == clientId }
                        if (c == null) {
                            _state.update { it.copy(isLoading = false, error = "Client not found.") }
                        } else {
                            _state.update {
                                it.copy(isLoading = false, name = c.name, email = c.email.orEmpty(), phone = c.phone.orEmpty())
                            }
                        }
                    }
                    .onFailure { _state.update { it.copy(isLoading = false, error = "Couldn't load client.") } }
            }
        }
    }

    fun setName(v: String) = _state.update { it.copy(name = v, error = null) }
    fun setEmail(v: String) = _state.update { it.copy(email = v) }
    fun setPhone(v: String) = _state.update { it.copy(phone = v) }

    fun save() {
        val userId = auth.currentUserId()
        if (userId == null) {
            _state.update { it.copy(error = "Session expired.") }
            return
        }
        val s = _state.value
        if (s.name.isBlank()) {
            _state.update { it.copy(error = "A client name is required.") }
            return
        }
        val input = ClientInput(
            id = clientId,
            name = s.name.trim(),
            email = s.email.ifBlank { null },
            phone = s.phone.ifBlank { null },
        )
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            clientRepository.saveClient(userId, input)
                .onSuccess { _state.update { it.copy(isSaving = false, saved = true) } }
                .onFailure { _state.update { it.copy(isSaving = false, error = "Couldn't save. Try again.") } }
        }
    }

    fun delete() {
        val userId = auth.currentUserId() ?: return
        val id = clientId ?: return
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            clientRepository.deleteClient(userId, id)
                .onSuccess { _state.update { it.copy(isSaving = false, saved = true) } }
                .onFailure { _state.update { it.copy(isSaving = false, error = "Couldn't delete. Try again.") } }
        }
    }

    companion object {
        const val ARG_ID = "id"
    }
}
