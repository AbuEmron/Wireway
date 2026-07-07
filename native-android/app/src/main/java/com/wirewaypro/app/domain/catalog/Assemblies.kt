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

    // ── Rooms & areas (PRO) — the job-walk building blocks ──────────────────────
    // Per-room templates the electrician stacks while walking: "3 bedrooms, 2
    // baths, 1 kitchen". Each expands to REAL catalog lines at typical
    // residential quantities (NEC receptacle/lighting/AFCI/smoke rules), scaled
    // by the area count in the composer. Every quantity is editable in the builder.
    private val roomsAndAreas: List<Assembly> = listOf(
        a(
            "recessed_light_each", "Recessed Light (per fixture)",
            "One recessed can — add as many as the room has. The atomic lighting unit.",
            "Rooms & Areas",
            i("light_recessed", qty = 1.0),
        ),
        a(
            "dedicated_20a", "Dedicated 20A Circuit",
            "One 20A branch circuit — appliance, bathroom, or small-appliance run.",
            "Rooms & Areas",
            i("circuit_20"),
        ),
        a(
            "bedroom_standard", "Bedroom (standard)",
            "Receptacles on spacing, ceiling box, switch, interconnected smoke/CO and the required AFCI.",
            "Rooms & Areas",
            i("outlet_standard", qty = 4.0, variantIdx = 1),  // new same-wall 15A
            i("light_ceiling", variantIdx = 1),               // new box + flush mount
            i("switch_single", variantIdx = 3),               // new location
            i("smoke_co_combo", variantIdx = 1),              // new interconnected
            i("arc_fault"),                                    // NEC 210.12 bedroom AFCI
        ),
        a(
            "living_room_standard", "Living / Family Room (standard)",
            "Wall receptacles on spacing, a switched ceiling box on 3-ways, and AFCI.",
            "Rooms & Areas",
            i("outlet_standard", qty = 5.0, variantIdx = 1),
            i("light_ceiling", variantIdx = 1),
            i("switch_3way", qty = 2.0, variantIdx = 2),      // new location, two ends
            i("arc_fault"),
        ),
        a(
            "bathroom_standard", "Bathroom (standard)",
            "GFCI receptacle, vanity light, exhaust fan, its own 20A circuit and a permit.",
            "Rooms & Areas",
            i("outlet_gfci", variantIdx = 1),                 // new install 15A
            i("light_vanity", variantIdx = 1),                // new standard bar
            i("exhaust_fan", variantIdx = 2),                 // new cut-in standard
            i("switch_single", variantIdx = 3),
            i("circuit_20", variantIdx = 1),                  // dedicated bath circuit
            i("permit_general"),
        ),
        a(
            "kitchen_area", "Kitchen (standard)",
            "Two small-appliance circuits, dishwasher + disposal, GFCI counters, recessed + under-cabinet lighting, permitted.",
            "Rooms & Areas",
            i("circuit_20", qty = 2.0, variantIdx = 1),       // small-appliance circuits
            i("circuit_dedicated", qty = 2.0),                // dishwasher + disposal
            i("outlet_gfci", qty = 4.0, variantIdx = 2),      // 20A counter receptacles
            i("light_recessed", qty = 6.0),
            i("light_undercab"),
            i("permit_general"),
        ),
        a(
            "hallway_standard", "Hallway (standard)",
            "A receptacle, a ceiling fixture on 3-way switches, and an interconnected smoke/CO.",
            "Rooms & Areas",
            i("outlet_standard", variantIdx = 1),
            i("light_ceiling", variantIdx = 1),
            i("switch_3way", qty = 2.0, variantIdx = 2),
            i("smoke_co_combo", variantIdx = 1),
        ),
        a(
            "garage_area", "Garage (standard)",
            "A 20A circuit, GFCI receptacles, ceiling lights and a switch.",
            "Rooms & Areas",
            i("circuit_20"),
            i("outlet_gfci", qty = 2.0, variantIdx = 2),
            i("light_ceiling", qty = 2.0, variantIdx = 1),
            i("switch_single", variantIdx = 3),
        ),
    )

    // ── Commercial & industrial (ELITE) ─────────────────────────────────────────
    // Every line sources an EliteCatalog entry where one exists. Hours are
    // typical journeyman defaults the contractor edits; material prices are
    // NEVER hardcoded — this gear is market/quote-priced, so lines seed at $0
    // material for the supplier's quote (the description says so).
    private const val QUOTE_NOTE = " Material at supplier quote — hours are typical defaults, edit to the job."

    private val commercialIndustrial: List<Assembly> = listOf(
        c(
            "com_lighting_circuit_277", "277V Lighting Circuit (per ~100 ft run)",
            "One 277 V lighting branch: EMT, THHN, boxes and make-up.$QUOTE_NOTE",
            "Commercial",
            ci("EMT 1/2\" conduit — 10-ft length", 10.0, 0.4, "c_emt"),
            ci("EMT set-screw fittings", 14.0, 0.05, "c_emt_fittings"),
            ci("THHN #12 Cu — per 100 ft", 3.0, 0.8, "c_feeder_cu"),
            ci("4\" square boxes, rings & covers", 4.0, 0.25),
            ci("Circuit make-up, terminations & test", 1.0, 1.0),
        ),
        c(
            "com_troffer_package", "LED Troffer Package (per 10 fixtures)",
            "Grid-ceiling troffers with MC whips and 0–10 V dimming tie-in.$QUOTE_NOTE",
            "Commercial",
            ci("2×4 LED troffer, 277 V", 10.0, 0.5, "c_troffer"),
            ci("MC cable whip 12/2", 10.0, 0.25, "c_mc"),
            ci("0–10 V dimming wire — per 100 ft", 2.0, 0.5, "c_dim_wire"),
            ci("Controls tie-in & test", 1.0, 1.5),
        ),
        c(
            "com_panelboard_480", "480Y/277V Panelboard Install",
            "Set, feed and terminate a 42-circuit 480Y/277 panelboard.$QUOTE_NOTE",
            "Commercial",
            ci("Panelboard 480Y/277V 42-circuit", 1.0, 8.0, "c_panel_480"),
            ci("XHHW-2 Al feeder conductor — per 100 ft", 4.0, 1.5, "c_feeder_al"),
            ci("EMT 2\" conduit — 10-ft length", 6.0, 0.75, "c_emt"),
            ci("Strut & supports — 10-ft length", 4.0, 0.5, "c_strut"),
            ci("Terminations, torque & circuit directory", 1.0, 3.0),
        ),
        c(
            "com_xfmr_75", "Dry-Type Transformer (75 kVA) + 208V Panel",
            "480→208Y/120 step-down with its secondary panelboard, fed and grounded.$QUOTE_NOTE",
            "Commercial",
            ci("Dry-type transformer 480Δ→208Y/120, 75 kVA", 1.0, 6.0, "c_xfmr_dry"),
            ci("Panelboard 208Y/120V 42-circuit", 1.0, 6.0, "c_panel_208"),
            ci("EMT 2\" conduit — 10-ft length", 8.0, 0.75, "c_emt"),
            ci("THHN Cu feeder conductor — per 100 ft", 8.0, 1.2, "c_feeder_cu"),
            ci("Grounding & bonding make-up", 1.0, 2.0),
            ci("Terminations, torque & testing", 1.0, 3.0),
        ),
        c(
            "com_lighting_contactor", "Lighting Contactor Panel",
            "Contactor-switched lighting from a timeclock, photocell or the BAS.$QUOTE_NOTE",
            "Commercial",
            ci("Lighting contactor — 8-pole", 1.0, 2.5, "c_ltg_contactor"),
            ci("Class 2 control wiring — per 100 ft", 2.0, 0.6, "i_class2"),
            ci("Timeclock / photocell tie-in & test", 1.0, 1.5),
        ),
        c(
            "com_occ_sensors", "Occupancy Sensor Package (per 8 rooms)",
            "Line-voltage sensors swapped in at the switch point, per energy code.$QUOTE_NOTE",
            "Commercial",
            ci("Occupancy sensor, line-voltage 120/277 V", 8.0, 0.75, "c_occ_sensor"),
            ci("Neutral pull / box make-up — per room", 8.0, 0.4),
        ),
        c(
            "com_fire_alarm_10", "Fire Alarm Rough-In (per 10 devices)",
            "Addressable smokes and horn/strobes wired back to the FACP.$QUOTE_NOTE",
            "Commercial",
            ci("Addressable smoke detector", 6.0, 0.75, "c_fa_smoke"),
            ci("Horn/strobe notification appliance", 4.0, 0.75, "c_horn_strobe"),
            ci("FPLR cable — 1000-ft spool (pulled)", 1.0, 6.0, "c_fplr"),
            ci("Panel tie-in, programming & test — per device", 10.0, 0.25),
        ),
        c(
            "ind_motor_vfd", "Motor Circuit + VFD (to 10 HP, 480 V)",
            "Drive, disconnect, branch conductors and startup for one 3Ø motor.$QUOTE_NOTE",
            "Industrial",
            ci("VFD 480 V (to 10 HP)", 1.0, 4.0, "i_vfd"),
            ci("Heavy-duty safety switch — 30 A", 1.0, 1.5, "i_safety_switch"),
            ci("XHHW-2 Cu motor branch — per 100 ft", 1.0, 1.2, "i_motor_feeder"),
            ci("EMT 3/4\" conduit — 10-ft length", 8.0, 0.45, "c_emt"),
            ci("Pilot devices / control station", 1.0, 1.5, "i_pilot"),
            ci("Parameter set-up & rotation check", 1.0, 2.0),
        ),
        c(
            "ind_motor_starter", "Motor Circuit + NEMA Starter (to 25 HP)",
            "Full-voltage starter, disconnect, branch conductors and bump test.$QUOTE_NOTE",
            "Industrial",
            ci("Magnetic starter — NEMA size 1–2", 1.0, 3.0, "i_starter"),
            ci("Overload relay / heater elements (set)", 1.0, 0.5, "i_overload"),
            ci("Heavy-duty safety switch", 1.0, 1.5, "i_safety_switch"),
            ci("XHHW-2 Cu motor branch — per 100 ft", 1.0, 1.2, "i_motor_feeder"),
            ci("EMT 3/4\" conduit — 10-ft length", 8.0, 0.45, "c_emt"),
            ci("Control wiring & bump test", 1.0, 1.5),
        ),
        c(
            "ind_feeder_run", "Feeder Run (per 100 ft, EMT + Cu)",
            "Conduit, conductors, supports and testing for one feeder stretch — scale qty by length.$QUOTE_NOTE",
            "Industrial",
            ci("EMT 2\" conduit — 10-ft length", 10.0, 0.75, "c_emt"),
            ci("THHN Cu feeder — per 100 ft per conductor", 4.0, 1.2, "c_feeder_cu"),
            ci("Strut / trapeze supports", 8.0, 0.5, "c_strut"),
            ci("Terminations & insulation-resistance test", 1.0, 2.0),
        ),
        c(
            "ind_busway_run", "Busway Run (per 50 ft)",
            "Overhead bus with two plug-in drops, hung and supported.$QUOTE_NOTE",
            "Industrial",
            ci("Busway — 10-ft section", 5.0, 1.5, "i_busway"),
            ci("Busway plug-in unit", 2.0, 1.0, "i_busplug"),
            ci("Hangers & supports", 5.0, 0.5, "c_strut"),
        ),
        c(
            "ind_cable_tray", "Cable Tray Run (per 60 ft)",
            "Ladder tray with fittings, tray cable and bonding.$QUOTE_NOTE",
            "Industrial",
            ci("Ladder cable tray — 12-ft section", 5.0, 1.0, "i_tray_ladder"),
            ci("Tray fittings / splice sets", 4.0, 0.5, "i_tray_fittings"),
            ci("TC-ER tray cable — per 100 ft", 2.0, 1.0, "i_tc_cable"),
            ci("Supports & bonding", 1.0, 2.0),
        ),
        c(
            "ind_control_panel", "Control Panel Set + Field Wiring",
            "Set a UL 508A panel, land control power and field I/O, and check it out point-to-point.$QUOTE_NOTE",
            "Industrial",
            ci("UL 508A control panel (set in place)", 1.0, 4.0, "i_plc_panel"),
            ci("Control transformer", 1.0, 1.0, "i_ctrl_xfmr"),
            ci("Class 2 field wiring — per 100 ft", 4.0, 0.6, "i_class2"),
            ci("Terminations & point-to-point checkout", 1.0, 4.0),
        ),
    )

    /** The curated (built-in) library. User-created templates live in Room. */
    val all: List<Assembly> = roomsAndAreas + residential + commercialIndustrial

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
