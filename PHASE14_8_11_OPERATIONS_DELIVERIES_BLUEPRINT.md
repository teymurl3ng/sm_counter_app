# Phase 14.8.11 — Operations Active deliveries blueprint

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION** (owner: continue without blueprint pause)

Date: 2026-09-20

## Where this checkpoint starts

Operations Work home, Receive milk, Make yogurt, and Record spoilage are connected. Deliveries is still a family preview notice.

This checkpoint owns **Active deliveries** only: pending list + mark delivered. It does not implement create delivery, cancel, archive, sale-link repair, Attendance, or Operations More.

## What the existing product already guarantees

- Operator has `DELIVERY`.
- `DeliveryService.transition` / bridge `transitionDelivery` move a pending delivery to `delivered` or `cancelled` on an open business date.
- Active list is not worker-assigned — label remains **Active deliveries**, not My deliveries.
- Cancellation / archive / create stay Manager/Owner workflows for this native slice.

## Normal path

1. Bottom nav → **Deliveries**.
2. Shared green header: Active deliveries, identity, role, business date, Open/Closed.
3. List pending deliveries grouped **Today** then **Earlier**, each card: customer, product + quantity + unit, route when present, status Pending.
4. Primary action **Delivered** locks while saving.
5. Success removes the card from the active list (or shows a brief confirmation then refreshes).
6. Empty state: **No active deliveries** — calm, no fake task.
7. Closed business date blocks mark-delivered with guidance.

## Out of scope

- Create delivery, cancel, archive, sale link
- Supervisor cancel approval
- Attendance, Operations More
- Launcher migration

## Verification

Facade + Compose contract tests; `qaScreen -Pqa.screen=deliveries`; foundation open + mark delivered. Prove on A06 with uninstall→install→launch.
