package com.grk.mpa.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.grk.mpa.app.ui.account.AccountScreen
import com.grk.mpa.app.ui.games.GamesScreen
import com.grk.mpa.app.ui.home.HomeScreen
import com.grk.mpa.app.ui.productivity.ProductivityScreen
import com.grk.mpa.app.ui.support.ContactScreen
import com.grk.mpa.app.ui.support.HelpScreen
import com.grk.mpa.app.ui.utilities.UtilitiesScreen

/** Pages reached from the Account tab; they are not in the bottom bar. */
const val HELP_ROUTE = "help"
const val CONTACT_ROUTE = "contact"

@Composable
fun MpaNavHost(
    navController: NavHostController,
    onNavigate: (Destination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Destination.HOME.route,
        modifier = modifier
    ) {
        composable(Destination.HOME.route) {
            HomeScreen(
                onSelect = onNavigate,
                onOpenHelp = { navController.navigate(HELP_ROUTE) },
                onOpenContact = { navController.navigate(CONTACT_ROUTE) }
            )
        }
        composable(Destination.GAMES.route) { GamesScreen() }
        composable(Destination.UTILITIES.route) { UtilitiesScreen() }
        composable(Destination.PRODUCTIVITY.route) { ProductivityScreen() }
        composable(Destination.ACCOUNT.route) {
            AccountScreen(
                onOpenHelp = { navController.navigate(HELP_ROUTE) },
                onOpenContact = { navController.navigate(CONTACT_ROUTE) }
            )
        }
        composable(HELP_ROUTE) {
            HelpScreen(
                onBack = { navController.popBackStack() },
                onContact = { navController.navigate(CONTACT_ROUTE) { launchSingleTop = true } }
            )
        }
        composable(CONTACT_ROUTE) {
            ContactScreen(
                onBack = { navController.popBackStack() },
                onHelp = { navController.navigate(HELP_ROUTE) { launchSingleTop = true } }
            )
        }
    }
}
