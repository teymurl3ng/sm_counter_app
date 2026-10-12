# Proposed design and deterministic scope

Use a calm dark forest header and a small, consistent hierarchy of text roles. Keep the current font family, body surfaces, capsule shapes/insets, top/bottom sizes, portrait lock, navigation and business behavior.

|Role|Night color|Day color|Type and placement|
|---|---|---|---|
|Signed-in header background|#20372D deep forest|Existing #173F32|Same title/context/action slots; no height or wording changes|
|Header title / person|#F0F3EF|Existing #FFFDF7|Existing sizes/weights; no new metric changes|
|Header shop / role / date|#C5D2CB|Existing effective metadata color|Existing context style; explicit readable night token instead of inheriting onPrimary|
|Product, person, task, section title|Existing #F0F3EF|Existing #17201B|16sp/22sp semibold for row names and section titles; retain the 22sp shop title|
|Reference, timestamp, route, short description|#B3C2B8|Existing #515F55|14sp/20sp medium; own line or clearly separated slot; prefixes stay visible|
|Setting label|#B3C2B8|Existing #515F55|14sp/20sp medium; existing label/value pairing and reflow|
|Setting value|Existing #F0F3EF|Existing #17201B|16sp/24sp semibold; long address/notes remain regular body text|
|Money / price / balance emphasis|#E9D6A7 warm ivory-gold|#62490E muted warm brown|16sp/24sp bold; predictable end-aligned slot when it fits, otherwise its own line; full currency and sign|
|Quantity / units|Primary text|Primary text|16sp/24sp semibold; quantity is not automatically money|
|Routine action|Existing mint #A8D8C4|Existing green #173F32|Existing button/selection affordance; keep action color separate from data emphasis|
|Status / error|Existing semantic colors|Existing semantic colors|Retain explicit status text; no red/green money classification|

Warm numeric emphasis identifies important data, not paid/unpaid, profit/loss or success. This makes it different from mint edit actions without adding a different hue for every field. References remain quiet but readable. Plain text values must not acquire button-like styling. Short settings subgroup headings become 16sp/22sp semibold primary text; supporting menu descriptions become medium. Long help paragraphs remain regular.

On a selected mint product surface, use its existing dark onPrimary foreground for both title and price; distinguish price by weight and placement. Do not use the night-card warm foreground on that bright background. This is a deliberate background-specific exception to the palette, not a different meaning for price.

Transaction row: first scan point is the task or available product/customer identity; numeric value occupies a consistent separate slot; reference and time are secondary; actor/status have their own supporting line rather than a single undifferentiated run. Product row: name, price and unit/state are distinct roles. Do not add unavailable product-name data to Records/Activity. All current information remains visible.

Measure the exact final TextStyle, including its font weight, when deciding whether a title/action or name/metric fits. At 150%+ text and for long names/large money, stack rather than shrink or clip. Preserve current 16dp inner insets, 12dp inline gaps, content-driven row heights and minimum touch targets. No new nested capsules or accordion.

Implement in these finite passes, finishing each before proceeding:

|Pass|Exact production boundary|Done when|
|---|---|---|
|A — header and settings|New common visual-role tokens wired through SaafTheme; NativeChrome; NativePreviewActivity status-icon policy; NativeInformationSection; OwnerShopSettingsScreen; five More screens (Owner/Admin/Manager/Cashier/Operations), only their section labels/menu titles/subtitles/security text roles|All five signed-in roles inherit the same dark band; correct status icons; Shop settings labels/values/subgroups and menu subtitles follow the contract. Top/bottom geometry unchanged. Stop for appearance review.|
|B — busy transaction/product lists|OwnerRecordsScreen RecordsRow and its existing read-only detail headings/facts; CashierActivityScreen ActivityRow/detail; OwnerProductsScreen OwnerProductRow/detail facts; CashierSaleScreen ProductCard and existing receipt labels/values only; DeliveriesScreen DeliveryCard only|Name/reference/time/status/metric can be scanned independently; selected product remains legible; amount/quantity roles correct; existing open/select/detail handlers preserved. No facade/model changes.|
|C — people lists|OwnerCustomersScreen OwnerCustomerRow/facts; OwnerSuppliersScreen OwnerSupplierRow/facts; OwnerEmployeesScreen OwnerEmployeeRow/facts; OwnerUsersRolesScreen OwnerUserRow/facts|Names, balances and contact/status metadata follow the same roles. Existing Admin/Manager delegated views inherit once; no duplicated redesign.|

Planning is not approval to implement this turn. Begin with **pass A only** after execution is requested. Existing grouping batches 2–4 remain separate; when reached, their security/entity/report/sync facts should use this contract. Other inventoried rows are follow-up candidates, not an authorization to widen A–C. Do not append newly discovered screens mid-pass; record them in LATER.md.

Excluded: global primary/onPrimary or global body typography replacements; footer redesign; changing capsule geometry; animation/haptics audit; authentication/biometric/PIN redesign; database/auth/account work; adding product-name summary fields; reporting/data behavior; landscape; new pagination/filters; rewriting forms or validations; mass coloring every Text call.

Validation stays scoped:

- Compile touched native UI. Pass A: affected NativeChromeGeometryTest methods for signed-in role headers plus existing NativeInformationSectionTest and Shop settings layout contracts; relevant More menu/security render methods only. Pass B: affected records/activity/products/product-selector/delivery row and detail contracts. Pass C: affected entity/user list/detail contracts. Select methods, not every workflow in each class.
- Meaningful text-role/component checks: actual text/background contrast >=4.5:1 for enabled roles, correct selected foregrounds, readable reference prefixes/units/signs, no visual overflow with long names and large values, identical header/footer bounds before/after navigation. No tests that merely repeat hex constants.
- Representative A06/S24 portrait captures in light/dark at default, 150% and 200% fixture text. Inspect rows with long names, balances, archived/corrected statuses and selected product states relevant to the pass. Do not change phone accessibility preferences to create fixtures.
- In A, verify installed Android status icons with the actual dark header; a Compose-only screenshot does not cover the activity policy. Affects all signed-in roles, so check role-header inheritance. Keep entrance and bottom icon behavior unchanged.
- Preserve identifiers, full money and quantity values, button semantics, read-only facts and existing action tags. Appearance approval is the user's; screenshots stay UNREVIEWED until then.
- No full qaFast/checkpoint, broad suite or landscape run. No commit/push/install unless execution calls for it. Stop at the selected pass boundary.
