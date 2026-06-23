// src/data/industrial-nec.js
// ═══ WIREWAY ELITE — NEC 2023 INDUSTRIAL REFERENCE ═══
// Industrial-scope articles in the same shape as the residential reference,
// with plain-English summaries (paraphrased, never verbatim code text).
// Ships dark behind the Elite tier gate; merges into the NEC reference UI
// when Elite mode is wired.

const INDUSTRIAL_NEC = [
  {
    article: "215", title: "Feeders", color: "#e8946a",
    summary: "Covers conductors between the service equipment and the final branch-circuit overcurrent devices — the backbone runs of every industrial distribution system.",
    rules: [
      { code: "215.2", title: "Minimum Rating", text: "Feeder conductors must have ampacity not less than required to supply the load, with continuous loads calculated at 125 percent plus noncontinuous loads at 100 percent." },
      { code: "215.3", title: "Overcurrent Protection", text: "Feeders must be protected against overcurrent per their conductor ampacity, with the same 125 percent continuous-load factor applied to the device rating." },
      { code: "215.12", title: "Identification", text: "Where a premises has more than one nominal voltage system, ungrounded feeder conductors must be identified by phase and system at termination and splice points." },
    ],
    violations: ["Sizing feeder conductors to the breaker instead of the calculated load", "Mixing voltage systems without phase identification"],
  },
  {
    article: "225", title: "Outside Branch Circuits and Feeders", color: "#e8946a",
    summary: "Rules for circuits run outdoors between buildings and structures on the same property — yard distribution, building-to-building feeders, and outdoor equipment supply.",
    rules: [
      { code: "225.18", title: "Clearances", text: "Overhead spans must maintain minimum clearances above grade: generally 10 ft at the lowest point, 12 ft over residential drives, and 18 ft over areas subject to truck traffic." },
      { code: "225.30", title: "Number of Supplies", text: "A building or structure is generally limited to one feeder or branch circuit supply unless special conditions such as capacity, different uses, or documented needs apply." },
      { code: "225.32", title: "Disconnect Location", text: "A disconnecting means must be installed at a readily accessible location nearest the point where the supply conductors enter each building or structure." },
    ],
    violations: ["No disconnect at the second building", "Overhead spans below required clearance over traffic areas"],
  },
  {
    article: "242", title: "Overvoltage (Surge) Protection", color: "#e8946a",
    summary: "Requirements for surge protective devices (SPDs) and surge arresters that protect equipment from transient overvoltages — increasingly required on services and critical gear.",
    rules: [
      { code: "242.12", title: "Listing and Location", text: "SPDs must be listed and may be installed at the service, feeder, or branch level (Type 1, 2, or 3) according to their listing." },
      { code: "242.24", title: "Connection Length", text: "SPD conductors should be as short and straight as practicable — excess lead length dramatically reduces protective performance." },
    ],
    violations: ["Long, looped SPD leads that defeat the device's clamping performance"],
  },
  {
    article: "342", title: "Intermediate Metal Conduit (IMC)", color: "#7eb8e8",
    summary: "Steel raceway lighter than rigid but with comparable physical protection — permitted in all atmospheric conditions and occupancies, threaded like RMC.",
    rules: [
      { code: "342.22", title: "Conductor Fill", text: "The number and size of conductors must not exceed the percentage fill of Chapter 9, Table 1 — generally 40 percent for three or more conductors." },
      { code: "342.30", title: "Securing and Supporting", text: "IMC must be secured within 3 ft of every box or termination and supported at intervals not exceeding 10 ft." },
    ],
    violations: ["Exceeding 10 ft support spacing on long runs"],
  },
  {
    article: "344", title: "Rigid Metal Conduit (RMC)", color: "#7eb8e8",
    summary: "The heaviest-duty raceway — threaded galvanized steel for maximum physical protection, standard in plants, exterior work, and hazardous locations.",
    rules: [
      { code: "344.14", title: "Dissimilar Metals", text: "Where practicable, contact with dissimilar metals must be avoided to prevent galvanic corrosion; approved fittings mitigate the issue." },
      { code: "344.28", title: "Reaming and Threading", text: "Cut ends must be reamed to remove rough edges, and field-cut threads must use a standard 3/4-inch-per-foot taper die." },
      { code: "344.30", title: "Securing and Supporting", text: "RMC must be secured within 3 ft of each box or termination and supported per the spacing table — up to 20 ft for larger trade sizes with threaded couplings." },
    ],
    violations: ["Unreamed field cuts damaging conductor insulation during pulls", "Running threads used where structurally inadequate"],
  },
  {
    article: "352", title: "Rigid PVC Conduit", color: "#7eb8e8",
    summary: "Nonmetallic raceway for corrosive environments, underground duct banks, and wet locations — requires expansion fittings and a wire-type equipment grounding conductor.",
    rules: [
      { code: "352.44", title: "Expansion Fittings", text: "Expansion fittings are required where thermal expansion or contraction is expected to exceed 1/4 inch in a run — critical on long exposed exterior runs." },
      { code: "352.60", title: "Grounding Conductor", text: "Because PVC is nonconductive, a separate equipment grounding conductor must be installed within the raceway where required." },
    ],
    violations: ["Long exterior PVC runs with no expansion fittings — buckled or pulled-apart conduit", "Relying on PVC raceway as a ground path"],
  },
  {
    article: "368", title: "Busways", color: "#7eb8e8",
    summary: "Factory-assembled bus duct distributing large blocks of power along a structure — common down plant aisles with plug-in units tapping power where needed.",
    rules: [
      { code: "368.17", title: "Overcurrent Protection", text: "Busways must be protected per their rating; plug-in devices used as branch taps generally require overcurrent protection at the busway tap." },
      { code: "368.30", title: "Support", text: "Busways must be securely supported at intervals not exceeding 5 ft unless designed and marked otherwise." },
    ],
    violations: ["Unprotected taps from plug-in openings", "Spanning structural gaps beyond listed support intervals"],
  },
  {
    article: "392", title: "Cable Trays", color: "#7eb8e8",
    summary: "Support systems — ladder, trough, and channel tray — that carry multiconductor cables across industrial facilities. The tray is a support system, not a raceway.",
    rules: [
      { code: "392.22", title: "Fill Calculations", text: "Allowable cable fill depends on tray type and cable size; multiconductor cables over 4/0 must be laid in a single layer in ladder tray." },
      { code: "392.60", title: "Grounding", text: "Metal cable trays may serve as equipment grounding conductors only where the tray and fittings are identified for grounding and sections are bonded." },
      { code: "392.18", title: "Cable Tray Installation", text: "Trays must be installed as a complete system before cables are pulled, with supports preventing stress at connection points." },
    ],
    violations: ["Stacked large cables in ladder tray beyond fill rules", "Unbonded tray sections used as the ground path"],
  },
  {
    article: "409", title: "Industrial Control Panels", color: "#6ede96",
    summary: "Requirements for assembled control panels operating at 1000V or less — short-circuit current ratings, markings, and overcurrent protection for the panel as a unit.",
    rules: [
      { code: "409.22", title: "Short-Circuit Current Rating", text: "An industrial control panel must not be installed where available fault current exceeds its marked SCCR — a frequently missed and dangerous oversight." },
      { code: "409.110", title: "Marking", text: "Panels must be marked with voltage, phase, frequency, full-load current, SCCR, and the identity of the responsible organization." },
    ],
    violations: ["Installing a 5kA SCCR panel on a system with 35kA available fault current"],
  },
  {
    article: "430", title: "Motors, Motor Circuits, and Controllers", color: "#6ede96",
    summary: "The biggest article in industrial work — conductor sizing, overload protection, branch-circuit protection, controllers, and disconnects for every motor installation.",
    rules: [
      { code: "430.6", title: "Table FLC Values", text: "Conductor and protection sizing uses the full-load current tables in the article — not the motor nameplate — except for overload protection, which uses nameplate current." },
      { code: "430.22", title: "Single Motor Conductors", text: "Branch-circuit conductors supplying a single motor must have ampacity of at least 125 percent of the motor's table full-load current." },
      { code: "430.24", title: "Several Motors", text: "Conductors supplying multiple motors are sized at 125 percent of the largest motor plus the sum of the other motors' full-load currents plus other loads." },
      { code: "430.32", title: "Overload Protection", text: "Continuous-duty motors over 1 HP require overload protection generally sized at 115 to 125 percent of nameplate current depending on motor characteristics." },
      { code: "430.102", title: "Disconnect Location", text: "A disconnecting means must be in sight from the controller, and in sight from the motor unless specific lockout provisions apply." },
    ],
    violations: ["Sizing everything from the nameplate instead of the tables", "Motor with no in-sight disconnect and no lockable disconnect provision"],
  },
  {
    article: "440", title: "Air-Conditioning and Refrigeration Equipment", color: "#6ede96",
    summary: "Hermetic compressor equipment follows its own rules — sizing comes from the nameplate's minimum circuit ampacity and maximum overcurrent protection values.",
    rules: [
      { code: "440.4", title: "Nameplate Data", text: "Equipment nameplates state minimum circuit ampacity (MCA) and maximum overcurrent protection (MOCP) — the installer's sizing instructions." },
      { code: "440.14", title: "Disconnect Location", text: "The disconnecting means must be readily accessible and within sight of the equipment." },
    ],
    violations: ["Breaker larger than nameplate MOCP", "RTU disconnect not within sight"],
  },
  {
    article: "445", title: "Generators", color: "#e87e7e",
    summary: "Installation rules for on-site generators — conductor ampacity, overcurrent protection, and disconnection requirements for standby and prime power machines.",
    rules: [
      { code: "445.13", title: "Conductor Ampacity", text: "Conductors from the generator output terminals must have ampacity of at least 115 percent of the nameplate current rating, with limited exceptions." },
      { code: "445.18", title: "Disconnect and Shutdown", text: "Generators require a disconnecting means and, for larger units, an emergency shutdown capability; one-family dwellings have specific remote shutdown rules." },
    ],
    violations: ["Tap conductors below 115 percent of nameplate", "No emergency shutdown provision"],
  },
  {
    article: "450", title: "Transformers", color: "#e8946a",
    summary: "Overcurrent protection, ventilation, and clearance requirements for transformers — the heart of every voltage-change point in a facility.",
    rules: [
      { code: "450.3", title: "Overcurrent Protection", text: "Protection is set by table percentages of rated current — primary-only protection or coordinated primary and secondary protection, depending on configuration and supervision." },
      { code: "450.9", title: "Ventilation", text: "Ventilation openings must remain clear; transformers must be installed so heat is carried away without exceeding temperature ratings." },
      { code: "450.13", title: "Accessibility", text: "Transformers must be readily accessible, with specific allowances for dry-types above hung ceilings and on open walls under size limits." },
    ],
    violations: ["Storage stacked against transformer ventilation openings", "Dry-type above a ceiling exceeding the size allowance"],
  },
  {
    article: "480", title: "Stationary Standby Batteries", color: "#dce0e8",
    summary: "Battery systems for UPS and standby power — ventilation, working space, and disconnection rules for stationary installations.",
    rules: [
      { code: "480.7", title: "Disconnecting Means", text: "A disconnecting means is required for battery circuits over 60 volts, readily accessible and within sight of the battery system." },
      { code: "480.10", title: "Battery Locations", text: "Installations require ventilation sufficient to prevent accumulation of explosive gas mixtures and adequate working space per 110.26." },
    ],
    violations: ["Battery rooms with no gas ventilation provision", "Blocked working space in front of battery racks"],
  },
  {
    article: "490", title: "Equipment Over 1000 Volts AC", color: "#e87e7e",
    summary: "General requirements for medium-voltage equipment — enclosures, working space, and qualified-person access rules above 1000V nominal.",
    rules: [
      { code: "490.21", title: "Circuit Interrupting Devices", text: "Switching devices must be rated for the duty, and load-interrupting capability must match the application — fused cutouts, interrupter switches, and breakers each have limits." },
      { code: "490.24", title: "Minimum Space Separation", text: "Minimum air separation distances between bare live parts and ground increase with voltage class and must be maintained in field-fabricated installations." },
    ],
    violations: ["Unqualified personnel access to MV compartments", "Field modifications reducing required clearances"],
  },
  {
    article: "500", title: "Hazardous (Classified) Locations", color: "#e87e7e",
    summary: "The classification framework — Class I (gases/vapors), Class II (combustible dust), Class III (fibers), each with Division 1 and 2 — determining everything about equipment and wiring methods in classified areas.",
    rules: [
      { code: "500.5", title: "Classifications", text: "Areas are classified by the type of hazardous material and the likelihood of its presence — Division 1 where hazards exist in normal operation, Division 2 where only under abnormal conditions." },
      { code: "500.8", title: "Equipment", text: "Equipment must be identified for the class, division, and group of the location, with temperature ratings below the ignition temperature of the material present." },
    ],
    violations: ["Standard equipment installed in classified areas", "Equipment group or temperature class not matched to the hazard"],
  },
  {
    article: "501", title: "Class I Locations", color: "#e87e7e",
    summary: "Wiring methods and sealing for flammable gas and vapor areas — threaded rigid conduit, explosionproof enclosures, and conduit seals that stop flame propagation.",
    rules: [
      { code: "501.10", title: "Wiring Methods", text: "Division 1 generally requires threaded RMC or IMC with explosionproof fittings; Division 2 permits additional methods including certain cable types." },
      { code: "501.15", title: "Sealing", text: "Conduit seals are required within 18 inches of explosionproof enclosures containing arcing devices, and where conduit leaves the classified area — poured seals block gas migration and flame propagation." },
    ],
    violations: ["Missing or unpoured seal fittings", "Standard flexible connections in Division 1 areas"],
  },
  {
    article: "590", title: "Temporary Installations", color: "#dce0e8",
    summary: "Power for construction, maintenance, and demolition — relaxed wiring methods balanced by strict GFCI protection requirements for personnel.",
    rules: [
      { code: "590.4", title: "General Requirements", text: "Temporary feeders and branch circuits may use cable assemblies and flexible cords, with physical protection and support requirements; receptacles must not be on lighting circuits." },
      { code: "590.6", title: "GFCI Protection", text: "All 125V and 125/250V receptacles 60A or less used for temporary power during construction must have GFCI protection for personnel." },
    ],
    violations: ["Temp receptacles without GFCI on a job site", "Lights and receptacles on the same temporary circuit"],
  },
  {
    article: "610", title: "Cranes and Hoists", color: "#6ede96",
    summary: "Contact conductors, festoon cables, runway bus systems, and disconnects for overhead cranes, monorails, and hoists in industrial buildings.",
    rules: [
      { code: "610.31", title: "Runway Disconnect", text: "A disconnecting means with provisions for locking must be provided between the runway contact conductors and the power supply, readily accessible from the floor." },
      { code: "610.41", title: "Overcurrent Protection", text: "Crane and hoist circuits require overcurrent protection sized for the duty-cycle nature of the loads, with demand factors permitted for multiple motors." },
    ],
    violations: ["No lockable runway disconnect reachable from floor level", "Damaged festoon systems left in service"],
  },
  {
    article: "630", title: "Electric Welders", color: "#6ede96",
    summary: "Arc and resistance welders draw heavy intermittent current — the Code permits conductor and protection sizing based on duty cycle rather than nameplate primary current alone.",
    rules: [
      { code: "630.11", title: "Conductor Sizing", text: "Supply conductors for arc welders are sized from the rated primary current multiplied by a duty-cycle factor, or per the welder's marked effective current." },
      { code: "630.12", title: "Overcurrent Protection", text: "Welder overcurrent protection is permitted at up to 200 percent of rated primary current — the intermittent load justifies the larger device." },
    ],
    violations: ["Treating a welder receptacle like a continuous load and oversizing everything", "Daisy-chained welder circuits beyond conductor capacity"],
  },
  {
    article: "670", title: "Industrial Machinery", color: "#6ede96",
    summary: "Supply circuit requirements for machine tools and industrial machines built to NFPA 79 — the nameplate governs, and a single supply disconnect is the rule.",
    rules: [
      { code: "670.3", title: "Nameplate Data", text: "Machine nameplates must show full-load current, largest motor load, short-circuit rating, and required overcurrent protection — the installer sizes the supply from this data." },
      { code: "670.4", title: "Supply Conductors", text: "Supply conductors must have ampacity not less than 125 percent of the full-load current of all resistance loads plus motor loads per the nameplate." },
    ],
    violations: ["Supplying a machine above its marked short-circuit current rating"],
  },
  {
    article: "695", title: "Fire Pumps", color: "#e87e7e",
    summary: "Fire pump power must survive a fire — the philosophy reverses normal practice: protect against short circuit only, never overload, so the pump runs until it can't.",
    rules: [
      { code: "695.4", title: "Continuity of Power", text: "Supply conductors must be dedicated and protected, sized to carry locked-rotor current indefinitely — overcurrent devices must not open under fire pump load conditions." },
      { code: "695.6", title: "Power Wiring", text: "Fire pump supply conductors require independent routing and physical protection — encased in concrete or protected by listed fire-rated assemblies where they pass through the building." },
    ],
    violations: ["Overload protection on a fire pump motor circuit", "Fire pump feeder routed unprotected through the building"],
  },
  {
    article: "700", title: "Emergency Systems", color: "#e87e7e",
    summary: "Life-safety power — egress lighting and systems essential to human life during normal-supply failure, with strict transfer time, separation, and testing requirements.",
    rules: [
      { code: "700.4", title: "Capacity and Testing", text: "Emergency systems must have adequate capacity for all loads operating simultaneously and require witnessed acceptance testing plus documented periodic testing." },
      { code: "700.10", title: "Wiring Independence", text: "Emergency circuit wiring must be kept entirely independent of all other wiring — separate raceways, boxes, and enclosures with narrow exceptions." },
      { code: "700.12", title: "Transfer Time", text: "Emergency power must be available within 10 seconds of normal supply failure." },
    ],
    violations: ["Emergency and normal circuits sharing a raceway", "Transfer arrangements that cannot meet the 10-second requirement"],
  },
  {
    article: "701", title: "Legally Required Standby Systems", color: "#e87e7e",
    summary: "Systems required by code but not directly life-safety — smoke control, sewage, and similar loads with a 60-second transfer allowance and lighter separation rules than Article 700.",
    rules: [
      { code: "701.12", title: "Transfer Time", text: "Legally required standby power must be available within 60 seconds of supply failure." },
    ],
    violations: ["Mixing legally required standby loads onto the emergency (700) system without capacity justification"],
  },
  {
    article: "702", title: "Optional Standby Systems", color: "#dce0e8",
    summary: "Owner-choice backup power — data rooms, process loads, comfort — with capacity selection rules and signage but no transfer-time mandate.",
    rules: [
      { code: "702.4", title: "Capacity", text: "With automatic transfer, the system must either carry the full load transferred or use an approved load-management system that prevents overload." },
      { code: "702.7", title: "Signage", text: "A sign at the service entrance must indicate the type and location of each on-site optional standby power source." },
    ],
    violations: ["Automatic transfer of more load than the generator can carry with no load management"],
  },
  {
    article: "705", title: "Interconnected Electric Power Production Sources", color: "#dce0e8",
    summary: "Connecting generators, solar, and storage in parallel with the utility — point-of-connection rules, busbar limits, and disconnecting requirements.",
    rules: [
      { code: "705.12", title: "Load-Side Connections", text: "Load-side source connections must respect busbar ratings — the sum of supply breakers is limited, commonly via the 120 percent rule for solar on panelboards." },
      { code: "705.20", title: "Disconnecting Means", text: "Each power source requires a readily accessible disconnecting means rated for the duty." },
    ],
    violations: ["Backfeed breakers exceeding busbar interconnection limits", "No accessible AC disconnect for the interconnected source"],
  },
  {
    article: "760", title: "Fire Alarm Systems", color: "#e87e7e",
    summary: "Power-limited and non-power-limited fire alarm circuit wiring — cable types, separation from power conductors, and survivability for circuits that must work during a fire.",
    rules: [
      { code: "760.46", title: "NPLFA Wiring", text: "Non-power-limited fire alarm circuits use Chapter 3 wiring methods with specific conductor and overcurrent provisions." },
      { code: "760.136", title: "Separation", text: "Power-limited fire alarm conductors must be separated from electric light and power conductors — generally 2 inches unless barriers or listed methods apply." },
    ],
    violations: ["FPL cable bundled with branch-circuit wiring", "Wrong cable substitution in plenums or risers"],
  },
];

export default INDUSTRIAL_NEC;
