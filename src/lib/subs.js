// src/lib/subs.js — Subcontractors + 1099 tracking data layer  ·  Feature 3
import { supabase } from "./supabase";

// IRS 1099-NEC reporting threshold by tax year. The 2025 reconciliation law raised
// it from the long-standing $600 to $2,000 starting in 2026 (inflation-indexed after).
const THRESHOLDS = { 2025: 600, 2026: 2000 };
export const thresholdForYear = (year) =>
  THRESHOLDS[year] ?? (year >= 2026 ? 2000 : 600);

export const PAY_METHODS = ["check", "cash", "ach", "zelle", "card", "other"];

// ── SUBCONTRACTORS ───────────────────────────────────────────────────────────
export async function getSubcontractors(userId) {
  const { data, error } = await supabase
    .from("subcontractors").select("*").eq("user_id", userId).order("name");
  return { data: data || [], error };
}

export async function upsertSubcontractor(userId, sub) {
  const payload = {
    user_id:       userId,
    name:          sub.name,
    business_name: sub.business_name || null,
    email:         sub.email || null,
    phone:         sub.phone || null,
    address:       sub.address || null,
    tax_id:        sub.tax_id || null,
    tax_id_type:   sub.tax_id_type || "ein",
    w9_received:   !!sub.w9_received,
    notes:         sub.notes || null,
  };
  if (sub.id) {
    const { data, error } = await supabase
      .from("subcontractors").update(payload).eq("id", sub.id).eq("user_id", userId).select().single();
    return { data, error };
  }
  const { data, error } = await supabase
    .from("subcontractors").insert(payload).select().single();
  return { data, error };
}

export async function deleteSubcontractor(id, userId) {
  const { error } = await supabase
    .from("subcontractors").delete().eq("id", id).eq("user_id", userId);
  return { error };
}

// ── PAYMENTS ─────────────────────────────────────────────────────────────────
export async function getSubPayments(userId, year) {
  const { data, error } = await supabase
    .from("sub_payments").select("*")
    .eq("user_id", userId)
    .gte("payment_date", `${year}-01-01`)
    .lte("payment_date", `${year}-12-31`)
    .order("payment_date", { ascending: false });
  return { data: data || [], error };
}

export async function addSubPayment(userId, payment) {
  const { data, error } = await supabase
    .from("sub_payments").insert({
      user_id:          userId,
      subcontractor_id: payment.subcontractor_id,
      job_id:           payment.job_id || null,
      amount:           Number(payment.amount) || 0,
      payment_date:     payment.payment_date,
      method:           payment.method || null,
      memo:             payment.memo || null,
    }).select().single();
  return { data, error };
}

export async function deleteSubPayment(id, userId) {
  const { error } = await supabase
    .from("sub_payments").delete().eq("id", id).eq("user_id", userId);
  return { error };
}

// ── LEDGER (subs + rolled-up YTD totals + 1099 flag) ─────────────────────────
export async function getSubLedger(userId, year) {
  const [{ data: subs }, { data: payments }] = await Promise.all([
    getSubcontractors(userId),
    getSubPayments(userId, year),
  ]);
  const threshold = thresholdForYear(year);
  const byId = {};
  for (const s of subs) byId[s.id] = { ...s, total: 0, count: 0, payments: [] };
  for (const p of payments) {
    const row = byId[p.subcontractor_id];
    if (!row) continue;
    row.total += Number(p.amount) || 0;
    row.count += 1;
    row.payments.push(p);
  }
  const ledger = Object.values(byId).map((s) => ({
    ...s,
    over_threshold: s.total >= threshold,
    needs_w9:       s.total >= threshold && !s.w9_received,
    remaining:      Math.max(0, threshold - s.total),
  })).sort((a, b) => b.total - a.total);

  return {
    ledger,
    threshold,
    totalPaid:    payments.reduce((a, p) => a + (Number(p.amount) || 0), 0),
    needCount:    ledger.filter((s) => s.over_threshold).length,
  };
}

// ── 1099-NEC SUMMARY (copy / download) ───────────────────────────────────────
const fmt = (n) => "$" + (Number(n) || 0).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const maskTaxId = (id) => (id ? `••• •• ${String(id).replace(/\D/g, "").slice(-4)}` : "— not on file —");

export function build1099Text({ sub, year, company = {}, threshold }) {
  const lines = [
    `WIREWAY — FORM 1099-NEC WORKSHEET`,
    `Tax Year: ${year}`,
    `Generated: ${new Date().toLocaleDateString()}`,
    `═══════════════════════════════════════════════════`,
    ``,
    `PAYER (you)`,
    `  Name:     ${company.name || "—"}`,
    `  Address:  ${company.address || "—"}`,
    `  TIN/EIN:  ${company.ein || company.license || "—"}`,
    ``,
    `RECIPIENT (subcontractor)`,
    `  Name:     ${sub.name}${sub.business_name ? ` (${sub.business_name})` : ""}`,
    `  Address:  ${sub.address || "—"}`,
    `  TIN:      ${maskTaxId(sub.tax_id)} (${(sub.tax_id_type || "ein").toUpperCase()})`,
    `  W-9 on file: ${sub.w9_received ? "Yes" : "NO — request before filing"}`,
    ``,
    `───────────────────────────────────────────────────`,
    `  Box 1 — Nonemployee compensation: ${fmt(sub.total)}`,
    `───────────────────────────────────────────────────`,
    ``,
    `Reporting threshold for ${year}: ${fmt(threshold)}`,
    sub.total >= threshold
      ? `This subcontractor is AT OR OVER the threshold — a 1099-NEC is required.`
      : `Below threshold — a 1099-NEC may not be required this year.`,
    ``,
    `PAYMENT DETAIL`,
    ...(sub.payments || []).slice().sort((a, b) => (a.payment_date || "").localeCompare(b.payment_date || ""))
      .map((p) => `  ${p.payment_date}  ${fmt(p.amount).padStart(12)}  ${p.method || ""} ${p.memo ? "· " + p.memo : ""}`),
    ``,
    `NOTE: This worksheet is for your records. File the official 1099-NEC with`,
    `the IRS (and the recipient) via your accountant or an e-file service. Verify`,
    `the recipient's TIN against their W-9 before filing.`,
    ``,
    `Generated by Wireway · wireway.cc`,
  ];
  return lines.join("\n");
}
