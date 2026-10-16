"""Only manual reminder/history and directly touched preview/share checks, in isolated A06 QA."""
from pathlib import Path
import json
OUT=Path(__file__).resolve().parent
source=(OUT.parent/'credit-status-plan/run-finish.py').read_text(encoding='utf-8')
selection={'CustomerCommunicationTest': ['reminderUsesCurrentBalanceAndKnownOverdueWithoutLeakingPrivateFacts', 'explicitPhoneAndUnicodeMessagesAreEncodedForOneRecipientOnly', 'historyIsMinimalIdempotentCustomerBoundedAndBackupCompatible', 'permissionsAndActionVocabularyAreEnforced'], 'CustomerReminderUiTest': ['lightReviewHandoffHasStableChromeAndTruthfulHistory', 'darkLargeTextReviewHandoffHasStableChromeAndTruthfulHistory', 'changedBalanceAndPhoneRequireFreshReviewBeforeAnyLaunch', 'missingPhoneOverrideKeyboardAndDiscardStayInsideTheDraft', 'unavailableWhatsAppAllowsDeliberateSmsAndGuardsBusyTaps', 'settledAdvanceAndExpiredAccessNeverOpenComposer'], 'CustomerStatementUiTest': ['statementReminderAndHistoryKeepOneHostAndReturnToCustomer', 'statementLightPreviewAndReturnKeepChromeAndDisclosure', 'statementDarkLargeTextPreviewAndReturnKeepChromeAndDisclosure', 'periodFailureRetainsPreviousSnapshotAndAllowsRevert', 'multiplePagesAndRefreshClampToNewContent'], 'StatementSharingUiTest': ['observableShareHistoryDoesNotTurnPostLaunchAuditFailureIntoHandoffFailure', 'lightChooserHandoffHasTruthfulFeedbackAndStableChrome', 'darkLargeTextChooserHandoffHasTruthfulFeedbackAndStableChrome', 'cancelDoesNotExportAndMissingHandlerReportsFailure', 'changedFactsBeforeOrAfterRenderingRequireReviewInsteadOfSharing', 'nativeChooserCanBeDismissedWithoutSending']}
line=next(s for s in source.splitlines() if s.startswith('SELECTION = '))
source=source.replace(line,'SELECTION = '+repr(selection))
(OUT/'manual-selection.json').write_text(json.dumps(selection,indent=2)+'\n')
exec(compile(source,str(OUT/'run-manual.py'),'exec'))
