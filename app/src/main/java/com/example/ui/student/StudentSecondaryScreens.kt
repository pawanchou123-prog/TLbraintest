package com.example.ui.student

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.audio.SoundEffect
import com.example.core.audio.SoundManager
import com.example.data.CurriculumCatalog
import com.example.data.SamplePreviewCatalog
import com.example.data.model.AllowedStudent
import com.example.data.model.AnnouncementItem
import com.example.data.model.AppConfig
import com.example.data.model.SupportMessage
import com.example.data.model.UserProfile

@Composable
fun StudentSubscriptionScreen(
    profile: UserProfile,
    allowedStudent: AllowedStudent?,
    isOwnerTestAccount: Boolean,
    appConfig: AppConfig,
    onOpenSupportChat: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val isHi = profile.language == "hi"

    val isActivePass = allowedStudent?.isSubscriptionCurrentlyValid() == true || isOwnerTestAccount
    val statusText = when {
        allowedStudent?.isSubscriptionCurrentlyValid() == true -> "ACTIVE STUDENT PASS"
        isOwnerTestAccount -> "ACTIVE (OWNER TEST ACCOUNT)"
        else -> "INACTIVE / RENEWAL NEEDED"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("student_subscription_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("subscription_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = scheme.onBackground)
                }
                Text(
                    text = if (isHi) "छात्र सदस्यता (Subscription)" else "Student Subscription Pass",
                    style = MaterialTheme.typography.headlineSmall,
                    color = scheme.onBackground
                )
            }
        }

        // Plan Pricing Hero Card (Rs. 50 / 3 Months)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .border(1.5.dp, scheme.primary, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CardMembership, contentDescription = null, tint = scheme.tertiary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Rs. 50 / 3 Months",
                            style = MaterialTheme.typography.headlineMedium,
                            color = scheme.tertiary
                        )
                    }
                    Text(
                        text = "Complete access to Classes 1–12 quizzes, General Knowledge, IQ & Memory Tests, all 14 playable 2D Brain Games, and Monthly Leaderboard Rewards.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = scheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Subscription Status: $statusText",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isActivePass) scheme.tertiary else scheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Student Gmail: ${profile.email}",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurface
                            )
                            Text(
                                text = "Start Date: ${
                                    if (allowedStudent != null) CurriculumCatalog.formatReadableDate(allowedStudent.subscriptionStartMs)
                                    else "Owner Test Mode"
                                }",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                            Text(
                                text = "Expiry Date: ${
                                    if (allowedStudent != null) CurriculumCatalog.formatReadableDate(allowedStudent.subscriptionEndMs)
                                    else "Unlimited (Owner Test Mode)"
                                }",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Manual Payment Methods Card (UPI & Cash Only — Zero Automatic Gateway)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .border(1.dp, scheme.outline, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isHi) "भुगतान के तरीके (कोई ऑटोमैटिक पेमेंट नहीं)" else "Supported Manual Payment Methods",
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface
                    )

                    Surface(shape = RoundedCornerShape(14.dp), color = scheme.primaryContainer.copy(alpha = 0.55f), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = scheme.secondary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("1. Manual UPI Transfer", style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                                Text("UPI Info: ${appConfig.upiId}", style = MaterialTheme.typography.bodySmall, color = scheme.secondary)
                            }
                        }
                    }

                    Surface(shape = RoundedCornerShape(14.dp), color = scheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = scheme.tertiary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("2. Direct Cash Payment", style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                                Text("Pay Rs. 50 directly in cash to Administrator Pritam Choudhary and share your Gmail for instant activation.", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                            }
                        }
                    }

                    Button(
                        onClick = onOpenSupportChat,
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("subscription_contact_admin_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Get Password / Contact Admin in Support")
                    }

                    if (appConfig.whatsappUrl.startsWith("https://")) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(appConfig.whatsappUrl)))
                                } catch (_: Exception) {
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Contact Admin on WhatsApp")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentCustomerSupportScreen(
    profile: UserProfile,
    messages: List<SupportMessage>,
    onSendMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val scheme = MaterialTheme.colorScheme
    val isHi = profile.language == "hi"
    var draft by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("student_customer_support_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("support_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = scheme.onBackground)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isHi) "ग्राहक सहायता (Customer Support)" else "Customer Support Desk",
                    style = MaterialTheme.typography.titleLarge,
                    color = scheme.onBackground
                )
                Text(
                    text = "Direct 1-to-1 with Administrator Pritam Choudhary",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.secondary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = scheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp)
        ) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = scheme.tertiary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Privacy Protected: Only you and the Administrator can read this thread. Student-to-student messaging is disabled.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = 720.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, scheme.outline.copy(alpha = 0.6f), RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = scheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Start a Support Conversation",
                                style = MaterialTheme.typography.titleMedium,
                                color = scheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ask Administrator Pritam Choudhary about your Rs. 50 / 3-Month subscription, class assignment, or quiz doubts below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(messages, key = { it.id }) { msg ->
                    val fromAdmin = msg.senderRole == "admin"
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (fromAdmin) scheme.secondaryContainer else scheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (fromAdmin) "Administrator (Pritam Choudhary)" else "You (${profile.displayName})",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(msg.text, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text(if (isHi) "अपना संदेश लिखें..." else "Write message to Administrator...") },
                modifier = Modifier.weight(1f).testTag("support_message_input"),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val clean = draft.trim()
                    if (clean.isNotEmpty()) {
                        SoundManager.play(SoundEffect.CHAT_SENT)
                        onSendMessage(clean)
                        draft = ""
                    }
                },
                modifier = Modifier.height(54.dp).testTag("support_message_send_btn"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Support Message")
            }
        }
    }
}

@Composable
fun StudentNotificationsScreen(
    profile: UserProfile,
    liveAnnouncements: List<AnnouncementItem>,
    readIds: Set<String>,
    onMarkRead: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val scheme = MaterialTheme.colorScheme
    val isHi = profile.language == "hi"
    val studentClass = profile.studentClass.toInt().coerceIn(1, 12)

    val filteredLive = remember(liveAnnouncements, studentClass) {
        liveAnnouncements.filter { it.targetClass == 0L || it.targetClass.toInt() == studentClass }
    }
    val showingSample = filteredLive.isEmpty()
    val displayItems = if (showingSample) SamplePreviewCatalog.sampleNotifications else filteredLive

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("student_notifications_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("notifications_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = scheme.onBackground)
                }
                Text(
                    text = if (isHi) "सूचनाएँ और अपडेट (Notifications)" else "In-App Notifications",
                    style = MaterialTheme.typography.headlineSmall,
                    color = scheme.onBackground
                )
            }
        }

        if (showingSample) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = scheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp)
                ) {
                    Text(
                        text = "SAMPLE PREVIEW NOTIFICATIONS — Live Firestore /announcements collection is currently empty.",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.secondary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        items(displayItems, key = { it.id }) { item ->
            val isRead = readIds.contains(item.id)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .border(
                        width = if (isRead) 1.dp else 1.5.dp,
                        color = if (isRead) scheme.outline.copy(alpha = 0.5f) else scheme.primary,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable {
                        SoundManager.play(SoundEffect.NOTIFICATION)
                        onMarkRead(item.id)
                    },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = if (isRead) Icons.Default.Notifications else Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = if (isRead) scheme.onSurfaceVariant else scheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "[${item.category.uppercase()}] ${item.title}",
                            style = MaterialTheme.typography.titleMedium,
                            color = scheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant
                        )
                    }
                    if (isRead) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Read", tint = scheme.secondary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Push Notification Spark-Plan Readiness Card
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = scheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Free Push & Real-Time Delivery Status",
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.secondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "In-app notifications sync live via free Firestore snapshots. Background push alerts are prepared for free Firebase Cloud Messaging (FCM) topic broadcasts from the Admin Console without paid Cloud Functions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
