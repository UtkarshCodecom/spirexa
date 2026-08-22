package com.desire.photos

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.desire.photos.di.ServiceLocator

class PhotosApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                BACKUP_CHANNEL_ID,
                "Photo backup",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Shown while your photos are backing up." }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    // Let Coil render video frames as thumbnails. Cloud content URLs already
    // carry a short-lived signed token from the server, so no auth header is
    // needed here — the app holds no storage credential of any kind.
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .crossfade(true)
            .build()

    companion object {
        const val BACKUP_CHANNEL_ID = "backup"
    }
}
