package com.wirewaypro.app.ui.nec

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.data.entitlements.TierService
import com.wirewaypro.app.domain.model.Tier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Resolves the tier for the NEC reference's Elite sections (commercial /
 * industrial / medical). `null` until known — no flash in either direction.
 */
@HiltViewModel
class NecReferenceViewModel @Inject constructor(
    private val tierService: TierService,
) : ViewModel() {

    private val _tier = MutableStateFlow<Tier?>(null)
    val tier: StateFlow<Tier?> = _tier.asStateFlow()

    init {
        viewModelScope.launch { _tier.value = tierService.current() }
    }
}
