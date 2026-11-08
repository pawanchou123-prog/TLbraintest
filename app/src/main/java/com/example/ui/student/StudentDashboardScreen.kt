package com.example.ui.student

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrackChanges
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.CurriculumCatalog
import com.example.data.model.AppConfig
import com.example.data.model.BannerItem
import com.example.data.model.QuizResultRecord
import com.example.data.model.UserProfile

@Composable
fun StudentDashboardScreen(
    profile: UserProfile,
    appConfig: AppConfig,
    banners: List<BannerItem>,
    recentResults: List<QuizResultRecord>,
    unreadNoticesCount: Int,
    onOpenQuizSection: () -> Unit,
    onLaunchGkQuiz: () -> Unit,
    onOpenGamesSection: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val isHi = profile.language == "hi"
    val studentClass = profile.studentClass.toInt().coerceIn(1, 12)
    val levelProgress = ((profile.xp % 200L).toFloat() / 200f).coerceIn(0f, 1f)

    val todayKey = remember { CurriculumCatalog.currentDayKey() }
    val todayActivities = remember(recentResults, todayKey) {
        recentResults.filter { it.dateKey == todayKey }
    }
    val todayXp = remember(todayActivities) { todayActivities.sumOf { it.xpEarned } }
    val todayQuizzes = remember(todayActivities) { todayActivities.count { it.activityType == "quiz" || it.activityType == "gk" } }
    val todayGames = remember(todayActivities) { todayActivities.count { it.activityType == "game" } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("student_home_dashboard"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Welcome Header Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
                    .border(1.5.dp, scheme.outline, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StudentAvatarBadge(avatarId = profile.avatarId, size = 56.dp, isSelected = true)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHi) "स्वागत है, ${profile.displayName}" else "Welcome back, ${profile.displayName}",
                            style = MaterialTheme.typography.titleLarge,
                            color = scheme.onSurface
                        )
                        Text(
                            text = buildString {
                                append("Class $studentClass")
                                if (profile.schoolName.isNotBlank()) {
                                    append("  •  ${profile.schoolName}")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.secondary
                        )
                    }
                    Box {
                        IconButton(
                            onClick = onOpenNotifications,
                            modifier = Modifier.testTag("dashboard_notifications_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = scheme.secondary
                            )
                        }
                        if (unreadNoticesCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 8.dp)
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(scheme.tertiary)
                            )
                        }
                    }
                }
            }
        }

        // 2. Promotional Banner (Live HTTPS Banner if available; honest Sample Preview Banner if empty)
        item {
            Column(modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp)) {
                if (banners.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(banners, key = { it.id }) { banner ->
                            Card(
                                modifier = Modifier
                                    .width(320.dp)
                                    .border(1.dp, scheme.outline, RoundedCornerShape(20.dp))
                                    .clickable(enabled = banner.actionUrl.startsWith("https://")) {
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(banner.actionUrl)))
                                        } catch (_: Exception) {
                                        }
                                    },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = scheme.surface)
                            ) {
                                Column {
                                    AsyncImage(
                                        model = banner.imageUrl,
                                        contentDescription = banner.title,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(135.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(banner.title, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                                        if (banner.subtitle.isNotBlank()) {
                                            Text(banner.subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(165.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .border(1.5.dp, scheme.outline, RoundedCornerShape(22.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_banner_1793633719046),
                            contentDescription = stringResource(R.string.cd_hero_banner),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            scheme.background.copy(alpha = 0.90f),
                                            scheme.background.copy(alpha = 0.40f)
                                        )
                                    )
                                )
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = scheme.primaryContainer,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "SAMPLE PREVIEW BANNER",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.secondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "TL BRAIN TEST — Learn, Think, Play, Improve.",
                                style = MaterialTheme.typography.titleMedium,
                                color = scheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = appConfig.appVersionBanner,
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.secondary
                            )
                        }
                    }
                }
            }
        }

        // 3. Total XP, Current Level, Quiz Accuracy, Total Points
        item {
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    DashboardStatCard(
                        icon = Icons.Default.Bolt,
                        label = if (isHi) "कुल XP (Total XP)" else "Total XP",
                        value = "${profile.xp} XP",
                        modifier = Modifier.weight(1f)
                    )
                    DashboardStatCard(
                        icon = Icons.Default.Star,
                        label = if (isHi) "वर्तमान लेवल" else "Current Level",
                        value = "Level ${profile.level}",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    DashboardStatCard(
                        icon = Icons.Default.TrackChanges,
                        label = if (isHi) "क्विज़ सटीकता" else "Quiz Accuracy",
                        value = "${profile.accuracyPercent}%",
                        modifier = Modifier.weight(1f)
                    )
                    DashboardStatCard(
                        icon = Icons.Default.EmojiEvents,
                        label = if (isHi) "कुल अंक (Points)" else "Total Points",
                        value = "${profile.totalPoints}",
                        modifier = Modifier.weight(1f)
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, scheme.outline.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isHi) "अगले लेवल तक प्रगति" else "Progress to Level ${profile.level + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                color = scheme.onSurface
                            )
                            Text(
                                text = "${profile.xp % 200} / 200 XP",
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.secondary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { levelProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = scheme.primary,
                            trackColor = scheme.surfaceVariant
                        )
                    }
                }
            }
        }

        // 4. Daily Activity Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
                    .border(1.dp, scheme.outline.copy(alpha = 0.7f), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = scheme.tertiary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHi) "आज की गतिविधि ($todayKey)" else "Daily Activity ($todayKey)",
                            style = MaterialTheme.typography.titleMedium,
                            color = scheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (todayActivities.isEmpty()) {
                            if (isHi) "आज अभी तक कोई क्विज़ या गेम पूरा नहीं हुआ। अभी अभ्यास शुरू करें।"
                            else "No quizzes or brain games completed yet today. Start a session below to earn daily XP."
                        } else {
                            "Today: $todayQuizzes Quizzes  •  $todayGames Brain Games  •  +$todayXp XP Earned"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (todayActivities.isEmpty()) scheme.onSurfaceVariant else scheme.tertiary
                    )
                }
            }
        }

        // 5. All 5 Primary Dashboard Action Buttons (Quiz, GK, Games, Leaderboard, Profile)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
                    .border(1.dp, scheme.outline, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isHi) "त्वरित नेविगेशन" else "Quick Launch Arena",
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = onOpenQuizSection,
                            modifier = Modifier.weight(1f).height(50.dp).testTag("dashboard_btn_quiz"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isHi) "क्विज़ (Quiz)" else "Quiz")
                        }
                        Button(
                            onClick = onLaunchGkQuiz,
                            modifier = Modifier.weight(1f).height(50.dp).testTag("dashboard_btn_gk"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isHi) "सामान्य ज्ञान (GK)" else "GK")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = onOpenGamesSection,
                            modifier = Modifier.weight(1f).height(50.dp).testTag("dashboard_btn_games"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.SportsEsports, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isHi) "गेम्स (Games)" else "Games")
                        }
                        OutlinedButton(
                            onClick = onOpenLeaderboard,
                            modifier = Modifier.weight(1f).height(50.dp).testTag("dashboard_btn_leaderboard"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isHi) "लीडरबोर्ड" else "Leaderboard")
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenProfile,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dashboard_btn_profile"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isHi) "छात्र प्रोफ़ाइल (Student Profile)" else "Student Profile")
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier.border(1.dp, scheme.outline.copy(alpha = 0.65f), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(scheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = scheme.secondary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(value, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface, fontWeight = FontWeight.Bold)
                Text(label, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            }
        }
    }
}
