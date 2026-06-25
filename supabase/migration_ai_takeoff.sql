-- ============================================================
-- Wireway AI Takeoff — required tables
-- Run this in Supabase SQL Editor (Dashboard → SQL Editor → New query)
--
-- Covers the two tables the Elite "AI Takeoff" feature touches:
--   • elite_estimates — saving / loading industrial takeoffs (EliteMode.jsx)
--   • ai_usage        — per-user AI rate limiting (api/claude.js)
-- (Your core tables — profiles, quotes, clients, jobs, etc. — must already
--  exist in your project; this file only adds what was missing for AI Takeoff.)
-- ============================================================

-- ── ELITE ESTIMATES (saved industrial takeoffs) ──────────────────────────────
CREATE TABLE IF NOT EXISTS elite_estimates (
  id          uuid        DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id     uuid        REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  job_name    text        NOT NULL DEFAULT 'Untitled estimate',
  co_mode     boolean     NOT NULL DEFAULT false,   -- change-order mode
  parent_ref  text,                                 -- parent contract reference
  payload     jsonb       NOT NULL DEFAULT '{}'::jsonb,  -- specs, conditions, crew, rate, recap knobs
  totals      jsonb       NOT NULL DEFAULT '{}'::jsonb,  -- { mat, hrs, bid }
  created_at  timestamptz NOT NULL DEFAULT now(),
  updated_at  timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE elite_estimates ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "elite_estimates: owner only" ON elite_estimates;
CREATE POLICY "elite_estimates: owner only"
  ON elite_estimates FOR ALL
  USING  (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS elite_estimates_user_updated
  ON elite_estimates (user_id, updated_at DESC);

-- ── AI USAGE (rate-limit ledger for /api/claude) ─────────────────────────────
-- Written/read by the server with the service-role key. The limiter fails open
-- if this table is absent, so AI Takeoff still runs — but Free-tier rate
-- limiting only works once this table exists.
CREATE TABLE IF NOT EXISTS ai_usage (
  id          uuid        DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id     uuid        REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  created_at  timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE ai_usage ENABLE ROW LEVEL SECURITY;
-- No client policies: only the service-role key (server) touches this table,
-- and the service role bypasses RLS. Clients get no direct access.

CREATE INDEX IF NOT EXISTS ai_usage_user_created
  ON ai_usage (user_id, created_at DESC);
