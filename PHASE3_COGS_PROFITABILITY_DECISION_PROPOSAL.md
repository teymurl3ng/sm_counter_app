# Phase 3 — COGS and Profitability Decision Proposal

**Status: PROPOSED — awaiting explicit approval; no costing implementation is authorized by this document**

**Prepared: 2026-08-27**

## 1. Decision requested

Approve an authoritative inventory-costing and management-profitability model for Saaf Milk before any COGS, gross-profit, net-profit, spoilage-loss, or per-product-margin figure is exposed as native truth.

The recommended model is:

> **Perpetual moving weighted-average actual acquisition cost, with effective-dated standard conversion cost for yogurt production, append-only cost-basis/valuation adjustments, and explicitly reported incomplete cost coverage.**

This is designed around the shop's commingled, high-turnover milk/yogurt inventory and the application's existing append-only event/correction architecture.

## 2. Standards basis and limitation

IAS 2 requires inventory to be measured at the lower of cost and net realisable value. Inventory cost includes purchase, conversion, and other costs incurred to bring inventory to its present location and condition. For interchangeable inventory, IAS 2 permits FIFO or weighted average; the same formula is used for inventory with similar nature and use. When inventory is sold, its carrying amount becomes expense in the period of the related revenue. Inventory losses and write-downs are recognised as expense when they occur.

IFRS for SMEs uses the same relevant foundation: purchase and conversion cost, FIFO or weighted average for interchangeable inventory, impairment when carrying amount is not recoverable, and expense recognition when inventory is sold. Its standard-cost technique is permitted only when it approximates cost and is regularly reviewed.

Pakistan's SECP guide states that the applicable reporting framework depends on company classification; medium and small companies may fall under IFRS for SMEs or AFRS for SSEs, with specified options to use notified IFRS. Therefore this proposal uses IAS 2/IFRS-for-SMEs inventory principles as a strong design basis, but the application must not advertise statutory IFRS compliance without an accountant confirming the shop's legal form, applicable framework, policies, and complete books.

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

The actual measured output receives the complete milk input cost. Normal yield loss is therefore absorbed into the yogurt produced rather than recorded as a second unexplained loss.

### 6.4 Conversion-cost recommendation

IAS 2 includes direct labour and systematic fixed/variable production overhead in conversion cost. The current app does not contain reliable batch-level actual conversion costs. The recommended small-business solution is an effective-dated **standard yogurt conversion cost per kilogram**, configured by an owner from normal culture/ingredients, production fuel/utilities, direct production labour, and production overhead.

Controls:

- each production event snapshots the active conversion-cost policy;
- the policy is versioned and never overwrites history;
- the app requires review at least monthly and whenever production economics materially change;
- a stale policy produces a visible warning and blocks claims of complete standards-based profit;
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

Production's normal measured yield difference remains absorbed in yogurt output cost. An abnormal, separately identified loss uses the spoilage workflow and is expensed. Corrections restate quantity and trigger deterministic cost recalculation from the earliest affected point.

## 8. Lower of cost and net realisable value

Perishable inventory can become worth less than its recorded cost even before it is physically discarded. A standards-aligned design therefore needs an append-only owner/admin valuation adjustment:

- product and quantity/value scope;
- assessment date;
- estimated ordinary selling price;
- estimated completion and necessary selling costs;
- resulting net realisable value;
- write-down amount and reason/evidence;
- reversal linked to the original write-down, capped at the un-reversed amount.

This adjustment changes carrying cost, not physical quantity. It is separate from spoilage and cannot be used to increase inventory above historical cost.

The daily operational UI need not force an NRV review on every transaction, but reporting-period close must expose unresolved valuation-review status before claiming standards-aligned inventory value.

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

### 11.4 Inventory losses and write-downs

Show spoilage losses and NRV write-downs/reversals separately after gross profit. They remain part of the recorded operating result and retain product attribution.

### 11.5 Recorded operating result—not statutory net profit

The current app records sale revenue, surcharge income, inventory events, operating-expense payments, and salary payments. It does not provide a complete accrual general ledger for depreciation, finance costs, tax, accrued/unpaid expenses, owner drawings, other income, and every possible financial-statement adjustment.

The honest initial label is therefore:

```text
Recorded operating result
= product sales revenue
 + delivery surcharge income
 - COGS
 - spoilage loss
 - net NRV write-downs
 - recorded operating expenses
 - recorded salary payments
```

It must carry the explanation **Based only on activities recorded in Saaf Milk; not statutory net profit**.

The React labels “Net Period Income” and “Take-home net profit” must not be used until a complete income-statement/accounting scope is separately approved and verified.

## 12. Cost coverage and report honesty

Every report returns a coverage status:

- `complete` — all included quantities have authoritative cost provenance and required valuation review is current;
- `partial` — some products/periods are costed and others are unavailable;
- `missing_basis` — positive stock or sales depend on unknown opening/acquisition cost;
- `unsupported_flow` — a product lacks an approved cost-bearing acquisition/production workflow;
- `stale_conversion_policy` — production used or depends on an overdue standard-cost review;
- `valuation_review_due` — NRV assessment is required for the reporting close.

Missing cost never silently becomes zero. Aggregate profit is withheld or clearly marked partial when coverage is incomplete.

## 13. Proposed additive native model

Implementation will probably require a new additive Room schema version, but the version number and exact DDL are not authorized until this policy is approved and the migration is designed.

Candidate source records:

- effective-dated `costing_policy_versions`;
- append-only `inventory_cost_basis_entries` for deliberate opening/migration estimates;
- append-only `inventory_valuation_adjustments` with linked reversals;
- production events or linked details that snapshot applied conversion policy/cost.

Candidate derived projection:

- `inventory_cost_movements` linked to event/product, containing quantity, cost-in/cost-out, running quantity, running carrying cost, and coverage status.

Source policy/basis/valuation records are business data and must be backed up. Derived cost movements may be rebuilt after restore, but any persisted projection must be verified against deterministic replay before commit.

## 14. Required tests before delivery

- weighted-average receipts at changing milk costs;
- purchase delivery fee included exactly once;
- sale releases historical moving-average cost, not current settings;
- full pool depletion leaves zero quantity and zero carrying cost;
- normal yogurt yield transfers all milk input cost to measured output;
- effective-dated conversion-cost snapshot and stale-policy warning;
- production and purchase correction rebuild downstream COGS;
- sale and spoilage corrections restate cost correctly;
- fresh opening quantity requires cost basis;
- existing unknown basis remains unavailable until deliberate owner initialization;
- unknown/unsupported product cost is never zero-filled;
- spoilage loss uses native carrying cost and cannot accept a caller rate;
- NRV write-down and capped reversal;
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
4. **Transfer Milk carrying cost into Yogurt and apply an effective-dated standard conversion cost per kg**, reviewed at least monthly and when economics materially change.
5. **Treat explicit spoilage as a separately reported inventory loss at native carrying cost; absorb normal production yield loss into output cost.**
6. **Support append-only lower-of-cost/NRV write-downs and capped reversals** before claiming standards-aligned inventory valuation.
7. **Limit authoritative product profitability to approved cost-bearing flows.** Other products remain unavailable until their acquisition/production workflow is approved.
8. **Separate delivery surcharge income from product revenue.**
9. **Label the initial bottom line “Recorded operating result,” not “net profit,”** until a complete accounting scope is approved.
10. **Use integer paisa plus BigDecimal calculations for new cost values**, with a documented event-level rounding rule.
11. **Rebuild derived cost projections from effective corrected truth** rather than mutating historical events or trusting React calculations.

## 16. Recommendation

Approve all eleven decisions as one coherent policy. They produce useful, auditable management margins without pretending missing information is zero or claiming that a transaction app is already a complete statutory accounting system.

After approval, the next checkpoint is an additive schema/service/test design. No React COGS or profitability screen should be enabled before the native cost engine and coverage statuses pass the focused and full device suites.
