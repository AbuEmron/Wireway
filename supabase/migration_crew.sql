-- ============================================================
-- Wireway — Crew Roster (Elite)  ·  Crew + Time Tracking
-- Run in Supabase SQL Editor (Dashboard → SQL Editor → New query).
-- Run AFTER migration_jobcosting.sql + migration_timetracking.sql.
-- Safe to re-run: every statement is idempotent.
-- ============================================================

-- The contractor's crew. hourly_cost_rate is the COST per hour (what you pay this
-- person) — never a client-facing bill rate. Logging a crew member's hours against
-- a job contributes hours × hourly_cost_rate as real labor cost into Job Costing.
CREATE TABLE IF NOT EXISTS crew_members (
  id                uuid          DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id           uuid          REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  name              text          NOT NULL,
  role              text,                                   -- e.g. Journeyman, Apprentice, Foreman
  hourly_cost_rate  numeric(10,2) NOT NULL DEFAULT 0,       -- labor COST per hour (what you pay)
  active            boolean       NOT NULL DEFAULT true,    -- inactive = kept for history, hidden from pickers
  created_at        timestamptz   DEFAULT now() NOT NULL
);

ALTER TABLE crew_members ENABLE ROW LEVEL SECURITY;

-- CREATE POLICY has no IF NOT EXISTS — drop-then-create to stay idempotent.
DROP POLICY IF EXISTS "crew_members: owner only" ON crew_members;
CREATE POLICY "crew_members: owner only"
  ON crew_members FOR ALL
  USING  (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS crew_members_user ON crew_members (user_id, active DESC, name);
