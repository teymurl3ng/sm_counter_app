# Phase 14.8.2 — Counter Ledger worker workflows

Status: **ACCEPTED AS AN IMPLEMENTATION BLUEPRINT — Compose foundation authorized and started**

Date: 2026-09-12

## Decision

The owner selected **Counter Ledger** from the three high-fidelity Cashier directions. This fixes the visual language for the remainder of the Phase 14.8.2 worker-role review. It does not yet approve the complete Cashier and Operations journeys or authorize production Compose work.

Counter Ledger is a warm, low-glare Android interface with strong transaction numerals, deep green identity, a restrained yellow completion action, and plain separators. The reference is an orderly physical counter ledger, expressed through hierarchy and rhythm rather than decorative paper, stamps, or bookkeeping imagery.

## Visual contract

- working background: warm ivory `#F4F1E8`;
- raised entry and result surfaces: `#FFFDF7`;
- primary identity, selected controls, and completion dock: deep green `#173F32`;
- primary commit action: yellow `#F2C85B` with dark text;
- text: near-black green `#17201B`, with muted labels around `#68726C`;
- type: Manrope with Roboto/system fallback; large tabular numerals receive the strongest emphasis;
- shapes: compact 10–16 dp field and control radii, with a larger outer phone/sheet radius only where the Android surface requires it;
- targets: at least 48 dp for primary actions and shared-phone identity controls;
- structure: one vertical work path, one dominant action, and four stable destinations per worker role;
- motion: a restrained 200 ms state settle and 240 ms success confirmation, disabled when Android Remove animations is enabled. These timings remain design judgment until device proof.

The system must remain recognizably Counter Ledger when it becomes denser for Owner and Manager screens. The palette, typography, separator rhythm, selected-state treatment, and decisive completion block are the reusable parts; a literal paper theme is prohibited.

## Cashier proposal

Cashier navigation is `Sell / Collect / Activity / More`. The default is `New sale`, with account identity and the open business date always visible.

| Screen | Normal path | Material states represented |
|---|---|---|
| New sale | Product → quantity or PKR value → payment destination → review dock → record | ready, credit sale, saving, success, insufficient stock, rate approval, missing setup |
| Collect payment | Customer → current balance → amount → receiving account → record | ready, saving, success, invalid amount, missing customer setup |
| My activity | Own records for the current business day | records, no records, access changed |
| Cash count | Physical drawer amount → blind submission | ready, submitting, submitted |

The Cashier sees only the customer context needed for the current collection, personal current-day activity, and a blind cash-count entry. Expected cash, variance, profit, costs, supplier finance, payroll, users, security audit, backup, and business settings remain outside this role.

## Operations proposal

Operations navigation is `Work / Deliveries / Attendance / More`. The default is a short physical-task home with relevant stock and today's status, without financial values.

| Screen | Normal path | Material states represented |
|---|---|---|
| Work home | Relevant stock/status → choose next physical task | ready, missing product setup |
| Receive milk | Supplier → liters → fat → LR → review dock → record | ready, saving, success, measurement error, missing supplier setup |
| Make yogurt | Milk used → yogurt produced → review conversion → record | ready, saving, success, insufficient milk, missing product setup |
| Record spoilage | Product → quantity → reason → review dock → record | ready, saving, success, unusual-loss approval, missing product setup |
| Active deliveries | Read delivery → mark delivered or open secondary action | active, delivered, cancel approval, no active deliveries |
| My attendance | Linked employee → mark present | not marked, present, account not linked |
| More | Own activity, Quick unlock, switch account, sign out | ready |

Operations results expose physical quantities, quality readings, stock effects, and work attribution. Supplier payable values and financial loss values must be removed at the service boundary, rather than merely hidden by the interface. Delivery wording remains `Active deliveries` because worker assignment is not implemented.

## Shared command behavior

- The final dock repeats the consequence and target before a write.
- Saving keeps the entered screen visible and disables the commit action.
- Success replaces the form in the current viewport and shows the record number plus the concrete stock, money, or attendance effect. The worker should never need to scroll upward to discover whether the action worked.
- A correctable error preserves all entered values and places the cause beside the affected action.
- A setup gap names the missing prerequisite and directs the worker to the Owner or Administrator.
- Restricted work opens one supervisor-approval sheet for that exact action. The saved audit must identify both worker and approver.
- Manager approval cannot bypass an owner-only reopen boundary.
- System Back on a dirty form asks before discarding; non-secret drafts may survive navigation and process recreation for the same signed-in account only.
- Offline local work remains available. Cloud state is never a blocker for daily entry.

## Deliberate scope boundaries

The proposal does not add a multi-item cart, barcode scanner, receipt printer, integrated card terminal, online checkout, or delivery assignment. Those capabilities do not exist in the verified product contract and must not appear as implied functionality.

## Review gate

The owner should now review whether Counter Ledger remains clear across the full Cashier and Operations set, especially:

1. repeated counter entry under time pressure;
2. physical measurement order for receipt and production;
3. immediate confidence after saving;
4. recovery without retyping after errors;
5. clear supervisor handoff without exposing restricted data;
6. stable, scrollbar-free worker navigation;
7. comfortable density at Android large-text settings.

Acceptance of this checkpoint authorizes design work for Owner, Store manager, and Administrator in the same visual system. Compose foundation work remains behind the complete five-role visual-proposal gate.

## Acceptance record

The owner accepted this worker-role proposal as a sufficient blueprint on 2026-09-13. Small adjustments may be made during native implementation and device review without reopening the visual direction, provided they preserve the role boundaries, field order, immediate result feedback, stable navigation, and Counter Ledger contract above.

The active design checkpoint moves to `PHASE14_8_2_COUNTER_LEDGER_LEADERSHIP.md` for Owner, Store manager, and Administrator.
