// src/lib/errorMonitor.js
// Lightweight crash reporting → writes to your existing Supabase `error_logs` table.
// Design rules:
//   • It must NEVER throw or block the app (all writes are wrapped + swallowed).
//   • It must NEVER spam the table (deduped per-message + hard capped per session).
// Auto-starts on import — just `import "./lib/errorMonitor";` once in App.jsx.
import { supabase } from "./supabase";

const recent = new Map();      // message -> last-logged time (dedupe)
let sessionCount = 0;          // hard cap per page load
const MAX_PER_SESSION = 40;
const DEDUPE_MS = 10000;       // same message at most once per 10s

async function currentUserId() {
  try {
    const { data } = await supabase.auth.getSession();
    return data?.session?.user?.id ?? null;
  } catch {
    return null;
  }
}

export async function logError(error, context = "") {
  try {
    const message = (error?.message || String(error || "Unknown error")).slice(0, 1000);
    const now = Date.now();

    if (now - (recent.get(message) || 0) < DEDUPE_MS) return; // skip rapid repeats
    if (sessionCount >= MAX_PER_SESSION) return;              // hard cap
    recent.set(message, now);
    sessionCount++;

    const payload = {
      message,
      stack: (error?.stack || "").slice(0, 4000) || null,
      context: String(context || "").slice(0, 500) || null,
      url: (typeof window !== "undefined" ? window.location.href : "") || null,
      user_agent: (typeof navigator !== "undefined" ? navigator.userAgent : "") || null,
      user_id: await currentUserId(),
    };

    await supabase.from("error_logs").insert(payload);
  } catch {
    // logging must never break the app — swallow everything
  }
}

let started = false;
export function initErrorMonitor() {
  if (started || typeof window === "undefined") return;
  started = true;

  window.addEventListener("error", (e) => {
    logError(e.error || new Error(e.message || "Script error"), "window.onerror");
  });

  window.addEventListener("unhandledrejection", (e) => {
    const reason = e.reason instanceof Error ? e.reason : new Error(String(e.reason));
    logError(reason, "unhandledrejection");
  });
}

// auto-start as soon as this file is imported
initErrorMonitor();
