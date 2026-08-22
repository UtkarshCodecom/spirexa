package com.desire.photos.di

import android.content.Context
import com.desire.photos.auth.AuthRepository
import com.desire.photos.backup.BackupManager
import com.desire.photos.backup.BackupScheduler
import com.desire.photos.data.local.UploadStore
import com.desire.photos.data.media.MediaRepository
import com.desire.photos.data.remote.PhotosApiClient
import com.desire.photos.data.settings.SettingsRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Tiny manual dependency container. Manual (rather than Hilt/Dagger) so the
 * build needs no annotation processor on this alpha toolchain.
 */
object ServiceLocator {

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(0, TimeUnit.SECONDS)   // large video uploads: no write timeout
            .callTimeout(0, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    val authRepository: AuthRepository by lazy { AuthRepository() }
    val apiClient: PhotosApiClient by lazy { PhotosApiClient(okHttpClient, authRepository) }
    val uploadStore: UploadStore by lazy { UploadStore(appContext) }
    val mediaRepository: MediaRepository by lazy { MediaRepository(appContext) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
    val backupScheduler: BackupScheduler by lazy { BackupScheduler(appContext) }

    val backupManager: BackupManager by lazy {
        BackupManager(
            appContext = appContext,
            auth = authRepository,
            settingsRepo = settingsRepository,
            mediaRepo = mediaRepository,
            api = apiClient,
            store = uploadStore,
        )
    }
}
