package com.desire.photos.data.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.desire.photos.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads photos and videos off the device via MediaStore, newest first. */
class MediaRepository(context: Context) {

    private val appContext = context.applicationContext

    /**
     * Returns device media sorted by date-added descending (most recent first),
     * so backup naturally starts from the newest photos.
     */
    suspend fun queryMedia(limit: Int = 0): List<MediaItem> = withContext(Dispatchers.IO) {
        val result = ArrayList<MediaItem>()
        result += queryCollection(isVideo = false)
        result += queryCollection(isVideo = true)
        result.sortByDescending { it.dateAddedSec }
        if (limit > 0 && result.size > limit) result.subList(0, limit) else result
    }

    private fun queryCollection(isVideo: Boolean): List<MediaItem> {
        val collection = if (isVideo) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
        )
        if (isVideo) projection += MediaStore.Video.VideoColumns.DURATION

        val items = ArrayList<MediaItem>()
        appContext.contentResolver.query(
            collection,
            projection.toTypedArray(),
            null,
            null,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val bucketCol = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val durationCol = if (isVideo)
                cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION) else -1

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                items += MediaItem(
                    id = id,
                    uri = uri,
                    displayName = cursor.getString(nameCol) ?: "media_$id",
                    sizeBytes = cursor.getLong(sizeCol),
                    dateAddedSec = cursor.getLong(dateCol),
                    mimeType = cursor.getString(mimeCol) ?: if (isVideo) "video/*" else "image/*",
                    isVideo = isVideo,
                    durationMs = if (durationCol >= 0) cursor.getLong(durationCol) else 0L,
                    bucket = if (bucketCol >= 0) cursor.getString(bucketCol) else null,
                )
            }
        }
        return items
    }

    /**
     * Reads GPS coordinates from a photo's EXIF data, or null if it has none
     * (most photos don't, or the file has been stripped of location data).
     * Called on-demand per item during backup — not during [queryMedia], so
     * scanning the whole library stays fast.
     *
     * On Android 10+ the media provider redacts location EXIF from the plain
     * content URI by default (privacy); [MediaStore.setRequireOriginal] asks
     * for the unredacted original, which needs the ACCESS_MEDIA_LOCATION
     * permission — if that's not granted this silently returns null instead
     * of crashing, so location is simply best-effort.
     */
    suspend fun readExifLocation(uri: Uri): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        runCatching {
            val target = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.setRequireOriginal(uri)
            } else {
                uri
            }
            appContext.contentResolver.openInputStream(target)?.use { stream ->
                val latLong = ExifInterface(stream).latLong
                latLong?.let { it[0] to it[1] }
            }
        }.getOrNull()
    }
}
