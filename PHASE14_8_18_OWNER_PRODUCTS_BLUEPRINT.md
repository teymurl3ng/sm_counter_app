# Phase 14.8.18 — Owner Products blueprint

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-21

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

Owner Products is a **leadership More deep destination**, not a hosted Cashier or Ops screen. Still:

1. **Role-neutral content** — product list, detail, loading, failure, and optional create-preview presentation must not import `OwnerSection` / `CashierSection` / `OperationsSection`, and must not render any role’s bottom nav when extracted for reuse.
2. **Role-owned shell** — Products wraps content in `OwnerScaffold` / Owner bottom nav; Back returns to Owner More menu (or clears detail first); session stays Owner-authenticated.
3. **Foundation proof** — Owner chrome present, Cashier/Ops chrome absent, Back → Owner More menu (detail → list → menu), session remains authenticated.

**Forbidden in this checkpoint (and forever):**

- Mounting Cashier Sell, Ops work screens, or any foreign shell as Owner Products.
- Translating `CashierSection` / `OperationsSection` events inside Owner.
- Treating callback-only “open Products” intents as hosted-entry coverage.
- Inventing costing, stock, COGS, or archive-write UIs beyond existing typed catalog fields already exposed by `ProductCatalogService`.

Reuse sibling **patterns** from Owner More personal-security nesting and Cashier list/detail density under `OwnerScaffold`. Prefer a dedicated `NativeOwnerProductsFacade` + Products destination under Owner More (same nesting model as Personal security).

## Where this checkpoint starts

Owner **Overview**, **Work**, **Records**, and **More** are connected in the debug `NativePreviewActivity` harness (`open_owner=true`). Owner More **Products** still surfaces an explicit deferred preview notice (`PHASE14_8_17`).

Authority for product shape:

- `PHASE14_8_17_OWNER_MORE_BLUEPRINT.md` — deferred Products row under “Things you sell”
- `PHASE14_8_2_JOURNEY_DIRECTIONS.md` — “Things you sell: Products”
- `PHASE14_8_2_ROLE_CALIBRATION.md` / Counter Ledger leadership — Owner master-data ownership
- `PHASE14_8_1_NATIVE_MIGRATION_INVENTORY` — Products row
- Shell law: `PHASE14_8_15A_SHELL_VS_CONTENT.md`
- Existing typed domain: `ProductCatalogService` / `ProductCatalogItem` / `ProductCatalogInput`; write path already used by `HybridCommandService.createProduct` / `updateProduct` / `setProductArchived` (permission `MANAGE_MASTER_DATA`)
- Sibling UI: Owner More menu → sub-screen (Personal security); Cashier list/detail density where helpful

This checkpoint owns **Owner Products** conservatively: active catalog list + read detail under Owner More. It does **not** require create/edit/archive write UI in the first connected slice if write wiring would expand scope without clear proof.

## What the existing product already guarantees

- Owner has `MANAGE_MASTER_DATA` and can already create/update/archive products through typed `HybridCommandService` + `ProductCatalogService` (bridge and legacy management paths).
- `ProductCatalogService.list(includeArchived = false)` returns active products with name, unit, default sale rate, inventory tracking, archived flag, and notes — no new formulas.
- Product identity lives in Room (`ProductDao` + `ProductProfileDao`); archive is soft via profile flag.
- Bridge JSON handlers must not be called from Compose; consume typed Kotlin services/facades only.
- Owner More already provides shell, business-date context, and nested destination Back (Personal security).

## Reuse / do-not-reuse

| Piece | Reuse? |
|---|---|
| Owner bottom nav + green header / `OwnerScaffold` | Yes |
| Owner More nested destination + “Back to More” pattern | Yes |
| Cashier list/detail **density** (row + facts) | Yes — visual sibling under Owner shell |
| Cashier Sell dock / product picker as Products admin | **No** |
| `ProductCatalogService.list` | Yes — primary read boundary |
| `HybridCommandService` / `ProductCatalogService.create|update|setArchived` | Optional in this slice — only if create/edit is proved simple; otherwise keep Add/Edit as explicit preview |
| `query.masterData` bridge JSON from Compose | **No** |
| Costing, stock on hand as primary Products UI | **No** in this checkpoint (rate/status from catalog only) |
| Customers / Suppliers / Employees / Backup under Products | **No** |

## Normal path

1. An Owner signs in (`open_owner=true`) and opens **More**.
2. Tap **Products** → Owner Products list (not a deferred snackbar).
3. Header stays Owner More chrome (title **More** or **Products** under Owner header tokens); business date / Open-Closed from More context.
4. List shows active products: name, unit, rate/status labels from catalog fields already available.
5. Tap a row → read-only detail (or inline facts) for that product: name, unit, default rate, tracking, notes.
6. Back from detail → Products list. Back from list → Owner More menu. Hardware Back matches that stack.
7. Optional **Add product** affordance: if write path is deferred, explicit preview notice only (“comes next”); do not invent a fake form.
8. Load failure shows inline error + Retry. Empty catalog shows an honest empty state.
9. Owner nav remains; no Cashier/Ops bottom nav.

## Connected vs preview / deferred in this checkpoint

### Connected (first slice)

- Products destination under Owner More (replace Products deferred notice).
- Active product list from `ProductCatalogService` (typed facade).
- Read detail (or equivalent inline facts).
- Owner chrome + Back → More menu; inventory + Foundation + `qaScreen` (extend `ownermore`).

### Explicit preview / deferred

- Create / edit / archive write UI (unless a minimal create path is already proven safe in-slice)
- Costing & plans, stock quantities, COGS
- Customers / Suppliers / Employees / shop settings / backup
- Manager / Admin Products shells

## Control contract

| Control | Required workflow | Visible result | Native test obligation |
|---|---|---|---|
| Products row | Open Products list | Active catalog; Owner nav | Destination + tags |
| Product row | Open detail / expand facts | Read-only product facts | Detail tags |
| Back (detail) | Return to list | List restored | Back contract |
| Back (list) | Return to More menu | Menu restored | Back contract |
| Retry | Reload catalog | Ready list | Failure recovery |
| Add product (if preview) | Explicit notice | Stay on Products | Notice contract |
| Foreign chrome | Must not appear | No Cashier/Ops nav | Foundation |

## Material states

| State | Behavior |
|---|---|
| Loading | Owner chrome + progress |
| Ready list | Active products; empty copy when none |
| Detail | Read-only facts |
| Load failure | Inline error + Retry |
| Create deferred | Explicit preview notice |
| Session expired | Leave via existing auth path |

## Native data boundary

`NativeOwnerProductsFacade` (presentation-neutral) + Owner More ViewModel Products destination (or thin dedicated VM hosted by More):

- require authenticated Owner session;
- require `MANAGE_MASTER_DATA` (or Owner + existing master-data gate equivalent) for catalog read;
- list via `ProductCatalogService.list(includeArchived = false)`;
- detail from the same catalog item fields — no new accounting formulas;
- optional create/update only via typed `ProductCatalogService` / existing command service with permission + conflict checks already defined — never bridge JSON from Compose;
- do not change Room schema, RolePermissions, Analytics, or costing engines.

## Visual / consistency notes

- Nested under Owner More like Personal security (Back to More).
- Dense list rows: name primary; unit + rate/status secondary.
- Detail uses labeled facts, not a Sell dock.
- Deferred create is a real control with explicit notice — not a silent no-op.

## Out of scope

- Full master-data write suite unless explicitly connected above
- Stock / costing / profitability editors
- Foreign role Product screens
- Launcher migration away from `HybridActivity`

## Verification

- Facade tests: Owner gate, list active products, reject non-Owner.
- Compose contract: Products list + detail tags; Products row opens destination (not deferred notice).
- Inventory rows with real workflow test names (no `workflow_test=-`).
- `qaScreen -Pqa.screen=ownermore` extended with Products Foundation/contract coverage.
- Foundation: Owner chrome ✓, foreign nav ✗, Back → More menu, session intact.
- Prove on device: `:app:assembleDebug` → `adb install -r` → launch `NativePreviewActivity` with `open_owner=true`.

## Accept gate

**AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION.** Connected scope: active list + read detail under Owner More shell; create/edit deferred with explicit preview unless write path is already proven in-slice.
