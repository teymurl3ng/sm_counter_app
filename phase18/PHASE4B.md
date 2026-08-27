# Phase 4B — Yogurt Production UI

Phase 4B adds the next UI vertical slice on top of the already-verified Phase 3 transaction engine.

## Scope

- Add measured Milk Liters Used input.
- Add measured Yogurt Kg Produced input.
- Save through the existing `YogurtProductionService`.
- Refresh canonical stock/dashboard values after a successful save.
- Show a clear user-facing error when validation or the transaction fails.
- Preserve atomic transaction behavior supplied by the existing domain service.

## Business rule boundary

The UI does not implement a yield formula. The domain service remains responsible for the transaction and uses the measured values directly, matching the core logic spec.

## Tests

`Phase4bProductionUiTest` covers:

1. successful production updates Room state and dashboard text;
2. insufficient milk produces a visible error and no event/stock change.
