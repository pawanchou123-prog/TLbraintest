package com.example.ui.questionmanager

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.AdminActivity
import com.example.MainActivity
import com.example.data.repository.QuestionManagerOperatorSession
import com.example.data.repository.QuestionManagerRepository
import com.example.navigation.QuestionManagerRoutes
import com.example.ui.auth.QuestionManagerAuthManager

private data class QmNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val tag: String
)

@Composable
fun QuestionManagerAppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by QuestionManagerAuthManager.observeAuthState().collectAsStateWithLifecycle(initialValue = null)

    val launchStudentApp = {
        context.startActivity(Intent(context, MainActivity::class.java))
    }
    val launchAdminApp = {
        context.startActivity(Intent(context, AdminActivity::class.java))
    }

    if (currentUser == null) {
        QuestionManagerLoginScreen(
            onSignInCompleted = { /* AuthStateListener refreshes automatically */ },
            onOpenStudentApp = launchStudentApp,
            onOpenAdminApp = launchAdminApp
        )
        return
    }

    val activeUser = currentUser!!
    val vmKey = "qm_vm_${activeUser.uid}"
    val viewModel: QuestionManagerViewModel = viewModel(
        key = vmKey,
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return QuestionManagerViewModel(
                    repository = QuestionManagerRepository(context.applicationContext),
                    currentUserId = activeUser.uid,
                    currentUserEmail = activeUser.email.orEmpty(),
                    currentDisplayName = activeUser.displayName.orEmpty(),
                    isEmailVerified = activeUser.isEmailVerified
                ) as T
            }
        }
    )

    val accessState by viewModel.accessState.collectAsStateWithLifecycle()

    when (val state = accessState) {
        is OperatorAccessState.Checking -> {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(
                            text = "Verifying Question Management Role...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }
        }
        is OperatorAccessState.Denied -> {
            QuestionManagerAccessDeniedScreen(
                email = activeUser.email.orEmpty(),
                reasonTitle = state.reasonTitle,
                reasonDetail = state.reasonDetail,
                onRetryVerification = { viewModel.verifyAuthorization() },
                onSignOut = { QuestionManagerAuthManager.signOutSession(context, scope) },
                onOpenStudentApp = launchStudentApp
            )
        }
        is OperatorAccessState.Approved -> {
            AuthorizedQuestionManagerWorkspace(
                session = state.session,
                viewModel = viewModel,
                onSignOut = { QuestionManagerAuthManager.signOutSession(context, scope) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthorizedQuestionManagerWorkspace(
    session: QuestionManagerOperatorSession,
    viewModel: QuestionManagerViewModel,
    onSignOut: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var currentRoute by remember { mutableStateOf(QuestionManagerRoutes.DASHBOARD) }

    val navigateSafely = { targetRoute: String ->
        if (QuestionManagerRoutes.isAllowedQuestionManagerNamespace(targetRoute)) {
            currentRoute = targetRoute
        }
    }

    if (currentRoute != QuestionManagerRoutes.DASHBOARD) {
        BackHandler {
            currentRoute = QuestionManagerRoutes.DASHBOARD
        }
    }

    val questions by viewModel.questions.collectAsStateWithLifecycle()
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val subjectCatalog by viewModel.subjectCatalog.collectAsStateWithLifecycle()
    val editingQuestion by viewModel.editingQuestion.collectAsStateWithLifecycle()
    val csvPreview by viewModel.csvPreview.collectAsStateWithLifecycle()
    val aiPrepStatus by viewModel.aiPrepStatus.collectAsStateWithLifecycle()
    val allowedManagers by viewModel.allowedManagers.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    val tabs = remember(session.isMainAdmin) {
        buildList {
            add(QmNavTab(QuestionManagerRoutes.DASHBOARD, "Dashboard", Icons.Default.Dashboard, "qm_nav_dashboard"))
            add(QmNavTab(QuestionManagerRoutes.EDITOR, "Add", Icons.Default.AddCircle, "qm_nav_editor"))
            add(QmNavTab(QuestionManagerRoutes.QUESTION_BANK, "Bank", Icons.AutoMirrored.Filled.LibraryBooks, "qm_nav_bank"))
            add(QmNavTab(QuestionManagerRoutes.IMPORT_EXPORT, "CSV", Icons.Default.UploadFile, "qm_nav_csv"))
            add(QmNavTab(QuestionManagerRoutes.AI_PREP, "AI Prep", Icons.Default.AutoAwesome, "qm_nav_ai"))
            if (session.isMainAdmin) {
                add(QmNavTab(QuestionManagerRoutes.ADMIN_CONTROLS, "Access", Icons.Default.AdminPanelSettings, "qm_nav_admin"))
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useSideRail = maxWidth >= 720.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "TL BRAIN TEST – QUESTION MANAGEMENT",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Owner: PRITAM CHOUDHARY  •  ${if (session.isMainAdmin) "Main Admin" else "Question Manager"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("qm_top_logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign out of Question Management"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = scheme.surface,
                        titleContentColor = scheme.onSurface
                    )
                )
            },
            bottomBar = {
                if (!useSideRail) {
                    NavigationBar(containerColor = scheme.surface) {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = currentRoute == tab.route,
                                onClick = { navigateSafely(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useSideRail) {
                    NavigationRail(
                        containerColor = scheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        tabs.forEach { tab ->
                            NavigationRailItem(
                                selected = currentRoute == tab.route,
                                onClick = { navigateSafely(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (statusBanner != null) {
                        Surface(
                            color = scheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = statusBanner.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { viewModel.clearStatusBanner() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss notification",
                                        tint = scheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (currentRoute) {
                            QuestionManagerRoutes.DASHBOARD -> QuestionDashboardScreen(
                                session = session,
                                stats = stats,
                                onAddNewQuestion = {
                                    viewModel.startEditingQuestion(null)
                                    navigateSafely(QuestionManagerRoutes.EDITOR)
                                },
                                onOpenQuestionBank = { navigateSafely(QuestionManagerRoutes.QUESTION_BANK) },
                                onOpenImportExport = { navigateSafely(QuestionManagerRoutes.IMPORT_EXPORT) },
                                onEditRecentQuestion = { q ->
                                    viewModel.startEditingQuestion(q)
                                    navigateSafely(QuestionManagerRoutes.EDITOR)
                                }
                            )
                            QuestionManagerRoutes.EDITOR -> QuestionEditorScreen(
                                session = session,
                                subjectCatalog = subjectCatalog,
                                editingQuestion = editingQuestion,
                                onClearEditing = { viewModel.startEditingQuestion(null) },
                                onSaveQuestion = { cls, subj, top, lang, qText, opts, corr, expl, diff, pts, tmr, img, action ->
                                    viewModel.saveQuestionFromEditor(
                                        studentClass = cls,
                                        subject = subj,
                                        topic = top,
                                        language = lang,
                                        questionText = qText,
                                        options = opts,
                                        correctIndex = corr,
                                        explanation = expl,
                                        difficulty = diff,
                                        points = pts,
                                        timerSeconds = tmr,
                                        imageUrl = img,
                                        desiredAction = action,
                                        onSavedSuccess = { navigateSafely(QuestionManagerRoutes.QUESTION_BANK) }
                                    )
                                }
                            )
                            QuestionManagerRoutes.QUESTION_BANK -> QuestionBankScreen(
                                session = session,
                                questions = questions,
                                subjectCatalog = subjectCatalog,
                                onEditQuestion = { q ->
                                    viewModel.startEditingQuestion(q)
                                    navigateSafely(QuestionManagerRoutes.EDITOR)
                                },
                                onSubmitForReview = { q -> viewModel.submitForReview(q) },
                                onApproveOrReject = { q, status, pub, notes ->
                                    viewModel.approveOrRejectQuestion(q, status, pub, notes)
                                },
                                onDeleteQuestion = { q -> viewModel.deleteQuestion(q) }
                            )
                            QuestionManagerRoutes.IMPORT_EXPORT -> QuestionImportExportScreen(
                                session = session,
                                csvPreview = csvPreview,
                                totalQuestionCount = questions.size,
                                onAnalyzeCsv = { raw -> viewModel.analyzeCsvInput(raw) },
                                onClearPreview = { viewModel.clearCsvPreview() },
                                onConfirmImport = { incDup, pub -> viewModel.confirmBulkCsvImport(incDup, pub) },
                                onGenerateExportCsv = { viewModel.exportAllVisibleQuestionsCsv() }
                            )
                            QuestionManagerRoutes.AI_PREP -> QuestionAiPrepScreen(
                                subjectCatalog = subjectCatalog,
                                aiStatus = aiPrepStatus,
                                onRunFreeTierAiCheck = { cls, subj, top, diff, cnt, lang ->
                                    viewModel.testOptionalFreeTierAiDraftGeneration(cls, subj, top, diff, cnt, lang)
                                },
                                onSaveReviewedAiDrafts = { viewModel.saveReviewedAiDraftsToQueue() },
                                onResetAiState = { viewModel.resetAiPrepState() }
                            )
                            QuestionManagerRoutes.ADMIN_CONTROLS -> QuestionManagerAdminControlsScreen(
                                session = session,
                                allowedManagers = allowedManagers,
                                subjectCatalog = subjectCatalog,
                                auditLogs = auditLogs,
                                onSaveManager = { email, name, active, canPub, canDel, notes ->
                                    viewModel.authorizeOrUpdateQuestionManager(email, name, active, canPub, canDel, notes)
                                },
                                onRevokeManager = { mgr -> viewModel.revokeQuestionManager(mgr) },
                                onSaveSubjectCatalog = { c1, c6, c9, c11 ->
                                    viewModel.saveSubjectCatalogFromAdmin(c1, c6, c9, c11)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
