// api/create-checkout.js
// Creates a Stripe Checkout Session for a CLIENT JOB PAYMENT.
//
// How money flows now (Stripe Connect):
//   • Requires the electrician's Supabase login token.
//   • The charge is created ON THE ELECTRICIAN'S OWN connected Stripe account
//     (the `stripeAccount` option = a direct charge), so the client's money goes
//     straight to the electrician. Wireway takes no fee and never holds the funds.
//   • The amount comes from the SAVED QUOTE in the database (looked up by id for
//     this user) — not from the browser.
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
    const { quoteId, depositOnly = false, depositPercent = 100 } = req.body || {};
    if (!quoteId) return res.status(400).json({ error: "Save the quote before requesting payment." });

    // The electrician's connected account (must be connected + able to take charges)
    const { data: profile } = await supabase
      .from("profiles")
      .select("stripe_account_id, stripe_charges_enabled, company_name")
      .eq("id", user.id)
      .single();
    if (!profile?.stripe_account_id || !profile.stripe_charges_enabled) {
      return res.status(400).json({ error: "Connect your Stripe account in Company Settings first." });
    }

    // The quote provides the server-known amount; it must belong to this user
    const { data: quote } = await supabase
      .from("quotes")
      .select("id, quote_number, client_email, job_name, total")
      .eq("id", quoteId)
      .eq("user_id", user.id)
      .single();
    if (!quote) return res.status(404).json({ error: "Quote not found." });

    const total = Number(quote.total) || 0;
    if (total <= 0) return res.status(400).json({ error: "Quote total must be greater than zero." });

    const pct = depositOnly ? Math.min(Math.max(Number(depositPercent) || 100, 1), 100) : 100;
    const chargeAmount = Math.round(total * (pct / 100) * 100); // cents
    const chargeLabel = (depositOnly && pct < 100)
      ? `${pct}% Deposit — ${quote.quote_number || "Quote"}`
      : `Payment — ${quote.quote_number || "Quote"}`;

    const description = [
      profile.company_name && `From: ${profile.company_name}`,
      quote.job_name && `Job: ${quote.job_name}`,
    ].filter(Boolean).join("\n") || undefined;

    const session = await stripe.checkout.sessions.create({
      mode: "payment",
      // Card + pay-by-bank (ACH Direct Debit). Still a direct charge on the
      // contractor's connected account (stripeAccount option below). ACH is async:
      // the session completes as "processing", so make sure the webhook also
      // handles checkout.session.async_payment_succeeded/failed before treating
      // an ACH payment as collected.
      payment_method_types: ["card", "us_bank_account"],
      line_items: [{
        price_data: {
          currency: "usd",
          product_data: { name: chargeLabel, ...(description ? { description } : {}) },
          unit_amount: chargeAmount,
        },
        quantity: 1,
      }],
      ...(quote.client_email ? { customer_email: quote.client_email } : {}),
      success_url: `${origin}/?payment=success&quote=${encodeURIComponent(quote.quote_number || "")}`,
      cancel_url:  `${origin}/?payment=cancelled`,
      metadata: {
        quote_id: quote.id,                       // webhook matches on this (unique)
        quote_number: quote.quote_number || "",
        depositOnly: depositOnly ? "true" : "false",
        deposit_percent: String(pct),
      },
      payment_intent_data: {
        description: `${profile.company_name || "Wireway"} — ${quote.quote_number || ""}`,
        metadata: { quote_id: quote.id },
      },
    }, {
      stripeAccount: profile.stripe_account_id,   // DIRECT CHARGE on the electrician's account
    });

    return res.status(200).json({ url: session.url, sessionId: session.id });
  } catch (err) {
    console.error("Stripe checkout error:", err);
    return res.status(500).json({ error: "Could not create checkout session." });
  }
};
