/* eslint-disable react-hooks/exhaustive-deps */
// src/ROIView.jsx — full "Wireway made/saved you $X" breakdown  ·  Phase 2 · Feature 2
import { useState, useEffect } from "react";
import { getROI, ESTIMATED_TAX_RATE } from "./lib/roi";

const fmt = (n) => "$" + Math.round(Number(n) || 0).toLocaleString("en-US");
const GREEN = "#7dcea0", GOLD = "#e8c97a", BLUE = "#7eb8e8", PURPLE = "#b87ee8";

export default function ROIView({ user, onClose }) {
  const [roi, setRoi] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user?.id) return;
    getROI(user.id).then((d) => { setRoi(d); setLoading(false); });
  }, [user?.id]);

  const wrap = { position: "fixed", inset: 0, zIndex: 360, background: "rgba(0,0,0,0.82)", backdropFilter: "blur(8px)", overflowY: "auto", display: "flex", justifyContent: "center", alignItems: "flex-start", padding: "24px 16px" };
  const panel = { background: "#111115", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 18, width: "100%", maxWidth: 560, padding: "24px" };

  const ROWS = roi ? [
    { label: "Estimated tax saved", val: fmt(roi.taxSaved), c: GREEN, sub: `from ${fmt(roi.mileageDeduction + roi.expenseDeductions)} in deductions captured · ~${Math.round(ESTIMATED_TAX_RATE * 100)}% rate` },
    { label: "Extra profit caught", val: fmt(roi.extraProfit), c: GOLD, sub: `${roi.profitableJobs} job${roi.profitableJobs !== 1 ? "s" : ""} where actual beat the bid` },
    { label: "Mileage deduction logged", val: fmt(roi.mileageDeduction), c: BLUE, sub: `${Math.round(roi.milesLogged).toLocaleString()} business miles` },
  ] : [];

  return (
    <div style={wrap}>
      <div style={panel}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 18 }}>
          <div>
            <div style={{ fontFamily: "'Syne',sans-serif", fontSize: 17, fontWeight: 800, color: "#fff" }}>Your Wireway ROI</div>
            <div style={{ fontSize: 11, color: "rgba(255,255,255,0.35)", marginTop: 2 }}>Real dollars made &amp; saved from your own data</div>
          </div>
          <button onClick={onClose} style={{ background: "transparent", border: "none", color: "rgba(255,255,255,0.4)", fontSize: 22, cursor: "pointer" }}>✕</button>
        </div>

        {loading ? (
          <div style={{ textAlign: "center", color: "rgba(255,255,255,0.3)", fontSize: 12, padding: "40px 0" }}>Adding it up…</div>
        ) : (
          <>
            <div style={{ background: "linear-gradient(135deg,rgba(125,206,160,0.12),rgba(255,255,255,0.02))", border: "1px solid rgba(125,206,160,0.3)", borderRadius: 16, padding: "22px", textAlign: "center", marginBottom: 18 }}>
              <div style={{ fontSize: 11, color: "rgba(255,255,255,0.45)", textTransform: "uppercase", letterSpacing: "0.1em" }}>Wireway has made &amp; saved you</div>
              <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 40, fontWeight: 700, color: GREEN, marginTop: 6, letterSpacing: "-0.02em" }}>{fmt(roi.total)}</div>
              <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", marginTop: 4 }}>estimated · grows as you track more</div>
            </div>

            <div style={{ display: "flex", flexDirection: "column", gap: 8, marginBottom: 16 }}>
              {ROWS.map((r) => (
                <div key={r.label} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12, padding: "13px 15px", background: "rgba(255,255,255,0.022)", border: "1px solid rgba(255,255,255,0.06)", borderRadius: 11 }}>
                  <div style={{ minWidth: 0 }}>
                    <div style={{ fontSize: 13, color: "#fff", fontWeight: 600 }}>{r.label}</div>
                    <div style={{ fontSize: 10, color: "rgba(255,255,255,0.35)", marginTop: 2 }}>{r.sub}</div>
                  </div>
                  <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 17, fontWeight: 600, color: r.c, flexShrink: 0 }}>{r.val}</div>
                </div>
              ))}
            </div>

            {/* Collection speed */}
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8, marginBottom: 8 }}>
              <div style={{ background: "rgba(184,126,232,0.06)", border: "1px solid rgba(184,126,232,0.18)", borderRadius: 11, padding: "13px 15px" }}>
                <div style={{ fontSize: 10, color: "rgba(255,255,255,0.35)", textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 5 }}>Avg time to get paid</div>
                <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 18, fontWeight: 600, color: PURPLE }}>{roi.avgCollectionDays != null ? `${roi.avgCollectionDays.toFixed(1)} days` : "—"}</div>
              </div>
              <div style={{ background: "rgba(184,126,232,0.06)", border: "1px solid rgba(184,126,232,0.18)", borderRadius: 11, padding: "13px 15px" }}>
                <div style={{ fontSize: 10, color: "rgba(255,255,255,0.35)", textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 5 }}>Faster than typical</div>
                <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 18, fontWeight: 600, color: roi.daysSaved > 0 ? GREEN : "rgba(255,255,255,0.4)" }}>{roi.daysSaved > 0 ? `${roi.daysSaved.toFixed(0)} days` : "—"}</div>
              </div>
            </div>

            <div style={{ textAlign: "center", marginTop: 12, fontSize: 10, color: "rgba(255,255,255,0.25)", lineHeight: 1.6 }}>
              Tax savings are an estimate at ~{Math.round(ESTIMATED_TAX_RATE * 100)}% — confirm with your accountant. Everything here is built from data you already entered.
            </div>
          </>
        )}
      </div>
    </div>
  );
}
