"""Frozen remaining-screen acceptance only; one authorized disposable phone target."""
from pathlib import Path
import json,subprocess,sys
OUT=Path(__file__).resolve().parent
source=(OUT.parents[1]/'2026-10-15/credit-status-plan/run-finish.py').read_text(encoding='utf-8')
selection={'FollowUpIntegrationTest': ['workPlanning', 'invalidation', 'disconnectCleanup', 'checkedPolicy', 'destinationValidation', 'permissionDecisions'], 'FollowUpIntegrationUiTest': ['contextualAction', 'guardedOpen', 'repeatedOpen'], 'FollowUpAlertTest': ['deniedPermissionKeepsPersistentAttention', 'firstGrantAndRestartNeverRealert', 'backgroundRefreshNeedsNoLocalSessionAndCannotRetarget', 'workerRetryCancellationAndScheduleConstraints', 'notificationPrivacyAndPermissionChannelChecks'], 'FollowUpNavigationTest': ['boundWorkspaceRejectsChangedSession', 'connectedModeNeverFallsBackOnUnknown'], 'FollowUpReviewUiTest': ['darkLargeScheduleKeepsDraftAndChrome', 'sharedStartedClaimOpensReviewAndReturnsActivity'], 'FollowUpDataTest': ['runtimeRefreshesOnceAndChecksLocalRole', 'runtimeUnknownRefreshPreservesTokens']}
line=next(s for s in source.splitlines() if s.startswith('SELECTION = '))
source=source.replace(line,'SELECTION = '+repr(selection)).replace("label+'-console.txt'","label+'-instrumentation.txt'")
adb_path='C:/Users/Teymur/AppData/Local/Android/Sdk/platform-tools/adb.exe'
available=[s.split()[0] for s in subprocess.check_output([adb_path,'devices'],text=True).splitlines()[1:] if s.strip().endswith('device')]
serial=next((s for s in available if s=='R8VXA02SNKH'),None) or next((s for s in available if 'RFCX116CYYX' in s),None)
assert serial,'No authorized QA phone is connected'
model=subprocess.check_output([adb_path,'-s',serial,'shell','getprop','ro.product.model'],text=True).strip()
assert model in ('SM-S921W','SM-A065F','SM-A065M')
source=source.replace("SERIAL = 'R8VXA02SNKH'",'SERIAL = '+repr(serial)).replace("'device': 'A06'","'device': "+repr('S24' if model=='SM-S921W' else 'A06'))
source=source.replace('raw = result.stdout + result.stderr', '''raw = result.stdout + result.stderr
subprocess.run([ADB,'-s',SERIAL,'pull','/sdcard/Android/data/'+TARGET+'/files/followup-navigation',str(OUT/(label+'-captures'))],capture_output=True,text=True)''')
(OUT/'b-integration-selection.json').write_text(json.dumps(selection,indent=2)+'\n')
allowed=len(sys.argv)>3 and sys.argv[3]=='allow-notifications'
if allowed:
    assert sys.argv[2]=='notificationPrivacyAndPermissionChannelChecks'
    source=source.replace("adb('shell', 'pm', 'revoke', '--user', '0', TARGET, 'android.permission.POST_NOTIFICATIONS')", "adb('shell', 'pm', 'grant', '--user', '0', TARGET, 'android.permission.POST_NOTIFICATIONS')")
    source=source.replace("'notificationPermission': 'revoked for isolated package'", "'notificationPermission': 'granted only for isolated notifier; revoked after instrumentation'")
try:
    exec(compile(source,str(OUT/'run-b-integration.py'),'exec'))
finally:
    if allowed: adb('shell','pm','revoke','--user','0',TARGET,'android.permission.POST_NOTIFICATIONS')
