// src/App.jsx — Full auth + routing with subscription enforcement
import { useState, useEffect, useCallback } from "react";
import { supabase, getProfile } from "./lib/supabase";
import AuthScreen from "./AuthScreen";
import SubscriptionPage from "./SubscriptionPage";
import Wireway from "./electrical-estimator";

export default function App() {
  const [session,     setSession]     = useState(undefined);
  const [profile,     setProfile]     = useState(null);
  const [loading,     setLoading]     = useState(true);
  const [showPricing, setShowPricing] = useState(false);
  const [paymentBanner, setPaymentBanner] = useState("");

  const loadProfile = useCallback(async (userId) => {
    setLoading(true);
    const { data } = await getProfile(userId);
    setProfile(data);
    setLoading(false);
  }, []);

  useEffect(() => {
    // Handle Stripe redirect params
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

    // Auth session
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

  // Refresh profile every 30s when subscription is pending
  useEffect(() => {
    if (!session?.user || !profile) return;
    if (profile.subscription_status === "trialing" || profile.plan !== "free") return;
    const timer = setInterval(() => loadProfile(session.user.id), 30000);
    return () => clearInterval(timer);
  }, [session, profile, loadProfile]);

  // ── Loading ──
  if (loading || session === undefined) {
    return (
      <div style={{ minHeight:"100vh", display:"flex", alignItems:"center", justifyContent:"center", background:"#0a0a0c", flexDirection:"column", gap:16 }}>
        <div style={{ width:44, height:44, borderRadius:10, background:"linear-gradient(135deg,#e8c97a,#c9a84c)", display:"flex", alignItems:"center", justifyContent:"center", fontSize:22, fontWeight:800, color:"#0a0a0c", fontFamily:"sans-serif" }}>W</div>
        <div style={{ fontSize:12, color:"rgba(255,255,255,0.3)", fontFamily:"sans-serif", letterSpacing:"0.05em" }}>Loading Wireway...</div>
      </div>
    );
  }

  // ── Not authenticated ──
  if (!session) return <AuthScreen onAuth={() => {}} />;

  // ── Pricing page ──
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

  // ── Main app ──
  return (
    <Wireway
      user={session.user}
      profile={profile}
      onProfileUpdate={(p) => { setProfile(p); if (onProfileUpdate) onProfileUpdate(p); }}
      onShowPricing={() => setShowPricing(true)}
      paymentBanner={paymentBanner}
      onClearBanner={() => setPaymentBanner("")}
    />
  );
}
