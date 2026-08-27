# Phase 0 — Freeze & Baseline

Status: COMPLETE

This project is the clean working baseline for the real hybrid rebuild. The original Phase 18 checkpoint and the original AI Studio UI source were received and copied without modifying their source content before this phase's project-governance files were added.

## Immutable Business Baselines

Logic specification: `phase18/milk-shop-core-logic-spec-v2.md`
SHA-256: `45e5bddabe8861a1b3df7c5666ac7673078439a89fa5628061ad293a865f9dea`

SQL blueprint: `phase18/milk_shop_schema_sql_only_final_v2.sql`
SHA-256: `fb701c90d2dd97a1e6d962154692c48ea0f4e77e9300f9dfac653d4ab67bbc51`

These two files are immutable unless the user explicitly authorizes a business/domain/schema change and the change is documented.

## Source Checkpoints

Original Phase 18 checkpoint ZIP SHA-256:
`527364fd49cd1def5baabfdf9399aef1d6b4f835b7471f484c33fd8c466fc426`

Original AI Studio UI source ZIP SHA-256:
`023ba6a1610a10e6d891a759904e44591c9279e47a861976ffc7ebce45e03b0f`

Locked postmortem/playbook SHA-256:
`11382ed2c57a3d18bfb3e7fa9743af08b36a2d792e4466e24cbb2cfe798043bc`

## Phase 0 Rule

The files under `phase18/` and `ui/` are the source baselines. Do not silently replace them with a prior hybrid/test-project checkpoint.

The original Phase 18 project already contains its own historical `PHASE*.md` files. Those describe the Phase 18 product-development history and are not replaced by the hybrid rebuild roadmap in the project root.

## Next

Phase 1 — Full AI Studio ↔ Phase 18 Capability Inventory.
