from pathlib import Path
import sys, json, subprocess, urllib.request, urllib.error, base64, hashlib, time
folder=Path(__file__).resolve().parent
root=folder.parents[2]
server=root/'audit/2026-10-14/finish-engineering/server-checkout'
sys.path.insert(0,str(server))
import admin, recover_identity
from lifecycle import CLI, NAME, ORG, DEV, PROD, objects

record=json.loads((folder/'created.json').read_text())
ref=record['ref']
assert ref not in [DEV,PROD] and record['name']==NAME and record['organization_id']==ORG
assert subprocess.check_output(['git','rev-parse','HEAD'],cwd=server,text=True).strip()=='c21b9837584725d95e8d5de63c00974fa92e30df'
fixture=json.loads((root/'audit/2026-10-13/identity-recovery-proposal/synthetic-fixture.json').read_text())
checks={}
def verified(name,condition):
    checks[name]=bool(condition)
    if not condition: raise AssertionError(name)
    print('PASS '+name,flush=True)
result=subprocess.run([*CLI,'projects','api-keys','--project-ref',ref,'--reveal','--output-format','json','--log-level','none','--agent','yes'],capture_output=True,text=True,encoding='utf-8',timeout=90)
assert result.returncode==0,'Disposable QA key lookup failed'
keys=json.loads(result.stdout)
secret=publishable=None
for row in objects(keys):
    if row.get('disabled'): continue
    value=row.get('api_key') or row.get('key') or row.get('value')
    if not isinstance(value,str): continue
    role=row.get('role') or row.get('name')
    if value.startswith('eyJ'):
        payload=json.loads(base64.urlsafe_b64decode(value.split('.')[1]+'==='))
        assert payload.get('ref')==ref
        role=payload.get('role')
    if value.startswith('sb_secret_') or role=='service_role': secret=value
    elif value.startswith('sb_publishable_') or role=='anon': publishable=value
assert secret and publishable,'Expected QA API key types missing'
del result,keys
cfg={'url':'https://'+ref+'.supabase.co','secret':secret,'project_name':NAME,'project_ref':ref,'fake_domain':'saafmilk.local','is_prod':False}
recover_identity.validated_target(cfg,ref)
class CountingClient(admin.AdminClient):
    creates=0
    def auth_create_user(self,email,password):
        self.creates+=1
        return super().auth_create_user(email,password)
client=CountingClient(cfg)
def public_request(method,path,token=None,body=None):
    headers={'apikey':publishable,'Content-Type':'application/json'}
    if token: headers['Authorization']='Bearer '+token
    request=urllib.request.Request(cfg['url']+path,data=json.dumps(body).encode() if body is not None else None,headers=headers,method=method)
    try:
        with urllib.request.urlopen(request,timeout=45) as response:
            raw=response.read()
            return response.status,json.loads(raw) if raw else None
    except urllib.error.HTTPError as error:
        return error.code,json.loads(error.read().decode())
def rpc(name,token=None,body=None):
    return public_request('POST','/rest/v1/rpc/'+name,token,body or {})
def snapshot():
    rows={table:client.rest_select(table,{'select':'*'}) for table in ['businesses','shops','staff','staff_shops','audit_log','sync_records','auth_reconciliation']}
    auth=client.request('GET','/auth/v1/admin/users',query={'page':1,'per_page':100})
    rows['auth_ids']=sorted(u['id'] for u in auth['users'])
    return rows
def digest(rows):
    return hashlib.sha256(json.dumps(rows,sort_keys=True,default=str).encode()).hexdigest()
credentials={}
try:
    # Poll only this read-only RPC for the new schema cache; never retry a restore/Auth mutation.
    for attempt in range(8):
        try:
            status=client.request('POST','/rest/v1/rpc/admin_identity_recovery_source_status',body={})
            break
        except admin.AdminError:
            if attempt==7: raise
            time.sleep(2)
    before=snapshot()
    verified('fresh_destination_empty',all(not rows for rows in before.values()))
    credentials=recover_identity.restore_rows(client,fixture)
    verified('exactly_two_auth_creates',client.creates==2 and set(credentials)=={'synthetic.owner','synthetic.disabled'})
    restored=snapshot()
    def field_value(field,value):
        if field.endswith('_at') and value is not None:
            from datetime import datetime, timezone
            return datetime.fromisoformat(value.replace('Z','+00:00')).astimezone(timezone.utc)
        return value
    for table in ['businesses','shops','staff','staff_shops']:
        actual={r.get('id') or (r.get('staff_id'),r.get('shop_id')):r for r in restored[table]}
        expected={r.get('id') or (r.get('staff_id'),r.get('shop_id')):r for r in fixture[table]}
        verified(table+'_identities_preserved',set(actual)==set(expected) and all(all(field_value(field,actual[key].get(field))==field_value(field,value) for field,value in source.items()) for key,source in expected.items()))
    semantic=['at','business_id','action','performed_by','performed_by_username','target_username','detail']
    def audit_sort(events):
        from datetime import datetime
        normalized=[]
        for row in events:
            item={k:row.get(k) for k in semantic}
            item['at']=datetime.fromisoformat(item['at'].replace('Z','+00:00')).isoformat()
            normalized.append(json.dumps(item,sort_keys=True))
        return sorted(normalized)
    original_audit=[r for r in restored['audit_log'] if r['action']!='recover_identity']
    recovery_markers=[r for r in restored['audit_log'] if r['action']=='recover_identity']
    verified('semantic_audit_preserved',audit_sort(original_audit)==audit_sort(fixture['audit_log']))
    verified('one_explicit_recovery_marker',len(recovery_markers)==1 and recovery_markers[0]['business_id']==fixture['businesses'][0]['id'] and recovery_markers[0]['detail']=={'from_export':True,'auth_pending':True})
    staff={r['username']:r for r in restored['staff']}
    verified('new_auth_links_and_terminated_unlinked',len(restored['auth_ids'])==2 and staff['synthetic.terminated']['auth_user_id'] is None and
        set(restored['auth_ids'])=={staff['synthetic.owner']['auth_user_id'],staff['synthetic.disabled']['auth_user_id']} and not(set(restored['auth_ids']) & {r['id'] for r in fixture['staff']}))
    tokens={}
    for username in ['synthetic.owner','synthetic.disabled']:
        code,session=public_request('POST','/auth/v1/token?grant_type=password',body={'email':admin.email_for(username,cfg['fake_domain']),'password':credentials[username]})
        verified(username+'_auth_signin',code==200 and bool(session.get('access_token')))
        tokens[username]=session['access_token']
    code,profile=rpc('my_profile',tokens['synthetic.owner'])
    verified('owner_profile_role_and_shop',code==200 and profile['staff_id']==staff['synthetic.owner']['id'] and profile['business_id']==fixture['businesses'][0]['id'] and profile['role']=='owner' and profile['active'] is True and len(profile['shops'])==1 and profile['shops'][0]['id']==fixture['shops'][0]['id'])
    code,overview=rpc('admin_overview',tokens['synthetic.owner'])
    verified('owner_admin_read_allowed',code==200 and isinstance(overview,dict))
    code,disabled=rpc('my_profile',tokens['synthetic.disabled'])
    verified('disabled_profile_stays_disabled',code==200 and disabled['active'] is False and disabled['role']=='cashier' and disabled['terminated_at'] is None)
    code,_=rpc('admin_overview',tokens['synthetic.disabled'])
    verified('disabled_admin_denied',code in [401,403])
    code,denial=rpc('sync_push',tokens['synthetic.disabled'],{'envelopes':[],'shop_id':fixture['shops'][0]['id']})
    verified('disabled_sync_write_denied',code==200 and denial.get('ok') is False and denial.get('error',{}).get('code')=='not_authorized')
    for actor,token in [('owner',tokens['synthetic.owner']),('disabled',tokens['synthetic.disabled']),('anonymous',None)]:
        for name,body in [('admin_identity_recovery_source_status',{}),('admin_restore_empty_identity',{'p_export':fixture})]:
            code,_=rpc(name,token,body)
            verified(actor+'_'+name+'_denied',code in [401,403])
        code,_=public_request('GET','/rest/v1/requests?select=*&limit=1',token)
        # Private-schema catalog ACLs are checked separately; a non-exposed public route is absent.
        verified(actor+'_private_reset_not_exposed',code==404)
    before_repeat=snapshot()
    refused=False
    try:
        recover_identity.restore_rows(client,fixture)
    except admin.AdminError as error:
        refused='destination is not empty' in str(error)
    verified('repeat_restore_refused_before_auth',refused and client.creates==2)
    after_repeat=snapshot()
    verified('repeat_refusal_preserves_exact_state',digest(before_repeat)==digest(after_repeat))
    result={'projectRef':ref,'sourceCommit':'c21b9837584725d95e8d5de63c00974fa92e30df','checks':checks,'authCreateCalls':client.creates,'identityCounts':{k:len(v) for k,v in restored.items()},'repeatBeforeSha256':digest(before_repeat),'repeatAfterSha256':digest(after_repeat),'passed':True}
except Exception as error:
    result={'projectRef':ref,'checks':checks,'authCreateCalls':client.creates,'passed':False,'errorType':type(error).__name__,'failedCheck':str(error) if isinstance(error,AssertionError) else None,'stage':getattr(error,'stage',None)}
    (folder/'live-results.json').write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps(result),flush=True)
    raise SystemExit(1)
else:
    (folder/'live-results.json').write_text(json.dumps(result,indent=2)+'\n')
    print('Hosted synthetic recovery and access checks accepted.',flush=True)
finally:
    credentials.clear()
