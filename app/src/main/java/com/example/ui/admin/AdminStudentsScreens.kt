package com.example.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.data.model.AllowedStudent
import com.example.data.model.QuizResultRecord
import com.example.data.model.SubscriptionRecord
import com.example.data.model.UserProfile
import com.example.data.model.sanitizeEmailKey

@Composable
fun AdminStudentsListScreen(
    students: List<AllowedStudent>,
    profiles: List<UserProfile>,
    pendingRequests: List<SubscriptionRecord>,
    onOpenStudentDetail: (String) -> Unit,
    onSaveOrApproveStudent: (
        email: String,
        name: String,
        studentClass: Int,
        lang: String,
        active: Boolean,
        method: String,
        startMs: Long,
        endMs: Long,
        notes: String
    ) -> Unit,
    onReviewRequest: (SubscriptionRecord, Boolean, String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var searchQuery by remember { mutableStateOf("") }
    var selectedClassFilter by remember { mutableIntStateOf(0) } // 0 = All, 1..12
    var statusFilter by remember { mutableStateOf("all") } // all | active | expired | banned
    var showAddStudentDialog by remember { mutableStateOf(false) }

    val profileByEmail = remember(profiles) {
        profiles.associateBy { it.email.trim().lowercase() }
    }

    val nowMs = System.currentTimeMillis()
    val filteredStudents = remember(students, searchQuery, selectedClassFilter, statusFilter, nowMs) {
        val q = searchQuery.trim().lowercase()
        students.filter { s ->
            val matchesQuery = q.isEmpty() ||
                s.studentName.lowercase().contains(q) ||
                s.email.lowercase().contains(q)
            val matchesClass = selectedClassFilter == 0 || s.studentClass.toInt() == selectedClassFilter
            val isActive = s.isSubscriptionCurrentlyValid(nowMs)
            val matchesStatus = when (statusFilter) {
                "active" -> isActive
                "expired" -> !s.isBanned && !isActive
                "banned" -> s.isBanned
                else -> true
            }
            matchesQuery && matchesClass && matchesStatus
        }
    }

    if (showAddStudentDialog) {
        StudentEditOrApproveModal(
            initial = null,
            onDismiss = { showAddStudentDialog = false },
            onSave = { email, name, sClass, lang, active, method, start, end, notes ->
                onSaveOrApproveStudent(email, name, sClass, lang, active, method, start, end, notes)
                showAddStudentDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_students_list_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Student Management (${students.size})",
                        style = MaterialTheme.typography.headlineSmall,
                        color = scheme.onBackground
                    )
                    Text(
                        text = "Create student accounts, set initial passwords, filter Classes 1–12, ban/unban, and audit student progress.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.secondary
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = { showAddStudentDialog = true },
                    modifier = Modifier.height(48.dp).testTag("admin_add_student_btn"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Create Student Account")
                }
            }
        }

        // Pending Access / Payment Requests Quick Queue
        val accessRequests = pendingRequests.filter { it.status == "pending" }
        if (accessRequests.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 860.dp)
                        .border(1.5.dp, scheme.tertiary, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Pending Access & Payment Requests (${accessRequests.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = scheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                        accessRequests.take(3).forEach { req ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = scheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${req.studentName}  •  Class ${req.studentClass}  •  ${req.studentEmail}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = scheme.onSurface
                                    )
                                    Text(
                                        text = "Mode: ${req.paymentMethod.uppercase()}  •  Ref: ${req.referenceCode.ifBlank { "None" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.onSurfaceVariant
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                onReviewRequest(
                                                    req,
                                                    true,
                                                    "Approved 90-day access from Student Management queue"
                                                )
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Approve 90d")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                onReviewRequest(
                                                    req,
                                                    false,
                                                    "Rejected access request after admin review"
                                                )
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Reject")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Search + Filters
        item {
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("Search student name or Gmail address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_student_search_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "all" to "All Status",
                        "active" to "Active Pass",
                        "expired" to "Expired",
                        "banned" to "Banned"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = statusFilter == key,
                            onClick = { statusFilter = key },
                            label = { Text(label) }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedClassFilter == 0,
                        onClick = { selectedClassFilter = 0 },
                        label = { Text("All Classes") }
                    )
                    (1..12).forEach { cls ->
                        FilterChip(
                            selected = selectedClassFilter == cls,
                            onClick = { selectedClassFilter = cls },
                            label = { Text("Class $cls") }
                        )
                    }
                }
            }
        }

        if (filteredStudents.isEmpty()) {
            item {
                Text(
                    text = "No student accounts match the current filter. Tap 'Approve Gmail' to authorize a student.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            items(filteredStudents, key = { it.id.ifBlank { it.email } }) { student ->
                val profile = profileByEmail[student.email.trim().lowercase()]
                val isValid = student.isSubscriptionCurrentlyValid(nowMs)
                val statusLabel = when {
                    student.isBanned -> "BANNED"
                    isValid -> "ACTIVE PASS"
                    else -> "EXPIRED / INACTIVE"
                }
                val statusColor = when {
                    student.isBanned -> scheme.error
                    isValid -> scheme.tertiary
                    else -> scheme.secondary
                }
                val emailKey = student.id.ifBlank { sanitizeEmailKey(student.email) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 860.dp)
                        .border(1.dp, scheme.outline, RoundedCornerShape(18.dp)),
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = student.studentName.ifBlank { "Student" },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = scheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${student.email}  •  Class ${student.studentClass}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = statusColor.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = statusLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Text(
                            text = "Pass Expiry: ${CurriculumCatalog.formatReadableDate(student.subscriptionEndMs)}  •  " +
                                if (profile != null) {
                                    "Points: ${profile.totalPoints}  •  Level ${profile.level}  •  Accuracy: ${profile.accuracyPercent}%"
                                } else {
                                    "Profile initializes on student's first Google Sign-In"
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.secondary
                        )

                        Button(
                            onClick = { onOpenStudentDetail(emailKey) },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("manage_student_$emailKey"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("View Profile, History, Points & Lifecycle")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminStudentDetailScreen(
    student: AllowedStudent?,
    profile: UserProfile?,
    studentQuizResults: List<QuizResultRecord>,
    onBack: () -> Unit,
    onUpdateStudentAccount: (
        email: String,
        name: String,
        studentClass: Int,
        lang: String,
        active: Boolean,
        method: String,
        startMs: Long,
        endMs: Long,
        notes: String
    ) -> Unit,
    onLifecycleAction: (AllowedStudent, String, String) -> Unit,
    onAdjustPoints: (UserProfile, Int, String) -> Unit,
    onToggleBadge: (UserProfile, String, Boolean, String) -> Unit
) {
    BackHandler(onBack = onBack)
    val scheme = MaterialTheme.colorScheme

    if (student == null) {
        Surface(modifier = Modifier.fillMaxSize(), color = scheme.background) {
            Column(modifier = Modifier.padding(24.dp)) {
                OutlinedButton(onClick = onBack) { Text("Back to Students") }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Student record not found.", color = scheme.onBackground)
            }
        }
        return
    }

    var lifecycleReason by remember { mutableStateOf("") }
    var pointsDeltaInput by remember { mutableStateOf("") }
    var pointsReasonInput by remember { mutableStateOf("") }
    var showEditModal by remember { mutableStateOf(false) }

    if (showEditModal) {
        StudentEditOrApproveModal(
            initial = student,
            onDismiss = { showEditModal = false },
            onSave = { email, name, sClass, lang, active, method, start, end, notes ->
                onUpdateStudentAccount(email, name, sClass, lang, active, method, start, end, notes)
                showEditModal = false
            }
        )
    }

    val isValid = student.isSubscriptionCurrentlyValid()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_student_detail_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("student_detail_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = scheme.onBackground)
                    }
                    Column {
                        Text(
                            text = student.studentName.ifBlank { "Student Detail" },
                            style = MaterialTheme.typography.headlineSmall,
                            color = scheme.onBackground
                        )
                        Text(
                            text = "${student.email}  •  Class ${student.studentClass}",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.secondary
                        )
                    }
                }
                OutlinedButton(onClick = { showEditModal = true }, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit")
                }
            }
        }

        // Profile & Subscription Summary
        item {
            Card(
                modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp).border(1.dp, scheme.outline, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Subscription & Performance Summary",
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface
                    )
                    Text(
                        text = "Status: ${if (student.isBanned) "BANNED" else if (isValid) "ACTIVE" else "EXPIRED"}  •  Method: ${student.paymentMethod.uppercase()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (student.isBanned) scheme.error else if (isValid) scheme.tertiary else scheme.secondary
                    )
                    Text(
                        text = "Start: ${CurriculumCatalog.formatReadableDate(student.subscriptionStartMs)}  •  Expiry: ${CurriculumCatalog.formatReadableDate(student.subscriptionEndMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                    if (!student.adminNotes.isNullOrBlank()) {
                        Text("Admin Notes: ${student.adminNotes}", style = MaterialTheme.typography.bodySmall, color = scheme.secondary)
                    }
                    if (profile != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Points: ${profile.totalPoints}  |  XP: ${profile.xp}  |  Level: ${profile.level}  |  Accuracy: ${profile.accuracyPercent}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Quizzes Played: ${profile.quizzesPlayed}  |  2D Games Played: ${profile.gamesPlayed}  |  Last Active: ${profile.lastActiveDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Account Lifecycle Controls (Activate / Deactivate / Ban / Unban)
        item {
            Card(
                modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp).border(1.dp, scheme.outline, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Account Status & Ban Controls", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                    OutlinedTextField(
                        value = lifecycleReason,
                        onValueChange = { lifecycleReason = it },
                        label = { Text("Audit Reason / Internal Note") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onLifecycleAction(student, "activate", lifecycleReason) },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("student_activate_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Activate")
                        }
                        OutlinedButton(
                            onClick = { onLifecycleAction(student, "deactivate", lifecycleReason) },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("student_deactivate_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Deactivate Pass")
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                onLifecycleAction(
                                    student,
                                    if (student.isBanned) "unban" else "ban",
                                    lifecycleReason
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("student_ban_toggle_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null, tint = scheme.error, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (student.isBanned) "Unban Student Account" else "Ban Student Account")
                        }
                    }
                }
            }
        }

        // Points Adjustment with Mandatory Reason
        item {
            Card(
                modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp).border(1.dp, scheme.outline, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Stars, contentDescription = null, tint = scheme.tertiary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Adjust Student Points (Audit Logged)", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                    }
                    if (profile == null) {
                        Text(
                            text = "This student has not signed into /app yet. Points and badges can be adjusted once their profile is initialized.",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    } else {
                        OutlinedTextField(
                            value = pointsDeltaInput,
                            onValueChange = { pointsDeltaInput = it },
                            label = { Text("Points Delta (e.g. 50 or -25)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("points_delta_input")
                        )
                        OutlinedTextField(
                            value = pointsReasonInput,
                            onValueChange = { pointsReasonInput = it },
                            label = { Text("Mandatory Reason for Points Adjustment") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("points_reason_input")
                        )
                        Button(
                            onClick = {
                                val delta = pointsDeltaInput.trim().toIntOrNull() ?: 0
                                onAdjustPoints(profile, delta, pointsReasonInput)
                                if (delta != 0 && pointsReasonInput.trim().length >= 3) {
                                    pointsDeltaInput = ""
                                    pointsReasonInput = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("apply_points_adjustment_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Apply Points Adjustment")
                        }
                    }
                }
            }
        }

        // Achievements & Badges Review
        if (profile != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp).border(1.dp, scheme.outline, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = scheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Achievements & Badges (${profile.badges.size} Unlocked)", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                        }
                        CurriculumCatalog.badges.forEach { badge ->
                            val hasBadge = profile.badges.contains(badge.id)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(badge.titleEn, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                                    Text(badge.descriptionEn, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                                }
                                OutlinedButton(
                                    onClick = {
                                        onToggleBadge(profile, badge.id, !hasBadge, "Reviewed in Main Admin")
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(if (hasBadge) "Revoke" else "Grant")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Student Quiz & Game Performance History
        item {
            Text(
                text = "Quiz & Brain Game History (${studentQuizResults.size})",
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onBackground,
                modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp)
            )
        }

        if (studentQuizResults.isEmpty()) {
            item {
                Text(
                    text = "No quiz or game attempts recorded yet for this student.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
        } else {
            items(studentQuizResults.take(25), key = { it.id.ifBlank { it.hashCode().toString() } }) { res ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = scheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 860.dp)
                        .border(1.dp, scheme.outline, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "${res.activityType.uppercase()}  •  ${res.title}",
                            style = MaterialTheme.typography.titleSmall,
                            color = scheme.onSurface
                        )
                        Text(
                            text = "Correct: ${res.correctAnswers}/${res.totalQuestions} (${res.accuracyPercent}%)  •  XP: +${res.xpEarned}  •  Date: ${res.dateKey}",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentEditOrApproveModal(
    initial: AllowedStudent?,
    onDismiss: () -> Unit,
    onSave: (
        email: String,
        name: String,
        studentClass: Int,
        lang: String,
        active: Boolean,
        method: String,
        startMs: Long,
        endMs: Long,
        notes: String
    ) -> Unit
) {
    val nowMs = System.currentTimeMillis()
    val ninetyDaysMs = 90L * 24L * 60L * 60L * 1000L

    var email by remember { mutableStateOf(initial?.email.orEmpty()) }
    var name by remember { mutableStateOf(initial?.studentName.orEmpty()) }
    var classText by remember { mutableStateOf((initial?.studentClass ?: 10L).toString()) }
    var lang by remember { mutableStateOf(initial?.preferredLanguage ?: "en") }
    var method by remember { mutableStateOf(initial?.paymentMethod ?: "upi") }
    var active by remember { mutableStateOf(initial?.subscriptionActive ?: true) }
    var durationDaysText by remember { mutableStateOf("90") }
    var notes by remember { mutableStateOf(initial?.adminNotes.orEmpty()) }
    var sendSetupLinkOnSave by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Create Student Account" else "Edit Student Account") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (initial == null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Zero-Trust Free-Tier Account Creation",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "To preserve the free Firebase Spark Plan without exposing service accounts or plaintext passwords, saving this profile authorizes the student's email and sends a secure Firebase password setup link so the student can set their initial password.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { if (initial == null) email = it },
                    enabled = initial == null,
                    label = { Text("Student Email Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_student_email_field")
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Student Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_student_name_field")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = classText,
                        onValueChange = { classText = it },
                        label = { Text("Class (1-12)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = durationDaysText,
                        onValueChange = { durationDaysText = it },
                        label = { Text("Valid Days") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = lang == "en", onClick = { lang = "en" }, label = { Text("English") })
                    FilterChip(selected = lang == "hi", onClick = { lang = "hi" }, label = { Text("Hindi") })
                    FilterChip(selected = method == "upi", onClick = { method = "upi" }, label = { Text("UPI") })
                    FilterChip(selected = method == "cash", onClick = { method = "cash" }, label = { Text("Cash") })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Activate Rs. 50 Pass Now")
                    Switch(checked = active, onCheckedChange = { active = it })
                }

                if (initial == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Send Firebase Password Setup Link", style = MaterialTheme.typography.bodySmall)
                        Switch(checked = sendSetupLinkOnSave, onCheckedChange = { sendSetupLinkOnSave = it })
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Internal Admin / School Notes") },
                    placeholder = { Text("e.g. Model School, Fee paid via UPI") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cls = classText.toIntOrNull()?.coerceIn(1, 12) ?: 10
                    val days = durationDaysText.toLongOrNull()?.coerceIn(1L, 3650L) ?: 90L
                    val start = initial?.subscriptionStartMs?.takeIf { it > 0L } ?: nowMs
                    val end = if (active) nowMs + (days * 24L * 60L * 60L * 1000L) else (initial?.subscriptionEndMs ?: (nowMs + ninetyDaysMs))
                    onSave(email, name, cls, lang, active, method, start, end, notes)
                    if (initial == null && sendSetupLinkOnSave && email.isNotBlank()) {
                        com.google.firebase.Firebase.auth.sendPasswordResetEmail(email.trim().lowercase())
                    }
                },
                modifier = Modifier.testTag("admin_save_student_btn")
            ) {
                Text(if (initial == null) "Create & Authorize Student" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
