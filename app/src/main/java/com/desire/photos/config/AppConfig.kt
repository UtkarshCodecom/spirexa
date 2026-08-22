package com.desire.photos.config

import com.desire.photos.BuildConfig

/**
 * Central place for the deployment-specific values. All of these come from
 * local.properties -> BuildConfig so no keys are committed to source control.
 *
 * The app holds no storage credentials at all — every file operation goes
 * through the Node API, which is the only thing that talks to Backblaze B2.
 *
 * See app/build.gradle.kts and README.md for how to fill them in.
 */
object AppConfig {

    // ---- Photos API server (Express/TS) ----
    val apiBaseUrl: String = BuildConfig.API_BASE_URL.trimEnd('/')

    val isApiConfigured: Boolean
        get() = apiBaseUrl.isNotBlank()

    // ---- Public web app (share links point here) ----
    val publicWebUrl: String = BuildConfig.PUBLIC_WEB_URL.trimEnd('/')

    // ---- Google Sign-In (OAuth Web client id) ----
    val googleWebClientId: String = BuildConfig.GOOGLE_WEB_CLIENT_ID

    val isGoogleSignInConfigured: Boolean
        get() = googleWebClientId.isNotBlank()
}
