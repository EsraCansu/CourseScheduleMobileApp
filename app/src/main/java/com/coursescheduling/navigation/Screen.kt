package com.coursescheduling.navigation

/**
 * Type-safe navigation routes for the Compose NavHost.
 *
 * Each object/data class maps to a single composable destination.
 */
sealed class Screen(val route: String) {
    // ── Auth ────────────────────────────────────────────────────────────────
    object SignIn  : Screen("sign_in")
    object SignUp  : Screen("sign_up")
    object ChangePassword : Screen("change_password")

    // ── Main tabs ───────────────────────────────────────────────────────────
    object Home     : Screen("home")
    object Calendar : Screen("calendar")
    object Data     : Screen("data")
    object Settings : Screen("settings")
    object LecturerCalendar : Screen("lecturer_calendar/{lecturerId}") {
        fun createRoute(lecturerId: String) = "lecturer_calendar/$lecturerId"
    }
}
