package dev.krydo.mobile.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.krydo.mobile.ui.components.FloatingBottomNav
import dev.krydo.mobile.ui.components.FloatingNavItem
import dev.krydo.mobile.ui.screens.ActivityScreen
import dev.krydo.mobile.ui.screens.CredentialDetailScreen
import dev.krydo.mobile.ui.screens.CredentialsScreen
import dev.krydo.mobile.ui.screens.HomeScreen
import dev.krydo.mobile.ui.screens.LoginScreen
import dev.krydo.mobile.ui.screens.ProveScreen
import dev.krydo.mobile.ui.screens.ResultScreen
import dev.krydo.mobile.ui.screens.ScanScreen
import dev.krydo.mobile.ui.screens.SettingsScreen
import dev.krydo.mobile.ui.theme.KrydoColors

private val tabs = listOf(
    FloatingNavItem("home", "Home", Icons.Outlined.Home),
    FloatingNavItem("credentials", "Credentials", Icons.Outlined.Badge),
    FloatingNavItem("scan", "Scan", Icons.Outlined.QrCodeScanner),
    FloatingNavItem("activity", "Activity", Icons.Outlined.History),
)

@Composable
fun KrydoRoot(
    viewModel: AppViewModel,
    pendingRequestId: String?,
    onDeepLinkConsumed: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    LaunchedEffect(pendingRequestId, settings.hasSession) {
        val id = pendingRequestId ?: return@LaunchedEffect
        if (!settings.hasSession) return@LaunchedEffect
        viewModel.loadRequest(id) {
            navController.navigate("prove") {
                launchSingleTop = true
            }
        }
        onDeepLinkConsumed()
    }

    if (!settings.hasSession) {
        LoginScreen(viewModel = viewModel)
        return
    }

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = tabs.any { it.route == currentRoute }

    Scaffold(
        containerColor = KrydoColors.BackgroundPrimary,
        contentColor = KrydoColors.TextPrimary,
        bottomBar = {},
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.fillMaxSize(),
            ) {
                composable("home") {
                    HomeScreen(
                        viewModel = viewModel,
                        settings = settings,
                        onOpenCredentials = { navController.navigate("credentials") },
                        onOpenProve = { navController.navigate("prove") },
                        onOpenScan = { navController.navigate("scan") },
                        onOpenSettings = { navController.navigate("settings") },
                        onOpenCredential = { id -> navController.navigate("credential/$id") },
                    )
                }
                composable("credentials") {
                    CredentialsScreen(
                        viewModel = viewModel,
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
                composable("activity") {
                    ActivityScreen(viewModel = viewModel, settings = settings)
                }
                composable("settings") {
                    SettingsScreen(viewModel = viewModel)
                }
                composable("result") {
                    ResultScreen(
                        viewModel = viewModel,
                        onBack = {
                            viewModel.clearProveFlow()
                            navController.popBackStack("home", inclusive = false)
                        },
                    )
                }
            }

            if (showBottomBar) {
                FloatingBottomNav(
                    items = tabs,
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp),
                )
            }
        }
    }
}
