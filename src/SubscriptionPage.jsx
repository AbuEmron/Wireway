// src/SubscriptionPage.jsx
// Pricing + upgrade page — shown when user is on free plan or trial
// Matches Wireway's dark gold UI exactly

import { useState } from "react";
import { isElite } from "./lib/supabase";

const PLANS = [
  {
    id: "free",
    name: "Free",
    price: 0,
    period: "",
    description: "Get started with the basics",
    color: "rgba(255,255,255,0.4)",
    features: [
      { text: "3 saved quotes",                  included: true },
      { text: "All 128+ services catalog",        included: true },
      { text: "NEC 2023 reference",               included: true },
      { text: "Wire size calculator",             included: true },
      { text: "Copy & email quotes",              included: true },
      { text: "Company branding + logo",          included: false },
      { text: "Unlimited saved quotes",           included: false },
      { text: "Client database",                  included: false },
      { text: "Client signature / acceptance",    included: false },
      { text: "Stripe payment collection",        included: false },
      { text: "Invoice mode",                     included: false },
      { text: "Load calculator (NEC 220.82)",     included: false },
      { text: "Inspection checklists",            included: false },
      { text: "Profit analysis",                  included: false },
      { text: "Material pull list",               included: false },
      { text: "PDF export",                       included: false },
    ],
    cta: "Current Plan",
    ctaDisabled: true,
  },
  {
    id: "pro",
    name: "Pro",
    price: 12,
    period: "/mo",
    annualPrice: 99,
    description: "Everything you need to win jobs",
    color: "var(--accent)",
    badge: "Most Popular",
    features: [
      { text: "Unlimited saved quotes",           included: true },
      { text: "All 128+ services catalog",        included: true },
      { text: "NEC 2023 reference",               included: true },
      { text: "Company branding + logo",          included: true },
      { text: "Client database",                  included: true },
      { text: "Client signature / acceptance",    included: true },
      { text: "Stripe payment collection",        included: true },
      { text: "Invoice mode",                     included: true },
      { text: "Wire size calculator",             included: true },
      { text: "Load calculator (NEC 220.82)",     included: true },
      { text: "Inspection checklists",            included: true },
      { text: "Profit analysis per job",          included: true },
      { text: "Material pull list",               included: true },
      { text: "PDF export",                       included: true },
      { text: "Email + SMS quote sending",        included: true },
      { text: "Priority support",                 included: true },
    ],
    cta: "Start 30-Day Free Trial",
    ctaDisabled: false,
  },
  {
    id: "teams",
    name: "Teams",
    price: 29,
    period: "/mo",
    annualPrice: 249,
    description: "For small crews and growing businesses",
    color: "#818cf8",
    features: [
      { text: "Everything in Pro",                included: true },
      { text: "Up to 5 users",                    included: true },
      { text: "Shared client database",           included: true },
      { text: "Shared quote history",             included: true },
      { text: "Team admin controls",              included: true },
      { text: "Usage analytics",                  included: true },
      { text: "Dedicated support",                included: true },
      { text: "Early access to new features",     included: true },
    ],
    cta: "Start 30-Day Free Trial",
    ctaDisabled: false,
  },
];

const ELITE_PLAN = {
  id: "elite",
  name: "Elite",
  price: 99,
  period: "/mo",
  description: "Industrial estimating — the whole bid desk",
  color: "#f0a818",
  features: [
    { text: "Everything in Pro",                       included: true },
    { text: "AI industrial takeoff (plain English)",   included: true },
    { text: "Panel schedule photo reading",            included: true },
    { text: "56-item industrial catalog + assemblies", included: true },
    { text: "11 job-condition labor multipliers",      included: true },
    { text: "Composite crew rates",                    included: true },
    { text: "Bid recap: burden, OH&P, bond",           included: true },
    { text: "Change orders — included, no upsell",     included: true },
    { text: "Excel/CSV bid export",                    included: true },
    { text: "Industrial NEC 2023 reference",           included: true },
  ],
  cta: "Upgrade to Elite",
};

const FAQS = [
  {
    q: "Do I need a credit card to start the free trial?",
    a: "No. Your 30-day Pro trial starts the moment you sign up. We only ask for a card when the trial ends.",
  },
  {
    q: "Can I cancel anytime?",
    a: "Yes. Cancel from your account settings anytime. No cancellation fees, no questions asked. You keep access until the end of your billing period.",
  },
  {
    q: "How does client payment collection work?",
    a: "You connect your own Stripe account in Company Settings. When a client pays your quote, the money goes directly to you. Wireway never touches your payment funds.",
  },
  {
    q: "What happens to my quotes if I downgrade?",
    a: "All your data is preserved. You can view your existing quotes but won't be able to create new ones beyond the free tier limit until you upgrade again.",
  },
  {
    q: "Is there a discount for annual billing?",
    a: "Yes — Pro is $99/year (save $45) and Teams is $249/year (save $99). Switch to annual in your billing settings anytime.",
  },
  {
    q: "I'm a solo electrician. Is Pro worth it?",
    a: "If you send 2+ quotes a month, yes. One won job from a professional quote pays for a year of Wireway. The payment collection alone saves hours of invoice chasing.",
  },
];

export default function SubscriptionPage({ user, profile, onClose, onUpgrade }) {
  const [annual,      setAnnual]      = useState(false);
  const [loading,     setLoading]     = useState("");
  const [openFaq,     setOpenFaq]     = useState(null);

  const currentPlan = profile?.plan || "free";
  const trialEnds   = profile?.trial_ends_at
    ? new Date(profile.trial_ends_at).toLocaleDateString()
    : null;
  const isTrialing  = profile?.subscription_status === "trialing";

  const handleUpgrade = async (planId) => {
    if (loading) return;
    setLoading(planId);
    try {
      const res = await fetch("/api/create-subscription", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          userId: user.id,
          email:  user.email,
          plan:   planId,
          annual,
        }),
      });
      const data = await res.json();
      if (data.url) {
        window.open(data.url, "_blank");
      } else {
        alert(data.error || "Something went wrong. Please try again.");
      }
    } catch (err) {
      alert("Network error. Please check your connection.");
    } finally {
      setLoading("");
    }
  };

  const handleManageBilling = async () => {
    setLoading("portal");
    try {
      const res = await fetch("/api/billing-portal", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ userId: user.id }),
      });
      const data = await res.json();
      if (data.url) window.open(data.url, "_blank");
    } catch {}
    finally { setLoading(""); }
  };

  return (
    <>
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=DM+Mono:wght@400;500&family=Syne:wght@700;800&family=DM+Sans:wght@400;500;600&display=swap');
        @keyframes fadeUp{from{opacity:0;transform:translateY(12px)}to{opacity:1;transform:translateY(0)}}
      `}</style>

      <div style={{ minHeight:"100vh", background:"var(--bg-scene)", fontFamily:"'DM Sans',sans-serif", color:"#fff", paddingBottom:80 }}>

        {/* Header */}
        <div style={{ borderBottom:"1px solid var(--line)", background:"rgba(10,10,12,0.9)", backdropFilter:"blur(20px)", WebkitBackdropFilter:"blur(20px)", position:"sticky", top:0, zIndex:100, padding:"0 20px" }}>
          <div style={{ maxWidth:900, margin:"0 auto", height:54, display:"flex", alignItems:"center", justifyContent:"space-between" }}>
            <div style={{ display:"flex", alignItems:"center", gap:8 }}>
              <img src="/logo192.png" alt="Wireway" style={{ height:28, width:28, borderRadius:6, objectFit:"contain" }} />
              <span style={{ fontFamily:"'Syne',sans-serif", fontSize:16, fontWeight:800, letterSpacing:"-0.03em" }}><span style={{ color:"var(--accent)" }}>WIRE</span>WAY</span>
            </div>
            <div style={{ display:"flex", gap:8, alignItems:"center" }}>
              {currentPlan !== "free" && (
                <button onClick={handleManageBilling} style={{ padding:"6px 12px", borderRadius:7, border:"1px solid var(--line-strong)", background:"var(--card)", color:"rgba(255,255,255,0.5)", fontSize:11, fontWeight:600, cursor:"pointer", fontFamily:"inherit" }}>
                  {loading === "portal" ? "Opening..." : "Manage Billing"}
                </button>
              )}
              {onClose && (
                <button onClick={onClose} style={{ padding:"6px 12px", borderRadius:7, border:"1px solid var(--line-strong)", background:"var(--card)", color:"rgba(255,255,255,0.5)", fontSize:11, fontWeight:600, cursor:"pointer", fontFamily:"inherit" }}>
                  ← Back to App
                </button>
              )}
            </div>
          </div>
        </div>

        <div style={{ maxWidth:900, margin:"0 auto", padding:"0 20px" }}>

          {/* Trial banner */}
          {isTrialing && trialEnds && (
            <div style={{ margin:"20px 0 0", padding:"12px 16px", background:"rgba(var(--accent-rgb),0.07)", border:"1px solid rgba(var(--accent-rgb),0.2)", borderRadius:10, fontSize:12, color:"rgba(var(--accent-rgb),0.8)", textAlign:"center" }}>
              ⏳ Your free trial ends on <strong>{trialEnds}</strong>. Upgrade now to keep access to all Pro features.
            </div>
          )}

          {/* Hero */}
          <div style={{ textAlign:"center", padding:"48px 0 36px", animation:"fadeUp 0.4s ease both" }}>
            <h1 style={{ fontFamily:"'Syne',sans-serif", fontSize:"clamp(26px,6vw,42px)", fontWeight:800, letterSpacing:"-0.04em", lineHeight:1.1, background:"linear-gradient(135deg,#ffffff 40%,rgba(var(--accent-rgb),0.85) 100%)", WebkitBackgroundClip:"text", WebkitTextFillColor:"transparent", marginBottom:14 }}>
              The only estimating tool<br />built for electricians
            </h1>
            <p style={{ fontSize:15, color:"rgba(255,255,255,0.45)", lineHeight:1.7, maxWidth:500, margin:"0 auto 28px" }}>
              NEC 2023 built-in. 128+ residential services. Wire size calculator. Load calculator. Inspection checklists. Client payments. All in one tool.
            </p>

            {/* Annual toggle */}
            <div style={{ display:"inline-flex", alignItems:"center", gap:10, background:"var(--card)", border:"1px solid var(--line-strong)", borderRadius:30, padding:"6px 14px" }}>
              <span style={{ fontSize:12, color: !annual ? "#fff" : "rgba(255,255,255,0.4)", fontWeight:600 }}>Monthly</span>
              <button onClick={() => setAnnual(v => !v)} style={{ width:40, height:22, borderRadius:11, border:"none", background: annual ? "var(--accent)" : "rgba(255,255,255,0.15)", cursor:"pointer", position:"relative", transition:"background 0.2s", flexShrink:0 }}>
                <div style={{ width:16, height:16, borderRadius:"50%", background:"#fff", position:"absolute", top:3, left: annual ? 21 : 3, transition:"left 0.2s" }}/>
              </button>
              <span style={{ fontSize:12, color: annual ? "var(--accent)" : "rgba(255,255,255,0.4)", fontWeight:600 }}>
                Annual <span style={{ fontSize:10, background:"rgba(var(--accent-rgb),0.15)", border:"1px solid rgba(var(--accent-rgb),0.3)", color:"var(--accent)", padding:"1px 5px", borderRadius:4, marginLeft:4 }}>Save 30%</span>
              </span>
            </div>
          </div>

          {/* Pricing cards */}
          <div style={{ display:"grid", gridTemplateColumns:"repeat(auto-fit,minmax(260px,1fr))", gap:16, marginBottom:60, animation:"fadeUp 0.4s ease 0.05s both" }}>
            {[...PLANS, ...(isElite(profile) ? [ELITE_PLAN] : [])].map(plan => {
              const isCurrent  = currentPlan === plan.id;
              const isPopular  = plan.badge;
              const price      = annual && plan.annualPrice
                ? Math.round(plan.annualPrice / 12)
                : plan.price;

              return (
                <div key={plan.id} style={{
                  background: isPopular
                    ? `linear-gradient(135deg,rgba(var(--accent-rgb),0.08) 0%,rgba(255,255,255,0.02) 100%)`
                    : "rgba(255,255,255,0.025)",
                  border: isPopular
                    ? "1px solid rgba(var(--accent-rgb),0.3)"
                    : "1px solid var(--line)",
                  borderRadius:16,
                  padding:"24px",
                  position:"relative",
                  display:"flex",
                  flexDirection:"column",
                }}>
                  {/* Badge */}
                  {isPopular && (
                    <div style={{ position:"absolute", top:-12, left:"50%", transform:"translateX(-50%)", background:"linear-gradient(135deg,var(--accent),#c9a84c)", borderRadius:20, padding:"3px 12px", fontSize:10, fontWeight:800, color:"var(--bg0)", letterSpacing:"0.06em", whiteSpace:"nowrap" }}>
                      {plan.badge}
                    </div>
                  )}

                  {/* Plan name + price */}
                  <div style={{ marginBottom:20 }}>
                    <div style={{ fontFamily:"'Syne',sans-serif", fontSize:18, fontWeight:800, color: plan.color, marginBottom:4 }}>{plan.name}</div>
                    <div style={{ fontSize:11, color:"rgba(255,255,255,0.35)", marginBottom:14 }}>{plan.description}</div>
                    <div style={{ display:"flex", alignItems:"baseline", gap:4 }}>
                      <span style={{ fontFamily:"'DM Mono',monospace", fontSize:36, fontWeight:500, color:"#fff", letterSpacing:"-0.03em" }}>
                        ${price}
                      </span>
                      {plan.period && (
                        <span style={{ fontSize:13, color:"rgba(255,255,255,0.35)" }}>/mo</span>
                      )}
                    </div>
                    {annual && plan.annualPrice && (
                      <div style={{ fontSize:10, color:"rgba(255,255,255,0.3)", marginTop:3 }}>
                        billed ${plan.annualPrice}/yr · save ${(plan.price * 12) - plan.annualPrice}/yr
                      </div>
                    )}
                  </div>

                  {/* CTA */}
                  <button
                    onClick={() => !plan.ctaDisabled && !isCurrent && handleUpgrade(plan.id)}
                    disabled={plan.ctaDisabled || isCurrent || loading === plan.id}
                    style={{
                      width:"100%", padding:"12px", borderRadius:10,
                      border: isCurrent ? "1px solid var(--line-strong)" : `1px solid ${plan.color}50`,
                      background: isCurrent ? "rgba(255,255,255,0.04)" : isPopular ? "linear-gradient(135deg,rgba(var(--accent-rgb),0.2),rgba(var(--accent-rgb),0.08))" : `${plan.color}12`,
                      color: isCurrent ? "rgba(255,255,255,0.3)" : plan.color,
                      fontSize:13, fontWeight:700,
                      cursor: (plan.ctaDisabled || isCurrent) ? "default" : "pointer",
                      fontFamily:"inherit", transition:"all 0.2s",
                      marginBottom:20,
                    }}
                  >
                    {loading === plan.id ? "Opening Stripe..." : isCurrent ? "Current Plan" : plan.cta}
                  </button>

                  {/* Features */}
                  <div style={{ flex:1 }}>
                    {plan.features.map((f, i) => (
                      <div key={i} style={{ display:"flex", alignItems:"center", gap:8, padding:"5px 0", borderBottom:"1px solid var(--line)" }}>
                        <span style={{ fontSize:12, color: f.included ? "#7dcea0" : "rgba(255,255,255,0.2)", flexShrink:0 }}>
                          {f.included ? "✓" : "✕"}
                        </span>
                        <span style={{ fontSize:12, color: f.included ? "rgba(255,255,255,0.7)" : "rgba(255,255,255,0.25)" }}>
                          {f.text}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              );
            })}
          </div>

          {/* Social proof */}
          <div style={{ textAlign:"center", marginBottom:60, animation:"fadeUp 0.4s ease 0.1s both" }}>
            <div style={{ fontSize:11, color:"rgba(255,255,255,0.25)", textTransform:"uppercase", letterSpacing:"0.1em", marginBottom:20 }}>Built for working electricians</div>
            <div style={{ display:"flex", justifyContent:"center", gap:32, flexWrap:"wrap" }}>
              {[
                { num:"128+", label:"Residential services" },
                { num:"NEC 2023", label:"Code references" },
                { num:"30 days", label:"Free trial" },
                { num:"$0", label:"Setup fee" },
              ].map(s => (
                <div key={s.label} style={{ textAlign:"center" }}>
                  <div style={{ fontFamily:"'DM Mono',monospace", fontSize:22, fontWeight:700, color:"var(--accent)" }}>{s.num}</div>
                  <div style={{ fontSize:11, color:"rgba(255,255,255,0.35)", marginTop:2 }}>{s.label}</div>
                </div>
              ))}
            </div>
          </div>

          {/* FAQ */}
          <div style={{ maxWidth:600, margin:"0 auto 60px", animation:"fadeUp 0.4s ease 0.15s both" }}>
            <div style={{ fontFamily:"'Syne',sans-serif", fontSize:20, fontWeight:800, color:"#fff", textAlign:"center", marginBottom:24, letterSpacing:"-0.02em" }}>
              Frequently asked questions
            </div>
            {FAQS.map((faq, i) => (
              <div key={i} style={{ borderBottom:"1px solid var(--line)", overflow:"hidden" }}>
                <button onClick={() => setOpenFaq(openFaq === i ? null : i)} style={{ width:"100%", display:"flex", justifyContent:"space-between", alignItems:"center", padding:"16px 0", background:"transparent", border:"none", cursor:"pointer", textAlign:"left", gap:12 }}>
                  <span style={{ fontSize:13, fontWeight:600, color:"rgba(255,255,255,0.8)" }}>{faq.q}</span>
                  <span style={{ color:"rgba(255,255,255,0.4)", fontSize:18, transform: openFaq===i ? "rotate(45deg)" : "rotate(0)", transition:"transform 0.2s", flexShrink:0 }}>+</span>
                </button>
                {openFaq === i && (
                  <div style={{ fontSize:13, color:"rgba(255,255,255,0.5)", lineHeight:1.7, paddingBottom:16 }}>{faq.a}</div>
                )}
              </div>
            ))}
          </div>

          {/* Bottom CTA */}
          <div style={{ textAlign:"center", padding:"40px 20px", background:"rgba(var(--accent-rgb),0.04)", border:"1px solid rgba(var(--accent-rgb),0.12)", borderRadius:16, marginBottom:40 }}>
            <div style={{ fontFamily:"'Syne',sans-serif", fontSize:22, fontWeight:800, color:"#fff", marginBottom:10, letterSpacing:"-0.02em" }}>
              Ready to win more jobs?
            </div>
            <div style={{ fontSize:13, color:"rgba(255,255,255,0.4)", marginBottom:20 }}>
              Start your 30-day free trial. No credit card required.
            </div>
            <button onClick={() => handleUpgrade("pro")} style={{ padding:"14px 32px", background:"linear-gradient(135deg,rgba(var(--accent-rgb),0.22),rgba(var(--accent-rgb),0.1))", border:"1px solid rgba(var(--accent-rgb),0.4)", borderRadius:11, color:"var(--accent)", fontSize:14, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>
              {loading === "pro" ? "Opening Stripe..." : "Start Free Trial — Pro $12/mo"}
            </button>
          </div>

          <div style={{ textAlign:"center", fontSize:10, color:"rgba(255,255,255,0.15)", letterSpacing:"0.06em" }}>
            WIREWAY · NEC 2023 RESIDENTIAL ESTIMATING · PROFESSIONAL GRADE
          </div>
        </div>
      </div>
    </>
  );
}
