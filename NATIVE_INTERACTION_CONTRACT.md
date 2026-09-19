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
| Cashier · Android Back | Leave an untouched screen immediately; if the draft was edited, require an explicit keep/discard decision before invalidating the memory-only session. | Keep editing preserves the entry; Discard and leave returns to Welcome; Continue requires account selection again. | `cashierSystemBackEndsSessionBeforeReturningToAccountPicker`; `editedDraftRequiresExplicitBackChoice` |
| Cashier · foreground return | Revalidate the memory-only session before protected Sale content can return after backgrounding. | An invalidated/expired session removes `New sale`, clears the application session, returns to account selection, and explains that sign-in is required. | `foregroundValidationRemovesAnInvalidatedSessionBeforeSaleReturns` |
| Cashier · Activity recreation | Preserve a valid in-progress draft across rotation/Activity recreation without persisting its memory-only session across process death. | Entry mode/value, selected customer, and expanded details reappear after recreation. | `editedSaleDraftSurvivesActivityRecreation` |
| Sale load · Try again | Retry the authoritative products, rates, stock, customer, cash-account, and business-day read. | The failure changes to visible loading and then the requested result. | `saleLoadFailureRetryChangesToVisibleLoadingState` |
| Product · Milk | Select the active Milk product and its stored retail rate. | Milk selected; the stored rate, current stock, and cash effect are recalculated. | `productButtonsSwitchProductRateUnitAndCalculatedEffects` |
| Product · Yogurt | Select the active Yogurt product and its stored retail rate. | Yogurt selected; the stored rate, current stock, and cash effect are recalculated. | `productButtonsSwitchProductRateUnitAndCalculatedEffects` |
| Entry · Quantity | Convert an entered amount back to product quantity. | Quantity selected; numeric field and unit show the converted quantity. | `entryModesInputAndSteppersConvertAndRecalculate` |
| Entry · Amount | Convert product quantity into its PKR amount using the stored rate. | Amount selected; the numeric field changes to the authoritative amount and remains editable. | `entryModesInputAndSteppersConvertAndRecalculate` |
| Numeric input | Accept decimal entry and recalculate the draft. | Typed value remains visible and derived effects update. | `entryModesInputAndSteppersConvertAndRecalculate` |
| Numeric keyboard · Done | Finish numeric entry and release field focus so the remaining form is immediately operable. | The entry remains visible and the field is no longer focused. | `saleEntryDoneActionClearsKeyboardFocus` |
| Decrease | Reduce quantity by 0.5 or amount by PKR 50. | Numeric field displays the reduced value. | `entryModesInputAndSteppersConvertAndRecalculate` |
| Increase | Increase quantity by 0.5 or amount by PKR 50. | Numeric field displays the increased value. | `entryModesInputAndSteppersConvertAndRecalculate` |
| Payment · Paid now | Route the draft to Cash. | Paid now selected; `Cash +PKR …` appears and customer row is hidden. | `customerPaymentOpensPickerAndPaidNowRestoresCashEffect` |
| Payment · Customer account | Open customer selection; switch payment only after a customer is chosen. | Picker opens; chosen customer becomes selected and `Customer due +PKR …` appears. | `customerPaymentOpensPickerAndPaidNowRestoresCashEffect` |
| Customer row | Choose a real active customer returned by Room. | The selected account name appears and the customer-balance effect replaces the cash effect. | `customerPickerCancelPreservesPaidNowAndBothAccountsCanBeSelected`; `customerAccountSaleWritesTheSelectedCustomerBalance` |
| Customer picker · long list | Keep all active customer accounts reachable without building an unbounded dialog. | The list scrolls to and selects its thirtieth account, then shows that account on the Sale screen. | `longCustomerListScrollsToTheLastAccountAndSelectsIt` |
| Customer picker · Cancel | Close without changing the existing payment selection. | Paid now remains selected when no customer was previously chosen. | `customerPickerCancelPreservesPaidNowAndBothAccountsCanBeSelected` |
| Selected customer row | Reopen the picker to change the customer. | Picker opens and a different customer can replace the current one. | `customerPickerCancelPreservesPaidNowAndBothAccountsCanBeSelected` |
| Wholesale, delivery & notes | Expand and collapse the secondary details. | Price, delivery, and note values appear, then disappear on collapse. | `secondaryDetailsToggleShowsAndHidesItsDefinedContent` |
| Record sale · invalid | Stay disabled for zero/blank entry or an incomplete payment destination. | Record sale cannot be activated. | `recordSaleRequiresAPositiveEntry` |
| Record sale · closed day | Keep recording disabled and explain the exact business date action required. | A live closed-day warning says to reopen the day before recording. | `closedBusinessDayExplainsTheBlockAndDisablesRecord` |
| Record sale · missing references | Keep recording disabled when no active rated product or Cash/Bank account exists and identify who can fix it. | Product/rate and payment-account warnings appear; Paid now reads `Unavailable`. | `missingProductAndPaymentReferencesExplainWhatTheOwnerMustFix` |
| Record sale · saving | Accept only the first submit while the authoritative command is running. | Button is disabled and reads `Recording…`. | ViewModel state guard plus `recordSaleWritesRoomAndShowsRefreshedReceipt` |
| Record sale · paid now | Write one authoritative SALE event to the default cash account, attribute the signed-in actor, refresh the read model, and clear the draft only after success. | A numbered receipt appears; stock text refreshes; entry resets to `2.0`. | `recordSaleWritesRoomAndShowsRefreshedReceipt`; `cashierRecordsQuantitySaleToCashWithNativeDefaultSurchargeAndSummaries` |
| Record sale · customer account | Write one authoritative SALE event to the chosen customer balance and attribute the signed-in actor. | A numbered receipt appears and the selected customer's financial movement exists. | `customerAccountSaleWritesTheSelectedCustomerBalance`; `ownerRecordsValueSaleOnCustomerCreditAndNativeDerivesQuantity` |
| Record sale · rejected | Preserve the draft and show the domain failure without a partial event, stock movement, or financial movement. | Insufficient-stock message appears beside the unchanged entry. | `insufficientStockKeepsDraftVisibleAndWritesNoSale`; `saleRejectsInsufficientStockClosedHistoryInvalidChannelAndUnauthorizedOperator` |
| Navigation · Sell | Confirm that the already-selected destination remains open. | `Sell is already open.` appears. | `everyBottomDestinationProvidesItsDefinedPreviewResponse` |
| Navigation · Collect | At this checkpoint, explain that the destination is not connected. | Explicit Collect preview notice appears. | `everyBottomDestinationProvidesItsDefinedPreviewResponse` |
| Navigation · Activity | At this checkpoint, explain that the destination is not connected. | Explicit Activity preview notice appears. | `everyBottomDestinationProvidesItsDefinedPreviewResponse` |
| Navigation · More | At this checkpoint, explain that the destination is not connected. | Explicit More preview notice appears. | `everyBottomDestinationProvidesItsDefinedPreviewResponse` |

The whole Sale surface must also retain its core input, payment, details, review, and Record controls at 150% text in both color schemes, expose meaningful input/state/live-region semantics to TalkBack, and keep the work form reachable in landscape. `largeTextDarkThemeKeepsCoreActionsAndTalkBackStateAvailable` and `nativeSaleRemainsUsableInLandscape` enforce that cross-control contract.

## Current evidence

- Device UI classes: `NativeOwnerBootstrapUiTest`, `NativeAuthenticationScreenContractTest`, and `NativeFoundationUiTest`
- Typed facade class: `NativeAuthenticationFacadeTest`
- Stable tag registry: `phase18/app/src/main/java/com/saaf/milkshop/nativeui/NativeUiTags.kt`
- Device: Samsung A06, `SM-A065F`, wired serial `R8VXA02SNKH`
- Result on 2026-09-19: **28/28 passed, 0 failed, 0 skipped** across the fresh-install Owner partition (1), Welcome/authentication/Cashier UI partition (12), typed authentication and screen-state partition (5), and core Quick unlock security partition (10).
- Manual Android prompt result on 2026-09-19: existing Owner Phone security enrollment was detected without re-enrollment; successful device-PIN approval opened native `New sale`; Android Back ended the session; prompt cancellation returned to the same Owner method picker with `Quick unlock was cancelled` visible.
- Authoritative Sale result on 2026-09-19: `NativeFoundationUiTest` **16/16 passed** after replacing its preview write with paid/customer/error/draft workflows; `NativeSaleScreenContractTest` **1/1 passed** for load retry; existing `Phase8SaleOperationsTest` **4/4 passed**. The first Sale checkpoint was therefore **21/21 passed, 0 failed, 0 skipped** on the wired A06.
- Hardened Sale result on 2026-09-20: `NativeFoundationUiTest` **20/20**, `NativeSaleHardeningContractTest` **4/4**, and `Phase8SaleOperationsTest` **4/4** passed together on the wired A06 — **28/28 passed, 0 failed, 0 skipped**. The separate current-code Sale load-retry contract remains **1/1 passed**. Coverage adds foreground expiry, Activity recreation, IME, landscape, 150% text, dark theme, TalkBack semantics/live feedback, long customer lists, closed day, and missing references.
