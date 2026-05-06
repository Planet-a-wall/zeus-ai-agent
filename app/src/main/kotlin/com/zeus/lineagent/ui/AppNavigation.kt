package com.zeus.lineagent.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zeus.lineagent.AppContainer
import com.zeus.lineagent.ui.screens.ComposeFollowupScreen
import com.zeus.lineagent.ui.screens.GroupMappingsScreen
import com.zeus.lineagent.ui.screens.HomeScreen
import com.zeus.lineagent.ui.screens.SettingsScreen

object Routes {
    const val Home = "home"
    const val Settings = "settings"
    const val Mappings = "mappings"
    const val Followup = "followup/{mappingId}"
    fun followup(mappingId: String) = "followup/$mappingId"
}

@Composable
fun AppNavigation(container: AppContainer) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.Home) {
        composable(Routes.Home) {
            HomeScreen(
                container = container,
                onOpenSettings = { nav.navigate(Routes.Settings) },
                onOpenMappings = { nav.navigate(Routes.Mappings) },
                onComposeFollowup = { mappingId -> nav.navigate(Routes.followup(mappingId)) },
            )
        }
        composable(Routes.Settings) {
            SettingsScreen(container = container, onBack = { nav.popBackStack() })
        }
        composable(Routes.Mappings) {
            GroupMappingsScreen(container = container, onBack = { nav.popBackStack() })
        }
        composable(Routes.Followup) { backStack ->
            val mappingId = backStack.arguments?.getString("mappingId").orEmpty()
            ComposeFollowupScreen(
                container = container,
                mappingId = mappingId,
                onBack = { nav.popBackStack() },
            )
        }
    }
}
