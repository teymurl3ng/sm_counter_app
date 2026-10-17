"""Apply the bounded, reviewed English copy map. No protocol or calculation changes."""
from pathlib import Path
import json

OUT = Path(__file__).resolve().parent
ROOT = OUT.parents[2]
BASE = ROOT / 'phase18/app/src/main/java/com/saaf/milkshop'
FILES = [
    'nativeui/owner/FollowUpWorkspace.kt',
    'nativeui/owner/FollowUpSharedReview.kt',
    'nativeui/owner/FollowUpReviewModels.kt',
    'nativeui/owner/FollowUpWorkspaceBinding.kt',
    'nativeui/owner/CustomerFollowUpScheduleScreen.kt',
    'nativeui/owner/CustomerReminderScreen.kt',
    'nativeui/owner/CustomerCreditPanel.kt',
    'nativeui/owner/CustomerStatementScreen.kt',
    'nativeui/owner/StatementExportUi.kt',
    'nativeui/owner/OwnerCustomersController.kt',
    'application/owner/CustomerCreditView.kt',
    'application/owner/CustomerSharedHandoff.kt',
    'application/owner/CustomerCommunication.kt',
    'application/owner/CustomerStatement.kt',
    'statements/ReminderIntents.kt',
]

# Whole quoted literals only, including literals nested inside Kotlin interpolation.
# Display labels never replace wire enums, mode keys, tags or stored audit values.
COPY = {
    'Held for Owner review. Accepted terms and flags remain in effect.': 'Needs Owner review. Current confirmed terms and review marks still apply.',
    'Pending server acceptance. Accepted terms and flags remain in effect.': 'Waiting for sync. Current confirmed terms and review marks still apply.',
    'Prepared $generated. This snapshot reflects local records for the selected period. Product names are current catalog labels.': 'Prepared $generated from records on this device for the selected period. Product names match the current product list.',
    'Customer statements require leadership access.': 'Sign in as Owner, Admin or Manager to prepare statements.',
    'Request review is unavailable.': 'Action review is unavailable.',
    'The request could not be checked. Try again.': 'The action could not be checked. Try again.',
    'This request is no longer present. Refresh first.': 'This action is no longer listed. Refresh first.',
    'Pending server acceptance': 'Waiting for sync',
    'Held for Owner review': 'Needs Owner review',
    'Future sale terms': 'Terms for new sales',
    'Change future sale terms': 'Change terms for new sales',
    'Flag for Owner review': 'Mark for Owner review',
    'Clear reviewed flags': 'Clear reviewed marks',
    'Owner review flag': 'Owner review mark',
    'Discard held terms proposal': 'Discard unsynced terms',
    'Discard held proposal?': 'Discard unsynced terms?',
    'Current debt only; this is not a lifetime reliability score. Sunday bills do not change payment deadlines.': "Based on unpaid sales, not the customer's full history. Sunday bills do not extend payment deadlines.",
    'Sync and enable credit terms after every posting phone is updated. Existing credit posting stays on its recorded default.': 'Update and sync every device used for sales before enabling this. Until then, credit sales keep the default payment terms.',
    'Pending actions do not replace accepted terms or flags. Sync, then refresh this customer.': 'These changes are not confirmed yet. Current terms and review marks still apply. Sync, then refresh this customer.',
    'No ledger movements yet.': 'No account activity yet.',
    'Owner review actions and accepted payment terms.': 'Payment terms and Owner review history.',
    'Applies to future new credit sales after server acceptance. Existing sales keep their agreed terms.': 'Applies to new credit sales after sync confirms the change. Existing sales keep their agreed terms.',
    'Calendar month means the same date next month, clamped to its final day. It is different from 30 days.': 'A calendar month ends on the same date next month, or its last day if that date is missing. This is different from 30 days.',
    'Clear only the flags reviewed now. Any unseen flag remains active. Overdue debt stays overdue.': 'Clear only the marks you reviewed. Any other marks stay active. Overdue debt stays overdue.',
    'This judgment is separate from the calculated debt status. Payments do not clear it automatically.': 'This review mark is separate from the debt status. It stays until cleared, even if the customer pays.',
    'Confirm every phone used to post sales has this compatible version and has synced. Old offline versions must stay out of use. The server will refuse new credit sales without recorded terms.': 'Update and sync every device used for sales. Stop using old versions, including offline devices. New credit sales must include recorded payment terms to sync.',
    'Only this unaccepted terms proposal is removed; its reviewed body remains in the security audit. Accepted agreements and history stay unchanged. You can then choose terms again.': 'Remove only this unsynced terms change. Its details stay in the security history. Confirmed terms and history stay unchanged. You can then choose terms again.',
    'Retry the same immutable change after reviewing sync. If terms changed elsewhere, discard the held terms proposal and choose again.': 'Check Sync, then retry this same change. If another device changed the terms, discard this unsynced change and choose again.',
    'All posting phones are updated': 'All sales devices are updated',
    'Terms proposal saved. Effective after sync acceptance.': 'Terms saved on this device. They apply after sync confirms the change.',
    'Held change queued for retry': 'Change ready to retry on the next sync',
    'Held terms proposal discarded': 'Unsynced terms discarded',
    'Review action saved. Pending sync acceptance.': 'Review saved on this device. Waiting for sync.',
    'Some debt cannot be dated or its agreement is unavailable. Review the account history.': 'Some debt has no confirmed date or payment terms. Check the account history.',
    'Next unpaid deadline: ${status.nextDeadline}.': 'Next payment due: ${status.nextDeadline}.',
    'Payment terms proposal': 'Terms change',

    'Shared follow-up activity': 'Follow-up activity',
    'Shared activity & request review': 'Activity & unfinished actions',
    'Set aside local request': 'Remove from this device',
    'Set aside local request?': 'Remove from this device?',
    'Set aside this local request?': 'Remove from this device?',
    'Set aside': 'Remove from this device',
    'Keep request': 'Keep for review',
    'Unfinished follow-up save': 'Save needs checking',
    'Shared state may have changed · Check the unfinished save': 'Changes may already be saved · Check the result',
    'The save is unfinished. Check its result before making another change.': 'The save was not confirmed. Check its result before making another change.',
    'Local unfinished save set aside. This does not undo a server change.': 'Removed from this device. Any change already synced stays in place.',
    'Save result checked. Current shared settings are shown.': 'Save checked. The latest synced settings are shown.',
    'The save result is still unresolved. Refresh or set aside only this local request.': 'The save is still unconfirmed. Refresh, or remove only this device\'s unfinished action.',
    'Local request set aside. Shared changes and message attempts are unchanged.': 'Removed from this device. Synced changes and message attempts stay in place.',
    'Request result checked. No app was opened and no alert was posted.': 'Action checked. No message app opened and no alert was shown.',
    'The request is still unfinished. Review the current shared state before trying again.': 'The action is still unconfirmed. Check the latest synced records before trying again.',
    'Check save result': 'Check save',
    'Check request result': 'Check action',
    'Automatic follow-ups are off.': 'Follow-up reminders are off.',
    'No current follow-up snapshot. Refresh to continue.': 'Follow-ups are not up to date. Refresh to continue.',
    'This only removes the unfinished request from this device. A change accepted by the server stays in place. Refresh to see its current state. It does not cancel or undo that change.': 'Remove only this device\'s unfinished save. Any change already synced stays in place. Refresh to see the latest result.',
    'Connect before a coordinated message handoff. You can still review the draft and statement.': 'Connect before opening a message app. You can still review the draft and statement offline.',
    "Review the statement here, or compose the reviewed message manually on the shop's separate phone. This app cannot confirm that sending.": "Review the statement here, or type and send the checked message on the shop's separate phone. This app cannot confirm whether you sent it.",
    'Owner attention': 'Owner reminders',
    'Automatic follow-ups': 'Follow-up reminders',
    'Notifications may run late. Customer records stay inside the app.': 'Alerts may arrive late. Customer details are not shown in alerts.',
    'Around (HH:mm)': 'Alert time (HH:mm)',
    'Shop statement weekday': 'Weekly statement day',
    'Overdue follow-up interval (1–30 days)': 'Repeat overdue reviews after (1–30 days)',
    'Automatic SMS: not configured. The tablet does not send through the separate shop phone.': 'Automatic SMS is not set up. To use the separate shop phone, type and send the message there.',
    'Use a 1–30 day interval, valid HH:mm times and different quiet-hour endpoints.': 'Enter 1–30 days and valid HH:mm times. Quiet hours must start and end at different times. Keep the alert time outside quiet hours.',
    'Only Owner can change the shop policy.': 'Only Owner can change these shop settings.',
    'Connect to save shared settings.': 'Connect to save settings for all devices.',
    'One device receives owner alerts. Synced devices need a connection to check message settings before opening a customer message, including when follow-ups are off. Drafts and statements remain available offline. The app cannot see payments on another offline device.': 'One device receives Owner alerts. Synced devices must connect before opening customer messages, even with reminders off. Drafts and statements work offline. Payments on another offline device may be missing.',
    'All posting devices have the compatible update': 'All sales devices are updated',
    'Owner needs to save shop follow-up settings before a customer schedule can be saved.': 'Ask Owner to save the shop follow-up settings first.',
    'Payment terms stay attached to each sale. This only changes statement review timing.': 'This changes when to review statements. Each sale keeps its payment deadline.',
    'Day of month (1–31; 0 for last day)': 'Day of month (1–31, or 0 for last day)',
    'Short months use their last day; the original day resumes next month.': 'If the day is missing, use the last day of that month. Later months keep the day you chose.',
    'Paused removes automated attention; manual account review stays available.': 'Paused customers do not appear in follow-up reminders. You can still open their accounts manually.',
    'Checking a request never opens a messaging app or posts an alert. Staff reports do not prove sending, delivery or payment.': 'Checking can finish an unconfirmed save. It never opens a message app or shows an alert. Staff reports do not confirm sending, delivery or payment.',
    'Unfinished requests': 'Unfinished actions',
    'No unfinished requests on this device.': 'No unfinished actions on this device.',
    'From a different connection. It cannot be retried here. Setting it aside removes only this local copy.': 'Saved under another connection. You cannot retry it here. Removing it clears only this device\'s copy.',
    'Message preparations & handoffs': 'Message attempts',
    'No active shared message attempts.': 'No message attempts waiting for review.',
    'A messaging app may have opened. This handoff stays unresolved until reviewed.': 'A message app may have opened. Review this attempt before starting another.',
    'A device has reserved message preparation. Nothing is confirmed sent.': 'A device is preparing this message. Sending is not confirmed.',
    'The preparing device or Owner must review this attempt.': 'Review this attempt on the device that started it, or ask Owner.',
    'Latest shared activity': 'Latest synced activity',
    'Latest 100 accepted actions. This is separate from preparation history saved on one phone.': 'Latest 100 actions confirmed by sync. Each device also has its own draft and share history.',
    'No shared activity yet.': 'No synced activity yet.',
    'Message preparation reserved': 'Message preparation started',
    'Message preparation extended': 'Message preparation time extended',
    'Message app handoff started': 'Message app may have opened',
    'Message handoff reviewed': 'Message attempt reviewed',
    'Handoff marked reviewed by staff': 'Message attempt marked reviewed by staff',
    'Staff reported no composer opened': 'Staff reported the message app did not open',
    'Owner alert reserved': 'Owner alert check recorded',
    'Request needs review': 'Action needs review',
    'Saved reminder state · Sync needed': 'Saved on this device · Sync needed',
    'Checked against shared records': 'Checked against synced records',
    'Check or set aside the unfinished save first.': 'Check the unfinished save first, or remove it from this device.',
    'Confirm that all posting devices are updated.': 'Confirm that all sales devices are updated.',
    'A message or notification request requires its separate review flow.': 'Review this message or alert under Activity & unfinished actions.',
    'Only Owner can review an unfinished shop-policy save.': 'Only Owner can check an unfinished shop-settings save.',
    'Connect to check the current shared state before reviewing this request.': 'Connect to check the latest synced records before reviewing this action.',
    'This request belongs to a different connection. It cannot be replayed here.': 'This action was saved under another connection. You cannot retry it here.',
    'Only Owner can review a shop-settings request.': 'Only Owner can check a shop-settings change.',
    'The save needs review. Check the unfinished request in Follow-up settings before changing it again.': 'The save was not confirmed. Check it in Follow-up settings before changing it again.',
    'Review unfinished requests under Customers → Follow-ups → Settings → Shared activity & request review.': 'Check unfinished actions under Customers → Follow-ups → Settings → Activity & unfinished actions.',
    'The server may already have accepted this request. This removes only the unfinished copy on this device. It does not undo a shared change, cancel a message preparation, resolve a handoff or confirm sending. The original connection or Owner may still need to review the shared attempt.': 'This action may already be saved online. Remove only this device\'s unfinished copy. Synced changes, message preparation and unreviewed attempts stay in place. This does not confirm sending. The original account or Owner may still need to review the attempt.',

    'Composer opened. Review and send in the other app; sending is not confirmed.': 'Message app opened. Review and send there. Sending is not confirmed.',
    'No composer opened. Review this handoff before trying another channel. Keep the draft or share the statement manually.': 'The message app did not open. Review this attempt before trying another option. You can keep the draft or share the statement.',
    'WhatsApp is unavailable. Choose SMS or enable WhatsApp, then try again.': 'WhatsApp is not available. Set it up, or choose another option.',
    'The composer could not open. Try again.': 'The message app could not open. Try again.',
    'Refresh the shared handoff before continuing.': 'Refresh this message attempt before continuing.',
    ' Local history could not be saved.': ' History on this device could not be saved.',
    'Handoff review updated. Sending or payment is not confirmed.': 'Message review saved. Sending or payment is not confirmed.',
    'The handoff could not be checked. Refresh before continuing.': 'The message attempt could not be checked. Refresh before continuing.',
    'Enter one full international number before opening a composer.': 'Enter one full number with + and the country code before opening a message app.',
    'I checked the recipient': 'I checked this number',
    'Shared message check': 'Check before messaging',
    'Checking current shared records…': 'Checking the latest synced records…',
    'Check unfinished request': 'Check unfinished action',
    'Review handoff': 'Review message attempt',
    'The server may already have accepted this request. This only removes the unfinished request on this phone. It does not cancel a preparation, undo a handoff or confirm sending. Current shared records will still block another message when needed.': 'This action may already be saved online. Remove only this device\'s unfinished copy. Message preparation and unreviewed attempts stay in place. This does not confirm sending.',
    'Review message handoff': 'Review message attempt',
    'A composer may already have opened. This app cannot confirm sending or delivery. Choose what you know before another attempt.': 'A message app may have opened. This app cannot confirm sending or delivery. Record what you know before trying again.',
    'No composer opened': 'Message app did not open',
    'I understand this is my report, not proof of sending or payment': 'This records my report. It does not confirm sending or payment.',
    'Preparation history': 'Draft & share history',
    'Latest 50 actions stored on this phone, including restored backups. This is not shared sending or delivery history.': 'Latest 50 actions saved on this device, including restored backups. This history is not shared across devices and does not confirm sending or delivery.',
    'No preparation actions recorded yet.': 'No drafts or share actions recorded yet.',
    'Composer opened': 'Message app opened',
    'Handoff failed': 'Could not confirm the app opened',
    'Customer communication requires leadership access.': 'Sign in as Owner, Admin or Manager to prepare customer messages.',
    'Known overdue amount (partial coverage)': 'Confirmed overdue amount (some records need checking)',
    'Rendering page…': 'Preparing page…',
    "Choose the recipient in Android's share menu. Opening it does not confirm sending.": 'Choose who to share with in the share menu. Opening it does not confirm sending.',
    'This period contains too many movements. Choose a smaller period.': 'There are too many entries for one statement. Choose a shorter period.',
    'Statement movements do not reconcile. Refresh or choose a smaller period; no statement was prepared.': 'The statement entries do not add up. Refresh or choose a shorter period. No statement was prepared.',
    'Connect to check shared follow-ups before opening a message.': 'Connect to check follow-up records before opening a message app.',
    'Shared follow-ups are unavailable. Refresh the shop settings.': 'Follow-up records are unavailable. Refresh the shop settings.',
    'Check the unfinished message request before trying again.': 'Check the unfinished message action before trying again.',
    'A message handoff needs review before another message can open.': 'Review the previous message attempt before opening another.',
    'Shared follow-ups are off. Review the shop settings before opening a coordinated message.': 'Follow-up reminders are off. Review the shop settings before opening a message app.',
    'This customer is paused or snoozed. Resume follow-ups before opening a coordinated message.': 'Follow-ups for this customer are paused or postponed. Resume them before opening a message app.',
    'The message request needs review. Check its result before trying again.': 'The message action needs checking. Check its result before trying again.',
    'Shared records changed. Review them before continuing.': 'Synced records changed. Review them before continuing.',
    'The handoff is uncertain. Review it before opening another message.': 'The last attempt is unconfirmed. Review it before opening another message.',
    'Records changed before opening. Review the started handoff before trying again.': 'Records changed before the message app opened. Review this attempt before trying again.',
    'Composer opened; sending is not confirmed. Review this handoff before opening another message.': 'Message app opened. Sending is not confirmed. Review this attempt before opening another message.',
    'This unfinished request is no longer present. Refresh first.': 'This unfinished action is no longer listed. Refresh first.',
    'This request belongs to another shop or installation.': 'This action belongs to another shop or device.',
    'This request belongs to a different task.': 'This action belongs to a different task.',
    'Confirm that setting aside a local request does not undo a server change.': 'Confirm that removing this copy does not undo a synced change.',
    'Confirm what is known about this handoff.': 'Confirm what you know about this message attempt.',
    'The handoff changed or requires Owner review. Refresh first.': 'The message attempt changed or needs Owner review. Refresh first.',
}

if __name__ == '__main__':
    paths = [BASE / f for f in FILES]
    originals = {p: p.read_text(encoding='utf-8') for p in paths}
    missing = [old for old in COPY if not any(('"'+old+'"') in text for text in originals.values())]
    assert not missing, f'Map no longer matches baseline: {missing}'
    changes = []
    for p, original in originals.items():
        updated = original
        for old, new in COPY.items():
            needle = '"'+old+'"'
            count = updated.count(needle)
            if count:
                updated = updated.replace(needle, '"'+new+'"')
                changes.append({'file': str(p.relative_to(ROOT)).replace('\\','/'), 'old': old, 'new': new, 'count': count})
        if updated != original:
            p.write_text(updated, encoding='utf-8', newline='\n')
    tests = []
    for p in (ROOT/'phase18/app/src/androidTest').rglob('*.kt'):
        original = p.read_text(encoding='utf-8'); updated = original
        for old,new in COPY.items():
            updated = updated.replace('"'+old+'"', '"'+new+'"')
        if updated != original:
            p.write_text(updated, encoding='utf-8', newline='\n');tests.append(str(p.relative_to(ROOT)).replace('\\','/'))
    (OUT/'COPY_MAP.json').write_text(json.dumps({'changes': changes, 'testSelectorsUpdated': tests}, indent=2, ensure_ascii=False)+'\n', encoding='utf-8')
    print(f'{len(changes)} mapped replacements in {len(set(x["file"] for x in changes))} source files; {len(tests)} affected test files')
