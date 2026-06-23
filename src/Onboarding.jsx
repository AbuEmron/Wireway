// src/Onboarding.jsx
// First-run experience: an emotional, benefit-led welcome that walks a brand-new
// electrician straight into their first AI-built estimate (the "aha" moment),
// plus a setup checklist that tracks real progress and routes each step to the
// right screen. Dismissals persist per device.
//
// Exports are unchanged (getOnboardState, setOnboardState, WelcomeHero,
// SetupChecklist) and component props are identical, so the rest of the app
// wires in exactly as before.

const ONBOARD_KEY = "wireway_onboard_v1";

export function getOnboardState() {
  try { return JSON.parse(window.localStorage.getItem(ONBOARD_KEY)) || {}; } catch { return {}; }
}
export function setOnboardState(patch) {
  try {
    const cur = getOnboardState();
    window.localStorage.setItem(ONBOARD_KEY, JSON.stringify({ ...cur, ...patch }));
  } catch { /* storage blocked */ }
}

export function WelcomeHero({ onStartAI, onDismiss }) {
  const steps = [
    { n: "1", t: "Describe the job", d: "Plain English, like you'd tell a buddy" },
    { n: "2", t: "Review the estimate", d: "NEC-coded, priced, every line yours to edit" },
    { n: "3", t: "Send & get paid", d: "Pro proposal, signed and paid by text" },
  ];
  return (
    <div style={{ background:"linear-gradient(160deg, rgba(var(--accent-rgb),0.14), rgba(var(--accent-rgb),0.03) 60%)", border:"1px solid rgba(var(--accent-rgb),0.35)", borderRadius:16, padding:"24px 20px", marginBottom:16, position:"relative" }}>
      <button onClick={onDismiss} aria-label="Dismiss welcome" style={{ position:"absolute", top:10, right:12, background:"transparent", border:"none", color:"rgba(255,255,255,0.3)", fontSize:14, cursor:"pointer", fontFamily:"inherit", padding:4 }}>✕</button>

      <div style={{ display:"inline-flex", alignItems:"center", gap:7, padding:"4px 11px", background:"rgba(var(--accent-rgb),0.12)", border:"1px solid rgba(var(--accent-rgb),0.25)", borderRadius:30, fontSize:10.5, fontWeight:700, letterSpacing:"0.04em", color:"var(--accent)", marginBottom:14 }}>
        ⚡ Your free trial is live
      </div>

      <div style={{ fontFamily:"'Syne',sans-serif", fontSize:23, fontWeight:800, letterSpacing:"-0.02em", lineHeight:1.15, marginBottom:8 }}>
        Get your evenings back.
      </div>
      <div style={{ fontSize:13, color:"rgba(255,255,255,0.6)", lineHeight:1.6, marginBottom:18, maxWidth:440 }}>
        The job usually goes to whoever quotes first. Let's price your next one right now — in under a minute, NEC-correct, ready to send before you've packed up the truck.
      </div>

      <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr 1fr", gap:8, marginBottom:18 }}>
        {steps.map(s => (
          <div key={s.n} style={{ background:"var(--card)", border:"1px solid var(--line)", borderRadius:11, padding:"11px 10px" }}>
            <div style={{ fontFamily:"'DM Mono',monospace", fontSize:11, color:"var(--accent)", fontWeight:700, marginBottom:5 }}>{s.n}</div>
            <div style={{ fontSize:11.5, fontWeight:700, marginBottom:3, lineHeight:1.3 }}>{s.t}</div>
            <div style={{ fontSize:9.5, color:"rgba(255,255,255,0.4)", lineHeight:1.45 }}>{s.d}</div>
          </div>
        ))}
      </div>

      <button onClick={onStartAI} style={{ width:"100%", padding:"15px", borderRadius:11, background:"linear-gradient(135deg, rgba(var(--accent-rgb),0.32), rgba(var(--accent-rgb),0.12))", border:"1px solid rgba(var(--accent-rgb),0.55)", color:"var(--accent)", fontSize:14.5, fontWeight:800, cursor:"pointer", fontFamily:"inherit" }}>
        ⚡ Build my first estimate free
      </button>
      <div style={{ fontSize:10, color:"rgba(255,255,255,0.32)", textAlign:"center", marginTop:9 }}>
        Starts with a real example job — edit it or type your own. No card needed.
      </div>
    </div>
  );
}

export function SetupChecklist({ items, onDismiss }) {
  const done = items.filter(i => i.done).length;
  const allDone = done === items.length;
  return (
    <div style={{ background:"var(--card)", border:"1px solid var(--line)", borderRadius:14, padding:"15px 16px", marginBottom:16, position:"relative" }}>
      <button onClick={onDismiss} aria-label="Dismiss checklist" style={{ position:"absolute", top:8, right:10, background:"transparent", border:"none", color:"rgba(255,255,255,0.25)", fontSize:13, cursor:"pointer", fontFamily:"inherit", padding:4 }}>✕</button>

      <div style={{ display:"flex", alignItems:"center", gap:10, marginBottom:5 }}>
        <span style={{ fontSize:12, fontWeight:800, fontFamily:"'Syne',sans-serif" }}>
          {allDone ? "You're set up ⚡" : "Set up to get paid"}
        </span>
        <span style={{ fontFamily:"'DM Mono',monospace", fontSize:10, color:"var(--accent)" }}>{done}/{items.length}</span>
      </div>
      <div style={{ fontSize:10.5, color:"rgba(255,255,255,0.4)", marginBottom:11, lineHeight:1.5 }}>
        {allDone
          ? "Everything's ready — every quote you send can now be signed and paid on the spot."
          : "Three quick steps and clients can pay your quotes by card from their phone."}
      </div>

      <div style={{ height:4, borderRadius:2, background:"rgba(255,255,255,0.07)", marginBottom:12, overflow:"hidden" }}>
        <div style={{ height:"100%", width:`${(done / items.length) * 100}%`, background:"var(--accent)", borderRadius:2, transition:"width 0.4s" }} />
      </div>

      {items.map((it, i) => (
        <div key={i} onClick={it.done ? undefined : it.onClick}
          style={{ display:"flex", alignItems:"center", gap:11, padding:"9px 4px", cursor: it.done ? "default" : "pointer", opacity: it.done ? 0.55 : 1 }}>
          <span style={{ width:20, height:20, borderRadius:"50%", flexShrink:0, display:"flex", alignItems:"center", justifyContent:"center", fontSize:11,
            border: it.done ? "1px solid rgba(100,220,130,0.5)" : "1px solid var(--line-strong)",
            background: it.done ? "rgba(100,220,130,0.12)" : "transparent",
            color: it.done ? "#7dcea0" : "rgba(255,255,255,0.3)" }}>
            {it.done ? "✓" : ""}
          </span>
          <span style={{ flex:1, minWidth:0 }}>
            <span style={{ display:"block", fontSize:12.5, fontWeight:600, textDecoration: it.done ? "line-through" : "none" }}>{it.label}</span>
            {!it.done && <span style={{ display:"block", fontSize:10, color:"rgba(255,255,255,0.35)" }}>{it.hint}</span>}
          </span>
          {!it.done && <span style={{ color:"var(--accent)", fontSize:13 }}>›</span>}
        </div>
      ))}
    </div>
  );
}
