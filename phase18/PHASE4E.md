# Phase 4E — Credit Collection UI

Built on the verified Phase 4D checkpoint supplied by the user.

Scope:
- Credit Collection UI only.
- Customer selection.
- Cash/JazzCash/Bank selection.
- Collection amount.
- Synchronous validation errors.
- Existing CreditCollectionService for the business transaction.
- Atomic transaction behavior remains in the domain service.
- Dashboard now also displays canonical customer balances.
- Customer and payment-account spinners are populated by the same initial data loaders used by the existing UI, with a small registration list for customer-dependent spinners.

Tests:
- Phase4eCreditCollectionUiTest success path.
- Phase4eCreditCollectionUiTest invalid amount path.
- Existing SpinnerWiringSmokeTest updated so its readiness condition waits for the Phase 4E spinners as well.

No SQL, Room schema, domain service, correction logic, or prior transaction logic was changed.
