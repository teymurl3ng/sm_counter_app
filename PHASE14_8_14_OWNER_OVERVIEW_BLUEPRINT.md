# Phase 14.8.14 — Owner Overview blueprint

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION** (owner: continue without blueprint pause)

Date: 2026-09-21

## Where this checkpoint starts

Cashier (`Sell / Collect / Activity / More`) and Operations (`Work / Deliveries / Attendance / More`) are connected in the debug `NativePreviewActivity` harness. Leadership native UI has not started.

The accepted Counter Ledger plan’s next retained family is **Owner** (`Overview / Work / Records / More`). Authority:

- `PHASE14_8_2_COUNTER_LEDGER_LEADERSHIP.md`
- `design/stitch/reference-blueprints/counter-ledger-leadership.html` (Owner Overview states)
- `design/refined-counter-ledger/index.html` (accepted Owner · Overview remake)
- `.cursor/rules/cross-role-native-ui-consistency.mdc` (reuse sibling chrome only when the surface kind matches; do not flatten Owner into worker shells)

This checkpoint owns **Owner Overview home** only: decision-first day summary + attention. Month reports nests under Overview as a later connected destination (`PHASE14_8_32_OWNER_REPORTS_BLUEPRINT.md`). It does not implement Owner Work, Records, More, Cash & close, or reopen in this checkpoint’s original slice.

## What the existing product already guarantees

- Owner has full native permissions, including `VIEW_OPERATIONAL` and `VIEW_FULL_FINANCIAL`.
- Typed reads already exist for stock (`query.dashboard` / `HybridReadModelService.dashboard`), period sales and balances (`financialOverview`), profitability with honest coverage (`profitabilityReport`), event history, reconciliation archive (cash counts / close actions), and cloud backup status (`CloudBackupStatus`).
- Business date open/closed and Cash account expected balance are already domain truth; Cashier More already records blind counts.
- There is **no** existing Compose Owner shell and **no** single `OwnerOverviewDto` — the native facade must compose presentation from existing services without inventing accounting formulas.
- Worker shells (Cashier Sell/Collect/Activity/More, Ops Work and task screens) are not the Overview layout. Shared theme tokens, identity-header *pattern*, NavigationBar chrome, and facade→VM→contract/`qaScreen` harness may be reused.

## Normal path

1. An Owner signs in (preview harness: `open_owner=true`) and opens the Owner shell on **Overview**.
2. Header matches the refined Owner Overview chrome: title **Overview**, shop/business-date line, Open/Closed. Do not paste Cashier Sell or Ops Work first-viewport content.
3. First viewport answers “how is the shop doing / what needs a decision?”:
   - **Attention** (when any): concise banner or list of issues that require Owner attention (see Material states).
   - **Today’s sales** — primary amount for the open business date.
   - **Estimated gross result** — for covered records only, with honest coverage wording when incomplete.
   - **Balances & outstanding** — expected cash, customers owe, owed to suppliers.
   - **Stock on hand** — Milk and Yogurt quantities/units from current position.
   - **Latest activity** — most recent business-wide record with actor and amount or quantity as applicable.
4. Bottom nav: **Overview** (connected) / **Work** / **Records** / **More**. Non-Overview destinations show explicit “next in family” preview notices until those Owner screens are owned.
5. Attention **Review** actions that would leave Overview (Close, Records, Backup/More) either route to the matching preview notice for this checkpoint or stay as visible non-mutating cues — they must not invent those destinations.

## Control contract

| Control | Required workflow | Visible result | Native test obligation |
|---|---|---|---|
| Overview nav | Open Owner Overview | Decision summary; money allowed | Destination asserted |
| Sales / gross / balances / stock / activity | Load composed overview | Real Room-backed figures for today | Facade projects existing reads |
| Attention (when present) | Surface urgent issues | Clear labels; no silent empty attention | Attention kinds asserted |
| Coverage warning | Show incomplete cost coverage | Gross/profit honesty callout; stock/cash still shown as reliable | Coverage state asserted |
| Work / Records / More nav | Preview destinations | Explicit not-connected notices | Nav contracts |
| Hardware Back on Overview | Leave authenticated area like Cashier Sell / Ops Work | Session ends; Welcome or Access path | Back contract |

## Material states

| State | Behavior |
|---|---|
| Loading | Owner chrome + progress |
| Needs attention | One or more attention items (cash count waiting; backup needs attention when status says so; optional coverage callout) |
| All clear | Calm “no urgent issues” status when none of the in-scope attention kinds apply |
| Coverage warning | Incomplete costing coverage disclosed; sales/balances/stock remain; gross labeled as covered-only / incomplete |
| Empty latest activity | Honest empty copy when no events exist yet |
| Closed business date | Open/Closed chip reflects closed; overview remains readable (no Owner reopen UI here) |
| Session expired | Leave authenticated area via existing auth path |

### Attention kinds in this checkpoint

Must support from existing services (no new domain engine):

1. **Cash count waiting** — open business date has no Cash physical count in the reconciliation archive (or equivalent existing read).
2. **Drive backup needs attention** — when cloud backup is configured and status reports stale, pending failure, or last-error attention (use `CloudBackupStatus` fields already available).
3. **Cost coverage incomplete** — from profitability coverage codes for the overview period (callout / gross labeling).

Deferred (leadership HTML shows them; do not invent aging or restore UI here):

- Customer/supplier **overdue aging** (“N balances overdue”) until a presentation-safe aging projection exists without new accounting rules.
- Sparkline / percent delta trend (refined accepted preview does not require a sparkline).
- Live navigation into Cash & close, business-wide Records, or Backup & recovery.

## Native data boundary

`NativeOwnerOverviewFacade` (presentation-neutral) + thin ViewModel:

- require authenticated session; role must be Owner for this shell (preview path seeds/opens Owner);
- require `VIEW_OPERATIONAL` + `VIEW_FULL_FINANCIAL` for money sections;
- compose for the open business date (today’s range):
  - sales total and related money from existing financial/period reads;
  - estimated gross / coverage honesty from existing profitability reads;
  - expected cash from Cash/Bank position and/or reconciliation truth already used by count/close;
  - customer due and supplier due as sums (or clear aggregates) of existing customer/vendor positions;
  - Milk / Yogurt stock from `dashboard` / current position;
  - latest activity from business-wide event history (Owner is not limited to own-actor Activity);
  - attention flags from reconciliation archive + cloud backup status + coverage;
- do **not** change Room schema, RolePermissions, Analytics formulas, costing, or backup cryptography;
- do **not** call bridge JSON handlers; consume typed Kotlin services/facades only.

## Visual / consistency notes

- Follow the **refined Owner Overview** composition (attention → sales/gross → balances → stock → latest activity → Owner bottom nav), not Cashier Sell or Ops Work.
- Reuse Counter Ledger theme tokens and NavigationBar styling.
- Adapt shared green header tokens only where they fit this screen’s accepted chrome; do not force worker menu-row or physical-task launcher patterns onto Overview.
- Inaccessible destinations do not appear as disabled placeholders inside Overview content.

## Out of scope

- Owner Work launcher, Records, More, Cash & close, Owner reopen (Month reports connected separately in 14.8.32)
- Manager / Administrator shells
- Overdue-balance aging engine
- Launcher migration away from `HybridActivity`
- Changing Cashier or Operations connected screens except shared harness wiring needed for Owner preview entry

## Connected follow-on

- **Month reports** — nested Overview destination; see `PHASE14_8_32_OWNER_REPORTS_BLUEPRINT.md`.

## Verification

Facade + Compose contract tests; register tags/inventory; harness `qaScreen -Pqa.screen=owner` (or `overview` if that name stays clearer in inventory — pick one and keep it stable). Prove on A06 with `:app:assembleDebug` → uninstall `com.saaf.milkshop` / `.qa` / `.qa.tests` → install → launch `NativePreviewActivity` with `open_owner=true`. Keep Cashier and Operations `qaCheckpoint` / connected contracts green.

## Accept gate

Auto-accepted under the standing “continue” instruction. If the composition or attention kinds need correction before Kotlin, say so before Phase B implementation.
