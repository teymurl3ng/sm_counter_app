package com.saaf.milkshop.nativeui.owner

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.saaf.milkshop.nativeui.common.*
import com.saaf.milkshop.nativeui.theme.*

@Composable
internal fun FollowUpSharedReview(state: FollowUpWorkspaceSnapshot,busy: Boolean,onCheck: (String)->Unit,
    onSetAside: (String)->Unit,onReview: (Long)->Unit) {
    Text(state.freshness,style=nativeSupportingTextStyle())
    Text("Checking a request never opens a messaging app or posts an alert. Staff reports do not prove sending, delivery or payment.",style=nativeSupportingTextStyle())
    Text("Unfinished requests",style=nativeSectionTextStyle())
    if(state.requests.isEmpty()) Text("No unfinished requests on this device.",style=nativeSupportingTextStyle())
    state.requests.forEach {request->NativeInformationSection(Modifier.testTag("followup-request-${request.uid}")) {
        Text(request.label,style=nativeValueTextStyle());request.customer?.let {Text(it,style=nativeSupportingTextStyle())}
        if(request.restored) Text("Restored from backup · Result needs review",style=nativeSupportingTextStyle())
        if(!request.sameBinding) Text("From a different connection. It cannot be retried here. Setting it aside removes only this local copy.",style=nativeSupportingTextStyle())
        TextButton(onClick=hapticAction {onCheck(request.uid)},enabled=!busy&&request.canCheck,
            modifier=Modifier.testTag("followup-review-check-${request.uid}")) {Text("Check request result")}
        TextButton(onClick=hapticAction {onSetAside(request.uid)},enabled=!busy&&request.canSetAside,
            modifier=Modifier.testTag("followup-review-aside-${request.uid}")) {Text("Set aside local request")}
    }}
    Text("Message preparations & handoffs",style=nativeSectionTextStyle())
    if(state.claims.isEmpty()) Text("No active shared message attempts.",style=nativeSupportingTextStyle())
    state.claims.forEach {claim->NativeInformationSection {
        Text(claim.customer,style=nativeValueTextStyle())
        Text(if(claim.phase=="HANDOFF_STARTED") "A messaging app may have opened. This handoff stays unresolved until reviewed."
            else "A device has reserved message preparation. Nothing is confirmed sent.",style=nativeSupportingTextStyle())
        if(claim.canReview&&claim.accountId!=null) TextButton(onClick=hapticAction {onReview(claim.accountId)},enabled=!busy,
            modifier=Modifier.testTag("followup-review-claim-${claim.accountId}")) {Text("Review message attempt")}
        else Text(if(claim.accountId==null) "Sync this device to review this customer." else "The preparing device or Owner must review this attempt.",style=nativeSupportingTextStyle())
    }}
    Text("Latest shared activity",style=nativeSectionTextStyle())
    Text("Latest 100 accepted actions. This is separate from preparation history saved on one phone.",style=nativeSupportingTextStyle())
    if(state.activity.isEmpty()) Text("No shared activity yet.",style=nativeSupportingTextStyle())
    state.activity.forEach {action->NativeInformationSection(Modifier.testTag("followup-activity-${action.sequence}")) {
        Text(action.label,style=nativeValueTextStyle());action.customer?.let {Text(it,style=nativeSupportingTextStyle())}
        Text(action.by,style=nativeSupportingTextStyle());Text(action.at,style=nativeSupportingTextStyle())
    }}
}
