package com.desire.photos.data.model

import android.net.Uri

/** A photo or video that lives on the device (backed by MediaStore). */
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val dateAddedSec: Long,
    val mimeType: String,
    val isVideo: Boolean,
    val durationMs: Long = 0L,
    /** MediaStore's folder name (e.g. "Camera", "WhatsApp Images", "Screenshots") — powers smart albums. */
    val bucket: String? = null,
) {
    val isLarge: Boolean
        get() = sizeBytes >= LARGE_BYTES

    companion object {
        /** Files at/above this size are considered "large" for degrade/skip rules. */
        const val LARGE_BYTES: Long = 15L * 1024 * 1024 // 15 MB
    }
}
