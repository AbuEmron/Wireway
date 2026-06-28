package com.wirewaypro.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.domain.model.AuthState
import com.wirewaypro.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    /** The signed-in user's email, or null while resolving. */
    val email: StateFlow<String?> = auth.authState
        .map { (it as? AuthState.Authenticated)?.email }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Signs out; the app's session observer then routes back to login. */
    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }
}
