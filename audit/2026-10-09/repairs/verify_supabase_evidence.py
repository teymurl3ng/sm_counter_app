"""Reconcile captured evidence without running SQL or rewriting older snapshots."""
from pathlib import Path
import hashlib
import json
import re

ROOT = Path(__file__).resolve().parents[3]
REPAIRS = Path(__file__).resolve().parent
OUT = REPAIRS / 'evidence'
SERVER = Path('C:/Users/Teymur/Desktop/saaf-anchor-tmp')

def digest(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def load(name): return json.loads((OUT / name).read_text(encoding='utf-8'))
def result(name):
    raw = load(name)
    assert not raw.get('isError'), name
    envelope = json.loads(raw['content'][0]['text'])
    match = re.search(r'(?:^|\n)<untrusted-data-[^>]+>\s*\n([\s\S]*?)\n</untrusted-data-', envelope['result'])
    assert match, name
    return json.loads(match[1])

old = load('verification-a06-final.json')
assert len(old['android_source_sha256']) == 81
assert all(digest(ROOT / name) == sha for name, sha in old['android_source_sha256'].items())
assert all(digest(OUT / name) == sha for name, sha in old['a06_evidence_sha256'].items())
comparison = load('supabase-parity-comparison.json')
assert comparison['dev_prod_function_count'] == 34
assert comparison['dev_prod_function_definitions_lexically_equal_after_documented_normalization']
assert not comparison['dev_prod_schema_policy_privilege_trigger_differences']
assert comparison['fresh_columns_and_profile_now_match_live']

native = {}
for filename in ['supabase-bootstrap-parity-final.txt', 'supabase-native-boundary-repair.txt']:
    log = (OUT / filename).read_text(encoding='utf-8-sig')
    checks = re.findall(r'^PASS (.+)$', log, re.M)
    assert len(checks) == 11, (filename, checks)
    assert 'Disposable cluster stopped' in log
    assert not re.search(r'^FAIL', log, re.M)
    native[filename] = {'checks': checks, 'passed': 11, 'scope': 'disposable PostgreSQL 18.6; local Auth stand-ins'}

projects = {}
for env, project, rows, shops in [('dev', 'tghzmofkkdsaasvnixps', 39, 3), ('prod', 'tlsuxbghuhlqntkgtrxt', 36, 1)]:
    prefix = 'supabase-' + env + '-'
    integrity = result(prefix + 'sync-integrity-2026-10-10.json')[0]['integrity']
    assert integrity == {'sync_rows': rows, 'non_object_payloads': 0, 'unknown_sync_tables': 0, 'business_shop_mismatches': 0}
    empty = result(prefix + 'no_jwt-2026-10-10.json')[0]['result']
    assert empty['auth_uid_is_null'] and empty['eligible_shops'] == 0
    assert empty['visible_sync_rows'] == 0 and empty['pull_records'] == 0 and empty['pull_ok']
    owner = result(prefix + 'owner_public_read-2026-10-10.json')[0]['result']
    assert owner['eligible_shops'] == shops and owner['profile_role'] == 'owner'
    assert owner['profile_nonnull'] and owner['overview_nonnull'] and owner['pull_ok'] and owner['pull_records'] == 1
    plan = result(prefix + 'insert_plan-2026-10-10.json')[0]['QUERY PLAN'][0]['Plan']
    assert plan['Operation'] == 'Insert' and plan['Relation Name'] == 'sync_records'
    for probe, helper in [('anon_boundary', 'sync_staff_shop_ids'), ('owner_read', 'admin_require_write_caller')]:
        raw = load(prefix + probe + '-2026-10-10.json')
        assert raw.get('isError')
        error = json.loads(raw['content'][0]['text'])['error']['message']
        assert '42501' in error and 'permission denied for function ' + helper in error
    catalog = load(prefix + 'catalog-2026-10-10.json')
    assert len(catalog['functions']) == 34 and all(t['rls'] for t in catalog['tables'])
    projects[env] = {'name': 'saafmilk-' + env, 'project_id': project,
                     'aggregate_snapshot': integrity, 'owner_public_sql_context': owner,
                     'no_identity_sql_context': empty, 'direct_insert_readonly_plan_accepted': True,
                     'anon_boundary': 'Expected private-helper ACL denial: 42501',
                     'initial_owner_read': 'Invalid private-helper probe; intended ACL denial, not product defect',
                     'real_auth_session_tested': False}

proposal = REPAIRS / 'SUPABASE_LIVE_REPAIR_2026-10-10.sql'
c1 = (SERVER / 'migration_c1_6_integrity.sql').read_text(encoding='utf-8')
c1_body = re.sub(r'^\s*(?:BEGIN|COMMIT);\s*$', '', c1, flags=re.M).strip()
assert c1_body in proposal.read_text(encoding='utf-8'), 'Proposal must contain the exact prepared C1.6 body'
diff_checks = load('supabase-final-diffchecks.json')
assert len(diff_checks) == 3 and all(r['status'] == 'fulfilled' and r['value']['exit_code'] == 0 for r in diff_checks)
tracker = (ROOT / 'REPAIR_PLAN_2026-10-10.md').read_text(encoding='utf-8')
assert set(range(1, 34)).issubset({int(n) for n in re.findall(r'\bF(\d{2})\b', tracker)})
assert 'S01 — HIGH' in tracker

reports = ['PROJECT_AUDIT_2026-10-09.md', 'REPAIR_PLAN_2026-10-10.md', 'FINDINGS_STATUS.md'] + [
    'audit/2026-10-09/repairs/' + name for name in ['A06_VERIFICATION_2026-10-10.md',
    'SUPABASE_VERIFICATION_2026-10-10.md', 'README.md', 'sql-check/README.md']]
server_sources = ['migration.sql', 'migration_c1_3_hardening.sql', 'migration_c1_6_integrity.sql',
                  'migration_sync_fresh.sql', 'bootstrap.sql', 'CONTRACT.md']
support = ['compare_supabase_catalogs.py', 'verify_supabase_evidence.py', 'prepare_bootstrap_parity.py',
           'sql-check/check.mjs', 'sql-check/live-boundary.mjs', 'sql-check/native-run.py', proposal.name]
artifacts = sorted(p for p in OUT.glob('supabase-*') if p.is_file()
                   and p.name != 'supabase-evidence-verification.txt')
manifest = {'date': '2026-10-10', 'operation_scope': 'Live read-only MCP; local source repairs and disposable SQL execution',
    'live_mutations': False, 'deployment': 'Not performed; exact DEV proposal awaits review',
    'new_finding': {'id': 'S01', 'severity': 'HIGH', 'status': 'Verified live control gap; locally reproduced/repaired; not deployed'},
    'projects': projects, 'comparison': comparison, 'native_batches': native,
    'test_counts_are_not_unique_coverage': True, 'android_source_hashes_unchanged': 81,
    'prior_a06_evidence_hashes_still_match': True,
    'remaining': ['DEV/PROD deployment', 'Real Auth/session and deployed reconciliation rehearsal',
                  'Owner visual acceptance', 'Release motion tracing', 'Persistent/costing profiling',
                  'Owner-approved historical partial-event recovery', 'Power-loss durability if required'],
    'proposal_sha256': digest(proposal),
    'older_snapshots_preserved': {n: digest(OUT / n) for n in ['verification-final.json', 'verification-a06-final.json']},
    'older_report_hashes': 'Dated historical snapshots; current reports intentionally updated for Supabase continuation',
    'server_source_sha256': {str(SERVER / n): digest(SERVER / n) for n in server_sources},
    'support_sha256': {str(p.relative_to(ROOT)): digest(p) for p in [REPAIRS / n for n in support]},
    'report_sha256': {n: digest(ROOT / n) for n in reports},
    'supabase_evidence_sha256': {p.name: digest(p) for p in artifacts},
    'verification_log_excluded': 'supabase-evidence-verification.txt is generated by this verifier'}
path = OUT / 'verification-supabase-final.json'
path.write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
saved = load(path.name)
for key, base in [('server_source_sha256', None), ('support_sha256', ROOT), ('report_sha256', ROOT), ('supabase_evidence_sha256', OUT)]:
    assert all(digest((base / name) if base else Path(name)) == sha for name, sha in saved[key].items())
print(json.dumps({'projects': {env: p['aggregate_snapshot'] for env,p in projects.items()},
                  'native_passes_by_batch': {name: r['passed'] for name,r in native.items()},
                  'unchanged_android_sources': 81, 'supabase_evidence_hashes': len(artifacts),
                  'deployment_performed': False, 'real_auth_session_verified': False}, indent=2))
print('SUPABASE_EVIDENCE_RECONCILIATION: PASS')
