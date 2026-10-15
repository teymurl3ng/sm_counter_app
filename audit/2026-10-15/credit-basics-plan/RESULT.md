# Phase 3: Credit basics — result

15 October 2026. Implemented against the pinned [baseline, research and judgment](PLAN.md). Scope is phase 3/L1 of the [six-phase plan](../../../NEXT_WORK_PLAN_2026-10-15.md), with local source/isolated QA acceptance. It is not a new ordinary signed release or whole-app acceptance.

- [x] ✅ ~~Baseline, targeted research and independent judgment.~~
- [x] ✅ ~~Read-only FIFO calculation and independent examples.~~
- [x] ✅ ~~Customer search and selected-customer/Owner detail summaries.~~
- [x] ✅ ~~Surcharge-inclusive preview and advisory warning with transactional revalidation.~~
- [x] ✅ ~~Affected checks, synthetic A06 visual inspection and plan reconciliation.~~

## What changed

The shared Cashier/Owner/Manager Sale picker searches active customers by name or phone digits. Filtering and cancellation preserve selection; phone/route information and duplicate-name account references help identify the right customer. Lazy scrolling remains. The selected Credit customer shows current debt or advance, projected balance, configured limit and unpaid age, explicitly based on this phone's records. Owner customer detail also shows age.

Age is computed from existing ledger movements and correction ancestry. Collections/advances offset the oldest obligations; carried-over opening debt has unknown age and is consumed first. Corrections adjust the original family rather than starting a new debt age. Stable root UIDs order same-day obligations. Raw decimal allocations are reconciled against the existing aggregate-then-round balance; unreliable history yields unavailable age without inventing transactions.

Sale draft totals now use the same merchandise-plus-surcharge arithmetic as posting. A null limit is unset, zero is an explicit advisory limit, and only a strictly greater projected balance triggers the limit warning. Back writes nothing. Continue rechecks customer eligibility, pricing, balance, limit and draft inside the existing write transaction; changed snapshots require review again when a warning is still necessary. Failed age coverage does not disable a valid limit check. Unavailable balance/limit reads require explicit confirmation and cannot override permissions, a closed day, an inactive customer or invalid input.

The existing save guard, receipt and delivery follow-up remain. Superseded preview/session results cannot publish into a newer draft/session. Saving cancels preview loading; a warning supplies its fresh summary instead of leaving a stuck loading label. Header/navigation bounds remain stable during summary changes in the affected fixtures. Credit text uses explicit theme colors.

## Verification and limits

**Six calculation tests and 28 unique affected A06 checks pass.** [First device results](first-results.json), [repaired fixtures and Owner detail](repaired-results.json), [final UI checks](visual-final-results.json), [final state checks](state-final-results.json). The runner pins the exact methods; no full suite ran. Tests use only `com.saaf.milkshop.qa.tests` and local/in-memory synthetic records. The runner verifies both APK package IDs and clears only this disposable QA package. Notifications were denied for it. No ordinary shop data, customer contact or cloud write belongs to this run.

The first device run passed 21/26. One synthetic event violated the database event-type guard, two captures assumed a single Compose root while an Android dialog was open, the dialog IME dismissal used the underlying Activity window, and one amount assertion matched three legitimate labels. The repaired age fixture uses a legal future-dated sale; the separate pure calculator still covers unsupported movements. Dialog Done, whole-window settled captures and exact amount-count assertions corrected the harness without relaxing production guards. Those five checks plus the two planned Owner detail checks passed. Only five UI methods were repeated after duplicate-name references/theme colors; only four state methods were repeated after the loading-state fix. Build-time positional-call compatibility and one source encoding byte were corrected without editing unrelated screens.

Final light/dark synthetic captures were inspected locally and remain ignored. The body fixtures use 100%/200% Compose text scale; the separate Android dialog uses its platform window density, so this is not a system-wide 200% font-setting or uncoached staff acceptance claim. The scrollable warning body and visible Back/Continue controls were exercised. Source/test hashes and the final QA APK are recorded in [verification](VERIFICATION.json) and [source manifest](SOURCE_MANIFEST.json). Earlier passing checks for unchanged calculation/facade/ledger/Owner paths were not repeated after presentation-only or ViewModel-only changes.

No schema/version change, server migration, new stored balance/score, release version bump or ordinary-app installation was made. A06's ordinary signed shop app retains its prior accepted version. Feature rollout/release delivery remains distinct from this isolated QA acceptance.

## Stop and next scope

Phase 3/L1 is complete. Phase 2 remains credited. Phase 1 still contains ordinary delivery and human/handover/custody participation. Phase 4 is next: first pin weekly category rules, overrides/history, research and judgment before implementation. Phases 5–6 remain pending. No category cutoff, due date, reminder, statement, automatic SMS, new identity system or unrelated audit was added here.
