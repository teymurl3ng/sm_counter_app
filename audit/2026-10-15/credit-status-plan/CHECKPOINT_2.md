# Phase 4: local metadata, permissions, migration and backup

15 October 2026. Source baseline `985c0939683c00c63f6df2a7256bb983fa396b17`. This is the next bounded implementation checkpoint of phase 4, **not completion of the phase or activation of advanced terms**.

- [x] ✅ ~~Local storage for accepted revisions/heads, sale agreements, review actions, pending metadata and separate sync state.~~
- [x] ✅ ~~Owner-only command boundary, pending-term behavior and immutable identity checks.~~
- [x] ✅ ~~Room 17→18 migration and explicit legacy agreement capture without changing ledger truth.~~
- [x] ✅ ~~Backup schema 13 export/restore, strict validation and recovery gating.~~
- [x] ✅ ~~Affected isolated A06 verification.~~
- [ ] Compatible server/client metadata transport and contracted sale posting.
- [ ] Existing customer/Sale/history screens and final affected two-phone acceptance.

## Implemented behavior

Six purpose-specific Room tables store accepted term revisions and current heads, immutable root-sale agreements, immutable SET/CLEAR review actions, a dedicated pending queue and a separate metadata cursor/contract marker. Financial balances, deadlines and calculated status are not stored. Stable customer/event UIDs avoid depending on local numeric IDs for metadata references.

`CreditMetadataStore` validates customer identity, root-sale financial destination, term/revision consistency and action dependencies. Same identity/body is idempotent; changed identities/bodies and stale accepted-head advancement are refused. Replaying an older accepted revision cannot move the current head backwards. SQL triggers prevent update/delete of accepted revisions, agreements and review actions; restore temporarily suspends them within its transaction and reinstates them before commit.

`CreditReviewService` enforces local Owner role in addition to ordinary permissions. Manager/Admin master-data permission does not authorize term/flag writes. Pending term proposals do not change the accepted head. SET/CLEAR actions and their dedicated pending entries commit together; a failed multi-clear rolls back the entire operation. The queue is deliberately separate from the existing sync allowlist and is **not dispatched yet**. Local actor IDs remain local audit descriptions, not server-verified person identities.

Room migration 17→18 classifies unambiguous existing original customer sales as explicit legacy weekly evidence. Current ordinary credit posting captures the same evidence atomically with its sale. A metadata failure rolls back the event, inventory and financial rows. Advanced contracted posting is intentionally unavailable pending transport integration: an enabled-contract marker prevents silently falling back to legacy seven-day sales. No UI or runtime path enables that marker in this checkpoint; tests set it only in isolated fixtures.

Legacy records arriving later through the old pull path are not guessed into contractual metadata here. Their explicit legacy classification/catch-up is part of the pending versioned sync work, before status/terms controls are enabled.

Backup schema is now 13; backup format/encryption are unchanged. Credit records are exported/restored with business data. Actor/review/accepted contractual history requires a security-bearing encrypted backup, consistent with existing sensitive-history rules. Legacy-only ordinary export remains supported. Restoring known older snapshots explicitly classifies their legacy sales. Missing metadata collections in a new-schema snapshot are not treated as an empty history. New fields reject fractional/coerced numbers, wrong types, cycles, duplicate identities and inconsistent references; errors roll back the existing restore transaction.

Restore preserves the required-contract marker but clears cached capability and its metadata cursor, and holds pending entries for revalidation. Thus a valid restored Owner cannot resume metadata edits merely because an old backup said support was enabled. Restore does not claim server convergence: reconciling a restored flag set with newer server actions is still part of the pending sync work.

## Verification

**15 unique affected A06 methods pass** in `com.saaf.milkshop.qa.tests`, with notification permission denied and disposable QA data cleared before instrumentation. [First results](storage-first-results.json), [strengthened restore gate](storage-gate-results.json), [verification/source hashes](STORAGE_VERIFICATION.json). The first complete functional run passed all fifteen. Only the encrypted round-trip method was repeated after adding an assertion for a valid restored Owner's capability gate. No full suite or visual audit ran.

Checks cover actual Room migration validation/legacy backfill and raw ledger preservation; stale revisions/replay/SQL immutability; Owner versus Manager/Admin/expired sessions; pending terms; immutable recorded and legacy agreements; rollback on failed metadata; duplicate and concurrent flag actions; multi-clear rollback; encrypted round-trip of both legacy and recorded monthly agreements/history/pending state; malformed/fractional/newer-schema rejection; old-schema restore; existing backup receipts; and affected Sale surcharge/aggregate/correction/default-payment behavior.

An initial compilation error used Room's SQL helper with the wrong receiver; the restore helpers now execute through the caller's existing transactional connection. The schema change required regenerated Room/test compilation. No compiler/dependency upgrade, unrelated warning cleanup or broad test expansion was made. Source contains the generated schema 18 JSON required for migration verification.

No server query/migration, PROD change, real shop/customer write, ordinary signed app update or new screen behavior occurred in this run. The A06 ordinary app retains its previous accepted version. The earlier 17 calculation checks remain credited; their unchanged pure calculation sources were not rerun without cause.

## Next bounded work

Implement and verify the already-pinned [versioned server/client contract](DATA_CONTRACT.md): metadata catch-up and acceptance, explicit permission/shop boundaries, atomic contracted sale envelope and old-client refusal after activation, restore reconciliation. Then connect the existing screens and run their affected acceptance. Do not enable the feature or mark phase 4 done on the strength of local-storage tests alone. Phases 1/5/6 remain pending and phases 2–3 stay accepted.
