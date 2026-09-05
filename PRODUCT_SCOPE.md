# Product Scope and Benchmark Policy

Status: **AUTHORITATIVE FOR FORWARD-LOOKING PRODUCT DECISIONS — 2026-09-04**

This document clarifies the product direction without rewriting completed phase records. Historical references to Saaf Milk, Milk, and Yogurt remain accurate descriptions of the implementation that was designed and verified in those phases.

## Product identity

- **Saaf Milk is the current product and working brand, not a permanent market boundary.** A future release may use another brand, configurable identity, or broader positioning through a separately approved product decision.
- **Milk and Yogurt are the current validated product scope, not the permanent product ceiling.** They were selected to build and prove the first end-to-end purchase, production, stock, sale, spoilage, costing, reporting, and recovery workflows.
- The longer-term product may support other products and other owner-managed shop contexts where its operating model is a good fit.
- Future breadth is a direction, not a claim that today's formulas already handle every product correctly.

## Intended user and product promise

The primary user is an owner or staff member of a small, operationally simple business who needs dependable daily records without being a professional accountant or enterprise manager.

The product promise is:

> Provide clear, offline-first operational and management records whose saved facts and displayed calculations are dependable within the explicitly supported scope.

### Mandatory low-friction secure access

Routine use on a trusted phone must not force owners or staff to repeatedly type long passphrases. Device-local **Quick unlock** using Android biometrics/device credential or a short per-user PIN is an approved, release-blocking requirement. The fast path must remain native, device-bound, rate-limited, revocable, and correctly attributed on shared phones.

Quick unlock may release a Keystore-protected local session or cloud key on the trusted installation. It may not reduce portable encrypted-backup recovery to four digits: a cleared, lost, or replacement phone still requires the full high-entropy recovery secret. `QUICK_UNLOCK_REQUIREMENT.md` is authoritative for this boundary.

The product is not currently presented as:

- a complete statutory accounting system;
- a tax-return or regulatory-filing system;
- a food-safety certification system;
- a full enterprise-resource-planning platform;
- a replacement for an accountant, auditor, tax adviser, or regulatory professional.

## Proportional quality principle

The standard is **modest scope with strong correctness**, not feature parity with industry giants.

- We may intentionally support fewer products, reports, roles, integrations, and accounting adjustments.
- We may not lower the correctness, auditability, security, or recovery standard for a function we do claim to support.
- A narrow calculation must be correct for its defined inputs and assumptions.
- Missing accounting coverage must be disclosed rather than treated as zero.
- Management estimates must not be labelled as statutory or complete financial results.
- Standards are guardrails against incorrect or misleading behavior, not instructions to reproduce every enterprise feature.

## Current validated domain

The current implementation is validated around:

- supplier milk purchases and payable balances;
- Milk and Yogurt stock;
- Milk-to-Yogurt production;
- cash and credit sales;
- customer receivables and collections;
- supplier payments, expenses, salaries, and deliveries;
- spoilage, physical cash counts, daily close/reopen, and corrections;
- Milk/Yogurt historical-cost management reporting;
- role-aware access, audit, encrypted backup, and recovery.

The current costing boundary uses perpetual moving weighted-average acquisition cost for interchangeable inventory, effective-dated Yogurt conversion cost, measured output, separately reviewed abnormal loss, and explicit incomplete-coverage disclosures. `PHASE3_COGS_PROFITABILITY_DECISION_PROPOSAL.md` and `PHASE11_5_NRV_TRIGGER_REASSESSMENT.md` remain authoritative for those decisions.

## Future product expansion

New products should reuse the existing platform only where the underlying facts and rules genuinely match. Adding a product name to the catalog does not by itself validate its accounting or operational treatment.

Before a new product family becomes authoritative, decide and test as applicable:

- unit and precision rules;
- purchased, produced, bundled, service, or non-stock behavior;
- inventory tracking and negative-stock policy;
- acquisition and conversion costing;
- recipes, by-products, yield, ordinary variation, and abnormal loss;
- shelf life, batch/lot, expiry, quality, rejection, and traceability needs;
- sale, return, discount, delivery, tax, and credit treatment;
- corrections, reporting, backup, migration, and permission effects;
- whether existing labels and disclosures remain truthful.

Product expansion must be evidence-led and incremental. Do not prebuild enterprise abstractions that have no approved user need.

## Benchmark hierarchy

Decisions use the smallest relevant set of evidence in this order:

1. **Applicable law and regulatory obligations** — only after jurisdiction and business applicability are established.
2. **Selected accounting/reporting basis** — used to prevent incorrect formulas and overclaiming, not to imply full compliance.
3. **Dairy or other domain guidance** — used when the supported workflow actually needs the record or control.
4. **Android and accessibility guidance** — used for platform behavior, insets, targets, focus, contrast, and adaptive layout.
5. **Comparable products** — used to discover familiar patterns and possible omissions, never as an automatic feature list.
6. **Observed owner/staff work** — used to validate language, sequence, speed, mistakes, and usefulness in the real context.
7. **Visual preference** — used only after correctness, clarity, accessibility, and task fit.

Useful reference anchors include:

- [IAS 2 Inventories](https://www.ifrs.org/issued-standards/list-of-standards/ias-2-inventories/) and [IFRS for SMEs](https://www.ifrs.org/issued-standards/ifrs-for-smes/) for proportional accounting design and honest reporting boundaries;
- [FAO dairy quality and testing guidance](https://www.fao.org/dairy-production-products/products/quality-and-testing/en/) and the [Codex milk hygiene code](https://www.fao.org/4/i2085e/i2085e00.pdf) when dairy quality or traceability is actually in scope;
- [Android core app quality](https://developer.android.com/develop/adaptive-apps/quality-guidelines/core-app-quality), [Android system-bar guidance](https://developer.android.com/design/ui/mobile/guides/foundations/system-bars), and [WCAG 2.2](https://www.w3.org/TR/WCAG22/) for the packaged hybrid interface;
- [ISO 9241-11 usability](https://www.iso.org/standard/63500.html) and [Nielsen's usability heuristics](https://www.nngroup.com/articles/ten-usability-heuristics/) for effectiveness, efficiency, satisfaction, feedback, error prevention, and understandable language.

Local tax, invoice, labour, and food rules are assessed separately when the intended jurisdiction, legal form, registration, and release claims are known. A general external standard does not automatically authorize a feature or prove local compliance.

## Review framework

### Functions

For each proposed capability, record:

- user and real task;
- observable facts entered;
- authoritative native records created or changed;
- expected outcome and error cases;
- external requirement, established convention, or user evidence;
- current support, gap, and deliberate exclusions;
- permissions, audit, correction, backup, and restore effects.

### Formulas and displayed numbers

For each material figure, record:

- plain-language meaning;
- authoritative inputs and source events;
- formula, unit, precision, rounding, sign, and period cutoff;
- accounting or operational basis;
- assumptions and missing coverage;
- permitted UI label and prohibited overclaims;
- normal, boundary, correction, and replay tests.

### Screens and workflows

For each meaningful state, review:

- intended user and immediate task;
- primary information, decision, and action;
- error prevention, confirmation, feedback, and recovery;
- terminology and data provenance;
- accessibility, system insets, keyboard, touch, and adaptive behavior;
- task completion accuracy, effort, and confidence;
- whether a finding is required, conventional, a user hypothesis, or aesthetic preference.

AI design-review tools may help apply this framework consistently, but they are not the authority for accounting, legal, dairy, or user-context decisions.

## Current intentional exclusions

Unless separately approved, the roadmap does not require:

- a complete accrual general ledger or statutory financial statements;
- depreciation, asset registers, finance costs, tax calculation, or owner-equity accounting;
- multi-company consolidation or enterprise approval hierarchies;
- enterprise analytics, forecasting, or integration parity;
- universal costing for every retail, manufactured, bundled, or service product;
- automatic tax filing, regulatory certification, or food-safety compliance claims.

These exclusions keep the product usable and honest. They may be reopened when a real target user, product family, reporting need, or legal obligation justifies them.

## Success criteria

The product succeeds when supported users can:

- complete important daily tasks accurately and without specialist terminology;
- understand what happened, what is owed, what stock remains, and what needs attention;
- trust that saved records, corrections, access controls, and backups behave predictably;
- understand the basis and limits of displayed results;
- recover from ordinary mistakes and device problems;
- enter and resume routine protected work quickly through secure device-local quick unlock;
- adopt future supported products without corrupting or misrepresenting earlier records.

It does not need to equal the breadth of a corporate ERP or the reporting scope expected by chartered accountants and senior enterprise management.

## Change-control rule

This scope guides Phase 12 and later work. A future brand, product family, statutory claim, regulatory integration, or enterprise feature requires an explicit decision record and proportionate design, migration, permission, test, and acceptance gates. It must not be inferred from generic catalog fields or introduced during visual polish.

## Plain English

Saaf Milk and Milk/Yogurt are where the product starts, not where it must end. We are building a dependable small-business tool, not competing feature-for-feature with giant accounting systems. Anything the app claims to do should be correct and understandable; anything outside its current scope should be stated honestly and added only when a real business need justifies it.
