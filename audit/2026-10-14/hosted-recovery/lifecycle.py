from pathlib import Path
import json, subprocess, secrets, sys
folder=Path(__file__).resolve().parent
CLI=['C:/Program Files/nodejs/node.exe','C:/Users/Teymur/AppData/Roaming/npm/node_modules/supabase/dist/supabase.js']
ORG='hlewlyngnpziytterocd'
NAME='saafmilk-recovery-qa-20261014'
DEV='tghzmofkkdsaasvnixps'
PROD='tlsuxbghuhlqntkgtrxt'
def invoke(args):
    return subprocess.run([*CLI,*args,'--output-format','json','--log-level','none','--agent','yes'],capture_output=True,text=True,encoding='utf-8',timeout=240)
def objects(value):
    if isinstance(value,dict):
        yield value
        for v in value.values(): yield from objects(v)
    elif isinstance(value,list):
        for v in value: yield from objects(v)
def project_list():
    result=invoke(['projects','list'])
    if result.returncode: raise RuntimeError('Project listing failed; no mutation attempted')
    body=json.loads(result.stdout)
    projects=[{k:p.get(k) for k in ['id','ref','name','organization_id','status','region']} for p in objects(body) if p.get('name') and p.get('organization_id') and (p.get('id') or p.get('ref'))]
    if not projects: raise RuntimeError('Unexpected project-list response')
    return {p.get('id') or p['ref']:p for p in projects}
mode=sys.argv[1] if __name__=='__main__' else None
projects=project_list() if mode else {}
if mode=='preflight':
    assert DEV in projects and PROD in projects
    assert not any(p['name']==NAME for p in projects.values())
    (folder/'projects-before.json').write_text(json.dumps(list(projects.values()),indent=2)+'\n')
    print(json.dumps(list(projects.values()),indent=2))
elif mode=='create':
    assert projects[DEV]['status'] not in ['ACTIVE_HEALTHY','COMING_UP']
    assert projects[PROD]['status']=='ACTIVE_HEALTHY'
    assert not any(p['name']==NAME for p in projects.values())
    assert all(p['organization_id']==ORG for p in projects.values())
    password=secrets.token_urlsafe(40)
    result=invoke(['projects','create',NAME,'--org-id',ORG,'--region','ap-northeast-1','--db-password',password])
    del password
    # A failed/lost create acknowledgement is followed only by a read, never a retry.
    after=project_list()
    found=[(ref,p) for ref,p in after.items() if p['name']==NAME and p['organization_id']==ORG]
    if len(found)==1:
        ref,created=found[0]
        assert ref not in projects and ref not in [DEV,PROD] and created['region']=='ap-northeast-1'
        record={'ref':ref,'name':NAME,'organization_id':ORG,'region':created['region'],'status':created['status'],'creationExit':result.returncode,'beforeRefs':sorted(projects)}
        (folder/'created.json').write_text(json.dumps(record,indent=2)+'\n')
        print(json.dumps(record),flush=True)
    else:
        raise RuntimeError('QA creation not confirmed; DEV must be resumed')
    if result.returncode: raise RuntimeError('QA project exists but create acknowledgement failed; inspect before proceeding')
elif mode=='delete':
    record=json.loads((folder/'created.json').read_text())
    ref=record['ref']
    assert ref not in [DEV,PROD] and record['name']==NAME and record['organization_id']==ORG
    assert projects[ref]['name']==NAME and projects[ref]['organization_id']==ORG
    result=invoke(['projects','delete',ref,'--yes'])
    after=project_list()
    confirmed=ref not in after
    (folder/'deleted.json').write_text(json.dumps({'ref':ref,'name':NAME,'confirmedAbsent':confirmed,'exit':result.returncode},indent=2)+'\n')
    assert confirmed,'QA deletion not confirmed; do not resume DEV into an occupied slot'
    print('Only the recorded disposable QA project was deleted; absence verified.',flush=True)
else:
    if mode is not None: raise RuntimeError('Unknown lifecycle action')
