# Wireway Pro — Vision 2.0

### Evolve, don't rebuild. From estimator to the decision platform electrical contractors consult before they act.

_Adopted 2026-07-05, from AJ's "Vision 2.0" direction and a long strategy pass (AJ + Claude + a second model). This sits ABOVE features: it is the philosophy every session inherits. Read alongside `WIREWAY_ARCHITECTURE_DOCTRINE.md` (how we build — accuracy-first), `WIREWAY_THE_BEAST_PLAN.md` (gates/strategy), and `WIREWAY_PRICING_TIERS.md` (value ladder). Where this doc adds a rule, it does not override the accuracy doctrine — it sharpens why we obey it._

---

## The reframe (say it in one line)

**Wireway Pro is not an estimating app. It is becoming the intelligent operating system electrical contractors consult before they make money decisions.**

Treat the current app as **Version 1** — a working estimate→get-paid loop with offline sync, calculators, AI takeoff, and the S+ interface. Do NOT redesign or replace it. Study the architecture, schema, Kotlin, UI/UX, and feature set, and **evolve** it into Version 2 while preserving everything that already works. Refactor only for maintainability, performance, or usability; keep backward compatibility; never lose data.

## The prime directive (the filter that kills features)

**Every major feature must serve a business DECISION, not just perform a task.**

Estimating is a task. *Deciding whether to bid, how to staff, where profit is at risk, whether the job will pass inspection, and how to be smarter on the next job* — those are decisions. Wireway should become the software a contractor opens before an important choice, not just the calculator they use after they've already decided.

Every feature must improve at least one of these:

- Am I pricing this correctly?
- Am I forgetting something?
- Is this job actually profitable?
- What risks am I overlooking?
- **Should I even take this job?**
- How should I staff this project?
- How can I finish sooner?
- Will this pass inspection here?
- How does my company get smarter after every completed job?

If a feature doesn't move one of those decisions, reconsider whether it belongs. Optimize for contractor trust, speed, transparency, and long-term usefulness — **not feature count**.

---

## The trust laws (non-negotiable — this is what lets the vision survive contact with a real electrician)

1. **Never show a number you can't defend.** A figure dressed as a measurement is more dangerous than no figure, because contractors trust it. One authoritative-looking invented number, smelled out once, kills the trust the whole platform rests on.
2. **No fake precision — Confidence is qualitative until the data earns a percentage.** Do NOT show "Confidence: 83%" sourced from an AI guess. Show:
   > Confidence: **Moderate**
   > • Ceiling type confirmed ✓
   > • Building occupied ✓
   > • Existing conduit not inspected ⚠
   > • Lift access unknown ⚠

   Only after enough of the contractor's OWN completed jobs exist does it harden into a defensible number:
   > "Based on your last 146 lighting retrofits, similar conditions averaged **+6.4% labor**."
3. **Learning must be earned (the honest ramp).** No feature pretends to know on day one. Day 1: "Not enough historical data." After ~50 jobs: "We're beginning to detect patterns." After 500: "Your crews consistently beat industry labor by 9% on schools." Build the *capture* now so the ramp can happen; never fake the destination.
4. **Deterministic core, AI optional.** Every money figure comes from formulas + validated tables + the user's inputs/history — never an AI guess (see the architecture doctrine's hard line). AI may phrase, summarize, suggest; it never sets the final number, and the contractor always sets the final price.
5. **Never silently modify an estimate.** Detectors and scores *recommend with reasoning*; the contractor accepts or rejects each item. Overrides are always allowed and leave an audit trail.
6. **Offline-first, zero data loss.** Field reality has no signal. Proper Room migrations, never destructive.

## Hard product constraints (non-negotiable, values-level)

- **No interest-based lending or financing anywhere in Wireway Pro — ever.** Do NOT build, integrate, scaffold, or surface client financing, pay-over-time lending, interest-bearing loans, or any third-party consumer-credit product (Wisetack, Affirm, and the like are prohibited). This is a firm decision by the owner and overrides any roadmap, pricing, or partner suggestion to the contrary.
- **Interest-free money movement only.** "Pay over time" is allowed ONLY as interest-free arrangements the contractor controls on existing rails: deposits, milestone/progress draws, and split payments with **zero interest, zero finance charge, zero APR**. Never route to a lender, never present a financing offer, never compute or display interest.

---

## The new systems (sequenced, not simultaneous)

Building all of these at once = ten unfinished systems. Build ONE excellent differentiator, prove it, then layer the next. Each is described with its honest ramp and its gate.

### 1. Bid IQ — "Should I even take this job?" (the flagship the flywheel builds toward)
The elevation from "how much will this cost" to "should I do this at all." It ships in **two honest stages**:
- **Stage 1 (now — deterministic, defensible day one):** from conditions the contractor enters + the estimate itself — schedule risk, access difficulty, live-work risk, lift required, thin modeled margin. "High schedule + access risk, margin looks thin — here's what to watch." All from real inputs, nothing invented.
- **Stage 2 (Gate 3 — earned by data):** the comparative verdict — "27 similar jobs, average actual margin 8%, **decline unless pricing increases ~$18,500**." This is the single highest fake-precision risk in the whole product; it may ONLY appear once the contractor's own similar-job history backs it. This is *why* we capture estimate-vs-actual from job one.

### 2. Field Reality Score — conditions → transparent labor adjustment (Gate 1–2)
A field assessment before an estimate is finalized: existing vs. new construction, ceiling access, occupancy, lift, live work, material transport distance, access restrictions, schedule, environmental complexity. Converts to labor adjustments **with the reasoning shown**. Adjustment factors must trace to published labor units or the contractor's own history — never a multiplier Claude picked. This is the doctrine's "deterministic sanity validators," productized.

### 3. Profit Leak Detector — catch commonly-missed costs (Gate 1–2)
Before an estimate completes, flag frequently-forgotten items: fire stopping, labels, lift rental, temp power, disposal, testing, core drilling, supports, grounding/bonding, cleanup, permit items. **Recommends with reasoning; never silently edits.** Rule-based (deterministic), not an AI hunch; AI may phrase the warning.

### 4. Labor Confidence — show why confidence is high or low (Gate 2)
Estimated labor, qualitative confidence (per trust law #2), the unknown assumptions, the major risk factors, and suggested verification steps. Percentage only after the data earns it.

### 5. Crew Reality Simulator — compare staffing plans before you win (Gate 2–3)
Compare multiple crew plans: duration, labor cost, estimated profitability, crew utilization, schedule impact. The natural extension of the Elite crew + time-tracking + job-costing already in build.

### 6. Wireway Intelligence — the learning engine (Gate 3, earned)
Continuously improves from completed jobs: labor accuracy, material waste, frequently-missed items, productivity trends, preferred methods, crew performance, change-order patterns, profitability by project type. **All recommendations explainable.** This is a Gate-3 payoff by definition — build the capture now (the Elite job-costing batch starts it), spin the flywheel, and let it surface via the honest ramp. Do not ship "intelligence" before there is signal.

---

## The standard-owning play — AHJ compliance (the moat competitors can't copy)

The deepest opportunity, and the one nobody fills: **an AHJ-aware code pre-check** — check the estimate and install against the rules the *local inspector* will actually use, before the job fails inspection.

Why it's open: the NEC is national, but no electrician is inspected against "the NEC" — they're inspected against the edition their jurisdiction adopted (many still on 2017/2020 while tools say 2023) plus that jurisdiction's local amendments. Every tool hardcodes one national codebase, so the software says "compliant," the inspector redlines, and the contractor eats a return trip, a re-inspection fee, and a reputation hit. That failure lives in the delta between national code and local rule — exactly where the money leaks and exactly where no software looks.

**If Wireway becomes the canonical, trusted place that "what does THIS jurisdiction enforce" lives, it stops being an app and becomes infrastructure other tools must reference. Whoever creates the standard — no matter how small — becomes the forerunner.**

Build rules (this is a trust artifact above all):
- **Anyone can select their AHJ — universal from day one.** The user picks their jurisdiction (state → county → city/AHJ), and it's saved to their profile so every estimate is checked against *their* local rules, not a hardcoded default. No user is locked out because their area isn't "the seed." A location/GPS assist can pre-suggest the jurisdiction, but the contractor always confirms and can override it.
- **Deterministic rule engine, not AI.** "This AHJ → adopted NEC 2020 + amendment set → this feeder/panel/circuit needs X." Every flag defensible in front of the inspector.
- **Verified-vs-unverified trust model, per jurisdiction.** Every rule is timestamped, sourced to the adopting ordinance, and marked `verified-by-outcome` vs `unverified`. Coverage depth is shown honestly for the selected AHJ — e.g. "Adopted edition: NEC 2020 (verified). Local amendments: not yet mapped for your county — help us verify." Never present an unverified or absent rule as certainty. That honesty is what makes inspectors and electricians willing to feed it — which is what makes it grow — which is what makes it the standard. Under-claim and it compounds; over-claim once and it dies.
- **Universal capture beats a single seed.** Because every user everywhere selects and (over time) validates their own jurisdiction, the dataset deepens from the whole user base, not one anchor city — that's a stronger flywheel. Bootstrap it with the finite, public **state-level adopted NEC edition** (the ~50 states/territories, many with known adoption dates) so *every* user gets a real, defensible baseline the moment they pick their area; then let county/city amendments and logged inspection outcomes harden each jurisdiction from `unverified` toward `verified-by-outcome`.

---

## Sequencing onto the gates (don't build everything at once)

**NOW (Gate 1–2, in flight):** S+ interface (done) · Elite crew + time-tracking + true job-costing · job-walk completion (voice, room/area templates, photos) · Field Reality Score inputs + Profit Leak Detector (deterministic validators) · Bid IQ **Stage 1** (deterministic risk side) · start capturing estimate-vs-actual for the flywheel.

**NEXT (Gate 2–3):** Labor Confidence · Crew Reality Simulator · AHJ standard seeded on the home jurisdiction (adopted-edition flag first) · deeper material manager, advanced calculators.

**LATER (Gate 3, earned):** Wireway Intelligence · Bid IQ **Stage 2** (comparative verdict) · AHJ standard spreading county-by-county · interest-free payment-plan depth (deposits, milestone draws, split payments — NEVER lending or interest, per Hard product constraints) · native bookkeeping. These require data or (for accounting sync) an external account — scaffold now, activate when the signal or account exists; never fake them.

---

## The review loop (scoped)

After a MAJOR feature lands, run a self-review from these lenses, then revise before calling it complete:
- A master electrician with 30 years in the field.
- A commercial estimator bidding $10M+ annually.
- A small contractor with 8–20 employees.
- A native-Android UX engineer.
- A Kotlin architect focused on maintainability + performance.

Scope it: **full panel on flagship / money features; a lighter pass on small ones** — or you spend more time role-playing critiques than building. And pair it with real verification: persona reviews catch UX friction, missing line items, and maintainability, but can also produce confident-but-ungrounded "expert" opinions — so **NEC-table unit tests, and eventually a real electrician's eyes, remain the source of truth.**

---

## The long-term goal

Transform Wireway from an estimating app into the **daily decision platform** for electrical contractors — helping them estimate accurately, decide which jobs to take, reduce risk, staff smarter, pass inspection, increase profitability, and get smarter after every completed job. Features are copyable. Algorithms are copyable. AI is copyable. **Years of a contractor's own proprietary performance data — and being the trusted standard for what each jurisdiction enforces — are not.** Every completed job should make the software more valuable to that contractor and harder to leave. Build so that Wireway feels like it was made by people who truly understand electrical contracting — because the numbers are always true.
