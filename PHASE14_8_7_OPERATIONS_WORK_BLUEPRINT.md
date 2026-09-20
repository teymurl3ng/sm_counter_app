# Phase 14.8.7 — Operations Work home blueprint

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION** (owner: continue without blueprint pause)

Date: 2026-09-20

## Where this checkpoint starts

Cashier Sell / Collect / Activity / More are connected in the debug `NativePreviewActivity` harness. The accepted Counter Ledger worker proposal’s next retained family is Operations (`Work / Deliveries / Attendance / More`).

This checkpoint owns **Work home** only: physical context + task launcher. It does not implement Receive milk, Make yogurt, Record spoilage entry forms, Deliveries list, Attendance mark-present, or Operations More.

## What the existing product already guarantees

- Operator role has `VIEW_OPERATIONAL`, `PURCHASE`, `PRODUCTION_AND_SPOILAGE`, `DELIVERY`, `ATTENDANCE`.
- Milk/yogurt stock, purchase receipts, production events, pending deliveries, and attendance records already exist in Room.
- App users may optionally link to an employee (`Salary`) account for self-attendance.
- Financial payables, rates, margins, and loss values must not reach Operator UI.

## Normal path

1. An Operator signs in and opens the Operations shell on **Work**.
2. Shared green header shows Work, identity, role, business date, Open/Closed.
3. Physical context (no money): milk on hand, latest receipt time today, latest production time today, active (pending) delivery count, own attendance status or unlinked guidance.
4. Task launcher: Receive milk, Make yogurt, Record spoilage — each opens an explicit “next in family” preview notice until those screens are owned.
5. Bottom nav: Work (connected) / Deliveries / Attendance / More — non-Work destinations show explicit preview notices.

## Control contract

| Control | Required workflow | Visible result | Native test obligation |
|---|---|---|---|
| Work nav | Open Work home | Context + tasks; no money | Destination asserted |
| Context strip | Load physical summary | Milk qty, times, delivery count, attendance | Facade strips finance |
| Receive / Yogurt / Spoilage | Open task preview | Explicit next-family notice | Tags + copy |
| Deliveries / Attendance / More nav | Preview destinations | Explicit not-connected notices | Nav contracts |
| Hardware Back on Work | Leave authenticated area / Welcome path matching Cashier Sell | Session ends like Cashier Sell | Back contract |

## Native data boundary

`NativeOperationsWorkFacade`:

- require authenticated session + `VIEW_OPERATIONAL`;
- project milk on hand, today’s latest PURCHASE_RECEIPT / PRODUCTION createdAt labels, pending delivery count, attendance link/status;
- never return rates, payables, variance, or financial loss.

## Out of scope

- Receive milk / Make yogurt / Record spoilage write forms
- Deliveries transitions, Attendance mark-present, Operations More
- Launcher migration; Owner/Manager shells

## Verification

Facade + Compose contract tests; harness `qaScreen -Pqa.screen=work`; prove on A06. Keep Cashier `qaCheckpoint` green.
