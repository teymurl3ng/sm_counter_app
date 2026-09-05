# Phase 14.2 — Role, Permission, Session, and Bypass QA

Status: **COMPLETE — 2026-09-06**

## Outcome

The packaged hybrid application and native bridge enforce the frozen five-role permission contract on the isolated Samsung SM-A065F / Android 16. No production defect was found, so this checkpoint changes tests and documentation only; it does not change product behavior, calculations, business rules, or UI design.

Two focused physical-device tests were added in `Phase14RolePermissionMatrixTest.kt`, bringing the complete device-suite inventory from 258 to 260 tests:

1. a native matrix that creates real owner, admin, manager, cashier, and operator sessions, checks each returned effective-permission set, and attempts a valid direct bridge request for every contract permission the role does not hold;
2. a packaged-UI matrix that signs in as all five roles and checks the exact visible application areas, daily tasks, account-administration controls, assignable roles, and backup/restore controls.

UI hiding is only presentation evidence. Every denied capability is independently checked at the native boundary.

## Verified packaged-UI matrix

| Role | Visible application areas | Daily tasks | Sensitive controls |
|---|---|---|---|
| Owner | Dashboard, Daily work, Deliveries, Accounts, History, Cash close, Products, Customers, Suppliers, Employees, Cost & plans, Settings, Access, Backup | Receive milk, Record sale, Make yogurt, Spoilage, Expense, Payments, Pay salary | Staff accounts, all five assignable roles, security history, manual backup, cloud backup, file restore |
| Admin | Same application areas as Owner | Same seven daily tasks | Staff accounts, ordinary assignable roles only, security history, manual backup only; no cloud or restore controls |
| Manager | Dashboard, Daily work, Deliveries, Accounts, History, Cash close, Products, Customers, Suppliers, Employees, Settings, Access | Same seven daily tasks | Own account only; no staff/security history, Cost & plans, or Backup |
| Cashier | Dashboard, Daily work, Deliveries, History, Cash close, Access | Record sale, Expense, Payments | Own account only |
| Operator | Dashboard, Daily work, Deliveries, History, Employees, Access | Receive milk, Make yogurt, Spoilage | Own account only |

## Verified native boundaries

- All 19 permission codes exposed by bridge contract v4 have a representative direct-call probe tied back to the generated contract snapshot.
- All five roles can use the two intentionally shared permissions: operational viewing and deliveries.
- Every role/permission combination not present in `RolePermissions` returns `permission_denied` at the native bridge before business work proceeds.
- The owner-only `MANAGE_PRIVILEGED_USERS` boundary is separately verified by an Admin attempting to create another Admin.
- Admin's direct `system.cloudBackupStatus` attempt is rejected even though Admin may create a manual encrypted backup; cloud backup remains Owner-only.
- Denied manual-backup and restore attempts do not open Android's create/open document pickers.
- Existing tests reconfirm recent reauthentication, final-owner protection, private enrollment/reset, disable/role-change session revocation, logout, inactivity/overall timeout, process restart, Activity recreation, one-use restore tickets, post-restore session invalidation, repeated restore safety, and security audit evidence.

## Verification

- Bridge contract: v4 / 86 endpoints / SHA-256 `D22921E90040EF70CED8AFDE535C6309544B75D5EBD7B726ED1DE1273E0FA136` — pass.
- UI cleanup/package gate: 85 active TypeScript/TSX files; removed prototype/dependencies remain absent — pass.
- Kotlin production and Android-test compilation — pass.
- New Phase 14.2 physical-device matrix: **2/2**, zero skipped, zero failed.
- Related security/session/backup/lifecycle/recovery physical-device regression: **36/36**, zero skipped, zero failed.
- Host-side debug unit suite: **7/7**, zero failed.
- Complete-suite inventory is now **260 tests**: 259 authenticated tests plus the separate one-test empty-identity visual partition. Phase 14.6 will add mandatory Quick unlock coverage; the resulting expanded complete two-part rerun is the Phase 14.7 closure gate.

## Evidence limits and remaining gates

- This checkpoint proves visibility with packaged DOM assertions and authorization with native responses. A screenshot alone cannot prove either permission enforcement or that a forbidden Android picker/provider action never ran, so the accepted 83-screen visual evidence was not redundantly recaptured.
- Live Google Drive consent, upload, listing, provider failure/retry, and revocation are not claimed here. They remain Phase 14.3 and require matching Android/Web OAuth clients plus an owner-controlled test account.
- The second Samsung was not changed. Phase 14.4 still requires explicit confirmation that a separate QA package and destructive replacement restore may be used without affecting its existing ordinary Saaf installation.

## Plain English

We signed in as every kind of user and checked exactly what each one can see and do. We also tried to bypass the screens and call protected Android functions directly. The app blocked every forbidden attempt, including Admin access to owner-only accounts, cloud backup, and restore. Nothing in the working app needed fixing; we added stronger tests so these rules stay protected.
