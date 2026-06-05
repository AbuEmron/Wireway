// src/lib/supabase.js
// Supabase client — import this everywhere you need DB or auth access
//
// Environment variables (set in Vercel dashboard AND .env.local for dev):
//   REACT_APP_SUPABASE_URL   — from Supabase project Settings → API
//   REACT_APP_SUPABASE_ANON_KEY — from Supabase project Settings → API

import { createClient } from "@supabase/supabase-js";

const supabaseUrl  = process.env.REACT_APP_SUPABASE_URL;
const supabaseAnon = process.env.REACT_APP_SUPABASE_ANON_KEY;

if (!supabaseUrl || !supabaseAnon) {
  console.error(
    "Missing Supabase env vars. Add REACT_APP_SUPABASE_URL and " +
    "REACT_APP_SUPABASE_ANON_KEY to your .env.local and Vercel environment."
  );
}

export const supabase = createClient(supabaseUrl, supabaseAnon, {
  auth: {
    // Persist session in localStorage so users stay logged in
    persistSession: true,
    autoRefreshToken: true,
    detectSessionInUrl: true,
  },
});

// ── AUTH HELPERS ──────────────────────────────────────────────

export const signUp = async ({ email, password, fullName }) => {
  const { data, error } = await supabase.auth.signUp({
    email,
    password,
    options: {
      data: { full_name: fullName },
      emailRedirectTo: `${window.location.origin}/auth/callback`,
    },
  });
  return { data, error };
};

export const signIn = async ({ email, password }) => {
  const { data, error } = await supabase.auth.signInWithPassword({ email, password });
  return { data, error };
};

export const signOut = async () => {
  const { error } = await supabase.auth.signOut();
  return { error };
};

export const getSession = async () => {
  const { data: { session } } = await supabase.auth.getSession();
  return session;
};

export const getUser = async () => {
  const { data: { user } } = await supabase.auth.getUser();
  return user;
};

export const resetPassword = async (email) => {
  const { error } = await supabase.auth.resetPasswordForEmail(email, {
    redirectTo: `${window.location.origin}/auth/reset`,
  });
  return { error };
};

// ── PROFILE HELPERS ───────────────────────────────────────────

export const getProfile = async (userId) => {
  const { data, error } = await supabase
    .from("profiles")
    .select("*")
    .eq("id", userId)
    .single();
  return { data, error };
};

export const updateProfile = async (userId, updates) => {
  const { data, error } = await supabase
    .from("profiles")
    .update(updates)
    .eq("id", userId)
    .select()
    .single();
  return { data, error };
};

// ── QUOTE HELPERS ─────────────────────────────────────────────

export const getQuotes = async (userId, { status, limit = 50 } = {}) => {
  let query = supabase
    .from("quotes")
    .select("id, quote_number, client_name, job_name, total, status, created_at, paid_at, sig_name")
    .eq("user_id", userId)
    .order("created_at", { ascending: false })
    .limit(limit);
  if (status) query = query.eq("status", status);
  const { data, error } = await query;
  return { data, error };
};

export const getQuote = async (id) => {
  const { data, error } = await supabase
    .from("quotes")
    .select("*")
    .eq("id", id)
    .single();
  return { data, error };
};

export const saveQuote = async (userId, quoteData) => {
  const payload = { ...quoteData, user_id: userId };
  if (quoteData.id) {
    // Update existing
    const { data, error } = await supabase
      .from("quotes")
      .update(payload)
      .eq("id", quoteData.id)
      .eq("user_id", userId)
      .select()
      .single();
    return { data, error };
  } else {
    // Insert new
    const { data, error } = await supabase
      .from("quotes")
      .insert(payload)
      .select()
      .single();
    return { data, error };
  }
};

export const deleteQuote = async (id, userId) => {
  const { error } = await supabase
    .from("quotes")
    .delete()
    .eq("id", id)
    .eq("user_id", userId);
  return { error };
};

export const updateQuoteStatus = async (id, status, extra = {}) => {
  const { data, error } = await supabase
    .from("quotes")
    .update({ status, ...extra })
    .eq("id", id)
    .select()
    .single();
  return { data, error };
};

// ── CLIENT HELPERS ────────────────────────────────────────────

export const getClients = async (userId) => {
  const { data, error } = await supabase
    .from("clients")
    .select("*")
    .eq("user_id", userId)
    .order("name");
  return { data, error };
};

export const upsertClient = async (userId, clientData) => {
  const payload = { ...clientData, user_id: userId };
  const { data, error } = await supabase
    .from("clients")
    .upsert(payload, { onConflict: "id" })
    .select()
    .single();
  return { data, error };
};

// ── LOGO UPLOAD ───────────────────────────────────────────────

export const uploadLogo = async (userId, file) => {
  const ext = file.name.split(".").pop();
  const path = `${userId}/logo.${ext}`;
  const { error: uploadError } = await supabase.storage
    .from("logos")
    .upload(path, file, { upsert: true, contentType: file.type });
  if (uploadError) return { url: null, error: uploadError };
  const { data } = supabase.storage.from("logos").getPublicUrl(path);
  return { url: data.publicUrl, error: null };
};
