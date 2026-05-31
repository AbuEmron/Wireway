import React, { useState, useEffect, useRef } from "react";
import NECReference from './NECReference';

const REGIONS = {
  "New York City, NY":2.15,"Albany, NY":1.38,"Binghamton, NY":1.0,
  "Buffalo, NY":1.18,"Rochester, NY":1.22,"Syracuse, NY":1.12,
  "Boston, MA":1.85,"Providence, RI":1.55,"Hartford, CT":1.6,
  "Newark, NJ":1.9,"Philadelphia, PA":1.58,"Pittsburgh, PA":1.28,
  "Baltimore, MD":1.48,"Washington DC":1.72,
  "Atlanta, GA":1.28,"Charlotte, NC":1.18,"Raleigh, NC":1.22,
  "Greensboro, NC":1.1,"Jacksonville, FL":1.12,"Tampa, FL":1.22,
  "Orlando, FL":1.18,"Miami, FL":1.35,"Nashville, TN":1.25,
  "Memphis, TN":0.98,"Louisville, KY":1.02,"Richmond, VA":1.22,
  "Virginia Beach, VA":1.18,"New Orleans, LA":1.08,
  "Chicago, IL":1.65,"Detroit, MI":1.25,"Minneapolis, MN":1.35,
  "Columbus, OH":1.12,"Cincinnati, OH":1.08,"Cleveland, OH":1.15,
  "Indianapolis, IN":1.08,"St. Louis, MO":1.12,"Kansas City, MO":1.12,
  "Milwaukee, WI":1.25,"Omaha, NE":1.0,"Des Moines, IA":1.02,
  "Houston, TX":1.12,"Dallas, TX":1.18,"Austin, TX":1.28,
  "San Antonio, TX":1.08,"El Paso, TX":0.92,"Oklahoma City, OK":0.98,
  "Tulsa, OK":0.98,"Albuquerque, NM":1.02,"Tucson, AZ":1.02,
  "Los Angeles, CA":1.95,"San Francisco, CA":2.25,"San Diego, CA":1.85,
  "Sacramento, CA":1.65,"Phoenix, AZ":1.18,"Las Vegas, NV":1.25,
  "Denver, CO":1.45,"Salt Lake City, UT":1.22,"Portland, OR":1.55,
  "Seattle, WA":1.82,"Spokane, WA":1.22,"Boise, ID":1.15,
  "Anchorage, AK":1.85,"Honolulu, HI":2.1,"Rural/Small Town":0.88,
};

const CATS = {
  "Wiring Devices": {
    receptacles:   {low:150,high:350,label:"Receptacles (Outlets)",           unit:"each",   nec:"210.52",      mat:12,  hours:1.0},
    gfciOutlet:    {low:150,high:350,label:"GFCI Outlets",                    unit:"each",   nec:"210.8",       mat:22,  hours:1.0},
    afciOutlet:    {low:175,high:375,label:"AFCI Outlets",                    unit:"each",   nec:"210.12",      mat:42,  hours:1.0},
    switches:      {low:150,high:200,label:"Single-Pole Switches",            unit:"each",   nec:"404.2",       mat:10,  hours:0.75},
    threewaySwitch:{low:175,high:250,label:"3-Way Switches",                  unit:"each",   nec:"404.2",       mat:18,  hours:1.25},
    dimmers:       {low:175,high:260,label:"Dimmer Switches",                 unit:"each",   nec:"404.14",      mat:30,  hours:1.0},
    outdoorOutlet: {low:180,high:350,label:"Outdoor GFCI Outlets",            unit:"each",   nec:"210.8(A)(3)", mat:30,  hours:1.5},
    usbOutlet:     {low:175,high:310,label:"USB Combo Outlets",               unit:"each",   nec:"210.52",      mat:35,  hours:1.0},
    tamperResist:  {low:150,high:300,label:"Tamper-Resistant Receptacles",    unit:"each",   nec:"406.12",      mat:15,  hours:0.75},
    evCharger:     {low:750,high:1800,label:"EV Charger Level 2 (240V)",      unit:"each",   nec:"625.40",      mat:220, hours:5.0},
  },
  "Lighting": {
    snapLED:       {low:125,high:300,label:"Snap-In LED Recessed Lights",     unit:"each",   nec:"410.116",     mat:28,  hours:1.0},
    canLight:      {low:150,high:300,label:"Traditional Can Lights",          unit:"each",   nec:"410.116",     mat:40,  hours:1.5},
    wallLight:     {low:150,high:350,label:"Wall Sconces / Light Fixtures",   unit:"each",   nec:"410.36",      mat:45,  hours:1.25},
    ceilingFan:    {low:250,high:600,label:"Ceiling Fans (with light)",       unit:"each",   nec:"314.27",      mat:85,  hours:2.0},
    chandelierLight:{low:300,high:2000,label:"Chandelier / Heavy Fixture",    unit:"each",   nec:"314.27(D)",   mat:120, hours:3.0},
    undercabinet:  {low:150,high:350,label:"Under-Cabinet Lighting",          unit:"each",   nec:"410.36",      mat:45,  hours:1.25},
    outdoorLight:  {low:150,high:350,label:"Outdoor Light Fixtures",          unit:"each",   nec:"410.10",      mat:55,  hours:1.5},
    motionLight:   {low:175,high:400,label:"Motion Sensor Lights",            unit:"each",   nec:"410.10",      mat:65,  hours:1.5},
    exitSign:      {low:250,high:500,label:"Emergency Exit Signs",            unit:"each",   nec:"700.16",      mat:85,  hours:2.0},
  },
  "Panels & Service": {
    panel100:      {low:1500,high:3000,label:"100A Panel Replacement",        unit:"flat",   nec:"230.79",      mat:450, hours:10},
    panel200:      {low:2000,high:4000,label:"200A Panel Replacement",        unit:"flat",   nec:"230.79",      mat:650, hours:12},
    panel400:      {low:4000,high:8000,label:"400A Panel Upgrade",            unit:"flat",   nec:"230.79",      mat:1400,hours:18},
    subpanel100:   {low:1000,high:2500,label:"100A Subpanel Install",         unit:"flat",   nec:"225.30",      mat:380, hours:8},
    subpanel200:   {low:1500,high:3500,label:"200A Subpanel Install",         unit:"flat",   nec:"225.30",      mat:550, hours:10},
    panelCircuit:  {low:200,high:500,label:"New Branch Circuit at Panel",     unit:"each",   nec:"210.11",      mat:55,  hours:2.5},
    meterBase:     {low:500,high:1200,label:"Meter Base Replacement",         unit:"flat",   nec:"230.66",      mat:180, hours:5},
    groundRods:    {low:400,high:800,label:"Grounding Electrode System",      unit:"flat",   nec:"250.50",      mat:90,  hours:4},
    surgeProtector:{low:300,high:600,label:"Whole-Home Surge Protector",      unit:"each",   nec:"230.67",      mat:130, hours:2},
    exteriorDisconn:{low:400,high:900,label:"Exterior Emergency Disconnect",  unit:"flat",   nec:"230.85",      mat:150, hours:3},
  },
  "Appliance Circuits": {
    dryer240:      {low:250,high:600,label:"Dryer Circuit (240V / 30A)",      unit:"each",   nec:"210.11(C)(2)",mat:90,  hours:3},
    range240:      {low:300,high:700,label:"Range/Oven Circuit (240V / 50A)", unit:"each",   nec:"210.19",      mat:90,  hours:3},
    acCircuit:     {low:250,high:600,label:"A/C Dedicated Circuit",           unit:"each",   nec:"440.62",      mat:80,  hours:2.5},
    hotTub:        {low:1000,high:2500,label:"Hot Tub / Spa Circuit",         unit:"each",   nec:"680.42",      mat:280, hours:8},
    pool:          {low:1500,high:4000,label:"Pool Electrical (bonding+circuit)",unit:"flat",nec:"680.26",      mat:450, hours:14},
    wellPump:      {low:500,high:1200,label:"Well Pump Circuit",              unit:"each",   nec:"430.22",      mat:110, hours:4},
    generator:     {low:2000,high:5000,label:"Generator + Transfer Switch",   unit:"flat",   nec:"702.12",      mat:900, hours:14},
    solarTie:      {low:1000,high:3000,label:"Solar PV Interconnect",         unit:"flat",   nec:"705.12",      mat:350, hours:10},
    batteryBackup: {low:3000,high:8000,label:"Battery Backup System",         unit:"flat",   nec:"702.4",       mat:1500,hours:16},
  },
  "Safety Devices": {
    smokeDetector: {low:100,high:200,label:"Smoke Detectors (hardwired)",     unit:"each",   nec:"760.32",      mat:28,  hours:1.0},
    coDetector:    {low:100,high:200,label:"CO Detectors (hardwired)",        unit:"each",   nec:"760.32",      mat:32,  hours:1.0},
    comboDet:      {low:120,high:220,label:"Combo Smoke/CO Detectors",        unit:"each",   nec:"760.32",      mat:45,  hours:1.0},
    afciBreaker:   {low:120,high:200,label:"AFCI Breakers",                   unit:"each",   nec:"210.12",      mat:50,  hours:1.0},
    gfciBreaker:   {low:120,high:200,label:"GFCI Breakers",                   unit:"each",   nec:"210.8",       mat:50,  hours:1.0},
    tamperGFCI:    {low:150,high:300,label:"Tamper-Resistant GFCI Outlets",   unit:"each",   nec:"406.12",      mat:25,  hours:1.0},
  },
  "Wiring & Rough-In": {
    rewireRoom:    {low:500,high:1200,label:"Rewire Single Room",             unit:"each",   nec:"310.12",      mat:180, hours:6},
    rewireHome:    {low:10000,high:30000,label:"Full Home Rewire",            unit:"flat",   nec:"310.12",      mat:4000,hours:100},
    aluminumFix:   {low:200,high:400,label:"Aluminum Wiring Fix (per outlet)",unit:"each",  nec:"110.14",      mat:35,  hours:2.0},
    lowVoltage:    {low:100,high:200,label:"Low Voltage (data/cable/phone)",  unit:"each",   nec:"800.24",      mat:22,  hours:1.0},
    conduitRun:    {low:8,  high:15, label:"Conduit Run (per linear foot)",   unit:"lin ft", nec:"358.10",      mat:5,   hours:0.1},
    wireRun:       {low:4,  high:10, label:"Wire Pull (per linear foot)",     unit:"lin ft", nec:"310.15",      mat:1.5, hours:0.04},
    junctionBox:   {low:100,high:250,label:"Junction Box (installed)",        unit:"each",   nec:"314.29",      mat:10,  hours:1.0},
  },
  "Outdoor & Specialty": {
    outdoorPanel:  {low:800,high:2000,label:"Outdoor Subpanel",               unit:"flat",  nec:"225.30",       mat:250, hours:8},
    landscape:     {low:400,high:1200,label:"Landscape Lighting System",      unit:"flat",   nec:"411.3",       mat:180, hours:6},
    shed:          {low:800,high:2000,label:"Shed / Detached Garage Electric",unit:"flat",   nec:"225.30",      mat:250, hours:8},
    securityCamera:{low:150,high:300,label:"Security Camera Power",           unit:"each",   nec:"210.52",      mat:25,  hours:1.25},
    doorbell:      {low:150,high:350,label:"Doorbell / Video Doorbell",       unit:"each",   nec:"725.3",       mat:30,  hours:1.5},
    poolLight:     {low:400,high:1000,label:"Pool / Spa Light",               unit:"each",   nec:"680.23",      mat:200, hours:4},
  },
};

const CONDS = {
  openWalls:      {label:"Customer opens walls",          mult:0.85, sign:"-15%", color:"#4a9e6a"},
  finishedWalls:  {label:"Finished walls (fish wire)",    mult:1.20, sign:"+20%", color:"#c8783a"},
  oldWiring:      {label:"Old / knob-and-tube wiring",   mult:1.30, sign:"+30%", color:"#c85a3a"},
  newConstruction:{label:"New construction rough-in",     mult:0.88, sign:"-12%", color:"#4a9e6a"},
  atticAccess:    {label:"Attic / crawl space access",   mult:0.92, sign:"-8%",  color:"#4a9e6a"},
  highCeilings:   {label:"High ceilings (10ft+)",        mult:1.15, sign:"+15%", color:"#c8783a"},
  hazmat:         {label:"Asbestos / hazmat present",    mult:1.40, sign:"+40%", color:"#c85a3a"},
};

const fmt = n => "$"+n.toLocaleString();
const fmtR = (lo,hi) => lo===hi?fmt(lo):`${fmt(lo)}–${fmt(hi)}`;
const today = () => new Date().toLocaleDateString("en-US",{year:"numeric",month:"long",day:"numeric"});
const invNum = () => "VQ-"+Date.now().toString().slice(-6);

const TRANS = {
  en:{appTagline:"Residential Electrical Estimator",estimator:"Estimate",photo:"Photo",
    contractor:"Profile",ask:"Ask AI",nec:"NEC 2023",history:"History",overhead:"Overhead",
    landing:"Home",flatRate:"Flat Rate",timeAndMat:"Time & Material",
    generateBtn:"Generate Estimate",selectItems:"Select line items to begin",
    totalEst:"Total",laborHours:"labor hours",estReady:"Estimate complete",
    customerView:"Client View",copyQuote:"Copy",copied:"Copied",
    profSummary:"Summary",generating:"Generating summary...",
    disclaimer:"All estimates subject to on-site verification · NEC 2023 · Pull permits",
    langToggle:"ES"},
  es:{appTagline:"Estimador Eléctrico Residencial",estimator:"Estimar",photo:"Foto",
    contractor:"Perfil",ask:"Preguntar",nec:"NEC 2023",history:"Historial",overhead:"Gastos",
    landing:"Inicio",flatRate:"Tarifa Fija",timeAndMat:"Tiempo y Material",
    generateBtn:"Generar Estimado",selectItems:"Selecciona elementos para comenzar",
    totalEst:"Total",laborHours:"horas",estReady:"Estimado listo",
    customerView:"Vista Cliente",copyQuote:"Copiar",copied:"Copiado",
    profSummary:"Resumen",generating:"Generando resumen...",
    disclaimer:"Todos los estimados sujetos a verificación · NEC 2023 · Saque permisos",
    langToggle:"EN"},
};

// ─── SQFT PRESETS ──────────────────────────────────────────────────────────────
const SQFT = {
  "Under 1,000 sq ft":    {receptacles:10,switches:6,snapLED:6,smokeDetector:2,gfciOutlet:4,panelCircuit:2},
  "1,000–2,000 sq ft":   {receptacles:18,switches:10,snapLED:10,smokeDetector:3,gfciOutlet:6,panelCircuit:3},
  "2,000–3,500 sq ft":   {receptacles:28,switches:16,snapLED:16,smokeDetector:5,gfciOutlet:8,panelCircuit:5},
  "3,500+ sq ft":         {receptacles:40,switches:22,snapLED:22,smokeDetector:7,gfciOutlet:10,panelCircuit:7},
};

export default function VoltQuote() {
  const [lang,setLang] = useState("en");
  const T = TRANS[lang];
  const [view,setView] = useState("landing");
  const [tab,setTab] = useState("estimator");
  const [mode,setMode] = useState("flat");
  const [qty,setQty] = useState({});
  const [cond,setCond] = useState({});
  const [markup,setMarkup] = useState(20);
  const [rate,setRate] = useState(85);
  const [incMat,setIncMat] = useState(true);
  const [incPermit,setIncPermit] = useState(true);
  const [region,setRegion] = useState("Binghamton, NY");
  const [result,setResult] = useState(null);
  const [aiSum,setAiSum] = useState("");
  const [loading,setLoading] = useState(false);
  const [copied,setCopied] = useState(false);
  const [saved,setSaved] = useState(false);
  const [expanded,setExpanded] = useState({"Wiring Devices":true});
  const [necSearch,setNecSearch] = useState(""); // eslint-disable-line no-unused-vars
  const [chatInput,setChatInput] = useState("");
  const [chatHistory,setChatHistory] = useState([]);
  const [chatLoading,setChatLoading] = useState(false);
  const [showCustomer,setShowCustomer] = useState(false);
  const [showInvoice,setShowInvoice] = useState(false);
  const [history,setHistory] = useState([]);
  const [photoAnalysis,setPhotoAnalysis] = useState("");
  const [photoLoading,setPhotoLoading] = useState(false);
  const [cName,setCName] = useState("");
  const [cPhone,setCPhone] = useState("");
  const [cEmail,setCEmail] = useState("");
  const [cLic,setCLic] = useState("");
  const [cCity,setCCity] = useState("");
  const [cState,setCState] = useState("NY");
  const [overhead,setOverhead] = useState({insurance:300,vehicle:400,tools:100,phone:80,misc:200});
  const [targetHrs,setTargetHrs] = useState(120);
  const [profitPct,setProfitPct] = useState(25);
  const [invoiceDue,setInvoiceDue] = useState(30);
  const [invoiceClient,setInvoiceClient] = useState("");
  const [invoiceNotes,setInvoiceNotes] = useState("");
  const fileRef = useRef(null);

  const rm = REGIONS[region]||1.0;
  const cm = () => Object.entries(cond).reduce((m,[k,v])=>v?m*CONDS[k].mult:m,1.0);
  const totalOh = Object.values(overhead).reduce((a,b)=>a+(Number(b)||0),0);
  const trueRate = targetHrs>0 ? Math.ceil((totalOh/targetHrs)*(1+profitPct/100)) : 0;
  const hasItems = Object.values(qty).some(v=>v>0);
  const totalItems = Object.values(qty).reduce((a,b)=>a+(b||0),0);

  useEffect(()=>{
    try{const s=localStorage.getItem("vq_hist");if(s)setHistory(JSON.parse(s));}catch{}
  },[]);

  const calculate = () => {
    let tLo=0,tHi=0,tMat=0,tHrs=0;
    const items=[];
    const c=cm();
    Object.entries(CATS).forEach(([cat,jobs])=>{
      Object.entries(jobs).forEach(([key,job])=>{
        const q=qty[key]; if(!q||q<=0) return;
        const lo=Math.round(job.low*q*rm*c);
        const hi=Math.round(job.high*q*rm*c);
        const mat=Math.round((job.mat||0)*q);
        const hrs=Math.round((job.hours||0)*q*10)/10;
        tLo+=lo;tHi+=hi;tMat+=mat;tHrs+=hrs;
        items.push({label:job.label,qty:q,low:lo,high:hi,mat,hrs,nec:job.nec,cat});
      });
    });
    let tm=0;
    if(mode==="tm") tm=Math.round(tHrs*rate+tMat*(1+markup/100));
    if(incMat&&mode==="flat"){
      const mlo=Math.round(tMat*(markup/100));
      const mhi=Math.round(tMat*(markup/100)*1.1);
      tLo+=mlo;tHi+=mhi;
      items.push({label:`Materials Markup (${markup}%)`,qty:null,low:mlo,high:mhi,mat:0,hrs:0,nec:null});
    }
    if(incPermit){
      const fee=cState==="NY"||region.includes("NY")?150:100;
      tLo+=fee;tHi+=fee;if(mode==="tm")tm+=fee;
      items.push({label:"Permit & Inspection",qty:null,low:fee,high:fee,mat:0,hrs:0,nec:null});
    }
    const r={items,tLo,tHi,tMat,tHrs,tm,region,mode,date:today(),id:invNum()};
    setResult(r);
    genSum(items,tLo,tHi,region,tHrs,mode,tm);
  };

  const genSum = async(items,lo,hi,rgn,hrs,md,tm)=>{
    setLoading(true);setAiSum("");
    const scope=items.filter(i=>i.qty).map(i=>`${i.label}×${i.qty}`).join(", ");
    const ps=md==="tm"?fmt(tm):`${fmt(lo)}–${fmt(hi)}`;
    try{
      const r=await fetch("/api/claude",{method:"POST",headers:{"Content-Type":"application/json"},
        body:JSON.stringify({model:"claude-sonnet-4-20250514",max_tokens:300,
          messages:[{role:"user",content:`You are a professional electrical contractor. Write a confident 3-sentence quote summary for a residential customer in ${rgn}. Scope: ${scope}. Total: ${ps}. Est labor: ${hrs}h. Mention NEC 2023 compliance and that final price depends on site conditions. Professional, no bullets.`}]})});
      const d=await r.json();
      setAiSum(d.content?.map(b=>b.text||"").join("")||"");
    }catch{setAiSum("Estimate complete. Contact us for a detailed on-site assessment.");}
    setLoading(false);
  };

  const sendChat = async()=>{
    if(!chatInput.trim()) return;
    const msg={role:"user",content:chatInput};
    const hist=[...chatHistory,msg];
    setChatHistory(hist);setChatInput("");setChatLoading(true);
    const ctx=result?`Current estimate: ${fmtR(result.tLo,result.tHi)} in ${result.region}.`:"";
    try{
      const r=await fetch("/api/claude",{method:"POST",headers:{"Content-Type":"application/json"},
        body:JSON.stringify({model:"claude-sonnet-4-20250514",max_tokens:400,
          system:`You are an expert residential electrician and NEC 2023 authority. Answer concisely with specific code references. ${ctx}`,
          messages:hist})});
      const d=await r.json();
      setChatHistory([...hist,{role:"assistant",content:d.content?.map(b=>b.text||"").join("")||""}]);
    }catch{setChatHistory([...hist,{role:"assistant",content:"Unable to respond. Try again."}]);}
    setChatLoading(false);
  };

  const handlePhoto = async(e)=>{
    const file=e.target.files[0];if(!file)return;
    setPhotoLoading(true);setPhotoAnalysis("");
    const b64=await new Promise((res,rej)=>{const r=new FileReader();r.onload=()=>res(r.result.split(",")[1]);r.onerror=()=>rej();r.readAsDataURL(file);});
    try{
      const r=await fetch("/api/claude",{method:"POST",headers:{"Content-Type":"application/json"},
        body:JSON.stringify({model:"claude-sonnet-4-20250514",max_tokens:500,
          messages:[{role:"user",content:[
            {type:"image",source:{type:"base64",media_type:file.type,data:b64}},
            {type:"text",text:"You are a residential electrician. Analyze this photo. List specific electrical work items visible or needed: outlets, switches, panels, lights, fans, smoke detectors, GFCI needs. Short paragraph then bullet list."}
          ]}]})});
      const d=await r.json();
      setPhotoAnalysis(d.content?.map(b=>b.text||"").join("")||"");
    }catch{setPhotoAnalysis("Could not analyze. Please try again.");}
    setPhotoLoading(false);
  };

  const saveEst = ()=>{
    if(!result) return;
    const est={...result,summary:aiSum};
    const updated=[est,...history].slice(0,30);
    setHistory(updated);
    try{localStorage.setItem("vq_hist",JSON.stringify(updated));}catch{}
    setSaved(true);setTimeout(()=>setSaved(false),2000);
  };

  const copyQuote = ()=>{
    if(!result) return;
    const co=cName?`\n${cName}${cPhone?" · "+cPhone:""}${cLic?" · Lic#"+cLic:""}`:"";
    const lines=result.items.map(i=>`  ${i.label}${i.qty?` ×${i.qty}`:""}: ${fmtR(i.low,i.high)}`).join("\n");
    const total=result.mode==="tm"?fmt(result.tm):`${fmt(result.tLo)}–${fmt(result.tHi)}`;
    navigator.clipboard.writeText(`VOLTQUOTE ESTIMATE${co}\n${result.region} · ${result.date} · ${result.id}\n${"─".repeat(48)}\n${lines}\n${"─".repeat(48)}\nTOTAL: ${total}  |  ~${result.tHrs} hours\n\n${aiSum}\n\nValid 30 days · All work per NEC 2023`);
    setCopied(true);setTimeout(()=>setCopied(false),2000);
  };

  // ── LANDING PAGE ────────────────────────────────────────────────────────────
  if(view==="landing") return (
    <div style={{background:"#040407",color:"#c8c0a8",fontFamily:"'DM Mono','Courier New',monospace",minHeight:"100vh"}}>
      {/* Top ticker bar */}
      <div style={{background:"#e8a030",color:"#040407",fontSize:10,letterSpacing:3,textTransform:"uppercase",padding:"6px 0",textAlign:"center",fontWeight:600}}>
        ⚡ VoltQuote · Professional Electrical Estimating · NEC 2023 · 55+ US Markets · Free to Start
      </div>
      {/* Nav */}
      <div style={{borderBottom:"1px solid rgba(232,160,48,0.15)",padding:"16px 32px",display:"flex",justifyContent:"space-between",alignItems:"center"}}>
        <div style={{display:"flex",alignItems:"center",gap:16}}>
          <div style={{width:32,height:32,background:"#e8a030",display:"flex",alignItems:"center",justifyContent:"center",fontSize:16,color:"#040407",fontWeight:700}}>V</div>
          <div>
            <div style={{fontSize:14,fontWeight:600,color:"#f0e8d0",letterSpacing:2}}>VOLTQUOTE</div>
            <div style={{fontSize:9,color:"#4a4438",letterSpacing:3,textTransform:"uppercase"}}>Electrical Estimator</div>
          </div>
        </div>
        <div style={{display:"flex",gap:12}}>
          <button onClick={()=>setLang(lang==="en"?"es":"en")} style={ghostBtn}>{T.langToggle}</button>
          <button onClick={()=>setView("app")} style={primaryBtn}>Launch App →</button>
        </div>
      </div>
      {/* Hero */}
      <div style={{maxWidth:860,margin:"0 auto",padding:"80px 32px 60px"}}>
        <div style={{fontSize:9,letterSpacing:5,color:"#e8a030",textTransform:"uppercase",marginBottom:24}}>
          ── Professional Tool · 2026 Verified Pricing ──
        </div>
        <div style={{fontSize:"clamp(40px,6vw,72px)",fontWeight:600,lineHeight:1.05,color:"#f0e8d0",marginBottom:8,letterSpacing:-1}}>
          Estimate electrical
        </div>
        <div style={{fontSize:"clamp(40px,6vw,72px)",fontWeight:300,lineHeight:1.05,color:"#e8a030",marginBottom:32,letterSpacing:-1}}>
          work with precision.
        </div>
        <div style={{fontSize:14,color:"#4a4438",lineHeight:2,maxWidth:560,marginBottom:48,fontWeight:400}}>
          Location-adjusted pricing for 55+ US cities. Complete NEC 2023 reference. AI-powered summaries. Built for electricians who work from the truck.
        </div>
        <div style={{display:"flex",gap:12,flexWrap:"wrap",marginBottom:16}}>
          <button onClick={()=>setView("app")} style={primaryBtn}>⚡ Start Free Estimate</button>
          <button onClick={()=>{setView("app");setTab("nec");}} style={ghostBtn}>Browse NEC 2023</button>
        </div>
        <div style={{fontSize:9,color:"#2a2418",letterSpacing:3,textTransform:"uppercase"}}>Free · No signup · Works offline</div>
      </div>
      {/* Data bar */}
      <div style={{borderTop:"1px solid rgba(232,160,48,0.1)",borderBottom:"1px solid rgba(232,160,48,0.1)",padding:"0 32px"}}>
        <div style={{maxWidth:860,margin:"0 auto",display:"grid",gridTemplateColumns:"repeat(4,1fr)"}}>
          {[["55+","US Cities"],["60+","Line Items"],["NEC 2023","Code Built-In"],["$0","To Start"]].map(([v,l],i)=>(
            <div key={i} style={{padding:"24px 0",borderRight:i<3?"1px solid rgba(232,160,48,0.1)":"none",paddingRight:i<3?24:0,paddingLeft:i>0?24:0}}>
              <div style={{fontSize:22,fontWeight:600,color:"#e8a030",marginBottom:4}}>{v}</div>
              <div style={{fontSize:9,color:"#3a3428",letterSpacing:3,textTransform:"uppercase"}}>{l}</div>
            </div>
          ))}
        </div>
      </div>
      {/* Features */}
      <div style={{maxWidth:860,margin:"0 auto",padding:"60px 32px"}}>
        <div style={{fontSize:9,letterSpacing:5,color:"#e8a030",textTransform:"uppercase",marginBottom:40}}>── Capabilities</div>
        <div style={{display:"grid",gridTemplateColumns:"repeat(auto-fit,minmax(240px,1fr))",gap:0}}>
          {[
            ["📍","Location Pricing","Rates auto-adjust for 55+ cities across every US state."],
            ["⚡","60+ Line Items","Every residential job type with 2026 verified pricing."],
            ["📷","Photo Analysis","Upload a room photo. AI identifies what work is needed."],
            ["📖","NEC 2023 Built-In","60+ residential code articles. Plain English. AI chat."],
            ["📊","Overhead Calculator","Know your real break-even rate before you bid."],
            ["📄","Invoice Generator","Estimate to professional invoice in one tap."],
            ["💬","AI Code Assistant","Ask any code question. Get the exact NEC article."],
            ["🌐","EN / ES","Full bilingual support built in."],
          ].map(([icon,title,desc],i)=>(
            <div key={i} style={{padding:"28px 24px",borderTop:"1px solid rgba(232,160,48,0.08)",borderRight:i%2===0?"1px solid rgba(232,160,48,0.08)":"none"}}>
              <div style={{fontSize:20,marginBottom:12}}>{icon}</div>
              <div style={{fontSize:12,fontWeight:600,color:"#c8c0a8",marginBottom:6,letterSpacing:1}}>{title}</div>
              <div style={{fontSize:11,color:"#3a3428",lineHeight:1.8}}>{desc}</div>
            </div>
          ))}
        </div>
      </div>
      {/* Pricing */}
      <div style={{borderTop:"1px solid rgba(232,160,48,0.1)",padding:"60px 32px"}}>
        <div style={{maxWidth:560,margin:"0 auto",textAlign:"center"}}>
          <div style={{fontSize:9,letterSpacing:5,color:"#e8a030",textTransform:"uppercase",marginBottom:40}}>── Pricing</div>
          <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:0,border:"1px solid rgba(232,160,48,0.15)"}}>
            {[
              {name:"Free",price:"$0",period:"forever",desc:["5 estimates / month","All core features","NEC 2023 reference"],hi:false},
              {name:"Pro",price:"$9.99",period:"per month",desc:["Unlimited estimates","Saved history","Invoice + AI features"],hi:true},
            ].map((p,i)=>(
              <div key={i} style={{padding:"32px 24px",borderRight:i===0?"1px solid rgba(232,160,48,0.15)":"none",background:p.hi?"rgba(232,160,48,0.04)":"transparent",position:"relative"}}>
                {p.hi&&<div style={{position:"absolute",top:0,left:0,right:0,height:2,background:"#e8a030"}}/>}
                <div style={{fontSize:9,letterSpacing:4,color:p.hi?"#e8a030":"#3a3428",textTransform:"uppercase",marginBottom:16}}>{p.name}</div>
                <div style={{fontSize:36,fontWeight:600,color:"#f0e8d0",marginBottom:2}}>{p.price}</div>
                <div style={{fontSize:10,color:"#3a3428",marginBottom:24,letterSpacing:2}}>{p.period.toUpperCase()}</div>
                {p.desc.map((d,j)=>(
                  <div key={j} style={{display:"flex",gap:10,marginBottom:8,alignItems:"flex-start"}}>
                    <span style={{color:"#e8a030",fontSize:10,marginTop:2}}>→</span>
                    <span style={{fontSize:11,color:"#4a4438",lineHeight:1.6}}>{d}</span>
                  </div>
                ))}
              </div>
            ))}
          </div>
        </div>
      </div>
      {/* CTA */}
      <div style={{textAlign:"center",padding:"60px 32px 80px"}}>
        <div style={{fontSize:9,letterSpacing:5,color:"#e8a030",textTransform:"uppercase",marginBottom:24}}>── Get Started</div>
        <div style={{fontSize:"clamp(24px,4vw,42px)",fontWeight:300,color:"#f0e8d0",marginBottom:8}}>Your first estimate is</div>
        <div style={{fontSize:"clamp(24px,4vw,42px)",fontWeight:600,color:"#e8a030",marginBottom:32}}>always free.</div>
        <button onClick={()=>setView("app")} style={{...primaryBtn,padding:"18px 48px",fontSize:14}}>⚡ Start Free Estimate</button>
        <div style={{fontSize:9,color:"#2a2018",letterSpacing:3,textTransform:"uppercase",marginTop:14}}>No credit card · No signup · Instant access</div>
      </div>
      <div style={{borderTop:"1px solid rgba(232,160,48,0.08)",padding:"20px 32px",display:"flex",justifyContent:"space-between",alignItems:"center"}}>
        <div style={{fontSize:9,color:"#2a2018",letterSpacing:2,textTransform:"uppercase"}}>© 2026 VoltQuote · voltquote.app</div>
        <div style={{fontSize:9,color:"#2a2018",letterSpacing:2,textTransform:"uppercase"}}>NEC 2023 · Built for the trades</div>
      </div>
    </div>
  );

  // ── INVOICE ─────────────────────────────────────────────────────────────────
  if(showInvoice&&result) return (
    <div style={{background:"#f8f5ee",minHeight:"100vh",padding:"32px 16px",fontFamily:"'DM Mono','Courier New',monospace"}}>
      <div style={{maxWidth:640,margin:"0 auto"}}>
        <div style={{background:"#040407",borderRadius:"4px 4px 0 0",padding:"28px 32px",display:"flex",justifyContent:"space-between",alignItems:"flex-start"}}>
          <div>
            <div style={{fontSize:9,letterSpacing:4,color:"#e8a030",textTransform:"uppercase",marginBottom:8}}>Invoice</div>
            <div style={{fontSize:18,fontWeight:600,color:"#f0e8d0"}}>{cName||"VoltQuote Contractor"}</div>
            {cPhone&&<div style={{fontSize:11,color:"#6a6050",marginTop:4}}>{cPhone}</div>}
            {cEmail&&<div style={{fontSize:11,color:"#6a6050"}}>{cEmail}</div>}
            {cLic&&<div style={{fontSize:10,color:"#e8a030",marginTop:4}}>Lic #{cLic}</div>}
          </div>
          <div style={{textAlign:"right"}}>
            <div style={{fontSize:9,color:"#3a3428",letterSpacing:3,textTransform:"uppercase",marginBottom:4}}>Invoice #</div>
            <div style={{fontSize:14,fontWeight:600,color:"#e8a030"}}>{result.id}</div>
            <div style={{fontSize:9,color:"#3a3428",letterSpacing:3,textTransform:"uppercase",marginTop:10,marginBottom:4}}>Date</div>
            <div style={{fontSize:11,color:"#c8c0a8"}}>{result.date}</div>
            <div style={{fontSize:9,color:"#3a3428",letterSpacing:3,textTransform:"uppercase",marginTop:8,marginBottom:4}}>Terms</div>
            <div style={{fontSize:11,color:"#c8c0a8"}}>Net {invoiceDue} days</div>
          </div>
        </div>
        <div style={{background:"white",border:"1px solid #e8e0d0",borderTop:"none",borderRadius:"0 0 4px 4px",padding:"28px 32px"}}>
          {invoiceClient&&<div style={{marginBottom:20,paddingBottom:16,borderBottom:"1px solid #f0ebe0"}}>
            <div style={{fontSize:9,color:"#8a8070",letterSpacing:3,textTransform:"uppercase",marginBottom:6}}>Bill To</div>
            <div style={{fontSize:13,fontWeight:600,color:"#1a1a1a"}}>{invoiceClient}</div>
          </div>}
          <table style={{width:"100%",borderCollapse:"collapse",marginBottom:20}}>
            <thead>
              <tr style={{borderBottom:"2px solid #040407"}}>
                {["Description","Qty","Amount"].map(h=>(
                  <th key={h} style={{textAlign:h==="Amount"?"right":"left",fontSize:9,letterSpacing:3,color:"#8a8070",padding:"0 0 10px",fontWeight:500,textTransform:"uppercase"}}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {result.items.map((item,i)=>(
                <tr key={i} style={{borderBottom:"1px solid #f5f0e8"}}>
                  <td style={{padding:"10px 0",fontSize:12,color:"#1a1a1a"}}>{item.label}</td>
                  <td style={{padding:"10px 0",fontSize:12,color:"#6a6055",textAlign:"center"}}>{item.qty||"—"}</td>
                  <td style={{padding:"10px 0",fontSize:12,fontWeight:600,color:"#1a1a1a",textAlign:"right"}}>{fmtR(item.low,item.high)}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <div style={{display:"flex",justifyContent:"flex-end",marginBottom:24}}>
            <div style={{width:220,borderTop:"2px solid #040407",paddingTop:14}}>
              <div style={{display:"flex",justifyContent:"space-between",alignItems:"center"}}>
                <span style={{fontSize:12,fontWeight:600,color:"#1a1a1a"}}>Total</span>
                <span style={{fontSize:20,fontWeight:600,color:"#040407",fontFamily:"'DM Mono','Courier New',monospace"}}>
                  {result.mode==="tm"?fmt(result.tm):`${fmt(result.tLo)}–${fmt(result.tHi)}`}
                </span>
              </div>
            </div>
          </div>
          {invoiceNotes&&<div style={{background:"#f8f5ee",padding:14,marginBottom:16,borderLeft:"2px solid #e8a030"}}>
            <div style={{fontSize:9,color:"#8a8070",letterSpacing:3,textTransform:"uppercase",marginBottom:6}}>Notes</div>
            <div style={{fontSize:11,color:"#3a3030",lineHeight:1.7}}>{invoiceNotes}</div>
          </div>}
          <div style={{fontSize:9,color:"#aaa",textAlign:"center",lineHeight:2,letterSpacing:1,textTransform:"uppercase"}}>
            Estimate valid 30 days · NEC 2023 · All work subject to on-site inspection
          </div>
        </div>
        <div style={{display:"flex",gap:10,marginTop:14,justifyContent:"center"}}>
          <button onClick={()=>window.print()} style={primaryBtn}>Print / Save PDF</button>
          <button onClick={()=>setShowInvoice(false)} style={ghostBtn}>← Back</button>
        </div>
      </div>
      <style>{`@media print{button{display:none!important}}`}</style>
    </div>
  );

  // ── CUSTOMER VIEW ────────────────────────────────────────────────────────────
  if(showCustomer&&result) return (
    <div style={{background:"#f8f5ee",minHeight:"100vh",padding:"32px 16px",fontFamily:"'DM Mono','Courier New',monospace"}}>
      <div style={{maxWidth:620,margin:"0 auto"}}>
        <div style={{background:"#040407",borderRadius:"4px 4px 0 0",padding:"28px"}}>
          <div style={{fontSize:9,letterSpacing:4,color:"#e8a030",textTransform:"uppercase",marginBottom:8}}>VoltQuote Estimate</div>
          <div style={{fontSize:18,fontWeight:600,color:"#f0e8d0"}}>{cName||"Professional Electrician"}</div>
          {cPhone&&<div style={{fontSize:11,color:"#6a6050",marginTop:3}}>{cPhone}</div>}
          {cEmail&&<div style={{fontSize:11,color:"#6a6050"}}>{cEmail}</div>}
          {cLic&&<div style={{fontSize:10,color:"#e8a030",marginTop:4}}>License #{cLic}</div>}
          <div style={{fontSize:9,color:"#2a2418",marginTop:8,letterSpacing:2}}>{result.date} · {result.region}</div>
        </div>
        <div style={{background:"white",border:"1px solid #e0d8c8",borderTop:"none",borderRadius:"0 0 4px 4px",padding:"28px"}}>
          {result.items.filter(i=>i.qty).map((item,i)=>(
            <div key={i} style={{display:"flex",justifyContent:"space-between",padding:"10px 0",borderBottom:"1px solid #f5f0e8"}}>
              <div>
                <div style={{fontSize:12,fontWeight:500,color:"#1a1a1a"}}>{item.label}</div>
                <div style={{fontSize:10,color:"#8a8070",marginTop:2}}>Qty: {item.qty}</div>
              </div>
              <div style={{fontSize:12,fontWeight:600,color:"#040407"}}>{fmtR(item.low,item.high)}</div>
            </div>
          ))}
          <div style={{display:"flex",justifyContent:"space-between",alignItems:"center",padding:"16px 0",borderTop:"2px solid #040407",marginTop:4}}>
            <div>
              <div style={{fontSize:13,fontWeight:600,color:"#1a1a1a"}}>Total Estimate</div>
              <div style={{fontSize:10,color:"#8a8070",marginTop:2}}>~{result.tHrs} labor hours</div>
            </div>
            <div style={{fontSize:22,fontWeight:600,color:"#040407"}}>
              {result.mode==="tm"?fmt(result.tm):`${fmt(result.tLo)}–${fmt(result.tHi)}`}
            </div>
          </div>
          {aiSum&&<div style={{background:"#f8f5ee",padding:14,marginBottom:16,borderLeft:"2px solid #e8a030",fontSize:11,lineHeight:1.8,color:"#3a3030"}}>{aiSum}</div>}
          <div style={{fontSize:9,color:"#aaa",textAlign:"center",letterSpacing:1,textTransform:"uppercase",lineHeight:2}}>
            Estimate valid 30 days · Final price subject to site inspection · All work per NEC 2023
          </div>
        </div>
        <div style={{display:"flex",gap:10,marginTop:14,justifyContent:"center"}}>
          <button onClick={()=>{setShowInvoice(true);setShowCustomer(false);}} style={primaryBtn}>Convert to Invoice</button>
          <button onClick={()=>setShowCustomer(false)} style={ghostBtn}>← Back</button>
        </div>
      </div>
    </div>
  );

  // ── MAIN APP ────────────────────────────────────────────────────────────────
  const TB = (t,l) => (
    <button onClick={()=>setTab(t)} style={{
      padding:"12px 14px",cursor:"pointer",
      fontSize:9,fontFamily:"'DM Mono','Courier New',monospace",
      letterSpacing:2,textTransform:"uppercase",border:"none",whiteSpace:"nowrap",
      background:"transparent",
      color:tab===t?"#e8a030":"#2a2418",
      borderBottom:tab===t?"2px solid #e8a030":"2px solid transparent",
      transition:"all 0.15s ease",fontWeight:tab===t?"600":"400",
    }}>{l}</button>
  );

  return (
    <div style={{background:"#040407",minHeight:"100vh",fontFamily:"'DM Mono','Courier New',monospace",color:"#c8c0a8"}}>
      {/* Header */}
      <div style={{borderBottom:"1px solid rgba(232,160,48,0.15)",padding:"14px 20px 0",background:"#040407",position:"sticky",top:0,zIndex:100}}>
        <div style={{maxWidth:900,margin:"0 auto"}}>
          <div style={{display:"flex",alignItems:"center",gap:12,marginBottom:10}}>
            <button onClick={()=>setView("landing")} style={{background:"none",border:"none",cursor:"pointer",padding:0,display:"flex",alignItems:"center",gap:10}}>
              <div style={{width:28,height:28,background:"#e8a030",display:"flex",alignItems:"center",justifyContent:"center",fontSize:13,color:"#040407",fontWeight:700,flexShrink:0}}>V</div>
              <div>
                <div style={{fontSize:11,fontWeight:600,color:"#f0e8d0",letterSpacing:2,textAlign:"left"}}>VOLTQUOTE</div>
                <div style={{fontSize:8,color:"#2a2418",letterSpacing:3,textTransform:"uppercase",textAlign:"left"}}>{T.appTagline}</div>
              </div>
            </button>
            <div style={{flex:1}}/>
            {totalItems>0&&<div style={{fontSize:9,color:"#e8a030",letterSpacing:2,textTransform:"uppercase"}}>{totalItems} items</div>}
            <button onClick={()=>setLang(lang==="en"?"es":"en")} style={{...ghostBtn,padding:"5px 10px",fontSize:9}}>{T.langToggle}</button>
          </div>
          <div style={{display:"flex",overflowX:"auto",gap:0,marginLeft:-4}}>
            {TB("estimator","⚡ "+T.estimator)}
            {TB("sqft","◻ Sq Ft")}
            {TB("photo","◎ "+T.photo)}
            {TB("overhead","$ "+T.overhead)}
            {TB("contractor","◈ "+T.contractor)}
            {TB("ask","⊕ "+T.ask)}
            {TB("history","≡ "+T.history)}
            {TB("nec","§ "+T.nec)}
          </div>
        </div>
      </div>

      <div style={{maxWidth:900,margin:"0 auto",padding:"24px 20px"}}>

        {/* ESTIMATOR */}
        {tab==="estimator"&&<>
          {photoAnalysis&&(
            <div style={{border:"1px solid rgba(74,158,106,0.3)",background:"rgba(74,158,106,0.04)",padding:14,marginBottom:20}}>
              <div style={{fontSize:9,letterSpacing:3,color:"#4a9e6a",textTransform:"uppercase",marginBottom:8}}>◎ Photo Analysis</div>
              <p style={{margin:0,fontSize:11,color:"#6a8060",lineHeight:1.8,whiteSpace:"pre-wrap"}}>{photoAnalysis}</p>
              <button onClick={()=>setPhotoAnalysis("")} style={{marginTop:8,fontSize:9,color:"#2a2418",background:"none",border:"none",cursor:"pointer",letterSpacing:2,textTransform:"uppercase"}}>✕ Dismiss</button>
            </div>
          )}

          {/* Region + Mode */}
          <Row label="01 — Region & Mode">
            <div style={{display:"grid",gridTemplateColumns:"1fr auto",gap:10,marginBottom:10}}>
              <select value={region} onChange={e=>setRegion(e.target.value)} style={SEL}>
                {Object.keys(REGIONS).map(r=><option key={r} value={r}>{r}</option>)}
              </select>
              <div style={{background:"rgba(232,160,48,0.06)",border:"1px solid rgba(232,160,48,0.2)",padding:"8px 14px",textAlign:"center",minWidth:70}}>
                <div style={{fontSize:8,color:"#4a4438",letterSpacing:3,textTransform:"uppercase",marginBottom:2}}>Rate</div>
                <div style={{fontSize:15,fontWeight:600,color:"#e8a030"}}>{rm.toFixed(2)}x</div>
              </div>
            </div>
            <div style={{display:"flex",gap:0,border:"1px solid rgba(232,160,48,0.15)"}}>
              {[["flat",T.flatRate],["tm",T.timeAndMat]].map(([v,l])=>(
                <button key={v} onClick={()=>setMode(v)} style={{flex:1,padding:"10px",cursor:"pointer",fontSize:9,fontFamily:"'DM Mono','Courier New',monospace",letterSpacing:2,textTransform:"uppercase",background:mode===v?"rgba(232,160,48,0.08)":"transparent",border:"none",borderRight:v==="flat"?"1px solid rgba(232,160,48,0.15)":"none",color:mode===v?"#e8a030":"#2a2418",fontWeight:mode===v?"600":"400",transition:"all 0.15s"}}>{l}</button>
              ))}
            </div>
            {mode==="tm"&&(
              <div style={{marginTop:10,border:"1px solid rgba(232,160,48,0.12)",padding:14}}>
                <div style={{fontSize:9,color:"#3a3428",marginBottom:8,letterSpacing:2,textTransform:"uppercase"}}>Hourly rate: <span style={{color:"#e8a030"}}>${rate}/hr</span></div>
                <input type="range" min={50} max={200} value={rate} onChange={e=>setRate(Number(e.target.value))} style={{width:"100%",accentColor:"#e8a030"}}/>
                <div style={{display:"flex",justifyContent:"space-between",fontSize:9,color:"#2a2018",marginTop:4,letterSpacing:1}}>
                  <span>$50</span><span>$125</span><span>$200/hr</span>
                </div>
              </div>
            )}
          </Row>

          {/* Scope */}
          <Row label="02 — Scope of Work">
            {Object.entries(CATS).map(([cat,jobs])=>(
              <div key={cat} style={{marginBottom:4}}>
                <button onClick={()=>setExpanded(e=>({...e,[cat]:!e[cat]}))} style={{width:"100%",display:"flex",justifyContent:"space-between",alignItems:"center",background:expanded[cat]?"rgba(232,160,48,0.04)":"transparent",border:"1px solid rgba(232,160,48,0.12)",padding:"10px 14px",cursor:"pointer",color:"#c8c0a8",fontSize:10,letterSpacing:2,textTransform:"uppercase",borderBottom:expanded[cat]?"none":"1px solid rgba(232,160,48,0.12)"}}>
                  <span style={{color:expanded[cat]?"#e8a030":"#4a4438"}}>{cat}</span>
                  <div style={{display:"flex",alignItems:"center",gap:8}}>
                    {Object.keys(jobs).some(k=>qty[k]>0)&&<span style={{fontSize:8,color:"#e8a030",letterSpacing:2}}>{Object.keys(jobs).filter(k=>qty[k]>0).length} SEL</span>}
                    <span style={{color:"#e8a030",fontSize:10}}>{expanded[cat]?"▲":"▼"}</span>
                  </div>
                </button>
                {expanded[cat]&&(
                  <div style={{border:"1px solid rgba(232,160,48,0.12)",borderTop:"none"}}>
                    {Object.entries(jobs).map(([key,job],idx)=>(
                      <div key={key} style={{display:"flex",alignItems:"center",justifyContent:"space-between",padding:"10px 14px",background:qty[key]>0?"rgba(232,160,48,0.04)":idx%2===0?"rgba(255,255,255,0.01)":"transparent",borderBottom:"1px solid rgba(232,160,48,0.06)"}}>
                        <div style={{flex:1,minWidth:0}}>
                          <div style={{fontSize:11,color:qty[key]>0?"#c8c0a8":"#5a5448",marginBottom:3}}>{job.label}</div>
                          <div style={{display:"flex",gap:8,flexWrap:"wrap",alignItems:"center"}}>
                            <span style={{fontSize:9,color:"#2a2418",letterSpacing:1}}>{fmt(Math.round(job.low*rm))}–{fmt(Math.round(job.high*rm))}/{job.unit}</span>
                            <span style={{fontSize:9,color:"#2a2418",letterSpacing:1}}>~{job.hours}h</span>
                            {job.nec&&<span style={{fontSize:8,color:"#3a4878",cursor:"pointer",letterSpacing:1,textDecoration:"underline"}} onClick={()=>{setTab("nec");}}> §{job.nec}</span>}
                          </div>
                        </div>
                        <div style={{display:"flex",alignItems:"center",gap:8,flexShrink:0}}>
                          <button onClick={()=>setQty(q=>({...q,[key]:Math.max(0,(q[key]||0)-1)}))} style={qBtnS}>−</button>
                          <span style={{minWidth:24,textAlign:"center",fontSize:13,fontWeight:600,color:qty[key]>0?"#e8a030":"#1a1810",fontFamily:"'DM Mono','Courier New',monospace"}}>{qty[key]||0}</span>
                          <button onClick={()=>setQty(q=>({...q,[key]:(q[key]||0)+1}))} style={qBtnS}>+</button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ))}
          </Row>

          {/* Conditions */}
          <Row label="03 — Site Conditions">
            <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:6}}>
              {Object.entries(CONDS).map(([key,c])=>(
                <label key={key} style={{display:"flex",alignItems:"center",gap:10,cursor:"pointer",border:`1px solid ${cond[key]?"rgba(232,160,48,0.25)":"rgba(232,160,48,0.08)"}`,padding:"10px 12px",background:cond[key]?"rgba(232,160,48,0.04)":"transparent",transition:"all 0.15s"}}>
                  <input type="checkbox" checked={!!cond[key]} onChange={e=>setCond(x=>({...x,[key]:e.target.checked}))} style={{accentColor:"#e8a030",width:13,height:13}}/>
                  <div style={{flex:1}}>
                    <div style={{fontSize:10,color:cond[key]?"#c8c0a8":"#4a4438"}}>{c.label}</div>
                    <div style={{fontSize:9,color:c.color,letterSpacing:1,marginTop:2}}>{c.sign}</div>
                  </div>
                </label>
              ))}
            </div>
          </Row>

          {/* Options */}
          <Row label="04 — Pricing Options">
            <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:6,marginBottom:10}}>
              {[{l:"Include materials",s:incMat,set:setIncMat},{l:"Include permit",s:incPermit,set:setIncPermit}].map(o=>(
                <label key={o.l} style={{display:"flex",alignItems:"center",gap:10,cursor:"pointer",border:`1px solid ${o.s?"rgba(232,160,48,0.25)":"rgba(232,160,48,0.08)"}`,padding:"10px 12px",background:o.s?"rgba(232,160,48,0.04)":"transparent",transition:"all 0.15s"}}>
                  <input type="checkbox" checked={o.s} onChange={e=>o.set(e.target.checked)} style={{accentColor:"#e8a030",width:13,height:13}}/>
                  <span style={{fontSize:10,color:o.s?"#c8c0a8":"#4a4438"}}>{o.l}</span>
                </label>
              ))}
            </div>
            {incMat&&<div style={{border:"1px solid rgba(232,160,48,0.1)",padding:12}}>
              <div style={{fontSize:9,color:"#3a3428",marginBottom:8,letterSpacing:2,textTransform:"uppercase"}}>Materials markup: <span style={{color:"#e8a030"}}>{markup}%</span></div>
              <input type="range" min={0} max={50} value={markup} onChange={e=>setMarkup(Number(e.target.value))} style={{width:"100%",accentColor:"#e8a030"}}/>
              <div style={{display:"flex",justifyContent:"space-between",fontSize:9,color:"#2a2018",marginTop:4,letterSpacing:1}}><span>0%</span><span>25%</span><span>50%</span></div>
            </div>}
          </Row>

          {/* Generate */}
          <button onClick={calculate} disabled={!hasItems} style={{width:"100%",padding:"16px",marginBottom:24,background:hasItems?"#e8a030":"rgba(232,160,48,0.04)",border:hasItems?"none":"1px solid rgba(232,160,48,0.1)",cursor:hasItems?"pointer":"not-allowed",fontSize:10,fontWeight:600,color:hasItems?"#040407":"#2a2018",letterSpacing:3,textTransform:"uppercase",fontFamily:"'DM Mono','Courier New',monospace",transition:"all 0.2s"}}>
            {hasItems?"⚡ "+T.generateBtn:T.selectItems}
          </button>

          {/* Result */}
          {result&&(
            <div style={{border:"1px solid rgba(232,160,48,0.2)",background:"rgba(232,160,48,0.02)",animation:"fadeIn 0.3s ease"}}>
              <div style={{borderBottom:"1px solid rgba(232,160,48,0.12)",padding:"14px 16px",display:"flex",justifyContent:"space-between",alignItems:"center",flexWrap:"wrap",gap:8}}>
                <div>
                  <div style={{fontSize:9,color:"#e8a030",letterSpacing:3,textTransform:"uppercase",marginBottom:3}}>{T.estReady}</div>
                  <div style={{fontSize:9,color:"#2a2418",letterSpacing:2}}>{result.region} · {rm.toFixed(2)}x · {result.date}</div>
                </div>
                <div style={{display:"flex",gap:6,flexWrap:"wrap"}}>
                  <button onClick={()=>setShowCustomer(true)} style={{...ghostBtn,fontSize:9,padding:"6px 10px"}}>Client View</button>
                  <button onClick={()=>setShowInvoice(true)} style={{...ghostBtn,fontSize:9,padding:"6px 10px"}}>Invoice</button>
                  <button onClick={saveEst} style={{...ghostBtn,fontSize:9,padding:"6px 10px",color:saved?"#4a9e6a":"#2a2418"}}>{saved?"✓ Saved":"Save"}</button>
                  <button onClick={copyQuote} style={{...ghostBtn,fontSize:9,padding:"6px 10px",color:copied?"#4a9e6a":"#2a2418"}}>{copied?"✓ "+T.copied:T.copyQuote}</button>
                </div>
              </div>
              {result.items.map((item,i)=>(
                <div key={i} style={{display:"flex",justifyContent:"space-between",alignItems:"center",padding:"8px 16px",borderBottom:"1px solid rgba(232,160,48,0.06)"}}>
                  <div>
                    <span style={{fontSize:11,color:"#8a8070"}}>{item.label}</span>
                    {item.qty&&<span style={{fontSize:9,color:"#2a2018",marginLeft:6,letterSpacing:1}}>×{item.qty}</span>}
                    {item.hrs>0&&<span style={{fontSize:9,color:"#2a2818",marginLeft:6,letterSpacing:1}}>{item.hrs}h</span>}
                    {item.nec&&<span style={{fontSize:8,color:"#2a2848",marginLeft:6,letterSpacing:1}}>§{item.nec}</span>}
                  </div>
                  <div style={{fontSize:11,color:"#6a6050",fontFamily:"'DM Mono','Courier New',monospace",flexShrink:0}}>{fmtR(item.low,item.high)}</div>
                </div>
              ))}
              <div style={{borderTop:"2px solid rgba(232,160,48,0.3)",padding:"16px",display:"flex",justifyContent:"space-between",alignItems:"center"}}>
                <div>
                  <div style={{fontSize:12,fontWeight:600,color:"#f0e8d0"}}>{T.totalEst}</div>
                  <div style={{fontSize:9,color:"#2a2418",letterSpacing:1,marginTop:2}}>~{result.tHrs} {T.laborHours}</div>
                </div>
                <div style={{fontSize:20,fontWeight:600,color:"#e8a030",fontFamily:"'DM Mono','Courier New',monospace"}}>
                  {result.mode==="tm"?fmt(result.tm):`${fmt(result.tLo)}–${fmt(result.tHi)}`}
                </div>
              </div>
              <div style={{borderTop:"1px solid rgba(232,160,48,0.1)",padding:16}}>
                <div style={{fontSize:9,color:"#e8a030",letterSpacing:3,textTransform:"uppercase",marginBottom:8}}>{T.profSummary}</div>
                {loading?<div style={{fontSize:10,color:"#2a2418",fontStyle:"italic",letterSpacing:1}}>{T.generating}</div>
                  :<p style={{margin:0,fontSize:11,color:"#5a5448",lineHeight:1.9}}>{aiSum}</p>}
              </div>
            </div>
          )}
        </>}

        {/* SQ FT */}
        {tab==="sqft"&&<Row label="◻ Quick Estimate by Square Footage">
          <p style={{fontSize:11,color:"#3a3428",marginTop:0,lineHeight:1.8,marginBottom:20}}>Select home size to auto-populate a typical rough-in scope then refine from there.</p>
          <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:6}}>
            {Object.entries(SQFT).map(([name,preset])=>(
              <button key={name} onClick={()=>{setQty(q=>({...q,...preset}));setTab("estimator");setExpanded({"Wiring Devices":true,"Lighting":true,"Safety Devices":true});}} style={{border:"1px solid rgba(232,160,48,0.15)",background:"transparent",padding:"20px 16px",cursor:"pointer",textAlign:"left",transition:"all 0.2s"}}>
                <div style={{fontSize:11,fontWeight:600,color:"#e8a030",marginBottom:8,letterSpacing:1}}>{name}</div>
                <div style={{fontSize:9,color:"#3a3428",lineHeight:2,letterSpacing:1}}>
                  {Object.entries(preset).slice(0,3).map(([k,v])=>`${v}× ${k}`).join(" · ")}
                </div>
                <div style={{fontSize:9,color:"#e8a030",marginTop:10,letterSpacing:2,textTransform:"uppercase"}}>→ Apply</div>
              </button>
            ))}
          </div>
        </Row>}

        {/* PHOTO */}
        {tab==="photo"&&<Row label="◎ Photo-to-Estimate">
          <p style={{fontSize:11,color:"#3a3428",marginTop:0,lineHeight:1.8}}>Upload a photo of any room, panel, or electrical area. AI analyzes what work may be needed.</p>
          <input ref={fileRef} type="file" accept="image/*" onChange={handlePhoto} style={{display:"none"}}/>
          <button onClick={()=>fileRef.current?.click()} disabled={photoLoading} style={{width:"100%",padding:40,border:"1px dashed rgba(232,160,48,0.2)",background:"transparent",cursor:"pointer",color:photoLoading?"#2a2418":"#e8a030",fontSize:9,fontFamily:"'DM Mono','Courier New',monospace",letterSpacing:3,textTransform:"uppercase",transition:"all 0.2s"}}>
            {photoLoading?"Analyzing...":"◎ Upload Photo"}
          </button>
          {photoAnalysis&&(
            <div style={{marginTop:16,border:"1px solid rgba(232,160,48,0.15)",padding:16}}>
              <div style={{fontSize:9,letterSpacing:3,color:"#e8a030",textTransform:"uppercase",marginBottom:10}}>AI Analysis</div>
              <p style={{margin:0,fontSize:11,color:"#5a5448",lineHeight:1.9,whiteSpace:"pre-wrap"}}>{photoAnalysis}</p>
              <button onClick={()=>setTab("estimator")} style={{...ghostBtn,marginTop:12,padding:"8px 14px",fontSize:9}}>→ Go to Estimator</button>
            </div>
          )}
        </Row>}

        {/* OVERHEAD */}
        {tab==="overhead"&&<Row label="$ Overhead & True Cost Calculator">
          <p style={{fontSize:11,color:"#3a3428",marginTop:0,lineHeight:1.8,marginBottom:16}}>Enter your monthly costs to calculate your real break-even hourly rate.</p>
          <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:8,marginBottom:16}}>
            {[{k:"insurance",l:"Liability Insurance /mo"},{k:"vehicle",l:"Vehicle / Gas /mo"},{k:"tools",l:"Tools / Equipment /mo"},{k:"phone",l:"Phone / Software /mo"},{k:"misc",l:"Misc Overhead /mo"}].map(f=>(
              <div key={f.k}>
                <div style={{fontSize:9,color:"#3a3428",letterSpacing:2,textTransform:"uppercase",marginBottom:4}}>{f.l}</div>
                <div style={{display:"flex",alignItems:"center",border:"1px solid rgba(232,160,48,0.15)"}}>
                  <span style={{padding:"0 10px",color:"#e8a030",fontSize:12,borderRight:"1px solid rgba(232,160,48,0.15)"}}>$</span>
                  <input type="number" value={overhead[f.k]} onChange={e=>setOverhead(o=>({...o,[f.k]:Number(e.target.value)||0}))} style={{flex:1,background:"transparent",border:"none",color:"#c8c0a8",fontSize:12,padding:"10px",outline:"none",fontFamily:"'DM Mono','Courier New',monospace"}}/>
                </div>
              </div>
            ))}
          </div>
          <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:8,marginBottom:16}}>
            <div>
              <div style={{fontSize:9,color:"#3a3428",letterSpacing:2,textTransform:"uppercase",marginBottom:4}}>Billable Hours / Month</div>
              <input type="number" value={targetHrs} onChange={e=>setTargetHrs(Number(e.target.value)||1)} style={{...SEL}}/>
            </div>
            <div>
              <div style={{fontSize:9,color:"#3a3428",letterSpacing:2,textTransform:"uppercase",marginBottom:4}}>Desired Profit %</div>
              <input type="number" value={profitPct} onChange={e=>setProfitPct(Number(e.target.value)||0)} style={{...SEL}}/>
            </div>
          </div>
          <div style={{border:"1px solid rgba(232,160,48,0.2)",background:"rgba(232,160,48,0.03)",padding:20}}>
            <div style={{display:"grid",gridTemplateColumns:"1fr 1fr 1fr",gap:16,textAlign:"center",marginBottom:16}}>
              {[["Monthly Overhead",fmt(totalOh),"#c85a3a"],["Break-Even Rate","$"+Math.ceil(totalOh/targetHrs)+"/hr","#c8783a"],["Your True Rate","$"+trueRate+"/hr","#e8a030"]].map(([l,v,c])=>(
                <div key={l}>
                  <div style={{fontSize:8,color:"#3a3428",letterSpacing:2,textTransform:"uppercase",marginBottom:6}}>{l}</div>
                  <div style={{fontSize:20,fontWeight:600,color:c,fontFamily:"'DM Mono','Courier New',monospace"}}>{v}</div>
                </div>
              ))}
            </div>
            <div style={{borderTop:"1px solid rgba(232,160,48,0.12)",paddingTop:14,fontSize:10,color:"#3a3428",textAlign:"center",lineHeight:1.8}}>
              Your true rate includes overhead recovery + {profitPct}% profit.
            </div>
            <div style={{textAlign:"center",marginTop:12}}>
              <button onClick={()=>{setRate(trueRate);setMode("tm");setTab("estimator");}} style={ghostBtn}>→ Apply to T&M Estimator</button>
            </div>
          </div>
        </Row>}

        {/* CONTRACTOR */}
        {tab==="contractor"&&<Row label="◈ Contractor Profile">
          <p style={{fontSize:11,color:"#3a3428",marginTop:0,lineHeight:1.8}}>Your info appears on client quotes and invoices.</p>
          <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:8}}>
            {[{l:"Company / Name",v:cName,set:setCName,ph:"Smith Electric LLC"},{l:"Phone",v:cPhone,set:setCPhone,ph:"(607) 555-0100"},{l:"Email",v:cEmail,set:setCEmail,ph:"you@email.com"},{l:"License #",v:cLic,set:setCLic,ph:"ME-12345"},{l:"City",v:cCity,set:setCCity,ph:"Binghamton"}].map(f=>(
              <div key={f.l}>
                <div style={{fontSize:9,color:"#3a3428",letterSpacing:2,textTransform:"uppercase",marginBottom:4}}>{f.l}</div>
                <input value={f.v} onChange={e=>f.set(e.target.value)} placeholder={f.ph} style={{...SEL,fontSize:11}}/>
              </div>
            ))}
            <div>
              <div style={{fontSize:9,color:"#3a3428",letterSpacing:2,textTransform:"uppercase",marginBottom:4}}>State</div>
              <select value={cState} onChange={e=>setCState(e.target.value)} style={SEL}>
                {["AL","AK","AZ","AR","CA","CO","CT","DE","FL","GA","HI","ID","IL","IN","IA","KS","KY","LA","ME","MD","MA","MI","MN","MS","MO","MT","NE","NV","NH","NJ","NM","NY","NC","ND","OH","OK","OR","PA","RI","SC","SD","TN","TX","UT","VT","VA","WA","WV","WI","WY"].map(s=><option key={s} value={s}>{s}</option>)}
              </select>
            </div>
          </div>
          {cName&&<div style={{marginTop:20,border:"1px solid rgba(232,160,48,0.2)",padding:16}}>
            <div style={{fontSize:9,color:"#e8a030",letterSpacing:3,textTransform:"uppercase",marginBottom:10}}>Preview</div>
            <div style={{fontSize:14,fontWeight:600,color:"#f0e8d0"}}>{cName}</div>
            {cPhone&&<div style={{fontSize:11,color:"#5a5448",marginTop:3}}>{cPhone}</div>}
            {cEmail&&<div style={{fontSize:11,color:"#5a5448"}}>{cEmail}</div>}
            {cLic&&<div style={{fontSize:10,color:"#e8a030",marginTop:3}}>License #{cLic}</div>}
            {(cCity||cState)&&<div style={{fontSize:10,color:"#3a3428",marginTop:2}}>{[cCity,cState].filter(Boolean).join(", ")}</div>}
          </div>}
          <div style={{marginTop:16}}>
            <div style={{fontSize:9,color:"#3a3428",letterSpacing:2,textTransform:"uppercase",marginBottom:8}}>Invoice Defaults</div>
            <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:8}}>
              <div>
                <div style={{fontSize:9,color:"#2a2418",letterSpacing:1,textTransform:"uppercase",marginBottom:4}}>Default Client Name</div>
                <input value={invoiceClient} onChange={e=>setInvoiceClient(e.target.value)} placeholder="Customer name" style={{...SEL,fontSize:11}}/>
              </div>
              <div>
                <div style={{fontSize:9,color:"#2a2418",letterSpacing:1,textTransform:"uppercase",marginBottom:4}}>Payment Terms</div>
                <select value={invoiceDue} onChange={e=>setInvoiceDue(Number(e.target.value))} style={SEL}>
                  {[15,30,45,60].map(d=><option key={d} value={d}>Net {d} days</option>)}
                </select>
              </div>
              <div style={{gridColumn:"1/-1"}}>
                <div style={{fontSize:9,color:"#2a2418",letterSpacing:1,textTransform:"uppercase",marginBottom:4}}>Invoice Notes</div>
                <textarea value={invoiceNotes} onChange={e=>setInvoiceNotes(e.target.value)} placeholder="e.g. 50% deposit required. Balance due on completion." style={{...SEL,height:64,resize:"vertical",lineHeight:1.6}}/>
              </div>
            </div>
          </div>
        </Row>}

        {/* ASK AI */}
        {tab==="ask"&&<Row label="⊕ Ask the AI Electrician">
          <p style={{fontSize:11,color:"#3a3428",marginTop:0,lineHeight:1.8}}>Ask any code or pricing question. References the current estimate if you have one active.</p>
          <div style={{border:"1px solid rgba(232,160,48,0.12)",minHeight:240,maxHeight:360,overflowY:"auto",marginBottom:10,padding:14}}>
            {chatHistory.length===0&&<div style={{color:"#1a1810",fontSize:9,letterSpacing:2,textTransform:"uppercase",textAlign:"center",marginTop:50}}>e.g. "Where is GFCI required?" or "What wire size for a dryer?"</div>}
            {chatHistory.map((m,i)=>(
              <div key={i} style={{marginBottom:12,display:"flex",gap:8,justifyContent:m.role==="user"?"flex-end":"flex-start"}}>
                {m.role==="assistant"&&<div style={{width:18,height:18,background:"#e8a030",display:"flex",alignItems:"center",justifyContent:"center",fontSize:9,color:"#040407",fontWeight:700,flexShrink:0,marginTop:2}}>V</div>}
                <div style={{maxWidth:"82%",border:`1px solid ${m.role==="user"?"rgba(232,160,48,0.2)":"rgba(232,160,48,0.08)"}`,padding:"8px 12px",fontSize:11,color:m.role==="user"?"#c8a860":"#6a6450",lineHeight:1.8,background:m.role==="user"?"rgba(232,160,48,0.04)":"transparent"}}>{m.content}</div>
              </div>
            ))}
            {chatLoading&&<div style={{color:"#1a1810",fontSize:9,letterSpacing:2,textTransform:"uppercase"}}>Processing...</div>}
          </div>
          <div style={{display:"flex",gap:8}}>
            <input value={chatInput} onChange={e=>setChatInput(e.target.value)} onKeyDown={e=>e.key==="Enter"&&!e.shiftKey&&sendChat()} placeholder="Ask anything..." style={{...SEL,flex:1,fontSize:11}}/>
            <button onClick={sendChat} disabled={!chatInput.trim()||chatLoading} style={{...primaryBtn,padding:"0 16px",opacity:chatInput.trim()?1:0.4}}>Send</button>
          </div>
        </Row>}

        {/* HISTORY */}
        {tab==="history"&&<Row label="≡ Saved Estimates">
          {history.length===0
            ?<div style={{textAlign:"center",color:"#1a1810",fontSize:9,letterSpacing:3,textTransform:"uppercase",padding:40}}>No saved estimates yet</div>
            :history.map(est=>(
              <div key={est.id} style={{border:"1px solid rgba(232,160,48,0.12)",padding:14,marginBottom:8}}>
                <div style={{display:"flex",justifyContent:"space-between",alignItems:"flex-start",marginBottom:6}}>
                  <div>
                    <div style={{fontSize:10,fontWeight:600,color:"#c8c0a8",fontFamily:"'DM Mono','Courier New',monospace"}}>{est.id}</div>
                    <div style={{fontSize:9,color:"#2a2418",letterSpacing:1,marginTop:2}}>{est.date} · {est.region}</div>
                  </div>
                  <div style={{fontSize:13,fontWeight:600,color:"#e8a030",fontFamily:"'DM Mono','Courier New',monospace"}}>{est.mode==="tm"?fmt(est.tm):`${fmt(est.tLo)}–${fmt(est.tHi)}`}</div>
                </div>
                <div style={{fontSize:9,color:"#2a2418",marginBottom:10,lineHeight:1.8}}>{est.items.filter(i=>i.qty).map(i=>`${i.label}×${i.qty}`).join(" · ")}</div>
                <div style={{display:"flex",gap:8}}>
                  <button onClick={()=>{setResult(est);setAiSum(est.summary||"");setTab("estimator");}} style={{...ghostBtn,fontSize:9,padding:"6px 10px"}}>Load</button>
                  <button onClick={()=>{const u=history.filter(e=>e.id!==est.id);setHistory(u);try{localStorage.setItem("vq_hist",JSON.stringify(u));}catch{};}} style={{...ghostBtn,fontSize:9,padding:"6px 10px",color:"#4a2418"}}>Delete</button>
                </div>
              </div>
            ))
          }
        </Row>}

        {/* NEC */}
        {tab==="nec"&&<Row label="§ NEC 2023 — Complete Residential Reference">
          <NECReference/>
        </Row>}

        <div style={{marginTop:28,paddingTop:16,borderTop:"1px solid rgba(232,160,48,0.08)",textAlign:"center"}}>
          <div style={{fontSize:9,color:"#1a1810",letterSpacing:3,textTransform:"uppercase"}}>VoltQuote · voltquote.app · {T.disclaimer}</div>
        </div>
      </div>

      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=DM+Mono:wght@300;400;500&display=swap');
        *{-webkit-tap-highlight-color:transparent;box-sizing:border-box;}
        input,select,textarea{-webkit-appearance:none;}
        ::-webkit-scrollbar{width:3px;}
        ::-webkit-scrollbar-track{background:#040407;}
        ::-webkit-scrollbar-thumb{background:#e8a030;border-radius:0;}
        @keyframes fadeIn{from{opacity:0;transform:translateY(6px)}to{opacity:1;transform:translateY(0)}}
        ::selection{background:rgba(232,160,48,0.25);color:#f0e8d0;}
        input:focus,select:focus,textarea:focus{outline:none;border-color:rgba(232,160,48,0.4)!important;}
        select option{background:#040407;color:#c8c0a8;}
        body{font-family:'DM Mono','Courier New',monospace;}
        @supports (-webkit-touch-callout:none){
          input,select,textarea{font-size:16px;}
        }
        @media print{button{display:none!important}}
      `}</style>
    </div>
  );
}

function Row({label,children}){
  return(
    <div style={{marginBottom:28}}>
      <div style={{display:"flex",alignItems:"center",gap:12,marginBottom:16}}>
        <div style={{fontSize:8,letterSpacing:4,color:"#e8a030",textTransform:"uppercase",fontWeight:600,whiteSpace:"nowrap"}}>{label}</div>
        <div style={{flex:1,height:"1px",background:"rgba(232,160,48,0.12)"}}/>
      </div>
      {children}
    </div>
  );
}

const primaryBtn = {background:"#e8a030",border:"none",padding:"11px 22px",color:"#040407",fontWeight:600,fontSize:10,cursor:"pointer",fontFamily:"'DM Mono','Courier New',monospace",letterSpacing:2,textTransform:"uppercase",transition:"all 0.15s ease"};
const ghostBtn = {background:"transparent",border:"1px solid rgba(232,160,48,0.2)",padding:"10px 18px",color:"#4a4438",fontWeight:400,fontSize:10,cursor:"pointer",fontFamily:"'DM Mono','Courier New',monospace",letterSpacing:2,textTransform:"uppercase",transition:"all 0.15s ease"};
const SEL = {width:"100%",padding:"10px 12px",background:"transparent",border:"1px solid rgba(232,160,48,0.15)",color:"#c8c0a8",fontSize:12,fontFamily:"'DM Mono','Courier New',monospace",cursor:"pointer",outline:"none",boxSizing:"border-box"};
const qBtnS = {width:28,height:28,background:"transparent",border:"1px solid rgba(232,160,48,0.15)",color:"#e8a030",fontSize:14,cursor:"pointer",display:"flex",alignItems:"center",justifyContent:"center",fontWeight:400,lineHeight:1,flexShrink:0,fontFamily:"'DM Mono','Courier New',monospace"};
