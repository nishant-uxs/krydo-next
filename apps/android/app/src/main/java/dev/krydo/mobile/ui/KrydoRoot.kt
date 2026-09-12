package dev.krydo.mobile.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import dev.krydo.mobile.KrydoDeepLink
import dev.krydo.mobile.ui.components.FloatingBottomNav
import dev.krydo.mobile.ui.components.FloatingNavItem
import dev.krydo.mobile.ui.screens.CredentialDetailScreen
import dev.krydo.mobile.ui.screens.CredentialsScreen
import dev.krydo.mobile.ui.screens.HomeScreen
import dev.krydo.mobile.ui.screens.IssuerInboxScreen
import dev.krydo.mobile.ui.screens.LoginScreen
import dev.krydo.mobile.ui.screens.ProveScreen
import dev.krydo.mobile.ui.screens.RequestCredentialScreen
import dev.krydo.mobile.ui.screens.ResultScreen
import dev.krydo.mobile.ui.screens.ScanScreen
import dev.krydo.mobile.ui.screens.SettingsScreen
import dev.krydo.mobile.ui.screens.VerifierVerifyScreen
import dev.krydo.mobile.ui.screens.ZkProofsScreen
import dev.krydo.mobile.ui.theme.KrydoColors

@Composable
fun KrydoRoot(
    viewModel: AppViewModel,
    pendingDeepLink: KrydoDeepLink?,
    onDeepLinkConsumed: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val guestVerifier by viewModel.guestVerifier.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    val tabs = remember(settings.isIssuerOrRoot) {
        buildList {
            add(FloatingNavItem("home", "Home", Icons.Outlined.Home))
            add(FloatingNavItem("credentials", "Credentials", Icons.Outlined.Badge))
            add(FloatingNavItem("request", "Request", Icons.Outlined.Send))
            if (settings.isIssuerOrRoot) {
                add(FloatingNavItem("inbox", "Inbox", Icons.Outlined.Inbox))
            }
            add(FloatingNavItem("zk", "ZKP", Icons.Outlined.Fingerprint))
            add(FloatingNavItem("scan", "Scan", Icons.Outlined.QrCodeScanner))
        }
    }

    LaunchedEffect(pendingDeepLink, settings.hasSession) {
        when (val link = pendingDeepLink) {
            is KrydoDeepLink.Auth -> {
                viewModel.applyMobileAuthDeepLink(link.address, link.token)
                onDeepLinkConsumed()
            }
            is KrydoDeepLink.Present -> {
                if (!settings.hasSession) return@LaunchedEffect
                viewModel.loadRequest(link.requestId) {
                    navController.navigate("prove") {
                        launchSingleTop = true
                    }
                }
                onDeepLinkConsumed()
            }
            null -> Unit
        }
    }

    if (!settings.hasSession && guestVerifier) {
        VerifierVerifyScreen(
            viewModel = viewModel,
            onBackToLogin = viewModel::exitGuestVerifier,
        )
        return
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
                        onOpenRequest = { navController.navigate("request") },
                        onOpenProve = { navController.navigate("prove") },
                        onOpenScan = { navController.navigate("scan") },
                        onOpenSettings = { navController.navigate("settings") },
                        onOpenCredential = { id -> navController.navigate("credential/$id") },
                        onOpenZk = { navController.navigate("zk") },
                        onOpenInbox = { navController.navigate("inbox") },
                    )
                }
                composable("credentials") {
                    CredentialsScreen(
                        viewModel = viewModel,
                        onOpen = { id -> navController.navigate("credential/$id") },
                        onOpenRequest = { navController.navigate("request") },
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
                composable("request") {
                    RequestCredentialScreen(viewModel = viewModel)
                }
                composable("inbox") {
                    IssuerInboxScreen(viewModel = viewModel)
                }
                composable("zk") {
                    ZkProofsScreen(viewModel = viewModel)
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
