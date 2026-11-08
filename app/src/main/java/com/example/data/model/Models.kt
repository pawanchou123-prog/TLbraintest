package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

const val OWNER_ADMIN_EMAIL = "choudharyriti92@gmail.com"
const val SECOND_ADMIN_EMAIL = "pawanchou123@gmail.com"

val AUTHORIZED_MAIN_ADMIN_EMAILS: Set<String> = setOf(
    OWNER_ADMIN_EMAIL,
    SECOND_ADMIN_EMAIL
)

fun isPreAuthorizedMainAdminEmail(email: String?): Boolean {
    if (email.isNullOrBlank()) return false
    return email.trim().lowercase() in AUTHORIZED_MAIN_ADMIN_EMAILS
}

fun sanitizeEmailKey(email: String): String {
    val clean = email.trim().lowercase().replace(Regex("[^a-z0-9_-]"), "_")
    return clean.take(120).ifBlank { "unknown_email" }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class AppPortal {
    STUDENT,
    MAIN_ADMIN,
    QUESTION_STUDIO
}

@IgnoreExtraProperties
data class RoleMember(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = "question_manager",
    val emailKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AllowedQuestionManager(
    @DocumentId val id: String = "",
    val email: String = "",
    val managerName: String = "",
    @get:PropertyName("isActive") val isActive: Boolean = true,
    val canPublishDirectly: Boolean = false,
    val canDeleteQuestions: Boolean = true,
    val addedByAdminEmail: String = "",
    val notes: String = "",
    val linkedUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SubjectCatalogDoc(
    @DocumentId val id: String = "global",
    val classes1To5: List<String> = listOf(
        "Mathematics", "English", "Hindi", "EVS", "General Knowledge"
    ),
    val classes6To8: List<String> = listOf(
        "Mathematics", "Science", "English", "Hindi", "Social Science", "General Knowledge"
    ),
    val classes9To10: List<String> = listOf(
        "Mathematics", "Science", "Social Science", "English", "Hindi"
    ),
    val classes11To12: List<String> = listOf(
        "Physics", "Chemistry", "Biology", "Mathematics",
        "Accountancy", "Business Studies", "Economics",
        "History", "Geography", "Political Science", "English"
    ),
    val updatedByEmail: String = OWNER_ADMIN_EMAIL,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class QuestionAuditLog(
    @DocumentId val id: String = "",
    val editorUid: String = "",
    val editorEmail: String = "",
    val editorRole: String = "question_manager", // main_admin | question_manager
    val actionType: String = "",
    val questionId: String = "",
    val questionSummary: String = "",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class QuestionDashboardStats(
    val totalQuestions: Int = 0,
    val publishedQuestions: Int = 0,
    val draftQuestions: Int = 0,
    val pendingReviewQuestions: Int = 0,
    val rejectedQuestions: Int = 0,
    val byClassCounts: Map<Int, Int> = emptyMap(),
    val bySubjectCounts: Map<String, Int> = emptyMap(),
    val recentQuestions: List<QuestionItem> = emptyList()
)

@IgnoreExtraProperties
data class AllowedStudent(
    @DocumentId val id: String = "",
    val email: String = "",
    val studentName: String = "",
    val studentClass: Long = 10L,
    val preferredLanguage: String = "en",
    @get:PropertyName("isBanned") val isBanned: Boolean = false,
    val subscriptionActive: Boolean = true,
    val subscriptionPlan: String = "Rs. 50 / 3 Months",
    val paymentMethod: String = "upi",
    val subscriptionStartMs: Long = 0L,
    val subscriptionEndMs: Long = 0L,
    val adminNotes: String? = null,
    val linkedUid: String? = null,
    val lastActiveSummary: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isSubscriptionCurrentlyValid(nowMs: Long = System.currentTimeMillis()): Boolean {
        return !isBanned && subscriptionActive && subscriptionEndMs > nowMs
    }
}

@IgnoreExtraProperties
data class SubscriptionRecord(
    @DocumentId val id: String = "",
    val studentEmail: String = "",
    val studentName: String = "",
    val studentClass: Long = 10L,
    val studentUid: String = "",
    val amountInr: Long = 50L,
    val planLabel: String = "Rs. 50 / 3 Months",
    val paymentMethod: String = "upi", // upi | cash | access_request
    val referenceCode: String = "",
    val status: String = "pending", // pending | approved | rejected | expired
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val verificationNotes: String = "",
    val verifiedByAdminEmail: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AdminAuditLog(
    @DocumentId val id: String = "",
    val adminUid: String = "",
    val adminEmail: String = "",
    val actionType: String = "",
    val targetId: String = "",
    val targetLabel: String = "",
    val reasonOrDetails: String = "",
    val pointsDelta: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class AdminDashboardStats(
    val totalStudents: Int = 0,
    val activeSubscriptions: Int = 0,
    val expiredSubscriptions: Int = 0,
    val bannedStudents: Int = 0,
    val totalQuizAttempts: Int = 0,
    val totalQuestions: Int = 0,
    val publishedQuestions: Int = 0,
    val totalPoints: Long = 0L,
    val todaysActiveUsers: Int = 0,
    val pendingRequestsCount: Int = 0,
    val unreadSupportCount: Int = 0
)

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val studentClass: Long = 10L,
    val schoolName: String = "",
    val avatarId: String = "cyber_brain",
    val language: String = "en",
    val xp: Long = 0L,
    val totalPoints: Long = 0L,
    val level: Long = 1L,
    val quizzesPlayed: Long = 0L,
    val gamesPlayed: Long = 0L,
    val totalQuestionsAnswered: Long = 0L,
    val totalCorrectAnswers: Long = 0L,
    val badges: List<String> = emptyList(),
    val soundEnabled: Boolean = true,
    val termsAccepted: Boolean = true,
    val lastActiveDate: String = "2026-11-03",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val accuracyPercent: Int
        get() = if (totalQuestionsAnswered <= 0L) 0
        else ((totalCorrectAnswers.toDouble() / totalQuestionsAnswered.toDouble()) * 100.0).toInt().coerceIn(0, 100)
}

@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val studentClass: Long = 10L,
    val dailyXp: Long = 0L,
    val dailyKey: String = "",
    val weeklyXp: Long = 0L,
    val weeklyKey: String = "",
    val monthlyXp: Long = 0L,
    val monthlyKey: String = "",
    val allTimeXp: Long = 0L,
    val level: Long = 1L,
    val accuracyPercent: Long = 0L,
    @get:PropertyName("isActiveBoard") val isActiveBoard: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class QuestionItem(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorEmail: String = "",
    val studentClass: Long = 10L, // 0L = GK (All Classes), 1L..12L = School Classes
    val subject: String = "Mathematics",
    val topic: String = "",
    val language: String = "en",
    val questionText: String = "",
    val options: List<String> = listOf("", "", "", ""),
    val correctIndex: Long = 0L,
    val explanation: String = "",
    val difficulty: String = "medium",
    val points: Long = 10L,
    val timerSeconds: Long = 30L,
    val imageUrl: String = "",
    @get:PropertyName("isPublished") val isPublished: Boolean = false,
    val reviewStatus: String = "approved", // draft | pending_review | approved | rejected
    val reviewNotes: String = "",
    val lastEditedByUid: String = "",
    val lastEditedByEmail: String = "",
    val source: String = "manual", // manual | gemini_ai | csv_import
    val attemptsCount: Long = 0L,
    val correctCount: Long = 0L,
    val totalResponseTimeSec: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val effectiveReviewStatus: String
        get() = when {
            isPublished -> "approved"
            reviewStatus in setOf("draft", "pending_review", "rejected", "approved") -> {
                if (reviewStatus == "approved") "draft" else reviewStatus
            }
            else -> "draft"
        }

    val normalizedDuplicateKey: String
        get() = "${studentClass}_${language.trim().lowercase()}_${questionText.trim().lowercase().replace(Regex("\\s+"), " ")}"

    val accuracyRatePercent: Int
        get() = if (attemptsCount <= 0L) 0
        else ((correctCount.toDouble() / attemptsCount.toDouble()) * 100.0).toInt().coerceIn(0, 100)

    val avgResponseSeconds: Int
        get() = if (attemptsCount <= 0L) 0
        else (totalResponseTimeSec / attemptsCount).toInt()
}

@IgnoreExtraProperties
data class QuizResultRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val activityType: String = "quiz", // quiz | gk | game
    val title: String = "",
    val studentClass: Long = 10L,
    val score: Long = 0L,
    val totalQuestions: Long = 1L,
    val correctAnswers: Long = 0L,
    val accuracyPercent: Long = 0L,
    val xpEarned: Long = 0L,
    val durationSeconds: Long = 0L,
    val dateKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SupportThread(
    @DocumentId val id: String = "",
    val studentUid: String = "",
    val studentName: String = "",
    val studentEmail: String = "",
    val studentClass: Long = 10L,
    val lastMessage: String = "",
    val unreadByAdmin: Boolean = true,
    val unreadByStudent: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SupportMessage(
    @DocumentId val id: String = "",
    val studentUid: String = "",
    val senderUid: String = "",
    val senderRole: String = "student",
    val senderName: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppConfig(
    @DocumentId val id: String = "global",
    val leaderboardEnabled: Boolean = true,
    val currentMonthKey: String = "2026-11",
    val youtubeUrl: String = "https://www.youtube.com",
    val whatsappUrl: String = "https://wa.me/",
    val upiId: String = "Contact Admin for UPI / Cash",
    val subscriptionPriceText: String = "Rs. 50 for 3 Months",
    val supportContactText: String = "Contact Pritam Choudhary via WhatsApp or Support to activate your 3-month student pass (Rs. 50).",
    val appVersionBanner: String = "TL BRAIN TEST v1.0 — Learn, Think, Play, Improve.",
    val termsAndConditionsEn: String = "",
    val termsAndConditionsHi: String = "",
    val monthlyResetDay: Long = 1L,
    val badgeRewardConfigNotes: String = "Age-appropriate educational certificates, books, and study rewards.",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BannerItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val imageUrl: String = "",
    val actionUrl: String = "",
    @get:PropertyName("isActive") val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PrizeItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val rankLabel: String = "Rank 1",
    val description: String = "",
    val imageUrl: String = "",
    @get:PropertyName("isActive") val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AnnouncementItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val category: String = "announcement", // announcement | notice | notification | update
    val targetClass: Long = 0L,
    @get:PropertyName("isActive") val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WinnerRecord(
    @DocumentId val id: String = "",
    val monthLabel: String = "",
    val winnerName: String = "",
    val studentClass: Long = 10L,
    val rank: Long = 1L,
    val xpScore: Long = 0L,
    val prizeWon: String = "",
    @get:PropertyName("isPublished") val isPublished: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)
