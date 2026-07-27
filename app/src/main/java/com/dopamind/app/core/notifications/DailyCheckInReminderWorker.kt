package com.dopamind.app.core.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dopamind.app.DopaMindApplication
import java.time.LocalDate
import java.time.ZoneId

/** Runs once a day; only actually notifies if today's Daily Vibe Check-in hasn't been done yet. */
class DailyCheckInReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as DopaMindApplication).container
        val profile = container.profileRepository.getProfileOnce()
        if (profile?.notificationsEnabled != true) return Result.success()

        val todayEpochDay = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        val alreadyCheckedIn = container.dailyVibeRepository.getForDate(todayEpochDay) != null
        if (!alreadyCheckedIn) {
            NotificationHelper.showDailyCheckInReminder(applicationContext)
        }
        return Result.success()
    }
}
