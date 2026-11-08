package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.CsvQuestionDraftRow
import com.example.data.GeneratedQuestionDraft
import com.example.data.model.AllowedQuestionManager
import com.example.data.model.QuestionAuditLog
import com.example.data.model.QuestionDashboardStats
import com.example.data.model.QuestionItem
import com.example.data.model.SubjectCatalogDoc
import com.example.data.model.isPreAuthorizedMainAdminEmail
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

data class QuestionManagerOperatorSession(
    val uid: String,
    val email: String,
    val displayName: String,
    val isMainAdmin: Boolean,
    val canPublishDirectly: Boolean,
    val canDeleteQuestions: Boolean
)

/**
 * Dedicated Firestore data layer for Application 3:
 * QUESTION MANAGEMENT APPLICATION (/question-manager).
 *
 * Shares the single provisioned Firestore database with /app and /admin while enforcing
 * strict role boundaries: Question Managers never receive Main Admin permissions,
 * students never access /question-manager, and every editorial change records an
 * immutable audit entry with timestamp and verified editor identity.
 */
class QuestionManagerRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireSignedInUid(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Operator must sign in with an authorized Google account.")
    }

    private fun currentVerifiedEmail(): String {
        return auth.currentUser?.email?.trim()?.lowercase()
            ?: throw IllegalStateException("Verified Google account email is required.")
    }

    // --- 1. Zero-Trust Role Verification & Session Sync ---

    suspend fun verifyAndSyncOperatorSession(
        uid: String,
        rawEmail: String?,
        displayName: String?,
        isEmailVerified: Boolean
    ): Result<QuestionManagerOperatorSession> {
        val cleanEmail = rawEmail?.trim()?.lowercase().orEmpty()
        if (uid.isBlank() || cleanEmail.isBlank() || !isEmailVerified) {
            return Result.failure(
                SecurityException("Access Denied: A verified Google account is required to enter Question Management.")
            )
        }

        // Path A: Pre-authorized Main Admin (Owner Pritam Choudhary or Second Main Admin)
        if (isPreAuthorizedMainAdminEmail(cleanEmail)) {
            val adminPath = "admins/$uid"
            return try {
                val adminRef = db.collection("admins").document(uid)
                val snap = adminRef.get().await()
                val safeName = displayName?.trim()?.ifBlank { "Main Admin" }?.take(80) ?: "Main Admin"
                if (snap.exists()) {
                    adminRef.update(
                        mapOf(
                            "email" to cleanEmail,
                            "name" to safeName,
                            "role" to "admin",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    adminRef.set(
                        mapOf(
                            "uid" to uid,
                            "email" to cleanEmail,
                            "name" to safeName,
                            "role" to "admin",
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
                Result.success(
                    QuestionManagerOperatorSession(
                        uid = uid,
                        email = cleanEmail,
                        displayName = safeName,
                        isMainAdmin = true,
                        canPublishDirectly = true,
                        canDeleteQuestions = true
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, adminPath)
                Result.failure(
                    SecurityException("Main Admin authorization check failed in Firestore Security Rules.")
                )
            }
        }

        // Path B: Authorized Question Manager listed in /allowed_question_managers/{emailKey}
        val emailKey = sanitizeEmailKey(cleanEmail)
        val allowlistPath = "allowed_question_managers/$emailKey"
        return try {
            val allowRef = db.collection("allowed_question_managers").document(emailKey)
            val allowSnap = allowRef.get().await()
            if (!allowSnap.exists()) {
                return Result.failure(
                    SecurityException("Access Denied: Your Google account ($cleanEmail) is not authorized for Question Management.")
                )
            }
            val allowRecord = allowSnap.toObject(
                AllowedQuestionManager::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            if (allowRecord == null || !allowRecord.isActive) {
                return Result.failure(
                    SecurityException("Access Denied: Your Question Manager account ($cleanEmail) is currently suspended or inactive.")
                )
            }

            // Link UID on allowlist record and sync /question_managers/{uid} role document
            allowRef.update(
                mapOf(
                    "linkedUid" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val safeName = allowRecord.managerName.ifBlank {
                displayName?.trim()?.ifBlank { "Question Manager" } ?: "Question Manager"
            }.take(80)

            val mgrRef = db.collection("question_managers").document(uid)
            val mgrSnap = mgrRef.get().await()
            if (mgrSnap.exists()) {
                mgrRef.update(
                    mapOf(
                        "email" to cleanEmail,
                        "name" to safeName,
                        "role" to "question_manager",
                        "emailKey" to emailKey,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                mgrRef.set(
                    mapOf(
                        "uid" to uid,
                        "email" to cleanEmail,
                        "name" to safeName,
                        "role" to "question_manager",
                        "emailKey" to emailKey,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(
                QuestionManagerOperatorSession(
                    uid = uid,
                    email = cleanEmail,
                    displayName = safeName,
                    isMainAdmin = false,
                    canPublishDirectly = allowRecord.canPublishDirectly,
                    canDeleteQuestions = allowRecord.canDeleteQuestions
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, allowlistPath)
            Result.failure(
                SecurityException("Access Denied: Only authorized Question Managers and Main Admins may access /question-manager.")
            )
        }
    }

    // --- 2. Real-Time Question Bank & Real Database Statistics ---

    fun observeQuestionsForSession(session: QuestionManagerOperatorSession): Flow<List<QuestionItem>> {
        val path = "questions"
        if (session.isMainAdmin) {
            return db.collection(path)
                .snapshots()
                .map { snap ->
                    snap.toObjects(QuestionItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { e ->
                    if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                    throw e
                }
        }

        // For non-admin Question Managers: combine published bank + own unpublished/draft/review items
        // (Aligns strictly with zero-DB-read `allow list` rules on /questions)
        val publishedFlow = db.collection(path)
            .whereEqualTo("isPublished", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(QuestionItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }

        val ownAuthoredFlow = db.collection(path)
            .whereEqualTo("authorId", session.uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(QuestionItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }

        return combine(publishedFlow, ownAuthoredFlow) { pubList, ownList ->
            val merged = LinkedHashMap<String, QuestionItem>()
            for (q in ownList) merged[q.id] = q
            for (q in pubList) merged[q.id] = q
            merged.values.sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
        }
    }

    fun computeDashboardStats(questions: List<QuestionItem>): QuestionDashboardStats {
        val published = questions.count { it.isPublished }
        val drafts = questions.count { !it.isPublished && it.effectiveReviewStatus == "draft" }
        val pending = questions.count { !it.isPublished && it.effectiveReviewStatus == "pending_review" }
        val rejected = questions.count { !it.isPublished && it.effectiveReviewStatus == "rejected" }

        val byClass = questions
            .groupingBy { it.studentClass.toInt().coerceIn(0, 12) }
            .eachCount()
            .toSortedMap()

        val bySubject = questions
            .groupingBy { it.subject.ifBlank { "General Knowledge" } }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .associate { it.key to it.value }

        val recent = questions
            .sortedByDescending { it.createdAt ?: it.updatedAt ?: Timestamp(0, 0) }
            .take(8)

        return QuestionDashboardStats(
            totalQuestions = questions.size,
            publishedQuestions = published,
            draftQuestions = drafts,
            pendingReviewQuestions = pending,
            rejectedQuestions = rejected,
            byClassCounts = byClass,
            bySubjectCounts = bySubject,
            recentQuestions = recent
        )
    }

    // --- 3. Question Creation, Editing, Drafts & Review Workflow ---

    fun validateQuestionInput(
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
        wantPublishOrReview: Boolean
    ): String? {
        if (studentClass !in 0..12) return "Select a valid Class (GK or Class 1 to 12)."
        if (subject.trim().isEmpty() || subject.trim().length > 60) return "Select or enter a valid Subject (max 60 characters)."
        if (topic.trim().length > 120) return "Topic or Chapter cannot exceed 120 characters."
        if (language !in setOf("en", "hi")) return "Language must be English or Hindi."
        if (questionText.trim().length < 3) return "Question text must be at least 3 characters."
        if (questionText.trim().length > 1000) return "Question text cannot exceed 1000 characters."
        if (options.size != 4 || options.any { it.trim().isEmpty() }) {
            return "All four answer options (A, B, C, and D) must be filled."
        }
        if (options.any { it.trim().length > 300 }) return "Each option must be 300 characters or fewer."
        if (correctIndex !in 0..3) return "Select the correct answer option (A, B, C, or D)."
        if (wantPublishOrReview && explanation.trim().isEmpty()) {
            return "Explanation of the correct answer is required before submitting or publishing."
        }
        if (explanation.trim().length > 1000) return "Explanation cannot exceed 1000 characters."
        if (difficulty.lowercase() !in setOf("easy", "medium", "hard")) return "Select Difficulty: Easy, Medium, or Hard."
        if (points !in 1..1000) return "Points must be between 1 and 1000."
        if (timerSeconds !in 5..300) return "Time limit must be between 5 and 300 seconds."
        val cleanImage = imageUrl.trim()
        if (cleanImage.isNotEmpty() && (!cleanImage.startsWith("https://") || cleanImage.length > 500)) {
            return "Optional Image URL must start with https:// (max 500 characters)."
        }
        return null
    }

    suspend fun saveOrUpdateQuestion(
        session: QuestionManagerOperatorSession,
        existingQuestion: QuestionItem?,
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
        source: String = "manual"
    ): Result<String> {
        val uid = requireSignedInUid()
        val email = currentVerifiedEmail()
        val wantPublishOrReview = desiredAction == "publish" || desiredAction == "pending_review"

        val validationError = validateQuestionInput(
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
            wantPublishOrReview = wantPublishOrReview
        )
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }

        val canPublish = session.isMainAdmin || session.canPublishDirectly
        if (desiredAction == "publish" && !canPublish) {
            return Result.failure(
                SecurityException("Direct publishing is restricted for your account. Save as Draft or Submit for Main Admin Review.")
            )
        }

        val finalPublish = desiredAction == "publish" && canPublish
        val finalReviewStatus = when {
            finalPublish -> "approved"
            desiredAction == "pending_review" -> "pending_review"
            else -> "draft"
        }

        val docId = existingQuestion?.id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val path = "questions/$docId"
        val cleanOptions = options.take(4).map { it.trim().take(300) }
        val cleanSubject = subject.trim().take(60)
        val cleanTopic = topic.trim().take(120)
        val cleanLang = if (language == "hi") "hi" else "en"
        val cleanDiff = difficulty.lowercase().let { if (it in setOf("easy", "medium", "hard")) it else "medium" }
        val cleanSource = if (source in setOf("manual", "gemini_ai", "csv_import")) source else "manual"

        return try {
            val docRef = db.collection("questions").document(docId)
            if (existingQuestion != null && existingQuestion.id.isNotBlank()) {
                val updateMap = mutableMapOf<String, Any>(
                    "studentClass" to studentClass.coerceIn(0, 12).toLong(),
                    "subject" to cleanSubject,
                    "topic" to cleanTopic,
                    "language" to cleanLang,
                    "questionText" to questionText.trim().take(1000),
                    "options" to cleanOptions,
                    "correctIndex" to correctIndex.coerceIn(0, 3).toLong(),
                    "explanation" to explanation.trim().take(1000),
                    "difficulty" to cleanDiff,
                    "points" to points.coerceIn(1, 1000).toLong(),
                    "timerSeconds" to timerSeconds.coerceIn(5, 300).toLong(),
                    "imageUrl" to imageUrl.trim().take(500),
                    "isPublished" to finalPublish,
                    "reviewStatus" to finalReviewStatus,
                    "lastEditedByUid" to uid,
                    "lastEditedByEmail" to email,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updateMap).await()
            } else {
                val createMap = mapOf(
                    "authorId" to uid,
                    "authorEmail" to email,
                    "studentClass" to studentClass.coerceIn(0, 12).toLong(),
                    "subject" to cleanSubject,
                    "topic" to cleanTopic,
                    "language" to cleanLang,
                    "questionText" to questionText.trim().take(1000),
                    "options" to cleanOptions,
                    "correctIndex" to correctIndex.coerceIn(0, 3).toLong(),
                    "explanation" to explanation.trim().take(1000),
                    "difficulty" to cleanDiff,
                    "points" to points.coerceIn(1, 1000).toLong(),
                    "timerSeconds" to timerSeconds.coerceIn(5, 300).toLong(),
                    "imageUrl" to imageUrl.trim().take(500),
                    "isPublished" to finalPublish,
                    "reviewStatus" to finalReviewStatus,
                    "reviewNotes" to "",
                    "lastEditedByUid" to uid,
                    "lastEditedByEmail" to email,
                    "source" to cleanSource,
                    "attemptsCount" to 0L,
                    "correctCount" to 0L,
                    "totalResponseTimeSec" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createMap).await()
            }

            val actionLabel = when {
                existingQuestion == null && finalPublish -> "create_and_publish"
                existingQuestion == null && finalReviewStatus == "pending_review" -> "create_submit_review"
                existingQuestion == null -> "create_draft"
                finalPublish -> "edit_and_publish"
                finalReviewStatus == "pending_review" -> "submit_for_review"
                else -> "edit_draft"
            }

            recordQuestionAuditLog(
                session = session,
                actionType = actionLabel,
                questionId = docId,
                questionSummary = "Class $studentClass • $cleanSubject" + if (cleanTopic.isNotBlank()) " ($cleanTopic)" else "",
                details = questionText.trim().take(160)
            )
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun submitDraftForReview(
        session: QuestionManagerOperatorSession,
        question: QuestionItem
    ): Result<Unit> {
        val uid = requireSignedInUid()
        val email = currentVerifiedEmail()
        val checkErr = validateQuestionInput(
            studentClass = question.studentClass.toInt(),
            subject = question.subject,
            topic = question.topic,
            language = question.language,
            questionText = question.questionText,
            options = question.options,
            correctIndex = question.correctIndex.toInt(),
            explanation = question.explanation,
            difficulty = question.difficulty,
            points = question.points.toInt(),
            timerSeconds = question.timerSeconds.toInt(),
            imageUrl = question.imageUrl,
            wantPublishOrReview = true
        )
        if (checkErr != null) return Result.failure(IllegalArgumentException(checkErr))

        val path = "questions/${question.id}"
        return try {
            db.collection("questions").document(question.id).update(
                mapOf(
                    "isPublished" to false,
                    "reviewStatus" to "pending_review",
                    "lastEditedByUid" to uid,
                    "lastEditedByEmail" to email,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            recordQuestionAuditLog(
                session = session,
                actionType = "submit_review",
                questionId = question.id,
                questionSummary = "Class ${question.studentClass} • ${question.subject}",
                details = "Submitted question for Main Admin review."
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- 4. Main Admin Review Actions (Approve, Reject, Publish, Unpublish, Delete) ---

    suspend fun reviewAndSetQuestionState(
        session: QuestionManagerOperatorSession,
        question: QuestionItem,
        newReviewStatus: String, // "approved" | "rejected" | "draft"
        publishNow: Boolean,
        reviewNotes: String = ""
    ): Result<Unit> {
        val uid = requireSignedInUid()
        val email = currentVerifiedEmail()
        if (!session.isMainAdmin && !(session.canPublishDirectly && question.authorId == uid)) {
            return Result.failure(SecurityException("Only Main Admin can approve, reject, or change global publication status."))
        }
        if (publishNow) {
            val err = validateQuestionInput(
                studentClass = question.studentClass.toInt(),
                subject = question.subject,
                topic = question.topic,
                language = question.language,
                questionText = question.questionText,
                options = question.options,
                correctIndex = question.correctIndex.toInt(),
                explanation = question.explanation,
                difficulty = question.difficulty,
                points = question.points.toInt(),
                timerSeconds = question.timerSeconds.toInt(),
                imageUrl = question.imageUrl,
                wantPublishOrReview = true
            )
            if (err != null) return Result.failure(IllegalArgumentException(err))
        }

        val safeStatus = if (publishNow) "approved" else newReviewStatus
        val safePublish = publishNow && safeStatus == "approved"
        val path = "questions/${question.id}"
        return try {
            db.collection("questions").document(question.id).update(
                mapOf(
                    "isPublished" to safePublish,
                    "reviewStatus" to safeStatus,
                    "reviewNotes" to reviewNotes.trim().take(500),
                    "lastEditedByUid" to uid,
                    "lastEditedByEmail" to email,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val action = when {
                safePublish -> "approve_and_publish"
                safeStatus == "rejected" -> "reject_question"
                else -> "unpublish_to_draft"
            }
            recordQuestionAuditLog(
                session = session,
                actionType = action,
                questionId = question.id,
                questionSummary = "Class ${question.studentClass} • ${question.subject}",
                details = reviewNotes.trim().ifBlank { "Updated status to $safeStatus (published=$safePublish)" }.take(450)
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePermittedQuestion(
        session: QuestionManagerOperatorSession,
        question: QuestionItem
    ): Result<Unit> {
        val uid = requireSignedInUid()
        if (!session.isMainAdmin) {
            if (!session.canDeleteQuestions || question.authorId != uid || question.isPublished) {
                return Result.failure(
                    SecurityException("Question Managers can only delete their own unpublished draft questions when permitted.")
                )
            }
        }
        val path = "questions/${question.id}"
        return try {
            db.collection("questions").document(question.id).delete().await()
            recordQuestionAuditLog(
                session = session,
                actionType = "delete_question",
                questionId = question.id,
                questionSummary = "Class ${question.studentClass} • ${question.subject}",
                details = "Deleted question: ${question.questionText.take(120)}"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- 5. Bulk Non-Destructive CSV Import & AI Draft Saving ---

    suspend fun importValidatedCsvRows(
        session: QuestionManagerOperatorSession,
        rowsToSave: List<CsvQuestionDraftRow>,
        publishImportedIfPermitted: Boolean
    ): Result<Int> {
        if (rowsToSave.isEmpty()) {
            return Result.failure(IllegalArgumentException("No valid rows selected for import."))
        }
        val canPublish = session.isMainAdmin || session.canPublishDirectly
        var importedCount = 0
        for (row in rowsToSave) {
            if (!row.isValid) continue
            val desired = if (publishImportedIfPermitted && row.requestedPublish && canPublish) {
                "publish"
            } else {
                "draft"
            }
            // Always pass existingQuestion = null so existing Firestore questions are NEVER overwritten
            val res = saveOrUpdateQuestion(
                session = session,
                existingQuestion = null,
                studentClass = row.studentClass,
                subject = row.subject,
                topic = row.topic,
                language = row.language,
                questionText = row.questionText,
                options = row.options,
                correctIndex = row.correctIndex,
                explanation = row.explanation,
                difficulty = row.difficulty,
                points = row.points,
                timerSeconds = row.timerSeconds,
                imageUrl = row.imageUrl,
                desiredAction = desired,
                source = "csv_import"
            )
            if (res.isSuccess) importedCount++
        }
        if (importedCount > 0) {
            recordQuestionAuditLog(
                session = session,
                actionType = "csv_bulk_import",
                questionId = "batch_${UUID.randomUUID().toString().take(8)}",
                questionSummary = "CSV Bulk Import ($importedCount questions)",
                details = "Imported $importedCount new questions without overwriting existing database items."
            )
            return Result.success(importedCount)
        }
        return Result.failure(IllegalStateException("CSV import failed to save rows. Check permissions and field constraints."))
    }

    suspend fun saveAiReviewedDrafts(
        session: QuestionManagerOperatorSession,
        studentClass: Int,
        subject: String,
        topic: String,
        language: String,
        drafts: List<GeneratedQuestionDraft>
    ): Result<Int> {
        var saved = 0
        for (item in drafts) {
            val res = saveOrUpdateQuestion(
                session = session,
                existingQuestion = null,
                studentClass = studentClass,
                subject = subject,
                topic = topic,
                language = language,
                questionText = item.questionText,
                options = item.options,
                correctIndex = item.correctIndex,
                explanation = item.explanation,
                difficulty = item.difficulty,
                points = item.points,
                timerSeconds = item.timerSeconds,
                imageUrl = "",
                desiredAction = "draft", // Strictly saved as draft; never auto-published
                source = "gemini_ai"
            )
            if (res.isSuccess) saved++
        }
        return if (saved > 0) Result.success(saved)
        else Result.failure(IllegalStateException("Failed to save reviewed AI drafts."))
    }

    // --- 6. Main Admin Controls: Authorize Question Managers & Subject Catalog ---

    fun observeSubjectCatalog(): Flow<SubjectCatalogDoc> {
        val path = "subject_catalog/global"
        return db.collection("subject_catalog").document("global")
            .snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(SubjectCatalogDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: SubjectCatalogDoc()
                } else {
                    SubjectCatalogDoc()
                }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.GET, path)
                emit(SubjectCatalogDoc())
            }
    }

    suspend fun saveSubjectCatalog(
        session: QuestionManagerOperatorSession,
        classes1To5: List<String>,
        classes6To8: List<String>,
        classes9To10: List<String>,
        classes11To12: List<String>
    ): Result<Unit> {
        if (!session.isMainAdmin) {
            return Result.failure(SecurityException("Only Main Admin can modify the platform Subject Catalog."))
        }
        val clean1To5 = classes1To5.map { it.trim().take(60) }.filter { it.isNotEmpty() }.distinct().take(25)
        val clean6To8 = classes6To8.map { it.trim().take(60) }.filter { it.isNotEmpty() }.distinct().take(25)
        val clean9To10 = classes9To10.map { it.trim().take(60) }.filter { it.isNotEmpty() }.distinct().take(25)
        val clean11To12 = classes11To12.map { it.trim().take(60) }.filter { it.isNotEmpty() }.distinct().take(30)
        if (clean1To5.isEmpty() || clean6To8.isEmpty() || clean9To10.isEmpty() || clean11To12.isEmpty()) {
            return Result.failure(IllegalArgumentException("Each class group must retain at least one subject."))
        }
        val path = "subject_catalog/global"
        return try {
            val docRef = db.collection("subject_catalog").document("global")
            val exists = docRef.get().await().exists()
            val payload = mutableMapOf<String, Any>(
                "classes1To5" to clean1To5,
                "classes6To8" to clean6To8,
                "classes9To10" to clean9To10,
                "classes11To12" to clean11To12,
                "updatedByEmail" to currentVerifiedEmail(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (exists) {
                docRef.update(payload).await()
            } else {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            }
            recordQuestionAuditLog(
                session = session,
                actionType = "update_subject_catalog",
                questionId = "global",
                questionSummary = "Master Subject Catalog",
                details = "Updated Class 1-12 subject organization lists."
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeAllowedQuestionManagers(): Flow<List<AllowedQuestionManager>> {
        val path = "allowed_question_managers"
        return db.collection(path)
            .snapshots()
            .map { snap ->
                snap.toObjects(AllowedQuestionManager::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    suspend fun saveAllowedQuestionManager(
        session: QuestionManagerOperatorSession,
        email: String,
        managerName: String,
        isActive: Boolean,
        canPublishDirectly: Boolean,
        canDeleteQuestions: Boolean,
        notes: String
    ): Result<Unit> {
        if (!session.isMainAdmin) {
            return Result.failure(SecurityException("Only Main Admin can authorize or modify Question Managers."))
        }
        val cleanEmail = email.trim().lowercase().take(120)
        if (!cleanEmail.contains("@") || cleanEmail.length < 5) {
            return Result.failure(IllegalArgumentException("Enter a valid Google Gmail address for the Question Manager."))
        }
        val key = sanitizeEmailKey(cleanEmail)
        val path = "allowed_question_managers/$key"
        return try {
            val docRef = db.collection("allowed_question_managers").document(key)
            val snap = docRef.get().await()
            val payload = mutableMapOf<String, Any>(
                "email" to cleanEmail,
                "managerName" to managerName.trim().ifBlank { "Question Manager" }.take(80),
                "isActive" to isActive,
                "canPublishDirectly" to canPublishDirectly,
                "canDeleteQuestions" to canDeleteQuestions,
                "addedByAdminEmail" to currentVerifiedEmail(),
                "notes" to notes.trim().take(300),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (snap.exists()) {
                docRef.update(payload).await()
            } else {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            }
            recordQuestionAuditLog(
                session = session,
                actionType = if (isActive) "authorize_manager" else "suspend_manager",
                questionId = key,
                questionSummary = cleanEmail,
                details = "Manager active=$isActive, directPublish=$canPublishDirectly, canDelete=$canDeleteQuestions"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun removeAllowedQuestionManager(
        session: QuestionManagerOperatorSession,
        manager: AllowedQuestionManager
    ): Result<Unit> {
        if (!session.isMainAdmin) {
            return Result.failure(SecurityException("Only Main Admin can remove Question Managers."))
        }
        val key = manager.id.ifBlank { sanitizeEmailKey(manager.email) }
        val path = "allowed_question_managers/$key"
        return try {
            db.collection("allowed_question_managers").document(key).delete().await()
            if (manager.linkedUid.isNotBlank()) {
                try {
                    db.collection("question_managers").document(manager.linkedUid).delete().await()
                } catch (_: Exception) {
                    // Non-fatal; synchronous rule lookup on allowed_question_managers already blocks access
                }
            }
            recordQuestionAuditLog(
                session = session,
                actionType = "revoke_manager",
                questionId = key,
                questionSummary = manager.email,
                details = "Revoked Question Manager access for ${manager.email}"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- 7. Immutable Question Audit Logs ---

    fun observeQuestionAuditLogs(session: QuestionManagerOperatorSession): Flow<List<QuestionAuditLog>> {
        val path = "question_audit_logs"
        val query = if (session.isMainAdmin) {
            db.collection(path)
        } else {
            db.collection(path).whereEqualTo("editorUid", session.uid)
        }
        return query
            .snapshots()
            .map { snap ->
                snap.toObjects(QuestionAuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    private suspend fun recordQuestionAuditLog(
        session: QuestionManagerOperatorSession,
        actionType: String,
        questionId: String,
        questionSummary: String,
        details: String
    ) {
        val logId = UUID.randomUUID().toString()
        val path = "question_audit_logs/$logId"
        try {
            db.collection("question_audit_logs").document(logId).set(
                mapOf(
                    "editorUid" to session.uid,
                    "editorEmail" to session.email.take(120),
                    "editorRole" to if (session.isMainAdmin) "main_admin" else "question_manager",
                    "actionType" to actionType.take(60),
                    "questionId" to questionId.ifBlank { "unknown" }.take(128),
                    "questionSummary" to questionSummary.ifBlank { "Question" }.take(200),
                    "details" to details.ifBlank { actionType }.take(500),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
        }
    }
}
