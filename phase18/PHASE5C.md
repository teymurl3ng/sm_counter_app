# Phase 5C — Correction UI

## Scope

Adds a correction editor to the existing read-only transaction detail surface. The UI constructs only the existing `CorrectionInput` types and delegates correction behavior to `CorrectionService`.

## Supported correction types

- Milk purchase
- Yogurt production
- Sale
- Vendor payment
- Credit collection
- Expense
- Salary payment

The UI preserves the existing correction service's account/product attribution rules and does not expose fields that the domain correction model does not support (for example, changing the payment destination account during a monetary correction).

The correction event keeps the original event's `occurredAt` value unless the existing domain model supplies a dedicated correction-date requirement; no new date semantics are introduced here.

## Safety

The original event is never edited. Successful correction creates a new correction event through the existing atomic `CorrectionService` transaction, then refreshes the dashboard and history from canonical state.

## Verification

Run the full regression suite, including all Phase 1–4G tests, Phase 5A, Phase 5B, Phase 5C, and the spinner smoke test.
