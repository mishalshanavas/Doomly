package com.doomly.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doomly.app.DoomStatsStore
import com.doomly.app.ProfileViewModel
import com.doomly.app.data.AuthRepository
import com.doomly.app.ui.components.PageHeader
import com.doomly.app.ui.components.PremiumCard
import com.doomly.app.ui.theme.*

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = viewModel(), onSignOut: () -> Unit = {}) {
    val owner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val name by viewModel.getDisplayName().observeAsState("Doomly user")
    val initials by viewModel.getInitials().observeAsState("D")
    val email by viewModel.getEmail().observeAsState("Local profile")
    val streak by viewModel.getStreak().observeAsState("0")
    val target by viewModel.getDailyTarget().observeAsState(DoomStatsStore.DEFAULT_DAILY_TARGET)
    val total by viewModel.getTotalReels().observeAsState("0")
    val signedIn by viewModel.getSignedIn().observeAsState(false)
    var confirmSignOut by remember { mutableStateOf(false) }

    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh() }
        owner.lifecycle.addObserver(observer)
        viewModel.refresh()
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val best = DoomStatsStore.dailyHistory(context).values.maxOrNull() ?: 0

    if (confirmSignOut) AlertDialog(
        onDismissRequest = { confirmSignOut = false },
        title = { Text("Sign out of Doomly?") },
        text = { Text("Your local counts stay on this device, but cloud sync and the league will pause.") },
        confirmButton = {
            TextButton(onClick = {
                AuthRepository.getInstance().signOut(context)
                confirmSignOut = false
                onSignOut()
            }) { Text("Sign out", color = Danger) }
        },
        dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Stay signed in") } }
    )

    Column(
        Modifier.fillMaxSize().background(Void).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(26.dp))
        PageHeader("Profile", "The person behind the highly trained thumb.")
        Spacer(Modifier.height(26.dp))

        Box(Modifier.size(82.dp).background(Frost, CircleShape), Alignment.Center) {
            Text(initials, style = MaterialTheme.typography.headlineMedium, color = Void)
        }
        Spacer(Modifier.height(12.dp))
        Text(name, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text(email, style = MaterialTheme.typography.bodySmall, color = Muted)
        Spacer(Modifier.height(22.dp))

        PremiumCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("LIFETIME REELS", style = MaterialTheme.typography.labelSmall, color = Muted)
                    Spacer(Modifier.height(5.dp))
                    Text(total, style = MaterialTheme.typography.displayMedium)
                }
                Icon(Icons.Rounded.AutoAwesome, null, tint = Frost, modifier = Modifier.padding(bottom = 7.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text("A number best enjoyed without context.", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileStat(streak, "DAY STREAK", Modifier.weight(1f))
            ProfileStat(target.toString(), "DAILY GOAL", Modifier.weight(1f))
            ProfileStat(best.toString(), "BEST DAY", Modifier.weight(1f))
        }

        Spacer(Modifier.height(22.dp))
        Text("ACCOUNT", style = MaterialTheme.typography.labelSmall, color = Muted, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(34.dp).background(Frost, CircleShape), Alignment.Center) {
                Icon(Icons.Rounded.Check, null, tint = Void, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Cloud sync", style = MaterialTheme.typography.titleSmall)
                Text(if (signedIn) "Signed in and keeping score" else "Local-only mode", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            Text(if (signedIn) "ON" else "OFF", style = MaterialTheme.typography.labelSmall)
        }

        if (signedIn) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { confirmSignOut = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger)
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign out")
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun ProfileStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Panel, RoundedCornerShape(14.dp)).padding(13.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 1)
        Spacer(Modifier.height(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Muted, maxLines = 1)
    }
}
