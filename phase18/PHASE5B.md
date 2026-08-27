# Phase 5B — Transaction Detail

## Scope

Adds a read-only transaction detail surface to the existing Phase 5A transaction history.

## Behavior

Each history event is individually selectable. Selecting an event loads the event itself and its existing inventory and financial ledger movements from the canonical Room data.

The detail view shows:

- event ID and event type;
- occurred/created timestamps;
- channel, correction reference, and notes when present;
- persisted event values relevant to the event type;
- inventory movements with product names;
- financial movements with account names.

No new transaction table, accounting model, or business calculation is introduced.

## Business authority

The existing event records, inventory log, financial log, and domain services remain authoritative. The UI is read-only and does not modify balances or ledger state.

## Tests

`Phase5bTransactionDetailUiTest` records an existing Vendor Payment, selects that specific history entry, and verifies the recorded event details and financial ledger movements are shown.

## Verification

Run the complete regression suite in Android Studio, including all Phase 1–4G tests, `Phase5aTransactionHistoryUiTest`, `Phase5bTransactionDetailUiTest`, and `SpinnerWiringSmokeTest`.
