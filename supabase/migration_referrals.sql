-- ============================================================
-- Wireway — Quote→Lead Flywheel (referral attribution)  ·  Phase 2 · Feature 5
-- Run in Supabase SQL Editor. Run anytime after the core schema.
-- Safe to re-run.
-- ============================================================

-- Logs when someone lands from a contractor's branded public doc (quote / pay
-- page). Rows are written by the PUBLIC /api/referral endpoint via the service
-- role; the referring contractor can read their own attribution (RLS below).
CREATE TABLE IF NOT EXISTS referral_events (
  id          uuid        DEFAULT gen_random_uuid() PRIMARY KEY,
  ref_user_id uuid        REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  kind        text        NOT NULL DEFAULT 'visit',  -- 'visit' | 'signup'
  source      text,                                   -- 'pay' | 'quote' | 'link' | 'app'
  created_at  timestamptz DEFAULT now() NOT NULL
);

ALTER TABLE referral_events ENABLE ROW LEVEL SECURITY;

-- The referrer can read their own attribution. Inserts come from the service-role
-- endpoint (which bypasses RLS) — there is intentionally no anon INSERT policy.
DROP POLICY IF EXISTS "referral_events: owner read" ON referral_events;
CREATE POLICY "referral_events: owner read"
  ON referral_events FOR SELECT
  USING (auth.uid() = ref_user_id);

CREATE INDEX IF NOT EXISTS referral_events_ref ON referral_events (ref_user_id, created_at DESC);
