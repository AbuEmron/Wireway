import { useState, useMemo } from "react";

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
      // Hot tub: ~8 hr — trench or conduit, 240V/50A, GFCI, bonding, disconnect
      { id: "hot_tub",          label: "Hot Tub / Spa Install",             nec: "NEC 680.42",      materialCost: 380,  laborCost: 680,  laborHours: 8,   unit: "unit",    variants: [{ label: "Plug-in NEMA 14-50", m: 1 }, { label: "Hardwired 240V/50A", m: 1.4 }, { label: "With GFCI + Bonding", m: 1.6 }] },
      { id: "ev_parking",       label: "Driveway / Parking EV Outlet",      nec: "NEC 625.40",      materialCost: 300,  laborCost: 510,  laborHours: 6,   unit: "unit",    variants: [{ label: "NEMA 14-50 Outdoor", m: 1 }, { label: "Level 2 EVSE Pedestal", m: 1.6 }] },
    ],
  },
];

// ─── NEC 2023 RESIDENTIAL REFERENCE ─────────────────────────────────────────
const NEC_REF = [
  {
    article: "90", title: "Introduction", color: "#e8c97a",
    summary: "Scope, purpose, and authority of the NEC. Establishes that the Code covers the installation of electrical conductors and equipment within or on public and private buildings.",
    rules: [
      { code: "90.1", title: "Purpose", text: "The purpose of this Code is the practical safeguarding of persons and property from hazards arising from the use of electricity. Not intended as a design specification or instruction manual." },
      { code: "90.2", title: "Scope", text: "Covers electrical conductors and equipment installed within or on buildings, structures, and other premises including mobile homes, RVs, floating buildings, and yards." },
      { code: "90.4", title: "Enforcement", text: "The authority having jurisdiction (AHJ) enforces this Code. The AHJ can waive specific requirements or permit alternative methods when equivalent safety is achieved." },
      { code: "90.7", title: "Listed Equipment", text: "Examination of equipment for safety purposes is the responsibility of qualified testing organizations. Listing labels are evidence of compliance with applicable standards." },
    ],
    violations: ["Installing equipment not listed or approved by the AHJ", "Failing to get required permits and inspections"],
  },
  {
    article: "100", title: "Definitions", color: "#e8c97a",
    summary: "Defines all terms used throughout the NEC. Understanding these definitions is critical because many code violations stem from misinterpreting what a term means.",
    rules: [
      { code: "100", title: "Ampacity", text: "The maximum current, in amperes, that a conductor can carry continuously under the conditions of use without exceeding its temperature rating." },
      { code: "100", title: "Arc-Fault Circuit Interrupter (AFCI)", text: "A device intended to provide protection from the effects of arc faults by recognizing characteristics unique to arcing and by functioning to de-energize the circuit." },
      { code: "100", title: "Branch Circuit", text: "The circuit conductors between the final overcurrent device protecting the circuit and the outlet(s)." },
      { code: "100", title: "Dwelling Unit", text: "A single unit providing complete and independent living facilities for one or more persons, including permanent provisions for living, sleeping, cooking, and sanitation." },
      { code: "100", title: "Ground-Fault Circuit Interrupter (GFCI)", text: "A device intended to protect personnel from shock by de-energizing a circuit when current to ground exceeds a predetermined value (typically 4–6 mA)." },
      { code: "100", title: "Overcurrent", text: "Any current in excess of the rated current of equipment or the ampacity of a conductor. Overcurrent may result from overload, short circuit, or ground fault." },
      { code: "100", title: "Service Entrance", text: "The conductors and equipment for delivering energy from the serving utility to the wiring system of the premises served." },
    ],
    violations: [],
  },
  {
    article: "110", title: "Requirements for Electrical Installations", color: "#7eb8e8",
    summary: "General requirements for all electrical installations: working clearances, examination of equipment, mounting, and connections. One of the most cited articles on residential inspections.",
    rules: [
      { code: "110.3(B)", title: "Installation of Listed Equipment", text: "Listed and labeled equipment shall be installed and used in accordance with any instructions included in the listing or labeling. Breakers must go in listed panels; fixtures installed per manufacturer specs." },
      { code: "110.9", title: "Interrupting Rating", text: "Equipment intended to interrupt current at fault levels shall have an interrupting rating sufficient for the nominal circuit voltage and the current that is available at the line terminals of the equipment." },
      { code: "110.12", title: "Mechanical Execution of Work", text: "Electrical equipment shall be installed in a neat and workmanlike manner. Open wiring, unsupported cables, junction boxes buried in walls without covers all violate this." },
      { code: "110.14", title: "Electrical Connections", text: "Terminals for more than one conductor and terminals for conductors not of the same material (Al vs Cu) shall be so identified. Use only Al-rated terminals for aluminum wire." },
      { code: "110.26", title: "Spaces About Electrical Equipment", text: "Working space: 30 in. wide minimum, 36 in. deep for 0–150V to ground (most residential), 78 in. headroom. Must be kept clear at all times — not used for storage." },
      { code: "110.27", title: "Guarding of Live Parts", text: "Live parts of electrical equipment operating at 50V or more shall be guarded against accidental contact by approved enclosures, location, or elevation." },
    ],
    violations: ["Panel blocked by shelving, water heater, or equipment", "Open knockouts in panels", "Conductors not properly terminated", "Exposed wiring in finished spaces"],
  },
  {
    article: "200", title: "Use and Identification of Grounded Conductors", color: "#a8e87e",
    summary: "Rules for the white (neutral) wire — identification, use, and where it can and cannot be used. Improper neutral identification is a top inspection failure.",
    rules: [
      { code: "200.6", title: "Means of Identifying Grounded Conductors", text: "Conductors #6 AWG or smaller must be identified by white or gray insulation, or three white/gray stripes. Larger conductors may be re-identified at terminations with white tape." },
      { code: "200.7(C)", title: "Use of White/Gray for Other Than Grounded", text: "A cable with white insulation may be re-identified for use as an ungrounded conductor only at terminations, using tape, painting, or other permanent markings. Common in switch loops." },
      { code: "200.9", title: "Means of Identification of Terminals", text: "The terminal for connection of the grounded conductor shall be identified with a white metal, silver-colored screw, or marking — used on outlets, fixtures, and devices." },
      { code: "200.11", title: "Polarity of Connections", text: "No grounded conductor shall be attached to any terminal or lead so as to reverse the designated polarity. Reversed polarity is a common outlet wiring error caught by plug-in testers." },
    ],
    violations: ["White wire used as hot without re-identification", "Reversed polarity at outlets", "Neutral wire connected to wrong terminal on device"],
  },
  {
    article: "210", title: "Branch Circuits", color: "#7eb8e8",
    summary: "The heart of residential wiring. Covers required circuits, outlet placement rules, GFCI and AFCI requirements, load calculations, and small appliance circuits. Most residential work touches this article.",
    rules: [
      { code: "210.8", title: "GFCI Protection — Dwelling Units", text: "Required in: bathrooms, garages, outdoors, crawl spaces, unfinished basements, kitchens (within 6 ft of sink), boathouses, bathtub/shower areas, laundry areas, and all 15A/20A 125V outlets in dwelling units (2023). All kitchen countertop receptacles require GFCI regardless of distance from sink." },
      { code: "210.11(C)", title: "Dwelling Unit Branch Circuits", text: "At least two 20A small-appliance circuits required for kitchen/dining. One 20A circuit for bathroom. One 20A or larger for laundry. One 20A for garage. These cannot serve other outlets." },
      { code: "210.12", title: "AFCI Protection", text: "Required for all 15A and 20A 125V branch circuits supplying outlets in: all dwelling unit areas (kitchens, family rooms, dining rooms, living rooms, parlors, libraries, dens, bedrooms, sunrooms, recreation rooms, closets, hallways, laundry areas, similar areas)." },
      { code: "210.17", title: "EV Charging — Outlet Required in Garage", text: "In new one- and two-family dwellings, at least one 208/240V branch circuit dedicated to EV charging must be installed in the garage. Must be at least 40A and installed in conduit." },
      { code: "210.19", title: "Conductors — Minimum Ampacity", text: "Branch circuit conductors shall have an ampacity not less than the maximum load to be served. For 15A circuits: min 14 AWG. For 20A: min 12 AWG. For 30A: min 10 AWG. For 40A: min 8 AWG." },
      { code: "210.52", title: "Dwelling Unit Receptacle Outlets", text: "Wall outlets: no point along wall can be more than 6 ft from a receptacle (i.e., outlets every 12 ft). Countertops: every 4 ft, within 2 ft of countertop end. Bathrooms: at least one within 3 ft of each basin. Outdoors: at least one front and rear at grade. Garage: at least one per attached car space. Basement: at least one. Hallways 10 ft+: at least one." },
      { code: "210.63", title: "HVAC Outlet", text: "At least one 125V 15A or 20A outlet must be installed within 25 ft of HVAC equipment for service use (humidifiers, air handlers, etc.), accessible from the equipment location." },
      { code: "210.64", title: "Electrical Service Areas", text: "At least one 125V 15A or 20A outlet shall be installed within 50 ft of the service equipment or panelboard, accessible without moving other equipment." },
    ],
    violations: ["Missing GFCI in garage, bathroom, exterior, laundry", "Missing AFCI on bedroom circuits", "Kitchen outlets not on small-appliance circuit", "Outlets spaced more than 12 ft apart", "No EV-ready circuit in new garage builds", "Missing outdoor outlets front and rear"],
  },
  {
    article: "215", title: "Feeders", color: "#7eb8e8",
    summary: "Rules for the wiring between the service panel and sub-panels. Feeder sizing, GFCI protection requirements, and load balancing for sub-panels in detached structures.",
    rules: [
      { code: "215.2", title: "Minimum Rating and Size", text: "Feeders shall have an ampacity not less than required to supply the load as computed in Parts III, IV, and V of Article 220. Feeder conductors sized to carry the connected load." },
      { code: "215.6", title: "Feeder Overcurrent Protection", text: "Feeders shall be protected against overcurrent in accordance with Article 240. The overcurrent device must be sized to protect the feeder conductors." },
      { code: "215.12", title: "Identification of Feeders", text: "Feeder conductors shall be identified at all termination, connection, and splice points. Color coding or permanent marking required — especially important in panels with multiple feeders." },
    ],
    violations: ["Undersized feeder conductors", "Missing disconnect at detached structure", "Improper neutral-ground bonding at sub-panel (bond only at main panel)"],
  },
  {
    article: "220", title: "Branch Circuit, Feeder, and Service Load Calculations", color: "#e8c97a",
    summary: "How to calculate the electrical load for sizing panels, feeders, and services. Required for any service upgrade or addition to ensure the system isn't overloaded.",
    rules: [
      { code: "220.12", title: "Lighting Load", text: "General lighting: 3 VA per square foot for dwelling units. Calculated on the outside dimensions of the dwelling, not livable space." },
      { code: "220.14", title: "Other Loads", text: "Outlets other than those covered by 220.12 are counted at minimum 180 VA each. Receptacles supplying specific loads are calculated at their actual load." },
      { code: "220.52", title: "Small Appliance and Laundry Loads", text: "Two 20A small-appliance circuits at 1,500 VA each (3,000 VA total). One 20A laundry circuit at 1,500 VA. These are added to the general lighting load." },
      { code: "220.54", title: "Clothes Dryers", text: "5,000 VA or the nameplate rating, whichever is larger, for each dryer circuit. Multiple dryers: use Table 220.54 demand factors." },
      { code: "220.55", title: "Cooking Appliances", text: "Use Table 220.55 for household electric ranges, wall-mounted ovens, and counter-mounted cooking units. Demand factors apply based on number and size of appliances." },
      { code: "220.82", title: "Optional Calculation for Dwelling Units", text: "Simplified load calculation for one-family dwellings. Allows 100% of first 10,000 VA + 40% of remainder. Widely used for service upgrade calculations." },
      { code: "220.83", title: "Optional Method — Adding AC Load", text: "When adding central AC, use 100% of the AC load plus 40% of other loads, if larger than the existing heat load. Used to determine if panel upgrade is needed." },
    ],
    violations: ["Service undersized for actual load", "Failing to include all loads in calculation", "Not applying demand factors correctly"],
  },
  {
    article: "225", title: "Outside Branch Circuits and Feeders", color: "#e8d47e",
    summary: "Rules for wiring to detached garages, outbuildings, sheds, and other structures on the same property. Covers overhead and underground runs, disconnects, and grounding.",
    rules: [
      { code: "225.6", title: "Conductor Size and Support", text: "Overhead conductors for spans up to 50 ft: min 10 AWG for copper. Over 50 ft: min 8 AWG. Open conductors on insulators must be supported within 12 in. of attachment." },
      { code: "225.18", title: "Clearance for Overhead Conductors", text: "Minimum clearances: 10 ft above finished grade, sidewalks, platforms. 12 ft over residential driveways. 18 ft over public streets and alleys. 3 ft from windows, doors, porches." },
      { code: "225.30", title: "Number of Supplies", text: "Only one feeder or branch circuit shall supply a separate structure (with exceptions). Additional circuits allowed only if all are protected by a single disconnect." },
      { code: "225.32", title: "Location of Disconnect", text: "The disconnecting means shall be installed either inside or outside the separate structure nearest the point of entry of the supply conductors — accessible without entering living space." },
      { code: "225.36", title: "Suitable for Service Entrance", text: "The disconnecting means in a separate structure shall be suitable for use as service equipment and be rated for the load." },
    ],
    violations: ["No disconnect at detached garage or shed", "Overhead clearances not met", "Neutral and ground bonded at sub-panel in separate structure (must be separate — bond only at main)"],
  },
  {
    article: "230", title: "Services", color: "#e8c97a",
    summary: "Everything about the utility service entrance: overhead and underground service, service conductor sizing, service entrance equipment, disconnecting means, and grounding. Critical for any service upgrade.",
    rules: [
      { code: "230.6", title: "Conductors Considered Outside Building", text: "Conductors under at least 2 in. of concrete beneath a building are considered outside the building — relevant for underground service entrance routing." },
      { code: "230.23", title: "Size and Rating", text: "Service entrance conductors shall have sufficient ampacity for the load as calculated per Article 220, and not less than 100A for a one-family dwelling (230.79(C))." },
      { code: "230.24", title: "Clearances", text: "Above roofs: 8 ft minimum (3 ft if slope exceeds 4:12). Above ground: 10 ft residential driveways, 12 ft over vehicles, 18 ft over public roads. Must maintain clearance from windows: 3 ft." },
      { code: "230.42", title: "Minimum Size and Rating", text: "Service conductors shall be sized to carry the calculated load without exceeding the temperature rating of the conductors." },
      { code: "230.66", title: "Classified Equipment", text: "Service equipment shall be listed and identified as suitable for use as service equipment. Must display maximum service rating." },
      { code: "230.67", title: "Surge Protection", text: "NEW in NEC 2023: Surge protection (SPD) is now required for all services supplying dwelling units. Must be listed Type 1 or Type 2." },
      { code: "230.70", title: "General — Service Disconnect", text: "Means shall be provided to disconnect all conductors in a building or structure from the service entrance conductors. Maximum of six disconnects (six throws of the hand rule)." },
      { code: "230.71", title: "Maximum Number of Disconnects", text: "The service disconnecting means shall consist of not more than six switches or six circuit breakers mounted in a single enclosure, a group of enclosures, or in a switchboard or panelboard." },
      { code: "230.79", title: "Rating of Service Disconnect", text: "One-family dwelling: minimum 100A (C). Two-family or multi-family: minimum 100A per unit (D). All others: minimum 60A." },
    ],
    violations: ["Surge protection missing on new/upgraded services (2023 requirement)", "Service clearances not maintained", "Service panel missing dead-front cover", "Open knockouts in service equipment", "More than six disconnects without main breaker"],
  },
  {
    article: "240", title: "Overcurrent Protection", color: "#e87e7e",
    summary: "Rules for fuses and circuit breakers: sizing, placement, and protection of conductors. Critical for proper panel wiring and breaker sizing.",
    rules: [
      { code: "240.4", title: "Protection of Conductors", text: "Conductors shall be protected against overcurrent in accordance with their ampacities. 14 AWG: max 15A. 12 AWG: max 20A. 10 AWG: max 30A. 8 AWG: max 40-50A depending on conditions." },
      { code: "240.6", title: "Standard Ampere Ratings", text: "Standard fuse and fixed-trip circuit breaker ratings: 15, 20, 25, 30, 35, 40, 45, 50, 60, 70, 80, 90, 100, 110, 125, 150, 175, 200, 225, 250, 300, 350, 400A." },
      { code: "240.21", title: "Location in Circuit", text: "Overcurrent protection shall be provided at the point where the conductor receives its supply. Tap rules (10 ft, 25 ft) allow some flexibility for larger conductors with specific conditions." },
      { code: "240.24", title: "Location in or on Premises", text: "Overcurrent devices shall be readily accessible. Not located in bathrooms, over stairs, or in clothes closets. Must be accessible without removing panels or equipment." },
      { code: "240.83", title: "Marking", text: "Circuit breakers shall be durably marked with their ampere rating and interrupting rating. Must be legible without removing any covers." },
    ],
    violations: ["14 AWG wiring on 20A breaker", "12 AWG wiring on 30A breaker", "Panel breakers not labeled", "Double-tapped breakers (two conductors on one breaker not rated for it)", "Oversized fuses or breakers"],
  },
  {
    article: "250", title: "Grounding and Bonding", color: "#a8e87e",
    summary: "One of the most complex and most-violated articles. Covers the entire grounding system: grounding electrode system, equipment grounding conductors, bonding, and sizing. Errors here are safety-critical.",
    rules: [
      { code: "250.4", title: "General Requirements", text: "Grounded systems: electrical systems shall be connected to earth to limit voltage imposed by lightning, line surges, or unintentional contact with higher-voltage lines. Equipment grounding: non-current-carrying conductive parts of equipment shall be connected together and to the supply system grounded conductor." },
      { code: "250.24", title: "Grounding Service-Supplied AC Systems", text: "The main panel neutral bar and ground bar shall be bonded together (only at the main panel, not at sub-panels). The grounded conductor must be connected to the grounding electrode system." },
      { code: "250.28", title: "Main Bonding Jumper", text: "For a grounded system, the main bonding jumper shall connect the grounded conductor to the equipment grounding conductor and the enclosure. Sized per Table 250.28." },
      { code: "250.50", title: "Grounding Electrode System", text: "All grounding electrodes present at a structure must be bonded together into a single grounding electrode system: ground rods, ground plates, buried metal water pipe (10 ft+), concrete-encased electrode (Ufer), ground ring, structural steel." },
      { code: "250.52", title: "Grounding Electrodes", text: "Acceptable electrodes: metal underground water pipe (in contact with earth for 10+ ft), metal frame of building (effectively grounded), concrete-encased electrode (20 ft of rebar or wire in concrete footer), ground ring, rod and pipe electrodes (min 8 ft long, 5/8 in. diameter)." },
      { code: "250.53", title: "Grounding Electrode Installation", text: "Rod electrodes shall be driven to a depth of 8 ft. If rock is encountered, the rod may be driven at up to 45° or buried in a 30 in. trench. Where a single rod has resistance over 25 ohms, a second electrode must be added." },
      { code: "250.66", title: "Size of Alternating Current Grounding Electrode Conductor", text: "GEC sizing based on size of largest service entrance conductor per Table 250.66. For 2/0–3/0 Cu service conductors, GEC is minimum 4 AWG Cu. For 350 kcmil and larger, minimum 2 AWG Cu." },
      { code: "250.104", title: "Bonding of Piping Systems", text: "Metal water piping systems in a building must be bonded to the service equipment enclosure, grounded conductor at service, or grounding electrode conductor. Gas piping bonded to the equipment grounding conductor." },
      { code: "250.118", title: "Types of Equipment Grounding Conductors", text: "Acceptable EGC types: copper or other corrosion-resistant wire, rigid metal conduit, intermediate metal conduit, EMT, armor of AC cable (BX), copper sheath of MI cable." },
      { code: "250.119", title: "Identification of Equipment Grounding Conductors", text: "EGCs of wire type: green insulation, green with yellow stripe, or bare copper. Conductors 4 AWG and larger may be re-identified with green tape at terminations." },
      { code: "250.122", title: "Size of Equipment Grounding Conductors", text: "EGC minimum sizes per Table 250.122 based on overcurrent device rating. 15A circuit: min 14 AWG. 20A: min 12 AWG. 30A: min 10 AWG. 60A: min 10 AWG. 100A: min 8 AWG." },
    ],
    violations: ["Neutral and ground bonded in sub-panel", "Missing ground rod or not driven full 8 ft", "Water pipe not bonded", "Gas pipe not bonded", "Green wire used as hot or neutral", "Missing main bonding jumper", "GEC undersized for service conductors"],
  },
  {
    article: "300", title: "Wiring Methods — General", color: "#7eb8e8",
    summary: "General installation rules that apply to all wiring methods: securing, protecting, splicing, routing through framing, and fill calculations. These requirements apply everywhere.",
    rules: [
      { code: "300.4", title: "Protection Against Physical Damage", text: "Where subject to physical damage, conductors shall be protected. Cables through framing members: must be at least 1¼ in. from edge of framing, or use nail plates. Cables in notches must be protected." },
      { code: "300.5", title: "Underground Installations", text: "Minimum cover requirements: 24 in. for 120V, 18 in. for RMC/IMC, 6 in. for rigid nonmetallic conduit under a 4 in. concrete slab. Direct burial cable rated for use." },
      { code: "300.6", title: "Protection Against Corrosion", text: "Metal raceways, fittings, boxes, and enclosures shall be protected inside and outside against corrosion. Ferrous metal raceways in concrete or in contact with soil must be corrosion-resistant." },
      { code: "300.11", title: "Securing and Supporting", text: "Wiring methods shall be secured and supported in accordance with specific article requirements. Cannot use ceiling grid support wires to support wiring unless permitted by specific articles." },
      { code: "300.14", title: "Length of Free Conductors at Outlets", text: "At outlets, at least 6 in. of free conductor (measured from the point of entry into the enclosure) shall be left at each outlet, junction, and switch point." },
      { code: "300.15", title: "Boxes, Conduit Bodies, or Fittings Required", text: "A box, conduit body, or fitting shall be installed at each conductor splice point, outlet, switch point, junction point, or pull point. No buried splices without junction boxes." },
      { code: "300.17", title: "Number and Size of Conductors in Raceway", text: "Conductors shall not fill a raceway to more than the percentage fill allowed by the specific wiring method. Use Chapter 9 tables for conduit fill calculations." },
      { code: "300.22", title: "Wiring in Ducts, Plenums, and Air-Handling Spaces", text: "Only wiring methods with plenum-rated cables permitted in air-handling spaces. Standard NM-B (Romex) is not permitted in plenum spaces." },
    ],
    violations: ["Cables not protected within 1¼ in. of framing edge", "No nail plates where cables are exposed to nails/screws", "Less than 6 in. of free conductor at boxes", "Splices made outside of boxes", "NM cable in plenum spaces"],
  },
  {
    article: "310", title: "Conductors for General Wiring", color: "#7eb8e8",
    summary: "Conductor ampacity tables, temperature ratings, and insulation types. Used any time you need to size wire for a given load or verify existing wire is adequate.",
    rules: [
      { code: "310.4", title: "Conductors in Parallel", text: "Conductors of size 1/0 AWG and larger may be run in parallel. Each set must be same length, same AWG, same insulation type, and terminated the same way. Smaller conductors cannot be paralleled." },
      { code: "310.14", title: "Aluminum Conductors", text: "Aluminum conductors must use terminals rated for aluminum (AL/CU or AL rated). Aluminum conductors #12 and #10 AWG are listed for specific uses. Anti-oxidant compound required at terminations per manufacturer instructions." },
      { code: "310.15", title: "Ampacity", text: "Table 310.15(B)(16): 14 AWG Cu = 15A; 12 AWG Cu = 20A; 10 AWG Cu = 30A; 8 AWG Cu = 40-50A; 6 AWG Cu = 55-65A; 4 AWG Cu = 70-85A; 2 AWG Cu = 95-115A; 1/0 AWG Cu = 120-150A; 2/0 AWG Cu = 135-175A; 3/0 AWG Cu = 155-200A. Derate for bundling (more than 3 current-carrying conductors), elevated temperature, and conduit in direct sunlight." },
      { code: "310.15(B)(3)", title: "Adjustment Factors", text: "When more than 3 current-carrying conductors are in a raceway or cable, ampacity must be derated: 4–6 conductors = 80%; 7–9 = 70%; 10–20 = 50%. Neutral conductors that carry unbalanced loads count as current-carrying." },
    ],
    violations: ["14 AWG on 20A circuits", "Aluminum conductors on Cu-only terminals", "Not derating bundled conductors in conduit", "Paralleling conductors smaller than 1/0 AWG"],
  },
  {
    article: "314", title: "Outlet, Device, Pull, and Junction Boxes", color: "#e8b87e",
    summary: "Box selection, sizing, fill calculations, mounting, and covers. Box fill violations are extremely common — too many wires crammed into a box is a fire and safety hazard.",
    rules: [
      { code: "314.16", title: "Number of Conductors in Outlet, Device, and Junction Boxes", text: "Box fill calculation: each conductor entering box = 1 unit (14 AWG = 2.0 cu in., 12 AWG = 2.25 cu in., 10 AWG = 2.5 cu in.). Add: 1 unit for all ground wires combined, 1 unit for each strap-mounted device (outlet, switch), 1 unit for all cable clamps. Total must not exceed box volume." },
      { code: "314.17", title: "Conductors Entering Boxes, Conduit Bodies, or Fittings", text: "Conductors entering through a cable connector or fitting must be protected from abrasion. All entries must have fittings or connectors — open holes in boxes are a violation." },
      { code: "314.20", title: "In Wall or Ceiling", text: "Boxes in walls or ceilings of concrete, tile, or similar material must be flush with the finished surface or project beyond it. In combustible material (drywall, wood paneling), box must be flush with the finished surface." },
      { code: "314.23", title: "Securing and Supporting", text: "Boxes shall be securely supported. Boxes mounted to structural members directly or with mounting brackets. Fan-rated boxes required where ceiling fans will be installed — standard boxes not rated for fan weight and motion." },
      { code: "314.25", title: "Covers and Canopies", text: "All boxes shall be provided with a cover, faceplate, or fixture canopy. Abandoned wiring in boxes must still have covers. No open boxes in finished spaces." },
      { code: "314.27", title: "Outlet Boxes for Fixtures", text: "Boxes used for fixture support: rated for the weight of the fixture. Fixtures over 50 lbs require independent support. Ceiling fan boxes must be listed for fan support (314.27(D)) — standard boxes can collapse under fan motion." },
    ],
    violations: ["Overfilled boxes (most common NEC violation)", "No cover on junction boxes", "Standard box used for ceiling fan", "Box recessed behind finished wall surface", "Open knockouts in boxes", "Unsupported boxes"],
  },
  {
    article: "334", title: "Nonmetallic-Sheathed Cable (NM/NM-B/Romex)", color: "#7eb8e8",
    summary: "Rules for the most common residential wiring method. NM-B (Romex) is used throughout homes but has specific limitations — it cannot be used in many commercial applications or exposed in areas subject to physical damage.",
    rules: [
      { code: "334.10", title: "Uses Permitted", text: "NM cable permitted in: one- and two-family dwellings, multi-family dwellings up to and including medium construction (not in areas subject to physical damage), and other structures not exceeding 3 stories above grade." },
      { code: "334.12", title: "Uses Not Permitted", text: "NM cable not permitted: exposed in dropped ceilings of commercial or institutional occupancies, embedded in poured concrete, exposed where subject to physical damage, in any structure exceeding 3 stories, in theaters, or in hazardous locations." },
      { code: "334.15", title: "Exposed Work", text: "In exposed work, cable shall follow the surface of building finish or be protected from damage. Must be secured within 12 in. of every box and at intervals not exceeding 4.5 ft." },
      { code: "334.17", title: "Through or Parallel to Framing Members", text: "Cable shall be protected where passing through framing members. Where closer than 1.25 in. to the edge, steel nail plates (at least 1/16 in. thick) are required." },
      { code: "334.30", title: "Securing and Supporting", text: "NM cable shall be secured within 12 in. of each box, conduit body, or fitting. Must be supported at intervals not exceeding 4.5 ft. Unsupported loops or sagging cable violates this." },
      { code: "334.80", title: "Ampacity", text: "Ampacity of NM cable limited by the 60°C column of Table 310.15(B)(16), regardless of conductor insulation temperature rating. This is why 12 AWG NM-B is rated 20A even though the wire itself might be 90°C rated." },
    ],
    violations: ["NM cable in conduit used outdoors or exposed to moisture", "NM cable in commercial buildings", "Cable not secured within 12 in. of box", "No nail plates where cable is within 1.25 in. of framing edge", "NM cable in poured concrete"],
  },
  {
    article: "358", title: "Electrical Metallic Tubing (EMT)", color: "#7eb8e8",
    summary: "Rules for EMT conduit — the most common metal raceway used in residential exposed work, garages, and service entrances. Covers installation, bending, and support requirements.",
    rules: [
      { code: "358.10", title: "Uses Permitted", text: "EMT permitted for exposed and concealed work in all occupancies. Permitted outdoors where not subject to physical damage. Listed for direct burial where marked. Not for severe physical damage areas — use RMC or IMC there." },
      { code: "358.20", title: "Size", text: "Minimum trade size 1/2 in. for most applications. Maximum trade size 4 in." },
      { code: "358.24", title: "Bends", text: "Bends shall be made so that the conduit is not damaged. Total bends between pull points (junction boxes, conduit bodies) shall not exceed 360°. Use proper bending tools — EMT cannot be bent with a pipe wrench." },
      { code: "358.26", title: "Number of Conductors", text: "Number of conductors as permitted by Chapter 9 fill tables. One conductor: 53% fill. Two conductors: 31% fill. Three or more: 40% fill of interior cross-sectional area." },
      { code: "358.30", title: "Securing and Supporting", text: "EMT shall be securely fastened within 3 ft of every outlet box, junction box, or fitting. Supported at intervals not exceeding 10 ft." },
    ],
    violations: ["More than 360° total bends between pull points", "EMT not supported within 3 ft of boxes", "Using EMT where subject to severe physical damage without protection", "Pulling too many conductors (exceeding fill)"],
  },
  {
    article: "404", title: "Switches", color: "#e87eb8",
    summary: "Requirements for all switches: grounding, neutral wire at switch boxes (new 2023 requirement), ratings, and grouping. The neutral-at-switch-box rule is a major change for smart switch installations.",
    rules: [
      { code: "404.2(C)", title: "Grounded Conductor at Switch Box — NEC 2023", text: "NEW: Where a grounded conductor exists in a switch enclosure, it must be connected to a metal switch box or a device that requires a neutral. This enables future smart switch installation without running a new neutral wire. Required in all new installations." },
      { code: "404.6", title: "Position and Connection of Switches", text: "Single-throw knife switches shall be installed so gravity will not tend to close them. Switches controlling equipment shall be positioned so the equipment may be safely operated." },
      { code: "404.7", title: "Rating and Use", text: "General-use snap switches shall be used only for controlling the following loads: resistive and inductive loads (motors limited to 80% of switch rating). Tungsten filament lamp loads limited to switches rated for tungsten filament lamps." },
      { code: "404.9", title: "Provisions for Snap Switches", text: "Snap switches shall be effectively grounded. Metal faceplates must be grounded through the device or mounting screw. Non-metallic faceplates permitted only on non-grounding circuits where no ground wire exists." },
      { code: "404.14", title: "Rating and Use of Snap Switches", text: "Dimmers shall be used only with loads for which they are rated. LED dimmers must be compatible with the specific LED drivers used. Dimmer rating must match load type (LED/incandescent not interchangeable)." },
    ],
    violations: ["Missing neutral at switch box in new construction (2023)", "Dimmer used with incompatible LED load causing flicker or premature failure", "Switch controlling motor loads without proper motor rating", "Ungrounded metal switch plates"],
  },
  {
    article: "406", title: "Receptacles, Cord Connectors, and Attachment Plugs", color: "#a8e87e",
    summary: "Rules for all receptacles: tamper-resistance, weather resistance, GFCI marking, replacement requirements, and face-up installation. Tamper-resistant and weather-resistant requirements expanded significantly in NEC 2023.",
    rules: [
      { code: "406.4(D)", title: "Replacements", text: "Replacement receptacles must: (1) be GFCI-protected if in a GFCI-required location per 210.8; (2) be tamper-resistant if replacing a TR outlet; (3) be grounded if a grounding means exists. Cannot replace 3-prong with 2-prong if ground exists." },
      { code: "406.9", title: "Receptacles in Damp or Wet Locations", text: "Outdoor receptacles require in-use covers (weatherproof while plug is inserted). Wet locations: listed in-use covers while receptacle is being used. Damp locations: acceptable when cover closed." },
      { code: "406.12", title: "Tamper-Resistant Receptacles", text: "All non-locking 15A and 20A 125V and 250V receptacles in dwelling units must be tamper-resistant (TR rated). Applies to all new installations and replacements in dwelling units. TR outlets have internal shutters that prevent insertion of foreign objects." },
      { code: "406.15", title: "Dimmer-Controlled Receptacles", text: "Dimmer switches shall not control receptacle outlets (only lighting outlets), unless listed for the purpose. Standard dimmers on receptacle circuits are a violation." },
    ],
    violations: ["Non-tamper-resistant receptacles in dwelling units", "No in-use cover on outdoor receptacles", "Replacing GFCI-required outlet without GFCI protection", "Dimmer switch on receptacle circuit"],
  },
  {
    article: "408", title: "Switchboards, Switchgear, and Panelboards", color: "#e8c97a",
    summary: "Requirements for residential load centers and panelboards: labeling, clearances, conductor organization, overcurrent device limits, and working space. Panel violations are very common.",
    rules: [
      { code: "408.4", title: "Circuit Directory", text: "All circuits and circuit modifications shall be legibly identified. Identification shall be available on the circuit directory located on the face or inside the door of the panel. 'Unknown' or 'misc' is not acceptable — every circuit must be labeled." },
      { code: "408.7", title: "Unused Openings", text: "Unused openings in panelboard enclosures shall be closed. Open knockouts allow fingers, rodents, or debris to enter live parts." },
      { code: "408.36", title: "Overcurrent Protection of Panelboards", text: "Panelboards shall be protected by an overcurrent device having a rating not greater than that of the panelboard. A 200A panel requires a 200A main breaker or larger feeder protection." },
      { code: "408.54", title: "Maximum Number of Overcurrent Devices", text: "A residential panelboard shall not contain more than 42 overcurrent devices (excluding the main breaker). Tandem breakers can be used in approved spaces to add circuits." },
      { code: "408.55", title: "Wire-Bending Space in Panelboards", text: "Sufficient wire-bending space must be maintained at all terminals. Cramming conductors into a panel without proper bending space can damage insulation." },
    ],
    violations: ["Unlabeled circuits", "Open knockouts in panel", "Panel amperage exceeded by connected breakers", "Double-tapped breakers (two wires on one breaker terminal not rated for two)", "Conductors from multiple circuits sharing a neutral (shared neutrals without handle ties on MWBC)"],
  },
  {
    article: "410", title: "Luminaires, Lampholders, and Lamps", color: "#e8b87e",
    summary: "Requirements for all lighting fixtures: mounting, wiring, clearances, recessed fixture installation (IC vs non-IC), wet and damp location ratings, and weight support. Recessed lighting violations are extremely common.",
    rules: [
      { code: "410.10", title: "Luminaires in Specific Locations", text: "Wet locations: listed for wet locations. Damp locations: listed for damp or wet locations. Bathtub/shower zones: listed for damp locations, no pendant fixtures within 3 ft horizontally and 8 ft vertically of rim." },
      { code: "410.36", title: "Means of Support", text: "Luminaires shall be securely supported. Fixtures weighing over 50 lbs shall be supported independently of the outlet box, unless the box is listed for that weight. Pendant fixtures on flexible cord shall not exceed 6 lbs without additional support." },
      { code: "410.116", title: "Recessed Luminaires — Clearances", text: "Non-IC rated fixtures: 1/2 in. clearance from combustibles, 3 in. from insulation. IC-rated (Insulation Contact): may be in contact with insulation. Airtight (AT) rated: reduces air infiltration. In insulated ceilings, IC-rated required. ASTM E283 airtight when in conditioned spaces." },
      { code: "410.130(G)", title: "Disconnecting Means for Recessed Luminaires", text: "NEW NEC 2023: Recessed luminaires must have a means to disconnect all ungrounded conductors, integral to the fixture or within sight from the fixture. Applies to new installations." },
      { code: "410.141", title: "Rating", text: "Lampholders shall be marked with the maximum allowable wattage. Installing higher-wattage bulbs than the fixture is rated for causes overheating and fire risk." },
    ],
    violations: ["Non-IC recessed fixture in contact with insulation", "Standard fixture in wet/damp location (shower, exterior)", "Ceiling fan on non-fan-rated box", "Fixture over rated wattage", "Missing disconnect at recessed fixture (2023 requirement)"],
  },
  {
    article: "422", title: "Appliances", color: "#b87ee8",
    summary: "Requirements for household appliances: disconnecting means, overcurrent protection, flexible cords, and specific appliance rules. Covers everything from dishwashers to water heaters.",
    rules: [
      { code: "422.11", title: "Overcurrent Protection", text: "Appliances shall be protected against overcurrent per their nameplate ratings. Water heaters and fixed appliances: if not labeled with overcurrent protection, circuit breaker may be up to 150% of nameplate rating." },
      { code: "422.12", title: "Central Heating Equipment", text: "Central heating equipment shall be supplied by an individual branch circuit. Electric space heating equipment: must not be cord-and-plug connected (except portable heaters)." },
      { code: "422.16", title: "Flexible Cords", text: "Dishwashers and waste disposals may be cord-and-plug connected with a flexible cord 3–4 ft long. Ranges, dryers, and built-in appliances must be hard-wired (with specific exceptions for ranges in existing locations)." },
      { code: "422.30", title: "Disconnecting Means", text: "A means to disconnect each appliance from all ungrounded conductors shall be provided. The disconnecting means shall be accessible to the user. For built-in appliances: the circuit breaker or fusible switch may serve as the disconnect if accessible." },
    ],
    violations: ["No disconnect accessible for built-in appliances", "Flexible cord on fixed appliance", "Dishwasher on shared circuit (must be individual)"],
  },
  {
    article: "440", title: "Air-Conditioning and Refrigerating Equipment", color: "#b87ee8",
    summary: "Specific rules for HVAC equipment: sizing conductors and overcurrent protection from nameplate data, disconnect requirements, and whip installation. Different sizing rules than general circuits.",
    rules: [
      { code: "440.4", title: "Marking on Hermetic Refrigerant Motor-Compressors", text: "HVAC equipment nameplate shows: rated load amps (RLA), minimum circuit ampacity (MCA), maximum overcurrent protection (MOCP or MFS). Use these values — not a general load calculation." },
      { code: "440.12", title: "Rating and Interrupting Capacity", text: "Disconnecting means shall have a rating of at least 115% of the nameplate rated-load current or branch-circuit selection current, whichever is greater." },
      { code: "440.14", title: "Location", text: "The disconnect shall be located within sight of the equipment and readily accessible. Cannot be more than 50 ft from the equipment and must be within line of sight." },
      { code: "440.32", title: "Single Motor-Compressor", text: "Branch circuit conductors shall have an ampacity not less than 125% of the motor-compressor rated-load current or branch-circuit selection current, whichever is greater." },
      { code: "440.62", title: "Room Air Conditioners", text: "Window/room AC units may be cord-and-plug connected to 15A or 20A circuits with specific current limitations. The air conditioner shall be the only load on the circuit." },
    ],
    violations: ["Disconnect not within sight of AC unit", "Conductor undersized for 125% of RLA", "Overcurrent protection exceeds MOCP on nameplate", "AC unit on shared circuit"],
  },
  {
    article: "550", title: "Mobile Homes, Manufactured Homes", color: "#e8d47e",
    summary: "Specific requirements for mobile and manufactured homes. Relevant when connecting a manufactured home to site electrical service.",
    rules: [
      { code: "550.10", title: "Power Supply", text: "Power supply to a manufactured home: listed manufactured home supply cord or permanent feeder. 30A (3-wire) or 50A (4-wire) supply based on load." },
      { code: "550.33", title: "Feeder", text: "Feeders to manufactured homes shall include a grounding conductor. The grounding conductor must be connected separately from the neutral at the disconnect." },
    ],
    violations: ["Neutral and ground bonded at the manufactured home panel (must be separate)"],
  },
  {
    article: "625", title: "Electric Vehicle Power Transfer", color: "#e8d47e",
    summary: "NEC 2023 significantly expanded EV charging requirements. Now requires EV-ready circuits in new homes, covers EVSE installation, and addresses bidirectional EV charging.",
    rules: [
      { code: "625.2", title: "Definitions", text: "EVSE: Electric Vehicle Supply Equipment. The conductors, including the ungrounded, grounded, and equipment grounding conductors, the electric vehicle connectors, attachment plugs, and all other fittings, devices, power outlets, or apparatus installed specifically for the purpose of transferring energy between the premises wiring and the electric vehicle." },
      { code: "625.40", title: "Electric Vehicle Branch Circuit", text: "Each EVSE outlet or attached EV supply equipment shall be supplied by a separate branch circuit. The circuit shall have no other outlets. Minimum 40A for Level 2 EVSE in new construction." },
      { code: "625.42", title: "Rating of Outlet", text: "For portable cord-connected EVSE (plug-in chargers), the receptacle or outlet shall be rated at not less than the minimum ampere rating of the EVSE." },
      { code: "625.52", title: "Ventilation Not Required for EV Charging", text: "Ventilation is not required for listed EV charging equipment in garages — these units manage charging within safe battery parameters. Older hydrogen venting concerns don't apply to lithium-ion EVs." },
      { code: "625.60", title: "Interactive Systems — Vehicle-to-Home (V2H)", text: "NEW NEC 2023: Provisions for bidirectional charging (vehicle-to-home, V2G). The EV can power home loads during outages. Requires listed interactive equipment and a transfer switch that prevents backfeed to utility." },
    ],
    violations: ["EVSE on shared circuit", "No EV-ready branch circuit in new garage (2023)", "Incorrect wire sizing for 40A EVSE circuit", "Missing or incorrect GFCI on EVSE outlets"],
  },
  {
    article: "680", title: "Swimming Pools, Fountains, and Similar Installations", color: "#7ee8d4",
    summary: "Some of the strictest rules in the NEC. Pool and spa electrical work requires bonding of all metal parts, GFCI protection, strict distance rules from water, and proper luminaire ratings. Violations here can be fatal.",
    rules: [
      { code: "680.6", title: "Grounding", text: "All electrical equipment within 5 ft of pool edge shall be grounded. Equipment grounding conductors shall be insulated (not bare) for connections to pool equipment." },
      { code: "680.21", title: "Motors", text: "Wiring methods for pool pump motors: rigid metal conduit, intermediate metal conduit, rigid PVC conduit, Type MC cable, or copper conductors in EMT permitted. Must be GFCI-protected (20A or less). Receptacle for pump motor within 6–10 ft of pool edge, GFCI-protected." },
      { code: "680.22", title: "Area Lighting, Receptacles, and Equipment", text: "Receptacles: minimum 6 ft from pool edge. GFCI protection for all receptacles within 20 ft of pool. No receptacles within 10 ft of indoor pools. Luminaires: minimum 12 ft horizontally from pool edge unless at least 5 ft above water level." },
      { code: "680.26", title: "Equipotential Bonding", text: "All metal parts of pools must be bonded: water, metal walls, metal forming shells, metal fittings, metal enclosures, motors, junction boxes, all metal within 5 ft. Bond wire: min 8 AWG solid copper. This bonding prevents voltage differences that can electrocute swimmers (electric shock drowning)." },
      { code: "680.42", title: "Outdoor Spas and Hot Tubs", text: "Must have GFCI protection. Metal parts within 5 ft must be bonded. Outdoor spas: only listed outdoor spa luminaires permitted. Flexible cord from motor to disconnect." },
    ],
    violations: ["Incomplete equipotential bonding (leaving metal parts unbonded)", "Receptacles within 6 ft of pool edge", "Lighting within 12 ft horizontally of pool edge", "Missing GFCI on pool equipment circuits", "Bare grounding conductors used in pool area"],
  },
  {
    article: "690", title: "Solar Photovoltaic (PV) Systems", color: "#e8d47e",
    summary: "Requirements for solar panel installations: rapid shutdown, arc-fault protection, disconnecting means, and backfeed protection. Critical for any solar or battery storage work.",
    rules: [
      { code: "690.12", title: "Rapid Shutdown of PV Systems on Buildings", text: "PV systems on buildings must include a rapid shutdown function that, when initiated, reduces PV system voltage to 30V or less within 30 seconds. Required for firefighter safety. Must include a labeled initiation device (roof-level disconnect or utility disconnect)." },
      { code: "690.15", title: "Disconnecting Means", text: "Means shall be provided to disconnect the PV system from all wiring systems including power systems, energy storage systems, and utilization equipment. Must be accessible, externally operable, and lockable." },
      { code: "690.56", title: "Equipment", text: "Buildings with PV systems shall have a permanent plaque or directory at the service entrance indicating the location of all PV system disconnecting means and that the building has a PV system." },
    ],
    violations: ["No rapid shutdown system", "Missing PV system placard at service entrance", "PV conductors in same conduit as service conductors without proper separation"],
  },
  {
    article: "700", title: "Emergency Systems", color: "#e87e7e",
    summary: "Requirements for emergency electrical systems in buildings where loss of power could threaten life safety. Covers automatic transfer equipment, testing, and backup power.",
    rules: [
      { code: "700.3", title: "Tests and Maintenance", text: "Emergency systems shall be tested periodically to ensure they will function properly in an emergency. Monthly operational tests and annual load tests required for systems that depend on storage batteries." },
      { code: "700.12", title: "General Requirements for Emergency Sources", text: "Emergency power sources include: storage batteries, generator sets, UPS, fuel cell systems, or other approved sources. Must supply emergency load within 10 seconds of power failure." },
    ],
    violations: ["Emergency system not tested", "Transfer switch not rated for connected load"],
  },
  {
    article: "702", title: "Optional Standby Systems", color: "#b87ee8",
    summary: "Requirements for portable and permanently installed generators used for optional standby power in homes. Covers transfer switches, inlet boxes, and interlocks — critical for safe generator hookup.",
    rules: [
      { code: "702.5", title: "Capacity and Rating", text: "An optional standby system shall have adequate capacity and rating for the supply of all equipment intended to be operated simultaneously. Load calculation required." },
      { code: "702.6", title: "Transfer Equipment", text: "Transfer equipment, including automatic transfer switches, shall be designed and installed to prevent inadvertent interconnection of normal and alternate sources. Interlock kit or transfer switch required — direct generator hookup to outlet (backfeed) is illegal and dangerous." },
      { code: "702.7", title: "Signals", text: "Audible and visual signal devices shall be provided, where practicable, for the following: derangement of the optional standby source, battery charging failure." },
    ],
    violations: ["No transfer switch (generator plugged directly into outlet — backfeed kills utility workers)", "Transfer switch undersized for connected load", "Missing inlet box weatherproof cover"],
  },
  {
    article: "760", title: "Fire Alarm Systems", color: "#e87e7e",
    summary: "Requirements for hardwired fire alarm and smoke detection systems. Covers wiring methods, power supplies, and interconnection of detectors. Key for smoke/CO detector installations.",
    rules: [
      { code: "760.30", title: "Fire Alarm Circuit Wiring", text: "Fire alarm circuit conductors are classified as NPLFA (non-power-limited) or PLFA (power-limited). Power-limited fire alarm cable (FPLR, FPLP) is commonly used in homes for hardwired detector interconnection." },
      { code: "760.41", title: "NPLFA Circuit Supply", text: "Non-power-limited fire alarm circuits supplied from standard branch circuits shall have overcurrent protection at 20A maximum. Must have a dedicated circuit label." },
      { code: "760.121", title: "Power Sources", text: "Power-limited fire alarm systems shall be supplied from a branch circuit at the panelboard. The circuit shall be dedicated to fire alarm and shall not supply other loads." },
    ],
    violations: ["Smoke detectors not interconnected (when 3+ units required)", "Using wrong cable type for fire alarm wiring", "Sharing fire alarm circuit with general loads"],
  },
  {
    article: "800", title: "Communications Circuits", color: "#7ee8d4",
    summary: "Rules for telephone, internet, and cable TV wiring. Covers entrance protection, grounding, and separation from power wiring. Often overlooked but can cause shock or fire hazards.",
    rules: [
      { code: "800.4", title: "Equipment — Listing", text: "Communications equipment installed in or on buildings shall be listed." },
      { code: "800.90", title: "Protective Devices", text: "A listed primary protector shall be provided for each circuit run partly or wholly in aerial wire or cable not confined within a block. The primary protector shall be listed for the purpose." },
      { code: "800.100", title: "Grounding", text: "Primary protector grounding conductors shall be copper or other corrosion-resistant material, insulated, listed, and not smaller than 14 AWG. Connected to the power grounding electrode system. Minimum length: as short as practicable." },
      { code: "800.133", title: "Installation of Communications Wires and Cables", text: "Communications cables shall be separated from power conductors. Maintain separation from power wiring by at least 2 in. unless in a separate raceway or conduit, or one of the conductors is in a metal raceway." },
    ],
    violations: ["Communications cable in same conduit as power (without proper separation)", "Missing surge protector on aerial communications entry", "Communications grounding not bonded to power ground"],
  },
];

const MARKUP_OPTIONS = [{ label: "15%", v: 0.15 }, { label: "20%", v: 0.20 }, { label: "25%", v: 0.25 }, { label: "30%", v: 0.30 }, { label: "40%", v: 0.40 }, { label: "50%", v: 0.50 }];
const HOURLY_RATES   = [55, 65, 75, 85, 95, 110, 125, 150];
const ALL_SERVICES   = CATEGORIES.flatMap(c => c.services.map(s => ({ ...s, catColor: c.color, catLabel: c.label })));



// ─── COUNTER ─────────────────────────────────────────────────────────────────
function Counter({ value, onChange }) {
  return (
    <div style={{ display:"flex", alignItems:"center", background:"rgba(255,255,255,0.04)", borderRadius:8, border:"1px solid rgba(255,255,255,0.08)", overflow:"hidden" }}>
      {[{ sym:"−", fn:() => onChange(Math.max(0, value-1)), dis: value===0 }, { sym:"+", fn:() => onChange(value+1), dis: false }].map(({ sym, fn, dis }) => (
        <button key={sym} onClick={fn} style={{ width:28, height:28, border:"none", background:"transparent", color: dis ? "rgba(255,255,255,0.15)" : "#e8c97a", fontSize:17, cursor: dis ? "default" : "pointer", display:"flex", alignItems:"center", justifyContent:"center", fontFamily:"inherit", transition:"background 0.15s" }}
          onMouseEnter={e => { if (!dis) e.currentTarget.style.background="rgba(232,201,122,0.1)"; }}
          onMouseLeave={e => e.currentTarget.style.background="transparent"}
        >{sym}</button>
      ))}
      <span style={{ width:24, textAlign:"center", fontFamily:"'DM Mono',monospace", fontSize:13, fontWeight:600, color:"#fff" }}>{value}</span>
    </div>
  );
}

// ─── SERVICE ROW ─────────────────────────────────────────────────────────────
function ServiceRow({ service, entry, onUpdate, accentColor, hourlyRate, clientBuys, showMaterials }) {
  const qty        = entry?.qty ?? 0;
  const variantIdx = entry?.variantIdx ?? 0;
  const cBuys      = entry?.clientBuys ?? clientBuys; // per-line override

  return (
    <div style={{ borderBottom:"1px solid rgba(255,255,255,0.04)", padding:"11px 0", transition:"all 0.2s" }}>
      <div style={{ display:"flex", alignItems:"flex-start", gap:8 }}>
        {/* Name col */}
        <div style={{ flex:1, minWidth:0 }}>
          <div style={{ fontSize:13, fontWeight:600, color: qty>0 ? "#fff" : "rgba(255,255,255,0.58)", lineHeight:1.3, letterSpacing:"-0.01em" }}>
            {service.label}
          </div>
          <div style={{ display:"flex", alignItems:"center", gap:5, marginTop:3, flexWrap:"wrap" }}>
            <span style={{ fontSize:9, fontFamily:"'DM Mono',monospace", color:"rgba(255,255,255,0.28)", background:"rgba(255,255,255,0.05)", border:"1px solid rgba(255,255,255,0.08)", padding:"1px 5px", borderRadius:3, letterSpacing:"0.03em" }}>{service.nec}</span>
            <span style={{ fontSize:9, color:"rgba(255,255,255,0.22)", fontFamily:"'DM Mono',monospace" }}>
              mat ${service.materialCost} · lab ${service.laborCost}/{service.unit}
            </span>
          </div>
        </div>

        {/* Right controls */}
        <div style={{ display:"flex", flexDirection:"column", alignItems:"flex-end", gap:5, flexShrink:0 }}>
          {/* Qty counter */}
          <Counter value={qty} onChange={v => onUpdate({ qty: v, variantIdx, clientBuys: cBuys })} />

          {qty > 0 && (
            <div style={{ display:"flex", flexDirection:"column", alignItems:"flex-end", gap:3 }}>
              {/* Variant */}
              {service.variants.length > 1 && (
                <select value={variantIdx} onChange={e => onUpdate({ qty, variantIdx: Number(e.target.value), clientBuys: cBuys })}
                  style={{ background:"rgba(255,255,255,0.06)", border:"1px solid rgba(255,255,255,0.1)", borderRadius:5, color:"#e8c97a", fontSize:10, padding:"3px 5px", fontFamily:"inherit", cursor:"pointer", maxWidth:130 }}>
                  {service.variants.map((v,i) => <option key={i} value={i} style={{ background:"#1a1a1e" }}>{v.label}</option>)}
                </select>
              )}

              {/* Who buys parts toggle */}
              <div style={{ display:"flex", gap:3 }}>
                {[{ lbl:"I buy parts", val: false }, { lbl:"Client buys", val: true }].map(opt => (
                  <button key={String(opt.val)} onClick={() => onUpdate({ qty, variantIdx, clientBuys: opt.val })}
                    style={{ padding:"2px 7px", borderRadius:4, fontSize:9, fontWeight:700, letterSpacing:"0.02em", cursor:"pointer", fontFamily:"inherit", transition:"all 0.15s",
                      border: cBuys===opt.val ? `1px solid ${accentColor}60` : "1px solid rgba(255,255,255,0.08)",
                      background: cBuys===opt.val ? `${accentColor}15` : "rgba(255,255,255,0.03)",
                      color: cBuys===opt.val ? accentColor : "rgba(255,255,255,0.35)" }}>
                    {opt.lbl}
                  </button>
                ))}
              </div>

              {/* Line cost pills */}
              <div style={{ display:"flex", gap:4, flexWrap:"wrap", justifyContent:"flex-end" }}>
                {showMaterials && !cBuys && (
                  <span style={{ fontSize:9, fontFamily:"'DM Mono',monospace", color:"rgba(255,255,255,0.4)", background:"rgba(255,255,255,0.05)", padding:"2px 6px", borderRadius:4 }}>
                    mat ${(service.materialCost * service.variants[variantIdx].m * qty).toLocaleString()}
                  </span>
                )}
                <span style={{ fontSize:9, fontFamily:"'DM Mono',monospace", color:"rgba(162,220,160,0.8)", background:"rgba(162,220,160,0.08)", padding:"2px 6px", borderRadius:4 }}>
                  lab ${(service.laborCost * service.variants[variantIdx].m * qty).toLocaleString()}
                </span>
                <span style={{ fontSize:10, fontFamily:"'DM Mono',monospace", fontWeight:700, color: accentColor, padding:"2px 6px", background:`${accentColor}12`, borderRadius:4, border:`1px solid ${accentColor}25` }}>
                  ${((cBuys ? service.laborCost : (service.materialCost + service.laborCost)) * service.variants[variantIdx].m * qty).toLocaleString()}
                </span>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

// ─── CATEGORY SECTION ─────────────────────────────────────────────────────────
function CategorySection({ category, entries, onUpdate, hourlyRate, clientBuys, showMaterials }) {
  const [open, setOpen] = useState(false);
  const active = category.services.filter(s => entries[s.id]?.qty > 0).length;

  return (
    <div style={{ background: active>0 ? `linear-gradient(135deg,${category.color}07 0%,rgba(255,255,255,0.015) 100%)` : "rgba(255,255,255,0.018)", border: active>0 ? `1px solid ${category.color}25` : "1px solid rgba(255,255,255,0.055)", borderRadius:13, overflow:"hidden", transition:"all 0.25s", marginBottom:8 }}>
      <button onClick={() => setOpen(o => !o)} style={{ width:"100%", display:"flex", alignItems:"center", gap:10, padding:"13px 16px", background:"transparent", border:"none", cursor:"pointer", textAlign:"left" }}>
        <div style={{ width:30, height:30, borderRadius:7, flexShrink:0, background: active>0 ? `${category.color}18` : "rgba(255,255,255,0.05)", display:"flex", alignItems:"center", justifyContent:"center", fontSize:13, color: active>0 ? category.color : "rgba(255,255,255,0.28)", transition:"all 0.2s" }}>{category.icon}</div>
        <div style={{ flex:1 }}>
          <div style={{ fontSize:13, fontWeight:700, color: active>0 ? "#fff" : "rgba(255,255,255,0.55)", fontFamily:"'Syne',sans-serif", letterSpacing:"-0.01em" }}>{category.label}</div>
          <div style={{ fontSize:10, color:"rgba(255,255,255,0.28)", marginTop:1, fontFamily:"'DM Mono',monospace" }}>
            {category.services.length} services{active>0 && <span style={{ color:category.color, marginLeft:5 }}>· {active} selected</span>}
          </div>
        </div>
        {active>0 && <span style={{ fontSize:10, fontWeight:700, color:category.color, background:`${category.color}15`, border:`1px solid ${category.color}30`, padding:"2px 7px", borderRadius:8, fontFamily:"'DM Mono',monospace" }}>{active}</span>}
        <span style={{ color:"rgba(255,255,255,0.28)", fontSize:16, transform: open ? "rotate(90deg)" : "rotate(0deg)", transition:"transform 0.2s", lineHeight:1 }}>›</span>
      </button>
      {open && (
        <div style={{ padding:"0 16px 12px" }}>
          {category.services.map(s => (
            <ServiceRow key={s.id} service={s} entry={entries[s.id]} onUpdate={d => onUpdate(s.id, d)}
              accentColor={category.color} hourlyRate={hourlyRate} clientBuys={clientBuys} showMaterials={showMaterials} />
          ))}
        </div>
      )}
    </div>
  );
}

// ─── PILL BUTTON ─────────────────────────────────────────────────────────────
function Pill({ label, active, onClick, color = "#e8c97a" }) {
  return (
    <button onClick={onClick} style={{ padding:"5px 11px", borderRadius:6, fontSize:12, fontWeight:600, border: active ? `1px solid ${color}55` : "1px solid rgba(255,255,255,0.09)", background: active ? `${color}13` : "rgba(255,255,255,0.04)", color: active ? color : "rgba(255,255,255,0.4)", cursor:"pointer", transition:"all 0.15s", fontFamily:"inherit" }}>{label}</button>
  );
}

// ─── STAT CARD ────────────────────────────────────────────────────────────────
function StatCard({ label, value, sub, color = "#e8c97a" }) {
  return (
    <div style={{ flex:1, minWidth:100, background:"rgba(255,255,255,0.025)", border:"1px solid rgba(255,255,255,0.07)", borderRadius:10, padding:"12px 14px" }}>
      <div style={{ fontSize:9, color:"rgba(255,255,255,0.3)", textTransform:"uppercase", letterSpacing:"0.1em", marginBottom:5 }}>{label}</div>
      <div style={{ fontFamily:"'DM Mono',monospace", fontSize:18, fontWeight:500, color, letterSpacing:"-0.02em" }}>{value}</div>
      {sub && <div style={{ fontSize:9, color:"rgba(255,255,255,0.25)", marginTop:3, fontFamily:"'DM Mono',monospace" }}>{sub}</div>}
    </div>
  );
}

// ─── NEC REFERENCE COMPONENT ─────────────────────────────────────────────────
function NECReference() {
  const [search, setSearch]         = useState("");
  const [expanded, setExpanded]     = useState({});
  const [expandedRule, setExpandedRule] = useState({});
  const [filterTag, setFilterTag]   = useState("all");

  const TAGS = [
    { id: "all",     label: "All Articles" },
    { id: "gfci",    label: "GFCI" },
    { id: "afci",    label: "AFCI" },
    { id: "ground",  label: "Grounding" },
    { id: "panel",   label: "Panel/Service" },
    { id: "wiring",  label: "Wiring Methods" },
    { id: "outlets", label: "Outlets/Switches" },
    { id: "lighting",label: "Lighting" },
    { id: "hvac",    label: "HVAC/Appliances" },
    { id: "outdoor", label: "Outdoor/Pool/EV" },
    { id: "new2023", label: "New in 2023" },
  ];

  const TAG_ARTICLE_MAP = {
    gfci:     ["210", "406", "680"],
    afci:     ["210", "404"],
    ground:   ["250", "200"],
    panel:    ["408", "230", "240", "215"],
    wiring:   ["300", "310", "334", "358", "110"],
    outlets:  ["406", "404", "210"],
    lighting: ["410", "314"],
    hvac:     ["440", "422", "550"],
    outdoor:  ["225", "680", "625", "702"],
    new2023:  ["230", "410", "404", "625", "210"],
  };

  const q = search.toLowerCase().trim();

  const filtered = NEC_REF.filter(art => {
    const matchesTag = filterTag === "all" || (TAG_ARTICLE_MAP[filterTag] || []).includes(art.article);
    if (!matchesTag) return false;
    if (!q) return true;
    return (
      art.article.includes(q) ||
      art.title.toLowerCase().includes(q) ||
      art.summary.toLowerCase().includes(q) ||
      art.rules.some(r => r.title.toLowerCase().includes(q) || r.text.toLowerCase().includes(q) || r.code.toLowerCase().includes(q)) ||
      (art.violations || []).some(v => v.toLowerCase().includes(q))
    );
  });

  const toggleArticle = (id) => setExpanded(p => ({ ...p, [id]: !p[id] }));
  const toggleRule = (key) => setExpandedRule(p => ({ ...p, [key]: !p[key] }));

  return (
    <div style={{ animation: "fadeUp 0.3s ease both" }}>
      {/* Header */}
      <div style={{ marginBottom: 16 }}>
        <div style={{ fontSize: 11, color: "rgba(255,255,255,0.3)", marginBottom: 10, lineHeight: 1.6 }}>
          Full NEC 2023 residential reference · {NEC_REF.length} articles · {NEC_REF.reduce((a, c) => a + c.rules.length, 0)} code sections · searchable
        </div>

        {/* Search */}
        <input
          placeholder="Search article, code number, or keyword (e.g. GFCI, 210.8, tamper)..."
          value={search}
          onChange={e => setSearch(e.target.value)}
          style={{ width: "100%", background: "rgba(255,255,255,0.04)", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 9, padding: "11px 14px", fontSize: 13, color: "#fff", fontFamily: "inherit", marginBottom: 10, transition: "border-color 0.15s" }}
          onFocus={e => e.target.style.borderColor = "rgba(232,201,122,0.4)"}
          onBlur={e => e.target.style.borderColor = "rgba(255,255,255,0.1)"}
        />

        {/* Filter tags */}
        <div style={{ display: "flex", flexWrap: "wrap", gap: 5 }}>
          {TAGS.map(t => (
            <button key={t.id} onClick={() => setFilterTag(t.id)}
              style={{ padding: "4px 10px", borderRadius: 5, fontSize: 10, fontWeight: 700, letterSpacing: "0.03em", cursor: "pointer", fontFamily: "inherit", transition: "all 0.15s",
                border: filterTag === t.id ? "1px solid rgba(232,201,122,0.5)" : "1px solid rgba(255,255,255,0.08)",
                background: filterTag === t.id ? "rgba(232,201,122,0.12)" : "rgba(255,255,255,0.03)",
                color: filterTag === t.id ? "#e8c97a" : "rgba(255,255,255,0.38)" }}>
              {t.label}
            </button>
          ))}
        </div>
      </div>

      {/* Results count */}
      {(q || filterTag !== "all") && (
        <div style={{ fontSize: 10, color: "rgba(255,255,255,0.25)", fontFamily: "'DM Mono',monospace", marginBottom: 12 }}>
          {filtered.length} article{filtered.length !== 1 ? "s" : ""} · {filtered.reduce((a, c) => a + c.rules.length, 0)} code sections
        </div>
      )}

      {/* Article cards */}
      {filtered.length === 0 ? (
        <div style={{ textAlign: "center", padding: "40px 20px", color: "rgba(255,255,255,0.2)", fontSize: 13 }}>
          No results for "{search}"
        </div>
      ) : (
        filtered.map(art => {
          const isOpen = expanded[art.article];
          return (
            <div key={art.article} style={{ background: isOpen ? `linear-gradient(135deg,${art.color}06 0%,rgba(255,255,255,0.015) 100%)` : "rgba(255,255,255,0.018)", border: isOpen ? `1px solid ${art.color}22` : "1px solid rgba(255,255,255,0.055)", borderRadius: 13, marginBottom: 8, overflow: "hidden", transition: "all 0.25s" }}>

              {/* Article header */}
              <button onClick={() => toggleArticle(art.article)} style={{ width: "100%", display: "flex", alignItems: "flex-start", gap: 12, padding: "14px 16px", background: "transparent", border: "none", cursor: "pointer", textAlign: "left" }}>
                <div style={{ flexShrink: 0, width: 46, height: 46, borderRadius: 8, background: `${art.color}15`, border: `1px solid ${art.color}25`, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center" }}>
                  <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 10, color: art.color, fontWeight: 700, letterSpacing: "0.02em" }}>ART.</div>
                  <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 13, color: art.color, fontWeight: 700, lineHeight: 1 }}>{art.article}</div>
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 13, fontWeight: 700, color: "#fff", fontFamily: "'Syne',sans-serif", letterSpacing: "-0.01em", lineHeight: 1.2 }}>{art.title}</div>
                  <div style={{ fontSize: 11, color: "rgba(255,255,255,0.35)", marginTop: 4, lineHeight: 1.5 }}>{art.summary.slice(0, 100)}{art.summary.length > 100 ? "…" : ""}</div>
                  <div style={{ display: "flex", gap: 8, marginTop: 5, flexWrap: "wrap" }}>
                    <span style={{ fontSize: 9, color: "rgba(255,255,255,0.25)", fontFamily: "'DM Mono',monospace" }}>{art.rules.length} sections</span>
                    {art.violations?.length > 0 && <span style={{ fontSize: 9, color: "rgba(232,120,120,0.6)", fontFamily: "'DM Mono',monospace" }}>· {art.violations.length} common violations</span>}
                  </div>
                </div>
                <span style={{ color: "rgba(255,255,255,0.25)", fontSize: 16, transform: isOpen ? "rotate(90deg)" : "rotate(0deg)", transition: "transform 0.2s", lineHeight: 1, flexShrink: 0 }}>›</span>
              </button>

              {/* Article content */}
              {isOpen && (
                <div style={{ padding: "0 16px 16px" }}>
                  {/* Full summary */}
                  <div style={{ fontSize: 12, color: "rgba(255,255,255,0.5)", lineHeight: 1.7, marginBottom: 14, paddingBottom: 14, borderBottom: "1px solid rgba(255,255,255,0.06)" }}>
                    {art.summary}
                  </div>

                  {/* Code sections */}
                  <div style={{ fontSize: 10, color: "rgba(255,255,255,0.25)", textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 8 }}>Code Sections</div>
                  {art.rules.map((rule, ri) => {
                    const rKey = `${art.article}-${ri}`;
                    const rOpen = expandedRule[rKey];
                    return (
                      <div key={ri} style={{ background: rOpen ? "rgba(255,255,255,0.04)" : "rgba(255,255,255,0.02)", border: "1px solid rgba(255,255,255,0.06)", borderRadius: 8, marginBottom: 5, overflow: "hidden", transition: "all 0.2s" }}>
                        <button onClick={() => toggleRule(rKey)} style={{ width: "100%", display: "flex", alignItems: "center", gap: 10, padding: "10px 12px", background: "transparent", border: "none", cursor: "pointer", textAlign: "left" }}>
                          <span style={{ fontFamily: "'DM Mono',monospace", fontSize: 10, color: art.color, fontWeight: 700, flexShrink: 0, minWidth: 60 }}>{rule.code}</span>
                          <span style={{ fontSize: 12, fontWeight: 600, color: rOpen ? "#fff" : "rgba(255,255,255,0.65)", flex: 1 }}>{rule.title}</span>
                          <span style={{ color: "rgba(255,255,255,0.2)", fontSize: 14, transform: rOpen ? "rotate(90deg)" : "rotate(0deg)", transition: "transform 0.2s", lineHeight: 1, flexShrink: 0 }}>›</span>
                        </button>
                        {rOpen && (
                          <div style={{ padding: "0 12px 12px", fontSize: 12, color: "rgba(255,255,255,0.55)", lineHeight: 1.75, borderTop: "1px solid rgba(255,255,255,0.05)", paddingTop: 10 }}>
                            {rule.text}
                          </div>
                        )}
                      </div>
                    );
                  })}

                  {/* Common violations */}
                  {art.violations?.length > 0 && (
                    <div style={{ marginTop: 14 }}>
                      <div style={{ fontSize: 10, color: "rgba(232,120,120,0.6)", textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 8, fontWeight: 700 }}>⚠ Common Violations</div>
                      {art.violations.map((v, vi) => (
                        <div key={vi} style={{ display: "flex", gap: 8, alignItems: "flex-start", marginBottom: 6 }}>
                          <span style={{ color: "rgba(232,120,120,0.5)", fontSize: 11, flexShrink: 0, marginTop: 1 }}>·</span>
                          <span style={{ fontSize: 11, color: "rgba(255,255,255,0.45)", lineHeight: 1.6 }}>{v}</span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </div>
          );
        })
      )}
    </div>
  );
}

// ─── MAIN APP ─────────────────────────────────────────────────────────────────
export default function Wireway() {
  const [entries,      setEntries]      = useState({});
  const [hourlyRate,   setHourlyRate]   = useState(85);
  const [markup,       setMarkup]       = useState(0.30);
  const [clientName,   setClientName]   = useState("");
  const [clientEmail,  setClientEmail]  = useState("");
  const [clientPhone,  setClientPhone]  = useState("");
  const [jobName,      setJobName]      = useState("");
  const [notes,        setNotes]        = useState("");
  const [tab,          setTab]          = useState("services");
  const [showMaterials,setShowMaterials]= useState(true);
  const [clientBuysAll,setClientBuysAll]= useState(false);
  const [sendModal,    setSendModal]    = useState(false);
  const [copied,       setCopied]       = useState(false);
  const [previewModal, setPreviewModal] = useState(false);

  // ── Company profile (persisted in localStorage) ──
  const [company, setCompany] = useState(() => {
    try { return JSON.parse(localStorage.getItem("wireway_company") || "{}"); } catch { return {}; }
  });
  const [editingCompany, setEditingCompany] = useState(false);
  const [companyDraft,   setCompanyDraft]   = useState(company);
  const [logoDataUrl,    setLogoDataUrl]    = useState(company.logoDataUrl || "");

  const saveCompany = () => {
    const saved = { ...companyDraft, logoDataUrl };
    setCompany(saved);
    try { localStorage.setItem("wireway_company", JSON.stringify(saved)); } catch {}
    setEditingCompany(false);
  };

  const handleLogoUpload = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (ev) => setLogoDataUrl(ev.target.result);
    reader.readAsDataURL(file);
  };

  const upd = (id, data) => setEntries(p => ({ ...p, [id]: data }));

  // ── Totals ──
  const { activeItems, totMat, totLab, totHrs, totClientBuysMat } = useMemo(() => {
    const items = ALL_SERVICES.filter(s => entries[s.id]?.qty > 0).map(s => {
      const e = entries[s.id];
      const v = s.variants[e.variantIdx ?? 0];
      const qty = e.qty;
      const cBuys = e.clientBuys ?? clientBuysAll;
      const mat  = s.materialCost * v.m * qty;
      const lab  = s.laborCost    * v.m * qty;
      const hrs  = s.laborHours   * v.m * qty;
      return { ...s, qty, variantLabel: v.label, mat, lab, hrs, cBuys, lineTotal: cBuys ? lab : mat + lab };
    });
    return {
      activeItems: items,
      totMat:           items.reduce((a,i) => a + (i.cBuys ? 0 : i.mat), 0),
      totLab:           items.reduce((a,i) => a + i.lab, 0),
      totHrs:           items.reduce((a,i) => a + i.hrs, 0),
      totClientBuysMat: items.reduce((a,i) => a + (i.cBuys ? i.mat : 0), 0),
    };
  }, [entries, clientBuysAll]);

  const subtotal = totMat + totLab;
  const markupAmt = subtotal * markup;
  const total = subtotal + markupAmt;
  const hasItems = activeItems.length > 0;

  // ── Build plain-text quote ──
  const buildQuoteText = () => {
    const cBuyItems = activeItems.filter(i => i.cBuys);
    const iSupply   = activeItems.filter(i => !i.cBuys);
    return [
      "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
      company.name ? `  ${company.name.toUpperCase()}` : "  ELECTRICAL ESTIMATE",
      company.phone ? `  ${company.phone}` : null,
      company.email ? `  ${company.email}` : null,
      company.address ? `  ${company.address}` : null,
      company.license ? `  License: ${company.license}` : null,
      "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
      clientName && `Client:   ${clientName}`,
      clientPhone && `Phone:    ${clientPhone}`,
      clientEmail && `Email:    ${clientEmail}`,
      jobName    && `Job:      ${jobName}`,
      `Date:     ${new Date().toLocaleDateString()}`,
      "",
      "SERVICES PROVIDED",
      "──────────────────────────────────",
      ...activeItems.map(i => `  • ${i.label} (${i.variantLabel}) × ${i.qty}`),
      "",
      ...(iSupply.length ? [
        "PARTS SUPPLIED BY US",
        "──────────────────────────────────",
        ...iSupply.map(i => `  ${i.label} × ${i.qty}  — $${i.mat.toLocaleString()}`),
        `  Materials subtotal: $${totMat.toLocaleString()}`, "",
      ] : []),
      ...(cBuyItems.length ? [
        "CLIENT SUPPLIES PARTS (labor only billed)",
        "──────────────────────────────────",
        ...cBuyItems.map(i => `  ${i.label} × ${i.qty}  — est. $${i.mat.toLocaleString()} (not charged)`),
        "",
      ] : []),
      "COST BREAKDOWN",
      "──────────────────────────────────",
      showMaterials ? `  Materials:  $${totMat.toLocaleString()}` : null,
      `  Labor:      $${totLab.toLocaleString()}  (${totHrs.toFixed(1)} hrs @ $${hourlyRate}/hr)`,
      `  Markup:     $${markupAmt.toLocaleString()}  (${(markup*100).toFixed(0)}%)`,
      "──────────────────────────────────",
      `  TOTAL:      $${total.toLocaleString()}`,
      notes ? `\nNotes:\n${notes}` : null,
      "",
      company.terms ? `TERMS:\n${company.terms}` : null,
      "",
      "Generated by Wireway · NEC 2023 Professional Estimating",
    ].filter(Boolean).join("\n");
  };

  const copyQuote = () => {
    navigator.clipboard.writeText(buildQuoteText());
    setCopied(true); setTimeout(() => setCopied(false), 2500);
  };

  const emailQuote = () => {
    const subject = encodeURIComponent(`Electrical Estimate${clientName ? " for " + clientName : ""}${jobName ? " — " + jobName : ""}`);
    const body = encodeURIComponent(buildQuoteText());
    const to = clientEmail ? encodeURIComponent(clientEmail) : "";
    window.open(`mailto:${to}?subject=${subject}&body=${body}`);
  };

  const smsQuote = () => {
    const body = encodeURIComponent(
      `Hi ${clientName || "there"}, here's your electrical estimate from ${company.name || "us"}:\n\n` +
      activeItems.map(i => `• ${i.label} × ${i.qty}`).join("\n") +
      `\n\nTOTAL: $${total.toLocaleString()}\n\nCall us: ${company.phone || ""}`
    );
    const to = clientPhone ? clientPhone.replace(/\D/g, "") : "";
    window.open(`sms:${to}?body=${body}`);
  };

  const TAB = (id, lbl) => (
    <button onClick={() => setTab(id)} style={{ flex:1, padding:"10px 6px", border:"none", cursor:"pointer", fontFamily:"'Syne',sans-serif", fontSize:12, fontWeight:700, letterSpacing:"-0.01em", transition:"all 0.2s", background: tab===id ? "rgba(232,201,122,0.1)" : "transparent", color: tab===id ? "#e8c97a" : "rgba(255,255,255,0.3)", borderBottom: tab===id ? "2px solid #e8c97a" : "2px solid transparent" }}>{lbl}</button>
  );

  const inputStyle = { background:"rgba(255,255,255,0.04)", border:"1px solid rgba(255,255,255,0.07)", borderRadius:7, padding:"8px 11px", fontSize:13, color:"#fff", fontFamily:"inherit", width:"100%", transition:"border-color 0.15s" };
  const focusGold = e => e.target.style.borderColor = "rgba(232,201,122,0.4)";
  const blurGray  = e => e.target.style.borderColor = "rgba(255,255,255,0.07)";

  return (
    <>
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=DM+Mono:wght@400;500&family=Syne:wght@600;700;800&family=DM+Sans:wght@400;500;600&display=swap');
        *{box-sizing:border-box;margin:0;padding:0}
        body{background:#0a0a0c}
        ::-webkit-scrollbar{width:3px}
        ::-webkit-scrollbar-thumb{background:rgba(255,255,255,0.1);border-radius:3px}
        input,textarea,select{outline:none}
        input::placeholder,textarea::placeholder{color:rgba(255,255,255,0.18)}
        select option{background:#1a1a1e}
        @keyframes fadeUp{from{opacity:0;transform:translateY(10px)}to{opacity:1;transform:translateY(0)}}
        @keyframes modalIn{from{opacity:0;transform:scale(0.96) translateY(12px)}to{opacity:1;transform:scale(1) translateY(0)}}
        .modal-overlay{position:fixed;inset:0;background:rgba(0,0,0,0.75);backdrop-filter:blur(8px);z-index:200;display:flex;align-items:flex-start;justify-content:center;overflow-y:auto;padding:24px 16px}
        .modal-box{background:#111115;border:1px solid rgba(255,255,255,0.1);border-radius:18px;width:100%;max-width:600px;animation:modalIn 0.25s ease both;margin:auto}
        @media print{.no-print{display:none!important}.print-quote{background:#fff!important;color:#000!important;padding:32px!important}}
      `}</style>

      <div style={{ minHeight:"100vh", background:"radial-gradient(ellipse 80% 45% at 50% -5%,rgba(232,201,122,0.065) 0%,transparent 55%),#0a0a0c", fontFamily:"'DM Sans',sans-serif", color:"#fff", paddingBottom:80 }}>

        {/* ── HEADER ── */}
        <div style={{ borderBottom:"1px solid rgba(255,255,255,0.055)", background:"rgba(10,10,12,0.88)", backdropFilter:"blur(20px)", position:"sticky", top:0, zIndex:100, padding:"0 20px" }} className="no-print">
          <div style={{ maxWidth:800, margin:"0 auto", height:54, display:"flex", alignItems:"center", justifyContent:"space-between" }}>
            <div style={{ display:"flex", alignItems:"center", gap:8 }}>
              {logoDataUrl
                ? <img src={logoDataUrl} alt="logo" style={{ height:28, width:"auto", borderRadius:4, objectFit:"contain" }} />
                : <div style={{ width:26, height:26, borderRadius:6, background:"linear-gradient(135deg,#e8c97a 0%,#c9a84c 100%)", display:"flex", alignItems:"center", justifyContent:"center", fontSize:12, fontWeight:800, color:"#0a0a0c" }}>W</div>
              }
              <span style={{ fontFamily:"'Syne',sans-serif", fontSize:16, fontWeight:800, letterSpacing:"-0.03em" }}>{company.name || "Wireway"}</span>
              <span style={{ fontSize:8, fontWeight:700, color:"rgba(232,201,122,0.6)", background:"rgba(232,201,122,0.07)", border:"1px solid rgba(232,201,122,0.16)", padding:"1px 5px", borderRadius:3, letterSpacing:"0.08em", textTransform:"uppercase" }}>NEC 2023</span>
            </div>
            <div style={{ display:"flex", alignItems:"center", gap:10 }}>
              {hasItems && <span style={{ fontFamily:"'DM Mono',monospace", fontSize:17, fontWeight:500, color:"#e8c97a", letterSpacing:"-0.02em" }}>${total.toLocaleString()}</span>}
              <button onClick={() => { setCompanyDraft(company); setLogoDataUrl(company.logoDataUrl||""); setEditingCompany(true); }}
                style={{ padding:"5px 10px", borderRadius:6, border:"1px solid rgba(255,255,255,0.1)", background:"rgba(255,255,255,0.04)", color:"rgba(255,255,255,0.5)", fontSize:11, fontWeight:600, cursor:"pointer", fontFamily:"inherit" }}>
                ⚙ Company
              </button>
            </div>
          </div>
        </div>

        <div style={{ maxWidth:800, margin:"0 auto", padding:"0 20px" }}>

          {/* ── HERO ── */}
          <div style={{ padding:"26px 0 20px", animation:"fadeUp 0.4s ease both" }} className="no-print">
            <h1 style={{ fontFamily:"'Syne',sans-serif", fontSize:"clamp(20px,5vw,30px)", fontWeight:800, letterSpacing:"-0.04em", lineHeight:1.15, background:"linear-gradient(135deg,#ffffff 40%,rgba(232,201,122,0.85) 100%)", WebkitBackgroundClip:"text", WebkitTextFillColor:"transparent", marginBottom:6 }}>
              Build your estimate.<br />Win the job.
            </h1>
            <p style={{ fontSize:12, color:"rgba(255,255,255,0.35)", lineHeight:1.6 }}>
              {CATEGORIES.flatMap(c => c.services).length} services · {CATEGORIES.length} categories · NEC 2023 · Material vs labor split · Client-supplied parts
            </p>
          </div>

          {/* ── JOB DETAILS ── */}
          <div style={{ background:"rgba(255,255,255,0.022)", border:"1px solid rgba(255,255,255,0.065)", borderRadius:13, padding:"14px 16px", marginBottom:12, animation:"fadeUp 0.4s ease 0.04s both" }} className="no-print">
            <div style={{ fontSize:9, color:"rgba(255,255,255,0.28)", textTransform:"uppercase", letterSpacing:"0.1em", marginBottom:10 }}>Client & Job Details</div>
            <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:8 }}>
              {[
                { ph:"Client name",    val:clientName,   set:setClientName },
                { ph:"Job / address",  val:jobName,      set:setJobName },
                { ph:"Client email",   val:clientEmail,  set:setClientEmail },
                { ph:"Client phone",   val:clientPhone,  set:setClientPhone },
              ].map(f => (
                <input key={f.ph} placeholder={f.ph} value={f.val} onChange={e => f.set(e.target.value)}
                  style={inputStyle} onFocus={focusGold} onBlur={blurGray} />
              ))}
            </div>
          </div>

          {/* ── RATE SETTINGS ── */}
          <div style={{ background:"rgba(255,255,255,0.022)", border:"1px solid rgba(255,255,255,0.065)", borderRadius:13, padding:"14px 16px", marginBottom:12, animation:"fadeUp 0.4s ease 0.08s both" }} className="no-print">
            <div style={{ fontSize:9, color:"rgba(255,255,255,0.28)", textTransform:"uppercase", letterSpacing:"0.1em", marginBottom:12 }}>Rate Settings</div>
            <div style={{ display:"flex", gap:18, flexWrap:"wrap" }}>
              <div style={{ flex:1, minWidth:220 }}>
                <div style={{ fontSize:11, color:"rgba(255,255,255,0.35)", marginBottom:8 }}>Hourly Rate — <span style={{ color:"#e8c97a", fontFamily:"'DM Mono',monospace", fontWeight:600 }}>${hourlyRate}/hr</span></div>
                <div style={{ display:"flex", flexWrap:"wrap", gap:5 }}>
                  {HOURLY_RATES.map(r => <Pill key={r} label={`$${r}`} active={r===hourlyRate} onClick={() => setHourlyRate(r)} />)}
                </div>
              </div>
              <div style={{ flex:1, minWidth:170 }}>
                <div style={{ fontSize:11, color:"rgba(255,255,255,0.35)", marginBottom:8 }}>Markup — <span style={{ color:"#e8c97a", fontFamily:"'DM Mono',monospace", fontWeight:600 }}>{(markup*100).toFixed(0)}%</span></div>
                <div style={{ display:"flex", flexWrap:"wrap", gap:5 }}>
                  {MARKUP_OPTIONS.map(m => <Pill key={m.v} label={m.label} active={m.v===markup} onClick={() => setMarkup(m.v)} />)}
                </div>
              </div>
            </div>
          </div>

          {/* ── GLOBAL SETTINGS BAR ── */}
          <div style={{ display:"flex", gap:8, marginBottom:14, flexWrap:"wrap", animation:"fadeUp 0.4s ease 0.12s both" }} className="no-print">
            <button onClick={() => setShowMaterials(v => !v)} style={{ display:"flex", alignItems:"center", gap:6, padding:"7px 12px", borderRadius:8, border: showMaterials ? "1px solid rgba(232,201,122,0.35)" : "1px solid rgba(255,255,255,0.08)", background: showMaterials ? "rgba(232,201,122,0.1)" : "rgba(255,255,255,0.03)", color: showMaterials ? "#e8c97a" : "rgba(255,255,255,0.38)", fontSize:11, fontWeight:600, cursor:"pointer", fontFamily:"inherit", transition:"all 0.15s" }}>
              <span>{showMaterials ? "◈" : "◇"}</span> {showMaterials ? "Showing material cost" : "Labor only (hide materials)"}
            </button>
            <button onClick={() => setClientBuysAll(v => !v)} style={{ display:"flex", alignItems:"center", gap:6, padding:"7px 12px", borderRadius:8, border: clientBuysAll ? "1px solid rgba(120,200,255,0.35)" : "1px solid rgba(255,255,255,0.08)", background: clientBuysAll ? "rgba(120,200,255,0.08)" : "rgba(255,255,255,0.03)", color: clientBuysAll ? "#7ec8e8" : "rgba(255,255,255,0.38)", fontSize:11, fontWeight:600, cursor:"pointer", fontFamily:"inherit", transition:"all 0.15s" }}>
              <span>{clientBuysAll ? "◉" : "○"}</span> {clientBuysAll ? "Client buys all parts" : "You supply all parts"}
            </button>
          </div>

          {/* ── TABS ── */}
          <div style={{ display:"flex", background:"rgba(255,255,255,0.025)", borderRadius:9, border:"1px solid rgba(255,255,255,0.065)", overflow:"hidden", marginBottom:14, animation:"fadeUp 0.4s ease 0.16s both" }} className="no-print">
            {TAB("services", `Services (${CATEGORIES.flatMap(c=>c.services).length})`)}
            {TAB("summary",  hasItems ? `Summary · $${total.toLocaleString()}` : "Summary")}
            {TAB("nec", "NEC 2023")}
          </div>

          {/* ════════════ SERVICES TAB ════════════ */}
          {tab==="services" && (
            <div style={{ animation:"fadeUp 0.3s ease both" }} className="no-print">
              {CATEGORIES.map(cat => (
                <CategorySection key={cat.id} category={cat} entries={entries} onUpdate={upd}
                  hourlyRate={hourlyRate} clientBuys={clientBuysAll} showMaterials={showMaterials} />
              ))}
            </div>
          )}

          {/* ════════════ SUMMARY TAB ════════════ */}
          {tab==="summary" && (
            <div style={{ animation:"fadeUp 0.3s ease both" }}>
              {!hasItems ? (
                <div style={{ textAlign:"center", padding:"48px 20px", color:"rgba(255,255,255,0.2)" }} className="no-print">
                  <div style={{ fontSize:30, marginBottom:10 }}>◎</div>
                  <div style={{ fontSize:13 }}>No services selected yet.</div>
                  <button onClick={() => setTab("services")} style={{ marginTop:18, padding:"9px 22px", background:"rgba(232,201,122,0.1)", border:"1px solid rgba(232,201,122,0.28)", borderRadius:8, color:"#e8c97a", fontSize:12, fontWeight:600, cursor:"pointer", fontFamily:"inherit" }}>Open Services</button>
                </div>
              ) : (
                <>
                  {/* ── STAT CARDS ── */}
                  <div style={{ display:"flex", gap:8, flexWrap:"wrap", marginBottom:16 }} className="no-print">
                    <StatCard label="Total Estimate"      value={`$${total.toLocaleString()}`}           color="#e8c97a" />
                    <StatCard label="Labor Only"          value={`$${totLab.toLocaleString()}`}          color="#a8e87e" sub={`${totHrs.toFixed(1)} hrs @ $${hourlyRate}/hr`} />
                    {showMaterials && <StatCard label="Materials (You)"    value={`$${totMat.toLocaleString()}`}          color="#7eb8e8" />}
                    {totClientBuysMat > 0 && <StatCard label="Client Buys (est.)" value={`$${totClientBuysMat.toLocaleString()}`} color="#e87eb8" sub="not charged to you" />}
                  </div>

                  {/* ── QUOTE PREVIEW CARD ── */}
                  <div style={{ background:"rgba(255,255,255,0.022)", border:"1px solid rgba(255,255,255,0.065)", borderRadius:13, padding:"20px", marginBottom:12 }} className="print-quote">
                    {/* Company header */}
                    <div style={{ display:"flex", alignItems:"center", gap:14, marginBottom:16, paddingBottom:16, borderBottom:"1px solid rgba(255,255,255,0.07)" }}>
                      {logoDataUrl
                        ? <img src={logoDataUrl} alt="logo" style={{ height:48, width:"auto", maxWidth:120, objectFit:"contain", borderRadius:6 }} />
                        : <div style={{ width:44, height:44, borderRadius:8, background:"linear-gradient(135deg,#e8c97a,#c9a84c)", display:"flex", alignItems:"center", justifyContent:"center", fontSize:20, fontWeight:800, color:"#0a0a0c", flexShrink:0 }}>
                            {(company.name || "W")[0].toUpperCase()}
                          </div>
                      }
                      <div style={{ flex:1 }}>
                        <div style={{ fontFamily:"'Syne',sans-serif", fontSize:15, fontWeight:800, color:"#fff", letterSpacing:"-0.02em" }}>{company.name || "Your Company Name"}</div>
                        <div style={{ fontSize:11, color:"rgba(255,255,255,0.4)", marginTop:2, lineHeight:1.6 }}>
                          {[company.phone, company.email, company.address, company.license && `Lic: ${company.license}`].filter(Boolean).join("  ·  ")}
                        </div>
                      </div>
                      <div style={{ textAlign:"right", flexShrink:0 }}>
                        <div style={{ fontSize:9, color:"rgba(255,255,255,0.3)", textTransform:"uppercase", letterSpacing:"0.08em" }}>Date</div>
                        <div style={{ fontSize:12, color:"rgba(255,255,255,0.6)", fontFamily:"'DM Mono',monospace" }}>{new Date().toLocaleDateString()}</div>
                      </div>
                    </div>

                    {/* Client info */}
                    {(clientName || jobName || clientEmail || clientPhone) && (
                      <div style={{ background:"rgba(255,255,255,0.03)", borderRadius:8, padding:"10px 12px", marginBottom:14, display:"grid", gridTemplateColumns:"1fr 1fr", gap:"4px 16px" }}>
                        {clientName  && <div style={{ fontSize:12 }}><span style={{ color:"rgba(255,255,255,0.35)" }}>Client: </span><span style={{ color:"#fff", fontWeight:600 }}>{clientName}</span></div>}
                        {jobName     && <div style={{ fontSize:12 }}><span style={{ color:"rgba(255,255,255,0.35)" }}>Job: </span><span style={{ color:"rgba(255,255,255,0.7)" }}>{jobName}</span></div>}
                        {clientEmail && <div style={{ fontSize:12 }}><span style={{ color:"rgba(255,255,255,0.35)" }}>Email: </span><span style={{ color:"rgba(255,255,255,0.7)" }}>{clientEmail}</span></div>}
                        {clientPhone && <div style={{ fontSize:12 }}><span style={{ color:"rgba(255,255,255,0.35)" }}>Phone: </span><span style={{ color:"rgba(255,255,255,0.7)" }}>{clientPhone}</span></div>}
                      </div>
                    )}

                    {/* Services provided */}
                    <div style={{ fontSize:9, color:"rgba(255,255,255,0.28)", textTransform:"uppercase", letterSpacing:"0.1em", marginBottom:10 }}>Services Provided</div>
                    {CATEGORIES.map(cat => {
                      const items = activeItems.filter(i => cat.services.find(s => s.id===i.id));
                      if (!items.length) return null;
                      return (
                        <div key={cat.id} style={{ marginBottom:12 }}>
                          <div style={{ fontSize:9, color:cat.color, textTransform:"uppercase", letterSpacing:"0.1em", fontWeight:700, marginBottom:5, opacity:0.85 }}>{cat.label}</div>
                          {items.map(item => (
                            <div key={item.id} style={{ display:"flex", alignItems:"flex-start", gap:8, padding:"6px 0", borderBottom:"1px solid rgba(255,255,255,0.04)" }}>
                              <div style={{ flex:1 }}>
                                <div style={{ fontSize:12, color:"rgba(255,255,255,0.78)", fontWeight:600 }}>{item.label}</div>
                                <div style={{ fontSize:10, color:"rgba(255,255,255,0.3)", fontFamily:"'DM Mono',monospace", marginTop:1 }}>
                                  {item.variantLabel} · {item.nec} · qty {item.qty}
                                  {item.cBuys ? <span style={{ color:"#7ec8e8", marginLeft:5 }}>· client supplies parts</span> : ""}
                                </div>
                              </div>
                              <div style={{ display:"flex", flexDirection:"column", alignItems:"flex-end", gap:1, flexShrink:0 }}>
                                <span style={{ fontSize:10, color:"rgba(162,220,160,0.75)", fontFamily:"'DM Mono',monospace" }}>lab ${item.lab.toLocaleString()}</span>
                                {showMaterials && !item.cBuys && <span style={{ fontSize:9, color:"rgba(255,255,255,0.28)", fontFamily:"'DM Mono',monospace" }}>mat ${item.mat.toLocaleString()}</span>}
                                <span style={{ fontSize:12, fontWeight:700, color:cat.color, fontFamily:"'DM Mono',monospace" }}>${item.lineTotal.toLocaleString()}</span>
                              </div>
                            </div>
                          ))}
                        </div>
                      );
                    })}

                    {/* Totals */}
                    <div style={{ background:"linear-gradient(135deg,rgba(232,201,122,0.065) 0%,rgba(255,255,255,0.018) 100%)", border:"1px solid rgba(232,201,122,0.18)", borderRadius:10, padding:"14px 16px", marginTop:8 }}>
                      {[
                        showMaterials && { label:"Materials (supplied by us)", val:totMat, color:"#7eb8e8" },
                        { label:`Labor — ${totHrs.toFixed(1)} hrs @ $${hourlyRate}/hr`, val:totLab, color:"#a8e87e" },
                        { label:`Markup — ${(markup*100).toFixed(0)}%`, val:markupAmt, color:"rgba(255,255,255,0.4)" },
                      ].filter(Boolean).map(row => (
                        <div key={row.label} style={{ display:"flex", justifyContent:"space-between", padding:"4px 0", fontSize:11 }}>
                          <span style={{ color:"rgba(255,255,255,0.38)" }}>{row.label}</span>
                          <span style={{ fontFamily:"'DM Mono',monospace", color:row.color, fontWeight:600 }}>${row.val.toLocaleString()}</span>
                        </div>
                      ))}
                      {totClientBuysMat > 0 && (
                        <div style={{ display:"flex", justifyContent:"space-between", padding:"4px 0", fontSize:11 }}>
                          <span style={{ color:"rgba(126,200,232,0.45)" }}>Client supplies parts (not charged)</span>
                          <span style={{ fontFamily:"'DM Mono',monospace", color:"rgba(126,200,232,0.45)" }}>~${totClientBuysMat.toLocaleString()}</span>
                        </div>
                      )}
                      <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", paddingTop:12, marginTop:6, borderTop:"1px solid rgba(232,201,122,0.18)" }}>
                        <span style={{ fontFamily:"'Syne',sans-serif", fontSize:14, fontWeight:800, color:"#fff" }}>Total Estimate</span>
                        <span style={{ fontFamily:"'DM Mono',monospace", fontSize:22, fontWeight:500, color:"#e8c97a", letterSpacing:"-0.03em" }}>${total.toLocaleString()}</span>
                      </div>
                    </div>

                    {/* Notes */}
                    {notes && (
                      <div style={{ marginTop:12, padding:"10px 12px", background:"rgba(255,255,255,0.03)", borderRadius:8, fontSize:11, color:"rgba(255,255,255,0.5)", lineHeight:1.6 }}>
                        <span style={{ color:"rgba(255,255,255,0.3)", textTransform:"uppercase", letterSpacing:"0.08em", fontSize:9 }}>Notes: </span>{notes}
                      </div>
                    )}

                    {/* Terms */}
                    {company.terms && (
                      <div style={{ marginTop:10, padding:"10px 12px", background:"rgba(255,255,255,0.02)", borderRadius:8, fontSize:10, color:"rgba(255,255,255,0.3)", lineHeight:1.7 }}>
                        <div style={{ color:"rgba(255,255,255,0.2)", textTransform:"uppercase", letterSpacing:"0.08em", fontSize:9, marginBottom:4 }}>Terms & Conditions</div>
                        {company.terms}
                      </div>
                    )}
                  </div>

                  {/* ── NOTES INPUT ── */}
                  <textarea placeholder="Job notes, permit info, scope exclusions, warranty..." value={notes} onChange={e => setNotes(e.target.value)} rows={2}
                    style={{ ...inputStyle, width:"100%", marginBottom:10, resize:"vertical", lineHeight:1.6 }} className="no-print"
                    onFocus={focusGold} onBlur={blurGray} />

                  {/* ── SEND ACTIONS ── */}
                  <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr 1fr", gap:8, marginBottom:8 }} className="no-print">
                    {[
                      { icon:"✉", label:"Email Quote", desc: clientEmail || "Open mail app", action: emailQuote, color:"#7eb8e8" },
                      { icon:"💬", label:"Text Quote",  desc: clientPhone || "Open messages", action: smsQuote,   color:"#a8e87e" },
                      { icon:"⎘",  label:"Copy Quote",  desc: copied ? "Copied!" : "Plain text",   action: copyQuote,  color: copied ? "#7dcea0" : "#e8c97a" },
                    ].map(btn => (
                      <button key={btn.label} onClick={btn.action} style={{ display:"flex", flexDirection:"column", alignItems:"center", gap:4, padding:"12px 8px", borderRadius:11, border:`1px solid ${btn.color}25`, background:`${btn.color}08`, cursor:"pointer", fontFamily:"inherit", transition:"all 0.2s" }}
                        onMouseEnter={e => e.currentTarget.style.background=`${btn.color}15`}
                        onMouseLeave={e => e.currentTarget.style.background=`${btn.color}08`}>
                        <span style={{ fontSize:18 }}>{btn.icon}</span>
                        <span style={{ fontSize:11, fontWeight:700, color:btn.color }}>{btn.label}</span>
                        <span style={{ fontSize:9, color:"rgba(255,255,255,0.3)" }}>{btn.desc}</span>
                      </button>
                    ))}
                  </div>

                  <button onClick={() => window.print()} style={{ width:"100%", padding:"11px", background:"rgba(255,255,255,0.04)", border:"1px solid rgba(255,255,255,0.1)", borderRadius:10, color:"rgba(255,255,255,0.5)", fontSize:12, fontWeight:600, cursor:"pointer", fontFamily:"inherit", transition:"all 0.2s" }} className="no-print"
                    onMouseEnter={e => e.currentTarget.style.background="rgba(255,255,255,0.08)"}
                    onMouseLeave={e => e.currentTarget.style.background="rgba(255,255,255,0.04)"}>
                    🖨 Print / Save as PDF
                  </button>
                </>
              )}
            </div>
          )}

          {/* ════════════ NEC REFERENCE TAB ════════════ */}
          {tab === "nec" && <NECReference />}

          <div style={{ textAlign:"center", marginTop:44, paddingTop:18, borderTop:"1px solid rgba(255,255,255,0.04)", fontSize:9, color:"rgba(255,255,255,0.13)", letterSpacing:"0.07em" }} className="no-print">
            WIREWAY · NEC 2023 RESIDENTIAL ESTIMATING · PROFESSIONAL GRADE
          </div>
        </div>
      </div>

      {/* ════════════ COMPANY PROFILE MODAL ════════════ */}
      {editingCompany && (
        <div className="modal-overlay" onClick={e => e.target===e.currentTarget && setEditingCompany(false)}>
          <div className="modal-box" style={{ padding:"24px" }}>
            <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:20 }}>
              <div>
                <div style={{ fontFamily:"'Syne',sans-serif", fontSize:16, fontWeight:800, color:"#fff" }}>Company Profile</div>
                <div style={{ fontSize:11, color:"rgba(255,255,255,0.3)", marginTop:2 }}>Appears on every quote you send</div>
              </div>
              <button onClick={() => setEditingCompany(false)} style={{ background:"transparent", border:"none", color:"rgba(255,255,255,0.4)", fontSize:20, cursor:"pointer", padding:"4px 8px" }}>✕</button>
            </div>

            {/* Logo upload */}
            <div style={{ marginBottom:16 }}>
              <div style={{ fontSize:10, color:"rgba(255,255,255,0.3)", textTransform:"uppercase", letterSpacing:"0.1em", marginBottom:8 }}>Company Logo</div>
              <div style={{ display:"flex", alignItems:"center", gap:12 }}>
                {logoDataUrl
                  ? <img src={logoDataUrl} alt="logo" style={{ height:52, width:"auto", maxWidth:140, objectFit:"contain", borderRadius:6, border:"1px solid rgba(255,255,255,0.1)" }} />
                  : <div style={{ width:52, height:52, borderRadius:8, background:"rgba(255,255,255,0.05)", border:"2px dashed rgba(255,255,255,0.15)", display:"flex", alignItems:"center", justifyContent:"center", fontSize:22, color:"rgba(255,255,255,0.2)" }}>⬡</div>
                }
                <div style={{ flex:1 }}>
                  <label style={{ display:"inline-block", padding:"8px 14px", background:"rgba(232,201,122,0.1)", border:"1px solid rgba(232,201,122,0.3)", borderRadius:7, color:"#e8c97a", fontSize:12, fontWeight:600, cursor:"pointer" }}>
                    {logoDataUrl ? "Change Logo" : "Upload Logo"}
                    <input type="file" accept="image/*" onChange={handleLogoUpload} style={{ display:"none" }} />
                  </label>
                  {logoDataUrl && (
                    <button onClick={() => setLogoDataUrl("")} style={{ marginLeft:8, padding:"8px 12px", background:"transparent", border:"1px solid rgba(255,255,255,0.1)", borderRadius:7, color:"rgba(255,255,255,0.4)", fontSize:11, cursor:"pointer", fontFamily:"inherit" }}>Remove</button>
                  )}
                  <div style={{ fontSize:10, color:"rgba(255,255,255,0.2)", marginTop:5 }}>PNG, JPG, SVG · max 2MB · appears on quotes</div>
                </div>
              </div>
            </div>

            {/* Company fields */}
            <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:8, marginBottom:12 }}>
              {[
                { ph:"Company name",      key:"name" },
                { ph:"Phone number",      key:"phone" },
                { ph:"Email address",     key:"email" },
                { ph:"Website",           key:"website" },
                { ph:"Street address",    key:"address" },
                { ph:"License number",    key:"license" },
              ].map(f => (
                <input key={f.key} placeholder={f.ph} value={companyDraft[f.key]||""} onChange={e => setCompanyDraft(p => ({ ...p, [f.key]: e.target.value }))}
                  style={inputStyle} onFocus={focusGold} onBlur={blurGray} />
              ))}
            </div>

            {/* Terms */}
            <div style={{ marginBottom:16 }}>
              <div style={{ fontSize:10, color:"rgba(255,255,255,0.3)", textTransform:"uppercase", letterSpacing:"0.1em", marginBottom:6 }}>Terms & Conditions (optional)</div>
              <textarea placeholder="Payment due within 30 days. 50% deposit required before work begins. Estimate valid for 30 days..."
                value={companyDraft.terms||""} onChange={e => setCompanyDraft(p => ({ ...p, terms: e.target.value }))}
                rows={3} style={{ ...inputStyle, resize:"vertical", lineHeight:1.6 }} onFocus={focusGold} onBlur={blurGray} />
            </div>

            <div style={{ display:"flex", gap:8 }}>
              <button onClick={saveCompany} style={{ flex:1, padding:"12px", background:"linear-gradient(135deg,rgba(232,201,122,0.2),rgba(232,201,122,0.08))", border:"1px solid rgba(232,201,122,0.4)", borderRadius:10, color:"#e8c97a", fontSize:13, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>
                Save Profile
              </button>
              <button onClick={() => setEditingCompany(false)} style={{ padding:"12px 20px", background:"transparent", border:"1px solid rgba(255,255,255,0.08)", borderRadius:10, color:"rgba(255,255,255,0.4)", fontSize:13, fontWeight:600, cursor:"pointer", fontFamily:"inherit" }}>
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
