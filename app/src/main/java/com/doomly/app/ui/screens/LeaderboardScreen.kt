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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doomly.app.LeagueViewModel
import com.doomly.app.data.LeaderboardRepository
import com.doomly.app.ui.components.SignInButton
import com.doomly.app.ui.components.PageHeader
import com.doomly.app.ui.components.BoardArt
import com.doomly.app.ui.components.GeneratedAvatar
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import com.doomly.app.ui.theme.*

@Composable
fun LeaderboardScreen(viewModel: LeagueViewModel = viewModel(), resumeCount: Int = 0) {
    val signedIn by viewModel.getSignedIn().observeAsState(false)
    LaunchedEffect(resumeCount) { viewModel.refresh() }
    val loading by viewModel.getLoading().observeAsState(false)
    val entries by viewModel.getEntries().observeAsState(emptyList())
    val errorTitle by viewModel.getErrorTitle().observeAsState()
    val errorBody by viewModel.getErrorBody().observeAsState()

    Column(Modifier.fillMaxSize().background(Void).padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(26.dp))
        PageHeader("The scroll club.", "More Reels. Higher rank. Fresh standings daily.")
        Spacer(Modifier.height(20.dp))
        if (signedIn && entries.isNotEmpty() && (errorTitle != null || loading)) {
            Text(if (loading) "Updating rankings…" else "Showing earlier rankings · refresh failed", style = MaterialTheme.typography.bodySmall, color = Muted)
            TextButton(onClick = { viewModel.refresh() }, enabled = !loading) { Text("Retry") }
        }
        when {
            !signedIn -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Your people are here.", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text("Sign in to share your display name and Reel totals on the daily leaderboard. Your local history stays available offline.", style = MaterialTheme.typography.bodyMedium, color = Muted)
                Spacer(Modifier.height(16.dp))
                SignInButton(onSuccess = { viewModel.refresh() })
            }
            loading && entries.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = Frost, strokeWidth = 2.dp)
            }
            entries.isEmpty() -> EmptyState(
                errorTitle ?: "The room is suspiciously quiet",
                errorBody ?: "Watch a few Reels and claim the least competitive first place.",
                onRetry = { viewModel.refresh() }
            )
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                item {
                    LeaguePodium(entries.take(3))
                    Spacer(Modifier.height(20.dp))
                    TextButton(onClick = { viewModel.refresh() }, enabled = !loading) { Text("Refresh rankings") }
                    if (entries.size > 3) {
                        Text("DAILY RANKINGS · HIGHEST COUNT FIRST", style = MaterialTheme.typography.labelSmall, color = Muted)
                        Spacer(Modifier.height(2.dp))
                    }
                }
                items(entries.drop(3), key = { it.uid + it.rank }) { entry -> LeaderRow(entry) }
            }
        }
    }
}

@Composable
private fun LeaguePodium(entries: List<LeaderboardRepository.LeaderboardEntry>) {
    val byRank = entries.associateBy { it.rank }
    // Keep the winner in the middle; absent places are omitted rather than invented.
    val places = listOfNotNull(byRank[2], byRank[1], byRank[3])
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text("Today’s leaders", style = MaterialTheme.typography.labelSmall, color = Muted)
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            places.forEach { entry ->
                val winner = entry.rank == 1
                val color = if (winner) Mint.copy(alpha = .12f) else Panel
                val podiumHeight = when (entry.rank) { 1 -> 100; 2 -> 80; else -> 64 }
                Column(Modifier.weight(1f).semantics(mergeDescendants = true) {
                    contentDescription = "Rank ${entry.rank}, ${entry.displayName}, ${entry.reels} Reels"
                }, horizontalAlignment = Alignment.CenterHorizontally) {
                    GeneratedAvatar(entry.uid.ifBlank { entry.displayName }, Modifier.size(if (winner) 48.dp else 42.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(entry.displayName.ifBlank { "Doomly player" },
                        style = MaterialTheme.typography.titleSmall, color = Frost,
                        textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().heightIn(min = (40 * fontScale).dp))
                    Spacer(Modifier.height(8.dp))
                    Column(Modifier.fillMaxWidth().background(color, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))

                        .heightIn(min = (podiumHeight * fontScale).dp).padding(horizontal = 6.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("${entry.rank}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Medium, color = Frost)
                        Spacer(Modifier.height(3.dp))
                        Text("${entry.reels}", style = MaterialTheme.typography.titleSmall, color = Frost)
                        Text("REELS", style = MaterialTheme.typography.labelSmall, color = Muted)
                    }
                }
            }
        }
        HorizontalDivider(color = Hairline.copy(alpha = .45f))
    }
}

@Composable
private fun LeaderRow(entry: LeaderboardRepository.LeaderboardEntry) {
    Row(
        Modifier.fillMaxWidth().background(if (entry.rank <= 3) Panel else Void, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            entry.rank.toString().padStart(2, '0'),
            style = MaterialTheme.typography.labelMedium,
            color = if (entry.rank <= 3) Frost else Muted,
            modifier = Modifier.width(28.dp)
        )
        GeneratedAvatar(entry.uid.ifBlank { entry.displayName }, Modifier.size(38.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(entry.reels.toString(), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun EmptyState(title: String, body: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(30.dp)) {
            Box(Modifier.size(80.dp).background(Peach, CircleShape), Alignment.Center) {
                BoardArt(com.doomly.app.R.drawable.board_trophy, Modifier.size(64.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(5.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = Muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onRetry) { Text("Try again") }
        }
    }
}
