"""Only B alert/count/worker/notifier and touched reader/runtime methods, isolated A06."""
from pathlib import Path
import json,sys
OUT=Path(__file__).resolve().parent
source=(OUT.parents[1]/'2026-10-15/credit-status-plan/run-finish.py').read_text(encoding='utf-8')
selection={
 'FollowUpAlertTest':['quietBoundariesAndMissedDaysUseCurrentShopDate','pausedSnoozedAndSettledNeverReserve','onlyCurrentPrimaryActorMayAlert','deniedPermissionKeepsPersistentAttention','firstGrantAndRestartNeverRealert','unknownAndRestoredDigestNeverPost','staleLedgerAndRevocationNeverPublishCurrentCount','backgroundRefreshNeedsNoLocalSessionAndCannotRetarget','attentionRestartAndFailureKeepStaleCount','workerRetryCancellationAndScheduleConstraints','notificationPrivacyAndPermissionChannelChecks'],
 'FollowUpAttentionUiTest':['normalAttentionRetainsChromeAcrossZeroAndStaleUpdates','darkLargeAttentionRetainsChromeAcrossZeroAndStaleUpdates'],
 'FollowUpReaderTest':['canonicalFactsExcludePrivateNotesAndReflectPaymentsWithoutWrites','cashierAndExpiredSessionCannotReadFollowUps'],
 'FollowUpDataTest':['runtimeRefreshesOnceAndChecksLocalRole','runtimeUnknownRefreshPreservesTokens']}
line=next(s for s in source.splitlines() if s.startswith('SELECTION = '))
source=source.replace(line,'SELECTION = '+repr(selection))
(OUT/'b-alert-selection.json').write_text(json.dumps(selection,indent=2)+'\n')
allowed=len(sys.argv)>3 and sys.argv[3]=='allow-notifications'
if allowed:
    assert sys.argv[2]=='notificationPrivacyAndPermissionChannelChecks'
    source=source.replace("adb('shell', 'pm', 'revoke', '--user', '0', TARGET, 'android.permission.POST_NOTIFICATIONS')",
                          "adb('shell', 'pm', 'grant', '--user', '0', TARGET, 'android.permission.POST_NOTIFICATIONS')")
    source=source.replace("'notificationPermission': 'revoked for isolated package'","'notificationPermission': 'granted only for isolated notifier; revoked after instrumentation'")
try:
    exec(compile(source,str(OUT/'run-b-alert.py'),'exec'))
finally:
    if allowed: adb('shell','pm','revoke','--user','0',TARGET,'android.permission.POST_NOTIFICATIONS')
