package com.desire.photos.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** How to treat large photos/videos during backup. */
enum class LargeMediaPolicy { ALWAYS, SOMETIMES, NEVER }

data class BackupSettings(
    val backupEnabled: Boolean = true,
    val backupOverWifi: Boolean = true,
    val backupOverMobile: Boolean = true,       // "allow backup on 5G / mobile data" (unlimited 5G → on by default)
    val backupInBackground: Boolean = true,
    val degradeLargeMedia: Boolean = true,       // recompress large images before upload
    val largeMediaPolicy: LargeMediaPolicy = LargeMediaPolicy.SOMETIMES, // "upload sometimes"
    val imageQuality: Int = 70,                  // JPEG quality used when degrading (0..100)
    val maxImageDimension: Int = 2048,           // longest edge (px) when degrading
    val backupVideos: Boolean = true,            // include videos in backup
    val maxUploadMb: Int = 0,                    // skip files larger than this (0 = no limit)
    val themeId: String = "classic",
) {
    /** With everything off there is no network we're allowed to use. */
    val hasAllowedNetwork: Boolean get() = backupOverWifi || backupOverMobile
}

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("backup_settings")

class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<BackupSettings> = dataStore.data.map { p ->
        BackupSettings(
            backupEnabled = p[KEY_ENABLED] ?: true,
            backupOverWifi = p[KEY_WIFI] ?: true,
            backupOverMobile = p[KEY_MOBILE] ?: true,
            backupInBackground = p[KEY_BACKGROUND] ?: true,
            degradeLargeMedia = p[KEY_DEGRADE] ?: true,
            largeMediaPolicy = runCatching {
                LargeMediaPolicy.valueOf(p[KEY_LARGE_POLICY] ?: LargeMediaPolicy.SOMETIMES.name)
            }.getOrDefault(LargeMediaPolicy.SOMETIMES),
            imageQuality = p[KEY_QUALITY] ?: 70,
            maxImageDimension = p[KEY_MAX_DIM] ?: 2048,
            backupVideos = p[KEY_VIDEOS] ?: true,
            maxUploadMb = p[KEY_MAX_MB] ?: 0,
            themeId = p[KEY_THEME] ?: "classic",
        )
    }

    suspend fun current(): BackupSettings = settings.first()

    suspend fun setBackupEnabled(v: Boolean) = dataStore.edit { it[KEY_ENABLED] = v }
    suspend fun setBackupOverWifi(v: Boolean) = dataStore.edit { it[KEY_WIFI] = v }
    suspend fun setBackupOverMobile(v: Boolean) = dataStore.edit { it[KEY_MOBILE] = v }
    suspend fun setBackupInBackground(v: Boolean) = dataStore.edit { it[KEY_BACKGROUND] = v }
    suspend fun setDegradeLargeMedia(v: Boolean) = dataStore.edit { it[KEY_DEGRADE] = v }
    suspend fun setLargeMediaPolicy(v: LargeMediaPolicy) = dataStore.edit { it[KEY_LARGE_POLICY] = v.name }
    suspend fun setBackupVideos(v: Boolean) = dataStore.edit { it[KEY_VIDEOS] = v }
    suspend fun setMaxUploadMb(v: Int) = dataStore.edit { it[KEY_MAX_MB] = v }
    suspend fun setThemeId(v: String) = dataStore.edit { it[KEY_THEME] = v }

    companion object {
        private val KEY_ENABLED = booleanPreferencesKey("backup_enabled")
        private val KEY_WIFI = booleanPreferencesKey("backup_wifi")
        private val KEY_MOBILE = booleanPreferencesKey("backup_mobile")
        private val KEY_BACKGROUND = booleanPreferencesKey("backup_background")
        private val KEY_DEGRADE = booleanPreferencesKey("degrade_large")
        private val KEY_LARGE_POLICY = stringPreferencesKey("large_policy")
        private val KEY_QUALITY = intPreferencesKey("image_quality")
        private val KEY_MAX_DIM = intPreferencesKey("max_dim")
        private val KEY_VIDEOS = booleanPreferencesKey("backup_videos")
        private val KEY_MAX_MB = intPreferencesKey("max_upload_mb")
        private val KEY_THEME = stringPreferencesKey("theme_id")
    }
}
