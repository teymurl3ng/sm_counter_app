# Phase 14.8.16 — Owner Records blueprint

Status: **DRAFT FOR OWNER APPROVAL** (planning pause — do not implement Compose until accepted)

Date: 2026-09-21

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

Owner Records is a **leadership destination**, not a hosted Cashier Activity. Still:

1. **Role-neutral content** — list, search, loading, failure, and detail presentation must not import `OwnerSection` / `CashierSection` / `OperationsSection`, and must not render any role’s bottom nav.
2. **Role-owned shell** — Owner Records wraps content in `OwnerScaffold` / Owner bottom nav / Owner Back path; session stays Owner-authenticated.
3. **Foundation proof** — if list/detail content is shared with Cashier Activity (or later Manager Records), Foundation must prove Owner chrome present, Cashier/Ops chrome absent, Back returns to Owner Records home (or Owner Overview when leaving the tab), and session remains authenticated.

**Forbidden in this checkpoint (and forever):**

- Mounting `CashierActivityScreen` (or any Cashier shell) as Owner Records.
- Translating `CashierSection` / `OperationsSection` events inside Owner.
- Treating callback-only “open Records” intents as hosted-entry coverage.
- Copying the superseded “host full foreign screen” lines from `PHASE14_8_15_OWNER_WORK_BLUEPRINT.md` (those lines are historical and superseded by 15A).

If Cashier Activity list/detail **row/detail layout** is reused, **extract content first**, then wrap with Owner shell. Do not ship a temporary foreign-screen host.

## Where this checkpoint starts

Owner **Overview** and **Work** are connected in the debug `NativePreviewActivity` harness (`open_owner=true`). Owner Work entry commands use the 15A shell-vs-content pattern (role-neutral entry content + Owner wrappers). Owner **Records** and **More** still show explicit preview notices (`OwnerRecordsPreview` / `OwnerMorePreview`).

Authority for product shape:

- `PHASE14_8_2_COUNTER_LEDGER_LEADERSHIP.md` — Owner business-wide Records
- `PHASE14_8_2_JOURNEY_DIRECTIONS.md` — Records: recent activity, search, balances/ledgers, deliveries, attendance, close history, corrections from detail
- `PHASE14_8_2_ROLE_CALIBRATION.md` — Owner nav `Overview / Work / Records / More`
- Blueprint *structure* (not defective host lines): `PHASE14_8_14_OWNER_OVERVIEW_BLUEPRINT.md`, `PHASE14_8_15_OWNER_WORK_BLUEPRINT.md`
- Shell law: `PHASE14_8_15A_SHELL_VS_CONTENT.md`
- Design references when present in checkout: `design/stitch/reference-blueprints/counter-ledger-leadership.html` (Owner Records states), `design/refined-counter-ledger/` (accepted remakes). **Not present in this checkout** — follow leadership/journey/role docs and Cashier Activity *content* patterns only.
- Sibling for list/detail **content patterns**: Cashier Activity (`PHASE14_8_5_CASHIER_ACTIVITY_BLUEPRINT.md`, `CashierActivityScreen`) — patterns only, not the whole screen.
- Existing reads: `HybridReadModelService.eventHistory` / `eventDetail`, Overview latest-activity projection, `HistoryCorrectionsActions` / `HistoryDataRepository` formatting semantics (read-side only). `NativeActivityFacade` remains Cashier-scoped and must not be the Owner Records data path.

This checkpoint owns **Owner Records home** conservatively: business-wide recent activity, search/filter, and read-only detail drill-in. It does **not** implement corrections write UI, reopen, full account ledgers, month Reports, Cash & close, or Owner More.

## What the existing product already guarantees

- Owner has `VIEW_OPERATIONAL`, `VIEW_FULL_FINANCIAL`, and `CORRECTION` (and full daily-command permissions elsewhere).
- Typed shop-wide history already exists: `HybridReadModelService.eventHistory(session)` → `List<EventHistoryDto>` (all events, actor snapshots, correction/superseded flags).
- Typed detail already exists: `eventDetail(session, eventId)` → inventory movements; financial movements when `VIEW_FULL_FINANCIAL`; `originalValues` when `CORRECTION` (read reconstruction for corrections — **not** a native write UI).
- Owner Overview already projects a single latest business-wide activity row from Room (not Cashier-scoped).
- Cashier Activity is intentionally **narrow**: own actor + current business date + Sale/Collection only via `NativeActivityFacade`. That scoping is correct for Cashier and **wrong** for Owner Records.
- Reconciliation archive (`reconciliationArchive`), deliveries, attendance, and `accountLedger` reads exist typed — available for later Records groups, not required in this first connected slice.
- There is **no** Compose Owner Records screen and **no** `NativeOwnerRecordsFacade` yet — the native facade must compose presentation from existing services without inventing accounting formulas or new correction semantics.
- Bridge JSON handlers must not be called from native UI; consume typed Kotlin services/facades only.

## Reuse / do-not-reuse

| Piece | Reuse? |
|---|---|
| Owner bottom nav + green header / `OwnerScaffold` tokens | Yes |
| Cashier Activity **row / detail field layout** (time, task, amount, counterpart, notes patterns) | Yes — as **role-neutral content** after extract, or as a visual sibling to copy once under Owner shell |
| Cashier Activity **whole screen** (`CashierActivityScreen` + Cashier nav) | **No** |
| `NativeActivityFacade` (own-actor Sale/Collection day list) | **No** as Owner data boundary |
| `HybridReadModelService.eventHistory` / `eventDetail` | Yes — primary read boundary |
| Overview latest-activity labeling conventions (task · #id, actor) | Yes — align copy where sensible |
| `HistoryCorrectionsActions` formatting / detail field ordering | Yes — presentation guidance for detail sections; do not wire XML UI |
| Corrections write / `command.correctEvent` / CorrectionService UI | **No** in this checkpoint (preview or omit) |
| Full `accountLedger` customer/supplier destinations | **No** in this checkpoint (explicit next-family notice if shown as a group) |
| Ops Deliveries / Attendance screens as Records | **No** — those stay role task surfaces; Records may later add history groups without hosting Ops chrome |
| Month Reports / Cash & close / reopen | **No** — Overview / Work / More territory |

## Normal path

1. An Owner signs in (preview harness: `open_owner=true`) and opens **Records** from Owner navigation.
2. Header matches Owner chrome: title **Records**, shop/identity line, business date, Open/Closed. Do not paste Cashier Activity header or Cashier bottom nav.
3. First viewport is the Records landing:
   - **Search** — compact field to filter the recent list (id, type label, actor name, notes, date) without reserving a large empty hero search wall.
   - Optional **Groups** strip (journey): Recent (selected/connected); Balances & ledgers, Closing history, Corrections — explicit “next in family” preview notices if shown; do not invent silent no-ops.
   - **Recent activity** — business-wide reverse-chronological list (all actors, all operational event types the history read returns), not “my sales today.”
4. Each row shows time (or date+time), task label, record identity (#…), actor, and a short amount/quantity/state trailing when projectable from existing event fields without new formulas.
5. Tap a row → Owner-scoped **detail** for that `eventId`: type, dates, actor, notes, inventory movements, financial movements (Owner money allowed), correction lineage flags (`correctsEventId` / superseded), and original-values section when the detail DTO already supplies them.
6. Detail is **read-only** in this checkpoint. No Correct / Void primary actions. If a Correct affordance appears in leadership HTML, show it only as an explicit not-connected notice or omit it.
7. Back from detail returns to the Records list (same search/filter state when practical). Hardware Back on Records home leaves the authenticated area like Overview/Work home (existing Owner Back contract).
8. Empty history shows calm empty copy; nav remains. Load failure shows inline error + Retry.

## Connected vs preview / deferred in this checkpoint

### Connected (first slice)

- Owner Records home shell (replace `OwnerRecordsPreview` notice).
- Business-wide recent activity list from existing history reads.
- Search / filter over that list (presentation-side; no new search index service).
- Read-only detail drill-in from existing `eventDetail`.
- Owner chrome + Back contracts; inventory + `qaScreen` gate.

### Explicit preview / deferred (do not invent write or ledger UIs here)

- Corrections **write** UI (form, save, post-correction refresh) even though Owner has `CORRECTION` and `originalValues` are readable.
- Owner-only **reopen** review / write.
- Customer / supplier **balance list + ledger drill-down** as live destinations (`accountLedger` exists typed — defer UI ownership).
- Dedicated **closing / cash-count / reopen history** browse UI (archive reads exist — defer).
- Dedicated **delivery history** / **attendance history** Records groups (task screens remain on Work / Ops).
- Month **Reports**, Cash & close, Owner More, Manager/Admin Records shells.
- Cross-role shared Records content extraction is encouraged when Cashier Activity content is factored; Foundation proves host chrome. Do not block this checkpoint on Manager/Admin shells.

## Control contract

| Control | Required workflow | Visible result | Native test obligation |
|---|---|---|---|
| Records nav | Open Owner Records home | Recent list (or empty); Owner nav selected | Destination asserted; preview notice gone |
| Search | Filter recent list | Matching rows only; clear empties honestly | Filter contract |
| Record row | Open detail | Detail for that `eventId`; all actors allowed | Correct id; business-wide (not own-only) |
| Back (detail) | Return to list | List restored | Back contract |
| Retry (load failure) | Reload history | Ready or empty | Failure then recovery |
| Deferred group (if shown) | Preview notice | Explicit not-connected copy | Tags + copy |
| Overview / Work / More nav | Existing Owner nav | Unchanged destinations; More may remain notice | Nav contracts |
| Hardware Back on Records home | Leave authenticated area | Welcome/Access like Overview/Work | Back contract |
| Foreign chrome | Must not appear | No Cashier/Ops bottom nav | Foundation if shared content hosted |

## Material states

| State | Behavior |
|---|---|
| Loading | Owner chrome + progress; no Cashier Activity chrome |
| Empty history | Clear empty copy; search/nav remain |
| Ready list | Reverse-chronological business-wide rows |
| Filtered empty | Honest “no matches” for current query |
| Detail | Owner-safe fields including money when permitted; read-only |
| Load failure | Inline error + Retry |
| Not found | Calm denial; return to list |
| Superseded / correction flags | Visible lineage from existing DTO flags; no write |
| Closed business date | Open/Closed chip reflects closed; Records remains readable |
| Session expired | Leave authenticated area via existing auth path |

## Native data boundary

`NativeOwnerRecordsFacade` (presentation-neutral) + thin ViewModel:

- require authenticated session; role must be Owner for this shell (preview path seeds/opens Owner);
- require `VIEW_OPERATIONAL` for list/detail; use `VIEW_FULL_FINANCIAL` for money sections already returned by `eventDetail`;
- compose list from `HybridReadModelService.eventHistory` (or equivalent typed Room projection already used by that read) — **shop-wide**, not `NativeActivityFacade.loadToday`;
- compose detail from `eventDetail` — project labels for task type, actor, inventory, financial movements, notes, correction lineage; do not invent amounts;
- optional presentation filter for search over already-loaded history (id, type, actor, notes, occurred/created dates);
- do **not** change Room schema, RolePermissions, Analytics formulas, CorrectionService write path, or backup cryptography;
- do **not** call bridge JSON handlers;
- do **not** widen Cashier Activity to “all actors” as a shortcut — keep Cashier Activity scoped; Owner Records is a separate facade.

## Visual / consistency notes

- Owner Records ≠ Cashier Activity chrome. Same product family: denser leadership list, actor emphasized, business-wide scope.
- Follow Owner header / NavigationBar tokens already used on Overview and Work.
- Reuse Counter Ledger list-row rhythm from Cashier Activity content where it helps (dividers, chevron, time · task · trailing amount) without Cashier Sell dock or Cashier nav.
- Inaccessible or deferred destinations do not appear as disabled fake rows inside Recent; use explicit preview copy on group chips if shown.
- Search expands where useful; do not dominate the first viewport with an empty marketing search block.

## Out of scope

- Compose implementation until this draft is accepted
- Corrections write UI, voids, supervisor approval flows
- Reopen write UI
- Full customer/supplier ledger destinations
- Month Reports, Cash & close, Owner More
- Manager / Administrator / Cashier Records shells (Cashier keeps Activity)
- Mounting or translating foreign role screens
- Launcher migration away from `HybridActivity`
- Changing Overview/Work connected behavior except harness wiring needed for Records entry

## Verification

- Facade unit/instrumentation coverage for business-wide list scoping, empty/ready/detail, search filter, failure/retry, and Owner-only gate.
- Compose contract tests for Records home + detail tags; register inventory rows with real workflow test names (no `workflow_test=-` for connected controls).
- Harness: `qaScreen -Pqa.screen=ownerrecords` (add to `phase18/build.gradle.kts` screen map when implementing). Keep `owner` / `ownerwork` green.
- If list/detail content is shared across roles: Foundation tests prove Owner chrome ✓, foreign chrome ✗, Back → Owner Records list/home, session intact.
- Prove on A06 with `:app:assembleDebug` → uninstall `com.saaf.milkshop` / `.qa` / `.qa.tests` → install → launch `NativePreviewActivity` with `open_owner=true`.

## Accept gate

**Draft for approval.** Do not start Phase B Kotlin/Compose until the owner accepts (or amends) connected vs deferred scope, especially the deferral of corrections write and ledger groups. If leadership HTML in another checkout contradicts this conservative slice, reconcile before implementation.
