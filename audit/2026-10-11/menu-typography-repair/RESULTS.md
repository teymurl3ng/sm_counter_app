# Menu typography repair — completed

Implemented the approved two-batch repair on 2026-10-11, then installed the final preview on A06. Scope: twelve production sources and four test sources compared with the saved working-tree baseline. Prior uncommitted authentication, entrance, PIN, chrome and other work was preserved. No commit/push requested.

## Result

- Shop settings uses consistent 16sp/22sp SemiBold section headings: Shop details, Rates, Opening balances, Expense categories and Category details. Dynamic shop/category names are separate full values. Supplier and Retail & delivery are 14sp/20sp SemiBold supporting subheadings.
- Inline read-only facts now align actual first text baselines. Labels remain 14sp Medium, values 16sp SemiBold or Bold for emphasized money. Address/notes remain regular prose; long values retain stacking and full content.
- Heading/action text shares a baseline when inline. Explicit action padding preserves the existing common trailing inset. Long-title/150%+ fallback actions remain trailing-aligned below the heading; touch targets remain at least 48dp.
- All nine equivalent navigation families use 16sp SemiBold titles, 14sp Medium descriptions, a 24dp icon and a 14dp icon gap with the existing owned inset/surface geometry. Group labels use the consistent 14sp SemiBold supporting role. Manager Work has two inline group labels; their typography/color were normalized in the same allowlisted source, without changing task controls or handlers.
- Fifteen named menu/group helpers are internal so instrumentation can exercise the actual layouts directly; no public API was added. Their callbacks and behavior outside presentation were retained.

Global typography/font family, light/night palette, header/footer dimensions, authentication, permissions, primary task tiles, badges, forms, validation, database schema and business handlers were retained. Source comparison verified nine menu files outside the fifteen named helpers (with the two explicitly recorded Manager inline label role changes) and eight settings editor/helper functions. All other main/test source hashes match the baseline.

## Scoped validation

**26 unique affected checks have passing latest results**, detailed in validation-summary.json:

- Four information-component fixtures: default light/night and 150%/200% text. Assert rendered first baselines within one physical pixel, heading roles, trailing normal/stacked action anchors, full text and callback behavior.
- Nine settings cases: long overview at default/150%/200%, category detail at default/night-large, Edit details/Edit rates/Add category callbacks and category navigation. Existing scrolling assertions keep outer chrome stable.
- Nine actual activity/menu checks: all five More menus, Admin People/Records, Owner Work and Manager Work. Each inspects the actual rendered menu role and captures its screen.
- Four menu stress fixtures exercise all nine production row helpers and six group-label helpers in light/night/default/150%/200% combinations. Check text positions, title/description roles, complete wrapped text, Work badge separation and one callback per tap.

There were 35 test executions including fixture diagnostics. The first four menu fixtures read clipped off-screen description bounds after scrolling only the title; the fixture was corrected to scroll the entire row. The next four reached nested Work badges but used the merged semantics tree; queries were corrected to inspect the actual unmerged badge node. Both failed logs are retained. All four final fixtures pass, and the final Manager inline-label adjustment was independently rechecked. No production workaround was made for these fixture issues.

Instrumentation/preview builds and diff whitespace checks passed. No full regression/checkpoint suite, unrelated workflows, landscape run or S24 dependency was introduced.

## Device delivery and evidence

Final `com.saaf.milkshop.qa` preview installed with `install -r` on A06 **R8VXA02SNKH**. Ordinary `com.saaf.milkshop` package metadata was identical before/after; the preview's original install time was retained. APK SHA-256:

`c91e455f0c444cdd30776b67357cf7af68ea14b8c66fd6f7484f4201ea4e0e73`

Installed preview checked at **Preview Owner → Shop settings**. One Edit details/back cycle passed with unchanged title bounds `[30,65][690,127]` and bottom More bounds `[608,1451][662,1482]`, matching the earlier baseline. No shop settings were saved. The first automation attempt read stale XML after a failed UI hierarchy dump; the script now refuses stale dumps and the retry passed. This was an automation failure, not a reproduced edit-route defect. This position check is not a frame-by-frame animation certification.

Evidence: 31 scoped component/settings/menu fixture or activity captures with original review sidecars, plus the installed settings capture and interactive re-entry JSON. Agent visual inspection covered representative settings light/night/200%, large-text menu anatomy, representative Owner More/Admin People/Manager Work captures and the final installed night settings screen. Original sidecars remain UNREVIEWED for user appearance review; screenshots do not certify every state of every screen.

The A06 is left on Preview Owner → Shop settings, ready to test. The S24 remains unchanged, as requested. Later list/detail/security grouping items remain in the approved plan's LATER.md; this repair does not start them.
