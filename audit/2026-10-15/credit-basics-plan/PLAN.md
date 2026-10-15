# Phase 3: Credit basics — pinned implementation brief

Date: 15 October 2026. Source baseline: `ddd481dbf04b9fe353a9bb86388e1dcd4fdbdedd`. Status: **implemented and accepted at the bounded local QA scope**. [Result](RESULT.md). Parent: [six-phase plan](../../../NEXT_WORK_PLAN_2026-10-15.md). Original commitment: [Phase 14.9](../../../PHASE14_9_CREDIT_REMINDERS_BLUEPRINT.md).

## One outcome, three bounded parts

Staff can find a credit customer, see their current debt and its age, and deliberately continue after an advisory limit warning. This phase contains customer search, a read-only credit summary/FIFO calculation, and a safe warning before the existing sale command. It does not introduce categories, flags, reminders, statements or a new credit ledger.

The owner confirmed weekly settlement. That is retained for phase 4's category/reminder policy; it does not justify labelling seven-day-old debt overdue, choosing a settlement weekday or inventing a grace period in this phase.

## Verified baseline

| Existing behavior | Source and consequence |
|---|---|
| Credit sales add positive movements to the customer account; collections subtract movements. Sale financial amount includes optional delivery surcharge. | `domain/Sale.kt`, `domain/CreditCollection.kt`. Reuse these facts and signs; no new postings or payment-allocation table. |
| Sale customer context contains only account ID/name; customer balance/limit are absent. The picker is a name-only LazyColumn without search. | `application/sale/NativeSaleFacade.kt`, `nativeui/sale/CashierSaleScreen.kt`. Actual gaps, not missing customer accounts. |
| Collection already has name/phone search and selected-customer balance loading. | `nativeui/collection/CashierCollectionScreen.kt`, `application/collection/NativeCollectionFacade.kt`. Reuse the interaction pattern; do not redesign Collection. |
| Customer profile already has phone, area/route, nullable credit limit and active status. Zero is allowed; null is distinct. Existing read DTOs expose balance and limit. | `domain/Profiles.kt`, `application/readmodels/HybridIdentityReadService.kt`. Read existing fields rather than add settings/schema. Define null/zero behavior explicitly. |
| Transaction references filter inactive customers and enforce existing transaction permissions. | `application/readmodels/HybridOperationalReadService.kt`. Keep active eligibility and existing Sale roles. |
| Owner customer detail already shows balance and configured limit; it has no aging summary. | `application/owner/NativeOwnerCustomersFacade.kt`, `nativeui/owner/OwnerCustomersScreen.kt`. Add a small read-only age explanation there; do not add a customer-history screen. |
| The ordinary account ledger contains original movements plus correction deltas; a correction can have no financial row when its amount does not change. | `domain/AccountLedger.kt`, `data/AccountLedgerReadModels.kt`, `data/Daos.kt`, `domain/Correction.kt`. Dropping superseded rows or treating every positive delta as a new sale would be wrong. Ancestor/root metadata must include zero-delta corrections when needed. |
| Current native sale/collection corrections preserve their original business date. Opening balances/corrections are separate opening-state events with delta movements. | `application/owner/NativeOwnerRecordsFacade.kt`, `domain/OpeningState.kt`. Corrections adjust their original obligation/payment family; opening debt has no demonstrated original sale age. |
| Ledger balance rounds after summing raw movements, using the established decimal HALF_UP SQL boundary. | `data/CanonicalMoneySql.kt`. Do not round every movement into paisa before allocating or add two already-rounded amounts to predict a new balance. |
| Sale's displayed draft total currently uses merchandise quantity/value without surcharge; the domain posts merchandise plus surcharge. | `CashierSaleScreen.kt` draft/review dock versus `SaleService.record`. A new projected debt must use the real posting total. Correct this narrowly related shared Sale preview discrepancy, not unrelated prices/reports. This is source-confirmed; no new device reproduction was run here. |
| The facade posts inside one transaction; the ViewModel has saving/session guards and retains receipt/delivery follow-up. | `NativeSaleFacade.kt`, `NativeSaleViewModel.kt`. Place warning revalidation before the existing command, preserving those protections. |

Paths are relative to `phase18/app/src/main/java/com/saaf/milkshop/`. No real account or shop data was read in this source-only baseline.

## Research and adaptation

- [Odoo 19 POS customer credit](https://www.odoo.com/documentation/19.0/applications/sales/point_of_sale/payment_methods/customer_credit.html) documents customer deposits, later settlement and a credit-limit warning that permits the sale to proceed. Adapt the visible advisory warning, not its full invoicing/accounting setup. Its official page text was available through the search result; direct opening failed, so no live Odoo installation is claimed.
- [Microsoft Dynamics GP payment application](https://learn.microsoft.com/en-us/dynamics-gp/financials/receivables-management-part2-transaction-entry#lockbox-apply-methods) documents multiple allocation choices, including oldest document date. FIFO is an established option and our recorded owner choice, not a universal rule that every business must use. Our app has no due dates; use sale business date, not a fabricated due date.
- [Microsoft customer aging](https://learn.microsoft.com/ro-ro/dynamics365/finance/accounts-receivable/customer-aging) distinguishes aging date bases. Our simpler display must say age since recorded sale, not days overdue. Weekly lateness policy belongs to phase 4.
- [Android Compose search](https://developer.android.com/develop/ui/compose/components/search-bar) provides state/query/filter patterns. Adapt those to the existing customer dialog/Collection behavior rather than add a full-screen search destination.

These sources support recognizable patterns. They do not certify our formulas, establish local accounting compliance, or supply the missing weekly policy. The fixed example oracle and current ledger implementation remain necessary. No broad market survey or new provider research is needed for phase 3.

## Judgment

The [independent-judgment hook](hook-output.json) is a reminder, not external code validation. The selected design is computed/read-only aging plus a non-blocking warning at the current Sale boundary.

Rejected alternatives: infer aging from the net balance alone; remove all superseded movements; start a fresh age for a correction delta; assume opening debt began on the app setup date; store a second authoritative balance/score; block credit automatically; or add another navigation screen. Each either loses existing truth or exceeds the recorded commitment.

## Pinned calculation and warning rules

1. **Identity and scope:** stable customer account identity, existing permission checks, active customers for a new sale. Current local records are the basis. Display that limitation in the summary; no claim that an offline phone has the latest shop-wide balance.
2. **Balance remains authoritative:** use the existing canonical balance boundary. Positive balance is debt; negative balance is customer advance; zero is settled. Show an advance positively as an advance, not negative debt.
3. **Read-only FIFO:** reconstruct effective sale/collection/opening families using event correction ancestry and the sum of their financial movements for this account. Preserve original rows; a correction updates its family, not a new debt. Missing ancestors, cycles, inconsistent account/type/date or unsupported movements produce incomplete/unavailable age, not invented allocations. Do not build a new correction engine or mutate history.
4. **Allocation:** for the current as-of business date, apply available collection/advance credit to outstanding positive obligations oldest first. Positive opening debt is an explicit unknown-age carried-over obligation, consumed before dated app sales. Remaining dated sales sort by business date then stable root event UID; device-local integer IDs are not a cross-phone tie-break. Any excess credit remains advance. Same-day ordering does not imply time-of-day precision.
5. **Opening/unknown history:** retain original opening debt as age unknown even though its entry date exists. Do not call its age zero. Show known unpaid sale age separately if carried-over debt remains. For an unreliable family or date, preserve authoritative balance and say age unavailable; do not silently exclude money or label a customer Good/Late.
6. **Dates:** use the trusted business date, not phone wall-clock arithmetic or a seven-day deadline. Phase 3 has no historical-report date selector. An as-of parameter is for deterministic calculation/testing. Future-dated or inconsistent records must not receive negative age; flag incomplete coverage rather than falsely reconcile an as-of subset to the all-record current balance. Unknown dates never get guessed from the first ten characters of arbitrary timestamps.
7. **Rounding:** allocate raw decimal amounts without pre-rounding individual ledger movements. Quantize the final displayed allocations consistently (cumulative HALF_UP differences in FIFO order) so a sub-paisa residual is not falsely called the oldest visible debt. Check their net against the established canonical balance. If they disagree, withhold the age breakdown and expose a retryable unavailable explanation; do not repair ledger values or create a balancing transaction. Add explicit half-paisa and cancellation tests.
8. **Sale total:** preview the same merchandise-plus-surcharge total as the authoritative sale calculation, including quantity/amount mode and wholesale rate. Extract/reuse a narrowly shared typed amount calculation if needed; no pricing policy change. The existing review dock, credit preview and warning must agree with the eventual receipt. Existing Paid now previews receive the same bounded correction because they share the dock.
9. **Projected balance:** calculate through the existing aggregate-then-round policy with the proposed raw sale movement. Do not simply add rounded displayed current debt and a rounded sale price. Keep advances in that signed calculation, then present debt versus advance clearly.
10. **Limit:** null means no configured limit; zero is an explicit zero advisory limit, not unlimited. Warn only when projected positive debt is strictly greater than the configured limit. Exactly equal is not over. A customer already over the limit still receives a warning for another credit sale. Invalid/nonfinite profile values are unavailable data, not unlimited credit.
11. **Warn, never automatically block:** on Record, re-read the selected customer, eligibility, balance, limit and sale amounts before any mutation. If over limit, show one in-place confirmation with Current debt/advance, This sale, After sale and Limit; actions Back and Record credit sale. Cancellation leaves the draft and creates no event. No extra account screen, mandatory owner approval, new role permission or delivery block.
12. **Stale confirmation:** the acknowledgment belongs only to the reviewed customer, date, draft/pricing and balance/limit snapshot. Recheck within the posting transaction. If material inputs changed, return a refreshed warning before any write. Guard duplicate taps and session/customer switches. A transient acknowledgment is not a reusable bypass flag or a stored credit authorization.
13. **Unavailable data:** age failure alone must not block an otherwise valid sale or hide a valid balance/limit warning. If balance/limit cannot be read, use an explicit credit-check-unavailable confirmation to continue the otherwise valid sale, rather than pretending the limit is unset. Existing security/closed-day/inactive-customer/invalid-input failures remain real failures and cannot be overridden by this confirmation.

## UI scope

- **Existing Sale picker:** name/phone search, trimmed case-insensitive name matching and normalized phone digits; existing active-customer eligibility and stable selection. Filter changes do not silently change the selected customer. Keep lazy scrolling, clear/no-match/empty-list/cancel behavior. Show phone/area when available to distinguish duplicate names; if still ambiguous, a small customer account reference is an explicit identification fallback. No system contact access or new customer creation flow.
- **Selected Credit customer in Sale:** a compact summary using existing information styles: Owes now or Advance, After sale, configured Limit, and oldest known unpaid sale age/unknown-age explanation. Load the selected customer's detail only; do not calculate full ledgers for every picker row. Clear stale detail when customer/session changes and discard late results.
- **Owner customer detail:** add the same read-only age summary below the existing balance/limit facts. No Late/Bad views, reliability colors, customer-list overhaul or new history navigation.
- **Existing Sale review dock:** correct the draft total to include surcharge; the warning opens only on an over-limit/unavailable-check submission. Ordinary within-limit sales retain the existing save path. Error/notice/receipt and optional delivery follow-up remain.
- **Chrome and keyboard:** preserve shared header/bottom dimensions and existing navigation/form guards. Search scrolls within the dialog and follows existing IME handling. Loading a credit summary must not reload the whole Sale page. Use light/dark and 200% text fixtures for the affected card/dialog/dock only.

## Fixed example oracle for implementation

All amounts below are synthetic PKR; as-of date is 15 October 2026 unless specified. They are expected outcomes, not test results.

| Case | Inputs | Expected result |
|---|---|---|
| Partial FIFO | Sale 100 on 1 Oct; sale 200 on 10 Oct; collect 150 | Debt 150 on the 10 Oct sale; oldest known age 5 days. |
| Full payment | Same sales; collect 300 | Debt 0, no unpaid age. |
| Advance | Collect 120 before a 100 sale | Advance 20; no unpaid age. |
| Opening debt | Carried-over debt 100; 10 Oct sale 80; collect 50 | Debt 130: unknown-age carried-over 50 plus dated 80, age 5 days for that dated portion only. |
| Corrected sale | 1 Oct sale 100 corrected to 160; 10 Oct sale 100; collect 120 | Debt 140: 40 attributable to 1 Oct and 100 to 10 Oct. Correction is not a new dated 60 debt. |
| Corrected collection | Sale 200; collection 150 corrected to 100 | Debt 100, linked to the original sale's age. No new borrowing age from the positive collection delta. |
| Zero-delta correction | Existing sale corrected only in nonfinancial facts | Debt/age unchanged; ancestry still valid. |
| Null/zero/equal limit | Projected debt 100; limit null / 0 / 100 | No configured-limit warning / warning / no over-limit warning, respectively. |
| Surcharge crossing | Debt 800, limit 1000, merchandise 180, surcharge 50 | Sale total 230, projected debt 1030; show warning. |
| Advance offsets sale | Advance 50, sale 80, limit 100 | Debt after sale 30, no over-limit warning. |
| Aggregate precision | Raw signed balance 0.004, proposed raw sale 0.004, limit 0 | Projected canonical debt 0.01; warning, despite separately displayed values rounding to 0.00. Preserve stored raw rows. |
| Stale warning | Warn at projected 1030; then collection/profile/draft changes before confirmation | Revalidate before writing; refreshed warning if still required, otherwise normal single save. Old acknowledgment cannot blindly authorize changed data. |
| Incomplete history | Missing ancestor, malformed date, future-dated balance or unsupported movement | No invented age or silently altered balance; explicit coverage/unavailable state. Security failures stay failures. |

## Implementation order and exact stopping points

- [x] ✅ ~~Baseline, relevant primary-source research and independent judgment.~~
- [x] ✅ ~~Credit calculation/read boundary and independent synthetic oracle checks.~~
- [x] ✅ ~~Existing customer search and selected-customer summary, plus the small Owner detail summary.~~
- [x] ✅ ~~Authoritative surcharge-inclusive preview and advisory warning/revalidation in the current save path.~~
- [x] ✅ ~~Affected checks and one isolated phone walkthrough; result/plan reconciliation.~~

Expected source boundaries: a small `domain/CustomerCreditAging` read-only calculation/read service; `data/Daos.kt`/ledger read DTOs for required UID/ancestry/aggregate reads; a narrowly shared sale amount helper if required; `NativeSaleFacade`, `NativeSaleViewModel`, `CashierSaleScreen`; `NativeOwnerCustomersFacade`, customer detail state/UI; the necessary strings/tags. Reuse existing permissions, transaction gate and canonical money SQL. No schema migration, stored balance/age/score, server SQL, new dependency/provider, release version bump or ordinary-app update is included.

If baseline data cannot support the defined age safely, expose the bounded coverage limitation and record the exact missing case. Do not silently add a data-repair/backfill project.

## Affected verification only

Pin the exact method list when implementing, based on touched functions. New calculation tests cover the oracle, stable-UID order under different local IDs, corrections/opening/advance, aggregate rounding and coverage failure. Facade/ViewModel tests cover strict warning thresholds, surcharge and wholesale modes, stale acknowledgement, cancel/no-write, duplicate taps, session isolation and normal single save. UI tests cover search/clear/no-match/duplicate names/last lazy row, card loading/error, warning Back/Continue, receipt and affected header/dock geometry.

Reuse selected existing methods from `Phase8AccountLedgerTest`, `Phase8SaleOperationsTest`, `NativeSaleHardeningContractTest` (surcharge/default/edit and closed day), `NativeSaleStateRegressionTest` and `NativeSaleScreenContractTest` (wholesale, payment picker and receipt/delivery preservation) only where touched. Add the selected Owner detail contract. Do not run all methods/classes or re-run two-phone convergence, hosted recovery, haptics or global animation audits. One connected phone with isolated QA data is sufficient; no real shop/customer transaction, message, account edit or cloud write belongs to QA.

## Completion rule

Cross out phase 3 only after calculation/search/summary/warning and their affected acceptance are actually complete. All five scoped substeps are now complete; the result records acceptance and delivery limits. Preserve phases 1 and 4–6 as pending; phase 2's accepted held-entry patch stays credited. Newly observed unrelated issues go in a later note, not this phase.
