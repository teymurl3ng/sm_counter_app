# 9a0 — Freeze the synthetic shop-day rehearsal

Planning is complete when the fixture, independent expectations, role assignments and execution limits are saved. It is not an executed shop-day result or permission to use ordinary shop records.

## Fixed fixture

Use a new isolated in-memory Room database inside the instrumentation sandbox. No ordinary database, remote Auth, sync connection, provider changes, credentials, signing or app installation is part of this planning batch. Preserve the older small `REHEARSAL.md` as historical scope; this document supersedes it for the next shop-day run.

- Synthetic store: **Shop Day QA**, currency PKR, timezone Asia/Karachi.
- Three disposable local users: Owner, Cashier and Operator. Authenticate mutations through the actual command/native facade boundaries; a service call alone does not prove role enforcement. Fixture creation is setup, not evidence of successful user onboarding.
- One active synthetic supplier and customer; seeded Cash, Milk and Yogurt. Opening Cash PKR 10,000; no opening inventory, debt or Bank balance. Zero opening stock avoids inventing an opening inventory cost basis.
- Supplier milk rate PKR 170/L, inbound delivery fee PKR 11/raw L, TS standard 13. Retail milk PKR 190/L; yogurt PKR 280/kg. No retail delivery surcharge, tax, salaries, owner funds or spoilage in this fixture.
- Production-month policy, approved by Owner with recent authentication: normal capacity 1,000 kg; five components all using 1,000 kg as basis. Source amounts: culture 1,000; packaging 2,000; variable energy 3,000; direct labor 4,000; fixed production overhead 5,000 PKR. Total conversion allocation PKR 15/kg. These are synthetic policy inputs, not posted expenses, real shop estimates or new cash payments.
- Day D is yesterday in the shop timezone; D+1 is today. Bind explicit dates at the supported boundaries, without changing phone or global clock. This tests posting rollover, not a physical wait across midnight. Policy month follows D, including month boundaries.

## One day, literal expectations

Amounts in this table are canonical PKR balances. Purchase's unrounded calculated header may retain more precision; do not replace it with an assertion that every field is a rounded display amount.

| Step | Acting role and operation | Cash | Customer owes | Supplier owed | Milk L | Yogurt kg |
|---|---|---:|---:|---:|---:|---:|
| 0 | Owner fixture/setup: opening Cash | 10,000.00 | 0.00 | 0.00 | 0 | 0 |
| 1 | Operator receives 100 raw L, fat 4%, LR 28 | 10,000.00 | 0.00 | 17,576.92 | 100 | 0 |
| 2 | Operator makes yogurt: measured 20 L used, 17 kg produced | 10,000.00 | 0.00 | 17,576.92 | 80 | 17 |
| 3 | Cashier enters cash milk sale: 2 L at 190 | 10,380.00 | 0.00 | 17,576.92 | 78 | 17 |
| 4 | Cashier sells 1 kg yogurt at 280 on customer credit | 10,380.00 | 280.00 | 17,576.92 | 78 | 16 |
| 5 | Cashier collects 100 from that customer into Cash | 10,480.00 | 180.00 | 17,576.92 | 78 | 16 |
| 6 | Cashier records Fuel expense 50 from Cash | 10,430.00 | 180.00 | 17,576.92 | 78 | 16 |
| 7 | Owner pays supplier 1,000 from Cash | 9,430.00 | 180.00 | 16,576.92 | 78 | 16 |
| 8 | Cashier creates delivery linked to the yogurt credit sale | 9,430.00 | 180.00 | 16,576.92 | 78 | 16 |
| 9 | Owner corrects the mistaken milk entry to 1.5 L, rate unchanged | 9,335.00 | 180.00 | 16,576.92 | 78.5 | 16 |

The independent calculator checks the table and freezes its amounts in `EXPECTED.json`. The opening-to-final cash identity is 10,000 + 285 + 100 - 50 - 1,000 = **9,335**. The yogurt sale is credit, so its 280 never enters the drawer.

Steps after the ledger entries:

1. Record a synthetic physical count of 9,330, five short. Closing with a variance reason while the linked delivery is pending must fail without a close action. Fulfil delivery once; inventory/finance do not move again. Repeating fulfilment is refused without another terminal status.
2. Closing the short count without a reason must fail. With `QA staged count: PKR 5 missed during first count`, Cashier closes D with expected 9,335, counted 9,330, variance -5. Count and close do not silently post a balancing adjustment. Repeat close is refused; attempted sale/correction into closed D must not change ledger totals.
3. Cashier reopen is denied; Owner reopen requires the actual passphrase and a reason, `QA review the recorded drawer count`. Wrong passphrase and blank reason preserve the closed state. After valid reopen, reuse of the pre-reopen count is refused. Cashier records a fresh count 9,335 (note: `QA recount includes the PKR 5 missed in the first count`) and recloses with zero variance. Preserve the original close, reopen and final close, actors, count references and supersession chain.
4. Load D+1: open for posting with carried balances, no second opening and no duplicated stock/cash. A Cashier cash sale of 0.5 L at 190 adds 95 cash and removes 0.5 L Milk. New balances: Cash 9,430; customer 180; supplier 16,576.92; Milk 78 L; Yogurt 16 kg. D stays closed. No second-day close is added to this run.

## Cost expectations and independent authority

Core frozen specification §§4–11 supplies acquisition, measured production, sales, money movements and correction truth/deltas. `PHASE3_COGS_IMPLEMENTATION_DESIGN.md` §§6–7 supplies the accepted extension: half-up integer-paisa moving-average releases and monthly conversion allocation. Its hash is saved alongside the frozen core hashes. It is not part of the original frozen core text.

`calculate-expectations.py` uses Python rational arithmetic, no Android calculator/service output and no previously passing test values. The correct acquisition is SNF 8.60%, TS 12.60%, adjusted litres 1260/13, amount owed 228500/13 PKR, rounded to 17,576.92. Physical stock is 100 raw litres, not adjusted litres. Measured yield is 17/20 (85%); this is a fixture input, not an automatic production formula or a yield-policy approval.

Expected cost transfer: Milk into production 3,515.38; conversion allocation 255; Yogurt batch cost 3,770.38. Effective corrected Milk sale releases 263.65, Yogurt sale 221.79. Final carrying costs: Milk 13,797.89, Yogurt 3,548.59. Product revenue 565; cost of sold products 485.44; profit before recorded shop expenses 79.56; Fuel expense 50; estimated recorded result 29.56. Collection and supplier payment are not revenue/operating expenses. This is the app's cost-based recorded result, not a claim of full business profit. Surcharge is zero and all fixture flows should have complete cost coverage.

## Next execution batch: 9a1, two new methods plus two relevant existing methods

Only add `SyntheticShopDayAcceptanceTest.kt` under `phase18/app/src/androidTest/java/com/saaf/milkshop/`. It may contain shared fixture helpers and literal expectations copied from frozen `EXPECTED.json`. Read back full saved headers/children and compare step balances, correction linkage/deltas, receipt calculations, effective cost rows, actors and close/count history. Do not generate expectations by calling the app's calculator or by observing a run and adopting its output.

Planned new methods (not present or executed yet):

- `authenticatedShopDayMatchesIndependentLedgerAndCost`: steps 0–9 plus delivery/count/close/reopen/recount. Include the named refusal assertions, full monetary/stock snapshots around refused actions, actual permission gates and immutable original-sale checks. Cashier production and supplier-payment permission denial should be verified against their actual boundary, without inventing an authorized Cashier route.
- `nextDateStartsOpenWithCarriedBalances`: the accepted D fixture, then the one D+1 sale and persistent closed D; do not simulate server convergence or claim actual midnight observation.

Two existing selected regression methods:

- `NativeCashCloseFacadeTest#cashMovementAfterCountRefusesCloseWithoutPartialWrite`
- `NativeCashCloseFacadeTest#earlierOpenDeliveryRefusesCloseUntilCashierResolvesIt`

Build only the instrumentation sandbox and run these four exact methods on one available phone, with ordinary packages/data untouched and bootstrap disabled when supported by the isolated fixtures. No complete test class/suite, browser/API, physical two-phone run, remote identity deployment or theme redesign. Source/selected check/APK provenance must be recorded before execution. If this story confirms a production mismatch, save the failing step and scope that specific fix before changing production; don't broaden the audit.

## Remaining acceptance, distinct from automation

9a2: one uncoached staff rehearsal using this fixed story on an explicitly disposable acceptance app. `TASK_CARDS.md` states business tasks, not navigation instructions; an evaluator independently holds the expected table. Use a prepared synthetic cash count, and distinguish intentional count error from missing money. Record success/failure, unclear controls and assistance; assistance is not an uncoached pass. Intended ordinary-device credentials/enrollment/handover are O3/phase 6 and remain pending.

9b: two physical writable phones only if both will write, using the existing frozen convergence plan. 9c: the named actual PIN/biometric, Back, haptic, keyboard and representative large-text checks. Neither is certified by in-memory Room or by screenshot fixtures. All unrelated findings go to the existing later queue and are not investigated in 9a1.
