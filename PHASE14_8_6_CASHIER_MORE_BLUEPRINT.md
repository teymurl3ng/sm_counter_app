# Phase 14.8.6 — Cashier More blueprint

Status: **ACCEPTED FOR NATIVE IMPLEMENTATION** (compressed gate; Counter Ledger already selected)

Date: 2026-09-20

## Where this checkpoint starts

Native Auth, Sale, Collect, and Activity are connected in the debug `NativePreviewActivity` harness with shared green Cashier chrome. `More` is still a preview notice.

This checkpoint owns the Cashier **More** destination as personal utilities + blind cash count. It does not start Expense, Deliveries, day-close UI, Owner/Manager More, or launcher migration.

## What the existing product already guarantees

- Cashier has `CASH_COUNT_AND_CLOSE` and may submit a physical cash count for the **Cash** account on the open business date.
- Domain `PhysicalCashCountService.record` returns expected cash and variance. Cashier native More must **not** show those values (blind count).
- Personal Quick unlock (PIN / phone security) already exists for sign-in; More only manages enrollment/removal for the signed-in account on this phone.
- Leaving the authenticated area already invalidates the native session (`leaveAuthenticatedArea` / switch account).
- Cashier More must not expose products, suppliers, employees, full financial reports, settings, user admin, security audit, or backup.

## Normal path

1. Open `More` from Cashier navigation.
2. Show the shared green Cashier header (`More`) with identity, role, business date, and Open/Closed.
3. Present a short menu of Cashier-safe actions (not a leadership tile grid):
   - **Count cash** — enter today’s blind drawer count
   - **Personal security** — Quick unlock status and manage PIN / phone security for this account
   - **Switch account** — leave this session and return to account selection
   - **Sign out** — leave this session and return to Welcome
4. **Count cash:** enter a non-negative PKR amount for Cash only; optional note; save once. Success confirms the count was saved **without** expected cash or variance. Re-count today is allowed when the domain already allows it.
5. **Personal security:** show whether Quick PIN and/or phone security are enrolled for this user on this device; enroll or remove with the existing Quick unlock services and confirmation-in-place for remove.
6. **Switch account** / **Sign out:** clear authenticated Cashier state and route to Access account picker or Welcome respectively.

## Control contract

| Control | Required workflow | Visible result | Native test obligation |
|---|---|---|---|
| `More` nav | Open Cashier More menu | Menu + shared header; no preview notice | Destination asserted |
| `Count cash` | Open blind count entry | Amount field; Cash destination fixed/labeled | Opens entry; no expected/variance text |
| `Record count` | Save one blind count | Success without expected/variance | One command; Cashier-safe projection |
| `Personal security` | Open Quick unlock management | Status + enroll/remove actions | Status matches device enrollment |
| Remove Quick unlock | Confirm then remove | Confirmation then cleared method | No silent removal |
| `Switch account` | End session → account picker | Access method/account flow | Session cleared |
| `Sign out` | End session → Welcome | Welcome screen | Session cleared |
| `Sell` / `Collect` / `Activity` | Existing nav | Unchanged destinations | Nav contracts unchanged |

## Material states

| State | Behavior |
|---|---|
| Loading | Shared chrome + progress |
| Ready menu | Four Cashier actions; Closed day disables count posting |
| Count entry | Amount + optional note; record locks while saving |
| Count success | Calm confirmation; no expected/variance |
| Count failure | Draft preserved; inline error + retry |
| Security ready | Current Quick unlock methods for this account |
| Closed date | Count disabled with reopen guidance |
| Session expired | Leave authenticated area via existing auth path |

## Native data boundary

Add a presentation-neutral `NativeMoreFacade` (and thin ViewModel):

- require authenticated session;
- load Cash account + business-date posting state for count entry;
- record count through existing reconciliation/command path with `CASH_COUNT_AND_CLOSE`, projecting only Cashier-safe fields (count id, counted amount, business date) — **strip expected and variance**;
- expose Quick unlock status/enroll/remove via existing `NativeQuickUnlockFacade` / `QuickUnlockService` without changing RolePermissions;
- switch/sign-out only clear session and navigate — no domain permission changes.

## Out of scope

- Expense, Deliveries, day close / reopen UI (even though Cashier has `CASH_COUNT_AND_CLOSE`)
- Showing expected cash, variance, or shop-wide reconciliation archive
- Changing passphrase from More (full passphrase change remains a later personal-security deepening if needed)
- Owner/Admin/Manager/Operator More shells
- Launcher migration away from `HybridActivity`

## Verification

Facade + Compose contract tests must prove More destination, blind count (no expected/variance leak), security status surface, switch account, and sign out. Harness: `qaScreen -Pqa.screen=more` after tags/inventory registration. Prove with `qaFast` and device run on A06.

## Accept gate

Reply **accept** (or note changes) before Phase B Kotlin/Compose.
