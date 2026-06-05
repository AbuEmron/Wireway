// api/create-subscription.js
// Creates a Stripe Billing checkout session for Wireway Pro subscription
//
// Env vars needed in Vercel:
//   STRIPE_SECRET_KEY           — Wireway's Stripe secret key
//   STRIPE_PRO_PRICE_ID         — Price ID for Wireway Pro ($12/mo) from Stripe dashboard
//   STRIPE_TEAMS_PRICE_ID       — Price ID for Wireway Teams ($29/mo)
//   SUPABASE_URL                — your Supabase project URL
//   SUPABASE_SERVICE_ROLE_KEY   — service role key (bypasses RLS for server-side updates)

const stripe = require("stripe")(process.env.STRIPE_SECRET_KEY);
const { createClient } = require("@supabase/supabase-js");

const supabase = createClient(
  process.env.SUPABASE_URL,
  process.env.SUPABASE_SERVICE_ROLE_KEY
);

module.exports = async function handler(req, res) {
  res.setHeader("Access-Control-Allow-Origin", "https://wireway.cc");
  res.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");

  if (req.method === "OPTIONS") return res.status(200).end();
  if (req.method !== "POST") return res.status(405).json({ error: "Method not allowed" });

  try {
    const { userId, email, plan = "pro" } = req.body;
    if (!userId || !email) return res.status(400).json({ error: "userId and email required" });

    // Get or create Stripe customer
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

    const priceId = plan === "teams"
      ? process.env.STRIPE_TEAMS_PRICE_ID
      : process.env.STRIPE_PRO_PRICE_ID;

    // Create Stripe Billing checkout session
    const session = await stripe.checkout.sessions.create({
      mode: "subscription",
      customer: customerId,
      line_items: [{ price: priceId, quantity: 1 }],
      allow_promotion_codes: true,
      subscription_data: {
        trial_period_days: 30,
        metadata: { supabase_user_id: userId, plan },
      },
      success_url: `https://wireway.cc/?subscription=success&plan=${plan}`,
      cancel_url:  `https://wireway.cc/?subscription=cancelled`,
      metadata: { supabase_user_id: userId, plan },
    });

    return res.status(200).json({ url: session.url });
  } catch (err) {
    console.error("Subscription checkout error:", err);
    return res.status(500).json({ error: err.message });
  }
};
