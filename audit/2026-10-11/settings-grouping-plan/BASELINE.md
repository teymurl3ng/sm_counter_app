# Settings and information grouping baseline

Planning only, 11 October 2026. Current working tree includes the previously implemented entrance and Quick PIN changes; those are preserved.

The reported Shop settings layout is supported by current code: identity facts, six rate facts, opening details/notices/actions, and expense categories share one scrolling Column. The first three groups have no containing surface. Expense categories already have outlined rows, so the whole screen is not uniformly plain. `OwnerShopSettingsScreen.kt:425-586` and the plain `OwnerShopSettingsFact` at 1220 are the direct evidence.

This pass covers the active native product across Owner, Admin, Manager, Cashier and Operations, shared task UI, authentication and welcome. The launcher alias targets `NativePreviewActivity`; the manifest explicitly retains HybridActivity dormant. Dormant WebView/legacy screens and Android-owned biometric/file/account prompts are outside the app layout redesign.

SCREEN_MAP.csv accounts for all 63 screen-named source files, including forwarding wrappers and the shared header (not 63 distinct user screens). All 137 native Kotlin sources were read for the source inventory and frozen by hash; rendered declarations in screens, role hosts, common components and the keypad are indexed separately. Source inspection covers composed list/detail/edit/review/receipt and relevant conditional sections. The declaration index is a search aid, not an AST or proof of every runtime permutation.

19 source files contain targeted grouping opportunities. Reused Owner layouts also appear under Admin and Manager; these are inherited routes, not additional independent implementations. This is a source-backed audit. No fresh whole-app phone walkthrough or exhaustive screenshot review was performed, and no visual acceptance is claimed.

|Order|Affected area|What is present today|Proposed treatment|
|---|---|---|---|
|1|Shop settings overview and category detail|Plain fact stacks; category list already surfaced|Four coherent overview sections; grouped category facts|
|2|Personal security, all five roles|PIN/phone state as plain text above two outlined fields and actions|Status section plus separate credential setup section|
|3|Customer, supplier, employee, product, user details|Readable entity rows lead to mostly bare fact pages|Account/contact/product/access sections; sensitive actions separated|
|3|Records and Cashier activity details|Amount, identity, state and movement facts run together|Receipt summary plus related detail sections|
|4|Backup and sync|Plain status stacks, divider-only recovery/review items|Status, recovery and review groups; restore warning stays prominent|
|4|Costing and plans|Policy rows surfaced; budgets/targets/yield/loss rows only padded/divided|Existing Record style for repeated items; grouped policy facts/components|
|4|Report totals and security-audit details|Plain facts beside already surfaced product/audit lists|Grouped read-only facts; existing list surfaces retained|

The absence of a background is not itself a functional defect. Work/More destination rows, entity/record lists, most selectors, sale controls, review panels and most receipts already have deliberate controls or surfaces. Welcome, supporting copy, form labels, native dialogs and the centered Drawing success composition can remain plain. Nothing in the evidence warrants adding a capsule around every Text element.

Specific bounded findings: Shop settings and Reports put a divider after already outlined rows; when those sections are touched, remove that redundant exterior separator. Sync status uses two unweighted texts in a SpaceBetween Row; long content may squeeze/overflow, but this was not reproduced on-device. The proposed fact layout must reflow it. Cashier blind-count success is unboxed but short and structured; any receipt consistency polish is deferred, not counted as a confirmed defect.

The independent-judgment hook was executed. Reassessment: clearer typography and dividers alone could support a valid settings layout; boxes are not an industry requirement. Grouped sections are chosen here because they fit the established ivory page / warm surface design and the user's requested readability improvement. They should replace loose fact spacing, not stack new containers over existing row cards.
