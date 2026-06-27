/* eslint-disable react-hooks/exhaustive-deps */
// src/App.jsx — root component with landing page, auth, dashboard, and main app
import { useState, useEffect, useCallback } from "react";
import "./styles/tokens.css";
import "./styles/interactions.css";
import "./lib/errorMonitor"; // auto-starts crash logging → Supabase error_logs
import { supabase, getProfile } from "./lib/supabase";
import LandingPage from "./LandingPage";
import GyroBackdrop from "./components/GyroBackdrop";
import AuthScreen from "./AuthScreen";
import SubscriptionPage from "./SubscriptionPage";
import QuotePublicPage from "./QuotePublicPage";
import PayDrawsPublicPage from "./PayDrawsPublicPage";
import Wireway from "./electrical-estimator";
import { logReferral } from "./lib/referral";
export default function App() {
  const [session,       setSession]       = useState(undefined);
  const [profile,       setProfile]       = useState(null);
  const [loading,       setLoading]       = useState(true);
  const [showPricing,   setShowPricing]   = useState(false);
  const [authMode,      setAuthMode]      = useState(null); // null | "signin" | "signup"
  const [paymentBanner, setPaymentBanner] = useState("");
  // Check if this is a public quote link: /quote/[id]
  const path = window.location.pathname;
  const quoteMatch = path.match(/^\/quote\/([a-f0-9-]{36})$/i);
  const publicQuoteId = quoteMatch?.[1];
  const payMatch = path.match(/^\/pay\/([a-f0-9-]{36})$/i);
  const publicPayJobId = payMatch?.[1];
  // Referral capture — log a visit from a contractor's branded public doc, stash
  // the ref for signup attribution, then clean the URL so refresh doesn't re-log.
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const ref = params.get("ref");
    if (!ref) return;
    logReferral({ ref, kind: "visit", source: params.get("src") || "link" });
    try { window.localStorage.setItem("ww_ref", ref); } catch { /* ignore */ }
    params.delete("ref"); params.delete("src");
    const qs = params.toString();
    window.history.replaceState({}, "", window.location.pathname + (qs ? `?${qs}` : ""));
  }, []);

  // Signup attribution — when a brand-new account lands and we have a stashed ref.
  useEffect(() => {
    if (!session?.user) return;
    let ref = null;
    try { ref = window.localStorage.getItem("ww_ref"); } catch { /* ignore */ }
    if (ref && ref !== session.user.id) {
      const created = session.user.created_at ? new Date(session.user.created_at).getTime() : 0;
      if (created && Date.now() - created < 10 * 60 * 1000) logReferral({ ref, kind: "signup", source: "app" });
    }
    try { window.localStorage.removeItem("ww_ref"); } catch { /* ignore */ }
  }, [session?.user?.id]);

  const loadProfile = useCallback(async (userId) => {
    setLoading(true);
    const { data } = await getProfile(userId);
    setProfile(data);
    setLoading(false);
  }, []);
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    if (params.get("subscription") === "success") {
      setPaymentBanner("pro");
      setShowPricing(false);
      window.history.replaceState({}, "", "/");
    }
    if (params.get("payment") === "success") {
      setPaymentBanner("paid");
      window.history.replaceState({}, "", "/");
    }
    supabase.auth.getSession().then(({ data: { session } }) => {
      setSession(session);
      if (session?.user) loadProfile(session.user.id);
      else setLoading(false);
    });
    const { data: { subscription } } = supabase.auth.onAuthStateChange(
      async (event, session) => {
        setSession(session);
        if (session?.user) await loadProfile(session.user.id);
        else { setProfile(null); setLoading(false); }
      }
    );
    return () => subscription.unsubscribe();
  }, [loadProfile]);
  // Public quote page — no auth needed (original colors + field)
  if (publicQuoteId) return (
    <GyroBackdrop variant="synapse" reskin={false}>
      <QuotePublicPage quoteId={publicQuoteId} />
    </GyroBackdrop>
  );
  // Public tap-to-pay / homeowner portal — no auth needed
  if (publicPayJobId) return (
    <GyroBackdrop variant="synapse" reskin={false}>
      <PayDrawsPublicPage jobId={publicPayJobId} />
    </GyroBackdrop>
  );
  // Loading splash
  if (loading || session === undefined) {
    return (
      <GyroBackdrop variant="circuit" reskin={false}>
        <div style={{ minHeight:"100vh", display:"flex", alignItems:"center", justifyContent:"center", background:"transparent", flexDirection:"column", gap:16 }}>
          <img src="/logo192.png" alt="Wireway" style={{ height:56, width:56, borderRadius:12, objectFit:"cover" }} />
          <div style={{ fontSize:12, color:"rgba(255,255,255,0.3)", fontFamily:"sans-serif", letterSpacing:"0.05em" }}>Loading Wireway...</div>
        </div>
      </GyroBackdrop>
    );
  }
  // Not authenticated — YOUR original landing (field mounted inside it) or the auth screen
  if (!session) {
    if (authMode) {
      return (
        <AuthScreen
          initialMode={authMode}
          onAuth={() => setAuthMode(null)}
          onBack={() => setAuthMode(null)}
        />
      );
    }
    return (
      <LandingPage
        onSignIn={() => setAuthMode("signin")}
        onSignUp={() => setAuthMode("signup")}
      />
    );
  }
  // Pricing page (original colors + field)
  if (showPricing) {
    return (
      <GyroBackdrop variant="horizon" reskin={false}>
        <SubscriptionPage
          user={session.user}
          profile={profile}
          onClose={() => setShowPricing(false)}
          onUpgrade={() => { setShowPricing(false); loadProfile(session.user.id); }}
        />
      </GyroBackdrop>
    );
  }
  // Main app (original colors + field)
  return (
    <GyroBackdrop variant="steel" reskin={false}>
      <Wireway
        user={session.user}
        profile={profile}
        onProfileUpdate={setProfile}
        onShowPricing={() => setShowPricing(true)}
        paymentBanner={paymentBanner}
        onClearBanner={() => setPaymentBanner("")}
      />
    </GyroBackdrop>
  );
}
