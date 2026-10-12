# B1b controlled profile catch-up — complete at bounded QA scope

An enrolled device now performs a one-time profile-only historical replay after its ordinary pull. It scans `sync_pull` from sequence zero in pages, applies only Product/Account rows carrying a `profile` key, and never changes the ordinary pull cursor. The existing parent-UID remapping, per-record savepoint, pending-local-edit park and no-push-echo rules remain in force. A network failure or refused profile leaves the completion marker unset, so the next sync retries from zero. The marker is bound to the server environment/URL, shop and no-backup installation identity; a restored installation receives a new identity and cannot silently inherit the marker.

Read-only DEV and PROD schema checks found the same `sync_pull` definition hash (`64eee07aa5fcd64d6d38ac0c5ef4423c`) and the same unique `(shop_id, table_name, row_uid)` index on `sync_records`. The pull function reads rows above `since_seq` in sequence order, so this replay sees each current server master row without changing a server RPC.

The isolated QA app and test APKs built successfully. On S24, the [five catch-up checks](b1b-catchup-s24.log) passed 5/5: paging, an advanced cursor, failure/retry, pending local edit, fresh cursor-zero behavior, malformed refusal and one-time completion. The [four directly affected receiver/apply checks](b1b-affected-regression-s24.log) passed 4/4 on the final source. No live shop data, server schema/RPC, ordinary app, enrollment state, commit or push was changed.

B1 profile receiving and catch-up are complete at this QA scope. Phase 2 still needs its separate planning receiver/catch-up batch B2/B10 and eventual physical two-phone acceptance in phase 5.
