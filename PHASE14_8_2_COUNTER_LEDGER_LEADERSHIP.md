# Phase 14.8.2 — Counter Ledger leadership roles

Status: **ACCEPTED AS AN IMPLEMENTATION BLUEPRINT — minor native polish deferred**

Date: 2026-09-13

## Starting checkpoint

The owner accepted the Counter Ledger Cashier and Operations proposal as a sufficient implementation blueprint. Minor visual adjustments remain expected during native implementation and physical-device review, but they do not block the design sequence.

This checkpoint applies the accepted visual language to the three higher-authority roles. The shared palette, typography, separators, action treatment, saved-result pattern, and stable four-destination navigation remain consistent. Information density, default destination, and command priority change with each role's job.

## Owner — decisions first

Primary navigation: `Overview / Work / Records / More`.

The Owner opens to `Overview`, not an entry form. The first viewport answers how the business is doing and what requires a decision:

- today's sales and covered gross result;
- expected cash, customer debt, supplier debt, and relevant stock;
- exceptions such as a missing cash count, overdue balances, and backup health;
- latest activity and actor;
- direct drill-down to reports, records, close, and recovery.

The blueprint includes Overview attention/clear/coverage-warning states; month reports with complete, incomplete, and empty coverage; an all-command Work launcher; business-wide Records; Cash & close; Owner-only reopen review; and the complete business-control More menu.

Owner authority remains protected. Restore, reopen, recovery changes, and Owner/Administrator account changes require recent full-passphrase authentication and an audit entry. Quick PIN or fingerprint cannot authorize those actions.

## Store manager — run today

Primary navigation: `Work / Close / Records / More`.

The Store manager opens to `Today's work`. Frequent daily commands are compact, while exceptions and close readiness carry the strongest emphasis:

- sale, receipt, expense, and payment shortcuts;
- restricted actions waiting for one-action approval;
- late delivery and attendance gaps;
- cash count, expected cash, variance, and close blockers;
- assigned-shop records and correction entry.

The blueprint includes attention and all-clear Work states; approval waiting, approved, and empty states; ready, blocked, and closed Cash & close states; recent/empty Records; and the bounded shop-tools More menu.

Approval records both the worker and manager. Closing shows the whole-day consequence and states that reopening remains Owner-only. The manager menu excludes payroll, costing policy, business settings, users, security audit, backup, restore, and owner recovery.

## Administrator — keep the system healthy

Primary navigation: `Work / People / Records / More`.

The Administrator opens to a `Work desk` centered on setup and administration health, with daily entry retained as a secondary launcher:

- accounts waiting for private enrollment;
- products or rates blocking transactions;
- application-account and employee-link gaps;
- staff access, employee records, attendance, customers, and suppliers;
- business records, corrections, and security history;
- delegated settings, costing/planning, and encrypted local export.

The blueprint includes attention/all-clear Work desk states; People directory and setup-pending states; active/pending Staff access; business/security Records; and system-tools/local-export-success More states.

Staff account creation offers Store manager, Cashier, and Operations staff. Owner and Administrator changes remain Owner-only. The Administrator can create an encrypted local export but cannot connect the Owner's Drive account, inspect or restore backups, change recovery policy, or transfer ownership.

## Shared hierarchy rules

- Owner rows emphasize amount, trend, coverage, exception, and actor.
- Manager rows emphasize urgency, age, next action, approval, and close readiness.
- Administrator rows emphasize setup state, access state, blocked work, and data integrity.
- Common records and commands keep the same names and results across roles.
- Inaccessible destinations do not appear as disabled placeholders.
- Rare controls stay in grouped More menus rather than competing with daily work.
- Bottom navigation labels and order stay fixed for the signed-in role and never scroll horizontally.
- Success appears in the current viewport with the affected record, actor, and remaining work.
- Large text must reflow without reducing touch targets or creating horizontal navigation.

## Review gate

The owner should now decide whether these three role blueprints express the intended hierarchy:

1. Owner feels data-driven and remains fully capable without becoming a wall of reports.
2. Store manager can run the day and resolve exceptions without receiving ownership powers.
3. Administrator feels like delegated system control rather than a second manager or second owner.
4. All five roles still feel like one product despite different defaults and density.

Acceptance closes the Phase 14.8.2 visual-proposal gate. The next checkpoint is Phase 14.8.3: establish the Compose foundation and implement one reviewable native vertical slice while preserving the hybrid fallback.

## Acceptance record

The owner accepted the Owner, Store manager, and Administrator proposal as solid on 2026-09-13. Minor visual adjustments may be made during native implementation and device review if they preserve the role hierarchy, authority boundaries, default priorities, and Counter Ledger visual contract.

The remaining Phase 14.8.2 access blueprint is recorded in `PHASE14_8_2_COUNTER_LEDGER_ACCESS.md` and is also accepted.
