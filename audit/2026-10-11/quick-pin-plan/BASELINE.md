# Quick PIN baseline before execution

Established 11 October 2026. This pass inspected source and existing tests only; no fresh device observation, build or test run is claimed. SOURCE_BASELINE.json records SHA-256 hashes of 11 relevant current files, Git HEAD and the existing working changes. The baseline includes the previous, uncommitted entrance redesign; do not replace those files with HEAD versions.

## Current behavior versus intended behavior

| Area | Observed implementation | Planned result |
|---|---|---|
| PIN-enabled account | Account list → method chooser → Quick PIN field → tap field → system number keyboard | Account list → immediately usable account keypad |
| Phone security | Optional method chooser button; Android BiometricPrompt | Optional keypad-page button when enrolled; same Android prompt and verification |
| PIN field | OutlinedTextField, masked; accepts up to 12 digits; no initial focus request | Masked display with touch keypad and physical-keyboard support; no touch-triggered IME |
| Unlock | Available for any nonblank input; clears field before verification | Available for 4–12 ASCII digits; clears entry before verification; one request at a time |
| Identity | Account name in instructions and again in IdentityCard | Name/role/login shown once in identity block; short generic instruction |
| PIN Back | Returns to method chooser | Returns to account list |
| Passphrase Back | For accounts with quick unlock, returns to chooser | Returns to keypad for PIN-enabled accounts; existing behavior for other accounts |
| Account switching | Current session retained until replacement signs in; picker Back cancels switch | Preserve this behavior, including selection of the current account |
| Error/busy feedback | Error added conditionally above Unlock; busy changes label | Fixed keypad/Unlock anchors; feedback below them; no page-height animation during these updates |
| Access chrome | Neutral toolbar remains mounted; body scrolls; system insets applied | Reuse the existing chrome and portrait-only layout |

## Security behavior to preserve

- App account PIN and Android device authentication are separate credentials. AndroidQuickUnlock uses the existing cryptographic system prompt for Phone security.
- PIN validation supports 4–12 ASCII digits. Leading zeroes are valid. No automatic fourth-digit submission or forced four/six-digit migration.
- PIN verification is serialized by the existing mutex. A third failed attempt pauses for 30 seconds, fourth for two minutes, and fifth requires full passphrase. Keep these exact rules and the existing public failure messages.
- Full passphrase remains available. Preserve enrollment validation, invalidation, session acceptance, role permissions and secret clearing. Existing UI strings do not expose remaining lockout time; do not invent a countdown.
- Current busy guards prevent leaving/submitting during verification. Preserve consistent toolbar and Android Back handling.

## What to add, remove, change and keep

| Action | Concrete work |
|---|---|
| Add | Account keypad; masked digit-count semantics; Clear/Backspace; physical number/Enter/Backspace handling; keypad test tags and inventory mappings; enrolled Phone security action on PIN page |
| Remove | PIN sign-in text field and its tap-to-type step; intermediate method chooser only for PIN-enabled accounts; repeated account name in PIN instructions |
| Change | PIN account routing; Back destinations; Phone security handler accepting QuickPin as well as MethodPicker; submission eligibility; layout and feedback placement |
| Keep | Method chooser for phone-only accounts; passphrase-first flow without quick unlock; all enrolled PINs; Android authentication; security engine; shared chrome; role-specific work screens; current switch-session contract |

## Implementation dependencies confirmed by inspection

NativeAuthenticationHost does not currently pass onUsePhoneSecurity into QuickPinSignInScreen. NativeAuthenticationViewModel.signInWithPhoneSecurity accepts only MethodPicker and restores that state after a failure. Both need a bounded change to support the PIN page as the prompt's origin. Selecting another account, busy guards, passphrase Back and the Activity's hardware Back callbacks must agree with the new route.

Existing end-to-end PIN tests inject text into AuthQuickPinInput and explicitly click AuthQuickPinMethod. They must be updated to select the account directly and tap real digit keys; keeping a hidden text field merely to satisfy old tests would defeat the new interaction. Existing source inventory needs entries for newly visible keypad controls, not a broad inventory redesign.

Some signed-in settings forms insist on four digits despite the core 4–12 rule. This is recorded for later; those forms are not part of sign-in redesign.
