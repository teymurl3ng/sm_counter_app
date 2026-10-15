# Phase 4 data contract — checkpoint 1

15 October 2026. Implementation baseline `08d0e28149f02acb4b11778d996618c21825cb4b`. The owner's “let's begin” follows the plan/research discussion and is authorization to implement its proposed term meanings/status bands. This file freezes the next persistence/transport work. **None of these database/API additions is deployed by this checkpoint.**

## Implemented calculation boundary

`CustomerCreditAging.allocate` returns reconciled remaining visible-paisa lots keyed by stable root UID, preserving the existing `calculate(...): CreditAge` API and FIFO/rounding rules. `CreditPaymentTerm` supports days 1–365 and exactly one calendar month. `CreditSaleAgreement` is an immutable root-sale/customer/revision/term/version value. `CreditTermEvidence` distinguishes a recorded agreement, explicitly identified legacy weekly debt, and unavailable data. Missing evidence never silently becomes seven days.

`CustomerCreditStatusCalculator` checks every remaining lot's own deadline. It preserves partial known overdue amounts under Needs review; Settled/Advance and the Owner flag remain separate. It implements the agreed Watch/1–14 days Late/15+ days Bad boundaries. These are current-debt policy, not learned reliability scores.

`CreditFlagReviewCalculator` projects immutable SET/CLEAR actions independent of arrival order or phone-clock winners. Repeated identical bodies are harmless, reused identities with different bodies are refused, missing SET dependencies remain explicitly incomplete, and a concurrent SET survives a clear of previously observed SETs. This is a pure projection, **not** permission enforcement, persistence, transport or a claim of multi-phone acceptance.

## Verified DEV server boundary

A single read-only catalog query inspected `public.sync_push` and `public.sync_pull` on DEV `tghzmofkkdsaasvnixps`. Their definitions/hashes and inspection scope are recorded in `DEV_CONTRACT_BASELINE.json`. No customer records, credentials or live financial rows were queried. No PROD parity is claimed.

- Current push arguments: `envelopes jsonb, device_id text, shop_id uuid, pull_cursor bigint`. It checks a fixed record-type allowlist; new metadata types are rejected. Current append-only handling audits changed bodies and continues, so it must not be copied as a successful credit-metadata conflict outcome.
- Current pull arguments: `since_seq bigint, max_rows integer, shop uuid`; no client capability/version argument. The app's `SyncApplyEngine` skips unknown table types. Merely adding new global-feed rows would let an old phone advance past them.
- Device ID is a caller-supplied string. It is not a verified device inventory or proof every offline writer is upgraded. This work does not invent a device-revocation system.

Therefore metadata must not be exposed/enabled on the old protocol by simply changing an allowlist. Keep customer terms/flags editing and new Sale metadata disabled until the following bounded server/client contract is implemented and checked.

## Persisted data and identity

Use stable UIDs in transport/backup, with local IDs only as resolved Room foreign keys. Do not store derived status, overdue totals, computed deadlines or a second balance.

1. **Customer term revisions:** immutable accepted revisions containing revision UID, customer UID, prior revision UID, term kind/value, contract version, local audit actor and server acceptance stamp. A separate current-head read identifies the accepted revision. Compare-and-swap against the reviewed head; a stale write returns `credit_terms_revision_conflict` without promoting the proposal. Only a server-authorized shop Owner may change terms. An unconfirmed edit is visibly pending and cannot become the effective sale preference. The weekly default has an explicit reserved revision identity.
2. **Sale agreement:** local immutable satellite keyed by root SALE UID, customer UID, revision UID, term kind/value and version. Send it **inside the root sale's event envelope**, so the server receives the financial event and its agreement atomically. It is not an independently queued row that can be acknowledged while the sale fails. Corrections reference/retain the root agreement. An existing UID/body retry is accepted idempotently; a different agreement for that UID is refused as `credit_agreement_conflict` and retained for review.
3. **Review actions:** immutable action UID, customer UID, SET/CLEAR, referenced SET UID for CLEAR, reason (nonblank, at most 500 characters), local actor UID, trusted recorded timestamp/business date and contract version. Owner-only writes, shop-scoped reads for the existing permitted readers. Repeated identical action is idempotent; different body for the same UID is `credit_review_identity_conflict`. CLEAR targets exactly the SETs reviewed by the Owner; clearing several creates several linked actions in one local transaction. Dependency absence parks/retries rather than losing history.

The local actor UID is an audit label. The server stamps its authenticated shop/actor separately; it must never authorize from a client-provided role/actor. Local Owner-only facade/service checks remain required even on an Owner-enrolled transport. A cashier transport may write its allowed original sale plus agreement, but cannot change customer terms or flags.

## Protocol and rollout

Use a versioned credit metadata RPC/feed separate from the old global cursor for accepted term revisions and review actions, with an independently persisted cursor and full initial catch-up from zero. Pin actual SQL names to the existing backend conventions when creating migration source; proposed API names are `credit_metadata_push_v1`, `credit_metadata_pull_v1`, and `credit_contract_status_v1`. A small shop-level contract record reports support/enabled state; failed/missing capability reads mean unavailable, not enabled. Do not create extra unverified identity/device administration.

The existing `sync_push` must validate the versioned agreement inside new credit SALE envelopes, including shop/customer/event identity, valid terms and accepted preference revision. A historical accepted revision remains valid for an offline sale; a later preference edit cannot rewrite it. UI explains that offline terms come from the last accepted sync. An edited term is not silently assumed to have reached an offline peer.

Activate advanced terms only after the intended posting phones are updated, caught up and their behavior is checked. Once activated, the server must refuse a **new** credit sale lacking the required contract (`credit_contract_required`), while preserving safe identical retries of already-known legacy events and paid-now behavior. Such refusals are held visibly, never silently relabelled or dropped. A server cannot stop an already-offline old binary from recording locally: the rollout must explicitly keep those old phones out of use until updated. Do not claim stronger enforcement without a separately scoped device-control design. No existing shop is activated by this checkpoint.

All new original credit sales on the updated enabled client get an agreement atomically with their financial posting, using the accepted local revision read in that transaction. Include the selected term/revision in the existing advisory-warning snapshot so a changed reviewed agreement requires fresh review. No new global warning or sales block based on Bad status. If required agreement metadata cannot be saved, the sale rolls back. If capability/terms are unknown for an enabled contract, explain the unavailable agreement instead of silently posting a seven-day fallback.

## Legacy, restore and rollback

Before first activation classify existing root credit sales as legacy weekly explicitly; retain that classification across backup/sync and cross-phone UID remapping. Do not infer legacy merely from a missing satellite on a newly contracted event. Old-backup restore requires its known older schema/version to establish legacy; malformed new-contract events require review.

Next Room schema is 18 if no intervening migration appears; backup schema becomes 13 (currently Room 17 and backup 12). Export/import accepted revisions, current heads, agreements, review actions, explicit legacy classification and the new cursor/pending state where appropriate. Preserve existing backup format/encryption and stable references; the existing newer-schema refusal must protect older readers. Add forward migration without changing prior ledgers. An app downgrade after contract activation is not a supported rollback: server refuses missing-agreement writes; instructions require the compatible app. Disabling future new terms must not delete agreements or flags already recorded.

## Next implementation checkpoint and exact acceptance

Implement the above Room/server/envelope/backup paths together before surfacing controls. Refresh the exact current backend migration source and official Supabase docs before DDL, use DEV/synthetic fixtures first, and retain before/after definitions. No PROD migration or shop activation is implied by this source checkpoint.

Focused acceptance must cover compare-and-swap refusal, permission/shop denials, atomic sale+agreement rollback/retry, stale warning agreement, flags in both arrival orders, missing dependencies, legacy catch-up, old/new backup and older-client required-contract rejection. Two-phone checks follow only when the persistence/transport slice is ready. Stop if implementation requires new device identity/revocation architecture; bring that concrete boundary back for scope judgment rather than expanding the phase.
