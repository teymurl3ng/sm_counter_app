# Phase 14.8.30 — Owner Work Cash count and close blueprint

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-21

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

`AppPermission.CASH_COUNT_AND_CLOSE` is shared with Cashier (blind Count cash in Cashier More). Owner must **not** mount `CashierMoreScreen` or any Cashier shell:

1. **Role-neutral content** — form, loading, failure, draft leave-guard presentation in `nativeui/cashclose/`. No `OwnerSection` / `CashierSection` / foreign bottom nav.
2. **Role-owned shell** — `OwnerCashCloseEntryScreen` wraps content in `OwnerScaffold(Work)`; hardware Back → Work home; `EntryLeaveGuard` on Back **and** bottom-nav exits.
3. **Foundation proof** — Owner chrome ✓, Cashier/Ops chrome ✗, Back → Work, session intact.

**Forbidden:** mounting Cashier More (or translating Cashier nav) inside Owner. Manager/Admin cash-close hosts are **out of scope** this slice.

## Connected (this slice)

- Owner Work **Cash count and close** opens the Owner-hosted count + close form.
- Load business date, posting open/closed, Cash drawer account, and expected cash from existing reads (`accountDao` Cash account, `dailyCloseDao.isPostingClosed`, `readModelDao.accountBalance` / latest count via the same canonical balance source used by reconciliation).
- Record count via typed `ReconciliationBridgeCommandService.recordCount` → domain `PhysicalCashCountService`.
- After count: show counted / expected / variance (Owner sees expected — **not** Cashier blind-only UX).
- Close day via `ReconciliationBridgeCommandService.closeDate` → domain `DailyCloseService`; require variance reason when variance ≠ 0 (match domain/bridge rules).
- Success receipt → Back to Work.
- Dirty draft leave-guard on hardware Back and Owner bottom nav.
- If already closed: closed banner; counting blocked per bridge/domain; **reopen write UI deferred** (preview notice only).

## Explicit preview / deferred

- Reopen day write UI (and passphrase re-auth flow)
- Manager / Admin cash-close host shells
- Cashier host remains blind Count cash only (no day close in Cashier More)

## Native data boundary

`NativeCashCloseFacade(database, security)`:

- require `AppPermission.CASH_COUNT_AND_CLOSE`;
- resolve Cash/Bank account named `Cash`;
- expose `expectedCashPaisa` from account balance and/or latest physical count for the date — never invent formulas;
- `recordCount` / `closeDay` go through `ReconciliationBridgeCommandService` only;
- never call domain `PhysicalCashCountService` / `DailyCloseService` directly from UI; never invent JSON bridge payloads;
- never implement reopen writes in this slice.

Domain `PhysicalCashCount.kt` / `DailyClose.kt` remain read-only for this work.

## Accept gate

**AUTO-ACCEPTED.** Connected = Owner-hosted Count + Close day under CORE METHOD. Reopen deferred.
