-- ============================================================
-- Wireway — Electronic Signature (ESIGN/UETA) records + append-only audit trail
-- Run in Supabase SQL Editor (Dashboard → SQL Editor → New query).
-- Run anytime after the core schema. Safe to re-run: every statement is idempotent.
--
-- Two tables, both per-user (RLS) and APPEND-ONLY:
--   esign_records       — one immutable row per sealed signature (the proof).
--   esign_audit_events  — the step-by-step signing log (consent, sign, seal, verify).
--
-- Append-only is enforced at the DATABASE level: we grant only INSERT + SELECT
-- policies. With RLS enabled and NO update/delete policy, Postgres denies every
-- UPDATE and DELETE by default — so a signed record or an audit event can never be
-- altered or removed through the API, even by its owner. That immutability is what
-- makes the trail trustworthy evidence.
--
-- NOTE ON HASHES: content_sha256 is the fingerprint of the signed proposal (printed
-- on the certificate); sealed_sha256 is the hash of the final sealed PDF that the
-- app's "Verify integrity" recomputes. The sealed PDF + encrypted signature image
-- are stored ON DEVICE in V1; server-side blob storage (Supabase Storage, encrypted)
-- is a documented follow-up — this schema holds the evidentiary record + hashes.
-- ============================================================

-- ── Signed records (immutable proof) ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS esign_records (
  id                uuid        PRIMARY KEY,                 -- app-minted (UUID) so offline seals are stable
  user_id           uuid        REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  quote_id          text        NOT NULL,                    -- the signed quote/proposal (no FK: id type stays decoupled)
  document_title    text        NOT NULL,
  signer_name       text        NOT NULL,
  signer_email      text,
  method            text        NOT NULL,                    -- 'drawn' | 'typed'
  consent_version   text        NOT NULL,                    -- disclosure version the signer saw
  consent_given_at  timestamptz NOT NULL,
  signed_at         timestamptz NOT NULL,
  content_sha256    text        NOT NULL,                    -- fingerprint of the signed proposal
  sealed_sha256     text        NOT NULL,                    -- hash of the final sealed PDF (tamper check)
  device_model      text        NOT NULL,
  app_version       text        NOT NULL,
  ip_address        text,                                    -- best-effort; null = not recorded (never fabricated)
  created_at        timestamptz DEFAULT now() NOT NULL
);
CREATE INDEX IF NOT EXISTS esign_records_user_idx  ON esign_records (user_id);
CREATE INDEX IF NOT EXISTS esign_records_quote_idx ON esign_records (quote_id);

ALTER TABLE esign_records ENABLE ROW LEVEL SECURITY;

-- INSERT + SELECT only — NO update/delete policy ⇒ rows are immutable (append-only).
DROP POLICY IF EXISTS "esign_records: owner insert" ON esign_records;
CREATE POLICY "esign_records: owner insert"
  ON esign_records FOR INSERT
  WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "esign_records: owner read" ON esign_records;
CREATE POLICY "esign_records: owner read"
  ON esign_records FOR SELECT
  USING (auth.uid() = user_id);

-- ── Append-only audit trail ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS esign_audit_events (
  id               uuid        PRIMARY KEY,                  -- app-minted
  user_id          uuid        REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  record_id        uuid        NOT NULL,                     -- the record this event belongs to (no FK: consent events precede the record row)
  event_type       text        NOT NULL,                    -- consent_presented | consent_given | declined_for_paper | signature_confirmed | signature_captured | document_sealed | integrity_verified | integrity_failed
  at               timestamptz NOT NULL,                     -- when the event occurred (device clock at signing)
  consent_version  text        NOT NULL,                     -- disclosure version in effect for this event
  detail_json      text,                                     -- small event-specific detail (method, hashes, ...)
  created_at       timestamptz DEFAULT now() NOT NULL
);
CREATE INDEX IF NOT EXISTS esign_audit_events_user_idx   ON esign_audit_events (user_id);
CREATE INDEX IF NOT EXISTS esign_audit_events_record_idx ON esign_audit_events (record_id);

ALTER TABLE esign_audit_events ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "esign_audit_events: owner insert" ON esign_audit_events;
CREATE POLICY "esign_audit_events: owner insert"
  ON esign_audit_events FOR INSERT
  WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "esign_audit_events: owner read" ON esign_audit_events;
CREATE POLICY "esign_audit_events: owner read"
  ON esign_audit_events FOR SELECT
  USING (auth.uid() = user_id);
