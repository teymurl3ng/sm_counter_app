"""Only affected document/sharing and existing statement-preview methods, in isolated A06 QA."""
from pathlib import Path
import json
OUT=Path(__file__).resolve().parent
source=(OUT.parent/'credit-status-plan/run-finish.py').read_text(encoding='utf-8')
selection={
 'StatementDocumentTest':['measuredPlanKeepsEveryEntryAndRejectsUnsafeSizeOrTotals','pdfAndPngUseIdenticalPagesAndProduceReadableSamples','cacheCleanupKeepsFreshAndActiveAndDeletesOnlyAgedOwnedExports','providerGrantsOnlySelectedFilesAndRejectsSiblingAndOutsidePaths','cancelledGenerationReleasesItsLeaseAndDoesNotLeavePartialFiles','cancellationWhileReturningCompletedExportDiscardsUnclaimedLease'],
 'StatementSharingUiTest':['lightChooserHandoffHasTruthfulFeedbackAndStableChrome','darkLargeTextChooserHandoffHasTruthfulFeedbackAndStableChrome','cancelDoesNotExportAndMissingHandlerReportsFailure','changedFactsBeforeOrAfterRenderingRequireReviewInsteadOfSharing','nativeChooserCanBeDismissedWithoutSending'],
 'CustomerStatementUiTest':['statementLightPreviewAndReturnKeepChromeAndDisclosure','statementDarkLargeTextPreviewAndReturnKeepChromeAndDisclosure','periodFailureRetainsPreviousSnapshotAndAllowsRevert','multiplePagesAndRefreshClampToNewContent'],
}
line=next(s for s in source.splitlines() if s.startswith('SELECTION = '))
source=source.replace(line,'SELECTION = '+repr(selection))
(OUT/'export-selection.json').write_text(json.dumps(selection,indent=2)+'\n')
exec(compile(source,str(OUT/'run-export.py'),'exec'))
