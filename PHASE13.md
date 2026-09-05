# Phase 13 — Dead, Mock, and Misleading UI Cleanup

Status: **COMPLETE — checkpoints 13.1–13.5 complete**

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

### 13.2 — Remove the unreachable prototype ✅

- Delete the 23 locked unreachable files and remove their six unused production dependencies.
- Prove the packaged entry still resolves only through the active hybrid tree.
- Rebuild and compare JavaScript/CSS/package size without claiming speed gains that were not measured.

Completed exactly against the 13.1 removal lock. The 23-file, 8,370-line prototype and six unused direct dependencies are gone; the import graph is now 85 reachable files / 85 total / zero unreachable. TypeScript, Vite, Android packaging/compilation, 7/7 host tests, and a 12/12 Samsung login/Product/navigation/responsive gate pass. Active JavaScript retains the exact Phase 12 content hash, while generated CSS falls 60.3% because dead prototype classes no longer enter Tailwind's scan. See `PHASE13_2_PROTOTYPE_REMOVAL.md`.

### 13.3 — Active control and meaning audit ✅

- Audit active buttons, disabled/permission states, success messages, presentation calculations, placeholders, and labels against their native source.
- Remove or correct any active no-op, duplicated, self-authorized, or misleading presentation.
- Resolve the heterogeneous opening-balance total without adding a new accounting formula.

Completed across all 85 active TypeScript/TSX files. A TypeScript AST audit found 120 literal buttons and zero disconnected controls; permission, disabled, placeholder, completion-message, and display-arithmetic paths remain tied to native authority or presentation-only behavior. The misleading unsigned opening-balance total was removed and replaced with an explicit explanation that cash/bank, customer, and supplier meanings remain separate. TypeScript/Vite, bridge verification, Android compilation, 7/7 host tests, and 4/4 focused Samsung opening-state tests pass. See `PHASE13_3_ACTIVE_UI_AUDIT.md`.

### 13.4 — Negative-source and focused device gate ✅

- Make localStorage business state, synthetic reporting, arbitrary JSON restore, obsolete shell entry, and removed dependency reintroduction fail a repeatable source/package check.
- Run focused authentication, navigation, Products, Settings/Backup, reporting, opening-state, and Phase 12 presentation device checks.

Completed with an executable cleanup gate enforced by npm/Vite, the post-package script, and Android `preBuild`. It rejects all 23 removed paths, the six removed dependencies, browser-local business storage, synthetic reports, old browser import/export/clear-all paths, the obsolete shell/store, and the misleading opening-balance total in source or packaged assets. Its self-test passes, and the isolated-QA Samsung gate passes 31/31 across authentication, Products, Settings/Backup, reporting, opening state, navigation, presentation, and responsive/accessibility behavior. See `PHASE13_4_NEGATIVE_GATE.md`.

### 13.5 — Complete regression and Phase 14 handoff ✅

- Rebuild and verify TypeScript, Vite assets, Android packaging, bridge drift, immutable hashes, host tests, and the complete physical-device suite.
- Publish removed-surface evidence, retained restrictions, known limitations, and the guarded Phase 14 device-QA handoff.

Completed against freshly rebuilt final assets. The cleanup guard self-test and source/package scans pass; bridge v4 remains at 86 operations; TypeScript/Vite, packaged-asset parity, Android packaging, Kotlin production/instrumentation compilation, immutable hashes, and 7/7 host tests pass. The Samsung SM-A065F / Android 16 complete suite passes 258/258 across the authenticated 257-test and empty-identity 1-test visual partitions, with zero skips or failures. The visual partition preserved 83 readable 720 × 1600 screenshots / 58 states. See `PHASE13_5_HANDOFF.md`.

## Current next action

Begin Phase 14 final device QA from `PHASE13_5_HANDOFF.md`: validate the complete role/permission bypass matrix, real Google Drive backup and retry behavior, accessibility and broader-device behavior, and safe full replacement restore on a second physical device without changing the frozen business boundaries.

## Plain English

Phase 13 is finished. The disconnected demo is gone, every live control has been checked, the misleading opening total is corrected, and automatic build rules stop the old behavior from returning. The final app passed all 258 phone tests plus 7 computer-side tests, and all 83 review screenshots were captured and validated. Next is real-device acceptance in Phase 14.
