/* eslint-disable react-hooks/exhaustive-deps */
// src/components/QuoteInsightBanner.jsx — smarter-next-bid nudge  ·  Phase 2 · Feature 3
// Shows a suggestion based on the user's own bid-vs-actual history while they work
// a quote. Renders nothing until there's enough history (>= 2 comparable jobs).
import { useState, useEffect } from "react";
import { getInsights, suggestForQuote } from "../lib/insights";

const money = (n) => "$" + Math.round(Number(n) || 0).toLocaleString("en-US");

export default function QuoteInsightBanner({ user, estCost, estMaterial, jobName }) {
  const [insights, setInsights] = useState(null);

  useEffect(() => {
    let alive = true;
    if (!user?.id) return;
    getInsights(user.id).then((d) => { if (alive) setInsights(d); }).catch(() => {});
    return () => { alive = false; };
  }, [user?.id]);

  const s = insights ? suggestForQuote(insights, { estCost, estMaterial, jobName }) : null;
  if (!s) return null;

  const overrun = s.materialOverrunPct != null && Math.abs(s.materialOverrunPct) >= 3;

  return (
    <div className="no-print" style={{ marginBottom: 12, background: "rgba(232,201,122,0.06)", border: "1px solid rgba(232,201,122,0.2)", borderRadius: 10, padding: "11px 14px" }}>
      <div style={{ display: "flex", alignItems: "center", gap: 7, marginBottom: 4 }}>
        <span style={{ fontSize: 13 }}>🧠</span>
        <span style={{ fontSize: 11, fontWeight: 800, color: "#e8c97a", textTransform: "uppercase", letterSpacing: "0.06em" }}>From your own jobs</span>
      </div>
      <div style={{ fontSize: 12, color: "rgba(255,255,255,0.7)", lineHeight: 1.6 }}>
        {overrun && (
          <>Similar jobs run <b style={{ color: s.materialOverrunPct > 0 ? "#e87e7e" : "#7dcea0" }}>{s.materialOverrunPct > 0 ? "+" : ""}{s.materialOverrunPct.toFixed(0)}%</b> on materials vs. your estimate{s.adjustedMaterial ? <> (~{money(s.adjustedMaterial)} actual)</> : null}. </>
        )}
        {s.suggestedPrice && (
          <>Suggested price for this job: <b style={{ color: "#e8c97a" }}>{money(s.suggestedPrice)}</b>{s.targetMarginPct != null ? <> to hold your ~{s.targetMarginPct.toFixed(0)}% margin</> : null}. </>
        )}
        <span style={{ color: "rgba(255,255,255,0.3)" }}>Based on {s.sampleSize} {s.basis} job{s.sampleSize !== 1 ? "s" : ""}.</span>
      </div>
    </div>
  );
}
