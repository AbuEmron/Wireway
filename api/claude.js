// api/claude.js — server-side proxy for AI features
// - Requires a valid Supabase session token (only signed-in Wireway users can call it)
// - Per-user rate limiting (protects against runaway AI cost/abuse), tracked in Supabase
// - Anthropic key stays server-side; output-token cap enforced
//
// Tunable via Vercel env vars (all optional):
//   AI_MODEL            (default "claude-sonnet-4-6")
//   AI_RATE_LIMIT       (default 60)  — max AI calls per user per window
//   AI_RATE_WINDOW_MIN  (default 60)  — the rolling window, in minutes
// Rate limiting requires the ai_usage table (see add-ai-usage-table.sql). If that
// table is missing, the limiter quietly does nothing (it never blocks a real user).

const AI_MODEL        = process.env.AI_MODEL || "claude-sonnet-4-6";
const RATE_LIMIT      = Number(process.env.AI_RATE_LIMIT) || 60;
const RATE_WINDOW_MIN = Number(process.env.AI_RATE_WINDOW_MIN) || 60;
const MAX_TOKENS_CAP  = 8192;

const supaUrl = () => process.env.SUPABASE_URL || process.env.REACT_APP_SUPABASE_URL;
const svcKey  = () => process.env.SUPABASE_SERVICE_ROLE_KEY;

// Rolling-window per-user limiter using the ai_usage table via service-role REST calls.
// Returns { ok:true } or { ok:false, retryMin }. FAILS OPEN on any infra error so a
// database hiccup never blocks a paying user from working.
async function checkRateLimit(userId) {
  const base = supaUrl();
  const key  = svcKey();
  if (!base || !key || !userId) return { ok: true }; // not configured → don't block
  const windowStart = new Date(Date.now() - RATE_WINDOW_MIN * 60 * 1000).toISOString();
  try {
    // Count this user's calls inside the window (count=exact returns the total in a header)
    const countRes = await fetch(
      `${base}/rest/v1/ai_usage?user_id=eq.${userId}&created_at=gte.${windowStart}&select=id&limit=1`,
      { headers: { apikey: key, Authorization: `Bearer ${key}`, Prefer: "count=exact" } }
    );
    if (!countRes.ok) return { ok: true }; // table missing / error → fail open
    let count = 0;
    const cr = countRes.headers.get("content-range"); // e.g. "0-0/25"
    if (cr && cr.includes("/")) count = parseInt(cr.split("/")[1], 10) || 0;

    if (count >= RATE_LIMIT) return { ok: false, retryMin: RATE_WINDOW_MIN };

    // Record this call
    await fetch(`${base}/rest/v1/ai_usage`, {
      method: "POST",
      headers: { apikey: key, Authorization: `Bearer ${key}`, "Content-Type": "application/json", Prefer: "return=minimal" },
      body: JSON.stringify({ user_id: userId }),
    });
    // Keep the table tiny — drop this user's rows older than the window
    await fetch(`${base}/rest/v1/ai_usage?user_id=eq.${userId}&created_at=lt.${windowStart}`, {
      method: "DELETE",
      headers: { apikey: key, Authorization: `Bearer ${key}` },
    });
    return { ok: true };
  } catch {
    return { ok: true }; // fail open
  }
}

export default async function handler(req, res) {
  if (req.method !== "POST") {
    return res.status(405).json({ error: "Method not allowed" });
  }
  if (!process.env.ANTHROPIC_API_KEY) {
    return res.status(500).json({ error: "ANTHROPIC_API_KEY not configured in Vercel environment variables" });
  }

  // ── Auth gate: verify the caller is a signed-in Supabase user, and capture their id ──
  const token = (req.headers.authorization || "").replace("Bearer ", "");
  if (!token) {
    return res.status(401).json({ error: "Sign in required" });
  }
  let userId;
  try {
    const authRes = await fetch(`${process.env.REACT_APP_SUPABASE_URL}/auth/v1/user`, {
      headers: {
        apikey: process.env.REACT_APP_SUPABASE_ANON_KEY,
        Authorization: `Bearer ${token}`,
      },
    });
    if (!authRes.ok) {
      return res.status(401).json({ error: "Session expired — sign in again" });
    }
    const user = await authRes.json();
    userId = user?.id;
    if (!userId) {
      return res.status(401).json({ error: "Session expired — sign in again" });
    }
  } catch {
    return res.status(401).json({ error: "Could not verify session" });
  }

  // ── Per-user rate limit (protects against AI cost abuse) ──
  const rl = await checkRateLimit(userId);
  if (!rl.ok) {
    return res.status(429).json({ error: `You've reached the AI request limit. Try again in about ${rl.retryMin} minutes.` });
  }

  try {
    const { system, messages, max_tokens, web_search } = req.body || {};
    const r = await fetch("https://api.anthropic.com/v1/messages", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "x-api-key": process.env.ANTHROPIC_API_KEY,
        "anthropic-version": "2023-06-01",
      },
      body: JSON.stringify({
        model: AI_MODEL,
        max_tokens: Math.min(max_tokens || 1500, MAX_TOKENS_CAP),
        system: system || "",
        messages: messages || [],
        ...(web_search ? { tools: [{ type: "web_search_20250305", name: "web_search" }] } : {}),
      }),
    });

    const data = await r.json();
    if (!r.ok) {
      return res.status(r.status).json({ error: data?.error?.message || "Anthropic API error" });
    }
    return res.status(200).json(data);
  } catch (err) {
    return res.status(500).json({ error: err.message || "Proxy error" });
  }
}
