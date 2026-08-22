package com.desire.photos.backup

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.desire.photos.data.settings.BackupSettings

enum class NetType { NONE, WIFI, MOBILE, OTHER }

object NetworkUtil {

    fun activeType(context: Context): NetType {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetType.NONE
        val network = cm.activeNetwork ?: return NetType.NONE
        val caps = cm.getNetworkCapabilities(network) ?: return NetType.NONE
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return NetType.NONE
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetType.WIFI
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) -> NetType.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetType.MOBILE
            else -> NetType.OTHER
        }
    }

    /** Is the current network one the user allows backup on? */
    fun isBackupAllowed(context: Context, settings: BackupSettings): Boolean =
        when (activeType(context)) {
            NetType.WIFI -> settings.backupOverWifi
            NetType.OTHER -> settings.backupOverWifi // ethernet / unmetered — treat like Wi-Fi
            NetType.MOBILE -> settings.backupOverMobile
            NetType.NONE -> false
        }
}
