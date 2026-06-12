import { WirewayMark } from "./Logo";
import DemoReel from "./DemoReel";
// src/LandingPage.jsx
// Public landing page shown at wirewaypro.com before the user signs in
// Converts visitors into signups — headline, features, proof, CTA

export default function LandingPage({ onSignIn, onSignUp }) {

  const FEATURES = [
    { icon:"⚡", title:"AI Quote Builder", desc:"Describe any job in plain English. AI maps it to 128+ NEC 2023 services, calculates labor and materials, builds your estimate in seconds." },
    { icon:"📐", title:"NEC 2023 Built-In", desc:"Every service comes with the correct code section. Wire size calculator, load calculator, and inspection checklists all reference 2023 NEC." },
    { icon:"💰", title:"Profit Center", desc:"See material cost, labor cost, markup, and margin on every job. Color-coded profit grade tells you if you're priced right before you send the quote." },
    { icon:"📄", title:"Client-Ready Quotes", desc:"Send branded quotes via text or email. Clients can view, sign, and pay from any phone — no app download required." },
    { icon:"💳", title:"Get Paid Online", desc:"Clients pay your quote directly with a card. Deposit or full amount. Money goes straight to your Stripe account." },
    { icon:"📅", title:"Job Calendar", desc:"Schedule accepted jobs on a calendar. Send On My Way texts, request Google reviews, and track job status all in one place." },
  ];

  const PROOF = [
    { num:"128+",    label:"Residential services" },
    { num:"NEC 2023", label:"Code references built-in" },
    { num:"30 days",  label:"Free trial" },
    { num:"$12/mo",   label:"After trial" },
  ];

  const QUICK_JOBS = [
    "Panel Upgrade","EV Charger","Generator Install","Hot Tub Circuit",
    "Service Call","Lighting Upgrade","GFCI Outlets","Circuit Addition",
  ];

  const style = {
    page: { minHeight:"100vh", background:"#0a0a0c", fontFamily:"'DM Sans',sans-serif", color:"#fff", overflowX:"hidden" },
    nav: { position:"fixed", top:0, left:0, right:0, zIndex:100, borderBottom:"1px solid rgba(255,255,255,0.06)", background:"rgba(10,10,12,0.92)", backdropFilter:"blur(20px)", WebkitBackdropFilter:"blur(20px)", padding:"0 24px" },
    navInner: { maxWidth:960, margin:"0 auto", height:56, display:"flex", alignItems:"center", justifyContent:"space-between" },
    section: { maxWidth:960, margin:"0 auto", padding:"0 24px" },
    h1: { fontFamily:"'Syne',sans-serif", fontSize:"clamp(32px,7vw,58px)", fontWeight:800, letterSpacing:"-0.04em", lineHeight:1.05, marginBottom:20 },
    h2: { fontFamily:"'Syne',sans-serif", fontSize:"clamp(22px,4vw,34px)", fontWeight:800, letterSpacing:"-0.03em", lineHeight:1.1, marginBottom:12 },
    btnPrimary: { padding:"14px 28px", background:"linear-gradient(135deg,rgba(232,201,122,0.22),rgba(232,201,122,0.1))", border:"1px solid rgba(232,201,122,0.4)", borderRadius:11, color:"#e8c97a", fontSize:15, fontWeight:700, cursor:"pointer", fontFamily:"inherit", transition:"all 0.2s", display:"inline-block" },
    btnSecondary: { padding:"12px 22px", background:"transparent", border:"1px solid rgba(255,255,255,0.15)", borderRadius:10, color:"rgba(255,255,255,0.6)", fontSize:14, fontWeight:600, cursor:"pointer", fontFamily:"inherit", transition:"all 0.2s", display:"inline-block" },
  };

  return (
    <>
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=DM+Mono:wght@400;500&family=Syne:wght@700;800&family=DM+Sans:wght@400;500;600&display=swap');
        *{box-sizing:border-box;margin:0;padding:0}
        body{background:#0a0a0c;overflow-x:hidden}
        @keyframes fadeUp{from{opacity:0;transform:translateY(20px)}to{opacity:1;transform:translateY(0)}}
        @keyframes float{0%,100%{transform:translateY(0)}50%{transform:translateY(-8px)}}
        .fade-up{animation:fadeUp 0.6s ease both}
        .hero-badge:hover,.btn-primary:hover{opacity:0.85}
        .btn-secondary:hover{background:rgba(255,255,255,0.07)!important;color:#fff!important}
        .feat-card:hover{background:rgba(255,255,255,0.04)!important;border-color:rgba(255,255,255,0.1)!important}
        .job-pill:hover{background:rgba(232,201,122,0.15)!important;border-color:rgba(232,201,122,0.4)!important;color:#e8c97a!important}
        .cta-btn:hover{background:linear-gradient(135deg,rgba(232,201,122,0.3),rgba(232,201,122,0.15))!important}
      `}</style>

      <div style={style.page}>

        {/* ── NAV ── */}
        <nav style={style.nav}>
          <div style={style.navInner}>
            <div style={{ display:"flex", alignItems:"center", gap:10 }}>
              <WirewayMark size={30} />
              <span style={{ fontFamily:"'Syne',sans-serif", fontSize:17, fontWeight:800, letterSpacing:"-0.02em" }}>
                <span style={{ color:"#e8c97a" }}>WIRE</span>WAY
              </span>
            </div>
            <div style={{ display:"flex", gap:10, alignItems:"center" }}>
              <button onClick={onSignIn} style={style.btnSecondary} className="btn-secondary">Sign in</button>
              <button onClick={onSignUp} style={style.btnPrimary} className="btn-primary">Start free trial</button>
            </div>
          </div>
        </nav>

        {/* ── HERO ── */}
        <section style={{ ...style.section, paddingTop:120, paddingBottom:80, textAlign:"center" }}>
          <div className="fade-up" style={{ animationDelay:"0.1s" }}>
            <div style={{ display:"inline-flex", alignItems:"center", gap:8, padding:"6px 14px", background:"rgba(232,201,122,0.08)", border:"1px solid rgba(232,201,122,0.2)", borderRadius:30, fontSize:12, color:"rgba(232,201,122,0.8)", fontWeight:600, marginBottom:28, letterSpacing:"0.04em" }}>
              ⚡ NEC 2023 · AI-Powered · Built by electricians
            </div>
          </div>

          <h1 style={{ ...style.h1 }} className="fade-up">
            <span style={{ background:"linear-gradient(135deg,#fff 40%,rgba(232,201,122,0.9) 100%)", WebkitBackgroundClip:"text", WebkitTextFillColor:"transparent" }}>
              The electrical estimating tool<br />your competitors don't have
            </span>
          </h1>

          <p className="fade-up" style={{ fontSize:"clamp(15px,2.5vw,18px)", color:"rgba(255,255,255,0.5)", lineHeight:1.7, maxWidth:580, margin:"0 auto 36px", animationDelay:"0.15s" }}>
            Describe any job, get a professional NEC 2023 estimate in seconds. Send it to the client. Get paid. All from your phone.
          </p>

          <div className="fade-up" style={{ display:"flex", gap:12, justifyContent:"center", flexWrap:"wrap", marginBottom:48, animationDelay:"0.2s" }}>
            <button onClick={onSignUp} className="cta-btn" style={{ padding:"16px 36px", background:"linear-gradient(135deg,rgba(232,201,122,0.22),rgba(232,201,122,0.08))", border:"1px solid rgba(232,201,122,0.4)", borderRadius:12, color:"#e8c97a", fontSize:16, fontWeight:700, cursor:"pointer", fontFamily:"inherit", transition:"all 0.2s" }}>
              Start 30-day free trial
            </button>
            <button onClick={onSignIn} className="btn-secondary" style={style.btnSecondary}>
              Sign in to your account →
            </button>
          </div>

          <p className="fade-up" style={{ fontSize:12, color:"rgba(255,255,255,0.2)", animationDelay:"0.25s" }}>
            No credit card required · 30-day trial · $12/mo after · Cancel anytime
          </p>
        </section>

        {/* ── WATCH IT WORK — animated demo ── */}
        <section style={{ ...style.section, paddingBottom:90 }}>
          <div style={{ textAlign:"center", marginBottom:30 }}>
            <div style={{ fontSize:11, fontWeight:800, letterSpacing:"0.14em", color:"var(--accent)", textTransform:"uppercase", marginBottom:10 }}>Watch it work</div>
            <h2 style={{ fontFamily:"'Syne',sans-serif", fontSize:"clamp(22px,4.5vw,32px)", fontWeight:800, letterSpacing:"-0.02em", lineHeight:1.15 }}>
              Job described to deposit paid —<br/>in under a minute
            </h2>
          </div>
          <DemoReel />
        </section>

        {/* ── PROOF NUMBERS ── */}
        <section style={{ borderTop:"1px solid rgba(255,255,255,0.06)", borderBottom:"1px solid rgba(255,255,255,0.06)", padding:"40px 24px", marginBottom:80 }}>
          <div style={{ maxWidth:960, margin:"0 auto", display:"grid", gridTemplateColumns:"repeat(auto-fit,minmax(160px,1fr))", gap:24 }}>
            {PROOF.map(p => (
              <div key={p.label} style={{ textAlign:"center" }}>
                <div style={{ fontFamily:"'DM Mono',monospace", fontSize:28, fontWeight:500, color:"#e8c97a", letterSpacing:"-0.02em" }}>{p.num}</div>
                <div style={{ fontSize:12, color:"rgba(255,255,255,0.4)", marginTop:4 }}>{p.label}</div>
              </div>
            ))}
          </div>
        </section>

        {/* ── FEATURES ── */}
        <section style={{ ...style.section, paddingBottom:80 }}>
          <div style={{ textAlign:"center", marginBottom:48 }}>
            <h2 style={style.h2}>Everything an electrician needs to run their business</h2>
            <p style={{ fontSize:15, color:"rgba(255,255,255,0.45)", maxWidth:520, margin:"0 auto" }}>
              Not a generic contractor app. Built specifically for electricians, with NEC 2023 knowledge baked into every feature.
            </p>
          </div>
          <div style={{ display:"grid", gridTemplateColumns:"repeat(auto-fit,minmax(280px,1fr))", gap:12 }}>
            {FEATURES.map(f => (
              <div key={f.title} className="feat-card" style={{ background:"rgba(255,255,255,0.022)", border:"1px solid rgba(255,255,255,0.065)", borderRadius:14, padding:"20px 22px", transition:"all 0.2s" }}>
                <div style={{ fontSize:24, marginBottom:10 }}>{f.icon}</div>
                <div style={{ fontFamily:"'Syne',sans-serif", fontSize:15, fontWeight:800, color:"#fff", marginBottom:8, letterSpacing:"-0.02em" }}>{f.title}</div>
                <div style={{ fontSize:13, color:"rgba(255,255,255,0.5)", lineHeight:1.65 }}>{f.desc}</div>
              </div>
            ))}
          </div>
        </section>

        {/* ── QUICK JOBS ── */}
        <section style={{ ...style.section, paddingBottom:80 }}>
          <div style={{ textAlign:"center", marginBottom:32 }}>
            <h2 style={style.h2}>One tap to start any common job</h2>
            <p style={{ fontSize:14, color:"rgba(255,255,255,0.4)", maxWidth:480, margin:"0 auto" }}>
              Pre-loaded estimates for the jobs you do every day. Tap a card, review the services, send the quote.
            </p>
          </div>
          <div style={{ display:"flex", flexWrap:"wrap", gap:8, justifyContent:"center" }}>
            {QUICK_JOBS.map(job => (
              <div key={job} className="job-pill" style={{ padding:"10px 18px", background:"rgba(255,255,255,0.04)", border:"1px solid rgba(255,255,255,0.08)", borderRadius:30, fontSize:13, fontWeight:600, color:"rgba(255,255,255,0.6)", cursor:"default", transition:"all 0.2s" }}>
                {job}
              </div>
            ))}
          </div>
        </section>

        {/* ── FINAL CTA ── */}
        <section style={{ ...style.section, paddingBottom:100, textAlign:"center" }}>
          <div style={{ background:"linear-gradient(135deg,rgba(232,201,122,0.07),rgba(255,255,255,0.01))", border:"1px solid rgba(232,201,122,0.15)", borderRadius:20, padding:"52px 32px" }}>
            <h2 style={{ ...style.h2, marginBottom:12 }}>
              Ready to win more jobs?
            </h2>
            <p style={{ fontSize:15, color:"rgba(255,255,255,0.45)", marginBottom:28 }}>
              Start your 30-day free trial. No credit card required.
            </p>
            <button onClick={onSignUp} className="cta-btn" style={{ padding:"16px 40px", background:"linear-gradient(135deg,rgba(232,201,122,0.22),rgba(232,201,122,0.08))", border:"1px solid rgba(232,201,122,0.4)", borderRadius:12, color:"#e8c97a", fontSize:16, fontWeight:700, cursor:"pointer", fontFamily:"inherit", transition:"all 0.2s" }}>
              Start Free Trial — 30 Days
            </button>
            <div style={{ fontSize:12, color:"rgba(255,255,255,0.2)", marginTop:12 }}>Then $12/mo · Cancel anytime</div>
          </div>
        </section>

        {/* ── FOOTER ── */}
        <footer style={{ borderTop:"1px solid rgba(255,255,255,0.05)", padding:"24px", textAlign:"center" }}>
          <div style={{ display:"flex", alignItems:"center", gap:8, justifyContent:"center", marginBottom:8 }}>
            <WirewayMark size={20} glow={false} />
            <span style={{ fontFamily:"'Syne',sans-serif", fontSize:13, fontWeight:800 }}><span style={{ color:"#e8c97a" }}>WIRE</span>WAY</span>
          </div>
          <div style={{ fontSize:11, color:"rgba(255,255,255,0.2)", letterSpacing:"0.04em" }}>
            NEC 2023 Professional Electrical Estimating · wirewaypro.com<br/>
            <a href="/privacy.html" style={{ color:"rgba(255,255,255,0.3)", textDecoration:"none", fontSize:10 }}>Privacy</a>
            <span style={{ color:"rgba(255,255,255,0.15)", margin:"0 8px", fontSize:10 }}>·</span>
            <a href="/terms.html" style={{ color:"rgba(255,255,255,0.3)", textDecoration:"none", fontSize:10 }}>Terms</a>
            <span style={{ color:"rgba(255,255,255,0.15)", margin:"0 8px", fontSize:10 }}>·</span>
            <a href="mailto:support@wirewaypro.com" style={{ color:"rgba(255,255,255,0.3)", textDecoration:"none", fontSize:10 }}>support@wirewaypro.com</a>
          </div>
        </footer>
      </div>
    </>
  );
}
