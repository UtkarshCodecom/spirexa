package com.desire.photos.data.remote

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Mirrors the server's MediaItem JSON shape (see server/src/modules/media/media.service.ts). */
data class MediaDto(
    val id: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    /** Relative, ready-to-use path (already carries a short-lived signed token). */
    val contentPath: String,
    val favorite: Boolean,
    val deleted: Boolean,
    val dateTakenSec: Long,
    val createdAtSec: Long,
    val width: Int = 0,
    val height: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** AI-generated search caption/tags — null/empty until the background analysis finishes. */
    val aiCaption: String? = null,
    val aiTags: List<String> = emptyList(),
    val isDocument: Boolean = false,
    val documentText: String? = null,
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
    val hasLocation: Boolean get() = latitude != null && longitude != null

    companion object {
        fun fromJson(obj: JSONObject): MediaDto {
            val metadata = obj.optJSONObject("metadata")
            val createdAt = parseIsoToEpochSec(obj.optString("createdAt").takeIf { it.isNotBlank() })
            val dateTaken = metadata?.optString("dateTaken")?.takeIf { it.isNotBlank() }
                ?.let(::parseIsoToEpochSec)
                ?.takeIf { it > 0 }
                ?: createdAt
            val location = metadata?.optJSONObject("location")
            val tags = metadata?.optJSONArray("aiTags")
            return MediaDto(
                id = obj.getString("id"),
                fileName = obj.optString("fileName", "file"),
                mimeType = obj.optString("mimeType", "application/octet-stream"),
                sizeBytes = obj.optLong("size", 0L),
                contentPath = obj.optString("storageUrl", ""),
                favorite = obj.optBoolean("favorite", false),
                deleted = obj.optBoolean("deleted", false),
                dateTakenSec = dateTaken,
                createdAtSec = createdAt,
                width = obj.optInt("width", 0),
                height = obj.optInt("height", 0),
                latitude = location?.takeIf { it.has("latitude") }?.optDouble("latitude"),
                longitude = location?.takeIf { it.has("longitude") }?.optDouble("longitude"),
                aiCaption = metadata?.optString("aiCaption")?.takeIf { it.isNotBlank() },
                aiTags = tags?.let { arr -> (0 until arr.length()).map { arr.optString(it, "") } }
                    ?.filter { it.isNotBlank() } ?: emptyList(),
                isDocument = metadata?.optBoolean("isDocument", false) ?: false,
                documentText = metadata?.optString("documentText")?.takeIf { it.isNotBlank() },
            )
        }

        private fun parseIsoToEpochSec(raw: String?): Long {
            if (raw.isNullOrBlank()) return 0L
            return runCatching {
                val trimmed = raw.substringBefore('.').substringBefore('+')
                val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                (fmt.parse(trimmed)?.time ?: 0L) / 1000L
            }.getOrDefault(0L)
        }
    }
}
