# Proposed UX repair and product scope

Date: 10 October 2026. Status: **proposal for discussion, not accepted implementation scope**.

Based on [the source audit and current external references](UX_AUDIT_2026-10-10.md). No application code or permissions were changed by the audit. Physical flashing/clipping reproduction remains an explicit prerequisite, not a completed check.

## Proposed work packages

| Package | Result | Boundary | Acceptance |
|---|---|---|---|
| 0. Matching runtime baseline | A06 recordings of every route and key state, with exact build provenance | QA package and disposable fixtures only; source defects remain separate from filmed symptoms | Complete route/state matrix, first/second/tenth entry, interrupted transitions, keyboard recovery flow |
| 1. Navigation and insets | Stable shell, correct current route after refresh, no blank/duplicate pane, readable recovery header | UI/VM state ownership; preserve domain authorization and draft guards | Reproduce original failing scenarios, then repeat with no pane reset/clip; failure and reduced-motion checks |
| 2. Shared interaction foundation | Readable lists, defined semantic feedback/haptics, reusable result transitions | Existing green/ivory/yellow visual direction can be retained; don't replace the app's identity without a product decision | Products/Employees/Records comparisons; contrast measurements; selection, save, fail, dismiss, disabled states |
| 3. App-wide adoption | Every native screen family uses the accepted interaction rules | All inventoried screens/hosts and supporting components; not just the dashboard examples | Every applicable matrix row has evidence and review status; shared helper and dependent-screen gates |
| 4. Shop identity | Configurable business name consistently displayed and refreshed | Trusted business read model and visible copy/outputs; actor identity separate | Rename reflected without restarting; long/Urdu names; first-run/restore; no technical namespace changes |
| 5. Catalog-backed selling | Products no longer disappear or inherit rates due to their names; picker scales | Native sale/reference facade plus UI; explicit price/stock/unit capability contract | 0/1/2/3/12/50 products, archived/unpriced/renamed variants, correct saved product ID/rate/quantity |
| 6. Daily work access | Clerk can work and perform the approved count/close workflow without owner presence | Reconcile native permissions, visible actions, scope of date lock, and business policy | Actual Cashier session tests; pending deliveries, variance, already-closed day, next date, audit actor, denied reopen |
| 7. General-purpose expansion, if selected | Supported new retail product families/units with truthful inventory/reporting | Separate domain/data/migration work; do not hide dairy assumptions by changing labels | Explicit family contracts and end-to-end purchase/sale/stock/correction/costing/report/backup/restore proof |

Packages 1–3 form one app-wide repair, delivered incrementally. Identity can follow the stable scaffold. Catalog and day access have separate capability boundaries. General-purpose accounting/stock expansion must not be bundled invisibly into animation work.

## Interaction contract to approve

### Navigation and loading

- Keep system bars, business/actor context, and primary navigation stable where the task stays in the same shell. Auth can use a shared stable frame instead of fading the header on every step.
- Give one component ownership of each route change. Nested animation is allowed for a distinct local change, not two effects animating the same navigation.
- Identify the route by role/section/destination. Data refresh, busy/error, and price updates must not change route identity.
- Render outgoing panes from their own valid snapshot for their full exit lifetime. Do not clear detail data while its pane is still exiting.
- Merge refreshed context into the latest UI state after checking the active session/request. Preserve newer navigation, drafts, selection, receipt, and delivery follow-up. Apply the same rule on soft failure.
- Cold loading may show a loading state. Warm loading keeps existing content and provides unobtrusive refresh status. Cold Loading → Ready must not replay the route entrance.
- Preserve Back/discard/saving behavior and once-only navigation. Repeated taps and interrupted animations must not register multiple guards or allow the wrong pane to receive a write.
- Honor Android animation settings, including zero scale. Check actual behavior; the unused duration helper in the current source does not establish compliance.

Motion timings and geometry should be calibrated on the A06, using approximately 160–240 ms as a starting design proposal for small transitions, not a mandatory industry number. Use predictable forward/back transitions for hierarchical steps and restrained peer-tab transitions. Avoid animating every table row on each refresh. Do not simulate a longer operation to make it feel more substantial.

### Feedback

| Situation | Proposed presentation | Exit rule |
|---|---|---|
| Button/selection accepted | Ripple/pressed/selected state, light semantic haptic if enabled | Immediate, no full-screen fade |
| Save/write in progress | Relevant button says what is happening, busy state prevents duplicate submit, inputs retain context | Actual completion/failure |
| Routine settings/catalog save | Accessible notification that names the saved thing; refresh affected content | Normal accessible notification timeout/dismissal |
| Sale/payment/cash close/important operation saved | Receipt/result with identifiers, amounts and next action | Explicit Done/Next; no four-second deletion of receipt state |
| Validation failure | Inline message at the relevant field; clear correction; no success haptic | Correct input or explicit dismissal where appropriate |
| Save failure | Persistent useful error, draft preserved, safe retry semantics | Successful retry or intentional exit |
| Saved but refresh failed | Say the record was saved and display failed to refresh; reference/retry refresh | Do not invite another transaction submission |
| Sync pending/offline | Separate local-save status and sync status | Actual sync outcome; offline is not local-save failure |
| Destructive/sensitive operation | Consequence-specific confirmation where warranted; busy/failed/succeeded state | Graceful dismissal after result or explicit cancellation |

Shared result semantics should drive visual feedback, announcements, and success haptics. Results need stable outcome IDs/consumption so navigation, rotation, or refresh does not repeat a success vibration or lose the message before it can be read. Material components can own their normal dialog/sheet animation; custom dismissal state is justified only if default behavior does not meet the verified requirement. Avoid wrapping a dialog in a second unrelated page animation.

### List readability and haptics

Use at least 4.5:1 for normal enabled text on its actual background; aim above the minimum to account for opacity and thin strokes. Review dark/light, selected/pressed/error, and large text separately. Distinguish the list group or item surface from the page, give important amounts a consistent location, and keep metadata readable. Every row needs clear interactive affordance and a full accessible touch target. Cards, dividers, and spacing are alternatives selected by density, not mandatory decorations.

Expected haptics should be recorded by interaction type for every inventory control. Navigation/selection/commit/success/reject/destructive actions need consistent treatment where feedback is justified. Ordinary typing and continuous scrolling do not need added vibrations. Respect user/system disablement; quality and device support matter more than forcing a buzz. Record whether missing feedback comes from an unwired control, disabled preference, or unsupported device. Haptics never substitute for a visible/accessible result.

## Product and day-policy decisions

### Shop identity

Recommend using the configured shop name as the business identity throughout business-facing UI, receipts and applicable outputs, with neutral first-run copy. Keep the signed-in staff name visibly separate. Choose a neutral installed product/launcher name independently; changing business settings should not rename packages, security domains, or identifiers. This is a display/read-model repair, not a data-key migration.

### Initial product breadth

Recommend fixing the catalog-to-sale contract first: product ID, its configured default price, active status, unit, and inventory capability. Keep Milk/Yogurt historical pricing behavior explicit where required for migrated records, rather than matching prefixes. Define what an unpriced product does before exposing it for sale.

Two viable scope levels need a business choice:

1. **Existing supported units first:** sell a broader small catalog in Liters/Kg, while dairy-specific production and costing retain honest boundaries. This solves disappearing products without claiming universal retail coverage.
2. **General retail next:** add a count unit (piece/unit) and selected purchased stock products, with sourcing, quantity precision, costing/reporting, correction, migration, sync and recovery decisions. This is a separate domain expansion and may require a schema change only where the existing representation cannot support it.

A multi-product cart, barcodes, variants, taxes, refunds, services, bundles, recipes and expiry are not implied by repairing the selector. Add them when explicitly required for the intended first shop types.

### Cashier autonomy

Recommend routine counter operation without an owner present, using the clerk's own account. The current system already normally opens a new date by accepting posting, so adding a mandatory opening screen is a new workflow choice, not necessary to unblock sales.

For the current single-shop model, the smallest path is to expose the existing Cashier count-and-close authority with clear scope, rather than promoting the clerk to Admin. Before locking, show the date, any earlier unclosed dates affected, pending-delivery blockers, counted/expected cash where permitted, and variance-reason requirements. Attribute count and close to the authenticated person. Keep Owner/Admin reopen with reason/full authentication, subject to existing account gates.

If the owner wants some cashiers to count but not close, split count/close permission or add explicit delegation with native enforcement and migration-safe defaults. That policy change should be agreed before implementation. An approval flow requiring an absent owner's live PIN would not solve the stated problem.

If multiple shifts/devices need independent opening/closing, introduce register sessions separately: opening count/float, actor/device, cash movements, final count/variance, handover, and manager review. Session closure must not lock other active staff out of a whole business date. Do not implement this by merely relabelling daily close.

## Complete physical acceptance matrix

The matrix applies to every screen/host in [screen_inventory.tsv](audit/2026-10-10/ux/screen_inventory.tsv), each routed pane, and every exposed overlay/form/detail state. Wrapper files share child evidence only when the role-specific entry path is also covered. Mark a condition N/A only with a reason; do not silently skip read-only screens.

| Axis | Required scenarios |
|---|---|
| Re-entry | Cold first open; second open; ten open/back cycles; after a successful write; after failed refresh |
| Interruption | Open/back before enter completes; double tap; switch tabs during refresh; refresh success/failure while destination changes; session expiry mid-transition |
| Layout | Portrait, landscape, large text, light/dark; status/navigation bars and cutout; keyboard opening/closing; long names; empty and populated lists |
| Motion | System animator scale 0, 1 and a slow value; gesture/hardware Back; modal open/confirm/cancel/error/dismiss; no blank full-content frames, unexpected duplicate panes or residual overlays |
| State | Scroll/selection/draft retained on warm entry; leaving dirty draft prompts; saving blocks duplicate writes/unsafe leave; soft refresh cannot restore old navigation |
| Haptics | Every defined semantic interaction, on supported A06 and a second device; touch haptics disabled; no repeated success on re-entry/rotation; actual feel evaluated, not just an API-call assertion |
| Outcomes | Success and failure for each write family; saved-with-refresh-failure; no silent form disappearance; receipt stays readable; announcement and focus behavior with TalkBack |
| Capability | Product fixtures 0/1/2/3/12/50; missing price; archive/rename; shop rename; Cashier count/close; denied reopen; next business date and prior-date locks |

Use timestamped recordings and representative screenshots tied to exact APK SHA-256, source revision/working-tree snapshot, device/Android build, route and scenario. Record transition frames for flashing; an idle screenshot cannot prove motion. Screen recordings likewise cannot prove physical haptics. Redact disposable secrets before evidence is retained.

A defect is closed only when its original reproduction and the affected role/path family pass. Perceptual review and domain proof are complementary. Functional tests passing or all control rows marked COVERED cannot replace the motion/readability review.

## Verification and completion gate

Implementation must follow `NATIVE_INTERACTION_CONTRACT.md` and `NATIVE_SCREEN_ACCEPTANCE.md`: qaFast after changes; affected qaScreen gates; qaCheckpoint for shared helpers and dependents; screenshot review statuses and untested counts recorded. Money/permission/catalog expansions additionally need meaningful domain/integration tests, saved-record proof, and migration/recovery coverage where impacted. Do not manufacture tests that merely mirror animation constants.

Run tests in the isolated QA package on the designated device. Preserve the ordinary app and existing source changes. Use the protected-logic exception only for separately accepted capability work, and list every affected protected file. The current dirty tree contains pre-existing protected changes; a gate failure from them must not be described as a UX-document regression.

No package in this proposal is considered implemented or accepted. Scope discussion should settle initial product breadth, whether Cashier closes the whole day or only submits a count, and whether explicit register sessions are actually needed. The evidence supports repairing shared foundations first, then applying them across the full screen/state matrix.
