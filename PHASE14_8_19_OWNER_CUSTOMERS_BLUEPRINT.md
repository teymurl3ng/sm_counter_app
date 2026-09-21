# Phase 14.8.19 — Owner Customers blueprint

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-21

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

Owner Customers is a **leadership More deep destination**, not a hosted Cashier Collect picker or Ops delivery customer screen. Still:

1. **Role-neutral content** — customer list, detail, loading, failure, and optional create-preview presentation must not import `OwnerSection` / `CashierSection` / `OperationsSection`, and must not render any role’s bottom nav when extracted for reuse.
2. **Role-owned shell** — Customers wraps content in `OwnerScaffold` / Owner bottom nav; Back returns to Owner More menu (or clears detail first); session stays Owner-authenticated.
3. **Foundation proof** — Owner chrome present, Cashier/Ops chrome absent, Back → Owner More menu (detail → list → menu), session remains authenticated.

**Forbidden in this checkpoint (and forever):**

- Mounting Cashier Collect, Ops Deliveries, or any foreign shell as Owner Customers.
- Translating `CashierSection` / `OperationsSection` events inside Owner.
- Treating callback-only “open Customers” intents as hosted-entry coverage.
- Inventing balance formulas, credit engines, payment posting, or profile-write UIs beyond existing typed fields already exposed by `CustomerManagementService` / `CustomerProfileService`.

Reuse sibling **patterns** from Owner Products (14.8.18) nesting under Owner More. Prefer a dedicated `NativeOwnerCustomersFacade` + Customers destination under Owner More (same nesting model as Products / Personal security).

## Where this checkpoint starts

Owner **Overview**, **Work**, **Records**, **More**, and More **Products** are connected in the debug `NativePreviewActivity` harness (`open_owner=true`). Owner More **Customers** still surfaces an explicit deferred preview notice (`PHASE14_8_17`).

Authority for product shape:

- `PHASE14_8_17_OWNER_MORE_BLUEPRINT.md` — deferred Customers row under “People and businesses”
- `PHASE14_8_18_OWNER_PRODUCTS_BLUEPRINT.md` — structural sibling (list + read detail under More)
- `PHASE14_8_2_JOURNEY_DIRECTIONS.md` — “People and businesses: Customers”
- `PHASE14_8_2_ROLE_CALIBRATION.md` / Counter Ledger leadership — Owner master-data ownership
- Shell law: `PHASE14_8_15A_SHELL_VS_CONTENT.md`
- Existing typed domain: `CustomerManagementService` / `CustomerInfo`; `CustomerProfileService` / `CustomerProfileEntity`; account category `Customer`; balances from existing `readModelDao().accountBalances()` via management list (no new formulas)
- Sibling UI: Owner Products list/detail under Owner More

This checkpoint owns **Owner Customers** conservatively: customer list + read detail under Owner More. It does **not** require create/edit/deactivate write UI in the first connected slice if write wiring would expand scope without clear proof.

## What the existing product already guarantees

- Owner has `MANAGE_MASTER_DATA` and can already create/rename customers and save profiles through typed management / command paths.
- `CustomerManagementService.list()` returns Customer-category accounts with display name and balance from existing read-model balances — no new formulas.
- `CustomerProfileService.get` returns phone, area/route, address, notes, credit limit, daily quota, and active flag.
- Bridge JSON handlers must not be called from Compose; consume typed Kotlin services/facades only.
- Owner More already provides shell, business-date context, and nested destination Back (Personal security, Products).

## Reuse / do-not-reuse

| Piece | Reuse? |
|---|---|
| Owner bottom nav + green header / `OwnerScaffold` | Yes |
| Owner Products nested destination + Back stack (detail → list → More) | Yes — primary structural sibling |
| Cashier Collect / sale customer picker as Customers admin | **No** |
| `CustomerManagementService.list` | Yes — primary list boundary |
| `CustomerProfileService.get` | Yes — detail facts |
| Balance from management list / existing accountBalances | Yes — display only; do not invent formulas |
| Create / rename / profile save write UI | Optional — deferred with explicit preview unless already proven simple |
| Bridge JSON / `query.masterData` from Compose | **No** |
| Suppliers / Employees / Backup under Customers | **No** |

## Normal path

1. An Owner signs in (`open_owner=true`) and opens **More**.
2. Tap **Customers** → Owner Customers list (not a deferred snackbar).
3. Header stays Owner More chrome (title **Customers** under Owner header tokens); business date / Open-Closed from More context.
4. List shows customers: name primary; balance + active/inactive secondary from typed fields.
5. Tap a row → read-only detail: name, balance, profile facts (phone, route, address, notes, credit limit, daily quota, status).
6. Back from detail → Customers list. Back from list → Owner More menu. Hardware Back matches that stack.
7. Optional **Add customer** affordance: if write path is deferred, explicit preview notice only (“comes next”); do not invent a fake form.
8. Load failure shows inline error + Retry. Empty list shows an honest empty state.
9. Owner nav remains; no Cashier/Ops bottom nav.

## Connected vs preview / deferred in this checkpoint

### Connected (first slice)

- Customers destination under Owner More (replace Customers deferred notice).
- Customer list from `CustomerManagementService` (+ profile active label).
- Read detail with profile facts where present.
- Owner chrome + Back → More menu; inventory + Foundation + `qaScreen` (extend `ownermore`).

### Explicit preview / deferred

- Create / rename / deactivate / profile edit write UI
- Payment posting, credit engines, delivery routes as workflows
- Suppliers / Employees / shop settings / backup
- Manager / Admin Customers shells

## Control contract

| Control | Required workflow | Visible result | Native test obligation |
|---|---|---|---|
| Customers row | Open Customers list | Customer accounts; Owner nav | Destination + tags |
| Customer row | Open detail | Read-only customer facts | Detail tags |
| Back (detail) | Return to list | List restored | Back contract |
| Back (list) | Return to More menu | Menu restored | Back contract |
| Retry | Reload list | Ready list | Failure recovery |
| Add customer (if preview) | Explicit notice | Stay on Customers | Notice contract |
| Foreign chrome | Must not appear | No Cashier/Ops nav | Foundation |

## Material states

| State | Behavior |
|---|---|
| Loading | Owner chrome + progress |
| Ready list | Customers; empty copy when none |
| Detail | Read-only facts |
| Load failure | Inline error + Retry |
| Create deferred | Explicit preview notice |
| Session expired | Leave via existing auth path |

## Native data boundary

`NativeOwnerCustomersFacade` (presentation-neutral) + Owner More ViewModel Customers destination:

- require authenticated Owner session;
- require `MANAGE_MASTER_DATA` for customer read;
- list via `CustomerManagementService.list()`;
- detail via management row + `CustomerProfileService.get` — balances only as already returned by management list;
- optional create/rename only via typed services already defined — never bridge JSON from Compose;
- do not change Room schema, RolePermissions, Analytics, or balance engines.

## Visual / consistency notes

- Nested under Owner More like Products (Back to More; detail → list).
- Dense list rows: name primary; balance + status secondary.
- Detail uses labeled facts, not a Collect dock or delivery picker.
- Deferred create is a real control with explicit notice — not a silent no-op.

## Out of scope

- Full people-master write suite unless explicitly connected above
- Payment / credit / delivery workflow UIs
- Foreign role Customer screens
- Launcher migration away from `HybridActivity`

## Verification

- Facade tests: Owner gate, list customers with balance/status, reject non-Owner.
- Compose contract: Customers list + detail tags; Customers row opens destination (not deferred notice).
- Inventory rows with real workflow test names (no `workflow_test=-`).
- `qaScreen -Pqa.screen=ownermore` extended with Customers Foundation/contract coverage.
- Foundation: Owner chrome ✓, foreign nav ✗, Back → More menu, session intact.
- Prove on device: `:app:assembleDebug` → `adb install -r` → launch `NativePreviewActivity` with `open_owner=true`.

## Accept gate

**AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION.** Connected scope: customer list + read detail under Owner More shell; create/edit deferred with explicit preview unless write path is already proven in-slice.
