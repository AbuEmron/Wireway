// src/lib/insights.js — learn from the user's own bid-vs-actual history  ·  Phase 2 · Feature 3
// Turns accumulated job costing into a smarter next bid: real cost multipliers,
// win rate, and a suggested price. Everything is derived from THEIR data only.
import { supabase } from "./supabase";
import { getJobsCosting } from "./jobCosting";

const num = (n) => Number(n) || 0;

// Coarse job-type buckets derived from the job/quote name (no separate type field).
const TYPES = [
  { key: "panel",     label: "Panel / service upgrade", kw: ["panel", "service upgrade", "200a", "100a", "main breaker", "meter"] },
  { key: "ev",        label: "EV charger",              kw: ["ev", "charger", "tesla", "level 2", "evse"] },
  { key: "rewire",    label: "Rewire",                  kw: ["rewire", "re-wire", "wiring", "knob and tube"] },
  { key: "lighting",  label: "Lighting",                kw: ["light", "lighting", "fixture", "recessed", "can light"] },
  { key: "generator", label: "Generator",               kw: ["generator", "genset", "standby", "transfer switch"] },
  { key: "outlets",   label: "Outlets / circuits",      kw: ["outlet", "receptacle", "circuit", "gfci", "dedicated"] },
];
export function classify(name = "") {
  const n = name.toLowerCase();
  for (const t of TYPES) if (t.kw.some((k) => n.includes(k))) return t.key;
  return "other";
}
const typeLabel = (key) => TYPES.find((t) => t.key === key)?.label || "Other jobs";

const avg = (arr) => (arr.length ? arr.reduce((s, x) => s + x, 0) / arr.length : null);

function bucketStats(jobs) {
  const costMults = [], matMults = [], wonPrices = [], marginPcts = [];
  for (const j of jobs) {
    if (num(j.est_cost) > 0 && num(j.actual_spend) > 0) costMults.push(num(j.actual_spend) / num(j.est_cost));
    if (num(j.est_material_cost) > 0 && num(j.actual_bank) > 0) matMults.push(num(j.actual_bank) / num(j.est_material_cost));
    if (num(j.bid_amount) > 0) {
      wonPrices.push(num(j.bid_amount));
      const realized = num(j.collected) > 0 ? num(j.actual_margin) : num(j.projected_margin);
      marginPcts.push(realized / num(j.bid_amount));
    }
  }
  return {
    sampleSize: jobs.length,
    costMultiplier: avg(costMults),
    materialMultiplier: avg(matMults),
    wonAvg: avg(wonPrices),
    marginPct: avg(marginPcts),
  };
}

export async function getInsights(userId) {
  const [jobsCost, quotesRes] = await Promise.all([
    getJobsCosting(userId),
    supabase.from("quotes").select("status, job_name, total").eq("user_id", userId),
  ]);
  const jobs = (jobsCost.data || []).filter((j) => num(j.bid_amount) > 0 || num(j.actual_spend) > 0);

  // Per-type breakdown
  const byType = {};
  for (const j of jobs) {
    const k = classify(j.title || "");
    (byType[k] ||= []).push(j);
  }
  const categories = Object.entries(byType)
    .map(([key, list]) => ({ key, label: typeLabel(key), ...bucketStats(list) }))
    .filter((c) => c.sampleSize > 0)
    .sort((a, b) => b.sampleSize - a.sampleSize);

  // Win rate from quotes
  const quotes = quotesRes.data || [];
  const WON = new Set(["accepted", "deposit_paid", "paid", "completed"]);
  const decided = quotes.filter((q) => q.status && q.status !== "draft");
  const won = decided.filter((q) => WON.has(q.status));
  const winRate = decided.length ? won.length / decided.length : null;

  return {
    overall: bucketStats(jobs),
    categories,
    winRate,
    quotesTotal: quotes.length,
    jobsAnalyzed: jobs.length,
  };
}

// Suggestion for a quote in progress. Returns null when there's not enough history.
export function suggestForQuote(insights, { estCost, estMaterial, jobName }) {
  if (!insights) return null;
  const key = classify(jobName || "");
  const cat = insights.categories.find((c) => c.key === key && c.sampleSize >= 2);
  const basis = cat || (insights.overall.sampleSize >= 2 ? { ...insights.overall, label: "your past jobs", key: "overall" } : null);
  if (!basis) return null;

  const out = { basis: basis.label, sampleSize: basis.sampleSize };

  // Material overrun warning
  if (basis.materialMultiplier && estMaterial > 0) {
    out.materialOverrunPct = (basis.materialMultiplier - 1) * 100;
    out.adjustedMaterial = estMaterial * basis.materialMultiplier;
  }
  // Suggested price = expected real cost × (1 / (1 − target margin))
  const realCost = basis.costMultiplier && estCost > 0 ? estCost * basis.costMultiplier : estCost;
  if (basis.marginPct != null && basis.marginPct < 0.95 && realCost > 0) {
    out.suggestedPrice = realCost / (1 - Math.max(0.05, basis.marginPct));
    out.targetMarginPct = basis.marginPct * 100;
  } else if (basis.wonAvg) {
    out.suggestedPrice = basis.wonAvg;
  }
  return out;
}
