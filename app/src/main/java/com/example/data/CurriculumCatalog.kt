package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class GameCatalogItem(
    val id: String,
    val titleEn: String,
    val titleHi: String,
    val subtitleEn: String,
    val subtitleHi: String,
    val category: String
)

data class BadgeDefinition(
    val id: String,
    val titleEn: String,
    val titleHi: String,
    val descriptionEn: String,
    val descriptionHi: String
)

data class AvatarDefinition(
    val id: String,
    val labelEn: String,
    val labelHi: String
)

object CurriculumCatalog {

    // All 10 core Quiz Categories explicitly featured in the Student Quiz Section
    val coreQuizCategories: List<String> = listOf(
        "General Knowledge",
        "Mathematics",
        "Science",
        "Reasoning",
        "English",
        "Hindi",
        "Social Science",
        "Computer",
        "IQ Test",
        "Memory Test"
    )

    val avatars: List<AvatarDefinition> = listOf(
        AvatarDefinition("cyber_brain", "Neural Core", "न्यूरल कोर"),
        AvatarDefinition("quantum_star", "Quantum Star", "क्वांटम स्टार"),
        AvatarDefinition("scholar_shield", "Scholar Shield", "स्कॉलर शील्ड"),
        AvatarDefinition("logic_bolt", "Logic Bolt", "लॉजिक बोल्ट"),
        AvatarDefinition("rocket_mind", "Apex Rocket", "एपेक्स रॉकेट"),
        AvatarDefinition("diamond_crest", "Diamond Crest", "डायमंड क्रेस्ट")
    )

    fun subjectsForClass(studentClass: Int): List<String> {
        return when (studentClass) {
            in 1..5 -> listOf(
                "Mathematics",
                "English",
                "Hindi",
                "EVS",
                "Environmental Studies",
                "Computer",
                "Reasoning",
                "General Knowledge"
            )
            in 6..8 -> listOf(
                "Mathematics",
                "Science",
                "English",
                "Hindi",
                "Social Science",
                "Computer",
                "Reasoning",
                "IQ Test",
                "General Knowledge"
            )
            in 9..10 -> listOf(
                "Mathematics",
                "Science",
                "Social Science",
                "English",
                "Hindi",
                "Computer",
                "Reasoning",
                "IQ Test",
                "General Knowledge"
            )
            else -> listOf(
                "Physics",
                "Chemistry",
                "Biology",
                "Mathematics",
                "Accountancy",
                "Business Studies",
                "Economics",
                "History",
                "Geography",
                "Political Science",
                "English",
                "Hindi",
                "Computer",
                "Reasoning",
                "IQ Test",
                "General Knowledge"
            )
        }
    }

    fun questionManagerSubjectsForClass(
        studentClass: Int,
        catalog: com.example.data.model.SubjectCatalogDoc? = null
    ): List<String> {
        val effective = catalog ?: com.example.data.model.SubjectCatalogDoc()
        return when (studentClass) {
            0 -> listOf("General Knowledge", "Reasoning", "IQ Test", "Memory Test", "Computer")
            in 1..5 -> effective.classes1To5.ifEmpty {
                listOf("Mathematics", "English", "Hindi", "EVS", "General Knowledge")
            }
            in 6..8 -> effective.classes6To8.ifEmpty {
                listOf("Mathematics", "Science", "English", "Hindi", "Social Science", "General Knowledge")
            }
            in 9..10 -> effective.classes9To10.ifEmpty {
                listOf("Mathematics", "Science", "Social Science", "English", "Hindi")
            }
            else -> effective.classes11To12.ifEmpty {
                listOf(
                    "Physics", "Chemistry", "Biology", "Mathematics",
                    "Accountancy", "Business Studies", "Economics",
                    "History", "Geography", "Political Science", "English"
                )
            }
        }
    }

    fun subjectLabel(subject: String, lang: String): String {
        if (lang != "hi") return subject
        return when (subject) {
            "General Knowledge" -> "सामान्य ज्ञान (GK)"
            "Mathematics" -> "गणित (Mathematics)"
            "Science" -> "विज्ञान (Science)"
            "Reasoning" -> "तर्कशक्ति (Reasoning)"
            "English" -> "अंग्रेजी (English)"
            "Hindi" -> "हिन्दी (Hindi)"
            "Social Science" -> "सामाजिक विज्ञान"
            "Computer", "Computer Basics", "Computer Science" -> "कंप्यूटर (Computer)"
            "IQ Test" -> "बुद्धिलब्धि परीक्षण (IQ Test)"
            "Memory Test" -> "स्मृति परीक्षण (Memory Test)"
            "EVS", "Environmental Studies" -> "पर्यावरण अध्ययन (EVS)"
            "Physics" -> "भौतिक विज्ञान (Physics)"
            "Chemistry" -> "रसायन विज्ञान (Chemistry)"
            "Biology" -> "जीव विज्ञान (Biology)"
            "Accountancy" -> "लेखाशास्त्र (Accountancy)"
            "Business Studies" -> "व्यवसाय अध्ययन (Business Studies)"
            "Economics" -> "अर्थशास्त्र (Economics)"
            "History", "History & Civics" -> "इतिहास (History)"
            "Geography", "Geography & Economics" -> "भूगोल (Geography)"
            "Political Science" -> "राजनीति विज्ञान (Political Science)"
            else -> subject
        }
    }

    val games: List<GameCatalogItem> = listOf(
        GameCatalogItem(
            id = "addition",
            titleEn = "Addition Challenge",
            titleHi = "जोड़ चुनौती",
            subtitleEn = "Rapid mental sum calculation",
            subtitleHi = "त्वरित मानसिक जोड़ गणना",
            category = "Arithmetic"
        ),
        GameCatalogItem(
            id = "subtraction",
            titleEn = "Subtraction Challenge",
            titleHi = "घटाव चुनौती",
            subtitleEn = "Fast difference solving drills",
            subtitleHi = "तेज घटाव अभ्यास",
            category = "Arithmetic"
        ),
        GameCatalogItem(
            id = "multiplication",
            titleEn = "Multiplication Challenge",
            titleHi = "गुणा चुनौती",
            subtitleEn = "Product mastery under pressure",
            subtitleHi = "समय सीमा में गुणा अभ्यास",
            category = "Arithmetic"
        ),
        GameCatalogItem(
            id = "division",
            titleEn = "Division Challenge",
            titleHi = "भाग चुनौती",
            subtitleEn = "Clean quotient mental math",
            subtitleHi = "सटीक भागफल गणना",
            category = "Arithmetic"
        ),
        GameCatalogItem(
            id = "number_pattern",
            titleEn = "Number Pattern",
            titleHi = "संख्या पैटर्न",
            subtitleEn = "Predict the missing sequence term",
            subtitleHi = "अगली संख्या पहचानें",
            category = "Logic"
        ),
        GameCatalogItem(
            id = "memory_match",
            titleEn = "Memory Match",
            titleHi = "स्मृति मिलान",
            subtitleEn = "Flip and pair symbol matrices",
            subtitleHi = "समान कार्ड जोड़ियां खोजें",
            category = "Memory"
        ),
        GameCatalogItem(
            id = "reaction_test",
            titleEn = "Reaction Test",
            titleHi = "प्रतिक्रिया परीक्षण",
            subtitleEn = "Millisecond reflex signal tap",
            subtitleHi = "मिलीसेकंड रिफ्लेक्स टेस्ट",
            category = "Reflex"
        ),
        GameCatalogItem(
            id = "number_scramble",
            titleEn = "Number Scramble",
            titleHi = "संख्या क्रम",
            subtitleEn = "Tap scrambled numbers in ascending order",
            subtitleHi = "संख्याओं को बढ़ते क्रम में चुनें",
            category = "Focus"
        ),
        GameCatalogItem(
            id = "word_scramble",
            titleEn = "Word Scramble",
            titleHi = "शब्द पहेली",
            subtitleEn = "Unscramble academic vocabulary words",
            subtitleHi = "अक्षरों को जोड़कर सही शब्द बनाएं",
            category = "Verbal"
        ),
        GameCatalogItem(
            id = "riddles",
            titleEn = "Riddle Challenge",
            titleHi = "बुद्धि पहेली चुनौती",
            subtitleEn = "Lateral thinking brain teasers",
            subtitleHi = "तर्क और बुद्धि पहेलियाँ",
            category = "Logic"
        ),
        GameCatalogItem(
            id = "table_test",
            titleEn = "Table Test",
            titleHi = "पहाड़ा परीक्षण",
            subtitleEn = "Tables 2 to 20 high-speed sprint",
            subtitleHi = "2 से 20 तक पहाड़ा स्पीड टेस्ट",
            category = "Arithmetic"
        ),
        GameCatalogItem(
            id = "color_recognition",
            titleEn = "Color Recognition",
            titleHi = "रंग पहचान",
            subtitleEn = "Match true swatch color against ink traps",
            subtitleHi = "सही रंग की त्वरित पहचान करें",
            category = "Focus"
        ),
        GameCatalogItem(
            id = "finger_speed",
            titleEn = "Finger Speed Test",
            titleHi = "फिंगर स्पीड टेस्ट",
            subtitleEn = "10-second rapid neon pulse tapper",
            subtitleHi = "10 सेकंड तेज़ टैपिंग चुनौती",
            category = "Reflex"
        ),
        GameCatalogItem(
            id = "math_challenge",
            titleEn = "Mixed Math Challenge",
            titleHi = "मिश्रित गणित चुनौती",
            subtitleEn = "Mixed multi-operation speed equations",
            subtitleHi = "तेज मिश्रित गणित समीकरण",
            category = "Arithmetic"
        )
    )

    val badges: List<BadgeDefinition> = listOf(
        BadgeDefinition(
            id = "first_step",
            titleEn = "First Spark",
            titleHi = "पहला कदम",
            descriptionEn = "Complete your first quiz or brain game",
            descriptionHi = "अपना पहला क्विज़ या गेम पूरा करें"
        ),
        BadgeDefinition(
            id = "quiz_scholar",
            titleEn = "Quiz Scholar",
            titleHi = "क्विज़ विद्वान",
            descriptionEn = "Complete 5 subject or GK quizzes",
            descriptionHi = "5 विषय या सामान्य ज्ञान क्विज़ पूरे करें"
        ),
        BadgeDefinition(
            id = "arcade_champion",
            titleEn = "Arcade Sprinter",
            titleHi = "गेम चैंपियन",
            descriptionEn = "Complete 10 educational 2D brain games",
            descriptionHi = "10 शैक्षिक ब्रेन गेम्स पूरे करें"
        ),
        BadgeDefinition(
            id = "sharp_accuracy",
            titleEn = "Precision Mind",
            titleHi = "सटीक मस्तिष्क",
            descriptionEn = "Maintain 85% or higher quiz accuracy",
            descriptionHi = "85% या अधिक सटीकता बनाए रखें"
        ),
        BadgeDefinition(
            id = "xp_500",
            titleEn = "Rising Prodigy",
            titleHi = "उभरती प्रतिभा",
            descriptionEn = "Reach 500 total XP",
            descriptionHi = "कुल 500 XP प्राप्त करें"
        ),
        BadgeDefinition(
            id = "level_5",
            titleEn = "Cyber Master L5",
            titleHi = "साइबर मास्टर लेवल 5",
            descriptionEn = "Advance to Player Level 5",
            descriptionHi = "लेवल 5 तक पहुँचें"
        )
    )

    fun computeUnlockedBadges(
        xp: Long,
        level: Long,
        quizzesPlayed: Long,
        gamesPlayed: Long,
        totalAnswered: Long,
        totalCorrect: Long,
        existing: List<String>
    ): List<String> {
        val result = existing.toMutableSet()
        if (quizzesPlayed + gamesPlayed >= 1L) result.add("first_step")
        if (quizzesPlayed >= 5L) result.add("quiz_scholar")
        if (gamesPlayed >= 10L) result.add("arcade_champion")
        val acc = if (totalAnswered >= 5L) (totalCorrect * 100L) / totalAnswered else 0L
        if (acc >= 85L) result.add("sharp_accuracy")
        if (xp >= 500L) result.add("xp_500")
        if (level >= 5L) result.add("level_5")
        return result.take(30)
    }

    fun currentDayKey(nowMs: Long = System.currentTimeMillis()): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMs))
    }

    fun currentWeekKey(nowMs: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance(Locale.US).apply { timeInMillis = nowMs }
        val year = cal.get(Calendar.YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        return String.format(Locale.US, "%04d-W%02d", year, week)
    }

    fun currentMonthKey(nowMs: Long = System.currentTimeMillis()): String {
        return SimpleDateFormat("yyyy-MM", Locale.US).format(Date(nowMs))
    }

    fun formatReadableDate(epochMs: Long): String {
        if (epochMs <= 0L) return "Not Set"
        return SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(epochMs))
    }
}
