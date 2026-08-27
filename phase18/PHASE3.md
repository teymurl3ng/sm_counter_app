# Milk Shop Android — Phase 3

Phase 3 implements the append-only correction workflow defined by Core Logic Spec §11.

## Blueprint changes required for correction completeness

The corrected business model adds these event-header fields:

- `amount` for Vendor Payment, Credit Collection, Expense, and Salary Payment.
- `milk_liters_used` and `yogurt_kg_produced` for Production.

A database migration from schema version 1 to version 2 is included.

## Correction rules

- The original event is never edited or deleted.
- A correction is a new event referencing the event being corrected.
- The correction event stores the corrected/restated truth.
- Inventory and financial rows attached to the correction store only the delta needed to reconcile the prior state to that truth.
- Correction event type must match the referenced event type.
- Corrections preserve the original product/account identities represented by the original ledger rows.

## Implemented correction types

- Vendor Purchase
- Yogurt Production
- Sale
- Vendor Payment
- Credit Collection
- Expense
- Salary Payment

## Tests

`Phase3CorrectionTest` covers corrected-truth storage, delta movements, canonical resulting balances/stock, event-type protection, and the new event-header amount fields.
