// src/lib/supabase.js
import { createClient } from "@supabase/supabase-js";
import { PUBLIC_ORIGIN } from "./nativeBridge";

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

// ── OFFLINE QUOTE QUEUE ───────────────────────────────────────────────────────
// Lets quotes save with no signal: a failed/offline save is held locally and
// synced automatically when the connection returns. Self-contained + fail-safe;
// if any of this errors it can never block a save. Only affects quotes.
const QUEUE_KEY = "wireway_quote_queue_v1";
let queueCache = null;

const loadQueue = () => {
  if (queueCache) return queueCache;
  try { queueCache = JSON.parse(window.localStorage.getItem(QUEUE_KEY) || "[]"); }
  catch { queueCache = []; }
  return Array.isArray(queueCache) ? queueCache : (queueCache = []);
};

const saveQueue = (arr) => {
  queueCache = arr;
  try { window.localStorage.setItem(QUEUE_KEY, JSON.stringify(arr)); } catch { /* memory-only fallback */ }
  notifySync();
};

const genUUID = () => {
  try { if (typeof crypto !== "undefined" && crypto.randomUUID) return crypto.randomUUID(); } catch { /* fall through */ }
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0; const v = c === "x" ? r : (r & 0x3) | 0x8; return v.toString(16);
  });
};

const looksOffline = (error) => {
  if (typeof navigator !== "undefined" && !navigator.onLine) return true;
  const m = (error?.message || "").toLowerCase();
  return m.includes("fetch") || m.includes("network") || error?.name === "TypeError";
};

const enqueueQuote = (userId, payload) => {
  const arr = loadQueue().filter((it) => it.payload.id !== payload.id); // replace any prior copy of same quote
  arr.push({ id: payload.id, userId, payload, createdAt: Date.now(), attempts: 0 });
  saveQueue(arr);
};

const removeQueued = (id) => {
  const arr = loadQueue();
  if (arr.some((it) => it.payload.id === id)) saveQueue(arr.filter((it) => it.payload.id !== id));
};

// Sync-status broadcasting (optional UI can subscribe to show a "pending" badge)
const syncListeners = new Set();
export const getPendingSyncCount = () => loadQueue().length;
const notifySync = () => {
  const n = loadQueue().length;
  syncListeners.forEach((fn) => { try { fn(n); } catch { /* ignore */ } });
};
export const subscribeSync = (fn) => {
  syncListeners.add(fn);
  try { fn(loadQueue().length); } catch { /* ignore */ }
  return () => syncListeners.delete(fn);
};

let flushing = false;
export const flushQuoteQueue = async () => {
  if (flushing) return;
  if (typeof navigator !== "undefined" && !navigator.onLine) return;
  flushing = true;
  try {
    for (const item of [...loadQueue()]) {
      try {
        const { error } = await supabase.from("quotes").upsert(item.payload); // idempotent on id
        if (!error) {
          removeQueued(item.payload.id);
        } else if (looksOffline(error)) {
          break; // still no real connection — try again later
        } else {
          // Permanent error (e.g. session expired). Keep the data, count attempts,
          // and stop auto-retrying after several tries so it can't loop forever.
          const bumped = loadQueue().map((it) =>
            it.payload.id === item.payload.id ? { ...it, attempts: (it.attempts || 0) + 1 } : it
          );
          saveQueue(bumped);
          const cur = bumped.find((it) => it.payload.id === item.payload.id);
          if (cur && cur.attempts >= 5) removeQueued(item.payload.id);
        }
      } catch { break; } // network threw — stop, retry on next online event
    }
  } finally { flushing = false; }
};

if (typeof window !== "undefined") {
  window.addEventListener("online", () => { flushQuoteQueue(); });
  setTimeout(() => { flushQuoteQueue(); }, 2500); // catch anything left from a prior session
}

const queuedToListItem = (it) => ({
  id: it.payload.id,
  quote_number: it.payload.quote_number,
  client_name: it.payload.client_name,
  job_name: it.payload.job_name,
  total: it.payload.total,
  status: it.payload.status || "draft",
  created_at: new Date(it.createdAt).toISOString(),
  paid_at: null,
  sig_name: null,
  _pendingSync: true, // harmless extra flag; UI can use it to show "waiting to sync"
});

// ── AUTH ────────────────────────────────────────────────────────────────────
export const signUp = async ({ email, password, fullName }) => {
  const { data, error } = await supabase.auth.signUp({
    email, password,
    options: {
      data: { full_name: fullName },
      emailRedirectTo: `${PUBLIC_ORIGIN}/`,
    },
  });
  return { data, error };
};

export const signIn = async ({ email, password }) => {
  const { data, error } = await supabase.auth.signInWithPassword({ email, password });
  return { data, error };
};

export const signOut = async () => {
  try { window.localStorage.removeItem("wireway_session_v1"); } catch { /* ignore */ }
  const { error } = await supabase.auth.signOut();
  return { error };
};

export const resetPassword = async (email) => {
  const { error } = await supabase.auth.resetPasswordForEmail(email, {
    redirectTo: `${PUBLIC_ORIGIN}/`,
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

  // Merge in any quotes still waiting to sync so they show in the list immediately.
  const serverList = data || [];
  const serverIds = new Set(serverList.map((r) => r.id));
  const pending = loadQueue()
    .filter((it) => it.userId === userId && !serverIds.has(it.payload.id))
    .map(queuedToListItem);

  return { data: [...pending, ...serverList], error };
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
  const isNew = !quoteData.id;
  const id = quoteData.id || genUUID(); // stable id whether saved online or offline
  const payload = {
    id,
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

  // Known offline up front → hold locally, report success so the user isn't blocked.
  if (typeof navigator !== "undefined" && !navigator.onLine) {
    enqueueQuote(userId, payload);
    return { data: { ...payload, created_at: new Date().toISOString() }, error: null, _queued: true };
  }

  try {
    let res;
    if (isNew) {
      res = await supabase.from("quotes").insert(payload).select().single();
    } else {
      res = await supabase.from("quotes").update(payload).eq("id", id).eq("user_id", userId).select().single();
    }

    if (res.error) {
      if (looksOffline(res.error)) {
        enqueueQuote(userId, payload); // looked like a connection drop — hold + sync later
        return { data: { ...payload, created_at: new Date().toISOString() }, error: null, _queued: true };
      }
      return res; // genuine error — surface it as before
    }

    removeQueued(id); // saved for real — clear any pending copy
    return res;
  } catch {
    enqueueQuote(userId, payload); // network threw — hold + sync later
    return { data: { ...payload, created_at: new Date().toISOString() }, error: null, _queued: true };
  }
};

export const deleteQuote = async (id, userId) => {
  removeQueued(id); // also drop it from the sync queue if it was waiting
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

// Partial update of a job (owner-scoped) — used for accept/decline reschedule and
// acknowledging client changes from the calendar.
export const updateJob = async (id, userId, patch) => {
  const { data, error } = await supabase
    .from("jobs")
    .update({ ...patch, updated_at: new Date().toISOString() })
    .eq("id", id)
    .eq("user_id", userId)
    .select()
    .single();
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

// ── WIREWAY ELITE (industrial tier) ──────
export const getEliteEstimates = async (userId) =>
  supabase.from("elite_estimates")
    .select("id, job_name, co_mode, parent_ref, totals, updated_at")
    .eq("user_id", userId)
    .order("updated_at", { ascending: false });

export const getEliteEstimate = async (id) =>
  supabase.from("elite_estimates").select("*").eq("id", id).single();

export const upsertEliteEstimate = async (row) =>
  supabase.from("elite_estimates")
    .upsert({ ...row, updated_at: new Date().toISOString() })
    .select("id")
    .single();

export const deleteEliteEstimate = async (id) =>
  supabase.from("elite_estimates").delete().eq("id", id);

// Dark until launch: unlocks for plan="elite" profiles, or on this device
// via localStorage "wireway_elite_preview" = "1" for pre-launch testing.
const ELITE_PREVIEW_ACCOUNTS = ["elite@wirewaypro.com"];

export const isElite = (profile) => {
  if (profile?.plan === "elite") return true;
  return !!profile?.email && ELITE_PREVIEW_ACCOUNTS.includes(profile.email.toLowerCase());
};

// ── THEME ────────────────────────────────
export const saveThemePref = async (userId, theme) => {
  try { await supabase.from("profiles").update({ theme }).eq("id", userId); } catch { /* non-critical */ }
};
