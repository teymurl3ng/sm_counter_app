-- DEV only. Entire fixture transaction rolls back, including synthetic auth identities.
BEGIN;
CREATE TEMP TABLE f_ctx(owner_auth uuid,second_auth uuid,admin_auth uuid,cashier_auth uuid,outsider_auth uuid,inactive_auth uuid,
  shop uuid,nozone uuid,business uuid,other_business uuid,other_shop uuid);
INSERT INTO f_ctx SELECT gen_random_uuid(),gen_random_uuid(),gen_random_uuid(),gen_random_uuid(),gen_random_uuid(),gen_random_uuid(),
  gen_random_uuid(),gen_random_uuid(),gen_random_uuid(),gen_random_uuid(),gen_random_uuid();
CREATE TEMP TABLE f_results(label text PRIMARY KEY);
GRANT SELECT ON f_ctx TO authenticated,anon;
CREATE FUNCTION pg_temp.check_f(ok boolean,label text) RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path='' AS $$
BEGIN
  IF ok IS DISTINCT FROM true THEN RAISE EXCEPTION 'Follow-up check failed: %',label; END IF;
  INSERT INTO pg_temp.f_results VALUES(label);
END $$;
CREATE FUNCTION pg_temp.snap_f() RETURNS jsonb LANGUAGE sql SET search_path='' AS $$
  SELECT public.followup_snapshot_v1(shop) FROM pg_temp.f_ctx
$$;
CREATE FUNCTION pg_temp.send_f(k text,extra jsonb DEFAULT '{}',device text DEFAULT 'one',id uuid DEFAULT gen_random_uuid())
RETURNS jsonb LANGUAGE plpgsql SET search_path='' AS $$
DECLARE s jsonb; op jsonb; rev jsonb; sh uuid;
BEGIN
  s:=pg_temp.snap_f(); SELECT shop INTO sh FROM pg_temp.f_ctx;
  SELECT e->'revision' INTO rev FROM jsonb_array_elements(s->'customers') e WHERE e->>'customer_uid'=coalesce(extra->>'customer_uid','qa');
  op:=jsonb_build_object('kind',k);
  IF k='POLICY' THEN op:=op||jsonb_build_object('expected_revision',s#>'{policy,revision}');
  ELSIF k NOT IN ('DIGEST') THEN op:=op||jsonb_build_object('customer_uid','qa'); END IF;
  IF k IN ('PREFERENCE','ACQUIRE','START','REVIEW','SNOOZE','RESOLVE') THEN op:=op||jsonb_build_object('expected_customer_revision',rev); END IF;
  IF k IN ('ACQUIRE','START','REVIEW','SNOOZE','RESOLVE','DIGEST') THEN op:=op||jsonb_build_object('expected_heads',s->'heads'); END IF;
  RETURN public.followup_command_v1(sh,device,id,op||extra);
END $$;
CREATE FUNCTION pg_temp.denied_f(p_shop uuid) RETURNS boolean LANGUAGE plpgsql SET search_path='' AS $$
DECLARE hit boolean:=false;
BEGIN
  BEGIN PERFORM public.followup_snapshot_v1(p_shop); EXCEPTION WHEN SQLSTATE 'PT403' OR insufficient_privilege THEN hit:=true; END;
  IF NOT hit THEN RETURN false; END IF;
  hit:=false;
  BEGIN PERFORM public.followup_command_v1(p_shop,'one',gen_random_uuid(),'{"kind":"DIGEST"}');
  EXCEPTION WHEN SQLSTATE 'PT403' OR insufficient_privilege THEN hit:=true; END;
  RETURN hit;
END $$;
CREATE FUNCTION pg_temp.bump_f(credit boolean DEFAULT false) RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path='' AS $$
DECLARE c record;
BEGIN
  SELECT * INTO c FROM pg_temp.f_ctx;
  IF credit THEN
    INSERT INTO credit_private.records(shop_id,kind,uid,payload,accepted_by)
      SELECT c.shop,'REVIEW_ACTION','qa-'||gen_random_uuid(),'{}',id FROM public.staff WHERE auth_user_id=c.owner_auth;
  ELSE
    INSERT INTO public.sync_records(business_id,shop_id,table_name,row_uid,payload,origin_device)
      VALUES(c.business,c.shop,'events','qa-collection-'||gen_random_uuid(),'{"event_type":"CREDIT_COLLECTION"}','fixture');
  END IF;
END $$;
CREATE FUNCTION pg_temp.expire_f() RETURNS void LANGUAGE sql SECURITY DEFINER SET search_path='' AS $$
  UPDATE followup_private.claims SET lease_until=clock_timestamp()-interval '1 minute' WHERE shop_id=(SELECT shop FROM pg_temp.f_ctx) AND phase='PREPARING'
$$;
INSERT INTO public.businesses(id,name) SELECT business,'Follow-up rolled-back QA' FROM f_ctx UNION ALL SELECT other_business,'Unrelated rolled-back QA' FROM f_ctx;
INSERT INTO public.shops(id,business_id,name,shop_timezone)
  SELECT shop,business,'Follow-up QA','Asia/Karachi' FROM f_ctx UNION ALL SELECT nozone,business,'Timezone missing QA',NULL FROM f_ctx
  UNION ALL SELECT other_shop,other_business,'Unrelated QA','Asia/Karachi' FROM f_ctx;
INSERT INTO auth.users(id) SELECT x FROM f_ctx,LATERAL unnest(ARRAY[owner_auth,second_auth,admin_auth,cashier_auth,outsider_auth,inactive_auth]) x;
INSERT INTO public.staff(auth_user_id,business_id,username,display_name,role,active)
  SELECT owner_auth,business,'qa_f_'||substr(owner_auth::text,1,20),'QA','owner',true FROM f_ctx UNION ALL
  SELECT second_auth,business,'qa_f_'||substr(second_auth::text,1,20),'QA','owner',true FROM f_ctx UNION ALL
  SELECT admin_auth,business,'qa_f_'||substr(admin_auth::text,1,20),'QA','admin',true FROM f_ctx UNION ALL
  SELECT cashier_auth,business,'qa_f_'||substr(cashier_auth::text,1,20),'QA','cashier',true FROM f_ctx UNION ALL
  SELECT outsider_auth,other_business,'qa_f_'||substr(outsider_auth::text,1,20),'QA','owner',true FROM f_ctx UNION ALL
  SELECT inactive_auth,business,'qa_f_'||substr(inactive_auth::text,1,20),'QA','owner',false FROM f_ctx;
INSERT INTO public.staff_shops(staff_id,shop_id) SELECT s.id,c.shop FROM f_ctx c JOIN public.staff s ON s.auth_user_id IN (c.admin_auth,c.cashier_auth);
INSERT INTO public.sync_records(business_id,shop_id,table_name,row_uid,payload,origin_device)
 SELECT business,shop,'accounts','qa','{"category":"Customer","name":"PRIVATE CUSTOMER NAME"}'::jsonb,'fixture' FROM f_ctx UNION ALL
 SELECT business,shop,'accounts','qa-two','{"category":"Customer"}'::jsonb,'fixture' FROM f_ctx;
SELECT pg_temp.check_f(NOT has_schema_privilege('authenticated','followup_private','USAGE') AND
  NOT has_table_privilege('authenticated','followup_private.policies','SELECT') AND
  (SELECT bool_and(relrowsecurity) FROM pg_class WHERE relnamespace='followup_private'::regnamespace AND relkind='r'),'private schema denies direct access and all tables enable RLS');
SELECT pg_temp.check_f(NOT has_function_privilege('anon','public.followup_snapshot_v1(uuid)','EXECUTE') AND
  NOT has_function_privilege('anon','public.followup_command_v1(uuid,text,uuid,jsonb)','EXECUTE') AND
  has_function_privilege('authenticated','public.followup_command_v1(uuid,text,uuid,jsonb)','EXECUTE'),'RPC grants explicitly restrict anonymous callers');
SET LOCAL ROLE anon;
SELECT pg_temp.check_f(pg_temp.denied_f(shop),'anonymous role denied both RPCs') FROM f_ctx;
SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claim.sub',cashier_auth::text,true) FROM f_ctx;
SELECT pg_temp.check_f(pg_temp.denied_f(shop),'cashier denied both RPCs') FROM f_ctx;
SELECT set_config('request.jwt.claim.sub',inactive_auth::text,true) FROM f_ctx;
SELECT pg_temp.check_f(pg_temp.denied_f(shop),'inactive staff denied both RPCs') FROM f_ctx;
SELECT set_config('request.jwt.claim.sub',outsider_auth::text,true) FROM f_ctx;
SELECT pg_temp.check_f(pg_temp.denied_f(shop),'unrelated business denied both RPCs') FROM f_ctx;
SELECT set_config('request.jwt.claim.sub',owner_auth::text,true) FROM f_ctx;
DO $checks$
DECLARE c record; s jsonb; a jsonb; b jsonb; op jsonb; value jsonb; saved jsonb; old_heads jsonb; rev text; sched text;
  id uuid; attempt uuid; attempt_two uuid; day date; invalid jsonb; finance_count bigint;
BEGIN
 SELECT * INTO c FROM pg_temp.f_ctx; s:=pg_temp.snap_f(); day:=(s#>>'{heads,business_date}')::date;
 PERFORM pg_temp.check_f(s->'policy'='null'::jsonb AND jsonb_array_length(s->'customers')=0,'migration creates no active shop policy');
 PERFORM pg_temp.check_f(s::text NOT LIKE '%PRIVATE CUSTOMER NAME%','snapshot contains no customer names or balances');
 PERFORM pg_temp.check_f(public.followup_snapshot_v1(c.nozone)#>'{heads,business_date}'='null'::jsonb,'missing timezone remains explicit');
 value:='{"enabled":true,"weekday":7,"repeat_days":7,"preferred_time":"00:00","quiet_start":"23:58","quiet_end":"23:59"}';
 op:=jsonb_build_object('kind','POLICY','expected_revision',NULL,'value',value,'make_primary',true,'phones_updated',true);
 a:=public.followup_command_v1(c.nozone,'one',gen_random_uuid(),op);
 PERFORM pg_temp.check_f(a#>>'{error,code}'='followup_shop_timezone_required','missing timezone refuses activation');
 a:=pg_temp.send_f('POLICY',op-'kind');
 PERFORM pg_temp.check_f(a#>>'{error,code}'='followup_upgrade_confirmation_required','credit capability is required before opt-in');
 PERFORM public.credit_contract_enable_v1(c.shop,true);
 a:=pg_temp.send_f('POLICY',(op-'kind')||'{"phones_updated":false}');
 PERFORM pg_temp.check_f(a#>>'{error,code}'='followup_upgrade_confirmation_required','explicit posting-phone upgrade acknowledgment is required');
 id:=gen_random_uuid(); a:=pg_temp.send_f('POLICY',op-'kind','one',id);
 PERFORM pg_temp.check_f(a->'ok'='true'::jsonb AND a->'launch_authorized'='false'::jsonb,'Owner opt-in creates accepted policy without external action');
 b:=pg_temp.send_f('POLICY',op-'kind','one',id);
 PERFORM pg_temp.check_f(b->'replayed'='true'::jsonb,'lost policy response is idempotent');
 a:=pg_temp.send_f('POLICY',(op-'kind')||'{"phones_updated":false}','one',id);
 PERFORM pg_temp.check_f(a#>>'{error,code}'='followup_command_identity_conflict','command UID cannot acquire a different meaning');
 a:=pg_temp.send_f('POLICY',op-'kind');
 PERFORM pg_temp.check_f(a#>>'{error,code}'='followup_policy_changed','old policy revision cannot overwrite accepted settings');
 s:=pg_temp.snap_f(); sched:=s#>>'{policy,schedule_revision}';
 a:=pg_temp.send_f('POLICY',jsonb_build_object('value',value||'{"repeat_days":10}','make_primary',false,'phones_updated',true));
 PERFORM pg_temp.check_f(a->'ok'='true'::jsonb AND pg_temp.snap_f()#>>'{policy,schedule_revision}'=sched,'non-cadence setting preserves statement cycle revision');
 FOR invalid IN SELECT x FROM jsonb_array_elements('[{"weekday":0},{"repeat_days":31},{"preferred_time":"24:00"},{"quiet_end":"23:58"},{"weekday":"7"},{"balance":100}]') x LOOP
   a:=pg_temp.send_f('POLICY',jsonb_build_object('value',value||invalid,'make_primary',false,'phones_updated',true));
   PERFORM pg_temp.check_f(a#>>'{error,code}'='followup_invalid_policy','invalid policy rejected: '||invalid::text);
 END LOOP;
 a:=pg_temp.send_f('POLICY',jsonb_build_object('value',value||'{"preferred_time":"23:58"}','make_primary',false,'phones_updated',true));
 PERFORM pg_temp.check_f(a#>>'{error,code}'='followup_choose_time_outside_quiet_hours','preferred time cannot become permanently trapped in quiet hours');
 a:=pg_temp.send_f('PREFERENCE','{"value":{"schedule":{"kind":"MONTHLY","day":31},"paused":false}}');
 PERFORM pg_temp.check_f(a->'ok'='true'::jsonb,'monthly customer schedule accepted separately from sale terms');
 sched:=a#>>'{result,schedule_revision}';
 a:=pg_temp.send_f('PREFERENCE','{"value":{"schedule":{"kind":"MONTHLY","day":31},"paused":true}}');
 PERFORM pg_temp.check_f(a#>>'{result,schedule_revision}'=sched,'pause preserves schedule and cycle identity');
 a:=pg_temp.send_f('ACQUIRE',jsonb_build_object('attempt_uid',gen_random_uuid(),'cycle','cycle-1'));
 PERFORM pg_temp.check_f(a#>>'{error,code}'='followup_customer_postponed','paused customer cannot acquire a handoff');
 a:=pg_temp.send_f('PREFERENCE','{"value":{"schedule":{"kind":"MONTHLY","day":31},"paused":false}}');
 PERFORM pg_temp.check_f(a#>>'{result,schedule_revision}'=sched,'resuming preserves schedule revision');
 FOR invalid IN SELECT x FROM jsonb_array_elements('[{"kind":"MONTHLY","day":32},{"kind":"WEEKLY","weekday":0},{"kind":"INTERVAL","days":366,"first_date":"2026-10-16"},{"kind":"INTERVAL","days":7,"first_date":"2027-02-30"},{"kind":"WEEKLY","weekday":7,"payment_terms":1}]') x LOOP
   a:=pg_temp.send_f('PREFERENCE',jsonb_build_object('value',jsonb_build_object('schedule',invalid,'paused',false)));
   PERFORM pg_temp.check_f(a->'ok'='false'::jsonb,'invalid customer cadence rejected: '||invalid::text);
 END LOOP;
 a:=pg_temp.send_f('PREFERENCE',jsonb_build_object('value',jsonb_build_object('schedule',jsonb_build_object('kind','INTERVAL','days',10,'first_date',day),'paused',false)));
 PERFORM pg_temp.check_f(a->'ok'='true'::jsonb AND a#>>'{result,schedule_revision}'<>sched,'custom cadence changes schedule revision');
 a:=pg_temp.send_f('PREFERENCE','{"expected_customer_revision":null,"value":{"schedule":null,"paused":false}}');
 PERFORM pg_temp.check_f(a#>>'{error,code}'='followup_customer_changed','stale customer edit cannot overwrite accepted preference');
 a:=pg_temp.send_f('ACQUIRE',jsonb_build_object('customer_uid','missing','attempt_uid',gen_random_uuid(),'cycle','cycle-1'));
 PERFORM pg_temp.check_f(a#>>'{error,code}'='followup_customer_missing','unknown customer is rejected');
 a:=pg_temp.send_f('SNOOZE',jsonb_build_object('until',day));
 PERFORM pg_temp.check_f(a#>>'{error,code}'='followup_invalid_snooze','snooze must name a future shop date');
 a:=pg_temp.send_f('SNOOZE',jsonb_build_object('until',day+1));
 PERFORM pg_temp.check_f(a->'ok'='true'::jsonb,'future snooze accepted');
 a:=pg_temp.send_f('ACQUIRE',jsonb_build_object('attempt_uid',gen_random_uuid(),'cycle','cycle-1'));
 PERFORM pg_temp.check_f(a#>>'{error,code}'='followup_customer_postponed','snooze blocks a handoff before its business date');
 -- Other customer is not snoozed; all further preparation checks use that UID.
 attempt:=gen_random_uuid(); op:=jsonb_build_object('customer_uid','qa-two','attempt_uid',attempt,'cycle','cycle-two');
 id:=gen_random_uuid(); saved:=op||jsonb_build_object('expected_heads',pg_temp.snap_f()->'heads');
 a:=pg_temp.send_f('ACQUIRE',saved,'one',id);
 PERFORM pg_temp.check_f(a->'ok'='true'::jsonb,'one preparation lease acquired');
 b:=pg_temp.send_f('ACQUIRE',saved,'one',id);
 PERFORM pg_temp.check_f(b->'replayed'='true'::jsonb AND b->'launch_authorized'='false'::jsonb,'acquire retry preserves attempt without external launch');
 a:=pg_temp.send_f('ACQUIRE',op||jsonb_build_object('attempt_uid',gen_random_uuid()),'two');
 PERFORM pg_temp.check_f(a#>>'{error,code}'='followup_claim_busy','second installation cannot claim the same customer');
 a:=pg_temp.send_f('REVIEW','{"customer_uid":"qa-two","cycle":"cycle-two"}','two');
 PERFORM pg_temp.check_f(a#>>'{error,code}'='followup_claim_busy','review cannot silently clear live preparation');
 a:=pg_temp.send_f('RENEW',op-ARRAY['cycle'],'two');
 PERFORM pg_temp.check_f(a#>>'{error,code}'='followup_preparation_lost','another installation cannot renew the claim');
 a:=pg_temp.send_f('RENEW',op-ARRAY['cycle']);
 PERFORM pg_temp.check_f(a->'ok'='true'::jsonb,'original installation renews its live lease');
 old_heads:=pg_temp.snap_f()->'heads'; PERFORM pg_temp.bump_f(false);
 a:=pg_temp.send_f('START',(op-'cycle')||jsonb_build_object('expected_heads',old_heads,'channel','SMS'));
 PERFORM pg_temp.check_f(a#>>'{error,code}'='followup_refresh_required','new synced collection head invalidates prepared facts');
 old_heads:=pg_temp.snap_f()->'heads'; PERFORM pg_temp.bump_f(true);
 a:=pg_temp.send_f('START',(op-'cycle')||jsonb_build_object('expected_heads',old_heads,'channel','SMS'));
 PERFORM pg_temp.check_f(a#>>'{error,code}'='followup_refresh_required','new Credit head invalidates prepared facts');
 old_heads:=pg_temp.snap_f()->'heads';
 a:=pg_temp.send_f('PREFERENCE','{"value":{"schedule":{"kind":"MONTHLY","day":31},"paused":true}}');
 a:=pg_temp.send_f('START',(op-'cycle')||jsonb_build_object('expected_heads',old_heads,'channel','SMS'));
 PERFORM pg_temp.check_f(a#>>'{error,code}'='followup_refresh_required','another metadata edit requires a fresh review');
 PERFORM pg_temp.expire_f();
 a:=pg_temp.send_f('START',(op-'cycle')||'{"channel":"SMS"}');
 PERFORM pg_temp.check_f(a#>>'{error,code}'='followup_preparation_lost','expired lease cannot start');
 attempt_two:=gen_random_uuid(); op:=op||jsonb_build_object('attempt_uid',attempt_two);
 a:=pg_temp.send_f('ACQUIRE',op,'two');
 PERFORM pg_temp.check_f(a->'ok'='true'::jsonb,'expired preparation may be replaced by another installation');
 id:=gen_random_uuid(); saved:=(op-'cycle')||jsonb_build_object('kind','START','expected_heads',pg_temp.snap_f()->'heads',
   'expected_customer_revision',NULL,'channel','WHATSAPP');
 a:=public.followup_command_v1(c.shop,'two',id,saved);
 PERFORM pg_temp.check_f(a->'launch_authorized'='true'::jsonb AND a#>>'{result,phase}'='HANDOFF_STARTED','original accepted start grants one launch');
 b:=public.followup_command_v1(c.shop,'two',id,saved);
 PERFORM pg_temp.check_f(b->'replayed'='true'::jsonb AND b->'launch_authorized'='false'::jsonb,'lost start response replay never authorizes another launch');
 a:=pg_temp.send_f('RELEASE',op-'cycle','two');
 PERFORM pg_temp.check_f(a#>>'{error,code}'='followup_preparation_lost','started handoff cannot expire or release as preparation');
 a:=pg_temp.send_f('ACQUIRE',op||jsonb_build_object('attempt_uid',gen_random_uuid()),'one');
 PERFORM pg_temp.check_f(a#>>'{error,code}'='followup_claim_busy','unknown handoff blocks a second attempt');
 a:=pg_temp.send_f('POLICY',jsonb_build_object('value',value||'{"enabled":false}','make_primary',false,'phones_updated',false));
 PERFORM pg_temp.check_f(a#>>'{error,code}'='followup_resolve_handoff_first','disabling cannot bypass an unresolved handoff');
 a:=pg_temp.send_f('RESOLVE',(op-'cycle')||'{"outcome":"RETRY_UNCERTAIN","acknowledge_uncertainty":false}','two');
 PERFORM pg_temp.check_f(a#>>'{error,code}'='followup_resolution_confirmation_required','uncertain retry requires explicit acknowledgment');
 a:=pg_temp.send_f('RESOLVE',(op-'cycle')||'{"outcome":"RETRY_UNCERTAIN","acknowledge_uncertainty":true}','one');
 PERFORM pg_temp.check_f(a->'ok'='true'::jsonb AND a#>>'{result,operator_report}'='RETRY_UNCERTAIN','Owner can explicitly resolve another installation without asserting delivery');
 a:=pg_temp.send_f('ACQUIRE',op,'one');
 PERFORM pg_temp.check_f(a#>>'{error,code}'='followup_identity_conflict','old attempt identity cannot be reused after resolution');
 attempt:=gen_random_uuid(); op:=op||jsonb_build_object('attempt_uid',attempt);
 a:=pg_temp.send_f('ACQUIRE',op);
 a:=pg_temp.send_f('START',(op-'cycle')||'{"channel":"SMS"}');
 a:=pg_temp.send_f('RESOLVE',(op-'cycle')||'{"outcome":"REVIEWED","acknowledge_uncertainty":true}');
 PERFORM pg_temp.check_f(a->'ok'='true'::jsonb AND (SELECT e->>'statement_cycle' FROM jsonb_array_elements(pg_temp.snap_f()->'customers') e WHERE e->>'customer_uid'='qa-two')='cycle-two','review acknowledges only the exact started cycle');
 a:=pg_temp.send_f('REVIEW','{"customer_uid":"qa-two","cycle":"new-cycle"}');
 PERFORM pg_temp.check_f(a->'ok'='true'::jsonb,'explicit shared review records current shop date');
 SELECT count(*) INTO finance_count FROM public.sync_records WHERE shop_id=c.shop;
 a:=pg_temp.send_f('DIGEST','{"has_work":false}');
 PERFORM pg_temp.check_f(a#>>'{error,code}'='followup_digest_deferred','no eligible work creates no notification reservation');
 -- Test policy above uses a one-minute quiet window far from the current afternoon shop time.
 id:=gen_random_uuid(); saved:=jsonb_build_object('kind','DIGEST','has_work',true,'expected_heads',pg_temp.snap_f()->'heads');
 a:=public.followup_command_v1(c.shop,'one',id,saved);
 PERFORM pg_temp.check_f(a->'notify_authorized'='true'::jsonb,'first primary digest grants one notification attempt');
 b:=public.followup_command_v1(c.shop,'one',id,saved);
 PERFORM pg_temp.check_f(b->'notify_authorized'='false'::jsonb AND b->'replayed'='true'::jsonb,'lost digest response replay never authorizes another alert');
 a:=pg_temp.send_f('DIGEST','{"has_work":true}');
 PERFORM pg_temp.check_f(a->'ok'='true'::jsonb AND a->'notify_authorized'='false'::jsonb,'new command on same shop date cannot reserve twice');
 a:=pg_temp.send_f('POLICY',jsonb_build_object('value',value,'make_primary',true,'phones_updated',true),'two');
 PERFORM pg_temp.check_f(a->'ok'='true'::jsonb,'Owner can explicitly transfer primary installation');
 a:=pg_temp.send_f('DIGEST','{"has_work":true}','two');
 PERFORM pg_temp.check_f(a->'notify_authorized'='false'::jsonb,'primary transfer does not create another daily alert');
 BEGIN
   PERFORM pg_temp.send_f('DIGEST','{"has_work":true}','one');
   RAISE EXCEPTION 'old primary unexpectedly allowed';
 EXCEPTION WHEN SQLSTATE 'PT403' THEN PERFORM pg_temp.check_f(true,'old primary rejected after transfer'); END;
 PERFORM pg_temp.check_f((SELECT count(*) FROM public.sync_records WHERE shop_id=c.shop)=finance_count,'review and notification commands make no financial writes');
 -- Preserve literal outcomes for the next date/missed-worker checks in Android B.
 old_heads:=pg_temp.snap_f()->'heads';
 a:=pg_temp.send_f('DIGEST',jsonb_build_object('has_work',true,'expected_heads',old_heads||jsonb_build_object('business_date',day-1)),'two');
 PERFORM pg_temp.check_f(a#>>'{error,code}'='followup_refresh_required','missed worker cannot replay an old business date');
END $checks$;
SELECT set_config('request.jwt.claim.sub',admin_auth::text,true) FROM f_ctx;
DO $admin$
DECLARE c record; a jsonb;
BEGIN
 SELECT * INTO c FROM pg_temp.f_ctx;
 PERFORM pg_temp.check_f(pg_temp.snap_f()->'ok'='true'::jsonb,'assigned Admin may read current follow-ups');
 BEGIN
   PERFORM public.followup_command_v1(c.shop,'admin',gen_random_uuid(),'{"kind":"POLICY"}');
   RAISE EXCEPTION 'admin policy unexpectedly allowed';
 EXCEPTION WHEN SQLSTATE 'PT403' THEN PERFORM pg_temp.check_f(true,'Admin cannot change Owner policy'); END;
 a:=pg_temp.send_f('PREFERENCE','{"value":{"schedule":null,"paused":false}}','admin');
 PERFORM pg_temp.check_f(a->'ok'='true'::jsonb,'assigned leadership may change customer follow-up preference');
END $admin$;
RESET ROLE;
SELECT jsonb_build_object('passed',count(*),'checks',jsonb_agg(label ORDER BY label),'rollback',true) AS verification FROM f_results;
ROLLBACK;
