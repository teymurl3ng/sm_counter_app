# Quick PIN keypad ready to test

Installed in **Saaf Milk (preview)** (`com.saaf.milkshop.qa`) on Samsung A06 **R8VXA02SNKH**. The app is left on **Choose account**. All six existing accounts were observed after install -r; no interactive data was cleared. Ordinary app package version and install timestamps remain identical.

## Delivered

- Tapping a PIN-enabled account opens its usable keypad directly. There is no intermediate method chooser or field-focus tap.
- Numeric keys, masked digit-count display, Clear, Backspace and physical-keyboard input replace the login text field. Leading zeroes and 4–12-digit PINs are preserved; Unlock remains explicit.
- Phone security appears only when enrolled and uses the existing Android prompt. Cancellation/failure returns to the originating page; late results cannot replace a newer route. Full passphrase remains available, and its Back returns to PIN for PIN-enabled accounts.
- Toolbar, keypad and Unlock anchors stay steady during local entry/busy/error updates. Feedback sits below Unlock and can wrap/scroll. Keys use existing theme surfaces, 64 dp minimum targets and 12 dp corners. Unlock retains the existing primary-action component's 54 dp minimum (rather than the preliminary proposed 56 dp).
- Retry rules, enrolled credentials, verification/storage, permissions and financial behavior are unchanged. Editing clears ordinary input errors; retry restrictions remain visible. Input is not saved or logged.

## Scoped verification

**20 unique affected checks passed**: five new keypad contracts/geometry cases, four authentication routing/session cases, two existing access contracts, six selected real sign-in/Back/switch journeys and three access-toolbar geometry cases. Compilation and control inventory passed (1005 rows, untested=0, orphans=0). No full checkpoint or broad qaFast/role-menu/finance run was launched.

The first selection passed 19/20. The failed navigation test issued hardware Back immediately after tapping the passphrase action, before waiting for that destination. A wait only after Back still failed. The final test waits for the passphrase input/destination before dispatching Back; its individual rerun passed. Application source was unchanged throughout these test-only iterations. Earlier failed logs remain in this folder; the full initial selection is not represented as a 20/20 run.

Phone-security cancellation/unavailability/success and late-result handling used an in-memory controlled device-authentication gate. No actual system biometric or phone credential was entered. PIN account rejection and valid sign-in used the real security/Room path in the isolated test app. The installed interactive preview was inspected without entering its private PIN.

## Evidence and phone check

Four PNGs and four UNREVIEWED sidecars are in evidence/: normal/light, 150%/light, 200%/dark constrained fixtures, and the actual Preview Owner keypad. Large-text screenshots may show a scrolled body; space below the constrained fixture is outside the app viewport. Developer inspected all four; Owner visual acceptance remains pending.

1. Tap **Preview Owner** and enter your existing PIN with the keypad; tap Unlock.
2. Before unlocking, try Clear/Backspace and confirm Back returns to Choose account.
3. Use full passphrase; Back should return to the keypad.

Current implementation and previous entrance redesign remain uncommitted. No additional commit/push was requested for this pass. Enrollment/settings inconsistencies noted in the plan remain deferred; no further scope was added.
