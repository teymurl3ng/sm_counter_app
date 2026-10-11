"""Attach real contract-test references. COVERED describes test coverage, not execution PASS."""
from pathlib import Path
import re
root=Path(__file__).resolve().parents[3]
path=root/'phase18/qa/native_control_inventory.tsv'
mapping={
 'owner_work_record_drawing':'drawingOpensConnectedDestination',
 'owner_drawing_entry_root':'drawingAmountAndNoteSubmitExactlyOnce',
 'drawing_record':'drawingAmountAndNoteSubmitExactlyOnce',
 'drawing_posting_closed':'closedDrawingCannotSubmit',
 'drawing_form':'drawingAmountAndNoteSubmitExactlyOnce',
 'drawing_amount_input':'drawingAmountAndNoteSubmitExactlyOnce',
 'drawing_note_toggle':'drawingAmountAndNoteSubmitExactlyOnce',
 'drawing_note_input':'drawingAmountAndNoteSubmitExactlyOnce',
 'drawing_error':'drawingErrorRemainsRecoverable',
 'drawing_success':'drawingSuccessOffersAnotherAndBack',
 'drawing_another':'drawingSuccessOffersAnotherAndBack',
 'drawing_back_to_work':'drawingSuccessOffersAnotherAndBack',
 'owner_records_correction_discard_dialog':'correctionDiscardCanKeepOrConfirmNavigation',
 'owner_records_correction_discard_confirm':'correctionDiscardCanKeepOrConfirmNavigation',
 'owner_records_correction_discard_keep':'correctionDiscardCanKeepOrConfirmNavigation',
 'owner_costing_opening_discard':'openingDiscardCanKeepOrConfirmNavigation',
 'owner_costing_opening_keep':'openingDiscardCanKeepOrConfirmNavigation',
 'owner_backup_connect_discard':'connectDiscardCanKeepOrConfirmNavigation',
 'owner_backup_connect_keep':'connectDiscardCanKeepOrConfirmNavigation',
 'owner_more_sync':'syncRowOpensFromMenu',
 'owner_sync_root':'syncScreenShowsStatusOrEnroll',
 'owner_sync_loading':'loadingShowsWhileStatusLoads',
 'owner_sync_status':'enrolledShowsServerShopSession',
 'owner_sync_now':'syncNowPullsAndPushes',
 'owner_sync_refresh':'refreshReloadsStatus',
 'owner_sync_unenroll':'unenrollOpensConfirm',
 'owner_sync_unenroll_confirm':'confirmStopsSyncing',
 'owner_sync_holds':'restoredHoldsListedForReview',
 'owner_sync_hold_keep':'keepMineReleasesEntryToPush',
 'owner_sync_hold_take':'takeServerDropsEntrySoParkApplies',
 'owner_sync_enroll':'enrollConnectsPhone',
 'owner_sync_username':'enrollConnectsPhone',
 'owner_sync_credential':'enrollConnectsPhone',
 'owner_sync_environment':'syncEnvironmentChooserChangesEnrollmentTarget',
 'owner_sync_shop_picker':'shopChoicesListedWhenAmbiguous',
 'owner_sync_shop_pick':'shopPickCompletesEnrollment',
 'owner_sync_owner_picker':'syncOwnerChooserUsesTheSelectedLocalOwner',
 'owner_sync_owner_pick':'syncOwnerChooserUsesTheSelectedLocalOwner',
 'owner_sync_retry_load':'coldSyncFailureOffersWorkingRetry',
 'owner_sync_notice':'noticeShowsResultSummary',
 'owner_sync_error':'errorShowsFailureReason',
 'cashier_payment_account_selector':'salePaymentPickerCancelsAndChangesDestination',
 'cashier_payment_cancel':'salePaymentPickerCancelsAndChangesDestination',
 'cashier_sale_cheque':'saleChequeNextReturnsToForm',
 'cashier_sale_next':'saleChequeNextReturnsToForm',
 'cashier_sale_note':'saleWholesaleRateAndNoteStayEditable',
 'cashier_wholesale_rate':'saleWholesaleRateAndNoteStayEditable',
}
tests='\n'.join(p.read_text(encoding='utf-8') for p in (root/'phase18/app/src/androidTest').rglob('*.kt'))
for name in set(mapping.values()):
    assert re.search(r'fun\s+'+re.escape(name)+r'\s*\(',tests),f'Missing real test: {name}'
lines=path.read_text(encoding='utf-8').splitlines(); seen=set(); output=[]
for line in lines:
    parts=line.split('\t')
    if len(parts)==7 and parts[1] in mapping:
        seen.add(parts[1]); parts[4]=mapping[parts[1]]; parts[6]='COVERED'; line='\t'.join(parts)
    output.append(line)
for tag in mapping.keys()-seen:
    screen='owner' if tag.startswith('drawing_') else 'ownermore'
    kind='VIEW' if tag.endswith(('root','form','error','success','closed','picker')) else 'ACTION'
    output.append('\t'.join([screen,tag,tag,kind,mapping[tag],kind,'COVERED']))
path.write_text('\n'.join(output)+'\n',encoding='utf-8')
print(f'Updated {len(mapping)} control references; device execution is tracked separately.')
