// api/connect-onboarding.js
// Stripe Connect (Standard accounts) onboarding for electricians.
// Requires a valid Supabase login token. Two actions:
//   { action: "link" }   → creates the electrician's connected account if needed,
//                          returns a Stripe-hosted onboarding URL to redirect to.
//   { action: "status" } → checks whether their account can accept charges yet,
//                          and caches that on their profile.
const stripe = require("stripe")(process.env.STRIPE_SECRET_KEY);
const { createClient } = require("@supabase/supabase-js");
const supabase = createClient(
  process.env.SUPABASE_URL,
  process.env.SUPABASE_SERVICE_ROLE_KEY
);

async function verifyUser(req) {
  const token = (req.headers.authorization || "").replace("Bearer ", "").trim();
  if (!token) return null;
  try {
    const r = await fetch(`${process.env.REACT_APP_SUPABASE_URL}/auth/v1/user`, {
      headers: {
        apikey: process.env.REACT_APP_SUPABASE_ANON_KEY,
        Authorization: `Bearer ${token}`,
      },
    });
    if (!r.ok) return null;
    const user = await r.json();
    return user?.id ? user : null;
  } catch {
    return null;
  }
}

module.exports = async function handler(req, res) {
  const ALLOWED = ["https://www.wirewaypro.com", "https://wirewaypro.com", "https://wireway.cc", "https://www.wireway.cc"];
  const origin = ALLOWED.includes(req.headers.origin) ? req.headers.origin : ALLOWED[0];
  res.setHeader("Access-Control-Allow-Origin", origin);
  res.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
  if (req.method === "OPTIONS") return res.status(200).end();
  if (req.method !== "POST") return res.status(405).json({ error: "Method not allowed" });

  const user = await verifyUser(req);
  if (!user) return res.status(401).json({ error: "Sign in required" });

  try {
    const { action = "link" } = req.body || {};

    const { data: profile } = await supabase
      .from("profiles")
      .select("stripe_account_id")
      .eq("id", user.id)
      .single();
    let accountId = profile?.stripe_account_id;

    // ── Check whether their connected account can take payments ──
    if (action === "status") {
      if (!accountId) return res.status(200).json({ connected: false, charges_enabled: false });
      const acct = await stripe.accounts.retrieve(accountId);
      const charges = !!acct.charges_enabled;
      await supabase.from("profiles").update({ stripe_charges_enabled: charges }).eq("id", user.id);
      return res.status(200).json({ connected: true, charges_enabled: charges });
    }

    // ── Start (or resume) onboarding: make the account if needed, return the link ──
    if (!accountId) {
      const account = await stripe.accounts.create({
        type: "standard",
        email: user.email,
        metadata: { supabase_user_id: user.id },
      });
      accountId = account.id;
      await supabase.from("profiles").update({ stripe_account_id: accountId }).eq("id", user.id);
    }

    const link = await stripe.accountLinks.create({
      account: accountId,
      refresh_url: `${origin}/?stripe_connect=refresh`,
      return_url:  `${origin}/?stripe_connect=done`,
      type: "account_onboarding",
    });

    return res.status(200).json({ url: link.url });
  } catch (err) {
    console.error("Connect onboarding error:", err);
    return res.status(500).json({ error: "Could not start Stripe Connect." });
  }
};
