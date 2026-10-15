from pathlib import Path
import json, subprocess, sys
OUT=Path(__file__).resolve().parent
ROOT=OUT.parents[2]
ADB='C:/Users/Teymur/AppData/Local/Android/Sdk/platform-tools/adb.exe'
serial='R8VXA02SNKH'
label=sys.argv[1]
methods=['refresh_nullTimezoneFallsBackToShopZone'] if label=='before' else [
    'refresh_nullTimezoneFallsBackToShopZone','refresh_absentTimezonesKeepExistingBusinessZone',
    'refresh_explicitTimezoneStillTakesPrecedence','signIn_allowed_carriesIdentityAndAnchorsTime',
    'signIn_disabledStaff_andNoStaffProfile_classifyDefinitively','signIn_unknownOutcomes_neverBecomeIdentityDecisions']
for rel in ['debug/app-debug.apk','androidTest/debug/app-debug-androidTest.apk']:
    result=subprocess.run([ADB,'-s',serial,'install','-r','--user','0',str(ROOT/'phase18/app/build/outputs/apk'/rel)],capture_output=True,text=True)
    assert result.returncode==0 and 'Success' in result.stdout,'Isolated QA installation failed'
runner='com.saaf.milkshop.qa.tests.test/com.saaf.milkshop.SaafMilkTestRunner'
registered=subprocess.check_output([ADB,'-s',serial,'shell','pm','list','instrumentation'],text=True)
assert runner in registered and '(target=com.saaf.milkshop.qa.tests)' in registered
classes=','.join('com.saaf.milkshop.SyncTransportTest#'+m for m in methods)
result=subprocess.run([ADB,'-s',serial,'shell','am','instrument','--user','0','-w','-r','-e','class',classes,'-e','saafSkipBootstrap','true',runner],capture_output=True,text=True,timeout=180)
raw=result.stdout+result.stderr
(OUT/(label+'-console.txt')).write_text(raw,encoding='utf-8')
state={}; tests=[]
for line in raw.splitlines():
    if line.startswith('INSTRUMENTATION_STATUS: '):
        key,_,value=line[len('INSTRUMENTATION_STATUS: '):].partition('=');state[key]=value
    if line.startswith('INSTRUMENTATION_STATUS_CODE: '):
        code=int(line.split(':',1)[1])
        if code!=1 and 'test' in state:tests.append({'test':state['test'],'code':code,'stack':state.get('stack')})
        state={}
summary={'label':label,'methods':methods,'tests':tests,'passed':sum(t['code']==0 for t in tests),'target':'com.saaf.milkshop.qa.tests'}
(OUT/(label+'-results.json')).write_text(json.dumps(summary,indent=2)+'\n')
print(json.dumps(summary,indent=2))
assert len(tests)==len(methods)
if label=='before': assert all(t['code']<0 for t in tests),'Baseline must reproduce the null-timezone error'
else: assert summary['passed']==len(methods),'Affected auth/refresh checks failed'
