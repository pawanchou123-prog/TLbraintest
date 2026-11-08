package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.OWNER_ADMIN_EMAIL
import com.example.data.model.sanitizeEmailKey
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AdminRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun bothAuthorizedMainAdmins_haveFullAdminPermissions_andAuditLogsAreImmutable() = runBlocking {
        // 1. Verify Authorized Admin 1: choudharyriti92@gmail.com
        val admin1Uid = signInVerifiedTestUser(OWNER_ADMIN_EMAIL)
        val adminRepo = AdminRepository(firestore, auth)

        val verify1 = withTimeout(5000L) {
            adminRepo.verifyAndSyncMainAdminSession(
                uid = admin1Uid,
                rawEmail = OWNER_ADMIN_EMAIL,
                displayName = "Pritam Choudhary",
                isEmailVerified = true
            )
        }
        assertTrue("Expected Admin 1 verification to succeed, got $verify1", verify1.isSuccess)

        // 2. Verify Authorized Admin 2: Pawanchou123@gmail.com (mixed-case input)
        val admin2Uid = signInVerifiedTestUser("Pawanchou123@gmail.com")
        val verify2 = withTimeout(5000L) {
            adminRepo.verifyAndSyncMainAdminSession(
                uid = admin2Uid,
                rawEmail = "Pawanchou123@gmail.com",
                displayName = "Pawan Choudhary",
                isEmailVerified = true
            )
        }
        assertTrue("Expected Admin 2 verification to succeed, got $verify2", verify2.isSuccess)

        // Admin 2 approves a student Gmail account
        val studentEmail = "rohan_${UUID.randomUUID().toString().take(6)}@student.com"
        val nowMs = System.currentTimeMillis()
        val endMs = nowMs + (90L * 24L * 60L * 60L * 1000L)

        val saveRes = withTimeout(5000L) {
            adminRepo.saveStudentAccount(
                email = studentEmail,
                studentName = "Rohan Sharma",
                studentClass = 10,
                preferredLanguage = "en",
                subscriptionActive = true,
                paymentMethod = "upi",
                startMs = nowMs,
                endMs = endMs,
                adminNotes = "Approved by Admin 2"
            )
        }
        assertTrue(saveRes.isSuccess)

        val students = withTimeout(4000L) {
            adminRepo.observeAllAllowedStudents().first { list -> list.any { it.email == studentEmail } }
        }
        val rohanAllowed = students.first { it.email == studentEmail }
        assertTrue(rohanAllowed.isSubscriptionCurrentlyValid(nowMs))

        // Audit log must exist and must reject deletion/tampering even by Admin
        val logs = withTimeout(4000L) {
            adminRepo.observeAuditLogs().first { list -> list.any { it.targetId == studentEmail } }
        }
        val createdLog = logs.first { it.targetId == studentEmail }
        assertEquals("pawanchou123@gmail.com", createdLog.adminEmail)

        try {
            withTimeout(4000L) {
                firestore.collection("admin_audit_logs").document(createdLog.id).delete().await()
            }
            fail("Expected PERMISSION_DENIED when attempting to delete an immutable AdminAuditLog")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull(fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
        }
    }

    @Test
    fun unauthorizedGoogleAccount_andStudentAccount_areStrictlyDeniedFromAdmin() = runBlocking {
        // 1. Random unauthorized Google account (verified email, not in allowlist)
        val intruderEmail = "intruder_${UUID.randomUUID().toString().take(6)}@gmail.com"
        val intruderUid = signInVerifiedTestUser(intruderEmail)
        val adminRepo = AdminRepository(firestore, auth)

        val intruderVerify = withTimeout(5000L) {
            adminRepo.verifyAndSyncMainAdminSession(
                uid = intruderUid,
                rawEmail = intruderEmail,
                displayName = "Intruder",
                isEmailVerified = true
            )
        }
        assertTrue("Unauthorized Google account must be denied", intruderVerify.isFailure)

        // Intruder cannot self-assign /admins/{uid}
        try {
            withTimeout(4000L) {
                firestore.collection("admins").document(intruderUid).set(
                    mapOf(
                        "uid" to intruderUid,
                        "email" to intruderEmail,
                        "name" to "Intruder",
                        "role" to "admin"
                    )
                ).await()
            }
            fail("Expected PERMISSION_DENIED on self-assigned admin role")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull(fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
        }

        // 2. Approved Student account attempting to access /admin collections
        val studentEmail = "priya_${UUID.randomUUID().toString().take(6)}@student.com"
        val studentUid = signInVerifiedTestUser(studentEmail)

        val studentAdminCheck = withTimeout(5000L) {
            adminRepo.verifyAndSyncMainAdminSession(
                uid = studentUid,
                rawEmail = studentEmail,
                displayName = "Priya Student",
                isEmailVerified = true
            )
        }
        assertTrue("Student account must be denied from /admin", studentAdminCheck.isFailure)

        try {
            withTimeout(3000L) {
                adminRepo.observeAllAllowedStudents().first()
            }
            fail("Expected PERMISSION_DENIED when student lists allowed_students")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull(fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
        }

        // 3. Unverified spoofed account must be blocked by Firestore rules
        val unverifiedEmail = "spoof_${UUID.randomUUID().toString().take(6)}@gmail.com"
        val unverifiedUid = signInTestUser(unverifiedEmail)
        val spoofCheck = withTimeout(5000L) {
            adminRepo.verifyAndSyncMainAdminSession(
                uid = unverifiedUid,
                rawEmail = OWNER_ADMIN_EMAIL,
                displayName = "Spoofer",
                isEmailVerified = false
            )
        }
        assertTrue("Unverified session must fail admin verification", spoofCheck.isFailure)
    }

    @Test
    fun subscriptionLifecycle_studentManagement_andSupportInbox_endToEnd() = runBlocking {
        // Student signs in, creates profile, submits pending Rs. 50 UPI request, and sends support inquiry
        val studentEmail = "aman_${UUID.randomUUID().toString().take(6)}@student.com"
        val studentUid = signInVerifiedTestUser(studentEmail)
        val studentRepo = TlBrainRepository(firestore, auth)

        val studentProfile = withTimeout(5000L) {
            studentRepo.ensureUserProfileInitialized(studentUid, studentEmail, "Aman Kumar", 9, "en").getOrThrow()
        }
        withTimeout(5000L) {
            studentRepo.recordActivityCompletion(
                profile = studentProfile,
                activityType = "quiz",
                title = "Class 9 Science",
                studentClass = 9,
                score = 60,
                totalQuestions = 6,
                correctAnswers = 6,
                xpEarned = 30,
                durationSeconds = 40
            ).getOrThrow()
            studentRepo.submitPendingSubscriptionOrAccessRequest(
                studentUid = studentUid,
                studentEmail = studentEmail,
                studentName = "Aman Kumar",
                studentClass = 9,
                paymentMethod = "upi",
                referenceCode = "UTR998877"
            ).getOrThrow()
            studentRepo.sendSupportMessage(
                studentUid = studentUid,
                studentName = "Aman Kumar",
                studentEmail = studentEmail,
                studentClass = 9,
                senderRole = "student",
                senderName = "Aman Kumar",
                text = "Sir I paid Rs. 50 via UPI UTR998877, please verify."
            ).getOrThrow()
        }

        // Switch to Authorized Admin 1 (Pritam Choudhary)
        val adminUid = signInVerifiedTestUser(OWNER_ADMIN_EMAIL)
        val adminRepo = AdminRepository(firestore, auth)
        withTimeout(5000L) {
            adminRepo.verifyAndSyncMainAdminSession(adminUid, OWNER_ADMIN_EMAIL, "Pritam Choudhary", true).getOrThrow()
        }

        // 1. Admin reviews and approves pending subscription request manually
        val pendingRec = withTimeout(4000L) {
            adminRepo.observeAllSubscriptionRecords()
                .first { list -> list.any { it.studentEmail == studentEmail && it.status == "pending" } }
                .first { it.studentEmail == studentEmail && it.status == "pending" }
        }
        val nowMs = System.currentTimeMillis()
        val endMs = nowMs + (90L * 24L * 60L * 60L * 1000L)
        withTimeout(5000L) {
            adminRepo.reviewSubscriptionRequest(
                record = pendingRec,
                approve = true,
                paymentMethod = "upi",
                startMs = nowMs,
                endMs = endMs,
                internalVerificationNotes = "Verified UTR998877 in bank statement"
            ).getOrThrow()
        }

        val approvedStudent = withTimeout(4000L) {
            adminRepo.observeAllAllowedStudents()
                .first { list -> list.any { it.email == studentEmail && it.subscriptionActive } }
                .first { it.email == studentEmail }
        }
        assertTrue(approvedStudent.isSubscriptionCurrentlyValid(nowMs))

        // 2. Admin adjusts student points with mandatory reason and grants badge
        val latestAmanProfile = withTimeout(4000L) {
            adminRepo.observeAllUserProfiles()
                .first { list -> list.any { it.userId == studentUid } }
                .first { it.userId == studentUid }
        }
        withTimeout(5000L) {
            adminRepo.adjustStudentPointsWithReason(latestAmanProfile, 50, "Science Olympiad bonus").getOrThrow()
            adminRepo.toggleStudentBadge(latestAmanProfile, "sharp_accuracy", true, "100% Science score").getOrThrow()
        }

        val updatedAmanProfile = withTimeout(4000L) {
            adminRepo.observeAllUserProfiles()
                .first { list -> list.any { it.userId == studentUid && it.totalPoints == 110L } }
                .first { it.userId == studentUid }
        }
        assertEquals(110L, updatedAmanProfile.totalPoints)
        assertTrue(updatedAmanProfile.badges.contains("sharp_accuracy"))

        // 3. Admin expires subscription and verifies historical student profile & quiz records remain intact
        withTimeout(5000L) {
            adminRepo.extendOrExpireSubscription(
                student = approvedStudent,
                newStartMs = nowMs - 100000L,
                newEndMs = nowMs - 1000L,
                markActive = false,
                paymentMethod = "upi",
                internalNotes = "3-month term ended"
            ).getOrThrow()
        }
        val expiredSnap = firestore.collection("allowed_students").document(sanitizeEmailKey(studentEmail)).get().await()
        assertFalse(expiredSnap.getBoolean("subscriptionActive") ?: true)

        val preservedProfileSnap = firestore.collection("users").document(studentUid).get().await()
        assertTrue("Student profile must NOT be deleted when subscription expires", preservedProfileSnap.exists())
        assertEquals(110L, preservedProfileSnap.getLong("totalPoints"))

        val preservedQuizResults = withTimeout(4000L) {
            adminRepo.observeAllQuizResults().first { list -> list.any { it.userId == studentUid } }
        }
        assertTrue("Historical quiz results must NOT be deleted when subscription expires", preservedQuizResults.isNotEmpty())

        // 4. Admin replies in 1-to-1 Support Inbox and clears unreadByAdmin
        val supportThread = withTimeout(4000L) {
            adminRepo.observeAllSupportThreads()
                .first { list -> list.any { it.studentUid == studentUid } }
                .first { it.studentUid == studentUid }
        }
        assertTrue(supportThread.unreadByAdmin)

        withTimeout(5000L) {
            adminRepo.replyToSupportThread(
                thread = supportThread,
                adminSenderName = "Pritam Choudhary",
                replyText = "Your Rs. 50 UPI pass has been verified. Thank you!"
            ).getOrThrow()
        }

        val threadMessages = withTimeout(4000L) {
            adminRepo.observeThreadMessages(studentUid).first { it.size >= 2 }
        }
        assertTrue(threadMessages.any { it.senderRole == "admin" && it.text.contains("verified") })
    }
}
