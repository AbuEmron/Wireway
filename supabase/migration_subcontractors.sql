-- ============================================================
-- Wireway — Subcontractors + 1099 Tracking  ·  Feature 3
-- Run in Supabase SQL Editor. Run AFTER migration_jobcosting.sql.
-- Safe to re-run.
-- ============================================================

-- ── SUBCONTRACTORS ─────────────────────────────────────────────────────────────
-- tax_id is sensitive PII (EIN/SSN). It is protected by owner-only RLS and is never
-- sent anywhere off Supabase — it exists only so the owner can generate a 1099.
CREATE TABLE IF NOT EXISTS subcontractors (
  id            uuid        DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id       uuid        REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  name          text        NOT NULL,
  business_name text,
  email         text,
  phone         text,
  address       text,
  tax_id        text,                              -- EIN or SSN (owner-only via RLS)
  tax_id_type   text        DEFAULT 'ein',         -- 'ein' | 'ssn'
  w9_received   boolean     NOT NULL DEFAULT false,
  notes         text,
  created_at    timestamptz DEFAULT now() NOT NULL
);

ALTER TABLE subcontractors ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "subcontractors: owner only" ON subcontractors;
CREATE POLICY "subcontractors: owner only"
  ON subcontractors FOR ALL
  USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS subcontractors_user ON subcontractors (user_id);

-- ── SUBCONTRACTOR PAYMENTS ───────────────────────────────────────────────────────
-- The per-sub payment ledger. job_id ties a payment to a job so it flows into
-- Job Costing actuals (Feature 1) as subcontractor cost.
CREATE TABLE IF NOT EXISTS sub_payments (
  id                uuid        DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id           uuid        REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  subcontractor_id  uuid        REFERENCES subcontractors(id) ON DELETE CASCADE NOT NULL,
  job_id            uuid        REFERENCES jobs(id) ON DELETE SET NULL,
  amount            numeric(12,2) NOT NULL CHECK (amount >= 0),
  payment_date      date        NOT NULL,
  method            text,                            -- check | cash | ach | zelle | card | other
  memo              text,
  created_at        timestamptz DEFAULT now() NOT NULL
);

ALTER TABLE sub_payments ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "sub_payments: owner only" ON sub_payments;
CREATE POLICY "sub_payments: owner only"
  ON sub_payments FOR ALL
  USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS sub_payments_user_date ON sub_payments (user_id, payment_date DESC);
CREATE INDEX IF NOT EXISTS sub_payments_sub       ON sub_payments (subcontractor_id);
CREATE INDEX IF NOT EXISTS sub_payments_job       ON sub_payments (job_id);
