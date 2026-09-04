# Phase 11.5 — Lower-of-Cost / NRV Trigger Reassessment

Status: **COMPLETE — DEFER FORMAL NRV SUBSYSTEM; REOPEN ON AN APPROVED TRIGGER**

Decision date: 2026-09-04

## 1. Decision

Do not add lower-of-cost/net-realisable-value write-down or reversal records in Phase 11.

No approved trigger is demonstrated by the repository, supplied business facts, or current authoritative costing scope. The existing historical-cost management reports must continue to identify themselves as `cost_based_management` and `cost_only_no_nrv`; they must not be represented as statutory or IFRS-compliant inventory valuation.

This is a conditional deferral, not a conclusion that NRV is unimportant or inapplicable. The gate reopens immediately if the owner supplies a triggered business fact or an accountant, auditor, lender, regulator, or applicable reporting framework requires formal NRV evidence.

## 2. Governing requirement checked

The current [IFRS Foundation IAS 2 summary](https://www.ifrs.org/issued-standards/list-of-standards/ias-2-inventories/) continues to state that inventories are measured at the lower of cost and net realisable value, with NRV based on estimated ordinary-course selling price less estimated completion costs and costs necessary to make the sale.

The [IFRS Interpretations Committee's June 2021 decision](https://www.ifrs.org/news-and-events/updates/ifric/2021/ifric-update-june-2021/) confirms that necessary selling costs cannot automatically be limited to incremental costs; the entity must apply judgement to its own facts and circumstances.

The implemented Saaf Milk model does not collect all of those estimates or their evidence. It therefore cannot create a defensible NRV amount from existing fields alone.

## 3. Approved-trigger reassessment

| Approved trigger | Current evidence | Decision |
|---|---|---|
| Material unsold inventory is routinely held beyond its normal short shelf life | The authoritative costing scope remains fast-turning Milk and Yogurt. The model has stock quantities and spoilage, but no batch expiry, shelf-life, or inventory-aging evidence, and no supplied business record establishes routine material carryover. | Not demonstrated. Reopen if routine material carryover is confirmed. |
| Products are repeatedly marked down or expected selling price falls below carrying cost | Actual sale events and a mutable default sale rate exist, but neither is an approved period-end expected selling-price estimate. There is no markdown history/classification or evidence of completion and necessary selling costs. | Not demonstrated. Do not infer NRV from one low sale, a promotion, wholesale pricing, or the default rate. |
| Reporting dates regularly contain material saleable inventory exposed to price deterioration | The cost projection can report carrying quantity and cost, but no supplied facts establish material deteriorated saleable stock at a reporting date. Spoilage covers unusable quantity; it is not an NRV substitute for saleable stock. | Not demonstrated. Reopen when a period-end condition and material amount are evidenced. |
| Longer-lived manufactured products or general merchandise enter the authoritative cost model | The catalog can name additional products, but costing outside the approved Milk/Yogurt flows remains `unsupported_flow`; no longer-lived or general-merchandise authoritative costing workflow was approved. | Not triggered by the current scope. Reopen before expanding authoritative costing to such products. |
| Accountant, auditor, lender, regulator, or reporting framework requires formal evidence | No such instruction or reporting-framework requirement is present in the supplied project record. | Not demonstrated. Any later requirement reopens the gate immediately. |

## 4. Why no automatic materiality threshold was added

The project contains no approved reporting framework, statement-user threshold, stock-age policy, expected-price evidence policy, or production business dataset from which a defensible materiality threshold could be derived. Choosing a percentage, rupee value, shelf-life, or price-drop rule here would invent accounting policy and business facts.

Historical sale prices may be useful as a future review indicator, but they cannot by themselves determine expected selling price, stock still on hand at the assessment date, necessary selling costs, or whether a difference is material. Phase 11 therefore preserves the honest disclosure rather than producing a false automated valuation.

## 5. Minimum safe design if the gate reopens

A separately approved design must cover, at minimum:

- append-only, owner/admin-authorized assessment records with assessment date, product/value scope, quantity, cost basis, expected selling price, completion costs, necessary selling costs, evidence, notes, and authenticated actor;
- reliable evidence at the reporting date, item/product-line grouping rules, materiality policy, and treatment of Milk used as material for Yogurt;
- write-down expense and later reassessment, with every reversal capped at the original write-down and linked to the assessment it reverses;
- strict separation between saleable-stock NRV decline, physical spoilage, and abnormal production loss;
- deterministic correction/rebuild behavior, closed-period rules, report presentation, audit, permissions/recent authentication, backup/restore, migration, bridge, React, and physical-device tests;
- explicit accountant-facing disclosures and plain shop-language UI that does not require staff to understand NRV terminology.

No part of that subsystem is authorized by this decision record alone.

## 6. Preserved implementation boundary

- `CostReportingScope.REPORTING_BASIS` remains `cost_based_management`.
- `CostReportingScope.VALUATION_SCOPE` remains `cost_only_no_nrv`.
- The Financial Report screen continues to state: “Basis: management estimate using saved product costs. Scope: recorded cost only; no lower-selling-price adjustment.”
- Spoilage remains a physical quantity loss valued at carrying cost, not an impairment workaround.
- No Room schema, business formula, bridge operation, UI action, or immutable baseline changes in checkpoint 11.5.

## 7. Reopen questions

The owner or accountant should reopen this gate if any answer becomes “yes”:

1. Is a material amount of saleable Milk or Yogurt still on hand past its normal selling window at a reporting date?
2. Are products repeatedly discounted below carrying cost, or is their evidence-based expected selling price less completion and necessary selling costs below carrying cost?
3. Are longer-lived products or general merchandise being brought into authoritative costing?
4. Does any accountant, auditor, lender, regulator, tax/statutory process, or selected reporting framework require formal NRV records?

## Plain English

Saaf Milk can honestly show what stock cost and what was earned or lost from recorded activity, but it does not have the evidence needed to estimate a formal lower selling value. Nothing supplied shows that this advanced adjustment is currently material or required. We are therefore keeping the clear “recorded cost only” warning and not guessing. If stock starts sitting unsold, is repeatedly discounted below cost, expands into longer-lived goods, or an accountant requires NRV, this decision must be reopened before claiming formal inventory valuation.
