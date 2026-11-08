package com.example.ui.admin

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.RestartAlt
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.CurriculumCatalog
import com.example.data.model.AppConfig
import com.example.data.model.LeaderboardEntry
import com.example.data.model.WinnerRecord

@Composable
fun AdminLeaderboardsScreen(
    appConfig: AppConfig,
    entries: List<LeaderboardEntry>,
    winners: List<WinnerRecord>,
    onSaveConfig: (AppConfig, String) -> Unit,
    onArchiveAndResetMonthly: (archiveMonthLabel: String, defaultPrizeLabel: String) -> Unit,
    onAddManualWinner: (monthLabel: String, name: String, studentClass: Int, rank: Int, xp: Long, prize: String) -> Unit,
    onToggleWinnerVisibility: (WinnerRecord, Boolean) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var boardScope by remember { mutableStateOf("monthly") } // daily | weekly | monthly | all_time
    var showConfirmResetDialog by remember { mutableStateOf(false) }

    var resetDayText by remember { mutableStateOf(appConfig.monthlyResetDay.toString()) }
    var rewardNotes by remember { mutableStateOf(appConfig.badgeRewardConfigNotes) }
    var archiveMonthLabel by remember { mutableStateOf(appConfig.currentMonthKey) }
    var defaultPrizeText by remember { mutableStateOf("Monthly Merit Certificate & Educational Study Kit") }

    // Manual winner form
    var winMonth by remember { mutableStateOf(appConfig.currentMonthKey) }
    var winName by remember { mutableStateOf("") }
    var winClassText by remember { mutableStateOf("10") }
    var winRankText by remember { mutableStateOf("1") }
    var winXpText by remember { mutableStateOf("500") }
    var winPrize by remember { mutableStateOf("Academic Excellence Book Set") }

    LaunchedEffect(appConfig) {
        resetDayText = appConfig.monthlyResetDay.toString()
        rewardNotes = appConfig.badgeRewardConfigNotes
        archiveMonthLabel = appConfig.currentMonthKey
    }

    val todayKey = CurriculumCatalog.currentDayKey()
    val weekKey = CurriculumCatalog.currentWeekKey()
    val monthKey = CurriculumCatalog.currentMonthKey()

    val rankedEntries = remember(entries, boardScope, todayKey, weekKey, monthKey) {
        when (boardScope) {
            "daily" -> entries.filter { it.dailyKey == todayKey && it.dailyXp > 0L }.sortedByDescending { it.dailyXp }
            "weekly" -> entries.filter { it.weeklyKey == weekKey && it.weeklyXp > 0L }.sortedByDescending { it.weeklyXp }
            "monthly" -> entries.filter { it.monthlyXp > 0L }.sortedByDescending { it.monthlyXp }
            else -> entries.sortedByDescending { it.allTimeXp }
        }
    }

    if (showConfirmResetDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmResetDialog = false },
            title = { Text("Archive Top Winners & Reset Monthly XP?") },
            text = {
                Text(
                    "This action archives up to the Top 3 monthly scorers for '$archiveMonthLabel' into /winners_history and resets ONLY monthlyXp to 0. All student accounts, allTimeXp, quiz history, and previous winners remain permanently preserved."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onArchiveAndResetMonthly(archiveMonthLabel, defaultPrizeText)
                        showConfirmResetDialog = false
                    }
                ) {
                    Text("Confirm Safe Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_leaderboards_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Leaderboard Visibility Toggle Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp).border(1.5.dp, scheme.primary, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = scheme.tertiary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Student Leaderboards (${if (appConfig.leaderboardEnabled) "ENABLED" else "DISABLED"})",
                                style = MaterialTheme.typography.titleMedium,
                                color = scheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Toggle live student leaderboard visibility across the /app Student Portal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = appConfig.leaderboardEnabled,
                        onCheckedChange = { enabled ->
                            onSaveConfig(
                                appConfig.copy(leaderboardEnabled = enabled),
                                "Set leaderboardEnabled=$enabled"
                            )
                        },
                        modifier = Modifier.testTag("leaderboard_master_switch")
                    )
                }
            }
        }

        // Rankings Filter Row
        item {
            Column(modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Live Rankings (${rankedEntries.size})", style = MaterialTheme.typography.titleMedium, color = scheme.onBackground)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "daily" to "Daily ($todayKey)",
                        "weekly" to "Weekly ($weekKey)",
                        "monthly" to "Monthly ($monthKey)",
                        "all_time" to "All-Time XP"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = boardScope == key,
                            onClick = { boardScope = key },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        if (rankedEntries.isEmpty()) {
            item {
                Text(
                    text = "No leaderboard entries recorded for the selected timeframe.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
        } else {
            itemsIndexed(rankedEntries.take(20), key = { _, e -> e.id.ifBlank { e.userId } }) { idx, entry ->
                val score = when (boardScope) {
                    "daily" -> entry.dailyXp
                    "weekly" -> entry.weeklyXp
                    "monthly" -> entry.monthlyXp
                    else -> entry.allTimeXp
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = scheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 860.dp)
                        .border(1.dp, scheme.outline, RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#${idx + 1}  ${entry.displayName} (Class ${entry.studentClass})",
                            style = MaterialTheme.typography.titleSmall,
                            color = scheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$score XP  •  Lv.${entry.level}  •  ${entry.accuracyPercent}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = scheme.tertiary
                        )
                    }
                }
            }
        }

        // Monthly Point Reset & Spark Free-Tier Config
        item {
            Card(
                modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp).border(1.dp, scheme.outline, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, tint = scheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Monthly Point Reset & Non-Destructive Winner Archival", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                    }
                    Text(
                        text = "Spark Free-Tier Policy: Scheduled Cloud Functions require Firebase Blaze billing, so automatic cron jobs are not enabled. In addition to automatic month-key bucketing on student activity, use the safe 1-tap reset below on your target day to archive Top 3 winners and reset monthlyXp = 0 without deleting any historical data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = resetDayText,
                            onValueChange = { resetDayText = it },
                            label = { Text("Target Monthly Reset Day (1-28)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = {
                                val day = resetDayText.toLongOrNull()?.coerceIn(1L, 28L) ?: 1L
                                onSaveConfig(
                                    appConfig.copy(monthlyResetDay = day),
                                    "Configured monthlyResetDay=$day"
                                )
                            },
                            modifier = Modifier.align(Alignment.CenterVertically).height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Day")
                        }
                    }
                    OutlinedTextField(
                        value = archiveMonthLabel,
                        onValueChange = { archiveMonthLabel = it },
                        label = { Text("Month Label to Archive (e.g. 2026-11)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = defaultPrizeText,
                        onValueChange = { defaultPrizeText = it },
                        label = { Text("Default Top-3 Educational Reward Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { showConfirmResetDialog = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("archive_reset_monthly_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Archive Top Winners & Reset Monthly XP")
                    }
                }
            }
        }

        // Previous Winners History + Manual Entry
        item {
            Card(
                modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp).border(1.dp, scheme.outline, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Previous Winners (${winners.size})", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                    winners.take(10).forEach { w ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${w.monthLabel} • Rank ${w.rank}: ${w.winnerName} (Class ${w.studentClass})", style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                                Text("${w.xpScore} XP  •  Reward: ${w.prizeWon}", style = MaterialTheme.typography.bodySmall, color = scheme.secondary)
                            }
                            Switch(
                                checked = w.isPublished,
                                onCheckedChange = { pub -> onToggleWinnerVisibility(w, pub) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Record Verified Winner Manually", style = MaterialTheme.typography.titleSmall, color = scheme.secondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = winMonth, onValueChange = { winMonth = it }, label = { Text("Month") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = winRankText, onValueChange = { winRankText = it }, label = { Text("Rank") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = winClassText, onValueChange = { winClassText = it }, label = { Text("Class") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = winName, onValueChange = { winName = it }, label = { Text("Winner Name") }, singleLine = true, modifier = Modifier.weight(2f))
                        OutlinedTextField(value = winXpText, onValueChange = { winXpText = it }, label = { Text("XP") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = winPrize, onValueChange = { winPrize = it }, label = { Text("Educational Reward Awarded") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedButton(
                        onClick = {
                            onAddManualWinner(
                                winMonth,
                                winName,
                                winClassText.toIntOrNull()?.coerceIn(1, 12) ?: 10,
                                winRankText.toIntOrNull()?.coerceIn(1, 10) ?: 1,
                                winXpText.toLongOrNull() ?: 0L,
                                winPrize
                            )
                            winName = ""
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add Previous Winner Record")
                    }
                }
            }
        }

        // Badges & Safe Age-Appropriate Rewards Policy Config
        item {
            Card(
                modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp).border(1.dp, scheme.outline, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = scheme.tertiary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Badges & Age-Appropriate Educational Rewards", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                    }
                    CurriculumCatalog.badges.forEach { badge ->
                        Text(
                            text = "${badge.titleEn} (${badge.id}): ${badge.descriptionEn}",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                    OutlinedTextField(
                        value = rewardNotes,
                        onValueChange = { rewardNotes = it },
                        label = { Text("Age-Appropriate Reward Policy & Notes") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onSaveConfig(
                                appConfig.copy(badgeRewardConfigNotes = rewardNotes),
                                "Updated badge & age-appropriate reward configuration"
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Reward Configuration")
                    }
                }
            }
        }
    }
}
