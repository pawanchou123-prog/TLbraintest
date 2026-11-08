package com.example.ui.questionmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.core.audio.SoundEffect
import com.example.core.audio.SoundManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.auth.QuestionManagerAuthManager

@Composable
fun QuestionManagerLoginScreen(
    onSignInCompleted: () -> Unit,
    onOpenStudentApp: () -> Unit,
    onOpenAdminApp: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme

    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("qm_login_screen"),
        color = scheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            scheme.primaryContainer.copy(alpha = 0.7f),
                            scheme.background,
                            scheme.background
                        )
                    )
                )
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, scheme.primary, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(scheme.primaryContainer)
                                .border(1.5.dp, scheme.secondary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.FactCheck,
                                contentDescription = "Question Management Emblem",
                                tint = scheme.secondary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "TL BRAIN TEST – QUESTION MANAGEMENT",
                            style = MaterialTheme.typography.titleLarge,
                            color = scheme.onSurface,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Application 3 (/question-manager)  •  Owner: PRITAM CHOUDHARY",
                            style = MaterialTheme.typography.labelLarge,
                            color = scheme.secondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = scheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = scheme.tertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Role-Based Question Bank Access",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = scheme.onSurface
                                    )
                                }
                                Text(
                                    text = "Public signup is disabled. Only Main Admins and Google accounts explicitly authorized as Question Managers may enter /question-manager. Students are strictly blocked, and Question Managers never receive Main Admin permissions.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        var qmEmailInput by remember { mutableStateOf("") }
                        var qmPasswordInput by remember { mutableStateOf("") }
                        var qmPasswordVisible by remember { mutableStateOf(false) }
                        var showQmResetDialog by remember { mutableStateOf(false) }
                        var qmResetSuccessMessage by remember { mutableStateOf<String?>(null) }

                        if (showQmResetDialog) {
                            var resetEmail by remember { mutableStateOf(qmEmailInput.trim()) }
                            var resetError by remember { mutableStateOf<String?>(null) }
                            var isSendingReset by remember { mutableStateOf(false) }

                            AlertDialog(
                                onDismissRequest = { if (!isSendingReset) showQmResetDialog = false },
                                title = { Text("Reset Question Manager Password") },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "Enter your authorized Question Manager or Main Admin email. Firebase will send a password reset email.",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        androidx.compose.material3.OutlinedTextField(
                                            value = resetEmail,
                                            onValueChange = {
                                                resetEmail = it
                                                resetError = null
                                            },
                                            label = { Text("Email Address") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().testTag("qm_reset_email_input")
                                        )
                                        if (resetError != null) {
                                            Text(
                                                text = resetError.orEmpty(),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = scheme.error
                                            )
                                        }
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            isSendingReset = true
                                            resetError = null
                                            scope.launch {
                                                val res = QuestionManagerAuthManager.sendPasswordReset(resetEmail)
                                                isSendingReset = false
                                                res.fold(
                                                    onSuccess = {
                                                        SoundManager.play(SoundEffect.REQUEST_SUBMITTED)
                                                        showQmResetDialog = false
                                                        qmResetSuccessMessage = "Password reset link sent to $resetEmail. Check your inbox."
                                                    },
                                                    onFailure = { err ->
                                                        SoundManager.play(SoundEffect.ERROR)
                                                        resetError = err.localizedMessage ?: "Failed to send reset email."
                                                    }
                                                )
                                            }
                                        },
                                        enabled = !isSendingReset
                                    ) {
                                        if (isSendingReset) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = scheme.onPrimary, strokeWidth = 2.dp)
                                        } else {
                                            Text("Send Reset Link")
                                        }
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showQmResetDialog = false }, enabled = !isSendingReset) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }

                        if (qmResetSuccessMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = scheme.tertiaryContainer,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = qmResetSuccessMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onTertiaryContainer,
                                    modifier = Modifier.padding(12.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        if (authError != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = scheme.errorContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = authError.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        androidx.compose.material3.OutlinedTextField(
                            value = qmEmailInput,
                            onValueChange = {
                                qmEmailInput = it
                                authError = null
                            },
                            label = { Text("Manager Email Address") },
                            placeholder = { Text("editor@gmail.com") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            modifier = Modifier.fillMaxWidth().testTag("qm_email_input"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        androidx.compose.material3.OutlinedTextField(
                            value = qmPasswordInput,
                            onValueChange = {
                                qmPasswordInput = it
                                authError = null
                            },
                            label = { Text("Password") },
                            placeholder = { Text("Enter manager password") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            trailingIcon = {
                                IconButton(onClick = { qmPasswordVisible = !qmPasswordVisible }) {
                                    Text(
                                        text = if (qmPasswordVisible) "Hide" else "Show",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = scheme.primary
                                    )
                                }
                            },
                            visualTransformation = if (qmPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None
                            else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().testTag("qm_password_input"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    authError = null
                                    showQmResetDialog = true
                                },
                                modifier = Modifier.testTag("qm_forgot_password_btn")
                            ) {
                                Text("Forgot Password?", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                authError = null
                                qmResetSuccessMessage = null
                                isSigningIn = true
                                scope.launch {
                                    val res = QuestionManagerAuthManager.signInWithEmailPassword(
                                        email = qmEmailInput,
                                        password = qmPasswordInput
                                    )
                                    isSigningIn = false
                                    res.fold(
                                        onSuccess = {
                                            SoundManager.play(SoundEffect.SUCCESS)
                                            onSignInCompleted()
                                        },
                                        onFailure = { err ->
                                            SoundManager.play(SoundEffect.ERROR)
                                            authError = err.localizedMessage
                                        }
                                    )
                                }
                            },
                            enabled = !isSigningIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("qm_email_login_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isSigningIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = scheme.onPrimary,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Login with Email & Password")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.HorizontalDivider(modifier = Modifier.weight(1f), color = scheme.outlineVariant)
                            Text(
                                text = "  OR  ",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant
                            )
                            androidx.compose.material3.HorizontalDivider(modifier = Modifier.weight(1f), color = scheme.outlineVariant)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                authError = null
                                qmResetSuccessMessage = null
                                isSigningIn = true
                                QuestionManagerAuthManager.signInWithGoogleAccount(
                                    context = context,
                                    scope = scope,
                                    onSuccess = {
                                        isSigningIn = false
                                        onSignInCompleted()
                                    },
                                    onCancelled = {
                                        isSigningIn = false
                                    },
                                    onError = { msg ->
                                        isSigningIn = false
                                        authError = msg
                                    }
                                )
                            },
                            enabled = !isSigningIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("qm_google_login_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Sign In with Google Account")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onOpenStudentApp,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("qm_open_student_app_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Student (/app)", style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = onOpenAdminApp,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("qm_open_admin_app_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Admin (/admin)", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuestionManagerAccessDeniedScreen(
    email: String,
    reasonTitle: String,
    reasonDetail: String,
    onRetryVerification: () -> Unit,
    onSignOut: () -> Unit,
    onOpenStudentApp: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("qm_access_denied_screen"),
        color = scheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(22.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .border(1.5.dp, scheme.error, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(scheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Access Denied",
                            tint = scheme.error,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = reasonTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = scheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = reasonDetail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = scheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = scheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Signed-in Account: ${email.ifBlank { "Unknown" }}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = scheme.onSurface
                                )
                            }
                            Text(
                                text = "Students and unapproved Google accounts cannot access /question-manager. Ask Main Admin Pritam Choudhary to authorize your Gmail address if you are a Question Manager.",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("qm_denied_logout_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out & Switch Google Account")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onRetryVerification,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("qm_denied_retry_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Re-Check Question Manager Access")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onOpenStudentApp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("qm_denied_open_student_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Return to Student User App (/app)")
                    }
                }
            }
        }
    }
}
