-- PROD deployment verification: read calls and EXPLAIN only, no fixture/history writes.
BEGIN;
SET LOCAL statement_timeout = '15s';
SET LOCAL lock_timeout = '3s';
DO $verify$
DECLARE
  owner_row record;
  profile jsonb;
  overview jsonb;
  pulled jsonb;
  redacted jsonb;
  rejected boolean;
  outcome jsonb := '{}'::jsonb;
BEGIN
  SELECT s.*, sh.id AS verification_shop_id INTO STRICT owner_row
  FROM public.staff s JOIN public.shops sh ON sh.business_id=s.business_id AND sh.active
  WHERE s.role='owner' AND s.active AND s.terminated_at IS NULL AND s.auth_user_id IS NOT NULL
  ORDER BY s.created_at,sh.created_at LIMIT 1;
  PERFORM set_config('request.jwt.claims',jsonb_build_object('role','authenticated','sub',owner_row.auth_user_id::text)::text,true);
  EXECUTE 'SET LOCAL ROLE authenticated';
  profile := public.my_profile();
  IF profile->>'staff_id' IS DISTINCT FROM owner_row.id::text OR profile->>'active' IS DISTINCT FROM 'true' THEN
    RAISE EXCEPTION 'Owner profile no longer resolves'; END IF;
  overview := public.admin_overview();
  IF overview IS NULL OR jsonb_typeof(overview)<>'object' THEN RAISE EXCEPTION 'Owner overview failed'; END IF;
  pulled := public.sync_pull(0,500,owner_row.verification_shop_id);
  IF pulled->>'ok' IS DISTINCT FROM 'true' OR jsonb_typeof(pulled->'records')<>'array' THEN
    RAISE EXCEPTION 'Owner sync pull failed'; END IF;
  outcome := outcome || jsonb_build_object('owner_profile_read',true,'owner_overview_read',true,'owner_sync_pull_read',true,'pulled_records',jsonb_array_length(pulled->'records'));
  rejected := false;
  BEGIN
    EXECUTE 'EXPLAIN INSERT INTO public.sync_records (business_id,shop_id,table_name,row_uid,payload,origin_device) VALUES (NULL,NULL,''settings'',''permission_plan_only'',''{}''::jsonb,''plan_only'')';
  EXCEPTION WHEN SQLSTATE '42501' THEN rejected := true; END;
  IF NOT rejected THEN RAISE EXCEPTION 'Direct authenticated insert planning unexpectedly allowed'; END IF;
  outcome := outcome || jsonb_build_object('authenticated_direct_insert_planning_denied',true);
  EXECUTE 'RESET ROLE';
  PERFORM set_config('request.jwt.claims','{"role":"anon"}',true);
  EXECUTE 'SET LOCAL ROLE anon';
  PERFORM public.ping();
  rejected := false;
  BEGIN EXECUTE 'EXPLAIN SELECT server_seq FROM public.sync_records';
  EXCEPTION WHEN SQLSTATE '42501' THEN rejected := true; END;
  IF NOT rejected THEN RAISE EXCEPTION 'Anonymous table read planning unexpectedly allowed'; END IF;
  outcome := outcome || jsonb_build_object('anonymous_ping_available',true,'anonymous_sync_table_read_planning_denied',true);
  EXECUTE 'RESET ROLE';
  PERFORM set_config('request.jwt.claims','{"role":"service_role"}',true);
  EXECUTE 'SET LOCAL ROLE service_role';
  redacted := public.redact_json_secrets('{"password":"synthetic-private-value","nested":{"access_token":"synthetic-private-value","ordinary":"kept"}}'::jsonb);
  IF redacted::text LIKE '%synthetic-private-value%' OR redacted#>>'{nested,ordinary}' IS DISTINCT FROM 'kept' THEN
    RAISE EXCEPTION 'Recursive redactor failed'; END IF;
  outcome := outcome || jsonb_build_object('service_recursive_redactor',true);
  EXECUTE 'RESET ROLE';
  PERFORM set_config('saaf.audit.prod_read_result',outcome::text,true);
END;
$verify$;
SELECT current_setting('saaf.audit.prod_read_result')::jsonb AS verification;
ROLLBACK;
