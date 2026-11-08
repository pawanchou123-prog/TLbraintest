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

private const val STUDENT_AUTH_TAG = "StudentAuthManager"
private const val STUDENT_PREFS_NAME = "tl_brain_student_app_prefs"
private const val KEY_TERMS_ACCEPTED = "terms_accepted_v1"
private const val KEY_READ_NOTICES = "read_notice_ids_v1"

/**
 * Dedicated authentication & local consent service for Application 1 (/app).
 * Keeps Firebase Auth, Credential Manager Google Sign-In, and Terms acceptance logic
 * cleanly separated from Jetpack Compose UI screens.
 */
object StudentAuthManager {

    fun observeAuthState(firebaseAuth: FirebaseAuth = Firebase.auth): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    fun hasAcceptedTermsLocally(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(STUDENT_PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_TERMS_ACCEPTED, false)
    }

    fun saveTermsAcceptedLocally(context: Context, accepted: Boolean) {
        val prefs = context.applicationContext.getSharedPreferences(STUDENT_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_TERMS_ACCEPTED, accepted).apply()
    }

    fun getReadNotificationIds(context: Context): Set<String> {
        val prefs = context.applicationContext.getSharedPreferences(STUDENT_PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_READ_NOTICES, emptySet()) ?: emptySet()
    }

    fun markNotificationReadLocally(context: Context, noticeId: String): Set<String> {
        val prefs = context.applicationContext.getSharedPreferences(STUDENT_PREFS_NAME, Context.MODE_PRIVATE)
        val updated = (prefs.getStringSet(KEY_READ_NOTICES, emptySet()) ?: emptySet()).toMutableSet()
        updated.add(noticeId)
        prefs.edit().putStringSet(KEY_READ_NOTICES, updated).apply()
        return updated
    }

    fun signInWithApprovedGoogleAccount(
        context: Context,
        scope: CoroutineScope,
        onSuccess: () -> Unit,
        onCancelled: () -> Unit,
        onError: (String) -> Unit
    ) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            onError("Google Sign-In client configuration missing.")
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
                    val firebaseCred = GoogleAuthProvider.getCredential(googleIdToken, null)
                    Firebase.auth.signInWithCredential(firebaseCred).await()
                    onSuccess()
                } else {
                    onError("Unsupported credential type returned.")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(STUDENT_AUTH_TAG, "Student cancelled Google Sign-In", e)
                onCancelled()
            } catch (e: Exception) {
                Log.e(STUDENT_AUTH_TAG, "Student Google Sign-In failed", e)
                onError(e.localizedMessage ?: "Sign-in failed. Please check your connection and try again.")
            }
        }
    }

    suspend fun signInWithEmailPassword(
        email: String,
        password: String
    ): Result<Unit> {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (password.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please enter your password."))
        }
        return try {
            Firebase.auth.signInWithEmailAndPassword(cleanEmail, password).await()
            Result.success(Unit)
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
            Result.failure(IllegalArgumentException("No student account found with this email. Only administrator-created accounts may log in."))
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            Result.failure(IllegalArgumentException("Incorrect password or invalid email format. Please check and try again."))
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException(e.localizedMessage ?: "Login failed. Please check your network connection and try again."))
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter your registered email address."))
        }
        return try {
            Firebase.auth.sendPasswordResetEmail(cleanEmail).await()
            Result.success(Unit)
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
            Result.failure(IllegalArgumentException("No account registered with this email address."))
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException(e.localizedMessage ?: "Failed to send password reset email. Check network connection."))
        }
    }

    fun signOutStudentSession(
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
                Log.w(STUDENT_AUTH_TAG, "Failed to clear credential manager state", e)
            } finally {
                onComplete()
            }
        }
    }
}
