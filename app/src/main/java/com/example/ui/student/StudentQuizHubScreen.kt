package com.example.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CurriculumCatalog
import com.example.data.GameCatalogItem
import com.example.data.model.QuestionItem
import com.example.data.model.QuizResultRecord
import com.example.data.model.UserProfile
import com.example.ui.games.BrainGamesData

private fun iconForQuizCategory(category: String): ImageVector = when (category) {
    "General Knowledge" -> Icons.Default.Public
    "Mathematics" -> Icons.Default.Calculate
    "Science" -> Icons.Default.Science
    "Reasoning" -> Icons.Default.Extension
    "English" -> Icons.Default.Language
    "Hindi" -> Icons.Default.Translate
    "Social Science" -> Icons.Default.School
    "Computer" -> Icons.Default.Computer
    "IQ Test" -> Icons.Default.Lightbulb
    "Memory Test" -> Icons.Default.AutoAwesomeMosaic
    else -> Icons.AutoMirrored.Filled.MenuBook
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudentQuizHubScreen(
    profile: UserProfile,
    publishedQuestions: List<QuestionItem>,
    recentResults: List<QuizResultRecord>,
    onStartCategoryOrSubjectQuiz: (categoryOrSubject: String, studentClass: Int, language: String, isGk: Boolean) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var selectedClass by remember(profile.studentClass) { mutableIntStateOf(profile.studentClass.toInt().coerceIn(1, 12)) }
    var selectedLang by remember(profile.language) { mutableStateOf(profile.language) }
    val isHi = selectedLang == "hi"
    val classSubjects = CurriculumCatalog.subjectsForClass(selectedClass)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("student_quiz_hub_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Class 1 to 12 & Language Filter Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
                    .border(1.dp, scheme.outline, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHi) "कक्षा (1 से 12) और भाषा" else "Class (1 to 12) & Language",
                            style = MaterialTheme.typography.titleMedium,
                            color = scheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = selectedLang == "en",
                                onClick = { selectedLang = "en" },
                                label = { Text("English") },
                                modifier = Modifier.testTag("quiz_hub_lang_en")
                            )
                            FilterChip(
                                selected = selectedLang == "hi",
                                onClick = { selectedLang = "hi" },
                                label = { Text("हिन्दी") },
                                modifier = Modifier.testTag("quiz_hub_lang_hi")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (1..12).forEach { cls ->
                            FilterChip(
                                selected = selectedClass == cls,
                                onClick = { selectedClass = cls },
                                label = { Text("Class $cls") }
                            )
                        }
                    }

                    if (publishedQuestions.isEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = scheme.primaryContainer.copy(alpha = 0.65f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "NOTE: Live Firestore Question Bank currently has 0 published items. Starting a category launches clearly labeled SAMPLE PREVIEW questions for testing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.secondary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section A: All 10 Core Quiz Categories
        item {
            Text(
                text = if (isHi) "10 मुख्य क्विज़ श्रेणियाँ (Core Quiz Categories)" else "10 Core Quiz Categories",
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onBackground,
                modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp)
            )
        }

        items(CurriculumCatalog.coreQuizCategories, key = { "cat_$it" }) { category ->
            val isGk = category == "General Knowledge"
            val liveCount = publishedQuestions.count { q ->
                if (isGk) q.studentClass == 0L || q.subject.equals("General Knowledge", ignoreCase = true)
                else q.subject.equals(category, ignoreCase = true) ||
                    (category == "Computer" && q.subject.contains("Computer", ignoreCase = true))
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
                    .border(
                        width = if (isGk) 1.5.dp else 1.dp,
                        color = if (isGk) scheme.tertiary else scheme.outline.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(18.dp)
                    ),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(scheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconForQuizCategory(category),
                            contentDescription = category,
                            tint = scheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = CurriculumCatalog.subjectLabel(category, selectedLang),
                            style = MaterialTheme.typography.titleMedium,
                            color = scheme.onSurface
                        )
                        Text(
                            text = if (liveCount > 0) "$liveCount Live Firestore Questions"
                            else "Sample Preview Mode Ready",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (liveCount > 0) scheme.tertiary else scheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            onStartCategoryOrSubjectQuiz(category, selectedClass, selectedLang, isGk)
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("start_category_${category.lowercase().replace(" ", "_")}"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isHi) "शुरू करें" else "Start")
                    }
                }
            }
        }

        // Section B: Class-Wise School Curriculum Subjects (Classes 1 to 12)
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isHi) "कक्षा $selectedClass के पाठ्यक्रम विषय" else "Class $selectedClass School Subjects",
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onBackground,
                modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp)
            )
        }

        items(classSubjects, key = { "cls_${selectedClass}_$it" }) { subject ->
            val liveCount = publishedQuestions.count {
                it.studentClass.toInt() == selectedClass && it.subject.equals(subject, ignoreCase = true)
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
                    .border(1.dp, scheme.outline.copy(alpha = 0.55f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Class $selectedClass • ${CurriculumCatalog.subjectLabel(subject, selectedLang)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = scheme.onSurface
                        )
                        Text(
                            text = if (liveCount > 0) "$liveCount Live Questions" else "Sample Preview Questions",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (liveCount > 0) scheme.tertiary else scheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            onStartCategoryOrSubjectQuiz(
                                subject,
                                selectedClass,
                                selectedLang,
                                subject == "General Knowledge"
                            )
                        },
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isHi) "अभ्यास" else "Practice")
                    }
                }
            }
        }

        // Recent Results Summary
        if (recentResults.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isHi) "हालिया परिणाम" else "Recent Quiz & Game Scores",
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onBackground,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp)
                )
            }
            items(recentResults.take(6), key = { it.id }) { res ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = scheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(res.title, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                            Text(
                                text = "Score: ${res.score} • Correct: ${res.correctAnswers}/${res.totalQuestions} (${res.accuracyPercent}%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "+${res.xpEarned} XP",
                            style = MaterialTheme.typography.labelLarge,
                            color = scheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudentGamesHubScreen(
    profile: UserProfile,
    onOpenPlayableGame: (GameCatalogItem) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val isHi = profile.language == "hi"
    val categories = listOf("All", "Arithmetic", "Logic", "Memory", "Reflex", "Focus", "Verbal")
    var selectedCat by remember { mutableStateOf("All") }

    val gamesToShow = remember(selectedCat) {
        if (selectedCat == "All") CurriculumCatalog.games
        else CurriculumCatalog.games.filter { it.category.equals(selectedCat, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("student_games_hub_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = scheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHi) "2D ब्रेन-ट्रेनिंग गेम्स" else "Lightweight 2D Brain Games",
                        style = MaterialTheme.typography.headlineSmall,
                        color = scheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isHi) "प्रत्येक गेम की अलग प्लेएबल स्क्रीन है और XP लाइव सेव होता है।"
                    else "Every game opens in a dedicated playable 2D arena and awards live XP & Points.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCat == cat,
                            onClick = { selectedCat = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }
        }

        items(gamesToShow, key = { it.id }) { game ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
                    .border(1.dp, scheme.outline.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
                    .clickable { onOpenPlayableGame(game) }
                    .testTag("open_game_${game.id}"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(scheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = BrainGamesData.iconForGame(game.id),
                            contentDescription = game.titleEn,
                            tint = scheme.secondary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHi) "${game.titleHi} (${game.titleEn})" else game.titleEn,
                            style = MaterialTheme.typography.titleMedium,
                            color = scheme.onSurface
                        )
                        Text(
                            text = if (isHi) game.subtitleHi else game.subtitleEn,
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { onOpenPlayableGame(game) },
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isHi) "खेलें" else "Play")
                    }
                }
            }
        }
    }
}
