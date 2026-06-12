/* eslint-disable react-hooks/exhaustive-deps */
// src/MaterialsListView.jsx
// Material Pull List — AI builds an itemized shopping list from the quote's
// line items, grouped by service, with realistic price estimates, store
// search links, and a comparison against the quote's material budget.

import { useState, useEffect } from "react";

const hdLink = (q) => `https://www.homedepot.com/s/${encodeURIComponent(q)}`;
const lwLink = (q) => `https://www.lowes.com/search?searchTerm=${encodeURIComponent(q)}`;

export default function MaterialsListView({ activeItems, totMat, jobName, onClose }) {
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState("");
  const [list,    setList]    = useState(null);   // { sections:[{service, items:[{name,spec,qty,unit,price}]}], notes }
  const [checked, setChecked] = useState({});

  useEffect(() => { generate(); }, []);

  const generate = async () => {
    setLoading(true); setError("");
    try {
      const jobLines = activeItems.map(i =>
        `- ${i.label}${i.variantLabel && i.variantLabel !== "Standard" ? ` (${i.variantLabel})` : ""} × ${i.qty}${i.cBuys ? " [client supplies materials]" : ""}`
      ).join("\n");

      const response = await fetch("/api/claude", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          max_tokens: 2500,
          system: `You are a master electrician with 30 years of residential experience writing a material pull list for a supply run. You know exactly what each job needs, including the consumables everyone forgets (staples, wire nuts, connectors, straps, tape).

RULES:
1. Group materials BY SERVICE — one section per service line given.
2. For each material: name (short, searchable — what you'd type into Home Depot's search), spec (gauge/amperage/size detail), qty (number), unit (ea, ft, box, roll), price (realistic mid-range US big-box unit price in dollars, number only).
3. Wire quantities in feet with sensible slack (10-15% extra). Round to purchasable amounts (wire sold in 25/50/100/250ft).
4. Skip materials for services marked [client supplies materials] but note them in "notes".
5. Consolidate shared consumables (wire nuts, staples, tape) into a final section called "Consumables & Rough-In".
6. In "notes": 1-3 sentences — anything client-supplied, items better bought at an electrical supply house than big-box, and any bulk-buy savings.

Respond ONLY with JSON, no markdown fences:
{"sections":[{"service":"...","items":[{"name":"...","spec":"...","qty":1,"unit":"ea","price":0.00}]}],"notes":"..."}`,
          messages: [{ role: "user", content: `Job: ${jobName || "Residential electrical"}\nServices:\n${jobLines}` }],
        }),
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data?.error || "AI request failed");
      const raw = (data.content?.[0]?.text || "").replace(/```json|```/g, "").trim();
      const parsed = JSON.parse(raw.slice(raw.indexOf("{"), raw.lastIndexOf("}") + 1));
      if (!parsed.sections) throw new Error("Unexpected response shape");
      setList(parsed);
    } catch (err) {
      setError(err.message || "Couldn't build the list — try again");
    }
    setLoading(false);
  };

  const allItems   = list ? list.sections.flatMap(s => s.items) : [];
  const estTotal   = allItems.reduce((a, i) => a + (i.price || 0) * (i.qty || 1), 0);
  const diff       = totMat - estTotal;
  const checkedCount = Object.values(checked).filter(Boolean).length;

  const copyList = async () => {
    const text = list.sections.map(s =>
      `${s.service.toUpperCase()}\n` + s.items.map(i =>
        `  ☐ ${i.qty} ${i.unit} — ${i.name}${i.spec ? ` (${i.spec})` : ""} — ~$${((i.price||0)*(i.qty||1)).toFixed(2)}`
      ).join("\n")
    ).join("\n\n") + `\n\nESTIMATED TOTAL: $${estTotal.toFixed(2)}`;
    try { await navigator.clipboard.writeText(text); alert("Pull list copied"); }
    catch { window.prompt("Copy your pull list:", text); }
  };

  const card = { background:"var(--card)", border:"1px solid var(--line)", borderRadius:12 };

  return (
    <div style={{ position:"fixed", inset:0, zIndex:350, background:"var(--bg-scene)", overflowY:"auto", fontFamily:"'DM Sans',sans-serif", color:"#fff" }}>
      {/* Header */}
      <div style={{ borderBottom:"1px solid var(--line)", background:"rgba(10,10,12,0.92)", backdropFilter:"blur(20px)", WebkitBackdropFilter:"blur(20px)", position:"sticky", top:0, zIndex:10, padding:"0 20px" }}>
        <div style={{ maxWidth:680, margin:"0 auto", height:54, display:"flex", alignItems:"center", justifyContent:"space-between" }}>
          <span style={{ fontFamily:"'Syne',sans-serif", fontSize:16, fontWeight:800 }}>🧾 Material Pull List</span>
          <div style={{ display:"flex", gap:6 }}>
            {list && (
              <button onClick={copyList} style={{ padding:"6px 12px", borderRadius:7, border:"1px solid rgba(var(--accent-rgb),0.35)", background:"rgba(var(--accent-rgb),0.08)", color:"var(--accent)", fontSize:11, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>Copy List</button>
            )}
            <button onClick={onClose} style={{ padding:"6px 12px", borderRadius:7, border:"1px solid var(--line-strong)", background:"transparent", color:"rgba(255,255,255,0.45)", fontSize:11, fontWeight:600, cursor:"pointer", fontFamily:"inherit" }}>✕ Close</button>
          </div>
        </div>
      </div>

      <div style={{ maxWidth:680, margin:"0 auto", padding:"20px 20px 80px" }}>
        {loading && (
          <div style={{ textAlign:"center", padding:"70px 20px" }}>
            <div style={{ display:"inline-block", width:22, height:22, border:"2px solid rgba(var(--accent-rgb),0.2)", borderTopColor:"var(--accent)", borderRadius:"50%", animation:"spin 0.8s linear infinite" }} />
            <style>{`@keyframes spin{to{transform:rotate(360deg)}}`}</style>
            <div style={{ fontSize:13, color:"rgba(255,255,255,0.4)", marginTop:14 }}>Building your pull list from {activeItems.length} service{activeItems.length !== 1 ? "s" : ""}...</div>
          </div>
        )}

        {error && !loading && (
          <div style={{ ...card, padding:"20px", textAlign:"center" }}>
            <div style={{ fontSize:12, color:"#e87e7e", marginBottom:12 }}>⚠ {error}</div>
            <button onClick={generate} style={{ padding:"9px 18px", borderRadius:8, background:"rgba(var(--accent-rgb),0.1)", border:"1px solid rgba(var(--accent-rgb),0.3)", color:"var(--accent)", fontSize:12, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>Try Again</button>
          </div>
        )}

        {list && !loading && (
          <>
            {/* Budget comparison */}
            <div style={{ ...card, padding:"14px 16px", marginBottom:14, border: diff >= 0 ? "1px solid rgba(100,220,130,0.25)" : "1px solid rgba(232,126,126,0.3)" }}>
              <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr 1fr", gap:10, textAlign:"center" }}>
                <div>
                  <div style={{ fontSize:9, color:"rgba(255,255,255,0.35)", textTransform:"uppercase", letterSpacing:"0.08em", marginBottom:4 }}>Quote budget</div>
                  <div style={{ fontFamily:"'DM Mono',monospace", fontSize:16, fontWeight:600 }}>${Math.round(totMat).toLocaleString()}</div>
                </div>
                <div>
                  <div style={{ fontSize:9, color:"rgba(255,255,255,0.35)", textTransform:"uppercase", letterSpacing:"0.08em", marginBottom:4 }}>Est. shelf cost</div>
                  <div style={{ fontFamily:"'DM Mono',monospace", fontSize:16, fontWeight:600, color:"var(--accent)" }}>${Math.round(estTotal).toLocaleString()}</div>
                </div>
                <div>
                  <div style={{ fontSize:9, color:"rgba(255,255,255,0.35)", textTransform:"uppercase", letterSpacing:"0.08em", marginBottom:4 }}>{diff >= 0 ? "Cushion" : "Over budget"}</div>
                  <div style={{ fontFamily:"'DM Mono',monospace", fontSize:16, fontWeight:600, color: diff >= 0 ? "#7dcea0" : "#e87e7e" }}>{diff >= 0 ? "+" : "−"}${Math.abs(Math.round(diff)).toLocaleString()}</div>
                </div>
              </div>
            </div>

            {/* Progress while shopping */}
            <div style={{ fontSize:10.5, color:"rgba(255,255,255,0.3)", marginBottom:12, textAlign:"center" }}>
              Tap items as you load the cart — {checkedCount}/{allItems.length} picked
            </div>

            {/* Sections */}
            {list.sections.map((sec, si) => (
              <div key={si} style={{ marginBottom:16 }}>
                <div style={{ fontSize:10, color:"var(--accent)", textTransform:"uppercase", letterSpacing:"0.1em", fontWeight:700, marginBottom:7 }}>{sec.service}</div>
                {sec.items.map((item, ii) => {
                  const key = `${si}-${ii}`;
                  const done = checked[key];
                  return (
                    <div key={key} style={{ ...card, display:"flex", alignItems:"center", gap:11, padding:"10px 13px", marginBottom:5, opacity: done ? 0.45 : 1, transition:"opacity 0.15s" }}>
                      <div onClick={() => setChecked(p => ({ ...p, [key]: !p[key] }))}
                        style={{ width:20, height:20, borderRadius:6, flexShrink:0, cursor:"pointer", display:"flex", alignItems:"center", justifyContent:"center", fontSize:12,
                          border: done ? "1px solid rgba(100,220,130,0.5)" : "1px solid rgba(255,255,255,0.2)",
                          background: done ? "rgba(100,220,130,0.15)" : "transparent", color:"#7dcea0" }}>
                        {done ? "✓" : ""}
                      </div>
                      <div style={{ flex:1, minWidth:0 }}>
                        <div style={{ fontSize:13, fontWeight:600, textDecoration: done ? "line-through" : "none" }}>{item.name}</div>
                        <div style={{ fontSize:10, color:"rgba(255,255,255,0.35)", fontFamily:"'DM Mono',monospace" }}>
                          {item.qty} {item.unit}{item.spec ? ` · ${item.spec}` : ""} · ~${(item.price || 0).toFixed(2)}/{item.unit}
                        </div>
                        <div style={{ display:"flex", gap:8, marginTop:4 }}>
                          <a href={hdLink(item.name)} target="_blank" rel="noreferrer" style={{ fontSize:9.5, color:"#e8946a", textDecoration:"none", fontWeight:700 }}>Home Depot ↗</a>
                          <a href={lwLink(item.name)} target="_blank" rel="noreferrer" style={{ fontSize:9.5, color:"#7eb8e8", textDecoration:"none", fontWeight:700 }}>Lowe's ↗</a>
                        </div>
                      </div>
                      <div style={{ fontFamily:"'DM Mono',monospace", fontSize:13, fontWeight:600, color:"var(--accent)", flexShrink:0 }}>
                        ${((item.price || 0) * (item.qty || 1)).toFixed(2)}
                      </div>
                    </div>
                  );
                })}
              </div>
            ))}

            {/* Total */}
            <div style={{ ...card, display:"flex", justifyContent:"space-between", alignItems:"center", padding:"14px 16px", border:"1px solid rgba(var(--accent-rgb),0.3)" }}>
              <span style={{ fontSize:13, fontWeight:700 }}>Estimated shelf total</span>
              <span style={{ fontFamily:"'DM Mono',monospace", fontSize:18, fontWeight:700, color:"var(--accent)" }}>${estTotal.toFixed(2)}</span>
            </div>

            {list.notes && (
              <div style={{ fontSize:11, color:"rgba(255,255,255,0.4)", lineHeight:1.7, marginTop:12, padding:"10px 13px", background:"rgba(var(--accent-rgb),0.04)", border:"1px solid rgba(var(--accent-rgb),0.12)", borderRadius:9 }}>
                💡 {list.notes}
              </div>
            )}

            <div style={{ fontSize:9, color:"rgba(255,255,255,0.2)", textAlign:"center", marginTop:14, lineHeight:1.6 }}>
              Prices are AI estimates of typical big-box shelf pricing — verify before purchase.<br/>Supply houses (CED, City Electric, Graybar) often beat big-box on wire and breakers.
            </div>
          </>
        )}
      </div>
    </div>
  );
}
