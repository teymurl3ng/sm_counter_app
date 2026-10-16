package com.saaf.milkshop.nativeui.owner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.owner.statementMoney
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.nativeui.common.*
import com.saaf.milkshop.nativeui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

internal data class FollowUpWorkspaceSnapshot(val policy: FollowUpPolicy,val today: LocalDate,val customers: List<FollowUpCustomer>,
    val preferences: Map<String,CustomerFollowUpPreference> = emptyMap(),val acknowledgments: Map<String,FollowUpAcknowledgment> = emptyMap(),
    val freshness: String="Sync needed",val online: Boolean=false,val owner: Boolean=false,val primaryDevice: String="Not selected",
    val notificationsAllowed: Boolean=false,val shopZone: String="Shop timezone",val hasMessageApp: Boolean=false,
    val saveToken: String="",val reviewReady: Boolean=true,val unfinished: List<FollowUpUnfinishedSave> = emptyList(),
    val activity: List<FollowUpActivityRow> = emptyList(),val requests: List<FollowUpRequestRow> = emptyList(),
    val claims: List<FollowUpClaimRow> = emptyList(),val configured: Boolean=true)
internal data class FollowUpActivityRow(val sequence: Long,val label: String,val customer: String?,val by: String,val at: String)
internal data class FollowUpRequestRow(val uid: String,val label: String,val customer: String?,val restored: Boolean,
    val sameBinding: Boolean,val canCheck: Boolean,val canSetAside: Boolean)
internal data class FollowUpClaimRow(val attempt: String,val accountId: Long?,val customer: String,val phase: String,val canReview: Boolean)
internal data class FollowUpUnfinishedSave(val uid: String,val kind: String)
internal data class FollowUpWorkspaceSave(val snapshot: FollowUpWorkspaceSnapshot,val accepted: Boolean)
internal sealed interface FollowUpCommand {
    data class Policy(val value: FollowUpPolicy,val makePrimary: Boolean=false,val phonesUpdated: Boolean=false): FollowUpCommand
    data class Preference(val customerUid: String,val value: CustomerFollowUpPreference): FollowUpCommand
    data class Reviewed(val customerUid: String,val expectedCycle: String?,val on: LocalDate): FollowUpCommand
    data class Snooze(val customerUid: String,val until: LocalDate): FollowUpCommand
}
internal data class FollowUpWorkspaceAccess(val load: suspend ()->FollowUpWorkspaceSnapshot,
    val command: suspend (FollowUpCommand,String)->FollowUpWorkspaceSave,
    val recover: suspend (String,Boolean)->FollowUpWorkspaceSave = {_,_->error("Unfinished-save review is unavailable.")},
    val reviewRequest: suspend (String,Boolean)->FollowUpWorkspaceSave = {_,_->error("Request review is unavailable.")})

/** Bound presentation; ordinary navigation waits for B's shared external-handoff gate. */
@Composable
internal fun FollowUpWorkspace(access: FollowUpWorkspaceAccess,onBack: ()->Unit,onStatement: (Long)->Unit,onReminder: (Long)->Unit,
    publishGuard: (EntryLeaveGuard?)->Unit={},openingCustomer: Boolean=false) {
    var snapshot by remember(access) {mutableStateOf<FollowUpWorkspaceSnapshot?>(null)}
    var error by remember {mutableStateOf<String?>(null)}
    var notice by remember {mutableStateOf<String?>(null)}
    var busy by remember {mutableStateOf(true)}
    var page by rememberSaveable {mutableStateOf("list")}
    var selected by rememberSaveable {mutableStateOf<String?>(null)}
    var filter by rememberSaveable {mutableStateOf("All")}
    var query by rememberSaveable {mutableStateOf("")}
    var postponed by rememberSaveable {mutableStateOf(false)}
    var retry by remember {mutableIntStateOf(0)}
    val scroll=remember(page) {ScrollState(0)}
    val scope=rememberCoroutineScope()
    val guard=rememberEntryLeaveGuard()
    DisposableEffect(guard) {publishGuard(guard);onDispose {publishGuard(null)}}
    fun leave(action: ()->Unit) {if(!busy&&!openingCustomer) guard.requestLeave(action)}
    fun back()=leave {when(page) {"list"->onBack();"schedule"->page="review";"activity"->page="settings";else->page="list"}}
    ActivePaneBackHandler(onBack={back()})
    LaunchedEffect(access,retry) {
        busy=true;error=null
        try {snapshot=access.load()} catch(cancelled: CancellationException) {throw cancelled}
        catch(failure: Exception) {error=failure.message ?: "Follow-ups could not refresh. Try again."} finally {busy=false}
    }
    fun submit(command: FollowUpCommand,success: String) {
        if(busy) return
        busy=true;error=null;notice=null
        scope.launch {
            try {
                val previous=requireNotNull(snapshot)
                val result=access.command(command,previous.saveToken)
                // A conflicting server revision must not replace the operator's failed draft.
                snapshot=if(result.accepted) result.snapshot else previous.copy(unfinished=result.snapshot.unfinished,
                    online=result.snapshot.online,reviewReady=false,saveToken=result.snapshot.saveToken,
                    freshness="Shared state may have changed · Check the unfinished save")
                if(result.accepted) {guard.clear();page="list";notice=success}
                else error="The save is unfinished. Check its result before making another change."
            }
            catch(cancelled: CancellationException) {throw cancelled}
            catch(failure: Exception) {error=failure.message ?: "Could not save. Check the connection and try again."}
            finally {busy=false}
        }
    }
    fun recover(uid: String,setAside: Boolean) {
        if(busy) return
        busy=true;error=null;notice=null
        scope.launch {
            try {
                val result=access.recover(uid,setAside);snapshot=result.snapshot
                if(result.accepted) {guard.clear();page="list";notice=if(setAside) "Local unfinished save set aside. This does not undo a server change." else "Save result checked. Current shared settings are shown."}
                else error="The save result is still unresolved. Refresh or set aside only this local request."
            } catch(cancelled: CancellationException) {throw cancelled}
            catch(failure: Exception) {error=failure.message ?: "Could not check the save. Try again."}
            finally {busy=false}
        }
    }
    var setAside by remember {mutableStateOf<String?>(null)}
    var reviewAside by remember {mutableStateOf<String?>(null)}
    fun reviewRequest(uid: String,aside: Boolean) {
        if(busy) return
        busy=true;error=null;notice=null
        scope.launch {
            try {
                val result=access.reviewRequest(uid,aside);snapshot=result.snapshot
                notice=if(aside) "Local request set aside. Shared changes and message attempts are unchanged."
                    else if(result.accepted) "Request result checked. No app was opened and no alert was posted."
                    else "The request is still unfinished. Review the current shared state before trying again."
            } catch(cancelled: CancellationException) {throw cancelled}
            catch(failure: Exception) {error=failure.message ?: "The request could not be checked. Try again."}
            finally {busy=false}
        }
    }
    LaunchedEffect(error) {if(error!=null) scroll.animateScrollTo(0)}
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(scroll).padding(16.dp).testTag("followup-workspace"),
        verticalArrangement=Arrangement.spacedBy(16.dp)) {
        TextButton(onClick=hapticAction {back()},enabled=!busy&&!openingCustomer) {Text(if(page=="list") "Back to customers" else "Back")}
        NativeInformationHeading(when(page) {"settings"->"Follow-up settings";"activity"->"Shared follow-up activity";"review"->"Review follow-up";"schedule"->"Statement schedule";else->"Follow-ups"},
            actionLabel=if(page=="list") "Settings" else null,actionTag="followup-settings",onAction=if(page=="list") ({leave {page="settings"}}) else null)
        Box(Modifier.fillMaxWidth().height(4.dp)) {if(busy||openingCustomer) LinearProgressIndicator(Modifier.fillMaxWidth().testTag("followup-progress"))}
        error?.let {Text(it,color=MaterialTheme.colorScheme.error,modifier=Modifier.testTag("followup-error").semantics {liveRegion=LiveRegionMode.Polite})}
        notice?.let {Text(it,modifier=Modifier.testTag("followup-notice").semantics {liveRegion=LiveRegionMode.Polite})}
        val state=snapshot
        if(state!=null) {
            if(page!="activity") state.unfinished.forEach {pending->NativeInformationSection {
                Text("Unfinished follow-up save",style=nativeSectionTextStyle())
                Text("${pending.kind.lowercase().replaceFirstChar {it.uppercase()}}: the server may already have accepted it. Checking uses the same request.",style=nativeSupportingTextStyle())
                TextButton(onClick=hapticAction {recover(pending.uid,false)},enabled=!busy && state.online,modifier=Modifier.testTag("followup-check-save")) {Text("Check save result")}
                TextButton(onClick=hapticAction {setAside=pending.uid},enabled=!busy,modifier=Modifier.testTag("followup-set-aside")) {Text("Set aside local request")}
            }}
            val editingBlocked=busy || openingCustomer || state.unfinished.isNotEmpty()
            val rows=CustomerFollowUps.ordered(state.customers.mapNotNull {customer->CustomerFollowUps.evaluate(customer,state.policy,
                state.preferences[customer.uid],state.acknowledgments[customer.uid] ?: FollowUpAcknowledgment(),state.today)})
            when(page) {
                "settings"->{
                    FollowUpPolicyEditor(state,editingBlocked,{guard.markDirty()}) {submit(it,"Follow-up settings saved.")}
                    OutlinedButton(onClick=hapticAction {leave {page="activity"}},enabled=!busy,
                        modifier=Modifier.fillMaxWidth().testTag("followup-shared-activity")) {Text("Shared activity & request review")}
                }
                "activity"->FollowUpSharedReview(state,busy||openingCustomer||error!=null,{reviewRequest(it,false)},{reviewAside=it},onReminder)
                "schedule"->{
                    val customer=state.customers.find {it.uid==selected}
                    if(customer!=null) FollowUpScheduleEditor(customer,state,editingBlocked,{guard.markDirty()}) {
                        submit(FollowUpCommand.Preference(customer.uid,it),"Statement schedule saved. Payment terms are unchanged.")
                    } else Text("Customer is no longer available. Return and refresh.")
                }
                "review"->{
                    val row=rows.find {it.customer.uid==selected}
                    if(row==null) NativeInformationSection {Text("This customer no longer needs follow-up.");Text("The current balance, schedule or review state changed.")}
                    else FollowUpReview(row,state,editingBlocked || error!=null,
                        {onStatement(row.customer.accountId)},{onReminder(row.customer.accountId)},{page="schedule"},
                        {submit(FollowUpCommand.Reviewed(row.customer.uid,row.statementCycle,state.today),"Marked reviewed. This does not confirm sending or payment.")},
                        {date->submit(FollowUpCommand.Snooze(row.customer.uid,date),"Snoozed until $date. Payment terms are unchanged.")})
                }
                else->{
                    Text("${state.today} · ${state.freshness}",style=nativeSupportingTextStyle())
                    if(!state.policy.enabled) NativeInformationSection {Text("Automatic follow-ups are off.");Text("Manual statements and reminders remain available from each customer.")}
                    if(!state.notificationsAllowed) Text("Android notifications are unavailable. Follow-ups remain visible here.",style=nativeSupportingTextStyle())
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        listOf("All","Statements","Overdue","Check records").forEach {choice->FilterChip(filter==choice,onClick=hapticAction {filter=choice},label={Text(choice)})}
                    }
                    OutlinedTextField(query,{query=it},label={Text("Search customers")},singleLine=true,modifier=Modifier.fillMaxWidth().testTag("followup-search"))
                    val shown=rows.filter {row->(row.actionable || postponed) && row.customer.name.contains(query.trim(),true) && when(filter) {
                        "Statements"->FollowUpReason.STATEMENT in row.reasons;"Overdue"->FollowUpReason.OVERDUE in row.reasons
                        "Check records"->FollowUpReason.RECORDS in row.reasons || FollowUpReason.OWNER_REVIEW in row.reasons;else->true
                    }}
                    Text("${shown.size} ${if(shown.size==1) "customer" else "customers"}",style=nativeSupportingTextStyle(),modifier=Modifier.testTag("followup-count"))
                    if(shown.isEmpty()) NativeInformationSection {
                        Text(if(query.isNotBlank()) "No customers match this search." else "No follow-ups need review.")
                        Text("Next shop statement review: ${CustomerFollowUps.next(state.policy.statementSchedule,maxOf(state.today,state.policy.effectiveFrom.minusDays(1)))}",style=nativeSupportingTextStyle())
                    }
                    shown.forEach {row->Column(Modifier.nativeListItem(NativeListStyle.Entity).clickable(enabled=!busy&&!openingCustomer,onClick=hapticAction {selected=row.customer.uid;page="review"})
                        .nativeListItemContent(NativeListStyle.Entity).testTag("followup-row-${row.customer.accountId}"),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        Text(row.customer.name,style=nativeValueTextStyle())
                        Text(statementMoney(row.customer.balancePaisa),style=nativeMetricTextStyle(),color=nativeVisualColors.metric)
                        Text(row.reasons.joinToString(" · ") {it.label},style=nativeSupportingTextStyle())
                        if(row.paused) Text("Paused",style=nativeSupportingTextStyle())
                        row.postponedUntil?.let {Text("Snoozed until $it",style=nativeSupportingTextStyle())}
                        Text("${row.lastReviewed?.let {"Last reviewed $it"} ?: "Not reviewed yet"} · Review",style=nativeSupportingTextStyle())
                    }}
                    TextButton(onClick=hapticAction {postponed=!postponed}) {Text(if(postponed) "Hide postponed" else "View postponed")}
                }
            }
        } else if(!busy) Text("No current follow-up snapshot. Refresh to continue.")
        OutlinedButton(onClick=hapticAction {leave {retry++}},enabled=!busy,modifier=Modifier.fillMaxWidth().testTag("followup-refresh")) {Text("Refresh follow-ups")}
    }
    reviewAside?.let {uid->FollowUpSetAsideDialog({reviewAside=null}) {reviewAside=null;reviewRequest(uid,true)}}
    if(setAside!=null) AlertDialog(onDismissRequest={setAside=null},title={Text("Set aside this local request?")},
        text={Column(Modifier.heightIn(max=360.dp).verticalScroll(rememberScrollState())) {Text("This only removes the unfinished request from this device. A change accepted by the server stays in place. Refresh to see its current state. It does not cancel or undo that change.")}},
        confirmButton={TextButton(onClick=hapticAction {val uid=requireNotNull(setAside);setAside=null;recover(uid,true)}) {Text("Set aside")}},
        dismissButton={TextButton(onClick=hapticAction {setAside=null}) {Text("Keep request")}})
    if(guard.discardConfirmationOpen) AlertDialog(onDismissRequest=guard::keepEditing,title={Text("Leave without saving?")},
        text={Text("Your follow-up setting changes will be discarded.")},confirmButton={TextButton(onClick=hapticAction(guard::confirmDiscard)) {Text("Discard changes")}},
        dismissButton={TextButton(onClick=hapticAction(guard::keepEditing)) {Text("Keep editing")}})
}

@Composable
private fun FollowUpReview(row: CustomerFollowUp,state: FollowUpWorkspaceSnapshot,busy: Boolean,onStatement: ()->Unit,onReminder: ()->Unit,
    onSchedule: ()->Unit,onReviewed: ()->Unit,onSnooze: (LocalDate)->Unit) {
    var choose by remember {mutableStateOf(false)}
    NativeInformationSection {
        Text(row.customer.name,style=nativeSectionTextStyle())
        NativeInformationFact("Outstanding",statementMoney(row.customer.balancePaisa),emphasized=true)
        row.customer.status?.overduePaisa?.takeIf {it>0}?.let {NativeInformationFact("Known overdue",statementMoney(it),emphasized=true)}
        Text(row.reasons.joinToString(" · ") {it.label},style=nativeSupportingTextStyle())
        Text(state.freshness,style=nativeSupportingTextStyle())
        NativeInformationFact("Recipient",row.customer.phone ?: "No saved number")
        Text("Reviewing this task does not change the payment deadline.",style=nativeSupportingTextStyle())
    }
    Button(onClick=hapticAction(if(FollowUpReason.STATEMENT in row.reasons) onStatement else onReminder),enabled=!busy,
        modifier=Modifier.fillMaxWidth().testTag("followup-prepare")) {Text(if(FollowUpReason.STATEMENT in row.reasons) "Prepare statement" else "Prepare reminder")}
    OutlinedButton(onClick=hapticAction(if(FollowUpReason.STATEMENT in row.reasons) onReminder else onStatement),enabled=!busy,
        modifier=Modifier.fillMaxWidth()) {Text(if(FollowUpReason.STATEMENT in row.reasons) "Prepare reminder" else "Prepare statement")}
    if(!state.hasMessageApp) NativeInformationSection {
        Text("No messaging app is configured on this device.",style=nativeValueTextStyle(),modifier=Modifier.testTag("followup-manual-fallback"))
        Text("Review the statement here, or compose the reviewed message manually on the shop's separate phone. This app cannot confirm that sending.")
    }
    if(!state.online) Text("Connect before a coordinated message handoff. You can still review the draft and statement.",style=nativeSupportingTextStyle())
    TextButton(onClick=hapticAction(onSchedule),enabled=!busy,modifier=Modifier.testTag("followup-schedule")) {Text("Schedule & pause")}
    TextButton(onClick=hapticAction(onReviewed),enabled=!busy && state.online && state.reviewReady,modifier=Modifier.testTag("followup-reviewed")) {Text("Mark reviewed")}
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        TextButton(onClick=hapticAction {onSnooze(state.today.plusDays(1))},enabled=!busy && state.online && state.reviewReady) {Text("Tomorrow")}
        TextButton(onClick=hapticAction {onSnooze(state.today.plusDays(7))},enabled=!busy && state.online && state.reviewReady) {Text("Next week")}
    }
    TextButton(onClick=hapticAction {choose=true},enabled=!busy && state.online && state.reviewReady) {Text("Choose snooze date")}
    if(choose) FollowUpDateDialog(state.today.plusDays(1),state.today.plusDays(1),{choose=false}) {choose=false;onSnooze(it)}
}

@Composable
private fun FollowUpPolicyEditor(state: FollowUpWorkspaceSnapshot,busy: Boolean,dirty: ()->Unit,onSave: (FollowUpCommand.Policy)->Unit) {
    val current=state.policy
    var enabled by rememberSaveable(current.revision) {mutableStateOf(current.enabled)}
    var repeat by rememberSaveable(current.revision) {mutableStateOf(current.repeatDays.toString())}
    var weekday by rememberSaveable(current.revision) {mutableStateOf((current.statementSchedule as? StatementReviewSchedule.Weekly)?.weekday ?: DayOfWeek.SUNDAY)}
    var preferred by rememberSaveable(current.revision) {mutableStateOf(current.preferredTime.toString())}
    var start by rememberSaveable(current.revision) {mutableStateOf(current.quietStart.toString())}
    var end by rememberSaveable(current.revision) {mutableStateOf(current.quietEnd.toString())}
    var confirm by remember {mutableStateOf<FollowUpCommand.Policy?>(null)}
    var phonesUpdated by remember {mutableStateOf(false)}
    var primary by remember {mutableStateOf(false)}
    val canEdit=state.owner && state.online && !busy
    val proposed=runCatching {current.copy(enabled=enabled,repeatDays=repeat.toInt(),statementSchedule=StatementReviewSchedule.Weekly(weekday),
        preferredTime=LocalTime.parse(preferred),quietStart=LocalTime.parse(start),quietEnd=LocalTime.parse(end))
        .also {require(!it.isQuiet(it.preferredTime));require(it.preferredTime.second==0&&it.quietStart.second==0&&it.quietEnd.second==0)}}.getOrNull()
    NativeInformationSection {
        Text("Owner attention",style=nativeSectionTextStyle())
        Row(verticalAlignment=Alignment.CenterVertically) {Switch(enabled,onCheckedChange={enabled=it;dirty()},enabled=canEdit,modifier=Modifier.testTag("followup-enabled"));Text("Automatic follow-ups",Modifier.weight(1f))}
        NativeInformationFact("Notification device",state.primaryDevice)
        TextButton(onClick=hapticAction {phonesUpdated=false;primary=true;confirm=FollowUpCommand.Policy(requireNotNull(proposed),makePrimary=true)},enabled=canEdit && proposed!=null,
            modifier=Modifier.testTag("followup-primary")) {Text("Use this device for notifications")}
        NativeInformationFact("Timezone",state.shopZone)
        Text("Notifications may run late. Customer records stay inside the app.",style=nativeSupportingTextStyle())
        OutlinedTextField(preferred,{preferred=it;dirty()},label={Text("Around (HH:mm)")},singleLine=true,enabled=canEdit,modifier=Modifier.fillMaxWidth())
        OutlinedTextField(start,{start=it;dirty()},label={Text("Quiet hours start (HH:mm)")},singleLine=true,enabled=canEdit,modifier=Modifier.fillMaxWidth())
        OutlinedTextField(end,{end=it;dirty()},label={Text("Quiet hours end (HH:mm)")},singleLine=true,enabled=canEdit,modifier=Modifier.fillMaxWidth())
        Text(if(state.notificationsAllowed) "Android notifications allowed" else "Android notifications unavailable; the list remains usable.",style=nativeSupportingTextStyle())
    }
    NativeInformationSection {
        Text("Review schedule",style=nativeSectionTextStyle())
        Text("Shop statement weekday",style=nativeValueTextStyle())
        DayOfWeek.entries.forEach {day->FollowUpChoice(day.name.lowercase().replaceFirstChar {it.uppercase()},weekday==day,canEdit) {weekday=day;dirty()}}
        OutlinedTextField(repeat,{repeat=it;dirty()},label={Text("Overdue follow-up interval (1–30 days)")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number,imeAction=ImeAction.Done),
            singleLine=true,enabled=canEdit,modifier=Modifier.fillMaxWidth().testTag("followup-repeat"))
        Text("Statement timing does not change payment deadlines.",style=nativeSupportingTextStyle())
    }
    NativeInformationSection {Text("Customer contact",style=nativeSectionTextStyle());Text("Review and send manually",style=nativeValueTextStyle());Text("Automatic SMS: not configured. The tablet does not send through the separate shop phone.")}
    if(proposed==null) Text("Use a 1–30 day interval, valid HH:mm times and different quiet-hour endpoints.",color=MaterialTheme.colorScheme.error)
    if(!state.owner) Text("Only Owner can change the shop policy.")
    else if(!state.online) Text("Connect to save shared settings.")
    Button(onClick=hapticAction {proposed?.let {
        if(it.enabled) {phonesUpdated=false;primary=state.primaryDevice=="Not selected";confirm=FollowUpCommand.Policy(it)} else onSave(FollowUpCommand.Policy(it))
    }},enabled=canEdit && proposed!=null,modifier=Modifier.fillMaxWidth().testTag("followup-save-policy")) {Text("Save settings")}
    if(confirm!=null) AlertDialog(onDismissRequest={confirm=null},title={Text("Confirm follow-up settings")},text={Column(Modifier.heightIn(max=360.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("One device receives owner alerts. Synced devices need a connection to check message settings before opening a customer message, including when follow-ups are off. Drafts and statements remain available offline. The app cannot see payments on another offline device.")
        Row(verticalAlignment=Alignment.CenterVertically) {Checkbox(phonesUpdated,{phonesUpdated=it},modifier=Modifier.testTag("followup-confirm-updated"));Text("All posting devices have the compatible update",Modifier.weight(1f))}
        Row(verticalAlignment=Alignment.CenterVertically) {Checkbox(primary,{primary=it},modifier=Modifier.testTag("followup-confirm-primary"));Text("Use this device for owner alerts",Modifier.weight(1f))}
    }},confirmButton={TextButton(onClick=hapticAction {val proposal=requireNotNull(confirm);confirm=null;onSave(proposal.copy(makePrimary=primary,phonesUpdated=phonesUpdated))},
        enabled=phonesUpdated && (state.primaryDevice!="Not selected" || primary),modifier=Modifier.testTag("followup-confirm-policy")) {Text("Confirm settings")}},
        dismissButton={TextButton(onClick=hapticAction {confirm=null}) {Text("Keep editing")}})
}

@Composable
internal fun FollowUpScheduleEditor(customer: FollowUpCustomer,state: FollowUpWorkspaceSnapshot,busy: Boolean,dirty: ()->Unit,onSave: (CustomerFollowUpPreference)->Unit) {
    val current=state.preferences[customer.uid]
    val key=current?.revision ?: "shop"
    var mode by rememberSaveable(key) {mutableStateOf(when {current?.paused==true->"Paused";current?.schedule is StatementReviewSchedule.Weekly->"Weekly";current?.schedule is StatementReviewSchedule.Monthly->"Monthly";current?.schedule is StatementReviewSchedule.Interval->"Custom interval";else->"Use shop schedule"})}
    var weekday by rememberSaveable(key) {mutableStateOf((current?.schedule as? StatementReviewSchedule.Weekly)?.weekday ?: DayOfWeek.SUNDAY)}
    var monthly by rememberSaveable(key) {mutableStateOf(((current?.schedule as? StatementReviewSchedule.Monthly)?.day ?: 31).toString())}
    var days by rememberSaveable(key) {mutableStateOf(((current?.schedule as? StatementReviewSchedule.Interval)?.days ?: 7).toString())}
    var first by rememberSaveable(key) {mutableStateOf(((current?.schedule as? StatementReviewSchedule.Interval)?.firstDate ?: state.today).toString())}
    var choose by remember {mutableStateOf(false)}
    val canEdit=state.online && state.configured && !busy
    if(!state.configured) Text("Owner needs to save shop follow-up settings before a customer schedule can be saved.",style=nativeSupportingTextStyle())
    val proposed=runCatching {
        val schedule=when(mode) {"Weekly"->StatementReviewSchedule.Weekly(weekday);"Monthly"->StatementReviewSchedule.Monthly(monthly.toInt());"Custom interval"->StatementReviewSchedule.Interval(days.toInt(),LocalDate.parse(first));"Paused"->current?.schedule;else->null}
        CustomerFollowUpPreference(key,state.today,schedule,mode=="Paused",current?.snoozedUntil)
    }.getOrNull()
    Text(customer.name,style=nativeValueTextStyle())
    NativeInformationSection {
        Text("Statement schedule",style=nativeSectionTextStyle())
        Text("Payment terms stay attached to each sale. This only changes statement review timing.",style=nativeSupportingTextStyle())
        listOf("Use shop schedule","Weekly","Monthly","Custom interval","Paused").forEach {choice->FollowUpChoice(choice,mode==choice,canEdit) {mode=choice;dirty()}}
        when(mode) {
            "Weekly"->DayOfWeek.entries.forEach {day->FollowUpChoice(day.name.lowercase().replaceFirstChar {it.uppercase()},weekday==day,canEdit) {weekday=day;dirty()}}
            "Monthly"->OutlinedTextField(monthly,{monthly=it;dirty()},label={Text("Day of month (1–31; 0 for last day)")},supportingText={Text("Short months use their last day; the original day resumes next month.")},
                enabled=canEdit,singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth().testTag("followup-monthly-day"))
            "Custom interval"->{
                OutlinedTextField(days,{days=it;dirty()},label={Text("Every (1–365 days)")},enabled=canEdit,singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth().testTag("followup-custom-days"))
                NativeInformationHeading("First review date",actionLabel="Change",actionTag="followup-first-date",onAction={if(canEdit) choose=true})
                Text(first,style=nativeValueTextStyle())
            }
        }
        Text("Paused removes automated attention; manual account review stays available.",style=nativeSupportingTextStyle())
    }
    if(proposed==null) Text("Check the day or interval before saving.",color=MaterialTheme.colorScheme.error)
    Button(onClick=hapticAction {proposed?.let(onSave)},enabled=canEdit && proposed!=null,modifier=Modifier.fillMaxWidth().testTag("followup-save-schedule")) {Text("Save schedule")}
    if(choose) FollowUpDateDialog(LocalDate.parse(first),null,{choose=false}) {first=it.toString();choose=false;dirty()}
}

@Composable
private fun FollowUpChoice(label: String,selected: Boolean,enabled: Boolean,onSelect: ()->Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min=56.dp).clickable(enabled=enabled,onClick=hapticAction(onSelect)),verticalAlignment=Alignment.CenterVertically) {
        RadioButton(selected,onClick=hapticAction(onSelect),enabled=enabled);Text(label,Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FollowUpDateDialog(initial: LocalDate,minimum: LocalDate?,dismiss: ()->Unit,onDate: (LocalDate)->Unit) {
    val picker=rememberDatePickerState(initialSelectedDateMillis=initial.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates=object: SelectableDates {override fun isSelectableDate(utcTimeMillis: Long)=minimum==null ||
            java.time.Instant.ofEpochMilli(utcTimeMillis).atZone(java.time.ZoneOffset.UTC).toLocalDate()>=minimum})
    DatePickerDialog(onDismissRequest=dismiss,confirmButton={TextButton(onClick=hapticAction {
        picker.selectedDateMillis?.let {onDate(java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate())}
    },enabled=picker.selectedDateMillis!=null) {Text("Use date")}},dismissButton={TextButton(onClick=hapticAction(dismiss)) {Text("Cancel")}}) {DatePicker(picker)}
}
