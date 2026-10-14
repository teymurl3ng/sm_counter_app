from pathlib import Path
import re, hashlib, json
folder=Path(__file__).resolve().parent
root=folder.parents[2]
source=root/'audit/2026-10-13/protected-reset-integration/server/supabase/migrations/20261012194632_protected_credential_reset_guard.sql'
assert hashlib.sha256(source.read_bytes()).hexdigest()=='282876836d1981a934c7a5e0ff31fe1be58c3f2ba64751d39b87636e4a08d204'
expected={name:hashlib.md5(body.encode()).hexdigest() for name,body in re.findall(r'CREATE FUNCTION\s+([\w.]+).*?\bAS \$\$(.*?)\$\$;',source.read_text(encoding='utf-8'),re.S|re.I)}
assert len(expected)==8
(folder/'guard-expected.json').write_text(json.dumps(expected,indent=2)+'\n',encoding='utf-8')
query="""select json_build_object(
'functions',(select json_agg(json_build_object('name',n.nspname||'.'||p.proname,'args',pg_get_function_identity_arguments(p.oid),'body_md5',md5(replace(p.prosrc,chr(13)||chr(10),chr(10))),'security_definer',p.prosecdef,'config',p.proconfig,'anon_exec',has_function_privilege('anon',p.oid,'EXECUTE'),'authenticated_exec',has_function_privilege('authenticated',p.oid,'EXECUTE'),'service_exec',has_function_privilege('service_role',p.oid,'EXECUTE')) order by n.nspname,p.proname) from pg_proc p join pg_namespace n on n.oid=p.pronamespace where n.nspname||'.'||p.proname in (NAMES)),
'schema',(select json_build_object('anon_usage',has_schema_privilege('anon',oid,'USAGE'),'authenticated_usage',has_schema_privilege('authenticated',oid,'USAGE'),'service_usage',has_schema_privilege('service_role',oid,'USAGE')) from pg_namespace where nspname='credential_reset_private'),
'tables',(select json_agg(json_build_object('name',c.relname,'rls',c.relrowsecurity,'anon_select',has_table_privilege('anon',c.oid,'SELECT'),'authenticated_select',has_table_privilege('authenticated',c.oid,'SELECT'),'service_select',has_table_privilege('service_role',c.oid,'SELECT'),'constraints',(select json_agg(pg_get_constraintdef(co.oid) order by co.conname) from pg_constraint co where co.conrelid=c.oid)) order by c.relname) from pg_class c join pg_namespace n on n.oid=c.relnamespace where n.nspname='credential_reset_private' and c.relkind='r'),
'trigger',(select json_build_object('enabled',t.tgenabled,'definition',pg_get_triggerdef(t.oid),'function',n.nspname||'.'||p.proname) from pg_trigger t join pg_proc p on p.oid=t.tgfoid join pg_namespace n on n.oid=p.pronamespace where t.tgrelid='public.staff'::regclass and t.tgname='staff_credential_reset_guard')
) as guard_catalog;""".replace('NAMES',','.join("'"+name+"'" for name in expected))
(folder/'guard-catalog.sql').write_text(query,encoding='utf-8')
print(json.dumps(expected))
