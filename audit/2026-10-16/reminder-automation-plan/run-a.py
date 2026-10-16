"""Only piece A calculator, reader and injected native-workspace checks in isolated A06 QA."""
from pathlib import Path
import json
OUT=Path(__file__).resolve().parent
source=(OUT.parents[1]/'2026-10-15/credit-status-plan/run-finish.py').read_text(encoding='utf-8')
selection={'FollowUpRulesTest': ['sundayStatementsNeverReplaceAgreedDueDates', 'monthlyClampingAndAnchoredIntervalsSurviveMissedRunsAndLeapYears', 'reviewsMergeReasonsSuppressRepeatsAndKeepStableCycleIdentity', 'paymentPauseSnoozeActivationAndUnknownDebtStayTruthful', 'invalidFactsAndQuietHourBoundariesAreExplicit'], 'FollowUpReaderTest': ['canonicalFactsExcludePrivateNotesAndReflectPaymentsWithoutWrites', 'cashierAndExpiredSessionCannotReadFollowUps'], 'FollowUpWorkspaceUiTest': ['lightListReviewAndSettingsRetainChromeAndManualFallback', 'darkLargeTextListReviewAndSettingsRetainChromeAndManualFallback', 'invalidPolicyCannotSaveAndDirtyExitCanBeKeptOrDiscarded', 'customerMonthlyAndCustomValidationDoesNotEditPaymentTerms', 'failedLoadAndFailedSaveRetainFeedbackAndCanRetry', 'offlineLeadershipKeepsReviewAvailableWithoutSavingSharedState']}
line=next(s for s in source.splitlines() if s.startswith('SELECTION = '))
source=source.replace(line,'SELECTION = '+repr(selection))
(OUT/'a-selection.json').write_text(json.dumps(selection,indent=2)+'\n')
exec(compile(source,str(OUT/'run-a.py'),'exec'))
