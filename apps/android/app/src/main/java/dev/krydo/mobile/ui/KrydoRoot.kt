package dev.krydo.mobile.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.krydo.mobile.ui.screens.CredentialDetailScreen
import dev.krydo.mobile.ui.screens.CredentialsScreen
import dev.krydo.mobile.ui.screens.HomeScreen
import dev.krydo.mobile.ui.screens.OnboardingScreen
import dev.krydo.mobile.ui.screens.ProveScreen
import dev.krydo.mobile.ui.screens.ResultScreen
import dev.krydo.mobile.ui.screens.ScanScreen
import dev.krydo.mobile.ui.screens.SettingsScreen

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab("home", "Home", Icons.Outlined.Home),
    Tab("credentials", "Credentials", Icons.Outlined.Badge),
    Tab("prove", "Prove", Icons.Outlined.Verified),
    Tab("scan", "Scan", Icons.Outlined.QrCodeScanner),
    Tab("settings", "Settings", Icons.Outlined.Settings),
)

@Composable
fun KrydoRoot(
    viewModel: AppViewModel,
    pendingRequestId: String?,
    onDeepLinkConsumed: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    LaunchedEffect(pendingRequestId) {
        val id = pendingRequestId ?: return@LaunchedEffect
        viewModel.loadRequest(id) {
            navController.navigate("prove") {
                launchSingleTop = true
            }
        }
        onDeepLinkConsumed()
    }

    if (!settings.onboardingDone) {
        OnboardingScreen(onContinue = viewModel::completeOnboarding)
        return
    }

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = tabs.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding),
        ) {
            composable("home") {
                HomeScreen(
                    settings = settings,
                    onOpenCredentials = { navController.navigate("credentials") },
                    onOpenProve = { navController.navigate("prove") },
                    onOpenSettings = { navController.navigate("settings") },
                )
            }
            composable("credentials") {
                CredentialsScreen(
                    credentials = viewModel.credentials,
                    onOpen = { id -> navController.navigate("credential/$id") },
                )
            }
            composable(
                route = "credential/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                CredentialDetailScreen(
                    credential = viewModel.credential(id),
                    onBack = { navController.popBackStack() },
                )
            }
            composable("prove") {
                ProveScreen(
                    viewModel = viewModel,
                    onShowResult = { navController.navigate("result") },
                )
            }
            composable("scan") {
                ScanScreen(
                    viewModel = viewModel,
                    onLoaded = { navController.navigate("prove") },
                )
            }
            composable("settings") {
                SettingsScreen(viewModel = viewModel)
            }
            composable("result") {
                ResultScreen(
                    viewModel = viewModel,
                    onBack = {
                        viewModel.clearProveFlow()
                        navController.popBackStack("prove", inclusive = false)
                    },
                )
            }
        }
    }
}
