package com.saaf.milkshop.nativeui.owner

import android.content.ActivityNotFoundException
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.owner.*
import com.saaf.milkshop.nativeui.common.*
import com.saaf.milkshop.nativeui.theme.*
import com.saaf.milkshop.statements.*
import kotlinx.coroutines.*
import java.util.UUID

data class CustomerCommunicationAccess(
    val reminder: suspend (Long)->CustomerReminder={error("Reminder loading is unavailable.")},
    val record: suspend (Long,CustomerCommunicationAction)->Unit={_,_->},
    val history: suspend (Long)->List<CustomerCommunicationHistory> = {emptyList()},
    val sharedHandoff: CustomerSharedHandoffAccess? = null,
)

@Composable
internal fun CustomerReminderScreen(customerId: Long,access: CustomerCommunicationAccess,onBack: ()->Unit,
    onEdit: ()->Unit,publishGuard: (EntryLeaveGuard?)->Unit={},launcher: ((ReminderChannel,String,String)->Unit)?=null) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val guard=rememberEntryLeaveGuard()
    var snapshot by remember(customerId) {mutableStateOf<CustomerReminder?>(null)}
    var error by remember {mutableStateOf<String?>(null)}
    var notice by rememberSaveable {mutableStateOf<String?>(null)}
    var loading by remember {mutableStateOf(true)}
    var busy by remember {mutableStateOf(false)}
    var retry by remember {mutableIntStateOf(0)}
    var channel by rememberSaveable {mutableStateOf(ReminderChannel.WhatsApp)}
    var override by rememberSaveable {mutableStateOf(false)}
    var recipient by rememberSaveable {mutableStateOf("")}
    var confirmed by rememberSaveable {mutableStateOf(false)}
    var sharedState by remember(customerId,access.sharedHandoff) {mutableStateOf<CustomerSharedHandoffState?>(null)}
    var sharedError by remember {mutableStateOf<String?>(null)}
    var resolution by remember {mutableStateOf<String?>(null)}
    var setAside by remember {mutableStateOf<String?>(null)}
    val toggleRecipient=hapticAction {confirmed=!confirmed}
    SideEffect {guard.onDiscard={recipient="";override=false;confirmed=false}}
    DisposableEffect(guard) {publishGuard(guard);onDispose {publishGuard(null)}}
    LaunchedEffect(override,recipient) {if(override && recipient.isNotBlank()) guard.markDirty() else guard.clear()}
    LaunchedEffect(customerId,retry) {
        loading=true;error=null;sharedError=null
        try {
            val next=access.reminder(customerId);require(next.customerId==customerId);snapshot=next;confirmed=false
            access.sharedHandoff?.let {shared->
                try {sharedState=shared.load(customerId)} catch(cancelled: CancellationException) {throw cancelled}
                catch(failure: Exception) {sharedError=failure.message ?: "Connect to check whether another device is handling this customer."}
            }
        }
        catch(cancelled: CancellationException) {throw cancelled}
        catch(failure: Exception) {error=failure.message ?: "Reminder could not load. Try again."}
        finally {loading=false}
    }
    val current=snapshot
    val raw=if(override) recipient else current?.phone.orEmpty()
    val normalized=runCatching {ReminderIntents.phone(raw)}.getOrNull()
    fun sharedWork(work: suspend ()->CustomerSharedHandoffState) {
        if(busy) return
        busy=true;notice=null;sharedError=null
        scope.launch {
            try {sharedState=work();snapshot=access.reminder(customerId);confirmed=false;notice="Handoff review updated. Sending or payment is not confirmed."}
            catch(cancelled: CancellationException) {throw cancelled}
            catch(failure: Exception) {sharedError=failure.message ?: "The handoff could not be checked. Refresh before continuing."}
            finally {busy=false}
        }
    }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal=16.dp,vertical=12.dp)
        .testTag("customer-reminder"),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        TextButton(onClick=hapticAction {guard.requestLeave(onBack)},modifier=Modifier.heightIn(min=48.dp)) {Text("Back to statement")}
        Text("Prepare reminder",style=MaterialTheme.typography.titleLarge)
        Box(Modifier.fillMaxWidth().height(4.dp)) {if(loading || busy) LinearProgressIndicator(Modifier.fillMaxWidth())}
        current?.let {facts->
            NativeInformationSection {
                NativeInformationHeading("Recipient",actionLabel=if(override) "Use saved number" else "Change number",actionTag="reminder-change-number",
                    onAction={if(!busy) {override=!override;confirmed=false}})
                Text(facts.customerName,style=nativeValueTextStyle())
                if(override || normalized==null) {
                    if(!override) Text("Saved number: ${facts.phone ?: "Not provided"}",style=nativeSupportingTextStyle())
                    OutlinedTextField(if(override) recipient else "",{override=true;recipient=it;confirmed=false;notice=null},
                        label={Text("International phone number")},supportingText={Text("Start with + and the country code. Used for this draft only.")},
                        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Phone),singleLine=true,enabled=!busy,
                        modifier=Modifier.fillMaxWidth().testTag("reminder-phone"))
                    TextButton(onClick=hapticAction {guard.requestLeave(onEdit)},enabled=!busy) {Text("Edit customer")}
                } else Text(normalized,style=nativeValueTextStyle(),modifier=Modifier.testTag("reminder-recipient"))
                if(normalized==null) Text("Enter one full international number before opening a composer.",color=MaterialTheme.colorScheme.error)
                Row(Modifier.fillMaxWidth().heightIn(min=48.dp).clickable(enabled=!busy && normalized!=null,onClick=hapticAction {confirmed=!confirmed}),
                    verticalAlignment=Alignment.CenterVertically) {
                    Checkbox(confirmed,onCheckedChange={toggleRecipient()},enabled=!busy && normalized!=null,modifier=Modifier.testTag("reminder-confirm-recipient"))
                    Text("I checked the recipient",Modifier.weight(1f))
                }
            }
            NativeInformationSection {
                Text("Message",style=nativeSectionTextStyle())
                ReminderChannel.entries.forEach {choice->Row(Modifier.fillMaxWidth().heightIn(min=56.dp)
                    .clickable(enabled=!busy,onClick=hapticAction {channel=choice;notice=null}),verticalAlignment=Alignment.CenterVertically) {
                    RadioButton(channel==choice,onClick=hapticAction {if(!busy) {channel=choice;notice=null}},enabled=!busy)
                    Text(choice.name,Modifier.weight(1f))
                }}
                Text(facts.message ?: if(facts.balancePaisa<0) "Customer advance balance: ${statementMoney(facts.balancePaisa)}. No payment reminder is needed."
                    else "Settled. No payment reminder is needed.",modifier=Modifier.testTag("reminder-message"),style=MaterialTheme.typography.bodyLarge)
                Text("Based on current local records, not the selected statement period.",style=nativeSupportingTextStyle())
            }
            access.sharedHandoff?.let {shared->NativeInformationSection {
                Text("Shared message check",style=nativeSectionTextStyle())
                Text(sharedError ?: sharedState?.blockedReason ?: if(loading) "Checking current shared records…" else "Current records will be checked again before opening.",
                    style=nativeSupportingTextStyle(),modifier=Modifier.testTag("reminder-shared-status"))
                sharedState?.pending?.forEach {request->
                    TextButton(onClick=hapticAction {sharedWork {shared.recover(customerId,request.uid)}},enabled=!busy,
                        modifier=Modifier.testTag("reminder-check-request")) {Text("Check unfinished request")}
                    TextButton(onClick=hapticAction {setAside=request.uid},enabled=!busy,
                        modifier=Modifier.testTag("reminder-set-aside")) {Text("Set aside local request")}
                }
                if(sharedState?.canRelease==true) TextButton(onClick=hapticAction {val attempt=requireNotNull(sharedState?.attemptUid);sharedWork {shared.release(customerId,attempt)}},
                    enabled=!busy&&sharedState?.pending?.isEmpty()==true,modifier=Modifier.testTag("reminder-cancel-preparation")) {Text("Cancel preparation")}
                if(sharedState?.canResolve==true) TextButton(onClick=hapticAction {resolution=sharedState?.attemptUid},enabled=!busy&&sharedState?.pending?.isEmpty()==true,
                    modifier=Modifier.testTag("reminder-review-handoff")) {Text("Review handoff")}
            }}
            val sharedReady=access.sharedHandoff==null || (sharedError==null&&sharedState!=null&&sharedState?.blockedReason==null&&sharedState?.pending?.isEmpty()==true)
            Button(enabled=!busy && !loading && error==null && normalized!=null && confirmed && facts.message!=null && sharedReady,
                modifier=Modifier.fillMaxWidth().testTag("reminder-open"),onClick=hapticAction {
                    if(busy) return@hapticAction
                    busy=true;notice=null
                    val reviewed=facts;val phone=normalized!!;val selectedChannel=channel
                    val operation=UUID.randomUUID().toString()
                    fun action(event: CommunicationEvent)=CustomerCommunicationAction(operation,reviewed.customerUid,"reminder",selectedChannel.name,reviewed.asOf,event)
                    scope.launch {
                        var opened=false
                        try {
                            val fresh=access.reminder(customerId)
                            if(fresh!=reviewed) {
                                snapshot=fresh;confirmed=false;notice="Records changed. Review the updated message and check the recipient again.";return@launch
                            }
                            access.record(customerId,action(CommunicationEvent.PREPARED))
                            val latest=access.reminder(customerId)
                            if(latest!=reviewed) {
                                snapshot=latest;confirmed=false;notice="Records changed. Review the updated message and check the recipient again.";return@launch
                            }
                            ensureActive()
                            val launch: suspend ()->Unit = {withContext(Dispatchers.Main.immediate) {
                                ensureActive()
                                (launcher ?: {c,p,m->ReminderIntents.open(context,c,p,m)})(selectedChannel,phone,reviewed.message!!)
                            }}
                            if(access.sharedHandoff!=null) sharedState=access.sharedHandoff.open(customerId,reviewed,phone,selectedChannel.name,launch)
                            else launch()
                            opened=true;notice="Composer opened. Review and send in the other app; sending is not confirmed."
                            try {access.record(customerId,action(CommunicationEvent.COMPOSER_OPENED))}
                            catch(cancelled: CancellationException) {throw cancelled}
                            catch(failure: Exception) {notice+=" Local history could not be saved."}
                        } catch(cancelled: CancellationException) {throw cancelled}
                        catch(failure: Exception) {
                            notice=if(failure is ActivityNotFoundException) {
                                if(access.sharedHandoff!=null) "No composer opened. Review this handoff before trying another channel. Keep the draft or share the statement manually."
                                else if(selectedChannel==ReminderChannel.WhatsApp) "WhatsApp is unavailable. Choose SMS or enable WhatsApp, then try again."
                                else "No SMS app is available. Enable an SMS app and try again."
                            } else failure.message ?: "The composer could not open. Try again."
                            access.sharedHandoff?.let {shared->
                                try {sharedState=shared.load(customerId)} catch(cancelled: CancellationException) {throw cancelled}
                                catch(checkError: Exception) {sharedError=checkError.message ?: "Refresh the shared handoff before continuing."}
                            }
                            if(!opened) try {access.record(customerId,action(CommunicationEvent.HANDOFF_FAILED))}
                            catch(cancelled: CancellationException) {throw cancelled}
                            catch(historyError: Exception) {notice+=" Local history could not be saved."}
                        } finally {busy=false}
                    }
                }) {Text("Open ${channel.name}")}
            Text(if(channel==ReminderChannel.SMS) "Review and send in your SMS app. Carrier charges may apply. No file is attached."
                else "Review and send in WhatsApp. Statement files are shared separately.",style=nativeSupportingTextStyle())
        }
        notice?.let {Text(it,modifier=Modifier.testTag("reminder-notice").semantics {liveRegion=LiveRegionMode.Polite})}
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        OutlinedButton(onClick=hapticAction {retry++},enabled=!busy && !loading,modifier=Modifier.fillMaxWidth()) {Text("Refresh current balance")}
    }
    resolution?.let {attempt->CustomerHandoffResolutionDialog(busy,{resolution=null}) {outcome,ack->
        resolution=null
        val shared=requireNotNull(access.sharedHandoff)
        sharedWork {shared.resolve(customerId,attempt,outcome,ack)}
    }}
    setAside?.let {request->AlertDialog(onDismissRequest={if(!busy) setAside=null},title={Text("Set aside local request?")},
        text={Text("The server may already have accepted this request. This only removes the unfinished request on this phone. It does not cancel a preparation, undo a handoff or confirm sending. Current shared records will still block another message when needed.")},
        confirmButton={TextButton(onClick=hapticAction {
            setAside=null;sharedWork {requireNotNull(access.sharedHandoff).setAside(customerId,request,true)}
        },enabled=!busy,modifier=Modifier.testTag("reminder-set-aside-confirm")) {Text("Set aside local request")}},
        dismissButton={TextButton(onClick=hapticAction {setAside=null},enabled=!busy) {Text("Keep request")}})}
    if(guard.discardConfirmationOpen) AlertDialog(onDismissRequest=guard::keepEditing,title={Text("Leave this draft?")},
        text={Text("The number entered for this draft will be cleared. The customer's saved number stays as it is.")},
        confirmButton={TextButton(onClick=hapticAction(guard::confirmDiscard),modifier=Modifier.testTag("reminder-discard")) {Text("Discard and leave")}},
        dismissButton={TextButton(onClick=hapticAction(guard::keepEditing)) {Text("Keep editing")}})
}

@Composable
private fun CustomerHandoffResolutionDialog(busy: Boolean,onDismiss: ()->Unit,onResolve: (String,Boolean)->Unit) {
    var outcome by remember {mutableStateOf<String?>(null)}
    var acknowledged by remember {mutableStateOf(false)}
    AlertDialog(onDismissRequest={if(!busy) onDismiss()},title={Text("Review message handoff")},text={
        Column(Modifier.heightIn(max=360.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("A composer may already have opened. This app cannot confirm sending or delivery. Choose what you know before another attempt.")
            listOf("REVIEWED" to "Mark reviewed","REPORTED_NOT_OPENED" to "No composer opened","RETRY_UNCERTAIN" to "Allow another attempt").forEach {(value,label)->
                Row(Modifier.fillMaxWidth().heightIn(min=56.dp).clickable(enabled=!busy,onClick=hapticAction {outcome=value}),verticalAlignment=Alignment.CenterVertically) {
                    RadioButton(outcome==value,onClick=hapticAction {outcome=value},enabled=!busy,modifier=Modifier.testTag("reminder-resolution-$value"))
                    Text(label,Modifier.weight(1f))
                }
            }
            Text("Allowing another attempt may cause duplicate contact if a message was already sent.",style=nativeSupportingTextStyle())
            Row(verticalAlignment=Alignment.CenterVertically) {Checkbox(acknowledged,{acknowledged=it},enabled=!busy,modifier=Modifier.testTag("reminder-resolution-ack"));Text("I understand this is my report, not proof of sending or payment",Modifier.weight(1f))}
        }
    },confirmButton={TextButton(onClick=hapticAction {outcome?.let {onResolve(it,acknowledged)}},enabled=!busy&&acknowledged&&outcome!=null,
        modifier=Modifier.testTag("reminder-resolution-confirm")) {Text("Confirm review")}},dismissButton={TextButton(onClick=hapticAction(onDismiss),enabled=!busy) {Text("Keep unresolved")}})
}

@Composable
internal fun CustomerPreparationHistory(customerId: Long,access: CustomerCommunicationAccess,onBack: ()->Unit) {
    var entries by remember {mutableStateOf<List<CustomerCommunicationHistory>>(emptyList())}
    var error by remember {mutableStateOf<String?>(null)}
    var loading by remember {mutableStateOf(true)}
    var retry by remember {mutableIntStateOf(0)}
    LaunchedEffect(customerId,retry) {
        loading=true;error=null
        try {entries=access.history(customerId)} catch(cancelled: CancellationException) {throw cancelled}
        catch(failure: Exception) {error=failure.message ?: "History could not load."} finally {loading=false}
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).testTag("customer-preparation-history"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        TextButton(onClick=hapticAction(onBack)) {Text("Back to statement")}
        Text("Preparation history",style=MaterialTheme.typography.titleLarge)
        Text("Latest 50 actions stored on this phone, including restored backups. This is not shared sending or delivery history.",style=nativeSupportingTextStyle())
        Box(Modifier.fillMaxWidth().height(4.dp)) {if(loading) LinearProgressIndicator(Modifier.fillMaxWidth())}
        if(!loading && error==null && entries.isEmpty()) Text("No preparation actions recorded yet.")
        entries.forEach {entry->NativeInformationSection {
            Text(if(entry.kind=="statement" && entry.event==CommunicationEvent.PREPARED) "Statement prepared" else entry.event.label,style=nativeSectionTextStyle())
            Text("${entry.channel} ${entry.kind}",style=nativeValueTextStyle())
            Text(if(entry.kind=="statement") "Statement period: ${entry.context.replace('/', '–')}" else "Balance as of ${entry.context}",style=nativeSupportingTextStyle())
            val recorded=runCatching {java.time.Instant.parse(entry.occurredAt).atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm"))}.getOrDefault(entry.occurredAt)
            Text("Recorded $recorded",style=nativeSupportingTextStyle())
        }}
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        TextButton(onClick=hapticAction {retry++},enabled=!loading) {Text("Refresh history")}
    }
}
