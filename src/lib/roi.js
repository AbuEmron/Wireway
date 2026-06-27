// src/lib/roi.js — "Wireway made/saved you $X" — computed from REAL data  ·  Phase 2 · Feature 2
import { supabase } from "./supabase";
import { irsRate } from "./financeApi";
import { getJobsCosting } from "./jobCosting";

// Conservative blended estimate for a self-employed trade (SE 15.3% + ~10% income).
// Labeled "estimated" everywhere it's shown — never presented as exact.
export const ESTIMATED_TAX_RATE = 0.25;
const BASELINE_COLLECTION_DAYS = 30; // typical contractor wait; we measure against it

const num = (n) => Number(n) || 0;
const yearOf = (d) => new Date(d).getFullYear();

let _cache = null; // session cache so the badge + view share one fetch

export async function getROI(userId, { force = false } = {}) {
  if (_cache && _cache.userId === userId && !force) return _cache.data;

  const [tripsRes, expRes, jobsCost, quotesRes, drawsRes] = await Promise.all([
    supabase.from("trips").select("miles, trip_date").eq("user_id", userId),
    supabase.from("expenses").select("amount, category").eq("user_id", userId),
    getJobsCosting(userId),
    supabase.from("quotes").select("created_at, paid_at").eq("user_id", userId).not("paid_at", "is", null),
    supabase.from("job_draws").select("invoiced_at, created_at, paid_at").eq("user_id", userId).eq("status", "paid"),
  ]);

  // Mileage deduction (IRS rate × miles, by the trip's year)
  const trips = tripsRes.data || [];
  const milesLogged = trips.reduce((s, t) => s + num(t.miles), 0);
  const mileageDeduction = trips.reduce((s, t) => s + num(t.miles) * irsRate(yearOf(t.trip_date)), 0);

  // Deductible expenses (meals at 50%)
  const expenseDeductions = (expRes.data || []).reduce(
    (s, e) => s + num(e.amount) * (e.category === "meals" ? 0.5 : 1), 0);

  // Estimated tax saved by capturing those deductions
  const taxSaved = (mileageDeduction + expenseDeductions) * ESTIMATED_TAX_RATE;

  // Jobs where actual beat the bid estimate → extra profit caught
  const jobs = jobsCost.data || [];
  let extraProfit = 0, profitableJobs = 0;
  for (const j of jobs) {
    if (num(j.bid_amount) <= 0) continue;
    const realized = num(j.collected) > 0 ? num(j.actual_margin) : num(j.projected_margin);
    if (realized > 0) profitableJobs += 1;
    const upside = realized - num(j.est_margin);
    if (upside > 0) extraProfit += upside;
  }

  // Collection speed (invoice/created → paid)
  const spans = [];
  for (const q of (quotesRes.data || [])) {
    const a = q.created_at, b = q.paid_at;
    if (a && b) { const d = (new Date(b) - new Date(a)) / 86400000; if (d >= 0) spans.push(d); }
  }
  for (const dr of (drawsRes.data || [])) {
    const a = dr.invoiced_at || dr.created_at, b = dr.paid_at;
    if (a && b) { const d = (new Date(b) - new Date(a)) / 86400000; if (d >= 0) spans.push(d); }
  }
  const avgCollectionDays = spans.length ? spans.reduce((s, d) => s + d, 0) / spans.length : null;
  const daysSaved = avgCollectionDays != null ? Math.max(0, BASELINE_COLLECTION_DAYS - avgCollectionDays) : 0;

  // Headline: cash-equivalent items only (tax saved already embeds mileage+expenses,
  // so we don't add mileageDeduction again — avoids double counting).
  const total = taxSaved + extraProfit;

  const data = {
    total,
    taxSaved,
    mileageDeduction,
    expenseDeductions,
    milesLogged,
    extraProfit,
    profitableJobs,
    avgCollectionDays,
    daysSaved,
    paidCount: spans.length,
  };
  _cache = { userId, data };
  return data;
}

export const clearROICache = () => { _cache = null; };
