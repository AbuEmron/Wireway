# Wireway Pro — Native Android

A ground-up **native** Android client for Wireway Pro, built with Kotlin +
Jetpack Compose. It talks to the **same Supabase backend** as the web app, so
data is shared across platforms.

This is **Phase 1: the foundation** — a clean, idiomatic MVVM skeleton with real
auth and one real data read. It is intentionally small. It is *not* the Capacitor
wrapper (that lives in `../android/` on `feature/capacitor-android`); this is a
separate, fully native app that installs **alongside** it during development.

---

## Stack

| Concern        | Choice |
| -------------- | ------ |
| Language       | Kotlin 2.0 |
| UI             | Jetpack Compose + Material 3 (dark-only brand theme) |
| Architecture   | MVVM, layered **data / domain / ui** |
| DI             | Hilt |
| Navigation     | Navigation-Compose (single-Activity) |
| Async          | Coroutines + Flow / StateFlow |
| Backend SDK    | [supabase-kt](https://github.com/supabase-community/supabase-kt) 3.x — `auth-kt` (gotrue) + `postgrest-kt` |
| HTTP engine    | Ktor (Android engine) |
| Build          | Gradle Kotlin DSL + version catalog (`gradle/libs.versions.toml`) |
| Min / Target   | minSdk 26, targetSdk 35 |

`applicationId` is **`com.wirewaypro.app.native`** (debug builds add `.dev`), so it
does **not** collide with the Capacitor app's `com.wirewaypro.app`. Both can be
installed at once on a test device.

---

## Project layout

```
native-android/
├── settings.gradle.kts          # modules + repositories
├── build.gradle.kts             # root plugins (apply false)
├── gradle/libs.versions.toml    # version catalog — all versions live here
├── local.properties.example     # copy → local.properties, add Supabase keys
└── app/
    ├── build.gradle.kts         # app module; reads Supabase keys → BuildConfig
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/                 # theme, colors, adaptive launcher icon
        └── java/com/wirewaypro/app/
            ├── WirewayApplication.kt   # @HiltAndroidApp
            ├── MainActivity.kt         # single Activity, hosts Compose
            ├── di/                     # SupabaseModule, RepositoryModule
            ├── data/                   # repository impls + DTOs (Supabase-facing)
            │   ├── auth/    AuthRepositoryImpl.kt
            │   ├── profile/ ProfileRepositoryImpl.kt, ProfileDto.kt
            │   ├── jobs/    JobRepositoryImpl.kt, JobDto.kt (+ JobDrawDto)
            │   ├── quotes/  QuoteRepositoryImpl.kt, QuoteDto.kt (JSON line items)
            │   └── clients/ ClientRepositoryImpl.kt, ClientDto.kt
            ├── domain/                 # SDK-agnostic models + repository interfaces
            │   ├── model/      AuthState, UserProfile, Job, JobDraw, Quote*, Client
            │   └── repository/ Auth, Profile, Job, Quote, Client repositories
            └── ui/
                ├── theme/              # Color, Type, Shape, Theme (brand palette)
                ├── navigation/         # Routes + bottom-nav tabs + DashDest
                ├── common/             # ListUiState
                ├── util/               # Format (money / date / status)
                ├── components/         # Wordmark, RefreshableList, ListCard,
                │                       #   DetailScaffold, SectionCard, StatusChip…
                ├── auth/               # LoginScreen + LoginViewModel
                ├── dashboard/          # DashboardScreen (nested NavHost) + HomeScreen
                ├── jobs/               # JobsScreen, JobDetailScreen + ViewModels
                ├── quotes/             # Estimates/Invoices/QuoteDetail + ViewModels
                ├── clients/            # ClientsScreen + ViewModel
                └── settings/           # SettingsScreen + ViewModel
```

### Architecture in one breath

The **ui** layer (Compose + ViewModels) depends only on **domain** interfaces.
The **data** layer implements those interfaces against supabase-kt and is the
*only* place that imports Supabase types. Hilt binds impl → interface
(`RepositoryModule`), so screens never see the SDK. This keeps the SDK swappable
and the UI unit-testable with fakes.

Flow:

1. **Login** → `LoginViewModel.signIn()` → `AuthRepository.signIn()` →
   gotrue email/password. The session is persisted by the SDK.
2. `SessionViewModel` mirrors gotrue's session into `AuthState`; `WirewayApp`
   observes it and routes to the dashboard (and back to login on sign-out).
3. **Dashboard** hosts a nested `NavHost`: four bottom-nav tabs (Home /
   Estimates / Invoices / Settings) plus Jobs & Clients lists (reached from Home)
   and list→detail screens, all single-Activity. Each list has its own
   ViewModel + `StateFlow` with loading / error / empty states and
   Material 3 pull-to-refresh.

### Read-only data screens (Phase 2)

All read against the live shared backend; RLS scopes rows to the user.

| Screen | Source | Notes |
| ------ | ------ | ----- |
| Home | `profiles` + `jobs` count | greeting + quick links |
| Estimates (tab) | `quotes` where `invoice_mode` ≠ true | list → detail |
| Invoices (tab) | `quotes` where `invoice_mode` = true | list → detail, surfaces paid/due |
| Jobs | `jobs` | list → detail |
| Job detail | `jobs` + `job_draws` | draws shown as progress-billing line items |
| Estimate / Invoice detail | `quotes` (+ JSON `entries` / `custom_items`) | line items + totals breakdown |
| Clients | `clients` | name, contact, job count, total billed |
| Settings | session | account email, sign out, app version |

> **There is no `invoices` table** — an invoice is a `quotes` row with
> `invoice_mode = true`. Quote **line items are not a related table**; they live
> in the JSON `entries` (catalog items, keyed by id) and `custom_items` (array)
> columns, flattened by `QuoteDto.parseLineItems`.

### Write flows (Phase 3)

All writes go through Postgrest (`insert`/`update`/`delete`) in the data layer;
RLS scopes ownership and `user_id` is set on inserts.

| Flow | Where | Notes |
| ---- | ----- | ----- |
| Quote builder (create/edit) | `QuoteBuilderScreen` | Totals computed by `QuoteCalculator` — identical formula to `electrical-estimator.jsx`. Estimate↔invoice toggle. Auto quote number `WW-YYYY-NNN`. |
| Quote/invoice delete | detail screen | confirm dialog |
| Invoice paid / due date | invoice detail | sets `invoice_paid` + `paid_at` + `status`; `invoice_due_date` |
| Job create/edit/delete | `JobEditScreen`, job detail | status chips; delete confirm (draws cascade) |
| Job draws CRUD | job detail dialog | label, amount, retainage %, status, due date — mirrors `lib/billing.js` |
| Client create/edit/delete | `ClientEditScreen` | |

**Matching the web math (exactly).** `QuoteCalculator.compute` reproduces
`electrical-estimator.jsx`: per custom item `mat = materialCost·qty`,
`lab = laborCost·qty`, `hrs = laborHours·qty`; then `subtotal = totMat + totLab`,
`markupAmt = subtotal·markup`, `taxAmt = taxEnabled ? totMat·taxRate : 0` (tax on
materials only), `total = subtotal + markupAmt + taxAmt`, rounded with the web's
`round2`. The builder edits **custom items only**; any catalog `entries` on an
edited quote are preserved untouched and their money contribution is frozen
(`stored totals − custom items`) so totals never drift. Quote number matches
`genQuoteNum()`: `WW-{year}-{(quoteCount+1) padded to 3}`.

---

## Supabase keys — where they come from

The app reuses the web app's **public** values:

- `REACT_APP_SUPABASE_URL`
- `REACT_APP_SUPABASE_ANON_KEY`

These are set in the web project's `.env` / Vercel env (see the repo's
`.env.example`). The anon key is a **public, RLS-protected** key and is safe to
ship in a client — Row-Level Security on Supabase scopes every query to the
signed-in user. **Never** put the service-role key here.

### Setup

1. Copy the template:
   ```
   cp local.properties.example local.properties
   ```
2. Fill in the two values (same as the web app's `.env`):
   ```properties
   REACT_APP_SUPABASE_URL=https://<project-ref>.supabase.co
   REACT_APP_SUPABASE_ANON_KEY=<public-anon-key>
   ```
3. `local.properties` is git-ignored — keys never get committed. At build time
   `app/build.gradle.kts` copies them into `BuildConfig.SUPABASE_URL` /
   `BuildConfig.SUPABASE_ANON_KEY`, which `SupabaseModule` reads.

Session persistence is automatic: gotrue stores and auto-refreshes the session
on-device (excluded from cloud backups via `res/xml/*` rules).

---

## Open & build in Android Studio

1. Open Android Studio (Ladybug / 2024.2+ recommended for AGP 8.7).
2. **File → Open** → select this `native-android/` folder (open it directly, not
   the repo root — it is its own Gradle project).
3. Create `local.properties` with your Supabase keys (above). Android Studio adds
   the `sdk.dir` line itself.
4. Let Gradle sync. First sync downloads the Compose/Hilt/Supabase dependencies.
5. Run the `app` configuration on a device/emulator (API 26+).

### Gradle wrapper note
The wrapper **scripts** (`gradlew`, `gradlew.bat`) and config are committed, but
the binary `gradle/wrapper/gradle-wrapper.jar` is **not** (binaries aren't
checked in here). Android Studio regenerates it automatically on first open, or
generate it manually with an installed Gradle:
```
gradle wrapper --gradle-version 8.11.1
```

---

## What compiles vs. what needs the SDK

- **Code is written to compile** against the pinned versions in the catalog. The
  build was authored without a local Android SDK, so it has **not been run
  through Gradle here** — expect to do a first sync in Android Studio and adjust
  a version or two if your installed AGP/SDK differs.
- Most likely first-sync touch-ups: confirming the **Compose BOM** ↔ **Kotlin
  2.0.21** ↔ **AGP 8.7.3** trio matches what your Studio has, and that the
  **supabase-kt 3.x** artifact coordinates resolve (the `bom` pins module
  versions). All are standard, current releases.
- Runtime needs your real Supabase keys in `local.properties` and at least one
  user in the project's auth table to sign in with.

---

## Phased roadmap to feature parity with the web app

Phase 1 (this branch) is the foundation. The web app is a deep
contractor-finance suite; reaching parity is a multi-month effort. Suggested
order, each phase building on the last:

| Phase | Theme | Highlights |
| ----- | ----- | ---------- |
| **1 ✅** | Foundation | Auth, session, DI, theme, dashboard shell, one live read |
| **2 ✅** | Read-only core | Estimates / Invoices / Jobs / Clients lists + Job & Quote detail screens, pull-to-refresh, loading/error/empty states, nested navigation, Settings |
| **3 ✅** | Write flows | Quote/estimate **builder** (custom line items, markup, tax, totals matching the web math; estimate↔invoice; auto quote number), Jobs create/edit + delete + `job_draws` CRUD, invoice mark-paid / due-date, Clients create/edit/delete. FABs + edit/delete wired into lists & detail |
| **4 ✅** | Catalog, pickers, receipts, money | Full NEC catalog ported + **editable catalog line items** in the builder (totals match the web exactly, incl. hourly-rate-scaled labor); Material 3 date/time pickers; **receipts + camera** (capture/gallery → `receipts` bucket → expense row) with an expenses list; read-only **money dashboard** (collected / spent / profit) |
| **5 ✅** | Offline, calendar, OCR, fonts | **Offline save queue** (DataStore + connectivity-aware flush; mirrors the web quote queue; pending-sync indicator) for quotes + expenses; **jobs calendar** (month grid + agenda); **in-app CameraX** preview + **receipt OCR** via the same `/api/claude` proxy (auto-fills amount/vendor/date, user confirms); brand-font wiring (drop-in via res/font) |
| **6 ✅** | Deeper money | **AR aging** (unpaid invoice-quotes + invoiced draws, bucketed current/1-30/31-60/61-90/90+), **per-job P&L** (winners/losers from bid vs live actuals), **accountant CSV export** via the share sheet (matches the web's columns) |
| **7** | Final polish | Bundle the actual brand .ttf binaries; WorkManager-backed background sync; offline image queueing for receipts; AR one-tap reminders (sms:/mailto:) |
| **5** | Bookkeeping & invoicing | Invoices, payment status, money dashboard, Stripe/Plaid-backed flows via the existing `/api` endpoints |
| **6** | Receipts & camera | CameraX capture, photo upload to Supabase Storage, receipt attach to jobs/expenses |
| **7** | Offline-first | Room cache + WorkManager sync; make estimates/jobs fully usable with no signal |
| **8** | Polish & release | Real upload-key signing (mirror Capacitor's `keystore.properties`), Play listing, switch `applicationId` to `com.wirewaypro.app` when ready to supersede the Capacitor build, crash reporting, deep links |

### Realistic effort

Phase 1 is a few hundred lines and a day or two of wiring. **Parity is not.** The
web app spans estimating, invoicing, job costing, scheduling, Plaid/Stripe
integrations, AI takeoff, PDF generation, and more — realistically **4–6+ months**
of focused native work for one developer, more if AI-takeoff and PDF/proposal
generation are reimplemented natively rather than reused via the existing `/api`
backend. The fastest path keeps using the web app's server-side `/api` endpoints
(as the Capacitor build does) and reimplements only the **UI** natively, deferring
heavy server logic.
