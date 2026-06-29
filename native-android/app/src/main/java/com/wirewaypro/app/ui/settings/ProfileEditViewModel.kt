package com.wirewaypro.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.data.prefs.SettingsPrefs
import com.wirewaypro.app.domain.model.ProfileInput
import com.wirewaypro.app.domain.model.UserProfile
import com.wirewaypro.app.domain.repository.AuthRepository
import com.wirewaypro.app.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileEditUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val fullName: String = "",
    val companyName: String = "",
    val companyPhone: String = "",
    val companyEmail: String = "",
    val companyLicense: String = "",
    val companyAddress: String = "",
    val companyWebsite: String = "",
    val notificationsEnabled: Boolean = true,
    val error: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val settingsPrefs: SettingsPrefs,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileEditUiState())
    val state: StateFlow<ProfileEditUiState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        val userId = auth.currentUserId()
        if (userId == null) {
            _state.update { it.copy(isLoading = false, error = "Session expired.") }
            return
        }
        viewModelScope.launch {
            val notify = settingsPrefs.notificationsEnabled.first()
            val profile = profileRepository.getProfile(userId).getOrNull()
            apply(profile, notify)
        }
    }

    private fun apply(p: UserProfile?, notify: Boolean) = _state.update {
        it.copy(
            isLoading = false,
            fullName = p?.fullName.orEmpty(),
            companyName = p?.companyName.orEmpty(),
            companyPhone = p?.companyPhone.orEmpty(),
            companyEmail = p?.companyEmail.orEmpty(),
            companyLicense = p?.companyLicense.orEmpty(),
            companyAddress = p?.companyAddress.orEmpty(),
            companyWebsite = p?.companyWebsite.orEmpty(),
            notificationsEnabled = notify,
        )
    }

    fun setFullName(v: String) = _state.update { it.copy(fullName = v, error = null) }
    fun setCompanyName(v: String) = _state.update { it.copy(companyName = v) }
    fun setCompanyPhone(v: String) = _state.update { it.copy(companyPhone = v) }
    fun setCompanyEmail(v: String) = _state.update { it.copy(companyEmail = v) }
    fun setCompanyLicense(v: String) = _state.update { it.copy(companyLicense = v) }
    fun setCompanyAddress(v: String) = _state.update { it.copy(companyAddress = v) }
    fun setCompanyWebsite(v: String) = _state.update { it.copy(companyWebsite = v) }
    fun setNotifications(v: Boolean) = _state.update { it.copy(notificationsEnabled = v) }

    fun save() {
        val userId = auth.currentUserId()
        if (userId == null) {
            _state.update { it.copy(error = "Session expired.") }
            return
        }
        val s = _state.value
        val input = ProfileInput(
            fullName = s.fullName.ifBlank { null },
            companyName = s.companyName.ifBlank { null },
            companyPhone = s.companyPhone.ifBlank { null },
            companyEmail = s.companyEmail.ifBlank { null },
            companyLicense = s.companyLicense.ifBlank { null },
            companyAddress = s.companyAddress.ifBlank { null },
            companyWebsite = s.companyWebsite.ifBlank { null },
        )
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            settingsPrefs.setNotificationsEnabled(s.notificationsEnabled)
            profileRepository.saveProfile(userId, input)
                .onSuccess { _state.update { it.copy(isSaving = false, saved = true) } }
                .onFailure { _state.update { it.copy(isSaving = false, error = "Couldn't save your profile. Try again.") } }
        }
    }
}
