package com.example.data

import com.example.data.model.AnnouncementItem
import com.example.data.model.LeaderboardEntry
import com.example.data.model.QuestionItem
import com.example.data.model.WinnerRecord

/**
 * SamplePreviewCatalog provides realistic SAMPLE PREVIEW items ONLY when live Firestore
 * collections are empty (for example before Application 2 /admin and Application 3 /question-manager
 * populate live questions, leaderboard seasons, or broadcast notifications).
 *
 * IMPORTANT: Every UI surface consuming these items MUST explicitly badge them as
 * "SAMPLE PREVIEW — NOT LIVE DATABASE DATA" so sample data is never mistaken for live Firestore data.
 */
object SamplePreviewCatalog {

    fun sampleQuestionsForCategory(
        categoryOrSubject: String,
        studentClass: Int,
        lang: String
    ): List<QuestionItem> {
        val isHi = lang == "hi"
        val key = categoryOrSubject.trim().lowercase()
        val raw = when {
            key.contains("general knowledge") -> listOf(
                sampleQ(
                    studentClass = 0,
                    subject = "General Knowledge",
                    lang = lang,
                    q = if (isHi) "भारत का राष्ट्रीय विज्ञान दिवस किस तिथि को मनाया जाता है?" else "National Science Day in India is celebrated on which date?",
                    opts = if (isHi) listOf("15 जनवरी", "28 फरवरी", "11 मई", "22 दिसंबर") else listOf("15 January", "28 February", "11 May", "22 December"),
                    ans = 1,
                    exp = if (isHi) "28 फरवरी 1928 को सी. वी. रमन ने रमन प्रभाव की खोज की थी।" else "Sir C. V. Raman discovered the Raman Effect on 28 February 1928."
                ),
                sampleQ(
                    studentClass = 0,
                    subject = "General Knowledge",
                    lang = lang,
                    q = if (isHi) "भारतीय अंतरिक्ष अनुसंधान संगठन (ISRO) का मुख्यालय कहाँ स्थित है?" else "Where is the headquarters of the Indian Space Research Organisation (ISRO) located?",
                    opts = if (isHi) listOf("हैदराबाद", "बेंगलुरु", "तिरुवनंतपुरम", "चेन्नई") else listOf("Hyderabad", "Bengaluru", "Thiruvananthapuram", "Chennai"),
                    ans = 1,
                    exp = if (isHi) "इसरो का मुख्यालय अंतरिक्ष भवन, बेंगलुरु में स्थित है।" else "ISRO is headquartered at Antariksh Bhavan in Bengaluru."
                ),
                sampleQ(
                    studentClass = 0,
                    subject = "General Knowledge",
                    lang = lang,
                    q = if (isHi) "पृथ्वी के वायुमंडल में सबसे अधिक मात्रा में कौन-सी गैस पाई जाती है?" else "Which gas is most abundant in Earth's atmosphere?",
                    opts = if (isHi) listOf("ऑक्सीजन", "नाइट्रोजन", "कार्बन डाइऑक्साइड", "आर्गन") else listOf("Oxygen", "Nitrogen", "Carbon Dioxide", "Argon"),
                    ans = 1,
                    exp = if (isHi) "नाइट्रोजन वायुमंडल का लगभग 78 प्रतिशत भाग बनाती है।" else "Nitrogen makes up approximately 78 percent of Earth's atmosphere."
                )
            )
            key.contains("math") -> listOf(
                sampleQ(
                    studentClass = studentClass,
                    subject = "Mathematics",
                    lang = lang,
                    q = if (isHi) "यदि 3x + 7 = 28 है, तो x का मान क्या होगा?" else "If 3x + 7 = 28, what is the value of x?",
                    opts = listOf("5", "6", "7", "9"),
                    ans = 2,
                    exp = if (isHi) "3x = 28 - 7 = 21, अतः x = 21 / 3 = 7।" else "Subtract 7 from both sides: 3x = 21, so x = 7."
                ),
                sampleQ(
                    studentClass = studentClass,
                    subject = "Mathematics",
                    lang = lang,
                    q = if (isHi) "6 सेमी और 8 सेमी भुजाओं वाले समकोण त्रिभुज का कर्ण क्या है?" else "What is the hypotenuse of a right triangle with sides 6 cm and 8 cm?",
                    opts = listOf("9 cm", "10 cm", "12 cm", "14 cm"),
                    ans = 1,
                    exp = if (isHi) "पाइथागोरस प्रमेय से: sqrt(36 + 64) = sqrt(100) = 10 सेमी।" else "Using Pythagoras theorem: sqrt(6^2 + 8^2) = sqrt(100) = 10 cm."
                ),
                sampleQ(
                    studentClass = studentClass,
                    subject = "Mathematics",
                    lang = lang,
                    q = if (isHi) "250 का 24% कितना होता है?" else "What is 24% of 250?",
                    opts = listOf("48", "54", "60", "65"),
                    ans = 2,
                    exp = if (isHi) "(24 / 100) x 250 = 60।" else "(24 / 100) x 250 = 60."
                )
            )
            key.contains("science") || key.contains("physics") || key.contains("chemistry") || key.contains("biology") || key.contains("environmental") -> listOf(
                sampleQ(
                    studentClass = studentClass,
                    subject = categoryOrSubject,
                    lang = lang,
                    q = if (isHi) "विद्युत धारा का SI मात्रक क्या है?" else "What is the SI unit of electric current?",
                    opts = if (isHi) listOf("वोल्ट", "ओम", "एम्पीयर", "वाट") else listOf("Volt", "Ohm", "Ampere", "Watt"),
                    ans = 2,
                    exp = if (isHi) "विद्युत धारा को एम्पीयर (A) में मापा जाता है।" else "Electric current is measured in Amperes (A)."
                ),
                sampleQ(
                    studentClass = studentClass,
                    subject = categoryOrSubject,
                    lang = lang,
                    q = if (isHi) "कोशिका का पावरहाउस किसे कहा जाता है?" else "Which organelle is known as the powerhouse of the cell?",
                    opts = if (isHi) listOf("राइबोसोम", "माइटोकॉन्ड्रिया", "केंद्रक", "गॉल्जीकाय") else listOf("Ribosome", "Mitochondria", "Nucleus", "Golgi Apparatus"),
                    ans = 1,
                    exp = if (isHi) "माइटोकॉन्ड्रिया ATP के रूप में ऊर्जा उत्पन्न करता है।" else "Mitochondria generate cellular energy in the form of ATP."
                ),
                sampleQ(
                    studentClass = studentClass,
                    subject = categoryOrSubject,
                    lang = lang,
                    q = if (isHi) "शुद्ध जल का pH मान सामान्य ताप पर कितना होता है?" else "What is the pH of pure water at 25 degrees Celsius?",
                    opts = listOf("5", "7", "9", "14"),
                    ans = 1,
                    exp = if (isHi) "शुद्ध जल उदासीन होता है, इसलिए इसका pH मान 7 होता है।" else "Pure water is neutral with a pH value of 7."
                )
            )
            key.contains("reasoning") -> listOf(
                sampleQ(
                    studentClass = studentClass,
                    subject = "Reasoning",
                    lang = lang,
                    q = if (isHi) "श्रृंखला को पूरा करें: 3, 7, 15, 31, ?" else "Complete the logical series: 3, 7, 15, 31, ?",
                    opts = listOf("47", "59", "63", "65"),
                    ans = 2,
                    exp = if (isHi) "प्रत्येक संख्या = पिछली संख्या x 2 + 1; अतः 31 x 2 + 1 = 63।" else "Each term doubles the previous and adds 1: 31 x 2 + 1 = 63."
                ),
                sampleQ(
                    studentClass = studentClass,
                    subject = "Reasoning",
                    lang = lang,
                    q = if (isHi) "यदि BRAIN को CSBJO लिखा जाता है, तो LOGIC को कैसे लिखा जाएगा?" else "If BRAIN is coded as CSBJO, how is LOGIC coded?",
                    opts = listOf("MPHJF", "MPHJD", "KNFHB", "MQHJD"),
                    ans = 1,
                    exp = if (isHi) "प्रत्येक अक्षर को वर्णमाला में +1 आगे बढ़ाया गया है: L->M, O->P, G->H, I->J, C->D।" else "Every letter shifts forward by +1 in the alphabet: MPHJD."
                ),
                sampleQ(
                    studentClass = studentClass,
                    subject = "Reasoning",
                    lang = lang,
                    q = if (isHi) "विषम विकल्प चुनें: वर्ग, त्रिभुज, आयत, गोला" else "Find the odd one out: Square, Triangle, Rectangle, Sphere",
                    opts = if (isHi) listOf("वर्ग", "त्रिभुज", "आयत", "गोला") else listOf("Square", "Triangle", "Rectangle", "Sphere"),
                    ans = 3,
                    exp = if (isHi) "गोला 3D आकृति है जबकि अन्य सभी 2D समतल आकृतियाँ हैं।" else "Sphere is a 3D solid whereas the others are flat 2D polygons."
                )
            )
            key.contains("english") -> listOf(
                sampleQ(
                    studentClass = studentClass,
                    subject = "English",
                    lang = lang,
                    q = "Choose the correct synonym of the word 'DILIGENT':",
                    opts = listOf("Careless", "Hardworking", "Temporary", "Silent"),
                    ans = 1,
                    exp = "'Diligent' means showing steady, earnest, and hardworking effort."
                ),
                sampleQ(
                    studentClass = studentClass,
                    subject = "English",
                    lang = lang,
                    q = "Identify the grammatically correct sentence:",
                    opts = listOf(
                        "Neither of the answers are correct.",
                        "Neither of the answers is correct.",
                        "Neither of the answer were correct.",
                        "Neither answers is correct."
                    ),
                    ans = 1,
                    exp = "'Neither' as a singular pronoun takes the singular verb 'is'."
                )
            )
            key.contains("hindi") -> listOf(
                sampleQ(
                    studentClass = studentClass,
                    subject = "Hindi",
                    lang = lang,
                    q = "'विद्यार्थी' शब्द का सही संधि-विच्छेद क्या है?",
                    opts = listOf("विद्या + अर्थी", "विद्य + आर्थी", "विद + यार्थी", "विद्या + रथी"),
                    ans = 0,
                    exp = "दीर्घ स्वर संधि के नियम से 'विद्या + अर्थी = विद्यार्थी' बनता है।"
                ),
                sampleQ(
                    studentClass = studentClass,
                    subject = "Hindi",
                    lang = lang,
                    q = "'कमल' का पर्यायवाची शब्द इनमें से कौन-सा है?",
                    opts = listOf("पंकज", "अंबर", "अनिल", "वारिद"),
                    ans = 0,
                    exp = "'पंकज', 'जलज', और 'नीरज' कमल के प्रमुख पर्यायवाची शब्द हैं।"
                )
            )
            key.contains("social") || key.contains("history") || key.contains("geography") -> listOf(
                sampleQ(
                    studentClass = studentClass,
                    subject = categoryOrSubject,
                    lang = lang,
                    q = if (isHi) "भारतीय संविधान कब लागू हुआ था?" else "On which date did the Constitution of India come into effect?",
                    opts = if (isHi) listOf("15 अगस्त 1947", "26 नवंबर 1949", "26 जनवरी 1950", "2 अक्टूबर 1950") else listOf("15 August 1947", "26 November 1949", "26 January 1950", "2 October 1950"),
                    ans = 2,
                    exp = if (isHi) "26 जनवरी 1950 को संविधान पूर्ण रूप से लागू हुआ।" else "The Constitution of India came into effect on 26 January 1950."
                ),
                sampleQ(
                    studentClass = studentClass,
                    subject = categoryOrSubject,
                    lang = lang,
                    q = if (isHi) "कर्क रेखा भारत के कितने राज्यों से होकर गुजरती है?" else "The Tropic of Cancer passes through how many Indian states?",
                    opts = listOf("6", "7", "8", "9"),
                    ans = 2,
                    exp = if (isHi) "कर्क रेखा भारत के 8 राज्यों से होकर गुजरती है।" else "The Tropic of Cancer passes through 8 states in India."
                )
            )
            key.contains("computer") -> listOf(
                sampleQ(
                    studentClass = studentClass,
                    subject = "Computer",
                    lang = lang,
                    q = if (isHi) "कंप्यूटर में RAM का पूर्ण रूप क्या है?" else "What does RAM stand for in computing?",
                    opts = listOf("Read Access Memory", "Random Access Memory", "Rapid Action Module", "Remote Array Memory"),
                    ans = 1,
                    exp = if (isHi) "RAM का अर्थ Random Access Memory है, जो अस्थायी प्राथमिक मेमोरी है।" else "RAM stands for Random Access Memory, used for fast volatile storage."
                ),
                sampleQ(
                    studentClass = studentClass,
                    subject = "Computer",
                    lang = lang,
                    q = if (isHi) "1 किलोबाइट (KB) बाइनरी मानक में कितने बाइट्स के बराबर होता है?" else "In binary computing standards, 1 Kilobyte (KiB) equals how many bytes?",
                    opts = listOf("512 Bytes", "1000 Bytes", "1024 Bytes", "2048 Bytes"),
                    ans = 2,
                    exp = if (isHi) "2^10 = 1024 बाइट्स।" else "2^10 equals 1024 bytes."
                )
            )
            key.contains("iq") -> listOf(
                sampleQ(
                    studentClass = studentClass,
                    subject = "IQ Test",
                    lang = lang,
                    q = if (isHi) "यदि 5 मशीनें 5 मिनट में 5 खिलौने बनाती हैं, तो 100 मशीनों को 100 खिलौने बनाने में कितना समय लगेगा?" else "If 5 machines make 5 widgets in 5 minutes, how long do 100 machines take to make 100 widgets?",
                    opts = if (isHi) listOf("1 मिनट", "5 मिनट", "20 मिनट", "100 मिनट") else listOf("1 minute", "5 minutes", "20 minutes", "100 minutes"),
                    ans = 1,
                    exp = if (isHi) "प्रत्येक मशीन 5 मिनट में 1 खिलौना बनाती है, इसलिए 100 मशीनें समानांतर काम करके 5 मिनट में 100 खिलौने बनाएँगी।" else "Each machine takes 5 minutes per widget, so 100 machines working simultaneously take 5 minutes."
                ),
                sampleQ(
                    studentClass = studentClass,
                    subject = "IQ Test",
                    lang = lang,
                    q = if (isHi) "एक तालाब में कमल के फूल प्रतिदिन दोगुने हो जाते हैं। यदि तालाब 30 दिन में पूरा भरता है, तो वह आधा किस दिन भरा था?" else "Water lilies in a pond double every day. If the pond is completely full on Day 30, on which day was it half full?",
                    opts = if (isHi) listOf("दिन 15", "दिन 20", "दिन 28", "दिन 29") else listOf("Day 15", "Day 20", "Day 28", "Day 29"),
                    ans = 3,
                    exp = if (isHi) "क्योंकि फूल हर दिन दोगुने होते हैं, 29वें दिन तालाब आधा भरा था और 30वें दिन पूरा भर गया।" else "Since coverage doubles each day, half-full occurs one day before Day 30 — on Day 29."
                )
            )
            else -> listOf(
                // Memory Test & fallback
                sampleQ(
                    studentClass = studentClass,
                    subject = "Memory Test",
                    lang = lang,
                    q = if (isHi) "इस अनुक्रम को ध्यान से पढ़ें: [ 49 - DELTA - 82 - SIGMA ]। तीसरा तत्व कौन-सा था?" else "Read carefully: [ 49 - DELTA - 82 - SIGMA ]. Which item was in the 3rd position?",
                    opts = listOf("49", "DELTA", "82", "SIGMA"),
                    ans = 2,
                    exp = if (isHi) "तीसरे स्थान पर '82' था।" else "'82' occupied the 3rd position in the sequence."
                ),
                sampleQ(
                    studentClass = studentClass,
                    subject = "Memory Test",
                    lang = lang,
                    q = if (isHi) "अनुक्रम [ PURPLE, WHITE, BLACK, VIOLET ] को उल्टे क्रम में रखने पर पहला शब्द क्या होगा?" else "If you reverse [ PURPLE, WHITE, BLACK, VIOLET ], which word comes first?",
                    opts = listOf("PURPLE", "WHITE", "BLACK", "VIOLET"),
                    ans = 3,
                    exp = if (isHi) "अंतिम शब्द VIOLET उल्टे क्रम में सबसे पहले आएगा।" else "Reversing places the last item, VIOLET, at position 1."
                )
            )
        }
        return raw
    }

    private fun sampleQ(
        studentClass: Int,
        subject: String,
        lang: String,
        q: String,
        opts: List<String>,
        ans: Int,
        exp: String
    ): QuestionItem {
        return QuestionItem(
            id = "", // Blank id ensures sample questions never attempt to mutate live Firestore question stats
            authorId = "sample_preview",
            studentClass = studentClass.toLong(),
            subject = subject,
            language = lang,
            questionText = q,
            options = opts,
            correctIndex = ans.toLong(),
            explanation = exp,
            difficulty = "medium",
            points = 15L,
            timerSeconds = 30L,
            imageUrl = "",
            isPublished = false,
            source = "manual"
        )
    }

    val sampleLeaderboard: List<LeaderboardEntry> = listOf(
        LeaderboardEntry(
            id = "sample_1",
            userId = "sample_1",
            displayName = "Aarav Sharma (Sample)",
            studentClass = 10L,
            dailyXp = 140L,
            dailyKey = CurriculumCatalog.currentDayKey(),
            weeklyXp = 520L,
            weeklyKey = CurriculumCatalog.currentWeekKey(),
            monthlyXp = 1480L,
            monthlyKey = CurriculumCatalog.currentMonthKey(),
            allTimeXp = 2940L,
            level = 15L,
            accuracyPercent = 94L
        ),
        LeaderboardEntry(
            id = "sample_2",
            userId = "sample_2",
            displayName = "Ananya Verma (Sample)",
            studentClass = 12L,
            dailyXp = 115L,
            dailyKey = CurriculumCatalog.currentDayKey(),
            weeklyXp = 460L,
            weeklyKey = CurriculumCatalog.currentWeekKey(),
            monthlyXp = 1310L,
            monthlyKey = CurriculumCatalog.currentMonthKey(),
            allTimeXp = 2510L,
            level = 13L,
            accuracyPercent = 91L
        ),
        LeaderboardEntry(
            id = "sample_3",
            userId = "sample_3",
            displayName = "Rohan Patel (Sample)",
            studentClass = 8L,
            dailyXp = 90L,
            dailyKey = CurriculumCatalog.currentDayKey(),
            weeklyXp = 390L,
            weeklyKey = CurriculumCatalog.currentWeekKey(),
            monthlyXp = 1120L,
            monthlyKey = CurriculumCatalog.currentMonthKey(),
            allTimeXp = 1980L,
            level = 10L,
            accuracyPercent = 88L
        )
    )

    val samplePreviousWinners: List<WinnerRecord> = listOf(
        WinnerRecord(
            id = "sample_win_1",
            monthLabel = "Previous Season (Sample)",
            winnerName = "Diya Rathore (Sample)",
            studentClass = 11L,
            rank = 1L,
            xpScore = 3420L,
            prizeWon = "Top Scholar Merit Kit"
        ),
        WinnerRecord(
            id = "sample_win_2",
            monthLabel = "Previous Season (Sample)",
            winnerName = "Kabir Singh (Sample)",
            studentClass = 9L,
            rank = 2L,
            xpScore = 3150L,
            prizeWon = "Science & Math Study Set"
        )
    )

    val sampleNotifications: List<AnnouncementItem> = listOf(
        AnnouncementItem(
            id = "sample_notice_1",
            title = "Welcome to TL Brain Test Student App (Sample Preview)",
            message = "Practice Class 1 to 12 quizzes, General Knowledge, IQ Tests, and 13+ Brain Games. Live notices from Pritam Choudhary will appear here automatically once published.",
            category = "notification",
            targetClass = 0L
        ),
        AnnouncementItem(
            id = "sample_notice_2",
            title = "Student Subscription Policy: Rs. 50 / 3 Months (Sample Preview)",
            message = "Subscriptions are activated manually via UPI or Cash by Administrator Pritam Choudhary.",
            category = "announcement",
            targetClass = 0L
        )
    )
}
