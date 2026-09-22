# Phase 14.8.21 — Owner Employees blueprint

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-21

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

Owner Employees is a **leadership More deep destination** for Salary-category employee master data — not Ops Attendance, not Users & roles (staff login accounts). Still:

1. **Role-neutral content** — employee list, detail, loading, failure, and optional create-preview presentation must not import `OwnerSection` / `CashierSection` / `OperationsSection`, and must not render any role’s bottom nav when extracted for reuse.
2. **Role-owned shell** — Employees wraps content in `OwnerScaffold` / Owner bottom nav; Back returns to Owner More menu (or clears detail first); session stays Owner-authenticated.
3. **Foundation proof** — Owner chrome present, Cashier/Ops chrome absent, Back → Owner More menu (detail → list → menu), session remains authenticated.

**Forbidden in this checkpoint (and forever):**

- Mounting Ops Attendance, Cashier Collect, or any foreign shell as Owner Employees.
- Translating `CashierSection` / `OperationsSection` events inside Owner.
- Treating callback-only “open Employees” intents as hosted-entry coverage.
- Confusing Employees (Salary accounts / profiles) with **Users & roles** (staff login accounts) — Users & roles stays deferred under “Your account and staff access”.
- Inventing salary-payment formulas or attendance write UIs. Profile create/rename/save write depth is owned by **14.8.40**.

Reuse sibling **patterns** from Owner Suppliers (14.8.20) / Owner Customers (14.8.19) nesting under Owner More. Prefer a dedicated `NativeOwnerEmployeesFacade` + Employees destination under Owner More (same nesting model as Suppliers / Customers / Products / Personal security).

## Where this checkpoint starts

Owner **Overview**, **Work**, **Records**, **More**, More **Products**, **Customers**, and **Suppliers** are connected in the debug `NativePreviewActivity` harness (`open_owner=true`). Owner More **Employees** still surfaces an explicit deferred preview notice (`PHASE14_8_17`).

Authority for product shape:

- `PHASE14_8_17_OWNER_MORE_BLUEPRINT.md` — deferred Employees row under “People and businesses”
- `PHASE14_8_20_OWNER_SUPPLIERS_BLUEPRINT.md` — primary structural sibling (list + read detail under More)
- `PHASE14_8_19_OWNER_CUSTOMERS_BLUEPRINT.md` — nesting sibling under Owner More
- `PHASE14_8_2_JOURNEY_DIRECTIONS.md` — “People and businesses: Employees”
- `PHASE14_8_2_ROLE_CALIBRATION.md` / Counter Ledger leadership — Owner master-data ownership
- Shell law: `PHASE14_8_15A_SHELL_VS_CONTENT.md`
- Existing typed domain: `EmployeeManagementService` / `EmployeeInfo`; `EmployeeProfileService` / `EmployeeProfileEntity`; account category `Salary`; balances from existing `readModelDao().accountBalances()` via management list (no new formulas)
- Sibling UI: Owner Suppliers / Customers list/detail under Owner More

This checkpoint owns **Owner Employees** conservatively: employee (Salary account) list + read detail under Owner More. It does **not** require create/edit/deactivate write UI in the first connected slice if write wiring would expand scope without clear proof. Attendance marking stays Ops Work / Attendance territory.

## What the existing product already guarantees

- Owner has `MANAGE_MASTER_DATA` and can already create/rename employees and save profiles through typed management / command paths.
- `EmployeeManagementService.list()` returns Salary-category accounts with display name and balance from existing read-model balances — no new formulas.
- `EmployeeProfileService.get` returns role, phone, reference salary, salary period, join date, and active flag.
- Bridge JSON handlers must not be called from Compose; consume typed Kotlin services/facades only.
- Owner More already provides shell, business-date context, and nested destination Back (Personal security, Products, Customers, Suppliers).

## Reuse / do-not-reuse

| Piece | Reuse? |
|---|---|
| Owner bottom nav + green header / `OwnerScaffold` | Yes |
| Owner Suppliers nested destination + Back stack (detail → list → More) | Yes — primary structural sibling |
| Ops Attendance mark-present as Employees admin | **No** |
| Users & roles / staff login enrollment as Employees | **No** — stays deferred under access section |
| `EmployeeManagementService.list` | Yes — primary list boundary |
| `EmployeeProfileService.get` | Yes — detail facts |
| Balance from management list / existing accountBalances | Yes — display only; do not invent formulas |
| Create / rename / profile save write UI | Optional — deferred with explicit preview unless already proven simple |
| Bridge JSON / `query.masterData` from Compose | **No** |
| Customers / Suppliers / Backup under Employees | **No** |

## Normal path

1. An Owner signs in (`open_owner=true`) and opens **More**.
2. Tap **Employees** → Owner Employees list (not a deferred snackbar).
3. Header stays Owner More chrome (title **Employees** under Owner header tokens); business date / Open-Closed from More context.
4. List shows employees: name primary; balance + active/inactive (+ role when present) secondary from typed fields.
5. Tap a row → read-only detail: name, balance, profile facts (role, phone, reference salary, salary period, join date, status).
6. Back from detail → Employees list. Back from list → Owner More menu. Hardware Back matches that stack.
7. Optional **Add employee** affordance: if write path is deferred, explicit preview notice only (“comes next”); do not invent a fake form.
8. Load failure shows inline error + Retry. Empty list shows an honest empty state.
9. Owner nav remains; no Cashier/Ops bottom nav.

## Connected vs preview / deferred in this checkpoint

### Connected (first slice)

- Employees destination under Owner More (replace Employees deferred notice).
- Employee list from `EmployeeManagementService` (+ profile active / role labels).
- Read detail with profile facts where present.
- Owner chrome + Back → More menu; inventory + Foundation + `qaScreen` (extend `ownermore`).

### Explicit preview / deferred (superseded write depth)

- Create / rename / deactivate / profile edit write UI — **connected in Phase 14.8.40**
- Salary payment posting as admin UI
- Attendance marking / history as Employees admin UI (Ops Attendance remains separate)
- Users & roles / staff login accounts
- Shop settings / backup
- Manager / Admin Employees shells

## Control contract

| Control | Required workflow | Visible result | Native test obligation |
|---|---|---|---|
| Employees row | Open Employees list | Salary accounts; Owner nav | Destination + tags |
| Employee row | Open detail | Read-only employee facts | Detail tags |
| Back (detail) | Return to list | List restored | Back contract |
| Back (list) | Return to More menu | Menu restored | Back contract |
| Retry | Reload list | Ready list | Failure recovery |
| Add employee (if preview) | Explicit notice | Stay on Employees | Notice contract |
| Foreign chrome | Must not appear | No Cashier/Ops nav | Foundation |

## Material states

| State | Behavior |
|---|---|
| Loading | Owner chrome + progress |
| Ready list | Employees; empty copy when none |
| Detail | Read-only facts |
| Load failure | Inline error + Retry |
| Create deferred | Explicit preview notice |
| Session expired | Leave via existing auth path |

## Native data boundary

`NativeOwnerEmployeesFacade` (presentation-neutral) + Owner More ViewModel Employees destination:

- require authenticated Owner session;
- require `MANAGE_MASTER_DATA` for employee read;
- list via `EmployeeManagementService.list()`;
- detail via management row + `EmployeeProfileService.get` — balances only as already returned by management list;
- optional create/rename only via typed services already defined — never bridge JSON from Compose;
- do not change Room schema, RolePermissions, Analytics, or balance engines.

## Visual / consistency notes

- Nested under Owner More like Suppliers (Back to More; detail → list).
- Dense list rows: name primary; status + balance (+ role) secondary.
- Detail uses labeled facts, not an attendance dock or salary-payment picker.
- Deferred create is a real control with explicit notice — not a silent no-op.
- Copy must say **Employees** (people / Salary accounts), never “Users” or login accounts.

## Out of scope

- Full people-master write suite unless explicitly connected above
- Attendance / salary-payment workflow UIs
- Users & roles administration
- Foreign role Employee screens
- Launcher migration away from `HybridActivity`

## Verification

- Facade tests: Owner gate, list employees with balance/status, reject non-Owner.
- Compose contract: Employees list + detail tags; Employees row opens destination (not deferred notice).
- Inventory rows with real workflow test names (no `workflow_test=-`).
- `qaScreen -Pqa.screen=ownermore` extended with Employees Foundation/contract coverage.
- Foundation: Owner chrome ✓, foreign nav ✗, Back → More menu, session intact.
- Prove on device: `:app:assembleDebug` → `adb install -r` → launch `NativePreviewActivity` with `open_owner=true`.

## Accept gate

**AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION.** Connected scope: employee list + read detail under Owner More shell; create/edit deferred with explicit preview unless write path is already proven in-slice.
