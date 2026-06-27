/* eslint-disable react-hooks/exhaustive-deps */
// src/PayDrawsPublicPage.jsx — PUBLIC tap-to-pay / homeowner portal  ·  Phase 2 · Feature 1
// Opened via a shareable link /pay/:jobId — no login. Shows the job's billing
// schedule and lets the homeowner pay each draw online (deposit → draws → final).
import { useState, useEffect } from "react";

const fmt = (n) => "$" + (Number(n) || 0).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const STATUS = {
  pending:  { c: "rgba(255,255,255,0.4)", label: "Not yet due" },
  invoiced: { c: "#e8c97a", label: "Due now" },
  paid:     { c: "#7dcea0", label: "Paid ✓" },
};

export default function PayDrawsPublicPage({ jobId }) {
  const [data,    setData]    = useState(null);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState("");
  const [paying,  setPaying]  = useState(null);

  const justPaid = new URLSearchParams(window.location.search).get("paid") === "1";

  const load = () => {
    fetch(`/api/pay-draw?jobId=${encodeURIComponent(jobId)}`)
      .then((r) => r.json())
      .then((d) => { if (d.error) setError(d.error); else setData(d); setLoading(false); })
      .catch(() => { setError("Could not load."); setLoading(false); });
  };
  useEffect(() => { if (jobId) load(); }, [jobId]);

  const pay = async (drawId) => {
    setPaying(drawId);
    try {
      const res = await fetch("/api/pay-draw", {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ drawId }),
      });
      const d = await res.json();
      if (d.url) window.location.href = d.url;
      else { setError(d.error || "Could not start payment."); setPaying(null); }
    } catch { setError("Payment error."); setPaying(null); }
  };

  const center = { minHeight: "100vh", display: "flex", alignItems: "center", justifyContent: "center", background: "var(--bg0)" };
  if (loading) return <div style={center}><div style={{ color: "rgba(255,255,255,0.4)", fontFamily: "sans-serif", fontSize: 14 }}>Loading…</div></div>;
  if (error && !data) return <div style={center}><div style={{ color: "#e87e7e", fontFamily: "sans-serif", fontSize: 14 }}>{error}</div></div>;

  const { job, company, draws = [], can_pay, ref } = data;
  const outstanding = draws.filter((d) => d.status !== "paid").reduce((s, d) => s + d.net, 0);
  const refUrl = `${window.location.origin}/?ref=${encodeURIComponent(ref || "")}&src=pay`;

  return (
    <>
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=DM+Mono:wght@400;500&family=Syne:wght@700;800&family=DM+Sans:wght@400;500;600&display=swap');
        *{box-sizing:border-box;margin:0;padding:0} body{background:var(--bg0)}
      `}</style>
      <div style={{ minHeight: "100vh", background: "var(--bg-scene)", fontFamily: "'DM Sans',sans-serif", color: "#fff", paddingBottom: 60 }}>
        {/* Header */}
        <div style={{ borderBottom: "1px solid var(--line)", padding: "14px 20px", background: "rgba(10,10,12,0.9)", backdropFilter: "blur(20px)" }}>
          <div style={{ maxWidth: 600, margin: "0 auto", display: "flex", alignItems: "center", gap: 10 }}>
            {company?.logo_url
              ? <img src={company.logo_url} alt="" style={{ height: 32, borderRadius: 5, objectFit: "contain" }} />
              : <div style={{ width: 30, height: 30, borderRadius: 6, background: "linear-gradient(135deg,var(--accent),#c9a84c)", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 14, fontWeight: 800, color: "var(--bg0)" }}>{(company?.name || "W")[0].toUpperCase()}</div>}
            <div>
              <div style={{ fontFamily: "'Syne',sans-serif", fontSize: 15, fontWeight: 800 }}>{company?.name || "Payment"}</div>
              <div style={{ fontSize: 10, color: "rgba(255,255,255,0.35)" }}>{job?.title}{job?.client_name ? ` · ${job.client_name}` : ""}</div>
            </div>
          </div>
        </div>

        <div style={{ maxWidth: 600, margin: "0 auto", padding: "24px 20px" }}>
          {justPaid && (
            <div style={{ textAlign: "center", padding: "12px", background: "rgba(100,220,130,0.08)", border: "1px solid rgba(100,220,130,0.25)", borderRadius: 10, marginBottom: 18, color: "#7dcea0", fontSize: 13, fontWeight: 700 }}>
              ✓ Payment received — thank you!
            </div>
          )}

          <div style={{ background: "linear-gradient(135deg,rgba(var(--accent-rgb),0.07),rgba(255,255,255,0.02))", border: "1px solid rgba(var(--accent-rgb),0.2)", borderRadius: 14, padding: "18px", marginBottom: 16 }}>
            <div style={{ fontSize: 10, color: "rgba(255,255,255,0.4)", textTransform: "uppercase", letterSpacing: "0.1em" }}>Balance due</div>
            <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 30, fontWeight: 500, color: "var(--accent)", marginTop: 4 }}>{fmt(outstanding)}</div>
          </div>

          {!can_pay && draws.some((d) => d.status !== "paid") && (
            <div style={{ marginBottom: 14, fontSize: 12, color: "rgba(232,201,122,0.85)", background: "rgba(232,201,122,0.05)", border: "1px solid rgba(232,201,122,0.15)", borderRadius: 9, padding: "10px 12px" }}>
              Online payment isn't enabled for this contractor yet — they'll reach out with payment details.
            </div>
          )}
          {error && <div style={{ marginBottom: 12, fontSize: 12, color: "#e87e7e" }}>{error}</div>}

          {/* Draw schedule */}
          <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
            {draws.map((d) => {
              const st = STATUS[d.status] || STATUS.pending;
              const payable = can_pay && d.status !== "paid";
              return (
                <div key={d.id} style={{ background: "var(--card)", border: "1px solid var(--line)", borderRadius: 12, padding: "14px 16px" }}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 10 }}>
                    <div style={{ minWidth: 0 }}>
                      <div style={{ fontSize: 14, fontWeight: 600 }}>{d.label}</div>
                      <div style={{ fontSize: 11, color: st.c, marginTop: 2 }}>{st.label}{d.due_date ? ` · due ${d.due_date}` : ""}</div>
                    </div>
                    <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 17, fontWeight: 600, flexShrink: 0 }}>{fmt(d.net)}</div>
                  </div>
                  {payable && (
                    <button onClick={() => pay(d.id)} disabled={paying === d.id}
                      style={{ width: "100%", marginTop: 12, padding: "12px", background: paying === d.id ? "rgba(99,102,241,0.06)" : "linear-gradient(135deg,rgba(99,102,241,0.25),rgba(139,92,246,0.12))", border: "1px solid rgba(99,102,241,0.35)", borderRadius: 10, color: "#818cf8", fontSize: 13, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>
                      {paying === d.id ? "Opening payment…" : `Pay ${fmt(d.net)}`}
                    </button>
                  )}
                </div>
              );
            })}
            {draws.length === 0 && <div style={{ textAlign: "center", color: "rgba(255,255,255,0.3)", fontSize: 13, padding: "30px 0" }}>No payment schedule posted yet.</div>}
          </div>

          {can_pay && (
            <div style={{ textAlign: "center", fontSize: 10, color: "rgba(255,255,255,0.2)", marginTop: 12 }}>
              Secure payment powered by Stripe · Your card info is never shared
            </div>
          )}

          {/* Soft lead-gen surface (Feature 5 enhances this) */}
          <div style={{ textAlign: "center", marginTop: 30, fontSize: 10, color: "rgba(255,255,255,0.18)", letterSpacing: "0.04em" }}>
            <a href={refUrl} style={{ color: "rgba(var(--accent-rgb),0.5)", textDecoration: "none" }}>
              Powered by Wireway — get quotes &amp; invoices like this
            </a>
          </div>
        </div>
      </div>
    </>
  );
}
