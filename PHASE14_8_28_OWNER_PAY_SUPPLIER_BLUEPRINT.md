# Phase 14.8.28 — Owner Work Pay supplier blueprint

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-21

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

`AppPermission.SUPPLIER_PAYMENT` is granted to Owner, Admin, and Manager — **not** Cashier. Even so, this slice uses content + Owner shell for consistency with Record expense and future Manager hosts:

1. **Role-neutral content** — form, loading, failure, draft leave-guard presentation in `nativeui/vendorpayment/`. No `OwnerSection` / foreign bottom nav.
2. **Role-owned shell** — `OwnerPaySupplierEntryScreen` wraps content in `OwnerScaffold(Work)`; hardware Back → Work home; `EntryLeaveGuard` on Back **and** bottom-nav exits.
3. **Foundation proof** — Owner chrome ✓, Cashier/Ops chrome ✗, Back → Work, session intact.

**Forbidden:** mounting another role’s full screen inside Owner. Cashier/Manager Pay-supplier host wrappers are **out of scope** this slice.

## Connected (this slice)

- Owner Work **Pay supplier** opens the Owner-hosted vendor payment form.
- Load active vendors + Cash/Bank payment accounts from `HybridReadModelService.transactionReferences`.
- Post via typed `OperationsBridgeCommandService.recordVendorPayment(session, VendorPaymentInput)`.
- Success receipt with **Pay another** and **Back** to Work.
- Dirty draft leave-guard on hardware Back and Owner bottom nav.

## Explicit preview / deferred

- Manager / Admin Pay-supplier host shells
- Pay salary / Cash count and close

## Native data boundary

`NativeVendorPaymentFacade(database, security)`:

- require `AppPermission.SUPPLIER_PAYMENT`;
- map active `vendors` + `paymentAccounts`; default payment account Cash when present;
- `record` converts paisa → amount and uses Collection-style `occurredAt = businessDate` (`LocalDate.toString()`);
- never call domain `VendorPaymentService` directly from UI; never invent JSON bridge payloads.

Domain `VendorPayment.kt` / `VendorPaymentInput` remain read-only for this work.

## Accept gate

**AUTO-ACCEPTED.** Connected = Owner-hosted Pay supplier form + post under CORE METHOD. Other role hosts deferred.
