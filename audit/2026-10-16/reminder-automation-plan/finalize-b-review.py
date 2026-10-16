"""Verify the frozen remaining-screen selection and retain actual per-run provenance."""
from pathlib import Path
import hashlib, json
OUT = Path(__file__).resolve().parent
ROOT = OUT.parents[2]
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def write(name, value): (OUT/name).write_text(json.dumps(value, indent=2)+'\n', encoding='utf-8')
selection = json.loads((OUT/'b-review-selection.json').read_text())
expected = {f'com.saaf.milkshop.{c}#{m}' for c, methods in selection.items() for m in methods}
assert len(expected) == 22
latest, runs = {}, {}
for name in ['b-review-first', 'b-review-corrected', 'b-review-offline']:
    filename = name+'-results.json'
    result = json.loads((OUT/filename).read_text())
    assert len(result['selection']) == result['expected'] == len(result['tests'])
    assert result['device'] == 'S24' and result['target'] == 'com.saaf.milkshop.qa.tests'
    assert not result['realShopPackageModified']
    assert result['passed'] == sum(t['code'] == 0 for t in result['tests'])
    for test in result['tests']:
        method = f"{test['class']}#{test['test']}"
        assert method in expected
        latest[method] = {'passed': test['code'] == 0, 'evidence': filename, 'apkSha256': result['appApkSha256']}
    runs[name] = {'expected': result['expected'], 'passed': result['passed'], 'apkSha256': result['appApkSha256']}
assert set(latest) == expected and all(v['passed'] for v in latest.values())
assert runs['b-review-offline']['apkSha256'] == sha(ROOT/'phase18/app/build/outputs/apk/debug/app-debug.apk')
captures = [OUT/p for p in ['b-review-offline-captures/shared-restored-request.png',
    'b-review-corrected-captures/schedule-dark-large-draft.png', 'b-review-offline-captures/settled-schedule-return.png']]
assert all(p.is_file() for p in captures)
write('B_REVIEW_VERIFICATION.json', {'date': '2026-10-16', 'baseline': 'b4fe77c',
    'uniqueMethods': 22, 'latestPassed': 22, 'latest': latest, 'runs': runs,
    'visuallyReviewedCaptures': {str(p.relative_to(OUT)): sha(p) for p in captures},
    'provenance': 'First22 passed21; dark schedule failure was a label+editable-text assertion although EditableText retained14. Corrected exact editable-text selector passed1/1 on identical production APK. Final two UI error-state disabling conditions and folded failed-refresh assertion rebuilt; only affected direct-schedule/shared-review methods reran2/2. Unchanged checks retain earlier APK provenance; no identical-final-APK full-run claim.',
    'scope': {'deviceSdk': 37, 'ordinaryAppTouched': False, 'cloudChanged': False,
        'realCustomerContact': False, 'physicalTwoPhoneConcurrencyProven': False,
        'notificationLifecycleActivated': False}})
paths = ['phase18/app/src/main/java/com/saaf/milkshop/'+p for p in [
    'sync/FollowUpWire.kt', 'nativeui/owner/CustomerFollowUpAccess.kt',
    'nativeui/owner/CustomerStatementScreen.kt', 'nativeui/owner/FollowUpWorkspace.kt',
    'nativeui/owner/FollowUpWorkspaceBinding.kt', 'nativeui/owner/OwnerCustomersScreen.kt',
    'nativeui/owner/FollowUpReviewModels.kt', 'nativeui/owner/FollowUpSharedReview.kt',
    'nativeui/owner/CustomerFollowUpScheduleScreen.kt']]
paths += ['phase18/app/src/androidTest/java/com/saaf/milkshop/'+p for p in [
    'FollowUpWorkspaceBindingTest.kt', 'FollowUpNavigationUiTest.kt',
    'FollowUpReviewBindingTest.kt', 'FollowUpReviewUiTest.kt']]
write('B_REVIEW_FILES.json', {'sha256': {p: sha(ROOT/p) for p in paths}})
print('22 unique affected methods have latest passing evidence; 13 source/test hashes retained.')
