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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Save
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
import com.example.core.audio.SoundEffect
import com.example.core.audio.SoundManager
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.CurriculumCatalog
import com.example.data.model.QuestionItem
import com.example.data.model.SubjectCatalogDoc
import com.example.data.repository.QuestionManagerOperatorSession

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuestionEditorScreen(
    session: QuestionManagerOperatorSession,
    subjectCatalog: SubjectCatalogDoc,
    editingQuestion: QuestionItem?,
    onClearEditing: () -> Unit,
    onSaveQuestion: (
        studentClass: Int,
        subject: String,
        topic: String,
        language: String,
        questionText: String,
        options: List<String>,
        correctIndex: Int,
        explanation: String,
        difficulty: String,
        points: Int,
        timerSeconds: Int,
        imageUrl: String,
        desiredAction: String
    ) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val canDirectPublish = session.isMainAdmin || session.canPublishDirectly

    var studentClass by remember { mutableIntStateOf(10) }
    var subject by remember { mutableStateOf("Mathematics") }
    var topic by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("en") }
    var questionText by remember { mutableStateOf("") }
    var optionA by remember { mutableStateOf("") }
    var optionB by remember { mutableStateOf("") }
    var optionC by remember { mutableStateOf("") }
    var optionD by remember { mutableStateOf("") }
    var correctIndex by remember { mutableIntStateOf(0) }
    var explanation by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("medium") }
    var pointsText by remember { mutableStateOf("10") }
    var timerSecondsText by remember { mutableStateOf("30") }
    var imageUrl by remember { mutableStateOf("") }
    var localValidationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(editingQuestion?.id) {
        if (editingQuestion != null) {
            studentClass = editingQuestion.studentClass.toInt().coerceIn(0, 12)
            subject = editingQuestion.subject
            topic = editingQuestion.topic
            language = editingQuestion.language
            questionText = editingQuestion.questionText
            optionA = editingQuestion.options.getOrElse(0) { "" }
            optionB = editingQuestion.options.getOrElse(1) { "" }
            optionC = editingQuestion.options.getOrElse(2) { "" }
            optionD = editingQuestion.options.getOrElse(3) { "" }
            correctIndex = editingQuestion.correctIndex.toInt().coerceIn(0, 3)
            explanation = editingQuestion.explanation
            difficulty = editingQuestion.difficulty
            pointsText = editingQuestion.points.toString()
            timerSecondsText = editingQuestion.timerSeconds.toString()
            imageUrl = editingQuestion.imageUrl
            localValidationError = null
        }
    }

    val availableSubjects = CurriculumCatalog.questionManagerSubjectsForClass(studentClass, subjectCatalog)
    LaunchedEffect(studentClass, availableSubjects) {
        if (editingQuestion == null && subject !in availableSubjects && availableSubjects.isNotEmpty()) {
            subject = availableSubjects.first()
        }
    }

    fun validateAndSubmit(action: String) {
        val pts = pointsText.trim().toIntOrNull()
        val tmr = timerSecondsText.trim().toIntOrNull()
        val opts = listOf(optionA.trim(), optionB.trim(), optionC.trim(), optionD.trim())
        val err = when {
            subject.trim().isEmpty() -> "Please select or enter a Subject."
            questionText.trim().length < 3 -> "Please enter the Question text (minimum 3 characters)."
            opts.any { it.isEmpty() } -> "All four answer options (A, B, C, and D) are required."
            (action == "publish" || action == "pending_review") && explanation.trim().isEmpty() ->
                "Explanation of the correct answer is required before submitting or publishing."
            pts == null || pts !in 1..1000 -> "Points must be a valid number between 1 and 1000."
            tmr == null || tmr !in 5..300 -> "Time limit must be between 5 and 300 seconds."
            imageUrl.trim().isNotEmpty() && !imageUrl.trim().startsWith("https://") ->
                "Optional image URL must start with https://"
            else -> null
        }
        if (err != null) {
            SoundManager.play(SoundEffect.ERROR)
            localValidationError = err
            return
        }
        localValidationError = null
        when (action) {
            "draft" -> SoundManager.play(SoundEffect.SAVE)
            "publish" -> SoundManager.play(SoundEffect.SUCCESS)
            else -> SoundManager.play(SoundEffect.REQUEST_SUBMITTED)
        }
        onSaveQuestion(
            studentClass,
            subject.trim(),
            topic.trim(),
            language,
            questionText.trim(),
            opts,
            correctIndex,
            explanation.trim(),
            difficulty,
            pts!!,
            tmr!!,
            imageUrl.trim(),
            action
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("qm_editor_screen"),
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (editingQuestion == null) "Add New Question" else "Edit Question",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = scheme.onSurface
                            )
                            Text(
                                text = if (editingQuestion == null) {
                                    "Create a curriculum question for Classes 1 to 12 or General Knowledge."
                                } else {
                                    "Editing ID: ${editingQuestion.id.take(12)}..."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                        if (editingQuestion != null) {
                            OutlinedButton(
                                onClick = {
                                    onClearEditing()
                                    questionText = ""
                                    optionA = ""
                                    optionB = ""
                                    optionC = ""
                                    optionD = ""
                                    explanation = ""
                                    topic = ""
                                    imageUrl = ""
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Blank")
                            }
                        }
                    }

                    // 1. Language Selection (English / Hindi)
                    Text("Language", style = MaterialTheme.typography.labelLarge, color = scheme.secondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = language == "en",
                            onClick = { language = "en" },
                            label = { Text("English") },
                            modifier = Modifier.testTag("qm_editor_lang_en")
                        )
                        FilterChip(
                            selected = language == "hi",
                            onClick = { language = "hi" },
                            label = { Text("Hindi (हिन्दी)") },
                            modifier = Modifier.testTag("qm_editor_lang_hi")
                        )
                    }

                    // 2. Class Selection (1 to 12 + GK)
                    Text("Class (1 to 12 or GK)", style = MaterialTheme.typography.labelLarge, color = scheme.secondary)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (1..12).forEach { cls ->
                            FilterChip(
                                selected = studentClass == cls,
                                onClick = { studentClass = cls },
                                label = { Text("Class $cls") }
                            )
                        }
                        FilterChip(
                            selected = studentClass == 0,
                            onClick = { studentClass = 0 },
                            label = { Text("GK (All)") }
                        )
                    }

                    // 3. Class-Wise Subject Selection
                    Text("Subject (Organized for Selected Class)", style = MaterialTheme.typography.labelLarge, color = scheme.secondary)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableSubjects.forEach { subj ->
                            FilterChip(
                                selected = subject.equals(subj, ignoreCase = true),
                                onClick = { subject = subj },
                                label = { Text(subj) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it.take(60) },
                        label = { Text("Selected / Custom Subject") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qm_editor_subject_input")
                    )

                    // 4. Topic or Chapter
                    OutlinedTextField(
                        value = topic,
                        onValueChange = { topic = it.take(120) },
                        label = { Text("Topic or Chapter (e.g., Fractions, Light Reflection)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qm_editor_topic_input")
                    )

                    // 5. Question Text
                    OutlinedTextField(
                        value = questionText,
                        onValueChange = { questionText = it.take(1000) },
                        label = { Text("Question Text *") },
                        minLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qm_editor_question_input")
                    )

                    // 6. Options A, B, C, D
                    OutlinedTextField(
                        value = optionA,
                        onValueChange = { optionA = it.take(300) },
                        label = { Text("Option A *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("qm_editor_opt_a")
                    )
                    OutlinedTextField(
                        value = optionB,
                        onValueChange = { optionB = it.take(300) },
                        label = { Text("Option B *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("qm_editor_opt_b")
                    )
                    OutlinedTextField(
                        value = optionC,
                        onValueChange = { optionC = it.take(300) },
                        label = { Text("Option C *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("qm_editor_opt_c")
                    )
                    OutlinedTextField(
                        value = optionD,
                        onValueChange = { optionD = it.take(300) },
                        label = { Text("Option D *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("qm_editor_opt_d")
                    )

                    // 7. Correct Answer Selection
                    Text("Correct Answer *", style = MaterialTheme.typography.labelLarge, color = scheme.secondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Option A" to 0, "Option B" to 1, "Option C" to 2, "Option D" to 3).forEach { (lbl, idx) ->
                            FilterChip(
                                selected = correctIndex == idx,
                                onClick = { correctIndex = idx },
                                label = { Text(lbl) },
                                modifier = Modifier.testTag("qm_editor_correct_$idx")
                            )
                        }
                    }

                    // 8. Explanation of Correct Answer
                    OutlinedTextField(
                        value = explanation,
                        onValueChange = { explanation = it.take(1000) },
                        label = { Text("Explanation of the Correct Answer *") },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qm_editor_explanation_input")
                    )

                    // 9. Difficulty, Points & Time Limit
                    Text("Difficulty", style = MaterialTheme.typography.labelLarge, color = scheme.secondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("easy" to "Easy", "medium" to "Medium", "hard" to "Hard").forEach { (key, lbl) ->
                            FilterChip(
                                selected = difficulty == key,
                                onClick = { difficulty = key },
                                label = { Text(lbl) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = pointsText,
                            onValueChange = { pointsText = it.filter { ch -> ch.isDigit() }.take(4) },
                            label = { Text("Points (1-1000)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("qm_editor_points_input")
                        )
                        OutlinedTextField(
                            value = timerSecondsText,
                            onValueChange = { timerSecondsText = it.filter { ch -> ch.isDigit() }.take(3) },
                            label = { Text("Time Limit (sec)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("qm_editor_timer_input")
                        )
                    }

                    // 10. Optional HTTPS Image URL
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it.take(500) },
                        label = { Text("Optional Image URL (https://...)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qm_editor_image_url_input")
                    )

                    if (localValidationError != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = scheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = localValidationError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // 11. Question Status Actions: Save Draft / Submit for Review / Publish
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { validateAndSubmit("draft") },
                            modifier = Modifier.testTag("qm_save_draft_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save as Draft")
                        }

                        if (!session.isMainAdmin) {
                            Button(
                                onClick = { validateAndSubmit("pending_review") },
                                modifier = Modifier.testTag("qm_submit_review_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Submit for Review")
                            }
                        }

                        if (canDirectPublish) {
                            Button(
                                onClick = { validateAndSubmit("publish") },
                                modifier = Modifier.testTag("qm_publish_question_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Publish Question")
                            }
                        }
                    }
                }
            }
        }
    }
}
