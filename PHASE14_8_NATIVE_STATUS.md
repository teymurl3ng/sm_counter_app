# Phase 14.8 native UI — status overview

Date: 2026-09-24
Branch: `codex/native-menu-coverage`

## Snapshot

| Family | Status |
|---|---|
| Cashier (`Sell / Collect / Activity / More` + Expense host) | **Done** |
| Operations | **Done** |
| Owner primary nav + Work entries + More deep dirs + Month/**custom-range** reports + Reopen day + Products create/edit/archive/unarchive + Customers create/edit + Suppliers create/edit + Employees create/edit + Shop settings identity/rates/categories/**opening** + Users enroll/disable/role-change/**credential reset** + Costing policy approve + **budgets / collections / opening cost basis / yield / production-loss assessment** + Backup export/restore/Drive/**cloud remote restore** + Records **balances / ledgers / closing / corrections write** | **Done** |
| Manager·Admin shells | **Manager M3 Sale/Collect/Ops hosts + Records corrections write Done**; **Admin A2 Records + system More Done** (M0–A2 foundation depth) |

## Done (recent)

- **14.8.53 Native menu coverage + UX baseline** — Records Balances/ledgers and Closing history are live for Owner/Manager/Admin; Cashier More opens Deliveries; Reports accepts validated custom ranges; Yield records and revises authoritative production-loss assessments. Warm Records/Reports refreshes retain content with inline progress, and all four More families share a short secondary transition while primary tabs remain immediate. Gates: `qaFast` PASS (`975` inventory rows, `0` untested/orphans); lint + unit + debug/release assembly PASS; focused device suites **45/45**, **18/18**, and Reports facade **5/5** PASS; latest `.preview` APK installed and native activity confirmed foreground. Contract: `PHASE14_8_53_NATIVE_MENU_COVERAGE_AND_UX_BASELINE.md`.
- **14.8.52 Native navigation runtime** — Cashier, Operations, Owner, Manager, and Administrator primary destinations now retain same-session ViewModel state; warm switches no longer close/reopen screens or force blocking reloads. Owner More uses explicit cold/warm readiness (including empty lists), stale session/read results are rejected, independent reads are parallelized, and per-tab saveable state preserves scroll/local UI state. Gates: `qaFast` PASS (`966` inventory rows, `0` untested/orphans); unit + Kotlin + Android-test compilation PASS; `lintDebug` PASS; debug assembly PASS; focused SM-A065F warm-navigation regression PASS; latest `.preview` APK installed. Contract: `PHASE14_8_52_NATIVE_NAVIGATION_RUNTIME.md`.
- **14.8.51 Owner opening stock cost basis** — Owner and Administrator Costing & plans now expose the effective positive Milk/Yogurt opening quantities, missing valuation coverage, typed initial/corrected cost-basis capture, documented-vs-estimate evidence, explicit validation, append-only supersession, recent-auth retry, dirty-draft protection, large-font-safe tabs, and direct Shop settings recovery. No schema or accounting rule changed. Gates: `qaFast` PASS (`966` inventory rows, `0` untested/orphans); focused device suite **34/34 PASS** on SM-A065F; forced-fresh debug + release assembly PASS; installed native-preview verification PASS; independent finish review APPROVED.
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

## Recommended next

0. DONE 2026-09-26 — interaction polish (14.8.54a/b/c) and endpoint-ownership closure (14.8.55/55b): 91/91 actions have native ownership; WebView/bridge retirement (14.8.6) is now unblocked
1. Reconcile the existing Vercel account service with the native identity/session model: inventory contracts, choose source-of-truth boundaries, and add a typed client behind the current local interfaces
2. Run a whole-app interaction polish pass on real data/device traces: remaining inline progress, micro-feedback, accessibility motion checks, and long-list performance
3. Approval queue only after an authoritative domain API exists
4. Native endpoint-ownership closure before WebView/bridge retirement and the full 14.8.7 regression

## Authority

- Owner/Cashier blueprints through `PHASE14_8_53`; Manager M3 `PHASE14_8_45`; Admin A2 `PHASE14_8_46`
- `.cursor/rules/native-shell-vs-content.mdc`
