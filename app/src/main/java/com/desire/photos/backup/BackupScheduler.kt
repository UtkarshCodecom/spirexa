package com.desire.photos.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.desire.photos.data.settings.BackupSettings
import java.util.concurrent.TimeUnit

/** Schedules the periodic background backup and one-off "back up now" runs. */
class BackupScheduler(context: Context) {

    private val appContext = context.applicationContext

    // Resolve WorkManager lazily and defensively so a mis-init never crashes the UI.
    private val workManager: WorkManager?
        get() = runCatching { WorkManager.getInstance(appContext) }.getOrNull()

    /**
     * Re-evaluate the periodic job whenever settings change. Reliable mode: runs a
     * foreground-service backup whenever the allowed network is up and the battery
     * isn't low — surviving app-kill and completing large backups. A low-priority
     * notification shows while it works.
     */
    fun applySettings(settings: BackupSettings) {
        val wm = workManager ?: return
        runCatching {
            if (!settings.backupEnabled || !settings.backupInBackground || !settings.hasAllowedNetwork) {
                wm.cancelUniqueWork(PERIODIC_WORK)
                return
            }
            val networkType = if (settings.backupOverMobile) NetworkType.CONNECTED else NetworkType.UNMETERED
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<BackupWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            wm.enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }

    /** Kick off an immediate background backup (used by the "Back up now" button). */
    fun backupNow() {
        val wm = workManager ?: return
        runCatching {
            val request = OneTimeWorkRequestBuilder<BackupWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            wm.enqueueUniqueWork(ONE_TIME_WORK, ExistingWorkPolicy.KEEP, request)
        }
    }

    fun cancelAll() {
        val wm = workManager ?: return
        runCatching {
            wm.cancelUniqueWork(PERIODIC_WORK)
            wm.cancelUniqueWork(ONE_TIME_WORK)
        }
    }

    companion object {
        private const val PERIODIC_WORK = "periodic_backup"
        private const val ONE_TIME_WORK = "one_time_backup"
    }
}
