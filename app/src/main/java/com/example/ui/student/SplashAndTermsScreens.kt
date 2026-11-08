package com.example.ui.student

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import kotlinx.coroutines.delay

fun avatarVectorForId(avatarId: String): ImageVector = when (avatarId) {
    "quantum_star" -> Icons.Default.Star
    "scholar_shield" -> Icons.Default.Shield
    "logic_bolt" -> Icons.Default.Bolt
    "rocket_mind" -> Icons.Default.RocketLaunch
    "diamond_crest" -> Icons.Default.Diamond
    else -> Icons.Default.Psychology
}

@Composable
fun StudentAvatarBadge(
    avatarId: String,
    size: Dp = 54.dp,
    isSelected: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        scheme.primary.copy(alpha = 0.45f),
                        scheme.primaryContainer
                    )
                )
            )
            .border(
                width = if (isSelected) 2.5.dp else 1.5.dp,
                color = if (isSelected) scheme.tertiary else scheme.outline,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = avatarVectorForId(avatarId),
            contentDescription = "Student profile emblem",
            tint = if (isSelected) scheme.tertiary else scheme.secondary,
            modifier = Modifier.size(size * 0.54f)
        )
    }
}

@Composable
fun StudentSplashScreen(
    onSplashFinished: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val pulseTransition = rememberInfiniteTransition(label = "splash_glow")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )
    var loadProgress by remember { mutableFloatStateOf(0.1f) }

    LaunchedEffect(Unit) {
        val steps = 14
        for (i in 1..steps) {
            loadProgress = i.toFloat() / steps.toFloat()
            delay(85L)
        }
        onSplashFinished()
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("student_splash_screen"),
        color = scheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            scheme.primaryContainer.copy(alpha = 0.75f),
                            scheme.background,
                            scheme.background
                        )
                    )
                )
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 420.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(126.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .border(2.5.dp, scheme.primary, CircleShape)
                        .background(scheme.surface)
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_icon_fg_1793633718794),
                        contentDescription = stringResource(R.string.cd_brand_icon),
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.brand_title),
                    style = MaterialTheme.typography.displayMedium,
                    color = scheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.brand_tagline),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.secondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                LinearProgressIndicator(
                    progress = { loadProgress },
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = scheme.primary,
                    trackColor = scheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "STUDENT APPLICATION  •  /app",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "OWNER: PRITAM CHOUDHARY",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.tertiary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StudentTermsScreen(
    isFirstLaunchGate: Boolean,
    language: String = "en",
    customTermsText: String = "",
    onAcceptTerms: () -> Unit,
    onBackFromReadOnly: () -> Unit = {}
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val isHindi = language == "hi"

    BackHandler(enabled = !isFirstLaunchGate) {
        onBackFromReadOnly()
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("student_terms_screen"),
        color = scheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 580.dp)
                    .border(1.5.dp, scheme.outline, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = scheme.secondary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isHindi) "नियम और शर्तें (Terms & Conditions)" else "Terms and Conditions",
                                style = MaterialTheme.typography.headlineSmall,
                                color = scheme.onSurface
                            )
                            Text(
                                text = "TL Brain Test — Owned by Pritam Choudhary",
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.secondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val rulesList = if (customTermsText.isNotBlank()) {
                        customTermsText.lines().map { it.trim() }.filter { it.isNotEmpty() }
                    } else if (isHindi) {
                        listOf(
                            "1. आमंत्रण-आधारित प्रवेश: सार्वजनिक पंजीकरण बंद है। केवल एडमिनिस्ट्रेटर प्रीतम चौधरी द्वारा स्वीकृत Google खाते ही छात्र ऐप (/app) का उपयोग कर सकते हैं।",
                            "2. सदस्यता नीति: छात्र पास का शुल्क ₹50 / 3 महीने है, जिसे केवल मैनुअल UPI या नकद द्वारा सक्रिय किया जाता है।",
                            "3. निष्पक्ष खेल और शैक्षणिक ईमानदारी: क्विज़, सामान्य ज्ञान और 2D गेम्स में किसी भी प्रकार की धोखाधड़ी या बॉट के उपयोग पर खाता निलंबित किया जा सकता है।",
                            "4. खाता सुरक्षा: अपना स्वीकृत Gmail खाता अन्य व्यक्तियों के साथ साझा न करें।",
                            "5. ग्राहक सहायता: इन-ऐप सहायता चैट केवल छात्र और एडमिनिस्ट्रेटर के बीच है। छात्रों के बीच आपसी चैट की अनुमति नहीं है।"
                        )
                    } else {
                        listOf(
                            "1. Approved Accounts Only: Public signup is disabled. Access to the TL Brain Test Student Application (/app) is strictly restricted to Google accounts approved by Administrator Pritam Choudhary.",
                            "2. Subscription Policy: Student membership is Rs. 50 for 3 Months via Manual UPI or Cash activation. No automatic card charges occur inside the app.",
                            "3. Fair Play & Academic Integrity: Automated scripts, unfair assistance, or manipulation of quiz timers and leaderboard XP will result in account suspension.",
                            "4. Single-Student Usage: Approved Google accounts are personal and non-transferable.",
                            "5. Safe Support Communication: Customer Support messaging connects you strictly 1-to-1 with the Administrator. Student-to-student messaging is disabled for student safety."
                        )
                    }

                    rulesList.forEach { clause ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = scheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                        ) {
                            Text(
                                text = clause,
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (isFirstLaunchGate) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    (context as? Activity)?.finish()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("terms_exit_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isHindi) "बाहर निकलें" else "Exit")
                            }

                            Button(
                                onClick = onAcceptTerms,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("terms_accept_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isHindi) "स्वीकार करें" else "Accept")
                            }
                        }
                    } else {
                        Button(
                            onClick = onBackFromReadOnly,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("terms_back_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isHindi) "प्रोफ़ाइल पर वापस जाएँ" else "Back to Profile")
                        }
                    }
                }
            }
        }
    }
}
