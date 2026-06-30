package com.wirewaypro.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.data.prefs.DEFAULT_HOURLY_RATE
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
    val hourlyRate: String = "",
    val flatRate: String = "",
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
            val hourly = settingsPrefs.defaultHourlyRate.first()
            val flat = settingsPrefs.defaultFlatRate.first()
            val profile = profileRepository.getProfile(userId).getOrNull()
            apply(profile, notify, hourly, flat)
        }
    }

    private fun apply(p: UserProfile?, notify: Boolean, hourly: Double, flat: Double) = _state.update {
        it.copy(
            isLoading = false,
            fullName = p?.fullName.orEmpty(),
            companyName = p?.companyName.orEmpty(),
            companyPhone = p?.companyPhone.orEmpty(),
            companyEmail = p?.companyEmail.orEmpty(),
            companyLicense = p?.companyLicense.orEmpty(),
            companyAddress = p?.companyAddress.orEmpty(),
            companyWebsite = p?.companyWebsite.orEmpty(),
            hourlyRate = if (hourly > 0) trimNum(hourly) else "",
            flatRate = if (flat > 0) trimNum(flat) else "",
            notificationsEnabled = notify,
        )
    }

    /** "85.0" -> "85", "120.5" -> "120.5" for clean display in the rate fields. */
    private fun trimNum(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()

    fun setFullName(v: String) = _state.update { it.copy(fullName = v, error = null) }
    fun setCompanyName(v: String) = _state.update { it.copy(companyName = v) }
    fun setCompanyPhone(v: String) = _state.update { it.copy(companyPhone = v) }
    fun setCompanyEmail(v: String) = _state.update { it.copy(companyEmail = v) }
    fun setCompanyLicense(v: String) = _state.update { it.copy(companyLicense = v) }
    fun setCompanyAddress(v: String) = _state.update { it.copy(companyAddress = v) }
    fun setCompanyWebsite(v: String) = _state.update { it.copy(companyWebsite = v) }
    fun setHourlyRate(v: String) = _state.update { it.copy(hourlyRate = v) }
    fun setFlatRate(v: String) = _state.update { it.copy(flatRate = v) }
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
            settingsPrefs.setDefaultHourlyRate(s.hourlyRate.toDoubleOrNull()?.takeIf { it > 0 } ?: DEFAULT_HOURLY_RATE)
            settingsPrefs.setDefaultFlatRate(s.flatRate.toDoubleOrNull()?.takeIf { it > 0 } ?: 0.0)
            profileRepository.saveProfile(userId, input)
                .onSuccess { _state.update { it.copy(isSaving = false, saved = true) } }
                .onFailure { _state.update { it.copy(isSaving = false, error = "Couldn't save your profile. Try again.") } }
        }
    }
}
