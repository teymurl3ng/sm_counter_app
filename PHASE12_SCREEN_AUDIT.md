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

1. **Intermittent WebView paint completeness on state changes.** Several captured states retain container backgrounds while omitting some text/icons, header content, or the active bottom-navigation label. The most severe examples are `028-accounts-record-payment-top.png` and `045-customers-empty-search-result-top.png`; partial instances also occur in later cost/backup states. This reproduced with both the original screenshot API and compositor capture, then remained after WebView visual-state synchronization. It must be isolated on the physical device before treating later screenshots as pixel-stable.
2. **System-bar contrast and edge-to-edge insets.** The activity always requests light-colored status icons. They become faint on the cream authentication background, and long-page content scrolls underneath the transparent status bar while the icon mode remains suited to the green header. Bottom captures visibly clip content at the top edge.
3. **Daily-work task tabs do not fit the phone.** Seven actions share one horizontal strip. Later labels are clipped or off-screen, a scrollbar is visible, and the current task can be difficult to identify without swiping.

### P2 — address while applying the Phase 12 presentation system

1. **Forms are taller than necessary.** Large gaps and repeated full-width controls push primary actions several screens below the title, increasing work for frequent shop-floor entries.
2. **Secondary-area navigation loses context.** Products, customers, settings, backup, and other secondary areas all activate the generic `More` destination in the fixed bar. The screen header carries the real location, but it disappears during long-page scrolling.
3. **Dense content wraps awkwardly.** Some monetary summaries split `PKR` from the value, compact headings wrap onto three lines, and long explanatory copy competes with the immediate action.
4. **Long bottom sheets need stronger mobile structure.** Customer and employee entry sheets approach the full viewport and require internal scrolling; action placement and section grouping should remain obvious throughout.
5. **Small/low-contrast supporting text needs an accessibility pass.** The 11 px navigation labels, muted slate text, disabled controls, and status-bar icons require contrast and readable-size verification in checkpoint 12.4.

## Locked interpretation

- Phase 12.1 changed test/evidence infrastructure only; production UI and business behavior are untouched.
- The findings are presentation defects or risks. Native accounting, authorization, audit, backup, and recovery semantics remain outside this phase's UI edit boundary.
- Baseline screenshots are review evidence, not golden tests yet. Pixel-diff gating waits until the physical-device paint defect and the Phase 12.2 visual target are resolved.

## Verification

- `:app:compileDebugKotlin :app:compileDebugAndroidTestKotlin` — passed.
- Bridge contract — v4, 86 endpoints, SHA-256 `D22921E90040EF70CED8AFDE535C6309544B75D5EBD7B726ED1DE1273E0FA136`.
- `Phase12VisualBaselineCaptureTest` on Samsung SM-A065F / Android 16 — 1/1 passed.
- Evidence integrity — 81/81 PNGs readable at 720 × 1600; manifest contains the header, 81 capture rows, and completion row.

## Next gate

Checkpoint 12.2 will first isolate the P1 paint/inset behavior, then lock the visual target and presentation tokens before production screen-by-screen changes begin.

## Plain English

We photographed every important app screen and state on the real Samsung phone, using realistic shop data. The app is generally clear, but we found four practical problems to solve next: some WebView content can fail to paint after changing views, the phone status bar clashes with light/scrolled screens, the daily-work tabs are overcrowded, and common forms require too much scrolling.
