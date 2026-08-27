# Phase 6D — Employee Management

## Scope
Expose the existing Salary-account employee model as master data without changing historical salary transaction identity.

## Implemented
- Employee list with current salary-account balance.
- Add employee through `EmployeeManagementService`.
- Rename employee without changing `account_id`.
- Existing Salary Payment “Add Employee” path now uses the same management service.
- Salary Payment spinner continues to read the same Salary accounts and preserves the existing `Employee: ...` stored/display value used by prior transaction tests.

## Integrity
- Employee salary accounts remain `type = Expense`, `category = Salary`.
- No deletion is introduced because the existing schema has no safe historical reassignment/deletion rule.
- Historical salary events retain their existing account IDs.
- No SQL schema or salary transaction logic changes.

## Tests
`Phase6dEmployeeManagementUiTest` verifies:
- employee rename preserves account identity and refreshes the management UI;
- duplicate employee creation is rejected without creating another Salary account.

Android Studio / real-device execution remains the authoritative verification step.
