/* eslint-disable react-hooks/exhaustive-deps */
// src/AppointmentPublicPage.jsx
// Client-facing appointment view — opened via a shareable link (/appt/[job id]).
// No login required. Client can Confirm, Request a new time, or Cancel.
// Mirrors QuotePublicPage styling + the /api/quote/[id] service-role pattern.

import { useState, useEffect } from "react";

function fmtDate(d) {
  if (!d) return "Date TBD";
  const dt = new Date(d + "T00:00:00");
  if (isNaN(dt)) return d;
  return dt.toLocaleDateString(undefined, { weekday: "long", month: "long", day: "numeric", year: "numeric" });
}
function fmtTime(t) {
  if (!t) return "";
  const [h, m] = t.split(":");
  const hr = Number(h);
  const ap = hr >= 12 ? "PM" : "AM";
  const h12 = ((hr + 11) % 12) + 1;
  return `${h12}:${m} ${ap}`;
}

export default function AppointmentPublicPage({ appointmentId }) {
  const [data,    setData]    = useState(null);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState("");
  const [busy,    setBusy]    = useState(false);
  const [mode,    setMode]    = useState(null);   // null | "reschedule" | "cancel"
  const [rsDate,  setRsDate]  = useState("");
  const [rsTime,  setRsTime]  = useState("");
  const [reason,  setReason]  = useState("");
  const [flash,   setFlash]   = useState("");

  useEffect(() => {
    if (!appointmentId) return;
    fetch(`/api/appointment/${appointmentId}`)
      .then(r => r.json())
      .then(d => { if (d.error) setError(d.error); else setData(d); setLoading(false); })
      .catch(() => { setError("Could not load appointment."); setLoading(false); });
  }, [appointmentId]);

  const act = async (action, extra = {}, okMsg = "") => {
    setBusy(true); setError("");
    try {
      const res = await fetch(`/api/appointment/${appointmentId}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ action, ...extra }),
      });
      const d = await res.json();
      if (d.success) { setData(prev => ({ ...prev, appointment: d.appointment })); setMode(null); setFlash(okMsg); }
      else setError(d.error || "Something went wrong.");
    } catch { setError("Network error — please try again."); }
    finally { setBusy(false); }
  };

  const a = data?.appointment;
  const e = data?.electrician;

  const IS = {
    background:"var(--card)", border:"1px solid var(--line-strong)",
    borderRadius:8, padding:"10px 13px", fontSize:14, color:"#fff",
    fontFamily:"inherit", width:"100%", outline:"none",
  };
  const btn = (bg, bd, col) => ({
    width:"100%", padding:"13px", borderRadius:10, background:bg, border:`1px solid ${bd}`,
    color:col, fontSize:13, fontWeight:700, cursor:"pointer", fontFamily:"inherit",
  });

  if (loading) return (
    <div style={{ minHeight:"100vh", display:"flex", alignItems:"center", justifyContent:"center", background:"var(--bg0)" }}>
      <div style={{ color:"rgba(255,255,255,0.4)", fontFamily:"sans-serif", fontSize:14 }}>Loading appointment...</div>
    </div>
  );
  if (error && !data) return (
    <div style={{ minHeight:"100vh", display:"flex", alignItems:"center", justifyContent:"center", background:"var(--bg0)" }}>
      <div style={{ color:"#e87e7e", fontFamily:"sans-serif", fontSize:14 }}>{error}</div>
    </div>
  );

  const cancelled = a?.status === "cancelled";
  const cs = a?.change_status;

  return (
    <>
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=DM+Mono:wght@400;500&family=Syne:wght@700;800&family=DM+Sans:wght@400;500;600&display=swap');
        *{box-sizing:border-box;margin:0;padding:0}
        body{background:var(--bg0)}
      `}</style>

      <div style={{ minHeight:"100vh", background:"var(--bg-scene)", fontFamily:"'DM Sans',sans-serif", color:"#fff", paddingBottom:60 }}>

        {/* Header */}
        <div style={{ borderBottom:"1px solid var(--line)", padding:"14px 20px", background:"rgba(10,10,12,0.9)", backdropFilter:"blur(20px)", WebkitBackdropFilter:"blur(20px)" }}>
          <div style={{ maxWidth:600, margin:"0 auto", display:"flex", alignItems:"center", gap:10 }}>
            {e?.logo_url
              ? <img src={e.logo_url} alt="logo" style={{ height:32, width:"auto", borderRadius:5, objectFit:"contain" }} />
              : <div style={{ width:30, height:30, borderRadius:6, background:"linear-gradient(135deg,var(--accent),#c9a84c)", display:"flex", alignItems:"center", justifyContent:"center", fontSize:14, fontWeight:800, color:"var(--bg0)" }}>
                  {(e?.company_name || "W")[0].toUpperCase()}
                </div>
            }
            <div>
              <div style={{ fontFamily:"'Syne',sans-serif", fontSize:15, fontWeight:800, color:"#fff" }}>{e?.company_name || "Your Electrician"}</div>
              <div style={{ fontSize:10, color:"rgba(255,255,255,0.35)" }}>Appointment</div>
            </div>
          </div>
        </div>

        <div style={{ maxWidth:600, margin:"0 auto", padding:"24px 20px" }}>

          {/* State banners */}
          {cancelled && (
            <div style={{ textAlign:"center", padding:"12px", background:"rgba(232,126,126,0.08)", border:"1px solid rgba(232,126,126,0.25)", borderRadius:10, marginBottom:20, color:"#e87e7e", fontSize:13, fontWeight:700 }}>
              This appointment has been cancelled.
            </div>
          )}
          {!cancelled && cs === "confirmed" && (
            <div style={{ textAlign:"center", padding:"12px", background:"rgba(100,220,130,0.08)", border:"1px solid rgba(100,220,130,0.25)", borderRadius:10, marginBottom:20, color:"#7dcea0", fontSize:13, fontWeight:700 }}>
              ✓ You confirmed this appointment. Thank you!
            </div>
          )}
          {!cancelled && cs === "reschedule_requested" && (
            <div style={{ textAlign:"center", padding:"12px", background:"rgba(126,184,232,0.08)", border:"1px solid rgba(126,184,232,0.25)", borderRadius:10, marginBottom:20, color:"#7eb8e8", fontSize:13, fontWeight:700 }}>
              Reschedule requested for {fmtDate(a.proposed_date)}{a.proposed_time ? ` at ${fmtTime(a.proposed_time)}` : ""}. Your electrician will confirm.
            </div>
          )}
          {flash && <div style={{ textAlign:"center", padding:"10px", background:"rgba(255,255,255,0.04)", border:"1px solid var(--line)", borderRadius:10, marginBottom:16, color:"rgba(255,255,255,0.7)", fontSize:12 }}>{flash}</div>}
          {error && <div style={{ textAlign:"center", padding:"10px", marginBottom:16, color:"#e87e7e", fontSize:12 }}>{error}</div>}

          {/* Appointment card */}
          <div style={{ background:"rgba(255,255,255,0.022)", border:"1px solid var(--line)", borderRadius:14, padding:"20px", marginBottom:20 }}>
            <div style={{ fontFamily:"'Syne',sans-serif", fontSize:18, fontWeight:800, color:"#fff", marginBottom:14 }}>{a?.title || "Your appointment"}</div>
            <div style={{ display:"flex", flexDirection:"column", gap:10 }}>
              <Row label="When" value={`${fmtDate(a?.scheduled_date)}${a?.scheduled_time ? "  ·  " + fmtTime(a?.scheduled_time) : ""}`} />
              {a?.duration_hours ? <Row label="Estimated time" value={`${a.duration_hours} hr`} /> : null}
              {a?.job_address ? <Row label="Location" value={a.job_address} /> : null}
              {a?.client_name ? <Row label="For" value={a.client_name} /> : null}
              {e?.company_phone ? <Row label="Questions?" value={e.company_phone} /> : null}
            </div>
          </div>

          {/* Actions — hidden once cancelled */}
          {!cancelled && mode === null && (
            <div style={{ display:"flex", flexDirection:"column", gap:10 }}>
              <button disabled={busy} onClick={() => act("confirm", {}, "Appointment confirmed.")}
                style={btn("linear-gradient(135deg,rgba(100,220,130,0.2),rgba(100,220,130,0.08))","rgba(100,220,130,0.4)","#7dcea0")}>
                {busy ? "..." : "✓ Confirm appointment"}
              </button>
              <button disabled={busy} onClick={() => { setMode("reschedule"); setRsDate(a?.scheduled_date || ""); setRsTime(a?.scheduled_time?.slice(0,5) || ""); }}
                style={btn("rgba(126,184,232,0.08)","rgba(126,184,232,0.3)","#7eb8e8")}>
                🕑 Request a new time
              </button>
              <button disabled={busy} onClick={() => setMode("cancel")}
                style={btn("rgba(232,126,126,0.06)","rgba(232,126,126,0.25)","#e87e7e")}>
                Cancel appointment
              </button>
            </div>
          )}

          {/* Reschedule form */}
          {!cancelled && mode === "reschedule" && (
            <div style={{ background:"rgba(255,255,255,0.022)", border:"1px solid var(--line)", borderRadius:14, padding:"18px" }}>
              <div style={{ fontSize:13, fontWeight:700, color:"#fff", marginBottom:12, fontFamily:"'Syne',sans-serif" }}>Request a new time</div>
              <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:8, marginBottom:12 }}>
                <input type="date" value={rsDate} onChange={e2 => setRsDate(e2.target.value)} style={{ ...IS, colorScheme:"dark" }} />
                <input type="time" value={rsTime} onChange={e2 => setRsTime(e2.target.value)} style={{ ...IS, colorScheme:"dark" }} />
              </div>
              <div style={{ display:"flex", gap:8 }}>
                <button disabled={busy || !rsDate} onClick={() => act("reschedule", { proposedDate: rsDate, proposedTime: rsTime || null }, "New time requested — your electrician will confirm.")}
                  style={{ ...btn("linear-gradient(135deg,rgba(126,184,232,0.2),rgba(126,184,232,0.08))","rgba(126,184,232,0.4)","#7eb8e8"), opacity: rsDate ? 1 : 0.5 }}>
                  {busy ? "..." : "Send request"}
                </button>
                <button disabled={busy} onClick={() => setMode(null)} style={btn("transparent","var(--line-strong)","rgba(255,255,255,0.5)")}>Back</button>
              </div>
            </div>
          )}

          {/* Cancel form */}
          {!cancelled && mode === "cancel" && (
            <div style={{ background:"rgba(255,255,255,0.022)", border:"1px solid var(--line)", borderRadius:14, padding:"18px" }}>
              <div style={{ fontSize:13, fontWeight:700, color:"#fff", marginBottom:12, fontFamily:"'Syne',sans-serif" }}>Cancel this appointment?</div>
              <textarea placeholder="Reason (optional)" value={reason} onChange={e2 => setReason(e2.target.value)} rows={3} style={{ ...IS, resize:"vertical", lineHeight:1.5, marginBottom:12 }} />
              <div style={{ display:"flex", gap:8 }}>
                <button disabled={busy} onClick={() => act("cancel", { reason }, "Appointment cancelled. Your electrician has been notified.")}
                  style={btn("rgba(232,126,126,0.1)","rgba(232,126,126,0.4)","#e87e7e")}>
                  {busy ? "..." : "Confirm cancellation"}
                </button>
                <button disabled={busy} onClick={() => setMode(null)} style={btn("transparent","var(--line-strong)","rgba(255,255,255,0.5)")}>Back</button>
              </div>
            </div>
          )}

          <div style={{ textAlign:"center", marginTop:24, fontSize:10, color:"rgba(255,255,255,0.25)" }}>Powered by Wireway</div>
        </div>
      </div>
    </>
  );
}

function Row({ label, value }) {
  return (
    <div style={{ display:"flex", justifyContent:"space-between", gap:12, paddingBottom:10, borderBottom:"1px solid var(--line)" }}>
      <span style={{ fontSize:12, color:"rgba(255,255,255,0.4)" }}>{label}</span>
      <span style={{ fontSize:13, color:"#fff", fontWeight:600, textAlign:"right" }}>{value}</span>
    </div>
  );
}
