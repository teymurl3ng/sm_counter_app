package com.saaf.milkshop.nativeui.owner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.saaf.milkshop.nativeui.common.hapticAction
import com.saaf.milkshop.nativeui.theme.nativeSupportingTextStyle
import com.saaf.milkshop.sync.*

internal enum class FollowUpPermissionAction { REQUEST, SETTINGS }
internal fun followUpPermissionAction(sdk: Int,granted: Boolean,requested: Boolean)=
    if(sdk>=33&&!granted&&!requested) FollowUpPermissionAction.REQUEST else FollowUpPermissionAction.SETTINGS

@Composable
internal fun FollowUpNotificationSettings() {
    val context=LocalContext.current
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    var revision by remember {mutableIntStateOf(0)}
    val prefs=remember(context) {context.getSharedPreferences("followup_notification_ui",0)}
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        revision++;FollowUpIntegration.changed(context)
    }
    DisposableEffect(lifecycle) {
        val observer=LifecycleEventObserver {_,event->if(event==Lifecycle.Event.ON_RESUME) revision++}
        lifecycle.addObserver(observer);onDispose {lifecycle.removeObserver(observer)}
    }
    val allowed=remember(revision) {FollowUpNotifier(context).allowed()}
    FollowUpNotificationRow(allowed) {
        val granted=Build.VERSION.SDK_INT<33||ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED
        when(followUpPermissionAction(Build.VERSION.SDK_INT,granted,prefs.getBoolean("requested",false))) {
            FollowUpPermissionAction.REQUEST->{
                prefs.edit().putBoolean("requested",true).apply()
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            FollowUpPermissionAction.SETTINGS->context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName))
        }
    }
}

@Composable
internal fun FollowUpNotificationRow(allowed: Boolean,onClick: ()->Unit) {
    Text(if(allowed) "Phone notifications allowed" else "Phone notifications are off. Follow-ups remain available in the app.",style=nativeSupportingTextStyle())
    TextButton(onClick=hapticAction(onClick),modifier=Modifier.testTag("followup-notification-settings")) {
        Text(if(allowed) "Notification settings" else "Enable phone notifications")
    }
}
