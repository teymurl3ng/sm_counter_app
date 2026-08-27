# Phase 1 — Full AI Studio ↔ Phase 18 Capability Inventory

Status: COMPLETE — source-level inventory baseline

## Purpose

This phase maps the desired capabilities represented by the new AI Studio UI against the actual native capabilities in the untouched Phase 18 codebase.

The rule is not "missing in Phase 18 = delete it." A capability that is genuinely desired proceeds to Phase 2 for product approval and, if approved, Phase 3 for native implementation.

## Classification

- EXISTING / READY TO WIRE — native capability exists and its semantics are sufficient.
- EXISTING / BRIDGE REQUIRED — native capability exists, but the future React/native bridge must expose it.
- NATIVE EXTENSION REQUIRED — desired UI capability has no adequate native implementation.
- BUSINESS DECISION REQUIRED — capability changes product/domain behavior and needs explicit approval.
- UI-ONLY / COSMETIC — useful presentation behavior that should not become domain state.
- CONFLICT / REMOVE FROM BUSINESS MODEL — UI behavior contradicts the frozen accounting architecture.

## Core Transaction Inventory

| UI capability | AI Studio location | Phase 18 native equivalent | Status | Notes |
|---|---|---|---|---|
| Milk purchase / supplier intake | Daily Operations | `VendorPurchaseService.record` + `VendorPurchaseCalculator` | EXISTING / BRIDGE REQUIRED | Native Fat/LR/SNF/TS/adjusted-litres/amount calculation already exists. |
| Retail sale | Daily Operations / POS | `SaleService.record` | EXISTING / BRIDGE REQUIRED | Supports quantity-or-value, payment account/on-credit, surcharge, channel. |
| Wholesale cash sale | Wholesale | `SaleService.record` | EXISTING / BRIDGE REQUIRED | Use canonical `wholesale` channel; Cash/Bank account required. |
| Wholesale credit sale | Wholesale | `SaleService.record` | EXISTING / BRIDGE REQUIRED | Uses customer account; no cash account movement for credit. |
| Customer credit collection | Accounts / payment flow | `CreditCollectionService.record` | EXISTING / BRIDGE REQUIRED | Native collection event exists. |
| Supplier payment | Supplier ledger / payment flow | `VendorPaymentService.record` | EXISTING / BRIDGE REQUIRED | Native vendor payment exists. |
| Expense | Expenses | `ExpenseService.record` | EXISTING / BRIDGE REQUIRED | Native expense event exists. |
| Salary payment | Employees / payment flow | `SalaryPaymentService.record` | EXISTING / BRIDGE REQUIRED | Native salary payment exists. |
| Yogurt production | Daily Operations | `YogurtProductionService.record` | EXISTING / BRIDGE REQUIRED | Native production event exists. |
| Transaction correction | History / correction UI | `CorrectionService.correct` | EXISTING / BRIDGE REQUIRED | Append-only correction model exists. |

## Master Data / People

| UI capability | Existing native support | Status | Required action |
|---|---|---|---|
| Create customer | `CustomerManagementService.create` | EXISTING / BRIDGE REQUIRED | Bridge + UI mapping. |
| Rename customer | `CustomerManagementService.rename` | EXISTING / BRIDGE REQUIRED | Bridge + UI mapping. |
| Create supplier | `VendorManagementService.create` | EXISTING / BRIDGE REQUIRED | Bridge + UI mapping. |
| Rename supplier | `VendorManagementService.rename` | EXISTING / BRIDGE REQUIRED | Bridge + UI mapping. |
| Create employee | `EmployeeManagementService.create` | EXISTING / BRIDGE REQUIRED | Bridge + UI mapping. |
| Rename employee | `EmployeeManagementService.rename` | EXISTING / BRIDGE REQUIRED | Bridge + UI mapping. |
| Customer phone | UI field exists | NATIVE EXTENSION REQUIRED | No native customer profile field exists. Decide in Phase 2. |
| Customer route/area | UI field exists | NATIVE EXTENSION REQUIRED | No native profile persistence. |
| Customer credit limit | UI field exists | NATIVE EXTENSION REQUIRED | No native persistence. |
| Customer daily quota | UI field exists | NATIVE EXTENSION REQUIRED | No native persistence. |
| Supplier phone | UI field exists | NATIVE EXTENSION REQUIRED | No native profile persistence. |
| Supplier location/notes | UI field exists | NATIVE EXTENSION REQUIRED | No native profile persistence. |
| Supplier default milk rate | UI field exists | CONFLICT / PRODUCT DECISION | Current native purchase rate comes from business settings, not per-supplier master data. |
| Employee role | UI field exists | NATIVE EXTENSION REQUIRED | Current native employee model is name + salary account identity only. |
| Employee phone | UI field exists | NATIVE EXTENSION REQUIRED | No native persistence. |
| Employee salary/salary type as master data | UI field exists | NATIVE EXTENSION REQUIRED | Current salary payment event exists, but employee master salary fields do not. |
| Employee attendance | UI action exists | NATIVE EXTENSION REQUIRED | No attendance domain model/service exists. |

## Inventory / Product Management

| UI capability | Existing native support | Status | Required action |
|---|---|---|---|
| View Milk/Yogurt/current stock | `CurrentStockView`, read model | EXISTING / BRIDGE REQUIRED | Native current stock exists. |
| Add product | UI function exists | NATIVE EXTENSION REQUIRED | `ProductEntity` exists, but no product-management domain service exists. |
| Edit product details | UI function exists | NATIVE EXTENSION REQUIRED | No native product update service. |
| Archive/inactivate product | UI function exists | NATIVE EXTENSION REQUIRED | No native active/archive field in `ProductEntity`. |
| Direct stock count/set stock | UI function exists | BUSINESS DECISION REQUIRED / DOMAIN EXTENSION | Current inventory is event-derived; direct mutation conflicts with current architecture unless a proper adjustment event is explicitly designed. |
| Spoilage/spillage entry | UI function exists | BUSINESS DECISION REQUIRED / NATIVE EXTENSION REQUIRED | No spoilage domain event/service in Phase 18. |

## Daily Operations / Delivery

| UI capability | Existing native support | Status | Required action |
|---|---|---|---|
| Customer delivery record | UI function exists | NATIVE EXTENSION REQUIRED | No delivery entity/service exists. |
| Mark delivery delivered | UI function exists | BUSINESS DECISION REQUIRED | Needs real operational delivery model if retained. |
| Delivery route | UI field exists | NATIVE EXTENSION REQUIRED | No native delivery/profile persistence. |
| Delivery credit revenue | UI metric exists | REQUIRES NATIVE READ-MODEL DESIGN | Financial sale is native; delivery operational state is not. |
| Daily close / lock day | UI function exists | BUSINESS DECISION REQUIRED / NATIVE EXTENSION REQUIRED | Phase 18 has no daily-close domain event/lock model. |
| Counted physical cash | UI function exists | NATIVE EXTENSION REQUIRED | Current native model has no persisted physical-count record. |

## Planning / Budgeting

| UI capability | Existing native support | Status | Notes |
|---|---|---|---|
| Budget planning | `BudgetManagementService` | EXISTING / BRIDGE REQUIRED | Native budget model/service already exists. |
| Copy previous budget period | `copyPreviousPeriod` | EXISTING / BRIDGE REQUIRED | Native support exists. |
| Sales target | `activeSalesTargetForWeek` + budget creation | EXISTING / BRIDGE REQUIRED | Native budget layer supports target derivation. |
| Collections target | `createOrReviseCollectionsTarget` | EXISTING / BRIDGE REQUIRED | Native support exists. |
| Budget actuals | `AnalyticsService.budgetActuals` | EXISTING / BRIDGE REQUIRED | Native read-side support exists. |
| Planning UI | No current AI Studio Planning screen in the submitted UI source | UI/PRODUCT DECISION | Native capability exists, but current AI Studio source does not expose a planning screen. |

## Reporting / Dashboard

| UI capability | Native support | Status | Notes |
|---|---|---|---|
| Current stock | `AnalyticsService.currentPosition` / read model | EXISTING / BRIDGE REQUIRED | Native. |
| Account balances | native read model | EXISTING / BRIDGE REQUIRED | Native. |
| Period sales value | `AnalyticsService.periodSummary` | EXISTING / BRIDGE REQUIRED | Native. |
| Purchase amount/litres | `periodSummary` | EXISTING / BRIDGE REQUIRED | Native. |
| Production/consumption | `periodSummary` | EXISTING / BRIDGE REQUIRED | Native. |
| Vendor payments | `periodSummary` | EXISTING / BRIDGE REQUIRED | Native. |
| Collections | `periodSummary` | EXISTING / BRIDGE REQUIRED | Native. |
| Operating expenses | `periodSummary` | EXISTING / BRIDGE REQUIRED | Native. |
| Salary payments | `periodSummary` | EXISTING / BRIDGE REQUIRED | Native. |
| COGS | UI displays it | NATIVE EXTENSION / BUSINESS DECISION | Current AnalyticsService does not provide an authoritative COGS result. Do not fake it in React. |
| Gross profit | UI displays it | NATIVE EXTENSION / BUSINESS DECISION | Depends on authoritative cost basis. |
| Net profit | UI displays it | NATIVE EXTENSION / BUSINESS DECISION | Must derive from approved native accounting model. |
| Per-product gross profitability | UI displays it | NATIVE EXTENSION / BUSINESS DECISION | Current native layer does not expose this. |
| Locked-day archive report | UI displays it | NATIVE EXTENSION REQUIRED | Current Phase 18 has no daily-close history model. |

## History / Corrections

| UI capability | Existing native support | Status |
|---|---|---|
| Global event history | `HistoryDataRepository.allEvents` | EXISTING / BRIDGE REQUIRED |
| Event detail | `HistoryDataRepository.eventById/detailData` | EXISTING / BRIDGE REQUIRED |
| Append-only correction | `CorrectionService.correct` | EXISTING / BRIDGE REQUIRED |
| Effective corrected state | Native correction/read logic | EXISTING / BRIDGE REQUIRED |
| Account-specific ledger | Current account balance/read models; no dedicated contact-ledger query | NATIVE EXTENSION REQUIRED |

## Settings / Preferences

| UI capability | Existing native support | Status |
|---|---|---|
| Vendor milk rate | `BusinessSettingsService` | EXISTING / BRIDGE REQUIRED |
| Vendor delivery fee | `BusinessSettingsService` | EXISTING / BRIDGE REQUIRED |
| TS standard | `BusinessSettingsService` | EXISTING / BRIDGE REQUIRED |
| Retail milk rate | `BusinessSettingsService` | EXISTING / BRIDGE REQUIRED |
| Retail yogurt rate | `BusinessSettingsService` | EXISTING / BRIDGE REQUIRED |
| Default delivery surcharge | `BusinessSettingsService` | EXISTING / BRIDGE REQUIRED |
| Theme preference | native `SharedPreferences` preference layer exists | EXISTING / BRIDGE REQUIRED |
| Language preference | native preference layer exists | EXISTING / BRIDGE REQUIRED |
| Display density | native preference layer exists | EXISTING / BRIDGE REQUIRED |
| Default reporting period | native preference layer exists | EXISTING / BRIDGE REQUIRED |
| Default opening section | native preference layer exists | EXISTING / BRIDGE REQUIRED |
| Store name | UI state only in current AI Studio source | NATIVE EXTENSION REQUIRED if desired to persist |
| Currency symbol | UI state only in current AI Studio source | NATIVE EXTENSION REQUIRED if desired to persist |
| Starting cash / opening stock value | UI state only in current AI Studio source; not part of `BusinessSettingsService` | BUSINESS DECISION REQUIRED |
| Custom expense categories | UI state only in current AI Studio source | NATIVE EXTENSION REQUIRED if persistent business data; otherwise UI preference |

## Backup / Restore / Export

| UI capability | Native support | Status |
|---|---|---|
| Full backup creation | `BackupDataRepository` / `BackupManager` | EXISTING / BRIDGE REQUIRED |
| Restore | `BackupDataRepository.restore` | EXISTING / BRIDGE REQUIRED |
| Integrity validation | backup implementation | EXISTING / BRIDGE REQUIRED |
| Export receipt image | browser/UI `html2canvas` | UI-ONLY / SYSTEM CAPABILITY | Does not currently map to native business state. |
| Export PDF receipt | browser/UI `jspdf` | UI-ONLY / SYSTEM CAPABILITY | Presentation artifact, not ledger data. |
| Arbitrary JSON import | UI `importAllData` | CONFLICT / REMOVE FROM BUSINESS MODEL | Current native backup model should remain authoritative; do not import arbitrary local UI state. |
| Reset/clear all business data | UI `clearAllData` | BUSINESS DECISION REQUIRED | Must not be implemented as casual UI localStorage deletion. |

## Critical Finding: AI Studio Local State Is Not the Native Truth

The submitted AI Studio `useMilkShopViewModel.ts` maintains local state and `localStorage` for:

- products;
- customers;
- suppliers;
- employees;
- business settings;
- supplier deliveries;
- attendances;
- customer deliveries;
- retail sales;
- wholesale sales;
- expenses;
- spoilages;
- customer payments;
- supplier payments;
- audit logs;
- daily closes;
- day locks;
- counted cash.

These local implementations are useful as product/UI intent evidence, but they are not to be copied into the final hybrid as a second accounting system.

## Priority Native Expansion Candidates

These are the major capabilities the new UI appears to want that Phase 18 does not currently implement adequately:

1. Product master management.
2. Customer profile data.
3. Supplier profile data / per-supplier operational data if desired.
4. Employee profile and attendance.
5. Delivery operational layer.
6. Spoilage/wastage event model.
7. Persisted physical cash count/reconciliation record.
8. Account-specific ledgers.
9. Daily close/locked-day model if desired.
10. Authoritative COGS/profit/reporting model if those metrics are genuinely required.
11. Persistent store identity/currency/opening-state model if required.
12. Persistent expense-category management if required.

These are not implementation approvals. They proceed to Phase 2 for explicit product decisions.

## Canonical Business Vocabulary Identified During Phase 1

The SQL/native model must remain canonical. The sale channel vocabulary is the existing Phase 18 vocabulary, not the strings invented by the AI Studio mockup.

Current schema channel values:
- `retail`
- `wholesale`
- `credit-retail`
- `credit-wholesale`

A future bridge contract must define these once and forbid alternate spellings.

## Phase 1 Exit Condition

Phase 1 is complete when every meaningful UI capability has a native status and a clear next action.

Phase 2 now decides which NATIVE EXTENSION REQUIRED and BUSINESS DECISION REQUIRED items are part of the real product.
