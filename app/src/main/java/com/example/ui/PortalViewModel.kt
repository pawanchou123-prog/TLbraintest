package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BuiltInQuestionSeed
import com.example.data.CurriculumCatalog
import com.example.data.GeminiQuestionService
import com.example.data.GeneratedQuestionDraft
import com.example.data.model.AllowedStudent
import com.example.data.model.AnnouncementItem
import com.example.data.model.AppConfig
import com.example.data.model.AppPortal
import com.example.data.model.BannerItem
import com.example.data.model.LeaderboardEntry
import com.example.data.model.OWNER_ADMIN_EMAIL
import com.example.data.model.PrizeItem
import com.example.data.model.QuestionItem
import com.example.data.model.QuizResultRecord
import com.example.data.model.SupportMessage
import com.example.data.model.SupportThread
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.WinnerRecord
import com.example.data.repository.TlBrainRepository
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

private const val TAG = "PortalViewModel"
private const val STOP_TIMEOUT_MS = 5000L

sealed interface AiGeneratorState {
    data object Idle : AiGeneratorState
    data object Generating : AiGeneratorState
    data class DraftsReady(val drafts: List<GeneratedQuestionDraft>) : AiGeneratorState
    data class Error(val message: String) : AiGeneratorState
}

@OptIn(ExperimentalCoroutinesApi::class)
class PortalViewModel(
    private val repository: TlBrainRepository,
    private val geminiService: GeminiQuestionService,
    val currentUserId: String,
    val currentUserEmail: String,
    val currentDisplayName: String
) : ViewModel() {

    private val isBootstrappedOwner = currentUserEmail.trim().equals(OWNER_ADMIN_EMAIL, ignoreCase = true)

    private val _isAdmin = MutableStateFlow(isBootstrappedOwner)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _isQuestionManager = MutableStateFlow(isBootstrappedOwner)
    val isQuestionManager: StateFlow<Boolean> = _isQuestionManager.asStateFlow()

    private val _rolesChecked = MutableStateFlow(false)
    val rolesChecked: StateFlow<Boolean> = _rolesChecked.asStateFlow()

    private val _activePortal = MutableStateFlow(
        if (isBootstrappedOwner) AppPortal.MAIN_ADMIN else AppPortal.STUDENT
    )
    val activePortal: StateFlow<AppPortal> = _activePortal.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _aiState = MutableStateFlow<AiGeneratorState>(AiGeneratorState.Idle)
    val aiState: StateFlow<AiGeneratorState> = _aiState.asStateFlow()

    private val _selectedSupportStudentUid = MutableStateFlow<String?>(null)
    val selectedSupportStudentUid: StateFlow<String?> = _selectedSupportStudentUid.asStateFlow()

    val appConfig: StateFlow<AppConfig> = repository.observeAppConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppConfig())

    val allowedStudent: StateFlow<AllowedStudent?> = repository.observeAllowedStudentByEmail(currentUserEmail)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val userProfileState: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing user profile", e)
            emit(UiState.Error(e.message ?: "Failed to load profile"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val publishedQuestions: StateFlow<List<QuestionItem>> = repository.observePublishedQuestions()
        .catch { e ->
            Log.w(TAG, "Error observing published questions", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val studentResults: StateFlow<List<QuizResultRecord>> = repository.observeStudentResults(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing student results", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val leaderboardEntries: StateFlow<List<LeaderboardEntry>> = repository.observeActiveLeaderboard()
        .catch { e ->
            Log.w(TAG, "Error observing leaderboard", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val publishedWinners: StateFlow<List<WinnerRecord>> = repository.observePublishedWinners()
        .catch { e ->
            Log.w(TAG, "Error observing winners history", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val activeBanners: StateFlow<List<BannerItem>> = repository.observeActiveBanners()
        .catch { e ->
            Log.w(TAG, "Error observing banners", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val activePrizes: StateFlow<List<PrizeItem>> = repository.observeActivePrizes()
        .catch { e ->
            Log.w(TAG, "Error observing prizes", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val activeAnnouncements: StateFlow<List<AnnouncementItem>> = repository.observeActiveAnnouncements()
        .catch { e ->
            Log.w(TAG, "Error observing announcements", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val mySupportMessages: StateFlow<List<SupportMessage>> = repository.observeStudentSupportMessages(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing student support messages", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    // --- Privileged Streams (Only subscribed when role permits) ---

    val adminAllStudents: StateFlow<List<AllowedStudent>> = _isAdmin
        .flatMapLatest { admin ->
            if (admin && isBootstrappedOwner) repository.observeAllAllowedStudents() else emptyFlow()
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val adminAllUsers: StateFlow<List<UserProfile>> = _isAdmin
        .flatMapLatest { admin ->
            if (admin && isBootstrappedOwner) repository.observeAllUsersForAdmin() else emptyFlow()
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val adminAllResults: StateFlow<List<QuizResultRecord>> = _isAdmin
        .flatMapLatest { admin ->
            if (admin && isBootstrappedOwner) repository.observeAllResultsForAdmin() else emptyFlow()
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val studioQuestions: StateFlow<List<QuestionItem>> = _isQuestionManager
        .flatMapLatest { canManage ->
            when {
                !canManage -> emptyFlow()
                isBootstrappedOwner -> repository.observeAllQuestionsForAdmin()
                else -> repository.observeAuthorQuestions(currentUserId)
            }
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val adminSupportThreads: StateFlow<List<SupportThread>> = _isAdmin
        .flatMapLatest { admin ->
            if (admin && isBootstrappedOwner) repository.observeAllSupportThreadsForAdmin() else emptyFlow()
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val adminSelectedThreadMessages: StateFlow<List<SupportMessage>> = _selectedSupportStudentUid
        .flatMapLatest { studentUid ->
            if (studentUid.isNullOrBlank()) emptyFlow()
            else repository.observeStudentSupportMessages(studentUid)
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    init {
        viewModelScope.launch {
            val extraAdmin = if (isBootstrappedOwner) true else repository.isExtraAdminRole(currentUserId)
            val qm = if (extraAdmin) true else repository.isQuestionManagerRole(currentUserId)
            _isAdmin.value = extraAdmin
            _isQuestionManager.value = qm

            val studentEntry = repository.getAllowedStudentOnce(currentUserEmail)
            when {
                extraAdmin -> {
                    _activePortal.value = AppPortal.MAIN_ADMIN
                    repository.ensureUserProfileInitialized(
                        uid = currentUserId,
                        email = currentUserEmail,
                        fallbackName = currentDisplayName.ifBlank { "Pritam Choudhary" },
                        studentClass = 10,
                        preferredLang = "en"
                    )
                    checkAutomaticMonthlyReset()
                }
                qm && (studentEntry == null || !studentEntry.isSubscriptionCurrentlyValid()) -> {
                    _activePortal.value = AppPortal.QUESTION_STUDIO
                }
                studentEntry != null && studentEntry.isSubscriptionCurrentlyValid() -> {
                    _activePortal.value = AppPortal.STUDENT
                    repository.ensureUserProfileInitialized(
                        uid = currentUserId,
                        email = currentUserEmail,
                        fallbackName = studentEntry.studentName.ifBlank { currentDisplayName },
                        studentClass = studentEntry.studentClass.toInt(),
                        preferredLang = studentEntry.preferredLanguage
                    )
                }
            }
            _rolesChecked.value = true
        }
    }

    private suspend fun checkAutomaticMonthlyReset() {
        if (!isBootstrappedOwner) return
        val cfg = appConfig.value
        val actualMonth = CurriculumCatalog.currentMonthKey()
        if (cfg.currentMonthKey.isNotBlank() && cfg.currentMonthKey != actualMonth) {
            repository.resetMonthlyLeaderboardAndArchiveWinners(
                monthLabel = cfg.currentMonthKey,
                defaultPrizeLabel = "Monthly Top Scholar Reward"
            )
            repository.saveAppConfig(cfg.copy(currentMonthKey = actualMonth))
        }
    }

    fun switchPortal(portal: AppPortal) {
        when (portal) {
            AppPortal.MAIN_ADMIN -> if (_isAdmin.value) _activePortal.value = AppPortal.MAIN_ADMIN
            AppPortal.QUESTION_STUDIO -> if (_isQuestionManager.value) _activePortal.value = AppPortal.QUESTION_STUDIO
            AppPortal.STUDENT -> _activePortal.value = AppPortal.STUDENT
        }
    }

    fun clearStatusBanner() {
        _statusBannerMessage.value = null
    }

    private fun postMessage(msg: String) {
        _statusBannerMessage.value = msg
    }

    // --- Student Actions ---

    fun updateStudentPreferences(
        profile: UserProfile,
        displayName: String,
        studentClass: Int,
        language: String,
        soundEnabled: Boolean
    ) {
        viewModelScope.launch {
            val res = repository.updateProfilePreferences(profile, displayName, studentClass, language, soundEnabled)
            postMessage(if (res.isSuccess) "Profile settings saved." else (res.exceptionOrNull()?.localizedMessage ?: "Failed to update settings."))
        }
    }

    fun submitQuizOrGameResult(
        activityType: String,
        title: String,
        studentClass: Int,
        score: Int,
        totalQuestions: Int,
        correctAnswers: Int,
        xpEarned: Int,
        durationSeconds: Int,
        questionAttempts: List<Pair<QuestionItem, Pair<Boolean, Int>>> = emptyList()
    ) {
        val currentProfile = (userProfileState.value as? UiState.Success)?.data
            ?: UserProfile(
                id = currentUserId,
                userId = currentUserId,
                email = currentUserEmail,
                displayName = currentDisplayName.ifBlank { "Student" },
                studentClass = 10L
            )
        viewModelScope.launch {
            repository.recordActivityCompletion(
                profile = currentProfile,
                activityType = activityType,
                title = title,
                studentClass = studentClass,
                score = score,
                totalQuestions = totalQuestions,
                correctAnswers = correctAnswers,
                xpEarned = xpEarned,
                durationSeconds = durationSeconds,
                questionAttempts = questionAttempts
            )
        }
    }

    fun sendStudentSupportMessage(text: String) {
        val profile = (userProfileState.value as? UiState.Success)?.data
        val sName = profile?.displayName?.ifBlank { currentDisplayName } ?: currentDisplayName.ifBlank { "Student" }
        val sClass = (profile?.studentClass ?: 10L).toInt()
        viewModelScope.launch {
            val res = repository.sendSupportMessage(
                studentUid = currentUserId,
                studentName = sName,
                studentEmail = currentUserEmail,
                studentClass = sClass,
                senderRole = "student",
                senderName = sName,
                text = text
            )
            if (res.isFailure) {
                postMessage(res.exceptionOrNull()?.localizedMessage ?: "Failed to send message.")
            }
        }
    }

    // --- Question Studio Actions ---

    fun saveQuestionEntry(
        existingId: String?,
        studentClass: Int,
        subject: String,
        language: String,
        questionText: String,
        options: List<String>,
        correctIndex: Int,
        explanation: String,
        difficulty: String,
        points: Int,
        timerSeconds: Int,
        imageUrl: String,
        isPublished: Boolean,
        onSaved: () -> Unit = {}
    ) {
        if (questionText.isBlank() || options.any { it.isBlank() }) {
            postMessage("Please fill the question text and all four options.")
            return
        }
        viewModelScope.launch {
            val res = repository.saveQuestion(
                existingId = existingId,
                studentClass = studentClass,
                subject = subject,
                language = language,
                questionText = questionText,
                options = options,
                correctIndex = correctIndex,
                explanation = explanation,
                difficulty = difficulty,
                points = points,
                timerSeconds = timerSeconds,
                imageUrl = imageUrl,
                isPublished = isPublished,
                source = "manual"
            )
            if (res.isSuccess) {
                postMessage(if (existingId.isNullOrBlank()) "Question added successfully." else "Question updated.")
                onSaved()
            } else {
                postMessage(res.exceptionOrNull()?.localizedMessage ?: "Failed to save question.")
            }
        }
    }

    fun togglePublishQuestion(questionId: String, publish: Boolean) {
        viewModelScope.launch {
            val res = repository.toggleQuestionPublish(questionId, publish)
            postMessage(
                if (res.isSuccess) {
                    if (publish) "Question published to student bank." else "Question moved back to drafts."
                } else {
                    res.exceptionOrNull()?.localizedMessage ?: "Failed to update status."
                }
            )
        }
    }

    fun deleteQuestionItem(questionId: String) {
        viewModelScope.launch {
            val res = repository.deleteQuestion(questionId)
            postMessage(if (res.isSuccess) "Question deleted." else "Failed to delete question.")
        }
    }

    fun seedStarterQuestions() {
        viewModelScope.launch {
            var count = 0
            for (spec in BuiltInQuestionSeed.starterPack) {
                val res = repository.saveQuestion(
                    existingId = null,
                    studentClass = spec.studentClass,
                    subject = spec.subject,
                    language = spec.language,
                    questionText = spec.questionText,
                    options = spec.options,
                    correctIndex = spec.correctIndex,
                    explanation = spec.explanation,
                    difficulty = spec.difficulty,
                    points = spec.points,
                    timerSeconds = spec.timerSeconds,
                    imageUrl = "",
                    isPublished = true,
                    source = "manual"
                )
                if (res.isSuccess) count++
            }
            postMessage("Loaded $count starter curriculum questions into live bank.")
        }
    }

    // --- Admin Actions ---

    fun saveStudentSubscriptionAccount(
        email: String,
        studentName: String,
        studentClass: Int,
        preferredLanguage: String,
        isBanned: Boolean,
        subscriptionActive: Boolean,
        subscriptionPlan: String,
        paymentMethod: String,
        subscriptionStartMs: Long,
        subscriptionEndMs: Long,
        adminNotes: String
    ) {
        viewModelScope.launch {
            val res = repository.saveAllowedStudent(
                email = email,
                studentName = studentName,
                studentClass = studentClass,
                preferredLanguage = preferredLanguage,
                isBanned = isBanned,
                subscriptionActive = subscriptionActive,
                subscriptionPlan = subscriptionPlan,
                paymentMethod = paymentMethod,
                subscriptionStartMs = subscriptionStartMs,
                subscriptionEndMs = subscriptionEndMs,
                adminNotes = adminNotes
            )
            postMessage(if (res.isSuccess) "Student account & subscription saved." else (res.exceptionOrNull()?.localizedMessage ?: "Failed to save student."))
        }
    }

    fun toggleBanStudent(email: String, ban: Boolean) {
        viewModelScope.launch {
            val res = repository.toggleStudentBan(email, ban)
            postMessage(if (res.isSuccess) (if (ban) "Student banned." else "Student unbanned.") else "Action failed.")
        }
    }

    fun removeStudentAccount(email: String) {
        viewModelScope.launch {
            val res = repository.deleteAllowedStudent(email)
            postMessage(if (res.isSuccess) "Student whitelist record removed." else "Delete failed.")
        }
    }

    fun assignQuestionManager(uid: String, email: String, name: String) {
        viewModelScope.launch {
            val res = repository.assignQuestionManagerByUid(uid, email, name)
            postMessage(if (res.isSuccess) "Question Studio permission granted to $name." else (res.exceptionOrNull()?.localizedMessage ?: "Failed to grant role."))
        }
    }

    fun generateAiQuestions(
        studentClass: Int,
        subject: String,
        language: String,
        difficulty: String,
        topicHint: String,
        count: Int
    ) {
        viewModelScope.launch {
            _aiState.value = AiGeneratorState.Generating
            val res = geminiService.generateDraftQuestions(
                studentClass = studentClass,
                subject = subject,
                language = language,
                difficulty = difficulty,
                topicHint = topicHint,
                count = count
            )
            _aiState.value = res.fold(
                onSuccess = { AiGeneratorState.DraftsReady(it) },
                onFailure = { AiGeneratorState.Error(it.localizedMessage ?: "AI generation failed.") }
            )
        }
    }

    fun saveAiDraftsToReviewQueue(
        studentClass: Int,
        subject: String,
        language: String,
        drafts: List<GeneratedQuestionDraft>
    ) {
        viewModelScope.launch {
            val res = repository.saveGeneratedDraftBatch(studentClass, subject, language, drafts)
            if (res.isSuccess) {
                _aiState.value = AiGeneratorState.Idle
                postMessage("Saved ${res.getOrDefault(0)} AI drafts to Question Studio Review Queue (Unpublished).")
            } else {
                postMessage(res.exceptionOrNull()?.localizedMessage ?: "Failed to save AI drafts.")
            }
        }
    }

    fun clearAiDrafts() {
        _aiState.value = AiGeneratorState.Idle
    }

    fun saveGlobalConfig(updated: AppConfig) {
        viewModelScope.launch {
            val res = repository.saveAppConfig(updated)
            postMessage(if (res.isSuccess) "Platform configuration updated." else "Failed to save settings.")
        }
    }

    fun createBanner(title: String, subtitle: String, imageUrl: String, actionUrl: String) {
        viewModelScope.launch {
            val res = repository.addBanner(title, subtitle, imageUrl, actionUrl)
            postMessage(if (res.isSuccess) "HTTPS banner published." else (res.exceptionOrNull()?.localizedMessage ?: "Invalid banner URL."))
        }
    }

    fun removeBanner(id: String) {
        viewModelScope.launch { repository.deleteBanner(id) }
    }

    fun createPrize(rankLabel: String, title: String, description: String, imageUrl: String) {
        viewModelScope.launch {
            val res = repository.addPrize(rankLabel, title, description, imageUrl)
            postMessage(if (res.isSuccess) "Prize & gift card added." else (res.exceptionOrNull()?.localizedMessage ?: "Invalid HTTPS image URL."))
        }
    }

    fun removePrize(id: String) {
        viewModelScope.launch { repository.deletePrize(id) }
    }

    fun publishNotice(title: String, message: String, category: String, targetClass: Int) {
        viewModelScope.launch {
            val res = repository.addAnnouncement(title, message, category, targetClass)
            postMessage(if (res.isSuccess) "Notice broadcasted." else "Failed to publish notice.")
        }
    }

    fun removeNotice(id: String) {
        viewModelScope.launch { repository.deleteAnnouncement(id) }
    }

    fun triggerManualMonthlyReset(monthLabel: String, prizeLabel: String) {
        viewModelScope.launch {
            val res = repository.resetMonthlyLeaderboardAndArchiveWinners(monthLabel, prizeLabel)
            if (res.isSuccess) {
                val archived = res.getOrDefault(0)
                val cfg = appConfig.value
                repository.saveAppConfig(cfg.copy(currentMonthKey = CurriculumCatalog.currentMonthKey()))
                postMessage("Monthly leaderboard reset complete. Archived $archived top winners.")
            } else {
                postMessage(res.exceptionOrNull()?.localizedMessage ?: "Monthly reset failed.")
            }
        }
    }

    fun selectSupportThread(studentUid: String?) {
        _selectedSupportStudentUid.value = studentUid
    }

    fun sendAdminSupportReply(thread: SupportThread, text: String) {
        viewModelScope.launch {
            val res = repository.sendSupportMessage(
                studentUid = thread.studentUid,
                studentName = thread.studentName,
                studentEmail = thread.studentEmail,
                studentClass = thread.studentClass.toInt(),
                senderRole = "admin",
                senderName = "Pritam Choudhary (Admin)",
                text = text
            )
            if (res.isFailure) {
                postMessage(res.exceptionOrNull()?.localizedMessage ?: "Reply failed.")
            }
        }
    }
}
