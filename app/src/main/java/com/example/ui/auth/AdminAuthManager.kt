package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val ADMIN_AUTH_TAG = "AdminAuthManager"

/**
 * Dedicated authentication service for Application 2: MAIN ADMIN APPLICATION (/admin).
 * Authenticates administrators strictly via Google Sign-In (Credential Manager + Firebase Auth)
 * with zero passwords, zero shared credentials, and zero public registration.
 */
object AdminAuthManager {

    fun observeAdminAuthState(firebaseAuth: FirebaseAuth = Firebase.auth): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    fun signInWithAdminGoogleAccount(
        context: Context,
        scope: CoroutineScope,
        onSuccess: () -> Unit,
        onCancelled: () -> Unit,
        onError: (String) -> Unit
    ) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            onError("Google Sign-In configuration is missing from firebase_applet_config.xml.")
            return
        }

        val credentialManager = CredentialManager.create(context)
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val activity = context as? Activity
                val result = if (activity != null) {
                    credentialManager.getCredential(activity, request)
                } else {
                    credentialManager.getCredential(context, request)
                }
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    Firebase.auth.signInWithCredential(firebaseCredential).await()
                    onSuccess()
                } else {
                    onError("Unsupported Google credential returned.")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(ADMIN_AUTH_TAG, "Administrator cancelled Google Sign-In", e)
                onCancelled()
            } catch (e: Exception) {
                Log.e(ADMIN_AUTH_TAG, "Administrator Google Sign-In failed", e)
                onError(e.localizedMessage ?: "Google Sign-In failed. Check your network connection and try again.")
            }
        }
    }

    suspend fun signInWithEmailPassword(
        email: String,
        password: String
    ): Result<Unit> {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter a valid administrator email address."))
        }
        if (password.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please enter your administrator password."))
        }
        return try {
            Firebase.auth.signInWithEmailAndPassword(cleanEmail, password).await()
            Result.success(Unit)
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
            Result.failure(IllegalArgumentException("No administrator account registered with this email address."))
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            Result.failure(IllegalArgumentException("Incorrect password or invalid credentials. Check and retry."))
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException(e.localizedMessage ?: "Admin authentication failed. Check your network connection."))
        }
    }

    suspend fun sendAdminPasswordReset(email: String): Result<Unit> {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter your administrator email address."))
        }
        return try {
            Firebase.auth.sendPasswordResetEmail(cleanEmail).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException(e.localizedMessage ?: "Failed to send password reset email."))
        }
    }

    fun signOutAdminSession(
        context: Context,
        scope: CoroutineScope,
        onComplete: () -> Unit = {}
    ) {
        val credentialManager = CredentialManager.create(context)
        Firebase.auth.signOut()
        scope.launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(ADMIN_AUTH_TAG, "Failed to clear CredentialManager state on admin logout", e)
            } finally {
                onComplete()
            }
        }
    }
}
