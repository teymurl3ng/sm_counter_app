# Layout/keyboard source repair - 14 October 2026

Status: audit and bounded implementation complete; affected native Kotlin compiles. Phone acceptance deferred. No final release publication or installation ran.

- Frozen source inventory: 112 field call sites in 28 files; 54 indexed presentation files and 73 header call sites in 36 files. See SCREEN_MAP.md and INVENTORY.json. Counts include shared helpers/read-only fields, not independent reproduced bugs.
- Costing's tab/action baseline no longer depends on an optional empty action row. Every section retains a font-aware primary action toolbar with its existing navigation/approval callbacks and tags. Duplicate New plan/Approve baseline list-heading actions moved into the toolbar. Opening provides the existing Shop settings navigation. Loading/error actions are disabled rather than removing the toolbar.
- All five role hosts share one consumed scaffold/keyboard inset calculation. Navigation stays anchored behind the IME, avoiding its abrupt composition removal. Nested standard form padding respects already consumed insets. The older subtraction helper delegates to standard handling, and the native activity explicitly requests documented resize support.
- Two full-screen reauthentication bodies and three dialog bodies now scroll under reduced keyboard space. Access forms already separate toolbar and scrollable body, and no authentication/biometric redesign was added.
- `:app:compileDebugKotlin` succeeded in 15 seconds. Its required existing asset-integrity build task also ran. No new unit/instrumentation/browser/device test, business submission, server mutation or app installation ran. Existing hybrid deprecation/JVM-target warnings are recorded in compile-console.txt; no unrelated repair expanded scope.
- The pre-repair signed APK must not be represented as containing these changes. Release stays paused and the focused CHECK_LATER.md remains open.

Shared source changes address the confirmed layout mechanisms. The exact reported green gap and perceived keyboard motion still require a device check; this result does not claim they have been visually verified.
