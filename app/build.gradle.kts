import java.net.URI
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

// Read secrets from local.properties (git-ignored) so keys never live in source control.
// Add these keys to local.properties:
//   API_BASE_URL=http://localhost:3002                   (the Photos server; see server/README)
//   GOOGLE_WEB_CLIENT_ID=xxxx.apps.googleusercontent.com  (OAuth Web client for Google Sign-In)
// No storage credentials live here on purpose — the app never talks to Backblaze
// B2 directly, only to the server, which is the only thing holding B2 keys.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun secret(key: String, default: String = ""): String =
    (localProps.getProperty(key) ?: providers.gradleProperty(key).orNull ?: default)

android {
    namespace = "com.desire.photos"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.desire.photos"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_BASE_URL", "\"${secret("API_BASE_URL")}\"")
        buildConfigField("String", "PUBLIC_WEB_URL", "\"${secret("PUBLIC_WEB_URL")}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${secret("GOOGLE_WEB_CLIENT_ID")}\"")

        // Lets AndroidManifest.xml's share deep-link <intent-filter> target whatever
        // host PUBLIC_WEB_URL actually points at (localhost in dev, a real domain in prod)
        // without hardcoding it in the manifest.
        val publicWebUrl = secret("PUBLIC_WEB_URL", "http://localhost:3000")
        val publicWebUri = runCatching { URI(publicWebUrl) }.getOrNull()
        manifestPlaceholders["shareLinkScheme"] = publicWebUri?.scheme ?: "https"
        manifestPlaceholders["shareLinkHost"] = publicWebUri?.host ?: "localhost"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    // Firebase Auth only — everything else (media, albums, backup index) goes
    // through the Node API now, not direct Firestore access from the app.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)

    // Google Sign-In via Credential Manager
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.identity.googleid)

    // Coroutines + Firebase Task await
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Image/video thumbnail loading
    implementation(libs.coil.compose)
    implementation(libs.coil.video)

    // Networking (talks to the Photos API server — never to storage directly)
    implementation(libs.okhttp)

    // Settings + background backup
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)

    // EXIF GPS reading for the Places feature
    implementation(libs.androidx.exifinterface)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
