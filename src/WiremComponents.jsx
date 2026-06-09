// Wireway v2.1 build:1780961706
import { useState, useMemo, useEffect } from "react";
import {
  signOut,
  getQuotes, upsertQuote, deleteQuote as dbDeleteQuote, updateQuoteStatus,
  getClients, upsertClient,
  isPro, isTrialing, trialDaysLeft,
} from "./lib/supabase";
import { CATEGORIES, MARKUP_OPTIONS, HOURLY_RATES, ALL_SERVICES } from "./data/catalog";
import { NEC_REF } from "./data/nec-reference";
import {
  JobCalendar, PhotoAttachments, QuickBooksExport,
  AutoInvoiceButton, OnMyWayButton, ReviewRequestButton,
} from "./features";
import AIQuoteBuilder from "./AIQuoteBuilder";



// ─── COUNTER ─────────────────────────────────────────────────────────────────
export function Counter({ value, onChange }) {
  return (
    <div style={{ display:"flex", alignItems:"center", background:"rgba(255,255,255,0.04)", borderRadius:7, border:"1px solid rgba(255,255,255,0.07)", overflow:"hidden" }}>
      <button onClick={() => onChange(Math.max(0, value-1))} style={{ width:26, height:26, border:"none", background:"transparent", color: value===0 ? "rgba(255,255,255,0.12)" : "#e8c97a", fontSize:16, cursor: value===0 ? "default" : "pointer", display:"flex", alignItems:"center", justifyContent:"center", fontFamily:"inherit" }}>−</button>
      <span style={{ width:22, textAlign:"center", fontFamily:"'DM Mono',monospace", fontSize:12, fontWeight:700, color:"#fff" }}>{value}</span>
      <button onClick={() => onChange(value+1)} style={{ width:26, height:26, border:"none", background:"transparent", color:"#e8c97a", fontSize:16, cursor:"pointer", display:"flex", alignItems:"center", justifyContent:"center", fontFamily:"inherit" }}>+</button>
    </div>
  );
}

// ─── SERVICE ROW ─────────────────────────────────────────────────────────────
export function ServiceRow({ service, entry, onUpdate, accentColor, hourlyRate, clientBuys, showMaterials }) {
  const qty        = entry?.qty ?? 0;
  const variantIdx = entry?.variantIdx ?? 0;
  const cBuys      = entry?.clientBuys ?? clientBuys; // per-line override

  return (
    <div style={{ borderBottom:"1px solid rgba(255,255,255,0.04)", padding:"11px 0", transition:"all 0.2s" }}>
      <div style={{ display:"flex", alignItems:"flex-start", gap:8 }}>
        {/* Name col */}
        <div style={{ flex:1, minWidth:0 }}>
          <div style={{ fontSize:13, fontWeight:600, color: qty>0 ? "#fff" : "rgba(255,255,255,0.58)", lineHeight:1.3, letterSpacing:"-0.01em" }}>
            {service.label}
          </div>
          <div style={{ display:"flex", alignItems:"center", gap:5, marginTop:3, flexWrap:"wrap" }}>
            <span style={{ fontSize:9, fontFamily:"'DM Mono',monospace", color:"rgba(255,255,255,0.28)", background:"rgba(255,255,255,0.05)", border:"1px solid rgba(255,255,255,0.08)", padding:"1px 5px", borderRadius:3, letterSpacing:"0.03em" }}>{service.nec}</span>
            <span style={{ fontSize:9, color:"rgba(255,255,255,0.22)", fontFamily:"'DM Mono',monospace" }}>
              mat ${service.materialCost} · lab ${service.laborCost}/{service.unit}
            </span>
          </div>
        </div>

        {/* Right controls */}
        <div style={{ display:"flex", flexDirection:"column", alignItems:"flex-end", gap:5, flexShrink:0 }}>
          {/* Qty counter */}
          <Counter value={qty} onChange={v => onUpdate({ qty: v, variantIdx, clientBuys: cBuys })} />

          {qty > 0 && (
            <div style={{ display:"flex", flexDirection:"column", alignItems:"flex-end", gap:3 }}>
              {/* Variant */}
              {service.variants.length > 1 && (
                <select value={variantIdx} onChange={e => onUpdate({ qty, variantIdx: Number(e.target.value), clientBuys: cBuys })}
                  style={{ background:"rgba(255,255,255,0.06)", border:"1px solid rgba(255,255,255,0.1)", borderRadius:5, color:"#e8c97a", fontSize:10, padding:"3px 5px", fontFamily:"inherit", cursor:"pointer", maxWidth:130 }}>
                  {service.variants.map((v,i) => <option key={i} value={i} style={{ background:"#1a1a1e" }}>{v.label}</option>)}
                </select>
              )}

              {/* Who buys parts toggle */}
              <div style={{ display:"flex", gap:3 }}>
                {[{ lbl:"I buy parts", val: false }, { lbl:"Client buys", val: true }].map(opt => (
                  <button key={String(opt.val)} onClick={() => onUpdate({ qty, variantIdx, clientBuys: opt.val })}
                    style={{ padding:"2px 7px", borderRadius:4, fontSize:9, fontWeight:700, letterSpacing:"0.02em", cursor:"pointer", fontFamily:"inherit", transition:"all 0.15s",
                      border: cBuys===opt.val ? `1px solid ${accentColor}60` : "1px solid rgba(255,255,255,0.08)",
                      background: cBuys===opt.val ? `${accentColor}15` : "rgba(255,255,255,0.03)",
                      color: cBuys===opt.val ? accentColor : "rgba(255,255,255,0.35)" }}>
                    {opt.lbl}
                  </button>
                ))}
              </div>

              {/* Line cost pills */}
              <div style={{ display:"flex", gap:4, flexWrap:"wrap", justifyContent:"flex-end" }}>
                {showMaterials && !cBuys && (
                  <span style={{ fontSize:9, fontFamily:"'DM Mono',monospace", color:"rgba(255,255,255,0.4)", background:"rgba(255,255,255,0.05)", padding:"2px 6px", borderRadius:4 }}>
                    mat ${(service.materialCost * service.variants[variantIdx].m * qty).toLocaleString()}
                  </span>
                )}
                <span style={{ fontSize:9, fontFamily:"'DM Mono',monospace", color:"rgba(162,220,160,0.8)", background:"rgba(162,220,160,0.08)", padding:"2px 6px", borderRadius:4 }}>
                  lab ${(service.laborCost * service.variants[variantIdx].m * qty).toLocaleString()}
                </span>
                <span style={{ fontSize:10, fontFamily:"'DM Mono',monospace", fontWeight:700, color: accentColor, padding:"2px 6px", background:`${accentColor}12`, borderRadius:4, border:`1px solid ${accentColor}25` }}>
                  ${((cBuys ? service.laborCost : (service.materialCost + service.laborCost)) * service.variants[variantIdx].m * qty).toLocaleString()}
                </span>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

// ─── CATEGORY SECTION ─────────────────────────────────────────────────────────
export function CategorySection({ category, entries, onUpdate, hourlyRate, clientBuys, showMaterials }) {
  const [open, setOpen] = useState(false);
  const active = category.services.filter(s => entries[s.id]?.qty > 0).length;

  return (
    <div style={{ background: active>0 ? `linear-gradient(135deg,${category.color}07 0%,rgba(255,255,255,0.015) 100%)` : "rgba(255,255,255,0.018)", border: active>0 ? `1px solid ${category.color}25` : "1px solid rgba(255,255,255,0.055)", borderRadius:13, overflow:"hidden", transition:"all 0.25s", marginBottom:8 }}>
      <button onClick={() => setOpen(o => !o)} style={{ width:"100%", display:"flex", alignItems:"center", gap:10, padding:"13px 16px", background:"transparent", border:"none", cursor:"pointer", textAlign:"left" }}>
        <div style={{ width:30, height:30, borderRadius:7, flexShrink:0, background: active>0 ? `${category.color}18` : "rgba(255,255,255,0.05)", display:"flex", alignItems:"center", justifyContent:"center", fontSize:13, color: active>0 ? category.color : "rgba(255,255,255,0.28)", transition:"all 0.2s" }}>{category.icon}</div>
        <div style={{ flex:1 }}>
          <div style={{ fontSize:13, fontWeight:700, color: active>0 ? "#fff" : "rgba(255,255,255,0.55)", fontFamily:"'Syne',sans-serif", letterSpacing:"-0.01em" }}>{category.label}</div>
          <div style={{ fontSize:10, color:"rgba(255,255,255,0.28)", marginTop:1, fontFamily:"'DM Mono',monospace" }}>
            {category.services.length} services{active>0 && <span style={{ color:category.color, marginLeft:5 }}>· {active} selected</span>}
          </div>
        </div>
        {active>0 && <span style={{ fontSize:10, fontWeight:700, color:category.color, background:`${category.color}15`, border:`1px solid ${category.color}30`, padding:"2px 7px", borderRadius:8, fontFamily:"'DM Mono',monospace" }}>{active}</span>}
        <span style={{ color:"rgba(255,255,255,0.28)", fontSize:16, transform: open ? "rotate(90deg)" : "rotate(0deg)", transition:"transform 0.2s", lineHeight:1 }}>›</span>
      </button>
      {open && (
        <div style={{ padding:"0 16px 12px" }}>
          {category.services.map(s => (
            <ServiceRow key={s.id} service={s} entry={entries[s.id]} onUpdate={d => onUpdate(s.id, d)}
              accentColor={category.color} hourlyRate={hourlyRate} clientBuys={clientBuys} showMaterials={showMaterials} />
          ))}
        </div>
      )}
    </div>
  );
}

// ─── PILL BUTTON ─────────────────────────────────────────────────────────────
export function Pill({ label, active, onClick, color = "#e8c97a" }) {
  return (
    <button onClick={onClick} style={{ padding:"4px 9px", borderRadius:5, fontSize:11, fontWeight:600, border: active ? `1px solid ${color}45` : "1px solid rgba(255,255,255,0.07)", background: active ? `${color}12` : "transparent", color: active ? color : "rgba(255,255,255,0.38)", cursor:"pointer", transition:"all 0.15s", fontFamily:"inherit" }}>{label}</button>
  );
}

// ─── STAT CARD ────────────────────────────────────────────────────────────────
export function StatCard({ label, value, sub, color = "#e8c97a" }) {
  return (
    <div style={{ flex:1, minWidth:90, background:"rgba(255,255,255,0.022)", border:"1px solid rgba(255,255,255,0.065)", borderRadius:9, padding:"10px 12px" }}>
      <div style={{ fontSize:8, color:"rgba(255,255,255,0.28)", textTransform:"uppercase", letterSpacing:"0.08em", marginBottom:4 }}>{label}</div>
      <div style={{ fontFamily:"'DM Mono',monospace", fontSize:17, fontWeight:500, color, letterSpacing:"-0.02em" }}>{value}</div>
      {sub && <div style={{ fontSize:9, color:"rgba(255,255,255,0.22)", marginTop:2, fontFamily:"'DM Mono',monospace" }}>{sub}</div>}
    </div>
  );
}

// ─── NEC REFERENCE COMPONENT ─────────────────────────────────────────────────
export function NECReference() {
  const [search, setSearch]         = useState("");
  const [expanded, setExpanded]     = useState({});
  const [expandedRule, setExpandedRule] = useState({});
  const [filterTag, setFilterTag]   = useState("all");

  const TAGS = [
    { id: "all",     label: "All Articles" },
    { id: "gfci",    label: "GFCI" },
    { id: "afci",    label: "AFCI" },
    { id: "ground",  label: "Grounding" },
    { id: "panel",   label: "Panel/Service" },
    { id: "wiring",  label: "Wiring Methods" },
    { id: "outlets", label: "Outlets/Switches" },
    { id: "lighting",label: "Lighting" },
    { id: "hvac",    label: "HVAC/Appliances" },
    { id: "outdoor", label: "Outdoor/Pool/EV" },
    { id: "new2023", label: "New in 2023" },
  ];

  const TAG_ARTICLE_MAP = {
    gfci:     ["210", "406", "680"],
    afci:     ["210", "404"],
    ground:   ["250", "200"],
    panel:    ["408", "230", "240", "215"],
    wiring:   ["300", "310", "334", "358", "110"],
    outlets:  ["406", "404", "210"],
    lighting: ["410", "314"],
    hvac:     ["440", "422", "550"],
    outdoor:  ["225", "680", "625", "702"],
    new2023:  ["230", "410", "404", "625", "210"],
  };

  const q = search.toLowerCase().trim();

  const filtered = NEC_REF.filter(art => {
    const matchesTag = filterTag === "all" || (TAG_ARTICLE_MAP[filterTag] || []).includes(art.article);
    if (!matchesTag) return false;
    if (!q) return true;
    return (
      art.article.includes(q) ||
      art.title.toLowerCase().includes(q) ||
      art.summary.toLowerCase().includes(q) ||
      art.rules.some(r => r.title.toLowerCase().includes(q) || r.text.toLowerCase().includes(q) || r.code.toLowerCase().includes(q)) ||
      (art.violations || []).some(v => v.toLowerCase().includes(q))
    );
  });

  const toggleArticle = (id) => setExpanded(p => ({ ...p, [id]: !p[id] }));
  const toggleRule = (key) => setExpandedRule(p => ({ ...p, [key]: !p[key] }));

  return (
    <div style={{ animation: "fadeUp 0.3s ease both" }}>
      {/* Header */}
      <div style={{ marginBottom: 16 }}>
        <div style={{ fontSize: 11, color: "rgba(255,255,255,0.3)", marginBottom: 10, lineHeight: 1.6 }}>
          Full NEC 2023 residential reference · {NEC_REF.length} articles · {NEC_REF.reduce((a, c) => a + c.rules.length, 0)} code sections · searchable
        </div>

        {/* Search */}
        <input
          placeholder="Search article, code number, or keyword (e.g. GFCI, 210.8, tamper)..."
          value={search}
          onChange={e => setSearch(e.target.value)}
          style={{ width: "100%", background: "rgba(255,255,255,0.04)", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 9, padding: "11px 14px", fontSize: 13, color: "#fff", fontFamily: "inherit", marginBottom: 10, transition: "border-color 0.15s" }}
          onFocus={e => e.target.style.borderColor = "rgba(232,201,122,0.4)"}
          onBlur={e => e.target.style.borderColor = "rgba(255,255,255,0.1)"}
        />

        {/* Filter tags */}
        <div style={{ display: "flex", flexWrap: "wrap", gap: 5 }}>
          {TAGS.map(t => (
            <button key={t.id} onClick={() => setFilterTag(t.id)}
              style={{ padding: "4px 10px", borderRadius: 5, fontSize: 10, fontWeight: 700, letterSpacing: "0.03em", cursor: "pointer", fontFamily: "inherit", transition: "all 0.15s",
                border: filterTag === t.id ? "1px solid rgba(232,201,122,0.5)" : "1px solid rgba(255,255,255,0.08)",
                background: filterTag === t.id ? "rgba(232,201,122,0.12)" : "rgba(255,255,255,0.03)",
                color: filterTag === t.id ? "#e8c97a" : "rgba(255,255,255,0.38)" }}>
              {t.label}
            </button>
          ))}
        </div>
      </div>

      {/* Results count */}
      {(q || filterTag !== "all") && (
        <div style={{ fontSize: 10, color: "rgba(255,255,255,0.25)", fontFamily: "'DM Mono',monospace", marginBottom: 12 }}>
          {filtered.length} article{filtered.length !== 1 ? "s" : ""} · {filtered.reduce((a, c) => a + c.rules.length, 0)} code sections
        </div>
      )}

      {/* Article cards */}
      {filtered.length === 0 ? (
        <div style={{ textAlign: "center", padding: "40px 20px", color: "rgba(255,255,255,0.2)", fontSize: 13 }}>
          No results for "{search}"
        </div>
      ) : (
        filtered.map(art => {
          const isOpen = expanded[art.article];
          return (
            <div key={art.article} style={{ background: isOpen ? `linear-gradient(135deg,${art.color}06 0%,rgba(255,255,255,0.015) 100%)` : "rgba(255,255,255,0.018)", border: isOpen ? `1px solid ${art.color}22` : "1px solid rgba(255,255,255,0.055)", borderRadius: 13, marginBottom: 8, overflow: "hidden", transition: "all 0.25s" }}>

              {/* Article header */}
              <button onClick={() => toggleArticle(art.article)} style={{ width: "100%", display: "flex", alignItems: "flex-start", gap: 12, padding: "14px 16px", background: "transparent", border: "none", cursor: "pointer", textAlign: "left" }}>
                <div style={{ flexShrink: 0, width: 46, height: 46, borderRadius: 8, background: `${art.color}15`, border: `1px solid ${art.color}25`, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center" }}>
                  <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 10, color: art.color, fontWeight: 700, letterSpacing: "0.02em" }}>ART.</div>
                  <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 13, color: art.color, fontWeight: 700, lineHeight: 1 }}>{art.article}</div>
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 13, fontWeight: 700, color: "#fff", fontFamily: "'Syne',sans-serif", letterSpacing: "-0.01em", lineHeight: 1.2 }}>{art.title}</div>
                  <div style={{ fontSize: 11, color: "rgba(255,255,255,0.35)", marginTop: 4, lineHeight: 1.5 }}>{art.summary.slice(0, 100)}{art.summary.length > 100 ? "…" : ""}</div>
                  <div style={{ display: "flex", gap: 8, marginTop: 5, flexWrap: "wrap" }}>
                    <span style={{ fontSize: 9, color: "rgba(255,255,255,0.25)", fontFamily: "'DM Mono',monospace" }}>{art.rules.length} sections</span>
                    {art.violations?.length > 0 && <span style={{ fontSize: 9, color: "rgba(232,120,120,0.6)", fontFamily: "'DM Mono',monospace" }}>· {art.violations.length} common violations</span>}
                  </div>
                </div>
                <span style={{ color: "rgba(255,255,255,0.25)", fontSize: 16, transform: isOpen ? "rotate(90deg)" : "rotate(0deg)", transition: "transform 0.2s", lineHeight: 1, flexShrink: 0 }}>›</span>
              </button>

              {/* Article content */}
              {isOpen && (
                <div style={{ padding: "0 16px 16px" }}>
                  {/* Full summary */}
                  <div style={{ fontSize: 12, color: "rgba(255,255,255,0.5)", lineHeight: 1.7, marginBottom: 14, paddingBottom: 14, borderBottom: "1px solid rgba(255,255,255,0.06)" }}>
                    {art.summary}
                  </div>

                  {/* Code sections */}
                  <div style={{ fontSize: 10, color: "rgba(255,255,255,0.25)", textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 8 }}>Code Sections</div>
                  {art.rules.map((rule, ri) => {
                    const rKey = `${art.article}-${ri}`;
                    const rOpen = expandedRule[rKey];
                    return (
                      <div key={ri} style={{ background: rOpen ? "rgba(255,255,255,0.04)" : "rgba(255,255,255,0.02)", border: "1px solid rgba(255,255,255,0.06)", borderRadius: 8, marginBottom: 5, overflow: "hidden", transition: "all 0.2s" }}>
                        <button onClick={() => toggleRule(rKey)} style={{ width: "100%", display: "flex", alignItems: "center", gap: 10, padding: "10px 12px", background: "transparent", border: "none", cursor: "pointer", textAlign: "left" }}>
                          <span style={{ fontFamily: "'DM Mono',monospace", fontSize: 10, color: art.color, fontWeight: 700, flexShrink: 0, minWidth: 60 }}>{rule.code}</span>
                          <span style={{ fontSize: 12, fontWeight: 600, color: rOpen ? "#fff" : "rgba(255,255,255,0.65)", flex: 1 }}>{rule.title}</span>
                          <span style={{ color: "rgba(255,255,255,0.2)", fontSize: 14, transform: rOpen ? "rotate(90deg)" : "rotate(0deg)", transition: "transform 0.2s", lineHeight: 1, flexShrink: 0 }}>›</span>
                        </button>
                        {rOpen && (
                          <div style={{ padding: "0 12px 12px", fontSize: 12, color: "rgba(255,255,255,0.55)", lineHeight: 1.75, borderTop: "1px solid rgba(255,255,255,0.05)", paddingTop: 10 }}>
                            {rule.text}
                          </div>
                        )}
                      </div>
                    );
                  })}

                  {/* Common violations */}
                  {art.violations?.length > 0 && (
                    <div style={{ marginTop: 14 }}>
                      <div style={{ fontSize: 10, color: "rgba(232,120,120,0.6)", textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 8, fontWeight: 700 }}>⚠ Common Violations</div>
                      {art.violations.map((v, vi) => (
                        <div key={vi} style={{ display: "flex", gap: 8, alignItems: "flex-start", marginBottom: 6 }}>
                          <span style={{ color: "rgba(232,120,120,0.5)", fontSize: 11, flexShrink: 0, marginTop: 1 }}>·</span>
                          <span style={{ fontSize: 11, color: "rgba(255,255,255,0.45)", lineHeight: 1.6 }}>{v}</span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </div>
          );
        })
      )}
    </div>
  );
}
