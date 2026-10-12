# Frozen scope: account Quick PIN keypad

Planning only. No application implementation authorized by this document itself. SOURCE_BASELINE.json is the source checkpoint; BASELINE.md states observed behavior. This document makes the earlier proposal concrete.

## Target navigation

| Account configuration/action | Destination |
|---|---|
| PIN enabled, with or without phone security | Quick PIN keypad directly |
| Phone security only | Existing method chooser |
| Neither quick method enabled | Existing passphrase page |
| Keypad: Use full passphrase | Existing passphrase page; its Back returns to keypad |
| Keypad: Phone security | Android prompt on explicit tap; cancel/failure returns to the same selected account's keypad |
| Keypad: toolbar or Android Back | Account list; if switching, preserve pending original session |
| Account list Back during Switch account | Restore original session, as today |
| Choose current account during Switch account | Restore original session, as today |

No saved preferred-method setting and no automatic biometric prompt. Phone-only accounts keep their existing passphrase Back route to the chooser.

## Proposed visual specification

Reuse the neutral AccessFrame; do not modify global header/footer dimensions. Body uses existing 16 dp gutters and scroll behavior.

Order: Quick PIN heading → short instruction → account identity → masked display → keypad → Unlock → feedback → alternative methods.

- Heading: existing 24 sp / 32 sp semibold. Instruction: “Enter your account PIN.”, 14 sp / 20 sp. Identity retains name, role and login once; long names wrap.
- Keypad and Unlock centered at maximum 320 dp width; shrink to available width. Keys: three equal columns, minimum 64 dp height, 8 dp row/column gaps, 12 dp corner radius. Key text: 24 sp / 32 sp. Keys grow vertically for large text.
- Use the theme's surface/onSurface tokens and a subtle outline for number keys; visible pressed state and existing tap haptics. Unlock uses the existing primary-action treatment. No new palette, illustrations, capsules or global style changes.
- Rows: 1 2 3 / 4 5 6 / 7 8 9 / Clear 0 Backspace. Clear and Backspace have explicit accessible labels. Empty deletion/clear produces no action or haptic. No randomized digit positions.
- Masked display: entered dots only, maximum 12; proposed 10 dp dots with 6 dp gaps. Reserve a stable display height; never show digit values or force four fixed slots. No field border or flashing caret.
- Unlock remains explicitly tappable for 4–12 digits, minimum 56 dp height; label changes to “Checking PIN…” with no height change. Never submit from digit taps.
- Feedback follows Unlock so a longer message cannot push the keypad or button down. Reserve one normal status line; allow genuine errors to wrap naturally and scroll rather than truncate. Do not promise a fixed entire-page height for arbitrary errors or enlarged text.
- Full passphrase remains available; show the phone method using its existing enrolled label only when enabled. Alternative buttons have minimum 48 dp targets and grow with text.
- Existing body fade may remain, provided its size transition does not move the page on busy/error changes. No bounce, keypad entrance sequence, whole-screen shake, or artificial success delay. The neutral toolbar stays mounted.
- In normal portrait the keypad is immediately usable without scrolling; at 150/200% text, preserve readability with scrolling instead of compressing controls. Android safe areas remain respected. No landscape work.

These are deliberately selected design dimensions, not externally mandated standards. Acceptance screenshots determine whether the normal handset composition fits before delivery.

## Interaction and state contracts

- Only accept ASCII 0–9, preserve leading zeroes, stop at 12 digits. Clear deletes all; Backspace deletes one. Physical number keys append, Backspace deletes, Enter invokes the same enabled Unlock action.
- Touch PIN entry must not show the software keyboard. Physical-keyboard support must not require adding an editable field or enabling an IME. TalkBack can operate each digit key; the protected entry display exposes only the entered count.
- Keep input ephemeral; clear on submission, account change, page exit and before launching phone security. Cancellation returns to an empty keypad. Never save PIN input with rememberSaveable, preferences or logs.
- Submission disables input and alternate methods during verification. Use existing busy guards for toolbar/hardware Back too. No second request while one is in progress.
- Errors remain until editing or a deliberate new attempt. Editing clears ordinary stale input errors; a retry-restriction message must remain accessible. Do not classify failures by matching message text: if persistence needs a category, add presentation-only typed failure metadata in the authentication state, without changing the security engine or retry rules.
- Phone-prompt cancellation or unavailability remains on the originating selected account page with an honest result. Capture the originating account/state and reject late callbacks if it is no longer the active request. Existing security validation remains authoritative.

## Allowed implementation files

Expected production changes: NativeAuthenticationScreens.kt, NativeAuthenticationViewModel.kt and NativeUiTags.kt. NativePreviewActivity.kt may change only if its existing Back wiring cannot express the new agreed route; verify that wiring either way. A keypad helper may live beside the auth screen if it makes the implementation clearer; it must remain local to authentication.

Expected test/evidence changes: relevant methods in NativeAuthenticationScreenContractTest.kt, NativeFoundationUiTest.kt, NativeChromeGeometryTest.kt and NativeAccessEvidenceTest.kt; focused authentication routing tests if needed; phase18/qa/native_control_inventory.tsv; this plan's execution results and screenshots.

Do not modify QuickUnlock.kt, AndroidQuickUnlock.kt, authentication facades, database/schema, settings/enrollment screens, Welcome, shared work chrome, day status, financial logic or account permissions. Existing unrelated working changes must be preserved.

## Execution order and stopping rules

1. Check the baseline file hashes, then implement the new routing and phone-prompt origin/Back behavior.
2. Implement the keypad and feedback using the visual specification; update only affected tests and control inventory entries.
3. Compile and run the selected checks below, capture final PIN screenshots, then deliver an update to the preview app on the explicitly selected A06 using install -r. Preserve its accounts/data and the ordinary app. Leave it ready at account selection for user review.

If a test fails, fix the changed code or its obsolete expectation and repeat only the relevant check. Expand scope only for a demonstrated dependency needed for this flow. Record unrelated discoveries for later; do not investigate them. No full checkpoint or broad role/menu/finance suite. User visual acceptance stays pending until reviewed.

## Scoped validation selection

Select methods explicitly; selecting a whole large test class is not a scoped run.

Update and run existing NativeFoundationUiTest methods:

- accessBackAccountChangeAndAuthenticationErrorFollowTheirVisibleContracts
- quickPinUnlocksTheSelectedAccount
- quickPinRejectsAnotherAccountsPinAndUnlocksTheSelectedAccount
- welcomeAccountPickerAndFullPassphraseOpenCashierSale
- switchAccountHardwareBackKeepsSessionAndReturnsToMore
- switchAccountUiBackKeepsSessionAndReturnsToMore

Update/run the relevant NativeAuthenticationScreenContractTest methods for retry-restricted PIN recovery and phone-only method selection/cancellation. Add focused contracts for the PIN page's enrolled/non-enrolled phone action, clear/delete/4–12 boundaries/leading zeroes, physical keys/masked semantics and single-submit busy state. Phone success/cancellation/unavailability may use a controlled test authenticator; distinguish it from an actual Android prompt observation.

Verify the routing matrix through focused view-model tests, including original-session preservation and late phone callbacks. There is no existing authentication view-model test file in the inspected test inventory; do not claim one has passed.

Run only access/keypad geometry cases, excluding Welcome, work headers, sale and all-role footers: 100% light with both alternatives and long identity; 150% light; 200% dark with constrained viewport and long error. Assert no toolbar/keypad movement across idle/input/busy/error states at fixed font/identity, hidden touch IME and reachable actions after scroll. Produce screenshots for those three fixtures and one real preview keypad screen. Inspect them; do not self-mark owner acceptance.

Run the lightweight inventory/tag consistency check because new keypad controls are added. No security-engine suite is planned while its production source hashes remain unchanged; validate wrong-account rejection and recovery through the selected real sign-in journeys instead.

## Completion boundary

Done means this route and keypad are installed in preview, selected checks passed, source/data boundaries verified, and screenshots/results handed over. It does not include enrollment cleanup, a general authentication rewrite, a global visual audit, or unrelated fixes.

Independent-judgment hook rerun after the bounded dependency review. The revised scope preserves a legitimate method chooser for phone-only accounts, explicit submission for variable PIN lengths and truthful error sizing; these constrain the visual proposal rather than assuming every extra screen or every layout change is a defect.
