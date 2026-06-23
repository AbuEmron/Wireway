// src/Logo.jsx
// The official Wireway Pro mark — twin plugs forming a W with a bolt between them,
// on the brand blue-to-green gradient chip. Vector recreation of the brand logo
// so it renders razor-sharp at every size.

export function WirewayMark({ size = 30, glow = false }) {
  return (
    <svg
      width={size} height={size} viewBox="0 0 96 96"
      fill="none" xmlns="http://www.w3.org/2000/svg"
      style={glow ? { filter: "drop-shadow(0 0 7px rgba(58,169,255,0.4))" } : undefined}
      aria-label="Wireway Pro"
    >
      <defs>
        <linearGradient id="wwg" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="#0a3d8f" />
          <stop offset="52%" stopColor="#1273d2" />
          <stop offset="100%" stopColor="#23a455" />
        </linearGradient>
      </defs>

      {/* brand chip */}
      <rect x="2" y="2" width="92" height="92" rx="21" fill="url(#wwg)" />

      {/* twin plug bodies forming the W */}
      <path d="M29 35 Q26 62 36 63 Q44 63 48 44" stroke="#fff" strokeWidth="8.5" strokeLinecap="round" fill="none" />
      <path d="M67 35 Q70 62 60 63 Q52 63 48 44" stroke="#fff" strokeWidth="8.5" strokeLinecap="round" fill="none" />

      {/* plug heads */}
      <rect x="22" y="23" width="14" height="15" rx="3.5" fill="#fff" transform="rotate(-12 29 30)" />
      <rect x="60" y="23" width="14" height="15" rx="3.5" fill="#fff" transform="rotate(12 67 30)" />

      {/* prongs */}
      <rect x="24.5" y="13.5" width="3.4" height="8" rx="1.7" fill="#ffd23f" transform="rotate(-12 26 17)" />
      <rect x="30.5" y="12.5" width="3.4" height="8" rx="1.7" fill="#ffd23f" transform="rotate(-12 32 16)" />
      <rect x="62.5" y="12.5" width="3.4" height="8" rx="1.7" fill="#ffd23f" transform="rotate(12 64 16)" />
      <rect x="68.5" y="13.5" width="3.4" height="8" rx="1.7" fill="#ffd23f" transform="rotate(12 70 17)" />

      {/* the bolt */}
      <path d="M52 21 L40 42 L46.5 42 L42 58 L57 35 L50 35 L54.5 21 Z" fill="#fff" />

      {/* energy arcs */}
      <path d="M30 71 Q48 80 66 71" stroke="#fff" strokeWidth="2.6" strokeLinecap="round" opacity="0.85" fill="none" />
      <path d="M35 77 Q48 83.5 61 77" stroke="#fff" strokeWidth="2" strokeLinecap="round" opacity="0.5" fill="none" />
    </svg>
  );
}

export function WirewayLogo({ size = 34, fontSize = 17, tagline = true }) {
  // The official lockup: brand chip + "Wireway Pro" + ELECTRICAL ESTIMATOR tagline.
  // Vector mark + system-rendered type = crisp at every size on every platform.
  return (
    <span style={{ display: "inline-flex", alignItems: "center", gap: 10 }}>
      <WirewayMark size={size} />
      <span style={{ display: "inline-flex", flexDirection: "column", lineHeight: 1.05 }}>
        <span style={{ fontFamily: "'Quicksand','DM Sans',sans-serif", fontSize, fontWeight: 700, letterSpacing: "0.01em", whiteSpace: "nowrap", color: "#fff" }}>
          Wireway<span style={{ fontWeight: 800, color: "#1f9ee0", marginLeft: Math.max(4, fontSize * 0.22) }}>Pro</span>
        </span>
        {tagline && (
          <span className="ww-tagline" style={{ fontSize: Math.max(7.5, fontSize * 0.40), letterSpacing: "0.24em", color: "rgba(255,255,255,0.5)", fontWeight: 600, whiteSpace: "nowrap", marginTop: 2 }}>
            ELECTRICAL ESTIMATOR
          </span>
        )}
      </span>
    </span>
  );
}
