/* eslint-disable react-hooks/exhaustive-deps */
// src/TimeTrackingView.jsx — Time-to-Job labor timer  ·  Feature 4
import { useState, useEffect, useCallback, useRef } from "react";
import {
  getRunningTimers, startTimer, stopTimer, addManualEntry, deleteTimeEntry,
  getTimeEntries, getDefaultRate, setDefaultRate, entryLaborCost,
} from "./lib/timeTracking";
import { getRecentJobs } from "./lib/receipts";

const IS = {
  background: "rgba(255,255,255,0.04)", border: "1px solid rgba(255,255,255,0.09)",
  borderRadius: 7, padding: "8px 11px", fontSize: 13, color: "#fff",
  fontFamily: "inherit", outline: "none", width: "100%",
};
const focusGold = (e) => (e.target.style.borderColor = "rgba(232,201,122,0.4)");
const blurGray  = (e) => (e.target.style.borderColor = "rgba(255,255,255,0.09)");
const fmt = (n) => "$" + (Number(n) || 0).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const PURPLE = "#b87ee8", GREEN = "#7dcea0", BLUE = "#7eb8e8";
const TODAY = new Date().toISOString().split("T")[0];

const elapsed = (sinceIso, nowMs) => {
  const s = Math.max(0, Math.floor((nowMs - new Date(sinceIso).getTime()) / 1000));
  const h = Math.floor(s / 3600), m = Math.floor((s % 3600) / 60), sec = s % 60;
  return `${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}:${String(sec).padStart(2, "0")}`;
};

export default function TimeTrackingView({ user, onClose }) {
  const [jobs,     setJobs]     = useState([]);
  const [running,  setRunning]  = useState([]);
  const [entries,  setEntries]  = useState([]);
  const [loading,  setLoading]  = useState(true);
  const [msg,      setMsg]      = useState("");
  const [now,      setNow]      = useState(() => 0); // ms; seeded after mount to avoid Date.now at import
  const [jobId,    setJobId]    = useState("");
  const [rate,     setRate]     = useState(() => getDefaultRate());
  const [worker,   setWorker]   = useState("");
  const [manual,   setManual]   = useState({ job_id: "", date: TODAY, hours: "", worker_name: "", notes: "" });
  const [showManual, setShowManual] = useState(false);
  const tick = useRef(null);

  const flash = (m) => { setMsg(m); setTimeout(() => setMsg(""), 2500); };

  const load = useCallback(async () => {
    if (!user?.id) return;
    setLoading(true);
    const [j, r, e] = await Promise.all([getRecentJobs(user.id, 40), getRunningTimers(user.id), getTimeEntries(user.id)]);
    setJobs(j); setRunning(r); setEntries(e);
    if (!jobId && j[0]) setJobId(j[0].id);
    setLoading(false);
  }, [user?.id]);

  useEffect(() => { load(); }, [load]);

  // Live clock — only run an interval while a timer is active.
  useEffect(() => {
    setNow(Date.now());
    if (running.length === 0) { if (tick.current) clearInterval(tick.current); return; }
    tick.current = setInterval(() => setNow(Date.now()), 1000);
    return () => tick.current && clearInterval(tick.current);
  }, [running.length]);

  const jobName = (id) => { const j = jobs.find((x) => x.id === id); return j ? j.title : "No job"; };

  const onStart = async () => {
    const r = Number(rate) || 0;
    setDefaultRate(r);
    const { error } = await startTimer(user.id, { job_id: jobId || null, worker_name: worker.trim() || null, rate: r });
    if (error) return flash("Could not start timer.");
    setWorker("");
    await load(); flash("Clocked in.");
  };

  const onStop = async (entry) => {
    const { error } = await stopTimer(user.id, entry);
    if (error) return flash("Could not stop timer.");
    await load(); flash("Clocked out — labor logged.");
  };

  const onAddManual = async () => {
    const h = parseFloat(manual.hours);
    if (!h || h <= 0) return flash("Enter hours.");
    const { error } = await addManualEntry(user.id, { ...manual, hours: h, rate: Number(rate) || 0 });
    if (error) return flash("Could not save.");
    setDefaultRate(Number(rate) || 0);
    setManual((m) => ({ ...m, hours: "", notes: "" }));
    await load(); flash("Time logged.");
  };

  const onDelete = async (id) => { setEntries((p) => p.filter((e) => e.id !== id)); await deleteTimeEntry(id, user.id); };

  const totalHours = entries.reduce((s, e) => s + (Number(e.hours) || 0), 0);
  const totalLabor = entries.reduce((s, e) => s + entryLaborCost(e), 0);

  const wrap = { position: "fixed", inset: 0, zIndex: 360, background: "rgba(0,0,0,0.82)", backdropFilter: "blur(8px)", overflowY: "auto", display: "flex", justifyContent: "center", alignItems: "flex-start", padding: "24px 16px" };
  const panel = { background: "#111115", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 18, width: "100%", maxWidth: 680, padding: "24px" };

  return (
    <div style={wrap}>
      <div style={panel}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 18 }}>
          <div>
            <div style={{ fontFamily: "'Syne',sans-serif", fontSize: 17, fontWeight: 800, color: "#fff" }}>Time on the Job</div>
            <div style={{ fontSize: 11, color: "rgba(255,255,255,0.35)", marginTop: 2 }}>Clock in/out · real labor cost flows to Job Costing</div>
          </div>
          <button onClick={onClose} style={{ background: "transparent", border: "none", color: "rgba(255,255,255,0.4)", fontSize: 22, cursor: "pointer" }}>✕</button>
        </div>

        {msg && <div style={{ marginBottom: 12, fontSize: 11, color: GREEN, background: "rgba(125,206,160,0.08)", border: "1px solid rgba(125,206,160,0.2)", borderRadius: 7, padding: "7px 11px" }}>{msg}</div>}

        {/* Running timers */}
        {running.length > 0 && (
          <div style={{ marginBottom: 16, display: "flex", flexDirection: "column", gap: 8 }}>
            {running.map((e) => (
              <div key={e.id} style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 12, padding: "14px 16px", background: "linear-gradient(135deg,rgba(184,126,232,0.12),rgba(255,255,255,0.02))", border: "1px solid rgba(184,126,232,0.3)", borderRadius: 12 }}>
                <div style={{ minWidth: 0 }}>
                  <div style={{ fontSize: 12, color: "#fff", fontWeight: 700, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{jobName(e.job_id)}{e.worker_name ? ` · ${e.worker_name}` : ""}</div>
                  <div style={{ fontSize: 10, color: "rgba(255,255,255,0.35)" }}>Running · {fmt(e.rate)}/hr</div>
                </div>
                <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 22, fontWeight: 600, color: PURPLE, flexShrink: 0 }}>{now ? elapsed(e.clock_in, now) : "00:00:00"}</div>
                <button onClick={() => onStop(e)} style={{ flexShrink: 0, padding: "9px 16px", borderRadius: 9, border: "1px solid rgba(232,126,126,0.4)", background: "rgba(232,126,126,0.1)", color: "#e87e7e", fontSize: 12, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>Clock Out</button>
              </div>
            ))}
          </div>
        )}

        {/* Clock in */}
        <div style={{ background: "rgba(255,255,255,0.02)", border: "1px solid rgba(255,255,255,0.06)", borderRadius: 12, padding: "14px", marginBottom: 12 }}>
          <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 10 }}>Clock in</div>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 110px", gap: 7, marginBottom: 7 }}>
            <select value={jobId} onChange={(e) => setJobId(e.target.value)} style={{ ...IS, colorScheme: "dark" }}>
              <option value="">No job</option>
              {jobs.map((j) => <option key={j.id} value={j.id}>{j.title}{j.client_name ? ` · ${j.client_name}` : ""}</option>)}
            </select>
            <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
              <span style={{ fontSize: 12, color: "rgba(255,255,255,0.4)" }}>$</span>
              <input type="number" min="0" step="1" placeholder="rate/hr" value={rate} onChange={(e) => setRate(e.target.value)} style={{ ...IS, fontFamily: "'DM Mono',monospace" }} onFocus={focusGold} onBlur={blurGray} />
            </div>
          </div>
          <input placeholder="Worker name (optional, for crews)" value={worker} onChange={(e) => setWorker(e.target.value)} style={{ ...IS, marginBottom: 10 }} onFocus={focusGold} onBlur={blurGray} />
          <button onClick={onStart} style={{ width: "100%", padding: "11px", borderRadius: 9, background: "linear-gradient(135deg,rgba(184,126,232,0.18),rgba(184,126,232,0.07))", border: "1px solid rgba(184,126,232,0.4)", color: PURPLE, fontSize: 13, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>▶ Clock In</button>
          <button onClick={() => setShowManual((v) => !v)} style={{ width: "100%", marginTop: 7, padding: "8px", borderRadius: 8, background: "transparent", border: "1px solid rgba(255,255,255,0.1)", color: "rgba(255,255,255,0.45)", fontSize: 11, fontWeight: 600, cursor: "pointer", fontFamily: "inherit" }}>
            {showManual ? "Hide manual entry" : "+ Add hours manually"}
          </button>

          {showManual && (
            <div style={{ marginTop: 10, paddingTop: 10, borderTop: "1px solid rgba(255,255,255,0.06)" }}>
              <div style={{ display: "grid", gridTemplateColumns: "1fr 110px 90px", gap: 7, marginBottom: 7 }}>
                <select value={manual.job_id} onChange={(e) => setManual((m) => ({ ...m, job_id: e.target.value }))} style={{ ...IS, colorScheme: "dark" }}>
                  <option value="">No job</option>
                  {jobs.map((j) => <option key={j.id} value={j.id}>{j.title}</option>)}
                </select>
                <input type="date" value={manual.date} onChange={(e) => setManual((m) => ({ ...m, date: e.target.value }))} style={{ ...IS, colorScheme: "dark" }} onFocus={focusGold} onBlur={blurGray} />
                <input type="number" min="0.25" step="0.25" placeholder="hrs" value={manual.hours} onChange={(e) => setManual((m) => ({ ...m, hours: e.target.value }))} style={{ ...IS, fontFamily: "'DM Mono',monospace" }} onFocus={focusGold} onBlur={blurGray} />
              </div>
              <button onClick={onAddManual} style={{ width: "100%", padding: "9px", borderRadius: 8, background: "rgba(184,126,232,0.08)", border: "1px solid rgba(184,126,232,0.25)", color: PURPLE, fontSize: 12, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>Add {manual.hours || "0"} hrs @ {fmt(rate)}/hr</button>
            </div>
          )}
        </div>

        {/* Summary */}
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8, marginBottom: 14 }}>
          <div style={{ background: "rgba(255,255,255,0.025)", border: "1px solid rgba(255,255,255,0.07)", borderRadius: 10, padding: "11px 13px" }}>
            <div style={{ fontSize: 9, color: "rgba(255,255,255,0.3)", textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 5 }}>Logged hours</div>
            <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 18, fontWeight: 600, color: BLUE }}>{loading ? "…" : totalHours.toFixed(2)}</div>
          </div>
          <div style={{ background: "rgba(255,255,255,0.025)", border: "1px solid rgba(255,255,255,0.07)", borderRadius: 10, padding: "11px 13px" }}>
            <div style={{ fontSize: 9, color: "rgba(255,255,255,0.3)", textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 5 }}>Labor cost</div>
            <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 18, fontWeight: 600, color: PURPLE }}>{loading ? "…" : fmt(totalLabor)}</div>
          </div>
        </div>

        {/* Entries */}
        {!loading && entries.length === 0 ? (
          <div style={{ textAlign: "center", color: "rgba(255,255,255,0.25)", padding: "30px 0" }}>
            <div style={{ fontSize: 26, marginBottom: 8 }}>⏱️</div>
            <div style={{ fontSize: 12 }}>No time logged yet</div>
          </div>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: 5 }}>
            {entries.map((e) => (
              <div key={e.id} style={{ display: "flex", alignItems: "center", gap: 10, padding: "9px 12px", background: "rgba(255,255,255,0.022)", border: "1px solid rgba(255,255,255,0.055)", borderRadius: 9 }}>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 12, color: "#fff", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{jobName(e.job_id)}{e.worker_name ? ` · ${e.worker_name}` : ""}</div>
                  <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", fontFamily: "'DM Mono',monospace" }}>{(e.clock_in || "").slice(0, 10)} · {Number(e.hours).toFixed(2)} hr @ {fmt(e.rate)}</div>
                </div>
                <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 13, fontWeight: 700, color: PURPLE, flexShrink: 0 }}>{fmt(entryLaborCost(e))}</div>
                <button onClick={() => onDelete(e.id)} style={{ width: 24, height: 24, borderRadius: 6, border: "1px solid rgba(232,126,126,0.2)", background: "rgba(232,126,126,0.06)", color: "rgba(232,126,126,0.5)", fontSize: 11, cursor: "pointer", flexShrink: 0 }}>✕</button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
