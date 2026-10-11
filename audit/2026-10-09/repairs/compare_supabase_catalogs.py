"""Compare captured metadata, preserving strings while ignoring comments/newlines."""
from pathlib import Path
import json
import re

OUT = Path(__file__).resolve().parent / 'evidence'
def load(name): return json.loads((OUT / name).read_text(encoding='utf-8'))
def tokens(sql):
    pattern = r"'(?:[^']|'')*'|\"(?:[^\"]|\"\")*\"|--[^\n]*|/\*[\s\S]*?\*/|[A-Za-z_][\w$]*|\d+(?:\.\d+)?|[^\s]"
    return [t if t[0] in "'\"" else t.lower()
            for t in re.findall(pattern, sql.replace('\r\n', '\n'))
            if not t.startswith(('--', '/*'))]
def fnkey(f): return f['name'] + '(' + f['identity_arguments'] + ')'
dev = load('supabase-dev-catalog-2026-10-10.json')
prod = load('supabase-prod-catalog-2026-10-10.json')
local = load('supabase-local-bootstrap-catalog.json')
df = {fnkey(f): f for f in dev['functions']}
pf = {fnkey(f): f for f in prod['functions']}
lf = {fnkey(f): f for f in local['functions']}
assert set(df) == set(pf)
dev_prod_fn_diffs = [name for name in df if tokens(df[name]['definition']) != tokens(pf[name]['definition'])]
assert dev_prod_fn_diffs == [], dev_prod_fn_diffs
def comparable_catalog(catalog):
    result = {}
    for group in ['columns','constraints','policies','table_privileges','sequences','triggers']:
        # PostgreSQL 18 reports NOT NULL constraints separately; compare column flags instead.
        objects = [o for o in catalog[group] or [] if not (group == 'constraints' and o['type'] == 'n')]
        result[group] = sorted([json.dumps(o, sort_keys=True) for o in objects])
    return result
dev_prod_catalog_diffs = [key for key, value in comparable_catalog(dev).items()
                          if value != comparable_catalog(prod)[key]]
assert not dev_prod_catalog_diffs, dev_prod_catalog_diffs
local_cols = {(c['table'], c['name']): c for c in local['columns']}
live_cols = {(c['table'], c['name']): c for c in dev['columns']}
assert local_cols == live_cols, 'Fresh columns should match the observed live nullable timezone contract'
assert tokens(lf['my_profile()']['definition']) == tokens(df['my_profile()']['definition'])
expected_new = ['admin_verify_created_identity(p_reconciliation_id uuid)', 'protect_last_active_owner()']
assert sorted(set(lf) - set(df)) == sorted(expected_new)
for env, catalog in [('dev',dev),('prod',prod)]:
    assert all(t['rls'] for t in catalog['tables'])
    assert not any(t['name']=='staff_last_owner_guard' for t in catalog['triggers'])
    assert 'create_verify' not in next(f for f in catalog['functions'] if f['name']=='admin_record_auth_reconciliation' and f['result']=='uuid')['definition']
    assert next(g for g in catalog['table_privileges'] if g['table']=='sync_records' and g['role']=='authenticated')['insert']
result = {
    'dev_prod_function_definitions_lexically_equal_after_documented_normalization': True,
    'dev_prod_function_count': len(df),
    'dev_prod_schema_policy_privilege_trigger_differences': dev_prod_catalog_diffs,
    'fresh_columns_and_profile_now_match_live': True,
    'prepared_c1_6_functions_missing_live': expected_new,
    'prepared_function_definition_differences': [name for name in sorted(set(lf)&set(df))
        if tokens(lf[name]['definition']) != tokens(df[name]['definition'])],
    'constraint_differences': {
        'fresh_payload_object_check': 'Prepared fresh schema enforces object payloads; live currently has no such check',
        'sync_foreign_key_deletion': 'Fresh schema uses NO ACTION; live uses ON DELETE CASCADE. Preserve live history/deletion behavior pending an explicit policy decision.'},
    'index_name_difference': 'sync_records_shop_seq_idx (fresh) vs sync_records_shop_seq (live); same indexed columns',
    'method_limit': 'Lexical comparison is not a SQL equivalence proof; PostgreSQL 17/18 catalog formatting is normalized only where documented.'}
(OUT / 'supabase-parity-comparison.json').write_text(json.dumps(result, indent=2)+'\n', encoding='utf-8')
print(json.dumps(result, indent=2))
print('SUPABASE_METADATA_COMPARISON: PASS (identified gaps remain explicit)')
