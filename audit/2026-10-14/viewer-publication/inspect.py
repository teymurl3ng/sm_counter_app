from pathlib import Path
import subprocess, json, sys, hashlib, re

folder = Path(__file__).resolve().parent
root = folder.parents[2]
checkout = root/'audit/2026-10-14/finish-engineering/viewer-checkout'
cli = ['C:/Program Files/nodejs/node.exe', 'C:/Users/Teymur/AppData/Roaming/npm/node_modules/vercel/dist/vc.js']
team = 'team_4S1INLjqHa6cjTQCbHxuAt6l'
project = 'prj_x3nKY8gE7Qllsl5Dvf0oyoivjIsR'

def read_api(path):
    result = subprocess.run([*cli, '--scope', 'mkrc', 'api', path+('&' if '?' in path else '?')+'teamId='+team,
        '--method', 'GET'], cwd=checkout, capture_output=True, text=True, encoding='utf-8')
    if result.returncode:
        raise RuntimeError('Vercel read failed; exit '+str(result.returncode)+'; '+result.stderr[:1200])
    body = json.loads(result.stdout)
    if 'error' in body:
        raise RuntimeError('Vercel API error: '+str(body['error'].get('code')))
    return body

deployment = read_api('/v13/deployments/dpl_9A3Bs3kVwzmgUvvuGNuXsdF5Rc7R?withGitRepoInfo=true')
proj = read_api('/v9/projects/'+project)
alias = read_api('/v4/aliases/sm-server-view-mkrc.vercel.app')
def deploy_record(d):
    meta = d.get('meta', {})
    return {key: d.get(key) for key in ['id','url','readyState','target','projectId','alias','aliasAssigned','aliasError','gitSource']} | {
        'sourceMetadata': {k:v for k,v in meta.items() if k in ['gitCommitHash','githubCommitSha','gitCommitRef','githubCommitRef','gitRepo','githubRepo','gitDirty']},
        'envNames': sorted(d.get('env',{}).keys()) if isinstance(d.get('env'),dict) else []}
production = proj.get('targets',{}).get('production',{})
config_checks = {}
if sys.argv[1:] == ['final']:
    for key,expected in [('ACCOUNT_SERVER_URL','https://tlsuxbghuhlqntkgtrxt.supabase.co'),('ACCOUNT_SERVER_ENV','prod')]:
        entry=next(v for v in proj.get('env',[]) if v.get('key')==key and 'production' in v.get('target',[]))
        value=read_api('/v1/projects/'+project+'/env/'+entry['id']).get('value')
        config_checks[key]={'valueReadable':bool(value),'matchesExpected':value.rstrip('/')==expected if value else None}
summary = {'preparedDeployment': deploy_record(deployment),
    'project': {'id':proj.get('id'),'name':proj.get('name'),'accountId':proj.get('accountId'),
        'gitLink':{k:proj.get('link',{}).get(k) for k in ['type','repo','org','productionBranch']}, 'productionTarget':deploy_record(production),
        'protectionConfigured':bool(proj.get('ssoProtection') or proj.get('passwordProtection')),
        'environmentKeys':[{'key':v.get('key'),'target':v.get('target'),'id':v.get('id'),'type':v.get('type')} for v in proj.get('env',[])]},
    'alias':{k:alias.get(k) for k in ['alias','deploymentId','projectId','deployment']}}
summary['nonSecretProductionConfig']=config_checks
assert proj['id']==project and proj['accountId']==team
assert deployment['projectId']==project and deployment['target']=='production' and deployment['readyState']=='READY'
manifest=json.loads((root/'audit/2026-10-14/finish-engineering/publication-manifest.json').read_text(encoding='utf-8'))['viewer']
summary['sourceHashesMatch']=all(hashlib.sha256((checkout/p).read_bytes()).hexdigest()==h for p,h in manifest.items())
summary['sourceCommit']=subprocess.check_output(['git','rev-parse','HEAD'],cwd=checkout,text=True).strip()
assert summary['sourceHashesMatch']
label=sys.argv[1] if len(sys.argv)>1 else 'before'
if label=='final':
    deployed=read_api('/v13/deployments/'+production['id']+'?withGitRepoInfo=true')
    aliases=[read_api('/v4/aliases/'+host) for host in ['sm-server-view.vercel.app','sm-server-view-mkrc.vercel.app','sm-server-view-git-main-mkrc.vercel.app']]
    summary['liveDeployment']=deploy_record(deployed)
    summary['productionAliases']=[{k:a.get(k) for k in ['alias','deploymentId','projectId']} for a in aliases]
    assert deployed['projectId']==project and deployed['readyState']=='READY' and deployed['target']=='production'
    assert deployed['meta']['githubCommitSha']==summary['sourceCommit']
    assert all(a['deploymentId']==deployed['id'] and a['projectId']==project for a in aliases)
    assert all(c['matchesExpected'] is not False for c in config_checks.values())
(folder/(label+'.json')).write_text(json.dumps(summary,indent=2)+'\n',encoding='utf-8')
print(json.dumps({'label':label,'productionConfig':config_checks,'productionAliases':summary.get('productionAliases'), 'productionTarget':summary['project']['productionTarget'],
    'alias':summary['alias'],'sourceCommit':summary['sourceCommit'],'sourceHashesMatch':summary['sourceHashesMatch']},indent=2))
