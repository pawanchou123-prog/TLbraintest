package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TlBrainRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureUserProfile_andRecordActivity_updatesProfileResultsAndLeaderboard() = runBlocking {
        val uniqueEmail = "alice_${UUID.randomUUID().toString().take(8)}@student.com"
        val aliceUid = signInTestUser(uniqueEmail)
        val repository = TlBrainRepository(firestore, auth)

        val profileRes = withTimeout(5000L) {
            repository.ensureUserProfileInitialized(
                uid = aliceUid,
                email = uniqueEmail,
                fallbackName = "Alice Verma",
                studentClass = 10,
                preferredLang = "en"
            )
        }
        assertTrue(profileRes.isSuccess)
        val initialProfile = profileRes.getOrThrow()
        assertEquals(aliceUid, initialProfile.userId)
        assertTrue(com.example.navigation.StudentRoutes.isAllowedStudentNamespace("/app/dashboard"))
        assertTrue(!com.example.navigation.StudentRoutes.isAllowedStudentNamespace("/admin/students"))
        assertTrue(!com.example.navigation.StudentRoutes.isAllowedStudentNamespace("/question-manager/bank"))

        val updateRes = withTimeout(5000L) {
            repository.updateProfilePreferences(
                profile = initialProfile,
                newName = "Alice Verma",
                newClass = 10,
                newLanguage = "hi",
                soundEnabled = true,
                newSchoolName = "Govt Model Sr Sec School",
                newAvatarId = "quantum_star",
                termsAccepted = true
            )
        }
        assertTrue(updateRes.isSuccess)

        val recordRes = withTimeout(5000L) {
            repository.recordActivityCompletion(
                profile = initialProfile,
                activityType = "game",
                title = "Addition Challenge",
                studentClass = 10,
                score = 80,
                totalQuestions = 10,
                correctAnswers = 8,
                xpEarned = 40,
                durationSeconds = 25
            )
        }
        assertTrue(recordRes.isSuccess)

        val updatedProfile = withTimeout(3000L) {
            repository.observeUserProfile(aliceUid).first { it != null && it.totalPoints == 80L }
        }
        assertEquals("Govt Model Sr Sec School", updatedProfile?.schoolName)
        assertEquals("quantum_star", updatedProfile?.avatarId)
        assertEquals(80L, updatedProfile?.totalPoints)

        val results = withTimeout(3000L) {
            repository.observeStudentResults(aliceUid).first { it.isNotEmpty() }
        }
        assertEquals("Addition Challenge", results.first().title)

        val leaderboard = withTimeout(3000L) {
            repository.observeActiveLeaderboard().first { list -> list.any { it.userId == aliceUid && it.allTimeXp == 40L } }
        }
        assertTrue(leaderboard.any { it.userId == aliceUid })
    }

    @Test
    fun crossUserIsolation_cannotReadAnotherStudentsPrivateProfileOrSupportThread() = runBlocking {
        val aliceEmail = "alice_iso_${UUID.randomUUID().toString().take(6)}@student.com"
        val bobEmail = "bob_iso_${UUID.randomUUID().toString().take(6)}@student.com"

        val aliceUid = signInTestUser(aliceEmail)
        val aliceRepo = TlBrainRepository(firestore, auth)
        withTimeout(5000L) {
            aliceRepo.ensureUserProfileInitialized(aliceUid, aliceEmail, "Alice", 9, "en").getOrThrow()
            aliceRepo.sendSupportMessage(
                studentUid = aliceUid,
                studentName = "Alice",
                studentEmail = aliceEmail,
                studentClass = 9,
                senderRole = "student",
                senderName = "Alice",
                text = "Hello Admin Sir"
            ).getOrThrow()
        }

        // Switch session to Bob
        signInTestUser(bobEmail)
        try {
            withTimeout(5000L) {
                firestore.collection("users").document(aliceUid).get().await()
            }
            fail("Expected PERMISSION_DENIED when Bob reads Alice's private profile")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
        }

        try {
            withTimeout(3000L) {
                aliceRepo.observeStudentSupportMessages(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED when Bob observes Alice's support thread")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
        }
    }

    @Test
    fun unauthenticatedCaller_rejectedWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = TlBrainRepository(firestore, auth)

        try {
            withTimeout(3000L) {
                repository.observePublishedQuestions().first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated question stream")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
        }
    }
}
