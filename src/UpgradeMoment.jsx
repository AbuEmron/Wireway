// src/UpgradeMoment.jsx
// The "aha" paywall. Shown ONCE, the first time a free user applies an AI-built
// estimate — i.e. at peak perceived value, the moment they see the tool actually
// works. It celebrates what just happened, anchors the price against the value of
// the job they just quoted, and offers the upgrade. Dismissable (no hard wall) so
// it never blocks the trial — it just converts at the high point.
//
// Props:
//   onUpgrade()  -> open the pricing/checkout page
//   onClose()    -> dismiss and keep using the current plan
//   itemCount    -> optional, number of services the AI just added
//   jobTotal     -> optional, dollar subtotal of the estimate just built

export default function UpgradeMoment({ onUpgrade, onClose, itemCount, jobTotal }) {
  const wins = [
    { icon: "♾️", t: "Unlimited saved quotes", d: "Free stops at 3. Pro never stops." },
    { icon: "💳", t: "Get paid by card", d: "Clients sign and pay your quote from their phone." },
    { icon: "📄", t: "Branded pro proposals", d: "Your logo, your terms — sent by text or email." },
    { icon: "📊", t: "Profit grade on every job", d: "See your margin before you send. Stop underbidding." },
  ];

  const fmt = (n) => (typeof n === "number" && n > 0)
    ? "$" + Math.round(n).toLocaleString()
    : null;
  const total = fmt(jobTotal);

  return (
    <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div style={{
        width: "100%", maxWidth: 420, background: "var(--card, #14161c)",
        border: "1px solid rgba(var(--accent-rgb),0.4)", borderRadius: 18,
        padding: "26px 22px", position: "relative",
        boxShadow: "0 24px 70px rgba(0,0,0,0.6)", fontFamily: "inherit", color: "#fff",
      }}>
        <button onClick={onClose} aria-label="Close" style={{ position: "absolute", top: 12, right: 14, background: "transparent", border: "none", color: "rgba(255,255,255,0.4)", fontSize: 20, cursor: "pointer", fontFamily: "inherit" }}>✕</button>

        <div style={{ display: "inline-flex", alignItems: "center", gap: 7, padding: "5px 12px", background: "rgba(100,220,130,0.12)", border: "1px solid rgba(100,220,130,0.35)", borderRadius: 30, fontSize: 11, fontWeight: 700, color: "#7dcea0", marginBottom: 14 }}>
          ✓ Estimate built{typeof itemCount === "number" && itemCount > 0 ? ` — ${itemCount} services` : ""}
        </div>

        <div style={{ fontFamily: "'Syne',sans-serif", fontSize: 24, fontWeight: 800, letterSpacing: "-0.02em", lineHeight: 1.15, marginBottom: 8 }}>
          That took seconds.{total ? <> Now send the <span style={{ color: "var(--accent)" }}>{total}</span> quote and get paid.</> : <> Now go win the job.</>}
        </div>

        <div style={{ fontSize: 13, color: "rgba(255,255,255,0.6)", lineHeight: 1.6, marginBottom: 18 }}>
          {total
            ? <>One won job like this pays for <strong style={{ color: "#fff" }}>years</strong> of Wireway. Unlock everything for <strong style={{ color: "#fff" }}>$12/mo</strong> — less than a single service call.</>
            : <>One won job pays for <strong style={{ color: "#fff" }}>years</strong> of Wireway. Unlock everything for <strong style={{ color: "#fff" }}>$12/mo</strong> — less than a single service call.</>}
        </div>

        <div style={{ display: "grid", gap: 8, marginBottom: 20 }}>
          {wins.map((w) => (
            <div key={w.t} style={{ display: "flex", gap: 11, alignItems: "flex-start", background: "rgba(255,255,255,0.03)", border: "1px solid rgba(255,255,255,0.07)", borderRadius: 11, padding: "11px 12px" }}>
              <span style={{ fontSize: 16, lineHeight: 1.2, flexShrink: 0 }}>{w.icon}</span>
              <span style={{ flex: 1, minWidth: 0 }}>
                <span style={{ display: "block", fontSize: 12.5, fontWeight: 700 }}>{w.t}</span>
                <span style={{ display: "block", fontSize: 11, color: "rgba(255,255,255,0.45)", lineHeight: 1.5 }}>{w.d}</span>
              </span>
            </div>
          ))}
        </div>

        <button onClick={onUpgrade} style={{ width: "100%", padding: "15px", borderRadius: 12, background: "var(--accent)", border: "none", color: "#07121d", fontSize: 15, fontWeight: 800, cursor: "pointer", fontFamily: "inherit", boxShadow: "0 6px 24px rgba(var(--accent-rgb),0.35)" }}>
          Unlock everything — $12/mo
        </button>
        <div style={{ fontSize: 11, color: "rgba(255,255,255,0.35)", textAlign: "center", marginTop: 10 }}>
          You're still on your free trial · cancel anytime
        </div>
        <button onClick={onClose} style={{ width: "100%", padding: "9px", marginTop: 6, background: "transparent", border: "none", color: "rgba(255,255,255,0.4)", fontSize: 12, fontWeight: 600, cursor: "pointer", fontFamily: "inherit" }}>
          Keep looking around first
        </button>
      </div>
    </div>
  );
}
