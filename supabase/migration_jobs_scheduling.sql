-- ============================================================
-- Wireway — Job scheduling change tracking
-- Adds client-driven cancel/reschedule tracking to jobs.
-- Run in Supabase SQL Editor. Idempotent.
-- ============================================================

ALTER TABLE jobs ADD COLUMN IF NOT EXISTS change_status       text DEFAULT 'none';  -- none | confirmed | reschedule_requested | cancel_requested
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS change_requested_by text;                 -- client | electrician
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS change_requested_at timestamptz;
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS proposed_date       date;
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS proposed_time       time without time zone;
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS original_date       date;
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS original_time       time without time zone;
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS cancel_reason       text;
-- seen_by_owner = false means the electrician has an unacknowledged client change.
-- Defaults to true so existing/electrician-created rows are not flagged.
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS seen_by_owner       boolean DEFAULT true;

CREATE INDEX IF NOT EXISTS jobs_change_attention ON jobs (user_id, seen_by_owner);
