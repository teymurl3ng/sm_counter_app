# B3b — withdrawal account correction verified

The scoped account-correction foundation is complete. Owner/Admin corrections can change a withdrawal's declared Cash/Bank account, optionally changing its amount. A changed account restores the old amount to the old account and deducts the new amount from the selected account. Later amount corrections and cancellation use the latest declared account. Original event and ledger facts remain in history. Genuine returned money remains a separate pending step.

For the literal fixture Cash Rs 1,000 and Bank Rs 500, an original Cash withdrawal Rs 250 leaves Cash Rs 750. Correcting it to Bank Rs 200 restores Cash to Rs 1,000 and leaves Bank Rs 300. A later Bank correction to Rs 150 leaves Bank Rs 350; cancellation restores Bank to Rs 500. No sale, operating expense, profit or inventory movement was added.

Room 14 -> 15 adds three-field immutable withdrawal detail and guarded Cash/Bank movement rules. Supported older same-account chains are backfilled without changing their saved events, UIDs or money rows. Ambiguous older history gains no guessed account. Its Records detail remains readable, says **Account needs review**, and disallows correction until the history can be resolved. The existing Records correction prefill now shows the latest account if an account correction already exists; the selector itself belongs to B3d.

Portable backup schema 11 carries the detail. Supported older backups infer only unambiguous history; backups with missing required new detail or incomplete cash movements fail atomically. Local UID replay remaps accounts and roots, validates the full financial movement set, repairs identical missing detail/child prefixes, parks missing dependencies and malformed or unsupported owner-funds bodies for attention, and avoids re-pushing applied rows. An older writable client must not be assumed compatible with this new shape.

## Focused verification

- **23 distinct latest passing A06 device checks**, assembled from the saved 22-case run, a two-case focused repair rerun and five affected Records checks. Earlier first run had 14/16 passes and two failures. The 22-case run had 20/22 passes and two test-fixture failures. All four failed cases passed the subsequent focused reruns. Original failing logs are retained.
- **4 existing OwnerDrawing host checks passed.** The main QA package compiled and built, as did its instrumentation package.
- Checks included wrong-account and amount-only chains, zero movement on restoration, permission/closed-date refusal, injected second-leg/actor rollback, exact SQL and complete-event validation, Room migration with ambiguous legacy data, ordinary/encrypted backup, old backup inference, repeat/missing-dependency/partial local replay, and existing same-account/Records/backup/close-policy regressions.
- [verification.json](verification.json) records each passing case's run, 16 current source/schema/test hashes, and the unchanged existing Records screen/view-model hashes. The QA installation was `com.saaf.milkshop.qa.tests` on physical serial `R8VXA02SNKH`. No ordinary app was installed and no shop data was changed.

This is a scoped result. A full regression suite and live two-phone/server transport acceptance were not run. B3 remains **PARTIAL**: B3c actual money received and B3d screens are still pending. The next run is B3c only, with its own preflight and affected checks. No commit/push or release packaging occurred.
