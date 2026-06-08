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
import Dashboard from "./Dashboard";



// ─── COUNTER ─────────────────────────────────────────────────────────────────
function Counter({ value, onChange }) {
  return (
    <div style={{ display:"flex", alignItems:"center", background:"rgba(255,255,255,0.04)", borderRadius:7, border:"1px solid rgba(255,255,255,0.07)", overflow:"hidden" }}>
      <button onClick={() => onChange(Math.max(0, value-1))} style={{ width:26, height:26, border:"none", background:"transparent", color: value===0 ? "rgba(255,255,255,0.12)" : "#e8c97a", fontSize:16, cursor: value===0 ? "default" : "pointer", display:"flex", alignItems:"center", justifyContent:"center", fontFamily:"inherit" }}>−</button>
      <span style={{ width:22, textAlign:"center", fontFamily:"'DM Mono',monospace", fontSize:12, fontWeight:700, color:"#fff" }}>{value}</span>
      <button onClick={() => onChange(value+1)} style={{ width:26, height:26, border:"none", background:"transparent", color:"#e8c97a", fontSize:16, cursor:"pointer", display:"flex", alignItems:"center", justifyContent:"center", fontFamily:"inherit" }}>+</button>
    </div>
  );
}

// ─── SERVICE ROW ─────────────────────────────────────────────────────────────
function ServiceRow({ service, entry, onUpdate, accentColor, hourlyRate, clientBuys, showMaterials }) {
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
function CategorySection({ category, entries, onUpdate, hourlyRate, clientBuys, showMaterials }) {
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
function Pill({ label, active, onClick, color = "#e8c97a" }) {
  return (
    <button onClick={onClick} style={{ padding:"4px 9px", borderRadius:5, fontSize:11, fontWeight:600, border: active ? `1px solid ${color}45` : "1px solid rgba(255,255,255,0.07)", background: active ? `${color}12` : "transparent", color: active ? color : "rgba(255,255,255,0.38)", cursor:"pointer", transition:"all 0.15s", fontFamily:"inherit" }}>{label}</button>
  );
}

// ─── STAT CARD ────────────────────────────────────────────────────────────────
function StatCard({ label, value, sub, color = "#e8c97a" }) {
  return (
    <div style={{ flex:1, minWidth:90, background:"rgba(255,255,255,0.022)", border:"1px solid rgba(255,255,255,0.065)", borderRadius:9, padding:"10px 12px" }}>
      <div style={{ fontSize:8, color:"rgba(255,255,255,0.28)", textTransform:"uppercase", letterSpacing:"0.08em", marginBottom:4 }}>{label}</div>
      <div style={{ fontFamily:"'DM Mono',monospace", fontSize:17, fontWeight:500, color, letterSpacing:"-0.02em" }}>{value}</div>
      {sub && <div style={{ fontSize:9, color:"rgba(255,255,255,0.22)", marginTop:2, fontFamily:"'DM Mono',monospace" }}>{sub}</div>}
    </div>
  );
}

// ─── NEC REFERENCE COMPONENT ─────────────────────────────────────────────────
function NECReference() {
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

// ─── MAIN APP ─────────────────────────────────────────────────────────────────
export default function Wireway({ user, profile, onProfileUpdate, onShowPricing, paymentBanner, onClearBanner }) {
  // ── Core state ──
  const [entries,        setEntries]        = useState({});
  const [hourlyRate,     setHourlyRate]     = useState(profile?.hourly_rate || 85);
  const [markup,         setMarkup]         = useState(Number(profile?.default_markup) || 0.30);
  const [clientName,     setClientName]     = useState("");
  const [clientEmail,    setClientEmail]    = useState("");
  const [clientPhone,    setClientPhone]    = useState("");
  const [jobName,        setJobName]        = useState("");
  const [notes,          setNotes]          = useState("");
  const [tab,            setTab]            = useState("services");
  const [showMaterials,  setShowMaterials]  = useState(true);
  const [clientBuysAll,  setClientBuysAll]  = useState(false);
  const [copied,         setCopied]         = useState(false);
  const [quoteNumber,    setQuoteNumber]    = useState("");
  const [quoteId,        setQuoteId]        = useState(null);
  const [signModal,      setSignModal]      = useState(false);
  const [sigName,        setSigName]        = useState("");
  const [sigDate,        setSigDate]        = useState("");
  const [sigSaved,       setSigSaved]       = useState(false);
  const [savedQuotes,    setSavedQuotes]    = useState([]);
  const [saveMsg,        setSaveMsg]        = useState("");
  const [customItems,    setCustomItems]    = useState([]);
  const [taxEnabled,     setTaxEnabled]     = useState(false);
  const [taxRate,        setTaxRate]        = useState(0.08);
  const [invoiceMode,    setInvoiceMode]    = useState(false);
  const [invoiceDueDate, setInvoiceDueDate] = useState("");
  const [invoicePaid,    setInvoicePaid]    = useState(false);
  const [flatRateMode,   setFlatRateMode]   = useState(false);
  const [clients,        setClients]        = useState([]);
  const [showClientDB,   setShowClientDB]   = useState(false);
  const [clientSearch,   setClientSearch]   = useState("");
  const [wireCalcOpen,   setWireCalcOpen]   = useState(false);
  const [wireAmps,       setWireAmps]       = useState("");
  const [wireLen,        setWireLen]        = useState("");
  const [wireVolt,       setWireVolt]       = useState("120");
  const [wireMat,        setWireMat]        = useState("copper");
  const [loadCalcOpen,   setLoadCalcOpen]   = useState(false);
  const [sqft,           setSqft]           = useState("");
  const [smallAppl,      setSmallAppl]      = useState(2);
  const [laundry,        setLaundry]        = useState(1);
  const [dryer,          setDryer]          = useState(0);
  const [range,          setRange]          = useState(0);
  const [acTons,         setAcTons]         = useState(0);
  const [heatKw,         setHeatKw]         = useState(0);
  const [checklistOpen,  setChecklistOpen]  = useState(false);
  const [checklistType,  setChecklistType]  = useState("service_upgrade");
  const [checkedItems,   setCheckedItems]   = useState({});
  const [depositOnly,    setDepositOnly]    = useState(true);
  const [depositPercent, setDepositPercent] = useState(50);
  const [paymentLoading, setPaymentLoading] = useState(false);
  const [paymentError,   setPaymentError]   = useState("");
  const [paymentSuccess, setPaymentSuccess] = useState(!!paymentBanner);
  const [showAccount,    setShowAccount]    = useState(false);
  const [showCalendar,   setShowCalendar]   = useState(false);
  const [showDashboard,  setShowDashboard]  = useState(true); // show dashboard by default
  const [showAIBuilder,  setShowAIBuilder]  = useState(false);
  const [installPrompt,  setInstallPrompt]  = useState(null);
  const [showInstall,    setShowInstall]    = useState(false);

  // Capture PWA install prompt
  useEffect(() => {
    const handler = (e) => { e.preventDefault(); setInstallPrompt(e); setShowInstall(true); };
    window.addEventListener("beforeinstallprompt", handler);
    return () => window.removeEventListener("beforeinstallprompt", handler);
  }, []);

  const triggerInstall = async () => {
    if (!installPrompt) return;
    installPrompt.prompt();
    const { outcome } = await installPrompt.userChoice;
    if (outcome === "accepted") setShowInstall(false);
    setInstallPrompt(null);
  };
  const [proGateMsg,     setProGateMsg]     = useState("");

  // ── Wire size calculator (NEC 310.15) ──
  const wireResult = useMemo(() => {
    const a = parseFloat(wireAmps);
    if (!a || a <= 0) return null;
    const cuTable = [{a:15,awg:"14"},{a:20,awg:"12"},{a:30,awg:"10"},{a:40,awg:"8"},{a:55,awg:"6"},{a:70,awg:"4"},{a:85,awg:"3"},{a:95,awg:"2"},{a:110,awg:"1"},{a:130,awg:"1/0"},{a:150,awg:"2/0"},{a:175,awg:"3/0"},{a:200,awg:"4/0"}];
    const alTable = [{a:15,awg:"12"},{a:20,awg:"10"},{a:30,awg:"8"},{a:40,awg:"6"},{a:55,awg:"4"},{a:65,awg:"3"},{a:75,awg:"2"},{a:85,awg:"1"},{a:100,awg:"1/0"},{a:120,awg:"2/0"},{a:135,awg:"3/0"},{a:155,awg:"4/0"}];
    const table = wireMat === "copper" ? cuTable : alTable;
    const needed = a * 1.25;
    const row = table.find(r => r.a >= needed) || table[table.length - 1];
    const len = parseFloat(wireLen) || 0;
    const cmilMap = {"14":4110,"12":6530,"10":10380,"8":16510,"6":26240,"4":41740,"3":52620,"2":66360,"1":83690,"1/0":105600,"2/0":133100,"3/0":167800,"4/0":211600};
    const cmil = cmilMap[row.awg] || 10380;
    const resistivity = wireMat === "copper" ? 10.4 : 17;
    const vDrop = len > 0 ? (2 * resistivity * len * a) / cmil : 0;
    const vDropPct = len > 0 ? (vDrop / parseFloat(wireVolt)) * 100 : 0;
    return { awg: row.awg, ampacity: row.a, continuous: needed.toFixed(1), vDrop: vDrop.toFixed(2), vDropPct: vDropPct.toFixed(1), vDropOk: vDropPct < 3, nec: "NEC 310.15(B)(16)" };
  }, [wireAmps, wireLen, wireVolt, wireMat]);

  // ── Load calculator (NEC 220.82) ──
  const loadResult = useMemo(() => {
    const sf = parseFloat(sqft) || 0;
    if (!sf) return null;
    const lighting = sf * 3;
    const sabc = (smallAppl * 1500) + (laundry * 1500);
    const genLoad = lighting + sabc;
    const gen = genLoad <= 10000 ? genLoad : 10000 + (genLoad - 10000) * 0.4;
    const dryerVA = dryer  * 5000;
    const rangeVA = range  * 8000;
    const acVA    = acTons * 3516;
    const heatVA  = heatKw * 1000;
    const hvac    = Math.max(acVA, heatVA);
    const totalVA = gen + dryerVA + rangeVA + hvac;
    const amps240 = totalVA / 240;
    const panelSize = amps240 <= 100 ? "100A" : amps240 <= 150 ? "150A" : amps240 <= 200 ? "200A" : "400A";
    return { lighting, sabc, gen: Math.round(gen), dryerVA, rangeVA, hvac, totalVA: Math.round(totalVA), amps240: Math.round(amps240), panelSize };
  }, [sqft, smallAppl, laundry, dryer, range, acTons, heatKw]);

  // ── Plan helpers ──
  const userIsPro = isPro(profile);
  const daysLeft  = trialDaysLeft(profile);
  const onTrial   = isTrialing(profile);
  // Debug — remove after confirming upgrade button works
  console.log("profile:", profile?.plan, profile?.subscription_status, "isPro:", userIsPro, "onTrial:", onTrial);

  const requirePro = (action) => {
    if (userIsPro) { action(); return; }
    setProGateMsg("This feature requires Wireway Pro. Upgrade to unlock.");
    setTimeout(() => setProGateMsg(""), 3500);
    if (onShowPricing) setTimeout(() => onShowPricing(), 400);
  };

  // ── Load Supabase data on mount ──
  useEffect(() => {
    if (!user?.id) return;
    getQuotes(user.id).then(({ data }) => { if (data?.length) setSavedQuotes(data); });
    getClients(user.id).then(({ data }) => { if (data?.length) setClients(data); });
  }, [user?.id]);

  const addCustomItem = () => setCustomItems(p => [...p, { id: Date.now(), label: "", qty: 1, materialCost: 0, laborCost: 0, laborHours: 0 }]);
  const updateCustomItem = (id, data) => setCustomItems(p => p.map(i => i.id === id ? { ...i, ...data } : i));
  const removeCustomItem = (id) => setCustomItems(p => p.filter(i => i.id !== id));
  const toggleCheck = (id) => setCheckedItems(p => ({ ...p, [id]: !p[id] }));

  const saveClientToDB = async () => {
    if (!clientName || !user?.id) return;
    const { data } = await upsertClient(user.id, { name: clientName, email: clientEmail, phone: clientPhone });
    if (data) setClients(prev => { const idx = prev.findIndex(c => c.id===data.id); return idx>=0 ? prev.map((c,i)=>i===idx?data:c) : [data,...prev]; });
  };

  const loadClient = (c) => { setClientName(c.name); setClientEmail(c.email||""); setClientPhone(c.phone||""); setShowClientDB(false); };

  const CHECKLISTS = {
    service_upgrade: {
      label: "Service Upgrade",
      items: [
        { id:"su1",  nec:"NEC 230.67",      text:"Whole-home surge protector installed (required 2023)" },
        { id:"su2",  nec:"NEC 230.79",      text:"Service disconnect rated correctly (min 100A residential)" },
        { id:"su3",  nec:"NEC 230.24",      text:"Service entrance clearances met (10 ft min at grade)" },
        { id:"su4",  nec:"NEC 250.50",      text:"Grounding electrode system complete and bonded" },
        { id:"su5",  nec:"NEC 250.104",     text:"Water and gas piping bonded" },
        { id:"su6",  nec:"NEC 250.28",      text:"Main bonding jumper installed at panel" },
        { id:"su7",  nec:"NEC 408.4",       text:"All circuits labeled on directory" },
        { id:"su8",  nec:"NEC 110.26",      text:"36 in. working clearance maintained in front of panel" },
        { id:"su9",  nec:"NEC 230.85",      text:"Exterior emergency disconnect installed (required 2023)" },
        { id:"su10", nec:"NEC 408.7",       text:"All unused knockouts sealed" },
      ],
    },
    new_circuit: {
      label: "New Circuit / Rough-In",
      items: [
        { id:"nc1",  nec:"NEC 210.12",      text:"AFCI protection on all required circuits" },
        { id:"nc2",  nec:"NEC 210.8",       text:"GFCI protection in all required locations" },
        { id:"nc3",  nec:"NEC 300.4",       text:"Nail plates installed within 1.25 in. of framing edge" },
        { id:"nc4",  nec:"NEC 300.14",      text:"6 in. free conductor at all boxes" },
        { id:"nc5",  nec:"NEC 314.16",      text:"Box fill calculations verified (no overfill)" },
        { id:"nc6",  nec:"NEC 334.30",      text:"NM cable secured within 12 in. of all boxes" },
        { id:"nc7",  nec:"NEC 240.4",       text:"Conductor sized correctly for overcurrent device" },
        { id:"nc8",  nec:"NEC 110.12",      text:"Workmanlike installation — cables properly routed" },
        { id:"nc9",  nec:"NEC 406.12",      text:"All receptacles are tamper-resistant (TR rated)" },
        { id:"nc10", nec:"NEC 210.52",      text:"Outlet spacing verified (no point more than 6 ft from outlet)" },
      ],
    },
    pool: {
      label: "Pool / Spa",
      items: [
        { id:"p1",   nec:"NEC 680.26",      text:"Equipotential bonding complete — all metal within 5 ft bonded" },
        { id:"p2",   nec:"NEC 680.22",      text:"Receptacles at least 6 ft from pool edge (GFCI protected)" },
        { id:"p3",   nec:"NEC 680.22",      text:"No luminaires within 12 ft horizontally of pool water" },
        { id:"p4",   nec:"NEC 680.21(C)",   text:"GFCI protection on all pump motor circuits" },
        { id:"p5",   nec:"NEC 680.23",      text:"Underwater lighting properly niched and rated" },
        { id:"p6",   nec:"NEC 680.12",      text:"Equipment disconnect within sight of pool equipment" },
        { id:"p7",   nec:"NEC 680.6",       text:"All equipment within 5 ft of pool grounded (insulated GEC)" },
        { id:"p8",   nec:"NEC 680.42",      text:"Spa/hot tub has GFCI protection on all circuits" },
      ],
    },
    final_inspection: {
      label: "Final Inspection",
      items: [
        { id:"fi1",  nec:"NEC 314.25",      text:"All boxes have covers or faceplates" },
        { id:"fi2",  nec:"NEC 408.4",       text:"Panel directory complete and legible" },
        { id:"fi3",  nec:"NEC 410.16",      text:"Closet lighting fixtures are LED (no incandescent)" },
        { id:"fi4",  nec:"NEC 200.11",      text:"All outlets tested — no reversed polarity" },
        { id:"fi5",  nec:"NEC 210.8",       text:"GFCI outlets tested and functional" },
        { id:"fi6",  nec:"NEC 210.12",      text:"AFCI breakers tested (test button)" },
        { id:"fi7",  nec:"NEC 760.41",      text:"Smoke detectors interconnected and functional" },
        { id:"fi8",  nec:"NEC 110.3(B)",    text:"All equipment installed per listing/labeling" },
        { id:"fi9",  nec:"NEC 230.67",      text:"Surge protector installed and indicator light green" },
        { id:"fi10", nec:"NEC 110.26",      text:"Panel working clearance verified and unobstructed" },
        { id:"fi11", nec:"NEC 406.9",       text:"All outdoor outlets have in-use weatherproof covers" },
        { id:"fi12", nec:"NEC 250.53",      text:"Ground rods driven full depth (8 ft min)" },
      ],
    },
  };

  // ── Generate quote number ──
  const genQuoteNum = () => {
    const yr  = new Date().getFullYear();
    const seq = (savedQuotes.length + 1).toString().padStart(3, "0");
    return `WW-${yr}-${seq}`;
  };

  // ── Save estimate to Supabase ──
  const saveQuote = async () => {
    if (!hasItems || !user?.id) return;
    const qn = quoteNumber || genQuoteNum();
    setQuoteNumber(qn);
    const payload = {
      id: quoteId || undefined,
      quoteNumber: qn, clientName, clientEmail, clientPhone, jobName, notes,
      hourlyRate, markup, showMaterials, clientBuysAll,
      flatRateMode, invoiceMode, invoiceDueDate, invoicePaid,
      taxEnabled, taxRate, entries, customItems,
      totMat, totLab, totHrs, markupAmt, taxAmt, total, status: "draft",
    };
    const { data, error } = await upsertQuote(user.id, payload);
    if (data) {
      setQuoteId(data.id);
      setSavedQuotes(prev => { const idx=prev.findIndex(q=>q.id===data.id); return idx>=0?prev.map((q,i)=>i===idx?data:q):[data,...prev]; });
    }
    saveClientToDB();
    setSaveMsg(error ? "Save failed" : "Saved!"); setTimeout(() => setSaveMsg(""), 2000);
  };

  // ── Load a saved quote — fetch full data first ──
  const loadQuote = async (q) => {
    // If entries are missing (summary-only row), fetch the full quote
    let fullQ = q;
    if (!q.entries && q.id && user?.id) {
      const { getQuote } = await import("./lib/supabase");
      const { data } = await getQuote(q.id);
      if (data) fullQ = data;
    }
    setEntries(fullQ.entries || {});
    setCustomItems(fullQ.custom_items || fullQ.customItems || []);
    setHourlyRate(fullQ.hourly_rate || fullQ.hourlyRate || 85);
    setMarkup(Number(fullQ.markup) || 0.30);
    setClientName(fullQ.client_name || fullQ.clientName || "");
    setClientEmail(fullQ.client_email || fullQ.clientEmail || "");
    setClientPhone(fullQ.client_phone || fullQ.clientPhone || "");
    setJobName(fullQ.job_name || fullQ.jobName || "");
    setNotes(fullQ.notes || "");
    setShowMaterials(fullQ.show_materials ?? fullQ.showMaterials ?? true);
    setClientBuysAll(fullQ.client_buys_all ?? fullQ.clientBuysAll ?? false);
    setFlatRateMode(fullQ.flat_rate_mode ?? fullQ.flatRateMode ?? false);
    setInvoiceMode(fullQ.invoice_mode ?? fullQ.invoiceMode ?? false);
    setInvoiceDueDate(fullQ.invoice_due_date || fullQ.invoiceDueDate || "");
    setInvoicePaid(fullQ.invoice_paid ?? fullQ.invoicePaid ?? false);
    setTaxEnabled(fullQ.tax_enabled ?? fullQ.taxEnabled ?? false);
    setTaxRate(Number(fullQ.tax_rate) || Number(fullQ.taxRate) || 0.08);
    setQuoteNumber(fullQ.quote_number || fullQ.quoteNumber || "");
    setQuoteId(fullQ.id || null);
    setTab("summary");
    setSaveMsg("Quote loaded");
    setTimeout(() => setSaveMsg(""), 2000);
  };

  // ── Delete a saved quote ──
  const deleteQuote = async (id) => {
    if (user?.id) await dbDeleteQuote(id, user.id);
    setSavedQuotes(prev => prev.filter(q => q.id !== id));
  };

  // ── Mark quote as accepted (signature) ──
  const acceptQuote = async () => {
    if (!sigName) return;
    const date = sigDate || new Date().toLocaleDateString();
    if (quoteId && user?.id) {
      await updateQuoteStatus(quoteId, "accepted", { sig_name: sigName, sig_date: date, signed_at: new Date().toISOString() });
    }
    setSavedQuotes(prev => prev.map(q => q.id===quoteId ? { ...q, status:"accepted", sig_name:sigName } : q));
    setSigSaved(true);
    setTimeout(() => { setSignModal(false); setSigSaved(false); }, 2000);
  };

  const currentQuoteStatus = savedQuotes.find(q => q.quoteNumber === quoteNumber);

  // ── Company profile — load from Supabase profile ──
  const [company, setCompany] = useState(() => {
    // Seed from Supabase profile if available, fallback to localStorage
    if (profile) {
      return {
        name:       profile.company_name    || "",
        phone:      profile.company_phone   || "",
        email:      profile.company_email   || "",
        address:    profile.company_address || "",
        license:    profile.license_number  || "",
        website:    profile.company_website || "",
        terms:      profile.terms           || "",
        logoDataUrl: profile.logo_url       || "",
        stripeKey:  profile.stripe_key      || "",
      };
    }
    try { return JSON.parse(localStorage.getItem("wireway_company") || "{}"); } catch { return {}; }
  });
  const [editingCompany, setEditingCompany] = useState(false);
  const [companyDraft,   setCompanyDraft]   = useState(company);
  const [logoDataUrl,    setLogoDataUrl]    = useState(company.logoDataUrl || profile?.logo_url || "");
  const [companySaving,  setCompanySaving]  = useState(false);

  // Sync company from profile when profile loads
  useEffect(() => {
    if (!profile) return;
    const fromProfile = {
      name:        profile.company_name    || "",
      phone:       profile.company_phone   || "",
      email:       profile.company_email   || "",
      address:     profile.company_address || "",
      license:     profile.license_number  || "",
      website:     profile.company_website || "",
      terms:       profile.terms           || "",
      logoDataUrl: profile.logo_url        || "",
      stripeKey:   profile.stripe_key      || "",
    };
    setCompany(fromProfile);
    setLogoDataUrl(profile.logo_url || "");
  }, [profile?.id, profile]);

  const saveCompany = async () => {
    setCompanySaving(true);
    const saved = { ...companyDraft, logoDataUrl };
    setCompany(saved);
    // Also save to localStorage as fallback
    try { localStorage.setItem("wireway_company", JSON.stringify(saved)); } catch {}
    // Save to Supabase profiles table
    if (user?.id) {
      const { updateProfile } = await import("./lib/supabase");
      await updateProfile(user.id, {
        company_name:    companyDraft.name    || null,
        company_phone:   companyDraft.phone   || null,
        company_email:   companyDraft.email   || null,
        company_address: companyDraft.address || null,
        license_number:  companyDraft.license || null,
        company_website: companyDraft.website || null,
        terms:           companyDraft.terms   || null,
        logo_url:        logoDataUrl          || null,
        stripe_key:      companyDraft.stripeKey || null,
        hourly_rate:     hourlyRate,
        default_markup:  markup,
      });
      if (onProfileUpdate) onProfileUpdate({ ...profile, company_name: companyDraft.name });
    }
    setCompanySaving(false);
    setEditingCompany(false);
  };

  const handleLogoUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    // If logged in, upload to Supabase Storage
    if (user?.id) {
      const { uploadLogo } = await import("./lib/supabase");
      const { url } = await uploadLogo(user.id, file);
      if (url) { setLogoDataUrl(url); return; }
    }
    // Fallback: base64 in memory
    const reader = new FileReader();
    reader.onload = (ev) => setLogoDataUrl(ev.target.result);
    reader.readAsDataURL(file);
  };

  // ── Handle quick job card tap from Dashboard ──
  const handleQuickJob = (job) => {
    // Reset and pre-load common services for this job type
    newQuote();
    // Set job name
    setJobName(job.label);
    // Small delay to let state reset, then switch to services tab
    setTimeout(() => { setShowDashboard(false); setTab("services"); }, 50);
  };

  // ── Apply AI-generated estimate items to entries ──
  const applyAIEstimate = (items) => {
    const newEntries = { ...entries };
    items.forEach(item => {
      newEntries[item.id] = {
        qty:        item.qty,
        variantIdx: item.variantIdx,
        clientBuys: false,
      };
    });
    setEntries(newEntries);
    setShowAIBuilder(false);
    setTab("summary");
    setSaveMsg(`${items.length} services added by AI`);
    setTimeout(() => setSaveMsg(""), 3000);
  };

  // ── New quote — reset all state ──
  const newQuote = (fromDashboard = false) => {
    if (!fromDashboard && hasItems && !window.confirm("Start a new quote? Your current estimate will be cleared.")) return;
    setEntries({});
    setCustomItems([]);
    setClientName(""); setClientEmail(""); setClientPhone("");
    setJobName(""); setNotes("");
    setQuoteNumber(""); setQuoteId(null);
    setSigName(""); setSigDate(""); setSigSaved(false);
    setInvoiceMode(false); setInvoiceDueDate(""); setInvoicePaid(false);
    setTaxEnabled(false); setFlatRateMode(false);
    setTab("services");
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const upd = (id, data) => setEntries(p => ({ ...p, [id]: data }));

  // ── Totals (includes custom items) ──
  const { activeItems, totMat, totLab, totHrs, totClientBuysMat } = useMemo(() => {
    const items = ALL_SERVICES.filter(s => entries[s.id]?.qty > 0).map(s => {
      const e = entries[s.id];
      const v = s.variants[e.variantIdx ?? 0];
      const qty = e.qty;
      const cBuys = e.clientBuys ?? clientBuysAll;
      const mat  = s.materialCost * v.m * qty;
      const lab  = s.laborCost    * v.m * qty;
      const hrs  = s.laborHours   * v.m * qty;
      return { ...s, qty, variantLabel: v.label, mat, lab, hrs, cBuys, lineTotal: cBuys ? lab : mat + lab };
    });
    // Add custom items
    const custActive = customItems.filter(i => i.label && i.qty > 0).map(i => ({
      ...i, variantLabel: "Custom", nec: "—", catColor: "#aaa", catLabel: "Custom",
      mat: i.materialCost * i.qty, lab: i.laborCost * i.qty, hrs: i.laborHours * i.qty,
      cBuys: false, lineTotal: (i.materialCost + i.laborCost) * i.qty,
    }));
    const all = [...items, ...custActive];
    return {
      activeItems: all,
      totMat:           all.reduce((a,i) => a + (i.cBuys ? 0 : i.mat), 0),
      totLab:           all.reduce((a,i) => a + i.lab, 0),
      totHrs:           all.reduce((a,i) => a + i.hrs, 0),
      totClientBuysMat: all.reduce((a,i) => a + (i.cBuys ? i.mat : 0), 0),
    };
  }, [entries, clientBuysAll, customItems]);

  const subtotal   = totMat + totLab;
  const markupAmt  = subtotal * markup;
  const taxAmt     = taxEnabled ? totMat * taxRate : 0;
  const total      = subtotal + markupAmt + taxAmt;
  const hasItems   = activeItems.length > 0;

  // Profit analysis
  const grossProfit   = markupAmt + taxAmt;
  const marginPct     = total > 0 ? (grossProfit / total * 100).toFixed(1) : "0";
  const laborPct      = total > 0 ? (totLab / total * 100).toFixed(1) : "0";
  const effectiveRate = totHrs > 0 ? (total / totHrs).toFixed(0) : "0";

  // ── Request payment via Stripe Checkout ──
  const requestPayment = async () => {
    if (!hasItems || paymentLoading) return;
    if (!company.stripeKey) {
      setPaymentError("Add your Stripe Publishable Key in ⚙ Company Settings first.");
      return;
    }
    setPaymentLoading(true);
    setPaymentError("");
    try {
      const qn = quoteNumber || genQuoteNum();
      if (!quoteNumber) { setQuoteNumber(qn); saveQuote(); }

      const res = await fetch("/api/create-checkout", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          quoteNumber: qn,
          clientName,
          clientEmail,
          jobName,
          total,
          depositOnly,
          depositPercent,
          companyName: company.name,
          lineItems: activeItems.slice(0, 5).map(i => ({ label: i.label, amount: i.lineTotal })),
        }),
      });
      const data = await res.json();
      if (data.url) {
        window.open(data.url, "_blank");
      } else {
        setPaymentError(data.error || "Could not create checkout session.");
      }
    } catch (err) {
      setPaymentError("Network error — check your connection and try again.");
    } finally {
      setPaymentLoading(false);
    }
  };

  // ── Build plain-text quote ──
  const buildQuoteText = () => {
    const cBuyItems = activeItems.filter(i => i.cBuys);
    const iSupply   = activeItems.filter(i => !i.cBuys);
    const docType   = invoiceMode ? "INVOICE" : "ESTIMATE";
    return [
      "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
      company.name ? `  ${company.name.toUpperCase()}` : `  ELECTRICAL ${docType}`,
      company.phone ? `  ${company.phone}` : null,
      company.email ? `  ${company.email}` : null,
      company.address ? `  ${company.address}` : null,
      company.license ? `  License: ${company.license}` : null,
      "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
      quoteNumber && `${docType} #: ${quoteNumber}`,
      clientName && `Client:   ${clientName}`,
      clientPhone && `Phone:    ${clientPhone}`,
      clientEmail && `Email:    ${clientEmail}`,
      jobName    && `Job:      ${jobName}`,
      `Date:     ${new Date().toLocaleDateString()}`,
      invoiceMode && invoiceDueDate ? `Due:      ${invoiceDueDate}` : null,
      "",
      "SERVICES PROVIDED",
      "──────────────────────────────────",
      ...activeItems.map(i => flatRateMode
        ? `  • ${i.label}${i.variantLabel && i.variantLabel !== "Custom" ? ` (${i.variantLabel})` : ""} × ${i.qty}  — $${i.lineTotal.toLocaleString()}`
        : `  • ${i.label}${i.variantLabel && i.variantLabel !== "Custom" ? ` (${i.variantLabel})` : ""} × ${i.qty}`
      ),
      "",
      ...((!flatRateMode && iSupply.length) ? [
        "PARTS SUPPLIED BY US",
        "──────────────────────────────────",
        ...iSupply.map(i => `  ${i.label} × ${i.qty}  — $${i.mat.toLocaleString()}`),
        `  Materials subtotal: $${totMat.toLocaleString()}`, "",
      ] : []),
      ...((!flatRateMode && cBuyItems.length) ? [
        "CLIENT SUPPLIES PARTS (labor only billed)",
        "──────────────────────────────────",
        ...cBuyItems.map(i => `  ${i.label} × ${i.qty}  — est. $${i.mat.toLocaleString()} (not charged)`), "",
      ] : []),
      "COST BREAKDOWN",
      "──────────────────────────────────",
      (!flatRateMode && showMaterials) ? `  Materials:  $${totMat.toLocaleString()}` : null,
      !flatRateMode ? `  Labor:      $${totLab.toLocaleString()}  (${totHrs.toFixed(1)} hrs @ $${hourlyRate}/hr)` : null,
      !flatRateMode ? `  Markup:     $${markupAmt.toLocaleString()}  (${(markup*100).toFixed(0)}%)` : null,
      taxEnabled ? `  Tax (${(taxRate*100).toFixed(1)}% on materials): $${taxAmt.toLocaleString()}` : null,
      "──────────────────────────────────",
      `  TOTAL:      $${total.toLocaleString()}`,
      invoiceMode && invoicePaid ? "\n  ✓ PAID IN FULL" : null,
      notes ? `\nNotes:\n${notes}` : null,
      "",
      company.terms ? `TERMS:\n${company.terms}` : null,
      "",
      "Generated by Wireway · NEC 2023 Professional Estimating · wireway.cc",
    ].filter(Boolean).join("\n");
  };

  const copyQuote = () => {
    navigator.clipboard.writeText(buildQuoteText());
    setCopied(true); setTimeout(() => setCopied(false), 2500);
  };

  const emailQuote = () => {
    const docType = invoiceMode ? "Invoice" : "Estimate";
    const subject = encodeURIComponent(`Electrical ${docType}${clientName ? " for " + clientName : ""}${jobName ? " — " + jobName : ""}`);
    const body = encodeURIComponent(buildQuoteText());
    const to = clientEmail ? encodeURIComponent(clientEmail) : "";
    window.open(`mailto:${to}?subject=${subject}&body=${body}`);
  };

  const smsQuote = () => {
    const body = encodeURIComponent(
      `Hi ${clientName || "there"}, here's your electrical estimate from ${company.name || "us"}:\n\n` +
      activeItems.map(i => `• ${i.label} × ${i.qty}`).join("\n") +
      `\n\nTOTAL: $${total.toLocaleString()}\n\nCall us: ${company.phone || ""}\nwireway.cc`
    );
    const to = clientPhone ? clientPhone.replace(/\D/g, "") : "";
    window.open(`sms:${to}?body=${body}`);
  };

  // Material pull list
  const buildMaterialList = () => {
    const lines = [
      "WIREWAY — MATERIAL PULL LIST",
      `Job: ${jobName || "—"}  |  Date: ${new Date().toLocaleDateString()}`,
      "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
      ...activeItems.filter(i => !i.cBuys && i.mat > 0).map(i =>
        `□  ${i.label} (${i.variantLabel})  qty: ${i.qty}  est: $${i.mat.toLocaleString()}`
      ),
      "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
      `TOTAL MATERIAL COST: $${totMat.toLocaleString()}`,
    ].join("\n");
    navigator.clipboard.writeText(lines);
    setSaveMsg("Material list copied!"); setTimeout(() => setSaveMsg(""), 2000);
  };

  const TAB = (id, lbl) => (
    <button onClick={() => setTab(id)} style={{ flex:1, padding:"9px 6px", border:"none", cursor:"pointer", fontFamily:"'DM Sans',sans-serif", fontSize:11, fontWeight:600, letterSpacing:"0", transition:"all 0.2s", background: tab===id ? "rgba(232,201,122,0.08)" : "transparent", color: tab===id ? "#e8c97a" : "rgba(255,255,255,0.32)", borderBottom: tab===id ? "2px solid #e8c97a" : "2px solid transparent", whiteSpace:"nowrap" }}>{lbl}</button>
  );

  const inputStyle = { background:"rgba(255,255,255,0.04)", border:"1px solid rgba(255,255,255,0.07)", borderRadius:7, padding:"8px 11px", fontSize:13, color:"#fff", fontFamily:"inherit", width:"100%", transition:"border-color 0.15s" };
  const focusGold = e => e.target.style.borderColor = "rgba(232,201,122,0.4)";
  const blurGray  = e => e.target.style.borderColor = "rgba(255,255,255,0.07)";

  return (
    <>
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=DM+Mono:wght@400;500&family=Syne:wght@600;700;800&family=DM+Sans:wght@400;500;600&display=swap');
        *{box-sizing:border-box;margin:0;padding:0}
        body{background:#0a0a0c}
        ::-webkit-scrollbar{width:3px}
        ::-webkit-scrollbar-thumb{background:rgba(255,255,255,0.1);border-radius:3px}
        input,textarea,select{outline:none}
        input::placeholder,textarea::placeholder{color:rgba(255,255,255,0.18)}
        select option{background:#1a1a1e}
        @keyframes fadeUp{from{opacity:0;transform:translateY(10px)}to{opacity:1;transform:translateY(0)}}
        @keyframes modalIn{from{opacity:0;transform:scale(0.96) translateY(12px)}to{opacity:1;transform:scale(1) translateY(0)}}
        .modal-overlay{position:fixed;inset:0;background:rgba(0,0,0,0.75);backdrop-filter:blur(8px);z-index:200;display:flex;align-items:flex-start;justify-content:center;overflow-y:auto;padding:24px 16px}
        .modal-box{background:#111115;border:1px solid rgba(255,255,255,0.1);border-radius:18px;width:100%;max-width:600px;animation:modalIn 0.25s ease both;margin:auto}
        @media print{.no-print{display:none!important}.print-quote{background:#fff!important;color:#000!important;padding:32px!important}}
      `}</style>

      {/* ── DASHBOARD ── */}
      {showDashboard && (
        <div style={{ minHeight:"100vh", background:"radial-gradient(ellipse 80% 40% at 50% 0%,rgba(232,201,122,0.06) 0%,transparent 55%),#0a0a0c", fontFamily:"'DM Sans',sans-serif" }}>
          <div style={{ borderBottom:"1px solid rgba(255,255,255,0.06)", background:"rgba(10,10,12,0.9)", backdropFilter:"blur(20px)", position:"sticky", top:0, zIndex:100, padding:"0 20px" }}>
            <div style={{ maxWidth:680, margin:"0 auto", height:54, display:"flex", alignItems:"center", justifyContent:"space-between" }}>
              <div style={{ display:"flex", alignItems:"center", gap:10 }}>
                <img src="/logo192.png" alt="Wireway" style={{ height:30, width:30, borderRadius:7, objectFit:"cover" }} />
                <span style={{ fontFamily:"'Syne',sans-serif", fontSize:16, fontWeight:800, letterSpacing:"-0.02em" }}>
                  <span style={{ color:"#e8c97a" }}>WIRE</span>WAY
                </span>
              </div>
              <div style={{ display:"flex", gap:8 }}>
                {onShowPricing && profile?.subscription_status !== "active" && (
                  <button onClick={onShowPricing} style={{ padding:"5px 11px", borderRadius:6, background:"rgba(232,201,122,0.08)", border:"1px solid rgba(232,201,122,0.3)", color:"#e8c97a", fontSize:10, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>⚡ Upgrade</button>
                )}
                <button onClick={() => { newQuote(true); setShowDashboard(false); }} style={{ padding:"6px 14px", borderRadius:7, background:"rgba(232,201,122,0.1)", border:"1px solid rgba(232,201,122,0.3)", color:"#e8c97a", fontSize:11, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>+ New Estimate</button>
                <button onClick={() => setShowAccount(true)} style={{ padding:"6px 12px", borderRadius:7, border:"1px solid rgba(255,255,255,0.08)", background:"transparent", color:"rgba(255,255,255,0.45)", fontSize:11, fontWeight:600, cursor:"pointer", fontFamily:"inherit" }}>Account</button>
              </div>
            </div>
          </div>
          <div style={{ maxWidth:680, margin:"0 auto", padding:"20px 20px 60px" }}>
            <Dashboard
              user={user} profile={profile}
              onNewQuote={(job) => { newQuote(true); setJobName(job.label); setShowDashboard(false); setTab("services"); }}
              onLoadQuote={(q) => { loadQuote(q); setShowDashboard(false); }}
              onShowAI={() => { setShowDashboard(false); setTimeout(() => setShowAIBuilder(true), 100); }}
              onOpenCalendar={() => { setShowDashboard(false); setTimeout(() => setShowCalendar(true), 100); }}
            />
          </div>
        </div>
      )}

      {/* ── MAIN ESTIMATOR (hidden when dashboard shown) ── */}
      <div style={{ display: showDashboard ? "none" : "block" }}>
      <div style={{ minHeight:"100vh", background:"radial-gradient(ellipse 80% 45% at 50% -5%,rgba(232,201,122,0.065) 0%,transparent 55%),#0a0a0c", fontFamily:"'DM Sans',sans-serif", color:"#fff", paddingBottom:80 }}>

        {/* ── HEADER ── */}
        <div style={{ borderBottom:"1px solid rgba(255,255,255,0.055)", background:"rgba(10,10,12,0.88)", backdropFilter:"blur(20px)", position:"sticky", top:0, zIndex:100, padding:"0 20px" }} className="no-print">
          <div style={{ maxWidth:800, margin:"0 auto", height:54, display:"flex", alignItems:"center", justifyContent:"space-between" }}>
            <div style={{ display:"flex", alignItems:"center", gap:9 }}>
              {logoDataUrl
                ? <img src={logoDataUrl} alt="logo" style={{ height:32, width:"auto", borderRadius:6, objectFit:"contain" }} />
                : <img src="/logo192.png" alt="Wireway" style={{ height:32, width:32, borderRadius:6, objectFit:"contain" }} />
              }
              <span style={{ fontFamily:"'Syne',sans-serif", fontSize:16, fontWeight:800, letterSpacing:"-0.03em" }}>{company.name || "Wireway"}</span>
              <span style={{ fontSize:8, fontWeight:700, color:"rgba(232,201,122,0.6)", background:"rgba(232,201,122,0.07)", border:"1px solid rgba(232,201,122,0.16)", padding:"1px 5px", borderRadius:3, letterSpacing:"0.08em", textTransform:"uppercase" }}>NEC 2023</span>
            </div>
            <div style={{ display:"flex", alignItems:"center", gap:6 }}>
              {hasItems && (
                <span style={{ fontFamily:"'DM Mono',monospace", fontSize:16, fontWeight:600, color:"#e8c97a", letterSpacing:"-0.02em" }}>
                  ${total.toLocaleString()}
                </span>
              )}
              {onShowPricing && profile?.subscription_status !== "active" && (
                <button onClick={onShowPricing} style={{ padding:"5px 11px", borderRadius:6, background:"linear-gradient(135deg,rgba(232,201,122,0.18),rgba(232,201,122,0.06))", border:"1px solid rgba(232,201,122,0.3)", color:"#e8c97a", fontSize:10, fontWeight:700, cursor:"pointer", fontFamily:"inherit", whiteSpace:"nowrap" }}>
                  ⚡ {onTrial && daysLeft > 0 ? `${daysLeft}d left` : "Upgrade"}
                </button>
              )}
              <button onClick={() => setShowDashboard(true)} style={{ padding:"5px 9px", borderRadius:6, border:"1px solid rgba(255,255,255,0.08)", background:"transparent", color:"rgba(255,255,255,0.45)", fontSize:14, cursor:"pointer" }} title="Home">
                🏠
              </button>
              <button onClick={newQuote} style={{ padding:"5px 11px", borderRadius:6, border:"1px solid rgba(255,255,255,0.08)", background:"transparent", color:"rgba(255,255,255,0.45)", fontSize:11, fontWeight:600, cursor:"pointer", fontFamily:"inherit" }}>
                + New
              </button>
              {/* Combined account + company menu */}
              <div style={{ display:"flex", gap:1, background:"rgba(255,255,255,0.04)", border:"1px solid rgba(255,255,255,0.08)", borderRadius:7, overflow:"hidden" }}>
                <button onClick={() => setShowAccount(true)} style={{ padding:"5px 10px", border:"none", background:"transparent", color:"rgba(255,255,255,0.45)", fontSize:11, fontWeight:600, cursor:"pointer", fontFamily:"inherit", borderRight:"1px solid rgba(255,255,255,0.06)" }}>
                  Account
                </button>
                <button onClick={() => { setCompanyDraft(company); setLogoDataUrl(company.logoDataUrl||""); setEditingCompany(true); }} style={{ padding:"5px 10px", border:"none", background:"transparent", color:"rgba(255,255,255,0.45)", fontSize:11, fontWeight:600, cursor:"pointer", fontFamily:"inherit" }}>
                  Company
                </button>
              </div>
            </div>
          </div>
        </div>

        <div style={{ maxWidth:800, margin:"0 auto", padding:"0 20px" }}>

          {/* ── SPACER (replaces hero — cleaner on daily use) ── */}
          <div style={{ height:20 }} className="no-print" />

          {/* ── STATUS BANNERS — one at a time, stacked cleanly ── */}
          {(paymentSuccess || proGateMsg || (onTrial && daysLeft <= 30) || (!userIsPro && savedQuotes.length >= 3)) && (
            <div style={{ display:"flex", flexDirection:"column", gap:6, marginBottom:12 }} className="no-print">
              {paymentSuccess && (
                <div style={{ padding:"10px 14px", background:"rgba(100,220,130,0.07)", border:"1px solid rgba(100,220,130,0.2)", borderRadius:9, display:"flex", justifyContent:"space-between", alignItems:"center" }}>
                  <span style={{ fontSize:12, color:"#7dcea0", fontWeight:600 }}>
                    {paymentBanner === "pro" ? "🎉 Wireway Pro is now active." : "✓ Payment received — quote marked paid."}
                  </span>
                  <button onClick={() => { setPaymentSuccess(false); if(onClearBanner) onClearBanner(); }} style={{ background:"transparent", border:"none", color:"rgba(100,220,130,0.4)", fontSize:16, cursor:"pointer", padding:"0 4px" }}>✕</button>
                </div>
              )}
              {proGateMsg && (
                <div style={{ padding:"9px 14px", background:"rgba(232,201,122,0.06)", border:"1px solid rgba(232,201,122,0.2)", borderRadius:9, fontSize:11, color:"rgba(232,201,122,0.9)", fontWeight:600 }}>
                  ⚡ {proGateMsg}
                </div>
              )}
              {onTrial && daysLeft <= 30 && daysLeft > 0 && (
                <div style={{ padding:"9px 14px", background:"rgba(232,201,122,0.05)", border:"1px solid rgba(232,201,122,0.15)", borderRadius:9, display:"flex", justifyContent:"space-between", alignItems:"center" }}>
                  <span style={{ fontSize:11, color:"rgba(232,201,122,0.75)" }}>⏳ {daysLeft} day{daysLeft!==1?"s":""} remaining in your trial.</span>
                  {onShowPricing && <button onClick={onShowPricing} style={{ padding:"4px 10px", borderRadius:5, border:"1px solid rgba(232,201,122,0.35)", background:"rgba(232,201,122,0.1)", color:"#e8c97a", fontSize:10, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>Upgrade</button>}
                </div>
              )}
              {!userIsPro && savedQuotes.length >= 3 && (
                <div style={{ padding:"9px 14px", background:"rgba(232,126,126,0.05)", border:"1px solid rgba(232,126,126,0.15)", borderRadius:9, display:"flex", justifyContent:"space-between", alignItems:"center" }}>
                  <span style={{ fontSize:11, color:"rgba(232,126,126,0.75)" }}>Quote limit reached — upgrade for unlimited.</span>
                  {onShowPricing && <button onClick={onShowPricing} style={{ padding:"4px 10px", borderRadius:5, border:"1px solid rgba(232,126,126,0.35)", background:"rgba(232,126,126,0.08)", color:"#e87e7e", fontSize:10, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>Upgrade</button>}
                </div>
              )}
            </div>
          )}
          <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:6, marginBottom:10, animation:"fadeUp 0.4s ease 0.04s both" }} className="no-print">
            {[
              { ph:"Client name",   val:clientName,  set:setClientName },
              { ph:"Job / address", val:jobName,     set:setJobName },
              { ph:"Email",         val:clientEmail, set:setClientEmail },
              { ph:"Phone",         val:clientPhone, set:setClientPhone },
            ].map(f => (
              <input key={f.ph} placeholder={f.ph} value={f.val} onChange={e => f.set(e.target.value)}
                style={inputStyle} onFocus={focusGold} onBlur={blurGray} />
            ))}
          </div>

          {/* ── RATE SETTINGS ── */}
          <div style={{ background:"rgba(255,255,255,0.02)", border:"1px solid rgba(255,255,255,0.055)", borderRadius:12, padding:"12px 14px", marginBottom:10, animation:"fadeUp 0.4s ease 0.06s both" }} className="no-print">
            <div style={{ display:"flex", gap:16, flexWrap:"wrap" }}>
              <div style={{ flex:1, minWidth:200 }}>
                <div style={{ fontSize:10, color:"rgba(255,255,255,0.3)", marginBottom:7 }}>
                  Hourly — <span style={{ color:"#e8c97a", fontFamily:"'DM Mono',monospace", fontWeight:700 }}>${hourlyRate}/hr</span>
                </div>
                <div style={{ display:"flex", flexWrap:"wrap", gap:4 }}>
                  {HOURLY_RATES.map(r => <Pill key={r} label={`$${r}`} active={r===hourlyRate} onClick={() => setHourlyRate(r)} />)}
                </div>
              </div>
              <div style={{ flex:1, minWidth:150 }}>
                <div style={{ fontSize:10, color:"rgba(255,255,255,0.3)", marginBottom:7 }}>
                  Markup — <span style={{ color:"#e8c97a", fontFamily:"'DM Mono',monospace", fontWeight:700 }}>{(markup*100).toFixed(0)}%</span>
                </div>
                <div style={{ display:"flex", flexWrap:"wrap", gap:4 }}>
                  {MARKUP_OPTIONS.map(m => <Pill key={m.v} label={m.label} active={m.v===markup} onClick={() => setMarkup(m.v)} />)}
                </div>
              </div>
            </div>
          </div>

          {/* ── UNIFIED SETTINGS + TOOLS PANEL ── */}
          <div style={{ background:"rgba(255,255,255,0.02)", border:"1px solid rgba(255,255,255,0.055)", borderRadius:12, padding:"12px 14px", marginBottom:12, animation:"fadeUp 0.4s ease 0.08s both" }} className="no-print">

            {/* Row 1: toggles */}
            <div style={{ display:"flex", gap:5, flexWrap:"wrap", marginBottom:10 }}>
              {[
                { label: showMaterials ? "Mat · On" : "Mat · Off",     active: showMaterials,  action: () => setShowMaterials(v=>!v),  color:"#e8c97a" },
                { label: clientBuysAll ? "Client buys" : "You supply", active: clientBuysAll,  action: () => setClientBuysAll(v=>!v), color:"#7ec8e8" },
                { label: flatRateMode  ? "Flat rate" : "Itemized",     active: flatRateMode,   action: () => setFlatRateMode(v=>!v),  color:"#a8e87e" },
                { label: invoiceMode   ? "Invoice" : "Estimate",       active: invoiceMode,    action: () => setInvoiceMode(v=>!v),   color:"#b87ee8" },
                { label: taxEnabled    ? `Tax ${(taxRate*100).toFixed(0)}%` : "Add tax", active: taxEnabled, action: () => setTaxEnabled(v=>!v), color:"#e8b87e" },
              ].map(t => (
                <button key={t.label} onClick={t.action} style={{ padding:"4px 10px", borderRadius:6, fontSize:10, fontWeight:700, border: t.active ? `1px solid ${t.color}40` : "1px solid rgba(255,255,255,0.07)", background: t.active ? `${t.color}12` : "transparent", color: t.active ? t.color : "rgba(255,255,255,0.35)", cursor:"pointer", fontFamily:"inherit", transition:"all 0.15s" }}>
                  {t.label}
                </button>
              ))}
            </div>

            {/* Conditional: tax rate or invoice due */}
            {(taxEnabled || invoiceMode) && (
              <div style={{ display:"flex", gap:10, flexWrap:"wrap", marginBottom:10, paddingTop:8, borderTop:"1px solid rgba(255,255,255,0.05)" }}>
                {taxEnabled && (
                  <div style={{ display:"flex", alignItems:"center", gap:5 }}>
                    <span style={{ fontSize:10, color:"rgba(255,255,255,0.35)" }}>Rate:</span>
                    {[0.05,0.06,0.07,0.08,0.09,0.10].map(r => (
                      <button key={r} onClick={() => setTaxRate(r)} style={{ padding:"3px 7px", borderRadius:4, fontSize:10, fontWeight:700, border: r===taxRate ? "1px solid rgba(232,184,126,0.5)" : "1px solid rgba(255,255,255,0.07)", background: r===taxRate ? "rgba(232,184,126,0.15)" : "transparent", color: r===taxRate ? "#e8b87e" : "rgba(255,255,255,0.3)", cursor:"pointer", fontFamily:"'DM Mono',monospace" }}>{(r*100).toFixed(0)}%</button>
                    ))}
                  </div>
                )}
                {invoiceMode && (
                  <div style={{ display:"flex", alignItems:"center", gap:6 }}>
                    <span style={{ fontSize:10, color:"rgba(255,255,255,0.35)" }}>Due:</span>
                    <input type="date" value={invoiceDueDate} onChange={e => setInvoiceDueDate(e.target.value)} style={{ ...inputStyle, width:"auto", fontSize:11, padding:"3px 8px", colorScheme:"dark" }} onFocus={focusGold} onBlur={blurGray} />
                    <button onClick={() => setInvoicePaid(v => !v)} style={{ padding:"3px 9px", borderRadius:5, border: invoicePaid ? "1px solid rgba(100,220,130,0.4)" : "1px solid rgba(255,255,255,0.07)", background: invoicePaid ? "rgba(100,220,130,0.1)" : "transparent", color: invoicePaid ? "#7dcea0" : "rgba(255,255,255,0.35)", fontSize:10, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>
                      {invoicePaid ? "✓ Paid" : "Mark paid"}
                    </button>
                  </div>
                )}
              </div>
            )}

            {/* Row 2: tools — subtle divider separates from toggles */}
            <div style={{ display:"flex", gap:5, flexWrap:"wrap", paddingTop:8, borderTop:"1px solid rgba(255,255,255,0.05)" }}>
              {[
                { label:"⚡ AI Quote", action:() => setShowAIBuilder(true),  highlight: true },
              { label:"📅 Calendar", action:() => setShowCalendar(true)   },
              { label:"Wire Calc",  action:() => setWireCalcOpen(true)  },
                { label:"Load Calc",  action:() => setLoadCalcOpen(true)  },
                { label:"Checklist",  action:() => setChecklistOpen(true) },
                { label:"Clients",    action:() => setShowClientDB(true)  },
                { label:"+ Custom",   action:addCustomItem                },
                hasItems ? { label:"Pull List", action:buildMaterialList } : null,
              ].filter(Boolean).map(btn => (
                <button key={btn.label} onClick={btn.action} style={{ padding:"4px 10px", borderRadius:6, fontSize:10, fontWeight: btn.highlight ? 700 : 600, border: btn.highlight ? "1px solid rgba(232,201,122,0.3)" : "1px solid rgba(255,255,255,0.07)", background: btn.highlight ? "rgba(232,201,122,0.08)" : "transparent", color: btn.highlight ? "#e8c97a" : "rgba(255,255,255,0.45)", cursor:"pointer", fontFamily:"inherit", transition:"all 0.15s" }}
                  onMouseEnter={e => { e.currentTarget.style.background = btn.highlight ? "rgba(232,201,122,0.15)" : "rgba(255,255,255,0.06)"; e.currentTarget.style.color = btn.highlight ? "#e8c97a" : "#fff"; }}
                  onMouseLeave={e => { e.currentTarget.style.background = btn.highlight ? "rgba(232,201,122,0.08)" : "transparent"; e.currentTarget.style.color = btn.highlight ? "#e8c97a" : "rgba(255,255,255,0.45)"; }}>
                  {btn.label}
                </button>
              ))}
            </div>
          </div>

          {/* Custom line items */}
          {customItems.length > 0 && (
            <div style={{ background:"rgba(232,201,122,0.04)", border:"1px solid rgba(232,201,122,0.12)", borderRadius:12, padding:"14px 16px", marginBottom:14 }} className="no-print">
              <div style={{ fontSize:10, color:"rgba(232,201,122,0.6)", textTransform:"uppercase", letterSpacing:"0.1em", marginBottom:10 }}>Custom Line Items</div>
              {customItems.map(item => (
                <div key={item.id} style={{ display:"grid", gridTemplateColumns:"1fr 55px 75px 75px 65px 30px", gap:5, marginBottom:7, alignItems:"center" }}>
                  <input placeholder="Description" value={item.label} onChange={e => updateCustomItem(item.id, { label:e.target.value })} style={{ ...inputStyle, fontSize:12 }} onFocus={focusGold} onBlur={blurGray} />
                  <input placeholder="Qty" type="number" min="1" value={item.qty} onChange={e => updateCustomItem(item.id, { qty:Number(e.target.value) })} style={{ ...inputStyle, fontSize:12 }} onFocus={focusGold} onBlur={blurGray} />
                  <input placeholder="Mat $" type="number" min="0" value={item.materialCost||""} onChange={e => updateCustomItem(item.id, { materialCost:Number(e.target.value) })} style={{ ...inputStyle, fontSize:12 }} onFocus={focusGold} onBlur={blurGray} />
                  <input placeholder="Lab $" type="number" min="0" value={item.laborCost||""} onChange={e => updateCustomItem(item.id, { laborCost:Number(e.target.value) })} style={{ ...inputStyle, fontSize:12 }} onFocus={focusGold} onBlur={blurGray} />
                  <input placeholder="Hrs" type="number" min="0" step="0.25" value={item.laborHours||""} onChange={e => updateCustomItem(item.id, { laborHours:Number(e.target.value) })} style={{ ...inputStyle, fontSize:12 }} onFocus={focusGold} onBlur={blurGray} />
                  <button onClick={() => removeCustomItem(item.id)} style={{ width:30, height:30, borderRadius:6, border:"1px solid rgba(255,100,100,0.2)", background:"rgba(255,100,100,0.06)", color:"rgba(255,100,100,0.5)", fontSize:14, cursor:"pointer", display:"flex", alignItems:"center", justifyContent:"center" }}>✕</button>
                </div>
              ))}
            </div>
          )}

          {/* ── TABS ── */}
          <div style={{ display:"flex", background:"rgba(255,255,255,0.025)", borderRadius:9, border:"1px solid rgba(255,255,255,0.065)", overflow:"hidden", marginBottom:14, animation:"fadeUp 0.4s ease 0.16s both" }} className="no-print">
            {TAB("services", "Services")}
            {TAB("summary",  hasItems ? `Summary $${total.toLocaleString()}` : "Summary")}
            {TAB("saved",    `Saved (${savedQuotes.length})`)}
            {TAB("profit",   "Profit")}
            {TAB("nec",      "NEC 2023")}
            {onShowPricing && profile?.subscription_status !== "active" && (
              <button onClick={onShowPricing} style={{ padding:"9px 10px", border:"none", background:"linear-gradient(135deg,rgba(232,201,122,0.18),rgba(232,201,122,0.06))", color:"#e8c97a", fontSize:11, fontWeight:800, cursor:"pointer", fontFamily:"'Syne',sans-serif", borderLeft:"1px solid rgba(232,201,122,0.2)", letterSpacing:"-0.01em", whiteSpace:"nowrap" }}>
                ⚡ {onTrial ? `${daysLeft}d left` : "Upgrade"}
              </button>
            )}
          </div>

          {/* ════════════ SERVICES TAB ════════════ */}
          {tab==="services" && (
            <div style={{ animation:"fadeUp 0.3s ease both" }} className="no-print">
              {CATEGORIES.map(cat => (
                <CategorySection key={cat.id} category={cat} entries={entries} onUpdate={upd}
                  hourlyRate={hourlyRate} clientBuys={clientBuysAll} showMaterials={showMaterials} />
              ))}
            </div>
          )}

          {/* ════════════ SUMMARY TAB ════════════ */}
          {tab==="summary" && (
            <div style={{ animation:"fadeUp 0.3s ease both" }}>
              {!hasItems ? (
                <div style={{ textAlign:"center", padding:"52px 20px 40px" }} className="no-print">
                  <div style={{ fontSize:13, color:"rgba(255,255,255,0.25)", marginBottom:16 }}>No services added yet</div>
                  <button onClick={() => setTab("services")} style={{ padding:"9px 20px", background:"rgba(232,201,122,0.08)", border:"1px solid rgba(232,201,122,0.22)", borderRadius:8, color:"#e8c97a", fontSize:12, fontWeight:600, cursor:"pointer", fontFamily:"inherit" }}>
                    Browse Services →
                  </button>
                </div>
              ) : (
                <>
                  {/* ── STAT CARDS ── */}
                  <div style={{ display:"flex", gap:8, flexWrap:"wrap", marginBottom:16 }} className="no-print">
                    <StatCard label="Total Estimate"      value={`$${total.toLocaleString()}`}           color="#e8c97a" />
                    <StatCard label="Labor Only"          value={`$${totLab.toLocaleString()}`}          color="#a8e87e" sub={`${totHrs.toFixed(1)} hrs @ $${hourlyRate}/hr`} />
                    {showMaterials && <StatCard label="Materials (You)"    value={`$${totMat.toLocaleString()}`}          color="#7eb8e8" />}
                    {totClientBuysMat > 0 && <StatCard label="Client Buys (est.)" value={`$${totClientBuysMat.toLocaleString()}`} color="#e87eb8" sub="not charged to you" />}
                  </div>

                  {/* ── QUOTE PREVIEW CARD ── */}
                  <div style={{ background:"rgba(255,255,255,0.022)", border:"1px solid rgba(255,255,255,0.065)", borderRadius:13, padding:"20px", marginBottom:12 }} className="print-quote">
                    {/* Company header */}
                    <div style={{ display:"flex", alignItems:"center", gap:14, marginBottom:16, paddingBottom:16, borderBottom:"1px solid rgba(255,255,255,0.07)" }}>
                      {logoDataUrl
                        ? <img src={logoDataUrl} alt="logo" style={{ height:48, width:"auto", maxWidth:120, objectFit:"contain", borderRadius:6 }} />
                        : <img src="/logo192.png" alt="Wireway" style={{ height:48, width:48, borderRadius:8, objectFit:"contain" }} />
                      }
                      <div style={{ flex:1 }}>
                        <div style={{ fontFamily:"'Syne',sans-serif", fontSize:15, fontWeight:800, color:"#fff", letterSpacing:"-0.02em" }}>{company.name || "Your Company Name"}</div>
                        <div style={{ fontSize:11, color:"rgba(255,255,255,0.4)", marginTop:2, lineHeight:1.6 }}>
                          {[company.phone, company.email, company.address, company.license && `Lic: ${company.license}`].filter(Boolean).join("  ·  ")}
                        </div>
                      </div>
                      <div style={{ textAlign:"right", flexShrink:0 }}>
                        {quoteNumber && <div style={{ fontFamily:"'DM Mono',monospace", fontSize:11, fontWeight:700, color:"#e8c97a", marginBottom:3 }}>{quoteNumber}</div>}
                        <div style={{ fontSize:9, color:"rgba(255,255,255,0.3)", textTransform:"uppercase", letterSpacing:"0.08em" }}>Date</div>
                        <div style={{ fontSize:12, color:"rgba(255,255,255,0.6)", fontFamily:"'DM Mono',monospace" }}>{new Date().toLocaleDateString()}</div>
                        {currentQuoteStatus?.status === "accepted" && (
                          <div style={{ marginTop:4, fontSize:9, fontWeight:700, color:"#7dcea0", background:"rgba(100,220,130,0.1)", border:"1px solid rgba(100,220,130,0.25)", padding:"2px 6px", borderRadius:4 }}>✓ ACCEPTED</div>
                        )}
                      </div>
                    </div>

                    {/* Client info */}
                    {(clientName || jobName || clientEmail || clientPhone) && (
                      <div style={{ background:"rgba(255,255,255,0.03)", borderRadius:8, padding:"10px 12px", marginBottom:14, display:"grid", gridTemplateColumns:"1fr 1fr", gap:"4px 16px" }}>
                        {clientName  && <div style={{ fontSize:12 }}><span style={{ color:"rgba(255,255,255,0.35)" }}>Client: </span><span style={{ color:"#fff", fontWeight:600 }}>{clientName}</span></div>}
                        {jobName     && <div style={{ fontSize:12 }}><span style={{ color:"rgba(255,255,255,0.35)" }}>Job: </span><span style={{ color:"rgba(255,255,255,0.7)" }}>{jobName}</span></div>}
                        {clientEmail && <div style={{ fontSize:12 }}><span style={{ color:"rgba(255,255,255,0.35)" }}>Email: </span><span style={{ color:"rgba(255,255,255,0.7)" }}>{clientEmail}</span></div>}
                        {clientPhone && <div style={{ fontSize:12 }}><span style={{ color:"rgba(255,255,255,0.35)" }}>Phone: </span><span style={{ color:"rgba(255,255,255,0.7)" }}>{clientPhone}</span></div>}
                      </div>
                    )}

                    {/* Services provided */}
                    <div style={{ fontSize:9, color:"rgba(255,255,255,0.28)", textTransform:"uppercase", letterSpacing:"0.1em", marginBottom:10 }}>Services Provided</div>
                    {CATEGORIES.map(cat => {
                      const items = activeItems.filter(i => cat.services.find(s => s.id===i.id));
                      if (!items.length) return null;
                      return (
                        <div key={cat.id} style={{ marginBottom:12 }}>
                          <div style={{ fontSize:9, color:cat.color, textTransform:"uppercase", letterSpacing:"0.1em", fontWeight:700, marginBottom:5, opacity:0.85 }}>{cat.label}</div>
                          {items.map(item => (
                            <div key={item.id} style={{ display:"flex", alignItems:"flex-start", gap:8, padding:"6px 0", borderBottom:"1px solid rgba(255,255,255,0.04)" }}>
                              <div style={{ flex:1 }}>
                                <div style={{ fontSize:12, color:"rgba(255,255,255,0.78)", fontWeight:600 }}>{item.label}</div>
                                <div style={{ fontSize:10, color:"rgba(255,255,255,0.3)", fontFamily:"'DM Mono',monospace", marginTop:1 }}>
                                  {item.variantLabel} · {item.nec} · qty {item.qty}
                                  {item.cBuys ? <span style={{ color:"#7ec8e8", marginLeft:5 }}>· client supplies parts</span> : ""}
                                </div>
                              </div>
                              <div style={{ display:"flex", flexDirection:"column", alignItems:"flex-end", gap:1, flexShrink:0 }}>
                                <span style={{ fontSize:10, color:"rgba(162,220,160,0.75)", fontFamily:"'DM Mono',monospace" }}>lab ${item.lab.toLocaleString()}</span>
                                {showMaterials && !item.cBuys && <span style={{ fontSize:9, color:"rgba(255,255,255,0.28)", fontFamily:"'DM Mono',monospace" }}>mat ${item.mat.toLocaleString()}</span>}
                                <span style={{ fontSize:12, fontWeight:700, color:cat.color, fontFamily:"'DM Mono',monospace" }}>${item.lineTotal.toLocaleString()}</span>
                              </div>
                            </div>
                          ))}
                        </div>
                      );
                    })}

                    {/* Totals */}
                    <div style={{ background:"linear-gradient(135deg,rgba(232,201,122,0.065) 0%,rgba(255,255,255,0.018) 100%)", border:"1px solid rgba(232,201,122,0.18)", borderRadius:10, padding:"14px 16px", marginTop:8 }}>
                      {[
                        showMaterials && { label:"Materials (supplied by us)", val:totMat, color:"#7eb8e8" },
                        { label:`Labor — ${totHrs.toFixed(1)} hrs @ $${hourlyRate}/hr`, val:totLab, color:"#a8e87e" },
                        { label:`Markup — ${(markup*100).toFixed(0)}%`, val:markupAmt, color:"rgba(255,255,255,0.4)" },
                      ].filter(Boolean).map(row => (
                        <div key={row.label} style={{ display:"flex", justifyContent:"space-between", padding:"4px 0", fontSize:11 }}>
                          <span style={{ color:"rgba(255,255,255,0.38)" }}>{row.label}</span>
                          <span style={{ fontFamily:"'DM Mono',monospace", color:row.color, fontWeight:600 }}>${row.val.toLocaleString()}</span>
                        </div>
                      ))}
                      {totClientBuysMat > 0 && (
                        <div style={{ display:"flex", justifyContent:"space-between", padding:"4px 0", fontSize:11 }}>
                          <span style={{ color:"rgba(126,200,232,0.45)" }}>Client supplies parts (not charged)</span>
                          <span style={{ fontFamily:"'DM Mono',monospace", color:"rgba(126,200,232,0.45)" }}>~${totClientBuysMat.toLocaleString()}</span>
                        </div>
                      )}
                      <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", paddingTop:12, marginTop:6, borderTop:"1px solid rgba(232,201,122,0.18)" }}>
                        <span style={{ fontFamily:"'Syne',sans-serif", fontSize:14, fontWeight:800, color:"#fff" }}>Total Estimate</span>
                        <span style={{ fontFamily:"'DM Mono',monospace", fontSize:22, fontWeight:500, color:"#e8c97a", letterSpacing:"-0.03em" }}>${total.toLocaleString()}</span>
                      </div>
                    </div>

                    {/* Notes */}
                    {notes && (
                      <div style={{ marginTop:12, padding:"10px 12px", background:"rgba(255,255,255,0.03)", borderRadius:8, fontSize:11, color:"rgba(255,255,255,0.5)", lineHeight:1.6 }}>
                        <span style={{ color:"rgba(255,255,255,0.3)", textTransform:"uppercase", letterSpacing:"0.08em", fontSize:9 }}>Notes: </span>{notes}
                      </div>
                    )}

                    {/* Terms */}
                    {company.terms && (
                      <div style={{ marginTop:10, padding:"10px 12px", background:"rgba(255,255,255,0.02)", borderRadius:8, fontSize:10, color:"rgba(255,255,255,0.3)", lineHeight:1.7 }}>
                        <div style={{ color:"rgba(255,255,255,0.2)", textTransform:"uppercase", letterSpacing:"0.08em", fontSize:9, marginBottom:4 }}>Terms & Conditions</div>
                        {company.terms}
                      </div>
                    )}
                  </div>

                  {/* ── NOTES INPUT ── */}
                  <textarea placeholder="Job notes, permit info, scope exclusions, warranty..." value={notes} onChange={e => setNotes(e.target.value)} rows={2}
                    style={{ ...inputStyle, width:"100%", marginBottom:10, resize:"vertical", lineHeight:1.6 }} className="no-print"
                    onFocus={focusGold} onBlur={blurGray} />

                  {/* ── QUOTE NUMBER + STATUS ── */}
                  <div style={{ display:"flex", alignItems:"center", gap:8, marginBottom:10 }} className="no-print">
                    <input placeholder="Quote # (auto-generated on save)" value={quoteNumber} onChange={e => setQuoteNumber(e.target.value)}
                      style={{ ...inputStyle, flex:1, fontSize:12 }} onFocus={focusGold} onBlur={blurGray} />
                    {currentQuoteStatus?.status === "accepted" && (
                      <div style={{ display:"flex", alignItems:"center", gap:5, padding:"6px 10px", background:"rgba(100,220,130,0.1)", border:"1px solid rgba(100,220,130,0.3)", borderRadius:7, flexShrink:0 }}>
                        <span style={{ fontSize:11 }}>✓</span>
                        <span style={{ fontSize:10, color:"#7dcea0", fontWeight:700 }}>ACCEPTED</span>
                      </div>
                    )}
                  </div>

                  {/* ── SAVE + SIGN BUTTONS ── */}
                  <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:8, marginBottom:10 }} className="no-print">
                    <button onClick={() => { if (!userIsPro && savedQuotes.length >= 3) { if(onShowPricing) onShowPricing(); return; } saveQuote(); }} style={{ padding:"12px", background: saveMsg ? "rgba(100,220,130,0.1)" : "linear-gradient(135deg,rgba(232,201,122,0.18),rgba(232,201,122,0.07))", border: saveMsg ? "1px solid rgba(100,220,130,0.38)" : "1px solid rgba(232,201,122,0.35)", borderRadius:10, color: saveMsg ? "#7dcea0" : "#e8c97a", fontSize:13, fontWeight:700, cursor:"pointer", fontFamily:"inherit", transition:"all 0.2s" }}>
                      {saveMsg ? "✓ Saved" : "💾 Save Quote"}
                    </button>
                    <button onClick={() => { setSigName(""); setSigDate(new Date().toLocaleDateString()); setSigSaved(false); setSignModal(true); }}
                      style={{ padding:"12px", background: currentQuoteStatus?.status === "accepted" ? "rgba(100,220,130,0.1)" : "rgba(255,255,255,0.04)", border: currentQuoteStatus?.status === "accepted" ? "1px solid rgba(100,220,130,0.35)" : "1px solid rgba(255,255,255,0.1)", borderRadius:10, color: currentQuoteStatus?.status === "accepted" ? "#7dcea0" : "rgba(255,255,255,0.5)", fontSize:13, fontWeight:700, cursor:"pointer", fontFamily:"inherit", transition:"all 0.2s" }}>
                      {currentQuoteStatus?.status === "accepted" ? "✓ Client Signed" : "✍ Client Signature"}
                    </button>
                  </div>

                  {/* ── SEND ACTIONS ── */}
                  <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr 1fr 1fr", gap:8, marginBottom:8 }} className="no-print">
                    {[
                      { icon:"✉", label:"Email",      desc: clientEmail || "Mail app",    action: emailQuote,  color:"#7eb8e8" },
                      { icon:"💬", label:"Text",       desc: clientPhone || "Messages",    action: smsQuote,    color:"#a8e87e" },
                      { icon:"⎘",  label:"Copy",       desc: copied ? "Copied!" : "Text", action: copyQuote,   color: copied ? "#7dcea0" : "#e8c97a" },
                      { icon:"🔗", label:"Share Link", desc: quoteId ? "Client link" : "Save first", action: () => {
                        if (!quoteId) { setSaveMsg("Save the quote first."); setTimeout(() => setSaveMsg(""), 2000); return; }
                        const link = `${window.location.origin}/quote/${quoteId}`;
                        navigator.clipboard.writeText(link);
                        setSaveMsg("Link copied!"); setTimeout(() => setSaveMsg(""), 2500);
                      }, color:"#b87ee8" },
                    ].map(btn => (
                      <button key={btn.label} onClick={btn.action} style={{ display:"flex", flexDirection:"column", alignItems:"center", gap:4, padding:"11px 4px", borderRadius:11, border:`1px solid ${btn.color}25`, background:`${btn.color}08`, cursor:"pointer", fontFamily:"inherit", transition:"all 0.2s" }}
                        onMouseEnter={e => e.currentTarget.style.background=`${btn.color}15`}
                        onMouseLeave={e => e.currentTarget.style.background=`${btn.color}08`}>
                        <span style={{ fontSize:16 }}>{btn.icon}</span>
                        <span style={{ fontSize:10, fontWeight:700, color:btn.color }}>{btn.label}</span>
                        <span style={{ fontSize:9, color:"rgba(255,255,255,0.3)" }}>{btn.desc}</span>
                      </button>
                    ))}
                  </div>

                  <button onClick={() => window.print()} style={{ width:"100%", padding:"11px", background:"rgba(255,255,255,0.04)", border:"1px solid rgba(255,255,255,0.1)", borderRadius:10, color:"rgba(255,255,255,0.5)", fontSize:12, fontWeight:600, cursor:"pointer", fontFamily:"inherit", transition:"all 0.2s" }} className="no-print"
                    onMouseEnter={e => e.currentTarget.style.background="rgba(255,255,255,0.08)"}
                    onMouseLeave={e => e.currentTarget.style.background="rgba(255,255,255,0.04)"}>
                    🖨 Print / Save as PDF
                  </button>

                  {/* ── ON MY WAY + REVIEW ── */}
                  <div style={{ display:"flex", flexDirection:"column", gap:6, marginTop:2 }} className="no-print">
                    <OnMyWayButton
                      clientName={clientName} clientPhone={clientPhone}
                      jobAddress={jobName} companyName={company.name}
                    />
                    {currentQuoteStatus?.status === "accepted" || currentQuoteStatus?.status === "paid" ? (
                      <ReviewRequestButton
                        clientName={clientName} clientPhone={clientPhone}
                        companyName={company.name} reviewUrl={company.reviewUrl}
                      />
                    ) : null}
                    <AutoInvoiceButton
                      isInvoiceMode={invoiceMode}
                      onConvert={() => { setInvoiceMode(true); setInvoiceDueDate(new Date(Date.now()+30*86400000).toISOString().split("T")[0]); }}
                    />
                    <QuickBooksExport
                      quote={{ clientName, jobName, quoteNumber, invoiceDueDate }}
                      company={company}
                      activeItems={activeItems}
                      total={total} totLab={totLab} totMat={totMat}
                      markupAmt={markupAmt} taxAmt={taxAmt}
                      taxRate={taxRate} taxEnabled={taxEnabled}
                    />
                  </div>

                  {/* ── PHOTO ATTACHMENTS ── */}
                  {quoteId && (
                    <div style={{ marginTop:14, padding:"14px 16px", background:"rgba(255,255,255,0.02)", border:"1px solid rgba(255,255,255,0.055)", borderRadius:12 }} className="no-print">
                      <PhotoAttachments user={user} quoteId={quoteId} />
                    </div>
                  )}

                  {/* ── UPGRADE PROMPT (trial / free users) ── */}
                  {(!userIsPro || onTrial) && onShowPricing && (
                    <div style={{ marginTop:16, background:"linear-gradient(135deg,rgba(232,201,122,0.08) 0%,rgba(99,102,241,0.06) 100%)", border:"1px solid rgba(232,201,122,0.2)", borderRadius:14, padding:"20px", textAlign:"center" }} className="no-print">
                      <div style={{ fontSize:20, marginBottom:8 }}>⚡</div>
                      <div style={{ fontFamily:"'Syne',sans-serif", fontSize:15, fontWeight:800, color:"#fff", marginBottom:6, letterSpacing:"-0.02em" }}>
                        {onTrial ? `${daysLeft} days left in your free trial` : "Unlock Pro features"}
                      </div>
                      <div style={{ fontSize:12, color:"rgba(255,255,255,0.4)", marginBottom:16, lineHeight:1.6 }}>
                        Client payments · Unlimited quotes · Signatures · Invoice mode · Profit analysis · and more
                      </div>
                      <div style={{ display:"flex", gap:8, justifyContent:"center", flexWrap:"wrap" }}>
                        <button onClick={onShowPricing} style={{ padding:"11px 24px", background:"linear-gradient(135deg,rgba(232,201,122,0.25),rgba(232,201,122,0.1))", border:"1px solid rgba(232,201,122,0.4)", borderRadius:9, color:"#e8c97a", fontSize:13, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>
                          Upgrade to Pro — $12/mo
                        </button>
                        <button onClick={onShowPricing} style={{ padding:"11px 20px", background:"rgba(255,255,255,0.04)", border:"1px solid rgba(255,255,255,0.1)", borderRadius:9, color:"rgba(255,255,255,0.4)", fontSize:12, fontWeight:600, cursor:"pointer", fontFamily:"inherit" }}>
                          View all plans
                        </button>
                      </div>
                      <div style={{ fontSize:10, color:"rgba(255,255,255,0.2)", marginTop:10 }}>
                        30-day free trial · No credit card required · Cancel anytime
                      </div>
                    </div>
                  )}

                  {/* ── STRIPE PAYMENT PANEL ── */}
                  <div style={{ marginTop:14, background:"linear-gradient(135deg,rgba(99,102,241,0.08) 0%,rgba(139,92,246,0.05) 100%)", border:"1px solid rgba(99,102,241,0.2)", borderRadius:13, padding:"18px 18px 14px" }} className="no-print">
                    <div style={{ display:"flex", alignItems:"center", gap:8, marginBottom:14 }}>
                      <div style={{ width:28, height:28, borderRadius:6, background:"rgba(99,102,241,0.2)", display:"flex", alignItems:"center", justifyContent:"center", fontSize:14 }}>⚡</div>
                      <div>
                        <div style={{ fontFamily:"'Syne',sans-serif", fontSize:13, fontWeight:800, color:"#fff" }}>Request Payment via Stripe</div>
                        <div style={{ fontSize:10, color:"rgba(255,255,255,0.35)" }}>Client pays online — card or bank · 2.9% + 30¢ per transaction</div>
                      </div>
                    </div>

                    {/* Deposit vs full toggle */}
                    <div style={{ display:"flex", gap:6, marginBottom:12 }}>
                      <button onClick={() => setDepositOnly(true)} style={{ flex:1, padding:"8px", borderRadius:8, border: depositOnly ? "1px solid rgba(99,102,241,0.5)" : "1px solid rgba(255,255,255,0.08)", background: depositOnly ? "rgba(99,102,241,0.15)" : "rgba(255,255,255,0.03)", color: depositOnly ? "#818cf8" : "rgba(255,255,255,0.4)", fontSize:11, fontWeight:700, cursor:"pointer", fontFamily:"inherit", transition:"all 0.15s" }}>
                        Deposit Only
                      </button>
                      <button onClick={() => setDepositOnly(false)} style={{ flex:1, padding:"8px", borderRadius:8, border: !depositOnly ? "1px solid rgba(99,102,241,0.5)" : "1px solid rgba(255,255,255,0.08)", background: !depositOnly ? "rgba(99,102,241,0.15)" : "rgba(255,255,255,0.03)", color: !depositOnly ? "#818cf8" : "rgba(255,255,255,0.4)", fontSize:11, fontWeight:700, cursor:"pointer", fontFamily:"inherit", transition:"all 0.15s" }}>
                        Full Amount
                      </button>
                    </div>

                    {/* Deposit percentage selector */}
                    {depositOnly && (
                      <div style={{ display:"flex", gap:5, marginBottom:12 }}>
                        <span style={{ fontSize:10, color:"rgba(255,255,255,0.35)", alignSelf:"center", flexShrink:0 }}>Deposit %:</span>
                        {[25, 33, 50, 75].map(pct => (
                          <button key={pct} onClick={() => setDepositPercent(pct)} style={{ flex:1, padding:"6px", borderRadius:6, border: depositPercent===pct ? "1px solid rgba(99,102,241,0.5)" : "1px solid rgba(255,255,255,0.08)", background: depositPercent===pct ? "rgba(99,102,241,0.12)" : "rgba(255,255,255,0.03)", color: depositPercent===pct ? "#818cf8" : "rgba(255,255,255,0.4)", fontSize:11, fontWeight:700, cursor:"pointer", fontFamily:"'DM Mono',monospace" }}>{pct}%</button>
                        ))}
                      </div>
                    )}

                    {/* Amount preview */}
                    <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", padding:"8px 12px", background:"rgba(255,255,255,0.04)", borderRadius:8, marginBottom:12 }}>
                      <span style={{ fontSize:11, color:"rgba(255,255,255,0.4)" }}>
                        {depositOnly ? `${depositPercent}% deposit` : "Full payment"} to charge:
                      </span>
                      <span style={{ fontFamily:"'DM Mono',monospace", fontSize:16, fontWeight:700, color:"#818cf8" }}>
                        ${depositOnly ? Math.round(total * depositPercent / 100).toLocaleString() : total.toLocaleString()}
                      </span>
                    </div>

                    {/* Error message */}
                    {paymentError && (
                      <div style={{ fontSize:11, color:"#e87e7e", background:"rgba(232,126,126,0.08)", border:"1px solid rgba(232,126,126,0.2)", borderRadius:7, padding:"8px 10px", marginBottom:10, lineHeight:1.5 }}>
                        ⚠ {paymentError}
                      </div>
                    )}

                    {/* Success message */}
                    {paymentSuccess && (
                      <div style={{ fontSize:11, color:"#7dcea0", background:"rgba(100,220,130,0.08)", border:"1px solid rgba(100,220,130,0.2)", borderRadius:7, padding:"8px 10px", marginBottom:10 }}>
                        ✓ Payment received! Quote marked as paid.
                      </div>
                    )}

                    {/* Pay button */}
                    {!company.stripeKey ? (
                      <button onClick={() => setEditingCompany(true)} style={{ width:"100%", padding:"13px", background:"rgba(99,102,241,0.1)", border:"1px solid rgba(99,102,241,0.3)", borderRadius:10, color:"#818cf8", fontSize:12, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>
                        ⚙ Connect Stripe in Company Settings
                      </button>
                    ) : (
                      <button onClick={() => requirePro(requestPayment)} disabled={paymentLoading} style={{ width:"100%", padding:"13px", background: paymentLoading ? "rgba(99,102,241,0.06)" : "linear-gradient(135deg,rgba(99,102,241,0.25),rgba(139,92,246,0.15))", border:"1px solid rgba(99,102,241,0.4)", borderRadius:10, color: paymentLoading ? "rgba(129,140,248,0.5)" : "#818cf8", fontSize:13, fontWeight:700, cursor: paymentLoading ? "default" : "pointer", fontFamily:"inherit", transition:"all 0.2s" }}>
                        {paymentLoading ? "Opening Stripe Checkout..." : `⚡ Send Payment Request — $${depositOnly ? Math.round(total * depositPercent / 100).toLocaleString() : total.toLocaleString()}`}
                      </button>
                    )}

                    <div style={{ textAlign:"center", fontSize:9, color:"rgba(255,255,255,0.2)", marginTop:8 }}>
                      Powered by Stripe · Secure · PCI compliant · Client pays in their browser
                    </div>
                  </div>
                </>
              )}
            </div>
          )}

          {/* ════════════ SAVED QUOTES TAB ════════════ */}
          {tab==="saved" && (
            <div style={{ animation:"fadeUp 0.3s ease both" }} className="no-print">
              {/* Free tier limit warning */}
              {!userIsPro && savedQuotes.length >= 3 && onShowPricing && (
                <div style={{ marginBottom:14, padding:"14px 16px", background:"linear-gradient(135deg,rgba(232,201,122,0.08),rgba(99,102,241,0.05))", border:"1px solid rgba(232,201,122,0.2)", borderRadius:12, display:"flex", justifyContent:"space-between", alignItems:"center", gap:12 }}>
                  <div>
                    <div style={{ fontSize:13, fontWeight:700, color:"#e8c97a", marginBottom:2 }}>Free plan limit reached</div>
                    <div style={{ fontSize:11, color:"rgba(255,255,255,0.4)" }}>You're using {savedQuotes.length}/3 saved quote slots. Upgrade for unlimited.</div>
                  </div>
                  <button onClick={onShowPricing} style={{ padding:"8px 14px", borderRadius:8, border:"1px solid rgba(232,201,122,0.4)", background:"rgba(232,201,122,0.12)", color:"#e8c97a", fontSize:11, fontWeight:700, cursor:"pointer", fontFamily:"inherit", flexShrink:0 }}>
                    Upgrade ⚡
                  </button>
                </div>
              )}
              {savedQuotes.length === 0 ? (
                <div style={{ textAlign:"center", padding:"48px 20px", color:"rgba(255,255,255,0.2)" }}>
                  <div style={{ fontSize:30, marginBottom:10 }}>◎</div>
                  <div style={{ fontSize:13 }}>No saved quotes yet.</div>
                  <div style={{ fontSize:11, marginTop:6, color:"rgba(255,255,255,0.15)" }}>Build an estimate and tap Save Quote.</div>
                </div>
              ) : (
                <>
                  <div style={{ fontSize:9, color:"rgba(255,255,255,0.28)", textTransform:"uppercase", letterSpacing:"0.1em", marginBottom:12 }}>
                    {savedQuotes.length} saved quote{savedQuotes.length !== 1 ? "s" : ""}
                  </div>
                  {savedQuotes.map(q => (
                    <div key={q.id} style={{
                      background: q.status === "accepted" ? "linear-gradient(135deg,rgba(100,220,130,0.06),rgba(255,255,255,0.02))" : "rgba(255,255,255,0.022)",
                      border: q.status === "accepted" ? "1px solid rgba(100,220,130,0.2)" : "1px solid rgba(255,255,255,0.065)",
                      borderRadius:13, padding:"14px 16px", marginBottom:8,
                      display:"flex", alignItems:"center", gap:12,
                    }}>
                      <div style={{ flex:1, minWidth:0 }}>
                        <div style={{ display:"flex", alignItems:"center", gap:8, marginBottom:4 }}>
                          <span style={{ fontFamily:"'DM Mono',monospace", fontSize:11, fontWeight:700, color:"#e8c97a" }}>{q.quoteNumber}</span>
                          {q.status === "accepted" && (
                            <span style={{ fontSize:9, fontWeight:700, color:"#7dcea0", background:"rgba(100,220,130,0.12)", border:"1px solid rgba(100,220,130,0.25)", padding:"1px 6px", borderRadius:4, letterSpacing:"0.05em" }}>
                              ✓ ACCEPTED
                            </span>
                          )}
                          {q.status === "draft" && (
                            <span style={{ fontSize:9, color:"rgba(255,255,255,0.25)", background:"rgba(255,255,255,0.05)", border:"1px solid rgba(255,255,255,0.08)", padding:"1px 6px", borderRadius:4, letterSpacing:"0.05em" }}>
                              DRAFT
                            </span>
                          )}
                        </div>
                        <div style={{ fontSize:13, fontWeight:600, color:"#fff", marginBottom:2 }}>
                          {q.clientName || "No client name"}{q.jobName ? ` — ${q.jobName}` : ""}
                        </div>
                        <div style={{ fontSize:10, color:"rgba(255,255,255,0.3)", fontFamily:"'DM Mono',monospace" }}>
                          ${q.total?.toLocaleString()} · {new Date(q.savedAt).toLocaleDateString()}
                          {q.sigName && <span style={{ color:"rgba(100,220,130,0.6)", marginLeft:8 }}>· Signed: {q.sigName}</span>}
                        </div>
                      </div>
                      <div style={{ display:"flex", gap:6, flexShrink:0 }}>
                        <button onClick={() => loadQuote(q)} style={{ padding:"7px 14px", borderRadius:7, border:"1px solid rgba(232,201,122,0.3)", background:"rgba(232,201,122,0.08)", color:"#e8c97a", fontSize:11, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>
                          Load →
                        </button>
                        <button onClick={() => deleteQuote(q.id)} style={{ padding:"7px 10px", borderRadius:7, border:"1px solid rgba(255,255,255,0.08)", background:"transparent", color:"rgba(255,100,100,0.5)", fontSize:11, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>
                          ✕
                        </button>
                      </div>
                    </div>
                  ))}
                </>
              )}
            </div>
          )}

          {/* ════════════ PROFIT ANALYSIS TAB ════════════ */}
          {tab==="profit" && (
            <div style={{ animation:"fadeUp 0.3s ease both" }} className="no-print">
              {!hasItems ? (
                <div style={{ textAlign:"center", padding:"40px 20px", color:"rgba(255,255,255,0.2)" }}>
                  <div style={{ fontSize:28, marginBottom:10 }}>◎</div>
                  <div style={{ fontSize:13 }}>Add services to see profit analysis.</div>
                </div>
              ) : (
                <>
                  {/* Key metrics */}
                  <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:8, marginBottom:16 }}>
                    {[
                      { label:"Total Revenue",     val:`$${total.toLocaleString()}`,         color:"#e8c97a", sub:"what client pays" },
                      { label:"Your Labor",         val:`$${totLab.toLocaleString()}`,         color:"#a8e87e", sub:`${totHrs.toFixed(1)} hrs @ $${hourlyRate}/hr` },
                      { label:"Materials Cost",     val:`$${totMat.toLocaleString()}`,         color:"#7eb8e8", sub:"your out-of-pocket" },
                      { label:"Gross Profit",       val:`$${markupAmt.toLocaleString()}`,      color:"#e8c97a", sub:`${marginPct}% margin` },
                      { label:"Effective Rate",     val:`$${effectiveRate}/hr`,                color:"#a8e87e", sub:"revenue per hour" },
                      { label:"Labor %",            val:`${laborPct}%`,                        color:"#7eb8e8", sub:"of total invoice" },
                    ].map(c => (
                      <div key={c.label} style={{ background:"rgba(255,255,255,0.025)", border:"1px solid rgba(255,255,255,0.07)", borderRadius:10, padding:"12px 14px" }}>
                        <div style={{ fontSize:9, color:"rgba(255,255,255,0.3)", textTransform:"uppercase", letterSpacing:"0.1em", marginBottom:5 }}>{c.label}</div>
                        <div style={{ fontFamily:"'DM Mono',monospace", fontSize:18, fontWeight:500, color:c.color }}>{c.val}</div>
                        <div style={{ fontSize:9, color:"rgba(255,255,255,0.25)", marginTop:2 }}>{c.sub}</div>
                      </div>
                    ))}
                  </div>

                  {/* Profitability grade */}
                  <div style={{ background: Number(marginPct) >= 30 ? "rgba(100,220,130,0.06)" : Number(marginPct) >= 20 ? "rgba(232,201,122,0.06)" : "rgba(232,120,120,0.06)", border: `1px solid ${Number(marginPct) >= 30 ? "rgba(100,220,130,0.2)" : Number(marginPct) >= 20 ? "rgba(232,201,122,0.2)" : "rgba(232,120,120,0.2)"}`, borderRadius:12, padding:"16px 18px", marginBottom:14 }}>
                    <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center" }}>
                      <div>
                        <div style={{ fontFamily:"'Syne',sans-serif", fontSize:14, fontWeight:800, color:"#fff" }}>
                          {Number(marginPct) >= 30 ? "✓ Strong margin" : Number(marginPct) >= 20 ? "⚡ Fair margin" : "⚠ Thin margin"}
                        </div>
                        <div style={{ fontSize:11, color:"rgba(255,255,255,0.4)", marginTop:3 }}>
                          {Number(marginPct) >= 30 ? "Industry benchmark: 25–35% is healthy for residential electrical." : Number(marginPct) >= 20 ? "Consider raising markup or hourly rate to improve margins." : "Increase markup or hourly rate — you may be underpriced."}
                        </div>
                      </div>
                      <div style={{ fontFamily:"'DM Mono',monospace", fontSize:28, fontWeight:700, color: Number(marginPct) >= 30 ? "#7dcea0" : Number(marginPct) >= 20 ? "#e8c97a" : "#e87e7e" }}>
                        {marginPct}%
                      </div>
                    </div>
                  </div>

                  {/* Per-service profitability breakdown */}
                  <div style={{ background:"rgba(255,255,255,0.022)", border:"1px solid rgba(255,255,255,0.065)", borderRadius:12, padding:"14px 16px" }}>
                    <div style={{ fontSize:9, color:"rgba(255,255,255,0.28)", textTransform:"uppercase", letterSpacing:"0.1em", marginBottom:12 }}>Service Breakdown</div>
                    {activeItems.map(item => {
                      const itemMarkup = (item.lineTotal * markup);
                      const itemRevenue = item.lineTotal + itemMarkup;
                      const itemMargin = itemRevenue > 0 ? ((itemMarkup / itemRevenue) * 100).toFixed(0) : 0;
                      return (
                        <div key={item.id} style={{ display:"flex", alignItems:"center", gap:8, padding:"7px 0", borderBottom:"1px solid rgba(255,255,255,0.04)" }}>
                          <div style={{ flex:1, minWidth:0 }}>
                            <div style={{ fontSize:12, color:"rgba(255,255,255,0.7)", fontWeight:600, lineHeight:1.2 }}>{item.label}</div>
                            <div style={{ fontSize:9, color:"rgba(255,255,255,0.3)", fontFamily:"'DM Mono',monospace", marginTop:1 }}>
                              {item.hrs.toFixed(1)} hrs · lab ${item.lab.toLocaleString()} · mat ${item.mat.toLocaleString()}
                            </div>
                          </div>
                          <div style={{ textAlign:"right", flexShrink:0 }}>
                            <div style={{ fontFamily:"'DM Mono',monospace", fontSize:13, fontWeight:700, color:"#e8c97a" }}>${itemRevenue.toLocaleString()}</div>
                            <div style={{ fontSize:9, color:"rgba(255,255,255,0.3)", fontFamily:"'DM Mono',monospace" }}>{itemMargin}% margin</div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </>
              )}
            </div>
          )}

          {/* ════════════ AI QUOTE BUILDER ════════════ */}
          {showAIBuilder && (
            <AIQuoteBuilder
              onApplyEstimate={applyAIEstimate}
              onClose={() => setShowAIBuilder(false)}
            />
          )}

          {/* ════════════ CALENDAR VIEW ════════════ */}
          {showCalendar && (
            <div style={{ position:"fixed", inset:0, zIndex:150, overflowY:"auto" }}>
              <JobCalendar user={user} onClose={() => setShowCalendar(false)} />
            </div>
          )}

          {/* ════════════ NEC REFERENCE TAB ════════════ */}
          {tab === "nec" && <NECReference />}

          <div style={{ textAlign:"center", marginTop:44, paddingTop:18, borderTop:"1px solid rgba(255,255,255,0.04)", fontSize:9, color:"rgba(255,255,255,0.13)", letterSpacing:"0.07em" }} className="no-print">
            WIREWAY · NEC 2023 · wireway.cc
          </div>
        </div>
      </div>
      </div>  {/* closes the showDashboard display:none wrapper */}

      {/* ════════════ ACCOUNT MODAL ════════════ */}
      {showAccount && (
        <div className="modal-overlay" onClick={e => e.target===e.currentTarget && setShowAccount(false)}>
          <div className="modal-box" style={{ padding:"24px" }}>
            <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:20 }}>
              <div>
                <div style={{ fontFamily:"'Syne',sans-serif", fontSize:16, fontWeight:800, color:"#fff" }}>Account</div>
                <div style={{ fontSize:11, color:"rgba(255,255,255,0.3)", marginTop:2 }}>{user?.email}</div>
              </div>
              <button onClick={() => setShowAccount(false)} style={{ background:"transparent", border:"none", color:"rgba(255,255,255,0.4)", fontSize:22, cursor:"pointer" }}>✕</button>
            </div>

            {/* Plan status */}
            <div style={{ background: userIsPro ? "rgba(100,220,130,0.06)" : "rgba(232,201,122,0.06)", border: userIsPro ? "1px solid rgba(100,220,130,0.2)" : "1px solid rgba(232,201,122,0.2)", borderRadius:10, padding:"12px 14px", marginBottom:16 }}>
              <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center" }}>
                <div>
                  <div style={{ fontSize:12, fontWeight:700, color: userIsPro ? "#7dcea0" : "#e8c97a" }}>
                    {userIsPro ? (onTrial ? `Pro Trial — ${daysLeft} days left` : "Wireway Pro") : "Free Plan"}
                  </div>
                  <div style={{ fontSize:10, color:"rgba(255,255,255,0.3)", marginTop:2 }}>
                    {userIsPro ? "All features unlocked" : "3 quote limit · upgrade to unlock everything"}
                  </div>
                </div>
                {!userIsPro && onShowPricing && (
                  <button onClick={() => { setShowAccount(false); onShowPricing(); }} style={{ padding:"6px 12px", borderRadius:7, border:"1px solid rgba(232,201,122,0.4)", background:"rgba(232,201,122,0.1)", color:"#e8c97a", fontSize:11, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>
                    Upgrade ⚡
                  </button>
                )}
                {userIsPro && (
                  <button onClick={async () => {
                    const res = await fetch("/api/billing-portal", { method:"POST", headers:{"Content-Type":"application/json"}, body:JSON.stringify({ userId: user.id }) });
                    const d = await res.json(); if (d.url) window.open(d.url, "_blank");
                  }} style={{ padding:"6px 12px", borderRadius:7, border:"1px solid rgba(255,255,255,0.1)", background:"rgba(255,255,255,0.04)", color:"rgba(255,255,255,0.5)", fontSize:11, fontWeight:600, cursor:"pointer", fontFamily:"inherit" }}>
                    Manage Billing
                  </button>
                )}
              </div>
            </div>

            {/* Stats */}
            <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr 1fr", gap:8, marginBottom:16 }}>
              {[
                { label:"Quotes saved", val: savedQuotes.length },
                { label:"Clients", val: clients.length },
                { label:"Accepted", val: savedQuotes.filter(q=>q.status==="accepted"||q.status==="paid").length },
              ].map(s => (
                <div key={s.label} style={{ background:"rgba(255,255,255,0.025)", border:"1px solid rgba(255,255,255,0.06)", borderRadius:8, padding:"10px", textAlign:"center" }}>
                  <div style={{ fontFamily:"'DM Mono',monospace", fontSize:20, fontWeight:700, color:"#e8c97a" }}>{s.val}</div>
                  <div style={{ fontSize:9, color:"rgba(255,255,255,0.3)", marginTop:2 }}>{s.label}</div>
                </div>
              ))}
            </div>

            {/* Sign out */}
            <button onClick={async () => {
              await signOut();
              window.location.reload();
            }} style={{ width:"100%", padding:"12px", background:"rgba(232,126,126,0.06)", border:"1px solid rgba(232,126,126,0.2)", borderRadius:10, color:"#e87e7e", fontSize:13, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>
              Sign Out
            </button>
          </div>
        </div>
      )}

      {/* ════════════ COMPANY PROFILE MODAL ════════════ */}
      {signModal && (
        <div className="modal-overlay" onClick={e => e.target===e.currentTarget && setSignModal(false)}>
          <div className="modal-box" style={{ padding:"28px" }}>
            <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:22 }}>
              <div>
                <div style={{ fontFamily:"'Syne',sans-serif", fontSize:17, fontWeight:800, color:"#fff" }}>Client Acceptance</div>
                <div style={{ fontSize:11, color:"rgba(255,255,255,0.35)", marginTop:3 }}>
                  Quote {quoteNumber || "—"} · Total ${total.toLocaleString()}
                </div>
              </div>
              <button onClick={() => setSignModal(false)} style={{ background:"transparent", border:"none", color:"rgba(255,255,255,0.4)", fontSize:22, cursor:"pointer" }}>✕</button>
            </div>

            {/* Quote summary for reference */}
            <div style={{ background:"rgba(255,255,255,0.03)", border:"1px solid rgba(255,255,255,0.07)", borderRadius:10, padding:"12px 14px", marginBottom:20 }}>
              <div style={{ fontSize:11, color:"rgba(255,255,255,0.4)", marginBottom:8 }}>Scope Summary</div>
              {activeItems.slice(0, 5).map(i => (
                <div key={i.id} style={{ fontSize:11, color:"rgba(255,255,255,0.6)", padding:"3px 0", borderBottom:"1px solid rgba(255,255,255,0.04)" }}>
                  {i.label} ({i.variantLabel}) × {i.qty} — <span style={{ color:"#e8c97a", fontFamily:"'DM Mono',monospace" }}>${i.lineTotal.toLocaleString()}</span>
                </div>
              ))}
              {activeItems.length > 5 && <div style={{ fontSize:10, color:"rgba(255,255,255,0.3)", paddingTop:6 }}>+ {activeItems.length - 5} more services</div>}
              <div style={{ display:"flex", justifyContent:"space-between", paddingTop:10, marginTop:6, borderTop:"1px solid rgba(232,201,122,0.15)" }}>
                <span style={{ fontSize:12, color:"rgba(255,255,255,0.5)" }}>Total</span>
                <span style={{ fontFamily:"'DM Mono',monospace", fontSize:16, fontWeight:600, color:"#e8c97a" }}>${total.toLocaleString()}</span>
              </div>
            </div>

            {/* Acceptance statement */}
            <div style={{ fontSize:11, color:"rgba(255,255,255,0.45)", lineHeight:1.7, marginBottom:20, padding:"12px 14px", background:"rgba(232,201,122,0.04)", borderRadius:8, border:"1px solid rgba(232,201,122,0.12)" }}>
              By entering your name and date below, you acknowledge that you have reviewed this estimate, agree to the scope of work and pricing, and authorize {company.name || "the electrician"} to proceed.
              {company.terms && <div style={{ marginTop:8, color:"rgba(255,255,255,0.3)", fontSize:10 }}>{company.terms}</div>}
            </div>

            {/* Signature fields */}
            <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:8, marginBottom:16 }}>
              <div>
                <div style={{ fontSize:10, color:"rgba(255,255,255,0.3)", textTransform:"uppercase", letterSpacing:"0.08em", marginBottom:6 }}>Client Full Name</div>
                <input placeholder="Type full name to accept" value={sigName} onChange={e => setSigName(e.target.value)}
                  style={{ ...inputStyle }} onFocus={focusGold} onBlur={blurGray} />
              </div>
              <div>
                <div style={{ fontSize:10, color:"rgba(255,255,255,0.3)", textTransform:"uppercase", letterSpacing:"0.08em", marginBottom:6 }}>Date</div>
                <input type="date" value={sigDate} onChange={e => setSigDate(e.target.value)}
                  style={{ ...inputStyle, colorScheme:"dark" }} onFocus={focusGold} onBlur={blurGray} />
              </div>
            </div>

            {/* Accept button */}
            {sigSaved ? (
              <div style={{ textAlign:"center", padding:"14px", background:"rgba(100,220,130,0.1)", border:"1px solid rgba(100,220,130,0.3)", borderRadius:10, color:"#7dcea0", fontSize:14, fontWeight:700 }}>
                ✓ Quote Accepted — {sigName}
              </div>
            ) : (
              <button onClick={acceptQuote} disabled={!sigName}
                style={{ width:"100%", padding:"14px", background: sigName ? "linear-gradient(135deg,rgba(100,220,130,0.2),rgba(100,220,130,0.08))" : "rgba(255,255,255,0.04)", border: sigName ? "1px solid rgba(100,220,130,0.4)" : "1px solid rgba(255,255,255,0.08)", borderRadius:11, color: sigName ? "#7dcea0" : "rgba(255,255,255,0.25)", fontSize:13, fontWeight:700, cursor: sigName ? "pointer" : "default", fontFamily:"inherit", transition:"all 0.2s" }}>
                ✍ Accept & Sign Quote
              </button>
            )}

            <div style={{ textAlign:"center", marginTop:10, fontSize:10, color:"rgba(255,255,255,0.2)" }}>
              This is an electronic acknowledgment. Save or print a copy for your records.
            </div>
          </div>
        </div>
      )}

      {/* ════════════ WIRE SIZE CALCULATOR MODAL ════════════ */}
      {wireCalcOpen && (
        <div className="modal-overlay" onClick={e => e.target===e.currentTarget && setWireCalcOpen(false)}>
          <div className="modal-box" style={{ padding:"24px" }}>
            <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:20 }}>
              <div>
                <div style={{ fontFamily:"'Syne',sans-serif", fontSize:16, fontWeight:800, color:"#fff" }}>Wire Size Calculator</div>
                <div style={{ fontSize:11, color:"rgba(255,255,255,0.3)", marginTop:2 }}>NEC Table 310.15(B)(16) · 60°C column</div>
              </div>
              <button onClick={() => setWireCalcOpen(false)} style={{ background:"transparent", border:"none", color:"rgba(255,255,255,0.4)", fontSize:22, cursor:"pointer" }}>✕</button>
            </div>
            <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:8, marginBottom:14 }}>
              {[
                { label:"Load (amps)", val:wireAmps, set:setWireAmps, ph:"e.g. 20", type:"number" },
                { label:"One-way run (ft)", val:wireLen, set:setWireLen, ph:"e.g. 75", type:"number" },
              ].map(f => (
                <div key={f.label}>
                  <div style={{ fontSize:10, color:"rgba(255,255,255,0.3)", marginBottom:5 }}>{f.label}</div>
                  <input type={f.type} placeholder={f.ph} value={f.val} onChange={e => f.set(e.target.value)} style={inputStyle} onFocus={focusGold} onBlur={blurGray} />
                </div>
              ))}
              <div>
                <div style={{ fontSize:10, color:"rgba(255,255,255,0.3)", marginBottom:5 }}>Voltage</div>
                <div style={{ display:"flex", gap:5 }}>
                  {["120","240"].map(v => <button key={v} onClick={() => setWireVolt(v)} style={{ flex:1, padding:"8px", borderRadius:6, border: wireVolt===v ? "1px solid rgba(232,201,122,0.5)" : "1px solid rgba(255,255,255,0.08)", background: wireVolt===v ? "rgba(232,201,122,0.1)" : "rgba(255,255,255,0.03)", color: wireVolt===v ? "#e8c97a" : "rgba(255,255,255,0.4)", fontSize:12, fontWeight:700, cursor:"pointer", fontFamily:"'DM Mono',monospace" }}>{v}V</button>)}
                </div>
              </div>
              <div>
                <div style={{ fontSize:10, color:"rgba(255,255,255,0.3)", marginBottom:5 }}>Conductor</div>
                <div style={{ display:"flex", gap:5 }}>
                  {["copper","aluminum"].map(m => <button key={m} onClick={() => setWireMat(m)} style={{ flex:1, padding:"8px", borderRadius:6, border: wireMat===m ? "1px solid rgba(232,201,122,0.5)" : "1px solid rgba(255,255,255,0.08)", background: wireMat===m ? "rgba(232,201,122,0.1)" : "rgba(255,255,255,0.03)", color: wireMat===m ? "#e8c97a" : "rgba(255,255,255,0.4)", fontSize:11, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>{m[0].toUpperCase()+m.slice(1)}</button>)}
                </div>
              </div>
            </div>
            {wireResult && (
              <div style={{ background:"rgba(255,255,255,0.03)", border:"1px solid rgba(255,255,255,0.08)", borderRadius:12, padding:"16px" }}>
                <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr 1fr", gap:12, marginBottom:12 }}>
                  <div style={{ textAlign:"center" }}>
                    <div style={{ fontFamily:"'DM Mono',monospace", fontSize:28, fontWeight:700, color:"#e8c97a" }}># {wireResult.awg}</div>
                    <div style={{ fontSize:10, color:"rgba(255,255,255,0.4)" }}>AWG minimum</div>
                  </div>
                  <div style={{ textAlign:"center" }}>
                    <div style={{ fontFamily:"'DM Mono',monospace", fontSize:22, fontWeight:600, color:"#a8e87e" }}>{wireResult.ampacity}A</div>
                    <div style={{ fontSize:10, color:"rgba(255,255,255,0.4)" }}>ampacity</div>
                  </div>
                  <div style={{ textAlign:"center" }}>
                    <div style={{ fontFamily:"'DM Mono',monospace", fontSize:18, fontWeight:600, color: wireResult.vDropOk ? "#a8e87e" : "#e87e7e" }}>
                      {wireLen ? `${wireResult.vDropPct}%` : "—"}
                    </div>
                    <div style={{ fontSize:10, color:"rgba(255,255,255,0.4)" }}>voltage drop</div>
                  </div>
                </div>
                <div style={{ fontSize:11, color:"rgba(255,255,255,0.4)", lineHeight:1.7, borderTop:"1px solid rgba(255,255,255,0.06)", paddingTop:10 }}>
                  <span style={{ color:"rgba(232,201,122,0.7)", fontFamily:"'DM Mono',monospace" }}>{wireResult.nec}</span> — continuous load ({wireResult.continuous}A at 125%) requires #{wireResult.awg} AWG {wireMat}.
                  {wireLen && !wireResult.vDropOk && <span style={{ color:"#e87e7e" }}> ⚠ Voltage drop exceeds 3% — consider upsizing one AWG.</span>}
                  {wireLen && wireResult.vDropOk && <span style={{ color:"#a8e87e" }}> ✓ Voltage drop within 3% limit.</span>}
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ════════════ LOAD CALCULATOR MODAL ════════════ */}
      {loadCalcOpen && (
        <div className="modal-overlay" onClick={e => e.target===e.currentTarget && setLoadCalcOpen(false)}>
          <div className="modal-box" style={{ padding:"24px" }}>
            <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:20 }}>
              <div>
                <div style={{ fontFamily:"'Syne',sans-serif", fontSize:16, fontWeight:800, color:"#fff" }}>Load Calculator</div>
                <div style={{ fontSize:11, color:"rgba(255,255,255,0.3)", marginTop:2 }}>NEC 220.82 Optional Method — dwelling units</div>
              </div>
              <button onClick={() => setLoadCalcOpen(false)} style={{ background:"transparent", border:"none", color:"rgba(255,255,255,0.4)", fontSize:22, cursor:"pointer" }}>✕</button>
            </div>
            <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:8, marginBottom:14 }}>
              {[
                { label:"Sq footage", val:sqft, set:setSqft, ph:"e.g. 2400" },
                { label:"Small appl. circuits", val:smallAppl, set:setSmallAppl, ph:"2 (min)" },
                { label:"Laundry circuits", val:laundry, set:setLaundry, ph:"1 (min)" },
                { label:"Electric dryers", val:dryer, set:setDryer, ph:"0" },
                { label:"Electric ranges", val:range, set:setRange, ph:"0" },
                { label:"AC (tons)", val:acTons, set:setAcTons, ph:"0" },
                { label:"Heat (kW)", val:heatKw, set:setHeatKw, ph:"0" },
              ].map(f => (
                <div key={f.label}>
                  <div style={{ fontSize:10, color:"rgba(255,255,255,0.3)", marginBottom:5 }}>{f.label}</div>
                  <input type="number" min="0" placeholder={f.ph} value={f.val} onChange={e => f.set(e.target.value)} style={inputStyle} onFocus={focusGold} onBlur={blurGray} />
                </div>
              ))}
            </div>
            {loadResult && (
              <div style={{ background:"rgba(255,255,255,0.03)", border:"1px solid rgba(255,255,255,0.08)", borderRadius:12, padding:"16px" }}>
                <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr 1fr", gap:12, marginBottom:12, textAlign:"center" }}>
                  <div>
                    <div style={{ fontFamily:"'DM Mono',monospace", fontSize:24, fontWeight:700, color:"#e8c97a" }}>{loadResult.amps240}A</div>
                    <div style={{ fontSize:10, color:"rgba(255,255,255,0.4)" }}>calculated load @240V</div>
                  </div>
                  <div>
                    <div style={{ fontFamily:"'DM Mono',monospace", fontSize:24, fontWeight:700, color:"#a8e87e" }}>{loadResult.panelSize}</div>
                    <div style={{ fontSize:10, color:"rgba(255,255,255,0.4)" }}>min panel size</div>
                  </div>
                  <div>
                    <div style={{ fontFamily:"'DM Mono',monospace", fontSize:20, fontWeight:600, color:"#7eb8e8" }}>{(loadResult.totalVA/1000).toFixed(1)}kVA</div>
                    <div style={{ fontSize:10, color:"rgba(255,255,255,0.4)" }}>total demand</div>
                  </div>
                </div>
                {[
                  { label:"General lighting (3 VA/sqft)", val: loadResult.lighting },
                  { label:"Small appliance + laundry", val: loadResult.sabc },
                  { label:"After demand factors", val: loadResult.gen },
                  { label:"Dryers", val: loadResult.dryerVA },
                  { label:"Ranges", val: loadResult.rangeVA },
                  { label:"HVAC (larger of AC/heat)", val: loadResult.hvac },
                ].filter(r => r.val > 0).map(row => (
                  <div key={row.label} style={{ display:"flex", justifyContent:"space-between", fontSize:11, padding:"3px 0", borderBottom:"1px solid rgba(255,255,255,0.04)" }}>
                    <span style={{ color:"rgba(255,255,255,0.4)" }}>{row.label}</span>
                    <span style={{ fontFamily:"'DM Mono',monospace", color:"rgba(255,255,255,0.6)" }}>{row.val.toLocaleString()} VA</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* ════════════ INSPECTION CHECKLIST MODAL ════════════ */}
      {checklistOpen && (
        <div className="modal-overlay" onClick={e => e.target===e.currentTarget && setChecklistOpen(false)}>
          <div className="modal-box" style={{ padding:"24px", maxHeight:"85vh", overflowY:"auto" }}>
            <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:16 }}>
              <div>
                <div style={{ fontFamily:"'Syne',sans-serif", fontSize:16, fontWeight:800, color:"#fff" }}>NEC 2023 Inspection Checklist</div>
                <div style={{ fontSize:11, color:"rgba(255,255,255,0.3)", marginTop:2 }}>Pre-inspection verification</div>
              </div>
              <button onClick={() => setChecklistOpen(false)} style={{ background:"transparent", border:"none", color:"rgba(255,255,255,0.4)", fontSize:22, cursor:"pointer" }}>✕</button>
            </div>
            <div style={{ display:"flex", gap:6, flexWrap:"wrap", marginBottom:16 }}>
              {Object.entries(CHECKLISTS).map(([key, val]) => (
                <button key={key} onClick={() => setChecklistType(key)} style={{ padding:"5px 10px", borderRadius:6, fontSize:10, fontWeight:700, border: checklistType===key ? "1px solid rgba(232,120,120,0.5)" : "1px solid rgba(255,255,255,0.08)", background: checklistType===key ? "rgba(232,120,120,0.1)" : "rgba(255,255,255,0.03)", color: checklistType===key ? "#e87e7e" : "rgba(255,255,255,0.4)", cursor:"pointer", fontFamily:"inherit" }}>{val.label}</button>
              ))}
            </div>
            {(() => {
              const cl = CHECKLISTS[checklistType];
              const done = cl.items.filter(i => checkedItems[i.id]).length;
              return (
                <>
                  <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:12 }}>
                    <div style={{ fontSize:11, color:"rgba(255,255,255,0.4)" }}>{done} / {cl.items.length} complete</div>
                    <div style={{ height:4, flex:1, marginLeft:12, background:"rgba(255,255,255,0.06)", borderRadius:2, overflow:"hidden" }}>
                      <div style={{ height:"100%", width:`${(done/cl.items.length)*100}%`, background:"#7dcea0", borderRadius:2, transition:"width 0.3s" }}/>
                    </div>
                  </div>
                  {cl.items.map(item => (
                    <div key={item.id} onClick={() => toggleCheck(item.id)} style={{ display:"flex", alignItems:"flex-start", gap:10, padding:"10px 0", borderBottom:"1px solid rgba(255,255,255,0.04)", cursor:"pointer" }}>
                      <div style={{ width:20, height:20, borderRadius:4, flexShrink:0, border: checkedItems[item.id] ? "1px solid #7dcea0" : "1px solid rgba(255,255,255,0.2)", background: checkedItems[item.id] ? "rgba(100,220,130,0.15)" : "transparent", display:"flex", alignItems:"center", justifyContent:"center", marginTop:1, transition:"all 0.15s" }}>
                        {checkedItems[item.id] && <span style={{ fontSize:11, color:"#7dcea0" }}>✓</span>}
                      </div>
                      <div style={{ flex:1 }}>
                        <div style={{ fontSize:12, color: checkedItems[item.id] ? "rgba(255,255,255,0.4)" : "rgba(255,255,255,0.8)", textDecoration: checkedItems[item.id] ? "line-through" : "none", lineHeight:1.4 }}>{item.text}</div>
                        <div style={{ fontSize:9, color:"rgba(232,201,122,0.5)", fontFamily:"'DM Mono',monospace", marginTop:2 }}>{item.nec}</div>
                      </div>
                    </div>
                  ))}
                  {done === cl.items.length && done > 0 && (
                    <div style={{ textAlign:"center", padding:"16px", background:"rgba(100,220,130,0.06)", border:"1px solid rgba(100,220,130,0.2)", borderRadius:10, marginTop:12, color:"#7dcea0", fontSize:13, fontWeight:700 }}>
                      ✓ All checks complete — ready for inspection
                    </div>
                  )}
                </>
              );
            })()}
          </div>
        </div>
      )}

      {/* ════════════ CLIENT DATABASE MODAL ════════════ */}
      {showClientDB && (
        <div className="modal-overlay" onClick={e => e.target===e.currentTarget && setShowClientDB(false)}>
          <div className="modal-box" style={{ padding:"24px" }}>
            <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:16 }}>
              <div>
                <div style={{ fontFamily:"'Syne',sans-serif", fontSize:16, fontWeight:800, color:"#fff" }}>Client Database</div>
                <div style={{ fontSize:11, color:"rgba(255,255,255,0.3)", marginTop:2 }}>{clients.length} saved client{clients.length !== 1 ? "s" : ""}</div>
              </div>
              <button onClick={() => setShowClientDB(false)} style={{ background:"transparent", border:"none", color:"rgba(255,255,255,0.4)", fontSize:22, cursor:"pointer" }}>✕</button>
            </div>
            <input placeholder="Search clients..." value={clientSearch} onChange={e => setClientSearch(e.target.value)} style={{ ...inputStyle, marginBottom:12 }} onFocus={focusGold} onBlur={blurGray} />
            {clients.length === 0 ? (
              <div style={{ textAlign:"center", padding:"30px 20px", color:"rgba(255,255,255,0.2)", fontSize:12 }}>
                No clients yet. Save a quote to add a client automatically.
              </div>
            ) : (
              clients.filter(c => !clientSearch || c.name.toLowerCase().includes(clientSearch.toLowerCase())).map(c => (
                <div key={c.id} style={{ display:"flex", alignItems:"center", gap:10, padding:"10px 0", borderBottom:"1px solid rgba(255,255,255,0.05)" }}>
                  <div style={{ width:36, height:36, borderRadius:8, background:"rgba(232,201,122,0.1)", border:"1px solid rgba(232,201,122,0.2)", display:"flex", alignItems:"center", justifyContent:"center", fontFamily:"'Syne',sans-serif", fontSize:14, fontWeight:800, color:"#e8c97a", flexShrink:0 }}>
                    {c.name[0].toUpperCase()}
                  </div>
                  <div style={{ flex:1, minWidth:0 }}>
                    <div style={{ fontSize:13, fontWeight:600, color:"#fff" }}>{c.name}</div>
                    <div style={{ fontSize:10, color:"rgba(255,255,255,0.35)", fontFamily:"'DM Mono',monospace" }}>
                      {[c.phone, c.email].filter(Boolean).join(" · ")} {c.jobCount > 1 ? `· ${c.jobCount} jobs` : ""}
                    </div>
                  </div>
                  <button onClick={() => loadClient(c)} style={{ padding:"6px 12px", borderRadius:7, border:"1px solid rgba(232,201,122,0.3)", background:"rgba(232,201,122,0.08)", color:"#e8c97a", fontSize:11, fontWeight:700, cursor:"pointer", fontFamily:"inherit", flexShrink:0 }}>
                    Load
                  </button>
                </div>
              ))
            )}
          </div>
        </div>
      )}
      {editingCompany && (
        <div className="modal-overlay" onClick={e => e.target===e.currentTarget && setEditingCompany(false)}>
          <div className="modal-box" style={{ padding:"24px" }}>
            <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:20 }}>
              <div>
                <div style={{ fontFamily:"'Syne',sans-serif", fontSize:16, fontWeight:800, color:"#fff" }}>Company Profile</div>
                <div style={{ fontSize:11, color:"rgba(255,255,255,0.3)", marginTop:2 }}>Appears on every quote you send</div>
              </div>
              <button onClick={() => setEditingCompany(false)} style={{ background:"transparent", border:"none", color:"rgba(255,255,255,0.4)", fontSize:20, cursor:"pointer", padding:"4px 8px" }}>✕</button>
            </div>

            {/* Logo upload */}
            <div style={{ marginBottom:16 }}>
              <div style={{ fontSize:10, color:"rgba(255,255,255,0.3)", textTransform:"uppercase", letterSpacing:"0.1em", marginBottom:8 }}>Company Logo</div>
              <div style={{ display:"flex", alignItems:"center", gap:12 }}>
                {logoDataUrl
                  ? <img src={logoDataUrl} alt="logo" style={{ height:52, width:"auto", maxWidth:140, objectFit:"contain", borderRadius:6, border:"1px solid rgba(255,255,255,0.1)" }} />
                  : <div style={{ width:52, height:52, borderRadius:10, overflow:"hidden" }}><img src="/logo192.png" alt="Wireway" style={{ width:"100%", height:"100%", objectFit:"contain" }} /></div>
                }
                <div style={{ flex:1 }}>
                  <label style={{ display:"inline-block", padding:"8px 14px", background:"rgba(232,201,122,0.1)", border:"1px solid rgba(232,201,122,0.3)", borderRadius:7, color:"#e8c97a", fontSize:12, fontWeight:600, cursor:"pointer" }}>
                    {logoDataUrl ? "Change Logo" : "Upload Logo"}
                    <input type="file" accept="image/*" onChange={handleLogoUpload} style={{ display:"none" }} />
                  </label>
                  {logoDataUrl && (
                    <button onClick={() => setLogoDataUrl("")} style={{ marginLeft:8, padding:"8px 12px", background:"transparent", border:"1px solid rgba(255,255,255,0.1)", borderRadius:7, color:"rgba(255,255,255,0.4)", fontSize:11, cursor:"pointer", fontFamily:"inherit" }}>Remove</button>
                  )}
                  <div style={{ fontSize:10, color:"rgba(255,255,255,0.2)", marginTop:5 }}>PNG, JPG, SVG · max 2MB · appears on quotes</div>
                </div>
              </div>
            </div>

            {/* Company fields */}
            <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:8, marginBottom:12 }}>
              {[
                { ph:"Company name",        key:"name" },
                { ph:"Phone number",        key:"phone" },
                { ph:"Email address",       key:"email" },
                { ph:"Website",             key:"website" },
                { ph:"Street address",      key:"address" },
                { ph:"License number",      key:"license" },
                { ph:"Google Review URL",   key:"reviewUrl" },
              ].map(f => (
                <input key={f.key} placeholder={f.ph} value={companyDraft[f.key]||""} onChange={e => setCompanyDraft(p => ({ ...p, [f.key]: e.target.value }))}
                  style={inputStyle} onFocus={focusGold} onBlur={blurGray} />
              ))}
            </div>

            {/* Terms */}
            <div style={{ marginBottom:16 }}>
              <div style={{ fontSize:10, color:"rgba(255,255,255,0.3)", textTransform:"uppercase", letterSpacing:"0.1em", marginBottom:6 }}>Terms & Conditions (optional)</div>
              <textarea placeholder="Payment due within 30 days. 50% deposit required before work begins. Estimate valid for 30 days..."
                value={companyDraft.terms||""} onChange={e => setCompanyDraft(p => ({ ...p, terms: e.target.value }))}
                rows={3} style={{ ...inputStyle, resize:"vertical", lineHeight:1.6 }} onFocus={focusGold} onBlur={blurGray} />
            </div>

            {/* Stripe */}
            <div style={{ marginBottom:16, padding:"14px", background:"rgba(99,102,241,0.06)", border:"1px solid rgba(99,102,241,0.18)", borderRadius:10 }}>
              <div style={{ fontSize:10, color:"rgba(129,140,248,0.8)", textTransform:"uppercase", letterSpacing:"0.1em", marginBottom:6 }}>⚡ Stripe Integration</div>
              <div style={{ fontSize:10, color:"rgba(255,255,255,0.35)", marginBottom:8, lineHeight:1.6 }}>
                Add your Stripe Secret Key to enable online payment collection. Get it from <span style={{ color:"#818cf8" }}>dashboard.stripe.com → Developers → API Keys</span>. Use test key (sk_test_...) first, then switch to live (sk_live_...) when ready.
              </div>
              <input
                placeholder="sk_live_... or sk_test_..."
                value={companyDraft.stripeKey||""}
                onChange={e => setCompanyDraft(p => ({ ...p, stripeKey: e.target.value }))}
                type="password"
                style={{ ...inputStyle, fontFamily:"'DM Mono',monospace", fontSize:11 }}
                onFocus={focusGold} onBlur={blurGray}
              />
              <div style={{ fontSize:9, color:"rgba(255,255,255,0.2)", marginTop:5 }}>
                Your key is stored locally on this device and never sent to Wireway servers — only to Stripe when a payment is requested.
              </div>
            </div>

            <div style={{ display:"flex", gap:8 }}>
              <button onClick={saveCompany} disabled={companySaving} style={{ flex:1, padding:"12px", background:"linear-gradient(135deg,rgba(232,201,122,0.2),rgba(232,201,122,0.08))", border:"1px solid rgba(232,201,122,0.4)", borderRadius:10, color: companySaving ? "rgba(232,201,122,0.4)" : "#e8c97a", fontSize:13, fontWeight:700, cursor: companySaving ? "default" : "pointer", fontFamily:"inherit" }}>
                {companySaving ? "Saving..." : "Save Profile"}
              </button>
              <button onClick={() => setEditingCompany(false)} style={{ padding:"12px 20px", background:"transparent", border:"1px solid rgba(255,255,255,0.08)", borderRadius:10, color:"rgba(255,255,255,0.4)", fontSize:13, fontWeight:600, cursor:"pointer", fontFamily:"inherit" }}>
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
      </div> {/* closes main app minHeight div */}
      </div> {/* closes display:none estimator wrapper */}

      {/* ── PWA INSTALL BANNER ── */}
      {showInstall && (
        <div style={{ position:"fixed", bottom:0, left:0, right:0, zIndex:300, padding:"14px 20px", background:"linear-gradient(135deg,#111115,#0a0a0c)", borderTop:"1px solid rgba(232,201,122,0.2)", display:"flex", alignItems:"center", gap:12, animation:"fadeUp 0.3s ease both" }} className="no-print">
          <img src="/logo192.png" alt="Wireway" style={{ height:36, width:36, borderRadius:7, objectFit:"contain" }} />
          <div style={{ flex:1 }}>
            <div style={{ fontFamily:"'Syne',sans-serif", fontSize:14, fontWeight:800, color:"#fff", letterSpacing:"-0.02em" }}>Install Wireway</div>
            <div style={{ fontSize:11, color:"rgba(255,255,255,0.4)", marginTop:1 }}>Add to your home screen for the full app experience</div>
          </div>
          <button onClick={triggerInstall} style={{ padding:"9px 18px", borderRadius:8, background:"linear-gradient(135deg,rgba(232,201,122,0.22),rgba(232,201,122,0.08))", border:"1px solid rgba(232,201,122,0.35)", color:"#e8c97a", fontSize:12, fontWeight:700, cursor:"pointer", fontFamily:"inherit", whiteSpace:"nowrap" }}>
            Install
          </button>
          <button onClick={() => setShowInstall(false)} style={{ background:"transparent", border:"none", color:"rgba(255,255,255,0.3)", fontSize:20, cursor:"pointer", padding:"0 4px" }}>✕</button>
        </div>
      )}
    </>
  );
}
