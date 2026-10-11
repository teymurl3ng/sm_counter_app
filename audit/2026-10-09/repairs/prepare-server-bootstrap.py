from pathlib import Path
root=Path('C:/Users/Teymur/Desktop/saaf-anchor-tmp')
patch=['*** Begin Patch']
def add(name,value):
    patch.append('*** Add File: '+(root/name).as_posix())
    patch.extend('+'+line for line in value.splitlines())
schema='''-- Fresh-environment sync schema. Existing deployments require schema parity review.
-- Derived from anchored C1 RPCs and SYNC_GLOBAL_SEQ_C1_MIGRATION.md; not a live schema dump.
BEGIN;
CREATE TABLE public.sync_records (
  server_seq bigint PRIMARY KEY,
  business_id uuid NOT NULL REFERENCES public.businesses(id),
  shop_id uuid NOT NULL REFERENCES public.shops(id),
  table_name text NOT NULL,
  row_uid text NOT NULL,
  payload jsonb NOT NULL CHECK (jsonb_typeof(payload) = 'object'),
  origin_device text,
  received_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT sync_records_shop_table_uid UNIQUE (shop_id, table_name, row_uid)
);
CREATE INDEX sync_records_shop_seq_idx ON public.sync_records(shop_id, server_seq);
CREATE SEQUENCE public.sync_records_server_seq_seq AS bigint OWNED BY public.sync_records.server_seq;
'''
import re
helpers=re.sub(r'(?m)^\$function\$$', '$function$;', (root/'sync-rpc-anchor/sync_helpers.sql').read_text(encoding='utf-8'))
schema+=helpers+'\n'
schema+='''CREATE FUNCTION public.sync_records_assign_seq()
RETURNS trigger LANGUAGE plpgsql SET search_path = '' AS $$
BEGIN
  IF TG_OP = 'INSERT' OR public.sync_normalize_payload(NEW.payload)
       IS DISTINCT FROM public.sync_normalize_payload(OLD.payload) THEN
    NEW.server_seq := nextval('public.sync_records_server_seq_seq');
  END IF;
  RETURN NEW;
END; $$;
CREATE TRIGGER sync_records_assign_seq BEFORE INSERT OR UPDATE ON public.sync_records
FOR EACH ROW EXECUTE FUNCTION public.sync_records_assign_seq();
ALTER TABLE public.sync_records ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.sync_records FROM PUBLIC, anon, authenticated;
GRANT SELECT ON public.sync_records TO authenticated;
GRANT ALL ON public.sync_records TO service_role;
GRANT USAGE, SELECT ON public.sync_records_server_seq_seq TO service_role;
CREATE POLICY sync_records_authorized_read ON public.sync_records FOR SELECT TO authenticated
USING (shop_id IN (SELECT public.sync_staff_shop_ids()));
'''
for file in ('sync_push.sql','sync_pull.sql'):
    schema+=(root/'sync-rpc-anchor'/file).read_text(encoding='utf-8').rstrip()+';\n'
schema+='''REVOKE ALL ON FUNCTION public.sync_staff_shop_ids() FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.sync_push(jsonb,text,uuid,bigint) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.sync_pull(bigint,integer,uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.sync_staff_shop_ids() TO authenticated, service_role;
GRANT EXECUTE ON FUNCTION public.sync_push(jsonb,text,uuid,bigint) TO authenticated;
GRANT EXECUTE ON FUNCTION public.sync_pull(bigint,integer,uuid) TO authenticated;
COMMIT;
'''
add('migration_sync_fresh.sql',schema)
add('bootstrap.sql','''-- psql -v ON_ERROR_STOP=1 -f bootstrap.sql (fresh Supabase environment only)
-- Supabase supplies auth schema, roles and auth.uid()/auth.role().
\\set ON_ERROR_STOP on
\\ir migration.sql
\\ir migration_c0_admin_read.sql
\\ir migration_c1_admin_write.sql
\\ir migration_c1_1_admin_write_rpcs.sql
\\ir migration_c1_2_admin_auth_db_helpers.sql
\\ir migration_c1_3_hardening.sql
\\ir migration_c1_4_reconciliation.sql
\\ir migration_c1_5_error_semantics.sql
\\ir migration_c1_6_integrity.sql
\\ir migration_sync_fresh.sql
''')
add('BOOTSTRAP_AND_REPAIR.md','''# Local repair and bootstrap status

`bootstrap.sql` orders identity, admin, reconciliation and sync definitions for a **fresh Supabase database**. It fails if sync tables already exist rather than guessing how to migrate live data. The sync DDL is derived from anchored RPC source and the client C1 specification, not a production schema dump.

Existing DEV/PROD: review and apply only `migration_c1_6_integrity.sql` after C1.5. Viewer create verification requires this migration first. Never run fresh bootstrap over an existing deployment. The timestamped C1.6 migration has identical content for Supabase migration users.

The database owner trigger covers CLI/service-role writes as well as Viewer RPCs. Concurrent competing removals serialize by business; mixed lock order with older RPCs can abort one transaction as a deadlock, preserving the owner. Retry a failed transaction after reloading staff state.

Create verification never deletes Auth users. A linked identity resolves automatically; an unlinked ambiguous identity stays pending for owner inspection. Reset its credential after recovering a committed account, because the original response did not disclose the credential.

DR exports paginate REST and Auth independently, record counts and detect duplicate row IDs. Their manifest explicitly identifies sequential, nontransactional consistency. Identity mutations during export still require a maintenance window or transaction-level backup for exact point-in-time recovery.

No migration, workflow or CLI action in these repairs has been run against DEV/PROD. Bootstrap execution and database concurrency need a local PostgreSQL/Supabase rehearsal before deployment. No local psql/docker runtime was available when prepared.
''')
patch.append('*** End Patch')
Path(__file__).with_name('server-bootstrap.patch.txt').write_text('\n'.join(patch)+'\n',encoding='utf-8')
