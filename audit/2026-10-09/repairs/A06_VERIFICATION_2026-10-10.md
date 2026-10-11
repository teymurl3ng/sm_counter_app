# A06 repair verification — 2026-10-10

This continues the existing repair task. PROJECT_AUDIT_2026-10-09.md remains the historical audit; REPAIR_PLAN_2026-10-10.md is the current implementation/status record. No finding severity was changed. This continuation modified four test fixtures and added two instrumentation probes plus their host evidence helpers. Production source hashes match the preceding verification-final.json snapshot. Nothing was deployed.

## Device and isolation

A06: SM-A065F, Android 16, USB R8VXA02SNKH, physical 720 × 1600. It was unlocked and selected explicitly for connected tests. Only com.saaf.milkshop.qa.tests and its test APK were installed/run; the actual runner is com.saaf.milkshop.SaafMilkTestRunner. The ordinary application was not installed over, cleared, launched or restored. No live Supabase credentials or endpoints were used by the probes.

## Functional checks

| Evidence under evidence/ | Authoritative result | Meaning |
|---|---|---|
| a06-ui-first-102.xml | 97 pass, 5 failures | Incorrect merged-text/placeholder/Bank selectors and both unsupported IME actions |
| a06-ui-profile-second-104.xml | 101 pass, 3 failures | Fixture dropped navigation callback; unsupported costing IME action; boxed Int/Long mismatch |
| a06-ui-profile-final-104.xml | 104 pass, no failures/errors/skips | 102 actual UI contracts plus two observational probes; initial motion evidence later strengthened |
| a06-data-final.xml | 42 pass, 2 intentional skips; 44 cases | Bundled-driver sync/receipt/recovery/sale regressions; destructive probes correctly opt out of default discovery |
| a06-profile-first-manual-run.txt | OK (2 tests) | Full dataset profile collected; initial motion screenshot evidence inconclusive |
| a06-motion-strengthened-run.txt | OK (1 test) | Actual requested pane composes and owns the active guard for 30 transitions; distinct window screenshots |
| a06-final-static-qa.txt | QA_FAST: PASS; BUILD SUCCESSFUL | Frozen baselines, bridge/UI gates, golden contract and 992-row native inventory; authorized allow.logic exception |

These overlap prior S24 batches and are not summed as unique project-wide coverage. The data-run console prints 46 at completion because of additional skip notifications; its XML contains 44 distinct cases. Both intentionally skipped methods were executed separately with explicit crash-stage arguments.

The sale's edited rate was 100.25 and its discarded EditableText was empty in the failing-run diagnostics; field labels/supporting text and the placeholder caused incorrect merged-text assertions. Bank legitimately appeared in the paid-now radio and the payment-account selector. Backup/costing fields expose Default IME actions; tests now hide the keyboard rather than invoke a nonexistent action. The hosted test scaffolds passed an empty navigation callback into the guard; they now pass the asserted callback. EventDao.count() returns Int, so the observational dataset assertion now compares Int values. These are validation corrections, not newly inferred product defects.

## Deliberately interrupted restore

restore_process_controller.py runs opt-in RestoreProcessDeathProbe methods, waits for a readiness marker, force-stops only the QA package and starts a fresh instrumentation process. The physical fixture database path is asserted. ContextWrapper redirects the synthetic database, files, encrypted-token directory and preferences away from the QA application's ordinary stores.

- Before commit: termination happens inside the real restore transaction before snapshot verification/commit. Fresh-process verification finds the original marker vendor, no restored vendor, zero fixture app users and an unheld original queue: SQL replacement rolled back.
- After commit: termination happens after Room's transaction returns and before token cleanup. Fresh-process verification finds the restored vendor, one restored fixture owner and all restored outbox rows held: SQL replacement persisted.
- Both: needsFreshIdentity/restoredRebaseline flags and previous source device ID persist. A stale synthetic token remains loadable, but isEnrolled is false, drain returns NotConfigured and the injected HTTP client sees zero calls.

Both fresh verifications report OK (1 test). a06-restore-process-results.json preserves marker/termination/exit results; the two deliberately-killed preparation logs are interruptions, never passing tests. The real complete-snapshot serializer and restore engine were used. Safety-snapshot creation was bypassed to isolate the SQL commit boundary; preference-content restore was not selected. This does not establish physical power-loss, filesystem fsync, safety-snapshot or every restore-selection behavior.

## Dataset timings

Actual native facades and bundled Room in-memory database; synthetic owner, 100 customers, 10,000 collection events/20,000 financial legs and one stock purchase. Two warmups and 20 timed samples per operation. Successful collection/sale receipt values and final event counts are asserted. This includes each command's required receipt/context reads, but not persistent disk, end-to-end UI dispatch or production/costing workloads.

| Operation | Manual median / p95 / max (ms) | Separate Gradle median / p95 (ms) |
|---|---|---|
| Collection command + receipt | 44.1 / 78.5 / 94.1 | 72.8 / 87.7 |
| Sale command + receipt | 272.9 / 322.5 / 384.4 | 327.3 / 352.9 |
| Monthly reports | 460.9 / 530.7 / 538.2 | 524.8 / 549.6 |

Raw samples: a06-native-data-profile.json and a06-native-data-profile-from-gradle.json. Collection-heavy seeding took 16.8 / 17.2 seconds respectively. No arbitrary performance budget or improvement relative to an unmeasured baseline is claimed.

## Motion and layout

The final probe uses real ActivityScenario/Choreographer, real NativeSectionTransition, sale content and the owner report/scaffold, rather than Compose's test clock. It waits for the requested pane's actual guard identity and asserts that identity after every settled transition. PixelCopy captures only the QA activity window. a06-motion-before.png shows sale; a06-motion-after.png shows reports. Both were visually inspected. The requested 80 ms mid capture already shows the settled report; it cannot certify an intermediate animation frame. These component fixtures are not a full authenticated host workflow or human acceptance.

Final raw metrics: a06-native-motion-profile.json, 30 measured transitions, 754 observations, total-duration median 27.6 ms, p95 263.5 ms, max 328.7 ms, 36 observations (4.8%) above the corresponding Android FrameMetrics.DEADLINE. Android's reported deadlines vary; the count uses each observation's own deadline. The metrics include instrumentation/debug overhead and both animated/settled frame callbacks, so total-duration quantiles are not a certification of interactive frame rate. Animator scale was read as 1 after profiling; device settings were not changed by these helpers.

**Open performance concern:** observed debug deadline overruns need release/profileable tracing before identifying their cause, proving a production regression or changing the owner-approved 200 ms motion. Dataset latency and motion measurements do not prove release performance acceptable.

Initial UiAutomation screenshots were identical. Their files and initial frame measurements are preserved with first-unverified names; they are not accepted transition evidence. The strengthened probe replaced the capture method and assertions, without changing product animation code.

## Infrastructure failures preserved

Gradle removes QA packages/data after connected tests. Initial artifact collection therefore produced an unknown-package error and the restore controller could not launch. Direct APK installation restored only the verified QA identities. The first direct launch also used AndroidJUnitRunner instead of the registered SaafMilkTestRunner; its error is saved. Logcat truncated the initial full motion JSON at approximately 4 KB, so a successful direct probe supplied complete file artifacts. The first qaFast rerun omitted allow.logic and rejected already-authorized domain changes; the corrected gate passes. These failures remain named in evidence/ and are not product crashes or test passes.

The final evidence verifier corrected the initially understated first-run failure count to five. Final read-only diff checks pass in Android, Viewer and server; sandboxed checks for the external checkouts initially reported a work-tree error, then passed when run outside that sandbox. No repository setting was changed to obtain the result.

## Still pending

Owner visual acceptance and broader viewport/font/IME/TalkBack coverage; release performance tracing and persistent/costing dataset profiling; physical power-loss durability if required; owner-approved historical partial-event recovery; deployed schema/RPC/grant and real Auth/reconciliation parity.

Supabase project names are confirmed: saafmilk-dev is DEV and saafmilk-prod is PROD. Supabase MCP tools are not exposed in this chat. Plugin discovery/suggestion did not create a callable connection. No CLI/REST credential workaround or live mutation was attempted.

verification-a06-final.json records current Android source hashes, authoritative case counts and saved evidence hashes. It supplements, rather than overwrites, the prior verification-final.json.

## Subsequent Supabase connection — 2026-10-10

The paragraph above records the access state at the A06 snapshot. Supabase MCP subsequently became callable; read-only DEV/PROD parity checks are complete and documented in SUPABASE_VERIFICATION_2026-10-10.md. They identified S01 and missing live C1.6. No live mutation occurred. Real Auth/session rehearsal, deployment and the device/owner checks above remain pending. verification-a06-final.json is preserved as that earlier snapshot; verification-supabase-final.json records the updated reports and confirms unchanged Android source hashes.
