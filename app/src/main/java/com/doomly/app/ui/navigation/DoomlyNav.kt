package com.doomly.app.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.doomly.app.ui.screens.*
import com.doomly.app.ui.theme.*

private object Route {
    const val HOME = "home"; const val BOARD = "board"
    const val PERMISSIONS = "permissions"; const val PROFILE = "profile"
}

@Composable
fun DoomlyNav(resumeCount: Int = 0, notificationDestination: String? = null, onNotificationHandled: () -> Unit = {}) {
    val context = LocalContext.current
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    var ready by remember { mutableStateOf(com.doomly.app.SetupPreferences.completed(context)) }
    val start = remember { if (ready) Route.HOME else Route.PERMISSIONS }
    LaunchedEffect(ready, notificationDestination) {
        if (ready && notificationDestination in listOf(Route.HOME, Route.PROFILE)) {
            nav.navigate(notificationDestination!!) { launchSingleTop = true }
            onNotificationHandled()
        }
    }

    Scaffold(
        containerColor = Void,
        bottomBar = {
            if (ready && route != Route.PERMISSIONS) {
                Box(Modifier.fillMaxWidth().background(Void).navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp)) {
                    Surface(shape = RoundedCornerShape(20.dp), color = Void,
                        border = BorderStroke(1.dp, Hairline.copy(alpha = .25f)), tonalElevation = 0.dp) {
                        Row(Modifier.fillMaxWidth().selectableGroup().padding(5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(
                                Triple(Route.HOME, Icons.Rounded.Home, "Today"),
                                Triple(Route.BOARD, Icons.Rounded.EmojiEvents, "League"),
                                Triple(Route.PROFILE, Icons.Rounded.Person, "Profile")
                            ).forEach { (destination, icon, label) ->
                                val selected = route == destination
                                val tabColor by animateColorAsState(if (selected) Mint else Color.Transparent,
                                    tween(180), label = "Selected tab")
                                Column(Modifier.weight(1f).clip(RoundedCornerShape(15.dp))
                                    .background(tabColor)
                                    .selectable(selected = selected, role = Role.Tab, onClick = {
                                        nav.navigate(destination) {
                                            popUpTo(Route.HOME) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }).heightIn(min = 56.dp).padding(horizontal = 6.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center) {
                                    Icon(icon, null, Modifier.size(23.dp), tint = if (selected) Frost else Muted)
                                    Spacer(Modifier.height(3.dp))
                                    Text(label, style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) Frost else Muted)
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(nav, start, Modifier.padding(padding)) {
            composable(Route.HOME) { HomeScreen(resumeCount = resumeCount) }
            composable(Route.BOARD) { LeaderboardScreen(resumeCount = resumeCount) }
            composable(Route.PERMISSIONS) {
                PermissionsScreen(resumeCount, onDone = {
                    ready = true
                    nav.navigate(Route.HOME) { popUpTo(Route.PERMISSIONS) { inclusive = true }; launchSingleTop = true }
                })
            }
            composable(Route.PROFILE) { ProfileScreen(resumeCount = resumeCount) }
        }
    }
}
