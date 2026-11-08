package com.example.ui.games

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.core.audio.SoundEffect
import com.example.core.audio.SoundManager
import com.example.data.GameCatalogItem
import kotlin.random.Random
import kotlinx.coroutines.delay

@Composable
fun BrainGamesArenaScreen(
    game: GameCatalogItem,
    studentClass: Int,
    language: String,
    soundEnabled: Boolean,
    onCompleteGame: (score: Int, totalRounds: Int, correctRounds: Int, xpEarned: Int, durationSec: Int) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var finishedSummary by remember { mutableStateOf<GameFinishData?>(null) }
    val startMs = remember { System.currentTimeMillis() }

    LaunchedEffect(Unit) {
        SoundManager.play(SoundEffect.GAME_START)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
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
                IconButton(
                    onClick = {
                        SoundManager.play(SoundEffect.CLICK)
                        onBack()
                    },
                    modifier = Modifier.testTag("game_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Games",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (language == "hi") game.titleHi else game.titleEn,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (language == "hi") game.subtitleHi else game.subtitleEn,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val summary = finishedSummary
            if (summary != null) {
                GameCompletionSummaryCard(
                    gameTitle = if (language == "hi") game.titleHi else game.titleEn,
                    summary = summary,
                    onPlayAgain = {
                        SoundManager.play(SoundEffect.GAME_START)
                        finishedSummary = null
                    },
                    onExit = {
                        SoundManager.play(SoundEffect.CLICK)
                        onBack()
                    }
                )
            } else {
                val completeCallback: (Int, Int, Int, Int) -> Unit = { score, total, correct, xp ->
                    val elapsedSec = ((System.currentTimeMillis() - startMs) / 1000L).toInt().coerceAtLeast(1)
                    onCompleteGame(score, total, correct, xp, elapsedSec)
                    finishedSummary = GameFinishData(score, total, correct, xp, elapsedSec)
                    SoundManager.play(SoundEffect.GAME_COMPLETED)
                    if (score >= 80) {
                        SoundManager.play(SoundEffect.NEW_RECORD)
                    } else {
                        SoundManager.play(SoundEffect.POINTS_EARNED)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .weight(1f),
                    contentAlignment = Alignment.TopCenter
                ) {
                    when (game.id) {
                        "memory_match" -> MemoryMatch2DGame(soundEnabled, completeCallback)
                        "reaction_test" -> ReactionTest2DGame(soundEnabled, completeCallback)
                        "number_scramble" -> NumberScramble2DGame(soundEnabled, completeCallback)
                        "word_scramble" -> WordScramble2DGame(language, soundEnabled, completeCallback)
                        "finger_speed" -> FingerSpeed2DGame(soundEnabled, completeCallback)
                        else -> SpeedTile2DGame(game.id, studentClass, language, soundEnabled, completeCallback)
                    }
                }
            }
        }
    }
}

private data class GameFinishData(
    val score: Int,
    val totalRounds: Int,
    val correctRounds: Int,
    val xpEarned: Int,
    val elapsedSec: Int
)

@Composable
private fun GameCompletionSummaryCard(
    gameTitle: String,
    summary: GameFinishData,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    val accuracy = if (summary.totalRounds > 0) (summary.correctRounds * 100) / summary.totalRounds else 100
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "Challenge Complete",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "$gameTitle Complete",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricPill("SCORE", "${summary.score}")
                MetricPill("ACCURACY", "$accuracy%")
                MetricPill("XP EARNED", "+${summary.xpEarned}")
            }
            Spacer(modifier = Modifier.height(22.dp))
            Button(
                onClick = onPlayAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("game_play_again_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play Again")
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onExit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("game_return_hub_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Return to Games Hub")
            }
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun SpeedTile2DGame(
    gameId: String,
    studentClass: Int,
    language: String,
    soundEnabled: Boolean,
    onFinish: (score: Int, total: Int, correct: Int, xp: Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val rounds = remember(gameId, studentClass, language) {
        BrainGamesData.generateSpeedRounds(gameId, studentClass, language)
    }
    var index by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var timeRemaining by remember { mutableIntStateOf(60) }

    LaunchedEffect(Unit) {
        while (timeRemaining > 0 && index < rounds.size) {
            delay(1000L)
            timeRemaining--
        }
        if (timeRemaining <= 0 && index < rounds.size) {
            val xp = (correctCount * 6).coerceAtLeast(5)
            onFinish(correctCount * 10, rounds.size, correctCount, xp)
        }
    }

    val current = rounds.getOrNull(index) ?: return
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Round ${index + 1} / ${rounds.size}", style = MaterialTheme.typography.labelLarge, color = scheme.secondary)
            Text("Timer: ${timeRemaining}s", style = MaterialTheme.typography.labelLarge, color = scheme.tertiary)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (index + 1).toFloat() / rounds.size.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = scheme.primary,
            trackColor = scheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(18.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, scheme.outline, RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = scheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (current.helperSubtitle.isNotBlank()) {
                    Text(
                        text = current.helperSubtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.secondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                if (current.swatchIndex >= 0) {
                    val swatchColor = when (current.swatchIndex) {
                        0 -> scheme.primary
                        1 -> scheme.tertiary
                        2 -> scheme.error
                        else -> scheme.onBackground
                    }
                    Canvas(
                        modifier = Modifier
                            .size(92.dp)
                            .border(2.dp, scheme.outline, CircleShape)
                    ) {
                        drawCircle(color = swatchColor, radius = size.minDimension / 2.2f)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
                Text(
                    text = current.prompt,
                    style = MaterialTheme.typography.headlineMedium,
                    color = scheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        current.options.forEachIndexed { optIdx, label ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, scheme.outline.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .clickable {
                        if (soundEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        val right = optIdx == current.correctIndex
                        if (right) {
                            SoundManager.play(SoundEffect.GAME_CORRECT_MOVE)
                        } else {
                            SoundManager.play(SoundEffect.GAME_WRONG_MOVE)
                        }
                        val nextCorrect = if (right) correctCount + 1 else correctCount
                        correctCount = nextCorrect
                        if (index + 1 >= rounds.size) {
                            val xp = (nextCorrect * 6).coerceAtLeast(8)
                            onFinish(nextCorrect * 10, rounds.size, nextCorrect, xp)
                        } else {
                            index++
                        }
                    }
                    .testTag("speed_game_option_$optIdx"),
                color = scheme.surfaceVariant
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoryMatch2DGame(
    soundEnabled: Boolean,
    onFinish: (score: Int, total: Int, correct: Int, xp: Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val icons = remember {
        listOf(
            Icons.Default.Star,
            Icons.Default.Bolt,
            Icons.Default.Diamond,
            Icons.Default.RocketLaunch,
            Icons.Default.Shield,
            Icons.Default.Hexagon
        )
    }
    val deck = remember { (icons.indices + icons.indices).shuffled() }
    val matched = remember { mutableStateListOf<Int>() }
    val flipped = remember { mutableStateListOf<Int>() }
    var moves by remember { mutableIntStateOf(0) }

    LaunchedEffect(flipped.size) {
        if (flipped.size == 2) {
            moves++
            val a = flipped[0]
            val b = flipped[1]
            if (deck[a] == deck[b]) {
                matched.add(a)
                matched.add(b)
                SoundManager.play(SoundEffect.MATCH_FOUND)
                flipped.clear()
                if (matched.size == deck.size) {
                    val bonusScore = (120 - moves * 4).coerceAtLeast(40)
                    SoundManager.play(SoundEffect.MATCH_COMPLETED)
                    onFinish(bonusScore, 6, 6, 35)
                }
            } else {
                SoundManager.play(SoundEffect.GAME_WRONG_MOVE)
                delay(650L)
                flipped.clear()
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Matched Pairs: ${matched.size / 2} / 6   |   Flips: $moves",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        for (row in 0 until 3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (col in 0 until 4) {
                    val idx = row * 4 + col
                    val isOpen = idx in matched || idx in flipped
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.5.dp,
                                if (idx in matched) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable(enabled = !isOpen && flipped.size < 2) {
                                if (soundEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                SoundManager.play(SoundEffect.SELECT)
                                flipped.add(idx)
                            }
                            .testTag("memory_card_$idx"),
                        color = if (isOpen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isOpen) {
                                Icon(
                                    imageVector = icons[deck[idx]],
                                    contentDescription = "Memory symbol",
                                    tint = if (idx in matched) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(34.dp)
                                )
                            } else {
                                Text("?", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ReactionTest2DGame(
    soundEnabled: Boolean,
    onFinish: (score: Int, total: Int, correct: Int, xp: Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var trial by remember { mutableIntStateOf(1) }
    var state by remember { mutableStateOf("WAIT") } // WAIT | GO | TOO_SOON
    var goTimestamp by remember { mutableLongStateOf(0L) }
    val timesMs = remember { mutableStateListOf<Long>() }

    LaunchedEffect(trial, state) {
        if (state == "WAIT") {
            delay(Random.nextLong(1200L, 2800L))
            goTimestamp = System.currentTimeMillis()
            state = "GO"
            SoundManager.play(SoundEffect.COUNTDOWN_TICK)
        }
    }

    val scheme = MaterialTheme.colorScheme
    val padColor = when (state) {
        "GO" -> scheme.tertiary
        "TOO_SOON" -> scheme.error
        else -> scheme.primaryContainer
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Reflex Trial $trial / 5",
            style = MaterialTheme.typography.titleMedium,
            color = scheme.secondary
        )
        if (timesMs.isNotEmpty()) {
            Text(
                text = "Average Reflex: ${timesMs.average().toInt()} ms",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(padColor)
                .border(2.dp, scheme.outline, RoundedCornerShape(26.dp))
                .clickable {
                    if (soundEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    when (state) {
                        "WAIT" -> {
                            state = "TOO_SOON"
                            SoundManager.play(SoundEffect.GAME_WRONG_MOVE)
                        }
                        "TOO_SOON" -> {
                            state = "WAIT"
                            SoundManager.play(SoundEffect.CLICK)
                        }
                        "GO" -> {
                            SoundManager.play(SoundEffect.GAME_CORRECT_MOVE)
                            val reaction = (System.currentTimeMillis() - goTimestamp).coerceAtLeast(80L)
                            timesMs.add(reaction)
                            if (trial >= 5) {
                                val avg = timesMs.average().toInt()
                                val score = (600 - avg).coerceIn(50, 500)
                                onFinish(score, 5, 5, 30)
                            } else {
                                trial++
                                state = "WAIT"
                            }
                        }
                    }
                }
                .testTag("reaction_signal_pad"),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Reflex pad",
                    tint = scheme.onPrimary,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = when (state) {
                        "GO" -> "TAP NOW!"
                        "TOO_SOON" -> "Too Early! Tap to Retry Trial"
                        else -> "Wait for Neon Green Signal..."
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = scheme.onPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun NumberScramble2DGame(
    soundEnabled: Boolean,
    onFinish: (score: Int, total: Int, correct: Int, xp: Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val numbers = remember {
        generateSequence { Random.nextInt(11, 99) }.distinct().take(9).toList()
    }
    val sortedTarget = remember(numbers) { numbers.sorted() }
    var nextIndex by remember { mutableIntStateOf(0) }
    var mistakes by remember { mutableIntStateOf(0) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Tap Numbers in Ascending Order (Smallest -> Largest)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Cleared: $nextIndex / 9   |   Mistakes: $mistakes",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        for (r in 0 until 3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (c in 0 until 3) {
                    val num = numbers[r * 3 + c]
                    val alreadyCleared = sortedTarget.indexOf(num) < nextIndex
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .border(
                                1.5.dp,
                                if (alreadyCleared) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(18.dp)
                            )
                            .clickable(enabled = !alreadyCleared) {
                                if (soundEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (num == sortedTarget[nextIndex]) {
                                    SoundManager.play(SoundEffect.GAME_CORRECT_MOVE)
                                    nextIndex++
                                    if (nextIndex >= 9) {
                                        SoundManager.play(SoundEffect.MATCH_COMPLETED)
                                        val correct = (9 - mistakes).coerceAtLeast(1)
                                        onFinish(correct * 12, 9, correct, 32)
                                    }
                                } else {
                                    SoundManager.play(SoundEffect.GAME_WRONG_MOVE)
                                    mistakes++
                                }
                            }
                            .testTag("number_scramble_tile_$num"),
                        color = if (alreadyCleared) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (alreadyCleared) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Cleared",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(34.dp)
                                )
                            } else {
                                Text(
                                    text = "$num",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordScramble2DGame(
    language: String,
    soundEnabled: Boolean,
    onFinish: (score: Int, total: Int, correct: Int, xp: Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val rounds = remember { BrainGamesData.generateWordScrambleRounds() }
    var roundIdx by remember { mutableIntStateOf(0) }
    var solvedCount by remember { mutableIntStateOf(0) }
    val pickedIndices = remember { mutableStateListOf<Int>() }

    val current = rounds.getOrNull(roundIdx) ?: return
    val builtWord = pickedIndices.map { current.scrambled[it] }.joinToString("")

    LaunchedEffect(builtWord) {
        if (builtWord.length == current.answer.length) {
            if (builtWord == current.answer) {
                SoundManager.play(SoundEffect.MATCH_FOUND)
                val nextSolved = solvedCount + 1
                solvedCount = nextSolved
                delay(250L)
                pickedIndices.clear()
                if (roundIdx + 1 >= rounds.size) {
                    onFinish(nextSolved * 20, rounds.size, nextSolved, 35)
                } else {
                    roundIdx++
                }
            } else {
                SoundManager.play(SoundEffect.GAME_WRONG_MOVE)
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Word ${roundIdx + 1} / ${rounds.size}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (language == "hi") current.clueHi else current.clueEn,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = builtWord.padEnd(current.answer.length, '_').toList().joinToString(" "),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            current.scrambled.forEachIndexed { idx, ch ->
                val used = idx in pickedIndices
                Surface(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        .clickable(enabled = !used) {
                            if (soundEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            SoundManager.play(SoundEffect.SELECT)
                            pickedIndices.add(idx)
                        }
                        .testTag("word_letter_$idx"),
                    color = if (used) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = ch.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = {
                    SoundManager.play(SoundEffect.DELETE)
                    pickedIndices.clear()
                },
                modifier = Modifier.height(48.dp)
            ) {
                Text("Clear Letters")
            }
            OutlinedButton(
                onClick = {
                    SoundManager.play(SoundEffect.CLICK)
                    pickedIndices.clear()
                    if (roundIdx + 1 >= rounds.size) {
                        onFinish(solvedCount * 20, rounds.size, solvedCount, (solvedCount * 7).coerceAtLeast(10))
                    } else {
                        roundIdx++
                    }
                },
                modifier = Modifier.height(48.dp)
            ) {
                Text("Skip Word")
            }
        }
    }
}

@Composable
private fun FingerSpeed2DGame(
    soundEnabled: Boolean,
    onFinish: (score: Int, total: Int, correct: Int, xp: Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var started by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableIntStateOf(10) }
    var taps by remember { mutableIntStateOf(0) }

    LaunchedEffect(started) {
        if (started) {
            while (secondsLeft > 0) {
                delay(1000L)
                secondsLeft--
                if (secondsLeft in 1..3) {
                    SoundManager.play(SoundEffect.TIMER_WARN)
                }
            }
            SoundManager.play(SoundEffect.TIMER_END)
            val xp = (taps / 2).coerceIn(10, 45)
            onFinish(taps * 3, taps.coerceAtLeast(1), taps.coerceAtLeast(1), xp)
        }
    }

    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Time Left: ${secondsLeft}s   |   Total Taps: $taps",
            style = MaterialTheme.typography.headlineSmall,
            color = scheme.secondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap the glowing cyber core as fast as possible in 10 seconds!",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(230.dp)
                .clip(CircleShape)
                .clickable {
                    if (soundEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (!started) {
                        started = true
                        SoundManager.play(SoundEffect.GAME_START)
                    }
                    if (secondsLeft > 0) {
                        taps++
                        SoundManager.play(SoundEffect.POINTS_EARNED)
                    }
                }
                .testTag("finger_speed_target"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                drawCircle(color = scheme.primaryContainer, radius = r)
                drawCircle(color = scheme.primary.copy(alpha = 0.45f), radius = r * 0.78f)
                drawCircle(color = scheme.secondary, radius = r * 0.52f, center = Offset(r, r))
            }
            Text(
                text = if (!started) "TAP TO START" else "$taps",
                style = MaterialTheme.typography.headlineMedium,
                color = scheme.onSecondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
