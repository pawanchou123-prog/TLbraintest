package com.example.ui.questionstudio

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Unpublished
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.CurriculumCatalog
import com.example.data.model.QuestionItem
import com.example.ui.PortalViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuestionStudioScreen(
    viewModel: PortalViewModel,
    questions: List<QuestionItem>
) {
    var activeTab by remember { mutableIntStateOf(0) }

    // Form state (supports both Add and Edit)
    var editingId by remember { mutableStateOf<String?>(null) }
    var selectedClass by remember { mutableIntStateOf(10) } // 0 = GK
    var subject by remember { mutableStateOf("Mathematics") }
    var language by remember { mutableStateOf("en") }
    var questionText by remember { mutableStateOf("") }
    var optA by remember { mutableStateOf("") }
    var optB by remember { mutableStateOf("") }
    var optC by remember { mutableStateOf("") }
    var optD by remember { mutableStateOf("") }
    var correctIdx by remember { mutableIntStateOf(0) }
    var explanation by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("medium") }
    var pointsText by remember { mutableStateOf("10") }
    var timerText by remember { mutableStateOf("30") }
    var imageUrl by remember { mutableStateOf("") }
    var publishImmediately by remember { mutableStateOf(true) }

    fun resetForm() {
        editingId = null
        questionText = ""
        optA = ""
        optB = ""
        optC = ""
        optD = ""
        correctIdx = 0
        explanation = ""
        difficulty = "medium"
        pointsText = "10"
        timerText = "30"
        imageUrl = ""
        publishImmediately = true
    }

    fun loadForEdit(q: QuestionItem) {
        editingId = q.id
        selectedClass = q.studentClass.toInt()
        subject = q.subject
        language = q.language
        questionText = q.questionText
        optA = q.options.getOrElse(0) { "" }
        optB = q.options.getOrElse(1) { "" }
        optC = q.options.getOrElse(2) { "" }
        optD = q.options.getOrElse(3) { "" }
        correctIdx = q.correctIndex.toInt().coerceIn(0, 3)
        explanation = q.explanation
        difficulty = q.difficulty
        pointsText = q.points.toString()
        timerText = q.timerSeconds.toString()
        imageUrl = q.imageUrl
        publishImmediately = q.isPublished
        activeTab = 0
    }

    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = scheme.surface,
            contentColor = scheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .clip(RoundedCornerShape(14.dp))
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text(if (editingId == null) "Entry Form" else "Edit Question") },
                icon = { Icon(Icons.Default.AddCircle, contentDescription = null) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Bank (${questions.size})") },
                icon = { Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = null) }
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                text = { Text("Statistics") },
                icon = { Icon(Icons.Default.Analytics, contentDescription = null) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (activeTab) {
            0 -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, scheme.outline, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = scheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (editingId == null) "Standalone Question Entry Module" else "Editing Question",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = scheme.onSurface
                                )
                                if (editingId == null && questions.isEmpty()) {
                                    OutlinedButton(
                                        onClick = { viewModel.seedStarterQuestions() },
                                        modifier = Modifier.testTag("seed_starter_questions_button")
                                    ) {
                                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Load Starter Pack")
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Select Class (GK or Class 1 to 12):", style = MaterialTheme.typography.labelLarge, color = scheme.secondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = selectedClass == 0,
                                    onClick = {
                                        selectedClass = 0
                                        subject = "General Knowledge"
                                    },
                                    label = { Text("GK (All)") }
                                )
                                (1..12).forEach { cls ->
                                    FilterChip(
                                        selected = selectedClass == cls,
                                        onClick = {
                                            selectedClass = cls
                                            val validSubs = CurriculumCatalog.subjectsForClass(cls)
                                            if (subject !in validSubs) subject = validSubs.first()
                                        },
                                        label = { Text("C$cls") }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Select Subject:", style = MaterialTheme.typography.labelLarge, color = scheme.secondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            val availableSubjects = if (selectedClass == 0) {
                                listOf("General Knowledge", "Current Affairs", "Indian History", "World Geography", "Science GK")
                            } else {
                                CurriculumCatalog.subjectsForClass(selectedClass)
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                availableSubjects.forEach { sub ->
                                    FilterChip(
                                        selected = subject == sub,
                                        onClick = { subject = sub },
                                        label = { Text(sub) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Language:", style = MaterialTheme.typography.labelLarge, color = scheme.secondary)
                                FilterChip(selected = language == "en", onClick = { language = "en" }, label = { Text("English") })
                                FilterChip(selected = language == "hi", onClick = { language = "hi" }, label = { Text("हिन्दी (Hindi)") })
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = questionText,
                                onValueChange = { questionText = it },
                                label = { Text("Question Prompt") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("studio_question_input"),
                                minLines = 2
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = imageUrl,
                                onValueChange = { imageUrl = it },
                                label = { Text("Optional HTTPS Image URL (https://...)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            if (imageUrl.startsWith("https://")) {
                                Spacer(modifier = Modifier.height(8.dp))
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Question image preview",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Four Answer Options (Tap radio to mark Correct Answer):", style = MaterialTheme.typography.labelLarge, color = scheme.secondary)
                            Spacer(modifier = Modifier.height(6.dp))

                            listOf(
                                Triple(0, "Option A", optA to { v: String -> optA = v }),
                                Triple(1, "Option B", optB to { v: String -> optB = v }),
                                Triple(2, "Option C", optC to { v: String -> optC = v }),
                                Triple(3, "Option D", optD to { v: String -> optD = v })
                            ).forEach { (idx, label, pair) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = correctIdx == idx,
                                        onClick = { correctIdx = idx }
                                    )
                                    OutlinedTextField(
                                        value = pair.first,
                                        onValueChange = pair.second,
                                        label = { Text(label) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(vertical = 4.dp)
                                            .testTag("studio_option_$idx"),
                                        singleLine = true
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = explanation,
                                onValueChange = { explanation = it },
                                label = { Text("Answer Explanation") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Difficulty:", style = MaterialTheme.typography.labelLarge, color = scheme.secondary)
                                listOf("easy", "medium", "hard").forEach { diff ->
                                    FilterChip(
                                        selected = difficulty == diff,
                                        onClick = { difficulty = diff },
                                        label = { Text(diff.replaceFirstChar { it.uppercase() }) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = pointsText,
                                    onValueChange = { pointsText = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Points") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = timerText,
                                    onValueChange = { timerText = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Timer (sec)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (publishImmediately) "Status: Publish Live Immediately" else "Status: Save as Unpublished Draft",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (publishImmediately) scheme.tertiary else scheme.secondary
                                )
                                Switch(
                                    checked = publishImmediately,
                                    onCheckedChange = { publishImmediately = it }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.saveQuestionEntry(
                                            existingId = editingId,
                                            studentClass = selectedClass,
                                            subject = subject,
                                            language = language,
                                            questionText = questionText,
                                            options = listOf(optA, optB, optC, optD),
                                            correctIndex = correctIdx,
                                            explanation = explanation,
                                            difficulty = difficulty,
                                            points = pointsText.toIntOrNull() ?: 10,
                                            timerSeconds = timerText.toIntOrNull() ?: 30,
                                            imageUrl = imageUrl,
                                            isPublished = publishImmediately,
                                            onSaved = { resetForm() }
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("studio_save_question_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (editingId == null) "Save Question" else "Update Question")
                                }
                                if (editingId != null) {
                                    OutlinedButton(
                                        onClick = { resetForm() },
                                        modifier = Modifier.height(50.dp),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text("Cancel Edit")
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            1 -> {
                var filterMode by remember { mutableStateOf("ALL") } // ALL | PUBLISHED | DRAFTS
                var searchQuery by remember { mutableStateOf("") }

                val filtered = remember(questions, filterMode, searchQuery) {
                    questions.filter { q ->
                        val matchesMode = when (filterMode) {
                            "PUBLISHED" -> q.isPublished
                            "DRAFTS" -> !q.isPublished
                            else -> true
                        }
                        val matchesSearch = searchQuery.isBlank() ||
                            q.questionText.contains(searchQuery, ignoreCase = true) ||
                            q.subject.contains(searchQuery, ignoreCase = true)
                        matchesMode && matchesSearch
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 760.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search question text or subject") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = filterMode == "ALL", onClick = { filterMode = "ALL" }, label = { Text("All (${questions.size})") })
                        FilterChip(
                            selected = filterMode == "PUBLISHED",
                            onClick = { filterMode = "PUBLISHED" },
                            label = { Text("Published (${questions.count { it.isPublished }})") }
                        )
                        FilterChip(
                            selected = filterMode == "DRAFTS",
                            onClick = { filterMode = "DRAFTS" },
                            label = { Text("Drafts / AI Queue (${questions.count { !it.isPublished }})") }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filtered, key = { it.id }) { q ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (q.isPublished) scheme.tertiary.copy(alpha = 0.6f) else scheme.outline,
                                        RoundedCornerShape(16.dp)
                                    ),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = scheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val clsTag = if (q.studentClass == 0L) "GK" else "Class ${q.studentClass}"
                                        Text(
                                            text = "$clsTag • ${q.subject} • ${q.language.uppercase()} • ${q.difficulty.uppercase()}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = scheme.secondary
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (q.isPublished) scheme.tertiaryContainer else scheme.primaryContainer
                                        ) {
                                            Text(
                                                text = if (q.isPublished) "LIVE" else if (q.source == "gemini_ai") "AI DRAFT" else "DRAFT",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (q.isPublished) scheme.onTertiaryContainer else scheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = q.questionText,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = scheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    q.options.forEachIndexed { idx, opt ->
                                        Text(
                                            text = "${listOf("A", "B", "C", "D").getOrElse(idx) { "" }}) $opt",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (idx == q.correctIndex.toInt()) scheme.tertiary else scheme.onSurfaceVariant,
                                            fontWeight = if (idx == q.correctIndex.toInt()) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${q.points} pts • ${q.timerSeconds}s • Attempts: ${q.attemptsCount}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = scheme.onSurfaceVariant
                                        )
                                        Row {
                                            IconButton(onClick = { viewModel.togglePublishQuestion(q.id, !q.isPublished) }) {
                                                Icon(
                                                    imageVector = if (q.isPublished) Icons.Default.Unpublished else Icons.Default.Publish,
                                                    contentDescription = if (q.isPublished) "Unpublish" else "Publish",
                                                    tint = if (q.isPublished) scheme.secondary else scheme.tertiary
                                                )
                                            }
                                            IconButton(onClick = { loadForEdit(q) }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit Question", tint = scheme.primary)
                                            }
                                            IconButton(onClick = { viewModel.deleteQuestionItem(q.id) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete Question", tint = scheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                val totalAttempts = questions.sumOf { it.attemptsCount }
                val totalCorrect = questions.sumOf { it.correctCount }
                val overallAcc = if (totalAttempts > 0L) ((totalCorrect * 100L) / totalAttempts).toInt() else 0

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 760.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, scheme.outline, RoundedCornerShape(18.dp)),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = scheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("TOTAL BANK", style = MaterialTheme.typography.labelSmall, color = scheme.secondary)
                                    Text("${questions.size}", style = MaterialTheme.typography.headlineSmall, color = scheme.onSurface)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("ATTEMPTS", style = MaterialTheme.typography.labelSmall, color = scheme.secondary)
                                    Text("$totalAttempts", style = MaterialTheme.typography.headlineSmall, color = scheme.onSurface)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("AVG ACCURACY", style = MaterialTheme.typography.labelSmall, color = scheme.secondary)
                                    Text("$overallAcc%", style = MaterialTheme.typography.headlineSmall, color = scheme.tertiary)
                                }
                            }
                        }
                    }

                    items(questions.sortedByDescending { it.attemptsCount }, key = { it.id }) { q ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = scheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, scheme.outlineVariant, RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(q.questionText, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface, maxLines = 2)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Attempts: ${q.attemptsCount}   |   Correct Rate: ${q.accuracyRatePercent}%   |   Avg Time: ${q.avgResponseSeconds}s",
                                    style = MaterialTheme.typography.bodySmall,
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
