package com.doomly.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doomly.app.LeagueViewModel
import com.doomly.app.data.AuthRepository
import com.doomly.app.data.LeaderboardRepository
import com.doomly.app.ui.components.*
import com.doomly.app.ui.theme.*

@Composable
fun LeaderboardScreen(viewModel: LeagueViewModel = viewModel(), resumeCount: Int = 0) {
    val context = LocalContext.current
    val signedIn by viewModel.getSignedIn().observeAsState(false)
    LaunchedEffect(resumeCount) { viewModel.refresh() }
    val loading by viewModel.getLoading().observeAsState(false)
    val entries by viewModel.getEntries().observeAsState(emptyList())
    val errorTitle by viewModel.getErrorTitle().observeAsState()
    val errorBody by viewModel.getErrorBody().observeAsState()
    val myUid = remember(signedIn) { if (signedIn) AuthRepository.getInstance().currentUid(context) else "" }
    val me = entries.firstOrNull { myUid.isNotEmpty() && it.uid == myUid }
    val ahead = me?.let { mine -> entries.lastOrNull { it.rank < mine.rank } }

    Column(Modifier.fillMaxSize().background(BodyBg).verticalScroll(rememberScrollState())) {
        TopSheet {
            SheetContent {
                LabelStack(
                    label = "Today's league",
                    title = when {
                        !signedIn -> "Sign in to compete"
                        me != null -> "You're #${me.rank}"
                        loading -> "Loading rankings…"
                        else -> "Not ranked yet"
                    },
                    meta = when {
                        !signedIn -> "Rankings reset at midnight"
                        entries.isNotEmpty() -> "${entries.size} players today"
                        else -> "Watch a Reel to join today's board"
                    }
                )
            }
            Spacer(Modifier.height(Spacing.lg))
            if (me != null && ahead != null) {
                InsetCard {
                    LabelStack(
                        label = "Up next…",
                        title = "Pass ${ahead.displayName.ifBlank { "Player" }}",
                        meta = "${(ahead.reels - me.reels + 1).grouped()} more reels"
                    )
                }
            } else if (me != null) {
                InsetCard { LabelStack(label = "Up next…", title = "Hold first place", meta = "Nobody is ahead of you") }
            }
        }

        Column(Modifier.padding(horizontal = Spacing.screen)) {
            Spacer(Modifier.height(Spacing.xxl))
            when {
                !signedIn -> SignInButton(onSuccess = { viewModel.refresh() })
                loading && entries.isEmpty() -> Box(Modifier.fillMaxWidth().padding(vertical = Spacing.xxxl), Alignment.Center) {
                    CircularProgressIndicator(color = BodyInk, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
                entries.isEmpty() -> {
                    GroupedSection {
                        GroupedRow(errorTitle ?: "No rankings yet", subtitle = errorBody ?: "Check back after a few Reels.", onClick = { viewModel.refresh() },
                            trailing = { Text("Retry", style = MaterialTheme.typography.titleMedium, color = BodyInk) })
                    }
                }
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            when { loading -> "Updating…"; errorTitle != null -> "Couldn't refresh"; else -> "Rankings" },
                            style = MaterialTheme.typography.labelMedium, color = BodyMuted, modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.refresh() }, enabled = !loading) {
                            Text("Refresh", style = MaterialTheme.typography.labelMedium, color = if (loading) BodyMuted else BodyInk)
                        }
                    }
                    GroupedSection {
                        entries.forEachIndexed { index, entry ->
                            if (index > 0) HairlineDivider()
                            LeaderRow(entry, isMe = entry.uid == myUid)
                        }
                    }
                }
            }
            Spacer(Modifier.height(Spacing.xxl))
        }
    }
}

@Composable
private fun LeaderRow(entry: LeaderboardRepository.LeaderboardEntry, isMe: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.md)
            .semantics(mergeDescendants = true) { contentDescription = "Rank ${entry.rank}, ${entry.displayName}, ${entry.reels} Reels" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${entry.rank}", style = tabular(MaterialTheme.typography.bodyLarge), color = if (entry.rank == 1) BodyInk else BodyMuted, modifier = Modifier.width(32.dp))
        DotAvatar(entry.uid.ifBlank { entry.displayName }, Modifier.size(32.dp), onBody = true)
        Spacer(Modifier.width(Spacing.md))
        Text(
            entry.displayName.ifBlank { "Player" } + if (isMe) " (you)" else "",
            style = MaterialTheme.typography.bodyLarge, color = BodyInk,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
        )
        Text(entry.reels.grouped(), style = tabular(MaterialTheme.typography.bodyLarge), color = BodyInk)
    }
}
