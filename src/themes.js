// src/themes.js
// Wireway theme engine — five looks named from the trade.
// Themes drive CSS variables; client-facing pages (proposals, pay links) stay brand gold.

export const THEMES = [
  {
    id: "gold",
    name: "Gold Standard",
    desc: "The original. Brass on matte black.",
    accent: "#e8c97a",
    accentRgb: "232,201,122",
    bg: "#0a0a0c",
    surface: "#111115",
    free: true,
  },
  {
    id: "copper",
    name: "Copper",
    desc: "Warm as a fresh-stripped conductor.",
    accent: "#e89a6a",
    accentRgb: "232,154,106",
    bg: "#0c0a09",
    surface: "#15110e",
    free: false,
  },
  {
    id: "voltage",
    name: "High Voltage",
    desc: "Arc-flash blue. Cool and sharp.",
    accent: "#6ab8e8",
    accentRgb: "106,184,232",
    bg: "#090b0e",
    surface: "#0e1217",
    free: false,
  },
  {
    id: "ground",
    name: "Ground",
    desc: "Grounding-conductor green. Steady.",
    accent: "#6ede96",
    accentRgb: "110,222,150",
    bg: "#090c0a",
    surface: "#0e1410",
    free: false,
  },
  {
    id: "neutral",
    name: "Neutral",
    desc: "Silver on black. All business.",
    accent: "#dce0e8",
    accentRgb: "220,224,232",
    bg: "#0a0a0b",
    surface: "#121214",
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
  return t;
}

export function getSavedTheme() {
  try { return window.localStorage.getItem("wireway_theme") || "gold"; } catch { return "gold"; }
}

export function saveTheme(id) {
  try { window.localStorage.setItem("wireway_theme", id); } catch { /* in-app browsers may block storage */ }
}
