// src/themes.js
// Wireway theme engine 2.0 — each theme is a full atmosphere, not a paint swap:
// scene (background scenery), line (hairline light), card (surface fill), accent.
// Client-facing pages (proposals, pay links, landing) stay on brand.

export const THEMES = [
  {
    id: "wireway",
    name: "Wireway",
    desc: "Official brand. Blue aurora, live current.",
    accent: "#3aa9ff",
    accentRgb: "58,169,255",
    bg: "#080b10",
    surface: "#0e1420",
    scene: "radial-gradient(ellipse 70% 38% at 50% 0%, rgba(58,169,255,0.10), transparent 60%), radial-gradient(ellipse 60% 42% at 90% 100%, rgba(35,164,85,0.07), transparent 60%), #080b10",
    line: "rgba(98,165,255,0.13)",
    lineStrong: "rgba(98,165,255,0.22)",
    card: "rgba(58,169,255,0.045)",
    free: true,
  },
  {
    id: "gold",
    name: "Gold Standard",
    desc: "The original. Brass on matte black.",
    accent: "#e8c97a",
    accentRgb: "232,201,122",
    bg: "#0a0a0c",
    surface: "#15120c",
    scene: "radial-gradient(ellipse 80% 40% at 50% 0%, rgba(232,201,122,0.08), transparent 55%), #0a0a0c",
    line: "rgba(232,201,122,0.12)",
    lineStrong: "rgba(232,201,122,0.22)",
    card: "rgba(232,201,122,0.045)",
    free: true,
  },
  {
    id: "copper",
    name: "Copper",
    desc: "Warm grain, fresh-stripped conductor.",
    accent: "#e89a6a",
    accentRgb: "232,154,106",
    bg: "#0c0a09",
    surface: "#171008",
    scene: "radial-gradient(ellipse 70% 35% at 18% 0%, rgba(232,154,106,0.11), transparent 55%), radial-gradient(ellipse 55% 35% at 100% 85%, rgba(232,108,60,0.06), transparent 60%), repeating-linear-gradient(0deg, rgba(255,200,160,0.012) 0px, rgba(255,200,160,0.012) 1px, transparent 1px, transparent 4px), #0c0a09",
    line: "rgba(232,154,106,0.15)",
    lineStrong: "rgba(232,154,106,0.26)",
    card: "rgba(232,154,106,0.055)",
    free: false,
  },
  {
    id: "voltage",
    name: "High Voltage",
    desc: "Blueprint grid. Engineer's table.",
    accent: "#6ab8e8",
    accentRgb: "106,184,232",
    bg: "#090b0e",
    surface: "#0c1218",
    scene: "radial-gradient(ellipse 75% 40% at 50% 0%, rgba(106,184,232,0.09), transparent 55%), repeating-linear-gradient(0deg, rgba(106,184,232,0.05) 0px, rgba(106,184,232,0.05) 1px, transparent 1px, transparent 30px), repeating-linear-gradient(90deg, rgba(106,184,232,0.05) 0px, rgba(106,184,232,0.05) 1px, transparent 1px, transparent 30px), #090b0e",
    line: "rgba(106,184,232,0.16)",
    lineStrong: "rgba(106,184,232,0.28)",
    card: "rgba(106,184,232,0.05)",
    free: false,
  },
  {
    id: "ground",
    name: "Ground",
    desc: "Grounding green, rising from below.",
    accent: "#6ede96",
    accentRgb: "110,222,150",
    bg: "#090c0a",
    surface: "#0c1510",
    scene: "radial-gradient(ellipse 75% 48% at 50% 105%, rgba(110,222,150,0.10), transparent 60%), linear-gradient(180deg, rgba(110,222,150,0.025), transparent 140px), #090c0a",
    line: "rgba(110,222,150,0.13)",
    lineStrong: "rgba(110,222,150,0.24)",
    card: "rgba(110,222,150,0.045)",
    free: false,
  },
  {
    id: "neutral",
    name: "Neutral",
    desc: "Silver on graphite. Zero noise.",
    accent: "#dce0e8",
    accentRgb: "220,224,232",
    bg: "#0a0a0b",
    surface: "#141416",
    scene: "linear-gradient(180deg, rgba(255,255,255,0.04), transparent 120px), #0a0a0b",
    line: "rgba(255,255,255,0.10)",
    lineStrong: "rgba(255,255,255,0.18)",
    card: "rgba(255,255,255,0.04)",
    free: false,
  },
];

export function applyTheme(themeId) {
  const t = THEMES.find(x => x.id === themeId) || THEMES[0];
  const r = document.documentElement.style;
  r.setProperty("--accent", t.accent);
  r.setProperty("--accent-rgb", t.accentRgb);
  r.setProperty("--bg0", t.bg);
  r.setProperty("--surface", t.surface);
  r.setProperty("--bg-scene", t.scene);
  r.setProperty("--line", t.line);
  r.setProperty("--line-strong", t.lineStrong);
  r.setProperty("--card", t.card);
  return t;
}

export function getSavedTheme() {
  try { return window.localStorage.getItem("wireway_theme") || "wireway"; } catch { return "wireway"; }
}

export function saveTheme(id) {
  try { window.localStorage.setItem("wireway_theme", id); } catch { /* in-app browsers may block storage */ }
}
