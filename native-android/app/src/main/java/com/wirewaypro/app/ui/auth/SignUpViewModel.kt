package com.wirewaypro.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.domain.model.SignUpOutcome
import com.wirewaypro.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignUpUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    /** Set once the account is created and an email confirmation is pending. */
    val confirmationRequired: Boolean = false,
) {
    val canSubmit: Boolean
        get() = fullName.isNotBlank() && email.isNotBlank() &&
            password.isNotBlank() && !isSubmitting
}

/**
 * Drives the sign-up form. Mirrors the web app's validation (full name required,
 * password ≥ 8 chars) and delegates account creation to [AuthRepository.signUp].
 * On a direct sign-in the app's session observer routes to the dashboard; when a
 * confirmation email is required we surface [SignUpUiState.confirmationRequired]
 * so the screen can tell the user to check their inbox.
 */
@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) =
        _uiState.update { it.copy(fullName = value, error = null) }

    fun onEmailChange(value: String) =
        _uiState.update { it.copy(email = value, error = null) }

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(password = value, error = null) }

    fun signUp() {
        val state = _uiState.value
        if (!state.canSubmit) return

        // Same guards the web app enforces before hitting Supabase.
        if (state.fullName.isBlank()) {
            _uiState.update { it.copy(error = "Please enter your full name.") }
            return
        }
        if (state.password.length < 8) {
            _uiState.update { it.copy(error = "Password must be at least 8 characters.") }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.signUp(state.email, state.password, state.fullName)
            result
                .onSuccess { outcome ->
                    when (outcome) {
                        // A live session exists — the session observer navigates away;
                        // no local state change needed (screen is torn down).
                        SignUpOutcome.SignedIn -> Unit
                        SignUpOutcome.ConfirmationRequired ->
                            _uiState.update {
                                it.copy(isSubmitting = false, confirmationRequired = true)
                            }
                    }
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(isSubmitting = false, error = t.message ?: "Sign-up failed.")
                    }
                }
        }
    }
}
