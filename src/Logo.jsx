// src/Logo.jsx
// The Wireway mark — a single wire bent into a W.
// Terminal dots like schematic endpoints, a spark at the apex.
// Inherits the active theme accent via CSS variables and glows softly.

export function WirewayMark({ size = 30, glow = true, color = "var(--accent)" }) {
  return (
    <svg
      width={size} height={size} viewBox="0 0 64 64"
      fill="none" xmlns="http://www.w3.org/2000/svg"
      style={glow ? { filter: "drop-shadow(0 0 7px rgba(var(--accent-rgb),0.45))" } : undefined}
      aria-label="Wireway"
    >
      {/* the wire */}
      <path
        d="M10 20 L21 46 L32 26 L43 46 L54 20"
        stroke={color} strokeWidth="6.5"
        strokeLinecap="round" strokeLinejoin="round"
      />
      {/* terminal endpoints */}
      <circle cx="10" cy="20" r="4.5" fill={color} />
      <circle cx="54" cy="20" r="4.5" fill={color} />
      {/* the spark */}
      <path d="M32 8 L34.4 13.6 L40 16 L34.4 18.4 L32 24 L29.6 18.4 L24 16 L29.6 13.6 Z" fill={color} />
    </svg>
  );
}

export function WirewayLogo({ size = 30, fontSize = 16 }) {
  return (
    <span style={{ display: "inline-flex", alignItems: "center", gap: 9 }}>
      <WirewayMark size={size} />
      <span style={{ fontFamily: "'Syne',sans-serif", fontSize, fontWeight: 800, letterSpacing: "-0.02em", whiteSpace: "nowrap", color: "#fff" }}>
        <span style={{ color: "var(--accent)" }}>WIRE</span>WAY
      </span>
    </span>
  );
}
