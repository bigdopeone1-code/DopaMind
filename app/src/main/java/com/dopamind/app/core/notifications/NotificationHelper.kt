package com.dopamind.app.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dopamind.app.MainActivity
import com.dopamind.app.R

/**
 * Thin wrapper around local notifications — everything here is device-local;
 * no push service, no server involved.
 */
object NotificationHelper {

    const val CHANNEL_REMINDERS = "dopamind_reminders"
    const val CHANNEL_CRAVING_ALERTS = "dopamind_craving_alerts"

    private const val NOTIFICATION_ID_DAILY_REMINDER = 1001
    private const val NOTIFICATION_ID_CRAVING_ALERT = 1002

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDERS,
                context.getString(R.string.notif_channel_reminders_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.notif_channel_reminders_description) }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CRAVING_ALERTS,
                context.getString(R.string.notif_channel_craving_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.notif_channel_craving_description) }
        )
    }

    private fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    fun showDailyCheckInReminder(context: Context) {
        if (!hasPermission(context)) return
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notif_daily_reminder_title))
            .setContentText(context.getString(R.string.notif_daily_reminder_body))
            .setAutoCancel(true)
            .setContentIntent(MainActivityPendingIntent.get(context))
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_DAILY_REMINDER, notification)
    }

    fun showCravingAlert(context: Context, bodyText: String) {
        if (!hasPermission(context)) return
        val notification = NotificationCompat.Builder(context, CHANNEL_CRAVING_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notif_craving_alert_title))
            .setContentText(bodyText)
            .setAutoCancel(true)
            .setContentIntent(MainActivityPendingIntent.get(context))
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_CRAVING_ALERT, notification)
    }
}

private object MainActivityPendingIntent {
    fun get(context: Context): android.app.PendingIntent {
        val intent = android.content.Intent(context, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        return android.app.PendingIntent.getActivity(context, 0, intent, flags)
    }
}
