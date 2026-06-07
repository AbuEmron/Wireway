// api/quote/[id].js
// Public quote endpoint — returns quote data for client-facing view
// No auth required — quote ID is the access token (unguessable UUID)
//
// GET /api/quote/[id] — returns quote data
// POST /api/quote/[id] — accepts quote (client signature)

const { createClient } = require("@supabase/supabase-js");

const supabase = createClient(
  process.env.SUPABASE_URL,
  process.env.SUPABASE_SERVICE_ROLE_KEY
);

module.exports = async function handler(req, res) {
  const { id } = req.query;

  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");

  if (req.method === "OPTIONS") return res.status(200).end();

  if (!id) return res.status(400).json({ error: "Quote ID required" });

  // ── GET — return quote + company info for client view ──
  if (req.method === "GET") {
    const { data: quote, error } = await supabase
      .from("quotes")
      .select(`
        id, quote_number, client_name, client_email, client_phone,
        job_name, notes, status, total, total_material, total_labor,
        total_hours, total_markup, total_tax, markup, hourly_rate,
        show_materials, flat_rate_mode, invoice_mode, invoice_due_date,
        tax_enabled, tax_rate, entries, custom_items, sig_name, signed_at,
        paid_at, deposit_only, deposit_percent, payment_amount,
        created_at, user_id
      `)
      .eq("id", id)
      .single();

    if (error || !quote) {
      return res.status(404).json({ error: "Quote not found" });
    }

    // Get company info from the electrician's profile
    const { data: electrician } = await supabase
      .from("profiles")
      .select("company_name, company_phone, company_email, company_address, license_number, logo_url, terms")
      .eq("id", quote.user_id)
      .single();

    return res.status(200).json({
      quote,
      electrician: electrician || {},
    });
  }

  // ── POST — client signs the quote ──
  if (req.method === "POST") {
    const { action, sigName, sigDate } = req.body;

    if (action === "accept") {
      if (!sigName) return res.status(400).json({ error: "Signature required" });

      const { data, error } = await supabase
        .from("quotes")
        .update({
          status:     "accepted",
          sig_name:   sigName,
          sig_date:   sigDate || new Date().toLocaleDateString(),
          signed_at:  new Date().toISOString(),
        })
        .eq("id", id)
        .select()
        .single();

      if (error) return res.status(500).json({ error: error.message });
      return res.status(200).json({ success: true, quote: data });
    }

    return res.status(400).json({ error: "Unknown action" });
  }

  return res.status(405).json({ error: "Method not allowed" });
};
