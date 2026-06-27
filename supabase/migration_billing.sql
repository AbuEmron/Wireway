-- ============================================================
-- Wireway — Progress Billing + Retainage  ·  Feature 5
-- Run in Supabase SQL Editor. Run AFTER migration_jobcosting.sql.
-- Safe to re-run.
-- ============================================================

-- A job's billing schedule: deposit → progress draws → final, with a retainage
-- percentage withheld per draw and released at the end. Paid draws sync into
-- jobs.collected (client-side) so Job Costing actual margin stays correct.
CREATE TABLE IF NOT EXISTS job_draws (
  id            uuid        DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id       uuid        REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  job_id        uuid        REFERENCES jobs(id) ON DELETE CASCADE NOT NULL,
  label         text        NOT NULL,
  amount        numeric(12,2) NOT NULL DEFAULT 0,    -- gross billable base for this draw
  retainage_pct numeric(5,2)  NOT NULL DEFAULT 0,    -- % withheld from this draw
  status        text        NOT NULL DEFAULT 'pending', -- pending | invoiced | paid
  due_date      date,
  invoiced_at   timestamptz,
  paid_at       timestamptz,
  sort_order    int         NOT NULL DEFAULT 0,
  created_at    timestamptz DEFAULT now() NOT NULL
);

ALTER TABLE job_draws ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "job_draws: owner only" ON job_draws;
CREATE POLICY "job_draws: owner only"
  ON job_draws FOR ALL
  USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS job_draws_user ON job_draws (user_id);
CREATE INDEX IF NOT EXISTS job_draws_job  ON job_draws (job_id, sort_order);
