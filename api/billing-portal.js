// api/billing-portal.js
// Opens the Stripe Customer Portal so a user can manage/cancel their subscription.
// SECURITY: now requires a valid Supabase login token. The user is identified from
// that verified token — NOT from a userId in the request body — so no one can open
// someone else's billing portal.
const stripe = require("stripe")(process.env.STRIPE_SECRET_KEY);
const { createClient } = require("@supabase/supabase-js");
const supabase = createClient(
  process.env.SUPABASE_URL,
  process.env.SUPABASE_SERVICE_ROLE_KEY
);

// Verify the caller's Supabase session token; returns the user object or null.
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

  // ── Auth: identify the caller from their token; ignore any userId in the body ──
  const user = await verifyUser(req);
  if (!user) return res.status(401).json({ error: "Sign in required" });

  try {
    const { data: profile } = await supabase
      .from("profiles")
      .select("stripe_customer_id")
      .eq("id", user.id)
      .single();
    if (!profile?.stripe_customer_id) {
      return res.status(400).json({ error: "No billing account found." });
    }
    const session = await stripe.billingPortal.sessions.create({
      customer: profile.stripe_customer_id,
      return_url: origin + "/",
    });
    return res.status(200).json({ url: session.url });
  } catch (err) {
    console.error("Billing portal error:", err);
    return res.status(500).json({ error: "Could not open billing portal." });
  }
};
