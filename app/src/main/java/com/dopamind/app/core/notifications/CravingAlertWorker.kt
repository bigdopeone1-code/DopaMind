package com.dopamind.app.core.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dopamind.app.DopaMindApplication
import com.dopamind.app.R
import com.dopamind.app.core.ai.craving.TrackedModule

private const val ALERT_WINDOW_MILLIS = 60 * 60 * 1000L // fires up to 1h ahead of a predicted peak
private const val MIN_CONFIDENCE = 0.35f

/**
 * Runs roughly hourly; checks [com.dopamind.app.core.ai.craving.CravingPredictionEngine]'s
 * predictions and notifies only if a predicted peak for the user's own historical pattern
 * is coming up within the next hour, with reasonable confidence.
 */
class CravingAlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as DopaMindApplication).container
        val profile = container.profileRepository.getProfileOnce()
        if (profile?.notificationsEnabled != true) return Result.success()

        val now = System.currentTimeMillis()
        val predictions = container.cravingPredictionEngine.predictAll(now)

        val upcoming = predictions.firstOrNull { prediction ->
            prediction.confidence >= MIN_CONFIDENCE &&
                prediction.nextPredictedEpochMillis != null &&
                prediction.nextPredictedEpochMillis - now in 0..ALERT_WINDOW_MILLIS
        }

        if (upcoming != null) {
            val moduleLabel = applicationContext.getString(moduleLabelRes(upcoming.module))
            val body = applicationContext.getString(R.string.notif_craving_alert_body, moduleLabel)
            NotificationHelper.showCravingAlert(applicationContext, body)
        }

        return Result.success()
    }

    private fun moduleLabelRes(module: TrackedModule): Int = when (module) {
        TrackedModule.CANNABIS -> R.string.module_cannabis
        TrackedModule.TOBACCO -> R.string.module_tobacco
        TrackedModule.ALCOHOL -> R.string.module_alcohol
    }
}
