package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CurriculumCatalog
import com.example.data.SamplePreviewCatalog
import com.example.navigation.StudentRoutes
import com.example.ui.auth.StudentAuthManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context and verify branding`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("TL Brain Test", context.getString(R.string.app_name))
        assertEquals("Learn, Think, Play, Improve.", context.getString(R.string.brand_tagline))
    }

    @Test
    fun `student application routes strictly isolate app namespace from admin and question manager`() {
        assertTrue(StudentRoutes.isAllowedStudentNamespace(StudentRoutes.DASHBOARD))
        assertTrue(StudentRoutes.isAllowedStudentNamespace(StudentRoutes.QUIZ_HUB))
        assertTrue(StudentRoutes.isAllowedStudentNamespace(StudentRoutes.GAMES_HUB))
        assertTrue(StudentRoutes.isAllowedStudentNamespace(StudentRoutes.SUBSCRIPTION))
        assertTrue(StudentRoutes.isAllowedStudentNamespace("/app/dashboard"))
        assertFalse(StudentRoutes.isAllowedStudentNamespace("/admin"))
        assertFalse(StudentRoutes.isAllowedStudentNamespace("/admin/users"))
        assertFalse(StudentRoutes.isAllowedStudentNamespace("admin/dashboard"))
        assertFalse(StudentRoutes.isAllowedStudentNamespace("/question-manager"))
        assertFalse(StudentRoutes.isAllowedStudentNamespace("question-manager/editor"))
    }

    @Test
    fun `student quiz categories games and terms persistence are complete`() {
        assertEquals(10, CurriculumCatalog.coreQuizCategories.size)
        assertTrue(CurriculumCatalog.games.size >= 13)

        // Every core category has English and Hindi preview coverage
        CurriculumCatalog.coreQuizCategories.forEach { category ->
            val enSample = SamplePreviewCatalog.sampleQuestionsForCategory(category, 10, "en")
            val hiSample = SamplePreviewCatalog.sampleQuestionsForCategory(category, 10, "hi")
            assertTrue("Expected sample EN questions for $category", enSample.isNotEmpty())
            assertTrue("Expected sample HI questions for $category", hiSample.isNotEmpty())
        }

        // Verify local first-login Terms acceptance persistence
        val context = ApplicationProvider.getApplicationContext<Context>()
        StudentAuthManager.saveTermsAcceptedLocally(context, false)
        assertFalse(StudentAuthManager.hasAcceptedTermsLocally(context))
        StudentAuthManager.saveTermsAcceptedLocally(context, true)
        assertTrue(StudentAuthManager.hasAcceptedTermsLocally(context))
    }

    @Test
    fun `admin application routes and two authorized admin emails are strictly isolated`() {
        assertTrue(com.example.navigation.AdminRoutes.isAllowedAdminNamespace(com.example.navigation.AdminRoutes.DASHBOARD))
        assertTrue(com.example.navigation.AdminRoutes.isAllowedAdminNamespace(com.example.navigation.AdminRoutes.STUDENTS))
        assertTrue(com.example.navigation.AdminRoutes.isAllowedAdminNamespace(com.example.navigation.AdminRoutes.SUBSCRIPTIONS))
        assertTrue(com.example.navigation.AdminRoutes.isAllowedAdminNamespace(com.example.navigation.AdminRoutes.SUPPORT))
        assertTrue(com.example.navigation.AdminRoutes.isAllowedAdminNamespace("/admin/dashboard"))
        assertFalse(com.example.navigation.AdminRoutes.isAllowedAdminNamespace("/app/dashboard"))
        assertFalse(com.example.navigation.AdminRoutes.isAllowedAdminNamespace("app/quiz"))
        assertFalse(com.example.navigation.AdminRoutes.isAllowedAdminNamespace("/question-manager"))

        // Verify exact two pre-authorized Main Admin Google accounts (case-insensitive)
        assertTrue(com.example.data.model.isPreAuthorizedMainAdminEmail("choudharyriti92@gmail.com"))
        assertTrue(com.example.data.model.isPreAuthorizedMainAdminEmail("Pawanchou123@gmail.com"))
        assertTrue(com.example.data.model.isPreAuthorizedMainAdminEmail("pawanchou123@gmail.com"))
        assertFalse(com.example.data.model.isPreAuthorizedMainAdminEmail("student@gmail.com"))
        assertFalse(com.example.data.model.isPreAuthorizedMainAdminEmail("intruder@gmail.com"))
        assertFalse(com.example.data.model.isPreAuthorizedMainAdminEmail(null))
    }

    @Test
    fun `question manager routes class subjects and csv duplicate validation work accurately`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals(
            "TL BRAIN TEST – QUESTION MANAGEMENT",
            context.getString(R.string.qm_brand_title)
        )

        // Verify /question-manager route isolation
        assertTrue(com.example.navigation.QuestionManagerRoutes.isAllowedQuestionManagerNamespace(com.example.navigation.QuestionManagerRoutes.DASHBOARD))
        assertTrue(com.example.navigation.QuestionManagerRoutes.isAllowedQuestionManagerNamespace(com.example.navigation.QuestionManagerRoutes.EDITOR))
        assertTrue(com.example.navigation.QuestionManagerRoutes.isAllowedQuestionManagerNamespace(com.example.navigation.QuestionManagerRoutes.QUESTION_BANK))
        assertTrue(com.example.navigation.QuestionManagerRoutes.isAllowedQuestionManagerNamespace(com.example.navigation.QuestionManagerRoutes.IMPORT_EXPORT))
        assertTrue(com.example.navigation.QuestionManagerRoutes.isAllowedQuestionManagerNamespace("/question-manager/dashboard"))
        assertFalse(com.example.navigation.QuestionManagerRoutes.isAllowedQuestionManagerNamespace("/app/dashboard"))
        assertFalse(com.example.navigation.QuestionManagerRoutes.isAllowedQuestionManagerNamespace("/admin/dashboard"))

        // Verify Class 1-5, 6-8, 9-10, and 11-12 curriculum organization
        assertEquals(
            listOf("Mathematics", "English", "Hindi", "EVS", "General Knowledge"),
            CurriculumCatalog.questionManagerSubjectsForClass(3)
        )
        assertEquals(
            listOf("Mathematics", "Science", "English", "Hindi", "Social Science", "General Knowledge"),
            CurriculumCatalog.questionManagerSubjectsForClass(7)
        )
        assertEquals(
            listOf("Mathematics", "Science", "Social Science", "English", "Hindi"),
            CurriculumCatalog.questionManagerSubjectsForClass(10)
        )
        assertEquals(
            listOf(
                "Physics", "Chemistry", "Biology", "Mathematics",
                "Accountancy", "Business Studies", "Economics",
                "History", "Geography", "Political Science", "English"
            ),
            CurriculumCatalog.questionManagerSubjectsForClass(12)
        )

        // Verify CSV parser catches invalid rows and detects duplicates against existing database items
        val existingItem = com.example.data.model.QuestionItem(
            id = "q_existing_1",
            studentClass = 10L,
            subject = "Mathematics",
            language = "en",
            questionText = "What is the degree of a quadratic polynomial?",
            options = listOf("1", "2", "3", "4"),
            correctIndex = 1L,
            explanation = "Highest power is 2."
        )
        val csvContent = """
            class,subject,topic,language,question_text,option_a,option_b,option_c,option_d,correct_option,explanation,difficulty,points,time_limit_seconds,image_url,status
            10,Mathematics,Algebra,en,"What is the degree of a quadratic polynomial?",1,2,3,4,B,"Highest power is 2.",easy,10,30,,draft
            8,Science,Force,en,"Valid new question?",OptA,OptB,OptC,OptD,C,"Valid explanation.",medium,15,40,https://tlbraintest.app/img.png,draft
            99,Science,Force,en,"Invalid class and http image",OptA,,OptC,OptD,Z,"",easy,10,30,http://insecure.example.com/a.png,draft
        """.trimIndent()

        val preview = com.example.data.QuestionCsvHelper.parseAndValidateCsv(csvContent, listOf(existingItem))
        assertEquals(3, preview.rows.size)
        assertEquals(1, preview.validNonDuplicateRows.size)
        assertEquals(1, preview.validDuplicateRows.size)
        assertTrue(preview.validDuplicateRows.first().isDuplicateInDatabase)
        assertEquals(1, preview.invalidRows.size)
    }

    @Test
    fun `student and admin email password authentication input validation behaves safely`() = kotlinx.coroutines.runBlocking {
        // Invalid or empty email format
        val invalidEmailRes = StudentAuthManager.signInWithEmailPassword("", "password123")
        assertTrue(invalidEmailRes.isFailure)

        // Invalid or empty password format
        val emptyPassRes = StudentAuthManager.signInWithEmailPassword("student@gmail.com", "")
        assertTrue(emptyPassRes.isFailure)

        // Password reset empty email validation
        val emptyResetRes = StudentAuthManager.sendPasswordReset("")
        assertTrue(emptyResetRes.isFailure)

        // Admin invalid format validation
        val invalidAdminRes = com.example.ui.auth.AdminAuthManager.signInWithEmailPassword("not-an-email", "pass")
        assertTrue(invalidAdminRes.isFailure)
    }

    @Test
    fun `sound effect catalog and procedural pcm wav synthesizer produce valid CC0 audio`() {
        // 1. Verify SoundEffect catalog completeness
        assertTrue("Catalog must contain at least 25 sound effects", com.example.core.audio.SoundEffect.entries.size >= 25)
        com.example.core.audio.SoundEffect.entries.forEach { effect ->
            assertTrue("Effect ID must not be blank: ${effect.name}", effect.id.isNotBlank())
            assertTrue("Effect display name must not be blank: ${effect.name}", effect.displayName.isNotBlank())
            assertTrue("Effect description must not be blank: ${effect.name}", effect.description.isNotBlank())
        }

        // 2. Verify RIFF WAVE header synthesis for key sounds
        val sampleEffects = listOf(
            com.example.core.audio.SoundEffect.CLICK,
            com.example.core.audio.SoundEffect.CORRECT,
            com.example.core.audio.SoundEffect.WRONG,
            com.example.core.audio.SoundEffect.SUCCESS,
            com.example.core.audio.SoundEffect.QUIZ_START,
            com.example.core.audio.SoundEffect.TIMER_WARN,
            com.example.core.audio.SoundEffect.MATCH_FOUND
        )
        sampleEffects.forEach { effect ->
            val wav = com.example.core.audio.PcmSoundSynthesizer.synthesizeWav(effect)
            assertTrue("WAV byte array must be at least 44 bytes (header size)", wav.size >= 44)
            // Check RIFF magic bytes
            assertEquals('R'.code.toByte(), wav[0])
            assertEquals('I'.code.toByte(), wav[1])
            assertEquals('F'.code.toByte(), wav[2])
            assertEquals('F'.code.toByte(), wav[3])
            // Check WAVE magic bytes
            assertEquals('W'.code.toByte(), wav[8])
            assertEquals('A'.code.toByte(), wav[9])
            assertEquals('V'.code.toByte(), wav[10])
            assertEquals('E'.code.toByte(), wav[11])
        }

        // 3. Verify SoundManager preferences, volume clamping, and graceful play
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.core.audio.SoundManager.init(context)

        // Toggle ON/OFF
        com.example.core.audio.SoundManager.setSoundEnabled(true)
        assertTrue(com.example.core.audio.SoundManager.isSoundEnabled())
        com.example.core.audio.SoundManager.setSoundEnabled(false)
        assertFalse(com.example.core.audio.SoundManager.isSoundEnabled())
        com.example.core.audio.SoundManager.setSoundEnabled(true)

        // Volume clamping
        com.example.core.audio.SoundManager.setVolume(1.5f)
        assertEquals(1.0f, com.example.core.audio.SoundManager.getVolume(), 0.001f)
        com.example.core.audio.SoundManager.setVolume(-0.5f)
        assertEquals(0.0f, com.example.core.audio.SoundManager.getVolume(), 0.001f)
        com.example.core.audio.SoundManager.setVolume(0.85f)
        assertEquals(0.85f, com.example.core.audio.SoundManager.getVolume(), 0.001f)

        // Graceful play without crashing in JVM test runner
        com.example.core.audio.SoundManager.play(com.example.core.audio.SoundEffect.CLICK)
        com.example.core.audio.SoundManager.play(com.example.core.audio.SoundEffect.CORRECT)
        com.example.core.audio.SoundManager.play(com.example.core.audio.SoundEffect.SUCCESS)
    }
}
