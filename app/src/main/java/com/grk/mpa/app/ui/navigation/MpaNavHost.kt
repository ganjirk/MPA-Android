package com.grk.mpa.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.grk.mpa.app.ui.common.PlaceholderScreen
import com.grk.mpa.app.ui.home.HomeScreen

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
            HomeScreen(onSelect = onNavigate)
        }
        Destination.sections.forEach { destination ->
            composable(destination.route) {
                PlaceholderScreen(destination)
            }
        }
    }
}
