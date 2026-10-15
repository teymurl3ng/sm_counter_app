# Phase 4: Android transport and contracted posting

15 October 2026. Baseline `5eb97531b477a13fa1514b552e31066eb3f43879`. [Bounded scope/research](ANDROID_SCOPE.md). This closes the client data-path implementation at affected local Android QA scope, not hosted convergence, rollout or phase 4.

- [x] ✅ ~~Strict credit codec and authenticated HTTP client using the existing session-refresh boundary.~~
- [x] ✅ ~~Separate metadata queue/cursor, dependency-safe apply, immutable acknowledgments and retained refusals.~~
- [x] ✅ ~~Restore revalidation and identical accepted-work reconciliation; unsent restored work stays held.~~
- [x] ✅ ~~Atomic accepted-term capture, root agreement envelopes, parked incoming agreement/replay and advisory snapshot revalidation.~~
- [x] ✅ ~~23 unique affected A06 methods accepted; ordinary/acceptance/preview package provenance unchanged.~~
- [ ] Existing customer terms/status/filters, flag/history controls and selected Sale presentation, including explicit pending/held review.
- [ ] Focused hosted two-phone convergence/restore and rollout acceptance; production delivery/activation only after that gate.

## Behavior and boundaries

`CreditMetadataClient` calls the three existing status/push/pull RPCs using the stored access token and public key. Runtime serializes through its existing process gate and retries an unauthorized request once after session refresh. Unknown refresh failures retain tokens. There is no new sign-in, activation endpoint call or privileged key. An unavailable/missing credit capability does not gate an ordinary inactive shop's existing sync. A previously required contract cannot revert to legacy; a missing contract or protocol clears its usable capability and retains the requirement.

`CreditMetadataSync` uses the independent Room cursor and dedicated queue. Each received record, its identical pending acknowledgment and cursor commit together. Missing dependencies, wrong-shop/invalid sequence, unknown kinds and malformed bodies cannot advance past the refused record. Retry replays from the retained cursor; no new unknown types enter the old feed. SET precedes queued CLEAR. Matching server acknowledgments delete only the exact queued kind/UID/body; proposals become accepted heads only through a valid server acceptance. Stale/conflicting server refusals hold the proposal and persist an audit reason, and runtime reports the halt through the existing notification mechanism. The planned screen checkpoint still needs to expose and deliberately review these metadata holds; they are not silently released by this checkpoint.

Restore starts with capability zero and a full metadata replay; an interrupted validation restarts at zero. Previously accepted local revisions must all be confirmed by server history before contracted posting is enabled locally. Identical server-accepted work can clear even a restored held queue entry, including a lost acknowledgment. Unaccepted restored work remains held. New server CLEAR history is applied while concurrent SETs survive. Neither a cached backup capability nor an unconfirmed restored accepted revision authorizes new contracted posting. This does not prove every hosted restore/rebaseline scenario; the named two-phone story remains required.

Sale now reads accepted cached terms inside its existing posting transaction. Inactive shops keep explicit legacy weekly evidence. An enabled, caught-up shop records the selected accepted revision (or explicit weekly default) atomically with the sale/ledger/stock; unavailable capability rolls the sale back. Future term edits do not rewrite a root agreement. Paid-now sales have no agreement. Recorded original agreements travel inside their root event envelopes. Incoming agreements share the event savepoint; missing revision/invalid evidence parks the whole record without partial finance. Later metadata arrival enables replay without duplicate finance or an outbox echo. Corrections retain their root's agreement. The existing advisory-warning hash includes term/revision and required capability, so a changed reviewed agreement needs fresh confirmation.

This retains the agreed offline rule: terms come from the last accepted local sync; historically accepted revisions remain valid. No code here claims an offline old phone has been upgraded. There is no real activation or new UI control in this run. Room 18 and backup schema 13 remain unchanged.

## Verification

[Evidence and source hashes](ANDROID_VERIFICATION.json), [first run](android-first-results.json), [three-method fixture rerun](android-fixture-retest-results.json). **23 unique methods pass** on A06 in `com.saaf.milkshop.qa.tests`, with notifications denied. These are actual Android Room/HTTP-seam/runtime tests using synthetic in-memory data and fake HTTP; not live JWT/RPC or two-phone tests.

Twelve new methods cover default/monthly posting, future revision retention, full rollback, paid-now behavior, changed warning agreement, lost response/feed acknowledgment, stale/changed acknowledgments, malformed/fractional/foreign/dependency feed refusal, restored flag history, unconfirmed restore gating, whole-sale park/replay, SET-before-CLEAR, HTTP classification and refresh/unknown-refresh retention. Eleven selected existing methods cover legacy/recorded storage, encrypted restore gating, surcharge warning, runtime restore/entitlement boundaries, envelope profiles/children and queue transient/permanent refusal. No full test suite or visual audit ran.

Initial compilation required an explicit Unit transaction type. The first functional run passed 20/23. Three new fixtures used an invalid `cash-retail` channel, repeated a feed record behind the requested cursor, and omitted the receiver's stock. Corrected fixtures preserve the existing channel, sequence and stock guards. Only those three methods were rebuilt/retested; all passed. No production logic changed after the first functional run. The incoming-sale test explicitly verifies the missing-revision park after providing the required stock.

A06 ordinary/acceptance/preview package versions, UIDs, install/update times and APK hashes match before/after. No ordinary shop database operation or S24 operation occurred; no cloud query/migration, PROD change, actual customer contact or signed release installation occurred. Existing server/calculation/storage evidence remains credited without rerunning it. [Independent judgment record](android-judgment.json) is a fixed reminder plus scope judgment, not an external review.

## Next bounded work

Connect the already planned existing customer/Sale/history screens. Include readable status/terms explanations, Owner-only future-term and SET/CLEAR actions, explicit pending/held feedback and deliberate review of retained metadata proposals. Keep the common header/bottom geometry. No new top-level destination, global redesign, messaging or identity system. Then run only the affected screens and frozen real two-phone metadata/restore/compatibility story before rollout. Phases 1 and 5–6 remain pending; phase 4 stays open, and phases 2–3 remain accepted.
