# Milk Shop Android — Phase 2

Phase 2 extends the Phase 1 Room foundation with the remaining ordinary business transactions from the frozen core specification.

## Implemented

- Vendor Payment
- Yogurt Production
- Sale
- Credit Collection
- Expense
- Salary Payment

Each operation is implemented as a separate domain service and uses one Room write transaction for its complete multi-row effect.

## Explicitly deferred in Phase 2

Corrections were intentionally deferred to Phase 3. Phase 3 updates the event header so every correction type can store corrected truth consistently, then implements the append-only delta workflow. See `PHASE3.md`.

## Tests

`Phase2TransactionTest` covers the implemented operations and their canonical stock/balance effects, plus invalid sale input.
