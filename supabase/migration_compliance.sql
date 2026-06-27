-- ============================================================
-- Wireway — Legally Load-Bearing (license/permit renewals)  ·  Phase 2 · Feature 4
-- Run in Supabase SQL Editor. Run anytime after the core schema.
-- Safe to re-run.
-- ============================================================

-- Track license / permit / insurance renewal dates so Wireway can nudge before
-- they lapse. (The 1099, Schedule C and sales-tax nudges are computed from data
-- that already exists — no table needed for those.)
CREATE TABLE IF NOT EXISTS license_renewals (
  id            uuid        DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id       uuid        REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  label         text        NOT NULL,
  kind          text        NOT NULL DEFAULT 'license', -- license | permit | insurance | registration | other
  identifier    text,                                    -- license/permit number
  issuer        text,                                    -- state board, city, carrier
  expires_on    date        NOT NULL,
  reminder_days int         NOT NULL DEFAULT 30,         -- start nudging this many days out
  notes         text,
  created_at    timestamptz DEFAULT now() NOT NULL
);

ALTER TABLE license_renewals ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "license_renewals: owner only" ON license_renewals;
CREATE POLICY "license_renewals: owner only"
  ON license_renewals FOR ALL
  USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS license_renewals_user ON license_renewals (user_id, expires_on);
