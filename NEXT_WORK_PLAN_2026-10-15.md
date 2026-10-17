# New plan: unfinished work only

Date: 15 October 2026. Status: phases 2–6 complete at their documented technical scopes. Phase1 private/human handover remains open; automatic SMS is a separate deferred decision.

**Execution update:** Phases2–5 and the Owner-reminder implementation in phase6 are complete at their documented technical scopes and included in signed0.6.0 on both phones. Both phones additionally have verified0.6.1; A06 has accepted Drive connection/upload/inspection. Phase1 retains private/human recovery and operating acceptance. Automatic customer SMS is a separate deferred decision; no sender is configured. B8 audit-only conflict attention stays a separate parked boundary.

**17 October delivery update (supersedes older delivery wording):** signed0.6.0/code9 includes accepted phase5, phase6 Owner reminders and the wording pass. ✅ ~~Release/signature/provenance, three upgrade/recovery checks, both ordinary phone updates and A06 account presence~~ complete. S24 reconnected and its exact APK/installation/launch checks passed. Signed-in A06 Overview and the existing private Drive signing backup are now verified; recovered key/settings passed a signing challenge. Private known-record/live-sync acceptance, staff outcomes and encrypted shop-data backup/recovery remain phase1. [Current handover](FINAL_HANDOVER.md), [exact result/pending actions](audit/2026-10-17/final-delivery/RESULT.md). Automatic customer SMS remains unconfigured and separate.

**17 October Drive acceptance update:** ✅ ~~A06 signed0.6.1 repair, approved Google Production publication, private consent, encrypted upload/listing and non-destructive local-key inspection.~~ Security state included; Restore remained disabled and no restore occurred. Hidden app-data shop backup is separate from the visible signing folder. S24 same-artifact update, retained installation and ordinary launch now pass. Independent passphrase-only recovery/custody, known-record/live-sync and staff acceptance remain phase1. [Current result](audit/2026-10-17/drive-release-fix/RESULT.md). Cosmetic brand review is a later note.

**17 October selected-backup UI update:** ✅ ~~Scoped0.6.2 UI repair, three affected checks, signed package and S24 retained-installation/launch verification.~~ This fixes recovery/result placement and accidental local-key retry after success; selecting another backup resets consent. A06's same-artifact UI update and S24 private passphrase recheck remain phase1, alongside existing isolated recovery and private/human acceptance. No new phase or broad audit. [Result](audit/2026-10-17/drive-inspection-ui/RESULT.md).

**Per-phase working rule:** Before each phase is implemented, record its source baseline, targeted primary-source research, independent judgment, exact inclusions/exclusions and affected acceptance checks. This is the owner's 15 October instruction. Planning/research does not count as implementation completion. The first pinned brief is [phase 2's plan](audit/2026-10-15/sync-attention-plan/PLAN.md).

This is the current reading order for unfinished work. It replaces the sequence in the recovered-plan document, not the accepted results or original business decisions. The [detailed recovered inventory](LATER_FEATURE_PLAN_2026-10-15.md) remains the source for each item's origin, exclusions and acceptance details. Completed engineering is not scheduled again.

## Six finite phases

### 1. Finish the existing handover

**Goal:** finish the remaining release participation without restarting accepted engineering.

- [x] ✅ ~~Install matching signed updates on A06 and S24.~~ Both now run 0.5.0/code8 with verified APK hashes, retained installations and native launch. Earlier A06 private login/live sync/sampled records were accepted on 0.4.3; private acceptance of the latest version remains below.
- [x] ✅ ~~Deliver the accepted phase5/Owner-reminder/wording features as signed0.6.0/code9 on A06 and S24, with exact APK and retained installation verification.~~ Private financial-record/live-sync acceptance remains below. This is the existing handover delivery gate, not a new phase.
- Complete the prepared human/staff checks: actual private phone-security unlock, readability, motion/vibration, uncoached shop-day tasks and next supported date observation.
- Confirm independent recovery-secret/signing-backup custody, complete the outstanding recovered-backup acceptance, and hand over the intended staff/device setup using the existing instructions.

**Who:** I handle installation, evidence comparison and technical follow-through. The owner/staff provide private authentication, custody and actual human outcomes. No secrets need to be sent in chat.

**Finish line:** remaining human/setup/custody results are recorded, the intended phones have the accepted version, and the actual operator receives the handover. The real A06 shop setup is already confirmed; do not recreate it.

**Boundary:** this can proceed alongside feature work when people/devices are available, but remains a gate for claiming whole-release acceptance. Do not repeat the completed seven-batch technical checks.

### 2. Make unresolved sync problems stay visible

**Goal:** the owner can tell that something still needs review without remembering a brief notice.

- Show persistent in-app attention for persisted entries already held for an Owner decision. Audited conflicts without that resolution model are a distinct retained B8 follow-up; do not count them as actionable holds or claim whole-conflict coverage.
- Open the existing review screen from that attention, retain it across navigation/restart, and clear it after actual resolution.
- Keep attention usable when Android notifications are denied. Reuse the existing upload-halt notification and review mechanisms where appropriate.

**Who:** I implement and run the affected status/review checks; owner reviews the resulting presentation.

**Finish line:** one unresolved fixture stays visible until resolved, including after restart and with notifications disabled. Existing reconciliation behavior is unchanged.

**Boundary:** no new conflict engine, identity system, ledger rewrite or full sync audit. This is the recommended first coding phase because it closes a recorded operational gap independently of the larger Credit feature.

**Pinned baseline/research/judgment:** [Specific phase 2 brief](audit/2026-10-15/sync-attention-plan/PLAN.md). Selected design is an Owner More badge plus the existing Sync menu/review, using observable local holds and keeping header/bar geometry stable. No new system notification or server change. This narrows the earlier broad conflict wording explicitly; B8 remains partial for audit-only conflict attention.

### 3. Build Credit basics

**Pinned plan:** [Phase 3 baseline, research, judgment and acceptance](audit/2026-10-15/credit-basics-plan/PLAN.md). ✅ ~~Phase 3 planning.~~ ✅ ~~Phase 3 implementation and affected acceptance.~~ [Result](audit/2026-10-15/credit-basics-plan/RESULT.md). The implemented warning includes surcharge, groups correction deltas, preserves aggregate rounding and revalidates advisory confirmation before posting. Weekly category cutoffs remain phase 4.

**Goal:** find the right customer quickly, understand unpaid debt, and get a clear credit-limit warning.

- Search the existing Sale customer picker.
- Calculate unpaid credit and its age from the existing ledger; collections pay off the oldest credit first.
- Define how opening debt, advances and corrections affect that calculation. Clearly identify debt whose original age is unknown.
- Show current/projected debt and warn above the configured limit. Allow the operator to continue deliberately; the limit stays advisory.

**Who:** I implement the calculation/picker/warning and focused checks.

**Finish line:** independently calculated examples agree, duplicate/long customer names are handled, and warning/cancel/continue never creates an unintended or duplicate sale.

**Boundary:** no categories, new score tables, bills, messages or broader product accounting. This is L1 in the detailed plan.

### 4. Show which credit customers need attention

**Implementation update:** ✅ ~~Phase 4 rules, data/storage/permissions/backup, DEV protocol, Android transport, Customer/Sale/history screens, pending review, focused QA, real two-phone recovery and PROD/signed rollout.~~ [Finish result](audit/2026-10-15/credit-status-plan/FINISH_RESULT.md). PROD has 39 passing rolled-back checks after exact approval; both ordinary phones have signed0.5.0/code8. No real shop activated. Human/private handover stays phase1.

**Goal:** make payment status clear in customer lists and history.

- Define Good/Watch/Late/Bad around each sale’s agreed payment period: seven-day default, per-customer monthly/custom terms. Retain the agreed terms on each sale; do not silently change historical deadlines.
- Seven days runs from each sale; Sunday is the usual bill-sending day, not a common deadline. Confirm the proposed category bands and explicit calendar-month/30-day distinction before enabling labels.
- Show status with words and a visual cue; provide Late/Bad views or filters in existing navigation.
- Add the original manual Bad override with authorized set/clear actions and history. Extend the existing customer ledger with an aging explanation.
- Include compatibility, two-phone sync and backup/restore for any new metadata.

**Who:** I propose the exact weekly policy and implementation; the owner confirms the business interpretation. I handle focused technical verification.

**Finish line:** boundaries and overrides behave consistently after payment/correction, on both phones and after restore. No duplicate stored balance or derived score becomes authoritative.

**Boundary:** no automatic messaging or new identity platform. This is L2 in the detailed plan.

### 5. Add clear bills and manual reminders

**Pinned plan:** [Baseline, research, judgment and three bounded pieces](audit/2026-10-15/statements-plan/PLAN.md). ✅ Planning complete. ✅ ~~Statement snapshot/preview and customer-detail fit~~ accepted with13 unique affected A06 methods; [result](audit/2026-10-15/statements-plan/PREVIEW_RESULT.md). ✅ ~~PDF/image sharing~~ is accepted at [piece 2](audit/2026-10-15/statements-plan/EXPORT_RESULT.md), with 15 unique affected methods across scoped runs. ✅ ~~Manual drafts/truthful local history~~ is accepted at [piece 3](audit/2026-10-15/statements-plan/MANUAL_RESULT.md), with 21 unique affected methods. Phase 5 is complete at implementation/isolated-QA scope; ordinary signed delivery remains phase 1. No scheduling, cloud migration or real customer sending belongs to this phase.

**UI fit:** [Placement/reflow plan](audit/2026-10-15/statements-plan/UI_FIT.md) and [clickable study](audit/2026-10-15/statements-plan/UI_PREVIEW.html): locally regroup customer detail, add one Statements & reminders entry, and keep Statement/Reminder in the existing Customers host. Same native design material/components; no new tab or global redesign.

**Goal:** give a customer a readable statement and deliberately prepare a reminder.

- Generate a previewable PDF and image statement from authoritative records, with shop/customer, activity, payments and outstanding totals.
- Include the retained product-mix visual treatment with a readable text fallback; keep artwork subordinate to accurate amounts.
- Share through Android and open prepared WhatsApp/SMS messages for one chosen customer.
- Handle missing phone numbers/apps and cancellation. Record only observable actions; opening another app does not prove delivery.

**Who:** I implement/export-check the output and handoff; owner reviews the wording and design. QA does not send real customer messages.

**Finish line:** preview/export totals match the ledger and the share/cancel/missing-app paths work without changing financial records.

**Boundary:** no automatic SMS, bulk sending, payment links, provider API or tax-invoice claim. This is L3 in the detailed plan.

### 6. Add reminder automation

**Pinned research/plan:** [Established-service research](audit/2026-10-16/reminder-automation-plan/RESEARCH.md), [finite scope](audit/2026-10-16/reminder-automation-plan/PLAN.md), [native UI placement](audit/2026-10-16/reminder-automation-plan/UI_PLAN.md) and [post-plan independent judgment](audit/2026-10-16/reminder-automation-plan/JUDGMENT.md). ✅ Planning complete; ✅ ~~A rules/read model/injected UI foundation~~ accepted in [A result](audit/2026-10-16/reminder-automation-plan/A_RESULT.md), with 13 unique passing affected methods across scoped runs. No ordinary route yet; B shared coordination/scheduling and C decision remain. Exactly two build pieces (rules/UI, then shared coordination/scheduling) and one explicit automatic-SMS decision. No new phase, provider purchase or customer sending. Preferred conditional unattended route is a central provider dispatcher; a dedicated SIM remains an assessed alternative, not a proven invalid route.

**Current status:** ✅ ~~Owner-reminder implementation, DEV/two-phone acceptance, exact PROD publication and signed ordinary delivery.~~ Automatic customer SMS remains a separate unconfigured/deferred decision; the original research/checkpoint wording below is historical. No paid provider or sender activation is included in current handover.

**Goal:** notify the owner about customers needing attention without checking every account manually.

- Add periodic detection, local owner notifications, visible in-app pending attention and reminder history.
- Use the agreed weekly policy; prevent duplicate/stale actions across retries, two phones and restores.
- Provide opt-in controls and quiet behavior. Do not promise an exact execution time.
- Make a separate explicit decision on the original automatic-SMS subpart: distribution/permission suitability, costs, consent, off-by-default settings and single-sender/retry rules. Do not silently cancel it or assume it is viable.

**Who:** I implement owner-side automation and focused checks; owner participates in any decision enabling customer sending or a paid provider.

**Finish line:** delayed/repeated jobs, denied notifications, changed balances and restore do not create duplicate or misleading reminders; the automatic-SMS decision is recorded distinctly.

**Boundary:** no provider purchase, real customer sending, delivery blocking, payment integration or bulk campaign as part of ordinary QA. Owner reminder automation can be accepted separately from an unresolved automatic-SMS route. This is L4 in the detailed plan.

## Retained work outside the six phases

These items have not been cancelled. They are deliberately unscheduled or conditional, rather than hidden extra tasks inside a phase. Selecting one creates its own bounded scope; listing it does not mean all are promised for the next release.

| Work | Next action when selected |
|---|---|
| Staff/device identity and revocation | Next recommended design brief after the core Credit work: shared cashier device versus named staff, enrollment, lost-device/person revocation, offline access and owner recovery. Existing cashier transport and guarded reset remain credited. Scope MFA/self-service/admin extensions only if needed. |
| Urdu | Select the actual staff flows to translate and check full strings, number/date presentation and layout. |
| General retail/new products | Choose one real product family before designing units, stock, costing, returns or expiry. Dynamic catalog support already exists. |
| WhatsApp Business API, JazzCash/EasyPaisa links, bulk reminders and delivery auto-pause | Separate integration/business-rule decisions beyond Credit v1. |
| History sidecar, divergence detection, price-change audit and historic-close backfill | Select one concrete audit/recovery need per run. Parked-row backup behavior needs re-decision; reopen/recount/reclose remains the current remedy for refused closed-day work. |
| Approval queue | Define an authoritative domain API and approver behavior before designing screens. |
| Backup snapshot viewer | Re-decide its distinct value alongside existing live sync/server Viewer before building. |
| Time display/threshold settings and advanced security/employee administration | Optional additions for an actual operating need; ordinary date handling and salary/attendance tasks already exist. |
| Receipt/artwork/product-name summary polish and tablet/desktop layouts | Select a small presentation scope. Phone landscape remains excluded. |
| Dormant-code removal, state/DI cleanup, toolchain, preview notes, lint/index/resource/artifact hygiene and stale metadata | Separate maintenance changes with a specific benefit, not a prerequisite to Credit. |
| Ignored host test, costing host coverage, inventory nondeterminism, crash reporting and performance | Reproduce a failure, identify coverage need or measure a named problem before changing code. |
| NRV, old affected feeds, TalkBack, physical power loss, provider hardening/monitoring and public-repo review | Original conditions remain in the detailed inventory. No action unless the relevant trigger/need is established. |
| Costing tab selection at large text and old sign-in transition observation | Verify the specific observation first; no renewed whole-app design or animation crawl. |

**Wording pass selected 17 October:** ✅ ~~Simplify clerk-facing language in the recent Credit, statements/messages and reminder implementation.~~ [Completed scope and 24 distinct affected checks](audit/2026-10-17/clerk-language/RESULT.md). Core meaning, permissions and stored/protocol values are retained. This closes the [earlier wording deferral](audit/2026-10-16/reminder-automation-plan/LATER.md); ordinary signed delivery remains phase 1.

## Rules that keep the plan finite

- Start one coding phase at a time. Phase 1 participation may continue alongside it; do not mark it complete on behalf of staff.
- Before coding, pin that phase's files/behaviors, acceptance examples and affected tests. Clarify any business rule needed for that phase only.
- After its checks pass, stop, report the result and cross out the completed phase. Do not broaden testing without a concrete new failure or change.
- Record unrelated discoveries in a later note. Do not add them to the active phase or silently add more phases.
- The original planning pass performed no implementation. Later phase results record their exact source/QA scope; no cloud mutation or customer contact belongs to phases 2–3.

## Progress

- [ ] 1. Final delivery and handover — ✅ ~~signed0.6.0 on both phones; independent signing-key recovery; signed0.6.1 on both phones; Google Production; private Drive consent/upload/listing/local-key inspection~~. S24 additionally has verified0.6.2 UI repair. A06 same-artifact update, S24 private passphrase recheck, known-record/live-sync, staff outcomes and independent supported recovery/custody remain. [Current handover](FINAL_HANDOVER.md).
- [x] ✅ ~~2. Persistent Owner attention for held sync entries.~~ Accepted at local Owner scope; audit-only conflict policy remains a separate retained B8 boundary. [Result](audit/2026-10-15/sync-attention-plan/RESULT.md).
- [x] ✅ ~~3. Credit basics: aging, customer search and advisory warning.~~ [Result](audit/2026-10-15/credit-basics-plan/RESULT.md). Delivered in signed 0.5.0; private/human acceptance remains phase 1.
- [x] ✅ ~~4. Customer payment terms, credit categories, overrides and history.~~ Implementation, affected checks, two-phone recovery and PROD/signed rollout complete. [Finish result](audit/2026-10-15/credit-status-plan/FINISH_RESULT.md).
- [x] ✅ ~~5. Statement preview, PDF/image sharing, manual WhatsApp/SMS drafts and observable local history.~~ [Final result](audit/2026-10-15/statements-plan/MANUAL_RESULT.md). Implementation/isolated QA complete; ordinary signed delivery/human acceptance remains phase 1.
- [x] ✅ ~~6. Owner reminder automation.~~ Automatic-SMS route/activation remains a separate deferred decision. Accepted checkpoints — ✅ research, baseline, UI plan and independent judgment; ✅ ~~A rules/read model/injected UI foundation~~; ✅ ~~B DEV server-contract checkpoint~~ (72 rolled-back SQL checks); ✅ ~~B Android data/cache/journal/backup checkpoint~~ (13 unique passing affected methods); ✅ ~~B connected workspace/save/recovery checkpoint~~ (18 unique passing affected methods); ✅ ~~B quiet-alert engine/scheduler/notifier and persistent-attention checkpoint~~ (17 unique passing affected methods); ✅ ~~B shared message adapter and reminder-screen hook checkpoint~~ passes17 unique affected methods; [result](audit/2026-10-16/reminder-automation-plan/B_HANDOFF_RESULT.md). ✅ ~~B Customers mode/factory/guarded return integration~~ passes21 unique affected methods on S24; [result](audit/2026-10-16/reminder-automation-plan/B_NAVIGATION_RESULT.md). ✅ ~~B direct customer schedules, shared activity and restored/original-request review~~ pass22 unique affected methods on S24; [result](audit/2026-10-16/reminder-automation-plan/B_REVIEW_RESULT.md). ✅ ~~B contextual notification permission, guarded destination, worker lifecycle and count invalidation wiring~~ pass20 unique affected methods on S24; [result](audit/2026-10-16/reminder-automation-plan/B_INTEGRATION_RESULT.md). ✅ ~~B live DEV/two-phone coordination, recovery and native Activity notification routing~~ are accepted at [the bounded technical checkpoint](audit/2026-10-16/reminder-automation-plan/B_ACCEPTANCE_RESULT.md). ✅ ~~Exact PROD publication and72 rolled-back checks~~ are complete; [result](audit/2026-10-16/reminder-automation-plan/PROD_PUBLICATION_RESULT.md). Signed0.6.0 ordinary delivery is complete on both phones; private/human acceptance remains phase1. C's final automatic-SMS sending-route decision stays deferred. [Data result](audit/2026-10-16/reminder-automation-plan/B_ANDROID_DATA_RESULT.md). [Checkpoint](audit/2026-10-16/reminder-automation-plan/B_RESULT.md).

Use this six-phase progress list in future completion reports. The old [seven-batch plan](audit/2026-10-11/remaining-work-plan/FINISH_BATCHES.md) remains the historical acceptance record; its open parts map to phase 1 here, and its completed parts are not reopened.
