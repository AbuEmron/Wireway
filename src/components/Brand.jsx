/*
  Brand — the official Wireway Pro lockup (icon + wordmark).

  Self-contained on purpose: no imports, so a bad path can never break the build.
  Shows the brand W icon (from /public) next to the "Wireway Pro" wordmark.
  If the icon file is ever missing, it hides gracefully and the wordmark stays.

  Props:
    pro   — show the "Pro" suffix (default true)
    size  — base font size in px; the icon scales from it (default 18)
    src   — icon path (default /logo192.png, your official app icon)
*/

export default function Brand({ pro = true, size = 18, src = '/logo192.png' }) {
  return (
    <div
      className="ww-brand"
      style={{ display: 'inline-flex', alignItems: 'center', gap: Math.round(size * 0.5), fontSize: size }}
    >
      <img
        className="ww-brand__logo"
        src={src}
        alt="Wireway Pro"
        style={{ height: Math.round(size * 1.7), width: 'auto', display: 'block' }}
        onError={(e) => { e.currentTarget.style.display = 'none'; }}
      />
      <span className="ww-brand__word" style={{ fontWeight: 700, color: '#fff', letterSpacing: '0.01em', whiteSpace: 'nowrap' }}>
        Wireway{pro && <span className="ww-brand__pro" style={{ color: '#1f9ee0', fontWeight: 800, marginLeft: Math.round(size * 0.28) }}>Pro</span>}
      </span>
    </div>
  );
}
