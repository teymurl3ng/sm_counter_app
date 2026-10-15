# Phase 4: customer payment terms, credit status and review history

15 October 2026. Source baseline: `f186c3fcbbfb751e3e8d61117fe667f717efa976`. **Planning only; no implementation or schema/cloud change.** Parent: [six-phase plan](../../../NEXT_WORK_PLAN_2026-10-15.md). This is phase 4/L2, not new phases in the roadmap.

## Owner input and bounded outcome

The owner clarified: each ordinary credit sale is payable within seven days. Bills are usually sent on Sunday, manually and not strictly. Individual customers must also support monthly/custom payment periods. Payment deadline and bill/reminder schedule are separate concepts.

Outcome: staff can understand a customer's current unpaid-credit status under that customer's agreed terms; the Owner can change future-sale terms, review/clear a manual risk flag and inspect the existing account history. No statements or sending are implemented here. Sunday billing is retained for phases 5–6; it does not extend or reset seven-day deadlines.

The earlier no-due-date-fields direction remains: calculate deadlines from recorded sale date plus its agreed term. **The new flexibility does require stored contractual metadata**, separately from financial movements. Do not claim zero schema change for this phase or hardcode seven days into every future sale. Do not add editable per-sale due dates, payment installments or a general contract builder.

## Source baseline and corrections to old assumptions

Paths below are under `phase18/app/src/main/java/com/saaf/milkshop/`.

| Verified implementation | Consequence |
|---|---|
| `domain/CustomerCreditAging.kt` reconstructs effective correction families, allocates collections FIFO and returns oldest known age plus unknown opening debt. It does not expose each remaining sale lot or its terms. | Extend that reconciled allocation result; do not create a second balance or infer overdue status from the oldest sale alone. An older monthly sale may be current while a newer seven-day sale is overdue. |
| `application/owner/NativeOwnerCustomersFacade.kt` and `nativeui/owner/OwnerCustomersScreen.kt` provide customer list/detail/edit; detail now contains unpaid age. Customer master-data access currently includes Owner, Manager and Admin. | Add status/filter/history in these existing destinations. Apply a separate explicit Owner-only write rule for payment terms/manual flags; do not accidentally grant those writes to every master-data role. |
| `application/owner/NativeOwnerRecordsFacade.kt::loadLedger` already returns canonical customer movements and running balances. | Reuse the ledger in customer detail navigation with a return path, retaining corrections and amounts. No duplicate transaction history. |
| `sync/SyncApplyEngine.kt::applyAccountProfile` applies received customer profile data for both inserted and updated accounts; `SyncEnvelopeFactory.kt` embeds profile data in account envelopes. | The 2 October claim that profile edits never flow down is outdated. Separate metadata is selected for immutable agreement/audit semantics, not to work around that old defect. |
| `SyncApplyEngine.applyOne` explicitly dispatches recognized table names and skips others. | A new table cannot be assumed to sync. Define envelope, apply/park/dependency, bootstrap/catch-up, server authorization and old-client behavior before enabling it. A cursor advancing past an unknown record is a compatibility risk, not an implemented migration solution. |
| Room database version is 17. BackupManager's backup schema version is 12 and newer backup schemas are refused; backup export/restore use explicit collections. | Room and backup schema numbers are different. Add the new collections and validation deliberately; bump the relevant versions without conflating them. No destructive migration. |

Only source and synthetic prior evidence were inspected. No live customer/shop/server catalog was read for this plan. Existing phase 3 checks remain credited, not rerun.

## Targeted research and adaptation

- [Odoo payment terms](https://www.odoo.com/documentation/19.0/applications/finance/accounting/customer_invoices/payment_terms.html) distinguishes date-based payment rules. [QuickBooks payment terms](https://quickbooks.intuit.com/learn-support/en-ca/help-article/pay-bills/set-payment-terms/L8BhXHl35_CA_en_CA) distinguishes days-after-invoice from date-driven terms. Adapt explicit terms; do not silently treat a calendar month as thirty days.
- [Dynamics customer aging](https://learn.microsoft.com/en-us/dynamics365/finance/accounts-receivable/customer-aging) supports configurable aging intervals, including seven-day intervals, and transaction detail. Adapt transparent computed status and access to the underlying history.
- [Odoo follow-up](https://www.odoo.com/documentation/17.0/applications/finance/accounting/payments/follow_up.html) separates configurable overdue follow-up levels from a manually selected debtor trust level. Adapt computed current-debt status plus a visibly distinct manual risk flag. Do not claim debt age proves historical payment reliability or copy automated messaging into this phase.
- [Supabase RLS](https://supabase.com/docs/guides/database/postgres/row-level-security) requires both grants and row policies for exposed data. Future metadata endpoints must enforce shop and actual server actor authority; local actor labels are audit descriptions, not server-verified staff identity.

These establish patterns, **not universal Good/Watch/Late/Bad thresholds**. The proposed thresholds below are an operating policy for this app, not an industry/accounting standard. Official Odoo page opening timed out; relevant documentation excerpts were available through search. Supabase's markdown changelog endpoint failed; its official changelog search index was checked. No platform implementation is attempted; recheck relevant current docs/changelog before actual migrations.

## Proposed exact payment terms

One default, one per-customer override, and an immutable term snapshot for each new credit sale:

| Choice | Defined meaning |
|---|---|
| Weekly / 7 days — default | Due on sale business date plus 7 calendar days. Sale on 1 October is due through 8 October; overdue from 9 October. |
| Monthly / 1 calendar month | Due on the same date in the following month, clamped to that month's last day. 31 January → 28/29 February. This is not month-end billing. |
| Custom days | Whole 1–365 calendar days. A 30-day shortcut is explicitly labelled 30 days, not calendar month. No free-form formula or installments. |

Owner changes apply to **future new credit sales**, with that effect stated before save. Existing credit sales keep their term snapshot. The metadata uses a versioned term kind and value; the calculation derives dates rather than stores a second editable deadline. Corrections retain their original root sale's term, including nonfinancial corrections. New credit terms are recorded atomically with the sale; a failed metadata write rolls back the entire sale. Paid-now sales do not need this snapshot.

Legacy credit sales lacking snapshots use the confirmed seven-day default with a visible legacy/default explanation. Missing data from a malformed/new-contract sale is not silently treated as legacy. Old backups contain no historical evidence for monthly terms, so none is invented. Changing terms does not retroactively reclassify an earlier sale. Re-aging historical agreements is excluded; it would need a separate explicit reviewed action.

FIFO allocation remains oldest sale date then stable root UID, with unknown-age opening debt consumed first. It does not become earliest-due-date allocation when terms vary. Evaluate **every remaining visible-paisa lot** against its own term; reconcile totals at the same canonical money boundary established in phase 3. Keep aggregate current debt, overdue amount, oldest overdue days and earliest upcoming deadline as derived read results only.

## Proposed status policy — review before implementation

Use the heading **Current credit status**, not a claim about lifelong reliability. No automatic block or delivery pause.

| Condition | Status and explanation |
|---|---|
| Balance zero / negative | Settled / Advance. Neither is evidence of historical good payment behavior. |
| Positive debt with incomplete allocation, unknown opening age, missing/unsupported terms or uncertain date | Needs review, with the specific coverage reason. Preserve any known overdue amount as partial information; never show a reassuring Good for an incompletely classified account. |
| Complete debt coverage; every unpaid deadline is at least 3 days away | Good — within agreed terms. |
| Complete debt coverage; no overdue debt; an unpaid deadline is today or within 2 days | Watch — due soon / due today. |
| Any debt is 1–14 days overdue | Late — show actual overdue amount and oldest overdue days. |
| Any debt is at least 15 days overdue | Bad — seriously overdue, with an explicit automatic-status explanation. This threshold is proposed, not yet an owner-approved rule. |

Manual Bad/risk review is **separate**: show “Bad — Owner flagged” and its reason/history without falsifying current debt/automatic status. A paid-off flagged customer still shows Settled/Advance plus the retained Owner flag. Payment does not silently clear a judgment flag. Clearing the flag reveals the computed status; it cannot make genuinely overdue debt Good.

Proposed controls: All / Watch / Late / Bad / Needs review filters inside Customers; existing search/archived behavior stays. Bad includes automatic Bad or active manual flag; flags on settled accounts remain findable. Each row has a compact text label plus a restrained theme-aware cue; no full-row warning paint or color-only meaning. Detail shows terms, balance/overdue explanation, flag reason and links to account history/override history. Loading/error updates only affected content; preserve shared header/bottom geometry. Sale gets a small selected-customer status/terms summary, retaining phase 3's existing advisory-limit confirmation. No second warning dialog solely for status.

Compute a list snapshot with a bounded batch read and shared allocation code; do not launch one full-history query for every recomposing row. Keep calculations off the UI thread, cancel superseded filter/session reads and expose incomplete/pending state honestly. No performance project or persisted financial cache is included.

## Metadata, roles and consistency contract

Pin these before coding; exact SQL/DTO names can follow existing conventions:

1. Customer future-sale terms are a small separate versioned record keyed by stable customer UID, with actor/time and revision. Concurrent edits must surface a revision conflict for Owner review; no phone-clock last-writer-wins. A pending local edit remains visibly pending. Local ownership/permissions and server shop/owner authorization are separate checks.
2. Sale term snapshots are immutable metadata keyed by root SALE event UID; reference customer UID and the actual selected term/version. A retry with the same identity/body is idempotent; a differing body is a conflict. Missing dependencies park atomically. Audit labels do not assert independently verified clerk identity.
3. Manual flags use append-only SET and CLEAR actions with stable action UIDs, customer UID, reason, actor and trusted timestamp/business date. CLEAR references the SET actions the Owner actually reviewed, rather than deleting history or overwriting one boolean. A concurrent/unseen SET remains active until explicitly cleared. Repeated transport of an identical action is idempotent; conflicting bodies are rejected. Require a reason for setting and clearing, and guard duplicate taps.
4. Term edits/flag actions are Owner-only writes. Existing authorized customer readers may see status/history; Cashier sees only the selected Sale summary and cannot edit policy/flags. Do not add a new staff identity platform. Preserve original financial permissions and sale/closed-day rules.
5. Add Room migration, outbox/envelopes, server validation/shop isolation, pull apply/park/replay and bootstrap/catch-up for exactly these metadata types. No generic sync rewrite. Specify a capability/version rollout gate: unsupported posting clients cannot silently create unrecorded seven-day sales for monthly customers. Do not enable advanced terms until the intended posting phones are upgraded and compatibility behavior is verified. Freeze the exact server handshake against the current endpoints before implementation; if it needs broader identity/enrollment changes, stop and re-scope that boundary instead of inventing them.
6. Backup export/restore preserves terms, immutable snapshots and full flag history. Older snapshots import explicit legacy defaults, not invented flags; newer snapshots require an appropriate reader. Restore/rebaseline must neither resurrect cleared flags nor erase a concurrent SET. Preserve stable references across different local IDs and local outbox state.

## Three implementation checkpoints inside phase 4

- [ ] **Rules and data contract:** confirm proposed status bands/monthly meaning/future-only changes; extend the existing allocation result, add terms/snapshot/flag metadata, migration and exact compatibility contract. Stop before UI rollout if compatibility cannot be kept bounded.
- [ ] **Existing screens:** customer terms editor, list status/filters, detail explanations, reused ledger and flag audit actions; selected Sale summary. No new top-level destination or global visual overhaul.
- [ ] **Affected acceptance:** focused calculation/permission/migration/sync/restore tests, then synthetic two-phone metadata propagation and the affected light/dark/large-text screens. Reconcile the same six-phase checklist and stop.

Two phones are needed only for the named convergence/rollout acceptance, not to write the plan or implement calculations. A disconnected phone is not grounds to broaden QA or claim convergence without evidence.

## Fixed examples and bounded acceptance

- Seven-day sale on 1 October: Good through 5 October, Watch 6–8 October, Late 9–22 October, automatic Bad from 23 October under the proposed bands. Test every boundary and business date rollover without wall-clock guesses.
- Monthly 31 January: deadline 28 February in a non-leap year, 29 February in a leap year. Thirty-day custom terms must differ when the calendar requires it.
- Older monthly sale plus newer seven-day sale: categorize using the unpaid overdue lot, not oldest overall age. Collections still follow the existing FIFO agreement; compare an independently calculated oracle.
- Customer switches from seven days to monthly: an earlier sale retains seven days; a subsequent sale gets monthly; correction retains its root's agreement. Repeated/stale UI saves never silently select different terms.
- Partial/full collection, advance, raw sub-paisa cancellation, unknown opening debt and missing ancestry retain phase 3's correct financial result and explicit coverage limits.
- Owner set/clear, expired/non-Owner denial, duplicate taps, offline retry, remote concurrent SET versus CLEAR, duplicate body and conflicting UID body have explicit expected results. Both phones must converge without losing audit history.
- New metadata before its customer/sale dependency parks then replays. Upgrade catch-up, unsupported/old client, old/new backup, restore/rebaseline and mixed local IDs preserve the same terms/flags without altering ledger totals.
- Affected filters, long/duplicate customer names, large text, dark colors, loading/error and return-from-ledger preserve navigation/header bounds. Do not rerun hosted recovery, all shop-day tests, haptics or whole-app animation checks.

Pin the exact selected test methods once the data contract is implemented; do not run entire historical test classes by default. Use isolated QA records/accounts/projects only; no real customer messages or live shop transactions.

## Judgment and stop boundary

The [judgment hook](hook-output.json) is a fixed reminder, not code validation. Reject hardcoded seven-day-only categories, treating bill day as due day, calling unknown debt Good, changing historical deadlines when a customer setting changes, storing derived balances/scores, overwriting flag history, or claiming the new table syncs automatically. Reject a generic workflow/terms builder or complete risk-scoring system.

The proposed future-only term snapshots are a deliberate bounded adjustment to the old blueprint required by the owner's new per-customer-period request. The exact category cutoffs and monthly definition remain **proposals for review**, not facts supplied by industry or previously approved business rules. Implementation has not started. Phases 1 and 5–6 remain pending; phases 2–3 stay complete. Sunday statement preparation and scheduling are retained for phases 5–6, with sending authorization/cost/automatic-SMS decisions still distinct.
