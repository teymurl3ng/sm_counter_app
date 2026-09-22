# Phase 14.8.1 — Native surface and dependency inventory

Status: **COMPLETE — migration boundary and surface ownership recorded**

Date: 2026-09-12

## Purpose

This inventory establishes what must move, what must remain authoritative, and how the working hybrid application stays available while Saaf Milk is redesigned in native Android UI. It is source-derived from the current launcher, React shell, bridge contract, dispatcher, Kotlin read models, command services, application container, manifest, and build configuration.

No business logic, schema, credential behavior, bridge contract, or launcher changed in this checkpoint.

## Current runtime

- `HybridActivity` is the exported launcher.
- It creates the native security, Quick unlock, backup picker, cloud authorization, bridge dispatcher, and secure WebView host.
- The WebView loads packaged React assets from `phase18/app/src/main/assets/hybrid`.
- `HybridAppShell` owns bootstrap/login/recovery presentation and loads authorized workspace data.
- `ManagementWorkspace` owns fourteen role-filtered top-level areas.
- `HybridNavigation` implements four preferred destinations plus a large overflow menu.
- Android Back, dirty-form confirmation, rotation restoration, renderer recovery, keyboard/inset handling, and foreground/background session behavior currently cross the Activity/JavaScript boundary.
- The app module has no Compose dependencies or Compose build feature.

## Authentication and startup surfaces

| Surface/state | Current operations | Native authority | Migration requirement |
|---|---|---|---|
| Secure connection check | `system.bridgeInfo`, `query.bootstrapStatus`, `system.quickUnlockStatus`, optional `query.currentSession` | Bridge snapshot, `HybridReadModelService`, `QuickUnlockService`, native session manager | Native root performs bootstrap/session checks directly; bridge handshake disappears with the WebView. |
| First owner setup | `command.auth.bootstrapOwner` | `UserSecurityService` through the authentication adapter | Native form preserves passphrase policy, recovery-code acknowledgement, and no-default-owner rule. |
| Sign in | `command.auth.login`, `command.auth.quickLogin` | `UserSecurityService`, `QuickUnlockService` | Native account selection, full passphrase, Quick PIN, and Phone security use the existing services and generic verification errors. |
| Owner recovery | `command.auth.recoverOwner` | `UserSecurityService` | Native flow preserves recovery proof, forced session changes, and the last-recoverable-owner safeguards. |
| Recovery-code acknowledgement | presentation state after owner bootstrap/recovery | Native security result plus UI-only acknowledgement | Native flow makes the save/acknowledge checkpoint obvious and prevents accidental loss. |

## Authenticated application areas

| Current area | Permission gate | Capability family | Current native owner | Migration order |
|---|---|---|---|---|
| Dashboard | `view_operational`; financial sections additionally filtered | operational, cashier, financial, profitability summaries and date periods | `HybridReadModelService` plus operational/financial read services | 2 — part of the vertical slice |
| Daily work | any relevant transaction permission | purchase, sale, production, collection, supplier payment, expense, salary, spoilage | `OperationsBridgeCommandService`, typed domain inputs, read models | 3 — Sale first, then remaining operations |
| Deliveries | `delivery` | create, progress, link sale, archive | `DeliveryBridgeCommandService`, operational reads | 4 |
| Accounts | `view_full_financial` | balances, account ledger, supplier payment shortcut | financial read service and operations command service | 5 |
| History | `view_operational`; correction action requires `correction` | event list, event detail, correction | operational reads and `HistoryBridgeCommandService` | 6 |
| Cash close | `cash_count_and_close`; reopen requires `reopen_day` | count, close, reopen, archive | reconciliation read/command services | 7 |
| Products | `manage_master_data` | create, update, archive, unarchive | `HybridCommandService`, identity/master-data reads | 8 |
| Customers | `manage_master_data` | create, rename, profile, enable/disable | `HybridCommandService`, identity/master-data reads | 8 |
| Suppliers | `manage_master_data` | create, rename, profile, enable/disable | `HybridCommandService`, identity/master-data reads | 8 |
| Employees | `manage_master_data` or `attendance` | employee profile/status and attendance | employee command service and operational reads | 8 |
| Cost & plans | `manage_business_settings` | budgets, targets, costing policy, opening cost, yield, loss assessment | costing/planning read and command services | 9 |
| Settings | master-data or business-settings permission by section | shop identity, rates, categories, opening state | identity reads and settings command service | 9 |
| Access & users | always visible for own access; privileged sections filtered | own passphrase, Quick unlock, staff users, roles/status, enrollment/reset, audit | `UserSecurityService`, `QuickUnlockService`, security read/command services | 10 |
| Backup & restore | export permission; restore controls additionally filtered | encrypted export/inspect/restore and Drive connect/configure/run/list/inspect/restore/disconnect | backup manager, cloud coordinator, Android picker/authorization, native security | 11 |

The order is a migration dependency order, not the proposed final navigation order. Checkpoint 14.8.2 owns the final information architecture.

## Bridge action ownership

The 91 contract-v5 actions collapse into these native capability groups:

| Group | Actions | Current adapter/service | Native migration disposition |
|---|---:|---|---|
| Bridge handshake | 1 | `HybridBridgeDispatcher`/snapshot | Retire after the last WebView caller. |
| Authentication/session | 8 | `AuthenticationBridgeHandler`, read models, `UserSecurityService` | Add a typed native authentication facade; retain the security service. |
| Quick unlock | 5 | `QuickUnlockBridgeHandler`, `QuickUnlockService` | Call the service from the native Activity/ViewModel; retain Android device gate. |
| Security administration | 8 | security handler/read/command services | Expose typed operations outside bridge JSON parsing. |
| Master/reference data | 11 | master-data handler, `HybridCommandService`, read models | Retain typed commands/DTOs; move or wrap bridge-named orchestration. |
| Employees/attendance | 5 | employee handler/read/command services | Retain typed command behavior; expose through neutral application API. |
| Settings/opening state | 7 | settings handler/read/command services | Preserve reauthentication and opening-state audit semantics. |
| Operations/reports | 13 | operations handler/read/command services | First command family migrated; retain all domain validation. |
| Deliveries | 5 | delivery handler/read/command services | Expose typed application boundary. |
| Account ledger | 1 | ledger handler/financial read service | Use read model directly. |
| History/corrections | 3 | history handler/read/command services | Preserve native original-value reconstruction and correction lineage. |
| Reconciliation/close | 4 | reconciliation handler/read/command services | Preserve count-before-close, close locks, verified actors, and reopen audit. |
| Costing/planning | 9 | costing/planning handler/read/command services | Preserve approved calculations and disclosure codes exactly. |
| Local encrypted backup | 3 | backup handler/manager/document picker | Replace bridge ticket presentation with Activity Result APIs while keeping inspection and strong-approval boundaries. |
| Cloud backup | 8 | cloud handler/coordinator/authorization | Retain provider, cryptography, outbox, scheduling, retention, vault, and restore behavior. |
| **Total** | **91** | | |

## Reusable native layers

### Directly reusable

- `MilkShopApplication` owns the database, security/session service, backup services, and cloud components.
- `HybridReadModelService` already exposes typed suspend reads for the current session, dashboard, transaction references, history, ledgers, deliveries, attendance, reconciliation, profitability, costing, planning, users, and security audit.
- The Kotlin DTO catalog already represents money as integer paisa strings at the bridge boundary and preserves explicit date, quantity, correction, and coverage state.
- Domain services and transaction repositories remain the only business authority.
- Quick unlock already owns Android biometric/device-credential prompts and device-bound records.

### Reusable after presentation-neutral extraction

- `OperationsBridgeCommandService`
- `DeliveryBridgeCommandService`
- `EmployeeBridgeCommandService`
- `HistoryBridgeCommandService`
- `ReconciliationBridgeCommandService`
- `SettingsBridgeCommandService`
- `CostingPlanningBridgeCommandService`
- `SecurityAdministrationBridgeCommandService`
- `HybridCommandService`

These services mostly accept typed Kotlin/domain inputs and enforce native transactions/permissions. Their useful orchestration should be moved or wrapped without copying it. JSON handlers remain adapters for fallback screens until retirement.

### Reference only

- the old XML `MainActivity` and imperative `presentation/*Ui.kt` classes;
- earlier ViewModels whose state matches a retained workflow;
- the React components as a list of capabilities and material edge cases;
- Phase 14 screenshots as behavior/accessibility evidence.

The old presentation is not a visual or structural target.

## Target native boundary

Each Compose screen family will use:

`Composable screen → screen ViewModel → typed application facade → domain/repository/security service → Room`

Rules:

- screen state is immutable and observable;
- state flows down and user events flow to the ViewModel;
- UI code does not calculate financial or inventory truth;
- UI code does not decide permissions or assert actor identity;
- session handles remain in native memory and never enter saved UI state;
- secrets do not enter navigation arguments, logs, screenshots, or retained state;
- every mutation has running, success, failure, and refreshed-result behavior;
- destructive and replacement operations keep explicit native approval/check flows;
- one-shot effects such as navigation, system pickers, and biometric prompts are Activity-owned.

## Coexistence and rollback boundary

- `HybridActivity` remains the launcher until the native authentication/dashboard/Sale vertical slice passes.
- The new Compose root is introduced as a separate native Activity or controlled route so it can be tested without replacing the accepted launcher.
- During family migration, native state and the WebView fallback share the same `MilkShopApplication`, Room database, native session manager, and services.
- A fallback route receives only a native session reference and destination identifier inside the app process. It does not receive credentials or duplicate data.
- The default launcher changes only after native startup/session behavior and the first vertical slice pass on device.
- A migrated family can be routed back to the hybrid fallback until bridge retirement if a regression is found.
- Database schema changes are outside this phase unless a separately documented product requirement proves one necessary.

## Parity gate for every family

A screen family becomes native-owned only when all applicable items pass:

- capability and result parity against the frozen hybrid baseline;
- native permission denial and session-expiry behavior;
- loading, empty, success, validation, conflict, locked-day, offline, and provider failure states;
- action feedback is visible without manual scrolling;
- Android Back closes layers, warns about dirty drafts, and returns predictably;
- rotation/process restoration preserves safe drafts and clears secrets;
- keyboard, system bars, compact landscape, 200% text, TalkBack, focus, and touch targets;
- targeted refresh shows the saved Room result;
- instrumentation verifies authoritative database/audit effects;
- physical S24 evidence, with A06 used only where a smaller/second device adds value.

## Bridge retirement gate

WebView and bridge removal is allowed only when:

1. all retained product capabilities have a native screen owner;
2. no Compose/ViewModel/application code calls a bridge handler or JSON contract;
3. no runtime route opens packaged hybrid assets;
4. local backup, cloud authorization, Quick unlock, and system pickers are native-owned;
5. full regression and native device acceptance pass;
6. source/package scans report zero live bridge/WebView dependencies;
7. the final native APK is installed and its hash is recorded.

The bridge contract remains frozen historical evidence after retirement; it is not silently rewritten to describe the native presentation API.

## Verification

- repository was clean on `main` and matched `origin/main` before documentation changes;
- current launcher and manifest ownership inspected;
- React route and permission construction inspected;
- all bridge action groups and dispatcher owners inspected;
- application container, typed read service, command services, legacy ViewModels, and build dependencies inspected;
- no Compose dependency or Compose build feature exists at this checkpoint;
- source was not modified.

## Next gate

Checkpoint 14.8.2 must produce the native journey map and direction before production Compose screens are built. Its first design problem is the top-level structure: reduce fourteen peer-looking destinations into a small set of understandable daily-work, records, people/stock, and control journeys while keeping permission-filtered access predictable.
