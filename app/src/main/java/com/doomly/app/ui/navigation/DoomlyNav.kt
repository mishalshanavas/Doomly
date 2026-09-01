package com.doomly.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.doomly.app.DeviceReadiness
import com.doomly.app.data.AuthRepository
import com.doomly.app.ui.screens.*
import com.doomly.app.ui.theme.*

private object Route {
    const val HOME = "home"; const val BOARD = "board"
    const val PERMISSIONS = "permissions"; const val PROFILE = "profile"
}

@Composable
fun DoomlyNav(resumeCount: Int = 0) {
    val context = LocalContext.current
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    var signedIn by remember { mutableStateOf(AuthRepository.getInstance().isSignedIn(context)) }
    var ready by remember { mutableStateOf(false) }

    LaunchedEffect(resumeCount, signedIn) {
        signedIn = AuthRepository.getInstance().isSignedIn(context)
        ready = DeviceReadiness.accessibilityEnabled(context) &&
            DeviceReadiness.batteryOptimizationDisabled(context) && signedIn
        val current = nav.currentDestination?.route
        if (ready && current == Route.PERMISSIONS) nav.navigate(Route.HOME) {
            popUpTo(Route.PERMISSIONS) { inclusive = true }
        } else if (!ready && current != null && current != Route.PERMISSIONS) nav.navigate(Route.PERMISSIONS) {
            popUpTo(0) { inclusive = true }
        }
    }

    val start = if (DeviceReadiness.accessibilityEnabled(context) &&
        DeviceReadiness.batteryOptimizationDisabled(context) && signedIn
    ) Route.HOME else Route.PERMISSIONS

    Scaffold(
        containerColor = Paper,
        bottomBar = {
            if (ready && route != Route.PERMISSIONS) NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                listOf(
                    Triple(Route.HOME, Icons.Rounded.Face, "Today"),
                    Triple(Route.BOARD, Icons.Rounded.BarChart, "Insights"),
                    Triple(Route.PROFILE, Icons.Rounded.Person, "Me")
                ).forEach { (destination, icon, label) ->
                    NavigationBarItem(
                        selected = route == destination,
                        onClick = { nav.navigate(destination) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        } },
                        icon = { Icon(icon, label) }, label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Ink, selectedTextColor = Ink, indicatorColor = Sunshine,
                            unselectedIconColor = Muted, unselectedTextColor = Muted
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(nav, start, Modifier.padding(padding)) {
            composable(Route.HOME) { HomeScreen() }
            composable(Route.BOARD) { LeaderboardScreen() }
            composable(Route.PERMISSIONS) { PermissionsScreen(resumeCount, onDone = { signedIn = true }) }
            composable(Route.PROFILE) { ProfileScreen(onSignOut = { signedIn = false }) }
        }
    }
}
