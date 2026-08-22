package com.desire.photos.data.model

/** Backup state we track locally for each device media item, per signed-in user. */
enum class UploadStatus {
    NOT_UPLOADED,
    PENDING,
    UPLOADING,
    UPLOADED,
    FAILED,
    SKIPPED,   // intentionally not uploaded this pass (e.g. large file, "sometimes" rule)
    EXCLUDED,  // user asked to never back this up
}

data class UploadRecord(
    val mediaId: Long,
    val status: UploadStatus,
    /** The server's media id (opaque UUID) once uploaded — used for trash/favorite/delete calls. */
    val serverId: String? = null,
    val uploadedAt: Long = 0L,
    val sizeBytes: Long = 0L,
    val error: String? = null,
    val excluded: Boolean = false,
    // Metadata kept so a backed-up item can still be shown after it's deleted
    // from the device (cloud-only tile).
    val displayName: String? = null,
    val isVideo: Boolean = false,
    val dateAddedSec: Long = 0L,
)
