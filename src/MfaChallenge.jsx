/* eslint-disable react-hooks/exhaustive-deps */
// src/MfaChallenge.jsx — login-time second factor. Shown AFTER password sign-in
// for users with a verified TOTP factor, BEFORE the app (and Plaid Link) renders.
import { useState, useEffect } from "react";
import { listFactors, verifyCode } from "./lib/mfa";
import { signOut } from "./lib/supabase";

export default function MfaChallenge({ onVerified }) {
  const [factorId, setFactorId] = useState(null);
  const [code, setCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    listFactors().then(({ factors }) => {
      if (factors[0]) setFactorId(factors[0].id);
      else onVerified(); // no verified factor after all — let them through
    });
  }, []);

  const submit = async () => {
    if (!factorId || code.trim().length < 6) { setError("Enter the 6-digit code."); return; }
    setBusy(true); setError("");
    const { error } = await verifyCode(factorId, code);
    setBusy(false);
    if (error) { setError(error.message || "Incorrect code — try again."); setCode(""); return; }
    onVerified();
  };

  const wrap = { minHeight: "100vh", display: "flex", alignItems: "center", justifyContent: "center", padding: "24px 16px", background: "transparent" };
  const card = { width: "100%", maxWidth: 380, background: "rgba(17,17,21,0.92)", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 18, padding: "28px 24px", backdropFilter: "blur(12px)" };
  const input = { width: "100%", background: "rgba(255,255,255,0.05)", border: "1px solid rgba(255,255,255,0.12)", borderRadius: 9, padding: "12px 14px", fontSize: 20, letterSpacing: "0.3em", color: "#fff", fontFamily: "'DM Mono',monospace", textAlign: "center", outline: "none" };

  return (
    <div style={wrap}>
      <div style={card}>
        <div style={{ display: "flex", alignItems: "center", gap: 9, marginBottom: 6 }}>
          <img src="/logo192.png" alt="Wireway" style={{ height: 28, width: 28, borderRadius: 7, objectFit: "cover" }} />
          <span style={{ fontFamily: "'Syne',sans-serif", fontSize: 16, fontWeight: 800, color: "#fff" }}>Two-factor verification</span>
        </div>
        <p style={{ fontSize: 12.5, color: "rgba(255,255,255,0.5)", lineHeight: 1.6, margin: "0 0 18px" }}>
          Enter the 6-digit code from your authenticator app to finish signing in.
        </p>

        <input value={code} onChange={(e) => setCode(e.target.value.replace(/\D/g, "").slice(0, 6))}
          inputMode="numeric" autoComplete="one-time-code" placeholder="••••••" autoFocus
          onKeyDown={(e) => e.key === "Enter" && submit()} style={input} />

        {error && <div style={{ fontSize: 11.5, color: "#e87e7e", marginTop: 10 }}>{error}</div>}

        <button onClick={submit} disabled={busy}
          style={{ width: "100%", marginTop: 16, padding: "12px", borderRadius: 10, border: "1px solid rgba(240,168,24,0.4)", background: busy ? "rgba(240,168,24,0.08)" : "linear-gradient(135deg,rgba(240,168,24,0.25),rgba(240,168,24,0.1))", color: "#f0a818", fontSize: 14, fontWeight: 700, cursor: busy ? "default" : "pointer", fontFamily: "inherit" }}>
          {busy ? "Verifying…" : "Verify & continue"}
        </button>

        <button onClick={async () => { await signOut(); window.location.reload(); }}
          style={{ width: "100%", marginTop: 10, padding: "9px", borderRadius: 9, border: "none", background: "transparent", color: "rgba(255,255,255,0.4)", fontSize: 12, cursor: "pointer", fontFamily: "inherit" }}>
          Sign out
        </button>
      </div>
    </div>
  );
}
