/* eslint-disable react-hooks/exhaustive-deps */
// src/App.jsx — root component with landing page, auth, dashboard, and main app
import { useState, useEffect, useCallback } from "react";
import { supabase, getProfile } from "./lib/supabase";
import LandingPage from "./LandingPage";
import AuthScreen from "./AuthScreen";
import SubscriptionPage from "./SubscriptionPage";
import QuotePublicPage from "./QuotePublicPage";
import Wireway from "./electrical-estimator";

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

  // Public quote page — no auth needed
  if (publicQuoteId) return <QuotePublicPage quoteId={publicQuoteId} />;

  // Loading splash
  if (loading || session === undefined) {
    return (
      <div style={{ minHeight:"100vh", display:"flex", alignItems:"center", justifyContent:"center", background:"#0a0a0c", flexDirection:"column", gap:16 }}>
        <img src="/logo192.png" alt="Wireway" style={{ height:56, width:56, borderRadius:12, objectFit:"cover" }} />
        <div style={{ fontSize:12, color:"rgba(255,255,255,0.3)", fontFamily:"sans-serif", letterSpacing:"0.05em" }}>Loading Wireway...</div>
      </div>
    );
  }

  // Not authenticated — show landing page or auth screen
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

  // Pricing page
  if (showPricing) {
    return (
      <SubscriptionPage
        user={session.user}
        profile={profile}
        onClose={() => setShowPricing(false)}
        onUpgrade={() => { setShowPricing(false); loadProfile(session.user.id); }}
      />
    );
  }

  // Main app
  return (
    <Wireway
      user={session.user}
      profile={profile}
      onProfileUpdate={setProfile}
      onShowPricing={() => setShowPricing(true)}
      paymentBanner={paymentBanner}
      onClearBanner={() => setPaymentBanner("")}
    />
  );
}
