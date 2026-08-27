# Phase 2 — Desired Product Capability Decisions

**Status: COMPLETE — desired capability set approved for native implementation planning**

## Purpose

Phase 1 mapped the latest AI Studio UI against the untouched Phase 18 native system.
The explicit project goal is to preserve the useful functionality of the new UI and
make the native layer genuinely capable of supporting it.

Therefore the Phase 2 decision is:

> A meaningful product capability present in the new AI Studio UI is retained as a
> desired product capability unless it is explicitly unsafe, redundant, or merely
> a technical/mock convenience.

Phase 3 is responsible for implementing the approved capabilities natively.

This phase does **not** change the Phase 18 SQL blueprint or frozen logic specification.

---

# 1. Approved Existing Capabilities

These are already native-backed and remain part of the product:

- milk purchase / supplier intake;
- retail sale;
- wholesale cash sale;
- wholesale credit sale;
- customer credit collection;
- supplier payment;
- expense;
- salary payment;
- yogurt production;
- corrections;
- customer/vendor/employee creation and renaming;
- business-rate settings;
- budgets/targets;
- history;
- analytics;
- backup/restore.

They proceed to Phase 4–8 through the normal native/read-model/bridge workflow.

---

# 2. Approved Native Capability Expansions

The following UI capabilities are intentionally retained and should be built natively
rather than removed merely because the original Phase 18 core did not contain them.

## 2.1 Product management

**Approved**

Native capability set:

- create product;
- edit product details;
- archive/deactivate product.

Constraint:

Inventory quantity must remain event-derived. Product management must not become
a direct stock mutation mechanism.

## 2.2 Customer profiles

**Approved**

Retain useful profile data represented by the UI, including as applicable:

- name;
- phone;
- route/area;
- credit limit;
- daily quota;
- notes;
- active/inactive state.

Phase 3 must define persistence and business semantics explicitly.

## 2.3 Supplier profiles

**Approved**

Retain:

- name;
- phone;
- location/area;
- notes;
- active/inactive state.

Per-supplier milk rate is **not** automatically approved as a replacement for the
existing global/business milk-rate mechanism. If retained, it must be defined as a
separate explicit business rule and must not silently override current pricing logic.

## 2.4 Employee profiles and attendance

**Approved**

Retain:

- employee profile information supported by the UI;
- role;
- phone;
- salary/master-pay information where useful;
- attendance.

Attendance is an operational feature, not a substitute for the existing salary
payment transaction.

## 2.5 Delivery operations

**Approved**

Retain delivery as an operational capability.

The desired model is:

Delivery
- customer;
- route;
- quantity;
- linked sale/credit transaction;
- delivery status;
- delivered timestamp.

Financial revenue remains owned by the sale transaction. Delivery state must not create
duplicate financial revenue.

## 2.6 Spoilage / wastage

**Approved**

Retain spoilage/spillage as a real inventory capability.

It must be implemented as a proper native event/model with:

- auditability;
- inventory effect;
- correction behavior;
- reporting behavior.

It must not be implemented as direct hidden stock mutation.

## 2.7 Physical cash count / reconciliation record

**Approved**

Retain physical counted-cash entry.

The final product should persist a legitimate reconciliation record rather than
keeping the count only in temporary React state.

Phase 3 must define exactly what is persisted and how repeated counts are handled.

## 2.8 Account-specific ledgers

**Approved**

Retain dedicated customer and supplier ledger views backed by native account identity
and effective-event semantics.

The UI must not reconstruct account history from an untyped global event list.

## 2.9 Daily close / day locking

**Approved in principle**

Retain daily close/locking because it is a meaningful business workflow represented by
the product UI.

However, Phase 3 must define the authoritative native semantics before implementation:

- what constitutes a close;
- what becomes immutable after close;
- how corrections interact with a closed day;
- whether reopened days are allowed;
- what report/archive is persisted.

No UI-only lock is acceptable.

## 2.10 COGS / profit reporting

**Approved in principle**

Retain COGS, gross profit, net profit, and per-product profitability as desired product
outputs because the new UI exposes them.

But these metrics require an explicit authoritative costing model.

Phase 3 must define the cost basis before these figures are exposed as authoritative.
React must never invent the figures from partial UI data.

## 2.11 Persistent business identity and opening state

**Approved**

Retain useful persistent business configuration represented by the UI:

- store name;
- currency display;
- opening-state values where genuinely required.

Opening cash/opening stock semantics must be native and auditable, not a React/localStorage
initialization trick.

## 2.12 Custom expense categories

**Approved**

Retain custom expense categories as persistent business data.

They must be native-backed and included in reporting/backup as appropriate.

## 2.13 Authenticated application users and role-based authorization

**Approved in principle**

Retain application user accounts as a first-class product and security capability.

Required boundaries:

- an application user is a security principal, not an accounting account or salary/employee profile;
- a user may optionally have a one-to-one link to an employee profile, but employee creation must not grant login access automatically;
- native code owns authentication, session state, and authorization enforcement;
- React may display login and permission-aware UI, but it must not accept a caller-supplied role as proof of authority;
- privileged operations, including reopen, user administration, restore, and any future destructive administration, must use the verified user/session and durable audit identity;
- disabled/locked users, first-owner bootstrap, credential recovery, migration, and backup/restore behavior require explicit semantics.

Phase 3 must explore and obtain approval for the exact credential/PIN policy, role and permission matrix, session lifetime, lockout/recovery rules, employee-link behavior, audit retention, and treatment of credential secrets in backups before implementation.

---

# 3. Approved Operational / Presentation Features

The following remain product features but do not require accounting-domain state by
themselves:

- theme preference;
- language preference;
- display density;
- default reporting period;
- default opening section;
- receipt/image/PDF presentation export where appropriate.

These are preferences/presentation artifacts, not accounting events.

---

# 4. Not Approved as Direct Operations

## 4.1 Direct "set stock to X"

**Not approved in its direct form.**

The product may support:

- stock count;
- stock adjustment;
- correction of a stock-related event;

but inventory must remain event-derived.

A proper adjustment/spoilage/count mechanism may be introduced in Phase 3 after its
domain semantics are defined.

## 4.2 Arbitrary JSON/localStorage import

**Not approved.**

The final product must not import arbitrary React/localStorage business state into the
native ledger.

Backup/restore remains the authoritative persistence transfer mechanism.

## 4.3 Unprotected "clear all business data"

**Not approved as a casual UI operation.**

A destructive reset, if ever required, must be a separately authorized administrative
workflow with explicit safeguards.

---

# 5. Capability Priority for Phase 3

## Tier 1 — Operationally essential

Implement first:

1. product management;
2. customer profiles;
3. supplier profiles;
4. employee profiles;
5. attendance;
6. delivery operations;
7. spoilage/wastage;
8. persisted physical cash count;
9. account-specific ledgers.

Security prerequisite across all tiers:

- authenticated application users;
- native role/permission enforcement;
- durable actor attribution for privileged and audited operations.

This is not merely a UI-login task. Its native identity and authorization design must be completed before bridge/UI callers can perform protected operations.

## Tier 2 — Business-management features

Then:

10. daily close/locking;
11. persistent store identity/opening state;
12. custom expense categories;
13. additional planning/budget presentation where required.

## Tier 3 — Advanced financial reporting

Then:

14. authoritative COGS;
15. gross profit;
16. net profit;
17. per-product profitability.

This ordering prevents advanced reports from being built on top of undefined costing
semantics.

User-account delivery is cross-cutting rather than a later reporting tier: native identity begins in Phase 3, is proven in Phase 4, exposed through read models and the bridge in Phases 5–6, and completed in the shell/UI, hardening, QA, and release phases.

---

# 6. Domain-Expansion Rule

Approval in Phase 2 means:

> The capability is desired.

It does **not** mean that its implementation may silently modify the frozen Phase 18
business logic or SQL blueprint.

If a capability requires a schema/business-rule amendment, Phase 3 must document the
proposed amendment and obtain explicit authorization before modifying the frozen
baseline.

---

# 7. Phase 2 Exit Condition

Phase 2 is complete when the desired product capability set is defined.

The implementation target for Phase 3 is now:

**Make the native Phase 18 layer genuinely capable of supporting the retained new UI,
while preserving the frozen accounting foundation and adding no UI-only business state.**
