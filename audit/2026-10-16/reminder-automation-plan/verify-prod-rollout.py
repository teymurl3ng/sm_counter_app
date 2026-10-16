"""Verify saved approved publication evidence; never connects to or mutates a database."""
from pathlib import Path
import hashlib,json
OUT=Path(__file__).resolve().parent
ROOT=OUT.parents[2]
def read(name): return json.loads((OUT/name).read_text(encoding='utf-8'))
r=read('PROD_ROLLOUT.json');m=read('PROD_PUBLICATION_MANIFEST.json')
assert r['project']=='tlsuxbghuhlqntkgtrxt' and r['approvedPackageCommit']=='7129418' and r['state']=='APPLIED_VERIFIED'
for artifact in m['migrations']+[m['checks']]:
    assert hashlib.sha256((ROOT/artifact['path']).read_bytes()).hexdigest()==artifact['sha256']
assert r['checks']['passed']==72 and len(r['checks']['checks'])==72 and r['checks']['rollback']
assert r['before']['table_hashes']==r['after']['table_hashes']==m['protectedExistingRowHashes']
assert r['before']['counts']==r['after']['counts']
assert r['before']['functions']==r['after']['functions']
assert r['before']['columns']==r['after']['columns']
assert r['definitions']==read('DEV_PREFLIGHT_REMOTE.json')['definitions']
assert r['inventory']==read('DEV_FOLLOWUP_INVENTORY.json')
assert all(r['closure'][key]==0 for key in ['policies','customers','claims','actions','commands','digests'])
assert r['closure']['protected_owner_guard']
versions={item['name']:item['version'] for item in r['migrations']}
assert versions['customer_followups_v1']=='20261016195451'
assert versions['followup_actor_indexes']=='20261016195459'
assert not r['advisorDelta']['security']['newLints']
new=r['advisorDelta']['performance']['newLints']
assert len(new)==1 and new[0]['name']=='unused_index' and new[0]['level']=='INFO' and new[0]['count']==6
assert all(f['metadata']['schema']=='followup_private' for f in new[0]['findings'])
assert not any(r[key] for key in ['ordinaryAppInstalled','shopActivated','customerContact'])
print('Verified: exact approved sources,72 rolled-back checks, matching DEV objects/permissions, empty reminder state and unchanged protected data.')
