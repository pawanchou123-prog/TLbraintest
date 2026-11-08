package com.example.ui.student

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.audio.SoundEffect
import com.example.core.audio.SoundManager
import com.example.data.model.QuestionItem
import kotlinx.coroutines.delay

@Composable
fun QuizEngineScreen(
    quizTitle: String,
    isGkMode: Boolean,
    studentClass: Int,
    questions: List<QuestionItem>,
    isSamplePreviewData: Boolean = false,
    onCompleteQuiz: (
        activityType: String,
        title: String,
        studentClass: Int,
        score: Int,
        totalQuestions: Int,
        correctAnswers: Int,
        xpEarned: Int,
        durationSeconds: Int,
        attempts: List<Pair<QuestionItem, Pair<Boolean, Int>>>
    ) -> Unit,
    onExit: () -> Unit
) {
    BackHandler(onBack = onExit)

    val scheme = MaterialTheme.colorScheme

    if (questions.isEmpty()) {
        Surface(modifier = Modifier.fillMaxSize(), color = scheme.background) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 500.dp)
                        .border(1.dp, scheme.outline, RoundedCornerShape(22.dp)),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "No questions yet",
                            tint = scheme.secondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Published Questions Yet",
                            style = MaterialTheme.typography.headlineSmall,
                            color = scheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No live questions are currently published for $quizTitle. Ask the Administrator or open Question Studio to publish questions for this subject.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onExit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("empty_quiz_back_button")
                        ) {
                            Text("Back to Subjects")
                        }
                    }
                }
            }
        }
        return
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var totalScore by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var isCompleted by remember { mutableStateOf(false) }
    val startMs = remember { System.currentTimeMillis() }
    val attempts = remember { mutableStateListOf<Pair<QuestionItem, Pair<Boolean, Int>>>() }

    val currentQuestion = questions.getOrNull(currentIndex)
    val initialSeconds = (currentQuestion?.timerSeconds?.toInt() ?: 30).coerceIn(5, 300)
    var secondsLeft by remember(currentIndex) { mutableIntStateOf(initialSeconds) }

    LaunchedEffect(Unit) {
        SoundManager.play(SoundEffect.QUIZ_START)
    }

    LaunchedEffect(currentIndex) {
        if (currentIndex > 0) {
            SoundManager.play(SoundEffect.QUESTION_APPEAR)
        }
    }

    LaunchedEffect(currentIndex, selectedOption, isCompleted) {
        if (!isCompleted && selectedOption == null && currentQuestion != null) {
            while (secondsLeft > 0 && selectedOption == null) {
                delay(1000L)
                secondsLeft--
                if (secondsLeft in 1..5 && selectedOption == null) {
                    SoundManager.play(SoundEffect.TIMER_WARN)
                }
            }
            if (secondsLeft <= 0 && selectedOption == null) {
                SoundManager.play(SoundEffect.TIMER_END)
                selectedOption = -1 // Time expired
                attempts.add(currentQuestion to (false to initialSeconds))
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = scheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onExit, modifier = Modifier.testTag("quiz_exit_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit Quiz", tint = scheme.onBackground)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(quizTitle, style = MaterialTheme.typography.titleLarge, color = scheme.onBackground)
                    if (!isCompleted) {
                        Text(
                            text = "Question ${currentIndex + 1} of ${questions.size}  •  Score: $totalScore",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.secondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isCompleted) {
                val accuracy = ((correctCount.toDouble() / questions.size.toDouble()) * 100.0).toInt()
                val xp = (totalScore + correctCount * 5).coerceAtLeast(10)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 540.dp)
                        .border(1.5.dp, scheme.outline, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Quiz Completed",
                            tint = scheme.tertiary,
                            modifier = Modifier.size(58.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Quiz Results", style = MaterialTheme.typography.headlineMedium, color = scheme.onSurface)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Correct: $correctCount / ${questions.size}   |   Accuracy: $accuracy%",
                            style = MaterialTheme.typography.titleMedium,
                            color = scheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Total Points: $totalScore   |   XP Earned: +$xp XP",
                            style = MaterialTheme.typography.titleMedium,
                            color = scheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(22.dp))
                        Button(
                            onClick = {
                                SoundManager.play(SoundEffect.SUCCESS)
                                onExit()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("quiz_finish_done_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save & Return")
                        }
                    }
                }
            } else if (currentQuestion != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (isSamplePreviewData) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = scheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        ) {
                            Text(
                                text = "SAMPLE PREVIEW QUESTIONS — Live Firestore Bank Empty for This Category",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.secondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Quiz Progress Indicator
                    LinearProgressIndicator(
                        progress = { (currentIndex + 1).toFloat() / questions.size.coerceAtLeast(1).toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = scheme.secondary,
                        trackColor = scheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = scheme.primaryContainer
                        ) {
                            Text(
                                text = "${currentQuestion.difficulty.uppercase()} • +${currentQuestion.points} PTS",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = if (secondsLeft <= 7) scheme.error else scheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${secondsLeft}s",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (secondsLeft <= 7) scheme.error else scheme.tertiary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { secondsLeft.toFloat() / initialSeconds.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (secondsLeft <= 7) scheme.error else scheme.primary,
                        trackColor = scheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, scheme.outline, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = scheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            if (currentQuestion.imageUrl.startsWith("https://")) {
                                AsyncImage(
                                    model = currentQuestion.imageUrl,
                                    contentDescription = "Question illustration",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(170.dp)
                                        .clip(RoundedCornerShape(14.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            Text(
                                text = currentQuestion.questionText,
                                style = MaterialTheme.typography.titleLarge,
                                color = scheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val optionPrefixes = listOf("A", "B", "C", "D")
                    currentQuestion.options.take(4).forEachIndexed { idx, optText ->
                        val answered = selectedOption != null
                        val isCorrect = idx == currentQuestion.correctIndex.toInt()
                        val isPicked = selectedOption == idx

                        val bgColor = when {
                            !answered -> scheme.surfaceVariant
                            isCorrect -> scheme.tertiaryContainer
                            isPicked && !isCorrect -> scheme.errorContainer
                            else -> scheme.surface
                        }
                        val borderColor = when {
                            !answered -> scheme.outline.copy(alpha = 0.6f)
                            isCorrect -> scheme.tertiary
                            isPicked && !isCorrect -> scheme.error
                            else -> scheme.outlineVariant
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
                                .clickable(enabled = !answered) {
                                    selectedOption = idx
                                    val timeSpent = (initialSeconds - secondsLeft).coerceAtLeast(1)
                                    val right = idx == currentQuestion.correctIndex.toInt()
                                    if (right) {
                                        correctCount++
                                        totalScore += currentQuestion.points.toInt()
                                        SoundManager.play(SoundEffect.CORRECT)
                                    } else {
                                        SoundManager.play(SoundEffect.WRONG)
                                    }
                                    attempts.add(currentQuestion to (right to timeSpent))
                                }
                                .testTag("quiz_option_$idx"),
                            color = bgColor
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${optionPrefixes.getOrElse(idx) { "" }}.",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = scheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = optText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = scheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    if (selectedOption != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        if (currentQuestion.explanation.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = scheme.secondaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = if (selectedOption == -1) "Time Expired — Explanation:" else "Explanation:",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = scheme.secondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = currentQuestion.explanation,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = scheme.onSecondaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        Button(
                            onClick = {
                                if (currentIndex + 1 >= questions.size) {
                                    val durationSec = ((System.currentTimeMillis() - startMs) / 1000L).toInt().coerceAtLeast(1)
                                    val xp = (totalScore + correctCount * 5).coerceAtLeast(10)
                                    onCompleteQuiz(
                                        if (isGkMode) "gk" else "quiz",
                                        quizTitle,
                                        studentClass,
                                        totalScore,
                                        questions.size,
                                        correctCount,
                                        xp,
                                        durationSec,
                                        attempts.toList()
                                    )
                                    val accuracy = ((correctCount.toDouble() / questions.size.toDouble()) * 100.0).toInt()
                                    SoundManager.play(SoundEffect.QUIZ_COMPLETE)
                                    if (accuracy >= 80) {
                                        SoundManager.play(SoundEffect.HIGH_SCORE)
                                    } else {
                                        SoundManager.play(SoundEffect.XP_EARNED)
                                    }
                                    isCompleted = true
                                } else {
                                    SoundManager.play(SoundEffect.NEXT_QUESTION)
                                    selectedOption = null
                                    currentIndex++
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("quiz_next_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(if (currentIndex + 1 >= questions.size) "View Final Score" else "Next Question")
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
