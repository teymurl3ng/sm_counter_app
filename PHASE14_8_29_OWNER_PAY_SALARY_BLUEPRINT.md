# Phase 14.8.29 — Owner Work Pay salary blueprint

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-21

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

`AppPermission.SALARY_PAYMENT` is granted to Owner, Admin, and Manager — **not** Cashier. Even so, this slice uses content + Owner shell for consistency with Pay supplier / Record expense and future Manager hosts:

1. **Role-neutral content** — form, loading, failure, draft leave-guard presentation in `nativeui/salarypayment/`. No `OwnerSection` / foreign bottom nav.
2. **Role-owned shell** — `OwnerPaySalaryEntryScreen` wraps content in `OwnerScaffold(Work)`; hardware Back → Work home; `EntryLeaveGuard` on Back **and** bottom-nav exits.
3. **Foundation proof** — Owner chrome ✓, Cashier/Ops chrome ✗, Back → Work, session intact.

**Forbidden:** mounting another role’s full screen inside Owner. Manager/Admin Pay-salary host wrappers are **out of scope** this slice. Cashier never hosts salary payment.

## Connected (this slice)

- Owner Work **Pay salary** opens the Owner-hosted salary payment form.
- Load active employees + Cash/Bank payment accounts from `HybridReadModelService.transactionReferences`.
- Post via typed `OperationsBridgeCommandService.recordSalaryPayment(session, SalaryPaymentInput)`.
- Success receipt with **Pay another** and **Back** to Work.
- Dirty draft leave-guard on hardware Back and Owner bottom nav.

## Explicit preview / deferred

- Manager / Admin Pay-salary host shells
- Cash count and close

## Native data boundary

`NativeSalaryPaymentFacade(database, security)`:

- require `AppPermission.SALARY_PAYMENT`;
- map active `employees` + `paymentAccounts`; default payment account Cash when present;
- `record` converts paisa → amount and uses Collection-style `occurredAt = businessDate` (`LocalDate.toString()`);
- never call domain `SalaryPaymentService` directly from UI; never invent JSON bridge payloads.

Domain `SalaryPayment.kt` / `SalaryPaymentInput` remain read-only for this work.

## Accept gate

**AUTO-ACCEPTED.** Connected = Owner-hosted Pay salary form + post under CORE METHOD. Other role hosts deferred.
