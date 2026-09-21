# Phase 14.8.33 — Owner Work Reopen day blueprint

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-22

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

**Reopen day is Owner-gated write depth inside the existing Cash count and close surface** — not a new Work tile and not a foreign-role host:

1. **Role-neutral content** — closed-day reopen form (reason, full passphrase, Confirm Reopen), loading, failure, and draft leave-guard stay in `nativeui/cashclose/`. No `OwnerSection` / `CashierSection` / foreign bottom nav enums in content.
2. **Role-owned shell** — continue using `OwnerCashCloseEntryScreen` + `OwnerScaffold(Work)`; hardware Back → Work home; `EntryLeaveGuard` on Back **and** bottom-nav exits.
3. **Foundation proof** — existing Owner Cash close Foundation (host chrome ✓, foreign chrome ✗, Back → Work, session intact) still covers the hosted entry; reopen is depth on that same entry.

**Forbidden:** mounting another role’s screen, inventing reopen without recent strong auth, calling JSON bridge from UI, or changing domain DailyClose formulas.

## Connected (this slice)

- When Cash close shows a **closed** business date, replace the deferred reopen notice with a real Owner reopen form:
  - Reason (required)
  - Current full passphrase / recent strong auth (required — Quick PIN cannot authorize)
  - Confirm Reopen button
- On success: refresh context to `postingOpen=true`; wipe passphrase `CharArray`s after use; inline success / error feedback.
- Permission: `AppPermission.REOPEN_DAY` (Owner/Admin only — Owner shell already).

## Native data boundary

`NativeCashCloseFacade.reopenDay(session, reason, passphrase)`:

- require closed date on load path; reopen goes through `security.reauthenticate` then typed `ReconciliationBridgeCommandService.reopenDate(VerifiedDailyReopenInput(...))`;
- match domain `DailyCloseService.reopenAuthenticated` recent-auth rules (full passphrase only);
- never call JSON bridge from UI; never invent reopen without passphrase re-auth;
- clear / null out `reopenDeferredNotice` now that reopen write UI is available.

Domain `DailyClose.kt` remains read-only for this work.

## Explicit preview / deferred

- Manager / Admin cash-close host shells
- Cashier host remains blind Count cash only

## Accept gate

**AUTO-ACCEPTED.** Connected = Owner-hosted Reopen day as depth inside Cash count and close under CORE METHOD.
