# Bounded completion plan

Planning complete; implementation not started by this turn. Every item in the reconciled backlog is assigned in ITEM_MAP.csv. Default scope remains the current dairy shop, portrait phone, existing local users and cashier-scoped transport. General retail and Credit/reminders are separate features.

## Execution rule

One batch per execution run. Preflight only its source/evidence, preserve existing working changes, implement the stated output, run affected checks, capture relevant evidence, update its status and stop. Do not continue automatically into the next batch. Small independent subunits below are separate bounded runs, not one combined server/Android job. No full regression while a batch is still changing.

No dates or completion percentage are promised. File boundaries are explicit; a necessary dependency may be added only with a recorded reason and updated scope before editing. New unrelated discoveries go to LATER.md.

## Batch 1 — money correctness and a supported drawing remedy

**1a Rounding (B5):** inspect the existing money-conversion choke points and correction comparisons only. Establish literal expected paisa for positive/negative ties, values immediately beside ties, quantity times rate, surcharge, correction deltas, large safe values, NaN/infinity and overflow. Proposed policy: an explicit decimal-to-paisa converter with HALF_UP, consistent with several current read paths. Confirm the frozen business specification before changing any calculation; retain canonical integer-paisa UI/API values. Preserve existing stored facts; no historical rewrite, Double-to-decimal database migration or quantity precision change.

Boundary: NativeSaleFacade, existing affected facade money conversion helpers, relevant domain correction comparison if the edge case proves it needs changing, and a small common converter only if needed. Select literal sale/collection/expense/correction checks plus new boundary cases, not all facade suites. Stop when the affected paths agree on the selected rule and pre-existing ordinary cases still match literal ledger rows.

**1b Drawings (B3):** first produce a small domain amendment covering erroneous amount/account, cancellation and a genuine return of previously withdrawn cash. Recommend a linked append-only correction/cancellation for a recording mistake; actual returned cash must be labelled distinctly. Record actor, reason, original UID, date and corrected truth; protect repeated attempts, closed-date policy, roles and cash-only movement. Determine the smallest event/trigger representation, inspect backup/sync/report compatibility, and keep drawings out of operating expense/profit.

Boundary: OwnerDrawing.kt, Correction.kt, Drawing facade/UI or existing Records correction UI, exact DatabaseSql/migration guard, sync validation/serialization and backup compatibility needed for that event. Frozen specification/SQL files remain unchanged; approved extension must have a reviewed migration instead of weakening checks. Do not merely allow positive OWNER_DRAWING rows indiscriminately. Tests: wrong role, closed date, duplicate correction/cancel, full and partial return semantics, injected rollback, cash impact, no P&L impact, export/restore and UID replay on bundled driver. Stop if representation requires broader event redesign: deliver the concrete amendment, not an improvised money workaround.

## Batch 2 — complete profile receiver behavior (B1)

Execution update (2026-10-12): [B1a five-family receiver](../../2026-10-12/profile-receiver/B1A_RESULT.md) and [B1b controlled catch-up](../../2026-10-12/profile-receiver/B1B_RESULT.md) are complete at the bounded QA scope. The ordinary cursor was not reset, queued local edits remained protected, and no server RPC changed. Batch 3 planning receiving remains separate.

Boundary: SyncApplyEngine product/account/profile application, profile DAOs/entities/read validation if necessary, SyncEnvelopeFactory/SyncMigrationSql only where a documented shape/capture dependency requires it. Existing sender envelopes and parent triggers are retained. Five families: Product, Customer, Vendor, Salary/Employee, Operating Expense/category. No people-screen redesign in this batch.

- Define fields absent versus explicit null versus profile object. Legacy missing satellites must not erase complete local profiles; required fields cannot silently become defaults. Validate category, unit, price/limit/activity shape using existing rules.
- Upsert the complete satellite on a new and existing UID; remap parent IDs on each database. Apply parent+child in the existing atomic record boundary. Preserve pending-local-edit, last accepted version, audit and no-push-echo behavior.
- Verify archive/restore, clearing optional fields, rate edits, active flags and subsequent catalog/people reads on a fresh receiver and receiver with different numeric IDs.
- Define controlled full replay/catch-up for earlier skipped profile content. Exercise in isolated data first; ordinary enrollment/cursor/history cannot be reset as an incidental repair. Mutable master-data replay must not overwrite queued local work.

Tests: existing SyncOutboxCaptureTest.profileSave_reenqueuesParentMasterRow, affected SyncApplyEngine/SyncRepairBoundary/SyncIntegrity methods and new parameterized five-family insert/update/replay/rollback/legacy fixtures. Stop when all five converge under the selected scenarios and old missing-profile payloads remain compatible. No schema or live RPC migration assumed unless the field contract proves one is needed.

## Batch 3 — planning receiver and catch-up (B2, B10)

Boundary: budget_plans and collections_targets dispatch/application, their existing DAOs, UID remapping/validation. Preserve append-only supersession and archive, dates, amounts and rate snapshots. Missing supersedes_uid dependencies park then retry; replay twice is idempotent. Differing local numeric IDs and reversed parent arrival must work. Define older-client behavior before deployment; do not call mixed-version receivers fully convergent.

Tests: new budget/target round-trip/create/supersede/archive/late-parent/conflict/rollback fixtures, current capture test deliveryLink_attendanceArchive_budgetArchive_areCaptured where touched, classified restore/rebaseline cases including planning-only restore. If B10 does not reproduce, close it with the narrow evidence; do not invent a repair. Stop when both families apply complete chains and the controlled replay restores old consumed rows without deleting unsynced work.

## Batch 4 — security setup and grouping (U3)

Execution update (2026-10-12): [batch 4](../../2026-10-12/personal-security-menu/BATCH4_RESULT.md) is complete at bounded QA scope on S24. Actual biometric prompt appearance/cancel remains in phase 5 phone acceptance.

Five More personal-security sections only, plus a shared presentation helper if useful. Align PIN setup to the existing 4–12-digit core rule, numeric/leading-zero behavior and full-passphrase confirmation. Group enrolled status separately from passphrase/PIN/phone-security controls. Reuse current body roles and information surfaces; preserve keypad/routing, Android prompt, crypto, retry/lockout and credential storage.

Tests: selected actual five-role security render/navigation/setup cases, lengths 3/4/12/13, nonnumeric/leading zero, wrong confirmation, busy/error/cancel and long status at light/night/100/150/200%. Verify actual prompt in the later phone acceptance; controlled gateway tests remain labelled. Stop when five roles match. Do not implement central revocation or credentials-on-server here.

## Batch 5 — records/product hierarchy and details (U1 + part U4; B6 as a separate subunit)

Execution update (2026-10-13): [5a B6 cashier Activity expenses](../../2026-10-12/cashier-activity-expenses/BATCH5A_RESULT.md) and [5b transaction/product presentation](../../2026-10-12/transaction-product-presentation/BATCH5B_RESULT.md) are complete at bounded QA scope on S24. Begin batch 6 people separately; do not reopen expense data or these five presentation families without a reproduced regression.

First **5a B6** adds only owned expenses to Cashier Activity list/detail with expense category, cash account, amount and actor checks; no broader shop financial disclosure. Existing sale/collection permission/date/receipt behavior remains. NativeActivityFacade and required presentation/contract sites only; confirm no other actor's expense appears. This is a data-boundary change and must not be hidden inside styling.

Then **5b presentation** covers CashierActivityScreen, OwnerRecordsScreen, OwnerProductsScreen list/detail, CashierSaleScreen product choices/receipt and DeliveriesScreen DeliveryCard. One consistent scan hierarchy for title, metric, unit, reference/time and status; record/product details get grouped facts once. Do not add product identities missing from summary models or new filters/pagination.

Before/after captures: representative transaction, long product with selected/unselected state, corrected/archived row, delivered/pending state and detail. Default/light/night plus one 150/200% stress fixture per shared pattern; full units/sign/currency, no ellipsis for essential values, stable chrome, actual callback. Select affected row/detail/selector contracts; no whole workflow classes unless every method is affected. Stop at these five files/families.

## Batch 6 — people hierarchy and details (U2 + remaining U4)

OwnerCustomersScreen, OwnerSuppliersScreen, OwnerEmployeesScreen and OwnerUsersRolesScreen; Admin/Manager delegates inherit once. Group detail facts with NativeInformationSection; list name and balance/action/contact/status have distinct roles. Preserve archived/not-set/disabled/setup-required state, permissions and forms. No schema/identity migration, new people features or employee-to-login conflation.

Tests: representative row+detail callbacks and state contracts for each family, long names/contact/status and large signed balances in light/night/100/150/200%, delegated role access. Stop when four families are accounted for; reused detail work from batch 5 is not reapplied.

## Batch 7 — management grouping (U5 and small presentation tails)

OwnerBackupScreen, OwnerSyncScreen, OwnerCostingPlansScreen, OwnerCostingPolicyScreen, OwnerReportsScreen and OwnerSecurityHistoryScreen; only mapped status/fact/list/detail/total helpers. Normalize Sync label/value emphasis and long-value reflow, remove redundant dividers outside outlined rows, replace residual Backup navigation wording through existing callbacks. Inventory remaining failure-pane variants once and migrate only these touched screens; others retain a named later item. Overview balances/Operator Today long text and optional blind-count receipt are capture-and-decide tails, not automatic redesign.

Tests: affected loading/empty/error/retry/not-set/recovery-required/held/long-value states and existing action/eligibility semantics. Verify labelled disabled/unavailable actions and exposed headings/facts, no merge of a whole section into one announcement. No backup execution, sync reset, report math, cloud provider, form-validation or money changes. Stop when each mapped state is migrated or deliberately retained with a reason.

## Batch 8 — access and recovery, separate bounded subruns

**8a Protected reset policy (B4):** proposed server policy is Owner-only reset of Owner/Admin; Admin reset restricted to ordinary lower roles in the same business. Prepare caller/target matrix, stale/demoted caller, concurrent role changes, disabled/terminated/unlinked target and no-mutation-on-denial tests. Inspect Viewer route, CLI and RPC contract surfaces so alternate supported administration paths cannot bypass the policy. Keep service secrets server-side. Coordinate privileged Auth mutation with authoritative target/caller policy; a browser-hidden button or stale target lookup is not sufficient. Deliver source/tests and concrete deployment diff before publication. No real credentials reset for testing.

**8b Recovery (O1, A2, O6):** read-only remote source/workflow visibility preflight, compare exact prepared files, verify snapshot completeness/pagination and two repeated runs. Rehearse identity restore with synthetic identities in a disposable environment, preserving stable business/shop/staff IDs, generating new credentials by the documented recovery contract and excluding terminated identities. Keep account/server identity recovery separate from app ledger and Drive objects. Rehearse app export/restore on an isolated destination and compare full records/children/profiles. Document actual backup age, failed-run detection, off-site recovery access and signing-key restore steps without printing secrets. No production wipe, ordinary-app restore or wholesale DEV identity reset.

**8c Operations (O2–O5, B9, A7):** check actual project/repository/deployment state first; don't re-publish already live Viewer changes or apply old migration again. Configure real shop device as needed with cashier transport/per-person local PINs, preserve offline business entry, document lock versus disconnect versus revoke. Provider policy/patch/deletion choices are separate concrete proposals. Any needed actual Auth/admin acceptance uses authorized disposable accounts; existing staff stay untouched.

**Identity expansion B7** gets a design boundary only: device credential lifecycle, by-device/by-person revocation, activation/linking, offline grace/break-glass and false-denial recovery. Do not simply flip ENFORCE_LINK_STATES. Implement it later if the deployment requires centrally revocable local access; report the limitation otherwise. **Conflict attention B8** can be a distinct small sync-notification subrun: deduplicated action-required alert from persistent review state, notification-disabled in-app fallback, no success spam, preserved review route.

## Batch 9 — shop-day and multi-device acceptance

**9a A1:** synthetic dry-run can begin earlier to expose practical blockers. Formal uncoached acceptance uses the final intended device/roles and a fixture with literal independently prepared cash, stock, receivable/payable expectations. Separate automated saved-domain proof from human task comprehension. Do not coach staff past an unclear UI and count it as success.

Minimum scenario: opening state, supplier receipt, production, cash/credit sales, collection, expense, delivery, count/variance reason, close, next date, correction and Owner reopen. For purchase/production, derive literal expected cost/yield from the frozen specification before generating transactions. The small ledger slice in REHEARSAL.md supplies exact sale/collection/expense/correction expectations; it is not complete production/costing acceptance.

**9b A3:** only for two writable phones, after 2/3: different local IDs, same-shop owner/counter roles; master/profile/planning edits, ledger writes, offline writes, close refusal/reopen/recount/reclose, repeat sync and interrupted/retried recovery. Compare UID-normalized complete event headers and financial/inventory/actor/other children, plus stock/cash/accounts/profile/planning state, not sequence counters alone. Start with two isolated databases on A06; a real two-physical-device session is a distinct final gate, not required to unblock single-device planning. S24 is not requested now.

**9c A4/A9:** representative actual phone PIN/biometric cancel/Back and enrolled account isolation; personally feel haptics and inspect known route re-entry, keyboard, text scale and TalkBack on used shop tasks. These observations cannot be replaced by screenshot existence. Historical affected records A8 are inspected/recovered only if the owner identifies affected UIDs and authoritative history.

## Batch 10 — freeze and ship the accepted candidate

A5/A6/A10: stop feature work, capture source/test selections and hashes, commit/push current accepted changes, version/sign the ordinary APK and verify certificate/package identity, upgrade continuity and rollback plan. Preserve the installed shop database; don't install instrumentation over ordinary production. Deliver release report with exact APK/source provenance and accepted limitations.

Validation is affected-only during batches. The earlier formal full-checkpoint debt remains scheduled once here, on frozen source, if selected for release acceptance. The user's scoped-run preference takes precedence; if the broad gate is intentionally omitted, report that evidence limit and do not fabricate a full PASS. Failed cases get one focused investigation/rerun and honest original-run status; no repeated whole-suite chase for a green label.

Measure shrinking/profile optimization only if an actual release budget fails or it is separately selected. Do not combine dependency upgrades, dormant-code deletion or wide-layout work into release packaging.

## Conditional launch paths

| Path | Gates | What may wait |
|---|---|---|
| Synthetic/supervised trial | Intended-role access, existing supported operations, independent fixture totals, isolated destination; restrict unresolved drawing/admin capabilities | All future features; incomplete profile sync can be explicitly outside a one-device dry-run |
| One-device live pilot alongside existing records | Applicable money/drawing/access fixes; current ledger recovery and server recovery plan; uncoached day acceptance; current accepted signed build and explicit evidence limits | Two-phone profile/planning and central identity expansion if not relied upon; nonblocking visual tails; future wishlist |
| Two writable phones | One-device gates plus batches 2/3/9b, conflict handling/identity trust boundaries and matched compatible builds | Advanced reminders/general retail/cleanup; must not assume local-only settings converge |
| Wider release | Pilot reconciliations plus current migration/security/provider/privacy/provenance/accessibility/recovery/performance closure | Only explicitly disclosed unsupported features |

Finishing all design passes is a project commitment; it is not proof that the app is ready for business records. Do not bypass money or recovery gates because the screens look finished.

## Ownership and stop rules

- Agent can automate source changes, synthetic tests, hashes, fixture capture, isolated comparisons and reviewable release preparation.
- Owner/staff provide uncoached task execution, felt haptic/appearance judgment, recovery-secret custody and any authoritative historical-data choice. Hardware access is needed only for the relevant physical gate.
- This plan does not authorize production deployment, account reset, identity wipe, destructive restore or recovery of guessed history. Complete concrete reviewable work first when an action needs separate authorization.
- Every row in ITEM_MAP.csv has a batch or later queue. A verify-first row closes as not-reproduced if its bounded check finds no issue; uncertain old labels do not generate speculative patches.
