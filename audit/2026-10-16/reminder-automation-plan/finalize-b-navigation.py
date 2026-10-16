"""Combine only the frozen affected selection; preserve failures and per-run APK provenance."""
from pathlib import Path
import hashlib,json
OUT=Path(__file__).resolve().parent
ROOT=OUT.parents[2]
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def write(n,v): (OUT/n).write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
selection=json.loads((OUT/'b-navigation-selection.json').read_text())
expected={f'com.saaf.milkshop.{c}#{m}' for c,methods in selection.items() for m in methods}
assert len(expected)==21
latest={};runs={}
for name in ['b-navigation-data','b-navigation-ui','b-navigation-ui-fixed','b-navigation-corrected']:
    filename=name+'-results.json';r=json.loads((OUT/filename).read_text())
    assert len(r['selection'])==r['expected']==len(r['tests'])
    assert r['device']=='S24' and r['target']=='com.saaf.milkshop.qa.tests' and not r['realShopPackageModified']
    for t in r['tests']:
        method=f"{t['class']}#{t['test']}";assert method in expected
        latest[method]={'passed':t['code']==0,'evidence':filename,'apkSha256':r['appApkSha256']}
    runs[name]={'expected':r['expected'],'passed':r['passed'],'apkSha256':r['appApkSha256']}
assert set(latest)==expected and all(v['passed'] for v in latest.values())
assert runs['b-navigation-corrected']['apkSha256']==sha(ROOT/'phase18/app/build/outputs/apk/debug/app-debug.apk')
captures=[OUT/'b-navigation-ui-fixed-captures/owner-review-return.png',OUT/'b-navigation-corrected-captures/dark-large-review-return.png']
assert all(p.is_file() for p in captures)
write('B_NAVIGATION_VERIFICATION.json',{'date':'2026-10-16','baseline':'fef70de','uniqueMethods':21,'latestPassed':21,'latest':latest,'runs':runs,
 'screenshots':{str(p.relative_to(OUT)):sha(p) for p in captures},
 'provenance':'Data13 passed before a UI-only pending-customer guard. Initial UI8 failed in Espresso3.5.0 before screen work; pinned test-only3.7.0 then passed6/8. Corrected invalid statement fixture plus wait and checked both supported WhatsApp editions; only failedUI2 and touched factory1 reran, passing3/3. Each result retains its actual APK hash; no all-runs-identical claim.',
 'scope':{'deviceSdk':37,'ordinaryAppTouched':False,'cloudChanged':False,'realCustomerContact':False,'physicalTwoPhoneConcurrencyProven':False,'notificationLifecycleActivated':False}})
paths=['phase18/app/build.gradle.kts']
paths += ['phase18/app/src/main/java/com/saaf/milkshop/'+p for p in [
 'application/owner/CustomerReminderMode.kt','nativeui/NativePreviewActivity.kt','nativeui/admin/AdminPeopleScreen.kt','nativeui/admin/AdminPreviewHost.kt',
 'nativeui/manager/ManagerMoreScreen.kt','nativeui/manager/ManagerPreviewHost.kt','nativeui/owner/CustomerFollowUpAccess.kt','nativeui/owner/CustomerReminderScreen.kt',
 'nativeui/owner/CustomerStatementScreen.kt','nativeui/owner/FollowUpWorkspace.kt','nativeui/owner/NativeOwnerMoreViewModel.kt','nativeui/owner/OwnerCustomersScreen.kt',
 'nativeui/owner/OwnerMoreDependencies.kt','nativeui/owner/OwnerMoreScreen.kt','nativeui/owner/OwnerPreviewHost.kt']]
paths += ['phase18/app/src/androidTest/java/com/saaf/milkshop/'+p for p in ['FollowUpHandoffTest.kt','FollowUpNavigationTest.kt','FollowUpNavigationUiTest.kt']]
write('B_NAVIGATION_FILES.json',{'sha256':{p:sha(ROOT/p) for p in paths}})
print('21 unique affected methods have latest passing evidence; per-run APK and viewed capture hashes retained.')
