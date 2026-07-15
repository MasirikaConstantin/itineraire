package com.mascode.itineraire.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mascode.itineraire.data.local.entity.LocalAccountEntity
import com.mascode.itineraire.domain.model.ThemeMode
import com.mascode.itineraire.ui.AppAccessState
import com.mascode.itineraire.ui.AppViewModel
import com.mascode.itineraire.ui.AppViewModelFactory
import com.mascode.itineraire.ui.auth.LockedScreen
import com.mascode.itineraire.ui.history.HistoryScreen
import com.mascode.itineraire.ui.history.HistoryViewModel
import com.mascode.itineraire.ui.places.PlacesScreen
import com.mascode.itineraire.ui.places.PlacesViewModel
import com.mascode.itineraire.ui.settings.ProfileScreen
import com.mascode.itineraire.ui.settings.SecurityScreen
import com.mascode.itineraire.ui.settings.SettingsScreen
import com.mascode.itineraire.ui.settings.ThemeScreen
import com.mascode.itineraire.ui.today.TodayScreen
import com.mascode.itineraire.ui.today.TodayViewModel

private enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    TODAY("today", "Aujourd'hui", Icons.Outlined.Home),
    HISTORY("history", "Historique", Icons.Outlined.History),
    PLACES("places", "Lieux", Icons.Outlined.Place),
    SETTINGS("settings", "Paramètres", Icons.Outlined.Settings),
}

private const val PROFILE_ROUTE = "settings/profile"
private const val SECURITY_ROUTE = "settings/security"
private const val THEME_ROUTE = "settings/theme"

@Composable
fun ItineraireApp(
    factory: AppViewModelFactory,
    activity: FragmentActivity,
    viewModel: AppViewModel,
    themeMode: ThemeMode,
) {
    val accessState by viewModel.accessState.collectAsStateWithLifecycle()

    when (val state = accessState) {
        AppAccessState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        is AppAccessState.Locked -> LockedScreen(
            activity = activity,
            displayName = state.account?.displayName,
            errorMessage = state.errorMessage,
            onAuthenticated = viewModel::unlock,
            onAuthenticationError = viewModel::reportAuthenticationError,
        )

        is AppAccessState.Authenticated -> MainNavigation(
            factory = factory,
            account = state.account,
            activity = activity,
            biometricLockEnabled = state.biometricLockEnabled,
            message = state.message,
            onLock = viewModel::lock,
            onSaveProfile = viewModel::saveProfile,
            onDeleteProfile = viewModel::deleteProfile,
            onProtectionChanged = viewModel::setBiometricLockEnabled,
            onAuthenticationError = viewModel::reportAuthenticationError,
            onClearMessage = viewModel::clearMessage,
            themeMode = themeMode,
            onThemeModeChanged = viewModel::setThemeMode,
        )
    }
}

@Composable
private fun MainNavigation(
    factory: AppViewModelFactory,
    account: LocalAccountEntity?,
    activity: FragmentActivity,
    biometricLockEnabled: Boolean,
    message: String?,
    onLock: () -> Unit,
    onSaveProfile: (String) -> Unit,
    onDeleteProfile: () -> Unit,
    onProtectionChanged: (Boolean) -> Unit,
    onAuthenticationError: (String) -> Unit,
    onClearMessage: () -> Unit,
    themeMode: ThemeMode,
    onThemeModeChanged: (ThemeMode) -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = Destination.entries.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
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
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.TODAY.route,
            modifier = if (showBottomBar) Modifier.padding(padding) else Modifier,
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
            composable(Destination.SETTINGS.route) {
                SettingsScreen(
                    account = account,
                    biometricLockEnabled = biometricLockEnabled,
                    onOpenProfile = {
                        onClearMessage()
                        navController.navigate(PROFILE_ROUTE)
                    },
                    onOpenSecurity = {
                        onClearMessage()
                        navController.navigate(SECURITY_ROUTE)
                    },
                    themeMode = themeMode,
                    onOpenTheme = { navController.navigate(THEME_ROUTE) },
                )
            }
            composable(PROFILE_ROUTE) {
                ProfileScreen(
                    account = account,
                    message = message,
                    onBack = navController::popBackStack,
                    onSave = onSaveProfile,
                    onDelete = onDeleteProfile,
                    onClearMessage = onClearMessage,
                )
            }
            composable(SECURITY_ROUTE) {
                SecurityScreen(
                    activity = activity,
                    biometricLockEnabled = biometricLockEnabled,
                    message = message,
                    onBack = navController::popBackStack,
                    onProtectionChanged = onProtectionChanged,
                    onLockNow = onLock,
                    onAuthenticationError = onAuthenticationError,
                )
            }
            composable(THEME_ROUTE) {
                ThemeScreen(
                    themeMode = themeMode,
                    onThemeModeChanged = onThemeModeChanged,
                    onBack = navController::popBackStack,
                )
            }
        }
    }
}
