# Phase 4 start: calculation foundation and data contract

15 October 2026. Baseline `08d0e28149f02acb4b11778d996618c21825cb4b`. **Phase 4 is in progress, not complete.** The owner authorized implementation after reviewing the scoped approach. No further business-input question is pending for the proposed category bands/monthly semantics.

- [x] ✅ ~~Extend the existing reconciled FIFO result with remaining sale lots.~~
- [x] ✅ ~~Implement weekly/monthly/custom term values and term-aware current status.~~
- [x] ✅ ~~Implement deterministic pure SET/CLEAR flag-history projection.~~
- [x] ✅ ~~Check the DEV RPC boundary read-only and freeze the next data contract.~~
- [x] ✅ ~~Run only affected calculation tests and reconcile the progress record.~~
- [ ] Persist metadata, permissions, migrations, versioned transport and backup support.
- [ ] Connect existing customer/Sale/history screens.
- [ ] Complete affected persistence, role, sync/restore and two-phone acceptance.

## What is implemented

The established aging calculator now additionally returns reconciled unpaid lots; the existing public age method still returns the same result. The status calculation evaluates each remaining sale's own term, preserving oldest-sale FIFO allocation rather than switching payment allocation to deadline order. Calendar-month deadlines clamp to month end and differ from 30-day deadlines. Unknown opening history or agreement coverage produces Needs review while retaining any known overdue amount. Terms and status are typed calculations; no stored derived balance or score was added.

Recorded agreements reference the original sale/customer/revision; a current preference is not an input that can rewrite historical terms. Missing evidence is distinct from explicitly identified legacy weekly debt. Manual flag projection keeps SET/CLEAR history independent of debt status. Clearing one reviewed SET cannot erase an unseen concurrent SET; duplicates of an identical identity/body are harmless, changed bodies are refused, and missing dependencies are explicitly incomplete.

These functions are not yet connected to customer screens, saved records or transport. They do not enforce Owner permissions by themselves. Actual persisted authorization and agreement-at-posting behavior belong to the next data checkpoint; no claim of verified two-phone convergence is made.

## Verification

**17 focused JVM tests pass:** 11 new payment/status/flag tests plus six existing aging/sale-amount tests. Kotlin production/test compilation also passed as part of that invocation. [Exact methods and source hashes](CALCULATION_VERIFICATION.json). Cases include every specified weekly boundary, leap/non-leap month end, mixed-term unpaid lots, partial/full payment/advance, correction ancestry, sub-paisa dust, missing/wrong agreement, unsupported version, invalid allocations, duplicate actions and out-of-order/concurrent SET/CLEAR. No full suite, phone check or ordinary-app installation ran.

One read-only DEV query fetched only the two existing sync RPC definitions. [Scope and definition hashes](DEV_CONTRACT_BASELINE.json). The current allowlist rejects new record types; pull has no contract-version parameter and old clients skip unknown types. The [frozen data contract](DATA_CONTRACT.md) therefore keeps activation disabled pending an explicit compatible protocol, accepted-revision checks, atomic sale+agreement payload and catch-up/backup support. Raw definitions remain local/ignored. No PROD inspection/parity, database migration, schema bump or server mutation is claimed.

## Next bounded checkpoint

Implement exactly the storage/permission/transport/backup contract before enabling controls: Owner-authorized accepted customer terms with revision-conflict review; immutable root-sale agreements carried atomically with event envelopes; immutable manual review actions; explicit legacy classification and versioned catch-up/rollout. Use isolated synthetic DEV fixtures first. Do not open general device identity, automated sending or another design audit.

The six-phase plan remains: 1 handover/delivery pending; 2 sync attention done; 3 Credit basics done; 4 in progress; 5 statements/manual reminders pending; 6 owner automation/automatic-SMS decision pending.
