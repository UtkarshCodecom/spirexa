package com.desire.photos.data.remote

import com.desire.photos.auth.AuthRepository
import com.desire.photos.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.source
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream

data class MediaPage(val items: List<MediaDto>, val nextCursor: String?)

/**
 * Talks to the Photos API server — the only thing this app talks to for media,
 * besides Firebase Auth (for the ID token used to authenticate every call
 * here). The app never holds a storage credential; the server is the only
 * thing that knows how to reach Backblaze B2.
 */
class PhotosApiClient(
    private val client: OkHttpClient,
    private val auth: AuthRepository,
) {
    private val base get() = AppConfig.apiBaseUrl

    private suspend fun authHeader(): String {
        val token = auth.idToken() ?: throw IOException("Not signed in")
        return "Bearer $token"
    }

    /**
     * Streams [openStream] straight into a multipart POST — the file's bytes
     * are read on demand as OkHttp writes the request, never buffered whole in
     * memory. Metadata fields are written before the file part, matching what
     * the server's busboy handler expects.
     */
    suspend fun uploadMedia(
        fileName: String,
        mimeType: String,
        sizeBytes: Long,
        takenAtSec: Long,
        latitude: Double? = null,
        longitude: Double? = null,
        openStream: () -> InputStream,
    ): Result<MediaDto> = withContext(Dispatchers.IO) {
        runCatching {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("fileName", fileName)
                .addFormDataPart("mimeType", mimeType)
                .addFormDataPart("size", sizeBytes.toString())
                .apply {
                    if (takenAtSec > 0) {
                        addFormDataPart("takenAt", isoInstant(takenAtSec))
                    }
                    if (latitude != null && longitude != null) {
                        addFormDataPart("latitude", latitude.toString())
                        addFormDataPart("longitude", longitude.toString())
                    }
                }
                .addFormDataPart(
                    "file",
                    fileName,
                    StreamRequestBody(mimeType.toMediaTypeOrNull(), sizeBytes, openStream),
                )
                .build()

            val request = Request.Builder()
                .url("$base/api/media/upload")
                .header("Authorization", authHeader())
                .post(body)
                .build()

            client.newCall(request).execute().use { resp ->
                val json = resp.bodyJsonObject()
                if (!resp.isSuccessful) throw ApiException(resp.code, json.errorMessage())
                MediaDto.fromJson(json.getJSONObject("data"))
            }
        }
    }

    suspend fun listMedia(cursor: String? = null, limit: Int = 200): Result<MediaPage> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = "$base/api/media?limit=$limit" + (cursor?.let { "&cursor=$it" } ?: "")
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", authHeader())
                    .get()
                    .build()

                client.newCall(request).execute().use { resp ->
                    val json = resp.bodyJsonObject()
                    if (!resp.isSuccessful) throw ApiException(resp.code, json.errorMessage())
                    val data = json.getJSONObject("data")
                    val array: JSONArray = data.getJSONArray("data")
                    val items = (0 until array.length()).map { MediaDto.fromJson(array.getJSONObject(it)) }
                    val nextCursor = data.optString("nextCursor").takeIf { it.isNotBlank() }
                    MediaPage(items, nextCursor)
                }
            }
        }

    /** Fetches every page — fine for a personal library; paginate in the UI if this grows huge. */
    suspend fun listAllMedia(): Result<List<MediaDto>> = withContext(Dispatchers.IO) {
        runCatching {
            val all = mutableListOf<MediaDto>()
            var cursor: String? = null
            do {
                val page = listMedia(cursor).getOrThrow()
                all += page.items
                cursor = page.nextCursor
            } while (cursor != null)
            all
        }
    }

    /** Creates a shareable, no-login-required link for the given (already backed-up) media ids. */
    suspend fun createShare(title: String, mediaIds: List<String>): Result<ShareDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = JSONObject().apply {
                    put("title", title)
                    put("mediaIds", JSONArray(mediaIds))
                }
                val request = Request.Builder()
                    .url("$base/api/shares")
                    .header("Authorization", authHeader())
                    .post(body.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()

                client.newCall(request).execute().use { resp ->
                    val json = resp.bodyJsonObject()
                    if (!resp.isSuccessful) throw ApiException(resp.code, json.errorMessage())
                    ShareDto.fromJson(json.getJSONObject("data"))
                }
            }
        }

    suspend fun listMyShares(): Result<List<ShareDto>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$base/api/shares")
                .header("Authorization", authHeader())
                .get()
                .build()
            client.newCall(request).execute().use { resp ->
                val json = resp.bodyJsonObject()
                if (!resp.isSuccessful) throw ApiException(resp.code, json.errorMessage())
                val array = json.getJSONArray("data")
                (0 until array.length()).map { ShareDto.fromJson(array.getJSONObject(it)) }
            }
        }
    }

    suspend fun deleteShare(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$base/api/shares/$id")
                .header("Authorization", authHeader())
                .delete()
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) throw ApiException(resp.code, resp.bodyJsonObject().errorMessage())
            }
        }
    }

    /**
     * Fetches a share's public view — deliberately no Authorization header,
     * matching the server's unauthenticated GET /api/shares/:id. Anyone with
     * the link can call this, signed in or not.
     */
    suspend fun getPublicShare(shareId: String): Result<PublicShareDto> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$base/api/shares/$shareId")
                .get()
                .build()
            client.newCall(request).execute().use { resp ->
                val json = resp.bodyJsonObject()
                if (!resp.isSuccessful) throw ApiException(resp.code, json.errorMessage())
                PublicShareDto.fromJson(json.getJSONObject("data"))
            }
        }
    }

    // ---- Albums ("folders") ----

    suspend fun listAlbums(): Result<List<AlbumDto>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$base/api/albums")
                .header("Authorization", authHeader())
                .get()
                .build()
            client.newCall(request).execute().use { resp ->
                val json = resp.bodyJsonObject()
                if (!resp.isSuccessful) throw ApiException(resp.code, json.errorMessage())
                val array = json.getJSONArray("data")
                (0 until array.length()).map { AlbumDto.fromJson(array.getJSONObject(it)) }
            }
        }
    }

    suspend fun createAlbum(title: String): Result<AlbumDto> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().apply { put("title", title) }
            val request = Request.Builder()
                .url("$base/api/albums")
                .header("Authorization", authHeader())
                .post(body.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
            client.newCall(request).execute().use { resp ->
                val json = resp.bodyJsonObject()
                if (!resp.isSuccessful) throw ApiException(resp.code, json.errorMessage())
                AlbumDto.fromJson(json.getJSONObject("data"))
            }
        }
    }

    suspend fun deleteAlbum(albumId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$base/api/albums/$albumId")
                .header("Authorization", authHeader())
                .delete()
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) throw ApiException(resp.code, resp.bodyJsonObject().errorMessage())
            }
        }
    }

    /** Ids of the media currently in the album (order: most recently added first). */
    suspend fun getAlbumMediaIds(albumId: String): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$base/api/albums/$albumId/media")
                .header("Authorization", authHeader())
                .get()
                .build()
            client.newCall(request).execute().use { resp ->
                val json = resp.bodyJsonObject()
                if (!resp.isSuccessful) throw ApiException(resp.code, json.errorMessage())
                val array = json.getJSONArray("data")
                (0 until array.length()).map { array.getJSONObject(it).getString("mediaId") }
            }
        }
    }

    suspend fun addToAlbum(albumId: String, mediaIds: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().apply { put("mediaIds", JSONArray(mediaIds)) }
            val request = Request.Builder()
                .url("$base/api/albums/$albumId/media")
                .header("Authorization", authHeader())
                .post(body.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) throw ApiException(resp.code, resp.bodyJsonObject().errorMessage())
            }
        }
    }

    suspend fun removeFromAlbum(albumId: String, mediaId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$base/api/albums/$albumId/media/$mediaId")
                .header("Authorization", authHeader())
                .delete()
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) throw ApiException(resp.code, resp.bodyJsonObject().errorMessage())
            }
        }
    }

    suspend fun trashMedia(id: String): Result<Unit> = simplePost("$base/api/media/$id/trash")

    suspend fun restoreMedia(id: String): Result<Unit> = simplePost("$base/api/media/$id/restore")

    suspend fun toggleFavorite(id: String): Result<Unit> = simplePost("$base/api/media/$id/favorite")

    /** Triggers server-side AI captioning/document-detection for one photo. Idempotent server-side. */
    suspend fun analyzeMedia(id: String): Result<Unit> = simplePost("$base/api/media/$id/analyze")

    /** Patches in EXIF location for media that was backed up before this was captured. */
    suspend fun updateLocation(id: String, latitude: Double, longitude: Double): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = JSONObject().put(
                    "metadata",
                    JSONObject().put(
                        "location",
                        JSONObject().put("latitude", latitude).put("longitude", longitude),
                    ),
                )
                val request = Request.Builder()
                    .url("$base/api/media/$id")
                    .header("Authorization", authHeader())
                    .patch(body.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()
                client.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) throw ApiException(resp.code, resp.bodyJsonObject().errorMessage())
                }
            }
        }

    suspend fun permanentDelete(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$base/api/media/$id/permanent")
                .header("Authorization", authHeader())
                .delete()
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) throw ApiException(resp.code, resp.bodyJsonObject().errorMessage())
            }
        }
    }

    private suspend fun simplePost(url: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(url)
                .header("Authorization", authHeader())
                .post(ByteArray(0).toRequestBody(null))
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) throw ApiException(resp.code, resp.bodyJsonObject().errorMessage())
            }
        }
    }

    /** Full URL for a media item's content — ready to hand to Coil as-is. */
    fun contentUrl(dto: MediaDto): String = "$base${dto.contentPath}"

    private fun okhttp3.Response.bodyJsonObject(): JSONObject =
        runCatching { JSONObject(body?.string().orEmpty()) }.getOrDefault(JSONObject())

    private fun JSONObject.errorMessage(): String =
        optJSONObject("error")?.optString("message") ?: "Request failed"

    class ApiException(val statusCode: Int, message: String) : IOException(message)

    private fun isoInstant(epochSec: Long): String {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
        fmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return fmt.format(java.util.Date(epochSec * 1000))
    }

    private class StreamRequestBody(
        private val mediaType: okhttp3.MediaType?,
        private val length: Long,
        private val opener: () -> InputStream,
    ) : RequestBody() {
        override fun contentType(): okhttp3.MediaType? = mediaType
        override fun contentLength(): Long = if (length > 0) length else -1
        override fun writeTo(sink: BufferedSink) {
            opener().use { input -> sink.writeAll(input.source()) }
        }
    }
}
