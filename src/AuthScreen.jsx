'use client';
// src/AuthScreen.jsx — gyroscopic auth (sign in / sign up / reset).
// Same Supabase logic and same props as before; new UI on top.
import { useState, useRef, useCallback } from "react";
import GyroField from "./components/GyroField/GyroField";
import Brand from "./components/Brand";
import { signIn, signUp, resetPassword } from "./lib/supabase";
import "./AuthScreen.css";

export default function AuthScreen({ onAuth, initialMode = "signin", onBack }) {
  const [mode,     setMode]     = useState(initialMode);
  const [email,    setEmail]    = useState("");
  const [password, setPassword] = useState("");
  const [name,     setName]     = useState("");
  const [loading,  setLoading]  = useState(false);
  const [error,    setError]    = useState("");
  const [success,  setSuccess]  = useState("");

  // glow the card each time a current pulse lands
  const [energized, setEnergized] = useState(false);
  const timer = useRef(null);
  const onEnergize = useCallback(() => {
    setEnergized(true);
    clearTimeout(timer.current);
    timer.current = setTimeout(() => setEnergized(false), 520);
  }, []);

  const handle = async () => {
    setError(""); setSuccess(""); setLoading(true);
    try {
      if (mode === "signin") {
        const { error } = await signIn({ email, password });
        if (error) throw error;
        onAuth();
      } else if (mode === "signup") {
        if (!name.trim()) throw new Error("Please enter your full name.");
        if (password.length < 8) throw new Error("Password must be at least 8 characters.");
        const { error } = await signUp({ email, password, fullName: name });
        if (error) throw error;
        setSuccess("Check your email to confirm your account, then sign in.");
        setMode("signin");
      } else if (mode === "reset") {
        const { error } = await resetPassword(email);
        if (error) throw error;
        setSuccess("Password reset email sent — check your inbox.");
      }
    } catch (err) {
      setError(err.message || "Something went wrong. Try again.");
    } finally {
      setLoading(false);
    }
  };

  const onKey = (e) => { if (e.key === "Enter") handle(); };
  const switchMode = (m) => { setMode(m); setError(""); setSuccess(""); };

  const title =
    mode === "signin" ? "Welcome back" :
    mode === "signup" ? "Create your account" : "Reset password";
  const subtitle =
    mode === "signin" ? "Sign in to your estimates, clients, and quotes." :
    mode === "signup" ? "30-day free trial. No credit card required." :
    "Enter your email and we'll send a reset link.";

  return (
    <>
      <GyroField variant="circuit" onEnergize={onEnergize} />
      <div className="ww-veil-auth" />
      <div className="ww-page ww-auth">
        <div className={"card" + (energized ? " energize" : "")}>
          <Brand size={18} src="/logo192.png" />
          <div className="tag">
            {mode === "signin" ? "CLOSE THE CIRCUIT" : mode === "signup" ? "WIRE IN" : "RESET"}
          </div>

          <h2 className="title">{title}</h2>
          <p className="subtitle">{subtitle}</p>

          {mode === "signup" && (
            <div className="fld">
              <label htmlFor="ww-name">Full name</label>
              <input id="ww-name" value={name} onChange={(e) => setName(e.target.value)}
                placeholder="Jordan Rivera" onKeyDown={onKey} />
            </div>
          )}

          <div className="fld">
            <label htmlFor="ww-email">Email</label>
            <input id="ww-email" type="email" value={email} onChange={(e) => setEmail(e.target.value)}
              placeholder="you@shop.com" autoComplete="email" onKeyDown={onKey} />
          </div>

          {mode !== "reset" && (
            <div className="fld">
              <label htmlFor="ww-pw">Password</label>
              <input id="ww-pw" type="password" value={password} onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••" autoComplete={mode === "signin" ? "current-password" : "new-password"}
                onKeyDown={onKey} />
            </div>
          )}

          {error   && <div className="msg err">{error}</div>}
          {success && <div className="msg ok">{success}</div>}

          <button type="button" className="btn" onClick={handle} disabled={loading}>
            {loading ? "Please wait…" : mode === "signin" ? "Sign in" : mode === "signup" ? "Create account" : "Send reset link"}
          </button>

          {mode === "signup" && (
            <p style={{ fontSize:11, lineHeight:1.5, color:"rgba(255,255,255,0.4)", textAlign:"center", marginTop:12 }}>
              By creating an account, you agree to our{" "}
              <a href="/terms.html" target="_blank" rel="noreferrer" style={{ color:"rgba(255,255,255,0.62)" }}>Terms of Service</a>
              {" "}and{" "}
              <a href="/privacy.html" target="_blank" rel="noreferrer" style={{ color:"rgba(255,255,255,0.62)" }}>Privacy Policy</a>.
              {" "}See also our{" "}
              <a href="/security.html" target="_blank" rel="noreferrer" style={{ color:"rgba(255,255,255,0.62)" }}>Security Policy</a>.
            </p>
          )}

          <div className="switches">
            {mode === "signin" && (
              <>
                <button type="button" className="link" onClick={() => switchMode("signup")}>
                  No account? <span className="accent">Sign up free</span>
                </button>
                <button type="button" className="link dim" onClick={() => switchMode("reset")}>
                  Forgot password?
                </button>
              </>
            )}
            {mode === "signup" && (
              <button type="button" className="link" onClick={() => switchMode("signin")}>
                Have an account? <span className="accent">Sign in</span>
              </button>
            )}
            {mode === "reset" && (
              <button type="button" className="link" onClick={() => switchMode("signin")}>
                ← Back to sign in
              </button>
            )}
          </div>

          {onBack && (
            <button type="button" className="back" onClick={onBack}>← Back to wirewaypro.com</button>
          )}
        </div>
      </div>
    </>
  );
}
