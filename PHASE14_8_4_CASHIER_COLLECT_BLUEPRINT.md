# Phase 14.8.4 — Cashier Collect blueprint

Status: **INTERACTIVE BLUEPRINT READY FOR OWNER REVIEW — NO PRODUCTION COLLECT CODE AUTHORIZED YET**

Date: 2026-09-20

## Where this checkpoint starts

The isolated native QA app already owns authentication and the Cashier Sale screen. Sale is lifecycle-hardened and its current focused test set is green. The next Cashier destination is `Collect`.

This checkpoint defines what Collect must do before Kotlin facade, Compose UI, or Room wiring begins. The interactive companion is [`design/refined-counter-ledger/collect.html`](design/refined-counter-ledger/collect.html).

## What the existing product already guarantees

- `CreditCollectionService` requires a positive amount, a Customer-category account, and a Cash/Bank-category account; the authenticated command boundary also rejects inactive or unavailable customers.
- One atomic transaction decreases the Customer balance and increases the selected Cash/Bank balance by the same amount.
- Owner, Administrator, Manager, and Cashier have the current `SALE_AND_COLLECTION` permission. Operations staff do not.
- The active business date cannot receive a posting after it is closed.
- The saved event retains the signed-in actor and contributes to the Cashier collection summary.
- An amount above what the Customer owes is valid. The resulting negative Customer balance represents an advance with the shop.
- The current Cashier read boundary does not allow the full financial overview.

The blueprint preserves those rules. It does not introduce refunds, receipt printing, integrated payments, editable business dates, or a Cashier correction power.

## Normal path

1. Open `Collect` from the Cashier navigation.
2. Choose the paying customer. Search uses the active customer's name and phone when available.
3. Show only the selected customer's usable balance context: owes, paid up, or has an advance.
4. Enter a PKR amount. `PKR 1,000` is a rapid common-value shortcut; `Pay full` fills the exact positive saved balance.
5. Confirm the visible Cash/Bank destination. A safe default remains visible and editable.
6. Optionally open `Add a note`.
7. Review both effects before saving: the resulting Customer balance and the equal increase to Cash/Bank.
8. Press `Record PKR …` once. The action becomes `Recording…` and rejects a second submission.
9. Replace the entry area with `Payment recorded`, amount, updated Customer balance, destination, and record number.
10. `Collect another` clears customer, amount, and note. It may retain the last non-secret Cash/Bank destination. `View record` opens the Cashier-scoped Activity detail.

## Deliberate advance path

The domain currently allows a payment above the amount owed. Silently posting it would be easy to do by mistake, while rejecting it would change existing product behavior.

When the entered amount would create or increase an advance:

1. the review block says the exact advance that will result;
2. the main action changes to `Review advance · PKR …`;
3. a confirmation shows amount owed, amount received, and resulting advance;
4. `Go back` preserves the draft;
5. `Record advance` performs the same atomic collection command;
6. success says `Now has PKR … advance`.

Normal payments remain one-save actions. Only the unusual advance case receives the extra confirmation.

## Control contract

| Control | Required workflow | Visible result | Native test obligation |
|---|---|---|---|
| Customer selector | Open searchable active-customer chooser | Sheet opens and search receives focus | Click opens picker; Back/dismiss closes it |
| Customer search | Filter by name or phone | Matching rows update in place | Query changes visible rows; no-match state is announced |
| Customer row | Select customer and discard stale amount from another customer | Name and one scoped balance replace the placeholder | Selection, balance label, and amount reset are asserted |
| Amount | Accept decimal PKR with at most two decimal places | Review amount and both effects update | Valid, empty, zero, malformed, and two-decimal cases |
| `PKR 1,000` | Replace amount with exactly 1,000 | Field and review update | Click changes semantic field value and review text |
| `Pay full` | Fill exact positive saved balance | Resulting Customer balance becomes paid in full | Enabled only for positive debt; exact-paisa assertion |
| Received-in selector | Open available Cash/Bank destinations | Account sheet opens | Click opens; selecting a row updates the visible destination |
| `Add a note` | Expand/collapse optional note | Text area appears without losing other fields | Expansion state and retained draft are asserted |
| `Record PKR …` | Start one normal collection | Button locks and says `Recording…` | One command despite repeat tap; exact input captured |
| `Review advance` | Open advance confirmation without posting | Exact owed, received, and advance values appear | No command before confirmation |
| `Go back` | Close confirmation | Original draft remains | Draft values unchanged |
| `Record advance` | Start one collection after confirmation | Saving then success | One command and exact advance result |
| `Collect another` | Start a safe new draft | Customer, amount, and note clear; destination remains | Reset/retention contract asserted |
| `View record` | Open the saved record in Cashier-scoped Activity | Matching record detail appears | Correct event ID and role scope asserted |
| `Sell` | Navigate to Cashier Sale | Sale screen appears | Navigation destination asserted |
| `Collect` | Keep or return to Collect | Collect screen appears | Selected navigation and draft policy asserted |
| `Activity` | Open own/current-day records | Cashier Activity appears | Role-scoped destination asserted when Activity is built |
| `More` | Open Cashier utilities | Cash count, personal security, switch account, sign out | Role-specific items and exclusions asserted when More is built |

The preview's `Activity`, `More`, and `View record` controls explain their future destination because those screens are later checkpoints. They are not production substitutes.

## Material states

| State | Behavior |
|---|---|
| Loading | Stable shell and progress semantics; no editable partial form |
| Ready without customer | Customer chooser first; record disabled |
| Ready with customer | Selected balance, amount, destination, and effects visible in one viewport |
| Invalid amount | Record disabled or field error; no command |
| Advance | Plain warning and explicit confirmation; no silent posting |
| Saving | Single in-flight command; controls locked |
| Success | Form replaced in the current viewport; updated authoritative balance and record number shown |
| Save failure | Draft preserved; inline error near the action; retry available |
| Closed date | Draft preserved; all posting actions disabled; Owner reopening guidance |
| No active customers | Calm setup blocker naming Owner or Administrator |
| No Cash/Bank account | Calm setup blocker naming Owner or Administrator |
| Customer archived after selection | Command rejected; stale selection removed after refresh; no event posted |
| Session expired | Preserve only non-secret draft in memory; sign in; restore only for the same account |
| Permission changed | Clear sensitive result and route to an allowed destination |

## Native data boundary required next

The existing `query.transactionReferences` gives the Cashier customer names and Cash/Bank names, but not customer balances. The existing `query.financialOverview` contains all customer balances and is correctly denied to Cashier. Native Collect must not solve that by granting the Cashier full financial overview.

The native implementation should add a scoped Collect facade:

- load active customer references and allowed Cash/Bank references for the authenticated Cashier;
- resolve one selected customer's current canonical balance after selection;
- expose the current business-date open/closed state;
- record through the existing authenticated collection command/service;
- return or re-query the authoritative updated selected-customer balance and destination effect for success;
- recheck permission, account activity/category, and day state at save time.

The screen may calculate a preview for speed, but success must use authoritative saved data.

## Verification already attached to the blueprint

`collect-model.test.mjs` exercises amount parsing, customer reset, Pay full, single-flight saving, exact balance update, advance confirmation, closed/invalid blocking, failure recovery, safe reset, and missing setup.

`collect-preview.contract.test.mjs` checks that every preview button has an interaction hook, all core controls have handlers, every material visual scenario is reachable, and changing results are announced through accessibility semantics.

The production checkpoint will add native unit/facade tests and Compose UI tests. It is not complete until those tests prove opening, state changes, visible text, disabled/enabled behavior, retry, duplicate-tap protection, navigation, and exact Room ledger effects.

## Review gate

Owner review should decide four visible product choices:

1. customer selector density and balance wording;
2. the `PKR 1,000` and `Pay full` shortcuts;
3. Cash/Bank selector prominence;
4. the explicit advance confirmation and success wording.

Approval authorizes native Collect implementation against this contract. It does not authorize Activity or More implementation beyond their navigation destinations.
