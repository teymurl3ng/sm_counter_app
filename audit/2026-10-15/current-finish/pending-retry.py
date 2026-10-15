"""Repeat only the pending-state tail after correcting the test's invalid enum."""
from pathlib import Path
source = (Path(__file__).parent/'prod-reset.py').read_text(encoding='utf-8')
exec(compile(source[:source.index('\ntry:\n')], 'prod-reset-gateways', 'exec'))
OUT = OUT/'pending-retry'
OUT.mkdir(exist_ok=True)

try:
    original = snapshot()
    owners = [r for r in original['staff'] if r['role']=='owner' and r['active'] and not r.get('terminated_at')]
    assert owners
    business = owners[0]['business_id']
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        oc = browser.new_context(viewport={'width':1100,'height':850})
        op = oc.new_page()
        for role in ['owner','cashier']:
            username = RUN+'_'+role
            password = f'{secrets.randbelow(10**15):015d}'
            identity = good(req('POST','/auth/v1/admin/users',{'email':username+'@saafmilk.local','password':password,'email_confirm':True}))
            row = {'id':str(uuid.uuid4()),'business_id':business,'auth_user_id':identity['id'],'username':username,
                'display_name':'Disposable pending QA '+role,'role':role,'active':True,'terminated_at':None,'password':password}
            fixtures.append(row); save()
            good(req('POST','/rest/v1/staff',{k:v for k,v in row.items() if k!='password'}))
            client.audit('create',username,{'staff_id':row['id'],'role':role,'fixture':RUN},business_id=business)
        owner, target = fixtures
        login(oc,op,owner)
        owner_token = good(signin(owner))['access_token']
        reference = str(uuid.uuid4())
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
        op.goto(VIEWER+'/staff/'+target['id'],wait_until='networkidle')
        expect(op.get_by_role('status').filter(has_text=reference)).to_be_visible(timeout=15000)
        expect(op.get_by_role('button',name='Reset Credential',exact=True)).to_be_disabled()
        op.get_by_role('button',name='Check reset status',exact=True).click()
        expect(op.get_by_role('status').filter(has_text=reference)).to_be_visible()
        status,data=viewer(oc,'POST','/api/admin/reset-credential',{'staff_id':target['id']})
        assert status==202 and data['pending'] and data['request_id']==reference and 'credential' not in data and len(audit(target))==0
        op.screenshot(path=str(OUT/'pending-reference.png'))
        ok('uncertain reservation shown with reference and safe warning; repeat reset blocked; participant edits and second claim denied')
        good(rpc('admin_finish_credential_reset',{'p_request_id':reference,'p_outcome':'completed'}))
        good(rpc('admin_finish_credential_reset',{'p_request_id':reference,'p_outcome':'completed'}))
        del outstanding[reference]; save()
        assert len(audit(target))==1 and signin(target)[0]==200
        assert good(rpc('admin_find_pending_credential_reset',{'p_staff_id':target['id']}))==[]
        op.get_by_role('button',name='Check reset status',exact=True).click()
        expect(op.get_by_role('button',name='Reset Credential',exact=True)).to_be_enabled(timeout=15000)
        expect(op.get_by_role('status')).to_have_count(0)
        ok('trusted known-success completion is idempotent; one audit, no Auth replay; browser status releases pending controls')
        browser.close()
except Exception as error:
    import traceback
    frames=traceback.extract_tb(error.__traceback__)
    failure={'type':type(error).__name__,'line':frames[-1].lineno if frames else None}
    print('STOP '+json.dumps(failure),flush=True); save()
finally:
    # The original finalizer resolves only proven outcomes and retires exact QA IDs.
    tail = source.split('\nfinally:\n',1)[1].replace('all three QA fixtures retired','all QA fixtures retired')
    exec(compile('try:\n    pass\nfinally:\n'+tail, 'prod-reset-finalizer', 'exec'))
