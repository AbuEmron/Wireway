/* eslint-disable react-hooks/exhaustive-deps */
// src/ReceivablesView.jsx — Get-Paid-Faster A/R aging + one-tap reminders  ·  Feature 6
import { useState, useEffect, useCallback } from "react";
import {
  getReceivables, agingBuckets, smsHref, emailHref, logReminder,
} from "./lib/ar";

const fmt = (n) => "$" + Math.round(Number(n) || 0).toLocaleString("en-US");
const fmt2 = (n) => "$" + (Number(n) || 0).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const GREEN = "#7dcea0", RED = "#e87e7e", GOLD = "#e8c97a", BLUE = "#7eb8e8";

const bucketColor = (days) =>
  days <= 0 ? "rgba(255,255,255,0.5)" : days <= 30 ? GOLD : days <= 60 ? "#e8b87e" : RED;
const agoText = (iso) => {
  if (!iso) return null;
  const d = Math.floor((Date.now() - new Date(iso).getTime()) / 86400000);
  return d <= 0 ? "today" : `${d}d ago`;
};

export default function ReceivablesView({ user, profile, company = {}, onClose }) {
  const [items,   setItems]   = useState([]);
  const [loading, setLoading] = useState(true);
  const [msg,     setMsg]     = useState("");
  const companyName = company.name || profile?.company_name || "";

  const flash = (m) => { setMsg(m); setTimeout(() => setMsg(""), 2500); };

  const load = useCallback(async () => {
    if (!user?.id) return;
    setLoading(true);
    setItems(await getReceivables(user.id));
    setLoading(false);
  }, [user?.id]);

  useEffect(() => { load(); }, [load]);

  const remind = async (item, channel) => {
    const href = channel === "sms" ? smsHref(item, companyName) : emailHref(item, companyName);
    window.open(href);
    await logReminder(user.id, item, channel);
    flash(`${channel === "sms" ? "Text" : "Email"} opened — reminder logged.`);
    load();
  };

  const b = agingBuckets(items);
  const overdueTotal = b.d1_30 + b.d31_60 + b.d61_90 + b.d90;

  const wrap = { position: "fixed", inset: 0, zIndex: 360, background: "rgba(0,0,0,0.82)", backdropFilter: "blur(8px)", overflowY: "auto", display: "flex", justifyContent: "center", alignItems: "flex-start", padding: "24px 16px" };
  const panel = { background: "#111115", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 18, width: "100%", maxWidth: 760, padding: "24px" };

  const BUCKETS = [
    { label: "Current", val: b.current, c: "rgba(255,255,255,0.6)" },
    { label: "1–30", val: b.d1_30, c: GOLD },
    { label: "31–60", val: b.d31_60, c: "#e8b87e" },
    { label: "61–90", val: b.d61_90, c: RED },
    { label: "90+", val: b.d90, c: RED },
  ];

  return (
    <div style={wrap}>
      <div style={panel}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 18 }}>
          <div>
            <div style={{ fontFamily: "'Syne',sans-serif", fontSize: 17, fontWeight: 800, color: "#fff" }}>Get Paid</div>
            <div style={{ fontSize: 11, color: "rgba(255,255,255,0.35)", marginTop: 2 }}>A/R aging · one-tap reminders with a pay link</div>
          </div>
          <button onClick={onClose} style={{ background: "transparent", border: "none", color: "rgba(255,255,255,0.4)", fontSize: 22, cursor: "pointer" }}>✕</button>
        </div>

        {msg && <div style={{ marginBottom: 12, fontSize: 11, color: GREEN, background: "rgba(125,206,160,0.08)", border: "1px solid rgba(125,206,160,0.2)", borderRadius: 7, padding: "7px 11px" }}>{msg}</div>}

        {/* Headline */}
        <div style={{ background: `linear-gradient(135deg, ${overdueTotal > 0 ? "rgba(232,126,126,0.1)" : "rgba(125,206,160,0.08)"}, rgba(255,255,255,0.02))`, border: `1px solid ${overdueTotal > 0 ? "rgba(232,126,126,0.25)" : "rgba(125,206,160,0.2)"}`, borderRadius: 14, padding: "16px 18px", marginBottom: 14 }}>
          <div style={{ fontSize: 10, color: "rgba(255,255,255,0.4)", textTransform: "uppercase", letterSpacing: "0.1em" }}>Owed to you</div>
          <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 30, fontWeight: 700, color: "#fff", marginTop: 4 }}>{loading ? "…" : fmt(b.total)}</div>
          {overdueTotal > 0 && <div style={{ fontSize: 11, color: RED, marginTop: 2, fontFamily: "'DM Mono',monospace" }}>{fmt(overdueTotal)} overdue</div>}
        </div>

        {/* Aging buckets */}
        <div style={{ display: "grid", gridTemplateColumns: "repeat(5,1fr)", gap: 6, marginBottom: 14 }}>
          {BUCKETS.map((x) => (
            <div key={x.label} style={{ background: "rgba(255,255,255,0.025)", border: "1px solid rgba(255,255,255,0.07)", borderRadius: 9, padding: "9px 8px", textAlign: "center" }}>
              <div style={{ fontSize: 9, color: "rgba(255,255,255,0.3)", marginBottom: 4 }}>{x.label}</div>
              <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 13, fontWeight: 600, color: x.val > 0 ? x.c : "rgba(255,255,255,0.2)" }}>{fmt(x.val)}</div>
            </div>
          ))}
        </div>

        {/* Cron flag — honest about what's automated vs. manual */}
        <div style={{ marginBottom: 16, fontSize: 10.5, color: "rgba(232,201,122,0.85)", background: "rgba(232,201,122,0.05)", border: "1px solid rgba(232,201,122,0.15)", borderRadius: 9, padding: "9px 12px", lineHeight: 1.6 }}>
          Reminders below send instantly from your phone (text/email) and are logged. <b>Fully-automated daily reminders</b> would need Vercel Cron (Pro plan) plus an SMS/email provider (e.g. Twilio/Resend) — not set up yet, so nothing sends on its own.
        </div>

        {/* List */}
        {loading ? (
          <div style={{ textAlign: "center", color: "rgba(255,255,255,0.3)", fontSize: 12, padding: "30px 0" }}>Loading…</div>
        ) : items.length === 0 ? (
          <div style={{ textAlign: "center", color: "rgba(255,255,255,0.25)", padding: "36px 0" }}>
            <div style={{ fontSize: 28, marginBottom: 8 }}>🎉</div>
            <div style={{ fontSize: 13 }}>Nothing outstanding</div>
            <div style={{ fontSize: 11, marginTop: 4, color: "rgba(255,255,255,0.18)" }}>Sent invoices &amp; invoiced draws that aren't paid will appear here.</div>
          </div>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
            {items.map((it) => {
              const overdue = it.days > 0;
              return (
                <div key={`${it.kind}-${it.id}`} style={{ display: "flex", alignItems: "center", gap: 10, padding: "11px 13px", background: "rgba(255,255,255,0.022)", border: `1px solid ${overdue ? "rgba(232,126,126,0.18)" : "rgba(255,255,255,0.06)"}`, borderRadius: 10 }}>
                  <div style={{ width: 3, height: 38, borderRadius: 2, background: bucketColor(it.days), flexShrink: 0 }} />
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 12.5, color: "#fff", fontWeight: 600, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{it.title}</div>
                    <div style={{ fontSize: 10, color: "rgba(255,255,255,0.35)", fontFamily: "'DM Mono',monospace" }}>
                      {it.client} · due {it.due_date} · {overdue ? `${it.days}d overdue` : "current"}
                      {it.lastRemindedAt && <span style={{ color: "rgba(232,201,122,0.7)" }}> · reminded {agoText(it.lastRemindedAt)}</span>}
                    </div>
                  </div>
                  <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 14, fontWeight: 700, color: overdue ? RED : "#fff", flexShrink: 0 }}>{fmt2(it.amount)}</div>
                  <div style={{ display: "flex", gap: 4, flexShrink: 0 }}>
                    {it.phone && (
                      <button onClick={() => remind(it, "sms")} title="Text reminder" style={{ padding: "5px 9px", borderRadius: 6, border: "1px solid rgba(168,232,126,0.3)", background: "rgba(168,232,126,0.08)", color: "#a8e87e", fontSize: 10, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>Text</button>
                    )}
                    {it.email && (
                      <button onClick={() => remind(it, "email")} title="Email reminder" style={{ padding: "5px 9px", borderRadius: 6, border: "1px solid rgba(126,184,232,0.3)", background: "rgba(126,184,232,0.08)", color: BLUE, fontSize: 10, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>Email</button>
                    )}
                    {it.payUrl && (
                      <button onClick={() => { navigator.clipboard.writeText(it.payUrl); flash("Pay link copied."); }} title="Copy pay link" style={{ width: 28, height: 28, borderRadius: 6, border: "1px solid rgba(255,255,255,0.12)", background: "transparent", color: "rgba(255,255,255,0.4)", fontSize: 11, cursor: "pointer" }}>🔗</button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
