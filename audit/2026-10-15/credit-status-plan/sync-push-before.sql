CREATE OR REPLACE FUNCTION public.sync_push(envelopes jsonb, device_id text DEFAULT NULL::text, shop_id uuid DEFAULT NULL::uuid, pull_cursor bigint DEFAULT NULL::bigint)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
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
    -- erratum 3: constraint-name target (inference cols collide with the param)
    insert into public.sync_records
      (business_id, shop_id, table_name, row_uid, payload, origin_device)
    values (
      (select business_id from public.shops where id = v_shop),
      v_shop, v_table, v_uid, v_payload, device_id)
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
$function$
