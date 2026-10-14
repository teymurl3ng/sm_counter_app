# Stable layout and keyboard repair - 14 October 2026

Release publication is paused by the user. This is a bounded presentation repair within finish step 7, before rebuilding the candidate. It does not reopen completed business/sync work or add another release batch. Functional tests remain deferred by the user's prior instruction.

## Evidence and limits

- The active launcher is NativePreviewActivity (native Compose). MainActivity/HybridActivity and the dormant web interface are excluded from this active-app repair.
- INVENTORY.json freezes the source hashes and call-site lines. There are 112 text-field call sites in 28 production native UI files, including read-only fields, shared field helpers and dialog searches. This is not a claim of 112 different screens or 112 reproduced bugs.
- Costing HubHeader adds an intrinsically sized row containing Approve policy only in the Policies ready branch. Other sections compose an empty row. Its different height is a confirmed source cause of the reported tab/content jump.
- All five role hosts use AdaptiveNavigationScaffold. It removes the full bottom-navigation slot when raw IME bottom becomes greater than zero. That is a discrete layout change during an otherwise animated keyboard transition.
- Most form roots apply standard imePadding; Sale, Collection and Deliveries instead use imePaddingExcludingNavBar. Opening cost basis and Sync lack a local IME modifier. Host padding is applied without consumeWindowInsets. These are confirmed implementation differences, not proof of each device's reported gap.
- The manifest comment claims adjustResize plus imePadding necessarily doubles keyboard height. Android's edge-to-edge Compose documentation instead specifies adjustResize and consumed insets. That comment is not sufficient evidence for the old workaround.
- Access forms already have a stationary toolbar and a scrolling body with navigationBarsPadding/imePadding. They require the activity/inset correction but no new authentication design. Quick PIN uses its dedicated keypad, and native biometric prompts remain platform-owned.
- Full-screen Backup and Opening reauthentication bodies cannot scroll. Report-range, policy and role reauthentication dialog bodies also cannot scroll. Shortened keyboard viewports and larger text can therefore leave their fields/context inaccessible.
- Other menus use fixed host chrome, in-body actions, scrolling groups, or existing loading/receipt transitions. The audit does not establish that every conditional business section is a defect. Preserve count/close guards, receipts, loading/error meaning and action eligibility.
- No physical keyboard trace or new functional test has run for this complaint. Exact perceived movement and the size/color of the user's gap remain phone acceptance items.

## Fixed implementation scope

1. Make the shared role scaffold apply and consume its own content padding, then apply animated IME padding. Keep navigation anchored at the physical bottom behind the keyboard rather than deleting its slot at the first animation frame. Pass zero padding to the already inset content. This covers all five roles and their hosted fields, including Opening and Sync. Nested standard IME padding sees the consumed amount and must not add it again.
2. Set the native activity's documented adjustResize mode; remove the unsupported double-count explanation. Change the custom subtraction helper to delegate to standard consumed IME padding so independent/previews and hosted forms agree. No manual Samsung-specific height arithmetic or delayed scroll hack.
3. Keep Costing tabs and a font-aware action toolbar at the same height in every section. Use existing actions: Approve policy, New plan, Shop settings, Approve baseline. Move the existing New plan and Approve baseline controls out of their list headings into that toolbar; retain tags, callbacks, access checks and secondary actions. Keep the toolbar present in loading/error states with its action disabled.
4. Allow the named short reauthentication and range-dialog bodies to scroll within their available viewport. Keep access chrome, global font/theme, money/stock rules, credentials, database, existing drafts and navigation callbacks unchanged.

No new settings, authentication flows, landscape design, broad capsule/theme redo, server changes or release publication in this repair. If unrelated issues appear, record them without expanding the implementation.

## Android guidance applied

The [Compose inset guide](https://developer.android.com/develop/ui/compose/system/insets-ui) specifies explicit resize support and automatic inset consumption, including consumption of ordinary parent padding. It also warns that raw inset reads during composition can lag layout by a frame. Use layout-phase inset modifiers rather than toggling an entire navigation slot on raw height.

The [keyboard animation guide](https://developer.android.com/develop/ui/compose/system/keyboard-animations) demonstrates animated IME padding. Gesture-driven imeNestedScroll is optional; do not add it to every business form just to obtain normal keyboard animation.

The [text-field focus guide](https://developer.android.com/develop/ui/compose/touch-input/focus/focus-in-text-fields) describes normal field focus and keyboard dismissal. Preserve normal Compose focus/caret behavior and scroll only the form viewport, not the host header.

## Completion boundary

- Source inventory and plan completed first.
- Implement only the changes above and compile affected Kotlin; compilation is not functional acceptance.
- Save a focused acceptance checklist for later: first/last fields, focus changes, Next/Done/Back, keyboard show/hide/re-entry, narrow and larger-text portrait, dialogs, all five role hosts and Costing tabs. No full suite, device installation or release build during this repair.
- The previously built 0.4.2 APK predates this repair. Do not describe it as containing this fix or install/publish it as the final candidate.
