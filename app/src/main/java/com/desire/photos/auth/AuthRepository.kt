package com.desire.photos.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.desire.photos.config.AppConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase-backed authentication. Supports email/password and Google Sign-In
 * (via Credential Manager). The signed-in user's uid scopes everything on the
 * server; the app never talks to storage or Firestore directly — every other
 * operation goes through the API, authenticated with the ID token from here.
 */
class AuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {

    /** Emits the current user (or null) whenever auth state changes. */
    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    val currentUser: FirebaseUser? get() = auth.currentUser
    val uid: String? get() = auth.currentUser?.uid

    /** Fresh Firebase ID token for calling the API, or null if signed out. Refreshes if stale. */
    suspend fun idToken(): String? =
        auth.currentUser?.getIdToken(false)?.await()?.token

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        Unit
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<Unit> = runCatching {
        auth.createUserWithEmailAndPassword(email.trim(), password).await()
        Unit
    }

    /**
     * Google Sign-In. Requires GOOGLE_WEB_CLIENT_ID to be configured and the
     * app's SHA-1 registered in the Firebase console. [activityContext] should
     * be an Activity so the account picker can be shown.
     */
    suspend fun signInWithGoogle(activityContext: Context): Result<Unit> = runCatching {
        check(AppConfig.isGoogleSignInConfigured) {
            "Google Sign-In is not configured. Add GOOGLE_WEB_CLIENT_ID to local.properties and register your SHA-1 in Firebase."
        }
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(AppConfig.googleWebClientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val response = CredentialManager.create(activityContext).getCredential(activityContext, request)
        val credential = response.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
            auth.signInWithCredential(firebaseCredential).await()
            Unit
        } else {
            throw IllegalStateException("Unexpected credential type from Credential Manager")
        }
    }

    fun signOut() = auth.signOut()
}
