-- ============================================================
-- Wireway — Jobs table (scheduling / calendar)
-- Run in Supabase SQL Editor. Idempotent (IF NOT EXISTS).
-- This documents the base jobs table the Job Calendar uses.
-- ============================================================

CREATE TABLE IF NOT EXISTS jobs (
  id               uuid        DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id          uuid        REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  quote_id         uuid        REFERENCES quotes(id) ON DELETE SET NULL,
  title            text        NOT NULL,
  client_name      text,
  client_phone     text,
  client_email     text,
  job_address      text,
  notes            text,
  scheduled_date   date,
  scheduled_time   time without time zone,
  duration_hours   numeric,
  status           text        DEFAULT 'scheduled',   -- scheduled | in_progress | complete | cancelled
  total            numeric,
  notified         boolean     DEFAULT false,
  review_requested boolean     DEFAULT false,
  review_url       text,
  qb_exported      boolean     DEFAULT false,
  created_at       timestamptz DEFAULT now() NOT NULL,
  updated_at       timestamptz DEFAULT now() NOT NULL
);

ALTER TABLE jobs ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "jobs: owner all" ON jobs;
CREATE POLICY "jobs: owner all"
  ON jobs FOR ALL
  USING  (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS jobs_user_date ON jobs (user_id, scheduled_date);
