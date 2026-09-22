# Phase 14.8.41 — Owner Shop settings edit (write depth)

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-22

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

**Shop settings identity / rates / expense-category writes are Owner write depth inside the existing Owner Shop settings surface** — not a new More tile and not a foreign-role host:

1. **Role-neutral content** — identity/rates/category forms (fields, loading, failure, draft leave-guard) stay presentation-only inside Shop settings content. No `OwnerSection` / `CashierSection` / `OperationsSection` / foreign bottom nav in reusable content.
2. **Role-owned shell** — continue using existing Owner Shop settings wrappers under `OwnerScaffold(More)`; hardware Back clears edit/create/detail → settings panel → More menu; `EntryLeaveGuard` on Back **and** bottom-nav exits while a draft is dirty.
3. **Foundation proof** — existing Owner Shop settings Foundation (host chrome ✓, foreign chrome ✗, Back → More menu, session intact) still covers the destination; edit forms are depth on that same entry.

**Forbidden:** mounting hybrid Settings React workspace / foreign shells; calling JSON bridge from Compose; inventing opening-state record/correct UI in this slice; changing `StoreIdentity` / `BusinessSettings` / `ExpenseCategoryManagement` domain validation.

## Connected (this slice)

### A. Store identity

Replace deferred identity edit with a real form prefilled from the current snapshot, saved via typed `StoreIdentityService.save(StoreIdentityInput(storeName, phone, address))` through `NativeOwnerShopSettingsFacade.saveIdentity`:

| Field | Rules (match `StoreIdentityService.save`) |
|---|---|
| storeName | Required, non-empty trim, no newline |
| phone, address | Optional (`cleanOptional`) |

Permission: `MANAGE_BUSINESS_SETTINGS` + Owner (same as Shop settings read / bridge `saveStoreIdentity`). Leave-guard while dirty.

### B. Business rates

Replace deferred rates edit with a real form prefilled from raw `BusinessSettings` Doubles on the snapshot (display labels retained), saved via typed `BusinessSettingsService.update(BusinessSettings(...))` through `NativeOwnerShopSettingsFacade.updateRates`:

| Field | Rules (match `BusinessSettingsService.validatePositive`) |
|---|---|
| milkRateVendor, deliveryFeeVendor, retailRateMilk, retailRateYogurt, defaultDeliverySurcharge | Finite Double **> 0** (PKR money units, same as domain / MainActivity) |
| tsStandard | Finite Double **> 0** (percent) |

Permission: `MANAGE_BUSINESS_SETTINGS` + Owner. Leave-guard while dirty.

### C. Expense categories

- **Create:** name-only form → `ExpenseCategoryManagementService.create` via `NativeOwnerShopSettingsFacade.createExpenseCategory`. Duplicate name → surface domain/bridge-equivalent error. Permission: `MANAGE_MASTER_DATA` + Owner (match bridge).
- **Archive / unarchive:** from category detail (or row action) with confirm → `setArchived(accountId, archived)` via `NativeOwnerShopSettingsFacade.setExpenseCategoryArchived`. Permission: `MANAGE_MASTER_DATA` + Owner. Leave-guard on create draft only.

### D. Opening state — still deferred

Keep an **explicit remaining notice on the opening section only**. Do **not** implement `recordOpeningState` / `correctOpeningState` here (recent auth + special conflicts). Documented as remaining deferred depth.

Remove the global “Editing comes next” / Edit-settings preview notice for connected identity, rates, and categories.

## Native data boundary

`NativeOwnerShopSettingsFacade`:

- `saveIdentity(session, StoreIdentityInput)` — `requireOwnerBusinessSettings` then `StoreIdentityService.save`
- `updateRates(session, BusinessSettings)` — `requireOwnerBusinessSettings` then `BusinessSettingsService.update`
- `createExpenseCategory(session, name): Long` — `requireOwnerMasterData` then `ExpenseCategoryManagementService.create`
- `setExpenseCategoryArchived(session, accountId, archived)` — `requireOwnerMasterData` then `ExpenseCategoryManagementService.setArchived`
- snapshot exposes raw rate Doubles for prefills **and** keeps PKR/percent display labels
- never call bridge JSON from Compose; do not change Room schema, RolePermissions, or domain validation

## Explicit preview / deferred

- Opening state record / correct (recent auth + conflict guards)
- Manager / Admin Shop settings shells

## Accept gate

**AUTO-ACCEPTED.** Connected = Owner Shop settings identity + rates + expense category create/archive write depth under CORE METHOD; opening writes remain deferred with an explicit section notice.
