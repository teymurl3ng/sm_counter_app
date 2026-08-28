# Phase 3 — COGS Native Implementation Design

**Status: IMPLEMENTED AND DEVICE-VERIFIED — 2026-08-28**

**Prepared: 2026-08-27**

## 1. Purpose and boundary

This document turns `PHASE3_COGS_PROFITABILITY_DECISION_PROPOSAL.md` into an additive Room/domain/test design. It does not modify the frozen Phase 18 logic specification or SQL blueprint.

Initial delivery covers authoritative historical-cost management reporting for Milk and Yogurt:

- moving-weighted-average acquisition cost;
- component-backed monthly Yogurt conversion cost;
- effective correction replay;
- opening-cost provenance;
- sale COGS;
- spoilage and identified abnormal production loss;
- product gross profit and recorded operating result;
- explicit incomplete-cost statuses;
- `cost_based_management` / `cost_only_no_nrv` disclosure.

Formal NRV write-down/reversal remains deferred under the approved trigger gate. General merchandise acquisition costing and statutory net profit remain out of scope.

## 2. Source truth versus derived truth

Append-only source records are business truth and must be backed up:

1. approved conversion-cost policy versions and their components;
2. opening inventory cost-basis entries and their superseding corrections;
3. yield-policy versions;
4. production-loss assessments and their superseding corrections;
5. the existing events, opening details, spoilage details, inventory logs, and actor/security records.

`inventory_cost_movements` and `cost_projection_state` are derived. They may be deleted and deterministically rebuilt from effective source truth. React never writes either table.

## 3. Additive Room schema

The next schema version is **8**.

### 3.1 `costing_policy_versions`

One immutable approved row per production calendar month:

- `policy_id` primary key;
- `calendar_month` unique `YYYY-MM`;
- `normal_capacity_kg` canonical positive decimal text;
- `review_reason` (`monthly`, `component_change`, `capacity_change`, `process_change`, `quarterly_variance`);
- `notes` optional;
- `approved_at` ISO instant;
- `approved_by_user_id` foreign key to `app_users`.

An existing month is corrected by appending a new version that supersedes the old row. The active row is the unsuperseded leaf. Update/delete triggers protect the audit chain.

### 3.2 `costing_policy_components`

Immutable children of a policy version:

- `policy_id`;
- `component_code` from `culture`, `packaging`, `variable_energy`, `direct_labor`, `fixed_production_overhead`;
- `source_amount_paisa` non-negative integer;
- `basis_quantity_kg` canonical positive decimal text;
- primary key `(policy_id, component_code)`.

The per-kg component rate is `source_amount_paisa / basis_quantity_kg`. Milk material, selling costs, unrelated administration, abnormal waste, and finished-goods storage are rejected. Fixed overhead uses approved normal capacity as its basis quantity. Total conversion cost is calculated with `BigDecimal`; only the event total is rounded to integer paisa.

### 3.3 `inventory_cost_basis_entries`

Append-only historical cost for positive Milk/Yogurt opening quantities:

- `basis_id` primary key;
- `opening_event_id` foreign key to the effective opening event;
- `product_id`;
- `quantity` canonical positive decimal text;
- `total_cost_paisa` positive integer;
- `basis_kind` (`documented_historical`, `owner_estimate`);
- `evidence_notes` required for an estimate;
- `created_at`, `created_by_user_id`;
- `supersedes_basis_id` nullable self-reference.

Only the unsuperseded leaf contributes. Its quantity must equal the effective opening snapshot quantity for that product. A fresh installation cannot complete positive Milk/Yogurt opening state without a basis. An upgraded database receives no synthetic row: profit remains unavailable until an owner deliberately supplies one.

### 3.4 `yield_policy_versions`

Append-only shop-specific alert calibration:

- `yield_policy_id` primary key;
- sample start/end dates;
- accepted batch count;
- median yield ratio and MAD as canonical decimal text;
- lower/upper alert limits as canonical decimal text;
- `effective_from`, `approved_at`, `approved_by_user_id`;
- optional supersedes link.

Approval requires at least 30 accepted batches spanning at least 28 calendar days. The limits are `median ± max(3 × 1.4826 × MAD, 0.03)`. Before calibration, the 85% example is display-only guidance and cannot classify a loss.

### 3.5 `production_loss_assessments`

Append-only owner/admin assessment of an observed production incident:

- `assessment_id` primary key;
- `production_event_id` foreign key to the assessed effective production event;
- `classification` (`ordinary`, `abnormal`);
- `abnormal_milk_liters` canonical non-negative decimal text;
- operational `reason_code` (`spill`, `contamination`, `power_failure`, `equipment_failure`, `measurement_correction`, `other`);
- notes required for `other`;
- `assessed_at`, `assessed_by_user_id`;
- optional supersedes link.

An alert never creates an assessment. An abnormal quantity cannot exceed the event's Milk input. Correcting the production event invalidates an assessment linked to the older event version; reassessment is required only when an abnormal loss is still claimed.

### 3.6 `inventory_cost_movements`

Rebuildable projection rows:

- `movement_id` primary key;
- `source_event_id`, `product_id`, and `movement_role` unique together;
- deterministic sequence number;
- source occurrence/creation timestamps;
- quantity in/out as canonical decimal text;
- `cost_in_paisa`, `cost_out_paisa`, `running_cost_paisa` nullable integers;
- running quantity as canonical decimal text;
- applied conversion/yield/assessment/basis IDs where relevant;
- coverage code;
- movement kind (`opening`, `purchase`, `production_input`, `production_output`, `sale`, `spoilage`, `abnormal_loss`).

Null cost means unavailable, never zero. Projection rows have no user-edit path.

### 3.7 `cost_projection_state`

Singleton rebuild metadata:

- engine version;
- maximum source event/policy/basis/yield/assessment IDs and source counts;
- rebuilt timestamp;
- overall cost-coverage code.

Because all relevant changes append rows, the watermarks detect staleness. A report transaction checks them and rebuilds before returning results.

## 4. Migration 7 → 8

The migration creates only new tables, indexes, constraints, and append-only triggers. It does not:

- edit existing events or ledger rows;
- create artificial purchases, opening events, costs, income, or losses;
- infer opening values;
- enable NRV records;
- change the frozen baseline files.

Existing histories made only from cost-bearing Milk purchases and measured Yogurt production can be replayed. Any positive opening quantity without an approved basis propagates `missing_basis`. Products outside Milk/Yogurt propagate `unsupported_flow`.

## 5. Deterministic replay

The cost engine runs inside one Room write transaction.

1. Select only effective leaf events—events with no correcting child.
2. Sort by `occurred_at`, then `created_at`, then `event_id`.
3. Resolve full corrected truth from the leaf event fields/details, not correction delta ledger rows.
4. Clear and rebuild derived movements.
5. Update the singleton watermarks only after the rebuild succeeds.

Product resolution for a corrected sale walks backward through its correction chain until it finds the stable inventory product reference. Purchase and production use the seeded Milk/Yogurt identities. Spoilage and opening corrections already carry full detail snapshots.

## 6. Pool arithmetic and rounding

Each product pool holds `BigDecimal` quantity plus either a known integer-paisa carrying cost or an unavailable-cost reason.

- Purchase cost in paisa is derived from frozen `amount_owed` using the documented half-up boundary conversion.
- A partial removal releases `carrying cost × removed quantity / quantity before removal`, rounded half-up to paisa.
- The last removal takes the entire residual, guaranteeing zero quantity means zero carrying cost.
- Known stock added to a positive unknown-cost pool remains unknown because a reliable weighted average cannot be calculated.
- Once an unknown pool is fully depleted, a later supported receipt may begin a known pool.
- Quantity and cost underflow aborts the rebuild instead of manufacturing a number.

## 7. Event treatment

### Purchase

Milk receives the frozen purchase `amount_owed`, including inbound delivery fee, exactly once. The cost is carried by received raw litres because raw litres are the physical inventory movement.

### Production

Milk input cost is released at the immediately preceding average cost. Any approved abnormal Milk-equivalent quantity is expensed separately. The remaining Milk input cost plus the applicable monthly conversion cost is added to measured Yogurt output.

If no active policy exists for the production month, production remains valid but Yogurt cost coverage becomes `stale_conversion_policy`; profitability depending on it is withheld.

### Sale

The sold quantity releases preceding average product cost. Product revenue excludes delivery surcharge. Sale value and surcharge remain separate report lines.

### Spoilage

The physical quantity releases preceding average cost to **Cost of spoiled stock**. The user never enters a monetary spoilage rate.

### Opening state

A matched cost-basis entry initializes the pool. A missing or quantity-mismatched basis initializes the quantity with unavailable cost and `missing_basis`.

## 8. Coverage and plain-language reason contract

Stable native codes:

- `complete` → all included historical product costs are known;
- `partial` → only part of the requested result is available;
- `missing_basis` → opening stock cost is missing;
- `unsupported_flow` → the product's stock source has no approved costing workflow;
- `stale_conversion_policy` → the Yogurt-making cost for that month is not approved.

Every report also returns:

- `reporting_basis = cost_based_management`;
- `valuation_scope = cost_only_no_nrv`.

Hybrid screens translate the codes into approved plain shop language. Raw codes remain in the contract/audit layer. Contextual financial education or info-button UI is intentionally deferred; it is not part of this implementation checkpoint.

## 9. Native services

### `CostingPolicyService`

- approve/correct monthly component-backed policy;
- calculate its precise per-kg rate;
- list versions and review status;
- require `MANAGE_BUSINESS_SETTINGS`, recent authentication, and actor attribution.

### `OpeningCostBasisService`

- record/correct a basis for effective opening Milk/Yogurt;
- verify exact quantity match and evidence rules;
- require `MANAGE_BUSINESS_SETTINGS`, recent authentication, and actor attribution.

### `YieldPolicyService`

- calculate median/MAD from accepted effective batches;
- approve version only after the sample gate;
- return alert status without posting a loss.

### `ProductionLossAssessmentService`

- record/correct an authorized incident assessment;
- validate against the effective production event;
- require operational reasons and actor attribution.

### `InventoryCostProjectionService`

- detect stale projection;
- rebuild transactionally;
- expose movement/audit details for tests and future accountant export.

### `ProfitabilityReadService`

For a selected half-open date range, return per product:

- quantity sold;
- product sales revenue;
- cost of products sold;
- profit from products sold before shop expenses;
- gross-margin percentage when meaningful;
- spoilage and abnormal-loss cost;
- coverage reason/action codes.

The aggregate returns delivery surcharge separately and the **Estimated result from everything entered in this app** using recorded operating expenses and salary payments. If required cost is unavailable, the affected result is null/withheld rather than zero-filled.

## 10. Permissions

- Owner/Admin: approve policy, basis, yield version, and abnormal-loss assessment through `MANAGE_BUSINESS_SETTINGS` plus recent reauthentication.
- Owner/Admin/Manager: view complete financial reports through `VIEW_FULL_FINANCIAL`.
- Operator/Cashier: continue entering only permitted observable operational facts; they cannot approve costing policy or see restricted profit reports.

No new role is created in this slice.

## 11. Backup and restore

Schema version becomes 8. Backup format remains encrypted format 2, but the source costing tables join the business group. Restore order inserts users before costing rows that reference actor IDs, then rebuilds derived cost movements instead of trusting backed-up projection rows.

Legacy backups without costing sources remain valid and restore with incomplete coverage where appropriate. Restore verification compares rebuilt movements/report totals, not derived row IDs.

## 12. Delivery slices

1. **Schema/source services:** migration, immutable sources, actor/permission enforcement.
2. **Replay core:** Milk purchase/opening/sale/spoilage weighted-average paths and correction replay.
3. **Yogurt costing:** monthly conversion policy, production transfer, yield alerts, abnormal loss.
4. **Read models:** product profitability and recorded-result reports with plain-language reason codes.
5. **Recovery and regression:** backup/restore, migrations, permission bypass, concurrency, and full device suite.

No React profitability screen is enabled until all five slices pass.

All five native slices are implemented. This statement does not enable or claim a React profitability screen; hybrid DTO, bridge, and presentation wiring remain later-phase work.

## 13. Mandatory verification

The implementation must cover every test listed in the approved proposal, plus:

- migration 7→8 preserves all version-7 rows and creates no source cost rows;
- append-only triggers reject source update/delete and broken supersession chains;
- correction leaves replay full truth rather than delta rows;
- projection staleness and transactional rollback;
- direct DAO/service permission bypass attempts;
- exact opening-basis quantity matching;
- null/withheld monetary results for incomplete cost;
- stable reason/action codes and management-report disclosures;
- legacy and version-8 backup/restore;
- unchanged frozen hashes;
- complete existing regression suite.

## 14. Implementation verification

Delivered in Room schema version 8 and the native domain layer:

- all four append-only costing source families and immutable-source triggers;
- deterministic derived cost movements/projection state;
- policy, opening-basis, yield, production-loss, replay, and profitability services;
- migration 7→8 with no fabricated costing or opening-cost rows;
- encrypted backup/restore of source truth with derived-projection rebuild;
- focused migration, costing, security-shell, and backup/restore coverage.

Verification on connected device `SM-A065F`:

- Kotlin and Android-test Kotlin compilation — PASS;
- focused costing plus backup/restore suite — 14/14 PASS;
- complete connected-device regression — 111/111 PASS, 0 skipped, 0 failed.

The frozen Phase 18 logic specification and SQL blueprint remain unchanged. Formal lower-of-cost/NRV write-down and reversal support, non-Milk/Yogurt acquisition costing, hybrid DTOs/bridge, React screens, and optional contextual financial explainers remain outside this delivered slice.
