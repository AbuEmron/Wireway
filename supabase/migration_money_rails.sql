-- ============================================================
-- Wireway — Money-Rails (online draw collection)  ·  Phase 2 · Feature 1
-- Run in Supabase SQL Editor. Run AFTER migration_billing.sql.
-- Safe to re-run.
-- ============================================================

-- Record online payments against a progress-billing draw. The Stripe webhook
-- (direct charge on the contractor's connected account) writes these and then
-- recomputes jobs.collected so Job Costing actual margin stays live.
ALTER TABLE job_draws ADD COLUMN IF NOT EXISTS payment_amount    numeric(12,2);
ALTER TABLE job_draws ADD COLUMN IF NOT EXISTS stripe_session_id text;

-- Used by the webhook for idempotency (don't double-count a retried event).
CREATE INDEX IF NOT EXISTS job_draws_session ON job_draws (stripe_session_id);
