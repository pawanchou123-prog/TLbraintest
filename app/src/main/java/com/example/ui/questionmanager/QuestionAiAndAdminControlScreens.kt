package com.example.ui.questionmanager

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CurriculumCatalog
import com.example.data.model.AllowedQuestionManager
import com.example.data.model.QuestionAuditLog
import com.example.data.model.SubjectCatalogDoc
import com.example.data.repository.QuestionManagerOperatorSession

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuestionAiPrepScreen(
    subjectCatalog: SubjectCatalogDoc,
    aiStatus: AiPrepStatus,
    onRunFreeTierAiCheck: (studentClass: Int, subject: String, topic: String, difficulty: String, count: Int, language: String) -> Unit,
    onSaveReviewedAiDrafts: () -> Unit,
    onResetAiState: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var selectedClass by remember { mutableIntStateOf(10) }
    val subjects = CurriculumCatalog.questionManagerSubjectsForClass(selectedClass, subjectCatalog)
    var selectedSubject by remember { mutableStateOf("Mathematics") }
    var topic by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("medium") }
    var questionCount by remember { mutableIntStateOf(3) }
    var language by remember { mutableStateOf("en") }

    LaunchedEffect(selectedClass, subjects) {
        if (selectedSubject !in subjects && subjects.isNotEmpty()) {
            selectedSubject = subjects.first()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("qm_ai_prep_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp)
                    .border(1.2.dp, scheme.primary, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = scheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI-Assisted Question Generation (Preparation Module)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurface
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = scheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = scheme.secondary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Spark Free-Tier Protection & Key Security Status",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = scheme.onSurface
                                )
                                Text(
                                    text = "1. Zero Gemini API keys are stored or exposed anywhere in frontend/client code.\n" +
                                        "2. Paid APIs and Firebase Blaze billing have NOT been activated.\n" +
                                        "3. On standalone Spark free-tier deployments without server-side Firebase AI Logic + App Check enabled, live AI generation remains marked 'Not Configured'.\n" +
                                        "4. Any generated items always require mandatory human review and are saved strictly as unpublished Drafts.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Text("Language", style = MaterialTheme.typography.labelLarge, color = scheme.secondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = language == "en", onClick = { language = "en" }, label = { Text("English") })
                        FilterChip(selected = language == "hi", onClick = { language = "hi" }, label = { Text("Hindi (हिन्दी)") })
                    }

                    Text("Class (1 to 12 or GK)", style = MaterialTheme.typography.labelLarge, color = scheme.secondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..12).forEach { cls ->
                            FilterChip(selected = selectedClass == cls, onClick = { selectedClass = cls }, label = { Text("Class $cls") })
                        }
                        FilterChip(selected = selectedClass == 0, onClick = { selectedClass = 0 }, label = { Text("GK") })
                    }

                    Text("Subject", style = MaterialTheme.typography.labelLarge, color = scheme.secondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        subjects.forEach { subj ->
                            FilterChip(
                                selected = selectedSubject == subj,
                                onClick = { selectedSubject = subj },
                                label = { Text(subj) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = topic,
                        onValueChange = { topic = it.take(120) },
                        label = { Text("Topic / Chapter Focus") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("qm_ai_topic_input")
                    )

                    Text("Difficulty & Number of Draft Questions", style = MaterialTheme.typography.labelLarge, color = scheme.secondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("easy", "medium", "hard").forEach { diff ->
                            FilterChip(
                                selected = difficulty == diff,
                                onClick = { difficulty = diff },
                                label = { Text(diff.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 3, 5, 8).forEach { n ->
                            FilterChip(
                                selected = questionCount == n,
                                onClick = { questionCount = n },
                                label = { Text("$n Question(s)") }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onRunFreeTierAiCheck(selectedClass, selectedSubject, topic, difficulty, questionCount, language)
                        },
                        enabled = aiStatus !is AiPrepStatus.Generating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qm_ai_generate_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (aiStatus is AiPrepStatus.Generating) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = scheme.onPrimary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Checking Free-Tier Firebase AI Logic...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Drafts for Human Review (If Free-Tier Configured)")
                        }
                    }
                }
            }
        }

        when (aiStatus) {
            is AiPrepStatus.NotConfiguredOrUnavailable -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 840.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = scheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Status: Not Configured on Current Free Tier",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = scheme.onErrorContainer
                            )
                            Text(
                                text = aiStatus.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onErrorContainer
                            )
                        }
                    }
                }
            }
            is AiPrepStatus.DraftsReadyForHumanReview -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 840.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = scheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Human Review Required (${aiStatus.drafts.size} Generated Drafts)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = scheme.onPrimaryContainer
                            )
                            Text(
                                text = "Inspect every generated question, option, and explanation below. Confirming saves them strictly as unpublished Drafts so you or Main Admin can edit and approve them.",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onPrimaryContainer
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = onSaveReviewedAiDrafts, shape = RoundedCornerShape(10.dp)) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save All as Unpublished Drafts")
                                }
                                OutlinedButton(onClick = onResetAiState, shape = RoundedCornerShape(10.dp)) {
                                    Text("Discard")
                                }
                            }
                        }
                    }
                }
                items(aiStatus.drafts) { draft ->
                    Card(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 840.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = scheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(draft.questionText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            draft.options.forEachIndexed { i, opt ->
                                val tag = if (i == draft.correctIndex) " [Correct]" else ""
                                Text("${'A' + i}) $opt$tag", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                            }
                            Text("Explanation: ${draft.explanation}", style = MaterialTheme.typography.labelSmall, color = scheme.secondary)
                        }
                    }
                }
            }
            else -> Unit
        }
    }
}

@Composable
fun QuestionManagerAdminControlsScreen(
    session: QuestionManagerOperatorSession,
    allowedManagers: List<AllowedQuestionManager>,
    subjectCatalog: SubjectCatalogDoc,
    auditLogs: List<QuestionAuditLog>,
    onSaveManager: (email: String, name: String, isActive: Boolean, canPublish: Boolean, canDelete: Boolean, notes: String) -> Unit,
    onRevokeManager: (AllowedQuestionManager) -> Unit,
    onSaveSubjectCatalog: (c1To5: String, c6To8: String, c9To10: String, c11To12: String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    if (!session.isMainAdmin) return

    var mgrEmail by remember { mutableStateOf("") }
    var mgrName by remember { mutableStateOf("") }
    var mgrCanPublish by remember { mutableStateOf(false) }
    var mgrCanDelete by remember { mutableStateOf(true) }
    var mgrNotes by remember { mutableStateOf("") }

    var c1To5Text by remember(subjectCatalog) { mutableStateOf(subjectCatalog.classes1To5.joinToString(", ")) }
    var c6To8Text by remember(subjectCatalog) { mutableStateOf(subjectCatalog.classes6To8.joinToString(", ")) }
    var c9To10Text by remember(subjectCatalog) { mutableStateOf(subjectCatalog.classes9To10.joinToString(", ")) }
    var c11To12Text by remember(subjectCatalog) { mutableStateOf(subjectCatalog.classes11To12.joinToString(", ")) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("qm_admin_controls_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Authorize Question Manager Accounts
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 860.dp)
                    .border(1.2.dp, scheme.primary, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = scheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Main Admin Control: Authorize Question Managers",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurface
                        )
                    }
                    Text(
                        text = "Approve Gmail accounts allowed to sign into /question-manager. Question Managers never receive Main Admin permissions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = mgrEmail,
                        onValueChange = { mgrEmail = it.take(120) },
                        label = { Text("Question Manager Gmail Address *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("qm_admin_mgr_email_input")
                    )
                    OutlinedTextField(
                        value = mgrName,
                        onValueChange = { mgrName = it.take(80) },
                        label = { Text("Full Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("qm_admin_mgr_name_input")
                    )
                    OutlinedTextField(
                        value = mgrNotes,
                        onValueChange = { mgrNotes = it.take(250) },
                        label = { Text("Assigned Classes / Notes (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = mgrCanPublish, onCheckedChange = { mgrCanPublish = it })
                        Text("Allow direct publishing without Main Admin review", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = mgrCanDelete, onCheckedChange = { mgrCanDelete = it })
                        Text("Allow deleting own unpublished draft questions", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = {
                            onSaveManager(mgrEmail, mgrName, true, mgrCanPublish, mgrCanDelete, mgrNotes)
                            mgrEmail = ""
                            mgrName = ""
                            mgrNotes = ""
                        },
                        enabled = mgrEmail.contains("@") && mgrName.isNotBlank(),
                        modifier = Modifier.testTag("qm_admin_authorize_mgr_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Authorize Question Manager")
                    }

                    allowedManagers.forEach { mgr ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = scheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${mgr.managerName} (${mgr.email})", style = MaterialTheme.typography.labelLarge, color = scheme.onSurface)
                                    Text(
                                        text = "Status: ${if (mgr.isActive) "ACTIVE" else "SUSPENDED"}  •  Direct Publish: ${if (mgr.canPublishDirectly) "YES" else "NO"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = scheme.secondary
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            onSaveManager(mgr.email, mgr.managerName, !mgr.isActive, mgr.canPublishDirectly, mgr.canDeleteQuestions, mgr.notes)
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(if (mgr.isActive) "Suspend" else "Activate")
                                    }
                                    OutlinedButton(
                                        onClick = { onRevokeManager(mgr) },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Revoke", tint = scheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Class 1-12 Subject Organization Control
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 860.dp)
                    .border(1.dp, scheme.outline, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Main Admin Control: Class 1 to 12 Subject Organization",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface
                    )
                    Text(
                        text = "Customize comma-separated subject lists used across Question Management.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                    OutlinedTextField(value = c1To5Text, onValueChange = { c1To5Text = it }, label = { Text("Classes 1 to 5 Subjects") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = c6To8Text, onValueChange = { c6To8Text = it }, label = { Text("Classes 6 to 8 Subjects") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = c9To10Text, onValueChange = { c9To10Text = it }, label = { Text("Classes 9 to 10 Subjects") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = c11To12Text, onValueChange = { c11To12Text = it }, label = { Text("Classes 11 to 12 Subjects") }, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = { onSaveSubjectCatalog(c1To5Text, c6To8Text, c9To10Text, c11To12Text) },
                        modifier = Modifier.testTag("qm_admin_save_subjects_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Subject Organization")
                    }
                }
            }
        }

        // 3. Immutable Question Editorial Audit Logs
        item {
            Row(
                modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = scheme.secondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Question Editorial Audit Trail (${auditLogs.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onBackground
                )
            }
        }

        items(auditLogs.take(30), key = { it.id }) { log ->
            Card(
                modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val dateStr = log.createdAt?.toDate()?.let { CurriculumCatalog.formatReadableDate(it.time) } ?: "Recent"
                    Text(
                        text = "${log.actionType.uppercase()}  •  ${log.questionSummary}",
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.secondary
                    )
                    Text(text = log.details, style = MaterialTheme.typography.bodySmall, color = scheme.onSurface)
                    Text(
                        text = "Editor: ${log.editorEmail} (${log.editorRole})  •  Date: $dateStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
