// src/lib/jobCosting.js — Job Costing (Bid vs Actual) data layer  ·  Feature 1
//
// The thesis: a job is the unit of profit. It's born from a quote (the bid) and
// accumulates real costs auto-matched from the bank feed, mileage and expenses.
// Actuals are computed live in JS from cost rows tagged with job_id — nothing is
// double-stored, so a recategorized/added cost is always reflected instantly.
import { supabase } from "./supabase";
import { irsRate } from "./financeApi";

// ── DERIVED MATH ───────────────────────────────────────────────────────────────
// agg = { bank, mileage, subs, labor, count } rolled up from the job's cost rows.
function decorate(job, agg = { bank: 0, mileage: 0, subs: 0, labor: 0, count: 0 }) {
  const bid       = Number(job.bid_amount) || 0;
  const estCost   = Number(job.est_cost) || 0;
  const collected = Number(job.collected) || 0;
  const spend     = (agg.bank || 0) + (agg.mileage || 0) + (agg.subs || 0) + (agg.labor || 0);
  return {
    ...job,
    actual_bank:     agg.bank || 0,     // supplies / materials from bank feed + manual expenses
    actual_mileage:  agg.mileage || 0,  // miles × IRS rate
    actual_subs:     agg.subs || 0,     // subcontractor payments (Feature 3)
    actual_labor:    agg.labor || 0,    // reserved for time-to-job (Feature 4)
    actual_spend:    spend,
    cost_count:      agg.count || 0,
    est_margin:        bid - estCost,
    est_margin_pct:    bid > 0 ? ((bid - estCost) / bid) * 100 : 0,
    projected_margin:  bid - spend,                 // margin if collected in full
    projected_margin_pct: bid > 0 ? ((bid - spend) / bid) * 100 : 0,
    actual_margin:     collected - spend,           // real profit so far (cash in − cash out)
    cost_variance:     spend - estCost,             // + = over budget, live
  };
}

const mileageCost = (t) =>
  (Number(t.miles) || 0) * irsRate(new Date(t.trip_date).getFullYear());

// ── JOBS + ROLLED-UP ACTUALS ────────────────────────────────────────────────────
// One query per cost source (only assigned rows), aggregated in memory. Cheap and
// avoids N+1. Returns every job decorated with live bid-vs-actual figures.
export async function getJobsCosting(userId) {
  const [jobsRes, expRes, tripRes, txnRes, subRes] = await Promise.all([
    supabase.from("jobs").select("*").eq("user_id", userId)
      .order("scheduled_date", { ascending: false, nullsFirst: false })
      .order("created_at", { ascending: false }),
    supabase.from("expenses").select("id,amount,job_id").eq("user_id", userId).not("job_id", "is", null),
    supabase.from("trips").select("id,miles,trip_date,job_id").eq("user_id", userId).not("job_id", "is", null),
    supabase.from("plaid_transactions").select("id,amount,job_id").eq("user_id", userId).not("job_id", "is", null),
    supabase.from("sub_payments").select("id,amount,job_id").eq("user_id", userId).not("job_id", "is", null),
  ]);

  const jobs = jobsRes.data || [];
  const agg = {};
  for (const j of jobs) agg[j.id] = { bank: 0, mileage: 0, subs: 0, labor: 0, count: 0 };

  for (const e of (expRes.data || []))  { const a = agg[e.job_id]; if (a) { a.bank    += Number(e.amount) || 0; a.count++; } }
  for (const t of (txnRes.data || []))  { const a = agg[t.job_id]; if (a) { a.bank    += Number(t.amount) || 0; a.count++; } }
  for (const t of (tripRes.data || [])) { const a = agg[t.job_id]; if (a) { a.mileage += mileageCost(t);        a.count++; } }
  for (const p of (subRes.data || []))  { const a = agg[p.job_id]; if (a) { a.subs    += Number(p.amount) || 0; a.count++; } }

  return { data: jobs.map((j) => decorate(j, agg[j.id])), error: jobsRes.error };
}

// Full cost rows assigned to a single job (for the detail breakdown / unassign).
export async function getJobCosts(userId, jobId) {
  const [expRes, tripRes, txnRes] = await Promise.all([
    supabase.from("expenses").select("*").eq("user_id", userId).eq("job_id", jobId).order("expense_date", { ascending: false }),
    supabase.from("trips").select("*").eq("user_id", userId).eq("job_id", jobId).order("trip_date", { ascending: false }),
    supabase.from("plaid_transactions").select("*").eq("user_id", userId).eq("job_id", jobId).order("txn_date", { ascending: false }),
  ]);
  return {
    expenses:   expRes.data || [],
    trips:      tripRes.data || [],
    plaidTxns:  txnRes.data || [],
  };
}

// ── UNASSIGNED COSTS (the pool to match into jobs) ───────────────────────────────
export async function getUnassignedCosts(userId) {
  const [expRes, tripRes, txnRes] = await Promise.all([
    supabase.from("expenses").select("*").eq("user_id", userId).is("job_id", null).order("expense_date", { ascending: false }).limit(200),
    supabase.from("trips").select("*").eq("user_id", userId).is("job_id", null).order("trip_date", { ascending: false }).limit(200),
    supabase.from("plaid_transactions").select("*").eq("user_id", userId).is("job_id", null).gt("amount", 0).order("txn_date", { ascending: false }).limit(300),
  ]);
  return {
    expenses:   expRes.data || [],
    trips:      tripRes.data || [],
    plaidTxns:  txnRes.data || [],
  };
}

// ── ASSIGNMENT ───────────────────────────────────────────────────────────────────
// jobId may be null to unassign. Every call is owner-scoped.
export const setExpenseJob = (id, userId, jobId) =>
  supabase.from("expenses").update({ job_id: jobId }).eq("id", id).eq("user_id", userId);

export const setTripJob = (id, userId, jobId) =>
  supabase.from("trips").update({ job_id: jobId }).eq("id", id).eq("user_id", userId);

export const setPlaidTxnJob = (id, userId, jobId) =>
  supabase.from("plaid_transactions").update({ job_id: jobId }).eq("id", id).eq("user_id", userId);

// Normalized cost shape for the matching UI so all three sources render the same.
export function normalizeCost(row, kind) {
  if (kind === "trip") {
    return { id: row.id, kind, date: row.trip_date, label: row.purpose || "Mileage",
             sub: `${Number(row.miles).toFixed(1)} mi`, amount: mileageCost(row) };
  }
  if (kind === "plaid") {
    return { id: row.id, kind, date: row.txn_date, label: row.merchant_name || row.raw_name || "Bank charge",
             sub: row.user_category || row.mapped_category || "", amount: Number(row.amount) || 0 };
  }
  return { id: row.id, kind: "expense", date: row.expense_date, label: row.vendor || "Expense",
           sub: row.category || "", amount: Number(row.amount) || 0 };
}

export function setCostJob(cost, userId, jobId) {
  if (cost.kind === "trip")  return setTripJob(cost.id, userId, jobId);
  if (cost.kind === "plaid") return setPlaidTxnJob(cost.id, userId, jobId);
  return setExpenseJob(cost.id, userId, jobId);
}

// Suggest the most likely job for an unassigned cost: nearest active job by date
// (prefer not-yet-complete jobs), within a 14-day window. Conservative on purpose —
// a wrong auto-match is worse than no match. Returns a job id or null.
export function suggestJobForCost(costDate, jobs) {
  if (!costDate || !jobs?.length) return null;
  const target = new Date(costDate).getTime();
  if (Number.isNaN(target)) return null;
  const DAY = 86400000;
  let best = null, bestScore = Infinity;
  for (const j of jobs) {
    const anchor = j.scheduled_date || j.created_at;
    if (!anchor) continue;
    const days = Math.abs(target - new Date(anchor).getTime()) / DAY;
    if (days > 14) continue;
    const penalty = (j.status === "complete" || j.status === "cancelled") ? 7 : 0;
    const score = days + penalty;
    if (score < bestScore) { bestScore = score; best = j.id; }
  }
  return best;
}

// ── JOB MUTATIONS ────────────────────────────────────────────────────────────────
export async function updateJobCosting(userId, jobId, fields) {
  const { data, error } = await supabase
    .from("jobs").update(fields).eq("id", jobId).eq("user_id", userId).select().single();
  return { data, error };
}

// Create a job from a quote, snapshotting the bid. Pulls the full quote row so the
// material/labor estimate is captured even when only the list row is on hand.
export async function createJobFromQuote(userId, quoteId) {
  const { data: q, error: qErr } = await supabase.from("quotes").select("*").eq("id", quoteId).single();
  if (qErr || !q) return { data: null, error: qErr || new Error("Quote not found") };

  const mat = Number(q.total_material) || 0;
  const lab = Number(q.total_labor) || 0;
  const isPaid = q.status === "paid" || q.status === "completed" || q.invoice_paid === true;

  const payload = {
    user_id:           userId,
    quote_id:          q.id,
    title:             q.job_name || `Quote ${q.quote_number || ""}`.trim(),
    client_name:       q.client_name || null,
    client_phone:      q.client_phone || null,
    client_email:      q.client_email || null,
    scheduled_date:    new Date().toISOString().split("T")[0],
    status:            "scheduled",
    bid_amount:        Number(q.total) || 0,
    est_material_cost: mat,
    est_labor_cost:    lab,
    est_cost:          mat + lab,
    collected:         isPaid ? (Number(q.total) || 0) : 0,
  };

  const { data, error } = await supabase.from("jobs").insert(payload).select().single();
  return { data, error };
}

// Link an existing (manually-created) job to a quote, snapshotting the bid.
export async function linkJobToQuote(userId, jobId, quoteId) {
  const { data: q } = await supabase.from("quotes").select("*").eq("id", quoteId).single();
  if (!q) return { data: null, error: new Error("Quote not found") };
  const mat = Number(q.total_material) || 0;
  const lab = Number(q.total_labor) || 0;
  return updateJobCosting(userId, jobId, {
    quote_id:          q.id,
    bid_amount:        Number(q.total) || 0,
    est_material_cost: mat,
    est_labor_cost:    lab,
    est_cost:          mat + lab,
  });
}
