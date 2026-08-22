package com.desire.photos.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desire.photos.data.settings.BackupSettings
import com.desire.photos.data.settings.LargeMediaPolicy
import com.desire.photos.di.ServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    private val repo = ServiceLocator.settingsRepository
    private val scheduler = ServiceLocator.backupScheduler
    private val auth = ServiceLocator.authRepository

    val settings: StateFlow<BackupSettings> =
        repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, BackupSettings())

    val userEmail: String? get() = auth.currentUser?.email
    val userId: String? get() = auth.uid

    init {
        // Keep the background schedule in sync with whatever the user chooses.
        viewModelScope.launch {
            repo.settings.collect { scheduler.applySettings(it) }
        }
    }

    fun setEnabled(v: Boolean) = viewModelScope.launch { repo.setBackupEnabled(v) }
    fun setWifi(v: Boolean) = viewModelScope.launch { repo.setBackupOverWifi(v) }
    fun setMobile(v: Boolean) = viewModelScope.launch { repo.setBackupOverMobile(v) }
    fun setBackground(v: Boolean) = viewModelScope.launch { repo.setBackupInBackground(v) }
    fun setDegrade(v: Boolean) = viewModelScope.launch { repo.setDegradeLargeMedia(v) }
    fun setLargePolicy(v: LargeMediaPolicy) = viewModelScope.launch { repo.setLargeMediaPolicy(v) }
    fun setBackupVideos(v: Boolean) = viewModelScope.launch { repo.setBackupVideos(v) }
    fun setMaxUploadMb(v: Int) = viewModelScope.launch { repo.setMaxUploadMb(v) }
    fun setThemeId(v: String) = viewModelScope.launch { repo.setThemeId(v) }

    fun signOut() {
        scheduler.cancelAll()
        auth.signOut()
    }
}
