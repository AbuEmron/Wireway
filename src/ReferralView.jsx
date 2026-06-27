/* eslint-disable react-hooks/exhaustive-deps */
// src/ReferralView.jsx — Quote→Lead flywheel, contractor side  ·  Phase 2 · Feature 5
import { useState, useEffect } from "react";
import { myRefLink, getReferralStats } from "./lib/referral";

const BLUE = "#7eb8e8", GREEN = "#7dcea0", GOLD = "#e8c97a";

export default function ReferralView({ user, onClose }) {
  const [stats, setStats] = useState(null);
  const [copied, setCopied] = useState(false);
  const link = user?.id ? myRefLink(user.id, "link") : "";

  useEffect(() => {
    if (!user?.id) return;
    getReferralStats(user.id).then(setStats).catch(() => setStats({ visits: 0, signups: 0 }));
  }, [user?.id]);

  const copy = () => { navigator.clipboard.writeText(link); setCopied(true); setTimeout(() => setCopied(false), 2000); };

  const wrap = { position: "fixed", inset: 0, zIndex: 150, background: "rgba(0,0,0,0.82)", backdropFilter: "blur(8px)", overflowY: "auto", display: "flex", justifyContent: "center", alignItems: "flex-start", padding: "24px 16px" };
  const panel = { background: "#111115", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 18, width: "100%", maxWidth: 520, padding: "24px" };

  return (
    <div style={wrap}>
      <div style={panel}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 18 }}>
          <div>
            <div style={{ fontFamily: "'Syne',sans-serif", fontSize: 17, fontWeight: 800, color: "#fff" }}>Spread the word</div>
            <div style={{ fontSize: 11, color: "rgba(255,255,255,0.35)", marginTop: 2 }}>Every quote &amp; invoice you send carries your link</div>
          </div>
          <button onClick={onClose} style={{ background: "transparent", border: "none", color: "rgba(255,255,255,0.4)", fontSize: 22, cursor: "pointer" }}>✕</button>
        </div>

        <div style={{ background: "linear-gradient(135deg,rgba(126,184,232,0.1),rgba(255,255,255,0.02))", border: "1px solid rgba(126,184,232,0.22)", borderRadius: 14, padding: "16px", marginBottom: 16 }}>
          <div style={{ fontSize: 10, color: "rgba(255,255,255,0.4)", textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 8 }}>Your referral link</div>
          <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 12, color: BLUE, wordBreak: "break-all", marginBottom: 10 }}>{link}</div>
          <button onClick={copy} style={{ width: "100%", padding: "11px", borderRadius: 9, background: copied ? "rgba(125,206,160,0.1)" : "linear-gradient(135deg,rgba(126,184,232,0.2),rgba(126,184,232,0.07))", border: `1px solid ${copied ? "rgba(125,206,160,0.35)" : "rgba(126,184,232,0.4)"}`, color: copied ? GREEN : BLUE, fontSize: 13, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>{copied ? "✓ Copied" : "Copy link"}</button>
        </div>

        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8, marginBottom: 16 }}>
          <div style={{ background: "rgba(255,255,255,0.025)", border: "1px solid rgba(255,255,255,0.07)", borderRadius: 11, padding: "14px" }}>
            <div style={{ fontSize: 10, color: "rgba(255,255,255,0.35)", textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 6 }}>Visits</div>
            <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 22, fontWeight: 600, color: BLUE }}>{stats ? stats.visits : "…"}</div>
          </div>
          <div style={{ background: "rgba(255,255,255,0.025)", border: "1px solid rgba(255,255,255,0.07)", borderRadius: 11, padding: "14px" }}>
            <div style={{ fontSize: 10, color: "rgba(255,255,255,0.35)", textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 6 }}>Signups</div>
            <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 22, fontWeight: 600, color: GOLD }}>{stats ? stats.signups : "…"}</div>
          </div>
        </div>

        <div style={{ fontSize: 11, color: "rgba(255,255,255,0.4)", lineHeight: 1.7, background: "rgba(255,255,255,0.02)", border: "1px solid rgba(255,255,255,0.06)", borderRadius: 10, padding: "12px 14px" }}>
          Every quote and pay page you send already shows a subtle “Powered by Wireway — get quotes like this” link. When a fellow contractor taps it and signs up, it shows here. Share the link directly too.
        </div>
      </div>
    </div>
  );
}
