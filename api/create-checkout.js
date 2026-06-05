// api/create-checkout.js
// Vercel Serverless Function — creates a Stripe Checkout Session
// Called by the Wireway frontend when a user clicks "Request Payment"
//
// Environment variables required (set in Vercel dashboard):
//   STRIPE_SECRET_KEY  — your Stripe secret key (sk_live_... or sk_test_...)

const stripe = require("stripe")(process.env.STRIPE_SECRET_KEY);

module.exports = async function handler(req, res) {
  // Only allow POST
  if (req.method !== "POST") {
    return res.status(405).json({ error: "Method not allowed" });
  }

  // CORS — allow requests from your domain
  res.setHeader("Access-Control-Allow-Origin", "https://wireway.cc");
  res.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");

  if (req.method === "OPTIONS") {
    return res.status(200).end();
  }

  try {
    const {
      quoteNumber,    // e.g. "WW-2026-001"
      clientName,     // e.g. "John Smith"
      clientEmail,    // e.g. "john@example.com"
      jobName,        // e.g. "Panel upgrade — 123 Main St"
      total,          // number in dollars, e.g. 2450
      depositOnly,    // boolean — charge deposit (50%) or full amount
      depositPercent, // number 0-100, e.g. 50
      companyName,    // e.g. "Jalil's Electric LLC"
      lineItems,      // array of { label, amount } for the checkout summary
    } = req.body;

    // Validate required fields
    if (!quoteNumber || !total || total <= 0) {
      return res.status(400).json({ error: "Missing required fields: quoteNumber and total" });
    }

    // Calculate the charge amount
    const chargeAmount = depositOnly
      ? Math.round(total * (depositPercent / 100) * 100) // Stripe uses cents
      : Math.round(total * 100);

    const chargeLabel = depositOnly
      ? `${depositPercent}% Deposit — ${quoteNumber}`
      : `Full Payment — ${quoteNumber}`;

    // Build line items for Stripe Checkout
    // Always show as a single line item with the quote breakdown in the description
    const stripeLineItems = [{
      price_data: {
        currency: "usd",
        product_data: {
          name: chargeLabel,
          description: [
            companyName && `From: ${companyName}`,
            jobName && `Job: ${jobName}`,
            lineItems?.length
              ? `Services: ${lineItems.slice(0, 3).map(i => i.label).join(", ")}${lineItems.length > 3 ? ` + ${lineItems.length - 3} more` : ""}`
              : null,
          ].filter(Boolean).join("\n"),
          metadata: { quoteNumber, jobName: jobName || "" },
        },
        unit_amount: chargeAmount,
      },
      quantity: 1,
    }];

    // Create the Checkout Session
    const session = await stripe.checkout.sessions.create({
      mode: "payment",
      payment_method_types: ["card"],
      line_items: stripeLineItems,

      // Pre-fill client email if available
      ...(clientEmail ? { customer_email: clientEmail } : {}),

      // Success and cancel URLs
      success_url: `https://wireway.cc/?payment=success&quote=${quoteNumber}`,
      cancel_url:  `https://wireway.cc/?payment=cancelled&quote=${quoteNumber}`,

      // Metadata to identify this payment in the webhook
      metadata: {
        quoteNumber,
        clientName:  clientName  || "",
        clientEmail: clientEmail || "",
        jobName:     jobName     || "",
        depositOnly: depositOnly ? "true" : "false",
        depositPercent: String(depositPercent || 100),
        total: String(total),
      },

      // Payment intent data — show statement descriptor
      payment_intent_data: {
        description: `${companyName || "Wireway"} — ${quoteNumber}`,
        metadata: {
          quoteNumber,
          jobName: jobName || "",
        },
      },
    });

    // Return the session URL to the frontend
    return res.status(200).json({
      sessionId: session.id,
      url: session.url,
    });

  } catch (err) {
    console.error("Stripe checkout error:", err);
    return res.status(500).json({
      error: err.message || "Failed to create checkout session",
    });
  }
};
