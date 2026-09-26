# Phase 14.8.55c — native create-delivery and archive flow

Date: 2026-09-26
Branch: `main` (follows 14.8.55b, commit `05fcd35`; seeder hardening + matrix re-run `6b747e0`)

## Outcome

The gap the owner found by hand-testing — deliveries could not be created anywhere in the native app
(the only creator was the preview sample-data seeder, which my 14.8.55 matrix wrongly counted as a
native caller) — is closed. Deliveries can now be created, and finished deliveries archived, entirely
in the native UI.

## What was built

- `NativeDeliveriesFacade`: `createDelivery` (quantity/positive + customer/product required at the
  boundary; active-customer, active-product, open-day, and audit invariants enforced by the shared
  bridge command service), `archiveDelivery`, and picker `references` (active customers,
  non-archived products) on the load context. `NativeDeliveryItem` gained a `pending` flag.
- `NativeDeliveriesViewModel`: draft state (customer/product/quantity/route/notes) + `createDelivery`
  and `archiveDelivery` actions on the established retention/error pattern.
- `DeliveriesScreen`: "New delivery" card at the top — customer/product pickers (dialog list with
  picker tags), numeric quantity, optional route/notes, Record delivery button; "Remove from active
  list" archive button on finished rows in the link section; "Delivery recorded (#N)" success banner.
  Scroll column tagged for test scrolling.

## Ownership matrix correction

The 14.8.55 matrix's identifier matching was too loose: `createDelivery`/`archiveDelivery` "passed"
via generic `create(`/`archive(` matches and the preview seeder. The matrix was re-run with
`application/preview/**` excluded; the manual confirmation stands: these two were genuine gaps, now
implemented. `transitionDelivery`, `linkDeliverySale`, and the rest remain proven.

## Also in this commit

- Preview seeder hardening (`6b747e0`): shared `Mutex` — two role hosts starting together raced
  check-then-create fixtures and surfaced a one-time UNIQUE constraint error.
- Records timestamps render in device local time (`NativeFormatters.formatRecordTimestamp`,
  `bf5ee90`); dashboard attention Review now opens Cash count & close (`4989335`).

## Verification

- `qaFast`: PASS (`990` inventory rows, `0` untested/orphans), `CHECK_UI_ONLY: PASS`; `lintDebug` +
  `assembleDebug` PASS
- Device (SM-A065F): `NativeDeliveriesScreenContractTest` **8/8 PASS** — includes new
  `createDeliveryRecordsPhysicalLogisticsPlan`, `createDeliveryRequiresCustomerProductQuantity`
  (button disabled until customer+product+positive quantity), and
  `archiveDeliveryRemovesFromActiveList`; scroll-to-node interactions mirror real user scrolling.
