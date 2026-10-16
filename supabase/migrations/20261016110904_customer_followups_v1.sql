-- Phase 6 B. Additive contract only: no existing shop is activated by migration.
CREATE SCHEMA followup_private;
REVOKE ALL ON SCHEMA followup_private FROM PUBLIC, anon, authenticated;

CREATE TABLE followup_private.policies (
  shop_id uuid PRIMARY KEY REFERENCES public.shops(id) ON DELETE CASCADE,
  revision uuid NOT NULL,
  schedule_revision uuid NOT NULL,
  effective_from date NOT NULL,
  enabled boolean NOT NULL,
  weekday int NOT NULL CHECK(weekday BETWEEN 1 AND 7),
  repeat_days int NOT NULL CHECK(repeat_days BETWEEN 1 AND 30),
  preferred_time time NOT NULL,
  quiet_start time NOT NULL,
  quiet_end time NOT NULL,
  primary_installation text CHECK(length(primary_installation) BETWEEN 1 AND 200),
  primary_actor uuid REFERENCES public.staff(id),
  updated_by uuid NOT NULL REFERENCES public.staff(id),
  updated_at timestamptz NOT NULL,
  CHECK(quiet_start<>quiet_end),
  CHECK((primary_installation IS NULL)=(primary_actor IS NULL)),
  CHECK(NOT enabled OR primary_installation IS NOT NULL)
);
CREATE TABLE followup_private.customers (
  shop_id uuid NOT NULL REFERENCES public.shops(id) ON DELETE CASCADE,
  customer_uid text NOT NULL CHECK(length(customer_uid) BETWEEN 1 AND 200),
  revision uuid NOT NULL,
  schedule_revision uuid,
  effective_from date,
  schedule jsonb,
  paused boolean NOT NULL DEFAULT false,
  snoozed_until date,
  statement_cycle text CHECK(length(statement_cycle) BETWEEN 1 AND 1024),
  reviewed_on date,
  PRIMARY KEY(shop_id,customer_uid),
  CHECK((schedule_revision IS NULL)=(effective_from IS NULL))
);
CREATE TABLE followup_private.claims (
  shop_id uuid NOT NULL REFERENCES public.shops(id) ON DELETE CASCADE,
  customer_uid text NOT NULL,
  attempt_uid uuid NOT NULL,
  actor_id uuid NOT NULL REFERENCES public.staff(id),
  installation text NOT NULL,
  cycle text NOT NULL CHECK(length(cycle) BETWEEN 1 AND 1024),
  phase text NOT NULL CHECK(phase IN ('PREPARING','HANDOFF_STARTED')),
  lease_until timestamptz,
  started_at timestamptz,
  channel text CHECK(channel IN ('WHATSAPP','SMS')),
  PRIMARY KEY(shop_id,customer_uid),
  UNIQUE(shop_id,attempt_uid),
  CHECK((phase='PREPARING' AND lease_until IS NOT NULL AND started_at IS NULL AND channel IS NULL)
    OR (phase='HANDOFF_STARTED' AND lease_until IS NULL AND started_at IS NOT NULL AND channel IS NOT NULL))
);
CREATE TABLE followup_private.actions (
  seq bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  shop_id uuid NOT NULL REFERENCES public.shops(id) ON DELETE CASCADE,
  command_uid uuid NOT NULL,
  kind text NOT NULL,
  actor_id uuid NOT NULL REFERENCES public.staff(id),
  installation text NOT NULL,
  customer_uid text,
  attempt_uid uuid,
  cycle text,
  accepted_at timestamptz NOT NULL,
  UNIQUE(shop_id,command_uid)
);
CREATE INDEX followup_actions_shop_seq ON followup_private.actions(shop_id,seq);
CREATE INDEX followup_actions_shop_attempt ON followup_private.actions(shop_id,attempt_uid) WHERE attempt_uid IS NOT NULL;
CREATE TABLE followup_private.commands (
  shop_id uuid NOT NULL REFERENCES public.shops(id) ON DELETE CASCADE,
  command_uid uuid NOT NULL,
  actor_id uuid NOT NULL REFERENCES public.staff(id),
  installation text NOT NULL,
  request jsonb NOT NULL,
  result jsonb NOT NULL,
  PRIMARY KEY(shop_id,command_uid)
);
CREATE TABLE followup_private.digests (
  shop_id uuid NOT NULL REFERENCES public.shops(id) ON DELETE CASCADE,
  business_date date NOT NULL,
  installation text NOT NULL,
  actor_id uuid NOT NULL REFERENCES public.staff(id),
  reserved_at timestamptz NOT NULL,
  PRIMARY KEY(shop_id,business_date)
);
ALTER TABLE followup_private.policies ENABLE ROW LEVEL SECURITY;
ALTER TABLE followup_private.customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE followup_private.claims ENABLE ROW LEVEL SECURITY;
ALTER TABLE followup_private.actions ENABLE ROW LEVEL SECURITY;
ALTER TABLE followup_private.commands ENABLE ROW LEVEL SECURITY;
ALTER TABLE followup_private.digests ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON ALL TABLES IN SCHEMA followup_private FROM PUBLIC, anon, authenticated;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA followup_private FROM PUBLIC, anon, authenticated;

CREATE FUNCTION followup_private.actor(p_shop uuid,p_owner boolean DEFAULT false) RETURNS uuid
LANGUAGE plpgsql STABLE SET search_path='' AS $$
DECLARE v_actor public.staff%rowtype;
BEGIN
  SELECT * INTO v_actor FROM public.staff WHERE auth_user_id=auth.uid() AND active AND terminated_at IS NULL;
  IF NOT FOUND OR p_shop IS NULL OR v_actor.role NOT IN ('owner','admin','manager')
    OR (p_owner AND v_actor.role<>'owner')
    OR NOT EXISTS(SELECT 1 FROM public.sync_staff_shop_ids() s WHERE s=p_shop) THEN
    RAISE SQLSTATE 'PT403' USING MESSAGE='followup_not_authorized';
  END IF;
  RETURN v_actor.id;
END $$;

CREATE FUNCTION followup_private.zone(p_shop uuid) RETURNS text
LANGUAGE plpgsql STABLE SET search_path='' AS $$
DECLARE v_zone text;
BEGIN
  SELECT shop_timezone INTO v_zone FROM public.shops WHERE id=p_shop;
  IF v_zone IS NULL OR NOT EXISTS(SELECT 1 FROM pg_catalog.pg_timezone_names WHERE name=v_zone) THEN
    RAISE SQLSTATE 'PT422' USING MESSAGE='followup_shop_timezone_required';
  END IF;
  RETURN v_zone;
END $$;

CREATE FUNCTION followup_private.heads(p_shop uuid,p_day date) RETURNS jsonb
LANGUAGE sql STABLE SET search_path='' AS $$
  SELECT jsonb_build_object('sync',coalesce((SELECT max(server_seq) FROM public.sync_records WHERE shop_id=p_shop),0),
    'credit',coalesce((SELECT max(server_seq) FROM credit_private.records WHERE shop_id=p_shop),0),
    'metadata',coalesce((SELECT max(seq) FROM followup_private.actions WHERE shop_id=p_shop),0),
    'policy_revision',(SELECT revision FROM followup_private.policies WHERE shop_id=p_shop),'business_date',p_day)
$$;

CREATE FUNCTION followup_private.assert_customer(p_shop uuid,p_uid text) RETURNS void
LANGUAGE plpgsql STABLE SET search_path='' AS $$
BEGIN
  IF p_uid IS NULL OR length(p_uid) NOT BETWEEN 1 AND 200 OR NOT EXISTS(SELECT 1 FROM public.sync_records
    WHERE shop_id=p_shop AND table_name='accounts' AND row_uid=p_uid AND payload->>'category'='Customer') THEN
    RAISE SQLSTATE 'PT422' USING MESSAGE='followup_customer_missing';
  END IF;
END $$;

CREATE FUNCTION followup_private.assert_schedule(p jsonb) RETURNS void
LANGUAGE plpgsql IMMUTABLE SET search_path='' AS $$
BEGIN
  IF p='null'::jsonb THEN RETURN; END IF;
  IF jsonb_typeof(p) IS DISTINCT FROM 'object' OR p->>'kind' IS NULL THEN
    RAISE SQLSTATE 'PT422' USING MESSAGE='followup_invalid_schedule';
  END IF;
  IF p->>'kind'='WEEKLY' AND p-ARRAY['kind','weekday']='{}'::jsonb
    AND jsonb_typeof(p->'weekday')='number' AND p->>'weekday' ~ '^[1-7]$' THEN RETURN;
  ELSIF p->>'kind'='MONTHLY' AND p-ARRAY['kind','day']='{}'::jsonb
    AND jsonb_typeof(p->'day')='number' AND p->>'day' ~ '^([0-9]|[12][0-9]|3[01])$' THEN RETURN;
  ELSIF p->>'kind'='INTERVAL' AND p-ARRAY['kind','days','first_date']='{}'::jsonb
    AND jsonb_typeof(p->'days')='number' AND p->>'days' ~ '^[1-9][0-9]{0,2}$'
    AND (p->>'days')::int<=365 AND jsonb_typeof(p->'first_date')='string'
    AND p->>'first_date' ~ '^\d{4}-\d{2}-\d{2}$' THEN
    PERFORM (p->>'first_date')::date; RETURN;
  END IF;
  RAISE SQLSTATE 'PT422' USING MESSAGE='followup_invalid_schedule';
END $$;

-- A full accepted snapshot under the same shop lock as sync_push, never stored balances.
CREATE FUNCTION public.followup_snapshot_v1(shop uuid) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path='' AS $$
DECLARE v_zone text; v_day date;
BEGIN
  PERFORM followup_private.actor(shop);
  PERFORM pg_advisory_xact_lock(hashtextextended(shop::text,0));
  PERFORM followup_private.actor(shop);
  SELECT shop_timezone INTO v_zone FROM public.shops WHERE id=shop;
  IF EXISTS(SELECT 1 FROM pg_catalog.pg_timezone_names WHERE name=v_zone) THEN
    v_day:=(clock_timestamp() AT TIME ZONE v_zone)::date;
  END IF;
  RETURN jsonb_build_object('ok',true,'contract_version',1,'shop_id',shop,'shop_timezone',v_zone,
    'heads',followup_private.heads(shop,v_day),'server_time_epoch_ms',(extract(epoch FROM clock_timestamp())*1000)::bigint,
    'policy',(SELECT to_jsonb(p)-'shop_id' FROM followup_private.policies p WHERE shop_id=shop),
    'customers',coalesce((SELECT jsonb_agg(to_jsonb(c)-'shop_id' ORDER BY customer_uid) FROM followup_private.customers c WHERE shop_id=shop),'[]'::jsonb),
    'claims',coalesce((SELECT jsonb_agg(to_jsonb(c)-'shop_id' ORDER BY customer_uid) FROM followup_private.claims c WHERE shop_id=shop),'[]'::jsonb),
    'actions',coalesce((SELECT jsonb_agg(to_jsonb(a)-'shop_id' ORDER BY seq) FROM
      (SELECT * FROM followup_private.actions WHERE shop_id=shop ORDER BY seq DESC LIMIT 100) a),'[]'::jsonb));
END $$;

CREATE FUNCTION public.followup_command_v1(shop uuid,installation text,command_uid uuid,operation jsonb) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path='' AS $$
DECLARE v_actor uuid; v_role text; v_now timestamptz; v_day date; v_zone text; v_time time;
  v_kind text; v_uid text; v_attempt uuid; v_cycle text; v_value jsonb; v_allowed text[];
  v_policy followup_private.policies%rowtype; v_customer followup_private.customers%rowtype;
  v_claim followup_private.claims%rowtype; v_old followup_private.commands%rowtype;
  v_result jsonb; v_revision uuid; v_schedule_revision uuid; v_effective date;
  v_primary text; v_primary_actor uuid; v_until date; v_notify boolean:=false; v_launch boolean:=false;
BEGIN
  v_actor:=followup_private.actor(shop);
  IF installation IS NULL OR installation !~ '^[A-Za-z0-9][A-Za-z0-9._:-]{0,199}$' OR command_uid IS NULL
    OR jsonb_typeof(operation) IS DISTINCT FROM 'object' OR octet_length(operation::text)>16384 THEN
    RAISE SQLSTATE 'PT422' USING MESSAGE='followup_invalid_command';
  END IF;
  v_kind:=operation->>'kind';
  v_allowed:=CASE v_kind
    WHEN 'POLICY' THEN ARRAY['kind','expected_revision','value','make_primary','phones_updated']
    WHEN 'PREFERENCE' THEN ARRAY['kind','customer_uid','expected_customer_revision','value']
    WHEN 'REVIEW' THEN ARRAY['kind','customer_uid','expected_customer_revision','expected_heads','cycle']
    WHEN 'SNOOZE' THEN ARRAY['kind','customer_uid','expected_customer_revision','expected_heads','until']
    WHEN 'ACQUIRE' THEN ARRAY['kind','customer_uid','expected_heads','expected_customer_revision','attempt_uid','cycle']
    WHEN 'RENEW' THEN ARRAY['kind','customer_uid','attempt_uid']
    WHEN 'START' THEN ARRAY['kind','customer_uid','expected_heads','expected_customer_revision','attempt_uid','channel']
    WHEN 'RELEASE' THEN ARRAY['kind','customer_uid','attempt_uid']
    WHEN 'RESOLVE' THEN ARRAY['kind','customer_uid','attempt_uid','outcome','acknowledge_uncertainty','expected_heads','expected_customer_revision']
    WHEN 'DIGEST' THEN ARRAY['kind','expected_heads','has_work'] END;
  IF v_allowed IS NULL OR operation-v_allowed<>'{}'::jsonb THEN RAISE SQLSTATE 'PT422' USING MESSAGE='followup_invalid_command'; END IF;
  IF v_kind='POLICY' THEN PERFORM followup_private.actor(shop,true); END IF;
  PERFORM pg_advisory_xact_lock(hashtextextended(shop::text,0));
  v_actor:=followup_private.actor(shop,v_kind='POLICY');
  SELECT role INTO v_role FROM public.staff WHERE id=v_actor;
  SELECT * INTO v_old FROM followup_private.commands c WHERE c.shop_id=shop AND c.command_uid=followup_command_v1.command_uid;
  IF FOUND THEN
    IF v_old.actor_id<>v_actor OR v_old.installation<>installation OR v_old.request<>operation THEN
      RAISE SQLSTATE 'PT409' USING MESSAGE='followup_command_identity_conflict';
    END IF;
    -- Persisted START/DIGEST results are recoverable state, never a second side-effect grant.
    RETURN jsonb_build_object('ok',true,'replayed',true,'launch_authorized',false,'notify_authorized',false,'result',v_old.result);
  END IF;
  v_now:=clock_timestamp(); v_zone:=followup_private.zone(shop);
  v_day:=(v_now AT TIME ZONE v_zone)::date; v_time:=(v_now AT TIME ZONE v_zone)::time;
  SELECT * INTO v_policy FROM followup_private.policies WHERE shop_id=shop;
  IF v_kind<>'POLICY' AND v_policy.shop_id IS NULL THEN RAISE SQLSTATE 'PT409' USING MESSAGE='followup_policy_required'; END IF;
  IF v_kind IN ('ACQUIRE','START','DIGEST') AND NOT v_policy.enabled THEN RAISE SQLSTATE 'PT409' USING MESSAGE='followup_disabled'; END IF;
  IF v_kind IN ('ACQUIRE','START','DIGEST','REVIEW','SNOOZE','RESOLVE')
    AND operation->'expected_heads' IS DISTINCT FROM followup_private.heads(shop,v_day) THEN
    RAISE SQLSTATE 'PT409' USING MESSAGE='followup_refresh_required';
  END IF;
  v_uid:=operation->>'customer_uid';
  IF v_kind NOT IN ('POLICY','DIGEST') THEN
    PERFORM followup_private.assert_customer(shop,v_uid);
    SELECT * INTO v_customer FROM followup_private.customers WHERE shop_id=shop AND customer_uid=v_uid;
    SELECT * INTO v_claim FROM followup_private.claims WHERE shop_id=shop AND customer_uid=v_uid;
    IF v_kind IN ('PREFERENCE','REVIEW','SNOOZE','ACQUIRE','START','RESOLVE') AND
      operation->'expected_customer_revision' IS DISTINCT FROM coalesce(to_jsonb(v_customer.revision),'null'::jsonb) THEN
      RAISE SQLSTATE 'PT409' USING MESSAGE='followup_customer_changed';
    END IF;
  END IF;
  IF v_kind IN ('ACQUIRE','START') AND (coalesce(v_customer.paused,false) OR v_customer.snoozed_until>v_day) THEN
    RAISE SQLSTATE 'PT409' USING MESSAGE='followup_customer_postponed';
  END IF;
  IF v_kind IN ('RENEW','START','RELEASE','RESOLVE','ACQUIRE') THEN
    IF jsonb_typeof(operation->'attempt_uid') IS DISTINCT FROM 'string' THEN RAISE SQLSTATE 'PT422' USING MESSAGE='followup_invalid_attempt'; END IF;
    v_attempt:=(operation->>'attempt_uid')::uuid;
  END IF;
  v_cycle:=operation->>'cycle';
  IF v_kind IN ('ACQUIRE','REVIEW') AND (jsonb_typeof(operation->'cycle') IS DISTINCT FROM 'string' OR length(v_cycle) NOT BETWEEN 1 AND 1024) THEN
    RAISE SQLSTATE 'PT422' USING MESSAGE='followup_invalid_cycle';
  END IF;
  v_revision:=gen_random_uuid();
  IF v_kind='POLICY' THEN
    IF operation->'expected_revision' IS DISTINCT FROM coalesce(to_jsonb(v_policy.revision),'null'::jsonb) THEN
      RAISE SQLSTATE 'PT409' USING MESSAGE='followup_policy_changed';
    END IF;
    v_value:=operation->'value';
    IF jsonb_typeof(v_value) IS DISTINCT FROM 'object' OR v_value-ARRAY['enabled','weekday','repeat_days','preferred_time','quiet_start','quiet_end']<>'{}'::jsonb
      OR jsonb_typeof(v_value->'enabled') IS DISTINCT FROM 'boolean'
      OR jsonb_typeof(v_value->'weekday') IS DISTINCT FROM 'number' OR (v_value->>'weekday') !~ '^[1-7]$'
      OR jsonb_typeof(v_value->'repeat_days') IS DISTINCT FROM 'number' OR (v_value->>'repeat_days') !~ '^([1-9]|[12][0-9]|30)$'
      OR jsonb_typeof(v_value->'preferred_time') IS DISTINCT FROM 'string' OR (v_value->>'preferred_time') !~ '^([01][0-9]|2[0-3]):[0-5][0-9]$'
      OR jsonb_typeof(v_value->'quiet_start') IS DISTINCT FROM 'string' OR (v_value->>'quiet_start') !~ '^([01][0-9]|2[0-3]):[0-5][0-9]$'
      OR jsonb_typeof(v_value->'quiet_end') IS DISTINCT FROM 'string' OR (v_value->>'quiet_end') !~ '^([01][0-9]|2[0-3]):[0-5][0-9]$'
      OR v_value->>'quiet_start'=v_value->>'quiet_end' OR jsonb_typeof(operation->'make_primary') IS DISTINCT FROM 'boolean'
      OR jsonb_typeof(operation->'phones_updated') IS DISTINCT FROM 'boolean' THEN
      RAISE SQLSTATE 'PT422' USING MESSAGE='followup_invalid_policy';
    END IF;
    IF (CASE WHEN (v_value->>'quiet_start')::time<(v_value->>'quiet_end')::time
      THEN (v_value->>'preferred_time')::time>=(v_value->>'quiet_start')::time AND (v_value->>'preferred_time')::time<(v_value->>'quiet_end')::time
      ELSE (v_value->>'preferred_time')::time>=(v_value->>'quiet_start')::time OR (v_value->>'preferred_time')::time<(v_value->>'quiet_end')::time END) THEN
      RAISE SQLSTATE 'PT422' USING MESSAGE='followup_choose_time_outside_quiet_hours';
    END IF;
    IF (v_value->>'enabled')::boolean AND (operation->'phones_updated' IS DISTINCT FROM 'true'::jsonb
      OR NOT EXISTS(SELECT 1 FROM credit_private.contracts WHERE shop_id=shop)) THEN
      RAISE SQLSTATE 'PT422' USING MESSAGE='followup_upgrade_confirmation_required';
    END IF;
    IF NOT (v_value->>'enabled')::boolean AND EXISTS(SELECT 1 FROM followup_private.claims WHERE shop_id=shop AND phase='HANDOFF_STARTED') THEN
      RAISE SQLSTATE 'PT409' USING MESSAGE='followup_resolve_handoff_first';
    END IF;
    v_primary:=CASE WHEN (operation->>'make_primary')::boolean THEN installation ELSE v_policy.primary_installation END;
    v_primary_actor:=CASE WHEN (operation->>'make_primary')::boolean THEN v_actor ELSE v_policy.primary_actor END;
    IF (v_value->>'enabled')::boolean AND v_primary IS NULL THEN RAISE SQLSTATE 'PT422' USING MESSAGE='followup_primary_required'; END IF;
    v_schedule_revision:=coalesce(v_policy.schedule_revision,gen_random_uuid());
    v_effective:=coalesce(v_policy.effective_from,v_day);
    IF v_policy.weekday IS DISTINCT FROM (v_value->>'weekday')::int OR
      (NOT coalesce(v_policy.enabled,false) AND (v_value->>'enabled')::boolean) THEN
      v_schedule_revision:=gen_random_uuid(); v_effective:=v_day;
    END IF;
    INSERT INTO followup_private.policies VALUES(shop,v_revision,v_schedule_revision,v_effective,(v_value->>'enabled')::boolean,
      (v_value->>'weekday')::int,(v_value->>'repeat_days')::int,(v_value->>'preferred_time')::time,
      (v_value->>'quiet_start')::time,(v_value->>'quiet_end')::time,v_primary,v_primary_actor,v_actor,v_now)
    ON CONFLICT(shop_id) DO UPDATE SET revision=excluded.revision,schedule_revision=excluded.schedule_revision,effective_from=excluded.effective_from,
      enabled=excluded.enabled,weekday=excluded.weekday,repeat_days=excluded.repeat_days,preferred_time=excluded.preferred_time,
      quiet_start=excluded.quiet_start,quiet_end=excluded.quiet_end,primary_installation=excluded.primary_installation,
      primary_actor=excluded.primary_actor,updated_by=excluded.updated_by,updated_at=excluded.updated_at;
    v_result:=jsonb_build_object('revision',v_revision,'schedule_revision',v_schedule_revision);
  ELSIF v_kind='PREFERENCE' THEN
    v_value:=operation->'value';
    IF jsonb_typeof(v_value) IS DISTINCT FROM 'object' OR v_value-ARRAY['schedule','paused']<>'{}'::jsonb
      OR NOT v_value ? 'schedule' OR jsonb_typeof(v_value->'paused') IS DISTINCT FROM 'boolean' THEN
      RAISE SQLSTATE 'PT422' USING MESSAGE='followup_invalid_preference';
    END IF;
    PERFORM followup_private.assert_schedule(v_value->'schedule');
    v_schedule_revision:=v_customer.schedule_revision; v_effective:=v_customer.effective_from;
    IF (v_value->'schedule') IS DISTINCT FROM coalesce(v_customer.schedule,'null'::jsonb) THEN
      v_schedule_revision:=gen_random_uuid(); v_effective:=v_day;
    END IF;
    INSERT INTO followup_private.customers(shop_id,customer_uid,revision,schedule_revision,effective_from,schedule,paused)
      VALUES(shop,v_uid,v_revision,v_schedule_revision,v_effective,nullif(v_value->'schedule','null'::jsonb),(v_value->>'paused')::boolean)
    ON CONFLICT(shop_id,customer_uid) DO UPDATE SET revision=excluded.revision,schedule_revision=excluded.schedule_revision,
      effective_from=excluded.effective_from,schedule=excluded.schedule,paused=excluded.paused;
    v_result:=jsonb_build_object('revision',v_revision,'schedule_revision',v_schedule_revision);
  ELSIF v_kind IN ('REVIEW','SNOOZE') THEN
    IF v_claim.phase='HANDOFF_STARTED' OR v_claim.lease_until>v_now THEN RAISE SQLSTATE 'PT409' USING MESSAGE='followup_claim_busy'; END IF;
    IF v_kind='SNOOZE' THEN
      IF jsonb_typeof(operation->'until') IS DISTINCT FROM 'string' OR operation->>'until' !~ '^\d{4}-\d{2}-\d{2}$' THEN
        RAISE SQLSTATE 'PT422' USING MESSAGE='followup_invalid_snooze';
      END IF;
      v_until:=(operation->>'until')::date;
      IF v_until<=v_day OR v_until>v_day+365 THEN RAISE SQLSTATE 'PT422' USING MESSAGE='followup_invalid_snooze'; END IF;
    END IF;
    INSERT INTO followup_private.customers(shop_id,customer_uid,revision,snoozed_until,statement_cycle,reviewed_on)
      VALUES(shop,v_uid,v_revision,v_until,CASE WHEN v_kind='REVIEW' THEN v_cycle END,CASE WHEN v_kind='REVIEW' THEN v_day END)
    ON CONFLICT(shop_id,customer_uid) DO UPDATE SET revision=excluded.revision,
      snoozed_until=CASE WHEN v_kind='SNOOZE' THEN excluded.snoozed_until ELSE followup_private.customers.snoozed_until END,
      statement_cycle=CASE WHEN v_kind='REVIEW' THEN excluded.statement_cycle ELSE followup_private.customers.statement_cycle END,
      reviewed_on=CASE WHEN v_kind='REVIEW' THEN excluded.reviewed_on ELSE followup_private.customers.reviewed_on END;
    DELETE FROM followup_private.claims WHERE shop_id=shop AND customer_uid=v_uid;
    v_result:=jsonb_build_object('revision',v_revision,'reviewed_on',CASE WHEN v_kind='REVIEW' THEN v_day END,'snoozed_until',v_until);
  ELSIF v_kind='ACQUIRE' THEN
    IF v_claim.phase='HANDOFF_STARTED' OR v_claim.lease_until>v_now THEN RAISE SQLSTATE 'PT409' USING MESSAGE='followup_claim_busy'; END IF;
    IF EXISTS(SELECT 1 FROM followup_private.actions WHERE shop_id=shop AND attempt_uid=v_attempt AND kind='ACQUIRE') THEN
      RAISE SQLSTATE 'PT409' USING MESSAGE='followup_identity_conflict';
    END IF;
    INSERT INTO followup_private.claims(shop_id,customer_uid,attempt_uid,actor_id,installation,cycle,phase,lease_until)
      VALUES(shop,v_uid,v_attempt,v_actor,installation,v_cycle,'PREPARING',v_now+interval '5 minutes')
    ON CONFLICT(shop_id,customer_uid) DO UPDATE SET attempt_uid=excluded.attempt_uid,actor_id=excluded.actor_id,
      installation=excluded.installation,cycle=excluded.cycle,phase='PREPARING',lease_until=excluded.lease_until,started_at=NULL,channel=NULL;
    v_result:=jsonb_build_object('attempt_uid',v_attempt,'phase','PREPARING','lease_until',v_now+interval '5 minutes');
  ELSIF v_kind IN ('RENEW','START','RELEASE') THEN
    IF v_claim.attempt_uid IS DISTINCT FROM v_attempt OR v_claim.actor_id IS DISTINCT FROM v_actor
      OR v_claim.installation IS DISTINCT FROM installation OR v_claim.phase<>'PREPARING' OR v_claim.lease_until<=v_now THEN
      RAISE SQLSTATE 'PT409' USING MESSAGE='followup_preparation_lost';
    END IF;
    IF v_kind='START' THEN
      IF jsonb_typeof(operation->'channel') IS DISTINCT FROM 'string' OR operation->>'channel' NOT IN ('WHATSAPP','SMS') THEN
        RAISE SQLSTATE 'PT422' USING MESSAGE='followup_invalid_channel';
      END IF;
      UPDATE followup_private.claims SET phase='HANDOFF_STARTED',lease_until=NULL,started_at=v_now,channel=operation->>'channel'
        WHERE shop_id=shop AND customer_uid=v_uid;
      v_launch:=true;
    ELSIF v_kind='RENEW' THEN
      UPDATE followup_private.claims SET lease_until=v_now+interval '5 minutes' WHERE shop_id=shop AND customer_uid=v_uid;
    ELSE
      DELETE FROM followup_private.claims WHERE shop_id=shop AND customer_uid=v_uid;
    END IF;
    v_result:=jsonb_build_object('attempt_uid',v_attempt,'phase',CASE v_kind WHEN 'START' THEN 'HANDOFF_STARTED' WHEN 'RENEW' THEN 'PREPARING' ELSE 'RELEASED' END);
  ELSIF v_kind='RESOLVE' THEN
    IF v_claim.attempt_uid IS DISTINCT FROM v_attempt OR v_claim.phase IS DISTINCT FROM 'HANDOFF_STARTED' THEN
      RAISE SQLSTATE 'PT409' USING MESSAGE='followup_attempt_changed';
    END IF;
    IF (v_claim.actor_id<>v_actor OR v_claim.installation<>installation) AND v_role<>'owner' THEN
      RAISE SQLSTATE 'PT403' USING MESSAGE='followup_owner_resolution_required';
    END IF;
    IF operation->'acknowledge_uncertainty' IS DISTINCT FROM 'true'::jsonb OR jsonb_typeof(operation->'outcome') IS DISTINCT FROM 'string'
      OR operation->>'outcome' NOT IN ('REVIEWED','REPORTED_NOT_OPENED','RETRY_UNCERTAIN') THEN
      RAISE SQLSTATE 'PT422' USING MESSAGE='followup_resolution_confirmation_required';
    END IF;
    IF operation->>'outcome'='REVIEWED' THEN
      INSERT INTO followup_private.customers(shop_id,customer_uid,revision,statement_cycle,reviewed_on)
        VALUES(shop,v_uid,v_revision,v_claim.cycle,v_day)
      ON CONFLICT(shop_id,customer_uid) DO UPDATE SET revision=excluded.revision,statement_cycle=excluded.statement_cycle,reviewed_on=excluded.reviewed_on;
    END IF;
    DELETE FROM followup_private.claims WHERE shop_id=shop AND customer_uid=v_uid;
    v_cycle:=v_claim.cycle;
    v_result:=jsonb_build_object('attempt_uid',v_attempt,'operator_report',operation->>'outcome');
  ELSIF v_kind='DIGEST' THEN
    IF v_policy.primary_installation IS DISTINCT FROM installation OR v_policy.primary_actor IS DISTINCT FROM v_actor THEN
      RAISE SQLSTATE 'PT403' USING MESSAGE='followup_primary_required';
    END IF;
    IF operation->'has_work' IS DISTINCT FROM 'true'::jsonb OR v_time<v_policy.preferred_time OR
      (CASE WHEN v_policy.quiet_start<v_policy.quiet_end THEN v_time>=v_policy.quiet_start AND v_time<v_policy.quiet_end
        ELSE v_time>=v_policy.quiet_start OR v_time<v_policy.quiet_end END) THEN
      RAISE SQLSTATE 'PT409' USING MESSAGE='followup_digest_deferred';
    END IF;
    INSERT INTO followup_private.digests VALUES(shop,v_day,installation,v_actor,v_now) ON CONFLICT DO NOTHING;
    v_notify:=FOUND;
    v_result:=jsonb_build_object('business_date',v_day,'reserved',v_notify);
  END IF;
  INSERT INTO followup_private.actions(shop_id,command_uid,kind,actor_id,installation,customer_uid,attempt_uid,cycle,accepted_at)
    VALUES(shop,command_uid,CASE WHEN v_kind='RESOLVE' THEN operation->>'outcome' ELSE v_kind END,v_actor,installation,v_uid,v_attempt,v_cycle,v_now);
  INSERT INTO followup_private.commands VALUES(shop,command_uid,v_actor,installation,operation,v_result);
  RETURN jsonb_build_object('ok',true,'replayed',false,'launch_authorized',v_launch,'notify_authorized',v_notify,
    'result',v_result,'heads',followup_private.heads(shop,v_day));
EXCEPTION WHEN SQLSTATE 'PT409' OR SQLSTATE 'PT422' THEN
  RETURN jsonb_build_object('ok',false,'error',jsonb_build_object('code',SQLERRM));
WHEN invalid_text_representation OR invalid_datetime_format OR datetime_field_overflow OR numeric_value_out_of_range THEN
  RETURN jsonb_build_object('ok',false,'error',jsonb_build_object('code','followup_invalid_value'));
WHEN unique_violation THEN
  RETURN jsonb_build_object('ok',false,'error',jsonb_build_object('code','followup_identity_conflict'));
END $$;

REVOKE ALL ON ALL FUNCTIONS IN SCHEMA followup_private FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.followup_snapshot_v1(uuid) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.followup_command_v1(uuid,text,uuid,jsonb) FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.followup_snapshot_v1(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.followup_command_v1(uuid,text,uuid,jsonb) TO authenticated;
