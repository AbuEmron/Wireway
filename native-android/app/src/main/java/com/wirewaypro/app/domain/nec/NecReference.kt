package com.wirewaypro.app.domain.nec

/**
 * A residential-focused NEC 2023 reference article. Plain-English summaries and the
 * common violations an inspector flags — ported from the web app's nec-reference.js.
 * Educational reference only; always confirm against the adopted code + local amendments.
 */
data class NecArticle(
    val number: String,   // e.g. "210"
    val title: String,    // e.g. "Branch Circuits"
    val summary: String,
    val rules: List<String>,
    val violations: List<String>,
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

    fun search(query: String): List<NecArticle> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return ARTICLES
        return ARTICLES.filter { it.search.contains(q) }
    }
}
