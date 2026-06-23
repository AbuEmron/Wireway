-- ============================================================
-- Wireway Finance Tables — Mileage Tracker + Expense Tracker
-- Run this in Supabase SQL Editor (Dashboard → SQL Editor → New query)
-- ============================================================

-- ── TRIPS (mileage log) ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS trips (
  id           uuid        DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id      uuid        REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  trip_date    date        NOT NULL,
  miles        numeric(8,1) NOT NULL CHECK (miles > 0),
  purpose      text        NOT NULL,
  start_loc    text,
  end_loc      text,
  notes        text,
  created_at   timestamptz DEFAULT now() NOT NULL
);

ALTER TABLE trips ENABLE ROW LEVEL SECURITY;

CREATE POLICY "trips: owner only"
  ON trips FOR ALL
  USING  (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS trips_user_date ON trips (user_id, trip_date DESC);

-- ── EXPENSES ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS expenses (
  id           uuid        DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id      uuid        REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  expense_date date        NOT NULL,
  amount       numeric(10,2) NOT NULL CHECK (amount >= 0),
  category     text        NOT NULL,
  vendor       text,
  description  text,
  receipt_url  text,
  created_at   timestamptz DEFAULT now() NOT NULL
);

ALTER TABLE expenses ENABLE ROW LEVEL SECURITY;

CREATE POLICY "expenses: owner only"
  ON expenses FOR ALL
  USING  (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS expenses_user_date ON expenses (user_id, expense_date DESC);
