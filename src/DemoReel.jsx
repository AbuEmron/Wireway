/* eslint-disable react-hooks/exhaustive-deps */
// src/DemoReel.jsx
// A self-playing animated walkthrough of Wireway — looks like a product video,
// built in code: instant load, razor-sharp on any phone, no buffering on job-site LTE.
// Four scenes: describe → AI estimate → proposal → paid by text. Tap to pause.

import { useState, useEffect } from "react";

const SCENES = [
  { id: 0, caption: "1 · Describe the job in plain English" },
  { id: 1, caption: "2 · AI builds your NEC-coded estimate" },
  { id: 2, caption: "3 · Send a professional proposal" },
  { id: 3, caption: "4 · Client pays from a text" },
];
const SCENE_MS = 4200;
const TYPED = "200A panel upgrade with surge protection, homeowner supplies the fixtures";

const AI_ITEMS = [
  { label: "Panel Upgrade — 200A",      nec: "NEC 408.36", price: "$2,850" },
  { label: "Whole-Home Surge (SPD)",    nec: "NEC 230.67", price: "$485" },
  { label: "Grounding Electrode Sys.",  nec: "NEC 250.50", price: "$390" },
  { label: "Fixture Install ×4",        nec: "labor only · client supplies", price: "$340" },
];

export default function DemoReel() {
  const [scene,   setScene]   = useState(0);
  const [playing, setPlaying] = useState(true);
  const [tick,    setTick]    = useState(0); // restarts inner animations per scene

  useEffect(() => {
    if (!playing) return;
    const t = setInterval(() => {
      setScene(s => (s + 1) % SCENES.length);
      setTick(k => k + 1);
    }, SCENE_MS);
    return () => clearInterval(t);
  }, [playing]);

  const [typed, setTyped] = useState("");
  useEffect(() => {
    if (scene !== 0) { setTyped(""); return; }
    let i = 0;
    const t = setInterval(() => {
      i += 2;
      setTyped(TYPED.slice(0, i));
      if (i >= TYPED.length) clearInterval(t);
    }, 38);
    return () => clearInterval(t);
  }, [scene, tick]);

  const dot = (active) => ({
    width: active ? 22 : 7, height: 7, borderRadius: 4, transition: "all 0.3s",
    background: active ? "var(--accent)" : "rgba(255,255,255,0.18)", cursor: "pointer",
  });

  return (
    <div onClick={() => setPlaying(p => !p)} style={{ cursor:"pointer", userSelect:"none" }}>
      <style>{`
        @keyframes drIn{from{opacity:0;transform:translateY(14px)}to{opacity:1;transform:translateY(0)}}
        @keyframes drPop{0%{opacity:0;transform:scale(0.92)}60%{transform:scale(1.03)}100%{opacity:1;transform:scale(1)}}
        @keyframes drPulse{0%,100%{box-shadow:0 0 0 0 rgba(var(--accent-rgb),0.4)}50%{box-shadow:0 0 0 9px rgba(var(--accent-rgb),0)}}
        @keyframes drBlink{50%{opacity:0}}
        .dr-item{animation:drIn 0.45s ease both}
        .dr-pop{animation:drPop 0.5s ease both}
      `}</style>

      {/* Phone frame */}
      <div style={{ maxWidth:340, margin:"0 auto", background:"#0a0d12", border:"1px solid rgba(255,255,255,0.12)", borderRadius:30, padding:"14px 12px", boxShadow:"0 24px 70px rgba(0,0,0,0.55), 0 0 0 1px rgba(var(--accent-rgb),0.08)" }}>
        <div style={{ width:74, height:5, borderRadius:3, background:"rgba(255,255,255,0.12)", margin:"0 auto 12px" }} />
        <div style={{ height:368, borderRadius:18, background:"var(--bg-scene)", border:"1px solid rgba(255,255,255,0.06)", overflow:"hidden", position:"relative", padding:"14px 13px", fontFamily:"'DM Sans',sans-serif" }}>

          {/* SCENE 1 — describe the job */}
          {scene === 0 && (
            <div key={tick}>
              <div style={{ fontSize:11, fontWeight:800, color:"var(--accent)", marginBottom:10 }}>⚡ AI Quote Builder</div>
              <div style={{ background:"rgba(255,255,255,0.04)", border:"1px solid rgba(255,255,255,0.1)", borderRadius:11, padding:"11px 12px", minHeight:86, fontSize:12, lineHeight:1.65, color:"rgba(255,255,255,0.85)" }}>
                {typed}<span style={{ animation:"drBlink 0.9s step-start infinite", color:"var(--accent)" }}>|</span>
              </div>
              <div style={{ display:"flex", gap:5, margin:"10px 0" }}>
                <span style={{ flex:1, textAlign:"center", padding:"7px", borderRadius:7, fontSize:9.5, fontWeight:700, border:"1px solid rgba(var(--accent-rgb),0.45)", background:"rgba(var(--accent-rgb),0.1)", color:"var(--accent)" }}>⚡ I supply materials</span>
                <span style={{ flex:1, textAlign:"center", padding:"7px", borderRadius:7, fontSize:9.5, fontWeight:700, border:"1px solid rgba(255,255,255,0.08)", color:"rgba(255,255,255,0.35)" }}>👤 Client supplies</span>
              </div>
              <div style={{ textAlign:"center", padding:"11px", borderRadius:10, fontSize:12, fontWeight:800, background:"linear-gradient(135deg,rgba(var(--accent-rgb),0.3),rgba(var(--accent-rgb),0.12))", border:"1px solid rgba(var(--accent-rgb),0.5)", color:"var(--accent)", animation:"drPulse 1.6s infinite" }}>
                Build My Estimate
              </div>
            </div>
          )}

          {/* SCENE 2 — AI estimate appears */}
          {scene === 1 && (
            <div key={tick}>
              <div style={{ fontSize:11, fontWeight:800, color:"var(--accent)", marginBottom:10 }}>Your estimate — built in seconds</div>
              {AI_ITEMS.map((it, i) => (
                <div key={i} className="dr-item" style={{ animationDelay:`${i * 0.35}s`, display:"flex", justifyContent:"space-between", alignItems:"center", padding:"9px 11px", marginBottom:6, background:"var(--card)", border:"1px solid var(--line)", borderRadius:9 }}>
                  <span style={{ minWidth:0 }}>
                    <span style={{ display:"block", fontSize:11, fontWeight:700, color:"#fff" }}>{it.label}</span>
                    <span style={{ fontSize:8.5, color:"var(--accent)", fontFamily:"'DM Mono',monospace" }}>{it.nec}</span>
                  </span>
                  <span style={{ fontFamily:"'DM Mono',monospace", fontSize:12, fontWeight:700, color:"var(--accent)" }}>{it.price}</span>
                </div>
              ))}
              <div className="dr-item" style={{ animationDelay:"1.6s", display:"flex", justifyContent:"space-between", padding:"10px 11px", borderTop:"1px solid var(--line-strong)", marginTop:4 }}>
                <span style={{ fontSize:12, fontWeight:800, color:"#fff" }}>Total</span>
                <span style={{ fontFamily:"'DM Mono',monospace", fontSize:15, fontWeight:800, color:"var(--accent)" }}>$4,065</span>
              </div>
            </div>
          )}

          {/* SCENE 3 — proposal */}
          {scene === 2 && (
            <div key={tick} className="dr-pop" style={{ background:"#f5f3ee", borderRadius:12, height:"100%", padding:"16px 15px", color:"#1a1a1c" }}>
              <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", borderBottom:"2px solid #1a1a1c", paddingBottom:9, marginBottom:11 }}>
                <span style={{ fontSize:11, fontWeight:800 }}>Your Company LLC</span>
                <span style={{ fontSize:10, fontWeight:800, letterSpacing:"0.1em", color:"#1273d2" }}>PROPOSAL</span>
              </div>
              {[78, 64, 84, 58].map((w, i) => (
                <div key={i} style={{ height:6, width:`${w}%`, background:"#d8d4cb", borderRadius:3, marginBottom:7 }} />
              ))}
              <div style={{ display:"flex", justifyContent:"space-between", marginTop:14, paddingTop:10, borderTop:"1.5px solid #1a1a1c" }}>
                <span style={{ fontSize:11, fontWeight:800 }}>Total Investment</span>
                <span style={{ fontSize:13, fontWeight:800, fontFamily:"'DM Mono',monospace" }}>$4,065</span>
              </div>
              <div style={{ marginTop:13, fontSize:9, color:"#8a8a8e" }}>Deposit due on acceptance (50%) — $2,033</div>
              <div className="dr-item" style={{ animationDelay:"1.4s", marginTop:14, textAlign:"center", padding:"9px", borderRadius:9, background:"#1a1a1c", color:"#fff", fontSize:11, fontWeight:800 }}>
                📄 Download PDF
              </div>
            </div>
          )}

          {/* SCENE 4 — paid by text */}
          {scene === 3 && (
            <div key={tick} style={{ paddingTop:18 }}>
              <div className="dr-item" style={{ maxWidth:"82%", background:"rgba(var(--accent-rgb),0.16)", border:"1px solid rgba(var(--accent-rgb),0.3)", borderRadius:"14px 14px 14px 4px", padding:"10px 12px", fontSize:11, lineHeight:1.6, color:"rgba(255,255,255,0.9)" }}>
                Hi Sarah, here's your secure payment link for the panel upgrade: <span style={{ color:"var(--accent)", textDecoration:"underline" }}>pay.wirewaypro.com/q1284</span>
              </div>
              <div className="dr-item" style={{ animationDelay:"1s", maxWidth:"60%", marginLeft:"auto", marginTop:10, background:"rgba(255,255,255,0.07)", borderRadius:"14px 14px 4px 14px", padding:"10px 12px", fontSize:11, color:"rgba(255,255,255,0.8)" }}>
                Just paid! Thank you 🙏
              </div>
              <div className="dr-pop" style={{ animationDelay:"2s", margin:"26px auto 0", maxWidth:210, textAlign:"center", padding:"14px", borderRadius:13, background:"rgba(100,220,130,0.1)", border:"1px solid rgba(100,220,130,0.4)" }}>
                <div style={{ fontSize:21, marginBottom:4 }}>✓</div>
                <div style={{ fontSize:12, fontWeight:800, color:"#7dcea0" }}>Deposit received</div>
                <div style={{ fontFamily:"'DM Mono',monospace", fontSize:16, fontWeight:800, color:"#7dcea0", marginTop:2 }}>$2,033</div>
              </div>
            </div>
          )}

          {/* pause veil */}
          {!playing && (
            <div style={{ position:"absolute", inset:0, background:"rgba(0,0,0,0.45)", display:"flex", alignItems:"center", justifyContent:"center", fontSize:34 }}>▶</div>
          )}
        </div>
      </div>

      {/* caption + scene dots */}
      <div style={{ textAlign:"center", marginTop:16 }}>
        <div style={{ fontSize:13.5, fontWeight:700, color:"rgba(255,255,255,0.85)", minHeight:20 }}>{SCENES[scene].caption}</div>
        <div style={{ display:"flex", gap:6, justifyContent:"center", marginTop:10 }}>
          {SCENES.map(s => (
            <span key={s.id} onClick={(e) => { e.stopPropagation(); setScene(s.id); setTick(k => k + 1); }} style={dot(scene === s.id)} />
          ))}
        </div>
        <div style={{ fontSize:9.5, color:"rgba(255,255,255,0.25)", marginTop:8 }}>tap to pause · tap a dot to jump</div>
      </div>
    </div>
  );
}
