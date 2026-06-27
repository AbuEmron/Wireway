-- ============================================================
-- Wireway — Get-Paid-Faster A/R  ·  Feature 6
-- Run in Supabase SQL Editor. Run AFTER migration_jobcosting.sql + migration_billing.sql.
-- Safe to re-run.
-- ============================================================

-- Log of payment reminders sent (manually one-tap today; a future Vercel Cron job
-- could write here too). Lets the A/R view show "last reminded N days ago" and
-- avoids nagging a client twice in a day.
CREATE TABLE IF NOT EXISTS ar_reminders (
  id         uuid        DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id    uuid        REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  quote_id   uuid        REFERENCES quotes(id) ON DELETE CASCADE,
  draw_id    uuid        REFERENCES job_draws(id) ON DELETE CASCADE,
  channel    text        NOT NULL,        -- 'sms' | 'email'
  sent_at    timestamptz DEFAULT now() NOT NULL
);

ALTER TABLE ar_reminders ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "ar_reminders: owner only" ON ar_reminders;
CREATE POLICY "ar_reminders: owner only"
  ON ar_reminders FOR ALL
  USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS ar_reminders_user  ON ar_reminders (user_id, sent_at DESC);
CREATE INDEX IF NOT EXISTS ar_reminders_quote ON ar_reminders (quote_id);
CREATE INDEX IF NOT EXISTS ar_reminders_draw  ON ar_reminders (draw_id);
