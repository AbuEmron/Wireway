// api/pay-draw.js — PUBLIC tap-to-pay for a progress-billing draw  ·  Phase 2 · Feature 1
//
// No login required: the job/draw UUID is the access token (same model as
// /api/quote/[id]). The charge is a DIRECT CHARGE on the contractor's own
// connected Stripe account — Wireway never holds the funds. The amount is the
// draw's NET (gross − retainage), computed server-side from the DB, never trusted
// from the browser. The webhook records the payment back into jobs.collected.
const stripe = require("stripe")(process.env.STRIPE_SECRET_KEY);
const { createClient } = require("@supabase/supabase-js");
const supabase = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_SERVICE_ROLE_KEY);

const ALLOWED = ["https://www.wirewaypro.com", "https://wirewaypro.com", "https://wireway.cc", "https://www.wireway.cc"];
const drawNet = (d) => Math.round(((Number(d.amount) || 0) * (1 - (Number(d.retainage_pct) || 0) / 100)) * 100) / 100;

module.exports = async function handler(req, res) {
  const origin = ALLOWED.includes(req.headers.origin) ? req.headers.origin : ALLOWED[0];
  res.setHeader("Access-Control-Allow-Origin", origin);
  res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");
  if (req.method === "OPTIONS") return res.status(200).end();

  // ── GET: render data for the public pay page (job + company + unpaid draws) ──
  if (req.method === "GET") {
    const jobId = req.query.jobId;
    if (!jobId) return res.status(400).json({ error: "jobId required" });

    const { data: job } = await supabase
      .from("jobs").select("id, title, client_name, user_id").eq("id", jobId).single();
    if (!job) return res.status(404).json({ error: "Not found" });

    const [{ data: profile }, { data: draws }] = await Promise.all([
      supabase.from("profiles").select("company_name, logo_url, stripe_charges_enabled").eq("id", job.user_id).single(),
      supabase.from("job_draws").select("id, label, amount, retainage_pct, status, due_date, sort_order")
        .eq("job_id", jobId).order("sort_order", { ascending: true }),
    ]);

    return res.status(200).json({
      job: { id: job.id, title: job.title, client_name: job.client_name },
      company: { name: profile?.company_name || "", logo_url: profile?.logo_url || "" },
      ref: job.user_id, // referral attribution for the branded "Powered by Wireway" CTA
      can_pay: !!profile?.stripe_charges_enabled,
      draws: (draws || []).map((d) => ({
        id: d.id, label: d.label, status: d.status, due_date: d.due_date, net: drawNet(d),
      })),
    });
  }

  if (req.method !== "POST") return res.status(405).json({ error: "Method not allowed" });

  // ── POST: create a Checkout session for one draw ──
  try {
    const { drawId } = req.body || {};
    if (!drawId) return res.status(400).json({ error: "drawId required" });

    const { data: draw } = await supabase
      .from("job_draws").select("id, job_id, user_id, label, amount, retainage_pct, status").eq("id", drawId).single();
    if (!draw) return res.status(404).json({ error: "Draw not found" });
    if (draw.status === "paid") return res.status(400).json({ error: "This draw is already paid." });

    const [{ data: job }, { data: profile }] = await Promise.all([
      supabase.from("jobs").select("id, title, client_name").eq("id", draw.job_id).single(),
      supabase.from("profiles").select("stripe_account_id, stripe_charges_enabled, company_name").eq("id", draw.user_id).single(),
    ]);
    if (!profile?.stripe_account_id || !profile.stripe_charges_enabled) {
      return res.status(400).json({ error: "This contractor isn't set up to accept online payments yet." });
    }

    const cents = Math.round(drawNet(draw) * 100);
    if (cents <= 0) return res.status(400).json({ error: "Nothing due on this draw." });

    const session = await stripe.checkout.sessions.create({
      mode: "payment",
      payment_method_types: ["card"],
      line_items: [{
        price_data: {
          currency: "usd",
          product_data: {
            name: `${draw.label} — ${job?.title || "Job"}`,
            ...(profile.company_name ? { description: `From: ${profile.company_name}` } : {}),
          },
          unit_amount: cents,
        },
        quantity: 1,
      }],
      success_url: `${origin}/pay/${draw.job_id}?paid=1`,
      cancel_url:  `${origin}/pay/${draw.job_id}`,
      metadata: { draw_id: draw.id, job_id: draw.job_id },
      payment_intent_data: {
        description: `${profile.company_name || "Wireway"} — ${draw.label}`,
        metadata: { draw_id: draw.id, job_id: draw.job_id },
      },
    }, {
      stripeAccount: profile.stripe_account_id, // DIRECT CHARGE on the contractor's account
    });

    return res.status(200).json({ url: session.url });
  } catch (err) {
    console.error("pay-draw error:", err);
    return res.status(500).json({ error: "Could not start payment." });
  }
};
