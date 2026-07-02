package com.wirewaypro.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.data.prefs.DEFAULT_HOURLY_RATE
import com.wirewaypro.app.data.prefs.SettingsPrefs
import com.wirewaypro.app.domain.model.ProfileInput
import com.wirewaypro.app.domain.model.UserProfile
import com.wirewaypro.app.domain.pricing.RateBand
import com.wirewaypro.app.domain.pricing.RegionalLaborRates
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
    val reviewLink: String = "",
    /** The contractor's client-financing application link; "" = financing not offered. */
    val financingLink: String = "",
    /** Proposal accent color as "#RRGGBB"; "" = default brand blue. */
    val brandColor: String = "",
    val rateSuggestion: RateBand? = null,
    val notificationsEnabled: Boolean = true,
    val logoUrl: String = "",
    val uploadingLogo: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
) {
    val hasLogo: Boolean get() = logoUrl.isNotBlank()
}

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
            val review = settingsPrefs.reviewLink.first()
            val financing = settingsPrefs.financingLink.first()
            val brand = settingsPrefs.brandColorHex.first()
            val profile = profileRepository.getProfile(userId).getOrNull()
            apply(profile, notify, hourly, flat, review, brand, financing)
            // Cache the region's typical rate so new quotes can default to it offline.
            cacheRegionalRate(RegionalLaborRates.forState(RegionalLaborRates.detectState(profile?.companyAddress)))
        }
    }

    /** Persist the region's typical billed rate for offline use by the quote builder. */
    private fun cacheRegionalRate(band: RateBand?) {
        viewModelScope.launch {
            settingsPrefs.setRegionalDefaultRate(band?.typical?.toDouble() ?: 0.0)
        }
    }

    private fun apply(p: UserProfile?, notify: Boolean, hourly: Double, flat: Double, review: String = "", brand: String = "", financing: String = "") = _state.update {
        it.copy(
            isLoading = false,
            brandColor = brand,
            financingLink = financing,
            fullName = p?.fullName.orEmpty(),
            companyName = p?.companyName.orEmpty(),
            companyPhone = p?.companyPhone.orEmpty(),
            companyEmail = p?.companyEmail.orEmpty(),
            companyLicense = p?.companyLicense.orEmpty(),
            companyAddress = p?.companyAddress.orEmpty(),
            companyWebsite = p?.companyWebsite.orEmpty(),
            hourlyRate = if (hourly > 0) trimNum(hourly) else "",
            flatRate = if (flat > 0) trimNum(flat) else "",
            reviewLink = review,
            notificationsEnabled = notify,
            logoUrl = p?.logoUrl.orEmpty(),
            rateSuggestion = RegionalLaborRates.forState(RegionalLaborRates.detectState(p?.companyAddress)),
        )
    }

    /** Uploads a picked business-logo image and stores its URL on the profile. */
    fun uploadLogo(bytes: ByteArray) {
        val userId = auth.currentUserId() ?: return
        _state.update { it.copy(uploadingLogo = true, error = null) }
        viewModelScope.launch {
            profileRepository.uploadLogo(userId, bytes)
                .onSuccess { url -> _state.update { it.copy(uploadingLogo = false, logoUrl = url) } }
                .onFailure { _state.update { it.copy(uploadingLogo = false, error = "Couldn't upload the logo. Try again.") } }
        }
    }

    /** "85.0" -> "85", "120.5" -> "120.5" for clean display in the rate fields. */
    private fun trimNum(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()

    fun setFullName(v: String) = _state.update { it.copy(fullName = v, error = null) }
    fun setCompanyName(v: String) = _state.update { it.copy(companyName = v) }
    fun setCompanyPhone(v: String) = _state.update { it.copy(companyPhone = v) }
    fun setCompanyEmail(v: String) = _state.update { it.copy(companyEmail = v) }
    fun setCompanyLicense(v: String) = _state.update { it.copy(companyLicense = v) }
    fun setCompanyAddress(v: String) {
        val band = RegionalLaborRates.forState(RegionalLaborRates.detectState(v))
        _state.update { it.copy(companyAddress = v, rateSuggestion = band) }
        cacheRegionalRate(band)
    }
    fun setCompanyWebsite(v: String) = _state.update { it.copy(companyWebsite = v) }
    fun setHourlyRate(v: String) = _state.update { it.copy(hourlyRate = v) }

    /** Applies the regional suggestion's typical rate to the hourly-rate field. */
    fun useSuggestedRate() = _state.update { s ->
        s.rateSuggestion?.let { s.copy(hourlyRate = it.typical.toString()) } ?: s
    }
    fun setFlatRate(v: String) = _state.update { it.copy(flatRate = v) }
    fun setReviewLink(v: String) = _state.update { it.copy(reviewLink = v) }
    fun setFinancingLink(v: String) = _state.update { it.copy(financingLink = v) }
    /** "" clears the custom accent (back to the default brand blue). */
    fun setBrandColor(hex: String) = _state.update { it.copy(brandColor = hex) }
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
        // A blank hourly rate falls back to the region's typical band (from the
        // address) instead of a flat national $85, so entering an address alone
        // yields a location-aware default rate.
        val rateFallback = s.rateSuggestion?.typical?.toDouble() ?: DEFAULT_HOURLY_RATE

        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            settingsPrefs.setNotificationsEnabled(s.notificationsEnabled)
            settingsPrefs.setDefaultHourlyRate(s.hourlyRate.toDoubleOrNull()?.takeIf { it > 0 } ?: rateFallback)
            settingsPrefs.setDefaultFlatRate(s.flatRate.toDoubleOrNull()?.takeIf { it > 0 } ?: 0.0)
            settingsPrefs.setReviewLink(s.reviewLink)
            settingsPrefs.setFinancingLink(s.financingLink)
            settingsPrefs.setBrandColor(s.brandColor)
            profileRepository.saveProfile(userId, input)
                .onSuccess { _state.update { it.copy(isSaving = false, saved = true) } }
                .onFailure { _state.update { it.copy(isSaving = false, error = "Couldn't save your profile. Try again.") } }
        }
    }
}
