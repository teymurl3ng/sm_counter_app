# Capsule/readability redesign — planning only

No app code, APK, phone data or design tokens changed. The independent-judgment UserPromptSubmit hook was inspected and run. Its reminder also appears in this turn's developer context. Impeccable is used as a researched design reference, not an installed skill or executed plugin.

## Evidence and limits

- Source map: 62 screen-source files, with embedded screen states/components listed in SCREEN_MAP.csv; all 135 nativeui Kotlin files scanned and hashed. Role entry wrappers inherit the underlying shared screen and must not get a second enclosing card.
- Existing capsule map: 36 helper call sites in EXISTING_CAPSULES.csv, with source lines and current spacing/height expressions. A source candidate is not automatically a visual defect.
- Confirmed shared weakness: NativeListItem.kt applies 4dp outer vertical spacing, 12dp corners, surface fill and a 1dp outline, but owns no inner content inset. AccountRow, Owner/Manager CommandRow and several dialog rows add vertical padding only. These can put leading content at the surface edge. OwnerProductRow and Cashier MoreMenuRow already have 16dp horizontal padding: the claim that every row has the same defect is not supported.
- Retained A06 account-picker screenshot visibly places avatar blocks against the left card boundary. This supports the inset finding. Other screens still require device visual comparison.
- Confirmed separate layout defect: CashierMoreScreen.kt:430 calls NativeSectionTransition; the form branch at 439–475 emits amount field, notes field, messages and Record button without a Column. NativeAsyncUi.kt:196 places that content in a Box, so these siblings can overlap. This is a credible cause of the reported Count cash regression. It is distinct from the CashCloseEntryScreen count/close form. Reproduce on-device before accepting the fix.
- Count/close SummaryRow uses two unweighted Text elements plus a fixed spacer. Long money values/large text are a reflow risk, not yet a reproduced defect.

## Priority order

|Priority|Scope|Reason|
|---|---|---|
|P0|Cashier blind Count cash; shared cash count/close panels|Overlapping form controls and readability of money/actions can block safe work. Keep blind count blind.|
|P1|Account picker; Work/More for all five roles; Admin People/Records hubs|Most visible and reused navigation; normalize all existing capsules, including rows already padded correctly.|
|P1|Sell; Collect; customer/supplier/employee/product/payment/category/reason pickers; deliveries|Frequent task and selection clarity; distinguish selected, disabled and active states.|
|P1|Products, customers, employees, suppliers, users/roles, records/activity, balances/ledgers|Readable entities and amounts, consistent list identity. Admin delegates inherit these treatments.|
|P2|Reports/overview, shop settings/rates/opening, costing/plans/yield, backup, sync/security history, attendance, production/drawing summaries|Group related information; avoid treating every fact or field as an independent card.|
|P3|Welcome and already clear standalone text/action areas|No capsule requirement identified; retain current composition unless device evidence shows a readability problem.|

The CSV is the implementation checklist. Each row must end as reapplied, inherited, grouped-section, or deliberately no capsule, with visual evidence/reason. Existing helper use does not exempt a row from review.

## Recommended visual direction

Use a restrained rounded surface system: warm ivory page, slightly lighter warm surface, green accents, Manrope typography, soft outline and no default shadow. Keep dark mode equivalents. Preserve this app's established identity while improving spacing and hierarchy.

Use rounded rectangles for menus and data rows, not full pill shapes. Reserve pills for short chips/status labels. Repeated lists can use one rounded section with internal separators; coherent entities with independent actions can use separate surfaces. Uniformity means the same treatment for the same purpose, not a border around every piece of text.

Research basis: [Android Card guidance](https://developer.android.com/develop/ui/compose/components/card) distinguishes coherent content containers from general page structure and documents filled/outlined variants. [Android accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults) require adequate touch targets and describe lifting selection behavior to the row. [Impeccable layout](https://impeccable.style/docs/layout/) recommends task-based grouping/density and can replace unnecessary boxes with spacing. [Impeccable typeset](https://impeccable.style/docs/typeset/) recommends consistent text roles and checking scaling/long content. These are design-system references, not evidence that our proposed values already work in this app.

## Proposed component dimensions

Values below are our initial proposal, not externally mandated standards. Measure in Android dp/sp; heights are minima and must expand with content.

|Variant|Where|Minimum height|Inner padding|Shape/layout|
|---|---|---:|---|---|
|Navigation|More/Work/hub destinations|72dp for title+supporting text; 56dp for one line|16dp horizontal, 12dp vertical|12dp radius; 24dp leading icon; 12dp gap; fixed trailing chevron slot|
|Entity|Accounts, products, people|72dp; content grows|16dp horizontal, 12dp vertical|12dp radius; optional 40dp avatar inside inset; 12dp gap|
|Selection|Picker dialogs/payment choices|56dp one line; 72dp two lines|16dp horizontal, 12dp vertical|12dp radius; explicit selected fill/outline and check/radio; one row click target|
|Record|Transactions, deliveries, history/plans|72dp base; expanded records grow|16dp horizontal, 12dp vertical|12dp radius; flexible title and reserved amount/status slot; stack metadata/actions when narrow|
|Section|Forms, totals, review, receipts|Content-driven|16dp all sides|16dp radius; 12dp internal spacing; field groups remain native|
|Status|Warning/error/success|Content-driven|16dp all sides|12dp radius; semantic fill/icon; never communicates state through color alone|

All page content: 16dp gutters, 8dp between separate row surfaces, 24dp between major sections. A grouped list has no double borders or per-row exterior gap. Container owns inner padding; the parent owns outer gaps. Remove old caller padding when migrating, rather than stacking another 16dp on top.

Text: primary 16sp/22–24sp, semibold; supporting 14sp/20sp, regular; metadata 12sp/16sp for nonessential details only. Primary and secondary text must remain legible against both fills; check contrast rather than assuming token names guarantee it. Text starts on a shared column within each list; icon rows and no-icon rows are explicit variants. Long entity names wrap; money is never truncated or squeezed into a fixed-width slot. Large text may move money/actions below labels. Minimum independent touch target 48dp, per Android guidance.

No per-screen arbitrary radius, fill or left inset. Exceptions are structural: product tiles, expanded delivery records and summary panels. They share tokens, not identical heights. DESIGN_PREVIEW.html is a visual proposal, not an app screenshot or validated Compose layout.

## Bounded implementation plan (next stage)

1. Capture before evidence: Cashier Count cash, one affected Work menu, all-role More menus, account picker, entity list and selection dialog. Mark other findings in the CSV; do not expand into business logic or navigation redesign.
2. Correct the Count cash form's layout container. Introduce shared Navigation/Entity/Selection/Record/Section variants with owned insets and states. Do not simply add padding to the old modifier, which would double-pad already correct callers.
3. Reapply every existing helper call site, then migrate mapped missing equivalents. Work by shared family, so role wrappers inherit once. Explicitly record no-capsule decisions.
4. Check every mapped destination/state visually in portrait, light/dark, default/150% text, long names/large money, loading/empty/error/selected/disabled as applicable. IME checks for Count cash and affected selectors. No landscape design.
5. Run qaFast, affected qaScreen groups, then one full checkpoint on frozen sources. Preserve before/after images, require Owner visual review; no self-approved sidecars. Install isolated QA only after this stage's validation.

Acceptance: no overlapping Count cash controls; consistent 16dp inner insets; aligned list columns; no clipped names/money/actions; appropriate semantic selection; no unexpected duplicate surfaces; all-role parity; no new flash/re-entry or keyboard reachability regressions. Completion requires every CSV row accounted for, not just a few attractive screenshots.

Deferred: branding changes, new business rules, money calculations, auth policy, broad animation rewrite, unrelated defects, tablet/landscape redesign. Discoveries outside this scope go to a later-work note.
