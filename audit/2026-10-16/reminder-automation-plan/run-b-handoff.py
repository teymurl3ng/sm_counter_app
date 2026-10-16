"""Pinned B message adapter and touched reminder UI only, disposable A06 package."""
from pathlib import Path
import json
OUT=Path(__file__).resolve().parent
source=(OUT.parents[1]/'2026-10-15/credit-status-plan/run-finish.py').read_text(encoding='utf-8')
selection={
 'FollowUpHandoffTest':['currentStartSingleLaunchNoFinancialWrites','changedFactsStaleHeadsOrPauseNeverLaunch','busyForeignClaimNeverLaunch','lostAcquireOrStartNeverReplays','restoredRequestsStayOriginalAndRecoveryNeverLaunches','cancellationOrFailedHandlerLeavesStartedUncertainty','explicitResolutionPinsAttemptRoleAndRevisions','expiredSessionAndBindingChangeCannotLaunch'],
 'CustomerReminderUiTest':['lightReviewHandoffHasStableChromeAndTruthfulHistory','darkLargeTextReviewHandoffHasStableChromeAndTruthfulHistory','changedBalanceAndPhoneRequireFreshReviewBeforeAnyLaunch','missingPhoneOverrideKeyboardAndDiscardStayInsideTheDraft','unavailableWhatsAppAllowsDeliberateSmsAndGuardsBusyTaps','settledAdvanceAndExpiredAccessNeverOpenComposer','sharedGateOwnsLaunchForBothChannels','sharedUncertaintyRequiresExactConfirmedResolution','sharedMissingHandlerNeverSilentlySwitchesChannel']}
line=next(s for s in source.splitlines() if s.startswith('SELECTION = '))
source=source.replace(line,'SELECTION = '+repr(selection))
# Keep raw instrumentation separate from any caller's redirected driver console on Windows.
source=source.replace("label+'-console.txt'","label+'-instrumentation.txt'")
(OUT/'b-handoff-selection.json').write_text(json.dumps(selection,indent=2)+'\n')
exec(compile(source,str(OUT/'run-b-handoff.py'),'exec'))
