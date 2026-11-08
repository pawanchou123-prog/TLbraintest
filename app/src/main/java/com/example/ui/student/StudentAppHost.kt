package com.example.ui.student

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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SportsEsports
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
import com.example.R
import com.example.data.CurriculumCatalog
import com.example.data.SamplePreviewCatalog
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.TlBrainRepository
import com.example.core.audio.SoundEffect
import com.example.core.audio.SoundManager
import com.example.navigation.StudentRoutes
import com.example.ui.auth.StudentAuthManager
import com.example.ui.games.BrainGamesArenaScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Standalone Root Host for Application 1: STUDENT USER APPLICATION (/app).
 * Completely isolated from /admin and /question-manager layouts, logins, and navigation.
 */
@Composable
fun StudentAppRoot() {
    val context = LocalContext.current
    val authFlow = remember { StudentAuthManager.observeAuthState() }
    val currentUser by authFlow.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    var splashDone by rememberSaveable { mutableStateOf(false) }
    var termsAccepted by rememberSaveable {
        mutableStateOf(StudentAuthManager.hasAcceptedTermsLocally(context))
    }

    // 1. Splash Screen
    if (!splashDone) {
        StudentSplashScreen(onSplashFinished = { splashDone = true })
        return
    }

    // 2. First-Login / First-Launch Terms & Conditions Gate
    if (!termsAccepted) {
        StudentTermsScreen(
            isFirstLaunchGate = true,
            onAcceptTerms = {
                StudentAuthManager.saveTermsAcceptedLocally(context, true)
                termsAccepted = true
            }
        )
        return
    }

    // 3. Dedicated Student Login Screen (/app/login)
    val user = currentUser
    if (user == null) {
        StudentLoginScreen(onSignInCompleted = { /* AuthStateListener reacts automatically */ })
        return
    }

    // 4. Authenticated Student Session Gate + /app Navigation Host
    AuthorizedStudentSessionHost(user = user)
}

@Composable
private fun AuthorizedStudentSessionHost(user: FirebaseUser) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()

    val studentViewModel: StudentViewModel = viewModel(
        key = "student_app_${user.uid}",
        factory = viewModelFactory {
            initializer {
                val dbId = appContext.getString(R.string.firestore_database_id)
                val firestore = FirebaseFirestore.getInstance(dbId)
                StudentViewModel(
                    appContext = appContext,
                    repository = TlBrainRepository(firestore),
                    currentUserId = user.uid,
                    currentUserEmail = user.email.orEmpty(),
                    currentDisplayName = user.displayName.orEmpty()
                )
            }
        }
    )

    val accessState by studentViewModel.accessState.collectAsStateWithLifecycle()
    val appConfig by studentViewModel.appConfig.collectAsStateWithLifecycle()
    val allowedStudent by studentViewModel.allowedStudent.collectAsStateWithLifecycle()
    val profileState by studentViewModel.userProfileState.collectAsStateWithLifecycle()
    val publishedQuestions by studentViewModel.publishedQuestions.collectAsStateWithLifecycle()
    val studentResults by studentViewModel.studentResults.collectAsStateWithLifecycle()
    val leaderboardEntries by studentViewModel.leaderboardEntries.collectAsStateWithLifecycle()
    val publishedWinners by studentViewModel.publishedWinners.collectAsStateWithLifecycle()
    val activeBanners by studentViewModel.activeBanners.collectAsStateWithLifecycle()
    val activePrizes by studentViewModel.activePrizes.collectAsStateWithLifecycle()
    val activeAnnouncements by studentViewModel.activeAnnouncements.collectAsStateWithLifecycle()
    val supportMessages by studentViewModel.supportMessages.collectAsStateWithLifecycle()
    val activeQuizLaunch by studentViewModel.activeQuizLaunch.collectAsStateWithLifecycle()
    val statusMessage by studentViewModel.statusMessage.collectAsStateWithLifecycle()
    val readNotificationIds by studentViewModel.readNotificationIds.collectAsStateWithLifecycle()

    val handleLogout: () -> Unit = {
        SoundManager.play(SoundEffect.LOGOUT)
        StudentAuthManager.signOutStudentSession(context, scope)
    }

    when (val gate = accessState) {
        is StudentAccessState.Checking -> {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Verifying Approved Student Account...",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        is StudentAccessState.Denied -> {
            StudentAccessDeniedScreen(
                email = user.email.orEmpty(),
                reasonTitle = gate.reasonTitle,
                reasonDetail = gate.reasonDetail,
                allowedRecord = gate.allowedRecord ?: allowedStudent,
                appConfig = appConfig,
                onRetryCheck = { studentViewModel.verifyStudentAuthorization() },
                onSignOut = handleLogout,
                onSubmitPendingRequest = { name, cls, method, ref, cb ->
                    studentViewModel.submitAccessOrPaymentRequest(name, cls, method, ref, cb)
                }
            )
        }

        is StudentAccessState.Approved -> {
            val profile = (profileState as? UiState.Success)?.data ?: UserProfile(
                id = user.uid,
                userId = user.uid,
                email = user.email.orEmpty(),
                displayName = gate.allowedRecord?.studentName?.ifBlank { user.displayName.orEmpty() }
                    ?: user.displayName.orEmpty().ifBlank { "Student" },
                studentClass = gate.allowedRecord?.studentClass ?: 10L,
                language = gate.allowedRecord?.preferredLanguage ?: "en"
            )

            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            val navigateToPrimaryTab: (String) -> Unit = { route ->
                if (StudentRoutes.isAllowedStudentNamespace(route)) {
                    SoundManager.play(SoundEffect.TAB_CHANGE)
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }

            val isHi = profile.language == "hi"
            val navTabs = listOf(
                Triple(StudentRoutes.DASHBOARD, if (isHi) "होम" else "Home", Icons.Default.Dashboard),
                Triple(StudentRoutes.QUIZ_HUB, if (isHi) "क्विज़" else "Quiz", Icons.AutoMirrored.Filled.MenuBook),
                Triple(StudentRoutes.GAMES_HUB, if (isHi) "गेम्स" else "Games", Icons.Default.SportsEsports),
                Triple(StudentRoutes.LEADERBOARD, if (isHi) "रैंक" else "Ranks", Icons.Default.EmojiEvents),
                Triple(StudentRoutes.PROFILE, if (isHi) "प्रोफ़ाइल" else "Profile", Icons.Default.Person)
            )

            val noticesPool = if (activeAnnouncements.isNotEmpty()) activeAnnouncements else SamplePreviewCatalog.sampleNotifications
            val unreadNoticesCount = noticesPool.count { !readNotificationIds.contains(it.id) }

            val scheme = MaterialTheme.colorScheme
            val showChromeBars = StudentRoutes.isPrimaryTabRoute(currentRoute)

            Surface(modifier = Modifier.fillMaxSize(), color = scheme.background) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (showChromeBars) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(scheme.surface)
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = stringResource(R.string.cd_brand_icon),
                                    tint = scheme.secondary,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.brand_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = scheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.brand_tagline),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = scheme.secondary
                                    )
                                }
                            }
                            IconButton(
                                onClick = handleLogout,
                                modifier = Modifier.testTag("student_top_logout_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Logout",
                                    tint = scheme.onSurfaceVariant
                                )
                            }
                        }
                        HorizontalDivider(color = scheme.outline.copy(alpha = 0.45f))
                    }

                    if (!statusMessage.isNullOrBlank()) {
                        Surface(
                            color = scheme.primaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .border(1.dp, scheme.outline, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = statusMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { studentViewModel.clearStatusMessage() },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        val isExpanded = maxWidth >= 600.dp

                        val navGraphContent: @Composable (Modifier) -> Unit = { mod ->
                            NavHost(
                                navController = navController,
                                startDestination = StudentRoutes.DASHBOARD,
                                modifier = mod.fillMaxSize()
                            ) {
                                composable(StudentRoutes.DASHBOARD) {
                                    StudentDashboardScreen(
                                        profile = profile,
                                        appConfig = appConfig,
                                        banners = activeBanners,
                                        recentResults = studentResults,
                                        unreadNoticesCount = unreadNoticesCount,
                                        onOpenQuizSection = { navigateToPrimaryTab(StudentRoutes.QUIZ_HUB) },
                                        onLaunchGkQuiz = {
                                            studentViewModel.prepareCategoryOrSubjectQuiz(
                                                categoryOrSubject = "General Knowledge",
                                                studentClass = profile.studentClass.toInt().coerceIn(1, 12),
                                                language = profile.language,
                                                isGkMode = true
                                            )
                                            navController.navigate(StudentRoutes.QUIZ_PLAY)
                                        },
                                        onOpenGamesSection = { navigateToPrimaryTab(StudentRoutes.GAMES_HUB) },
                                        onOpenLeaderboard = { navigateToPrimaryTab(StudentRoutes.LEADERBOARD) },
                                        onOpenProfile = { navigateToPrimaryTab(StudentRoutes.PROFILE) },
                                        onOpenNotifications = { navController.navigate(StudentRoutes.NOTIFICATIONS) }
                                    )
                                }

                                composable(StudentRoutes.QUIZ_HUB) {
                                    StudentQuizHubScreen(
                                        profile = profile,
                                        publishedQuestions = publishedQuestions,
                                        recentResults = studentResults,
                                        onStartCategoryOrSubjectQuiz = { categoryOrSubj, sClass, lang, isGk ->
                                            studentViewModel.prepareCategoryOrSubjectQuiz(
                                                categoryOrSubject = categoryOrSubj,
                                                studentClass = sClass,
                                                language = lang,
                                                isGkMode = isGk
                                            )
                                            navController.navigate(StudentRoutes.QUIZ_PLAY)
                                        }
                                    )
                                }

                                composable(StudentRoutes.QUIZ_PLAY) {
                                    val quizSession = activeQuizLaunch
                                    if (quizSession == null) {
                                        navController.popBackStack()
                                    } else {
                                        QuizEngineScreen(
                                            quizTitle = quizSession.quizTitle,
                                            isGkMode = quizSession.isGkMode,
                                            studentClass = quizSession.studentClass,
                                            questions = quizSession.questions,
                                            isSamplePreviewData = quizSession.isSamplePreviewData,
                                            onCompleteQuiz = { type, title, sClass, score, total, correct, xp, duration, attempts ->
                                                studentViewModel.submitActivityResult(
                                                    currentProfile = profile,
                                                    activityType = type,
                                                    title = title,
                                                    studentClass = sClass,
                                                    score = score,
                                                    totalQuestions = total,
                                                    correctAnswers = correct,
                                                    xpEarned = xp,
                                                    durationSeconds = duration,
                                                    questionAttempts = attempts
                                                )
                                            },
                                            onExit = {
                                                studentViewModel.clearActiveQuiz()
                                                navController.popBackStack()
                                            }
                                        )
                                    }
                                }

                                composable(StudentRoutes.GAMES_HUB) {
                                    StudentGamesHubScreen(
                                        profile = profile,
                                        onOpenPlayableGame = { game ->
                                            navController.navigate(StudentRoutes.gamePlayRoute(game.id))
                                        }
                                    )
                                }

                                composable(
                                    route = StudentRoutes.GAME_PLAY,
                                    arguments = listOf(navArgument("gameId") { type = NavType.StringType })
                                ) { backStackEntry ->
                                    val gameId = backStackEntry.arguments?.getString("gameId").orEmpty()
                                    val gameItem = CurriculumCatalog.games.firstOrNull { it.id == gameId }
                                        ?: CurriculumCatalog.games.first()
                                    BrainGamesArenaScreen(
                                        game = gameItem,
                                        studentClass = profile.studentClass.toInt().coerceIn(1, 12),
                                        language = profile.language,
                                        soundEnabled = profile.soundEnabled,
                                        onCompleteGame = { score, totalRounds, correctRounds, xpEarned, durationSec ->
                                            studentViewModel.submitActivityResult(
                                                currentProfile = profile,
                                                activityType = "game",
                                                title = gameItem.titleEn,
                                                studentClass = profile.studentClass.toInt().coerceIn(1, 12),
                                                score = score,
                                                totalQuestions = totalRounds,
                                                correctAnswers = correctRounds,
                                                xpEarned = xpEarned,
                                                durationSeconds = durationSec
                                            )
                                        },
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable(StudentRoutes.LEADERBOARD) {
                                    StudentLeaderboardTab(
                                        currentUserId = user.uid,
                                        appConfig = appConfig,
                                        entries = leaderboardEntries,
                                        prizes = activePrizes,
                                        winners = publishedWinners
                                    )
                                }

                                composable(StudentRoutes.PROFILE) {
                                    StudentProfileSectionScreen(
                                        profile = profile,
                                        allowedStudent = gate.allowedRecord ?: allowedStudent,
                                        isOwnerTestAccount = gate.isOwnerTestAccount,
                                        appConfig = appConfig,
                                        onSaveProfile = { name, sClass, school, avatar, lang, sound ->
                                            studentViewModel.updateStudentProfile(
                                                currentProfile = profile,
                                                displayName = name,
                                                studentClass = sClass,
                                                schoolName = school,
                                                avatarId = avatar,
                                                language = lang,
                                                soundEnabled = sound
                                            )
                                        },
                                        onOpenSubscriptionPage = { navController.navigate(StudentRoutes.SUBSCRIPTION) },
                                        onOpenSupportPage = { navController.navigate(StudentRoutes.SUPPORT) },
                                        onOpenNotificationsPage = { navController.navigate(StudentRoutes.NOTIFICATIONS) },
                                        onOpenTermsPage = { navController.navigate(StudentRoutes.TERMS_VIEW) },
                                        onLogout = handleLogout
                                    )
                                }

                                composable(StudentRoutes.SUBSCRIPTION) {
                                    StudentSubscriptionScreen(
                                        profile = profile,
                                        allowedStudent = gate.allowedRecord ?: allowedStudent,
                                        isOwnerTestAccount = gate.isOwnerTestAccount,
                                        appConfig = appConfig,
                                        onOpenSupportChat = { navController.navigate(StudentRoutes.SUPPORT) },
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable(StudentRoutes.SUPPORT) {
                                    StudentCustomerSupportScreen(
                                        profile = profile,
                                        messages = supportMessages,
                                        onSendMessage = { msg -> studentViewModel.sendSupportMessage(profile, msg) },
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable(StudentRoutes.NOTIFICATIONS) {
                                    StudentNotificationsScreen(
                                        profile = profile,
                                        liveAnnouncements = activeAnnouncements,
                                        readIds = readNotificationIds,
                                        onMarkRead = { id -> studentViewModel.markNotificationRead(id) },
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable(StudentRoutes.TERMS_VIEW) {
                                    val customTerms = if (profile.language == "hi") {
                                        appConfig.termsAndConditionsHi
                                    } else {
                                        appConfig.termsAndConditionsEn
                                    }
                                    StudentTermsScreen(
                                        isFirstLaunchGate = false,
                                        language = profile.language,
                                        customTermsText = customTerms,
                                        onAcceptTerms = {},
                                        onBackFromReadOnly = { navController.popBackStack() }
                                    )
                                }
                            }
                        }

                        if (isExpanded && showChromeBars) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                NavigationRail(
                                    containerColor = scheme.surface,
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    navTabs.forEach { (route, label, icon) ->
                                        NavigationRailItem(
                                            selected = currentRoute == route,
                                            onClick = { navigateToPrimaryTab(route) },
                                            icon = { Icon(icon, contentDescription = label) },
                                            label = { Text(label) },
                                            modifier = Modifier.testTag("nav_${label.lowercase()}")
                                        )
                                    }
                                }
                                navGraphContent(Modifier.weight(1f))
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                navGraphContent(Modifier.weight(1f))
                                if (showChromeBars) {
                                    NavigationBar(
                                        containerColor = scheme.surface,
                                        modifier = Modifier.navigationBarsPadding()
                                    ) {
                                        navTabs.forEach { (route, label, icon) ->
                                            NavigationBarItem(
                                                selected = currentRoute == route,
                                                onClick = { navigateToPrimaryTab(route) },
                                                icon = { Icon(icon, contentDescription = label) },
                                                label = { Text(label, maxLines = 1) },
                                                modifier = Modifier.testTag("nav_${label.lowercase()}")
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
