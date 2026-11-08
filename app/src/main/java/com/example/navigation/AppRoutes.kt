package com.example.navigation

// Strict route namespace isolation across the three separate TL Brain Test applications:
// 1. Student User Application: /app (Completed and preserved)
// 2. Main Admin Application: /admin (Built and active in Stage 2)
// 3. Question Management Application: /question-manager (Reserved isolated namespace for Stage 3)
object AppNamespaces {
    const val STUDENT_ROOT = "/app"
    const val ADMIN_ROOT = "/admin"
    const val QUESTION_MANAGER_RESERVED_ROOT = "/question-manager"
}

object StudentRoutes {
    const val SPLASH = "app/splash"
    const val TERMS_FIRST_RUN = "app/terms"
    const val LOGIN = "app/login"
    const val ACCESS_DENIED = "app/access-denied"

    // Authenticated and Approved Student Routes inside the /app namespace
    const val DASHBOARD = "app/dashboard"
    const val QUIZ_HUB = "app/quiz"
    const val QUIZ_PLAY = "app/quiz-play"
    const val GAMES_HUB = "app/games"
    const val GAME_PLAY = "app/game-play/{gameId}"
    const val LEADERBOARD = "app/leaderboard"
    const val PROFILE = "app/profile"
    const val SUBSCRIPTION = "app/subscription"
    const val SUPPORT = "app/support"
    const val NOTIFICATIONS = "app/notifications"
    const val TERMS_VIEW = "app/terms-view"

    fun gamePlayRoute(gameId: String): String = "app/game-play/$gameId"

    fun isPrimaryTabRoute(route: String?): Boolean {
        return route in setOf(DASHBOARD, QUIZ_HUB, GAMES_HUB, LEADERBOARD, PROFILE)
    }

    fun isAllowedStudentNamespace(route: String): Boolean {
        val canonical = "/" + route.trim().trimStart('/')
        return canonical.startsWith("${AppNamespaces.STUDENT_ROOT}/") &&
            !canonical.startsWith(AppNamespaces.ADMIN_ROOT) &&
            !canonical.startsWith(AppNamespaces.QUESTION_MANAGER_RESERVED_ROOT)
    }
}

object AdminRoutes {
    const val LOGIN = "admin/login"
    const val ACCESS_DENIED = "admin/access-denied"

    // Authenticated and Verified Main Admin Routes inside the /admin namespace
    const val DASHBOARD = "admin/dashboard"
    const val STUDENTS = "admin/students"
    const val STUDENT_DETAIL = "admin/student-detail/{emailKey}"
    const val SUBSCRIPTIONS = "admin/subscriptions"
    const val CONTENT = "admin/content"
    const val LEADERBOARDS = "admin/leaderboards"
    const val SUPPORT = "admin/support"
    const val SUPPORT_THREAD = "admin/support-thread/{studentUid}"
    const val AUDIT_LOGS = "admin/audit-logs"

    fun studentDetailRoute(emailKey: String): String = "admin/student-detail/$emailKey"

    fun supportThreadRoute(studentUid: String): String = "admin/support-thread/$studentUid"

    fun isPrimaryTabRoute(route: String?): Boolean {
        return route in setOf(DASHBOARD, STUDENTS, SUBSCRIPTIONS, CONTENT, LEADERBOARDS, SUPPORT)
    }

    fun isAllowedAdminNamespace(route: String): Boolean {
        val canonical = "/" + route.trim().trimStart('/')
        return canonical.startsWith("${AppNamespaces.ADMIN_ROOT}/") &&
            !canonical.startsWith("${AppNamespaces.STUDENT_ROOT}/") &&
            !canonical.startsWith(AppNamespaces.QUESTION_MANAGER_RESERVED_ROOT)
    }
}

object QuestionManagerRoutes {
    const val LOGIN = "question-manager/login"
    const val ACCESS_DENIED = "question-manager/access-denied"

    // Authenticated and Verified Question Management Routes inside /question-manager
    const val DASHBOARD = "question-manager/dashboard"
    const val EDITOR = "question-manager/editor"
    const val QUESTION_BANK = "question-manager/bank"
    const val IMPORT_EXPORT = "question-manager/import-export"
    const val AI_PREP = "question-manager/ai-prep"
    const val ADMIN_CONTROLS = "question-manager/admin-controls"

    fun isPrimaryTabRoute(route: String?): Boolean {
        return route in setOf(DASHBOARD, EDITOR, QUESTION_BANK, IMPORT_EXPORT, AI_PREP, ADMIN_CONTROLS)
    }

    fun isAllowedQuestionManagerNamespace(route: String): Boolean {
        val canonical = "/" + route.trim().trimStart('/')
        return canonical.startsWith("${AppNamespaces.QUESTION_MANAGER_RESERVED_ROOT}/") &&
            !canonical.startsWith("${AppNamespaces.STUDENT_ROOT}/") &&
            !canonical.startsWith("${AppNamespaces.ADMIN_ROOT}/")
    }
}
