package com.wirewaypro.app.ui.quotes

import androidx.lifecycle.ViewModel
import com.wirewaypro.app.data.ai.TakeoffHandoff
import com.wirewaypro.app.domain.catalog.Assemblies
import com.wirewaypro.app.domain.catalog.Assembly
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Backs the job-template picker. Selecting a template seeds the same one-shot
 * [TakeoffHandoff] the AI takeoff uses, so a freshly-opened quote builder picks
 * up its line items — no AI, no network, just a fast pre-filled estimate.
 */
@HiltViewModel
class AssembliesViewModel @Inject constructor(
    private val handoff: TakeoffHandoff,
) : ViewModel() {

    val assemblies: List<Assembly> = Assemblies.all

    /** Hand the template's line items to the builder about to open. */
    fun seed(assembly: Assembly) {
        handoff.put(assembly.toCatalogEntries())
    }
}
