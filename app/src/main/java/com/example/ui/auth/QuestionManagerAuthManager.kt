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

private const val QM_AUTH_TAG = "QuestionManagerAuth"

/**
 * Dedicated Google Sign-In authentication service for Application 3:
 * QUESTION MANAGEMENT APPLICATION (/question-manager).
 *
 * Uses Jetpack Credential Manager + Firebase Auth with zero shared passwords,
 * zero public signup, and zero self-assigned roles.
 */
object QuestionManagerAuthManager {

    fun observeAuthState(firebaseAuth: FirebaseAuth = Firebase.auth): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    fun signInWithGoogleAccount(
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
                Log.w(QM_AUTH_TAG, "Question Manager cancelled Google Sign-In", e)
                onCancelled()
            } catch (e: Exception) {
                Log.e(QM_AUTH_TAG, "Question Manager Google Sign-In failed", e)
                onError(e.localizedMessage ?: "Google Sign-In failed. Check your network connection and retry.")
            }
        }
    }

    suspend fun signInWithEmailPassword(
        email: String,
        password: String
    ): Result<Unit> {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter a valid Question Manager email address."))
        }
        if (password.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please enter your password."))
        }
        return try {
            Firebase.auth.signInWithEmailAndPassword(cleanEmail, password).await()
            Result.success(Unit)
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
            Result.failure(IllegalArgumentException("No account registered with this email address."))
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            Result.failure(IllegalArgumentException("Incorrect password or invalid credentials."))
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException(e.localizedMessage ?: "Authentication failed. Check your network connection."))
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
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException(e.localizedMessage ?: "Failed to send password reset email."))
        }
    }

    fun signOutSession(
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
                Log.w(QM_AUTH_TAG, "Failed to clear CredentialManager state on logout", e)
            } finally {
                onComplete()
            }
        }
    }
}
