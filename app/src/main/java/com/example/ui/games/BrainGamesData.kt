package com.example.ui.games

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.random.Random

data class SpeedPromptRound(
    val prompt: String,
    val helperSubtitle: String = "",
    val options: List<String>,
    val correctIndex: Int,
    val swatchIndex: Int = -1 // Used by Color Recognition (0=Purple, 1=Emerald, 2=Crimson, 3=White)
)

data class WordScrambleRound(
    val scrambled: String,
    val answer: String,
    val clueEn: String,
    val clueHi: String
)

object BrainGamesData {

    fun iconForGame(gameId: String): ImageVector = when (gameId) {
        "math_challenge" -> Icons.Default.Calculate
        "addition" -> Icons.Default.AddCircle
        "subtraction" -> Icons.Default.RemoveCircle
        "multiplication" -> Icons.Default.Close
        "division" -> Icons.Default.Percent
        "number_pattern" -> Icons.Default.Functions
        "memory_match" -> Icons.Default.AutoAwesomeMosaic
        "reaction_test" -> Icons.Default.Bolt
        "number_scramble" -> Icons.Default.Apps
        "word_scramble" -> Icons.Default.Spellcheck
        "riddles" -> Icons.Default.Lightbulb
        "table_test" -> Icons.Default.GridOn
        "color_recognition" -> Icons.Default.Palette
        "finger_speed" -> Icons.Default.TouchApp
        else -> Icons.Default.Calculate
    }

    private fun buildNumericChoices(answer: Int): Pair<List<String>, Int> {
        val set = mutableSetOf(answer)
        var attempts = 0
        while (set.size < 4 && attempts < 40) {
            attempts++
            val delta = listOf(-12, -8, -5, -3, -2, -1, 1, 2, 3, 5, 8, 10, 12).random()
            val candidate = (answer + delta).coerceAtLeast(0)
            if (candidate != answer) set.add(candidate)
        }
        while (set.size < 4) {
            set.add(answer + set.size + 1)
        }
        val shuffled = set.toList().shuffled()
        return shuffled.map { it.toString() } to shuffled.indexOf(answer)
    }

    fun generateSpeedRounds(gameId: String, studentClass: Int, lang: String): List<SpeedPromptRound> {
        val scale = studentClass.coerceIn(1, 12)
        return when (gameId) {
            "addition" -> List(10) {
                val a = Random.nextInt(4 * scale, 18 * scale + 10)
                val b = Random.nextInt(3 * scale, 15 * scale + 8)
                val ans = a + b
                val (opts, idx) = buildNumericChoices(ans)
                SpeedPromptRound("$a + $b = ?", if (lang == "hi") "सही योग चुनें" else "Select the exact sum", opts, idx)
            }
            "subtraction" -> List(10) {
                val b = Random.nextInt(3 * scale, 14 * scale + 6)
                val diff = Random.nextInt(2 * scale, 16 * scale + 8)
                val a = b + diff
                val (opts, idx) = buildNumericChoices(diff)
                SpeedPromptRound("$a - $b = ?", if (lang == "hi") "सही अंतर चुनें" else "Select the difference", opts, idx)
            }
            "multiplication" -> List(10) {
                val a = Random.nextInt(2, (scale + 6).coerceAtMost(18))
                val b = Random.nextInt(3, 13)
                val ans = a * b
                val (opts, idx) = buildNumericChoices(ans)
                SpeedPromptRound("$a × $b = ?", if (lang == "hi") "सही गुणनफल चुनें" else "Calculate the product", opts, idx)
            }
            "division" -> List(10) {
                val divisor = Random.nextInt(2, 13)
                val quotient = Random.nextInt(2, (scale + 8).coerceAtMost(16))
                val dividend = divisor * quotient
                val (opts, idx) = buildNumericChoices(quotient)
                SpeedPromptRound("$dividend ÷ $divisor = ?", if (lang == "hi") "सही भागफल चुनें" else "Find the quotient", opts, idx)
            }
            "table_test" -> List(12) {
                val table = Random.nextInt(2, 21)
                val mult = Random.nextInt(2, 11)
                val ans = table * mult
                val (opts, idx) = buildNumericChoices(ans)
                SpeedPromptRound("$table × $mult = ?", if (lang == "hi") "पहाड़ा $table" else "Table of $table", opts, idx)
            }
            "number_pattern" -> List(8) { round ->
                when (round % 4) {
                    0 -> {
                        val start = Random.nextInt(2, 15)
                        val step = Random.nextInt(3, 9)
                        val seq = List(4) { i -> start + i * step }
                        val ans = start + 4 * step
                        val (opts, idx) = buildNumericChoices(ans)
                        SpeedPromptRound("${seq.joinToString(", ")}, ?", "Arithmetic sequence", opts, idx)
                    }
                    1 -> {
                        val start = Random.nextInt(2, 6)
                        val seq = listOf(start, start * 2, start * 4, start * 8)
                        val ans = start * 16
                        val (opts, idx) = buildNumericChoices(ans)
                        SpeedPromptRound("${seq.joinToString(", ")}, ?", "Doubling pattern", opts, idx)
                    }
                    2 -> {
                        val base = Random.nextInt(1, 5)
                        val seq = List(4) { i -> (base + i) * (base + i) }
                        val ans = (base + 4) * (base + 4)
                        val (opts, idx) = buildNumericChoices(ans)
                        SpeedPromptRound("${seq.joinToString(", ")}, ?", "Perfect squares", opts, idx)
                    }
                    else -> {
                        val a = Random.nextInt(2, 7)
                        val b = Random.nextInt(3, 8)
                        val c = a + b
                        val d = b + c
                        val ans = c + d
                        val (opts, idx) = buildNumericChoices(ans)
                        SpeedPromptRound("$a, $b, $c, $d, ?", "Sum of previous two numbers", opts, idx)
                    }
                }
            }
            "riddles" -> {
                val bank = if (lang == "hi") {
                    listOf(
                        SpeedPromptRound("वह क्या है जो हमेशा आगे बढ़ता है लेकिन कभी पीछे नहीं आता?", "बुद्धि पहेली", listOf("समय (Time)", "परछाई", "हवा", "पानी"), 0),
                        SpeedPromptRound("किस चीज़ में शहर, पहाड़ और नदियाँ हैं, लेकिन घर, पेड़ या मछलियाँ नहीं हैं?", "बुद्धि पहेली", listOf("ग्लोब / मानचित्र (Map)", "पुस्तक", "दर्पण", "घड़ी"), 0),
                        SpeedPromptRound("जितना अधिक आप इसमें से निकालते हैं, यह उतना ही बड़ा होता जाता है। वह क्या है?", "बुद्धि पहेली", listOf("गड्ढा (Hole)", "गुब्बारा", "बादल", "समुद्र"), 0),
                        SpeedPromptRound("मेरे पास कुंजियाँ (Keys) हैं पर कोई ताला नहीं; स्पेस है पर कोई कमरा नहीं। मैं क्या हूँ?", "बुद्धि पहेली", listOf("कीबोर्ड (Keyboard)", "पियानो", "कैलकुलेटर", "डायरी"), 0),
                        SpeedPromptRound("कौन-सा महीना साल में 28 दिन रखता है?", "ध्यान से सोचें", listOf("सभी 12 महीने", "केवल फरवरी", "केवल जनवरी", "कोई नहीं"), 0),
                        SpeedPromptRound("वह क्या है जिसे तोड़े बिना इस्तेमाल नहीं किया जा सकता?", "बुद्धि पहेली", listOf("अंडा (Egg)", "कागज", "पेंसिल", "ताला"), 0)
                    )
                } else {
                    listOf(
                        SpeedPromptRound("What goes forward every second and never comes back?", "Lateral Thinking", listOf("Time", "Shadow", "Echo", "Tide"), 0),
                        SpeedPromptRound("I have cities but no houses, mountains but no trees, and water but no fish. What am I?", "Lateral Thinking", listOf("A Map", "A Telescope", "A Compass", "A Dictionary"), 0),
                        SpeedPromptRound("The more you take away from me, the bigger I become. What am I?", "Lateral Thinking", listOf("A Hole", "A Fraction", "A Number", "A Shadow"), 0),
                        SpeedPromptRound("I have keys but no locks, space but no room, and you can enter but not go outside. What am I?", "Lateral Thinking", listOf("A Keyboard", "A Safe", "A Notebook", "A Calculator"), 0),
                        SpeedPromptRound("How many months in a calendar year have at least 28 days?", "Read Carefully", listOf("All 12 months", "Only 1 month", "6 months", "7 months"), 0),
                        SpeedPromptRound("What belongs to you, yet other people use it far more often than you do?", "Lateral Thinking", listOf("Your Name", "Your Pen", "Your Textbooks", "Your Watch"), 0)
                    )
                }
                bank.shuffled().take(5).map { r ->
                    val correctText = r.options[r.correctIndex]
                    val mixed = r.options.shuffled()
                    r.copy(options = mixed, correctIndex = mixed.indexOf(correctText))
                }
            }
            "color_recognition" -> {
                val colorNames = if (lang == "hi") {
                    listOf("बैंगनी (Purple)", "हरा (Emerald)", "लाल (Red)", "सफ़ेद (White)")
                } else {
                    listOf("Purple", "Emerald", "Crimson", "White")
                }
                List(8) {
                    val trueColorIdx = Random.nextInt(0, 4)
                    val distractorWordIdx = ((trueColorIdx + Random.nextInt(1, 4)) % 4)
                    SpeedPromptRound(
                        prompt = colorNames[distractorWordIdx].uppercase(),
                        helperSubtitle = if (lang == "hi") "शब्द नहीं, बॉक्स के असली रंग को चुनें!" else "Tap the TRUE SWATCH COLOR, ignore the word text!",
                        options = colorNames,
                        correctIndex = trueColorIdx,
                        swatchIndex = trueColorIdx
                    )
                }
            }
            else -> { // math_challenge mixed
                List(10) { round ->
                    when (round % 4) {
                        0 -> {
                            val a = Random.nextInt(8, 25 + scale * 3)
                            val b = Random.nextInt(6, 20 + scale * 2)
                            val c = Random.nextInt(2, 12)
                            val ans = a + b - c
                            val (opts, idx) = buildNumericChoices(ans)
                            SpeedPromptRound("$a + $b - $c = ?", "Mixed Challenge", opts, idx)
                        }
                        1 -> {
                            val a = Random.nextInt(3, 12)
                            val b = Random.nextInt(3, 11)
                            val c = Random.nextInt(4, 15)
                            val ans = (a * b) + c
                            val (opts, idx) = buildNumericChoices(ans)
                            SpeedPromptRound("($a × $b) + $c = ?", "BODMAS Order", opts, idx)
                        }
                        2 -> {
                            val d = Random.nextInt(2, 10)
                            val q = Random.nextInt(3, 12)
                            val add = Random.nextInt(5, 16)
                            val ans = q + add
                            val (opts, idx) = buildNumericChoices(ans)
                            SpeedPromptRound("(${d * q} ÷ $d) + $add = ?", "BODMAS Order", opts, idx)
                        }
                        else -> {
                            val a = Random.nextInt(4, 13)
                            val b = Random.nextInt(2, 8)
                            val ans = (a * b) - a
                            val (opts, idx) = buildNumericChoices(ans)
                            SpeedPromptRound("($a × $b) - $a = ?", "Mixed Challenge", opts, idx)
                        }
                    }
                }
            }
        }
    }

    fun generateWordScrambleRounds(): List<WordScrambleRound> {
        val words = listOf(
            Triple("GRAVITY", "Force that pulls objects toward Earth", "पृथ्वी की ओर खींचने वाला बल"),
            Triple("NEURON", "Messenger cell inside the human brain", "मस्तिष्क की तंत्रिका कोशिका"),
            Triple("ALGEBRA", "Branch of math using symbols and letters", "गणित की बीजगणित शाखा"),
            Triple("OXYGEN", "Essential gas humans breathe to live", "प्राणवायु गैस"),
            Triple("PLANET", "Celestial body orbiting a star", "सूर्य की परिक्रमा करने वाला पिंड"),
            Triple("FRACTION", "Numerical quantity representing part of a whole", "भिन्न संख्या"),
            Triple("COMPASS", "Instrument used to find magnetic North", "दिशा सूचक यंत्र"),
            Triple("TRIANGLE", "Polygon with three edges and three vertices", "तीन भुजाओं वाली आकृति")
        )
        return words.shuffled().take(5).map { (word, en, hi) ->
            var scrambled = word.toList().shuffled().joinToString("")
            if (scrambled == word) {
                scrambled = word.reversed()
            }
            WordScrambleRound(scrambled = scrambled, answer = word, clueEn = en, clueHi = hi)
        }
    }
}
