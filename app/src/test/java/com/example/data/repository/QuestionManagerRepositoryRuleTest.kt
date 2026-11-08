package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.QuestionCsvHelper
import com.example.data.model.QuestionItem
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QuestionManagerRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private lateinit var qmRepository: QuestionManagerRepository
    private lateinit var studentRepository: TlBrainRepository

    @Before
    override fun setUpFirebase() {
        super.setUpFirebase()
        qmRepository = QuestionManagerRepository(firestore, auth)
        studentRepository = TlBrainRepository(firestore, auth)
    }

    @Test
    fun `authorized question manager workflow role isolation and csv import succeed while admin collections stay blocked`() = runBlocking {
        withTimeout(30_000L) {
            val suffix = System.currentTimeMillis().toString().takeLast(6)
            val managerEmail = "qmanager.$suffix@gmail.com"

            // 1. Main Admin signs in and authorizes Question Manager + customizes Subject Catalog
            val adminUid = signInVerifiedTestUser("choudharyriti92@gmail.com")
            val adminSessionResult = qmRepository.verifyAndSyncOperatorSession(
                uid = adminUid,
                rawEmail = "choudharyriti92@gmail.com",
                displayName = "Pritam Choudhary",
                isEmailVerified = true
            )
            assertTrue("Main Admin session should succeed", adminSessionResult.isSuccess)
            val adminSession = adminSessionResult.getOrThrow()
            assertTrue(adminSession.isMainAdmin)

            val allowRes = qmRepository.saveAllowedQuestionManager(
                session = adminSession,
                email = managerEmail,
                managerName = "Curriculum Editor",
                isActive = true,
                canPublishDirectly = false,
                canDeleteQuestions = true,
                notes = "Class 9-10 Science Editor"
            )
            assertTrue("Main Admin should authorize Question Manager", allowRes.isSuccess)

            val catalogRes = qmRepository.saveSubjectCatalog(
                session = adminSession,
                classes1To5 = listOf("Mathematics", "English", "Hindi", "EVS", "General Knowledge"),
                classes6To8 = listOf("Mathematics", "Science", "English", "Hindi", "Social Science", "General Knowledge"),
                classes9To10 = listOf("Mathematics", "Science", "Social Science", "English", "Hindi"),
                classes11To12 = listOf("Physics", "Chemistry", "Biology", "Mathematics", "Accountancy", "Business Studies", "Economics", "History", "Geography", "Political Science", "English")
            )
            assertTrue("Main Admin should save Subject Catalog", catalogRes.isSuccess)

            // 2. Authorized Question Manager signs in and syncs session
            val managerUid = signInVerifiedTestUser(managerEmail)
            val mgrSessionResult = qmRepository.verifyAndSyncOperatorSession(
                uid = managerUid,
                rawEmail = managerEmail,
                displayName = "Curriculum Editor",
                isEmailVerified = true
            )
            assertTrue("Authorized Question Manager session should succeed", mgrSessionResult.isSuccess)
            val mgrSession = mgrSessionResult.getOrThrow()
            assertFalse("Question Manager must NOT receive Main Admin role", mgrSession.isMainAdmin)
            assertFalse(mgrSession.canPublishDirectly)
            assertTrue(mgrSession.canDeleteQuestions)

            // 3. Verify Question Manager is strictly blocked from Main Admin collections in Firestore Rules
            val blockedStudentsRead = runCatching {
                firestore.collection("allowed_students").get().await()
            }
            assertEquals(
                FirebaseFirestoreException.Code.PERMISSION_DENIED,
                blockedStudentsRead.exceptionOrNull()?.unwrapFirestoreException()?.code
            )

            val blockedSubscriptionsRead = runCatching {
                firestore.collection("subscription_records").get().await()
            }
            assertEquals(
                FirebaseFirestoreException.Code.PERMISSION_DENIED,
                blockedSubscriptionsRead.exceptionOrNull()?.unwrapFirestoreException()?.code
            )

            val blockedAdminLogsRead = runCatching {
                firestore.collection("admin_audit_logs").get().await()
            }
            assertEquals(
                FirebaseFirestoreException.Code.PERMISSION_DENIED,
                blockedAdminLogsRead.exceptionOrNull()?.unwrapFirestoreException()?.code
            )

            assertTrue(
                "Question Manager cannot self-promote or add other managers",
                qmRepository.saveAllowedQuestionManager(
                    session = mgrSession,
                    email = "other@gmail.com",
                    managerName = "Other",
                    isActive = true,
                    canPublishDirectly = true,
                    canDeleteQuestions = true,
                    notes = ""
                ).isFailure
            )

            // 4. Question Manager cannot publish directly when canPublishDirectly == false
            val directPubAttempt = qmRepository.saveOrUpdateQuestion(
                session = mgrSession,
                existingQuestion = null,
                studentClass = 10,
                subject = "Science",
                topic = "Light Reflection",
                language = "en",
                questionText = "What is the speed of light in vacuum ($suffix)?",
                options = listOf("3x10^8 m/s", "3x10^6 m/s", "3x10^5 m/s", "3x10^4 m/s"),
                correctIndex = 0,
                explanation = "Light travels at approximately 3 x 10^8 m/s in vacuum.",
                difficulty = "medium",
                points = 15,
                timerSeconds = 30,
                imageUrl = "",
                desiredAction = "publish"
            )
            assertTrue("Restricted manager must not publish directly", directPubAttempt.isFailure)

            // 5. Question Manager creates a Draft, edits it, submits for review, and runs bulk CSV import
            val draftSave = qmRepository.saveOrUpdateQuestion(
                session = mgrSession,
                existingQuestion = null,
                studentClass = 10,
                subject = "Science",
                topic = "Light Reflection",
                language = "en",
                questionText = "Which mirror is used as a rear-view mirror in vehicles ($suffix)?",
                options = listOf("Concave mirror", "Convex mirror", "Plane mirror", "Parabolic mirror"),
                correctIndex = 1,
                explanation = "Convex mirrors always form erect, diminished images with a wider field of view.",
                difficulty = "easy",
                points = 10,
                timerSeconds = 30,
                imageUrl = "https://tlbraintest.app/diagrams/convex_mirror.png",
                desiredAction = "draft"
            )
            assertTrue("Draft creation should succeed", draftSave.isSuccess)
            val draftId = draftSave.getOrThrow()

            val visibleAfterDraft = qmRepository.observeQuestionsForSession(mgrSession).first { list ->
                list.any { it.id == draftId }
            }
            val savedDraft = visibleAfterDraft.first { it.id == draftId }
            assertFalse(savedDraft.isPublished)
            assertEquals("draft", savedDraft.effectiveReviewStatus)

            val submitRes = qmRepository.submitDraftForReview(mgrSession, savedDraft)
            assertTrue("Submitting draft for review should succeed", submitRes.isSuccess)

            val csvRaw = """
                class,subject,topic,language,question_text,option_a,option_b,option_c,option_d,correct_option,explanation,difficulty,points,time_limit_seconds,image_url,status
                9,Mathematics,Polynomials,en,"Degree of constant non-zero polynomial ($suffix)?",0,1,2,3,A,"A non-zero constant polynomial has degree 0.",easy,10,25,,draft
            """.trimIndent()
            val preview = QuestionCsvHelper.parseAndValidateCsv(csvRaw, visibleAfterDraft)
            assertEquals(1, preview.validNonDuplicateRows.size)
            val importRes = qmRepository.importValidatedCsvRows(
                session = mgrSession,
                rowsToSave = preview.validNonDuplicateRows,
                publishImportedIfPermitted = false
            )
            assertTrue("Bulk CSV import should succeed", importRes.isSuccess)
            assertEquals(1, importRes.getOrThrow())

            val finalQuestions = qmRepository.observeQuestionsForSession(mgrSession).first { list ->
                list.count { it.authorId == managerUid } >= 2
            }
            val stats = qmRepository.computeDashboardStats(finalQuestions)
            assertTrue(stats.totalQuestions >= 2)
            assertTrue(stats.pendingReviewQuestions >= 1)
            assertTrue(stats.draftQuestions >= 1)
        }
    }

    @Test
    fun `unauthorized google user and student account are denied question manager access`() = runBlocking {
        withTimeout(25_000L) {
            val suffix = System.currentTimeMillis().toString().takeLast(6)

            // 1. Unapproved Google user tries to enter /question-manager
            val intruderEmail = "intruder.$suffix@gmail.com"
            val intruderUid = signInVerifiedTestUser(intruderEmail)
            val intruderSession = qmRepository.verifyAndSyncOperatorSession(
                uid = intruderUid,
                rawEmail = intruderEmail,
                displayName = "Intruder",
                isEmailVerified = true
            )
            assertTrue("Unauthorized Google user must be rejected", intruderSession.isFailure)

            // Direct Firestore attempt to self-assign question_manager role or create a question must fail
            val selfRoleAttempt = runCatching {
                firestore.collection("question_managers").document(intruderUid).set(
                    mapOf(
                        "uid" to intruderUid,
                        "email" to intruderEmail,
                        "name" to "Intruder",
                        "role" to "question_manager",
                        "emailKey" to intruderEmail.replace(".", "_")
                    )
                ).await()
            }
            assertEquals(
                FirebaseFirestoreException.Code.PERMISSION_DENIED,
                selfRoleAttempt.exceptionOrNull()?.unwrapFirestoreException()?.code
            )

            val writeQuestionAttempt = runCatching {
                firestore.collection("questions").document("unauth_$suffix").set(
                    mapOf(
                        "authorId" to intruderUid,
                        "studentClass" to 8L,
                        "subject" to "Science",
                        "language" to "en",
                        "questionText" to "Unauthorized question?",
                        "options" to listOf("A", "B", "C", "D"),
                        "correctIndex" to 0L,
                        "explanation" to "None",
                        "difficulty" to "easy",
                        "points" to 10L,
                        "timerSeconds" to 30L,
                        "imageUrl" to "",
                        "isPublished" to true,
                        "reviewStatus" to "approved",
                        "source" to "manual"
                    )
                ).await()
            }
            assertEquals(
                FirebaseFirestoreException.Code.PERMISSION_DENIED,
                writeQuestionAttempt.exceptionOrNull()?.unwrapFirestoreException()?.code
            )

            // 2. Approved Student user tries to enter /question-manager -> denied
            val studentEmail = "student.qm.$suffix@gmail.com"
            val studentUid = signInVerifiedTestUser(studentEmail)
            val studentQmAttempt = qmRepository.verifyAndSyncOperatorSession(
                uid = studentUid,
                rawEmail = studentEmail,
                displayName = "Student",
                isEmailVerified = true
            )
            assertTrue("Student account must never enter Question Management", studentQmAttempt.isFailure)
        }
    }

    @Test
    fun `main admin approval rejection audit immutability and live student app integration`() = runBlocking {
        withTimeout(30_000L) {
            val suffix = System.currentTimeMillis().toString().takeLast(6)
            val managerEmail = "reviewer.qm.$suffix@gmail.com"

            // 1. Main Admin #2 (Pawanchou123@gmail.com) authorizes a Question Manager
            val admin2Uid = signInVerifiedTestUser("Pawanchou123@gmail.com")
            val admin2Session = qmRepository.verifyAndSyncOperatorSession(
                uid = admin2Uid,
                rawEmail = "Pawanchou123@gmail.com",
                displayName = "Pawan Choudhary",
                isEmailVerified = true
            ).getOrThrow()
            assertTrue(admin2Session.isMainAdmin)

            qmRepository.saveAllowedQuestionManager(
                session = admin2Session,
                email = managerEmail,
                managerName = "Hindi Subject Editor",
                isActive = true,
                canPublishDirectly = false,
                canDeleteQuestions = false,
                notes = "Review required"
            ).getOrThrow()

            // 2. Question Manager submits two questions (one to approve, one to reject)
            val mgrUid = signInVerifiedTestUser(managerEmail)
            val mgrSession = qmRepository.verifyAndSyncOperatorSession(
                uid = mgrUid,
                rawEmail = managerEmail,
                displayName = "Hindi Subject Editor",
                isEmailVerified = true
            ).getOrThrow()

            val approvedQId = qmRepository.saveOrUpdateQuestion(
                session = mgrSession,
                existingQuestion = null,
                studentClass = 8,
                subject = "Science",
                topic = "Force and Pressure",
                language = "en",
                questionText = "SI unit of force ($suffix) is?",
                options = listOf("Joule", "Newton", "Pascal", "Watt"),
                correctIndex = 1,
                explanation = "Newton (N) is the SI unit of force.",
                difficulty = "easy",
                points = 10,
                timerSeconds = 30,
                imageUrl = "",
                desiredAction = "pending_review"
            ).getOrThrow()

            val rejectedQId = qmRepository.saveOrUpdateQuestion(
                session = mgrSession,
                existingQuestion = null,
                studentClass = 8,
                subject = "Science",
                topic = "Force and Pressure",
                language = "en",
                questionText = "Draft with ambiguous wording ($suffix)?",
                options = listOf("One", "Two", "Three", "Four"),
                correctIndex = 0,
                explanation = "Needs clearer phrasing.",
                difficulty = "medium",
                points = 10,
                timerSeconds = 30,
                imageUrl = "",
                desiredAction = "pending_review"
            ).getOrThrow()

            // 3. Main Admin signs back in, approves + publishes first question, rejects second question
            signInVerifiedTestUser("Pawanchou123@gmail.com")
            val allAdminQuestions = qmRepository.observeQuestionsForSession(admin2Session).first { list ->
                list.any { it.id == approvedQId } && list.any { it.id == rejectedQId }
            }
            val qToApprove: QuestionItem = allAdminQuestions.first { it.id == approvedQId }
            val qToReject: QuestionItem = allAdminQuestions.first { it.id == rejectedQId }

            val approveRes = qmRepository.reviewAndSetQuestionState(
                session = admin2Session,
                question = qToApprove,
                newReviewStatus = "approved",
                publishNow = true,
                reviewNotes = "Verified accurate for Class 8 Science."
            )
            assertTrue("Main Admin approve & publish should succeed", approveRes.isSuccess)

            val rejectRes = qmRepository.reviewAndSetQuestionState(
                session = admin2Session,
                question = qToReject,
                newReviewStatus = "rejected",
                publishNow = false,
                reviewNotes = "Please clarify question stem before resubmitting."
            )
            assertTrue("Main Admin rejection should succeed", rejectRes.isSuccess)

            // Verify immutable audit logs exist and cannot be deleted even by Main Admin
            val logs = qmRepository.observeQuestionAuditLogs(admin2Session).first { it.isNotEmpty() }
            val approveLog = logs.firstOrNull { it.questionId == approvedQId && it.actionType == "approve_and_publish" }
            assertNotNull("Audit log must record Main Admin approval", approveLog)

            val deleteLogAttempt = runCatching {
                firestore.collection("question_audit_logs").document(logs.first().id).delete().await()
            }
            assertEquals(
                FirebaseFirestoreException.Code.PERMISSION_DENIED,
                deleteLogAttempt.exceptionOrNull()?.unwrapFirestoreException()?.code
            )

            // 4. Student signs in to /app (TlBrainRepository) -> sees ONLY approved & published question
            signInVerifiedTestUser("verified.student.$suffix@gmail.com")
            val studentFeed = studentRepository.observePublishedQuestions().first { list ->
                list.any { it.id == approvedQId }
            }
            assertTrue("Approved & published question must appear in Student App", studentFeed.any { it.id == approvedQId })
            assertFalse("Rejected question must NEVER appear in Student App", studentFeed.any { it.id == rejectedQId })
        }
    }
}
