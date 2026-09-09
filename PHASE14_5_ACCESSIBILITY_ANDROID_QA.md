# Phase 14.5 — Accessibility, Adaptivity, and Android Behavior QA

Status: **COMPLETE — targeted physical-device audit finished; fixes are assigned to Phase 14.7**

Date: 2026-09-09

Devices:

- Samsung SM-A065F (A06), Android 16 — primary accessibility, rotation, landscape, keyboard, Back, lifecycle, and picker checks.
- Samsung SM-S921W (S24), Android 16 — split-screen/device-class check.

Package boundary: only the isolated `com.saaf.milkshop.qa` installation was used for destructive or draft-state checks. Ordinary Saaf installations/profiles were not changed.

## Outcome

The focused native audit is complete. Normal-size operation, TalkBack announcements, app switching/locking, split-screen, system picker cancellation, and the locked light presentation remain usable. Six verified UI/runtime defects are recorded for Phase 14.7: five P1 major issues and one P2 minor issue. There are no P0 blockers and no P3-only polish findings.

This is an internal product audit for a small-business application, not an external accessibility certification.

## Native audit score

| Dimension | Score | Evidence |
| --- | ---: | --- |
| Accessibility | 1/4 | Controls and fields are generally announced clearly, but large text breaks the interface and TalkBack cannot activate a dialog close control. |
| Performance | 3/4 | The accepted Phase 12.5 startup/navigation measurements remain the baseline and no new responsiveness failure appeared; performance was not rebenchmarked in this checkpoint. |
| Appearance and theming | 3/4 | The current light theme remains coherent and stable when system dark mode changes. A true dark theme is outside the locked Phase 14 scope. |
| Platform conformance | 2/4 | Picker cancellation, app switching, locking, and the first keyboard-closing Back action pass; IME avoidance and safe draft navigation do not. |
| Adaptivity | 2/4 | S24 split-screen is usable, but A06 landscape needs refinement and large text/rotation expose serious failures. |
| **Total** | **11/20 — Acceptable, with major fixes required** | Release acceptance remains open until the P1 findings are fixed and retested. |

Platform-conformance verdict: **FAIL for final release acceptance**. The application is not generally an unadapted web port, but essential Android behavior fails around the keyboard, Back, rotation, and one TalkBack action.

## Live check matrix

| Check | Result | Observation |
| --- | --- | --- |
| Increased system text size | Fail | Navigation, buttons, and text overlap across the app; many screens become uncomfortable or impractical. |
| A06 landscape | Partial | Mostly usable, but Settings and other layouts are awkward and need broad refinement. |
| Keyboard/IME | Fail | The keyboard obscures content across screens; the page does not reliably move or focus the active field. |
| Android Back with an unfinished form | Fail | First Back closes the keyboard; the next Back returns to Dashboard and silently loses the draft. |
| TalkBack announcements | Pass | Buttons and fields were announced clearly. |
| TalkBack dialog close | Fail | Focusing and double-tapping the X did not close the dialog and caused a visual glitch; Android Back remained an escape. |
| App switch and lock/unlock | Pass | Current screen and unfinished input were retained. |
| Rotation | Fail | Unfinished input was discarded. |
| S24 split-screen | Pass | Smaller and less comfortable, but usable without a blocking layout failure. |
| System dark mode | Pass within scope | The app intentionally remains on its locked light theme; no partial-theme breakage was reported. |
| Android document picker cancellation | Pass | Back returned to the app, reported that no file was changed, cleared both passphrase fields, and did not freeze or alter the session. |

## Verified findings

### F14.5-01 — Systemic large-text reflow failure

- Severity: **P1 major**
- Evidence: increased Android text size causes navigation, buttons, and text to overlap across many screens.
- Required direction: make navigation and content containers reflow or scroll at large font scales; do not solve this by clipping or shrinking accessible text.

### F14.5-02 — Landscape layout needs systemic refinement

- Severity: **P2 minor**
- Evidence: A06 landscape remains mostly usable, but Settings and other layouts are poorly arranged and uncomfortable.
- Required direction: improve compact-height spacing, scrolling, action placement, and navigation without inventing a separate product layout.

### F14.5-03 — Keyboard obscures active content across the app

- Severity: **P1 major**
- Evidence: opening the keyboard hides content and the active field does not reliably move into view. The existing viewport/focus handling therefore does not work on the tested physical device.
- Required direction: fix Android window-inset and focused-field visibility behavior at the shell level, then verify representative forms instead of applying isolated screen workarounds.

### F14.5-04 — Android Back silently discards unfinished drafts

- Severity: **P1 major**
- Evidence: the first Back closes the keyboard, while a second Back navigates to Dashboard from a form and loses entered values without warning.
- Required direction: preserve the draft or present a clear discard confirmation before leaving; Back should return through the user's navigation history rather than unexpectedly jumping to Dashboard.

### F14.5-05 — TalkBack cannot activate a dialog close control

- Severity: **P1 major**
- Evidence: the X is announced, but TalkBack activation does not close the dialog and produces a visual glitch. Android Back can still dismiss it.
- Required direction: use a reliable semantic button/click target with correct focus and dismissal behavior, then retest by swipe focus and double-tap activation.

### F14.5-06 — Rotation discards unfinished input

- Severity: **P1 major**
- Evidence: app switching and lock/unlock retain the draft, but rotating the device removes it.
- Required direction: preserve presentation drafts across Activity/WebView recreation without moving business authority or durable business state into React.

## Positive evidence and boundaries

- TalkBack announced the remaining tested buttons and fields clearly.
- App switching and lock/unlock retained the active state.
- S24 split-screen remained usable.
- Cancelling the Android document picker was safe and cleared sensitive passphrase inputs.
- The app's lack of a dark theme is an explicit current design boundary, not a Phase 14.5 defect. Partial or automatic dark-theme work must not be added during defect closure.
- Existing automated responsive, inset, lifecycle, and navigation tests remain valuable regression checks, but they did not catch these live interaction failures and cannot close them by themselves.

## Handoff

Phase 14.6 is next: implement and physically verify mandatory native device-bound Quick unlock. Phase 14.7 then fixes these six findings together with the five earlier live cloud/recovery findings and runs the expanded complete regression. The large-text, keyboard, Back/draft, TalkBack-close, and rotation findings are release-acceptance requirements.

## Plain English

The app works normally in several Android situations, and cancelling the backup file picker is safe. However, large text breaks layouts, the keyboard covers forms, Back and rotation can erase unfinished work, and TalkBack cannot close one dialog. These issues are now formally recorded for the final fix phase.
