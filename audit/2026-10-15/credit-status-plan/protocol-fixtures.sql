-- Run only inside the migration verification transaction; no synthetic identity survives ROLLBACK.
CREATE TEMP TABLE credit_qa_context(owner_auth uuid,cashier_auth uuid,admin_auth uuid,outsider_auth uuid,shop uuid,other_shop uuid,business uuid,other_business uuid);
INSERT INTO credit_qa_context VALUES(gen_random_uuid(),gen_random_uuid(),gen_random_uuid(),gen_random_uuid(),gen_random_uuid(),gen_random_uuid(),gen_random_uuid(),gen_random_uuid());
CREATE TEMP TABLE credit_qa_results(label text PRIMARY KEY);
CREATE FUNCTION pg_temp.credit_check(ok boolean,label text) RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path='' AS $$
BEGIN
 IF ok IS DISTINCT FROM true THEN RAISE EXCEPTION 'Credit QA failed: %',label; END IF;
 INSERT INTO pg_temp.credit_qa_results VALUES(label);
END $$;
CREATE FUNCTION pg_temp.credit_send(uid text,payload jsonb,shop uuid) RETURNS jsonb LANGUAGE sql SET search_path='' AS $$
 SELECT public.sync_push(jsonb_build_array(jsonb_build_object('table','events','uid',uid,'payload',payload)),'qa-device',shop,0)
$$;
GRANT SELECT ON credit_qa_context TO authenticated,anon;
INSERT INTO public.businesses(id,name) SELECT business,'Synthetic Credit Protocol QA' FROM credit_qa_context
  UNION ALL SELECT other_business,'Synthetic unrelated Credit QA' FROM credit_qa_context;
INSERT INTO public.shops(id,business_id,name) SELECT shop,business,'Credit protocol QA' FROM credit_qa_context
  UNION ALL SELECT other_shop,other_business,'Unrelated credit QA' FROM credit_qa_context;
INSERT INTO auth.users(id) SELECT owner_auth FROM credit_qa_context UNION ALL SELECT cashier_auth FROM credit_qa_context
  UNION ALL SELECT admin_auth FROM credit_qa_context UNION ALL SELECT outsider_auth FROM credit_qa_context;
INSERT INTO public.staff(auth_user_id,business_id,username,display_name,role)
 SELECT owner_auth,business,'qa.'||substr(owner_auth::text,1,20),'Credit QA Owner','owner' FROM credit_qa_context UNION ALL
 SELECT cashier_auth,business,'qa.'||substr(cashier_auth::text,1,20),'Credit QA Cashier','cashier' FROM credit_qa_context UNION ALL
 SELECT admin_auth,business,'qa.'||substr(admin_auth::text,1,20),'Credit QA Admin','admin' FROM credit_qa_context UNION ALL
 SELECT outsider_auth,other_business,'qa.'||substr(outsider_auth::text,1,20),'Unrelated QA Owner','owner' FROM credit_qa_context;
INSERT INTO public.staff_shops(staff_id,shop_id) SELECT s.id,c.shop FROM credit_qa_context c JOIN public.staff s ON s.auth_user_id IN (c.cashier_auth,c.admin_auth);
INSERT INTO public.sync_records(business_id,shop_id,table_name,row_uid,payload,origin_device)
 SELECT business,shop,'accounts','qa-customer','{"name":"Customer: QA","category":"Customer","type":"Asset"}'::jsonb,'qa-device' FROM credit_qa_context UNION ALL
 SELECT business,shop,'accounts','qa-cash','{"name":"Cash","category":"Cash/Bank","type":"Asset"}'::jsonb,'qa-device' FROM credit_qa_context;
SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claim.sub',owner_auth::text,true) FROM credit_qa_context;
DO $qa$
DECLARE c record; response jsonb; legacy jsonb; sale jsonb; agreement jsonb; ops jsonb; revision jsonb; a jsonb; count_before int; flag boolean;
BEGIN
 SELECT * INTO c FROM pg_temp.credit_qa_context;
 PERFORM pg_temp.credit_check((public.credit_contract_status_v1(c.shop)->>'enabled')::boolean=false,'inactive shop remains inactive');
 legacy:=jsonb_build_object('event_type','SALE','channel','credit-retail','occurred_at','2026-10-01','sale_quantity',1,'sale_value',100,'unit_rate',100,
   'financial_logs',jsonb_build_array(jsonb_build_object('account_uid','qa-customer','amount',100)),'inventory_logs','[]'::jsonb);
 response:=public.sync_push(jsonb_build_array(jsonb_build_object('table','events','uid','qa-legacy','payload',legacy)),'qa-device',c.shop,0);
 PERFORM pg_temp.credit_check((response->>'ok')::boolean,'legacy sale accepted before activation');
 PERFORM pg_temp.credit_check(jsonb_array_length(public.credit_metadata_pull_v1(c.shop)->'records')=0,'no automatic activation or metadata leak');
 flag:=false; BEGIN PERFORM public.credit_contract_enable_v1(c.shop,false); EXCEPTION WHEN SQLSTATE 'PT422' THEN flag:=true; END;
 PERFORM pg_temp.credit_check(flag,'activation requires explicit upgrade confirmation');
 response:=public.credit_contract_enable_v1(c.shop,true);
 PERFORM pg_temp.credit_check((response->>'enabled')::boolean,'explicit owner activation');
 response:=public.credit_metadata_pull_v1(c.shop);
 PERFORM pg_temp.credit_check(jsonb_array_length(response->'records')=1 AND response#>>'{records,0,kind}'='LEGACY_AGREEMENT','legacy originals explicitly classified in separate feed');
 PERFORM public.credit_contract_enable_v1(c.shop,true);
 PERFORM pg_temp.credit_check(jsonb_array_length(public.credit_metadata_pull_v1(c.shop)->'records')=1,'activation retry is idempotent');
 revision:='{"revision_uid":"qa-monthly","customer_uid":"qa-customer","prior_revision_uid":"weekly-default-v1","term_kind":"CALENDAR_MONTHS","term_value":1,"local_actor_uid":"local-owner-label","contract_version":1}';
 ops:=jsonb_build_array(jsonb_build_object('kind','TERM_REVISION','uid','qa-monthly','payload',revision));
 response:=public.credit_metadata_push_v1(c.shop,ops);
 PERFORM pg_temp.credit_check((response->>'ok')::boolean AND response#>>'{accepted,0,payload,accepted_at}' IS NOT NULL,'owner monthly revision accepted with server stamp');
 response:=public.credit_metadata_push_v1(c.shop,ops);
 PERFORM pg_temp.credit_check((response->>'ok')::boolean AND jsonb_array_length(public.credit_metadata_pull_v1(c.shop)->'records')=2,'revision retry idempotent');
 response:=public.credit_metadata_push_v1(c.shop,jsonb_build_array(jsonb_build_object('kind','TERM_REVISION','uid','qa-stale','payload',revision||'{"revision_uid":"qa-stale"}')));
 PERFORM pg_temp.credit_check(response#>>'{error,code}'='credit_terms_revision_conflict','stale revision rejected without head overwrite');
 response:=public.credit_metadata_push_v1(c.shop,jsonb_build_array(jsonb_build_object('kind','TERM_REVISION','uid','qa-monthly','payload',revision||'{"term_kind":"NET_DAYS","term_value":30}')));
 PERFORM pg_temp.credit_check(response#>>'{error,code}'='credit_identity_conflict','changed revision identity rejected');
 response:=public.credit_metadata_push_v1(c.shop,jsonb_build_array(jsonb_build_object('kind','TERM_REVISION','uid','qa-fractional','payload',revision||'{"revision_uid":"qa-fractional","prior_revision_uid":"qa-monthly","term_kind":"NET_DAYS","term_value":7.5}')));
 PERFORM pg_temp.credit_check(response#>>'{error,code}'='credit_invalid_term','fractional term rejected');
 a:='{"action_uid":"qa-set-a","customer_uid":"qa-customer","kind":"SET","clears_set_uid":null,"reason":"Synthetic review","local_actor_uid":"local-owner-label","occurred_at":"2026-10-15T08:00:00Z","business_date":"2026-10-15","contract_version":1}';
 ops:=jsonb_build_array(jsonb_build_object('kind','REVIEW_ACTION','uid','qa-set-a','payload',a),jsonb_build_object('kind','REVIEW_ACTION','uid','qa-set-b','payload',a||'{"action_uid":"qa-set-b"}'));
 PERFORM pg_temp.credit_check((public.credit_metadata_push_v1(c.shop,ops)->>'ok')::boolean,'concurrent sets accepted as distinct history');
 ops:=jsonb_build_array(jsonb_build_object('kind','REVIEW_ACTION','uid','qa-clear-a','payload',a||'{"action_uid":"qa-clear-a","kind":"CLEAR","clears_set_uid":"qa-set-a"}'));
 PERFORM pg_temp.credit_check((public.credit_metadata_push_v1(c.shop,ops)->>'ok')::boolean,'clear references observed set only');
 PERFORM pg_temp.credit_check((public.credit_metadata_push_v1(c.shop,ops)->>'ok')::boolean,'clear retry idempotent');
 response:=public.credit_metadata_push_v1(c.shop,jsonb_build_array(jsonb_build_object('kind','REVIEW_ACTION','uid','qa-clear-missing','payload',a||'{"action_uid":"qa-clear-missing","kind":"CLEAR","clears_set_uid":"qa-missing"}')));
 PERFORM pg_temp.credit_check(response#>>'{error,code}'='credit_clear_dependency_missing','unknown clear dependency refused');
 count_before:=jsonb_array_length(public.credit_metadata_pull_v1(c.shop)->'records');
 ops:=jsonb_build_array(jsonb_build_object('kind','REVIEW_ACTION','uid','qa-rolled-back','payload',a||'{"action_uid":"qa-rolled-back"}'),
   jsonb_build_object('kind','REVIEW_ACTION','uid','qa-bad','payload',a||'{"action_uid":"qa-bad","reason":""}'));
 response:=public.credit_metadata_push_v1(c.shop,ops);
 PERFORM pg_temp.credit_check(response#>>'{error,code}'='credit_malformed_review' AND jsonb_array_length(public.credit_metadata_pull_v1(c.shop)->'records')=count_before,'metadata batch failure rolls back earlier operations');
 response:=public.sync_push(jsonb_build_array(jsonb_build_object('table','events','uid','qa-old-new','payload',legacy)),'qa-device',c.shop,0);
 PERFORM pg_temp.credit_check(response#>>'{error,code}'='credit_contract_required','old client new credit sale refused after activation');
 response:=public.sync_push(jsonb_build_array(jsonb_build_object('table','events','uid','qa-legacy','payload',legacy)),'qa-device',c.shop,0);
 PERFORM pg_temp.credit_check((response->>'ok')::boolean,'identical legacy retry still accepted after activation');
 agreement:='{"root_sale_uid":"qa-contracted","customer_uid":"qa-customer","evidence":"RECORDED","term_kind":"CALENDAR_MONTHS","term_value":1,"revision_uid":"qa-monthly","contract_version":1}';
 sale:=legacy||jsonb_build_object('credit_contract_version',1,'credit_agreement',agreement);
 response:=public.sync_push(jsonb_build_array(jsonb_build_object('table','events','uid','qa-contracted','payload',sale)),'qa-device',c.shop,0);
 PERFORM pg_temp.credit_check((response->>'ok')::boolean,'sale and monthly agreement accepted atomically');
 response:=public.sync_push(jsonb_build_array(jsonb_build_object('table','events','uid','qa-contracted','payload',sale)),'qa-device',c.shop,0);
 PERFORM pg_temp.credit_check((response->>'ok')::boolean,'contracted sale retry accepted');
 response:=pg_temp.credit_send('qa-contracted',sale||jsonb_build_object('credit_agreement',agreement||'{"term_kind":"NET_DAYS","term_value":7}'),c.shop);
 PERFORM pg_temp.credit_check(response#>>'{error,code}'='credit_agreement_conflict','same sale cannot acknowledge a changed agreement');
 response:=pg_temp.credit_send('qa-bad-agreement',sale||jsonb_build_object('credit_agreement',agreement||'{"root_sale_uid":"qa-bad-agreement","revision_uid":"missing"}'),c.shop);
 PERFORM pg_temp.credit_check(response#>>'{error,code}'='credit_agreement_revision_missing','missing accepted revision refused');
 response:=pg_temp.credit_send('qa-paid',legacy||'{"channel":"retail","financial_logs":[{"account_uid":"qa-cash","amount":100}]}',c.shop);
 PERFORM pg_temp.credit_check((response->>'ok')::boolean,'paid-now sale unchanged');
 response:=public.credit_metadata_push_v1(c.shop,jsonb_build_array(jsonb_build_object('kind','TERM_REVISION','uid','qa-thirty-days',
   'payload',revision||'{"revision_uid":"qa-thirty-days","prior_revision_uid":"qa-monthly","term_kind":"NET_DAYS","term_value":30}')));
 PERFORM pg_temp.credit_check((response->>'ok')::boolean,'future preference advances accepted head');
 response:=pg_temp.credit_send('qa-offline-old-terms',sale||jsonb_build_object('credit_agreement',agreement||'{"root_sale_uid":"qa-offline-old-terms"}'),c.shop);
 PERFORM pg_temp.credit_check((response->>'ok')::boolean,'offline sale may retain historically accepted agreement');
 response:=pg_temp.credit_send('qa-wrong-finance',sale||jsonb_build_object('credit_agreement',agreement||'{"root_sale_uid":"qa-wrong-finance"}',
   'financial_logs','[{"account_uid":"qa-customer","amount":100},{"account_uid":"qa-cash","amount":1}]'::jsonb),c.shop);
 PERFORM pg_temp.credit_check(response#>>'{error,code}'='credit_agreement_finance_mismatch','agreement cannot hide another financial destination');
 response:=public.sync_push(jsonb_build_array(
   jsonb_build_object('table','events','uid','qa-atomic-first','payload',sale||jsonb_build_object('credit_agreement',agreement||'{"root_sale_uid":"qa-atomic-first"}')),
   jsonb_build_object('table','events','uid','qa-atomic-bad','payload',sale||jsonb_build_object('credit_agreement',agreement||'{"root_sale_uid":"qa-atomic-bad","revision_uid":"missing"}'))),
   'qa-device',c.shop,0);
 PERFORM pg_temp.credit_check(response#>>'{error,code}'='credit_agreement_revision_missing' AND
   NOT EXISTS(SELECT 1 FROM jsonb_array_elements(public.sync_pull(0,500,c.shop)->'records') x WHERE x->>'uid'='qa-atomic-first'),
   'bad agreement rolls back earlier global batch sale');
 response:=public.credit_metadata_push_v1(c.shop,jsonb_build_array(jsonb_build_object('kind','TERM_REVISION','uid','qa-foreign',
   'payload',revision||'{"revision_uid":"qa-foreign","customer_uid":"other-shop-customer"}')));
 PERFORM pg_temp.credit_check(response#>>'{error,code}'='credit_customer_missing','metadata customer must resolve in the same shop');
 -- The old feed contains event facts, never new metadata table types.
 response:=public.sync_pull(0,500,c.shop);
 PERFORM pg_temp.credit_check(NOT EXISTS(SELECT 1 FROM jsonb_array_elements(response->'records') r WHERE r->>'table' LIKE 'credit%'),'old feed excludes metadata types');
 flag:=false; BEGIN PERFORM public.credit_metadata_pull_v1(c.other_shop); EXCEPTION WHEN SQLSTATE 'PT403' THEN flag:=true; END;
 PERFORM pg_temp.credit_check(flag,'owner cannot access unrelated business');
 flag:=false; BEGIN PERFORM 1 FROM credit_private.records; EXCEPTION WHEN insufficient_privilege THEN flag:=true; END;
 PERFORM pg_temp.credit_check(flag,'authenticated cannot directly read private metadata');
END $qa$;
SELECT set_config('request.jwt.claim.sub',cashier_auth::text,true) FROM credit_qa_context;
DO $qa$
DECLARE c record; flag boolean; response jsonb;
BEGIN
 SELECT * INTO c FROM pg_temp.credit_qa_context;
 response:=public.credit_metadata_pull_v1(c.shop);
 PERFORM pg_temp.credit_check((response->>'ok')::boolean,'cashier may read assigned shop metadata');
 flag:=false; BEGIN PERFORM public.credit_contract_enable_v1(c.shop,true); EXCEPTION WHEN SQLSTATE 'PT403' THEN flag:=true; END;
 PERFORM pg_temp.credit_check(flag,'cashier cannot activate');
 flag:=false; BEGIN PERFORM public.credit_metadata_push_v1(c.shop,'[]'); EXCEPTION WHEN SQLSTATE 'PT403' THEN flag:=true; END;
 PERFORM pg_temp.credit_check(flag,'cashier cannot write terms or flags');
 response:=pg_temp.credit_send('qa-cashier-sale','{"event_type":"SALE","channel":"credit-retail","financial_logs":[{"account_uid":"qa-customer","amount":100}],
   "credit_contract_version":1,"credit_agreement":{"root_sale_uid":"qa-cashier-sale","customer_uid":"qa-customer","evidence":"RECORDED","term_kind":"CALENDAR_MONTHS","term_value":1,"revision_uid":"qa-monthly","contract_version":1}}',c.shop);
 PERFORM pg_temp.credit_check((response->>'ok')::boolean,'cashier may post original sale with valid agreement');
END $qa$;
SELECT set_config('request.jwt.claim.sub',admin_auth::text,true) FROM credit_qa_context;
DO $qa$
DECLARE c record; flag boolean;
BEGIN
 SELECT * INTO c FROM pg_temp.credit_qa_context;
 flag:=false; BEGIN PERFORM public.credit_metadata_push_v1(c.shop,'[]'); EXCEPTION WHEN SQLSTATE 'PT403' THEN flag:=true; END;
 PERFORM pg_temp.credit_check(flag,'admin master-data access does not grant owner credit edits');
END $qa$;
RESET ROLE;
UPDATE public.staff SET role='owner' WHERE auth_user_id=(SELECT admin_auth FROM credit_qa_context);
UPDATE public.staff SET active=false WHERE auth_user_id=(SELECT owner_auth FROM credit_qa_context);
SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claim.sub',owner_auth::text,true) FROM credit_qa_context;
DO $qa$
DECLARE c record; flag boolean;
BEGIN
 SELECT * INTO c FROM pg_temp.credit_qa_context;
 flag:=false; BEGIN PERFORM public.credit_contract_status_v1(c.shop); EXCEPTION WHEN SQLSTATE 'PT403' THEN flag:=true; END;
 PERFORM pg_temp.credit_check(flag,'inactive server owner loses access despite existing identity');
END $qa$;
SET LOCAL ROLE anon;
DO $qa$
DECLARE c record; flag boolean;
BEGIN
 SELECT * INTO c FROM pg_temp.credit_qa_context;
 flag:=false; BEGIN PERFORM public.credit_contract_status_v1(c.shop); EXCEPTION WHEN insufficient_privilege THEN flag:=true; END;
 PERFORM pg_temp.credit_check(flag,'anonymous has no RPC execution grant');
END $qa$;
RESET ROLE;
SELECT jsonb_build_object('passed',count(*),'checks',jsonb_agg(label ORDER BY label)) as credit_protocol_test_result FROM credit_qa_results;
