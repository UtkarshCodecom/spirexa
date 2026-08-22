package com.desire.photos.data.remote

import org.json.JSONObject

/** A share the signed-in user created — mirrors server's shares.service.ts `Share`. */
data class ShareDto(
    val id: String,
    val title: String,
    val mediaCount: Int,
    val shareUrl: String,
) {
    companion object {
        fun fromJson(obj: JSONObject): ShareDto = ShareDto(
            id = obj.getString("id"),
            title = obj.optString("title", ""),
            mediaCount = obj.optInt("mediaCount", 0),
            shareUrl = obj.optString("shareUrl", ""),
        )
    }
}

/** The public, no-login view of a share — mirrors server's `SharePublicView`. */
data class PublicShareDto(
    val id: String,
    val title: String,
    val media: List<MediaDto>,
) {
    companion object {
        fun fromJson(obj: JSONObject): PublicShareDto {
            val mediaArray = obj.optJSONArray("media")
            val media = mediaArray?.let { arr ->
                (0 until arr.length()).map { MediaDto.fromJson(arr.getJSONObject(it)) }
            } ?: emptyList()
            return PublicShareDto(
                id = obj.getString("id"),
                title = obj.optString("title", "Shared photos"),
                media = media,
            )
        }
    }
}
