/* eslint-disable react-hooks/exhaustive-deps */
// src/MarketIntelView.jsx — Local Market Intelligence (opt-in)  ·  Phase 2 · Feature 6
import { useState, useEffect, useCallback } from "react";
import { US_STATES, setMarketOptIn, getBenchmark } from "./lib/market";

const IS = {
  background: "rgba(255,255,255,0.04)", border: "1px solid rgba(255,255,255,0.09)",
  borderRadius: 7, padding: "8px 11px", fontSize: 13, color: "#fff",
  fontFamily: "inherit", outline: "none", width: "100%",
};
const money = (n) => (n == null ? "—" : "$" + Math.round(n).toLocaleString("en-US"));
const GREEN = "#7dcea0", BLUE = "#7eb8e8", GOLD = "#e8c97a";

export default function MarketIntelView({ user, profile, onProfileUpdate, onClose }) {
  const [optIn,  setOptIn]  = useState(!!profile?.market_opt_in);
  const [region, setRegion] = useState(profile?.region || "");
  const [data,   setData]   = useState(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [msg, setMsg] = useState("");

  const flash = (m) => { setMsg(m); setTimeout(() => setMsg(""), 2500); };

  const loadBench = useCallback(async () => {
    if (!profile?.market_opt_in) return;
    setLoading(true);
    setData(await getBenchmark());
    setLoading(false);
  }, [profile?.market_opt_in]);

  useEffect(() => { loadBench(); }, [loadBench]);

  const save = async () => {
    if (optIn && !region) return flash("Pick your state to compare locally.");
    setSaving(true);
    const { data: updated, error } = await setMarketOptIn(user.id, { opt_in: optIn, region });
    setSaving(false);
    if (error) return flash("Could not save.");
    if (updated && onProfileUpdate) onProfileUpdate(updated);
    flash(optIn ? "Joined — comparing locally." : "Opted out.");
    if (optIn) { setData(null); setTimeout(loadBench, 50); } else setData(null);
  };

  const wrap = { position: "fixed", inset: 0, zIndex: 150, background: "rgba(0,0,0,0.82)", backdropFilter: "blur(8px)", overflowY: "auto", display: "flex", justifyContent: "center", alignItems: "flex-start", padding: "24px 16px" };
  const panel = { background: "#111115", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 18, width: "100%", maxWidth: 640, padding: "24px" };

  const buckets = data?.buckets || [];

  return (
    <div style={wrap}>
      <div style={panel}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 18 }}>
          <div>
            <div style={{ fontFamily: "'Syne',sans-serif", fontSize: 17, fontWeight: 800, color: "#fff" }}>Local Market</div>
            <div style={{ fontSize: 11, color: "rgba(255,255,255,0.35)", marginTop: 2 }}>How your pricing compares — anonymous &amp; aggregated</div>
          </div>
          <button onClick={onClose} style={{ background: "transparent", border: "none", color: "rgba(255,255,255,0.4)", fontSize: 22, cursor: "pointer" }}>✕</button>
        </div>

        {msg && <div style={{ marginBottom: 12, fontSize: 11, color: GREEN, background: "rgba(125,206,160,0.08)", border: "1px solid rgba(125,206,160,0.2)", borderRadius: 7, padding: "7px 11px" }}>{msg}</div>}

        {/* Opt-in control */}
        <div style={{ background: "rgba(126,184,232,0.05)", border: "1px solid rgba(126,184,232,0.18)", borderRadius: 12, padding: "14px", marginBottom: 16 }}>
          <label style={{ display: "flex", alignItems: "flex-start", gap: 10, cursor: "pointer" }}>
            <input type="checkbox" checked={optIn} onChange={(e) => setOptIn(e.target.checked)} style={{ marginTop: 2 }} />
            <div>
              <div style={{ fontSize: 13, fontWeight: 700, color: "#fff" }}>Share anonymized pricing to see local benchmarks</div>
              <div style={{ fontSize: 11, color: "rgba(255,255,255,0.4)", marginTop: 3, lineHeight: 1.6 }}>
                Only aggregate averages are ever shown, and only for job types where at least {data?.k || 5} contractors in your state contribute. Your individual jobs, clients, and identity are never exposed. Opt out anytime.
              </div>
            </div>
          </label>
          {optIn && (
            <div style={{ display: "flex", gap: 8, marginTop: 12, alignItems: "center" }}>
              <span style={{ fontSize: 11, color: "rgba(255,255,255,0.4)" }}>Your state</span>
              <select value={region} onChange={(e) => setRegion(e.target.value)} style={{ ...IS, width: "auto", colorScheme: "dark" }}>
                <option value="">Select…</option>
                {US_STATES.map((s) => <option key={s} value={s}>{s}</option>)}
              </select>
            </div>
          )}
          <button onClick={save} disabled={saving} style={{ marginTop: 12, padding: "9px 16px", borderRadius: 8, border: "1px solid rgba(126,184,232,0.4)", background: "rgba(126,184,232,0.12)", color: BLUE, fontSize: 12, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>
            {saving ? "Saving…" : optIn ? "Save & compare" : "Opt out"}
          </button>
        </div>

        {/* Benchmarks */}
        {!profile?.market_opt_in ? (
          <div style={{ textAlign: "center", color: "rgba(255,255,255,0.25)", padding: "30px 0" }}>
            <div style={{ fontSize: 28, marginBottom: 8 }}>🌎</div>
            <div style={{ fontSize: 13 }}>Opt in above to see local benchmarks</div>
          </div>
        ) : loading ? (
          <div style={{ textAlign: "center", color: "rgba(255,255,255,0.3)", fontSize: 12, padding: "30px 0" }}>Loading benchmarks…</div>
        ) : buckets.length === 0 ? (
          <div style={{ textAlign: "center", color: "rgba(255,255,255,0.25)", padding: "30px 0" }}>
            <div style={{ fontSize: 13 }}>Not enough local data yet</div>
            <div style={{ fontSize: 11, marginTop: 4, color: "rgba(255,255,255,0.18)" }}>
              Benchmarks appear once ≥ {data?.k || 5} contractors{data?.region ? ` in ${data.region}` : ""} contribute to a job type. Check back as more join.
            </div>
          </div>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
            {buckets.map((b) => {
              const under = b.deltaPct != null && b.deltaPct < 0;
              return (
                <div key={b.type} style={{ display: "flex", alignItems: "center", gap: 10, padding: "12px 14px", background: "rgba(255,255,255,0.022)", border: "1px solid rgba(255,255,255,0.06)", borderRadius: 10 }}>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 13, color: "#fff", fontWeight: 600 }}>{b.label}</div>
                    <div style={{ fontSize: 10, color: "rgba(255,255,255,0.35)", fontFamily: "'DM Mono',monospace" }}>
                      market avg {money(b.marketAvg)} · {b.contributors} contractors{b.yourAvg != null ? ` · you ${money(b.yourAvg)}` : ""}
                    </div>
                  </div>
                  {b.deltaPct != null ? (
                    <div style={{ textAlign: "right", flexShrink: 0 }}>
                      <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 14, fontWeight: 700, color: under ? GOLD : GREEN }}>{b.deltaPct > 0 ? "+" : ""}{b.deltaPct}%</div>
                      <div style={{ fontSize: 9, color: "rgba(255,255,255,0.3)" }}>{under ? "under market" : "vs market"}</div>
                    </div>
                  ) : (
                    <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", flexShrink: 0 }}>bid one to compare</div>
                  )}
                </div>
              );
            })}
          </div>
        )}

        <div style={{ textAlign: "center", marginTop: 16, fontSize: 10, color: "rgba(255,255,255,0.2)", lineHeight: 1.6 }}>
          Aggregated server-side · k-anonymity ≥ {data?.k || 5} · no individual data is ever shown.
        </div>
      </div>
    </div>
  );
}
