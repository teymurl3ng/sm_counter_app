# 8b2 completed — backup automation, 13 October 2026

Published exactly the workflow, paginated collector and operator note to private account-server main, commits `871c01f58b427967ac0b4d99c9118960d2aeaafa` and `802ec5009cf767dc588f87439d6edb47b5ac79ec`. The latter adds only three explanatory/precise Semgrep annotation comments at the validated URL call. No unrelated server changes were published. The three final files are synchronized to the existing local server checkout with baseline hash guards; unrelated inspected sources and its checkout HEAD were preserved.

The workflow now switches to the backup branch before creating its unique folder, paginates all six tables and Auth users, accepts supported privileged key formats, restricts the URL to the expected PROD HTTPS project, validates its complete manifest/counts/hashes, and stages only that new folder. Weekly cadence is retained and writers are serialized. A failed collection cannot publish a partial snapshot.

## Acceptance

- Eight new focused collector checks passed, alongside five existing pagination/branch-preservation checks. The initial local test attempt failed because the sandbox denied temporary files outside the workspace; the corrected workspace-local run passed. No Android or broad suite ran.
- [First live run](https://github.com/teymurl3ng/saaf-milk-account-server/actions/runs/38363862127) succeeded at `871c01f`; [second live run](https://github.com/teymurl3ng/saaf-milk-account-server/actions/runs/38364747021) succeeded at final `802ec50`. They execute the same collector behavior; the source comments are the only difference.
- Both distinct saved folders contain seven complete payloads and a manifest. Actual private payload hashes/counts were checked in memory against each manifest; source SHA and successful run provenance match. Each has 1 business, 2 shops, 3 staff, 2 assignments, 50 audit events, 0 reconciliation rows and 3 Auth users.
- The newest verified snapshot finished at **07:46:04 UTC / 12:46:04 PKT on 13 October**. It was under two minutes old at the verification recorded in `remote-snapshot-verification.json`.
- All seven Git blob hashes in the 9 October recovery point still match the preflight baseline. No private identity/Auth rows, finding values, API keys or tokens were saved in this app repository.

## Limits and deferred findings

The [final security workflow](https://github.com/teymurl3ng/saaf-milk-account-server/actions/runs/38364408444) is **not green**: Bandit and pip-audit pass; Semgrep still flags the existing `smoke_prod.py` dynamic URL call, rather than the new collector. Five distinct Gitleaks flagged values were compared in memory: three exactly match historical staff `auth_user_id` UUIDs; two exactly match manifest SHA-256 checksums for Auth users and reconciliation. These are identified scanner false positives, not evidence of exposed credential values. Scanner configuration/history was not changed. A separate narrow scanner-classification fix can be considered later; it does not alter these two successful backup results.

Snapshots remain sequential/nontransactional identity captures, not atomic database dumps, app ledger backups or accepted portable identity restores. No schema write, password reset, identity restore, DEV wipe, Android release or notification-preference change ran. Failure detection and off-machine access guidance are in the published `IDENTITY_BACKUP_OPERATIONS.md`; account notification settings remain an operator setting.

**Stop condition reached. Next existing unit: 8b3 safe identity recovery repair/rehearsal**, with explicit rejection of the current in-place wipe path, synthetic identities, preserved stable IDs/audit actors, validated snapshot conversion and an empty isolated destination. Real Auth acceptance must be distinguished from fake-gateway tests. No shared DEV/PROD restore is authorized by this result. App restore (8b4), shop setup (8c), reset browser/production acceptance, phase 5 and phase 6 remain separate.
