package com.wirewaypro.app.domain.catalog

import com.wirewaypro.app.domain.model.QuoteCatalogEntry

/**
 * The job-walk composer (flagship layer 1, deterministic core): the electrician
 * walks the job picking an area template per room/system, and Wireway merges
 * them into ONE estimate seed — materials + labor from the same template data a
 * single pick uses. No AI anywhere in this path.
 */
object JobWalk {

    data class Merged(
        val entries: List<QuoteCatalogEntry>,
        val customItems: List<AssemblyCustomItem>,
    )

    /** One-per-job lines: never stacked when several areas each carry one. */
    private val SINGLETONS = setOf("permit_service", "permit_general", "inspection_final")

    /**
     * Merge rules (deterministic, order-stable):
     *  - The same catalog service picked in two areas ADDS UP (kitchen's cans +
     *    basement's cans), keeping the first pick's variant.
     *  - Permits/inspections dedupe to ONE line — and a service permit covers
     *    general work, so `permit_general` drops when `permit_service` rides.
     *  - Commercial custom lines concatenate (they're labeled per area).
     */
    fun merge(assemblies: List<Assembly>): Merged {
        val entries = assemblies.flatMap { it.toCatalogEntries() }
        val hasServicePermit = entries.any { it.serviceId == "permit_service" }
        val merged = LinkedHashMap<String, QuoteCatalogEntry>()
        entries.forEach { e ->
            if (e.serviceId == "permit_general" && hasServicePermit) return@forEach
            val prev = merged[e.serviceId]
            merged[e.serviceId] = when {
                prev == null -> e
                e.serviceId in SINGLETONS -> prev
                else -> prev.copy(qty = prev.qty + e.qty)
            }
        }
        val out = merged.values.map { entry ->
            if (entry.serviceId in SINGLETONS && entry.qty > 1.0) entry.copy(qty = 1.0) else entry
        }
        return Merged(out, assemblies.flatMap { it.customItems })
    }
}
