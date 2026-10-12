# Phase 3 batch 4 — personal security result

The five More security destinations (cashier, operator, manager, admin, owner) now render the same two grouped surfaces: enrolled status and changes on this phone. Each keeps its own screen chrome and callbacks. The Quick PIN field accepts exactly 4–12 ASCII digits, retains leading zeroes, and rejects invalid/overlength paste rather than silently saving a different PIN. The three phone-security controllers use the same rule. The form requires the current full passphrase and accurately explains that entering a PIN while enabling phone security saves/replaces the phone's PIN; it does not claim to verify the prior PIN.

The shared panel preserves loading, busy/error states, haptic actions, remove confirmation/cancel, and role-specific test tags. Its input state is keyed by local user ID so it does not carry over when the account changes. The old duplicated status/pin forms are removed. Login keypad, biometric prompt/crypto, server credentials, revocation and security history were outside this batch and unchanged.

Focused verification on the S24 (`SM_S921W`, ADB `RFCX116CYYX`) used the isolated QA package `com.saaf.milkshop.qa.tests`:

- `QuickPinEntryPolicyTest`: 2 host tests passed (3/4/12/13 digits, ASCII-only input, leading zeroes, invalid paste).
- Final selected instrumentation: **OK (11 tests)**. It rendered all five role destinations, exercised PIN/passphrase transfer, phone-security PIN requirement and cancel, checked busy/error at light 100%, dark 150% and light 200%, and proved that a wrong full passphrase does not enroll a 12-digit PIN while a correct one does.
- `:app:assembleDebug` and `:app:assembleDebugAndroidTest` passed with `-Psaaf.instrumentationSandbox=true`; only the isolated QA app was installed for these checks. `git diff --check` passed on the touched tracked UI/inventory files.

This is bounded QA evidence, not a visual sign-off of every phone prompt. Actual biometric prompt appearance/cancel, re-entry and haptics remain in phase 5 phone acceptance. No ordinary app installation, broad suite, commit or push was performed in this batch.

Next bounded unit: phase 3 batch 5a, cashier Activity visibility of only that cashier's own expenses. Keep 5b transaction/product presentation separate.
