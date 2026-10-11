"""Verify DEV deployment evidence; preserve earlier read-only audit snapshots."""
from pathlib import Path
import hashlib
import json
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[3]
BASE = Path(__file__).resolve().parent
OUT = BASE / 'evidence'
def load(name): return json.loads((OUT / name).read_text(encoding='utf-8'))
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def rows(name):
    r = load(name)
    assert not r.get('isError'), name
    envelope = json.loads(r['content'][0]['text'])['result']
    match = re.search(r'(?:^|\n)<untrusted-data-[^>]+>\s*\n([\s\S]*?)\n</untrusted-data-', envelope)
    assert match, name
    return json.loads(match[1])
def tokens(sql):
    pattern = r"'(?:[^']|'')*'|\"(?:[^\"]|\"\")*\"|--[^\n]*|/\*[\s\S]*?\*/|[A-Za-z_][\w$]*|\d+(?:\.\d+)?|[^\s]"
    return [t if t[0] in "'\"" else t.lower() for t in re.findall(pattern, sql.replace('\r\n','\n'))
            if not t.startswith(('--','/*'))]

pre = load('deploy-dev-preflight-catalog.json')
post = load('deploy-dev-final-catalog.json')
assert len(pre['functions']) == 34 and len(post['functions']) == 36
functions = {f['name']+'('+f['identity_arguments']+')':f for f in post['functions']}
before = {f['name']+'('+f['identity_arguments']+')':f for f in pre['functions']}
expected_added = {'admin_verify_created_identity(p_reconciliation_id uuid)','protect_last_active_owner()'}
assert set(functions)-set(before) == expected_added
allowed_changed = {'admin_record_auth_reconciliation(p_business_id uuid, p_operation text, p_auth_user_id uuid, p_target_username text, p_performed_by uuid, p_performed_by_username text, p_error_code text, p_correlation_id uuid)', 'redact_json_secrets(p_val jsonb)'}
changed = {key for key in before if tokens(before[key]['definition']) != tokens(functions[key]['definition'])}
assert changed == allowed_changed, changed
for key in expected_added:
    assert not functions[key]['anon_execute'] and not functions[key]['authenticated_execute']
assert functions['admin_verify_created_identity(p_reconciliation_id uuid)']['service_execute']
assert next(t for t in post['triggers'] if t['name']=='staff_last_owner_guard')['enabled']=='O'
assert any(c['table']=='sync_records' and c['name']=='sync_records_payload_check' for c in post['constraints'])
for role in ['anon','authenticated','service_role']:
    grant = next(g for g in post['table_privileges'] if g['table']=='sync_records' and g['role']==role)
    expected = {'select':role!='anon','insert':role=='service_role','update':role=='service_role','delete':role=='service_role'}
    assert all(grant[k]==v for k,v in expected.items()), grant
    seq = next(g for g in post['sequences'] if g['name']=='sync_records_server_seq_seq' and g['role']==role)
    assert seq['usage']==(role=='service_role') and seq['select']==(role=='service_role')
assert not any(p['tablename']=='sync_records' and p['cmd']=='INSERT' for p in post['policies'])
assert {(c['table'],c['name']):c for c in pre['columns']}=={(c['table'],c['name']):c for c in post['columns']}
assert pre['aggregate_integrity']==post['aggregate_integrity']
assert load('deploy-dev-preflight-integrity.json')==load('deploy-dev-post-integrity.json')
rpc = rows('deploy-dev-rollback-checks-final-result.json')[0]['verification']
assert len(rpc)==5 and all(v is True for v in rpc.values())
negative = load('deploy-dev-insert_plan-result.json')
assert negative['isError'] and '42501' in negative['content'][0]['text'] and 'permission denied for table sync_records' in negative['content'][0]['text']
assert rows('deploy-dev-owner_public_read-result.json')[0]['result']['pull_ok']
assert rows('deploy-dev-no_jwt-result.json')[0]['result']['visible_sync_rows']==0
redacted=rows('deploy-dev-redactor-result.json')[0]['redacted']
assert redacted['password']=='[REDACTED]' and redacted['nested']=={'token':'[REDACTED]','safe':'visible'}
security=json.loads(load('deploy-dev-security-final.json')['content'][0]['text'])['result']['lints']
assert 'function_search_path_mutable' not in {r['name'] for r in security}
old=load('verification-a06-final.json')
assert all(sha(ROOT / name)==digest for name,digest in old['android_source_sha256'].items())

prod_path=OUT/'deploy-prod-post-catalog.json'
if prod_path.exists():
    # PROD can only be classified deployed after successful result and captured evidence.
    prod=load(prod_path.name)
    result=load('deploy-prod-migration-approved-result.json')
    assert not result.get('isError') and json.loads(result['content'][0]['text'])['success']
    assert len(prod['functions'])==36
    assert next(g for g in prod['table_privileges'] if g['table']=='sync_records' and g['role']=='authenticated')['insert'] is False
    prod_status='Applied after explicit PROD authorization; post-deployment evidence captured'
else:
    rejected=load('deploy-prod-migration-result.json')
    assert rejected['isError'] and 'rejected' in rejected['content'][0]['text']
    prod=load('deploy-prod-after-rejection-catalog.json')
    assert len(prod['functions'])==34
    assert next(g for g in prod['table_privileges'] if g['table']=='sync_records' and g['role']=='authenticated')['insert']
    prod_status='Unchanged; automatic approval review rejected exact PROD deployment; explicit approval requested'

reports=[ROOT/'REPAIR_PLAN_2026-10-10.md',ROOT/'FINDINGS_STATUS.md',ROOT/'PROJECT_AUDIT_2026-10-09.md',BASE/'DEPLOYMENT_AND_MANUAL_CHECKS_2026-10-10.md',BASE/'README.md',BASE/'SUPABASE_VERIFICATION_2026-10-10.md']
assert all(p.exists() for p in reports)

# Reconcile the completed probes; failed attempts remain evidence, not passes.
for name in ['a06-benchmark-data-run.txt','a06-benchmark-data-before-yield-fix-run.txt','a06-benchmark-motion-first-run.txt','a06-benchmark-motion-run.txt']:
    assert 'OK (1 test)' in (OUT/name).read_text(encoding='utf-8')
assert 'OK (7 tests)' in (OUT/'a06-benchmark-costing-regressions.txt').read_text(encoding='utf-8')
assert 'BUILD SUCCESSFUL' in (OUT/'a06-benchmark-post-yield-gates.txt').read_text(encoding='utf-8')
data=load('a06-benchmark-native-data-profile.json')
baseline=load('a06-benchmark-native-data-profile-before-yield-fix.json')
for d in [data,baseline]:
    assert d['build']=='benchmark' and d['device']=='SM-A065F' and d['storage']=='persistent isolated Room fixture'
    for key,n in [('yield_context_40_batches',3),('collection_command_and_receipt',20),('sale_command_and_receipt',20),('month_reports',20)]:
        assert d[key]['samples']==len(d[key]['values_ms'])==n
assert data['yield_context_40_batches']['median_ms'] < baseline['yield_context_40_batches']['median_ms']
motion=load('a06-benchmark-native-motion-profile.json')
assert motion['transitions']==30 and motion['frame_total']['samples']==len(motion['frame_total']['values_ms'])==882
assert motion['frames_over_deadline']==25
assert '"App Deadline Missed"' in (OUT/'a06-benchmark-trace-analysis-full-compilation-corrected.txt').read_text(encoding='utf-8')
assert (OUT/'a06-benchmark-motion.perfetto-trace').stat().st_size>0
assert not (ROOT/'phase18/app/src/benchmark/java/com/saaf/milkshop/AuditMotionBenchmarkActivity.kt').exists()
host_xml=sorted((ROOT/'phase18/app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'))
assert host_xml
host_counts={k:sum(int(ET.parse(p).getroot().get(k,0)) for p in host_xml) for k in ['tests','failures','errors','skipped']}
assert host_counts=={'tests':93,'failures':0,'errors':0,'skipped':1},host_counts
lint_xml=ROOT/'phase18/app/build/reports/lint-results-benchmark.xml'
lint_counts={s:sum(x.get('severity')==s for x in ET.parse(lint_xml).getroot().findall('issue')) for s in ['Error','Fatal','Warning']}
assert lint_counts=={'Error':0,'Fatal':0,'Warning':235},lint_counts
support=[Path(__file__),BASE/'DEV_DEPLOYED_ROLLBACK_CHECKS.sql',BASE/'prepare_release_profile.py',BASE/'run_a06_benchmark.py',BASE/'a06-benchmark-trace.pbtxt',BASE/'a06-benchmark-trace-analysis.sql',ROOT/'phase18/app/src/androidTest/java/com/saaf/milkshop/NativeReleaseProfileProbe.kt',ROOT/'phase18/app/src/benchmark/AndroidManifest.xml',ROOT/'phase18/app/src/main/java/com/saaf/milkshop/domain/Costing.kt',ROOT/'phase18/app/src/main/java/com/saaf/milkshop/application/owner/NativeOwnerCostingPlansFacade.kt',ROOT/'phase18/app/src/androidTest/java/com/saaf/milkshop/Phase3CostingTest.kt']
evidence=sorted(p for p in OUT.glob('deploy-*') if p.is_file() and p.name!='deploy-evidence-verification.txt')
profile=sorted(p for p in OUT.glob('a06-benchmark-*') if p.is_file())
manifest={'date':'2026-10-10','dev':'saafmilk-dev / tghzmofkkdsaasvnixps: applied and verified',
    'prod':prod_status,'dev_deployed_db_behavior':rpc,'real_auth_session_verified':False,
    'dev_fixture_rows':'Rolled back; sequence allocations remain consumed, no sequence reset',
    'dev_integrity':load('deploy-dev-post-integrity.json'),'preserved_prior_android_source_hashes':81,
    'expected_changed_existing_public_function_bodies':sorted(changed),
    'a01':'MEDIUM repeated effective-history reads; locally repaired; Android release pending',
    'a01_yield_context_before':baseline['yield_context_40_batches'],
    'a01_yield_context_after':data['yield_context_40_batches'],
    'host_counts':host_counts,'benchmark_lint_counts':lint_counts,'costing_device_cases_passed':7,
    'motion':'Instrumented traces collected; frame concern open; production smoothness not certified; further profiling deferred',
    'optional_direct_activity':'Compiled only; never executed; unused source removed',
    'gate_xml_sha256':{str(p.relative_to(ROOT)):sha(p) for p in [*host_xml,lint_xml]},
    'proposal_sha256':sha(BASE/'SUPABASE_LIVE_REPAIR_2026-10-10.sql'),
    'older_snapshot_sha256':{n:sha(OUT/n) for n in ['verification-final.json','verification-a06-final.json','verification-supabase-final.json']},
    'report_sha256':{str(p.relative_to(ROOT)):sha(p) for p in reports},
    'support_sha256':{str(p.relative_to(ROOT)):sha(p) for p in support},
    'deployment_evidence_sha256':{p.name:sha(p) for p in evidence},
    'benchmark_evidence_sha256':{p.name:sha(p) for p in profile}}
(OUT/'verification-deployment-final.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
print(json.dumps({'dev':'Applied and verified','prod':prod_status,'dev_db_behavior_checks':rpc,'real_auth_session_verified':False,'preserved_android_sources':81,'deployment_artifacts':len(evidence),'benchmark_artifacts':len(profile)},indent=2))
print('DEPLOYMENT_EVIDENCE_RECONCILIATION: PASS')
