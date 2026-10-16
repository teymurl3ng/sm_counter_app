"""Pinned connected workspace and affected runtime/presentation checks only."""
from pathlib import Path
import json
OUT=Path(__file__).resolve().parent
source=(OUT.parents[1]/'2026-10-15/credit-status-plan/run-finish.py').read_text(encoding='utf-8')
selection={
 'FollowUpWorkspaceBindingTest':['acceptedLoadUsesActualReaderAndServerSchedule','policyAndPrimaryRequireExplicitOwnerConfirmation','customerCommandsKeepFrozenRevisionsAndFinancialRecords','staleViewOrLocalFinancialChangesCannotSubmitReview','uncertainSaveRecoversOriginalIdAndNeverDuplicates','abandonedMetadataDoesNotUndoRemoteStateOrHandleExternalRequests','bindingChangeAndLogoutCannotPublishOrRetarget','unavailableDataStaysStaleAndCannotSave'],
 'FollowUpWorkspaceUiTest':['lightListReviewAndSettingsRetainChromeAndManualFallback','darkLargeTextListReviewAndSettingsRetainChromeAndManualFallback','invalidPolicyCannotSaveAndDirtyExitCanBeKeptOrDiscarded','customerMonthlyAndCustomValidationDoesNotEditPaymentTerms','failedLoadAndFailedSaveRetainFeedbackAndCanRetry','offlineLeadershipKeepsReviewAvailableWithoutSavingSharedState','primaryTransferRequiresExplicitCompatibleDeviceConfirmation','unfinishedSaveBlocksEditsAndSetAsideExplainsServerOutcome'],
 'FollowUpDataTest':['runtimeRefreshesOnceAndChecksLocalRole','runtimeUnknownRefreshPreservesTokens']}
line=next(s for s in source.splitlines() if s.startswith('SELECTION = '))
source=source.replace(line,'SELECTION = '+repr(selection))
(OUT/'b-workspace-selection.json').write_text(json.dumps(selection,indent=2)+'\n')
exec(compile(source,str(OUT/'run-b-workspace.py'),'exec'))
