package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.CurriculumCatalog
import com.example.data.GeneratedQuestionDraft
import com.example.data.model.AllowedStudent
import com.example.data.model.AnnouncementItem
import com.example.data.model.AppConfig
import com.example.data.model.BannerItem
import com.example.data.model.LeaderboardEntry
import com.example.data.model.PrizeItem
import com.example.data.model.QuestionItem
import com.example.data.model.QuizResultRecord
import com.example.data.model.RoleMember
import com.example.data.model.SupportMessage
import com.example.data.model.SupportThread
import com.example.data.model.UserProfile
import com.example.data.model.WinnerRecord
import com.example.data.model.sanitizeEmailKey
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class TlBrainRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- Role & Whitelist Checks ---

    suspend fun isQuestionManagerRole(uid: String): Boolean {
        val path = "question_managers/$uid"
        return try {
            db.collection("question_managers").document(uid).get().await().exists()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            false
        }
    }

    suspend fun isExtraAdminRole(uid: String): Boolean {
        val path = "admins/$uid"
        return try {
            db.collection("admins").document(uid).get().await().exists()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            false
        }
    }

    fun observeAllowedStudentByEmail(email: String): Flow<AllowedStudent?> {
        val key = sanitizeEmailKey(email)
        val path = "allowed_students/$key"
        return db.collection("allowed_students").document(key)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(AllowedStudent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.GET, path)
                emit(null)
            }
    }

    suspend fun getAllowedStudentOnce(email: String): AllowedStudent? {
        val key = sanitizeEmailKey(email)
        val path = "allowed_students/$key"
        return try {
            val snap = db.collection("allowed_students").document(key).get().await()
            if (snap.exists()) {
                snap.toObject(AllowedStudent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            null
        }
    }

    // --- Student User Profile ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.GET, path)
                throw e
            }
    }

    suspend fun ensureUserProfileInitialized(
        uid: String,
        email: String,
        fallbackName: String,
        studentClass: Int,
        preferredLang: String
    ): Result<UserProfile> {
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (snap.exists()) {
                val existing = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: UserProfile(id = uid, userId = uid, email = email, displayName = fallbackName)
                Result.success(existing)
            } else {
                val safeName = fallbackName.trim().ifBlank { "Student" }.take(80)
                val safeClass = studentClass.coerceIn(1, 12).toLong()
                val safeLang = if (preferredLang == "hi") "hi" else "en"
                val today = CurriculumCatalog.currentDayKey()
                val payload = mapOf(
                    "userId" to uid,
                    "email" to email.trim().lowercase().take(120),
                    "displayName" to safeName,
                    "studentClass" to safeClass,
                    "schoolName" to "",
                    "avatarId" to "cyber_brain",
                    "language" to safeLang,
                    "xp" to 0L,
                    "totalPoints" to 0L,
                    "level" to 1L,
                    "quizzesPlayed" to 0L,
                    "gamesPlayed" to 0L,
                    "totalQuestionsAnswered" to 0L,
                    "totalCorrectAnswers" to 0L,
                    "badges" to emptyList<String>(),
                    "soundEnabled" to true,
                    "termsAccepted" to true,
                    "lastActiveDate" to today,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()

                // Also initialize PII-free leaderboard entry
                val lbPayload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "studentClass" to safeClass,
                    "dailyXp" to 0L,
                    "dailyKey" to today,
                    "weeklyXp" to 0L,
                    "weeklyKey" to CurriculumCatalog.currentWeekKey(),
                    "monthlyXp" to 0L,
                    "monthlyKey" to CurriculumCatalog.currentMonthKey(),
                    "allTimeXp" to 0L,
                    "level" to 1L,
                    "accuracyPercent" to 0L,
                    "isActiveBoard" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("leaderboard_entries").document(uid).set(lbPayload).await()

                val created = docRef.get().await()
                    .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: UserProfile(id = uid, userId = uid, email = email, displayName = safeName, studentClass = safeClass, language = safeLang)
                Result.success(created)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfilePreferences(
        profile: UserProfile,
        newName: String,
        newClass: Int,
        newLanguage: String,
        soundEnabled: Boolean,
        newSchoolName: String = profile.schoolName,
        newAvatarId: String = profile.avatarId,
        termsAccepted: Boolean = profile.termsAccepted
    ): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            val safeName = newName.trim().ifBlank { profile.displayName }.take(80)
            val safeClass = newClass.coerceIn(1, 12).toLong()
            val safeSchool = newSchoolName.trim().take(120)
            val safeAvatar = newAvatarId.trim().ifBlank { "cyber_brain" }.take(40)
            val safeLang = if (newLanguage == "hi") "hi" else "en"
            db.collection("users").document(uid).update(
                mapOf(
                    "displayName" to safeName,
                    "studentClass" to safeClass,
                    "schoolName" to safeSchool,
                    "avatarId" to safeAvatar,
                    "language" to safeLang,
                    "soundEnabled" to soundEnabled,
                    "termsAccepted" to termsAccepted,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val lbRef = db.collection("leaderboard_entries").document(uid)
            if (lbRef.get().await().exists()) {
                lbRef.update(
                    mapOf(
                        "displayName" to safeName,
                        "studentClass" to safeClass,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- Recording Quiz & 2D Game Completion + XP, Levels, Badges & Leaderboards ---

    suspend fun recordActivityCompletion(
        profile: UserProfile,
        activityType: String, // "quiz" | "gk" | "game"
        title: String,
        studentClass: Int,
        score: Int,
        totalQuestions: Int,
        correctAnswers: Int,
        xpEarned: Int,
        durationSeconds: Int,
        questionAttempts: List<Pair<QuestionItem, Pair<Boolean, Int>>> = emptyList()
    ): Result<Unit> {
        val uid = requireUserId()
        val resultId = UUID.randomUUID().toString()
        val resultPath = "quiz_results/$resultId"
        return try {
            val safeTotal = totalQuestions.coerceAtLeast(1)
            val safeCorrect = correctAnswers.coerceIn(0, safeTotal)
            val accPercent = ((safeCorrect.toDouble() / safeTotal.toDouble()) * 100.0).toInt().coerceIn(0, 100)
            val safeXp = xpEarned.coerceAtLeast(0).toLong()
            val todayKey = CurriculumCatalog.currentDayKey()
            val weekKey = CurriculumCatalog.currentWeekKey()
            val monthKey = CurriculumCatalog.currentMonthKey()

            // 1. Write immutable QuizResultRecord
            val resultPayload = mapOf(
                "userId" to uid,
                "activityType" to activityType,
                "title" to title.trim().ifBlank { "Practice" }.take(100),
                "studentClass" to studentClass.coerceIn(0, 12).toLong(),
                "score" to score.coerceAtLeast(0).toLong(),
                "totalQuestions" to safeTotal.toLong(),
                "correctAnswers" to safeCorrect.toLong(),
                "accuracyPercent" to accPercent.toLong(),
                "xpEarned" to safeXp,
                "durationSeconds" to durationSeconds.coerceAtLeast(0).toLong(),
                "dateKey" to todayKey,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("quiz_results").document(resultId).set(resultPayload).await()

            // 2. Update UserProfile XP, Total Points, Level, Accuracy & Badges
            val newXp = profile.xp + safeXp
            val newPoints = profile.totalPoints + score.coerceAtLeast(0).toLong()
            val newLevel = (newXp / 200L) + 1L
            val newQuizzes = if (activityType == "quiz" || activityType == "gk") profile.quizzesPlayed + 1L else profile.quizzesPlayed
            val newGames = if (activityType == "game") profile.gamesPlayed + 1L else profile.gamesPlayed
            val newTotalAns = if (activityType == "quiz" || activityType == "gk") {
                profile.totalQuestionsAnswered + safeTotal.toLong()
            } else {
                profile.totalQuestionsAnswered
            }
            val newTotalCorr = if (activityType == "quiz" || activityType == "gk") {
                profile.totalCorrectAnswers + safeCorrect.toLong()
            } else {
                profile.totalCorrectAnswers
            }
            val newBadges = CurriculumCatalog.computeUnlockedBadges(
                xp = newXp,
                level = newLevel,
                quizzesPlayed = newQuizzes,
                gamesPlayed = newGames,
                totalAnswered = newTotalAns,
                totalCorrect = newTotalCorr,
                existing = profile.badges
            )
            val overallAcc = if (newTotalAns > 0L) ((newTotalCorr * 100L) / newTotalAns).coerceIn(0L, 100L) else accPercent.toLong()

            db.collection("users").document(uid).update(
                mapOf(
                    "xp" to newXp,
                    "totalPoints" to newPoints,
                    "level" to newLevel,
                    "quizzesPlayed" to newQuizzes,
                    "gamesPlayed" to newGames,
                    "totalQuestionsAnswered" to newTotalAns,
                    "totalCorrectAnswers" to newTotalCorr,
                    "badges" to newBadges,
                    "lastActiveDate" to todayKey,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // 3. Update PII-Free LeaderboardEntry
            val lbRef = db.collection("leaderboard_entries").document(uid)
            val lbSnap = lbRef.get().await()
            if (lbSnap.exists()) {
                val existingLb = lbSnap.toObject(LeaderboardEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                val nextDaily = if (existingLb?.dailyKey == todayKey) (existingLb.dailyXp + safeXp) else safeXp
                val nextWeekly = if (existingLb?.weeklyKey == weekKey) (existingLb.weeklyXp + safeXp) else safeXp
                val nextMonthly = if (existingLb?.monthlyKey == monthKey) (existingLb.monthlyXp + safeXp) else safeXp
                lbRef.update(
                    mapOf(
                        "displayName" to profile.displayName.ifBlank { "Student" }.take(80),
                        "studentClass" to profile.studentClass.coerceIn(1L, 12L),
                        "dailyXp" to nextDaily,
                        "dailyKey" to todayKey,
                        "weeklyXp" to nextWeekly,
                        "weeklyKey" to weekKey,
                        "monthlyXp" to nextMonthly,
                        "monthlyKey" to monthKey,
                        "allTimeXp" to newXp,
                        "level" to newLevel,
                        "accuracyPercent" to overallAcc,
                        "isActiveBoard" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                lbRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to profile.displayName.ifBlank { "Student" }.take(80),
                        "studentClass" to profile.studentClass.coerceIn(1L, 12L),
                        "dailyXp" to safeXp,
                        "dailyKey" to todayKey,
                        "weeklyXp" to safeXp,
                        "weeklyKey" to weekKey,
                        "monthlyXp" to safeXp,
                        "monthlyKey" to monthKey,
                        "allTimeXp" to newXp,
                        "level" to newLevel,
                        "accuracyPercent" to overallAcc,
                        "isActiveBoard" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // 4. Update individual published Question statistics (attempts, correct, response time)
            for ((q, outcome) in questionAttempts) {
                if (q.id.isNotBlank() && q.isPublished) {
                    val (wasCorrect, secSpent) = outcome
                    try {
                        db.collection("questions").document(q.id).update(
                            mapOf(
                                "attemptsCount" to FieldValue.increment(1L),
                                "correctCount" to FieldValue.increment(if (wasCorrect) 1L else 0L),
                                "totalResponseTimeSec" to FieldValue.increment(secSpent.coerceAtLeast(1).toLong()),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    } catch (_: Exception) {
                        // Non-fatal if question was unpublished mid-quiz
                    }
                }
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, resultPath)
            Result.failure(e)
        }
    }

    fun observeStudentResults(userId: String): Flow<List<QuizResultRecord>> {
        val path = "quiz_results"
        return db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(QuizResultRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    // --- Leaderboards ---

    fun observeActiveLeaderboard(): Flow<List<LeaderboardEntry>> {
        val path = "leaderboard_entries"
        return db.collection(path)
            .whereEqualTo("isActiveBoard", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(LeaderboardEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    fun observePublishedWinners(): Flow<List<WinnerRecord>> {
        val path = "winners_history"
        return db.collection(path)
            .whereEqualTo("isPublished", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(WinnerRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareByDescending<WinnerRecord> { it.createdAt ?: Timestamp(0, 0) }.thenBy { it.rank })
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    // --- Question Bank (Published Student Feed + Dedicated Question Studio + AI Drafts) ---

    fun observePublishedQuestions(): Flow<List<QuestionItem>> {
        val path = "questions"
        return db.collection(path)
            .whereEqualTo("isPublished", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(QuestionItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { it.isPublished && (it.reviewStatus.isBlank() || it.reviewStatus == "approved") }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    fun observeAllQuestionsForAdmin(): Flow<List<QuestionItem>> {
        val path = "questions"
        return db.collection(path)
            .snapshots()
            .map { snap ->
                snap.toObjects(QuestionItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    fun observeAuthorQuestions(authorId: String): Flow<List<QuestionItem>> {
        val path = "questions"
        return db.collection(path)
            .whereEqualTo("authorId", authorId)
            .snapshots()
            .map { snap ->
                snap.toObjects(QuestionItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    suspend fun saveQuestion(
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
        source: String = "manual"
    ): Result<String> {
        val uid = requireUserId()
        val docId = existingId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val path = "questions/$docId"
        return try {
            val cleanOptions = (0 until 4).map { idx ->
                options.getOrNull(idx)?.trim()?.ifBlank { "Option ${idx + 1}" }?.take(300) ?: "Option ${idx + 1}"
            }
            val cleanDiff = difficulty.lowercase().let { if (it in listOf("easy", "medium", "hard")) it else "medium" }
            val cleanLang = if (language == "hi") "hi" else "en"
            val cleanSource = if (source == "gemini_ai") "gemini_ai" else "manual"

            val docRef = db.collection("questions").document(docId)
            if (existingId != null && existingId.isNotBlank()) {
                docRef.update(
                    mapOf(
                        "studentClass" to studentClass.coerceIn(0, 12).toLong(),
                        "subject" to subject.trim().ifBlank { "General Knowledge" }.take(60),
                        "language" to cleanLang,
                        "questionText" to questionText.trim().take(1000),
                        "options" to cleanOptions,
                        "correctIndex" to correctIndex.coerceIn(0, 3).toLong(),
                        "explanation" to explanation.trim().take(1000),
                        "difficulty" to cleanDiff,
                        "points" to points.coerceIn(1, 1000).toLong(),
                        "timerSeconds" to timerSeconds.coerceIn(5, 300).toLong(),
                        "imageUrl" to imageUrl.trim().take(500),
                        "isPublished" to isPublished,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "authorId" to uid,
                        "studentClass" to studentClass.coerceIn(0, 12).toLong(),
                        "subject" to subject.trim().ifBlank { "General Knowledge" }.take(60),
                        "language" to cleanLang,
                        "questionText" to questionText.trim().take(1000),
                        "options" to cleanOptions,
                        "correctIndex" to correctIndex.coerceIn(0, 3).toLong(),
                        "explanation" to explanation.trim().take(1000),
                        "difficulty" to cleanDiff,
                        "points" to points.coerceIn(1, 1000).toLong(),
                        "timerSeconds" to timerSeconds.coerceIn(5, 300).toLong(),
                        "imageUrl" to imageUrl.trim().take(500),
                        "isPublished" to isPublished,
                        "source" to cleanSource,
                        "attemptsCount" to 0L,
                        "correctCount" to 0L,
                        "totalResponseTimeSec" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveGeneratedDraftBatch(
        studentClass: Int,
        subject: String,
        language: String,
        drafts: List<GeneratedQuestionDraft>
    ): Result<Int> {
        var saved = 0
        for (draft in drafts) {
            val res = saveQuestion(
                existingId = null,
                studentClass = studentClass,
                subject = subject,
                language = language,
                questionText = draft.questionText,
                options = draft.options,
                correctIndex = draft.correctIndex,
                explanation = draft.explanation,
                difficulty = draft.difficulty,
                points = draft.points,
                timerSeconds = draft.timerSeconds,
                imageUrl = "",
                isPublished = false, // Always saved as unpublished draft for mandatory Admin review
                source = "gemini_ai"
            )
            if (res.isSuccess) saved++
        }
        return if (saved > 0) Result.success(saved)
        else Result.failure(IllegalStateException("Could not save generated drafts to review queue."))
    }

    suspend fun toggleQuestionPublish(questionId: String, publish: Boolean): Result<Unit> {
        val path = "questions/$questionId"
        return try {
            db.collection("questions").document(questionId).update(
                mapOf(
                    "isPublished" to publish,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteQuestion(questionId: String): Result<Unit> {
        val path = "questions/$questionId"
        return try {
            db.collection("questions").document(questionId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Customer Support (Strict 1-to-1 Student <-> Admin) ---

    fun observeStudentSupportMessages(studentUid: String): Flow<List<SupportMessage>> {
        val path = "support_threads/$studentUid/messages"
        return db.collection("support_threads").document(studentUid).collection("messages")
            .snapshots()
            .map { snap ->
                snap.toObjects(SupportMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    fun observeAllSupportThreadsForAdmin(): Flow<List<SupportThread>> {
        val path = "support_threads"
        return db.collection(path)
            .snapshots()
            .map { snap ->
                snap.toObjects(SupportThread::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    suspend fun sendSupportMessage(
        studentUid: String,
        studentName: String,
        studentEmail: String,
        studentClass: Int,
        senderRole: String, // "student" | "admin"
        senderName: String,
        text: String
    ): Result<Unit> {
        val currentUid = requireUserId()
        val cleanText = text.trim().take(1000)
        if (cleanText.isBlank()) return Result.failure(IllegalArgumentException("Message cannot be empty."))
        val threadPath = "support_threads/$studentUid"
        return try {
            val threadRef = db.collection("support_threads").document(studentUid)
            val threadSnap = threadRef.get().await()
            if (threadSnap.exists()) {
                threadRef.update(
                    mapOf(
                        "studentName" to studentName.trim().ifBlank { "Student" }.take(80),
                        "studentClass" to studentClass.coerceIn(1, 12).toLong(),
                        "lastMessage" to cleanText.take(500),
                        "unreadByAdmin" to (senderRole == "student"),
                        "unreadByStudent" to (senderRole == "admin"),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                threadRef.set(
                    mapOf(
                        "studentUid" to studentUid,
                        "studentName" to studentName.trim().ifBlank { "Student" }.take(80),
                        "studentEmail" to studentEmail.trim().ifBlank { "student@gmail.com" }.take(120),
                        "studentClass" to studentClass.coerceIn(1, 12).toLong(),
                        "lastMessage" to cleanText.take(500),
                        "unreadByAdmin" to (senderRole == "student"),
                        "unreadByStudent" to (senderRole == "admin"),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val msgId = UUID.randomUUID().toString()
            threadRef.collection("messages").document(msgId).set(
                mapOf(
                    "studentUid" to studentUid,
                    "senderUid" to currentUid,
                    "senderRole" to if (senderRole == "admin") "admin" else "student",
                    "senderName" to senderName.trim().ifBlank { "User" }.take(80),
                    "text" to cleanText,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, threadPath)
            Result.failure(e)
        }
    }

    // --- Admin Controls: Student Accounts, Subscriptions, Banners, Prizes, Announcements, Config ---

    fun observeAllAllowedStudents(): Flow<List<AllowedStudent>> {
        val path = "allowed_students"
        return db.collection(path)
            .snapshots()
            .map { snap ->
                snap.toObjects(AllowedStudent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    fun observeAllUsersForAdmin(): Flow<List<UserProfile>> {
        val path = "users"
        return db.collection(path)
            .snapshots()
            .map { snap ->
                snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    fun observeAllResultsForAdmin(): Flow<List<QuizResultRecord>> {
        val path = "quiz_results"
        return db.collection(path)
            .snapshots()
            .map { snap ->
                snap.toObjects(QuizResultRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    suspend fun saveAllowedStudent(
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
    ): Result<Unit> {
        val cleanEmail = email.trim().lowercase().take(120)
        if (!cleanEmail.contains("@")) return Result.failure(IllegalArgumentException("Enter a valid student Gmail address."))
        val key = sanitizeEmailKey(cleanEmail)
        val path = "allowed_students/$key"
        return try {
            val docRef = db.collection("allowed_students").document(key)
            val snap = docRef.get().await()
            val payload = mutableMapOf<String, Any>(
                "email" to cleanEmail,
                "studentName" to studentName.trim().ifBlank { "Student" }.take(80),
                "studentClass" to studentClass.coerceIn(1, 12).toLong(),
                "preferredLanguage" to if (preferredLanguage == "hi") "hi" else "en",
                "isBanned" to isBanned,
                "subscriptionActive" to subscriptionActive,
                "subscriptionPlan" to subscriptionPlan.trim().ifBlank { "Rs. 50 / 3 Months" }.take(80),
                "paymentMethod" to paymentMethod.lowercase().let { if (it in listOf("upi", "cash", "manual")) it else "upi" },
                "subscriptionStartMs" to subscriptionStartMs.coerceAtLeast(0L),
                "subscriptionEndMs" to subscriptionEndMs.coerceAtLeast(0L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (adminNotes.isNotBlank()) {
                payload["adminNotes"] = adminNotes.trim().take(300)
            }
            if (snap.exists()) {
                docRef.update(payload).await()
            } else {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleStudentBan(email: String, ban: Boolean): Result<Unit> {
        val key = sanitizeEmailKey(email)
        val path = "allowed_students/$key"
        return try {
            db.collection("allowed_students").document(key).update(
                mapOf(
                    "isBanned" to ban,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAllowedStudent(email: String): Result<Unit> {
        val key = sanitizeEmailKey(email)
        val path = "allowed_students/$key"
        return try {
            db.collection("allowed_students").document(key).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Global Config, Banners, Prizes, Announcements & Monthly Reset ---

    fun observeAppConfig(): Flow<AppConfig> {
        val path = "app_config/global"
        return db.collection("app_config").document("global")
            .snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(AppConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: AppConfig()
                } else {
                    AppConfig()
                }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.GET, path)
                emit(AppConfig())
            }
    }

    suspend fun saveAppConfig(config: AppConfig): Result<Unit> {
        val path = "app_config/global"
        return try {
            val docRef = db.collection("app_config").document("global")
            val exists = docRef.get().await().exists()
            val payload = mutableMapOf<String, Any>(
                "leaderboardEnabled" to config.leaderboardEnabled,
                "currentMonthKey" to config.currentMonthKey.ifBlank { CurriculumCatalog.currentMonthKey() }.take(20),
                "youtubeUrl" to config.youtubeUrl.trim().take(500),
                "whatsappUrl" to config.whatsappUrl.trim().take(500),
                "upiId" to config.upiId.trim().take(120),
                "subscriptionPriceText" to config.subscriptionPriceText.trim().ifBlank { "Rs. 50 for 3 Months" }.take(100),
                "supportContactText" to config.supportContactText.trim().take(300),
                "appVersionBanner" to config.appVersionBanner.trim().take(300),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (exists) {
                docRef.update(payload).await()
            } else {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeActiveBanners(): Flow<List<BannerItem>> {
        val path = "banners"
        return db.collection(path)
            .whereEqualTo("isActive", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(BannerItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    suspend fun addBanner(title: String, subtitle: String, imageUrl: String, actionUrl: String): Result<Unit> {
        val cleanImg = imageUrl.trim()
        if (!cleanImg.startsWith("https://")) {
            return Result.failure(IllegalArgumentException("Banner image URL must start with https://"))
        }
        val id = UUID.randomUUID().toString()
        val path = "banners/$id"
        return try {
            db.collection("banners").document(id).set(
                mapOf(
                    "title" to title.trim().ifBlank { "Featured Update" }.take(120),
                    "subtitle" to subtitle.trim().take(250),
                    "imageUrl" to cleanImg.take(500),
                    "actionUrl" to actionUrl.trim().take(500),
                    "isActive" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteBanner(bannerId: String): Result<Unit> {
        val path = "banners/$bannerId"
        return try {
            db.collection("banners").document(bannerId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeActivePrizes(): Flow<List<PrizeItem>> {
        val path = "prizes"
        return db.collection(path)
            .whereEqualTo("isActive", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(PrizeItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.rankLabel }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    suspend fun addPrize(rankLabel: String, title: String, description: String, imageUrl: String): Result<Unit> {
        val cleanImg = imageUrl.trim()
        if (!cleanImg.startsWith("https://")) {
            return Result.failure(IllegalArgumentException("Prize image URL must start with https://"))
        }
        val id = UUID.randomUUID().toString()
        val path = "prizes/$id"
        return try {
            db.collection("prizes").document(id).set(
                mapOf(
                    "rankLabel" to rankLabel.trim().ifBlank { "Rank 1" }.take(60),
                    "title" to title.trim().ifBlank { "Winner Gift" }.take(120),
                    "description" to description.trim().take(300),
                    "imageUrl" to cleanImg.take(500),
                    "isActive" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePrize(prizeId: String): Result<Unit> {
        val path = "prizes/$prizeId"
        return try {
            db.collection("prizes").document(prizeId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeActiveAnnouncements(): Flow<List<AnnouncementItem>> {
        val path = "announcements"
        return db.collection(path)
            .whereEqualTo("isActive", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(AnnouncementItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    suspend fun addAnnouncement(
        title: String,
        message: String,
        category: String,
        targetClass: Int
    ): Result<Unit> {
        val id = UUID.randomUUID().toString()
        val path = "announcements/$id"
        val cleanCat = category.lowercase().let {
            if (it in listOf("announcement", "notification", "update")) it else "announcement"
        }
        return try {
            db.collection("announcements").document(id).set(
                mapOf(
                    "title" to title.trim().ifBlank { "Important Notice" }.take(150),
                    "message" to message.trim().ifBlank { "Please check the latest update." }.take(1000),
                    "category" to cleanCat,
                    "targetClass" to targetClass.coerceIn(0, 12).toLong(),
                    "isActive" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAnnouncement(announcementId: String): Result<Unit> {
        val path = "announcements/$announcementId"
        return try {
            db.collection("announcements").document(announcementId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun resetMonthlyLeaderboardAndArchiveWinners(
        monthLabel: String,
        defaultPrizeLabel: String
    ): Result<Int> {
        val path = "leaderboard_entries"
        return try {
            val activeMonth = CurriculumCatalog.currentMonthKey()
            val snap = db.collection(path).whereEqualTo("isActiveBoard", true).get().await()
            val entries = snap.toObjects(LeaderboardEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val topScorers = entries
                .filter { it.monthlyXp > 0L }
                .sortedByDescending { it.monthlyXp }
                .take(3)

            topScorers.forEachIndexed { index, entry ->
                val winId = UUID.randomUUID().toString()
                db.collection("winners_history").document(winId).set(
                    mapOf(
                        "monthLabel" to monthLabel.trim().ifBlank { activeMonth }.take(40),
                        "winnerName" to entry.displayName.take(80),
                        "studentClass" to entry.studentClass.coerceIn(1L, 12L),
                        "rank" to (index + 1).toLong(),
                        "xpScore" to entry.monthlyXp,
                        "prizeWon" to defaultPrizeLabel.trim().ifBlank { "Monthly Champion Trophy" }.take(120),
                        "isPublished" to true,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            for (doc in snap.documents) {
                doc.reference.update(
                    mapOf(
                        "monthlyXp" to 0L,
                        "monthlyKey" to activeMonth,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(topScorers.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun assignQuestionManagerByUid(uid: String, email: String, name: String): Result<Unit> {
        val cleanUid = uid.trim()
        if (cleanUid.isBlank()) return Result.failure(IllegalArgumentException("UID is required."))
        val path = "question_managers/$cleanUid"
        return try {
            db.collection("question_managers").document(cleanUid).set(
                mapOf(
                    "uid" to cleanUid,
                    "email" to email.trim().ifBlank { "editor@gmail.com" }.take(120),
                    "name" to name.trim().ifBlank { "Question Operator" }.take(80),
                    "role" to "question_manager",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitPendingSubscriptionOrAccessRequest(
        studentUid: String,
        studentEmail: String,
        studentName: String,
        studentClass: Int,
        paymentMethod: String, // "upi" | "cash" | "access_request"
        referenceCode: String
    ): Result<Unit> {
        val cleanEmail = studentEmail.trim().lowercase().take(120)
        if (studentUid.isBlank() || !cleanEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Valid signed-in Google account required."))
        }
        val cleanMethod = paymentMethod.lowercase().let {
            if (it in listOf("upi", "cash", "access_request")) it else "access_request"
        }
        val recId = UUID.randomUUID().toString()
        val path = "subscription_records/$recId"
        return try {
            db.collection("subscription_records").document(recId).set(
                mapOf(
                    "studentEmail" to cleanEmail,
                    "studentName" to studentName.trim().ifBlank { "Student" }.take(80),
                    "studentClass" to studentClass.coerceIn(1, 12).toLong(),
                    "studentUid" to studentUid.take(128),
                    "amountInr" to 50L,
                    "planLabel" to "Rs. 50 / 3 Months",
                    "paymentMethod" to cleanMethod,
                    "referenceCode" to referenceCode.trim().take(120),
                    "status" to "pending",
                    "startMs" to 0L,
                    "endMs" to 0L,
                    "verificationNotes" to "",
                    "verifiedByAdminEmail" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }
}
