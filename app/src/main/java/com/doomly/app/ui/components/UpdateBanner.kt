package com.doomly.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.doomly.app.ui.theme.*
import com.doomly.app.update.UpdateState

/** Update progress as a quiet ink band across the top; only the progress line is accent. */
@Composable
fun UpdateBanner(state: UpdateState, onInstall: () -> Unit) {
    if (state !is UpdateState.Downloading && state !is UpdateState.ReadyToInstall) return
    Column(Modifier.fillMaxWidth().background(Ink).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.screen, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                when (state) {
                    is UpdateState.Downloading -> "Updating Doomly · ${state.percent}%"
                    is UpdateState.ReadyToInstall -> "Update ready · ${state.release.versionName}"
                    else -> ""
                },
                style = tabular(MaterialTheme.typography.bodyMedium), color = Ground, modifier = Modifier.weight(1f)
            )
            if (state is UpdateState.ReadyToInstall) TextButton(onClick = onInstall) { Text("Install", color = Ground) }
        }
        if (state is UpdateState.Downloading) LinearProgressIndicator(
            progress = { state.percent / 100f },
            modifier = Modifier.fillMaxWidth().height(3.dp), color = Accent, trackColor = Color.Transparent
        )
    }
}