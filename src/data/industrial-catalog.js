// src/data/industrial-catalog.js
// ═══ WIREWAY ELITE — INDUSTRIAL ESTIMATING ENGINE ═══
// Industrial work prices by unit of measure: raceway and conductors by the foot,
// terminations each, motors by horsepower, gear by the section. Labor hours are
// independently derived industry-typical units (NECA-style structure, original
// numbers) and get adjusted by job-condition multipliers — the way real
// industrial estimators think. Baseline material prices; the AI layer
// live-checks big-ticket items at quote time.

// ── JOB CONDITIONS (labor multipliers — stack multiplicatively) ──
export const CONDITIONS = [
  { id: "height_10",  label: "Work at 10–20 ft (ladders)",      factor: 1.15 },
  { id: "height_20",  label: "Over 20 ft (lift work)",          factor: 1.30 },
  { id: "occupied",   label: "Occupied / operating facility",   factor: 1.15 },
  { id: "demo",       label: "Demo or rework in path",          factor: 1.25 },
  { id: "underground",label: "Trench / under slab",             factor: 1.20 },
  { id: "mechroom",   label: "Congested mechanical space",      factor: 1.10 },
  { id: "offhours",   label: "Night / weekend shift",           factor: 1.35 },
  { id: "shutdown",   label: "Shutdown / outage window",        factor: 1.40 },
  { id: "remote",     label: "Remote site / long mobilization", factor: 1.20 },
  { id: "clearance",  label: "Security clearance facility",     factor: 1.15 },
  { id: "winter",     label: "Winter / weather exposure",       factor: 1.15 },
];

export function conditionFactor(conditionIds = []) {
  return CONDITIONS.filter(c => conditionIds.includes(c.id))
    .reduce((f, c) => f * c.factor, 1);
}

// Line pricing: qty units × (material + laborHrs × rate × conditions)
export function computeIndustrialLine({ mat, hrs }, qty, rate, conditionIds = []) {
  const factor = conditionFactor(conditionIds);
  const labHrs = hrs * qty * factor;
  return {
    matTotal: mat * qty,
    laborHrs: labHrs,
    labTotal: labHrs * rate,
    total: mat * qty + labHrs * rate,
  };
}

// ── THE INDUSTRIAL CATALOG ──
// Shape mirrors the residential catalog: categories → items → variants.
// unit: "ft" = per foot, "ea" = each, "term" = per termination.
export const INDUSTRIAL_CATEGORIES = [
  {
    id: "raceway", label: "Raceway & Conduit", icon: "═", color: "#7eb8e8",
    items: [
      { id: "emt_run", label: "EMT Run (installed)", unit: "ft", nec: "NEC 358",
        note: "Includes straps, couplings, connectors allowance",
        variants: [
          { label: '1/2"',   mat: 1.15,  hrs: 0.032 },
          { label: '3/4"',   mat: 1.50,  hrs: 0.036 },
          { label: '1"',     mat: 2.20,  hrs: 0.042 },
          { label: '1-1/4"', mat: 3.30,  hrs: 0.050 },
          { label: '1-1/2"', mat: 4.00,  hrs: 0.055 },
          { label: '2"',     mat: 5.40,  hrs: 0.065 },
          { label: '2-1/2"', mat: 8.80,  hrs: 0.085 },
          { label: '3"',     mat: 11.50, hrs: 0.100 },
          { label: '4"',     mat: 15.80, hrs: 0.130 },
        ]},
      { id: "rmc_run", label: "Rigid (RMC) Run", unit: "ft", nec: "NEC 344",
        note: "Threaded, includes fittings allowance",
        variants: [
          { label: '3/4"',   mat: 4.20,  hrs: 0.058 },
          { label: '1"',     mat: 5.80,  hrs: 0.068 },
          { label: '1-1/2"', mat: 9.60,  hrs: 0.088 },
          { label: '2"',     mat: 12.40, hrs: 0.105 },
          { label: '3"',     mat: 24.00, hrs: 0.160 },
          { label: '4"',     mat: 33.00, hrs: 0.210 },
        ]},
      { id: "pvc_run", label: "PVC Sch 40 Run", unit: "ft", nec: "NEC 352",
        variants: [
          { label: '3/4"', mat: 0.95, hrs: 0.030 },
          { label: '1"',   mat: 1.30, hrs: 0.035 },
          { label: '2"',   mat: 2.60, hrs: 0.052 },
          { label: '3"',   mat: 4.80, hrs: 0.080 },
          { label: '4"',   mat: 6.50, hrs: 0.100 },
        ]},
      { id: "trapeze", label: "Trapeze / Strut Rack", unit: "ft", nec: "NEC 300.11",
        variants: [ { label: "Standard", mat: 7.00, hrs: 0.080 } ]},
    ],
  },
  {
    id: "conductors", label: "Conductors & Pulls", icon: "≋", color: "#e8946a",
    items: [
      { id: "thhn_pull", label: "THHN Cu (per conductor)", unit: "ft", nec: "NEC 310.16",
        note: "Price per foot per conductor — multiply qty by run length × conductor count",
        variants: [
          { label: "#12",    mat: 0.24,  hrs: 0.0040 },
          { label: "#10",    mat: 0.38,  hrs: 0.0045 },
          { label: "#8",     mat: 0.60,  hrs: 0.0050 },
          { label: "#6",     mat: 0.92,  hrs: 0.0060 },
          { label: "#4",     mat: 1.40,  hrs: 0.0075 },
          { label: "#2",     mat: 2.20,  hrs: 0.0090 },
          { label: "1/0",    mat: 3.50,  hrs: 0.0110 },
          { label: "2/0",    mat: 4.30,  hrs: 0.0120 },
          { label: "4/0",    mat: 6.90,  hrs: 0.0150 },
          { label: "250MCM", mat: 8.20,  hrs: 0.0170 },
          { label: "350MCM", mat: 11.20, hrs: 0.0200 },
          { label: "500MCM", mat: 15.80, hrs: 0.0240 },
        ]},
      { id: "termination", label: "Termination (lug/land)", unit: "term", nec: "NEC 110.14",
        variants: [
          { label: "#6–#2",      mat: 13.00, hrs: 0.25 },
          { label: "1/0–4/0",    mat: 20.00, hrs: 0.45 },
          { label: "250–500MCM", mat: 34.00, hrs: 0.80 },
        ]},
      { id: "mc_cable", label: "MC Cable Run", unit: "ft", nec: "NEC 330",
        variants: [
          { label: "12/2", mat: 1.60, hrs: 0.022 },
          { label: "12/3", mat: 2.10, hrs: 0.024 },
          { label: "10/3", mat: 3.00, hrs: 0.027 },
          { label: "8/3",  mat: 4.60, hrs: 0.032 },
        ]},
    ],
  },
  {
    id: "motors", label: "Motor Circuits & Controls", icon: "◉", color: "#6ede96",
    items: [
      { id: "motor_conn", label: "Motor Connection (480V 3Ø)", unit: "ea", nec: "NEC 430",
        note: "Final connection, flex, peckerhead — feeder run priced separately",
        variants: [
          { label: "1–5 HP",    mat: 90.00,  hrs: 3.0 },
          { label: "7.5–15 HP", mat: 150.00, hrs: 4.5 },
          { label: "20–30 HP",  mat: 230.00, hrs: 6.0 },
          { label: "40–60 HP",  mat: 400.00, hrs: 8.0 },
          { label: "75–100 HP", mat: 580.00, hrs: 10.0 },
        ]},
      { id: "disconnect", label: "HD Safety Disconnect", unit: "ea", nec: "NEC 430.102",
        variants: [
          { label: "30A NF",  mat: 95.00,   hrs: 1.5 },
          { label: "60A NF",  mat: 160.00,  hrs: 1.8 },
          { label: "60A F",   mat: 240.00,  hrs: 1.8 },
          { label: "100A F",  mat: 420.00,  hrs: 2.5 },
          { label: "200A F",  mat: 850.00,  hrs: 3.5 },
        ]},
      { id: "starter", label: "Starter / MCC Bucket Install", unit: "ea", nec: "NEC 430.83",
        variants: [
          { label: "Size 1", mat: 310.00,  hrs: 4.0 },
          { label: "Size 2", mat: 520.00,  hrs: 5.0 },
          { label: "Size 3", mat: 900.00,  hrs: 6.5 },
        ]},
      { id: "vfd", label: "VFD Install & Program", unit: "ea", nec: "NEC 430 Part X",
        variants: [
          { label: "≤10 HP",  mat: 850.00,  hrs: 6.0 },
          { label: "15–30 HP", mat: 1900.00, hrs: 9.0 },
          { label: "40–75 HP", mat: 3800.00, hrs: 14.0 },
        ]},
    ],
  },
  {
    id: "distribution", label: "Distribution & Gear", icon: "▣", color: "#e8c97a",
    items: [
      { id: "panelboard", label: "Panelboard Set & Connect", unit: "ea", nec: "NEC 408",
        note: "Set, level, interior, breakers landed — feeder priced separately",
        variants: [
          { label: "100A 208V 24ckt", mat: 950.00,  hrs: 8.0 },
          { label: "225A 208V 42ckt", mat: 1700.00, hrs: 12.0 },
          { label: "400A 480V 42ckt", mat: 2600.00, hrs: 16.0 },
        ]},
      { id: "xfmr", label: "Dry-Type Transformer Set", unit: "ea", nec: "NEC 450",
        note: "Set, vibration pads, primary/secondary connect — feeders separate",
        variants: [
          { label: "15 kVA",    mat: 1000.00,  hrs: 8.0 },
          { label: "30 kVA",    mat: 1500.00,  hrs: 10.0 },
          { label: "45 kVA",    mat: 2000.00,  hrs: 12.0 },
          { label: "75 kVA",    mat: 3000.00,  hrs: 16.0 },
          { label: "112.5 kVA", mat: 4400.00,  hrs: 20.0 },
          { label: "150 kVA",   mat: 5400.00,  hrs: 24.0 },
          { label: "225 kVA",   mat: 8000.00,  hrs: 30.0 },
        ]},
      { id: "switchboard", label: "Switchboard Section Set", unit: "ea", nec: "NEC 408",
        variants: [ { label: "Per section", mat: 2100.00, hrs: 20.0 } ]},
      { id: "mcc_section", label: "MCC Section Set", unit: "ea", nec: "NEC 430.98",
        variants: [ { label: "Per section", mat: 2700.00, hrs: 24.0 } ]},
    ],
  },
  {
    id: "grounding", label: "Grounding & Bonding", icon: "⏚", color: "#dce0e8",
    items: [
      { id: "ground_ring", label: "Ground Ring (#4/0 bare Cu)", unit: "ft", nec: "NEC 250.52",
        variants: [ { label: "Direct buried", mat: 7.40, hrs: 0.045 } ]},
      { id: "ground_rod", label: "Ground Rod Driven", unit: "ea", nec: "NEC 250.53",
        variants: [ { label: '5/8" × 8ft', mat: 28.00, hrs: 0.75 } ]},
      { id: "cadweld", label: "Exothermic Weld", unit: "ea", nec: "NEC 250.70",
        variants: [ { label: "Standard", mat: 22.00, hrs: 0.60 } ]},
      { id: "bond_jumper", label: "Bonding Jumper Install", unit: "ea", nec: "NEC 250.102",
        variants: [ { label: "Standard", mat: 18.00, hrs: 0.50 } ]},
    ],
  },
  {
    id: "traybus", label: "Cable Tray & Busway", icon: "≡", color: "#7eb8e8",
    items: [
      { id: "ladder_tray", label: "Ladder Tray Run", unit: "ft", nec: "NEC 392",
        note: "Includes supports and hardware allowance",
        variants: [
          { label: '12" wide', mat: 14.00, hrs: 0.090 },
          { label: '18" wide', mat: 18.00, hrs: 0.100 },
          { label: '24" wide', mat: 23.00, hrs: 0.115 },
        ]},
      { id: "tray_fitting", label: "Tray Fitting (elbow/tee/cross)", unit: "ea", nec: "NEC 392.18",
        variants: [
          { label: '12"', mat: 85.00,  hrs: 0.80 },
          { label: '24"', mat: 140.00, hrs: 1.10 },
        ]},
      { id: "tray_cable", label: "Tray Cable TC-ER (per conductor bundle)", unit: "ft", nec: "NEC 336",
        variants: [
          { label: "12/4", mat: 2.40, hrs: 0.014 },
          { label: "8/4",  mat: 5.20, hrs: 0.018 },
          { label: "4/4",  mat: 9.80, hrs: 0.024 },
        ]},
      { id: "busway_run", label: "Busway / Bus Duct Run", unit: "ft", nec: "NEC 368",
        note: "Hangers and joints included",
        variants: [
          { label: "225A",  mat: 95.00,  hrs: 0.35 },
          { label: "400A",  mat: 140.00, hrs: 0.40 },
          { label: "800A",  mat: 260.00, hrs: 0.55 },
        ]},
      { id: "bus_plug", label: "Bus Plug Unit Install", unit: "ea", nec: "NEC 368.17",
        variants: [
          { label: "30–60A",   mat: 480.00,  hrs: 2.0 },
          { label: "100–200A", mat: 1300.00, hrs: 3.5 },
        ]},
    ],
  },
  {
    id: "lighting_ind", label: "Industrial Lighting", icon: "☀", color: "#e8c97a",
    items: [
      { id: "highbay", label: "LED High-Bay Fixture", unit: "ea", nec: "NEC 410",
        note: "Hung, whipped, and connected — circuit run priced separately",
        variants: [
          { label: "150W",         mat: 210.00, hrs: 1.2 },
          { label: "240W",         mat: 320.00, hrs: 1.4 },
          { label: "240W w/ sensor", mat: 390.00, hrs: 1.6 },
        ]},
      { id: "strip_fix", label: "LED Strip / Vapor-Tight", unit: "ea", nec: "NEC 410",
        variants: [
          { label: "4 ft", mat: 85.00,  hrs: 0.8 },
          { label: "8 ft", mat: 140.00, hrs: 1.0 },
        ]},
      { id: "pole_light", label: "Site Pole Light Set", unit: "ea", nec: "NEC 410.30",
        note: "Pole set on existing base, fixture, fusing, terminations",
        variants: [
          { label: "20 ft single head", mat: 2400.00, hrs: 8.0 },
          { label: "25 ft double head", mat: 3600.00, hrs: 10.0 },
        ]},
      { id: "egress_light", label: "Emergency / Exit Fixture", unit: "ea", nec: "NEC 700.16",
        variants: [
          { label: "Exit combo", mat: 95.00,  hrs: 0.8 },
          { label: "Remote head", mat: 60.00, hrs: 0.6 },
        ]},
      { id: "ltg_contactor", label: "Lighting Contactor Install", unit: "ea", nec: "NEC 404",
        variants: [
          { label: "30A mech held",  mat: 280.00, hrs: 2.5 },
          { label: "60A elec held",  mat: 460.00, hrs: 3.0 },
        ]},
      { id: "occ_sensor", label: "Occupancy / Daylight Sensor", unit: "ea", nec: "NEC 404.2",
        variants: [ { label: "Line voltage", mat: 75.00, hrs: 0.7 } ]},
    ],
  },
  {
    id: "power_dev", label: "Power Devices, Welding & Cranes", icon: "⚒", color: "#e8946a",
    items: [
      { id: "weld_recep", label: "Welding Receptacle Circuit", unit: "ea", nec: "NEC 630",
        note: "Receptacle, box, and connection — feeder run priced separately",
        variants: [
          { label: "50A 480V", mat: 180.00, hrs: 2.0 },
          { label: "60A 480V", mat: 240.00, hrs: 2.2 },
        ]},
      { id: "twistlock", label: "Twist-Lock Receptacle", unit: "ea", nec: "NEC 406",
        variants: [
          { label: "20A 480V", mat: 70.00,  hrs: 1.0 },
          { label: "30A 480V", mat: 110.00, hrs: 1.2 },
        ]},
      { id: "cord_drop", label: "Cord Drop / Pendant", unit: "ea", nec: "NEC 400",
        variants: [
          { label: "Reel w/ outlet", mat: 320.00, hrs: 2.0 },
          { label: "SO cord pendant", mat: 130.00, hrs: 1.5 },
        ]},
      { id: "crane_bus", label: "Crane Conductor Bar", unit: "ft", nec: "NEC 610",
        variants: [ { label: "4-bar 100A", mat: 28.00, hrs: 0.14 } ]},
      { id: "equip_conn", label: "Equipment Connection (oven/kiln/process)", unit: "ea", nec: "NEC 422",
        variants: [
          { label: "≤60A",     mat: 160.00, hrs: 3.0 },
          { label: "61–200A",  mat: 380.00, hrs: 5.0 },
        ]},
    ],
  },
  {
    id: "controls", label: "Controls & Instrumentation", icon: "⌁", color: "#6ede96",
    items: [
      { id: "ctrl_wire", label: "Control Wiring (per conductor)", unit: "ft", nec: "NEC 724",
        variants: [
          { label: "#14 MTW",     mat: 0.20, hrs: 0.0050 },
          { label: "18/2 shielded", mat: 0.55, hrs: 0.0060 },
        ]},
      { id: "ctrl_panel", label: "Control Panel Set & Wire", unit: "ea", nec: "NEC 409",
        variants: [
          { label: 'Small (≤24")', mat: 450.00,  hrs: 6.0 },
          { label: 'Medium (36")', mat: 900.00,  hrs: 10.0 },
          { label: 'Large (60")',  mat: 1800.00, hrs: 16.0 },
        ]},
      { id: "io_term", label: "PLC I/O Termination", unit: "term", nec: "NEC 409",
        variants: [ { label: "Per point", mat: 3.50, hrs: 0.20 } ]},
      { id: "pb_station", label: "Pushbutton / Operator Station", unit: "ea", nec: "NEC 430.83",
        variants: [
          { label: "Start-stop", mat: 90.00,  hrs: 1.2 },
          { label: "E-stop",     mat: 110.00, hrs: 1.2 },
        ]},
      { id: "field_device", label: "Field Device Mount & Wire", unit: "ea", nec: "NEC 724",
        note: "Photo eye, limit switch, prox, float — device by others unless noted",
        variants: [ { label: "Standard", mat: 45.00, hrs: 1.0 } ]},
    ],
  },
  {
    id: "hazloc", label: "Hazardous Locations", icon: "☢", color: "#e87e7e",
    items: [
      { id: "sealoff", label: "Conduit Seal-Off (poured)", unit: "ea", nec: "NEC 501.15",
        variants: [
          { label: '3/4"', mat: 38.00, hrs: 0.9 },
          { label: '1"',   mat: 48.00, hrs: 1.0 },
          { label: '2"',   mat: 95.00, hrs: 1.4 },
        ]},
      { id: "xp_fixture", label: "Explosionproof Fixture", unit: "ea", nec: "NEC 501.130",
        variants: [ { label: "LED Class I Div 1", mat: 650.00, hrs: 2.5 } ]},
      { id: "xp_device", label: "XP Device (switch/receptacle)", unit: "ea", nec: "NEC 501.105",
        variants: [ { label: "Class I Div 1", mat: 320.00, hrs: 1.8 } ]},
    ],
  },
  {
    id: "emergency", label: "Generators & Emergency Power", icon: "⚡", color: "#e87e7e",
    items: [
      { id: "gen_connect", label: "Generator Set Connection", unit: "ea", nec: "NEC 445",
        note: "Terminations, control wiring, startup assist — feeder runs separate",
        variants: [
          { label: "≤50 kW",     mat: 900.00,  hrs: 16.0 },
          { label: "60–150 kW",  mat: 1800.00, hrs: 24.0 },
          { label: "200–400 kW", mat: 3200.00, hrs: 40.0 },
        ]},
      { id: "ats", label: "Automatic Transfer Switch", unit: "ea", nec: "NEC 700.5",
        variants: [
          { label: "100A",  mat: 2200.00, hrs: 8.0 },
          { label: "200A",  mat: 3400.00, hrs: 10.0 },
          { label: "400A",  mat: 6800.00, hrs: 14.0 },
        ]},
      { id: "dock_station", label: "Generator Docking Station", unit: "ea", nec: "NEC 702",
        variants: [ { label: "400A cam-lock", mat: 4200.00, hrs: 12.0 } ]},
      { id: "load_bank", label: "Load Bank Test (assist)", unit: "ea", nec: "NEC 700.4",
        variants: [ { label: "Per test", mat: 150.00, hrs: 6.0 } ]},
    ],
  },
  {
    id: "site_ug", label: "Site & Underground", icon: "▽", color: "#dce0e8",
    items: [
      { id: "ductbank", label: "Duct Bank (concrete encased)", unit: "ft", nec: "NEC 300.5",
        note: "PVC, spacers, rebar, concrete — excavation by others unless added",
        variants: [
          { label: '2-way 4"', mat: 28.00, hrs: 0.22 },
          { label: '4-way 4"', mat: 46.00, hrs: 0.32 },
        ]},
      { id: "trench", label: "Trench & Backfill (machine)", unit: "ft", nec: "NEC 300.5",
        variants: [ { label: '24" deep', mat: 4.00, hrs: 0.10 } ]},
      { id: "handhole", label: "Handhole Set", unit: "ea", nec: "NEC 314.30",
        variants: [ { label: '24"×36" polymer', mat: 850.00, hrs: 6.0 } ]},
      { id: "bore", label: "Directional Bore (sub)", unit: "ft", nec: "NEC 300.5",
        variants: [ { label: '4" conduit', mat: 32.00, hrs: 0.05 } ]},
    ],
  },
  {
    id: "demo_test", label: "Demo, Testing & Commissioning", icon: "✓", color: "#dce0e8",
    items: [
      { id: "demo_raceway", label: "Demo Conduit & Wire", unit: "ft", nec: "—",
        variants: [ { label: "Make safe & remove", mat: 0.30, hrs: 0.030 } ]},
      { id: "demo_gear", label: "Demo Equipment / Gear", unit: "ea", nec: "—",
        variants: [
          { label: "Panel/disconnect", mat: 25.00,  hrs: 3.0 },
          { label: "Gear section",     mat: 120.00, hrs: 10.0 },
        ]},
      { id: "megger", label: "Insulation Resistance Test", unit: "ea", nec: "—",
        variants: [ { label: "Per feeder, documented", mat: 5.00, hrs: 0.75 } ]},
      { id: "torque_check", label: "Torque Verification", unit: "ea", nec: "NEC 110.14",
        variants: [ { label: "Per gear section", mat: 0.00, hrs: 1.0 } ]},
      { id: "thermal_scan", label: "IR Thermal Scan (assist)", unit: "ea", nec: "—",
        variants: [ { label: "Per panel", mat: 0.00, hrs: 0.50 } ]},
    ],
  },
];

// Flat lookup for the AI layer and future UI
export const INDUSTRIAL_ITEMS = INDUSTRIAL_CATEGORIES.flatMap(cat =>
  cat.items.map(item => ({ ...item, category: cat.id, categoryLabel: cat.label }))
);

export function findIndustrialItem(id) {
  return INDUSTRIAL_ITEMS.find(i => i.id === id) || null;
}
