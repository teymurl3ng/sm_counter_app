# Phase 5 UI fit: customer workspace, statement and manual reminder

15 October 2026. Source baseline `3dd5468`. This supplements the phase 5 functional plan before implementation. The initial brief identified the destination but did not sufficiently plan its information hierarchy; this document closes that gap. App code remains unchanged. [Interactive layout study](UI_PREVIEW.html) uses synthetic facts and the existing bundled Manrope files. It is a browser planning aid, not Android layout/animation acceptance.

## Evidence and design authority

- Inspected current `OwnerCustomersDetailScreen`, `CustomerCreditPanel`, `NativeInformationSection`, `NativeInformationHeading`, `NativeInformationFact`, `NativeListItem`, `NativeVisualRoles`, `SaafTheme`, `CashierScreenHeader` and `NativeSectionTransition`. Inspected the retained phase4 light capture: its visible portion confirms the grouped surfaces and history/management density; it is not a new full-page device audit.
- Today, a large customer name is followed by one facts section containing active status, balance, phone, route, address, limit, age, quota and notes. The credit panel then adds status/terms explanation, up to three Owner controls, pending actions, expandable account history and review history. Edit customer is the last filled button. This structure is observed in code; exact first-viewport fit depends on customer content/text settings.
- Use accepted native Counter Ledger leadership/workflow specifications plus the implemented October capsule, section grouping, typography/baseline, night hierarchy and chrome repairs. The code's shared components/tokens govern current dimensions. Root `DESIGN.md` explicitly describes the historical hybrid fallback; its white/emerald palette is not the native implementation authority. The earlier Stitch study was not selected wholesale; do not import it as a fresh visual theme.
- Reuse [Impeccable layout](https://impeccable.style/docs/layout/) for task grouping, spacing and density, and [typeset](https://impeccable.style/docs/typeset/) for consistent roles/scaling. These are the same researched references used in the prior capsule work, not a claimed installed/executed Impeccable plugin. [Android cards](https://developer.android.com/develop/ui/compose/components/card) support coherent groups rather than enclosing every line; [accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults) support minimum targets and visible scalable text. [Bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets) provide secondary content, but a full statement/recipient review deserves the existing body pane in this app. That last choice is our fit judgment, not a platform prohibition on sheets.

## Navigation decision

```
More / People → Customers → Customer detail
                             ├─ Statements & reminders → Statement preview
                             │                             ├─ Share statement → format dialog → Android chooser
                             │                             └─ Prepare reminder → Reminder review → external composer
                             ├─ Account history (existing, expandable)
                             ├─ Customer details (contact/profile, expandable)
                             └─ Credit settings & review (existing controls, expandable)
```

Customer list/search/filter/rows remain as they are. There is one new feature entry on detail, not a new bottom tab, a dashboard tile, a row action on every customer, or a toolbar of PDF/image/WhatsApp/SMS icons. The header continues to say Customers; the short task title appears in the body. The role's existing bottom navigation remains mounted/selected exactly as in its current Customers host.

## A. Customer detail: give the new entry a deliberate place

Reading order:

1. Existing customer name, using its current type role; long names wrap in the scroll body rather than altering the header.
2. One account-summary surface: balance (metric style), explicit credit status/coverage and accepted future-sale terms. Account active/inactive status is a separate, quiet profile fact. Do not repeat balance/status/age in a second card. Keep the full uncertainty/overdue explanation reachable; material unknown/held states remain visible, not hidden behind a generic Good label.
3. **Statements & reminders** navigation row: document icon, title, supporting line “Preview a statement or prepare a message”, trailing chevron. Use existing Navigation geometry; the full padded row is the hit target. This is the sole new feature entry.
4. Persistent pending/held credit attention, when present: show the unresolved count/type and a Review action. Its urgent nature is visible without opening a disclosure. Detailed retry/discard controls open the Credit settings & review section, including scroll/focus to the relevant item. Do not move unresolved attention below collapsed history.
5. Quiet grouped disclosures: Account history; Customer details; Credit settings & review. Retain existing data, permissions, action confirmations and history; no financial or state behavior changes. Only one disclosure opens at a time to keep the page manageable. Reopening the same customer retains its selection/scroll; switching customers resets it. Histories load on demand if needed, using existing sources.

Customer details owns phone/route/address/active status/limit/quota/private notes and the existing Edit customer action via `NativeInformationHeading`; edit becomes a secondary text action, not another competing filled page button. Its existing editor/save/leave guard is unchanged. Credit settings owns accepted terms details, activation/term editing, flag set/clear and review history; use neutral title “Credit settings & review”, with actions shown only to roles already authorized. The summary's accepted term is information, not an editor control.

This is a local reorganisation of the existing customer-detail family. It requires extracting presentation sections from `CustomerCreditPanel`; it does not authorize global menu/card/chrome restyling. Existing phase4 interaction contracts must be updated/checked where disclosure changes affect reachability. Flag state, uncertainty and pending attention cannot disappear merely to make a cleaner screenshot.

## B. Statement task: one preview and one clear next action

Within the unchanged host, use one scrollable body, in order:

1. Back to customer, then title **Statement** and customer name as supporting context. Back has a minimum48dp target. No second green band or large customer headline here.
2. One compact Period control showing preset and actual dates. Last7days default; This month; Custom dates open a native date-range dialog, not permanently visible date fields or a second chip row. The selected value is visibly labelled; long/large text wraps and grows. Picker cancel leaves the current period/snapshot unchanged.
3. Document preview, showing exactly the frozen document pages in a clean paper region, with shop identity and repeated page identity. The normal theme surrounds the document. Exported pages remain light paper in both themes; dark-mode UI labels/actions still use dark tokens. This is a deliberate document treatment, not a partial light-mode settings screen.
4. One filled primary action **Share statement** and a lower-emphasis outlined **Prepare reminder**. Actions are full width, stacked with8dp separation; do not squeeze two verbs side by side at large text. They live in the body after the preview, not in an added fixed bottom action bar. The existing navigation stays unobscured. For multi-page preview, display one page at a time with Page x of y and Previous/Next48dp targets; the file still contains every page. This avoids an endless stack of page thumbnails before the actions. A readable text equivalent of the selected page is available below a disclosure for accessibility; the thumbnail must not be the only way to read it.
5. Muted snapshot/as-of explanation, plus collapsed **Preparation history on this phone** when records exist. Do not show a permanent empty-history card.

Share opens one small native choice dialog: PDF (selected by default) / Images, explanatory file/page count, primary **Open share menu** and Cancel. Do not put separate PDF, PNG, WhatsApp and SMS buttons on the statement page. The Android chooser remains the recipient-selection surface for files. “Prepare reminder” navigates directly to the sibling Reminder review body pane.

Period change regenerates only the document area with its bounds preserved. Keep the previous preview dimmed with a progress overlay and actions disabled until the new snapshot is ready; never leave a visually current document paired with a changed date label. On failure, identify the attempted period/error and mark the old preview as previous; Retry and Revert restore a coherent selection. First load has a stable placeholder. Inaccessible/invalid data replaces the document region with a concise explanation and Retry, not a blank whole screen.

At 200% text or narrow widths the document preview can be shorter and actions may require ordinary vertical scrolling. Do not shrink text, crop currency or make the page horizontally scroll. Pinch/zoom is not required for this first scope: the page's text equivalent ensures full readable content; any image preview scale is labelled as a preview. Fit and generation limits remain explicit, never truncate financial rows silently.

## C. Reminder review: recipient first, then message, then handoff

Dedicated scroll body in the same Customers host:

1. Back to statement; **Prepare reminder**, customer context.
2. **Recipient** information section: selected customer, confirmed phone; quiet Change number action opens an inline labelled international-number field. It is not auto-focused. Override affects this draft only. Missing number shows the field and a clear instruction, with no surprise keyboard on entry.
3. **Message** section: a WhatsApp/SMS single-choice control, then a neutral read-only text preview. At large text use vertically stacked existing Selection rows instead of a cramped segmented bar. Default WhatsApp is a proposal for this shop's manual flow, not an availability claim; missing handler gives an inline fallback choice. Text can still be edited in the external app, so a second in-app message editor is unnecessary in this scope.
4. Current balance and as-of context appear in the message once; a separate historical statement amount is never used to request payment. If current facts change, disable handoff, explain “Balance changed — review the updated message”, replace the preview and require deliberate confirmation. Retain recipient override/channel. Settled/advance state shows its actual outcome and replaces the request with a no-reminder-needed explanation; retain the same action region rather than collapsing the screen abruptly.
5. One filled action **Open WhatsApp** or **Open SMS**, followed by the plain explanation “Review and send in the other app.” SMS also shows carrier-charge wording. Missing handler/invalid phone errors are next to the corresponding control/action; preserve the draft on failure/return.

No automatic launch, automatic send, success checkmark claiming delivery or disappearing confirmation. Returning from the other app keeps the reviewed draft with “Composer opened; sending is managed in the other app.” User cancellation outside this app is unknown. Local preparation history says what was observed and is reached from the statement task, not inserted into the financial ledger or terms history.

## Reuse matrix and exact fit rules

| Concern | Existing resource | Phase5 application |
|---|---|---|
| Palette/font | `SaafTheme`, bundled Manrope and `NativeVisualRoles` | Reuse light ivory/forest, dark forest, warm metric emphasis and supporting text roles. No new brand colors/font. |
| Grouped facts | `NativeInformationSection/Heading/Fact` | Summary, recipient and message; container owns16dp inset once. Heading actions reflow using existing measurements. |
| Menu entry | `NativeListStyle.Navigation` |72dp minimum,12dp radius,16dp horizontal/12dp vertical content inset,24dp icon/12dp gap, chevron. Height expands with content. |
| Spacing | Existing section/capsule rules |16dp page gutters;12–16dp inside related groups;24dp between different tasks;8dp between stacked actions. No card inside card merely to frame a button. |
| Type hierarchy | `nativeSectionTextStyle`, supporting/value/metric helpers | Section16sp/22 semibold; value16sp/24 semibold; support14sp/20 medium; money16sp/24 bold. Task title uses existing22sp titleLarge. No extra display-sized amount/hero card. |
| Chrome | `CashierScreenHeader`, inherited role scaffold | No dimensions, status/Open chip, footer items or inset changes. Short body titles do not resize the header. |
| Transition | `NativeSectionTransition` | Existing180ms content crossfade only for genuine pane identity. No size interpolation or whole-window recreation. Loading/format/channel changes do not remount chrome. Respect system animation scale. |
| Feedback | Existing busy/error/status/haptic patterns | Haptic on user action; saving/generation in context; duplicate-tap guard. No added animated success screen for opening a chooser. |
| Keyboard | Existing IME/inset ownership and leave guard | Only explicit recipient override invokes typing; one inset owner, no page-height animation. Back dismisses keyboard first, then returns through task stack. |

These are reuse/product choices, not external mandates for every metric. The body must scroll under existing chrome, and longer names/amounts must reflow instead of forcing exact card heights.

## Alternatives judged and bounded acceptance

Reject appending four export/channel buttons to customer detail: it spreads one workflow across an already dense profile. Reject a statement/reminder bottom tab: this is one customer's subtask. Reject a full multi-step modal sheet: recipient input, long document and error states deserve the existing body scroll/keyboard handling. Reject new internal Overview/Statements/History tabs: a single feature entry and purposeful disclosures are sufficient for this phase and avoid a new navigation layer.

The detail reorganisation is broader than merely adding one row, but directly necessary to place the new workflow without compounding existing duplication. It is bounded to this customer family. Scope does not include new Sales, global menu, Overview, general capsule or settings redesign.

Before finalizing the Compose implementation, compare these four body states: customer detail, ready statement, format choice and reminder review; then the same affected states at dark/200% text with long names, large amounts, missing phone, pending/held flags and generation error. Pin source/test names for only these components. Verify card padding/baselines, full money/identity, reachable actions, preserved history/actions, stable header/footer bounds, second-entry crossfade and IME opening/closing. Browser study is explanatory only; one connected phone provides the actual acceptance surface. No whole-app design audit or full test run.

Planning progress: ✅ source/design-material review; ✅ same-reference research; ✅ alternatives/judgment; ✅ exact placement/state/reflow plan; ✅ inspectable browser study. Compose and physical acceptance remain pending inside phase5's existing three pieces. No app/data/phone/cloud/customer-contact change in this planning run.
