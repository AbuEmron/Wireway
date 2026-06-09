// src/data/catalog.js
// NEC 2023 residential service catalog — 128+ services across 12 categories
// Imported by electrical-estimator.jsx

// ─── CATALOG: each service has separate materialCost + laborCost ──────────────
// materialCost = typical material cost (electrician supplies)
// laborCost    = labor only (no materials)
// variants multiply the TOTAL of both
// ─── LABOR HOUR NOTES (NECA MLU anchored, remodel/existing construction) ─────
// 1-for-1 swap = existing box, no fishing wire, device only: ~0.25 hr outlets/switches, ~0.5 hr fixtures
// New outlet/switch in existing wall (fish wire, cut box): ~0.6 hr outlets, ~0.5 hr switches
// New circuit from panel: 3.5–6 hr depending on distance/complexity
// Panel breaker swap: ~0.75 hr standard, ~1.25 hr AFCI/GFCI (more wiring)
// Panel upgrade 100A: 6–8 hr; 200A: 8–12 hr; 400A: 14–18 hr
// Ceiling fixture swap: ~0.75 hr; new box + fixture: ~1.5 hr
// Ceiling fan swap: ~1.25 hr; new fan on existing box: ~1.5 hr; new box + fan: ~2.5 hr
// Recessed can remodel: ~1.25 hr each; new construction: ~0.75 hr each
// Low-voltage run: ~1.5–2 hr per drop depending on distance

const CATEGORIES = [
  {
    id: "service_entrance", label: "Service Entrance & Panel", icon: "▦", color: "#e8c97a",
    services: [
      // Panel upgrades: 100A ~7 hr realistic with utility coordination; 200A ~10 hr; 400A ~16 hr
      { id: "panel_100",        label: "Panel Upgrade – 100A",              nec: "NEC 230.79(C)",   materialCost: 650,  laborCost: 595,  laborHours: 7,   unit: "panel",   variants: [{ label: "Standard", m: 1 }, { label: "Main Breaker", m: 1.15 }] },
      { id: "panel_200",        label: "Panel Upgrade – 200A",              nec: "NEC 230.79(D)",   materialCost: 1200, laborCost: 850,  laborHours: 10,  unit: "panel",   variants: [{ label: "Standard", m: 1 }, { label: "Main Breaker", m: 1.15 }, { label: "Smart Panel", m: 1.6 }] },
      { id: "panel_400",        label: "Panel Upgrade – 400A",              nec: "NEC 230.79",      materialCost: 2800, laborCost: 1360, laborHours: 16,  unit: "panel",   variants: [{ label: "Standard", m: 1 }, { label: "With Meter Stack", m: 1.3 }] },
      // Sub-panel: 5–7 hr depending on feed distance
      { id: "sub_panel",        label: "Sub-Panel Install",                 nec: "NEC 225.30",      materialCost: 500,  laborCost: 510,  laborHours: 6,   unit: "panel",   variants: [{ label: "60A", m: 1 }, { label: "100A", m: 1.2 }, { label: "150A", m: 1.4 }] },
      // Breaker swap: standard ~0.75 hr; AFCI/GFCI ~1.25 hr (more wiring, testing)
      { id: "breaker_single",   label: "Breaker – Single Pole",             nec: "NEC 240.6",       materialCost: 60,   laborCost: 64,   laborHours: 0.75,unit: "breaker", variants: [{ label: "15A Standard", m: 1 }, { label: "20A Standard", m: 1.05 }, { label: "AFCI", m: 1.65 }, { label: "GFCI", m: 1.65 }, { label: "Dual AFCI/GFCI", m: 1.85 }] },
      { id: "breaker_double",   label: "Breaker – Double Pole",             nec: "NEC 240.6",       materialCost: 90,   laborCost: 85,   laborHours: 1,   unit: "breaker", variants: [{ label: "30A", m: 1 }, { label: "40A", m: 1.1 }, { label: "50A", m: 1.2 }, { label: "60A", m: 1.3 }] },
      // Surge protector: ~1.5 hr at panel
      { id: "surge_whole",      label: "Whole-Home Surge Protector",        nec: "NEC 230.67",      materialCost: 180,  laborCost: 128,  laborHours: 1.5, unit: "unit",    variants: [{ label: "Type 1", m: 1 }, { label: "Type 2", m: 0.9 }, { label: "Type 1+2 Combo", m: 1.2 }] },
      // Meter socket: 2.5–3 hr, utility coordination required
      { id: "meter_socket",     label: "Meter Socket Replacement",          nec: "NEC 230.66",      materialCost: 200,  laborCost: 213,  laborHours: 2.5, unit: "unit",    variants: [{ label: "100A", m: 1 }, { label: "200A", m: 1.2 }] },
      // Grounding: drive rod + connect GEC ~2.5 hr; Ufer is concrete work, ~4 hr
      { id: "grounding",        label: "Grounding Electrode System",        nec: "NEC 250.50",      materialCost: 120,  laborCost: 213,  laborHours: 2.5, unit: "system",  variants: [{ label: "Ground Rod", m: 1 }, { label: "Rod + Plate", m: 1.4 }, { label: "Ufer Ground", m: 1.6 }] },
      // Bonding: ~1 hr water pipe, ~1 hr gas pipe
      { id: "bonding",          label: "Bonding – Water / Gas Pipe",        nec: "NEC 250.104",     materialCost: 40,   laborCost: 85,   laborHours: 1,   unit: "each",    variants: [{ label: "Water Pipe", m: 1 }, { label: "Gas Pipe", m: 1 }, { label: "Both", m: 1.8 }] },
    ],
  },
  {
    id: "circuits", label: "Branch Circuits & Wiring", icon: "⌁", color: "#7eb8e8",
    services: [
      // New circuits in finished home: fish walls, drill plates, run to panel — 3.5–5 hr typical
      { id: "circuit_15",       label: "New 15A Circuit",                   nec: "NEC 210.11",      materialCost: 120,  laborCost: 298,  laborHours: 3.5, unit: "circuit", variants: [{ label: "Standard", m: 1 }, { label: "AFCI Required", m: 1.2 }, { label: "Long Run (50+ ft)", m: 1.5 }] },
      { id: "circuit_20",       label: "New 20A Circuit",                   nec: "NEC 210.11",      materialCost: 140,  laborCost: 340,  laborHours: 4,   unit: "circuit", variants: [{ label: "Standard", m: 1 }, { label: "Kitchen/Bath", m: 1.15 }, { label: "AFCI Required", m: 1.25 }] },
      // 240V circuits: heavier wire, more panel work — 5–6 hr
      { id: "circuit_30",       label: "New 240V Circuit – 30A",            nec: "NEC 210.19",      materialCost: 180,  laborCost: 425,  laborHours: 5,   unit: "circuit", variants: [{ label: "Dryer", m: 1 }, { label: "HVAC", m: 1.1 }, { label: "Hot Tub/Spa", m: 1.3 }] },
      { id: "circuit_50",       label: "New 240V Circuit – 50A",            nec: "NEC 210.19",      materialCost: 220,  laborCost: 510,  laborHours: 6,   unit: "circuit", variants: [{ label: "Range/Oven", m: 1 }, { label: "EV Charger", m: 1.1 }, { label: "Pool Equipment", m: 1.2 }] },
      { id: "circuit_dedicated",label: "Dedicated Appliance Circuit",       nec: "NEC 210.52",      materialCost: 130,  laborCost: 340,  laborHours: 4,   unit: "circuit", variants: [{ label: "Refrigerator", m: 1 }, { label: "Microwave", m: 1 }, { label: "Dishwasher", m: 1 }, { label: "Disposal", m: 1 }] },
      // Room rewire: open walls or extensive fish work — 8–16 hr
      { id: "rewire_room",      label: "Room Rewire",                       nec: "NEC 310.15",      materialCost: 350,  laborCost: 680,  laborHours: 8,   unit: "room",    variants: [{ label: "Bedroom", m: 1 }, { label: "Living Room", m: 1.2 }, { label: "Kitchen", m: 1.8 }, { label: "Bathroom", m: 1.3 }] },
      { id: "knob_tube",        label: "Knob & Tube Removal/Replace",       nec: "NEC 394.12",      materialCost: 600,  laborCost: 1190, laborHours: 14,  unit: "room",    variants: [{ label: "Single Room", m: 1 }, { label: "Per Floor", m: 3 }, { label: "Whole House", m: 7 }] },
      // Aluminum remediation: ~0.4 hr per point (CO/ALR device or pigtail)
      { id: "aluminum_wiring",  label: "Aluminum Wiring Remediation",       nec: "NEC 310.14(B)",   materialCost: 50,   laborCost: 34,   laborHours: 0.4, unit: "outlet",  variants: [{ label: "CO/ALR Device", m: 1 }, { label: "Pigtail w/ Marrette", m: 1.3 }] },
      // Low voltage: ~1.5 hr per drop in finished home
      { id: "low_voltage",      label: "Low-Voltage / Data Run",            nec: "NEC 725.41",      materialCost: 60,   laborCost: 128,  laborHours: 1.5, unit: "run",     variants: [{ label: "Cat6 Ethernet", m: 1 }, { label: "Coax", m: 0.9 }, { label: "Speaker Wire", m: 0.8 }, { label: "HDMI/AV", m: 1.1 }] },
    ],
  },
  {
    id: "receptacles", label: "Receptacles & Outlets", icon: "⬡", color: "#a8e87e",
    services: [
      // 1-for-1 swap: ~0.25 hr (pull out, reconnect, done — no fishing)
      // New outlet in existing wall: ~0.6 hr (cut box, fish wire, connect)
      { id: "outlet_standard",  label: "Standard Duplex Outlet",            nec: "NEC 210.52",      materialCost: 18,   laborCost: 21,   laborHours: 0.25,unit: "outlet",  variants: [{ label: "1-for-1 Swap", m: 1 }, { label: "New – Same Wall (15A)", m: 2.4 }, { label: "New – Same Wall (20A)", m: 2.65 }] },
      { id: "outlet_gfci",      label: "GFCI Outlet",                       nec: "NEC 210.8",       materialCost: 38,   laborCost: 25,   laborHours: 0.3, unit: "outlet",  variants: [{ label: "1-for-1 Swap", m: 1 }, { label: "New Install – 15A", m: 2.3 }, { label: "New Install – 20A", m: 2.5 }, { label: "WR + New Install", m: 2.8 }] },
      { id: "outlet_afci",      label: "AFCI Outlet",                       nec: "NEC 210.12",      materialCost: 55,   laborCost: 25,   laborHours: 0.3, unit: "outlet",  variants: [{ label: "1-for-1 Swap", m: 1 }, { label: "New Install – 15A", m: 2.2 }, { label: "New Install – 20A", m: 2.4 }] },
      { id: "outlet_usb",       label: "USB / Smart Outlet",                nec: "NEC 210.52",      materialCost: 55,   laborCost: 25,   laborHours: 0.3, unit: "outlet",  variants: [{ label: "1-for-1 Swap", m: 1 }, { label: "New Install – USB-A/C", m: 2.2 }, { label: "New Install – Smart Wi-Fi", m: 2.8 }, { label: "New Install – TR", m: 2.3 }] },
      // 240V outlet: no swap variant — always involves new wire or circuit work
      { id: "outlet_240",       label: "240V Outlet",                       nec: "NEC 210.19",      materialCost: 55,   laborCost: 170,  laborHours: 2,   unit: "outlet",  variants: [{ label: "NEMA 6-20 (Welder)", m: 1 }, { label: "NEMA 14-30 (Dryer)", m: 1.1 }, { label: "NEMA 14-50 (RV/EV)", m: 1.2 }] },
      // Floor outlet: always new work, ~2 hr
      { id: "outlet_floor",     label: "Floor Outlet",                      nec: "NEC 314.27(B)",   materialCost: 85,   laborCost: 170,  laborHours: 2,   unit: "outlet",  variants: [{ label: "Standard", m: 1 }, { label: "With Cover", m: 1.2 }] },
      // Exterior: ~1.5 hr new; swap existing box is ~0.5 hr
      { id: "outlet_exterior",  label: "Exterior Outlet",                   nec: "NEC 210.52(E)",   materialCost: 45,   laborCost: 43,   laborHours: 0.5, unit: "outlet",  variants: [{ label: "1-for-1 Swap", m: 1 }, { label: "New – In-Use Cover", m: 3 }, { label: "New – WR + In-Use Cover", m: 3.4 }, { label: "New – Recessed", m: 4 }] },
      { id: "outlet_countertop",label: "Countertop Popup Outlet",           nec: "NEC 210.52(C)",   materialCost: 120,  laborCost: 213,  laborHours: 2.5, unit: "outlet",  variants: [{ label: "Standard", m: 1 }, { label: "With USB", m: 1.2 }, { label: "Wireless Charger", m: 1.5 }] },
      // EV: ~5 hr for Level 2 hardwire including panel work
      { id: "outlet_ev",        label: "EV Charger / EVSE",                 nec: "NEC 625.40",      materialCost: 280,  laborCost: 425,  laborHours: 5,   unit: "unit",    variants: [{ label: "NEMA 14-50 Outlet", m: 1 }, { label: "Level 2 Hardwire", m: 1.5 }, { label: "Smart EVSE", m: 1.8 }] },
    ],
  },
  {
    id: "switches", label: "Switches & Controls", icon: "◈", color: "#e87eb8",
    services: [
      // 1-for-1 swap: ~0.25 hr (remove old, connect new — box and wire already there)
      // New switch location: ~0.5 hr (cut box, fish wire, connect)
      { id: "switch_single",    label: "Single Pole Switch",                nec: "NEC 404.2",       materialCost: 14,   laborCost: 21,   laborHours: 0.25,unit: "switch",  variants: [{ label: "1-for-1 Swap – Standard", m: 1 }, { label: "1-for-1 Swap – Decorator", m: 1.05 }, { label: "1-for-1 Swap – Lighted", m: 1.1 }, { label: "New Location", m: 2 }] },
      { id: "switch_3way",      label: "3-Way Switch",                      nec: "NEC 404.2",       materialCost: 24,   laborCost: 34,   laborHours: 0.4, unit: "switch",  variants: [{ label: "1-for-1 Swap – Standard", m: 1 }, { label: "1-for-1 Swap – Decorator", m: 1.1 }, { label: "New Location", m: 2.5 }] },
      { id: "switch_4way",      label: "4-Way Switch",                      nec: "NEC 404.2",       materialCost: 35,   laborCost: 43,   laborHours: 0.5, unit: "switch",  variants: [{ label: "1-for-1 Swap – Standard", m: 1 }, { label: "1-for-1 Swap – Decorator", m: 1.1 }, { label: "New Location", m: 2.5 }] },
      // Dimmer swap: ~0.35 hr (slightly more — check load compatibility, neutral wire)
      { id: "dimmer_single",    label: "Dimmer – Single Pole",              nec: "NEC 404.14",      materialCost: 35,   laborCost: 30,   laborHours: 0.35,unit: "switch",  variants: [{ label: "1-for-1 Swap – Standard", m: 1 }, { label: "1-for-1 Swap – LED Compatible", m: 1.1 }, { label: "1-for-1 Swap – Smart/Wi-Fi", m: 1.4 }, { label: "New Location", m: 2.1 }] },
      { id: "dimmer_3way",      label: "Dimmer – 3-Way",                    nec: "NEC 404.14",      materialCost: 55,   laborCost: 43,   laborHours: 0.5, unit: "switch",  variants: [{ label: "1-for-1 Swap – Standard", m: 1 }, { label: "1-for-1 Swap – Smart/Wi-Fi", m: 1.5 }, { label: "New Location", m: 2.5 }] },
      // Smart switch: ~0.75 hr swap (app pairing, neutral check, load programming)
      { id: "switch_smart",     label: "Smart Switch / Scene Controller",   nec: "NEC 404.2",       materialCost: 85,   laborCost: 64,   laborHours: 0.75,unit: "switch",  variants: [{ label: "1-for-1 Swap – Wi-Fi", m: 1 }, { label: "1-for-1 Swap – Z-Wave/Zigbee", m: 1.15 }, { label: "1-for-1 Swap – Lutron Caseta", m: 1.25 }, { label: "New Location", m: 1.8 }] },
      { id: "gfci_switch",      label: "GFCI Combo Switch",                 nec: "NEC 210.8",       materialCost: 45,   laborCost: 30,   laborHours: 0.35,unit: "switch",  variants: [{ label: "1-for-1 Swap", m: 1 }, { label: "New Install", m: 2 }] },
      { id: "occupancy_sensor", label: "Occupancy / Motion Switch",         nec: "NEC 404.2",       materialCost: 55,   laborCost: 51,   laborHours: 0.6, unit: "switch",  variants: [{ label: "1-for-1 Swap – Wall Switch", m: 1 }, { label: "New Location – Wall", m: 1.8 }, { label: "New – Ceiling Sensor", m: 2.5 }] },
      { id: "timer_switch",     label: "Timer Switch",                      nec: "NEC 404.2",       materialCost: 35,   laborCost: 30,   laborHours: 0.35,unit: "switch",  variants: [{ label: "1-for-1 Swap – Mechanical", m: 1 }, { label: "1-for-1 Swap – Digital", m: 1.15 }, { label: "New – Outdoor Intermatic", m: 2.2 }] },
    ],
  },
  {
    id: "lighting", label: "Lighting & Fixtures", icon: "◎", color: "#e8b87e",
    services: [
      // Fixture swap on existing box: ~0.75 hr; new box + fixture: ~1.5 hr
      { id: "light_ceiling",    label: "Ceiling Light Fixture",             nec: "NEC 314.27",      materialCost: 55,   laborCost: 64,   laborHours: 0.75,unit: "fixture", variants: [{ label: "Swap on Existing Box", m: 1 }, { label: "New Box + Flush Mount", m: 2 }, { label: "New Box + Semi-Flush", m: 2.1 }] },
      // Recessed: remodel retrofit ~1.25 hr; new construction open ceiling ~0.75 hr
      { id: "light_recessed",   label: "Recessed Lighting (Can)",           nec: "NEC 410.116",     materialCost: 45,   laborCost: 106,  laborHours: 1.25,unit: "fixture", variants: [{ label: "Remodel/Retrofit", m: 1 }, { label: "New Construction", m: 0.6 }, { label: "IC Rated Remodel", m: 1.1 }, { label: "Airtight AT Rated", m: 1.15 }] },
      // Pendant: more wire work, canopy, ~1.5 hr swap; new location ~2.5 hr
      { id: "light_pendant",    label: "Pendant Light",                     nec: "NEC 410.36",      materialCost: 80,   laborCost: 128,  laborHours: 1.5, unit: "fixture", variants: [{ label: "Swap on Existing Box", m: 1 }, { label: "New Location – Standard", m: 1.65 }, { label: "New – Over Island", m: 1.85 }, { label: "New – Heavy/Oversized", m: 2.3 }] },
      // Chandelier: 2.5–6 hr depending on weight and ceiling height
      { id: "light_chandelier", label: "Chandelier",                        nec: "NEC 410.36",      materialCost: 120,  laborCost: 213,  laborHours: 2.5, unit: "fixture", variants: [{ label: "Swap on Existing Box", m: 1 }, { label: "New – Standard (<50 lbs)", m: 1.4 }, { label: "New – Heavy (50-100 lbs)", m: 2 }, { label: "New – Vaulted/High Ceiling", m: 2.4 }] },
      // Fan swap on fan-rated box: ~1.25 hr; new fan-rated box install: ~2.5 hr
      { id: "light_fan",        label: "Ceiling Fan",                       nec: "NEC 314.27(D)",   materialCost: 100,  laborCost: 106,  laborHours: 1.25,unit: "fixture", variants: [{ label: "Swap on Fan-Rated Box", m: 1 }, { label: "New Fan-Rated Box + Fan", m: 2 }, { label: "With Light Kit – Swap", m: 1.1 }, { label: "Remote/Smart – Swap", m: 1.25 }, { label: "Vaulted Mount – New", m: 2.4 }] },
      { id: "light_track",      label: "Track Lighting",                    nec: "NEC 410.151",     materialCost: 90,   laborCost: 128,  laborHours: 1.5, unit: "run",     variants: [{ label: "4 ft", m: 1 }, { label: "8 ft", m: 1.6 }, { label: "Flexible/Monorail", m: 2 }] },
      { id: "light_undercab",   label: "Under-Cabinet Lighting",            nec: "NEC 411.2",       materialCost: 70,   laborCost: 128,  laborHours: 1.5, unit: "run",     variants: [{ label: "LED Strip (per 4 ft)", m: 1 }, { label: "Puck Lights", m: 0.9 }, { label: "Hardwired LED Bar", m: 1.3 }] },
      { id: "light_exterior",   label: "Exterior / Porch Light",            nec: "NEC 410.10",      materialCost: 55,   laborCost: 64,   laborHours: 0.75,unit: "fixture", variants: [{ label: "Swap on Existing Box", m: 1 }, { label: "New – Wall Sconce", m: 2 }, { label: "New – Flood/Security", m: 2.2 }, { label: "New – Motion Activated", m: 2.4 }, { label: "New – Post/Pier Mount", m: 3 }] },
      { id: "light_vanity",     label: "Bathroom Vanity Light",             nec: "NEC 410.10(D)",   materialCost: 60,   laborCost: 64,   laborHours: 0.75,unit: "fixture", variants: [{ label: "Swap on Existing Box", m: 1 }, { label: "New – Standard Bar", m: 2 }, { label: "New – Hollywood/Globe", m: 2.1 }, { label: "New – Lighted Mirror", m: 2.5 }] },
      // Exhaust fan: replace existing ~1 hr; new cut-in ~2.5 hr
      { id: "exhaust_fan",      label: "Bathroom Exhaust Fan",              nec: "NEC 210.11",      materialCost: 80,   laborCost: 85,   laborHours: 1,   unit: "unit",    variants: [{ label: "1-for-1 Swap – Standard", m: 1 }, { label: "1-for-1 Swap – With Light", m: 1.15 }, { label: "New Cut-In – Standard", m: 2.5 }, { label: "New Cut-In – With Heater", m: 2.8 }, { label: "New Cut-In – High-CFM", m: 2.6 }] },
      // LED retrofit: bulb swap ~10 min; recessed retrofit kit ~30 min
      { id: "led_retrofit",     label: "LED Retrofit / Recessed Kit",       nec: "NEC 410.6",       materialCost: 20,   laborCost: 17,   laborHours: 0.2, unit: "fixture", variants: [{ label: "Standard Bulb Swap", m: 1 }, { label: "Recessed Retrofit Kit", m: 2.5 }] },
      { id: "landscape_light",  label: "Landscape / Low-Voltage Lighting",  nec: "NEC 411.2",       materialCost: 120,  laborCost: 213,  laborHours: 2.5, unit: "zone",    variants: [{ label: "Low-Voltage Kit", m: 1 }, { label: "Hardwired Line Voltage", m: 1.8 }, { label: "Solar Accent", m: 0.6 }] },
    ],
  },
  {
    id: "safety", label: "Safety & Detection", icon: "◉", color: "#e87e7e",
    services: [
      // Smoke/CO detector swap: ~0.3 hr; new hardwired with interconnect: ~0.75 hr
      { id: "smoke_detector",   label: "Smoke Detector",                    nec: "NEC 760.41",      materialCost: 35,   laborCost: 34,   laborHours: 0.4, unit: "unit",    variants: [{ label: "Battery (Swap/New)", m: 1 }, { label: "Hardwired – Swap", m: 1 }, { label: "Hardwired + Interconnect – New", m: 1.9 }] },
      { id: "co_detector",      label: "CO Detector",                       nec: "NEC 760.41",      materialCost: 40,   laborCost: 34,   laborHours: 0.4, unit: "unit",    variants: [{ label: "Battery (Swap/New)", m: 1 }, { label: "Hardwired – Swap", m: 1.05 }, { label: "Combo Smoke/CO – Swap", m: 1.15 }] },
      { id: "smoke_co_combo",   label: "Smoke + CO Combo (Hardwired)",      nec: "NEC 210.12",      materialCost: 65,   laborCost: 51,   laborHours: 0.6, unit: "unit",    variants: [{ label: "Swap Existing", m: 1 }, { label: "New – Interconnected", m: 1.65 }, { label: "New – Smart (Nest/Ring)", m: 2 }] },
      // AFCI/GFCI retrofit: ~1.25 hr at breaker; ~0.4 hr outlet type
      { id: "arc_fault",        label: "AFCI Protection – Retrofit",        nec: "NEC 210.12",      materialCost: 70,   laborCost: 106,  laborHours: 1.25,unit: "circuit", variants: [{ label: "Breaker Type", m: 1 }, { label: "Outlet Type", m: 0.32 }] },
      { id: "gfci_protection",  label: "GFCI Protection – Retrofit",        nec: "NEC 210.8",       materialCost: 38,   laborCost: 85,   laborHours: 1,   unit: "circuit", variants: [{ label: "Breaker Type", m: 1 }, { label: "Outlet Type (Feeds Multiple)", m: 0.4 }] },
      { id: "tamper_resistant", label: "Tamper-Resistant Receptacles",      nec: "NEC 406.12",      materialCost: 20,   laborCost: 21,   laborHours: 0.25,unit: "outlet",  variants: [{ label: "1-for-1 Swap – Standard TR", m: 1 }, { label: "1-for-1 Swap – TR + WR", m: 1.15 }] },
      { id: "surge_individual", label: "Point-of-Use Surge Protection",     nec: "NEC 230.67",      materialCost: 30,   laborCost: 21,   laborHours: 0.25,unit: "unit",    variants: [{ label: "Outlet Type – Swap", m: 1 }, { label: "Hardwired", m: 3 }] },
    ],
  },
  {
    id: "hvac_appliance", label: "HVAC & Appliance Connections", icon: "⧖", color: "#b87ee8",
    services: [
      // AC disconnect/whip: ~2.5 hr mount, connect, test
      { id: "ac_disconnect",    label: "AC Disconnect / Whip",              nec: "NEC 440.14",      materialCost: 120,  laborCost: 213,  laborHours: 2.5, unit: "unit",    variants: [{ label: "Non-Fused", m: 1 }, { label: "Fused", m: 1.2 }, { label: "60A", m: 1 }, { label: "100A", m: 1.3 }] },
      // HVAC dedicated circuit: 4–6 hr (panel to unit, outdoor disconnect)
      { id: "hvac_circuit",     label: "HVAC Dedicated Circuit",            nec: "NEC 440.32",      materialCost: 180,  laborCost: 425,  laborHours: 5,   unit: "unit",    variants: [{ label: "Mini Split", m: 1 }, { label: "Central AC", m: 1.2 }, { label: "Heat Pump", m: 1.2 }, { label: "Electric Furnace", m: 1.4 }] },
      // Dryer/range hookup: ~1.5 hr connection only; includes pulling appliance out
      { id: "dryer_hookup",     label: "Dryer Hookup / Connection",         nec: "NEC 220.54",      materialCost: 55,   laborCost: 128,  laborHours: 1.5, unit: "unit",    variants: [{ label: "3-Wire NEMA 10-30", m: 1 }, { label: "4-Wire NEMA 14-30", m: 1 }, { label: "Gas Dryer Outlet Only", m: 0.6 }] },
      { id: "range_hookup",     label: "Range / Oven Hookup",               nec: "NEC 220.55",      materialCost: 65,   laborCost: 128,  laborHours: 1.5, unit: "unit",    variants: [{ label: "Freestanding Range", m: 1 }, { label: "Wall Oven", m: 1.2 }, { label: "Cooktop + Oven", m: 1.5 }] },
      // Water heater circuit: ~3.5 hr including panel breaker and disconnect
      { id: "water_heater",     label: "Electric Water Heater Circuit",     nec: "NEC 422.11",      materialCost: 150,  laborCost: 298,  laborHours: 3.5, unit: "unit",    variants: [{ label: "Standard Tank", m: 1 }, { label: "Tankless/On-Demand", m: 1.6 }] },
      // Generator transfer switch: ~6 hr manual; ATS ~12+ hr
      { id: "generator_switch", label: "Generator Transfer Switch",         nec: "NEC 702.6",       materialCost: 450,  laborCost: 510,  laborHours: 6,   unit: "unit",    variants: [{ label: "Manual Transfer", m: 1 }, { label: "Interlock Kit", m: 0.6 }, { label: "Auto Transfer (ATS)", m: 2.2 }] },
      { id: "generator_inlet",  label: "Generator Inlet Box",               nec: "NEC 702.7",       materialCost: 120,  laborCost: 213,  laborHours: 2.5, unit: "unit",    variants: [{ label: "30A Inlet", m: 1 }, { label: "50A Inlet", m: 1.2 }] },
      // Pool equipment: 6–8 hr full pad; GFCI wiring, bonding, disconnect
      { id: "pool_equipment",   label: "Pool / Spa Equipment Circuit",      nec: "NEC 680.21",      materialCost: 250,  laborCost: 510,  laborHours: 6,   unit: "unit",    variants: [{ label: "Pump Motor", m: 1 }, { label: "Heater", m: 1.3 }, { label: "Full Equipment Pad", m: 2 }] },
    ],
  },
  {
    id: "smart_home", label: "Structured Wiring & Smart Home", icon: "⬡", color: "#7ee8d4",
    services: [
      // Data panel: ~4 hr build-out and mount
      { id: "data_panel",       label: "Structured Media / Data Panel",     nec: "NEC 800.133",     materialCost: 220,  laborCost: 340,  laborHours: 4,   unit: "unit",    variants: [{ label: "Basic", m: 1 }, { label: "With Network Switch", m: 1.4 }] },
      // Cat6 per drop: ~1.5 hr finished home (fish + terminate)
      { id: "cat6_drop",        label: "Cat6 Home Run (Per Drop)",          nec: "NEC 725.41",      materialCost: 60,   laborCost: 128,  laborHours: 1.5, unit: "drop",    variants: [{ label: "To Patch Panel", m: 1 }, { label: "Terminated Both Ends", m: 1.1 }] },
      { id: "coax_drop",        label: "Coax Run (Per Drop)",               nec: "NEC 820.133",     materialCost: 35,   laborCost: 106,  laborHours: 1.25,unit: "drop",    variants: [{ label: "Standard", m: 1 }, { label: "RG6 Quad Shield", m: 1.1 }] },
      // Doorbell: ~1 hr swap; new wired ~1.5 hr
      { id: "doorbell",         label: "Doorbell / Video Doorbell",         nec: "NEC 725.41",      materialCost: 60,   laborCost: 64,   laborHours: 0.75,unit: "unit",    variants: [{ label: "1-for-1 Swap", m: 1 }, { label: "New Wired – Ring/Nest", m: 2 }, { label: "New Circuit Required", m: 3.3 }] },
      { id: "intercom",         label: "Intercom System",                   nec: "NEC 725.41",      materialCost: 180,  laborCost: 255,  laborHours: 3,   unit: "unit",    variants: [{ label: "Basic Door Intercom", m: 1 }, { label: "Multi-Room", m: 2 }] },
      { id: "security_prewire", label: "Security System Pre-Wire",          nec: "NEC 760.41",      materialCost: 120,  laborCost: 255,  laborHours: 3,   unit: "zone",    variants: [{ label: "Per Zone", m: 1 }, { label: "Full House (8 zones)", m: 5 }] },
      { id: "whole_audio",      label: "Whole-Home Audio Pre-Wire",         nec: "NEC 725.41",      materialCost: 80,   laborCost: 170,  laborHours: 2,   unit: "room",    variants: [{ label: "Stereo (2 speakers)", m: 1 }, { label: "Surround (5 drops)", m: 2 }] },
    ],
  },
  {
    id: "outdoor", label: "Outdoor, Garage & Specialty", icon: "◫", color: "#e8d47e",
    services: [
      // Garage circuit: ~3.5 hr standard; 240V adds panel work ~5 hr
      { id: "garage_circuit",   label: "Garage Circuit",                    nec: "NEC 210.52(G)",   materialCost: 140,  laborCost: 298,  laborHours: 3.5, unit: "circuit", variants: [{ label: "20A General", m: 1 }, { label: "240V Workshop", m: 1.4 }, { label: "EV Charger", m: 1.55 }] },
      // Outdoor sub-panel: ~10 hr overhead; underground adds trenching ~12 hr
      { id: "outdoor_subpanel", label: "Outdoor Sub-Panel (Detached)",      nec: "NEC 225.30",      materialCost: 600,  laborCost: 850,  laborHours: 10,  unit: "unit",    variants: [{ label: "60A Overhead", m: 1 }, { label: "100A Overhead", m: 1.3 }, { label: "100A Underground", m: 1.5 }] },
      // Outdoor GFCI: ~0.5 hr swap; new mount ~1.5 hr
      { id: "gfci_outdoor",     label: "Outdoor GFCI Outlet",               nec: "NEC 210.8(A)(3)", materialCost: 45,   laborCost: 43,   laborHours: 0.5, unit: "outlet",  variants: [{ label: "1-for-1 Swap", m: 1 }, { label: "New – Wall Mount", m: 3 }, { label: "New – Deck Box", m: 3.4 }, { label: "New – In-Ground/Pedestal", m: 4 }] },
      // Flood light: ~0.75 hr swap; new mount with wire ~2 hr
      { id: "flood_light",      label: "Security / Flood Light",            nec: "NEC 410.10",      materialCost: 65,   laborCost: 64,   laborHours: 0.75,unit: "fixture", variants: [{ label: "Swap on Existing Box", m: 1 }, { label: "New – Motion Flood", m: 2.65 }, { label: "New – Camera + Light", m: 3.2 }, { label: "New – Dusk-to-Dawn", m: 2.8 }] },
      // Conduit: ~45 min per 10 ft EMT; underground adds ~50% more
      { id: "conduit_run",      label: "Conduit Run (per 10 ft)",           nec: "NEC 358.26",      materialCost: 35,   laborCost: 64,   laborHours: 0.75,unit: "10 ft",   variants: [{ label: "EMT", m: 1 }, { label: "PVC Schedule 40", m: 0.85 }, { label: "Rigid/GRC", m: 1.4 }, { label: "Underground Direct Burial", m: 1.6 }] },
      { id: "solar_ready",      label: "Solar / Battery Ready Conduit",     nec: "NEC 690.12",      materialCost: 280,  laborCost: 510,  laborHours: 6,   unit: "unit",    variants: [{ label: "Panel Conduit Stub", m: 1 }, { label: "Full Conduit + Junction", m: 1.5 }] },
      { id: "hot_tub",          label: "Hot Tub / Spa Install",             nec: "NEC 680.42",      materialCost: 380,  laborCost: 680,  laborHours: 8,   unit: "unit",    variants: [{ label: "Plug-in NEMA 14-50", m: 1 }, { label: "Hardwired 240V/50A", m: 1.4 }, { label: "With GFCI + Bonding", m: 1.6 }] },
      { id: "ev_parking",       label: "Driveway / Parking EV Outlet",      nec: "NEC 625.40",      materialCost: 300,  laborCost: 510,  laborHours: 6,   unit: "unit",    variants: [{ label: "NEMA 14-50 Outdoor", m: 1 }, { label: "Level 2 EVSE Pedestal", m: 1.6 }] },
      { id: "exterior_disconnect", label: "Exterior Emergency Disconnect",  nec: "NEC 230.85",      materialCost: 180,  laborCost: 255,  laborHours: 3,   unit: "unit",    variants: [{ label: "Meter Main Combo", m: 1 }, { label: "Separate Exterior Disconnect", m: 1.3 }] },
      { id: "energy_mgmt",      label: "Energy Management System",          nec: "NEC 220.70",      materialCost: 350,  laborCost: 425,  laborHours: 5,   unit: "unit",    variants: [{ label: "Basic Load Monitor", m: 1 }, { label: "Full Smart Panel Integration", m: 2 }] },
    ],
  },
  {
    id: "pool_water", label: "Pool, Spa & Water Features", icon: "◌", color: "#7ec8e8",
    services: [
      // Pool bonding: all metal within 5 ft must be bonded — 6–10 hr thorough job
      { id: "pool_bonding",       label: "Pool Equipotential Bonding",        nec: "NEC 680.26",      materialCost: 280,  laborCost: 595,  laborHours: 7,   unit: "system",  variants: [{ label: "New Pool – Full Bond", m: 1 }, { label: "Existing Pool – Remediation", m: 1.3 }, { label: "With Rebar Grid", m: 1.5 }] },
      { id: "pool_grounding",     label: "Pool Equipment Grounding",          nec: "NEC 680.6",       materialCost: 120,  laborCost: 255,  laborHours: 3,   unit: "system",  variants: [{ label: "Equipment Pad Ground", m: 1 }, { label: "Full System Ground + Bond", m: 1.8 }] },
      { id: "pool_pump_circuit",  label: "Pool Pump Motor Circuit",           nec: "NEC 680.21",      materialCost: 180,  laborCost: 425,  laborHours: 5,   unit: "unit",    variants: [{ label: "120V Single Speed", m: 1 }, { label: "240V Single Speed", m: 1.2 }, { label: "240V Variable Speed", m: 1.4 }] },
      { id: "pool_heater_circuit",label: "Pool / Spa Heater Circuit",         nec: "NEC 680.21",      materialCost: 220,  laborCost: 510,  laborHours: 6,   unit: "unit",    variants: [{ label: "Gas Heater Outlet", m: 0.6 }, { label: "Electric Heater 240V/50A", m: 1 }, { label: "Heat Pump Circuit", m: 1.1 }] },
      { id: "pool_receptacle",    label: "Pool Area Receptacle",              nec: "NEC 680.22",      materialCost: 65,   laborCost: 213,  laborHours: 2.5, unit: "outlet",  variants: [{ label: "6–10 ft from Edge (GFCI)", m: 1 }, { label: "10–20 ft from Edge (GFCI)", m: 1 }, { label: "Deck Pedestal Box", m: 1.5 }] },
      { id: "pool_light_under",   label: "Underwater Pool Light",             nec: "NEC 680.23",      materialCost: 380,  laborCost: 510,  laborHours: 6,   unit: "fixture", variants: [{ label: "120V Niched Fixture", m: 1 }, { label: "12V Low-Voltage", m: 0.9 }, { label: "LED Color Retrofit", m: 0.8 }, { label: "New Niche Install", m: 1.6 }] },
      { id: "pool_light_above",   label: "Above-Water / Deck Pool Lighting",  nec: "NEC 680.22",      materialCost: 120,  laborCost: 255,  laborHours: 3,   unit: "fixture", variants: [{ label: "Deck Post Light", m: 1 }, { label: "Landscape Uplighting", m: 1.2 }, { label: "Step / Hardscape Lights", m: 1.4 }] },
      { id: "pool_disconnect",    label: "Pool Equipment Disconnect",         nec: "NEC 680.12",      materialCost: 120,  laborCost: 213,  laborHours: 2.5, unit: "unit",    variants: [{ label: "Single Equipment", m: 1 }, { label: "Full Pad Disconnect", m: 1.5 }] },
      { id: "pool_gfci",          label: "Pool GFCI Breaker Protection",      nec: "NEC 680.21(C)",   materialCost: 90,   laborCost: 106,  laborHours: 1.25,unit: "circuit", variants: [{ label: "20A Pump Circuit", m: 1 }, { label: "With Heater Circuit", m: 1.5 }] },
      { id: "fountain_circuit",   label: "Fountain / Water Feature Circuit",  nec: "NEC 680.51",      materialCost: 120,  laborCost: 255,  laborHours: 3,   unit: "unit",    variants: [{ label: "Small Fountain (120V)", m: 1 }, { label: "Large Feature (240V)", m: 1.5 }, { label: "With Underwater Lighting", m: 1.8 }] },
    ],
  },
  {
    id: "generator_systems", label: "Generator & Backup Power Systems", icon: "⚡", color: "#c8e87e",
    services: [
      { id: "generator_install",  label: "Standby Generator Installation",    nec: "NEC 702.4",       materialCost: 800,  laborCost: 1020, laborHours: 12,  unit: "unit",    variants: [{ label: "7–12 kW Air-Cooled", m: 1 }, { label: "14–20 kW Air-Cooled", m: 1.3 }, { label: "22+ kW Liquid-Cooled", m: 1.8 }] },
      { id: "generator_transfer2",label: "Generator Transfer Switch",         nec: "NEC 702.6",       materialCost: 450,  laborCost: 510,  laborHours: 6,   unit: "unit",    variants: [{ label: "Manual – 6 Circuit", m: 1 }, { label: "Manual – 10 Circuit", m: 1.2 }, { label: "Interlock Kit", m: 0.55 }, { label: "Automatic (ATS)", m: 2.2 }] },
      { id: "generator_inlet2",   label: "Generator Inlet Box",               nec: "NEC 702.7",       materialCost: 120,  laborCost: 213,  laborHours: 2.5, unit: "unit",    variants: [{ label: "30A / 7500W", m: 1 }, { label: "50A / 12500W", m: 1.2 }] },
      { id: "generator_circuit",  label: "Generator Dedicated Circuit / Run", nec: "NEC 702.5",       materialCost: 200,  laborCost: 340,  laborHours: 4,   unit: "unit",    variants: [{ label: "From Panel to Inlet", m: 1 }, { label: "Long Run (50+ ft)", m: 1.5 }] },
      { id: "battery_storage",    label: "Battery Storage System (e.g. Powerwall)", nec: "NEC 706.20",  materialCost: 1200, laborCost: 850,  laborHours: 10,  unit: "unit",    variants: [{ label: "Single Unit", m: 1 }, { label: "Stacked (2+ Units)", m: 1.6 }, { label: "With Gateway/Inverter", m: 1.4 }] },
      { id: "solar_inverter",     label: "Solar Inverter / Disconnect Install",nec: "NEC 690.15",      materialCost: 400,  laborCost: 510,  laborHours: 6,   unit: "unit",    variants: [{ label: "String Inverter", m: 1 }, { label: "Microinverter System", m: 1.4 }, { label: "Hybrid Inverter (Solar + Battery)", m: 1.8 }] },
      { id: "generator_grounding",label: "Generator Grounding & Bonding",     nec: "NEC 250.35",      materialCost: 80,   laborCost: 170,  laborHours: 2,   unit: "unit",    variants: [{ label: "Portable Generator", m: 1 }, { label: "Standby – Separately Derived", m: 1.8 }] },
    ],
  },
  {
    id: "required_outlets", label: "Required Outlets & Lighting (NEC 210)", icon: "◎", color: "#e8a87e",
    services: [
      // NEC 210.70 required lighting outlets — often missed on additions/remodels
      { id: "lighting_stair",     label: "Stairway Lighting Outlet",          nec: "NEC 210.70(A)(2)", materialCost: 55,  laborCost: 170,  laborHours: 2,   unit: "outlet",  variants: [{ label: "New Box + Switch", m: 1 }, { label: "3-Way Stair Control", m: 1.5 }] },
      { id: "lighting_attic",     label: "Attic / Crawl Space Lighting",      nec: "NEC 210.70(A)(3)", materialCost: 55,  laborCost: 213,  laborHours: 2.5, unit: "outlet",  variants: [{ label: "Single Fixture + Switch", m: 1 }, { label: "Multiple Fixtures", m: 1.5 }] },
      { id: "lighting_basement",  label: "Basement / Utility Room Lighting",  nec: "NEC 210.70(A)(3)", materialCost: 55,  laborCost: 170,  laborHours: 2,   unit: "outlet",  variants: [{ label: "Single Fixture + Switch", m: 1 }, { label: "Multiple Fixtures", m: 1.4 }] },
      { id: "lighting_garage_req",label: "Garage Required Lighting Outlet",   nec: "NEC 210.70(A)(2)", materialCost: 55,  laborCost: 170,  laborHours: 2,   unit: "outlet",  variants: [{ label: "Ceiling Fixture + Switch", m: 1 }, { label: "With Opener Circuit", m: 1.4 }] },
      { id: "lighting_closet",    label: "Closet Lighting",                   nec: "NEC 410.16",       materialCost: 35,  laborCost: 85,   laborHours: 1,   unit: "fixture", variants: [{ label: "LED Surface Mount (Swap)", m: 1 }, { label: "New Box + Fixture", m: 1.8 }] },
      { id: "hvac_outlet_req",    label: "HVAC Service Outlet (Required)",    nec: "NEC 210.63",       materialCost: 45,  laborCost: 255,  laborHours: 3,   unit: "outlet",  variants: [{ label: "Within 25 ft of Equipment", m: 1 }] },
      { id: "panel_outlet_req",   label: "Panel Service Outlet (Required)",   nec: "NEC 210.64",       materialCost: 45,  laborCost: 213,  laborHours: 2.5, unit: "outlet",  variants: [{ label: "Within 50 ft of Panel", m: 1 }] },
      { id: "bathroom_circuit",   label: "Bathroom Branch Circuit",           nec: "NEC 210.11(C)(3)", materialCost: 140, laborCost: 340,  laborHours: 4,   unit: "circuit", variants: [{ label: "Dedicated 20A Bathroom", m: 1 }, { label: "Shared Bath (max 2)", m: 1 }] },
      { id: "kitchen_sabc",       label: "Kitchen Small Appliance Circuit",   nec: "NEC 210.52(B)",    materialCost: 140, laborCost: 340,  laborHours: 4,   unit: "circuit", variants: [{ label: "First 20A Circuit", m: 1 }, { label: "Second 20A Circuit (Required)", m: 1 }] },
      { id: "laundry_circuit",    label: "Laundry Branch Circuit",            nec: "NEC 210.52(F)",    materialCost: 140, laborCost: 340,  laborHours: 4,   unit: "circuit", variants: [{ label: "20A Dedicated Laundry", m: 1 }] },
      { id: "ev_ready_conduit",   label: "EV-Ready Garage Conduit (Required)", nec: "NEC 210.17",     materialCost: 180, laborCost: 340,  laborHours: 4,   unit: "unit",    variants: [{ label: "Conduit Stub to Panel", m: 1 }, { label: "Full Conduit + 40A Circuit", m: 1.8 }] },
    ],
  },
  {
    id: "inspection_permits", label: "Permits, Inspections & Load Calc", icon: "◻", color: "#b0b0c8",
    services: [
      { id: "permit_service",     label: "Electrical Permit (Service/Panel)", nec: "NEC 90.4",         materialCost: 0,   laborCost: 300,  laborHours: 2,   unit: "permit",  variants: [{ label: "Standard Permit Pull", m: 1 }, { label: "Expedited", m: 1.5 }] },
      { id: "permit_general",     label: "Electrical Permit (General Work)",  nec: "NEC 90.4",         materialCost: 0,   laborCost: 150,  laborHours: 1,   unit: "permit",  variants: [{ label: "Standard", m: 1 }, { label: "Expedited", m: 1.5 }] },
      { id: "load_calc",          label: "Load Calculation Service",          nec: "NEC 220.82",        materialCost: 0,   laborCost: 255,  laborHours: 3,   unit: "service", variants: [{ label: "Optional Method (220.82)", m: 1 }, { label: "Standard Method (220.42)", m: 1.2 }, { label: "With Written Report", m: 1.5 }] },
      { id: "panel_labeling",     label: "Panel Circuit Labeling",            nec: "NEC 408.4",         materialCost: 15,  laborCost: 128,  laborHours: 1.5, unit: "panel",   variants: [{ label: "Existing Panel – Label Only", m: 1 }, { label: "Full Circuit Directory + Label", m: 1.5 }] },
      { id: "inspection_final",   label: "Final Electrical Inspection",       nec: "NEC 110.3",         materialCost: 0,   laborCost: 170,  laborHours: 2,   unit: "service", variants: [{ label: "Standard Inspection", m: 1 }, { label: "Re-Inspection (corrections)", m: 0.75 }] },
      { id: "whole_house_afci",   label: "Whole-House AFCI Upgrade",          nec: "NEC 210.12",        materialCost: 600, laborCost: 850,  laborHours: 10,  unit: "system",  variants: [{ label: "10–15 Circuits", m: 1 }, { label: "20+ Circuits", m: 1.5 }] },
      { id: "whole_house_gfci",   label: "Whole-House GFCI Upgrade",          nec: "NEC 210.8",         materialCost: 400, laborCost: 595,  laborHours: 7,   unit: "system",  variants: [{ label: "Required Locations Only", m: 1 }, { label: "Full Home", m: 1.5 }] },
      { id: "whole_house_surge",  label: "Whole-House Surge + AFCI/GFCI Package", nec: "NEC 230.67",   materialCost: 900, laborCost: 1190, laborHours: 14,  unit: "system",  variants: [{ label: "Standard Package", m: 1 }, { label: "With Panel Upgrade", m: 1.8 }] },
    ],
  },
];


const MARKUP_OPTIONS = [{ label: "15%", v: 0.15 }, { label: "20%", v: 0.20 }, { label: "25%", v: 0.25 }, { label: "30%", v: 0.30 }, { label: "40%", v: 0.40 }, { label: "50%", v: 0.50 }];
const HOURLY_RATES   = [55, 65, 75, 85, 95, 110, 125, 150];
const ALL_SERVICES   = CATEGORIES.flatMap(c => c.services.map(s => ({ ...s, catColor: c.color, catLabel: c.label })));



  const CHECKLISTS = {
    service_upgrade: {
      label: "Service Upgrade",
      items: [
        { id:"su1",  nec:"NEC 230.67",      text:"Whole-home surge protector installed (required 2023)" },
        { id:"su2",  nec:"NEC 230.79",      text:"Service disconnect rated correctly (min 100A residential)" },
        { id:"su3",  nec:"NEC 230.24",      text:"Service entrance clearances met (10 ft min at grade)" },
        { id:"su4",  nec:"NEC 250.50",      text:"Grounding electrode system complete and bonded" },
        { id:"su5",  nec:"NEC 250.104",     text:"Water and gas piping bonded" },
        { id:"su6",  nec:"NEC 250.28",      text:"Main bonding jumper installed at panel" },
        { id:"su7",  nec:"NEC 408.4",       text:"All circuits labeled on directory" },
        { id:"su8",  nec:"NEC 110.26",      text:"36 in. working clearance maintained in front of panel" },
        { id:"su9",  nec:"NEC 230.85",      text:"Exterior emergency disconnect installed (required 2023)" },
        { id:"su10", nec:"NEC 408.7",       text:"All unused knockouts sealed" },
      ],
    },
    new_circuit: {
      label: "New Circuit / Rough-In",
      items: [
        { id:"nc1",  nec:"NEC 210.12",      text:"AFCI protection on all required circuits" },
        { id:"nc2",  nec:"NEC 210.8",       text:"GFCI protection in all required locations" },
        { id:"nc3",  nec:"NEC 300.4",       text:"Nail plates installed within 1.25 in. of framing edge" },
        { id:"nc4",  nec:"NEC 300.14",      text:"6 in. free conductor at all boxes" },
        { id:"nc5",  nec:"NEC 314.16",      text:"Box fill calculations verified (no overfill)" },
        { id:"nc6",  nec:"NEC 334.30",      text:"NM cable secured within 12 in. of all boxes" },
        { id:"nc7",  nec:"NEC 240.4",       text:"Conductor sized correctly for overcurrent device" },
        { id:"nc8",  nec:"NEC 110.12",      text:"Workmanlike installation — cables properly routed" },
        { id:"nc9",  nec:"NEC 406.12",      text:"All receptacles are tamper-resistant (TR rated)" },
        { id:"nc10", nec:"NEC 210.52",      text:"Outlet spacing verified (no point more than 6 ft from outlet)" },
      ],
    },
    pool: {
      label: "Pool / Spa",
      items: [
        { id:"p1",   nec:"NEC 680.26",      text:"Equipotential bonding complete — all metal within 5 ft bonded" },
        { id:"p2",   nec:"NEC 680.22",      text:"Receptacles at least 6 ft from pool edge (GFCI protected)" },
        { id:"p3",   nec:"NEC 680.22",      text:"No luminaires within 12 ft horizontally of pool water" },
        { id:"p4",   nec:"NEC 680.21(C)",   text:"GFCI protection on all pump motor circuits" },
        { id:"p5",   nec:"NEC 680.23",      text:"Underwater lighting properly niched and rated" },
        { id:"p6",   nec:"NEC 680.12",      text:"Equipment disconnect within sight of pool equipment" },
        { id:"p7",   nec:"NEC 680.6",       text:"All equipment within 5 ft of pool grounded (insulated GEC)" },
        { id:"p8",   nec:"NEC 680.42",      text:"Spa/hot tub has GFCI protection on all circuits" },
      ],
    },
    final_inspection: {
      label: "Final Inspection",
      items: [
        { id:"fi1",  nec:"NEC 314.25",      text:"All boxes have covers or faceplates" },
        { id:"fi2",  nec:"NEC 408.4",       text:"Panel directory complete and legible" },
        { id:"fi3",  nec:"NEC 410.16",      text:"Closet lighting fixtures are LED (no incandescent)" },
        { id:"fi4",  nec:"NEC 200.11",      text:"All outlets tested — no reversed polarity" },
        { id:"fi5",  nec:"NEC 210.8",       text:"GFCI outlets tested and functional" },
        { id:"fi6",  nec:"NEC 210.12",      text:"AFCI breakers tested (test button)" },
        { id:"fi7",  nec:"NEC 760.41",      text:"Smoke detectors interconnected and functional" },
        { id:"fi8",  nec:"NEC 110.3(B)",    text:"All equipment installed per listing/labeling" },
        { id:"fi9",  nec:"NEC 230.67",      text:"Surge protector installed and indicator light green" },
        { id:"fi10", nec:"NEC 110.26",      text:"Panel working clearance verified and unobstructed" },
        { id:"fi11", nec:"NEC 406.9",       text:"All outdoor outlets have in-use weatherproof covers" },
        { id:"fi12", nec:"NEC 250.53",      text:"Ground rods driven full depth (8 ft min)" },
      ],
    },
  };

export { CATEGORIES, MARKUP_OPTIONS, HOURLY_RATES, ALL_SERVICES, CHECKLISTS };
