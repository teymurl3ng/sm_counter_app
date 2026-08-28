# Phase 5 — Frozen Hybrid-Facing DTO Catalog

**Status: FROZEN — 2026-08-28**

## 1. Contract boundary

This catalog is the authoritative native read boundary for the future React/WebView product. The Kotlin definitions are in:

- `phase18/app/src/main/java/com/saaf/milkshop/application/readmodels/HybridReadModels.kt`;
- `phase18/app/src/main/java/com/saaf/milkshop/application/readmodels/HybridReadModelService.kt`.

Phase 5 defines native queries and response shapes only. It does not define JavaScript action names, JSON envelopes, bridge errors, commands, WebView wiring, or React behavior; those belong to Phase 6 and later.

## 2. Representation rules

1. Every money value uses integer PKR paisa and has a `Paisa` suffix. Rates use `PaisaPerUnit`.
2. Quantities, ratios, and percentages use canonical decimal strings. The bridge must not convert them through binary floating-point and back.
3. Dates and timestamps remain canonical native strings: dates are ISO `YYYY-MM-DD`; persisted timestamps are ISO instants.
4. A nullable financial value means unknown, unavailable, or not authorized. It must never be replaced with zero.
5. Fields ending in `Code` are stable machine values. React must translate them into plain shop language rather than showing raw codes.
6. IDs are native 64-bit identifiers. Phase 6 must choose a lossless JavaScript representation.
7. DTOs never include a session handle, password/passphrase, credential verifier, salt, recovery code/verifier, failed-login state, or lockout timing.

## 3. Frozen query surface

| Native query | Required permission | Frozen response | Purpose |
|---|---|---|---|
| `bootstrapStatus()` | none | `BootstrapStatusDto` | Says only whether first-owner setup is required. |
| `currentSession(sessionHandle)` | valid session / `VIEW_OPERATIONAL` | `CurrentSessionDto` | Authenticated status, current identity, role, optional employee link, effective permissions. The handle is input only. |
| `dashboard(sessionHandle, start, end)` | `VIEW_OPERATIONAL` | `DashboardDto` | Current stock and non-monetary effective operational quantities only. |
| `cashierSummary(sessionHandle, start, end)` | `SALE_AND_COLLECTION` | `CashierSummaryDto` | Payment-account positions plus sales and collection totals needed for till work. |
| `financialOverview(sessionHandle, start, end)` | `VIEW_FULL_FINANCIAL` | `FinancialOverviewDto` | Customer/vendor balances and full period totals. |
| `businessSettings(sessionHandle)` | `MANAGE_BUSINESS_SETTINGS` | `BusinessSettingsDto` | Editable store identity, PKR, business rates, opening state, expense categories. |
| `masterData(sessionHandle)` | `MANAGE_MASTER_DATA` | `MasterDataDto` | Full product/customer/vendor/employee profiles and canonical positions. |
| `transactionReferences(sessionHandle)` | `VIEW_OPERATIONAL` | `TransactionReferencesDto` | Native lookup lists/rates filtered to the signed-in role's actual transaction duties. Contract v2 adds active expense categories for `EXPENSE` users; unauthorized lists are empty and unauthorized rates are null. |
| `eventHistory(sessionHandle)` | `VIEW_OPERATIONAL` | `List<EventHistoryDto>` | Complete append-only event history with correction, superseded, and durable actor fields. |
| `eventDetail(sessionHandle, eventId)` | `VIEW_OPERATIONAL` | `EventDetailDto` | Inventory movements for operational users; financial movements are present only with `VIEW_FULL_FINANCIAL`. |
| `accountLedger(sessionHandle, accountId)` | `VIEW_FULL_FINANCIAL` | `AccountLedgerDto` | Canonical customer/vendor movement ledger and running/current balance. |
| `deliveries(sessionHandle)` | `DELIVERY` | `List<DeliveryDto>` | Active delivery board with current status and sale linkage. |
| `attendance(sessionHandle, businessDate)` | `ATTENDANCE` | `List<AttendanceDto>` | Effective staff attendance for one business date. |
| `spoilageHistory(sessionHandle)` | `PRODUCTION_AND_SPOILAGE` | `List<SpoilageDto>` | Spoilage history with correction state and physical reason code. |
| `reconciliationArchive(sessionHandle)` | `CASH_COUNT_AND_CLOSE` | `ReconciliationArchiveDto` | Physical counts, expected cash, variances, and close/reopen history. |
| `profitabilityReport(sessionHandle, start, end)` | `VIEW_FULL_FINANCIAL` | `ProfitabilityReportDto` | Authoritative cost-based management report with honest null/coverage states. |
| `costingManagement(sessionHandle)` | `MANAGE_BUSINESS_SETTINGS` | `CostingManagementDto` | Cost policies/components, opening bases, yield policies/alerts, loss assessments, and code catalog. |
| `planning(sessionHandle)` | `MANAGE_BUSINESS_SETTINGS` | `PlanningDto` | Versioned budgets and collection targets. |
| `users(sessionHandle)` | `MANAGE_STAFF_USERS` plus native privileged-role rules | `List<UserAdministrationDto>` | Authorized user administration without credential material. |
| `securityAudit(sessionHandle)` | `VIEW_SECURITY_AUDIT` | `List<SecurityAuditDto>` | Durable actor snapshots, action/result/reason codes, target, and non-secret details. |

All permission checks execute in Kotlin before database data is returned. React hiding a button is usability only and never authorization.

## 4. Source-of-truth map

| DTO group | Authoritative native source |
|---|---|
| stock and account positions | Room `current_stock` and `account_balances` read views |
| daily/period totals | `AnalyticsService`, which reads effective leaf events |
| history and corrections | append-only events/logs plus native correction relationships |
| customer/vendor ledger | `AccountLedgerService` canonical financial movements |
| store/opening/settings | `StoreIdentityService`, `OpeningStateService`, `BusinessSettingsService` |
| profiles and operational records | existing profile, attendance, delivery, spoilage, count, and daily-close DAOs |
| COGS/profit | `ProfitabilityReadService`; React may not recompute it |
| costing controls | immutable costing/yield/basis/loss source records and `YieldPolicyService` assessments |
| session/user/audit | `UserSecurityService`, role mapping, app-user rows, audit snapshots, event-actor snapshots |

No DTO reads the React prototype's localStorage, mock arrays, hardcoded `costRate`, or client-side profit calculations.

## 5. Frozen code sets

The Kotlin domain constants remain authoritative. The `CostingCodeCatalogDto` returns the supported costing vocabulary so Phase 6 can preserve it exactly.

- cost coverage: `complete`, `partial`, `missing_basis`, `unsupported_flow`, `stale_conversion_policy`;
- reporting basis: `cost_based_management`;
- valuation scope: `cost_only_no_nrv`;
- conversion components: `culture`, `packaging`, `variable_energy`, `direct_labor`, `fixed_production_overhead`;
- costing review reasons: `monthly`, `component_change`, `capacity_change`, `process_change`, `quarterly_variance`;
- opening basis kinds: `documented_historical`, `owner_estimate`;
- yield statuses: `calibration_pending`, `review_required`, `within_expected_range`;
- production-loss classifications: `ordinary`, `abnormal`;
- production-loss reasons: `spill`, `contamination`, `power_failure`, `equipment_failure`, `measurement_correction`, `other`.

Role and permission codes come from `AppRole` and `AppPermission`. Event/channel, attendance, delivery, close/reopen, security action/result/reason, and profile-status codes pass through their existing native contracts without reinterpretation.

## 6. Security and legacy behavior

- `bootstrapStatus()` is the only unauthenticated read.
- A successful `currentSession()` returns `authenticationStatusCode = authenticated`; an invalid or expired handle throws the native authentication/session failure rather than returning a guessed user.
- `EventHistoryDto.actor` is nullable because pre-security and not-yet-attributed native events are honestly legacy/unverified. When an `event_actor_links` snapshot exists, the durable user ID/name/role snapshot is returned.
- Schema-6 daily-close typed actors remain visible; schema-7+ authenticated records also carry `actor.userId`.
- User-administration DTOs expose identity, role, status, setup-required state, and audit IDs only. Credential and recovery tables are never queried by this layer.
- `SecurityAuditDto.details` is limited to the native audit service's non-secret role/status metadata; no arbitrary credential input is written there.

## 7. Freeze rule

Phase 6 may serialize these shapes but may not silently rename fields, change money/decimal representation, weaken permissions, add secret fields, reinterpret codes, or recalculate facts. Any required change must amend this catalog, update Kotlin and TypeScript together, add drift checks/tests, and record the compatibility impact.
