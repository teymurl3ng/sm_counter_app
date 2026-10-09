package com.saaf.milkshop.sync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Owner-facing attention notification for the sync queue, mirroring the
 * cloud backup notifier: silent unless notifications are granted, one
 * channel, alerting only when a push halts and needs owner action.
 */
class SyncNotifier(private val context: Context) {

    fun pushHalted(reasonCode: String, message: String) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Sync attention", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Alerts only when data sync needs owner action"
                },
            )
        }
        val openApp = PendingIntent.getActivity(
            context,
            0,
            // M27: the retired WebView shell is no longer a notification
            // target — taps land on the native launcher activity.
            Intent(context, com.saaf.milkshop.nativeui.NativePreviewActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Data sync needs attention")
            .setContentText("Uploads paused: $message ($reasonCode)")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Uploads paused: $message ($reasonCode)"))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "sync_attention"
        private const val NOTIFICATION_ID = 1041
    }
}
