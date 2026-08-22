package com.desire.photos.backup

import android.content.Context
import com.desire.photos.auth.AuthRepository
import com.desire.photos.data.local.UploadStore
import com.desire.photos.data.media.MediaRepository
import com.desire.photos.data.model.MediaItem
import com.desire.photos.data.model.UploadRecord
import com.desire.photos.data.model.UploadStatus
import com.desire.photos.data.remote.MediaDto
import com.desire.photos.data.remote.PhotosApiClient
import com.desire.photos.data.settings.BackupSettings
import com.desire.photos.data.settings.LargeMediaPolicy
import com.desire.photos.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayInputStream
import kotlin.random.Random

data class BackupProgress(
    val running: Boolean = false,
    val done: Int = 0,
    val total: Int = 0,
    val currentName: String? = null,
    val message: String? = null,
)

data class BackupSummary(
    val uploaded: Int,
    val skipped: Int,
    val failed: Int,
    val stoppedReason: String? = null,
)

/** name minus extension, lowercased — matches the same photo across devices/degrades. */
private fun signatureOf(name: String): String = name.substringBeforeLast('.').lowercase().trim()

/**
 * Runs one backup pass: newest media first, honoring the user's network,
 * degrade, and "upload large sometimes" preferences. Shared by the manual
 * "Back up now" button and the background [BackupWorker]. Every upload goes
 * through the Photos API server — this app never talks to storage directly.
 */
class BackupManager(
    private val appContext: Context,
    private val auth: AuthRepository,
    private val settingsRepo: SettingsRepository,
    private val mediaRepo: MediaRepository,
    private val api: PhotosApiClient,
    private val store: UploadStore,
) {
    private val mutex = Mutex()

    private val _progress = MutableStateFlow(BackupProgress())
    val progress: StateFlow<BackupProgress> = _progress.asStateFlow()

    /** Fraction (0..100) of "large media, sometimes" that actually gets uploaded. */
    private val sometimesUploadPercent = 50

    suspend fun runBackup(force: Boolean = false): BackupSummary {
        if (mutex.isLocked) return BackupSummary(0, 0, 0, "Already running")
        return mutex.withLock {
            val uid = auth.uid
                ?: return@withLock BackupSummary(0, 0, 0, "Not signed in")
            val settings = settingsRepo.current()

            if (!force) {
                if (!settings.backupEnabled) return@withLock BackupSummary(0, 0, 0, "Backup is off")
                if (!NetworkUtil.isBackupAllowed(appContext, settings))
                    return@withLock BackupSummary(0, 0, 0, "Waiting for an allowed network")
            }

            store.load(uid)
            // Learn what's already backed up (from any device) so we don't re-upload it.
            val remoteBySignature = api.listAllMedia().getOrDefault(emptyList())
                .associateBy { signatureOf(it.fileName) }
            val media = mediaRepo.queryMedia()
            _progress.value = BackupProgress(running = true, total = media.size, message = "Starting…")

            var uploaded = 0
            var skipped = 0
            var failed = 0
            var already = 0

            for ((index, item) in media.withIndex()) {
                _progress.value = _progress.value.copy(
                    done = index, currentName = item.displayName, message = "Backing up…"
                )

                // Re-check the network mid-run so we stop the moment it's no longer allowed.
                if (!force && !NetworkUtil.isBackupAllowed(appContext, settings)) {
                    _progress.value = BackupProgress(message = "Network no longer allowed")
                    return@withLock BackupSummary(uploaded, skipped, failed, "Network changed")
                }

                val existing = store.snapshot()[item.id]
                if (existing?.excluded == true) { skipped++; continue }
                if (existing?.status == UploadStatus.UPLOADED) continue

                // Already backed up from another device (matched by filename)? Record it, don't re-upload.
                val remoteMatch = remoteBySignature[signatureOf(item.displayName)]
                if (remoteMatch != null) {
                    store.upsert(uid, record(item, UploadStatus.UPLOADED, serverId = remoteMatch.id))
                    already++
                    continue
                }

                val decision = decide(item, settings)
                if (decision == Decision.SKIP) {
                    store.upsert(uid, record(item, UploadStatus.SKIPPED))
                    skipped++
                    continue
                }

                store.upsert(uid, record(item, UploadStatus.UPLOADING))
                val result = uploadItem(item, settings, degrade = decision == Decision.DEGRADE)
                result.onSuccess { dto ->
                    store.upsert(
                        uid,
                        record(item, UploadStatus.UPLOADED, serverId = dto.id, uploadedAt = System.currentTimeMillis()),
                    )
                    uploaded++
                }.onFailure { e ->
                    store.upsert(uid, record(item, UploadStatus.FAILED, error = e.message))
                    failed++
                }
            }

            val alreadyNote = if (already > 0) ", $already already backed up" else ""
            _progress.value = BackupProgress(
                running = false, done = media.size, total = media.size,
                message = "Done · $uploaded uploaded, $skipped skipped, $failed failed$alreadyNote",
            )
            BackupSummary(uploaded, skipped, failed)
        }
    }

    /** Builds a record carrying the metadata needed to show cloud-only tiles later. */
    private fun record(
        item: MediaItem,
        status: UploadStatus,
        serverId: String? = null,
        uploadedAt: Long = 0L,
        error: String? = null,
    ) = UploadRecord(
        mediaId = item.id,
        status = status,
        serverId = serverId,
        uploadedAt = uploadedAt,
        sizeBytes = item.sizeBytes,
        error = error,
        displayName = item.displayName,
        isVideo = item.isVideo,
        dateAddedSec = item.dateAddedSec,
    )

    private enum class Decision { UPLOAD, DEGRADE, SKIP }

    private fun decide(item: MediaItem, settings: BackupSettings): Decision {
        // Never upload videos if the user turned videos off.
        if (item.isVideo && !settings.backupVideos) return Decision.SKIP
        // Never upload files above the user's size limit (0 = no limit).
        if (settings.maxUploadMb > 0 && item.sizeBytes > settings.maxUploadMb * 1024L * 1024L) {
            return Decision.SKIP
        }
        if (!item.isLarge) return Decision.UPLOAD
        // Large file rules:
        return when (settings.largeMediaPolicy) {
            LargeMediaPolicy.NEVER -> Decision.SKIP
            LargeMediaPolicy.SOMETIMES ->
                if (Random.nextInt(100) < sometimesUploadPercent) largeUploadMode(item, settings)
                else Decision.SKIP
            LargeMediaPolicy.ALWAYS -> largeUploadMode(item, settings)
        }
    }

    private fun largeUploadMode(item: MediaItem, settings: BackupSettings): Decision =
        if (!item.isVideo && settings.degradeLargeMedia) Decision.DEGRADE else Decision.UPLOAD

    private suspend fun uploadItem(
        item: MediaItem,
        settings: BackupSettings,
        degrade: Boolean,
    ): Result<MediaDto> {
        val safeName = item.displayName.replace('/', '_')
        val resolver = appContext.contentResolver

        // Best-effort — most photos have no GPS tag, and reading it needs the
        // (optional) ACCESS_MEDIA_LOCATION permission; either way this just
        // silently comes back null rather than blocking the upload.
        val location = if (!item.isVideo) mediaRepo.readExifLocation(item.uri) else null

        if (degrade && !item.isVideo) {
            val bytes = MediaDegrader.degradeImage(
                resolver, item.uri, settings.maxImageDimension, settings.imageQuality
            )
            if (bytes != null) {
                val jpgName = safeName.substringBeforeLast('.') + ".jpg"
                return api.uploadMedia(
                    jpgName, "image/jpeg", bytes.size.toLong(), item.dateAddedSec,
                    latitude = location?.first, longitude = location?.second,
                ) { ByteArrayInputStream(bytes) }
            }
            // Fall through to original if degrade failed.
        }

        return api.uploadMedia(
            safeName, item.mimeType, item.sizeBytes, item.dateAddedSec,
            latitude = location?.first, longitude = location?.second,
        ) {
            resolver.openInputStream(item.uri)
                ?: throw java.io.IOException("Cannot open ${item.uri}")
        }
    }
}
