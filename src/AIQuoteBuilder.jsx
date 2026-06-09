// src/AIQuoteBuilder.jsx
// AI Quote Builder — describe a job in plain English, get a full estimate
// Uses the Anthropic API to identify services, map them to the catalog,
// calculate labor/materials, and populate the estimate automatically.

import { useState } from "react";
import { ALL_SERVICES, CATEGORIES } from "./data/catalog";

const IS = {
  background: "rgba(255,255,255,0.04)",
  border: "1px solid rgba(255,255,255,0.08)",
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

// Build the system prompt with the full service catalog so the AI knows what's available
function buildSystemPrompt() {
  const catalogSummary = CATEGORIES.map(cat =>
    `${cat.label}:\n` +
    cat.services.map(s =>
      `  - id:"${s.id}" | "${s.label}" | ${s.nec} | mat:$${s.materialCost} lab:$${s.laborCost} per ${s.unit} | variants: ${s.variants.map(v => v.label).join(", ")}`
    ).join("\n")
  ).join("\n\n");

  return `You are an expert residential electrical estimating AI for Wireway, built by a licensed electrician with NEC 2023 expertise.

Your job: analyze a job description and return a JSON array of matched services from the Wireway catalog.

WIREWAY SERVICE CATALOG:
${catalogSummary}

RULES:
1. Return ONLY valid JSON — no markdown, no explanation, no preamble
2. Match services from the catalog by their exact "id" field
3. Choose the most appropriate variant index (0 = first variant)
4. Set realistic quantities based on the job description
5. If a service isn't in the catalog, skip it — only use catalog items
6. Include NEC-required related items (e.g. GFCI with pool circuits, surge with panel upgrade)
7. Be thorough — include ALL relevant services for the described job

RETURN FORMAT (JSON array):
[
  {
    "id": "service_id_from_catalog",
    "qty": 2,
    "variantIdx": 0,
    "variantLabel": "variant name",
    "reason": "one sentence why this is included"
  }
]

After the JSON array, on a new line starting with "SUMMARY:", write a 2-sentence plain-English summary of the scope and what the electrician should know about this job.`;
}

export default function AIQuoteBuilder({ onApplyEstimate, onClose }) {
  const [prompt,    setPrompt]    = useState("");
  const [loading,   setLoading]   = useState(false);
  const [error,     setError]     = useState("");
  const [result,    setResult]    = useState(null); // { items, summary, total }
  const [selected,  setSelected]  = useState({});   // id -> bool
  const [applying,  setApplying]  = useState(false);

  const focusGold = e => e.target.style.borderColor = "rgba(232,201,122,0.4)";
  const blurGray  = e => e.target.style.borderColor = "rgba(255,255,255,0.08)";

  const analyze = async () => {
    if (!prompt.trim() || loading) return;
    setLoading(true);
    setError("");
    setResult(null);
    setSelected({});

    try {
      const response = await fetch("https://api.anthropic.com/v1/messages", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          model: "claude-sonnet-4-20250514",
          max_tokens: 1000,
          system: buildSystemPrompt(),
          messages: [{ role: "user", content: prompt }],
        }),
      });

      const data = await response.json();
      const raw = data.content?.[0]?.text || "";

      // Extract JSON array
      const jsonMatch = raw.match(/\[[\s\S]*?\]/);
      if (!jsonMatch) throw new Error("Could not parse AI response");

      const parsed = JSON.parse(jsonMatch[0]);

      // Extract summary
      const summaryMatch = raw.match(/SUMMARY:\s*([\s\S]*?)(?:\n\n|$)/i);
      const summary = summaryMatch?.[1]?.trim() || "";

      // Map to full service objects
      const items = parsed.map(item => {
        const service = ALL_SERVICES.find(s => s.id === item.id);
        if (!service) return null;
        const variant = service.variants[item.variantIdx ?? 0] || service.variants[0];
        const qty = Math.max(1, Math.round(item.qty));
        const mat = service.materialCost * variant.m * qty;
        const lab = service.laborCost    * variant.m * qty;
        const hrs = service.laborHours   * variant.m * qty;
        return {
          id:           service.id,
          label:        service.label,
          nec:          service.nec,
          qty,
          variantIdx:   item.variantIdx ?? 0,
          variantLabel: variant.label,
          mat,
          lab,
          hrs,
          lineTotal:    mat + lab,
          reason:       item.reason,
        };
      }).filter(Boolean);

      if (items.length === 0) throw new Error("No matching services found. Try describing the job with more detail.");

      // Select all by default
      const sel = {};
      items.forEach(i => sel[i.id] = true);
      setSelected(sel);

      setResult({ items, summary });

    } catch (err) {
      setError(err.message || "Something went wrong. Try again.");
    } finally {
      setLoading(false);
    }
  };

  const applyToEstimate = () => {
    if (!result || applying) return;
    setApplying(true);
    const toApply = result.items.filter(i => selected[i.id]);
    onApplyEstimate(toApply);
    setApplying(false);
  };

  const selectedItems  = result?.items.filter(i => selected[i.id]) || [];
  const totalMat = selectedItems.reduce((a, i) => a + i.mat, 0);
  const totalLab = selectedItems.reduce((a, i) => a + i.lab, 0);
  const totalHrs = selectedItems.reduce((a, i) => a + i.hrs, 0);
  const subtotal = totalMat + totalLab;

  return (
    <>
      <style>{`
        @keyframes fadeUp{from{opacity:0;transform:translateY(10px)}to{opacity:1;transform:translateY(0)}}
        @keyframes spin{to{transform:rotate(360deg)}}
        .ai-spinner{display:inline-block;width:16px;height:16px;border:2px solid rgba(232,201,122,0.2);border-top-color:#e8c97a;border-radius:50%;animation:spin 0.8s linear infinite}
        .ai-item:hover{background:rgba(255,255,255,0.04)!important}
      `}</style>

      <div style={{ position:"fixed", inset:0, background:"rgba(0,0,0,0.82)", backdropFilter:"blur(12px)", zIndex:200, display:"flex", alignItems:"flex-start", justifyContent:"center", overflowY:"auto", padding:"20px 16px 40px" }}>
        <div style={{ width:"100%", maxWidth:640, animation:"fadeUp 0.25s ease both" }}>

          {/* Header */}
          <div style={{ display:"flex", justifyContent:"space-between", alignItems:"flex-start", marginBottom:20 }}>
            <div>
              <div style={{ display:"flex", alignItems:"center", gap:10, marginBottom:4 }}>
                <div style={{ width:32, height:32, borderRadius:8, background:"linear-gradient(135deg,rgba(232,201,122,0.25),rgba(232,201,122,0.08))", border:"1px solid rgba(232,201,122,0.3)", display:"flex", alignItems:"center", justifyContent:"center", fontSize:16 }}>⚡</div>
                <span style={{ fontFamily:"'Syne',sans-serif", fontSize:20, fontWeight:800, color:"#fff", letterSpacing:"-0.03em" }}>AI Quote Builder</span>
              </div>
              <div style={{ fontSize:12, color:"rgba(255,255,255,0.4)", marginLeft:42 }}>
                Describe the job — AI maps it to NEC services automatically
              </div>
            </div>
            <button onClick={onClose} style={{ background:"transparent", border:"none", color:"rgba(255,255,255,0.35)", fontSize:24, cursor:"pointer", padding:"0 4px", marginTop:4 }}>✕</button>
          </div>

          {/* Input area */}
          <div style={{ background:"rgba(255,255,255,0.025)", border:"1px solid rgba(255,255,255,0.07)", borderRadius:14, padding:"16px", marginBottom:12 }}>
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
                <div style={{ fontSize:10, color:"rgba(255,255,255,0.25)", textTransform:"uppercase", letterSpacing:"0.08em", marginBottom:7 }}>Try these</div>
                <div style={{ display:"flex", flexWrap:"wrap", gap:5 }}>
                  {EXAMPLE_PROMPTS.slice(0, 4).map((ex, i) => (
                    <button key={i} onClick={() => setPrompt(ex)} style={{ padding:"4px 10px", borderRadius:6, border:"1px solid rgba(255,255,255,0.08)", background:"rgba(255,255,255,0.03)", color:"rgba(255,255,255,0.4)", fontSize:10, cursor:"pointer", fontFamily:"inherit", textAlign:"left", lineHeight:1.4, transition:"all 0.15s" }}
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
              <span style={{ fontSize:10, color:"rgba(255,255,255,0.2)" }}>⌘ + Enter to analyze</span>
              <button
                onClick={analyze}
                disabled={!prompt.trim() || loading}
                style={{ padding:"10px 24px", background: (!prompt.trim() || loading) ? "rgba(232,201,122,0.06)" : "linear-gradient(135deg,rgba(232,201,122,0.22),rgba(232,201,122,0.08))", border:`1px solid ${(!prompt.trim() || loading) ? "rgba(232,201,122,0.15)" : "rgba(232,201,122,0.4)"}`, borderRadius:9, color: (!prompt.trim() || loading) ? "rgba(232,201,122,0.3)" : "#e8c97a", fontSize:13, fontWeight:700, cursor: (!prompt.trim() || loading) ? "default" : "pointer", fontFamily:"inherit", display:"flex", alignItems:"center", gap:8, transition:"all 0.2s" }}>
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
                <div style={{ padding:"12px 16px", background:"rgba(232,201,122,0.06)", border:"1px solid rgba(232,201,122,0.15)", borderRadius:11, marginBottom:12 }}>
                  <div style={{ fontSize:10, color:"rgba(232,201,122,0.6)", textTransform:"uppercase", letterSpacing:"0.08em", marginBottom:5 }}>AI Analysis</div>
                  <div style={{ fontSize:13, color:"rgba(255,255,255,0.75)", lineHeight:1.6 }}>{result.summary}</div>
                </div>
              )}

              {/* Service list */}
              <div style={{ background:"rgba(255,255,255,0.022)", border:"1px solid rgba(255,255,255,0.065)", borderRadius:13, overflow:"hidden", marginBottom:12 }}>
                <div style={{ padding:"12px 16px", borderBottom:"1px solid rgba(255,255,255,0.06)", display:"flex", justifyContent:"space-between", alignItems:"center" }}>
                  <div>
                    <span style={{ fontSize:13, fontWeight:700, color:"#fff" }}>{result.items.length} services identified</span>
                    <span style={{ fontSize:11, color:"rgba(255,255,255,0.35)", marginLeft:8 }}>· uncheck to remove</span>
                  </div>
                  <div style={{ display:"flex", gap:8 }}>
                    <button onClick={() => { const s = {}; result.items.forEach(i => s[i.id]=true); setSelected(s); }} style={{ fontSize:10, color:"rgba(232,201,122,0.7)", background:"transparent", border:"none", cursor:"pointer", fontFamily:"inherit" }}>Select all</button>
                    <button onClick={() => setSelected({})} style={{ fontSize:10, color:"rgba(255,255,255,0.35)", background:"transparent", border:"none", cursor:"pointer", fontFamily:"inherit" }}>Clear</button>
                  </div>
                </div>

                {result.items.map((item, idx) => (
                  <div key={item.id} className="ai-item" onClick={() => setSelected(p => ({ ...p, [item.id]: !p[item.id] }))}
                    style={{ display:"flex", alignItems:"flex-start", gap:12, padding:"11px 16px", borderBottom: idx < result.items.length-1 ? "1px solid rgba(255,255,255,0.04)" : "none", cursor:"pointer", background:"transparent", transition:"background 0.15s" }}>

                    {/* Checkbox */}
                    <div style={{ width:18, height:18, borderRadius:4, flexShrink:0, border: selected[item.id] ? "1px solid #e8c97a" : "1px solid rgba(255,255,255,0.2)", background: selected[item.id] ? "rgba(232,201,122,0.15)" : "transparent", display:"flex", alignItems:"center", justifyContent:"center", marginTop:1, transition:"all 0.15s" }}>
                      {selected[item.id] && <span style={{ fontSize:10, color:"#e8c97a" }}>✓</span>}
                    </div>

                    {/* Service info */}
                    <div style={{ flex:1, minWidth:0 }}>
                      <div style={{ display:"flex", alignItems:"center", gap:8, flexWrap:"wrap", marginBottom:2 }}>
                        <span style={{ fontSize:13, fontWeight:600, color: selected[item.id] ? "#fff" : "rgba(255,255,255,0.4)", transition:"color 0.15s" }}>{item.label}</span>
                        <span style={{ fontSize:9, color:"rgba(232,201,122,0.5)", fontFamily:"'DM Mono',monospace" }}>{item.nec}</span>
                        <span style={{ fontSize:10, color:"rgba(255,255,255,0.3)", background:"rgba(255,255,255,0.05)", padding:"1px 6px", borderRadius:4 }}>{item.variantLabel}</span>
                        <span style={{ fontSize:10, color:"rgba(255,255,255,0.3)" }}>× {item.qty}</span>
                      </div>
                      <div style={{ fontSize:11, color:"rgba(255,255,255,0.35)", lineHeight:1.4 }}>{item.reason}</div>
                    </div>

                    {/* Line total */}
                    <div style={{ textAlign:"right", flexShrink:0 }}>
                      <div style={{ fontFamily:"'DM Mono',monospace", fontSize:14, fontWeight:600, color: selected[item.id] ? "#e8c97a" : "rgba(255,255,255,0.25)", transition:"color 0.15s" }}>${item.lineTotal.toLocaleString()}</div>
                      <div style={{ fontSize:9, color:"rgba(255,255,255,0.25)", fontFamily:"'DM Mono',monospace" }}>{item.hrs.toFixed(1)}h</div>
                    </div>
                  </div>
                ))}
              </div>

              {/* Totals */}
              <div style={{ background:"linear-gradient(135deg,rgba(232,201,122,0.08),rgba(255,255,255,0.02))", border:"1px solid rgba(232,201,122,0.2)", borderRadius:12, padding:"14px 16px", marginBottom:14 }}>
                <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr 1fr 1fr", gap:12 }}>
                  {[
                    { label:"Materials",  val:`$${totalMat.toLocaleString()}` },
                    { label:"Labor",      val:`$${totalLab.toLocaleString()}` },
                    { label:"Est. hours", val:`${totalHrs.toFixed(1)}h` },
                    { label:"Subtotal",   val:`$${subtotal.toLocaleString()}` },
                  ].map(s => (
                    <div key={s.label} style={{ textAlign:"center" }}>
                      <div style={{ fontSize:9, color:"rgba(255,255,255,0.35)", textTransform:"uppercase", letterSpacing:"0.08em", marginBottom:4 }}>{s.label}</div>
                      <div style={{ fontFamily:"'DM Mono',monospace", fontSize:16, fontWeight:600, color:"#e8c97a" }}>{s.val}</div>
                    </div>
                  ))}
                </div>
                <div style={{ fontSize:10, color:"rgba(255,255,255,0.25)", textAlign:"center", marginTop:8 }}>
                  Subtotal before markup · add your margin in the estimate
                </div>
              </div>

              {/* Actions */}
              <div style={{ display:"flex", gap:8 }}>
                <button onClick={applyToEstimate} disabled={selectedItems.length === 0}
                  style={{ flex:1, padding:"14px", background: selectedItems.length > 0 ? "linear-gradient(135deg,rgba(232,201,122,0.22),rgba(232,201,122,0.08))" : "rgba(255,255,255,0.04)", border:`1px solid ${selectedItems.length > 0 ? "rgba(232,201,122,0.4)" : "rgba(255,255,255,0.08)"}`, borderRadius:11, color: selectedItems.length > 0 ? "#e8c97a" : "rgba(255,255,255,0.25)", fontSize:14, fontWeight:700, cursor: selectedItems.length > 0 ? "pointer" : "default", fontFamily:"inherit", transition:"all 0.2s" }}>
                  Apply {selectedItems.length} service{selectedItems.length !== 1 ? "s" : ""} to Estimate →
                </button>
                <button onClick={() => { setResult(null); setPrompt(""); setSelected({}); }}
                  style={{ padding:"14px 18px", background:"transparent", border:"1px solid rgba(255,255,255,0.08)", borderRadius:11, color:"rgba(255,255,255,0.4)", fontSize:13, fontWeight:600, cursor:"pointer", fontFamily:"inherit" }}>
                  Start over
                </button>
              </div>

              <div style={{ textAlign:"center", fontSize:10, color:"rgba(255,255,255,0.18)", marginTop:10 }}>
                Powered by Claude AI · Review all items before sending to client · Always verify NEC compliance
              </div>
            </div>
          )}
        </div>
      </div>
    </>
  );
}
