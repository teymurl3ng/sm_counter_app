"""Only the disposable credit DEV shop and isolated QA package. Never print credentials."""
from pathlib import Path
import argparse,ast,contextlib,hashlib,importlib.util,io,json,secrets,sqlite3,subprocess,tempfile,time,uuid
from datetime import datetime,timezone,timedelta
OUT=Path(__file__).resolve().parent
ROOT=OUT.parents[2]
ADB='C:/Users/Teymur/AppData/Local/Android/Sdk/platform-tools/adb.exe'
PACKAGE='com.saaf.milkshop.qa.tests'
DEV='https://tghzmofkkdsaasvnixps.supabase.co'
PHONES={'owner':'R8VXA02SNKH','counter':'RFCX116CYYX'}
RUN=secrets.token_hex(6); SHOP=str(uuid.uuid4()); SHOP_NAME='qa_credit_'+RUN
DAY=datetime.now(timezone(timedelta(hours=5))).date().isoformat()
fixtures=[];steps=[];comparisons=[];failure=None;created=False;before={}
def save():
    (OUT/'two-phone-results.json').write_text(json.dumps(dict(run=RUN,shop=SHOP,steps=steps,comparisons=comparisons,failure=failure),indent=2)+'\n')
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
        assert shell(role,'shell','run-as',PACKAGE,'tee',f'files/credit-phone-{RUN}/input.json',stdin=data,binary=True)==data
    output=shell(role,'shell','am','instrument','-w','-r','-e','class','com.saaf.milkshop.PhysicalCreditProbe#runStage',
        '-e','saafSkipBootstrap','true','-e','qaRun',RUN,'-e','qaDay',DAY,'-e','qaPhone',role,'-e','qaStage',name,
        PACKAGE+'.test/com.saaf.milkshop.SaafMilkTestRunner')
    (OUT/f'credit-{len(steps):02d}-{role}-{name}-console.txt').write_text(output,encoding='utf-8')
    assert 'OK (1 test)' in output and 'FAILURES' not in output, 'Credit stage failed: '+role+'/'+name
    result=json.loads(shell(role,'exec-out','run-as',PACKAGE,'cat',f'files/credit-phone-{RUN}/result.json'))
    assert result['run']==RUN and result['stage']==name
    steps.append(result);save();print('PASS '+role+'/'+name,flush=True);return result
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
    data=shell(role,'exec-out','run-as',PACKAGE,'cat',f'files/credit-phone-{RUN}/{subdir}/{name}',binary=True)
    with tempfile.TemporaryDirectory(prefix='saaf-credit-') as temp:
        path=Path(temp)/'synthetic.db';path.write_bytes(data)
        db=sqlite3.connect(path);db.row_factory=sqlite3.Row
        try:
            rows={t:[dict(r) for r in db.execute('SELECT * FROM '+t)] for t in TABLES}
            metadata={t:sorted([dict(r) for r in db.execute('SELECT * FROM '+t)],key=lambda r:json.dumps(r,sort_keys=True)) for t in META}
        finally:db.close()
    return dict(rows=rows,metadata=metadata,balances=result['balances'],stock=result['stock'])
def compare(label,cash,debt,milk):
    a=snapshot('owner',stage('owner','status'));b=snapshot('counter',stage('counter','status'))
    ca,cb=canonical(a),canonical(b)
    differing=[t for t in ca if ca[t]!=cb[t]]+([] if label=='legacy' else [t for t in META if a['metadata'][t]!=b['metadata'][t]])
    if differing:
        (OUT/'credit-differences.json').write_text(json.dumps(dict(label=label,differing=differing,owner=ca,counter=cb,ownerMetadata=a['metadata'],counterMetadata=b['metadata']),indent=2)+'\n')
    assert not differing,'Credit convergence differs: '+','.join(differing)
    for v in [a,b]:
        assert v['balances']['Cash']==cash and v['balances']['Customer: Credit QA Customer']==debt and v['stock']['Milk']==milk
        assert not v['metadata']['credit_metadata_pending']
    assert next(r['account_id'] for r in a['rows']['accounts'] if r['category']=='Customer')!=next(r['account_id'] for r in b['rows']['accounts'] if r['category']=='Customer')
    comparisons.append(dict(label=label,tablesEqual=20 if label=='legacy' else 25,cash=cash,debt=debt,milk=milk,sha256=hashlib.sha256(json.dumps(dict(business=ca,metadata=a['metadata']),sort_keys=True).encode()).hexdigest()))
    save();print('PASS '+label+' table comparison',flush=True)
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
    for role in ['owner','cashier']:
        username='qa_credit_'+RUN+('_o' if role=='owner' else '_c');credential=f'{secrets.randbelow(10**15):015d}'
        auth=client.auth_create_user(username+'@'+client.fake_domain,credential);auth_id=(auth.get('user') or auth)['id']
        row=dict(id=str(uuid.uuid4()),business_id=business,auth_user_id=auth_id,username=username,display_name='Disposable Credit '+role,role=role,active=True,credential=credential)
        fixtures.append(row)
        client.rest_insert('staff',{k:v for k,v in row.items() if k!='credential'})
        client.rest_insert('staff_shops',dict(staff_id=row['id'],shop_id=SHOP))
    stage('owner','enroll',fixtures[0]);stage('owner','exchange')
    stage('counter','enroll',fixtures[1]);stage('counter','exchange');compare('legacy',10000,100,99)
    stage('owner','activate');stage('counter','exchange');compare('activated-legacy',10000,100,99)
    lost=stage('owner','terms_lost_ack');shell('owner','shell','am','force-stop',PACKAGE)
    resumed=stage('owner','exchange');assert resumed['pid']!=lost['pid']
    stage('counter','exchange');compare('monthly-after-lost-ack',10000,100,99)
    stage('counter','sale');stage('counter','exchange');stage('owner','exchange');compare('monthly-sale',10000,300,97)
    stage('owner','terms_weekly');stage('owner','exchange');stage('counter','exchange')
    stage('owner','correct_collect');stage('owner','exchange');stage('counter','exchange');compare('correction-payment',10050,300,96.5)
    stage('counter','old_client_refused');compare('old-client-refusal-preserves-finance',10050,300,96.5)
    stage('owner','flag_a');stage('owner','exchange');stage('counter','exchange');stage('owner','backup')
    stage('owner','flags_clear_and_b');stage('owner','exchange');stage('counter','exchange');compare('clear-and-concurrent-set',10050,300,96.5)
    stage('owner','restore');stage('owner','enroll',fixtures[0]);stage('owner','exchange');stage('counter','exchange')
    restored=stage('owner','status');assert restored['flags']==['set-b-'+RUN]
    compare('encrypted-restore-convergence',10050,300,96.5)
    stage('owner','exchange');stage('counter','exchange');compare('idempotent-repeat',10050,300,96.5)
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
    (OUT/'two-phone-cleanup.json').write_text(json.dumps(dict(run=RUN,cleanup=cleanup,before=baseline,after=after,existingRowsUnchanged=after==baseline),indent=2)+'\n')
    save();print(json.dumps(dict(comparisonsPassed=len(comparisons),failure=failure,cleanup=cleanup)),flush=True)
