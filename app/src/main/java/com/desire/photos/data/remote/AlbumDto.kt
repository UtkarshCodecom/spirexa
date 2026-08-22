package com.desire.photos.data.remote

import org.json.JSONObject

/** Mirrors the server's Album JSON shape (see server/src/modules/albums/albums.service.ts). */
data class AlbumDto(
    val id: String,
    val title: String,
    val description: String?,
    val coverMediaId: String?,
    val mediaCount: Int,
) {
    companion object {
        fun fromJson(obj: JSONObject): AlbumDto = AlbumDto(
            id = obj.getString("id"),
            title = obj.optString("title", "Untitled"),
            description = obj.optString("description").takeIf { it.isNotBlank() },
            coverMediaId = obj.optString("coverMediaId").takeIf { it.isNotBlank() },
            mediaCount = obj.optInt("mediaCount", 0),
        )
    }
}
