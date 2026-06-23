// api/plaid-sync.js — syncs transactions for all linked banks via /transactions/sync
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

// ── Category mapping (CommonJS mirror of src/lib/plaidCategorize.js) ──────────
const MERCHANT_RULES = [
  { keywords: ["shell", "exxon", "mobil", "chevron", "bp", "sunoco", "speedway", "marathon", "circle k", "wawa", "pilot", "flying j", "loves travel", "kwik trip", "casey", "valero", "arco"], cat: "fuel" },
  { keywords: ["home depot", "lowes", "lowe's", "menards", "fastenal", "grainger", "platt electric", "rexel", "anixter", "graybar", "wesco", "city electric", "independent electric", "border states", "crescent electric", "watt electric", "elliot electric", "cec industries", "sonepar", "summit electric"], cat: "materials" },
  { keywords: ["harbor freight", "northern tool", "snap-on", "snapon", "klein tools", "dewalt", "milwaukee tool", "ridgid", "fluke", "ideal industries", "tool depot", "mac tools", "matco"], cat: "tools" },
  { keywords: ["autozone", "advance auto", "o'reilly", "oreilly", "napa auto", "car quest", "pep boys", "jiffy lube", "firestone", "goodyear", "mavis", "valvoline", "midas", "monro", "sears auto", "les schwab", "discount tire", "americas tire", "ntb", "mr. tire", "take 5 oil"], cat: "vehicle" },
  { keywords: ["progressive", "geico", "state farm", "allstate", "nationwide", "liberty mutual", "travelers", "farmers insurance", "zurich", "hartford", "markel", "guard insurance", "usaa", "insurance"], cat: "insurance" },
  { keywords: ["google ads", "facebook ads", "meta ads", "yelp for business", "angi", "angies list", "thumbtack", "homeadvisor", "houzz", "nextdoor ads", "mailchimp", "constant contact", "vistaprint", "4imprint"], cat: "advertising" },
  { keywords: ["mcdonald", "subway", "chick-fil-a", "taco bell", "burger king", "wendy's", "dominos", "pizza hut", "starbucks", "dunkin", "panera", "chipotle", "olive garden", "applebee", "denny", "ihop", "cracker barrel", "perkins", "waffle house", "sonic drive", "dairy queen", "popeyes", "kfc", "little caesars"], cat: "meals" },
  { keywords: ["verizon", "at&t", "t-mobile", "sprint", "us cellular", "comcast", "xfinity", "spectrum", "cox communications", "charter communications", "google fi", "visible wireless", "cricket wireless", "metro pcs", "boost mobile"], cat: "phone" },
  { keywords: ["staples", "office depot", "amazon business", "costco business", "sam's club", "uline", "quill"], cat: "office" },
];

const DETAILED_MAP = {
  "TRANSPORTATION_GAS_AND_CONVENIENCE": "fuel", "TRANSPORTATION_FUEL": "fuel", "GAS_AND_CONVENIENCE_GAS": "fuel",
  "TRANSPORTATION_PARKING": "vehicle", "TRANSPORTATION_AUTOMOTIVE": "vehicle", "TRANSPORTATION_AUTO_PARTS": "vehicle",
  "TRANSPORTATION_CAR_RENTAL": "vehicle", "TRANSPORTATION_TAXIS_AND_RIDE_SHARES": "vehicle",
  "FOOD_AND_DRINK_FAST_FOOD": "meals", "FOOD_AND_DRINK_COFFEE": "meals", "FOOD_AND_DRINK_RESTAURANTS": "meals", "FOOD_AND_DRINK_BAR": "meals",
  "HOME_IMPROVEMENT_HARDWARE_STORES": "materials", "HOME_IMPROVEMENT_PLUMBING": "materials",
  "HOME_IMPROVEMENT_SECURITY": "materials", "HOME_IMPROVEMENT_FURNITURE": "materials", "HOME_IMPROVEMENT_CONTRACTORS": "subcontractors",
  "INSURANCE_AUTO_INSURANCE": "insurance", "INSURANCE_BUSINESS_INSURANCE": "insurance",
  "INSURANCE_HEALTH_INSURANCE": "insurance", "INSURANCE_LIFE_INSURANCE": "insurance", "INSURANCE_PROPERTY_AND_CASUALTY": "insurance",
  "TELECOMMUNICATION_SERVICES_WIRELESS": "phone", "TELECOMMUNICATION_SERVICES_CABLE": "phone", "TELECOMMUNICATION_SERVICES_INTERNET": "phone",
  "ADVERTISING_AND_MARKETING_MEDIA": "advertising", "ADVERTISING_AND_MARKETING_PRINT": "advertising",
  "PROFESSIONAL_SERVICES_CONSULTING": "subcontractors", "PROFESSIONAL_SERVICES_STAFFING": "subcontractors",
  "PROFESSIONAL_SERVICES_LEGAL": "office", "PROFESSIONAL_SERVICES_ACCOUNTING": "office",
};

const PRIMARY_MAP = {
  "TRANSPORTATION": "vehicle", "GAS_AND_CONVENIENCE": "fuel", "HOME_IMPROVEMENT": "materials",
  "FOOD_AND_DRINK": "meals", "RESTAURANTS": "meals", "TRAVEL": "vehicle",
  "UTILITIES": "phone", "TELECOMMUNICATION_SERVICES": "phone", "INSURANCE": "insurance",
  "PROFESSIONAL_SERVICES": "subcontractors", "ADVERTISING_AND_MARKETING": "advertising",
};

function mapPlaidCategory({ primary = "", detailed = "", merchant = "" }) {
  const ml = merchant.toLowerCase();
  for (const rule of MERCHANT_RULES) {
    if (rule.keywords.some((kw) => ml.includes(kw))) return rule.cat;
  }
  const dk = detailed.toUpperCase().replace(/[^A-Z_]/g, "_");
  if (DETAILED_MAP[dk]) return DETAILED_MAP[dk];
  const pk = primary.toUpperCase().replace(/[^A-Z_]/g, "_");
  if (PRIMARY_MAP[pk]) return PRIMARY_MAP[pk];
  return "other";
}

// ── Helpers ───────────────────────────────────────────────────────────────────

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

async function syncItem(client, item) {
  const added = [], modified = [], removed = [];
  let cursor = item.cursor || null;
  let hasMore = true;
  while (hasMore) {
    const res = await client.transactionsSync({
      access_token: item.access_token,
      cursor: cursor || undefined,
      count: 500,
    });
    added.push(...(res.data.added || []));
    modified.push(...(res.data.modified || []));
    removed.push(...(res.data.removed || []));
    cursor = res.data.next_cursor;
    hasMore = res.data.has_more;
  }
  return { added, modified, removed, cursor };
}

// ── Handler ───────────────────────────────────────────────────────────────────

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
    return res.status(503).json({ error: "Plaid not configured" });
  }

  const supabase = createClient(
    process.env.SUPABASE_URL,
    process.env.SUPABASE_SERVICE_ROLE_KEY
  );

  const { data: items, error: itemsErr } = await supabase
    .from("plaid_items")
    .select("*")
    .eq("user_id", user.id);

  if (itemsErr) return res.status(500).json({ error: "Failed to load bank connections" });
  if (!items || items.length === 0) return res.status(200).json({ synced: 0, items: [] });

  const client = plaidClient();
  let totalSynced = 0;
  const results = [];

  for (const item of items) {
    try {
      const { added, modified, removed, cursor } = await syncItem(client, item);

      const upsertRows = [...added, ...modified]
        .filter((t) => !t.pending)
        .map((t) => {
          const primary  = t.personal_finance_category?.primary || "";
          const detailed = t.personal_finance_category?.detailed || "";
          const merchant = t.merchant_name || t.name || "";
          return {
            id:                     t.transaction_id,
            user_id:                user.id,
            plaid_item_id:          item.id,
            account_id:             t.account_id,
            txn_date:               t.date,
            amount:                 t.amount,
            merchant_name:          merchant,
            raw_name:               t.name,
            plaid_category_primary: primary,
            plaid_category_detail:  detailed,
            mapped_category:        mapPlaidCategory({ primary, detailed, merchant }),
            pending:                false,
            synced_at:              new Date().toISOString(),
          };
        });

      if (upsertRows.length > 0) {
        const { error: upsertErr } = await supabase
          .from("plaid_transactions")
          .upsert(upsertRows, { onConflict: "id" });
        if (upsertErr) console.error("Upsert error for item", item.id, upsertErr);
      }

      if (removed.length > 0) {
        await supabase
          .from("plaid_transactions")
          .delete()
          .in("id", removed.map((t) => t.transaction_id))
          .eq("user_id", user.id);
      }

      await supabase
        .from("plaid_items")
        .update({ cursor, last_synced_at: new Date().toISOString() })
        .eq("id", item.id);

      totalSynced += upsertRows.length;
      results.push({ item_id: item.item_id, institution_name: item.institution_name, synced: upsertRows.length });
    } catch (err) {
      console.error("Sync error for item", item.item_id, err?.response?.data || err.message);
      results.push({ item_id: item.item_id, institution_name: item.institution_name, error: err?.response?.data?.error_message || err.message });
    }
  }

  return res.status(200).json({ synced: totalSynced, items: results });
};
