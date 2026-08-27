# Phase 15 — ViewModel Contract Refinement

## Status
Implemented from the last successfully verified Phase 11 ViewModel checkpoint; Android Studio verification pending.

## Purpose
Standardize the presentation-state contract of the existing ViewModels before the UI-technology decision.

## Scope
The existing six screen ViewModels now expose their immutable presentation state through `StateFlow<State>` backed by private `MutableStateFlow`. Their previous synchronous `state` accessors remain as compatibility accessors so the current XML presentation implementation does not require a broad behavioral rewrite.

Refined ViewModels:
- Purchase
- Sale
- Dashboard
- History & Corrections
- Settings / App Preferences
- Backup / Restore

## Explicit non-goals
- No business-logic changes.
- No SQL/schema changes.
- No Room/data-layer changes.
- No transaction-semantics changes.
- No preference or backup/restore semantics changes.
- No XML/layout changes.
- No navigation changes.
- No Compose or Material migration.
- No Hilt, repository, use-case, or other new architectural layer.

## Intended boundary

```text
UI
 ↓
ViewModel / StateFlow presentation state
 ↓
existing presentation/application actions and domain services
 ↓
existing data layer
```

## Verification
Android Studio / real-device execution is authoritative. Unit tests were updated to verify that observable `StateFlow` state mirrors the existing synchronous state contract. No runtime verification is claimed until the user runs the checkpoint.
