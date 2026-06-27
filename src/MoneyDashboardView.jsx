/* eslint-disable react-hooks/exhaustive-deps */
// src/MoneyDashboardView.jsx — Contractor money dashboard + accountant CSV  ·  Feature 7
import { useState, useEffect, useCallback } from "react";
import { getMoneySnapshot, buildAccountantCsv } from "./lib/dashboard";

const fmt = (n) => (n < 0 ? "-$" : "$") + Math.abs(Math.round(Number(n) || 0)).toLocaleString("en-US");
const BLUE = "#7eb8e8", GREEN = "#7dcea0", GOLD = "#e8c97a", RED = "#e87e7e", PURPLE = "#b87ee8";
const MONTHS = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
const profitColor = (n) => (n >= 0 ? GREEN : RED);

export default function MoneyDashboardView({ user, onClose }) {
  const now = new Date();
  const [month, setMonth] = useState(now.getMonth());
  const [year,  setYear]  = useState(now.getFullYear());
  const [snap,  setSnap]  = useState(null);
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState(false);
  const [msg, setMsg] = useState("");

  const flash = (m) => { setMsg(m); setTimeout(() => setMsg(""), 2500); };

  const load = useCallback(async () => {
    if (!user?.id) return;
    setLoading(true);
    setSnap(await getMoneySnapshot(user.id, { month, year }));
    setLoading(false);
  }, [user?.id, month, year]);

  useEffect(() => { load(); }, [load]);

  const exportCsv = async () => {
    setExporting(true);
    const { csv, count } = await buildAccountantCsv(user.id, year);
    setExporting(false);
    if (!count) return flash("No transactions to export for " + year + ".");
    const blob = new Blob([csv], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url; a.download = `wireway-books-${year}.csv`; a.click();
    URL.revokeObjectURL(url);
    flash(`Exported ${count} transactions for your accountant.`);
  };

  const prevMonth = () => { if (month === 0) { setMonth(11); setYear((y) => y - 1); } else setMonth((m) => m - 1); };
  const nextMonth = () => { if (month === 11) { setMonth(0); setYear((y) => y + 1); } else setMonth((m) => m + 1); };

  const METRICS = snap ? [
    { label: "Bid", val: snap.bid, c: BLUE, sub: "estimates written" },
    { label: "Won", val: snap.won, c: PURPLE, sub: "accepted" },
    { label: "Collected", val: snap.collected, c: GREEN, sub: "cash in" },
    { label: "Owed to you", val: snap.owed, c: GOLD, sub: snap.overdue > 0 ? `${fmt(snap.overdue)} overdue` : "A/R balance" },
    { label: "Spent", val: snap.spent, c: "#e8b87e", sub: "cash out" },
    { label: "Real profit", val: snap.realProfit, c: profitColor(snap.realProfit), sub: "collected − spent" },
  ] : [];

  const wrap = { position: "fixed", inset: 0, zIndex: 150, background: "rgba(0,0,0,0.82)", backdropFilter: "blur(8px)", overflowY: "auto", display: "flex", justifyContent: "center", alignItems: "flex-start", padding: "24px 16px" };
  const panel = { background: "#111115", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 18, width: "100%", maxWidth: 820, padding: "24px" };

  const JobRow = ({ j }) => (
    <div style={{ display: "flex", alignItems: "center", gap: 10, padding: "9px 12px", background: "rgba(255,255,255,0.022)", border: "1px solid rgba(255,255,255,0.055)", borderRadius: 9 }}>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 12, color: "#fff", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{j.title}</div>
        <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", fontFamily: "'DM Mono',monospace" }}>{j.client_name || "—"} · bid {fmt(j.bid)} · spent {fmt(j.spend)}{!j.settled ? " · projected" : ""}</div>
      </div>
      <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 14, fontWeight: 700, color: profitColor(j.margin), flexShrink: 0 }}>{fmt(j.margin)}</div>
    </div>
  );

  return (
    <div style={wrap}>
      <div style={panel}>
        {/* Header */}
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 18 }}>
          <div>
            <div style={{ fontFamily: "'Syne',sans-serif", fontSize: 17, fontWeight: 800, color: "#fff" }}>Money</div>
            <div style={{ fontSize: 11, color: "rgba(255,255,255,0.35)", marginTop: 2 }}>What you bid, won, collected — and what's actually profit</div>
          </div>
          <button onClick={onClose} style={{ background: "transparent", border: "none", color: "rgba(255,255,255,0.4)", fontSize: 22, cursor: "pointer" }}>✕</button>
        </div>

        {msg && <div style={{ marginBottom: 12, fontSize: 11, color: GREEN, background: "rgba(125,206,160,0.08)", border: "1px solid rgba(125,206,160,0.2)", borderRadius: 7, padding: "7px 11px" }}>{msg}</div>}

        {/* Month nav */}
        <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 16 }}>
          <button onClick={prevMonth} style={{ padding: "4px 11px", borderRadius: 6, border: "1px solid rgba(255,255,255,0.12)", background: "transparent", color: "rgba(255,255,255,0.5)", fontSize: 15, cursor: "pointer" }}>‹</button>
          <div style={{ fontFamily: "'Syne',sans-serif", fontSize: 15, fontWeight: 700, color: "#fff", minWidth: 150, textAlign: "center" }}>{MONTHS[month]} {year}</div>
          <button onClick={nextMonth} style={{ padding: "4px 11px", borderRadius: 6, border: "1px solid rgba(255,255,255,0.12)", background: "transparent", color: "rgba(255,255,255,0.5)", fontSize: 15, cursor: "pointer" }}>›</button>
          <button onClick={() => { setMonth(now.getMonth()); setYear(now.getFullYear()); }} style={{ padding: "4px 10px", borderRadius: 5, border: "1px solid rgba(255,255,255,0.1)", background: "transparent", color: "rgba(255,255,255,0.35)", fontSize: 10, fontWeight: 600, cursor: "pointer", fontFamily: "inherit" }}>This month</button>
          <div style={{ flex: 1 }} />
          <button onClick={exportCsv} disabled={exporting} style={{ padding: "7px 13px", borderRadius: 8, border: "1px solid rgba(40,180,100,0.3)", background: "rgba(40,180,100,0.08)", color: "#4ade80", fontSize: 11, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>
            {exporting ? "Exporting…" : `⬇ Accountant CSV (${year})`}
          </button>
        </div>

        {/* Headline metrics */}
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(120px,1fr))", gap: 8, marginBottom: 18 }}>
          {loading || !snap ? (
            <div style={{ gridColumn: "1 / -1", textAlign: "center", color: "rgba(255,255,255,0.3)", fontSize: 12, padding: "30px 0" }}>Loading…</div>
          ) : METRICS.map((m) => (
            <div key={m.label} style={{ background: "rgba(255,255,255,0.025)", border: "1px solid rgba(255,255,255,0.07)", borderRadius: 11, padding: "13px 14px" }}>
              <div style={{ fontSize: 10, color: "rgba(255,255,255,0.35)", textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 7 }}>{m.label}</div>
              <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 21, fontWeight: 600, color: m.c, lineHeight: 1 }}>{fmt(m.val)}</div>
              <div style={{ fontSize: 9, color: "rgba(255,255,255,0.28)", marginTop: 5, fontFamily: "'DM Mono',monospace" }}>{m.sub}</div>
            </div>
          ))}
        </div>

        {snap && !loading && (
          <>
            {/* Spend breakdown */}
            {snap.spent > 0 && (
              <div style={{ marginBottom: 18 }}>
                <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 8 }}>Where it went — {MONTHS[month]}</div>
                <div style={{ display: "flex", height: 10, borderRadius: 5, overflow: "hidden", marginBottom: 8 }}>
                  {[
                    { k: "materials", c: "#e8b87e" }, { k: "subs", c: "#7ee8b8" },
                    { k: "labor", c: PURPLE }, { k: "mileage", c: "#a8e87e" },
                  ].map((x) => {
                    const w = snap.spent > 0 ? (snap.breakdown[x.k] / snap.spent) * 100 : 0;
                    return w > 0 ? <div key={x.k} title={`${x.k}: ${fmt(snap.breakdown[x.k])}`} style={{ width: `${w}%`, background: x.c, opacity: 0.7 }} /> : null;
                  })}
                </div>
                <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                  {[["Materials/bank", snap.breakdown.materials, "#e8b87e"], ["Subs", snap.breakdown.subs, "#7ee8b8"], ["Labor", snap.breakdown.labor, PURPLE], ["Mileage", snap.breakdown.mileage, "#a8e87e"]]
                    .filter(([, v]) => v > 0)
                    .map(([label, v, c]) => (
                      <div key={label} style={{ fontSize: 10, color: c, background: `${c}14`, border: `1px solid ${c}30`, borderRadius: 5, padding: "2px 8px", fontFamily: "'DM Mono',monospace" }}>{label}: {fmt(v)}</div>
                    ))}
                </div>
              </div>
            )}

            {/* Jobs that made / lost money */}
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 14 }}>
              <div>
                <div style={{ fontSize: 10, color: GREEN, textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 8 }}>Made money</div>
                <div style={{ display: "flex", flexDirection: "column", gap: 5 }}>
                  {snap.winners.slice(0, 6).map((j) => <JobRow key={j.id} j={j} />)}
                  {snap.winners.length === 0 && <div style={{ fontSize: 11, color: "rgba(255,255,255,0.25)" }}>No profitable jobs yet.</div>}
                </div>
              </div>
              <div>
                <div style={{ fontSize: 10, color: RED, textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 8 }}>Lost money</div>
                <div style={{ display: "flex", flexDirection: "column", gap: 5 }}>
                  {snap.losers.slice(0, 6).map((j) => <JobRow key={j.id} j={j} />)}
                  {snap.losers.length === 0 && <div style={{ fontSize: 11, color: "rgba(255,255,255,0.25)" }}>None in the red — nice. 👍</div>}
                </div>
              </div>
            </div>

            <div style={{ textAlign: "center", marginTop: 18, fontSize: 10, color: "rgba(255,255,255,0.2)", lineHeight: 1.6 }}>
              The accountant CSV is a clean transaction export for QuickBooks/Xero — Wireway tracks the money, your accountant files the return.
            </div>
          </>
        )}
      </div>
    </div>
  );
}
