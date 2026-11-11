package com.doomly.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.doomly.app.update.AppUpdateViewModel
import com.doomly.app.ui.components.TabLabel
import com.doomly.app.ui.components.UpdateBanner
import com.doomly.app.ui.screens.*
import com.doomly.app.ui.theme.*

private object Route {
    const val HOME = "home"; const val BOARD = "board"
    const val PERMISSIONS = "permissions"; const val PROFILE = "profile"
}

@Composable
fun DoomlyNav(resumeCount: Int = 0, notificationDestination: String? = null, onNotificationHandled: () -> Unit = {}, updates: AppUpdateViewModel? = null, onInstallUpdate: () -> Unit = {}) {
    val context = LocalContext.current
    val reducedMotion = LocalReduceMotion.current
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    var ready by remember { mutableStateOf(com.doomly.app.SetupPreferences.completed(context)) }
    val start = remember { if (ready) Route.HOME else Route.PERMISSIONS }
    LaunchedEffect(ready, notificationDestination) {
        if (ready && notificationDestination in listOf(Route.HOME, Route.PROFILE)) {
            nav.navigate(notificationDestination!!) { popUpTo(Route.HOME) { saveState = true }; launchSingleTop = true; restoreState = true }
            onNotificationHandled()
        }
    }
    val tabs = listOf(Route.HOME to "Today", Route.BOARD to "League", Route.PROFILE to "Profile")
    Scaffold(
        containerColor = BodyBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { if (updates != null) UpdateBanner(updates.state, onInstallUpdate) },
        bottomBar = {
            if (ready && route != Route.PERMISSIONS) {
                Column(Modifier.fillMaxWidth().background(BodyBg)) {
                    HorizontalDivider(thickness = 0.5.dp, color = BodySeparator)
                    Row(
                        Modifier.fillMaxWidth().navigationBarsPadding().height(56.dp)
                            .padding(horizontal = Spacing.sm).selectableGroup()
                    ) {
                        tabs.forEach { (destination, label) ->
                            TabLabel(
                                label = label,
                                selected = route == destination,
                                onClick = { nav.navigate(destination) { popUpTo(Route.HOME) { saveState = true }; launchSingleTop = true; restoreState = true } },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().background(BodyBg).padding(padding).consumeWindowInsets(padding)) {
            NavHost(nav, start, Modifier.fillMaxSize(),
                enterTransition = { if (reducedMotion) EnterTransition.None else fadeIn(tween(250)) },
                exitTransition = { if (reducedMotion) ExitTransition.None else fadeOut(tween(250)) }
            ) {
                composable(Route.HOME) { HomeScreen(resumeCount = resumeCount) }
                composable(Route.BOARD) { LeaderboardScreen(resumeCount = resumeCount) }
                composable(Route.PERMISSIONS) {
                    PermissionsScreen(resumeCount, onDone = {
                        ready = true
                        nav.navigate(Route.HOME) { popUpTo(Route.PERMISSIONS) { inclusive = true }; launchSingleTop = true }
                    })
                }
                composable(Route.PROFILE) { ProfileScreen(resumeCount = resumeCount, updates = updates, onInstallUpdate = onInstallUpdate) }
            }
        }
    }
}
