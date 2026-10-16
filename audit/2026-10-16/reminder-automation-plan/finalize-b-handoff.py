"""Assemble exact scoped evidence; raw logs/screenshots/APKs remain local."""
from pathlib import Path
import hashlib,json,re
OUT=Path(__file__).resolve().parent
ROOT=OUT.parents[2]
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def write(name,value): (OUT/name).write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')
selection=json.loads((OUT/'b-handoff-selection.json').read_text())
expected={f'com.saaf.milkshop.{cls}#{method}' for cls,methods in selection.items() for method in methods}
raw=(OUT/'b-handoff-first-runner-log.txt').read_text(encoding='utf-8-sig')
def methods(event): return [f'{cls}#{method}' for method,cls in re.findall(rf'{event}: (\w+)\((com\.saaf\.milkshop\.\w+)\)',raw)]
assert set(methods('started'))==expected==set(methods('finished'))
assert len(methods('started'))==len(methods('finished'))==17
failed=set(methods('failed'))
assert failed=={'com.saaf.milkshop.FollowUpHandoffTest#changedFactsStaleHeadsOrPauseNeverLaunch'}
assert 'run finished: 17 tests, 1 failed, 0 ignored' in raw
first={'label':'b-handoff-first','device':'A06','target':'com.saaf.milkshop.qa.tests',
 'evidence':'Android TestRunner per-method started/finished/failure log and final run summary; host stdout writer failed on a locked path',
 'rawLogSha256':sha(OUT/'b-handoff-first-runner-log.txt'),'expected':17,'passed':16,
 'tests':[{'method':m,'outcome':'failed' if m in failed else 'passed'} for m in methods('finished')],
 'hostFailure':'PermissionError writing raw output to the same file as PowerShell redirection; corrected driver uses a separate instrumentation filename',
 'realShopPackageModified':False}
write('b-handoff-first-results.json',first)
latest={test['method']:{'outcome':test['outcome'],'evidence':'b-handoff-first-results.json'} for test in first['tests']}
hashes=set()
for filename in ['b-handoff-corrected-results.json','b-handoff-visual-results.json']:
    result=json.loads((OUT/filename).read_text())
    assert result['expected']==result['passed']==len(result['selection'])
    hashes.add(result['appApkSha256'])
    for test in result['tests']:
        assert test['code']==0
        method=f"{test['class']}#{test['test']}"
        assert method in expected
        latest[method]={'outcome':'passed','evidence':filename}
assert set(latest)==expected and all(t['outcome']=='passed' for t in latest.values())
apk=ROOT/'phase18/app/build/outputs/apk/debug/app-debug.apk'
assert hashes=={sha(apk)}
write('B_HANDOFF_VERIFICATION.json',{'date':'2026-10-16','baseline':'e4453c9','uniqueMethods':17,'latestPassed':17,
 'latest':latest,'appApkSha256':sha(apk),'firstRunResultSource':first['evidence'],
 'builds':['b-handoff-test-build-console.txt','b-handoff-fixture-build-console.txt'],
 'screenshots':{p.name:sha(p) for p in sorted((OUT/'b-handoff-captures').glob('*.png'))},
 'scope':{'syntheticTransport':True,'realRoomRuntimeSecurity':True,'ordinaryAppTouched':False,'cloudChanged':False,
          'realCustomerContact':False,'physicalTwoPhoneConcurrencyProven':False,'ordinaryActivation':False}})
files=[
 'phase18/app/src/main/java/com/saaf/milkshop/application/owner/CustomerSharedHandoff.kt',
 'phase18/app/src/main/java/com/saaf/milkshop/nativeui/owner/CustomerReminderScreen.kt',
 'phase18/app/src/main/java/com/saaf/milkshop/sync/FollowUpSessionAccess.kt',
 'phase18/app/src/main/java/com/saaf/milkshop/sync/SyncRuntime.kt',
 'phase18/app/src/androidTest/java/com/saaf/milkshop/FollowUpHandoffTest.kt',
 'phase18/app/src/androidTest/java/com/saaf/milkshop/CustomerReminderUiTest.kt']
write('B_HANDOFF_FILES.json',{'sha256':{p:sha(ROOT/p) for p in files}})
print('17 unique methods have latest passing evidence; synthetic capture hashes and six source hashes recorded.')
