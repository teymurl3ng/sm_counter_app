"""Combine only the frozen notification/lifecycle checks; retain actual APK provenance."""
from pathlib import Path
import hashlib,json
OUT=Path(__file__).resolve().parent
ROOT=OUT.parents[2]
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def write(name,value): (OUT/name).write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')
selection=json.loads((OUT/'b-integration-selection.json').read_text())
expected={f'com.saaf.milkshop.{c}#{m}' for c,methods in selection.items() for m in methods}
assert len(expected)==20
latest={};runs={}
for name in ['b-integration-first','b-integration-notification','b-integration-guard']:
    filename=name+'-results.json';result=json.loads((OUT/filename).read_text())
    assert result['expected']==len(result['selection'])==len(result['tests'])
    assert result['device']=='S24' and result['target']=='com.saaf.milkshop.qa.tests' and not result['realShopPackageModified']
    assert result['passed']==sum(t['code']==0 for t in result['tests'])
    for test in result['tests']:
        method=f"{test['class']}#{test['test']}";assert method in expected
        latest[method]={'passed':test['code']==0,'evidence':filename,'apkSha256':result['appApkSha256']}
    runs[name]={'expected':result['expected'],'passed':result['passed'],'apkSha256':result['appApkSha256'],'permission':result['notificationPermission']}
assert set(latest)==expected and all(t['passed'] for t in latest.values())
assert sha(ROOT/'phase18/app/build/outputs/apk/debug/app-debug.apk')==runs['b-integration-guard']['apkSha256']
capture=OUT/'b-integration-first-captures/notification-followups-return.png'
write('B_INTEGRATION_VERIFICATION.json',{'date':'2026-10-16','baseline':'18d9ede','uniqueMethods':20,'latestPassed':20,
    'latest':latest,'runs':runs,'visuallyReviewedCaptures':{str(capture.relative_to(OUT)):sha(capture)},
    'provenance':'Initial20/20 and separate granted-notification1/1 passed. Source review then added a second live draft-guard check when the asynchronous More load finishes, clearing abandoned requests after load failure; affected guarded/repeated navigation2/2 reran. Unchanged methods retain the actual earlier APK hash. The guarded helper and actual Customers-pane route are tested; no full real-account cold-authentication or elapsed scheduling claim.',
    'scope':{'deviceSdk':37,'ordinaryAppTouched':False,'cloudChanged':False,'realCustomerContact':False,
        'physicalTwoPhoneConcurrencyProven':False,'ordinaryColdNotificationTapAccepted':False,
        'elapsedPeriodicSchedulingProven':False,'shopActivated':False}})
paths=['phase18/app/src/main/java/com/saaf/milkshop/'+p for p in [
    'nativeui/NativePreviewActivity.kt','nativeui/FollowUpDestination.kt',
    'nativeui/admin/AdminPreviewHost.kt','nativeui/manager/ManagerPreviewHost.kt','nativeui/owner/OwnerPreviewHost.kt',
    'nativeui/owner/CustomerFollowUpAccess.kt','nativeui/owner/FollowUpWorkspace.kt','nativeui/owner/FollowUpNotificationSettings.kt',
    'nativeui/owner/NativeOwnerMoreViewModel.kt','nativeui/owner/OwnerCustomersScreen.kt',
    'sync/FollowUpAttentionStore.kt','sync/FollowUpWorker.kt','sync/FollowUpIntegration.kt','sync/SyncRuntime.kt',
    'sync/SyncScheduler.kt','sync/SyncShopBinding.kt','sync/SyncTokenStore.kt']]
paths+=['phase18/app/src/androidTest/java/com/saaf/milkshop/'+p for p in [
    'FollowUpNavigationUiTest.kt','FollowUpIntegrationTest.kt','FollowUpIntegrationUiTest.kt']]
write('B_INTEGRATION_FILES.json',{'sha256':{p:sha(ROOT/p) for p in paths}})
print('20 unique affected methods have latest passing evidence; source/test and viewed capture hashes retained.')
