"""Named PROD acceptance, disposable identities only; no secrets saved or printed."""
from pathlib import Path
import argparse, base64, contextlib, hashlib, importlib.util, io, json, re, secrets, subprocess, sys, uuid
import urllib.request, urllib.error
from playwright.sync_api import sync_playwright, expect

OUT = Path(__file__).resolve().parent
ROOT = OUT.parents[2]
SERVER = ROOT/'audit/2026-10-14/finish-engineering/server-checkout'
sys.path.insert(0, str(SERVER))
import admin
REF = 'tlsuxbghuhlqntkgtrxt'
BASE = 'https://'+REF+'.supabase.co'
VIEWER = 'https://sm-server-view.vercel.app'
CLI = ['C:/Program Files/nodejs/node.exe', 'C:/Users/Teymur/AppData/Roaming/npm/node_modules/supabase/dist/supabase.js']
RUN = 'qa_finish_'+secrets.token_hex(3)
fixtures, passed, outstanding = [], [], {}
failure = None
original = None

def save():
    record = {'run': RUN, 'project': REF, 'viewer': VIEWER, 'viewerCommit': 'd2c94e1',
        'passed': passed, 'failure': failure, 'fixtures': [{k:r[k] for k in ('id','username','role','auth_user_id','business_id')} for r in fixtures],
        'outstanding': outstanding}
    (OUT/'prod-reset.json').write_text(json.dumps(record, indent=2)+'\n', encoding='utf-8')

def ok(name):
    passed.append(name); save(); print('PASS '+name, flush=True)

def objects(value):
    if isinstance(value, dict):
        yield value
        for v in value.values(): yield from objects(v)
    elif isinstance(value, list):
        for v in value: yield from objects(v)

lookup = subprocess.run([*CLI, 'projects','api-keys','--project-ref',REF,'--reveal','--output-format','json','--log-level','none','--agent','yes'],
    capture_output=True, text=True, encoding='utf-8', timeout=90)
assert lookup.returncode == 0, 'Existing PROD key lookup unavailable; no fixture created'
secret = publishable = None
for row in objects(json.loads(lookup.stdout)):
    if row.get('disabled'): continue
    value = row.get('api_key') or row.get('key') or row.get('value')
    if not isinstance(value, str): continue
    role = row.get('role') or row.get('name')
    if value.startswith('eyJ'):
        payload = json.loads(base64.urlsafe_b64decode(value.split('.')[1]+'==='))
        assert payload.get('ref') == REF
        role = payload.get('role')
    if value.startswith('sb_secret_') or role == 'service_role': secret = value
    elif value.startswith('sb_publishable_') or role == 'anon': publishable = value
assert secret and publishable, 'Expected existing key types missing; no fixture created'
del lookup
client = admin.AdminClient({'url':BASE,'secret':secret,'project_name':'saafmilk-prod','project_ref':REF,'fake_domain':'saafmilk.local','is_prod':True})

def req(method, path, body=None, token=None):
    headers = {'apikey': publishable if token is not None else secret, 'Content-Type':'application/json'}
    if token: headers['Authorization'] = 'Bearer '+token
    elif token is None and secret.startswith('eyJ'): headers['Authorization'] = 'Bearer '+secret
    request = urllib.request.Request(BASE+path, data=json.dumps(body).encode() if body is not None else None, headers=headers, method=method)
    try: response = urllib.request.urlopen(request, timeout=35)
    except urllib.error.HTTPError as error: response = error
    raw = response.read()
    return response.status, json.loads(raw) if raw else None

def good(pair):
    status, data = pair
    if not 200 <= status < 300:
        raise RuntimeError('Provider rejected HTTP '+str(status)+' code '+str(data.get('code') if isinstance(data,dict) else 'unknown'))
    return data

def rpc(name, body, token=None): return req('POST','/rest/v1/rpc/'+name,body,token)
def signin(row, password=None): return req('POST','/auth/v1/token?grant_type=password',{'email':row['username']+'@saafmilk.local','password':password or row['password']},'')
def audit(row): return good(req('GET','/rest/v1/audit_log?target_username=eq.'+row['username']+'&action=eq.reset_server_credential&select=id,performed_by,detail'))
def digest(value): return hashlib.sha256(json.dumps(value,sort_keys=True,default=str).encode()).hexdigest()
def snapshot():
    tables = {t:good(req('GET','/rest/v1/'+t+'?select=*&order=id')) for t in ['businesses','shops','staff']}
    auth = good(req('GET','/auth/v1/admin/users?page=1&per_page=1000'))['users']
    assert len(auth)<1000, 'Pagination boundary requires explicit handling before any fixture'
    tables['auth_ids'] = sorted(r['id'] for r in auth)
    return tables
def viewer(context, method, path, body=None):
    response = context.request.fetch(VIEWER+path, method=method, data=body, timeout=45000)
    return response.status, response.json()
def login(context, page, row):
    page.goto(VIEWER+'/login', wait_until='networkidle')
    page.get_by_placeholder('Enter your username').fill(row['username'])
    page.get_by_placeholder('Enter server credential').fill(row['password'])
    page.get_by_role('button',name='Sign in',exact=True).click()
    page.wait_for_url(lambda url: '/login' not in url, timeout=35000)
    status, data = viewer(context,'GET','/api/system')
    assert status == 200 and data['ok'] and data['account_server']['env']=='prod'
    assert data['viewer']['commit']=='d2c94e1' and data['account_server']['status']=='online'
    return data

try:
    original = snapshot()
    owners = [r for r in original['staff'] if r['role']=='owner' and r['active'] and not r.get('terminated_at')]
    assert owners, 'No existing Owner for safe supported fixture retirement'
    business = owners[0]['business_id']
    (OUT/'prod-original-checkpoint.json').write_text(json.dumps({t:{'count':len(rows),'sha256':digest(rows)} for t,rows in original.items()},indent=2)+'\n')
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        contexts = [browser.new_context(viewport={'width':1100,'height':850}) for _ in range(2)]
        for role in ['owner','admin','cashier']:
            username = RUN+'_'+role
            password = f'{secrets.randbelow(10**15):015d}'
            identity = good(req('POST','/auth/v1/admin/users',{'email':username+'@saafmilk.local','password':password,'email_confirm':True}))
            row = {'id':str(uuid.uuid4()),'business_id':business,'auth_user_id':identity['id'],'username':username,
                'display_name':'Disposable finish QA '+role,'role':role,'active':True,'terminated_at':None,'password':password}
            fixtures.append(row); save()
            good(req('POST','/rest/v1/staff',{k:v for k,v in row.items() if k!='password'}))
            client.audit('create',username,{'staff_id':row['id'],'role':role,'fixture':RUN},business_id=business)
        owner, actor, target = fixtures
        oc, ac = contexts
        op, ap = oc.new_page(), ac.new_page()
        login(oc,op,owner); login(ac,ap,actor)
        ok('actual browser Owner/Admin login reaches published PROD with online authenticated account-server response')
        owner_token = good(signin(owner))['access_token']
        actor_token = good(signin(actor))['access_token']
        status,_ = rpc('admin_claim_credential_reset',{'p_request_id':str(uuid.uuid4())},actor_token)
        assert status in (401,403,404)
        ok('ordinary staff token cannot call service-only reset claim')
        for protected in [owner,actor]:
            status,data = viewer(ac,'POST','/api/admin/reset-credential',{'staff_id':protected['id']})
            assert status==403 and not data['ok'] and 'credential' not in data and not audit(protected)
            assert signin(protected)[0]==200
        good(req('PATCH','/rest/v1/staff?id=eq.'+target['id'],{'active':False}))
        status,data = viewer(ac,'POST','/api/admin/reset-credential',{'staff_id':target['id']})
        assert status==403 and not data['ok'] and not audit(target)
        ap.goto(VIEWER+'/staff/'+owner['id'],wait_until='networkidle')
        expect(ap.get_by_role('button',name='Reset Credential',exact=True)).to_be_disabled(timeout=15000)
        expect(ap.get_by_role('status').filter(has_text='Only an Owner')).to_be_visible()
        ap.screenshot(path=str(OUT/'admin-protected-target.png'))
        ok('Admin cannot reset Owner/Admin/disabled Cashier; browser disables protected-target reset and explains why')
        op.goto(VIEWER+'/staff/'+target['id'],wait_until='networkidle')
        reset = op.get_by_role('button',name='Reset Credential',exact=True)
        expect(reset).to_be_enabled(timeout=15000)
        op.once('dialog',lambda d:d.dismiss())
        reset.click()
        assert not audit(target) and signin(target)[0]==200
        ok('browser confirmation cancellation sends no reset and keeps original fixture credential')
        old = target['password']
        op.once('dialog',lambda d:d.accept())
        with op.expect_response(lambda r: r.url==VIEWER+'/api/admin/reset-credential' and r.request.method=='POST') as received:
            reset.click()
        response = received.value
        data = response.json()
        assert response.status==200 and data['ok'] and data['pending'] is False
        target['password']=data['credential']
        expect(op.get_by_text('Credential Reset Successfully',exact=True)).to_be_visible(timeout=15000)
        credential = op.get_by_text(re.compile(r'^\d{5}-\d{5}-\d{5}$'))
        expect(credential).to_have_count(1)
        assert re.sub(r'\D','',credential.inner_text())==target['password']
        assert signin(target)[0]==200 and signin(target,old)[0]==400
        assert good(req('GET','/rest/v1/staff?id=eq.'+target['id']+'&select=active'))[0]['active'] is False
        assert len(audit(target))==1
        op.screenshot(path=str(OUT/'owner-reset-success-masked.png'),mask=[credential])
        op.get_by_role('button',name='Dismiss',exact=True).click()
        expect(op.get_by_text('Credential Reset Successfully',exact=True)).to_have_count(0)
        ok('Owner browser reset displays working one-time credential; old fails; disabled target stays disabled; one audit; dismissal works')
        good(req('PATCH','/rest/v1/staff?id=eq.'+target['id'],{'active':True}))
        old=target['password']
        status,data = viewer(ac,'POST','/api/admin/reset-credential',{'staff_id':target['id']})
        assert status==200 and data['ok'] and not data['pending']
        target['password']=data['credential']
        assert signin(target)[0]==200 and signin(target,old)[0]==400 and len(audit(target))==2
        ok('Admin can reset active Cashier; replacement works and completion audit occurs once')
        reference=str(uuid.uuid4())
        good(rpc('admin_begin_credential_reset',{'p_staff_id':target['id'],'p_request_id':reference},owner_token))
        outstanding[reference]='reserved_no_auth'; save()
        for participant in [owner,target]:
            status,data=req('PATCH','/rest/v1/staff?id=eq.'+participant['id'],{'active':False})
            assert status>=400 and data.get('code')=='55000'
        claim=good(rpc('admin_claim_credential_reset',{'p_request_id':reference}))[0]
        outstanding[reference]='claimed_no_auth'; save()
        status,data=rpc('admin_claim_credential_reset',{'p_request_id':reference})
        assert status>=400 and data.get('code')=='55000'
        password=f'{secrets.randbelow(10**15):015d}'
        outstanding[reference]='auth_outcome_unknown'; save()
        identity=good(req('PUT','/auth/v1/admin/users/'+claim['auth_user_id'],{'password':password}))
        assert identity['id']==target['auth_user_id']
        target['password']=password
        outstanding[reference]='known_success'; save()
        good(rpc('admin_mark_credential_reset_uncertain',{'p_request_id':reference,'p_code':'AUTH_UNKNOWN'}))
        op.reload(wait_until='networkidle')
        expect(op.get_by_role('status').filter(has_text=reference)).to_be_visible(timeout=15000)
        expect(op.get_by_role('button',name='Reset Credential',exact=True)).to_be_disabled()
        op.get_by_role('button',name='Check reset status',exact=True).click()
        expect(op.get_by_role('status').filter(has_text=reference)).to_be_visible()
        status,data=viewer(oc,'POST','/api/admin/reset-credential',{'staff_id':target['id']})
        assert status==202 and data['pending'] and data['request_id']==reference and 'credential' not in data and len(audit(target))==2
        op.screenshot(path=str(OUT/'pending-reference.png'))
        ok('uncertain reservation shown with reference and safe warning; repeat reset blocked; participant edits and second claim denied')
        good(rpc('admin_finish_credential_reset',{'p_request_id':reference,'p_outcome':'completed'}))
        good(rpc('admin_finish_credential_reset',{'p_request_id':reference,'p_outcome':'completed'}))
        del outstanding[reference]; save()
        assert len(audit(target))==3 and signin(target)[0]==200
        assert good(rpc('admin_find_pending_credential_reset',{'p_staff_id':target['id']}))==[]
        op.get_by_role('button',name='Check reset status',exact=True).click()
        expect(op.get_by_role('button',name='Reset Credential',exact=True)).to_be_enabled(timeout=15000)
        expect(op.get_by_role('status')).to_have_count(0)
        ok('trusted known-success completion is idempotent; one audit, no Auth replay; browser status releases pending controls')
        browser.close()
except Exception as error:
    # Exceptions may contain page/request data; retain only class and a source line.
    import traceback
    frames=traceback.extract_tb(error.__traceback__)
    failure={'type':type(error).__name__,'line':frames[-1].lineno if frames else None}
    print('STOP '+json.dumps(failure),flush=True); save()
finally:
    for reference,state in list(outstanding.items()):
        outcome = 'completed' if state=='known_success' else 'not_applied' if state in ('reserved_no_auth','claimed_no_auth') else None
        if outcome:
            try:
                good(rpc('admin_finish_credential_reset',{'p_request_id':reference,'p_outcome':outcome}))
                del outstanding[reference]
            except Exception: pass
    if not outstanding:
        retired=[]
        for row in reversed(fixtures):
            try:
                assert row['username'].startswith(RUN+'_') and row['id'] in [r['id'] for r in fixtures]
                assert good(rpc('admin_find_pending_credential_reset',{'p_staff_id':row['id']}))==[]
                with contextlib.redirect_stdout(io.StringIO()):
                    code=admin.cmd_terminate(client,argparse.Namespace(username=row['username'],confirm_username=row['username'],confirm_project='saafmilk-prod'))
                assert code==0
                retired.append(row['id'])
            except Exception:
                failure=failure or {'type':'FixtureRetirementIncomplete','staffId':row['id']}
        if len(retired)==len(fixtures) and fixtures: ok('all three QA fixtures retired through supported lifecycle, with no held participants')
        if original is not None:
            try:
                final=snapshot()
                ids={r['id'] for r in fixtures}
                final_original=dict(final,staff=[r for r in final['staff'] if r['id'] not in ids])
                checks={t:digest(original[t])==digest(final_original[t]) for t in original}
                retired_rows=[r for r in final['staff'] if r['id'] in ids]
                checks['fixturesRetired']=all(not r['active'] and r['terminated_at'] and r['auth_user_id'] is None for r in retired_rows) and len(retired_rows)==len(fixtures)
                (OUT/'prod-cleanup.json').write_text(json.dumps({'originalUnchanged':checks,'fixtureCount':len(fixtures),'outstanding':outstanding},indent=2)+'\n')
                assert all(checks.values())
                ok('original businesses, shops, staff rows and Auth IDs unchanged; fixture identities removed and history retained')
            except Exception:
                failure=failure or {'type':'OriginalStateComparisonFailed'}
    else:
        failure=failure or {'type':'UnknownAuthOutcomeHeld'}
    save()
sys.exit(1 if failure else 0)
