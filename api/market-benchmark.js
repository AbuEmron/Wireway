// api/market-benchmark.js — opt-in, aggregated, k-anonymous market benchmarks
//                            Phase 2 · Feature 6
//
// PRIVACY RULES (enforced here, server-side only):
//   • Only OPTED-IN users can request benchmarks (reciprocity).
//   • Aggregates ONLY — never returns raw rows, user ids, or job titles.
//   • A bucket is shown ONLY if it has >= K distinct contributing contractors
//     (k-anonymity). Otherwise it's omitted entirely.
//   • Compares within the requester's own region (coarse: US state).
const { createClient } = require("@supabase/supabase-js");
const supabase = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_SERVICE_ROLE_KEY);

const K_ANON = 5; // minimum distinct contractors per bucket before it's revealed
const ALLOWED = ["https://www.wirewaypro.com", "https://wirewaypro.com", "https://wireway.cc", "https://www.wireway.cc"];

// Server-side mirror of src/lib/insights classify().
const TYPES = [
  { key: "panel",     label: "Panel / service upgrade", kw: ["panel", "service upgrade", "200a", "100a", "main breaker", "meter"] },
  { key: "ev",        label: "EV charger",              kw: ["ev", "charger", "tesla", "level 2", "evse"] },
  { key: "rewire",    label: "Rewire",                  kw: ["rewire", "re-wire", "wiring", "knob and tube"] },
  { key: "lighting",  label: "Lighting",                kw: ["light", "lighting", "fixture", "recessed", "can light"] },
  { key: "generator", label: "Generator",               kw: ["generator", "genset", "standby", "transfer switch"] },
  { key: "outlets",   label: "Outlets / circuits",      kw: ["outlet", "receptacle", "circuit", "gfci", "dedicated"] },
];
const classify = (name = "") => {
  const n = String(name).toLowerCase();
  for (const t of TYPES) if (t.kw.some((k) => n.includes(k))) return t.key;
  return "other";
};
const labelFor = (key) => TYPES.find((t) => t.key === key)?.label || "Other jobs";

async function verifyUser(req) {
  const token = (req.headers.authorization || "").replace("Bearer ", "").trim();
  if (!token) return null;
  try {
    const r = await fetch(`${process.env.REACT_APP_SUPABASE_URL}/auth/v1/user`, {
      headers: { apikey: process.env.REACT_APP_SUPABASE_ANON_KEY, Authorization: `Bearer ${token}` },
    });
    if (!r.ok) return null;
    const u = await r.json();
    return u?.id ? u : null;
  } catch { return null; }
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

  // Requester must be opted in and have a region (reciprocity + scope).
  const { data: me } = await supabase
    .from("profiles").select("market_opt_in, region").eq("id", user.id).single();
  if (!me?.market_opt_in) return res.status(200).json({ opted_in: false });
  if (!me.region) return res.status(200).json({ opted_in: true, region: null, buckets: [] });

  const region = me.region;

  // Opted-in contractors in the same region.
  const { data: peers } = await supabase
    .from("profiles").select("id").eq("market_opt_in", true).eq("region", region);
  const peerIds = (peers || []).map((p) => p.id);
  if (peerIds.length === 0) return res.status(200).json({ opted_in: true, region, buckets: [] });

  // Won jobs (have a bid) for those contractors — aggregate only.
  const { data: jobs } = await supabase
    .from("jobs").select("user_id, title, bid_amount").in("user_id", peerIds).gt("bid_amount", 0);

  const buckets = {}; // type -> { sum, n, users:Set }
  const mine = {};    // type -> { sum, n } (requester only)
  for (const j of (jobs || [])) {
    const t = classify(j.title || "");
    const amt = Number(j.bid_amount) || 0;
    (buckets[t] ||= { sum: 0, n: 0, users: new Set() });
    buckets[t].sum += amt; buckets[t].n += 1; buckets[t].users.add(j.user_id);
    if (j.user_id === user.id) { (mine[t] ||= { sum: 0, n: 0 }); mine[t].sum += amt; mine[t].n += 1; }
  }

  const result = [];
  for (const [type, b] of Object.entries(buckets)) {
    if (b.users.size < K_ANON) continue; // k-anonymity gate — omit thin buckets entirely
    const marketAvg = b.sum / b.n;
    const yourAvg = mine[type] && mine[type].n ? mine[type].sum / mine[type].n : null;
    result.push({
      type, label: labelFor(type),
      marketAvg: Math.round(marketAvg),
      contributors: b.users.size,
      sampleJobs: b.n,
      yourAvg: yourAvg != null ? Math.round(yourAvg) : null,
      deltaPct: yourAvg != null && marketAvg > 0 ? Math.round(((yourAvg - marketAvg) / marketAvg) * 100) : null,
    });
  }
  result.sort((a, b) => b.sampleJobs - a.sampleJobs);

  return res.status(200).json({ opted_in: true, region, k: K_ANON, buckets: result });
};
