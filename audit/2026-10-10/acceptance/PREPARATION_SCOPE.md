# Acceptance preparation — 2026-10-10

Goal: give the owner one current A06 preview and one local DEV Viewer, ready for the checks that need a real login or personal judgment.

## Scope questions and decisions

- Do the two launcher names identify production versus QA? No. A06 currently has com.saaf.milkshop.preview (last updated October 4) and com.saaf.milkshop.qa.benchmark (October 10). No com.saaf.milkshop production package is installed. The benchmark inherits the ordinary Saaf Milk label and caused confusion.
- Do we need another app/package or a fresh audit? No. Update the existing preview in place, preserving its data. Its public saved server configuration is absent; do not enroll it or obtain credentials on the owner's behalf.
- Do we need another Viewer build/test cycle? The repaired Viewer build exists and is newer than its source inputs. Reuse it if it launches successfully with explicit local DEV settings. Rebuild only if the existing artifact fails readiness.
- Do we need more performance experiments? No. Existing device and host evidence is sufficient for this preparation; frame research remains deferred.
- Can all real account checks be completed without the owner? No. An authorized DEV login is required. Prepare the login surfaces and environment first; do not guess credentials, reuse/extract private sessions, reset accounts or provision arbitrary test identities.

## Bounded steps

1. Build the current ordinary debug preview once; verify APK package/label and install only com.saaf.milkshop.preview with an in-place update.
2. Hide the isolated benchmark, preserving its data and test evidence, so the acceptance app is unambiguous. The icon-only component change was rejected by Android; the supported package-level disable-user action succeeded. This is reversible and affects only the isolated QA benchmark.
3. Start the existing repaired Viewer locally, bound to 127.0.0.1 and explicitly targeting saafmilk-dev. Check readiness and the unauthenticated login surface. Keep privileged configuration server-side and out of evidence.
4. Smoke-check preview launch and a small existing-fixture navigation path; leave a clear starting screen and a short owner check order. Record build/install/runtime provenance separately from previous audit snapshots.

Stop when these surfaces are ready or a concrete prerequisite is identified. No PROD mutation, production publication, new broad test suite, animation retuning, history recovery or provider-policy change belongs to this pass. After two failed attempts at a step, investigate the specific failure and report the remaining prerequisite instead of expanding the task.

## Owner work comes last

Use the prepared preview for visual acceptance and an authorized DEV session for Viewer/phone workflow acceptance. Historical recovery and policy choices remain decisions, not hidden deployment prerequisites. PROD rollout stays gated on explicit approval; its earlier automatic approval rejection is unchanged.
