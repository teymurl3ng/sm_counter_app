-- Reviewable live repair, apply only after approving the exact DEV deployment.
-- Adds the already-prepared C1.6 integrity repair and closes direct sync writes.
-- No Auth user deletion, blanket grants, historical row rewrite or sequence reset.
BEGIN;
-- C1.6: preserve ambiguous account creation; protect the final owner in every write path.
-- Apply after C1.5. No Auth identity deletion is authorized by this migration.

CREATE OR REPLACE FUNCTION public.admin_record_auth_reconciliation(
  p_business_id uuid,
  p_operation text,
  p_auth_user_id uuid,
  p_target_username text,
  p_performed_by uuid,
  p_performed_by_username text,
  p_error_code text,
  p_correlation_id uuid
)
RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
  v_id uuid;
BEGIN
  IF auth.role() IS DISTINCT FROM 'service_role' THEN
    RAISE EXCEPTION 'service role required' USING errcode = '42501';
  END IF;
  IF p_operation NOT IN ('create_cleanup', 'create_verify', 'terminate_cleanup', 'reset_audit') THEN
    RAISE EXCEPTION 'invalid reconciliation operation' USING errcode = 'P0011';
  END IF;
  IF p_error_code NOT IN (
    'ALREADY_EXISTS', 'FORBIDDEN', 'INVALID_INPUT', 'NOT_FOUND',
    'RATE_LIMITED', 'SERVER_ERROR', 'UNREACHABLE', 'UNKNOWN'
  ) THEN
    RAISE EXCEPTION 'invalid reconciliation error code' USING errcode = 'P0012';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM public.staff
    WHERE id = p_performed_by
      AND business_id = p_business_id
      AND username = p_performed_by_username
  ) THEN
    RAISE EXCEPTION 'invalid reconciliation performer' USING errcode = '42501';
  END IF;

  INSERT INTO public.auth_reconciliation (
    business_id, operation, auth_user_id, target_username, state,
    attempt_count, error_code, correlation_id, performed_by,
    performed_by_username, created_at, updated_at
  ) VALUES (
    p_business_id, p_operation, p_auth_user_id, lower(btrim(p_target_username)),
    'pending', 1, p_error_code, p_correlation_id, p_performed_by,
    p_performed_by_username, now(), now()
  )
  ON CONFLICT (operation, auth_user_id) WHERE state = 'pending'
  DO UPDATE SET
    attempt_count = public.auth_reconciliation.attempt_count + 1,
    error_code = EXCLUDED.error_code,
    correlation_id = EXCLUDED.correlation_id,
    updated_at = now(),
    last_attempt_at = now()
  RETURNING id INTO v_id;

  RETURN v_id;
END;
$$;


CREATE OR REPLACE FUNCTION public.admin_verify_created_identity(p_reconciliation_id uuid)
RETURNS boolean LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE pending record;
BEGIN
  IF auth.role() IS DISTINCT FROM 'service_role' THEN
    RAISE EXCEPTION 'service role required' USING errcode = '42501';
  END IF;
  SELECT * INTO pending FROM public.auth_reconciliation
  WHERE id = p_reconciliation_id AND operation IN ('create_verify', 'create_cleanup')
    AND state = 'pending' FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'reconciliation record not found' USING errcode = 'P0002'; END IF;
  IF EXISTS (SELECT 1 FROM public.staff
    WHERE auth_user_id = pending.auth_user_id AND business_id = pending.business_id
      AND username = pending.target_username) THEN
    UPDATE public.auth_reconciliation SET state = 'resolved', resolved = true,
      error_code = NULL, last_attempt_at = now(), resolved_at = now(), updated_at = now(),
      attempt_count = attempt_count + 1 WHERE id = pending.id;
    RETURN true;
  END IF;
  UPDATE public.auth_reconciliation SET last_attempt_at = now(), updated_at = now(),
    attempt_count = attempt_count + 1 WHERE id = pending.id;
  RETURN false;
END; $$;
REVOKE ALL ON FUNCTION public.admin_verify_created_identity(uuid) FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.admin_verify_created_identity(uuid) TO service_role;

CREATE OR REPLACE FUNCTION public.protect_last_active_owner()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
BEGIN
  IF OLD.role <> 'owner' OR NOT OLD.active OR OLD.terminated_at IS NOT NULL THEN
    IF TG_OP = 'DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
  END IF;
  IF TG_OP = 'UPDATE' THEN
    IF NEW.business_id = OLD.business_id AND NEW.role = 'owner'
      AND NEW.active AND NEW.terminated_at IS NULL THEN RETURN NEW; END IF;
  END IF;
  -- Version the business row as well as locking it. A competing removal
  -- under REPEATABLE READ must serialize/retry, not count its old snapshot.
  UPDATE public.businesses SET name = name WHERE id = OLD.business_id;
  IF NOT EXISTS (SELECT 1 FROM public.staff WHERE business_id = OLD.business_id
      AND id <> OLD.id AND role = 'owner' AND active AND terminated_at IS NULL) THEN
    RAISE EXCEPTION 'cannot remove the last active owner' USING errcode = 'P0001';
  END IF;
  IF TG_OP = 'DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END; $$;
REVOKE ALL ON FUNCTION public.protect_last_active_owner() FROM PUBLIC, anon, authenticated;
DROP TRIGGER IF EXISTS staff_last_owner_guard ON public.staff;
CREATE TRIGGER staff_last_owner_guard BEFORE UPDATE OR DELETE ON public.staff
FOR EACH ROW EXECUTE FUNCTION public.protect_last_active_owner();


-- All ordinary sync writes must use the existing SECURITY DEFINER sync_push RPC.
-- Keep authenticated SELECT for invoker sync_pull; leave service_role unchanged.
REVOKE ALL ON TABLE public.sync_records FROM PUBLIC, anon, authenticated;
GRANT SELECT ON TABLE public.sync_records TO authenticated;
REVOKE ALL ON SEQUENCE public.sync_records_server_seq_seq FROM PUBLIC, anon, authenticated;
DROP POLICY IF EXISTS sync_records_staff_insert ON public.sync_records;

-- Enforce the same payload-object shape already required by sync_push.
-- Validate existing rows; any incompatible history aborts this transaction.
DO $boundary$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint
    WHERE conrelid = 'public.sync_records'::regclass
      AND conname = 'sync_records_payload_check') THEN
    ALTER TABLE public.sync_records ADD CONSTRAINT sync_records_payload_check
      CHECK (jsonb_typeof(payload) = 'object') NOT VALID;
  END IF;
END;
$boundary$;
ALTER TABLE public.sync_records VALIDATE CONSTRAINT sync_records_payload_check;

-- Internal invoker helper: built-ins resolve through pg_catalog; recursive call is qualified.
ALTER FUNCTION public.redact_json_secrets(jsonb) SET search_path = '';
COMMIT;

