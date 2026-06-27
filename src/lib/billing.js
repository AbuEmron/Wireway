// src/lib/billing.js — Progress billing + retainage  ·  Feature 5
// Paid draws sync into jobs.collected so Job Costing (Feature 1) actual margin
// reflects money actually received.
import { supabase } from "./supabase";

const round2 = (n) => Math.round((Number(n) || 0) * 100) / 100;

// Per-draw math. amount = gross billable base; retainage withheld from it.
export const drawRetainage = (d) => round2((Number(d.amount) || 0) * (Number(d.retainage_pct) || 0) / 100);
export const drawNet       = (d) => round2((Number(d.amount) || 0) - drawRetainage(d)); // billed now

export function scheduleTotals(draws) {
  const billable   = draws.reduce((s, d) => s + drawNet(d), 0);
  const billedToDate = draws.filter((d) => d.status === "invoiced" || d.status === "paid").reduce((s, d) => s + drawNet(d), 0);
  const collected  = draws.filter((d) => d.status === "paid").reduce((s, d) => s + drawNet(d), 0);
  const retainageHeld = draws.reduce((s, d) => s + drawRetainage(d), 0);
  return {
    billable:     round2(billable),
    billedToDate: round2(billedToDate),
    collected:    round2(collected),
    retainageHeld: round2(retainageHeld),
    outstanding:  round2(billedToDate - collected),
  };
}

// ── CRUD ─────────────────────────────────────────────────────────────────────
export async function getDraws(userId, jobId) {
  const { data } = await supabase
    .from("job_draws").select("*").eq("user_id", userId).eq("job_id", jobId)
    .order("sort_order", { ascending: true }).order("created_at", { ascending: true });
  return data || [];
}

export async function upsertDraw(userId, draw) {
  const payload = {
    user_id: userId,
    job_id: draw.job_id,
    label: draw.label,
    amount: Number(draw.amount) || 0,
    retainage_pct: Number(draw.retainage_pct) || 0,
    status: draw.status || "pending",
    due_date: draw.due_date || null,
    sort_order: draw.sort_order ?? 0,
  };
  if (draw.id) {
    const { data, error } = await supabase.from("job_draws").update(payload).eq("id", draw.id).eq("user_id", userId).select().single();
    return { data, error };
  }
  const { data, error } = await supabase.from("job_draws").insert(payload).select().single();
  return { data, error };
}

export async function deleteDraw(id, userId) {
  const { error } = await supabase.from("job_draws").delete().eq("id", id).eq("user_id", userId);
  return { error };
}

export async function setDrawStatus(userId, draw, status) {
  const patch = { status };
  if (status === "invoiced") patch.invoiced_at = new Date().toISOString();
  if (status === "paid")     patch.paid_at = new Date().toISOString();
  const { data, error } = await supabase
    .from("job_draws").update(patch).eq("id", draw.id).eq("user_id", userId).select().single();
  return { data, error };
}

// Write collected (sum of paid draws' net) back to the job so Job Costing is correct.
export async function syncJobCollected(userId, jobId, draws) {
  const { collected } = scheduleTotals(draws);
  await supabase.from("jobs").update({ collected }).eq("id", jobId).eq("user_id", userId);
  return collected;
}

// ── Schedule generator ───────────────────────────────────────────────────────
// Deposit (no retainage) → N equal progress draws (retainage withheld) →
// Final draw (retainage withheld) → Retainage Release (bills back what was held).
export async function generateSchedule(userId, jobId, { bid, depositPct = 30, progressDraws = 2, retainagePct = 10 }) {
  const contract = Number(bid) || 0;
  if (contract <= 0) return { error: new Error("Job has no bid amount to bill against") };

  const deposit = round2(contract * depositPct / 100);
  const remaining = round2(contract - deposit);
  const phases = Math.max(1, progressDraws + 1); // progress draws + final
  const per = round2(remaining / phases);

  const rows = [];
  let order = 0;
  rows.push({ label: "Deposit", amount: deposit, retainage_pct: 0, sort_order: order++ });
  for (let i = 1; i <= progressDraws; i++) {
    rows.push({ label: `Progress draw ${i}`, amount: per, retainage_pct: retainagePct, sort_order: order++ });
  }
  // Final draw absorbs any rounding remainder.
  const finalAmt = round2(remaining - per * progressDraws);
  rows.push({ label: "Final", amount: finalAmt, retainage_pct: retainagePct, sort_order: order++ });

  const heldTotal = round2((per * progressDraws + finalAmt) * retainagePct / 100);
  if (heldTotal > 0) {
    rows.push({ label: "Retainage release", amount: heldTotal, retainage_pct: 0, sort_order: order++ });
  }

  const payload = rows.map((r) => ({ ...r, user_id: userId, job_id: jobId, status: "pending" }));
  const { error } = await supabase.from("job_draws").insert(payload);
  return { error };
}

// ── Draw invoice text (copy) ─────────────────────────────────────────────────
const fmt = (n) => "$" + (Number(n) || 0).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
export function buildDrawInvoiceText({ draw, job, company = {} }) {
  const ret = drawRetainage(draw);
  const lines = [
    `INVOICE — ${draw.label}`,
    `${company.name || "Wireway"}`,
    company.license ? `License: ${company.license}` : null,
    `═══════════════════════════════════════════`,
    `Job:      ${job.title || "—"}`,
    `Client:   ${job.client_name || "—"}`,
    `Date:     ${new Date().toLocaleDateString()}`,
    draw.due_date ? `Due:      ${draw.due_date}` : null,
    ``,
    `Contract value:        ${fmt(job.bid_amount)}`,
    `This draw (gross):     ${fmt(draw.amount)}`,
    ret > 0 ? `Retainage withheld (${draw.retainage_pct}%): -${fmt(ret)}` : null,
    `───────────────────────────────────────────`,
    `AMOUNT DUE THIS DRAW:  ${fmt(drawNet(draw))}`,
    ``,
    `Generated by Wireway · wireway.cc`,
  ].filter((l) => l !== null);
  return lines.join("\n");
}
