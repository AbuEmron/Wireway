// src/lib/dataExport.js — "never trapped" raw export (free for everyone)  ·  Phase 2
// Dumps everything the user owns to a single JSON file. No gating — a user can
// always walk away with their data, regardless of plan.
import { supabase } from "./supabase";

const OWNED_TABLES = [
  "quotes", "clients", "jobs", "job_draws", "trips", "expenses",
  "plaid_transactions", "subcontractors", "sub_payments", "time_entries",
  "license_renewals",
];

export async function exportAllData(userId) {
  const out = { exported_at: new Date().toISOString(), user_id: userId, tables: {} };
  for (const table of OWNED_TABLES) {
    try {
      const { data, error } = await supabase.from(table).select("*").eq("user_id", userId);
      out.tables[table] = error ? [] : (data || []);
    } catch { out.tables[table] = []; }
  }
  const blob = new Blob([JSON.stringify(out, null, 2)], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `wireway-data-export-${new Date().toISOString().split("T")[0]}.json`;
  a.click();
  URL.revokeObjectURL(url);
  const count = Object.values(out.tables).reduce((s, rows) => s + rows.length, 0);
  return { count };
}
