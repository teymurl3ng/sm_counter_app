# Phase 5 — Native Read-Model Expansion

**Status: COMPLETE — 2026-08-28**

## 1. Purpose and boundary

Phase 5 creates and freezes the authoritative, permission-aware native information surface required by the approved future hybrid UI. It reuses the Kotlin/Room business truth completed in Phases 3–4 and does not start the WebView bridge or React wiring.

The exact frozen catalog is `PHASE5_DTO_CATALOG.md`.

## 2. Implemented changes

- Added strongly typed hybrid-facing DTOs for bootstrap/session identity, settings/opening state, products and people, operational references, stock/payment positions, daily totals, full financial positions, effective history/detail, account ledgers, delivery, attendance, spoilage, cash reconciliation/daily close, profitability, costing controls, planning, users, and security audit.
- Added `HybridReadModelService`, a read-only façade that checks native permissions before composing existing services and DAOs. Shared operational dashboards contain no cash/payroll/expense totals; cashier sales/collection information and full management finances use separately authorized reads.
- Standardized all transport money to integer PKR paisa and all quantities/ratios/percentages to canonical decimal strings.
- Kept correction relationships and superseded flags explicit. Financial event movements are withheld—not zeroed—for operational users without full-financial permission.
- Added durable event and security audit actor snapshots while preserving honest nullable actor identity for legacy/unattributed records.
- Added read-only DAO queries for complete yield-policy and production-loss-assessment history. No table, trigger, migration, or database version changed.
- Added an explicit costing code catalog covering report coverage, reporting/valuation scope, conversion components, review reasons, opening basis kinds, yield statuses, and production-loss classifications/reasons.
- Excluded all credential, recovery, lockout, and session-handle material from DTOs.

## 3. Permission boundary

Every protected query calls `UserSecurityService` before returning data. Operational, full-financial, settings, master-data, delivery, attendance, production/spoilage, close/reconciliation, staff-user, and audit permissions remain separate. The React layer will later use effective permissions for presentation, but Kotlin remains the enforcement authority.

## 4. Verification

Connected device: `SM-A065F`

- production Kotlin compilation — PASS;
- Android-test Kotlin compilation — PASS;
- focused `Phase5HybridReadModelTest` — 4/4 PASS;
- assertions cover owner bootstrap/current session, effective permission codes, PKR/paisa and decimal representation, opening-state correction flags, operational/cashier/financial separation, role-filtered transaction references, owner/cashier/operator boundaries, safe user/audit reads, costing code stability, and forbidden security-field scanning.

The complete pre-Phase-5 baseline was 120 connected-device tests and 7 host tests. Phase 5 adds four connected-device tests. Final regression: 124/124 connected-device tests and 7/7 host tests passed, with zero skipped or failed.

## 5. Frozen files and schema

- Room remains schema version 8; no migration was needed.
- Frozen Phase 18 logic specification remains unchanged.
- Frozen Phase 18 SQL blueprint remains unchanged.
- Logic specification SHA-256: `45E5BDDABE8861A1B3DF7C5666AC7673078439A89FA5628061AD293A865F9DEA`.
- SQL blueprint SHA-256: `FB701C90D2DD97A1E6D962154692C48EA0F4E77E9300F9DFAC653D4AB67BBC51`.

## 6. Known later work

- Phase 6 must define `bridge/CONTRACT.md`, JSON/error envelopes, authenticated query/command action names, lossless ID serialization, refresh behavior, and Kotlin/TypeScript drift checking.
- Phase 7 must prove the first physical-device authenticated React → WebView → Kotlin → Room round trip.
- React still contains mock/local business state and client-side calculations; Phase 8 must replace them screen by screen rather than adapting them as truth.
- Event actor identity remains null for legacy or currently unattributed event writers until authenticated command wiring attributes future events.
- The optional financial explainer/info-button layer remains deferred.

## 7. Next gate

Begin Phase 6 by turning the frozen DTO/query catalog into one canonical bridge contract. Do not implement broad React wiring before the contract and its drift checker exist.

## Plain English

The trusted Android side can now answer every main question the future screens need: what stock exists, who owes what, what happened today, what was corrected, whether cash matches, what profit is actually known, which costs need attention, who is signed in, and what that person may see. Money travels as exact paisa, and passwords and recovery secrets never travel to the screens. The next job is to define the secure messenger that carries these answers between Android and the new interface.
