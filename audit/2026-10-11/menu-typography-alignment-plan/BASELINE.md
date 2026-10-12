# Menu typography and alignment baseline

Planning only, 2026-10-11. No app/test sources changed, no build/install, no database writes. Source hashes match the exact sources used for the currently installed A06 preview from night-hierarchy pass A.

Coverage: lexical style/layout screening of all current native Kotlin sources; explicit classification of 42 menu, heading, fact and related detail sites; coverage map for all 63 screen-named source families, including forwarding wrappers and shared header support. These counts are **not** counts of defective screens. STYLE_SITES.csv is a search index, not an AST, renderer or proof of a defect. MENU_MAP.csv contains the inspected families, current roles, evidence strength and next action. SCREEN_MAP.csv carries the wider screen-family coverage.

Visual evidence re-reviewed: actual installed A06 Shop settings, actual Owner More and Cashier More captures, and the prior dark information-component fixture. No claim of a fresh visual crawl of every account, submenu, editor or device. The A06 remains sufficient; S24 reconnect/install is outside this task.

## Confirmed findings, in priority order

| ID | Where | Current evidence | Consequence / classification |
| --- | --- | --- | --- |
| T1 | Shop settings short facts; expense-category Account/Status inherit the helper | NativeInformationFact uses 14sp/20sp Medium labels, 16sp/24sp SemiBold/Bold values and Alignment.Top. Currency and PKR are visibly offset in the installed screenshot; XML gives both a top of 638px but text rectangles of 36px and 41px height. | Confirmed pairing/alignment defect. Text-box tops are aligned instead of text baselines. Exact baseline displacement has not been measured this turn. |
| T2 | Shop settings card headings | Dynamic shop name uses titleLarge 22sp/28sp; Rates, Opening balances and Expense categories use 16sp/22sp. | Inconsistent section-header anatomy. The larger shop name could legitimately be an identity treatment, but its placement in the same heading/action slot makes that distinction unclear. Use a consistent section header and separate name value. |
| T3 | Rates subgroups | Rates, Supplier and Retail & delivery all use 16sp/22sp SemiBold and primary text color. | Hierarchy flattened. Rates and Retail & delivery are **already the same size**; the mismatch is their level of emphasis, while the shop-name heading is larger. |
| T4 | Edit details / Edit rates / Add category header actions | Heading/action children are center-aligned; headings vary in size. Both Edit labels end at x637 in the installed XML, despite different widths. The helper moves long-title/large-text actions to the start of a new line. | Confirmed inconsistent vertical relationship and fallback placement. There is no evidence that Edit details and Edit rates have random horizontal anchors. Default button padding explains their inset relative to fact values ending at x660. |
| T5 | Owner, Admin and Manager More group labels | The last pass promoted labels from 14sp supporting text to 16sp/22sp SemiBold primary, matching menu titles. Owner capture shows this directly. Admin People/Records group labels remain 14sp/20sp SemiBold supporting. | Confirmed cross-menu inconsistency introduced by the last pass, and weaker separation between group labels and item titles. |
| T6 | Admin People/Records vs five More menus | Titles are consistently 16sp/22sp SemiBold and row insets/icon gaps match. Hub descriptions are 14sp Regular; More descriptions are 14sp Medium using the newer supporting token. | Confirmed style inconsistency; individual hub overflow/misalignment is not visually established. |
| T7 | Owner and Manager Work command rows vs More navigation | Work command titles inherit 16sp Bold, descriptions 14sp Regular, icon gap 12dp. More uses SemiBold / Medium / 14dp. Both use shared Navigation surfaces and 16dp inner padding. | Confirmed variation in equivalent navigation-row anatomy. Primary action tiles are a different control and do not need to be converted. |

## Related differences mapped for later

- Product, Customer, Supplier, Employee, User/access, Costing, Backup, Reports and Security-audit fact helpers use stacked 14sp Bold labels and 16sp Regular values. Business-record and Cashier-activity detail helpers use uppercase 12sp labels instead. These differ from the newer Medium-label/SemiBold-value roles, but they are stacked and do **not** inherit the inline Currency/PKR defect. Review them with the existing detail-grouping work.
- Sync status uses an unweighted SpaceBetween row: label primary, value supporting, both 14sp Regular. Long shop/account/offline-status values have no explicit allocation or stacking policy. This is a source-level hierarchy inconsistency and fit risk, not a newly reproduced overlap.
- Overview BalanceRow mixes 16sp/24sp labels with 16sp/22sp values in a default top-aligned row. Operator Today's work pairs 16sp titles with 14sp times similarly. Those need targeted baseline/fit checks when the deferred list work resumes; they are outside this menu repair.
- Personal-security forms have stronger 16sp status statements and differing body-title treatment across role shells. This is recorded for the already planned security-grouping batch; this audit does not establish that every security form is visually broken.

## Consistent or intentionally different

Five More item rows have matching title/description sizes, insets, text gaps and icon centering. Cashier's current capture shows coherent wrapping. Preserve this row anatomy. Admin People/Records group-label hierarchy is already appropriate. Material FilterChips, task buttons, account identity rows, stacked cash-close summaries and receipts have different purposes and should not be forced into one menu-row style.

All explicit app typography roles in SaafTheme use the bundled Manrope family, including small/headline slots. Inspected screen overrides do not select a second family. There is no source evidence of a font-loading failure; the confirmed issues are role assignment, weight, line-height relationship and alignment. There is also no evidence that every menu is defective.

## Earlier verification gap

Last-pass checks established contrast, rendered-line fit, large-text reflow, navigation and stable outer chrome. They did not assert shared label/value text baselines, equal-level section roles or consistent heading/action baselines. Passing those checks therefore did not establish the appearance now reported. The More group-label promotion is traceable to the last pass. The fact helper's top alignment and the 22sp/16sp heading split existed in the grouping implementation; do not describe every finding as newly introduced by the night-color change.

Evidence references:

- [Installed A06 settings](../night-hierarchy-pass-a/after-a06-settings-top.png)
- [Owner More](../night-hierarchy-pass-a/evidence-a06/owner-more-surfaces.png)
- [Cashier More](../night-hierarchy-pass-a/evidence-a06/hierarchy-cashier-menu.png)
- [Dark fact and long-heading fixture](../night-hierarchy-pass-a/evidence-a06/component-1.0-dark.png) — fixture system-bar colors and click ripple are not installed-app appearance evidence.
- A06_TEXT_BOUNDS.json — text rectangles, not glyph-baseline measurements.
- SOURCE_HASHES.json / AUDIT_SUMMARY.json — no production/test changes since installed pass A.
