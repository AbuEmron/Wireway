// src/lib/market.js — client side of Local Market Intelligence  ·  Phase 2 · Feature 6
import { supabase, updateProfile } from "./supabase";

export const US_STATES = ["AL","AK","AZ","AR","CA","CO","CT","DE","FL","GA","HI","ID","IL","IN","IA","KS","KY","LA","ME","MD","MA","MI","MN","MS","MO","MT","NE","NV","NH","NJ","NM","NY","NC","ND","OH","OK","OR","PA","RI","SC","SD","TN","TX","UT","VT","VA","WA","WV","WI","WY","DC"];

export async function setMarketOptIn(userId, { opt_in, region }) {
  return updateProfile(userId, { market_opt_in: !!opt_in, region: region || null });
}

export async function getBenchmark() {
  const { data: { session } } = await supabase.auth.getSession();
  const token = session?.access_token;
  const res = await fetch("/api/market-benchmark", {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
    body: JSON.stringify({}),
  });
  if (!res.ok) return { opted_in: false, error: true };
  return res.json();
}
