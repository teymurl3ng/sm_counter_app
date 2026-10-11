# Automated UX work — execution sequence

10 October 2026. The user requested the plan, minimal required involvement, and automation. This file makes the next steps reviewable; it does not claim implementation or device acceptance.

## Start here

1. Preserve the current working tree. The prior audit inventories the UI; `execution_baseline.json` records SHA-256 hashes of the five existing protected modifications that blocked qaFast. No new domain change is authorized by a QA exception.
2. After the scoped QA exception is authorized, run the canonical Gradle gates against the isolated A06 package. Build and identify a matching QA APK; record source/build/device provenance. Do not use the older convenience runner as the authority: its screen map is narrower than the current Gradle map.
3. Reproduce authentication/recovery and the repeated Work → ReceiveMilk/Deliveries → Back paths, including refresh completing during navigation. Capture transition frames and retain disposable fixtures only.
4. Add meaningful regression coverage for the stale-destination race, fix UI state merging, then fix the reproduced transition/inset mechanisms. Do not change animation constants until the failing route is established.
5. Introduce and validate shared list, feedback, and haptic components on Products/Employees/Records and representative transactions. Retain the existing color direction while correcting readability.
6. Apply the accepted behavior across all screen families, including dialogs, details, success, failure, and warm re-entry. Every applicable route/state must have recorded acceptance evidence.
7. Apply configurable business identity. Repair catalog-to-sale mapping and scalable picking after product breadth is settled. Reconcile cashier day access after close policy is settled.
8. Run dependent screen suites and the complete checkpoint. Present a compact visual review and a tactile check; never label unreviewed screenshots or an API vibration call as physical acceptance.

## Automation coverage

The agent can implement fixes, create fixtures, drive isolated instrumentation, repeat routes, inject refresh failures, capture/analyze frames, measure contrast, run regression checks, and produce a traceable result ledger. Further harness coverage will be added where existing tests only check settled screens.

User involvement is limited to device availability/unlock or OS prompts when necessary, a short haptic feel check, final visual review required by the repository, and two business decisions: initial product breadth and cashier close authority. Live cloud authorization may need user interaction only if a changed flow actually requires it; it is not a prerequisite for basic UI work.

## Scoped QA exception

The existing guard states: `Re-run with -AllowLogic / -Pallow.logic=true only with owner approval.` The proposed exception permits test/build execution over the five already-modified files in `execution_baseline.json`. It does not authorize editing those files, accepting their correctness, changing money/permission rules, installing the ordinary package, or overriding later protected changes.

Before each use, verify those file hashes and the protected-change set still match the recorded baseline. If additional protected changes appear, do not reuse this approval for them. Keep `LOGIC EXCEPTION ACTIVE` and the protected-file list in the run evidence.

Canonical commands, from `phase18`, once authorized:

```powershell
.\gradlew.bat qaFast -Pallow.logic=true
.\gradlew.bat qaScreen -Pqa.screen=auth -Pqa.device=R8VXA02SNKH -Pallow.logic=true
.\gradlew.bat qaScreen -Pqa.screen=ownerwork -Pqa.device=R8VXA02SNKH -Pallow.logic=true
.\gradlew.bat qaScreen -Pqa.screen=deliveries -Pqa.device=R8VXA02SNKH -Pallow.logic=true
```

These are initial functional gates, not an all-screen motion pass. Capture/inspect exact failing routes and expand the regression matrix before final qaCheckpoint. The exception remains pending until the user grants it.
