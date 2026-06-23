// src/PlaidView.jsx — Phase 2 scaffold: Plaid card-linking UI
// This component is ready to wire up once you have Plaid API keys.
// See PLAID_SETUP.md for full instructions.

export default function PlaidView({ user, onClose }) {
  return (
    <div style={{ position: "fixed", inset: 0, zIndex: 150, background: "rgba(0,0,0,0.8)", backdropFilter: "blur(8px)", display: "flex", justifyContent: "center", alignItems: "center", padding: "24px 16px" }}>
      <div style={{ background: "#111115", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 18, width: "100%", maxWidth: 560, padding: "32px 28px" }}>

        <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 24 }}>
          <div>
            <div style={{ fontFamily: "'Syne',sans-serif", fontSize: 17, fontWeight: 800, color: "#fff" }}>Auto Card Import</div>
            <div style={{ fontSize: 11, color: "rgba(255,255,255,0.35)", marginTop: 2 }}>Phase 2 — Plaid bank connection</div>
          </div>
          <button onClick={onClose} style={{ background: "transparent", border: "none", color: "rgba(255,255,255,0.4)", fontSize: 22, cursor: "pointer" }}>✕</button>
        </div>

        {/* Status badge */}
        <div style={{ display: "inline-flex", alignItems: "center", gap: 6, padding: "4px 10px", background: "rgba(232,184,126,0.1)", border: "1px solid rgba(232,184,126,0.25)", borderRadius: 6, fontSize: 10, fontWeight: 700, color: "#e8b87e", marginBottom: 20 }}>
          <span style={{ width: 6, height: 6, borderRadius: "50%", background: "#e8b87e", display: "inline-block" }} />
          Needs your Plaid account to activate
        </div>

        <div style={{ fontSize: 13, color: "rgba(255,255,255,0.55)", lineHeight: 1.8, marginBottom: 24 }}>
          Connect your business credit or debit card and transactions are pulled automatically.
          New charges get categorized (materials, fuel, tools, etc.) and are ready to claim at year-end —
          no more hunting through bank statements.
        </div>

        {/* Feature list */}
        <div style={{ display: "flex", flexDirection: "column", gap: 10, marginBottom: 28 }}>
          {[
            { icon: "🔗", label: "Link any bank or card",     desc: "5,000+ institutions via Plaid Link" },
            { icon: "⚡", label: "Transactions auto-sync",    desc: "New charges appear within minutes" },
            { icon: "🏷",  label: "Smart categorization",     desc: "AI-suggested categories on each charge" },
            { icon: "📊", label: "Feeds your Tax Export",     desc: "Mileage + card spend → Schedule C PDF" },
          ].map((f) => (
            <div key={f.label} style={{ display: "flex", gap: 12, alignItems: "flex-start" }}>
              <span style={{ fontSize: 18, flexShrink: 0 }}>{f.icon}</span>
              <div>
                <div style={{ fontSize: 12, fontWeight: 700, color: "#fff" }}>{f.label}</div>
                <div style={{ fontSize: 11, color: "rgba(255,255,255,0.35)" }}>{f.desc}</div>
              </div>
            </div>
          ))}
        </div>

        {/* Setup steps */}
        <div style={{ background: "rgba(232,201,122,0.04)", border: "1px solid rgba(232,201,122,0.12)", borderRadius: 12, padding: "16px", marginBottom: 20 }}>
          <div style={{ fontSize: 10, color: "rgba(232,201,122,0.6)", textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 12 }}>To activate this feature</div>
          {[
            "Create a free Plaid developer account at plaid.com",
            "Get your client_id and sandbox/production secret",
            "Add PLAID_CLIENT_ID + PLAID_SECRET to Vercel env vars",
            "Deploy the api/plaid-link-token.js serverless function",
            "This screen will update to show the Plaid Link button",
          ].map((step, i) => (
            <div key={i} style={{ display: "flex", gap: 10, alignItems: "flex-start", marginBottom: 8 }}>
              <span style={{ fontFamily: "'DM Mono',monospace", fontSize: 10, color: "rgba(232,201,122,0.5)", fontWeight: 700, flexShrink: 0, marginTop: 1 }}>{i + 1}.</span>
              <span style={{ fontSize: 11, color: "rgba(255,255,255,0.5)", lineHeight: 1.5 }}>{step}</span>
            </div>
          ))}
          <div style={{ fontSize: 10, color: "rgba(255,255,255,0.25)", marginTop: 8 }}>See PLAID_SETUP.md in the repo root for detailed instructions.</div>
        </div>

        <button onClick={onClose}
          style={{ width: "100%", padding: "12px", background: "rgba(255,255,255,0.04)", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 10, color: "rgba(255,255,255,0.4)", fontSize: 12, fontWeight: 600, cursor: "pointer", fontFamily: "inherit" }}>
          Close
        </button>
      </div>
    </div>
  );
}
