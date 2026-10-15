# Flat completion checklist â€” 14 October 2026

**Later15October current patch:** Real ordinaryA06 Owner/PROD setup and historicalrecords are observed. A live null-timezone sync failure is reproduced/fixed with6 affected passes. Signed0.4.3/code7 is installed/hash-verified onA06, dataretained; S24 matching patch is pending connection, so R35 is open for the current patch. R36 remains partial until post-update private signin/live sync/record comparison. [Patch](../../2026-10-15/prod-timezone-fix/RESULT.md). Earlier installation/freeze completion notes refer to0.4.2.

**15 October reconciliation:** R22–R28 and R30–R35 are complete at their named technical scope. Hosted recovery, source publication, production browser/reset, both signed phone installations and seventeen affected checks are credited. R29 still needs independent custody/recovered-backup acceptance; R36/R37 need private ordinary-app records/login and real setup; R38 has completed instructions but final private handover remains open. Batch 4 human outcomes remain open. [Current result](../../2026-10-15/current-finish/RESULT.md). Older dated notes below are historical.


Detailed substeps retained from the inventory. The user-facing progress plan is [seven finite batches](FINISH_BATCHES.md); these 38 rows are not 38 separate jobs. Preserve stable R/C IDs for evidence mapping. New findings stay attached to their blocking batch and need a bounded scope before editing.

## Remaining completion tasks

- [x] **R01** ~~Fix reception of another phoneâ€™s closing record.~~ <!-- 9b2a / A3 -->
- [x] **R02** ~~Retain and retry refused closing records safely.~~ <!-- 9b2a / A3 -->
- [x] **R03** ~~Verify sales can be saved during an actual network outage.~~ <!-- 9b2 / A3 -->
- [x] **R04** ~~Verify queued sales sync once after reconnection.~~ <!-- 9b2 / A3 -->
- [x] **R05** ~~Compare both phonesâ€™ complete records and balances after offline work.~~ <!-- 9b2 / A3 -->
- [x] **R06** ~~Verify a Cashierâ€™s count and close reach both phones.~~ <!-- 9b2 / A3 -->
- [x] **R07** ~~Verify closed-day sales are refused on both phones.~~ <!-- 9b2 / A3 -->
- [x] **R08** ~~Verify Cashier reopening is refused in the two-phone scenario.~~ <!-- 9b2 / A3 -->
- [x] **R09** ~~Verify Owner reopening with authentication and a reason reaches both phones.~~ <!-- 9b2 / A3 -->
- [x] **R10** ~~Verify fresh recount and reclose without changing money or stock.~~ <!-- 9b2 / A3 -->
- [x] **R11** ~~Verify interrupted sync/recovery resumes without lost or duplicated records.~~ <!-- 9b3 / A3 -->
- [x] **R12** ~~Prepare the disposable app and saved task cards for staff testing.~~ <!-- 9a2 / A1 -->
- [ ] **R13** Have staff complete the shop day without navigation coaching. <!-- 9a2 / A1 -->
- [ ] **R14** Test cancellation and account isolation with phone-security unlock actually enrolled. <!-- 9c / A4 -->
- [ ] **R15** Test successful real phone-security/biometric unlock of the chosen account. <!-- 9c / A4 -->
- [ ] **R16** Check vibration strength and consistency by feel. <!-- 9c / A4 -->
- [ ] **R17** Personally check repeated screen transitions for flashing and jumps. <!-- 9c / A4 -->
- [ ] **R18** Check Sale, Count cash and Settings at the intended phone text size. <!-- 9c / A9 -->
- [x] **R19** ~~Finish the named Overview screen visual check.~~ <!-- 9c / A10 -->
- [x] **R20** ~~Finish the named Operator Today visual check.~~ <!-- 9c / A10 -->
- [x] **R21** ~~Finish the cash-count receipt visual check.~~ <!-- 9c / A10 -->
- [x] **R22** ~~Choose and authorize the isolated hosted recovery test environment.~~ <!-- 8b3b / O1; existing proposal needs exact route approval -->
- [x] **R23** ~~Test server identity recovery and real account sign-in there.~~ <!-- 8b3 / O1 -->
- [x] **R24** ~~Publish the accepted server recovery tools.~~ <!-- 8b3 / O1 -->
- [x] **R25** ~~Complete the real browser credential-reset feedback check.~~ <!-- 8a / B4; currently blocked by approval/access review -->
- [x] **R26** ~~Roll out the protected reset guard to production when accepting that feature.~~ <!-- 8a / B4 -->
- [x] **R27** ~~Publish/promote the updated reset interface in the admin Viewer.~~ <!-- 8a / B4 -->
- [x] **R28** ~~Publish the updated protected-reset CLI integration.~~ <!-- 8a / B4 -->
- [ ] **R29** Verify current signing-key custody and signing with the recovered key. <!-- 10 / O6 -->
- [x] **R30** ~~Freeze the accepted source version.~~ <!-- 10 / A5 -->
- [x] **R31** ~~Run the agreed final checks on that frozen version.~~ <!-- 10 / A5; affected-only preference retained -->
- [x] **R32** ~~Save the exact release evidence, build hashes and limitations.~~ <!-- 10 / A5 -->
- [x] **R33** ~~Commit and push the accepted app changes.~~ <!-- 10 / A6 -->
- [x] **R34** ~~Build and version the signed ordinary app; verify its certificate/package.~~ <!-- 10 / A6 -->
- [ ] **R35** Install the current0.4.3 patch on intended phones: A06 done; S24 awaits connection. <!-- 10 / A6 -->
- [ ] **R36** Verify the upgrade preserves existing records and logins. <!-- 10 / A6 -->
- [ ] **R37** Configure the actual shop, staff access, PINs and sync connection. <!-- O3 / actual handover -->
- [ ] **R38** Hand over the devices with final recovery/rollback instructions. <!-- O3 / O6 / 10 -->

R01/R02 are accepted in [close-sync repair](../../2026-10-14/close-sync-repair/RESULT.md), following the scope in [closing repair](../../2026-10-14/offline-close-baseline/FIX_SCOPE.md). R03â€“R10 are accepted in [physical offline/closing](../../2026-10-14/physical-offline-close/RESULT.md), using the frozen [offline expectations](../../2026-10-14/offline-close-baseline/EXPECTED.json), after that repair. R11 is accepted at its frozen missed-response/fresh-process scenario in [recovery result](../../2026-10-14/sync-recovery/RESULT.md); newly emitted queued closing history is repaired. Old affected feeds remain conditional C03. A06 and S24 both completed the accepted physical scenario. Check current connectivity before the separate recovery run.

Historical pre-acceptance note superseded: the user subsequently authorized the exact hosted-recovery route and the current technical finish. Those tests, publication and installation are now accepted at the scopes above. No permission to broaden platform changes is implied.

## Conditional items â€” enable only when their stated condition applies

- [ ] **C01** Check TalkBack on account selection, Sale and Count cash. If used in the pilot; remains a wider-release accessibility check.
- [ ] **C02** Verify production administration using disposable accounts. If production administration/reset is enabled and used.
- [ ] **C03** Repair previously affected historical records. Only if affected UIDs and authoritative source history are identified; R02 does not automatically repair old skipped history.
- [ ] **C04** Implement central staff/device revocation. Only if the deployment requires that trust model; currently deferred design.
- [ ] **C05** Add persistent conflict alerts. If unattended multi-device operation depends on them; current review list remains.
- [ ] **C06** Run a physical power-loss experiment. Optional, if specifically required; distinct from R11 process/interruption checks.

## Finished work retained at its recorded evidence scope

- [x] **D01** ~~Entrance/welcome/account selection redesign and Open-button repair.~~
- [x] **D02** ~~Quick PIN keypad and phone-security integration implementation.~~
- [x] **D03** ~~Portrait layout and stable top/bottom chrome implementation.~~
- [x] **D04** ~~Dynamic sale products and configurable shop identity.~~
- [x] **D05** ~~Mapped capsule/list/settings/night-mode/typography improvements.~~
- [x] **D06** ~~Outcome feedback, transition wiring and haptic dispatch repairs.~~
- [x] **D07** ~~Money rounding and owner-withdrawal correction/receipt/remedy handling.~~
- [x] **D08** ~~Profile and planning receiving/catch-up, including SQLite profile dates.~~
- [x] **D09** ~~Cashier Activity expenses and transaction/product/people/management presentation.~~
- [x] **D10** ~~Fresh Counter factory-seed reconciliation.~~
- [x] **D11** ~~Opening-detail and unlinked remote-author record transport.~~
- [x] **D12** ~~Server backup automation and current isolated app restore checks.~~
- [x] **D13** ~~Access wording/local lock versus sync preservation checks.~~
- [x] **D14** ~~DEV reset API/CLI guard acceptance; browser/production work stays R25â€“R28.~~
- [x] **D15** ~~Independent shop-day oracle and automated shop-day rehearsal.~~
- [x] **D16** ~~Seven representative automated actual-phone checks.~~
- [x] **D17** ~~Physical online two-phone exchange: all18 tables at base/sale/repeat.~~
- [x] **D18** ~~Offline/closing baseline: role rules verified and import defect reproduced/scoped.~~

Actual human acceptance remains R13â€“R21/C01; implementation/automated passes above do not certify felt haptics or successful enrolled biometrics. Current app restore and backup success do not certify hosted identity recovery or signing recovery.

## Separate later backlog â€” unchanged

B11/B12, E1â€“E7 and F1â€“F10 remain in [ITEM_MAP.csv](ITEM_MAP.csv): future deletion guardrails and general-product rules; dormant UI/state/toolchain/host-test/lint/observability/docs cleanup; credit/reminders; payment/message integrations; approval queues; Urdu/general-retail valuation; snapshot viewer; central directory/MFA; expanded history/rate audits; time-display options; advanced employee administration; receipt artwork/wide layouts. These entries are retained ideas and conditions, not newly added R tasks. Portrait remains supported.

Underlying historical plan/evidence: [PLAN.md](PLAN.md), [latest baseline](../../../CURRENT_BASELINE.md), [online acceptance](../../2026-10-14/event-detail-sync/RESULT.md), [offline baseline](../../2026-10-14/offline-close-baseline/RESULT.md).

Batch4 preparation update: R12 and bounded named capture reviewsR19–R21 are accepted on both A06/S24 in [handoff result](../../2026-10-14/staff-phone-handoff/RESULT.md). R13–R18 human results remain pending. Use the independent Shop Day QA app and saved [instructions](../../2026-10-14/staff-phone-handoff/START_HERE.md); no preview/ordinary reset or coached staff success.
