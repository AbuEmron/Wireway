// src/lib/dashboard.js — Money Dashboard aggregates + accountant CSV  ·  Feature 7
import { supabase } from "./supabase";
import { irsRate } from "./financeApi";
import { getJobsCosting } from "./jobCosting";
import { getReceivables, agingBuckets } from "./ar";
import { drawNet } from "./billing";

const pad = (n) => String(n).padStart(2, "0");
function monthRange(year, month0) {
  const last = new Date(year, month0 + 1, 0).getDate();
  return { start: `${year}-${pad(month0 + 1)}-01`, end: `${year}-${pad(month0 + 1)}-${pad(last)}` };
}
const inRange = (d, s, e) => d && d >= s && d <= e;
const num = (n) => Number(n) || 0;

const WON = new Set(["accepted", "deposit_paid", "paid", "completed"]);

// ── This-month money snapshot ────────────────────────────────────────────────
export async function getMoneySnapshot(userId, { month, year }) {
  const { start, end } = monthRange(year, month);

  const [quotesRes, expRes, txnRes, tripRes, subRes, timeRes, drawRes, jobsCost, receivables] = await Promise.all([
    supabase.from("quotes").select("total,status,created_at,paid_at").eq("user_id", userId),
    supabase.from("expenses").select("amount,expense_date").eq("user_id", userId).gte("expense_date", start).lte("expense_date", end),
    supabase.from("plaid_transactions").select("amount,txn_date").eq("user_id", userId).gt("amount", 0).gte("txn_date", start).lte("txn_date", end),
    supabase.from("trips").select("miles,trip_date").eq("user_id", userId).gte("trip_date", start).lte("trip_date", end),
    supabase.from("sub_payments").select("amount,payment_date").eq("user_id", userId).gte("payment_date", start).lte("payment_date", end),
    supabase.from("time_entries").select("hours,rate,clock_in").eq("user_id", userId).eq("is_running", false),
    supabase.from("job_draws").select("amount,retainage_pct,paid_at,status").eq("user_id", userId).eq("status", "paid"),
    getJobsCosting(userId),
    getReceivables(userId),
  ]);

  const quotes = quotesRes.data || [];
  let bid = 0, won = 0, collected = 0;
  for (const q of quotes) {
    const created = q.created_at?.split("T")[0];
    if (inRange(created, start, end)) { bid += num(q.total); if (WON.has(q.status)) won += num(q.total); }
    const paid = q.paid_at?.split("T")[0];
    if (inRange(paid, start, end)) collected += num(q.total);
  }
  // Collected also includes progress-draw payments made this month.
  for (const d of (drawRes.data || [])) {
    const paid = d.paid_at?.split("T")[0];
    if (inRange(paid, start, end)) collected += drawNet(d);
  }

  const spendMaterials = (expRes.data || []).reduce((s, e) => s + num(e.amount), 0)
                       + (txnRes.data || []).reduce((s, t) => s + num(t.amount), 0);
  const spendMileage = (tripRes.data || []).reduce((s, t) => s + num(t.miles) * irsRate(year), 0);
  const spendSubs = (subRes.data || []).reduce((s, p) => s + num(p.amount), 0);
  const spendLabor = (timeRes.data || [])
    .filter((t) => inRange(t.clock_in?.split("T")[0], start, end))
    .reduce((s, t) => s + num(t.hours) * num(t.rate), 0);
  const spent = spendMaterials + spendMileage + spendSubs + spendLabor;

  const ar = agingBuckets(receivables);

  // Per-job P&L (made or lost money) — uses live actuals.
  const jobs = (jobsCost.data || []).map((j) => ({
    id: j.id, title: j.title, client_name: j.client_name,
    bid: num(j.bid_amount), spend: num(j.actual_spend), collected: num(j.collected),
    margin: num(j.collected) > 0 ? num(j.actual_margin) : num(j.projected_margin),
    settled: num(j.collected) > 0,
  })).filter((j) => j.bid > 0 || j.spend > 0);
  const winners = jobs.filter((j) => j.margin >= 0).sort((a, b) => b.margin - a.margin);
  const losers  = jobs.filter((j) => j.margin < 0).sort((a, b) => a.margin - b.margin);

  return {
    bid, won, collected,
    owed: ar.total,
    overdue: ar.d1_30 + ar.d31_60 + ar.d61_90 + ar.d90,
    spent,
    realProfit: collected - spent,
    breakdown: { materials: spendMaterials, mileage: spendMileage, subs: spendSubs, labor: spendLabor },
    winners, losers,
  };
}

// ── Accountant CSV (QuickBooks/Xero-friendly, generic) ───────────────────────
// One row per money movement for the year. Expenses are negative, income positive.
// Columns import cleanly into QuickBooks Online / Xero transaction imports.
const csvCell = (v) => {
  const s = String(v ?? "");
  return /[",\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
};

export async function buildAccountantCsv(userId, year) {
  const ys = `${year}-01-01`, ye = `${year}-12-31`;
  const [expRes, txnRes, tripRes, subRes, timeRes, quotesRes, drawRes, jobsRes, subsRes] = await Promise.all([
    supabase.from("expenses").select("*").eq("user_id", userId).gte("expense_date", ys).lte("expense_date", ye),
    supabase.from("plaid_transactions").select("*").eq("user_id", userId).gt("amount", 0).gte("txn_date", ys).lte("txn_date", ye),
    supabase.from("trips").select("*").eq("user_id", userId).gte("trip_date", ys).lte("trip_date", ye),
    supabase.from("sub_payments").select("*").eq("user_id", userId).gte("payment_date", ys).lte("payment_date", ye),
    supabase.from("time_entries").select("*").eq("user_id", userId).eq("is_running", false).gte("clock_in", `${ys}T00:00:00`).lte("clock_in", `${ye}T23:59:59`),
    supabase.from("quotes").select("quote_number,client_name,total,paid_at").eq("user_id", userId).not("paid_at", "is", null).gte("paid_at", `${ys}T00:00:00`).lte("paid_at", `${ye}T23:59:59`),
    supabase.from("job_draws").select("*").eq("user_id", userId).eq("status", "paid").not("paid_at", "is", null).gte("paid_at", `${ys}T00:00:00`).lte("paid_at", `${ye}T23:59:59`),
    supabase.from("jobs").select("id,title").eq("user_id", userId),
    supabase.from("subcontractors").select("id,name").eq("user_id", userId),
  ]);

  const jobName = Object.fromEntries((jobsRes.data || []).map((j) => [j.id, j.title]));
  const subName = Object.fromEntries((subsRes.data || []).map((s) => [s.id, s.name]));
  const rows = [];

  for (const e of (expRes.data || []))
    rows.push([e.expense_date, "Expense", e.vendor || "", e.category || "", jobName[e.job_id] || "", -num(e.amount), e.description || ""]);
  for (const t of (txnRes.data || []))
    rows.push([t.txn_date, "Expense (bank)", t.merchant_name || t.raw_name || "", t.user_category || t.mapped_category || "", jobName[t.job_id] || "", -num(t.amount), ""]);
  for (const t of (tripRes.data || []))
    rows.push([t.trip_date, "Mileage", "", "vehicle", jobName[t.job_id] || "", -(num(t.miles) * irsRate(year)), `${num(t.miles)} mi @ ${irsRate(year)}/mi`]);
  for (const p of (subRes.data || []))
    rows.push([p.payment_date, "Subcontractor", subName[p.subcontractor_id] || "", "subcontractors", jobName[p.job_id] || "", -num(p.amount), p.memo || ""]);
  for (const t of (timeRes.data || []))
    rows.push([(t.clock_in || "").split("T")[0], "Labor", t.worker_name || "", "labor", jobName[t.job_id] || "", -(num(t.hours) * num(t.rate)), `${num(t.hours)} hr @ ${num(t.rate)}/hr`]);
  for (const q of (quotesRes.data || []))
    rows.push([(q.paid_at || "").split("T")[0], "Income (invoice)", q.client_name || "", "income", "", num(q.total), q.quote_number || ""]);
  for (const d of (drawRes.data || []))
    rows.push([(d.paid_at || "").split("T")[0], "Income (draw)", "", "income", jobName[d.job_id] || "", drawNet(d), d.label || ""]);

  rows.sort((a, b) => String(a[0]).localeCompare(String(b[0])));

  const header = ["Date", "Type", "Name", "Category", "Job", "Amount", "Memo"];
  const lines = [header, ...rows].map((r) => r.map(csvCell).join(","));
  return { csv: lines.join("\n"), count: rows.length };
}
