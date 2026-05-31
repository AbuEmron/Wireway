import React, { useState, useEffect, useRef } from "react";
import NECReference from './NECReference';

// ─── CONSTANTS ───────────────────────────────────────────────────────────────

// ─── REGION MULTIPLIERS (2026 verified vs national baseline) ─────────────────
// Base = national average. Multipliers derived from BLS wage data, HomeGuide,
// Angi, Workiz 2026 electrician cost reports by metro area.
// National baseline hourly: $75–$100/hr. NYC/SF: $150–$200/hr. Rural: $50–$75/hr.

const REGION_MULTIPLIERS = {
  // Northeast — highest labor costs in US
  "New York City, NY":2.15,"Albany, NY":1.38,"Binghamton, NY":1.0,
  "Buffalo, NY":1.18,"Rochester, NY":1.22,"Syracuse, NY":1.12,
  "Boston, MA":1.85,"Providence, RI":1.55,"Hartford, CT":1.6,
  "Newark, NJ":1.9,"Philadelphia, PA":1.58,"Pittsburgh, PA":1.28,
  "Baltimore, MD":1.48,"Washington DC":1.72,
  // Southeast — moderate costs
  "Atlanta, GA":1.28,"Charlotte, NC":1.18,"Raleigh, NC":1.22,
  "Greensboro, NC":1.1,"Jacksonville, FL":1.12,"Tampa, FL":1.22,
  "Orlando, FL":1.18,"Miami, FL":1.35,"Nashville, TN":1.25,
  "Memphis, TN":0.98,"Louisville, KY":1.02,"Richmond, VA":1.22,
  "Virginia Beach, VA":1.18,"New Orleans, LA":1.08,
  // Midwest — moderate to slightly below average
  "Chicago, IL":1.65,"Detroit, MI":1.25,"Minneapolis, MN":1.35,
  "Columbus, OH":1.12,"Cincinnati, OH":1.08,"Cleveland, OH":1.15,
  "Indianapolis, IN":1.08,"St. Louis, MO":1.12,"Kansas City, MO":1.12,
  "Milwaukee, WI":1.25,"Omaha, NE":1.0,"Des Moines, IA":1.02,
  // South Central — below average
  "Houston, TX":1.12,"Dallas, TX":1.18,"Austin, TX":1.28,
  "San Antonio, TX":1.08,"El Paso, TX":0.92,"Oklahoma City, OK":0.98,
  "Tulsa, OK":0.98,"Albuquerque, NM":1.02,"Tucson, AZ":1.02,
  // West — high costs
  "Los Angeles, CA":1.95,"San Francisco, CA":2.25,"San Diego, CA":1.85,
  "Sacramento, CA":1.65,"Phoenix, AZ":1.18,"Las Vegas, NV":1.25,
  "Denver, CO":1.45,"Salt Lake City, UT":1.22,"Portland, OR":1.55,
  "Seattle, WA":1.82,"Spokane, WA":1.22,"Boise, ID":1.15,
  // Alaska & Hawaii — highest costs nationwide
  "Anchorage, AK":1.85,"Honolulu, HI":2.1,
  // Rural
  "Rural/Small Town":0.88,
};

// ─── JOB CATEGORIES — 2026 VERIFIED PRICING ──────────────────────────────────
// Sources: HomeGuide May 2026, Homewyse Jan/May 2026, Angi Mar 2026,
// HomeAdvisor 2026, Fixr 2026, UseCalcPro Apr 2026
// Base prices = national average installed cost (labor + materials)
// low = simple replacement/retrofit | high = new circuit / finished walls
// mat = material cost for markup calculation | hours = estimated labor hours

const JOB_CATEGORIES = {
  "Wiring Devices": {
    receptacles:    {low:150,high:350,  label:"Receptacles (Outlets)",           unit:"each",   nec:"210.52",      mat:12,  hours:1.0,  note:"$150–$350 avg per Angi/HomeGuide May 2026. Low=swap, high=new circuit."},
    gfciOutlet:     {low:150,high:350,  label:"GFCI Outlets",                    unit:"each",   nec:"210.8",       mat:22,  hours:1.0,  note:"$120–$350 per HomeGuide 2026. Required in kitchens, baths, garages, outdoors."},
    afciOutlet:     {low:175,high:375,  label:"AFCI Outlets",                    unit:"each",   nec:"210.12",      mat:42,  hours:1.0,  note:"AFCI breaker adds $45–$75 per Angi 2026. Required in bedrooms and living areas."},
    switches:       {low:150,high:200,  label:"Single-Pole Switches",            unit:"each",   nec:"404.2",       mat:10,  hours:0.75, note:"$80–$200 per HomeGuide 2026. Low=replacement, high=new run."},
    threewaySwitch: {low:175,high:250,  label:"3-Way Switches",                  unit:"each",   nec:"404.2",       mat:18,  hours:1.25, note:"More complex wiring — requires 3-conductor cable between switches."},
    dimmers:        {low:175,high:260,  label:"Dimmer Switches",                 unit:"each",   nec:"404.14",      mat:30,  hours:1.0,  note:"Smart dimmer adds cost. Must be compatible with LED fixtures."},
    outdoorOutlet:  {low:180,high:350,  label:"Outdoor GFCI Outlets",            unit:"each",   nec:"210.8(A)(3)", mat:30,  hours:1.5,  note:"$180–$350 per HomeGuide 2026. In-use weatherproof cover required."},
    usbOutlet:      {low:175,high:310,  label:"USB Combo Outlets",               unit:"each",   nec:"210.52",      mat:35,  hours:1.0,  note:"USB-A/C combo outlets $25–$45 material. Same labor as standard outlet."},
    tamperResist:   {low:150,high:300,  label:"Tamper-Resistant Receptacles",    unit:"each",   nec:"406.12",      mat:15,  hours:0.75, note:"Required on ALL outlets in dwelling units per NEC 406.12."},
    evCharger:      {low:750,high:1800, label:"EV Charger Level 2 (240V)",       unit:"each",   nec:"625.40",      mat:220, hours:5.0,  note:"$750–$2,200 per HomeGuide 2026. Requires dedicated 40–50A 240V circuit."},
  },
  "Lighting": {
    snapLED:        {low:125,high:300,  label:"Snap-In LED Recessed Lights",     unit:"each",   nec:"410.116",     mat:28,  hours:1.0,  note:"$125–$300 per Angi 2026. IC-rated required where insulation present."},
    canLight:       {low:150,high:300,  label:"Traditional Can Lights",          unit:"each",   nec:"410.116",     mat:40,  hours:1.5,  note:"$125–$300/can per HomeGuide 2026. Fan-rated box needed for heavier fixtures."},
    wallLight:      {low:150,high:350,  label:"Wall Sconces / Light Fixtures",   unit:"each",   nec:"410.36",      mat:45,  hours:1.25, note:"$100–$350 labor per HomeGuide 2026. Heavy fixtures need fan-rated box."},
    ceilingFan:     {low:250,high:600,  label:"Ceiling Fans (with light)",       unit:"each",   nec:"314.27",      mat:85,  hours:2.0,  note:"$300–$900 total per Angi 2026 (incl. fan). Fan-rated box mandatory per NEC 314.27."},
    chandelierLight:{low:300,high:2000, label:"Chandelier / Heavy Fixture",      unit:"each",   nec:"314.27(D)",   mat:120, hours:3.0,  note:"$220–$3,400 total per HomeGuide 2026. Heavy fixtures need fan-rated medallion box."},
    undercabinet:   {low:150,high:350,  label:"Under-Cabinet Lighting",          unit:"each",   nec:"410.36",      mat:45,  hours:1.25, note:"LED strip or puck lights. Hardwired preferred over plug-in for clean install."},
    outdoorLight:   {low:150,high:350,  label:"Outdoor Light Fixtures",          unit:"each",   nec:"410.10",      mat:55,  hours:1.5,  note:"Wet-location rated fixtures required outdoors. GFCI protection needed nearby."},
    motionLight:    {low:175,high:400,  label:"Motion Sensor Lights",            unit:"each",   nec:"410.10",      mat:65,  hours:1.5,  note:"Outdoor security lights. Wet-rated, GFCI protected, aimed per code."},
    exitSign:       {low:250,high:500,  label:"Emergency Exit Signs",            unit:"each",   nec:"700.16",      mat:85,  hours:2.0,  note:"Battery backup required. Must illuminate whenever building is occupied."},
  },
  "Panels & Service": {
    panel100:       {low:1500,high:3000, label:"100A Panel Replacement",         unit:"flat",   nec:"230.79",      mat:450, hours:10,   note:"$1,500–$4,000 per HomeGuide 2026. Includes permit, labor, materials."},
    panel200:       {low:2000,high:4000, label:"200A Panel Replacement",         unit:"flat",   nec:"230.79",      mat:650, hours:12,   note:"$2,000–$5,000 per Angi 2026. Most common upgrade for modern homes."},
    panel400:       {low:4000,high:8000, label:"400A Panel Upgrade",             unit:"flat",   nec:"230.79",      mat:1400,hours:18,   note:"$4,000–$8,000+ installed. Required for large homes with EV, solar, or heavy loads."},
    subpanel100:    {low:1000,high:2500, label:"100A Subpanel Install",          unit:"flat",   nec:"225.30",      mat:380, hours:8,    note:"$1,000–$2,500 per HomeGuide 2026. Common for garages, additions, or workshops."},
    subpanel200:    {low:1500,high:3500, label:"200A Subpanel Install",          unit:"flat",   nec:"225.30",      mat:550, hours:10,   note:"$1,500–$3,500 installed. Separate neutral and ground bars required per NEC 250."},
    panelCircuit:   {low:200,high:500,   label:"New Branch Circuit at Panel",    unit:"each",   nec:"210.11",      mat:55,  hours:2.5,  note:"$200–$500 per circuit per Angi 2026. Includes breaker, wire run, and device."},
    meterBase:      {low:500,high:1200,  label:"Meter Base Replacement",         unit:"flat",   nec:"230.66",      mat:180, hours:5,    note:"Utility coordination required. Permit and inspection always needed."},
    groundRods:     {low:400,high:800,   label:"Grounding Electrode System",     unit:"flat",   nec:"250.50",      mat:90,  hours:4,    note:"Two ground rods minimum typically. GEC sized per NEC Table 250.66."},
    surgeProtector: {low:300,high:600,   label:"Whole-Home Surge Protector (SPD)",unit:"each",  nec:"230.67",      mat:130, hours:2,    note:"NOW REQUIRED by NEC 2023 §230.67 on all new services. Type 1 or 2 SPD."},
    exteriorDisconn:{low:400,high:900,   label:"Exterior Emergency Disconnect",  unit:"flat",   nec:"230.85",      mat:150, hours:3,    note:"NOW REQUIRED by NEC 2023 §230.85 on all new residential services."},
  },
  "Appliance Circuits": {
    dryer240:       {low:250,high:600,   label:"Dryer Circuit (240V / 30A)",     unit:"each",   nec:"210.11(C)(2)",mat:90,  hours:3,    note:"$250–$800 per HomeGuide 2026. 10 AWG / 30A. 4-wire connection required."},
    range240:       {low:300,high:700,   label:"Range/Oven Circuit (240V / 50A)",unit:"each",   nec:"210.19",      mat:90,  hours:3,    note:"$250–$800 per HomeGuide 2026. 6 AWG / 50A. 4-wire required."},
    acCircuit:      {low:250,high:600,   label:"A/C Dedicated Circuit",          unit:"each",   nec:"440.62",      mat:80,  hours:2.5,  note:"Sized per nameplate. Disconnect required within sight per NEC 440.62."},
    hotTub:         {low:1000,high:2500, label:"Hot Tub / Spa Circuit",          unit:"each",   nec:"680.42",      mat:280, hours:8,    note:"$1,000–$2,500 installed. GFCI required. 50A/240V typical."},
    pool:           {low:1500,high:4000, label:"Pool Electrical (bonding+circuit)",unit:"flat", nec:"680.26",      mat:450, hours:14,   note:"$1,500–$4,000 per HomeGuide 2026. Bonding all metal parts mandatory."},
    wellPump:       {low:500,high:1200,  label:"Well Pump Circuit",              unit:"each",   nec:"430.22",      mat:110, hours:4,    note:"Sized at 125% of motor FLA. Disconnect required near pump per NEC 430."},
    generator:      {low:2000,high:5000, label:"Generator + Transfer Switch",    unit:"flat",   nec:"702.12",      mat:900, hours:14,   note:"$2,000–$5,000 installed per Angi 2026. Transfer switch mandatory — prevents backfeed."},
    solarTie:       {low:1000,high:3000, label:"Solar PV Interconnect",          unit:"flat",   nec:"705.12",      mat:350, hours:10,   note:"120% rule applies to panel bus. Rapid shutdown required per NEC 690."},
    batteryBackup:  {low:3000,high:8000, label:"Battery Backup System",          unit:"flat",   nec:"702.4",       mat:1500,hours:16,   note:"$3,000–$8,000+ installed. Permitting and utility notification typically required."},
  },
  "Safety Devices": {
    smokeDetector:  {low:100,high:200,   label:"Smoke Detectors (hardwired)",    unit:"each",   nec:"760.32",      mat:28,  hours:1.0,  note:"$70–$150 labor per Angi 2026. Interconnected hardwired required in new construction."},
    coDetector:     {low:100,high:200,   label:"CO Detectors (hardwired)",       unit:"each",   nec:"760.32",      mat:32,  hours:1.0,  note:"Required near sleeping areas. Battery backup required per most local codes."},
    comboDet:       {low:120,high:220,   label:"Combo Smoke/CO Detectors",       unit:"each",   nec:"760.32",      mat:45,  hours:1.0,  note:"Most efficient option. Replaces both devices with one listed combination unit."},
    afciBreaker:    {low:120,high:200,   label:"AFCI Breakers",                  unit:"each",   nec:"210.12",      mat:50,  hours:1.0,  note:"$45–$75 each per Angi 2026. Required in all living areas per NEC 210.12."},
    gfciBreaker:    {low:120,high:200,   label:"GFCI Breakers",                  unit:"each",   nec:"210.8",       mat:50,  hours:1.0,  note:"Protects entire circuit. Alternative to GFCI outlets in wet locations."},
    tamperGFCI:     {low:150,high:300,   label:"Tamper-Resistant GFCI Outlets",  unit:"each",   nec:"406.12",      mat:25,  hours:1.0,  note:"Dual protection — both tamper-resistant shutters AND GFCI. Best practice for kitchens."},
  },
  "Wiring & Rough-In": {
    rewireRoom:     {low:500,high:1200,  label:"Rewire Single Room",             unit:"each",   nec:"310.12",      mat:180, hours:6,    note:"$500–$1,500 per room. Includes outlets, switches, lights on new circuits."},
    rewireHome:     {low:10000,high:30000,label:"Full Home Rewire",              unit:"flat",   nec:"310.12",      mat:4000,hours:100,  note:"$10,000–$30,000 per HomeGuide 2026. Required for knob-and-tube or heavily damaged wiring."},
    aluminumFix:    {low:200,high:400,   label:"Aluminum Wiring Fix (per outlet)",unit:"each",  nec:"110.14",      mat:35,  hours:2.0,  note:"COPALUM crimp or AlumiConn connector required. Cannot use standard wire nuts."},
    lowVoltage:     {low:100,high:200,   label:"Low Voltage (data/cable/phone)", unit:"each",   nec:"800.24",      mat:22,  hours:1.0,  note:"Category 6 data, coax cable, or phone line. 2-inch separation from power wiring."},
    conduitRun:     {low:8,  high:15,    label:"Conduit Run (per linear foot)",  unit:"lin ft", nec:"358.10",      mat:5,   hours:0.1,  note:"EMT $1.80–$4.50/ft labor, RMC $6–$12/ft per 2026 BhumiCalculator data."},
    wireRun:        {low:4,  high:10,    label:"Wire Pull (per linear foot)",    unit:"lin ft", nec:"310.15",      mat:1.5, hours:0.04, note:"$4–$8/ft for standard residential wire runs per HomeGuide 2026."},
    junctionBox:    {low:100,high:250,   label:"Junction Box (installed)",       unit:"each",   nec:"314.29",      mat:10,  hours:1.0,  note:"All splices must be in accessible boxes with covers per NEC 300.15."},
  },
  "Outdoor & Specialty": {
    outdoorPanel:   {low:800,high:2000,  label:"Outdoor Subpanel",               unit:"flat",   nec:"225.30",      mat:250, hours:8,    note:"NEMA 3R rated enclosure required. Single feeder to structure per NEC 225.30."},
    landscape:      {low:400,high:1200,  label:"Landscape Lighting System",      unit:"flat",   nec:"411.3",       mat:180, hours:6,    note:"Low voltage systems popular. Line voltage requires GFCI protection outdoors."},
    shed:           {low:800,high:2000,  label:"Shed / Detached Garage Electric",unit:"flat",   nec:"225.30",      mat:250, hours:8,    note:"Grounding electrode required at detached structure. Disconnect required at building."},
    securityCamera: {low:150,high:300,   label:"Security Camera Power",          unit:"each",   nec:"210.52",      mat:25,  hours:1.25, note:"PoE cameras can share data circuit. Hardwired power provides best reliability."},
    doorbell:       {low:150,high:350,   label:"Doorbell / Video Doorbell",      unit:"each",   nec:"725.3",       mat:30,  hours:1.5,  note:"Transformer typically 16V/30VA. Most smart doorbells require 16–24VAC."},
    poolLight:      {low:400,high:1000,  label:"Pool / Spa Light",               unit:"each",   nec:"680.23",      mat:200, hours:4,    note:"Must be low voltage (12V) or listed for wet locations. GFCI required per NEC 680."},
  },
};


const US_STATES = ["AL","AK","AZ","AR","CA","CO","CT","DE","FL","GA","HI","ID","IL","IN","IA","KS","KY","LA","ME","MD","MA","MI","MN","MS","MO","MT","NE","NV","NH","NJ","NM","NY","NC","ND","OH","OK","OR","PA","RI","SC","SD","TN","TX","UT","VT","VA","WA","WV","WI","WY"];

const SQFT_PRESETS = {
  "Small (< 1000 sq ft)":   { receptacles:10, switches:6, snapLED:6,  smokeDetector:2, gfciOutlet:4, panelCircuit:2 },
  "Medium (1000–2000 sq ft)":{ receptacles:18, switches:10,snapLED:10, smokeDetector:3, gfciOutlet:6, panelCircuit:3 },
  "Large (2000–3500 sq ft)": { receptacles:28, switches:16,snapLED:16, smokeDetector:5, gfciOutlet:8, panelCircuit:5 },
  "XL (3500+ sq ft)":        { receptacles:40, switches:22,snapLED:22, smokeDetector:7, gfciOutlet:10,panelCircuit:7 },
};

const TRANSLATIONS = {
  en: {
    appTagline:"Residential Electrical Estimator",
    estimator:"Estimate", photo:"Photo", contractor:"My Info", ask:"Ask AI",
    nec:"NEC 2023", history:"History", overhead:"Overhead", landing:"Home",
    region:"Location & Pricing Mode", scopeTitle:"Scope of Work",
    conditions:"Job Conditions", pricingOpts:"Pricing Options",
    generateBtn:"Generate Estimate", selectItems:"Select items above to estimate",
    totalEst:"Total Estimate", laborHours:"labor hours", estReady:"Estimate Ready",
    customerView:"Customer View", copyQuote:"Copy Quote", copied:"Copied!",
    profSummary:"Professional Summary", generating:"Generating summary...",
    disclaimer:"Estimates based on regional averages · Final pricing varies · Always pull permits",
    flatRate:"Flat Rate", timeAndMat:"Time & Material", hourlyRate:"Hourly rate",
    matMarkup:"Materials markup", includePermit:"Include permit fee",
    includeMat:"Include materials", sqftTitle:"Quick Estimate by Square Footage",
    sqftDesc:"Select home size to auto-populate a typical rough-in scope.",
    overheadTitle:"Overhead & True Cost Calculator",
    overheadDesc:"Enter your monthly business costs to calculate your real break-even rate.",
    historyTitle:"Saved Estimates", historyEmpty:"No saved estimates yet.",
    saveEst:"Save Estimate", savedOk:"Saved!", invoiceTitle:"Generate Invoice",
    invoiceBtn:"Create Invoice", pdfBtn:"Export PDF",
    shareBtn:"Share VoltQuote", shareMsg:"Share VoltQuote with another electrician and both get 5 free estimates!",
    langToggle:"Español",
  },
  es: {
    appTagline:"Estimador Eléctrico Residencial",
    estimator:"Estimar", photo:"Foto", contractor:"Mi Info", ask:"Preguntar AI",
    nec:"NEC 2023", history:"Historial", overhead:"Gastos", landing:"Inicio",
    region:"Ubicación y Modo de Precio", scopeTitle:"Alcance del Trabajo",
    conditions:"Condiciones del Trabajo", pricingOpts:"Opciones de Precio",
    generateBtn:"Generar Estimado", selectItems:"Selecciona elementos arriba",
    totalEst:"Total del Estimado", laborHours:"horas de trabajo", estReady:"Estimado Listo",
    customerView:"Vista del Cliente", copyQuote:"Copiar Cotización", copied:"¡Copiado!",
    profSummary:"Resumen Profesional", generating:"Generando resumen...",
    disclaimer:"Estimados basados en promedios regionales · El precio final varía · Siempre saque permisos",
    flatRate:"Tarifa Fija", timeAndMat:"Tiempo y Material", hourlyRate:"Tarifa por hora",
    matMarkup:"Margen de materiales", includePermit:"Incluir tarifa de permiso",
    includeMat:"Incluir materiales", sqftTitle:"Estimado Rápido por Pies Cuadrados",
    sqftDesc:"Seleccione el tamaño del hogar para llenar el alcance típico.",
    overheadTitle:"Calculadora de Gastos Generales",
    overheadDesc:"Ingrese sus costos mensuales para calcular su tarifa de equilibrio real.",
    historyTitle:"Estimados Guardados", historyEmpty:"No hay estimados guardados.",
    saveEst:"Guardar Estimado", savedOk:"¡Guardado!", invoiceTitle:"Generar Factura",
    invoiceBtn:"Crear Factura", pdfBtn:"Exportar PDF",
    shareBtn:"Compartir VoltQuote", shareMsg:"¡Comparte VoltQuote con otro electricista y ambos obtienen 5 estimados gratis!",
    langToggle:"English",
  }
};

const CONDITION_ADJUSTMENTS = {
  openWalls:{label:"Customer opens walls",labelEs:"Cliente abre paredes",multiplier:0.85,icon:"🪚",color:"#50c878"},
  finishedWalls:{label:"Finished walls (fish wire)",labelEs:"Paredes terminadas",multiplier:1.2,icon:"🏠",color:"#e08a55"},
  oldWiring:{label:"Old / knob-and-tube wiring",labelEs:"Cableado antiguo",multiplier:1.3,icon:"⚠️",color:"#e05555"},
  newConstruction:{label:"New construction rough-in",labelEs:"Construcción nueva",multiplier:0.88,icon:"🏗️",color:"#50c878"},
  atticAccess:{label:"Attic/crawl space access",labelEs:"Acceso al ático",multiplier:0.92,icon:"🔦",color:"#50c878"},
  highCeilings:{label:"High ceilings (10ft+)",labelEs:"Techos altos (3m+)",multiplier:1.15,icon:"📏",color:"#e08a55"},
  hazmat:{label:"Asbestos / hazmat present",labelEs:"Asbesto / materiales peligrosos",multiplier:1.4,icon:"☣️",color:"#e05555"},
};

// NEC references moved to NECReference.js component

// ─── HELPERS ─────────────────────────────────────────────────────────────────

const fmt = (n) => "$" + n.toLocaleString();
const fmtRange = (lo, hi) => lo === hi ? fmt(lo) : `${fmt(lo)}–${fmt(hi)}`;
const today = () => new Date().toLocaleDateString("en-US",{year:"numeric",month:"long",day:"numeric"});
const invNum = () => "VQ-" + Date.now().toString().slice(-6);

// ─── MAIN APP ─────────────────────────────────────────────────────────────────

export default function ElectricalEstimator() {
  const [lang, setLang] = useState("en");
  const T = TRANSLATIONS[lang];
  const [view, setView] = useState("landing");
  const [activeTab, setActiveTab] = useState("estimator");
  const [pricingMode, setPricingMode] = useState("flat");
  const [quantities, setQuantities] = useState({});
  const [conditions, setConditions] = useState({});
  const [includeMaterials, setIncludeMaterials] = useState(true);
  const [includePermit, setIncludePermit] = useState(true);
  const [markupPct, setMarkupPct] = useState(20);
  const [hourlyRate, setHourlyRate] = useState(85);
  const [region, setRegion] = useState("Binghamton, NY");
  const [result, setResult] = useState(null);
  const [copied, setCopied] = useState(false);
  const [saved, setSaved] = useState(false);
  const [loading, setLoading] = useState(false);
  const [aiSummary, setAiSummary] = useState("");
  const [necSearch, setNecSearch] = useState(""); // eslint-disable-line no-unused-vars
  const [necCategory, setNecCategory] = useState("All"); // eslint-disable-line no-unused-vars
  const [expandedCats, setExpandedCats] = useState({"Wiring Devices":true});
  const [photoAnalysis, setPhotoAnalysis] = useState("");
  const [photoLoading, setPhotoLoading] = useState(false);
  const [aiChat, setAiChat] = useState([]);
  const [chatInput, setChatInput] = useState("");
  const [chatLoading, setChatLoading] = useState(false);
  const [showCustomer, setShowCustomer] = useState(false);
  const [showInvoice, setShowInvoice] = useState(false);
  const [savedEstimates, setSavedEstimates] = useState([]);
  const [showShare, setShowShare] = useState(false);
  const [referralCount, setReferralCount] = useState(0);
  // Contractor info
  const [contractorName, setContractorName] = useState("");
  const [contractorPhone, setContractorPhone] = useState("");
  const [contractorEmail, setContractorEmail] = useState("");
  const [contractorLicense, setContractorLicense] = useState("");
  const [contractorCity, setContractorCity] = useState("");
  const [contractorState, setContractorState] = useState("NY");
  // Overhead
  const [overhead, setOverhead] = useState({insurance:300,vehicle:400,tools:100,phone:80,misc:200});
  const [targetHours, setTargetHours] = useState(120);
  const [desiredProfit, setDesiredProfit] = useState(25);
  // Invoice
  const [invoiceClient, setInvoiceClient] = useState("");
  const [invoiceAddress, setInvoiceAddress] = useState(""); // eslint-disable-line no-unused-vars
  const [invoiceNotes, setInvoiceNotes] = useState("");
  const [invoiceDue, setInvoiceDue] = useState(30);

  const fileRef = useRef(null);
  const regionMultiplier = REGION_MULTIPLIERS[region] || 1.0;

  // Load saved estimates from storage
  useEffect(() => {
    try {
      const stored = localStorage.getItem("vq_estimates");
      if (stored) setSavedEstimates(JSON.parse(stored));
      const rc = localStorage.getItem("vq_referrals");
      if (rc) setReferralCount(parseInt(rc)||0);
    } catch {}
  }, []);

  const getCondMult = () => {
    let m = 1.0;
    Object.entries(conditions).forEach(([k,v]) => { if(v) m *= CONDITION_ADJUSTMENTS[k].multiplier; });
    return m;
  };

  const totalOverhead = Object.values(overhead).reduce((a,b)=>a+(Number(b)||0),0);
  const trueHourlyRate = targetHours > 0
    ? Math.ceil((totalOverhead / targetHours) * (1 + desiredProfit/100))
    : 0;

  const calculateEstimate = () => {
    let totalLow=0, totalHigh=0, totalMat=0, totalHours=0;
    const lineItems = [];
    const condMult = getCondMult();
    Object.entries(JOB_CATEGORIES).forEach(([cat,items]) => {
      Object.entries(items).forEach(([key,item]) => {
        const qty = quantities[key];
        if (!qty || qty <= 0) return;
        const low = Math.round(item.low * qty * regionMultiplier * condMult);
        const high = Math.round(item.high * qty * regionMultiplier * condMult);
        const mat = Math.round((item.mat||0) * qty);
        const hrs = Math.round((item.hours||0) * qty * 10)/10;
        totalLow+=low; totalHigh+=high; totalMat+=mat; totalHours+=hrs;
        lineItems.push({label:item.label,qty,low,high,mat,hrs,nec:item.nec,cat,unit:item.unit});
      });
    });
    let tmTotal = 0;
    if (pricingMode==="tm") tmTotal = Math.round(totalHours * hourlyRate + totalMat*(1+markupPct/100));
    if (includeMaterials && pricingMode==="flat") {
      const mLow = Math.round(totalMat*(markupPct/100));
      const mHigh = Math.round(totalMat*(markupPct/100)*1.1);
      totalLow+=mLow; totalHigh+=mHigh;
      lineItems.push({label:"Materials Markup ("+markupPct+"%)",qty:null,low:mLow,high:mHigh,mat:0,hrs:0,nec:null});
    }
    if (includePermit) {
      const fee = contractorState==="NY"||region.includes("NY")?150:100;
      totalLow+=fee; totalHigh+=fee; if(pricingMode==="tm") tmTotal+=fee;
      lineItems.push({label:"Permit Fee (est.)",qty:null,low:fee,high:fee,mat:0,hrs:0,nec:null});
    }
    const r = {lineItems,totalLow,totalHigh,totalMat,totalHours,tmTotal,region,pricingMode,date:today(),id:invNum()};
    setResult(r);
    fetchAiSummary(lineItems,totalLow,totalHigh,region,totalHours,pricingMode,tmTotal);
  };

  const fetchAiSummary = async (items,low,high,rgn,hrs,mode,tm) => {
    setLoading(true); setAiSummary("");
    const scope = items.filter(i=>i.qty).map(i=>`${i.label} x${i.qty}`).join(", ");
    const priceStr = mode==="tm"?fmt(tm):`${fmt(low)}–${fmt(high)}`;
    try {
      const r = await fetch("/api/claude",{
        method:"POST",headers:{"Content-Type":"application/json"},
        body:JSON.stringify({model:"claude-sonnet-4-20250514",max_tokens:300,
          messages:[{role:"user",content:`You are a professional electrical contractor. Write a warm, confident 3-sentence quote summary for a residential customer in ${rgn}. Scope: ${scope}. Total: ${priceStr}. Est. labor: ${hrs} hours. Mention NEC 2023 code compliance and that final price depends on site conditions. No bullets.`}]})
      });
      const d = await r.json();
      setAiSummary(d.content?.map(b=>b.text||"").join("")||"");
    } catch { setAiSummary("Estimate complete. Contact us for a detailed on-site assessment."); }
    setLoading(false);
  };

  const saveEstimate = () => {
    if (!result) return;
    const est = {...result, summary:aiSummary, contractor:contractorName, clientName:invoiceClient};
    const updated = [est, ...savedEstimates].slice(0,20);
    setSavedEstimates(updated);
    try { localStorage.setItem("vq_estimates", JSON.stringify(updated)); } catch {}
    setSaved(true); setTimeout(()=>setSaved(false),2000);
  };

  const loadEstimate = (est) => {
    setResult(est);
    setAiSummary(est.summary||"");
    setActiveTab("estimator");
  };

  const deleteEstimate = (id) => {
    const updated = savedEstimates.filter(e=>e.id!==id);
    setSavedEstimates(updated);
    try { localStorage.setItem("vq_estimates", JSON.stringify(updated)); } catch {}
  };

  const copyQuote = () => {
    if (!result) return;
    const co = contractorName?`\n${contractorName}${contractorPhone?" · "+contractorPhone:""}${contractorLicense?" · Lic#"+contractorLicense:""}`:""
    const lines = result.lineItems.map(i=>`  ${i.label}${i.qty?` (×${i.qty})`:""}: ${fmtRange(i.low,i.high)}`).join("\n");
    const total = result.pricingMode==="tm"?fmt(result.tmTotal):`${fmt(result.totalLow)}–${fmt(result.totalHigh)}`;
    navigator.clipboard.writeText(`VOLTQUOTE ESTIMATE${co}\nRegion: ${result.region} · ${result.date}\n${"─".repeat(44)}\n${lines}\n${"─".repeat(44)}\nTOTAL: ${total}\nEst. Labor: ~${result.totalHours} hours\n\n${aiSummary}\n\nValid 30 days. Final price subject to on-site inspection. All work to NEC 2023.`);
    setCopied(true); setTimeout(()=>setCopied(false),2000);
  };

  const handlePhotoUpload = async (e) => {
    const file = e.target.files[0]; if (!file) return;
    setPhotoLoading(true); setPhotoAnalysis("");
    const b64 = await new Promise((res,rej)=>{const r=new FileReader();r.onload=()=>res(r.result.split(",")[1]);r.onerror=()=>rej();r.readAsDataURL(file);});
    try {
      const r = await fetch("/api/claude",{
        method:"POST",headers:{"Content-Type":"application/json"},
        body:JSON.stringify({model:"claude-sonnet-4-20250514",max_tokens:500,
          messages:[{role:"user",content:[
            {type:"image",source:{type:"base64",media_type:file.type,data:b64}},
            {type:"text",text:"You are an experienced residential electrician. Analyze this photo. Identify visible electrical items and what work may be needed: receptacles, switches, panels, lights, fans, smoke detectors, GFCI needs, code violations, etc. Give a short paragraph then a bulleted list of suggested estimate items. Be specific and practical."}
          ]}]})
      });
      const d = await r.json();
      setPhotoAnalysis(d.content?.map(b=>b.text||"").join("")||"");
    } catch { setPhotoAnalysis("Could not analyze photo. Please try again."); }
    setPhotoLoading(false);
  };

  const sendChat = async () => {
    if (!chatInput.trim()) return;
    const userMsg = {role:"user",content:chatInput};
    const history = [...aiChat,userMsg];
    setAiChat(history); setChatInput(""); setChatLoading(true);
    const ctx = result?`Current estimate: ${fmt(result.totalLow)}–${fmt(result.totalHigh)} in ${result.region}. Items: ${result.lineItems.filter(i=>i.qty).map(i=>`${i.label}×${i.qty}`).join(", ")}.`:"";
    try {
      const r = await fetch("/api/claude",{
        method:"POST",headers:{"Content-Type":"application/json"},
        body:JSON.stringify({model:"claude-sonnet-4-20250514",max_tokens:400,
          system:`You are an expert residential electrician and contractor with 20+ years experience. Help with electrical questions, code, pricing, and scope. Be concise and practical. ${ctx}`,
          messages:history})
      });
      const d = await r.json();
      setAiChat([...history,{role:"assistant",content:d.content?.map(b=>b.text||"").join("")||""}]);
    } catch { setAiChat([...history,{role:"assistant",content:"Sorry, couldn't respond. Please try again."}]); }
    setChatLoading(false);
  };

  const handleShare = () => {
    const newCount = referralCount + 1;
    setReferralCount(newCount);
    try { localStorage.setItem("vq_referrals", String(newCount)); } catch {}
    if (navigator.share) {
      navigator.share({title:"VoltQuote",text:"Free residential electrical estimator — fast, accurate, built for contractors.",url:"https://voltquote.app"});
    } else {
      navigator.clipboard.writeText("Check out VoltQuote — free electrical estimating tool for contractors: https://voltquote.app");
      setShowShare(true); setTimeout(()=>setShowShare(false),3000);
    }
  };

  const applySquareFootage = (preset) => {
    setQuantities(q=>({...q,...preset}));
    setActiveTab("estimator");
    setExpandedCats({"Wiring Devices":true,"Lighting":true,"Safety Devices":true});
  };

  const hasItems = Object.values(quantities).some(v=>v>0);
  const totalItems = Object.values(quantities).reduce((a,b)=>a+(b||0),0);

  // ── LANDING PAGE ──────────────────────────────────────────────────────────
  if (view==="landing") return (
    <div style={{minHeight:"100vh",background:"#0a0a0f",color:"#e8e0d0",fontFamily:"Georgia,serif"}}>
      <div style={{position:"fixed",inset:0,background:"radial-gradient(ellipse at 20% 40%,#1a1a2e 0%,transparent 55%),radial-gradient(ellipse at 80% 10%,#16213e 0%,transparent 50%)",pointerEvents:"none"}}/>
      <div style={{position:"relative",zIndex:1}}>
        {/* Nav */}
        <div style={{display:"flex",justifyContent:"space-between",alignItems:"center",padding:"20px 32px",borderBottom:"1px solid rgba(245,166,35,0.15)"}}>
          <div style={{display:"flex",alignItems:"center",gap:10}}>
            <div style={{width:32,height:32,borderRadius:"50%",background:"linear-gradient(135deg,#f5a623,#e8860a)",display:"flex",alignItems:"center",justifyContent:"center",fontSize:16,boxShadow:"0 0 20px rgba(245,166,35,0.4)"}}>⚡</div>
            <span style={{fontSize:18,fontWeight:700}}><span style={{color:"#f5a623"}}>VOLT</span><span style={{color:"rgba(245,166,35,0.4)",margin:"0 3px"}}>●</span><span style={{color:"#fff",fontWeight:400}}>QUOTE</span></span>
          </div>
          <div style={{display:"flex",gap:12,alignItems:"center"}}>
            <button onClick={()=>setLang(lang==="en"?"es":"en")} style={{...ghostBtn,fontSize:11}}>{T.langToggle}</button>
            <button onClick={()=>setView("app")} style={{background:"linear-gradient(135deg,#f5a623,#e8860a)",border:"none",borderRadius:8,padding:"10px 22px",color:"#0a0a0f",fontWeight:700,fontSize:13,cursor:"pointer",fontFamily:"monospace",letterSpacing:1}}>Try Free →</button>
          </div>
        </div>
        {/* Hero */}
        <div style={{maxWidth:700,margin:"0 auto",textAlign:"center",padding:"80px 24px 60px"}}>
          <div style={{fontSize:11,letterSpacing:5,color:"#f5a623",fontFamily:"monospace",textTransform:"uppercase",marginBottom:20}}>Built for the trades</div>
          <h1 style={{fontSize:"clamp(32px,6vw,58px)",fontWeight:700,lineHeight:1.15,margin:"0 0 24px",color:"#fff"}}>
            Professional electrical estimates<br/><span style={{color:"#f5a623"}}>in under 5 minutes.</span>
          </h1>
          <p style={{fontSize:16,color:"#8a8070",lineHeight:1.8,maxWidth:520,margin:"0 auto 40px"}}>
            Location-adjusted pricing for 55+ US cities. NEC 2023 code reference built in. AI-powered quote summaries. Made for residential electricians who work from the truck.
          </p>
          <div style={{display:"flex",gap:14,justifyContent:"center",flexWrap:"wrap"}}>
            <button onClick={()=>setView("app")} style={{background:"linear-gradient(135deg,#f5a623,#e8860a)",border:"none",borderRadius:10,padding:"16px 36px",color:"#0a0a0f",fontWeight:700,fontSize:15,cursor:"pointer",boxShadow:"0 4px 30px rgba(245,166,35,0.4)"}}>⚡ Start Free Estimate</button>
            <button onClick={()=>{setView("app");setActiveTab("nec");}} style={{...ghostBtn,padding:"16px 28px",fontSize:14}}>📖 Browse NEC 2023</button>
          </div>
          <p style={{fontSize:11,color:"#3a3040",fontFamily:"monospace",marginTop:20,letterSpacing:1}}>FREE · NO SIGNUP · WORKS OFFLINE</p>
        </div>
        {/* Features grid */}
        <div style={{maxWidth:900,margin:"0 auto",padding:"0 24px 80px",display:"grid",gridTemplateColumns:"repeat(auto-fit,minmax(240px,1fr))",gap:20}}>
          {[
            {icon:"📍",title:"Location-Based Pricing",desc:"Rates auto-adjust for 55+ cities across every US state. Binghamton to San Francisco."},
            {icon:"⚡",title:"60+ Line Items",desc:"Every residential job type — panels, rewires, EV chargers, pools, generators and more."},
            {icon:"📷",title:"Photo Analysis",desc:"Upload a room photo. AI identifies what electrical work is needed and suggests items."},
            {icon:"📖",title:"NEC 2023 Built In",desc:"22 key residential code articles with plain-English summaries. Always at your fingertips."},
            {icon:"💬",title:"AI Electrician Chat",desc:"Ask code questions, get pricing advice, clarify scope. Context-aware for your estimate."},
            {icon:"👤",title:"Customer View & Invoice",desc:"Professional quote the customer sees. One-click invoice. Copy or export instantly."},
            {icon:"📊",title:"Overhead Calculator",desc:"Know your real break-even rate. Enter your monthly costs and let VoltQuote do the math."},
            {icon:"🌐",title:"English & Español",desc:"Full bilingual support. Switch languages in one tap — built for the whole workforce."},
          ].map((f,i)=>(
            <div key={i} style={{background:"rgba(255,255,255,0.02)",border:"1px solid rgba(245,166,35,0.12)",borderRadius:12,padding:22}}>
              <div style={{fontSize:28,marginBottom:10}}>{f.icon}</div>
              <div style={{fontSize:14,fontWeight:700,color:"#fff",marginBottom:6}}>{f.title}</div>
              <div style={{fontSize:12,color:"#6a6055",lineHeight:1.7}}>{f.desc}</div>
            </div>
          ))}
        </div>
        {/* Pricing */}
        <div style={{maxWidth:700,margin:"0 auto",padding:"0 24px 80px",textAlign:"center"}}>
          <div style={{fontSize:11,letterSpacing:4,color:"#f5a623",fontFamily:"monospace",textTransform:"uppercase",marginBottom:16}}>Simple pricing</div>
          <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:16,maxWidth:500,margin:"0 auto"}}>
            {[
              {name:"Free",price:"$0",desc:"5 estimates/month\nAll core features\nNEC 2023 reference"},
              {name:"Pro",price:"$9.99/mo",desc:"Unlimited estimates\nSaved history\nInvoice generator\nPriority support",highlight:true},
            ].map(p=>(
              <div key={p.name} style={{background:p.highlight?"rgba(245,166,35,0.08)":"rgba(255,255,255,0.02)",border:`1px solid ${p.highlight?"rgba(245,166,35,0.4)":"rgba(255,255,255,0.08)"}`,borderRadius:12,padding:24}}>
                <div style={{fontSize:14,fontWeight:700,color:p.highlight?"#f5a623":"#fff",marginBottom:8}}>{p.name}</div>
                <div style={{fontSize:26,fontWeight:700,color:"#fff",marginBottom:12}}>{p.price}</div>
                <div style={{fontSize:11,color:"#6a6055",lineHeight:2,whiteSpace:"pre-line"}}>{p.desc}</div>
              </div>
            ))}
          </div>
        </div>
        {/* CTA */}
        <div style={{textAlign:"center",padding:"0 24px 80px"}}>
          <button onClick={()=>setView("app")} style={{background:"linear-gradient(135deg,#f5a623,#e8860a)",border:"none",borderRadius:10,padding:"18px 48px",color:"#0a0a0f",fontWeight:700,fontSize:16,cursor:"pointer",boxShadow:"0 4px 30px rgba(245,166,35,0.35)"}}>
            ⚡ Start Your First Estimate — Free
          </button>
          <p style={{fontSize:11,color:"#3a3040",fontFamily:"monospace",marginTop:16,letterSpacing:1}}>NO CREDIT CARD · NO SIGNUP · INSTANT ACCESS</p>
        </div>
        <div style={{textAlign:"center",padding:"20px",borderTop:"1px solid rgba(255,255,255,0.04)"}}>
          <p style={{fontSize:10,color:"#2a2030",fontFamily:"monospace",margin:0}}>VoltQuote · The electrician's estimating tool · voltquote.app</p>
        </div>
      </div>
    </div>
  );

  // ── INVOICE VIEW ──────────────────────────────────────────────────────────
  if (showInvoice && result) return (
    <div style={{minHeight:"100vh",background:"#f4f1ea",fontFamily:"Georgia,serif",color:"#1a1a1a",padding:"32px 16px"}}>
      <div style={{maxWidth:640,margin:"0 auto"}}>
        <div style={{background:"#1a1a2e",color:"white",borderRadius:"12px 12px 0 0",padding:"28px 32px",display:"flex",justifyContent:"space-between",alignItems:"center"}}>
          <div>
            <div style={{fontSize:10,letterSpacing:4,color:"#f5a623",fontFamily:"monospace",marginBottom:4}}>INVOICE</div>
            <div style={{fontSize:20,fontWeight:700}}>{contractorName||"VoltQuote Contractor"}</div>
            {contractorPhone&&<div style={{fontSize:12,color:"#c0b89a",marginTop:2}}>📞 {contractorPhone}</div>}
            {contractorEmail&&<div style={{fontSize:12,color:"#c0b89a"}}>✉️ {contractorEmail}</div>}
            {contractorLicense&&<div style={{fontSize:11,color:"#f5a623",marginTop:4}}>License #{contractorLicense}</div>}
          </div>
          <div style={{textAlign:"right"}}>
            <div style={{fontSize:10,color:"#8a7a60",fontFamily:"monospace"}}>INVOICE #</div>
            <div style={{fontSize:15,fontWeight:700,color:"#f5a623",fontFamily:"monospace"}}>{result.id}</div>
            <div style={{fontSize:10,color:"#8a7a60",fontFamily:"monospace",marginTop:6}}>DATE</div>
            <div style={{fontSize:12,color:"white"}}>{result.date}</div>
            <div style={{fontSize:10,color:"#8a7a60",fontFamily:"monospace",marginTop:4}}>DUE</div>
            <div style={{fontSize:12,color:"white"}}>Net {invoiceDue} days</div>
          </div>
        </div>
        <div style={{background:"white",border:"1px solid #e0d8c8",borderTop:"none",borderRadius:"0 0 12px 12px",padding:"28px 32px"}}>
          {invoiceClient&&<div style={{marginBottom:20,paddingBottom:16,borderBottom:"1px solid #f0ebe0"}}>
            <div style={{fontSize:10,color:"#8a8070",fontFamily:"monospace",letterSpacing:2,marginBottom:4}}>BILL TO</div>
            <div style={{fontSize:14,fontWeight:600}}>{invoiceClient}</div>
            {invoiceAddress&&<div style={{fontSize:12,color:"#6a6055"}}>{invoiceAddress}</div>}
          </div>}
          <table style={{width:"100%",borderCollapse:"collapse",marginBottom:20}}>
            <thead>
              <tr style={{borderBottom:"2px solid #1a1a2e"}}>
                <th style={{textAlign:"left",fontSize:10,fontFamily:"monospace",letterSpacing:2,color:"#8a8070",padding:"0 0 8px",fontWeight:400}}>DESCRIPTION</th>
                <th style={{textAlign:"center",fontSize:10,fontFamily:"monospace",letterSpacing:2,color:"#8a8070",padding:"0 0 8px",fontWeight:400}}>QTY</th>
                <th style={{textAlign:"right",fontSize:10,fontFamily:"monospace",letterSpacing:2,color:"#8a8070",padding:"0 0 8px",fontWeight:400}}>AMOUNT</th>
              </tr>
            </thead>
            <tbody>
              {result.lineItems.map((item,i)=>(
                <tr key={i} style={{borderBottom:"1px solid #f0ebe0"}}>
                  <td style={{padding:"10px 0",fontSize:12,color:"#1a1a1a"}}>{item.label}</td>
                  <td style={{padding:"10px 0",fontSize:12,color:"#6a6055",textAlign:"center"}}>{item.qty||"—"}</td>
                  <td style={{padding:"10px 0",fontSize:12,fontFamily:"monospace",textAlign:"right",color:"#1a1a2e",fontWeight:600}}>{fmtRange(item.low,item.high)}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <div style={{display:"flex",justifyContent:"flex-end",marginBottom:24}}>
            <div style={{minWidth:220}}>
              <div style={{display:"flex",justifyContent:"space-between",padding:"12px 0",borderTop:"2px solid #1a1a2e"}}>
                <span style={{fontSize:15,fontWeight:700}}>Total</span>
                <span style={{fontSize:18,fontWeight:700,color:"#1a1a2e",fontFamily:"monospace"}}>
                  {result.pricingMode==="tm"?fmt(result.tmTotal):`${fmt(result.totalLow)}–${fmt(result.totalHigh)}`}
                </span>
              </div>
            </div>
          </div>
          {invoiceNotes&&<div style={{background:"#f8f4ec",borderRadius:8,padding:14,marginBottom:20}}>
            <div style={{fontSize:10,color:"#8a8070",fontFamily:"monospace",letterSpacing:2,marginBottom:6}}>NOTES</div>
            <div style={{fontSize:12,color:"#3a3030",lineHeight:1.7}}>{invoiceNotes}</div>
          </div>}
          <div style={{fontSize:10,color:"#aaa",textAlign:"center",lineHeight:1.8}}>
            All work performed to NEC 2023 standards · Estimate valid 30 days<br/>
            Thank you for your business
          </div>
        </div>
        <div style={{display:"flex",gap:10,marginTop:16,justifyContent:"center",flexWrap:"wrap"}}>
          <button onClick={()=>window.print()} style={{background:"#1a1a2e",color:"white",border:"none",borderRadius:8,padding:"12px 24px",cursor:"pointer",fontSize:13}}>🖨️ Print / Save PDF</button>
          <button onClick={()=>setShowInvoice(false)} style={{...ghostBtn,padding:"12px 20px",fontSize:13}}>← Back</button>
        </div>
      </div>
      <style>{`@media print{button{display:none!important}}`}</style>
    </div>
  );

  // ── CUSTOMER VIEW ─────────────────────────────────────────────────────────
  if (showCustomer && result) return (
    <div style={{minHeight:"100vh",background:"#f4f1ea",fontFamily:"Georgia,serif",color:"#1a1a1a",padding:"32px 16px"}}>
      <div style={{maxWidth:620,margin:"0 auto"}}>
        <div style={{background:"#1a1a2e",color:"white",borderRadius:"12px 12px 0 0",padding:28}}>
          <div style={{fontSize:10,letterSpacing:3,color:"#f5a623",fontFamily:"monospace",marginBottom:4}}>VOLT●QUOTE ESTIMATE</div>
          <div style={{fontSize:19,fontWeight:700}}>{contractorName||"Professional Electrician"}</div>
          {contractorPhone&&<div style={{fontSize:12,color:"#c0b89a",marginTop:3}}>📞 {contractorPhone}</div>}
          {contractorEmail&&<div style={{fontSize:12,color:"#c0b89a"}}>✉️ {contractorEmail}</div>}
          {contractorLicense&&<div style={{fontSize:11,color:"#f5a623",marginTop:4}}>License #{contractorLicense}</div>}
          <div style={{fontSize:11,color:"#5a5060",marginTop:8,fontFamily:"monospace"}}>{result.date} · {result.region}</div>
        </div>
        <div style={{background:"white",border:"1px solid #e0d8c8",borderTop:"none",borderRadius:"0 0 12px 12px",padding:28}}>
          {result.lineItems.filter(i=>i.qty).map((item,i)=>(
            <div key={i} style={{display:"flex",justifyContent:"space-between",padding:"10px 0",borderBottom:"1px solid #f0ebe0"}}>
              <div>
                <div style={{fontSize:13,fontWeight:500}}>{item.label}</div>
                <div style={{fontSize:11,color:"#8a8070"}}>Qty: {item.qty}</div>
              </div>
              <div style={{fontSize:13,fontFamily:"monospace",fontWeight:600,color:"#1a1a2e"}}>{fmtRange(item.low,item.high)}</div>
            </div>
          ))}
          <div style={{display:"flex",justifyContent:"space-between",alignItems:"center",padding:"16px 0",borderTop:"2px solid #1a1a2e",marginTop:4}}>
            <div>
              <div style={{fontSize:14,fontWeight:700}}>Total Estimate</div>
              <div style={{fontSize:11,color:"#8a8070"}}>~{result.totalHours} labor hours</div>
            </div>
            <div style={{fontSize:22,fontWeight:700,color:"#1a1a2e",fontFamily:"monospace"}}>
              {result.pricingMode==="tm"?fmt(result.tmTotal):`${fmt(result.totalLow)}–${fmt(result.totalHigh)}`}
            </div>
          </div>
          {aiSummary&&<div style={{background:"#f8f4ec",borderRadius:8,padding:16,fontSize:12,lineHeight:1.8,color:"#3a3030",borderLeft:"3px solid #f5a623",marginBottom:16}}>{aiSummary}</div>}
          <div style={{fontSize:10,color:"#aaa",textAlign:"center",lineHeight:1.8}}>Estimate valid 30 days · Final price subject to on-site inspection<br/>All work performed to NEC 2023 standards</div>
        </div>
        <div style={{display:"flex",gap:10,marginTop:14,justifyContent:"center",flexWrap:"wrap"}}>
          <button onClick={()=>{setShowInvoice(true);setShowCustomer(false);}} style={{background:"#1a1a2e",color:"white",border:"none",borderRadius:8,padding:"11px 20px",cursor:"pointer",fontSize:12}}>📄 Convert to Invoice</button>
          <button onClick={()=>setShowCustomer(false)} style={{...ghostBtn,padding:"11px 18px",fontSize:12}}>← Back</button>
        </div>
      </div>
    </div>
  );

  // ── MAIN APP ──────────────────────────────────────────────────────────────
  const tabBtn = (t,label) => (
    <button onClick={()=>setActiveTab(t)} style={{
      padding:"10px 14px",cursor:"pointer",fontSize:11,fontFamily:"monospace",
      letterSpacing:1.5,textTransform:"uppercase",border:"none",whiteSpace:"nowrap",
      background:activeTab===t?"rgba(245,166,35,0.15)":"transparent",
      color:activeTab===t?"#f5a623":"#5a5555",
      borderBottom:activeTab===t?"2px solid #f5a623":"2px solid transparent",transition:"all 0.2s"
    }}>{label}</button>
  );

  return (
    <div style={{minHeight:"100vh",background:"#0a0a0f",fontFamily:"Georgia,serif",color:"#e8e0d0"}}>
      <div style={{position:"fixed",inset:0,zIndex:0,background:"radial-gradient(ellipse at 20% 50%,#1a1a2e 0%,transparent 60%),radial-gradient(ellipse at 80% 20%,#16213e 0%,transparent 50%)",pointerEvents:"none"}}/>
      {/* Header */}
      <div style={{position:"relative",zIndex:1,borderBottom:"1px solid rgba(245,166,35,0.3)",padding:"18px 18px 0",background:"linear-gradient(180deg,#0d0d1a 0%,transparent 100%)"}}>
        <div style={{maxWidth:860,margin:"0 auto"}}>
          <div style={{display:"flex",alignItems:"center",gap:10,marginBottom:4}}>
            <button onClick={()=>setView("landing")} style={{background:"none",border:"none",cursor:"pointer",padding:0}}>
              <div style={{width:32,height:32,borderRadius:"50%",background:"linear-gradient(135deg,#f5a623,#e8860a)",display:"flex",alignItems:"center",justifyContent:"center",fontSize:17,boxShadow:"0 0 20px rgba(245,166,35,0.45)"}}>⚡</div>
            </button>
            <div style={{flex:1}}>
              <div style={{fontSize:9,letterSpacing:4,color:"#f5a623",fontFamily:"monospace",textTransform:"uppercase"}}>{T.appTagline}</div>
              <div style={{fontSize:18,fontWeight:700,lineHeight:1}}><span style={{color:"#f5a623"}}>VOLT</span><span style={{color:"rgba(245,166,35,0.35)",margin:"0 3px",fontSize:12}}>●</span><span style={{color:"#fff",fontWeight:400}}>QUOTE</span></div>
            </div>
            <div style={{display:"flex",gap:8,alignItems:"center"}}>
              {totalItems>0&&<div style={{background:"rgba(245,166,35,0.18)",border:"1px solid rgba(245,166,35,0.35)",borderRadius:20,padding:"3px 10px",fontSize:10,color:"#f5a623",fontFamily:"monospace"}}>{totalItems}</div>}
              <button onClick={()=>setLang(lang==="en"?"es":"en")} style={{...ghostBtn,fontSize:10,padding:"5px 10px"}}>{T.langToggle}</button>
              <button onClick={handleShare} style={{...ghostBtn,fontSize:10,padding:"5px 10px"}}>🔗 {showShare?"Copied!":"Share"}</button>
            </div>
          </div>
          <div style={{display:"flex",overflowX:"auto",gap:0}}>
            {tabBtn("estimator","📋 "+T.estimator)}
            {tabBtn("sqft","📐 Sq Ft")}
            {tabBtn("photo","📷 "+T.photo)}
            {tabBtn("overhead","💰 "+T.overhead)}
            {tabBtn("contractor","🪪 "+T.contractor)}
            {tabBtn("ask","💬 "+T.ask)}
            {tabBtn("history","🗂 "+T.history)}
            {tabBtn("nec","📖 "+T.nec)}
          </div>
        </div>
      </div>

      <div style={{maxWidth:860,margin:"0 auto",padding:"20px 18px",position:"relative",zIndex:1}}>

        {/* ── ESTIMATOR TAB ── */}
        {activeTab==="estimator"&&<>
          {photoAnalysis&&(
            <div style={{background:"rgba(85,168,120,0.07)",border:"1px solid rgba(85,168,120,0.28)",borderRadius:10,padding:14,marginBottom:18}}>
              <div style={{fontSize:9,letterSpacing:3,color:"#55a878",fontFamily:"monospace",marginBottom:6,textTransform:"uppercase"}}>📷 Photo Analysis</div>
              <p style={{margin:0,fontSize:12,color:"#b0c89a",lineHeight:1.7,whiteSpace:"pre-wrap"}}>{photoAnalysis}</p>
              <button onClick={()=>setPhotoAnalysis("")} style={{marginTop:8,fontSize:10,color:"#4a4a4a",background:"none",border:"none",cursor:"pointer"}}>✕ Dismiss</button>
            </div>
          )}
          <Sec title={"01 — "+T.region}>
            <div style={{display:"grid",gridTemplateColumns:"1fr auto",gap:10,marginBottom:10}}>
              <select value={region} onChange={e=>setRegion(e.target.value)} style={sel}>{Object.keys(REGION_MULTIPLIERS).map(r=><option key={r} value={r}>{r}</option>)}</select>
              <div style={{background:"rgba(245,166,35,0.09)",border:"1px solid rgba(245,166,35,0.28)",borderRadius:8,padding:"8px 12px",textAlign:"center",flexShrink:0}}>
                <div style={{fontSize:9,color:"#6a6055",fontFamily:"monospace"}}>RATE</div>
                <div style={{fontSize:15,fontWeight:700,color:"#f5a623",fontFamily:"monospace"}}>{regionMultiplier.toFixed(2)}x</div>
                <div style={{fontSize:8,color:"#4a4040",fontFamily:"monospace",marginTop:2}}>prices live</div>
              </div>
            </div>
            <div style={{display:"flex",gap:8}}>
              {[[" flat",T.flatRate],["tm",T.timeAndMat]].map(([v,l])=>(
                <button key={v} onClick={()=>setPricingMode(v.trim())} style={{flex:1,padding:"9px",borderRadius:8,cursor:"pointer",fontSize:12,fontFamily:"monospace",background:pricingMode===v.trim()?"rgba(245,166,35,0.13)":"rgba(255,255,255,0.02)",border:`1px solid ${pricingMode===v.trim()?"rgba(245,166,35,0.45)":"rgba(255,255,255,0.07)"}`,color:pricingMode===v.trim()?"#f5a623":"#5a5555",fontWeight:pricingMode===v.trim()?700:400}}>{l}</button>
              ))}
            </div>
            {pricingMode==="tm"&&(
              <div style={{marginTop:10,background:"rgba(255,255,255,0.02)",border:"1px solid rgba(255,255,255,0.06)",borderRadius:8,padding:"12px 14px"}}>
                <div style={{fontSize:11,color:"#7a7060",marginBottom:7}}>{T.hourlyRate}: <span style={{color:"#f5a623",fontWeight:700}}>${hourlyRate}/hr</span></div>
                <input type="range" min={50} max={200} value={hourlyRate} onChange={e=>setHourlyRate(Number(e.target.value))} style={{width:"100%",accentColor:"#f5a623"}}/>
                <div style={{display:"flex",justifyContent:"space-between",fontSize:10,color:"#3a3040",fontFamily:"monospace",marginTop:3}}><span>$50</span><span>$125</span><span>$200</span></div>
              </div>
            )}
          </Sec>

          <Sec title={"02 — "+T.scopeTitle}>
            {Object.entries(JOB_CATEGORIES).map(([cat,items])=>(
              <div key={cat} style={{marginBottom:7}}>
                <button onClick={()=>setExpandedCats(e=>({...e,[cat]:!e[cat]}))} style={{width:"100%",display:"flex",justifyContent:"space-between",alignItems:"center",background:"rgba(255,255,255,0.025)",border:"1px solid rgba(245,166,35,0.15)",borderRadius:expandedCats[cat]?"8px 8px 0 0":"8px",padding:"10px 13px",cursor:"pointer",color:"#e8e0d0",fontSize:13,fontWeight:600}}>
                  <span>{cat}</span>
                  <div style={{display:"flex",alignItems:"center",gap:7}}>
                    {Object.keys(items).some(k=>quantities[k]>0)&&<span style={{fontSize:9,background:"rgba(245,166,35,0.18)",color:"#f5a623",borderRadius:10,padding:"2px 7px",fontFamily:"monospace"}}>{Object.keys(items).filter(k=>quantities[k]>0).length} sel.</span>}
                    <span style={{color:"#f5a623",fontSize:13}}>{expandedCats[cat]?"▲":"▼"}</span>
                  </div>
                </button>
                {expandedCats[cat]&&(
                  <div style={{border:"1px solid rgba(245,166,35,0.15)",borderTop:"none",borderRadius:"0 0 8px 8px",overflow:"hidden"}}>
                    {Object.entries(items).map(([key,item],idx)=>(
                      <div key={key} style={{display:"flex",alignItems:"center",justifyContent:"space-between",padding:"9px 13px",background:quantities[key]>0?"rgba(245,166,35,0.04)":idx%2===0?"rgba(255,255,255,0.01)":"transparent",borderBottom:"1px solid rgba(255,255,255,0.03)"}}>
                        <div style={{flex:1,minWidth:0}}>
                          <div style={{fontSize:12,color:"#e0d8c8"}}>{item.label}</div>
                          <div style={{display:"flex",gap:6,flexWrap:"wrap",marginTop:2}}>
                            <span style={{fontSize:10,color:"#4a4848",fontFamily:"monospace"}}>${Math.round(item.low*regionMultiplier)}–${Math.round(item.high*regionMultiplier)}/{item.unit}</span>
                            <span style={{fontSize:10,color:"#4a5448",fontFamily:"monospace"}}>~{item.hours}h</span>
                            {item.nec&&<span style={{fontSize:9,background:"rgba(85,136,224,0.1)",color:"#7aa8f0",borderRadius:3,padding:"1px 5px",fontFamily:"monospace",cursor:"pointer"}} onClick={()=>{setActiveTab("nec");setNecSearch(item.nec);}}>§{item.nec}</span>}
                          </div>
                        </div>
                        <div style={{display:"flex",alignItems:"center",gap:7,flexShrink:0}}>
                          <button onClick={()=>setQuantities(q=>({...q,[key]:Math.max(0,(q[key]||0)-1)}))} style={qBtn}>−</button>
                          <span style={{minWidth:24,textAlign:"center",fontSize:14,fontWeight:700,color:quantities[key]>0?"#f5a623":"#2a2a3a"}}>{quantities[key]||0}</span>
                          <button onClick={()=>setQuantities(q=>({...q,[key]:(q[key]||0)+1}))} style={qBtn}>+</button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ))}
          </Sec>

          <Sec title={"03 — "+T.conditions}>
            <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:7}}>
              {Object.entries(CONDITION_ADJUSTMENTS).map(([key,cond])=>(
                <label key={key} style={{display:"flex",alignItems:"center",gap:8,cursor:"pointer",background:conditions[key]?"rgba(245,166,35,0.05)":"rgba(255,255,255,0.02)",border:`1px solid ${conditions[key]?"rgba(245,166,35,0.32)":"rgba(255,255,255,0.05)"}`,borderRadius:8,padding:"9px 11px",transition:"all 0.2s"}}>
                  <input type="checkbox" checked={!!conditions[key]} onChange={e=>setConditions(c=>({...c,[key]:e.target.checked}))} style={{accentColor:"#f5a623",width:14,height:14}}/>
                  <div style={{flex:1}}>
                    <div style={{fontSize:11,color:"#e0d8c8"}}>{cond.icon} {lang==="es"?cond.labelEs:cond.label}</div>
                    <div style={{fontSize:10,color:cond.color,fontFamily:"monospace"}}>{cond.multiplier>1?"+":""}{Math.round((cond.multiplier-1)*100)}%</div>
                  </div>
                </label>
              ))}
            </div>
          </Sec>

          <Sec title={"04 — "+T.pricingOpts}>
            <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:8,marginBottom:10}}>
              {[{label:T.includeMat,state:includeMaterials,set:setIncludeMaterials},{label:T.includePermit,state:includePermit,set:setIncludePermit}].map(opt=>(
                <label key={opt.label} style={{display:"flex",alignItems:"center",gap:8,cursor:"pointer",background:opt.state?"rgba(245,166,35,0.05)":"rgba(255,255,255,0.02)",border:`1px solid ${opt.state?"rgba(245,166,35,0.28)":"rgba(255,255,255,0.05)"}`,borderRadius:8,padding:"10px 13px",transition:"all 0.2s"}}>
                  <input type="checkbox" checked={opt.state} onChange={e=>opt.set(e.target.checked)} style={{accentColor:"#f5a623",width:14,height:14}}/>
                  <span style={{fontSize:12,color:"#b8b09a"}}>{opt.label}</span>
                </label>
              ))}
            </div>
            {includeMaterials&&<div style={{background:"rgba(255,255,255,0.02)",border:"1px solid rgba(255,255,255,0.05)",borderRadius:8,padding:"12px 13px"}}>
              <div style={{fontSize:11,color:"#7a7060",marginBottom:7}}>{T.matMarkup}: <span style={{color:"#f5a623",fontWeight:700}}>{markupPct}%</span></div>
              <input type="range" min={0} max={50} value={markupPct} onChange={e=>setMarkupPct(Number(e.target.value))} style={{width:"100%",accentColor:"#f5a623"}}/>
              <div style={{display:"flex",justifyContent:"space-between",fontSize:10,color:"#3a3040",fontFamily:"monospace",marginTop:3}}><span>0%</span><span>25%</span><span>50%</span></div>
            </div>}
          </Sec>

          <button onClick={calculateEstimate} disabled={!hasItems} style={{width:"100%",padding:"15px",marginBottom:22,background:hasItems?"linear-gradient(135deg,#f5a623,#e8860a)":"#111120",border:"none",borderRadius:10,cursor:hasItems?"pointer":"not-allowed",fontSize:13,fontWeight:700,color:hasItems?"#0a0a0f":"#2a2a3a",letterSpacing:2,textTransform:"uppercase",fontFamily:"monospace",boxShadow:hasItems?"0 4px 28px rgba(245,166,35,0.32)":"none",transition:"all 0.3s"}}>
            {hasItems?"⚡ "+T.generateBtn:T.selectItems}
          </button>

          {result&&(
            <div style={{background:"rgba(245,166,35,0.025)",border:"1px solid rgba(245,166,35,0.2)",borderRadius:12,padding:20,animation:"fadeIn 0.4s ease"}}>
              <div style={{display:"flex",justifyContent:"space-between",alignItems:"flex-start",marginBottom:16,flexWrap:"wrap",gap:8}}>
                <div>
                  <div style={{fontSize:9,letterSpacing:3,color:"#f5a623",fontFamily:"monospace",textTransform:"uppercase"}}>{T.estReady}</div>
                  <div style={{fontSize:11,color:"#5a5555"}}>{result.region} · {result.pricingMode==="tm"?T.timeAndMat:T.flatRate} · {result.date}</div>
                </div>
                <div style={{display:"flex",gap:7,flexWrap:"wrap"}}>
                  <button onClick={()=>setShowCustomer(true)} style={{background:"rgba(85,136,224,0.12)",border:"1px solid rgba(85,136,224,0.3)",borderRadius:6,padding:"7px 11px",cursor:"pointer",color:"#7aa8f0",fontSize:10,fontFamily:"monospace"}}>👤 {T.customerView}</button>
                  <button onClick={()=>setShowInvoice(true)} style={{background:"rgba(85,168,120,0.12)",border:"1px solid rgba(85,168,120,0.3)",borderRadius:6,padding:"7px 11px",cursor:"pointer",color:"#55a878",fontSize:10,fontFamily:"monospace"}}>📄 Invoice</button>
                  <button onClick={saveEstimate} style={{background:saved?"rgba(80,200,120,0.12)":"rgba(255,255,255,0.04)",border:`1px solid ${saved?"rgba(80,200,120,0.35)":"rgba(255,255,255,0.08)"}`,borderRadius:6,padding:"7px 11px",cursor:"pointer",color:saved?"#50c878":"#8a8070",fontSize:10,fontFamily:"monospace"}}>{saved?"✓ "+T.savedOk:T.saveEst}</button>
                  <button onClick={copyQuote} style={{background:copied?"rgba(80,200,120,0.12)":"rgba(245,166,35,0.09)",border:`1px solid ${copied?"rgba(80,200,120,0.35)":"rgba(245,166,35,0.28)"}`,borderRadius:6,padding:"7px 11px",cursor:"pointer",color:copied?"#50c878":"#f5a623",fontSize:10,fontFamily:"monospace"}}>{copied?"✓ "+T.copied:T.copyQuote}</button>
                </div>
              </div>
              {result.lineItems.map((item,i)=>(
                <div key={i} style={{display:"flex",justifyContent:"space-between",alignItems:"center",padding:"8px 0",borderBottom:"1px solid rgba(255,255,255,0.04)"}}>
                  <div>
                    <span style={{fontSize:12,color:"#d8d0c0"}}>{item.label}</span>
                    {item.qty&&<span style={{fontSize:10,color:"#3a3a3a",marginLeft:5,fontFamily:"monospace"}}>×{item.qty}</span>}
                    {item.hrs>0&&<span style={{fontSize:10,color:"#3a4a3a",marginLeft:5,fontFamily:"monospace"}}>{item.hrs}h</span>}
                    {item.nec&&<span style={{fontSize:9,background:"rgba(85,136,224,0.08)",color:"#6898e0",borderRadius:3,padding:"1px 4px",marginLeft:5,fontFamily:"monospace"}}>§{item.nec}</span>}
                  </div>
                  <div style={{fontSize:12,color:"#b8b09a",fontFamily:"monospace",flexShrink:0}}>{fmtRange(item.low,item.high)}</div>
                </div>
              ))}
              <div style={{display:"flex",justifyContent:"space-between",alignItems:"center",padding:"13px 0",borderTop:"2px solid rgba(245,166,35,0.28)",marginTop:4}}>
                <div>
                  <div style={{fontSize:13,fontWeight:700,color:"#fff"}}>{T.totalEst}</div>
                  <div style={{fontSize:10,color:"#4a4a4a",fontFamily:"monospace"}}>~{result.totalHours} {T.laborHours}</div>
                </div>
                <div style={{fontSize:18,fontWeight:700,color:"#f5a623",fontFamily:"monospace"}}>
                  {result.pricingMode==="tm"?fmt(result.tmTotal):`${fmt(result.totalLow)}–${fmt(result.totalHigh)}`}
                </div>
              </div>
              <div style={{marginTop:13,padding:13,background:"rgba(0,0,0,0.22)",borderRadius:8,borderLeft:"3px solid #f5a623"}}>
                <div style={{fontSize:9,letterSpacing:3,color:"#f5a623",fontFamily:"monospace",marginBottom:7,textTransform:"uppercase"}}>{T.profSummary}</div>
                {loading?<div style={{color:"#3a3a3a",fontSize:12,fontStyle:"italic"}}>{T.generating}</div>
                  :<p style={{margin:0,fontSize:12,color:"#b0a898",lineHeight:1.8}}>{aiSummary}</p>}
              </div>
            </div>
          )}
        </>}

        {/* ── SQ FT TAB ── */}
        {activeTab==="sqft"&&(
          <Sec title={"📐 "+T.sqftTitle}>
            <p style={{fontSize:12,color:"#5a5555",marginTop:0,lineHeight:1.7,marginBottom:20}}>{T.sqftDesc}</p>
            <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:12}}>
              {Object.entries(SQFT_PRESETS).map(([name,preset])=>(
                <button key={name} onClick={()=>applySquareFootage(preset)} style={{background:"rgba(245,166,35,0.04)",border:"1px solid rgba(245,166,35,0.2)",borderRadius:10,padding:18,cursor:"pointer",textAlign:"left",transition:"all 0.2s"}}>
                  <div style={{fontSize:13,fontWeight:700,color:"#f5a623",marginBottom:6}}>{name}</div>
                  <div style={{fontSize:11,color:"#5a5555",lineHeight:1.8}}>
                    {Object.entries(preset).map(([k,v])=>`${v} ${JOB_CATEGORIES["Wiring Devices"][k]?.label||JOB_CATEGORIES["Lighting"][k]?.label||JOB_CATEGORIES["Safety Devices"][k]?.label||JOB_CATEGORIES["Panels & Service"][k]?.label||k}`).join("\n")}
                  </div>
                  <div style={{marginTop:10,fontSize:10,color:"#f5a623",fontFamily:"monospace",letterSpacing:1}}>→ APPLY TO ESTIMATE</div>
                </button>
              ))}
            </div>
          </Sec>
        )}

        {/* ── PHOTO TAB ── */}
        {activeTab==="photo"&&(
          <Sec title="📷 Photo-to-Estimate">
            <p style={{fontSize:12,color:"#5a5555",marginTop:0,lineHeight:1.7}}>Upload a photo of any room, panel, or electrical area and AI analyzes what work may be needed.</p>
            <input ref={fileRef} type="file" accept="image/*" onChange={handlePhotoUpload} style={{display:"none"}}/>
            <button onClick={()=>fileRef.current?.click()} disabled={photoLoading} style={{width:"100%",padding:36,border:"2px dashed rgba(245,166,35,0.3)",borderRadius:12,background:"rgba(245,166,35,0.03)",cursor:"pointer",color:"#f5a623",fontSize:13,fontFamily:"monospace",letterSpacing:2,textTransform:"uppercase",transition:"all 0.2s"}}>
              {photoLoading?"🔍 Analyzing...":"📷 Upload Photo"}
            </button>
            {photoAnalysis&&(
              <div style={{marginTop:18,background:"rgba(255,255,255,0.02)",border:"1px solid rgba(245,166,35,0.18)",borderRadius:10,padding:16}}>
                <div style={{fontSize:9,letterSpacing:3,color:"#f5a623",fontFamily:"monospace",marginBottom:8,textTransform:"uppercase"}}>AI Analysis</div>
                <p style={{margin:0,fontSize:12,color:"#b0a898",lineHeight:1.8,whiteSpace:"pre-wrap"}}>{photoAnalysis}</p>
                <button onClick={()=>setActiveTab("estimator")} style={{marginTop:12,background:"rgba(245,166,35,0.12)",border:"1px solid rgba(245,166,35,0.3)",borderRadius:8,padding:"10px 18px",cursor:"pointer",color:"#f5a623",fontSize:11,fontFamily:"monospace"}}>→ Go Build Estimate</button>
              </div>
            )}
          </Sec>
        )}

        {/* ── OVERHEAD TAB ── */}
        {activeTab==="overhead"&&(
          <div>
            <Sec title={"💰 "+T.overheadTitle}>
              <p style={{fontSize:12,color:"#5a5555",marginTop:0,lineHeight:1.7,marginBottom:16}}>{T.overheadDesc}</p>
              <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:10,marginBottom:16}}>
                {[
                  {key:"insurance",label:"Liability Insurance /mo"},
                  {key:"vehicle",label:"Vehicle / Gas /mo"},
                  {key:"tools",label:"Tools / Equipment /mo"},
                  {key:"phone",label:"Phone / Software /mo"},
                  {key:"misc",label:"Misc Overhead /mo"},
                ].map(f=>(
                  <div key={f.key}>
                    <div style={{fontSize:10,color:"#5a5555",fontFamily:"monospace",marginBottom:4,letterSpacing:1}}>{f.label.toUpperCase()}</div>
                    <div style={{display:"flex",alignItems:"center",background:"rgba(255,255,255,0.03)",border:"1px solid rgba(245,166,35,0.2)",borderRadius:8,overflow:"hidden"}}>
                      <span style={{padding:"0 10px",color:"#f5a623",fontSize:13}}>$</span>
                      <input type="number" value={overhead[f.key]} onChange={e=>setOverhead(o=>({...o,[f.key]:Number(e.target.value)||0}))} style={{flex:1,background:"transparent",border:"none",color:"#e8e0d0",fontSize:13,padding:"10px 10px 10px 0",outline:"none",fontFamily:"Georgia,serif"}}/>
                    </div>
                  </div>
                ))}
              </div>
              <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:10,marginBottom:20}}>
                <div>
                  <div style={{fontSize:10,color:"#5a5555",fontFamily:"monospace",marginBottom:4,letterSpacing:1}}>BILLABLE HOURS / MONTH</div>
                  <input type="number" value={targetHours} onChange={e=>setTargetHours(Number(e.target.value)||1)} style={{...sel,fontSize:13}}/>
                </div>
                <div>
                  <div style={{fontSize:10,color:"#5a5555",fontFamily:"monospace",marginBottom:4,letterSpacing:1}}>DESIRED PROFIT MARGIN %</div>
                  <input type="number" value={desiredProfit} onChange={e=>setDesiredProfit(Number(e.target.value)||0)} style={{...sel,fontSize:13}}/>
                </div>
              </div>
              <div style={{background:"rgba(245,166,35,0.07)",border:"1px solid rgba(245,166,35,0.3)",borderRadius:12,padding:22}}>
                <div style={{display:"grid",gridTemplateColumns:"1fr 1fr 1fr",gap:16,textAlign:"center"}}>
                  <div>
                    <div style={{fontSize:10,color:"#8a8070",fontFamily:"monospace",letterSpacing:1,marginBottom:6}}>MONTHLY OVERHEAD</div>
                    <div style={{fontSize:22,fontWeight:700,color:"#e05555",fontFamily:"monospace"}}>{fmt(totalOverhead)}</div>
                  </div>
                  <div>
                    <div style={{fontSize:10,color:"#8a8070",fontFamily:"monospace",letterSpacing:1,marginBottom:6}}>BREAK-EVEN RATE</div>
                    <div style={{fontSize:22,fontWeight:700,color:"#e08a55",fontFamily:"monospace"}}>${Math.ceil(totalOverhead/targetHours)}/hr</div>
                  </div>
                  <div>
                    <div style={{fontSize:10,color:"#8a8070",fontFamily:"monospace",letterSpacing:1,marginBottom:6}}>YOUR TRUE RATE</div>
                    <div style={{fontSize:22,fontWeight:700,color:"#f5a623",fontFamily:"monospace"}}>${trueHourlyRate}/hr</div>
                  </div>
                </div>
                <div style={{marginTop:16,paddingTop:14,borderTop:"1px solid rgba(245,166,35,0.2)",fontSize:12,color:"#8a8070",textAlign:"center",lineHeight:1.7}}>
                  Your true hourly rate includes overhead recovery + {desiredProfit}% profit margin.<br/>
                  <span style={{color:"#f5a623"}}>Use ${trueHourlyRate}/hr</span> in your Time & Material estimates to actually profit.
                </div>
                <div style={{marginTop:12,textAlign:"center"}}>
                  <button onClick={()=>{setHourlyRate(trueHourlyRate);setPricingMode("tm");setActiveTab("estimator");}} style={{background:"rgba(245,166,35,0.15)",border:"1px solid rgba(245,166,35,0.35)",borderRadius:8,padding:"10px 20px",cursor:"pointer",color:"#f5a623",fontSize:12,fontFamily:"monospace"}}>→ Apply to T&M Estimator</button>
                </div>
              </div>
            </Sec>
          </div>
        )}

        {/* ── CONTRACTOR TAB ── */}
        {activeTab==="contractor"&&(
          <Sec title={"🪪 "+T.contractor}>
            <p style={{fontSize:12,color:"#5a5555",marginTop:0,lineHeight:1.7}}>Your info appears on customer quotes and invoices.</p>
            <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:10}}>
              {[
                {label:"Company / Name",val:contractorName,set:setContractorName,ph:"Smith Electric LLC"},
                {label:"Phone",val:contractorPhone,set:setContractorPhone,ph:"(607) 555-0100"},
                {label:"Email",val:contractorEmail,set:setContractorEmail,ph:"you@email.com"},
                {label:"License #",val:contractorLicense,set:setContractorLicense,ph:"ME-12345"},
                {label:"City",val:contractorCity,set:setContractorCity,ph:"Binghamton"},
              ].map(f=>(
                <div key={f.label}>
                  <div style={{fontSize:10,color:"#5a5555",fontFamily:"monospace",marginBottom:4,letterSpacing:1}}>{f.label.toUpperCase()}</div>
                  <input value={f.val} onChange={e=>f.set(e.target.value)} placeholder={f.ph} style={{...sel,fontSize:12}}/>
                </div>
              ))}
              <div>
                <div style={{fontSize:10,color:"#5a5555",fontFamily:"monospace",marginBottom:4,letterSpacing:1}}>STATE</div>
                <select value={contractorState} onChange={e=>setContractorState(e.target.value)} style={sel}>{US_STATES.map(s=><option key={s} value={s}>{s}</option>)}</select>
              </div>
            </div>
            <div style={{marginTop:20}}>
              <div style={{fontSize:10,color:"#5a5555",fontFamily:"monospace",marginBottom:10,letterSpacing:1}}>INVOICE DEFAULTS</div>
              <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:10}}>
                <div>
                  <div style={{fontSize:10,color:"#5a5555",fontFamily:"monospace",marginBottom:4}}>DEFAULT CLIENT NAME</div>
                  <input value={invoiceClient} onChange={e=>setInvoiceClient(e.target.value)} placeholder="Customer name" style={{...sel,fontSize:12}}/>
                </div>
                <div>
                  <div style={{fontSize:10,color:"#5a5555",fontFamily:"monospace",marginBottom:4}}>PAYMENT TERMS (DAYS)</div>
                  <select value={invoiceDue} onChange={e=>setInvoiceDue(Number(e.target.value))} style={sel}>{[15,30,45,60].map(d=><option key={d} value={d}>Net {d}</option>)}</select>
                </div>
                <div style={{gridColumn:"1/-1"}}>
                  <div style={{fontSize:10,color:"#5a5555",fontFamily:"monospace",marginBottom:4}}>INVOICE NOTES / TERMS</div>
                  <textarea value={invoiceNotes} onChange={e=>setInvoiceNotes(e.target.value)} placeholder="e.g. 50% deposit required. Balance due on completion." style={{...sel,height:70,resize:"vertical",lineHeight:1.6}}/>
                </div>
              </div>
            </div>
            {contractorName&&(
              <div style={{marginTop:20,background:"rgba(245,166,35,0.05)",border:"1px solid rgba(245,166,35,0.22)",borderRadius:10,padding:16}}>
                <div style={{fontSize:9,color:"#f5a623",fontFamily:"monospace",letterSpacing:2,marginBottom:8,textTransform:"uppercase"}}>Preview</div>
                <div style={{fontSize:15,fontWeight:700,color:"#fff"}}>{contractorName}</div>
                {contractorPhone&&<div style={{fontSize:12,color:"#b8b09a"}}>📞 {contractorPhone}</div>}
                {contractorEmail&&<div style={{fontSize:12,color:"#b8b09a"}}>✉️ {contractorEmail}</div>}
                {contractorLicense&&<div style={{fontSize:11,color:"#f5a623"}}>License #{contractorLicense}</div>}
                {(contractorCity||contractorState)&&<div style={{fontSize:11,color:"#6a6055"}}>{[contractorCity,contractorState].filter(Boolean).join(", ")}</div>}
              </div>
            )}
          </Sec>
        )}

        {/* ── ASK AI TAB ── */}
        {activeTab==="ask"&&(
          <Sec title="💬 Ask the AI Electrician">
            <p style={{fontSize:12,color:"#5a5555",marginTop:0,lineHeight:1.7}}>Ask code questions, pricing, scope clarification — anything. Context-aware if you have an active estimate.</p>
            <div style={{background:"rgba(255,255,255,0.02)",border:"1px solid rgba(255,255,255,0.06)",borderRadius:10,padding:14,minHeight:260,maxHeight:380,overflowY:"auto",marginBottom:10}}>
              {aiChat.length===0&&<div style={{color:"#2a2535",fontSize:12,fontFamily:"monospace",textAlign:"center",marginTop:50}}>e.g. "Do I need AFCI in the kitchen?" or "What gauge wire for a dryer?"</div>}
              {aiChat.map((m,i)=>(
                <div key={i} style={{marginBottom:12,display:"flex",gap:8,justifyContent:m.role==="user"?"flex-end":"flex-start"}}>
                  {m.role==="assistant"&&<div style={{width:22,height:22,borderRadius:"50%",background:"linear-gradient(135deg,#f5a623,#e8860a)",display:"flex",alignItems:"center",justifyContent:"center",fontSize:11,flexShrink:0}}>⚡</div>}
                  <div style={{maxWidth:"82%",background:m.role==="user"?"rgba(245,166,35,0.1)":"rgba(255,255,255,0.03)",border:`1px solid ${m.role==="user"?"rgba(245,166,35,0.22)":"rgba(255,255,255,0.06)"}`,borderRadius:9,padding:"9px 13px",fontSize:12,color:m.role==="user"?"#e8c878":"#b0a898",lineHeight:1.7}}>{m.content}</div>
                </div>
              ))}
              {chatLoading&&<div style={{color:"#3a3535",fontSize:12,fontStyle:"italic",fontFamily:"monospace"}}>⚡ Thinking...</div>}
            </div>
            <div style={{display:"flex",gap:8}}>
              <input value={chatInput} onChange={e=>setChatInput(e.target.value)} onKeyDown={e=>e.key==="Enter"&&!e.shiftKey&&sendChat()} placeholder="Ask anything..." style={{...sel,flex:1,fontSize:12}}/>
              <button onClick={sendChat} disabled={!chatInput.trim()||chatLoading} style={{background:chatInput.trim()?"linear-gradient(135deg,#f5a623,#e8860a)":"#111120",border:"none",borderRadius:8,padding:"0 16px",cursor:chatInput.trim()?"pointer":"not-allowed",color:chatInput.trim()?"#0a0a0f":"#2a2a3a",fontSize:12,fontFamily:"monospace",fontWeight:700,flexShrink:0}}>Send</button>
            </div>
          </Sec>
        )}

        {/* ── HISTORY TAB ── */}
        {activeTab==="history"&&(
          <Sec title={"🗂 "+T.historyTitle}>
            {savedEstimates.length===0
              ?<div style={{textAlign:"center",color:"#3a3040",fontFamily:"monospace",fontSize:12,padding:40}}>{T.historyEmpty}</div>
              :savedEstimates.map(est=>(
                <div key={est.id} style={{background:"rgba(255,255,255,0.02)",border:"1px solid rgba(245,166,35,0.15)",borderRadius:10,padding:16,marginBottom:10}}>
                  <div style={{display:"flex",justifyContent:"space-between",alignItems:"flex-start",marginBottom:8}}>
                    <div>
                      <div style={{fontSize:13,fontWeight:600,color:"#e8e0d0"}}>{est.clientName||est.region}</div>
                      <div style={{fontSize:10,color:"#5a5555",fontFamily:"monospace"}}>{est.date} · {est.region}</div>
                    </div>
                    <div style={{fontSize:14,fontWeight:700,color:"#f5a623",fontFamily:"monospace"}}>{est.pricingMode==="tm"?fmt(est.tmTotal):`${fmt(est.totalLow)}–${fmt(est.totalHigh)}`}</div>
                  </div>
                  <div style={{fontSize:11,color:"#4a4a4a",marginBottom:10}}>{est.lineItems.filter(i=>i.qty).map(i=>`${i.label} ×${i.qty}`).join(" · ")}</div>
                  <div style={{display:"flex",gap:8}}>
                    <button onClick={()=>loadEstimate(est)} style={{background:"rgba(245,166,35,0.1)",border:"1px solid rgba(245,166,35,0.28)",borderRadius:6,padding:"6px 12px",cursor:"pointer",color:"#f5a623",fontSize:10,fontFamily:"monospace"}}>Load</button>
                    <button onClick={()=>deleteEstimate(est.id)} style={{background:"rgba(224,85,85,0.08)",border:"1px solid rgba(224,85,85,0.22)",borderRadius:6,padding:"6px 12px",cursor:"pointer",color:"#e05555",fontSize:10,fontFamily:"monospace"}}>Delete</button>
                  </div>
                </div>
              ))
            }
          </Sec>
        )}

        {/* ── NEC TAB ── */}
        {activeTab==="nec"&&(
          <div>
            <Sec title="📖 NEC 2023 — Complete Residential Reference">
              <NECReference/>
            </Sec>
          </div>
        )}

        <div style={{textAlign:"center",marginTop:24,paddingTop:14,borderTop:"1px solid rgba(255,255,255,0.04)"}}>
          <p style={{fontSize:10,color:"#1a1020",fontFamily:"monospace",margin:0}}>VoltQuote · {T.disclaimer}</p>
        </div>
      </div>
      <style>{`@keyframes fadeIn{from{opacity:0;transform:translateY(7px)}to{opacity:1;transform:translateY(0)}} select option{background:#1a1a2e} textarea{font-family:Georgia,serif;color:#e8e0d0;} ::-webkit-scrollbar{width:4px} ::-webkit-scrollbar-track{background:#0a0a0f} ::-webkit-scrollbar-thumb{background:#f5a623;border-radius:2px}`}</style>
    </div>
  );
}

function Sec({title,children}){return <div style={{marginBottom:22}}><div style={{fontSize:10,letterSpacing:4,color:"#f5a623",fontFamily:"monospace",textTransform:"uppercase",marginBottom:10}}>{title}</div>{children}</div>;}

const sel={width:"100%",padding:"10px 12px",background:"rgba(255,255,255,0.035)",border:"1px solid rgba(245,166,35,0.25)",borderRadius:8,color:"#e8e0d0",fontSize:13,fontFamily:"Georgia,serif",cursor:"pointer",outline:"none",boxSizing:"border-box"};
const qBtn={width:27,height:27,borderRadius:6,background:"rgba(245,166,35,0.09)",border:"1px solid rgba(245,166,35,0.2)",color:"#f5a623",fontSize:15,cursor:"pointer",display:"flex",alignItems:"center",justifyContent:"center",fontWeight:700,lineHeight:1,flexShrink:0};
const ghostBtn={background:"rgba(255,255,255,0.04)",border:"1px solid rgba(255,255,255,0.1)",borderRadius:7,cursor:"pointer",color:"#6a6055",fontFamily:"monospace",letterSpacing:1};
