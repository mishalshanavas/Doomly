package com.doomly.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.doomly.app.update.UpdateState
import com.doomly.app.ui.theme.*

/** Only active update work occupies space on the main screens. */
@Composable
fun UpdateBanner(state: UpdateState, onInstall: () -> Unit) {
    if (state !is UpdateState.Downloading && state !is UpdateState.ReadyToInstall) return
    Column(Modifier.fillMaxWidth().background(Void).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().background(Panel).padding(horizontal = 24.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(when (state) {
                is UpdateState.Downloading -> "Updating Doomly · ${state.percent}%"
                is UpdateState.ReadyToInstall -> "Update ready · ${state.release.versionName}"
                else -> ""
            }, style = MaterialTheme.typography.bodySmall, color = Frost, modifier = Modifier.weight(1f))
            if (state is UpdateState.ReadyToInstall) TextButton(onClick = onInstall) { Text("Install") }
            else Spacer(Modifier.height(40.dp))
        }
        if (state is UpdateState.Downloading) LinearProgressIndicator(progress = { state.percent / 100f },
            modifier = Modifier.fillMaxWidth().height(2.dp), color = Mint, trackColor = PanelRaised)
    }
}
