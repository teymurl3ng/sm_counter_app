-- DEV-tested phase 4 contract. Do not activate an existing shop as part of migration.
CREATE SCHEMA credit_private;
REVOKE ALL ON SCHEMA credit_private FROM PUBLIC, anon, authenticated;

CREATE TABLE credit_private.contracts (
  shop_id uuid PRIMARY KEY REFERENCES public.shops(id) ON DELETE CASCADE,
  activated_at timestamptz NOT NULL DEFAULT now(),
  activated_by uuid NOT NULL REFERENCES public.staff(id)
);
CREATE TABLE credit_private.records (
  server_seq bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  shop_id uuid NOT NULL REFERENCES public.shops(id) ON DELETE CASCADE,
  kind text NOT NULL CHECK(kind IN ('TERM_REVISION','REVIEW_ACTION','LEGACY_AGREEMENT')),
  uid text NOT NULL CHECK(length(uid) BETWEEN 1 AND 200),
  payload jsonb NOT NULL CHECK(jsonb_typeof(payload)='object'),
  accepted_by uuid NOT NULL REFERENCES public.staff(id),
  accepted_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(shop_id,kind,uid)
);
CREATE INDEX credit_records_shop_seq ON credit_private.records(shop_id,server_seq);
CREATE TABLE credit_private.heads (
  shop_id uuid NOT NULL REFERENCES public.shops(id) ON DELETE CASCADE,
  customer_uid text NOT NULL,
  revision_uid text NOT NULL,
  PRIMARY KEY(shop_id,customer_uid)
);
ALTER TABLE credit_private.contracts ENABLE ROW LEVEL SECURITY;
ALTER TABLE credit_private.records ENABLE ROW LEVEL SECURITY;
ALTER TABLE credit_private.heads ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON ALL TABLES IN SCHEMA credit_private FROM PUBLIC, anon, authenticated;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA credit_private FROM PUBLIC, anon, authenticated;

-- Every exposed RPC resolves the actual active server actor and shop. Client audit labels never authorize.
CREATE FUNCTION credit_private.actor(p_shop uuid, p_owner boolean DEFAULT false)
RETURNS uuid LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path='' AS $$
DECLARE a public.staff%rowtype;
BEGIN
  SELECT * INTO a FROM public.staff WHERE auth_user_id=auth.uid() AND active AND terminated_at IS NULL;
  IF NOT FOUND OR p_shop IS NULL OR NOT EXISTS(SELECT 1 FROM public.sync_staff_shop_ids() s WHERE s=p_shop) THEN
    RAISE SQLSTATE 'PT403' USING MESSAGE='credit_not_authorized';
  END IF;
  IF a.role NOT IN ('owner','admin','manager','cashier') OR (p_owner AND a.role<>'owner') THEN
    RAISE SQLSTATE 'PT403' USING MESSAGE='credit_owner_required';
  END IF;
  RETURN a.id;
END $$;

CREATE FUNCTION credit_private.assert_term(p jsonb) RETURNS void
LANGUAGE plpgsql IMMUTABLE SET search_path='' AS $$
BEGIN
  IF jsonb_typeof(p->'contract_version') IS DISTINCT FROM 'number' OR p->>'contract_version'<>'1'
     OR jsonb_typeof(p->'term_kind') IS DISTINCT FROM 'string'
     OR jsonb_typeof(p->'term_value') IS DISTINCT FROM 'number'
     OR (p->>'term_value') !~ '^[0-9]{1,3}$'
     OR NOT ((p->>'term_kind'='NET_DAYS' AND (p->>'term_value')::int BETWEEN 1 AND 365)
       OR (p->>'term_kind'='CALENDAR_MONTHS' AND p->>'term_value'='1')) THEN
    RAISE SQLSTATE 'PT422' USING MESSAGE='credit_invalid_term';
  END IF;
END $$;

CREATE FUNCTION credit_private.assert_customer(p_shop uuid,p_uid text) RETURNS void
LANGUAGE plpgsql STABLE SET search_path='' AS $$
BEGIN
  IF p_uid IS NULL OR NOT EXISTS(SELECT 1 FROM public.sync_records
    WHERE shop_id=p_shop AND table_name='accounts' AND row_uid=p_uid AND payload->>'category'='Customer') THEN
    RAISE SQLSTATE 'PT422' USING MESSAGE='credit_customer_missing';
  END IF;
END $$;

CREATE FUNCTION public.credit_contract_status_v1(shop uuid) RETURNS jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path='' AS $$
BEGIN
  PERFORM credit_private.actor(shop);
  RETURN jsonb_build_object('ok',true,'contract_version',1,'enabled',EXISTS(SELECT 1 FROM credit_private.contracts WHERE shop_id=shop),
    'shop_id',shop,'server_time_epoch_ms',(extract(epoch FROM now())*1000)::bigint);
END $$;

CREATE FUNCTION public.credit_contract_enable_v1(shop uuid, posting_phones_updated boolean) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path='' AS $$
DECLARE actor_id uuid; r record; customer text;
BEGIN
  actor_id:=credit_private.actor(shop,true);
  IF posting_phones_updated IS DISTINCT FROM true THEN RAISE SQLSTATE 'PT422' USING MESSAGE='credit_upgrade_confirmation_required'; END IF;
  PERFORM pg_advisory_xact_lock(hashtextextended(shop::text,0));
  IF NOT EXISTS(SELECT 1 FROM credit_private.contracts WHERE shop_id=shop) THEN
    -- Classify authoritative pre-activation originals, not merely missing client metadata.
    FOR r IN SELECT row_uid,payload FROM public.sync_records WHERE shop_id=shop AND table_name='events'
      AND payload->>'event_type'='SALE' AND nullif(payload->>'corrects_event_uid','') IS NULL LOOP
      SELECT min(f->>'account_uid') INTO customer FROM jsonb_array_elements(coalesce(r.payload->'financial_logs','[]')) f
        JOIN public.sync_records a ON a.shop_id=shop AND a.table_name='accounts' AND a.row_uid=f->>'account_uid'
        WHERE a.payload->>'category'='Customer'
        HAVING count(DISTINCT f->>'account_uid')=1;
      IF customer IS NOT NULL THEN
        INSERT INTO credit_private.records(shop_id,kind,uid,payload,accepted_by) VALUES(shop,'LEGACY_AGREEMENT',r.row_uid,
          jsonb_build_object('root_sale_uid',r.row_uid,'customer_uid',customer,'evidence','LEGACY_WEEKLY','term_kind','NET_DAYS',
            'term_value',7,'revision_uid','weekly-default-v1','contract_version',1),actor_id);
      ELSIF r.payload->>'channel' IN ('credit-retail','credit-wholesale') THEN
        RAISE SQLSTATE 'PT422' USING MESSAGE='credit_legacy_customer_unresolved';
      END IF;
    END LOOP;
    INSERT INTO credit_private.contracts(shop_id,activated_by) VALUES(shop,actor_id);
  END IF;
  RETURN public.credit_contract_status_v1(shop);
END $$;

CREATE FUNCTION public.credit_metadata_push_v1(shop uuid, operations jsonb) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path='' AS $$
DECLARE actor_id uuid; op jsonb; p jsonb; k text; u text; customer text; prior text; target jsonb; old jsonb;
  accepted jsonb:='[]'; accepted_at_text text;
BEGIN
  actor_id:=credit_private.actor(shop,true);
  IF jsonb_typeof(operations) IS DISTINCT FROM 'array' OR jsonb_array_length(operations)>100 THEN
    RAISE SQLSTATE 'PT422' USING MESSAGE='credit_invalid_operations';
  END IF;
  PERFORM pg_advisory_xact_lock(hashtextextended(shop::text,0));
  IF NOT EXISTS(SELECT 1 FROM credit_private.contracts WHERE shop_id=shop) THEN RAISE SQLSTATE 'PT422' USING MESSAGE='credit_contract_not_enabled'; END IF;
  FOR op IN SELECT value FROM jsonb_array_elements(operations) LOOP
    k:=op->>'kind'; u:=op->>'uid'; p:=op->'payload'; customer:=p->>'customer_uid';
    IF k IS NULL OR k NOT IN ('TERM_REVISION','REVIEW_ACTION') OR jsonb_typeof(op->'uid') IS DISTINCT FROM 'string'
      OR length(u) NOT BETWEEN 1 AND 200 OR jsonb_typeof(p) IS DISTINCT FROM 'object'
      OR jsonb_typeof(p->'local_actor_uid') IS DISTINCT FROM 'string' OR length(btrim(p->>'local_actor_uid')) NOT BETWEEN 1 AND 200
      OR jsonb_typeof(p->'contract_version') IS DISTINCT FROM 'number' OR p->>'contract_version'<>'1' THEN
      RAISE SQLSTATE 'PT422' USING MESSAGE='credit_malformed_operation';
    END IF;
    PERFORM credit_private.assert_customer(shop,customer);
    SELECT payload INTO old FROM credit_private.records WHERE shop_id=shop AND kind=k AND uid=u;
    IF FOUND THEN
      IF (old-'accepted_at') IS DISTINCT FROM p THEN RAISE SQLSTATE 'PT422' USING MESSAGE='credit_identity_conflict'; END IF;
      accepted:=accepted||jsonb_build_array(jsonb_build_object('kind',k,'uid',u,'payload',old));
      CONTINUE;
    END IF;
    IF k='TERM_REVISION' THEN
      IF p->>'revision_uid' IS DISTINCT FROM u OR u='weekly-default-v1' OR
        p - ARRAY['revision_uid','customer_uid','prior_revision_uid','term_kind','term_value','local_actor_uid','contract_version'] <> '{}'::jsonb THEN
        RAISE SQLSTATE 'PT422' USING MESSAGE='credit_malformed_revision';
      END IF;
      PERFORM credit_private.assert_term(p);
      SELECT revision_uid INTO prior FROM credit_private.heads WHERE shop_id=shop AND customer_uid=customer;
      prior:=coalesce(prior,'weekly-default-v1');
      IF p->>'prior_revision_uid' IS DISTINCT FROM prior THEN RAISE SQLSTATE 'PT422' USING MESSAGE='credit_terms_revision_conflict'; END IF;
      accepted_at_text:=to_char(clock_timestamp() AT TIME ZONE 'UTC','YYYY-MM-DD"T"HH24:MI:SS.US"Z"');
      p:=p||jsonb_build_object('accepted_at',accepted_at_text);
      INSERT INTO credit_private.heads(shop_id,customer_uid,revision_uid) VALUES(shop,customer,u)
        ON CONFLICT(shop_id,customer_uid) DO UPDATE SET revision_uid=excluded.revision_uid;
    ELSE
      IF p->>'action_uid' IS DISTINCT FROM u OR p->>'kind' IS NULL OR p->>'kind' NOT IN ('SET','CLEAR')
        OR jsonb_typeof(p->'reason') IS DISTINCT FROM 'string' OR length(btrim(p->>'reason')) NOT BETWEEN 1 AND 500
        OR jsonb_typeof(p->'business_date') IS DISTINCT FROM 'string' OR (p->>'business_date') !~ '^\d{4}-\d{2}-\d{2}$'
        OR jsonb_typeof(p->'occurred_at') IS DISTINCT FROM 'string' OR (p->>'occurred_at') !~ '(Z|[+-]\d{2}:\d{2})$'
        OR NOT p ? 'clears_set_uid'
        OR p - ARRAY['action_uid','customer_uid','kind','clears_set_uid','reason','local_actor_uid','occurred_at','business_date','contract_version'] <> '{}'::jsonb THEN
        RAISE SQLSTATE 'PT422' USING MESSAGE='credit_malformed_review';
      END IF;
      PERFORM (p->>'business_date')::date; PERFORM (p->>'occurred_at')::timestamptz;
      IF p->>'kind'='SET' THEN
        IF p->'clears_set_uid' IS DISTINCT FROM 'null'::jsonb THEN RAISE SQLSTATE 'PT422' USING MESSAGE='credit_invalid_clear'; END IF;
      ELSE
        SELECT payload INTO target FROM credit_private.records WHERE shop_id=shop AND kind='REVIEW_ACTION' AND uid=p->>'clears_set_uid';
        IF NOT FOUND OR target->>'customer_uid' IS DISTINCT FROM customer OR target->>'kind'<>'SET' THEN
          RAISE SQLSTATE 'PT422' USING MESSAGE='credit_clear_dependency_missing';
        END IF;
      END IF;
    END IF;
    INSERT INTO credit_private.records(shop_id,kind,uid,payload,accepted_by) VALUES(shop,k,u,p,actor_id);
    accepted:=accepted||jsonb_build_array(jsonb_build_object('kind',k,'uid',u,'payload',p));
  END LOOP;
  RETURN jsonb_build_object('ok',true,'accepted',accepted,'server_time_epoch_ms',(extract(epoch FROM now())*1000)::bigint);
EXCEPTION WHEN SQLSTATE 'PT422' THEN
  RETURN jsonb_build_object('ok',false,'error',jsonb_build_object('code',SQLERRM));
WHEN invalid_datetime_format OR datetime_field_overflow THEN
  RETURN jsonb_build_object('ok',false,'error',jsonb_build_object('code','credit_invalid_date'));
END $$;

CREATE FUNCTION public.credit_metadata_pull_v1(shop uuid, since_seq bigint DEFAULT 0, max_rows int DEFAULT 100) RETURNS jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path='' AS $$
DECLARE result jsonb;
BEGIN
  PERFORM credit_private.actor(shop);
  IF since_seq IS NULL OR since_seq<0 OR max_rows IS NULL OR max_rows NOT BETWEEN 1 AND 500 THEN
    RAISE SQLSTATE 'PT422' USING MESSAGE='credit_invalid_cursor';
  END IF;
  SELECT coalesce(jsonb_agg(jsonb_build_object('server_seq',r.server_seq,'shop_id',r.shop_id,'kind',r.kind,'uid',r.uid,'payload',r.payload)
    ORDER BY r.server_seq),'[]'::jsonb) INTO result
    FROM (SELECT * FROM credit_private.records WHERE shop_id=shop AND server_seq>since_seq ORDER BY server_seq LIMIT max_rows) r;
  RETURN jsonb_build_object('ok',true,'records',result,'contract_version',1,'server_time_epoch_ms',(extract(epoch FROM now())*1000)::bigint);
END $$;

-- Called only after the existing global RPC has resolved the active server actor and locked the shop.
CREATE FUNCTION credit_private.validate_sale(p_shop uuid,p_uid text,p jsonb,p_existing jsonb) RETURNS void
LANGUAGE plpgsql SET search_path='' AS $$
DECLARE a jsonb; revision jsonb; finance_row jsonb; customer text; customer_count int;
BEGIN
  IF NOT (p ? 'credit_agreement' OR p ? 'credit_contract_version') AND
    NOT EXISTS(SELECT 1 FROM credit_private.contracts WHERE shop_id=p_shop) THEN RETURN; END IF;
  IF p->>'event_type' IS DISTINCT FROM 'SALE' OR nullif(p->>'corrects_event_uid','') IS NOT NULL THEN
    IF p ? 'credit_agreement' OR p ? 'credit_contract_version' THEN RAISE SQLSTATE 'PT422' USING MESSAGE='credit_agreement_wrong_event'; END IF;
    RETURN;
  END IF;
  IF jsonb_typeof(p->'financial_logs') IS DISTINCT FROM 'array' THEN
    IF p ? 'credit_agreement' OR p->>'channel' IN ('credit-retail','credit-wholesale') THEN RAISE SQLSTATE 'PT422' USING MESSAGE='credit_sale_customer_invalid'; END IF;
    RETURN;
  END IF;
  SELECT count(DISTINCT movement.value->>'account_uid'),min(movement.value->>'account_uid') INTO customer_count,customer
    FROM jsonb_array_elements(p->'financial_logs') AS movement(value) JOIN public.sync_records acc
    ON acc.shop_id=p_shop AND acc.table_name='accounts' AND acc.row_uid=movement.value->>'account_uid' WHERE acc.payload->>'category'='Customer';
  IF customer_count=0 AND coalesce(p->>'channel','') NOT IN ('credit-retail','credit-wholesale') THEN
    IF p ? 'credit_agreement' OR p ? 'credit_contract_version' THEN RAISE SQLSTATE 'PT422' USING MESSAGE='credit_agreement_paid_sale'; END IF;
    RETURN;
  END IF;
  IF customer_count<>1 THEN RAISE SQLSTATE 'PT422' USING MESSAGE='credit_sale_customer_invalid'; END IF;
  IF p_existing IS NOT NULL THEN
    IF (p_existing->'credit_agreement') IS DISTINCT FROM (p->'credit_agreement') OR
       (p_existing->'credit_contract_version') IS DISTINCT FROM (p->'credit_contract_version') THEN
      RAISE SQLSTATE 'PT422' USING MESSAGE='credit_agreement_conflict';
    END IF;
    RETURN; -- Same pre-contract retries retain their original agreement/evidence.
  END IF;
  IF NOT EXISTS(SELECT 1 FROM credit_private.contracts WHERE shop_id=p_shop) THEN
    IF p ? 'credit_agreement' OR p ? 'credit_contract_version' THEN RAISE SQLSTATE 'PT422' USING MESSAGE='credit_contract_not_enabled'; END IF;
    RETURN;
  END IF;
  IF NOT p ? 'credit_agreement' THEN RAISE SQLSTATE 'PT422' USING MESSAGE='credit_contract_required'; END IF;
  a:=p->'credit_agreement';
  IF jsonb_typeof(a) IS DISTINCT FROM 'object' OR p->'credit_contract_version' IS DISTINCT FROM '1'::jsonb OR
    a->>'root_sale_uid' IS DISTINCT FROM p_uid OR a->>'customer_uid' IS DISTINCT FROM customer OR
    a->>'evidence' IS DISTINCT FROM 'RECORDED' OR jsonb_typeof(a->'revision_uid') IS DISTINCT FROM 'string' OR
    a - ARRAY['root_sale_uid','customer_uid','evidence','term_kind','term_value','revision_uid','contract_version'] <> '{}'::jsonb THEN
    RAISE SQLSTATE 'PT422' USING MESSAGE='credit_agreement_invalid';
  END IF;
  PERFORM credit_private.assert_term(a);
  FOR finance_row IN SELECT value FROM jsonb_array_elements(p->'financial_logs') LOOP
    IF finance_row->>'account_uid' IS DISTINCT FROM customer OR jsonb_typeof(finance_row->'amount') IS DISTINCT FROM 'number' OR (finance_row->>'amount')::numeric<=0 THEN
      RAISE SQLSTATE 'PT422' USING MESSAGE='credit_agreement_finance_mismatch';
    END IF;
  END LOOP;
  IF a->>'revision_uid'='weekly-default-v1' THEN
    IF a->>'term_kind'<>'NET_DAYS' OR a->>'term_value'<>'7' THEN RAISE SQLSTATE 'PT422' USING MESSAGE='credit_agreement_invalid'; END IF;
  ELSE
    SELECT payload INTO revision FROM credit_private.records WHERE shop_id=p_shop AND kind='TERM_REVISION' AND uid=a->>'revision_uid';
    IF NOT FOUND OR revision->>'customer_uid' IS DISTINCT FROM customer OR revision->'term_kind' IS DISTINCT FROM a->'term_kind'
      OR revision->'term_value' IS DISTINCT FROM a->'term_value' THEN RAISE SQLSTATE 'PT422' USING MESSAGE='credit_agreement_revision_missing'; END IF;
  END IF;
END $$;

REVOKE ALL ON ALL FUNCTIONS IN SCHEMA credit_private FROM PUBLIC,anon,authenticated;
REVOKE ALL ON FUNCTION public.credit_contract_status_v1(uuid), public.credit_contract_enable_v1(uuid,boolean),
  public.credit_metadata_push_v1(uuid,jsonb),public.credit_metadata_pull_v1(uuid,bigint,int) FROM PUBLIC,anon,authenticated;
GRANT EXECUTE ON FUNCTION public.credit_contract_status_v1(uuid), public.credit_contract_enable_v1(uuid,boolean),
  public.credit_metadata_push_v1(uuid,jsonb),public.credit_metadata_pull_v1(uuid,bigint,int) TO authenticated;

-- Preserve the reviewed global implementation; add one preflight after actor/shop validation and row lookup.
DO $patch$
DECLARE body text; needle text; replacement text;
BEGIN
  body:=pg_get_functiondef('public.sync_push(jsonb,text,uuid,bigint)'::regprocedure);
  IF body LIKE '%credit_private.validate_sale%' THEN RAISE EXCEPTION 'Credit patch is already installed'; END IF;
  needle:=E'    if found then\n      -- INVARIANT 4';
  replacement:=E'    if v_table = ''events'' then\n      perform credit_private.validate_sale(v_shop, v_uid, v_payload, case when found then v_existing.payload else null end);\n    end if;\n\n    if v_existing.server_seq is not null then\n      -- INVARIANT 4';
  IF position(needle IN body)=0 THEN RAISE EXCEPTION 'sync_push source changed; re-review the patch point'; END IF;
  body:=replace(body,needle,replacement);
  needle:=E'\nend\n$function$';
  IF position(needle IN body)=0 THEN RAISE EXCEPTION 'sync_push exception boundary changed'; END IF;
  body:=replace(body,needle,E'\nexception when sqlstate ''PT422'' then\n  return jsonb_build_object(''ok'',false,''error'',jsonb_build_object(''code'',SQLERRM));\nend\n$function$');
  EXECUTE body;
END $patch$;
