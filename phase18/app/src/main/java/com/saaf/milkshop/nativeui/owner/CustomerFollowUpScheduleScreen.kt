package com.saaf.milkshop.nativeui.owner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.domain.CustomerFollowUpPreference
import com.saaf.milkshop.nativeui.common.*
import com.saaf.milkshop.nativeui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** All customers, including settled accounts; eligibility never controls preference access. */
@Composable
internal fun CustomerFollowUpScheduleScreen(customerId: Long,access: FollowUpWorkspaceAccess,onBack: ()->Unit,
    publishGuard: (EntryLeaveGuard?)->Unit={}) {
    var snapshot by remember(customerId,access) {mutableStateOf<FollowUpWorkspaceSnapshot?>(null)}
    var busy by remember {mutableStateOf(true)}
    var error by remember {mutableStateOf<String?>(null)}
    var notice by remember {mutableStateOf<String?>(null)}
    var retry by remember {mutableIntStateOf(0)}
    var draftEpoch by rememberSaveable(customerId) {mutableIntStateOf(0)}
    val guard=rememberEntryLeaveGuard();val scope=rememberCoroutineScope()
    SideEffect {guard.onDiscard={draftEpoch++}}
    DisposableEffect(guard) {publishGuard(guard);onDispose {publishGuard(null)}}
    fun back() {if(!busy) guard.requestLeave(onBack)}
    ActivePaneBackHandler {back()}
    LaunchedEffect(customerId,access,retry) {
        busy=true;error=null
        try {snapshot=access.load()} catch(cancelled: CancellationException) {throw cancelled}
        catch(failure: Exception) {error=failure.message ?: "The schedule could not load. Connect and try again."}
        finally {busy=false}
    }
    fun save(value: CustomerFollowUpPreference) {
        if(busy) return
        val before=snapshot ?: return;val customer=before.customers.singleOrNull {it.accountId==customerId} ?: return
        busy=true;error=null;notice=null
        scope.launch {
            try {
                val result=access.command(FollowUpCommand.Preference(customer.uid,value),before.saveToken)
                if(result.accepted) {snapshot=result.snapshot;guard.clear();draftEpoch++;notice="Statement schedule saved. Payment terms are unchanged."}
                else {snapshot=before.copy(online=result.snapshot.online,unfinished=result.snapshot.unfinished,requests=result.snapshot.requests,saveToken=result.snapshot.saveToken)
                    error="The save needs review. Check the unfinished request in Follow-up settings before changing it again."}
            } catch(cancelled: CancellationException) {throw cancelled}
            catch(failure: Exception) {error=failure.message ?: "The schedule could not save. Your draft is still here."}
            finally {busy=false}
        }
    }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp).testTag("customer-followup-schedule"),
        verticalArrangement=Arrangement.spacedBy(16.dp)) {
        TextButton(onClick=hapticAction {back()},enabled=!busy) {Text("Back to statement")}
        Text("Schedule & pause",style=MaterialTheme.typography.titleLarge)
        Box(Modifier.fillMaxWidth().height(4.dp)) {if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())}
        notice?.let {Text(it,modifier=Modifier.testTag("customer-schedule-notice"))}
        error?.let {Text(it,color=MaterialTheme.colorScheme.error,modifier=Modifier.testTag("customer-schedule-error"))}
        snapshot?.let {state->
            val customer=state.customers.singleOrNull {it.accountId==customerId}
            if(customer==null) Text("Customer is no longer available. Return and refresh.")
            else key(draftEpoch) {FollowUpScheduleEditor(customer,state,busy||error!=null||state.unfinished.isNotEmpty()||state.requests.any {it.sameBinding},
                {guard.markDirty();notice=null},::save)}
            if(state.requests.any {it.sameBinding}||state.unfinished.isNotEmpty()) Text("Review unfinished requests under Customers → Follow-ups → Settings → Shared activity & request review.",style=nativeSupportingTextStyle())
        }
        OutlinedButton(onClick=hapticAction {guard.requestLeave {draftEpoch++;retry++}},enabled=!busy,modifier=Modifier.fillMaxWidth()) {Text("Refresh schedule")}
    }
    if(guard.discardConfirmationOpen) AlertDialog(onDismissRequest=guard::keepEditing,title={Text("Leave without saving?")},
        text={Text("Your statement schedule changes will be discarded. Payment terms stay unchanged.")},
        confirmButton={TextButton(onClick=hapticAction(guard::confirmDiscard),modifier=Modifier.testTag("customer-schedule-discard")) {Text("Discard changes")}},
        dismissButton={TextButton(onClick=hapticAction(guard::keepEditing)) {Text("Keep editing")}})
}

@Composable
internal fun FollowUpSetAsideDialog(onDismiss: ()->Unit,onConfirm: ()->Unit) {
    AlertDialog(onDismissRequest=onDismiss,title={Text("Set aside local request?")},
        text={Column(Modifier.heightIn(max=360.dp).verticalScroll(rememberScrollState())) {
            Text("The server may already have accepted this request. This removes only the unfinished copy on this device. It does not undo a shared change, cancel a message preparation, resolve a handoff or confirm sending. The original connection or Owner may still need to review the shared attempt.")
        }},confirmButton={TextButton(onClick=hapticAction(onConfirm),modifier=Modifier.testTag("followup-review-aside-confirm")) {Text("Set aside local request")}},
        dismissButton={TextButton(onClick=hapticAction(onDismiss)) {Text("Keep request")}})
}
