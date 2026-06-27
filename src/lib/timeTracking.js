// src/lib/timeTracking.js — Time-to-Job labor timer  ·  Feature 4
// Completed entries feed hours × rate into Job Costing as real labor cost.
import { supabase } from "./supabase";

const RATE_KEY = "ww_labor_rate_v1";

// Remember the last labor cost rate on the device so the timer is one tap.
export const getDefaultRate = () => {
  try { return Number(window.localStorage.getItem(RATE_KEY)) || 0; } catch { return 0; }
};
export const setDefaultRate = (rate) => {
  try { window.localStorage.setItem(RATE_KEY, String(Number(rate) || 0)); } catch { /* ignore */ }
};

export const hoursBetween = (inIso, outIso) => {
  const a = new Date(inIso).getTime(), b = new Date(outIso).getTime();
  if (Number.isNaN(a) || Number.isNaN(b) || b <= a) return 0;
  return Math.round(((b - a) / 3600000) * 100) / 100;
};

export const entryLaborCost = (e) => (Number(e.hours) || 0) * (Number(e.rate) || 0);

// ── Running timers ───────────────────────────────────────────────────────────
export async function getRunningTimers(userId) {
  const { data } = await supabase
    .from("time_entries").select("*").eq("user_id", userId).eq("is_running", true)
    .order("clock_in", { ascending: false });
  return data || [];
}

export async function startTimer(userId, { job_id, worker_name, rate }) {
  const { data, error } = await supabase
    .from("time_entries").insert({
      user_id: userId,
      job_id: job_id || null,
      worker_name: worker_name || null,
      rate: Number(rate) || 0,
      clock_in: new Date().toISOString(),
      is_running: true,
    }).select().single();
  return { data, error };
}

export async function stopTimer(userId, entry) {
  const out = new Date().toISOString();
  const hours = hoursBetween(entry.clock_in, out);
  const { data, error } = await supabase
    .from("time_entries")
    .update({ clock_out: out, hours, is_running: false })
    .eq("id", entry.id).eq("user_id", userId).select().single();
  return { data, error };
}

// ── Manual entry ─────────────────────────────────────────────────────────────
export async function addManualEntry(userId, { job_id, date, hours, rate, worker_name, notes }) {
  // Anchor a manual entry at noon on the chosen day so it lands on the right date.
  const clockIn = date ? new Date(`${date}T12:00:00`).toISOString() : new Date().toISOString();
  const { data, error } = await supabase
    .from("time_entries").insert({
      user_id: userId,
      job_id: job_id || null,
      worker_name: worker_name || null,
      hours: Number(hours) || 0,
      rate: Number(rate) || 0,
      clock_in: clockIn,
      is_running: false,
      notes: notes || null,
    }).select().single();
  return { data, error };
}

export async function deleteTimeEntry(id, userId) {
  const { error } = await supabase.from("time_entries").delete().eq("id", id).eq("user_id", userId);
  return { error };
}

// Completed entries (newest first), optionally scoped to a job.
export async function getTimeEntries(userId, jobId = null) {
  let q = supabase.from("time_entries").select("*").eq("user_id", userId).eq("is_running", false);
  if (jobId) q = q.eq("job_id", jobId);
  const { data } = await q.order("clock_in", { ascending: false }).limit(200);
  return data || [];
}
