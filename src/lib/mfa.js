// src/lib/mfa.js — consumer two-factor (TOTP) via Supabase Auth's built-in MFA.
// Thin wrappers around supabase.auth.mfa.* so the UI and the login gate share one
// surface. No secrets here — Supabase manages the factor secrets server-side.
import { supabase } from "./supabase";

// Assurance level: tells us whether a verified factor exists and whether the
// current session has satisfied it. nextLevel === "aal2" while currentLevel is
// "aal1" means: this user HAS MFA and must complete a challenge to proceed.
export async function getAAL() {
  return supabase.auth.mfa.getAuthenticatorAssuranceLevel();
}

export async function mfaRequired() {
  try {
    const { data, error } = await getAAL();
    if (error || !data) return false; // fail open — never lock a user out on an API hiccup
    return data.nextLevel === "aal2" && data.currentLevel !== "aal2";
  } catch {
    return false;
  }
}

// Verified TOTP factors (what the user manages in Settings → Security).
export async function listFactors() {
  const { data, error } = await supabase.auth.mfa.listFactors();
  return { factors: data?.totp || [], all: data?.all || [], error };
}

// Start enrollment → returns { id, totp: { qr_code (SVG data URI), secret, uri } }.
export async function enrollTotp(friendlyName = "Authenticator") {
  return supabase.auth.mfa.enroll({ factorType: "totp", friendlyName });
}

// Confirm enrollment (or re-verify) by challenging the factor and submitting a code.
export async function verifyCode(factorId, code) {
  const ch = await supabase.auth.mfa.challenge({ factorId });
  if (ch.error) return ch;
  return supabase.auth.mfa.verify({ factorId, challengeId: ch.data.id, code: code.trim() });
}

export async function unenroll(factorId) {
  return supabase.auth.mfa.unenroll({ factorId });
}
