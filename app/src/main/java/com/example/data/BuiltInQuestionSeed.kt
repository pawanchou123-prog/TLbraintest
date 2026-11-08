package com.example.data

data class SeedQuestionSpec(
    val studentClass: Int,
    val subject: String,
    val language: String,
    val questionText: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val difficulty: String,
    val points: Int,
    val timerSeconds: Int
)

object BuiltInQuestionSeed {
    val starterPack: List<SeedQuestionSpec> = listOf(
        SeedQuestionSpec(
            studentClass = 0,
            subject = "General Knowledge",
            language = "en",
            questionText = "Which planet in our solar system is known as the Red Planet?",
            options = listOf("Venus", "Mars", "Jupiter", "Saturn"),
            correctIndex = 1,
            explanation = "Iron oxide (rust) on the surface of Mars gives it a reddish appearance.",
            difficulty = "easy",
            points = 10,
            timerSeconds = 25
        ),
        SeedQuestionSpec(
            studentClass = 0,
            subject = "General Knowledge",
            language = "hi",
            questionText = "भारत के संविधान का जनक किसे कहा जाता है?",
            options = listOf("महात्मा गांधी", "डॉ. बी. आर. अम्बेडकर", "सरदार पटेल", "जवाहरलाल नेहरू"),
            correctIndex = 1,
            explanation = "डॉ. भीमराव अम्बेडकर संविधान की प्रारूप समिति के अध्यक्ष थे।",
            difficulty = "easy",
            points = 10,
            timerSeconds = 25
        ),
        SeedQuestionSpec(
            studentClass = 10,
            subject = "Science",
            language = "en",
            questionText = "What is the SI unit of electric current?",
            options = listOf("Volt", "Ohm", "Ampere", "Watt"),
            correctIndex = 2,
            explanation = "Electric current is measured in Amperes (A), representing one coulomb per second.",
            difficulty = "easy",
            points = 10,
            timerSeconds = 25
        ),
        SeedQuestionSpec(
            studentClass = 10,
            subject = "Mathematics",
            language = "en",
            questionText = "In a right-angled triangle with legs 6 cm and 8 cm, what is the length of the hypotenuse?",
            options = listOf("9 cm", "10 cm", "12 cm", "14 cm"),
            correctIndex = 1,
            explanation = "By Pythagoras theorem: sqrt(6^2 + 8^2) = sqrt(36 + 64) = sqrt(100) = 10 cm.",
            difficulty = "medium",
            points = 15,
            timerSeconds = 35
        ),
        SeedQuestionSpec(
            studentClass = 10,
            subject = "Science",
            language = "hi",
            questionText = "मानव शरीर की सबसे बड़ी ग्रंथि कौन-सी है?",
            options = listOf("अग्न्याशय", "यकृत (लीवर)", "थायरॉइड", "पीयूष ग्रंथि"),
            correctIndex = 1,
            explanation = "यकृत (लीवर) मानव शरीर की सबसे बड़ी आंतरिक ग्रंथि है।",
            difficulty = "easy",
            points = 10,
            timerSeconds = 25
        ),
        SeedQuestionSpec(
            studentClass = 8,
            subject = "Mathematics",
            language = "en",
            questionText = "What is the cube root of 512?",
            options = listOf("6", "7", "8", "9"),
            correctIndex = 2,
            explanation = "8 multiplied by itself three times (8 x 8 x 8) equals 512.",
            difficulty = "medium",
            points = 15,
            timerSeconds = 30
        ),
        SeedQuestionSpec(
            studentClass = 6,
            subject = "Science",
            language = "en",
            questionText = "Which gas do green plants absorb from the atmosphere during photosynthesis?",
            options = listOf("Oxygen", "Nitrogen", "Carbon Dioxide", "Hydrogen"),
            correctIndex = 2,
            explanation = "Plants take in carbon dioxide and water using sunlight to synthesize glucose and release oxygen.",
            difficulty = "easy",
            points = 10,
            timerSeconds = 25
        ),
        SeedQuestionSpec(
            studentClass = 12,
            subject = "Physics",
            language = "en",
            questionText = "Which phenomenon proves the transverse nature of electromagnetic light waves?",
            options = listOf("Refraction", "Diffraction", "Polarization", "Interference"),
            correctIndex = 2,
            explanation = "Only transverse waves can undergo polarization; longitudinal waves cannot be polarized.",
            difficulty = "hard",
            points = 20,
            timerSeconds = 30
        )
    )
}
