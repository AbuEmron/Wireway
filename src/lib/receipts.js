// src/lib/receipts.js — Snap-a-Receipt: image → OCR → expense tagged to today's job
// OCR runs server-side through the existing /api/claude proxy (Anthropic vision),
// so there's no client OCR dependency, the API key stays on the server, and the
// call is auth-gated + rate-limited like every other AI feature.
import { supabase } from "./supabase";
import { addExpense, EXPENSE_CATEGORIES } from "./financeApi";

const CATEGORY_IDS = EXPENSE_CATEGORIES.map((c) => c.id);

// ── Image processing ─────────────────────────────────────────────────────────
// Downscale + re-encode to JPEG before OCR/upload. Phone photos are huge; this
// cuts OCR cost/latency and storage, and normalizes HEIC/PNG to a format the
// vision model accepts. Returns { base64, mediaType, blob, dataUrl }.
export function processReceiptImage(file, maxDim = 1600, quality = 0.85) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file);
    const img = new Image();
    img.onload = () => {
      URL.revokeObjectURL(url);
      try {
        const scale = Math.min(1, maxDim / Math.max(img.width, img.height));
        const w = Math.max(1, Math.round(img.width * scale));
        const h = Math.max(1, Math.round(img.height * scale));
        const canvas = document.createElement("canvas");
        canvas.width = w; canvas.height = h;
        canvas.getContext("2d").drawImage(img, 0, 0, w, h);
        const dataUrl = canvas.toDataURL("image/jpeg", quality);
        const base64 = dataUrl.split(",")[1];
        canvas.toBlob(
          (blob) => resolve({ base64, mediaType: "image/jpeg", blob, dataUrl }),
          "image/jpeg", quality
        );
      } catch (e) { reject(e); }
    };
    img.onerror = () => { URL.revokeObjectURL(url); reject(new Error("Could not read image")); };
    img.src = url;
  });
}

async function authToken() {
  const { data: { session } } = await supabase.auth.getSession();
  return session?.access_token || "";
}

// ── OCR via Anthropic vision (through /api/claude) ───────────────────────────
// Returns { vendor, date, amount, category, summary } with best-effort fields.
export async function ocrReceipt({ base64, mediaType }) {
  const token = await authToken();
  const body = {
    max_tokens: 400,
    system:
      "You are a precise receipt OCR extractor for a contractor's bookkeeping app. " +
      "Return ONLY a single compact JSON object, no prose, no code fences.",
    messages: [{
      role: "user",
      content: [
        { type: "image", source: { type: "base64", media_type: mediaType, data: base64 } },
        { type: "text", text:
          "Extract these fields from the receipt:\n" +
          "- vendor: the store/merchant name\n" +
          "- date: the purchase date as YYYY-MM-DD (null if not visible)\n" +
          "- amount: the grand TOTAL actually paid, as a number (no currency symbol)\n" +
          "- category: classify into exactly one of: " + CATEGORY_IDS.join(", ") + "\n" +
          "- summary: a 3-6 word description of what was bought\n" +
          'Respond as JSON: {"vendor":"","date":"YYYY-MM-DD","amount":0,"category":"","summary":""}. ' +
          "Use null for any field you cannot read. Do not guess the date." },
      ],
    }],
  };

  const res = await fetch("/api/claude", {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.error || "OCR failed");
  }
  const data = await res.json();
  const text = data?.content?.map((b) => b.text || "").join("") || "";
  return parseOcrJson(text);
}

function parseOcrJson(text) {
  let raw = (text || "").trim().replace(/^```(?:json)?/i, "").replace(/```$/, "").trim();
  const start = raw.indexOf("{");
  const end = raw.lastIndexOf("}");
  if (start >= 0 && end > start) raw = raw.slice(start, end + 1);
  let obj = {};
  try { obj = JSON.parse(raw); } catch { /* leave blank — user can fill in */ }

  const category = CATEGORY_IDS.includes(obj.category) ? obj.category : "materials";
  const amount = Number(obj.amount);
  const dateOk = typeof obj.date === "string" && /^\d{4}-\d{2}-\d{2}$/.test(obj.date);
  return {
    vendor:   obj.vendor && obj.vendor !== "null" ? String(obj.vendor) : "",
    date:     dateOk ? obj.date : "",
    amount:   Number.isFinite(amount) && amount > 0 ? Math.round(amount * 100) / 100 : "",
    category,
    summary:  obj.summary && obj.summary !== "null" ? String(obj.summary) : "",
  };
}

// ── Upload the receipt image to the private-by-path receipts bucket ───────────
export async function uploadReceiptImage(userId, blob) {
  const path = `${userId}/${Date.now()}-${Math.random().toString(36).slice(2, 8)}.jpg`;
  const { error } = await supabase.storage
    .from("receipts").upload(path, blob, { contentType: "image/jpeg", upsert: false });
  if (error) return { url: null, error };
  const { data } = supabase.storage.from("receipts").getPublicUrl(path);
  return { url: data.publicUrl, error: null };
}

// ── Today's job (auto-assign target) ─────────────────────────────────────────
// Prefer a job actually happening today; tie-break in_progress > scheduled.
export async function getTodaysJobs(userId) {
  const today = new Date().toISOString().split("T")[0];
  const { data } = await supabase
    .from("jobs").select("id,title,client_name,status,scheduled_date")
    .eq("user_id", userId).eq("scheduled_date", today);
  const rank = { in_progress: 0, scheduled: 1, complete: 2, cancelled: 3 };
  return (data || []).sort((a, b) => (rank[a.status] ?? 9) - (rank[b.status] ?? 9));
}

// Recent jobs (for the manual override dropdown when there's no job today).
export async function getRecentJobs(userId, limit = 25) {
  const { data } = await supabase
    .from("jobs").select("id,title,client_name,status,scheduled_date")
    .eq("user_id", userId)
    .order("scheduled_date", { ascending: false, nullsFirst: false })
    .limit(limit);
  return data || [];
}

// ── Save: create the expense from the (edited) OCR fields + receipt image ─────
export async function saveReceiptExpense(userId, { fields, blob, jobId }) {
  let receiptUrl = null;
  if (blob) {
    const { url } = await uploadReceiptImage(userId, blob);
    receiptUrl = url; // non-fatal if it fails — the expense still saves
  }
  return addExpense(userId, {
    expense_date: fields.date || new Date().toISOString().split("T")[0],
    amount:       Number(fields.amount) || 0,
    category:     fields.category || "materials",
    vendor:       fields.vendor || null,
    description:  fields.summary || null,
    receipt_url:  receiptUrl,
    job_id:       jobId || null,
    source:       "receipt",
  });
}
