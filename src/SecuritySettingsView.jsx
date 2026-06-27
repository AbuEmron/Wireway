/* eslint-disable react-hooks/exhaustive-deps */
// src/SecuritySettingsView.jsx — Settings → Security: manage two-factor (TOTP).
// (Danger Zone / account deletion is added alongside this in a later commit.)
import { useState, useEffect, useCallback } from "react";
import { listFactors, enrollTotp, verifyCode, unenroll } from "./lib/mfa";

const GREEN = "#7dcea0", RED = "#e87e7e", GOLD = "#f0a818";

export default function SecuritySettingsView({ user, onClose }) {
  const [factors, setFactors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [msg, setMsg] = useState("");
  const [enroll, setEnroll] = useState(null); // { factorId, qr, secret }
  const [code, setCode] = useState("");
  const [busy, setBusy] = useState(false);

  const flash = (m) => { setMsg(m); setTimeout(() => setMsg(""), 3000); };

  const refresh = useCallback(async () => {
    setLoading(true);
    const { factors } = await listFactors();
    setFactors(factors);
    setLoading(false);
  }, []);
  useEffect(() => { refresh(); }, [refresh]);

  const startEnroll = async () => {
    setBusy(true); setMsg("");
    const { data, error } = await enrollTotp(`Authenticator ${new Date().toISOString().slice(0, 10)}`);
    setBusy(false);
    if (error) { flash(error.message || "Could not start enrollment."); return; }
    setEnroll({ factorId: data.id, qr: data.totp.qr_code, secret: data.totp.secret });
    setCode("");
  };

  const confirmEnroll = async () => {
    if (code.trim().length < 6) { flash("Enter the 6-digit code from your app."); return; }
    setBusy(true);
    const { error } = await verifyCode(enroll.factorId, code);
    setBusy(false);
    if (error) { flash(error.message || "Incorrect code — try again."); setCode(""); return; }
    setEnroll(null); setCode("");
    await refresh();
    flash("Two-factor authentication is on.");
  };

  const cancelEnroll = async () => {
    if (enroll?.factorId) { try { await unenroll(enroll.factorId); } catch { /* ignore */ } }
    setEnroll(null); setCode("");
  };

  const removeFactor = async (id) => {
    if (!window.confirm("Turn off two-factor for your account? You'll sign in with just your password.")) return;
    setBusy(true);
    const { error } = await unenroll(id);
    setBusy(false);
    if (error) { flash(error.message || "Could not remove."); return; }
    await refresh();
    flash("Two-factor turned off.");
  };

  const wrap = { position: "fixed", inset: 0, zIndex: 410, background: "rgba(0,0,0,0.82)", backdropFilter: "blur(8px)", overflowY: "auto", display: "flex", justifyContent: "center", alignItems: "flex-start", padding: "24px 16px" };
  const panel = { background: "#111115", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 18, width: "100%", maxWidth: 520, padding: "24px" };
  const input = { width: "100%", background: "rgba(255,255,255,0.05)", border: "1px solid rgba(255,255,255,0.12)", borderRadius: 9, padding: "11px 13px", fontSize: 18, letterSpacing: "0.25em", color: "#fff", fontFamily: "'DM Mono',monospace", textAlign: "center", outline: "none" };

  return (
    <div style={wrap}>
      <div style={panel}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 18 }}>
          <div>
            <div style={{ fontFamily: "'Syne',sans-serif", fontSize: 17, fontWeight: 800, color: "#fff" }}>Security</div>
            <div style={{ fontSize: 11, color: "rgba(255,255,255,0.35)", marginTop: 2 }}>Two-factor authentication for {user?.email}</div>
          </div>
          <button onClick={onClose} style={{ background: "transparent", border: "none", color: "rgba(255,255,255,0.4)", fontSize: 22, cursor: "pointer" }}>✕</button>
        </div>

        {msg && <div style={{ marginBottom: 14, fontSize: 11.5, color: msg.includes("Incorrect") || msg.includes("Could not") ? RED : GREEN, background: "rgba(255,255,255,0.03)", border: "1px solid rgba(255,255,255,0.08)", borderRadius: 8, padding: "8px 11px" }}>{msg}</div>}

        {/* Status */}
        <div style={{ display: "flex", alignItems: "center", gap: 9, background: factors.length ? "rgba(125,206,160,0.07)" : "rgba(240,168,24,0.06)", border: `1px solid ${factors.length ? "rgba(125,206,160,0.22)" : "rgba(240,168,24,0.2)"}`, borderRadius: 11, padding: "12px 14px", marginBottom: 16 }}>
          <span style={{ fontSize: 18 }}>{factors.length ? "🔒" : "🔓"}</span>
          <div>
            <div style={{ fontSize: 13, fontWeight: 700, color: factors.length ? GREEN : GOLD }}>
              {loading ? "Checking…" : factors.length ? "Two-factor is ON" : "Two-factor is OFF"}
            </div>
            <div style={{ fontSize: 10.5, color: "rgba(255,255,255,0.45)", marginTop: 1 }}>
              {factors.length ? "A code from your authenticator is required at sign-in." : "Add an authenticator app for an extra layer of protection."}
            </div>
          </div>
        </div>

        {/* Enrolled factors */}
        {!loading && factors.length > 0 && !enroll && (
          <div style={{ display: "flex", flexDirection: "column", gap: 6, marginBottom: 14 }}>
            {factors.map((f) => (
              <div key={f.id} style={{ display: "flex", alignItems: "center", gap: 10, padding: "11px 13px", background: "rgba(255,255,255,0.022)", border: "1px solid rgba(255,255,255,0.06)", borderRadius: 10 }}>
                <span style={{ fontSize: 15 }}>📱</span>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 12.5, color: "#fff", fontWeight: 600 }}>{f.friendly_name || "Authenticator app"}</div>
                  <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)" }}>TOTP · verified</div>
                </div>
                <button onClick={() => removeFactor(f.id)} disabled={busy}
                  style={{ padding: "5px 11px", borderRadius: 7, border: "1px solid rgba(232,126,126,0.28)", background: "rgba(232,126,126,0.06)", color: RED, fontSize: 11, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>Remove</button>
              </div>
            ))}
          </div>
        )}

        {/* Enrollment flow */}
        {enroll ? (
          <div style={{ background: "rgba(255,255,255,0.02)", border: "1px solid rgba(255,255,255,0.08)", borderRadius: 12, padding: "16px" }}>
            <div style={{ fontSize: 12, color: "rgba(255,255,255,0.6)", lineHeight: 1.6, marginBottom: 12 }}>
              Scan this QR code in your authenticator app (Google Authenticator, Authy, 1Password…), then enter the 6-digit code it shows.
            </div>
            <div style={{ display: "flex", justifyContent: "center", marginBottom: 12 }}>
              <div style={{ background: "#fff", padding: 10, borderRadius: 10 }} dangerouslySetInnerHTML={{ __html: enroll.qr }} />
            </div>
            <div style={{ fontSize: 10.5, color: "rgba(255,255,255,0.4)", textAlign: "center", marginBottom: 12 }}>
              Can't scan? Enter this key manually:<br />
              <span style={{ fontFamily: "'DM Mono',monospace", color: "rgba(255,255,255,0.7)", wordBreak: "break-all", fontSize: 11 }}>{enroll.secret}</span>
            </div>
            <input value={code} onChange={(e) => setCode(e.target.value.replace(/\D/g, "").slice(0, 6))}
              inputMode="numeric" placeholder="••••••" autoFocus onKeyDown={(e) => e.key === "Enter" && confirmEnroll()} style={input} />
            <div style={{ display: "flex", gap: 8, marginTop: 14 }}>
              <button onClick={cancelEnroll} style={{ padding: "11px 16px", borderRadius: 9, border: "1px solid rgba(255,255,255,0.12)", background: "transparent", color: "rgba(255,255,255,0.5)", fontSize: 12, fontWeight: 600, cursor: "pointer", fontFamily: "inherit" }}>Cancel</button>
              <button onClick={confirmEnroll} disabled={busy}
                style={{ flex: 1, padding: "11px", borderRadius: 9, border: "1px solid rgba(125,206,160,0.4)", background: "rgba(125,206,160,0.12)", color: GREEN, fontSize: 13, fontWeight: 700, cursor: busy ? "default" : "pointer", fontFamily: "inherit" }}>
                {busy ? "Verifying…" : "Verify & turn on"}
              </button>
            </div>
          </div>
        ) : !loading && (
          <button onClick={startEnroll} disabled={busy}
            style={{ width: "100%", padding: "12px", borderRadius: 10, border: "1px solid rgba(240,168,24,0.4)", background: "linear-gradient(135deg,rgba(240,168,24,0.18),rgba(240,168,24,0.07))", color: GOLD, fontSize: 13, fontWeight: 700, cursor: busy ? "default" : "pointer", fontFamily: "inherit" }}>
            {factors.length ? "+ Add another authenticator" : "Set up two-factor authentication"}
          </button>
        )}

        <div style={{ textAlign: "center", marginTop: 16, fontSize: 10, color: "rgba(255,255,255,0.2)", lineHeight: 1.6 }}>
          Two-factor protects your account and your linked financial data even if your password is compromised.
        </div>
      </div>
    </div>
  );
}
