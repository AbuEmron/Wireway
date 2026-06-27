// api/delete-account.js — irreversible self-serve account deletion.
// Verifies the caller's JWT, revokes any linked Plaid items (so no access tokens
// stay active), deletes all owner-scoped data, then deletes the auth user itself.
// Requires SUPABASE_SERVICE_ROLE_KEY (already set server-side) for admin.deleteUser.
const { Configuration, PlaidApi, PlaidEnvironments } = require("plaid");
const { createClient } = require("@supabase/supabase-js");

const ALLOWED = ["https://www.wirewaypro.com", "https://wirewaypro.com", "https://wireway.cc", "https://www.wireway.cc"];

// Owner-scoped tables keyed by user_id. Deleted explicitly for a clean, auditable
// wipe; auth.users CASCADE then catches anything else (profiles, photos, etc.).
const USER_TABLES = [
  "quotes", "clients", "photos", "jobs", "job_draws", "trips", "expenses",
  "subcontractors", "sub_payments", "time_entries", "license_renewals",
  "ar_reminders", "elite_estimates", "ai_usage", "appointments",
];

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

  if (!process.env.SUPABASE_SERVICE_ROLE_KEY) {
    return res.status(500).json({ error: "Server not configured for account deletion." });
  }
  const supabase = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_SERVICE_ROLE_KEY);
  const uid = user.id;

  // 1) Revoke linked Plaid items so no access tokens remain active.
  try {
    const { data: items } = await supabase.from("plaid_items").select("id, access_token").eq("user_id", uid);
    const client = plaidClient();
    if (client && items?.length) {
      for (const item of items) {
        try { await client.itemRemove({ access_token: item.access_token }); }
        catch (e) { console.error("item/remove (delete-account) failed:", e?.response?.data?.error_message || e.message); }
      }
    }
  } catch (e) { console.error("plaid revoke phase error:", e.message); }

  // 2) Delete Plaid rows first (tokens + synced transactions), then the rest.
  try { await supabase.from("plaid_transactions").delete().eq("user_id", uid); } catch { /* continue */ }
  try { await supabase.from("plaid_items").delete().eq("user_id", uid); } catch { /* continue */ }
  for (const table of USER_TABLES) {
    try { await supabase.from(table).delete().eq("user_id", uid); } catch { /* table may not exist — cascade covers it */ }
  }
  try { await supabase.from("referral_events").delete().eq("ref_user_id", uid); } catch { /* continue */ }

  // 3) Delete the auth user. ON DELETE CASCADE removes any remaining owner rows
  //    (profiles, etc.). This is the irreversible step.
  const { error } = await supabase.auth.admin.deleteUser(uid);
  if (error) {
    console.error("admin.deleteUser failed:", error.message);
    return res.status(500).json({ error: "Could not fully delete the account. Contact support@wirewaypro.com." });
  }

  return res.status(200).json({ ok: true });
};
