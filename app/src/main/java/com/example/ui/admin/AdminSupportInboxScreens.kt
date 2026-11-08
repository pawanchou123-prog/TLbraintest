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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SupportMessage
import com.example.data.model.SupportThread

@Composable
fun AdminSupportInboxScreen(
    threads: List<SupportThread>,
    onOpenThread: (String) -> Unit,
    onToggleUnread: (String, Boolean) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var unreadOnly by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(threads, unreadOnly, searchQuery) {
        val q = searchQuery.trim().lowercase()
        threads.filter { t ->
            val matchRead = !unreadOnly || t.unreadByAdmin
            val matchText = q.isEmpty() ||
                t.studentName.lowercase().contains(q) ||
                t.studentEmail.lowercase().contains(q) ||
                t.lastMessage.lowercase().contains(q)
            matchRead && matchText
        }
    }

    val unreadCount = threads.count { it.unreadByAdmin }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_support_inbox_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp).border(1.dp, scheme.primary, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null, tint = scheme.tertiary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "1-to-1 Student Support Inbox ($unreadCount Unread)",
                            style = MaterialTheme.typography.titleMedium,
                            color = scheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Firestore-enforced private Student-to-Admin support threads. Student-to-student chat is strictly prohibited by security rules.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("Search student name, Gmail, or inquiry message") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !unreadOnly,
                        onClick = { unreadOnly = false },
                        label = { Text("All Threads (${threads.size})") }
                    )
                    FilterChip(
                        selected = unreadOnly,
                        onClick = { unreadOnly = true },
                        label = { Text("Unread Only ($unreadCount)") }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Text(
                    text = "No student support inquiries match the current filter.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            items(filtered, key = { it.id.ifBlank { it.studentUid } }) { thread ->
                val uid = thread.studentUid.ifBlank { thread.id }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 860.dp)
                        .border(
                            width = if (thread.unreadByAdmin) 1.5.dp else 1.dp,
                            color = if (thread.unreadByAdmin) scheme.tertiary else scheme.outline,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onOpenThread(uid) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${thread.studentName}  •  Class ${thread.studentClass}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = scheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = thread.studentEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.secondary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (thread.unreadByAdmin) scheme.tertiary.copy(alpha = 0.2f) else scheme.surfaceVariant
                            ) {
                                Text(
                                    text = if (thread.unreadByAdmin) "UNREAD" else "READ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (thread.unreadByAdmin) scheme.tertiary else scheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = thread.lastMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Updated: ${formatAdminTimestamp(thread.updatedAt?.seconds)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.secondary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onToggleUnread(uid, !thread.unreadByAdmin) },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (thread.unreadByAdmin) Icons.Default.MarkEmailRead else Icons.Default.MarkEmailUnread,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (thread.unreadByAdmin) "Mark Read" else "Mark Unread")
                                }
                                Button(
                                    onClick = { onOpenThread(uid) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("open_support_thread_$uid")
                                ) {
                                    Text("Open & Reply")
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
fun AdminSupportThreadScreen(
    thread: SupportThread?,
    messages: List<SupportMessage>,
    onMarkRead: () -> Unit,
    onSendReply: (SupportThread, String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val scheme = MaterialTheme.colorScheme
    var replyText by remember { mutableStateOf("") }

    LaunchedEffect(thread?.studentUid, thread?.unreadByAdmin) {
        if (thread?.unreadByAdmin == true) {
            onMarkRead()
        }
    }

    if (thread == null) {
        Surface(modifier = Modifier.fillMaxSize(), color = scheme.background) {
            Column(modifier = Modifier.padding(24.dp)) {
                OutlinedButton(onClick = onBack) { Text("Back to Support Inbox") }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Support thread not found.", color = scheme.onBackground)
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_support_thread_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("support_thread_back_btn")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = scheme.onBackground)
            }
            Column {
                Text(
                    text = "${thread.studentName} (Class ${thread.studentClass})",
                    style = MaterialTheme.typography.titleLarge,
                    color = scheme.onBackground
                )
                Text(
                    text = "${thread.studentEmail}  •  1-to-1 Student Support",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.secondary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = 860.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Text(
                        text = "No messages in this thread yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            } else {
                items(messages, key = { it.id.ifBlank { it.hashCode().toString() } }) { msg ->
                    val isAdmin = msg.senderRole == "admin"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isAdmin) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isAdmin) scheme.primaryContainer else scheme.surface,
                            modifier = Modifier
                                .widthIn(max = 520.dp)
                                .border(1.dp, if (isAdmin) scheme.primary else scheme.outline, RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "${if (isAdmin) "ADMIN" else "STUDENT"}  •  ${msg.senderName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isAdmin) scheme.tertiary else scheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = scheme.onSurface
                                )
                                Text(
                                    text = formatAdminTimestamp(msg.createdAt?.seconds),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = replyText,
                onValueChange = { replyText = it },
                label = { Text("Write reply to ${thread.studentName}") },
                modifier = Modifier.weight(1f).testTag("admin_support_reply_input")
            )
            Button(
                onClick = {
                    val clean = replyText.trim()
                    if (clean.isNotEmpty()) {
                        onSendReply(thread, clean)
                        replyText = ""
                    }
                },
                modifier = Modifier.height(54.dp).testTag("admin_support_send_btn"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Reply")
            }
        }
    }
}
