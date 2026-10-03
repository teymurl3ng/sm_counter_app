# SYNC C1 MIGRATION — Decision Record & SQL

**Branch:** `fix/sync-c1-propagation` · **Date:** 2026-10-02 · **Status:** approved plan v5 (owner)
**Supersedes:** the in-code "pull before pushing" locked decision (`SyncRuntime.pullOnce` comment) and the undocumented "server order wins" sync behavior.

---

## 1. The problem (verified end-to-end)

**C1 — updates to already-pulled rows never propagate.** `sync_push` upserts `sync_records` by `(shop_id, table_name, row_uid)` without advancing `server_seq`; `sync_pull` delivers only `WHERE server_seq > since_seq`; the client cursor only advances via pulls (no resync path). Once device B has pulled row X's seq, device A's later edit of X is delivered to nobody. Two devices editing the same row offline permanently diverge; the overwritten edit survives nowhere. Live on the owner's two phones today.

**C1b — sequence order ≠ commit order (pre-existing, ledger-bearing).** Postgres assigns identity seqs at insert-attempt time, not commit time. Two concurrent pushes can take seqs 10 and 11 with 11 committing first; a pull in between advances the cursor past 11 and row 10 is never delivered — a committed, money-bearing ledger row silently dropped. Fixed here by the per-shop advisory lock.

**Blast radius:** mutable synced tables — `products`, `accounts`, `settings`, `store_profile`, `deliveries`, `attendance_records` (parent-row updates; the append-only status-history rows propagate as new inserts). Append-only tables (`events`, `delivery_status_history`, `daily_close_actions`, `physical_cash_counts`) are unaffected by C1 — new rows always get fresh seqs — but were one crafted call away from silent overwrite (closed by §3 hardening).

## 2. Chosen design — A′ (single-column seq-bump), last-push-wins + parking

- `server_seq` becomes a plain bigint assigned by a `BEFORE INSERT OR UPDATE` trigger from an owned sequence: **every insert bumps; every meaningful update bumps; an unchanged re-push is a complete no-op** (normalized payload comparison strips `sync_updated_at`).
- `sync_pull` is unchanged — it keeps filtering/ordering by `server_seq` and emitting that key, so the client pull/apply/cursor path is untouched. Old cursors remain valid (seq values never reused or lowered by the migration; the one-time re-sequence only renumbers monotonically upward-shifted, and both phones re-pull by design afterwards).
- Drain order flips to **push-before-pull** (a local edit reaches the server before any pull can overwrite it). Supersedes the pull-first locked decision: *locked because* it optimized first-convergence for freshly enrolled devices, which the separate enrollment baseline already covers; *revisit if* a future feature depends on pulling before any local drain.
- **Conflict policy: last-push-wins by push order.** Both edits reach the server; devices converge to the last push. Every true overwrite leaves a server `audit_log` row (`sync_conflict`) holding the **losing payload**. True = payload changed AND the existing row was pushed by a different device AND (`existing.server_seq > pull_cursor` the pusher hadn't seen it, OR the push carries a `parked_seq` hint for a version the pusher saw but refused to apply locally).
- **Pending-edit guard + parking:** pulled rows whose uid still has a pending local outbox entry (unpushed edit, e.g. during a push-halt) are **parked** in a new local `sync_parked_rows` table, not skipped — the cursor still advances, but parked rows are re-evaluated automatically, so nothing is silently lost.
- **Ack-equality:** an outbox entry is deleted after a push only when the live row still structurally equals the pushed envelope (`JSONObject.similar()` — `org.json` key order is HashMap-random; byte comparison would re-push forever). An edit landing mid-push keeps the entry, which re-pushes.

## 3. Per-table policy matrix (writer roles derived from the client's own RolePermissions)

| table | policy | writers | parents / notes |
|---|---|---|---|
| `events` | append-only (server-enforced) | any active staff; `corrects_event_uid` requires owner/admin (D8, unchanged) | insert-only; corrections are new events |
| `delivery_status_history` | append-only (server-enforced) | any active staff | insert-only |
| `daily_close_actions` | append-only (server-enforced) | CASH_COUNT_AND_CLOSE holders | insert-only |
| `physical_cash_counts` | append-only (server-enforced) | CASH_COUNT_AND_CLOSE holders | insert-only |
| `products` | mutable master data | **owner/admin only** (server-enforced; matches client `requireOwnerMasterData`) | children: events reference product_id via uid |
| `accounts` | mutable master data | **owner/admin only** | children: events, deliveries |
| `settings` | mutable master data | **owner/admin only** | one row per key (`setting_key` PK) — per-key LWW is safe |
| `store_profile` | mutable master data | **owner/admin only** | singleton |
| `deliveries` | mutable operational | any active staff | parent of delivery_status_history |
| `attendance_records` | mutable operational | any active staff | supersedes chains |
| `budget_plans`, `collections_targets` | pushed, not yet applied client-side | any active staff | planning sync not landed (unchanged) |

Server-side consequence of owner-only enrollment: the server sees a device-level owner, not per-cashier identity. Accepted for two owner-held phones pre-pilot; per-device revocation and a lower-privilege counter-device credential are on the before-pilot list (§9).

## 4. Invariants (testable statements — each with its enforcing mechanism)

1. **No committed row is skipped by the cursor.** Seq order == commit order per shop → enforced by `pg_advisory_xact_lock(hashtextextended(shop_id::text,0))` in `sync_push` after auth, before the first `nextval`.
2. **Apply never permanently drops a row for a missing parent.** Dependency-failure rows are parked and retried (client `sync_parked_rows`), not skipped-and-forgotten.
3. **Pull never overwrites a pending local edit.** Apply-side guard parks pulled updates for uids with pending outbox entries.
4. **Append-only tables are insert-only on both sides.** Server: `ON CONFLICT DO NOTHING` + audit on differing payload. Client: existing skip-by-uid retained as defense-in-depth.
5. **Outbox order preserves dependency order.** Capture UPDATE triggers enqueue with `INSERT … WHERE NOT EXISTS` (position kept, no DELETE+INSERT tail-move).
6. **Ack removes only the pushed snapshot.** Outbox entry deleted after push only on structural envelope equality with the live row, in the same transaction as the delete.
7. **An overwritten seen-but-unapplied version always leaves an audit row holding the losing payload.** `parked_seq` hint + `pull_cursor` rule; `OLD.payload` stored.
8. **Unchanged re-pushes are invisible.** Server: `DO UPDATE … WHERE` normalized-payload-differs → `server_seq`, `origin_device`, `received_at` all preserved (so conflict attribution stays correct).

## 5. Locked decisions (with reasons and revisit conditions)

| decision | locked because | revisit if |
|---|---|---|
| push-before-pull drain order | local edits must reach the server before any pull; nothing needs pull-first (enrollment baselines separately) | a feature requires reading other devices' state before the first local drain |
| last-push-wins (no field merge) | whole-row snapshots + shop-scale master data; field merge is error-prone | disjoint-field clobbering causes real disputes pre-pilot |
| halt-at-head strict FIFO push | no dependent record overtakes a refused one | — |
| upsert-only sync, no deletes/tombstones | no synced table is ever hard-deleted by app code | a hard-delete feature is introduced (must add tombstones first) |
| enroll owner-only devices (M9) | push identity is the enrolled account, stable across local sign-ins | shared counter phones ship (needs per-device revocation + credential tiers) |
| parking (not skipping) for dependency and pending-edit deferrals | skip-and-advance permanently loses rows the cursor passed | — |

## 6. Migration SQL — apply per environment (DEV rehearsal first, then PROD behind its gate)

> Names of the JSON body keys (`envelopes`, `device_id`, `shop_id`) MUST be re-verified against the client's request body before applying (pre-flight). Runs as ONE transaction. No overloads survive.

**Rehearsal errata (go 5, DEV-verified 2026-10-03 — ALREADY APPLIED in the SQL below; the SQL is what actually runs).** The DEV rehearsal on a shape-faithful PROD-data copy found three plpgsql traps in the originally drafted §6.5/§8, all fixed and re-verified over REST:

1. **`nullif(shop_id, '')` broke every call** (`22P02 invalid input syntax for type uuid: ""`): the untyped `''` coerces to the `uuid` param at plan time. The param is already uuid; PostgREST cannot bind an empty string into it, and the app client only sends non-empty shops (`SyncServerClient` guards `isNotEmpty`). Fixed to `v_shop := shop_id;` in BOTH §6.5 and §8.
2. **plpgsql param/column name collision** (`42702 column reference "shop_id" is ambiguous`): the `shop_id` PARAMETER (name required for PostgREST body-key binding) collides with the `shop_id` COLUMN. Fixed by qualifying the `v_existing` lookup's column refs AND targeting `on conflict on constraint sync_records_shop_table_uid` (the same constraint-name lesson phase 5 recorded).
3. **Seq burn on existing-row envelopes (benign, no action):** the `BEFORE INSERT` trigger fires before conflict redirection, so every envelope for an existing row burns one `nextval()` on the discarded insert tuple before the `DO UPDATE` leg (which burns a second one only when the payload actually changed). No row ever carries a burned seq, so this is invisible to clients — it only widens future gaps (bigint). Order==commit per shop is unaffected (advisory lock serializes assignment).
4. **Comment correction:** an unchanged/append-only-refused envelope hits `continue` BEFORE the `applied` counter, so refused append-only rows are NOT counted in the response's `applied` (the original comment claimed they were). The client never branches on `applied` (it counts its own envelopes), so behavior is fine — comment corrected below.

DEV-verified end state (go 5): 8 REST scenarios PASS (legacy 3-key no-op, C1-kill propagation, pull_cursor conflict with loser payload preserved, parked_seq conflict, append-only refusal, insert via trigger, unknown-table refusal, full ordered pull); §7 re-sequence PASS (dense 1..N, per-shop order preserved); §8 rollback dry-run PASS (rolled back, state intact). **Canonical function hash: applying §6.5 VERBATIM (comments included — `pg_get_functiondef` embeds comments, so hash comparisons only work against byte-identical text) yields md5 = `cc681db1a8a41e61971876750b908da8`; PROD and DEV were both verified at this hash on 2026-10-03.** Any future re-apply must use the §6.5 text unchanged or the hash will differ even when behavior is identical.

```sql
begin;

-- 6.1  No overloads survive
drop function if exists public.sync_push(jsonb);
drop function if exists public.sync_push(jsonb, text, uuid);

-- 6.2  server_seq: identity -> owned sequence
alter table public.sync_records alter column server_seq drop identity;
create sequence public.sync_records_server_seq_seq as bigint
  owned by public.sync_records.server_seq;
select setval('public.sync_records_server_seq_seq',
              (select coalesce(max(server_seq), 0) from public.sync_records));

-- 6.3  Normalized-payload helper (volatile client stamps excluded)
create or replace function public.sync_normalize_payload(p jsonb)
returns jsonb language sql immutable set search_path = ''
as $$
  select p - 'sync_updated_at' - 'received_at'
$$;

-- 6.4  Seq assignment: every insert bumps; updates bump only on meaningful change
create or replace function public.sync_records_assign_seq()
returns trigger language plpgsql set search_path = ''
as $$
begin
  if tg_op = 'INSERT' then
    new.server_seq := nextval('public.sync_records_server_seq_seq');
    return new;
  end if;
  if public.sync_normalize_payload(new.payload)
     is not distinct from public.sync_normalize_payload(old.payload) then
    return new;  -- unchanged: leave explicitly-set values alone
  end if;
  new.server_seq := nextval('public.sync_records_server_seq_seq');
  return new;
end
$$;

create trigger sync_records_assign_seq
before insert or update on public.sync_records
for each row execute function public.sync_records_assign_seq();

-- 6.5  sync_push: 4-arg (old 3-arg calls keep working via DEFAULT NULL)
create or replace function public.sync_push(
  envelopes   jsonb,
  device_id   text   default null,
  shop_id     uuid   default null,
  pull_cursor bigint default null
)
returns jsonb language plpgsql security definer set search_path = ''
as $$
declare
  v_staff       record;
  v_shop        uuid;
  v_env         jsonb;
  v_table       text;
  v_uid         text;
  v_payload     jsonb;
  v_parked_seq  bigint;
  v_existing    public.sync_records%rowtype;
  v_applied     int := 0;
  v_conflicts   int := 0;
begin
  select s.id, s.business_id, s.role, s.username into v_staff
    from public.staff s
   where s.auth_user_id = auth.uid() and s.active and s.terminated_at is null;
  if not found then
    return jsonb_build_object('ok', false, 'error', jsonb_build_object(
      'code', 'not_authorized', 'message', 'No active staff profile'));
  end if;

  -- erratum 1: nullif(shop_id,'') casts '' to the uuid param at plan time (22P02)
  v_shop := shop_id;
  if v_shop is null or v_shop not in (select public.sync_staff_shop_ids()) then
    return jsonb_build_object('ok', false, 'error', jsonb_build_object(
      'code', 'shop_required', 'message',
      'The request must name a shop this account may write to'));
  end if;

  if jsonb_typeof(envelopes) <> 'array' or jsonb_array_length(envelopes) > 500 then
    return jsonb_build_object('ok', false, 'error', jsonb_build_object(
      'code', 'too_many_envelopes', 'message',
      'Send between 0 and 500 envelopes per call'));
  end if;

  -- INVARIANT 1: seq order == commit order per shop
  perform pg_advisory_xact_lock(hashtextextended(v_shop::text, 0));

  for v_env in select * from jsonb_array_elements(envelopes) loop
    v_table      := v_env->>'table';
    v_uid        := v_env->>'uid';
    v_payload    := v_env->'payload';
    v_parked_seq := nullif(v_env->>'parked_seq', '')::bigint;

    if v_table is null or v_uid is null
       or jsonb_typeof(v_payload) <> 'object' then
      return jsonb_build_object('ok', false, 'error', jsonb_build_object(
        'code', 'malformed_envelope', 'message',
        'Every envelope needs table, uid and a payload object'));
    end if;

    if v_table not in (
      'events', 'products', 'accounts', 'budget_plans', 'collections_targets',
      'deliveries', 'attendance_records', 'delivery_status_history',
      'daily_close_actions', 'physical_cash_counts', 'settings', 'store_profile'
    ) then
      return jsonb_build_object('ok', false, 'error', jsonb_build_object(
        'code', 'unknown_table', 'message', 'Unknown synced table: ' || v_table));
    end if;

    -- D8 (unchanged): corrections of recorded history from privileged sources only
    if v_table = 'events'
       and (v_payload->>'corrects_event_uid') is not null
       and v_staff.role not in ('owner', 'admin') then
      return jsonb_build_object('ok', false, 'error', jsonb_build_object(
        'code', 'correction_privilege_required', 'message',
        'Correcting recorded history requires an owner or admin account'));
    end if;

    -- erratum 2: qualify columns (unqualified shop_id collides with the param, 42702)
    select * into v_existing from public.sync_records
     where public.sync_records.shop_id    = v_shop
       and public.sync_records.table_name = v_table
       and public.sync_records.row_uid    = v_uid;

    if found then
      -- INVARIANT 4: append-only tables are insert-only
      if v_table in ('events', 'delivery_status_history',
                     'daily_close_actions', 'physical_cash_counts') then
        if public.sync_normalize_payload(v_existing.payload)
           is distinct from public.sync_normalize_payload(v_payload) then
          insert into public.audit_log (action, business_id, performed_by,
                                        performed_by_username, detail)
          values ('sync_append_only_refused', v_staff.business_id, v_staff.id,
                  v_staff.username,
                  jsonb_build_object('table', v_table, 'uid', v_uid,
                                     'shop', v_shop, 'device', device_id));
        end if;
        continue;  -- insert-only: never update; NOT counted in 'applied' (erratum 4)
      end if;

      -- master-data role gate
      if v_table in ('products', 'accounts', 'settings', 'store_profile')
         and v_staff.role not in ('owner', 'admin') then
        return jsonb_build_object('ok', false, 'error', jsonb_build_object(
          'code', 'master_data_privilege_required', 'message',
          'Updating ' || v_table || ' requires an owner or admin account'));
      end if;

      -- INVARIANT 7: true-conflict audit, losing payload preserved
      if public.sync_normalize_payload(v_existing.payload)
         is distinct from public.sync_normalize_payload(v_payload)
         and v_existing.origin_device is distinct from device_id
         and (
              (pull_cursor is not null and v_existing.server_seq > pull_cursor)
              or (v_parked_seq is not null
                  and v_existing.server_seq >= v_parked_seq)
             ) then
        insert into public.audit_log (action, business_id, performed_by,
                                      performed_by_username, detail)
        values ('sync_conflict', v_staff.business_id, v_staff.id,
                v_staff.username,
                jsonb_build_object(
                  'table', v_table, 'uid', v_uid, 'shop', v_shop,
                  'loser_payload', v_existing.payload,
                  'loser_device',  v_existing.origin_device,
                  'loser_seq',     v_existing.server_seq,
                  'winner_device', device_id,
                  'detected_by',   case when v_parked_seq is not null
                                        then 'parked_seq' else 'pull_cursor' end));
        v_conflicts := v_conflicts + 1;
      end if;
    end if;

    -- INVARIANT 8: unchanged re-push is a complete no-op (WHERE fails -> nothing set)
    insert into public.sync_records
      (business_id, shop_id, table_name, row_uid, payload, origin_device)
    values (
      (select business_id from public.shops where id = v_shop),
      v_shop, v_table, v_uid, v_payload, device_id)
    -- erratum 3: constraint-name target (inference cols collide with the param)
    on conflict on constraint sync_records_shop_table_uid do update
      set payload       = excluded.payload,
          origin_device = excluded.origin_device,
          received_at   = now()
      where public.sync_normalize_payload(public.sync_records.payload)
            is distinct from public.sync_normalize_payload(excluded.payload);

    v_applied := v_applied + 1;
  end loop;

  insert into public.audit_log (action, business_id, performed_by,
                                performed_by_username, detail)
  values ('sync_push', v_staff.business_id, v_staff.id, v_staff.username,
          jsonb_build_object('envelopes', v_applied, 'conflicts', v_conflicts,
                             'device', device_id, 'shop', v_shop));

  return jsonb_build_object('ok', true, 'applied', v_applied,
                            'conflicts', v_conflicts,
                            'server_time_epoch_ms',
                            (extract(epoch from now()) * 1000)::bigint);
end
$$;

-- 6.6  Grants (match client posture: authenticated only)
revoke execute on function public.sync_push(jsonb, text, uuid, bigint) from anon, public;
grant  execute on function public.sync_push(jsonb, text, uuid, bigint) to authenticated;

commit;
```

## 7. Re-sequence SQL — separate, LAST (after both phones run the new client)

Repairs existing divergence: renumbers all rows in current seq order so both phones' cursors fall below head and everything re-pulls. Two-pass shift avoids transient PK collisions; the bump trigger is disabled for the transaction.

```sql
begin;
alter table public.sync_records disable trigger sync_records_assign_seq;

create temp table reseq_old_max as
  select coalesce(max(server_seq), 0) as m from public.sync_records;

update public.sync_records
   set server_seq = server_seq + (select max(server_seq) from public.sync_records);

with ordered as (
  select row_number() over (order by server_seq) as rn, server_seq as shifted
    from public.sync_records
)
update public.sync_records sr
   set server_seq = ordered.rn
  from ordered
 where sr.server_seq = ordered.shifted;

-- erratum 5 (go 6): the dense renumber lands BELOW old cursors whenever the old
-- identity skipped values (PROD: rows 2..37 -> 1..36). The sequence must not be
-- left below the highest cursor any device could hold (= old max), or the next
-- assigned seq would be invisible to fully-drained devices (a C1b-class drop).
select setval('public.sync_records_server_seq_seq',
              greatest((select max(server_seq) from public.sync_records),
                       (select m from reseq_old_max)));

alter table public.sync_records enable trigger sync_records_assign_seq;
commit;
```

## 8. Rollback SQL — restores SERVICE, not the bug (keeps 4-arg acceptance)

If the migration misbehaves: drops the bump machinery, recreates a pre-C1-behavior `sync_push` that still accepts (and ignores) `pull_cursor` and `parked_seq`, so both client generations keep syncing. Pre-C1 semantics return (updates stop propagating) — documented honestly.

```sql
begin;
alter table public.sync_records disable trigger sync_records_assign_seq;
drop trigger if exists sync_records_assign_seq on public.sync_records;
drop function if exists public.sync_records_assign_seq();
drop function if exists public.sync_push(jsonb, text, uuid, bigint);

create or replace function public.sync_push(
  envelopes   jsonb,
  device_id   text   default null,
  shop_id     uuid   default null,
  pull_cursor bigint default null   -- accepted, ignored
)
returns jsonb language plpgsql security definer set search_path = ''
as $$
declare
  v_staff record;  v_shop uuid;  v_env jsonb;  v_table text;  v_uid text;
  v_applied int := 0;
begin
  select s.id, s.business_id, s.role, s.username into v_staff
    from public.staff s
   where s.auth_user_id = auth.uid() and s.active and s.terminated_at is null;
  if not found then
    return jsonb_build_object('ok', false, 'error', jsonb_build_object(
      'code', 'not_authorized', 'message', 'No active staff profile'));
  end if;

  -- erratum 1 (same as §6.5): no nullif on the uuid param
  v_shop := shop_id;
  if v_shop is null or v_shop not in (select public.sync_staff_shop_ids()) then
    return jsonb_build_object('ok', false, 'error', jsonb_build_object(
      'code', 'shop_required', 'message', 'shop required'));
  end if;

  if jsonb_typeof(envelopes) <> 'array' or jsonb_array_length(envelopes) > 500 then
    return jsonb_build_object('ok', false, 'error', jsonb_build_object(
      'code', 'too_many_envelopes', 'message', '0..500 envelopes per call'));
  end if;

  for v_env in select * from jsonb_array_elements(envelopes) loop
    v_table := v_env->>'table';
    v_uid   := v_env->>'uid';
    if v_table is null or v_uid is null
       or jsonb_typeof(v_env->'payload') <> 'object' then
      return jsonb_build_object('ok', false, 'error', jsonb_build_object(
        'code', 'malformed_envelope', 'message', 'table, uid, payload required'));
    end if;
    if v_table not in (
      'events', 'products', 'accounts', 'budget_plans', 'collections_targets',
      'deliveries', 'attendance_records', 'delivery_status_history',
      'daily_close_actions', 'physical_cash_counts', 'settings', 'store_profile'
    ) then
      return jsonb_build_object('ok', false, 'error', jsonb_build_object(
        'code', 'unknown_table', 'message', 'Unknown synced table: ' || v_table));
    end if;
    if v_table = 'events'
       and (v_env->'payload'->>'corrects_event_uid') is not null
       and v_staff.role not in ('owner', 'admin') then
      return jsonb_build_object('ok', false, 'error', jsonb_build_object(
        'code', 'correction_privilege_required', 'message', 'owner or admin required'));
    end if;

    insert into public.sync_records
      (server_seq, business_id, shop_id, table_name, row_uid, payload, origin_device)
    values
      (nextval('public.sync_records_server_seq_seq'),
       (select business_id from public.shops where id = v_shop),
       v_shop, v_table, v_uid, v_env->'payload', device_id)
    on conflict on constraint sync_records_shop_table_uid do update
      set payload = excluded.payload,
          origin_device = excluded.origin_device,
          received_at = now();
    v_applied := v_applied + 1;
  end loop;

  insert into public.audit_log (action, business_id, performed_by,
                                performed_by_username, detail)
  values ('sync_push', v_staff.business_id, v_staff.id, v_staff.username,
          jsonb_build_object('envelopes', v_applied, 'device', device_id, 'shop', v_shop));

  return jsonb_build_object('ok', true, 'applied', v_applied,
                            'server_time_epoch_ms',
                            (extract(epoch from now()) * 1000)::bigint);
end
$$;

revoke execute on function public.sync_push(jsonb, text, uuid, bigint) from anon, public;
grant  execute on function public.sync_push(jsonb, text, uuid, bigint) to authenticated;
commit;
```

## 9. Runbooks

- **Migration→install window:** after the server migration, OLD clients pull updates and apply them unconditionally (no guard, no parking). Therefore: drain both phones to outbox 0 first, allow **no edits** on either phone during the window, and install the new client immediately after the server step. The re-sequence runs only after both phones run the new client.
- **Poisoned head:** a permanent-refused outbox row halts the queue (by design). Remedy: fix the cause on the device (the notification + Security History carry the reason code); if unfixable, owner may unenroll/re-enroll (re-baselines). The pending-edit guard + parking make pulls safe during a halt.
- **Re-baseline:** full re-enroll resets config/binding/tokens and re-baselines from server state; local ledger rows are kept. With cursor-reset-on-restore also present, there are two documented recovery paths.
- **Restore:** backups do NOT contain the outbox (verified) or parked rows; restore resets the pull cursor to 0 so every row re-pulls (idempotent). Restored planning rows re-enqueue by capture triggers — known documented behavior (M5), unchanged here.
- **Environment verification:** DEV and PROD function bodies/columns/indexes/triggers/grants are diffed before every apply (audit baseline: parity confirmed except the DEV-only legacy overload this migration removes).

## 10. Deferred (before pilot)

Epoch stored with the cursor (re-baseline + environment-switch protection) · `sync_records_history` sidecar · conflict notification surface · per-device revocation without terminating the owner account · lower-privilege credential tier for shared counter phones · per-cashier server-side attribution · spec decisions: offline-authored events on a day another device closed; product unit/price changes vs historical events · backup format bump to carry parked rows.
