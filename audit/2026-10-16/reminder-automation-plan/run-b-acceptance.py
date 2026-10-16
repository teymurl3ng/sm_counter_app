"""Fixed real DEV follow-up story; two physical phones; no external composer launch."""
from pathlib import Path
import concurrent.futures,threading
import argparse,ast,contextlib,hashlib,importlib.util,io,json,secrets,sqlite3,subprocess,tempfile,time,uuid
from datetime import datetime,timezone,timedelta
OUT=Path(__file__).resolve().parent
ROOT=OUT.parents[2]
ADB='C:/Users/Teymur/AppData/Local/Android/Sdk/platform-tools/adb.exe'
PACKAGE='com.saaf.milkshop.qa.tests'
DEV='https://tghzmofkkdsaasvnixps.supabase.co'
available=[s.split()[0] for s in subprocess.check_output([ADB,'devices'],text=True).splitlines()[1:] if s.strip().endswith('device')]
assert 'R8VXA02SNKH' in available,'A06 is required before creating any DEV fixtures'
s24=next((s for s in available if 'RFCX116CYYX' in s),None);assert s24,'S24 is required before creating any DEV fixtures'
PHONES={'owner':'R8VXA02SNKH','counter':s24}
lock=threading.Lock();race_at=None
RUN=secrets.token_hex(6); SHOP=str(uuid.uuid4()); SHOP_NAME='qa_followup_'+RUN
DAY=datetime.now(timezone(timedelta(hours=5))).date().isoformat()
fixtures=[];steps=[];comparisons=[];failure=None;created=False;before={}
def save():
    (OUT/'b-acceptance-results.json').write_text(json.dumps(dict(run=RUN,project="tghzmofkkdsaasvnixps",shop=SHOP,fixtureStaff=[{k:v for k,v in f.items() if k!="credential"} for f in fixtures],steps=steps,comparisons=comparisons,failure=failure),indent=2)+'\n')
def shell(role,*args,stdin=None,binary=False):
    r=subprocess.run([ADB,'-s',PHONES[role],*args],input=stdin,capture_output=True,timeout=180)
    if r.returncode: raise RuntimeError('ADB operation failed: '+role+'/'+args[0])
    return r.stdout if binary else r.stdout.decode('utf-8','replace')
def provenance(role):
    return {p:[s.strip() for s in shell(role,'shell','dumpsys','package',p).splitlines()
        if any(k in s for k in ['versionCode=','versionName=','userId=','firstInstallTime=','lastUpdateTime='])]
        for p in ['com.saaf.milkshop','com.saaf.milkshop.qa','com.saaf.milkshop.acceptance']}
def stage(role,name,credential=None):
    if credential:
        data=json.dumps(dict(baseUrl=DEV,publishableKey=PUB,shopId=SHOP,username=credential['username'],credential=credential['credential'])).encode()
        assert shell(role,'shell','run-as',PACKAGE,'tee',f'files/followup-phone-{RUN}/input.json',stdin=data,binary=True)==data
    output=shell(role,'shell','am','instrument','-w','-r','-e','class','com.saaf.milkshop.PhysicalFollowUpProbe#runStage',
        '-e','saafSkipBootstrap','true','-e','qaRun',RUN,'-e','qaDay',DAY,'-e','qaPhone',role,'-e','qaStage',name,'-e','qaStartAt',str(race_at or 0),
        PACKAGE+'.test/com.saaf.milkshop.SaafMilkTestRunner')
    (OUT/f'b-acceptance-{len(steps):02d}-{role}-{name}-console.txt').write_text(output,encoding='utf-8')
    assert 'OK (1 test)' in output and 'FAILURES' not in output, 'Credit stage failed: '+role+'/'+name
    result=json.loads(shell(role,'exec-out','run-as',PACKAGE,'cat',f'files/followup-phone-{RUN}/result.json'))
    assert result['run']==RUN and result['stage']==name
    with lock: steps.append(result);save()
    print('PASS '+role+'/'+name,flush=True);return result
TABLES=['products','accounts','events','inventory_log','financial_log','product_profiles','customer_profiles','vendor_profiles',
    'employee_profiles','expense_category_profiles','budget_plans','collections_targets','settings','store_profile','opening_batches',
    'opening_inventory_entries','opening_account_entries','event_actor_links','physical_cash_counts','daily_close_actions']
META=['credit_term_revisions','credit_term_heads','credit_sale_agreements','credit_review_actions','credit_metadata_pending']
# Reuse the accepted UID-normalization definition without executing the old driver.
tree=ast.parse((ROOT/'audit/2026-10-14/sync-recovery/run-recovery.py').read_text())
node=next(n for n in tree.body if isinstance(n,ast.FunctionDef) and n.name=='canonical')
exec(compile(ast.Module(body=[node],type_ignores=[]),'accepted-canonical','exec'))
def snapshot(role,result):
    name=result['dbFilename'];assert name.endswith('.db') and '/' not in name and '\\' not in name
    subdir=result.get('dbSubdir','db');assert subdir in {'db','restored-db'}
    data=shell(role,'exec-out','run-as',PACKAGE,'cat',f'files/followup-phone-{RUN}/{subdir}/{name}',binary=True)
    with tempfile.TemporaryDirectory(prefix='saaf-credit-') as temp:
        path=Path(temp)/'synthetic.db';path.write_bytes(data)
        db=sqlite3.connect(path);db.row_factory=sqlite3.Row
        try:
            rows={t:[dict(r) for r in db.execute('SELECT * FROM '+t)] for t in TABLES}
            metadata={t:sorted([dict(r) for r in db.execute('SELECT * FROM '+t)],key=lambda r:json.dumps(r,sort_keys=True)) for t in META}
        finally:db.close()
    return dict(rows=rows,metadata=metadata,balances=result['balances'],stock=result['stock'])
def compare(label):
    a=snapshot('owner',stage('owner','status'));b=snapshot('counter',stage('counter','status'))
    ca,cb=canonical(a),canonical(b)
    assert ca==cb,'Financial projection diverged at '+label
    assert a['balances']['Customer: Credit QA Customer']==100 and b['balances']['Customer: Credit QA Customer']==100
    digest=hashlib.sha256(json.dumps(ca,sort_keys=True).encode()).hexdigest()
    if comparisons: assert digest==comparisons[0]['sha256'],'Follow-ups changed financial facts'
    comparisons.append(dict(label=label,tablesEqual=20,sha256=digest));save()
    print('PASS '+label+' unchanged finance',flush=True)

def integrity():
    result={}
    for table in ['businesses','shops','staff','sync_records']:
        rows=[]
        while True:
            page=client.rest_select(table,dict(select='*',order='server_seq.asc' if table=='sync_records' else 'id.asc',limit='500',offset=str(len(rows))))
            if not page:break
            rows.extend(page)
        rows=[r for r in rows if not (table=='shops' and r['id']==SHOP) and not (table=='staff' and r['id'] in [f['id'] for f in fixtures]) and not (table=='sync_records' and r['shop_id']==SHOP)]
        result[table]=dict(count=len(rows),sha256=hashlib.sha256(json.dumps(sorted(rows,key=lambda r:json.dumps(r,sort_keys=True)),sort_keys=True).encode()).hexdigest())
    return result
values={}
for line in Path('C:/Users/Teymur/Documents/Projects/saaf-milk-admin-viewer/.env.local').read_text().splitlines():
    if '=' in line and not line.lstrip().startswith('#'):
        k,v=line.split('=',1);values[k.strip()]=v.strip().strip('"').strip("'")
assert values['ACCOUNT_SERVER_ENV']=='dev' and values['ACCOUNT_SERVER_URL'].rstrip('/')==DEV
PUB=values['ACCOUNT_SERVER_PUBLISHABLE_KEY']
spec=importlib.util.spec_from_file_location('credit_qa_admin','C:/Users/Teymur/Desktop/saaf-anchor-tmp/admin.py')
admin=importlib.util.module_from_spec(spec);spec.loader.exec_module(admin)
client=admin.AdminClient(dict(url=DEV,secret=values['ACCOUNT_SERVER_ADMIN_KEY'],fake_domain=values.get('FAKE_DOMAIN','saafmilk.local'),project_name='saafmilk-dev',is_prod=False))
clock_samples={}
for role in PHONES:
    start=int(time.time()*1000)
    stamp=int(shell(role,'shell','date','+%s%3N').strip())
    end=int(time.time()*1000)
    clock_samples[role]=dict(offset=stamp-(start+end)//2,uncertainty=(end-start)//2+1)
assert all(abs(s['offset'])<2000 and s['uncertainty']<1000 for s in clock_samples.values()),'Device clock calibration needs review'
(OUT/'b-acceptance-clocks.json').write_text(json.dumps(clock_samples,indent=2)+'\n')
baseline=integrity()
try:
    for role in PHONES:
        before[role]=provenance(role)
        for apk in ['debug/app-debug.apk','androidTest/debug/app-debug-androidTest.apk']:
            artifact=ROOT/'phase18/app/build/outputs/apk'/apk
            badging=subprocess.check_output(['C:/Users/Teymur/AppData/Local/Android/Sdk/build-tools/36.0.0/aapt.exe','dump','badging',str(artifact)],text=True)
            expected=PACKAGE+('.test' if apk.startswith('androidTest/') else '')
            assert "package: name='"+expected+"'" in badging
            assert 'Success' in shell(role,'install','-r',str(artifact))
        stage(role,'init')
    business=client.rest_select('staff',dict(select='business_id',role='eq.owner',active='eq.true',terminated_at='is.null',limit='1'))[0]['business_id']
    client.rest_insert('shops',dict(id=SHOP,business_id=business,name=SHOP_NAME,active=True,shop_timezone='Asia/Karachi'));created=True
    for phone in ['owner','counter']:
        role='owner'
        username='qa_followup_'+RUN+('_o' if phone=='owner' else '_s');credential=f'{secrets.randbelow(10**15):015d}'
        auth=client.auth_create_user(username+'@'+client.fake_domain,credential);auth_id=(auth.get('user') or auth)['id']
        row=dict(id=str(uuid.uuid4()),business_id=business,auth_user_id=auth_id,username=username,display_name='Disposable Credit '+role,role=role,active=True,credential=credential)
        fixtures.append(row)
        client.rest_insert('staff',{k:v for k,v in row.items() if k!='credential'})
        client.rest_insert('staff_shops',dict(staff_id=row['id'],shop_id=SHOP))
    stage('owner','enroll',fixtures[0]);stage('owner','exchange')
    stage('counter','enroll',fixtures[1]);stage('counter','exchange')
    stage('owner','activate');stage('counter','exchange');compare('baseline')
    stage('owner','policy');stage('owner','schedule')
    # Schedule/pause visibility already passed in the first attempt. Remaining story only.
    race_at=int(time.time()*1000)+30000
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        a=pool.submit(stage,'owner','race');b=pool.submit(stage,'counter','race')
        racers={'owner':a.result(),'counter':b.result()}
    assert sum(r['raceAccepted'] for r in racers.values())==1
    intervals=[dict(started=r['lastCommandHttp']['started']-clock_samples[role]['offset']+clock_samples[role]['uncertainty'],finished=r['lastCommandHttp']['finished']-clock_samples[role]['offset']-clock_samples[role]['uncertainty'],clockUncertaintyMs=clock_samples[role]['uncertainty']) for role,r in racers.items()]
    overlap=max(r['started'] for r in intervals)<min(r['finished'] for r in intervals)
    assert overlap,'Requests did not overlap; do not claim concurrent acceptance'
    winner=next(role for role,r in racers.items() if r['raceAccepted'])
    stage(winner,'release');comparisons.append(dict(label='overlapping-acquire',oneWinner=True,httpIntervals=intervals,overlap=True));save()
    lost=stage('owner','start_lost');shell('owner','shell','am','force-stop',PACKAGE)
    recovered=stage('owner','recover');assert recovered['pid']!=lost['pid']
    stage('counter','blocked');stage('counter','resolve')
    opened=stage('counter','open');assert opened['launches']==1
    stage('owner','blocked');stage('owner','resolve')
    # Compare finance separately from the race diagnostic above.
    race_evidence=comparisons.pop();compare('handoff-loss-recovery');comparisons.append(race_evidence)
    first=stage('owner','digest');assert first['notifyGrant']=='NOTIFY'
    stage('counter','transfer');second=stage('counter','digest');assert second['notifyGrant']=='NONE'
    race_evidence=comparisons.pop();compare('primary-transfer-daily-replay');comparisons.append(race_evidence)

except Exception as error:
    failure=type(error).__name__+': '+str(error);save();raise
finally:
    cleanup=[]
    for role in PHONES:
        try:
            stage(role,'cleanup');assert provenance(role)==before[role]
            cleanup.append(role+': isolated transport cleared; ordinary package provenance unchanged')
        except Exception as error:cleanup.append(role+': cleanup needs attention '+type(error).__name__)
    for row in fixtures:
        try:
            current=client.get_staff_by_username(row['username']);assert current['id']==row['id'] and current['auth_user_id']==row['auth_user_id']
            with contextlib.redirect_stdout(io.StringIO()): admin.cmd_terminate(client,argparse.Namespace(username=row['username'],confirm_username=row['username'],confirm_project=''))
            assert client.auth_get_user(row['auth_user_id']) is None
            cleanup.append(row['role']+': disposable Auth retired')
        except Exception as error:cleanup.append(row['role']+': retirement needs attention '+type(error).__name__)
    if created:
        client.rest_update('shops',dict(id='eq.'+SHOP,name='eq.'+SHOP_NAME),dict(active=False));cleanup.append('Exact disposable shop deactivated')
    after=integrity();assert after==baseline
    (OUT/'b-acceptance-cleanup.json').write_text(json.dumps(dict(run=RUN,cleanup=cleanup,before=baseline,after=after,existingRowsUnchanged=after==baseline),indent=2)+'\n')
    save();print(json.dumps(dict(comparisonsPassed=len(comparisons),failure=failure,cleanup=cleanup)),flush=True)
