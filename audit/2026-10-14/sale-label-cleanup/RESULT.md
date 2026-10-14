# Sale form label cleanup — 14 October 2026

## Baseline and judgment

The shared native sale form repeated Product / Tap to switch above named selectable product cards, Enter sale above the Quantity / Amount selector, and Payment destination above Paid now / Customer account. These labels add no distinct instruction. The separate At PKR rate repeated the product card for retail sales.

There is a verified exception: the calculation uses an editable wholesale rate, while product cards previously always displayed retail rates. Removing the calculation's rate without adjusting the selected card would conceal the active wholesale price in the main entry area. This conclusion comes from the implementation, not the user's suggested redundancy.

## Bounded change

- Remove the four repeated labels and the separate At PKR sentence.
- Retain the existing product → quantity/amount → payment → review order, with consistent existing section dividers. Keep the calculated total or converted quantity below the entry field.
- Display the active wholesale rate on the selected card, explicitly marked Wholesale. If that rate is invalid, ask for a wholesale rate instead of displaying a retail price as the active sale price. Unselected cards retain their retail prices.
- For more than six products, replace the external tapping instruction with a Change product label and chevron inside the existing picker card. Keep search, cancel and selection behavior.

Two production files: CashierSaleScreen.kt and values/strings.xml. Sale calculations, persistence, permissions, payment choices, surcharge, notes, delivery and shared header/keyboard code are unchanged. This is presentation work inside finish batch 7, not an additional finish batch.

## Verification and delivery limits

Source diff reviewed and whitespace checked. `:app:compileDebugKotlin -Psaaf.instrumentationSandbox=true` succeeded (42 seconds; no tests). Functional tests and visual phone acceptance remain deferred at the user's request.

## Requested commit and installation

Source committed as `d2928710ca8d96602d2fb0a2dbe20d31dad2110e`, including the prior recovery/header/Quick PIN setup fixes. `:app:assembleRelease` then succeeded; the signed regular 0.4.2/code 6 build has the existing release certificate, preview fixtures disabled and embedded Git SHA matching this commit. PACKAGE_MANIFEST.json records source and APK hashes.

S24 was the only connected phone. It received a normal-profile `install -r --user 0` update, without uninstalling or clearing app data. Its installed APK hash and version match the verified artifact, and the launcher opened. A06 was disconnected and retains the prior build. INSTALLED.json records the exact result. No functional tests or Git push ran. Installation does not establish broader business-data, visual or staff acceptance.
