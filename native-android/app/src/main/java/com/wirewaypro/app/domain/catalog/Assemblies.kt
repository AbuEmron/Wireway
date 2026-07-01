package com.wirewaypro.app.domain.catalog

import com.wirewaypro.app.domain.model.QuoteCatalogEntry

/**
 * A reusable job template — a named bundle of catalog line items for a common
 * job (200A service upgrade, EV charger, can-light package, …). It's the fast
 * path to an estimate WITHOUT AI: pick a template, land in the quote builder
 * pre-filled with sensible line items, then tweak quantities and price.
 *
 * Every [AssemblyItem.serviceId] must reference a real [Catalog] service and a
 * valid variant index; [Assemblies.validate] guards that in tests/dev.
 */
data class AssemblyItem(
    val serviceId: String,
    val qty: Double = 1.0,
    val variantIdx: Int = 0,
)

data class Assembly(
    val id: String,
    val label: String,
    val description: String,
    val items: List<AssemblyItem>,
) {
    /** Seed for the quote builder (via the same handoff the AI takeoff uses). */
    fun toCatalogEntries(): List<QuoteCatalogEntry> =
        items.map { QuoteCatalogEntry(serviceId = it.serviceId, qty = it.qty, variantIdx = it.variantIdx) }

    /** Total catalog line count (for the picker subtitle). */
    val itemCount: Int get() = items.size
}

private fun a(id: String, label: String, description: String, vararg items: AssemblyItem) =
    Assembly(id, label, description, items.toList())

private fun i(serviceId: String, qty: Double = 1.0, variantIdx: Int = 0) =
    AssemblyItem(serviceId, qty, variantIdx)

/**
 * Curated starter library of electrical job templates. Line items and variants
 * reference [Catalog] ids verbatim so totals compute exactly like a hand-built
 * quote. Quantities are typical starting points the contractor edits per job.
 */
object Assemblies {

    val all: List<Assembly> = listOf(
        a(
            "service_upgrade_200",
            "200A Service Upgrade",
            "Panel, meter socket, grounding, surge, and bonding — the full service swap.",
            i("panel_200"),
            i("meter_socket", variantIdx = 1),      // 200A
            i("grounding"),
            i("surge_whole", variantIdx = 1),        // Type 2
            i("bonding", variantIdx = 2),            // Water + Gas
        ),
        a(
            "panel_swap_100",
            "100A Panel Swap",
            "Straightforward panel replacement with grounding and whole-home surge.",
            i("panel_100"),
            i("grounding"),
            i("surge_whole", variantIdx = 1),
        ),
        a(
            "ev_charger_l2",
            "EV Charger (Level 2)",
            "Dedicated 50A circuit and a NEMA 14-50 outlet for a Level 2 charger.",
            i("circuit_50", variantIdx = 1),         // EV Charger
            i("outlet_240", variantIdx = 2),         // NEMA 14-50
        ),
        a(
            "can_lights_6",
            "Recessed Can-Lights (6)",
            "Six recessed cans on a new circuit with a dimmer.",
            i("light_recessed", qty = 6.0),
            i("dimmer_single"),
            i("circuit_15"),
        ),
        a(
            "kitchen_remodel",
            "Kitchen Remodel",
            "Small-appliance circuits, GFCI counters, recessed + under-cabinet lighting.",
            i("circuit_20", qty = 2.0, variantIdx = 1),   // Kitchen/Bath
            i("circuit_dedicated", qty = 2.0),            // dishwasher + disposal
            i("outlet_gfci", qty = 4.0, variantIdx = 1),  // new 15A
            i("light_recessed", qty = 6.0),
            i("light_undercab"),
        ),
        a(
            "bathroom_remodel",
            "Bathroom Remodel",
            "GFCI circuit, exhaust fan, vanity light, and recessed cans.",
            i("circuit_20", variantIdx = 1),
            i("outlet_gfci", variantIdx = 1),
            i("exhaust_fan"),
            i("light_vanity"),
            i("light_recessed", qty = 2.0),
        ),
        a(
            "whole_home_safety",
            "Whole-Home Safety / Code",
            "Hardwired smoke+CO, surge protection, and GFCI/AFCI retrofits.",
            i("smoke_co_combo", qty = 4.0),
            i("surge_whole", variantIdx = 1),
            i("gfci_protection", qty = 2.0),
            i("arc_fault", qty = 2.0),
        ),
        a(
            "hot_tub_spa",
            "Hot Tub / Spa",
            "Spa circuit with the required exterior disconnect.",
            i("hot_tub"),
            i("ac_disconnect"),
        ),
        a(
            "generator_backup",
            "Generator Backup",
            "Transfer switch and inlet box for a portable generator.",
            i("generator_switch"),
            i("generator_inlet"),
        ),
    )

    fun byId(id: String): Assembly? = all.firstOrNull { it.id == id }

    /**
     * True if every item references a real catalog service and an in-range
     * variant. Guards the curated list against catalog drift (used in tests).
     */
    fun validate(): Boolean = all.all { assembly ->
        assembly.items.all { item ->
            val svc = Catalog.service(item.serviceId)
            svc != null && item.variantIdx in svc.variants.indices
        }
    }
}
