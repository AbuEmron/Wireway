// src/data/industrial-assemblies.js
// ═══ WIREWAY ELITE — ASSEMBLIES & CREWS ═══
// Assemblies are how industrial estimators actually think: one line = a complete
// composed scope (a device drop, a motor hookup, a feeder per foot) that expands
// into catalog items automatically. The incumbents make you spend weeks building
// these; Elite ships them ready — and the AI layer can compose custom ones from
// plain English.

import { findIndustrialItem, computeIndustrialLine } from "./industrial-catalog.js";

// ── COMPOSITE CREWS (blended hourly-rate factors applied to your base rate) ──
export const CREWS = [
  { id: "jw",      label: "Journeyman (solo)",            factor: 1.00 },
  { id: "jw_app",  label: "JW + Apprentice (blended)",    factor: 0.82 },
  { id: "two_jw",  label: "Two Journeymen (heavy gear)",  factor: 1.00 },
  { id: "foreman", label: "Foreman-led crew (blended)",   factor: 1.10 },
];

export function crewRate(baseRate, crewId) {
  const crew = CREWS.find(c => c.id === crewId);
  return baseRate * (crew ? crew.factor : 1);
}

// ── PREBUILT ASSEMBLIES ──
// driver: "ft" assemblies scale per foot of run; "ea" per unit.
// components: { id (catalog item), variant (exact label), qty (per driver unit) }
export const ASSEMBLIES = [
  {
    id: "asm_branch_emt", label: 'Branch Circuit — 3/4" EMT, 2#12+G', driver: "ft",
    desc: "Conduit, conductors, and ground per foot of branch run",
    components: [
      { id: "emt_run",   variant: '3/4"', qty: 1 },
      { id: "thhn_pull", variant: "#12",  qty: 3 },
    ],
  },
  {
    id: "asm_feeder_100a", label: '100A Feeder — 1-1/4" EMT, 4#2+G', driver: "ft",
    desc: "Per foot of feeder run, conductors and ground included",
    components: [
      { id: "emt_run",   variant: '1-1/4"', qty: 1 },
      { id: "thhn_pull", variant: "#2",     qty: 4 },
      { id: "thhn_pull", variant: "#8",     qty: 1 },
    ],
  },
  {
    id: "asm_feeder_400a", label: '400A Feeder — 3" EMT, 4×500MCM+G', driver: "ft",
    desc: "Heavy feeder per foot — raceway, phase conductors, ground",
    components: [
      { id: "emt_run",   variant: '3"',     qty: 1 },
      { id: "thhn_pull", variant: "500MCM", qty: 4 },
      { id: "thhn_pull", variant: "#2",     qty: 1 },
    ],
  },
  {
    id: "asm_feeder_terms_400a", label: "400A Feeder Termination Set (per end)", driver: "ea",
    desc: "Land one end of a 4×500MCM+G feeder",
    components: [
      { id: "termination", variant: "250–500MCM", qty: 4 },
      { id: "termination", variant: "#6–#2",      qty: 1 },
    ],
  },
  {
    id: "asm_device_drop", label: "Receptacle Drop from Tray (complete)", driver: "ea",
    desc: "12 ft of 3/4\" EMT, conductors, and a 480V twist-lock — one tap, whole drop",
    components: [
      { id: "emt_run",   variant: '3/4"',     qty: 12 },
      { id: "thhn_pull", variant: "#12",      qty: 36 },
      { id: "twistlock", variant: "20A 480V", qty: 1 },
    ],
  },
  {
    id: "asm_motor_30hp", label: "30 HP Motor Hookup (complete)", driver: "ea",
    desc: "Connection, fused disconnect, and terminations — feeder run priced separately",
    components: [
      { id: "motor_conn",  variant: "20–30 HP", qty: 1 },
      { id: "disconnect",  variant: "60A F",    qty: 1 },
      { id: "termination", variant: "#6–#2",    qty: 6 },
    ],
  },
  {
    id: "asm_mcc_bucket", label: "MCC Bucket Complete (w/ controls)", driver: "ea",
    desc: "Bucket install, I/O points landed, control wiring allowance",
    components: [
      { id: "starter",   variant: "Size 1",        qty: 1 },
      { id: "io_term",   variant: "Per point",     qty: 6 },
      { id: "ctrl_wire", variant: "18/2 shielded", qty: 50 },
    ],
  },
  {
    id: "asm_highbay_row", label: "High-Bay Row of 4 (wired)", driver: "ea",
    desc: "Four 240W high-bays with branch raceway and conductors between them",
    components: [
      { id: "highbay",   variant: "240W", qty: 4 },
      { id: "emt_run",   variant: '3/4"', qty: 60 },
      { id: "thhn_pull", variant: "#12",  qty: 180 },
    ],
  },
  {
    id: "asm_weld_station", label: "Welding Station Complete", driver: "ea",
    desc: "50A receptacle, disconnect, 40 ft of 1\" EMT with #6 conductors, landed both ends",
    components: [
      { id: "weld_recep",  variant: "50A 480V", qty: 1 },
      { id: "disconnect",  variant: "60A F",    qty: 1 },
      { id: "emt_run",     variant: '1"',       qty: 40 },
      { id: "thhn_pull",   variant: "#6",       qty: 120 },
      { id: "termination", variant: "#6–#2",    qty: 6 },
    ],
  },
  {
    id: "asm_xfmr75", label: "75 kVA Transformer Set (complete)", driver: "ea",
    desc: "Set and connect with bonding and primary/secondary terminations",
    components: [
      { id: "xfmr",        variant: "75 kVA",   qty: 1 },
      { id: "bond_jumper", variant: "Standard", qty: 2 },
      { id: "termination", variant: "1/0–4/0",  qty: 8 },
    ],
  },
  {
    id: "asm_ground_triad", label: "Grounding Triad (3 rods, ring, welds)", driver: "ea",
    desc: "Three driven rods, 30 ft of bare 4/0 ring, four exothermic welds",
    components: [
      { id: "ground_rod",  variant: '5/8" × 8ft',    qty: 3 },
      { id: "ground_ring", variant: "Direct buried", qty: 30 },
      { id: "cadweld",     variant: "Standard",      qty: 4 },
    ],
  },
];

// Expand an assembly into priced catalog lines.
// qty = number of driver units (feet for "ft" assemblies, count for "ea").
export function expandAssembly(assemblyId, qty, rate, conditionIds = [], crewId = "jw") {
  const asm = ASSEMBLIES.find(a => a.id === assemblyId);
  if (!asm) return null;
  const effRate = crewRate(rate, crewId);
  const lines = [];
  let matTotal = 0, laborHrs = 0, labTotal = 0;

  for (const comp of asm.components) {
    const item = findIndustrialItem(comp.id);
    if (!item) return null; // broken reference — caught by validation
    const variant = item.variants.find(v => v.label === comp.variant);
    if (!variant) return null;
    const compQty = comp.qty * qty;
    const line = computeIndustrialLine(variant, compQty, effRate, conditionIds);
    lines.push({ itemId: comp.id, label: item.label, variant: comp.variant, unit: item.unit, qty: compQty, ...line });
    matTotal += line.matTotal; laborHrs += line.laborHrs; labTotal += line.labTotal;
  }
  return { assembly: asm.label, driver: asm.driver, qty, lines, matTotal, laborHrs, labTotal, total: matTotal + labTotal };
}
