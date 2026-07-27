package com.dopamind.app.core.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

private const val WORK_NAME_DAILY_REMINDER = "dopamind_daily_checkin_reminder"
private const val WORK_NAME_CRAVING_ALERTS = "dopamind_craving_alerts"

/** Schedules/cancels the two background WorkManager jobs that back local notifications. */
class NotificationScheduler(private val context: Context) {

    fun scheduleDailyCheckInReminder(hour: Int) {
        NotificationHelper.ensureChannels(context)

        val now = ZonedDateTime.now(ZoneId.systemDefault())
        var target = ZonedDateTime.of(LocalDate.now(), LocalTime.of(hour.coerceIn(0, 23), 0), ZoneId.systemDefault())
        if (target.isBefore(now)) target = target.plusDays(1)
        val initialDelay = Duration.between(now, target).toMillis()

        val request = PeriodicWorkRequestBuilder<DailyCheckInReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME_DAILY_REMINDER, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun scheduleCravingAlerts() {
        NotificationHelper.ensureChannels(context)

        val request = PeriodicWorkRequestBuilder<CravingAlertWorker>(1, TimeUnit.HOURS).build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME_CRAVING_ALERTS, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancelAll() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_DAILY_REMINDER)
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_CRAVING_ALERTS)
    }
}
