package com.wirewaypro.app.domain.catalog

import com.wirewaypro.app.domain.model.QuoteCatalogEntry

/**
 * The job-walk composer (flagship layer 2, deterministic core): the electrician
 * walks the job adding areas/rooms — "3 bedrooms", "2 baths", "1 kitchen" — and
 * Wireway expands each area's template to REAL catalog line items, multiplies by
 * the area count, and merges everything into ONE estimate seed. Same catalog
 * data a single quote line uses; no AI anywhere in this path, every number
 * traceable to [Catalog].
 */
object JobWalk {

    data class Merged(
        val entries: List<QuoteCatalogEntry>,
        val customItems: List<AssemblyCustomItem>,
    )

    /**
     * One area on the walk: a template plus how many of it (3 bedrooms), and an
     * optional label the contractor typed ("Master bath"). [count] scales every
     * line the template carries.
     */
    data class WalkArea(
        val assembly: Assembly,
        val count: Int = 1,
        val name: String? = null,
    )

    /** One-per-job lines: never stacked when several areas each carry one. */
    private val SINGLETONS = setOf("permit_service", "permit_general", "inspection_final")

    /** Back-compat: a flat list of areas, each counted once. */
    fun merge(assemblies: List<Assembly>): Merged =
        mergeAreas(assemblies.map { WalkArea(it, 1) })

    /**
     * Merge every area into one estimate seed (deterministic, order-stable):
     *  - Each area's catalog lines are multiplied by its [WalkArea.count]
     *    (3 bedrooms → 3× every bedroom line) BEFORE merging.
     *  - The same catalog service appearing in two areas ADDS UP, keeping the
     *    first area's variant (kitchen cans + basement cans on one line).
     *  - Permits/inspections dedupe to ONE line — and a service permit covers
     *    general work, so `permit_general` drops when `permit_service` rides.
     *  - Commercial custom lines scale by count and concatenate (labeled per area).
     */
    fun mergeAreas(areas: List<WalkArea>): Merged {
        val scaled = areas.flatMap { area ->
            val n = area.count.coerceAtLeast(0)
            area.assembly.toCatalogEntries()
                .map { it.copy(qty = it.qty * n) }
                .filter { it.qty > 0.0 }
        }
        val hasServicePermit = scaled.any { it.serviceId == "permit_service" }
        val merged = LinkedHashMap<String, QuoteCatalogEntry>()
        scaled.forEach { e ->
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
        val customs = areas.flatMap { area ->
            val n = area.count.coerceAtLeast(0)
            area.assembly.customItems
                .map { it.copy(qty = it.qty * n) }
                .filter { it.qty > 0.0 }
        }
        return Merged(out, customs)
    }

    /**
     * Deterministic labor hours a merged catalog line represents — the same
     * hours × variant multiplier × qty the quote calculator bills, surfaced so
     * the job-walk review can show its work (NEC labor units, not a guess).
     */
    fun laborHoursFor(entry: QuoteCatalogEntry): Double {
        val svc = Catalog.service(entry.serviceId) ?: return 0.0
        val m = svc.variants.getOrNull(entry.variantIdx)?.m ?: 1.0
        return svc.laborHours * m * entry.qty
    }

    /** Total deterministic labor hours across a merge (transparent-math footer). */
    fun totalLaborHours(merged: Merged): Double =
        merged.entries.sumOf { laborHoursFor(it) } +
            merged.customItems.sumOf { it.laborHours * it.qty }
}
