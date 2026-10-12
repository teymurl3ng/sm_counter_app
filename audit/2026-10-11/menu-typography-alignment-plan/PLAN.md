# Proposed bounded repair

Repair the shared information layout first, then normalize equivalent navigation menus. Pause the deferred transaction/people styling passes until this contract is stable. This is a design and execution plan; no implementation is part of the current turn.

## Typography contract

Keep Manrope and the current light/night palette. Assign roles by purpose rather than choosing a size separately in each screen. Same-level elements share the same role; labels, values and descriptions retain a useful hierarchy.

| Role | Size / line height | Weight | Placement and color |
| --- | --- | --- | --- |
| Section heading inside an information card | 16sp / 22sp | SemiBold | Leading edge, primary text; same role across Shop details, Rates, Opening balances, Expense categories and Category details |
| Menu group label / rates subgroup | 14sp / 20sp | SemiBold | Supporting color; title case/sentence case as appropriate, no automatic all-caps |
| Navigation item title | 16sp / 22sp | SemiBold | Primary text; all nine equivalent More/hub/command row families |
| Navigation description / fact label | 14sp / 20sp | Medium | Supporting color; complete lines |
| Short fact value / identity value | 16sp / 24sp | SemiBold | Primary text; money may retain current Bold/accent role |
| Address, notes and explanatory prose | 16sp / 24sp | Regular | Primary body text; explicit stacked layout where appropriate |
| Secondary header action | 14sp / 20sp | Bold | Existing action color and button styling; stable trailing anchor; minimum 48dp touch target |

These sizes are app-specific choices drawn from the existing scale, not an industry mandate. Do not change global SaafTheme typography to achieve them. Add explicit group-label/navigation-title roles to NativeVisualRoles; retain the existing section, supporting and value roles where they fit.

## Layout contract

1. **Inline facts:** label at the leading edge, value at the trailing edge, both on the same first text baseline. Keep the existing 12dp gap and available-width/large-text stacking policy. Align through Compose text baselines rather than manual pixel offsets or center-aligning unequal text boxes.
2. **Stacked facts:** label then value with the existing 2dp gap; both leading-aligned. Long names, addresses, notes and complete money values may grow. No ellipsis or smaller font to make a value fit.
3. **Heading + action:** one section-heading style. Heading/action text share a baseline when inline, while the button keeps its 48dp target. Preserve the current common horizontal action inset, made explicit and used by the fit measurement. For the existing long-title/150%+ fallback, action moves below but remains trailing-aligned, rather than changing to the left. Verify the actual rendered button-text baseline; do not assume its bounding-box center is sufficient.
4. **Navigation rows:** one owned 16dp horizontal / 12dp vertical inset, 24dp leading icon, 14dp icon-to-text gap, 2dp title-to-description gap, centered icon/chevron, 72dp minimum height. Height may grow for wrapped text. Keep the existing exterior 4dp vertical padding and surfaces.
5. **Group labels:** align with the outer row/container leading edge; keep existing 10dp top/bottom padding in grouped hubs. Rates subgroups retain their existing interior padding/divider positions, with the smaller subgroup role.
6. **Stable shell:** preserve header/footer dimensions and routes. A long-content reflow may grow the scrollable body, but ordinary navigation/re-entry must not change chrome geometry. No typography or animation-driven resizing of the top/bottom shell.

## Batch A — fix Shop settings and shared information

Production allowlist: exactly three sources, relative to nativeui:

- common/NativeInformationSection.kt: baseline-aligned inline facts and heading/actions; explicit trailing fallback placement; consistent heading role.
- theme/NativeVisualRoles.kt: separate section/group/navigation roles; no palette change.
- owner/OwnerShopSettingsScreen.kt: use **Shop details** as the first section heading and show the actual shop name as a **Shop name** fact. Separate Supplier and Retail & delivery as subgroups. Use **Category details** as the category section heading with a separate category-name value. Retain complete dynamic names, existing detail-name semantics/tag, currency, rate/amount values and Edit/Add wording.

Keep editors, fields, event handlers, permissions, account data and validation unchanged. New name facts reuse the existing snapshot/category data and require no database change.

Acceptance:

- All information-card section titles have the 16sp/22sp SemiBold role. Rates subgroups have the 14sp/20sp SemiBold supporting role.
- Currency/PKR, Phone/Not set, TS standard/percentage, money pairs, opening counts and category Account/Status share first baselines whenever inline. Measured first-baseline difference <=1 physical pixel in the test viewport.
- Edit details, Edit rates and Add category share a consistent heading/action baseline and trailing action anchor. Long headers and 150%/200% cases use the documented trailing fallback with no overlap/clipping.
- The dynamic shop/category name remains fully readable, including a long-name fixture. Address/notes stay readable prose. No horizontal change to top/bottom chrome on Edit/back re-entry.

Validation: only affected NativeInformationSection and NativeOwnerShopSettingsScreenContract cases, expanded to assert rendered baselines and roles rather than rectangle tops. Check default light/night, then only targeted 150%/200% fixtures with long names/money. Inspect real A06 Shop settings and one Edit/back cycle; no settings saves needed. Reuse a small existing chrome-position assertion; no whole-app chrome suite.

**Stop and inspect Batch A captures before propagating.** This is a quality check within the approved implementation, not a new permission gate.

## Batch B — normalize equivalent navigation menus

Production allowlist: nine screen sources:

- owner/OwnerMoreScreen.kt
- admin/AdminMoreScreen.kt
- manager/ManagerMoreScreen.kt
- more/CashierMoreScreen.kt
- operations/OperationsMoreScreen.kt
- admin/AdminPeopleScreen.kt
- admin/AdminRecordsScreen.kt
- owner/OwnerWorkScreen.kt
- manager/ManagerWorkScreen.kt

Modify only the named MenuRow/CommandRow and group-label/SectionTitle presentation helpers. Use the shared title/description/group roles; normalize Work command icon gap to 14dp. Keep primary action tiles, badges, personal-security forms/status helpers and all destinations unchanged. Cashier/Operator flat menus do not need invented group labels. Existing five More row geometry is the reference; this pass does not redesign capsules.

Acceptance:

- Equivalent navigation titles use SemiBold and descriptions Medium in every role/hub/command family.
- Group labels are consistently smaller supporting text, and visibly distinct from item titles.
- Leading text starts at the same relative position for all icon-led navigation rows; descriptions align with their title; icons/chevrons remain centered after wrapping.
- Long descriptions, optional badges and 150%/200% text can grow without clipping/overlap. Navigation and existing callbacks still work.

Validation: affected role-menu cases in NativeFoundationUiTest plus targeted hub/Work row fixtures if existing coverage does not expose those helpers. One shared default/night/large-text anatomy fixture avoids duplicating every font/theme combination per role. Check route callbacks through the relevant existing navigation cases. Actual A06 captures for the five More menus, Admin People/Records and Owner/Manager Work; use QA fixture identities where available. No account/database setup or broad functional suite.

## Finish and stop

The repair is complete when the twelve-source allowlist and affected checks satisfy the two batch contracts and the A06 evidence has been inspected. Record the installed APK and exact tested-source hashes if implementation is later approved. Newly noticed defects go to LATER.md instead of widening the pass. No transaction-list pass, entity/detail grouping, security-form overhaul, color/shape redesign, full regression run or S24 dependency is added.
