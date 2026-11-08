package com.example.core.audio

/**
 * Complete catalog of UI, Quiz, Game, Leaderboard, and System sound effects
 * for the TL Brain Test application (Owner: PRITAM CHOUDHARY).
 *
 * All audio generated procedurally or bundled under Creative Commons Zero (CC0)
 * Public Domain. 100% Royalty-Free, Free-Tier, Zero Cloud/API cost.
 */
enum class SoundEffect(
    val id: String,
    val displayName: String,
    val category: String,
    val description: String
) {
    // General UI
    CLICK("click", "Button Click", "General UI", "Crisp subtle tactile button click"),
    SELECT("select", "Selection / Chip", "General UI", "Light selection tap"),
    MENU_OPEN("menu_open", "Menu Open", "General UI", "Soft ascending whoosh/chime"),
    MENU_CLOSE("menu_close", "Menu Close", "General UI", "Soft descending close tap"),
    TAB_CHANGE("tab_change", "Tab Switch", "General UI", "Smooth navigation notch"),
    PAGE_TRANSITION("page_transition", "Page Transition", "General UI", "Light air sweep"),
    TOGGLE_ON("toggle_on", "Toggle Switch ON", "General UI", "Pleasant rising confirmation blip"),
    TOGGLE_OFF("toggle_off", "Toggle Switch OFF", "General UI", "Soft descending disengage blip"),
    CHECKBOX("checkbox", "Checkbox", "General UI", "Crisp check snap"),
    NOTIFICATION("notification", "Notification", "General UI", "Crystal two-tone alert chime"),
    POPUP_OPEN("popup_open", "Dialog / Popup Open", "General UI", "Warm bubble pop open"),
    POPUP_CLOSE("popup_close", "Dialog / Popup Close", "General UI", "Subtle bubble pop close"),
    ERROR("error", "Error / Invalid Action", "General UI", "Low warning buzz indicating error"),
    WARNING("warning", "Warning Prompt", "General UI", "Muted attention alert tone"),
    SUCCESS("success", "Success / Done", "General UI", "Bright major chord confirmation chime"),
    SAVE("save", "Save Completed", "General UI", "Satisfying save chime"),
    DELETE("delete", "Delete / Remove", "General UI", "Low clear/trash confirmation tone"),
    LOGOUT("logout", "Logout Confirmation", "General UI", "Gentle farewell chime"),

    // Quiz Engine
    QUIZ_START("quiz_start", "Quiz Start", "Quiz", "Energizing ascending intro fanfare"),
    QUESTION_APPEAR("question_appear", "Question Appearing", "Quiz", "Clean glockenspiel ping"),
    OPTION_SELECT("option_select", "Option Selected", "Quiz", "Crisp answer choice tick"),
    CORRECT("correct", "Correct Answer", "Quiz", "Triumphant high bell chord"),
    WRONG("wrong", "Wrong Answer", "Quiz", "Gentle muted wrong answer buzzer"),
    TIMER_WARN("timer_warn", "Timer Warning (<7s)", "Quiz", "Urgent high countdown alert ping"),
    TIMER_END("timer_end", "Timer Expired", "Quiz", "Time's up double warning buzz"),
    NEXT_QUESTION("next_question", "Next Question", "Quiz", "Soft forward sweep"),
    QUIZ_COMPLETE("quiz_complete", "Quiz Completed", "Quiz", "Celebratory completion flourish"),
    SCORE_DISPLAY("score_display", "Score / Result Revealed", "Quiz", "Sparkling score counter chime"),
    HIGH_SCORE("high_score", "New High Score", "Quiz", "Grand victory fanfare"),
    XP_EARNED("xp_earned", "XP / Points Earned", "Quiz", "Bright coin reward ping"),
    LEVEL_UP("level_up", "Level Up", "Quiz", "Ascending triumphant royal arpeggio"),
    ACHIEVEMENT_UNLOCKED("achievement_unlocked", "Badge Unlocked", "Quiz", "Rich golden reward chord"),

    // Brain Games Arena
    GAME_START("game_start", "Game Start", "Brain Games", "Ready-set-go 3-beat chime"),
    GAME_CORRECT_MOVE("game_correct", "Correct Move", "Brain Games", "Pleasant melodic match note"),
    GAME_WRONG_MOVE("game_wrong", "Wrong Move / Miss", "Brain Games", "Gentle bounce buzz"),
    MATCH_FOUND("match_found", "Pair / Match Found", "Brain Games", "Harmonious double bell ping"),
    MATCH_COMPLETED("match_completed", "Match Completed", "Brain Games", "Bright stage victory chime"),
    COUNTDOWN_TICK("countdown_tick", "Countdown Tick", "Brain Games", "Clean percussive clock tick"),
    GAME_OVER("game_over", "Game Over", "Brain Games", "Soft descending resolution phrase"),
    NEW_RECORD("new_record", "New Game Record", "Brain Games", "High-energy victory flourish"),
    GAME_COMPLETED("game_completed", "Game Completed", "Brain Games", "Full game victory resolution chord"),
    POINTS_EARNED("points_earned", "Game Points Earned", "Brain Games", "Rapid ascending sparkle ping"),

    // Leaderboard & Social
    LEADERBOARD_OPEN("leaderboard_open", "Leaderboard Open", "Leaderboard", "Trophy shimmer sweep"),
    RANKING_CHANGED("ranking_changed", "Rank Up / Changed", "Leaderboard", "Ascending competitive chime"),

    // Profile & Customization
    AVATAR_SELECT("avatar_select", "Avatar Selected", "Profile", "Cheerful character pop"),
    SETTINGS_SAVED("settings_saved", "Settings Saved", "Profile", "Clean configuration chime"),

    // Account & Subscription
    SUBSCRIPTION_ACTIVATED("subscription_activated", "VIP Activated", "Account", "Golden royal chime"),
    REQUEST_SUBMITTED("request_submitted", "Request Submitted", "Account", "Clean dispatch tone"),

    // Support Chat
    CHAT_SENT("chat_sent", "Message Sent", "Support Chat", "Light message sent pop"),
    CHAT_RECEIVED("chat_received", "Message Received", "Support Chat", "Warm waterdrop incoming message chime")
}
