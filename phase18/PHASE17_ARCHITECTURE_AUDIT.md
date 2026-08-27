# Phase 17 Architecture Audit — 17.6 Checkpoint

## Scope

This audit checkpoint covers the remaining direct application/data orchestration previously found in `MainActivity`.

## Boundary expectations

`MainActivity` may:
- parse user input;
- construct UI dialogs and callbacks;
- wire repositories/ViewModels/UI helpers together;
- coordinate UI refreshes after successful operations.

`MainActivity` must not directly construct or invoke the database-backed services/managers below for migrated operations:
- `BusinessSettingsService` for settings persistence;
- `VendorManagementService` / `CustomerManagementService` / `EmployeeManagementService` for management operations;
- `CorrectionService` for correction execution;
- `BackupManager.restore` / `BackupManager.createLocalSnapshot` for database-backed backup operations.

## 17.6 implementation result

The four remaining operation groups are behind:
- `BusinessSettingsDataRepository`;
- existing `ManagementDataRepository`;
- `CorrectionDataRepository`;
- `BackupDataRepository`.

The existing domain services and `BackupManager` remain authoritative. No business logic was duplicated or rewritten in the repositories.

## Verification status

Static source audit: PASS for removal of the four direct operation calls from `MainActivity`.

Runtime verification: PENDING Android Studio execution of the Phase 17.6 checkpoint.

After runtime verification, 17.7 should perform the final project-wide boundary audit before Phase 17 is closed.
