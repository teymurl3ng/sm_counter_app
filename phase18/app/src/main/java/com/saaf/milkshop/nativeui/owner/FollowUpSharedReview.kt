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
    Text("Checking can finish an unconfirmed save. It never opens a message app or shows an alert. Staff reports do not confirm sending, delivery or payment.",style=nativeSupportingTextStyle())
    Text("Unfinished actions",style=nativeSectionTextStyle())
    if(state.requests.isEmpty()) Text("No unfinished actions on this device.",style=nativeSupportingTextStyle())
    state.requests.forEach {request->NativeInformationSection(Modifier.testTag("followup-request-${request.uid}")) {
        Text(request.label,style=nativeValueTextStyle());request.customer?.let {Text(it,style=nativeSupportingTextStyle())}
        if(request.restored) Text("Restored from backup · Result needs review",style=nativeSupportingTextStyle())
        if(!request.sameBinding) Text("Saved under another connection. You cannot retry it here. Removing it clears only this device's copy.",style=nativeSupportingTextStyle())
        TextButton(onClick=hapticAction {onCheck(request.uid)},enabled=!busy&&request.canCheck,
            modifier=Modifier.testTag("followup-review-check-${request.uid}")) {Text("Check action")}
        TextButton(onClick=hapticAction {onSetAside(request.uid)},enabled=!busy&&request.canSetAside,
            modifier=Modifier.testTag("followup-review-aside-${request.uid}")) {Text("Remove from this device")}
    }}
    Text("Message attempts",style=nativeSectionTextStyle())
    if(state.claims.isEmpty()) Text("No message attempts waiting for review.",style=nativeSupportingTextStyle())
    state.claims.forEach {claim->NativeInformationSection {
        Text(claim.customer,style=nativeValueTextStyle())
        Text(if(claim.phase=="HANDOFF_STARTED") "A message app may have opened. Review this attempt before starting another."
            else "A device is preparing this message. Sending is not confirmed.",style=nativeSupportingTextStyle())
        if(claim.canReview&&claim.accountId!=null) TextButton(onClick=hapticAction {onReview(claim.accountId)},enabled=!busy,
            modifier=Modifier.testTag("followup-review-claim-${claim.accountId}")) {Text("Review message attempt")}
        else Text(if(claim.accountId==null) "Sync this device to review this customer." else "Review this attempt on the device that started it, or ask Owner.",style=nativeSupportingTextStyle())
    }}
    Text("Latest synced activity",style=nativeSectionTextStyle())
    Text("Latest 100 actions confirmed by sync. Each device also has its own draft and share history.",style=nativeSupportingTextStyle())
    if(state.activity.isEmpty()) Text("No synced activity yet.",style=nativeSupportingTextStyle())
    state.activity.forEach {action->NativeInformationSection(Modifier.testTag("followup-activity-${action.sequence}")) {
        Text(action.label,style=nativeValueTextStyle());action.customer?.let {Text(it,style=nativeSupportingTextStyle())}
        Text(action.by,style=nativeSupportingTextStyle());Text(action.at,style=nativeSupportingTextStyle())
    }}
}
