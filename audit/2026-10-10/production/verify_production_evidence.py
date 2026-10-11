"""Verify this PROD deployment snapshot without contacting or mutating a server."""
from pathlib import Path
import json
import hashlib
import re
from datetime import datetime, timezone

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]

def load(name):
    return json.loads((HERE / name).read_text(encoding='utf-8'))

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def envelope(raw):
    assert not raw.get('isError'), raw
    return json.loads(raw['content'][0]['text'])

def rows(raw):
    text = envelope(raw)['result']
    match = re.search(r'(?:^|\n)<untrusted-data-[^>]+>\s*\n([\s\S]*?)\n</untrusted-data-', text)
    assert match
    return json.loads(match[1])

before, after = load('prod-preflight.json'), load('prod-postflight.json')
assert rows(load('prod-postflight-raw.json'))[0]['state'] == after
assert envelope(load('prod-migration-result.json'))['success'] is True
projects = envelope(load('project-identities.json'))['projects']
assert any(p['id'] == 'tlsuxbghuhlqntkgtrxt' and p['name'] == 'saafmilk-prod' for p in projects)
mb = envelope(load('prod-migrations-before.json'))['migrations']
ma = envelope(load('prod-migrations-after.json'))['migrations']
assert ma[:-1] == mb
assert ma[-1] == {'version': '20261010141548', 'name': 'saaf_c1_6_sync_boundary'}
sql = ROOT / 'audit/2026-10-09/repairs/SUPABASE_LIVE_REPAIR_2026-10-10.sql'
assert sha(sql) == '8856aa3b069eb7a6badd165d97003c373c43963f8f7004950911129e8e5b8308'

key = lambda f: f['name'] + '(' + f['args'] + ')'
b = {key(f): f for f in before['functions']}
a = {key(f): f for f in after['functions']}
assert len(b) == 34 and len(a) == 36
assert set(a) - set(b) == {'protect_last_active_owner()', 'admin_verify_created_identity(p_reconciliation_id uuid)'}
changed = {k for k in b if b[k]['definition_md5'] != a[k]['definition_md5']}
assert changed == {
    'redact_json_secrets(p_val jsonb)',
    'admin_record_auth_reconciliation(p_business_id uuid, p_operation text, p_auth_user_id uuid, p_target_username text, p_performed_by uuid, p_performed_by_username text, p_error_code text, p_correlation_id uuid)',
}
for field in ['counts', 'sync', 'business_data_md5', 'shop_data_md5', 'staff_data_md5', 'integrity']:
    assert before[field] == after[field], field
assert after['sync']['rows'] == 36 and after['sync']['max_seq'] == 72
assert all(after['sync'][k] == 0 for k in ['non_objects', 'unknown_tables', 'mismatched_shops'])
assert all(n == 0 for n in after['integrity'].values())
for p in after['privileges']:
    service = p['role'] == 'service_role'
    assert p['select'] == (p['role'] != 'anon')
    assert all(p[k] == service for k in ['insert', 'update', 'delete', 'sequence_usage', 'sequence_select', 'sequence_update'])
assert any(t['name'] == 'staff_last_owner_guard' and t['enabled'] == 'O' for t in after['triggers'])
assert any(c['name'] == 'sync_records_payload_check' and c['validated'] for c in after['constraints'])
assert not any(p['cmd'] in ('INSERT', 'ALL') for p in after['policies'])
for name in ['protect_last_active_owner', 'admin_verify_created_identity']:
    f = next(f for f in after['functions'] if f['name'] == name)
    assert not f['anon_execute'] and not f['authenticated_execute']
assert next(f for f in after['functions'] if f['name'] == 'admin_verify_created_identity')['service_execute']

definitions = lambda filename: {key(f): f['definition'].replace('\r\n', '\n') for f in load(filename)}
assert definitions('prod-migration-functions.json') == definitions('dev-migration-functions.json')
prod_helper = rows(load('timestamp-helper-prod.json'))[0]['definition']
dev_helper = rows(load('timestamp-helper-dev.json'))[0]['definition']
assert prod_helper.replace('\r\n', '\n') == dev_helper.replace('\r\n', '\n')
reads = rows(load('prod-read-verification-result.json'))[0]['verification']
assert reads['pulled_records'] == 36
assert all(v is True for k, v in reads.items() if k != 'pulled_records')
advisors = envelope(load('prod-security-after.json'))['result']['lints']
assert not any(a['name'] == 'function_search_path_mutable' for a in advisors)

manifest = {
    'verified_at': datetime.now(timezone.utc).isoformat(),
    'project': 'saafmilk-prod / tlsuxbghuhlqntkgtrxt',
    'migration': ma[-1],
    'migration_sha256': sha(sql),
    's01': {'severity': 'HIGH', 'status': 'repaired_and_verified_dev_and_prod'},
    'production_data_digests_and_counts_preserved': True,
    'prod_counts': after['counts'],
    'sync_records': 36,
    'migration_function_definitions_match_dev': True,
    'sql_role_read_and_permission_checks': reads,
    'production_auth_login_or_client_session_tested': False,
    'production_test_history_inserted': False,
    'android_or_viewer_published_by_this_step': False,
    'remaining_advisors': [{'name': x['name'], 'level': x['level'], 'count': x['count']} for x in advisors],
    'artifact_sha256': {p.name: sha(p) for p in HERE.iterdir() if p.is_file() and p.name not in ['verification-production-final.json']},
    'report_sha256': {str(p.relative_to(ROOT)): sha(p) for p in [ROOT / 'REPAIR_PLAN_2026-10-10.md', ROOT / 'PROJECT_AUDIT_2026-10-09.md', ROOT / 'audit/2026-10-09/repairs/DEPLOYMENT_AND_MANUAL_CHECKS_2026-10-10.md']},
}
(HERE / 'verification-production-final.json').write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
print(json.dumps({k: v for k, v in manifest.items() if not k.endswith('_sha256')}, indent=2))
