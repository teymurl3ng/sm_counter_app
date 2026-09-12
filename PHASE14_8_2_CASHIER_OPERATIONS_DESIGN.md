# Phase 14.8.2 — Cashier and Operations staff daily-work design

Status: **PROPOSED FOR OWNER REVIEW — complete interaction direction, no Compose implementation yet**

Date: 2026-09-12

## What this checkpoint decides

This checkpoint turns the accepted role policy into the first two complete native work experiences. It starts with Cashier and Operations staff because these accounts perform the most repeated entries and are expected to need the least training.

The design is based on the real Saaf commands and saved results. It does not add receipt printing, delivery assignment, online sync, or any other capability that the current product does not have. It does identify a small number of authorization and read-model changes that must precede native ownership of these screens.

No production Compose code is authorized by this document. The next checkpoint is owner review of the interactive flows, including the normal, success, error, empty, and supervisor-approval states.

## Shared daily-work rules

### One job per screen

A work screen begins with the fields needed to finish its named job. It does not place dashboard cards, reports, or a second task above the form. Less common fields sit behind a clearly labelled `More details` row.

### The normal path is short and explicit

- The current open business date is visible but not editable for Cashier or Operations staff.
- Product, supplier, customer, or loss reason is always visibly selected before save.
- Saved rates and calculations appear as read-only context. These roles do not retype policy values during normal work.
- A normal cash sale defaults to the single normal cash account when exactly one exists. Otherwise the last non-secret payment account is selected and remains visible.
- Customer selection never carries into a new credit sale.
- One primary save action stays above the keyboard and includes the amount or quantity when that prevents a mistake.
- Save is a single in-flight operation. The action changes to `Saving…` and cannot be pressed twice.

### Every save answers “did it work?” immediately

Success replaces the entry area in the current viewport. It shows the record number, the essential quantities or money destination, and the stock or balance effect. It then offers the next likely action. The worker never has to scroll upward to discover the result.

### Plain language

Worker screens use task words: `Sell`, `Collect payment`, `Receive milk`, `Make yogurt`, `Record spoilage`, and `Delivered`. Accounting terms such as debit, credit, journal, ledger, and contra stay out of the daily entry path.

### Touch, type, motion, and feedback

- All important targets are at least 48 dp high and separated enough for one-handed use.
- Editable text is at least 16 sp. Totals and final quantities are visually stronger than labels.
- Numeric fields request the Android decimal keypad and move to the next field from the keyboard.
- Navigation and result changes use a short 180–220 ms shared-axis or fade-through transition. Reduced-motion settings remove translation.
- A successful save gives a short haptic confirmation. Errors use a different haptic pattern and never depend on color alone.
- Large text reflows vertically. There is no horizontal scrolling in app navigation, forms, results, or error text.

## Cashier experience

### Shell and navigation

The Cashier opens directly on `Sell`. The stable bottom navigation is:

1. **Sell** — new sale and post-sale delivery handoff.
2. **Collect** — customer payment collection.
3. **Activity** — this Cashier's records for the current business day.
4. **More** — cash count, personal security, account switch, and sign out.

The top app bar shows the current account and a compact `Today · Open` business-day state. Account switch is visible on a shared phone. A closed-day state disables transaction actions and says `This business date is closed. Ask the Owner to reopen it.`

### Sell — normal path

Field and decision order follows the conversation at the counter:

1. **Product** — large Milk and Yogurt choices. A choice is required and stays unmistakably selected.
2. **Enter by** — `Quantity` or `PKR`, with the last non-secret mode retained on this device.
3. **Quantity or amount** — the only large numeric field on first view.
4. **Payment** — `Paid now` or `On customer account`.
5. **Received in** — visible cash/bank choice for paid sales. It defaults only under the safe rule above.
6. **Customer** — required for an account sale; search and recent customers appear before the full list.
7. **Total preview** — quantity, saved unit rate, sale value, optional delivery charge, and final total.
8. **Primary action** — for example, `Record sale · PKR 600`.

The configured product rate is displayed but locked for a Cashier. Wholesale, delivery charge, and notes sit under `More sale details`. Wholesale appears only when that permission is granted. A rate exception opens supervisor approval; it never silently grants the Cashier a rate-edit permission.

After `Record another`, the product, entry mode, and non-secret payment account may remain selected and clearly visible. Quantity/value, customer, delivery charge, and notes reset. This preserves speed without carrying customer debt or a one-off charge into the next sale.

### Sell — success

The success surface shows:

- `Sale recorded` and the native record number;
- product and quantity;
- final total;
- either `Received in Cash` or `Added to [customer]'s balance`;
- the inventory reduction in plain language;
- `Record another` as the primary action;
- `View record` as a secondary action;
- `Add delivery` only when the sale is eligible and the Cashier has the grant.

There is no print button because Saaf has no printing capability.

### Collect payment

The first control searches or chooses a customer. Selection shows only the customer's usable balance context. The Cashier then enters amount, confirms the visible payment account, and presses `Record PKR …`. Success shows the customer's updated balance and where the money was received.

The form does not expose supplier payments, salary payments, account ledgers, or other customers' history.

### Activity

Activity opens at `Today` and is limited to records attributed to the signed-in Cashier on the current business date. Each row shows time, task label, record number, amount, and paid/account state. The Cashier can inspect a record they created but cannot correct or void it directly. A restricted correction begins a one-action supervisor approval flow.

Current data already has actor attribution and timestamps, so this scope does not depend on inventing a shift entity.

### Blind cash count

`More > Submit cash count` asks for the physical drawer total and an optional note. It does not show expected cash, variance, bank, or wallet balances. After save it says `Cash count submitted to the manager` and shows the counted amount and record number. It does not reveal variance, preventing repeated guesses against the expected result.

### Cashier empty and blocked states

- No product: `Sales are not ready yet. Ask the Owner or Administrator to add a product and selling rate.`
- No usable payment account: `There is no account to receive this payment. Ask the Owner or Administrator to finish setup.`
- Credit sale with no customer: the customer field receives focus and explains what is missing.
- Insufficient stock: show available stock, keep the draft, and offer `Change quantity`; a supervisor path appears only if policy permits an override.
- Closed date: preserve the draft, disable save, and direct the worker to the Owner.
- Session expired: preserve non-secret draft fields in memory, require sign-in, then restore the draft only to the same account.
- Permission changed while open: show that access changed, retain no sensitive result, and return to an allowed destination.

## Operations staff experience

### Shell and navigation

The Operations account opens on `Work`. The stable bottom navigation is:

1. **Work** — Receive milk, Make yogurt, and Record spoilage.
2. **Deliveries** — active delivery records and status updates.
3. **Attendance** — the signed-in worker's own status.
4. **More** — own activity, personal security, account switch, and sign out.

The Work screen is a task launcher rather than a business dashboard. It shows physical context that helps choose the next job: available milk, today's latest receipt/production time, active delivery count, and whether the worker has marked attendance. It shows no money, margins, debt, payroll, or owner alerts.

### Receive milk

The form follows the physical measurement sequence:

1. supplier;
2. raw liters;
3. fat percentage;
4. LR reading;
5. optional note under `More details`;
6. `Record receipt`.

Recent suppliers can appear as large choices, but a supplier must be explicitly confirmed for every receipt. The configured rate and amount owed are not delivered to this role's UI. Success shows the record number, supplier, raw liters, adjusted liters, SNF, total solids, and the stock increase. Financial payable results are reserved for Manager, Administrator, and Owner views.

### Make yogurt

The screen starts with available milk. The worker enters milk used and yogurt produced, with notes secondary. The primary action includes the physical conversion, for example `Record 20 L → 15 kg`. Success shows both stock movements and the record number. If milk is insufficient, the draft remains and the error shows the available quantity beside `Change amount`.

### Record spoilage

The worker selects product, quantity, and a concrete reason: spoiled/sour/curdled, spill, contamination, power failure, equipment failure, or other. Current stock is visible beside the quantity. `Other` requires a note.

An amount above the configured unusual-loss threshold opens supervisor approval before any mutation. The worker sees physical quantity only; the financial loss assessment remains on Manager and Owner surfaces. Success shows the quantity removed and remaining stock.

### Deliveries

The list contains active delivery records already in Saaf, grouped by today and older pending work. Each card shows customer, product, quantity, route when present, and status. `Delivered` is the dominant action. Cancellation, archive, sale-link repair, and record correction live behind `More` and require a Manager or Owner as policy dictates.

Saaf does not currently assign a delivery to a worker, so the first native version must say `Active deliveries`, not `My deliveries`. Worker assignment can be designed only after the data model supports it.

### My attendance

Attendance shows the linked employee name and today's state. An unrecorded day has one primary action: `Mark present`. Half-day, absent, edits, and other employees belong to Manager/Administrator/Owner workflows. If the account is not linked to an employee, the empty state says `Ask the Owner or Administrator to link this account to an employee.`

### Operations empty and blocked states

- No supplier: explain that receiving needs a supplier and direct setup to Owner/Administrator.
- No tracked product: disable production/spoilage and direct setup to Owner/Administrator.
- No active deliveries: a calm empty state, `No active deliveries`, with no fake task.
- No employee link: keep Work available and explain why self-attendance is unavailable.
- Insufficient stock, closed date, expired session, saving, and permission-change behavior follow the shared rules.

## Supervisor approval

Approval is a separate security action, not a hidden role upgrade:

1. the worker starts a restricted action and sees why approval is needed;
2. they select an eligible Manager or Owner account;
3. the supervisor enters that account's Quick PIN or full passphrase on the shared phone;
4. Saaf approves exactly the displayed action and returns to the preserved draft;
5. the final audit records both the worker who initiated it and the supervisor who approved it.

Failure, cancel, timeout, account switch, or app backgrounding performs no mutation. Owner-only actions, including reopening a business date, cannot be approved by a Manager.

## Back, interruption, and restoration

- Back from an untouched form returns immediately.
- Back from a meaningful draft opens `Discard this entry?` with `Keep editing` as the safe action.
- Opening account switch with a draft explains that switching will discard it.
- Rotation, resizing, split screen, and keyboard dismissal retain the draft and selected details.
- Process restoration keeps non-secret identifiers and entered values, but never passphrases, Quick PINs, approval credentials, or decrypted backup keys.
- A successful save clears the draft before showing success so Back cannot resubmit it.

## Material state contract for every entry screen

| State | Required behavior |
|---|---|
| Loading | Show field-shaped placeholders; keep the main action disabled; do not flash incorrect defaults. |
| Ready | Put focus on the first missing decision; keep computed context visible near the main action. |
| Field error | Explain the problem beside the field, announce it, and focus the first invalid field. |
| Saving | Keep entered values visible; change the action label to `Saving…`; block duplicate submission. |
| Success | Replace the form in the current viewport; show record number and business effect; offer the next task. |
| Domain conflict | Preserve the draft and show actionable facts such as available stock or closed date. |
| Permission denied | Perform no partial mutation; explain that access changed or approval is required. |
| Session expired | Preserve only non-secret draft data and restore it only after the same account signs in. |
| Empty setup | Name the missing setup item and the role that can fix it. |
| Offline | Continue local daily work normally; cloud state does not block Room-backed entry. |

## Required application-boundary changes before implementation

These are security and scope changes, not presentation polish:

1. split sale/collection, production/spoilage, delivery transitions, attendance, cash-count submission, and master-data capabilities as defined by the role architecture;
2. add record scopes for own actor/current business date and self-linked employee attendance;
3. add role-filtered read/result models so Operations staff never receives supplier payable values and Cashiers never receive expected cash or variance;
4. add the one-action supervisor-approval capability and dual-actor audit record;
5. add an unusual-spoilage policy threshold and approval requirement without exposing its financial loss value to Operations staff;
6. expose typed, presentation-neutral Kotlin facades for the accepted daily-work reads and commands;
7. keep the current bridge path as behavioral fallback until each native screen passes parity.

Hiding sensitive fields in Compose is insufficient. The native service boundary must refuse unauthorized commands and omit restricted values from the returned data.

## Acceptance questions for the visual review

The owner should judge the proposal by concrete work, not styling preference alone:

- Can a first-time Cashier understand how to record a normal paid sale without instruction?
- Is the selected product, payment method, customer, and final total unmistakable before save?
- Does success appear immediately and make the saved effect clear?
- Can an Operations worker record receipt, production, spoilage, delivery, and own attendance without seeing financial administration?
- Are supervisor-only exceptions visible at the moment they are needed, without cluttering every normal task?
- Are errors recoverable without retyping the entry?
- Is every screen usable at large text with one hand and no horizontal scroll?

## Next checkpoint after acceptance

After the Cashier and Operations direction is accepted, create the corresponding Owner, Store manager, and Administrator journeys using the same field, result, error, and approval components but different priorities and access. Only after all five role experiences are coherent does Phase 14.8.2 close and Phase 14.8.3 begin the Compose foundation.
