# Phase 3 — COGS and Profitability Decision Proposal

**Status: APPROVED — user authorized continuation on 2026-08-27 after the yield, conversion-cost, NRV-deferral, user-account, and plain-language revisions**

**Prepared: 2026-08-27**

## 1. Decision requested

Approve an authoritative inventory-costing and management-profitability model for Saaf Milk before any COGS, gross-profit, net-profit, spoilage-loss, or per-product-margin figure is exposed as native truth.

The recommended model is:

> **Perpetual moving weighted-average actual acquisition cost, with effective-dated standard conversion cost for yogurt production, append-only cost-basis initialization, explicit spoilage loss, and honestly reported cost coverage.**

This is designed around the shop's commingled, high-turnover milk/yogurt inventory and the application's existing append-only event/correction architecture.

## 2. Standards basis and limitation

IAS 2 requires inventory to be measured at the lower of cost and net realisable value. Inventory cost includes purchase, conversion, and other costs incurred to bring inventory to its present location and condition. For interchangeable inventory, IAS 2 permits FIFO or weighted average; the same formula is used for inventory with similar nature and use. When inventory is sold, its carrying amount becomes expense in the period of the related revenue. Inventory losses and write-downs are recognised as expense when they occur.

IFRS for SMEs uses the same relevant foundation: purchase and conversion cost, FIFO or weighted average for interchangeable inventory, impairment when carrying amount is not recoverable, and expense recognition when inventory is sold. Its standard-cost technique is permitted only when it approximates cost and is regularly reviewed.

Pakistan's SECP guide states that the applicable reporting framework depends on company classification; medium and small companies may fall under IFRS for SMEs or AFRS for SSEs, with specified options to use notified IFRS. Therefore this proposal uses IAS 2/IFRS-for-SMEs inventory principles as a strong design basis, but the application must not advertise statutory IFRS compliance without an accountant confirming the shop's legal form, applicable framework, policies, and complete books.

The initial COGS release deliberately implements historical-cost management reporting, not the complete lower-of-cost/NRV measurement model. That is a sequencing decision based on the shop's fast-turning, highly perishable inventory—not a claim that IAS 2 permits NRV to be ignored in statutory accounts. Until formal NRV review, write-down, and reversal support exists, every affected report must identify itself as cost-based management reporting and must not claim IFRS/statutory inventory valuation.

Authoritative references:

- [IFRS Foundation — IAS 2 Inventories](https://www.ifrs.org/issued-standards/list-of-standards/ias-2-inventories/)
- [IFRS Foundation — IAS 2 issued text](https://www.ifrs.org/content/dam/ifrs/publications/pdf-standards/english/2021/issued/part-a/ias-2-inventories.pdf)
- [IFRS Foundation — IFRS for SMEs, Section 13](https://www.ifrs.org/content/dam/ifrs/publications/ifrs-for-smes/english/2015/ifrs-for-smes-standard-part-a.pdf?bypass=on)
- [IFRS Foundation — 2025 IFRS for SMEs update and effective date](https://www.ifrs.org/news-and-events/news/2025/02/iasb-issues-major-update-smes-accounting-standard/)
- [SECP — Guide on Financial Statements](https://www.secp.gov.pk/document/guide-on-financial-statements/)

## 3. Facts the current native model already records

### Milk purchase

Each `PURCHASE_RECEIPT` records:

- raw litres received;
- fat, LR, SNF, TS, and adjusted litres;
- frozen milk rate, delivery fee, and TS standard;
- total amount owed to the vendor;
- a positive Milk inventory movement.

The total amount owed is a usable acquisition cost because it includes the TS-adjusted purchase amount plus the directly attributable inbound delivery fee.

### Yogurt production

Each `PRODUCTION` records measured milk litres consumed and measured yogurt kilograms produced. It does not record culture, fuel, packaging, direct labour, or allocated production overhead.

### Sale

Each `SALE` records product, quantity, unit rate, sale value, optional surcharge, payment/receivable movement, and a negative product inventory movement. It does not record the carrying cost of the units sold.

### Spoilage

Each `SPOILAGE` records product, measured quantity, reason, and a negative inventory movement. It deliberately stores no invented monetary loss.

### Opening state

Opening state records product quantities but no opening unit cost or total carrying value. Existing opening events must not be retroactively assigned an invented cost.

### Corrections

Corrections preserve the original event and append corrected truth plus inventory/financial deltas. Costing must follow the effective leaf truth and remain rebuildable when a correction changes an earlier purchase, production, sale, spoilage, or opening basis.

## 4. Why the React calculation is rejected

The React prototype stores an editable/hardcoded `costRate` on each product and calculates COGS as current `costRate × quantity sold`. It also calculates spoilage loss from a caller-supplied rate and labels a partial cash/transaction calculation “Take-home net profit.”

That model is not authoritative because it:

- applies today's editable cost to historical sales;
- ignores actual purchase-price changes;
- ignores the cost transferred from milk into yogurt;
- ignores normal production yield and conversion cost;
- permits user-supplied spoilage valuation;
- invents costs for products without native acquisition history;
- can change historical profit merely by editing a product field;
- omits parts of a full income statement while claiming net profit.

Those fields and calculations remain UI mock data until replaced by native read models.

## 5. Recommended cost formula

### 5.1 Perpetual moving weighted average

Use one moving weighted-average pool per inventory-tracked product.

After a cost-bearing receipt:

```text
new carrying quantity = old quantity + received quantity
new carrying cost     = old carrying cost + received cost
new average unit cost = new carrying cost / new carrying quantity
```

For a sale or abnormal spoilage:

```text
event cost = quantity removed × average unit cost immediately before the event
remaining carrying cost = previous carrying cost - event cost
```

The final removal from a pool consumes the remaining carrying cost exactly so fractional rounding cannot leave a ghost balance.

### 5.2 Why weighted average is recommended over FIFO

Milk deliveries are physically commingled, yogurt batches become interchangeable stock, and turnover is frequent. Weighted average is explicitly permitted for interchangeable inventory and matches the physical operation better than pretending specific litres remain identifiable. It also produces a simpler, auditable mobile implementation than maintaining FIFO layers through production and correction chains.

FIFO remains standards-permitted, but it is not recommended for this product. LIFO is not permitted.

### 5.3 Consistency

Use weighted average for all interchangeable inventory with the same nature/use. A different method may be introduced only for genuinely non-interchangeable, specifically identified items and must be separately approved.

## 6. Cost-bearing event policy

### 6.1 Milk purchase cost

For `PURCHASE_RECEIPT`:

```text
received quantity = raw litres
received cost     = amount owed
```

The inbound delivery fee remains included because it is directly attributable to acquiring the milk. Adjusted litres remain part of the vendor-pricing formula, not inventory quantity.

### 6.2 Opening inventory

A fresh installation that records non-zero opening inventory must also provide the inventory's historical carrying cost—not its sale price or replacement price. The UI may accept either unit cost or total cost but must store one normalized total carrying cost.

For existing installations:

- replay cost from native source events where provenance is complete;
- never manufacture a cost from current sale price or the React prototype;
- if positive stock lacks complete cost provenance, mark the product **Cost basis required**;
- allow an owner, after recent reauthentication, to append a dated one-time cost-basis initialization for the then-current quantity;
- identify that amount as an owner-supplied historical estimate and audit it;
- leave reports before that initialization explicitly unavailable/incomplete.

An unknown cost is never treated as zero.

### 6.3 Yogurt production

At each production event:

```text
milk input cost = milk litres used × Milk average cost before production
yogurt output cost = milk input cost + applied conversion cost
yogurt receipt quantity = measured yogurt kg produced
```

The actual measured output receives the milk input cost except for any separately identified abnormal input loss. Ordinary process variation is therefore absorbed into the yogurt produced rather than recorded as a second unexplained loss.

#### Concrete yield baseline and abnormal-loss rule

The frozen core specification's `17 L → 14.5 kg` (~85%) example remains a reference-only sanity check. It is not silently promoted into a costing formula or a universal loss threshold.

Yield is monitored as:

```text
batch yield ratio = usable yogurt kg produced / milk litres charged to the batch
```

The application uses this concrete calibration procedure:

1. **Calibration state:** collect at least 30 accepted batches spanning at least 28 calendar days. A batch is accepted for calibration only when its effective corrected measurements are complete and it has no identified equipment failure, contamination, spill, rejected output, or unresolved measurement issue.
2. **Provisional reference:** while fewer than 30 accepted batches exist, display the observed ratio and the specification's ~85% sanity reference, but do not automatically label any variance normal or abnormal.
3. **Baseline:** after calibration, calculate the median yield ratio and the median absolute deviation (`MAD`) from the accepted batches. The median/MAD pair is used because it is less distorted by occasional extreme batches than a simple mean/standard deviation.
4. **Yield-review limits:** flag a batch outside `median ± max(3 × 1.4826 × MAD, 0.03)`. The `0.03` floor means the alert band can never be narrower than three percentage points on either side merely because early observations happen to be identical.
5. **Human classification:** an alert is a review trigger, not an accounting entry. An owner/manager must choose one of: correct a measurement; accept explained ordinary variance; or record an abnormal loss with measured input-equivalent quantity and reason.
6. **Incident override:** a known spill, burn, contamination/curdling incident, power/equipment failure, operator mistake, or rejected batch is abnormal even when the final ratio falls inside the statistical band.
7. **No automatic loss invention:** the system never converts the gap between expected and actual output into a spoilage quantity. An abnormal input loss must be measured or deliberately estimated by an authorized user, documented, and audited.

For a batch with an approved abnormal Milk-equivalent loss:

```text
abnormal loss cost = abnormal milk-equivalent litres × Milk average cost before production
yogurt material cost = total milk input cost - abnormal loss cost
```

The abnormal loss is expensed separately. It is not loaded into the remaining yogurt. A finished-yogurt loss is recorded after production through the ordinary `SPOILAGE` event at Yogurt carrying cost.

The yield baseline is versioned and reviewed monthly. The app may propose a replacement from the latest eligible batches, but only an owner can approve a new version; earlier batches and reports keep the policy that applied at their time. This statistical rule is an operational control for detecting exceptions, not a claim that IAS 2 prescribes a dairy yield percentage.

### 6.4 Conversion-cost recommendation

IAS 2 includes direct labour and systematic fixed/variable production overhead in conversion cost. The current app does not contain reliable batch-level actual conversion costs. The recommended small-business solution is an effective-dated **standard yogurt conversion cost per kilogram** backed by explicit cost components rather than a single unexplained rate.

Each policy version records:

- normal culture/ingredient cost per kg;
- normal packaging cost per kg when packaging is required to bring the product to saleable condition;
- variable production fuel/electricity cost per kg;
- direct production labour cost per kg;
- monthly fixed production-overhead pool;
- normal monthly production capacity in kg;
- fixed overhead rate per kg (`overhead pool / normal capacity`);
- resulting total standard conversion cost per kg;
- source/evidence note, preparer, approver, approval timestamp, and effective date.

Selling/distribution cost, unrelated administration, abnormal waste, and finished-goods storage are excluded. Milk material cost is also excluded from this standard because each batch already receives its actual moving-average Milk cost.

Controls:

- each production event snapshots the active conversion-cost policy and applied amount;
- a policy is approved for one calendar month and never overwrites history;
- at the first production batch of a new month, the previous policy is copied only as a draft and requires owner review/approval, even when every amount is unchanged;
- production is not blocked when review is overdue, but it continues under the last approved rate with `stale_conversion_policy`, and complete COGS/profit claims are withheld until review;
- an immediate mid-month policy review is triggered when a component price or normal-capacity estimate changes by at least 10%, or whenever recipe/process, packaging, wage, energy tariff, or production equipment changes materially;
- Milk purchase-rate changes do not trigger conversion-policy revision because Milk is costed separately at actual moving average;
- once per quarter, the owner compares applied standard conversion cost with the best available actual component totals; an absolute variance of 10% or more requires an explanation and a revised next policy, while a smaller variance may retain the rate with a recorded review note;
- abnormal waste, selling costs, unrelated administration, and ordinary finished-goods storage are excluded;
- the standard must approximate actual normal cost; it is not a desired-margin input.

This approach is more practical than asking staff to allocate electricity, LPG, labour, and packaging during every batch while retaining the accounting requirement to include normal conversion cost.

### 6.5 Other products

Current native operations provide cost provenance for purchased Milk and produced Yogurt. Merely creating another product does not create an acquisition or production path.

Until a separately approved cost-bearing inventory-receipt or production workflow exists:

- other products may not display authoritative COGS/profitability;
- their cost status is **Unsupported cost flow**, not zero;
- React's hardcoded paneer, cream, butter, and ghee costs are ignored.

Adding a general merchandise receipt is a separate product/transaction decision and is not silently authorized by this proposal.

## 7. Spoilage and inventory losses

Explicit `SPOILAGE` is valued at the product's average carrying cost immediately before the event.

The report presents it as a separate **Inventory loss — spoilage** line rather than allowing staff to type a loss rate. IAS 2 requires inventory losses to be expensed when they occur.

Ordinary process variance within the approved/reviewed yield policy remains absorbed in yogurt output cost. Identified abnormal Milk-equivalent loss is recorded against the production batch and expensed; finished-yogurt loss uses the spoilage workflow. A yield alert alone never invents a loss. Corrections restate quantity/classification and trigger deterministic cost recalculation from the earliest affected point.

## 8. NRV scope decision—deferred from the initial COGS release

Lower-of-cost/NRV measurement, including capped reversals of prior write-downs, is a valid standards requirement. It is also a separate valuation subsystem from the operational costing needed to calculate COGS. For this shop, Milk and Yogurt turn quickly or become explicit spoilage; a distinct NRV impairment is expected to be exceptional. Building the full evidence, approval, linkage, reversal, reporting, backup, and test surface now would delay the higher-value cost foundation.

The initial release therefore includes:

- moving-weighted-average historical cost;
- explicit spoilage and identified abnormal-loss expense;
- no NRV write-down or reversal records;
- visible report metadata: `reporting_basis = cost_based_management` and `valuation_scope = cost_only_no_nrv`;
- no statement that inventory values or reports are IFRS/statutory compliant.

This deferral must be reconsidered during Phase 11 hardening—or earlier—if any of these triggers occurs:

- material unsold inventory is routinely carried beyond its normal short shelf life;
- products are repeatedly marked down or expected selling price falls below carrying cost;
- reporting dates regularly contain material saleable inventory exposed to price deterioration;
- longer-lived manufactured products or general merchandise enter the authoritative cost model;
- the accountant, auditor, lender, regulator, or applicable reporting framework requires formal NRV evidence.

If triggered, the later design should add append-only owner/admin write-downs, evidence of estimated selling/completion costs, linkage to affected product/value scope, and reversals capped at the original write-down. Until that subsystem is delivered, spoilage must remain a quantity loss—not a substitute mechanism for saleable inventory whose NRV has declined.

## 9. Corrections and chronological replay

Cost results are derived projections, not a second source of business truth.

Effective events are replayed by:

1. business occurrence date;
2. creation timestamp;
3. event ID as a deterministic final tie-breaker.

Only the effective leaf of a correction chain contributes restated business truth. When a correction affects an earlier cost-bearing or cost-consuming event, the cost projection is rebuilt transactionally from the earliest affected point. Derived downstream COGS may change; the original event and correction audit history do not.

A closed day cannot receive ordinary backdated postings. Reopen/correction authorization remains native and audited.

## 10. Monetary precision

New authoritative cost totals must not use unconstrained binary floating-point arithmetic.

Recommended representation:

- store total currency movements in integer paisa (`Long`);
- perform unit-cost division and multiplication with Kotlin `BigDecimal` at defined precision;
- round event-level currency results to paisa using one documented rule;
- allocate any final fractional residual to the last quantity removal so carrying quantity zero always means carrying cost zero;
- display PKR with two decimal places while preserving measured quantities at their existing validated precision.

The exact boundary conversion from existing `Double` transaction amounts must be covered by tolerance and rounding tests before migration is approved.

## 11. Authoritative report definitions

### 11.1 Product sales revenue

Use effective `SALE.sale_value` for the product. Present delivery surcharge separately as **Delivery surcharge income** so it does not distort product unit margin.

### 11.2 COGS

COGS is the carrying cost released by effective sale events during the selected period. Purchases are not COGS merely because they were paid or received in the period.

### 11.3 Product gross profit

```text
product gross profit = product sales revenue - product COGS
gross margin % = product gross profit / product sales revenue × 100
```

Reports show sold quantity, sales revenue, COGS, gross profit, and margin by product. Average unit cost is a read-only derived value, never an editable product master field.

### 11.4 Inventory losses

Show spoilage and identified abnormal losses separately after gross profit. They remain part of the recorded operating result and retain product attribution. The initial release has no NRV write-down/reversal line because formal NRV valuation is deferred under Section 8.

### 11.5 Recorded operating result—not statutory net profit

The current app records sale revenue, surcharge income, inventory events, operating-expense payments, and salary payments. It does not provide a complete accrual general ledger for depreciation, finance costs, tax, accrued/unpaid expenses, owner drawings, other income, and every possible financial-statement adjustment.

The honest initial label is therefore:

```text
Recorded operating result
= product sales revenue
 + delivery surcharge income
 - COGS
 - spoilage loss
 - identified abnormal inventory/process loss
 - recorded operating expenses
 - recorded salary payments
```

It must carry the explanation **Based only on activities recorded in Saaf Milk; not statutory net profit**.

The React labels “Net Period Income” and “Take-home net profit” must not be used until a complete income-statement/accounting scope is separately approved and verified.

## 12. Cost coverage and report honesty

Every report returns a cost-coverage status:

- `complete` — all included quantities have authoritative historical-cost provenance;
- `partial` — some products/periods are costed and others are unavailable;
- `missing_basis` — positive stock or sales depend on unknown opening/acquisition cost;
- `unsupported_flow` — a product lacks an approved cost-bearing acquisition/production workflow;
- `stale_conversion_policy` — production used or depends on an overdue standard-cost review.

Missing cost never silently becomes zero. Aggregate profit is withheld or clearly marked partial when coverage is incomplete.

Cost coverage and valuation scope are different dimensions. Even a `complete` cost report must expose `reporting_basis = cost_based_management` and `valuation_scope = cost_only_no_nrv`; `complete` must never be presented as complete statutory or IFRS valuation coverage.

### 12.1 Plain-language operating rule

Financial literacy is not a prerequisite for operating Saaf Milk. The software owns the accounting rules; shop users enter facts they can directly observe and understand.

The product has two language layers:

- the native model, audit trail, exports, and technical documentation retain precise accounting names and stable machine codes;
- the everyday UI translates those concepts into plain shop language, shows the practical meaning, and states the next action when something is incomplete.

Formal terminology may appear as optional help or on an accountant-facing export, but a staff member must never need to understand terms such as COGS, NRV, carrying value, impairment, weighted average, conversion overhead, or cost provenance to complete ordinary work.

Required everyday translations include:

| Formal/internal concept | Everyday presentation |
|---|---|
| COGS | **Cost of products sold** |
| Gross profit | **Profit from products sold—before shop expenses** |
| Spoilage loss | **Cost of spoiled stock** |
| Recorded operating result | **Estimated result from everything entered in this app** |
| `missing_basis` | **Profit cannot be calculated yet—opening stock cost is missing** |
| `unsupported_flow` | **Profit is not available for this product yet—how it enters stock has not been set up** |
| `stale_conversion_policy` | **Yogurt-making cost needs owner review** |
| `cost_only_no_nrv` | **Advanced accountant valuation is not included in this report** |

The estimated-result screen must also say: **This is not cash in hand, owner take-home money, tax profit, or complete annual accounts.** A short **How was this calculated?** view shows the plain additions and deductions without exposing editable formulas.

Interaction rules:

- staff record observed facts—litres, kilograms, prices, quantities, and what physically happened;
- the engine calculates product cost, loss value, and report classifications;
- calculated rates and accounting classifications are not editable text fields;
- setup asks concrete questions such as culture/packaging cost, usual batch output, and monthly production fuel/electricity—not abstract questions about allocating conversion overhead;
- yield incidents use operational choices such as spill, contamination, power failure, or measurement correction; approved mappings determine the accounting treatment;
- every unavailable number is replaced with a plain reason and a direct resolution step, never zero, a raw status code, or an unexplained blank;
- staff are not asked to guess; genuinely exceptional accounting judgments are restricted to a guided owner/admin workflow and may be left pending for an accountant.

## 13. Proposed additive native model

Implementation will probably require a new additive Room schema version, but the version number and exact DDL are not authorized until this policy is approved and the migration is designed.

Candidate source records:

- effective-dated `costing_policy_versions`;
- append-only `inventory_cost_basis_entries` for deliberate opening/migration estimates;
- production events or linked details that snapshot applied conversion policy/cost.

Candidate derived projection:

- `inventory_cost_movements` linked to event/product, containing quantity, cost-in/cost-out, running quantity, running carrying cost, and coverage status.

Source policy and basis records are business data and must be backed up. Derived cost movements may be rebuilt after restore, but any persisted projection must be verified against deterministic replay before commit.

## 14. Required tests before delivery

- weighted-average receipts at changing milk costs;
- purchase delivery fee included exactly once;
- sale releases historical moving-average cost, not current settings;
- full pool depletion leaves zero quantity and zero carrying cost;
- normal yogurt yield transfers all milk input cost to measured output;
- yield calibration requires 30 accepted batches over at least 28 days and does not auto-post from an alert;
- median/MAD alert limits, incident override, authorized classification, and version preservation;
- identified abnormal Milk-equivalent loss is excluded from yogurt cost and separately expensed;
- effective-dated conversion-cost snapshot and stale-policy warning;
- monthly conversion-policy approval, 10%/process change trigger, and quarterly actual-versus-standard review;
- conversion policy is component-backed and excludes Milk material, selling, unrelated administration, abnormal waste, and finished-goods storage;
- production and purchase correction rebuild downstream COGS;
- sale and spoilage corrections restate cost correctly;
- fresh opening quantity requires cost basis;
- existing unknown basis remains unavailable until deliberate owner initialization;
- unknown/unsupported product cost is never zero-filled;
- spoilage loss uses native carrying cost and cannot accept a caller rate;
- report metadata/disclosures cannot imply statutory or IFRS valuation and distinguish cost coverage from the deferred NRV scope;
- everyday screens translate formal accounting concepts and machine codes into the approved plain-language labels, reasons, and next actions;
- staff workflows require only observable shop facts and never expose an editable calculated cost, loss rate, or accounting classification;
- shopkeeper acceptance confirms that ordinary sale, purchase, production, spoilage, correction, and report workflows can be completed without knowing accounting terminology;
- surcharge separated from product revenue;
- partial coverage withholds or labels aggregate profit;
- paisa rounding and residual allocation;
- migration creates no artificial costs or profit;
- backup/restore preserves source cost policy and rebuilds identical projections;
- permission, recent-reauthentication, daily-close, and audit attribution checks;
- full existing regression remains green.

## 15. Decisions proposed for approval

1. **Use perpetual moving weighted average** for interchangeable inventory.
2. **Use purchase amount owed, including inbound delivery fee, as Milk acquisition cost.**
3. **Require historical cost for fresh opening inventory and an audited owner-supplied initialization when an upgraded installation lacks cost provenance.** Never invent or zero-fill it.
4. **Transfer Milk carrying cost into Yogurt and apply a component-backed standard conversion cost per kg.** Approve one version per calendar month, trigger immediate review for a ≥10% component/capacity change or material process change, and require explanation/revision when the quarterly actual-versus-standard variance reaches 10%.
5. **Define normal yield through a versioned shop-specific calibration:** at least 30 accepted batches across 28 days, median/MAD alert limits, human review, and no automatic accounting loss. Ordinary process variance is absorbed into output; identified abnormal Milk-equivalent loss or finished-product spoilage is separately expensed.
6. **Defer formal lower-of-cost/NRV write-down and reversal support from the initial COGS release.** Ship historical-cost management reporting plus explicit spoilage/abnormal loss, expose `cost_only_no_nrv`, make no IFRS/statutory valuation claim, and reassess the deferral during Phase 11 or when a trigger in Section 8 occurs.
7. **Limit authoritative product profitability to approved cost-bearing flows.** Other products remain unavailable until their acquisition/production workflow is approved.
8. **Separate delivery surcharge income from product revenue.**
9. **Label the initial bottom line “Recorded operating result,” not “net profit,”** until a complete accounting scope is approved.
10. **Use integer paisa plus BigDecimal calculations for new cost values**, with a documented event-level rounding rule.
11. **Rebuild derived cost projections from effective corrected truth** rather than mutating historical events or trusting React calculations.
12. **Make financial literacy unnecessary for ordinary use.** Keep precise accounting concepts inside the native/audit layer, translate them into approved plain shop language, ask users only for observable facts, and always provide a reason plus next action when a result cannot be calculated.

## 16. Recommendation

Approve all twelve decisions as one coherent policy. They produce useful, auditable management margins without requiring shop users to understand accounting, pretending missing information is zero, or claiming that a transaction app is already a complete statutory accounting system.

After approval, the next checkpoint is an additive schema/service/test design. No React COGS or profitability screen should be enabled before the native cost engine and coverage statuses pass the focused and full device suites.
