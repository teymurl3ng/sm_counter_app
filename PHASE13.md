# Phase 13 — Dead, Mock, and Misleading UI Cleanup

Status: **IN PROGRESS — checkpoint 13.1 complete; checkpoint 13.2 next**

## Purpose

Remove inactive prototype code, browser-local business state, unused presentation dependencies, dead controls, duplicated paths, and misleading UI without removing a real native-backed capability or inventing new product semantics.

## Locked boundaries

- Room remains the only writable business truth; React must not store or reconstruct business records.
- Kotlin remains responsible for validation, authorization, accounting, precision, encryption, audit, backup, and recovery.
- Bridge contract v4 and its 86 operations remain frozen unless a separately approved additive capability passes its own gate.
- Git history is the archive for removed prototype code. Dead production-source files are not retained merely as historical reference.
- Placeholder text used as an input example is not mock data. Loading, empty, disabled, and permission-aware states are not dead when they truthfully represent a reachable condition.
- The current Milk/Yogurt formulas are validated scope, not a permanent product limit. Cleanup must not imply that unsupported products already have authoritative workflows.

## Ordered checkpoints

### 13.1 — Reachability and risk inventory ✅

- Trace the packaged import graph from `main.tsx`.
- Classify browser state, hardcoded business values, UI-side calculations, synthetic reports, arbitrary import/export, mock users, dead controls, obsolete tests, and misleading labels.
- Lock the removal candidates before changing source files.

Completed with 108 TypeScript/TSX files classified: 85 are reachable from the packaged entry and 23 are an unreachable 8,370-line prototype tree. Six production dependencies are unused by the active app. The active app has no browser-local business store; one misleading client-side opening-balance total requires correction in checkpoint 13.3. See `PHASE13_1_INVENTORY.md`.

### 13.2 — Remove the unreachable prototype

- Delete the 23 locked unreachable files and remove their six unused production dependencies.
- Prove the packaged entry still resolves only through the active hybrid tree.
- Rebuild and compare JavaScript/CSS/package size without claiming speed gains that were not measured.

### 13.3 — Active control and meaning audit

- Audit active buttons, disabled/permission states, success messages, presentation calculations, placeholders, and labels against their native source.
- Remove or correct any active no-op, duplicated, self-authorized, or misleading presentation.
- Resolve the heterogeneous opening-balance total without adding a new accounting formula.

### 13.4 — Negative-source and focused device gate

- Make localStorage business state, synthetic reporting, arbitrary JSON restore, obsolete shell entry, and removed dependency reintroduction fail a repeatable source/package check.
- Run focused authentication, navigation, Products, Settings/Backup, reporting, opening-state, and Phase 12 presentation device checks.

### 13.5 — Complete regression and Phase 14 handoff

- Rebuild and verify TypeScript, Vite assets, Android packaging, bridge drift, immutable hashes, host tests, and the complete physical-device suite.
- Publish removed-surface evidence, retained restrictions, known limitations, and the guarded Phase 14 device-QA handoff.

## Current next action

Run checkpoint 13.2: remove the locked 23-file prototype tree and six unused dependencies, then rebuild and verify that the active hybrid application and contract are unchanged.

## Plain English

The real app uses 85 source files. Another 23 files are an old, disconnected demo that keeps fake shop records in browser storage and calculates its own reports. They cannot appear in the installed app today, but keeping them makes the project confusing and easier to misuse later. The next step removes that dead demo safely; after that we will fix the one questionable total found in the active app.
