-- DEV only: deployed DB behavior, not a GoTrue/JWT/session test.
-- Every inserted/updated row is rolled back. Sequence nextval is not transactional.
BEGIN;
SET LOCAL statement_timeout = '15s';
SET LOCAL lock_timeout = '3s';
DO $verify$
DECLARE
  owner_row record;
  fixture_business uuid := gen_random_uuid();
  fixture_owner uuid := gen_random_uuid();
  reconciliation uuid;
  unlinked_identity uuid := gen_random_uuid();
  selected_shop uuid;
  fixture_uid text := 'audit_rollback_' || gen_random_uuid()::text;
  receipt jsonb;
  pulled jsonb;
  rejected boolean;
  identity_ok boolean;
  outcome jsonb := '{}'::jsonb;
BEGIN
  SELECT * INTO STRICT owner_row FROM public.staff
  WHERE role='owner' AND active AND terminated_at IS NULL AND auth_user_id IS NOT NULL
  ORDER BY created_at LIMIT 1;
  SELECT id INTO STRICT selected_shop FROM public.shops
  WHERE business_id=owner_row.business_id AND active ORDER BY created_at LIMIT 1;

  INSERT INTO public.businesses(id,name) VALUES(fixture_business,'Disposable rollback check');
  INSERT INTO public.staff(id,business_id,username,display_name,role)
  VALUES(fixture_owner,fixture_business,'audit_'||substr(replace(fixture_owner::text,'-',''),1,24),'Disposable rollback owner','owner');
  rejected := false;
  BEGIN UPDATE public.staff SET active=false WHERE id=fixture_owner;
  EXCEPTION WHEN SQLSTATE 'P0001' THEN rejected := true; END;
  IF NOT rejected THEN RAISE EXCEPTION 'Final owner disable unexpectedly permitted'; END IF;
  rejected := false;
  BEGIN UPDATE public.staff SET role='cashier' WHERE id=fixture_owner;
  EXCEPTION WHEN SQLSTATE 'P0001' THEN rejected := true; END;
  IF NOT rejected THEN RAISE EXCEPTION 'Final owner demotion unexpectedly permitted'; END IF;
  rejected := false;
  BEGIN DELETE FROM public.staff WHERE id=fixture_owner;
  EXCEPTION WHEN SQLSTATE 'P0001' THEN rejected := true; END;
  IF NOT rejected THEN RAISE EXCEPTION 'Final owner deletion unexpectedly permitted'; END IF;
  outcome := outcome || jsonb_build_object('final_owner_disable_demote_delete_denied',true);

  PERFORM set_config('request.jwt.claims',jsonb_build_object('role','authenticated','sub',owner_row.auth_user_id::text)::text,true);
  EXECUTE 'SET LOCAL ROLE authenticated';
  IF current_user <> 'authenticated' THEN RAISE EXCEPTION 'Incorrect role for push check'; END IF;
  receipt := public.sync_push(jsonb_build_array(jsonb_build_object('table','settings','uid',fixture_uid,'payload',jsonb_build_object('audit_rollback',true))),'audit_rollback',selected_shop,NULL);
  IF receipt->>'ok' <> 'true' OR receipt->>'applied' <> '1' THEN RAISE EXCEPTION 'Ordinary RPC push failed: %',receipt; END IF;
  pulled := public.sync_pull(0,500,selected_shop);
  IF pulled->>'ok' <> 'true' OR NOT EXISTS(SELECT 1 FROM jsonb_array_elements(pulled->'records') r WHERE r->>'uid'=fixture_uid) THEN
    RAISE EXCEPTION 'Ordinary RPC pull did not return the inserted fixture';
  END IF;
  outcome := outcome || jsonb_build_object('authenticated_rpc_push_pull_round_trip',true);
  receipt := public.sync_push(jsonb_build_array(jsonb_build_object('table','unknown_table','uid',fixture_uid,'payload','{}'::jsonb)),'audit_rollback',selected_shop,NULL);
  IF receipt->'error'->>'code' <> 'unknown_table' THEN RAISE EXCEPTION 'Unknown table was not rejected'; END IF;
  outcome := outcome || jsonb_build_object('rpc_table_whitelist_preserved',true);
  EXECUTE 'RESET ROLE';

  PERFORM set_config('request.jwt.claims','{"role":"service_role"}',true);
  EXECUTE 'SET LOCAL ROLE service_role';
  reconciliation := public.admin_record_auth_reconciliation(owner_row.business_id,'create_verify',owner_row.auth_user_id,owner_row.username,owner_row.id,owner_row.username,'UNREACHABLE',gen_random_uuid());
  IF NOT public.admin_verify_created_identity(reconciliation) THEN RAISE EXCEPTION 'Linked identity verification failed'; END IF;
  EXECUTE 'RESET ROLE';
  SELECT ar.state='resolved' AND ar.resolved AND ar.error_code IS NULL INTO identity_ok FROM public.auth_reconciliation ar WHERE ar.id=reconciliation;
  IF NOT identity_ok THEN RAISE EXCEPTION 'Linked identity reconciliation was not resolved'; END IF;
  outcome := outcome || jsonb_build_object('linked_identity_reconciliation_resolves',true);

  EXECUTE 'SET LOCAL ROLE service_role';
  reconciliation := public.admin_record_auth_reconciliation(owner_row.business_id,'create_verify',unlinked_identity,'audit_unlinked',owner_row.id,owner_row.username,'UNREACHABLE',gen_random_uuid());
  IF public.admin_verify_created_identity(reconciliation) THEN RAISE EXCEPTION 'Unlinked identity incorrectly verified'; END IF;
  EXECUTE 'RESET ROLE';
  SELECT ar.state='pending' AND NOT ar.resolved INTO identity_ok FROM public.auth_reconciliation ar WHERE ar.id=reconciliation;
  IF NOT identity_ok THEN RAISE EXCEPTION 'Unlinked identity was not retained for reconciliation'; END IF;
  outcome := outcome || jsonb_build_object('unlinked_identity_remains_pending',true);

  PERFORM set_config('saaf.audit.rollback_result',outcome::text,true);
END;
$verify$;
SELECT current_setting('saaf.audit.rollback_result')::jsonb AS verification;
ROLLBACK;
