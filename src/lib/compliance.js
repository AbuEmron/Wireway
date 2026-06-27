// src/lib/compliance.js — compliance nudges from existing data  ·  Phase 2 · Feature 4
import { supabase } from "./supabase";
import { getSubLedger } from "./subs";

const num = (n) => Number(n) || 0;
const DAY = 86400000;
export const daysUntil = (d) => Math.ceil((new Date(d).getTime() - Date.now()) / DAY);

// ── 1099 alerts (reuses sub tracking — 2026 $2,000 threshold) ────────────────
export async function get1099Status(userId, year) {
  const { ledger, threshold, needCount } = await getSubLedger(userId, year);
  const over = ledger.filter((s) => s.over_threshold);
  return {
    threshold,
    needCount,
    overThreshold: over.map((s) => ({ name: s.name, total: s.total, needs_w9: s.needs_w9 })),
    missingW9: over.filter((s) => s.needs_w9).length,
  };
}

// ── Sales tax collected (what you must remit) ────────────────────────────────
export async function getSalesTax(userId, year) {
  const { data } = await supabase
    .from("quotes").select("total_tax, tax_enabled, status, paid_at, created_at")
    .eq("user_id", userId);
  const rows = data || [];
  const inYear = (d) => d && new Date(d).getFullYear() === year;
  let collected = 0, invoiced = 0;
  for (const q of rows) {
    if (!q.tax_enabled) continue;
    const t = num(q.total_tax);
    if (inYear(q.paid_at)) collected += t;            // collected = remit obligation
    if (inYear(q.created_at)) invoiced += t;           // billed (some not yet paid)
  }
  return { collected, invoiced };
}

// ── Schedule C readiness checklist ───────────────────────────────────────────
export async function getScheduleCReadiness(userId, year) {
  const start = `${year}-01-01`, end = `${year}-12-31`;
  const [{ data: exp }, { data: trips }] = await Promise.all([
    supabase.from("expenses").select("category, receipt_url, source").eq("user_id", userId).gte("expense_date", start).lte("expense_date", end),
    supabase.from("trips").select("id").eq("user_id", userId).gte("trip_date", start).lte("trip_date", end),
  ]);
  const expenses = exp || [];
  const total = expenses.length;
  const categorized = expenses.filter((e) => e.category && e.category !== "other").length;
  const withReceipt = expenses.filter((e) => e.receipt_url).length;

  const checks = [
    { key: "expenses",   label: "Business expenses logged",       ok: total > 0,                          detail: `${total} this year` },
    { key: "mileage",    label: "Mileage logged",                 ok: (trips || []).length > 0,           detail: `${(trips || []).length} trips` },
    { key: "categorized",label: "Expenses categorized",           ok: total > 0 && categorized / total >= 0.8, detail: total ? `${Math.round((categorized / total) * 100)}% categorized` : "—" },
    { key: "receipts",   label: "Receipts attached",              ok: total > 0 && withReceipt / total >= 0.5, detail: total ? `${withReceipt}/${total} have a photo` : "—" },
  ];
  const passed = checks.filter((c) => c.ok).length;
  return { checks, passed, total: checks.length, score: Math.round((passed / checks.length) * 100) };
}

// ── License / permit renewals ────────────────────────────────────────────────
export async function getRenewals(userId) {
  const { data } = await supabase
    .from("license_renewals").select("*").eq("user_id", userId).order("expires_on", { ascending: true });
  return (data || []).map((r) => ({ ...r, days_left: daysUntil(r.expires_on) }));
}

export async function upsertRenewal(userId, r) {
  const payload = {
    user_id: userId, label: r.label, kind: r.kind || "license",
    identifier: r.identifier || null, issuer: r.issuer || null,
    expires_on: r.expires_on, reminder_days: Number(r.reminder_days) || 30, notes: r.notes || null,
  };
  if (r.id) {
    const { data, error } = await supabase.from("license_renewals").update(payload).eq("id", r.id).eq("user_id", userId).select().single();
    return { data, error };
  }
  const { data, error } = await supabase.from("license_renewals").insert(payload).select().single();
  return { data, error };
}

export async function deleteRenewal(id, userId) {
  const { error } = await supabase.from("license_renewals").delete().eq("id", id).eq("user_id", userId);
  return { error };
}

export const renewalDue = (r) => r.days_left <= (r.reminder_days || 30);

// ── Combined alert feed (the "load-bearing" nudges) ──────────────────────────
export async function getComplianceAlerts(userId, year) {
  const [n1099, renewals, salesTax] = await Promise.all([
    get1099Status(userId, year),
    getRenewals(userId),
    getSalesTax(userId, year),
  ]);
  const alerts = [];
  if (n1099.needCount > 0)
    alerts.push({ level: "high", text: `${n1099.needCount} subcontractor${n1099.needCount !== 1 ? "s" : ""} over the ${year} $${n1099.threshold.toLocaleString()} 1099 threshold${n1099.missingW9 ? ` · ${n1099.missingW9} missing a W-9` : ""}.` });
  for (const r of renewals.filter(renewalDue)) {
    alerts.push({ level: r.days_left < 0 ? "high" : "med", text: r.days_left < 0 ? `${r.label} EXPIRED ${Math.abs(r.days_left)} day(s) ago.` : `${r.label} expires in ${r.days_left} day(s).` });
  }
  if (salesTax.collected > 0)
    alerts.push({ level: "med", text: `$${Math.round(salesTax.collected).toLocaleString()} sales tax collected in ${year} — set aside to remit to your state.` });
  return alerts;
}
