# UX audit evidence — 10 October 2026

Read [the findings](../../../UX_AUDIT_2026-10-10.md) and [the proposed scope](../../../UX_SCOPE_PROPOSAL_2026-10-10.md) first.

This folder contains a reproducible static inventory, not a device acceptance certificate.

Run from the repository root:

```powershell
python audit/2026-10-10/ux/source_inventory.py
```

| Artifact | Meaning |
|---|---|
| `source_hashes.tsv` | All 130 native UI Kotlin files at the time of inventory |
| `screen_inventory.tsv` | All 71 files whose names end in Screen, Screens, Shell, PreviewHost, or Activity; composables and search signals; every runtime status explicitly pending |
| `motion_inventory.tsv` | 65 transition call sites, with nearby targets, keys, and specifications |
| `feedback_state_inventory.tsv` | 24 ViewModels; receipt/success fields, reset/timer locations, warm-refresh signals |
| `branding_inventory.tsv` | Native Kotlin string literals and Android string resources containing Saaf; candidates for classification, not instructions for global replacement |
| `token_contrast.tsv` | Reproducible sRGB token contrast calculations; separates text tests from surface/boundary signals |
| `inventory_summary.json` | Counts and Git HEAD; HEAD alone does not identify the dirty working tree |

The 992 existing control rows are all labelled COVERED in the repository's earlier inventory. This audit did not rerun their device tests. That label does not prove perceptual animation, actual vibration, visual contrast, accessible feedback, or second-entry behavior.

Direct haptic call counts cannot establish which controls are silent: wrappers delegate to children and callers can supply feedback. Confirmed missing examples were inspected individually. Conversely, a haptic API call does not prove a phone physically vibrated.

Read-only ADB provenance: the designated Samsung A06 (`R8VXA02SNKH`) was connected; the ordinary package reported version 5 / 0.4.1, while the isolated QA package reported version 4 / 0.4.0-qa. Its focused window was NotificationShade and focused app was `com.saaf.milkshop/.Main`. Version numbers do not prove byte-for-byte correspondence with the dirty source. No package was installed, launched, cleared, or interacted with for this audit. No recovery secrets or screenshots were collected. Runtime visual evidence for this audit: **0**.

Only the UX audit documents and this evidence folder were created/edited by this task. Existing application edits were present before this task and were preserved.

Verification: the inventory script completed; qaFast passed frozen baselines, golden-ledger host contract, and native inventory (992 rows, untested=0, orphans=0), then failed checkUiOnly on five pre-existing changed data/domain files. No protected-logic override was used. The overall gate is not green. See the findings document for the exact files and limitations.
