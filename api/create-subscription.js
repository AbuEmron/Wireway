// api/create-subscription.js
// Creates a Stripe Billing checkout session for a Wireway subscription.
// SECURITY: now requires a valid Supabase login token. The user id and email come
// from that verified token — NOT from the request body — so a request can't be
// aimed at someone else's account. The plan still comes from the body, but it only
// selects which fixed server-side Price ID to charge (the browser can't set a price).
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

  // ── Auth: trust the token, not the body ──
  const user = await verifyUser(req);
  if (!user) return res.status(401).json({ error: "Sign in required" });

  try {
    const { plan = "pro" } = req.body || {};
    const userId = user.id;
    const email  = user.email;

    // Get or create the Stripe customer for this user
    const { data: profile } = await supabase
      .from("profiles")
      .select("stripe_customer_id")
      .eq("id", userId)
      .single();
    let customerId = profile?.stripe_customer_id;
    if (!customerId) {
      const customer = await stripe.customers.create({
        email,
        metadata: { supabase_user_id: userId },
      });
      customerId = customer.id;
      await supabase.from("profiles").update({ stripe_customer_id: customerId }).eq("id", userId);
    }

    const priceId = plan === "elite"
      ? process.env.STRIPE_ELITE_PRICE_ID
      : plan === "teams"
      ? process.env.STRIPE_TEAMS_PRICE_ID
      : process.env.STRIPE_PRO_PRICE_ID;
    if (!priceId) return res.status(400).json({ error: "Price not configured for plan: " + plan });

    const session = await stripe.checkout.sessions.create({
      mode: "subscription",
      customer: customerId,
      line_items: [{ price: priceId, quantity: 1 }],
      allow_promotion_codes: true,
      subscription_data: {
        trial_period_days: plan === "elite" ? 14 : 30,
        metadata: { supabase_user_id: userId, plan },
      },
      success_url: `${origin}/?subscription=success&plan=${plan}`,
      cancel_url:  `${origin}/?subscription=cancelled`,
      metadata: { supabase_user_id: userId, plan },
    });
    return res.status(200).json({ url: session.url });
  } catch (err) {
    console.error("Subscription checkout error:", err);
    return res.status(500).json({ error: "Could not start checkout." });
  }
};
