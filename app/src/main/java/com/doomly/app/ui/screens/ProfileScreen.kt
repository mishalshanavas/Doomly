package com.doomly.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doomly.app.DoomStatsStore
import com.doomly.app.ProfileViewModel
import com.doomly.app.data.AuthRepository
import com.doomly.app.ui.components.SoftCard
import com.doomly.app.ui.theme.*

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = viewModel(), onSignOut: () -> Unit = {}) {
    val owner = LocalLifecycleOwner.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val name by viewModel.getDisplayName().observeAsState("Doomly user")
    val initials by viewModel.getInitials().observeAsState("D")
    val streak by viewModel.getStreak().observeAsState("0")
    val target by viewModel.getDailyTarget().observeAsState(DoomStatsStore.DEFAULT_DAILY_TARGET)
    val total by viewModel.getTotalReels().observeAsState("0")
    val signedIn by viewModel.getSignedIn().observeAsState(false)
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh() }
        owner.lifecycle.addObserver(observer); viewModel.refresh()
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val best = DoomStatsStore.dailyHistory(context).values.maxOrNull() ?: 0

    Column(
        Modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(34.dp))
        Box(Modifier.size(86.dp).background(Sunshine, CircleShape), Alignment.Center) {
            Text(initials, style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(14.dp))
        Text(name, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text("Your scrolling personality", style = MaterialTheme.typography.bodyMedium, color = Muted)
        Spacer(Modifier.height(24.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfileStat("🔥", streak, "streak", Modifier.weight(1f))
            ProfileStat("🎯", target.toString(), "goal", Modifier.weight(1f))
            ProfileStat("🏆", best.toString(), "best", Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))
        SoftCard(Modifier.fillMaxWidth()) {
            Text("Lifetime mood", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
                Column {
                    Text(total, style = MaterialTheme.typography.displayMedium)
                    Text("Reels watched", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                Text("Still curious ✨", style = MaterialTheme.typography.labelMedium, color = Coral)
            }
        }
        Spacer(Modifier.height(18.dp))
        if (signedIn) OutlinedButton(
            onClick = { AuthRepository.getInstance().signOut(context); onSignOut() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(17.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Coral)
        ) { Text("Sign out") }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun ProfileStat(icon: String, value: String, label: String, modifier: Modifier) {
    SoftCard(modifier) {
        Text(icon, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(7.dp))
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Muted)
    }
}
