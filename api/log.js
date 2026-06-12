// api/log.js — error collector. Client-side crashes land in your Supabase
// error_logs table so you see what broke, for whom, before they even text you.

export default async function handler(req, res) {
  if (req.method !== "POST") return res.status(405).end();

  try {
    const { message, stack, url, ua, uid } = req.body || {};
    if (!message) return res.status(204).end();

    await fetch(`${process.env.REACT_APP_SUPABASE_URL}/rest/v1/error_logs`, {
      method: "POST",
      headers: {
        apikey: process.env.REACT_APP_SUPABASE_ANON_KEY,
        Authorization: `Bearer ${process.env.REACT_APP_SUPABASE_ANON_KEY}`,
        "Content-Type": "application/json",
        Prefer: "return=minimal",
      },
      body: JSON.stringify({
        message: String(message).slice(0, 500),
        stack: String(stack || "").slice(0, 2000),
        url: String(url || "").slice(0, 300),
        ua: String(ua || "").slice(0, 300),
        uid: uid ? String(uid).slice(0, 64) : null,
      }),
    });
  } catch { /* logging must never break anything */ }

  return res.status(204).end();
}
