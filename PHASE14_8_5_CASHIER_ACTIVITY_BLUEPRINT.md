# Phase 14.8.5 — Cashier Activity blueprint

Status: **ACCEPTED FOR NATIVE IMPLEMENTATION** (compressed gate; Counter Ledger already selected)

Date: 2026-09-20

## Where this checkpoint starts

Native Auth, Sale, and Collect are connected in the debug `NativePreviewActivity` harness. Collect success still offers `View record`, and Cashier nav still offers `Activity`, but both were preview notices. This checkpoint owns those destinations.

More (cash count, personal security, switch account, sign out) remains a later checkpoint.

## What the existing product already guarantees

- Saved Sale and Collect events retain actor attribution via `event_actor_links`.
- Cashier has `VIEW_OPERATIONAL` and `SALE_AND_COLLECTION` but not `VIEW_FULL_FINANCIAL` or `CORRECTION`.
- `query.eventHistory` returns the shop-wide operational list and is too wide for Cashier Activity.
- Cashier Activity must not invent a shift entity; scope is actor + current business date.

## Normal path

1. Open `Activity` from Cashier navigation (or land on detail via Collect `View record`).
2. Show **Today** for the open business date: records attributed to the signed-in Cashier only.
3. Each row shows time, task label (Sale / Collection), record number, amount, and a short paid/account state.
4. Tap a row to open Cashier-scoped detail for that event.
5. Detail shows record number, time, task, amount, counterpart (customer when applicable), destination/payment state when applicable, and notes when present.
6. Detail does **not** show full multi-account ledger dumps, expected cash, variance, profit, or other cashiers' work.
7. Back from detail returns to the Today list (or to Collect when opened from `View record` if that stack is active).
8. Empty today shows a calm “No records yet today” state with navigation still available.

## Control contract

| Control | Required workflow | Visible result | Native test obligation |
|---|---|---|---|
| `Activity` nav | Open Cashier Activity list | Today list (or empty) | Destination asserted; no preview notice |
| Record row | Open owned detail | Detail for that `eventId` | Correct id; other actors' events absent from list |
| Back (detail) | Return to list or prior stack | List/Collect restored | Back contract asserted |
| Collect `View record` | Open detail for saved receipt event | Matching detail | Ownership + id asserted |
| Retry (load failure) | Reload today list | Ready or empty | Failure then recovery asserted |
| `Sell` / `Collect` / `More` | Existing nav contracts | Unchanged destinations | More may remain notice until its checkpoint |

## Material states

| State | Behavior |
|---|---|
| Loading | Stable Cashier chrome + progress; no partial foreign rows |
| Empty today | Clear empty copy; nav remains |
| Ready list | Reverse-chronological own records for the business date |
| Detail | Cashier-safe fields only; no correct/void action |
| Load failure | Inline error + Retry |
| Not found / not owned | Calm denial; return to list |
| Session expired | Leave authenticated area via existing auth path |
| Access changed | Clear sensitive detail; route to an allowed destination |

## Native data boundary

Add a presentation-neutral `NativeActivityFacade`:

- require an authenticated session with `VIEW_OPERATIONAL`;
- resolve current business date and posting context the same way Sale/Collect do;
- list only events linked to the session actor for that business date;
- detail only when the same actor owns the event (and prefer same-day Activity list entries);
- project Cashier-safe summary fields from the event (and minimal related labels) without exposing `VIEW_FULL_FINANCIAL` ledger arrays;
- do not call or widen `query.eventHistory` for this screen.

## Out of scope

- More tab, blind cash count, Quick unlock management from Activity
- Corrections, voids, supervisor approval flows
- Owner/Manager/Admin/Operator Activity shells
- Launcher migration away from `HybridActivity`

## Verification

Native facade and Compose contract tests must prove list scoping, empty/ready/detail states, ownership rejection, Collect `View record` handoff, and navigation. Harness: `qaScreen -Pqa.screen=activity` after tags/inventory registration.
