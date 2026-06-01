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
  "Wiring Devices":{
    receptacles:   {low:150,high:350,label:"Receptacles (Outlets)",           unit:"each",   nec:"210.52",mat:12, hours:1.0},
    gfciOutlet:    {low:150,high:350,label:"GFCI Outlets",                    unit:"each",   nec:"210.8", mat:22, hours:1.0},
    afciOutlet:    {low:175,high:375,label:"AFCI Outlets",                    unit:"each",   nec:"210.12",mat:42, hours:1.0},
    switches:      {low:150,high:200,label:"Single-Pole Switches",            unit:"each",   nec:"404.2", mat:10, hours:0.75},
    threewaySwitch:{low:175,high:250,label:"3-Way Switches",                  unit:"each",   nec:"404.2", mat:18, hours:1.25},
    dimmers:       {low:175,high:260,label:"Dimmer Switches",                 unit:"each",   nec:"404.14",mat:30, hours:1.0},
    outdoorOutlet: {low:180,high:350,label:"Outdoor GFCI Outlets",            unit:"each",   nec:"210.8(A)(3)",mat:30,hours:1.5},
    usbOutlet:     {low:175,high:310,label:"USB Combo Outlets",               unit:"each",   nec:"210.52",mat:35, hours:1.0},
    tamperResist:  {low:150,high:300,label:"Tamper-Resistant Receptacles",    unit:"each",   nec:"406.12",mat:15, hours:0.75},
    evCharger:     {low:750,high:1800,label:"EV Charger Level 2 (240V)",      unit:"each",   nec:"625.40",mat:220,hours:5.0},
  },
  "Lighting":{
    snapLED:       {low:125,high:300,label:"Snap-In LED Recessed Lights",     unit:"each",   nec:"410.116",mat:28,hours:1.0},
    canLight:      {low:150,high:300,label:"Traditional Can Lights",          unit:"each",   nec:"410.116",mat:40,hours:1.5},
    wallLight:     {low:150,high:350,label:"Wall Sconces / Light Fixtures",   unit:"each",   nec:"410.36", mat:45,hours:1.25},
    ceilingFan:    {low:250,high:600,label:"Ceiling Fans (with light)",       unit:"each",   nec:"314.27", mat:85,hours:2.0},
    chandelierLight:{low:300,high:2000,label:"Chandelier / Heavy Fixture",    unit:"each",   nec:"314.27(D)",mat:120,hours:3.0},
    undercabinet:  {low:150,high:350,label:"Under-Cabinet Lighting",          unit:"each",   nec:"410.36", mat:45,hours:1.25},
    outdoorLight:  {low:150,high:350,label:"Outdoor Light Fixtures",          unit:"each",   nec:"410.10", mat:55,hours:1.5},
    motionLight:   {low:175,high:400,label:"Motion Sensor Lights",            unit:"each",   nec:"410.10", mat:65,hours:1.5},
  },
  "Panels & Service":{
    panel100:      {low:1500,high:3000,label:"100A Panel Replacement",        unit:"flat",   nec:"230.79",mat:450,hours:10},
    panel200:      {low:2000,high:4000,label:"200A Panel Replacement",        unit:"flat",   nec:"230.79",mat:650,hours:12},
    panel400:      {low:4000,high:8000,label:"400A Panel Upgrade",            unit:"flat",   nec:"230.79",mat:1400,hours:18},
    subpanel100:   {low:1000,high:2500,label:"100A Subpanel Install",         unit:"flat",   nec:"225.30",mat:380,hours:8},
    subpanel200:   {low:1500,high:3500,label:"200A Subpanel Install",         unit:"flat",   nec:"225.30",mat:550,hours:10},
    panelCircuit:  {low:200,high:500,label:"New Branch Circuit at Panel",     unit:"each",   nec:"210.11",mat:55, hours:2.5},
    meterBase:     {low:500,high:1200,label:"Meter Base Replacement",         unit:"flat",   nec:"230.66",mat:180,hours:5},
    groundRods:    {low:400,high:800,label:"Grounding Electrode System",      unit:"flat",   nec:"250.50",mat:90, hours:4},
    surgeProtector:{low:300,high:600,label:"Whole-Home Surge Protector",      unit:"each",   nec:"230.67",mat:130,hours:2},
    exteriorDisconn:{low:400,high:900,label:"Exterior Emergency Disconnect",  unit:"flat",   nec:"230.85",mat:150,hours:3},
  },
  "Appliance Circuits":{
    dryer240:      {low:250,high:600,label:"Dryer Circuit (240V / 30A)",      unit:"each",   nec:"210.11(C)(2)",mat:90,hours:3},
    range240:      {low:300,high:700,label:"Range/Oven Circuit (240V / 50A)", unit:"each",   nec:"210.19",mat:90, hours:3},
    acCircuit:     {low:250,high:600,label:"A/C Dedicated Circuit",           unit:"each",   nec:"440.62",mat:80, hours:2.5},
    hotTub:        {low:1000,high:2500,label:"Hot Tub / Spa Circuit",         unit:"each",   nec:"680.42",mat:280,hours:8},
    pool:          {low:1500,high:4000,label:"Pool Electrical (bonding+circuit)",unit:"flat",nec:"680.26",mat:450,hours:14},
    wellPump:      {low:500,high:1200,label:"Well Pump Circuit",              unit:"each",   nec:"430.22",mat:110,hours:4},
    generator:     {low:2000,high:5000,label:"Generator + Transfer Switch",   unit:"flat",   nec:"702.12",mat:900,hours:14},
    solarTie:      {low:1000,high:3000,label:"Solar PV Interconnect",         unit:"flat",   nec:"705.12",mat:350,hours:10},
    batteryBackup: {low:3000,high:8000,label:"Battery Backup System",         unit:"flat",   nec:"702.4", mat:1500,hours:16},
  },
  "Safety Devices":{
    smokeDetector: {low:100,high:200,label:"Smoke Detectors (hardwired)",     unit:"each",   nec:"760.32",mat:28, hours:1.0},
    coDetector:    {low:100,high:200,label:"CO Detectors (hardwired)",        unit:"each",   nec:"760.32",mat:32, hours:1.0},
    comboDet:      {low:120,high:220,label:"Combo Smoke/CO Detectors",        unit:"each",   nec:"760.32",mat:45, hours:1.0},
    afciBreaker:   {low:120,high:200,label:"AFCI Breakers",                   unit:"each",   nec:"210.12",mat:50, hours:1.0},
    gfciBreaker:   {low:120,high:200,label:"GFCI Breakers",                   unit:"each",   nec:"210.8", mat:50, hours:1.0},
  },
  "Wiring & Rough-In":{
    rewireRoom:    {low:500,high:1200,label:"Rewire Single Room",             unit:"each",   nec:"310.12",mat:180,hours:6},
    rewireHome:    {low:10000,high:30000,label:"Full Home Rewire",            unit:"flat",   nec:"310.12",mat:4000,hours:100},
    aluminumFix:   {low:200,high:400,label:"Aluminum Wiring Fix (per outlet)",unit:"each",  nec:"110.14",mat:35, hours:2.0},
    lowVoltage:    {low:100,high:200,label:"Low Voltage (data/cable/phone)",  unit:"each",   nec:"800.24",mat:22, hours:1.0},
    conduitRun:    {low:8,  high:15, label:"Conduit Run (per linear foot)",   unit:"lin ft", nec:"358.10",mat:5,  hours:0.1},
    wireRun:       {low:4,  high:10, label:"Wire Pull (per linear foot)",     unit:"lin ft", nec:"310.15",mat:1.5,hours:0.04},
    junctionBox:   {low:100,high:250,label:"Junction Box (installed)",        unit:"each",   nec:"314.29",mat:10, hours:1.0},
  },
  "Outdoor & Specialty":{
    outdoorPanel:  {low:800,high:2000,label:"Outdoor Subpanel",               unit:"flat",  nec:"225.30", mat:250,hours:8},
    landscape:     {low:400,high:1200,label:"Landscape Lighting System",      unit:"flat",   nec:"411.3",  mat:180,hours:6},
    shed:          {low:800,high:2000,label:"Shed / Detached Garage Electric",unit:"flat",   nec:"225.30", mat:250,hours:8},
    securityCamera:{low:150,high:300,label:"Security Camera Power",           unit:"each",   nec:"210.52", mat:25, hours:1.25},
    doorbell:      {low:150,high:350,label:"Doorbell / Video Doorbell",       unit:"each",   nec:"725.3",  mat:30, hours:1.5},
    poolLight:     {low:400,high:1000,label:"Pool / Spa Light",               unit:"each",   nec:"680.23", mat:200,hours:4},
  },
};

const CONDS = {
  openWalls:      {label:"Customer opens walls",          mult:0.85,sign:"-15%",clr:"#30d158"},
  finishedWalls:  {label:"Finished walls (fish wire)",    mult:1.20,sign:"+20%",clr:"#ffd60a"},
  oldWiring:      {label:"Old / knob-and-tube wiring",   mult:1.30,sign:"+30%",clr:"#ff453a"},
  newConstruction:{label:"New construction rough-in",     mult:0.88,sign:"-12%",clr:"#30d158"},
  atticAccess:    {label:"Attic / crawl space access",   mult:0.92,sign:"-8%", clr:"#30d158"},
  highCeilings:   {label:"High ceilings (10ft+)",        mult:1.15,sign:"+15%",clr:"#ffd60a"},
  hazmat:         {label:"Asbestos / hazmat present",    mult:1.40,sign:"+40%",clr:"#ff453a"},
};

const SQFT_PRESETS = {
  "Small — Under 1,000 sq ft":  {receptacles:10,switches:6,snapLED:6,smokeDetector:2,gfciOutlet:4,panelCircuit:2},
  "Medium — 1,000–2,000 sq ft": {receptacles:18,switches:10,snapLED:10,smokeDetector:3,gfciOutlet:6,panelCircuit:3},
  "Large — 2,000–3,500 sq ft":  {receptacles:28,switches:16,snapLED:16,smokeDetector:5,gfciOutlet:8,panelCircuit:5},
  "XL — 3,500+ sq ft":          {receptacles:40,switches:22,snapLED:22,smokeDetector:7,gfciOutlet:10,panelCircuit:7},
};

const US_STATES = ["AL","AK","AZ","AR","CA","CO","CT","DE","FL","GA","HI","ID","IL","IN","IA","KS","KY","LA","ME","MD","MA","MI","MN","MS","MO","MT","NE","NV","NH","NJ","NM","NY","NC","ND","OH","OK","OR","PA","RI","SC","SD","TN","TX","UT","VT","VA","WA","WV","WI","WY"];

const T = {
  en:{disclaimer:"Estimates based on regional averages · Final pricing subject to on-site verification · Always pull permits",langBtn:"ES"},
  es:{disclaimer:"Estimados basados en promedios regionales · Precio final sujeto a verificación · Siempre saque permisos",langBtn:"EN"},
};

const fmt  = n => "$"+n.toLocaleString();
const fmtR = (lo,hi) => lo===hi ? fmt(lo) : `${fmt(lo)}–${fmt(hi)}`;
const today = () => new Date().toLocaleDateString("en-US",{year:"numeric",month:"long",day:"numeric"});
const invNum = () => "VQ-"+Date.now().toString().slice(-6);

// ─── APPLE DARK DESIGN TOKENS ─────────────────────────────────────────────────
const G = {
  // Backgrounds — layered like iOS
  bg:          "#000000",       // true black (OLED)
  bgElevated:  "#0d0d0d",       // slightly elevated
  surface:     "#1c1c1e",       // card / grouped background
  surfaceRaised:"#2c2c2e",      // raised card
  surfaceTert: "#3a3a3c",       // tertiary fill
  // Separators
  sep:         "rgba(255,255,255,0.08)",
  sepStrong:   "rgba(255,255,255,0.14)",
  // Labels
  label:       "#ffffff",
  labelSec:    "rgba(235,235,245,0.6)",
  labelTert:   "rgba(235,235,245,0.3)",
  labelQuat:   "rgba(235,235,245,0.18)",
  // Accent — VoltQuote amber
  amber:       "#f5a623",
  amberDim:    "rgba(245,166,35,0.18)",
  amberGlow:   "rgba(245,166,35,0.35)",
  // System colors (Apple)
  blue:        "#0a84ff",
  blueL:       "rgba(10,132,255,0.15)",
  green:       "#30d158",
  greenL:      "rgba(48,209,88,0.15)",
  red:         "#ff453a",
  redL:        "rgba(255,69,58,0.15)",
  yellow:      "#ffd60a",
  yellowL:     "rgba(255,214,10,0.15)",
  orange:      "#ff9f0a",
  // Glass
  glass:       "rgba(28,28,30,0.85)",
  glassBorder: "rgba(255,255,255,0.1)",
};

// ─── SHARED STYLES ────────────────────────────────────────────────────────────
const PB = {
  background: G.amber,
  border:"none",
  borderRadius:12,
  padding:"11px 22px",
  color:"#000",
  fontWeight:700,
  fontSize:14,
  cursor:"pointer",
  fontFamily:"inherit",
  letterSpacing:-0.1,
  transition:"all 0.15s ease",
};
const GB = {
  background:"rgba(255,255,255,0.08)",
  border:"1px solid rgba(255,255,255,0.12)",
  borderRadius:12,
  padding:"10px 20px",
  color:G.label,
  fontWeight:600,
  fontSize:14,
  cursor:"pointer",
  fontFamily:"inherit",
  transition:"all 0.15s ease",
};
const SEL = {
  width:"100%",
  padding:"11px 14px",
  background:G.surfaceRaised,
  border:"1px solid "+G.sep,
  borderRadius:10,
  color:G.label,
  fontSize:15,
  fontFamily:"inherit",
  cursor:"pointer",
  outline:"none",
  boxSizing:"border-box",
  appearance:"none",
  WebkitAppearance:"none",
};
const QB = {
  width:32,
  height:32,
  borderRadius:8,
  background:G.surfaceRaised,
  border:"1px solid "+G.sep,
  color:G.amber,
  fontSize:17,
  cursor:"pointer",
  display:"flex",
  alignItems:"center",
  justifyContent:"center",
  fontWeight:700,
  flexShrink:0,
  transition:"all 0.15s",
};

export default function VoltQuote() {
  const [lang,setLang] = useState("en");
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
  const [savedOk,setSavedOk] = useState(false);
  const [expanded,setExpanded] = useState({"Wiring Devices":true});
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
  const [necSearch,setNecSearch] = useState(""); // eslint-disable-line no-unused-vars
  const fileRef = useRef(null);

  const rm = REGIONS[region]||1.0;
  const cm = () => Object.entries(cond).reduce((m,[k,v])=>v?m*CONDS[k].mult:m,1.0);
  const totalOh = Object.values(overhead).reduce((a,b)=>a+(Number(b)||0),0);
  const trueRate = targetHrs>0 ? Math.ceil((totalOh/targetHrs)*(1+profitPct/100)) : 0;
  const hasItems = Object.values(qty).some(v=>v>0);
  const totalItems = Object.values(qty).reduce((a,b)=>a+(b||0),0);

  useEffect(()=>{
    try{const s=localStorage.getItem("vq_hist2");if(s)setHistory(JSON.parse(s));}catch{}
  },[]);

  const calculate = () => {
    let tLo=0,tHi=0,tMat=0,tHrs=0;
    const items=[];
    const c=cm();
    Object.entries(CATS).forEach(([cat,jobs])=>{
      Object.entries(jobs).forEach(([key,job])=>{
        const q=qty[key];if(!q||q<=0) return;
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
      items.push({label:"Permit & Inspection (est.)",qty:null,low:fee,high:fee,mat:0,hrs:0,nec:null});
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
          system:`You are an expert residential electrician and NEC 2023 authority. Answer concisely with specific code article references. Be practical and clear. ${ctx}`,
          messages:hist})});
      const d=await r.json();
      setChatHistory([...hist,{role:"assistant",content:d.content?.map(b=>b.text||"").join("")||""}]);
    }catch{setChatHistory([...hist,{role:"assistant",content:"Unable to respond. Please try again."}]);}
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
            {type:"text",text:"You are a residential electrician. Analyze this photo. Identify electrical work visible or needed: outlets, switches, panels, lighting, fans, smoke detectors, GFCI. Write a short paragraph then a bulleted list of specific estimate items."}
          ]}]})});
      const d=await r.json();
      setPhotoAnalysis(d.content?.map(b=>b.text||"").join("")||"");
    }catch{setPhotoAnalysis("Could not analyze photo. Please try again.");}
    setPhotoLoading(false);
  };

  const saveEst = ()=>{
    if(!result) return;
    const est={...result,summary:aiSum};
    const updated=[est,...history].slice(0,30);
    setHistory(updated);
    try{localStorage.setItem("vq_hist2",JSON.stringify(updated));}catch{}
    setSavedOk(true);setTimeout(()=>setSavedOk(false),2000);
  };

  const copyQuote = ()=>{
    if(!result) return;
    const co=cName?`\n${cName}${cPhone?" · "+cPhone:""}${cLic?" · Lic#"+cLic:""}`:"";
    const lines=result.items.map(i=>`  ${i.label}${i.qty?` ×${i.qty}`:""}: ${fmtR(i.low,i.high)}`).join("\n");
    const total=result.mode==="tm"?fmt(result.tm):`${fmt(result.tLo)}–${fmt(result.tHi)}`;
    navigator.clipboard.writeText(`VOLTQUOTE ESTIMATE${co}\n${result.region} · ${result.date} · ${result.id}\n${"─".repeat(50)}\n${lines}\n${"─".repeat(50)}\nTOTAL: ${total}  |  ~${result.tHrs} labor hours\n\n${aiSum}\n\nValid 30 days. All work per NEC 2023 standards.`);
    setCopied(true);setTimeout(()=>setCopied(false),2000);
  };

  // ── INVOICE ──────────────────────────────────────────────────────────────────
  if(showInvoice&&result) return (
    <div style={{background:G.bg,minHeight:"100vh",padding:"32px 16px",fontFamily:"-apple-system,BlinkMacSystemFont,'SF Pro Display','SF Pro Text','Helvetica Neue',system-ui,sans-serif"}}>
      <div style={{maxWidth:640,margin:"0 auto"}}>
        {/* Invoice header */}
        <div style={{background:G.surface,borderRadius:"20px 20px 0 0",padding:"32px",border:`1px solid ${G.sep}`,borderBottom:"none",display:"flex",justifyContent:"space-between",alignItems:"flex-start"}}>
          <div>
            <div style={{display:"flex",alignItems:"center",gap:10,marginBottom:16}}>
              <div style={{width:36,height:36,background:G.amber,borderRadius:10,display:"flex",alignItems:"center",justifyContent:"center",fontSize:18,fontWeight:800,color:"#000"}}>⚡</div>
              <span style={{fontSize:11,fontWeight:700,color:G.amber,letterSpacing:2,textTransform:"uppercase"}}>VoltQuote</span>
            </div>
            <div style={{fontSize:20,fontWeight:700,color:G.label,letterSpacing:-0.5}}>{cName||"Professional Electrician"}</div>
            {cPhone&&<div style={{fontSize:13,color:G.labelSec,marginTop:4}}>📞 {cPhone}</div>}
            {cEmail&&<div style={{fontSize:13,color:G.labelSec}}>✉️ {cEmail}</div>}
            {cLic&&<div style={{fontSize:12,color:G.amber,marginTop:6,fontWeight:600}}>License #{cLic}</div>}
          </div>
          <div style={{textAlign:"right"}}>
            <div style={{fontSize:10,color:G.labelTert,textTransform:"uppercase",letterSpacing:2,marginBottom:4}}>Invoice</div>
            <div style={{fontSize:16,fontWeight:700,color:G.amber,fontFamily:"monospace"}}>{result.id}</div>
            <div style={{fontSize:10,color:G.labelTert,textTransform:"uppercase",letterSpacing:2,marginTop:12,marginBottom:4}}>Date</div>
            <div style={{fontSize:13,color:G.labelSec}}>{result.date}</div>
            <div style={{fontSize:10,color:G.labelTert,textTransform:"uppercase",letterSpacing:2,marginTop:10,marginBottom:4}}>Terms</div>
            <div style={{fontSize:13,color:G.labelSec}}>Net {invoiceDue} days</div>
          </div>
        </div>
        {/* Invoice body */}
        <div style={{background:G.surface,border:`1px solid ${G.sep}`,borderTop:`1px solid ${G.sepStrong}`,borderRadius:"0 0 20px 20px",padding:"28px 32px"}}>
          {invoiceClient&&<div style={{marginBottom:20,paddingBottom:16,borderBottom:`1px solid ${G.sep}`}}>
            <div style={{fontSize:11,color:G.labelTert,textTransform:"uppercase",letterSpacing:2,marginBottom:6}}>Bill To</div>
            <div style={{fontSize:15,fontWeight:600,color:G.label}}>{invoiceClient}</div>
          </div>}
          <table style={{width:"100%",borderCollapse:"collapse",marginBottom:20}}>
            <thead>
              <tr style={{borderBottom:`1px solid ${G.sepStrong}`}}>
                {["Description","Qty","Amount"].map(h=>(
                  <th key={h} style={{textAlign:h==="Amount"?"right":"left",fontSize:11,color:G.labelTert,padding:"0 0 12px",fontWeight:600,textTransform:"uppercase",letterSpacing:1}}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {result.items.map((item,i)=>(
                <tr key={i} style={{borderBottom:`1px solid ${G.sep}`}}>
                  <td style={{padding:"12px 0",fontSize:14,color:G.label}}>{item.label}</td>
                  <td style={{padding:"12px 0",fontSize:14,color:G.labelSec,textAlign:"center"}}>{item.qty||"—"}</td>
                  <td style={{padding:"12px 0",fontSize:14,fontWeight:600,color:G.label,textAlign:"right",fontFamily:"monospace"}}>{fmtR(item.low,item.high)}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <div style={{display:"flex",justifyContent:"flex-end",marginBottom:24}}>
            <div style={{width:260,borderTop:`1px solid ${G.sepStrong}`,paddingTop:16}}>
              <div style={{display:"flex",justifyContent:"space-between",alignItems:"center"}}>
                <span style={{fontSize:15,fontWeight:600,color:G.labelSec}}>Total</span>
                <span style={{fontSize:26,fontWeight:700,color:G.amber,fontFamily:"monospace"}}>{result.mode==="tm"?fmt(result.tm):`${fmt(result.tLo)}–${fmt(result.tHi)}`}</span>
              </div>
            </div>
          </div>
          {invoiceNotes&&<div style={{background:G.amberDim,borderRadius:12,padding:16,marginBottom:16,border:`1px solid ${G.amberGlow}`}}>
            <div style={{fontSize:11,color:G.amber,fontWeight:600,textTransform:"uppercase",letterSpacing:1,marginBottom:6}}>Notes</div>
            <div style={{fontSize:13,color:G.labelSec,lineHeight:1.7}}>{invoiceNotes}</div>
          </div>}
          <div style={{fontSize:12,color:G.labelTert,textAlign:"center",lineHeight:1.9}}>
            All work performed to NEC 2023 standards · Estimate valid 30 days
          </div>
        </div>
        <div style={{display:"flex",gap:10,marginTop:16,justifyContent:"center"}}>
          <button onClick={()=>window.print()} style={PB}>🖨️ Print / Save PDF</button>
          <button onClick={()=>setShowInvoice(false)} style={GB}>← Back</button>
        </div>
      </div>
    </div>
  );

  // ── CUSTOMER VIEW ────────────────────────────────────────────────────────────
  if(showCustomer&&result) return (
    <div style={{background:G.bg,minHeight:"100vh",padding:"32px 16px",fontFamily:"-apple-system,BlinkMacSystemFont,'SF Pro Display','SF Pro Text','Helvetica Neue',system-ui,sans-serif"}}>
      <div style={{maxWidth:600,margin:"0 auto"}}>
        <div style={{background:G.surface,borderRadius:"20px 20px 0 0",padding:"28px",border:`1px solid ${G.sep}`,borderBottom:"none"}}>
          <div style={{display:"flex",alignItems:"center",gap:10,marginBottom:16}}>
            <div style={{width:30,height:30,background:G.amber,borderRadius:8,display:"flex",alignItems:"center",justifyContent:"center",fontSize:15,fontWeight:800,color:"#000"}}>⚡</div>
            <span style={{fontSize:10,fontWeight:700,color:G.amber,letterSpacing:2,textTransform:"uppercase"}}>VoltQuote Estimate</span>
          </div>
          <div style={{fontSize:19,fontWeight:700,color:G.label,letterSpacing:-0.5}}>{cName||"Professional Electrician"}</div>
          {cPhone&&<div style={{fontSize:13,color:G.labelSec,marginTop:3}}>📞 {cPhone}</div>}
          {cEmail&&<div style={{fontSize:13,color:G.labelSec}}>✉️ {cEmail}</div>}
          {cLic&&<div style={{fontSize:11,color:G.amber,marginTop:4}}>License #{cLic}</div>}
          <div style={{fontSize:12,color:G.labelTert,marginTop:8}}>{result.date} · {result.region}</div>
        </div>
        <div style={{background:G.surface,border:`1px solid ${G.sep}`,borderTop:`1px solid ${G.sepStrong}`,borderRadius:"0 0 20px 20px",padding:"24px 28px"}}>
          {result.items.filter(i=>i.qty).map((item,i)=>(
            <div key={i} style={{display:"flex",justifyContent:"space-between",padding:"12px 0",borderBottom:`1px solid ${G.sep}`}}>
              <div>
                <div style={{fontSize:14,fontWeight:500,color:G.label}}>{item.label}</div>
                <div style={{fontSize:12,color:G.labelTert,marginTop:2}}>Qty: {item.qty}</div>
              </div>
              <div style={{fontSize:14,fontWeight:600,color:G.amber,fontFamily:"monospace"}}>{fmtR(item.low,item.high)}</div>
            </div>
          ))}
          <div style={{display:"flex",justifyContent:"space-between",alignItems:"center",padding:"18px 0",borderTop:`1px solid ${G.sepStrong}`,marginTop:4}}>
            <div>
              <div style={{fontSize:16,fontWeight:700,color:G.label}}>Total Estimate</div>
              <div style={{fontSize:12,color:G.labelTert,marginTop:2}}>~{result.tHrs} labor hours</div>
            </div>
            <div style={{fontSize:28,fontWeight:700,color:G.amber,fontFamily:"monospace"}}>
              {result.mode==="tm"?fmt(result.tm):`${fmt(result.tLo)}–${fmt(result.tHi)}`}
            </div>
          </div>
          {aiSum&&<div style={{background:G.amberDim,borderRadius:12,padding:16,marginBottom:16,border:`1px solid ${G.amberGlow}`,fontSize:13,lineHeight:1.8,color:G.labelSec}}>{aiSum}</div>}
          <div style={{fontSize:12,color:G.labelTert,textAlign:"center",lineHeight:1.9}}>
            Estimate valid 30 days · Final price subject to on-site inspection<br/>All work performed to NEC 2023 standards
          </div>
        </div>
        <div style={{display:"flex",gap:10,marginTop:16,justifyContent:"center"}}>
          <button onClick={()=>{setShowInvoice(true);setShowCustomer(false);}} style={PB}>Convert to Invoice</button>
          <button onClick={()=>setShowCustomer(false)} style={GB}>← Back</button>
        </div>
      </div>
    </div>
  );

  // ── LANDING PAGE ─────────────────────────────────────────────────────────────
  if(view==="landing") return (
    <div style={{background:G.bg,color:G.label,fontFamily:"-apple-system,BlinkMacSystemFont,'SF Pro Display','SF Pro Text','Helvetica Neue',system-ui,sans-serif",minHeight:"100vh"}}>
      {/* Nav */}
      <div style={{
        background:"rgba(0,0,0,0.75)",
        backdropFilter:"saturate(180%) blur(20px)",
        WebkitBackdropFilter:"saturate(180%) blur(20px)",
        borderBottom:`1px solid ${G.sep}`,
        padding:"14px 32px",
        display:"flex",justifyContent:"space-between",alignItems:"center",
        position:"sticky",top:0,zIndex:100,
      }}>
        <div style={{display:"flex",alignItems:"center",gap:12}}>
          <div style={{width:34,height:34,background:G.amber,borderRadius:10,display:"flex",alignItems:"center",justifyContent:"center",fontSize:17,fontWeight:800,color:"#000",boxShadow:`0 0 20px ${G.amberGlow}`}}>⚡</div>
          <div>
            <div style={{fontSize:16,fontWeight:700,color:G.label,letterSpacing:-0.5}}>VoltQuote</div>
            <div style={{fontSize:10,color:G.labelTert,fontWeight:500}}>Electrical Estimator</div>
          </div>
        </div>
        <div style={{display:"flex",gap:10,alignItems:"center"}}>
          <button onClick={()=>setLang(lang==="en"?"es":"en")} style={GB}>{T[lang].langBtn}</button>
          <button onClick={()=>setView("app")} style={PB}>Try Free →</button>
        </div>
      </div>

      {/* Hero */}
      <div style={{
        padding:"110px 32px 90px",
        textAlign:"center",
        position:"relative",
        overflow:"hidden",
        background:"radial-gradient(ellipse 80% 50% at 50% -10%, rgba(245,166,35,0.15) 0%, transparent 60%)",
      }}>
        <div style={{position:"relative",zIndex:1,maxWidth:720,margin:"0 auto"}}>
          <div style={{display:"inline-flex",alignItems:"center",gap:8,background:"rgba(245,166,35,0.1)",border:`1px solid rgba(245,166,35,0.25)`,borderRadius:40,padding:"6px 18px",marginBottom:32}}>
            <div style={{width:6,height:6,borderRadius:"50%",background:G.amber,boxShadow:`0 0 10px ${G.amber}`}}/>
            <span style={{fontSize:12,fontWeight:600,color:G.amber,letterSpacing:0.5}}>2026 Verified Pricing · 55+ US Markets</span>
          </div>
          <h1 style={{fontSize:"clamp(38px,6vw,68px)",fontWeight:800,color:G.label,lineHeight:1.05,margin:"0 0 20px",letterSpacing:-2}}>
            Professional electrical<br/>
            <span style={{color:G.amber}}>estimates in minutes.</span>
          </h1>
          <p style={{fontSize:18,color:G.labelSec,lineHeight:1.75,maxWidth:540,margin:"0 auto 48px",fontWeight:400}}>
            Location-adjusted pricing for every US market. NEC 2023 code reference built in. AI-powered summaries. Built for electricians who work from the truck.
          </p>
          <div style={{display:"flex",gap:14,justifyContent:"center",flexWrap:"wrap",marginBottom:24}}>
            <button onClick={()=>setView("app")} style={{...PB,padding:"18px 40px",fontSize:16,borderRadius:14,boxShadow:`0 0 40px ${G.amberGlow}`}}>⚡ Start Free Estimate</button>
            <button onClick={()=>{setView("app");setTab("nec");}} style={{...GB,padding:"17px 32px",fontSize:15,borderRadius:14}}>Browse NEC 2023 →</button>
          </div>
          <p style={{fontSize:12,color:G.labelTert,letterSpacing:0.5}}>Free · No signup · Works offline</p>
        </div>
      </div>

      {/* Stats bar */}
      <div style={{borderTop:`1px solid ${G.sep}`,borderBottom:`1px solid ${G.sep}`}}>
        <div style={{maxWidth:800,margin:"0 auto",display:"grid",gridTemplateColumns:"repeat(4,1fr)"}}>
          {[["55+","US Cities"],["60+","Line Items"],["NEC 2023","Code Built-In"],["$0","To Start"]].map(([v,l],i)=>(
            <div key={i} style={{padding:"28px 20px",textAlign:"center",borderRight:i<3?`1px solid ${G.sep}`:"none"}}>
              <div style={{fontSize:26,fontWeight:800,color:G.amber,marginBottom:4,letterSpacing:-1}}>{v}</div>
              <div style={{fontSize:11,color:G.labelTert,fontWeight:600,textTransform:"uppercase",letterSpacing:1}}>{l}</div>
            </div>
          ))}
        </div>
      </div>

      {/* Features */}
      <div style={{maxWidth:960,margin:"0 auto",padding:"80px 24px"}}>
        <div style={{textAlign:"center",marginBottom:56}}>
          <h2 style={{fontSize:36,fontWeight:800,color:G.label,margin:"0 0 12px",letterSpacing:-1}}>Everything you need on the job</h2>
          <p style={{fontSize:16,color:G.labelSec,margin:0}}>Built by electricians, for electricians.</p>
        </div>
        <div style={{display:"grid",gridTemplateColumns:"repeat(auto-fit,minmax(260px,1fr))",gap:2}}>
          {[
            {icon:"📍",title:"Location-Based Pricing",desc:"Rates auto-adjust for 55+ cities. Your market, your prices."},
            {icon:"⚡",title:"60+ Line Items",desc:"Every residential job with 2026 verified pricing."},
            {icon:"📷",title:"Photo Analysis",desc:"Upload a room photo. AI identifies what work is needed instantly."},
            {icon:"📖",title:"NEC 2023 Reference",desc:"60+ residential articles. Plain English. Searchable. AI chat."},
            {icon:"📊",title:"Overhead Calculator",desc:"Know your real break-even rate before you bid."},
            {icon:"📄",title:"Invoice Generator",desc:"Estimate to professional invoice in one tap."},
            {icon:"💬",title:"AI Code Assistant",desc:"Ask any NEC question. Get the specific article reference."},
            {icon:"🌐",title:"English & Español",desc:"Full bilingual support. Switch in one tap."},
          ].map((f,i)=>(
            <div key={i} style={{background:G.surface,border:`1px solid ${G.sep}`,padding:"28px 24px",transition:"all 0.2s"}}>
              <div style={{fontSize:28,marginBottom:16}}>{f.icon}</div>
              <div style={{fontSize:15,fontWeight:700,color:G.label,marginBottom:8,letterSpacing:-0.3}}>{f.title}</div>
              <div style={{fontSize:14,color:G.labelSec,lineHeight:1.7}}>{f.desc}</div>
            </div>
          ))}
        </div>
      </div>

      {/* Pricing */}
      <div style={{borderTop:`1px solid ${G.sep}`,padding:"80px 24px"}}>
        <div style={{maxWidth:620,margin:"0 auto",textAlign:"center"}}>
          <h2 style={{fontSize:36,fontWeight:800,color:G.label,margin:"0 0 12px",letterSpacing:-1}}>Simple pricing</h2>
          <p style={{fontSize:16,color:G.labelSec,marginBottom:48}}>Start free. Upgrade when you're ready.</p>
          <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:2}}>
            {[
              {name:"Free",price:"$0",period:"forever",desc:["5 estimates / month","All core features","NEC 2023 reference","Works offline"],hi:false},
              {name:"Pro",price:"$9.99",period:"/ month",desc:["Unlimited estimates","Saved history","Invoice generator","AI chat + photo analysis"],hi:true},
            ].map(p=>(
              <div key={p.name} style={{background:p.hi?G.surfaceRaised:G.surface,border:`1px solid ${p.hi?G.amberGlow:G.sep}`,borderRadius:20,padding:"32px 24px",position:"relative",overflow:"hidden",textAlign:"left"}}>
                {p.hi&&<div style={{position:"absolute",top:0,left:0,right:0,height:2,background:G.amber}}/>}
                <div style={{fontSize:11,fontWeight:700,color:p.hi?G.amber:G.labelTert,textTransform:"uppercase",letterSpacing:2,marginBottom:14}}>{p.name}</div>
                <div style={{fontSize:44,fontWeight:800,color:G.label,lineHeight:1,marginBottom:4,letterSpacing:-2}}>{p.price}</div>
                <div style={{fontSize:13,color:G.labelTert,marginBottom:28}}>{p.period}</div>
                <div style={{borderTop:`1px solid ${G.sep}`,paddingTop:20}}>
                  {p.desc.map((d,i)=>(
                    <div key={i} style={{display:"flex",gap:10,alignItems:"flex-start",marginBottom:12}}>
                      <span style={{color:G.amber,fontSize:14,flexShrink:0,marginTop:1}}>✓</span>
                      <span style={{fontSize:14,color:G.labelSec,lineHeight:1.5}}>{d}</span>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* CTA */}
      <div style={{padding:"80px 24px",textAlign:"center",borderTop:`1px solid ${G.sep}`}}>
        <h2 style={{fontSize:44,fontWeight:800,color:G.label,margin:"0 0 10px",letterSpacing:-1.5}}>Your first estimate is free.</h2>
        <p style={{fontSize:16,color:G.labelSec,margin:"0 0 40px"}}>No credit card. No signup. Start in seconds.</p>
        <button onClick={()=>setView("app")} style={{...PB,padding:"20px 56px",fontSize:16,borderRadius:16,boxShadow:`0 0 40px ${G.amberGlow}`}}>⚡ Start Free Estimate</button>
      </div>

      <div style={{borderTop:`1px solid ${G.sep}`,padding:"20px 32px",display:"flex",justifyContent:"space-between",alignItems:"center",flexWrap:"wrap",gap:10}}>
        <div style={{fontSize:12,color:G.labelTert}}>© 2026 VoltQuote · voltquote.app</div>
        <div style={{fontSize:12,color:G.labelTert}}>NEC 2023 · Built for the trades</div>
      </div>
    </div>
  );

  // ── MAIN APP ─────────────────────────────────────────────────────────────────
  const TB = (t,icon,l) => (
    <button onClick={()=>setTab(t)} style={{
      padding:"10px 14px",cursor:"pointer",fontSize:12,fontFamily:"inherit",
      border:"none",whiteSpace:"nowrap",
      background:tab===t?"rgba(245,166,35,0.12)":"transparent",
      color:tab===t?G.amber:G.labelTert,
      borderBottom:tab===t?`2px solid ${G.amber}`:"2px solid transparent",
      fontWeight:tab===t?700:500,
      transition:"all 0.15s",
      display:"flex",alignItems:"center",gap:6,
      borderRadius:tab===t?"6px 6px 0 0":"0",
    }}><span style={{fontSize:13}}>{icon}</span>{l}</button>
  );

  return (
    <div style={{background:G.bg,minHeight:"100svh",fontFamily:"-apple-system,BlinkMacSystemFont,'SF Pro Display','SF Pro Text','Helvetica Neue',system-ui,sans-serif",color:G.label}}>
      {/* App Header */}
      <div style={{
        background:"rgba(0,0,0,0.8)",
        backdropFilter:"saturate(180%) blur(20px)",
        WebkitBackdropFilter:"saturate(180%) blur(20px)",
        borderBottom:`1px solid ${G.sep}`,
        padding:"0 20px",
        position:"sticky",top:0,zIndex:100,
      }}>
        <div style={{maxWidth:900,margin:"0 auto"}}>
          <div style={{display:"flex",alignItems:"center",gap:12,paddingTop:12,paddingBottom:4}}>
            <button onClick={()=>setView("landing")} style={{background:"none",border:"none",cursor:"pointer",padding:0,display:"flex",alignItems:"center",gap:10}}>
              <div style={{width:30,height:30,background:G.amber,borderRadius:8,display:"flex",alignItems:"center",justifyContent:"center",fontSize:15,fontWeight:800,color:"#000"}}>⚡</div>
              <div style={{textAlign:"left"}}>
                <div style={{fontSize:15,fontWeight:700,color:G.label,lineHeight:1,letterSpacing:-0.5}}>VoltQuote</div>
                <div style={{fontSize:10,color:G.labelTert,fontWeight:500}}>Electrical Estimator</div>
              </div>
            </button>
            <div style={{flex:1}}/>
            {totalItems>0&&(
              <div style={{background:G.amberDim,border:`1px solid ${G.amberGlow}`,borderRadius:20,padding:"4px 12px",fontSize:12,color:G.amber,fontWeight:700}}>{totalItems} items</div>
            )}
            <button onClick={()=>setLang(lang==="en"?"es":"en")} style={{...GB,padding:"6px 12px",fontSize:11}}>{T[lang].langBtn}</button>
          </div>
          <div style={{display:"flex",overflowX:"auto",gap:0,scrollbarWidth:"none"}}>
            {TB("estimator","⚡","Estimate")}
            {TB("sqft","📐","Sq Ft")}
            {TB("photo","📷","Photo")}
            {TB("overhead","💰","Overhead")}
            {TB("contractor","🪪","Profile")}
            {TB("ask","💬","Ask AI")}
            {TB("history","🗂","History")}
            {TB("nec","📖","NEC 2023")}
          </div>
        </div>
      </div>

      <div style={{maxWidth:900,margin:"0 auto",padding:"28px 20px"}}>

        {/* ESTIMATOR */}
        {tab==="estimator"&&<>
          {photoAnalysis&&(
            <div style={{background:G.greenL,border:`1px solid ${G.green}33`,borderRadius:16,padding:16,marginBottom:24}}>
              <div style={{fontSize:11,fontWeight:700,color:G.green,textTransform:"uppercase",letterSpacing:1,marginBottom:8}}>📷 Photo Analysis</div>
              <p style={{margin:0,fontSize:13,color:G.labelSec,lineHeight:1.7,whiteSpace:"pre-wrap"}}>{photoAnalysis}</p>
              <button onClick={()=>setPhotoAnalysis("")} style={{marginTop:10,fontSize:11,color:G.labelTert,background:"none",border:"none",cursor:"pointer"}}>✕ Dismiss</button>
            </div>
          )}

          <Section title="Location & Pricing Mode">
            <div style={{display:"grid",gridTemplateColumns:"1fr auto",gap:10,marginBottom:12}}>
              <div style={{position:"relative"}}>
                <select value={region} onChange={e=>setRegion(e.target.value)} style={SEL}>
                  {Object.keys(REGIONS).map(r=><option key={r} value={r}>{r}</option>)}
                </select>
                <div style={{position:"absolute",right:12,top:"50%",transform:"translateY(-50%)",color:G.labelTert,pointerEvents:"none",fontSize:12}}>▾</div>
              </div>
              <div style={{background:G.amberDim,border:`1px solid ${G.amberGlow}`,borderRadius:12,padding:"10px 16px",textAlign:"center",minWidth:80}}>
                <div style={{fontSize:10,color:G.amber,fontWeight:600,textTransform:"uppercase",letterSpacing:1,marginBottom:2}}>Rate</div>
                <div style={{fontSize:20,fontWeight:800,color:G.amber,letterSpacing:-0.5}}>{rm.toFixed(2)}x</div>
              </div>
            </div>
            <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:8}}>
              {[["flat","Flat Rate"],["tm","Time & Material"]].map(([v,l])=>(
                <button key={v} onClick={()=>setMode(v)} style={{
                  padding:"12px",cursor:"pointer",fontSize:13,fontFamily:"inherit",
                  background:mode===v?G.amber:G.surfaceRaised,
                  border:`1px solid ${mode===v?"transparent":G.sep}`,
                  borderRadius:12,
                  color:mode===v?"#000":G.labelSec,
                  fontWeight:mode===v?700:500,
                  transition:"all 0.15s",
                }}>{l}</button>
              ))}
            </div>
            {mode==="tm"&&(
              <div style={{marginTop:12,background:G.surfaceRaised,borderRadius:12,padding:16,border:`1px solid ${G.sep}`}}>
                <div style={{fontSize:13,color:G.labelSec,marginBottom:10,fontWeight:500}}>Hourly rate: <strong style={{color:G.amber}}>${rate}/hr</strong></div>
                <input type="range" min={50} max={200} value={rate} onChange={e=>setRate(Number(e.target.value))} style={{width:"100%",accentColor:G.amber,height:4,cursor:"pointer"}}/>
                <div style={{display:"flex",justifyContent:"space-between",fontSize:11,color:G.labelTert,marginTop:6}}>
                  <span>$50</span><span>$125</span><span>$200/hr</span>
                </div>
              </div>
            )}
          </Section>

          <Section title="Scope of Work">
            {Object.entries(CATS).map(([cat,jobs])=>(
              <div key={cat} style={{marginBottom:4}}>
                <button onClick={()=>setExpanded(e=>({...e,[cat]:!e[cat]}))} style={{
                  width:"100%",display:"flex",justifyContent:"space-between",alignItems:"center",
                  background:expanded[cat]?"rgba(245,166,35,0.1)":G.surfaceRaised,
                  border:`1px solid ${expanded[cat]?G.amberGlow:G.sep}`,
                  borderRadius:expanded[cat]?"12px 12px 0 0":"12px",
                  padding:"13px 16px",cursor:"pointer",transition:"all 0.15s",
                }}>
                  <span style={{fontSize:13,fontWeight:700,color:expanded[cat]?G.amber:G.label,letterSpacing:-0.2}}>{cat}</span>
                  <div style={{display:"flex",alignItems:"center",gap:8}}>
                    {Object.keys(jobs).some(k=>qty[k]>0)&&(
                      <span style={{fontSize:11,background:G.amber,color:"#000",borderRadius:20,padding:"2px 10px",fontWeight:700}}>{Object.keys(jobs).filter(k=>qty[k]>0).length}</span>
                    )}
                    <span style={{color:expanded[cat]?G.amber:G.labelTert,fontSize:11}}>{expanded[cat]?"▲":"▼"}</span>
                  </div>
                </button>
                {expanded[cat]&&(
                  <div style={{border:`1px solid ${G.amberGlow}`,borderTop:"none",borderRadius:"0 0 12px 12px",overflow:"hidden"}}>
                    {Object.entries(jobs).map(([key,job],idx)=>(
                      <div key={key} style={{
                        display:"flex",alignItems:"center",justifyContent:"space-between",
                        padding:"12px 16px",
                        background:qty[key]>0?G.amberDim:idx%2===0?G.surface:G.surfaceRaised,
                        borderBottom:`1px solid ${G.sep}`,
                      }}>
                        <div style={{flex:1,minWidth:0}}>
                          <div style={{fontSize:14,color:qty[key]>0?G.label:G.labelSec,marginBottom:3,fontWeight:qty[key]>0?600:400}}>{job.label}</div>
                          <div style={{display:"flex",gap:8,flexWrap:"wrap",alignItems:"center"}}>
                            <span style={{fontSize:11,color:G.labelTert,fontWeight:500}}>{fmt(Math.round(job.low*rm))}–{fmt(Math.round(job.high*rm))}/{job.unit}</span>
                            <span style={{fontSize:11,color:G.labelTert}}>~{job.hours}h</span>
                            {job.nec&&(
                              <span onClick={()=>setTab("nec")} style={{fontSize:11,background:G.blueL,color:G.blue,borderRadius:6,padding:"2px 8px",fontWeight:600,cursor:"pointer"}}>§{job.nec}</span>
                            )}
                          </div>
                        </div>
                        <div style={{display:"flex",alignItems:"center",gap:10,flexShrink:0}}>
                          <button onClick={()=>setQty(q=>({...q,[key]:Math.max(0,(q[key]||0)-1)}))} style={QB}>−</button>
                          <span style={{minWidth:30,textAlign:"center",fontSize:16,fontWeight:800,color:qty[key]>0?G.amber:G.labelTert,fontFamily:"monospace"}}>{qty[key]||0}</span>
                          <button onClick={()=>setQty(q=>({...q,[key]:(q[key]||0)+1}))} style={QB}>+</button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ))}
          </Section>

          <Section title="Job Conditions">
            <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:8}}>
              {Object.entries(CONDS).map(([key,c])=>(
                <label key={key} style={{
                  display:"flex",alignItems:"flex-start",gap:10,cursor:"pointer",
                  background:cond[key]?G.amberDim:G.surfaceRaised,
                  border:`1px solid ${cond[key]?G.amberGlow:G.sep}`,
                  borderRadius:12,padding:"12px 14px",transition:"all 0.15s",
                }}>
                  <input type="checkbox" checked={!!cond[key]} onChange={e=>setCond(x=>({...x,[key]:e.target.checked}))} style={{accentColor:G.amber,width:15,height:15,marginTop:2}}/>
                  <div>
                    <div style={{fontSize:13,color:G.label,fontWeight:500,marginBottom:3}}>{c.label}</div>
                    <div style={{fontSize:11,color:c.clr,fontWeight:700}}>{c.sign}</div>
                  </div>
                </label>
              ))}
            </div>
          </Section>

          <Section title="Pricing Options">
            <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:8,marginBottom:12}}>
              {[{l:"Include materials markup",s:incMat,set:setIncMat},{l:"Include permit & inspection",s:incPermit,set:setIncPermit}].map(o=>(
                <label key={o.l} style={{
                  display:"flex",alignItems:"center",gap:10,cursor:"pointer",
                  background:o.s?G.amberDim:G.surfaceRaised,
                  border:`1px solid ${o.s?G.amberGlow:G.sep}`,
                  borderRadius:12,padding:"12px 14px",transition:"all 0.15s",
                }}>
                  <input type="checkbox" checked={o.s} onChange={e=>o.set(e.target.checked)} style={{accentColor:G.amber,width:15,height:15}}/>
                  <span style={{fontSize:13,color:G.labelSec,fontWeight:500}}>{o.l}</span>
                </label>
              ))}
            </div>
            {incMat&&(
              <div style={{background:G.surfaceRaised,borderRadius:12,padding:16,border:`1px solid ${G.sep}`}}>
                <div style={{fontSize:13,color:G.labelSec,marginBottom:10,fontWeight:500}}>Materials markup: <strong style={{color:G.amber}}>{markup}%</strong></div>
                <input type="range" min={0} max={50} value={markup} onChange={e=>setMarkup(Number(e.target.value))} style={{width:"100%",accentColor:G.amber,cursor:"pointer"}}/>
                <div style={{display:"flex",justifyContent:"space-between",fontSize:11,color:G.labelTert,marginTop:6}}>
                  <span>0%</span><span>25%</span><span>50%</span>
                </div>
              </div>
            )}
          </Section>

          <button onClick={calculate} disabled={!hasItems} style={{
            width:"100%",padding:"18px",marginBottom:28,
            background:hasItems?G.amber:G.surfaceRaised,
            border:"none",borderRadius:14,cursor:hasItems?"pointer":"not-allowed",
            fontSize:15,fontWeight:700,color:hasItems?"#000":G.labelTert,
            boxShadow:hasItems?`0 0 40px ${G.amberGlow}`:"none",
            transition:"all 0.2s",letterSpacing:-0.2,
          }}>
            {hasItems?"⚡ Generate Estimate":"Select items above to begin"}
          </button>

          {result&&(
            <div style={{background:G.surface,border:`1px solid ${G.sep}`,borderRadius:20,overflow:"hidden",boxShadow:"0 20px 60px rgba(0,0,0,0.5)"}}>
              {/* Result header */}
              <div style={{padding:"18px 20px",borderBottom:`1px solid ${G.sep}`,display:"flex",justifyContent:"space-between",alignItems:"center",flexWrap:"wrap",gap:8}}>
                <div>
                  <div style={{fontSize:12,color:G.amber,fontWeight:700,textTransform:"uppercase",letterSpacing:1,marginBottom:3}}>Estimate Ready</div>
                  <div style={{fontSize:12,color:G.labelTert}}>{result.region} · {rm.toFixed(2)}x · {result.date}</div>
                </div>
                <div style={{display:"flex",gap:6,flexWrap:"wrap"}}>
                  {[
                    {label:"👤 Client",fn:()=>setShowCustomer(true)},
                    {label:"📄 Invoice",fn:()=>setShowInvoice(true)},
                    {label:savedOk?"✓ Saved":"💾 Save",fn:saveEst,ok:savedOk},
                    {label:copied?"✓ Copied":"📋 Copy",fn:copyQuote,primary:true,ok:copied},
                  ].map(btn=>(
                    <button key={btn.label} onClick={btn.fn} style={{
                      background:btn.ok?G.greenL:btn.primary?G.amberDim:G.surfaceRaised,
                      color:btn.ok?G.green:btn.primary?G.amber:G.labelSec,
                      border:`1px solid ${btn.ok?G.green+"44":btn.primary?G.amberGlow:G.sep}`,
                      borderRadius:8,padding:"7px 14px",cursor:"pointer",fontSize:12,fontWeight:600,fontFamily:"inherit",transition:"all 0.15s",
                    }}>{btn.label}</button>
                  ))}
                </div>
              </div>
              {/* Line items */}
              {result.items.map((item,i)=>(
                <div key={i} style={{display:"flex",justifyContent:"space-between",alignItems:"center",padding:"12px 20px",borderBottom:`1px solid ${G.sep}`,background:i%2===0?G.surface:G.surfaceRaised}}>
                  <div>
                    <span style={{fontSize:14,color:G.label,fontWeight:500}}>{item.label}</span>
                    {item.qty&&<span style={{fontSize:11,color:G.labelTert,marginLeft:8}}>×{item.qty}</span>}
                    {item.hrs>0&&<span style={{fontSize:11,color:G.labelTert,marginLeft:6}}>{item.hrs}h</span>}
                    {item.nec&&<span style={{fontSize:10,background:G.blueL,color:G.blue,borderRadius:4,padding:"1px 6px",marginLeft:8,fontWeight:600}}>§{item.nec}</span>}
                  </div>
                  <div style={{fontSize:14,color:G.label,fontWeight:600,fontFamily:"monospace",flexShrink:0}}>{fmtR(item.low,item.high)}</div>
                </div>
              ))}
              {/* Total bar */}
              <div style={{display:"flex",justifyContent:"space-between",alignItems:"center",padding:"20px",borderTop:`1px solid ${G.sepStrong}`,background:G.surfaceRaised}}>
                <div>
                  <div style={{fontSize:15,fontWeight:700,color:G.label}}>Total Estimate</div>
                  <div style={{fontSize:12,color:G.labelTert,marginTop:2}}>~{result.tHrs} labor hours · Est. materials: {fmt(result.tMat)}</div>
                </div>
                <div style={{fontSize:28,fontWeight:800,color:G.amber,fontFamily:"monospace",letterSpacing:-1}}>
                  {result.mode==="tm"?fmt(result.tm):`${fmt(result.tLo)}–${fmt(result.tHi)}`}
                </div>
              </div>
              {/* AI Summary */}
              <div style={{padding:"18px 20px",background:G.amberDim,borderTop:`1px solid ${G.amberGlow}`}}>
                <div style={{fontSize:11,fontWeight:700,color:G.amber,textTransform:"uppercase",letterSpacing:1,marginBottom:8}}>Professional Summary</div>
                {loading
                  ?<div style={{fontSize:13,color:G.labelTert,fontStyle:"italic"}}>Generating summary...</div>
                  :<p style={{margin:0,fontSize:14,color:G.labelSec,lineHeight:1.8}}>{aiSum}</p>}
              </div>
            </div>
          )}
        </>}

        {/* SQ FT */}
        {tab==="sqft"&&<Section title="Quick Estimate by Square Footage">
          <p style={{fontSize:14,color:G.labelSec,marginTop:0,lineHeight:1.7,marginBottom:24}}>Select home size to auto-populate a typical rough-in scope. Refine from there.</p>
          <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:12}}>
            {Object.entries(SQFT_PRESETS).map(([name,preset])=>(
              <button key={name} onClick={()=>{setQty(q=>({...q,...preset}));setTab("estimator");setExpanded({"Wiring Devices":true,"Lighting":true,"Safety Devices":true});}} style={{
                background:G.surfaceRaised,border:`1px solid ${G.sep}`,
                borderRadius:16,padding:"20px",cursor:"pointer",textAlign:"left",transition:"all 0.2s",
              }}>
                <div style={{fontSize:14,fontWeight:700,color:G.label,marginBottom:8,letterSpacing:-0.3}}>{name}</div>
                <div style={{fontSize:12,color:G.labelTert,lineHeight:1.8}}>
                  {Object.entries(preset).slice(0,3).map(([k,v])=>`${v}× ${k}`).join(" · ")}
                </div>
                <div style={{marginTop:12,fontSize:13,fontWeight:700,color:G.amber}}>Apply to Estimate →</div>
              </button>
            ))}
          </div>
        </Section>}

        {/* PHOTO */}
        {tab==="photo"&&<Section title="Photo-to-Estimate">
          <p style={{fontSize:14,color:G.labelSec,marginTop:0,lineHeight:1.7}}>Upload a photo of any room, panel, or electrical area and AI identifies what work may be needed.</p>
          <input ref={fileRef} type="file" accept="image/*" onChange={handlePhoto} style={{display:"none"}}/>
          <button onClick={()=>fileRef.current?.click()} disabled={photoLoading} style={{
            width:"100%",padding:48,
            border:`2px dashed ${G.amberGlow}`,
            borderRadius:16,background:G.amberDim,cursor:"pointer",
            color:photoLoading?G.labelTert:G.amber,
            fontSize:15,fontWeight:600,transition:"all 0.2s",
          }}>
            {photoLoading?"🔍 Analyzing photo...":"📷 Upload Room / Area Photo"}
          </button>
          {photoAnalysis&&(
            <div style={{marginTop:16,background:G.surface,border:`1px solid ${G.sep}`,borderRadius:16,padding:20}}>
              <div style={{fontSize:11,fontWeight:700,color:G.amber,textTransform:"uppercase",letterSpacing:1,marginBottom:10}}>AI Analysis</div>
              <p style={{margin:0,fontSize:14,color:G.labelSec,lineHeight:1.8,whiteSpace:"pre-wrap"}}>{photoAnalysis}</p>
              <button onClick={()=>setTab("estimator")} style={{...PB,marginTop:16,fontSize:13}}>→ Build Estimate</button>
            </div>
          )}
        </Section>}

        {/* OVERHEAD */}
        {tab==="overhead"&&<Section title="Overhead & True Cost Calculator">
          <p style={{fontSize:14,color:G.labelSec,marginTop:0,lineHeight:1.7,marginBottom:20}}>Enter your monthly business costs to calculate your real break-even hourly rate.</p>
          <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:10,marginBottom:16}}>
            {[{k:"insurance",l:"Liability Insurance /mo"},{k:"vehicle",l:"Vehicle / Gas /mo"},{k:"tools",l:"Tools / Equipment /mo"},{k:"phone",l:"Phone / Software /mo"},{k:"misc",l:"Misc Overhead /mo"}].map(f=>(
              <div key={f.k}>
                <div style={{fontSize:11,color:G.labelTert,fontWeight:600,textTransform:"uppercase",letterSpacing:1,marginBottom:6}}>{f.l}</div>
                <div style={{display:"flex",alignItems:"center",border:`1px solid ${G.sep}`,borderRadius:10,overflow:"hidden",background:G.surfaceRaised}}>
                  <span style={{padding:"0 12px",color:G.amber,fontSize:15,fontWeight:700,borderRight:`1px solid ${G.sep}`,background:G.amberDim,alignSelf:"stretch",display:"flex",alignItems:"center"}}>$</span>
                  <input type="number" value={overhead[f.k]} onChange={e=>setOverhead(o=>({...o,[f.k]:Number(e.target.value)||0}))} style={{flex:1,background:"transparent",border:"none",color:G.label,fontSize:14,padding:"11px 12px",outline:"none",fontFamily:"inherit"}}/>
                </div>
              </div>
            ))}
          </div>
          <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:10,marginBottom:20}}>
            <div>
              <div style={{fontSize:11,color:G.labelTert,fontWeight:600,textTransform:"uppercase",letterSpacing:1,marginBottom:6}}>Billable Hours / Month</div>
              <input type="number" value={targetHrs} onChange={e=>setTargetHrs(Number(e.target.value)||1)} style={{...SEL}}/>
            </div>
            <div>
              <div style={{fontSize:11,color:G.labelTert,fontWeight:600,textTransform:"uppercase",letterSpacing:1,marginBottom:6}}>Desired Profit %</div>
              <input type="number" value={profitPct} onChange={e=>setProfitPct(Number(e.target.value)||0)} style={{...SEL}}/>
            </div>
          </div>
          <div style={{background:G.surfaceRaised,borderRadius:16,padding:24,border:`1px solid ${G.sep}`}}>
            <div style={{display:"grid",gridTemplateColumns:"1fr 1fr 1fr",gap:16,textAlign:"center",marginBottom:20}}>
              {[["Monthly Overhead",fmt(totalOh),G.red],["Break-Even Rate","$"+Math.ceil(totalOh/targetHrs)+"/hr",G.orange],["Your True Rate","$"+trueRate+"/hr",G.amber]].map(([l,v,c])=>(
                <div key={l}>
                  <div style={{fontSize:10,color:G.labelTert,fontWeight:600,textTransform:"uppercase",letterSpacing:1,marginBottom:8}}>{l}</div>
                  <div style={{fontSize:24,fontWeight:800,color:c,fontFamily:"monospace",letterSpacing:-1}}>{v}</div>
                </div>
              ))}
            </div>
            <div style={{borderTop:`1px solid ${G.sep}`,paddingTop:16,fontSize:13,color:G.labelTert,textAlign:"center",lineHeight:1.8}}>
              Your true rate includes overhead recovery + {profitPct}% profit margin.
            </div>
            <div style={{textAlign:"center",marginTop:14}}>
              <button onClick={()=>{setRate(trueRate);setMode("tm");setTab("estimator");}} style={GB}>→ Apply to T&M Estimator</button>
            </div>
          </div>
        </Section>}

        {/* CONTRACTOR */}
        {tab==="contractor"&&<Section title="Contractor Profile">
          <p style={{fontSize:14,color:G.labelSec,marginTop:0,lineHeight:1.7}}>Your info appears on client-facing quotes and invoices.</p>
          <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:10}}>
            {[{l:"Company / Name",v:cName,set:setCName,ph:"Smith Electric LLC"},{l:"Phone",v:cPhone,set:setCPhone,ph:"(607) 555-0100"},{l:"Email",v:cEmail,set:setCEmail,ph:"you@email.com"},{l:"License #",v:cLic,set:setCLic,ph:"ME-12345"},{l:"City",v:cCity,set:setCCity,ph:"Binghamton"}].map(f=>(
              <div key={f.l}>
                <div style={{fontSize:11,color:G.labelTert,fontWeight:600,textTransform:"uppercase",letterSpacing:1,marginBottom:6}}>{f.l}</div>
                <input value={f.v} onChange={e=>f.set(e.target.value)} placeholder={f.ph} style={{...SEL,fontSize:14}} />
              </div>
            ))}
            <div>
              <div style={{fontSize:11,color:G.labelTert,fontWeight:600,textTransform:"uppercase",letterSpacing:1,marginBottom:6}}>State</div>
              <div style={{position:"relative"}}>
                <select value={cState} onChange={e=>setCState(e.target.value)} style={SEL}>
                  {US_STATES.map(s=><option key={s} value={s}>{s}</option>)}
                </select>
                <div style={{position:"absolute",right:12,top:"50%",transform:"translateY(-50%)",color:G.labelTert,pointerEvents:"none",fontSize:12}}>▾</div>
              </div>
            </div>
          </div>
          {cName&&(
            <div style={{marginTop:24,background:G.surfaceRaised,borderRadius:16,padding:24,border:`1px solid ${G.sep}`}}>
              <div style={{fontSize:11,fontWeight:700,color:G.amber,textTransform:"uppercase",letterSpacing:1,marginBottom:14}}>Profile Preview</div>
              <div style={{fontSize:18,fontWeight:700,color:G.label,letterSpacing:-0.5}}>{cName}</div>
              {cPhone&&<div style={{fontSize:14,color:G.labelSec,marginTop:5}}>📞 {cPhone}</div>}
              {cEmail&&<div style={{fontSize:14,color:G.labelSec}}>✉️ {cEmail}</div>}
              {cLic&&<div style={{fontSize:13,color:G.amber,marginTop:5,fontWeight:600}}>License #{cLic}</div>}
              {(cCity||cState)&&<div style={{fontSize:13,color:G.labelTert,marginTop:3}}>{[cCity,cState].filter(Boolean).join(", ")}</div>}
            </div>
          )}
          <div style={{marginTop:24}}>
            <div style={{fontSize:11,color:G.labelTert,fontWeight:700,textTransform:"uppercase",letterSpacing:1,marginBottom:14}}>Invoice Defaults</div>
            <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:10}}>
              <div>
                <div style={{fontSize:11,color:G.labelTert,fontWeight:600,textTransform:"uppercase",letterSpacing:1,marginBottom:6}}>Default Client Name</div>
                <input value={invoiceClient} onChange={e=>setInvoiceClient(e.target.value)} placeholder="Customer name" style={{...SEL,fontSize:14}}/>
              </div>
              <div>
                <div style={{fontSize:11,color:G.labelTert,fontWeight:600,textTransform:"uppercase",letterSpacing:1,marginBottom:6}}>Payment Terms</div>
                <div style={{position:"relative"}}>
                  <select value={invoiceDue} onChange={e=>setInvoiceDue(Number(e.target.value))} style={SEL}>
                    {[15,30,45,60].map(d=><option key={d} value={d}>Net {d} days</option>)}
                  </select>
                  <div style={{position:"absolute",right:12,top:"50%",transform:"translateY(-50%)",color:G.labelTert,pointerEvents:"none",fontSize:12}}>▾</div>
                </div>
              </div>
              <div style={{gridColumn:"1/-1"}}>
                <div style={{fontSize:11,color:G.labelTert,fontWeight:600,textTransform:"uppercase",letterSpacing:1,marginBottom:6}}>Invoice Notes / Terms</div>
                <textarea value={invoiceNotes} onChange={e=>setInvoiceNotes(e.target.value)} placeholder="e.g. 50% deposit required. Balance due on completion." style={{...SEL,height:80,resize:"vertical",lineHeight:1.6}}/>
              </div>
            </div>
          </div>
        </Section>}

        {/* ASK AI */}
        {tab==="ask"&&<Section title="Ask the AI Electrician">
          <p style={{fontSize:14,color:G.labelSec,marginTop:0,lineHeight:1.7}}>Ask any code or pricing question. Gets a specific NEC article reference every time.</p>
          <div style={{background:G.surfaceRaised,border:`1px solid ${G.sep}`,borderRadius:16,padding:16,minHeight:280,maxHeight:400,overflowY:"auto",marginBottom:12}}>
            {chatHistory.length===0&&(
              <div style={{color:G.labelTert,fontSize:14,textAlign:"center",marginTop:60,lineHeight:2}}>
                e.g. "Where is GFCI required?" or "What wire for a dryer circuit?"
              </div>
            )}
            {chatHistory.map((m,i)=>(
              <div key={i} style={{marginBottom:14,display:"flex",gap:10,justifyContent:m.role==="user"?"flex-end":"flex-start"}}>
                {m.role==="assistant"&&(
                  <div style={{width:30,height:30,background:G.amber,borderRadius:8,display:"flex",alignItems:"center",justifyContent:"center",fontSize:14,fontWeight:800,color:"#000",flexShrink:0}}>⚡</div>
                )}
                <div style={{
                  maxWidth:"82%",
                  background:m.role==="user"?G.amberDim:G.surface,
                  border:`1px solid ${m.role==="user"?G.amberGlow:G.sep}`,
                  borderRadius:14,padding:"11px 16px",
                  fontSize:14,color:m.role==="user"?G.label:G.labelSec,
                  lineHeight:1.7,
                }}>
                  {m.content}
                </div>
              </div>
            ))}
            {chatLoading&&<div style={{color:G.labelTert,fontSize:13,fontStyle:"italic"}}>⚡ Looking up code...</div>}
          </div>
          <div style={{display:"flex",gap:8}}>
            <input value={chatInput} onChange={e=>setChatInput(e.target.value)} onKeyDown={e=>e.key==="Enter"&&!e.shiftKey&&sendChat()} placeholder="Ask any NEC 2023 question..." style={{...SEL,flex:1,fontSize:14}}/>
            <button onClick={sendChat} disabled={!chatInput.trim()||chatLoading} style={{...PB,opacity:chatInput.trim()?1:0.4,flexShrink:0}}>Ask</button>
          </div>
        </Section>}

        {/* HISTORY */}
        {tab==="history"&&<Section title="Saved Estimates">
          {history.length===0?(
            <div style={{textAlign:"center",color:G.labelTert,fontSize:14,padding:48}}>No saved estimates yet. Generate one and hit Save.</div>
          ):history.map(est=>(
            <div key={est.id} style={{background:G.surfaceRaised,border:`1px solid ${G.sep}`,borderRadius:16,padding:18,marginBottom:10}}>
              <div style={{display:"flex",justifyContent:"space-between",alignItems:"flex-start",marginBottom:10}}>
                <div>
                  <div style={{fontSize:14,fontWeight:700,color:G.label,fontFamily:"monospace"}}>{est.id}</div>
                  <div style={{fontSize:12,color:G.labelTert,marginTop:2}}>{est.date} · {est.region}</div>
                </div>
                <div style={{fontSize:18,fontWeight:800,color:G.amber,fontFamily:"monospace",letterSpacing:-0.5}}>{est.mode==="tm"?fmt(est.tm):`${fmt(est.tLo)}–${fmt(est.tHi)}`}</div>
              </div>
              <div style={{fontSize:12,color:G.labelTert,marginBottom:14,lineHeight:1.7}}>{est.items.filter(i=>i.qty).map(i=>`${i.label}×${i.qty}`).join(" · ")}</div>
              <div style={{display:"flex",gap:8}}>
                <button onClick={()=>{setResult(est);setAiSum(est.summary||"");setTab("estimator");}} style={PB}>Load</button>
                <button onClick={()=>{const u=history.filter(e=>e.id!==est.id);setHistory(u);try{localStorage.setItem("vq_hist2",JSON.stringify(u));}catch{};}} style={{...GB,color:G.red,borderColor:G.red+"44"}}>Delete</button>
              </div>
            </div>
          ))}
        </Section>}

        {/* NEC */}
        {tab==="nec"&&<Section title="NEC 2023 — Complete Residential Reference">
          <NECReference/>
        </Section>}

        <div style={{textAlign:"center",marginTop:32,paddingTop:20,borderTop:`1px solid ${G.sep}`}}>
          <div style={{fontSize:11,color:G.labelTert}}>{T[lang].disclaimer}</div>
        </div>
      </div>

      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap');
        *{-webkit-tap-highlight-color:transparent;box-sizing:border-box;}
        input,select,textarea{-webkit-appearance:none;color-scheme:dark;}
        body{font-family:-apple-system,BlinkMacSystemFont,'SF Pro Display','SF Pro Text','Helvetica Neue',system-ui,sans-serif;background:#000000;}
        select option{background:#1c1c1e;color:#ffffff;}
        ::-webkit-scrollbar{width:0;height:0;}
        @keyframes fadeIn{from{opacity:0;transform:translateY(10px)}to{opacity:1;transform:translateY(0)}}
        ::selection{background:rgba(245,166,35,0.3);color:#ffffff;}
        input:focus,select:focus,textarea:focus{outline:none;border-color:rgba(245,166,35,0.5)!important;box-shadow:0 0 0 3px rgba(245,166,35,0.1)!important;}
        input::placeholder,textarea::placeholder{color:rgba(235,235,245,0.25);}
        @supports(-webkit-touch-callout:none){input,select,textarea{font-size:16px;}}
        @media print{button{display:none!important}}
        button:active{transform:scale(0.97);}
      `}</style>
    </div>
  );
}

function Section({title,children}){
  return(
    <div style={{marginBottom:28}}>
      <div style={{display:"flex",alignItems:"center",gap:12,marginBottom:16}}>
        <h3 style={{margin:0,fontSize:11,fontWeight:700,color:"rgba(235,235,245,0.3)",textTransform:"uppercase",letterSpacing:1.5,whiteSpace:"nowrap"}}>{title}</h3>
        <div style={{flex:1,height:"1px",background:"linear-gradient(90deg,rgba(255,255,255,0.08),transparent)"}}/>
      </div>
      {children}
    </div>
  );
}
