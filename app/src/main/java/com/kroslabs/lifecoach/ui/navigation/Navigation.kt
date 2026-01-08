package com.kroslabs.lifecoach.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Science
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object ApiKeySetup : Screen("api_key_setup")
    data object Onboarding : Screen("onboarding")
    data object Dashboard : Screen("dashboard")
    data object Experiments : Screen("experiments")
    data object Journal : Screen("journal")
    data object Profile : Screen("profile")
    data object Analytics : Screen("analytics")
    data object DeepDive : Screen("deep_dive")
    data object DebugLogs : Screen("debug_logs")
    data object PathDetail : Screen("path/{pathId}") {
        fun createRoute(pathId: Long) = "path/$pathId"
    }
    data object ExperimentDetail : Screen("experiment/{experimentId}") {
        fun createRoute(experimentId: Long) = "experiment/$experimentId"
    }
    data object CheckIn : Screen("checkin/{experimentId}") {
        fun createRoute(experimentId: Long) = "checkin/$experimentId"
    }
    data object CreateExperiment : Screen("create_experiment?pathId={pathId}") {
        fun createRoute(pathId: Long? = null) = if (pathId != null) {
            "create_experiment?pathId=$pathId"
        } else {
            "create_experiment"
        }
    }
    data object JournalEntry : Screen("journal_entry?entryId={entryId}") {
        fun createRoute(entryId: Long? = null) = if (entryId != null) {
            "journal_entry?entryId=$entryId"
        } else {
            "journal_entry"
        }
    }
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(
        screen = Screen.Dashboard,
        label = "Dashboard",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    ),
    BottomNavItem(
        screen = Screen.Experiments,
        label = "Experiments",
        selectedIcon = Icons.Filled.Science,
        unselectedIcon = Icons.Outlined.Science
    ),
    BottomNavItem(
        screen = Screen.Journal,
        label = "Journal",
        selectedIcon = Icons.Filled.Book,
        unselectedIcon = Icons.Outlined.Book
    ),
    BottomNavItem(
        screen = Screen.Profile,
        label = "Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
)
