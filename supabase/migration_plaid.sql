-- ============================================================
-- Wireway Phase 2 — Plaid Tables
-- Run in Supabase SQL Editor (Dashboard → SQL Editor → New query)
-- Run AFTER migration_finance.sql (Phase 1).
-- ============================================================

-- ── PLAID ITEMS ──────────────────────────────────────────────────────────────
-- One row per linked bank/card account.
-- access_token is sensitive; Supabase encrypts at rest and RLS restricts rows.
CREATE TABLE IF NOT EXISTS plaid_items (
  id               uuid        DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id          uuid        REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  item_id          text        NOT NULL,
  access_token     text        NOT NULL,            -- Plaid access token (server-only)
  institution_id   text,
  institution_name text,
  cursor           text,                             -- /transactions/sync pagination cursor
  last_synced_at   timestamptz,
  created_at       timestamptz DEFAULT now() NOT NULL,
  UNIQUE (user_id, item_id)
);

ALTER TABLE plaid_items ENABLE ROW LEVEL SECURITY;

-- Users can only read/delete their own items (no direct insert/update from client;
-- that goes through the serverless functions which use the service role key).
CREATE POLICY "plaid_items: owner read/delete"
  ON plaid_items FOR SELECT
  USING (auth.uid() = user_id);

CREATE POLICY "plaid_items: owner delete"
  ON plaid_items FOR DELETE
  USING (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS plaid_items_user ON plaid_items (user_id);

-- ── PLAID TRANSACTIONS ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS plaid_transactions (
  id                     text        PRIMARY KEY,   -- Plaid transaction_id (stable)
  user_id                uuid        REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  plaid_item_id          uuid        REFERENCES plaid_items(id) ON DELETE CASCADE NOT NULL,
  account_id             text        NOT NULL,
  txn_date               date        NOT NULL,
  amount                 numeric(10,2) NOT NULL,    -- positive = money out (expense)
  merchant_name          text,
  raw_name               text,
  plaid_category_primary text,
  plaid_category_detail  text,
  mapped_category        text        NOT NULL DEFAULT 'other',
  pending                boolean     NOT NULL DEFAULT false,
  -- user can override the auto-mapped category
  user_category          text,
  synced_at              timestamptz DEFAULT now() NOT NULL
);

ALTER TABLE plaid_transactions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "plaid_transactions: owner all"
  ON plaid_transactions FOR ALL
  USING  (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS plaid_txn_user_date
  ON plaid_transactions (user_id, txn_date DESC);

CREATE INDEX IF NOT EXISTS plaid_txn_item
  ON plaid_transactions (plaid_item_id);
