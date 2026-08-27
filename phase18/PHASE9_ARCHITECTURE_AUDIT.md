# Phase 9 Architecture Audit

> **Superseding security amendment (2026-08-27):** portable exports now use Argon2id-derived AES-256-GCM authenticated encryption, and automatic local snapshots use AES-256-GCM with a non-exportable Android Keystore key. Security payloads exclude sessions and transient lockout timing state. The original audit below is retained as history and its statement that app-level encryption was not added no longer describes the current implementation.

## Checkpoint
Phase 9 first implementation checkpoint — backup/restore infrastructure prepared; Android Studio/device verification pending.

## GREEN
- Backup is outside the accounting core.
- Complete snapshots serialize authoritative Room state rather than inventing a second ledger.
- Planning data and app preferences are included in the snapshot.
- Restore uses a pre-restore safety snapshot before changing current state.
- Restore operations use a Room transaction and verify the reconstructed state before the transaction commits.
- Manual export uses the Android document provider, allowing a user-controlled local destination or Google Drive/provider destination.
- Backup files have explicit metadata and a SHA-256 integrity checksum.
- Restore uses dependency-aware groups instead of arbitrary individual tables.
- No app-level encryption was added, per the finalized product decision.

## YELLOW
- Scheduled cloud execution and persistent background retry/backoff are documented requirements but are not implemented in this first checkpoint; no schedule was invented.
- Automatic local snapshots are kept in app-private storage. They are useful for immediate recovery but do not survive uninstall by themselves; durable recovery is provided by exported/user-controlled backup files.
- Exact cross-version logical migration behavior beyond the current schema is not yet exercised in an Android test; the restore format rejects newer unsupported versions and accepts older schema numbers for future forward-migration handling.

## RED
None identified in the design checkpoint.

## Carry-forward from earlier audits
- MainActivity growth remains a watch item.
- Master-data duplicate-name semantics still rely on DB uniqueness/generic handling.
- Repeated master-data services remain intentionally explicit.
- Event/header ↔ ledger audit coverage remains a hardening candidate.
