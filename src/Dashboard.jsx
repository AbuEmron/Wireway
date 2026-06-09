// src/Dashboard.jsx
// Business dashboard — shown as home screen after login
// Revenue, profit, pending quotes, upcoming jobs, quick quote cards

import { useState, useEffect } from "react";
import { getQuotes, getJobs } from "./lib/supabase";

const QUICK_JOBS = [
  {
    id: "panel_upgrade",
    label: "Panel Upgrade",
    icon: "⚡",
    color: "#e8c97a",
    desc: "100A–400A service upgrade",
    services: ["panel_200a", "surge_protector", "grounding_electrode"],
  },
  {
    id: "ev_charger",
    label: "EV Charger",
    icon: "🔌",
    color: "#7eb8e8",
    desc: "Level 2 EVSE installation",
    services: ["ev_charger_l2", "dedicated_circuit"],
  },
  {
    id: "generator",
    label: "Generator",
    icon: "🔋",
    color: "#a8e87e",
    desc: "Standby + transfer switch",
    services: ["generator_install", "generator_transfer2", "generator_grounding"],
  },
  {
    id: "hot_tub",
    label: "Hot Tub",
    icon: "♨️",
    color: "#e87eb8",
    desc: "240V/50A + GFCI + bonding",
    services: ["hot_tub", "pool_bonding"],
  },
  {
    id: "service_call",
    label: "Service Call",
    icon: "🔧",
    color: "#e8b87e",
    desc: "Troubleshoot & repair",
    services: ["outlet_gfci", "outlet_standard"],
  },
  {
    id: "lighting",
    label: "Lighting Upgrade",
    icon: "💡",
    color: "#e8e87e",
    desc: "Recessed, dimmer, fixture",
    services: ["recessed_led", "dimmer_switch", "fixture_replacement"],
  },
  {
    id: "gfci_afci",
    label: "GFCI / AFCI",
    icon: "🛡",
    color: "#b87ee8",
    desc: "Code-required protection",
    services: ["outlet_gfci", "afci_breaker"],
  },
  {
    id: "rewire",
    label: "Rewire / Circuits",
    icon: "📡",
    color: "#7ee8e8",
    desc: "New circuits + wiring",
    services: ["new_circuit_20a", "new_circuit_15a"],
  },
  {
    id: "pool",
    label: "Pool / Spa",
    icon: "🏊",
    color: "#7ec8e8",
    desc: "Bonding, GFCI, lights",
    services: ["pool_bonding", "pool_pump_circuit", "pool_light_under", "pool_gfci"],
  },
  {
    id: "smart_home",
    label: "Smart Home",
    icon: "📱",
    color: "#c8a8e8",
    desc: "Smart switches + hub",
    services: ["smart_switch", "dedicated_circuit"],
  },
];

const STATUS_COLORS = {
  draft:        { bg:"rgba(255,255,255,0.06)",      text:"rgba(255,255,255,0.5)",  label:"Draft" },
  sent:         { bg:"rgba(126,184,232,0.12)",       text:"#7eb8e8",               label:"Sent" },
  accepted:     { bg:"rgba(100,220,130,0.1)",        text:"#7dcea0",               label:"Accepted" },
  deposit_paid: { bg:"rgba(168,232,126,0.1)",        text:"#a8e87e",               label:"Deposit Paid" },
  paid:         { bg:"rgba(100,220,130,0.15)",       text:"#7dcea0",               label:"Paid" },
  invoiced:     { bg:"rgba(184,126,232,0.1)",        text:"#b87ee8",               label:"Invoiced" },
  completed:    { bg:"rgba(100,220,130,0.08)",       text:"#5db87e",               label:"Complete" },
  cancelled:    { bg:"rgba(232,126,126,0.08)",       text:"#e87e7e",               label:"Cancelled" },
};

function MetricCard({ label, value, sub, color = "#e8c97a", icon }) {
  return (
    <div style={{ background:"rgba(255,255,255,0.022)", border:"1px solid rgba(255,255,255,0.065)", borderRadius:12, padding:"14px 16px" }}>
      <div style={{ display:"flex", justifyContent:"space-between", alignItems:"flex-start", marginBottom:8 }}>
        <div style={{ fontSize:10, color:"rgba(255,255,255,0.35)", textTransform:"uppercase", letterSpacing:"0.08em" }}>{label}</div>
        {icon && <span style={{ fontSize:16, opacity:0.6 }}>{icon}</span>}
      </div>
      <div style={{ fontFamily:"'DM Mono',monospace", fontSize:22, fontWeight:500, color, letterSpacing:"-0.02em", lineHeight:1 }}>{value}</div>
      {sub && <div style={{ fontSize:10, color:"rgba(255,255,255,0.25)", marginTop:5, fontFamily:"'DM Mono',monospace" }}>{sub}</div>}
    </div>
  );
}

export default function Dashboard({ user, profile, onNewQuote, onLoadQuote, onShowAI, onOpenCalendar }) {
  const [quotes,    setQuotes]    = useState([]);
  const [jobs,      setJobs]      = useState([]);
  const [loading,   setLoading]   = useState(true);
  const [greeting,  setGreeting]  = useState("Good morning");

  useEffect(() => {
    const h = new Date().getHours();
    setGreeting(h < 12 ? "Good morning" : h < 17 ? "Good afternoon" : "Good evening");
  }, []);

  useEffect(() => {
    if (!user?.id) return;
    Promise.all([
      getQuotes(user.id),
      getJobs(user.id),
    ]).then(([qRes, jRes]) => {
      setQuotes(qRes.data || []);
      setJobs(jRes.data || []);
      setLoading(false);
    });
  }, [user?.id]);

  // Metrics
  const now     = new Date();
  const month   = now.getMonth();
  const year    = now.getFullYear();

  const thisMonthQuotes = quotes.filter(q => {
    const d = new Date(q.created_at);
    return d.getMonth() === month && d.getFullYear() === year;
  });

  const revenue     = thisMonthQuotes.filter(q => q.status === "paid" || q.status === "completed").reduce((a,q) => a + (q.total || 0), 0);
  const pending     = quotes.filter(q => q.status === "draft" || q.status === "sent").length;
  const accepted    = quotes.filter(q => q.status === "accepted" || q.status === "deposit_paid").length;
  const avgJob      = quotes.length ? (quotes.reduce((a,q) => a + (q.total||0), 0) / quotes.length) : 0;
  const paidTotal   = quotes.filter(q => q.status === "paid").reduce((a,q) => a + (q.total||0), 0);
  const acceptRate  = quotes.length ? Math.round((accepted / quotes.length) * 100) : 0;

  const upcomingJobs = jobs
    .filter(j => j.status === "scheduled" && j.scheduled_date >= now.toISOString().split("T")[0])
    .sort((a,b) => a.scheduled_date.localeCompare(b.scheduled_date))
    .slice(0, 3);

  const recentQuotes = [...quotes]
    .sort((a,b) => new Date(b.created_at) - new Date(a.created_at))
    .slice(0, 5);

  const companyName = profile?.company_name || user?.email?.split("@")[0] || "there";

  return (
    <>
      <style>{`
        @keyframes fadeUp{from{opacity:0;transform:translateY(10px)}to{opacity:1;transform:translateY(0)}}
        .qcard:hover{background:rgba(255,255,255,0.045)!important;border-color:rgba(255,255,255,0.1)!important;transform:translateY(-1px)}
        .qrow:hover{background:rgba(255,255,255,0.035)!important}
        .dash-ai:hover{background:linear-gradient(135deg,rgba(232,201,122,0.2),rgba(232,201,122,0.06))!important}
      `}</style>

      <div style={{ fontFamily:"'DM Sans',sans-serif", color:"#fff", paddingBottom:60 }}>

        {/* Greeting */}
        <div style={{ marginBottom:24, animation:"fadeUp 0.4s ease both" }}>
          <div style={{ fontSize:20, fontFamily:"'Syne',sans-serif", fontWeight:800, letterSpacing:"-0.02em", marginBottom:3 }}>
            {greeting}, {companyName.split(" ")[0]} 👋
          </div>
          <div style={{ fontSize:13, color:"rgba(255,255,255,0.35)" }}>
            {new Date().toLocaleDateString("en-US", { weekday:"long", month:"long", day:"numeric" })}
          </div>
        </div>

        {/* ── METRICS ── */}
        <div style={{ display:"grid", gridTemplateColumns:"repeat(auto-fit,minmax(140px,1fr))", gap:8, marginBottom:20, animation:"fadeUp 0.4s ease 0.05s both" }}>
          <MetricCard label="Revenue this month" value={loading ? "..." : `$${Math.round(revenue).toLocaleString()}`} icon="💰" color="#e8c97a" sub="paid jobs" />
          <MetricCard label="Pending quotes"      value={loading ? "..." : pending}    icon="📋" color="#7eb8e8" sub="awaiting response" />
          <MetricCard label="Accepted"            value={loading ? "..." : accepted}   icon="✓"  color="#7dcea0" sub={`${acceptRate}% win rate`} />
          <MetricCard label="Avg job size"        value={loading ? "..." : `$${Math.round(avgJob).toLocaleString()}`} icon="📊" color="#b87ee8" sub="all time" />
          <MetricCard label="Total quotes"        value={loading ? "..." : quotes.length} icon="📄" color="rgba(255,255,255,0.6)" sub="all time" />
          <MetricCard label="Upcoming jobs"       value={loading ? "..." : upcomingJobs.length} icon="📅" color="#a8e87e" sub="this week" />
        </div>

        {/* ── AI QUOTE BUILDER CTA ── */}
        <div onClick={onShowAI} className="dash-ai" style={{ background:"linear-gradient(135deg,rgba(232,201,122,0.1),rgba(232,201,122,0.04))", border:"1px solid rgba(232,201,122,0.25)", borderRadius:14, padding:"18px 20px", marginBottom:20, cursor:"pointer", transition:"all 0.2s", animation:"fadeUp 0.4s ease 0.08s both" }}>
          <div style={{ display:"flex", alignItems:"center", justifyContent:"space-between" }}>
            <div>
              <div style={{ display:"flex", alignItems:"center", gap:8, marginBottom:4 }}>
                <span style={{ fontSize:18 }}>⚡</span>
                <span style={{ fontFamily:"'Syne',sans-serif", fontSize:15, fontWeight:800, color:"#fff" }}>AI Quote Builder</span>
                <span style={{ fontSize:9, fontWeight:700, color:"#e8c97a", background:"rgba(232,201,122,0.15)", border:"1px solid rgba(232,201,122,0.3)", padding:"2px 6px", borderRadius:4, letterSpacing:"0.05em" }}>AI</span>
              </div>
              <div style={{ fontSize:12, color:"rgba(255,255,255,0.45)" }}>
                Describe any job → get a full NEC estimate in seconds
              </div>
            </div>
            <div style={{ fontSize:20, color:"rgba(232,201,122,0.5)" }}>→</div>
          </div>
        </div>

        {/* ── QUICK QUOTE CARDS ── */}
        <div style={{ marginBottom:20, animation:"fadeUp 0.4s ease 0.1s both" }}>
          <div style={{ fontSize:10, color:"rgba(255,255,255,0.28)", textTransform:"uppercase", letterSpacing:"0.1em", marginBottom:12 }}>Quick quote — tap to start</div>
          <div style={{ display:"grid", gridTemplateColumns:"repeat(auto-fill,minmax(140px,1fr))", gap:8 }}>
            {QUICK_JOBS.map(job => (
              <div key={job.id} className="qcard" onClick={() => onNewQuote(job)}
                style={{ background:"rgba(255,255,255,0.025)", border:"1px solid rgba(255,255,255,0.065)", borderRadius:12, padding:"14px 14px", cursor:"pointer", transition:"all 0.2s" }}>
                <div style={{ fontSize:22, marginBottom:8 }}>{job.icon}</div>
                <div style={{ fontSize:12, fontWeight:700, color:"#fff", marginBottom:3, lineHeight:1.3 }}>{job.label}</div>
                <div style={{ fontSize:10, color:"rgba(255,255,255,0.35)", lineHeight:1.4 }}>{job.desc}</div>
              </div>
            ))}
          </div>
        </div>

        {/* ── UPCOMING JOBS ── */}
        {upcomingJobs.length > 0 && (
          <div style={{ marginBottom:20, animation:"fadeUp 0.4s ease 0.12s both" }}>
            <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:10 }}>
              <div style={{ fontSize:10, color:"rgba(255,255,255,0.28)", textTransform:"uppercase", letterSpacing:"0.1em" }}>Upcoming jobs</div>
              <button onClick={onOpenCalendar} style={{ fontSize:11, color:"rgba(232,201,122,0.6)", background:"transparent", border:"none", cursor:"pointer", fontFamily:"inherit" }}>View calendar →</button>
            </div>
            {upcomingJobs.map(job => (
              <div key={job.id} className="qrow" style={{ display:"flex", alignItems:"center", gap:12, padding:"10px 14px", background:"rgba(255,255,255,0.022)", border:"1px solid rgba(255,255,255,0.055)", borderRadius:10, marginBottom:6, cursor:"default", transition:"background 0.15s" }}>
                <div style={{ width:3, height:32, background:"#7eb8e8", borderRadius:2, flexShrink:0 }}/>
                <div style={{ flex:1 }}>
                  <div style={{ fontSize:13, fontWeight:600 }}>{job.title}</div>
                  <div style={{ fontSize:10, color:"rgba(255,255,255,0.35)", fontFamily:"'DM Mono',monospace" }}>
                    {job.scheduled_date} {job.scheduled_time?.slice(0,5)} · {job.client_name}
                  </div>
                </div>
                <div style={{ fontSize:11, fontWeight:700, color:"#7eb8e8", background:"rgba(126,184,232,0.1)", padding:"3px 8px", borderRadius:5 }}>Scheduled</div>
              </div>
            ))}
          </div>
        )}

        {/* ── RECENT QUOTES ── */}
        {recentQuotes.length > 0 && (
          <div style={{ animation:"fadeUp 0.4s ease 0.14s both" }}>
            <div style={{ fontSize:10, color:"rgba(255,255,255,0.28)", textTransform:"uppercase", letterSpacing:"0.1em", marginBottom:10 }}>Recent quotes</div>
            {recentQuotes.map(q => {
              const sc = STATUS_COLORS[q.status] || STATUS_COLORS.draft;
              return (
                <div key={q.id} className="qrow" onClick={() => onLoadQuote(q)}
                  style={{ display:"flex", alignItems:"center", gap:10, padding:"10px 14px", background:"rgba(255,255,255,0.022)", border:"1px solid rgba(255,255,255,0.055)", borderRadius:10, marginBottom:6, cursor:"pointer", transition:"background 0.15s" }}>
                  <div style={{ flex:1, minWidth:0 }}>
                    <div style={{ fontSize:13, fontWeight:600, color:"#fff" }}>{q.client_name || "No client"}</div>
                    <div style={{ fontSize:10, color:"rgba(255,255,255,0.35)", fontFamily:"'DM Mono',monospace" }}>
                      {q.quote_number} · {new Date(q.created_at).toLocaleDateString()}
                    </div>
                  </div>
                  <div style={{ textAlign:"right", flexShrink:0 }}>
                    <div style={{ fontFamily:"'DM Mono',monospace", fontSize:13, fontWeight:600, color:"#e8c97a" }}>${(q.total || 0).toLocaleString()}</div>
                    <div style={{ fontSize:9, fontWeight:700, color:sc.text, background:sc.bg, padding:"2px 6px", borderRadius:4, marginTop:2 }}>{sc.label}</div>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* Empty state */}
        {!loading && quotes.length === 0 && (
          <div style={{ textAlign:"center", padding:"40px 20px", color:"rgba(255,255,255,0.2)" }}>
            <div style={{ fontSize:32, marginBottom:12 }}>📋</div>
            <div style={{ fontSize:14, marginBottom:6 }}>No quotes yet</div>
            <div style={{ fontSize:12, marginBottom:20 }}>Tap any quick quote card above or use the AI builder to create your first estimate</div>
            <button onClick={onShowAI} style={{ padding:"10px 22px", background:"rgba(232,201,122,0.1)", border:"1px solid rgba(232,201,122,0.3)", borderRadius:9, color:"#e8c97a", fontSize:13, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>
              ⚡ Build first quote with AI
            </button>
          </div>
        )}
      </div>
    </>
  );
}
