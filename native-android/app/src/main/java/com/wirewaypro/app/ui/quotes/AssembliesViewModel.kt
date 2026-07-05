package com.wirewaypro.app.ui.quotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.data.ai.TakeoffHandoff
import com.wirewaypro.app.data.entitlements.TierService
import com.wirewaypro.app.domain.catalog.Assemblies
import com.wirewaypro.app.domain.catalog.Assembly
import com.wirewaypro.app.domain.catalog.AssemblyItem
import com.wirewaypro.app.domain.catalog.AssemblySector
import com.wirewaypro.app.domain.catalog.JobWalk
import com.wirewaypro.app.domain.model.Tier
import com.wirewaypro.app.domain.model.UserTemplate
import com.wirewaypro.app.domain.repository.AuthRepository
import com.wirewaypro.app.domain.repository.UserTemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Backs the job-template picker and the area-based job walk. Selecting a
 * template (or a whole walk of areas) seeds the same one-shot [TakeoffHandoff]
 * the AI takeoff uses, so a freshly-opened quote builder picks up the exact
 * catalog line items — no AI, no network, just a fast pre-filled estimate.
 *
 * The library is built-ins ([Assemblies]) plus the contractor's own templates
 * (Room + Supabase). Browsing is free; STARTING an estimate is the gated moment
 * — residential templates are Pro, commercial/industrial are Elite.
 */
@HiltViewModel
class AssembliesViewModel @Inject constructor(
    private val handoff: TakeoffHandoff,
    private val tierService: TierService,
    private val userTemplates: UserTemplateRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    private val userId: String? = auth.currentUserId()

    /** null until resolved — the detail sheet shows a loading CTA meanwhile. */
    private val _tier = MutableStateFlow<Tier?>(null)
    val tier: StateFlow<Tier?> = _tier.asStateFlow()

    private val _mine = MutableStateFlow<List<UserTemplate>>(emptyList())
    val mine: StateFlow<List<UserTemplate>> = _mine.asStateFlow()

    /** Built-ins + the contractor's own templates, combined and live. */
    private val _library = MutableStateFlow(Assemblies.all)
    val library: StateFlow<List<Assembly>> = _library.asStateFlow()

    init {
        viewModelScope.launch { _tier.value = tierService.current() }
        userId?.let { uid ->
            viewModelScope.launch { runCatching { userTemplates.refresh(uid) } }
            viewModelScope.launch {
                userTemplates.observe(uid).collect { list ->
                    _mine.value = list
                    _library.value = list.map { it.toAssembly() } + Assemblies.all
                }
            }
        }
    }

    private fun resolve(assemblyId: String): Assembly? =
        _library.value.firstOrNull { it.id == assemblyId } ?: Assemblies.byId(assemblyId)

    /** The tier a template needs to start an estimate from it (user templates = Pro). */
    fun requiredTier(assembly: Assembly): Tier =
        if (assembly.sector == AssemblySector.COMMERCIAL_INDUSTRIAL) Tier.ELITE else Tier.PRO

    fun isMine(assembly: Assembly): Boolean = UserTemplate.isUserAssemblyId(assembly.id)

    /** Hand a single template's line items to the builder about to open. */
    fun seed(assembly: Assembly) {
        handoff.put(assembly.toCatalogEntries())
        if (assembly.customItems.isNotEmpty()) handoff.putCustom(assembly.customItems)
    }

    // ── Job walk: areas with counts, merged into ONE estimate ──────────────────

    /** One area on the walk: a template + how many of it (3 bedrooms). */
    data class WalkPick(val assemblyId: String, val count: Int)

    private val _walk = MutableStateFlow<List<WalkPick>>(emptyList())
    val walk: StateFlow<List<WalkPick>> = _walk.asStateFlow()

    fun inWalk(assemblyId: String): Boolean = _walk.value.any { it.assemblyId == assemblyId }

    /** Add an area (count 1) or bump its count if already on the walk. */
    fun addToWalk(assembly: Assembly) {
        _walk.value = _walk.value.toMutableList().also { list ->
            val i = list.indexOfFirst { it.assemblyId == assembly.id }
            if (i >= 0) list[i] = list[i].copy(count = list[i].count + 1) else list.add(WalkPick(assembly.id, 1))
        }
    }

    fun setCount(assemblyId: String, count: Int) {
        _walk.value = if (count <= 0) {
            _walk.value.filterNot { it.assemblyId == assemblyId }
        } else {
            _walk.value.map { if (it.assemblyId == assemblyId) it.copy(count = count) else it }
        }
    }

    fun removeFromWalk(assemblyId: String) {
        _walk.value = _walk.value.filterNot { it.assemblyId == assemblyId }
    }

    fun clearWalk() { _walk.value = emptyList() }

    /** Resolve the current walk to (assembly, count) areas, dropping any stale ids. */
    fun walkAreas(): List<JobWalk.WalkArea> =
        _walk.value.mapNotNull { pick -> resolve(pick.assemblyId)?.let { JobWalk.WalkArea(it, pick.count) } }

    fun requiredWalkTier(): Tier =
        if (walkAreas().any { it.assembly.sector == AssemblySector.COMMERCIAL_INDUSTRIAL }) Tier.ELITE else Tier.PRO

    /** The deterministic expansion of the whole walk — drives the transparent math. */
    fun mergedWalk(): JobWalk.Merged = JobWalk.mergeAreas(walkAreas())

    /** Merge every area into one estimate seed and hand it to the builder. */
    fun seedWalk() {
        val merged = mergedWalk()
        handoff.put(merged.entries)
        if (merged.customItems.isNotEmpty()) handoff.putCustom(merged.customItems)
        clearWalk()
    }

    // ── User templates: create / edit / delete ─────────────────────────────────

    /** Save (create or update) a contractor template from catalog line items. */
    fun saveTemplate(existingId: String?, name: String, description: String, items: List<AssemblyItem>, onDone: () -> Unit) {
        val uid = userId ?: return
        val template = UserTemplate(
            id = existingId ?: UUID.randomUUID().toString(),
            name = name.trim(),
            description = description.trim(),
            category = "My templates",
            items = items,
        )
        viewModelScope.launch {
            runCatching { userTemplates.save(uid, template) }
            onDone()
        }
    }

    fun deleteTemplate(assemblyId: String) {
        val uid = userId ?: return
        val id = UserTemplate.idFromAssemblyId(assemblyId)
        removeFromWalk(assemblyId)
        viewModelScope.launch { runCatching { userTemplates.delete(uid, id) } }
    }

    /** The domain template behind a "user:" assembly id, for the editor. */
    fun userTemplateFor(assemblyId: String): UserTemplate? {
        val id = UserTemplate.idFromAssemblyId(assemblyId)
        return _mine.value.firstOrNull { it.id == id }
    }
}
