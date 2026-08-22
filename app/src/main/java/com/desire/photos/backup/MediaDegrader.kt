package com.desire.photos.backup

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Reduces the size of large images before upload ("degrade quality").
 *
 * Note: true video transcoding needs a heavy media pipeline (e.g. Media3
 * Transformer) and is intentionally left out here — large videos are instead
 * governed by the "upload sometimes" policy in [BackupManager]. Images are
 * downscaled to a max edge and re-encoded as JPEG.
 */
object MediaDegrader {

    /** Returns recompressed JPEG bytes, or null if the image couldn't be decoded. */
    fun degradeImage(
        resolver: ContentResolver,
        uri: Uri,
        maxDimension: Int,
        quality: Int,
    ): ByteArray? {
        // 1) Read bounds only.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val srcW = bounds.outWidth
        val srcH = bounds.outHeight
        if (srcW <= 0 || srcH <= 0) return null

        // 2) Decode downsampled to keep memory reasonable.
        var sample = 1
        while (max(srcW, srcH) / sample > maxDimension * 2) sample *= 2
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        var bitmap: Bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: return null

        // 3) Scale precisely to the target max edge.
        val longest = max(bitmap.width, bitmap.height)
        if (longest > maxDimension) {
            val scale = maxDimension.toFloat() / longest
            val nw = (bitmap.width * scale).toInt().coerceAtLeast(1)
            val nh = (bitmap.height * scale).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(bitmap, nw, nh, true)
            if (scaled != bitmap) bitmap.recycle()
            bitmap = scaled
        }

        // 4) Encode JPEG.
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), out)
            bitmap.recycle()
            out.toByteArray()
        }
    }
}
