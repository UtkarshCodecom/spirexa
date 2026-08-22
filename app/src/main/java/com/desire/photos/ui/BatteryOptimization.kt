package com.desire.photos.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

/** Helpers for asking the OS to stop battery-optimizing (killing) the app. */
object BatteryOptimization {

    fun isIgnored(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** System dialog asking the user to exempt this app from battery optimization. */
    @SuppressLint("BatteryLife")
    fun requestIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
}
