// api/plaid-remove-item.js — disconnect ONE linked bank/card.
// Verifies the caller's JWT, calls Plaid /item/remove to revoke the access token,
// then deletes that plaid_items row and its synced transactions. Owner-scoped.
const { Configuration, PlaidApi, PlaidEnvironments } = require("plaid");
const { createClient } = require("@supabase/supabase-js");

const ALLOWED = ["https://www.wirewaypro.com", "https://wirewaypro.com", "https://wireway.cc", "https://www.wireway.cc"];

async function verifyUser(req) {
  const token = (req.headers.authorization || "").replace("Bearer ", "").trim();
  if (!token) return null;
  try {
    const r = await fetch(`${process.env.REACT_APP_SUPABASE_URL}/auth/v1/user`, {
      headers: { apikey: process.env.REACT_APP_SUPABASE_ANON_KEY, Authorization: `Bearer ${token}` },
    });
    if (!r.ok) return null;
    const user = await r.json();
    return user?.id ? user : null;
  } catch { return null; }
}

function plaidClient() {
  if (!process.env.PLAID_CLIENT_ID || !process.env.PLAID_SECRET) return null;
  const env = process.env.PLAID_ENV || "sandbox";
  return new PlaidApi(new Configuration({
    basePath: PlaidEnvironments[env],
    baseOptions: { headers: { "PLAID-CLIENT-ID": process.env.PLAID_CLIENT_ID, "PLAID-SECRET": process.env.PLAID_SECRET } },
  }));
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

  const { itemId } = req.body || {};
  if (!itemId) return res.status(400).json({ error: "itemId required" });

  const supabase = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_SERVICE_ROLE_KEY);

  // The item must belong to this user.
  const { data: item } = await supabase
    .from("plaid_items").select("id, access_token").eq("id", itemId).eq("user_id", user.id).single();
  if (!item) return res.status(404).json({ error: "Bank connection not found." });

  // Revoke the access token at Plaid (best-effort — proceed with local cleanup
  // even if Plaid is unreachable so the user can always disconnect).
  const client = plaidClient();
  if (client) {
    try { await client.itemRemove({ access_token: item.access_token }); }
    catch (e) { console.error("plaid item/remove failed:", e?.response?.data?.error_message || e.message); }
  }

  // Delete the synced transactions, then the item (which holds the token).
  await supabase.from("plaid_transactions").delete().eq("plaid_item_id", item.id).eq("user_id", user.id);
  const { error } = await supabase.from("plaid_items").delete().eq("id", item.id).eq("user_id", user.id);
  if (error) return res.status(500).json({ error: "Could not remove the connection." });

  return res.status(200).json({ ok: true });
};
