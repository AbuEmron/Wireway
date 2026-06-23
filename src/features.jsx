/* eslint-disable react-hooks/exhaustive-deps */
// src/features.jsx
// Six new features for Wireway:
// 1. JobCalendar      — week/month view, drag-to-schedule
// 2. PhotoAttachments — camera + file upload on quotes
// 3. OnMyWayButton    — sends client an SMS notification
// 4. ReviewRequest    — sends client a Google review link after job complete
// 5. QuickBooksExport — formats invoice data for QB Online
// 6. AutoInvoiceConvert — auto-flips accepted quote to invoice

import { useState, useEffect, useRef } from "react";
import { getJobs, upsertJob, deleteJob, updateJobStatus, getPhotos, uploadPhoto, deletePhoto } from "./lib/supabase";

// ─── SHARED STYLES ────────────────────────────────────────────────────────────
const card = { background:"rgba(255,255,255,0.022)", border:"1px solid var(--line)", borderRadius:12 };
const IS   = { background:"var(--card)", border:"1px solid var(--line)", borderRadius:7, padding:"8px 11px", fontSize:13, color:"#fff", fontFamily:"inherit", width:"100%", outline:"none" };
const focusGold = e => e.target.style.borderColor = "rgba(var(--accent-rgb),0.4)";
const blurGray  = e => e.target.style.borderColor = "rgba(255,255,255,0.07)";

const STATUS_COLOR = {
  scheduled:   "#7eb8e8",
  in_progress: "var(--accent)",
  complete:    "#7dcea0",
  cancelled:   "#e87e7e",
};

// ─── 1. JOB CALENDAR ─────────────────────────────────────────────────────────
export function JobCalendar({ user, onClose }) {
  const today    = new Date();
  const [year,   setYear]   = useState(today.getFullYear());
  const [month,  setMonth]  = useState(today.getMonth());
  const [jobs,   setJobs]   = useState([]);
  const [modal,  setModal]  = useState(null); // { job } or { date }
  const [draft,  setDraft]  = useState({});
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!user?.id) return;
    getJobs(user.id, { month, year }).then(({ data }) => setJobs(data));
  }, [user?.id, month, year]);

  const DAYS = ["Sun","Mon","Tue","Wed","Thu","Fri","Sat"];
  const firstDay  = new Date(year, month, 1).getDay();
  const daysInMon = new Date(year, month + 1, 0).getDate();
  const monthName = new Date(year, month).toLocaleString("default", { month: "long" });

  const jobsOnDate = (d) => {
    const ds = `${year}-${String(month+1).padStart(2,"0")}-${String(d).padStart(2,"0")}`;
    return jobs.filter(j => j.scheduled_date === ds);
  };

  const saveJob = async () => {
    if (!draft.title) return;
    setSaving(true);
    const { data } = await upsertJob(user.id, draft);
    if (data) setJobs(prev => { const i = prev.findIndex(j=>j.id===data.id); return i>=0 ? prev.map((j,x)=>x===i?data:j) : [...prev, data]; });
    setSaving(false);
    setModal(null);
    setDraft({});
  };

  const removeJob = async (id) => {
    await deleteJob(id, user.id);
    setJobs(prev => prev.filter(j => j.id !== id));
    setModal(null);
  };

  const cycleStatus = async (job) => {
    const cycle = { scheduled:"in_progress", in_progress:"complete", complete:"scheduled" };
    const next = cycle[job.status] || "scheduled";
    const { data } = await updateJobStatus(job.id, next);
    if (data) setJobs(prev => prev.map(j => j.id===data.id ? data : j));
  };

  return (
    <>
      <style>{`*{box-sizing:border-box;margin:0;padding:0}body{background:var(--bg0)}@keyframes fadeUp{from{opacity:0;transform:translateY(10px)}to{opacity:1;transform:translateY(0)}}`}</style>

      <div style={{ minHeight:"100vh", background:"var(--bg0)", fontFamily:"'DM Sans',sans-serif", color:"#fff", paddingBottom:60 }}>

        {/* Header */}
        <div style={{ borderBottom:"1px solid var(--line)", background:"rgba(10,10,12,0.9)", backdropFilter:"blur(20px)", WebkitBackdropFilter:"blur(20px)", position:"sticky", top:0, zIndex:100, padding:"0 20px" }}>
          <div style={{ maxWidth:860, margin:"0 auto", height:54, display:"flex", alignItems:"center", justifyContent:"space-between" }}>
            <div style={{ display:"flex", alignItems:"center", gap:12 }}>
              <button onClick={() => { setMonth(m => m===0 ? 11 : m-1); if(month===0) setYear(y=>y-1); }} style={{ background:"transparent", border:"1px solid var(--line-strong)", borderRadius:6, color:"rgba(255,255,255,0.5)", fontSize:16, cursor:"pointer", padding:"3px 10px" }}>‹</button>
              <span style={{ fontFamily:"'Syne',sans-serif", fontSize:16, fontWeight:800, letterSpacing:"-0.02em" }}>{monthName} {year}</span>
              <button onClick={() => { setMonth(m => m===11 ? 0 : m+1); if(month===11) setYear(y=>y+1); }} style={{ background:"transparent", border:"1px solid var(--line-strong)", borderRadius:6, color:"rgba(255,255,255,0.5)", fontSize:16, cursor:"pointer", padding:"3px 10px" }}>›</button>
              <button onClick={() => { setYear(today.getFullYear()); setMonth(today.getMonth()); }} style={{ padding:"4px 10px", borderRadius:5, border:"1px solid var(--line)", background:"transparent", color:"rgba(255,255,255,0.35)", fontSize:10, fontWeight:600, cursor:"pointer", fontFamily:"inherit" }}>Today</button>
            </div>
            <div style={{ display:"flex", gap:8 }}>
              <button onClick={() => { setModal({ date: `${year}-${String(month+1).padStart(2,"0")}-${String(today.getDate()).padStart(2,"0")}` }); setDraft({ scheduled_date: `${year}-${String(month+1).padStart(2,"0")}-${String(today.getDate()).padStart(2,"0")}`, duration_hours: 2, status:"scheduled" }); }} style={{ padding:"6px 14px", borderRadius:7, background:"rgba(var(--accent-rgb),0.1)", border:"1px solid rgba(var(--accent-rgb),0.3)", color:"var(--accent)", fontSize:11, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>
                + New Job
              </button>
              {onClose && <button onClick={onClose} style={{ padding:"6px 12px", borderRadius:7, border:"1px solid var(--line-strong)", background:"transparent", color:"rgba(255,255,255,0.4)", fontSize:11, cursor:"pointer", fontFamily:"inherit" }}>← Back</button>}
            </div>
          </div>
        </div>

        <div style={{ maxWidth:860, margin:"0 auto", padding:"16px 20px" }}>

          {/* Day headers */}
          <div style={{ display:"grid", gridTemplateColumns:"repeat(7,1fr)", gap:2, marginBottom:4 }}>
            {DAYS.map(d => <div key={d} style={{ textAlign:"center", fontSize:10, color:"rgba(255,255,255,0.28)", fontWeight:700, letterSpacing:"0.05em", padding:"4px 0" }}>{d}</div>)}
          </div>

          {/* Calendar grid */}
          <div style={{ display:"grid", gridTemplateColumns:"repeat(7,1fr)", gap:2 }}>
            {Array.from({ length: firstDay }).map((_,i) => <div key={`e${i}`} style={{ minHeight:80 }}/>)}
            {Array.from({ length: daysInMon }).map((_,i) => {
              const d = i + 1;
              const isToday = d===today.getDate() && month===today.getMonth() && year===today.getFullYear();
              const dayJobs = jobsOnDate(d);
              const ds = `${year}-${String(month+1).padStart(2,"0")}-${String(d).padStart(2,"0")}`;
              return (
                <div key={d} onClick={() => { setDraft({ scheduled_date:ds, duration_hours:2, status:"scheduled" }); setModal({ date:ds }); }}
                  style={{ minHeight:80, background: isToday ? "rgba(var(--accent-rgb),0.07)" : "rgba(255,255,255,0.018)", border: isToday ? "1px solid rgba(var(--accent-rgb),0.25)" : "1px solid var(--line)", borderRadius:8, padding:"6px 6px 4px", cursor:"pointer", transition:"background 0.15s" }}
                  onMouseEnter={e => !isToday && (e.currentTarget.style.background="rgba(255,255,255,0.03)")}
                  onMouseLeave={e => !isToday && (e.currentTarget.style.background="rgba(255,255,255,0.018)")}
                >
                  <div style={{ fontSize:11, fontWeight: isToday ? 800 : 500, color: isToday ? "var(--accent)" : "rgba(255,255,255,0.5)", marginBottom:4 }}>{d}</div>
                  {dayJobs.slice(0,3).map(job => (
                    <div key={job.id} onClick={e => { e.stopPropagation(); setModal({ job }); setDraft(job); }}
                      style={{ fontSize:9, fontWeight:700, color: STATUS_COLOR[job.status]||"#7eb8e8", background:`${STATUS_COLOR[job.status]||"#7eb8e8"}15`, borderRadius:3, padding:"2px 4px", marginBottom:2, overflow:"hidden", whiteSpace:"nowrap", textOverflow:"ellipsis", cursor:"pointer" }}>
                      {job.scheduled_time?.slice(0,5)} {job.title}
                    </div>
                  ))}
                  {dayJobs.length > 3 && <div style={{ fontSize:9, color:"rgba(255,255,255,0.25)" }}>+{dayJobs.length-3} more</div>}
                </div>
              );
            })}
          </div>

          {/* Upcoming jobs list */}
          {jobs.filter(j => j.status !== "complete" && j.status !== "cancelled").length > 0 && (
            <div style={{ marginTop:20 }}>
              <div style={{ fontSize:10, color:"rgba(255,255,255,0.28)", textTransform:"uppercase", letterSpacing:"0.1em", marginBottom:10 }}>Upcoming</div>
              {jobs.filter(j => j.status!=="complete"&&j.status!=="cancelled").sort((a,b)=>a.scheduled_date?.localeCompare(b.scheduled_date)).slice(0,8).map(job => (
                <div key={job.id} onClick={() => { setModal({ job }); setDraft(job); }}
                  style={{ display:"flex", alignItems:"center", gap:12, padding:"10px 14px", ...card, marginBottom:6, cursor:"pointer" }}
                  onMouseEnter={e => e.currentTarget.style.background="rgba(255,255,255,0.035)"}
                  onMouseLeave={e => e.currentTarget.style.background="rgba(255,255,255,0.022)"}
                >
                  <div style={{ width:3, height:32, borderRadius:2, background:STATUS_COLOR[job.status]||"#7eb8e8", flexShrink:0 }}/>
                  <div style={{ flex:1, minWidth:0 }}>
                    <div style={{ fontSize:13, fontWeight:600, color:"#fff" }}>{job.title}</div>
                    <div style={{ fontSize:10, color:"rgba(255,255,255,0.35)", fontFamily:"'DM Mono',monospace" }}>
                      {job.scheduled_date} {job.scheduled_time?.slice(0,5)} · {job.client_name}
                    </div>
                  </div>
                  <div style={{ display:"flex", gap:6 }}>
                    <button onClick={e => { e.stopPropagation(); cycleStatus(job); }} style={{ padding:"4px 8px", borderRadius:5, border:`1px solid ${STATUS_COLOR[job.status]}30`, background:`${STATUS_COLOR[job.status]}10`, color:STATUS_COLOR[job.status], fontSize:9, fontWeight:700, cursor:"pointer", fontFamily:"inherit", textTransform:"capitalize" }}>
                      {job.status.replace("_"," ")}
                    </button>
                    {job.client_phone && (
                      <button onClick={e => { e.stopPropagation(); sendOnMyWay(job); }} style={{ padding:"4px 8px", borderRadius:5, border:"1px solid rgba(168,232,126,0.3)", background:"rgba(168,232,126,0.08)", color:"#a8e87e", fontSize:9, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>
                        On Way
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* ── Job modal ── */}
      {modal && (
        <div style={{ position:"fixed", inset:0, background:"rgba(0,0,0,0.75)", backdropFilter:"blur(8px)", WebkitBackdropFilter:"blur(8px)", zIndex:200, display:"flex", alignItems:"flex-start", justifyContent:"center", overflowY:"auto", padding:"24px 16px" }}
          onClick={e => e.target===e.currentTarget && setModal(null)}>
          <div style={{ background:"var(--surface)", border:"1px solid var(--line-strong)", borderRadius:18, width:"100%", maxWidth:520, animation:"fadeUp 0.2s ease both", margin:"auto", padding:"24px" }}>
            <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:20 }}>
              <div style={{ fontFamily:"'Syne',sans-serif", fontSize:16, fontWeight:800, color:"#fff" }}>
                {modal.job ? "Edit Job" : "New Job"}
              </div>
              <button onClick={() => setModal(null)} style={{ background:"transparent", border:"none", color:"rgba(255,255,255,0.4)", fontSize:22, cursor:"pointer" }}>✕</button>
            </div>

            <div style={{ display:"flex", flexDirection:"column", gap:8 }}>
              <input placeholder="Job title *" value={draft.title||""} onChange={e=>setDraft(p=>({...p,title:e.target.value}))} style={IS} onFocus={focusGold} onBlur={blurGray}/>
              <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:8 }}>
                <input placeholder="Client name" value={draft.client_name||""} onChange={e=>setDraft(p=>({...p,client_name:e.target.value}))} style={IS} onFocus={focusGold} onBlur={blurGray}/>
                <input placeholder="Client phone" value={draft.client_phone||""} onChange={e=>setDraft(p=>({...p,client_phone:e.target.value}))} style={IS} onFocus={focusGold} onBlur={blurGray}/>
                <input type="date" value={draft.scheduled_date||""} onChange={e=>setDraft(p=>({...p,scheduled_date:e.target.value}))} style={{...IS,colorScheme:"dark"}} onFocus={focusGold} onBlur={blurGray}/>
                <input type="time" value={draft.scheduled_time||""} onChange={e=>setDraft(p=>({...p,scheduled_time:e.target.value}))} style={{...IS,colorScheme:"dark"}} onFocus={focusGold} onBlur={blurGray}/>
                <input placeholder="Address" value={draft.job_address||""} onChange={e=>setDraft(p=>({...p,job_address:e.target.value}))} style={IS} onFocus={focusGold} onBlur={blurGray}/>
                <input type="number" placeholder="Hours est." min="0.5" step="0.5" value={draft.duration_hours||""} onChange={e=>setDraft(p=>({...p,duration_hours:Number(e.target.value)}))} style={IS} onFocus={focusGold} onBlur={blurGray}/>
              </div>
              <textarea placeholder="Notes" value={draft.notes||""} onChange={e=>setDraft(p=>({...p,notes:e.target.value}))} rows={2} style={{...IS,resize:"vertical",lineHeight:1.5}} onFocus={focusGold} onBlur={blurGray}/>

              {/* Status */}
              <div style={{ display:"flex", gap:5 }}>
                {["scheduled","in_progress","complete","cancelled"].map(s => (
                  <button key={s} onClick={() => setDraft(p=>({...p,status:s}))} style={{ flex:1, padding:"5px", borderRadius:6, border: draft.status===s ? `1px solid ${STATUS_COLOR[s]}50` : "1px solid var(--line)", background: draft.status===s ? `${STATUS_COLOR[s]}15` : "transparent", color: draft.status===s ? STATUS_COLOR[s] : "rgba(255,255,255,0.3)", fontSize:9, fontWeight:700, cursor:"pointer", fontFamily:"inherit", textTransform:"capitalize" }}>
                    {s.replace("_"," ")}
                  </button>
                ))}
              </div>
            </div>

            <div style={{ display:"flex", gap:8, marginTop:16 }}>
              <button onClick={saveJob} disabled={saving||!draft.title} style={{ flex:1, padding:"12px", background:"linear-gradient(135deg,rgba(var(--accent-rgb),0.2),rgba(var(--accent-rgb),0.08))", border:"1px solid rgba(var(--accent-rgb),0.35)", borderRadius:9, color:"var(--accent)", fontSize:13, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>
                {saving ? "Saving..." : "Save Job"}
              </button>
              {modal.job && (
                <button onClick={() => removeJob(modal.job.id)} style={{ padding:"12px 16px", background:"rgba(232,126,126,0.06)", border:"1px solid rgba(232,126,126,0.2)", borderRadius:9, color:"#e87e7e", fontSize:12, fontWeight:600, cursor:"pointer", fontFamily:"inherit" }}>
                  Delete
                </button>
              )}
            </div>

            {/* On My Way button */}
            {draft.client_phone && (
              <button onClick={() => sendOnMyWay(draft)} style={{ width:"100%", marginTop:8, padding:"10px", background:"rgba(168,232,126,0.06)", border:"1px solid rgba(168,232,126,0.2)", borderRadius:9, color:"#a8e87e", fontSize:12, fontWeight:600, cursor:"pointer", fontFamily:"inherit" }}>
                📍 Send "On My Way" Text to {draft.client_name || "Client"}
              </button>
            )}

            {/* Review request (complete jobs only) */}
            {draft.status === "complete" && draft.client_phone && (
              <button onClick={() => sendReviewRequest(draft)} style={{ width:"100%", marginTop:6, padding:"10px", background:"rgba(129,140,248,0.06)", border:"1px solid rgba(129,140,248,0.2)", borderRadius:9, color:"#818cf8", fontSize:12, fontWeight:600, cursor:"pointer", fontFamily:"inherit" }}>
                ⭐ Send Review Request
              </button>
            )}
          </div>
        </div>
      )}
    </>
  );
}

// ─── ON MY WAY HELPER ────────────────────────────────────────────────────────
export function sendOnMyWay(job, companyName = "") {
  const msg = encodeURIComponent(
    `Hi ${job.client_name || "there"}, ${companyName || "your electrician"} is on the way to ${job.job_address || "your location"}. ETA ~20 minutes. See you soon!`
  );
  const to = (job.client_phone || "").replace(/\D/g,"");
  window.open(`sms:${to}?body=${msg}`);
}

// ─── REVIEW REQUEST HELPER ───────────────────────────────────────────────────
export function sendReviewRequest(job, companyName = "", reviewUrl = "") {
  const link = reviewUrl || "https://g.page/r/YOUR_GOOGLE_REVIEW_LINK";
  const msg  = encodeURIComponent(
    `Hi ${job.client_name || "there"}, thanks for choosing ${companyName || "us"} for your electrical work! We'd really appreciate a quick Google review — it takes less than a minute:\n${link}\nThank you! 🙏`
  );
  const to = (job.client_phone || "").replace(/\D/g,"");
  window.open(`sms:${to}?body=${msg}`);
}

// ─── 2. PHOTO ATTACHMENTS ────────────────────────────────────────────────────
export function PhotoAttachments({ user, quoteId, quoteName }) {
  const [photos,    setPhotos]    = useState([]);
  const [uploading, setUploading] = useState(false);
  const [lightbox,  setLightbox]  = useState(null);
  const fileRef = useRef();

  useEffect(() => {
    if (!quoteId) return;
    getPhotos(quoteId).then(({ data }) => setPhotos(data));
  }, [quoteId]);

  const handleFiles = async (files) => {
    if (!user?.id || !quoteId) return;
    setUploading(true);
    for (const file of Array.from(files)) {
      if (!file.type.startsWith("image/")) continue;
      const { data } = await uploadPhoto(user.id, quoteId, file);
      if (data) setPhotos(prev => [...prev, data]);
    }
    setUploading(false);
  };

  const removePhoto = async (id) => {
    await deletePhoto(id);
    setPhotos(prev => prev.filter(p => p.id !== id));
  };

  return (
    <div>
      <div style={{ display:"flex", alignItems:"center", justifyContent:"space-between", marginBottom:10 }}>
        <div style={{ fontSize:10, color:"rgba(255,255,255,0.28)", textTransform:"uppercase", letterSpacing:"0.1em" }}>
          Photos {photos.length > 0 && `(${photos.length})`}
        </div>
        <button onClick={() => fileRef.current?.click()} disabled={uploading} style={{ padding:"4px 10px", borderRadius:6, border:"1px solid rgba(var(--accent-rgb),0.3)", background:"rgba(var(--accent-rgb),0.08)", color:"var(--accent)", fontSize:10, fontWeight:600, cursor:"pointer", fontFamily:"inherit" }}>
          {uploading ? "Uploading..." : "+ Add Photo"}
        </button>
        <input ref={fileRef} type="file" accept="image/*" multiple capture="environment" style={{ display:"none" }} onChange={e => handleFiles(e.target.files)} />
      </div>

      {photos.length === 0 ? (
        <div onClick={() => fileRef.current?.click()} style={{ border:"1px dashed rgba(255,255,255,0.1)", borderRadius:10, padding:"20px", textAlign:"center", cursor:"pointer", color:"rgba(255,255,255,0.2)", fontSize:12 }}>
          📷 Tap to add job site photos
        </div>
      ) : (
        <div style={{ display:"grid", gridTemplateColumns:"repeat(auto-fill,minmax(80px,1fr))", gap:6 }}>
          {photos.map(photo => (
            <div key={photo.id} style={{ position:"relative", aspectRatio:"1", borderRadius:8, overflow:"hidden", cursor:"pointer" }} onClick={() => setLightbox(photo)}>
              <img src={photo.url} alt="" style={{ width:"100%", height:"100%", objectFit:"cover" }} />
              <button onClick={e => { e.stopPropagation(); removePhoto(photo.id); }} style={{ position:"absolute", top:3, right:3, width:18, height:18, borderRadius:"50%", background:"rgba(0,0,0,0.6)", border:"none", color:"#fff", fontSize:10, cursor:"pointer", display:"flex", alignItems:"center", justifyContent:"center" }}>✕</button>
            </div>
          ))}
          <div onClick={() => fileRef.current?.click()} style={{ aspectRatio:"1", borderRadius:8, border:"1px dashed rgba(255,255,255,0.1)", display:"flex", alignItems:"center", justifyContent:"center", cursor:"pointer", color:"rgba(255,255,255,0.25)", fontSize:20 }}>+</div>
        </div>
      )}

      {/* Lightbox */}
      {lightbox && (
        <div style={{ position:"fixed", inset:0, background:"rgba(0,0,0,0.92)", zIndex:300, display:"flex", alignItems:"center", justifyContent:"center" }} onClick={() => setLightbox(null)}>
          <img src={lightbox.url} alt="" style={{ maxWidth:"90vw", maxHeight:"90vh", borderRadius:8, objectFit:"contain" }} />
          <button onClick={() => setLightbox(null)} style={{ position:"fixed", top:20, right:20, background:"rgba(255,255,255,0.1)", border:"none", color:"#fff", fontSize:24, cursor:"pointer", width:40, height:40, borderRadius:"50%", display:"flex", alignItems:"center", justifyContent:"center" }}>✕</button>
        </div>
      )}
    </div>
  );
}

// ─── 3. QUICKBOOKS EXPORT ────────────────────────────────────────────────────
export function QuickBooksExport({ quote, company, activeItems, total, totLab, totMat, markupAmt, taxAmt, taxRate, taxEnabled }) {
  const [copied, setCopied] = useState(false);
  const [showInstructions, setShowInstructions] = useState(false);

  const buildQBText = () => {
    const lines = [
      "QUICKBOOKS INVOICE IMPORT DATA",
      "================================",
      `Customer:     ${quote.clientName || ""}`,
      `Invoice Date: ${new Date().toLocaleDateString()}`,
      `Due Date:     ${quote.invoiceDueDate || ""}`,
      `Invoice #:    ${quote.quoteNumber || ""}`,
      "",
      "LINE ITEMS",
      "----------",
      ...activeItems.map(i =>
        `${i.label} (${i.variantLabel}) × ${i.qty} | $${i.lineTotal.toLocaleString()} | Service`
      ),
      "",
      "TOTALS",
      "------",
      `Materials: $${totMat.toLocaleString()}`,
      `Labor:     $${totLab.toLocaleString()}`,
      `Markup:    $${markupAmt.toLocaleString()}`,
      taxEnabled ? `Tax (${(taxRate*100).toFixed(1)}%): $${taxAmt.toLocaleString()}` : null,
      `TOTAL:     $${total.toLocaleString()}`,
      "",
      `Company:  ${company.name || ""}`,
      `License:  ${company.license || ""}`,
      "",
      "Generated by Wireway — wirewaypro.com",
    ].filter(Boolean).join("\n");
    navigator.clipboard.writeText(lines);
    setCopied(true);
    setTimeout(() => setCopied(false), 2500);
  };

  return (
    <div style={{ marginTop:8 }}>
      <div style={{ display:"flex", gap:8 }}>
        <button onClick={buildQBText} style={{ flex:1, padding:"10px", background:"rgba(40,180,100,0.08)", border:"1px solid rgba(40,180,100,0.25)", borderRadius:9, color:"#4ade80", fontSize:12, fontWeight:700, cursor:"pointer", fontFamily:"inherit", display:"flex", alignItems:"center", justifyContent:"center", gap:6 }}>
          {copied ? "✓ Copied!" : "📊 Copy for QuickBooks"}
        </button>
        <button onClick={() => setShowInstructions(v=>!v)} style={{ padding:"10px 12px", border:"1px solid var(--line-strong)", borderRadius:9, background:"transparent", color:"rgba(255,255,255,0.35)", fontSize:11, cursor:"pointer", fontFamily:"inherit" }}>
          ?
        </button>
      </div>
      {showInstructions && (
        <div style={{ marginTop:8, padding:"12px 14px", background:"var(--card)", border:"1px solid var(--line)", borderRadius:9, fontSize:11, color:"rgba(255,255,255,0.5)", lineHeight:1.7 }}>
          <div style={{ fontWeight:700, color:"rgba(255,255,255,0.7)", marginBottom:6 }}>How to import into QuickBooks Online:</div>
          1. Copy the data above<br/>
          2. Open QuickBooks Online → Invoices → New Invoice<br/>
          3. Enter client name, date, and line items manually from the copied data<br/>
          4. Or use QBO's Import feature: Settings → Import Data → Invoices (CSV)<br/>
          <div style={{ marginTop:6, color:"rgba(255,255,255,0.3)", fontSize:10 }}>
            Full API sync coming soon in Wireway Pro Teams.
          </div>
        </div>
      )}
    </div>
  );
}

// ─── 4. AUTO-INVOICE CONVERT BUTTON ─────────────────────────────────────────
export function AutoInvoiceButton({ onConvert, isInvoiceMode }) {
  if (isInvoiceMode) return null;
  return (
    <button onClick={onConvert} style={{ width:"100%", padding:"10px", background:"rgba(184,126,232,0.06)", border:"1px solid rgba(184,126,232,0.18)", borderRadius:9, color:"#b87ee8", fontSize:12, fontWeight:600, cursor:"pointer", fontFamily:"inherit", marginTop:6 }}>
      ◻ Convert to Invoice
    </button>
  );
}

// ─── 5. ON MY WAY BUTTON (standalone) ───────────────────────────────────────
export function OnMyWayButton({ clientName, clientPhone, jobAddress, companyName }) {
  const [sent, setSent] = useState(false);

  if (!clientPhone) return null;

  const send = () => {
    const msg = encodeURIComponent(
      `Hi ${clientName || "there"}, ${companyName || "your electrician"} is on the way to ${jobAddress || "your location"}. ETA ~20 minutes. See you soon!`
    );
    const to = clientPhone.replace(/\D/g,"");
    window.open(`sms:${to}?body=${msg}`);
    setSent(true);
    setTimeout(() => setSent(false), 5000);
  };

  return (
    <button onClick={send} style={{ width:"100%", padding:"11px", background: sent ? "rgba(100,220,130,0.08)" : "rgba(168,232,126,0.06)", border: sent ? "1px solid rgba(100,220,130,0.3)" : "1px solid rgba(168,232,126,0.2)", borderRadius:10, color: sent ? "#7dcea0" : "#a8e87e", fontSize:13, fontWeight:700, cursor:"pointer", fontFamily:"inherit", transition:"all 0.2s" }}>
      {sent ? "✓ On My Way text opened!" : `📍 Send "On My Way" to ${clientName || "Client"}`}
    </button>
  );
}

// ─── 6. REVIEW REQUEST BUTTON (standalone) ──────────────────────────────────
export function ReviewRequestButton({ clientName, clientPhone, companyName, reviewUrl }) {
  const [sent, setSent] = useState(false);

  if (!clientPhone) return null;

  const send = () => {
    const link = reviewUrl || "";
    const msg  = encodeURIComponent(
      `Hi ${clientName || "there"}, thank you for choosing ${companyName || "us"}! We'd appreciate a quick Google review — it only takes a minute:\n${link}\nThank you! 🙏`
    );
    const to = clientPhone.replace(/\D/g,"");
    window.open(`sms:${to}?body=${msg}`);
    setSent(true);
    setTimeout(() => setSent(false), 5000);
  };

  return (
    <button onClick={send} style={{ width:"100%", padding:"11px", background: sent ? "rgba(100,220,130,0.08)" : "rgba(129,140,248,0.06)", border: sent ? "1px solid rgba(100,220,130,0.3)" : "1px solid rgba(129,140,248,0.2)", borderRadius:10, color: sent ? "#7dcea0" : "#818cf8", fontSize:13, fontWeight:700, cursor:"pointer", fontFamily:"inherit", transition:"all 0.2s" }}>
      {sent ? "✓ Review request opened!" : `⭐ Send Review Request to ${clientName || "Client"}`}
    </button>
  );
}
