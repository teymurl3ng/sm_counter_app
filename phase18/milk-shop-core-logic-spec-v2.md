# Milk Shop — Core Business Logic Spec

This is the source of truth for what the numbers must do. UI, database, and screens are all built *on top of* this — none of them should ever touch a balance directly. If a bug shows up, check here first: is this document wrong, or is the code not following it?

---

## 1. Products & Inventory

- **Milk** — tracked in liters
- **Yogurt** — tracked in kg, made from milk (see §6)

## 2. Money Accounts

- Cash in hand
- JazzCash
- Bank

Every transaction that moves real money specifies exactly one of these — or none, if it's a credit sale/purchase with no immediate cash movement.

## 3. Settings (manually edited, never automated)

| Setting | Value | Notes |
|---|---|---|
| Milk rate (₨/liter) | ~170 | seasonal, changes slowly |
| Delivery fee (₨/liter) | ~11 | tracks fuel prices |
| TS standard | 13.00% | fixed, government-set |
| Walk-in retail rate — milk | *(set yours)* | rarely changes |
| Walk-in retail rate — yogurt | 280 | rarely changes |
| Default far-delivery surcharge | 50 | per delivery, optional toggle |

---

## 4. Vendor Purchase (Milk In)

Recorded every delivery. Inputs: **Raw Liters** (measured at your shop), **Fat%**, **LR**.

```
SNF%  = (0.25 × LR) + (0.22 × Fat%) + 0.72
TS%   = Fat% + SNF%
Adjusted Liters = Raw Liters × (TS% ÷ 13.00%)
Amount Owed = (Adjusted Liters × Milk Rate) + (Raw Liters × Delivery Fee)
```

**Effects:**
- Milk stock `+= Raw Liters` — what you actually received; this is your sellable stock
- Vendor payable balance `+= Amount Owed` — paid off weekly, not per delivery

## 5. Vendor Payment

Inputs: Amount, Account, Date.
**Event field:** `amount` stores the amount being paid.
**Effect:** Vendor payable balance `-= Amount`; Account balance `-= Amount`.

## 6. Yogurt Production

Recorded per batch. Inputs: **Milk Liters Used**, **Yogurt Kg Produced** — both weighed/measured at the time, never calculated from a fixed ratio.

**Effect:**
- Milk stock `-= Milk Liters Used`
- Yogurt stock `+= Yogurt Kg Produced`

*Reference only (not used in any formula): a typical batch runs ~85% yield, e.g. 17L → 14.5kg. Useful as a sanity check when logging a new batch, nothing more.*

## 7. Sales — one mechanism, every channel

Applies identically to retail, wholesale, credit-retail, and credit-wholesale. "Channel" is a label for reporting only — it never drives a pricing rule.

Inputs: Product, **Quantity or Value** (enter one, compute the other), **Rate** (pre-fills from the relevant setting in §3, always editable), Payment Method (Cash/JazzCash/Bank) **or** "On Credit," Delivery Surcharge (optional toggle, defaults ₨50, editable; may be applied to any sale, regardless of channel or payment method. Current practice is to use it primarily for home/far delivery), Note (optional, for any manual override).

```
If Quantity entered: Value = Quantity × Rate
If Value entered:    Quantity = Value ÷ Rate
Total = Value + (Surcharge, if toggled on)
```

**Effect:**
- Product stock `-= Quantity`
- If a Payment Method was chosen: that Account `+= Total`
- If "On Credit": that Customer's receivable balance `+= Total`

## 8. Credit Payment Received

Inputs: Customer, Amount, Account, Date.
**Event field:** `amount` stores the amount received.
**Effect:** Customer's receivable balance `-= Amount`; Account balance `+= Amount`.

## 9. Expenses

Inputs: Category (Rent / Electricity / Internet / Water / Fuel / Poly bags / LPG / Other), Amount, Account, Date.
**Event field:** `amount` stores the expense amount.
**Effect:** Account balance `-= Amount`.
Fixed, monthly: rent, electricity, internet, water. Variable, as-needed: fuel, poly bags, LPG, misc.

## 10. Salaries

Inputs: Employee, Amount, Account, Date — paid weekly.
**Event field:** `amount` stores the salary amount.
**Effect:** Account balance `-= Amount`.

---

## 11. Corrections

Any event can be corrected by adding a new event that references the original (`corrects_event_id`). The original event is never edited or deleted — the correction sits alongside it.

- **The correction event's own fields hold the corrected, true value** — amount owed, monetary `amount`, quantity, value, etc., as if you were entering the event fresh and getting it right the first time. Not a delta.
- **The ledger movement attached to the correction (stock change, account change) is the *difference*** between the corrected truth and what was originally logged. That's what actually needs to move to bring stock/balances from the old, wrong state to the new, right one.

**Example:** A purchase was logged as ₨18,374.62 owed, but the Fat% reading was wrong — it should have been 4.0, not 4.5. Recalculated correctly, the true amount owed is ₨17,576.92.

- Correction event's amount owed = **₨17,576.92** (the restated truth)
- Correction's effect on the vendor's payable balance = **-₨797.69** (the delta that brings the balance down from the old figure to the true one)

Same pattern for a sale or a production batch: the correction's own quantity/value fields describe what actually happened, and the stock movement it creates is just enough to reconcile the original entry to that truth.

## 12. Assumptions worth a second look

These were reasonable defaults chosen while building this doc — cheap to confirm now, expensive to discover wrong later:

- **"Raw Liters" in §4** = what you measure at your own shop on arrival, not the vendor's claimed figure. This is also the number that drives your sellable milk stock.
- The **₨11 delivery fee** applies per raw liter delivered, flat, on top of the TS-adjusted milk cost.
- **TS standard (13.00%)** is fixed and isn't expected to change inside the app.

---

## 13. Dev Notes — Deferred, Not Forgotten

Found during a schema review (Aug 2026) and intentionally left open rather than guessed at. A future review — AI or human — should start here instead of re-discovering these from scratch.

1. **Surcharge vs. channel mismatch — resolved.** The Delivery Surcharge is intentionally allowed on any sale, regardless of channel or payment method. It is currently used primarily for home/far delivery, but channel must remain a reporting label and must not control whether a surcharge is permitted.

2. **No DB-level check that a ledger row matches its event header.** Nothing currently confirms `inventory_log` quantities actually equal what the event's `raw_liters`/`sale_quantity` says, or that `financial_log` amounts sum to `amount_owed` / `sale_value + surcharge`. Deliberately left out of the schema as a trigger — currency values need a rounding tolerance, not exact equality, and it's a better fit for an automated test against `purchase_calculation_audit` / `sale_calculation_audit` than a hand-rolled trigger. Worth writing that test early in app development, per the closing note below.

3. **No seed rows for Vendor / Customer / Salary accounts.** Assumed intentional — these are business-specific (a named vendor, a named customer, a named employee), not something to hardcode. Consequence worth knowing going in: the very first `PURCHASE_RECEIPT`, `CREDIT_COLLECTION`, or `SALARY_PAYMENT` will fail until the app creates at least one account in that category. Confirm this matches the intended onboarding flow.

4. **`PRAGMA foreign_keys = ON` is per-connection in SQLite.** It's set at the top of the schema file, but SQLite doesn't persist it — every new connection the app opens has to re-issue it, or foreign key constraints silently stop being enforced. Worth a one-line check in whatever DB-connection helper the app ends up using.

---

## What this document is for

Any AI (Claude, ChatGPT, Gemini, whichever) building or debugging your app should be pointed at this file first. If a proposed fix contradicts something here, the fix is wrong — not this document. Once code exists, the next step is turning §4–§10 into automated tests: real numbers in, one correct answer out, so nothing can quietly drift again.
