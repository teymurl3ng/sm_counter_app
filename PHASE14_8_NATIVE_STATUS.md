# Phase 14.8 native UI — status overview

Date: 2026-09-22  
Branch: `fix/native-shell-vs-content-owner-work`

## Snapshot

| Family | Status |
|---|---|
| Cashier (`Sell / Collect / Activity / More` + Expense host) | **Done** |
| Operations | **Done** |
| Owner primary nav + Work entries + More deep dirs + Month reports + Reopen day + Products create/edit/archive/unarchive + Customers create/edit + Suppliers create/edit + Employees create/edit + Shop settings identity/rates/categories + Users enroll/disable/role-change + Costing policy approve + Backup export/restore/Drive | **Done** (read slices / money posts + Cash close reopen + Products write depth + Customers write depth + Suppliers write depth + Employees write depth + Shop settings write depth + Users & roles write depth + Costing approve write depth + Backup write depth) |
| Manager·Admin shells | **Manager M2 Records+More Done**; **Admin A2 Records + system More Done** (M0–A2 foundation depth) |

## Done (recent)

- **14.8.46 A2** Admin Records + system More — Records hub (business records + security history under Admin chrome); More system tools (Costing / Shop settings / Products / local encrypted export only — Drive/restore omitted); `qaScreen -Pqa.screen=adminshell` (+ managershell re-check for shared HostedScaffold)
- **14.8.46 A1** Admin Work desk + People — setup-health Work (enrollment / rates / employee-link gaps) + Expense CORE METHOD host; People hub (Users & roles enroll Manager/Cashier/Ops only + Employees/Customers/Suppliers under Admin chrome); `qaScreen -Pqa.screen=adminshell`
- **14.8.45 M2** Manager Records + bounded More — Records list/detail under Manager chrome; shop-tools More (Products/Customers/Suppliers/Employees + personal security); Owner-only rows omitted; `qaScreen -Pqa.screen=managershell`
- **14.8.45 M1** Manager Work desk + Close host — Expense / Pay supplier CORE METHOD wrappers; Cash close under Close (reopen hidden); Sale/Collect/Ops entries deferred; `qaScreen -Pqa.screen=managershell`
- **14.8.46** Admin shell M0 — `Work / People / Records / More` chrome + stubs; `open_admin=true`; no Cashier fall-through; `qaScreen -Pqa.screen=adminshell`
- **14.8.45 M0** Manager shell chrome — `Work / Close / Records / More`; `open_manager=true` + `preview.manager`; no Cashier fall-through
- **14.8.44** Owner Backup export / restore / Drive connect — write depth inside Backup (`BackupManager` + `NativeOwnerCloudBackupGateway` / live Google authorization); cloud remote list/inspect/restore still deferred; live Google picker device-manual beyond fake-gateway tests
- **14.8.43** Owner Costing & plans policy approve — write depth inside Costing & plans (`CostingPolicyService.approve` + full passphrase reauth); budgets / yield / opening basis / in-place edit still deferred
- **14.8.42** Owner Users & roles enroll + disable/enable + role change — write depth inside Users & roles (`UserSecurityService.beginUserEnrollment` / `completeUserEnrollment` / `setUserEnabled` / `changeRole` + full passphrase reauth); credential reset still deferred
- **14.8.41** Owner Shop settings identity + rates + expense category create/archive — write depth inside Shop settings (`StoreIdentityService` / `BusinessSettingsService` / `ExpenseCategoryManagementService`); opening record/correct still deferred
- **14.8.40** Owner Employees create + rename/profile — write depth inside Employees (`EmployeeManagementService.create` / `rename` + `EmployeeProfileService.save`)
- **14.8.39** Owner Suppliers create + rename/profile — write depth inside Suppliers (`VendorManagementService.create` / `rename` + `VendorProfileService.save`)
- **14.8.38** Owner Products unarchive — Active\|Archived filter + restore via `ProductCatalogService.setArchived(false)`
- **14.8.37** Owner Customers rename + profile save — write depth inside Customers (`CustomerManagementService.rename` + `CustomerProfileService.save`)
- **14.8.36** Owner Customers create — write depth inside Customers (`CustomerManagementService.create`)
- **14.8.35** Owner Products edit + soft archive — write depth inside Products (`ProductCatalogService.update` / `setArchived`)
- **14.8.34** Owner Products create — write depth inside Products (`ProductCatalogService.create`)
- **14.8.33** Owner Reopen day — depth inside Cash count and close (`REOPEN_DAY` + full passphrase re-auth)
- **14.8.32** Owner Month reports — Overview nested destination; `profitabilityReport` read panel
- **14.8.27–30** Owner Work money (Expense…Cash close)
- **14.8.22–26** Owner More Costing…Backup
- **14.8.31** Cashier Expense host — shared `ExpenseEntryContent` + `CashierExpenseEntryScreen` (CORE METHOD)

## Deferred depth

- Manager Corrections write UI; residual Sale/Collect/Ops hosted entries
- Remaining Owner More write UIs (Shop settings opening record/correct / Users credential reset / Costing budgets·yield·opening-basis / Backup cloud remote restore); Cashier Deliveries-from-More; custom report date range

## Recommended next

1. Manager Corrections write UI; residual Sale/Collect hosting if needed
2. Residual Owner More depth (opening state / credential reset / costing budgets·yield / cloud remote restore)

## Authority

- Owner/Cashier blueprints through `PHASE14_8_44`; Manager M2 `PHASE14_8_45`; Admin A2 `PHASE14_8_46`
- `.cursor/rules/native-shell-vs-content.mdc`
