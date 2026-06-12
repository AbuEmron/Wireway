// api/create-subscription.js
// Creates a Stripe Billing checkout session for Wireway Pro subscription
//
// Env vars needed in Vercel:
//   STRIPE_SECRET_KEY           — Wireway's Stripe secret key
//   STRIPE_PRO_PRICE_ID         — Price ID for Wireway Pro ($12/mo) from Stripe dashboard
//   STRIPE_TEAMS_PRICE_ID       — Price ID for Wireway Teams ($29/mo)
//   STRIPE_ELITE_PRICE_ID       — Price ID for Wireway Elite ($99/mo industrial)
//   SUPABASE_URL                — your Supabase project URL
//   SUPABASE_SERVICE_ROLE_KEY   — service role key (bypasses RLS for server-side updates)

const stripe = require("stripe")(process.env.STRIPE_SECRET_KEY);
const { createClient } = require("@supabase/supabase-js");

const supabase = createClient(
  process.env.SUPABASE_URL,
  process.env.SUPABASE_SERVICE_ROLE_KEY
);

module.exports = async function handler(req, res) {
  const ALLOWED = ["https://www.wirewaypro.com", "https://wirewaypro.com", "https://wireway.cc", "https://www.wireway.cc"];
  const origin = ALLOWED.includes(req.headers.origin) ? req.headers.origin : ALLOWED[0];
  res.setHeader("Access-Control-Allow-Origin", origin);
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

    const priceId = plan === "elite"
      ? process.env.STRIPE_ELITE_PRICE_ID
      : plan === "teams"
      ? process.env.STRIPE_TEAMS_PRICE_ID
      : process.env.STRIPE_PRO_PRICE_ID;
    if (!priceId) return res.status(400).json({ error: "Price not configured for plan: " + plan });

    // Create Stripe Billing checkout session
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
    return res.status(500).json({ error: err.message });
  }
};
