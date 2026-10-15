# PHASE 14.9 — CREDIT & REMINDERS BLUEPRINT

Status: PLANNED (not started). Owner: "MUST-DO — we will do it one day" (2026-10-02).

15 October reconciliation: the core Credit/reminders system remains planned. T4's sale wording subpart is complete in source `305a91d` and the latest signed S24 build: Credit / Choose customer / Required for a credit sale. T4 customer search remains unbuilt in the inspected Sale picker. The tasks below preserve the original plan; they are not thirteen wholly untouched tasks. See [current inventory](REMAINING_AND_LATER_2026-10-15.md). Android SMS/distribution and external integration assumptions must be revalidated when implementation is scoped.
Numbering: **14.9 deliberately** — root Phase 15 is RESERVED for uncoached Shopkeeper QA and
Phase 16 for Release Readiness in the frozen roadmap (POSTMORTEM.md, ROADMAP_RECONCILIATION);
`phase18/PHASE*.md` is the inherited-app history and must not be numbered into.

Restored 2026-10-08 from the preserved session record after the original blueprint write was
lost (permission denial). This document IS the surviving plan; treat it as authoritative.

## Goal

A customer payment-reliability system for credit customers: computed credit tiers, aging,
late/bad-credit views, PDF + image bills, WhatsApp/SMS reminders, scheduled automation.

## Locked owner decisions (2026-10-02)

1. **Aging = ledger FIFO.** Collections offset the oldest credit sale first. NO due-date
   fields — zero schema change.
2. **Categorization = auto tier (Good / Watch / Late / Bad), computed, never stored** + a
   manual Bad flag override. Manual flags must live in a NEW satellite table (not columns on
   `customer_profiles`) because the sync pull-side `applyProfileChild` path does not flow
   profile edits down.
3. **Credit limit = WARN on a credit sale past the limit, never block.** The limit already
   exists in `customer_profiles` and is advisory today.
4. **v1 automation = daily WorkManager detect + owner local notification + auto-SMS**
   (`SmsManager`, `SEND_SMS` permission, default OFF, carrier cost disclosed). WhatsApp is
   one-tap manual only (no app can silently send WhatsApp — official platform constraint).
   WhatsApp Business Cloud API deferred (Supabase edge function + Meta verification +
   per-conversation cost).

## What exists and is reusable (2026-10-02 audit)

- `customer_profiles` (phone nullable, credit_limit, daily_quota, active).
- Credit sale = SALE event with `OnCredit` → positive `financial_log` row on the customer
  account; balance = the computed `account_balances` view.
- `AccountLedgerService` per-customer ledger with running balance.
- WorkManager patterns to copy: `CloudBackupScheduler` (24h periodic), `SyncPushWorker`.
- `SyncNotifier` notification pattern; Owner More-menu registration points; `BusinessClock`
  trusted time.

## What is missing (to build)

- Aging logic (explicitly disclaimed today in `NativeOwnerOverviewFacade.kt`).
- Tiers/flags, any PDF or image generation (use platform `PdfDocument` + Canvas→Bitmap —
  no new dependencies), share intents / FileProvider (manifest has neither), product logos
  (products are text-only today).
- Sale customer picker is name-only without search — the collection screen's
  `CustomerPickerDialog` is the richer pattern to copy.
- Rename: sale UI target "Customer account" → "Credit" (`strings.xml` 459–465, 488 +
  contract tests).
- Amber is not in SaafTheme — list coloring needs it added.

## Task menu (T1–T13, in order)

T1 aging engine → T2 flags satellite table → T3 limit warning → T4 rename + picker →
T5 list coloring → T6 product-mix logos → T7 detail history → T8 Late/Bad More menus →
T9 bill renderer (PDF + PNG) → T10 share intents → T11 reminder job + reminder_log →
T12 auto-SMS rules → T13 manual Remind button.

Deferred (out of v1): WhatsApp Business API, JazzCash/EasyPaisa payment links, delivery
auto-pause at limit, bulk reminders.

## Market reference

Khatabook/OkCredit reminder-button pattern (pre-filled WhatsApp/SMS, schedulable, bulk =
paid tier); dairy platforms do pre-breach limit alerts + AR aging + delivery auto-pause.
