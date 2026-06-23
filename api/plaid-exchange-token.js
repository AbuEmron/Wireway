// api/plaid-exchange-token.js — exchanges Plaid public_token for access_token and stores it server-side
// Required env vars: PLAID_CLIENT_ID, PLAID_SECRET, PLAID_ENV,
//                    SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY,
//                    REACT_APP_SUPABASE_URL, REACT_APP_SUPABASE_ANON_KEY

const { Configuration, PlaidApi, PlaidEnvironments } = require("plaid");
const { createClient } = require("@supabase/supabase-js");

const ALLOWED = [
  "https://www.wirewaypro.com",
  "https://wirewaypro.com",
  "https://wireway.cc",
  "https://www.wireway.cc",
];

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
  } catch { return null; }
}

function plaidClient() {
  const env = process.env.PLAID_ENV || "sandbox";
  const config = new Configuration({
    basePath: PlaidEnvironments[env],
    baseOptions: {
      headers: {
        "PLAID-CLIENT-ID": process.env.PLAID_CLIENT_ID,
        "PLAID-SECRET": process.env.PLAID_SECRET,
      },
    },
  });
  return new PlaidApi(config);
}

module.exports = async function handler(req, res) {
  const origin = ALLOWED.includes(req.headers.origin) ? req.headers.origin : ALLOWED[0];
  res.setHeader("Access-Control-Allow-Origin", origin);
  res.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
  if (req.method === "OPTIONS") return res.status(200).end();
  if (req.method !== "POST") return res.status(405).json({ error: "Method not allowed" });

  const user = await verifyUser(req);
  if (!user) return res.status(401).json({ error: "Sign in required" });

  const { public_token, institution_id, institution_name } = req.body || {};
  if (!public_token) return res.status(400).json({ error: "public_token required" });

  if (!process.env.PLAID_CLIENT_ID || !process.env.PLAID_SECRET) {
    return res.status(503).json({ error: "Plaid not configured" });
  }

  try {
    // Exchange public token for access token (server-side only — never sent to browser)
    const client = plaidClient();
    const exchangeRes = await client.itemPublicTokenExchange({ public_token });
    const { access_token, item_id } = exchangeRes.data;

    // Store in Supabase using service role key (bypasses RLS so we can write securely)
    const supabase = createClient(
      process.env.SUPABASE_URL,
      process.env.SUPABASE_SERVICE_ROLE_KEY
    );

    const { data: item, error: dbError } = await supabase
      .from("plaid_items")
      .upsert(
        { user_id: user.id, item_id, access_token, institution_id, institution_name },
        { onConflict: "user_id,item_id" }
      )
      .select("id, item_id, institution_name")
      .single();

    if (dbError) {
      console.error("Supabase upsert error:", dbError);
      return res.status(500).json({ error: "Failed to save bank connection" });
    }

    return res.status(200).json({ success: true, item });
  } catch (err) {
    console.error("Plaid exchange error:", err?.response?.data || err.message);
    return res.status(500).json({ error: "Token exchange failed", detail: err?.response?.data?.error_message || err.message });
  }
};
