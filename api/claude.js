// api/claude.js — server-side proxy for AI features
// Requires a valid Supabase session token: only signed-in Wireway users can call it.

export default async function handler(req, res) {
  if (req.method !== "POST") {
    return res.status(405).json({ error: "Method not allowed" });
  }
  if (!process.env.ANTHROPIC_API_KEY) {
    return res.status(500).json({ error: "ANTHROPIC_API_KEY not configured in Vercel environment variables" });
  }

  // ── Auth gate: verify the caller is a signed-in Supabase user ──
  const token = (req.headers.authorization || "").replace("Bearer ", "");
  if (!token) {
    return res.status(401).json({ error: "Sign in required" });
  }
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
  } catch {
    return res.status(401).json({ error: "Could not verify session" });
  }

  try {
    const { system, messages, max_tokens } = req.body || {};
    const r = await fetch("https://api.anthropic.com/v1/messages", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "x-api-key": process.env.ANTHROPIC_API_KEY,
        "anthropic-version": "2023-06-01",
      },
      body: JSON.stringify({
        model: "claude-sonnet-4-6",
        max_tokens: Math.min(max_tokens || 1500, 4096),
        system: system || "",
        messages: messages || [],
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
