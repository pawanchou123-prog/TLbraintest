package com.example.ui.student

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SamplePreviewCatalog
import com.example.data.model.AllowedStudent
import com.example.data.model.AnnouncementItem
import com.example.data.model.AppConfig
import com.example.data.model.BannerItem
import com.example.data.model.LeaderboardEntry
import com.example.data.model.OWNER_ADMIN_EMAIL
import com.example.data.model.PrizeItem
import com.example.data.model.QuestionItem
import com.example.data.model.QuizResultRecord
import com.example.data.model.SupportMessage
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.WinnerRecord
import com.example.data.repository.TlBrainRepository
import com.example.ui.auth.StudentAuthManager
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

private const val TAG = "StudentViewModel"
private const val STOP_TIMEOUT_MS = 5000L

sealed interface StudentAccessState {
    data object Checking : StudentAccessState
    data class Approved(
        val isOwnerTestAccount: Boolean,
        val allowedRecord: AllowedStudent?
    ) : StudentAccessState
    data class Denied(
        val reasonTitle: String,
        val reasonDetail: String,
        val allowedRecord: AllowedStudent?
    ) : StudentAccessState
}

data class ActiveQuizLaunch(
    val quizTitle: String,
    val isGkMode: Boolean,
    val studentClass: Int,
    val isSamplePreviewData: Boolean,
    val questions: List<QuestionItem>
)

/**
 * Standalone ViewModel exclusively powering Application 1: Student User Application (/app).
 * Strictly enforces admin-approved Google account verification BEFORE subscribing to any
 * student data streams so unauthorized Google accounts never access student data.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StudentViewModel(
    private val appContext: Context,
    private val repository: TlBrainRepository,
    val currentUserId: String,
    val currentUserEmail: String,
    val currentDisplayName: String
) : ViewModel() {

    private val isOwnerTestAccount = currentUserEmail.trim().equals(OWNER_ADMIN_EMAIL, ignoreCase = true)

    private val _accessState = MutableStateFlow<StudentAccessState>(StudentAccessState.Checking)
    val accessState: StateFlow<StudentAccessState> = _accessState.asStateFlow()

    private val _isAuthorized = MutableStateFlow(false)

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _activeQuizLaunch = MutableStateFlow<ActiveQuizLaunch?>(null)
    val activeQuizLaunch: StateFlow<ActiveQuizLaunch?> = _activeQuizLaunch.asStateFlow()

    private val _readNotificationIds = MutableStateFlow(StudentAuthManager.getReadNotificationIds(appContext))
    val readNotificationIds: StateFlow<Set<String>> = _readNotificationIds.asStateFlow()

    // Public global config (safe contact / UPI / WhatsApp info for both lock screen and student app)
    val appConfig: StateFlow<AppConfig> = repository.observeAppConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppConfig())

    val allowedStudent: StateFlow<AllowedStudent?> = repository.observeAllowedStudentByEmail(currentUserEmail)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    // Strictly gated student Firestore streams — NEVER subscribed unless _isAuthorized == true
    val userProfileState: StateFlow<UiState<UserProfile?>> = _isAuthorized
        .flatMapLatest { authorized ->
            if (!authorized) emptyFlow()
            else repository.observeUserProfile(currentUserId)
                .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
                .catch { e ->
                    Log.w(TAG, "Error observing student profile", e)
                    emit(UiState.Error(e.message ?: "Failed to load student profile"))
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val publishedQuestions: StateFlow<List<QuestionItem>> = _isAuthorized
        .flatMapLatest { authorized ->
            if (!authorized) emptyFlow() else repository.observePublishedQuestions()
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val studentResults: StateFlow<List<QuizResultRecord>> = _isAuthorized
        .flatMapLatest { authorized ->
            if (!authorized) emptyFlow() else repository.observeStudentResults(currentUserId)
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val leaderboardEntries: StateFlow<List<LeaderboardEntry>> = _isAuthorized
        .flatMapLatest { authorized ->
            if (!authorized) emptyFlow() else repository.observeActiveLeaderboard()
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val publishedWinners: StateFlow<List<WinnerRecord>> = _isAuthorized
        .flatMapLatest { authorized ->
            if (!authorized) emptyFlow() else repository.observePublishedWinners()
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val activeBanners: StateFlow<List<BannerItem>> = _isAuthorized
        .flatMapLatest { authorized ->
            if (!authorized) emptyFlow() else repository.observeActiveBanners()
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val activePrizes: StateFlow<List<PrizeItem>> = _isAuthorized
        .flatMapLatest { authorized ->
            if (!authorized) emptyFlow() else repository.observeActivePrizes()
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val activeAnnouncements: StateFlow<List<AnnouncementItem>> = _isAuthorized
        .flatMapLatest { authorized ->
            if (!authorized) emptyFlow() else repository.observeActiveAnnouncements()
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val supportMessages: StateFlow<List<SupportMessage>> = _isAuthorized
        .flatMapLatest { authorized ->
            if (!authorized) emptyFlow() else repository.observeStudentSupportMessages(currentUserId)
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    init {
        verifyStudentAuthorization()
    }

    fun verifyStudentAuthorization() {
        viewModelScope.launch {
            _accessState.value = StudentAccessState.Checking
            val record = repository.getAllowedStudentOnce(currentUserEmail)
            when {
                record != null && record.isSubscriptionCurrentlyValid() -> {
                    _isAuthorized.value = true
                    repository.ensureUserProfileInitialized(
                        uid = currentUserId,
                        email = currentUserEmail,
                        fallbackName = record.studentName.ifBlank { currentDisplayName },
                        studentClass = record.studentClass.toInt().coerceIn(1, 12),
                        preferredLang = record.preferredLanguage
                    )
                    _accessState.value = StudentAccessState.Approved(
                        isOwnerTestAccount = false,
                        allowedRecord = record
                    )
                }
                isOwnerTestAccount -> {
                    // Owner Pritam Choudhary is automatically authorized to preview and test Application 1 (/app)
                    _isAuthorized.value = true
                    repository.ensureUserProfileInitialized(
                        uid = currentUserId,
                        email = currentUserEmail,
                        fallbackName = currentDisplayName.ifBlank { "Pritam Choudhary" },
                        studentClass = 10,
                        preferredLang = "en"
                    )
                    _accessState.value = StudentAccessState.Approved(
                        isOwnerTestAccount = true,
                        allowedRecord = record
                    )
                }
                record?.isBanned == true -> {
                    _isAuthorized.value = false
                    _accessState.value = StudentAccessState.Denied(
                        reasonTitle = "Student Account Suspended",
                        reasonDetail = "Your Google account has been suspended by Administrator Pritam Choudhary. Please contact support for assistance.",
                        allowedRecord = record
                    )
                }
                record != null -> {
                    _isAuthorized.value = false
                    _accessState.value = StudentAccessState.Denied(
                        reasonTitle = "Student Subscription Expired",
                        reasonDetail = "Your Rs. 50 / 3-Month student subscription has expired. Contact Administrator Pritam Choudhary via Manual UPI or Cash to renew access.",
                        allowedRecord = record
                    )
                }
                else -> {
                    _isAuthorized.value = false
                    _accessState.value = StudentAccessState.Denied(
                        reasonTitle = "Administrator Approval Required",
                        reasonDetail = "TL Brain Test does not allow public signup. Only Google accounts approved and activated by Administrator Pritam Choudhary can enter the Student Application.",
                        allowedRecord = null
                    )
                }
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun prepareCategoryOrSubjectQuiz(
        categoryOrSubject: String,
        studentClass: Int,
        language: String,
        isGkMode: Boolean
    ) {
        val livePool = publishedQuestions.value
        val matchedLive = livePool.filter { q ->
            val langMatch = q.language.equals(language, ignoreCase = true)
            if (isGkMode || categoryOrSubject.equals("General Knowledge", ignoreCase = true)) {
                (q.studentClass == 0L || q.subject.equals("General Knowledge", ignoreCase = true)) && langMatch
            } else {
                val subjMatch = q.subject.equals(categoryOrSubject, ignoreCase = true) ||
                    (categoryOrSubject == "Computer" && q.subject.contains("Computer", ignoreCase = true))
                (q.studentClass.toInt() == studentClass || q.studentClass == 0L) && subjMatch && langMatch
            }
        }.ifEmpty {
            livePool.filter { q ->
                if (isGkMode || categoryOrSubject.equals("General Knowledge", ignoreCase = true)) {
                    q.studentClass == 0L || q.subject.equals("General Knowledge", ignoreCase = true)
                } else {
                    val subjMatch = q.subject.equals(categoryOrSubject, ignoreCase = true) ||
                        (categoryOrSubject == "Computer" && q.subject.contains("Computer", ignoreCase = true))
                    q.studentClass.toInt() == studentClass && subjMatch
                }
            }
        }

        if (matchedLive.isNotEmpty()) {
            _activeQuizLaunch.value = ActiveQuizLaunch(
                quizTitle = if (isGkMode) "General Knowledge (Live)" else "Class $studentClass • $categoryOrSubject",
                isGkMode = isGkMode,
                studentClass = studentClass,
                isSamplePreviewData = false,
                questions = matchedLive.take(15)
            )
        } else {
            // Honest fallback to clearly labeled Sample Preview questions when live Firestore bank has 0 questions for this topic
            val previewItems = SamplePreviewCatalog.sampleQuestionsForCategory(
                categoryOrSubject = categoryOrSubject,
                studentClass = studentClass,
                lang = language
            )
            _activeQuizLaunch.value = ActiveQuizLaunch(
                quizTitle = "$categoryOrSubject (Sample Preview)",
                isGkMode = isGkMode,
                studentClass = studentClass,
                isSamplePreviewData = true,
                questions = previewItems
            )
        }
    }

    fun clearActiveQuiz() {
        _activeQuizLaunch.value = null
    }

    fun updateStudentProfile(
        currentProfile: UserProfile,
        displayName: String,
        studentClass: Int,
        schoolName: String,
        avatarId: String,
        language: String,
        soundEnabled: Boolean
    ) {
        if (!_isAuthorized.value) return
        viewModelScope.launch {
            val res = repository.updateProfilePreferences(
                profile = currentProfile,
                newName = displayName,
                newClass = studentClass,
                newLanguage = language,
                soundEnabled = soundEnabled,
                newSchoolName = schoolName,
                newAvatarId = avatarId,
                termsAccepted = true
            )
            _statusMessage.value = if (res.isSuccess) {
                if (language == "hi") "छात्र प्रोफ़ाइल सफलतापूर्वक सहेजी गई।" else "Student profile updated successfully."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to update student profile."
            }
        }
    }

    fun submitActivityResult(
        currentProfile: UserProfile,
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
        if (!_isAuthorized.value) return
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

    fun sendSupportMessage(currentProfile: UserProfile, messageText: String) {
        if (!_isAuthorized.value) return
        val clean = messageText.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val res = repository.sendSupportMessage(
                studentUid = currentUserId,
                studentName = currentProfile.displayName.ifBlank { currentDisplayName.ifBlank { "Student" } },
                studentEmail = currentUserEmail,
                studentClass = currentProfile.studentClass.toInt().coerceIn(1, 12),
                senderRole = "student",
                senderName = currentProfile.displayName.ifBlank { currentDisplayName.ifBlank { "Student" } },
                text = clean
            )
            if (res.isFailure) {
                _statusMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Unable to send support message."
            }
        }
    }

    fun markNotificationRead(noticeId: String) {
        _readNotificationIds.value = StudentAuthManager.markNotificationReadLocally(appContext, noticeId)
    }

    fun submitAccessOrPaymentRequest(
        studentName: String,
        studentClass: Int,
        paymentMethod: String,
        referenceCode: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.submitPendingSubscriptionOrAccessRequest(
                studentUid = currentUserId,
                studentEmail = currentUserEmail,
                studentName = studentName.ifBlank { currentDisplayName.ifBlank { "Student" } },
                studentClass = studentClass,
                paymentMethod = paymentMethod,
                referenceCode = referenceCode
            )
            res.fold(
                onSuccess = {
                    onResult(true, "Pending request submitted for manual review by Administrator Pritam Choudhary.")
                },
                onFailure = { e ->
                    onResult(false, e.localizedMessage ?: "Failed to submit request.")
                }
            )
        }
    }
}
