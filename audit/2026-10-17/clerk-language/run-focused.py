"""Pinned wording acceptance. Only the disposable QA package is installed/reset."""
from pathlib import Path
import subprocess, json

OUT = Path(__file__).resolve().parent
ROOT = OUT.parents[2]
SELECTION = {
    'CreditFinishUiTest': ['lightTermsReviewAndHistoryKeepChromeStable', 'darkLargeTextTermsReviewAndHistoryKeepChromeStable'],
    'CreditFinishTest': ['pendingAndRestoredClearsCannotHideAcceptedFlag'],
    'FollowUpWorkspaceUiTest': ['lightListReviewAndSettingsRetainChromeAndManualFallback', 'darkLargeTextListReviewAndSettingsRetainChromeAndManualFallback', 'primaryTransferRequiresExplicitCompatibleDeviceConfirmation', 'unfinishedSaveBlocksEditsAndSetAsideExplainsServerOutcome'],
    'FollowUpReviewUiTest': ['darkLargeScheduleKeepsDraftAndChrome', 'sharedActivityAndRecoveryNeverLaunch', 'foreignRestoreRequiresExplicitOwnerSetAside'],
    'CustomerReminderUiTest': ['lightReviewHandoffHasStableChromeAndTruthfulHistory', 'darkLargeTextReviewHandoffHasStableChromeAndTruthfulHistory', 'sharedUncertaintyRequiresExactConfirmedResolution', 'sharedMissingHandlerNeverSilentlySwitchesChannel'],
    'StatementSharingUiTest': ['lightChooserHandoffHasTruthfulFeedbackAndStableChrome', 'darkLargeTextChooserHandoffHasTruthfulFeedbackAndStableChrome', 'observableShareHistoryDoesNotTurnPostLaunchAuditFailureIntoHandoffFailure'],
    'CustomerStatementUiTest': ['statementReminderAndHistoryKeepOneHostAndReturnToCustomer', 'statementDarkLargeTextPreviewAndReturnKeepChromeAndDisclosure'],
    'FollowUpReviewBindingTest': ['activityMapsActorsAndNeverClaimsDelivery'],
    'FollowUpWorkspaceBindingTest': ['uncertainSaveRecoversOriginalIdAndNeverDuplicates'],
    'CustomerCommunicationTest': ['reminderUsesCurrentBalanceAndKnownOverdueWithoutLeakingPrivateFacts'],
    'FollowUpHandoffTest': ['restoredRequestsStayOriginalAndRecoveryNeverLaunches', 'cancellationOrFailedHandlerLeavesStartedUncertainty'],
}
assert sum(map(len, SELECTION.values())) == 24
source = (ROOT/'audit/2026-10-15/credit-status-plan/run-finish.py').read_text(encoding='utf-8')
line = next(s for s in source.splitlines() if s.startswith('SELECTION = '))
source = source.replace(line, 'SELECTION = '+repr(SELECTION)).replace("label+'-console.txt'", "label+'-instrumentation.txt'")
adb = 'C:/Users/Teymur/AppData/Local/Android/Sdk/platform-tools/adb.exe'
available = [s.split()[0] for s in subprocess.check_output([adb,'devices'],text=True).splitlines()[1:] if s.strip().endswith('device')]
serial = next((s for s in available if s=='R8VXA02SNKH'),None) or next((s for s in available if 'RFCX116CYYX' in s),None)
assert serial, 'No authorized QA phone connected'
lock = subprocess.check_output([adb,'-s',serial,'shell','dumpsys','window','policy'],text=True)
assert 'showing=false' in lock and 'screenState=SCREEN_STATE_ON' in lock, 'Unlock the QA phone first'
model = subprocess.check_output([adb,'-s',serial,'shell','getprop','ro.product.model'],text=True).strip()
assert model in ('SM-S921W','SM-A065F','SM-A065M')
source = source.replace("SERIAL = 'R8VXA02SNKH'", 'SERIAL = '+repr(serial)).replace("'device': 'A06'", "'device': "+repr(model))
source = source.replace('raw = result.stdout + result.stderr', '''raw = result.stdout + result.stderr
(OUT/(label+'-captures')).mkdir(exist_ok=True)
for folder in ['followup-navigation','followup-a','reminder-ui','credit-finish','statement-preview','statement-export-ui']:
    subprocess.run([ADB,'-s',SERIAL,'pull','/sdcard/Android/data/'+TARGET+'/files/'+folder,str(OUT/(label+'-captures')/folder)],capture_output=True,text=True)
''')
(OUT/'SELECTION.json').write_text(json.dumps(SELECTION,indent=2)+'\n')
exec(compile(source,str(OUT/'run-focused.py'),'exec'))
