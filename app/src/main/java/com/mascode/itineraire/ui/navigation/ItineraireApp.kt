package com.mascode.itineraire.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mascode.itineraire.ui.AppViewModelFactory
import com.mascode.itineraire.ui.history.HistoryScreen
import com.mascode.itineraire.ui.history.HistoryViewModel
import com.mascode.itineraire.ui.places.PlacesScreen
import com.mascode.itineraire.ui.places.PlacesViewModel
import com.mascode.itineraire.ui.today.TodayScreen
import com.mascode.itineraire.ui.today.TodayViewModel

private enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    TODAY("today", "Aujourd'hui", Icons.Outlined.Home),
    HISTORY("history", "Historique", Icons.Outlined.History),
    PLACES("places", "Lieux", Icons.Outlined.Place),
}

@Composable
fun ItineraireApp(factory: AppViewModelFactory) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                                modifier = Modifier.size(30.dp),
                            )
                        },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.TODAY.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Destination.TODAY.route) {
                val viewModel: TodayViewModel = viewModel(factory = factory)
                TodayScreen(viewModel, onOpenPlaces = { navController.navigate(Destination.PLACES.route) })
            }
            composable(Destination.HISTORY.route) {
                val viewModel: HistoryViewModel = viewModel(factory = factory)
                HistoryScreen(viewModel)
            }
            composable(Destination.PLACES.route) {
                val viewModel: PlacesViewModel = viewModel(factory = factory)
                PlacesScreen(viewModel)
            }
        }
    }
}
