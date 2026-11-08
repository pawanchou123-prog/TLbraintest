package com.example.ui.questionmanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CsvImportPreview
import com.example.data.CsvQuestionDraftRow
import com.example.data.GeminiQuestionService
import com.example.data.GeneratedQuestionDraft
import com.example.data.QuestionCsvHelper
import com.example.data.model.AllowedQuestionManager
import com.example.data.model.QuestionAuditLog
import com.example.data.model.QuestionDashboardStats
import com.example.data.model.QuestionItem
import com.example.data.model.SubjectCatalogDoc
import com.example.data.repository.QuestionManagerOperatorSession
import com.example.data.repository.QuestionManagerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MS = 5000L

sealed interface OperatorAccessState {
    data object Checking : OperatorAccessState
    data class Approved(val session: QuestionManagerOperatorSession) : OperatorAccessState
    data class Denied(val reasonTitle: String, val reasonDetail: String) : OperatorAccessState
}

sealed interface AiPrepStatus {
    data object IdleNotConfigured : AiPrepStatus
    data object Generating : AiPrepStatus
    data class DraftsReadyForHumanReview(
        val studentClass: Int,
        val subject: String,
        val topic: String,
        val language: String,
        val drafts: List<GeneratedQuestionDraft>
    ) : AiPrepStatus
    data class NotConfiguredOrUnavailable(val explanation: String) : AiPrepStatus
}

@OptIn(ExperimentalCoroutinesApi::class)
class QuestionManagerViewModel(
    private val repository: QuestionManagerRepository,
    val currentUserId: String,
    val currentUserEmail: String,
    val currentDisplayName: String,
    private val isEmailVerified: Boolean
) : ViewModel() {

    private val geminiService by lazy { GeminiQuestionService() }

    private val _accessState = MutableStateFlow<OperatorAccessState>(OperatorAccessState.Checking)
    val accessState: StateFlow<OperatorAccessState> = _accessState.asStateFlow()

    private val _approvedSession = MutableStateFlow<QuestionManagerOperatorSession?>(null)
    val approvedSession: StateFlow<QuestionManagerOperatorSession?> = _approvedSession.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _editingQuestion = MutableStateFlow<QuestionItem?>(null)
    val editingQuestion: StateFlow<QuestionItem?> = _editingQuestion.asStateFlow()

    private val _csvPreview = MutableStateFlow<CsvImportPreview?>(null)
    val csvPreview: StateFlow<CsvImportPreview?> = _csvPreview.asStateFlow()

    private val _aiPrepStatus = MutableStateFlow<AiPrepStatus>(AiPrepStatus.IdleNotConfigured)
    val aiPrepStatus: StateFlow<AiPrepStatus> = _aiPrepStatus.asStateFlow()

    val questions: StateFlow<List<QuestionItem>> = _approvedSession
        .flatMapLatest { session ->
            if (session == null) emptyFlow() else repository.observeQuestionsForSession(session)
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val dashboardStats: StateFlow<QuestionDashboardStats> = questions
        .map { list -> repository.computeDashboardStats(list) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), QuestionDashboardStats())

    val subjectCatalog: StateFlow<SubjectCatalogDoc> = _approvedSession
        .flatMapLatest { session ->
            if (session == null) emptyFlow() else repository.observeSubjectCatalog()
        }
        .catch { emit(SubjectCatalogDoc()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SubjectCatalogDoc())

    val allowedManagers: StateFlow<List<AllowedQuestionManager>> = _approvedSession
        .flatMapLatest { session ->
            if (session == null || !session.isMainAdmin) emptyFlow()
            else repository.observeAllowedQuestionManagers()
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val auditLogs: StateFlow<List<QuestionAuditLog>> = _approvedSession
        .flatMapLatest { session ->
            if (session == null) emptyFlow() else repository.observeQuestionAuditLogs(session)
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    init {
        verifyAuthorization()
    }

    fun verifyAuthorization() {
        viewModelScope.launch {
            _accessState.value = OperatorAccessState.Checking
            val res = repository.verifyAndSyncOperatorSession(
                uid = currentUserId,
                rawEmail = currentUserEmail,
                displayName = currentDisplayName,
                isEmailVerified = isEmailVerified
            )
            res.fold(
                onSuccess = { session ->
                    _approvedSession.value = session
                    _accessState.value = OperatorAccessState.Approved(session)
                },
                onFailure = { err ->
                    _approvedSession.value = null
                    _accessState.value = OperatorAccessState.Denied(
                        reasonTitle = "Question Management Access Denied",
                        reasonDetail = err.localizedMessage
                            ?: "Only authorized Question Managers and Main Admins may enter /question-manager. Student accounts and unapproved Google accounts are strictly blocked."
                    )
                }
            )
        }
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun startEditingQuestion(question: QuestionItem?) {
        _editingQuestion.value = question
    }

    fun saveQuestionFromEditor(
        studentClass: Int,
        subject: String,
        topic: String,
        language: String,
        questionText: String,
        options: List<String>,
        correctIndex: Int,
        explanation: String,
        difficulty: String,
        points: Int,
        timerSeconds: Int,
        imageUrl: String,
        desiredAction: String, // "draft" | "pending_review" | "publish"
        onSavedSuccess: () -> Unit
    ) {
        val session = _approvedSession.value ?: return
        viewModelScope.launch {
            val res = repository.saveOrUpdateQuestion(
                session = session,
                existingQuestion = _editingQuestion.value,
                studentClass = studentClass,
                subject = subject,
                topic = topic,
                language = language,
                questionText = questionText,
                options = options,
                correctIndex = correctIndex,
                explanation = explanation,
                difficulty = difficulty,
                points = points,
                timerSeconds = timerSeconds,
                imageUrl = imageUrl,
                desiredAction = desiredAction
            )
            res.fold(
                onSuccess = {
                    _editingQuestion.value = null
                    _statusBanner.value = when (desiredAction) {
                        "publish" -> "Question validated, approved, and published to Student Quiz Bank."
                        "pending_review" -> "Question saved and submitted for Main Admin review."
                        else -> "Question saved safely as Draft (not visible to students)."
                    }
                    onSavedSuccess()
                },
                onFailure = { err ->
                    _statusBanner.value = err.localizedMessage ?: "Failed to save question."
                }
            )
        }
    }

    fun submitForReview(question: QuestionItem) {
        val session = _approvedSession.value ?: return
        viewModelScope.launch {
            val res = repository.submitDraftForReview(session, question)
            _statusBanner.value = res.fold(
                onSuccess = { "Submitted question for Main Admin review." },
                onFailure = { it.localizedMessage ?: "Could not submit question for review." }
            )
        }
    }

    fun approveOrRejectQuestion(
        question: QuestionItem,
        newStatus: String, // "approved" | "rejected" | "draft"
        publishNow: Boolean,
        reviewNotes: String
    ) {
        val session = _approvedSession.value ?: return
        viewModelScope.launch {
            val res = repository.reviewAndSetQuestionState(
                session = session,
                question = question,
                newReviewStatus = newStatus,
                publishNow = publishNow,
                reviewNotes = reviewNotes
            )
            _statusBanner.value = res.fold(
                onSuccess = {
                    when {
                        publishNow -> "Question approved and published for students."
                        newStatus == "rejected" -> "Question marked as rejected (hidden from students)."
                        else -> "Question moved to unpublished draft."
                    }
                },
                onFailure = { it.localizedMessage ?: "Review update failed." }
            )
        }
    }

    fun deleteQuestion(question: QuestionItem) {
        val session = _approvedSession.value ?: return
        viewModelScope.launch {
            val res = repository.deletePermittedQuestion(session, question)
            _statusBanner.value = res.fold(
                onSuccess = { "Question deleted and logged." },
                onFailure = { it.localizedMessage ?: "Permission denied to delete this question." }
            )
        }
    }

    // --- CSV Bulk Import & Export ---

    fun analyzeCsvInput(rawCsv: String) {
        val preview = QuestionCsvHelper.parseAndValidateCsv(
            rawCsv = rawCsv,
            existingQuestions = questions.value
        )
        _csvPreview.value = preview
        if (preview.rows.isEmpty()) {
            _statusBanner.value = "No valid CSV rows detected. Check column headers or load the CSV template."
        }
    }

    fun clearCsvPreview() {
        _csvPreview.value = null
    }

    fun confirmBulkCsvImport(
        includeDuplicateRows: Boolean,
        publishIfPermitted: Boolean
    ) {
        val session = _approvedSession.value ?: return
        val preview = _csvPreview.value ?: return
        val targetRows: List<CsvQuestionDraftRow> = if (includeDuplicateRows) {
            preview.validNonDuplicateRows + preview.validDuplicateRows
        } else {
            preview.validNonDuplicateRows
        }
        viewModelScope.launch {
            val res = repository.importValidatedCsvRows(
                session = session,
                rowsToSave = targetRows,
                publishImportedIfPermitted = publishIfPermitted
            )
            res.fold(
                onSuccess = { count ->
                    _csvPreview.value = null
                    _statusBanner.value = "Imported $count validated CSV questions without overwriting existing items."
                },
                onFailure = { err ->
                    _statusBanner.value = err.localizedMessage ?: "CSV import failed."
                }
            )
        }
    }

    fun exportAllVisibleQuestionsCsv(): String {
        return QuestionCsvHelper.exportQuestionsToCsv(questions.value)
    }

    // --- Optional AI Question Generation Preparation (Free-Tier Safe, Human Review Mandatory) ---

    fun testOptionalFreeTierAiDraftGeneration(
        studentClass: Int,
        subject: String,
        topic: String,
        difficulty: String,
        count: Int,
        language: String
    ) {
        if (_approvedSession.value == null) return
        viewModelScope.launch {
            _aiPrepStatus.value = AiPrepStatus.Generating
            val res = geminiService.generateDraftQuestions(
                studentClass = studentClass,
                subject = subject,
                language = language,
                difficulty = difficulty,
                topicHint = topic,
                count = count
            )
            res.fold(
                onSuccess = { drafts ->
                    _aiPrepStatus.value = AiPrepStatus.DraftsReadyForHumanReview(
                        studentClass = studentClass,
                        subject = subject,
                        topic = topic,
                        language = language,
                        drafts = drafts
                    )
                },
                onFailure = { err ->
                    _aiPrepStatus.value = AiPrepStatus.NotConfiguredOrUnavailable(
                        explanation = "AI Generation is currently Not Configured / Unavailable on the Spark Free Tier (${err.localizedMessage ?: "Server-side AI endpoint not enabled"}). Zero paid APIs were activated and zero API keys are exposed in frontend code. Use Add Question or Bulk CSV Import instead."
                    )
                }
            )
        }
    }

    fun resetAiPrepState() {
        _aiPrepStatus.value = AiPrepStatus.IdleNotConfigured
    }

    fun saveReviewedAiDraftsToQueue() {
        val session = _approvedSession.value ?: return
        val ready = _aiPrepStatus.value as? AiPrepStatus.DraftsReadyForHumanReview ?: return
        viewModelScope.launch {
            val res = repository.saveAiReviewedDrafts(
                session = session,
                studentClass = ready.studentClass,
                subject = ready.subject,
                topic = ready.topic,
                language = ready.language,
                drafts = ready.drafts
            )
            res.fold(
                onSuccess = { saved ->
                    _aiPrepStatus.value = AiPrepStatus.IdleNotConfigured
                    _statusBanner.value = "Saved $saved human-reviewed AI questions as unpublished Drafts."
                },
                onFailure = { err ->
                    _statusBanner.value = err.localizedMessage ?: "Failed to save AI drafts."
                }
            )
        }
    }

    // --- Main Admin Controls inside Question Management ---

    fun authorizeOrUpdateQuestionManager(
        email: String,
        managerName: String,
        isActive: Boolean,
        canPublishDirectly: Boolean,
        canDeleteQuestions: Boolean,
        notes: String
    ) {
        val session = _approvedSession.value ?: return
        viewModelScope.launch {
            val res = repository.saveAllowedQuestionManager(
                session = session,
                email = email,
                managerName = managerName,
                isActive = isActive,
                canPublishDirectly = canPublishDirectly,
                canDeleteQuestions = canDeleteQuestions,
                notes = notes
            )
            _statusBanner.value = res.fold(
                onSuccess = { "Question Manager authorization saved for ${email.trim().lowercase()}." },
                onFailure = { it.localizedMessage ?: "Failed to update Question Manager authorization." }
            )
        }
    }

    fun revokeQuestionManager(manager: AllowedQuestionManager) {
        val session = _approvedSession.value ?: return
        viewModelScope.launch {
            val res = repository.removeAllowedQuestionManager(session, manager)
            _statusBanner.value = res.fold(
                onSuccess = { "Revoked Question Manager access for ${manager.email}." },
                onFailure = { it.localizedMessage ?: "Failed to revoke Question Manager." }
            )
        }
    }

    fun saveSubjectCatalogFromAdmin(
        classes1To5Csv: String,
        classes6To8Csv: String,
        classes9To10Csv: String,
        classes11To12Csv: String
    ) {
        val session = _approvedSession.value ?: return
        val parse = { s: String -> s.split(",").map { it.trim() }.filter { it.isNotEmpty() } }
        viewModelScope.launch {
            val res = repository.saveSubjectCatalog(
                session = session,
                classes1To5 = parse(classes1To5Csv),
                classes6To8 = parse(classes6To8Csv),
                classes9To10 = parse(classes9To10Csv),
                classes11To12 = parse(classes11To12Csv)
            )
            _statusBanner.value = res.fold(
                onSuccess = { "Master Class 1-12 Subject Catalog updated in Firestore." },
                onFailure = { it.localizedMessage ?: "Failed to save Subject Catalog." }
            )
        }
    }
}
