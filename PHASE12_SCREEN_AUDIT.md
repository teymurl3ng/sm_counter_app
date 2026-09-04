# Phase 12.1 — Physical-Device Screen Audit

Status: **COMPLETE — 2026-09-04**

## Baseline

- Device: Samsung SM-A065F, Android 16, 720 × 1600.
- Installation: isolated `com.saaf.milkshop.qa` identity with representative native-backed shop data.
- Evidence: `phase12-evidence/sm-a065f-android16-render-synced/`.
- Inventory: 81 PNG screenshots covering 57 named UI states, plus `manifest.tsv`.
- Manifest SHA-256: `1C81401D6B8E25904A788B23B506CADDE8D052B0B0597D89894014FB86C715AC`.
- Privacy: the one-time owner recovery code was replaced with review-safe text before capture.
- Capture reliability: each frame waits for WebView's visual-state callback and is then saved through Android's compositor-level `screencap` command.

## Coverage matrix

| Area | Named states | Screenshots |
| --- | ---: | ---: |
| Authentication and recovery | 5 | 5 |
| Dashboard and navigation | 2 | 3 |
| Daily work | 7 | 14 |
| Deliveries | 1 | 2 |
| Accounts | 4 | 5 |
| History and correction | 3 | 4 |
| Cash close | 1 | 2 |
| Products | 4 | 4 |
| Customers | 5 | 6 |
| Suppliers | 4 | 4 |
| Employees and attendance | 5 | 5 |
| Costing and planning | 4 | 7 |
| Settings | 5 | 8 |
| Access and security | 4 | 7 |
| Backup and restore | 3 | 5 |
| **Total** | **57** | **81** |

Long pages and tall panels have separate top and bottom captures. Dialogs, empty results, inactive records, failed login, ledger/detail views, correction, and backup modes are included.

## Findings

There is no Phase 12.1 P0 finding: the review exposed no saved secret, destructive action, or missing native record.

### P1 — fix before broad visual polish

1. **Apparent WebView paint completeness on state changes — resolved as a review-preview artifact in 12.2.** Some scaled previews of tall screenshots appeared to retain containers while omitting text/icons. Original-size pixel inspection and exact crops showed the supposedly missing brand, header, navigation, and body glyphs in the saved PNGs. A second PixelCopy and visual-state-synchronized 81-image set confirms the source evidence, while a 60-transition normal-use device check shows no corresponding app failure. Reviewers must verify the source file or a crop before filing this visual symptom as a product defect.
2. **System-bar contrast and edge-to-edge insets — resolved in 12.2.** The activity now protects the transparent top inset with the exact header color while retaining edge-to-edge behavior and readable white status icons. The physical-device system-inset and presentation checks pass together.
3. **Daily-work task tabs do not fit the phone — assigned to 12.3.** Seven actions share one horizontal strip. Later labels are clipped or off-screen, a scrollbar is visible, and the current task can be difficult to identify without swiping.

### P2 — address while applying the Phase 12 presentation system

1. **Forms are taller than necessary.** Large gaps and repeated full-width controls push primary actions several screens below the title, increasing work for frequent shop-floor entries.
2. **Secondary-area navigation loses context.** Products, customers, settings, backup, and other secondary areas all activate the generic `More` destination in the fixed bar. The screen header carries the real location, but it disappears during long-page scrolling.
3. **Dense content wraps awkwardly.** Some monetary summaries split `PKR` from the value, compact headings wrap onto three lines, and long explanatory copy competes with the immediate action.
4. **Long bottom sheets need stronger mobile structure.** Customer and employee entry sheets approach the full viewport and require internal scrolling; action placement and section grouping should remain obvious throughout.
5. **Small/low-contrast supporting text needs an accessibility pass.** The 11 px navigation labels, muted slate text, disabled controls, and status-bar icons require contrast and readable-size verification in checkpoint 12.4.

## Locked interpretation

- Phase 12.1 changed test/evidence infrastructure only; production UI and business behavior are untouched.
- The findings are presentation defects or risks. Native accounting, authorization, audit, backup, and recovery semantics remain outside this phase's UI edit boundary.
- Baseline screenshots are review evidence, not golden tests yet. The apparent paint defect was isolated to scaled previewing, and the Phase 12.2 visual target is now locked; screen-level golden-test policy can be decided after 12.4 completes the intended visual changes.

## Verification

- `:app:compileDebugKotlin :app:compileDebugAndroidTestKotlin` — passed.
- Bridge contract — v4, 86 endpoints, SHA-256 `D22921E90040EF70CED8AFDE535C6309544B75D5EBD7B726ED1DE1273E0FA136`.
- `Phase12VisualBaselineCaptureTest` on Samsung SM-A065F / Android 16 — 1/1 passed.
- Evidence integrity — 81/81 PNGs readable at 720 × 1600; manifest contains the header, 81 capture rows, and completion row.

## Next gate

Checkpoint 12.2 resolved the paint/inset questions and locked `DESIGN.md`. Checkpoint 12.3 now addresses navigation and natural task flow, beginning with Daily work and secondary destinations under `More`.

## Plain English

We photographed every important app screen and state on the real Samsung phone, using realistic shop data. The text that seemed to disappear was actually present in the files—the tall-image preview was misleading. The phone-level status-bar foundation is now corrected. The next real usability problem is navigation, especially the overcrowded Daily work choices and secondary screens hidden under `More`; form density and accessibility follow in 12.4.
