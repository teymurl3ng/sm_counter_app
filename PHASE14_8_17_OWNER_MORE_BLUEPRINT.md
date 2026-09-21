# Phase 14.8.17 — Owner More blueprint

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-21

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

Owner More is a **leadership destination**, not a hosted Cashier More or Operations More screen. Still:

1. **Role-neutral content** — menu rows, personal-security form, loading, failure presentation must not import `OwnerSection` / `CashierSection` / `OperationsSection`, and must not render any role’s bottom nav when extracted for reuse.
2. **Role-owned shell** — Owner More wraps content in `OwnerScaffold` / Owner bottom nav / Owner Back path; session stays Owner-authenticated.
3. **Foundation proof** — Owner chrome present, Cashier/Ops chrome absent, Back returns to Owner More menu (or leaves authenticated area from More home), session remains authenticated.

**Forbidden in this checkpoint (and forever):**

- Mounting `CashierMoreScreen` or `OperationsMoreScreen` (or any foreign shell) as Owner More.
- Translating `CashierSection` / `OperationsSection` events inside Owner.
- Treating callback-only “open More” intents as hosted-entry coverage.
- Inventing master-data editors, backup/restore write UI, security-audit browsers, or month-report UIs behind live rows.

Reuse sibling **patterns** (green header, menu-row rhythm, personal Quick unlock form) from Cashier More / Operations More under `OwnerScaffold`. Prefer extracting role-neutral personal-security content when practical; adapting the sibling form once under Owner shell is acceptable for this slice if foreign nav never appears.

## Where this checkpoint starts

Owner **Overview**, **Work**, and **Records** are connected in the debug `NativePreviewActivity` harness (`open_owner=true`). Owner **More** still shows an explicit preview notice (`OwnerMorePreview`).

Authority for product shape:

- `PHASE14_8_2_ROLE_CALIBRATION.md` — Owner More: products, people, planning, shop settings, access/users, security history, backup/recovery
- `PHASE14_8_2_JOURNEY_DIRECTIONS.md` — More sections (Things you sell; People and businesses; Planning and setup; Your account and staff access; Safety and recovery)
- `PHASE14_8_2_COUNTER_LEDGER_LEADERSHIP.md` — complete business-control More
- Sibling shell patterns: `PHASE14_8_13_OPERATIONS_MORE_BLUEPRINT.md`, Cashier More (`CashierMoreScreen` / `NativeMoreFacade`)
- Blueprint *structure*: `PHASE14_8_14` / `15` / `16` Owner family blueprints
- Shell law: `PHASE14_8_15A_SHELL_VS_CONTENT.md`
- Existing Quick unlock: `QuickUnlockService` via the same enroll/remove path Cashier/Ops More already use

This checkpoint owns **Owner More home** conservatively: sectioned menu matching journey groups, connected personal security + switch account + sign out, and explicit preview notices for deep destinations not yet native. It does **not** implement Products/Customers/Suppliers/Employees editors, costing/plans, shop settings, users/roles admin, security history browser, or backup/restore write UI.

## What the existing product already guarantees

- Owner has full native permissions, including personal Quick unlock enrollment on the signed-in account.
- Quick unlock enroll/remove already exists via `QuickUnlockService` (same path Cashier More and Operations More use).
- Leaving the authenticated area already invalidates the native session (`leaveAuthenticatedArea` / switch account / sign out).
- Business date open/closed and shop name already project through Owner Overview / Work / Records context pattern.
- There is **no** Compose Owner More screen and **no** `NativeOwnerMoreFacade` yet — the native facade must compose presentation from existing services without inventing master-data or backup write flows.
- Bridge JSON handlers must not be called from native UI; consume typed Kotlin services/facades only.

## Reuse / do-not-reuse

| Piece | Reuse? |
|---|---|
| Owner bottom nav + green header / `OwnerScaffold` tokens | Yes |
| Cashier / Ops More **menu-row** and **personal security form** layout patterns | Yes — under Owner shell (or as extracted role-neutral content) |
| Cashier More / Ops More **whole screen** + foreign nav | **No** |
| `NativeMoreFacade` Count cash / blind count | **No** — Cashier-only; Owner cash & close stays Work / later |
| Ops More “Today’s work” physical list | **No** — Operator personal utilities, not Owner More |
| `QuickUnlockService` enroll/remove / status | Yes — same personal path as Cashier/Ops |
| Products / customers / suppliers / employees / settings / access / backup editors | **No** in this checkpoint — explicit preview notices only |
| Month Reports / Cash & close write UI | **No** — Overview / Work territory |

## Normal path

1. An Owner signs in (preview harness: `open_owner=true`) and opens **More** from Owner navigation.
2. Header matches Owner chrome: title **More**, shop/identity line, business date, Open/Closed. Do not paste Cashier or Ops More header/nav.
3. First viewport is the sectioned More landing (journey groups):
   - **Things you sell** — Products (**connected in 14.8.18** — list + read detail; was preview in 14.8.17)
   - **People and businesses** — Customers (**connected in 14.8.19** — list + read detail); Suppliers (**connected in 14.8.20** — list + read detail); Employees (preview)
   - **Planning and setup** — Costing & plans, Shop settings (preview)
   - **Your account and staff access** — Personal security (connected Quick unlock); Users & roles, Security history (preview)
   - **Safety and recovery** — Backup & restore (preview)
   - **Account** — Switch account, Sign out (connected)
4. Tap **Personal security** → Owner-scoped Quick unlock manage (status, enroll PIN / phone security, remove with confirmation) using the same service path as Cashier/Ops More.
5. Back from Personal security returns to Owner More menu. Hardware Back on More home leaves the authenticated area like Overview/Work/Records home (or returns to Overview if that is the existing Owner Back contract for non-home tabs — keep consistent with current OwnerPreviewHost More behavior after wiring).
6. Tap a deferred row → explicit not-connected preview notice (snackbar or inline notice). Do not open a fake empty editor.
7. Switch account → leave session → account picker. Sign out → leave session → Welcome.
8. Load failure shows inline error + Retry. Empty security status still allows enroll when Quick unlock is available.

## Connected vs preview / deferred in this checkpoint

### Connected (first slice)

- Owner More home shell (replace `OwnerMorePreview` notice).
- Sectioned menu matching journey groups.
- Personal security (Quick unlock status + enroll/remove).
- Switch account + Sign out.
- Owner chrome + Back contracts; inventory + `qaScreen` gate.

### Explicit preview / deferred (do not invent these UIs here)

- Products master-data editor — **connected in Phase 14.8.18** as active list + read detail under Owner More (create/edit remain deferred there unless explicitly proven)
- Customers directory — **connected in Phase 14.8.19** as list + read detail under Owner More (create/edit remain deferred there)
- Suppliers directory — **connected in Phase 14.8.20** as list + read detail under Owner More (create/edit remain deferred there)
- Employees directories and editors
- Costing / plans, shop settings, opening state, expense categories
- Users & roles administration
- Security history / audit browser
- Local backup, Drive backup, restore write UI
- Month reports, Cash & close (remain Overview / Work)
- Manager / Admin More shells

## Control contract

| Control | Required workflow | Visible result | Native test obligation |
|---|---|---|---|
| More nav | Open Owner More home | Sectioned menu; Owner nav selected | Destination asserted; preview notice gone |
| Personal security | Open Quick unlock manage | Status + enroll/remove controls | Destination + tags |
| Deferred row (Employees, etc.) | Preview notice | Explicit not-connected copy | Tags + notice contract |
| Products row | Open Owner Products | Active list under Owner More | Connected in 14.8.18 |
| Customers row | Open Owner Customers | Customer list under Owner More | Connected in 14.8.19 |
| Suppliers row | Open Owner Suppliers | Supplier list under Owner More | Connected in 14.8.20 |
| Switch account | Leave → picker | Account picker | Leave path |
| Sign out | Leave → Welcome | Welcome | Foundation leave |
| Back (security) | Return to More menu | Menu restored | Back contract |
| Retry (load failure) | Reload context | Ready menu | Failure then recovery |
| Overview / Work / Records nav | Existing Owner nav | Unchanged destinations | Nav contracts |
| Hardware Back on More home | Leave authenticated area (or Overview per host contract) | Session ends or Overview | Back contract |
| Foreign chrome | Must not appear | No Cashier/Ops bottom nav | Foundation |

## Material states

| State | Behavior |
|---|---|
| Loading | Owner chrome + progress; no Cashier/Ops More chrome |
| Ready menu | Sectioned journey groups; connected + preview rows |
| Personal security | Quick unlock status; enroll/remove; confirm remove dialog |
| Load failure | Inline error + Retry |
| Deferred destination tap | Explicit not-connected notice; stay on More |
| Closed business date | Open/Closed chip reflects closed; More remains usable |
| Session expired | Leave authenticated area via existing auth path |

## Native data boundary

`NativeOwnerMoreFacade` (presentation-neutral) + thin ViewModel:

- require authenticated session; role must be Owner for this shell (preview path seeds/opens Owner);
- require `VIEW_OPERATIONAL` for context load;
- compose business date / posting open / shop name from existing Room / settings reads (same pattern as Owner Overview/Records);
- compose personal security from `QuickUnlockService` status for the signed-in login (same fields as `NativeMoreSecurityStatus`);
- enroll/remove Quick unlock via existing service API; wipe passphrase/PIN char arrays after use;
- do **not** change Room schema, RolePermissions, Analytics formulas, backup cryptography, or user-admin write paths;
- do **not** call bridge JSON handlers;
- do **not** mount or call Cashier Count cash from Owner More.

## Visual / consistency notes

- Owner More ≠ Cashier More / Ops More chrome. Same product family: denser leadership menu, journey section labels, business-control destinations.
- Follow Owner header / NavigationBar tokens already used on Overview, Work, and Records.
- Reuse Counter Ledger menu-row rhythm (icon, title, subtitle, chevron, dividers) from Cashier/Ops More without Cashier Sell dock or Ops nav.
- Deferred destinations appear as real rows that surface explicit preview notices — not disabled silent no-ops, and not invented editors.
- Personal security copy matches Cashier/Ops (“Quick unlock for this account on this phone”).

## Out of scope

- Full master-data editors (products, people, costing, settings)
- Users/roles write UI, security audit browser
- Backup/restore write UI
- Month Reports, Cash & close
- Manager / Administrator / Cashier / Ops More shells
- Mounting or translating foreign role screens
- Launcher migration away from `HybridActivity`
- Changing Overview/Work/Records connected behavior except harness wiring needed for More entry

## Verification

- Facade unit/instrumentation coverage for Owner-only gate, context load, security status, and no cash-count fields on the Owner More boundary.
- Compose contract tests for More home + personal security tags; register inventory rows with real workflow test names (no `workflow_test=-` for connected controls).
- Harness: `qaScreen -Pqa.screen=ownermore` (add to `phase18/build.gradle.kts` screen map). Keep `owner` / `ownerwork` / `ownerrecords` green when practical.
- Foundation: Owner chrome ✓, foreign chrome ✗, Back → More menu / leave path, session intact for connected slice.
- Prove on A06 with `:app:assembleDebug` → `adb install -r` (preserve preview accounts) → force-stop → launch `NativePreviewActivity` with `open_owner=true`.

## Accept gate

**AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION.** Connected vs deferred scope follows this blueprint: personal security + switch/sign-out connected; deep master-data / backup / audit destinations deferred with explicit preview notices under Owner shell.
