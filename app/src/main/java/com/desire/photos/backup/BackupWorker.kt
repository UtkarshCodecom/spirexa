package com.desire.photos.backup

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.desire.photos.PhotosApp
import com.desire.photos.R
import com.desire.photos.di.ServiceLocator

/**
 * Reliable background backup. Runs as a foreground service so it survives the app
 * being swiped away and isn't capped at the ~10-minute background limit, letting a
 * large backup finish. Shows a low-priority "Backing up…" notification while active.
 */
class BackupWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // Promote to a foreground service; if the OS refuses, fall back to plain background.
        runCatching { setForeground(createForegroundInfo()) }

        val summary = ServiceLocator.backupManager.runBackup()
        return if (summary.failed > 0 && summary.uploaded == 0 && summary.stoppedReason == null) {
            Result.retry()
        } else {
            Result.success()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = createForegroundInfo()

    private fun createForegroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, PhotosApp.BACKUP_CHANNEL_ID)
            .setContentTitle("Backing up photos")
            .setContentText("Uploading your photos to the cloud…")
            .setSmallIcon(R.drawable.ic_backup)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 42
    }
}
