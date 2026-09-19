# Native interaction contract

Status: **REQUIRED GATE — applies to every native screen from Phase 14.8.3 onward**

Date adopted: 2026-09-19

## Definition of an implemented interaction

Every visible interactive control must have all of the following before its screen can pass a checkpoint:

1. a written workflow with its starting state, user action, required state change, and visible result;
2. a stable Compose test tag;
3. a device test that performs the real action;
4. assertions for both the resulting state and the user-visible evidence;
5. cancel, dismissal, disabled, error, and retry coverage when those states exist.

An interaction is incomplete if it silently ignores a tap, changes only hidden state, displays stale calculated values, opens the wrong surface, or cannot prove its result on screen. A control whose destination is not built yet must be visibly disabled or give an explicit temporary preview response. When the real destination is implemented, that temporary contract and test must be replaced in the same change.

For database writes, the test must also prove the saved domain result and immediate refreshed UI. Failure tests must prove that no partial write occurred.

## Screen acceptance procedure

Before accepting a screen:

1. inventory every `Button`, `IconButton`, selectable card, clickable row, navigation destination, text input, dialog action, and system Back override;
2. match every inventory item to one workflow row and at least one test path;
3. run the focused Compose tests on an Android device;
4. report the exact number of passed, failed, and untested workflows;
5. keep the screen checkpoint open if any visible control is silent or untested.

## Current Welcome, authentication, and Cashier contract

| Control | Required workflow | Visible evidence | Device test |
|---|---|---|---|
| Welcome · Continue | Enter the native access flow and inspect the local vault. | First run shows Owner setup; returning installations show the account picker. | `firstRunCreatesOwnerShowsRecoveryCodeAndEntersCashier`; `welcomeAccountPickerAndFullPassphraseOpenCashierSale` |
| Startup · Try again | Retry a failed account-startup query. | The requested account state replaces the failure screen. | `startupFailureRetryChangesToTheRequestedVisibleState` |
| Account picker · Back | Return to the branded Welcome surface. | `Saaf Milk` appears and no work screen is exposed. | `accessBackAccountChangeAndAuthenticationErrorFollowTheirVisibleContracts` |
| Account picker · Android Back | Follow the same safe route as the visible Back action. | `Saaf Milk` appears. | `accessBackAccountChangeAndAuthenticationErrorFollowTheirVisibleContracts` |
| Account row | Select that identity for authentication. | Accounts with enrolled Quick unlock show their account-specific method picker; other accounts show full passphrase directly. | `welcomeAccountPickerAndFullPassphraseOpenCashierSale`; `accessBackAccountChangeAndAuthenticationErrorFollowTheirVisibleContracts` |
| Method picker · header Back / Android Back | Return to account selection without authenticating. | `Who is using Saaf?` appears. | `accessBackAccountChangeAndAuthenticationErrorFollowTheirVisibleContracts` |
| Method picker · Choose another account | Return to account selection without invoking an authentication method. | `Who is using Saaf?` appears. | `accessBackAccountChangeAndAuthenticationErrorFollowTheirVisibleContracts` |
| Method picker · Quick PIN | Open PIN entry only when that account has a PIN enrolled on this phone. | `Enter Quick PIN` appears for the selected identity; blank Unlock is disabled. | `accessBackAccountChangeAndAuthenticationErrorFollowTheirVisibleContracts`; `quickPinUnlocksTheSelectedAccount` |
| Method picker · Phone security | Invoke Android's enrolled phone-security prompt only when that account has the method enrolled. | Cancellation returns to the same identity and shows `Quick unlock was cancelled`; an unenrolled method is absent. | `methodPickerShowsOnlyEnrolledPhoneSecurityAndSurfacesCancellation` |
| Method picker · Full passphrase | Open the always-available full Saaf passphrase route. | The selected identity remains visible on the passphrase screen. | `accessBackAccountChangeAndAuthenticationErrorFollowTheirVisibleContracts` |
| Quick PIN · header Back / Android Back | Return to the selected account's method picker without authenticating. | `Unlock <display name>` appears. | `accessBackAccountChangeAndAuthenticationErrorFollowTheirVisibleContracts` |
| Quick PIN · Use full passphrase | Leave PIN entry and open the selected account's recovery authentication route. | The full-passphrase field appears for the same identity. | `accessBackAccountChangeAndAuthenticationErrorFollowTheirVisibleContracts`; `rateLimitedQuickPinKeepsFullPassphraseRecoveryVisibleAndWorking` |
| Quick PIN · Unlock | Stay disabled while blank; authenticate only the selected account with its phone-bound PIN. | A correct PIN opens `New sale`; another account's PIN shows an identity-specific error; starting a retry clears the old error and the corrected PIN succeeds. | `quickPinUnlocksTheSelectedAccount`; `quickPinRejectsAnotherAccountsPinAndUnlocksTheSelectedAccount` |
| Passphrase · header Back | Return to the selected account's method picker when Quick unlock exists; otherwise return to account selection. | `Unlock <display name>` or `Who is using Saaf?` appears as applicable. | `accessBackAccountChangeAndAuthenticationErrorFollowTheirVisibleContracts` |
| Passphrase · Choose another account | Return to account selection without authenticating. | `Who is using Saaf?` appears. | `accessBackAccountChangeAndAuthenticationErrorFollowTheirVisibleContracts` |
| Passphrase · Android Back | Follow the same safe route as the visible header Back action. | The method picker or account picker appears as applicable. | `accessBackAccountChangeAndAuthenticationErrorFollowTheirVisibleContracts` |
| Passphrase · Sign in | Stay disabled while blank; authenticate with the selected account's full passphrase and clear any PIN pause for that identity. | A valid secret opens `New sale`; an invalid secret keeps the identity visible; starting a retry clears the old error and the corrected passphrase succeeds. | `welcomeAccountPickerAndFullPassphraseOpenCashierSale`; `accessBackAccountChangeAndAuthenticationErrorFollowTheirVisibleContracts` |
| First Owner · Back / Android Back | Return to Welcome without creating an identity. | `Saaf Milk` appears; returning to access still shows first-Owner setup. | `firstRunCreatesOwnerShowsRecoveryCodeAndEntersCashier` |
| First Owner · Create Owner | Stay disabled until all fields are filled; reject mismatched secrets; create the only first Owner through native security. | Mismatch feedback appears without advancing; valid input shows the one-time recovery code. | `firstRunCreatesOwnerShowsRecoveryCodeAndEntersCashier` |
| Recovery code · Android Back | Prevent accidental dismissal before the one-time code is acknowledged. | The recovery-code screen remains visible. | `firstRunCreatesOwnerShowsRecoveryCodeAndEntersCashier` |
| Recovery code · I saved this code | Acknowledge the one-time code and enter the authenticated role shell. | `New sale` appears. | `firstRunCreatesOwnerShowsRecoveryCodeAndEntersCashier` |
| Cashier · Android Back | Leave the authenticated area, invalidate its memory-only session, and return to Welcome. | Welcome appears; Continue requires account selection again. | `cashierSystemBackEndsSessionBeforeReturningToAccountPicker` |
| Product · Milk | Select Milk and use PKR 300/L. | Milk selected; rate and cash effect show the milk calculation. | `productButtonsSwitchProductRateUnitAndCalculatedEffects` |
| Product · Yogurt | Select Yogurt and use PKR 400/kg. | Yogurt selected; `PKR 400 / kg` and recalculated cash effect appear. | `productButtonsSwitchProductRateUnitAndCalculatedEffects` |
| Entry · Quantity | Convert an entered amount back to product quantity. | Quantity selected; numeric field and unit show the converted quantity. | `entryModesInputAndSteppersConvertAndRecalculate` |
| Entry · Amount | Convert product quantity into its PKR amount. | Amount selected; `2.0 L` becomes `600 PKR`. | `entryModesInputAndSteppersConvertAndRecalculate` |
| Numeric input | Accept decimal entry and recalculate the draft. | Typed value remains visible and derived effects update. | `entryModesInputAndSteppersConvertAndRecalculate` |
| Decrease | Reduce quantity by 0.5 or amount by PKR 50. | Numeric field displays the reduced value. | `entryModesInputAndSteppersConvertAndRecalculate` |
| Increase | Increase quantity by 0.5 or amount by PKR 50. | Numeric field displays the increased value. | `entryModesInputAndSteppersConvertAndRecalculate` |
| Payment · Paid now | Route the draft to Cash. | Paid now selected; `Cash +PKR …` appears and customer row is hidden. | `customerPaymentOpensPickerAndPaidNowRestoresCashEffect` |
| Payment · Customer account | Open customer selection; switch payment only after a customer is chosen. | Picker opens; chosen customer becomes selected and `Customer due +PKR …` appears. | `customerPaymentOpensPickerAndPaidNowRestoresCashEffect` |
| Customer · Ahmed Store | Choose Ahmed Store for the sale. | Ahmed Store appears as the selected account. | `customerPickerCancelPreservesPaidNowAndBothAccountsCanBeSelected` |
| Customer · Bilal Dairy Point | Choose Bilal Dairy Point for the sale. | Bilal Dairy Point appears as the selected account. | `customerPickerCancelPreservesPaidNowAndBothAccountsCanBeSelected` |
| Customer picker · Cancel | Close without changing the existing payment selection. | Paid now remains selected when no customer was previously chosen. | `customerPickerCancelPreservesPaidNowAndBothAccountsCanBeSelected` |
| Selected customer row | Reopen the picker to change the customer. | Picker opens and a different customer can replace the current one. | `customerPickerCancelPreservesPaidNowAndBothAccountsCanBeSelected` |
| Wholesale, delivery & notes | Expand and collapse the secondary details. | Price, delivery, and note values appear, then disappear on collapse. | `secondaryDetailsToggleShowsAndHidesItsDefinedContent` |
| Record sale | At this preview checkpoint, prevent writes and explain the result. | `Native preview only — no sale was recorded.` appears. | `recordSaleShowsSafeNonWritingResult` |
| Navigation · Sell | Confirm that the already-selected destination remains open. | `Sell is already open.` appears. | `everyBottomDestinationProvidesItsDefinedPreviewResponse` |
| Navigation · Collect | At this checkpoint, explain that the destination is not connected. | Explicit Collect preview notice appears. | `everyBottomDestinationProvidesItsDefinedPreviewResponse` |
| Navigation · Activity | At this checkpoint, explain that the destination is not connected. | Explicit Activity preview notice appears. | `everyBottomDestinationProvidesItsDefinedPreviewResponse` |
| Navigation · More | At this checkpoint, explain that the destination is not connected. | Explicit More preview notice appears. | `everyBottomDestinationProvidesItsDefinedPreviewResponse` |

## Current evidence

- Device UI classes: `NativeOwnerBootstrapUiTest`, `NativeAuthenticationScreenContractTest`, and `NativeFoundationUiTest`
- Typed facade class: `NativeAuthenticationFacadeTest`
- Stable tag registry: `phase18/app/src/main/java/com/saaf/milkshop/nativeui/NativeUiTags.kt`
- Device: Samsung A06, `SM-A065F`, wired serial `R8VXA02SNKH`
- Result on 2026-09-19: **28/28 passed, 0 failed, 0 skipped** across the fresh-install Owner partition (1), Welcome/authentication/Cashier UI partition (12), typed authentication and screen-state partition (5), and core Quick unlock security partition (10).
- Manual Android prompt result on 2026-09-19: existing Owner Phone security enrollment was detected without re-enrollment; successful device-PIN approval opened native `New sale`; Android Back ended the session; prompt cancellation returned to the same Owner method picker with `Quick unlock was cancelled` visible.
