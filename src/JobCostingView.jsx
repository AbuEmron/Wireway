/* eslint-disable react-hooks/exhaustive-deps */
// src/JobCostingView.jsx — Job Costing: Bid vs Actual  ·  Feature 1
// Turns the estimate into a profit tracker: bid + estimated margin vs. live
// actual spend (bank feed + mileage + expenses tagged to the job) + actual margin.
import { useState, useEffect, useCallback } from "react";
import { getQuotes } from "./lib/supabase";
import {
  getJobsCosting, getJobCosts, getUnassignedCosts,
  normalizeCost, setCostJob, suggestJobForCost,
  updateJobCosting, createJobFromQuote, linkJobToQuote,
} from "./lib/jobCosting";

const fmt = (n) =>
  "$" + Math.round(Number(n) || 0).toLocaleString("en-US");
const fmt2 = (n) =>
  "$" + (Number(n) || 0).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

const GREEN = "#7dcea0", RED = "#e87e7e", BLUE = "#7eb8e8";
const profitColor = (n) => (n >= 0 ? GREEN : RED);

const IS = {
  background: "rgba(255,255,255,0.04)", border: "1px solid rgba(255,255,255,0.09)",
  borderRadius: 7, padding: "8px 11px", fontSize: 13, color: "#fff",
  fontFamily: "inherit", outline: "none", width: "100%",
};
const focusGold = (e) => (e.target.style.borderColor = "rgba(232,201,122,0.4)");
const blurGray  = (e) => (e.target.style.borderColor = "rgba(255,255,255,0.09)");

const STATUS_COLOR = { scheduled: BLUE, in_progress: "#e8c97a", complete: GREEN, cancelled: RED };

// ── A spend-vs-bid bar ──────────────────────────────────────────────────────────
function SpendBar({ spend, bid, estCost }) {
  const cap = Math.max(bid, spend, estCost, 1);
  const pct = (n) => `${Math.min(100, (n / cap) * 100)}%`;
  const over = spend > bid && bid > 0;
  return (
    <div style={{ marginTop: 10 }}>
      <div style={{ position: "relative", height: 7, background: "rgba(255,255,255,0.06)", borderRadius: 4, overflow: "hidden" }}>
        <div style={{ position: "absolute", inset: 0, width: pct(spend), background: over ? RED : GREEN, opacity: 0.55, transition: "width 0.3s" }} />
        {bid > 0 && estCost > 0 && (
          <div title="Estimated cost" style={{ position: "absolute", top: -2, bottom: -2, left: pct(estCost), width: 2, background: "rgba(255,255,255,0.5)" }} />
        )}
      </div>
      <div style={{ display: "flex", justifyContent: "space-between", marginTop: 4, fontSize: 9, color: "rgba(255,255,255,0.3)", fontFamily: "'DM Mono',monospace" }}>
        <span>Spent {fmt(spend)}</span>
        <span>Bid {fmt(bid)}</span>
      </div>
    </div>
  );
}

// ── Job card (list) ───────────────────────────────────────────────────────────────
function JobCard({ job, onOpen }) {
  const linked = job.quote_id || job.bid_amount > 0;
  const margin = job.collected > 0 ? job.actual_margin : job.projected_margin;
  const marginLabel = job.collected > 0 ? "Profit so far" : "Projected profit";
  return (
    <div onClick={() => onOpen(job)}
      style={{ background: "rgba(255,255,255,0.022)", border: "1px solid rgba(255,255,255,0.07)", borderRadius: 12, padding: "14px 16px", marginBottom: 8, cursor: "pointer", transition: "background 0.15s" }}
      onMouseEnter={(e) => (e.currentTarget.style.background = "rgba(255,255,255,0.04)")}
      onMouseLeave={(e) => (e.currentTarget.style.background = "rgba(255,255,255,0.022)")}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: 10 }}>
        <div style={{ minWidth: 0, flex: 1 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 7, marginBottom: 2 }}>
            <span style={{ width: 6, height: 6, borderRadius: "50%", background: STATUS_COLOR[job.status] || BLUE, flexShrink: 0 }} />
            <span style={{ fontSize: 13, fontWeight: 700, color: "#fff", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{job.title}</span>
          </div>
          <div style={{ fontSize: 10, color: "rgba(255,255,255,0.35)", fontFamily: "'DM Mono',monospace" }}>
            {[job.client_name, job.scheduled_date].filter(Boolean).join(" · ") || "No date"}
            {job.cost_count > 0 && <span> · {job.cost_count} cost{job.cost_count !== 1 ? "s" : ""}</span>}
          </div>
        </div>
        <div style={{ textAlign: "right", flexShrink: 0 }}>
          <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 16, fontWeight: 600, color: profitColor(margin) }}>{fmt(margin)}</div>
          <div style={{ fontSize: 9, color: "rgba(255,255,255,0.3)" }}>{marginLabel}</div>
        </div>
      </div>
      {linked ? (
        <SpendBar spend={job.actual_spend} bid={job.bid_amount} estCost={job.est_cost} />
      ) : (
        <div style={{ marginTop: 8, fontSize: 10, color: "rgba(232,201,122,0.7)" }}>
          Spent {fmt(job.actual_spend)} · no bid linked — open to link a quote
        </div>
      )}
    </div>
  );
}

// ── Stat tile ──────────────────────────────────────────────────────────────────
function Stat({ label, value, color = "#fff", sub }) {
  return (
    <div style={{ background: "rgba(255,255,255,0.025)", border: "1px solid rgba(255,255,255,0.07)", borderRadius: 10, padding: "11px 13px" }}>
      <div style={{ fontSize: 9, color: "rgba(255,255,255,0.3)", textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 5 }}>{label}</div>
      <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 17, fontWeight: 600, color, lineHeight: 1 }}>{value}</div>
      {sub && <div style={{ fontSize: 9, color: "rgba(255,255,255,0.28)", marginTop: 4, fontFamily: "'DM Mono',monospace" }}>{sub}</div>}
    </div>
  );
}

// ── A small assignable / assigned cost row ───────────────────────────────────────
function CostRow({ cost, suggested, onAssign, onRemove }) {
  const KIND_COLOR = { plaid: BLUE, expense: "#e8b87e", trip: "#a8e87e" };
  const KIND_LABEL = { plaid: "Bank", expense: "Expense", trip: "Mileage" };
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 9, padding: "8px 10px", background: "rgba(255,255,255,0.02)", border: "1px solid rgba(255,255,255,0.05)", borderRadius: 8 }}>
      <span style={{ fontSize: 8, fontWeight: 800, color: KIND_COLOR[cost.kind], background: `${KIND_COLOR[cost.kind]}18`, border: `1px solid ${KIND_COLOR[cost.kind]}30`, borderRadius: 4, padding: "2px 5px", flexShrink: 0, textTransform: "uppercase", letterSpacing: "0.04em" }}>
        {KIND_LABEL[cost.kind]}
      </span>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 12, color: "#fff", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{cost.label}</div>
        <div style={{ fontSize: 9, color: "rgba(255,255,255,0.3)", fontFamily: "'DM Mono',monospace" }}>{cost.date}{cost.sub ? ` · ${cost.sub}` : ""}</div>
      </div>
      <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 12, fontWeight: 600, color: "#fff", flexShrink: 0 }}>{fmt2(cost.amount)}</div>
      {onAssign && (
        <button onClick={() => onAssign(cost)}
          style={{ flexShrink: 0, padding: "4px 9px", borderRadius: 6, border: `1px solid ${suggested ? "rgba(125,206,160,0.5)" : "rgba(126,184,232,0.3)"}`, background: suggested ? "rgba(125,206,160,0.1)" : "rgba(126,184,232,0.07)", color: suggested ? GREEN : BLUE, fontSize: 10, fontWeight: 700, cursor: "pointer", fontFamily: "inherit", whiteSpace: "nowrap" }}>
          {suggested ? "✓ Assign" : "Assign"}
        </button>
      )}
      {onRemove && (
        <button onClick={() => onRemove(cost)} title="Unassign from job"
          style={{ flexShrink: 0, width: 24, height: 24, borderRadius: 6, border: "1px solid rgba(232,126,126,0.25)", background: "rgba(232,126,126,0.06)", color: "rgba(232,126,126,0.6)", fontSize: 11, cursor: "pointer" }}>✕</button>
      )}
    </div>
  );
}

// ── Main ─────────────────────────────────────────────────────────────────────────
export default function JobCostingView({ user, onClose }) {
  const [jobs,       setJobs]       = useState([]);
  const [quotes,     setQuotes]     = useState([]);
  const [unassigned, setUnassigned] = useState({ expenses: [], trips: [], plaidTxns: [] });
  const [loading,    setLoading]    = useState(true);
  const [selectedId, setSelectedId] = useState(null);
  const [detail,     setDetail]     = useState(null); // { expenses, trips, plaidTxns }
  const [estDraft,   setEstDraft]   = useState("");
  const [collDraft,  setCollDraft]  = useState("");
  const [busy,       setBusy]       = useState(false);
  const [msg,        setMsg]        = useState("");
  const [showNew,    setShowNew]    = useState(false);

  const flash = (m) => { setMsg(m); setTimeout(() => setMsg(""), 2500); };

  const load = useCallback(async () => {
    if (!user?.id) return;
    setLoading(true);
    const [{ data: jobsData }, { data: quotesData }, un] = await Promise.all([
      getJobsCosting(user.id),
      getQuotes(user.id),
      getUnassignedCosts(user.id),
    ]);
    setJobs(jobsData || []);
    setQuotes(quotesData || []);
    setUnassigned(un);
    setLoading(false);
  }, [user?.id]);

  useEffect(() => { load(); }, [load]);

  const selected = jobs.find((j) => j.id === selectedId) || null;

  // Load the assigned cost rows whenever a job is opened.
  useEffect(() => {
    if (!selectedId) { setDetail(null); return; }
    getJobCosts(user.id, selectedId).then(setDetail);
    const j = jobs.find((x) => x.id === selectedId);
    if (j) { setEstDraft(String(Number(j.est_cost) || 0)); setCollDraft(String(Number(j.collected) || 0)); }
  }, [selectedId]);

  // ── Portfolio totals ──
  const totals = jobs.reduce((a, j) => ({
    bid: a.bid + (Number(j.bid_amount) || 0),
    collected: a.collected + (Number(j.collected) || 0),
    spend: a.spend + (Number(j.actual_spend) || 0),
  }), { bid: 0, collected: 0, spend: 0 });
  const realProfit = totals.collected - totals.spend;
  const unassignedCount = unassigned.expenses.length + unassigned.trips.length + unassigned.plaidTxns.length;

  // Normalized unassigned cost list (newest first across all sources).
  const unassignedCosts = [
    ...unassigned.plaidTxns.map((r) => normalizeCost(r, "plaid")),
    ...unassigned.expenses.map((r) => normalizeCost(r, "expense")),
    ...unassigned.trips.map((r) => normalizeCost(r, "trip")),
  ].sort((a, b) => (b.date || "").localeCompare(a.date || ""));

  // ── Actions ──
  const saveEst = async () => {
    if (!selected) return;
    setBusy(true);
    const { error } = await updateJobCosting(user.id, selected.id, { est_cost: Number(estDraft) || 0 });
    setBusy(false);
    if (error) return flash("Could not save.");
    setJobs((p) => p.map((j) => j.id === selected.id ? { ...j, est_cost: Number(estDraft) || 0, est_margin: (Number(j.bid_amount) || 0) - (Number(estDraft) || 0), cost_variance: (Number(j.actual_spend) || 0) - (Number(estDraft) || 0) } : j));
    flash("Saved.");
  };

  const saveCollected = async (val) => {
    if (!selected) return;
    setBusy(true);
    const v = Number(val) || 0;
    const { error } = await updateJobCosting(user.id, selected.id, { collected: v });
    setBusy(false);
    if (error) return flash("Could not save.");
    setCollDraft(String(v));
    setJobs((p) => p.map((j) => j.id === selected.id ? { ...j, collected: v, actual_margin: v - (Number(j.actual_spend) || 0) } : j));
    flash("Saved.");
  };

  const assignToSelected = async (cost) => {
    if (!selected) return;
    setBusy(true);
    const { error } = await setCostJob(cost, user.id, selected.id);
    setBusy(false);
    if (error) return flash("Could not assign.");
    await load();
    // keep the job open + refresh its cost rows
    getJobCosts(user.id, selected.id).then(setDetail);
  };

  const unassign = async (cost) => {
    setBusy(true);
    const { error } = await setCostJob(cost, user.id, null);
    setBusy(false);
    if (error) return flash("Could not remove.");
    await load();
    if (selectedId) getJobCosts(user.id, selectedId).then(setDetail);
  };

  const makeJobFromQuote = async (quoteId) => {
    setBusy(true);
    const { data, error } = await createJobFromQuote(user.id, quoteId);
    setBusy(false);
    setShowNew(false);
    if (error || !data) return flash("Could not create job.");
    await load();
    setSelectedId(data.id);
    flash("Job created from quote.");
  };

  const linkQuote = async (quoteId) => {
    if (!selected) return;
    setBusy(true);
    const { error } = await linkJobToQuote(user.id, selected.id, quoteId);
    setBusy(false);
    if (error) return flash("Could not link.");
    await load();
    flash("Quote linked.");
  };

  // Quotes not already turned into a job.
  const linkedQuoteIds = new Set(jobs.map((j) => j.quote_id).filter(Boolean));
  const availableQuotes = quotes.filter((q) => !linkedQuoteIds.has(q.id));

  const wrap = { position: "fixed", inset: 0, zIndex: 150, background: "rgba(0,0,0,0.82)", backdropFilter: "blur(8px)", overflowY: "auto", display: "flex", justifyContent: "center", alignItems: "flex-start", padding: "24px 16px" };
  const panel = { background: "#111115", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 18, width: "100%", maxWidth: 760, padding: "24px" };

  // ── DETAIL VIEW ──
  if (selected) {
    const assignedCosts = detail ? [
      ...detail.plaidTxns.map((r) => normalizeCost(r, "plaid")),
      ...detail.expenses.map((r) => normalizeCost(r, "expense")),
      ...detail.trips.map((r) => normalizeCost(r, "trip")),
    ].sort((a, b) => (b.date || "").localeCompare(a.date || "")) : [];

    const margin = selected.collected > 0 ? selected.actual_margin : selected.projected_margin;
    return (
      <div style={wrap}>
        <div style={panel}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 18 }}>
            <button onClick={() => setSelectedId(null)} style={{ background: "transparent", border: "1px solid rgba(255,255,255,0.12)", borderRadius: 7, color: "rgba(255,255,255,0.5)", fontSize: 11, padding: "5px 11px", cursor: "pointer", fontFamily: "inherit" }}>← All jobs</button>
            <button onClick={onClose} style={{ background: "transparent", border: "none", color: "rgba(255,255,255,0.4)", fontSize: 22, cursor: "pointer" }}>✕</button>
          </div>

          <div style={{ fontFamily: "'Syne',sans-serif", fontSize: 18, fontWeight: 800, color: "#fff" }}>{selected.title}</div>
          <div style={{ fontSize: 11, color: "rgba(255,255,255,0.35)", marginTop: 2, fontFamily: "'DM Mono',monospace" }}>
            {[selected.client_name, selected.scheduled_date].filter(Boolean).join(" · ") || "—"}
          </div>

          {msg && <div style={{ marginTop: 12, fontSize: 11, color: GREEN, background: "rgba(125,206,160,0.08)", border: "1px solid rgba(125,206,160,0.2)", borderRadius: 7, padding: "7px 11px" }}>{msg}</div>}

          {/* Headline margin */}
          <div style={{ marginTop: 16, background: `linear-gradient(135deg, ${profitColor(margin)}14, rgba(255,255,255,0.02))`, border: `1px solid ${profitColor(margin)}33`, borderRadius: 14, padding: "16px 18px" }}>
            <div style={{ fontSize: 10, color: "rgba(255,255,255,0.4)", textTransform: "uppercase", letterSpacing: "0.1em" }}>
              {selected.collected > 0 ? "Actual profit (collected − spent)" : "Projected profit (bid − spent)"}
            </div>
            <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 30, fontWeight: 700, color: profitColor(margin), marginTop: 4 }}>{fmt(margin)}</div>
            <div style={{ fontSize: 11, color: "rgba(255,255,255,0.4)", marginTop: 2, fontFamily: "'DM Mono',monospace" }}>
              {selected.bid_amount > 0 ? `${(((selected.collected > 0 ? margin : selected.projected_margin) / selected.bid_amount) * 100).toFixed(0)}% margin` : "no bid linked"}
              {selected.est_cost > 0 && (
                <span style={{ color: profitColor(-selected.cost_variance), marginLeft: 8 }}>
                  · {selected.cost_variance > 0 ? "over" : "under"} budget by {fmt(Math.abs(selected.cost_variance))}
                </span>
              )}
            </div>
            <SpendBar spend={selected.actual_spend} bid={selected.bid_amount} estCost={selected.est_cost} />
          </div>

          {/* Bid vs actual grid */}
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 14, marginTop: 16 }}>
            {/* BID */}
            <div>
              <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 8 }}>The Bid</div>
              <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
                <Stat label="Bid amount" value={fmt(selected.bid_amount)} color={BLUE} />
                <div style={{ background: "rgba(255,255,255,0.025)", border: "1px solid rgba(255,255,255,0.07)", borderRadius: 10, padding: "11px 13px" }}>
                  <div style={{ fontSize: 9, color: "rgba(255,255,255,0.3)", textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 6 }}>Estimated cost</div>
                  <div style={{ display: "flex", gap: 6 }}>
                    <input value={estDraft} onChange={(e) => setEstDraft(e.target.value)} type="number" min="0" style={{ ...IS, padding: "5px 9px", fontFamily: "'DM Mono',monospace" }} onFocus={focusGold} onBlur={blurGray} />
                    <button onClick={saveEst} disabled={busy} style={{ padding: "5px 11px", borderRadius: 6, border: "1px solid rgba(232,201,122,0.35)", background: "rgba(232,201,122,0.08)", color: "#e8c97a", fontSize: 11, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>Save</button>
                  </div>
                </div>
                <Stat label="Estimated margin" value={fmt(selected.est_margin)} color={profitColor(selected.est_margin)} sub={selected.bid_amount > 0 ? `${selected.est_margin_pct.toFixed(0)}%` : ""} />
              </div>
            </div>
            {/* ACTUAL */}
            <div>
              <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 8 }}>The Reality</div>
              <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
                <Stat label="Spent — supplies & bank" value={fmt(selected.actual_bank)} color="#e8b87e" />
                <Stat label="Spent — mileage" value={fmt(selected.actual_mileage)} color="#a8e87e" />
                <Stat label="Total actual spend" value={fmt(selected.actual_spend)} color="#fff" sub={`${selected.cost_count} cost${selected.cost_count !== 1 ? "s" : ""} tagged`} />
              </div>
            </div>
          </div>

          {/* Collected */}
          <div style={{ marginTop: 14, background: "rgba(255,255,255,0.025)", border: "1px solid rgba(255,255,255,0.07)", borderRadius: 10, padding: "12px 14px" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 10, flexWrap: "wrap" }}>
              <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", textTransform: "uppercase", letterSpacing: "0.08em" }}>Collected from client</div>
              <div style={{ display: "flex", gap: 6, alignItems: "center" }}>
                <input value={collDraft} onChange={(e) => setCollDraft(e.target.value)} type="number" min="0" style={{ ...IS, width: 130, padding: "5px 9px", fontFamily: "'DM Mono',monospace" }} onFocus={focusGold} onBlur={blurGray} />
                <button onClick={() => saveCollected(collDraft)} disabled={busy} style={{ padding: "5px 11px", borderRadius: 6, border: "1px solid rgba(125,206,160,0.35)", background: "rgba(125,206,160,0.08)", color: GREEN, fontSize: 11, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>Save</button>
                {selected.bid_amount > 0 && (
                  <button onClick={() => saveCollected(selected.bid_amount)} disabled={busy} style={{ padding: "5px 11px", borderRadius: 6, border: "1px solid rgba(255,255,255,0.12)", background: "transparent", color: "rgba(255,255,255,0.5)", fontSize: 11, fontWeight: 600, cursor: "pointer", fontFamily: "inherit" }}>Paid in full</button>
                )}
              </div>
            </div>
          </div>

          {/* Link quote (only when not linked) */}
          {!selected.quote_id && availableQuotes.length > 0 && (
            <div style={{ marginTop: 14, background: "rgba(232,201,122,0.04)", border: "1px solid rgba(232,201,122,0.14)", borderRadius: 10, padding: "12px 14px" }}>
              <div style={{ fontSize: 11, color: "#e8c97a", marginBottom: 8 }}>Link the bid — pull the estimate so margin is real:</div>
              <select onChange={(e) => e.target.value && linkQuote(e.target.value)} defaultValue=""
                style={{ ...IS, colorScheme: "dark" }}>
                <option value="">Choose a quote…</option>
                {availableQuotes.map((q) => (
                  <option key={q.id} value={q.id}>{q.job_name || q.quote_number || "Quote"} — {fmt(q.total)} ({q.client_name || "—"})</option>
                ))}
              </select>
            </div>
          )}

          {/* Assigned costs */}
          <div style={{ marginTop: 18 }}>
            <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 10 }}>Tagged costs ({assignedCosts.length})</div>
            {assignedCosts.length === 0 ? (
              <div style={{ fontSize: 11, color: "rgba(255,255,255,0.25)", padding: "12px 0" }}>No costs tagged yet. Assign from the pool below.</div>
            ) : (
              <div style={{ display: "flex", flexDirection: "column", gap: 5 }}>
                {assignedCosts.map((c) => <CostRow key={`${c.kind}-${c.id}`} cost={c} onRemove={unassign} />)}
              </div>
            )}
          </div>

          {/* Unassigned pool — assign into this job (suggestions highlighted) */}
          {unassignedCosts.length > 0 && (
            <div style={{ marginTop: 18 }}>
              <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 10 }}>
                Assign costs to this job ({unassignedCosts.length} unassigned)
              </div>
              <div style={{ display: "flex", flexDirection: "column", gap: 5, maxHeight: 320, overflowY: "auto" }}>
                {unassignedCosts.slice(0, 60).map((c) => (
                  <CostRow key={`${c.kind}-${c.id}`} cost={c}
                    suggested={suggestJobForCost(c.date, jobs) === selected.id}
                    onAssign={assignToSelected} />
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    );
  }

  // ── LIST VIEW ──
  return (
    <div style={wrap}>
      <div style={panel}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 18 }}>
          <div>
            <div style={{ fontFamily: "'Syne',sans-serif", fontSize: 17, fontWeight: 800, color: "#fff" }}>Job Costing</div>
            <div style={{ fontSize: 11, color: "rgba(255,255,255,0.35)", marginTop: 2 }}>Bid vs. actual — every job's real profit, live</div>
          </div>
          <button onClick={onClose} style={{ background: "transparent", border: "none", color: "rgba(255,255,255,0.4)", fontSize: 22, cursor: "pointer" }}>✕</button>
        </div>

        {msg && <div style={{ marginBottom: 12, fontSize: 11, color: GREEN, background: "rgba(125,206,160,0.08)", border: "1px solid rgba(125,206,160,0.2)", borderRadius: 7, padding: "7px 11px" }}>{msg}</div>}

        {/* Portfolio */}
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(120px,1fr))", gap: 8, marginBottom: 16 }}>
          <Stat label="Total bid" value={fmt(totals.bid)} color={BLUE} />
          <Stat label="Collected" value={fmt(totals.collected)} color={GREEN} />
          <Stat label="Spent" value={fmt(totals.spend)} color="#e8b87e" />
          <Stat label="Real profit" value={fmt(realProfit)} color={profitColor(realProfit)} sub="collected − spent" />
        </div>

        {/* Actions */}
        <div style={{ display: "flex", gap: 8, marginBottom: 14, flexWrap: "wrap" }}>
          <button onClick={() => setShowNew((v) => !v)}
            style={{ flex: 1, minWidth: 200, padding: "11px", borderRadius: 10, background: "linear-gradient(135deg,rgba(126,184,232,0.18),rgba(126,184,232,0.07))", border: "1px solid rgba(126,184,232,0.4)", color: BLUE, fontSize: 13, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>
            + New job from a quote
          </button>
        </div>

        {showNew && (
          <div style={{ marginBottom: 16, background: "rgba(255,255,255,0.02)", border: "1px solid rgba(255,255,255,0.08)", borderRadius: 12, padding: "14px" }}>
            <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 10 }}>Turn a quote into a tracked job</div>
            {availableQuotes.length === 0 ? (
              <div style={{ fontSize: 11, color: "rgba(255,255,255,0.3)" }}>No unlinked quotes. Every quote already has a job, or you haven't saved one yet.</div>
            ) : (
              <div style={{ display: "flex", flexDirection: "column", gap: 5, maxHeight: 260, overflowY: "auto" }}>
                {availableQuotes.map((q) => (
                  <div key={q.id} style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 10, padding: "9px 11px", background: "rgba(255,255,255,0.02)", border: "1px solid rgba(255,255,255,0.05)", borderRadius: 8 }}>
                    <div style={{ minWidth: 0 }}>
                      <div style={{ fontSize: 12, color: "#fff", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{q.job_name || q.quote_number || "Quote"}</div>
                      <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", fontFamily: "'DM Mono',monospace" }}>{q.client_name || "—"} · {fmt(q.total)} · {q.status}</div>
                    </div>
                    <button onClick={() => makeJobFromQuote(q.id)} disabled={busy}
                      style={{ flexShrink: 0, padding: "5px 11px", borderRadius: 6, border: "1px solid rgba(126,184,232,0.35)", background: "rgba(126,184,232,0.08)", color: BLUE, fontSize: 11, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>
                      Create
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {unassignedCount > 0 && (
          <div style={{ marginBottom: 14, fontSize: 11, color: "rgba(232,201,122,0.85)", background: "rgba(232,201,122,0.05)", border: "1px solid rgba(232,201,122,0.15)", borderRadius: 9, padding: "9px 12px" }}>
            {unassignedCount} cost{unassignedCount !== 1 ? "s" : ""} not tagged to a job yet — open a job to assign them.
          </div>
        )}

        {/* Jobs list */}
        {loading ? (
          <div style={{ textAlign: "center", color: "rgba(255,255,255,0.3)", fontSize: 12, padding: "40px 0" }}>Loading…</div>
        ) : jobs.length === 0 ? (
          <div style={{ textAlign: "center", color: "rgba(255,255,255,0.25)", padding: "40px 0" }}>
            <div style={{ fontSize: 28, marginBottom: 8 }}>📐</div>
            <div style={{ fontSize: 13 }}>No jobs yet</div>
            <div style={{ fontSize: 11, marginTop: 4, color: "rgba(255,255,255,0.18)" }}>Create one from a quote to start tracking profit.</div>
          </div>
        ) : (
          jobs.map((j) => <JobCard key={j.id} job={j} onOpen={(job) => setSelectedId(job.id)} />)
        )}
      </div>
    </div>
  );
}
