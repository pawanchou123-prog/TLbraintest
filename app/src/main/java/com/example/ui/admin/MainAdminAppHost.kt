package com.example.ui.admin

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.MainActivity
import com.example.R
import com.example.data.model.sanitizeEmailKey
import com.example.data.repository.AdminRepository
import com.example.navigation.AdminRoutes
import com.example.ui.auth.AdminAuthManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Standalone Root Host for Application 2: MAIN ADMIN APPLICATION (/admin).
 * Completely separate layout, login interface, role-based permissions, and routing
 * from Application 1 (/app) and reserved Application 3 (/question-manager).
 */
@Composable
fun MainAdminAppRoot() {
    val context = LocalContext.current
    val authFlow = remember { AdminAuthManager.observeAdminAuthState() }
    val currentUser by authFlow.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    val openStudentApp: () -> Unit = {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        context.startActivity(intent)
    }

    val user = currentUser
    if (user == null) {
        AdminLoginScreen(
            onSignInCompleted = { /* AuthStateListener reacts automatically */ },
            onOpenStudentApp = openStudentApp
        )
        return
    }

    AuthorizedMainAdminSessionHost(
        user = user,
        onOpenStudentApp = openStudentApp
    )
}

@Composable
private fun AuthorizedMainAdminSessionHost(
    user: FirebaseUser,
    onOpenStudentApp: () -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()

    val adminViewModel: MainAdminViewModel = viewModel(
        key = "main_admin_${user.uid}",
        factory = viewModelFactory {
            initializer {
                val dbId = appContext.getString(R.string.firestore_database_id)
                val firestore = FirebaseFirestore.getInstance(dbId)
                MainAdminViewModel(
                    repository = AdminRepository(firestore),
                    currentAdminUid = user.uid,
                    currentAdminEmail = user.email.orEmpty(),
                    currentAdminName = user.displayName.orEmpty(),
                    isEmailVerified = user.isEmailVerified
                )
            }
        }
    )

    val accessState by adminViewModel.accessState.collectAsStateWithLifecycle()
    val feedback by adminViewModel.feedback.collectAsStateWithLifecycle()
    val stats by adminViewModel.dashboardStats.collectAsStateWithLifecycle()
    val allowedStudents by adminViewModel.allowedStudents.collectAsStateWithLifecycle()
    val userProfiles by adminViewModel.userProfiles.collectAsStateWithLifecycle()
    val subscriptionRecords by adminViewModel.subscriptionRecords.collectAsStateWithLifecycle()
    val quizResults by adminViewModel.quizResults.collectAsStateWithLifecycle()
    val leaderboardEntries by adminViewModel.leaderboardEntries.collectAsStateWithLifecycle()
    val winnersHistory by adminViewModel.winnersHistory.collectAsStateWithLifecycle()
    val banners by adminViewModel.banners.collectAsStateWithLifecycle()
    val prizes by adminViewModel.prizes.collectAsStateWithLifecycle()
    val announcements by adminViewModel.announcements.collectAsStateWithLifecycle()
    val supportThreads by adminViewModel.supportThreads.collectAsStateWithLifecycle()
    val appConfig by adminViewModel.appConfig.collectAsStateWithLifecycle()
    val auditLogs by adminViewModel.auditLogs.collectAsStateWithLifecycle()

    val handleLogout: () -> Unit = {
        AdminAuthManager.signOutAdminSession(context, scope)
    }

    when (val gate = accessState) {
        is AdminAccessState.Checking -> {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Verifying Main Admin Security Rules...",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        is AdminAccessState.Denied -> {
            AdminAccessDeniedScreen(
                email = gate.email,
                reasonTitle = gate.reasonTitle,
                reasonDetail = gate.reasonDetail,
                onRetryVerification = { adminViewModel.verifyMainAdminAccess() },
                onSignOut = handleLogout,
                onOpenStudentApp = onOpenStudentApp
            )
        }

        is AdminAccessState.Authorized -> {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            val navigateToAdminTab: (String) -> Unit = { route ->
                if (AdminRoutes.isAllowedAdminNamespace(route)) {
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }

            val navTabs = listOf(
                Triple(AdminRoutes.DASHBOARD, "Dash", Icons.Default.Dashboard),
                Triple(AdminRoutes.STUDENTS, "Students", Icons.Default.Group),
                Triple(AdminRoutes.SUBSCRIPTIONS, "Passes", Icons.Default.CardMembership),
                Triple(AdminRoutes.CONTENT, "Content", Icons.Default.Campaign),
                Triple(AdminRoutes.LEADERBOARDS, "Ranks", Icons.Default.EmojiEvents),
                Triple(AdminRoutes.SUPPORT, "Support", Icons.Default.SupportAgent)
            )

            val scheme = MaterialTheme.colorScheme
            val showChrome = AdminRoutes.isPrimaryTabRoute(currentRoute)

            Surface(modifier = Modifier.fillMaxSize(), color = scheme.background) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (showChrome) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(scheme.surface)
                                .statusBarsPadding()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = "Main Admin",
                                    tint = scheme.tertiary,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "TL BRAIN TEST – MAIN ADMIN",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = scheme.onSurface
                                    )
                                    Text(
                                        text = "/admin  •  ${gate.roleMember.email}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = scheme.secondary
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { navController.navigate(AdminRoutes.AUDIT_LOGS) },
                                    modifier = Modifier.testTag("admin_top_audit_btn")
                                ) {
                                    Icon(Icons.Default.History, contentDescription = "Audit Logs", tint = scheme.secondary)
                                }
                                IconButton(
                                    onClick = onOpenStudentApp,
                                    modifier = Modifier.testTag("admin_top_open_student_btn")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open Student App", tint = scheme.onSurfaceVariant)
                                }
                                IconButton(
                                    onClick = handleLogout,
                                    modifier = Modifier.testTag("admin_top_logout_btn")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = scheme.onSurfaceVariant)
                                }
                            }
                        }
                        HorizontalDivider(color = scheme.outline.copy(alpha = 0.45f))
                    }

                    val activeFeedback = feedback
                    if (activeFeedback != null) {
                        Surface(
                            color = if (activeFeedback.isError) scheme.errorContainer else scheme.primaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .border(
                                    width = 1.dp,
                                    color = if (activeFeedback.isError) scheme.error else scheme.outline,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeFeedback.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (activeFeedback.isError) scheme.onErrorContainer else scheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { adminViewModel.clearFeedback() },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        val isExpanded = maxWidth >= 640.dp

                        val navGraphContent: @Composable (Modifier) -> Unit = { mod ->
                            NavHost(
                                navController = navController,
                                startDestination = AdminRoutes.DASHBOARD,
                                modifier = mod.fillMaxSize()
                            ) {
                                composable(AdminRoutes.DASHBOARD) {
                                    AdminDashboardScreen(
                                        adminEmail = gate.roleMember.email,
                                        stats = stats,
                                        recentLogs = auditLogs,
                                        onOpenStudents = { navigateToAdminTab(AdminRoutes.STUDENTS) },
                                        onOpenSubscriptions = { navigateToAdminTab(AdminRoutes.SUBSCRIPTIONS) },
                                        onOpenContent = { navigateToAdminTab(AdminRoutes.CONTENT) },
                                        onOpenLeaderboards = { navigateToAdminTab(AdminRoutes.LEADERBOARDS) },
                                        onOpenSupport = { navigateToAdminTab(AdminRoutes.SUPPORT) },
                                        onOpenAuditLogs = { navController.navigate(AdminRoutes.AUDIT_LOGS) }
                                    )
                                }

                                composable(AdminRoutes.STUDENTS) {
                                    val nowMs = System.currentTimeMillis()
                                    val ninetyDaysMs = 90L * 24L * 60L * 60L * 1000L
                                    AdminStudentsListScreen(
                                        students = allowedStudents,
                                        profiles = userProfiles,
                                        pendingRequests = subscriptionRecords,
                                        onOpenStudentDetail = { emailKey ->
                                            navController.navigate(AdminRoutes.studentDetailRoute(emailKey))
                                        },
                                        onSaveOrApproveStudent = { email, name, cls, lang, active, method, start, end, notes ->
                                            adminViewModel.saveOrApproveStudentAccount(email, name, cls, lang, active, method, start, end, notes)
                                        },
                                        onReviewRequest = { req, approve, notes ->
                                            adminViewModel.reviewPendingSubscriptionRequest(
                                                record = req,
                                                approve = approve,
                                                paymentMethod = if (req.paymentMethod == "cash") "cash" else "upi",
                                                startMs = nowMs,
                                                endMs = nowMs + ninetyDaysMs,
                                                verificationNotes = notes
                                            )
                                        }
                                    )
                                }

                                composable(
                                    route = AdminRoutes.STUDENT_DETAIL,
                                    arguments = listOf(navArgument("emailKey") { type = NavType.StringType })
                                ) { entry ->
                                    val emailKey = entry.arguments?.getString("emailKey").orEmpty()
                                    val student = allowedStudents.firstOrNull {
                                        it.id == emailKey || sanitizeEmailKey(it.email) == emailKey
                                    }
                                    val profile = student?.let { s ->
                                        userProfiles.firstOrNull {
                                            it.email.equals(s.email, ignoreCase = true) ||
                                                (s.linkedUid != null && it.userId == s.linkedUid)
                                        }
                                    }
                                    val studentHistory = profile?.let { p ->
                                        quizResults.filter { it.userId == p.userId.ifBlank { p.id } }
                                    }.orEmpty()

                                    AdminStudentDetailScreen(
                                        student = student,
                                        profile = profile,
                                        studentQuizResults = studentHistory,
                                        onBack = { navController.popBackStack() },
                                        onUpdateStudentAccount = { email, name, cls, lang, active, method, start, end, notes ->
                                            adminViewModel.saveOrApproveStudentAccount(email, name, cls, lang, active, method, start, end, notes)
                                        },
                                        onLifecycleAction = { s, action, reason ->
                                            adminViewModel.updateStudentLifecycle(s, action, reason)
                                        },
                                        onAdjustPoints = { p, delta, reason ->
                                            adminViewModel.adjustStudentPoints(p, delta, reason)
                                        },
                                        onToggleBadge = { p, badgeId, grant, reason ->
                                            adminViewModel.toggleStudentBadge(p, badgeId, grant, reason)
                                        }
                                    )
                                }

                                composable(AdminRoutes.SUBSCRIPTIONS) {
                                    AdminSubscriptionsScreen(
                                        students = allowedStudents,
                                        records = subscriptionRecords,
                                        onReviewRequest = { rec, approve, method, sMs, eMs, notes ->
                                            adminViewModel.reviewPendingSubscriptionRequest(rec, approve, method, sMs, eMs, notes)
                                        },
                                        onRecordDirectPayment = { email, name, cls, method, sMs, eMs, ref, notes ->
                                            adminViewModel.recordDirectManualPayment(email, name, cls, method, sMs, eMs, ref, notes)
                                        },
                                        onExtendOrExpire = { s, sMs, eMs, active, method, notes ->
                                            adminViewModel.extendOrExpireStudentSubscription(s, sMs, eMs, active, method, notes)
                                        }
                                    )
                                }

                                composable(AdminRoutes.CONTENT) {
                                    AdminContentScreen(
                                        appConfig = appConfig,
                                        announcements = announcements,
                                        banners = banners,
                                        prizes = prizes,
                                        onSaveConfig = { cfg, summary -> adminViewModel.saveGlobalConfig(cfg, summary) },
                                        onSaveAnnouncement = { id, title, body, cat, cls, active ->
                                            adminViewModel.saveAnnouncementItem(id, title, body, cat, cls, active)
                                        },
                                        onDeleteAnnouncement = { id, title -> adminViewModel.deleteAnnouncementItem(id, title) },
                                        onSaveBanner = { id, title, sub, img, act, active ->
                                            adminViewModel.saveBannerItem(id, title, sub, img, act, active)
                                        },
                                        onDeleteBanner = { id, title -> adminViewModel.deleteBannerItem(id, title) },
                                        onSavePrize = { id, rank, title, desc, img, active ->
                                            adminViewModel.savePrizeItem(id, rank, title, desc, img, active)
                                        },
                                        onDeletePrize = { id, title -> adminViewModel.deletePrizeItem(id, title) }
                                    )
                                }

                                composable(AdminRoutes.LEADERBOARDS) {
                                    AdminLeaderboardsScreen(
                                        appConfig = appConfig,
                                        entries = leaderboardEntries,
                                        winners = winnersHistory,
                                        onSaveConfig = { cfg, summary -> adminViewModel.saveGlobalConfig(cfg, summary) },
                                        onArchiveAndResetMonthly = { monthLabel, prize ->
                                            adminViewModel.archiveWinnersAndResetMonthlyPoints(monthLabel, prize)
                                        },
                                        onAddManualWinner = { m, n, c, r, xp, p ->
                                            adminViewModel.addManualPreviousWinner(m, n, c, r, xp, p)
                                        },
                                        onToggleWinnerVisibility = { w, pub ->
                                            adminViewModel.toggleWinnerPublished(w, pub)
                                        }
                                    )
                                }

                                composable(AdminRoutes.SUPPORT) {
                                    AdminSupportInboxScreen(
                                        threads = supportThreads,
                                        onOpenThread = { uid ->
                                            navController.navigate(AdminRoutes.supportThreadRoute(uid))
                                        },
                                        onToggleUnread = { uid, unread ->
                                            adminViewModel.markSupportThreadReadState(uid, unread)
                                        }
                                    )
                                }

                                composable(
                                    route = AdminRoutes.SUPPORT_THREAD,
                                    arguments = listOf(navArgument("studentUid") { type = NavType.StringType })
                                ) { entry ->
                                    val studentUid = entry.arguments?.getString("studentUid").orEmpty()
                                    val thread = supportThreads.firstOrNull {
                                        it.studentUid == studentUid || it.id == studentUid
                                    }
                                    val messagesFlow = remember(studentUid) {
                                        adminViewModel.observeSupportMessagesForStudent(studentUid)
                                    }
                                    val messages by messagesFlow.collectAsStateWithLifecycle(initialValue = emptyList())

                                    AdminSupportThreadScreen(
                                        thread = thread,
                                        messages = messages,
                                        onMarkRead = { adminViewModel.markSupportThreadReadState(studentUid, false) },
                                        onSendReply = { t, txt -> adminViewModel.sendAdminSupportReply(t, txt) },
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable(AdminRoutes.AUDIT_LOGS) {
                                    AdminAuditLogsScreen(
                                        logs = auditLogs,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                            }
                        }

                        if (isExpanded && showChrome) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                NavigationRail(
                                    containerColor = scheme.surface,
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    navTabs.forEach { (route, label, icon) ->
                                        NavigationRailItem(
                                            selected = currentRoute == route,
                                            onClick = { navigateToAdminTab(route) },
                                            icon = { Icon(icon, contentDescription = label) },
                                            label = { Text(label) },
                                            modifier = Modifier.testTag("admin_nav_${label.lowercase()}")
                                        )
                                    }
                                }
                                navGraphContent(Modifier.weight(1f))
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                navGraphContent(Modifier.weight(1f))
                                if (showChrome) {
                                    NavigationBar(
                                        containerColor = scheme.surface,
                                        modifier = Modifier.navigationBarsPadding()
                                    ) {
                                        navTabs.forEach { (route, label, icon) ->
                                            NavigationBarItem(
                                                selected = currentRoute == route,
                                                onClick = { navigateToAdminTab(route) },
                                                icon = { Icon(icon, contentDescription = label) },
                                                label = { Text(label, maxLines = 1) },
                                                modifier = Modifier.testTag("admin_nav_${label.lowercase()}")
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
