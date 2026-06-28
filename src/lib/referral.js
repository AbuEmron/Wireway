// src/lib/referral.js — referral link + attribution  ·  Phase 2 · Feature 5
import { supabase } from "./supabase";
import { PUBLIC_ORIGIN } from "./nativeBridge";

export const myRefLink = (userId, source = "link") =>
  `${PUBLIC_ORIGIN}/?ref=${userId}&src=${source}`;

// Public log (used from public pages + the landing capture in App).
export async function logReferral({ ref, kind = "visit", source = "link" }) {
  if (!ref) return;
  try {
    await fetch("/api/referral", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ ref, kind, source }),
    });
  } catch { /* attribution must never break anything */ }
}

// Owner reads their own attribution (RLS-scoped).
export async function getReferralStats(userId) {
  const { data } = await supabase
    .from("referral_events").select("kind").eq("ref_user_id", userId);
  const rows = data || [];
  return {
    visits:  rows.filter((r) => r.kind === "visit").length,
    signups: rows.filter((r) => r.kind === "signup").length,
    total:   rows.length,
  };
}
