package com.example.data

import com.example.data.model.QuestionItem

data class CsvQuestionDraftRow(
    val rowNumber: Int,
    val studentClass: Int,
    val subject: String,
    val topic: String,
    val language: String,
    val questionText: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val difficulty: String,
    val points: Int,
    val timerSeconds: Int,
    val imageUrl: String,
    val requestedPublish: Boolean,
    val errors: List<String>,
    val isDuplicateInDatabase: Boolean,
    val isDuplicateInFile: Boolean
) {
    val isValid: Boolean
        get() = errors.isEmpty()

    val normalizedDuplicateKey: String
        get() = "${studentClass}_${language.trim().lowercase()}_${questionText.trim().lowercase().replace(Regex("\\s+"), " ")}"
}

data class CsvImportPreview(
    val rows: List<CsvQuestionDraftRow>,
    val validNonDuplicateRows: List<CsvQuestionDraftRow>,
    val validDuplicateRows: List<CsvQuestionDraftRow>,
    val invalidRows: List<CsvQuestionDraftRow>
)

/**
 * RFC-4180 compliant CSV parser, validator, duplicate detector, and exporter
 * for Application 3: Question Management Application (/question-manager).
 */
object QuestionCsvHelper {

    val CSV_HEADERS: List<String> = listOf(
        "class",
        "subject",
        "topic",
        "language",
        "question_text",
        "option_a",
        "option_b",
        "option_c",
        "option_d",
        "correct_option",
        "explanation",
        "difficulty",
        "points",
        "time_limit_seconds",
        "image_url",
        "status"
    )

    fun sampleCsvTemplate(): String {
        val header = CSV_HEADERS.joinToString(",")
        val row1 = listOf(
            "10",
            "Mathematics",
            "Quadratic Equations",
            "en",
            "What is the degree of a quadratic polynomial?",
            "1",
            "2",
            "3",
            "4",
            "B",
            "A quadratic polynomial has highest power 2.",
            "easy",
            "10",
            "30",
            "",
            "draft"
        ).joinToString(",") { escapeCsvCell(it) }
        val row2 = listOf(
            "8",
            "Science",
            "Force and Pressure",
            "hi",
            "बल का SI मात्रक क्या है?",
            "जूल",
            "न्यूटन",
            "पास्कल",
            "वाट",
            "B",
            "बल का SI मात्रक न्यूटन (N) होता है।",
            "easy",
            "10",
            "30",
            "",
            "draft"
        ).joinToString(",") { escapeCsvCell(it) }
        return "$header\n$row1\n$row2"
    }

    fun exportQuestionsToCsv(questions: List<QuestionItem>): String {
        val sb = StringBuilder()
        sb.appendLine(CSV_HEADERS.joinToString(","))
        for (q in questions) {
            val answerLetter = when (q.correctIndex.toInt()) {
                0 -> "A"
                1 -> "B"
                2 -> "C"
                else -> "D"
            }
            val statusText = if (q.isPublished) "published" else q.effectiveReviewStatus
            val cells = listOf(
                q.studentClass.toString(),
                q.subject,
                q.topic,
                q.language,
                q.questionText,
                q.options.getOrElse(0) { "" },
                q.options.getOrElse(1) { "" },
                q.options.getOrElse(2) { "" },
                q.options.getOrElse(3) { "" },
                answerLetter,
                q.explanation,
                q.difficulty,
                q.points.toString(),
                q.timerSeconds.toString(),
                q.imageUrl,
                statusText
            )
            sb.appendLine(cells.joinToString(",") { escapeCsvCell(it) })
        }
        return sb.toString()
    }

    fun parseAndValidateCsv(
        rawCsv: String,
        existingQuestions: List<QuestionItem>
    ): CsvImportPreview {
        val records = parseCsvRecords(rawCsv.trim())
        if (records.isEmpty()) {
            return CsvImportPreview(emptyList(), emptyList(), emptyList(), emptyList())
        }

        val existingKeys = existingQuestions.map { it.normalizedDuplicateKey }.toSet()
        val seenKeysInFile = mutableSetOf<String>()

        // Detect if row 0 is header
        val firstCell = records.first().firstOrNull()?.trim()?.lowercase().orEmpty()
        val dataStartIdx = if (firstCell == "class" || firstCell == "student_class" || firstCell == "studentclass") 1 else 0

        val parsedRows = mutableListOf<CsvQuestionDraftRow>()
        for (i in dataStartIdx until records.size) {
            val rawCells = records[i]
            if (rawCells.all { it.isBlank() }) continue
            val rowNum = i + 1
            val errors = mutableListOf<String>()

            val classRaw = rawCells.getOrNull(0)?.trim().orEmpty()
            val studentClass = classRaw.toIntOrNull()
            if (studentClass == null || studentClass !in 0..12) {
                errors.add("Class must be between 0 (GK) and 12.")
            }

            val subject = rawCells.getOrNull(1)?.trim().orEmpty()
            if (subject.isBlank() || subject.length > 60) {
                errors.add("Subject is required (max 60 chars).")
            }

            val topic = rawCells.getOrNull(2)?.trim().orEmpty()
            if (topic.length > 120) {
                errors.add("Topic exceeds 120 characters.")
            }

            val langRaw = rawCells.getOrNull(3)?.trim()?.lowercase().orEmpty()
            val language = when (langRaw) {
                "en", "english" -> "en"
                "hi", "hindi", "हिन्दी" -> "hi"
                else -> {
                    errors.add("Language must be 'en'/'english' or 'hi'/'hindi'.")
                    "en"
                }
            }

            val questionText = rawCells.getOrNull(4)?.trim().orEmpty()
            if (questionText.length < 3 || questionText.length > 1000) {
                errors.add("Question text must be 3 to 1000 characters.")
            }

            val optA = rawCells.getOrNull(5)?.trim().orEmpty()
            val optB = rawCells.getOrNull(6)?.trim().orEmpty()
            val optC = rawCells.getOrNull(7)?.trim().orEmpty()
            val optD = rawCells.getOrNull(8)?.trim().orEmpty()
            val options = listOf(optA, optB, optC, optD)
            if (options.any { it.isBlank() || it.length > 300 }) {
                errors.add("All four options A, B, C, and D are required (max 300 chars each).")
            }

            val correctRaw = rawCells.getOrNull(9)?.trim()?.uppercase().orEmpty()
            val correctIndex = when (correctRaw) {
                "A", "0", "1" -> if (correctRaw == "1" && rawCells.getOrNull(9)?.trim() == "1") 0 else 0
                "B" -> 1
                "C" -> 2
                "D" -> 3
                else -> {
                    val numeric = correctRaw.toIntOrNull()
                    if (numeric != null && numeric in 0..3) {
                        numeric
                    } else {
                        errors.add("Correct option must be A, B, C, or D (or 0..3).")
                        0
                    }
                }
            }

            val explanation = rawCells.getOrNull(10)?.trim().orEmpty()
            if (explanation.isBlank() || explanation.length > 1000) {
                errors.add("Explanation of the correct answer is required (max 1000 chars).")
            }

            val diffRaw = rawCells.getOrNull(11)?.trim()?.lowercase().orEmpty()
            val difficulty = if (diffRaw in setOf("easy", "medium", "hard")) {
                diffRaw
            } else {
                errors.add("Difficulty must be easy, medium, or hard.")
                "medium"
            }

            val pointsRaw = rawCells.getOrNull(12)?.trim().orEmpty()
            val points = pointsRaw.toIntOrNull()
            if (points == null || points !in 1..1000) {
                errors.add("Points must be between 1 and 1000.")
            }

            val timerRaw = rawCells.getOrNull(13)?.trim().orEmpty()
            val timerSeconds = timerRaw.toIntOrNull()
            if (timerSeconds == null || timerSeconds !in 5..300) {
                errors.add("Time limit must be between 5 and 300 seconds.")
            }

            val imageUrl = rawCells.getOrNull(14)?.trim().orEmpty()
            if (imageUrl.isNotEmpty() && (!imageUrl.startsWith("https://") || imageUrl.length > 500)) {
                errors.add("Image URL must be empty or start with https:// (max 500 chars).")
            }

            val statusRaw = rawCells.getOrNull(15)?.trim()?.lowercase().orEmpty()
            val requestedPublish = statusRaw == "published" || statusRaw == "publish"

            val safeClass = studentClass ?: 10
            val dupKey = "${safeClass}_${language}_${questionText.lowercase().replace(Regex("\\s+"), " ")}"
            val dupDb = questionText.isNotBlank() && dupKey in existingKeys
            val dupFile = questionText.isNotBlank() && !seenKeysInFile.add(dupKey)

            parsedRows.add(
                CsvQuestionDraftRow(
                    rowNumber = rowNum,
                    studentClass = safeClass,
                    subject = subject,
                    topic = topic,
                    language = language,
                    questionText = questionText,
                    options = options,
                    correctIndex = correctIndex,
                    explanation = explanation,
                    difficulty = difficulty,
                    points = points ?: 10,
                    timerSeconds = timerSeconds ?: 30,
                    imageUrl = imageUrl,
                    requestedPublish = requestedPublish,
                    errors = errors,
                    isDuplicateInDatabase = dupDb,
                    isDuplicateInFile = dupFile
                )
            )
        }

        val validNonDup = parsedRows.filter { it.isValid && !it.isDuplicateInDatabase && !it.isDuplicateInFile }
        val validDup = parsedRows.filter { it.isValid && (it.isDuplicateInDatabase || it.isDuplicateInFile) }
        val invalid = parsedRows.filter { !it.isValid }

        return CsvImportPreview(
            rows = parsedRows,
            validNonDuplicateRows = validNonDup,
            validDuplicateRows = validDup,
            invalidRows = invalid
        )
    }

    private fun escapeCsvCell(value: String): String {
        val clean = value.replace("\r\n", "\n").replace("\r", "\n")
        return if (clean.contains(',') || clean.contains('"') || clean.contains('\n')) {
            "\"${clean.replace("\"", "\"\"")}\""
        } else {
            clean
        }
    }

    internal fun parseCsvRecords(input: String): List<List<String>> {
        if (input.isBlank()) return emptyList()
        val rows = mutableListOf<List<String>>()
        var currentRow = mutableListOf<String>()
        val currentCell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < input.length) {
            val ch = input[i]
            when {
                inQuotes -> {
                    if (ch == '"') {
                        if (i + 1 < input.length && input[i + 1] == '"') {
                            currentCell.append('"')
                            i++
                        } else {
                            inQuotes = false
                        }
                    } else {
                        currentCell.append(ch)
                    }
                }
                ch == '"' -> inQuotes = true
                ch == ',' -> {
                    currentRow.add(currentCell.toString())
                    currentCell.setLength(0)
                }
                ch == '\r' -> {
                    if (i + 1 < input.length && input[i + 1] == '\n') i++
                    currentRow.add(currentCell.toString())
                    currentCell.setLength(0)
                    rows.add(currentRow)
                    currentRow = mutableListOf()
                }
                ch == '\n' -> {
                    currentRow.add(currentCell.toString())
                    currentCell.setLength(0)
                    rows.add(currentRow)
                    currentRow = mutableListOf()
                }
                else -> currentCell.append(ch)
            }
            i++
        }
        if (currentCell.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentCell.toString())
            rows.add(currentRow)
        }
        return rows
    }
}
