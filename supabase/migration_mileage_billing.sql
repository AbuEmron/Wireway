-- ============================================================
-- Wireway — Billable Mileage on Quotes/Invoices
-- Adds an optional link so tracked trips can be marked as billed
-- onto a specific quote. Additive + nullable + idempotent — safe to
-- re-run, breaks nothing existing, and requires NO new serverless
-- endpoint (the app writes these columns via the existing Supabase
-- client under the existing "trips: owner only" RLS policy).
-- Run in Supabase SQL Editor (Dashboard → SQL Editor → New query).
-- ============================================================

-- When a trip's miles have been pulled onto a quote as a billable line,
-- billed_at is set (so it drops out of the "unbilled trips" picker) and
-- billed_quote_id links it back to that quote.
ALTER TABLE trips
  ADD COLUMN IF NOT EXISTS billed_at       timestamptz;

ALTER TABLE trips
  ADD COLUMN IF NOT EXISTS billed_quote_id uuid
  REFERENCES quotes(id) ON DELETE SET NULL;

-- Fast lookup of a user's unbilled trips (billed_at IS NULL).
CREATE INDEX IF NOT EXISTS trips_user_unbilled
  ON trips (user_id, billed_at);
