# Phase 12.3 — Navigation and Natural Task Flows

Status: **COMPLETE — 2026-09-05**

## Scope and evidence

Checkpoint 12.3 addresses the two navigation findings recorded in `PHASE12_SCREEN_AUDIT.md`: seven Daily work tasks did not fit the Samsung screen, and a secondary destination appeared only as generic `More` in the fixed navigation. It also reconciles the hybrid app's opening area, report-period default, and presentation-preference boundary.

The decisions use the proportional product policy in `PRODUCT_SCOPE.md`, the presentation rules in `DESIGN.md`, the Phase 12.1/12.2 physical-device evidence, and the references below. No external design skill certified this checkpoint.

| Source or evidence | Finding or principle | Phase 12.3 decision |
| --- | --- | --- |
| Samsung SM-A065F screenshots | The seven-item horizontal strip clipped later task labels and required discovery by swiping. A permanently expanded replacement made the work form too slow to reach. | Show one compact current-task control; open all authorized tasks in a non-scrolling, two-column bottom sheet. |
| Android core app quality | Navigation should remain consistent and intuitive; touch targets should be at least 48 dp; representative physical devices should be used for acceptance. | Retain stable bottom navigation, use 48 px minimum task targets, preserve Android Back, and verify the complete packaged UI on the Samsung. |
| Nielsen usability heuristics | Keep system status visible, prefer recognition to recall, preserve consistency, and match the user's language. | Show the actual secondary area name and icon in the bottom bar; show the selected Daily work task on the closed chooser; retain plain task verbs and existing area names. |
| WCAG 2.2 and `DESIGN.md` | Current state must not depend on color alone; controls need accessible names, state, focus, and sufficient target size. | Pair selection color with icon, label, `aria-pressed`/`aria-current`, `aria-expanded`, and dialog semantics; keep every chooser action keyboard-focusable only while visible. |
| `PRODUCT_SCOPE.md` | Optimize for clear daily work in an owner-managed business, not corporate-ERP density or feature parity. | Use one small chooser and grouped overflow rather than a larger permanent navigation hierarchy. No new feature or record type was added. |

Primary references: [Android core app quality](https://developer.android.com/develop/adaptive-apps/quality-guidelines/core-app-quality), [Nielsen's usability heuristics](https://www.nngroup.com/articles/ten-usability-heuristics/), and [WCAG 2.2](https://www.w3.org/TR/WCAG22/).

## Implemented navigation

- The normal Daily work screen now shows a compact `Current task` control with the selected task's icon and label.
- `Change` opens a bottom sheet containing every task authorized for the signed-in account in a two-column grid. No horizontal scrolling or clipped task label remains.
- Selecting a task closes the sheet, updates the current-task control, returns to the top, and renders the existing form and native-backed action.
- Android Back closes the task chooser before performing area-level navigation.
- Existing task IDs remain unchanged, preserving the completed transaction test paths.
- When a secondary area is active, the fifth bottom-navigation destination shows that area's actual icon and label—such as `Customers`, `Cost & plans`, or `Backup`—instead of only `More`.
- Tapping that destination still opens the complete permission-aware grouped area chooser.
- Bottom-navigation labels now meet the presentation system's 12 px minimum and may wrap instead of being truncated.

## Opening area, report period, and preferences

- Owner, administrator, and manager permission bundles open on Dashboard because they can view both operational and full financial information.
- Cashier and operator bundles open on Daily work because their primary purpose is recording shop activity.
- A valid area restored during secure Activity recreation still takes precedence over the default.
- Dashboard reporting remains `Today` by default. The visible From/Through controls continue to support any valid period. No evidence currently justifies silently defaulting an operational overview to a month.
- Phase 12 retains the locked light theme, comfortable density, and current English interface. The legacy non-exported native screen's saved theme/language/density/opening/report settings are not presented as working hybrid preferences because bridge v4 has no preference contract.
- A future saved preference requires an explicit user need and an approved native-backed additive contract. Phase 12.3 does not create browser-local or misleading persistence.

## Verification

- TypeScript no-emit lint — passed.
- Production Vite build and Android asset packaging — passed: 1,897 modules.
- Bridge contract — unchanged at v4, 86 endpoints, SHA-256 `D22921E90040EF70CED8AFDE535C6309544B75D5EBD7B726ED1DE1273E0FA136`.
- Android/Kotlin and instrumentation compilation — passed.
- `Phase12NavigationFlowTest` plus the retained `Phase9NavigationRuntimeTest` — 3/3 passed on Samsung SM-A065F / Android 16.
- Device proof covers role-appropriate owner opening, real secondary-area identity, grouped overflow, task chooser layout, all seven authorized task selections, and Android Back.
- Full `Phase12VisualBaselineCaptureTest` — 1/1 passed and captured 82 screenshots across 58 named states.
- Evidence: `phase12-evidence/sm-a065f-android16-phase12-3-navigation/`; 82/82 PNGs are readable at 720 × 1600. The 84-line manifest SHA-256 is `8A0740D95DD5713C7CD9CB752A62E721D51F49173C731D1A446987C7E0C28821`.
- The last complete isolated-QA regression remains 247/247 from Phase 11. Checkpoint 12.6 owns the next complete-suite rerun.

## Plain English

Daily work no longer makes the user swipe through a cramped row of seven choices. The current job is always named, and tapping Change shows every permitted task in one clear sheet. When the user visits a secondary area, the bottom bar now says where they actually are instead of merely highlighting More. Owners start with an overview; task-focused staff start where they record work. Calculations, permissions, and saved records are unchanged.
