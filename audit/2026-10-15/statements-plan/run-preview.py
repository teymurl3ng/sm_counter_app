"""Reuse the already-reviewed isolated A06 driver, selecting only these affected methods."""
from pathlib import Path
import json
OUT=Path(__file__).resolve().parent
source=(OUT.parent/'credit-status-plan/run-finish.py').read_text(encoding='utf-8')
selection={
    'CustomerStatementTest':['openingSalesCorrectionsAndPaymentReconcileWithoutWriting','fractionalRowsUseCumulativePaisaAndDoNotLoseTheAggregate','advanceHistoricalPeriodsAndInvalidDatesStayDistinct','customerAndSessionPermissionsAreRechecked'],
    'CustomerStatementUiTest':['detailDisclosuresRetainPendingAttentionAndExistingOwnerActions','statementLightPreviewAndReturnKeepChromeAndDisclosure','statementDarkLargeTextPreviewAndReturnKeepChromeAndDisclosure','periodFailureRetainsPreviousSnapshotAndAllowsRevert','multiplePagesAndRefreshClampToNewContent'],
    'NativeOwnerCustomersScreenContractTest':['detailShowsFactsAndEdit','longCustomerDetailReflowsInDarkExtraLargeText'],
    'CreditFinishUiTest':['lightTermsReviewAndHistoryKeepChromeStable','darkLargeTextTermsReviewAndHistoryKeepChromeStable'],
}
line=next(s for s in source.splitlines() if s.startswith('SELECTION = '))
source=source.replace(line,'SELECTION = '+repr(selection))
(OUT/'preview-selection.json').write_text(json.dumps(selection,indent=2)+'\n')
exec(compile(source,str(OUT/'run-preview.py'),'exec'))
