# Wireway Pro — Google Play subscriptions setup (owner walkthrough)

The native app already speaks Play Billing 7 and expects these six subscription
product IDs (each is its own subscription with one base plan):

| Product ID | Tier | Cycle | Price to set |
|---|---|---|---|
| `wireway_pro_monthly` | Pro | 1 month | $12 |
| `wireway_pro_yearly` | Pro | 1 year | $120 (2 months free) |
| `wireway_teams_monthly` | Teams | 1 month | $29 |
| `wireway_teams_yearly` | Teams | 1 year | $290 (2 months free) |
| `wireway_elite_monthly` | Elite | 1 month | match your Stripe Elite price (Stripe → Products) |
| `wireway_elite_yearly` | Elite | 1 year | 10× the Elite monthly |

Product IDs are permanent — they can't be renamed or reused after creation, so
enter them exactly as above.

---

## Step 0 — Decide which Play app these live under (do this first)

Play products belong to ONE package name. Your Play test track currently has the
**wrapped** build under `com.wirewaypro.app`; the native app builds as
`com.wirewaypro.app.native`.

- **Option A — one app forever (recommended):** ship the native app under
  `com.wirewaypro.app`. Testers upgrade in place, one listing, one set of
  products. Requires: bump `applicationId` in the native build config, a higher
  `versionCode` than the wrapped build, and signing with the SAME upload key
  the wrapped build used.
- **Option B — separate listing:** create a new Play app for
  `com.wirewaypro.app.native`. Clean, but two listings and the products must be
  created under the new app.

Create the six products under whichever app will actually ship the native build.

## Step 1 — Payments (merchant) profile — required before any product

1. Go to https://play.google.com/console → left menu (top level, not inside an
   app) → **Settings → Payments profile**.
2. **Create payments profile** (or link an existing Google Payments profile):
   legal business name + address (sole proprietor: your own name is fine).
3. Complete **tax info** (US: W-9 details / SSN or EIN) and add the **bank
   account** for payouts; Google verifies it with a small test deposit
   (~2–3 days).
4. When the profile shows as active, the **Monetize with Play** section unlocks.

Only you can do this step — it's identity + banking.

## Step 2 — Create the six subscriptions

For each row of the table, inside your app in Play Console:

1. **Monetize with Play → Products → Subscriptions → Create subscription**.
2. **Product ID:** exactly e.g. `wireway_pro_monthly`. **Name:** e.g.
   "Wireway Pro (Monthly)" — shown to buyers.
3. **Add base plan** → id `monthly` (or `yearly`), type **Auto-renewing**,
   billing period **1 month** (or **1 year**).
4. Recommended base-plan settings: grace period **7 days**, account hold on
   (default), resubscribe on.
5. **Set price:** enter the US price; let Play auto-fill other countries.
6. **Activate** the base plan, then **Activate** the subscription.

Repeat ×6. No offers/free trials needed to start — they can be added later
without code changes.

## Step 3 — Make purchases testable

1. **Settings → License testing** (top-level menu): add your Google account
   email(s). License testers use test payment methods — no real charges, fast
   renewal cycles.
2. Billing only works for builds **installed via Play**: upload a signed release
   build (AAB) to **Internal testing**, add yourself as a tester, install from
   the opt-in link, then open Profile → subscriptions in the app. The Play
   "products unavailable" state disappears once the products are active and the
   installed build matches the uploaded package + signature.

## Step 4 — Entitlement sync (backend, later)

The app acknowledges purchases (no auto-refunds), but `profiles.plan` in
Supabase doesn't know about Play purchases yet. When ready:

1. Google Cloud: create a **service account**; in Play Console → **Users and
   permissions / API access**, link it and grant "View financial data" +
   "Manage orders and subscriptions".
2. Backend endpoint (`api/`): verify the purchase token with the Play Developer
   API (`purchases.subscriptionsv2.get`) and set `profiles.plan` from the
   product ID prefix (`wireway_elite_*` → elite, etc.).
3. Optional but worth it: **Real-time developer notifications** (Pub/Sub) so
   cancellations/renewals sync automatically.

## Quick answers

- **Fees:** 15% on the first $1M/yr of subscription revenue (with the 15%
  program), then 30%.
- **Can I create products before uploading a build?** Yes — only the payments
  profile is required. Purchases, however, need a Play-installed build.
- **Changing a price later:** allowed; existing subscribers keep legacy pricing
  unless you migrate them.
- **The app is already safe:** if products don't exist yet, the subscriptions
  screen shows a clean "unavailable" state — nothing crashes.
