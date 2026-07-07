package com.wirewaypro.app.domain.model

import com.wirewaypro.app.domain.catalog.Assembly
import com.wirewaypro.app.domain.catalog.AssemblyItem
import com.wirewaypro.app.domain.catalog.AssemblySector

/**
 * A contractor-authored job template. It carries the SAME [AssemblyItem] lines
 * the built-in [com.wirewaypro.app.domain.catalog.Assemblies] library uses —
 * real [com.wirewaypro.app.domain.catalog.Catalog] service ids with quantities
 * and variants — so a user template expands through the exact deterministic path
 * (no fabricated prices, no AI). It maps to an [Assembly] to flow through the
 * job-walk composer and the quote builder unchanged.
 */
data class UserTemplate(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val items: List<AssemblyItem>,
    val syncState: SyncState = SyncState.SYNCED,
) {
    /** Bridge into the built-in template machinery. */
    fun toAssembly(): Assembly = Assembly(
        id = ASSEMBLY_ID_PREFIX + id,
        label = name,
        description = description,
        items = items,
        customItems = emptyList(),
        category = category,
        sector = AssemblySector.RESIDENTIAL,
    )

    val lineCount: Int get() = items.size

    companion object {
        /** Namespacing so a user template id never collides with a built-in one. */
        const val ASSEMBLY_ID_PREFIX = "user:"

        fun isUserAssemblyId(assemblyId: String): Boolean = assemblyId.startsWith(ASSEMBLY_ID_PREFIX)

        fun idFromAssemblyId(assemblyId: String): String = assemblyId.removePrefix(ASSEMBLY_ID_PREFIX)
    }
}
