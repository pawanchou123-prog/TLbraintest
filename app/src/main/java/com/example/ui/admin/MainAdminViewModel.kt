package com.example.ui.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CurriculumCatalog
import com.example.data.model.AdminAuditLog
import com.example.data.model.AdminDashboardStats
import com.example.data.model.AllowedStudent
import com.example.data.model.AnnouncementItem
import com.example.data.model.AppConfig
import com.example.data.model.BannerItem
import com.example.data.model.LeaderboardEntry
import com.example.data.model.PrizeItem
import com.example.data.model.QuestionItem
import com.example.data.model.QuizResultRecord
import com.example.data.model.RoleMember
import com.example.data.model.SubscriptionRecord
import com.example.data.model.SupportMessage
import com.example.data.model.SupportThread
import com.example.data.model.UserProfile
import com.example.data.model.WinnerRecord
import com.example.data.model.isPreAuthorizedMainAdminEmail
import com.example.data.repository.AdminRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val ADMIN_VM_TAG = "MainAdminViewModel"
private const val ADMIN_STOP_TIMEOUT_MS = 5000L

sealed interface AdminAccessState {
    data object Checking : AdminAccessState
    data class Authorized(val roleMember: RoleMember) : AdminAccessState
    data class Denied(
        val email: String,
        val reasonTitle: String,
        val reasonDetail: String
    ) : AdminAccessState
}

data class AdminFeedbackBanner(
    val message: String,
    val isError: Boolean
)

/**
 * Dedicated ViewModel exclusively powering Application 2: MAIN ADMIN APPLICATION (/admin).
 * Performs a real server-enforced Firestore verification handshake before subscribing to
 * any admin collection streams, ensuring student or unauthorized accounts are strictly blocked.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainAdminViewModel(
    private val repository: AdminRepository,
    val currentAdminUid: String,
    val currentAdminEmail: String,
    val currentAdminName: String,
    val isEmailVerified: Boolean
) : ViewModel() {

    private val _accessState = MutableStateFlow<AdminAccessState>(AdminAccessState.Checking)
    val accessState: StateFlow<AdminAccessState> = _accessState.asStateFlow()

    private val _isAuthorized = MutableStateFlow(false)

    private val _feedback = MutableStateFlow<AdminFeedbackBanner?>(null)
    val feedback: StateFlow<AdminFeedbackBanner?> = _feedback.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    // Real-time Firestore collections — subscribed ONLY after _isAuthorized becomes true
    val allowedStudents: StateFlow<List<AllowedStudent>> = _isAuthorized
        .flatMapLatest { ok -> if (!ok) emptyFlow() else repository.observeAllAllowedStudents() }
        .catch { e ->
            Log.w(ADMIN_VM_TAG, "allowedStudents stream error", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(ADMIN_STOP_TIMEOUT_MS), emptyList())

    val userProfiles: StateFlow<List<UserProfile>> = _isAuthorized
        .flatMapLatest { ok -> if (!ok) emptyFlow() else repository.observeAllUserProfiles() }
        .catch { e ->
            Log.w(ADMIN_VM_TAG, "userProfiles stream error", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(ADMIN_STOP_TIMEOUT_MS), emptyList())

    val subscriptionRecords: StateFlow<List<SubscriptionRecord>> = _isAuthorized
        .flatMapLatest { ok -> if (!ok) emptyFlow() else repository.observeAllSubscriptionRecords() }
        .catch { e ->
            Log.w(ADMIN_VM_TAG, "subscriptionRecords stream error", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(ADMIN_STOP_TIMEOUT_MS), emptyList())

    val quizResults: StateFlow<List<QuizResultRecord>> = _isAuthorized
        .flatMapLatest { ok -> if (!ok) emptyFlow() else repository.observeAllQuizResults() }
        .catch { e ->
            Log.w(ADMIN_VM_TAG, "quizResults stream error", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(ADMIN_STOP_TIMEOUT_MS), emptyList())

    val questions: StateFlow<List<QuestionItem>> = _isAuthorized
        .flatMapLatest { ok -> if (!ok) emptyFlow() else repository.observeAllQuestions() }
        .catch { e ->
            Log.w(ADMIN_VM_TAG, "questions stream error", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(ADMIN_STOP_TIMEOUT_MS), emptyList())

    val leaderboardEntries: StateFlow<List<LeaderboardEntry>> = _isAuthorized
        .flatMapLatest { ok -> if (!ok) emptyFlow() else repository.observeAllLeaderboardEntries() }
        .catch { e ->
            Log.w(ADMIN_VM_TAG, "leaderboardEntries stream error", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(ADMIN_STOP_TIMEOUT_MS), emptyList())

    val winnersHistory: StateFlow<List<WinnerRecord>> = _isAuthorized
        .flatMapLatest { ok -> if (!ok) emptyFlow() else repository.observeAllWinners() }
        .catch { e ->
            Log.w(ADMIN_VM_TAG, "winnersHistory stream error", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(ADMIN_STOP_TIMEOUT_MS), emptyList())

    val banners: StateFlow<List<BannerItem>> = _isAuthorized
        .flatMapLatest { ok -> if (!ok) emptyFlow() else repository.observeAllBanners() }
        .catch { e ->
            Log.w(ADMIN_VM_TAG, "banners stream error", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(ADMIN_STOP_TIMEOUT_MS), emptyList())

    val prizes: StateFlow<List<PrizeItem>> = _isAuthorized
        .flatMapLatest { ok -> if (!ok) emptyFlow() else repository.observeAllPrizes() }
        .catch { e ->
            Log.w(ADMIN_VM_TAG, "prizes stream error", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(ADMIN_STOP_TIMEOUT_MS), emptyList())

    val announcements: StateFlow<List<AnnouncementItem>> = _isAuthorized
        .flatMapLatest { ok -> if (!ok) emptyFlow() else repository.observeAllAnnouncements() }
        .catch { e ->
            Log.w(ADMIN_VM_TAG, "announcements stream error", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(ADMIN_STOP_TIMEOUT_MS), emptyList())

    val supportThreads: StateFlow<List<SupportThread>> = _isAuthorized
        .flatMapLatest { ok -> if (!ok) emptyFlow() else repository.observeAllSupportThreads() }
        .catch { e ->
            Log.w(ADMIN_VM_TAG, "supportThreads stream error", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(ADMIN_STOP_TIMEOUT_MS), emptyList())

    val appConfig: StateFlow<AppConfig> = _isAuthorized
        .flatMapLatest { ok -> if (!ok) emptyFlow() else repository.observeAppConfig() }
        .catch { emit(AppConfig()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(ADMIN_STOP_TIMEOUT_MS), AppConfig())

    val auditLogs: StateFlow<List<AdminAuditLog>> = _isAuthorized
        .flatMapLatest { ok -> if (!ok) emptyFlow() else repository.observeAuditLogs() }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(ADMIN_STOP_TIMEOUT_MS), emptyList())

    /**
     * Real-time Dashboard Statistics computed 100% from live Firestore collections.
     * Never invents or simulates statistics; empty collections show 0.
     */
    val dashboardStats: StateFlow<AdminDashboardStats> = combine(
        combine(allowedStudents, userProfiles, quizResults) { students, profiles, results ->
            Triple(students, profiles, results)
        },
        combine(questions, subscriptionRecords, supportThreads) { qs, subs, threads ->
            Triple(qs, subs, threads)
        }
    ) { (students, profiles, results), (qs, subs, threads) ->
        val nowMs = System.currentTimeMillis()
        val todayKey = CurriculumCatalog.currentDayKey(nowMs)

        val studentProfilesOnly = profiles.filterNot { isPreAuthorizedMainAdminEmail(it.email) }
        val distinctStudentIdentities = buildSet {
            students.forEach { s ->
                val e = s.email.trim().lowercase()
                if (e.isNotEmpty()) add(e)
            }
            studentProfilesOnly.forEach { p ->
                val e = p.email.trim().lowercase()
                if (e.isNotEmpty()) add(e) else if (p.userId.isNotEmpty()) add(p.userId)
            }
        }

        val activeTodayUids = buildSet {
            profiles.filter { it.lastActiveDate == todayKey }.forEach { p ->
                val uid = p.userId.ifBlank { p.id }
                if (uid.isNotBlank()) add(uid)
            }
            results.filter { it.dateKey == todayKey }.forEach { r ->
                if (r.userId.isNotBlank()) add(r.userId)
            }
        }

        AdminDashboardStats(
            totalStudents = distinctStudentIdentities.size,
            activeSubscriptions = students.count { it.isSubscriptionCurrentlyValid(nowMs) },
            expiredSubscriptions = students.count { !it.isBanned && !it.isSubscriptionCurrentlyValid(nowMs) },
            bannedStudents = students.count { it.isBanned },
            totalQuizAttempts = results.size,
            totalQuestions = qs.size,
            publishedQuestions = qs.count { it.isPublished },
            totalPoints = profiles.sumOf { it.totalPoints },
            todaysActiveUsers = activeTodayUids.size,
            pendingRequestsCount = subs.count { it.status == "pending" },
            unreadSupportCount = threads.count { it.unreadByAdmin }
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(ADMIN_STOP_TIMEOUT_MS),
        AdminDashboardStats()
    )

    init {
        verifyMainAdminAccess()
    }

    fun verifyMainAdminAccess() {
        viewModelScope.launch {
            _accessState.value = AdminAccessState.Checking
            val res = repository.verifyAndSyncMainAdminSession(
                uid = currentAdminUid,
                rawEmail = currentAdminEmail,
                displayName = currentAdminName,
                isEmailVerified = isEmailVerified
            )
            res.fold(
                onSuccess = { member ->
                    _isAuthorized.value = true
                    _accessState.value = AdminAccessState.Authorized(member)
                },
                onFailure = { err ->
                    _isAuthorized.value = false
                    val isStudentOrUnauthorized = !isPreAuthorizedMainAdminEmail(currentAdminEmail)
                    _accessState.value = AdminAccessState.Denied(
                        email = currentAdminEmail,
                        reasonTitle = if (isStudentOrUnauthorized) {
                            "Unauthorized Administrator Account"
                        } else {
                            "Admin Verification Failed"
                        },
                        reasonDetail = err.localizedMessage
                            ?: "Access to TL Brain Test Main Admin Panel (/admin) is strictly restricted to the two pre-authorized Main Admin Google accounts."
                    )
                }
            )
        }
    }

    fun clearFeedback() {
        _feedback.value = null
    }

    fun observeSupportMessagesForStudent(studentUid: String): Flow<List<SupportMessage>> {
        if (!_isAuthorized.value || studentUid.isBlank()) return emptyFlow()
        return repository.observeThreadMessages(studentUid)
    }

    private fun runAdminMutation(
        successText: String,
        block: suspend () -> Result<Unit>
    ) {
        if (!_isAuthorized.value) return
        viewModelScope.launch {
            _isBusy.value = true
            val res = block()
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    _feedback.value = AdminFeedbackBanner(message = successText, isError = false)
                },
                onFailure = { e ->
                    _feedback.value = AdminFeedbackBanner(
                        message = e.localizedMessage ?: "Database operation failed. Check permissions or input values.",
                        isError = true
                    )
                }
            )
        }
    }

    // --- Student Management Actions ---

    fun saveOrApproveStudentAccount(
        email: String,
        studentName: String,
        studentClass: Int,
        preferredLanguage: String,
        subscriptionActive: Boolean,
        paymentMethod: String,
        startMs: Long,
        endMs: Long,
        adminNotes: String
    ) {
        runAdminMutation("Saved student account '$email' in Firestore.") {
            repository.saveStudentAccount(
                email = email,
                studentName = studentName,
                studentClass = studentClass,
                preferredLanguage = preferredLanguage,
                subscriptionActive = subscriptionActive,
                paymentMethod = paymentMethod,
                startMs = startMs,
                endMs = endMs,
                adminNotes = adminNotes
            )
        }
    }

    fun updateStudentLifecycle(
        student: AllowedStudent,
        action: String,
        reason: String
    ) {
        runAdminMutation("Updated account status ($action) for ${student.email}.") {
            repository.updateStudentLifecycleState(student, action, reason)
        }
    }

    fun adjustStudentPoints(
        profile: UserProfile,
        pointsDelta: Int,
        reason: String
    ) {
        val sign = if (pointsDelta > 0) "+$pointsDelta" else "$pointsDelta"
        runAdminMutation("Adjusted points ($sign) for ${profile.displayName} and logged audit entry.") {
            repository.adjustStudentPointsWithReason(profile, pointsDelta, reason)
        }
    }

    fun toggleStudentBadge(
        profile: UserProfile,
        badgeId: String,
        grant: Boolean,
        reason: String
    ) {
        val verb = if (grant) "Granted" else "Revoked"
        runAdminMutation("$verb badge '$badgeId' for ${profile.displayName}.") {
            repository.toggleStudentBadge(profile, badgeId, grant, reason)
        }
    }

    // --- Subscription Management Actions ---

    fun reviewPendingSubscriptionRequest(
        record: SubscriptionRecord,
        approve: Boolean,
        paymentMethod: String,
        startMs: Long,
        endMs: Long,
        verificationNotes: String
    ) {
        val label = if (approve) "Approved Rs. 50 / 3-Month pass" else "Rejected request"
        runAdminMutation("$label for ${record.studentEmail}.") {
            repository.reviewSubscriptionRequest(
                record = record,
                approve = approve,
                paymentMethod = paymentMethod,
                startMs = startMs,
                endMs = endMs,
                internalVerificationNotes = verificationNotes
            )
        }
    }

    fun recordDirectManualPayment(
        email: String,
        studentName: String,
        studentClass: Int,
        paymentMethod: String,
        startMs: Long,
        endMs: Long,
        referenceCode: String,
        internalNotes: String
    ) {
        runAdminMutation("Recorded verified manual ${paymentMethod.uppercase()} payment and activated $email.") {
            repository.recordDirectManualSubscription(
                email = email,
                studentName = studentName,
                studentClass = studentClass,
                paymentMethod = paymentMethod,
                startMs = startMs,
                endMs = endMs,
                referenceCode = referenceCode,
                internalNotes = internalNotes
            )
        }
    }

    fun extendOrExpireStudentSubscription(
        student: AllowedStudent,
        newStartMs: Long,
        newEndMs: Long,
        markActive: Boolean,
        paymentMethod: String,
        internalNotes: String
    ) {
        val msg = if (markActive) {
            "Updated subscription validity dates for ${student.email}."
        } else {
            "Expired subscription for ${student.email}. Student profile and historical records are preserved."
        }
        runAdminMutation(msg) {
            repository.extendOrExpireSubscription(
                student = student,
                newStartMs = newStartMs,
                newEndMs = newEndMs,
                markActive = markActive,
                paymentMethod = paymentMethod,
                internalNotes = internalNotes
            )
        }
    }

    // --- Content & Config Actions ---

    fun saveGlobalConfig(config: AppConfig, summary: String) {
        runAdminMutation("Saved global platform configuration ($summary).") {
            repository.saveAppConfig(config, summary)
        }
    }

    fun saveBannerItem(
        existingId: String?,
        title: String,
        subtitle: String,
        imageUrl: String,
        actionUrl: String,
        isActive: Boolean
    ) {
        runAdminMutation("Saved dashboard banner '$title'.") {
            repository.saveBanner(existingId, title, subtitle, imageUrl, actionUrl, isActive)
        }
    }

    fun deleteBannerItem(bannerId: String, title: String) {
        runAdminMutation("Deleted dashboard banner '$title'.") {
            repository.deleteBanner(bannerId, title)
        }
    }

    fun savePrizeItem(
        existingId: String?,
        rankLabel: String,
        title: String,
        description: String,
        imageUrl: String,
        isActive: Boolean
    ) {
        runAdminMutation("Saved gift/reward item '$title'.") {
            repository.savePrize(existingId, rankLabel, title, description, imageUrl, isActive)
        }
    }

    fun deletePrizeItem(prizeId: String, title: String) {
        runAdminMutation("Deleted gift/reward item '$title'.") {
            repository.deletePrize(prizeId, title)
        }
    }

    fun saveAnnouncementItem(
        existingId: String?,
        title: String,
        message: String,
        category: String,
        targetClass: Int,
        isActive: Boolean
    ) {
        runAdminMutation("Saved $category '$title'.") {
            repository.saveAnnouncement(existingId, title, message, category, targetClass, isActive)
        }
    }

    fun deleteAnnouncementItem(announcementId: String, title: String) {
        runAdminMutation("Deleted notice '$title'.") {
            repository.deleteAnnouncement(announcementId, title)
        }
    }

    // --- Leaderboard Management Actions ---

    fun archiveWinnersAndResetMonthlyPoints(
        archiveMonthLabel: String,
        defaultPrizeLabel: String
    ) {
        if (!_isAuthorized.value) return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.resetMonthlyLeaderboardPreservingHistory(
                currentConfig = appConfig.value,
                archiveMonthLabel = archiveMonthLabel,
                defaultPrizeLabel = defaultPrizeLabel
            )
            _isBusy.value = false
            res.fold(
                onSuccess = { count ->
                    _feedback.value = AdminFeedbackBanner(
                        message = "Archived $count top monthly winners and reset monthlyXp to 0. All historical records are preserved.",
                        isError = false
                    )
                },
                onFailure = { e ->
                    _feedback.value = AdminFeedbackBanner(
                        message = e.localizedMessage ?: "Failed to reset monthly leaderboard.",
                        isError = true
                    )
                }
            )
        }
    }

    fun addManualPreviousWinner(
        monthLabel: String,
        winnerName: String,
        studentClass: Int,
        rank: Int,
        xpScore: Long,
        prizeWon: String
    ) {
        runAdminMutation("Recorded previous winner '$winnerName' ($monthLabel).") {
            repository.saveManualWinner(monthLabel, winnerName, studentClass, rank, xpScore, prizeWon)
        }
    }

    fun toggleWinnerPublished(winner: WinnerRecord, publish: Boolean) {
        runAdminMutation("Updated winner visibility for ${winner.winnerName}.") {
            repository.toggleWinnerVisibility(winner, publish)
        }
    }

    // --- Support Inbox Actions ---

    fun markSupportThreadReadState(studentUid: String, unreadByAdmin: Boolean) {
        if (!_isAuthorized.value || studentUid.isBlank()) return
        viewModelScope.launch {
            repository.setSupportThreadUnreadByAdmin(studentUid, unreadByAdmin)
        }
    }

    fun sendAdminSupportReply(thread: SupportThread, replyText: String) {
        val senderLabel = currentAdminName.ifBlank { "Main Admin (Pritam Choudhary)" }
        runAdminMutation("Sent support reply to ${thread.studentName}.") {
            repository.replyToSupportThread(thread, senderLabel, replyText)
        }
    }
}
