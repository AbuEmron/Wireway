-- ============================================================
-- Wireway — Link crew members to logged time (Elite)
-- Run in Supabase SQL Editor AFTER migration_crew.sql + migration_timetracking.sql.
-- Safe to re-run: additive only, every statement idempotent.
-- ============================================================

-- Tie a time entry to a crew member. Nullable + ON DELETE SET NULL so deleting a
-- crew member never deletes the hours already logged (the labor cost is kept for
-- history; the entry just becomes unassigned). worker_name/rate on the row are
-- snapshotted at log time from the crew member, so job costing stays correct even
-- if the crew member is later removed or their rate changes.
ALTER TABLE time_entries
  ADD COLUMN IF NOT EXISTS crew_member_id uuid REFERENCES crew_members(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS time_entries_crew ON time_entries (crew_member_id);
