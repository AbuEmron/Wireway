package com.wirewaypro.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.data.prefs.SettingsPrefs
import com.wirewaypro.app.domain.model.AuthState
import com.wirewaypro.app.domain.repository.AuthRepository
import com.wirewaypro.app.ui.theme.ThemeMode
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
    private val settingsPrefs: SettingsPrefs,
) : ViewModel() {

    /** The signed-in user's email, or null while resolving. */
    val email: StateFlow<String?> = auth.authState
        .map { (it as? AuthState.Authenticated)?.email }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The current light/dark preference. */
    val themeMode: StateFlow<ThemeMode> = settingsPrefs.themeMode
        .map { ThemeMode.fromName(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsPrefs.setThemeMode(mode.name) }
    }

    /** Signs out; the app's session observer then routes back to login. */
    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }
}
