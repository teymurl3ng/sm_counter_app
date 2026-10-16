package com.saaf.milkshop.nativeui

import android.content.Intent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.nativeui.common.EntryLeaveGuard
import com.saaf.milkshop.sync.FollowUpNotifier

/** Marker is navigation intent only. Ordinary authentication and destination readers own authority. */
internal fun isFollowUpDestination(intent: Intent?)=intent?.getStringExtra(FollowUpNotifier.EXTRA_DESTINATION)=="followups"
internal fun canOpenFollowUpDestination(role: AppRole?)=role in setOf(AppRole.OWNER,AppRole.ADMIN,AppRole.MANAGER)

@Composable
internal fun GuardedFollowUpDestination(request: String?,session: String,guard: EntryLeaveGuard?,
    open: ()->Unit,consume: (String)->Unit) {
    var attempted by rememberSaveable(session) {mutableStateOf<String?>(null)}
    val latestOpen=rememberUpdatedState(open);val latestConsume=rememberUpdatedState(consume)
    LaunchedEffect(request,session) {
        if(request!=null&&attempted!=request) {
            attempted=request
            val navigate={latestOpen.value();latestConsume.value(request)}
            guard?.requestLeave(navigate) ?: navigate()
        }
    }
}
