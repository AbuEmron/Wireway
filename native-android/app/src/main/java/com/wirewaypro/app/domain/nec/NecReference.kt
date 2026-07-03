package com.wirewaypro.app.domain.nec

/**
 * An NEC 2023 reference article. Plain-English summaries and the common
 * violations an inspector flags — residential set ported from the web app's
 * nec-reference.js; commercial/industrial/medical set is the Elite layer.
 * Educational reference only; always confirm against the adopted code + local amendments.
 */
data class NecArticle(
    val number: String,   // e.g. "210"
    val title: String,    // e.g. "Branch Circuits"
    val summary: String,
    val rules: List<String>,
    val violations: List<String>,
    /** Grouping label: Residential (all tiers) or an Elite sector. */
    val sector: String = "Residential",
) {
    /** Lowercase haystack for search. */
    val search: String = (number + " " + title + " " + summary).lowercase()
}

object NecReference {

    val ARTICLES: List<NecArticle> = listOf(
        NecArticle(
            "110", "Requirements for Electrical Installations",
            "General rules for any install: workmanship, listed equipment, terminations, and working clearances.",
            listOf(
                "Working space in front of panels/equipment: 36 in deep, 30 in wide (or equipment width), 78 in high (110.26).",
                "Use only listed/labeled equipment and install per its instructions (110.3).",
                "Torque terminations to the manufacturer's spec; match conductor temperature rating (110.14).",
            ),
            listOf(
                "Panel blocked by shelving/appliances inside the 36-in working space.",
                "Backstabbed devices or loose lugs (no torque to spec).",
                "Mixing copper/aluminum on terminals not rated CU-AL.",
            ),
        ),
        NecArticle(
            "210", "Branch Circuits",
            "The heart of residential wiring: GFCI/AFCI protection, receptacle spacing, and the required circuits in a dwelling.",
            listOf(
                "GFCI protection (210.8): kitchens, bathrooms, garages, outdoors, basements, laundry, within 6 ft of any sink, dishwashers.",
                "AFCI protection (210.12): nearly all 120 V 15/20 A dwelling branch circuits (bedrooms, living areas, kitchens, laundry, etc.).",
                "Receptacle spacing (210.52): no point along a wall > 6 ft from a receptacle (i.e., outlets at least every 12 ft); any wall ≥ 24 in counts.",
                "Kitchen counters: receptacle within 24 in of each counter point and any counter ≥ 12 in wide gets one (210.52(C)).",
                "Required circuits: ≥ 2 small-appliance, 1 laundry, 1 bathroom (20 A each).",
            ),
            listOf(
                "No GFCI on a garage, basement, or within-6-ft-of-sink receptacle.",
                "Missing AFCI on bedroom/living-room circuits.",
                "Kitchen counter spacing > 24 in to a receptacle.",
                "Bathroom sharing the 20 A circuit with other rooms.",
            ),
        ),
        NecArticle(
            "220", "Branch-Circuit, Feeder, and Service Load Calculations",
            "How to size the service/feeders for a dwelling. The optional method (220.82) is the common residential shortcut.",
            listOf(
                "General lighting load: 3 VA per sq ft of living area.",
                "Two small-appliance circuits + laundry: 1500 VA each.",
                "Optional dwelling method (220.82): 100% of first 10 kVA + 40% of the remainder, plus largest heating/AC load.",
                "Add fixed appliances, EV, range, dryer, water heater at nameplate (with allowed demand factors).",
            ),
            listOf(
                "Adding an EV charger or heat pump without recalculating service capacity.",
                "Omitting the small-appliance/laundry 1500 VA allowances.",
            ),
        ),
        NecArticle(
            "230", "Services",
            "Service-entrance conductors, the service disconnect, and how power enters the dwelling.",
            listOf(
                "Service disconnect must be readily accessible, outside or nearest the point of entry (230.70).",
                "Emergency disconnect required at one- and two-family dwellings, marked, outside (230.85).",
                "No more than six disconnects per service grouping (230.71).",
            ),
            listOf(
                "No exterior emergency disconnect on a new/updated service.",
                "Service disconnect buried deep inside the house far from entry.",
            ),
        ),
        NecArticle(
            "240", "Overcurrent Protection",
            "Breaker/fuse sizing to protect conductors. The small-conductor rule (240.4(D)) is the everyday one.",
            listOf(
                "Small-conductor max OCPD (240.4(D)): 14 AWG = 15 A, 12 AWG = 20 A, 10 AWG = 30 A copper.",
                "Breaker must protect the smallest conductor on the circuit.",
                "Handle ties for multi-wire branch circuits feeding the same yoke.",
            ),
            listOf(
                "20 A breaker on 14 AWG wire (very common and dangerous).",
                "Oversized breaker 'fixing' nuisance trips instead of the real fault.",
            ),
        ),
        NecArticle(
            "250", "Grounding and Bonding",
            "The grounding electrode system, bonding, and equipment grounding conductors — the safety backbone.",
            listOf(
                "Grounding electrode system: ground rods (two, 6 ft apart unless 25 Ω met), plus a Ufer (concrete-encased) where present (250.50).",
                "Bond metal water pipe within 5 ft of entry and the gas piping system (250.104).",
                "Size the EGC from breaker rating per Table 250.122.",
                "Neutral and ground bonded only at the service; separated in subpanels.",
            ),
            listOf(
                "Subpanel with neutral and ground bonded together (not isolated).",
                "Single ground rod with no second rod and no proof of < 25 Ω.",
                "Missing main bonding jumper at the service.",
            ),
        ),
        NecArticle(
            "300", "General Requirements for Wiring Methods",
            "Burial depths, protection from damage, and conductor handling.",
            listOf(
                "Direct-burial / conduit depths (300.5): 24 in direct burial, 18 in PVC, 6 in RMC under a slab (typical residential).",
                "Protect cables within 1.25 in of a stud face with a steel plate (300.4).",
                "All conductors of a circuit run together in the same raceway/cable.",
            ),
            listOf(
                "NM cable run on the face of studs without protection.",
                "Underground feeder too shallow.",
            ),
        ),
        NecArticle(
            "310", "Conductors for General Wiring",
            "Conductor ampacity and insulation. Table 310.16 is the workhorse.",
            listOf(
                "Common copper ampacities (60/75°C): 14 AWG = 15 A, 12 = 20/25 A, 10 = 30/35 A, 8 = 40/50 A.",
                "Terminations on most residential gear are rated 60°C (≤ 100 A) — size to that column.",
                "Apply ambient + conduit-fill derating where applicable.",
            ),
            listOf(
                "Using the 90°C column for ampacity on 60°C-rated terminals.",
            ),
        ),
        NecArticle(
            "314", "Outlet, Device, Pull, and Junction Boxes",
            "Box fill, support, and accessibility.",
            listOf(
                "Calculate box fill: count conductors, devices (×2), clamps, grounds per 314.16.",
                "Junction boxes must remain accessible — never buried in a wall.",
                "Support boxes independently per 314.23.",
            ),
            listOf(
                "Overstuffed box (fill exceeds the cubic-inch allowance).",
                "Buried junction box behind drywall.",
            ),
        ),
        NecArticle(
            "334", "Nonmetallic-Sheathed Cable (NM / Romex)",
            "The most common residential wiring method and its support/protection rules.",
            listOf(
                "Secure within 12 in of a box and at least every 4.5 ft (334.30).",
                "Not allowed exposed where subject to damage; protect in unfinished basements/garages.",
                "Don't run NM in conduit underground or in wet locations.",
            ),
            listOf(
                "Romex stapled too far from the box or sagging between supports.",
                "NM cable used outdoors/underground.",
            ),
        ),
        NecArticle(
            "404", "Switches",
            "Switch placement, grounding, and the neutral-at-the-switch rule.",
            listOf(
                "A grounded (neutral) conductor is required at most switch locations (404.2(C)) for smart switches.",
                "Snap switches must be grounded.",
                "Switch the ungrounded (hot) conductor, not the neutral.",
            ),
            listOf(
                "No neutral in the switch box (common in older homes; blocks smart switches).",
                "Switching the neutral instead of the hot.",
            ),
        ),
        NecArticle(
            "406", "Receptacles, Cord Connectors, and Attachment Plugs",
            "Tamper-resistant and weather-resistant receptacle requirements.",
            listOf(
                "Tamper-resistant receptacles required in dwellings (406.12).",
                "Outdoor/damp/wet receptacles must be weather-resistant with an in-use ('bubble') cover (406.9).",
                "Receptacles installed so the box/plaster ring is flush and supported.",
            ),
            listOf(
                "Standard (non-TR) receptacles in a dwelling.",
                "Exterior receptacle with a flat 'while-closed' cover instead of in-use cover.",
            ),
        ),
        NecArticle(
            "408", "Switchboards, Switchgear, and Panelboards",
            "Panel labeling, circuit directory, and breaker limits.",
            listOf(
                "Legible, accurate circuit directory required (408.4).",
                "Don't exceed the panel's listed breaker count; no double-tapping unless the breaker is listed for two conductors.",
                "Maintain working clearances (see 110.26).",
            ),
            listOf(
                "Missing/blank panel directory.",
                "Two wires under one breaker not rated for it.",
            ),
        ),
        NecArticle(
            "410", "Luminaires, Lampholders, and Lamps",
            "Fixture clearances, closet rules, and recessed-can ratings.",
            listOf(
                "Clothes-closet clearances (410.16): 12 in for surface incandescent, 6 in for recessed/LED to storage space.",
                "Recessed luminaires in insulation must be IC-rated.",
                "Wet/damp-location fixtures listed for the location.",
            ),
            listOf(
                "Non-IC recessed can buried in attic insulation.",
                "Surface fixture too close to closet shelving.",
            ),
        ),
        NecArticle(
            "422", "Appliances",
            "Dedicated circuits and disconnecting means for fixed appliances.",
            listOf(
                "Dishwasher and disposal: GFCI-protected; provide a disconnect (cord-and-plug or switch).",
                "Dedicated circuits for fixed appliances sized to nameplate.",
                "Provide a means to disconnect each appliance (422.30+).",
            ),
            listOf(
                "Dishwasher with no GFCI and no accessible disconnect.",
            ),
        ),
        NecArticle(
            "440", "Air-Conditioning and Refrigerating Equipment",
            "Sizing and disconnect rules for AC/heat-pump compressors.",
            listOf(
                "Use the nameplate MCA (min circuit ampacity) and MOCP (max OCPD) — not the breaker you 'usually use'.",
                "A disconnect must be within sight of the outdoor unit (440.14).",
            ),
            listOf(
                "AC condenser with no in-sight disconnect.",
                "Breaker larger than the nameplate MOCP.",
            ),
        ),
        NecArticle(
            "445", "Generators",
            "Permanently installed standby generators.",
            listOf(
                "Provide overcurrent protection and a disconnect for the generator output.",
                "Bond/ground per the system type (separately derived vs. not) — affects neutral switching in the transfer switch.",
            ),
            listOf(
                "Transfer switch that doesn't switch the neutral when required for a separately derived system.",
            ),
        ),
        NecArticle(
            "590", "Temporary Installations",
            "Construction-site and temporary power.",
            listOf(
                "GFCI protection for all 15/20/30 A receptacles used by personnel during construction (590.6).",
                "Temporary wiring removed at completion.",
            ),
            listOf(
                "Construction receptacles with no GFCI.",
            ),
        ),
        NecArticle(
            "625", "Electric Vehicle Power Transfer System",
            "EV charging (EVSE) circuits — increasingly common residential work.",
            listOf(
                "Dedicated circuit sized at 125% of the EVSE continuous rating (a 48 A charger needs a 60 A circuit).",
                "GFCI/personnel protection as required; many Level 2 units include CCID.",
                "Energy/load management systems (625.42) can avoid a service upgrade by limiting simultaneous load.",
                "New dwelling garages: provide EV-ready capacity per 210.71 / local adoption.",
            ),
            listOf(
                "EVSE on an undersized circuit (not 125% continuous).",
                "Adding 48 A of EV load to a maxed-out service with no load management.",
            ),
        ),
        NecArticle(
            "680", "Swimming Pools, Spas, Hot Tubs, and Fountains",
            "Wet-location bonding and GFCI — a high-liability area inspectors scrutinize.",
            listOf(
                "Equipotential bonding grid (#8 solid copper) around pools/spas (680.26).",
                "GFCI protection for pool pumps, lighting, and nearby receptacles.",
                "Receptacle clearances: 6–20 ft rules from the water's edge (680.22).",
            ),
            listOf(
                "Missing equipotential bonding around a spa/pool.",
                "Pool pump or light without GFCI.",
            ),
        ),
        NecArticle(
            "702", "Optional Standby Systems",
            "Whole-home/partial standby generators and transfer equipment (the typical residential backup).",
            listOf(
                "Transfer equipment must prevent inadvertent paralleling with the utility (702.5).",
                "Size the optional standby system and inlet to the load served.",
            ),
            listOf(
                "Backfeeding a panel through a dryer outlet ('suicide cord') with no interlock.",
            ),
        ),
        NecArticle(
            "705", "Interconnected Electric Power Production Sources",
            "Solar PV / battery interconnection and the busbar backfeed rules.",
            listOf(
                "120% busbar rule (705.12): main breaker + backfeed breaker ≤ 120% of busbar rating, with the PV breaker at the opposite end.",
                "Label all interconnection and disconnect points.",
            ),
            listOf(
                "PV breaker added to a fully loaded busbar exceeding the 120% allowance.",
                "Missing solar/backfeed labeling at the panel and meter.",
            ),
        ),
    )

    /**
     * Commercial, industrial and health-care articles (ELITE tier). Real NEC 2023
     * article numbers only — plain-English rules an electrician can act on, and
     * always verified against the adopted edition + local amendments on the job.
     */
    val ELITE_ARTICLES: List<NecArticle> = listOf(

        // ── Commercial ──────────────────────────────────────────────────────────
        NecArticle(
            "210", "Branch Circuits — Non-Dwelling",
            "The commercial angle on branch circuits: GFCI in non-dwelling occupancies and the 125% continuous-load rule that governs lighting and signage.",
            listOf(
                "GFCI in non-dwelling occupancies (210.8(B)): bathrooms, kitchens, rooftops, outdoors, indoor damp/wet locations, and within 6 ft of sinks.",
                "Continuous loads (3 hours+, e.g. commercial lighting): conductors and OCPD sized at 125% (210.19(A), 210.20(A)).",
                "Multiwire branch circuits need a means to simultaneously disconnect all ungrounded conductors (210.4(B)).",
            ),
            listOf(
                "Rooftop HVAC service receptacle with no GFCI.",
                "A 20 A breaker loaded to 20 A of continuous lighting (max is 16 A).",
                "Shared-neutral circuits on independent breakers with no handle tie.",
            ),
            sector = "Commercial",
        ),
        NecArticle(
            "220", "Load Calculations — Non-Dwelling",
            "Sizing services and feeders for commercial occupancies: lighting by occupancy type, 180 VA receptacles, and the demand factors.",
            listOf(
                "General lighting load by occupancy from Table 220.42(A) (VA per sq ft varies by building type).",
                "Each general-use receptacle strap counts as 180 VA (220.14(I)); multioutlet assemblies per 220.14(H).",
                "Non-dwelling receptacle loads over 10 kVA may take a 50% demand factor on the remainder (Table 220.44).",
                "Show windows and track lighting carry their own VA allowances (220.14(G), (F)).",
            ),
            listOf(
                "Counting commercial receptacles below 180 VA each to shrink the service.",
                "Ignoring the 125% continuous factor on the lighting load.",
            ),
            sector = "Commercial",
        ),
        NecArticle(
            "645", "Information Technology Equipment (IT / Data Rooms)",
            "Server and data rooms — Article 645's relaxed wiring rules apply ONLY when the room meets every condition in 645.4.",
            listOf(
                "To use Art. 645 wiring permissions the room needs: dedicated HVAC, disconnecting means, restricted access, and separation per 645.4 — otherwise wire it under Chapter 3 like any room.",
                "Disconnecting means for IT equipment power AND dedicated HVAC, readily accessible at the exit doors (645.10).",
                "Supply circuits under a raised floor must use the wiring methods listed in 645.5(E).",
            ),
            listOf(
                "Treating any server closet as an 'Article 645 room' to run unprotected cabling.",
                "No disconnect at the principal exit doors where Art. 645 is applied.",
            ),
            sector = "Commercial",
        ),
        NecArticle(
            "700", "Emergency Systems",
            "Life-safety power: egress lighting, exit signs, fire alarm — legally required and held to the strictest transfer and separation rules.",
            listOf(
                "Power must be available within 10 seconds of normal-source failure (700.12).",
                "Emergency circuit wiring kept entirely independent of all other wiring (700.10(B)) — separate raceways, boxes and panels.",
                "Transfer equipment must be automatic and listed for emergency use; test and maintain per 700.3.",
            ),
            listOf(
                "Emergency and normal circuits sharing a raceway or junction box.",
                "Egress lighting on a plain contactor instead of listed automatic transfer equipment.",
            ),
            sector = "Commercial",
        ),
        NecArticle(
            "701", "Legally Required Standby Systems",
            "Code-required but not life-safety loads — smoke removal, communications — with a 60-second transfer window.",
            listOf(
                "Power available within 60 seconds of failure (701.12).",
                "Serves loads legally required by building/fire codes that aren't Art. 700 emergency loads.",
                "Automatic transfer equipment required; wiring MAY share raceways with other wiring (unlike Art. 700).",
            ),
            listOf(
                "Putting true egress/life-safety loads on a 701 system to dodge Art. 700's separation rules.",
            ),
            sector = "Commercial",
        ),

        // ── Industrial ──────────────────────────────────────────────────────────
        NecArticle(
            "430", "Motors, Motor Circuits, and Controllers",
            "The motor article: conductors from the FLC tables, overloads from the nameplate, and a disconnect in sight.",
            listOf(
                "Branch conductors ≥ 125% of the motor full-load current taken from Tables 430.247–430.250 — not the nameplate (430.22, 430.6(A)(1)).",
                "Running overload protection sized from the NAMEPLATE full-load amps (430.32).",
                "Short-circuit/ground-fault protection per Table 430.52 percentages (e.g. inverse-time breaker up to 250% FLC).",
                "Disconnecting means in sight from the motor and controller (430.102(B)).",
                "Adjustable-speed drives: supply conductors at 125% of the DRIVE's rated input current (430.122).",
            ),
            listOf(
                "Sizing conductors from the nameplate instead of the 430.250 table value.",
                "No in-sight disconnect at the motor (and no lockable controller disconnect exception met).",
                "Breaker sized for the conductors nuisance-tripping on motor start instead of using 430.52's allowances.",
            ),
            sector = "Industrial",
        ),
        NecArticle(
            "450", "Transformers and Transformer Vaults",
            "Overcurrent protection per Table 450.3, plus placement, ventilation and vault rules.",
            listOf(
                "OCP for transformers ≤ 1000 V per Table 450.3(B): primary-only at 125%, or the primary/secondary combination the table permits.",
                "Secondary conductors get their own protection rules — see 240.21(C) tap provisions.",
                "Keep ventilation openings clear and maintain the marked clearances (450.9); dry-types ≥ 112.5 kVA need a fire-resistant room or spacing per 450.21(B).",
                "Oil-insulated transformers indoors generally require a vault (450 Part III).",
            ),
            listOf(
                "Missing secondary protection where the primary-only option doesn't cover it.",
                "Transformer boxed in by storage with its ventilation blocked.",
            ),
            sector = "Industrial",
        ),
        NecArticle(
            "500", "Hazardous (Classified) Locations — Overview (Arts. 500–516)",
            "The classification system: Class I gases/vapors, Class II dusts, Class III fibers, each split into Division 1 (normal presence) and Division 2 (abnormal).",
            listOf(
                "Classify the area first — material group and Division drive every equipment and wiring choice (500.5, 500.6).",
                "Equipment must use a protection technique recognized in 500.7 (explosionproof, intrinsic safety, purged/pressurized, etc.) and carry a temperature (T) code below the ignition temperature.",
                "Specific occupancies get their own articles: motor-fuel dispensing 514, spray application 516, aircraft hangars 513, and more (511–516).",
            ),
            listOf(
                "Standard wiring methods carried into a Division 2 boundary.",
                "Equipment with no T-code check against the material's ignition temperature.",
            ),
            sector = "Industrial",
        ),
        NecArticle(
            "501", "Class I Locations — Gases and Vapors",
            "Wiring methods and sealing where flammable gases or vapors may be present.",
            listOf(
                "Division 1 wiring: threaded rigid metal or IMC conduit, or a cable type listed for the location (501.10(A)).",
                "Conduit seals within 18 in of arcing/sparking enclosures and where conduit crosses a classification boundary (501.15).",
                "Flexible connections use fittings listed for the Class/Division (501.10(A)(2)).",
                "Luminaires listed for Class I with the right Division and T-code (501.130).",
            ),
            listOf(
                "EMT or NM cable inside a Class I area.",
                "Seal fittings installed but never poured with compound.",
                "Boundary crossings with no seal at the boundary.",
            ),
            sector = "Industrial",
        ),
        NecArticle(
            "725", "Class 2 and Class 3 Circuits (Controls / Low Voltage)",
            "Power-limited control and signaling wiring — thermostats, sensors, 0–10 V dimming, PLC I/O — and its separation-from-power rules.",
            listOf(
                "Class 2/3 circuits must be supplied by a listed power-limited source; NEC 2023 moved non-power-limited Class 1 to Article 724.",
                "Keep Class 2/3 conductors separated from power conductors — not in the same cable, raceway or enclosure except as 725.136 permits (barriers, 0.25-in spacing rules).",
                "Use listed cable for the space: plenum (CL2P/CL3P), riser (CL2R/CL3R), general (CL2/CL3), PLTC in trays (725.135).",
                "Reclassifying a circuit as Class 1 forfeits all Class 2 wiring permissions.",
            ),
            listOf(
                "Thermostat or 0–10 V dimming wire pulled through the same raceway as branch-circuit power.",
                "Non-plenum cable above a plenum ceiling.",
            ),
            sector = "Industrial",
        ),

        // ── Medical / Health Care ───────────────────────────────────────────────
        NecArticle(
            "517", "Health Care Facilities",
            "Hospitals, clinics and dental offices: redundant grounding in patient care spaces, hospital-grade receptacles, and the essential electrical system's three branches.",
            listOf(
                "Patient care space branch circuits need TWO grounding paths: a metal raceway or listed cable armor qualifying as an EGC PLUS an insulated copper equipment grounding conductor (517.13(A) and (B)).",
                "Patient bed locations: minimum receptacle counts per category (e.g. 8 for general care, 14 for critical care) and hospital-grade listing (517.18, 517.19).",
                "Essential electrical system separates into life safety, critical, and equipment branches with their own transfer switches (517.30–517.35); life safety follows Art. 700 rules.",
                "Wet procedure locations (e.g. ORs unless governed otherwise): GFCI protection or an isolated power system per 517.20.",
                "Category of the patient care space (basic, general, critical, support) is set by the facility's governing body — confirm it before wiring (517.2).",
            ),
            listOf(
                "NM cable or a single ground path in a patient care area (MC cable whose armor is not a listed EGC, with no insulated ground).",
                "Standard-grade receptacles at a patient bed location.",
                "Life-safety branch loads mixed onto the critical branch.",
            ),
            sector = "Medical / Health Care",
        ),
    )

    /** Everything a given tier can see. */
    fun articlesFor(includeElite: Boolean): List<NecArticle> =
        if (includeElite) ARTICLES + ELITE_ARTICLES else ARTICLES

    fun search(query: String, includeElite: Boolean = false): List<NecArticle> {
        val q = query.trim().lowercase()
        val pool = articlesFor(includeElite)
        if (q.isEmpty()) return pool
        return pool.filter { it.search.contains(q) }
    }
}
