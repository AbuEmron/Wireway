// src/App.js
// Root component — handles auth state, session, and renders either
// the AuthScreen or the main Wireway app

import { useState, useEffect } from "react";
import { supabase, getProfile } from "./lib/supabase";
import AuthScreen from "./AuthScreen";
import Wireway from "./electrical-estimator";

export default function App() {
  const [session,  setSession]  = useState(undefined); // undefined = loading
  const [profile,  setProfile]  = useState(null);
  const [loading,  setLoading]  = useState(true);

  useEffect(() => {
    // Get initial session
    supabase.auth.getSession().then(({ data: { session } }) => {
      setSession(session);
      if (session?.user) loadProfile(session.user.id);
      else setLoading(false);
    });

    // Listen for auth changes (sign in, sign out, token refresh)
    const { data: { subscription } } = supabase.auth.onAuthStateChange(
      async (event, session) => {
        setSession(session);
        if (session?.user) {
          await loadProfile(session.user.id);
        } else {
          setProfile(null);
          setLoading(false);
        }
      }
    );

    return () => subscription.unsubscribe();
  }, []);

  const loadProfile = async (userId) => {
    setLoading(true);
    const { data } = await getProfile(userId);
    setProfile(data);
    setLoading(false);
  };

  // Loading splash
  if (loading || session === undefined) {
    return (
      <div style={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background: "#0a0a0c",
        flexDirection: "column",
        gap: 16,
      }}>
        <div style={{
          width: 44, height: 44, borderRadius: 10,
          background: "linear-gradient(135deg,#e8c97a,#c9a84c)",
          display: "flex", alignItems: "center", justifyContent: "center",
          fontSize: 22, fontWeight: 800, color: "#0a0a0c",
          fontFamily: "sans-serif",
        }}>W</div>
        <div style={{ fontSize: 12, color: "rgba(255,255,255,0.3)", fontFamily: "sans-serif", letterSpacing: "0.05em" }}>
          Loading...
        </div>
      </div>
    );
  }

  // Not authenticated — show auth screen
  if (!session) {
    return <AuthScreen onAuth={() => {}} />;
  }

  // Authenticated — show main app
  return (
    <Wireway
      user={session.user}
      profile={profile}
      onProfileUpdate={setProfile}
    />
  );
}
