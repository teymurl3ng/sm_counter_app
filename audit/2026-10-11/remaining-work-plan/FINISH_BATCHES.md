# Seven finite finish batches

These are the reporting units. FLAT_CHECKLIST.md retains their substeps, not 38 separate jobs. Cross out a whole batch only after its named scope is accepted. Leaving tests for later defers acceptance checks, not unfinished engineering. App Git publication is authorized by the user's commit-and-push instruction; package installation/release and Viewer publication remain paused.

1. [x] ~~Fix closing sync and retry.~~ R01-R02. Accepted 14 October: ten targeted A06 checks. [Result](../../2026-10-14/close-sync-repair/RESULT.md).
2. [x] ~~Test offline sales and staged closing across both phones.~~ R03-R10. Accepted 14 October: eight 20-table checkpoints and actual A06 outage/restoration. [Result](../../2026-10-14/physical-offline-close/RESULT.md).
3. [x] ~~Test interrupted sync/recovery.~~ R11. Accepted 14 October: seven focused controls and six 20-table physical checkpoints for missed-response/fresh-process retry and newly emitted queued closing history. Old affected feeds retain conditional C03. [Result](../../2026-10-14/sync-recovery/RESULT.md).
4. [ ] Staff and human phone checks. R12-R21. Preparation on A06/S24 and named visual reviews completed; uncoached staff/remaining human checks are deferred, not accepted. [Start here](../../2026-10-14/staff-phone-handoff/START_HERE.md).
5. [ ] Hosted server recovery and tool publication. R22-R24. Recovery/protected-reset tooling published to account-server main (c21b983). Hosted restore/sign-in verification remains deferred; test deferral does not approve an isolated project, downtime or cost.
6. [ ] Password-reset browser verification and rollout. R25-R28. Earlier DEV checks remain credited. Exact PROD guarded-reset migration applied; reviewed Viewer production build ready. Final Viewer/source publication remains unfinished and paused. Browser/live PROD acceptance remains deferred, not accepted.
7. [ ] Signed release and handover. R29-R38. App source recorded in Git (565e747 plus the layout repair); 0.4.2 signed candidate built. Installation, final handover, real setup/custody and upgrade/business acceptance remain open. That candidate predates the layout/keyboard repair and must be rebuilt before representing that fix. [Repair result](../../2026-10-14/layout-keyboard-audit/RESULT.md): complete active native source map and ten-file bounded repair; Kotlin compilation passed; device acceptance deferred. This presentation prerequisite is inside step 7, not a new finish batch. Git publication does not install an updated APK or accept a release.

No fresh functional tests or release installation/publication ran in the layout repair. See its [focused checks for later](../../2026-10-14/layout-keyboard-audit/CHECK_LATER.md) and the [existing deferred acceptance checklist](../../2026-10-14/finish-engineering/TEST_LATER.md).

Conditional/future work keeps its original conditions. Newly observed unrelated issues must be noted without expanding this seven-batch scope.
