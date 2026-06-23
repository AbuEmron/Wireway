// api/stripe-webhook.js
// Handles ALL Stripe webhook events:
//   - Subscription created/updated/deleted → update Supabase profile (Wireway's own billing)
//   - account.updated (Connect)            → mark an electrician "connected" when ready
//   - checkout.session.completed (Connect) → mark the matching quote paid (by unique quote_id)
//
// Webhook endpoint in Stripe: https://wireway.cc/api/stripe-webhook
// Events to enable:
//   customer.subscription.created / updated / deleted
//   invoice.payment_failed
//   checkout.session.completed
//   account.updated
// IMPORTANT: this endpoint must also LISTEN TO CONNECTED ACCOUNTS so that the
// account.updated and (direct-charge) checkout.session.completed events arrive here.
const stripe = require("stripe")(process.env.STRIPE_SECRET_KEY);
const { createClient } = require("@supabase/supabase-js");
const supabase = createClient(
  process.env.SUPABASE_URL,
  process.env.SUPABASE_SERVICE_ROLE_KEY
);
export const config = { api: { bodyParser: false } };
async function getRawBody(req) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    req.on("data", c => chunks.push(c));
    req.on("end",  () => resolve(Buffer.concat(chunks)));
    req.on("error", reject);
  });
}
module.exports = async function handler(req, res) {
  if (req.method !== "POST") return res.status(405).end();
  let event;
  try {
    const raw = await getRawBody(req);
    event = stripe.webhooks.constructEvent(
      raw,
      req.headers["stripe-signature"],
      process.env.STRIPE_WEBHOOK_SECRET
    );
  } catch (err) {
    return res.status(400).json({ error: `Webhook error: ${err.message}` });
  }
  try {
    switch (event.type) {
      case "customer.subscription.created":
      case "customer.subscription.updated": {
        const sub = event.data.object;
        const userId = sub.metadata?.supabase_user_id;
        if (!userId) break;
        const status = sub.status;
        const plan   = sub.metadata?.plan || "pro";
        await supabase.from("profiles").update({
          subscription_status:    status,
          stripe_subscription_id: sub.id,
          plan: status === "active" || status === "trialing" ? plan : "free",
          trial_ends_at: sub.trial_end ? new Date(sub.trial_end * 1000).toISOString() : null,
        }).eq("id", userId);
        break;
      }
      case "customer.subscription.deleted": {
        const sub = event.data.object;
        const userId = sub.metadata?.supabase_user_id;
        if (!userId) break;
        await supabase.from("profiles").update({
          subscription_status: "canceled",
          plan: "free",
        }).eq("id", userId);
        break;
      }
      case "invoice.payment_failed": {
        const inv = event.data.object;
        const { data: profile } = await supabase
          .from("profiles").select("id")
          .eq("stripe_customer_id", inv.customer).single();
        if (profile) {
          await supabase.from("profiles").update({ subscription_status: "past_due" }).eq("id", profile.id);
        }
        break;
      }
      // ── CONNECT: an electrician's account status changed (e.g. finished onboarding) ──
      case "account.updated": {
        const acct = event.data.object;
        await supabase.from("profiles")
          .update({ stripe_charges_enabled: !!acct.charges_enabled })
          .eq("stripe_account_id", acct.id);
        break;
      }
      // ── CONNECT: a client paid a job invoice — mark THAT quote paid (matched by unique id) ──
      case "checkout.session.completed": {
        const session = event.data.object;
        if (session.mode !== "payment") break;
        const quoteId = session.metadata?.quote_id;
        if (!quoteId) break; // older/unrelated sessions without our id — skip
        const amountPaid = (session.amount_total || 0) / 100;
        const newStatus  = session.metadata?.depositOnly === "true" ? "deposit_paid" : "paid";
        const { error } = await supabase.from("quotes")
          .update({ status: newStatus, paid_at: new Date().toISOString(), payment_amount: amountPaid, stripe_session_id: session.id })
          .eq("id", quoteId);
        if (error) throw error; // let Stripe retry if the DB write failed
        break;
      }
      default:
        break;
    }
  } catch (err) {
    console.error("Webhook handler error:", err);
    // Return non-2xx so Stripe retries instead of silently dropping a real event.
    return res.status(500).json({ error: "handler error" });
  }
  return res.status(200).json({ received: true });
};
