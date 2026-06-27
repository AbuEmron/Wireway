-- ============================================================
-- Wireway — Snap-a-Receipt  ·  Feature 2
-- Run in Supabase SQL Editor. Run AFTER migration_jobcosting.sql.
-- Safe to re-run.
-- ============================================================

-- Receipts become expense rows (expenses already has receipt_url + job_id from
-- earlier migrations). Add a `source` tag so the dashboard can tell a snapped
-- receipt apart from a manual entry or CSV import.
ALTER TABLE expenses ADD COLUMN IF NOT EXISTS source text NOT NULL DEFAULT 'manual';
  -- values: 'manual' | 'receipt' | 'csv'

-- ── RECEIPTS STORAGE BUCKET ──────────────────────────────────────────────────
-- Public bucket (consistent with the existing photos/logos buckets) so the saved
-- receipt image can be shown back to the owner via a public URL. Paths are
-- namespaced by user id and unguessable; writes are owner-scoped by RLS below.
INSERT INTO storage.buckets (id, name, public)
VALUES ('receipts', 'receipts', true)
ON CONFLICT (id) DO NOTHING;

-- Owner-scoped object policies: a user may only write under their own /{uid}/ folder.
DROP POLICY IF EXISTS "receipts: owner upload" ON storage.objects;
CREATE POLICY "receipts: owner upload"
  ON storage.objects FOR INSERT TO authenticated
  WITH CHECK (bucket_id = 'receipts' AND (storage.foldername(name))[1] = auth.uid()::text);

DROP POLICY IF EXISTS "receipts: owner update" ON storage.objects;
CREATE POLICY "receipts: owner update"
  ON storage.objects FOR UPDATE TO authenticated
  USING (bucket_id = 'receipts' AND (storage.foldername(name))[1] = auth.uid()::text);

DROP POLICY IF EXISTS "receipts: owner delete" ON storage.objects;
CREATE POLICY "receipts: owner delete"
  ON storage.objects FOR DELETE TO authenticated
  USING (bucket_id = 'receipts' AND (storage.foldername(name))[1] = auth.uid()::text);

DROP POLICY IF EXISTS "receipts: public read" ON storage.objects;
CREATE POLICY "receipts: public read"
  ON storage.objects FOR SELECT
  USING (bucket_id = 'receipts');
