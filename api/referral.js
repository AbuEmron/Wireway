// api/referral.js — PUBLIC referral-attribution logger  ·  Phase 2 · Feature 5
// Records a visit/signup that originated from a contractor's branded public doc.
// No auth: the ref is just a user id used for attribution. Writes via service role.
const { createClient } = require("@supabase/supabase-js");
const supabase = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_SERVICE_ROLE_KEY);

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const KINDS = new Set(["visit", "signup"]);
const SOURCES = new Set(["pay", "quote", "link", "app"]);

module.exports = async function handler(req, res) {
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");
  if (req.method === "OPTIONS") return res.status(200).end();
  if (req.method !== "POST") return res.status(405).json({ error: "Method not allowed" });

  try {
    const { ref, kind = "visit", source = "link" } = req.body || {};
    if (!ref || !UUID.test(ref)) return res.status(200).json({ ok: false }); // ignore junk quietly

    await supabase.from("referral_events").insert({
      ref_user_id: ref,
      kind: KINDS.has(kind) ? kind : "visit",
      source: SOURCES.has(source) ? source : "link",
    });
    return res.status(200).json({ ok: true });
  } catch {
    return res.status(200).json({ ok: false }); // attribution must never break a page
  }
};
