package com.example.ui.admin

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
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminAuditLog
import com.example.data.model.AdminDashboardStats
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun formatAdminTimestamp(epochSeconds: Long?): String {
    if (epochSeconds == null || epochSeconds <= 0L) return "Just now"
    return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(epochSeconds * 1000L))
}

@Composable
fun AdminDashboardScreen(
    adminEmail: String,
    stats: AdminDashboardStats,
    recentLogs: List<AdminAuditLog>,
    onOpenStudents: () -> Unit,
    onOpenSubscriptions: () -> Unit,
    onOpenContent: () -> Unit,
    onOpenLeaderboards: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenAuditLogs: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    val statPairs = listOf(
        Triple("Total Students", stats.totalStudents.toString(), Icons.Default.Group) to
            Triple("Active Subscriptions", stats.activeSubscriptions.toString(), Icons.Default.CheckCircle),
        Triple("Expired Subscriptions", stats.expiredSubscriptions.toString(), Icons.Default.EventBusy) to
            Triple("Banned Students", stats.bannedStudents.toString(), Icons.Default.Block),
        Triple("Total Quiz Attempts", stats.totalQuizAttempts.toString(), Icons.Default.Quiz) to
            Triple("Total Questions (${stats.publishedQuestions} Live)", stats.totalQuestions.toString(), Icons.AutoMirrored.Filled.MenuBook),
        Triple("Total Points Earned", stats.totalPoints.toString(), Icons.Default.Stars) to
            Triple("Today's Active Users", stats.todaysActiveUsers.toString(), Icons.AutoMirrored.Filled.TrendingUp)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Executive Header Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 860.dp)
                    .border(1.5.dp, scheme.primary, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = scheme.tertiary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TL BRAIN TEST – MAIN ADMIN PANEL",
                            style = MaterialTheme.typography.titleLarge,
                            color = scheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Owner: PRITAM CHOUDHARY  •  Tagline: Learn, Think, Play, Improve.",
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.secondary
                    )
                    Text(
                        text = "Verified Session: $adminEmail  •  Live Cloud Firestore Telemetry (Zero Simulated Numbers)",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
        }

        // Actionable Alerts Row (Pending Payments & Unread Support)
        if (stats.pendingRequestsCount > 0 || stats.unreadSupportCount > 0) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 860.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (stats.pendingRequestsCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = scheme.primaryContainer,
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, scheme.primary, RoundedCornerShape(16.dp))
                                .clickable { onOpenSubscriptions() }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PendingActions, contentDescription = null, tint = scheme.tertiary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${stats.pendingRequestsCount} Pending Requests",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = scheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Review UPI/Cash & access requests",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.secondary
                                    )
                                }
                            }
                        }
                    }
                    if (stats.unreadSupportCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = scheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, scheme.tertiary, RoundedCornerShape(16.dp))
                                .clickable { onOpenSupport() }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = scheme.tertiary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${stats.unreadSupportCount} Unread Support Inquiries",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = scheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Tap to open Support Inbox",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 8 Real Firebase Statistics Grid
        items(statPairs) { (left, right) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 860.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminStatMetricCard(
                    title = left.first,
                    value = left.second,
                    icon = left.third,
                    modifier = Modifier.weight(1f)
                )
                AdminStatMetricCard(
                    title = right.first,
                    value = right.second,
                    icon = right.third,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick Module Shortcuts Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 860.dp)
                    .border(1.dp, scheme.outline, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Main Admin Operations",
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onOpenStudents,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("dash_open_students_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Students")
                        }
                        Button(
                            onClick = onOpenSubscriptions,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("dash_open_subscriptions_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CardMembership, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Subscriptions")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenContent,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("dash_open_content_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Content")
                        }
                        OutlinedButton(
                            onClick = onOpenLeaderboards,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("dash_open_leaderboards_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Leaderboards")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenSupport,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("dash_open_support_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Support Inbox")
                        }
                        OutlinedButton(
                            onClick = onOpenAuditLogs,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("dash_open_audit_logs_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Audit Logs")
                        }
                    }
                }
            }
        }

        // Recent Admin Audit Logs Preview
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 860.dp)
                    .border(1.dp, scheme.outline, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.FactCheck, contentDescription = null, tint = scheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Recent Admin Audit Trail",
                                style = MaterialTheme.typography.titleMedium,
                                color = scheme.onSurface
                            )
                        }
                        OutlinedButton(onClick = onOpenAuditLogs, shape = RoundedCornerShape(10.dp)) {
                            Text("View All (${recentLogs.size})")
                        }
                    }

                    if (recentLogs.isEmpty()) {
                        Text(
                            text = "No administrative actions recorded yet in /admin_audit_logs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    } else {
                        recentLogs.take(5).forEach { log ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = scheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "${log.actionType.uppercase()}  •  ${log.targetLabel} (${log.targetId})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = scheme.onSurface
                                    )
                                    Text(
                                        text = log.reasonOrDetails,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "By ${log.adminEmail}  •  ${formatAdminTimestamp(log.createdAt?.seconds)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = scheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminStatMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier.border(1.dp, scheme.outline.copy(alpha = 0.75f), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = title, tint = scheme.secondary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = scheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AdminAuditLogsScreen(
    logs: List<AdminAuditLog>,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val scheme = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }

    val filtered = remember(logs, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) logs
        else logs.filter {
            it.actionType.lowercase().contains(q) ||
                it.targetId.lowercase().contains(q) ||
                it.targetLabel.lowercase().contains(q) ||
                it.adminEmail.lowercase().contains(q) ||
                it.reasonOrDetails.lowercase().contains(q)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_audit_logs_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("audit_logs_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = scheme.onBackground)
                }
                Column {
                    Text(
                        text = "Immutable Admin Audit Logs",
                        style = MaterialTheme.typography.headlineSmall,
                        color = scheme.onBackground
                    )
                    Text(
                        text = "Server-enforced append-only history of student, points, subscription, and content changes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.secondary
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Filter by action, student Gmail, admin email, or reason") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp)
            )
        }

        if (filtered.isEmpty()) {
            item {
                Text(
                    text = "No matching audit records found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            items(filtered, key = { it.id.ifBlank { it.hashCode().toString() } }) { log ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 860.dp)
                        .border(1.dp, scheme.outline, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = log.actionType.uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                color = scheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatAdminTimestamp(log.createdAt?.seconds),
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.secondary
                            )
                        }
                        Text(
                            text = "Target: ${log.targetLabel} (${log.targetId})",
                            style = MaterialTheme.typography.titleSmall,
                            color = scheme.onSurface
                        )
                        if (log.pointsDelta != 0L) {
                            val sign = if (log.pointsDelta > 0) "+${log.pointsDelta}" else "${log.pointsDelta}"
                            Text(
                                text = "Points Delta: $sign",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (log.pointsDelta > 0) scheme.tertiary else scheme.error
                            )
                        }
                        Text(
                            text = "Reason / Details: ${log.reasonOrDetails}",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                        Text(
                            text = "Executed by Admin: ${log.adminEmail}",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.secondary
                        )
                    }
                }
            }
        }
    }
}
