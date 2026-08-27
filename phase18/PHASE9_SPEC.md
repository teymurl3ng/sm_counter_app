# Phase 9 — Backup & Restore: Finalized Specification

> **Superseding security amendment (2026-08-27):** Rule 17 below describes the original pre-account Phase 9 contract. The approved Phase 3 user-security contract now implements Argon2id-derived AES-256-GCM portable format-2 exports and Android-Keystore AES-256-GCM automatic local snapshots. Legacy format-1 imports remain supported. See the root `PHASE3_USER_ACCOUNTS_DESIGN_PROPOSAL.md` and `PHASE3.md`.

## Purpose
Provide complete, recoverable snapshots of application state so a fresh installation/reset can be restored and continue operating from the saved state.

## Finalized rules
1. Back up the complete application state: business data, settings, planning data, and app preferences.
2. Keep both durable local/user-controlled backup files and Google Drive/cloud backup capability.
3. Backups are full snapshots; normal backup does not ask the user to choose tables or categories.
4. Backup files are timestamped and named `SHOP_NAME_YYYY-MM-DD_HH-MM-SS.milkbackup`.
5. No automatic retention limit or pruning for now.
6. Manual backup is available.
7. Automatic local snapshots may be created after successful business transactions; cloud backup is intended to run in the background/scheduled path. The exact cloud schedule is an implementation detail of this phase and is not a business rule.
8. Backup integrity must be verified before a snapshot is marked usable.
9. If backup fails, the app clearly reports the failure, the reason, and the recovery action; the previous valid backup remains intact.
10. Automatic backup failures retry with increasing delay/backoff; persistent failure remains visible and escalates to manual action rather than silently disappearing.
11. Full restore replaces current application state. There is no merge/import mode in Phase 9.
12. Selective restore uses dependency-aware groups rather than arbitrary raw tables.
13. Before restore, create a pre-restore safety snapshot automatically.
14. Restore requires a detailed warning and exact typed confirmation: `I understood the consequences and I agree to proceed forward`.
15. Restore verifies backup compatibility before replacement and verifies the restored state before reporting success.
16. Older backups may be migrated forward when a verified migration path exists. Backups created by a newer unsupported format/schema are rejected and the user is instructed to update the app.
17. No app-level backup encryption. Device and Google account security are relied upon.
18. App preferences are included in complete backups.
19. Restore failure must explain what failed and what recovery action was taken; current valid data must remain recoverable through the pre-restore safety snapshot.
20. This phase does not alter accounting meaning or create a second source of truth. Backup serializes/restores existing authoritative state.

## Architectural boundary
```text
Authoritative Room/database state
        ↓
Backup serialization + integrity layer
        ↓
Local snapshot / user-selected document provider (including Google Drive)
        ↓
Restore validation → pre-restore safety snapshot → transactional restore → verification
```

## First implementation checkpoint
This checkpoint adds:
- complete logical backup serialization;
- timestamped local snapshots;
- manual export through Android's document provider (local storage or Google Drive);
- backup integrity verification;
- full and dependency-aware selective restore;
- pre-restore safety snapshot;
- compatibility metadata;
- explicit restore warnings and typed confirmation;
- automatic local snapshot requests after transaction save/correction flows.

Cloud scheduled execution, persistent retry scheduling, and boot-resilient background execution are intentionally documented as the next implementation checkpoint rather than silently guessing a schedule.
