package com.doomly.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
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
import com.doomly.app.LeagueViewModel
import com.doomly.app.data.LeaderboardRepository
import com.doomly.app.ui.components.PageHeader
import com.doomly.app.ui.components.PremiumCard
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
        owner.lifecycle.addObserver(observer)
        viewModel.refresh()
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    Column(Modifier.fillMaxSize().background(Void).padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(26.dp))
        PageHeader("The league", "A very serious ranking of unserious scrolling.")
        Spacer(Modifier.height(20.dp))
        when {
            loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = Frost, strokeWidth = 2.dp)
            }
            entries.isEmpty() -> EmptyState(
                errorTitle ?: "The room is suspiciously quiet",
                errorBody ?: "Watch a few Reels and claim the least competitive first place."
            )
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                item {
                    LeaderSummary(entries.first())
                    Spacer(Modifier.height(20.dp))
                    Text("TODAY", style = MaterialTheme.typography.labelSmall, color = Muted)
                    Spacer(Modifier.height(2.dp))
                }
                items(entries.take(30), key = { it.uid + it.rank }) { entry -> LeaderRow(entry) }
            }
        }
    }
}

@Composable
private fun LeaderSummary(leader: LeaderboardRepository.LeaderboardEntry) {
    PremiumCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(Frost, CircleShape), Alignment.Center) {
                Icon(Icons.Rounded.EmojiEvents, null, tint = Void, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("CURRENT MENACE", style = MaterialTheme.typography.labelSmall, color = Muted)
                Text(leader.displayName, style = MaterialTheme.typography.titleLarge, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${leader.reels}", style = MaterialTheme.typography.headlineMedium)
                Text("REELS", style = MaterialTheme.typography.labelSmall, color = Muted)
            }
        }
    }
}

@Composable
private fun LeaderRow(entry: LeaderboardRepository.LeaderboardEntry) {
    Row(
        Modifier.fillMaxWidth().background(if (entry.rank <= 3) Panel else Void, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            entry.rank.toString().padStart(2, '0'),
            style = MaterialTheme.typography.labelMedium,
            color = if (entry.rank <= 3) Frost else Muted,
            modifier = Modifier.width(28.dp)
        )
        Box(Modifier.size(38.dp).background(if (entry.rank == 1) Mint.copy(alpha = .5f) else PanelRaised, CircleShape), Alignment.Center) {
            Text(entry.initials(), style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(if (entry.rank <= 3) "Certified scroll athlete" else "Still in the thumb race", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Text(entry.reels.toString(), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun EmptyState(title: String, body: String) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(30.dp)) {
            Box(Modifier.size(56.dp).background(Panel, CircleShape), Alignment.Center) {
                Icon(Icons.Rounded.EmojiEvents, null, tint = Frost)
            }
            Spacer(Modifier.height(14.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(5.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = Muted, textAlign = TextAlign.Center)
        }
    }
}
