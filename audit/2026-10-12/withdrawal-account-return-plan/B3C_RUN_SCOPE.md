# B3c local run scope

Status: completed at [the focused result](../withdrawal-money-received/RESULT.md). B3d Records UI remains separate.

Outcome: record real owner money received against a withdrawal; correct its amount/account or cancel a mistaken receipt without changing profit or stock. Keep B3d screens separate.

Touch only the owner-funds domain/application API, Room 15→16 owner-funds guards, snapshot/restore and event-envelope replay needed for this new money event, plus selected checks. A receipt uses its own stable UID, actual open date, Cash/Bank destination and immutable withdrawal-root reference. The latest correction is the current receipt truth; ledger rows are exact differences. Excess over the corrected withdrawal is allowed only after a current-facts acknowledgement. Existing receipts survive later withdrawal correction/cancellation.

The [corrected server preflight](B3C_SERVER_PREFLIGHT.md) established that the RPC relays opaque event JSON and requires no new event-type allowlist. It also established that a pusher-role gate cannot prove the offline local actor. B3c claims local role enforcement and compatible event transport, not remote actor-proof or live two-device readiness. No remote deployment, UI, general owner-capital feature, broad suite or ordinary app install in this run.

Selected proof: receipt/correction/cancellation balance examples; date/permission/UID/excess cases; Room 15 upgrade and SQL guard refusals; ordinary/encrypted snapshot round trips; replay into a second database with different numeric IDs; existing B3b regression cases affected by version or owner-funds changes. Stop after this run and record B3d next.
