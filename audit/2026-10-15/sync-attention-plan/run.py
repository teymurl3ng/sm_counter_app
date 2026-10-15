from pathlib import Path
import hashlib, json, subprocess, sys

OUT = Path(__file__).resolve().parent
ROOT = OUT.parents[2]
ADB = 'C:/Users/Teymur/AppData/Local/Android/Sdk/platform-tools/adb.exe'
SERIAL = 'R8VXA02SNKH'
TARGET = 'com.saaf.milkshop.qa.tests'
RUNNER = TARGET + '.test/com.saaf.milkshop.SaafMilkTestRunner'
SELECTION = {
    'SyncReviewAttentionTest': [
        'committedHoldsEmitAndSurviveFileReopen', 'outboxParkAndHistoricalConflictAreNotReviewHolds',
        'readFailureRetainsCountAndCancellationIsNotFailure',
        'ownerReadWorksWithoutEnrollmentButExpiredAndCashierSessionsAreDenied'],
    'OwnerSyncAttentionUiTest': [
        'badgeLightDefaultKeepsChromeStable', 'badgeDarkDoubleTextKeepsChromeStable',
        'disconnectedHoldsStayVisibleAndCannotResolve',
        'sessionReplacementCancelsOldObservationAndFailureCanRetry',
        'localHoldUpdatesReachOpenReviewAndResolveWithoutReload'],
    'NativeOwnerMoreScreenContractTest': [
        'syncMenuShowsPersistentReviewCountAndKeepsExistingRoute', 'syncRowOpensFromMenu',
        'restoredHoldsListedForReview', 'keepMineReleasesEntryToPush', 'takeServerDropsEntrySoParkApplies'],
    'SyncRepairBoundaryTest': ['failedTakeServerKeepsReviewAndLocalEditThenSuccessfulApplyResolvesBoth'],
    'NativeManagementPresentationTest': ['syncLightDefault', 'syncDarkExtraLarge'],
}

def adb(*args, **kwargs):
    return subprocess.run([ADB, '-s', SERIAL, *args], capture_output=True, text=True, check=True, **kwargs)

label = sys.argv[1] if len(sys.argv) > 1 else 'first'
selected = [f'com.saaf.milkshop.{name}#{method}' for name, methods in SELECTION.items() for method in methods]
if len(sys.argv) > 2:
    requested = set(sys.argv[2].split(','))
    selected = [name for name in selected if name.split('#')[1] in requested]
    assert len(selected) == len(requested), 'Unknown or duplicated retry selection'

for rel in ['debug/app-debug.apk', 'androidTest/debug/app-debug-androidTest.apk']:
    apk = ROOT/'phase18/app/build/outputs/apk'/rel
    assert apk.is_file()
    aapt = Path('C:/Users/Teymur/AppData/Local/Android/Sdk/build-tools/36.0.0/aapt.exe')
    badging = subprocess.check_output([str(aapt), 'dump', 'badging', str(apk)], text=True)
    expected = TARGET if rel.startswith('debug/') else TARGET + '.test'
    assert f"package: name='{expected}'" in badging, 'Refusing a non-isolated APK'
    assert 'Success' in adb('install', '-r', '--user', '0', str(apk)).stdout

assert f'{RUNNER} (target={TARGET})' in adb('shell', 'pm', 'list', 'instrumentation').stdout
# A denied notification permission is deliberate test setup on the isolated package only.
adb('shell', 'pm', 'revoke', '--user', '0', TARGET, 'android.permission.POST_NOTIFICATIONS')
result = adb('shell', 'am', 'instrument', '--user', '0', '-w', '-r', '-e', 'class', ','.join(selected),
             '-e', 'saafSkipBootstrap', 'true', RUNNER, timeout=480)
raw = result.stdout + result.stderr
(OUT/(label+'-console.txt')).write_text(raw, encoding='utf-8')
state, tests = {}, []
for line in raw.splitlines():
    if line.startswith('INSTRUMENTATION_STATUS: '):
        key, _, value = line[len('INSTRUMENTATION_STATUS: '):].partition('=')
        state[key] = value
    if line.startswith('INSTRUMENTATION_STATUS_CODE: '):
        code = int(line.split(':', 1)[1])
        if code != 1 and 'test' in state:
            tests.append({'class': state.get('class'), 'test': state['test'], 'code': code, 'stack': state.get('stack')})
        state = {}

summary = {'label': label, 'device': 'A06', 'target': TARGET, 'selection': selected, 'tests': tests,
           'passed': sum(test['code'] == 0 for test in tests), 'expected': len(selected),
           'notificationPermission': 'revoked for isolated package', 'realShopPackageModified': False,
           'appApkSha256': hashlib.sha256((ROOT/'phase18/app/build/outputs/apk/debug/app-debug.apk').read_bytes()).hexdigest()}
(OUT/(label+'-results.json')).write_text(json.dumps(summary, indent=2)+'\n')
print(json.dumps(summary, indent=2))
assert len(tests) == len(selected) and summary['passed'] == len(selected), 'Affected checks did not all pass'
