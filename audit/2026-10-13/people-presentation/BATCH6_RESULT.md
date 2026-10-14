# Phase 3 batch 6 — people presentation

Completed the mapped row/read-only detail presentation for customers, suppliers, employees and local users. Customer/supplier/employee rows now separate name, full signed balance, route/location/role and status. User rows separate display name, role, login, status and setup-required notice. Existing detail facts share one responsive information section; balances, credit limits and reference salaries use metric emphasis. Existing optional facts, labels, callbacks and forms remain. Employee salary accounts and staff logins remain separate.

Source inspection confirms AdminPeopleScreen delegates to all four components, while ManagerMoreScreen delegates to customers, suppliers and employees. No duplicated role-specific presentation or permission change was introduced.

Verification used the isolated QA app on S24 (SM_S921W). Both APKs built and eleven selected existing UI contracts passed: each family's row/detail callbacks plus staff setup/disable actions. Three added cases have passing results: archived long customer row at light 150% with `PKR -123,456,789.99`; archived customer detail at dark 200% with long name/address and reachable Edit; disabled staff detail with Enable and Resume setup. After the balance lines were given the full available row width, the supplier/employee row callbacks passed again and both customer stress cases passed on the final source. Git diff whitespace checks passed.

The first stress assertion used TextLayoutResult.hasVisualOverflow and failed on both a balance and the short word “Archived”. Measurement showed a text container narrower than the available row and no height overflow. The final test checks complete character coverage, no ellipsis, no height overflow, and glyph right edges within one pixel of the whole-pixel container. This handles fractional glyph/container rounding without accepting missing text. These failures do not establish visible clipping.

Three actual-screen captures were pulled and visually reviewed, retained locally at `output/batch6/customer-detail-light.png`, `customer-row-light-150.png`, and `customer-detail-dark-200.png`. They show consistent fact alignment/insets and complete signed balances. The shared header date still abbreviates at 200%, already recorded under phase 5. This is representative verification, not a full theme/font/state matrix for every family; wider phone acceptance remains phase 5. Raw device captures remain local.

The previous completed-work checkpoint d3f962f was pushed to origin/main before this batch. Batch 6 changes are currently local; the isolated QA APK is installed on S24. No full test suite or ordinary release install was performed.

Next bounded execution: phase 3 batch 7, mapped management information only.
