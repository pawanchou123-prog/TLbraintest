package com.example.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiQuestionService"

data class GeneratedQuestionDraft(
    val questionText: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val difficulty: String,
    val points: Int,
    val timerSeconds: Int
)

class GeminiQuestionService {

    private val responseSchema = Schema.obj(
        properties = mapOf(
            "questions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "questionText" to Schema.string(description = "Clear curriculum question without emojis"),
                        "optionA" to Schema.string(description = "Option A"),
                        "optionB" to Schema.string(description = "Option B"),
                        "optionC" to Schema.string(description = "Option C"),
                        "optionD" to Schema.string(description = "Option D"),
                        "correctIndex" to Schema.integer(description = "0 for Option A, 1 for B, 2 for C, 3 for D"),
                        "explanation" to Schema.string(description = "Clear concise educational explanation"),
                        "difficulty" to Schema.enumeration(
                            values = listOf("easy", "medium", "hard"),
                            description = "Difficulty tier"
                        ),
                        "points" to Schema.integer(description = "Points between 10 and 30"),
                        "timerSeconds" to Schema.integer(description = "Recommended timer in seconds between 15 and 60")
                    )
                ),
                description = "List of generated educational multiple-choice questions"
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3-flash-preview",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = this@GeminiQuestionService.responseSchema
            }
        )
    }

    suspend fun generateDraftQuestions(
        studentClass: Int,
        subject: String,
        language: String,
        difficulty: String,
        topicHint: String,
        count: Int
    ): Result<List<GeneratedQuestionDraft>> {
        val safeCount = count.coerceIn(1, 8)
        val langLabel = if (language == "hi") "Hindi (Devanagari script)" else "English"
        val classLabel = if (studentClass == 0) "General Knowledge (All Classes)" else "Class $studentClass"
        val focusClause = if (topicHint.isNotBlank()) "Focus specifically on topic: ${topicHint.trim()}." else ""

        val prompt = """
            Generate $safeCount high-quality, factually accurate multiple-choice educational questions for $classLabel in subject "$subject".
            Write all questions, options, and explanations strictly in $langLabel.
            Target difficulty: $difficulty.
            $focusClause
            Do not use any emojis anywhere. Ensure exactly one option is unambiguously correct and correctIndex is 0, 1, 2, or 3.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val rawJson = response.text ?: return Result.failure(
                IllegalStateException("Gemini returned an empty response.")
            )
            val root = JSONObject(rawJson)
            val arr = root.getJSONArray("questions")
            val drafts = buildList {
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val options = listOf(
                        item.optString("optionA", "").trim(),
                        item.optString("optionB", "").trim(),
                        item.optString("optionC", "").trim(),
                        item.optString("optionD", "").trim()
                    )
                    if (options.all { it.isNotBlank() }) {
                        add(
                            GeneratedQuestionDraft(
                                questionText = item.optString("questionText", "").trim().take(950),
                                options = options.map { it.take(280) },
                                correctIndex = item.optInt("correctIndex", 0).coerceIn(0, 3),
                                explanation = item.optString("explanation", "").trim().take(950),
                                difficulty = item.optString("difficulty", difficulty).lowercase().let {
                                    if (it in listOf("easy", "medium", "hard")) it else "medium"
                                },
                                points = item.optInt("points", 10).coerceIn(5, 100),
                                timerSeconds = item.optInt("timerSeconds", 30).coerceIn(10, 180)
                            )
                        )
                    }
                }
            }
            if (drafts.isEmpty()) {
                Result.failure(IllegalStateException("No valid structured questions were returned."))
            } else {
                Result.success(drafts)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI question generation timed out", e)
            Result.failure(IllegalStateException("AI generation timed out after 60 seconds. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI question generation failed", e)
            Result.failure(e)
        }
    }
}
