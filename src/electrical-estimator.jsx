/* eslint-disable react-hooks/exhaustive-deps */
import { useState, useMemo, useEffect } from "react";
import { supabase, signOut, getQuotes, upsertQuote, deleteQuote as dbDeleteQuote, updateQuoteStatus, getClients, upsertClient, isPro, isTrialing, trialDaysLeft, saveThemePref, isElite } from "./lib/supabase";
import { CATEGORIES, MARKUP_OPTIONS, HOURLY_RATES, ALL_SERVICES, CHECKLISTS } from "./data/catalog";
import { JobCalendar, PhotoAttachments, QuickBooksExport, AutoInvoiceButton, OnMyWayButton, ReviewRequestButton } from "./features";
import AIQuoteBuilder from "./AIQuoteBuilder";
import EliteMode from "./EliteMode";
import ProposalView from "./ProposalView";
import CustomersView from "./CustomersView";
import { THEMES, applyTheme, getSavedTheme, saveTheme } from "./themes";
import MaterialsListView from "./MaterialsListView";
import { WirewayMark, WirewayLogo } from "./Logo";
import { WelcomeHero, SetupChecklist, getOnboardState, setOnboardState } from "./Onboarding";
import Dashboard from "./Dashboard";
import { Pill, StatCard, CategorySection, NECReference } from "./WiremComponents";
import WiremModals from "./WiremModals";
import LoadAdvisor from "./LoadAdvisor";
// ── SESSION RESTORE ──────────────────────────────────────────────
// Mobile browsers evict the page when you switch apps or follow a link.
// Snapshot the working state continuously; restore it on reload so the
// app reopens exactly where you left off instead of resetting to the dashboard.
const SESSION_KEY = "wireway_session_v1";
function loadSession() {
  try {
    const raw = window.localStorage.getItem(SESSION_KEY);
    if (!raw) return null;
    const s = JSON.parse(raw);
    if (!s || (Date.now() - (s.at || 0)) > 1000 * 60 * 60 * 12) return null; // 12h freshness
    return s;
  } catch { return null; }
}
function saveSession(s) {
  try { window.localStorage.setItem(SESSION_KEY, JSON.stringify({ ...s, at: Date.now() })); } catch { /* storage blocked */ }
}
export function clearSession() {
  try { window.localStorage.removeItem(SESSION_KEY); } catch { /* ignore */ }
}

export default function Wireway({ user, profile, onProfileUpdate, onShowPricing, paymentBanner, onClearBanner }) {
  const SAVED = (() => {
    const s = loadSession();
    return s && (!s.uid || s.uid === user?.id) ? s : null;
  })();
  const [entries,        setEntries]        = useState(SAVED?.entries || {});
  const [hourlyRate,     setHourlyRate]     = useState(profile?.hourly_rate || 85);
  const [markup,         setMarkup]         = useState(Number(profile?.default_markup) || 0.30);
  const [clientName,     setClientName]     = useState(SAVED?.clientName || "");
  const [clientEmail,    setClientEmail]    = useState(SAVED?.clientEmail || "");
  const [clientPhone,    setClientPhone]    = useState(SAVED?.clientPhone || "");
  const [jobName,        setJobName]        = useState(SAVED?.jobName || "");
  const [notes,          setNotes]          = useState(SAVED?.notes || "");
  const [tab,            setTab]            = useState(SAVED?.tab || "services");
  const [showMaterials,  setShowMaterials]  = useState(true);
  const [clientBuysAll,  setClientBuysAll]  = useState(false);
  const [copied,         setCopied]         = useState(false);
  const [quoteNumber,    setQuoteNumber]    = useState(SAVED?.quoteNumber || "");
  const [quoteId,        setQuoteId]        = useState(SAVED?.quoteId || null);
  const [signModal,      setSignModal]      = useState(false);
  const [sigName,        setSigName]        = useState("");
  const [sigDate,        setSigDate]        = useState("");
  const [sigSaved,       setSigSaved]       = useState(false);
  const [savedQuotes,    setSavedQuotes]    = useState([]);
  const [saveMsg,        setSaveMsg]        = useState("");
  const [customItems,    setCustomItems]    = useState(SAVED?.customItems || []);
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
  const [depositOnly,    setDepositOnly]    = useState(SAVED ? !!SAVED.depositOnly : true);
  const [depositPercent, setDepositPercent] = useState(SAVED?.depositPercent || 50);
  const [paymentLoading, setPaymentLoading] = useState(false);
  const [paymentError,   setPaymentError]   = useState("");
  const [paymentSuccess, setPaymentSuccess] = useState(!!paymentBanner);
  const [showAccount,    setShowAccount]    = useState(false);
  const [showCalendar,   setShowCalendar]   = useState(false);
  const [showAIBuilder,  setShowAIBuilder]  = useState(false);
  const [showLoadAdvisor, setShowLoadAdvisor] = useState(false);
  const [showElite,      setShowElite]      = useState(false);
  const [onboard,        setOnboard]        = useState(getOnboardState());
  const [aiSeed,         setAiSeed]         = useState("");
  const [showProposal,   setShowProposal]   = useState(false);
  const [showPullList,   setShowPullList]   = useState(SAVED ? !!SAVED.showPullList : false);
  const [showCustomers,  setShowCustomers]  = useState(false);
  const [showDashboard,  setShowDashboard]  = useState(SAVED ? !!SAVED.showDashboard : true);
  const [theme,          setTheme]          = useState(() => profile?.theme || getSavedTheme());
  useEffect(() => { applyTheme(theme); }, [theme]);
  useEffect(() => {
    saveSession({ uid: user?.id, showDashboard, tab, entries, customItems, clientName, clientEmail, clientPhone, jobName, notes, quoteNumber, quoteId, depositOnly, depositPercent, showPullList });
  }, [showDashboard, tab, entries, customItems, clientName, clientEmail, clientPhone, jobName, notes, quoteNumber, quoteId, depositOnly, depositPercent, showPullList]);
  const [proGateMsg,     setProGateMsg]     = useState("");

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

  const userIsPro = isPro(profile);
  const daysLeft  = trialDaysLeft(profile);
  const onTrial   = isTrialing(profile);

  const requirePro = (action) => {
    if (userIsPro) { action(); return; }
    setProGateMsg("This feature requires Wireway Pro. Upgrade to unlock.");
    setTimeout(() => setProGateMsg(""), 3500);
    if (onShowPricing) setTimeout(() => onShowPricing(), 400);
  };

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


  const genQuoteNum = () => {
    const yr  = new Date().getFullYear();
    const seq = (savedQuotes.length + 1).toString().padStart(3, "0");
    return `WW-${yr}-${seq}`;
  };

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
    return data?.id;
  };

  const loadQuote = async (q) => {
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

  const deleteQuote = async (id) => {
    if (user?.id) await dbDeleteQuote(id, user.id);
    setSavedQuotes(prev => prev.filter(q => q.id !== id));
  };

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

  const [company, setCompany] = useState(() => {
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
    try { localStorage.setItem("wireway_company", JSON.stringify(saved)); } catch {}
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
    if (user?.id) {
      const { uploadLogo } = await import("./lib/supabase");
      const { url } = await uploadLogo(user.id, file);
      if (url) { setLogoDataUrl(url); return; }
    }
    const reader = new FileReader();
    reader.onload = (ev) => setLogoDataUrl(ev.target.result);
    reader.readAsDataURL(file);
  };

  const applyAIEstimate = (items) => {
    const newEntries = { ...entries };
    items.forEach(item => {
      newEntries[item.id] = {
        qty:        item.qty,
        variantIdx: item.variantIdx,
        clientBuys: !!item.clientBuys,
      };
    });
    setEntries(newEntries);
    setShowAIBuilder(false);
    setTab("summary");
    setSaveMsg(`${items.length} services added by AI`);
    setTimeout(() => setSaveMsg(""), 3000);
  };

  const newQuote = () => {
    if (hasItems && !window.confirm("Start a new quote? Your current estimate will be cleared.")) return;
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

  // Catalog labor dollars are authored at this base hourly rate; we scale them
  // by the user's chosen rate so the totals stay reactive when the rate changes.
  const BASE_HOURLY = 85;

  const { activeItems, totMat, totLab, totHrs, totClientBuysMat } = useMemo(() => {
    const items = ALL_SERVICES.filter(s => entries[s.id]?.qty > 0).map(s => {
      const e = entries[s.id];
      const v = s.variants[e.variantIdx ?? 0];
      const qty = e.qty;
      const cBuys = e.clientBuys ?? clientBuysAll;
      const mat  = s.materialCost * v.m * qty;
      const lab  = Math.round(s.laborCost * v.m * qty * (hourlyRate / BASE_HOURLY));
      const hrs  = s.laborHours   * v.m * qty;
      return { ...s, qty, variantLabel: v.label, mat, lab, hrs, cBuys, lineTotal: cBuys ? lab : mat + lab };
    });
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
  }, [entries, clientBuysAll, customItems, hourlyRate]);

  const subtotal   = totMat + totLab;
  const markupAmt  = subtotal * markup;
  const taxAmt     = taxEnabled ? totMat * taxRate : 0;
  const total      = subtotal + markupAmt + taxAmt;
  const hasItems   = activeItems.length > 0;

  const grossProfit   = markupAmt + taxAmt;
  const marginPct     = total > 0 ? (grossProfit / total * 100).toFixed(1) : "0";
  const laborPct      = total > 0 ? (totLab / total * 100).toFixed(1) : "0";
  const effectiveRate = totHrs > 0 ? (total / totHrs).toFixed(0) : "0";

  const requestPayment = async (mode = "open") => {
    if (!hasItems || paymentLoading) return;
    if (!profile?.stripe_charges_enabled) {
      setPaymentError("Connect your Stripe account in ⚙ Company Settings first.");
      return;
    }
    setPaymentLoading(true);
    setPaymentError("");
    try {
      // Make sure the quote is saved so the server can charge against its stored amount
      let id = quoteId;
      if (!id) { id = await saveQuote(); }
      if (!id) { setPaymentError("Save the quote first, then request payment."); setPaymentLoading(false); return; }

      const { data: { session } } = await supabase.auth.getSession();
      const token = session?.access_token;
      if (!token) { setPaymentError("Please sign in again."); setPaymentLoading(false); return; }

      const res = await fetch("/api/create-checkout", {
        method: "POST",
        headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
        body: JSON.stringify({ quoteId: id, depositOnly, depositPercent }),
      });
      const data = await res.json();
      if (data.url) {
        if (mode === "sms") {
          const msg = `Hi ${clientName || "there"}, here's your secure payment link for ${jobName || quoteNumber || "your project"}: ${data.url} — ${company.name || "Wireway Pro"}`;
          const sep = /iPhone|iPad|iPod/i.test(navigator.userAgent) ? "?&" : "?";
          window.location.href = `sms:${(clientPhone || "").replace(/[^+\d]/g, "")}${sep}body=${encodeURIComponent(msg)}`;
        } else if (mode === "copy") {
          try { await navigator.clipboard.writeText(data.url); setSaveMsg("Payment link copied — paste it anywhere"); setTimeout(() => setSaveMsg(""), 3000); }
          catch { window.prompt("Copy this payment link:", data.url); }
        } else {
          window.open(data.url, "_blank");
        }
      } else {
        setPaymentError(data.error || "Could not create checkout session.");
      }
    } catch (err) {
      setPaymentError("Network error — check your connection and try again.");
    } finally {
      setPaymentLoading(false);
    }
  };

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
      "Generated by Wireway Pro · NEC 2023 Professional Estimating · wirewaypro.com",
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
      `\n\nTOTAL: $${total.toLocaleString()}\n\nCall us: ${company.phone || ""}\nwirewaypro.com`
    );
    const to = clientPhone ? clientPhone.replace(/\D/g, "") : "";
    window.open(`sms:${to}?body=${body}`);
  };

  const buildMaterialList = () => {
    const lines = [
      "WIREWAY PRO — MATERIAL PULL LIST",
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
    <button onClick={() => setTab(id)} style={{ flex:1, padding:"9px 6px", border:"none", cursor:"pointer", fontFamily:"'DM Sans',sans-serif", fontSize:11, fontWeight:600, letterSpacing:"0", transition:"all 0.2s", background: tab===id ? "rgba(var(--accent-rgb),0.08)" : "transparent", color: tab===id ? "var(--accent)" : "rgba(255,255,255,0.32)", borderBottom: tab===id ? "2px solid var(--accent)" : "2px solid transparent", whiteSpace:"nowrap" }}>{lbl}</button>
  );

  const inputStyle = { background:"var(--card)", border:"1px solid var(--line)", borderRadius:7, padding:"8px 11px", fontSize:13, color:"#fff", fontFamily:"inherit", width:"100%", transition:"border-color 0.15s" };
  const focusGold = e => e.target.style.borderColor = "rgba(var(--accent-rgb),0.4)";
  const blurGray  = e => e.target.style.borderColor = "rgba(255,255,255,0.07)";

  // ── DASHBOARD HOME (early return) ──
  if (showDashboard) {
    return (
      <>
        <style>{`
          *{box-sizing:border-box;margin:0;padding:0}
          @keyframes fadeUp{from{opacity:0;transform:translateY(12px)}to{opacity:1;transform:translateY(0)}}
        `}</style>
        <div style={{ minHeight:"100vh", background:"var(--bg-scene)", fontFamily:"'DM Sans',sans-serif", color:"#fff" }}>
          <div style={{ borderBottom:"1px solid var(--line)", background:"rgba(10,10,12,0.9)", backdropFilter:"blur(20px)", WebkitBackdropFilter:"blur(20px)", position:"sticky", top:0, zIndex:100, padding:"0 20px" }}>
            <div style={{ maxWidth:680, margin:"0 auto", height:54, display:"flex", alignItems:"center", justifyContent:"space-between" }}>
              <div style={{ display:"flex", alignItems:"center", gap:10 }}>
                <WirewayLogo size={34} fontSize={16} tagline />
              </div>
              <div style={{ display:"flex", gap:6 }}>
                <button onClick={() => setShowCustomers(true)} title="Customers" style={{ padding:"6px 11px", borderRadius:7, border:"1px solid var(--line-strong)", background:"transparent", color:"rgba(255,255,255,0.5)", fontSize:13, cursor:"pointer", fontFamily:"inherit" }}>👥</button>
                <button onClick={() => { newQuote(true); setShowDashboard(false); }} style={{ padding:"6px 11px", borderRadius:7, background:"rgba(var(--accent-rgb),0.1)", border:"1px solid rgba(var(--accent-rgb),0.3)", color:"var(--accent)", fontSize:11, fontWeight:700, cursor:"pointer", fontFamily:"inherit", whiteSpace:"nowrap" }}>+ New</button>
                <button onClick={() => setShowAccount(true)} title="Account" style={{ padding:"6px 10px", borderRadius:7, border:"1px solid var(--line-strong)", background:"transparent", color:"rgba(255,255,255,0.5)", fontSize:13, cursor:"pointer", fontFamily:"inherit" }}>⚙</button>
              </div>
            </div>
          </div>
          <div style={{ maxWidth:680, margin:"0 auto", padding:"20px 20px 60px" }}>
            {showElite && <EliteMode profile={profile} onClose={() => setShowElite(false)} />}
            {isElite(profile) && (
              <button onClick={() => setShowElite(true)} style={{ width:"100%", marginBottom:16, padding:"13px 16px", borderRadius:12, background:"linear-gradient(135deg, rgba(240,168,24,0.14), rgba(240,168,24,0.04))", border:"1px solid rgba(240,168,24,0.45)", color:"#f0a818", fontSize:13, fontWeight:800, cursor:"pointer", fontFamily:"inherit", letterSpacing:"0.04em", textAlign:"left", display:"flex", alignItems:"center", gap:10 }}>
                <span style={{ fontSize:16 }}>⚙</span>
                <span style={{ flex:1 }}>WIREWAY ELITE — Industrial Estimator</span>
                <span style={{ fontSize:14 }}>→</span>
              </button>
            )}
            {savedQuotes.length === 0 && !onboard.heroDone && (
              <WelcomeHero
                onStartAI={() => {
                  setOnboardState({ heroDone: true }); setOnboard(o => ({ ...o, heroDone: true }));
                  setAiSeed("Finished basement: add 8 recessed lights on a dimmer, 6 receptacles on a new 20A circuit, and a smoke/CO detector. Drywall is already up. Homeowner is supplying the light trims.");
                  setShowDashboard(false); setTimeout(() => setShowAIBuilder(true), 100);
                }}
                onDismiss={() => { setOnboardState({ heroDone: true }); setOnboard(o => ({ ...o, heroDone: true })); }}
              />
            )}
            {!onboard.listDone && (savedQuotes.length > 0 || onboard.heroDone) && (() => {
              const items = [
                { done: savedQuotes.length > 0, label: "Build your first estimate", hint: "Describe a job — AI does the rest",
                  onClick: () => { setShowDashboard(false); setTimeout(() => setShowAIBuilder(true), 100); } },
                { done: !!company.name, label: "Add your company info", hint: "Your name and logo go on every proposal",
                  onClick: () => { setShowDashboard(false); setTimeout(() => setShowAccount(true), 100); } },
                { done: clients.length > 0, label: "Save a customer", hint: "Client details auto-fill future quotes",
                  onClick: () => setShowCustomers(true) },
                { done: savedQuotes.some(q => ["accepted","paid","deposit_paid"].includes(q.status)), label: "Get a quote signed or paid", hint: "Send a proposal and text the pay link",
                  onClick: () => { if (savedQuotes[0]) { loadQuote(savedQuotes[0]); setShowDashboard(false); setTab("summary"); } else { setShowDashboard(false); setTimeout(() => setShowAIBuilder(true), 100); } } },
              ];
              return items.every(i => i.done) ? null : (
                <SetupChecklist items={items} onDismiss={() => { setOnboardState({ listDone: true }); setOnboard(o => ({ ...o, listDone: true })); }} />
              );
            })()}
            <Dashboard
              user={user} profile={profile}
              onNewQuote={(job) => { newQuote(true); setJobName(job.label); setShowDashboard(false); setTab("services"); }}
              onLoadQuote={(q) => { loadQuote(q); setShowDashboard(false); }}
              onShowAI={() => { setShowDashboard(false); setTimeout(() => setShowAIBuilder(true), 100); }}
              onOpenCalendar={() => { setShowDashboard(false); setTimeout(() => setShowCalendar(true), 100); }}
            />
          </div>
        </div>
        {showCustomers && (
          <CustomersView
            user={user} onClientsChange={setClients}
            clients={clients} savedQuotes={savedQuotes}
            onLoadQuote={(q) => { loadQuote(q); setShowCustomers(false); setShowDashboard(false); }}
            onNewEstimate={(cl) => { newQuote(true); setClientName(cl.name || ""); setClientEmail(cl.email || ""); setClientPhone(cl.phone || ""); setShowCustomers(false); setShowDashboard(false); setTab("services"); }}
            onClose={() => setShowCustomers(false)}
          />
        )}
        {showAccount && (
          <div onClick={e => e.target === e.currentTarget && setShowAccount(false)} style={{ position:"fixed", inset:0, background:"rgba(0,0,0,0.75)", backdropFilter:"blur(8px)", WebkitBackdropFilter:"blur(8px)", zIndex:400, display:"flex", alignItems:"center", justifyContent:"center", padding:"24px 16px" }}>
          <div style={{ background:"var(--surface)", border:"1px solid var(--line-strong)", borderRadius:18, width:"100%", maxWidth:380, padding:"24px", fontFamily:"'DM Sans',sans-serif", color:"#fff" }}>
            <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:14 }}>
              <span style={{ fontFamily:"'Syne',sans-serif", fontSize:16, fontWeight:800 }}>Account</span>
              <button onClick={() => setShowAccount(false)} style={{ background:"transparent", border:"none", color:"rgba(255,255,255,0.5)", fontSize:20, cursor:"pointer" }}>✕</button>
            </div>
            <div style={{ fontSize:12, color:"rgba(255,255,255,0.5)", marginBottom:16 }}>{user?.email}</div>

            <div style={{ fontSize:10, color:"rgba(255,255,255,0.5)", textTransform:"uppercase", letterSpacing:"0.1em", marginBottom:8 }}>Appearance</div>
            <div style={{ marginBottom:16 }}>
              {THEMES.map(t => {
                const locked = !t.free && !userIsPro;
                const active = theme === t.id;
                return (
                  <div key={t.id}
                    onClick={() => {
                      if (locked) { setShowAccount(false); if (onShowPricing) onShowPricing(); return; }
                      setTheme(t.id); saveTheme(t.id);
                      if (user?.id) saveThemePref(user.id, t.id);
                    }}
                    style={{ display:"flex", alignItems:"center", gap:11, padding:"9px 11px", marginBottom:5, borderRadius:10, cursor:"pointer",
                      border: active ? `1px solid ${t.accent}` : "1px solid var(--line)",
                      background: active ? `rgba(${t.accentRgb},0.08)` : "rgba(255,255,255,0.02)",
                      opacity: locked ? 0.55 : 1, transition:"all 0.15s" }}>
                    <span style={{ width:22, height:22, borderRadius:7, flexShrink:0, background:`linear-gradient(135deg, ${t.accent}, rgba(${t.accentRgb},0.4))`, border:"1px solid var(--line-strong)" }} />
                    <span style={{ flex:1, minWidth:0 }}>
                      <span style={{ display:"block", fontSize:12.5, fontWeight:700, color: active ? t.accent : "rgba(255,255,255,0.8)" }}>{t.name}</span>
                      <span style={{ display:"block", fontSize:10, color:"rgba(255,255,255,0.5)" }}>{t.desc}</span>
                    </span>
                    {locked ? <span style={{ fontSize:11 }}>🔒</span> : active ? <span style={{ fontSize:11, color:t.accent }}>✓</span> : null}
                  </div>
                );
              })}
              {!userIsPro && (
                <div style={{ fontSize:10, color:"rgba(255,255,255,0.5)", textAlign:"center", marginTop:4 }}>
                  Themes unlock with Pro
                </div>
              )}
            </div>
            {onShowPricing && profile?.subscription_status !== "active" && (
              <button onClick={onShowPricing} style={{ width:"100%", padding:"12px", marginBottom:8, background:"rgba(var(--accent-rgb),0.1)", border:"1px solid rgba(var(--accent-rgb),0.3)", borderRadius:10, color:"var(--accent)", fontSize:13, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>⚡ Upgrade to Pro</button>
            )}
            <button onClick={async () => { await signOut(); window.location.reload(); }} style={{ width:"100%", padding:"12px", background:"rgba(232,126,126,0.06)", border:"1px solid rgba(232,126,126,0.2)", borderRadius:10, color:"#e87e7e", fontSize:13, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>Sign Out</button>
          </div>
          </div>
        )}
      </>
    );
  }

  return (
    <>
      <style>{`
        *{box-sizing:border-box;margin:0;padding:0}
        body{background:var(--bg0)}
        ::-webkit-scrollbar{width:3px}
        ::-webkit-scrollbar-thumb{background:rgba(255,255,255,0.1);border-radius:3px}
        input,textarea,select{outline:none}
        input::placeholder,textarea::placeholder{color:rgba(255,255,255,0.18)}
        select option{background:#1a1a1e}
        @keyframes fadeUp{from{opacity:0;transform:translateY(10px)}to{opacity:1;transform:translateY(0)}}
        @media(max-width:480px){.hide-xs{display:none!important}}
        @keyframes modalIn{from{opacity:0;transform:scale(0.96) translateY(12px)}to{opacity:1;transform:scale(1) translateY(0)}}
        .modal-overlay{position:fixed;inset:0;background:rgba(0,0,0,0.75);backdrop-filter:blur(8px);z-index:200;display:flex;align-items:flex-start;justify-content:center;overflow-y:auto;padding:24px 16px}
        .modal-box{background:linear-gradient(var(--surface,#15151b),var(--surface,#15151b)),#0d0d11;border:1px solid var(--line-strong);border-radius:18px;width:100%;max-width:600px;animation:modalIn 0.25s ease both;margin:auto}
        @media print{.no-print{display:none!important}.print-quote{background:#fff!important;color:#000!important;padding:32px!important}}

        /* ── interaction polish: consistent, tactile, accessible ── */
        html{scroll-behavior:smooth}
        body{-webkit-font-smoothing:antialiased;-moz-osx-font-smoothing:grayscale;text-rendering:optimizeLegibility}
        button,[role="button"],a,label{-webkit-tap-highlight-color:transparent}
        button,[role="button"]{transition:transform .12s cubic-bezier(.2,.7,.2,1),background-color .18s ease,border-color .18s ease,color .18s ease,box-shadow .18s ease,opacity .18s ease}
        button:not(:disabled):active,[role="button"]:active{transform:scale(.975)}
        button:focus-visible,a:focus-visible,[tabindex]:focus-visible{outline:2px solid var(--accent);outline-offset:2px}
        ::selection{background:rgba(var(--accent-rgb),0.28);color:#fff}
        @media(prefers-reduced-motion:reduce){*,*::before,*::after{animation-duration:.001ms!important;animation-iteration-count:1!important;transition-duration:.001ms!important;scroll-behavior:auto!important}}
      `}</style>

      <div style={{ minHeight:"100vh", background:"var(--bg-scene)", fontFamily:"'DM Sans',sans-serif", color:"#fff", paddingBottom:80 }}>

        {/* ── HEADER ── */}
        <div style={{ borderBottom:"1px solid var(--line)", background:"rgba(10,10,12,0.88)", backdropFilter:"blur(20px)", WebkitBackdropFilter:"blur(20px)", position:"sticky", top:0, zIndex:100, padding:"0 20px" }} className="no-print">
          <div style={{ maxWidth:800, margin:"0 auto", height:54, display:"flex", alignItems:"center", justifyContent:"space-between" }}>
            <div style={{ display:"flex", alignItems:"center", gap:9, minWidth:0, flex:1 }}>
              {logoDataUrl
                ? <img src={logoDataUrl} alt="logo" style={{ height:32, width:"auto", borderRadius:6, objectFit:"contain" }} />
                : <WirewayMark size={32} />
              }
              <span style={{ fontFamily:"'Syne',sans-serif", fontSize:16, fontWeight:800, letterSpacing:"-0.03em", whiteSpace:"nowrap", overflow:"hidden", textOverflow:"ellipsis", maxWidth:"32vw" }}>{company.name || "Wireway Pro"}</span>
              <span className="hide-xs" style={{ fontSize:10, fontWeight:700, color:"rgba(var(--accent-rgb),0.6)", background:"rgba(var(--accent-rgb),0.07)", border:"1px solid rgba(var(--accent-rgb),0.16)", padding:"1px 5px", borderRadius:3, letterSpacing:"0.08em", textTransform:"uppercase" }}>NEC 2023</span>
            </div>
            <div style={{ display:"flex", alignItems:"center", gap:6 }}>
              {hasItems && (
                <span style={{ fontFamily:"'DM Mono',monospace", fontSize:16, fontWeight:600, color:"var(--accent)", letterSpacing:"-0.02em" }}>
                  ${total.toLocaleString()}
                </span>
            )}
              {onShowPricing && profile?.subscription_status !== "active" && (
                <button onClick={onShowPricing} style={{ padding:"5px 11px", borderRadius:6, background:"linear-gradient(135deg,rgba(var(--accent-rgb),0.18),rgba(var(--accent-rgb),0.06))", border:"1px solid rgba(var(--accent-rgb),0.3)", color:"var(--accent)", fontSize:10, fontWeight:700, cursor:"pointer", fontFamily:"inherit", whiteSpace:"nowrap" }}>
                  ⚡ {onTrial && daysLeft > 0 ? `${daysLeft}d left` : "Upgrade"}
                </button>
            )}
              <button onClick={newQuote} style={{ padding:"5px 11px", borderRadius:6, border:"1px solid var(--line-strong)", background:"transparent", color:"rgba(255,255,255,0.5)", fontSize:11, fontWeight:600, cursor:"pointer", fontFamily:"inherit" }}>
                + New </button>
              <button onClick={() => setShowDashboard(true)} title="Home" style={{ padding:"5px 9px", borderRadius:6, border:"1px solid var(--line-strong)", background:"transparent", color:"rgba(255,255,255,0.5)", fontSize:13, cursor:"pointer" }}>
                🏠 </button>
              {/* Combined account + company menu */}
              <div style={{ display:"flex", gap:1, background:"var(--card)", border:"1px solid var(--line-strong)", borderRadius:7, overflow:"hidden" }}>
                <button onClick={() => setShowAccount(true)} style={{ padding:"5px 10px", border:"none", background:"transparent", color:"rgba(255,255,255,0.5)", fontSize:11, fontWeight:600, cursor:"pointer", fontFamily:"inherit", borderRight:"1px solid var(--line)" }}>
                  Account </button>
                <button onClick={() => { setCompanyDraft(company); setLogoDataUrl(company.logoDataUrl||""); setEditingCompany(true); }} style={{ padding:"5px 10px", border:"none", background:"transparent", color:"rgba(255,255,255,0.5)", fontSize:11, fontWeight:600, cursor:"pointer", fontFamily:"inherit" }}>
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
                    {paymentBanner === "pro" ? "🎉 Wireway Pro is now active." : "✓ Payment received — quote marked paid."} </span>
                  <button onClick={() => { setPaymentSuccess(false); if(onClearBanner) onClearBanner(); }} style={{ background:"transparent", border:"none", color:"rgba(100,220,130,0.4)", fontSize:16, cursor:"pointer", padding:"0 4px" }}>✕</button>
                </div>
                )}
              {proGateMsg && (
                <div style={{ padding:"9px 14px", background:"rgba(var(--accent-rgb),0.06)", border:"1px solid rgba(var(--accent-rgb),0.2)", borderRadius:9, fontSize:11, color:"rgba(var(--accent-rgb),0.9)", fontWeight:600 }}>
                  ⚡ {proGateMsg} </div>
                  )}
              {onTrial && daysLeft <= 30 && daysLeft > 0 && (
                <div style={{ padding:"9px 14px", background:"rgba(var(--accent-rgb),0.05)", border:"1px solid rgba(var(--accent-rgb),0.15)", borderRadius:9, display:"flex", justifyContent:"space-between", alignItems:"center" }}>
                  <span style={{ fontSize:11, color:"rgba(var(--accent-rgb),0.75)" }}>⏳ {daysLeft} day{daysLeft!==1?"s":""} remaining in your trial.</span>
                  {onShowPricing && <button onClick={onShowPricing} style={{ padding:"4px 10px", borderRadius:5, border:"1px solid rgba(var(--accent-rgb),0.35)", background:"rgba(var(--accent-rgb),0.1)", color:"var(--accent)", fontSize:10, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>Upgrade</button>}
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
          <div style={{ background:"var(--card)", border:"1px solid var(--line)", borderRadius:12, padding:"12px 14px", marginBottom:10, animation:"fadeUp 0.4s ease 0.06s both" }} className="no-print">
            <div style={{ display:"flex", gap:16, flexWrap:"wrap" }}>
              <div style={{ flex:1, minWidth:200 }}>
                <div style={{ fontSize:10, color:"rgba(255,255,255,0.5)", marginBottom:7 }}>
                  Hourly — <span style={{ color:"var(--accent)", fontFamily:"'DM Mono',monospace", fontWeight:700 }}>${hourlyRate}/hr</span>
                </div>
                <div style={{ display:"flex", flexWrap:"wrap", gap:4 }}>
                  {HOURLY_RATES.map(r => <Pill key={r} label={`$${r}`} active={r===hourlyRate} onClick={() => setHourlyRate(r)} />)}
                </div>
                </div>
              <div style={{ flex:1, minWidth:150 }}>
                <div style={{ fontSize:10, color:"rgba(255,255,255,0.5)", marginBottom:7 }}>
                  Markup — <span style={{ color:"var(--accent)", fontFamily:"'DM Mono',monospace", fontWeight:700 }}>{(markup*100).toFixed(0)}%</span>
                </div>
                <div style={{ display:"flex", flexWrap:"wrap", gap:4 }}>
                  {MARKUP_OPTIONS.map(m => <Pill key={m.v} label={m.label} active={m.v===markup} onClick={() => setMarkup(m.v)} />)}
                </div>
                </div>
                </div>
                </div>

          {/* ── UNIFIED SETTINGS + TOOLS PANEL ── */}
          <div style={{ background:"var(--card)", border:"1px solid var(--line)", borderRadius:12, padding:"12px 14px", marginBottom:12, animation:"fadeUp 0.4s ease 0.08s both" }} className="no-print">

            {/* Row 1: toggles */}
            <div style={{ display:"flex", gap:5, flexWrap:"wrap", marginBottom:10 }}>
              {[
                { label: showMaterials ? "Mat · On" : "Mat · Off",     active: showMaterials,  action: () => setShowMaterials(v=>!v),  color:"var(--accent)" },
                { label: clientBuysAll ? "Client buys" : "You supply", active: clientBuysAll,  action: () => setClientBuysAll(v=>!v), color:"#7ec8e8" },
                { label: flatRateMode  ? "Flat rate" : "Itemized",     active: flatRateMode,   action: () => setFlatRateMode(v=>!v),  color:"#a8e87e" },
                { label: invoiceMode   ? "Invoice" : "Estimate",       active: invoiceMode,    action: () => setInvoiceMode(v=>!v),   color:"#b87ee8" },
                { label: taxEnabled    ? `Tax ${(taxRate*100).toFixed(0)}%` : "Add tax", active: taxEnabled, action: () => setTaxEnabled(v=>!v), color:"#e8b87e" },
              ].map(t => (
                <button key={t.label} onClick={t.action} style={{ padding:"4px 10px", borderRadius:6, fontSize:10, fontWeight:700, border: t.active ? `1px solid ${t.color}40` : "1px solid var(--line)", background: t.active ? `${t.color}12` : "transparent", color: t.active ? t.color : "rgba(255,255,255,0.35)", cursor:"pointer", fontFamily:"inherit", transition:"all 0.15s" }}>
                  {t.label} </button>
              ))}
              </div>

            {/* Conditional: tax rate or invoice due */}
            {(taxEnabled || invoiceMode) && (
              <div style={{ display:"flex", gap:10, flexWrap:"wrap", marginBottom:10, paddingTop:8, borderTop:"1px solid var(--line)" }}>
                {taxEnabled && (
                  <div style={{ display:"flex", alignItems:"center", gap:5 }}>
                    <span style={{ fontSize:10, color:"rgba(255,255,255,0.5)" }}>Rate:</span>
                    {[0.05,0.06,0.07,0.08,0.09,0.10].map(r => (
                      <button key={r} onClick={() => setTaxRate(r)} style={{ padding:"3px 7px", borderRadius:4, fontSize:10, fontWeight:700, border: r===taxRate ? "1px solid rgba(232,184,126,0.5)" : "1px solid var(--line)", background: r===taxRate ? "rgba(232,184,126,0.15)" : "transparent", color: r===taxRate ? "#e8b87e" : "rgba(255,255,255,0.3)", cursor:"pointer", fontFamily:"'DM Mono',monospace" }}>{(r*100).toFixed(0)}%</button>
                    ))}
                    </div>
                    )}
                {invoiceMode && (
                  <div style={{ display:"flex", alignItems:"center", gap:6 }}>
                    <span style={{ fontSize:10, color:"rgba(255,255,255,0.5)" }}>Due:</span>
                    <input type="date" value={invoiceDueDate} onChange={e => setInvoiceDueDate(e.target.value)} style={{ ...inputStyle, width:"auto", fontSize:11, padding:"3px 8px", colorScheme:"dark" }} onFocus={focusGold} onBlur={blurGray} />
                    <button onClick={() => setInvoicePaid(v => !v)} style={{ padding:"3px 9px", borderRadius:5, border: invoicePaid ? "1px solid rgba(100,220,130,0.4)" : "1px solid var(--line)", background: invoicePaid ? "rgba(100,220,130,0.1)" : "transparent", color: invoicePaid ? "#7dcea0" : "rgba(255,255,255,0.35)", fontSize:10, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>
                      {invoicePaid ? "✓ Paid" : "Mark paid"}
                    </button>
                  </div>
                    )}
                    </div>
                    )}

            {/* Row 2: tools — subtle divider separates from toggles */}
            <div style={{ display:"flex", gap:5, flexWrap:"wrap", paddingTop:8, borderTop:"1px solid var(--line)" }}>
              {[
                { label:"⚡ AI Quote", action:() => setShowAIBuilder(true),  highlight: true },
              { label:"📅 Calendar", action:() => setShowCalendar(true)   },
              { label:"Wire Calc",  action:() => setWireCalcOpen(true)  },
                { label:"Load Calc",  action:() => setLoadCalcOpen(true)  },
                { label:"⚡ Load Advisor", action:() => setShowLoadAdvisor(true) },
                { label:"Checklist",  action:() => setChecklistOpen(true) },
                { label:"Clients",    action:() => setShowClientDB(true)  },
                { label:"+ Custom",   action:addCustomItem                },
                hasItems ? { label:"Pull List", action:buildMaterialList } : null,
              ].filter(Boolean).map(btn => (
                <button key={btn.label} onClick={btn.action} style={{ padding:"4px 10px", borderRadius:6, fontSize:10, fontWeight: btn.highlight ? 700 : 600, border: btn.highlight ? "1px solid rgba(var(--accent-rgb),0.3)" : "1px solid var(--line)", background: btn.highlight ? "rgba(var(--accent-rgb),0.08)" : "transparent", color: btn.highlight ? "var(--accent)" : "rgba(255,255,255,0.45)", cursor:"pointer", fontFamily:"inherit", transition:"all 0.15s" }}
                  onMouseEnter={e => { e.currentTarget.style.background = btn.highlight ? "rgba(var(--accent-rgb),0.15)" : "rgba(255,255,255,0.06)"; e.currentTarget.style.color = btn.highlight ? "var(--accent)" : "#fff"; }}
                  onMouseLeave={e => { e.currentTarget.style.background = btn.highlight ? "rgba(var(--accent-rgb),0.08)" : "transparent"; e.currentTarget.style.color = btn.highlight ? "var(--accent)" : "rgba(255,255,255,0.45)"; }}>
                  {btn.label} </button>
              ))}
              </div>
              </div>

          {/* Custom line items */}
          {customItems.length > 0 && (
            <div style={{ background:"rgba(var(--accent-rgb),0.04)", border:"1px solid rgba(var(--accent-rgb),0.12)", borderRadius:12, padding:"14px 16px", marginBottom:14 }} className="no-print">
              <div style={{ fontSize:10, color:"rgba(var(--accent-rgb),0.6)", textTransform:"uppercase", letterSpacing:"0.1em", marginBottom:10 }}>Custom Line Items</div>
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
          <div style={{ display:"flex", background:"var(--card)", borderRadius:9, border:"1px solid var(--line)", overflow:"hidden", marginBottom:14, animation:"fadeUp 0.4s ease 0.16s both" }} className="no-print">
            {TAB("services", "Services")}
            {TAB("summary",  hasItems ? `Summary $${total.toLocaleString()}` : "Summary")}
            {TAB("saved",    `Saved (${savedQuotes.length})`)}
            {TAB("profit",   "Profit")}
            {TAB("nec",      "NEC 2023")}
            {onShowPricing && profile?.subscription_status !== "active" && (
              <button onClick={onShowPricing} style={{ padding:"9px 10px", border:"none", background:"linear-gradient(135deg,rgba(var(--accent-rgb),0.18),rgba(var(--accent-rgb),0.06))", color:"var(--accent)", fontSize:11, fontWeight:800, cursor:"pointer", fontFamily:"'Syne',sans-serif", borderLeft:"1px solid rgba(var(--accent-rgb),0.2)", letterSpacing:"-0.01em", whiteSpace:"nowrap" }}>
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
                  <div style={{ fontSize:13, color:"rgba(255,255,255,0.5)", marginBottom:16 }}>No services added yet</div>
                  <button onClick={() => setTab("services")} style={{ padding:"9px 20px", background:"rgba(var(--accent-rgb),0.08)", border:"1px solid rgba(var(--accent-rgb),0.22)", borderRadius:8, color:"var(--accent)", fontSize:12, fontWeight:600, cursor:"pointer", fontFamily:"inherit" }}>
                    Browse Services →
                  </button>
                </div>
              ) : (
                <>
                  {/* ── STAT CARDS ── */}
                  <div style={{ display:"flex", gap:8, flexWrap:"wrap", marginBottom:16 }} className="no-print">
                    <StatCard label="Total Estimate"      value={`$${total.toLocaleString()}`}           color="var(--accent)" />
                    <StatCard label="Labor Only"          value={`$${totLab.toLocaleString()}`}          color="#a8e87e" sub={`${totHrs.toFixed(1)} hrs @ $${hourlyRate}/hr`} />
                    {showMaterials && <StatCard label="Materials (You)"    value={`$${totMat.toLocaleString()}`}          color="#7eb8e8" />}
                    {totClientBuysMat > 0 && <StatCard label="Client Buys (est.)" value={`$${totClientBuysMat.toLocaleString()}`} color="#e87eb8" sub="not charged to you" />}
                  </div>

                  {/* ── QUOTE PREVIEW CARD ── */}
                  <div style={{ background:"rgba(255,255,255,0.022)", border:"1px solid var(--line)", borderRadius:13, padding:"20px", marginBottom:12 }} className="print-quote">
                    {/* Company header */}
                    <div style={{ display:"flex", alignItems:"center", gap:14, marginBottom:16, paddingBottom:16, borderBottom:"1px solid var(--line)" }}>
                      {logoDataUrl
                        ? <img src={logoDataUrl} alt="logo" style={{ height:48, width:"auto", maxWidth:120, objectFit:"contain", borderRadius:6 }} />
                        : <WirewayMark size={44} glow={false} />
                      }
                      <div style={{ flex:1 }}>
                        <div style={{ fontFamily:"'Syne',sans-serif", fontSize:15, fontWeight:800, color:"#fff", letterSpacing:"-0.02em" }}>{company.name || "Your Company Name"}</div>
                        <div style={{ fontSize:11, color:"rgba(255,255,255,0.5)", marginTop:2, lineHeight:1.6 }}>
                          {[company.phone, company.email, company.address, company.license && `Lic: ${company.license}`].filter(Boolean).join("  ·  ")}
                        </div>
                        </div>
                      <div style={{ textAlign:"right", flexShrink:0 }}>
                        {quoteNumber && <div style={{ fontFamily:"'DM Mono',monospace", fontSize:11, fontWeight:700, color:"var(--accent)", marginBottom:3 }}>{quoteNumber}</div>}
                        <div style={{ fontSize:10, color:"rgba(255,255,255,0.5)", textTransform:"uppercase", letterSpacing:"0.08em" }}>Date</div>
                        <div style={{ fontSize:12, color:"rgba(255,255,255,0.6)", fontFamily:"'DM Mono',monospace" }}>{new Date().toLocaleDateString()}</div>
                        {currentQuoteStatus?.status === "accepted" && (
                          <div style={{ marginTop:4, fontSize:10, fontWeight:700, color:"#7dcea0", background:"rgba(100,220,130,0.1)", border:"1px solid rgba(100,220,130,0.25)", padding:"2px 6px", borderRadius:4 }}>✓ ACCEPTED</div>
                        )}
                        </div>
                        </div>

                    {/* Client info */}
                    {(clientName || jobName || clientEmail || clientPhone) && (
                      <div style={{ background:"var(--card)", borderRadius:8, padding:"10px 12px", marginBottom:14, display:"grid", gridTemplateColumns:"1fr 1fr", gap:"4px 16px" }}>
                        {clientName  && <div style={{ fontSize:12 }}><span style={{ color:"rgba(255,255,255,0.5)" }}>Client: </span><span style={{ color:"#fff", fontWeight:600 }}>{clientName}</span>
                      </div>}
                        {jobName     && <div style={{ fontSize:12 }}><span style={{ color:"rgba(255,255,255,0.5)" }}>Job: </span><span style={{ color:"rgba(255,255,255,0.7)" }}>{jobName}</span>
                      </div>}
                        {clientEmail && <div style={{ fontSize:12 }}><span style={{ color:"rgba(255,255,255,0.5)" }}>Email: </span><span style={{ color:"rgba(255,255,255,0.7)" }}>{clientEmail}</span>
                      </div>}
                        {clientPhone && <div style={{ fontSize:12 }}><span style={{ color:"rgba(255,255,255,0.5)" }}>Phone: </span><span style={{ color:"rgba(255,255,255,0.7)" }}>{clientPhone}</span>
                      </div>}
                      </div>
                      )}

                    {/* Services provided */}
                    <div style={{ fontSize:10, color:"rgba(255,255,255,0.5)", textTransform:"uppercase", letterSpacing:"0.1em", marginBottom:10 }}>Services Provided</div>
                    {CATEGORIES.map(cat => {
                      const items = activeItems.filter(i => cat.services.find(s => s.id===i.id));
                      if (!items.length) return null;
                      return (
                        <div key={cat.id} style={{ marginBottom:12 }}>
                          <div style={{ fontSize:10, color:cat.color, textTransform:"uppercase", letterSpacing:"0.1em", fontWeight:700, marginBottom:5, opacity:0.85 }}>{cat.label}</div>
                          {items.map(item => (
                            <div key={item.id} style={{ display:"flex", alignItems:"flex-start", gap:8, padding:"6px 0", borderBottom:"1px solid var(--line)" }}>
                              <div style={{ flex:1 }}>
                                <div style={{ fontSize:12, color:"rgba(255,255,255,0.78)", fontWeight:600 }}>{item.label}</div>
                                <div style={{ fontSize:10, color:"rgba(255,255,255,0.5)", fontFamily:"'DM Mono',monospace", marginTop:1 }}>
                                  {item.variantLabel} · {item.nec} · qty {item.qty}
                                  {item.cBuys ? <span style={{ color:"#7ec8e8", marginLeft:5 }}>· client supplies parts</span> : ""}
                                </div>
                                </div>
                              <div style={{ display:"flex", flexDirection:"column", alignItems:"flex-end", gap:1, flexShrink:0 }}>
                                <span style={{ fontSize:10, color:"rgba(162,220,160,0.75)", fontFamily:"'DM Mono',monospace" }}>lab ${item.lab.toLocaleString()}</span>
                                {showMaterials && !item.cBuys && <span style={{ fontSize:10, color:"rgba(255,255,255,0.5)", fontFamily:"'DM Mono',monospace" }}>mat ${item.mat.toLocaleString()}</span>}
                                <span style={{ fontSize:12, fontWeight:700, color:cat.color, fontFamily:"'DM Mono',monospace" }}>${item.lineTotal.toLocaleString()}</span>
                              </div>
                              </div>
                          ))}
                          </div>
                      );
                    })}

                    {/* Totals */}
                    <div style={{ background:"linear-gradient(135deg,rgba(var(--accent-rgb),0.065) 0%,rgba(255,255,255,0.018) 100%)", border:"1px solid rgba(var(--accent-rgb),0.18)", borderRadius:10, padding:"14px 16px", marginTop:8 }}>
                      {[
                        showMaterials && { label:"Materials (supplied by us)", val:totMat, color:"#7eb8e8" },
                        { label:`Labor — ${totHrs.toFixed(1)} hrs @ $${hourlyRate}/hr`, val:totLab, color:"#a8e87e" },
                        { label:`Markup — ${(markup*100).toFixed(0)}%`, val:markupAmt, color:"rgba(255,255,255,0.5)" },
                      ].filter(Boolean).map(row => (
                        <div key={row.label} style={{ display:"flex", justifyContent:"space-between", padding:"4px 0", fontSize:11 }}>
                          <span style={{ color:"rgba(255,255,255,0.5)" }}>{row.label}</span>
                          <span style={{ fontFamily:"'DM Mono',monospace", color:row.color, fontWeight:600 }}>${row.val.toLocaleString()}</span>
                        </div>
                      ))}
                      {totClientBuysMat > 0 && (
                        <div style={{ display:"flex", justifyContent:"space-between", padding:"4px 0", fontSize:11 }}>
                          <span style={{ color:"rgba(126,200,232,0.45)" }}>Client supplies parts (not charged)</span>
                          <span style={{ fontFamily:"'DM Mono',monospace", color:"rgba(126,200,232,0.45)" }}>~${totClientBuysMat.toLocaleString()}</span>
                        </div>
                        )}
                      <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", paddingTop:12, marginTop:6, borderTop:"1px solid rgba(var(--accent-rgb),0.18)" }}>
                        <span style={{ fontFamily:"'Syne',sans-serif", fontSize:14, fontWeight:800, color:"#fff" }}>Total Estimate</span>
                        <span style={{ fontFamily:"'DM Mono',monospace", fontSize:22, fontWeight:500, color:"var(--accent)", letterSpacing:"-0.03em" }}>${total.toLocaleString()}</span>
                      </div>
                      </div>

                    {/* Notes */}
                    {notes && (
                      <div style={{ marginTop:12, padding:"10px 12px", background:"var(--card)", borderRadius:8, fontSize:11, color:"rgba(255,255,255,0.5)", lineHeight:1.6 }}>
                        <span style={{ color:"rgba(255,255,255,0.5)", textTransform:"uppercase", letterSpacing:"0.08em", fontSize:10 }}>Notes: </span>{notes}
                      </div>
                      )}

                    {/* Terms */}
                    {company.terms && (
                      <div style={{ marginTop:10, padding:"10px 12px", background:"var(--card)", borderRadius:8, fontSize:10, color:"rgba(255,255,255,0.5)", lineHeight:1.7 }}>
                        <div style={{ color:"rgba(255,255,255,0.5)", textTransform:"uppercase", letterSpacing:"0.08em", fontSize:10, marginBottom:4 }}>Terms & Conditions</div>
                        {company.terms} </div>
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
                    <button onClick={() => { if (!userIsPro && savedQuotes.length >= 3) { if(onShowPricing) onShowPricing(); return; } saveQuote(); }} style={{ padding:"12px", background: saveMsg ? "rgba(100,220,130,0.1)" : "linear-gradient(135deg,rgba(var(--accent-rgb),0.18),rgba(var(--accent-rgb),0.07))", border: saveMsg ? "1px solid rgba(100,220,130,0.38)" : "1px solid rgba(var(--accent-rgb),0.35)", borderRadius:10, color: saveMsg ? "#7dcea0" : "var(--accent)", fontSize:13, fontWeight:700, cursor:"pointer", fontFamily:"inherit", transition:"all 0.2s" }}>
                      {saveMsg ? "✓ Saved" : "💾 Save Quote"} </button>
                    <button onClick={() => { setSigName(""); setSigDate(new Date().toLocaleDateString()); setSigSaved(false); setSignModal(true); }}
                      style={{ padding:"12px", background: currentQuoteStatus?.status === "accepted" ? "rgba(100,220,130,0.1)" : "rgba(255,255,255,0.04)", border: currentQuoteStatus?.status === "accepted" ? "1px solid rgba(100,220,130,0.35)" : "1px solid var(--line-strong)", borderRadius:10, color: currentQuoteStatus?.status === "accepted" ? "#7dcea0" : "rgba(255,255,255,0.5)", fontSize:13, fontWeight:700, cursor:"pointer", fontFamily:"inherit", transition:"all 0.2s" }}>
                      {currentQuoteStatus?.status === "accepted" ? "✓ Client Signed" : "✍ Client Signature"}
                    </button>
                  </div>

                  {/* ── SEND ACTIONS ── */}
                  <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr 1fr 1fr", gap:8, marginBottom:8 }} className="no-print">
                    {[
                      { icon:"✉", label:"Email",      desc: clientEmail || "Mail app",    action: emailQuote,  color:"#7eb8e8" },
                      { icon:"💬", label:"Text",       desc: clientPhone || "Messages",    action: smsQuote,    color:"#a8e87e" },
                      { icon:"⎘",  label:"Copy",       desc: copied ? "Copied!" : "Text", action: copyQuote,   color: copied ? "#7dcea0" : "var(--accent)" },
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
                        <span style={{ fontSize:10, color:"rgba(255,255,255,0.5)" }}>{btn.desc}</span>
                      </button>
                    ))}
                    </div>

                  <button onClick={() => setShowProposal(true)} style={{ width:"100%", padding:"13px", background:"linear-gradient(135deg,rgba(var(--accent-rgb),0.18),rgba(var(--accent-rgb),0.06))", border:"1px solid rgba(var(--accent-rgb),0.35)", borderRadius:10, color:"var(--accent)", fontSize:13, fontWeight:700, cursor:"pointer", fontFamily:"inherit", transition:"all 0.2s" }} className="no-print">
                    📄 Generate Proposal (PDF)
                  </button>

                  <button onClick={() => setShowPullList(true)} style={{ width:"100%", padding:"12px", marginTop:6, background:"var(--card)", border:"1px solid var(--line-strong)", borderRadius:10, color:"rgba(255,255,255,0.6)", fontSize:12.5, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }} className="no-print">
                    🧾 Material Pull List & Prices
                  </button>

                  {/* ── ON MY WAY + REVIEW ── */}
                  <div style={{ display:"flex", flexDirection:"column", gap:6, marginTop:2 }} className="no-print">
                    <OnMyWayButton
                      clientName={clientName} clientPhone={clientPhone}
                      jobAddress={jobName} companyName={company.name} />
                    {currentQuoteStatus?.status === "accepted" || currentQuoteStatus?.status === "paid" ? (
                      <ReviewRequestButton
                        clientName={clientName} clientPhone={clientPhone}
                        companyName={company.name} reviewUrl={company.reviewUrl} />
                    ) : null}
                    <AutoInvoiceButton
                      isInvoiceMode={invoiceMode}
                      onConvert={() => { setInvoiceMode(true); setInvoiceDueDate(new Date(Date.now()+30*86400000).toISOString().split("T")[0]); }} />
                    <QuickBooksExport
                      quote={{ clientName, jobName, quoteNumber, invoiceDueDate }}
                      company={company}
                      activeItems={activeItems}
                      total={total} totLab={totLab} totMat={totMat}
                      markupAmt={markupAmt} taxAmt={taxAmt}
                      taxRate={taxRate} taxEnabled={taxEnabled} />
                  </div>

                  {/* ── PHOTO ATTACHMENTS ── */}
                  {quoteId && (
                    <div style={{ marginTop:14, padding:"14px 16px", background:"var(--card)", border:"1px solid var(--line)", borderRadius:12 }} className="no-print">
                      <PhotoAttachments user={user} quoteId={quoteId} />
                    </div>
                  )}

                  {/* ── DEPOSIT + PAYMENT ── */}
                  <div style={{ display:"flex", gap:6, marginBottom:10 }}>
                    <button onClick={() => setDepositOnly(false)} style={{ flex:1, padding:"8px", borderRadius:7, border: !depositOnly ? "1px solid rgba(99,102,241,0.5)" : "1px solid var(--line-strong)", background: !depositOnly ? "rgba(99,102,241,0.12)" : "rgba(255,255,255,0.03)", color: !depositOnly ? "#818cf8" : "rgba(255,255,255,0.4)", fontSize:11, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>
                      Full Amount
                    </button>
                    <button onClick={() => setDepositOnly(true)} style={{ flex:1, padding:"8px", borderRadius:7, border: depositOnly ? "1px solid rgba(99,102,241,0.5)" : "1px solid var(--line-strong)", background: depositOnly ? "rgba(99,102,241,0.12)" : "rgba(255,255,255,0.03)", color: depositOnly ? "#818cf8" : "rgba(255,255,255,0.4)", fontSize:11, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>
                      Deposit Only
                    </button>
                  </div>
                  {depositOnly && (
                    <div style={{ display:"flex", gap:4, marginBottom:10 }}>
                      {[25,50,75,100].map(pct => (
                        <button key={pct} onClick={() => setDepositPercent(pct)} style={{ flex:1, padding:"6px", borderRadius:6, border: depositPercent===pct ? "1px solid rgba(99,102,241,0.5)" : "1px solid var(--line-strong)", background: depositPercent===pct ? "rgba(99,102,241,0.12)" : "rgba(255,255,255,0.03)", color: depositPercent===pct ? "#818cf8" : "rgba(255,255,255,0.4)", fontSize:11, fontWeight:700, cursor:"pointer", fontFamily:"'DM Mono',monospace" }}>
                          {pct}%
                        </button>
                      ))}
                    </div>
                  )}
                  <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", padding:"8px 12px", background:"var(--card)", borderRadius:8, marginBottom:12 }}>
                    <span style={{ fontSize:11, color:"rgba(255,255,255,0.5)" }}>
                      {depositOnly ? `${depositPercent}% deposit` : "Full payment"} to charge:
                    </span>
                    <span style={{ fontFamily:"'DM Mono',monospace", fontSize:16, fontWeight:700, color:"#818cf8" }}>
                      ${depositOnly ? Math.round(total * depositPercent / 100).toLocaleString() : total.toLocaleString()}
                    </span>
                  </div>
                  {paymentError && (
                    <div style={{ fontSize:11, color:"#e87e7e", background:"rgba(232,126,126,0.08)", border:"1px solid rgba(232,126,126,0.2)", borderRadius:7, padding:"8px 10px", marginBottom:10, lineHeight:1.5 }}>
                      ⚠ {paymentError}
                    </div>
                  )}
                  {paymentSuccess && (
                    <div style={{ fontSize:11, color:"#7dcea0", background:"rgba(100,220,130,0.08)", border:"1px solid rgba(100,220,130,0.2)", borderRadius:7, padding:"8px 10px", marginBottom:10 }}>
                      ✓ Payment received! Quote marked as paid.
                    </div>
                  )}
                  {!profile?.stripe_charges_enabled ? (
                    <button onClick={() => setEditingCompany(true)} style={{ width:"100%", padding:"13px", background:"rgba(99,102,241,0.1)", border:"1px solid rgba(99,102,241,0.3)", borderRadius:10, color:"#818cf8", fontSize:12, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>
                      ⚙ Connect Stripe in Company Settings
                    </button>
                  ) : (
                    <button onClick={() => requirePro(requestPayment)} disabled={paymentLoading} style={{ width:"100%", padding:"13px", background: paymentLoading ? "rgba(99,102,241,0.06)" : "linear-gradient(135deg,rgba(99,102,241,0.25),rgba(139,92,246,0.15))", border:"1px solid rgba(99,102,241,0.4)", borderRadius:10, color: paymentLoading ? "rgba(129,140,248,0.5)" : "#818cf8", fontSize:13, fontWeight:700, cursor: paymentLoading ? "default" : "pointer", fontFamily:"inherit", transition:"all 0.2s" }}>
                      {paymentLoading ? "Opening Stripe Checkout..." : `⚡ Send Payment Request — $${depositOnly ? Math.round(total * depositPercent / 100).toLocaleString() : total.toLocaleString()}`}
                    </button>
                  )}
                  {profile?.stripe_charges_enabled && (
                    <div style={{ display:"flex", gap:6, marginTop:8 }}>
                      <button onClick={() => requirePro(() => requestPayment("sms"))} disabled={paymentLoading} style={{ flex:1, padding:"10px", borderRadius:9, background:"rgba(126,200,232,0.07)", border:"1px solid rgba(126,200,232,0.25)", color:"#7ec8e8", fontSize:11.5, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>
                        💬 Text Pay Link
                      </button>
                      <button onClick={() => requirePro(() => requestPayment("copy"))} disabled={paymentLoading} style={{ flex:1, padding:"10px", borderRadius:9, background:"var(--card)", border:"1px solid var(--line-strong)", color:"rgba(255,255,255,0.55)", fontSize:11.5, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>
                        🔗 Copy Pay Link
                      </button>
                    </div>
                  )}
                  <div style={{ textAlign:"center", fontSize:10, color:"rgba(255,255,255,0.5)", marginTop:8 }}>
                    Powered by Stripe · Secure · PCI compliant · Apple Pay & Google Pay supported
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
                <div style={{ marginBottom:14, padding:"14px 16px", background:"linear-gradient(135deg,rgba(var(--accent-rgb),0.08),rgba(99,102,241,0.05))", border:"1px solid rgba(var(--accent-rgb),0.2)", borderRadius:12, display:"flex", justifyContent:"space-between", alignItems:"center", gap:12 }}>
                  <div>
                    <div style={{ fontSize:13, fontWeight:700, color:"var(--accent)", marginBottom:2 }}>Free plan limit reached</div>
                    <div style={{ fontSize:11, color:"rgba(255,255,255,0.5)" }}>You're using {savedQuotes.length}/3 saved quote slots. Upgrade for unlimited.</div>
                  </div>
                  <button onClick={onShowPricing} style={{ padding:"8px 14px", borderRadius:8, border:"1px solid rgba(var(--accent-rgb),0.4)", background:"rgba(var(--accent-rgb),0.12)", color:"var(--accent)", fontSize:11, fontWeight:700, cursor:"pointer", fontFamily:"inherit", flexShrink:0 }}>
                    Upgrade ⚡
                  </button>
                </div>
                  )}
              {savedQuotes.length === 0 ? (
                <div style={{ textAlign:"center", padding:"48px 20px", color:"rgba(255,255,255,0.5)" }}>
                  <div style={{ fontSize:30, marginBottom:10 }}>◎</div>
                  <div style={{ fontSize:13 }}>No saved quotes yet.</div>
                  <div style={{ fontSize:11, marginTop:6, color:"rgba(255,255,255,0.5)" }}>Build an estimate and tap Save Quote.</div>
                  </div>
              ) : (
                <>
                  <div style={{ fontSize:10, color:"rgba(255,255,255,0.5)", textTransform:"uppercase", letterSpacing:"0.1em", marginBottom:12 }}>
                    {savedQuotes.length} saved quote{savedQuotes.length !== 1 ? "s" : ""} </div>
                  {savedQuotes.map(q => (
                    <div key={q.id} style={{
                      background: q.status === "accepted" ? "linear-gradient(135deg,rgba(100,220,130,0.06),rgba(255,255,255,0.02))" : "rgba(255,255,255,0.022)",
                      border: q.status === "accepted" ? "1px solid rgba(100,220,130,0.2)" : "1px solid var(--line)",
                      borderRadius:13, padding:"14px 16px", marginBottom:8,
                      display:"flex", alignItems:"center", gap:12,
                    }}>
                      <div style={{ flex:1, minWidth:0 }}>
                        <div style={{ display:"flex", alignItems:"center", gap:8, marginBottom:4 }}>
                          <span style={{ fontFamily:"'DM Mono',monospace", fontSize:11, fontWeight:700, color:"var(--accent)" }}>{q.quoteNumber}</span>
                          {q.status === "accepted" && (
                            <span style={{ fontSize:10, fontWeight:700, color:"#7dcea0", background:"rgba(100,220,130,0.12)", border:"1px solid rgba(100,220,130,0.25)", padding:"1px 6px", borderRadius:4, letterSpacing:"0.05em" }}>
                              ✓ ACCEPTED
                            </span>
                        )}
                          {q.status === "draft" && (
                            <span style={{ fontSize:10, color:"rgba(255,255,255,0.5)", background:"rgba(255,255,255,0.05)", border:"1px solid var(--line-strong)", padding:"1px 6px", borderRadius:4, letterSpacing:"0.05em" }}>
                              DRAFT
                            </span>
                        )}
                            </div>
                        <div style={{ fontSize:13, fontWeight:600, color:"#fff", marginBottom:2 }}>
                          {q.clientName || "No client name"}{q.jobName ? ` — ${q.jobName}` : ""} </div>
                        <div style={{ fontSize:10, color:"rgba(255,255,255,0.5)", fontFamily:"'DM Mono',monospace" }}>
                          ${q.total?.toLocaleString()} · {new Date(q.savedAt).toLocaleDateString()}
                          {q.sigName && <span style={{ color:"rgba(100,220,130,0.6)", marginLeft:8 }}>· Signed: {q.sigName}</span>}
                        </div>
                        </div>
                      <div style={{ display:"flex", gap:6, flexShrink:0 }}>
                        <button onClick={() => loadQuote(q)} style={{ padding:"7px 14px", borderRadius:7, border:"1px solid rgba(var(--accent-rgb),0.3)", background:"rgba(var(--accent-rgb),0.08)", color:"var(--accent)", fontSize:11, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>
                          Load → </button>
                        <button onClick={() => deleteQuote(q.id)} style={{ padding:"7px 10px", borderRadius:7, border:"1px solid var(--line-strong)", background:"transparent", color:"rgba(255,100,100,0.5)", fontSize:11, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>
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
                <div style={{ textAlign:"center", padding:"40px 20px", color:"rgba(255,255,255,0.5)" }}>
                  <div style={{ fontSize:28, marginBottom:10 }}>◎</div>
                  <div style={{ fontSize:13 }}>Add services to see profit analysis.</div>
                  </div>
              ) : (
                <>
                  {/* Key metrics */}
                  <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:8, marginBottom:16 }}>
                    {[
                      { label:"Total Revenue",     val:`$${total.toLocaleString()}`,         color:"var(--accent)", sub:"what client pays" },
                      { label:"Your Labor",         val:`$${totLab.toLocaleString()}`,         color:"#a8e87e", sub:`${totHrs.toFixed(1)} hrs @ $${hourlyRate}/hr` },
                      { label:"Materials Cost",     val:`$${totMat.toLocaleString()}`,         color:"#7eb8e8", sub:"your out-of-pocket" },
                      { label:"Gross Profit",       val:`$${markupAmt.toLocaleString()}`,      color:"var(--accent)", sub:`${marginPct}% margin` },
                      { label:"Effective Rate",     val:`$${effectiveRate}/hr`,                color:"#a8e87e", sub:"revenue per hour" },
                      { label:"Labor %",            val:`${laborPct}%`,                        color:"#7eb8e8", sub:"of total invoice" },
                    ].map(c => (
                      <div key={c.label} style={{ background:"var(--card)", border:"1px solid var(--line)", borderRadius:10, padding:"12px 14px" }}>
                        <div style={{ fontSize:10, color:"rgba(255,255,255,0.5)", textTransform:"uppercase", letterSpacing:"0.1em", marginBottom:5 }}>{c.label}</div>
                        <div style={{ fontFamily:"'DM Mono',monospace", fontSize:18, fontWeight:500, color:c.color }}>{c.val}</div>
                        <div style={{ fontSize:10, color:"rgba(255,255,255,0.5)", marginTop:2 }}>{c.sub}</div>
                        </div>
                    ))}
                    </div>

                  {/* Profitability grade */}
                  <div style={{ background: Number(marginPct) >= 30 ? "rgba(100,220,130,0.06)" : Number(marginPct) >= 20 ? "rgba(var(--accent-rgb),0.06)" : "rgba(232,120,120,0.06)", border: `1px solid ${Number(marginPct) >= 30 ? "rgba(100,220,130,0.2)" : Number(marginPct) >= 20 ? "rgba(var(--accent-rgb),0.2)" : "rgba(232,120,120,0.2)"}`, borderRadius:12, padding:"16px 18px", marginBottom:14 }}>
                    <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center" }}>
                      <div>
                        <div style={{ fontFamily:"'Syne',sans-serif", fontSize:14, fontWeight:800, color:"#fff" }}>
                          {Number(marginPct) >= 30 ? "✓ Strong margin" : Number(marginPct) >= 20 ? "⚡ Fair margin" : "⚠ Thin margin"} </div>
                        <div style={{ fontSize:11, color:"rgba(255,255,255,0.5)", marginTop:3 }}>
                          {Number(marginPct) >= 30 ? "Industry benchmark: 25–35% is healthy for residential electrical." : Number(marginPct) >= 20 ? "Consider raising markup or hourly rate to improve margins." : "Increase markup or hourly rate — you may be underpriced."}
                        </div>
                        </div>
                      <div style={{ fontFamily:"'DM Mono',monospace", fontSize:28, fontWeight:700, color: Number(marginPct) >= 30 ? "#7dcea0" : Number(marginPct) >= 20 ? "var(--accent)" : "#e87e7e" }}>
                        {marginPct}% </div>
                        </div>
                        </div>

                  {/* Per-service profitability breakdown */}
                  <div style={{ background:"rgba(255,255,255,0.022)", border:"1px solid var(--line)", borderRadius:12, padding:"14px 16px" }}>
                    <div style={{ fontSize:10, color:"rgba(255,255,255,0.5)", textTransform:"uppercase", letterSpacing:"0.1em", marginBottom:12 }}>Service Breakdown</div>
                    {activeItems.map(item => {
                      const itemMarkup = (item.lineTotal * markup);
                      const itemRevenue = item.lineTotal + itemMarkup;
                      const itemMargin = itemRevenue > 0 ? ((itemMarkup / itemRevenue) * 100).toFixed(0) : 0;
                      return (
                        <div key={item.id} style={{ display:"flex", alignItems:"center", gap:8, padding:"7px 0", borderBottom:"1px solid var(--line)" }}>
                          <div style={{ flex:1, minWidth:0 }}>
                            <div style={{ fontSize:12, color:"rgba(255,255,255,0.7)", fontWeight:600, lineHeight:1.2 }}>{item.label}</div>
                            <div style={{ fontSize:10, color:"rgba(255,255,255,0.5)", fontFamily:"'DM Mono',monospace", marginTop:1 }}>
                              {item.hrs.toFixed(1)} hrs · lab ${item.lab.toLocaleString()} · mat ${item.mat.toLocaleString()}
                            </div>
                            </div>
                          <div style={{ textAlign:"right", flexShrink:0 }}>
                            <div style={{ fontFamily:"'DM Mono',monospace", fontSize:13, fontWeight:700, color:"var(--accent)" }}>${itemRevenue.toLocaleString()}</div>
                            <div style={{ fontSize:10, color:"rgba(255,255,255,0.5)", fontFamily:"'DM Mono',monospace" }}>{itemMargin}% margin</div>
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
              initialPrompt={aiSeed}
              onApplyEstimate={applyAIEstimate}
              onClose={() => setShowAIBuilder(false)}
            />
          )}

          {/* ════════════ ELECTRIFICATION LOAD ADVISOR ════════════ */}
          {showLoadAdvisor && (
            <LoadAdvisor
              onApplyEstimate={applyAIEstimate}
              onClose={() => setShowLoadAdvisor(false)}
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

          <div style={{ textAlign:"center", marginTop:44, paddingTop:18, borderTop:"1px solid var(--line)", fontSize:10, color:"rgba(255,255,255,0.5)", letterSpacing:"0.07em" }} className="no-print">
            WIREWAY PRO · NEC 2023 · wirewaypro.com
          </div>
      </div>
      </div>

      {/* ════════════ ACCOUNT MODAL ════════════ */}
      {showAccount && (
        <div className="modal-overlay" onClick={e => e.target===e.currentTarget && setShowAccount(false)}>
          <div className="modal-box" style={{ padding:"24px" }}>
            <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:20 }}>
              <div>
                <div style={{ fontFamily:"'Syne',sans-serif", fontSize:16, fontWeight:800, color:"#fff" }}>Account</div>
                <div style={{ fontSize:11, color:"rgba(255,255,255,0.5)", marginTop:2 }}>{user?.email}</div>
                </div>
              <button onClick={() => setShowAccount(false)} style={{ background:"transparent", border:"none", color:"rgba(255,255,255,0.5)", fontSize:22, cursor:"pointer" }}>✕</button>
            </div>

            {/* Plan status */}
            <div style={{ background: userIsPro ? "rgba(100,220,130,0.06)" : "rgba(var(--accent-rgb),0.06)", border: userIsPro ? "1px solid rgba(100,220,130,0.2)" : "1px solid rgba(var(--accent-rgb),0.2)", borderRadius:10, padding:"12px 14px", marginBottom:16 }}>
              <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center" }}>
                <div>
                  <div style={{ fontSize:12, fontWeight:700, color: userIsPro ? "#7dcea0" : "var(--accent)" }}>
                    {userIsPro ? (onTrial ? `Pro Trial — ${daysLeft} days left` : "Wireway Pro") : "Free Plan"} </div>
                  <div style={{ fontSize:10, color:"rgba(255,255,255,0.5)", marginTop:2 }}>
                    {userIsPro ? "All features unlocked" : "3 quote limit · upgrade to unlock everything"}
                  </div>
                  </div>
                {!userIsPro && onShowPricing && (
                  <button onClick={() => { setShowAccount(false); onShowPricing(); }} style={{ padding:"6px 12px", borderRadius:7, border:"1px solid rgba(var(--accent-rgb),0.4)", background:"rgba(var(--accent-rgb),0.1)", color:"var(--accent)", fontSize:11, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>
                    Upgrade ⚡
                  </button>
              )}
                {userIsPro && (
                  <button onClick={async () => {
                    const { data: { session } } = await supabase.auth.getSession();
                    const token = session?.access_token;
                    if (!token) return;
                    const res = await fetch("/api/billing-portal", { method:"POST", headers:{ "Content-Type":"application/json", Authorization:`Bearer ${token}` }, body:JSON.stringify({}) });
                    const d = await res.json(); if (d.url) window.open(d.url, "_blank");
                  }} style={{ padding:"6px 12px", borderRadius:7, border:"1px solid var(--line-strong)", background:"var(--card)", color:"rgba(255,255,255,0.5)", fontSize:11, fontWeight:600, cursor:"pointer", fontFamily:"inherit" }}>
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
                <div key={s.label} style={{ background:"var(--card)", border:"1px solid var(--line)", borderRadius:8, padding:"10px", textAlign:"center" }}>
                  <div style={{ fontFamily:"'DM Mono',monospace", fontSize:20, fontWeight:700, color:"var(--accent)" }}>{s.val}</div>
                  <div style={{ fontSize:10, color:"rgba(255,255,255,0.5)", marginTop:2 }}>{s.label}</div>
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
                <div style={{ fontSize:11, color:"rgba(255,255,255,0.5)", marginTop:3 }}>
                  Quote {quoteNumber || "—"} · Total ${total.toLocaleString()}
                  </div>
                  </div>
              <button onClick={() => setSignModal(false)} style={{ background:"transparent", border:"none", color:"rgba(255,255,255,0.5)", fontSize:22, cursor:"pointer" }}>✕</button>
            </div>

            {/* Quote summary for reference */}
            <div style={{ background:"var(--card)", border:"1px solid var(--line)", borderRadius:10, padding:"12px 14px", marginBottom:20 }}>
              <div style={{ fontSize:11, color:"rgba(255,255,255,0.5)", marginBottom:8 }}>Scope Summary</div>
              {activeItems.slice(0, 5).map(i => (
                <div key={i.id} style={{ fontSize:11, color:"rgba(255,255,255,0.6)", padding:"3px 0", borderBottom:"1px solid var(--line)" }}>
                  {i.label} ({i.variantLabel}) × {i.qty} — <span style={{ color:"var(--accent)", fontFamily:"'DM Mono',monospace" }}>${i.lineTotal.toLocaleString()}</span>
                </div>
              ))}
              {activeItems.length > 5 && <div style={{ fontSize:10, color:"rgba(255,255,255,0.5)", paddingTop:6 }}>+ {activeItems.length - 5} more services</div>}
              <div style={{ display:"flex", justifyContent:"space-between", paddingTop:10, marginTop:6, borderTop:"1px solid rgba(var(--accent-rgb),0.15)" }}>
                <span style={{ fontSize:12, color:"rgba(255,255,255,0.5)" }}>Total</span>
                <span style={{ fontFamily:"'DM Mono',monospace", fontSize:16, fontWeight:600, color:"var(--accent)" }}>${total.toLocaleString()}</span>
              </div>
              </div>

            {/* Acceptance statement */}
            <div style={{ fontSize:11, color:"rgba(255,255,255,0.5)", lineHeight:1.7, marginBottom:20, padding:"12px 14px", background:"rgba(var(--accent-rgb),0.04)", borderRadius:8, border:"1px solid rgba(var(--accent-rgb),0.12)" }}>
              By entering your name and date below, you acknowledge that you have reviewed this estimate, agree to the scope of work and pricing, and authorize {company.name || "the electrician"} to proceed.
              {company.terms && <div style={{ marginTop:8, color:"rgba(255,255,255,0.5)", fontSize:10 }}>{company.terms}</div>}
            </div>

            {/* Signature fields */}
            <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:8, marginBottom:16 }}>
              <div>
                <div style={{ fontSize:10, color:"rgba(255,255,255,0.5)", textTransform:"uppercase", letterSpacing:"0.08em", marginBottom:6 }}>Client Full Name</div>
                <input placeholder="Type full name to accept" value={sigName} onChange={e => setSigName(e.target.value)}
                  style={{ ...inputStyle }} onFocus={focusGold} onBlur={blurGray} />
              </div>
              <div>
                <div style={{ fontSize:10, color:"rgba(255,255,255,0.5)", textTransform:"uppercase", letterSpacing:"0.08em", marginBottom:6 }}>Date</div>
                <input type="date" value={sigDate} onChange={e => setSigDate(e.target.value)}
                  style={{ ...inputStyle, colorScheme:"dark" }} onFocus={focusGold} onBlur={blurGray} />
              </div>
              </div>

            {/* Accept button */}
            {sigSaved ? (
              <div style={{ textAlign:"center", padding:"14px", background:"rgba(100,220,130,0.1)", border:"1px solid rgba(100,220,130,0.3)", borderRadius:10, color:"#7dcea0", fontSize:14, fontWeight:700 }}>
                ✓ Quote Accepted — {sigName} </div>
            ) : (
              <button onClick={acceptQuote} disabled={!sigName}
                style={{ width:"100%", padding:"14px", background: sigName ? "linear-gradient(135deg,rgba(100,220,130,0.2),rgba(100,220,130,0.08))" : "rgba(255,255,255,0.04)", border: sigName ? "1px solid rgba(100,220,130,0.4)" : "1px solid var(--line-strong)", borderRadius:11, color: sigName ? "#7dcea0" : "rgba(255,255,255,0.25)", fontSize:13, fontWeight:700, cursor: sigName ? "pointer" : "default", fontFamily:"inherit", transition:"all 0.2s" }}>
                ✍ Accept & Sign Quote
              </button>
          )}

            <div style={{ textAlign:"center", marginTop:10, fontSize:10, color:"rgba(255,255,255,0.5)" }}>
              This is an electronic acknowledgment. Save or print a copy for your records.
            </div>
          </div>
        </div>
      )}

      {showCustomers && (
        <CustomersView
          user={user} onClientsChange={setClients}
          clients={clients} savedQuotes={savedQuotes}
          onLoadQuote={(q) => { loadQuote(q); setShowCustomers(false); }}
          onNewEstimate={(cl) => { newQuote(true); setClientName(cl.name || ""); setClientEmail(cl.email || ""); setClientPhone(cl.phone || ""); setShowCustomers(false); setTab("services"); }}
          onClose={() => setShowCustomers(false)}
        />
      )}

      {showPullList && (
        <MaterialsListView
          activeItems={activeItems}
          totMat={totMat}
          jobName={jobName}
          onClose={() => setShowPullList(false)}
        />
      )}

      {showProposal && (
        <ProposalView
          company={company} clientName={clientName} clientEmail={clientEmail} clientPhone={clientPhone}
          jobName={jobName} notes={notes} quoteNumber={quoteNumber} activeItems={activeItems}
          subtotal={subtotal} markupAmt={markupAmt} taxAmt={taxAmt} taxEnabled={taxEnabled} total={total}
          depositPercent={depositPercent} sigName={sigSaved ? sigName : ""} sigDate={sigSaved ? sigDate : ""}
          onClose={() => setShowProposal(false)}
        />
      )}

      <WiremModals {...{
        wireCalcOpen,setWireCalcOpen,wireAmps,setWireAmps,wireLen,setWireLen,
        wireVolt,setWireVolt,wireMat,setWireMat,wireResult,
        loadCalcOpen,setLoadCalcOpen,sqft,setSqft,smallAppl,setSmallAppl,
        laundry,setLaundry,dryer,setDryer,range,setRange,acTons,setAcTons,
        heatKw,setHeatKw,loadResult,checklistOpen,setChecklistOpen,
        checklistType,setChecklistType,checkedItems,toggleCheck,CHECKLISTS,
        showClientDB,setShowClientDB,clientSearch,setClientSearch,clients,loadClient,
        signModal,setSignModal,sigName,setSigName,sigDate,setSigDate,sigSaved,
        acceptQuote,quoteNumber,total,activeItems,company,inputStyle,focusGold,
        blurGray,currentQuoteStatus,editingCompany,setEditingCompany,companyDraft,
        setCompanyDraft,logoDataUrl,setLogoDataUrl,saveCompany,handleLogoUpload,
        companySaving,showAccount,setShowAccount,user,profile,savedQuotes,
        onShowPricing,paymentBanner,paymentSuccess,setPaymentSuccess,onClearBanner,
      }} />
    </>
  );
}
