package com.saaf.milkshop.sync

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.saaf.milkshop.nativeui.NativePreviewActivity

/** Generic owner digest; no customer identity, amount, message text or financial extras. */
internal class FollowUpNotifier(private val context: Context,private val channel: String=CHANNEL): FollowUpAlertSink {
    private val manager get()=context.getSystemService(NotificationManager::class.java)
    override fun allowed(): Boolean {
        if(!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return false
        return manager.getNotificationChannel(channel)?.importance!=NotificationManager.IMPORTANCE_NONE
    }
    internal fun destinationIntent()=Intent(context,NativePreviewActivity::class.java).putExtra(EXTRA_DESTINATION,"followups")
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    internal fun notification(): Notification {
        manager.createNotificationChannel(NotificationChannel(channel,"Credit follow-ups",NotificationManager.IMPORTANCE_DEFAULT).apply {
            description="A daily owner reminder to review current credit records"
            lockscreenVisibility=Notification.VISIBILITY_PRIVATE
        })
        val open=PendingIntent.getActivity(context,ID,destinationIntent(),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(context,channel).setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Credit follow-ups need review").setContentText("Open the app to check current records")
            .setCategory(NotificationCompat.CATEGORY_REMINDER).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(open).setAutoCancel(true).build()
    }
    override fun post() {
        if(!allowed()) return
        try {NotificationManagerCompat.from(context).notify(ID,notification())} catch(_: SecurityException) {/* Permission changed after reservation: no replay. */}
    }
    override fun cancel() {manager.cancel(ID)}
    companion object {
        const val CHANNEL="credit_followups"
        const val ID=1043
        const val EXTRA_DESTINATION="com.saaf.milkshop.OPEN_FOLLOWUPS"
    }
}
