# Quick PIN bounded execution

User authorized implementation of quick-pin-plan/SCOPE.md. Baseline hashes matched before editing. Previous uncommitted entrance redesign is preserved.

The user explicitly requested affected tests only. This overrides the broad qaFast/qaScreen/qaCheckpoint and landscape gates in NATIVE_SCREEN_ACCEPTANCE.md for this pass. Run explicit method selections in selection.txt, compile the isolated sandbox, and check inventory consistency. Do not run full class selections, the full checkpoint, unrelated finance/menu tests or landscape.

Device mutations must use A06 R8VXA02SNKH. Tests use com.saaf.milkshop.qa.tests and its .test instrumentation. Delivery uses install -r on com.saaf.milkshop.qa. No clears/uninstalls of interactive preview or ordinary app. Owner visual acceptance remains UNREVIEWED. System phone prompt validation uses a controlled gate in the routing tests, not a claim of actual biometric authentication.
