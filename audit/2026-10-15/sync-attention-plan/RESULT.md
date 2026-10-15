# Phase 2 result: persistent Owner attention for held entries

Status: **ACCEPTED at the named local Owner / held-entry scope**, 15 October 2026. [Pinned plan](PLAN.md), [reconciled verification](verification.json).

## Completed steps

- [x] ✅ ~~Verify the baseline, research relevant guidance and judge the design.~~
- [x] ✅ ~~Observe persisted held-entry counts without requiring an online connection.~~
- [x] ✅ ~~Show persistent attention on Owner More and its Sync menu row.~~
- [x] ✅ ~~Keep retained holds visible while disconnected; preserve the existing resolver.~~
- [x] ✅ ~~Run affected checks and inspect synthetic A06 captures.~~
- [x] ✅ ~~Reconcile the plan and preserve the remaining scope boundaries.~~

## Resulting behavior

Owner Overview/Work/Records/More share a count badge on the existing More navigation icon. The Sync menu row explains how many entries need review. The count comes from observable Room queries; navigating away or opening the list does not dismiss it. The list observes actual held rows while open. No new persistent alert flag, dismiss action, polling job, server request or database schema/version change was added.

The observer is keyed to the signed-in Owner session and lifecycle. Read failure retains a known count with unavailable/retry wording; a fresh unknown state is not treated as healthy zero. The facade denies expired and non-Owner sessions. Retained entries remain visible with reconnect guidance and disabled resolution controls if the connection/session is absent. Existing Keep mine / Take server transaction behavior remains; Keep mine awaits upload and Take server's notice now describes its actual local application.

Header and bottom-navigation dimensions remain stable across count changes. The badge shows `99+` above 99, while navigation semantics announce the exact count. Visual inspection found that 200% text made this redundant status glyph dominate the icon. Only the badge's font scaling is capped at 1.3; normal menu/body text still follows the user's text setting and the exact count remains available in that text and accessibility semantics. The final dark 200% capture was inspected after that correction.

## Verification and limits

**17 unique affected checks pass on A06**, isolated package `com.saaf.milkshop.qa.tests`, Android notification permission explicitly denied for that package. This includes file-backed count/list reopen, unrelated outbox/park/audit exclusion, read failure/cancellation/retry, Owner/expired/Cashier boundaries, session replacement, More routing, retained disconnected holds, existing Keep mine/Take server callbacks and failure preservation, real Room-to-UI updates without a destination reload, and light/default plus dark/200% geometry. No full suite ran.

The first run passed 14/17. The three failing badge methods used a merged-tree lookup, while navigation merges the badge's descendants. Captures confirmed rendering and the raw error identified the unmerged node. An intermediate retry still lacked the unmerged lookup. The corrected tests retain actual display, exact count/text, item containment and stable header/navigation assertions; only those three methods were rerun. All three passed before the visual-only badge adjustment and passed again afterward. The other fourteen checks cover unchanged query/facade/controller/menu behavior and were not repeated without cause. [First results](first-results.json), [intermediate results](overlay-retry-results.json), [corrected results](unmerged-retry-results.json), [final affected results](visual-final-results.json).

Application APK SHA-256 for the final isolated build: `88c262f5503e2041e2c997e5efabffca15f1ea55f9ac2d2cb0c37180763d39f8`. These QA bytes were built from the working changes on baseline `3af0117`; they are not a new signed ordinary release. The final source manifest records the changed/new production files. The regular shop app was not installed, seeded, synced or edited by this run. Captures are synthetic and ignored locally.

The initial build encountered a full C: drive. A generated-build relocation to D: failed KSP's cross-drive path rule; after space became available, the ordinary project build completed. A test compilation exposed trailing-lambda compatibility in navigation parameters; the signature was adjusted to preserve existing callers. No user files, signing material, backups or release artifacts were deleted; no infrastructure repair or broad toolchain upgrade was opened.

This is not a real process-kill/whole-app pilot test: persistence is established by file-backed reopen and UI/session observation tests. The physical walkthrough exercised the actual components and local resolver with synthetic data under instrumentation. Human comfort and uncoached staff outcomes remain phase 1 participation.

## Retained boundary and next step

Historical audit-only conflicts, ordinary pending uploads and automatic dependency parks are deliberately not counted as held entries. The broader B8 conflict-attention commitment remains **partial**, including its separate review/acknowledgment policy. This patch does not provide cross-phone alerts or expose review to Cashier/Manager/Admin/Operations sessions. It does not establish unattended conflict monitoring on a cashier-only phone.

The next feature is phase 3 Credit basics. Its own bounded baseline/research/judgment must precede implementation. No Credit feature or new cloud/release work was started here. Ordinary app delivery belongs to the handover/release boundary, not this isolated QA acceptance.
