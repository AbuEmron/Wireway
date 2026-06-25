// src/lib/financeApi.js — mileage trips + business expenses data layer
import { supabase } from "./supabase";

// IRS standard mileage rates by tax year
// IRS standard mileage rate by tax year (business). Easy to update: add next year here.
export const IRS_RATES = {
  2022: 0.585,
  2023: 0.655,
  2024: 0.67,
  2025: 0.70,
  2026: 0.725, // 72.5¢/mile — IRS business standard mileage rate for 2026
};
export const CURRENT_IRS_RATE = IRS_RATES[2026];

export const irsRate = (year) => IRS_RATES[year] ?? CURRENT_IRS_RATE;

// Default rate used when *billing* tracked mileage to a client on a quote/invoice.
// Defaults to the IRS standard business rate, but this is independent of the
// tax-deduction rate above — the user can override it per line or in settings.
export const DEFAULT_BILLABLE_RATE = CURRENT_IRS_RATE;

export const EXPENSE_CATEGORIES = [
  { id: "materials",      label: "Materials & Parts",   scheduleC: "Line 22",  color: "#7eb8e8" },
  { id: "fuel",           label: "Gas & Fuel",          scheduleC: "Line 9",   color: "#e8b87e" },
  { id: "tools",          label: "Tools & Equipment",   scheduleC: "Line 22",  color: "#a8e87e" },
  { id: "vehicle",        label: "Vehicle (Other)",     scheduleC: "Line 9",   color: "#e8c97a" },
  { id: "insurance",      label: "Insurance",           scheduleC: "Line 15",  color: "#b87ee8" },
  { id: "advertising",    label: "Advertising",         scheduleC: "Line 8",   color: "#7ec8e8" },
  { id: "meals",          label: "Meals (50% ded.)",    scheduleC: "Line 24b", color: "#e87eb8" },
  { id: "phone",          label: "Phone & Internet",    scheduleC: "Line 27a", color: "#c8e87e" },
  { id: "permits",        label: "Licenses & Permits",  scheduleC: "Line 23",  color: "#e8c77e" },
  { id: "subcontractors", label: "Subcontractors",      scheduleC: "Line 11",  color: "#7ee8b8" },
  { id: "office",         label: "Office Supplies",     scheduleC: "Line 18",  color: "#e8e87e" },
  { id: "other",          label: "Other",               scheduleC: "Line 27a", color: "rgba(255,255,255,0.45)" },
];

export const categoryById = (id) =>
  EXPENSE_CATEGORIES.find((c) => c.id === id) ?? EXPENSE_CATEGORIES[EXPENSE_CATEGORIES.length - 1];

// ── TRIPS ────────────────────────────────────────────────────────────────────

export const getTrips = async (userId, year) => {
  const start = `${year}-01-01`;
  const end   = `${year}-12-31`;
  const { data, error } = await supabase
    .from("trips")
    .select("*")
    .eq("user_id", userId)
    .gte("trip_date", start)
    .lte("trip_date", end)
    .order("trip_date", { ascending: false });
  return { data: data || [], error };
};

export const addTrip = async (userId, trip) => {
  const { data, error } = await supabase
    .from("trips")
    .insert({ ...trip, user_id: userId })
    .select()
    .single();
  return { data, error };
};

export const deleteTrip = async (id, userId) => {
  const { error } = await supabase
    .from("trips")
    .delete()
    .eq("id", id)
    .eq("user_id", userId);
  return { error };
};

// ── BILLABLE MILEAGE ─────────────────────────────────────────────────────────
// Trips have no native client/job link (only free-text purpose/locations), so
// "which miles belong to this invoice" is resolved by an explicit picker plus an
// optional billed_at / billed_quote_id marker on the trip. These columns are added
// by supabase/migration_mileage_billing.sql (nullable, additive — no new endpoint).

// Trips not yet billed onto any quote. Falls back gracefully if the billed_at
// column hasn't been migrated yet (returns all trips, newest first).
export const getUnbilledTrips = async (userId) => {
  let res = await supabase
    .from("trips")
    .select("*")
    .eq("user_id", userId)
    .is("billed_at", null)
    .order("trip_date", { ascending: false })
    .limit(250);
  if (res.error) {
    // Column may not exist yet — degrade to "all trips" so the picker still works.
    res = await supabase
      .from("trips")
      .select("*")
      .eq("user_id", userId)
      .order("trip_date", { ascending: false })
      .limit(250);
  }
  return { data: res.data || [], error: res.error };
};

// Mark the given trips as billed onto a quote (links trip → quote).
export const markTripsBilled = async (userId, tripIds, quoteId) => {
  if (!tripIds || tripIds.length === 0) return { error: null };
  const { error } = await supabase
    .from("trips")
    .update({ billed_at: new Date().toISOString(), billed_quote_id: quoteId || null })
    .in("id", tripIds)
    .eq("user_id", userId);
  return { error };
};

// Return the given trips to the unbilled pool (e.g. the mileage line was removed).
export const unmarkTripsBilled = async (userId, tripIds) => {
  if (!tripIds || tripIds.length === 0) return { error: null };
  const { error } = await supabase
    .from("trips")
    .update({ billed_at: null, billed_quote_id: null })
    .in("id", tripIds)
    .eq("user_id", userId);
  return { error };
};

// ── EXPENSES ─────────────────────────────────────────────────────────────────

export const getExpenses = async (userId, year) => {
  const start = `${year}-01-01`;
  const end   = `${year}-12-31`;
  const { data, error } = await supabase
    .from("expenses")
    .select("*")
    .eq("user_id", userId)
    .gte("expense_date", start)
    .lte("expense_date", end)
    .order("expense_date", { ascending: false });
  return { data: data || [], error };
};

export const addExpense = async (userId, expense) => {
  const { data, error } = await supabase
    .from("expenses")
    .insert({ ...expense, user_id: userId })
    .select()
    .single();
  return { data, error };
};

export const deleteExpense = async (id, userId) => {
  const { error } = await supabase
    .from("expenses")
    .delete()
    .eq("id", id)
    .eq("user_id", userId);
  return { error };
};

// ── CSV IMPORT ───────────────────────────────────────────────────────────────
// Expected columns (case-insensitive): date, amount, category, vendor, description
// Returns array of parsed expense objects (not yet saved)

export const parseExpenseCsv = (text) => {
  const lines = text.split(/\r?\n/).filter(Boolean);
  if (lines.length < 2) return [];

  const headers = lines[0].split(",").map((h) => h.trim().toLowerCase().replace(/[^a-z_]/g, ""));
  const idxOf   = (name) => headers.findIndex((h) => h.includes(name));

  const dateIdx   = idxOf("date");
  const amtIdx    = idxOf("amount");
  const catIdx    = idxOf("categor");
  const vendIdx   = idxOf("vendor");
  const descIdx   = idxOf("desc");

  return lines.slice(1).map((line) => {
    const cols = line.split(",").map((c) => c.trim().replace(/^"|"$/g, ""));
    const rawCat = cols[catIdx]?.toLowerCase() ?? "";
    const matchedCat = EXPENSE_CATEGORIES.find(
      (c) => rawCat.includes(c.id) || rawCat.includes(c.label.toLowerCase().split(" ")[0])
    );
    return {
      expense_date: cols[dateIdx] ? new Date(cols[dateIdx]).toISOString().split("T")[0] : new Date().toISOString().split("T")[0],
      amount:       Math.abs(parseFloat(cols[amtIdx] ?? "0") || 0),
      category:     matchedCat?.id ?? "other",
      vendor:       cols[vendIdx] ?? "",
      description:  cols[descIdx] ?? "",
    };
  }).filter((e) => e.amount > 0);
};

// ── PLAID TRANSACTIONS ────────────────────────────────────────────────────────

export const getPlaidTransactions = async (userId, year) => {
  const { data, error } = await supabase
    .from("plaid_transactions")
    .select("*")
    .eq("user_id", userId)
    .gte("txn_date", `${year}-01-01`)
    .lte("txn_date", `${year}-12-31`)
    .gt("amount", 0)
    .order("txn_date", { ascending: false });
  return { data: data || [], error };
};

export const updatePlaidTxnCategory = async (id, category) => {
  const { error } = await supabase
    .from("plaid_transactions")
    .update({ user_category: category })
    .eq("id", id);
  return { error };
};

// ── TAX EXPORT BUILDER ───────────────────────────────────────────────────────

// plaidTxns is optional — auto-imported bank transactions to include in totals.
export const buildScheduleCText = ({ year, trips, expenses, plaidTxns = [] }) => {
  const rate       = irsRate(year);
  const totalMiles = trips.reduce((s, t) => s + Number(t.miles), 0);
  const mileageDed = totalMiles * rate;

  const byCategory = {};
  for (const cat of EXPENSE_CATEGORIES) byCategory[cat.id] = 0;
  for (const e of expenses) {
    byCategory[e.category] = (byCategory[e.category] || 0) + Number(e.amount);
  }

  // Merge auto-imported bank transactions (use user_category override if set)
  const plaidByCategory = {};
  for (const cat of EXPENSE_CATEGORIES) plaidByCategory[cat.id] = 0;
  for (const t of plaidTxns) {
    const cat = t.user_category || t.mapped_category || "other";
    plaidByCategory[cat] = (plaidByCategory[cat] || 0) + Number(t.amount);
    byCategory[cat]      = (byCategory[cat]      || 0) + Number(t.amount);
  }
  const plaidTotal    = plaidTxns.reduce((s, t) => s + Number(t.amount), 0);
  const manualTotal   = expenses.reduce((s, e) => s + Number(e.amount), 0);
  const totalExpenses = manualTotal + plaidTotal;
  const mealsDed      = (byCategory.meals || 0) * 0.5;
  const totalDed      = mileageDed + totalExpenses - (byCategory.meals || 0) + mealsDed;

  const fmt  = (n) => `$${n.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  const pad  = (s, n) => String(s).padEnd(n);
  const rpad = (s, n) => String(s).padStart(n);

  const lines = [
    `WIREWAY — SCHEDULE C EXPENSE SUMMARY`,
    `Tax Year: ${year}${plaidTxns.length > 0 ? ` (includes ${plaidTxns.length} auto-imported bank transactions)` : ""}`,
    `Generated: ${new Date().toLocaleDateString()}`,
    `═══════════════════════════════════════════════════`,
    ``,
    `SECTION A — MILEAGE DEDUCTION (Method: Standard Rate)`,
    `───────────────────────────────────────────────────`,
    `  Business miles logged:    ${rpad(totalMiles.toLocaleString() + " mi", 14)}`,
    `  IRS standard rate:        ${rpad("$" + rate.toFixed(3) + "/mi", 14)}`,
    `  Mileage deduction:        ${rpad(fmt(mileageDed), 14)}   → Schedule C Line 9`,
    `  (Report on Part II, Car & Truck Expenses)`,
    ``,
    `SECTION B — BUSINESS EXPENSES`,
    `───────────────────────────────────────────────────`,
    ...EXPENSE_CATEGORIES
      .filter((cat) => byCategory[cat.id] > 0)
      .map((cat) => {
        const amt = byCategory[cat.id];
        const note = cat.id === "meals" ? ` → enter ${fmt(amt * 0.5)} (50%)` : "";
        return `  ${pad(cat.label, 26)} ${rpad(fmt(amt), 12)}   ${cat.scheduleC}${note}`;
      }),
    `  ─────────────────────────────────────────────────`,
    `  ${pad("Total expenses (before meals adj.)", 26)} ${rpad(fmt(totalExpenses), 12)}`,
    ``,
    `SECTION C — COMBINED DEDUCTION SUMMARY`,
    `───────────────────────────────────────────────────`,
    `  Mileage deduction:        ${rpad(fmt(mileageDed), 14)}`,
    `  Business expenses:        ${rpad(fmt(totalExpenses - (byCategory.meals || 0) + mealsDed), 14)}`,
    `  ─────────────────────────────────────`,
    `  TOTAL DEDUCTIONS:         ${rpad(fmt(totalDed), 14)}`,
    ``,
    `NOTE: This summary is for reference. Review with your`,
    `tax professional and enter amounts on Schedule C of`,
    `Form 1040. Meals are 50% deductible — enter half.`,
    `Keep all receipts as IRS documentation.`,
    ``,
    `Generated by Wireway · wireway.cc`,
  ];

  return lines.join("\n");
};
