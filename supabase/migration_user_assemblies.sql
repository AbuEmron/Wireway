-- ============================================================
-- Wireway — User assemblies (contractor-authored job templates)
-- Run in Supabase SQL Editor. Idempotent (IF NOT EXISTS).
--
-- Backs the job-walk template library's "My templates". Each row is a named
-- bundle of REAL catalog line items (service id + qty + variant index) the
-- contractor built; it expands deterministically in the app — no prices are
-- stored here, only references, so nothing can go stale or be fabricated.
-- ============================================================

CREATE TABLE IF NOT EXISTS user_assemblies (
  id           uuid        DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id      uuid        REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  name         text        NOT NULL,
  description  text        DEFAULT '' NOT NULL,
  category     text        DEFAULT 'Custom' NOT NULL,
  -- Array of { service_id: text, qty: numeric, variant_idx: int }.
  items        jsonb       DEFAULT '[]'::jsonb NOT NULL,
  created_at   timestamptz DEFAULT now() NOT NULL,
  updated_at   timestamptz DEFAULT now() NOT NULL
);

ALTER TABLE user_assemblies ENABLE ROW LEVEL SECURITY;

-- Owner-only: a contractor sees and edits only their own templates.
DROP POLICY IF EXISTS "user_assemblies: owner all" ON user_assemblies;
CREATE POLICY "user_assemblies: owner all"
  ON user_assemblies FOR ALL
  USING  (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS user_assemblies_user ON user_assemblies (user_id);

-- Keep updated_at fresh on edits (mirrors the other tables' trigger convention).
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS user_assemblies_set_updated_at ON user_assemblies;
CREATE TRIGGER user_assemblies_set_updated_at
  BEFORE UPDATE ON user_assemblies
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();
