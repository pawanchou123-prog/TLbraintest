package com.example.ui.questionmanager

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.QuestionDashboardStats
import com.example.data.model.QuestionItem
import com.example.data.repository.QuestionManagerOperatorSession

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuestionDashboardScreen(
    session: QuestionManagerOperatorSession,
    stats: QuestionDashboardStats,
    onAddNewQuestion: () -> Unit,
    onOpenQuestionBank: () -> Unit,
    onOpenImportExport: () -> Unit,
    onEditRecentQuestion: (QuestionItem) -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("qm_dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header & Role Badge Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 860.dp)
                    .border(1.2.dp, scheme.primary, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TL BRAIN TEST – QUESTION MANAGEMENT",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = scheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Owner: PRITAM CHOUDHARY  •  Signed in: ${session.email}",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.secondary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (session.isMainAdmin) scheme.tertiaryContainer else scheme.primaryContainer
                        ) {
                            Text(
                                text = if (session.isMainAdmin) "MAIN ADMIN" else "QUESTION MANAGER",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (session.isMainAdmin) scheme.onTertiaryContainer else scheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onAddNewQuestion,
                            modifier = Modifier.testTag("qm_dash_add_question_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add New Question")
                        }
                        OutlinedButton(
                            onClick = onOpenQuestionBank,
                            modifier = Modifier.testTag("qm_dash_open_bank_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Manage Question Bank")
                        }
                        OutlinedButton(
                            onClick = onOpenImportExport,
                            modifier = Modifier.testTag("qm_dash_open_csv_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bulk CSV Import / Export")
                        }
                    }
                }
            }
        }

        // Real Database Summary Metrics (Zero Fake Numbers)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 860.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Live Firestore Question Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onBackground
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Total Questions",
                        value = stats.totalQuestions.toString(),
                        icon = Icons.AutoMirrored.Filled.LibraryBooks
                    )
                    StatMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Published (Live)",
                        value = stats.publishedQuestions.toString(),
                        icon = Icons.Default.CheckCircle
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Draft Questions",
                        value = stats.draftQuestions.toString(),
                        icon = Icons.Default.EditNote
                    )
                    StatMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Pending Review / Rejected",
                        value = "${stats.pendingReviewQuestions} / ${stats.rejectedQuestions}",
                        icon = Icons.Default.HourglassTop
                    )
                }
            }
        }

        // Questions by Class (GK + Classes 1 to 12)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 860.dp)
                    .border(1.dp, scheme.outlineVariant, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.School, contentDescription = null, tint = scheme.secondary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Questions by Class (Classes 1 to 12 & GK)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = scheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val gkCount = stats.byClassCounts[0] ?: 0
                        AssistChip(
                            onClick = onOpenQuestionBank,
                            label = { Text("GK (All): $gkCount") }
                        )
                        (1..12).forEach { cls ->
                            val cnt = stats.byClassCounts[cls] ?: 0
                            AssistChip(
                                onClick = onOpenQuestionBank,
                                label = { Text("Class $cls: $cnt") }
                            )
                        }
                    }
                }
            }
        }

        // Questions by Subject
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 860.dp)
                    .border(1.dp, scheme.outlineVariant, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.FactCheck, contentDescription = null, tint = scheme.secondary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Questions by Subject",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = scheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if (stats.bySubjectCounts.isEmpty()) {
                        Text(
                            text = "0 questions recorded across subjects yet. Add a question or import a CSV file to populate real subject counts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            stats.bySubjectCounts.forEach { (subject, count) ->
                                AssistChip(
                                    onClick = onOpenQuestionBank,
                                    label = { Text("$subject: $count") }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recently Added Questions
        item {
            Text(
                text = "Recently Added Questions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 860.dp)
            )
        }

        if (stats.recentQuestions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 860.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant)
                ) {
                    Text(
                        text = "No questions have been added to Firestore yet. Tap 'Add New Question' to create your first question.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(stats.recentQuestions, key = { it.id }) { q ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 860.dp)
                        .border(1.dp, scheme.outlineVariant, RoundedCornerShape(16.dp))
                        .clickable { onEditRecentQuestion(q) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val classTag = if (q.studentClass == 0L) "GK" else "Class ${q.studentClass}"
                            Text(
                                text = "$classTag  •  ${q.subject}" + if (q.topic.isNotBlank()) "  •  ${q.topic}" else "",
                                style = MaterialTheme.typography.labelLarge,
                                color = scheme.secondary
                            )
                            StatusBadgePill(isPublished = q.isPublished, reviewStatus = q.effectiveReviewStatus)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = q.questionText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurface,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Language: ${if (q.language == "hi") "Hindi" else "English"}  •  Difficulty: ${q.difficulty.uppercase()}  •  Points: ${q.points}",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun StatMetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier.border(1.dp, scheme.outline, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = scheme.secondary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface
            )
        }
    }
}

@Composable
internal fun StatusBadgePill(isPublished: Boolean, reviewStatus: String) {
    val scheme = MaterialTheme.colorScheme
    val (bg, fg, label) = when {
        isPublished -> Triple(scheme.tertiaryContainer, scheme.onTertiaryContainer, "PUBLISHED")
        reviewStatus == "pending_review" -> Triple(scheme.secondaryContainer, scheme.onSecondaryContainer, "IN REVIEW")
        reviewStatus == "rejected" -> Triple(scheme.errorContainer, scheme.onErrorContainer, "REJECTED")
        else -> Triple(scheme.surfaceVariant, scheme.onSurfaceVariant, "DRAFT")
    }
    Surface(shape = RoundedCornerShape(8.dp), color = bg) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
