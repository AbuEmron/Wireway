/* eslint-disable react-hooks/exhaustive-deps */
// src/ProgressBillingView.jsx — Progress billing + retainage  ·  Feature 5
import { useState, useEffect, useCallback } from "react";
import { getJobsCosting } from "./lib/jobCosting";
import {
  getDraws, upsertDraw, deleteDraw, setDrawStatus, syncJobCollected,
  generateSchedule, scheduleTotals, drawNet, drawRetainage, buildDrawInvoiceText,
} from "./lib/billing";

const IS = {
  background: "rgba(255,255,255,0.04)", border: "1px solid rgba(255,255,255,0.09)",
  borderRadius: 7, padding: "8px 11px", fontSize: 13, color: "#fff",
  fontFamily: "inherit", outline: "none", width: "100%",
};
const focusGold = (e) => (e.target.style.borderColor = "rgba(232,201,122,0.4)");
const blurGray  = (e) => (e.target.style.borderColor = "rgba(255,255,255,0.09)");
const fmt = (n) => "$" + Math.round(Number(n) || 0).toLocaleString("en-US");
const fmt2 = (n) => "$" + (Number(n) || 0).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const BLUE = "#7eb8e8", GREEN = "#7dcea0", GOLD = "#e8c97a";
const STATUS = {
  pending:  { c: "rgba(255,255,255,0.4)", label: "Pending" },
  invoiced: { c: GOLD, label: "Invoiced" },
  paid:     { c: GREEN, label: "Paid" },
};

export default function ProgressBillingView({ user, company = {}, onClose }) {
  const [jobs,     setJobs]     = useState([]);
  const [jobId,    setJobId]    = useState("");
  const [draws,    setDraws]    = useState([]);
  const [loading,  setLoading]  = useState(true);
  const [msg,      setMsg]      = useState("");
  const [gen,      setGen]      = useState({ depositPct: 30, progressDraws: 2, retainagePct: 10 });
  const [showGen,  setShowGen]  = useState(false);
  const [newDraw,  setNewDraw]  = useState({ label: "", amount: "", retainage_pct: 10, due_date: "" });
  const [copied,   setCopied]   = useState(null);

  const flash = (m) => { setMsg(m); setTimeout(() => setMsg(""), 2500); };
  const job = jobs.find((j) => j.id === jobId) || null;

  const loadJobs = useCallback(async () => {
    if (!user?.id) return;
    setLoading(true);
    const { data } = await getJobsCosting(user.id);
    const billable = (data || []).filter((j) => Number(j.bid_amount) > 0);
    setJobs(billable);
    if (!jobId && billable[0]) setJobId(billable[0].id);
    setLoading(false);
  }, [user?.id]);

  useEffect(() => { loadJobs(); }, [loadJobs]);

  const loadDraws = useCallback(async () => {
    if (!jobId) { setDraws([]); return; }
    setDraws(await getDraws(user.id, jobId));
  }, [jobId, user?.id]);

  useEffect(() => { loadDraws(); }, [loadDraws]);

  // After any change that affects paid draws, push collected back to the job.
  const refreshAndSync = async () => {
    const next = await getDraws(user.id, jobId);
    setDraws(next);
    await syncJobCollected(user.id, jobId, next);
  };

  const onGenerate = async () => {
    if (draws.length && !window.confirm("This job already has a schedule. Add a fresh template anyway?")) return;
    const { error } = await generateSchedule(user.id, jobId, { bid: job?.bid_amount, ...gen });
    if (error) return flash(error.message || "Could not generate.");
    setShowGen(false);
    await refreshAndSync();
    flash("Schedule created.");
  };

  const onAddDraw = async () => {
    if (!newDraw.label.trim()) return flash("Enter a label.");
    const amt = parseFloat(newDraw.amount);
    if (!amt || amt <= 0) return flash("Enter an amount.");
    const { error } = await upsertDraw(user.id, { ...newDraw, amount: amt, job_id: jobId, sort_order: draws.length });
    if (error) return flash("Could not add.");
    setNewDraw({ label: "", amount: "", retainage_pct: newDraw.retainage_pct, due_date: "" });
    await refreshAndSync();
  };

  const cycle = async (d) => {
    const next = d.status === "pending" ? "invoiced" : d.status === "invoiced" ? "paid" : "pending";
    const { error } = await setDrawStatus(user.id, d, next);
    if (error) return flash("Could not update.");
    await refreshAndSync();
  };

  const onDelete = async (id) => { await deleteDraw(id, user.id); await refreshAndSync(); };

  const copyInvoice = (d) => {
    navigator.clipboard.writeText(buildDrawInvoiceText({ draw: d, job, company }));
    setCopied(d.id); setTimeout(() => setCopied(null), 2000);
  };

  const t = scheduleTotals(draws);
  const wrap = { position: "fixed", inset: 0, zIndex: 150, background: "rgba(0,0,0,0.82)", backdropFilter: "blur(8px)", overflowY: "auto", display: "flex", justifyContent: "center", alignItems: "flex-start", padding: "24px 16px" };
  const panel = { background: "#111115", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 18, width: "100%", maxWidth: 720, padding: "24px" };

  return (
    <div style={wrap}>
      <div style={panel}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 18 }}>
          <div>
            <div style={{ fontFamily: "'Syne',sans-serif", fontSize: 17, fontWeight: 800, color: "#fff" }}>Progress Billing</div>
            <div style={{ fontSize: 11, color: "rgba(255,255,255,0.35)", marginTop: 2 }}>Deposit → draws → final, with retainage held back</div>
          </div>
          <button onClick={onClose} style={{ background: "transparent", border: "none", color: "rgba(255,255,255,0.4)", fontSize: 22, cursor: "pointer" }}>✕</button>
        </div>

        {msg && <div style={{ marginBottom: 12, fontSize: 11, color: GREEN, background: "rgba(125,206,160,0.08)", border: "1px solid rgba(125,206,160,0.2)", borderRadius: 7, padding: "7px 11px" }}>{msg}</div>}

        {loading ? (
          <div style={{ textAlign: "center", color: "rgba(255,255,255,0.3)", fontSize: 12, padding: "40px 0" }}>Loading…</div>
        ) : jobs.length === 0 ? (
          <div style={{ textAlign: "center", color: "rgba(255,255,255,0.25)", padding: "40px 0" }}>
            <div style={{ fontSize: 28, marginBottom: 8 }}>🧾</div>
            <div style={{ fontSize: 13 }}>No jobs with a bid yet</div>
            <div style={{ fontSize: 11, marginTop: 4, color: "rgba(255,255,255,0.18)" }}>Create a job from a quote in Job Costing first.</div>
          </div>
        ) : (
          <>
            {/* Job picker */}
            <select value={jobId} onChange={(e) => setJobId(e.target.value)} style={{ ...IS, colorScheme: "dark", marginBottom: 14 }}>
              {jobs.map((j) => <option key={j.id} value={j.id}>{j.title} — {fmt(j.bid_amount)}{j.client_name ? ` · ${j.client_name}` : ""}</option>)}
            </select>

            {/* Totals */}
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(110px,1fr))", gap: 8, marginBottom: 14 }}>
              {[
                { label: "Contract", val: fmt(job?.bid_amount), c: BLUE },
                { label: "Billed", val: fmt(t.billedToDate), c: GOLD },
                { label: "Collected", val: fmt(t.collected), c: GREEN },
                { label: "Retainage held", val: fmt(t.retainageHeld), c: "#e8b87e" },
              ].map((c) => (
                <div key={c.label} style={{ background: "rgba(255,255,255,0.025)", border: "1px solid rgba(255,255,255,0.07)", borderRadius: 10, padding: "10px 12px" }}>
                  <div style={{ fontSize: 9, color: "rgba(255,255,255,0.3)", textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 4 }}>{c.label}</div>
                  <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 16, fontWeight: 600, color: c.c }}>{c.val}</div>
                </div>
              ))}
            </div>

            {/* Generate template */}
            <button onClick={() => setShowGen((v) => !v)} style={{ width: "100%", padding: "9px", borderRadius: 8, background: "rgba(126,184,232,0.07)", border: "1px solid rgba(126,184,232,0.25)", color: BLUE, fontSize: 12, fontWeight: 700, cursor: "pointer", fontFamily: "inherit", marginBottom: showGen ? 8 : 14 }}>
              {showGen ? "Hide template" : draws.length ? "Generate a fresh schedule" : "Generate schedule from contract"}
            </button>
            {showGen && (
              <div style={{ background: "rgba(126,184,232,0.04)", border: "1px solid rgba(126,184,232,0.14)", borderRadius: 10, padding: "12px", marginBottom: 14 }}>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 8, marginBottom: 10 }}>
                  {[
                    { k: "depositPct", label: "Deposit %" },
                    { k: "progressDraws", label: "Progress draws" },
                    { k: "retainagePct", label: "Retainage %" },
                  ].map((f) => (
                    <div key={f.k}>
                      <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", marginBottom: 4 }}>{f.label}</div>
                      <input type="number" min="0" value={gen[f.k]} onChange={(e) => setGen((g) => ({ ...g, [f.k]: Number(e.target.value) }))} style={{ ...IS, fontFamily: "'DM Mono',monospace" }} onFocus={focusGold} onBlur={blurGray} />
                    </div>
                  ))}
                </div>
                <button onClick={onGenerate} style={{ width: "100%", padding: "9px", borderRadius: 8, background: "rgba(126,184,232,0.12)", border: "1px solid rgba(126,184,232,0.35)", color: BLUE, fontSize: 12, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>Create {gen.progressDraws + 2}-line schedule</button>
              </div>
            )}

            {/* Draws */}
            <div style={{ display: "flex", flexDirection: "column", gap: 6, marginBottom: 14 }}>
              {draws.map((d) => {
                const st = STATUS[d.status] || STATUS.pending;
                const ret = drawRetainage(d);
                return (
                  <div key={d.id} style={{ display: "flex", alignItems: "center", gap: 10, padding: "11px 13px", background: "rgba(255,255,255,0.022)", border: "1px solid rgba(255,255,255,0.06)", borderRadius: 10 }}>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontSize: 13, color: "#fff", fontWeight: 600 }}>{d.label}</div>
                      <div style={{ fontSize: 10, color: "rgba(255,255,255,0.35)", fontFamily: "'DM Mono',monospace" }}>
                        gross {fmt2(d.amount)}{ret > 0 ? ` · −${fmt2(ret)} retainage (${d.retainage_pct}%)` : ""}{d.due_date ? ` · due ${d.due_date}` : ""}
                      </div>
                    </div>
                    <div style={{ textAlign: "right", flexShrink: 0 }}>
                      <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 14, fontWeight: 700, color: "#fff" }}>{fmt2(drawNet(d))}</div>
                      <div style={{ fontSize: 9, color: "rgba(255,255,255,0.3)" }}>net</div>
                    </div>
                    <button onClick={() => cycle(d)} title="Tap to advance status" style={{ flexShrink: 0, padding: "4px 9px", borderRadius: 6, border: `1px solid ${st.c}40`, background: `${st.c}14`, color: st.c, fontSize: 10, fontWeight: 700, cursor: "pointer", fontFamily: "inherit", minWidth: 64 }}>{st.label}</button>
                    <button onClick={() => copyInvoice(d)} title="Copy draw invoice" style={{ flexShrink: 0, width: 26, height: 26, borderRadius: 6, border: "1px solid rgba(255,255,255,0.12)", background: "transparent", color: copied === d.id ? GREEN : "rgba(255,255,255,0.4)", fontSize: 11, cursor: "pointer" }}>{copied === d.id ? "✓" : "⧉"}</button>
                    <button onClick={() => onDelete(d.id)} style={{ flexShrink: 0, width: 26, height: 26, borderRadius: 6, border: "1px solid rgba(232,126,126,0.2)", background: "rgba(232,126,126,0.06)", color: "rgba(232,126,126,0.5)", fontSize: 11, cursor: "pointer" }}>✕</button>
                  </div>
                );
              })}
              {draws.length === 0 && <div style={{ textAlign: "center", fontSize: 12, color: "rgba(255,255,255,0.25)", padding: "16px 0" }}>No draws yet — generate a schedule or add one below.</div>}
            </div>

            {/* Add draw */}
            <div style={{ background: "rgba(255,255,255,0.02)", border: "1px solid rgba(255,255,255,0.06)", borderRadius: 10, padding: "12px" }}>
              <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 8 }}>Add a draw</div>
              <div style={{ display: "grid", gridTemplateColumns: "1fr 110px 90px", gap: 7, marginBottom: 7 }}>
                <input placeholder="Label (e.g. Rough-in)" value={newDraw.label} onChange={(e) => setNewDraw((p) => ({ ...p, label: e.target.value }))} style={IS} onFocus={focusGold} onBlur={blurGray} />
                <input type="number" min="0" placeholder="Amount" value={newDraw.amount} onChange={(e) => setNewDraw((p) => ({ ...p, amount: e.target.value }))} style={{ ...IS, fontFamily: "'DM Mono',monospace" }} onFocus={focusGold} onBlur={blurGray} />
                <input type="number" min="0" max="100" placeholder="Ret %" value={newDraw.retainage_pct} onChange={(e) => setNewDraw((p) => ({ ...p, retainage_pct: e.target.value }))} style={{ ...IS, fontFamily: "'DM Mono',monospace" }} onFocus={focusGold} onBlur={blurGray} />
              </div>
              <button onClick={onAddDraw} style={{ width: "100%", padding: "9px", borderRadius: 8, background: "rgba(232,201,122,0.08)", border: "1px solid rgba(232,201,122,0.3)", color: GOLD, fontSize: 12, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>+ Add draw</button>
            </div>

            <div style={{ textAlign: "center", marginTop: 14, fontSize: 10, color: "rgba(255,255,255,0.2)", lineHeight: 1.6 }}>
              Tap a status to advance Pending → Invoiced → Paid. Paid draws update the job's collected total automatically.
            </div>
          </>
        )}
      </div>
    </div>
  );
}
