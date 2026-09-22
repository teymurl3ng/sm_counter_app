# Phase 14.8 native UI — status overview

Date: 2026-09-22  
Branch: `fix/native-shell-vs-content-owner-work`

## Snapshot

| Family | Status |
|---|---|
| Cashier (`Sell / Collect / Activity / More` + Expense host) | **Done** |
| Operations | **Done** |
| Owner primary nav + Work entries + More deep dirs + Month reports + Reopen day + Products create/edit/archive/unarchive + Customers create/edit + Suppliers create/edit + Employees create/edit + Shop settings identity/rates/categories/**opening** + Users enroll/disable/role-change/**credential reset** + Costing policy approve + Backup export/restore/Drive/**cloud remote restore** | **Done** |
| Manager·Admin shells | **Manager M3 Sale/Collect/Ops hosts Done**; **Admin A2 Records + system More Done** (M0–A2 foundation depth) |

## Done (recent)

- **14.8.47** Owner More residuals — Shop settings opening record/correct (+ recent auth); Users credential reset; Backup cloud remote list/inspect/restore via typed gateway (fake-gateway in CI; live Drive list device-manual). Admin Backup stays `localExportOnly`. Gates: `qaScreen -Pqa.screen=ownermore` **204/204 PASS**; `qaScreen -Pqa.screen=managershell` **48/48 PASS**; debug APK installed with `open_owner=true`.
- **14.8.45 M3** Manager Sale / Collect / Ops Work hosts — `ManagerSaleEntryScreen` / `ManagerCollectionEntryScreen` / Receive milk / Make yogurt / Spoilage / Deliveries CORE METHOD wrappers under Manager Work; Pay salary + reopen still excluded; `qaScreen -Pqa.screen=managershell` **48/48 PASS**
- **14.8.46 A2** Admin Records + system More — Records hub; More system tools (local export only — Drive/restore omitted)
- **14.8.46 A1** Admin Work desk + People
- **14.8.45 M2** Manager Records + bounded More
- **14.8.45 M1** Manager Work desk + Close host
- **14.8.46** Admin shell M0
- **14.8.45 M0** Manager shell chrome
- **14.8.44** Owner Backup export / restore / Drive connect — cloud remote list/inspect/restore **connected in 14.8.47** (fake-gateway tests; live Google list device-manual)
- **14.8.43** Owner Costing & plans policy approve — budgets / yield / opening basis / in-place edit still deferred
- **14.8.42** Owner Users & roles enroll + disable/enable + role change — credential reset **connected in 14.8.47**
- **14.8.41** Owner Shop settings identity + rates + expense categories — opening record/correct **connected in 14.8.47**
- **14.8.40…34** Employees / Suppliers / Products / Customers write depth
- **14.8.33…17** Owner Reopen / Month / Work money / More deep dirs

## Deferred depth

- Manager Corrections write UI; Approval queue write UI
- Costing budgets · yield · opening-basis; Cashier Deliveries-from-More; custom report date range

## Recommended next

1. Manager Corrections write UI / Approval queue
2. Costing budgets / yield / opening cost basis

## Authority

- Owner/Cashier blueprints through `PHASE14_8_47`; Manager M3 `PHASE14_8_45`; Admin A2 `PHASE14_8_46`
- `.cursor/rules/native-shell-vs-content.mdc`
