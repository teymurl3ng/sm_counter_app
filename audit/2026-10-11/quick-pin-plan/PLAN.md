# Quick PIN sign-in plan

Planning only. No application code, account data, device settings or builds changed in this pass.

The concrete pre-execution baseline is now in [BASELINE.md](BASELINE.md), with current-source hashes in SOURCE_BASELINE.json. [SCOPE.md](SCOPE.md) freezes navigation, dimensions, state rules, allowed files, selected validation and delivery boundary; it supersedes the preliminary sizing ranges and feedback description below.

## Verified baseline

- NativeAuthenticationViewModel.selectAccount sends accounts with any enrolled quick-unlock method to MethodPicker. Selecting Quick PIN then opens a separate QuickPin state.
- QuickPinSignInScreen uses an OutlinedTextField with a NumberPassword keyboard. It has no focus request, so the user taps the field to start entering digits. Submission uses Unlock or the keyboard's Done action.
- The PIN input supports up to 12 digits; QuickPinHasher's enrollment policy is 4–12 ASCII digits. Preserve existing enrolled PINs and this range.
- Phone security already uses Android's BiometricPrompt and a cryptographic object. It is a different method from the account's Quick PIN.

Evidence inspected: NativeAuthenticationScreens.kt, NativeAuthenticationViewModel.kt, QuickUnlock.kt and AndroidQuickUnlock.kt in phase18/app/src/main/java/com/saaf/milkshop.

## Recommendation and evidence

Use an app-owned numeric keypad for Quick PIN. Selecting a PIN-enabled account should open it directly. Keep Android's system prompt for the existing separately enrolled Phone security method.

Android's DEVICE_CREDENTIAL authenticates the device's screen-lock PIN, pattern or password. It does not offer a system keypad for verifying an arbitrary app account PIN. Replacing Quick PIN with it would change what credential is accepted, rather than merely improve presentation. [Android documentation](https://developer.android.com/identity/sign-in/biometric-auth).

An account-specific PIN is an established POS approach: Shopify documents staff using personal PINs to unlock POS with their own staff profiles after device login. This supports keeping account PINs; it does not establish our proposed dimensions or animations as industry requirements. [Shopify POS PIN documentation](https://help.shopify.com/en/manual/sell-in-person/shopify-pos/manage-your-shopify-pos-pin).

The cheaper alternative is to focus the existing field automatically and show the numeric system keyboard immediately. That removes the field tap, and a method chooser can legitimately help people who prefer phone security. However, autofocus alone preserves the method-picker detour and keyboard-driven viewport changes. For this app's repeated account access, a directly available keypad with visible alternatives is the better fit. This is a design judgment, not a measured usability result.

## Bounded implementation scope

1. For accounts with Quick PIN enabled, account selection goes directly to the PIN page. Accounts without Quick PIN retain their present routing. Selecting the currently active account during Switch account retains the existing return-to-session behavior.
2. Replace the login PIN field with a masked digit display and a 3-column keypad: 1–9, Clear, 0, Backspace. Preserve leading zeroes and the 12-digit limit. Keep explicit Unlock, enabled for 4–12 digits; do not auto-submit on the fourth digit or change PIN policy.
3. Show account name/role once and a short instruction. Reuse the neutral access toolbar, existing typography and safe-area treatment. Keypad has a proposed maximum width of 320 dp, minimum 56 dp key height, 8–12 dp gaps and restrained rounded-square key surfaces. These dimensions are proposals to validate, not external standards.
4. Make Phone security visible only when enrolled, alongside Use full passphrase. Trigger the Android prompt only after a deliberate tap. Cancelling it returns to the same PIN page. Back from PIN returns to Choose account; Back from passphrase opened here returns to PIN. Preserve pending account-switch cancellation/session behavior.
5. Reserve a status area for Checking PIN, failure and retry restrictions. Disable submission and digit editing during verification. Clear the PIN on submission/account change/leaving the page. Keep recovery visible. Use the existing security checks, retry limits and session acceptance; no changes to verification, storage, enrollment or authorization policy.
6. Keep the toolbar, account identity and keypad anchors stable during entry and checking. Use a short opacity transition for page changes and local pressed feedback; no page bounce, full-screen shake, keyboard pop-up or success delay. At enlarged text, allow scrolling rather than shrinking text or cutting off actions.
7. Provide accessible digit-key labels and a protected masked-entry description that announces only the count, never the entered numbers. Support hardware numeric input and existing haptic preferences. Keep real errors visible rather than relying only on color or vibration.

## Acceptance and scoped checks

- Account tap immediately presents a usable keypad, with no intermediate method chooser or field-focus tap for PIN-enabled accounts.
- Correct PIN unlocks the selected account; wrong PIN and retry restriction retain passphrase recovery. Long PINs and leading-zero PINs still work.
- Phone-security success/cancellation/unavailability and passphrase fallback preserve the correct selected account and Back behavior.
- Back/account switching and repeated submission cannot accept a stale account result or submit twice.
- Keyboard stays hidden during touch PIN entry. Error/busy changes do not remount or shift the toolbar/keypad on the normal portrait layout.
- Light/dark and 100%, 150%, 200% text remain readable, accessible and reachable. Test the keypad's 4/12-digit boundaries, masked semantics and physical-keyboard path.

Run only the affected authentication view-model routing tests, PIN screen contracts/geometry and selected-account PIN/phone-security/fallback journeys. No full checkpoint, broad role-menu testing or unrelated finance tests. Reuse relevant existing tests where possible; extend only the contracts changed here.

## Deferred, without further investigation

Some signed-in More security forms impose a four-digit condition while Quick PIN enrollment's core policy accepts 4–12 digits. That inconsistency was observed during this bounded read; enrollment/settings redesign is a separate task.

## Judgment hook reassessment

Read and executed C:/Users/Teymur/.codex/hooks/independent_judgment.py after inspecting the implementation and forming the provisional recommendation. Reassessed the strongest practical alternative (autofocus plus native numeric keyboard) and the method chooser's legitimate role. Final conclusion: remove the chooser only for PIN-enabled accounts, make alternatives accessible on the PIN page, and preserve the existing Android phone-security method. No claim that custom UI improves the cryptographic security of Quick PIN, or that Android phone security is categorically inappropriate.
