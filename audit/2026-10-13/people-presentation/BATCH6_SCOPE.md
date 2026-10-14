# Phase 3 batch 6 — people presentation

Only the row and read-only detail presentation in OwnerCustomersScreen, OwnerSuppliersScreen, OwnerEmployeesScreen and OwnerUsersRolesScreen. Admin/Manager delegates reuse these screens. Preserve all models, permissions, edit/enrol/reset actions, disabled/setup-required states, and the separation between an employee account and a staff login.

Rows: name first; signed balance as a distinct complete metric for customers/suppliers/employees; then contact or role and state on separate supporting lines. User rows separate display name, role, login, status and setup-required notice. Details: one NativeInformationSection per person with the existing facts and responsive label/value reflow. No form, schema, data, identity or navigation changes.

Verify representative row/detail callbacks and archived/disabled/setup-required contracts for each family, plus a long-name/large-value stress fixture using the existing shared fact geometry checks. Run only affected contract methods on S24. Stop after these four families; management screens remain batch 7.
