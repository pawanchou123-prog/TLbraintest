package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.CurriculumCatalog
import com.example.data.model.AdminAuditLog
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

/**
 * Dedicated Firestore data layer for Application 2: MAIN ADMIN APPLICATION (/admin).
 * Reuses the existing provisioned Firestore database and collections while enforcing
 * server-side Firestore Security Rules and recording immutable AdminAuditLog entries.
 */
class AdminRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireAdminIdentity(): Pair<String, String> {
        val user = auth.currentUser
            ?: throw IllegalStateException("Administrator must be signed in with Google before accessing /admin.")
        val email = user.email?.trim()?.lowercase().orEmpty()
        if (email.isBlank()) {
            throw IllegalStateException("Authenticated Google account does not expose a verified email.")
        }
        return user.uid to email
    }

    /**
     * Authoritative backend verification handshake:
     * 1. Rejects immediately if email is not verified or if neither pre-authorized email nor /admins/{uid} matches.
     * 2. Executes a real Firestore write/read against /admins/{uid} and /allowed_students (limit 1)
     *    so Firestore Security Rules strictly enforce access on the server before /admin unlocks.
     */
    suspend fun verifyAndSyncMainAdminSession(
        uid: String,
        rawEmail: String,
        displayName: String,
        isEmailVerified: Boolean
    ): Result<RoleMember> {
        val email = rawEmail.trim().lowercase()
        val adminPath = "admins/$uid"
        return try {
            if (!isEmailVerified) {
                return Result.failure(
                    SecurityException("Your Google account email is not verified. Unverified accounts are blocked.")
                )
            }

            if (isPreAuthorizedMainAdminEmail(email)) {
                val safeName = displayName.trim().ifBlank { "Main Administrator" }.take(80)
                val docRef = db.collection("admins").document(uid)
                val existingSnap = docRef.get().await()
                if (existingSnap.exists()) {
                    docRef.update(
                        mapOf(
                            "email" to email.take(120),
                            "name" to safeName,
                            "role" to "admin",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    docRef.set(
                        mapOf(
                            "uid" to uid,
                            "email" to email.take(120),
                            "name" to safeName,
                            "role" to "admin",
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
                // Verify list permission enforced by Firestore Security Rules
                db.collection("allowed_students").limit(1).get().await()
                val roleMember = docRef.get().await()
                    .toObject(RoleMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: RoleMember(id = uid, uid = uid, email = email, name = safeName, role = "admin")
                Result.success(roleMember)
            } else {
                Result.failure(
                    SecurityException("Google account '$rawEmail' is not authorized for TL Brain Test Main Admin (/admin).")
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, adminPath)
            Result.failure(e)
        }
    }

    suspend fun recordAuditLog(
        actionType: String,
        targetId: String,
        targetLabel: String,
        reasonOrDetails: String,
        pointsDelta: Long = 0L
    ) {
        val (adminUid, adminEmail) = requireAdminIdentity()
        val logId = UUID.randomUUID().toString()
        val path = "admin_audit_logs/$logId"
        try {
            db.collection("admin_audit_logs").document(logId).set(
                mapOf(
                    "adminUid" to adminUid,
                    "adminEmail" to adminEmail.take(120),
                    "actionType" to actionType.trim().ifBlank { "admin_action" }.take(60),
                    "targetId" to targetId.trim().ifBlank { "unknown" }.take(128),
                    "targetLabel" to targetLabel.trim().ifBlank { "Target" }.take(120),
                    "reasonOrDetails" to reasonOrDetails.trim().ifBlank { "Executed by Main Admin" }.take(500),
                    "pointsDelta" to pointsDelta.coerceIn(-100000L, 100000L),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
        }
    }

    // --- Real-Time Admin Streams (Subscribed ONLY after Admin Authorization succeeds) ---

    fun observeAllAllowedStudents(): Flow<List<AllowedStudent>> =
        db.collection("allowed_students").snapshots()
            .map { snap ->
                snap.toObjects(AllowedStudent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "allowed_students")
                throw e
            }

    fun observeAllUserProfiles(): Flow<List<UserProfile>> =
        db.collection("users").snapshots()
            .map { snap ->
                snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "users")
                throw e
            }

    fun observeAllSubscriptionRecords(): Flow<List<SubscriptionRecord>> =
        db.collection("subscription_records").snapshots()
            .map { snap ->
                snap.toObjects(SubscriptionRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "subscription_records")
                throw e
            }

    fun observeAllQuizResults(): Flow<List<QuizResultRecord>> =
        db.collection("quiz_results").snapshots()
            .map { snap ->
                snap.toObjects(QuizResultRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "quiz_results")
                throw e
            }

    fun observeAllQuestions(): Flow<List<QuestionItem>> =
        db.collection("questions").snapshots()
            .map { snap ->
                snap.toObjects(QuestionItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "questions")
                throw e
            }

    fun observeAllLeaderboardEntries(): Flow<List<LeaderboardEntry>> =
        db.collection("leaderboard_entries").snapshots()
            .map { snap ->
                snap.toObjects(LeaderboardEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "leaderboard_entries")
                throw e
            }

    fun observeAllWinners(): Flow<List<WinnerRecord>> =
        db.collection("winners_history").snapshots()
            .map { snap ->
                snap.toObjects(WinnerRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareByDescending<WinnerRecord> { it.createdAt ?: Timestamp(0, 0) }.thenBy { it.rank })
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "winners_history")
                throw e
            }

    fun observeAllBanners(): Flow<List<BannerItem>> =
        db.collection("banners").snapshots()
            .map { snap ->
                snap.toObjects(BannerItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "banners")
                throw e
            }

    fun observeAllPrizes(): Flow<List<PrizeItem>> =
        db.collection("prizes").snapshots()
            .map { snap ->
                snap.toObjects(PrizeItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.rankLabel }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "prizes")
                throw e
            }

    fun observeAllAnnouncements(): Flow<List<AnnouncementItem>> =
        db.collection("announcements").snapshots()
            .map { snap ->
                snap.toObjects(AnnouncementItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "announcements")
                throw e
            }

    fun observeAllSupportThreads(): Flow<List<SupportThread>> =
        db.collection("support_threads").snapshots()
            .map { snap ->
                snap.toObjects(SupportThread::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "support_threads")
                throw e
            }

    fun observeThreadMessages(studentUid: String): Flow<List<SupportMessage>> =
        db.collection("support_threads").document(studentUid).collection("messages")
            .snapshots()
            .map { snap ->
                snap.toObjects(SupportMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "support_threads/$studentUid/messages")
                throw e
            }

    fun observeAppConfig(): Flow<AppConfig> =
        db.collection("app_config").document("global")
            .snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(AppConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: AppConfig()
                } else {
                    AppConfig()
                }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.GET, "app_config/global")
                throw e
            }

    fun observeAuditLogs(): Flow<List<AdminAuditLog>> =
        db.collection("admin_audit_logs").snapshots()
            .map { snap ->
                snap.toObjects(AdminAuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "admin_audit_logs")
                throw e
            }

    // --- Student Management Operations ---

    suspend fun saveStudentAccount(
        email: String,
        studentName: String,
        studentClass: Int,
        preferredLanguage: String,
        subscriptionActive: Boolean,
        paymentMethod: String,
        startMs: Long,
        endMs: Long,
        adminNotes: String
    ): Result<Unit> {
        requireAdminIdentity()
        val cleanEmail = email.trim().lowercase().take(120)
        if (!cleanEmail.contains("@") || cleanEmail.length < 5) {
            return Result.failure(IllegalArgumentException("Enter a valid student Gmail address."))
        }
        val key = sanitizeEmailKey(cleanEmail)
        val path = "allowed_students/$key"
        return try {
            val docRef = db.collection("allowed_students").document(key)
            val snap = docRef.get().await()
            val safeName = studentName.trim().ifBlank { "Student" }.take(80)
            val safeMethod = paymentMethod.lowercase().let { if (it in listOf("upi", "cash", "manual")) it else "upi" }
            val payload = mutableMapOf<String, Any>(
                "email" to cleanEmail,
                "studentName" to safeName,
                "studentClass" to studentClass.coerceIn(1, 12).toLong(),
                "preferredLanguage" to if (preferredLanguage == "hi") "hi" else "en",
                "isBanned" to false,
                "subscriptionActive" to subscriptionActive,
                "subscriptionPlan" to "Rs. 50 / 3 Months",
                "paymentMethod" to safeMethod,
                "subscriptionStartMs" to startMs.coerceAtLeast(0L),
                "subscriptionEndMs" to endMs.coerceAtLeast(0L),
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
            recordAuditLog(
                actionType = if (snap.exists()) "update_student" else "approve_access",
                targetId = cleanEmail,
                targetLabel = safeName,
                reasonOrDetails = "Class ${studentClass.coerceIn(1, 12)} | Active=$subscriptionActive | ${adminNotes.ifBlank { "Saved by Admin" }}"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateStudentLifecycleState(
        student: AllowedStudent,
        action: String, // "activate" | "deactivate" | "ban" | "unban"
        reason: String
    ): Result<Unit> {
        requireAdminIdentity()
        val key = student.id.ifBlank { sanitizeEmailKey(student.email) }
        val path = "allowed_students/$key"
        val cleanReason = reason.trim().ifBlank { "Updated account state: $action" }.take(300)
        return try {
            val nowMs = System.currentTimeMillis()
            val ninetyDaysMs = 90L * 24L * 60L * 60L * 1000L
            val updates = when (action) {
                "activate" -> mapOf(
                    "isBanned" to false,
                    "subscriptionActive" to true,
                    "subscriptionStartMs" to if (student.subscriptionStartMs > 0L) student.subscriptionStartMs else nowMs,
                    "subscriptionEndMs" to if (student.subscriptionEndMs > nowMs) student.subscriptionEndMs else (nowMs + ninetyDaysMs),
                    "adminNotes" to cleanReason,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                "deactivate" -> mapOf(
                    "subscriptionActive" to false,
                    "adminNotes" to cleanReason,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                "ban" -> mapOf(
                    "isBanned" to true,
                    "adminNotes" to cleanReason,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                "unban" -> mapOf(
                    "isBanned" to false,
                    "adminNotes" to cleanReason,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                else -> return Result.failure(IllegalArgumentException("Unknown lifecycle action: $action"))
            }
            db.collection("allowed_students").document(key).update(updates).await()
            recordAuditLog(
                actionType = "${action}_student",
                targetId = student.email,
                targetLabel = student.studentName,
                reasonOrDetails = cleanReason
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adjustStudentPointsWithReason(
        profile: UserProfile,
        pointsDelta: Int,
        reason: String
    ): Result<Unit> {
        requireAdminIdentity()
        if (pointsDelta == 0) {
            return Result.failure(IllegalArgumentException("Points adjustment must be non-zero (for example +50 or -20)."))
        }
        val cleanReason = reason.trim()
        if (cleanReason.length < 3) {
            return Result.failure(IllegalArgumentException("Please enter a clear reason for adjusting student points."))
        }
        val uid = profile.userId.ifBlank { profile.id }
        if (uid.isBlank()) {
            return Result.failure(IllegalStateException("This student has not signed in to initialize a profile yet."))
        }
        val userPath = "users/$uid"
        return try {
            val newPoints = (profile.totalPoints + pointsDelta.toLong()).coerceAtLeast(0L)
            val newXp = (profile.xp + pointsDelta.toLong()).coerceAtLeast(0L)
            val newLevel = (newXp / 200L) + 1L
            db.collection("users").document(uid).update(
                mapOf(
                    "totalPoints" to newPoints,
                    "xp" to newXp,
                    "level" to newLevel,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val lbRef = db.collection("leaderboard_entries").document(uid)
            if (lbRef.get().await().exists()) {
                lbRef.update(
                    mapOf(
                        "allTimeXp" to newXp,
                        "level" to newLevel,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            recordAuditLog(
                actionType = "adjust_points",
                targetId = profile.email.ifBlank { uid },
                targetLabel = profile.displayName,
                reasonOrDetails = cleanReason,
                pointsDelta = pointsDelta.toLong()
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userPath)
            Result.failure(e)
        }
    }

    suspend fun toggleStudentBadge(
        profile: UserProfile,
        badgeId: String,
        grant: Boolean,
        reason: String
    ): Result<Unit> {
        requireAdminIdentity()
        val uid = profile.userId.ifBlank { profile.id }
        if (uid.isBlank()) {
            return Result.failure(IllegalStateException("Student profile UID is not initialized yet."))
        }
        val path = "users/$uid"
        return try {
            val nextBadges = if (grant) {
                (profile.badges + badgeId).distinct().take(30)
            } else {
                profile.badges.filterNot { it == badgeId }
            }
            db.collection("users").document(uid).update(
                mapOf(
                    "badges" to nextBadges,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            recordAuditLog(
                actionType = if (grant) "grant_badge" else "revoke_badge",
                targetId = profile.email.ifBlank { uid },
                targetLabel = profile.displayName,
                reasonOrDetails = "${if (grant) "Granted" else "Revoked"} badge '$badgeId': ${reason.ifBlank { "Admin review" }}"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- Subscription & Manual Payment Verification Operations (Rs. 50 / 3 Months) ---

    suspend fun reviewSubscriptionRequest(
        record: SubscriptionRecord,
        approve: Boolean,
        paymentMethod: String,
        startMs: Long,
        endMs: Long,
        internalVerificationNotes: String
    ): Result<Unit> {
        val (_, adminEmail) = requireAdminIdentity()
        if (record.id.isBlank()) {
            return Result.failure(IllegalArgumentException("Subscription request ID is missing."))
        }
        val cleanNotes = internalVerificationNotes.trim()
            .ifBlank { if (approve) "Manually verified Rs. 50 payment" else "Request rejected after review" }
            .take(500)
        val cleanMethod = paymentMethod.lowercase().let { if (it in listOf("upi", "cash")) it else "upi" }
        val recPath = "subscription_records/${record.id}"
        return try {
            db.collection("subscription_records").document(record.id).update(
                mapOf(
                    "paymentMethod" to if (approve) cleanMethod else record.paymentMethod,
                    "status" to if (approve) "approved" else "rejected",
                    "startMs" to if (approve) startMs.coerceAtLeast(0L) else 0L,
                    "endMs" to if (approve) endMs.coerceAtLeast(0L) else 0L,
                    "verificationNotes" to cleanNotes,
                    "verifiedByAdminEmail" to adminEmail.take(120),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (approve) {
                val cleanEmail = record.studentEmail.trim().lowercase()
                val key = sanitizeEmailKey(cleanEmail)
                val studentRef = db.collection("allowed_students").document(key)
                val snap = studentRef.get().await()
                val studentPayload = mutableMapOf<String, Any>(
                    "email" to cleanEmail,
                    "studentName" to record.studentName.ifBlank { "Student" }.take(80),
                    "studentClass" to record.studentClass.coerceIn(1L, 12L),
                    "preferredLanguage" to "en",
                    "isBanned" to false,
                    "subscriptionActive" to true,
                    "subscriptionPlan" to "Rs. 50 / 3 Months",
                    "paymentMethod" to cleanMethod,
                    "subscriptionStartMs" to startMs.coerceAtLeast(0L),
                    "subscriptionEndMs" to endMs.coerceAtLeast(0L),
                    "adminNotes" to cleanNotes.take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (record.studentUid.isNotBlank()) {
                    studentPayload["linkedUid"] = record.studentUid.take(128)
                }
                if (snap.exists()) {
                    studentRef.update(studentPayload).await()
                } else {
                    studentPayload["createdAt"] = FieldValue.serverTimestamp()
                    studentRef.set(studentPayload).await()
                }
            }

            recordAuditLog(
                actionType = if (approve) "approve_payment" else "reject_payment",
                targetId = record.studentEmail,
                targetLabel = record.studentName,
                reasonOrDetails = "${if (approve) "Approved" else "Rejected"} (${cleanMethod.uppercase()}): $cleanNotes"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, recPath)
            Result.failure(e)
        }
    }

    suspend fun recordDirectManualSubscription(
        email: String,
        studentName: String,
        studentClass: Int,
        paymentMethod: String, // "upi" | "cash"
        startMs: Long,
        endMs: Long,
        referenceCode: String,
        internalNotes: String
    ): Result<Unit> {
        val (_, adminEmail) = requireAdminIdentity()
        val cleanEmail = email.trim().lowercase().take(120)
        if (!cleanEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Enter a valid student Gmail address."))
        }
        val cleanMethod = if (paymentMethod.lowercase() == "cash") "cash" else "upi"
        val cleanName = studentName.trim().ifBlank { "Student" }.take(80)
        val cleanNotes = internalNotes.trim().ifBlank { "Verified manual $cleanMethod payment (Rs. 50 / 3 Months)" }.take(500)
        val recId = UUID.randomUUID().toString()
        val recPath = "subscription_records/$recId"
        return try {
            db.collection("subscription_records").document(recId).set(
                mapOf(
                    "studentEmail" to cleanEmail,
                    "studentName" to cleanName,
                    "studentClass" to studentClass.coerceIn(1, 12).toLong(),
                    "studentUid" to "",
                    "amountInr" to 50L,
                    "planLabel" to "Rs. 50 / 3 Months",
                    "paymentMethod" to cleanMethod,
                    "referenceCode" to referenceCode.trim().take(120),
                    "status" to "approved",
                    "startMs" to startMs.coerceAtLeast(0L),
                    "endMs" to endMs.coerceAtLeast(0L),
                    "verificationNotes" to cleanNotes,
                    "verifiedByAdminEmail" to adminEmail.take(120),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            saveStudentAccount(
                email = cleanEmail,
                studentName = cleanName,
                studentClass = studentClass,
                preferredLanguage = "en",
                subscriptionActive = true,
                paymentMethod = cleanMethod,
                startMs = startMs,
                endMs = endMs,
                adminNotes = cleanNotes
            ).getOrThrow()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, recPath)
            Result.failure(e)
        }
    }

    suspend fun extendOrExpireSubscription(
        student: AllowedStudent,
        newStartMs: Long,
        newEndMs: Long,
        markActive: Boolean,
        paymentMethod: String,
        internalNotes: String
    ): Result<Unit> {
        val (_, adminEmail) = requireAdminIdentity()
        val key = student.id.ifBlank { sanitizeEmailKey(student.email) }
        val path = "allowed_students/$key"
        val cleanMethod = paymentMethod.lowercase().let { if (it in listOf("upi", "cash")) it else "upi" }
        val cleanNotes = internalNotes.trim().ifBlank {
            if (markActive) "Subscription dates updated by Admin" else "Subscription expired; historical account data preserved"
        }.take(500)
        return try {
            // Restricts paid access when expired while preserving /users, /quiz_results, and /leaderboard_entries
            db.collection("allowed_students").document(key).update(
                mapOf(
                    "subscriptionActive" to markActive,
                    "paymentMethod" to cleanMethod,
                    "subscriptionStartMs" to newStartMs.coerceAtLeast(0L),
                    "subscriptionEndMs" to newEndMs.coerceAtLeast(0L),
                    "adminNotes" to cleanNotes.take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val recId = UUID.randomUUID().toString()
            db.collection("subscription_records").document(recId).set(
                mapOf(
                    "studentEmail" to student.email.trim().lowercase(),
                    "studentName" to student.studentName.ifBlank { "Student" }.take(80),
                    "studentClass" to student.studentClass.coerceIn(1L, 12L),
                    "studentUid" to (student.linkedUid ?: ""),
                    "amountInr" to if (markActive) 50L else 0L,
                    "planLabel" to "Rs. 50 / 3 Months",
                    "paymentMethod" to cleanMethod,
                    "referenceCode" to "",
                    "status" to if (markActive) "approved" else "expired",
                    "startMs" to newStartMs.coerceAtLeast(0L),
                    "endMs" to newEndMs.coerceAtLeast(0L),
                    "verificationNotes" to cleanNotes,
                    "verifiedByAdminEmail" to adminEmail.take(120),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            recordAuditLog(
                actionType = if (markActive) "extend_subscription" else "expire_subscription",
                targetId = student.email,
                targetLabel = student.studentName,
                reasonOrDetails = cleanNotes
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- Content, Global Config, Leaderboards & Support Inbox Operations ---

    suspend fun saveAppConfig(config: AppConfig, auditSummary: String): Result<Unit> {
        requireAdminIdentity()
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
                "termsAndConditionsEn" to config.termsAndConditionsEn.trim().take(5000),
                "termsAndConditionsHi" to config.termsAndConditionsHi.trim().take(5000),
                "monthlyResetDay" to config.monthlyResetDay.coerceIn(1L, 28L),
                "badgeRewardConfigNotes" to config.badgeRewardConfigNotes.trim().take(1000),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (exists) {
                docRef.update(payload).await()
            } else {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            }
            recordAuditLog(
                actionType = "update_config",
                targetId = "global",
                targetLabel = "Platform Configuration",
                reasonOrDetails = auditSummary
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveBanner(
        existingId: String?,
        title: String,
        subtitle: String,
        imageUrl: String,
        actionUrl: String,
        isActive: Boolean
    ): Result<Unit> {
        requireAdminIdentity()
        val cleanImg = imageUrl.trim()
        if (!cleanImg.startsWith("https://")) {
            return Result.failure(IllegalArgumentException("Banner image URL must begin with https://"))
        }
        val id = existingId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val path = "banners/$id"
        return try {
            val docRef = db.collection("banners").document(id)
            val cleanTitle = title.trim().ifBlank { "Promotional Banner" }.take(120)
            val fields = mutableMapOf<String, Any>(
                "title" to cleanTitle,
                "subtitle" to subtitle.trim().take(250),
                "imageUrl" to cleanImg.take(500),
                "actionUrl" to actionUrl.trim().take(500),
                "isActive" to isActive,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (existingId.isNullOrBlank()) {
                fields["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(fields).await()
            } else {
                docRef.update(fields).await()
            }
            recordAuditLog("manage_content", id, cleanTitle, "Saved dashboard banner (active=$isActive)")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteBanner(bannerId: String, title: String): Result<Unit> {
        requireAdminIdentity()
        val path = "banners/$bannerId"
        return try {
            db.collection("banners").document(bannerId).delete().await()
            recordAuditLog("manage_content", bannerId, title, "Deleted dashboard banner")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun savePrize(
        existingId: String?,
        rankLabel: String,
        title: String,
        description: String,
        imageUrl: String,
        isActive: Boolean
    ): Result<Unit> {
        requireAdminIdentity()
        val cleanImg = imageUrl.trim()
        if (!cleanImg.startsWith("https://")) {
            return Result.failure(IllegalArgumentException("Gift/Prize image URL must begin with https://"))
        }
        val id = existingId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val path = "prizes/$id"
        return try {
            val docRef = db.collection("prizes").document(id)
            val cleanTitle = title.trim().ifBlank { "Educational Reward" }.take(120)
            val fields = mutableMapOf<String, Any>(
                "rankLabel" to rankLabel.trim().ifBlank { "Rank 1" }.take(60),
                "title" to cleanTitle,
                "description" to description.trim().take(300),
                "imageUrl" to cleanImg.take(500),
                "isActive" to isActive,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (existingId.isNullOrBlank()) {
                fields["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(fields).await()
            } else {
                docRef.update(fields).await()
            }
            recordAuditLog("manage_content", id, cleanTitle, "Saved reward/prize item (active=$isActive)")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePrize(prizeId: String, title: String): Result<Unit> {
        requireAdminIdentity()
        val path = "prizes/$prizeId"
        return try {
            db.collection("prizes").document(prizeId).delete().await()
            recordAuditLog("manage_content", prizeId, title, "Deleted reward/prize item")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveAnnouncement(
        existingId: String?,
        title: String,
        message: String,
        category: String,
        targetClass: Int,
        isActive: Boolean
    ): Result<Unit> {
        requireAdminIdentity()
        val id = existingId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val path = "announcements/$id"
        val cleanCat = category.lowercase().let {
            if (it in listOf("announcement", "notice", "notification", "update")) it else "announcement"
        }
        return try {
            val docRef = db.collection("announcements").document(id)
            val cleanTitle = title.trim().ifBlank { "Official Notice" }.take(150)
            val fields = mutableMapOf<String, Any>(
                "title" to cleanTitle,
                "message" to message.trim().ifBlank { "Please review this update." }.take(1000),
                "category" to cleanCat,
                "targetClass" to targetClass.coerceIn(0, 12).toLong(),
                "isActive" to isActive,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (existingId.isNullOrBlank()) {
                fields["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(fields).await()
            } else {
                docRef.update(fields).await()
            }
            recordAuditLog("manage_content", id, cleanTitle, "Saved $cleanCat (active=$isActive)")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAnnouncement(announcementId: String, title: String): Result<Unit> {
        requireAdminIdentity()
        val path = "announcements/$announcementId"
        return try {
            db.collection("announcements").document(announcementId).delete().await()
            recordAuditLog("manage_content", announcementId, title, "Deleted announcement/notice")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    /**
     * Archives top monthly scorers into /winners_history and resets ONLY monthlyXp = 0.
     * Strictly preserves all historical /quiz_results, /users profiles, allTimeXp, and past /winners_history.
     */
    suspend fun resetMonthlyLeaderboardPreservingHistory(
        currentConfig: AppConfig,
        archiveMonthLabel: String,
        defaultPrizeLabel: String
    ): Result<Int> {
        requireAdminIdentity()
        val path = "leaderboard_entries"
        return try {
            val newMonthKey = CurriculumCatalog.currentMonthKey()
            val snap = db.collection(path).get().await()
            val entries = snap.toObjects(LeaderboardEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val topScorers = entries
                .filter { it.monthlyXp > 0L }
                .sortedByDescending { it.monthlyXp }
                .take(3)

            topScorers.forEachIndexed { index, entry ->
                val winId = UUID.randomUUID().toString()
                db.collection("winners_history").document(winId).set(
                    mapOf(
                        "monthLabel" to archiveMonthLabel.trim().ifBlank { currentConfig.currentMonthKey }.take(40),
                        "winnerName" to entry.displayName.take(80),
                        "studentClass" to entry.studentClass.coerceIn(1L, 12L),
                        "rank" to (index + 1).toLong(),
                        "xpScore" to entry.monthlyXp,
                        "prizeWon" to defaultPrizeLabel.trim().ifBlank { "Monthly Merit Certificate & Educational Kit" }.take(120),
                        "isPublished" to true,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            for (doc in snap.documents) {
                doc.reference.update(
                    mapOf(
                        "monthlyXp" to 0L,
                        "monthlyKey" to newMonthKey,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            saveAppConfig(
                config = currentConfig.copy(currentMonthKey = newMonthKey),
                auditSummary = "Archived ${topScorers.size} monthly winners for '$archiveMonthLabel' and reset monthlyXp (historical records preserved)"
            )
            Result.success(topScorers.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveManualWinner(
        monthLabel: String,
        winnerName: String,
        studentClass: Int,
        rank: Int,
        xpScore: Long,
        prizeWon: String
    ): Result<Unit> {
        requireAdminIdentity()
        val id = UUID.randomUUID().toString()
        val path = "winners_history/$id"
        return try {
            val cleanName = winnerName.trim().ifBlank { "Student" }.take(80)
            db.collection("winners_history").document(id).set(
                mapOf(
                    "monthLabel" to monthLabel.trim().ifBlank { CurriculumCatalog.currentMonthKey() }.take(40),
                    "winnerName" to cleanName,
                    "studentClass" to studentClass.coerceIn(1, 12).toLong(),
                    "rank" to rank.coerceIn(1, 10).toLong(),
                    "xpScore" to xpScore.coerceAtLeast(0L),
                    "prizeWon" to prizeWon.trim().ifBlank { "Merit Reward" }.take(120),
                    "isPublished" to true,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            recordAuditLog("record_winner", id, cleanName, "Added previous winner for $monthLabel (Rank $rank)")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleWinnerVisibility(winner: WinnerRecord, publish: Boolean): Result<Unit> {
        requireAdminIdentity()
        val path = "winners_history/${winner.id}"
        return try {
            db.collection("winners_history").document(winner.id).update("isPublished", publish).await()
            recordAuditLog("update_winner", winner.id, winner.winnerName, "Set winner published=$publish")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun setSupportThreadUnreadByAdmin(studentUid: String, unreadByAdmin: Boolean): Result<Unit> {
        requireAdminIdentity()
        val path = "support_threads/$studentUid"
        return try {
            db.collection("support_threads").document(studentUid).update(
                mapOf(
                    "unreadByAdmin" to unreadByAdmin,
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

    suspend fun replyToSupportThread(
        thread: SupportThread,
        adminSenderName: String,
        replyText: String
    ): Result<Unit> {
        val (adminUid, _) = requireAdminIdentity()
        val cleanText = replyText.trim().take(1000)
        if (cleanText.isBlank()) {
            return Result.failure(IllegalArgumentException("Reply message cannot be empty."))
        }
        val studentUid = thread.studentUid.ifBlank { thread.id }
        val threadPath = "support_threads/$studentUid"
        return try {
            val threadRef = db.collection("support_threads").document(studentUid)
            threadRef.update(
                mapOf(
                    "lastMessage" to cleanText.take(500),
                    "unreadByAdmin" to false,
                    "unreadByStudent" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val msgId = UUID.randomUUID().toString()
            threadRef.collection("messages").document(msgId).set(
                mapOf(
                    "studentUid" to studentUid,
                    "senderUid" to adminUid,
                    "senderRole" to "admin",
                    "senderName" to adminSenderName.trim().ifBlank { "Admin Support" }.take(80),
                    "text" to cleanText,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            recordAuditLog(
                actionType = "support_reply",
                targetId = thread.studentEmail.ifBlank { studentUid },
                targetLabel = thread.studentName,
                reasonOrDetails = "Replied to student support inquiry"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, threadPath)
            Result.failure(e)
        }
    }
}
