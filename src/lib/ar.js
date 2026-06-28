// src/lib/ar.js — Accounts Receivable: aging + one-tap reminders  ·  Feature 6
// A/R is derived from existing data: unpaid invoice-quotes and invoiced (unpaid)
// progress-billing draws. Reminders are sent one-tap via the device (sms:/mailto:)
// — no SMS/email provider or server cron required. (See note in ReceivablesView
// about fully-automated scheduled reminders needing Vercel Cron + a provider.)
import { supabase } from "./supabase";
import { drawNet } from "./billing";
import { PUBLIC_ORIGIN } from "./nativeBridge";

const DAY = 86400000;
const todayStr = () => new Date().toISOString().split("T")[0];

export const daysOverdue = (dueDate) => {
  if (!dueDate) return 0;
  const d = new Date(dueDate).getTime();
  if (Number.isNaN(d)) return 0;
  return Math.floor((Date.now() - d) / DAY);
};

const payUrl = (quoteId) =>
  quoteId ? `${PUBLIC_ORIGIN}/quote/${quoteId}` : "";

// ── Gather receivables ───────────────────────────────────────────────────────
export async function getReceivables(userId) {
  const [qRes, jobsRes, drawsRes, remRes] = await Promise.all([
    supabase.from("quotes")
      .select("id, quote_number, client_name, client_email, client_phone, total, status, invoice_mode, invoice_due_date, invoice_paid, created_at")
      .eq("user_id", userId).limit(500),
    supabase.from("jobs").select("id, title, client_name, client_phone, client_email, quote_id").eq("user_id", userId),
    supabase.from("job_draws").select("*").eq("user_id", userId).eq("status", "invoiced"),
    supabase.from("ar_reminders").select("quote_id, draw_id, sent_at").eq("user_id", userId).order("sent_at", { ascending: false }),
  ]);

  // latest reminder per quote / draw
  const lastRemind = {};
  for (const r of (remRes.data || [])) {
    const k = r.quote_id ? `q:${r.quote_id}` : `d:${r.draw_id}`;
    if (!lastRemind[k]) lastRemind[k] = r.sent_at;
  }

  const items = [];

  // Invoice-quotes still owed: sent/accepted, not paid.
  const OWED = new Set(["sent", "accepted", "deposit_paid", "invoiced"]);
  const PAIDLIKE = new Set(["paid", "completed", "declined", "draft"]);
  for (const q of (qRes.data || [])) {
    const isPaid = q.invoice_paid === true || q.status === "paid" || q.status === "completed";
    const looksReceivable = !isPaid && !PAIDLIKE.has(q.status) && (q.invoice_mode === true || OWED.has(q.status));
    if (!looksReceivable) continue;
    const due = q.invoice_due_date || q.created_at?.split("T")[0] || todayStr();
    items.push({
      kind: "quote", id: q.id,
      title: q.quote_number ? `Invoice ${q.quote_number}` : "Invoice",
      client: q.client_name || "—",
      phone: q.client_phone || "", email: q.client_email || "",
      amount: Number(q.total) || 0,
      due_date: due,
      days: daysOverdue(due),
      payUrl: payUrl(q.id),
      lastRemindedAt: lastRemind[`q:${q.id}`] || null,
    });
  }

  // Invoiced (unpaid) progress draws.
  const jobById = {};
  for (const j of (jobsRes.data || [])) jobById[j.id] = j;
  for (const d of (drawsRes.data || [])) {
    const job = jobById[d.job_id] || {};
    const due = d.due_date || d.invoiced_at?.split("T")[0] || todayStr();
    items.push({
      kind: "draw", id: d.id,
      title: `${job.title || "Job"} — ${d.label}`,
      client: job.client_name || "—",
      phone: job.client_phone || "", email: job.client_email || "",
      amount: drawNet(d),
      due_date: due,
      days: daysOverdue(due),
      payUrl: payUrl(job.quote_id),
      lastRemindedAt: lastRemind[`d:${d.id}`] || null,
    });
  }

  items.sort((a, b) => b.days - a.days);
  return items;
}

// ── Aging buckets ────────────────────────────────────────────────────────────
export function agingBuckets(items) {
  const b = { current: 0, d1_30: 0, d31_60: 0, d61_90: 0, d90: 0, total: 0 };
  for (const it of items) {
    b.total += it.amount;
    if (it.days <= 0) b.current += it.amount;
    else if (it.days <= 30) b.d1_30 += it.amount;
    else if (it.days <= 60) b.d31_60 += it.amount;
    else if (it.days <= 90) b.d61_90 += it.amount;
    else b.d90 += it.amount;
  }
  return b;
}

// ── One-tap reminder (device sms:/mailto:) ───────────────────────────────────
const money = (n) => "$" + (Number(n) || 0).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

export function reminderMessage(item, companyName = "") {
  const who = companyName || "your contractor";
  const overdue = item.days > 0 ? ` (now ${item.days} day${item.days !== 1 ? "s" : ""} past due)` : "";
  const pay = item.payUrl ? `\nPay online: ${item.payUrl}` : "";
  return `Hi ${item.client}, a friendly reminder from ${who}: ${money(item.amount)} is due for ${item.title}${overdue}.${pay}\nThank you!`;
}

export function smsHref(item, companyName) {
  const to = (item.phone || "").replace(/\D/g, "");
  return `sms:${to}?body=${encodeURIComponent(reminderMessage(item, companyName))}`;
}
export function emailHref(item, companyName) {
  const subj = `Payment reminder — ${item.title}`;
  return `mailto:${item.email}?subject=${encodeURIComponent(subj)}&body=${encodeURIComponent(reminderMessage(item, companyName))}`;
}

export async function logReminder(userId, item, channel) {
  await supabase.from("ar_reminders").insert({
    user_id: userId,
    quote_id: item.kind === "quote" ? item.id : null,
    draw_id:  item.kind === "draw" ? item.id : null,
    channel,
  });
}
