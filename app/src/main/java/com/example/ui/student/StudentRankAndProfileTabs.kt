package com.example.ui.student

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.CurriculumCatalog
import com.example.data.SamplePreviewCatalog
import com.example.data.model.AllowedStudent
import com.example.data.model.AppConfig
import com.example.data.model.LeaderboardEntry
import com.example.data.model.PrizeItem
import com.example.data.model.UserProfile
import com.example.data.model.WinnerRecord

@Composable
fun StudentLeaderboardTab(
    currentUserId: String,
    appConfig: AppConfig,
    entries: List<LeaderboardEntry>,
    prizes: List<PrizeItem>,
    winners: List<WinnerRecord>
) {
    val scheme = MaterialTheme.colorScheme
    var boardPeriod by remember { mutableIntStateOf(2) } // 0=Daily, 1=Weekly, 2=Monthly, 3=All-Time
    var showSamplePreviewWhenEmpty by remember { mutableStateOf(false) }
    val periodTitles = listOf("Daily", "Weekly", "Monthly", "All-Time")

    val dayKey = CurriculumCatalog.currentDayKey()
    val weekKey = CurriculumCatalog.currentWeekKey()
    val monthKey = CurriculumCatalog.currentMonthKey()

    val liveRanked = remember(entries, boardPeriod, dayKey, weekKey, monthKey) {
        when (boardPeriod) {
            0 -> entries.filter { it.dailyKey == dayKey && it.dailyXp > 0L }.sortedByDescending { it.dailyXp }
            1 -> entries.filter { it.weeklyKey == weekKey && it.weeklyXp > 0L }.sortedByDescending { it.weeklyXp }
            2 -> entries.filter { it.monthlyKey == monthKey && it.monthlyXp > 0L }.sortedByDescending { it.monthlyXp }
            else -> entries.filter { it.allTimeXp > 0L }.sortedByDescending { it.allTimeXp }
        }
    }

    val usingSampleEntries = liveRanked.isEmpty() && showSamplePreviewWhenEmpty
    val rankedList = if (usingSampleEntries) SamplePreviewCatalog.sampleLeaderboard else liveRanked

    val usingSampleWinners = winners.isEmpty() && showSamplePreviewWhenEmpty
    val displayWinners = if (usingSampleWinners) SamplePreviewCatalog.samplePreviousWinners else winners

    LaunchedEffect(Unit) {
        SoundManager.play(SoundEffect.LEADERBOARD_OPEN)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("student_leaderboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp)) {
                Text("Student Rankings & Leaderboards", style = MaterialTheme.typography.headlineSmall, color = scheme.onBackground)
                Spacer(modifier = Modifier.height(10.dp))
                TabRow(selectedTabIndex = boardPeriod, containerColor = scheme.surface) {
                    periodTitles.forEachIndexed { idx, label ->
                        Tab(
                            selected = boardPeriod == idx,
                            onClick = {
                                boardPeriod = idx
                                SoundManager.play(SoundEffect.TAB_CHANGE)
                            },
                            text = { Text(label) },
                            modifier = Modifier.testTag("leaderboard_tab_${label.lowercase()}")
                        )
                    }
                }
            }
        }

        if (!appConfig.leaderboardEnabled) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp)
                        .border(1.dp, scheme.outline, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.PauseCircle, contentDescription = null, tint = scheme.secondary, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Leaderboard Paused by Administrator", style = MaterialTheme.typography.titleLarge, color = scheme.onSurface)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Rankings are temporarily hidden during evaluation. Keep practicing quizzes and games — your XP is still being recorded.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else if (rankedList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp)
                        .border(1.dp, scheme.outline.copy(alpha = 0.7f), RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Live Firestore Scores Yet (${periodTitles[boardPeriod]})",
                            style = MaterialTheme.typography.titleMedium,
                            color = scheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Complete any quiz or 2D brain game to post the first live score, or preview the leaderboard layout with sample rows below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { showSamplePreviewWhenEmpty = true },
                            modifier = Modifier.testTag("leaderboard_show_sample_btn")
                        ) {
                            Text("Preview Sample Rankings (Not Live Data)")
                        }
                    }
                }
            }
        } else {
            if (usingSampleEntries) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = scheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SAMPLE PREVIEW RANKINGS — Not Live Firestore Data",
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = true,
                                onClick = { showSamplePreviewWhenEmpty = false },
                                label = { Text("Hide Sample") }
                            )
                        }
                    }
                }
            }

            itemsIndexed(rankedList.take(50), key = { _, item -> item.id }) { index, entry ->
                val xpVal = when (boardPeriod) {
                    0 -> entry.dailyXp
                    1 -> entry.weeklyXp
                    2 -> entry.monthlyXp
                    else -> entry.allTimeXp
                }
                val isMe = entry.userId == currentUserId
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp)
                        .border(
                            width = if (isMe || index < 3) 1.5.dp else 1.dp,
                            color = when {
                                index == 0 -> scheme.tertiary
                                isMe -> scheme.primary
                                else -> scheme.outline.copy(alpha = 0.5f)
                            },
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isMe) scheme.primaryContainer.copy(alpha = 0.45f) else scheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (index < 3) scheme.tertiaryContainer else scheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#${index + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (index < 3) scheme.tertiary else scheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isMe) "${entry.displayName} (You)" else entry.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = scheme.onSurface
                            )
                            Text(
                                text = "Class ${entry.studentClass}  •  Level ${entry.level}  •  ${entry.accuracyPercent}% Accuracy",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "$xpVal XP",
                            style = MaterialTheme.typography.titleMedium,
                            color = scheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Monthly Prizes Showcase
        if (prizes.isNotEmpty()) {
            item {
                Text(
                    text = "Monthly Prizes & Scholar Rewards",
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onBackground,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp).padding(top = 6.dp)
                )
            }
            items(prizes, key = { it.id }) { prize ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp)
                        .border(1.dp, scheme.outline, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.surface)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (prize.imageUrl.startsWith("https://")) {
                            AsyncImage(
                                model = prize.imageUrl,
                                contentDescription = prize.title,
                                modifier = Modifier.size(68.dp).clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        } else {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = scheme.tertiary, modifier = Modifier.size(38.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prize.rankLabel, style = MaterialTheme.typography.labelLarge, color = scheme.tertiary)
                            Text(prize.title, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                            if (prize.description.isNotBlank()) {
                                Text(prize.description, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // Previous Winners Hall of Fame
        item {
            Row(
                modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp).padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = scheme.secondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (usingSampleWinners) "Previous Winners (Sample Preview)" else "Previous Monthly Winners",
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onBackground
                )
            }
        }

        if (displayWinners.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = scheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp)
                ) {
                    Text(
                        text = "No archived monthly winners published in Firestore yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(displayWinners, key = { it.id }) { win ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = scheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${win.monthLabel} • Rank #${win.rank}: ${win.winnerName}", style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                            Text("Class ${win.studentClass} • Reward: ${win.prizeWon}", style = MaterialTheme.typography.bodySmall, color = scheme.secondary)
                        }
                        Text("${win.xpScore} XP", style = MaterialTheme.typography.labelLarge, color = scheme.tertiary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudentProfileSectionScreen(
    profile: UserProfile,
    allowedStudent: AllowedStudent?,
    isOwnerTestAccount: Boolean,
    appConfig: AppConfig,
    onSaveProfile: (
        displayName: String,
        studentClass: Int,
        schoolName: String,
        avatarId: String,
        language: String,
        soundEnabled: Boolean
    ) -> Unit,
    onOpenSubscriptionPage: () -> Unit,
    onOpenSupportPage: () -> Unit,
    onOpenNotificationsPage: () -> Unit,
    onOpenTermsPage: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val isHi = profile.language == "hi"
    val globalSoundEnabled by SoundManager.soundEnabledFlow.collectAsStateWithLifecycle()
    val globalSoundVolume by SoundManager.soundVolumeFlow.collectAsStateWithLifecycle()

    var editName by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var editClass by remember(profile.studentClass) { mutableIntStateOf(profile.studentClass.toInt().coerceIn(1, 12)) }
    var editSchool by remember(profile.schoolName) { mutableStateOf(profile.schoolName) }
    var editAvatar by remember(profile.avatarId) { mutableStateOf(profile.avatarId.ifBlank { "cyber_brain" }) }
    var editLang by remember(profile.language) { mutableStateOf(profile.language) }
    var editSound by remember(globalSoundEnabled) { mutableStateOf(globalSoundEnabled) }

    val subStatusText = when {
        allowedStudent?.isSubscriptionCurrentlyValid() == true ->
            "ACTIVE (Expires ${CurriculumCatalog.formatReadableDate(allowedStudent.subscriptionEndMs)})"
        isOwnerTestAccount -> "ACTIVE (Owner Test Account)"
        else -> "INACTIVE"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("student_profile_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Student Identity Summary Card (Name, Gmail, XP, Level, Points, Subscription Status)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
                    .border(1.5.dp, scheme.outline, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StudentAvatarBadge(avatarId = editAvatar, size = 64.dp, isSelected = true)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(profile.displayName, style = MaterialTheme.typography.titleLarge, color = scheme.onSurface)
                            Text(profile.email, style = MaterialTheme.typography.bodySmall, color = scheme.secondary)
                            Text(
                                text = "Subscription: $subStatusText",
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ProfileMiniStat("Level", "${profile.level}", Modifier.weight(1f))
                        ProfileMiniStat("Total XP", "${profile.xp}", Modifier.weight(1f))
                        ProfileMiniStat("Points", "${profile.totalPoints}", Modifier.weight(1f))
                        ProfileMiniStat("Accuracy", "${profile.accuracyPercent}%", Modifier.weight(1f))
                    }
                }
            }
        }

        // 2. Editable Student Profile Card (Name, Gmail, Class 1-12, School Name, Avatar, Language)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
                    .border(1.dp, scheme.outline.copy(alpha = 0.8f), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isHi) "छात्र प्रोफ़ाइल विवरण (Student Profile)" else "Student Profile Details",
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface
                    )

                    Text("Choose Profile Avatar:", style = MaterialTheme.typography.labelLarge, color = scheme.onSurface)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CurriculumCatalog.avatars.forEach { av ->
                            Box(modifier = Modifier.clickable {
                                editAvatar = av.id
                                SoundManager.play(SoundEffect.AVATAR_SELECT)
                            }) {
                                StudentAvatarBadge(
                                    avatarId = av.id,
                                    size = 46.dp,
                                    isSelected = editAvatar == av.id
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Student Name") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_name_field"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = profile.email,
                        onValueChange = {},
                        enabled = false,
                        label = { Text("Approved Gmail (Verified)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editSchool,
                        onValueChange = { editSchool = it },
                        label = { Text("School Name") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_school_field"),
                        singleLine = true
                    )

                    Text("Class Selection (Class 1 to 12):", style = MaterialTheme.typography.labelLarge, color = scheme.onSurface)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..12).forEach { cls ->
                            FilterChip(
                                selected = editClass == cls,
                                onClick = {
                                    editClass = cls
                                    SoundManager.play(SoundEffect.SELECT)
                                },
                                label = { Text("Class $cls") }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Preferred Language:", style = MaterialTheme.typography.labelLarge, color = scheme.onSurface)
                        FilterChip(
                            selected = editLang == "en",
                            onClick = {
                                editLang = "en"
                                SoundManager.play(SoundEffect.SELECT)
                            },
                            label = { Text("English") },
                            modifier = Modifier.testTag("profile_lang_en_chip")
                        )
                        FilterChip(
                            selected = editLang == "hi",
                            onClick = {
                                editLang = "hi"
                                SoundManager.play(SoundEffect.SELECT)
                            },
                            label = { Text("हिन्दी (Hindi)") },
                            modifier = Modifier.testTag("profile_lang_hi_chip")
                        )
                    }

                    // --- Global Audio & Sound Settings Section ---
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = scheme.surfaceVariant.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, scheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (globalSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                        contentDescription = "Sound Status",
                                        tint = if (globalSoundEnabled) scheme.secondary else scheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (isHi) "ध्वनि प्रभाव (Sound Effects)" else "Sound Effects & Audio",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = scheme.onSurface
                                        )
                                        Text(
                                            text = if (globalSoundEnabled) "ON (Enabled)" else "MUTED (Disabled)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (globalSoundEnabled) scheme.secondary else scheme.error
                                        )
                                    }
                                }
                                Switch(
                                    checked = globalSoundEnabled,
                                    onCheckedChange = { checked ->
                                        SoundManager.setSoundEnabled(checked)
                                        editSound = checked
                                        SoundManager.play(if (checked) SoundEffect.TOGGLE_ON else SoundEffect.TOGGLE_OFF)
                                    },
                                    modifier = Modifier.testTag("audio_toggle_switch")
                                )
                            }

                            if (globalSoundEnabled) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isHi) "मास्टर वॉल्यूम" else "Master Sound Volume",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = scheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${(globalSoundVolume * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = scheme.secondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Slider(
                                        value = globalSoundVolume,
                                        onValueChange = { newVol ->
                                            SoundManager.setVolume(newVol)
                                        },
                                        valueRange = 0.0f..1.0f,
                                        modifier = Modifier.fillMaxWidth().testTag("audio_volume_slider")
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            SoundManager.play(SoundEffect.SUCCESS)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("audio_test_preview_btn")
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isHi) "ऑडियो टेस्ट" else "Test Sound Preview", style = MaterialTheme.typography.labelMedium)
                                    }

                                    Text(
                                        text = "100% Free • CC0 Royalty-Free",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = scheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            SoundManager.play(SoundEffect.SAVE)
                            SoundManager.setSoundEnabled(editSound)
                            onSaveProfile(editName, editClass, editSchool, editAvatar, editLang, editSound)
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("profile_save_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isHi) "प्रोफ़ाइल सहेजें" else "Save Student Profile")
                    }
                }
            }
        }

        // 3. Achievements & Badges Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
                    .border(1.dp, scheme.outline.copy(alpha = 0.7f), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = scheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Achievements & Badges (${profile.badges.size}/${CurriculumCatalog.badges.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = scheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    CurriculumCatalog.badges.forEach { badge ->
                        val unlocked = profile.badges.contains(badge.id)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (unlocked) scheme.primaryContainer.copy(alpha = 0.55f) else scheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (unlocked) Icons.Default.MilitaryTech else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (unlocked) scheme.tertiary else scheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isHi) badge.titleHi else badge.titleEn,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = scheme.onSurface
                                    )
                                    Text(
                                        text = if (isHi) badge.descriptionHi else badge.descriptionEn,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = if (unlocked) "UNLOCKED" else "LOCKED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (unlocked) scheme.tertiary else scheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Student Services, Channels, Legal & Logout
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
                    .border(1.dp, scheme.outline, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isHi) "खाता सेवाएँ और सहायता" else "Student Services & Channels",
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface
                    )

                    OutlinedButton(
                        onClick = onOpenSubscriptionPage,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("profile_open_subscription_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.CardMembership, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Subscription Details (Rs. 50 / 3 Months)")
                    }

                    OutlinedButton(
                        onClick = onOpenSupportPage,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("profile_open_support_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Customer Support (1-to-1 Admin Help)")
                    }

                    OutlinedButton(
                        onClick = onOpenNotificationsPage,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("profile_open_notifications_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("In-App Notifications")
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                if (appConfig.whatsappUrl.startsWith("https://")) {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(appConfig.whatsappUrl)))
                                    } catch (_: Exception) {
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("profile_whatsapp_channel_btn"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WhatsApp")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                        }

                        OutlinedButton(
                            onClick = {
                                if (appConfig.youtubeUrl.startsWith("https://")) {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(appConfig.youtubeUrl)))
                                    } catch (_: Exception) {
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("profile_youtube_channel_btn"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("YouTube")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenTermsPage,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("profile_open_terms_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Gavel, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Terms and Conditions")
                    }

                    if (com.example.data.model.isPreAuthorizedMainAdminEmail(profile.email)) {
                        OutlinedButton(
                            onClick = {
                                context.startActivity(Intent(context, com.example.AdminActivity::class.java))
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("profile_open_admin_activity_btn"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Main Admin Application (/admin)")
                        }
                    }

                    Button(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("profile_logout_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Logout")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileMiniStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
