# Entrance redesign baseline — 11 October 2026

Planning only. No application source, APK or phone data changed. The independent-judgment hook at `C:/Users/Teymur/.codex/hooks/independent_judgment.py` was inspected and run. It emits an evidence/judgment reminder; it does not audit or certify the design.

Baseline: installed `com.saaf.milkshop.qa`, version `0.4.1-qa`, updated at 05:55:01 on 11 October. Production files match the retained installed-build source manifest. See `baseline-manifest.json` for hashes and screenshot provenance. The account-picker image shows five accounts before Preview Owner was added; the current preview has six. These are retained screenshots, not fresh captures of the current account set.

## Verified findings and changes to propose

|Surface|Current evidence|Useful information/action to retain|Proposed treatment|
|---|---|---|---|
|Initial in-app Welcome|`WelcomeScreen.kt` adds `NativeChromeBand` above the existing mark, shop name, tagline and assurances. Screenshot has a large repeated green region.|Configured shop identity, brief purpose, offline/private reassurance, Continue.|Remove the green region and redundant Welcome heading/metadata. One brand composition on the page background, one concise reassurance line, pinned Continue.|
|Choose account and Switch account|`AccessFrame` supplies a 136dp-minimum content band plus status inset. Shop name, Private account access and On this phone repeat context in the body. Rows have their own outlines plus exterior dividers.|Back/cancel semantics, shop identity, account name, role and actually available methods; existing account-specific authentication.|Compact neutral toolbar with Back and shop name; page heading and short instruction in the body. Preserve readable account rows; remove the redundant dividers between independently outlined rows.|
|Other access steps|Loading, startup failure, first Owner setup, passphrase, method picker, Quick PIN and recovery share the same AccessFrame. Removing only the picker's band would bring it back on account selection.|Selected identity, credential wording, errors, retry, recovery warning and acknowledgement.|Recommended scope: one neutral access frame for all eight visible state types, with a fixed toolbar slot. Treat both picker modes as separate acceptance cases. No workflow or security changes.|
|Open/Closed day-status action|`CashierScreenHeader.kt` uses a content-width Surface with height 48dp and fully rounded corners. Text has padding without explicit centering; the retained Count cash image shows the nearly circular shape and high label.|Visible day state and existing authorized day-status action; all role shells.|A deliberate horizontal status pill with centered label inside an adequate touch area. Size a shared slot for both labels so changing Open/Closed cannot move the rest of the header.|
|System bars|`NativePreviewActivity.kt` chooses icon brightness by Welcome versus Access. The light Welcome image currently shows dark status icons on green; the source comment still assumes Welcome has no green band.|Status/navigation inset protection and readable OS icons.|Derive appearance from the actual neutral entrance versus green signed-in surface. Light neutral entrance gets dark status icons; dark entrance gets light icons. Preserve signed-in treatment.|

The header adds roughly 136dp of content space before the OS inset at default text, but none of its information requires a separate coloured region here. That is a product-design conclusion based on duplication and task relevance, not a universal rule against coloured headers. Signed-in work headers still communicate actor, role/date and day state and remain in place.

“Initial screen” is scoped to the in-app Welcome/startup access states. An Android launch splash is a separate system surface; no splash duration, launcher icon or launch-flow changes are proposed.

Current source anchors: `nativeui/welcome/WelcomeScreen.kt`; `nativeui/auth/NativeAuthenticationScreens.kt` (`AccessFrame`, `AccountPickerScreen`, `AccountRow`); `nativeui/sale/CashierScreenHeader.kt`; `nativeui/common/NativeChrome.kt`; `nativeui/NativePreviewActivity.kt` (`SystemBarAppearance`). Paths are relative to `phase18/app/src/main/java/com/saaf/milkshop/`.

## Baseline acceptance limits

The preceding bar-unification full checkpoint is recorded as 606/609, FAIL, with the exact three cases passing unchanged focused reruns. That result is historical baseline evidence, not a green checkpoint for this redesign. Visual sidecars remain unreviewed. This planning pass makes no new device, motion or accessibility acceptance claim.
