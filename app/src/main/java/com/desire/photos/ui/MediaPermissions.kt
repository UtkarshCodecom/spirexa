package com.desire.photos.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object MediaPermissions {

    fun required(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    /**
     * Requested alongside [required] but never gates [hasAccess] — Places
     * (grouping backed-up photos by where they were taken) just has less to
     * show if this is denied, nothing else in the app depends on it.
     */
    fun optional(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(Manifest.permission.ACCESS_MEDIA_LOCATION)
        } else {
            emptyArray()
        }

    fun hasAccess(context: Context): Boolean =
        required().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    /** True if there's nothing left to ask for (also true pre-Q, where [optional] is empty). */
    fun hasOptionalAccess(context: Context): Boolean =
        optional().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
}
