/* eslint-disable react-hooks/exhaustive-deps */
// src/EliteMode.jsx
// ═══ WIREWAY ELITE — INDUSTRIAL ESTIMATOR ═══
// Full-screen industrial estimating environment: plain-English AI takeoff,
// assembly & catalog builder, condition/crew engine, bid recap with CSV export,
// change-order mode, confidence scoring, and a value-engineering copilot.
// Renders ONLY behind the Elite tier gate — invisible to everyone else.

import { useState, useMemo, useEffect, useRef } from "react";
import { supabase, getEliteEstimates, getEliteEstimate, upsertEliteEstimate, deleteEliteEstimate } from "./lib/supabase";
import { INDUSTRIAL_ITEMS, CONDITIONS, computeIndustrialLine, findIndustrialItem } from "./data/industrial-catalog";
import { ASSEMBLIES, CREWS, crewRate, expandAssembly } from "./data/industrial-assemblies";
import INDUSTRIAL_NEC from "./data/industrial-nec";
import { computeBidRecap, exportBidCSV, downloadCSV } from "./lib/bid-recap";

// Elite wears its own identity: steel + safety amber, independent of consumer themes.
const E = {
  bg: "#0b0e13", panel: "#12161d", card: "#171c25", line: "#232a36", lineStrong: "#2f3949",
  text: "#e8ecf2", dim: "rgba(232,236,242,0.55)", faint: "rgba(232,236,242,0.35)",
  amber: "#f0a818", amberDim: "rgba(240,168,24,0.14)", steel: "#9fb2c8", green: "#5fd38a", red: "#e87e7e",
};
const mono = "'DM Mono',monospace";

export default function EliteMode({ profile, onClose }) {
  const [view, setView]             = useState("build"); // build | recap | nec
  const [specs, setSpecs]           = useState([]);      // [{key,type:'asm'|'item',id,variant,qty}]
  const [conditions, setConditions] = useState([]);
  const [crewId, setCrewId]         = useState("jw");
  const [rate, setRate]             = useState(Number(profile?.hourly_rate) || 95);
  const [coMode, setCoMode]         = useState(false);
  const [jobName, setJobName]       = useState("");
  const [parentRef, setParentRef]   = useState("");

  // AI takeoff
  const [aiText, setAiText]         = useState("");
  const [aiBusy, setAiBusy]         = useState(false);
  const [aiError, setAiError]       = useState("");
  const [confidence, setConfidence] = useState(null);
  const [assumptions, setAssumptions] = useState([]);

  // VE copilot
  const [veQ, setVeQ]               = useState("");
  const [veBusy, setVeBusy]         = useState(false);
  const [veAnswer, setVeAnswer]     = useState("");

  // Recap knobs
  const [recapIn, setRecapIn] = useState({ burdenPct: 28, matTaxPct: 8, contingencyPct: 3, overheadPct: 10, profitPct: 12, bondPct: 0, mobilization: 0 });

  // Persistence
  const [estId, setEstId]           = useState(null);
  const [jobs, setJobs]             = useState([]);
  const [saveBusy, setSaveBusy]     = useState(false);
  const [savedFlash, setSavedFlash] = useState(false);
  const [panelBusy, setPanelBusy]   = useState(false);
  const fileRef = useRef(null);

  useEffect(() => {
    if (profile?.id) getEliteEstimates(profile.id).then(({ data }) => setJobs(data || []));
  }, []);

  const saveEstimate = async () => {
    if (!profile?.id || saveBusy || !specs.length) return;
    setSaveBusy(true);
    const row = {
      ...(estId ? { id: estId } : {}),
      user_id: profile.id,
      job_name: jobName || (coMode ? "Untitled change order" : "Untitled estimate"),
      co_mode: coMode,
      parent_ref: parentRef || null,
      payload: { specs, conditions, crewId, rate, recapIn },
      totals: { mat: totals.mat, hrs: totals.hrs, bid: recap.bidTotal },
    };
    const { data } = await upsertEliteEstimate(row);
    if (data?.id) {
      setEstId(data.id);
      setSavedFlash(true); setTimeout(() => setSavedFlash(false), 1800);
      getEliteEstimates(profile.id).then(({ data: d }) => setJobs(d || []));
    }
    setSaveBusy(false);
  };

  const loadJob = async (id) => {
    const { data } = await getEliteEstimate(id);
    if (!data) return;
    const p = data.payload || {};
    setSpecs((p.specs || []).map(s => ({ ...s, key: Date.now() + Math.random() })));
    setConditions(p.conditions || []);
    setCrewId(p.crewId || "jw");
    setRate(p.rate || 95);
    setRecapIn(p.recapIn || { burdenPct: 28, matTaxPct: 8, contingencyPct: 3, overheadPct: 10, profitPct: 12, bondPct: 0, mobilization: 0 });
    setJobName(data.job_name || ""); setCoMode(!!data.co_mode); setParentRef(data.parent_ref || "");
    setEstId(data.id); setView("build");
  };

  const removeJob = async (id) => {
    await deleteEliteEstimate(id);
    if (id === estId) setEstId(null);
    setJobs(j => j.filter(x => x.id !== id));
  };

  // Manual add pickers
  const [pickAsm, setPickAsm]       = useState(ASSEMBLIES[0].id);
  const [pickAsmQty, setPickAsmQty] = useState("");
  const [pickItem, setPickItem]     = useState(INDUSTRIAL_ITEMS[0].id);
  const [pickVar, setPickVar]       = useState(INDUSTRIAL_ITEMS[0].variants[0].label);
  const [pickQty, setPickQty]       = useState("");

  // ── DERIVED PRICING: specs + knobs in, priced lines out ──
  const priced = useMemo(() => specs.map(s => {
    if (s.type === "asm") {
      const r = expandAssembly(s.id, s.qty, rate, conditions, crewId);
      if (!r) return null;
      return { key: s.key, label: r.assembly, qty: s.qty, unit: r.driver, matTotal: r.matTotal, laborHrs: r.laborHrs, labTotal: r.labTotal, total: r.total, variant: "assembly" };
    }
    const item = findIndustrialItem(s.id);
    const variant = item?.variants.find(v => v.label === s.variant);
    if (!item || !variant) return null;
    const line = computeIndustrialLine(variant, s.qty, crewRate(rate, crewId), conditions);
    return { key: s.key, label: item.label, variant: s.variant, qty: s.qty, unit: item.unit, ...line };
  }).filter(Boolean), [specs, conditions, crewId, rate]);

  const totals = useMemo(() => priced.reduce((a, l) => ({
    mat: a.mat + l.matTotal, hrs: a.hrs + l.laborHrs, lab: a.lab + l.labTotal, total: a.total + l.total,
  }), { mat: 0, hrs: 0, lab: 0, total: 0 }), [priced]);

  const recap = useMemo(() => computeBidRecap({ matTotal: totals.mat, laborHrs: totals.hrs, rate: crewRate(rate, crewId), ...recapIn }), [totals, rate, crewId, recapIn]);

  const addSpec = (spec) => setSpecs(p => [...p, { ...spec, key: Date.now() + Math.random() }]);
  const removeSpec = (key) => setSpecs(p => p.filter(s => s.key !== key));
  const toggleCondition = (id) => setConditions(p => p.includes(id) ? p.filter(c => c !== id) : [...p, id]);

  // ── AI INDUSTRIAL TAKEOFF ──
  const buildIndex = () => {
    const asms = ASSEMBLIES.map(a => `${a.id} | ${a.label} | per ${a.driver === "ft" ? "FOOT" : "EACH"}`).join("\n");
    const items = INDUSTRIAL_ITEMS.map(i => `${i.id} (per ${i.unit}) variants: ${i.variants.map(v => v.label).join(", ")}`).join("\n");
    return { asms, items };
  };

  const applyParsed = (parsed) => {
    const next = [];
    for (const l of parsed.lines || []) {
      const qty = Number(l.qty) || 0;
      if (qty <= 0) continue;
      if (l.type === "asm" && ASSEMBLIES.find(a => a.id === l.id)) next.push({ type: "asm", id: l.id, qty });
      else if (l.type === "item") {
        const item = findIndustrialItem(l.id);
        if (item && item.variants.find(v => v.label === l.variant)) next.push({ type: "item", id: l.id, variant: l.variant, qty });
      }
    }
    if (!next.length) throw new Error("Couldn't map that scope — try adding sizes and footages");
    next.forEach(addSpec);
    if (Array.isArray(parsed.conditions)) setConditions(prev => [...new Set([...prev, ...parsed.conditions.filter(c => CONDITIONS.find(x => x.id === c))])]);
    if (typeof parsed.confidence === "number") setConfidence(Math.max(0, Math.min(100, parsed.confidence)));
    setAssumptions([...(parsed.assumptions || []), ...(parsed.exclusions || []).map(e => "Excluded: " + e)]);
  };

  const runTakeoff = async () => {
    if (!aiText.trim() || aiBusy) return;
    setAiBusy(true); setAiError(""); setConfidence(null); setAssumptions([]);
    try {
      const { data: { session } } = await supabase.auth.getSession();
      const { asms, items } = buildIndex();
      const sys = `You are a chief industrial electrical estimator. Convert the scope description into a takeoff using ONLY these libraries.

ASSEMBLIES (prefer these when scope matches — qty is in driver units):
${asms}

CATALOG ITEMS (variant label must match exactly):
${items}

CONDITIONS (apply only those clearly stated or strongly implied): ${CONDITIONS.map(c => c.id + "=" + c.label).join("; ")}

${coMode ? "THIS IS A CHANGE ORDER: price ONLY the added or changed scope described — never the original contract work.\n" : ""}RULES:
1. Extract exact quantities. Footage drives "ft" entries; counts drive "ea".
2. For feeders: one "ft" assembly entry for the run PLUS termination sets per end.
3. Be lean — never pad scope that wasn't described.
4. confidence: honest 0-100 score of takeoff completeness given the description.
5. assumptions: list what you assumed; exclusions: what you deliberately left out.
Respond ONLY with JSON, no markdown fences:
{"lines":[{"type":"asm","id":"...","qty":0} or {"type":"item","id":"...","variant":"...","qty":0}],"conditions":["..."],"confidence":0,"assumptions":["..."],"exclusions":["..."]}`;

      const res = await fetch("/api/claude", {
        method: "POST",
        headers: { "Content-Type": "application/json", ...(session?.access_token ? { Authorization: `Bearer ${session.access_token}` } : {}) },
        body: JSON.stringify({ max_tokens: 3000, system: sys, messages: [{ role: "user", content: aiText }] }),
      });
      if (!res.ok) throw new Error("AI request failed — check your connection");
      const data = await res.json();
      const raw = (data.content || []).filter(b => b.type === "text").map(b => b.text).join("").replace(/```json|```/g, "").trim();
      const parsed = JSON.parse(raw);

      applyParsed(parsed);
      setAiText("");
    } catch (e) {
      setAiError(e.message || "Takeoff failed — try again");
    }
    setAiBusy(false);
  };

  // ── PANEL SCHEDULE PHOTO READER ──
  const readPanelPhoto = async (file) => {
    if (!file || panelBusy) return;
    setPanelBusy(true); setAiError(""); setConfidence(null); setAssumptions([]);
    try {
      // Downscale on-device so the upload stays small and fast
      const bitmap = await createImageBitmap(file);
      const scale = Math.min(1, 1600 / Math.max(bitmap.width, bitmap.height));
      const canvas = document.createElement("canvas");
      canvas.width = Math.round(bitmap.width * scale);
      canvas.height = Math.round(bitmap.height * scale);
      canvas.getContext("2d").drawImage(bitmap, 0, 0, canvas.width, canvas.height);
      const b64 = canvas.toDataURL("image/jpeg", 0.82).split(",")[1];

      const { data: { session } } = await supabase.auth.getSession();
      const { asms, items } = buildIndex();
      const sys = `You are a chief industrial electrical estimator reading a PANEL SCHEDULE photo. Extract the circuits, then propose a takeoff for wiring those circuits using ONLY these libraries.

ASSEMBLIES:\n${asms}\n\nCATALOG ITEMS:\n${items}

RULES:
1. Count branch circuits by breaker size/poles from the schedule.
2. Assume 40 ft average run per branch circuit unless the photo indicates otherwise — state this in assumptions.
3. Use asm_branch_emt for 20A 1-pole circuits (qty = circuits × 40 ft); larger circuits use catalog items.
4. Skip spares and spaces. confidence: honest 0-100 (photo quality matters). List assumptions and exclusions.
Respond ONLY with JSON, no markdown fences:
{"lines":[...same schema...],"conditions":[],"confidence":0,"assumptions":["..."],"exclusions":["..."]}`;

      const res = await fetch("/api/claude", {
        method: "POST",
        headers: { "Content-Type": "application/json", ...(session?.access_token ? { Authorization: `Bearer ${session.access_token}` } : {}) },
        body: JSON.stringify({
          max_tokens: 3000, system: sys,
          messages: [{ role: "user", content: [
            { type: "image", source: { type: "base64", media_type: "image/jpeg", data: b64 } },
            { type: "text", text: "Read this panel schedule and produce the takeoff JSON." },
          ]}],
        }),
      });
      if (!res.ok) throw new Error("Photo read failed — check connection");
      const data = await res.json();
      const raw = (data.content || []).filter(b => b.type === "text").map(b => b.text).join("").replace(/```json|```/g, "").trim();
      applyParsed(JSON.parse(raw));
    } catch (e) {
      setAiError(e.message || "Couldn't read that photo — try a closer, straight-on shot");
    }
    setPanelBusy(false);
    if (fileRef.current) fileRef.current.value = "";
  };

  // ── VALUE ENGINEERING COPILOT ──
  const askVE = async (q) => {
    if (!q.trim() || veBusy || !priced.length) return;
    setVeBusy(true); setVeAnswer("");
    try {
      const { data: { session } } = await supabase.auth.getSession();
      const res = await fetch("/api/claude", {
        method: "POST",
        headers: { "Content-Type": "application/json", ...(session?.access_token ? { Authorization: `Bearer ${session.access_token}` } : {}) },
        body: JSON.stringify({
          max_tokens: 1200,
          system: "You are a chief industrial electrical estimator reviewing a bid. Be specific and numeric. Reference the actual lines. Keep it under 250 words, plain text.",
          messages: [{ role: "user", content: `Estimate lines:\n${JSON.stringify(priced.map(l => ({ label: l.label, variant: l.variant, qty: l.qty, unit: l.unit, total: Math.round(l.total) })))}\nTotals: material $${totals.mat.toFixed(0)}, ${totals.hrs.toFixed(1)} labor hrs, bid total $${recap.bidTotal.toFixed(0)}.\n\nQuestion: ${q}` }],
        }),
      });
      const data = await res.json();
      setVeAnswer((data.content || []).filter(b => b.type === "text").map(b => b.text).join("").trim() || "No answer returned.");
    } catch { setVeAnswer("Copilot request failed — try again."); }
    setVeBusy(false);
  };

  const doExport = () => {
    const name = (coMode ? "CO - " : "") + (jobName || "Industrial Estimate");
    downloadCSV(exportBidCSV({ jobName: name, items: priced, recap }));
  };

  const selItem = findIndustrialItem(pickItem);
  const fmt = (n) => "$" + n.toLocaleString(undefined, { maximumFractionDigits: 0 });

  // ── styles ──
  const inp = { background: E.card, border: `1px solid ${E.line}`, borderRadius: 8, color: E.text, padding: "9px 10px", fontSize: 13, fontFamily: "inherit", width: "100%" };
  const btn = (solid) => ({ padding: "10px 14px", borderRadius: 9, fontSize: 12.5, fontWeight: 800, cursor: "pointer", fontFamily: "inherit", border: solid ? "none" : `1px solid ${E.lineStrong}`, background: solid ? E.amber : "transparent", color: solid ? "#14100a" : E.steel, whiteSpace: "nowrap" });
  const chip = (on) => ({ padding: "7px 11px", borderRadius: 999, fontSize: 11, fontWeight: 700, cursor: "pointer", fontFamily: "inherit", border: `1px solid ${on ? E.amber : E.line}`, background: on ? E.amberDim : "transparent", color: on ? E.amber : E.dim });
  const tabS = (on) => ({ padding: "9px 14px", borderRadius: 8, fontSize: 12, fontWeight: 800, cursor: "pointer", fontFamily: "inherit", border: "none", background: on ? E.amberDim : "transparent", color: on ? E.amber : E.dim });

  return (
    <div style={{ position: "fixed", inset: 0, zIndex: 9000, background: E.bg, color: E.text, overflowY: "auto", fontFamily: "'Outfit','Syne',sans-serif" }}>
      {/* ── HEADER ── */}
      <div style={{ position: "sticky", top: 0, zIndex: 10, background: "rgba(11,14,19,0.92)", backdropFilter: "blur(12px)", borderBottom: `1px solid ${E.line}` }}>
        <div style={{ maxWidth: 760, margin: "0 auto", padding: "12px 16px", display: "flex", alignItems: "center", gap: 10 }}>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontFamily: "'Syne',sans-serif", fontWeight: 800, fontSize: 16, letterSpacing: "0.02em" }}>
              WIREWAY <span style={{ color: E.amber }}>ELITE</span>
            </div>
            <div style={{ fontSize: 9, letterSpacing: "0.22em", color: E.faint, fontWeight: 700 }}>INDUSTRIAL ESTIMATOR</div>
          </div>
          <div style={{ display: "flex", gap: 2, background: E.panel, borderRadius: 10, padding: 3 }}>
            <button style={tabS(view === "build")} onClick={() => setView("build")}>Build</button>
            <button style={tabS(view === "recap")} onClick={() => setView("recap")}>Recap</button>
            <button style={tabS(view === "nec")} onClick={() => setView("nec")}>NEC</button>
            <button style={tabS(view === "jobs")} onClick={() => setView("jobs")}>Jobs</button>
          </div>
          <button onClick={saveEstimate} disabled={saveBusy || !specs.length} aria-label="Save estimate" style={{ ...btn(false), padding: "9px 12px", color: savedFlash ? E.green : E.steel, borderColor: savedFlash ? E.green : E.lineStrong }}>
            {savedFlash ? "✓" : saveBusy ? "…" : "💾"}
          </button>
          <button onClick={onClose} aria-label="Exit Elite" style={{ ...btn(false), padding: "9px 12px" }}>✕</button>
        </div>
      </div>

      <div style={{ maxWidth: 760, margin: "0 auto", padding: "16px 16px 80px" }}>
        {view === "build" && (
          <>
            {/* ── JOB / CHANGE ORDER ── */}
            <div style={{ display: "flex", gap: 8, marginBottom: 12 }}>
              <input style={{ ...inp, flex: 2 }} placeholder={coMode ? "Change order title..." : "Job name..."} value={jobName} onChange={e => setJobName(e.target.value)} />
              <button style={chip(coMode)} onClick={() => setCoMode(!coMode)}>⟳ Change Order</button>
            </div>
            {coMode && (
              <input style={{ ...inp, marginBottom: 12, borderColor: E.amber }} placeholder="Parent job / contract reference..." value={parentRef} onChange={e => setParentRef(e.target.value)} />
            )}

            {/* ── AI TAKEOFF ── */}
            <div style={{ background: E.panel, border: `1px solid ${E.line}`, borderRadius: 14, padding: 16, marginBottom: 14 }}>
              <div style={{ fontSize: 12, fontWeight: 800, marginBottom: 8, color: E.amber }}>⚡ AI TAKEOFF {coMode && "— CHANGE ORDER MODE"}</div>
              <textarea
                style={{ ...inp, minHeight: 88, resize: "vertical", lineHeight: 1.5 }}
                placeholder={coMode
                  ? 'Describe ONLY the added scope... e.g. "GC added 6 welding receptacles along the north wall, 120ft of 1 inch EMT total, plant stays running"'
                  : 'Describe the scope... e.g. "Pull 200ft of 400A feeder from the switchgear to MCC-2 at 25ft, terminate both ends, hook up a 30HP motor, occupied plant"'}
                value={aiText} onChange={e => setAiText(e.target.value)}
              />
              <div style={{ display: "flex", gap: 8, marginTop: 10 }}>
                <button style={{ ...btn(true), flex: 2, opacity: aiBusy ? 0.6 : 1 }} onClick={runTakeoff} disabled={aiBusy || panelBusy}>
                  {aiBusy ? "Running takeoff..." : "Run AI Takeoff"}
                </button>
                <button style={{ ...btn(false), flex: 1, opacity: panelBusy ? 0.6 : 1 }} onClick={() => fileRef.current?.click()} disabled={aiBusy || panelBusy}>
                  {panelBusy ? "Reading..." : "📷 Panel"}
                </button>
                <input ref={fileRef} type="file" accept="image/*" style={{ display: "none" }} onChange={e => readPanelPhoto(e.target.files?.[0])} />
              </div>
              {aiError && <div style={{ color: E.red, fontSize: 11.5, marginTop: 8 }}>{aiError}</div>}
              {confidence !== null && (
                <div style={{ marginTop: 10, display: "flex", alignItems: "center", gap: 8 }}>
                  <span style={{ fontSize: 10.5, color: E.dim, fontWeight: 700 }}>TAKEOFF CONFIDENCE</span>
                  <div style={{ flex: 1, height: 5, background: E.card, borderRadius: 3, overflow: "hidden" }}>
                    <div style={{ width: `${confidence}%`, height: "100%", borderRadius: 3, background: confidence >= 75 ? E.green : confidence >= 50 ? E.amber : E.red }} />
                  </div>
                  <span style={{ fontFamily: mono, fontSize: 12, fontWeight: 700, color: confidence >= 75 ? E.green : confidence >= 50 ? E.amber : E.red }}>{confidence}%</span>
                </div>
              )}
              {assumptions.length > 0 && (
                <div style={{ marginTop: 8, fontSize: 10.5, color: E.dim, lineHeight: 1.6 }}>
                  {assumptions.map((a, i) => <div key={i}>• {a}</div>)}
                </div>
              )}
            </div>

            {/* ── CONDITIONS & CREW ── */}
            <div style={{ background: E.panel, border: `1px solid ${E.line}`, borderRadius: 14, padding: 16, marginBottom: 14 }}>
              <div style={{ fontSize: 11, fontWeight: 800, color: E.dim, marginBottom: 9 }}>JOB CONDITIONS</div>
              <div style={{ display: "flex", flexWrap: "wrap", gap: 6, marginBottom: 13 }}>
                {CONDITIONS.map(c => (
                  <button key={c.id} style={chip(conditions.includes(c.id))} onClick={() => toggleCondition(c.id)}>
                    {c.label} ×{c.factor}
                  </button>
                ))}
              </div>
              <div style={{ display: "flex", gap: 8 }}>
                <select style={{ ...inp, flex: 2 }} value={crewId} onChange={e => setCrewId(e.target.value)}>
                  {CREWS.map(c => <option key={c.id} value={c.id}>{c.label}</option>)}
                </select>
                <div style={{ flex: 1, position: "relative" }}>
                  <span style={{ position: "absolute", left: 10, top: 10, fontSize: 13, color: E.dim }}>$</span>
                  <input style={{ ...inp, paddingLeft: 22 }} type="number" inputMode="decimal" value={rate} onChange={e => setRate(Number(e.target.value) || 0)} />
                  <span style={{ position: "absolute", right: 10, top: 11, fontSize: 10, color: E.faint }}>/hr</span>
                </div>
              </div>
            </div>

            {/* ── MANUAL ADD ── */}
            <div style={{ background: E.panel, border: `1px solid ${E.line}`, borderRadius: 14, padding: 16, marginBottom: 14 }}>
              <div style={{ fontSize: 11, fontWeight: 800, color: E.dim, marginBottom: 9 }}>ADD ASSEMBLY</div>
              <div style={{ display: "flex", gap: 8, marginBottom: 12 }}>
                <select style={{ ...inp, flex: 3 }} value={pickAsm} onChange={e => setPickAsm(e.target.value)}>
                  {ASSEMBLIES.map(a => <option key={a.id} value={a.id}>{a.label} (per {a.driver})</option>)}
                </select>
                <input style={{ ...inp, flex: 1 }} type="number" inputMode="decimal" placeholder="Qty" value={pickAsmQty} onChange={e => setPickAsmQty(e.target.value)} />
                <button style={btn(false)} onClick={() => { const q = Number(pickAsmQty); if (q > 0) { addSpec({ type: "asm", id: pickAsm, qty: q }); setPickAsmQty(""); } }}>Add</button>
              </div>
              <div style={{ fontSize: 11, fontWeight: 800, color: E.dim, marginBottom: 9 }}>ADD CATALOG ITEM</div>
              <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                <select style={{ ...inp, flex: "2 1 100%" }} value={pickItem} onChange={e => { setPickItem(e.target.value); const it = findIndustrialItem(e.target.value); setPickVar(it.variants[0].label); }}>
                  {INDUSTRIAL_ITEMS.map(i => <option key={i.id} value={i.id}>{i.categoryLabel} — {i.label}</option>)}
                </select>
                <select style={{ ...inp, flex: 2 }} value={pickVar} onChange={e => setPickVar(e.target.value)}>
                  {(selItem?.variants || []).map(v => <option key={v.label} value={v.label}>{v.label}</option>)}
                </select>
                <input style={{ ...inp, flex: 1, minWidth: 70 }} type="number" inputMode="decimal" placeholder={`Qty (${selItem?.unit})`} value={pickQty} onChange={e => setPickQty(e.target.value)} />
                <button style={btn(false)} onClick={() => { const q = Number(pickQty); if (q > 0) { addSpec({ type: "item", id: pickItem, variant: pickVar, qty: q }); setPickQty(""); } }}>Add</button>
              </div>
            </div>

            {/* ── LINES ── */}
            {priced.length > 0 && (
              <div style={{ background: E.panel, border: `1px solid ${E.line}`, borderRadius: 14, padding: 16, marginBottom: 14 }}>
                <div style={{ fontSize: 11, fontWeight: 800, color: E.dim, marginBottom: 10 }}>TAKEOFF — {priced.length} LINE{priced.length !== 1 ? "S" : ""}</div>
                {priced.map(l => (
                  <div key={l.key} style={{ display: "flex", alignItems: "center", gap: 10, padding: "9px 0", borderBottom: `1px solid ${E.line}` }}>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontSize: 12.5, fontWeight: 700, lineHeight: 1.35 }}>{l.label}</div>
                      <div style={{ fontSize: 10.5, color: E.dim, fontFamily: mono }}>
                        {l.variant !== "assembly" ? l.variant + " · " : ""}{l.qty} {l.unit} · {l.laborHrs.toFixed(1)} hrs
                      </div>
                    </div>
                    <div style={{ fontFamily: mono, fontSize: 13, fontWeight: 700, color: E.amber }}>{fmt(l.total)}</div>
                    <button onClick={() => removeSpec(l.key)} aria-label="Remove line" style={{ background: "transparent", border: "none", color: E.faint, cursor: "pointer", fontSize: 13, padding: 4, fontFamily: "inherit" }}>✕</button>
                  </div>
                ))}
                <div style={{ display: "flex", justifyContent: "space-between", paddingTop: 12, fontSize: 13 }}>
                  <span style={{ color: E.dim }}>Material {fmt(totals.mat)} · {totals.hrs.toFixed(1)} hrs</span>
                  <span style={{ fontFamily: mono, fontWeight: 800, color: E.text }}>{fmt(totals.total)} direct</span>
                </div>
                <button style={{ ...btn(true), width: "100%", marginTop: 12 }} onClick={() => setView("recap")}>Build Bid Recap →</button>
              </div>
            )}

            {/* ── VE COPILOT ── */}
            {priced.length > 0 && (
              <div style={{ background: E.panel, border: `1px solid ${E.line}`, borderRadius: 14, padding: 16 }}>
                <div style={{ fontSize: 12, fontWeight: 800, marginBottom: 9, color: E.steel }}>🛠 ESTIMATOR COPILOT</div>
                <div style={{ display: "flex", gap: 6, flexWrap: "wrap", marginBottom: 10 }}>
                  <button style={chip(false)} onClick={() => askVE("Find ways to reduce this estimate by 8% without cutting scope quality.")}>Cut 8%</button>
                  <button style={chip(false)} onClick={() => askVE("Would aluminum feeders instead of copper save money here? Show the tradeoffs.")}>Cu vs Al feeders</button>
                  <button style={chip(false)} onClick={() => askVE("What are the biggest risks and most likely busts in this estimate?")}>Risk check</button>
                </div>
                <div style={{ display: "flex", gap: 8 }}>
                  <input style={{ ...inp, flex: 1 }} placeholder="Ask anything about this estimate..." value={veQ} onChange={e => setVeQ(e.target.value)} onKeyDown={e => { if (e.key === "Enter") { askVE(veQ); setVeQ(""); } }} />
                  <button style={btn(false)} disabled={veBusy} onClick={() => { askVE(veQ); setVeQ(""); }}>{veBusy ? "..." : "Ask"}</button>
                </div>
                {veAnswer && <div style={{ marginTop: 11, fontSize: 12, lineHeight: 1.65, color: E.text, whiteSpace: "pre-wrap" }}>{veAnswer}</div>}
              </div>
            )}
          </>
        )}

        {/* ── RECAP VIEW ── */}
        {view === "recap" && (
          <>
            {coMode && (
              <div style={{ background: E.amberDim, border: `1px solid ${E.amber}`, borderRadius: 10, padding: "10px 14px", marginBottom: 14, fontSize: 12, fontWeight: 700, color: E.amber }}>
                ⟳ CHANGE ORDER{parentRef ? ` — ${parentRef}` : ""}
              </div>
            )}
            <div style={{ background: E.panel, border: `1px solid ${E.line}`, borderRadius: 14, padding: 16, marginBottom: 14 }}>
              <div style={{ fontSize: 11, fontWeight: 800, color: E.dim, marginBottom: 11 }}>RECAP SETTINGS</div>
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 9 }}>
                {[["burdenPct","Burden %"],["matTaxPct","Mat Tax %"],["contingencyPct","Conting. %"],["overheadPct","Overhead %"],["profitPct","Profit %"],["bondPct","Bond %"]].map(([k, lab]) => (
                  <div key={k}>
                    <div style={{ fontSize: 9.5, color: E.faint, fontWeight: 700, marginBottom: 4 }}>{lab}</div>
                    <input style={inp} type="number" inputMode="decimal" value={recapIn[k]} onChange={e => setRecapIn(p => ({ ...p, [k]: Number(e.target.value) || 0 }))} />
                  </div>
                ))}
              </div>
            </div>
            <div style={{ background: E.panel, border: `1px solid ${E.line}`, borderRadius: 14, padding: 16, marginBottom: 14 }}>
              <div style={{ fontSize: 11, fontWeight: 800, color: E.dim, marginBottom: 10 }}>BID RECAP {jobName && `— ${jobName.toUpperCase()}`}</div>
              {recap.lines.map(l => (
                <div key={l.key} style={{ display: "flex", justifyContent: "space-between", padding: l.total ? "12px 0 2px" : l.subtotal ? "9px 0" : "5px 0", borderTop: l.total ? `2px solid ${E.amber}` : l.subtotal ? `1px solid ${E.lineStrong}` : "none", marginTop: l.total ? 8 : 0 }}>
                  <span style={{ fontSize: l.total ? 14 : 12, fontWeight: l.total ? 800 : l.subtotal ? 700 : 500, color: l.total ? E.amber : l.subtotal ? E.text : E.dim }}>{l.label}</span>
                  <span style={{ fontFamily: mono, fontSize: l.total ? 16 : 12.5, fontWeight: l.total ? 800 : 600, color: l.total ? E.amber : E.text }}>{fmt(l.amount)}</span>
                </div>
              ))}
              <div style={{ display: "flex", gap: 14, marginTop: 12, fontSize: 10.5, color: E.faint, fontFamily: mono }}>
                <span>eff. ${recap.effectiveRate.toFixed(0)}/hr</span>
                <span>markup {recap.markupOnCost.toFixed(1)}%</span>
              </div>
            </div>
            <button style={{ ...btn(true), width: "100%" }} onClick={doExport} disabled={!priced.length}>⬇ Export Bid CSV (Excel)</button>
            <button style={{ ...btn(false), width: "100%", marginTop: 9 }} onClick={() => setView("build")}>← Back to takeoff</button>
          </>
        )}

        {/* ── SAVED JOBS ── */}
        {view === "jobs" && (
          <>
            {jobs.length === 0 && (
              <div style={{ textAlign: "center", padding: "50px 20px", color: E.dim, fontSize: 13 }}>
                No saved Elite estimates yet — build a takeoff and tap 💾
              </div>
            )}
            {jobs.map(j => (
              <div key={j.id} style={{ background: E.panel, border: `1px solid ${j.id === estId ? E.amber : E.line}`, borderRadius: 12, padding: "13px 14px", marginBottom: 9, display: "flex", alignItems: "center", gap: 11 }}>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 13, fontWeight: 700, lineHeight: 1.35 }}>
                    {j.co_mode && <span style={{ color: E.amber, fontSize: 10, fontWeight: 800, marginRight: 6 }}>CO</span>}
                    {j.job_name}
                  </div>
                  <div style={{ fontSize: 10.5, color: E.dim, fontFamily: mono }}>
                    {j.totals?.bid ? "$" + Math.round(j.totals.bid).toLocaleString() : "—"} · {new Date(j.updated_at).toLocaleDateString()}
                  </div>
                </div>
                <button style={btn(false)} onClick={() => loadJob(j.id)}>Open</button>
                <button onClick={() => removeJob(j.id)} aria-label="Delete estimate" style={{ background: "transparent", border: "none", color: E.faint, cursor: "pointer", fontSize: 13, padding: 4, fontFamily: "inherit" }}>🗑</button>
              </div>
            ))}
          </>
        )}

        {/* ── INDUSTRIAL NEC ── */}
        {view === "nec" && (
          <NecBrowser />
        )}
      </div>
    </div>
  );
}

function NecBrowser() {
  const [open, setOpen] = useState(null);
  const [q, setQ] = useState("");
  const list = INDUSTRIAL_NEC.filter(a => !q || (a.article + a.title + a.summary).toLowerCase().includes(q.toLowerCase()));
  return (
    <>
      <input style={{ background: E.card, border: `1px solid ${E.line}`, borderRadius: 8, color: E.text, padding: "10px 12px", fontSize: 13, fontFamily: "inherit", width: "100%", marginBottom: 12 }} placeholder="Search industrial articles... (motors, hazardous, fire pump)" value={q} onChange={e => setQ(e.target.value)} />
      {list.map(a => (
        <div key={a.article} style={{ background: E.panel, border: `1px solid ${E.line}`, borderRadius: 12, marginBottom: 9, overflow: "hidden" }}>
          <button onClick={() => setOpen(open === a.article ? null : a.article)} style={{ width: "100%", display: "flex", alignItems: "center", gap: 10, padding: "13px 14px", background: "transparent", border: "none", cursor: "pointer", textAlign: "left", fontFamily: "inherit", color: E.text }}>
            <span style={{ fontFamily: mono, fontSize: 12, fontWeight: 700, color: a.color, flexShrink: 0 }}>{a.article}</span>
            <span style={{ fontSize: 12.5, fontWeight: 700, flex: 1 }}>{a.title}</span>
            <span style={{ color: E.faint, fontSize: 11 }}>{open === a.article ? "▲" : "▼"}</span>
          </button>
          {open === a.article && (
            <div style={{ padding: "0 14px 14px" }}>
              <div style={{ fontSize: 11.5, color: E.dim, lineHeight: 1.6, marginBottom: 10 }}>{a.summary}</div>
              {a.rules.map((r, i) => (
                <div key={i} style={{ marginBottom: 9 }}>
                  <div style={{ fontSize: 11, fontWeight: 800, color: E.steel }}><span style={{ fontFamily: mono, color: a.color }}>{r.code}</span> — {r.title}</div>
                  <div style={{ fontSize: 11, color: E.dim, lineHeight: 1.55 }}>{r.text}</div>
                </div>
              ))}
              {a.violations.length > 0 && (
                <div style={{ marginTop: 10, padding: "9px 11px", background: "rgba(232,126,126,0.07)", border: `1px solid rgba(232,126,126,0.25)`, borderRadius: 8 }}>
                  {a.violations.map((v, i) => <div key={i} style={{ fontSize: 10.5, color: E.red, lineHeight: 1.6 }}>⚠ {v}</div>)}
                </div>
              )}
            </div>
          )}
        </div>
      ))}
    </>
  );
}
