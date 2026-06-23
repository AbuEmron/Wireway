/* eslint-disable react-hooks/exhaustive-deps */
// src/QuotePublicPage.jsx
// Client-facing quote view — opened via a shareable link
// No login required. Client can view, accept, and pay.

import { useState, useEffect } from "react";

export default function QuotePublicPage({ quoteId }) {
  const [data,     setData]     = useState(null);
  const [loading,  setLoading]  = useState(true);
  const [error,    setError]    = useState("");
  const [sigName,  setSigName]  = useState("");
  const [sigDate,  setSigDate]  = useState(new Date().toLocaleDateString());
  const [signing,  setSigning]  = useState(false);
  const [signed,   setSigned]   = useState(false);
  const [paying,   setPaying]   = useState(false);

  useEffect(() => {
    if (!quoteId) return;
    fetch(`/api/quote/${quoteId}`)
      .then(r => r.json())
      .then(d => { setData(d); setLoading(false); })
      .catch(() => { setError("Could not load quote."); setLoading(false); });
  }, [quoteId]);

  const acceptQuote = async () => {
    if (!sigName.trim()) return;
    setSigning(true);
    try {
      const res = await fetch(`/api/quote/${quoteId}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ action: "accept", sigName, sigDate }),
      });
      const d = await res.json();
      if (d.success) setSigned(true);
      else setError(d.error || "Failed to sign quote.");
    } catch { setError("Network error."); }
    finally { setSigning(false); }
  };

  const requestStripePayment = async () => {
    if (!data?.quote) return;
    setPaying(true);
    try {
      const res = await fetch("/api/create-checkout", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          quoteNumber:   data.quote.quote_number,
          clientName:    data.quote.client_name,
          clientEmail:   data.quote.client_email,
          jobName:       data.quote.job_name,
          total:         data.quote.total,
          depositOnly:   data.quote.deposit_only,
          depositPercent:data.quote.deposit_percent || 50,
          companyName:   data.electrician?.company_name,
          lineItems:     [],
        }),
      });
      const d = await res.json();
      if (d.url) window.location.href = d.url;
      else setError(d.error || "Could not open payment.");
    } catch { setError("Payment error."); }
    finally { setPaying(false); }
  };

  const q = data?.quote;
  const e = data?.electrician;

  const IS = {
    background:"var(--card)", border:"1px solid var(--line-strong)",
    borderRadius:8, padding:"10px 13px", fontSize:14, color:"#fff",
    fontFamily:"inherit", width:"100%", outline:"none",
  };

  if (loading) return (
    <div style={{ minHeight:"100vh", display:"flex", alignItems:"center", justifyContent:"center", background:"var(--bg0)" }}>
      <div style={{ color:"rgba(255,255,255,0.4)", fontFamily:"sans-serif", fontSize:14 }}>Loading quote...</div>
    </div>
  );

  if (error && !data) return (
    <div style={{ minHeight:"100vh", display:"flex", alignItems:"center", justifyContent:"center", background:"var(--bg0)" }}>
      <div style={{ color:"#e87e7e", fontFamily:"sans-serif", fontSize:14 }}>{error}</div>
    </div>
  );

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
          <div style={{ maxWidth:600, margin:"0 auto", display:"flex", alignItems:"center", justifyContent:"space-between" }}>
            <div style={{ display:"flex", alignItems:"center", gap:10 }}>
              {e?.logo_url
                ? <img src={e.logo_url} alt="logo" style={{ height:32, width:"auto", borderRadius:5, objectFit:"contain" }} />
                : <div style={{ width:30, height:30, borderRadius:6, background:"linear-gradient(135deg,var(--accent),#c9a84c)", display:"flex", alignItems:"center", justifyContent:"center", fontSize:14, fontWeight:800, color:"var(--bg0)" }}>
                    {(e?.company_name || "W")[0].toUpperCase()}
                  </div>
              }
              <div>
                <div style={{ fontFamily:"'Syne',sans-serif", fontSize:15, fontWeight:800, color:"#fff" }}>{e?.company_name || "Electrical Estimate"}</div>
                {e?.license_number && <div style={{ fontSize:10, color:"rgba(255,255,255,0.35)" }}>Lic: {e.license_number}</div>}
              </div>
            </div>
            <div style={{ fontFamily:"'DM Mono',monospace", fontSize:22, fontWeight:500, color:"var(--accent)" }}>${q?.total?.toLocaleString()}</div>
          </div>
        </div>

        <div style={{ maxWidth:600, margin:"0 auto", padding:"24px 20px" }}>

          {/* Status badge */}
          {q?.status === "accepted" || signed ? (
            <div style={{ textAlign:"center", padding:"12px", background:"rgba(100,220,130,0.08)", border:"1px solid rgba(100,220,130,0.25)", borderRadius:10, marginBottom:20, color:"#7dcea0", fontSize:13, fontWeight:700 }}>
              ✓ Quote Accepted {q?.sig_name ? `— Signed by ${q.sig_name}` : ""}
            </div>
          ) : q?.status === "paid" ? (
            <div style={{ textAlign:"center", padding:"12px", background:"rgba(100,220,130,0.08)", border:"1px solid rgba(100,220,130,0.25)", borderRadius:10, marginBottom:20, color:"#7dcea0", fontSize:13, fontWeight:700 }}>
              ✓ Paid in Full — Thank you!
            </div>
          ) : null}

          {/* Quote header */}
          <div style={{ background:"var(--card)", border:"1px solid var(--line)", borderRadius:14, padding:"18px", marginBottom:14 }}>
            <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:"6px 20px", marginBottom:14, paddingBottom:14, borderBottom:"1px solid var(--line)" }}>
              {[
                { label:"Quote #",   val: q?.quote_number },
                { label:"Date",      val: new Date(q?.created_at).toLocaleDateString() },
                { label:"Client",    val: q?.client_name },
                { label:"Job",       val: q?.job_name },
                q?.invoice_due_date && { label:"Due", val: q.invoice_due_date },
              ].filter(Boolean).map(row => (
                <div key={row.label}>
                  <div style={{ fontSize:9, color:"rgba(255,255,255,0.3)", textTransform:"uppercase", letterSpacing:"0.08em" }}>{row.label}</div>
                  <div style={{ fontSize:13, color:"rgba(255,255,255,0.8)", fontWeight:600, marginTop:2 }}>{row.val || "—"}</div>
                </div>
              ))}
            </div>

            {/* Company contact */}
            <div style={{ fontSize:11, color:"rgba(255,255,255,0.35)", lineHeight:1.8 }}>
              {[e?.company_phone, e?.company_email, e?.company_address].filter(Boolean).join("  ·  ")}
            </div>
          </div>

          {/* Services */}
          <div style={{ background:"rgba(255,255,255,0.022)", border:"1px solid var(--line)", borderRadius:13, padding:"14px 16px", marginBottom:14 }}>
            <div style={{ fontSize:9, color:"rgba(255,255,255,0.28)", textTransform:"uppercase", letterSpacing:"0.1em", marginBottom:12 }}>Services</div>
            {q?.entries && Object.keys(q.entries).filter(k => q.entries[k]?.qty > 0).length === 0 && (
              <div style={{ fontSize:12, color:"rgba(255,255,255,0.3)" }}>No line items</div>
            )}
            {q?.entries && Object.entries(q.entries).filter(([,v]) => v?.qty > 0).map(([id, entry]) => (
              <div key={id} style={{ display:"flex", justifyContent:"space-between", padding:"7px 0", borderBottom:"1px solid var(--line)", fontSize:12 }}>
                <span style={{ color:"rgba(255,255,255,0.7)" }}>{id.replace(/_/g," ")} × {entry.qty}</span>
                <span style={{ fontFamily:"'DM Mono',monospace", color:"rgba(255,255,255,0.5)" }}></span>
              </div>
            ))}
          </div>

          {/* Totals */}
          <div style={{ background:"linear-gradient(135deg,rgba(var(--accent-rgb),0.07),rgba(255,255,255,0.02))", border:"1px solid rgba(var(--accent-rgb),0.2)", borderRadius:13, padding:"16px", marginBottom:20 }}>
            {[
              q?.show_materials && { label:"Materials", val: q?.total_material },
              { label:`Labor (${q?.total_hours?.toFixed(1)} hrs)`, val: q?.total_labor },
              { label:`Markup`, val: q?.total_markup },
              q?.tax_enabled && { label:`Tax`, val: q?.total_tax },
            ].filter(Boolean).map(row => (
              <div key={row.label} style={{ display:"flex", justifyContent:"space-between", padding:"4px 0", fontSize:12 }}>
                <span style={{ color:"rgba(255,255,255,0.4)" }}>{row.label}</span>
                <span style={{ fontFamily:"'DM Mono',monospace", color:"rgba(255,255,255,0.55)" }}>${Number(row.val||0).toLocaleString()}</span>
              </div>
            ))}
            <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", paddingTop:12, marginTop:8, borderTop:"1px solid rgba(var(--accent-rgb),0.18)" }}>
              <span style={{ fontFamily:"'Syne',sans-serif", fontSize:15, fontWeight:800 }}>Total</span>
              <span style={{ fontFamily:"'DM Mono',monospace", fontSize:24, fontWeight:500, color:"var(--accent)" }}>${Number(q?.total||0).toLocaleString()}</span>
            </div>
          </div>

          {/* Notes */}
          {q?.notes && (
            <div style={{ marginBottom:20, padding:"12px 14px", background:"var(--card)", borderRadius:10, fontSize:12, color:"rgba(255,255,255,0.5)", lineHeight:1.7 }}>
              <div style={{ fontSize:9, color:"rgba(255,255,255,0.25)", textTransform:"uppercase", letterSpacing:"0.08em", marginBottom:4 }}>Notes</div>
              {q.notes}
            </div>
          )}

          {/* Terms */}
          {e?.terms && (
            <div style={{ marginBottom:20, padding:"12px 14px", background:"var(--card)", borderRadius:10, fontSize:11, color:"rgba(255,255,255,0.3)", lineHeight:1.7 }}>
              <div style={{ fontSize:9, color:"rgba(255,255,255,0.2)", textTransform:"uppercase", letterSpacing:"0.08em", marginBottom:4 }}>Terms</div>
              {e.terms}
            </div>
          )}

          {/* Accept + Pay — only if not already signed/paid */}
          {q?.status !== "paid" && !signed && (
            <>
              {/* Signature */}
              {q?.status !== "accepted" && (
                <div style={{ background:"var(--card)", border:"1px solid var(--line)", borderRadius:13, padding:"18px", marginBottom:12 }}>
                  <div style={{ fontSize:13, fontWeight:700, color:"#fff", marginBottom:4, fontFamily:"'Syne',sans-serif" }}>Accept this Quote</div>
                  <div style={{ fontSize:11, color:"rgba(255,255,255,0.4)", marginBottom:14, lineHeight:1.6 }}>
                    By signing below you authorize {e?.company_name || "the electrician"} to proceed with the described work.
                  </div>
                  <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:8, marginBottom:12 }}>
                    <div>
                      <div style={{ fontSize:10, color:"rgba(255,255,255,0.3)", marginBottom:5 }}>Your full name</div>
                      <input placeholder="Type name to accept" value={sigName} onChange={e => setSigName(e.target.value)} style={IS} />
                    </div>
                    <div>
                      <div style={{ fontSize:10, color:"rgba(255,255,255,0.3)", marginBottom:5 }}>Date</div>
                      <input type="date" value={sigDate} onChange={e => setSigDate(e.target.value)} style={{ ...IS, colorScheme:"dark" }} />
                    </div>
                  </div>
                  {error && <div style={{ fontSize:11, color:"#e87e7e", marginBottom:10 }}>{error}</div>}
                  <button onClick={acceptQuote} disabled={!sigName || signing} style={{ width:"100%", padding:"13px", background: sigName ? "linear-gradient(135deg,rgba(100,220,130,0.2),rgba(100,220,130,0.08))" : "rgba(255,255,255,0.04)", border: sigName ? "1px solid rgba(100,220,130,0.4)" : "1px solid var(--line-strong)", borderRadius:10, color: sigName ? "#7dcea0" : "rgba(255,255,255,0.25)", fontSize:13, fontWeight:700, cursor: sigName ? "pointer" : "default", fontFamily:"inherit" }}>
                    {signing ? "Signing..." : "✍ Accept & Sign Quote"}
                  </button>
                </div>
              )}

              {/* Pay button */}
              <button onClick={requestStripePayment} disabled={paying} style={{ width:"100%", padding:"14px", background: paying ? "rgba(99,102,241,0.06)" : "linear-gradient(135deg,rgba(99,102,241,0.25),rgba(139,92,246,0.12))", border:"1px solid rgba(99,102,241,0.35)", borderRadius:11, color: paying ? "rgba(129,140,248,0.4)" : "#818cf8", fontSize:14, fontWeight:700, cursor: paying ? "default" : "pointer", fontFamily:"inherit" }}>
                {paying ? "Opening payment..." : `⚡ Pay ${q?.deposit_only ? `${q.deposit_percent}% Deposit` : "Full Amount"} — $${q?.deposit_only ? Math.round((q?.total||0) * (q?.deposit_percent||50) / 100).toLocaleString() : Number(q?.total||0).toLocaleString()}`}
              </button>

              <div style={{ textAlign:"center", fontSize:10, color:"rgba(255,255,255,0.2)", marginTop:8 }}>
                Secure payment powered by Stripe · Your card info is never shared
              </div>
            </>
          )}

          <div style={{ textAlign:"center", marginTop:32, fontSize:10, color:"rgba(255,255,255,0.15)", letterSpacing:"0.05em" }}>
            Powered by <span style={{ color:"rgba(var(--accent-rgb),0.5)" }}>Wireway</span> · Professional Electrical Estimating
          </div>
        </div>
      </div>
    </>
  );
}
