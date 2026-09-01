package com.doomly.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doomly.app.LeagueViewModel
import com.doomly.app.data.LeaderboardRepository
import com.doomly.app.ui.theme.*

@Composable
fun LeaderboardScreen(viewModel: LeagueViewModel = viewModel()) {
    val owner = LocalLifecycleOwner.current
    val loading by viewModel.getLoading().observeAsState(false)
    val entries by viewModel.getEntries().observeAsState(emptyList())
    val errorTitle by viewModel.getErrorTitle().observeAsState()
    val errorBody by viewModel.getErrorBody().observeAsState()
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh() }
        owner.lifecycle.addObserver(observer); viewModel.refresh()
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    Column(Modifier.fillMaxSize().background(Paper).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(28.dp))
        Text("Insights", style = MaterialTheme.typography.headlineLarge)
        Text("See who's in the scrolling mood today.", style = MaterialTheme.typography.bodyMedium, color = Muted)
        Spacer(Modifier.height(20.dp))
        when {
            loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Coral) }
            entries.isEmpty() -> EmptyState(errorTitle ?: "The board is quiet", errorBody ?: "Watch a few Reels and claim the first spot.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(entries.take(30), key = { it.uid + it.rank }) { entry -> LeaderRow(entry) }
            }
        }
    }
}

@Composable
private fun LeaderRow(entry: LeaderboardRepository.LeaderboardEntry) {
    val color = when (entry.rank) { 1 -> Sunshine; 2 -> Sky; 3 -> Lavender; else -> Color.White }
    Row(
        Modifier.fillMaxWidth().background(color, RoundedCornerShape(22.dp)).padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(38.dp).background(Ink, CircleShape), Alignment.Center) {
            Text(if (entry.rank <= 3) "#${entry.rank}" else entry.initials(), color = Color.White, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text("Today's mood", style = MaterialTheme.typography.labelSmall, color = Muted)
        }
        Text("${entry.reels}", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun EmptyState(title: String, body: String) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text("😴", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = Muted, textAlign = TextAlign.Center)
        }
    }
}
