package com.mascode.itineraire.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.mascode.itineraire.ui.journey.ActiveJourneyScreen
import com.mascode.itineraire.ui.journey.ActiveJourneyViewModel
import com.mascode.itineraire.ui.places.PlacesScreen
import com.mascode.itineraire.ui.places.PlacesViewModel
import com.mascode.itineraire.ui.settings.ProfileScreen
import com.mascode.itineraire.ui.settings.PrivacyPolicyScreen
import com.mascode.itineraire.ui.settings.SecurityScreen
import com.mascode.itineraire.ui.settings.SettingsScreen
import com.mascode.itineraire.ui.settings.ThemeScreen
import com.mascode.itineraire.ui.today.TodayScreen
import com.mascode.itineraire.ui.today.TodayViewModel
import kotlinx.coroutines.launch

private enum class Destination(val label: String, val icon: ImageVector) {
    TODAY("Aujourd'hui", Icons.Outlined.Home),
    HISTORY("Historique", Icons.Outlined.History),
    PLACES("Lieux", Icons.Outlined.Place),
    SETTINGS("Paramètres", Icons.Outlined.Settings),
}

private const val MAIN_ROUTE = "main"
private const val ACTIVE_JOURNEY_ROUTE = "journey/{journeyId}"
private const val PROFILE_ROUTE = "settings/profile"
private const val SECURITY_ROUTE = "settings/security"
private const val THEME_ROUTE = "settings/theme"
private const val PRIVACY_POLICY_ROUTE = "settings/privacy-policy"

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
    val pagerState = rememberPagerState(pageCount = { Destination.entries.size })
    val coroutineScope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute == MAIN_ROUTE

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    Destination.entries.forEachIndexed { index, destination ->
                        NavigationBarItem(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
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
            startDestination = MAIN_ROUTE,
            modifier = Modifier.padding(padding),
        ) {
            composable(MAIN_ROUTE) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { Destination.entries[it].name },
                    beyondViewportPageCount = 1,
                ) { page ->
                    when (Destination.entries[page]) {
                        Destination.TODAY -> {
                            val viewModel: TodayViewModel = viewModel(factory = factory)
                            TodayScreen(
                                viewModel = viewModel,
                                onOpenPlaces = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(Destination.PLACES.ordinal)
                                    }
                                },
                                onOpenJourney = { journeyId -> navController.navigate("journey/$journeyId") },
                            )
                        }

                        Destination.HISTORY -> {
                            val viewModel: HistoryViewModel = viewModel(factory = factory)
                            HistoryScreen(
                                viewModel = viewModel,
                                onOpenJourney = { journeyId -> navController.navigate("journey/$journeyId") },
                            )
                        }

                        Destination.PLACES -> {
                            val viewModel: PlacesViewModel = viewModel(factory = factory)
                            PlacesScreen(viewModel)
                        }

                        Destination.SETTINGS -> SettingsScreen(
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
                            onOpenPrivacyPolicy = { navController.navigate(PRIVACY_POLICY_ROUTE) },
                        )
                    }
                }
            }
            composable(ACTIVE_JOURNEY_ROUTE) { entry ->
                val journeyId = entry.arguments?.getString("journeyId") ?: return@composable
                val viewModel: ActiveJourneyViewModel = viewModel(
                    key = "active-journey-$journeyId",
                    factory = factory.activeJourneyFactory(journeyId),
                )
                ActiveJourneyScreen(viewModel = viewModel, onBack = navController::popBackStack)
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
            composable(PRIVACY_POLICY_ROUTE) {
                PrivacyPolicyScreen(onBack = navController::popBackStack)
            }
        }
    }
}
