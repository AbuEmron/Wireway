/* eslint-disable react-hooks/exhaustive-deps */
// src/MaterialsListView.jsx
// Material Pull List — AI builds an itemized shopping list from the quote's
// line items, grouped by service, with realistic price estimates, store
// search links, and a comparison against the quote's material budget.

import { useState, useEffect } from "react";
import { supabase } from "./lib/supabase";

// Cache the generated list per quote so leaving the app (store links, tab switches)
// never loses it — coming back restores the same list and your shopping checkmarks.
const PULL_CACHE_KEY = "wireway_pulllist_v1";
function quoteSig(activeItems) {
  return JSON.stringify(activeItems.map(i => [i.id || i.label, i.qty, i.variantLabel, !!i.cBuys]));
}
function loadPullCache(sig) {
  try {
    const raw = window.localStorage.getItem(PULL_CACHE_KEY);
    if (!raw) return null;
    const c = JSON.parse(raw);
    return c && c.sig === sig ? c : null;
  } catch { return null; }
}
function savePullCache(sig, list, checked) {
  try { window.localStorage.setItem(PULL_CACHE_KEY, JSON.stringify({ sig, list, checked })); } catch { /* ignore */ }
}

// Parse the AI's JSON, recovering gracefully if the response was cut off mid-list.
function parsePullList(raw) {
  const s = raw.slice(raw.indexOf("{"));
  // 1. Clean parse
  try { return JSON.parse(s.slice(0, s.lastIndexOf("}") + 1)); } catch { /* try salvage */ }
  // 2. Truncated mid-items: cut back to the last complete item and close the structure
  let cut = s.lastIndexOf("},");
  while (cut > -1) {
    try {
      const fixed = JSON.parse(s.slice(0, cut + 1) + "]}]}");
      fixed._salvaged = true;
      return fixed;
    } catch { cut = s.lastIndexOf("},", cut - 1); }
  }
  return null;
}

const hdLink = (q) => `https://www.homedepot.com/s/${encodeURIComponent(q)}`;
const lwLink = (q) => `https://www.lowes.com/search?searchTerm=${encodeURIComponent(q)}`;

export default function MaterialsListView({ activeItems, totMat, jobName, onClose }) {
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState("");
  const [list,    setList]    = useState(null);   // { sections:[{service, items:[{name,spec,qty,unit,price}]}], notes }
  const [checked, setChecked] = useState({});

  const sig = quoteSig(activeItems);

  useEffect(() => {
    const cached = loadPullCache(sig);
    if (cached) {
      setList(cached.list);
      setChecked(cached.checked || {});
      setLoading(false);
    } else {
      generate();
    }
  }, []);

  // Persist checkmarks as you shop
  useEffect(() => {
    if (list) savePullCache(sig, list, checked);
  }, [list, checked]);

  const generate = async () => {
    setLoading(true); setError("");
    try {
      const jobLines = activeItems.map(i =>
        `- ${i.label}${i.variantLabel && i.variantLabel !== "Standard" ? ` (${i.variantLabel})` : ""} × ${i.qty}${i.cBuys ? " [client supplies materials]" : ""}`
      ).join("\n");

      const { data: { session } } = await supabase.auth.getSession();
      const response = await fetch("/api/claude", {
        method: "POST",
        headers: { "Content-Type": "application/json", Authorization: `Bearer ${session?.access_token || ""}` },
        body: JSON.stringify({
          max_tokens: 4096,
          web_search: true,
          system: `You are a master electrician with 30 years of residential experience writing a material pull list for a supply run. You know exactly what each job needs, including the consumables everyone forgets (staples, wire nuts, connectors, straps, tape).

RULES:
1. Group materials BY SERVICE — one section per service line given.
2. For each material: name (short, searchable — what you'd type into Home Depot's search), spec (gauge/amperage/size detail), qty (number), unit (ea, ft, box, roll), price (US big-box unit price in dollars, number only).
2b. MANDATORY LIVE PRICING AT BOTH STORES — your memorized prices for copper wire and cable are YEARS out of date and 40-60% TOO LOW. NEVER price wire, cable, panels, or breakers from memory. Before writing your answer you MUST web-search CURRENT prices at BOTH Home Depot AND Lowe's for: (a) EVERY wire/cable coil or spool — NM-B/Romex, THHN, MC, UF (search like "Home Depot 250 ft 12/2 NM-B price" AND "Lowe's 250 ft 12/2 NM-B price"); (b) every panel/load center; (c) every breaker, especially AFCI/GFCI/dual-function; (d) EV chargers, disconnects, and any item worth roughly $25 or more. Prioritize the most expensive items first if the list is very long. For each searched item set "hd" (Home Depot price) and "lw" (Lowe's price) when found, set "price" to the LOWEST live price you found, and set "live": true. Only commodity smalls (wire nuts, staples, straps, plates, boxes under ~$20) may use typical current prices — bias those slightly HIGH, never low, and leave hd/lw off.
3. Wire quantities in feet with sensible slack (10-15% extra). Round to purchasable amounts (wire sold in 25/50/100/250ft).
4. Skip materials for services marked [client supplies materials] but note them in "notes".
5. Consolidate shared consumables (wire nuts, staples, tape) into a final section called "Consumables & Rough-In".
6. In "notes": 1-3 sentences — anything client-supplied, items better bought at an electrical supply house than big-box, and any bulk-buy savings.

Respond ONLY with JSON, no markdown fences:
{"sections":[{"service":"...","items":[{"name":"...","spec":"...","qty":1,"unit":"ea","price":0.00,"hd":0.00,"lw":0.00,"live":true}]}],"notes":"..."}
("hd"/"lw" = current Home Depot / Lowe's unit prices from your searches; "price" = the lower of the two; "live": true ONLY for searched prices.)
("live": true ONLY when that price came from a web search you ran; omit or false otherwise.)`,
          messages: [{ role: "user", content: `Job: ${jobName || "Residential electrical"}\nServices:\n${jobLines}` }],
        }),
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data?.error || "AI request failed");
      const raw = (data.content || []).filter(b => b.type === "text").map(b => b.text).join("").replace(/```json|```/g, "").trim();
      const wasCut = data.stop_reason === "max_tokens";
      const parsed = parsePullList(raw);
      if (!parsed || !parsed.sections) throw new Error("Couldn't read the AI's list — tap Try Again");
      if (wasCut && parsed._salvaged) {
        parsed.notes = (parsed.notes ? parsed.notes + " " : "") + "⚠ Very large job — the list was trimmed for length. Tap Try Again if anything looks missing.";
      }
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
            <div style={{ fontSize:13, color:"rgba(255,255,255,0.4)", marginTop:14 }}>Building your pull list and checking live prices at Home Depot & Lowe's for {activeItems.length} service{activeItems.length !== 1 ? "s" : ""}... (~45-60s)</div>
          </div>
        )}

        {error && !loading && (
          <div style={{ ...card, padding:"20px", textAlign:"center" }}>
            <div style={{ fontSize:12, color:"#e87e7e", marginBottom:12 }}>⚠ {error}</div>
            <button onClick={() => { try { window.localStorage.removeItem(PULL_CACHE_KEY); } catch { /* ignore */ } generate(); }} style={{ padding:"9px 18px", borderRadius:8, background:"rgba(var(--accent-rgb),0.1)", border:"1px solid rgba(var(--accent-rgb),0.3)", color:"var(--accent)", fontSize:12, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>Try Again</button>
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
                      <div style={{ textAlign:"right", flexShrink:0 }}>
                        <div style={{ fontFamily:"'DM Mono',monospace", fontSize:13, fontWeight:600, color:"var(--accent)" }}>
                          ${((item.price || 0) * (item.qty || 1)).toFixed(2)}
                        </div>
                        {(item.hd || item.lw) ? (
                          <div style={{ fontSize:8.5, fontFamily:"'DM Mono',monospace", marginTop:3, lineHeight:1.5 }}>
                            {item.hd ? (
                              <div style={{ color: (!item.lw || item.hd <= item.lw) ? "#e8946a" : "rgba(232,148,106,0.45)", fontWeight: (!item.lw || item.hd <= item.lw) ? 800 : 500 }}>
                                HD ${item.hd.toFixed(2)}{(!item.lw || item.hd <= item.lw) ? " ✓" : ""}
                              </div>
                            ) : null}
                            {item.lw ? (
                              <div style={{ color: (!item.hd || item.lw < item.hd) ? "#7eb8e8" : "rgba(126,184,232,0.45)", fontWeight: (!item.hd || item.lw < item.hd) ? 800 : 500 }}>
                                LW ${item.lw.toFixed(2)}{(!item.hd || item.lw < item.hd) ? " ✓" : ""}
                              </div>
                            ) : null}
                          </div>
                        ) : item.live ? (
                          <div style={{ fontSize:8, fontWeight:800, letterSpacing:"0.08em", color:"#7dcea0", marginTop:2 }}>● LIVE</div>
                        ) : null}
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

            {(() => {
              const dual = allItems.filter(i => i.hd && i.lw);
              if (!dual.length) return null;
              const hdOnly  = dual.reduce((a, i) => a + i.hd * (i.qty || 1), 0);
              const lwOnly  = dual.reduce((a, i) => a + i.lw * (i.qty || 1), 0);
              const best    = dual.reduce((a, i) => a + Math.min(i.hd, i.lw) * (i.qty || 1), 0);
              const savings = Math.min(hdOnly, lwOnly) - best;
              return savings >= 5 ? (
                <div style={{ fontSize:11, color:"#7dcea0", background:"rgba(100,220,130,0.06)", border:"1px solid rgba(100,220,130,0.2)", borderRadius:9, padding:"9px 12px", marginTop:10, textAlign:"center" }}>
                  💰 Shopping the ✓ best price per item saves ~${savings.toFixed(0)} vs one-store shopping
                </div>
              ) : null;
            })()}

            {list.notes && (
              <div style={{ fontSize:11, color:"rgba(255,255,255,0.4)", lineHeight:1.7, marginTop:12, padding:"10px 13px", background:"rgba(var(--accent-rgb),0.04)", border:"1px solid rgba(var(--accent-rgb),0.12)", borderRadius:9 }}>
                💡 {list.notes}
              </div>
            )}

            <div style={{ fontSize:9, color:"rgba(255,255,255,0.2)", textAlign:"center", marginTop:14, lineHeight:1.6 }}>
              Wire, panels, breakers & big-ticket items priced live at Home Depot and Lowe's (✓ = cheaper store); small parts estimated — verify in cart.<br/>Supply houses (CED, City Electric, Graybar) often beat big-box on wire and breakers.
            </div>
          </>
        )}
      </div>
    </div>
  );
}
