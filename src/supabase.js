// src/lib/supabase.js
import { createClient } from "@supabase/supabase-js";

const supabaseUrl  = process.env.REACT_APP_SUPABASE_URL;
const supabaseAnon = process.env.REACT_APP_SUPABASE_ANON_KEY;

// Safe storage — some in-app browsers (Facebook/Instagram/private mode) block
// localStorage entirely, which crashes the client on init and white-screens the app.
const memoryStore = {};
const safeStorage = {
  getItem: (k) => { try { return window.localStorage.getItem(k); } catch { return memoryStore[k] ?? null; } },
  setItem: (k, v) => { try { window.localStorage.setItem(k, v); } catch { memoryStore[k] = v; } },
  removeItem: (k) => { try { window.localStorage.removeItem(k); } catch { delete memoryStore[k]; } },
};

export const supabase = createClient(supabaseUrl, supabaseAnon, {
  auth: { persistSession: true, autoRefreshToken: true, detectSessionInUrl: true, storage: safeStorage },
});

// ── AUTH ────────────────────────────────────────────────────────────────────
export const signUp = async ({ email, password, fullName }) => {
  const { data, error } = await supabase.auth.signUp({
    email, password,
    options: {
      data: { full_name: fullName },
      emailRedirectTo: `${window.location.origin}/`,
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

export const resetPassword = async (email) => {
  const { error } = await supabase.auth.resetPasswordForEmail(email, {
    redirectTo: `${window.location.origin}/`,
  });
  return { error };
};

export const updatePassword = async (newPassword) => {
  const { error } = await supabase.auth.updateUser({ password: newPassword });
  return { error };
};

export const updateEmail = async (newEmail) => {
  const { error } = await supabase.auth.updateUser({ email: newEmail });
  return { error };
};

// ── PROFILE ─────────────────────────────────────────────────────────────────
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

// ── PLAN HELPERS ─────────────────────────────────────────────────────────────
export const isPro = (profile) => {
  if (!profile) return false;
  return (
    profile.plan === "pro" ||
    profile.plan === "teams" ||
    profile.subscription_status === "trialing" ||
    profile.subscription_status === "active"
  );
};

export const isTrialing = (profile) => profile?.subscription_status === "trialing";

export const trialDaysLeft = (profile) => {
  if (!profile?.trial_ends_at) return 0;
  const diff = new Date(profile.trial_ends_at) - new Date();
  return Math.max(0, Math.ceil(diff / (1000 * 60 * 60 * 24)));
};

// ── QUOTES ───────────────────────────────────────────────────────────────────
export const getQuotes = async (userId) => {
  const { data, error } = await supabase
    .from("quotes")
    .select("id, quote_number, client_name, job_name, total, status, created_at, paid_at, sig_name")
    .eq("user_id", userId)
    .order("created_at", { ascending: false })
    .limit(100);
  return { data: data || [], error };
};

export const getQuote = async (id) => {
  const { data, error } = await supabase
    .from("quotes")
    .select("*")
    .eq("id", id)
    .single();
  return { data, error };
};

export const upsertQuote = async (userId, quoteData) => {
  const payload = {
    user_id:        userId,
    quote_number:   quoteData.quoteNumber,
    client_name:    quoteData.clientName,
    client_email:   quoteData.clientEmail,
    client_phone:   quoteData.clientPhone,
    job_name:       quoteData.jobName,
    notes:          quoteData.notes,
    hourly_rate:    quoteData.hourlyRate,
    markup:         quoteData.markup,
    show_materials: quoteData.showMaterials,
    client_buys_all:quoteData.clientBuysAll,
    flat_rate_mode: quoteData.flatRateMode,
    invoice_mode:   quoteData.invoiceMode,
    invoice_due_date: quoteData.invoiceDueDate || null,
    invoice_paid:   quoteData.invoicePaid,
    tax_enabled:    quoteData.taxEnabled,
    tax_rate:       quoteData.taxRate,
    entries:        quoteData.entries,
    custom_items:   quoteData.customItems,
    total_material: quoteData.totMat,
    total_labor:    quoteData.totLab,
    total_hours:    quoteData.totHrs,
    total_markup:   quoteData.markupAmt,
    total_tax:      quoteData.taxAmt,
    total:          quoteData.total,
    status:         quoteData.status || "draft",
  };

  if (quoteData.id) {
    const { data, error } = await supabase
      .from("quotes").update(payload).eq("id", quoteData.id).eq("user_id", userId).select().single();
    return { data, error };
  } else {
    const { data, error } = await supabase
      .from("quotes").insert(payload).select().single();
    return { data, error };
  }
};

export const deleteQuote = async (id, userId) => {
  const { error } = await supabase.from("quotes").delete().eq("id", id).eq("user_id", userId);
  return { error };
};

export const updateQuoteStatus = async (id, status, extra = {}) => {
  const { data, error } = await supabase
    .from("quotes").update({ status, ...extra }).eq("id", id).select().single();
  return { data, error };
};

// ── CLIENTS ──────────────────────────────────────────────────────────────────
export const getClients = async (userId) => {
  const { data, error } = await supabase
    .from("clients")
    .select("*")
    .eq("user_id", userId)
    .order("name");
  return { data: data || [], error };
};

export const upsertClient = async (userId, clientData) => {
  const payload = {
    user_id: userId,
    name:    clientData.name,
    email:   clientData.email || null,
    phone:   clientData.phone || null,
  };

  // Check if client exists by name for this user
  const { data: existing } = await supabase
    .from("clients").select("id, job_count, total_billed")
    .eq("user_id", userId).ilike("name", clientData.name).maybeSingle();

  if (existing) {
    const { data, error } = await supabase
      .from("clients").update({ ...payload, job_count: (existing.job_count || 0) + 1 })
      .eq("id", existing.id).select().single();
    return { data, error };
  } else {
    const { data, error } = await supabase.from("clients").insert({ ...payload, job_count: 1 }).select().single();
    return { data, error };
  }
};

// ── LOGO UPLOAD ───────────────────────────────────────────────────────────────
export const uploadLogo = async (userId, file) => {
  const ext  = file.name.split(".").pop();
  const path = `${userId}/logo.${ext}`;
  const { error: uploadError } = await supabase.storage
    .from("logos").upload(path, file, { upsert: true, contentType: file.type });
  if (uploadError) return { url: null, error: uploadError };
  const { data } = supabase.storage.from("logos").getPublicUrl(path);
  return { url: data.publicUrl + `?t=${Date.now()}`, error: null };
};

// ── JOBS ─────────────────────────────────────────────────────────────────────

export const getJobs = async (userId, { month, year } = {}) => {
  let query = supabase
    .from("jobs")
    .select("*")
    .eq("user_id", userId)
    .order("scheduled_date", { ascending: true })
    .order("scheduled_time", { ascending: true });
  if (month !== undefined && year !== undefined) {
    const start = `${year}-${String(month+1).padStart(2,'0')}-01`;
    const end   = `${year}-${String(month+1).padStart(2,'0')}-31`;
    query = query.gte("scheduled_date", start).lte("scheduled_date", end);
  }
  const { data, error } = await query;
  return { data: data || [], error };
};

export const upsertJob = async (userId, jobData) => {
  const payload = { ...jobData, user_id: userId };
  if (jobData.id) {
    const { data, error } = await supabase.from("jobs").update(payload).eq("id", jobData.id).eq("user_id", userId).select().single();
    return { data, error };
  }
  const { data, error } = await supabase.from("jobs").insert(payload).select().single();
  return { data, error };
};

export const deleteJob = async (id, userId) => {
  const { error } = await supabase.from("jobs").delete().eq("id", id).eq("user_id", userId);
  return { error };
};

export const updateJobStatus = async (id, status) => {
  const { data, error } = await supabase.from("jobs").update({ status }).eq("id", id).select().single();
  return { data, error };
};

// ── PHOTOS ───────────────────────────────────────────────────────────────────

export const getPhotos = async (quoteId) => {
  const { data, error } = await supabase.from("photos").select("*").eq("quote_id", quoteId).order("created_at");
  return { data: data || [], error };
};

export const uploadPhoto = async (userId, quoteId, file) => {
  const ext  = file.name.split(".").pop();
  const path = `${userId}/${quoteId}/${Date.now()}.${ext}`;
  const { error: upErr } = await supabase.storage.from("photos").upload(path, file, { contentType: file.type });
  if (upErr) return { url: null, error: upErr };
  const { data: urlData } = supabase.storage.from("photos").getPublicUrl(path);
  const { data, error } = await supabase.from("photos").insert({
    user_id: userId, quote_id: quoteId, url: urlData.publicUrl,
    size_bytes: file.size,
  }).select().single();
  return { data, error };
};

export const deletePhoto = async (id) => {
  const { error } = await supabase.from("photos").delete().eq("id", id);
  return { error };
};

export const updateClient = async (userId, clientId, fields) => {
  const { data, error } = await supabase
    .from("clients")
    .update({ name: fields.name, email: fields.email || null, phone: fields.phone || null })
    .eq("id", clientId).eq("user_id", userId)
    .select().single();
  return { data, error };
};

export const deleteClient = async (userId, clientId) => {
  const { error } = await supabase
    .from("clients").delete().eq("id", clientId).eq("user_id", userId);
  return { error };
};

// ── THEME ────────────────────────────────
export const saveThemePref = async (userId, theme) => {
  try { await supabase.from("profiles").update({ theme }).eq("id", userId); } catch { /* non-critical */ }
};
