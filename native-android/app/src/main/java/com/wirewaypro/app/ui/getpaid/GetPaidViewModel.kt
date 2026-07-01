package com.wirewaypro.app.ui.getpaid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.data.stripe.StripeService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GetPaidUiState(
    val loading: Boolean = true,
    val connected: Boolean = false,
    val chargesEnabled: Boolean = false,
    val startingOnboarding: Boolean = false,
    val onboardingUrl: String? = null, // one-shot: the screen opens it in a Custom Tab
    val error: String? = null,
)

/**
 * Drives the "Get paid" (Stripe Connect) screen: reads the contractor's Connect
 * status and starts the Stripe-hosted onboarding flow. Status is re-checked when
 * the screen resumes (i.e. after returning from the onboarding browser tab).
 */
@HiltViewModel
class GetPaidViewModel @Inject constructor(
    private val stripe: StripeService,
) : ViewModel() {

    private val _state = MutableStateFlow(GetPaidUiState())
    val state: StateFlow<GetPaidUiState> = _state.asStateFlow()

    fun refreshStatus() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            stripe.connectStatus()
                .onSuccess { s ->
                    _state.update { it.copy(loading = false, connected = s.connected, chargesEnabled = s.chargesEnabled) }
                }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message ?: e.toString()) } }
        }
    }

    fun startOnboarding() {
        _state.update { it.copy(startingOnboarding = true, error = null) }
        viewModelScope.launch {
            stripe.createConnectOnboardingLink()
                .onSuccess { url -> _state.update { it.copy(startingOnboarding = false, onboardingUrl = url) } }
                .onFailure { e -> _state.update { it.copy(startingOnboarding = false, error = e.message ?: e.toString()) } }
        }
    }

    fun onboardingUrlConsumed() = _state.update { it.copy(onboardingUrl = null) }
}
