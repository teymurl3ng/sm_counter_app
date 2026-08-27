# Phase 4C — Vendor Payment UI

## Scope

This phase adds only the Vendor Payment UI slice on top of the verified Phase 4B project.

## Flow

Vendor Payment screen → existing `VendorPaymentService` → atomic Room transaction → canonical account balances → dashboard refresh.

## Inputs

- Vendor
- Payment account (Cash / JazzCash / Bank)
- Amount
- Date is recorded as the current app date, matching the existing Phase 4 transaction UI pattern.

## Validation

- Vendor must be selected.
- Payment account must be selected.
- Amount must be greater than zero.
- Service/domain validation remains authoritative for transaction integrity.

## Tests

`Phase4cVendorPaymentUiTest` covers a successful vendor payment and invalid amount handling without changing previous phase tests.
