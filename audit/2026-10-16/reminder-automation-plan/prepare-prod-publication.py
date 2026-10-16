"""Read-only comparison and immutable publication manifest. Never applies a migration."""
from pathlib import Path
import hashlib,json,re
OUT=Path(__file__).resolve().parent
ROOT=OUT.parents[2]
def read(name): return json.loads((OUT/name).read_text(encoding='utf-8'))
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def write(name,value): (OUT/name).write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')
prod=read('PROD_PREFLIGHT_REMOTE.json');dev=read('DEV_PREFLIGHT_REMOTE.json')
assert prod['project']['name']=='saafmilk-prod' and prod['project']['ref']=='tlsuxbghuhlqntkgtrxt'
assert prod['project']['status']=='ACTIVE_HEALTHY'
assert prod['prerequisites']['columns']==dev['prerequisites']['columns']
assert prod['prerequisites']['functions']==dev['prerequisites']['functions']
assert all(prod['baseline'][k] is None for k in ['followup_schema','snapshot_rpc','command_rpc'])
assert prod['baseline']['shop_timezone'] and prod['baseline']['credit_contracts']==0
paths=['supabase/migrations/20261016110904_customer_followups_v1.sql',
    'supabase/migrations/20261016112511_followup_actor_indexes.sql']
source=(ROOT/paths[0]).read_text(encoding='utf-8')
functions=re.findall(r'CREATE FUNCTION\s+([\w.]+)\(.*?\) RETURNS.*?AS \$\$(.*?)\$\$;',source,re.S)
assert len(functions)==7
body_comparison={}
for name,body in functions:
    schema,function=name.split('.')
    deployed=next(d for d in dev['definitions'] if d['nspname']==schema and d['proname']==function)
    remote=re.search(r'AS \$function\$(.*?)\$function\$',deployed['definition'],re.S).group(1)
    assert body.strip()==remote.strip(),name+' deployed body differs'
    body_comparison[name]=hashlib.sha256(body.strip().encode()).hexdigest()
inventory=read('DEV_FOLLOWUP_INVENTORY.json')
assert len(inventory['tables'])==6 and len(inventory['indexes'])==16
assert all(t['rls'] and not t['anon_access'] and not t['authenticated_access'] for t in inventory['tables'])
assert all(p['definer'] and p['authenticated'] and not p['anon'] and p['config']==['search_path=""'] for p in inventory['rpc_privileges'])
explicit_indexes=re.findall(r'CREATE INDEX\s+(\w+)',source+'\n'+(ROOT/paths[1]).read_text())
assert len(explicit_indexes)==8
assert set(explicit_indexes)<={i['name'] for i in inventory['indexes']}
assert not re.search(r'\b(?:INSERT\s+INTO|UPDATE|DELETE\s+FROM)\s+(?:public|credit_private)\.',source,re.I)
assert read('B_CONTRACT_CHECKS.json')['verification']['passed']==72
checks=(OUT/'b-contract-checks.sql').read_text()
assert checks.startswith('-- DEV only.') and checks.rstrip().endswith('ROLLBACK;')
body=checks.split('\n',1)[1]
guard="""DO $date_guard$
BEGIN
  IF (clock_timestamp() AT TIME ZONE 'Asia/Karachi')::time >= time '23:57'
    OR (clock_timestamp() AT TIME ZONE 'Asia/Karachi')::time < time '00:01' THEN
    RAISE EXCEPTION 'Run the scoped fixture outside the shop midnight/quiet-window boundary';
  END IF;
END $date_guard$;
"""
prepared='-- Exact PROD approval required; synthetic fixtures roll back. Original72 checks unchanged.\n'+body.replace('BEGIN;\n','BEGIN;\n'+guard,1)
(OUT/'prod-contract-checks.sql').write_text(prepared,encoding='utf-8')
assert prepared.split('\n',1)[1].replace(guard,'',1)==body
write('PROD_PUBLICATION_MANIFEST.json',{'date':'2026-10-17','sourceBaseline':'cc60170',
    'project':{'name':'saafmilk-prod','ref':'tlsuxbghuhlqntkgtrxt'},'state':'PREPARED_NOT_APPLIED',
    'migrations':[{'name':name,'path':path,'sha256':sha(ROOT/path)} for name,path in zip(['customer_followups_v1','followup_actor_indexes'],paths)],
    'deployedDevFunctionBodiesMatch':body_comparison,'prerequisiteColumnsAndFunctionsMatch':True,
    'devInventory':{'privateTables':6,'totalIndexes':16,'explicitIndexes':8,'authenticatedRpcs':2},
    'checks':{'path':str((OUT/'prod-contract-checks.sql').relative_to(ROOT)),'sha256':sha(OUT/'prod-contract-checks.sql'),
        'expected':72,'originalBodyUnchangedApartFromPreFixtureTimeGuard':True,'transaction':'BEGIN/ROLLBACK'},
    'protectedExistingRowHashes':prod['prerequisites']['table_hashes'],
    'activation':False,'financialRewrite':False,'ordinaryApkInstall':False,'customerContact':False,
    'approvalIncludes':'Apply exactly the two named migrations in order, then run scoped rolled-back synthetic contract/permission checks, compare deployed objects and protected row hashes, and review relevant advisor deltas. No shop opt-in or ordinary install.'})
print('Prepared only: seven deployed DEV bodies match; prerequisites identical; six private tables/two RPCs/eight explicit indexes; original72 checks pinned.')
