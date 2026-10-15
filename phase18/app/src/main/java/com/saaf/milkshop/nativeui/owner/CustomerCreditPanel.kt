package com.saaf.milkshop.nativeui.owner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.owner.*
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.nativeui.common.*

@Composable
internal fun CustomerCreditStatusText(view: CustomerCreditView) {
    val urgent=view.status.kind in setOf(CreditStatusKind.BAD,CreditStatusKind.LATE) || view.status.ownerFlagged
    Text(view.label,style=MaterialTheme.typography.labelLarge,color=if(urgent) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.primary,modifier=Modifier.testTag("credit-status"))
}

@Composable
internal fun CustomerCreditPanel(detail: NativeOwnerCustomerDetail, owner: Boolean, busy: Boolean,
    onAction: (CustomerCreditCommand)->Unit) {
    CustomerCreditBody(detail, owner, busy, CustomerCreditContent.All, onAction)
}

internal enum class CustomerCreditContent { All, Summary, Management }

@Composable
internal fun CustomerCreditBody(detail: NativeOwnerCustomerDetail, owner: Boolean, busy: Boolean,
    content: CustomerCreditContent, onAction: (CustomerCreditCommand)->Unit) {
    val credit=detail.row.credit?:return
    var dialog by rememberSaveable(detail.row.accountId) { mutableStateOf("") }
    var reason by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf("7 days") }
    var days by rememberSaveable { mutableStateOf("") }
    var reviewedRevision by rememberSaveable { mutableStateOf(credit.revisionUid) }
    var reviewedSets by remember { mutableStateOf(emptySet<String>()) }
    var pendingUid by rememberSaveable { mutableStateOf("") }
    var showLedger by rememberSaveable(detail.row.accountId) { mutableStateOf(false) }
    var showAudit by rememberSaveable(detail.row.accountId) { mutableStateOf(false) }
    CreditInformationSection(content,Modifier.fillMaxWidth().testTag("credit-detail")) {
        if(content != CustomerCreditContent.Management) {
        Text("Current credit",style=MaterialTheme.typography.titleMedium)
        NativeInformationFact("Balance", detail.row.balanceLabel, emphasized = true)
        CustomerCreditStatusText(credit)
        Text(credit.explanation,style=MaterialTheme.typography.bodyMedium)
        }
        NativeInformationFact("Future sale terms",credit.term.label)
        if(content != CustomerCreditContent.Summary) {
            if(credit.status.usesLegacyWeekly) Text("Older sales use the recorded 7-day default.",style=MaterialTheme.typography.bodyMedium)
            Text("Current debt only; this is not a lifetime reliability score. Sunday bills do not change payment deadlines.",style=MaterialTheme.typography.bodyMedium)
        }
        if(owner && content != CustomerCreditContent.Summary) {
            if(!credit.enabled) {
                Text("Sync and enable credit terms after every posting phone is updated. Existing credit posting stays on its recorded default.")
                OutlinedButton(onClick=hapticAction { dialog="enable" },enabled=!busy,
                    modifier=Modifier.fillMaxWidth().testTag("credit-enable")) { Text("Enable agreed terms") }
            } else {
                OutlinedButton(onClick=hapticAction {
                    reviewedRevision=credit.revisionUid
                    mode=when(credit.term) { CreditPaymentTerm.Weekly->"7 days"; CreditPaymentTerm.Monthly->"Calendar month"
                        CreditPaymentTerm(CreditTermKind.NET_DAYS,30)->"30 days"; else->"Custom days" }
                    days=credit.term.value.toString(); dialog="terms"
                },enabled=!busy,modifier=Modifier.fillMaxWidth().testTag("credit-edit-terms")) { Text("Change future sale terms") }
                OutlinedButton(onClick=hapticAction { reason=""; dialog="set" },enabled=!busy,
                    modifier=Modifier.fillMaxWidth().testTag("credit-set-flag")) { Text("Flag for Owner review") }
                if(credit.activeSetUids.isNotEmpty()) OutlinedButton(onClick=hapticAction {
                    reviewedSets=credit.activeSetUids.toSet(); reason=""; dialog="clear"
                },enabled=!busy,modifier=Modifier.fillMaxWidth().testTag("credit-clear-flag")) { Text("Clear reviewed flags") }
            }
        }
        if(content == CustomerCreditContent.Summary) Box(Modifier.fillMaxWidth().height(4.dp)) {
            if(busy) LinearProgressIndicator(Modifier.fillMaxWidth().testTag("credit-saving"))
        } else if(busy) LinearProgressIndicator(Modifier.fillMaxWidth().testTag("credit-saving"))
    }
    if(content == CustomerCreditContent.Summary) return
    if(credit.pending.isNotEmpty()) CreditInformationSection(content,Modifier.fillMaxWidth().testTag("credit-pending")) {
        Text("Changes awaiting sync",style=MaterialTheme.typography.titleMedium)
        for(change in credit.pending) {
            Text("${change.description} · ${if(change.held) "Held for Owner review" else "Pending server acceptance"}")
            if(owner && change.held && credit.enabled) {
                TextButton(onClick=hapticAction { pendingUid=change.uid; dialog="retry" },enabled=!busy) { Text("Review and retry") }
                if(change.kind=="TERM_REVISION") TextButton(onClick=hapticAction { pendingUid=change.uid; dialog="discard" },enabled=!busy) { Text("Discard held terms proposal") }
            }
        }
        Text("Pending actions do not replace accepted terms or flags. Sync, then refresh this customer.",style=MaterialTheme.typography.bodySmall)
    }
    if(content == CustomerCreditContent.All) {
    TextButton(onClick=hapticAction { showLedger=!showLedger },modifier=Modifier.testTag("credit-history")) {
        Text(if(showLedger) "Close account history" else "Account history")
    }
    if(showLedger) NativeInformationSection(Modifier.fillMaxWidth().testTag("credit-ledger")) {
        Text("Account history",style=MaterialTheme.typography.titleMedium)
        detail.ledger?.entries?.forEach { entry ->
            Text("${entry.occurredAt} · ${entry.taskLabel} · ${entry.stateLabel}",style=MaterialTheme.typography.labelLarge)
            NativeInformationFact(entry.movementLabel,"Balance ${entry.balanceAfterLabel}")
        }
        if(detail.ledger?.entries.isNullOrEmpty()) Text("No ledger movements yet.")
    }
    TextButton(onClick=hapticAction { showAudit=!showAudit },modifier=Modifier.testTag("credit-audit")) { Text(if(showAudit) "Close review history" else "Terms and review history") }
    }
    if(showAudit || content == CustomerCreditContent.Management) CreditInformationSection(content,Modifier.fillMaxWidth().testTag("credit-audit-history")) {
        Text("Owner review actions and accepted payment terms.",style=MaterialTheme.typography.bodySmall)
        credit.history.sortedDescending().forEach { Text(it,style=MaterialTheme.typography.bodyMedium) }
        if(credit.history.isEmpty()) Text("No terms changes or review actions.")
    }
    if(dialog.isNotEmpty()) {
        val custom=days.toIntOrNull()?.takeIf { it in 1..365 }
        val valid=when(dialog) { "terms" -> mode!="Custom days" || custom!=null; "set","clear" -> reason.trim().length in 1..500; else->true }
        AlertDialog(onDismissRequest={ if(!busy) dialog="" },title={ Text(when(dialog) {
            "terms"->"Future sale terms"; "set"->"Owner review flag"; "clear"->"Clear reviewed flags"
            "enable"->"Enable agreed credit terms?"; "discard"->"Discard held proposal?"; else->"Retry this change?"
        }) },text={ Column(Modifier.imePadding().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            when(dialog) {
                "terms" -> {
                    Text("Applies to future new credit sales after server acceptance. Existing sales keep their agreed terms.")
                    listOf("7 days","Calendar month","30 days","Custom days").forEach { option ->
                        Row(Modifier.fillMaxWidth().clickable(onClick=hapticAction { mode=option }),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                            RadioButton(mode==option,onClick=hapticAction { mode=option }); Text(option)
                        }
                    }
                    if(mode=="Custom days") OutlinedTextField(days,{ days=it },label={ Text("Days (1–365)") },singleLine=true,
                        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.testTag("credit-custom-days"))
                    Text("Calendar month means the same date next month, clamped to its final day. It is different from 30 days.")
                }
                "set","clear" -> {
                    Text(if(dialog=="clear") "Clear only the flags reviewed now. Any unseen flag remains active. Overdue debt stays overdue." else "This judgment is separate from the calculated debt status. Payments do not clear it automatically.")
                    OutlinedTextField(reason,{reason=it},label={ Text("Reason") },modifier=Modifier.fillMaxWidth().testTag("credit-review-reason"))
                }
                "enable" -> Text("Confirm every phone used to post sales has this compatible version and has synced. Old offline versions must stay out of use. The server will refuse new credit sales without recorded terms.")
                "discard" -> Text("Only this unaccepted terms proposal is removed; its reviewed body remains in the security audit. Accepted agreements and history stay unchanged. You can then choose terms again.")
                else -> Text("Retry the same immutable change after reviewing sync. If terms changed elsewhere, discard the held terms proposal and choose again.")
            }
        } },confirmButton={ TextButton(onClick=hapticCommitAction {
            if(!valid || busy) return@hapticCommitAction
            val command=when(dialog) {
                "terms"->CustomerCreditCommand.Terms(reviewedRevision,when(mode) { "Calendar month"->CreditPaymentTerm.Monthly; "30 days"->CreditPaymentTerm(CreditTermKind.NET_DAYS,30); "Custom days"->CreditPaymentTerm(CreditTermKind.NET_DAYS,custom!!); else->CreditPaymentTerm.Weekly })
                "set"->CustomerCreditCommand.Flag(reason.trim())
                "clear"->CustomerCreditCommand.Clear(reviewedSets,reason.trim())
                "enable"->CustomerCreditCommand.Enable
                "discard"->CustomerCreditCommand.DiscardTerm(pendingUid)
                else->CustomerCreditCommand.Retry(pendingUid)
            }
            dialog=""; onAction(command)
        },enabled=valid && !busy,modifier=Modifier.testTag("credit-confirm")) { Text(if(dialog=="enable") "All posting phones are updated" else "Confirm") } },
            dismissButton={ TextButton(onClick=hapticAction { dialog="" },enabled=!busy) { Text("Cancel") } })
    }
}

@Composable
private fun CreditInformationSection(mode: CustomerCreditContent,modifier: Modifier,content: @Composable ColumnScope.()->Unit) {
    if(mode==CustomerCreditContent.Management) Column(modifier,verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
    else NativeInformationSection(modifier,content)
}
