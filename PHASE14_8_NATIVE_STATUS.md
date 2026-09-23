# Phase 14.8 native UI — status overview

Date: 2026-09-23
Branch: `codex/owner-more-modularization`

## Snapshot

| Family | Status |
|---|---|
| Cashier (`Sell / Collect / Activity / More` + Expense host) | **Done** |
| Operations | **Done** |
| Owner primary nav + Work entries + More deep dirs + Month reports + Reopen day + Products create/edit/archive/unarchive + Customers create/edit + Suppliers create/edit + Employees create/edit + Shop settings identity/rates/categories/**opening** + Users enroll/disable/role-change/**credential reset** + Costing policy approve + **budgets / collections / yield** + Backup export/restore/Drive/**cloud remote restore** + Records **corrections write** | **Done** |
| Manager·Admin shells | **Manager M3 Sale/Collect/Ops hosts + Records corrections write Done**; **Admin A2 Records + system More Done** (M0–A2 foundation depth) |

## Done (recent)

- **Owner More modularization + native-preview launcher correction** — the 4,570-line `NativeOwnerMoreViewModel` is now a roughly 470-line coordinator over feature-owned catalog, people, costing, settings, users/security, backup, navigation, state, and runtime boundaries. Focused unit/device gates, `qaFast`, lint, Kotlin compilation, and debug/release assembly pass. The isolated `.preview` launcher now resolves to `NativePreviewActivity`; release continues to retain the hybrid launcher until the Phase 14.8.6 cutover gate.
- **14.8.50 audit remediation** — debug/native-preview now has an isolated application ID and release-disabled fixtures; authenticated hosts follow system theme; passphrases/PINs no longer use saveable state and are wiped across submission, early-return, retry, cancellation, and ViewModel teardown paths; Cashier/Operations/Owner/Manager/Admin shells switch from bottom navigation to a rail at 600dp; inherited lint errors are closed; pending Owner backup/user workflows are extracted and backup secret ownership is centralized; the frozen-baseline script works in Windows PowerShell 5.1 and PowerShell 7. Gates: `qaFast` PASS; `lintDebug` PASS (`0 errors`, `181 warnings`); debug + release assemble PASS; affected UI contract matrix **51/51 PASS** on SM-A065F, including adaptive-shell boundary **2/2**.
- **14.8.49** Owner Costing & plans budgets + collections targets + yield policy approve (Owner + Admin host; leave-guard; yield recent-auth). Opening cost basis still deferred. Gates: `ownermore` **213/213 PASS**; `adminshell` **38/38 PASS**. Debug APK with `open_owner=true`.
- **14.8.48** Native Corrections write UI (Step 1) — typed `NativeOwnerRecordsFacade.correct` (same rules as `HistoryBridgeCommandService.correct`); role-neutral correction editor under Owner/Manager/Admin Records hosts; Corrections group filter connected; leave-guard on dirty draft. **Approval queue still deferred** (no domain API). Gates: `managershell` **50/50 PASS**; `ownermore` **213/213 PASS**. Debug APK with `open_manager=true`.
- **14.8.47** Owner More residuals — Shop settings opening record/correct (+ recent auth); Users credential reset; Backup cloud remote list/inspect/restore via typed gateway (fake-gateway in CI; live Drive list device-manual). Admin Backup stays `localExportOnly`.
- **14.8.45 M3** Manager Sale / Collect / Ops Work hosts
- **14.8.46 A2** Admin Records + system More
- **14.8.46 A1** Admin Work desk + People
- **14.8.45 M2** Manager Records + bounded More
- **14.8.45 M1** Manager Work desk + Close host
- **14.8.46** Admin shell M0
- **14.8.45 M0** Manager shell chrome
- **14.8.44…17** Owner Backup / Costing / Users / Shop settings / Employees / Suppliers / Products / Customers / Reopen / Month / Work money / More

## Deferred depth

- Approval queue write UI (no domain API — do not invent)
- Costing opening-basis; production-loss assessment write UI; Cashier Deliveries-from-More; custom report date range
- Records Balances & ledgers / Closing history groups

## Recommended next

1. Opening cost basis (Costing Step 5)
2. Records Balances & ledgers / Closing history groups
3. Cashier Deliveries-from-More, custom report date range, and production-loss assessment write depth
4. Approval queue only after an authoritative domain API exists
5. Native endpoint-ownership closure before WebView/bridge retirement and the full 14.8.7 regression

## Authority

- Owner/Cashier blueprints through `PHASE14_8_49`; Manager M3 `PHASE14_8_45`; Admin A2 `PHASE14_8_46`
- `.cursor/rules/native-shell-vs-content.mdc`
