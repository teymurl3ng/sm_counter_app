# Ready-to-test results — 11 October 2026

Scope is closed after one full A06 run and one correction batch/rerun. Capsule/menu styling is deferred; phones remain portrait-only.

- qaFast: PASS. Inventory: 1,001 controls, untested 0, orphans 0.
- Full qaCheckpoint: FAIL, 594/596 passed. Do not relabel this full run PASS.
- Focused qaScreen[uxready]: PASS, 8/8, including both formerly failing cases.
- Corrections changed test fixtures and the focused Gradle group only. Cashier-close testing now restores the date through authorized Owner reopen and retains the close audit. Collection retention waits for keyboard dismissal and checks the actual submitted destination and amount. Production source was unchanged between the runs.
- Five protected files match the approved baseline. Deferred capsule/theme files remain unchanged.
- Visual acceptance: pending Owner review, 14 PNGs/14 UNREVIEWED and a finalized 60-second re-entry MP4. Automated results do not establish felt haptic quality.

Full-run evidence is retained under checkpoint-evidence/checkpoint-current. Canonical phase14-evidence/checkpoint-current contains that run plus the focused collection export; provenance remains in the separate result folders. The full checkpoint was not repeated: this is a bounded QA handoff, not a claim of release approval.

Interactive QA package: com.saaf.milkshop.qa, launcher Saaf Milk (preview). Installation updates the existing QA profile with data preserved. Ordinary com.saaf.milkshop is excluded. See TEST_ON_PHONE.md for the bounded device checklist. Installation/build proof is recorded separately in handoff.json.
