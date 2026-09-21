# Phase 14.8.13 — Operations More blueprint

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION** (owner: continue without blueprint pause)

Date: 2026-09-20

## Where this checkpoint starts

Operations Work, Receive milk, Make yogurt, Record spoilage, Active deliveries, and My attendance are connected. Operations More is still a family preview notice.

This checkpoint owns **Operations More** only: today's own physical work list, personal Quick unlock, switch account, and sign out. It does not implement Count cash, Expense, leadership More, or launcher migration.

## What the existing product already guarantees

- Operator More is personal utilities + own operational records (Counter Ledger workflows).
- Cash count / close, sales, payments, expenses, financial accounts, corrections, settings, user admin, and backup stay outside Operator.
- Quick unlock enroll/remove already exists via `QuickUnlockService` (same path Cashier More uses).
- Leaving the authenticated area already invalidates the native session (`leaveAuthenticatedArea` / switch account).

## Normal path

1. Bottom nav → **More**.
2. Shared green header: More, identity, role, business date, Open/Closed.
3. Menu (no cash count):
   - **Today's work** — own physical records for the business date (receive / yogurt / spoilage)
   - **Personal security** — Quick unlock status and manage PIN / phone security
   - **Switch account** — leave session → account picker
   - **Sign out** — leave session → Welcome
4. Today's work lists physical task labels + quantities only (no rates, amounts, balances, or loss values).
5. Personal security matches Cashier More enroll/remove confirmation behavior.

## Out of scope

- Count cash / expected / variance
- Delivery status history or attendance rows in Today's work (stay on Deliveries / Attendance tabs)
- Expense, day close, Owner/Admin/Manager More
- Launcher migration

## Verification

Facade + Compose contract tests; `qaScreen -Pqa.screen=opsmore`; foundation open + switch/sign-out paths. Prove on A06 with assembleDebug → uninstall → install → launch.
