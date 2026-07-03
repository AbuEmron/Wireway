package com.wirewaypro.app.domain.catalog

import com.wirewaypro.app.domain.model.QuoteCatalogEntry

/**
 * A reusable job template — a named bundle of line items for a common job
 * (200A service upgrade, EV charger, motor circuit, …). It's the fast path to
 * an estimate WITHOUT AI: pick a template, land in the quote builder pre-filled
 * with a near-complete job, then tweak quantities and price.
 *
 * Two kinds of lines:
 *  - [AssemblyItem]: a residential [Catalog] service (deterministic material +
 *    labor defaults, exactly like a hand-built quote line).
 *  - [AssemblyCustomItem]: a commercial/industrial line sourced from
 *    [EliteCatalog]. Typical labor hours are provided as editable defaults;
 *    labor dollars are priced at the CONTRACTOR'S OWN rate when the builder
 *    seeds, and material stays at 0 for the supplier's quote — this gear is
 *    market-priced, so a hardcoded material number would be a fabrication.
 *
 * [Assemblies.validate] guards every reference against catalog drift in tests.
 */
data class AssemblyItem(
    val serviceId: String,
    val qty: Double = 1.0,
    val variantIdx: Int = 0,
)

data class AssemblyCustomItem(
    val label: String,
    val qty: Double = 1.0,
    /** Typical labor hours PER UNIT — an editable starting point, not a bid. */
    val laborHours: Double,
    /** [EliteCatalog] id this line sources from (spec/unit provenance). */
    val eliteMaterialId: String? = null,
)

enum class AssemblySector(val label: String) {
    RESIDENTIAL("Residential & Service"),
    COMMERCIAL_INDUSTRIAL("Commercial & Industrial"),
}

data class Assembly(
    val id: String,
    val label: String,
    val description: String,
    val items: List<AssemblyItem> = emptyList(),
    val customItems: List<AssemblyCustomItem> = emptyList(),
    val category: String = "General",
    val sector: AssemblySector = AssemblySector.RESIDENTIAL,
) {
    /** Seed for the quote builder (via the same handoff the AI takeoff uses). */
    fun toCatalogEntries(): List<QuoteCatalogEntry> =
        items.map { QuoteCatalogEntry(serviceId = it.serviceId, qty = it.qty, variantIdx = it.variantIdx) }

    /** Total line count (for the picker subtitle). */
    val itemCount: Int get() = items.size + customItems.size

    /** Lowercase haystack for search. */
    val search: String = (label + " " + description + " " + category).lowercase()
}

private fun a(
    id: String, label: String, description: String,
    category: String, vararg items: AssemblyItem,
) = Assembly(id, label, description, items.toList(), emptyList(), category)

private fun i(serviceId: String, qty: Double = 1.0, variantIdx: Int = 0) =
    AssemblyItem(serviceId, qty, variantIdx)

private fun c(
    id: String, label: String, description: String,
    category: String, vararg items: AssemblyCustomItem,
) = Assembly(id, label, description, emptyList(), items.toList(), category, AssemblySector.COMMERCIAL_INDUSTRIAL)

private fun ci(label: String, qty: Double = 1.0, hours: Double, source: String? = null) =
    AssemblyCustomItem(label, qty, hours, source)

/**
 * Curated library of electrical job templates. Residential lines reference
 * [Catalog] ids verbatim so totals compute exactly like a hand-built quote;
 * commercial/industrial lines reference [EliteCatalog] for provenance.
 * Quantities are typical starting points the contractor edits per job.
 */
object Assemblies {

    // ── Residential & service (PRO) ─────────────────────────────────────────────
    private val residential: List<Assembly> = listOf(
        // Service & panel
        a(
            "service_upgrade_200", "200A Service Upgrade",
            "Panel, meter socket, grounding, surge, bonding, permit and directory — the full service swap.",
            "Service & Panel",
            i("panel_200"),
            i("meter_socket", variantIdx = 1),      // 200A
            i("grounding"),
            i("surge_whole", variantIdx = 1),        // Type 2
            i("bonding", variantIdx = 2),            // Water + Gas
            i("panel_labeling", variantIdx = 1),     // full directory
            i("permit_service"),
        ),
        a(
            "panel_swap_100", "100A Panel Swap",
            "Straightforward panel replacement with grounding, surge, directory and permit.",
            "Service & Panel",
            i("panel_100"),
            i("grounding"),
            i("surge_whole", variantIdx = 1),
            i("panel_labeling", variantIdx = 1),
            i("permit_service"),
        ),
        a(
            "sub_panel_add", "Sub-Panel Add (100A)",
            "100A sub-panel for a garage, addition or shop — panel, feeder breaker and permit.",
            "Service & Panel",
            i("sub_panel", variantIdx = 1),          // 100A
            i("breaker_double", variantIdx = 3),     // 60A feeder breaker (edit to feeder size)
            i("permit_general"),
        ),
        a(
            "meter_service_repair", "Meter & Service Device Repair",
            "Meter socket replacement with the exterior emergency disconnect brought to current code.",
            "Service & Panel",
            i("meter_socket", variantIdx = 1),
            i("exterior_disconnect", variantIdx = 1),
            i("permit_service"),
        ),

        // EV & power
        a(
            "ev_charger_l2", "EV Charger (Level 2)",
            "Dedicated 50A circuit, NEMA 14-50 outlet and permit for a Level 2 charger.",
            "EV & Power",
            i("circuit_50", variantIdx = 1),         // EV Charger
            i("outlet_240", variantIdx = 2),         // NEMA 14-50
            i("permit_general"),
        ),
        a(
            "ev_load_managed", "EV Charger + Load Management",
            "Smart EVSE with an energy-management system and load calc — EV charging without a service upgrade.",
            "EV & Power",
            i("outlet_ev", variantIdx = 2),          // Smart EVSE
            i("energy_mgmt"),
            i("load_calc"),
            i("permit_general"),
        ),
        a(
            "standby_generator", "Standby Generator Install",
            "Air-cooled standby generator with automatic transfer, dedicated run, grounding and permit.",
            "EV & Power",
            i("generator_install"),
            i("generator_transfer2", variantIdx = 3), // Automatic (ATS)
            i("generator_circuit"),
            i("generator_grounding", variantIdx = 1), // separately derived
            i("permit_service"),
        ),
        a(
            "generator_backup", "Portable Generator Backup",
            "Manual transfer switch and inlet box for a portable generator, permitted.",
            "EV & Power",
            i("generator_switch"),
            i("generator_inlet"),
            i("permit_general"),
        ),
        a(
            "battery_storage_install", "Battery Storage System",
            "Wall-mounted battery storage with inverter/disconnect and permit.",
            "EV & Power",
            i("battery_storage", variantIdx = 2),    // with gateway/inverter
            i("solar_inverter"),
            i("permit_service"),
        ),

        // Lighting
        a(
            "can_lights_6", "Recessed Can-Lights (6)",
            "Six recessed cans on a new circuit with a dimmer.",
            "Lighting",
            i("light_recessed", qty = 6.0),
            i("dimmer_single"),
            i("circuit_15"),
        ),
        a(
            "can_lights_12", "Recessed Can-Lights (12) — Great Room",
            "Twelve cans on a new circuit, 3-way dimmed from two locations.",
            "Lighting",
            i("light_recessed", qty = 12.0),
            i("dimmer_3way"),
            i("switch_3way"),
            i("circuit_15"),
        ),
        a(
            "ceiling_fan_add", "Ceiling Fan (New Location)",
            "Fan-rated box, fan and its own switch at a new location.",
            "Lighting",
            i("light_fan", variantIdx = 1),          // new fan-rated box + fan
            i("switch_single", variantIdx = 3),      // new location
        ),
        a(
            "exterior_lighting", "Exterior / Security Lighting",
            "Motion floods and porch lights with a timer switch.",
            "Lighting",
            i("flood_light", qty = 2.0, variantIdx = 1), // new motion flood
            i("light_exterior", qty = 2.0, variantIdx = 1),
            i("timer_switch", variantIdx = 2),
        ),

        // Remodel
        a(
            "kitchen_remodel", "Kitchen Remodel",
            "Small-appliance circuits, GFCI counters, recessed + under-cabinet lighting, permitted.",
            "Remodel",
            i("circuit_20", qty = 2.0, variantIdx = 1),   // Kitchen/Bath
            i("circuit_dedicated", qty = 2.0),            // dishwasher + disposal
            i("outlet_gfci", qty = 4.0, variantIdx = 1),  // new 15A
            i("light_recessed", qty = 6.0),
            i("light_undercab"),
            i("permit_general"),
        ),
        a(
            "bathroom_remodel", "Bathroom Remodel",
            "GFCI circuit, exhaust fan, vanity light and recessed cans, permitted.",
            "Remodel",
            i("circuit_20", variantIdx = 1),
            i("outlet_gfci", variantIdx = 1),
            i("exhaust_fan"),
            i("light_vanity"),
            i("light_recessed", qty = 2.0),
            i("permit_general"),
        ),
        a(
            "basement_finish", "Basement Finish",
            "General + small-appliance circuits, receptacles, cans, smoke/CO and code lighting.",
            "Remodel",
            i("circuit_15", qty = 2.0),
            i("circuit_20"),
            i("outlet_standard", qty = 8.0, variantIdx = 1), // new same-wall
            i("light_recessed", qty = 6.0),
            i("smoke_co_combo", variantIdx = 1),
            i("lighting_basement"),
            i("permit_general"),
        ),
        a(
            "laundry_room", "Laundry Room Package",
            "Dedicated laundry circuit, dryer hookup and a GFCI receptacle.",
            "Remodel",
            i("laundry_circuit"),
            i("dryer_hookup", variantIdx = 1),       // 4-wire
            i("outlet_gfci", variantIdx = 1),
        ),

        // HVAC & appliance
        a(
            "ac_condenser_circuit", "A/C Condenser Circuit",
            "Dedicated HVAC circuit, in-sight disconnect and the required service receptacle.",
            "HVAC & Appliance",
            i("hvac_circuit", variantIdx = 1),       // central AC
            i("ac_disconnect"),
            i("hvac_outlet_req"),
            i("permit_general"),
        ),
        a(
            "water_heater_circuit", "Electric Water Heater Circuit",
            "Dedicated water-heater circuit, permitted.",
            "HVAC & Appliance",
            i("water_heater"),
            i("permit_general"),
        ),

        // Safety & code
        a(
            "whole_home_safety", "Whole-Home Safety / Code",
            "Hardwired smoke+CO, surge protection, and GFCI/AFCI retrofits.",
            "Safety & Code",
            i("smoke_co_combo", qty = 4.0),
            i("surge_whole", variantIdx = 1),
            i("gfci_protection", qty = 2.0),
            i("arc_fault", qty = 2.0),
        ),
        a(
            "smoke_co_package", "Smoke / CO Detector Package",
            "Five interconnected hardwired smoke+CO combos — bedrooms, halls, each level.",
            "Safety & Code",
            i("smoke_co_combo", qty = 5.0, variantIdx = 1), // new interconnected
        ),
        a(
            "afci_upgrade", "Whole-House AFCI Upgrade",
            "AFCI protection across the panel with a fresh circuit directory.",
            "Safety & Code",
            i("whole_house_afci"),
            i("panel_labeling", variantIdx = 1),
        ),
        a(
            "surge_protection", "Whole-Home Surge Protection",
            "Type 2 surge protective device at the panel.",
            "Safety & Code",
            i("surge_whole", variantIdx = 1),
        ),

        // Rewire & legacy
        a(
            "whole_house_rewire", "Whole-House Rewire",
            "Room-by-room rewire with a 200A panel, smoke/CO, permit and final inspection. Set rooms to the house.",
            "Rewire & Legacy",
            i("rewire_room", qty = 8.0),
            i("panel_200"),
            i("smoke_co_combo", qty = 4.0, variantIdx = 1),
            i("permit_service"),
            i("inspection_final"),
        ),
        a(
            "knob_tube_house", "Knob & Tube Replacement",
            "Whole-house knob & tube removal/replacement with a 200A panel and permit.",
            "Rewire & Legacy",
            i("knob_tube", variantIdx = 2),          // whole house
            i("panel_200"),
            i("permit_service"),
            i("inspection_final"),
        ),
        a(
            "aluminum_remediation", "Aluminum Wiring Remediation",
            "CO/ALR or pigtail remediation across ~20 devices, permitted.",
            "Rewire & Legacy",
            i("aluminum_wiring", qty = 20.0),
            i("permit_general"),
        ),

        // Outdoor & specialty
        a(
            "hot_tub_spa", "Hot Tub / Spa",
            "Spa circuit with GFCI + bonding, the required disconnect, and permit.",
            "Outdoor & Specialty",
            i("hot_tub", variantIdx = 2),            // with GFCI + bonding
            i("ac_disconnect"),
            i("permit_general"),
        ),
        a(
            "detached_garage", "Detached Garage / Shop",
            "Underground-fed 100A sub-panel, circuits, GFCI and a flood light, permitted.",
            "Outdoor & Specialty",
            i("outdoor_subpanel", variantIdx = 2),   // 100A underground
            i("garage_circuit", qty = 2.0),
            i("gfci_outdoor", variantIdx = 1),
            i("flood_light", variantIdx = 1),
            i("permit_general"),
        ),
        a(
            "pool_equipment", "Pool Equipment Package",
            "Pump circuit, bonding, disconnect and GFCI breaker protection, permitted.",
            "Outdoor & Specialty",
            i("pool_pump_circuit", variantIdx = 2),  // 240V variable speed
            i("pool_bonding", variantIdx = 1),       // existing pool remediation
            i("pool_disconnect"),
            i("pool_gfci"),
            i("permit_general"),
        ),
    )

    // ── Commercial & industrial (ELITE) — added in the Elite templates commit ──
    private val commercialIndustrial: List<Assembly> = emptyList()

    val all: List<Assembly> = residential + commercialIndustrial

    fun byId(id: String): Assembly? = all.firstOrNull { it.id == id }

    /**
     * True if every line references a real catalog service with an in-range
     * variant (residential) or a real EliteCatalog id when one is claimed
     * (commercial/industrial). Guards the curated list against catalog drift.
     */
    fun validate(): Boolean = all.all { assembly ->
        val catalogOk = assembly.items.all { item ->
            val svc = Catalog.service(item.serviceId)
            svc != null && item.variantIdx in svc.variants.indices && item.qty > 0.0
        }
        val customOk = assembly.customItems.all { item ->
            item.qty > 0.0 && item.laborHours > 0.0 &&
                (item.eliteMaterialId == null || EliteCatalog.material(item.eliteMaterialId) != null)
        }
        catalogOk && customOk && (assembly.items.isNotEmpty() || assembly.customItems.isNotEmpty())
    }
}
