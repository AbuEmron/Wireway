package com.wirewaypro.app.domain.catalog

/**
 * Commercial + industrial material library (ELITE tier).
 *
 * Accuracy doctrine: every entry carries a truthful generic spec, the governing
 * NEC 2023 article, and how the item is actually sold (its price basis) — but NO
 * dollar figures. Commercial/industrial gear is market- and quote-priced (copper
 * moves daily; switchgear is engineered to order), so a hardcoded price would be
 * a fabrication. The UI says so honestly and points at live distributor pricing.
 */
enum class EliteSector(val label: String) {
    COMMERCIAL("Commercial"),
    INDUSTRIAL("Industrial"),
}

data class EliteMaterial(
    val id: String,
    val label: String,
    /** Truthful generic spec: ratings, sizes, listings. */
    val spec: String,
    /** What work it's for, in field language. */
    val typicalUse: String,
    /** Governing NEC 2023 article/section. */
    val nec: String,
    /** Estimating/purchasing unit. */
    val unit: String,
    /** How it's sold + why it isn't list-priced here. */
    val priceBasis: String,
    /** Typical supply channels that stock it. */
    val vendors: List<String>,
)

data class EliteCategory(
    val id: String,
    val label: String,
    val sector: EliteSector,
    val materials: List<EliteMaterial>,
)

object EliteCatalog {

    private val DISTRIBUTORS = listOf("Graybar", "Rexel", "CED", "City Electric Supply", "Border States")
    private val GEAR_CHANNEL = listOf("Graybar", "Wesco", "Rexel", "CED")
    private val LIFE_SAFETY = listOf("Graybar", "Wesco", "ADI Global", "Rexel")
    private val HAZLOC_CHANNEL = listOf("Graybar", "Wesco", "Rexel", "hazardous-location specialists (Appleton, Killark, Crouse-Hinds lines)")

    private fun m(
        id: String, label: String, spec: String, typicalUse: String,
        nec: String, unit: String, priceBasis: String,
        vendors: List<String> = DISTRIBUTORS,
    ) = EliteMaterial(id, label, spec, typicalUse, nec, unit, priceBasis, vendors)

    val categories: List<EliteCategory> = listOf(

        // ── COMMERCIAL ──────────────────────────────────────────────────────────
        EliteCategory("c_distribution", "277/480V Distribution", EliteSector.COMMERCIAL, listOf(
            m(
                "c_panel_480", "Panelboard — 480Y/277V, 3Ø 4W",
                "42-circuit bolt-on, 225 A bus, main-lug or main-breaker, series ratings to 65 kAIC, NEMA 1.",
                "Distribution for 277 V lighting and 480 V mechanical loads in offices, retail, schools.",
                "NEC 408.36", "each",
                "Interior + trim + can priced separately; breakers per schedule. Quote-priced by configuration.",
                GEAR_CHANNEL,
            ),
            m(
                "c_panel_208", "Panelboard — 208Y/120V, 3Ø 4W",
                "42-circuit bolt-on, 100–225 A, main-lug or main-breaker, NEMA 1.",
                "Receptacle and small-appliance branch circuits fed from a step-down transformer.",
                "NEC 408.36", "each",
                "Interior + trim + can priced separately; breakers per schedule. Quote-priced by configuration.",
                GEAR_CHANNEL,
            ),
            m(
                "c_xfmr_dry", "Dry-Type Transformer — 480Δ → 208Y/120V",
                "15–112.5 kVA, 150 °C rise, aluminum windings standard (copper optional), NEMA 2 ventilated, floor/wall mount.",
                "Steps 480 V distribution down to 208Y/120 V for receptacles and general equipment.",
                "NEC 450.3(B)", "each",
                "Priced by kVA class (15 / 30 / 45 / 75 / 112.5). Quote from distributor.",
                GEAR_CHANNEL,
            ),
            m(
                "c_feeder_cu", "Feeder Conductor — THHN/THWN-2 Copper",
                "600 V, 90 °C dry / 75 °C wet, 8 AWG–500 kcmil, VW-1.",
                "Panel feeders and branch runs pulled in conduit.",
                "NEC 310.16", "per foot",
                "Sold by the foot or 500-ft reel, by size. Copper is commodity-priced and moves daily — price at order time.",
            ),
            m(
                "c_feeder_al", "Feeder Conductor — XHHW-2 Aluminum",
                "600 V, 90 °C, AA-8000 series alloy, 6 AWG–750 kcmil.",
                "Cost-effective long feeders; terminate on 75/90 °C-rated lugs and size per ampacity tables.",
                "NEC 310.16", "per foot",
                "Sold by the foot or reel, by size. Aluminum is commodity-priced — price at order time.",
            ),
            m(
                "c_switchboard", "Service Switchboard — 800–1200 A",
                "480Y/277 V 3Ø 4W, 65–100 kAIC, service-entrance rated; ground-fault protection required on 1000 A+ services.",
                "Main service gear for mid-size commercial buildings.",
                "NEC 230.95", "each",
                "Engineered-to-order — quoted per job from approved drawings.",
                GEAR_CHANNEL,
            ),
        )),

        EliteCategory("c_raceway", "Commercial Raceway & Wire", EliteSector.COMMERCIAL, listOf(
            m(
                "c_emt", "EMT Conduit — 1/2\" to 4\"",
                "Galvanized steel, 10-ft lengths, UL 797.",
                "The workhorse raceway for commercial interiors.",
                "NEC 358", "10-ft length",
                "Per length, by trade size. Steel is commodity-priced — confirm current pricing.",
            ),
            m(
                "c_emt_fittings", "EMT Fittings — Set-Screw / Compression",
                "Steel or die-cast zinc; compression styles for damp locations, raintight where listed.",
                "Couplings and connectors for EMT runs.",
                "NEC 358.42", "each",
                "Per fitting by trade size; bulk boxes cheaper per unit.",
            ),
            m(
                "c_rigid", "Rigid Conduit (GRC) — 1/2\" to 4\"",
                "Hot-dip galvanized steel, threaded, 10-ft lengths, UL 6.",
                "Physical-damage areas, service masts, exterior work and classified locations.",
                "NEC 344", "10-ft length",
                "Per length, by trade size. Steel commodity pricing — confirm at order.",
            ),
            m(
                "c_strut", "Strut Channel & Hardware",
                "1-5/8\" steel channel, plain or slotted, pre-galv or hot-dip; spring nuts, straps, trapeze hardware.",
                "Conduit racks and equipment supports.",
                "NEC 300.11", "10-ft length",
                "Channel per length; hardware per piece or box.",
            ),
            m(
                "c_wireway", "Wireway (Lay-In) — 4×4 to 8×8",
                "NEMA 1 painted steel, hinged or screw cover, 1–10 ft sections plus fittings.",
                "Gutter runs between gear and multiple raceway taps.",
                "NEC 376", "per section",
                "Per section + fittings, by cross-section and length.",
            ),
            m(
                "c_surface_raceway", "Surface Metal Raceway",
                "One- and two-piece steel raceway with matching device boxes and fittings (Wiremold-type).",
                "Adding circuits on finished walls without opening them.",
                "NEC 386", "5-ft length",
                "Per length + per fitting/box.",
            ),
            m(
                "c_mc", "MC Cable — 12/2 to 12/4, 277 V circuits",
                "THHN conductors in interlocked aluminum armor, 600 V; color-coded phases/neutrals for 277 V lighting.",
                "Commercial lighting whips and branch wiring where permitted.",
                "NEC 330", "250-ft coil",
                "Per coil, or cut by the foot. Copper content — price at order time.",
            ),
        )),

        EliteCategory("c_lighting", "Commercial Lighting & Controls", EliteSector.COMMERCIAL, listOf(
            m(
                "c_troffer", "LED Troffer — 2×4 / 2×2, 277 V",
                "UL + DLC listed, CCT-selectable 3500–5000 K, 30–50 W, 0–10 V dimming driver.",
                "Grid-ceiling lighting in offices, classrooms, retail.",
                "NEC 410", "each",
                "Per fixture; project quantities usually quoted through the lighting package.",
            ),
            m(
                "c_highbay", "LED High-Bay — 120–277 / 480 V",
                "UL + DLC listed, 100–240 W, 130+ lm/W, hook or pendant mount.",
                "Warehouse, gym and production ceilings 20 ft and up.",
                "NEC 410", "each",
                "Per fixture; quoted with the lighting package on larger jobs.",
            ),
            m(
                "c_ltg_contactor", "Lighting Contactor — 20 A, 2–12 Pole",
                "Electrically or mechanically held, 120/277 V coils, UL 508 listed, enclosed or panel-mount.",
                "Switching banks of 277 V lighting from timeclocks, photocells or the building automation system.",
                "NEC 404", "each",
                "Per contactor by pole count and enclosure.",
            ),
            m(
                "c_occ_sensor", "Occupancy Sensor — Line-Voltage 120/277 V",
                "PIR or dual-technology, wall-switch or ceiling mount; neutral required at the switch box.",
                "Energy-code auto-off control in offices, restrooms, storage rooms.",
                "NEC 404.2(C)", "each",
                "Per sensor; ceiling low-voltage systems add a power pack per zone.",
            ),
            m(
                "c_exit_combo", "Exit / Emergency Combo Unit",
                "LED, 90-minute battery, universal chevrons and mounting; self-diagnostic models available.",
                "Egress path illumination and exit marking.",
                "NEC 700.16", "each",
                "Per unit; remote-head capable units cost more.",
            ),
            m(
                "c_dim_wire", "0–10 V Dimming Control Wire",
                "Class 2 rated pair (16/2 or 18/2), purple/gray convention.",
                "Continuous dimming of LED drivers from wall or networked controls.",
                "NEC 725", "1000-ft spool",
                "Per spool; also sold by the foot.",
            ),
        )),

        EliteCategory("c_fire_alarm", "Fire Alarm & Life Safety", EliteSector.COMMERCIAL, listOf(
            m(
                "c_facp", "Fire Alarm Control Panel (FACP)",
                "Addressable, 1–2 signaling loops expandable, UL 864 listed, with standby batteries and charger.",
                "The brain of a commercial fire alarm system; sized to device count and loops.",
                "NEC 760", "each",
                "Quoted with the fire alarm submittal package; panel + modules + batteries.",
                LIFE_SAFETY,
            ),
            m(
                "c_fa_smoke", "Addressable Smoke Detector",
                "Photoelectric, twist-lock base, UL 268 listed.",
                "Area smoke detection reporting a unique address to the FACP.",
                "NEC 760", "each",
                "Per head + base; must match the panel manufacturer's protocol.",
                LIFE_SAFETY,
            ),
            m(
                "c_horn_strobe", "Horn/Strobe Notification Appliance",
                "24 VDC, field-selectable 15–110 cd, wall or ceiling, UL 464/1971 listed.",
                "Audible and visual occupant notification on NAC circuits.",
                "NEC 760", "each",
                "Per device; candela mix set by the designer's layout.",
                LIFE_SAFETY,
            ),
            m(
                "c_fplr", "Fire Alarm Cable — FPLR",
                "14–18 AWG solid pairs, red jacket, power-limited riser listed.",
                "SLC and NAC wiring between the panel and devices.",
                "NEC 760.179", "1000-ft spool",
                "Per spool by gauge/pair count. Copper content — price at order.",
                LIFE_SAFETY,
            ),
            m(
                "c_duct_detector", "Duct Smoke Detector",
                "Photoelectric with sampling tubes sized to duct width, UL 268A listed.",
                "HVAC unit shutdown on smoke detection per the mechanical code.",
                "NEC 760", "each",
                "Per detector + sampling tube + remote test/indicator station.",
                LIFE_SAFETY,
            ),
        )),

        // ── INDUSTRIAL ──────────────────────────────────────────────────────────
        EliteCategory("i_motor", "Motor Control & Drives", EliteSector.INDUSTRIAL, listOf(
            m(
                "i_starter", "Magnetic Motor Starter — NEMA Size 1–4",
                "600 V class, 3-pole, melting-alloy or solid-state overload relay, 120 V control coil.",
                "Full-voltage starting of three-phase motors up to roughly 100 HP.",
                "NEC 430 Part VII", "each",
                "Per starter by NEMA size and enclosure; overload heaters/elements sized to nameplate FLA.",
                GEAR_CHANNEL,
            ),
            m(
                "i_vfd", "Variable Frequency Drive — 5–50 HP, 480 V",
                "3Ø in/out, V/Hz and sensorless-vector control, NEMA 1 or flange mount, EMC filter options.",
                "Speed control and soft starting for pumps, fans, conveyors. Supply conductors sized at 125% of drive input current.",
                "NEC 430.122", "each",
                "Per drive by HP/frame; commissioning and line/load reactors quoted separately.",
                GEAR_CHANNEL,
            ),
            m(
                "i_combo", "Combination Motor Controller",
                "Fusible or circuit-breaker disconnect + magnetic starter in one NEMA 12 enclosure.",
                "In-sight disconnect and motor control in a single box at the machine.",
                "NEC 430.102", "each",
                "Per unit by HP, fault rating and enclosure.",
                GEAR_CHANNEL,
            ),
            m(
                "i_overload", "Overload Relay / Heater Elements",
                "Class 10/20/30 trip curves; elements or dial set to motor nameplate full-load amps.",
                "Running overload protection sized to the nameplate — not a code table.",
                "NEC 430.32", "each",
                "Per relay or per set of heater elements.",
                GEAR_CHANNEL,
            ),
            m(
                "i_pilot", "Pilot Devices — 30 mm",
                "Start/stop pushbuttons, selector switches, pilot lights; NEMA 4/13 heads and contact blocks.",
                "Operator stations on machines and MCC buckets.",
                "NEC 430", "each",
                "Per operator + contact block + legend plate.",
                GEAR_CHANNEL,
            ),
            m(
                "i_motor_feeder", "Motor Branch Conductors — XHHW-2 Cu",
                "600 V copper, sized at no less than 125% of the motor's full-load current from Table 430.250.",
                "Branch-circuit conductors to individual three-phase motors.",
                "NEC 430.22", "per foot",
                "By the foot or reel, by size. Copper commodity pricing — price at order.",
            ),
        )),

        EliteCategory("i_power", "Industrial Power & Gear", EliteSector.INDUSTRIAL, listOf(
            m(
                "i_xfmr", "Transformer — 480 V, 150–500 kVA",
                "Dry-type or padmount, delta-wye, 80/115 °C rise options, ±2×2.5% taps.",
                "Load-center distribution inside plants and process areas.",
                "NEC 450.3(B)", "each",
                "Engineered/quoted per job by kVA, impedance and enclosure.",
                GEAR_CHANNEL,
            ),
            m(
                "i_switchboard", "Switchboard — 1200–3000 A, 480 V",
                "3Ø 4W, 65–100 kAIC, feeder breaker and metering sections.",
                "Plant main and heavy distribution gear.",
                "NEC 408", "each",
                "Engineered-to-order from approved drawings — quoted per job.",
                GEAR_CHANNEL,
            ),
            m(
                "i_busway", "Busway / Bus Duct — 225–800 A",
                "Aluminum or copper bus, feeder and plug-in types, 10-ft sections plus elbows/ends.",
                "Overhead power spine with tap-off flexibility down production lines.",
                "NEC 368", "10-ft section",
                "Per section + fittings; layouts quoted from drawings.",
                GEAR_CHANNEL,
            ),
            m(
                "i_busplug", "Busway Plug-In Unit — 30–400 A",
                "Fusible or breaker type, hookstick operable, interlocked covers.",
                "Dropping power from overhead busway to individual machines.",
                "NEC 368", "each",
                "Per unit by amperage and fuse/breaker type.",
                GEAR_CHANNEL,
            ),
            m(
                "i_safety_switch", "Heavy-Duty Safety Switch — 30–600 A",
                "600 V, fusible or non-fused, NEMA 3R/12/4X enclosures, quick-make quick-break.",
                "Equipment disconnects and feeder isolation.",
                "NEC 404", "each",
                "Per switch by amperage, fusing and enclosure; fuses separate.",
                GEAR_CHANNEL,
            ),
        )),

        EliteCategory("i_tray", "Cable Tray & Industrial Wiring", EliteSector.INDUSTRIAL, listOf(
            m(
                "i_tray_ladder", "Ladder Cable Tray — 12–24\" wide",
                "Aluminum or steel ladder tray, 12-ft sections, NEMA VE-1; load class per span.",
                "Routing feeder and control cable across process and mechanical areas.",
                "NEC 392", "12-ft section",
                "Per section by width/material; supports and covers separate.",
            ),
            m(
                "i_tray_fittings", "Tray Fittings — Elbows, Tees, Reducers",
                "Match rail height, width and material; splice plates, hold-downs, barrier strips.",
                "Turning, branching and stepping tray runs.",
                "NEC 392", "each",
                "Per fitting; splice hardware per pair.",
            ),
            m(
                "i_tc_cable", "Tray Cable — TC-ER",
                "600 V multi-conductor with ground, sunlight-resistant, exposed-run (ER) rated.",
                "Power and control from tray into equipment without conduit for the last stretch.",
                "NEC 336", "per foot",
                "By the foot or 1000-ft reel, by conductor count/size. Copper pricing at order.",
            ),
            m(
                "i_soow", "Flexible Cord — SOOW",
                "600 V thermoset, oil- and water-resistant, 18 AWG–2 AWG multi-conductor.",
                "Pendant drops, festoons and portable equipment connections.",
                "NEC 400.10", "per foot",
                "By the foot or 250-ft reel, by size.",
            ),
            m(
                "i_cord_grip", "Cord Grips / Strain Reliefs",
                "Aluminum body or wire-mesh grip styles, NPT hubs, liquidtight options.",
                "Terminating cords into enclosures without stressing the terminations.",
                "NEC 400", "each",
                "Per grip by cord OD range and hub size.",
            ),
        )),

        EliteCategory("i_controls", "Controls & PLC", EliteSector.INDUSTRIAL, listOf(
            m(
                "i_ctrl_xfmr", "Control Transformer — 50–1000 VA",
                "480/240 × 120 V machine-tool rated, fused primary/secondary options.",
                "120 V control power inside motor-control and machine panels.",
                "NEC 430.72", "each",
                "Per transformer by VA; fuse kits separate.",
                GEAR_CHANNEL,
            ),
            m(
                "i_plc_panel", "Industrial Control Panel / PLC Enclosure",
                "UL 508A listed assembly: NEMA 12 enclosure, backpanel, wire duct, terminals, disconnect.",
                "Housing the PLC, drives, relays and I/O for machine control.",
                "NEC 409", "each",
                "Assembled panels quoted per bill of materials and shop labor.",
                GEAR_CHANNEL,
            ),
            m(
                "i_terminals", "Terminal Blocks & DIN Hardware",
                "600 V feed-through, fused and ground blocks on 35 mm DIN rail; jumpers and markers.",
                "Landing field wiring cleanly inside control panels.",
                "NEC 409", "50-pack",
                "Per pack/piece by block type.",
                GEAR_CHANNEL,
            ),
            m(
                "i_relays", "Machine Control Relays",
                "IEC ice-cube or NEMA style, 120 VAC / 24 VDC coils, DPDT–4PDT with sockets.",
                "Interlocks and logic interfacing between control voltages.",
                "NEC 409", "each",
                "Per relay + socket.",
                GEAR_CHANNEL,
            ),
            m(
                "i_class2", "Class 2 Control Wiring",
                "18–22 AWG, PLTC/CM listings for panel-to-field sensor and I/O runs.",
                "24 V sensor, actuator and I/O wiring around machines.",
                "NEC 725", "1000-ft spool",
                "Per spool by gauge/pair count.",
            ),
        )),

        EliteCategory("i_hazloc", "Hazardous Locations (Class I)", EliteSector.INDUSTRIAL, listOf(
            m(
                "i_seal_off", "Conduit Seal-Off Fittings (EYS-type)",
                "Threaded, vertical/horizontal styles, poured with listed sealing compound over a fiber dam.",
                "Sealing conduit where it crosses Division boundaries or enters arcing-device enclosures.",
                "NEC 501.15", "each",
                "Per fitting by trade size; compound and fiber sold separately.",
                HAZLOC_CHANNEL,
            ),
            m(
                "i_xp_box", "Explosionproof Enclosure / Box",
                "Cast aluminum or iron, threaded hubs and covers, listed Class I Div 1 (Groups C, D).",
                "Splices, devices and controls inside classified areas.",
                "NEC 500.7", "each",
                "Per enclosure by size and hub configuration.",
                HAZLOC_CHANNEL,
            ),
            m(
                "i_xp_fixture", "Hazardous-Location LED Fixture",
                "Listed Class I Div 1 or Div 2 with a temperature (T) code suited to the area classification.",
                "Lighting for fuel, chemical and grain-handling areas.",
                "NEC 501.130", "each",
                "Per fixture; Div 1 listings cost substantially more than Div 2.",
                HAZLOC_CHANNEL,
            ),
            m(
                "i_xp_flex", "Explosionproof Flexible Coupling",
                "Threaded bronze/stainless flexible fitting, listed for Class I Division 1.",
                "Vibration isolation at motors and equipment in classified areas.",
                "NEC 501.10(A)", "each",
                "Per coupling by trade size and length.",
                HAZLOC_CHANNEL,
            ),
        )),
    )

    val allMaterials: List<EliteMaterial> = categories.flatMap { it.materials }

    private val byId: Map<String, EliteMaterial> = allMaterials.associateBy { it.id }

    fun material(id: String): EliteMaterial? = byId[id]
}
