# Phase 5A — Transaction History

## Scope

Adds the first Phase 5 vertical slice: a read-only transaction history on top of the verified Phase 4G project.

## Behavior

The history reads directly from the existing `events` table through `EventDao` and displays all recorded events in reverse chronological order (`occurred_at DESC, event_id DESC`).

Each event shows:

- event ID;
- human-readable event type where the type is already defined by the existing domain services;
- occurred date;
- relevant persisted event values;
- correction reference when `corrects_event_id` is present;
- note when one exists.

No new accounting model, event table, or history table is introduced.

The history is loaded on initial screen load and refreshed after each successful transaction save so the UI reflects the same canonical event history already persisted by the domain services.

## Business authority

The existing event records and domain services remain authoritative. The UI performs no transaction calculation and does not modify ledger state.

## Tests

`Phase5aTransactionHistoryUiTest` seeds two existing domain events, verifies both appear in the history, verifies reverse chronological ordering, and checks representative persisted values are displayed.

## Verification

Run the complete regression suite in Android Studio, including all Phase 1–4G tests, `Phase5aTransactionHistoryUiTest`, and `SpinnerWiringSmokeTest`.
