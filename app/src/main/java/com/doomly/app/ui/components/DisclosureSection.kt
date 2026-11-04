package com.doomly.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.doomly.app.ui.theme.*

/** Secondary information stays discoverable without competing with the main task. */
@Composable
fun DisclosureSection(
    title: String,
    summary: String,
    initiallyExpanded: Boolean = false,
    key: Any? = Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable(key) { mutableStateOf(initiallyExpanded) }
    val chevronAngle by animateFloatAsState(if (expanded) 180f else 0f, tween(180), label = "Details chevron")
    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider(color = Hairline.copy(alpha = .25f))
        Row(Modifier.fillMaxWidth().semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
            .clickable(role = Role.Button, onClickLabel = if (expanded) "Hide $title" else "Show $title") { expanded = !expanded }
            .heightIn(min = 64.dp).padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Frost)
                Spacer(Modifier.height(3.dp))
                Text(summary, style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Rounded.ExpandMore, null, Modifier.rotate(chevronAngle), tint = Muted)
        }
        AnimatedVisibility(expanded) {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp), content = content)
        }
    }
}
