package com.desire.photos.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desire.photos.data.model.MediaItem
import com.desire.photos.data.model.UploadRecord
import com.desire.photos.data.model.UploadStatus
import com.desire.photos.data.remote.MediaDto
import com.desire.photos.data.remote.ShareDto
import com.desire.photos.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class HomeFilter { ALL, UPLOADED, EXCLUDED }

data class MediaUi(
    val item: MediaItem,
    val record: UploadRecord?,
    /** true = still on the device; false = only exists in the cloud backup. */
    val isLocal: Boolean = true,
    /** Model Coil should load for a cloud-only image (null for cloud-only video). */
    val cloudModel: String? = null,
    val favorite: Boolean = false,
) {
    val status: UploadStatus
        get() = record?.status ?: UploadStatus.NOT_UPLOADED
}

/** name minus extension, lowercased — matches the same photo across devices/degrades. */
internal fun signatureOf(name: String): String = name.substringBeforeLast('.').lowercase().trim()

class HomeViewModel : ViewModel() {

    private val mediaRepo = ServiceLocator.mediaRepository
    private val store = ServiceLocator.uploadStore
    private val auth = ServiceLocator.authRepository
    private val backup = ServiceLocator.backupManager
    private val api = ServiceLocator.apiClient
    private val settingsRepo = ServiceLocator.settingsRepository
    private val scheduler = ServiceLocator.backupScheduler

    private val _media = MutableStateFlow<List<MediaItem>>(emptyList())
    private val _remote = MutableStateFlow<List<MediaDto>>(emptyList())
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    val progress = backup.progress
    val selection = MutableStateFlow<Set<Long>>(emptySet())
    val filter = MutableStateFlow(HomeFilter.ALL)

    /**
     * Device media + the server's media list, merged into one newest-first
     * timeline. The server is authoritative for backup state, so this looks
     * the same on every device the user signs in from.
     */
    val items: StateFlow<List<MediaUi>> =
        combine(_media, store.records, _remote, filter) { media, records, remote, f ->
            val remoteBySignature = remote.filter { !it.deleted }.associateBy { signatureOf(it.fileName) }
            val list = ArrayList<MediaUi>(media.size + remote.size)
            val matchedSignatures = HashSet<String>()

            for (m in media) {
                val sig = signatureOf(m.displayName)
                val dto = remoteBySignature[sig]
                if (dto != null) matchedSignatures.add(sig)

                val localRec = records[m.id]
                val excluded = localRec?.excluded == true
                val uploaded = localRec?.status == UploadStatus.UPLOADED || dto != null
                val status = when {
                    excluded -> UploadStatus.EXCLUDED
                    uploaded -> UploadStatus.UPLOADED
                    else -> localRec?.status ?: UploadStatus.NOT_UPLOADED
                }
                list.add(
                    MediaUi(
                        item = m,
                        record = UploadRecord(
                            mediaId = m.id,
                            status = status,
                            serverId = localRec?.serverId ?: dto?.id,
                            excluded = excluded,
                        ),
                        isLocal = true,
                        favorite = dto?.favorite == true,
                    )
                )
            }

            // Backed-up files with no matching on-device photo → cloud-only tiles.
            for (dto in remote) {
                val sig = signatureOf(dto.fileName)
                if (dto.deleted || sig in matchedSignatures) continue
                val stableId = -abs(dto.id.hashCode().toLong()) - 1 // negative → never clashes with MediaStore ids
                val synthetic = MediaItem(
                    id = stableId,
                    uri = Uri.EMPTY,
                    displayName = dto.fileName,
                    sizeBytes = dto.sizeBytes,
                    dateAddedSec = dto.dateTakenSec,
                    mimeType = dto.mimeType,
                    isVideo = dto.isVideo,
                )
                list.add(
                    MediaUi(
                        item = synthetic,
                        record = UploadRecord(
                            mediaId = stableId,
                            status = UploadStatus.UPLOADED,
                            serverId = dto.id,
                        ),
                        isLocal = false,
                        cloudModel = if (!dto.isVideo) api.contentUrl(dto) else null,
                        favorite = dto.favorite,
                    )
                )
            }

            list.sortByDescending { it.item.dateAddedSec }
            list.filter { ui ->
                when (f) {
                    HomeFilter.ALL -> true
                    HomeFilter.UPLOADED -> ui.status == UploadStatus.UPLOADED
                    HomeFilter.EXCLUDED -> ui.record?.excluded == true
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val stats: StateFlow<Pair<Int, Int>> =
        combine(_media, store.records, _remote) { media, records, remote ->
            val remoteSignatures = remote.filter { !it.deleted }.mapTo(HashSet()) { signatureOf(it.fileName) }
            val matched = HashSet<String>()
            var backedUp = 0
            for (m in media) {
                val sig = signatureOf(m.displayName)
                val inCloud = sig in remoteSignatures
                if (inCloud) matched.add(sig)
                if (records[m.id]?.status == UploadStatus.UPLOADED || inCloud) backedUp++
            }
            val cloudOnly = remoteSignatures.count { it !in matched }
            (backedUp + cloudOnly) to (media.size + cloudOnly)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0 to 0)

    val uploadedCount: StateFlow<Int> =
        stats.map { it.first }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val totalCount: StateFlow<Int> =
        stats.map { it.second }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            auth.uid?.let { store.load(it) }
            _media.value = mediaRepo.queryMedia()
            api.listAllMedia().onSuccess { _remote.value = it }
            // Register the background (idle) backup job as soon as we're signed in,
            // so it keeps running even after the app is swiped away.
            scheduler.applySettings(settingsRepo.current())
            _loading.value = false
        }
    }

    /** Drops one item from the in-memory remote list right away, no refetch — used after deleting from the photo viewer. */
    fun removeFromRemote(serverId: String) {
        _remote.value = _remote.value.filterNot { it.id == serverId }
    }

    fun backupNow() {
        viewModelScope.launch {
            // Explicit user action: run right now, ignoring the enabled/network gates.
            backup.runBackup(force = true)
            api.listAllMedia().onSuccess { _remote.value = it }
        }
    }

    fun toggleSelect(id: Long) {
        val cur = selection.value.toMutableSet()
        if (!cur.add(id)) cur.remove(id)
        selection.value = cur
    }

    fun clearSelection() { selection.value = emptySet() }

    fun excludeSelected() = updateExclusion(true)
    fun includeSelected() = updateExclusion(false)

    /** Exclusions are a per-device preference — they don't sync across devices. */
    private fun updateExclusion(excluded: Boolean) {
        val uid = auth.uid ?: return
        val ids = selection.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            store.setExcluded(uid, ids, excluded)
            clearSelection()
        }
    }

    /** Server ids of the current selection that are actually backed up — only these can be shared. */
    fun selectedServerIds(): List<String> {
        val current = items.value
        return selection.value.mapNotNull { id -> current.firstOrNull { it.item.id == id }?.record?.serverId }
    }

    /**
     * Creates a public, no-login-required link for the selection's backed-up
     * items. Items that haven't finished uploading yet are silently skipped
     * (the caller should check [selectedServerIds] first to warn about that).
     */
    fun createShare(title: String, onResult: (Result<ShareDto>) -> Unit) {
        val serverIds = selectedServerIds()
        if (serverIds.isEmpty()) {
            onResult(Result.failure(IllegalStateException("Nothing here is backed up yet")))
            return
        }
        viewModelScope.launch {
            val result = api.createShare(title.ifBlank { "Shared photos" }, serverIds)
            if (result.isSuccess) clearSelection()
            onResult(result)
        }
    }

    /**
     * Remove the selected items' copies from the server (and B2 storage).
     * This never touches the on-device files — it only removes the online backup.
     */
    fun deleteSelectedFromCloud() {
        val uid = auth.uid ?: return
        val ids = selection.value
        if (ids.isEmpty()) return
        val current = items.value
        viewModelScope.launch {
            for (id in ids) {
                val ui = current.firstOrNull { it.item.id == id } ?: continue
                ui.record?.serverId?.let { api.permanentDelete(it) }
                store.delete(uid, id)
            }
            api.listAllMedia().onSuccess { _remote.value = it }
            clearSelection()
        }
    }
}
