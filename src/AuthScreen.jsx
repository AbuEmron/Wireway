// src/AuthScreen.jsx
import { useState } from "react";
import { signIn, signUp, resetPassword } from "./lib/supabase";

const IS = {
  background: "rgba(255,255,255,0.04)",
  border: "1px solid rgba(255,255,255,0.07)",
  borderRadius: 8,
  padding: "10px 13px",
  fontSize: 14,
  color: "#fff",
  fontFamily: "inherit",
  width: "100%",
  outline: "none",
  transition: "border-color 0.15s",
};

export default function AuthScreen({ onAuth }) {
  const [mode,     setMode]     = useState("signin");
  const [email,    setEmail]    = useState("");
  const [password, setPassword] = useState("");
  const [name,     setName]     = useState("");
  const [loading,  setLoading]  = useState(false);
  const [error,    setError]    = useState("");
  const [success,  setSuccess]  = useState("");

  const focusGold = e => e.target.style.borderColor = "rgba(232,201,122,0.4)";
  const blurGray  = e => e.target.style.borderColor = "rgba(255,255,255,0.07)";

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

  return (
    <>
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=DM+Mono:wght@400;500&family=Syne:wght@700;800&family=DM+Sans:wght@400;500;600&display=swap');
        *{box-sizing:border-box;margin:0;padding:0}
        body{background:#0a0a0c}
        @keyframes fadeUp{from{opacity:0;transform:translateY(14px)}to{opacity:1;transform:translateY(0)}}
      `}</style>

      <div style={{ minHeight:"100vh", display:"flex", flexDirection:"column", alignItems:"center", justifyContent:"center", background:"radial-gradient(ellipse 80% 50% at 50% 0%,rgba(232,201,122,0.07) 0%,transparent 60%),#0a0a0c", fontFamily:"'DM Sans',sans-serif", padding:"24px 20px" }}>

        <div style={{ display:"flex", alignItems:"center", gap:10, marginBottom:40, animation:"fadeUp 0.4s ease both" }}>
          <div style={{ width:38, height:38, borderRadius:9, background:"linear-gradient(135deg,#e8c97a,#c9a84c)", display:"flex", alignItems:"center", justifyContent:"center", fontSize:18, fontWeight:800, color:"#0a0a0c" }}>W</div>
          <div>
            <div style={{ fontFamily:"'Syne',sans-serif", fontSize:22, fontWeight:800, letterSpacing:"-0.03em", color:"#fff" }}>Wireway</div>
            <div style={{ fontSize:10, color:"rgba(232,201,122,0.6)", letterSpacing:"0.08em", textTransform:"uppercase" }}>NEC 2023 Professional</div>
          </div>
        </div>

        <div style={{ width:"100%", maxWidth:380, background:"rgba(255,255,255,0.025)", border:"1px solid rgba(255,255,255,0.07)", borderRadius:18, padding:"28px 24px", animation:"fadeUp 0.4s ease 0.05s both" }}>

          <div style={{ fontFamily:"'Syne',sans-serif", fontSize:18, fontWeight:800, color:"#fff", marginBottom:6, letterSpacing:"-0.02em" }}>
            {mode === "signin" ? "Welcome back" : mode === "signup" ? "Create your account" : "Reset password"}
          </div>
          <div style={{ fontSize:12, color:"rgba(255,255,255,0.35)", marginBottom:24, lineHeight:1.6 }}>
            {mode === "signin"
              ? "Sign in to access your estimates, clients, and quotes."
              : mode === "signup"
              ? "30-day free trial. No credit card required."
              : "Enter your email and we'll send a reset link."}
          </div>

          <div style={{ display:"flex", flexDirection:"column", gap:10, marginBottom:16 }}>
            {mode === "signup" && (
              <input placeholder="Full name" value={name} onChange={e => setName(e.target.value)}
                style={IS} onFocus={focusGold} onBlur={blurGray} />
            )}
            <input placeholder="Email address" type="email" value={email} onChange={e => setEmail(e.target.value)}
              style={IS} onFocus={focusGold} onBlur={blurGray}
              onKeyDown={e => e.key === "Enter" && handle()} />
            {mode !== "reset" && (
              <input placeholder="Password" type="password" value={password} onChange={e => setPassword(e.target.value)}
                style={IS} onFocus={focusGold} onBlur={blurGray}
                onKeyDown={e => e.key === "Enter" && handle()} />
            )}
          </div>

          {error   && <div style={{ fontSize:12, color:"#e87e7e", background:"rgba(232,126,126,0.08)", border:"1px solid rgba(232,126,126,0.2)", borderRadius:7, padding:"8px 10px", marginBottom:12, lineHeight:1.5 }}>{error}</div>}
          {success && <div style={{ fontSize:12, color:"#7dcea0", background:"rgba(100,220,130,0.08)", border:"1px solid rgba(100,220,130,0.2)", borderRadius:7, padding:"8px 10px", marginBottom:12, lineHeight:1.5 }}>{success}</div>}

          <button onClick={handle} disabled={loading} style={{ width:"100%", padding:"13px", background: loading ? "rgba(232,201,122,0.08)" : "linear-gradient(135deg,rgba(232,201,122,0.22),rgba(232,201,122,0.1))", border:"1px solid rgba(232,201,122,0.35)", borderRadius:10, color: loading ? "rgba(232,201,122,0.4)" : "#e8c97a", fontSize:14, fontWeight:700, cursor: loading ? "default" : "pointer", fontFamily:"inherit", transition:"all 0.2s", marginBottom:16 }}>
            {loading ? "Please wait..." : mode === "signin" ? "Sign In" : mode === "signup" ? "Create Account" : "Send Reset Link"}
          </button>

          <div style={{ display:"flex", flexDirection:"column", gap:8, alignItems:"center" }}>
            {mode === "signin" && (
              <>
                <button onClick={() => { setMode("signup"); setError(""); setSuccess(""); }} style={{ background:"transparent", border:"none", color:"rgba(255,255,255,0.4)", fontSize:12, cursor:"pointer", fontFamily:"inherit" }}>
                  Don't have an account? <span style={{ color:"#e8c97a", fontWeight:600 }}>Sign up free</span>
                </button>
                <button onClick={() => { setMode("reset"); setError(""); setSuccess(""); }} style={{ background:"transparent", border:"none", color:"rgba(255,255,255,0.25)", fontSize:11, cursor:"pointer", fontFamily:"inherit" }}>
                  Forgot password?
                </button>
              </>
            )}
            {mode === "signup" && (
              <button onClick={() => { setMode("signin"); setError(""); setSuccess(""); }} style={{ background:"transparent", border:"none", color:"rgba(255,255,255,0.4)", fontSize:12, cursor:"pointer", fontFamily:"inherit" }}>
                Already have an account? <span style={{ color:"#e8c97a", fontWeight:600 }}>Sign in</span>
              </button>
            )}
            {mode === "reset" && (
              <button onClick={() => { setMode("signin"); setError(""); setSuccess(""); }} style={{ background:"transparent", border:"none", color:"rgba(255,255,255,0.4)", fontSize:12, cursor:"pointer", fontFamily:"inherit" }}>
                ← Back to sign in
              </button>
            )}
          </div>
        </div>

        <div style={{ marginTop:32, fontSize:10, color:"rgba(255,255,255,0.15)", letterSpacing:"0.05em", textAlign:"center" }}>
          WIREWAY · NEC 2023 · PROFESSIONAL ELECTRICAL ESTIMATING
        </div>
      </div>
    </>
  );
}
