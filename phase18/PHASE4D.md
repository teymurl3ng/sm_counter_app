# Phase 4D — Sales UI

## Scope

This phase adds only the Sales UI vertical slice on top of the verified Phase 4C project.

## Flow

Sales screen → existing `SaleService` → atomic Room transaction → canonical stock/account views → dashboard refresh.

## Inputs

- Product (Milk / Yogurt)
- Quantity or Value (exactly one at a time)
- Rate (pre-filled from the relevant setting, editable)
- Channel (retail / wholesale / credit-retail / credit-wholesale; reporting label only)
- Payment: Cash / JazzCash / Bank or On Credit
- Customer selection for credit sales, with Add Customer for dynamic customer accounts
- Delivery surcharge (optional, allowed on any sale, default from settings, editable)
- Note (optional)

## Validation

- Product must be available.
- Rate must be greater than zero.
- Quantity or value must be entered according to the selected mode and must be greater than zero.
- Surcharge, when enabled, must be present and non-negative.
- A cash/account sale requires a Cash/Bank account.
- A credit sale requires a Customer account.
- Domain/service validation remains authoritative for transaction integrity.

## UI behavior

- Quantity mode calculates/display value preview.
- Value mode calculates/display quantity preview.
- Changing the product pre-fills the relevant configured rate.
- Changing channel does not alter pricing.
- The surcharge can be enabled regardless of channel/payment mode.
- After a successful save, the dashboard is re-read from canonical views.
- Invalid local validation is displayed synchronously without a polling wait.

## Tests

`Phase4dSaleUiTest` covers:

- quantity + cash sale with surcharge
- value + credit sale with an edited rate and surcharge
- channel selection without changing the edited rate
- invalid quantity handling without creating an event

`SpinnerWiringSmokeTest` is updated to seed a customer so the newly-added customer spinner is included in its existing structural wiring check.
