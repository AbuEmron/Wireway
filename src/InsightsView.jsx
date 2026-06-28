/* eslint-disable react-hooks/exhaustive-deps */
// src/InsightsView.jsx — "what your own data is teaching you"  ·  Phase 2 · Feature 3
import { useState, useEffect } from "react";
import { getInsights } from "./lib/insights";

const pct = (n) => (n == null ? "—" : `${(n * 100).toFixed(0)}%`);
const mult = (n) => (n == null ? "—" : `${n.toFixed(2)}×`);
const money = (n) => (n == null ? "—" : "$" + Math.round(n).toLocaleString("en-US"));
const GREEN = "#7dcea0", RED = "#e87e7e", BLUE = "#7eb8e8";

export default function InsightsView({ user, onClose }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user?.id) return;
    getInsights(user.id).then((d) => { setData(d); setLoading(false); });
  }, [user?.id]);

  const wrap = { position: "fixed", inset: 0, zIndex: 360, background: "rgba(0,0,0,0.82)", backdropFilter: "blur(8px)", overflowY: "auto", display: "flex", justifyContent: "center", alignItems: "flex-start", padding: "24px 16px" };
  const panel = { background: "#111115", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 18, width: "100%", maxWidth: 700, padding: "24px" };

  const overrunColor = (m) => (m == null ? "#fff" : m > 1.05 ? RED : m < 0.98 ? GREEN : "#fff");

  return (
    <div style={wrap}>
      <div style={panel}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 18 }}>
          <div>
            <div style={{ fontFamily: "'Syne',sans-serif", fontSize: 17, fontWeight: 800, color: "#fff" }}>What Your Jobs Are Teaching You</div>
            <div style={{ fontSize: 11, color: "rgba(255,255,255,0.35)", marginTop: 2 }}>Real cost multipliers &amp; win rate from your own history</div>
          </div>
          <button onClick={onClose} style={{ background: "transparent", border: "none", color: "rgba(255,255,255,0.4)", fontSize: 22, cursor: "pointer" }}>✕</button>
        </div>

        {loading ? (
          <div style={{ textAlign: "center", color: "rgba(255,255,255,0.3)", fontSize: 12, padding: "40px 0" }}>Crunching your history…</div>
        ) : data.jobsAnalyzed < 2 ? (
          <div style={{ textAlign: "center", color: "rgba(255,255,255,0.25)", padding: "40px 0" }}>
            <div style={{ fontSize: 28, marginBottom: 8 }}>🧠</div>
            <div style={{ fontSize: 13 }}>Not enough job history yet</div>
            <div style={{ fontSize: 11, marginTop: 4, color: "rgba(255,255,255,0.18)" }}>Track a few jobs bid-vs-actual and Wireway will start spotting your patterns.</div>
          </div>
        ) : (
          <>
            {/* Headline */}
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(150px,1fr))", gap: 8, marginBottom: 18 }}>
              {[
                { label: "Cost multiplier", val: mult(data.overall.costMultiplier), c: overrunColor(data.overall.costMultiplier), sub: "actual ÷ estimated" },
                { label: "Material overrun", val: data.overall.materialMultiplier != null ? `${((data.overall.materialMultiplier - 1) * 100).toFixed(0)}%` : "—", c: overrunColor(data.overall.materialMultiplier), sub: "over your estimate" },
                { label: "Win rate", val: pct(data.winRate), c: BLUE, sub: `${data.quotesTotal} quotes` },
                { label: "Avg won price", val: money(data.overall.wonAvg), c: GREEN, sub: `${data.jobsAnalyzed} jobs` },
              ].map((c) => (
                <div key={c.label} style={{ background: "rgba(255,255,255,0.025)", border: "1px solid rgba(255,255,255,0.07)", borderRadius: 11, padding: "13px 14px" }}>
                  <div style={{ fontSize: 10, color: "rgba(255,255,255,0.35)", textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 6 }}>{c.label}</div>
                  <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 19, fontWeight: 600, color: c.c }}>{c.val}</div>
                  <div style={{ fontSize: 9, color: "rgba(255,255,255,0.28)", marginTop: 4 }}>{c.sub}</div>
                </div>
              ))}
            </div>

            {/* Per-type table */}
            <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 10 }}>By job type</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
              {data.categories.map((c) => (
                <div key={c.key} style={{ display: "flex", alignItems: "center", gap: 10, padding: "11px 13px", background: "rgba(255,255,255,0.022)", border: "1px solid rgba(255,255,255,0.06)", borderRadius: 10 }}>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 13, color: "#fff", fontWeight: 600 }}>{c.label}</div>
                    <div style={{ fontSize: 10, color: "rgba(255,255,255,0.35)", fontFamily: "'DM Mono',monospace" }}>{c.sampleSize} job{c.sampleSize !== 1 ? "s" : ""} · avg won {money(c.wonAvg)}</div>
                  </div>
                  <div style={{ textAlign: "right", flexShrink: 0 }}>
                    <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 13, fontWeight: 700, color: overrunColor(c.materialMultiplier) }}>
                      {c.materialMultiplier != null ? `${((c.materialMultiplier - 1) * 100).toFixed(0)}% mat` : (c.costMultiplier != null ? `${mult(c.costMultiplier)} cost` : "—")}
                    </div>
                    <div style={{ fontSize: 9, color: "rgba(255,255,255,0.3)" }}>margin {pct(c.marginPct)}</div>
                  </div>
                </div>
              ))}
            </div>

            <div style={{ textAlign: "center", marginTop: 16, fontSize: 10, color: "rgba(255,255,255,0.25)", lineHeight: 1.6 }}>
              When you start a new estimate, Wireway uses these patterns to flag overruns and suggest a price.
            </div>
          </>
        )}
      </div>
    </div>
  );
}
