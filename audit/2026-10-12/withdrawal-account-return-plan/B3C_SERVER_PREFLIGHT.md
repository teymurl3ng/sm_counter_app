# B3c server preflight — owner-funds write gate

Status: **preflight corrected after live verification.** The generic sync payload store accepts a new event type without a remote schema/RPC change. A proposed pusher-role gate was tested on development and then reverted because it would reject legitimate offline owner entries from a cashier-enrolled device. Production was not changed. Development and production `sync_push` definitions now both have the original fingerprint `cc681db1a8a41e61971876750b908da8`. No app build or install was made in this preflight.

## Evidence

- The Android push client sends event envelopes to `/rest/v1/rpc/sync_push` (`SyncServerClient.kt`).
- The checked-in `audit/2026-10-09/repairs/server-bootstrap.patch.txt` definition of `public.sync_push` accepts `events` as a table. It checks `v_staff.role in ('owner','admin')` only when `corrects_event_uid` is present. It does not inspect `payload.event_type` for a new original event. Thus an active manager/cashier account authorized for the shop can submit an original `OWNER_FUNDS_IN` envelope. The server would store and relay it even though local Android permissions would refuse the UI command.
- Live read-only checks on 2026-10-12 confirmed both development and production had the saved `sync_push` definition hash, `cc681db1a8a41e61971876750b908da8`, before the development test. After reverting the test migration, both again have this exact hash.
- The RPC uses a generic JSON payload with no event-type allowlist, so a new type alone needs no remote table/schema addition. Remote actor-proof for shared offline entries remains a separate access/transport issue.

## Superseded proposed server patch

Do **not** deploy `OWNER_FUNDS_RPC_GATE.sql` to production. It was applied to development, verified with rolled-back role probes, and reverted by `REVERT_OWNER_FUNDS_RPC_GATE.sql`. The device background worker drains one shared offline outbox under the currently enrolled server identity (`SyncRuntime.engineDrainLocked` and `SyncServerClient.push`). That identity can be cashier even when a local owner/admin performed the original entry. A pusher-role test would confuse transport identity with the actual actor and permanently halt a legitimate queued owner-funds event. The existing correction gate has the same potential limitation; it is not solved here.

Illustrative condition inside the authenticated envelope loop:

```sql
if v_table = 'events'
   and v_payload->>'event_type' in ('OWNER_DRAWING', 'OWNER_FUNDS_IN')
   and v_staff.role not in ('owner', 'admin') then
  return jsonb_build_object('ok', false, 'error', jsonb_build_object(
    'code', 'owner_funds_privilege_required',
    'message', 'Owner funds require an owner or admin account'));
end if;
```

Development probes were wrapped in explicit transactions and rolled back: cashier/operator owner-funds originals were refused, cashier correction was refused, and cashier sale plus owner receipt were accepted. Zero probe rows remained. These results establish what the temporary guard did, not that it was suitable for deployment. The first admin fixture had no active shop and returned `shop_required`; there was no need to extend testing after the shared-outbox defect was confirmed.

**Disposition:** B3c local receipt/correction work may proceed because the server accepts opaque event JSON. The app's local Owner/Admin permission remains the immediate write control. Strong remote actor authorization for offline-shared writes needs an actor-proof or device-role design and belongs in the existing access/transport trust work; it is not a B3c server patch. Do not claim hostile-client security or two-device readiness from local UID replay. B3d remains the separate UI run.
