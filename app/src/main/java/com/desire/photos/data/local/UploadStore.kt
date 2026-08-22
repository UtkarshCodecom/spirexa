package com.desire.photos.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.desire.photos.data.model.UploadRecord
import com.desire.photos.data.model.UploadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Local, per-user record of which media items have been backed up.
 *
 * Implemented with a plain [SQLiteOpenHelper] on purpose: it needs no annotation
 * processor (KSP/KAPT), which keeps the build robust on this bleeding-edge
 * AGP/Kotlin toolchain. Rows are namespaced by Firebase uid so two accounts on
 * the same device never see each other's backup state.
 *
 * We also keep a little metadata (name / isVideo / dateAdded) so a backed-up
 * item can still be shown after the user deletes it from device storage.
 */
class UploadStore(context: Context) : SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    private val _records = MutableStateFlow<Map<Long, UploadRecord>>(emptyMap())
    /** Reactive view of the currently-loaded user's records, keyed by mediaId. */
    val records: StateFlow<Map<Long, UploadRecord>> = _records.asStateFlow()

    private var loadedUid: String? = null

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE (
                uid TEXT NOT NULL,
                media_id INTEGER NOT NULL,
                status TEXT NOT NULL,
                remote_path TEXT,
                uploaded_at INTEGER NOT NULL DEFAULT 0,
                size_bytes INTEGER NOT NULL DEFAULT 0,
                error TEXT,
                excluded INTEGER NOT NULL DEFAULT 0,
                display_name TEXT,
                is_video INTEGER NOT NULL DEFAULT 0,
                date_added INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(uid, media_id)
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN display_name TEXT")
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN is_video INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN date_added INTEGER NOT NULL DEFAULT 0")
        }
    }

    private fun ContentValues.putRecord(uid: String, r: UploadRecord) {
        put("uid", uid)
        put("media_id", r.mediaId)
        put("status", r.status.name)
        put("remote_path", r.serverId)
        put("uploaded_at", r.uploadedAt)
        put("size_bytes", r.sizeBytes)
        put("error", r.error)
        put("excluded", if (r.excluded) 1 else 0)
        put("display_name", r.displayName)
        put("is_video", if (r.isVideo) 1 else 0)
        put("date_added", r.dateAddedSec)
    }

    /** Load all rows for [uid] into the reactive cache. Call after sign-in. */
    suspend fun load(uid: String) = withContext(Dispatchers.IO) {
        loadedUid = uid
        val map = LinkedHashMap<Long, UploadRecord>()
        readableDatabase.query(
            TABLE, null, "uid = ?", arrayOf(uid), null, null, "date_added DESC"
        ).use { c ->
            val idIdx = c.getColumnIndexOrThrow("media_id")
            val statusIdx = c.getColumnIndexOrThrow("status")
            val remoteIdx = c.getColumnIndexOrThrow("remote_path")
            val atIdx = c.getColumnIndexOrThrow("uploaded_at")
            val sizeIdx = c.getColumnIndexOrThrow("size_bytes")
            val errIdx = c.getColumnIndexOrThrow("error")
            val exclIdx = c.getColumnIndexOrThrow("excluded")
            val nameIdx = c.getColumnIndexOrThrow("display_name")
            val videoIdx = c.getColumnIndexOrThrow("is_video")
            val dateIdx = c.getColumnIndexOrThrow("date_added")
            while (c.moveToNext()) {
                val mediaId = c.getLong(idIdx)
                map[mediaId] = UploadRecord(
                    mediaId = mediaId,
                    status = runCatching { UploadStatus.valueOf(c.getString(statusIdx)) }
                        .getOrDefault(UploadStatus.NOT_UPLOADED),
                    serverId = if (c.isNull(remoteIdx)) null else c.getString(remoteIdx),
                    uploadedAt = c.getLong(atIdx),
                    sizeBytes = c.getLong(sizeIdx),
                    error = if (c.isNull(errIdx)) null else c.getString(errIdx),
                    excluded = c.getInt(exclIdx) == 1,
                    displayName = if (c.isNull(nameIdx)) null else c.getString(nameIdx),
                    isVideo = c.getInt(videoIdx) == 1,
                    dateAddedSec = c.getLong(dateIdx),
                )
            }
        }
        _records.value = map
    }

    fun snapshot(): Map<Long, UploadRecord> = _records.value

    suspend fun upsert(uid: String, record: UploadRecord) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply { putRecord(uid, record) }
        writableDatabase.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        if (uid == loadedUid) {
            _records.value = _records.value.toMutableMap().apply { put(record.mediaId, record) }
        }
    }

    suspend fun setExcluded(uid: String, mediaIds: Collection<Long>, excluded: Boolean) =
        withContext(Dispatchers.IO) {
            val db = writableDatabase
            db.beginTransaction()
            try {
                for (id in mediaIds) {
                    val record = buildExclusion(id, excluded)
                    db.insertWithOnConflict(
                        TABLE, null,
                        ContentValues().apply { putRecord(uid, record) },
                        SQLiteDatabase.CONFLICT_REPLACE,
                    )
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            if (uid == loadedUid) {
                val map = _records.value.toMutableMap()
                for (id in mediaIds) map[id] = buildExclusion(id, excluded)
                _records.value = map
            }
        }

    private fun buildExclusion(id: Long, excluded: Boolean): UploadRecord {
        val existing = _records.value[id]
        val newStatus = when {
            excluded -> UploadStatus.EXCLUDED
            existing?.status == UploadStatus.EXCLUDED || existing == null -> UploadStatus.NOT_UPLOADED
            else -> existing.status
        }
        return (existing ?: UploadRecord(id, UploadStatus.NOT_UPLOADED))
            .copy(status = newStatus, excluded = excluded)
    }

    /** Remove a record entirely (used after deleting the remote copy). */
    suspend fun delete(uid: String, mediaId: Long) = withContext(Dispatchers.IO) {
        writableDatabase.delete(TABLE, "uid = ? AND media_id = ?", arrayOf(uid, mediaId.toString()))
        if (uid == loadedUid) {
            _records.value = _records.value.toMutableMap().apply { remove(mediaId) }
        }
    }

    companion object {
        private const val DB_NAME = "photos_backup.db"
        private const val DB_VERSION = 2
        private const val TABLE = "uploads"
    }
}
