# Phase 8 — Budget & Targets: Finalized Specification

## Purpose
Introduce a planning layer above the frozen accounting core and Phase 7A read-only analytics. Phase 8 stores planning intent; it does not create accounting transactions or alternate accounting truth.

## Finalized business rules
1. A budget is the maximum planned spend per category and period.
2. Sales Target is a weekly target to reach or exceed. A sale counts immediately, including credit sales.
3. Collections Target is a separate weekly cash target; only CreditCollection events count as actual collections.
4. Budget categories: Milk purchases (Cost of Goods); Salaries, Rent, Electricity, Internet, Water, Fuel, Poly bags, LPG, Other (Operating Costs).
5. Milk Purchases stores only Planned Liters. Planned spend = Planned Liters × vendor milk rate snapshot. Weekly Sales Target = Planned Liters × retail milk rate snapshot.
6. Yogurt is not predicted/reallocated into the Sales Target at week start. Actual milk sales and actual yogurt sales remain separate, with a combined informational Milk + Yogurt actual.
7. Waste/spoilage does not reduce the Sales Target; it remains a visible shortfall.
8. Weekly Sales and Collections targets are fixed to Monday–Sunday periods. Monthly/quarterly/yearly views are roll-ups, not separately typed targets.
9. Operating-cost budgets use monthly Monday-independent calendar periods: first day of month through first day of next month.
10. No automatic budget repetition. Each period is explicitly created. Copy Previous Period is a convenience shortcut.
11. Mid-period revision becomes the current version immediately. The previous version remains visible in history.
12. A period closes automatically when its date range ends. Closed periods cannot be revised.
13. Old versions are archived/superseded, never deleted. Bringing back an old version means creating a new version from its values.
14. Business-rate changes do not mutate existing budget/target rows. Budget/target rows snapshot applicable rates at creation. A new rate is used by a new budget or deliberate revision.
15. Planned liters = 0 is a real explicit target; an absent budget/target is a separate “not set” state.
16. Actuals are obtained through Phase 7A AnalyticsService. Phase 8 does not query raw events.
17. Variance displays both amount and percentage. Percentage is “—” when target is zero.
18. If a weekly period crosses a calendar month, the whole week belongs to the calendar month containing the majority of its days; it is never prorated.
19. Closed-period transaction corrections preserve the original budget comparison and produce a separate recalculated comparison using the budget version that was active when the period closed.
20. Cash-flow/reserve planning is explicitly outside Phase 8 and is a future separate phase beginning with simple manual known obligations, not forecasting.

## Version/history model
Planning rows are append-only. A revision points to the superseded version; archive/hide is represented without physical deletion.

## Architectural boundary
```text
Frozen accounting core
        ↓
Phase 7A read-only analytics
        ↓
Phase 8 budget/target planning state
        ↓
Budget & Targets UI
```

Phase 8 must not rewrite events, financial logs, inventory logs, accounts, settings, canonical balances, or stock.

## Current implementation checkpoint
The first Phase 8 checkpoint implements budget/target authoring, rate snapshots, period-specific planning, revisions, history viewing, copy-previous convenience, Phase 7A-derived actuals, variance display, weekly sales/collections target reporting, and migration of planning tables. Closed-period comparison snapshots/recalculation presentation are documented rules and remain a follow-up within Phase 8 if not yet surfaced by the UI checkpoint.
