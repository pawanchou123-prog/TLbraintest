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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Unpublished
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import com.example.data.model.QuestionItem
import com.example.data.model.SubjectCatalogDoc
import com.example.data.repository.QuestionManagerOperatorSession

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuestionBankScreen(
    session: QuestionManagerOperatorSession,
    questions: List<QuestionItem>,
    subjectCatalog: SubjectCatalogDoc,
    onEditQuestion: (QuestionItem) -> Unit,
    onSubmitForReview: (QuestionItem) -> Unit,
    onApproveOrReject: (question: QuestionItem, newStatus: String, publishNow: Boolean, reviewNotes: String) -> Unit,
    onDeleteQuestion: (QuestionItem) -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    var searchQuery by remember { mutableStateOf("") }
    var selectedClassFilter by remember { mutableIntStateOf(-1) } // -1 = All, 0 = GK, 1..12
    var selectedSubjectFilter by remember { mutableStateOf("All") }
    var topicFilter by remember { mutableStateOf("") }
    var selectedDifficultyFilter by remember { mutableStateOf("All") }
    var selectedStatusFilter by remember { mutableStateOf("All") }

    var rejectTarget by remember { mutableStateOf<QuestionItem?>(null) }
    var rejectNotes by remember { mutableStateOf("") }
    var deleteConfirmTarget by remember { mutableStateOf<QuestionItem?>(null) }

    val subjectOptions by remember(selectedClassFilter, questions, subjectCatalog) {
        derivedStateOf {
            val fromCatalog = if (selectedClassFilter in 0..12) {
                CurriculumCatalog.questionManagerSubjectsForClass(selectedClassFilter, subjectCatalog)
            } else {
                questions.map { it.subject }.filter { it.isNotBlank() }
            }
            (listOf("All") + fromCatalog + questions.map { it.subject }.filter { it.isNotBlank() }).distinct()
        }
    }

    val filteredQuestions by remember(
        questions, searchQuery, selectedClassFilter, selectedSubjectFilter,
        topicFilter, selectedDifficultyFilter, selectedStatusFilter
    ) {
        derivedStateOf {
            questions.filter { q ->
                val matchesSearch = searchQuery.isBlank() ||
                    q.questionText.contains(searchQuery.trim(), ignoreCase = true) ||
                    q.explanation.contains(searchQuery.trim(), ignoreCase = true) ||
                    q.subject.contains(searchQuery.trim(), ignoreCase = true)
                val matchesClass = selectedClassFilter == -1 || q.studentClass.toInt() == selectedClassFilter
                val matchesSubj = selectedSubjectFilter == "All" || q.subject.equals(selectedSubjectFilter, ignoreCase = true)
                val matchesTopic = topicFilter.isBlank() || q.topic.contains(topicFilter.trim(), ignoreCase = true)
                val matchesDiff = selectedDifficultyFilter == "All" || q.difficulty.equals(selectedDifficultyFilter, ignoreCase = true)
                val matchesStatus = when (selectedStatusFilter) {
                    "Published" -> q.isPublished
                    "Draft" -> !q.isPublished && q.effectiveReviewStatus == "draft"
                    "In Review" -> !q.isPublished && q.effectiveReviewStatus == "pending_review"
                    "Rejected" -> !q.isPublished && q.effectiveReviewStatus == "rejected"
                    else -> true
                }
                matchesSearch && matchesClass && matchesSubj && matchesTopic && matchesDiff && matchesStatus
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("qm_bank_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Search & Multi-Filter Control Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 880.dp)
                    .border(1.dp, scheme.outline, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Question Bank & Review Queue (${filteredQuestions.size} / ${questions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search question text or explanation") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("qm_bank_search_input")
                        )
                        OutlinedTextField(
                            value = topicFilter,
                            onValueChange = { topicFilter = it },
                            label = { Text("Filter Topic") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(0.75f)
                                .testTag("qm_bank_topic_filter")
                        )
                    }

                    Text("Status Filter", style = MaterialTheme.typography.labelMedium, color = scheme.secondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("All", "Published", "Draft", "In Review", "Rejected").forEach { st ->
                            FilterChip(
                                selected = selectedStatusFilter == st,
                                onClick = { selectedStatusFilter = st },
                                label = { Text(st) }
                            )
                        }
                    }

                    Text("Class Filter", style = MaterialTheme.typography.labelMedium, color = scheme.secondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedClassFilter == -1,
                            onClick = { selectedClassFilter = -1; selectedSubjectFilter = "All" },
                            label = { Text("All Classes") }
                        )
                        FilterChip(
                            selected = selectedClassFilter == 0,
                            onClick = { selectedClassFilter = 0; selectedSubjectFilter = "All" },
                            label = { Text("GK") }
                        )
                        (1..12).forEach { cls ->
                            FilterChip(
                                selected = selectedClassFilter == cls,
                                onClick = { selectedClassFilter = cls; selectedSubjectFilter = "All" },
                                label = { Text("C$cls") }
                            )
                        }
                    }

                    Text("Subject & Difficulty Filter", style = MaterialTheme.typography.labelMedium, color = scheme.secondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        subjectOptions.take(14).forEach { subj ->
                            FilterChip(
                                selected = selectedSubjectFilter == subj,
                                onClick = { selectedSubjectFilter = subj },
                                label = { Text(subj) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("All", "easy", "medium", "hard").forEach { diff ->
                            FilterChip(
                                selected = selectedDifficultyFilter == diff,
                                onClick = { selectedDifficultyFilter = diff },
                                label = { Text(diff.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }
            }
        }

        if (filteredQuestions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 880.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant)
                ) {
                    Text(
                        text = "No questions match the selected filters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            items(filteredQuestions, key = { it.id }) { q ->
                val isOwn = q.authorId == session.uid
                val canEdit = session.isMainAdmin || (isOwn && (!q.isPublished || session.canPublishDirectly))
                val canDelete = session.isMainAdmin || (isOwn && !q.isPublished && session.canDeleteQuestions)
                val canSubmitReview = !session.isMainAdmin && isOwn && !q.isPublished && q.effectiveReviewStatus != "pending_review"
                val canPublishToggle = session.isMainAdmin || (isOwn && session.canPublishDirectly)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 880.dp)
                        .border(1.dp, scheme.outlineVariant, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val classLbl = if (q.studentClass == 0L) "GK (All)" else "Class ${q.studentClass}"
                            Text(
                                text = "$classLbl  •  ${q.subject}" + if (q.topic.isNotBlank()) "  •  ${q.topic}" else "",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                            StatusBadgePill(isPublished = q.isPublished, reviewStatus = q.effectiveReviewStatus)
                        }

                        Text(
                            text = q.questionText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = scheme.onSurface
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            val letters = listOf("A", "B", "C", "D")
                            q.options.take(4).forEachIndexed { idx, opt ->
                                val isAnswer = idx == q.correctIndex.toInt()
                                Text(
                                    text = "${letters[idx]}) $opt" + if (isAnswer) "  [Correct]" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isAnswer) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isAnswer) scheme.tertiary else scheme.onSurfaceVariant
                                )
                            }
                        }

                        if (q.explanation.isNotBlank()) {
                            Text(
                                text = "Explanation: ${q.explanation}",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                        }

                        if (q.reviewNotes.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = scheme.secondaryContainer.copy(alpha = 0.65f)
                            ) {
                                Text(
                                    text = "Admin Review Note: ${q.reviewNotes}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onSecondaryContainer,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        val editorTag = q.lastEditedByEmail.ifBlank { q.authorEmail.ifBlank { q.authorId.take(10) } }
                        val updatedText = q.updatedAt?.toDate()?.let { CurriculumCatalog.formatReadableDate(it.time) } ?: "Just now"
                        Text(
                            text = "Lang: ${q.language.uppercase()}  •  Diff: ${q.difficulty.uppercase()}  •  ${q.points} pts / ${q.timerSeconds}s  •  Source: ${q.source}  •  Editor: $editorTag ($updatedText)",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (canEdit) {
                                OutlinedButton(
                                    onClick = { onEditQuestion(q) },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit")
                                }
                            }

                            if (canSubmitReview) {
                                Button(
                                    onClick = { onSubmitForReview(q) },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Submit for Review")
                                }
                            }

                            if (canPublishToggle && !q.isPublished) {
                                Button(
                                    onClick = { onApproveOrReject(q, "approved", true, "Approved and published") },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (session.isMainAdmin) "Approve & Publish" else "Publish")
                                }
                            }

                            if (canPublishToggle && q.isPublished) {
                                OutlinedButton(
                                    onClick = { onApproveOrReject(q, "draft", false, "Unpublished back to draft") },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Unpublished, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Unpublish")
                                }
                            }

                            if (session.isMainAdmin && q.effectiveReviewStatus != "rejected") {
                                OutlinedButton(
                                    onClick = {
                                        rejectTarget = q
                                        rejectNotes = q.reviewNotes
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reject")
                                }
                            }

                            if (canDelete) {
                                OutlinedButton(
                                    onClick = { deleteConfirmTarget = q },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = scheme.error, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete", color = scheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (rejectTarget != null) {
        val target = rejectTarget!!
        AlertDialog(
            onDismissRequest = { rejectTarget = null },
            title = { Text("Reject Question") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Rejected questions are unpublished and never appear in student quizzes.")
                    OutlinedTextField(
                        value = rejectNotes,
                        onValueChange = { rejectNotes = it.take(400) },
                        label = { Text("Reason / Feedback for Question Manager") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onApproveOrReject(target, "rejected", false, rejectNotes.ifBlank { "Needs revision" })
                    rejectTarget = null
                }) {
                    Text("Confirm Reject")
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectTarget = null }) { Text("Cancel") }
            }
        )
    }

    if (deleteConfirmTarget != null) {
        val target = deleteConfirmTarget!!
        AlertDialog(
            onDismissRequest = { deleteConfirmTarget = null },
            title = { Text("Delete Question Permanently?") },
            text = { Text(target.questionText.take(200)) },
            confirmButton = {
                Button(onClick = {
                    onDeleteQuestion(target)
                    deleteConfirmTarget = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmTarget = null }) { Text("Cancel") }
            }
        )
    }
}
