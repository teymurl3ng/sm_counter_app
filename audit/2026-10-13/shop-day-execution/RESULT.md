# 9a1 — Isolated authenticated shop day accepted

Four selected methods are accepted on A06 (SM-A065F, serial R8VXA02SNKH). This is saved-record/command/native-facade acceptance in fresh in-memory Room fixtures, not an uncoached staff session, screen traversal, physical midnight test or two-phone convergence claim.

## Outcome

The new [SyntheticShopDayAcceptanceTest.kt](../../../phase18/app/src/androidTest/java/com/saaf/milkshop/SyntheticShopDayAcceptanceTest.kt) exercises the frozen story using authenticated Operator, Cashier and Owner boundaries. Receipt, production and correction command services and native sale/collection/expense/supplier-payment/closing facades produce the independently prepared quantities and balances. Setup creates disposable fixture identities and opening state; it is not ordinary shop onboarding acceptance.

- All ten D balance checkpoints match: final Cash **PKR 9,335.00**, customer receivable **180.00**, supplier payable **16,576.92**, Milk **78.5 L**, Yogurt **16 kg**. Other Cash/Bank accounts remain zero.
- Saved receipt/header fields, each inventory/financial child row, event actors and success attribution are checked. Physical stock uses 100 raw litres while acquisition uses TS-adjusted pricing. Measured production is 20 L to 17 kg; no automatic loss/yield policy is created.
- The correction retains the original sale/header/children, stores restated 1.5 L / 285 PKR and writes only +0.5 L / -95 PKR movements. Full event, inventory and money lists are unchanged by delivery creation/fulfilment and count/close/reopen/recount.
- Supported cost coverage is complete. Milk production transfer **3,515.38**, Yogurt batch cost **3,770.38**, final Milk/Yogurt carrying costs **13,797.89 / 3,548.59** match. Effective revenue **565**, COGS **485.44**, recorded Fuel expense **50**, estimated recorded result **29.56** match. Conversion inputs are allocations, not extra cash expenses; collection and supplier payment do not inflate revenue/expenses.
- Cashier production and supplier payment, pending-delivery close, duplicate fulfilment, unexplained variance close, repeat close, closed-date sale/correction, Cashier reopen, blank-reason/wrong-passphrase Owner reopen and pre-reopen count reuse are refused. Every refusal compares complete saved ledger, close/count, delivery/status and actor rows before/after. Authentication/audit metadata may legitimately record a failed authentication; that is not treated as a business mutation.
- First close retains expected **9,335**, counted **9,330**, variance **-5** and truthful staged-count reason. Owner authenticated reopen and Cashier fresh **9,335** recount/reclose preserve three actions, two counts, actors, reasons, count references and supersession chain. No balancing entry is invented.
- D+1 is open with carried balances, no second opening. One 0.5 L cash sale adds **95**: final Cash **9,430**, Milk **78 L**, Yogurt **16 kg**, customer **180**, supplier **16,576.92**. D remains closed. Explicit dates test posting rollover; no global/phone clock change or real midnight observation occurred.

## Focused validation and repairs

The first compile used the wrong `uid` field name in the new test; corrected to the existing `eventUid` field before phone execution. The first four-method A06 run passed both existing close regressions, while both new methods failed at fixture setup because the new test looked for an account named Fuel. The app represents Fuel through its expense-category profile. The test now uses that existing profile lookup. Those failures did not reach business-event setup and did not reproduce a product defect.

After this test-only repair, **only the two affected new methods** were rerun; both passed on final source in **17.162 seconds**. The final build passed in 11 seconds (earlier successful compile build: 22 seconds). The existing selected methods remain accepted from their first run:

- `NativeCashCloseFacadeTest#cashMovementAfterCountRefusesCloseWithoutPartialWrite`
- `NativeCashCloseFacadeTest#earlierOpenDeliveryRefusesCloseUntilCashierResolvesIt`

Their source and main APK hashes are identical across both runs. Exact selections, first failure logs, final passing logs, summaries, oracle checks and tested source/APK hashes are retained here. `verification.json` checks four distinct accepted methods, all eleven literal balance rows against the frozen oracle, unchanged source/specification hashes and unchanged ordinary/preview/QA package versions/install timestamps. The full suite was not run. Existing Gradle experimental-source-set/JDK-25-to-24 fallback warnings remain; no dependency/toolchain change was made.

Only one new instrumentation source file was added. All 394 baseline main/reference/selected-existing-test hashes still match; EXPECTED.json is unchanged. Only `com.saaf.milkshop.qa.tests` and its instrumentation package were installed. Fresh in-memory fixtures avoid ordinary databases; no real credentials, server Auth/sync/provider/Drive operation, publication, signing or ordinary release installation ran. Unrelated dirty work is preserved.

## Remaining plan and next scope

1. Money/withdrawal fixes — complete at bounded QA scope.
2. Receiving sync — complete at bounded QA scope; physical convergence remains conditional phase 5.
3. Mapped menus/lists — complete at bounded QA scope; actual phone appearance remains phase 5.
4. Access/recovery/setup — local checks and copy complete; hosted identity/reset publication and intended-device enrollment/handover remain open. B7 central revocation and B8 unattended conflict attention retain their conditional gates.
5. Shop-day/phone acceptance — 9a0 scope and **9a1 automated story complete**. **9a2 uncoached staff acceptance** uses the existing task cards/oracle; 9c actual phone prompt/Back/haptic/keyboard/text checks can run independently after narrowing their existing scope. 9b needs two physical writable phones only if both will write. Do not turn this automated result into a human pass or start a new audit.
6. Freeze/commit/push/sign/install — pending accepted release, recovered-signing/upgrade evidence and actual ordinary-device handover.

This batch stops here. No production mismatch was reproduced and no production patch is needed for this story. Next preparation uses the already saved staff cards or the existing named phone checks, without reopening completed implementation.
