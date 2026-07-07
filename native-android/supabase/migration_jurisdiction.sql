-- ============================================================
-- Wireway — AHJ Jurisdiction (universal, broadly available)
-- Run in Supabase SQL Editor (Dashboard → SQL Editor → New query).
-- Run anytime after the core schema. Safe to re-run: every statement is idempotent.
-- ============================================================

-- The user's selected Authority Having Jurisdiction (AHJ) — the place whose
-- electrical code + inspector actually govern their work. state_code is required;
-- county/city refine it and are optional (state alone is a valid selection). One
-- active jurisdiction per user (enforced by the unique index below). The app's
-- adopted-NEC-edition baseline is deterministic + client-side; this table only
-- stores WHERE the user works so every estimate/job checks against their rules.
CREATE TABLE IF NOT EXISTS user_jurisdictions (
  id          uuid        DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id     uuid        REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  state_code  text        NOT NULL,                       -- 2-letter postal code, e.g. 'TX'
  county      text,                                       -- optional refinement
  city        text,                                       -- optional refinement (city / local AHJ)
  source      text        NOT NULL DEFAULT 'manual',      -- manual | gps_confirmed (always user-confirmed)
  created_at  timestamptz DEFAULT now() NOT NULL
);

ALTER TABLE user_jurisdictions ENABLE ROW LEVEL SECURITY;

-- CREATE POLICY has no IF NOT EXISTS — drop-then-create to stay idempotent.
DROP POLICY IF EXISTS "user_jurisdictions: owner only" ON user_jurisdictions;
CREATE POLICY "user_jurisdictions: owner only"
  ON user_jurisdictions FOR ALL
  USING  (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

-- One active jurisdiction per user. The client reuses the existing row id when the
-- user changes their selection, so this also lets the app upsert by user safely.
CREATE UNIQUE INDEX IF NOT EXISTS user_jurisdictions_one_per_user ON user_jurisdictions (user_id);
