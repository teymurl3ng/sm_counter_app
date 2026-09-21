# Phase 14.8.15 — Owner Work home blueprint

> **WARNING (2026-09-21):** The **Connected in this checkpoint** lines that say host `CashierSaleScreen` / `CashierCollectionScreen` / `ReceiveMilkScreen` / `MakeYogurtScreen` / `RecordSpoilageScreen` / `DeliveriesScreen` as full foreign screens are **SUPERSEDED** by `PHASE14_8_15A_SHELL_VS_CONTENT.md` and the CORE METHOD in `.cursor/rules/native-shell-vs-content.mdc`. Do **not** follow those host-full-screen lines for new work. Use role-neutral content + Owner shell + Foundation proof. Keep the rest of this document as historical Work-home planning.

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION** (owner: continue without blueprint pause)

Date: 2026-09-21

## Where this checkpoint starts

Owner Overview is connected (`Overview / Work / Records / More`). Work, Records, and More still show explicit preview notices.

Authority: `PHASE14_8_2_COUNTER_LEDGER_LEADERSHIP.md`, `design/stitch/reference-blueprints/counter-ledger-leadership.html` (`ownerWork`), and the cross-role consistency rule (do not flatten Owner Work into Ops Work or Cashier Sell).

This checkpoint owns **Owner Work home** only: the all-command launcher. It connects existing native entry screens where they already exist, and keeps explicit next-family notices for commands that are not yet Owner-hosted.

## What the existing product already guarantees

- Owner has full daily-command permissions (sale, collection, purchase, production/spoilage, delivery, expense, supplier/salary payment, cash count/close).
- Native Compose entry already exists for Sale, Collect, Receive milk, Make yogurt, Record spoilage, and Deliveries (Cashier/Ops shells).
- Expense, supplier payment, salary payment, and full Cash & close UI are not yet native-owned.

## Reuse / do-not-reuse

| Piece | Reuse? |
|---|---|
| Owner bottom nav + green header tokens | Yes |
| Ops Work physical context strip | No — Owner Work is command-first, not physical-only |
| Cashier Sell as default home | No |
| Existing Sale / Collect / Receive / Yogurt / Spoilage / Deliveries screens | Yes — host inside Owner Work destinations |
| Leadership Work action groups | Yes — primary actions + Make/move stock + Money and close |

## Normal path

1. Owner opens **Work** from Owner navigation.
2. Header: **Work**, shop/identity line, business date, Open/Closed.
3. Primary actions (grid): Record sale, Receive milk, Record payment, Record expense.
4. Section **Make and move stock**: Make yogurt, Record spoilage, Deliveries (badge = active pending count when known).
5. Section **Money and close**: Pay supplier, Pay salary, Cash count and close.
6. Connected actions open the existing native entry screen inside the Owner shell and return to Work home on Back.
7. Not-yet-connected actions show an explicit next-family preview notice (not a silent no-op).

## Connected in this checkpoint

- Record sale → Owner-hosted sale content / sale facade (CORE METHOD)
- Record payment → Owner-hosted collection content / collection facade (CORE METHOD)
- Receive milk → Owner-hosted receive content (CORE METHOD)
- Make yogurt → Owner-hosted yogurt content (CORE METHOD)
- Record spoilage → Owner-hosted spoilage content (CORE METHOD)
- Deliveries → Owner-hosted deliveries content (CORE METHOD)

## Connected later (Queue B)

- Record expense → Owner-hosted expense content / `NativeExpenseFacade` (CORE METHOD) — see `PHASE14_8_27_OWNER_EXPENSE_BLUEPRINT.md`
- Pay supplier → Owner-hosted vendor payment content / `NativeVendorPaymentFacade` (CORE METHOD) — see `PHASE14_8_28_OWNER_PAY_SUPPLIER_BLUEPRINT.md`
- Pay salary → Owner-hosted salary payment content / `NativeSalaryPaymentFacade` (CORE METHOD) — see `PHASE14_8_29_OWNER_PAY_SALARY_BLUEPRINT.md`

## Preview notices in this checkpoint

- Cash count and close

## Control contract

| Control | Required workflow | Visible result | Native test obligation |
|---|---|---|---|
| Work nav | Open Owner Work home | Launcher groups visible | Destination asserted |
| Record sale / payment / receive / yogurt / spoilage / deliveries / expense / pay supplier / pay salary | Open connected entry | Owner-hosted entry | Tags + Foundation chrome/Back |
| Cash close | Preview notice | Explicit not-connected copy | Tags + copy |
| Hardware Back on Work home | Leave authenticated area | Welcome/Access like Overview | Back contract |
| Hardware Back inside connected entry | Return to Work home | Work launcher | Back contract |

## Native data boundary

`NativeOwnerWorkFacade`:

- require authenticated Owner session;
- project business date, posting open/closed, active pending delivery count for the Deliveries badge;
- do not invent close blockers or approval queues here (Manager Work territory).

## Out of scope

- Owner Records, More, Reports, Cash & close write UI, reopen
- New Expense / supplier / salary native forms
- Manager / Admin shells
- Launcher migration away from `HybridActivity`

## Verification

Facade + Compose contract tests; `qaScreen -Pqa.screen=ownerwork` (or extend `owner`). Prove on A06 with assembleDebug → uninstall → install → `open_owner=true`. Keep Overview and worker contracts green.
