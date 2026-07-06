# Wireway Pro — Pricing Tiers & Upgrade Ladder
_Locked 2026-07-02. The value ladder and the natural upgrade triggers. Features are gated Free / Pro / Elite; upgrades surface as contextual moments, never nag walls._

## The ladder in one line
**Free = try it and trust it. Pro = win the job and get paid. Elite = run and grow the business.**
Each jump should feel like the app leveling up *with* the contractor, at the moment they need it.

## Guiding rules
- **Never paywall accuracy.** The NEC calculators stay usable on Free — correctness is the trust magnet that earns the download and the word-of-mouth.
- **The wall must arrive at the moment of readiness to pay.** Free proves value fully, then hits a ceiling exactly when the user is ready to act like a business.
- **Upgrades are contextual, not naggy.** Show the upsell inline at the point of value ("Send it clean, with your logo — Pro"), not as interstitial walls.

## FREE — try it and trust it
- NEC calculators, **unlimited** (Wire Size, Voltage Drop, Conduit Fill, Box Fill, Derating) — the trust magnet.
- A small number of complete quotes so they experience the AI quote builder + live material pull list end to end.
- Basic material list. PDF export is **watermarked** ("Made with Wireway") and cannot carry the contractor's logo.
- Caps: limited saved estimates/clients.

**Free → Pro trigger: "I just landed a customer."**
- Finishes a quote, goes to send it — it's watermarked and can't show their logo. Nobody hands a paying customer a watermarked quote.
- Wins the job and wants a deposit / invoice / to take a card or ACH payment — getting paid lives in Pro (money in their pocket today = strongest, most natural upgrade).
- Starts bidding regularly and hits the free quote cap.

## PRO (~$12/mo) — win the job and get paid
Everything a working electrician needs to quote and get paid:
- Unlimited estimates / clients / jobs.
- AI quote builder + live material pull list (with the tap-to-detail price breakdown).
- All NEC calculators, offline.
- Branded, un-watermarked PDF proposals (own logo + accent color).
- Convert-to-invoice + get-paid: card/ACH pay links, deposits.
- Templates / assemblies, duplicate estimate, follow-ups, expiring-quote flags.
- Per-job profitability (estimate-level).
- **Basic scheduling calendar** — schedule their own jobs/appointments, see the week/month at a glance.
- Offline-first sync, material database + labor calculator.

**Pro → Elite trigger: "I'm growing, and I'm leaving money on the table."** (Elite pain only appears once Pro made them busy — so it feels like a promotion, not a tax.)
- Brings on a helper/apprentice → needs crew time-tracking + job assignments.
- Wants true job cost across labor + materials (actuals vs estimate), not just the estimate-level profit Pro shows.
- Bookkeeping doubled → wants QuickBooks/Xero sync.
- Bidding bigger jobs → e-signature lifts close rate and ticket size; advanced calculators + blueprint takeoff handle the scope. (NO client financing — see Hard product constraints in WIREWAY_VISION_2.0.md.)
- Too many open quotes to chase by hand → automated follow-ups.

## ELITE — run and grow the business
Everything in Pro, plus the money-making power tools:
- Crew + time tracking; true job costing (actuals vs estimate).
- **Crew scheduling / dispatch calendar** — assign crew to jobs, see everyone's schedule (the team layer on top of Pro's personal calendar).
- Advanced calculators: load/service/panel/transformer/motor sizing, parallel conductors, conduit bending/offsets, solar/battery, demand factors.
- QuickBooks / Xero sync.
- E-signature on proposals.
- Interest-free payment plans only (deposits, milestone/progress draws, split payments — contractor-controlled, ZERO interest/finance charge/APR). NO lending, NO interest-based client financing anywhere — see Hard product constraints in WIREWAY_VISION_2.0.md.
- Deep material manager: barcode/QR scan, supplier price history, truck inventory, POs, vendors.
- Blueprint takeoff (tap fixtures, draw conduit, measure).
- Foreman mode: daily logs, RFIs, change orders, safety/weather/inspection logs.
- Estimate version history / revision tracking / comparison.
- Follow-up automation, deeper branding, priority support.

## TEAMS (~$29/mo) — multi-seat add-on
Sits on top as a seats option: shared clients/jobs, roles, crew assignments. A seats axis rather than a separate feature set (pairs with Pro or Elite).

## Scheduling calendar — placement
- **Pro:** personal job/appointment calendar (schedule your own work, week/month views, tie appointments to jobs/clients).
- **Elite:** crew scheduling/dispatch (assign crew members to jobs, see the whole team's calendar).

## Implementation notes
- Gate features behind a tier flag (Free/Pro/Elite) resolved from the user's subscription (Play Billing).
- Every gated feature has a **contextual upgrade moment** at its point of value, not a generic paywall screen.
- Keep the accuracy/calculator core reachable on Free.
