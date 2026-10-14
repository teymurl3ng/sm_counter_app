# Recovery label, title position and Quick PIN entry - scoped plan

14 October 2026. Planning and source diagnosis only. No app-code change, build, installation, functional test or release action in this pass. The independent-judgment prompt hook applies; conclusions below separate verified source behavior from an unverified visual cause.

Baseline: pushed app commit 3579bf5cfc49bbc7911f340ad2aa4a4f4ce05d04; regular signed 0.4.2/code 6 installed on A06 and S24. Installation notes and seven-batch plan already contain local updates. Preserve these and all existing app data.

The user clarified two points: move title text lower inside the existing top section (do not shorten the section); the Quick PIN dark shape appears when its screen opens (not on digit taps or successful unlock).

## Causes and confidence

| Item | Verified source behavior | Conclusion |
|---|---|---|
| Recovery's small Your shop | ShopIdentityProvider starts/falls back to Your shop. NativeAuthenticationHost owns AccessFrame, whose toolbar always renders LocalShopName. RecoveryCodeScreen's nested frame is content-only, so that outer merchant label remains visible. | Confirmed source of the unwanted label. It is a display choice, not evidence of a lost shop name or credential defect. |
| Signed-in titles too close to the top | All five role hosts use CashierScreenHeader, which delegates to the same NativeChromeBand. The title is centered in a font-aware row immediately after the status-bar inset, with no extra downward placement adjustment. | Confirmed common placement mechanism; the requested change is a design preference. No evidence that status-bar insets themselves are wrong. |
| Quick PIN entry jitter | AccessFrame scrolls to zero when the step title changes. NativeAuthenticationHost clears focus on state-class changes. Its NativeSectionTransition also clears focus and hides the software keyboard. QuickPinKeypad then requests focus after one frame; the host still uses animated IME padding. | A plausible focus/scroll/IME timing interaction. There is no editable field or text-input session in the custom keypad, so its focus request alone does not establish that a new Android keyboard opens. |
| Brief dark shape at PIN entry | Access-step bodies crossfade together for 180 ms, retaining the outgoing picker/passphrase body. The access Surface remains mounted. The native light window background is light; the dark window background matches the dark page. | Outgoing-content/ripple overlap or residual IME are reasonable alternatives. The exact shape's source is not yet established without an entry-frame/inset trace. Do not claim a proven keyboard bug or a proven theme flash. |

Existing NativeQuickPinContractTest starts directly on Quick PIN, checks touch/hardware/busy behavior and no IME at idle, and checks geometry. That evidence does not inspect the first frames of account-picker/passphrase-to-PIN navigation. Do not broaden testing to unrelated sign-in/biometric/server flows.

## Smallest proposed fix

1. **Recovery label:** make the outer access frame omit the merchant label for RecoveryCode only. Keep the reserved access toolbar geometry so this deletion does not cause another whole-page jump. Keep the recovery heading, code, warning, acknowledgement and one-time security behavior. Do not remove shop identity from the rest of the app, replace it with invented branding or redesign the recovery flow.
2. **All signed-in title positions:** use one shared downward title-placement adjustment in NativeChromeBand, initially 8 dp within the existing title row. Keep the band height, status inset, metadata rows, day-status chip, bottom navigation and body start fixed. Verify font-aware bounds before finalizing; do not add outer padding or individual per-screen offsets. This applies to every native role-hosted green/top section. The neutral access/recovery and welcome headings are separate layouts, not a reason to add green headers back.
3. **Quick PIN opening:** first capture only the relevant entry frames and IME/focus/scroll geometry, using a disposable or already safe QA account and no credential submission. Compare picker-to-PIN with return from a passphrase field while its IME is visible. Then change only the established mechanism: coordinate access-step focus/scroll handling if timing is implicated, or use a localized access transition if outgoing overlap is implicated. Retain hardware-key support and normal screen-reader focus. Do not remove keypad focus blindly, add arbitrary timeouts, change PIN length/rate limits, replace the keypad, disable animation app-wide or reopen the shared five-role keyboard repair.

**Judgment:** accept the first two narrowly defined changes. For Quick PIN, accept the bounded entry diagnosis and require its result before selecting a code fix. The timing observation rules out an ordinary digit-press effect as the main explanation, but does not distinguish old-page overlap from IME/focus movement. No reason to change shared business screens, auth storage or the whole palette.

## File boundary and stop rule

- Expected production edits: NativeAuthenticationScreens.kt and NativeChrome.kt.
- Quick PIN: at most QuickPinKeypad.kt plus an access-local transition/helper if the trace warrants it. NativeAsyncUi.kt, NativePreviewActivity.kt, authentication view model, theme and activity window settings are inspection references; generic transitions and keyboard policy are not automatic edit targets.
- Preserve application/domain/database/sync/server logic, shop profile data, actual accounts, recovery-code secrecy, PIN buffers, biometric prompt and existing signing identity.
- If an unrelated problem appears, note it and stop expanding this run. The scope remains these three complaints inside finish step 7; no eighth finish batch.
- No screenshot/log of an actual recovery code or typed secret is required. No live account creation, PIN enrollment, real sign-in or business submission belongs to the diagnostic capture.

## Verification to schedule with implementation

Affected Kotlin compilation; existing header geometry at normal/larger text; recovery label absent with code/warning/action retained; a short PIN entry trace covering both entry paths with stationary keypad anchors, no unintended IME appearance and no transient old-page shape. Reuse the existing keypad hardware/busy/fallback controls if focus changes. One phone first, then a brief geometry/motion comparison on the other; no full app suite.

Prior deferred functional acceptance remains deferred. This pass does not execute the diagnostic/acceptance plan or change the installed app.

## Primary guidance

- [Android Compose focus requests](https://developer.android.com/develop/ui/compose/touch-input/focus/request-focus): request focus from an event/effect; the existing effect is not inherently invalid. Coordinate its ownership rather than labelling all programmatic focus a defect.
- [Android Compose insets](https://developer.android.com/develop/ui/compose/system/insets-ui): standard inset modifiers animate with the IME and consume existing spacing. A no-text-field step reacting to a still-closing IME is a possible entry timing issue, not grounds for a new global height workaround. The proposed 8 dp title adjustment is our design choice, not an Android-mandated measurement.
