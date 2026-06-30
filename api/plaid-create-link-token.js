// api/plaid-create-link-token.js — creates a Plaid Link token for the frontend
// Vercel serverless function — CommonJS, runs server-side only.
// Required env vars: PLAID_CLIENT_ID, PLAID_SECRET, PLAID_ENV (sandbox|development|production)

const { Configuration, PlaidApi, PlaidEnvironments, Products, CountryCode } = require("plaid");

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

  if (!process.env.PLAID_CLIENT_ID || !process.env.PLAID_SECRET) {
    return res.status(503).json({ error: "Plaid not configured. Add PLAID_CLIENT_ID and PLAID_SECRET to Vercel env vars." });
  }

  try {
    const client = plaidClient();
    // The Plaid Link Android SDK requires the link_token to carry the calling
    // app's android_package_name; the native app sends it in the request body.
    // (The package must also be allowlisted in the Plaid dashboard.) Web callers
    // omit it, so this is a no-op for the web flow.
    const androidPackageName = req.body && req.body.android_package_name;
    const response = await client.linkTokenCreate({
      user: { client_user_id: user.id },
      client_name: "Wireway",
      products: [Products.Transactions],
      country_codes: [CountryCode.Us],
      language: "en",
      ...(androidPackageName ? { android_package_name: androidPackageName } : {}),
    });
    return res.status(200).json({ link_token: response.data.link_token });
  } catch (err) {
    console.error("Plaid link token error:", err?.response?.data || err.message);
    return res.status(500).json({ error: "Failed to create link token", detail: err?.response?.data?.error_message || err.message });
  }
};
