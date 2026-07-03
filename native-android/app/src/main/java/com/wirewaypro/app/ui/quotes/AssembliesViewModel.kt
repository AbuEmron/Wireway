package com.wirewaypro.app.ui.quotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.data.ai.TakeoffHandoff
import com.wirewaypro.app.data.entitlements.TierService
import com.wirewaypro.app.domain.catalog.Assemblies
import com.wirewaypro.app.domain.catalog.Assembly
import com.wirewaypro.app.domain.catalog.AssemblySector
import com.wirewaypro.app.domain.catalog.JobWalk
import com.wirewaypro.app.domain.model.Tier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the job-template picker. Selecting a template seeds the same one-shot
 * [TakeoffHandoff] the AI takeoff uses, so a freshly-opened quote builder picks
 * up its line items — no AI, no network, just a fast pre-filled estimate.
 *
 * Everyone can browse the whole library (it sells the upgrade better than a
 * wall); STARTING an estimate is the gated moment — residential templates are
 * Pro, commercial/industrial are Elite.
 */
@HiltViewModel
class AssembliesViewModel @Inject constructor(
    private val handoff: TakeoffHandoff,
    private val tierService: TierService,
) : ViewModel() {

    val assemblies: List<Assembly> = Assemblies.all

    /** null until resolved — the detail sheet shows a loading CTA meanwhile. */
    private val _tier = MutableStateFlow<Tier?>(null)
    val tier: StateFlow<Tier?> = _tier.asStateFlow()

    init {
        viewModelScope.launch { _tier.value = tierService.current() }
    }

    /** The tier a template needs to start an estimate from it. */
    fun requiredTier(assembly: Assembly): Tier =
        if (assembly.sector == AssemblySector.COMMERCIAL_INDUSTRIAL) Tier.ELITE else Tier.PRO

    /** Hand the template's line items to the builder about to open. */
    fun seed(assembly: Assembly) {
        handoff.put(assembly.toCatalogEntries())
        if (assembly.customItems.isNotEmpty()) handoff.putCustom(assembly.customItems)
    }

    // ── Job walk: pick an area template per room/system, build ONE estimate ────

    /** Selected template ids, in pick order (insertion-ordered set). */
    private val _walk = MutableStateFlow<Set<String>>(emptySet())
    val walk: StateFlow<Set<String>> = _walk.asStateFlow()

    fun toggleWalk(assembly: Assembly) {
        _walk.value = _walk.value.let { if (assembly.id in it) it - assembly.id else it + assembly.id }
    }

    fun clearWalk() {
        _walk.value = emptySet()
    }

    private fun walkAssemblies(): List<Assembly> = _walk.value.mapNotNull(Assemblies::byId)

    /** The walk needs the highest tier any picked area needs. */
    fun requiredWalkTier(): Tier =
        if (walkAssemblies().any { it.sector == AssemblySector.COMMERCIAL_INDUSTRIAL }) Tier.ELITE else Tier.PRO

    /** Merge every picked area into one estimate seed (JobWalk rules) and hand it off. */
    fun seedWalk() {
        val merged = JobWalk.merge(walkAssemblies())
        handoff.put(merged.entries)
        if (merged.customItems.isNotEmpty()) handoff.putCustom(merged.customItems)
        clearWalk()
    }
}
