# Remaining work reconciled — 11 October 2026

This is the current consolidated backlog after the menu typography repair. It supersedes stale open/next-step statements in REMAINING_WORK.md, FINDINGS_STATUS.md, README.md and the September roadmap for deciding what to do next. Historical records are retained. A prior plan saying “deferred” is not proof that the feature is still missing: later results and current source take precedence.

## Readiness judgment

The main dairy-shop workflows are implemented. The recorded October 10 production phone login/enrollment/sync and Viewer login/read acceptance are complete. The current preview contains newer entrance, PIN, settings, night-header and menu repairs than that signed production release.

**A controlled single-device trial is within reach; unrestricted two-device use is not yet established.** This is a judgment from the remaining boundaries below, not a measured completion percentage or a claim that all business data is correct. Before replacing the existing record system, close the relevant money/access limitations, establish recovery, ship the exact accepted build, and complete an independently checked shop-day rehearsal. Multi-device operation additionally needs profile/planning application and real convergence/close recovery evidence.

The remaining visual passes are real commitments, but they are not all prerequisites for testing the core workflows. Landscape is excluded by the user's portrait-only decision. S24 connectivity is not a blocker for single-device work.

## Audit method and evidence limits

- Read the root remaining-work/finding/decision/phase ledgers, hidden cutover/C1 planning records, October 9–10 repair/release/acceptance continuations, and October 11 plans/results/deferred notes.
- Reconciled newer completion records against older “open” statements. Inspected current native launcher, sale/catalog, sync application, identity policy, drawing/correction, Activity, PIN settings and notification implementations.
- Read the prepared account-server workflow/bootstrap notes and current local Viewer credential-reset route. Its role restriction remains absent from the inspected route. External checkout Git status could not be read inside this sandbox; no fresh remote publication or live-server audit is claimed. Viewer local HEAD is b5122e8, later than the 115a636 release record; whether that later presentation commit is deployed needs verification, not an assumption.
- Current source files match the final menu-repair source-hash manifest. That verifies artifact continuity, not overall correctness. The final affected-test record is 26 unique latest passes. No build, device test, full suite, deployment, account change or business-data mutation was performed for this inventory.

Evidence labels: **source-confirmed** means the inspected implementation establishes the limitation; **recorded pending** means the latest scoped handoff explicitly leaves it open; **verify first** means an old item has no adequate current closure or reproduction; **conditional** means required only if that feature/deployment is used; **future** means it must not silently become a pilot gate.

## 1. Business correctness and access boundaries

| ID | Work left, in plain language | Evidence/status | Required boundary |
|---|---|---|---|
| B1 | Sync complete product/customer/supplier/employee/expense profiles, including updates, active/archive state, prices, limits and contact data. | Source-confirmed: SyncApplyEngine.applyProfileChild creates only a limited new Customer profile; existing profile updates and Vendor/Salary/Operating Expense satellites are absent. Product profiles are not applied by applyProduct. | Before treating two writable phones as equivalent. Local catalog pricing does work. |
| B2 | Apply incoming budgets and collection targets. | Source-confirmed: budget_plans and collections_targets return SKIPPED. These work locally, but are not convergent planning sync. | Before relying on shared planning between phones; otherwise explicitly retain the limitation. |
| B3 | Give a mistaken owner withdrawal a supported reversal. | Source-confirmed: OwnerDrawingService requires a positive amount and posts a negative cash movement; CorrectionService excludes OWNER_DRAWING. The old “reverse drawing” comment describes a remedy that is not implemented. | Before using Owner drawings for authoritative records, or deliberately keep that feature out of the trial. |
| B4 | Resolve whether a server Admin may reset an Owner's login credential. | Source-confirmed in the inspected Viewer reset route: target role is not fetched/checked. October 9 audit records this as intentional contract policy, not an accidental implementation bug. | Before treating Admin as a safely lower-trust role; owner-only administration can avoid exposing this trust boundary during a small trial. |
| B5 | Finish the money rounding/tolerance policy review (M15). | Source-confirmed mixed conversion policies: NativeSaleFacade uses kotlin.math.round; Activity/More use BigDecimal HALF_UP. No new live financial loss reproduced by this audit. | Focused half-paisa and correction-boundary checks before claiming one consistent money policy; do not start an unrestricted accounting rewrite. |
| B6 | Show Cashier's own expenses in Activity (M19). | Source-confirmed: list/detail only allow SALE and CREDIT_COLLECTION. Expense entry exists; this is a visibility gap. | Small usability follow-up; not evidence that expense posting is missing. |
| B7 | Finish device-specific revocation and verified per-person server identity/linking. | Source-confirmed: enrollment accepts owner/cashier, counter enrollment skips local-owner linking, ENFORCE_LINK_STATES=false. Local users/PINs and local actor attribution already exist. | Before promising central clerk revocation or server-verified clerk attribution. Do not enable the flag without its documented guardrails. |
| B8 | Add a clear notification when a sync conflict needs review. | Recorded during-pilot item; current SyncNotifier alerts on halted uploads, not general conflicts. Existing held-entry review is already built. | Before unattended multi-device work depends on someone noticing conflicts. |
| B9 | Clarify Stop syncing / Lock-switch semantics and retained entitlement (M34). | Intentional: local account switching leaves installation sync alive; Stop syncing retains the Room binding/grace state. | Documentation/product wording; not a request to revoke both phones' server sessions. |
| B10 | Recheck planning-only restore enqueue behavior (M5). | Verify first: old item explicitly needs re-verification after classified rebaseline. No newly reproduced defect. | One bounded regression if this restore/planning combination is needed. |
| B11 | Keep the no-hard-delete-on-synced-tables rule enforceable (M10). | Upsert-only invariant is documented; old source survey found no user-facing hard deletes. | Guardrail for future delete features; not a current demand to implement deletion sync. |
| B12 | Complete genuinely general-purpose product rules. | Dynamic active catalog and ID-based prices are implemented. Purchase/production/stock units/costing remain the validated dairy model, and some historical product identity remains Milk/Yogurt-specific. | Before advertising arbitrary retail/product-family support; not a blocker for the current dairy shop. |

Primary evidence: [current repair ledger](REPAIR_PLAN_2026-10-10.md), [intentional policy inventory](PROJECT_AUDIT_2026-10-09.md#intentional-decisions-and-incomplete-features-to-revisit), [identity decision](IDENTITY_DECISION_MEMO.md), and current SyncApplyEngine.kt, ServerAccount.kt, OwnerDrawing.kt, Correction.kt, NativeSaleFacade.kt and NativeActivityFacade.kt under phase18/app/src/main/java/com/saaf/milkshop/.

## 2. Design commitments still unfinished

These are five finite planned passes. Some detail work overlaps the list passes; share the design/components and do not charge it as a second complete redesign.

| ID | Remaining pass | Exact screens/families | Status |
|---|---|---|---|
| U1 | Transaction/product hierarchy — night plan B | Owner Records + detail, Cashier Activity + detail, Products + detail, Sale product choices/receipt, Deliveries cards. Distinguish names, references, time, amount, units and status. | Recorded pending; current menu repair did not implement it. |
| U2 | People hierarchy — night plan C | Customers, Suppliers, Employees, Users/access lists and their existing facts; delegated Admin/Manager views inherit once. | Recorded pending. |
| U3 | Personal-security grouping — grouping batch 2 | Owner/Admin/Manager/Cashier/Operations: separate enrolled status from passphrase/PIN/phone-security controls; reconcile body-title treatment. | Recorded pending. Include the source-confirmed four-digit-only setup restriction across all five More forms versus the core/login 4–12 policy. |
| U4 | Entity and record details — grouping batch 3 | Product, Customer, Supplier, Employee, User/access, Business record and Cashier Activity details. Clear label/value groups and safe full-value wrapping. | Recorded pending; coordinate with U1/U2. |
| U5 | Management information — grouping batch 4 | Backup/sync status and inspections, held-entry review, costing/planning rows/details, reports totals, security-audit detail. | Recorded pending. Sync long-value allocation/emphasis is a mapped candidate; not a reproduced overflow here. |

Smaller recorded follow-ups: remaining differing loading/error layouts (M20, exact remaining count needs a targeted inventory); residual Backup “Back to More” wording; Overview balances and Operator Today's work baseline/long-value/time allocation checks; optional blind-count receipt consistency. None is justification for an unbounded screen crawl.

Plans: [grouping](audit/2026-10-11/settings-grouping-plan/PLAN.md), [night hierarchy](audit/2026-10-11/night-hierarchy-plan/PLAN.md), [later typography observations](audit/2026-10-11/menu-typography-alignment-plan/LATER.md), [PIN setup observation](audit/2026-10-11/quick-pin-plan/PLAN.md#deferred-without-further-investigation).

## 3. Acceptance and shipping work

| ID | Work left | Status and stopping condition |
|---|---|---|
| A1 | Uncoached shopkeeper trial (root Phase 15), then a controlled pilot week. | Recorded not completed. Perform a realistic day: staff access, receipt/purchase, production, cash/credit sales, collection, delivery, expense, count/close, next date, mistake/correction and Owner reopen. Compare stock/cash/debtor/vendor totals against independently prepared expectations. This is the strongest practical next gate, not another visual redesign. |
| A2 | Current-native backup/restore recovery rehearsal. | Earlier live Drive and cross-device recovery tests are complete, so do not claim backup is unimplemented. A current-build, representative-data restore drill and safely stored recovery secret remain in the pilot checklist; use an isolated destination, never destructive replacement of the sole shop database. |
| A3 | Two-phone full-ledger convergence and offline close/reopen recovery. | Recorded outside the bounded production acceptance. Verify immutable event headers AND ledger children, profiles after B1, planning after B2, queued offline writes, closed-day refusal/reopen/recount/reclose. Required if both phones write. A quiet repeat sync alone does not prove these. |
| A4 | Accept current appearance, physical motion/haptics and actual phone-security prompt. | Fresh scoped screenshots mostly remain UNREVIEWED. Current PIN routing prompt tests use a controlled gate; prior genuine platform checks have narrower provenance. Review representative real phone flows, cancellation/Back and the settings actually used. No need to certify every historical screenshot separately. |
| A5 | Close the final validation record on the chosen frozen release candidate. | Latest completed broad native checkpoint record is 606/609, formally FAIL; exact three cases passed focused retries. Entrance's later 617-case run was cancelled by the user. New passes have scoped green evidence, not a subsequent all-green full checkpoint. One final planned acceptance run is a standing formal gate, not authorization to repeat it after every UI pass or to run it now. |
| A6 | Commit/push latest Android changes, build/sign/version and install the accepted production candidate. | Source-confirmed: main/origin-main checkout is b379f67 with later tracked/untracked UI/PIN/settings/menu edits. That checkpoint already published the earlier repair work, so the October 10 “all Android fixes uncommitted” statement is stale. Newest A06 app is com.saaf.milkshop.qa; ordinary signed 0.4.1 was preserved. Shipping that release does not ship these newer preview changes. |
| A7 | Production admin mutation acceptance, only where needed. | DEV real create/disable/re-enable/revocation/final-owner checks passed. PROD phone and Viewer read/login acceptance passed. Complete PROD administration behavior was not exercised in that bounded pass; use a specifically authorized disposable identity, never real staff as destructive fixtures. |
| A8 | Historical partial-record recovery, if affected history exists. | Conditional; repair code and rollback/repair tests exist. Determine affected UIDs and authoritative history first. Do not guess whether private shop history needs rewriting. |
| A9 | Representative native accessibility/IME/large-text review. | Current repairs cover selected 100/150/200% layouts. Exhaustive all-state TalkBack/IME/device matrix was deferred; earlier hybrid accessibility acceptance does not automatically certify all new native layouts. Scope to actual shop roles/tasks. |
| A10 | Release readiness/optimization closure (root Phase 16). | Partly done: signed release, certificate continuity, native cutover and earlier performance work exist. Final source/artifact provenance, migration/security/backup boundaries, release report/archive and measured shrinking/Baseline/Startup Profile decisions still need a current closure. Existing release-derived jank observations are not production smoothness acceptance. |
| A11 | Power-loss/filesystem durability experiment. | Explicitly optional/deferred. Completed process-death checks cover their two commit boundaries. Not an automatic pilot blocker or permission to interrupt a live shop device. |

Evidence: [production acceptance complete](audit/2026-10-10/production-acceptance/PROD_ACCEPTANCE_COMPLETE_2026-10-10.md), [release provenance](audit/2026-10-10/releases/RELEASE_2026-10-10.md), [native chrome checkpoint](audit/2026-10-11/chrome-unification/RESULTS.md), [current repair delivery](audit/2026-10-11/menu-typography-repair/RESULTS.md), and Phase 15/16 criteria in [roadmap](ROADMAP_RECONCILIATION_2026-08-27.md).

## 4. Server, device and operational follow-ups

| ID | Remaining work | Evidence/status |
|---|---|---|
| O1 | Publish prepared account-server CLI/bootstrap/DR changes; prove scheduled backup works repeatedly and restore identity in an isolated environment. | Recorded pending. Source/offline fixes exist, including backup-branch continuation and pagination; live repeated automation and full Auth/staff/role/shop-binding restoration were not accepted. A ledger backup alone does not restore the server identity layer. External Git status/publication was not newly certified here. |
| O2 | Confirm keep-alive, inactivity, quota and backup monitoring procedure. | Standing operational item. Existing scheduled workflow and inactivity caveat are documented; current provider state/pricing was not queried in this repository audit. Do not create automation merely because the old plan mentioned one. |
| O3 | Set up the actual shared shop device with cashier-scoped sync and per-person local access; decide the physical pilot device. | Cashier server tier is built and verified; it is NOT still missing. Actual shop-tablet deployment/enrollment/role walkthrough, recovery transfer if replacing A06, and battery background-work setting remain operations. Keep its transport credential lower-privilege than Owner. Tablet-specific design is separate from the portrait phone commitment. |
| O4 | Resolve remaining provider/security/deletion policies. | Recorded: leaked-password protection/MFA/self-service reset direction, provider patch eligibility, fresh NO ACTION versus live CASCADE sync deletion semantics. Verify current state before action; no live policy changes made here. Owner/admin trust choice is B4. |
| O5 | Verify publication of the later Viewer presentation repair. | October 10 admin-ui record proves 13 local presentation changes and 114 tests, not deployment. Local Viewer HEAD b5122e8 is later; current remote/deployed state was not checked. The earlier repaired Viewer 115a636 release/login is already accepted. |
| O6 | Finish signing-key backup documentation and release recovery notes. | Key off-machine backup was already verified in the old ledger; documentation/disclosure/rollback-compatibility notes remain. Do not manufacture a missing signing key. |

## 5. Engineering debt that can be scoped separately

- Remove dormant MainActivity, HybridActivity/WebView, old presentation and transport code/assets (M28). Native launcher, notification routes and five live command-service extraction (M27/M29) are done. Removal is cleanup, not an absent native capability.
- Reduce the remaining large state object/manual dependency wiring (M30/M31); add pinned-toolchain/dependency verification guards (M32); document release-disabled preview machinery risk (M35).
- Reproduce/close inventory-gate nondeterminism (M3). Current inventories have recorded passes; an old open entry alone does not establish an active failing gate.
- Debug the one ignored Robolectric closed-day engine persistence test; broaden costing host coverage if useful. Current bundled-driver device evidence remains distinct from the host driver.
- Lint/static-analysis hygiene, unused imports/tags/resources, unindexed foreign keys and stale artifact cleanup. Earlier Android lint was zero errors with about 236 warnings, not a release compile failure; no fresh lint run/count is claimed.
- Add privacy-appropriate crash reporting/operational observability (M14) if selected. No crash-reporting dependency found in the inspected Android build files.
- Correct stale cloud contract-display metadata and consolidate old documentation. An old version label is not proof that live RPC writes are blocked. Avoid letting contradictory dated “open” statements drive duplicate work.

## 6. Explicitly future or conditional features

1. **Phase 14.9 Credit & reminders — not started.** All 13 tasks remain: FIFO aging; manual bad-credit satellite flag/computed Good-Watch-Late-Bad tiers; advisory limit warning; Credit wording and searchable customer picker; list coloring; product-mix logos; detail history; Late/Bad menus; PDF/PNG bills; sharing; reminder job/log; opt-in SMS rules; manual Remind action. Existing balances/credit sales/collections are not this feature. The approved blueprint's platform assumptions must be rechecked before implementation.
2. Its post-v1 integrations: WhatsApp Business API, JazzCash/EasyPaisa links, automatic delivery pause at limits and bulk reminders.
3. Approval queue: blocked on an authoritative domain API. Existing correction entry is implemented; do not invent an approval workflow to fill this gap.
4. Urdu translation/localization; broader general-product families/units/recipes/costing; formal lower-of-cost/NRV valuation only if its recorded trigger becomes applicable. Current dairy cost disclosures and current English workflows are intentional boundaries.
5. Old optional read-only owner-phone snapshot viewer: still not implemented, but its original “only multi-device direction” restriction predates approved live sync. Re-decide whether it has value; do not add it as a mandatory step beside live sync.
6. One central staff directory/activation codes, staff MFA/self-service reset and broader business management. Device/per-person revocation is tracked in B7; avoid duplicating the same identity migration.
7. Extra sync_records_history sidecar, explicit parked-row backup semantics, a divergence detector, settings-rate change audit trail, backfilled historic closes and “re-record refused work as correction on an open day.” Existing reopen/recount/reclose is the chosen remedy; these are not implemented replacement remedies.
8. Configurable time-trust thresholds and display-timezone refinements abroad. Shop business-date calculation repairs are done; these narrower optional display/settings concerns remain separate.
9. Advanced security-history filter/export/purge and extra employee salary/attendance administration are retained scope exclusions where not already delegated; no current commitment to build every old blueprint exclusion. Operations attendance and salary posting already exist in their supported task screens.
10. Optional receipt/image polish, branding artwork and wide tablet/desktop layouts. Portrait phone operation remains the chosen product scope; no landscape redesign is owed.

Source: [Credit blueprint](PHASE14_9_CREDIT_REMINDERS_BLUEPRINT.md), [NRV triggers](PHASE11_5_NRV_TRIGGER_REASSESSMENT.md), [old pilot decisions](BEFORE_PILOT_DECISIONS.md), [latest later notes](audit/2026-10-11/settings-grouping-plan/LATER.md) and [product scope](PRODUCT_SCOPE.md).

## 7. Finished work not to reopen from stale plans

- Native screen-family ownership, launcher cutover, notification cutover and live command extraction.
- All F01–F33 source repairs, A01 costing performance repair and A02 Viewer acknowledgement repair at their recorded validation scope; DEV and PROD C1.6/S01 database repair.
- Published earlier repaired Viewer, signed 0.4.1 Android release, actual production phone login/enrollment/quiescent sync, and owner-confirmed Viewer login/shop/staff read acceptance.
- Dynamic active sale catalog, catalog-ID pricing and searchable large-catalog selection. General product accounting expansion is separate.
- Configurable shop identity, persistent receipt/outcome feedback, cashier count/close policy, guards, known re-entry fixes and haptic wiring checks.
- Capsule reapplication/Count cash layout, shared stable work chrome, neutral Welcome/access redesign, centered Open/Closed pill, direct usable Quick PIN keypad, Shop settings grouping batch 1, night hierarchy pass A and final menu typography repair.
- Phone portrait lock. Landscape work was expressly removed from scope.

Completion means the recorded bounded implementation/checks; it does not erase outstanding subjective appearance, production performance or complete multi-device acceptance.

## Suggested shortest route to real use

1. Freeze a pilot scope around one shared dairy-shop device. Close B3/B5 and B4 if those roles/features will be used; establish O1/A2 recovery and the actual cashier setup O3. Keep unsupported features explicitly outside that trial.
2. Do A1 on the accepted current preview with independently checked opening/closing totals. Use it to find practical blockers; log unrelated polish without expanding the run.
3. Fix only blockers from that rehearsal, close A4/A5 on the frozen candidate, then commit/build/sign/install A6 with clear artifact provenance. One final acceptance checkpoint belongs here, not after each design batch.
4. Start a controlled pilot with reconciliation against the existing records. Add a second writable phone only after B1/B2/A3 and the relevant identity/conflict boundaries are closed or explicitly limited.
5. Finish U1–U5 in finite batches. Credit/reminders, general retail expansion and cleanup remain separately scheduled.

No implementation or automatic permission to publish/deploy is created by this inventory.
