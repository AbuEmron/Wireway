-- ============================================================
-- Wireway — Local Market Intelligence (opt-in benchmarking)  ·  Phase 2 · Feature 6
-- Run in Supabase SQL Editor. Run anytime after the core schema.
-- Safe to re-run.
-- ============================================================

-- Opt-in flag + coarse region (US state) on the contractor's profile. Benchmarks
-- are computed SERVER-SIDE only, aggregated, and only returned when a bucket has
-- enough distinct contributors (k-anonymity) — raw rows are never exposed.
-- Both columns are owner-controlled via the existing profiles RLS.
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS market_opt_in boolean NOT NULL DEFAULT false;
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS region        text;   -- e.g. 'CA', 'TX'

-- Helps the server-side aggregator scan opted-in peers by region efficiently.
CREATE INDEX IF NOT EXISTS profiles_market ON profiles (region) WHERE market_opt_in;
