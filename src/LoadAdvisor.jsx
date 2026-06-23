/* eslint-disable react-hooks/exhaustive-deps */
// src/LoadAdvisor.jsx
// Electrification Load Advisor — can an existing service take a new big load
// (EV charger, heat pump, range, hot tub…) without a service upgrade?
//
// Deterministic NEC math (NOT AI):
//   • NEC 220.87 (metered): existing peak demand × 1.25 + new load — the method
//     that usually proves the existing service is fine and saves the upgrade.
//   • NEC 220.83 (calculated): 100% of first 8 kVA + 40% of the remainder — the
//     conservative fallback when there's no usage data.
// When the service is over capacity it recommends the two real paths — upgrade
// the service, or keep it and add load management (NEC 625.42 / 220.70) — and can
// drop the matching catalog line items straight into the quote.

import { useState, useMemo } from "react";

const SERVICE_SIZES = [100, 125, 150, 200, 400];

// New loads, with typical demand values and the catalog item each maps to.
const NEW_LOADS = [
  { key:"ev",     label:"EV Charger (Level 2)",  icon:"🔌", amps:[32,40,48,64], defAmp:48, volts:240, continuous:true,  cat:{ id:"outlet_ev",    variantIdx:1 } },
  { key:"heat",   label:"Heat Pump / Mini-Split", icon:"♨️", amps:[15,20,30,40,50], defAmp:30, volts:240, continuous:false, cat:{ id:"hvac_circuit",  variantIdx:2 } },
  { key:"range",  label:"Electric Range / Oven",  icon:"🍳", fixedVA:8000, volts:240, continuous:false, cat:{ id:"range_hookup",  variantIdx:0 } },
  { key:"dryer",  label:"Electric Dryer",         icon:"🌀", fixedVA:5000, volts:240, continuous:false, cat:{ id:"dryer_hookup",  variantIdx:1 } },
  { key:"wh",     label:"Electric Water Heater",  icon:"💧", fixedVA:4500, volts:240, continuous:true,  cat:{ id:"water_heater",  variantIdx:0 } },
  { key:"tub",    label:"Hot Tub / Spa",          icon:"🛁", amps:[40,50,60], defAmp:50, volts:240, continuous:false, cat:{ id:"hot_tub",       variantIdx:1 } },
  { key:"custom", label:"Other / Custom",         icon:"⚙️", custom:true, volts:240, continuous:true,  cat:null },
];

// Existing appliances for the 220.83 calculated method (typical nameplate VA)
const EXISTING_APPLIANCES = [
  { key:"range", label:"Electric range/oven", va:8000 },
  { key:"dryer", label:"Electric dryer",      va:5000 },
  { key:"wh",    label:"Electric water heater", va:4500 },
  { key:"ac",    label:"Central AC / heat",   va:7000 },
];

// ── Pure NEC calculations (unit-testable) ──────────────────────────────
export function newLoadBaseVA(load, sel) {
  if (!load) return 0;
  if (load.custom)  return (Number(sel.customAmps) || 0) * (load.volts || 240);
  if (load.fixedVA) return load.fixedVA;
  return (Number(sel.amp) || load.defAmp) * load.volts;
}
export function contribVA(load, baseVA) {
  return load && load.continuous ? baseVA * 1.25 : baseVA;
}
// NEC 220.87 — existing maximum demand × 125% + new load
export function meteredAmps(serviceA, peakKW, contrib) {
  const existingVA = (Number(peakKW) || 0) * 1000 * 1.25;
  const totalVA = existingVA + contrib;
  const totalA = totalVA / 240;
  return { existingVA, totalVA, totalA, headroomA: serviceA - totalA };
}
// NEC 220.83(A) — 100% of first 8 kVA + 40% of remainder (no added AC/heat case)
export function calcAmps(serviceA, sqft, saCircuits, laundry, applianceVA, contrib) {
  const general = (Number(sqft) || 0) * 3;
  const sabc = ((Number(saCircuits) || 0) + (Number(laundry) || 0)) * 1500;
  const other = general + sabc + (Number(applianceVA) || 0) + contrib;
  const demandVA = 8000 + Math.max(0, other - 8000) * 0.4;
  const totalA = demandVA / 240;
  return { demandVA, totalA, headroomA: serviceA - totalA };
}
export function recommendUpgrade(totalA) {
  return totalA <= 200
    ? { id:"panel_200", variantIdx:0, label:"200A service upgrade" }
    : { id:"panel_400", variantIdx:0, label:"400A service upgrade" };
}

export default function LoadAdvisor({ onApplyEstimate, onClose }) {
  const [method,     setMethod]     = useState("metered"); // "metered" | "calc"
  const [serviceA,   setServiceA]   = useState(100);
  const [peakKW,     setPeakKW]     = useState("");
  const [sqft,       setSqft]       = useState("");
  const [saCircuits, setSaCircuits] = useState(2);
  const [laundry,    setLaundry]    = useState(1);
  const [appliances, setAppliances] = useState({}); // key -> bool
  const [loadKey,    setLoadKey]    = useState("ev");
  const [amp,        setAmp]        = useState(48);
  const [customAmps, setCustomAmps] = useState("");
  const [fix,        setFix]        = useState("manage"); // "manage" | "upgrade"
  const [added,      setAdded]      = useState(false);

  const load = NEW_LOADS.find(l => l.key === loadKey);
  const applianceVA = EXISTING_APPLIANCES.reduce((a, ap) => a + (appliances[ap.key] ? ap.va : 0), 0);

  const r = useMemo(() => {
    const base = newLoadBaseVA(load, { amp, customAmps });
    const contrib = contribVA(load, base);
    const res = method === "metered"
      ? meteredAmps(serviceA, peakKW, contrib)
      : calcAmps(serviceA, sqft, saCircuits, laundry, applianceVA, contrib);
    const newLoadA = base / 240;
    const fits = res.headroomA >= 0;
    return { ...res, base, contrib, newLoadA, fits };
  }, [load, amp, customAmps, method, serviceA, peakKW, sqft, saCircuits, laundry, applianceVA]);

  const ready = method === "metered" ? Number(peakKW) > 0 : Number(sqft) > 0;
  const upgrade = recommendUpgrade(r.totalA);
  const a = (n) => Math.round(n).toLocaleString();

  const summaryText = () => {
    const m = method === "metered" ? "NEC 220.87 (measured usage)" : "NEC 220.83 (calculated)";
    if (!ready) return "";
    if (r.fits) {
      return `Good news: your existing ${serviceA}A service can take the new ${load.label.toLowerCase()}. ` +
        `By ${m}, the calculated demand is about ${a(r.totalA)}A, leaving roughly ${a(r.headroomA)}A of headroom — ` +
        `no service upgrade needed.`;
    }
    return `Heads up: adding the ${load.label.toLowerCase()} pushes the calculated demand to about ${a(r.totalA)}A ` +
      `(${m}), which is over your ${serviceA}A service by about ${a(Math.abs(r.headroomA))}A. ` +
      `Two options: upgrade the service, or keep it and add load management so the new load only draws when there's spare capacity.`;
  };

  const applyToQuote = () => {
    const items = [];
    if (load.cat) items.push({ ...load.cat, qty: 1, clientBuys: false });
    if (!r.fits) {
      if (fix === "upgrade") items.push({ id: upgrade.id, variantIdx: upgrade.variantIdx, qty: 1, clientBuys: false });
      else items.push({ id: "energy_mgmt", variantIdx: 1, qty: 1, clientBuys: false }); // load management
    }
    if (items.length && onApplyEstimate) {
      onApplyEstimate(items);
      setAdded(true);
      setTimeout(() => { if (onClose) onClose(); }, 700);
    }
  };

  const copySummary = async () => {
    try { await navigator.clipboard.writeText(summaryText()); }
    catch { window.prompt("Copy:", summaryText()); }
  };

  const card  = { background:"var(--card)", border:"1px solid var(--line)", borderRadius:12 };
  const chip  = (active, color="var(--accent)") => ({
    padding:"7px 12px", borderRadius:8, fontSize:12, fontWeight:700, cursor:"pointer", fontFamily:"inherit",
    border: active ? `1px solid ${color}` : "1px solid var(--line-strong)",
    background: active ? "rgba(var(--accent-rgb),0.12)" : "rgba(255,255,255,0.03)",
    color: active ? color : "rgba(255,255,255,0.45)",
  });
  const inp = { background:"rgba(255,255,255,0.04)", border:"1px solid var(--line-strong)", borderRadius:8, padding:"9px 11px", fontSize:14, color:"#fff", fontFamily:"inherit", width:"100%" };
  const lbl = { fontSize:10, color:"rgba(255,255,255,0.5)", textTransform:"uppercase", letterSpacing:"0.08em", marginBottom:7 };

  return (
    <div style={{ position:"fixed", inset:0, zIndex:360, background:"#0a0a0c", overflowY:"auto", fontFamily:"'DM Sans',sans-serif", color:"#fff" }}>
      {/* Header */}
      <div style={{ borderBottom:"1px solid var(--line)", background:"rgba(10,10,12,0.92)", backdropFilter:"blur(20px)", WebkitBackdropFilter:"blur(20px)", position:"sticky", top:0, zIndex:10, padding:"0 20px" }}>
        <div style={{ maxWidth:680, margin:"0 auto", height:54, display:"flex", alignItems:"center", justifyContent:"space-between" }}>
          <span style={{ fontFamily:"'Syne',sans-serif", fontSize:16, fontWeight:800 }}>⚡ Load Advisor</span>
          <button onClick={onClose} style={{ padding:"6px 12px", borderRadius:7, border:"1px solid var(--line-strong)", background:"transparent", color:"rgba(255,255,255,0.5)", fontSize:11, fontWeight:600, cursor:"pointer", fontFamily:"inherit" }}>✕ Close</button>
        </div>
      </div>

      <div style={{ maxWidth:680, margin:"0 auto", padding:"20px 20px 90px" }}>
        <p style={{ fontSize:13, color:"rgba(255,255,255,0.5)", lineHeight:1.6, marginBottom:18 }}>
          Check whether an existing service can take a new electrification load — and what to do if it can't.
        </p>

        {/* Method toggle */}
        <div style={{ ...lbl }}>Existing load method</div>
        <div style={{ display:"flex", gap:6, marginBottom:8 }}>
          <button onClick={() => setMethod("metered")} style={{ ...chip(method==="metered"), flex:1 }}>Measured usage · 220.87</button>
          <button onClick={() => setMethod("calc")}    style={{ ...chip(method==="calc"),    flex:1 }}>Calculated · 220.83</button>
        </div>
        <p style={{ fontSize:10.5, color:"rgba(255,255,255,0.5)", lineHeight:1.5, marginBottom:18 }}>
          {method === "metered"
            ? "Best method — uses the home's actual peak demand from a year of utility data. Usually shows the service is fine."
            : "Conservative fallback when there's no usage data — estimates load from the home's size and appliances."}
        </p>

        {/* Service size */}
        <div style={{ ...card, padding:"14px 16px", marginBottom:10 }}>
          <div style={lbl}>Existing service size</div>
          <div style={{ display:"flex", gap:6, flexWrap:"wrap" }}>
            {SERVICE_SIZES.map(s => (
              <button key={s} onClick={() => setServiceA(s)} style={chip(serviceA===s)}>{s}A</button>
            ))}
          </div>
        </div>

        {/* Method inputs */}
        {method === "metered" ? (
          <div style={{ ...card, padding:"14px 16px", marginBottom:10 }}>
            <div style={lbl}>Peak demand (last 12 months)</div>
            <div style={{ display:"flex", alignItems:"center", gap:10 }}>
              <input type="number" inputMode="decimal" placeholder="e.g. 14.5" value={peakKW} onChange={e => setPeakKW(e.target.value)} style={{ ...inp, maxWidth:140 }} />
              <span style={{ fontSize:13, color:"rgba(255,255,255,0.5)" }}>kW peak</span>
            </div>
            <p style={{ fontSize:10.5, color:"rgba(255,255,255,0.5)", marginTop:8, lineHeight:1.5 }}>
              From the utility's bill/portal (highest monthly demand) or 30 days of metering. The calc applies the NEC 125% factor for you.
            </p>
          </div>
        ) : (
          <div style={{ ...card, padding:"14px 16px", marginBottom:10 }}>
            <div style={lbl}>Home details</div>
            <div style={{ display:"flex", gap:10, flexWrap:"wrap", marginBottom:12 }}>
              <div style={{ flex:1, minWidth:120 }}>
                <div style={{ fontSize:11, color:"rgba(255,255,255,0.5)", marginBottom:4 }}>Square footage</div>
                <input type="number" inputMode="numeric" placeholder="e.g. 1800" value={sqft} onChange={e => setSqft(e.target.value)} style={inp} />
              </div>
              <div style={{ width:88 }}>
                <div style={{ fontSize:11, color:"rgba(255,255,255,0.5)", marginBottom:4 }}>SA circuits</div>
                <input type="number" inputMode="numeric" value={saCircuits} onChange={e => setSaCircuits(e.target.value)} style={inp} />
              </div>
              <div style={{ width:80 }}>
                <div style={{ fontSize:11, color:"rgba(255,255,255,0.5)", marginBottom:4 }}>Laundry</div>
                <input type="number" inputMode="numeric" value={laundry} onChange={e => setLaundry(e.target.value)} style={inp} />
              </div>
            </div>
            <div style={{ fontSize:11, color:"rgba(255,255,255,0.5)", marginBottom:7 }}>Existing 240V appliances</div>
            <div style={{ display:"flex", gap:6, flexWrap:"wrap" }}>
              {EXISTING_APPLIANCES.map(ap => (
                <button key={ap.key} onClick={() => setAppliances(p => ({ ...p, [ap.key]: !p[ap.key] }))} style={chip(!!appliances[ap.key])}>
                  {appliances[ap.key] ? "✓ " : ""}{ap.label}
                </button>
              ))}
            </div>
          </div>
        )}

        {/* New load */}
        <div style={{ ...card, padding:"14px 16px", marginBottom:14 }}>
          <div style={lbl}>New load being added</div>
          <div style={{ display:"flex", gap:6, flexWrap:"wrap", marginBottom: load.custom || load.amps ? 12 : 0 }}>
            {NEW_LOADS.map(l => (
              <button key={l.key} onClick={() => { setLoadKey(l.key); if (l.amps) setAmp(l.defAmp); }} style={chip(loadKey===l.key)}>
                {l.icon} {l.label}
              </button>
            ))}
          </div>
          {load.amps && (
            <div>
              <div style={{ fontSize:11, color:"rgba(255,255,255,0.5)", marginBottom:6 }}>Breaker / circuit size</div>
              <div style={{ display:"flex", gap:6, flexWrap:"wrap" }}>
                {load.amps.map(x => <button key={x} onClick={() => setAmp(x)} style={chip(amp===x)}>{x}A</button>)}
              </div>
            </div>
          )}
          {load.custom && (
            <div style={{ display:"flex", alignItems:"center", gap:10 }}>
              <input type="number" inputMode="numeric" placeholder="amps" value={customAmps} onChange={e => setCustomAmps(e.target.value)} style={{ ...inp, maxWidth:120 }} />
              <span style={{ fontSize:13, color:"rgba(255,255,255,0.5)" }}>A @ 240V</span>
            </div>
          )}
          {load.continuous && !load.custom && (
            <p style={{ fontSize:10, color:"rgba(255,255,255,0.5)", marginTop:8 }}>Continuous load — calculated at 125% per NEC.</p>
          )}
        </div>

        {/* Verdict */}
        {ready && (
          <>
            <div style={{
              borderRadius:14, padding:"18px 18px", marginBottom:12,
              background: r.fits ? "rgba(100,220,130,0.08)" : "rgba(232,160,90,0.08)",
              border: r.fits ? "1px solid rgba(100,220,130,0.3)" : "1px solid rgba(232,160,90,0.35)",
            }}>
              <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:12 }}>
                <span style={{ fontFamily:"'Syne',sans-serif", fontSize:16, fontWeight:800, color: r.fits ? "#7dcea0" : "#e8a85a" }}>
                  {r.fits ? "✓ Service can take it" : "⚠ Over capacity"}
                </span>
                <span style={{ fontFamily:"'DM Mono',monospace", fontSize:13, color:"rgba(255,255,255,0.5)" }}>
                  {method === "metered" ? "NEC 220.87" : "NEC 220.83"}
                </span>
              </div>
              <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr 1fr", gap:10, textAlign:"center" }}>
                {[
                  { l:"Calculated demand", v:`${a(r.totalA)}A`, c:"#fff" },
                  { l:"Service rating", v:`${serviceA}A`, c:"rgba(255,255,255,0.7)" },
                  { l: r.fits ? "Headroom" : "Over by", v:`${a(Math.abs(r.headroomA))}A`, c: r.fits ? "#7dcea0" : "#e8a85a" },
                ].map(x => (
                  <div key={x.l}>
                    <div style={{ fontSize:10, color:"rgba(255,255,255,0.5)", textTransform:"uppercase", letterSpacing:"0.06em", marginBottom:4 }}>{x.l}</div>
                    <div style={{ fontFamily:"'DM Mono',monospace", fontSize:18, fontWeight:600, color:x.c }}>{x.v}</div>
                  </div>
                ))}
              </div>
            </div>

            {/* Plain-English line for the homeowner */}
            <div style={{ ...card, padding:"12px 14px", marginBottom:12, display:"flex", gap:10, alignItems:"flex-start" }}>
              <div style={{ fontSize:12.5, color:"rgba(255,255,255,0.7)", lineHeight:1.6, flex:1 }}>{summaryText()}</div>
              <button onClick={copySummary} style={{ flexShrink:0, padding:"6px 10px", borderRadius:7, border:"1px solid var(--line-strong)", background:"rgba(255,255,255,0.04)", color:"rgba(255,255,255,0.5)", fontSize:10, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>Copy</button>
            </div>

            {/* If over capacity — choose the path */}
            {!r.fits && (
              <div style={{ marginBottom:12 }}>
                <div style={lbl}>Recommended fix — pick one</div>
                <div style={{ display:"flex", flexDirection:"column", gap:8 }}>
                  <button onClick={() => setFix("manage")} style={{ textAlign:"left", padding:"13px 15px", borderRadius:11, cursor:"pointer", fontFamily:"inherit",
                    border: fix==="manage" ? "1px solid rgba(var(--accent-rgb),0.5)" : "1px solid var(--line-strong)",
                    background: fix==="manage" ? "rgba(var(--accent-rgb),0.08)" : "rgba(255,255,255,0.02)" }}>
                    <div style={{ fontSize:13, fontWeight:700, color: fix==="manage" ? "var(--accent)" : "#fff", marginBottom:3 }}>{fix==="manage" ? "● " : "○ "}Keep the service — add load management</div>
                    <div style={{ fontSize:11, color:"rgba(255,255,255,0.5)", lineHeight:1.5 }}>A smart panel or EMS (NEC 625.42 / 220.70) lets the new load run only when there's spare capacity. Cheapest path — no service upgrade.</div>
                  </button>
                  <button onClick={() => setFix("upgrade")} style={{ textAlign:"left", padding:"13px 15px", borderRadius:11, cursor:"pointer", fontFamily:"inherit",
                    border: fix==="upgrade" ? "1px solid rgba(var(--accent-rgb),0.5)" : "1px solid var(--line-strong)",
                    background: fix==="upgrade" ? "rgba(var(--accent-rgb),0.08)" : "rgba(255,255,255,0.02)" }}>
                    <div style={{ fontSize:13, fontWeight:700, color: fix==="upgrade" ? "var(--accent)" : "#fff", marginBottom:3 }}>{fix==="upgrade" ? "● " : "○ "}Upgrade the service — {upgrade.label}</div>
                    <div style={{ fontSize:11, color:"rgba(255,255,255,0.5)", lineHeight:1.5 }}>Full headroom for this and future loads. Higher cost, but the permanent fix.</div>
                  </button>
                </div>
              </div>
            )}

            {/* Apply to quote */}
            <button onClick={applyToQuote} disabled={added} style={{ width:"100%", padding:"14px", borderRadius:11, fontSize:14, fontWeight:700, cursor: added ? "default" : "pointer", fontFamily:"inherit",
              background: added ? "rgba(100,220,130,0.12)" : "linear-gradient(135deg,rgba(var(--accent-rgb),0.22),rgba(var(--accent-rgb),0.08))",
              border: added ? "1px solid rgba(100,220,130,0.4)" : "1px solid rgba(var(--accent-rgb),0.4)",
              color: added ? "#7dcea0" : "var(--accent)" }}>
              {added ? "✓ Added to your quote" :
                r.fits ? `Add ${load.label} to quote →`
                       : `Add ${load.cat ? load.label + " + " : ""}${fix==="upgrade" ? upgrade.label : "load management"} to quote →`}
            </button>
            {load.custom && !load.cat && (
              <p style={{ fontSize:10.5, color:"rgba(255,255,255,0.5)", textAlign:"center", marginTop:8 }}>
                Custom load isn't in the catalog — add its circuit line manually after.
              </p>
            )}

            <p style={{ fontSize:10, color:"rgba(255,255,255,0.5)", textAlign:"center", marginTop:16, lineHeight:1.6 }}>
              Advisory estimate from the entered figures. Confirm with a full load calculation and your AHJ before bidding or installing.
            </p>
          </>
        )}
      </div>
    </div>
  );
}
