# Phase 6B — Vendor Management

## Scope
Expose master-data management for the existing Vendor account model.

### Implemented
- Vendor list with current payable balance.
- Add vendor through `VendorManagementService`.
- Rename vendor through `VendorManagementService`.
- Vendor identity remains `account_id`; renaming changes only the current account name.
- Existing purchase/payment screens continue to use the same vendor accounts and spinners.
- Existing direct purchase-screen vendor creation now uses the same vendor management service.

## Architectural constraints
- No new vendor table.
- No deletion of vendor accounts, because historical financial logs reference account IDs and the core model treats vendor accounts as persistent business entities.
- No changes to event, inventory, financial-ledger, correction, or SQL semantics.
- Account type/category remain `Liability` / `Vendor`.

## Tests
`Phase6bVendorManagementUiTest` verifies:
- a vendor rename preserves the account identity and renders the renamed vendor in the management UI;
- duplicate vendor creation is rejected without creating another account.

Android Studio / real-device execution remains the authoritative verification step.
