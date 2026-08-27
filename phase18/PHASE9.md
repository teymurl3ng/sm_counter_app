# Phase 9 — Backup & Restore

> **Superseding security amendment (2026-08-27):** the approved Phase 3 account-security work adds implemented encrypted portable format-2 exports and Android-Keystore automatic local snapshots while retaining legacy format-1 imports. The original Phase 9 text below is retained as implementation history.

Current status: **first implementation checkpoint prepared; Android Studio/device verification pending.**

The phase protects the complete application state without changing the accounting core. Business data, planning data, settings, and app preferences are serialized into timestamped backup snapshots. Backups are integrity-checked. Restore uses explicit warnings, a pre-restore safety snapshot, compatibility checks, dependency-aware group selection, and post-restore verification.

The first checkpoint provides durable user-controlled export through Android's document provider (including Google Drive where available) and automatic local snapshots after transaction/correction save flows. Scheduled cloud background execution and persistent retry scheduling remain the next backup implementation checkpoint because the exact schedule is not a business rule and should not be invented here.
