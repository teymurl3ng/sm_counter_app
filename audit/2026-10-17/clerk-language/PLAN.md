# Clerk language — 17 October 2026

Baseline: `6945500`. This closes the earlier deferred wording cleanup for the newly added Credit, statements/manual messages and follow-up features. It is not a whole-app rewrite.

## Observed problems and priority

1. **Actions and uncertain results:** composer, handoff, server acceptance, shared state, immutable change and local request require implementation knowledge. Long confirmation text obscures what happens next. Opening a message app cannot prove sending. Checking an unfinished action may complete its original save; removing its local copy cannot undo an online result.
2. **Settings:** “Automatic follow-ups” can suggest automatic customer sending, which is not implemented. “Posting devices,” policy and quiet-hour endpoints need ordinary words. Keep payment deadlines separate from statement reviews and alerts.
3. **Credit and history:** distinguish waiting for sync from confirmed changes; retain existing terms until confirmation. Explain marks separately from calculated debt status. Raw SET/CLEAR activity kinds need display labels. Keep staff identifiers as identifiers; identity redesign is outside this task.
4. **Statement and draft text:** replace ledger movements, reconciliation and partial coverage with plain descriptions. Already clear labels such as Credit, statement, payment terms, weekly/monthly/custom and amounts stay.

## Chosen wording

Use message app / message attempt, synced records, unfinished action, Waiting for sync, Remove from this device, Owner review mark, and Draft & share history consistently. The switch becomes Follow-up reminders, alongside the existing explicit manual-send explanation. It covers the follow-up list and owner alerts; calling it only SMS or only notifications would be inaccurate.

The exact old/new inventory is recorded in COPY_MAP.json. Reusable native components cover their customer entry routes. In-scope source files are pinned in apply-copy.py, plus a display-only Credit action-label mapping. No resource architecture refactor, translated language, PDF template redesign, sync protocol, permission, calculation, schedule, database or layout changes.

## Research and judgment

[Microsoft's writing guidance](https://learn.microsoft.com/en-us/style-guide/word-choice/use-simple-words-concise-sentences) supports simple, precise words and concise sentences. Adapt that guidance to a clerk who needs the action and result, rather than replacing financial concepts with vague shorthand.

The independent-judgment reminder hook was run. Source inspection supports the jargon findings; the hook is not an independent reviewer. Shorter is not always safer: keep the duplicate-contact warning, reports-versus-proof distinction, missing offline payments, incomplete overdue records, and the difference between checking and removing an unfinished action. Mode labels used as state keys, wire enums, audit values, tags and stored messages must not change. Existing history receives clearer display labels only.

## Fixed acceptance and stop line

- Review the full source diff and verify it matches the copy map plus the small history-display mapping.
- Build only the isolated QA app and its test APK. Run the pinned affected UI/data methods on one available QA phone; include light, dark and 200% text, save recovery and uncertain message review. Never install/reset the real shop app for this pass.
- Inspect representative captures. Update existing selectors where visible words changed; do not weaken behavior assertions.
- No PROD/DEV migration rerun, live customer contact, two-phone concurrency rerun or whole-app suite. New unrelated findings go to LATER.
- Record results and commit/push the bounded change. Signed ordinary delivery remains phase 1.

Progress: ✅ ~~baseline, research and judgment~~; ✅ ~~wording implementation and display labels~~; ✅ ~~24 distinct affected checks across bounded runs and visual review~~. [Completion evidence](RESULT.md). Ordinary signed delivery remains phase 1.
