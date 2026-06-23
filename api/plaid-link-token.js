// api/plaid-link-token.js — Phase 2 scaffold: Plaid link token endpoint
// PHASE 2 — not active yet. Needs PLAID_CLIENT_ID + PLAID_SECRET env vars.
// See PLAID_SETUP.md for full instructions.
//
// To activate:
//   npm install plaid
//   Set PLAID_CLIENT_ID and PLAID_SECRET in Vercel env vars
//   Uncomment the implementation below

export default async function handler(req, res) {
  if (req.method !== "POST") {
    return res.status(405).json({ error: "Method not allowed" });
  }

  // ── PHASE 2 IMPLEMENTATION (uncomment after adding Plaid to package.json) ──
  //
  // const { PlaidApi, PlaidEnvironments, Configuration, Products, CountryCode } = require("plaid");
  //
  // const client = new PlaidApi(new Configuration({
  //   basePath: PlaidEnvironments[process.env.PLAID_ENV || "sandbox"],
  //   baseOptions: {
  //     headers: {
  //       "PLAID-CLIENT-ID": process.env.PLAID_CLIENT_ID,
  //       "PLAID-SECRET":    process.env.PLAID_SECRET,
  //     },
  //   },
  // }));
  //
  // const { userId } = req.body;
  // if (!userId) return res.status(400).json({ error: "userId required" });
  //
  // try {
  //   const response = await client.linkTokenCreate({
  //     user:         { client_user_id: userId },
  //     client_name:  "Wireway",
  //     products:     [Products.Transactions],
  //     country_codes:[CountryCode.Us],
  //     language:     "en",
  //   });
  //   return res.json({ link_token: response.data.link_token });
  // } catch (err) {
  //   console.error("Plaid link token error:", err.response?.data || err.message);
  //   return res.status(500).json({ error: "Failed to create link token" });
  // }

  return res.status(501).json({
    error: "Plaid integration is Phase 2. See PLAID_SETUP.md to activate.",
  });
}
