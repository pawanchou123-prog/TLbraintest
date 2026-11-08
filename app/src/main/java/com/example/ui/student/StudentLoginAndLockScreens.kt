package com.example.ui.student

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CurriculumCatalog
import com.example.data.model.AllowedStudent
import com.example.data.model.AppConfig
import com.example.ui.auth.StudentAuthManager

@Composable
fun StudentLoginScreen(
    onSignInCompleted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme

    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var showSupportModal by remember { mutableStateOf(false) }

    if (showSupportModal) {
        GetAccessContactDialog(
            appConfig = AppConfig(),
            onDismiss = { showSupportModal = false }
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("student_login_screen"),
        color = scheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            scheme.primaryContainer.copy(alpha = 0.50f),
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
                    .widthIn(max = 500.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hero Neon Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(185.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.5.dp, scheme.outline, RoundedCornerShape(24.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_banner_1793633719046),
                        contentDescription = stringResource(R.string.cd_hero_banner),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        scheme.background.copy(alpha = 0.15f),
                                        scheme.background.copy(alpha = 0.88f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = stringResource(R.string.cd_brand_icon),
                                tint = scheme.secondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.brand_title),
                                style = MaterialTheme.typography.headlineMedium,
                                color = scheme.onBackground
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.brand_tagline),
                            style = MaterialTheme.typography.labelLarge,
                            color = scheme.secondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, scheme.outline.copy(alpha = 0.8f), RoundedCornerShape(22.dp)),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = scheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Student Portal Login (/app)",
                                style = MaterialTheme.typography.titleLarge,
                                color = scheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Login using your administrator-created credentials. Public student registration is disabled.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = scheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Student Pass: Rs. 50 / 3 Months (UPI or Cash)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = scheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Owner: PRITAM CHOUDHARY",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.secondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        var emailInput by remember { mutableStateOf("") }
                        var passwordInput by remember { mutableStateOf("") }
                        var passwordVisible by remember { mutableStateOf(false) }
                        var showResetDialog by remember { mutableStateOf(false) }
                        var resetSuccessMessage by remember { mutableStateOf<String?>(null) }

                        if (showResetDialog) {
                            var resetEmailInput by remember { mutableStateOf(emailInput.trim()) }
                            var resetError by remember { mutableStateOf<String?>(null) }
                            var isSendingReset by remember { mutableStateOf(false) }

                            AlertDialog(
                                onDismissRequest = { if (!isSendingReset) showResetDialog = false },
                                title = { Text("Forgot / Reset Password") },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "Enter your administrator-registered student email. Firebase will send a secure password reset link directly to your inbox.",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        androidx.compose.material3.OutlinedTextField(
                                            value = resetEmailInput,
                                            onValueChange = {
                                                resetEmailInput = it
                                                resetError = null
                                            },
                                            label = { Text("Student Email Address") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().testTag("reset_email_input")
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
                                            if (resetEmailInput.isBlank()) {
                                                resetError = "Please enter your email address."
                                                return@Button
                                            }
                                            isSendingReset = true
                                            resetError = null
                                            scope.launch {
                                                val res = StudentAuthManager.sendPasswordReset(resetEmailInput)
                                                isSendingReset = false
                                                res.fold(
                                                    onSuccess = {
                                                        SoundManager.play(SoundEffect.REQUEST_SUBMITTED)
                                                        showResetDialog = false
                                                        resetSuccessMessage = "Password reset email sent to $resetEmailInput. Check your inbox to set a new password."
                                                    },
                                                    onFailure = { err ->
                                                        SoundManager.play(SoundEffect.ERROR)
                                                        resetError = err.localizedMessage ?: "Failed to send reset email."
                                                    }
                                                )
                                            }
                                        },
                                        enabled = !isSendingReset,
                                        modifier = Modifier.testTag("send_reset_link_btn")
                                    ) {
                                        if (isSendingReset) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = scheme.onPrimary, strokeWidth = 2.dp)
                                        } else {
                                            Text("Send Reset Link")
                                        }
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showResetDialog = false }, enabled = !isSendingReset) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }

                        if (resetSuccessMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = scheme.tertiaryContainer,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = resetSuccessMessage.orEmpty(),
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
                            value = emailInput,
                            onValueChange = {
                                emailInput = it
                                authError = null
                            },
                            label = { Text("Email Address") },
                            placeholder = { Text("student@gmail.com") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            modifier = Modifier.fillMaxWidth().testTag("student_email_input"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        androidx.compose.material3.OutlinedTextField(
                            value = passwordInput,
                            onValueChange = {
                                passwordInput = it
                                authError = null
                            },
                            label = { Text("Password") },
                            placeholder = { Text("Enter your password") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Text(
                                        text = if (passwordVisible) "Hide" else "Show",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = scheme.primary
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None
                            else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().testTag("student_password_input"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    authError = null
                                    showResetDialog = true
                                },
                                modifier = Modifier.testTag("student_forgot_password_btn")
                            ) {
                                Text("Forgot Password?", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Email + Password Sign In Button
                        Button(
                            onClick = {
                                authError = null
                                resetSuccessMessage = null
                                isSigningIn = true
                                scope.launch {
                                    val res = StudentAuthManager.signInWithEmailPassword(
                                        email = emailInput,
                                        password = passwordInput
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
                                .testTag("student_email_login_button"),
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

                        // Secondary: Sign in with Approved Google Account (Pre-existing account backward-compatibility)
                        OutlinedButton(
                            onClick = {
                                authError = null
                                resetSuccessMessage = null
                                isSigningIn = true
                                StudentAuthManager.signInWithApprovedGoogleAccount(
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
                                .testTag("student_google_login_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Sign In with Google Account")
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { showSupportModal = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("login_get_password_support_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Get Account Credentials / Contact Admin")
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        TextButton(
                            onClick = {
                                context.startActivity(Intent(context, com.example.AdminActivity::class.java))
                            },
                            modifier = Modifier.testTag("launch_admin_portal_from_login_btn")
                        ) {
                            Text(
                                text = "Administrator? Open Main Admin Panel (/admin)",
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentAccessDeniedScreen(
    email: String,
    reasonTitle: String,
    reasonDetail: String,
    allowedRecord: AllowedStudent?,
    appConfig: AppConfig,
    onRetryCheck: () -> Unit,
    onSignOut: () -> Unit,
    onSubmitPendingRequest: ((String, Int, String, String, (Boolean, String) -> Unit) -> Unit)? = null
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val isBanned = allowedRecord?.isBanned == true
    var showContactDialog by remember { mutableStateOf(false) }
    var showRequestDialog by remember { mutableStateOf(false) }
    var requestFeedback by remember { mutableStateOf<String?>(null) }

    if (showContactDialog) {
        GetAccessContactDialog(
            appConfig = appConfig,
            onDismiss = { showContactDialog = false }
        )
    }

    if (showRequestDialog && onSubmitPendingRequest != null) {
        var reqName by remember { mutableStateOf(allowedRecord?.studentName.orEmpty()) }
        var reqClassText by remember { mutableStateOf((allowedRecord?.studentClass ?: 10L).toString()) }
        var reqMethod by remember { mutableStateOf("upi") }
        var reqRef by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showRequestDialog = false },
            title = { Text("Submit Access / Payment Verification Request") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Requests are never auto-approved. Administrator Pritam Choudhary will verify your Rs. 50 UPI/Cash payment before activating access.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = reqName,
                        onValueChange = { reqName = it },
                        label = { Text("Student Full Name") },
                        singleLine = true
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = reqClassText,
                        onValueChange = { reqClassText = it },
                        label = { Text("Class (1-12)") },
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.FilterChip(selected = reqMethod == "upi", onClick = { reqMethod = "upi" }, label = { Text("UPI") })
                        androidx.compose.material3.FilterChip(selected = reqMethod == "cash", onClick = { reqMethod = "cash" }, label = { Text("Cash") })
                        androidx.compose.material3.FilterChip(selected = reqMethod == "access_request", onClick = { reqMethod = "access_request" }, label = { Text("Access") })
                    }
                    androidx.compose.material3.OutlinedTextField(
                        value = reqRef,
                        onValueChange = { reqRef = it },
                        label = { Text("UPI UTR / Payment Note (Optional)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cls = reqClassText.toIntOrNull()?.coerceIn(1, 12) ?: 10
                        onSubmitPendingRequest(reqName, cls, reqMethod, reqRef) { _, msg ->
                            requestFeedback = msg
                            showRequestDialog = false
                        }
                    }
                ) {
                    Text("Submit Pending Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRequestDialog = false }) { Text("Cancel") }
            }
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("student_access_denied_screen"),
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
                    .border(
                        width = 1.5.dp,
                        color = if (isBanned) scheme.error else scheme.outline,
                        shape = RoundedCornerShape(24.dp)
                    ),
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
                            .background(scheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBanned) Icons.Default.Block else Icons.Default.Lock,
                            contentDescription = reasonTitle,
                            tint = if (isBanned) scheme.error else scheme.secondary,
                            modifier = Modifier.size(32.dp)
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

                    if (!requestFeedback.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = scheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = requestFeedback.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onPrimaryContainer,
                                modifier = Modifier.padding(10.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = scheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Signed-in Gmail: $email",
                                style = MaterialTheme.typography.labelLarge,
                                color = scheme.onSurface
                            )
                            if (allowedRecord != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Subscription Expiry: ${CurriculumCatalog.formatReadableDate(allowedRecord.subscriptionEndMs)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Fee: ${appConfig.subscriptionPriceText} (Manual UPI / Cash)",
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.secondary
                            )
                            Text(
                                text = "Payment Instructions: ${appConfig.upiId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (!isBanned && onSubmitPendingRequest != null) {
                        Button(
                            onClick = { showRequestDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("denied_submit_request_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Submit Payment / Access Request")
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Button(
                        onClick = {
                            if (appConfig.whatsappUrl.startsWith("https://") && appConfig.whatsappUrl.length > 14) {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(appConfig.whatsappUrl)))
                                } catch (_: Exception) {
                                    showContactDialog = true
                                }
                            } else {
                                showContactDialog = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("denied_contact_support_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Get Password / Contact Admin")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onRetryCheck,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("denied_recheck_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Re-Check Approval Status")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("denied_logout_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Logout / Switch Google Account")
                    }

                    if (com.example.data.model.isPreAuthorizedMainAdminEmail(email)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                context.startActivity(Intent(context, com.example.AdminActivity::class.java))
                            }
                        ) {
                            Text("Open Main Admin Panel (/admin)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GetAccessContactDialog(
    appConfig: AppConfig,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Account Activation & Support") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("TL Brain Test uses secure Google Sign-In strictly for administrator-approved Gmail accounts (no passwords stored in database, no public signup).")
                Text("Subscription Plan: ${appConfig.subscriptionPriceText}")
                Text("Payment Modes: Manual UPI or Cash (${appConfig.upiId})")
                Text("Step 1: Contact Administrator Pritam Choudhary and share your student Gmail address and Class (1 to 12).")
                Text("Step 2: Once your 3-month pass is activated, tap 'Sign in with Approved Google Account' using that Gmail.")
            }
        },
        confirmButton = {
            if (appConfig.whatsappUrl.startsWith("https://")) {
                Button(
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(appConfig.whatsappUrl)))
                        } catch (_: Exception) {
                        }
                        onDismiss()
                    }
                ) {
                    Text("Open WhatsApp")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
