// src/AIQuoteBuilder.jsx
// AI Quote Builder — describe a job in plain English, get a full estimate.
// Uses the Anthropic API (via /api/claude) to identify services, map them to
// the catalog, calculate labor/materials, and populate the estimate.
//
// v2: returns one clean JSON object (services + summary + assumptions),
// parsed with a tolerant balanced-brace reader + cutoff salvage; surfaces the
// AI's assumptions; and lets you adjust quantities inline before applying.

import { useState } from "react";
import { supabase } from "./lib/supabase";
import { ALL_SERVICES, CATEGORIES } from "./data/catalog";

const IS = {
  background: "rgba(255,255,255,0.04)",
  border: "1px solid var(--line-strong)",
  borderRadius: 10,
  padding: "12px 14px",
  fontSize: 14,
  color: "#fff",
  fontFamily: "inherit",
  width: "100%",
  outline: "none",
  resize: "vertical",
  lineHeight: 1.6,
};

const EXAMPLE_PROMPTS = [
  "Install 12 recessed lights with 3 dimmers and a dedicated 20A circuit for a home office",
  "200A panel upgrade with whole-home surge protection and new grounding electrode",
  "EV charger installation — Level 2 NEMA 14-50 in the garage, run from panel",
  "Replace 6 GFCI outlets in kitchen and bathrooms, add tamper-resistant covers",
  "Generator transfer switch installation — 10-circuit manual with inlet box",
  "Hot tub circuit — 240V 50A GFCI breaker, hardwired, with bonding",
  "Add 3 dedicated circuits: dishwasher, microwave, and refrigerator",
  "Whole home AFCI upgrade — replace all bedroom and living area breakers",
];

// ── Tolerant JSON reading ───────────────────────────────────────────────────
// Find the first balanced {…} or […] block starting at/after `from`, correctly
// skipping braces that appear inside strings. Returns the substring or null.
function balancedBlock(s, open, close, from = 0) {
  const start = s.indexOf(open, from);
  if (start === -1) return null;
  let depth = 0, inStr = false, esc = false;
  for (let i = start; i < s.length; i++) {
    const ch = s[i];
    if (inStr) {
      if (esc) esc = false;
      else if (ch === "\\") esc = true;
      else if (ch === '"') inStr = false;
    } else if (ch === '"') inStr = true;
    else if (ch === open) depth++;
    else if (ch === close) { depth--; if (depth === 0) return s.slice(start, i + 1); }
  }
  return null; // never closed → truncated
}

// Parse the AI reply into { services, summary, assumptions }. Tries, in order:
// a clean JSON object (scanning past any prose/citations), a bare services
// array (back-compat), then salvages a services array that was cut off.
function parseQuote(raw) {
  if (!raw || typeof raw !== "string") return null;
  const s = raw.replace(/```json|```/g, "").trim();

  // 1. Preferred — a JSON object that contains a "services" array.
  let from = s.indexOf("{");
  while (from > -1) {
    const block = balancedBlock(s, "{", "}", from);
    if (block) {
      try {
        const o = JSON.parse(block);
        if (o && Array.isArray(o.services)) {
          return {
            services: o.services,
            summary: o.summary || "",
            assumptions: Array.isArray(o.assumptions) ? o.assumptions : [],
          };
        }
      } catch { /* not it — keep scanning */ }
    }
    from = s.indexOf("{", from + 1);
  }

  // 2. Back-compat — a bare array of services, with an optional "SUMMARY:" line.
  const arr = balancedBlock(s, "[", "]");
  if (arr) {
    try {
      const a = JSON.parse(arr);
      if (Array.isArray(a)) {
        const sm = s.match(/SUMMARY:\s*([\s\S]*?)(?:\n\n|$)/i);
        return { services: a, summary: sm?.[1]?.trim() || "", assumptions: [] };
      }
    } catch { /* fall through */ }
  }

  // 3. Salvage — a services array cut off mid-item: keep the complete objects.
  const arrStart = s.indexOf("[");
  if (arrStart > -1) {
    const frag = s.slice(arrStart);
    let cut = frag.lastIndexOf("},");
    while (cut > -1) {
      try {
        const salvaged = JSON.parse(frag.slice(0, cut + 1) + "]");
        if (Array.isArray(salvaged) && salvaged.length) {
          const sm = s.match(/SUMMARY:\s*([\s\S]*?)(?:\n\n|$)/i);
          return { services: salvaged, summary: sm?.[1]?.trim() || "", assumptions: [], _salvaged: true };
        }
      } catch { /* keep cutting back */ }
      cut = frag.lastIndexOf("},", cut - 1);
    }
  }
  return null;
}

// Build the system prompt with the full service catalog so the AI knows what's available
function buildSystemPrompt() {
  const catalogSummary = CATEGORIES.map(cat =>
    `${cat.label}:\n` +
    cat.services.map(s =>
      `  - id:"${s.id}" | "${s.label}" | ${s.nec} | mat:$${s.materialCost} lab:$${s.laborCost} per ${s.unit} | variants: ${s.variants.map(v => v.label).join(", ")}`
    ).join("\n")
  ).join("\n\n");

  return `You are an expert residential electrical estimating AI for Wireway, built by a licensed electrician with NEC 2023 expertise.

Your job: analyze a job description and return a JSON object matching services from the Wireway catalog.

WIREWAY SERVICE CATALOG:
${catalogSummary}

RULES:
1. Return ONLY a single valid JSON object — no markdown fences, no preamble, no text outside the JSON.
2. Match services from the catalog by their exact "id" field.
3. Choose the most appropriate variant index (0 = first variant).
4. Set realistic quantities based on the job description.
5. If a service isn't in the catalog, skip it — only use catalog items.
6. Include NEC-required related items (e.g. GFCI with pool circuits, surge with panel upgrade).
7. Be thorough — include ALL relevant services for the described job.
8. MATERIAL SUPPLIER: set "clientBuys": true on any item where the description says the customer/client/homeowner is supplying, purchasing, or providing that material (e.g. "customer bought the fixtures", "homeowner is supplying the fan"). If they say they're supplying ALL materials, set it true on every item. Otherwise false. Labor is ALWAYS charged regardless of who supplies material.
9. QUANTITY ACCURACY: extract exact counts from the text ("six recessed lights" = qty 6, "three bedrooms each getting 2 receptacles" = qty 6). Never guess high. If no count is given, use the realistic minimum for the described scope.
10. VARIANT ACCURACY: match variants to stated specs — amperage (100A/200A/400A), wire gauge, fixture type, indoor/outdoor, finished vs unfinished walls. If the description says "200 amp panel", you MUST pick the 200A variant, not the default.
11. SCOPE DISCIPLINE: include ONLY what the job requires plus NEC-mandated companions (AFCI/GFCI protection, grounding, surge protection on services). Do NOT pad with unrelated items. An accurate lean quote beats an inflated one.
12. JOB CONTEXT: account for stated conditions that change labor — finished walls (fishing wire), crawlspace/attic access, plaster, multi-story, occupied home. Choose the variant that reflects difficulty when one exists.
13. ASSUMPTIONS: in "assumptions", list every quantity you INFERRED rather than read, every variant you chose without an explicit spec, and any missing detail the electrician should confirm before bidding (panel amperage, wire-run length, wall finish, breaker type). Keep each to a short phrase. This protects the electrician from a wrong bid. If you assumed nothing, use an empty array.

RETURN FORMAT — a single JSON object, nothing else:
{
  "services": [
    { "id": "service_id_from_catalog", "qty": 2, "variantIdx": 0, "variantLabel": "variant name", "clientBuys": false, "reason": "one sentence why this is included" }
  ],
  "summary": "2-sentence plain-English summary of the scope and what the electrician should know about this job.",
  "assumptions": ["short note on anything inferred or to confirm"]
}`;
}

export default function AIQuoteBuilder({ onApplyEstimate, onClose, initialPrompt, hourlyRate = 85 }) {
  const [prompt,    setPrompt]    = useState(initialPrompt || "");
  const [matSupplier, setMatSupplier] = useState("me"); // "me" | "client"
  const [loading,   setLoading]   = useState(false);
  const [error,     setError]     = useState("");
  const [result,    setResult]    = useState(null); // { items, summary, assumptions }
  const [selected,  setSelected]  = useState({});   // id -> bool
  const [applying,  setApplying]  = useState(false);

  const focusGold = e => e.target.style.borderColor = "rgba(var(--accent-rgb),0.4)";
  const blurGray  = e => e.target.style.borderColor = "rgba(255,255,255,0.08)";

  const analyze = async () => {
    if (!prompt.trim() || loading) return;
    setLoading(true);
    setError("");
    setResult(null);
    setSelected({});

    try {
      const { data: { session } } = await supabase.auth.getSession();
      const response = await fetch("/api/claude", {
        method: "POST",
        headers: { "Content-Type": "application/json", Authorization: `Bearer ${session?.access_token || ""}` },
        body: JSON.stringify({
          max_tokens: 3000,
          system: buildSystemPrompt(),
          messages: [{ role: "user", content: prompt }],
        }),
      });

      const data = await response.json();
      if (!response.ok) throw new Error(data?.error || "AI request failed");

      // Read every text block (not just the first) and parse tolerantly
      const raw = (data.content || []).filter(b => b.type === "text").map(b => b.text).join("");
      const parsed = parseQuote(raw);
      if (!parsed || !Array.isArray(parsed.services)) throw new Error("Couldn't read the AI's response — tap Analyze to try again.");

      // Catalog labor dollars are authored at an $85/hr base. Scale to the
      // electrician's actual rate so the preview matches the applied estimate.
      const rateScale = (Number(hourlyRate) || 85) / 85;
      // Map to full service objects (store per-unit costs so quantities stay editable)
      const items = parsed.services.map(item => {
        const service = ALL_SERVICES.find(s => s.id === item.id);
        if (!service) return null;
        const vIdx = item.variantIdx ?? 0;
        const variant = service.variants[vIdx] || service.variants[0];
        const qty = Math.max(1, Math.round(Number(item.qty) || 1));
        const unitMat = service.materialCost * variant.m;
        const unitLab = Math.round(service.laborCost * variant.m * rateScale);
        const unitHrs = service.laborHours   * variant.m;
        return {
          id:           service.id,
          label:        service.label,
          nec:          service.nec,
          qty,
          variantIdx:   vIdx,
          variantLabel: variant.label,
          unitMat, unitLab, unitHrs,
          mat:          unitMat * qty,
          lab:          unitLab * qty,
          hrs:          unitHrs * qty,
          lineTotal:    (unitMat + unitLab) * qty,
          clientBuys:   !!item.clientBuys,
          reason:       item.reason,
        };
      }).filter(Boolean);

      if (items.length === 0) throw new Error("No matching services found. Try describing the job with more detail.");

      // Select all by default
      const sel = {};
      items.forEach(i => sel[i.id] = true);
      setSelected(sel);

      setResult({ items, summary: parsed.summary, assumptions: parsed.assumptions || [], salvaged: !!parsed._salvaged });

    } catch (err) {
      setError(err.message || "Something went wrong. Try again.");
    } finally {
      setLoading(false);
    }
  };

  // Adjust a service's quantity inline and recompute its line totals
  const changeQty = (id, delta) => {
    setResult(r => {
      if (!r) return r;
      return {
        ...r,
        items: r.items.map(it => {
          if (it.id !== id) return it;
          const qty = Math.max(1, it.qty + delta);
          return { ...it, qty, mat: it.unitMat * qty, lab: it.unitLab * qty, hrs: it.unitHrs * qty, lineTotal: (it.unitMat + it.unitLab) * qty };
        }),
      };
    });
  };

  const applyToEstimate = () => {
    if (!result || applying) return;
    setApplying(true);
    const toApply = result.items.filter(i => selected[i.id]);
    const withSupplier = toApply.map(i => ({ ...i, clientBuys: matSupplier === "client" }));
    onApplyEstimate(withSupplier);
    setApplying(false);
  };

  const selectedItems  = result?.items.filter(i => selected[i.id]) || [];
  const effBuys  = (i) => matSupplier === "client";
  const totalMat = selectedItems.reduce((a, i) => a + (effBuys(i) ? 0 : i.mat), 0);
  const clientMat = selectedItems.reduce((a, i) => a + (effBuys(i) ? i.mat : 0), 0);
  const totalLab = selectedItems.reduce((a, i) => a + i.lab, 0);
  const totalHrs = selectedItems.reduce((a, i) => a + i.hrs, 0);
  const subtotal = totalMat + totalLab;

  return (
    <>
      <style>{`
        @keyframes fadeUp{from{opacity:0;transform:translateY(10px)}to{opacity:1;transform:translateY(0)}}
        @keyframes spin{to{transform:rotate(360deg)}}
        .ai-spinner{display:inline-block;width:16px;height:16px;border:2px solid rgba(var(--accent-rgb),0.2);border-top-color:var(--accent);border-radius:50%;animation:spin 0.8s linear infinite}
        .ai-item:hover{background:rgba(255,255,255,0.04)!important}
        .qty-btn{width:22px;height:22px;border-radius:6px;border:1px solid var(--line-strong);background:rgba(255,255,255,0.04);color:rgba(255,255,255,0.7);font-size:13px;font-weight:700;cursor:pointer;display:flex;align-items:center;justify-content:center;line-height:1;font-family:inherit}
        .qty-btn:hover{background:rgba(var(--accent-rgb),0.12);color:var(--accent);border-color:rgba(var(--accent-rgb),0.4)}
      `}</style>

      <div style={{ position:"fixed", inset:0, background:"rgba(0,0,0,0.82)", backdropFilter:"blur(12px)", WebkitBackdropFilter:"blur(12px)", zIndex:360, display:"flex", alignItems:"flex-start", justifyContent:"center", overflowY:"auto", padding:"20px 16px calc(40px + env(safe-area-inset-bottom,0px))" }}>
        <div style={{ width:"100%", maxWidth:640, animation:"fadeUp 0.25s ease both" }}>

          {/* Header */}
          <div style={{ display:"flex", justifyContent:"space-between", alignItems:"flex-start", marginBottom:20 }}>
            <div>
              <div style={{ display:"flex", alignItems:"center", gap:10, marginBottom:4 }}>
                <div style={{ width:32, height:32, borderRadius:8, background:"linear-gradient(135deg,rgba(var(--accent-rgb),0.25),rgba(var(--accent-rgb),0.08))", border:"1px solid rgba(var(--accent-rgb),0.3)", display:"flex", alignItems:"center", justifyContent:"center", fontSize:16 }}>⚡</div>
                <span style={{ fontFamily:"'Syne',sans-serif", fontSize:20, fontWeight:800, color:"#fff", letterSpacing:"-0.03em" }}>AI Quote Builder</span>
              </div>
              <div style={{ fontSize:12, color:"rgba(255,255,255,0.5)", marginLeft:42 }}>
                Describe the job — AI maps it to NEC services automatically
              </div>
            </div>
            <button onClick={onClose} style={{ background:"transparent", border:"none", color:"rgba(255,255,255,0.5)", fontSize:24, cursor:"pointer", padding:"0 4px", marginTop:4 }}>✕</button>
          </div>

          {/* Input area */}
          <div style={{ background:"var(--card)", border:"1px solid var(--line)", borderRadius:14, padding:"16px", marginBottom:12 }}>
            <div style={{ display:"flex", gap:6, marginBottom:10 }}>

              <button onClick={() => setMatSupplier("me")} style={{ flex:1, padding:"8px", borderRadius:7, border: matSupplier==="me" ? "1px solid rgba(var(--accent-rgb),0.5)" : "1px solid var(--line-strong)", background: matSupplier==="me" ? "rgba(var(--accent-rgb),0.12)" : "rgba(255,255,255,0.03)", color: matSupplier==="me" ? "var(--accent)" : "rgba(255,255,255,0.4)", fontSize:11, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>

                ⚡ I supply materials

              </button>

              <button onClick={() => setMatSupplier("client")} style={{ flex:1, padding:"8px", borderRadius:7, border: matSupplier==="client" ? "1px solid rgba(126,200,232,0.5)" : "1px solid var(--line-strong)", background: matSupplier==="client" ? "rgba(126,200,232,0.1)" : "rgba(255,255,255,0.03)", color: matSupplier==="client" ? "#7ec8e8" : "rgba(255,255,255,0.4)", fontSize:11, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>

                👤 Client supplies

              </button>

            </div>
            <textarea
              placeholder="Describe the electrical job in plain English...&#10;&#10;Example: Install 12 recessed lights with 3 dimmers, add a dedicated 20A circuit for a home office, and replace 4 GFCI outlets in the kitchen."
              value={prompt}
              onChange={e => setPrompt(e.target.value)}
              rows={4}
              style={IS}
              onFocus={focusGold}
              onBlur={blurGray}
              onKeyDown={e => { if (e.key === "Enter" && e.metaKey) analyze(); }}
            />

            {/* Example prompts */}
            {!prompt && (
              <div style={{ marginTop:10 }}>
                <div style={{ fontSize:10, color:"rgba(255,255,255,0.5)", textTransform:"uppercase", letterSpacing:"0.08em", marginBottom:7 }}>Try these</div>
                <div style={{ display:"flex", flexWrap:"wrap", gap:5 }}>
                  {EXAMPLE_PROMPTS.slice(0, 4).map((ex, i) => (
                    <button key={i} onClick={() => setPrompt(ex)} style={{ padding:"4px 10px", borderRadius:6, border:"1px solid var(--line-strong)", background:"var(--card)", color:"rgba(255,255,255,0.5)", fontSize:10, cursor:"pointer", fontFamily:"inherit", textAlign:"left", lineHeight:1.4, transition:"all 0.15s" }}
                      onMouseEnter={e => { e.currentTarget.style.background="rgba(255,255,255,0.07)"; e.currentTarget.style.color="#fff"; }}
                      onMouseLeave={e => { e.currentTarget.style.background="rgba(255,255,255,0.03)"; e.currentTarget.style.color="rgba(255,255,255,0.4)"; }}>
                      {ex.length > 55 ? ex.slice(0, 55) + "..." : ex}
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* Analyze button */}
            <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginTop:12 }}>
              <span style={{ fontSize:10, color:"rgba(255,255,255,0.5)" }}>⌘ + Enter to analyze</span>
              <button
                onClick={analyze}
                disabled={!prompt.trim() || loading}
                style={{ padding:"10px 24px", background: (!prompt.trim() || loading) ? "rgba(var(--accent-rgb),0.06)" : "linear-gradient(135deg,rgba(var(--accent-rgb),0.22),rgba(var(--accent-rgb),0.08))", border:`1px solid ${(!prompt.trim() || loading) ? "rgba(var(--accent-rgb),0.15)" : "rgba(var(--accent-rgb),0.4)"}`, borderRadius:9, color: (!prompt.trim() || loading) ? "rgba(var(--accent-rgb),0.3)" : "var(--accent)", fontSize:13, fontWeight:700, cursor: (!prompt.trim() || loading) ? "default" : "pointer", fontFamily:"inherit", display:"flex", alignItems:"center", gap:8, transition:"all 0.2s" }}>
                {loading ? <><span className="ai-spinner"/>Analyzing job...</> : "⚡ Analyze Job"}
              </button>
            </div>
          </div>

          {/* Error */}
          {error && (
            <div style={{ padding:"10px 14px", background:"rgba(232,126,126,0.08)", border:"1px solid rgba(232,126,126,0.2)", borderRadius:9, fontSize:12, color:"#e87e7e", marginBottom:12 }}>
              ⚠ {error}
            </div>
          )}

          {/* Results */}
          {result && (
            <div style={{ animation:"fadeUp 0.3s ease both" }}>

              {/* AI Summary */}
              {result.summary && (
                <div style={{ padding:"12px 16px", background:"rgba(var(--accent-rgb),0.06)", border:"1px solid rgba(var(--accent-rgb),0.15)", borderRadius:11, marginBottom:12 }}>
                  <div style={{ fontSize:10, color:"rgba(var(--accent-rgb),0.6)", textTransform:"uppercase", letterSpacing:"0.08em", marginBottom:5 }}>AI Analysis</div>
                  <div style={{ fontSize:13, color:"rgba(255,255,255,0.75)", lineHeight:1.6 }}>{result.summary}</div>
                </div>
              )}

              {/* Assumptions — things to confirm before bidding */}
              {result.assumptions && result.assumptions.length > 0 && (
                <div style={{ padding:"12px 16px", background:"rgba(232,184,126,0.06)", border:"1px solid rgba(232,184,126,0.22)", borderRadius:11, marginBottom:12 }}>
                  <div style={{ fontSize:10, color:"#e8b87e", textTransform:"uppercase", letterSpacing:"0.08em", marginBottom:7, fontWeight:700 }}>⚠ Assumptions — confirm before you bid</div>
                  <ul style={{ margin:0, paddingLeft:18, display:"flex", flexDirection:"column", gap:4 }}>
                    {result.assumptions.map((a, i) => (
                      <li key={i} style={{ fontSize:12, color:"rgba(255,255,255,0.65)", lineHeight:1.5 }}>{a}</li>
                    ))}
                  </ul>
                </div>
              )}

              {/* Salvage notice */}
              {result.salvaged && (
                <div style={{ padding:"9px 14px", background:"rgba(232,184,126,0.05)", border:"1px solid rgba(232,184,126,0.18)", borderRadius:9, fontSize:11, color:"rgba(232,184,126,0.85)", marginBottom:12 }}>
                  Large job — the response was trimmed for length. Tap Analyze again if anything looks missing.
                </div>
              )}

              {/* Service list */}
              <div style={{ background:"rgba(255,255,255,0.022)", border:"1px solid var(--line)", borderRadius:13, overflow:"hidden", marginBottom:12 }}>
                <div style={{ padding:"12px 16px", borderBottom:"1px solid var(--line)", display:"flex", justifyContent:"space-between", alignItems:"center" }}>
                  <div>
                    <span style={{ fontSize:13, fontWeight:700, color:"#fff" }}>{result.items.length} services identified</span>
                    <span style={{ fontSize:11, color:"rgba(255,255,255,0.5)", marginLeft:8 }}>· adjust qty or uncheck</span>
                  </div>
                  <div style={{ display:"flex", gap:8 }}>
                    <button onClick={() => { const s = {}; result.items.forEach(i => s[i.id]=true); setSelected(s); }} style={{ fontSize:10, color:"rgba(var(--accent-rgb),0.7)", background:"transparent", border:"none", cursor:"pointer", fontFamily:"inherit" }}>Select all</button>
                    <button onClick={() => setSelected({})} style={{ fontSize:10, color:"rgba(255,255,255,0.5)", background:"transparent", border:"none", cursor:"pointer", fontFamily:"inherit" }}>Clear</button>
                  </div>
                </div>

                {result.items.map((item, idx) => (
                  <div key={item.id} className="ai-item" onClick={() => setSelected(p => ({ ...p, [item.id]: !p[item.id] }))}
                    style={{ display:"flex", alignItems:"flex-start", gap:12, padding:"11px 16px", borderBottom: idx < result.items.length-1 ? "1px solid var(--line)" : "none", cursor:"pointer", background:"transparent", transition:"background 0.15s" }}>

                    {/* Checkbox */}
                    <div style={{ width:18, height:18, borderRadius:4, flexShrink:0, border: selected[item.id] ? "1px solid var(--accent)" : "1px solid rgba(255,255,255,0.2)", background: selected[item.id] ? "rgba(var(--accent-rgb),0.15)" : "transparent", display:"flex", alignItems:"center", justifyContent:"center", marginTop:1, transition:"all 0.15s" }}>
                      {selected[item.id] && <span style={{ fontSize:10, color:"var(--accent)" }}>✓</span>}
                    </div>

                    {/* Service info */}
                    <div style={{ flex:1, minWidth:0 }}>
                      <div style={{ display:"flex", alignItems:"center", gap:8, flexWrap:"wrap", marginBottom:4 }}>
                        <span style={{ fontSize:13, fontWeight:600, color: selected[item.id] ? "#fff" : "rgba(255,255,255,0.4)", transition:"color 0.15s" }}>{item.label}</span>
                        <span style={{ fontSize:10, color:"rgba(var(--accent-rgb),0.5)", fontFamily:"'DM Mono',monospace" }}>{item.nec}</span>
                        <span style={{ fontSize:10, color:"rgba(255,255,255,0.5)", background:"rgba(255,255,255,0.05)", padding:"1px 6px", borderRadius:4 }}>{item.variantLabel}</span>
                      </div>
                      {/* Quantity stepper (does not toggle the row) */}
                      <div onClick={e => e.stopPropagation()} style={{ display:"inline-flex", alignItems:"center", gap:8, marginBottom:5 }}>
                        <button className="qty-btn" onClick={() => changeQty(item.id, -1)} aria-label="decrease quantity">−</button>
                        <span style={{ fontFamily:"'DM Mono',monospace", fontSize:12, fontWeight:700, color:"rgba(255,255,255,0.85)", minWidth:28, textAlign:"center" }}>× {item.qty}</span>
                        <button className="qty-btn" onClick={() => changeQty(item.id, +1)} aria-label="increase quantity">+</button>
                      </div>
                      <div style={{ fontSize:11, color:"rgba(255,255,255,0.5)", lineHeight:1.4 }}>{item.reason}</div>
                    </div>

                    {/* Line total */}
                    <div style={{ textAlign:"right", flexShrink:0 }}>
                      <div style={{ fontFamily:"'DM Mono',monospace", fontSize:14, fontWeight:600, color: selected[item.id] ? "var(--accent)" : "rgba(255,255,255,0.25)", transition:"color 0.15s" }}>${(effBuys(item) ? item.lab : item.lineTotal).toLocaleString()}</div>
                      <div style={{ fontSize:10, color:"rgba(255,255,255,0.5)", fontFamily:"'DM Mono',monospace" }}>{item.hrs.toFixed(1)}h</div>
                    </div>
                  </div>
                ))}
              </div>

              {/* Totals */}
              <div style={{ background:"linear-gradient(135deg,rgba(var(--accent-rgb),0.08),rgba(255,255,255,0.02))", border:"1px solid rgba(var(--accent-rgb),0.2)", borderRadius:12, padding:"14px 16px", marginBottom:14 }}>
                <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr 1fr 1fr", gap:12 }}>
                  {[
                    { label:"Materials",  val:`$${totalMat.toLocaleString()}` },
                    { label:"Labor",      val:`$${totalLab.toLocaleString()}` },
                    { label:"Est. hours", val:`${totalHrs.toFixed(1)}h` },
                    { label:"Subtotal",   val:`$${subtotal.toLocaleString()}` },
                  ].map(s => (
                    <div key={s.label} style={{ textAlign:"center" }}>
                      <div style={{ fontSize:10, color:"rgba(255,255,255,0.5)", textTransform:"uppercase", letterSpacing:"0.08em", marginBottom:4 }}>{s.label}</div>
                      <div style={{ fontFamily:"'DM Mono',monospace", fontSize:16, fontWeight:600, color:"var(--accent)" }}>{s.val}</div>
                    </div>
                  ))}
                </div>
                {clientMat > 0 && (
                  <div style={{ marginTop:10, fontSize:11, color:"#7ec8e8", background:"rgba(126,200,232,0.07)", border:"1px solid rgba(126,200,232,0.2)", borderRadius:7, padding:"7px 10px", textAlign:"center" }}>
                    👤 Client supplies ${clientMat.toLocaleString()} in materials — not included in your price
                  </div>
                )}
                <div style={{ fontSize:10, color:"rgba(255,255,255,0.5)", textAlign:"center", marginTop:8 }}>
                  Subtotal before markup · add your margin in the estimate
                </div>
              </div>

              {/* Actions */}
              <div style={{ display:"flex", gap:8 }}>
                <button onClick={applyToEstimate} disabled={selectedItems.length === 0}
                  style={{ flex:1, padding:"14px", background: selectedItems.length > 0 ? "linear-gradient(135deg,rgba(var(--accent-rgb),0.22),rgba(var(--accent-rgb),0.08))" : "rgba(255,255,255,0.04)", border:`1px solid ${selectedItems.length > 0 ? "rgba(var(--accent-rgb),0.4)" : "rgba(255,255,255,0.08)"}`, borderRadius:11, color: selectedItems.length > 0 ? "var(--accent)" : "rgba(255,255,255,0.25)", fontSize:14, fontWeight:700, cursor: selectedItems.length > 0 ? "pointer" : "default", fontFamily:"inherit", transition:"all 0.2s" }}>
                  Apply {selectedItems.length} service{selectedItems.length !== 1 ? "s" : ""} to Estimate →
                </button>
                <button onClick={() => { setResult(null); setPrompt(""); setSelected({}); }}
                  style={{ padding:"14px 18px", background:"transparent", border:"1px solid var(--line-strong)", borderRadius:11, color:"rgba(255,255,255,0.5)", fontSize:13, fontWeight:600, cursor:"pointer", fontFamily:"inherit" }}>
                  Start over
                </button>
              </div>

              <div style={{ textAlign:"center", fontSize:10, color:"rgba(255,255,255,0.5)", marginTop:10 }}>
                Powered by Claude AI · Review all items before sending to client · Always verify NEC compliance
              </div>
            </div>
          )}
        </div>
      </div>
    </>
  );
}
