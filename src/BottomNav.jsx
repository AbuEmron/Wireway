// src/BottomNav.jsx — unified mobile-first navigation: primary bottom tab bar + grouped "More" sheet.
// Presentational: parent passes `active`, `onGo(dest)`, and renders <MoreSheet> when needed.
import { useEffect } from "react";

const TABS = [
  { id: "home",     label: "Home",     icon: "🏠" },
  { id: "estimate", label: "Estimate", icon: "⚡" },
  { id: "calendar", label: "Calendar", icon: "📅" },
  { id: "clients",  label: "Clients",  icon: "👥" },
  { id: "more",     label: "More",     icon: "☰" },
];

export function BottomNav({ active = "estimate", onGo, attention = 0 }) {
  return (
    <nav style={{
      position: "fixed", left: 0, right: 0, bottom: 0, zIndex: 300,
      display: "flex", justifyContent: "space-around", alignItems: "stretch",
      background: "rgba(10,10,12,0.92)", backdropFilter: "blur(20px)", WebkitBackdropFilter: "blur(20px)",
      borderTop: "1px solid var(--line)", paddingBottom: "env(safe-area-inset-bottom, 0px)",
    }} className="no-print">
      {TABS.map((t) => {
        const on = active === t.id;
        return (
          <button key={t.id} onClick={() => onGo(t.id)} aria-label={t.label} style={{
            flex: 1, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 3,
            padding: "8px 2px 7px", border: "none", background: "transparent", cursor: "pointer", fontFamily: "inherit",
            position: "relative", color: on ? "var(--accent)" : "rgba(255,255,255,0.45)",
          }}>
            <span style={{ position: "absolute", top: 0, left: "50%", transform: "translateX(-50%)", width: 26, height: 2, borderRadius: 2, background: on ? "var(--accent)" : "transparent" }} />
            <span style={{ fontSize: 17, lineHeight: 1, position: "relative" }}>
              {t.icon}
              {t.id === "calendar" && attention > 0 && (
                <span style={{ position: "absolute", top: -4, right: -8, minWidth: 14, height: 14, padding: "0 3px", borderRadius: 7, background: "#f0a818", color: "#1a1205", fontSize: 9, fontWeight: 800, display: "flex", alignItems: "center", justifyContent: "center" }}>{attention}</span>
              )}
            </span>
            <span style={{ fontSize: 10, fontWeight: on ? 700 : 500, letterSpacing: "0.01em" }}>{t.label}</span>
          </button>
        );
      })}
    </nav>
  );
}

function Row({ icon, label, hint, onClick, danger }) {
  return (
    <button onClick={onClick} style={{
      width: "100%", display: "flex", alignItems: "center", gap: 12, padding: "13px 14px",
      border: "1px solid var(--line)", borderRadius: 11, background: "var(--card)", cursor: "pointer",
      fontFamily: "inherit", textAlign: "left", marginBottom: 8,
    }}>
      <span style={{ fontSize: 17, width: 22, textAlign: "center" }}>{icon}</span>
      <span style={{ flex: 1, minWidth: 0 }}>
        <span style={{ display: "block", fontSize: 13.5, fontWeight: 700, color: danger ? "#e87e7e" : "#fff" }}>{label}</span>
        {hint && <span style={{ display: "block", fontSize: 10.5, color: "rgba(255,255,255,0.4)", marginTop: 1 }}>{hint}</span>}
      </span>
      <span style={{ color: "rgba(255,255,255,0.25)", fontSize: 14 }}>›</span>
    </button>
  );
}

export function MoreSheet({ onGo, onClose, onSignOut, isElite = false }) {
  useEffect(() => {
    const onKey = (e) => e.key === "Escape" && onClose();
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [onClose]);

  const Group = ({ title, children }) => (
    <div style={{ marginBottom: 18 }}>
      <div style={{ fontSize: 10, color: "rgba(255,255,255,0.4)", textTransform: "uppercase", letterSpacing: "0.1em", fontWeight: 700, marginBottom: 9 }}>{title}</div>
      {children}
    </div>
  );

  return (
    <div onClick={(e) => e.target === e.currentTarget && onClose()} style={{
      position: "fixed", inset: 0, zIndex: 350, background: "rgba(0,0,0,0.6)", backdropFilter: "blur(6px)",
      display: "flex", alignItems: "flex-end", justifyContent: "center",
    }} className="no-print">
      <style>{`@keyframes moreup{from{transform:translateY(24px);opacity:0}to{transform:translateY(0);opacity:1}}`}</style>
      <div style={{
        width: "100%", maxWidth: 560, maxHeight: "82vh", overflowY: "auto",
        background: "var(--surface)", borderTop: "1px solid var(--line-strong)",
        borderRadius: "20px 20px 0 0", padding: "18px 18px calc(20px + env(safe-area-inset-bottom,0px))",
        animation: "moreup 0.2s ease both",
      }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 18 }}>
          <span style={{ fontFamily: "'Syne',sans-serif", fontSize: 17, fontWeight: 800, color: "#fff" }}>More</span>
          <button onClick={onClose} style={{ background: "transparent", border: "none", color: "rgba(255,255,255,0.45)", fontSize: 22, cursor: "pointer", lineHeight: 1 }}>✕</button>
        </div>

        <Group title="Money">
          <Row icon="🏦" label="Bank / Expenses" hint="Auto-import & categorize purchases" onClick={() => onGo("bank")} />
          <Row icon="🚗" label="Mileage" hint="Live GPS or manual · IRS deduction" onClick={() => onGo("mileage")} />
          <Row icon="🧾" label="Expenses & tax export" hint="Schedule C summary" onClick={() => onGo("expenses")} />
        </Group>

        <Group title="Estimating tools">
          <Row icon="⚡" label="AI Quote Builder" onClick={() => onGo("ai")} />
          <Row icon="🔌" label="Load Advisor" onClick={() => onGo("advisor")} />
          <Row icon="📐" label="Wire Calculator" onClick={() => onGo("wirecalc")} />
          <Row icon="🧮" label="Load Calculator" onClick={() => onGo("loadcalc")} />
          <Row icon="✓" label="Job Checklist" onClick={() => onGo("checklist")} />
          <Row icon="§" label="NEC Reference" onClick={() => onGo("nec")} />
        </Group>

        {isElite && (
          <Group title="Industrial">
            <Row icon="⚙️" label="Wireway Elite — Industrial Estimator" onClick={() => onGo("elite")} />
          </Group>
        )}

        <Group title="Account">
          <Row icon="🏢" label="Business info & logo" onClick={() => onGo("company")} />
          <Row icon="⚙️" label="Settings & plan" onClick={() => onGo("settings")} />
          {onSignOut && <Row icon="↩" label="Sign out" danger onClick={onSignOut} />}
        </Group>
      </div>
    </div>
  );
}
