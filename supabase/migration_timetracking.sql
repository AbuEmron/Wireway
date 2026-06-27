-- ============================================================
-- Wireway — Time-to-Job Labor  ·  Feature 4
-- Run in Supabase SQL Editor. Run AFTER migration_jobcosting.sql.
-- Safe to re-run.
-- ============================================================

-- Simple in/out timer (and manual entry) per job. Completed entries contribute
-- hours × rate as real labor cost into Job Costing actuals (Feature 1).
CREATE TABLE IF NOT EXISTS time_entries (
  id           uuid        DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id      uuid        REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  job_id       uuid        REFERENCES jobs(id) ON DELETE SET NULL,
  worker_name  text,                                  -- optional, for crews
  clock_in     timestamptz,
  clock_out    timestamptz,
  hours        numeric(8,2),                          -- null while running; set on stop / manual
  rate         numeric(10,2) NOT NULL DEFAULT 0,      -- labor COST per hour (what you pay)
  is_running   boolean     NOT NULL DEFAULT false,
  notes        text,
  created_at   timestamptz DEFAULT now() NOT NULL
);

ALTER TABLE time_entries ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "time_entries: owner only" ON time_entries;
CREATE POLICY "time_entries: owner only"
  ON time_entries FOR ALL
  USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS time_entries_user    ON time_entries (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS time_entries_job     ON time_entries (job_id);
CREATE INDEX IF NOT EXISTS time_entries_running ON time_entries (user_id) WHERE is_running;
