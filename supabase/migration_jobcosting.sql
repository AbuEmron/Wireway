-- ============================================================
-- Wireway — Job Costing (Bid vs Actual)  ·  Feature 1
-- Run in Supabase SQL Editor (Dashboard → SQL Editor → New query)
-- Run AFTER migration_finance.sql + migration_plaid.sql
-- Safe to re-run: every statement is idempotent.
-- ============================================================

-- ── JOBS ───────────────────────────────────────────────────────────────────────
-- The jobs table was created by hand in an earlier phase (no prior migration file).
-- Recreate defensively so this migration is self-contained on a fresh project, then
-- add the costing columns. ADD COLUMN IF NOT EXISTS makes it a no-op on existing rows.
CREATE TABLE IF NOT EXISTS jobs (
  id              uuid        DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id         uuid        REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  title           text        NOT NULL,
  client_name     text,
  client_phone    text,
  job_address     text,
  scheduled_date  date,
  scheduled_time  time,
  duration_hours  numeric(6,2),
  status          text        NOT NULL DEFAULT 'scheduled',
  notes           text,
  created_at      timestamptz DEFAULT now() NOT NULL
);

-- Costing columns — a snapshot of the bid (the quote) + actuals tracking.
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS quote_id          uuid REFERENCES quotes(id) ON DELETE SET NULL;
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS client_email      text;
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS bid_amount        numeric(12,2) NOT NULL DEFAULT 0;  -- contract value (what you collect)
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS est_material_cost numeric(12,2) NOT NULL DEFAULT 0;  -- snapshot from quote
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS est_labor_cost    numeric(12,2) NOT NULL DEFAULT 0;  -- snapshot from quote
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS est_cost          numeric(12,2) NOT NULL DEFAULT 0;  -- editable expected total cost
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS collected         numeric(12,2) NOT NULL DEFAULT 0;  -- money actually received
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS completed_at      timestamptz;

ALTER TABLE jobs ENABLE ROW LEVEL SECURITY;

-- CREATE POLICY has no IF NOT EXISTS — drop-then-create to stay idempotent.
DROP POLICY IF EXISTS "jobs: owner only" ON jobs;
CREATE POLICY "jobs: owner only"
  ON jobs FOR ALL
  USING  (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS jobs_user_date ON jobs (user_id, scheduled_date DESC);
CREATE INDEX IF NOT EXISTS jobs_quote     ON jobs (quote_id);

-- ── LINK COST SOURCES TO A JOB ─────────────────────────────────────────────────
-- Nullable. ON DELETE SET NULL: deleting a job never deletes the underlying cost
-- (it just becomes unassigned again), so mileage/expense/tax records are never lost.
ALTER TABLE expenses           ADD COLUMN IF NOT EXISTS job_id uuid REFERENCES jobs(id) ON DELETE SET NULL;
ALTER TABLE trips              ADD COLUMN IF NOT EXISTS job_id uuid REFERENCES jobs(id) ON DELETE SET NULL;
ALTER TABLE plaid_transactions ADD COLUMN IF NOT EXISTS job_id uuid REFERENCES jobs(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS expenses_job  ON expenses           (job_id);
CREATE INDEX IF NOT EXISTS trips_job      ON trips             (job_id);
CREATE INDEX IF NOT EXISTS plaid_txn_job ON plaid_transactions (job_id);
