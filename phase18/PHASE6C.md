# Phase 6C — Customer Management

## Scope

Expose the existing Customer account model as master data without changing historical transaction identity.

## Implemented

- Customer list with current receivable balance.
- Add customer through `CustomerManagementService`.
- Rename customer without changing `account_id`.
- Existing Sales “Add Customer” path now uses the same service.
- Existing sales/credit-collection customer spinners continue to read the same Customer accounts.

## Integrity

- Customer accounts remain `type = Asset`, `category = Customer`.
- No deletion is introduced because the existing schema has no safe historical reassignment/deletion rule.
- Historical events retain their existing account IDs.
- No SQL schema or business transaction logic changes.

## Tests

`Phase6cCustomerManagementUiTest` verifies rename/account identity and duplicate-name rejection.
