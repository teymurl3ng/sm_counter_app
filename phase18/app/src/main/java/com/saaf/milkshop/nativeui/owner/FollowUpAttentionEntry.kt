package com.saaf.milkshop.nativeui.owner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.nativeui.common.*
import com.saaf.milkshop.nativeui.theme.*
import com.saaf.milkshop.sync.FollowUpAttention

/** One reserved body entry in the upcoming customer route; zero/stale updates do not remount chrome. */
@Composable
internal fun FollowUpAttentionEntry(state: FollowUpAttention,onOpen: ()->Unit) {
    Column(Modifier.fillMaxWidth().nativeListItem(NativeListStyle.Navigation).clickable(onClick=hapticAction(onOpen))
        .nativeListItemContent(NativeListStyle.Navigation).testTag("followup-attention-entry"),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Text("Follow-ups",style=nativeValueTextStyle())
        Text(when {
            state.count==null->"Open to check customer follow-ups"
            state.stale->"${state.count} ${if(state.count==1) "customer" else "customers"} · Sync to check current records"
            state.count==0->"No follow-ups need review"
            state.count==1->"1 customer needs review"
            else->"${state.count} customers need review"
        },style=nativeSupportingTextStyle(),modifier=Modifier.testTag("followup-attention-description"))
    }
}
