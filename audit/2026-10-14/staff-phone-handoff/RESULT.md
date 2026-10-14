# Staff/phone acceptance prepared; human results still pending

Shop Day QA is installed and opened on both A06 and S24 as a new independent com.saaf.milkshop.acceptance package. Actual APK label/identity and BuildConfig previewFiller=false are verified. Existing production/preview/.qa/.qa.tests versions/install timestamps and every pre-run main/schema/existing-test source hash remain unchanged. No cloud enrollment, remote account, global display/radio setting, ordinary data reset or phone credential action occurred.

## Prepared human fixture

Each independent phone uses the same frozen Shop Day QA starting fixture: three Day Owner/Cashier/Operator accounts; Day Supplier/Customer; rates/settings and approved15PKR/kg conversion allocation; Cash10000; Milk0/Yogurt0; only one opening event. Password is the shared disposable acceptance password requested by the user, documented in START_HERE.md. No sale, receipt, production or close is rehearsed in this starting database. Native launch preserves the fixture; after all visual tests a separate raw synthetic-business query confirms only that opening state, empty stock and zero count/close history on each phone. Raw DB copies are temporary; credential/security tables are not queried or archived.

Preparation is one new setup method per phone, both passing. The build-time init script creates the separate package/label and disables ordinary debug filling only in the delivered acceptance artifact. Actual version name is0.4.1-qa-tests and hashes are saved. No production code or permanent build configuration changed. Original staff TASK_CARDS.md bytes are copied unchanged. START_HERE.md supplies the short personal checklist; EVALUATOR_RESULTS.md is held separately so staff do not see expected totals or navigation coaching.

## Three named visual tails

Final Owner Overview, Operator Today's work and cash-close receipt methods pass on both phones at their current text settings. Captures show structured facts/record groups, complete measured labels and visible controls; full details can be reached by scrolling. See capture-review.json. These are real composables rendered with synthetic fixture states; no extra events are added to the human DB. Named visual tails R19–R21 are accepted at this bounded capture scope, while actual personal text usability R18 remains pending.

Initial visual test width-flag assertions incorrectly reject full labels. Captures and final line-edge measurements show no missing characters/ellipsis, with glyph edges within layout widths. The new check now follows the existing presentation helper's explicit full-text/line-edge criterion, retaining JSON evidence. The Operator fixture was corrected from Work home to the specifically named Today's work list with host header. Final A06 three methods pass; the two adjusted screenshot frames also pass. S24 initially auto-locks and its tests cannot see Compose hierarchies; window-policy read confirms keyguard/input restriction/sleep. After the user unlocks it, only its three visual methods are rerun and all pass. The original errors are retained, not hidden or counted as acceptance. Build/resource/import fixes are explained in VISUAL_TEST_NOTE.md.

## Remaining human part of batch4

R12 preparation and R19–R21 named capture reviews are complete. R13 uncoached staff use, R14/R15 actually enrolled phone unlock cancel/success, R16 vibration comfort, R17 personally observed movement and R18 actual Sale/Count/Settings readability remain pending. Do the full business story on one phone; the second is an independent fixture for observation, with no sync connection. Actual next-date observation waits for a supported next date; do not silently change phone time. TalkBack stays conditional if used. No personal feel, biometric success or uncoached staff outcome is inferred from automation.

Whole batch4 is **not crossed off**. Human replies/results are required for it. Batch5 hosted recovery, batch6 reset rollout and batch7 signed delivery remain pending. No new independent finish task or broad audit added; no commit/push.
